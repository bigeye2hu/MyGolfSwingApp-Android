package com.mygolfswing.app.data.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val videoFile = File(videoDir(context), "memo_$ts.mp4")
        val ok = runCatching {
            context.contentResolver.openInputStream(src)?.use { input ->
                FileOutputStream(videoFile).use { out ->
                    input.copyTo(out)
                }
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
        if (!ok || !videoFile.exists() || videoFile.length() == 0L) return@withContext null

        val (duration, w, h, thumb) = extractMetadata(context, videoFile)
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
        if (!file.exists() || file.length() == 0L) return@withContext null
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val target = File(videoDir(context), "memo_$ts.mp4")
        // FileProvider 临时文件通常在 cacheDir/videos/，rename 到 filesDir/videos/
        val moved = runCatching {
            if (file.absolutePath == target.absolutePath) return@runCatching true
            file.copyTo(target, overwrite = true)
            file.delete()
            true
        }.getOrDefault(false)
        if (!moved) return@withContext null
        val (duration, w, h, thumb) = extractMetadata(context, target)
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
        val dir = File(context.cacheDir, "videos").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "capture_$ts.mp4")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return file to uri
    }

    /** 计算所有已导入视频 / 缩略图占用的字节数，用于「存储管理」页。 */
    suspend fun calculateUsedBytes(context: Context): Long = withContext(Dispatchers.IO) {
        fun sumDir(dir: File): Long {
            if (!dir.exists()) return 0L
            return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        sumDir(videoDir(context)) + sumDir(thumbDir(context))
    }

    /** 清空所有视频 + 缩略图缓存。返回回收字节数。 */
    suspend fun clearAll(context: Context): Long = withContext(Dispatchers.IO) {
        val before = calculateUsedBytes(context)
        videoDir(context).deleteRecursively()
        thumbDir(context).deleteRecursively()
        before
    }

    // ============ 私有 ============

    private fun videoDir(context: Context): File =
        File(context.filesDir, VIDEO_DIR).apply { mkdirs() }

    private fun thumbDir(context: Context): File =
        File(context.filesDir, THUMB_DIR).apply { mkdirs() }

    private data class Meta(val durationSec: Double, val w: Int, val h: Int, val thumbPath: String?)

    private fun extractMetadata(context: Context, videoFile: File): Meta {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            val thumbPath = frame?.let { saveThumbnail(context, it, videoFile.nameWithoutExtension) }
            Meta(durMs / 1000.0, w, h, thumbPath)
        } catch (t: Throwable) {
            Meta(0.0, 0, 0, null)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun saveThumbnail(context: Context, bitmap: Bitmap, baseName: String): String? {
        val out = File(thumbDir(context), "${baseName}.jpg")
        return runCatching {
            FileOutputStream(out).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
            }
            out.absolutePath
        }.getOrNull()
    }
}
