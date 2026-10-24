package com.mygolfswing.app.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

internal const val V2_CLIPBOARD_COPY_FAILURE = "复制失败，请稍后重试"

internal fun attemptClipboardCopy(write: () -> Unit): Boolean = runCatching(write).isSuccess

internal fun Context.copyPlainTextToClipboard(
    label: String,
    value: String,
    successMessage: String = "已复制",
    writeOverride: ((ClipData) -> Unit)? = null,
): Boolean {
    if (value.isBlank()) return false
    val data = ClipData.newPlainText(label, value)
    val copied = attemptClipboardCopy {
        if (writeOverride != null) {
            writeOverride(data)
        } else {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: error("Clipboard service unavailable")
            clipboard.setPrimaryClip(data)
        }
    }
    Toast.makeText(
        this,
        if (copied) successMessage else V2_CLIPBOARD_COPY_FAILURE,
        Toast.LENGTH_SHORT,
    ).show()
    return copied
}
