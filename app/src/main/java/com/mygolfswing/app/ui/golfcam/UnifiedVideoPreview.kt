package com.mygolfswing.app.ui.golfcam

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import com.mygolfswing.app.ui.golfcam.model.GolfCamStep
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.NormalizedPoint
import com.mygolfswing.app.ui.golfcam.model.TrajectoryTiming
import com.mygolfswing.app.ui.golfcam.render.GolfCamCurveSpec
import com.mygolfswing.app.ui.golfcam.render.TrajectoryAnimationOverlay
import com.mygolfswing.app.ui.golfcam.render.TrajectoryStaticOverlay
import com.mygolfswing.app.ui.golfcam.render.VideoDisplayGeometry
import kotlinx.coroutines.delay

internal fun normalizedVideoCoordinate(positionPx: Float, dimensionPx: Float): Float {
    if (dimensionPx <= 0f) return 0f
    return (positionPx / dimensionPx).coerceIn(0f, 1f)
}

internal fun normalizedLandingCoordinate(
    positionPx: Float,
    dimensionPx: Float,
    markerSizePx: Float,
): Float {
    return normalizedVideoCoordinate(
        positionPx + markerSizePx * LANDING_MARKER_ANCHOR_RATIO,
        dimensionPx,
    )
}

internal fun clampedCurveControlPoint(
    xPx: Float,
    yPx: Float,
    widthPx: Float,
    heightPx: Float,
): NormalizedPoint {
    if (widthPx <= 0f || heightPx <= 0f) return NormalizedPoint(0.5f, 0.5f)
    return GolfCamCurveSpec.clampDraggedControlPoint(NormalizedPoint(xPx / widthPx, yPx / heightPx))
}

private fun Offset.distanceSquaredTo(other: Offset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return dx * dx + dy * dy
}

private fun visibleCurveHitCenter(
    point: NormalizedPoint,
    widthPx: Float,
    heightPx: Float,
    targetSizePx: Float,
): Offset {
    fun clampToVisible(value: Float, dimension: Float): Float {
        if (dimension <= 0f) return 0f
        if (dimension <= targetSizePx) return dimension / 2f
        val half = targetSizePx / 2f
        return value.coerceIn(half, dimension - half)
    }
    return Offset(
        x = clampToVisible(point.x * widthPx, widthPx),
        y = clampToVisible(point.y * heightPx, heightPx),
    )
}

private const val LANDING_MARKER_ANCHOR_RATIO = 0.25f

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
    candidateFrames: List<CandidateImpactFrame> = emptyList(),
    onCandidateFrameSelected: ((CandidateImpactFrame) -> Unit)? = null,
    onImpactPositionChange: ((Float, Float) -> Unit)? = null,
    onLandingChange: ((Float, Float) -> Unit)? = null,
    onFlightCurveChange: ((NormalizedPoint, NormalizedPoint) -> Unit)? = null,
) {
    var playing by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var videoAspectRatio by remember { mutableFloatStateOf(9f / 16f) }

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

    LaunchedEffect(playing) {
        if (playing) player.play() else player.pause()
    }

    LaunchedEffect(playing) {
        while (playing) {
            elapsedMs = player.currentPosition - (state.impactFrame?.timeMs ?: 0L)
            delay(16L)
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
                                val flightTimeMs = ((state.flightCurve?.landingPoint?.flightTimeSec ?: 3.0) * 1000)
                                    .toLong()
                                    .coerceAtLeast(500L)
                                TrajectoryTiming(
                                    impactTimeMs = impact.timeMs,
                                    landingTimeMs = impact.timeMs + flightTimeMs,
                                    fps = videoFrameRate,
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
            }
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
    var nx by remember(pos) { mutableFloatStateOf(pos?.normalizedX ?: 0.5f) }
    var ny by remember(pos) { mutableFloatStateOf(pos?.normalizedY ?: 0.5f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragGrabOffset by remember { mutableStateOf(Offset.Zero) }

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
        val dragRadiusPx = with(density) { 50.dp.toPx() }
        val markerSizeDp = 28.dp
        val markerHalfPx = with(density) { (markerSizeDp / 2).toPx() }
        val markerCenter = Offset(nx * wPx, ny * hPx)
        val currentMarkerCenter by rememberUpdatedState(markerCenter)

        // 双击全局跳位
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            nx = normalizedVideoCoordinate(offset.x, wPx)
                            ny = normalizedVideoCoordinate(offset.y, hPx)
                            onPositionChange?.invoke(nx, ny)
                        },
                    )
                }
                .pointerInput(Unit) {
                    var hitDrag = false
                    detectDragGestures(
                        onDragStart = { offset ->
                            val center = currentMarkerCenter
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            hitDrag = (dx * dx + dy * dy) <= dragRadiusPx * dragRadiusPx
                            dragGrabOffset = if (hitDrag) offset - center else Offset.Zero
                            isDragging = hitDrag
                        },
                        onDrag = { change, _ ->
                            if (hitDrag) {
                                change.consume()
                                val center = change.position - dragGrabOffset
                                nx = normalizedVideoCoordinate(center.x, wPx)
                                ny = normalizedVideoCoordinate(center.y, hPx)
                                onPositionChange?.invoke(nx, ny)
                            }
                        },
                        onDragEnd = { isDragging = false; hitDrag = false; dragGrabOffset = Offset.Zero },
                        onDragCancel = { isDragging = false; hitDrag = false; dragGrabOffset = Offset.Zero },
                    )
                },
        )

        // 拖动时的外圈（对齐 iOS 白色圆环）
        if (isDragging) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White,
                    radius = with(density) { 18.dp.toPx() },
                    center = Offset(nx * size.width, ny * size.height),
                    style = Stroke(2.5f),
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
                },
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
    var nx by remember(landing) { mutableFloatStateOf(landing?.normalizedX ?: 0.5f) }
    var ny by remember(landing) { mutableFloatStateOf(landing?.normalizedY ?: 0.5f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragGrabOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        if (landing == null) onLandingChange?.invoke(nx, ny)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val markerSizePx = with(density) { 24.dp.toPx() }
        val dragRadiusPx = with(density) { 50.dp.toPx() }
        val anchorCenter = Offset(nx * wPx, ny * hPx)
        val currentAnchorCenter by rememberUpdatedState(anchorCenter)
        val visualCx = anchorCenter.x
        val visualCy = anchorCenter.y - markerSizePx * LANDING_MARKER_ANCHOR_RATIO

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            nx = normalizedVideoCoordinate(offset.x, wPx)
                            ny = normalizedVideoCoordinate(offset.y, hPx)
                            onLandingChange?.invoke(nx, ny)
                        },
                    )
                }
                .pointerInput(Unit) {
                    var hitDrag = false
                    detectDragGestures(
                        onDragStart = { offset ->
                            val anchor = currentAnchorCenter
                            val visualCenter = anchor - Offset(0f, markerSizePx * LANDING_MARKER_ANCHOR_RATIO)
                            val radiusSquared = dragRadiusPx * dragRadiusPx
                            hitDrag = offset.distanceSquaredTo(anchor) <= radiusSquared ||
                                offset.distanceSquaredTo(visualCenter) <= radiusSquared
                            dragGrabOffset = if (hitDrag) offset - anchor else Offset.Zero
                            isDragging = hitDrag
                        },
                        onDragEnd = { hitDrag = false; isDragging = false; dragGrabOffset = Offset.Zero },
                        onDragCancel = { hitDrag = false; isDragging = false; dragGrabOffset = Offset.Zero },
                    ) { change, _ ->
                        if (hitDrag) {
                            change.consume()
                            val center = change.position - dragGrabOffset
                            nx = normalizedVideoCoordinate(center.x, wPx)
                            ny = normalizedVideoCoordinate(center.y, hPx)
                            onLandingChange?.invoke(nx, ny)
                        }
                    }
                },
        ) {
            val cx = nx * size.width
            val anchorOffset = markerSizePx * LANDING_MARKER_ANCHOR_RATIO
            val cy = ny * size.height - anchorOffset
            drawLandingMarker(anchor = Offset(cx, cy + anchorOffset), active = isDragging)
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (visualCx - with(density) { 23.dp.toPx() }).toInt(),
                        y = (visualCy - with(density) { 44.dp.toPx() }).toInt(),
                    )
                }
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
            Text("落点", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
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
    val defaults = remember(impact, landing) {
        GolfCamCurveSpec.suggestedControlPoints(
            start = NormalizedPoint(impact.normalizedX, impact.normalizedY),
            end = NormalizedPoint(landing.normalizedX, landing.normalizedY),
        )
    }

    var cp1x by remember(prev, impact, landing) {
        mutableStateOf(prev?.controlPoint1?.x ?: defaults.first.x)
    }
    var cp1y by remember(prev, impact, landing) {
        mutableStateOf(prev?.controlPoint1?.y ?: defaults.first.y)
    }
    var cp2x by remember(prev, impact, landing) {
        mutableStateOf(prev?.controlPoint2?.x ?: defaults.second.x)
    }
    var cp2y by remember(prev, impact, landing) {
        mutableStateOf(prev?.controlPoint2?.y ?: defaults.second.y)
    }
    var dragging by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(cp1x, cp1y, cp2x, cp2y) {
        onFlightCurveChange?.invoke(NormalizedPoint(cp1x, cp1y), NormalizedPoint(cp2x, cp2y))
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val touchTargetDp = 88.dp
        val touchTargetPx = with(density) { touchTargetDp.toPx() }
        val cp1HitCenter = visibleCurveHitCenter(NormalizedPoint(cp1x, cp1y), wPx, hPx, touchTargetPx)
        val cp2HitCenter = visibleCurveHitCenter(NormalizedPoint(cp2x, cp2y), wPx, hPx, touchTargetPx)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val p0 = Offset(impact.normalizedX * w, impact.normalizedY * h)
            val p1 = Offset(cp1x * w, cp1y * h)
            val p2 = Offset(cp2x * w, cp2y * h)
            val p3 = Offset(landing.normalizedX * w, landing.normalizedY * h)

            val helperDash = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx()))
            drawLine(Color.White.copy(alpha = 0.30f), p0, p1, strokeWidth = 1.dp.toPx(), pathEffect = helperDash)
            drawLine(Color.White.copy(alpha = 0.30f), p2, p3, strokeWidth = 1.dp.toPx(), pathEffect = helperDash)

            drawGolfCamCurveStroke(p0 = p0, p1 = p1, p2 = p2, p3 = p3)

            drawCircle(Color.White.copy(alpha = 0.9f), 4.5.dp.toPx(), p0)
            drawCircle(Color(0xFF8B5CF6).copy(alpha = 0.9f), 4.5.dp.toPx(), p3)
            drawCurveControlMarker(center = p1, dragging = dragging == 1, label = "P1")
            drawCurveControlMarker(center = p2, dragging = dragging == 2, label = "P2")
        }

        CurveControlHitTarget(
            targetSize = touchTargetDp,
            onDragStart = { dragging = 1 },
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
        )
        CurveControlHitTarget(
            targetSize = touchTargetDp,
            onDragStart = { dragging = 2 },
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
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDrag by rememberUpdatedState(onDrag)

    Box(
        modifier = modifier
            .size(targetSize)
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

private fun DrawScope.drawLandingMarker(anchor: Offset, active: Boolean) {
    val markerSize = if (active) 26.dp.toPx() else 24.dp.toPx()
    val anchorOffset = markerSize * LANDING_MARKER_ANCHOR_RATIO
    val cx = anchor.x
    val top = anchor.y - anchorOffset - markerSize / 2f
    val left = cx - markerSize / 2f
    fun arrowPath(offset: Offset = Offset.Zero) = Path().apply {
        moveTo(left + offset.x + markerSize * 0.35f, top + offset.y + markerSize * 0.08f)
        lineTo(left + offset.x + markerSize * 0.65f, top + offset.y + markerSize * 0.08f)
        lineTo(left + offset.x + markerSize * 0.65f, top + offset.y + markerSize * 0.46f)
        lineTo(left + offset.x + markerSize * 0.92f, top + offset.y + markerSize * 0.46f)
        lineTo(left + offset.x + markerSize * 0.50f, top + offset.y + markerSize * 0.94f)
        lineTo(left + offset.x + markerSize * 0.08f, top + offset.y + markerSize * 0.46f)
        lineTo(left + offset.x + markerSize * 0.35f, top + offset.y + markerSize * 0.46f)
        close()
    }
    val shadowOffset = Offset(0f, 2.dp.toPx())
    if (active) {
        drawCircle(Color.White.copy(alpha = 0.42f), radius = 18.dp.toPx(), center = anchor, style = Stroke(2.dp.toPx()))
    }
    drawPath(path = arrowPath(shadowOffset), color = Color.Black.copy(alpha = 0.50f))
    drawPath(path = arrowPath(), color = Color.White)
    drawCircle(Color.Black.copy(alpha = 0.55f), radius = 4.dp.toPx(), center = anchor + Offset(0f, 1.dp.toPx()))
    drawCircle(Color.White, radius = 3.dp.toPx(), center = anchor)
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
