package com.mygolfswing.app.ui.golfcam

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.R
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.TrackStyleSelection
import kotlin.math.roundToInt

/**
 * GolfCam 轨迹绘制主入口（对齐 iOS V2TrajectorySourceSheet + handleTrajectoryVideoSelected）。
 *
 * 状态机：
 * - 无视频 → 显示「选择视频」底部弹窗
 * - 有视频、未完成 → 进入 FlightPathUnifiedContainer
 * - 有视频、已完成 → 进入 OverlayPreviewScreen（导出预览）
 */
@Composable
fun GolfCamTrajectoryEntry(
    onDismiss: () -> Unit,
) {
    val state = remember { GolfCamWorkflowState() }
    var pickedUri: Uri? by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var lastSelection by remember { mutableStateOf<TrackStyleSelection?>(null) }
    var lastImpactTimeMs by remember { mutableStateOf(0L) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            statusMessage = "正在准备轨迹绘制流程"
            if (pickedUri != uri) state.clearAll()
            pickedUri = uri
            state.videoUri = uri
            showPreview = false
            statusMessage = null
        }
    }

    when {
        showPreview && pickedUri != null && lastSelection != null -> {
            val uri = pickedUri ?: return
            GolfCamOverlayPreviewScreen(
                videoUri = uri,
                selection = lastSelection!!,
                impactTimeMs = lastImpactTimeMs,
                onDismiss = onDismiss,
                onReEdit = { showPreview = false },
            )
        }
        pickedUri != null -> {
            val uri = pickedUri ?: return
            FlightPathUnifiedContainer(
                videoUri = uri,
                state = state,
                initialStep = state.completedStep,
                onComplete = { selection ->
                    lastSelection = selection
                    lastImpactTimeMs = state.impactFrame?.timeMs ?: 0L
                    showPreview = true
                },
                onReselect = {
                    // 清空状态，重新打开相册
                    state.clearAll()
                    pickedUri = null
                    showPreview = false
                    videoPicker.launch("video/*")
                },
                onCancel = onDismiss,
            )
        }
        else -> {
            TrajectorySourceSheet(
                statusMessage = statusMessage,
                onPickVideo = { videoPicker.launch("video/*") },
                onDismiss = onDismiss,
            )
        }
    }
}

// ─── 底部弹窗（对齐 iOS V2TrajectorySourceSheet）────────────────────────────────

@Composable
private fun TrajectorySourceSheet(
    statusMessage: String?,
    onPickVideo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isBusy = statusMessage != null

    // 进入动画
    var launched by remember { mutableStateOf(false) }
    val dimAlpha by animateFloatAsState(
        targetValue = if (launched) 0.42f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "dim",
    )
    val sheetOffsetFraction by animateFloatAsState(
        targetValue = if (launched) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 350f),
        label = "sheetOffset",
    )
    LaunchedEffect(Unit) { launched = true }

    // 拖动偏移
    var dragDelta by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── 半透明遮罩 ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimAlpha))
                .then(
                    if (!isBusy) Modifier.clickable(onClick = onDismiss) else Modifier,
                ),
        )

        // ── 底部 Sheet ──
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        SheetCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset { IntOffset(0, ((sheetOffsetFraction * 560 + dragDelta.coerceAtLeast(0f)) * density).roundToInt()) }
                .then(
                    if (!isBusy) Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragDelta > 80f) onDismiss() else dragDelta = 0f
                            },
                            onDragCancel = { dragDelta = 0f },
                        ) { _, delta -> dragDelta = (dragDelta + delta).coerceAtLeast(0f) }
                    } else Modifier,
                ),
            statusMessage = statusMessage,
            isBusy = isBusy,
            onPickVideo = onPickVideo,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun SheetCard(
    modifier: Modifier,
    statusMessage: String?,
    isBusy: Boolean,
    onPickVideo: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(DSColors.warmWhite)
            .navigationBarsPadding(),
    ) {
        // 拖动手柄
        Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DSColors.shadowGray.copy(alpha = 0.3f)),
            )
        }

        // Hero 背景图
        HeroHeader(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))

        // 内容区
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isBusy) {
                ProcessingCard(statusMessage = statusMessage)
            } else {
                SourceButton(
                    title = "从相册选择",
                    subtitle = "导入击球视频，进入 V2 六步轨迹绘制流程",
                    onClick = onPickVideo,
                )
                InfoStrip()
            }
        }

        // 取消按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 16.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DSColors.sandBeige.copy(alpha = if (isBusy) 0.58f else 1f))
                .then(if (!isBusy) Modifier.clickable(onClick = onDismiss) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (isBusy) "正在处理" else "取消",
                color = if (isBusy) DSColors.shadowGray else DSColors.inkGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─── Hero 头部 ────────────────────────────────────────────────────────────────

@Composable
private fun HeroHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.BottomStart,
    ) {
        Image(
            painter = painterResource(id = R.drawable.v2_hero_grass_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // 渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DSColors.deepGreen.copy(alpha = 0.24f),
                            DSColors.deepGreen.copy(alpha = 0.82f),
                        ),
                    ),
                ),
        )
        // 文字内容
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // 球路标注徽章
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.TravelExplore, null, tint = Color.White, modifier = Modifier.size(13.dp))
                Text("球路标注", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("创建飞行轨迹", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "裁剪、击球帧、击球位置、落点、曲线与样式",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

// ─── 从相册选择按钮 ───────────────────────────────────────────────────────────

@Composable
private fun SourceButton(title: String, subtitle: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(DSColors.deepGreen, DSColors.fairway)))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp, lineHeight = 16.sp)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

// ─── 信息条 ─────────────────────────────────────────────────────────────────

@Composable
private fun InfoStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.82f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.AutoAwesome, null, tint = DSColors.shadowGray, modifier = Modifier.size(12.dp))
        Text(
            "系统会自动识别视频比例与帧率，并恢复同一视频的上次编辑进度",
            color = DSColors.shadowGray,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

// ─── 处理中卡片 ───────────────────────────────────────────────────────────────

@Composable
private fun ProcessingCard(statusMessage: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DSColors.mistGreen),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = DSColors.fairway,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                statusMessage ?: "正在读取相册视频",
                color = DSColors.inkGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "大视频可能需要几秒钟，完成后会自动进入轨迹绘制",
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}
