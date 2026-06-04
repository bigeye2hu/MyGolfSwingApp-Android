package com.mygolfswing.app.ui.memo

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.store.V2GoalStore
import java.util.UUID
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.store.V2CategoryStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.video.V2OnlineVideoMetadataFetcher
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.data.video.deleteFileCountingFreedBytes
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * 视频 Memo 创建页（对齐 iOS `V2MemoCreateVideoView`，3 个视频源）。
 *
 * 视频源（互斥三选一）：
 * - 拍视频 → 系统相机 `CaptureVideo` → 录完移到 App 私有目录 + 抽缩略图
 * - 选相册 → `PickVisualMedia.VideoOnly` → 复制到 App 私有目录 + 抽缩略图
 * - 在线视频 → 弹出"粘贴 URL"对话框（http/https）
 *
 * 选好后立即在卡片中央用 ExoPlayer 做小预览，可点击全屏播放。
 */
@OptIn(UnstableApi::class)
@Composable
fun V2MemoCreateVideoScreen(
    onClose: () -> Unit = {},
    onOpenOnlinePicker: (() -> Unit)? = null,
    pendingOnline: V2OnlineVideoSelection? = null,
    onPendingOnlineConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 视频源状态
    var sourceKind by remember { mutableStateOf<SourceKind?>(null) }
    var localAbsolutePath by remember { mutableStateOf<String?>(null) }
    var localThumbPath by remember { mutableStateOf<String?>(null) }
    var localDurationSec by remember { mutableStateOf<Double?>(null) }
    var onlineUrl by remember { mutableStateOf<String?>(null) }
    var onlinePlatform by remember { mutableStateOf<String?>(null) }
    var onlineThumbUrl by remember { mutableStateOf<String?>(null) }
    var showOnlineDialog by remember { mutableStateOf(false) }
    var selectedTagIds by remember { mutableStateOf(setOf<String>()) }
    var selectedGoalIds by remember { mutableStateOf(setOf<UUID>()) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val activeGoals by V2GoalStore.goals.collectAsState()

    var importing by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    // 系统相机
    val captureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo(),
    ) { ok ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (ok && file != null) {
            scope.launch {
                importing = true
                val r = V2VideoFileStore.adoptLocalFile(context, file)
                importing = false
                if (r == null) {
                    deleteFileCountingFreedBytes(file)
                    importError = "录制失败，请重试"
                } else {
                    sourceKind = SourceKind.CAMERA
                    localAbsolutePath = r.absolutePath
                    localThumbPath = r.thumbnailPath
                    localDurationSec = r.durationSec
                    onlineUrl = null
                }
            }
        } else {
            deleteFileCountingFreedBytes(file)
        }
    }

    fun launchCameraCapture() {
        val (file, uri) = V2VideoFileStore.createCameraOutputUri(context)
        pendingCameraFile = file
        captureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) launchCameraCapture()
        else importError = "需要相机权限才能录制视频"
    }

    // 表单状态
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var noteExpanded by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(pendingOnline) {
        val pick = pendingOnline ?: return@LaunchedEffect
        onlineUrl = pick.url
        onlinePlatform = pick.platform
        onlineThumbUrl = pick.thumbnailUrl
        localDurationSec = pick.durationSec
        if (title.isBlank()) title = pick.title.take(24)
        sourceKind = SourceKind.ONLINE
        localAbsolutePath = null
        localThumbPath = null
        onPendingOnlineConsumed()
    }

    // 系统相册
    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                importing = true
                val r = V2VideoFileStore.importVideo(context, uri)
                importing = false
                if (r == null) {
                    importError = "无法读取视频，请检查权限或换一个视频"
                } else {
                    sourceKind = SourceKind.ALBUM
                    localAbsolutePath = r.absolutePath
                    localThumbPath = r.thumbnailPath
                    localDurationSec = r.durationSec
                    onlineUrl = null
                }
            }
        }
    }

    val hasVideo = localAbsolutePath != null || onlineUrl != null
    val canSave = hasVideo && title.isNotBlank() && !importing
    val hasDraft = title.isNotBlank() || note.isNotBlank() || hasVideo ||
        selectedTagIds.isNotEmpty() || selectedGoalIds.isNotEmpty()

    fun attemptClose() {
        if (hasDraft) showDiscardConfirm = true else onClose()
    }

    fun buildAndSave() {
        val isOnline = onlineUrl != null
        val videoStringForStore = onlineUrl ?: localAbsolutePath?.let { "file://$it" }
        val thumbStringForStore = localThumbPath?.let { "file://$it" }
        val tagNames = V2CategoryStore.tagNamesFor(selectedTagIds)
        val tagIds = selectedTagIds.toList()
        val topic = V2MemoBridge.inferTopic(
            category = V2CategoryStore.groupForItemId(tagIds.firstOrNull() ?: "")?.name,
            tags = tagNames,
            title = title.trim(),
        )
        val training = when (sourceKind) {
            SourceKind.CAMERA -> "拍摄"
            SourceKind.ALBUM -> "本地上传"
            SourceKind.ONLINE -> "在线视频"
            null -> null
        }
        val memo = V2Memo.mixedMemo(
            displayTitle = title.trim().ifBlank { "视频 Memo" },
            body = note.trim(),
            videoUrl = videoStringForStore,
            durationSec = localDurationSec,
            topic = topic,
            tags = tagNames,
            metaText = buildMetaText(isOnline = isOnline, durationSec = localDurationSec),
        ).apply {
            this.tagIds = tagIds
            thumbnailUrl = if (isOnline) onlineThumbUrl else thumbStringForStore
            trainingMethod = training
            isOnlineVideo = isOnline
            platform = if (isOnline) onlinePlatform else null
            linkedGoalIds = selectedGoalIds.toList()
            thumbnailKind = V2MemoBridge.thumbnailKindFor(topic)
        }
        V2MemoStore.create(memo)
        selectedGoalIds.forEach { goalId -> V2GoalStore.linkMemo(memo.id, goalId) }
        onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F0)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NavBar(canSave = canSave, onClose = ::attemptClose, onSave = ::buildAndSave)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                VideoSourceBlock(
                    sourceKind = sourceKind,
                    localPath = localAbsolutePath,
                    localThumbPath = localThumbPath,
                    onlineUrl = onlineUrl,
                    onlineThumbUrl = onlineThumbUrl,
                    durationSec = localDurationSec,
                    importing = importing,
                    onPickCamera = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            launchCameraCapture()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onPickAlbum = {
                        pickLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.VideoOnly,
                            ),
                        )
                    },
                    onPickOnline = {
                        if (onOpenOnlinePicker != null) onOpenOnlinePicker()
                        else showOnlineDialog = true
                    },
                    onClear = {
                        sourceKind = null
                        localAbsolutePath = null
                        localThumbPath = null
                        localDurationSec = null
                        onlineUrl = null
                        onlinePlatform = null
                        onlineThumbUrl = null
                    },
                )
                TitleSection(title = title, onChange = { if (it.length <= 24) title = it })
                NoteSection(
                    note = note,
                    expanded = noteExpanded,
                    onToggleExpand = { noteExpanded = !noteExpanded },
                    onChange = { if (it.length <= 240) note = it },
                )
                V2MemoTagPicker(
                    selectedTagIds = selectedTagIds,
                    onSelectedTagIdsChange = { selectedTagIds = it },
                )
                LinkedGoalsSection(
                    goals = activeGoals.filter { it.status == V2Goal.Status.ACTIVE },
                    selected = selectedGoalIds,
                    onToggle = { id ->
                        selectedGoalIds = if (id in selectedGoalIds) selectedGoalIds - id else selectedGoalIds + id
                    },
                )
            }
        }
        BottomSaveBar(canSave = canSave, onSave = ::buildAndSave, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showOnlineDialog && onOpenOnlinePicker == null) {
        OnlineUrlDialog(
            onConfirm = { url ->
                showOnlineDialog = false
                onlineUrl = url
                onlinePlatform = detectPlatform(url)
                sourceKind = SourceKind.ONLINE
                localAbsolutePath = null
                localThumbPath = null
                localDurationSec = null
            },
            onDismiss = { showOnlineDialog = false },
        )
    }

    if (importError != null) {
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text("出错了") },
            text = { Text(importError ?: "") },
            confirmButton = {
                TextButton(onClick = { importError = null }) { Text("好") }
            },
        )
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("放弃编辑？") },
            text = { Text("当前内容尚未保存，确定要离开吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onClose()
                }) { Text("放弃", color = Color(0xFFC0392B)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("继续编辑") }
            },
        )
    }
}

private enum class SourceKind { CAMERA, ALBUM, ONLINE }

private fun detectPlatform(url: String): String = when {
    url.contains("bilibili", ignoreCase = true) -> "bilibili"
    url.contains("douyin", ignoreCase = true) -> "douyin"
    url.contains("xiaohongshu", ignoreCase = true) -> "xiaohongshu"
    url.contains("youtube", ignoreCase = true) -> "youtube"
    else -> "online"
}

private fun buildMetaText(isOnline: Boolean, durationSec: Double?): String {
    val parts = mutableListOf<String>()
    if (isOnline) parts.add("在线视频") else parts.add("本地视频")
    if (durationSec != null && durationSec > 0) {
        val total = durationSec.toInt()
        val m = total / 60
        val s = total % 60
        parts.add(String.format(Locale.ROOT, "%d:%02d", m, s))
    }
    return parts.joinToString(" · ")
}

// =================================================================
// 标题 / 笔记 / 底部栏
// =================================================================

@Composable
private fun TitleSection(title: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("标题", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A21))
        Row(verticalAlignment = Alignment.Bottom) {
            BasicTextField(
                value = title,
                onValueChange = onChange,
                textStyle = TextStyle(fontSize = 15.sp, color = Color(0xFF1B2A21)),
                cursorBrush = SolidColor(Color(0xFF2D6A4F)),
                modifier = Modifier.weight(1f),
                singleLine = true,
                decorationBox = { inner ->
                    if (title.isEmpty()) {
                        Text("给这条 Memo 起个标题…", fontSize = 15.sp, color = Color(0xFF9CA3AF))
                    }
                    inner()
                },
            )
            Text("${title.length}/24", fontSize = 11.sp, color = Color(0xFF9CA3AF))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E7EB)))
    }
}

@Composable
private fun NoteSection(
    note: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onChange: (String) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFAF7F0))
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                .clickable { onToggleExpand() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (note.isEmpty()) "加点笔记…" else "笔记",
                fontSize = 15.sp,
                color = if (note.isEmpty()) Color(0xFF9CA3AF) else Color(0xFF1B2A21),
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
            )
        }
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 100.dp)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(Color(0xFFFAF7F0))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .padding(16.dp),
            ) {
                BasicTextField(
                    value = note,
                    onValueChange = onChange,
                    textStyle = TextStyle(fontSize = 15.sp, color = Color(0xFF1B2A21)),
                    cursorBrush = SolidColor(Color(0xFF2D6A4F)),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (note.isEmpty()) {
                    Text("今天练习推杆时注意力不集中…", fontSize = 15.sp, color = Color(0xFFC5C5C5))
                }
                Text(
                    "${note.length}/240",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
    }
}

@Composable
private fun BottomSaveBar(canSave: Boolean, onSave: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFAF7F0))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("将保存到 视频笔记 · 视频 Memo", fontSize = 11.sp, color = Color(0xFF9CA3AF))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(50))
                .background(if (canSave) Color(0xFF2D6A4F) else Color(0xFF2D6A4F).copy(alpha = 0.4f))
                .clickable(enabled = canSave, onClick = onSave),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("保存视频 Memo", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// =================================================================
// NavBar
// =================================================================

@Composable
private fun NavBar(canSave: Boolean, onClose: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .background(Color.White.copy(alpha = 0.92f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(width = 1.dp, color = Color(0xFFE5E7EB), shape = CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color(0xFF1B2A21), modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("新建视频 Memo", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A21))
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "保存",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (canSave) Color(0xFF2D6A4F) else Color(0xFF9CA3AF),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(enabled = canSave) { onSave() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

// =================================================================
// 视频源选择卡片 + 预览
// =================================================================

@OptIn(UnstableApi::class)
@Composable
private fun VideoSourceBlock(
    sourceKind: SourceKind?,
    localPath: String?,
    localThumbPath: String?,
    onlineUrl: String?,
    onlineThumbUrl: String?,
    durationSec: Double?,
    importing: Boolean,
    onPickCamera: () -> Unit,
    onPickAlbum: () -> Unit,
    onPickOnline: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("视频来源", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A21))
            Spacer(Modifier.weight(1f))
            if (sourceKind != null && !importing) {
                Text(
                    text = "重新选择",
                    fontSize = 12.sp,
                    color = Color(0xFFC0392B),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onClear() }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SourceChip(
                icon = Icons.Filled.Videocam,
                label = "拍视频",
                selected = sourceKind == SourceKind.CAMERA,
                modifier = Modifier.weight(1f),
                onClick = onPickCamera,
            )
            SourceChip(
                icon = Icons.Filled.Photo,
                label = "选相册",
                selected = sourceKind == SourceKind.ALBUM,
                modifier = Modifier.weight(1f),
                onClick = onPickAlbum,
            )
            SourceChip(
                icon = Icons.Filled.Link,
                label = "在线视频",
                selected = sourceKind == SourceKind.ONLINE,
                modifier = Modifier.weight(1f),
                onClick = onPickOnline,
            )
        }

        PreviewArea(
            sourceKind = sourceKind,
            localPath = localPath,
            localThumbPath = localThumbPath,
            onlineUrl = onlineUrl,
            onlineThumbUrl = onlineThumbUrl,
            durationSec = durationSec,
            importing = importing,
        )
    }
}

@Composable
private fun LinkedGoalsSection(
    goals: List<com.mygolfswing.app.data.model.V2Goal>,
    selected: Set<UUID>,
    onToggle: (UUID) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("关联到目标", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A21))
        if (goals.isEmpty()) {
            Text("还没有进行中的目标", fontSize = 12.sp, color = Color(0xFF9CA3AF), modifier = Modifier.padding(vertical = 8.dp))
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(goals, key = { it.id }) { goal ->
                    val on = goal.id in selected
                    Text(
                        text = goal.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (on) Color(0xFF2D6A4F) else Color(0xFF1B2A21),
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (on) Color(0xFFE8F5E9) else Color.White)
                            .border(1.dp, if (on) Color(0xFF2D6A4F) else Color(0xFFE5E7EB), RoundedCornerShape(50))
                            .clickable { onToggle(goal.id) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(70.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFFEBF5EC) else Color.White)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color(0xFF2D6A4F) else Color(0xFFE5E7EB),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable { onClick() }
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color(0xFF2D6A4F) else Color(0xFF1B2A21),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color(0xFF2D6A4F) else Color(0xFF1B2A21),
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PreviewArea(
    sourceKind: SourceKind?,
    localPath: String?,
    localThumbPath: String?,
    onlineUrl: String?,
    onlineThumbUrl: String?,
    durationSec: Double?,
    importing: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(202.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A2E22)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            importing -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("处理视频中…", color = Color.White, fontSize = 12.sp)
                }
            }
            localPath != null -> LocalPreview(localPath, localThumbPath)
            onlineUrl != null -> OnlinePreview(onlineUrl, onlineThumbUrl)
            else -> EmptyPlaceholder()
        }

        if (!importing && (localPath != null || onlineUrl != null)) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .border(width = 2.dp, color = Color.White.copy(alpha = 0.7f), shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        if (durationSec != null && durationSec > 0) {
            val total = durationSec.toInt()
            val m = total / 60
            val s = total % 60
            Text(
                text = String.format(Locale.ROOT, "%d:%02d", m, s),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun LocalPreview(absolutePath: String, thumbPath: String?) {
    // 优先用本地缩略图（静态图），点击之后用户去主播放页全屏播
    if (!thumbPath.isNullOrBlank()) {
        AsyncImage(
            model = "file://$thumbPath",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        // 没缩略图则用 ExoPlayer 暂停在第 0 帧
        val context = LocalContext.current
        val player = remember(absolutePath) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri("file://$absolutePath"))
                volume = 0f
                prepare()
                seekTo(0)
            }
        }
        DisposableEffect(Unit) {
            onDispose { player.release() }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                }
            },
        )
    }
}

@Composable
private fun OnlinePreview(url: String, thumbnailUrl: String?) {
    if (!thumbnailUrl.isNullOrBlank()) {
        coil.compose.AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.Link,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = url,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun EmptyPlaceholder() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Filled.PlayCircle,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(36.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text("先选择或拍摄一段视频", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
    }
}

// =================================================================
// 在线视频 URL 输入对话框
// =================================================================

@Composable
private fun OnlineUrlDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("在线视频 URL") },
        text = {
            Column {
                Text(
                    text = "粘贴可以直接播放的视频地址（https 开头，支持 mp4 / m3u8）。",
                    fontSize = 12.sp,
                    color = Color(0xFF7B8A82),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; error = null },
                    placeholder = { Text("https://example.com/video.mp4") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error!!, color = Color(0xFFC0392B), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = V2OnlineVideoMetadataFetcher.cleanVideoUrl(input)
                if (!trimmed.startsWith("https://", ignoreCase = true)) {
                    error = "URL 必须以 https 开头"
                } else {
                    onConfirm(trimmed)
                }
            }) { Text("使用此地址") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
