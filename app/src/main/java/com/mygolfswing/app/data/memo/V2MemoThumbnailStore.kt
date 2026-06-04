package com.mygolfswing.app.data.memo

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.video.V2OnlineVideoMetadataFetcher
import com.mygolfswing.app.data.video.V2VideoThumbnailUtil
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Memo 缩略图落盘（对齐 iOS `VideoMemo.thumbnailImage` + 云端 `thumbnailBase64`）。
 *
 * 文件目录：`filesDir/memo_thumbs/{memoId}.jpg`
 */
object V2MemoThumbnailStore {

    private const val THUMB_DIR = "memo_thumbs"

    fun thumbFile(context: Context, memoId: UUID, userId: String? = null): File =
        thumbDir(context, userId).resolve("${memoId}.jpg")

    fun fileUrlForThumb(context: Context, memoId: UUID, userId: String? = null): String? {
        val f = thumbFile(context, memoId, userId)
        return if (f.exists() && f.length() > 0) "file://${f.absolutePath}" else null
    }

    /** 云端 Base64 → 本地文件 → `file://` URL */
    fun saveFromBase64(context: Context, memoId: UUID, base64: String, userId: String? = null): String? {
        if (base64.isBlank()) return null
        val bytes = runCatching {
            Base64.decode(base64, Base64.DEFAULT)
        }.getOrNull() ?: return null
        if (bytes.isEmpty()) return null
        val out = thumbFile(context, memoId, userId)
        return runCatching {
            FileOutputStream(out).use { it.write(bytes) }
            "file://${out.absolutePath}"
        }.getOrNull()
    }

    /** 从本地视频抽首帧并落盘 */
    fun extractFromVideo(context: Context, memoId: UUID, videoPath: String?, userId: String? = null): String? {
        val path = V2MemoBridge.localVideoAbsolutePath(videoPath) ?: return null
        val file = File(path)
        if (!file.exists()) return null
        val existing = fileUrlForThumb(context, memoId, userId)
        if (existing != null) return existing
        val base64 = V2VideoThumbnailUtil.jpegBase64ForUpload(path, maxBytes = 500_000)
        if (base64.isBlank()) return null
        return saveFromBase64(context, memoId, base64, userId)
    }

    /** 在线视频封面猜测（YouTube 等） */
    fun onlineThumbnailGuess(playUrl: String?): String? {
        if (playUrl.isNullOrBlank()) return null
        return V2OnlineVideoMetadataFetcher.youtubeThumbnailUrl(playUrl)
    }

    fun resolveThumbnailUrl(context: Context, memo: V2Memo, userId: String? = null): String? {
        memo.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { url ->
            if (V2MemoBridge.isRemoteUrl(url)) return V2MemoBridge.coilImageModel(url)
            val path = V2MemoBridge.localVideoAbsolutePath(url) ?: return@let
            if (File(path).exists()) return V2MemoBridge.coilImageModel(url)
        }
        fileUrlForThumb(context, memo.id, userId)?.let { return it }
        if (userId != null) fileUrlForThumb(context, memo.id)?.let { return it }
        if (memo.isOnlineVideo) {
            onlineThumbnailGuess(memo.videoUrl)?.let { return it }
        }
        return null
    }

    /**
     * 云同步后补齐缺失封面：已有 thumb 文件 / 在线猜测 / 本地抽帧。
     * @return 更新后的 memo（thumbnailUrl 有变化时）
     */
    fun recoverThumbnail(context: Context, memo: V2Memo, userId: String? = null): V2Memo? {
        if (!memo.hasVideo) return null
        val current = resolveThumbnailUrl(context, memo, userId)
        if (current != null && memo.thumbnailUrl == current) return null
        if (current != null) return memo.copy(thumbnailUrl = current)

        val fromVideo = extractFromVideo(context, memo.id, memo.videoUrl, userId)
        if (fromVideo != null) return memo.copy(thumbnailUrl = fromVideo)

        val online = if (memo.isOnlineVideo) onlineThumbnailGuess(memo.videoUrl) else null
        if (online != null) return memo.copy(thumbnailUrl = online)

        val fetched = fetchOnlineThumbnail(memo.videoUrl, memo.platform)
        if (fetched != null) return memo.copy(thumbnailUrl = fetched)

        return null
    }

    private fun fetchOnlineThumbnail(playUrl: String?, platform: String?): String? {
        if (playUrl.isNullOrBlank()) return null
        return runCatching {
            runBlocking {
                val p = platform?.takeIf { it.isNotBlank() }
                    ?: V2OnlineVideoMetadataFetcher.detectPlatform(playUrl)
                V2OnlineVideoMetadataFetcher.fetch(playUrl, p).thumbnailUrl?.takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    private fun thumbDir(context: Context, userId: String?): File {
        val root = if (userId.isNullOrBlank()) {
            context.filesDir
        } else {
            File(context.filesDir, "users/$userId")
        }
        return File(root, THUMB_DIR).apply { mkdirs() }
    }

}
