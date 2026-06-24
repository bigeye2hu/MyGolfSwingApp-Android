package com.mygolfswing.app.ui.golfcam

import android.net.Uri
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.CandidateImpactFrame
import com.mygolfswing.app.ui.golfcam.model.FlightCurveSelection
import com.mygolfswing.app.ui.golfcam.model.GolfCamFrameTimeline
import com.mygolfswing.app.ui.golfcam.model.GolfCamStep
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.ImpactFrameSelection
import com.mygolfswing.app.ui.golfcam.model.ImpactPositionSelection
import com.mygolfswing.app.ui.golfcam.model.LandingOutput
import com.mygolfswing.app.ui.golfcam.model.NormalizedPoint
import com.mygolfswing.app.ui.golfcam.model.TrackStyleSelection
import com.mygolfswing.app.ui.golfcam.steps.FlightCurveStepTool
import com.mygolfswing.app.ui.golfcam.steps.ImpactFrameStepTool
import com.mygolfswing.app.ui.golfcam.steps.ImpactPositionStepTool
import com.mygolfswing.app.ui.golfcam.steps.LandingStepTool
import com.mygolfswing.app.ui.golfcam.steps.TrackStyleStepTool
import com.mygolfswing.app.ui.golfcam.steps.TrimStepTool
import androidx.compose.runtime.mutableIntStateOf

/**
 * 六步统一轨迹编辑容器（对齐 iOS FlightPathUnifiedContainer）。
 * 整体为米白浅色主题，步骤指示器为圆形徽章 + 连接线。
 */
@Composable
fun FlightPathUnifiedContainer(
    videoUri: Uri,
    state: GolfCamWorkflowState,
    initialStep: GolfCamStep = GolfCamStep.TRIM,
    onComplete: (TrackStyleSelection) -> Unit,
    onReselect: (() -> Unit)? = null,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(initialStep) }
    // FlightCurve 帧导航状态
    var flightCurveFrameIndex by remember { mutableIntStateOf(0) }
    var flightCurveTotalFrames by remember { mutableIntStateOf(0) }
    var flightCurveFrameRate by remember { mutableStateOf<Double?>(null) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var videoFrameRate by remember { mutableStateOf(30.0) }
    // 音频检测候选帧
    var candidateFrames by remember { mutableStateOf<List<CandidateImpactFrame>>(emptyList()) }
    var candidateFrameRate by remember { mutableStateOf<Double?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) { onDispose { player.release() } }

    DisposableEffect(player) {
        fun updateFrameRate() {
            val frameRate = player.videoFormat?.frameRate ?: return
            if (frameRate > 1f) {
                videoFrameRate = frameRate.coerceIn(1f, 120f).toDouble()
            }
        }
        updateFrameRate()
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && durationMs <= 0L) {
                    durationMs = player.duration.coerceAtLeast(1L)
                }
                updateFrameRate()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                updateFrameRate()
            }
        }
        player.addListener(listener)
        if (player.playbackState == Player.STATE_READY) {
            durationMs = player.duration.coerceAtLeast(1L)
            updateFrameRate()
        }
        onDispose { player.removeListener(listener) }
    }

    // 裁剪完成后触发音频检测，候选帧索引按真实视频帧率换算。
    LaunchedEffect(state.trimResult, videoFrameRate) {
        val trim = state.trimResult ?: return@LaunchedEffect
        if (candidateFrames.isNotEmpty() && candidateFrameRate == videoFrameRate) return@LaunchedEffect
        val startSec = trim.startMs / 1000.0
        val endSec = trim.endMs / 1000.0
        val rawCandidates = AudioImpactDetector.detect(context, videoUri, startSec, endSec)
        candidateFrames = rawCandidates
            .mapIndexed { _, raw ->
                val relTime = raw.timeSec - startSec
                CandidateImpactFrame(
                    frameIndex = GolfCamFrameTimeline.frameIndexAt(relTime, videoFrameRate),
                    confidence = raw.confidence,
                    timeSec = relTime,
                )
            }
            .sortedByDescending { it.confidence }
        candidateFrameRate = videoFrameRate
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── 顶部导航栏 ──
            StepNavBar(
                currentStep = currentStep,
                onBack = onCancel,
                onHelp = { showHelp = true },
            )

            // ── 步骤进度指示器 ──
            StepBadgeRow(
                currentStep = currentStep,
                completedStep = state.completedStep,
                onStepTap = { step ->
                    if (step.index <= state.completedStep.index + 1) currentStep = step
                },
            )

            // ── 视频预览区 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                UnifiedVideoPreview(
                    player = player,
                    currentStep = currentStep,
                    state = state,
                    videoFrameRate = videoFrameRate,
                    candidateFrames = candidateFrames,
                    onCandidateFrameSelected = { candidate ->
                        val trim = state.trimResult
                        val startMs = trim?.startMs ?: 0L
                        val endMs = trim?.endMs ?: durationMs
                        val timeMs = (startMs + (candidate.timeSec * 1000).toLong()).coerceIn(startMs, endMs)
                        player.seekTo(timeMs)
                        player.pause()
                        state.impactFrame = ImpactFrameSelection(
                            frameIndex = candidate.frameIndex,
                            timeMs = timeMs,
                            candidateFrames = candidateFrames,
                        )
                    },
                    onImpactPositionChange = { nx, ny ->
                        state.impactPosition = ImpactPositionSelection(nx, ny)
                    },
                    onLandingChange = { nx, ny ->
                        state.landing = LandingOutput(nx, ny, state.landing?.flightTimeSec ?: 4.5)
                    },
                    onFlightCurveChange = { cp1, cp2 ->
                        val impact = state.impactPosition ?: return@UnifiedVideoPreview
                        val landing = state.landing ?: return@UnifiedVideoPreview
                        state.flightCurve = FlightCurveSelection(
                            impactPosition = impact,
                            landingPoint = landing,
                            controlPoint1 = cp1,
                            controlPoint2 = cp2,
                        )
                    },
                )
            }

            // ── 步骤工具区 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                when (currentStep) {
                    GolfCamStep.TRIM -> TrimStepTool(
                        videoUri = videoUri,
                        durationMs = durationMs,
                        savedTrimResult = state.trimResult,
                        onResult = { state.trimResult = it },
                    )
                    GolfCamStep.IMPACT_FRAME -> ImpactFrameStepTool(
                        player = player,
                        videoUri = videoUri,
                        durationMs = durationMs,
                        state = state,
                        candidateFrames = candidateFrames,
                        frameRate = videoFrameRate,
                        onResult = { state.impactFrame = it },
                    )
                    GolfCamStep.IMPACT_POSITION -> ImpactPositionStepTool(state = state)
                    GolfCamStep.LANDING -> LandingStepTool(
                        videoUri = videoUri,
                        state = state,
                        onFlightTimeChange = { sec ->
                            state.landing = state.landing?.copy(flightTimeSec = sec)
                                ?: LandingOutput(0.5f, 0.5f, sec)
                            val impactMs = state.impactFrame?.timeMs ?: state.trimResult?.startMs ?: 0L
                            val clipEndMs = state.trimResult?.endMs ?: durationMs
                            val safeEndMs = (clipEndMs - 33L).coerceAtLeast(0L)
                            val targetMs = (impactMs + (sec * 1000).toLong()).coerceAtMost(safeEndMs)
                            player.seekTo(targetMs.coerceAtLeast(0L))
                            player.pause()
                        },
                    )
                    GolfCamStep.FLIGHT_CURVE -> {
                        val trimStartMs = state.trimResult?.startMs ?: 0L
                        val fps = videoFrameRate
                        fun seekCurveFrame(frameIndex: Int) {
                            val target = GolfCamFrameTimeline.timeMsForFrame(trimStartMs, frameIndex, fps)
                            player.seekTo(target.coerceAtLeast(0L))
                            player.pause()
                        }
                        // 进入曲线步骤时初始化帧数
                        LaunchedEffect(durationMs, state.impactFrame, fps) {
                            if (durationMs > 0 && flightCurveFrameRate != fps) {
                                flightCurveTotalFrames = GolfCamFrameTimeline.totalFrames(durationMs, fps)
                                flightCurveFrameIndex = (state.impactFrame?.frameIndex ?: flightCurveFrameIndex)
                                    .coerceIn(0, (flightCurveTotalFrames - 1).coerceAtLeast(0))
                                flightCurveFrameRate = fps
                                seekCurveFrame(flightCurveFrameIndex)
                            }
                        }
                        FlightCurveStepTool(
                            videoUri = videoUri,
                            trimStartMs = trimStartMs,
                            state = state,
                            currentFrameIndex = flightCurveFrameIndex,
                            totalFrames = flightCurveTotalFrames,
                            frameRate = fps,
                            onFrameSelected = { frameIndex ->
                                flightCurveFrameIndex = frameIndex.coerceIn(0, (flightCurveTotalFrames - 1).coerceAtLeast(0))
                                seekCurveFrame(flightCurveFrameIndex)
                            },
                            onImpactFrame = {
                                flightCurveFrameIndex = (state.impactFrame?.frameIndex ?: 0)
                                    .coerceIn(0, (flightCurveTotalFrames - 1).coerceAtLeast(0))
                                seekCurveFrame(flightCurveFrameIndex)
                            },
                            onPrevFrame = {
                                if (flightCurveFrameIndex > 0) {
                                    flightCurveFrameIndex--
                                    seekCurveFrame(flightCurveFrameIndex)
                                }
                            },
                            onNextFrame = {
                                if (flightCurveFrameIndex < flightCurveTotalFrames - 1) {
                                    flightCurveFrameIndex++
                                    seekCurveFrame(flightCurveFrameIndex)
                                }
                            },
                        )
                    }
                    GolfCamStep.TRACK_STYLE -> TrackStyleStepTool(
                        state = state,
                        onResult = { state.trackStyle = it },
                    )
                }
            }

            // ── 底部三栏导航栏 ──
            StepBottomBar(
                currentStep = currentStep,
                state = state,
                onLeftAction = {
                    when {
                        currentStep.isFirst && onReselect != null -> onReselect()
                        currentStep.isFirst -> onCancel()
                        else -> currentStep = currentStep.prev ?: currentStep
                    }
                },
                onNext = { currentStep = currentStep.next ?: currentStep },
                onComplete = { state.trackStyle?.let { onComplete(it) } },
            )
        }

        if (showHelp) {
            StepHelpPopCard(
                step = currentStep,
                onDismiss = { showHelp = false },
            )
        }
    }
}

// ─── 顶部导航栏 ──────────────────────────────────────────────────────────────────

@Composable
private fun StepNavBar(
    currentStep: GolfCamStep,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, ambientColor = DSColors.deepGreen.copy(alpha = 0.05f), spotColor = DSColors.deepGreen.copy(alpha = 0.05f))
            .background(DSColors.cardWhite)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.Filled.Home,
                contentDescription = "返回",
                tint = DSColors.inkGreen,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = currentStep.title,
            color = DSColors.inkGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onHelp) {
            Icon(
                Icons.Filled.Info,
                contentDescription = "帮助",
                tint = DSColors.inkGreen,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ─── 步骤圆形徽章进度条 ────────────────────────────────────────────────────────────

@Composable
private fun StepBadgeRow(
    currentStep: GolfCamStep,
    completedStep: GolfCamStep,
    onStepTap: (GolfCamStep) -> Unit,
) {
    val steps = GolfCamStep.values()
    val circleSize = 28.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.cardWhite)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = step == currentStep
            val isDone = step.index < currentStep.index

            // 步骤圆形徽章（占用固定宽度，均分）
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(44.dp)
                    .clickable { onStepTap(step) },
            ) {
                Box(
                    modifier = Modifier
                        .size(circleSize)
                        .clip(CircleShape)
                        .background(
                            when {
                                isActive -> DSColors.fairway.copy(alpha = 0.16f)
                                isDone -> DSColors.fairway
                                else -> DSColors.mistGreen
                            },
                        )
                        .then(
                            if (isActive) Modifier.border(2.dp, DSColors.fairway, CircleShape)
                            else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        isDone -> Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        isActive -> Icon(
                            step.icon,
                            contentDescription = null,
                            tint = DSColors.fairway,
                            modifier = Modifier.size(12.dp),
                        )
                        else -> Text(
                            text = "${step.index + 1}",
                            color = DSColors.shadowGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.shortTitle,
                    color = when {
                        isActive -> DSColors.inkGreen
                        isDone -> DSColors.fairway
                        else -> DSColors.shadowGray
                    },
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }

            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = circleSize / 2 - 1.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            if (isDone) DSColors.fairway else DSColors.mistGreen,
                        ),
                )
            }
        }
    }
}

// ─── 底部三栏固定导航栏 ──────────────────────────────────────────────────────────

@Composable
private fun StepBottomBar(
    currentStep: GolfCamStep,
    state: GolfCamWorkflowState,
    onLeftAction: () -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit,
) {
    val canContinue = state.canContinueFrom(currentStep)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, ambientColor = DSColors.deepGreen.copy(alpha = 0.08f), spotColor = DSColors.deepGreen.copy(alpha = 0.08f))
            .background(DSColors.cardWhite)
            .navigationBarsPadding()
            .padding(top = 1.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DSColors.mistGreen.copy(alpha = 0.7f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FooterActionButton(
                text = if (currentStep.isFirst) "重新选择" else "上一步",
                icon = if (currentStep.isFirst) Icons.Filled.Refresh else Icons.AutoMirrored.Filled.ArrowBack,
                enabled = true,
                primary = false,
                onClick = onLeftAction,
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "${currentStep.index + 1} / 6",
                    color = DSColors.inkGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    currentStep.title,
                    color = DSColors.shadowGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                )
            }

            FooterActionButton(
                text = if (currentStep.isLast) "完成" else "下一步",
                icon = if (currentStep.isLast) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
                enabled = canContinue,
                primary = true,
                onClick = {
                    if (currentStep.isLast) onComplete() else onNext()
                },
            )
        }
    }
}

@Composable
private fun FooterActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val background = if (primary && enabled) {
        Brush.linearGradient(listOf(DSColors.fairway, DSColors.deepGreen))
    } else {
        Brush.linearGradient(listOf(DSColors.mistGreen, DSColors.mistGreen))
    }
    val contentColor = when {
        primary && enabled -> Color.White
        primary -> DSColors.shadowGray
        else -> DSColors.inkGreen
    }
    Row(
        modifier = Modifier
            .width(118.dp)
            .height(44.dp)
            .clip(shape)
            .background(background)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (!primary) {
            Icon(icon, null, tint = contentColor.copy(alpha = if (enabled) 1f else 0.45f), modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text,
            color = contentColor.copy(alpha = if (enabled) 1f else 0.55f),
            fontSize = 14.sp,
            fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Medium,
        )
        if (primary) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(icon, null, tint = contentColor.copy(alpha = if (enabled) 1f else 0.55f), modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun StepHelpPopCard(
    step: GolfCamStep,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2C2C2E))
                .clickable { }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(step.icon, null, tint = DSColors.fairway, modifier = Modifier.size(18.dp))
                Text(step.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, null, tint = Color.White.copy(alpha = 0.68f), modifier = Modifier.size(13.dp))
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                step.helpItems().forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(DSColors.fairway.copy(alpha = 0.8f)),
                        )
                        Text(item, color = Color(0xFFD1D1D6), fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

private fun GolfCamWorkflowState.canContinueFrom(step: GolfCamStep): Boolean {
    return when (step) {
        GolfCamStep.TRIM -> trimResult != null
        GolfCamStep.IMPACT_FRAME -> impactFrame != null
        GolfCamStep.IMPACT_POSITION -> impactPosition != null
        GolfCamStep.LANDING -> landing != null
        GolfCamStep.FLIGHT_CURVE -> flightCurve != null
        GolfCamStep.TRACK_STYLE -> trackStyle != null
    }
}

private fun GolfCamStep.helpItems(): List<String> = when (this) {
    GolfCamStep.TRIM -> listOf("拖动两端手柄选择有效片段", "建议保留从挥杆准备到球落地的完整过程")
    GolfCamStep.IMPACT_FRAME -> listOf("拖动缩略图时间轴定位", "使用左右按钮微调", "系统会自动检测候选击球帧")
    GolfCamStep.IMPACT_POSITION -> listOf("拖动标记点到球的位置", "尽量精确标记球的中心")
    GolfCamStep.LANDING -> listOf("拖动时间轴设置飞行时间", "在画面中标记落点位置", "预览曲线会实时更新")
    GolfCamStep.FLIGHT_CURVE -> listOf("拖动 P1/P2 控制点调整曲线形状", "预览线会实时显示", "点击击球帧可快速回到击球时刻")
    GolfCamStep.TRACK_STYLE -> listOf("选择颜色方案", "调整宽度、速度与拖尾长度", "距离标签会同步到预览和导出")
}
