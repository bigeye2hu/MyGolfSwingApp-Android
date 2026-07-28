package com.mygolfswing.app.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.data.video.V2VideoThumbnailLoader
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/** 存储管理（对齐 iOS `StorageManagementView` 精简版） */
@Composable
fun V2StorageManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var videoBytes by remember { mutableLongStateOf(0L) }
    var temporaryVideoBytes by remember { mutableLongStateOf(0L) }
    var cacheBytes by remember { mutableLongStateOf(0L) }
    var loading by remember { mutableStateOf(true) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmClearCache by remember { mutableStateOf(false) }
    var clearedMsg by remember { mutableStateOf<String?>(null) }
    var failureMsg by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            try {
                videoBytes = V2VideoFileStore.calculateUsedBytes(context)
                temporaryVideoBytes = calculateTemporaryVideoBytes(context)
                cacheBytes = calculateCacheBytes(context)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                failureMsg = "存储信息刷新失败，请稍后重试"
            } finally {
                loading = false
            }
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
                .v2NavigationBarPadding()
                .padding(DSSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OverviewCard(
                total = formatBytes(videoBytes + temporaryVideoBytes + cacheBytes),
                videoSize = formatBytes(videoBytes),
                temporarySize = formatBytes(temporaryVideoBytes),
                cacheSize = formatBytes(cacheBytes),
                loading = loading,
                onRefresh = { refresh() },
            )
            Text("存储详情", style = DSTypography.bodyBold, color = DSColors.inkGreen)
            StorageRow(
                label = "视频 Memo（本地）",
                size = formatBytes(videoBytes),
                hint = "含视频文件与缩略图",
                color = DSColors.fairway,
                icon = Icons.Filled.VideoLibrary,
            )
            StorageRow(
                label = "临时拍摄文件",
                size = formatBytes(temporaryVideoBytes),
                hint = "拍摄中断或导入前的临时视频",
                color = DSColors.trophyGold,
                icon = Icons.Filled.Folder,
            )
            StorageRow(
                label = "应用缓存",
                size = formatBytes(cacheBytes),
                hint = "临时文件、图片缓存等",
                color = DSColors.flagRed,
                icon = Icons.Filled.CleaningServices,
                canClean = cacheBytes > 0 && !loading,
                onClear = { confirmClearCache = true },
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "仅清理应用缓存不会删除视频文件；清理普通视频会同时移除临时拍摄文件，收藏和成长池中的挥杆视频会保留。",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )

            CleanupActionButton(
                text = "仅清理应用缓存",
                icon = Icons.Filled.CleaningServices,
                enabled = !loading && cacheBytes > 0,
                gradient = Brush.horizontalGradient(listOf(DSColors.fairway, DSColors.deepGreen)),
                shadowColor = DSColors.fairway.copy(alpha = 0.22f),
                onClick = { confirmClearCache = true },
            )

            CleanupActionButton(
                text = "清理普通视频与缓存",
                icon = Icons.Filled.Delete,
                enabled = !loading && (videoBytes + temporaryVideoBytes + cacheBytes) > 0,
                gradient = Brush.horizontalGradient(listOf(DSColors.flagRed, Color(0xFF922D24))),
                shadowColor = DSColors.flagRed.copy(alpha = 0.25f),
                onClick = { confirmClear = true },
            )
        }
    }

    if (confirmClearCache) {
        AlertDialog(
            onDismissRequest = { confirmClearCache = false },
            title = { Text("确认清理缓存？") },
            text = { Text("将清理临时文件、图片缓存等；不会删除视频文件和已保存的 Memo 记录。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearCache = false
                    scope.launch {
                        loading = true
                        try {
                            val freedCache = clearAppCache(context)
                            V2VideoThumbnailLoader.clearMemoryCache()
                            videoBytes = V2VideoFileStore.calculateUsedBytes(context)
                            temporaryVideoBytes = calculateTemporaryVideoBytes(context)
                            cacheBytes = calculateCacheBytes(context)
                            clearedMsg = "已释放 ${formatBytes(freedCache)}"
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Throwable) {
                            failureMsg = "缓存清理失败，请稍后重试"
                        } finally {
                            loading = false
                        }
                    }
                }) { Text("清理", color = DSColors.flagRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearCache = false }) { Text("取消") }
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("确认清理普通视频？") },
            text = { Text("将删除未收藏且未进入成长池的本地视频文件、临时拍摄文件，并清理应用缓存；收藏和成长池中的挥杆视频会保留。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch {
                        loading = true
                        try {
                            val protectedMedia = V2SwingSessionStore.permanentMediaPaths()
                            val freedVideos = V2VideoFileStore.clearAll(context, protectedAbsolutePaths = protectedMedia)
                            val freedTemporaryVideos = clearTemporaryVideos(context)
                            val freedCache = clearAppCache(context)
                            V2VideoThumbnailLoader.clearMemoryCache()
                            videoBytes = V2VideoFileStore.calculateUsedBytes(context)
                            temporaryVideoBytes = calculateTemporaryVideoBytes(context)
                            cacheBytes = calculateCacheBytes(context)
                            clearedMsg = "已释放 ${formatBytes(freedVideos + freedTemporaryVideos + freedCache)}"
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Throwable) {
                            failureMsg = "清理失败，请稍后重试"
                        } finally {
                            loading = false
                        }
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

    failureMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { failureMsg = null },
            title = { Text("操作失败") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { failureMsg = null }) { Text("好", color = DSColors.fairway) }
            },
        )
    }
}

@Composable
private fun CleanupActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    gradient: Brush,
    shadowColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = shadowColor,
                spotColor = shadowColor,
            )
            .clip(RoundedCornerShape(14.dp))
            .background(gradient)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        Text(
            text,
            style = DSTypography.bodyBold,
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun OverviewCard(
    total: String,
    videoSize: String,
    temporarySize: String,
    cacheSize: String,
    loading: Boolean,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.06f),
                spotColor = DSColors.fairway.copy(alpha = 0.06f),
            )
            .background(DSColors.cardWhite, RoundedCornerShape(16.dp))
            .padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(DSSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("存储概览", style = DSTypography.titleMedium, color = DSColors.inkGreen, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRefresh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "刷新", tint = DSColors.fairway, modifier = Modifier.size(17.dp))
            }
        }
        if (loading) {
            Row(
                modifier = Modifier.height(80.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(color = DSColors.fairway, modifier = Modifier.size(24.dp))
                Text("正在计算…", style = DSTypography.body, color = DSColors.shadowGray, modifier = Modifier.padding(start = 8.dp))
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(DSSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StorageRing(total = total)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StorageBadge("视频 Memo", videoSize, DSColors.fairway)
                    StorageBadge("临时文件", temporarySize, DSColors.trophyGold)
                    StorageBadge("应用缓存", cacheSize, DSColors.flagRed)
                }
            }
        }
    }
}

@Composable
private fun StorageRing(total: String) {
    Box(
        modifier = Modifier.size(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(100.dp)) {
            val strokeWidth = 12.dp.toPx()
            val inset = strokeWidth / 2
            drawArc(
                color = DSColors.mistGreen,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.linearGradient(listOf(DSColors.fairway, DSColors.deepGreen)),
                startAngle = -90f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(total, style = DSTypography.bodyBold, color = DSColors.inkGreen)
            Text("已使用", style = DSTypography.caption, color = DSColors.shadowGray)
        }
    }
}

@Composable
private fun StorageBadge(label: String, size: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(label, style = DSTypography.caption, color = DSColors.shadowGray, modifier = Modifier.weight(1f))
        Text(size, style = DSTypography.caption.copy(fontWeight = FontWeight.SemiBold), color = DSColors.inkGreen)
    }
}

@Composable
private fun StorageRow(
    label: String,
    size: String,
    hint: String,
    color: Color,
    icon: ImageVector,
    canClean: Boolean = false,
    onClear: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.cardWhite, RoundedCornerShape(14.dp))
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = DSTypography.bodyBold, color = DSColors.inkGreen)
            Text(hint, style = DSTypography.caption, color = DSColors.shadowGray)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(size, style = DSTypography.bodyBold, color = DSColors.inkGreen)
            if (canClean) {
                Text(
                    "清理",
                    style = DSTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = DSColors.flagRed,
                    modifier = Modifier.clickable(onClick = onClear),
                )
            }
        }
    }
}

private fun calculateCacheBytes(context: android.content.Context): Long {
    fun dirSizeExcludingTemporaryVideos(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        val temporaryVideosDir = File(dir, "videos")
            .takeIf { it.isDirectory }
            ?.canonicalPathOrNull()
        return dir.walkTopDown()
            .filter { it.isFile }
            .filter { file ->
                val path = file.canonicalPathOrNull()
                temporaryVideosDir == null || path == null || !path.startsWith("$temporaryVideosDir${File.separator}")
            }
            .sumOf { it.length() }
    }
    return dirSizeExcludingTemporaryVideos(context.cacheDir) + dirSizeExcludingTemporaryVideos(context.externalCacheDir)
}

private fun calculateTemporaryVideoBytes(context: android.content.Context): Long =
    listOf(File(context.cacheDir, "videos"), context.externalCacheDir?.let { File(it, "videos") })
        .sumOf { it?.dirSizeBytes() ?: 0L }

private fun clearTemporaryVideos(context: android.content.Context): Long =
    listOf(File(context.cacheDir, "videos"), context.externalCacheDir?.let { File(it, "videos") })
        .sumOf { it?.clearDirContentsCountingBytes() ?: 0L }

private fun File.dirSizeBytes(): Long {
    if (!exists()) return 0L
    return walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

private fun clearAppCache(context: android.content.Context): Long {
    fun clearDir(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var freed = 0L
        dir.listFiles()?.forEach { child ->
            if (child.name != "videos" || !child.isDirectory) {
                freed += child.deleteRecursivelyCountingBytes()
            }
        }
        return freed
    }
    return clearDir(context.cacheDir) + clearDir(context.externalCacheDir)
}

private fun File.clearDirContentsCountingBytes(): Long {
    if (!exists() || !isDirectory) return 0L
    return listFiles()?.sumOf { it.deleteRecursivelyCountingBytes() } ?: 0L
}

private fun File.deleteRecursivelyCountingBytes(): Long {
    val bytes = walkTopDown().filter { it.isFile }.sumOf { it.length() }
    return if (runCatching { deleteRecursively() }.getOrDefault(false)) bytes else 0L
}

private fun File.canonicalPathOrNull(): String? = runCatching { canonicalPath }.getOrNull()

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 * 1024 -> String.format(Locale.ROOT, "%.1f GB", bytes / 1024.0 / 1024 / 1024)
    bytes >= 1024 * 1024 -> String.format(Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024)
    bytes >= 1024 -> String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
