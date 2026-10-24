package com.mygolfswing.app.data.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import com.mygolfswing.app.data.memo.V2MemoBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 运行时视频首帧加载（对齐 iOS `V2VideoThumbnailLoader`）。
 * 当 [V2Memo.thumbnailUrl] 为空时使用。默认只读取本地文件；教练素材等明确需要
 * 对齐远程视频首帧的页面可以通过 [allowRemote] 单独启用网络数据源。
 */
object V2VideoThumbnailLoader {

    private val cache = LruCache<String, Bitmap>(30)

    fun cached(path: String): Bitmap? = cache.get(path)

    fun clearMemoryCache() {
        cache.evictAll()
    }

    suspend fun loadFirstFrame(
        videoUrlOrPath: String?,
        allowRemote: Boolean = false,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val value = videoUrlOrPath?.trim().orEmpty()
        if (value.isEmpty()) return@withContext null
        val localPath = V2MemoBridge.localVideoAbsolutePath(value)
        val source = localPath?.takeIf { File(it).exists() }
            ?: value.takeIf {
                allowRemote && (
                    it.startsWith("https://", ignoreCase = true) ||
                        it.startsWith("http://", ignoreCase = true)
                )
            }
            ?: return@withContext null
        cache.get(source)?.let { return@withContext it }
        val retriever = MediaMetadataRetriever()
        val bitmap = try {
            if (
                source.startsWith("https://", ignoreCase = true) ||
                source.startsWith("http://", ignoreCase = true)
            ) {
                retriever.setDataSource(source, emptyMap())
            } else {
                retriever.setDataSource(source)
            }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
            val frameTimeUs = minOf(800L, (durationMs * 0.08).toLong()) * 1_000L
            retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
        bitmap?.let { cache.put(source, it) }
        bitmap
    }
}
