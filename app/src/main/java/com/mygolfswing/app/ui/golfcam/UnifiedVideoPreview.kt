package com.mygolfswing.app.ui.golfcam

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.ui.designsystem.DSColors
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.golfcam.model.BezierPath
import com.mygolfswing.app.ui.golfcam.model.CandidateImpactFrame
import com.mygolfswing.app.ui.golfcam.model.GolfCamFrameTimeline
import com.mygolfswing.app.ui.golfcam.model.GolfCamSpeedConfig
import com.mygolfswing.app.ui.golfcam.model.GolfCamStep
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.NormalizedPoint
import com.mygolfswing.app.ui.golfcam.model.TrajectoryTiming
import com.mygolfswing.app.ui.golfcam.render.GolfCamCurveSpec
import com.mygolfswing.app.ui.golfcam.render.TrajectoryAnimationOverlay
import com.mygolfswing.app.ui.golfcam.render.TrajectoryStaticOverlay
import com.mygolfswing.app.ui.golfcam.render.VideoDisplayGeometry
import com.mygolfswing.app.ui.golfcam.steps.CURVE_CONTROL_HIT_TARGET_DP
import kotlinx.coroutines.delay

internal fun normalizedVideoCoordinate(positionPx: Float, dimensionPx: Float): Float {
    if (dimensionPx <= 0f || !dimensionPx.isFinite() || !positionPx.isFinite()) return 0.5f
    return (positionPx / dimensionPx).coerceIn(0f, 1f)
}

internal fun normalizedCoordinateOrCenter(value: Float?): Float {
    return value?.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
}

internal fun normalizedLandingCoordinate(
    positionPx: Float,
    dimensionPx: Float,
    @Suppress("UNUSED_PARAMETER")
    markerSizePx: Float,
): Float {
    return normalizedVideoCoordinate(positionPx, dimensionPx)
}

internal fun unifiedPreviewElapsedMs(currentPositionMs: Long, impactTimeMs: Long): Long {
    return (currentPositionMs - impactTimeMs).coerceAtLeast(0L)
}

internal fun previewTrajectoryTiming(
    impactTimeMs: Long,
    flightTimeSec: Double?,
    fps: Double,
): TrajectoryTiming {
    val safeFlightTimeSec = flightTimeSec
        ?.takeIf { it.isFinite() }
        ?.coerceAtLeast(0.5)
        ?: 3.0
    return TrajectoryTiming(
        impactTimeMs = impactTimeMs,
        landingTimeMs = GolfCamFrameTimeline.timeMsAfterSeconds(impactTimeMs, safeFlightTimeSec),
        fps = GolfCamFrameTimeline.safeFrameRate(fps),
    )
}

internal fun trackStyleStaticPreviewProgress(
    speedPreviewMode: Boolean,
    timing: TrajectoryTiming?,
    speedLevel: Double,
): Float {
    if (!speedPreviewMode || timing == null) return 1f
    return GolfCamSpeedConfig.progress(
        timeProgress = 0.5 / timing.totalDurationSec.coerceAtLeast(0.000001),
        speedLevel = speedLevel,
    ).toFloat().coerceIn(0f, 1f)
}

internal fun clampedCurveControlPoint(
    xPx: Float,
    yPx: Float,
    widthPx: Float,
    heightPx: Float,
): NormalizedPoint {
    if (widthPx <= 0f || heightPx <= 0f || !widthPx.isFinite() || !heightPx.isFinite() || !xPx.isFinite() || !yPx.isFinite()) {
        return NormalizedPoint(0.5f, 0.5f)
    }
    return GolfCamCurveSpec.clampDraggedControlPoint(NormalizedPoint(xPx / widthPx, yPx / heightPx))
}

internal fun visibleCurveHitCenter(
    point: NormalizedPoint,
    widthPx: Float,
    heightPx: Float,
    targetSizePx: Float,
): Offset {
    @Suppress("UNUSED_PARAMETER")
    val ignoredTargetSizePx = targetSizePx
    fun scaled(value: Float, dimension: Float): Float {
        if (dimension <= 0f || !dimension.isFinite()) return 0f
        return (value.takeIf { it.isFinite() } ?: 0.5f) * dimension
    }
    return Offset(
        x = scaled(point.x, widthPx),
        y = scaled(point.y, heightPx),
    )
}

private const val LANDING_MARKER_ANCHOR_RATIO = 0.25f
internal const val IMPACT_MARKER_SIZE_DP = 24f
internal const val IMPACT_DRAG_RADIUS_DP = 50f
internal const val IMPACT_ACTIVE_RING_SIZE_DP = 34f
internal const val IMPACT_ACTIVE_RING_STROKE_DP = 2f
internal const val IMPACT_ACTIVE_RING_ALPHA = 0.6f
internal const val LANDING_MARKER_SIZE_DP = 24f
internal const val LANDING_DRAG_RADIUS_DP = 50f

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
    videoFrameRate: Double = 30.0,
    trackStyleSpeedPreviewMode: Boolean = false,
    candidateFrames: List<CandidateImpactFrame> = emptyList(),
    onCandidateFrameSelected: ((CandidateImpactFrame) -> Unit)? = null,
    onImpactPositionChange: ((Float, Float) -> Unit)? = null,
    onLandingChange: ((Float, Float) -> Unit)? = null,
    onFlightCurveChange: ((NormalizedPoint, NormalizedPoint) -> Unit)? = null,
) {
    var playing by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var videoAspectRatio by remember { mutableFloatStateOf(9f / 16f) }
    val supportsZoom = currentStep == GolfCamStep.LANDING || currentStep == GolfCamStep.FLIGHT_CURVE
    val minimumZoomScale = if (currentStep == GolfCamStep.FLIGHT_CURVE) 0.72f else 1f
    var zoomScale by remember(currentStep) {
        mutableFloatStateOf(if (currentStep == GolfCamStep.FLIGHT_CURVE) 0.86f else 1f)
    }
    var curveVisible by remember(currentStep) { mutableStateOf(true) }
    var trackStyleShowsStaticPreview by remember(currentStep) {
        mutableStateOf(currentStep == GolfCamStep.TRACK_STYLE)
    }
    val haptics = LocalHapticFeedback.current

    DisposableEffect(player) {
        @Suppress("DEPRECATION")
        fun updateAspect(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                val rawAspect = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height
                videoAspectRatio = if (videoSize.unappliedRotationDegrees == 90 || videoSize.unappliedRotationDegrees == 270) {
                    1f / rawAspect.coerceAtLeast(0.01f)
                } else {
                    rawAspect.coerceAtLeast(0.01f)
                }
            }
        }
        updateAspect(player.videoSize)
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                updateAspect(videoSize)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player, playing) {
        if (playing) player.play() else player.pause()
    }

    LaunchedEffect(currentStep, trackStyleSpeedPreviewMode) {
        if (currentStep == GolfCamStep.TRACK_STYLE) {
            playing = false
            trackStyleShowsStaticPreview = true
        }
    }

    LaunchedEffect(player, currentStep, state.impactFrame, playing) {
        while (true) {
            if (playing || currentStep == GolfCamStep.TRACK_STYLE) {
                elapsedMs = unifiedPreviewElapsedMs(
                    currentPositionMs = player.currentPosition,
                    impactTimeMs = state.impactFrame?.timeMs ?: 0L,
                )
            }
            delay(if (playing) 16L else 33L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val displaySize = VideoDisplayGeometry.fit(
                containerWidth = maxWidth.value,
                containerHeight = maxHeight.value,
                videoAspectRatio = videoAspectRatio,
            )

            Box(
                modifier = Modifier.size(displaySize.width.dp, displaySize.height.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                        }
                        .pointerInput(supportsZoom, minimumZoomScale) {
                            if (!supportsZoom) return@pointerInput
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.changes.count { it.pressed } >= 2) {
                                        val zoomChange = event.calculateZoom()
                                        if (zoomChange.isFinite() && zoomChange > 0f) {
                                            zoomScale = (zoomScale * zoomChange).coerceIn(minimumZoomScale, 4f)
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                        .testTag("golfcam-video-scaled-content"),
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = false
                            }
                        },
                        update = { view -> view.player = player },
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    if (currentStep == GolfCamStep.TRACK_STYLE) {
                                        trackStyleShowsStaticPreview = false
                                    }
                                    playing = !playing
                                }
                            },
                    )

                    when (currentStep) {
                        GolfCamStep.IMPACT_POSITION -> ImpactPositionOverlay(
                            state = state,
                            onPositionChange = onImpactPositionChange,
                        )
                        GolfCamStep.LANDING -> LandingOverlay(
                            state = state,
                            onLandingChange = onLandingChange,
                        )
                        GolfCamStep.FLIGHT_CURVE -> if (curveVisible) {
                            FlightCurveOverlay(
                                state = state,
                                onFlightCurveChange = onFlightCurveChange,
                            )
                        }
                        GolfCamStep.TRACK_STYLE -> {
                            val bezier = state.flightCurve?.bezierPath
                            val trackStyle = state.trackStyle?.style
                            if (bezier != null && trackStyle != null) {
                                val timing = state.impactFrame?.let { impact ->
                                    previewTrajectoryTiming(
                                        impactTimeMs = impact.timeMs,
                                        flightTimeSec = state.flightCurve?.landingPoint?.flightTimeSec,
                                        fps = videoFrameRate,
                                    )
                                }
                                if (trackStyleShowsStaticPreview) {
                                    val staticProgress = trackStyleStaticPreviewProgress(
                                        speedPreviewMode = trackStyleSpeedPreviewMode,
                                        timing = timing,
                                        speedLevel = trackStyle.speedLevel,
                                    )
                                    TrajectoryStaticOverlay(
                                        bezier = bezier,
                                        style = trackStyle,
                                        progress = staticProgress,
                                        modifier = Modifier.testTag("golfcam-track-style-static-preview"),
                                    )
                                } else if (timing != null) {
                                    TrajectoryAnimationOverlay(
                                        bezier = bezier,
                                        timing = timing,
                                        style = trackStyle,
                                        playing = playing,
                                        elapsedMs = elapsedMs.coerceAtLeast(0L),
                                        modifier = Modifier.testTag("golfcam-track-style-animated-preview"),
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
                        GolfCamStep.IMPACT_FRAME -> {
                            if (candidateFrames.isNotEmpty()) {
                                CandidateFrameBadges(
                                    candidates = candidateFrames,
                                    currentFrameIndex = state.impactFrame?.frameIndex ?: -1,
                                    onSelect = onCandidateFrameSelected,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 10.dp, top = 10.dp),
                                )
                            }
                        }
                        else -> { /* TRIM 无视频叠加，TRIM 显示播放控件 */ }
                    }

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

                    if (currentStep == GolfCamStep.TRACK_STYLE) {
                        TrackStyleTransportControls(
                            playing = playing,
                            onPlayPause = {
                                trackStyleShowsStaticPreview = false
                                playing = !playing
                            },
                            onReplay = {
                                trackStyleShowsStaticPreview = false
                                player.seekTo((state.impactFrame?.timeMs ?: 0L).coerceAtLeast(0L))
                                playing = true
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                        )
                    }
                }

                if (supportsZoom && zoomScale > minimumZoomScale + 0.001f) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.60f))
                            .clickable {
                                zoomScale = minimumZoomScale
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("golfcam-reset-zoom"),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.ZoomOutMap, null, tint = Color.White, modifier = Modifier.size(11.dp))
                        Text("重置", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                if (currentStep == GolfCamStep.FLIGHT_CURVE) {
                    if (!curveVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            curveVisible = true
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                    )
                                }
                                .testTag("golfcam-flight-curve-hidden-overlay"),
                        )
                        Text(
                            "长按显示轨迹",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.48f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }

                    IconButton(
                        onClick = {
                            curveVisible = !curveVisible
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.50f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                            .shadow(8.dp, CircleShape)
                            .testTag("golfcam-flight-curve-visibility"),
                    ) {
                        Icon(
                            imageVector = if (curveVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (curveVisible) "隐藏轨迹" else "显示轨迹",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

// ─── 视频播放控制栏 ──────────────────────────────────────────────────────────────

@Composable
private fun TrackStyleTransportControls(
    playing: Boolean,
    onPlayPause: () -> Unit,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 40.dp, vertical = 16.dp)
            .testTag("golfcam-track-style-transport"),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.90f))
                .clickable(onClick = onPlayPause)
                .testTag("golfcam-track-style-play-pause"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "暂停轨迹预览" else "播放轨迹预览",
                tint = DSColors.deepGreen,
                modifier = Modifier.size(25.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.78f))
                .clickable(onClick = onReplay)
                .testTag("golfcam-track-style-replay"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "重播轨迹预览",
                tint = DSColors.deepGreen,
                modifier = Modifier.size(25.dp),
            )
        }
    }
}

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
    var nx by remember(pos) { mutableFloatStateOf(normalizedCoordinateOrCenter(pos?.normalizedX)) }
    var ny by remember(pos) { mutableFloatStateOf(normalizedCoordinateOrCenter(pos?.normalizedY)) }
    var isDragging by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        if (pos == null) onPositionChange?.invoke(nx, ny)
    }

    val impactMarkPainter = androidx.compose.ui.res.painterResource(
        id = com.mygolfswing.app.R.drawable.impact_mark,
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val markerSizeDp = IMPACT_MARKER_SIZE_DP.dp
        val markerHalfPx = with(density) { (markerSizeDp / 2).toPx() }
        val dragRadiusPx = with(density) { IMPACT_DRAG_RADIUS_DP.dp.toPx() }

        // iOS：画面双击直接跳位；拖动只由击球点周围 100dp 的透明圆形热区接管。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("golfcam-impact-position-overlay")
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            nx = normalizedVideoCoordinate(offset.x, wPx)
                            ny = normalizedVideoCoordinate(offset.y, hPx)
                            onPositionChange?.invoke(nx, ny)
                        },
                    )
                },
        )

        Box(
            modifier = Modifier
                .size((IMPACT_DRAG_RADIUS_DP * 2f).dp)
                .offset {
                    IntOffset(
                        x = (nx * wPx - dragRadiusPx).toInt(),
                        y = (ny * hPx - dragRadiusPx).toInt(),
                    )
                }
                .testTag("golfcam-impact-position-drag-target")
                .pointerInput(wPx, hPx) {
                    detectDragGestures(
                        onDragStart = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isDragging = true
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            nx = normalizedVideoCoordinate(nx * wPx + dragAmount.x, wPx)
                            ny = normalizedVideoCoordinate(ny * hPx + dragAmount.y, hPx)
                            onPositionChange?.invoke(nx, ny)
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                    )
                },
        )

        // iOS active ring: 34pt, 2pt white, 60% opacity.
        if (isDragging) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = IMPACT_ACTIVE_RING_ALPHA),
                    radius = with(density) { (IMPACT_ACTIVE_RING_SIZE_DP / 2f).dp.toPx() },
                    center = Offset(nx * size.width, ny * size.height),
                    style = Stroke(with(density) { IMPACT_ACTIVE_RING_STROKE_DP.dp.toPx() }),
                )
            }
        }

        // ImpactMark 图标（白色，24dp，居中，带阴影效果）
        androidx.compose.foundation.Image(
            painter = impactMarkPainter,
            contentDescription = "击球点",
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White),
            modifier = Modifier
                .size(markerSizeDp)
                .offset {
                    IntOffset(
                        x = (nx * wPx - markerHalfPx).toInt(),
                        y = (ny * hPx - markerHalfPx).toInt(),
                    )
                }
                .shadow(
                    elevation = 4.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.5f),
                )
                .testTag("golfcam-impact-position-marker"),
        )
    }
}

// ─── 落点叠加层 ────────────────────────────────────────────────────────────────

@Composable
private fun LandingOverlay(
    state: GolfCamWorkflowState,
    onLandingChange: ((Float, Float) -> Unit)?,
) {
    val landing = state.landing
    var nx by remember(landing) { mutableFloatStateOf(normalizedCoordinateOrCenter(landing?.normalizedX)) }
    var ny by remember(landing) { mutableFloatStateOf(normalizedCoordinateOrCenter(landing?.normalizedY)) }
    var isDragging by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val landingMarkPainter = androidx.compose.ui.res.painterResource(com.mygolfswing.app.R.drawable.landing_mark)
    val currentOnLandingChange by rememberUpdatedState(onLandingChange)

    LaunchedEffect(Unit) {
        if (landing == null) onLandingChange?.invoke(nx, ny)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("golfcam-landing-overlay"),
    ) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val markerSizeDp = (LANDING_MARKER_SIZE_DP * if (isDragging) 1.08f else 1f).dp
        val markerSizePx = with(density) { markerSizeDp.toPx() }
        val dragTargetPx = with(density) { (LANDING_DRAG_RADIUS_DP * 2f).dp.toPx() }
        val anchorCenter = Offset(nx * wPx, ny * hPx)
        val visualCx = anchorCenter.x
        val visualCy = anchorCenter.y - markerSizePx * LANDING_MARKER_ANCHOR_RATIO
        val labelCenterY = anchorCenter.y - markerSizePx * 0.8f - with(density) { 12.dp.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            nx = normalizedVideoCoordinate(offset.x, wPx)
                            ny = normalizedVideoCoordinate(offset.y, hPx)
                            currentOnLandingChange?.invoke(nx, ny)
                        },
                    )
                },
        ) {}

        Image(
            painter = landingMarkPainter,
            contentDescription = "落点",
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White),
            modifier = Modifier
                .size(markerSizeDp)
                .offset {
                    IntOffset(
                        x = (visualCx - markerSizePx / 2f).toInt(),
                        y = (visualCy - markerSizePx / 2f).toInt(),
                    )
                }
                .shadow(
                    elevation = 3.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.50f),
                    spotColor = Color.Black.copy(alpha = 0.50f),
                )
                .testTag("golfcam-landing-marker"),
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (anchorCenter.x - with(density) { 22.dp.toPx() }).toInt(),
                        y = (labelCenterY - with(density) { 11.dp.toPx() }).toInt(),
                    )
                }
                .width(44.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("落点", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }

        Box(
            modifier = Modifier
                .size((LANDING_DRAG_RADIUS_DP * 2f).dp)
                .offset {
                    IntOffset(
                        x = (anchorCenter.x - dragTargetPx / 2f).toInt(),
                        y = (anchorCenter.y - dragTargetPx / 2f).toInt(),
                    )
                }
                .pointerInput(wPx, hPx) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                    ) { change, dragAmount ->
                        change.consume()
                        nx = normalizedVideoCoordinate(nx * wPx + dragAmount.x, wPx)
                        ny = normalizedVideoCoordinate(ny * hPx + dragAmount.y, hPx)
                        currentOnLandingChange?.invoke(nx, ny)
                    }
                }
                .testTag("golfcam-landing-drag-target"),
        )
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
    val haptics = LocalHapticFeedback.current
    val defaults = remember(impact, landing) {
        GolfCamCurveSpec.suggestedControlPoints(
            start = NormalizedPoint(impact.normalizedX, impact.normalizedY),
            end = NormalizedPoint(landing.normalizedX, landing.normalizedY),
        )
    }

    var cp1x by remember(impact, landing) {
        mutableFloatStateOf(prev?.controlPoint1?.x ?: defaults.first.x)
    }
    var cp1y by remember(impact, landing) {
        mutableFloatStateOf(prev?.controlPoint1?.y ?: defaults.first.y)
    }
    var cp2x by remember(impact, landing) {
        mutableFloatStateOf(prev?.controlPoint2?.x ?: defaults.second.x)
    }
    var cp2y by remember(impact, landing) {
        mutableFloatStateOf(prev?.controlPoint2?.y ?: defaults.second.y)
    }
    var dragging by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        onFlightCurveChange?.invoke(NormalizedPoint(cp1x, cp1y), NormalizedPoint(cp2x, cp2y))
    }
    LaunchedEffect(cp1x, cp1y, cp2x, cp2y) {
        delay(80L)
        onFlightCurveChange?.invoke(NormalizedPoint(cp1x, cp1y), NormalizedPoint(cp2x, cp2y))
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("golfcam-flight-curve-overlay"),
    ) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val touchTargetDp = CURVE_CONTROL_HIT_TARGET_DP.dp
        val touchTargetPx = with(density) { touchTargetDp.toPx() }
        val cp1HitCenter = visibleCurveHitCenter(NormalizedPoint(cp1x, cp1y), wPx, hPx, touchTargetPx)
        val cp2HitCenter = visibleCurveHitCenter(NormalizedPoint(cp2x, cp2y), wPx, hPx, touchTargetPx)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val p0 = Offset(impact.normalizedX * w, impact.normalizedY * h)
            val p1 = Offset(cp1x * w, cp1y * h)
            val p2 = Offset(cp2x * w, cp2y * h)
            val p3 = Offset(landing.normalizedX * w, landing.normalizedY * h)

            drawGolfCamCurveStroke(p0 = p0, p1 = p1, p2 = p2, p3 = p3)

            drawCurveControlMarker(center = p1, dragging = dragging == 1, label = "P1")
            drawCurveControlMarker(center = p2, dragging = dragging == 2, label = "P2")
        }

        CurveControlHitTarget(
            targetSize = touchTargetDp,
            onDragStart = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                dragging = 1
            },
            onDragEnd = { dragging = null },
            onDrag = { dragAmount ->
                val next = clampedCurveControlPoint(
                    xPx = cp1x * wPx + dragAmount.x,
                    yPx = cp1y * hPx + dragAmount.y,
                    widthPx = wPx,
                    heightPx = hPx,
                )
                cp1x = next.x
                cp1y = next.y
            },
            modifier = Modifier.offset {
                IntOffset((cp1HitCenter.x - touchTargetPx / 2f).toInt(), (cp1HitCenter.y - touchTargetPx / 2f).toInt())
            },
            tag = "golfcam-flight-curve-p1-target",
        )
        CurveControlHitTarget(
            targetSize = touchTargetDp,
            onDragStart = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                dragging = 2
            },
            onDragEnd = { dragging = null },
            onDrag = { dragAmount ->
                val next = clampedCurveControlPoint(
                    xPx = cp2x * wPx + dragAmount.x,
                    yPx = cp2y * hPx + dragAmount.y,
                    widthPx = wPx,
                    heightPx = hPx,
                )
                cp2x = next.x
                cp2y = next.y
            },
            modifier = Modifier.offset {
                IntOffset((cp2HitCenter.x - touchTargetPx / 2f).toInt(), (cp2HitCenter.y - touchTargetPx / 2f).toInt())
            },
            tag = "golfcam-flight-curve-p2-target",
        )
    }
}

@Composable
private fun CurveControlHitTarget(
    targetSize: androidx.compose.ui.unit.Dp,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    tag: String,
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDrag by rememberUpdatedState(onDrag)

    Box(
        modifier = modifier
            .size(targetSize)
            .testTag(tag)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { currentOnDragStart() },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragEnd() },
                ) { change, dragAmount ->
                    change.consume()
                    currentOnDrag(dragAmount)
                }
            },
    )
}

private fun DrawScope.drawCurveControlMarker(center: Offset, dragging: Boolean, label: String) {
    if (dragging) {
        drawCircle(Color.White.copy(alpha = 0.6f), 22.dp.toPx(), center, style = Stroke(2.dp.toPx()))
    }
    drawCircle(Color.Black.copy(alpha = 0.22f), 13.dp.toPx(), center + Offset(0f, 1.5.dp.toPx()))
    drawCircle(Color.White, 12.dp.toPx(), center)
    drawCircle(DSColors.fairway, 12.dp.toPx(), center, style = Stroke(1.5.dp.toPx()))
    val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = DSColors.fairway.toArgb()
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = 10.dp.toPx()
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    drawContext.canvas.nativeCanvas.drawText(
        label,
        center.x,
        center.y - (labelPaint.ascent() + labelPaint.descent()) / 2f,
        labelPaint,
    )
}

private fun DrawScope.drawGolfCamCurveStroke(
    p0: Offset,
    p1: Offset,
    p2: Offset,
    p3: Offset,
    segments: Int = GolfCamCurveSpec.SEGMENTS,
) {
    for (index in 0 until segments) {
        val start = cubicPoint(index.toFloat() / segments, p0, p1, p2, p3)
        val end = cubicPoint((index + 1).toFloat() / segments, p0, p1, p2, p3)
        drawLine(
            color = Color.Black.copy(alpha = GolfCamCurveSpec.SHADOW_ALPHA),
            start = start,
            end = end,
            strokeWidth = GolfCamCurveSpec.foregroundWidth(index, segments) + GolfCamCurveSpec.SHADOW_WIDTH_ADD,
            cap = GolfCamCurveSpec.STROKE_CAP,
        )
    }

    for (index in 0 until segments) {
        val start = cubicPoint(index.toFloat() / segments, p0, p1, p2, p3)
        val end = cubicPoint((index + 1).toFloat() / segments, p0, p1, p2, p3)
        drawLine(
            color = GolfCamCurveSpec.segmentColor(index, segments),
            start = start,
            end = end,
            strokeWidth = GolfCamCurveSpec.foregroundWidth(index, segments),
            cap = GolfCamCurveSpec.STROKE_CAP,
        )
    }
}

private fun cubicPoint(t: Float, p0: Offset, p1: Offset, p2: Offset, p3: Offset): Offset {
    val u = 1f - t
    val tt = t * t
    val uu = u * u
    val uuu = uu * u
    val ttt = tt * t
    return Offset(
        x = uuu * p0.x + 3f * uu * t * p1.x + 3f * u * tt * p2.x + ttt * p3.x,
        y = uuu * p0.y + 3f * uu * t * p1.y + 3f * u * tt * p2.y + ttt * p3.y,
    )
}

// ─── 候选帧徽章浮层（对齐 iOS candidateFrameButton）──────────────────────────

@Composable
private fun CandidateFrameBadges(
    candidates: List<CandidateImpactFrame>,
    currentFrameIndex: Int,
    onSelect: ((CandidateImpactFrame) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        candidates.take(3).forEachIndexed { idx, candidate ->
            val rank = idx + 1
            val isPrimary = rank == 1
            val isSelected = currentFrameIndex == candidate.frameIndex

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.70f))
                    .then(
                        if (isSelected) Modifier.border(
                            width = 2.5.dp,
                            color = Color(0xFFFFD700),
                            shape = CircleShape,
                        ) else Modifier.border(
                            width = 1.5.dp,
                            color = Color.Gray.copy(alpha = 0.50f),
                            shape = CircleShape,
                        ),
                    )
                    .clickable { onSelect?.invoke(candidate) },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "#$rank",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (isPrimary && isSelected) {
                            Text("★", color = Color(0xFFFFD700), fontSize = 9.sp)
                        }
                    }
                    Text(
                        "${candidate.confidencePercent}%",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 8.sp,
                    )
                }
            }
        }
    }
}
