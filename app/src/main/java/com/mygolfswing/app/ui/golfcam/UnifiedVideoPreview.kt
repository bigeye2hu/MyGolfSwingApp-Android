package com.mygolfswing.app.ui.golfcam

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.BezierPath
import com.mygolfswing.app.ui.golfcam.model.GolfCamStep
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.NormalizedPoint
import com.mygolfswing.app.ui.golfcam.model.TrajectoryTiming
import com.mygolfswing.app.ui.golfcam.render.TrajectoryAnimationOverlay
import com.mygolfswing.app.ui.golfcam.render.TrajectoryStaticOverlay
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * 视频预览 + 步骤叠加交互层（对齐 iOS UnifiedVideoPreview）。
 * 圆角卡片样式，底部覆盖播放控制按钮（TRIM 步骤显示）。
 */
@Composable
fun UnifiedVideoPreview(
    player: ExoPlayer,
    currentStep: GolfCamStep,
    state: GolfCamWorkflowState,
    modifier: Modifier = Modifier,
    onImpactPositionChange: ((Float, Float) -> Unit)? = null,
    onLandingChange: ((Float, Float) -> Unit)? = null,
    onFlightCurveChange: ((NormalizedPoint, NormalizedPoint) -> Unit)? = null,
) {
    var playing by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(playing) {
        if (playing) player.play() else player.pause()
    }

    LaunchedEffect(playing) {
        while (playing) {
            elapsedMs = player.currentPosition - (state.impactFrame?.timeMs ?: 0L)
            delay(16L)
        }
    }

    // 圆角卡片容器
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White),
    ) {
        // 视频层（点击播放/暂停）
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { playing = !playing }
                },
        )

        // 步骤叠加层
        when (currentStep) {
            GolfCamStep.IMPACT_POSITION -> ImpactPositionOverlay(
                state = state,
                onPositionChange = onImpactPositionChange,
            )
            GolfCamStep.LANDING -> LandingOverlay(
                state = state,
                onLandingChange = onLandingChange,
            )
            GolfCamStep.FLIGHT_CURVE -> FlightCurveOverlay(
                state = state,
                onFlightCurveChange = onFlightCurveChange,
            )
            GolfCamStep.TRACK_STYLE -> {
                val bezier = state.flightCurve?.bezierPath
                val trackStyle = state.trackStyle?.style
                if (bezier != null && trackStyle != null) {
                    val timing = state.impactFrame?.let { impact ->
                        TrajectoryTiming(
                            impactTimeMs = impact.timeMs,
                            landingTimeMs = impact.timeMs + 3000L,
                            fps = 30.0,
                        )
                    }
                    if (timing != null) {
                        TrajectoryAnimationOverlay(
                            bezier = bezier,
                            timing = timing,
                            style = trackStyle,
                            playing = playing,
                            elapsedMs = elapsedMs.coerceAtLeast(0L),
                        )
                    } else {
                        TrajectoryStaticOverlay(bezier = bezier, style = trackStyle)
                    }
                } else if (bezier != null) {
                    TrajectoryStaticOverlay(
                        bezier = bezier,
                        style = com.mygolfswing.app.ui.golfcam.model.TrackStyle(),
                    )
                }
            }
            else -> { /* TRIM / IMPACT_FRAME 无视频叠加，但 TRIM 显示播放控件 */ }
        }

        // 播放控制栏（TRIM 步骤显示，其他步骤隐藏）
        if (currentStep == GolfCamStep.TRIM) {
            VideoTransportControls(
                playing = playing,
                onPrev = { player.seekTo((player.currentPosition - 3000L).coerceAtLeast(0L)) },
                onPlayPause = { playing = !playing },
                onNext = { player.seekTo(player.currentPosition + 3000L) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
            )
        }
    }
}

// ─── 视频播放控制栏 ──────────────────────────────────────────────────────────────

@Composable
private fun VideoTransportControls(
    playing: Boolean,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 上一段
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "上一段",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // 播放/暂停（较大）
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(DSColors.fairway),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onPlayPause, modifier = Modifier.size(52.dp)) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        // 下一段
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "下一段",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ─── 击球位置叠加层（直接在视频上拖拽）────────────────────────────────────────────

@Composable
private fun ImpactPositionOverlay(
    state: GolfCamWorkflowState,
    onPositionChange: ((Float, Float) -> Unit)?,
) {
    val pos = state.impactPosition
    val nx = pos?.normalizedX ?: 0.35f
    val ny = pos?.normalizedY ?: 0.75f

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onPositionChange?.invoke(
                        (offset.x / size.width).coerceIn(0.02f, 0.98f),
                        (offset.y / size.height).coerceIn(0.02f, 0.98f),
                    )
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    onPositionChange?.invoke(
                        (change.position.x / size.width).coerceIn(0.02f, 0.98f),
                        (change.position.y / size.height).coerceIn(0.02f, 0.98f),
                    )
                }
            },
    ) {
        val cx = nx * size.width
        val cy = ny * size.height
        val r = 28f
        drawCircle(color = Color.White.copy(alpha = 0.18f), radius = r * 1.8f, center = Offset(cx, cy), style = Stroke(1.5f))
        drawCircle(color = DSColors.fairway, radius = r, center = Offset(cx, cy), style = Stroke(2.8f))
        drawLine(Color.White, Offset(cx - r * 1.7f, cy), Offset(cx + r * 1.7f, cy), strokeWidth = 1.5f)
        drawLine(Color.White, Offset(cx, cy - r * 1.7f), Offset(cx, cy + r * 1.7f), strokeWidth = 1.5f)
        drawCircle(color = DSColors.fairway, radius = 5f, center = Offset(cx, cy))
    }
}

// ─── 落点叠加层 ────────────────────────────────────────────────────────────────

@Composable
private fun LandingOverlay(
    state: GolfCamWorkflowState,
    onLandingChange: ((Float, Float) -> Unit)?,
) {
    val landing = state.landing
    val nx = landing?.normalizedX ?: 0.85f
    val ny = landing?.normalizedY ?: 0.78f

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onLandingChange?.invoke(
                        (offset.x / size.width).coerceIn(0.02f, 0.98f),
                        (offset.y / size.height).coerceIn(0.02f, 0.98f),
                    )
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    onLandingChange?.invoke(
                        (change.position.x / size.width).coerceIn(0.02f, 0.98f),
                        (change.position.y / size.height).coerceIn(0.02f, 0.98f),
                    )
                }
            },
    ) {
        val cx = nx * size.width
        val cy = ny * size.height
        drawCircle(color = DSColors.fairway.copy(alpha = 0.22f), radius = 36f, center = Offset(cx, cy))
        drawCircle(color = DSColors.fairway, radius = 22f, center = Offset(cx, cy), style = Stroke(2.8f))
        val d = 7f
        val path = Path().apply {
            moveTo(cx, cy - d); lineTo(cx + d, cy); lineTo(cx, cy + d); lineTo(cx - d, cy); close()
        }
        drawPath(path, color = DSColors.fairway)
    }
}

// ─── 飞行曲线叠加层 ────────────────────────────────────────────────────────────

@Composable
private fun FlightCurveOverlay(
    state: GolfCamWorkflowState,
    onFlightCurveChange: ((NormalizedPoint, NormalizedPoint) -> Unit)?,
) {
    val impact = state.impactPosition ?: return
    val landing = state.landing ?: return
    val prev = state.flightCurve

    var cp1x by remember {
        mutableStateOf(prev?.controlPoint1?.x ?: (impact.normalizedX + (landing.normalizedX - impact.normalizedX) * 0.3f))
    }
    var cp1y by remember {
        mutableStateOf(prev?.controlPoint1?.y ?: (impact.normalizedY - 0.28f).coerceIn(0.02f, 0.98f))
    }
    var cp2x by remember {
        mutableStateOf(prev?.controlPoint2?.x ?: (impact.normalizedX + (landing.normalizedX - impact.normalizedX) * 0.7f))
    }
    var cp2y by remember {
        mutableStateOf(prev?.controlPoint2?.y ?: (impact.normalizedY - 0.22f).coerceIn(0.02f, 0.98f))
    }
    var dragging by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(cp1x, cp1y, cp2x, cp2y) {
        onFlightCurveChange?.invoke(NormalizedPoint(cp1x, cp1y), NormalizedPoint(cp2x, cp2y))
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat(); val h = size.height.toFloat()
                        val d1 = abs(offset.x - cp1x * w) + abs(offset.y - cp1y * h)
                        val d2 = abs(offset.x - cp2x * w) + abs(offset.y - cp2y * h)
                        dragging = if (d1 < d2) 1 else 2
                    },
                    onDragEnd = { dragging = null },
                ) { change, _ ->
                    val w = size.width.toFloat(); val h = size.height.toFloat()
                    val newX = (change.position.x / w).coerceIn(0.02f, 0.98f)
                    val newY = (change.position.y / h).coerceIn(0.02f, 0.98f)
                    if (dragging == 1) { cp1x = newX; cp1y = newY }
                    else if (dragging == 2) { cp2x = newX; cp2y = newY }
                }
            },
    ) {
        val w = size.width; val h = size.height
        val p0 = Offset(impact.normalizedX * w, impact.normalizedY * h)
        val p1 = Offset(cp1x * w, cp1y * h)
        val p2 = Offset(cp2x * w, cp2y * h)
        val p3 = Offset(landing.normalizedX * w, landing.normalizedY * h)

        drawLine(Color.White.copy(alpha = 0.35f), p0, p1, strokeWidth = 1.5f)
        drawLine(Color.White.copy(alpha = 0.35f), p3, p2, strokeWidth = 1.5f)

        val path = Path().apply {
            moveTo(p0.x, p0.y)
            cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
        }
        drawPath(path, color = DSColors.fairway, style = Stroke(width = 3.5f, cap = StrokeCap.Round))

        drawCircle(Color.White.copy(alpha = 0.9f), 9f, p0)
        drawCircle(Color.White.copy(alpha = 0.9f), 9f, p3)
        drawCircle(DSColors.fairway.copy(alpha = 0.25f), 26f, p1)
        drawCircle(DSColors.fairway, 14f, p1)
        drawCircle(DSColors.fairway.copy(alpha = 0.25f), 26f, p2)
        drawCircle(DSColors.fairway, 14f, p2)
    }
}
