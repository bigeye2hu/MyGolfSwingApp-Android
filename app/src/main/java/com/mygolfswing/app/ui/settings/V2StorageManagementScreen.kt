package com.mygolfswing.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.data.video.V2VideoThumbnailLoader
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/** 存储管理（对齐 iOS `StorageManagementView` 精简版） */
@Composable
fun V2StorageManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var videoBytes by remember { mutableLongStateOf(0L) }
    var cacheBytes by remember { mutableLongStateOf(0L) }
    var loading by remember { mutableStateOf(true) }
    var confirmClear by remember { mutableStateOf(false) }
    var clearedMsg by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            videoBytes = V2VideoFileStore.calculateUsedBytes(context)
            cacheBytes = calculateCacheBytes(context)
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        V2NavTopBarCentered(title = "存储管理", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(DSSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OverviewCard(
                total = formatBytes(videoBytes + cacheBytes),
                loading = loading,
                onRefresh = { refresh() },
            )
            StorageRow(label = "视频 Memo（本地）", size = formatBytes(videoBytes), hint = "含视频文件与缩略图")
            StorageRow(label = "应用缓存", size = formatBytes(cacheBytes), hint = "临时文件、图片缓存等")

            Spacer(Modifier.height(8.dp))
            Text(
                "清理缓存不会删除已保存的 Memo 记录；收藏和成长池中的挥杆视频会保留。",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )

            Button(
                onClick = { confirmClear = true },
                enabled = !loading && (videoBytes + cacheBytes) > 0,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DSColors.flagRed),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("清理视频与缓存", style = DSTypography.bodyBold, color = DSColors.warmWhite)
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("确认清理？") },
            text = { Text("将删除普通本地视频文件与应用缓存；收藏和成长池中的挥杆视频会保留。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch {
                        loading = true
                        val protectedMedia = V2SwingSessionStore.permanentMediaPaths()
                        val freedVideos = V2VideoFileStore.clearAll(context, protectedAbsolutePaths = protectedMedia)
                        val freedCache = clearAppCache(context)
                        V2VideoThumbnailLoader.clearMemoryCache()
                        videoBytes = V2VideoFileStore.calculateUsedBytes(context)
                        cacheBytes = calculateCacheBytes(context)
                        loading = false
                        clearedMsg = "已释放 ${formatBytes(freedVideos + freedCache)}"
                    }
                }) { Text("清理", color = DSColors.flagRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
            },
        )
    }

    clearedMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { clearedMsg = null },
            title = { Text("清理完成") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { clearedMsg = null }) { Text("好") }
            },
        )
    }
}

@Composable
private fun OverviewCard(total: String, loading: Boolean, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.cardWhite, RoundedCornerShape(20.dp))
            .padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("存储概览", style = DSTypography.titleMedium, color = DSColors.inkGreen, modifier = Modifier.weight(1f))
            TextButton(onClick = onRefresh) { Text("刷新", color = DSColors.fairway) }
        }
        if (loading) {
            CircularProgressIndicator(color = DSColors.fairway, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Text(total, style = DSTypography.display.copy(fontWeight = FontWeight.Bold), color = DSColors.fairway)
            Text("已占用空间", style = DSTypography.caption, color = DSColors.shadowGray)
        }
    }
}

@Composable
private fun StorageRow(label: String, size: String, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.cardWhite, RoundedCornerShape(16.dp))
            .padding(DSSpacing.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = DSTypography.bodyBold, color = DSColors.inkGreen, modifier = Modifier.weight(1f))
            Text(size, style = DSTypography.bodyBold, color = DSColors.fairway)
        }
        Text(hint, style = DSTypography.caption, color = DSColors.shadowGray, modifier = Modifier.padding(top = 4.dp))
    }
}

private fun calculateCacheBytes(context: android.content.Context): Long {
    fun dirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
    return dirSize(context.cacheDir) + dirSize(context.externalCacheDir)
}

private fun clearAppCache(context: android.content.Context): Long {
    fun clearDir(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var freed = 0L
        dir.listFiles()?.forEach { child ->
            if (child.name == "videos" && child.isDirectory) {
                child.listFiles()?.forEach { nested ->
                    freed += nested.deleteRecursivelyCountingBytes()
                }
            } else {
                freed += child.deleteRecursivelyCountingBytes()
            }
        }
        return freed
    }
    return clearDir(context.cacheDir) + clearDir(context.externalCacheDir)
}

private fun File.deleteRecursivelyCountingBytes(): Long {
    val bytes = walkTopDown().filter { it.isFile }.sumOf { it.length() }
    return if (runCatching { deleteRecursively() }.getOrDefault(false)) bytes else 0L
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 * 1024 -> String.format(Locale.ROOT, "%.1f GB", bytes / 1024.0 / 1024 / 1024)
    bytes >= 1024 * 1024 -> String.format(Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024)
    bytes >= 1024 -> String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
