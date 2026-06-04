package com.mygolfswing.app.ui.memo

import com.mygolfswing.app.data.memo.V2MemoBridge
import java.io.File

internal fun memoVideoPlayable(videoUrl: String?): Boolean {
    val trimmed = videoUrl?.trim()?.takeIf { it.isNotBlank() } ?: return false
    if (V2MemoBridge.isRemoteUrl(trimmed)) return true
    val localPath = V2MemoBridge.localVideoAbsolutePath(trimmed) ?: return false
    val file = File(localPath)
    return file.exists() && file.isFile
}
