package com.mygolfswing.app.ui.settings

import android.content.Context
import android.os.StatFs
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.video.V2VideoThumbnailLoader
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale

/** 存储管理（对齐 iOS `StorageManagementView`）。 */
@Composable
internal fun V2StorageManagementScreen(
    onBack: () -> Unit,
    operations: StorageManagementOperations? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resolvedOperations = operations ?: remember(context) {
        StorageManagementOperations(
            calculate = { calculateStorageSnapshot(context) },
            clearCategory = { clearStorageCategory(context, it) },
            clearAllCaches = { clearAllCacheCategories(context) },
            clearMemoryThumbnails = V2VideoThumbnailLoader::clearMemoryCache,
        )
    }
    var snapshot by remember { mutableStateOf(StorageSnapshot()) }
    var loading by remember { mutableStateOf(true) }
    var clearingKind by remember { mutableStateOf<StorageCategory?>(null) }
    var confirmKind by remember { mutableStateOf<StorageCategory?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showClearSuccess by remember { mutableStateOf(false) }
    var failureMsg by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            try {
                snapshot = resolvedOperations.calculate()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                failureMsg = storageOperationFailureMessage(StorageOperationErrorKind.REFRESH)
            } finally {
                loading = false
            }
        }
    }

    fun clear(kind: StorageCategory) {
        scope.launch {
            clearingKind = kind
            try {
                resolvedOperations.clearCategory(kind)
                if (kind == StorageCategory.THUMBNAILS) resolvedOperations.clearMemoryThumbnails()
                snapshot = resolvedOperations.calculate()
                showClearSuccess = true
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                failureMsg = storageOperationFailureMessage(
                    if (kind == StorageCategory.VIDEOS) StorageOperationErrorKind.VIDEO_CLEAR
                    else StorageOperationErrorKind.CACHE_CLEAR,
                )
            } finally {
                clearingKind = null
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            StorageTopBar(
                onClose = onBack,
                onRefresh = ::refresh,
                refreshEnabled = !loading && clearingKind == null,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .v2NavigationBarPadding()
                    .padding(horizontal = DSSpacing.lg)
                    .padding(top = DSSpacing.md),
                verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
            ) {
                StorageOverviewCard(snapshot = snapshot, loading = loading)
                StorageDetailsSection(
                    items = snapshot.items,
                    loading = loading,
                    clearingKind = clearingKind,
                    onClear = { confirmKind = it },
                )
                ClearAllCacheButton(
                    enabled = !loading && clearingKind == null,
                    onClick = { showClearAllConfirm = true },
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    confirmKind?.let { kind ->
        AlertDialog(
            onDismissRequest = { confirmKind = null },
            title = { Text("确认清理") },
            text = { Text(storageClearConfirmMessage(kind)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmKind = null
                        clearingKind = kind
                        scope.launch {
                            delay(STORAGE_DIALOG_TRANSITION_MILLIS)
                            clear(kind)
                        }
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "确认清理${storageCategoryLabel(kind)}"
                    },
                ) { Text("清理", color = DSColors.flagRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmKind = null }) { Text("取消") }
            },
        )
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("确认清理所有缓存") },
            text = { Text("将清理缩略图、临时文件、头像缓存等。\n不会删除视频文件和AI模型。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllConfirm = false
                        clearingKind = StorageCategory.OTHER
                        scope.launch {
                            delay(STORAGE_DIALOG_TRANSITION_MILLIS)
                            try {
                                resolvedOperations.clearAllCaches()
                                resolvedOperations.clearMemoryThumbnails()
                                snapshot = resolvedOperations.calculate()
                                showClearSuccess = true
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Throwable) {
                                failureMsg = storageOperationFailureMessage(StorageOperationErrorKind.CACHE_CLEAR)
                            } finally {
                                clearingKind = null
                            }
                        }
                    },
                    modifier = Modifier.semantics { contentDescription = "确认清理所有缓存" },
                ) { Text("清理", color = DSColors.flagRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) { Text("取消") }
            },
        )
    }

    if (showClearSuccess) {
        AlertDialog(
            onDismissRequest = { showClearSuccess = false },
            title = { Text("清理完成") },
            text = { Text("存储空间已释放") },
            confirmButton = {
                TextButton(onClick = { showClearSuccess = false }) { Text("好的") }
            },
        )
    }

    failureMsg?.let { message ->
        AlertDialog(
            onDismissRequest = { failureMsg = null },
            title = { Text("操作失败") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { failureMsg = null }) { Text("好", color = DSColors.fairway) }
            },
        )
    }
}

@Composable
private fun StorageTopBar(
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    refreshEnabled: Boolean,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .height(44.dp)
            .testTag("storage-management-top-bar"),
    ) {
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
            Text("关闭", color = DSColors.fairway, fontWeight = FontWeight.Medium)
        }
        Text(
            "存储管理",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = DSColors.inkGreen,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(48.dp)
                .clickable(enabled = refreshEnabled, onClick = onRefresh)
                .alpha(if (refreshEnabled) 1f else 0.45f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "刷新存储信息",
                tint = DSColors.fairway,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun StorageOverviewCard(snapshot: StorageSnapshot, loading: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.06f),
                spotColor = DSColors.fairway.copy(alpha = 0.06f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(DSSpacing.md),
    ) {
        Text("存储概览", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
        if (loading) {
            Row(Modifier.height(80.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = DSColors.fairway, modifier = Modifier.size(24.dp))
                Text("正在计算…", color = DSColors.shadowGray, modifier = Modifier.padding(start = 8.dp))
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(DSSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StorageRing(formatBytes(snapshot.totalBytes), snapshot.ringFraction)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshot.items.take(3).forEach { item ->
                        val visual = storageVisual(item.kind)
                        StorageBadge(visual.label, formatBytes(item.bytes), visual.color)
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageDetailsSection(
    items: List<StorageItemSnapshot>,
    loading: Boolean,
    clearingKind: StorageCategory?,
    onClear: (StorageCategory) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DSSpacing.sm)) {
        Text("存储详情", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
        if (loading) {
            repeat(4) { StoragePlaceholderRow() }
        } else {
            items.forEach { item ->
                StorageItemRow(
                    item = item,
                    isClearing = clearingKind == item.kind,
                    enabled = clearingKind == null,
                    onClear = { onClear(item.kind) },
                )
            }
        }
    }
}

@Composable
private fun StorageItemRow(
    item: StorageItemSnapshot,
    isClearing: Boolean,
    enabled: Boolean,
    onClear: () -> Unit,
) {
    val visual = storageVisual(item.kind)
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DSColors.cardWhite)
                .border(1.dp, DSColors.borderLine, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(visual.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(visual.icon, null, tint = visual.color, modifier = Modifier.size(17.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(visual.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
                Text("${item.fileCount} 个文件", fontSize = 12.sp, color = DSColors.shadowGray, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatBytes(item.bytes), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
                if (item.bytes > 0L) {
                    Text(
                        "清理",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DSColors.flagRed,
                        modifier = Modifier
                            .semantics { contentDescription = "清理${visual.label}" }
                            .clickable(enabled = enabled, onClick = onClear),
                    )
                }
            }
        }
        if (isClearing) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun StoragePlaceholderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.cardWhite)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(DSColors.mistGreen))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(width = 80.dp, height = 12.dp).clip(RoundedCornerShape(4.dp)).background(DSColors.mistGreen))
            Box(Modifier.size(width = 120.dp, height = 10.dp).clip(RoundedCornerShape(4.dp)).background(DSColors.mistGreen.copy(alpha = 0.6f)))
        }
        Box(Modifier.size(width = 50.dp, height = 12.dp).clip(RoundedCornerShape(4.dp)).background(DSColors.mistGreen))
    }
}

@Composable
private fun ClearAllCacheButton(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = DSColors.flagRed.copy(alpha = 0.25f),
                spotColor = DSColors.flagRed.copy(alpha = 0.25f),
            )
            .alpha(if (enabled) 1f else 0.5f),
        enabled = enabled,
        shape = shape,
        color = DSColors.flagRed,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(DSColors.flagRed, Color(0xFF922D24)))),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Delete, null, tint = Color.White, modifier = Modifier.size(15.dp))
                Text(
                    "清理所有缓存",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StorageRing(total: String, usageFraction: Float) {
    Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(100.dp)) {
            val strokeWidth = 12.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                DSColors.mistGreen,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
            if (usageFraction > 0f) {
                drawArc(
                    brush = Brush.linearGradient(listOf(DSColors.fairway, DSColors.deepGreen)),
                    startAngle = -90f,
                    sweepAngle = 360f * usageFraction.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(total, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
            Text("已使用", fontSize = 11.sp, color = DSColors.shadowGray)
        }
    }
}

@Composable
private fun StorageBadge(label: String, size: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 13.sp, color = DSColors.shadowGray, modifier = Modifier.weight(1f))
        Text(size, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
    }
}

private data class StorageVisual(val label: String, val color: Color, val icon: ImageVector)

private fun storageVisual(kind: StorageCategory): StorageVisual = when (kind) {
    StorageCategory.VIDEOS -> StorageVisual("视频文件", Color(0xFF8B5CF6), Icons.Filled.VideoLibrary)
    StorageCategory.THUMBNAILS -> StorageVisual("缩略图缓存", Color(0xFF3B82F6), Icons.Filled.Image)
    StorageCategory.LOCAL_LLM -> StorageVisual("本地AI模型", Color(0xFFF59E0B), Icons.Filled.Memory)
    StorageCategory.TEMP_FILES -> StorageVisual("临时文件", Color(0xFFEF4444), Icons.Filled.Description)
    StorageCategory.AVATAR_CACHE -> StorageVisual("头像缓存", Color(0xFF10B981), Icons.Filled.AccountCircle)
    StorageCategory.OTHER -> StorageVisual("其他缓存", Color(0xFF6B7280), Icons.Filled.Folder)
}

internal enum class StorageCategory {
    VIDEOS,
    THUMBNAILS,
    LOCAL_LLM,
    TEMP_FILES,
    AVATAR_CACHE,
    OTHER,
}

internal data class StorageItemSnapshot(
    val kind: StorageCategory,
    val bytes: Long,
    val fileCount: Int,
)

internal data class StorageSnapshot(
    val totalBytes: Long = 0L,
    val deviceTotalBytes: Long = 0L,
    val items: List<StorageItemSnapshot> = StorageCategory.entries.map {
        StorageItemSnapshot(it, 0L, 0)
    },
) {
    val ringFraction: Float
        get() {
            if (deviceTotalBytes <= 0L) return 0f
            return ((totalBytes.toDouble() / deviceTotalBytes.toDouble()) * 10.0)
                .coerceIn(0.0, 1.0)
                .toFloat()
        }
}

internal data class StorageManagementOperations(
    val calculate: suspend () -> StorageSnapshot,
    val clearCategory: suspend (StorageCategory) -> Unit,
    val clearAllCaches: suspend () -> Unit,
    val clearMemoryThumbnails: () -> Unit = {},
)

internal data class StorageFileMetrics(val bytes: Long = 0L, val fileCount: Int = 0)

private suspend fun calculateStorageSnapshot(context: Context): StorageSnapshot = withContext(Dispatchers.IO) {
    calculateStorageSnapshot(
        filesDir = context.filesDir,
        cacheDir = context.cacheDir,
        externalCacheDir = context.externalCacheDir,
        userId = ServiceLocator.session.current().userId,
        deviceTotalBytes = runCatching { StatFs(context.filesDir.absolutePath).totalBytes }.getOrDefault(0L),
    )
}

internal fun calculateStorageSnapshot(
    filesDir: File,
    cacheDir: File?,
    externalCacheDir: File?,
    userId: String?,
    deviceTotalBytes: Long,
): StorageSnapshot {
    val roots = storageRoots(filesDir, cacheDir, externalCacheDir, userId)
    val items = StorageCategory.entries.map { kind ->
        val metrics = if (kind == StorageCategory.OTHER) {
            metricsExcluding(roots.cacheRoots, roots.explicitCacheRoots)
        } else {
            metricsFor(roots.byCategory.getValue(kind))
        }
        StorageItemSnapshot(kind, metrics.bytes, metrics.fileCount)
    }.sortedByDescending(StorageItemSnapshot::bytes)
    return StorageSnapshot(items.sumOf(StorageItemSnapshot::bytes), deviceTotalBytes, items)
}

private suspend fun clearStorageCategory(context: Context, kind: StorageCategory): Long = withContext(Dispatchers.IO) {
    clearStorageCategory(context.filesDir, context.cacheDir, context.externalCacheDir, ServiceLocator.session.current().userId, kind)
}

internal fun clearStorageCategory(
    filesDir: File,
    cacheDir: File?,
    externalCacheDir: File?,
    userId: String?,
    kind: StorageCategory,
): Long {
    val roots = storageRoots(filesDir, cacheDir, externalCacheDir, userId)
    return when (kind) {
        StorageCategory.OTHER -> clearFilesExcluding(roots.cacheRoots, roots.explicitCacheRoots)
        StorageCategory.TEMP_FILES -> clearDirectories(roots.byCategory.getValue(kind), strict = false)
        else -> clearDirectories(roots.byCategory.getValue(kind), strict = true)
    }
}

private suspend fun clearAllCacheCategories(context: Context): Long = withContext(Dispatchers.IO) {
    clearAllCacheCategories(context.filesDir, context.cacheDir, context.externalCacheDir, ServiceLocator.session.current().userId)
}

internal fun clearAllCacheCategories(
    filesDir: File,
    cacheDir: File?,
    externalCacheDir: File?,
    userId: String?,
): Long = listOf(
    StorageCategory.THUMBNAILS,
    StorageCategory.TEMP_FILES,
    StorageCategory.AVATAR_CACHE,
    StorageCategory.OTHER,
).sumOf { clearStorageCategory(filesDir, cacheDir, externalCacheDir, userId, it) }

private data class StorageRoots(
    val byCategory: Map<StorageCategory, List<File>>,
    val cacheRoots: List<File>,
    val explicitCacheRoots: List<File>,
)

private fun storageRoots(
    filesDir: File,
    cacheDir: File?,
    externalCacheDir: File?,
    userId: String?,
): StorageRoots {
    val userRoot = userId?.takeIf(String::isNotBlank)?.let { File(filesDir, "users/$it") }
    val cacheRoots = listOfNotNull(cacheDir, externalCacheDir).distinctBy(File::storagePathKey)
    fun cacheChildren(vararg names: String): List<File> = cacheRoots.flatMap { root -> names.map { File(root, it) } }

    val thumbnails = listOfNotNull(userRoot?.let { File(it, "thumbs") }) +
        cacheChildren("Thumbnails", "GolfCamThumbnails", "thumbnails")
    val tempFiles = cacheChildren("videos")
    val avatarCache = cacheChildren("AvatarCache", "avatar_cache")
    val byCategory = mapOf(
        StorageCategory.VIDEOS to listOfNotNull(userRoot?.let { File(it, "videos") }),
        StorageCategory.THUMBNAILS to thumbnails,
        StorageCategory.LOCAL_LLM to listOf(File(filesDir, "LocalLLM"), File(filesDir, "local_llm"), File(filesDir, "models")),
        StorageCategory.TEMP_FILES to tempFiles,
        StorageCategory.AVATAR_CACHE to avatarCache,
        StorageCategory.OTHER to emptyList(),
    )
    return StorageRoots(
        byCategory = byCategory,
        cacheRoots = cacheRoots,
        explicitCacheRoots = (thumbnails + tempFiles + avatarCache).distinctBy(File::storagePathKey),
    )
}

private fun metricsFor(roots: List<File>): StorageFileMetrics {
    var bytes = 0L
    var count = 0
    roots.distinctBy(File::storagePathKey).forEach { root ->
        if (!root.exists()) return@forEach
        root.walkTopDown().filter(File::isFile).forEach { file ->
            bytes += file.length().coerceAtLeast(0L)
            count += 1
        }
    }
    return StorageFileMetrics(bytes, count)
}

private fun metricsExcluding(cacheRoots: List<File>, excludedRoots: List<File>): StorageFileMetrics {
    val excluded = excludedRoots.mapNotNull(File::canonicalPathOrNull)
    var bytes = 0L
    var count = 0
    cacheRoots.forEach { root ->
        if (!root.exists()) return@forEach
        root.walkTopDown().filter(File::isFile).forEach { file ->
            val path = file.canonicalPathOrNull()
            if (path == null || excluded.none { path == it || path.startsWith("$it${File.separator}") }) {
                bytes += file.length().coerceAtLeast(0L)
                count += 1
            }
        }
    }
    return StorageFileMetrics(bytes, count)
}

private fun clearDirectories(roots: List<File>, strict: Boolean): Long = roots.distinctBy(File::storagePathKey).sumOf { root ->
    if (!root.exists() || !root.isDirectory) return@sumOf 0L
    val children = root.listFiles()
    if (children == null) {
        if (strict) throw IOException("Unable to enumerate storage directory")
        return@sumOf 0L
    }
    children.sumOf { it.deleteRecursivelyCountingBytes(strict) }
}

private fun clearFilesExcluding(cacheRoots: List<File>, excludedRoots: List<File>): Long {
    val excluded = excludedRoots.mapNotNull(File::canonicalPathOrNull)
    var freed = 0L
    cacheRoots.forEach outer@{ root ->
        if (!root.exists()) return@outer
        root.walkBottomUp().forEach inner@{ file ->
            if (file == root) return@inner
            val path = file.canonicalPathOrNull()
            val isExcluded = path != null && excluded.any { path == it || path.startsWith("$it${File.separator}") }
            if (!isExcluded) {
                if (file.isFile) {
                    val bytes = file.length().coerceAtLeast(0L)
                    if (runCatching { file.delete() }.getOrDefault(false)) freed += bytes
                } else if (file.isDirectory && file.list()?.isEmpty() == true) {
                    runCatching { file.delete() }
                }
            }
        }
    }
    return freed
}

private fun File.deleteRecursivelyCountingBytes(strict: Boolean): Long {
    val bytes = if (exists()) walkTopDown().filter(File::isFile).sumOf { it.length().coerceAtLeast(0L) } else 0L
    val deleted = runCatching { deleteRecursively() }.getOrDefault(false)
    if (!deleted && exists() && strict) throw IOException("Unable to delete storage item")
    return if (deleted) bytes else 0L
}

private fun File.canonicalPathOrNull(): String? = runCatching { canonicalPath }.getOrNull()

private fun File.storagePathKey(): String =
    (canonicalPathOrNull() ?: absolutePath).lowercase(Locale.ROOT)

internal fun storageClearConfirmMessage(kind: StorageCategory): String {
    val label = storageCategoryLabel(kind)
    val base = "确定要清理${label}吗？"
    return when (kind) {
        StorageCategory.VIDEOS -> "$base\n⚠️ 这将删除所有下载的视频文件！"
        StorageCategory.LOCAL_LLM -> "$base\n⚠️ 这将删除本地AI模型，需要重新下载！"
        else -> "$base\n此操作不可恢复。"
    }
}

private fun storageCategoryLabel(kind: StorageCategory): String = when (kind) {
    StorageCategory.VIDEOS -> "视频文件"
    StorageCategory.THUMBNAILS -> "缩略图缓存"
    StorageCategory.LOCAL_LLM -> "本地AI模型"
    StorageCategory.TEMP_FILES -> "临时文件"
    StorageCategory.AVATAR_CACHE -> "头像缓存"
    StorageCategory.OTHER -> "其他缓存"
}

internal enum class StorageOperationErrorKind { REFRESH, CACHE_CLEAR, VIDEO_CLEAR }

internal fun storageOperationFailureMessage(kind: StorageOperationErrorKind): String = when (kind) {
    StorageOperationErrorKind.REFRESH -> "存储信息刷新失败，请稍后重试"
    StorageOperationErrorKind.CACHE_CLEAR -> "缓存清理失败，请稍后重试"
    StorageOperationErrorKind.VIDEO_CLEAR -> "清理失败，请稍后重试"
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> String.format(Locale.ROOT, "%.1f GB", bytes / 1024.0 / 1024 / 1024)
    bytes >= 1024L * 1024L -> String.format(Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024)
    bytes >= 1024L -> String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

private const val STORAGE_DIALOG_TRANSITION_MILLIS = 220L
