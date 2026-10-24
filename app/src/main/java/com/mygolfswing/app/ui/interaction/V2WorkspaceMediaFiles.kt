package com.mygolfswing.app.ui.interaction

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.mygolfswing.app.data.coach.V2CoachUserFacingException
import com.mygolfswing.app.data.coach.V2WorkspaceDraftAttachment
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.data.video.importedVideoDurationMs
import com.mygolfswing.app.data.video.normalizedVideoTrimRange
import com.mygolfswing.app.data.video.videoTrimKeepsFullSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.CancellationException

internal data class V2PreparedWorkspaceVideo(
    val file: File,
    val cover: File?,
    val durationSeconds: Double?,
    val uploadId: String,
    val requestId: String,
    val sourceKind: String = "album",
    val coverTimeSeconds: Double? = null,
    val sourceRecordId: String? = null,
) {
    fun deleteTemporaryFiles() {
        file.delete()
        cover?.delete()
    }
}

internal fun restorePreparedWorkspaceVideo(
    attachment: V2WorkspaceDraftAttachment,
    fallbackRequestPrefix: String,
): V2PreparedWorkspaceVideo? {
    val media = attachment.videoPath?.let(::File)?.takeIf(File::isFile) ?: return null
    val cover = attachment.coverPath?.let(::File)?.takeIf(File::isFile)
    if (attachment.sourceKind != "voice_recording" && cover == null) return null
    return V2PreparedWorkspaceVideo(
        file = media,
        cover = cover,
        durationSeconds = attachment.durationSeconds,
        uploadId = attachment.uploadId,
        requestId = attachment.requestId ?: "${fallbackRequestPrefix}_${attachment.uploadId}",
        sourceKind = attachment.sourceKind,
        coverTimeSeconds = attachment.coverTimeSeconds,
        sourceRecordId = attachment.sourceRecordId,
    )
}

internal fun V2PreparedWorkspaceVideo.asVideoImport(): V2VideoFileStore.ImportResult =
    V2VideoFileStore.ImportResult(
        absolutePath = file.absolutePath,
        thumbnailPath = cover?.absolutePath,
        durationSec = durationSeconds ?: 0.0,
        widthPx = 0,
        heightPx = 0,
        sizeBytes = file.length(),
    )

internal suspend fun trimPreparedWorkspaceVideo(
    context: Context,
    prepared: V2PreparedWorkspaceVideo,
    startMs: Long,
    endMs: Long,
): V2PreparedWorkspaceVideo {
    val durationMs = prepared.durationSeconds?.let(::importedVideoDurationMs)
    val trimRange = durationMs?.let { normalizedVideoTrimRange(it, startMs, endMs) }
    if (durationMs != null && trimRange != null && !videoTrimKeepsFullSource(durationMs, trimRange)) {
        ensureWorkspaceVideoStorage(context, prepared.file.length())
    }
    val trimmed = V2VideoFileStore.trimImportedVideo(
        context = context,
        imported = prepared.asVideoImport(),
        startMs = startMs,
        endMs = endMs,
    ) ?: throw V2CoachUserFacingException("视频裁剪失败，请重新选择或使用整段")
    if (trimmed.absolutePath == prepared.file.absolutePath) return prepared

    val trimmedFile = File(trimmed.absolutePath)
    var trimmedCover = trimmed.thumbnailPath?.let(::File)?.takeIf { it.isFile && it.length() > 0L }
    try {
        if (trimmedCover == null) trimmedCover = createWorkspaceVideoCover(trimmedFile)
        val result = V2PreparedWorkspaceVideo(
            file = trimmedFile,
            cover = trimmedCover,
            durationSeconds = trimmed.durationSec,
            uploadId = prepared.uploadId,
            requestId = prepared.requestId,
            sourceKind = prepared.sourceKind,
            coverTimeSeconds = null,
            sourceRecordId = prepared.sourceRecordId,
        )
        prepared.deleteTemporaryFiles()
        return result
    } catch (throwable: Throwable) {
        trimmedFile.delete()
        trimmedCover?.delete()
        throw throwable
    }
}

internal suspend fun prepareWorkspaceVideo(
    context: Context,
    uri: Uri,
    uploadId: String,
    requestId: String,
    sourceKind: String = "album",
): V2PreparedWorkspaceVideo = prepareWorkspaceVideoInDirectory(
    context = context,
    uri = uri,
    uploadId = uploadId,
    requestId = requestId,
    sourceKind = sourceKind,
    destinationDirectory = context.cacheDir,
)

internal suspend fun preparePersistentLibraryImportVideo(
    context: Context,
    uri: Uri,
    ownerUserId: String,
    uploadId: String,
    requestId: String,
    sourceKind: String = "album",
): V2PreparedWorkspaceVideo = prepareWorkspaceVideoInDirectory(
    context = context,
    uri = uri,
    uploadId = uploadId,
    requestId = requestId,
    sourceKind = sourceKind,
    destinationDirectory = coachLibraryDraftDirectory(context.filesDir, ownerUserId),
)

internal fun coachLibraryDraftDirectory(filesDir: File, ownerUserId: String): File =
    File(filesDir, "users/$ownerUserId/coach_library_drafts")

private suspend fun prepareWorkspaceVideoInDirectory(
    context: Context,
    uri: Uri,
    uploadId: String,
    requestId: String,
    sourceKind: String,
    destinationDirectory: File,
): V2PreparedWorkspaceVideo = withContext(Dispatchers.IO) {
    val displayName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
        }
    val mime = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
    val extension = displayName?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)
        ?.takeIf { it in setOf("mp4", "mov", "m4v") }
        ?: when (mime) {
            "video/quicktime" -> "mov"
            "video/x-m4v" -> "m4v"
            else -> "mp4"
        }
    val declaredSize = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
        ?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst()) cursor.getLong(index).takeIf { it > 0L } else null
        }
    if (declaredSize != null && !workspaceVideoUploadSizeIsAllowed(declaredSize)) {
        throw V2CoachUserFacingException("视频文件需要小于500MB")
    }
    ensureWorkspaceVideoStorage(context, declaredSize)
    destinationDirectory.mkdirs()
    val video = File.createTempFile("workspace-video-", ".$extension", destinationDirectory)
    try {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw V2CoachUserFacingException("无法读取所选视频")
        input.buffered().use { source ->
            video.outputStream().buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = source.read(buffer)
                    if (read < 0) break
                    total += read
                    if (!workspaceVideoUploadSizeIsAllowed(total)) {
                        throw V2CoachUserFacingException("视频文件需要小于500MB")
                    }
                    output.write(buffer, 0, read)
                }
            }
        }
        if (video.length() <= 0L) throw V2CoachUserFacingException("无法读取所选视频")

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(video.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.takeIf { it > 0 }?.div(1_000.0)
            val cover = createWorkspaceVideoCover(video)
            V2PreparedWorkspaceVideo(video, cover, duration, uploadId, requestId, sourceKind)
        } finally {
            retriever.release()
        }
    } catch (throwable: Throwable) {
        video.delete()
        throw throwable
    }
}

internal fun workspaceVideoUploadSizeIsAllowed(sizeBytes: Long): Boolean =
    sizeBytes in 1L..MAX_WORKSPACE_VIDEO_BYTES

internal fun workspaceVideoRequiredFreeBytes(sourceBytes: Long?): Long {
    val safeSource = sourceBytes?.takeIf { it > 0L }
    return if (safeSource == null) {
        UNKNOWN_SOURCE_STORAGE_FLOOR_BYTES
    } else {
        safeSource + maxOf(WORKSPACE_STORAGE_RESERVE_BYTES, safeSource / 10L)
    }
}

internal fun workspaceVideoHasEnoughStorage(availableBytes: Long, sourceBytes: Long?): Boolean =
    availableBytes >= workspaceVideoRequiredFreeBytes(sourceBytes)

private fun ensureWorkspaceVideoStorage(context: Context, sourceBytes: Long?) {
    val availableBytes = runCatching { StatFs(context.cacheDir.absolutePath).availableBytes }.getOrDefault(0L)
    if (!workspaceVideoHasEnoughStorage(availableBytes, sourceBytes)) {
        throw V2CoachUserFacingException("本机存储空间不足，请清理空间后再处理视频")
    }
}

internal fun normalizedWorkspaceCoverTimeSeconds(
    durationSeconds: Double?,
    requestedSeconds: Double?,
): Double {
    val duration = durationSeconds?.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
    val fallback = minOf(0.8, duration * 0.08)
    return (requestedSeconds?.takeIf(Double::isFinite) ?: fallback).coerceIn(0.0, duration)
}

internal suspend fun previewWorkspaceVideoCover(
    prepared: V2PreparedWorkspaceVideo,
    selectedSeconds: Double,
): File = withContext(Dispatchers.IO) {
    createWorkspaceVideoCover(prepared.file, selectedSeconds)
}

internal suspend fun replaceWorkspaceVideoCover(
    prepared: V2PreparedWorkspaceVideo,
    selectedSeconds: Double,
): V2PreparedWorkspaceVideo = withContext(Dispatchers.IO) {
    val normalizedSeconds = normalizedWorkspaceCoverTimeSeconds(prepared.durationSeconds, selectedSeconds)
    val replacement = createWorkspaceVideoCover(prepared.file, normalizedSeconds)
    prepared.cover?.delete()
    prepared.copy(cover = replacement, coverTimeSeconds = normalizedSeconds)
}

internal suspend fun saveWorkspaceAnalysisFrameToGallery(
    context: Context,
    video: File,
    positionMs: Long,
): String = withContext(Dispatchers.IO) {
    if (!video.isFile || video.length() <= 0L) return@withContext "视频文件不可用，请重新打开后截图"
    val retriever = MediaMetadataRetriever()
    var pendingUri: Uri? = null
    try {
        retriever.setDataSource(video.absolutePath)
        val bitmap = retriever.getFrameAtTime(
            positionMs.coerceAtLeast(0L) * 1_000L,
            MediaMetadataRetriever.OPTION_CLOSEST,
        ) ?: return@withContext "当前帧暂时无法生成截图"
        try {
            val displayName = "huibei_analysis_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val root = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: return@withContext "系统图片目录暂时不可用，请稍后重试"
                val folder = File(root, "Huibei").apply { mkdirs() }
                val target = File(folder, displayName)
                val wrote = target.outputStream().buffered().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }
                if (!wrote || !target.isFile || target.length() <= 0L) {
                    target.delete()
                    return@withContext "截图写入失败，请稍后重试"
                }
                MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("image/png"), null)
                return@withContext "截图已保存到相册"
            }
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Huibei")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext "系统相册暂时不可写，请稍后重试"
            pendingUri = uri
            val wrote = resolver.openOutputStream(uri)?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            } == true
            if (!wrote) {
                resolver.delete(uri, null, null)
                pendingUri = null
                return@withContext "截图写入失败，请稍后重试"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            pendingUri = null
            "截图已保存到相册"
        } finally {
            bitmap.recycle()
        }
    } catch (cancelled: CancellationException) {
        pendingUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
        throw cancelled
    } catch (_: Throwable) {
        pendingUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
        "当前帧无法保存，请稍后重试"
    } finally {
        runCatching { retriever.release() }
    }
}

internal suspend fun saveWorkspaceAnalysisRecordingToGallery(
    context: Context,
    video: File,
): String = withContext(Dispatchers.IO) {
    if (!video.isFile || video.length() <= 0L) return@withContext "录屏文件不可用，请重新录制"
    val displayName = "huibei_analysis_recording_${System.currentTimeMillis()}.mp4"
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        val root = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: return@withContext "系统视频目录暂时不可用，请稍后重试"
        val folder = File(root, "Huibei").apply { mkdirs() }
        val target = File(folder, displayName)
        return@withContext try {
            video.inputStream().buffered().use { input ->
                target.outputStream().buffered().use(input::copyTo)
            }
            if (!target.isFile || target.length() <= 0L) {
                target.delete()
                "视频写入相册失败，请稍后重试"
            } else {
                MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("video/mp4"), null)
                "录屏已保存到相册"
            }
        } catch (cancelled: CancellationException) {
            target.delete()
            throw cancelled
        } catch (_: Throwable) {
            target.delete()
            "保存到相册失败，请稍后重试"
        }
    }

    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Huibei")
        put(MediaStore.Video.Media.IS_PENDING, 1)
    }
    var pendingUri: Uri? = null
    try {
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext "系统相册暂时不可写，请稍后重试"
        pendingUri = uri
        val wrote = resolver.openOutputStream(uri)?.use { output ->
            video.inputStream().buffered().use { input -> input.copyTo(output) }
        } != null
        if (!wrote) {
            resolver.delete(uri, null, null)
            pendingUri = null
            return@withContext "视频写入相册失败，请稍后重试"
        }
        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        pendingUri = null
        "录屏已保存到相册"
    } catch (cancelled: CancellationException) {
        pendingUri?.let { runCatching { resolver.delete(it, null, null) } }
        throw cancelled
    } catch (_: Throwable) {
        pendingUri?.let { runCatching { resolver.delete(it, null, null) } }
        "保存到相册失败，请稍后重试"
    }
}

private fun createWorkspaceVideoCover(video: File, preferredTimeSeconds: Double? = null): File {
    val retriever = MediaMetadataRetriever()
    val cover = File.createTempFile("workspace-cover-", ".jpg", video.parentFile)
    try {
        retriever.setDataSource(video.absolutePath)
        val durationSeconds = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toDoubleOrNull()?.div(1_000.0)
        val selectedSeconds = normalizedWorkspaceCoverTimeSeconds(durationSeconds, preferredTimeSeconds)
        val bitmap = retriever.getFrameAtTime(
            (selectedSeconds * 1_000_000.0).toLong(),
            MediaMetadataRetriever.OPTION_CLOSEST,
        )
            ?: throw V2CoachUserFacingException("无法生成视频封面")
        try {
            cover.outputStream().buffered().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 78, output)) {
                    throw V2CoachUserFacingException("无法生成视频封面")
                }
            }
        } finally {
            bitmap.recycle()
        }
        return cover
    } catch (throwable: Throwable) {
        cover.delete()
        throw throwable
    } finally {
        retriever.release()
    }
}

private const val MAX_WORKSPACE_VIDEO_BYTES = 500L * 1024L * 1024L
private const val WORKSPACE_STORAGE_RESERVE_BYTES = 64L * 1_024L * 1_024L
private const val UNKNOWN_SOURCE_STORAGE_FLOOR_BYTES = 128L * 1_024L * 1_024L
