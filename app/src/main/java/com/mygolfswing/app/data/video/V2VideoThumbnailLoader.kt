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
 * 当 [V2Memo.thumbnailUrl] 为空且本地视频存在时使用。
 */
object V2VideoThumbnailLoader {

    private val cache = LruCache<String, Bitmap>(30)

    fun cached(path: String): Bitmap? = cache.get(path)

    fun clearMemoryCache() {
        cache.evictAll()
    }

    suspend fun loadFirstFrame(videoUrlOrPath: String?): Bitmap? = withContext(Dispatchers.IO) {
        val path = V2MemoBridge.localVideoAbsolutePath(videoUrlOrPath) ?: return@withContext null
        val file = File(path)
        if (!file.exists()) return@withContext null
        cache.get(path)?.let { return@withContext it }
        val retriever = MediaMetadataRetriever()
        val bitmap = try {
            retriever.setDataSource(file.absolutePath)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
        bitmap?.let { cache.put(path, it) }
        bitmap
    }
}
