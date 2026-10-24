package com.mygolfswing.app.data.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.mygolfswing.app.data.ServiceLocator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

internal fun deleteRecursivelyCountingFreedBytes(vararg dirs: File): Long =
    dirs.sumOf { dir ->
        if (!dir.exists()) {
            0L
        } else {
            val bytes = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            if (runCatching { dir.deleteRecursively() }.getOrDefault(false)) bytes else 0L
        }
    }

internal fun deleteFileCountingFreedBytes(file: File?): Long {
    if (file == null || !file.exists() || !file.isFile) return 0L
    val bytes = file.length()
    return if (runCatching { file.delete() }.getOrDefault(false)) bytes else 0L
}

internal fun workspaceAnalysisCacheDirectory(filesDir: File, userId: String): File =
    File(filesDir, "users/$userId/workspace_analysis")

internal fun workspacePlaybackCacheDirectory(filesDir: File, userId: String): File =
    File(filesDir, "users/$userId/workspace_playback")

internal const val WORKSPACE_ANALYSIS_CACHE_MAX_BYTES: Long = 2L * 1024L * 1024L * 1024L
internal const val WORKSPACE_ANALYSIS_CACHE_MAX_AGE_MILLIS: Long = 7L * 24L * 60L * 60L * 1_000L

internal data class WorkspaceAnalysisCacheSnapshot(
    val bytes: Long,
    val fileCount: Int,
)

internal fun cleanupWorkspaceAnalysisCacheDirectory(
    root: File,
    nowMillis: Long = System.currentTimeMillis(),
    maxAgeMillis: Long = WORKSPACE_ANALYSIS_CACHE_MAX_AGE_MILLIS,
    maxBytes: Long = WORKSPACE_ANALYSIS_CACHE_MAX_BYTES,
): WorkspaceAnalysisCacheSnapshot {
    if (!root.exists()) return WorkspaceAnalysisCacheSnapshot(bytes = 0L, fileCount = 0)
    val currentFiles = root.walkTopDown().filter { it.isFile }.toList()
    currentFiles.filter { nowMillis - it.lastModified() > maxAgeMillis }.forEach(File::delete)
    val retained = root.walkTopDown().filter { it.isFile }.sortedBy(File::lastModified).toMutableList()
    var total = retained.sumOf(File::length)
    while (total > maxBytes && retained.isNotEmpty()) {
        val oldest = retained.removeAt(0)
        val size = oldest.length()
        if (oldest.delete()) total = (total - size).coerceAtLeast(0L)
    }
    root.walkBottomUp().filter { it.isDirectory && it != root }.forEach { directory ->
        if (directory.listFiles().isNullOrEmpty()) directory.delete()
    }
    val files = root.walkTopDown().filter { it.isFile }.toList()
    return WorkspaceAnalysisCacheSnapshot(
        bytes = files.sumOf(File::length),
        fileCount = files.size,
    )
}

internal fun normalizedRemoteVideoDownloadUrl(url: String): String? {
    val trimmed = url.trim()
    return when {
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        trimmed.startsWith("http://", ignoreCase = true) -> "https://" + trimmed.drop("http://".length)
        else -> null
    }
}

internal const val MAX_IMPORT_VIDEO_SIZE_BYTES: Long = 2L * 1024L * 1024L * 1024L
internal const val MIN_VIDEO_TRIM_DURATION_MS: Long = 500L
internal const val MAX_VIDEO_TRIM_DURATION_MS: Long = Long.MAX_VALUE / 1_000L

internal fun sourceVideoSizeIsAllowed(sizeBytes: Long?): Boolean =
    sizeBytes == null || sizeBytes in 0..MAX_IMPORT_VIDEO_SIZE_BYTES

internal fun localVideoFileIsUsableForImport(file: File): Boolean =
    file.exists() && file.isFile && file.length() > 0L && sourceVideoSizeIsAllowed(file.length())

internal fun importedVideoDurationMs(durationSec: Double): Long? {
    if (!durationSec.isFinite() || durationSec <= 0.0) return null
    val durationMs = durationSec * 1_000.0
    if (!durationMs.isFinite() || durationMs > MAX_VIDEO_TRIM_DURATION_MS.toDouble()) return null
    return durationMs.roundToLong().coerceAtLeast(1L)
}

internal data class VideoTrimRange(val startMs: Long, val endMs: Long) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

internal fun normalizedVideoTrimRange(
    durationMs: Long,
    startMs: Long,
    endMs: Long,
): VideoTrimRange? {
    val safeDurationMs = durationMs.takeIf { it > 0L } ?: return null
    if (safeDurationMs <= MIN_VIDEO_TRIM_DURATION_MS) {
        return VideoTrimRange(startMs = 0L, endMs = safeDurationMs)
    }
    val safeStartMs = startMs.coerceIn(0L, safeDurationMs - MIN_VIDEO_TRIM_DURATION_MS)
    val safeEndMs = endMs.coerceIn(safeStartMs + MIN_VIDEO_TRIM_DURATION_MS, safeDurationMs)
    return VideoTrimRange(startMs = safeStartMs, endMs = safeEndMs)
}

internal fun videoTrimKeepsFullSource(
    durationMs: Long,
    range: VideoTrimRange,
    toleranceMs: Long = 100L,
): Boolean =
    range.startMs <= toleranceMs && range.endMs >= durationMs - toleranceMs

internal fun shouldStopTrimmedTrackCopy(sampleTimeUs: Long, endUs: Long): Boolean =
    sampleTimeUs < 0L || sampleTimeUs >= endUs

internal fun supportedVideoExtensionFromName(name: String?): String? {
    val cleaned = name
        ?.substringBefore('?')
        ?.substringBefore('#')
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null
    val ext = cleaned.substringAfterLast('.', missingDelimiterValue = "")
        .lowercase(Locale.US)
        .takeIf { it.isNotBlank() }
        ?: return null
    return when (ext) {
        "mp4", "m4v", "mov" -> ext
        "qt" -> "mov"
        else -> null
    }
}

internal fun videoFileName(prefix: String, timestamp: String, sourceName: String?): String {
    val ext = supportedVideoExtensionFromName(sourceName) ?: "mp4"
    return "${prefix}_${timestamp}.${ext}"
}

internal data class GalleryVideoSpec(
    val displayName: String,
    val mimeType: String,
)

internal data class GalleryVideoPublishResult(
    val saved: Boolean,
    val message: String,
    val uri: Uri? = null,
)

internal fun galleryVideoSpec(source: File, displayBase: String, timestamp: Long): GalleryVideoSpec {
    val extension = supportedVideoExtensionFromName(source.name) ?: "mp4"
    val mimeType = when (extension) {
        "mov" -> "video/quicktime"
        "m4v" -> "video/x-m4v"
        else -> "video/mp4"
    }
    val safeBase = displayBase
        .trim()
        .map { char -> if (char.isLetterOrDigit() || char == '-' || char == '_') char else '_' }
        .joinToString("")
        .replace(Regex("_+"), "_")
        .trim('_')
        .take(48)
        .ifBlank { "video" }
    return GalleryVideoSpec(
        displayName = "Huibei_${safeBase}_$timestamp.$extension",
        mimeType = mimeType,
    )
}

/** Publishes a downloaded private video to the system gallery, matching iOS plaza-download behavior. */
internal suspend fun publishDownloadedVideoToGallery(
    context: Context,
    source: File,
    displayBase: String,
    timestamp: Long = System.currentTimeMillis(),
): GalleryVideoPublishResult = withContext(Dispatchers.IO) {
    if (!source.isFile || source.length() <= 0L) {
        return@withContext GalleryVideoPublishResult(false, "下载的视频文件不可用，请重新下载")
    }
    val spec = galleryVideoSpec(source, displayBase, timestamp)

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        val root = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: return@withContext GalleryVideoPublishResult(false, "系统视频目录暂时不可用，请稍后重试")
        val target = File(File(root, "Huibei").apply { mkdirs() }, spec.displayName)
        return@withContext try {
            source.inputStream().buffered().use { input ->
                target.outputStream().buffered().use { output -> input.copyTo(output) }
            }
            if (!target.isFile || target.length() <= 0L) {
                target.delete()
                GalleryVideoPublishResult(false, "视频写入相册失败，请检查存储空间后重试")
            } else {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(target.absolutePath),
                    arrayOf(spec.mimeType),
                    null,
                )
                GalleryVideoPublishResult(true, "已保存到系统相册")
            }
        } catch (cancelled: CancellationException) {
            target.delete()
            throw cancelled
        } catch (_: Throwable) {
            target.delete()
            GalleryVideoPublishResult(false, "保存到相册失败，请检查存储空间后重试")
        }
    }

    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, spec.displayName)
        put(MediaStore.Video.Media.MIME_TYPE, spec.mimeType)
        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/挥呗")
        put(MediaStore.Video.Media.IS_PENDING, 1)
    }
    var pendingUri: Uri? = null
    try {
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext GalleryVideoPublishResult(false, "系统相册暂时不可写，请检查存储空间后重试")
        pendingUri = uri
        val wrote = resolver.openOutputStream(uri)?.use { output ->
            source.inputStream().buffered().use { input -> input.copyTo(output) }
        } != null
        if (!wrote) {
            resolver.delete(uri, null, null)
            pendingUri = null
            return@withContext GalleryVideoPublishResult(false, "视频写入相册失败，请稍后重试")
        }
        val published = resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) },
            null,
            null,
        ) > 0
        if (!published) {
            resolver.delete(uri, null, null)
            pendingUri = null
            return@withContext GalleryVideoPublishResult(false, "相册保存未完成，请稍后重试")
        }
        pendingUri = null
        GalleryVideoPublishResult(true, "已保存到系统相册", uri)
    } catch (cancelled: CancellationException) {
        pendingUri?.let { runCatching { resolver.delete(it, null, null) } }
        throw cancelled
    } catch (_: Throwable) {
        pendingUri?.let { runCatching { resolver.delete(it, null, null) } }
        GalleryVideoPublishResult(false, "保存到相册失败，请检查存储空间后重试")
    }
}

internal inline fun <T> runVideoFileStoreCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}

internal fun archiveLocalVideoFile(source: File, target: File): Boolean {
    if (source.absolutePath == target.absolutePath) {
        return source.exists() && source.isFile && source.length() > 0L
    }
    target.parentFile?.mkdirs()
    if (runCatching { source.renameTo(target) }.getOrDefault(false)) {
        return target.exists() && target.isFile && target.length() > 0L
    }
    val copied = runCatching {
        source.copyTo(target, overwrite = true)
        target.exists() && target.isFile && target.length() > 0L
    }.getOrDefault(false)
    if (!copied) {
        deleteFileCountingFreedBytes(target)
        return false
    }
    source.delete()
    return true
}

internal fun deleteMediaFilesCountingFreedBytes(userRoot: File, protectedAbsolutePaths: Set<String> = emptySet()): Long {
    val protectedCanonicalPaths = protectedAbsolutePaths.mapNotNullTo(mutableSetOf()) { path ->
        runCatching { File(path).canonicalPath }.getOrNull()
    }
    return listOf(File(userRoot, "videos"), File(userRoot, "thumbs")).sumOf { dir ->
        deleteDirContentsCountingFreedBytes(dir, protectedCanonicalPaths)
    }
}

private fun deleteDirContentsCountingFreedBytes(dir: File, protectedCanonicalPaths: Set<String>): Long {
    if (!dir.exists()) return 0L
    var freed = 0L
    dir.walkBottomUp().forEach { file ->
        if (file == dir) return@forEach
        if (file.isFile) {
            val canonical = runCatching { file.canonicalPath }.getOrNull()
            if (canonical == null || canonical !in protectedCanonicalPaths) {
                freed += deleteFileCountingFreedBytes(file)
            }
        } else if (file.isDirectory && file.list()?.isEmpty() == true) {
            runCatching { file.delete() }
        }
    }
    return freed
}

/**
 * 视频文件导入器（对齐 iOS `V2MemoCreateBridge.extractFirstFrameJPEG` + V1 VideoMemoManager 落盘逻辑）。
 *
 * 职责：
 * 1. 把外部 Uri（系统相机临时文件 / 相册 PHPicker）**复制**到 App 私有目录 `filesDir/videos/`，
 *    避免 FileProvider grant 过期、Uri 失效。
 * 2. 用 [MediaMetadataRetriever] 抽出第一帧 → 压成 JPEG → 落到 `filesDir/thumbs/`。
 * 3. 读 duration（秒） / pixelWidth / pixelHeight。
 * 4. 提供 `createCameraOutputUri()` 给系统相机录制使用。
 *
 * 所有 IO 都在 [Dispatchers.IO] 上跑，不会阻塞 UI。
 */
object V2VideoFileStore {

    private const val VIDEO_DIR = "videos"
    private const val THUMB_DIR = "thumbs"
    private val _workspaceAnalysisDownloadCount = MutableStateFlow(0)
    val workspaceAnalysisDownloadCount: StateFlow<Int> = _workspaceAnalysisDownloadCount.asStateFlow()

    data class ImportResult(
        val absolutePath: String,
        val thumbnailPath: String?,
        val durationSec: Double,
        val widthPx: Int,
        val heightPx: Int,
        val sizeBytes: Long,
    )

    /**
     * 把外部 [src] Uri 复制到 App 私有目录，并抽缩略图。
     * 返回 null 表示读不到源（权限失败 / 文件不存在）。
     */
    suspend fun importVideo(context: Context, src: Uri): ImportResult? = withContext(Dispatchers.IO) {
        val userId = ServiceLocator.session.current().userId ?: return@withContext null
        val declaredSize = sourceVideoSizeBytes(context, src)
        if (!sourceVideoSizeIsAllowed(declaredSize)) return@withContext null
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sourceName = sourceVideoDisplayName(context, src) ?: src.lastPathSegment
        val videoFile = File(videoDir(context, userId), videoFileName("memo", ts, sourceName))
        val ok = runVideoFileStoreCatching {
            context.contentResolver.openInputStream(src)?.use { input ->
                FileOutputStream(videoFile).use { out ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        copied += read
                        if (!sourceVideoSizeIsAllowed(copied)) return@runVideoFileStoreCatching false
                        out.write(buffer, 0, read)
                    }
                }
            } ?: return@runVideoFileStoreCatching false
            true
        }.getOrDefault(false)
        if (!ok || !videoFile.exists() || videoFile.length() == 0L || !sourceVideoSizeIsAllowed(videoFile.length())) {
            deleteFileCountingFreedBytes(videoFile)
            return@withContext null
        }

        val (duration, w, h, thumb) = extractMetadata(context, userId, videoFile)
        ImportResult(
            absolutePath = videoFile.absolutePath,
            thumbnailPath = thumb,
            durationSec = duration,
            widthPx = w,
            heightPx = h,
            sizeBytes = videoFile.length(),
        )
    }

    /**
     * 接收一个**已经落到本地**的 File（例如系统相机直接写到 FileProvider 临时文件），
     * 把它移到私有目录 + 抽缩略图。
     */
    suspend fun adoptLocalFile(context: Context, file: File): ImportResult? = withContext(Dispatchers.IO) {
        val userId = ServiceLocator.session.current().userId ?: return@withContext null
        if (!localVideoFileIsUsableForImport(file)) return@withContext null
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val target = File(videoDir(context, userId), videoFileName("memo", ts, file.name))
        if (!archiveLocalVideoFile(file, target)) return@withContext null
        val (duration, w, h, thumb) = extractMetadata(context, userId, target)
        ImportResult(
            absolutePath = target.absolutePath,
            thumbnailPath = thumb,
            durationSec = duration,
            widthPx = w,
            heightPx = h,
            sizeBytes = target.length(),
        )
    }

    /**
     * 下载远端视频到 App 私有目录，并走同一套 metadata / thumbnail 提取流程。
     *
     * @param onProgress emits 0.0..1.0 when response exposes content length; callers should treat -1 as indeterminate.
     */
    suspend fun downloadRemoteVideo(
        context: Context,
        url: String,
        onProgress: (Double) -> Unit = {},
    ): ImportResult? = withContext(Dispatchers.IO) {
        val userId = ServiceLocator.session.current().userId ?: return@withContext null
        val cleaned = normalizedRemoteVideoDownloadUrl(url)
            ?: return@withContext null
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val target = File(videoDir(context, userId), videoFileName("download", ts, cleaned))
        val request = Request.Builder().url(cleaned).get().build()
        val call = ServiceLocator.net.httpClient.newCall(request)
        val coroutineContext = currentCoroutineContext()
        val cancellationHandle = coroutineContext[Job]?.invokeOnCompletion { cause ->
            if (cause is CancellationException) call.cancel()
        }
        val ok = try {
            runVideoFileStoreCatching {
                call.execute().use { response ->
                if (!response.isSuccessful) return@runVideoFileStoreCatching false
                val body = response.body ?: return@runVideoFileStoreCatching false
                val total = body.contentLength()
                if (total > MAX_IMPORT_VIDEO_SIZE_BYTES) return@runVideoFileStoreCatching false
                var copied = 0L
                body.byteStream().use { input ->
                    FileOutputStream(target).use { out ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            copied += read
                            if (!sourceVideoSizeIsAllowed(copied)) return@runVideoFileStoreCatching false
                            if (total > 0L) onProgress((copied.toDouble() / total.toDouble()).coerceIn(0.0, 1.0))
                            else onProgress(-1.0)
                        }
                    }
                }
                true
            }
            }.getOrDefault(false)
        } finally {
            cancellationHandle?.dispose()
        }
        if (!ok || !target.exists() || target.length() == 0L) {
            deleteFileCountingFreedBytes(target)
            return@withContext null
        }
        onProgress(1.0)
        val (duration, w, h, thumb) = extractMetadata(context, userId, target)
        ImportResult(
            absolutePath = target.absolutePath,
            thumbnailPath = thumb,
            durationSec = duration,
            widthPx = w,
            heightPx = h,
            sizeBytes = target.length(),
        )
    }

    /**
     * Downloads a private coach/student workspace video into a separate, logout-purged cache.
     * Personal memo videos continue to live in `videos/` and are intentionally retained.
     */
    suspend fun downloadWorkspaceAnalysisVideo(
        context: Context,
        url: String,
        onProgress: (Double) -> Unit = {},
    ): ImportResult? {
        val userId = ServiceLocator.session.current().userId ?: return null
        _workspaceAnalysisDownloadCount.update { it + 1 }
        return try {
            val imported = downloadRemoteVideo(context, url, onProgress) ?: return null
            withContext(Dispatchers.IO) {
                val cacheRoot = workspaceAnalysisRoot(context, userId).apply { mkdirs() }
                val sourceVideo = File(imported.absolutePath)
                val cachedVideo = File(cacheRoot, sourceVideo.name)
                val sourceThumb = imported.thumbnailPath?.let(::File)
                val cachedThumb = sourceThumb?.let { File(cacheRoot, it.name) }
                try {
                    sourceVideo.moveReplacing(cachedVideo)
                    if (sourceThumb != null && cachedThumb != null) sourceThumb.moveReplacing(cachedThumb)
                    cleanupWorkspaceAnalysisCacheDirectory(cacheRoot)
                    imported.copy(
                        absolutePath = cachedVideo.absolutePath,
                        thumbnailPath = cachedThumb?.absolutePath,
                    )
                } catch (throwable: Throwable) {
                    cachedVideo.delete()
                    cachedThumb?.delete()
                    sourceVideo.delete()
                    sourceThumb?.delete()
                    null
                }
            }
        } finally {
            _workspaceAnalysisDownloadCount.update { (it - 1).coerceAtLeast(0) }
        }
    }

    /** Clears only protected workspace-analysis media, preserving personal videos and notes. */
    suspend fun clearWorkspaceAnalysisCache(context: Context, userId: String): Long = withContext(Dispatchers.IO) {
        WorkspacePlaybackCache.release()
        deleteRecursivelyCountingFreedBytes(
            workspaceAnalysisRoot(context, userId),
            workspacePlaybackCacheDirectory(context.filesDir, userId),
        )
    }

    /** Applies the same seven-day / 2GB policy as iOS and returns the current real cache usage. */
    internal suspend fun cleanupWorkspaceAnalysisCache(
        context: Context,
        userId: String,
    ): WorkspaceAnalysisCacheSnapshot = withContext(Dispatchers.IO) {
        val analysis = cleanupWorkspaceAnalysisCacheDirectory(workspaceAnalysisRoot(context, userId))
        val playbackRoot = workspacePlaybackCacheDirectory(context.filesDir, userId)
        val playbackFiles = if (playbackRoot.exists()) playbackRoot.walkTopDown().filter { it.isFile }.toList() else emptyList()
        WorkspaceAnalysisCacheSnapshot(
            bytes = analysis.bytes + playbackFiles.sumOf { it.length() },
            fileCount = analysis.fileCount + playbackFiles.size,
        )
    }

    /**
     * Creates a passthrough-trimmed copy of an already imported local video.
     *
     * MediaMuxer trims on sample/keyframe boundaries, so the physical start can be slightly earlier
     * than the requested start; replay analysis favors preserving source quality over re-encoding.
     */
    suspend fun trimImportedVideo(
        context: Context,
        imported: ImportResult,
        startMs: Long,
        endMs: Long,
    ): ImportResult? = withContext(Dispatchers.IO) {
        val userId = ServiceLocator.session.current().userId ?: return@withContext null
        val source = File(imported.absolutePath)
        if (!localVideoFileIsUsableForImport(source)) return@withContext null
        val sourceDurationMs = importedVideoDurationMs(imported.durationSec)
            ?: return@withContext null
        val trimRange = normalizedVideoTrimRange(sourceDurationMs, startMs, endMs)
            ?: return@withContext null
        if (videoTrimKeepsFullSource(sourceDurationMs, trimRange)) return@withContext imported

        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val target = File(videoDir(context, userId), "memo_trim_$ts.mp4")
        val trimmed = runVideoFileStoreCatching {
            trimVideoPassthrough(
                source = source,
                target = target,
                trimRange = trimRange,
            )
        }.getOrDefault(false)
        if (!trimmed || !target.exists() || target.length() == 0L || !sourceVideoSizeIsAllowed(target.length())) {
            deleteFileCountingFreedBytes(target)
            return@withContext null
        }

        val (duration, w, h, thumb) = extractMetadata(context, userId, target)
        if (duration <= 0.0) {
            deleteFileCountingFreedBytes(target)
            thumb?.let { deleteFileCountingFreedBytes(File(it)) }
            return@withContext null
        }
        ImportResult(
            absolutePath = target.absolutePath,
            thumbnailPath = thumb,
            durationSec = duration,
            widthPx = w,
            heightPx = h,
            sizeBytes = target.length(),
        )
    }

    /**
     * 为系统相机创建一个**临时**输出 File + FileProvider Uri。
     * 路径必须匹配 `res/xml/file_paths.xml` 中的 `cache-path videos/` 映射。
     * 录制完成后，调用 [adoptLocalFile] 移到私有目录。
     */
    fun createCameraOutputUri(context: Context): Pair<File, Uri> {
        val file = createCameraOutputFile(context)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return file to uri
    }

    /** 为 App 内相机录制创建临时输出文件；录制完成后调用 [adoptLocalFile] 归档。 */
    fun createCameraOutputFile(context: Context): File {
        val dir = File(context.cacheDir, "videos").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "capture_$ts.mp4")
    }

    /** 计算所有已导入视频 / 缩略图占用的字节数，用于「存储管理」页。 */
    suspend fun calculateUsedBytes(context: Context): Long = withContext(Dispatchers.IO) {
        val userId = ServiceLocator.session.current().userId ?: return@withContext 0L
        calculateMediaBytes(userRootPath(context, userId))
    }

    /** 清空所有视频 + 缩略图缓存。返回回收字节数。 */
    suspend fun clearAll(context: Context, protectedAbsolutePaths: Set<String> = emptySet()): Long = withContext(Dispatchers.IO) {
        val userId = ServiceLocator.session.current().userId ?: return@withContext 0L
        deleteMediaFilesCountingFreedBytes(userRootPath(context, userId), protectedAbsolutePaths)
    }

    /** 删除指定用户的本地媒体目录（账号删除时调用）。 */
    suspend fun deleteUserDirectory(context: Context, userId: String) = withContext(Dispatchers.IO) {
        File(context.filesDir, "users/$userId").deleteRecursively()
    }

    // ============ 私有 ============

    internal fun calculateMediaBytes(userRoot: File): Long =
        listOf(File(userRoot, VIDEO_DIR), File(userRoot, THUMB_DIR)).sumOf { dir ->
            if (!dir.exists()) 0L else dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }

    private fun userRootPath(context: Context, userId: String): File =
        File(context.filesDir, "users/$userId")

    private fun workspaceAnalysisRoot(context: Context, userId: String): File =
        workspaceAnalysisCacheDirectory(context.filesDir, userId)

    private fun File.moveReplacing(target: File) {
        target.parentFile?.mkdirs()
        if (target.exists()) target.delete()
        if (!renameTo(target)) {
            copyTo(target, overwrite = true)
            if (!delete()) throw IllegalStateException("Unable to remove source after cache move")
        }
    }

    private fun sourceVideoSizeBytes(context: Context, uri: Uri): Long? =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && cursor.moveToFirst()) cursor.getLong(sizeIndex).takeIf { it >= 0 } else null
            }
        }.getOrNull()

    private fun sourceVideoDisplayName(context: Context, uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        }.getOrNull()

    private fun userRoot(context: Context, userId: String): File =
        userRootPath(context, userId).apply { mkdirs() }

    private fun videoDir(context: Context, userId: String): File =
        File(userRoot(context, userId), VIDEO_DIR).apply { mkdirs() }

    private fun thumbDir(context: Context, userId: String): File =
        File(userRoot(context, userId), THUMB_DIR).apply { mkdirs() }

    private data class Meta(val durationSec: Double, val w: Int, val h: Int, val thumbPath: String?)

    private fun extractMetadata(context: Context, userId: String, videoFile: File): Meta {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            val thumbPath = frame?.let { saveThumbnail(context, userId, it, videoFile.nameWithoutExtension) }
            Meta(durMs / 1000.0, w, h, thumbPath)
        } catch (t: Throwable) {
            Meta(0.0, 0, 0, null)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun saveThumbnail(context: Context, userId: String, bitmap: Bitmap, baseName: String): String? {
        val out = File(thumbDir(context, userId), "${baseName}.jpg")
        return runCatching {
            FileOutputStream(out).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
            }
            out.absolutePath
        }.getOrNull()
    }

    private data class MuxTrack(val sourceTrackIndex: Int, val muxerTrackIndex: Int, val format: MediaFormat)

    private fun trimVideoPassthrough(
        source: File,
        target: File,
        trimRange: VideoTrimRange,
    ): Boolean {
        target.parentFile?.mkdirs()
        if (target.exists()) target.delete()
        val sourceTracks = readPassthroughTracks(source)
        if (sourceTracks.none { it.format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true }) {
            return false
        }

        var muxer: MediaMuxer? = null
        var muxerStarted = false
        return try {
            val activeMuxer = MediaMuxer(target.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = activeMuxer
            videoRotationDegrees(source)?.let(activeMuxer::setOrientationHint)
            val muxTracks = sourceTracks.map { track ->
                MuxTrack(
                    sourceTrackIndex = track.sourceTrackIndex,
                    muxerTrackIndex = activeMuxer.addTrack(track.format),
                    format = track.format,
                )
            }
            activeMuxer.start()
            muxerStarted = true
            val startUs = trimRange.startMs * 1_000L
            val endUs = trimRange.endMs * 1_000L
            muxTracks.forEach { track ->
                copyTrimmedTrack(
                    source = source,
                    track = track,
                    muxer = activeMuxer,
                    startUs = startUs,
                    endUs = endUs,
                )
            }
            activeMuxer.stop()
            muxerStarted = false
            true
        } catch (_: Throwable) {
            false
        } finally {
            if (muxerStarted) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            if (!target.exists() || target.length() == 0L) target.delete()
        }
    }

    private data class SourceTrack(val sourceTrackIndex: Int, val format: MediaFormat)

    private fun readPassthroughTracks(source: File): List<SourceTrack> {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(source.absolutePath)
            buildList {
                for (trackIndex in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(trackIndex)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                        add(SourceTrack(trackIndex, format))
                    }
                }
            }
        } finally {
            extractor.release()
        }
    }

    private fun copyTrimmedTrack(
        source: File,
        track: MuxTrack,
        muxer: MediaMuxer,
        startUs: Long,
        endUs: Long,
    ) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(source.absolutePath)
            extractor.selectTrack(track.sourceTrackIndex)
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val buffer = ByteBuffer.allocate(trackCopyBufferSize(track.format))
            val bufferInfo = MediaCodec.BufferInfo()
            var firstSampleTimeUs: Long? = null

            while (true) {
                val sampleTimeUs = extractor.sampleTime
                if (shouldStopTrimmedTrackCopy(sampleTimeUs, endUs)) break
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                val baseTimeUs = firstSampleTimeUs ?: sampleTimeUs.also { firstSampleTimeUs = it }
                bufferInfo.set(
                    0,
                    sampleSize,
                    (sampleTimeUs - baseTimeUs).coerceAtLeast(0L),
                    muxerSampleFlags(extractor.sampleFlags),
                )
                muxer.writeSampleData(track.muxerTrackIndex, buffer, bufferInfo)
                if (!extractor.advance()) break
            }
        } finally {
            extractor.release()
        }
    }

    private fun trackCopyBufferSize(format: MediaFormat): Int {
        val defaultSize = 4 * 1024 * 1024
        val maxInputSize = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            defaultSize
        }
        return maxInputSize.coerceIn(DEFAULT_BUFFER_SIZE, 16 * 1024 * 1024)
    }

    internal fun muxerSampleFlags(extractorSampleFlags: Int): Int {
        var flags = 0
        if (extractorSampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
            flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
        }
        if (extractorSampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME != 0) {
            flags = flags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
        }
        return flags
    }

    private fun videoRotationDegrees(source: File): Int? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(source.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull()
                ?.takeIf { it == 90 || it == 180 || it == 270 }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
