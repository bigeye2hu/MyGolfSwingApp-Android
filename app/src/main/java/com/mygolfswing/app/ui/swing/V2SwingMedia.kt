package com.mygolfswing.app.ui.swing

import com.mygolfswing.app.data.memo.V2MemoBridge
import java.io.File

internal fun swingMediaImageModel(pathOrUrl: String?): String? {
    val trimmed = pathOrUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (V2MemoBridge.isRemoteUrl(trimmed)) return trimmed
    val localPath = V2MemoBridge.localVideoAbsolutePath(trimmed) ?: return null
    val file = File(localPath)
    return if (file.exists() && file.isFile) "file://${file.absolutePath}" else null
}

internal fun swingClipPreviewModel(thumbnailPath: String?, videoPath: String): String? =
    swingMediaImageModel(thumbnailPath) ?: swingMediaImageModel(videoPath)
