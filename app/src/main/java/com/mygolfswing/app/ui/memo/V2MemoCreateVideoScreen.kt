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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.CheckCircle
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
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2Topic
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.video.V2VideoFileStore
import kotlinx.coroutines.launch
import java.io.File

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
    var tags by remember { mutableStateOf(listOf<String>()) }
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
                    importError = "录制失败，请重试"
                } else {
                    sourceKind = SourceKind.CAMERA
                    localAbsolutePath = r.absolutePath
                    localThumbPath = r.thumbnailPath
                    localDurationSec = r.durationSec
                    onlineUrl = null
                }
            }
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
    var topic by remember { mutableStateOf(V2Topic.SWING) }

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
    val hasDraft = title.isNotBlank() || note.isNotBlank() || hasVideo || tags.isNotEmpty() || selectedGoalIds.isNotEmpty()

    fun attemptClose() {
        if (hasDraft) showDiscardConfirm = true else onClose()
    }

    fun buildAndSave() {
        val isOnline = onlineUrl != null
        val videoStringForStore = onlineUrl ?: localAbsolutePath?.let { "file://$it" }
        val thumbStringForStore = localThumbPath?.let { "file://$it" }
        val training = when (sourceKind) {
            SourceKind.CAMERA -> "拍摄"
            SourceKind.ALBUM -> "本地上传"
            SourceKind.ONLINE -> "在线"
            null -> null
        }
        val memo = V2Memo.mixedMemo(
            displayTitle = title.trim().ifBlank { "视频 Memo" },
            body = note.trim(),
            videoUrl = videoStringForStore,
            durationSec = localDurationSec,
            topic = topic,
            tags = tags,
            metaText = buildMetaText(
                isOnline = isOnline,
                durationSec = localDurationSec,
            ),
        ).apply {
            thumbnailUrl = if (isOnline) onlineThumbUrl else thumbStringForStore
            trainingMethod = training
            isOnlineVideo = isOnline
            platform = if (isOnline) onlinePlatform else null
            linkedGoalIds = selectedGoalIds.toList()
        }
        V2MemoStore.create(memo)
        selectedGoalIds.forEach { goalId -> V2GoalStore.linkMemo(memo.id, goalId) }
        onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    0.0f to Color(0xFFF0F8F4),
                    0.5f to Color(0xFFF5F5F0),
                    1.0f to Color(0xFFFAF7F0),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NavBar(canSave = canSave, onClose = ::attemptClose, onSave = ::buildAndSave)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
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

                FormCard {
                    SectionLabel("标题")
                    OutlineField(
                        text = title,
                        onChange = { if (it.length <= 24) title = it },
                        placeholder = "如：7号铁下杆节奏",
                    )

                    DividerLine()

                    SectionLabel("笔记（可选）")
                    OutlineField(
                        text = note,
                        onChange = { if (it.length <= 240) note = it },
                        placeholder = "记下今天的观察 / 教练点评……",
                        minHeight = 100.dp,
                        singleLine = false,
                    )

                    DividerLine()

                    SectionLabel("话题")
                    TopicChipRow(topic = topic, onSelect = { topic = it })

                    DividerLine()

                    SectionLabel("标签")
                    V2MemoTagPicker(selected = tags, onChange = { tags = it })

                    DividerLine()

                    SectionLabel("关联目标")
                    LinkedGoalsField(
                        goals = activeGoals.filter { it.status == V2Goal.Status.ACTIVE },
                        selected = selectedGoalIds,
                        onToggle = { id ->
                            selectedGoalIds = if (id in selectedGoalIds) selectedGoalIds - id else selectedGoalIds + id
                        },
                    )
                }

                SubmitButton(canSubmit = canSave, onClick = ::buildAndSave)
            }
        }
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
        parts.add(String.format("%d:%02d", m, s))
    }
    return parts.joinToString(" · ")
}

// =================================================================
// NavBar
// =================================================================

@Composable
private fun NavBar(canSave: Boolean, onClose: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
        Text("视频 Memo", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A21))
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
            Text("视频源", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B2A21))
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
private fun LinkedGoalsField(
    goals: List<com.mygolfswing.app.data.model.V2Goal>,
    selected: Set<UUID>,
    onToggle: (UUID) -> Unit,
) {
    if (goals.isEmpty()) {
        Text(
            "暂无进行中的目标，可先创建目标后再关联",
            fontSize = 12.sp,
            color = Color(0xFF9CA3AF),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        goals.take(8).forEach { goal ->
            val on = goal.id in selected
            Text(
                text = goal.title,
                fontSize = 13.sp,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                color = if (on) Color(0xFF2D6A4F) else Color(0xFF1B2A21),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) Color(0xFFEBF5EC) else Color(0xFFF9FAFB))
                    .border(
                        1.dp,
                        if (on) Color(0xFF2D6A4F) else Color(0xFFE5E7EB),
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onToggle(goal.id) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
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
                text = String.format("%d:%02d", m, s),
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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
                    text = "粘贴可以直接播放的视频地址（http/https 开头，支持 mp4 / m3u8）。",
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
                val trimmed = input.trim()
                if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                    error = "URL 必须以 http 或 https 开头"
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

// =================================================================
// 表单子组件
// =================================================================

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .padding(vertical = 4.dp),
    ) { content() }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1B2A21),
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .background(Color.Black.copy(alpha = 0.06f)),
    )
}

@Composable
private fun OutlineField(
    text: String,
    onChange: (String) -> Unit,
    placeholder: String,
    minHeight: androidx.compose.ui.unit.Dp = 44.dp,
    singleLine: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFAF7F0))
            .border(width = 1.dp, color = Color(0xFFE5E7EB), shape = RoundedCornerShape(12.dp))
            .defaultMinSize(minHeight = minHeight)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (text.isEmpty()) {
            Text(text = placeholder, fontSize = 14.sp, color = Color(0xFFB8BEC4))
        }
        BasicTextField(
            value = text,
            onValueChange = onChange,
            textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF1B2A21)),
            cursorBrush = SolidColor(Color(0xFF2D6A4F)),
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
        )
    }
}

@Composable
private fun TopicChipRow(topic: V2Topic, onSelect: (V2Topic) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(V2Topic.assignable, key = { it.name }) { t ->
            val selected = t == topic
            Text(
                text = t.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) Color.White else Color(0xFF1B2A21),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) Color(0xFF2D6A4F) else Color.White)
                    .border(width = 1.dp, color = if (selected) Color(0xFF2D6A4F) else Color(0xFFE5E7EB), shape = RoundedCornerShape(50))
                    .clickable { onSelect(t) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun SubmitButton(canSubmit: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(elevation = if (canSubmit) 10.dp else 0.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        if (canSubmit) Color(0xFF2D6A4F) else Color(0xFFB8BEC4),
                        if (canSubmit) Color(0xFF40916C) else Color(0xFFD1D5DB),
                    ),
                ),
            )
            .clickable(enabled = canSubmit, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(16.dp)
                .padding(end = 4.dp),
        )
        Text(text = "保存视频 Memo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
