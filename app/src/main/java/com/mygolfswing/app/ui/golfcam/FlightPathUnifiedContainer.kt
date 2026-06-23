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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.CandidateImpactFrame
import com.mygolfswing.app.ui.golfcam.model.FlightCurveSelection
import com.mygolfswing.app.ui.golfcam.model.GolfCamStep
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
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
    var durationMs by remember { mutableLongStateOf(0L) }
    // 音频检测候选帧
    var candidateFrames by remember { mutableStateOf<List<CandidateImpactFrame>>(emptyList()) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) { onDispose { player.release() } }

    LaunchedEffect(player) {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && durationMs <= 0L) {
                    durationMs = player.duration.coerceAtLeast(1L)
                }
            }
        })
        if (player.playbackState == Player.STATE_READY && durationMs <= 0L) {
            durationMs = player.duration.coerceAtLeast(1L)
        }
    }

    // 裁剪完成后触发音频检测（仅跑一次）
    LaunchedEffect(state.trimResult) {
        val trim = state.trimResult ?: return@LaunchedEffect
        if (candidateFrames.isNotEmpty()) return@LaunchedEffect
        val startSec = trim.startMs / 1000.0
        val endSec = trim.endMs / 1000.0
        val fps = 30.0
        val rawCandidates = AudioImpactDetector.detect(context, videoUri, startSec, endSec)
        candidateFrames = rawCandidates
            .mapIndexed { _, raw ->
                val relTime = raw.timeSec - startSec
                CandidateImpactFrame(
                    frameIndex = (relTime * fps).toInt().coerceAtLeast(0),
                    confidence = raw.confidence,
                    timeSec = relTime,
                )
            }
            .sortedByDescending { it.confidence }
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
                    candidateFrames = candidateFrames,
                    onCandidateFrameSelected = { candidate ->
                        val trim = state.trimResult
                        val startMs = trim?.startMs ?: 0L
                        val timeMs = startMs + (candidate.timeSec * 1000).toLong()
                        player.seekTo(timeMs)
                        player.pause()
                        state.impactFrame = state.impactFrame?.copy(
                            frameIndex = candidate.frameIndex,
                            timeMs = timeMs,
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
                        onResult = { state.trimResult = it },
                    )
                    GolfCamStep.IMPACT_FRAME -> ImpactFrameStepTool(
                        player = player,
                        videoUri = videoUri,
                        durationMs = durationMs,
                        state = state,
                        candidateFrames = candidateFrames,
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
                        val fps = 30.0
                        fun seekCurveFrame(frameIndex: Int) {
                            val target = trimStartMs + (frameIndex / fps * 1000).toLong()
                            player.seekTo(target.coerceAtLeast(0L))
                            player.pause()
                        }
                        // 进入曲线步骤时初始化帧数
                        LaunchedEffect(durationMs, state.impactFrame) {
                            if (durationMs > 0 && flightCurveTotalFrames == 0) {
                                flightCurveTotalFrames = (durationMs / 1000.0 * fps).toInt()
                                    .coerceAtLeast(1)
                                flightCurveFrameIndex = state.impactFrame?.frameIndex ?: 0
                                seekCurveFrame(flightCurveFrameIndex)
                            }
                        }
                        FlightCurveStepTool(
                            videoUri = videoUri,
                            trimStartMs = trimStartMs,
                            state = state,
                            currentFrameIndex = flightCurveFrameIndex,
                            totalFrames = flightCurveTotalFrames,
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
    }
}

// ─── 顶部导航栏 ──────────────────────────────────────────────────────────────────

@Composable
private fun StepNavBar(
    currentStep: GolfCamStep,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.warmWhite)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
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
        IconButton(onClick = { /* 提示信息 */ }) {
            Icon(
                Icons.Filled.Info,
                contentDescription = "帮助",
                tint = DSColors.shadowGray,
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
    // 圆圈高度 32dp，连接线需对齐到圆圈中心 = top 15dp（32/2 - 1）
    val circleSize = 32.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.warmWhite)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = step == currentStep
            val isDone = step.index < currentStep.index

            // 步骤圆形徽章（占用固定宽度，均分）
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable { onStepTap(step) },
            ) {
                Box(
                    modifier = Modifier
                        .size(circleSize)
                        .clip(CircleShape)
                        .background(
                            when {
                                isActive -> DSColors.fairway.copy(alpha = 0.18f)
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
                            modifier = Modifier.size(16.dp),
                        )
                        else -> Text(
                            text = "${step.index + 1}",
                            color = DSColors.shadowGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = step.shortTitle,
                    color = if (isActive) DSColors.fairway else DSColors.shadowGray,
                    fontSize = 9.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }

            // 连接线（最后一步不画）：padding top = circleSize/2 - 1dp 对齐圆圈中心
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 左侧按钮（重新选择 / 上一步）
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(DSColors.mistGreen)
                .clickable { onLeftAction() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!currentStep.isFirst) {
                    Text(
                        "< 上一步",
                        color = DSColors.inkGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    Text(
                        "↺ 重新选择",
                        color = DSColors.inkGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // 中间步骤信息
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "${currentStep.index + 1}/6",
                color = DSColors.shadowGray,
                fontSize = 11.sp,
            )
            Text(
                currentStep.title,
                color = DSColors.inkGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // 右侧按钮（下一步 / 完成）
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (!canContinue)
                        DSColors.fairway.copy(alpha = 0.4f)
                    else
                        DSColors.fairway,
                )
                .clickable(enabled = canContinue) {
                    if (currentStep.isLast) onComplete() else onNext()
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (currentStep.isLast) "完成 ✓" else "下一步 →",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
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
