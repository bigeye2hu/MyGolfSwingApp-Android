package com.mygolfswing.app.data.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.ByteArrayOutputStream
import java.io.File

/** 视频缩略图工具（对齐 iOS 上传前 JPEG Base64，≤100KB） */
object V2VideoThumbnailUtil {

    fun jpegBase64ForUpload(path: String?, maxBytes: Int = 100_000): String {
        if (path.isNullOrBlank()) return ""
        val filePath = path.removePrefix("file://")
        val file = File(filePath)
        if (!file.exists()) return ""
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val frame = retriever.getFrameAtTime(0) ?: return ""
            compressToBase64(frame, maxBytes)
        } catch (_: Exception) {
            ""
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun compressToBase64(bitmap: Bitmap, maxBytes: Int): String {
        var quality = 85
        var bytes: ByteArray
        do {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            bytes = stream.toByteArray()
            quality -= 10
        } while (bytes.size > maxBytes && quality > 30)
        if (bytes.size > maxBytes) return ""
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }
}
