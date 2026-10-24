package com.mygolfswing.app.ui.interaction

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun V2CellularUploadConfirmationDialog(
    fileSizeBytes: Long,
    busy: Boolean,
    sendMode: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val size = Formatter.formatFileSize(LocalContext.current, fileSizeBytes)
    V2WorkspaceConfirmationSheet(
        title = if (sendMode) "使用蜂窝网络发送？" else "使用蜂窝网络上传？",
        message = if (sendMode) {
            "这段视频约 $size，继续会使用手机流量；暂不发送不会丢失草稿。"
        } else {
            "这段视频约 $size，继续会使用手机流量。暂不上传不会丢失草稿，连接 Wi-Fi 后会自动继续。"
        },
        confirmLabel = if (sendMode) "继续发送" else "继续上传",
        busyLabel = "发送中…",
        cancelLabel = if (sendMode) "暂不发送" else "暂不上传",
        busy = busy,
        destructive = false,
        testTag = if (sendMode) "workspace-cellular-send-sheet" else "workspace-cellular-upload-sheet",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
