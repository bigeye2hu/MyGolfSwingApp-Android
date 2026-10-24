package com.mygolfswing.app.data.video

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import com.mygolfswing.app.data.ServiceLocator
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream

/**
 * 互动空间在线视频的磁盘缓存。播放时边下边写入本机，下次同一条视频直接读本地。
 * 地址里的签名参数不参与缓存键，避免临时链接每次都当成新视频。
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal object WorkspacePlaybackCache {
    private const val MAX_BYTES = 512L * 1024L * 1024L
    private const val TAIL_TOLERANCE_BYTES = 64L * 1024L
    private val lock = Any()
    private var cache: SimpleCache? = null
    private var cacheUserId: String? = null

    fun isRemote(uri: String): Boolean {
        val value = uri.trim().lowercase()
        return value.startsWith("https://") || value.startsWith("http://")
    }

    fun loadControl(): DefaultLoadControl = DefaultLoadControl.Builder()
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBufferDurationsMs(
            15_000,
            10 * 60_000,
            1_000,
            2_000,
        )
        .build()

    fun createMediaSource(context: Context, uri: String): MediaSource {
        val factory = cacheDataSourceFactory(cacheFor(context.applicationContext))
        return DefaultMediaSourceFactory(factory).createMediaSource(MediaItem.fromUri(uri))
    }

    private fun cacheDataSourceFactory(cache: SimpleCache): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true))
            .setCacheKeyFactory { dataSpec -> stableKey(dataSpec.uri) }
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    fun isFullyCached(context: Context, uri: String): Boolean {
        if (!isRemote(uri)) return true
        val cache = cacheFor(context.applicationContext)
        val key = stableKey(Uri.parse(uri))
        val length = ContentMetadata.getContentLength(cache.getContentMetadata(key))
        if (length <= 0L) return false
        val cachedHead = cache.getCachedLength(key, 0, length)
        if (cachedHead < 0L) return false
        // 播放器读完画面就会停，文件尾巴往往还剩几十到几百字节没写入。
        return length - cachedHead <= TAIL_TOLERANCE_BYTES
    }

    fun exportFile(context: Context, uri: String): File? {
        if (!isFullyCached(context, uri)) return null
        val appContext = context.applicationContext
        val cache = cacheFor(appContext)
        val parsed = Uri.parse(uri)
        val key = stableKey(parsed)
        val length = ContentMetadata.getContentLength(cache.getContentMetadata(key))
        if (length <= 0L) return null
        val outDir = File(appContext.cacheDir, "workspace_playback_export").apply { mkdirs() }
        val out = File(outDir, "export-${key.hashCode()}.mp4")
        if (out.isFile && out.length() == length) return out
        val dataSource = cacheDataSourceFactory(cache).createDataSource()
        return try {
            dataSource.open(DataSpec(parsed))
            FileOutputStream(out).use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = dataSource.read(buffer, 0, buffer.size)
                    if (read == C.RESULT_END_OF_INPUT || read <= 0) break
                    output.write(buffer, 0, read)
                }
            }
            out.takeIf { it.isFile && it.length() > 0L }
        } catch (_: Throwable) {
            out.delete()
            null
        } finally {
            runCatching { dataSource.close() }
        }
    }

    fun release() {
        synchronized(lock) {
            cache?.release()
            cache = null
            cacheUserId = null
        }
    }

    private fun cacheFor(context: Context): SimpleCache {
        val userId = runBlocking { ServiceLocator.session.current() }.userId?.takeIf { it.isNotBlank() } ?: "signed-out"
        synchronized(lock) {
            val existing = cache
            if (existing != null && cacheUserId == userId) return existing
            existing?.release()
            val directory = workspacePlaybackCacheDirectory(context.filesDir, userId).apply { mkdirs() }
            val created = SimpleCache(
                directory,
                LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                StandaloneDatabaseProvider(context),
            )
            cache = created
            cacheUserId = userId
            return created
        }
    }

    private fun stableKey(uri: Uri): String =
        uri.buildUpon().clearQuery().fragment(null).build().toString()
}
