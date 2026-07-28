package com.mygolfswing.app.data.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.content.FileProvider
import com.mygolfswing.app.data.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val videoFile = File(videoDir(context, userId), "memo_$ts.mp4")
        val ok = runCatching {
            context.contentResolver.openInputStream(src)?.use { input ->
                FileOutputStream(videoFile).use { out ->
                    input.copyTo(out)
                }
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
        if (!ok || !videoFile.exists() || videoFile.length() == 0L) return@withContext null

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
        if (!file.exists() || file.length() == 0L) return@withContext null
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val target = File(videoDir(context, userId), "memo_$ts.mp4")
        // FileProvider 临时文件通常在 cacheDir/videos/，rename 到 filesDir/videos/
        val moved = runCatching {
            if (file.absolutePath == target.absolutePath) return@runCatching true
            file.copyTo(target, overwrite = true)
            file.delete()
            true
        }.getOrDefault(false)
        if (!moved) return@withContext null
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
        val cleaned = url.trim().takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?: return@withContext null
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val target = File(videoDir(context, userId), "download_$ts.mp4")
        val request = Request.Builder().url(cleaned).get().build()
        val ok = runCatching {
            ServiceLocator.net.httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching false
                val body = response.body ?: return@runCatching false
                val total = body.contentLength()
                var copied = 0L
                body.byteStream().use { input ->
                    FileOutputStream(target).use { out ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            copied += read
                            if (total > 0L) onProgress((copied.toDouble() / total.toDouble()).coerceIn(0.0, 1.0))
                            else onProgress(-1.0)
                        }
                    }
                }
                true
            }
        }.getOrDefault(false)
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
}
