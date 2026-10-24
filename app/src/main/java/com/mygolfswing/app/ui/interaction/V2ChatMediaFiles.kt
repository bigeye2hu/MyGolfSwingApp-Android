package com.mygolfswing.app.ui.interaction

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.ceil

internal const val V2_CHAT_MAX_FILE_BYTES = 1_000_000_000L

internal enum class V2PreparedChatMediaKind { IMAGE, VIDEO, FILE }

internal data class V2PreparedChatMedia(
    val file: File,
    val kind: V2PreparedChatMediaKind,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationSeconds: Int? = null,
    val snapshotFile: File? = null,
)

internal object V2ChatMediaFiles {
    suspend fun prepare(
        context: Context,
        uri: Uri,
        forceFile: Boolean = false,
    ): V2PreparedChatMedia = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val declaredMime = resolver.getType(uri).orEmpty()
        val originalName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) null else {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
                    if (size != null && size > V2_CHAT_MAX_FILE_BYTES) error("文件不能超过1GB")
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                }
            }
            ?.takeIf(String::isNotBlank)
            ?: "聊天文件"
        val kind = when {
            forceFile -> V2PreparedChatMediaKind.FILE
            declaredMime.startsWith("image/") -> V2PreparedChatMediaKind.IMAGE
            declaredMime.startsWith("video/") -> V2PreparedChatMediaKind.VIDEO
            else -> V2PreparedChatMediaKind.FILE
        }
        val suffix = safeSuffix(originalName, declaredMime, kind)
        val root = File(context.cacheDir, "chat_media").apply { mkdirs() }
        val target = File(root, "${kind.name.lowercase()}_${UUID.randomUUID()}$suffix")
        try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > V2_CHAT_MAX_FILE_BYTES) error("文件不能超过1GB")
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error("无法读取选中的文件")
            require(target.length() > 0L) { "选中的文件为空" }
            inspectLocalFile(target, kind, originalName, declaredMime)
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    suspend fun inspectCapture(
        file: File,
        kind: V2PreparedChatMediaKind,
        mimeType: String,
    ): V2PreparedChatMedia = withContext(Dispatchers.IO) {
        require(file.isFile && file.length() > 0L) { "没有获取到拍摄内容" }
        require(file.length() <= V2_CHAT_MAX_FILE_BYTES) { "文件不能超过1GB" }
        inspectLocalFile(file, kind, file.name, mimeType)
    }

    fun createCaptureFile(context: Context, kind: V2PreparedChatMediaKind): File {
        val root = File(context.cacheDir, "chat_media").apply { mkdirs() }
        val suffix = if (kind == V2PreparedChatMediaKind.IMAGE) ".jpg" else ".mp4"
        return File(root, "capture_${UUID.randomUUID()}$suffix")
    }

    private fun inspectLocalFile(
        target: File,
        kind: V2PreparedChatMediaKind,
        displayName: String,
        mimeType: String,
    ): V2PreparedChatMedia {
        if (kind != V2PreparedChatMediaKind.VIDEO) {
            return V2PreparedChatMedia(target, kind, displayName, mimeType, target.length())
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(target.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            require(durationMs > 0L) { "无法读取视频时长" }
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: error("无法生成视频封面")
            val snapshot = File(target.parentFile, "snapshot_${UUID.randomUUID()}.jpg")
            try {
                FileOutputStream(snapshot).use { output ->
                    require(frame.compress(Bitmap.CompressFormat.JPEG, 82, output)) { "无法保存视频封面" }
                }
            } catch (error: Throwable) {
                snapshot.delete()
                throw error
            } finally {
                frame.recycle()
            }
            V2PreparedChatMedia(
                file = target,
                kind = kind,
                displayName = displayName,
                mimeType = mimeType.ifBlank { "video/mp4" },
                sizeBytes = target.length(),
                durationSeconds = ceil(durationMs / 1_000.0).toInt().coerceAtLeast(1),
                snapshotFile = snapshot,
            )
        } finally {
            retriever.release()
        }
    }

    private fun safeSuffix(
        displayName: String,
        mimeType: String,
        kind: V2PreparedChatMediaKind,
    ): String {
        val fromName = displayName.substringAfterLast('.', "").lowercase()
            .takeIf { it.matches(Regex("[a-z0-9]{1,10}")) }
        val extension = fromName ?: when {
            kind == V2PreparedChatMediaKind.IMAGE && mimeType == "image/png" -> "png"
            kind == V2PreparedChatMediaKind.IMAGE -> "jpg"
            kind == V2PreparedChatMediaKind.VIDEO -> "mp4"
            else -> "bin"
        }
        return ".$extension"
    }
}
