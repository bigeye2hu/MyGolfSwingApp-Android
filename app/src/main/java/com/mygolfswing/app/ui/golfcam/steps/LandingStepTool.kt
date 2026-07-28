package com.mygolfswing.app.ui.golfcam.steps

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.R
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.GolfCamFrameTimeline
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.LandingOutput
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MIN_FLIGHT_SEC = 0.5
private const val MAX_FLIGHT_SEC = 8.0
private const val DEFAULT_FLIGHT_SEC = 4.5
internal const val LANDING_THUMB_COUNT = 12
internal const val LANDING_TIMELINE_HEIGHT_DP = 56
internal const val LANDING_TIMELINE_THUMB_WIDTH_DP = 32
internal const val LANDING_TIMELINE_PIXELS_PER_SECOND_DP = 80
internal const val LANDING_TIMELINE_TRACK_CORNER_RADIUS_DP = 4
internal const val LANDING_CENTER_INDICATOR_WIDTH_DP = 20
internal const val LANDING_CENTER_INDICATOR_BAR_WIDTH_DP = 4
internal const val LANDING_CENTER_INDICATOR_DOT_SIZE_DP = 8
internal const val LANDING_CENTER_INDICATOR_VERTICAL_INSET_DP = 4
internal const val LANDING_VIRTUAL_THUMBNAIL_ALPHA = 0.6f
internal const val LANDING_VIRTUAL_OVERLAY_ALPHA = 0.4f
internal const val LANDING_MIN_TIME_MARKER_WIDTH_DP = 2
internal const val LANDING_MIN_TIME_LABEL_FONT_SP = 9
internal const val LANDING_TOOL_VERTICAL_SPACING_DP = 6
internal const val LANDING_TOOL_LABEL_FONT_SP = 11
internal const val LANDING_TOOL_VALUE_FONT_SP = 14
internal const val LANDING_TOOL_MARK_SIZE_DP = 16
internal const val LANDING_TOOL_MARK_SPACING_DP = 6
internal const val LANDING_TOOL_TITLE_FONT_SP = 13
internal const val LANDING_VIRTUAL_BADGE_FONT_SP = 10
internal const val LANDING_VIRTUAL_BADGE_CORNER_RADIUS_DP = 4
internal const val LANDING_VIRTUAL_BADGE_HORIZONTAL_PADDING_DP = 6
internal const val LANDING_VIRTUAL_BADGE_VERTICAL_PADDING_DP = 2
internal const val LANDING_HINT_ICON_SIZE_DP = 14
internal const val LANDING_HINT_SPACING_DP = 8
internal const val LANDING_HINT_FONT_SP = 12
private val VIRTUAL_BLUE = Color(0xFF4285F4)

private fun finiteDoubleIn(value: Double, min: Double, max: Double, fallback: Double): Double {
    return value.takeIf { it.isFinite() }?.coerceIn(min, max) ?: fallback.coerceIn(min, max)
}

private fun finiteFloatIn(value: Float, min: Float, max: Float, fallback: Float): Float {
    return value.takeIf { it.isFinite() }?.coerceIn(min, max) ?: fallback.coerceIn(min, max)
}

internal fun landingTailSeconds(impactTimeMs: Long, clipEndMs: Long): Double {
    return ((clipEndMs - impactTimeMs) / 1000.0)
        .coerceAtLeast(MIN_FLIGHT_SEC)
        .coerceAtMost(MAX_FLIGHT_SEC)
}

internal fun landingFlightTimeSeconds(landing: LandingOutput?): Double {
    return finiteDoubleIn(
        value = landing?.flightTimeSec ?: DEFAULT_FLIGHT_SEC,
        min = MIN_FLIGHT_SEC,
        max = MAX_FLIGHT_SEC,
        fallback = DEFAULT_FLIGHT_SEC,
    )
}

internal fun landingOutputForTimeChange(current: LandingOutput?, flightTimeSec: Double): LandingOutput {
    return (current ?: LandingOutput(0.5f, 0.5f, DEFAULT_FLIGHT_SEC)).copy(
        flightTimeSec = finiteDoubleIn(flightTimeSec, MIN_FLIGHT_SEC, MAX_FLIGHT_SEC, DEFAULT_FLIGHT_SEC),
    )
}

internal fun landingOutputForPositionChange(
    current: LandingOutput?,
    normalizedX: Float,
    normalizedY: Float,
): LandingOutput {
    return LandingOutput(
        normalizedX = finiteFloatIn(normalizedX, 0f, 1f, fallback = 0.5f),
        normalizedY = finiteFloatIn(normalizedY, 0f, 1f, fallback = 0.5f),
        flightTimeSec = landingFlightTimeSeconds(current),
    )
}

internal fun isLandingVirtualSegment(flightTimeSec: Double, tailSeconds: Double): Boolean {
    val safeFlightTime = finiteDoubleIn(flightTimeSec, MIN_FLIGHT_SEC, MAX_FLIGHT_SEC, DEFAULT_FLIGHT_SEC)
    val safeTailSeconds = finiteDoubleIn(tailSeconds, MIN_FLIGHT_SEC, MAX_FLIGHT_SEC, MIN_FLIGHT_SEC)
    return safeFlightTime > safeTailSeconds
}

internal fun formatLandingTimeLabel(flightTimeSec: Double): String {
    return String.format(Locale.US, "%.1fs", finiteDoubleIn(flightTimeSec, MIN_FLIGHT_SEC, MAX_FLIGHT_SEC, DEFAULT_FLIGHT_SEC))
}

internal fun formatLandingPositionLabel(xPct: Float, yPct: Float): String {
    return String.format(
        Locale.US,
        "%.0f%%, %.0f%%",
        finiteFloatIn(xPct, 0f, 100f, fallback = 50f),
        finiteFloatIn(yPct, 0f, 100f, fallback = 50f),
    )
}

internal fun landingSeekTimeMs(
    trimStartMs: Long,
    impactTimeMs: Long,
    flightTimeSec: Double,
    clipEndMs: Long,
    frameRate: Double,
): Long {
    val safeFlightTime = finiteDoubleIn(flightTimeSec, MIN_FLIGHT_SEC, MAX_FLIGHT_SEC, DEFAULT_FLIGHT_SEC)
    val targetMs = GolfCamFrameTimeline.timeMsAfterSeconds(impactTimeMs, safeFlightTime)
    val lastSeekMs = GolfCamFrameTimeline.lastFrameTimeMsForRange(trimStartMs, clipEndMs, frameRate)
    return targetMs.coerceIn(trimStartMs.coerceAtLeast(0L), lastSeekMs.coerceAtLeast(trimStartMs))
}

/**
 * 步骤 4：落点选择（对齐 iOS LandingStepTool）。
 * 工具区显示飞行时间、落点坐标、中心固定的缩略图时间轴和虚拟段提示；
 * 落点位置仍在上方视频画面中拖动。
 */
@Composable
fun LandingStepTool(
    videoUri: Uri,
    state: GolfCamWorkflowState = GolfCamWorkflowState(),
    frameRate: Double = 30.0,
    onFlightTimeChange: ((Double) -> Unit)? = null,
) {
    val landing = state.landing
    val trimStartMs = state.trimResult?.startMs ?: 0L
    val impactTimeMs = state.impactFrame?.timeMs ?: state.trimResult?.startMs ?: 0L
    val clipEndMs = state.trimResult?.endMs ?: 0L
    val tTail = landingTailSeconds(impactTimeMs, clipEndMs)

    val xPct = (landing?.normalizedX ?: 0.5f) * 100f
    val yPct = (landing?.normalizedY ?: 0.5f) * 100f
    val flightTime = landingFlightTimeSeconds(landing)
    var sliderValue by remember(flightTime) { mutableDoubleStateOf(flightTime) }
    val isInVirtualSegment = isLandingVirtualSegment(sliderValue, tTail)

    LaunchedEffect(Unit) {
        if (landing == null) onFlightTimeChange?.invoke(sliderValue)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LANDING_TOOL_VERTICAL_SPACING_DP.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text("飞行时间", color = DSColors.shadowGray, fontSize = LANDING_TOOL_LABEL_FONT_SP.sp)
                Text(
                    formatLandingTimeLabel(sliderValue),
                    color = if (isInVirtualSegment) VIRTUAL_BLUE else DSColors.inkGreen,
                    fontSize = LANDING_TOOL_VALUE_FONT_SP.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LANDING_TOOL_MARK_SPACING_DP.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.landing_mark),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(DSColors.cardWhite),
                    modifier = Modifier.size(LANDING_TOOL_MARK_SIZE_DP.dp),
                )
                Text(
                    "落点",
                    color = DSColors.inkGreen,
                    fontSize = LANDING_TOOL_TITLE_FONT_SP.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (isInVirtualSegment) {
                    Text(
                        "虚拟段",
                        color = VIRTUAL_BLUE,
                        fontSize = LANDING_VIRTUAL_BADGE_FONT_SP.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(LANDING_VIRTUAL_BADGE_CORNER_RADIUS_DP.dp))
                            .background(VIRTUAL_BLUE.copy(alpha = 0.2f))
                            .padding(
                                horizontal = LANDING_VIRTUAL_BADGE_HORIZONTAL_PADDING_DP.dp,
                                vertical = LANDING_VIRTUAL_BADGE_VERTICAL_PADDING_DP.dp,
                            ),
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("位置", color = DSColors.shadowGray, fontSize = LANDING_TOOL_LABEL_FONT_SP.sp)
                Text(
                    formatLandingPositionLabel(xPct, yPct),
                    color = DSColors.inkGreen,
                    fontSize = LANDING_TOOL_VALUE_FONT_SP.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        LandingThumbnailTimeline(
            videoUri = videoUri,
            trimStartMs = trimStartMs,
            impactTimeMs = impactTimeMs,
            clipEndMs = clipEndMs,
            frameRate = frameRate,
            flightTimeSec = sliderValue,
            tTailSec = tTail,
            onTimeChanged = { newTime ->
                sliderValue = newTime
                onFlightTimeChange?.invoke(newTime)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(LANDING_TIMELINE_HEIGHT_DP.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LANDING_HINT_SPACING_DP.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Filled.LocationOn,
                null,
                tint = DSColors.fairway,
                modifier = Modifier.size(LANDING_HINT_ICON_SIZE_DP.dp),
            )
            Text(
                "滑动时间轴选择落点时间，拖动画面中的落点标记",
                color = DSColors.shadowGray,
                fontSize = LANDING_HINT_FONT_SP.sp,
            )
        }
    }
}

@Composable
private fun LandingThumbnailTimeline(
    videoUri: Uri,
    trimStartMs: Long,
    impactTimeMs: Long,
    clipEndMs: Long,
    frameRate: Double,
    flightTimeSec: Double,
    tTailSec: Double,
    onTimeChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val thumbnailSampleTimesMs = remember(trimStartMs, impactTimeMs, clipEndMs, frameRate) {
        List(LANDING_THUMB_COUNT) { index ->
            val relSec = index * MAX_FLIGHT_SEC / (LANDING_THUMB_COUNT - 1)
            landingSeekTimeMs(
                trimStartMs = trimStartMs,
                impactTimeMs = impactTimeMs,
                flightTimeSec = relSec,
                clipEndMs = clipEndMs,
                frameRate = frameRate,
            )
        }
    }
    var thumbnails by remember(videoUri, thumbnailSampleTimesMs) {
        mutableStateOf(
            GolfCamThumbnailCache.cachedTimelineThumbnails(videoUri, thumbnailSampleTimesMs)
                .map { it?.asImageBitmap() },
        )
    }
    val priorityTimeMs = landingSeekTimeMs(
        trimStartMs = trimStartMs,
        impactTimeMs = impactTimeMs,
        flightTimeSec = flightTimeSec,
        clipEndMs = clipEndMs,
        frameRate = frameRate,
    )
    val priorityThumbnailIndex = remember(thumbnailSampleTimesMs, priorityTimeMs) {
        golfCamThumbnailPriorityIndex(thumbnailSampleTimesMs, priorityTimeMs)
    }
    val prioritySampleTimeMs = remember(thumbnailSampleTimesMs, priorityThumbnailIndex, priorityTimeMs) {
        golfCamThumbnailPrioritySampleTimeMs(thumbnailSampleTimesMs, priorityTimeMs)
    }

    LaunchedEffect(videoUri, thumbnailSampleTimesMs, priorityThumbnailIndex) {
        val exactCachedSampleTimes = GolfCamThumbnailCache.cachedExactSampleTimes(videoUri, thumbnailSampleTimesMs)
        thumbnails = GolfCamThumbnailCache.cachedTimelineThumbnails(videoUri, thumbnailSampleTimesMs)
            .map { it?.asImageBitmap() }
        GolfCamThumbnailCache.loadTimelineThumbnails(
            context = context,
            videoUri = videoUri,
            sampleTimesMs = thumbnailSampleTimesMs,
            priorityTimeMs = prioritySampleTimeMs,
            skipCachedSampleTimesMs = exactCachedSampleTimes,
        ) { index, bitmap ->
            thumbnails = golfCamFillMissingThumbnailSlotsFromNearest(
                thumbnails.toMutableList().also { current ->
                    if (index in current.indices) current[index] = bitmap.asImageBitmap()
                },
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(LANDING_TIMELINE_TRACK_CORNER_RADIUS_DP.dp))
            .background(Color(0xFF262626)),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val pixelsPerSecond = with(density) { LANDING_TIMELINE_PIXELS_PER_SECOND_DP.dp.toPx() }
        val thumbWidthPx = with(density) { LANDING_TIMELINE_THUMB_WIDTH_DP.dp.toPx() }
        val centerX = widthPx / 2f

        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(widthPx, flightTimeSec) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val next = (flightTimeSec - dragAmount.x / pixelsPerSecond)
                            .coerceIn(MIN_FLIGHT_SEC, MAX_FLIGHT_SEC)
                        onTimeChanged(next)
                    }
                },
        ) {
            drawLandingFilmstrip(
                thumbnails = thumbnails,
                flightTimeSec = flightTimeSec,
                tTailSec = tTailSec,
                pixelsPerSecond = pixelsPerSecond,
                thumbWidthPx = thumbWidthPx,
                centerX = centerX,
            )
        }

        CenterTimeIndicator(modifier = Modifier.matchParentSize())
    }
}

private fun DrawScope.drawLandingFilmstrip(
    thumbnails: List<ImageBitmap?>,
    flightTimeSec: Double,
    tTailSec: Double,
    pixelsPerSecond: Float,
    thumbWidthPx: Float,
    centerX: Float,
) {
    val firstColumn = floor((flightTimeSec * pixelsPerSecond - centerX) / thumbWidthPx).toInt() - 1
    val columnCount = ceil(size.width / thumbWidthPx).toInt() + 4

    for (column in firstColumn until firstColumn + columnCount) {
        val startTime = column * thumbWidthPx / pixelsPerSecond
        val x = centerX + ((startTime - flightTimeSec) * pixelsPerSecond).toFloat()
        val sampleTime = startTime.toDouble().coerceIn(0.0, MAX_FLIGHT_SEC)
        val virtual = sampleTime > tTailSec
        val imageIndex = ((sampleTime / MAX_FLIGHT_SEC) * (LANDING_THUMB_COUNT - 1))
            .roundToInt()
            .coerceIn(0, LANDING_THUMB_COUNT - 1)

        drawRect(
            color = Color(0xFF333333),
            topLeft = Offset(x, 0f),
            size = Size(thumbWidthPx - 1f, size.height),
        )
        thumbnails.getOrNull(imageIndex)?.let { image ->
            drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(image.width, image.height),
                dstOffset = IntOffset(x.roundToInt(), 0),
                dstSize = IntSize(max(1, thumbWidthPx.roundToInt()), size.height.roundToInt()),
                filterQuality = FilterQuality.Low,
                alpha = if (virtual) LANDING_VIRTUAL_THUMBNAIL_ALPHA else 1f,
            )
        }
        if (virtual) {
            drawRect(
                color = VIRTUAL_BLUE.copy(alpha = LANDING_VIRTUAL_OVERLAY_ALPHA),
                topLeft = Offset(x, 0f),
                size = Size(thumbWidthPx - 1f, size.height),
            )
        }
    }

    drawTimeMarker(0.5, flightTimeSec, pixelsPerSecond, centerX, Color.White.copy(alpha = 0.7f), "0.5s")
    if (tTailSec < MAX_FLIGHT_SEC) {
        drawTimeMarker(tTailSec, flightTimeSec, pixelsPerSecond, centerX, VIRTUAL_BLUE, null)
    }
}

private fun DrawScope.drawTimeMarker(
    timeSec: Double,
    flightTimeSec: Double,
    pixelsPerSecond: Float,
    centerX: Float,
    color: Color,
    label: String?,
) {
    val x = centerX + ((timeSec - flightTimeSec) * pixelsPerSecond).toFloat()
    if (x < -8f || x > size.width + 8f) return
    val markerWidth = LANDING_MIN_TIME_MARKER_WIDTH_DP.dp.toPx()
    drawRect(color = color, topLeft = Offset(x - markerWidth / 2f, 0f), size = Size(markerWidth, size.height))
    if (label != null) {
        drawContext.canvas.nativeCanvas.drawText(
            label,
            x + 5f,
            14f,
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = android.graphics.Color.argb(210, 255, 255, 255)
                textSize = LANDING_MIN_TIME_LABEL_FONT_SP.sp.toPx()
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            },
        )
    }
}

@Composable
private fun CenterTimeIndicator(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val verticalInset = LANDING_CENTER_INDICATOR_VERTICAL_INSET_DP.dp.toPx()
        val barWidth = LANDING_CENTER_INDICATOR_BAR_WIDTH_DP.dp.toPx()
        val dotRadius = LANDING_CENTER_INDICATOR_DOT_SIZE_DP.dp.toPx() / 2f
        val barTop = verticalInset
        val barBottom = size.height - verticalInset
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(cx - barWidth / 2f, barTop),
            size = Size(barWidth, barBottom - barTop),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
        )
        drawCircle(Color.White, radius = dotRadius, center = Offset(cx, dotRadius))
        drawCircle(Color.White, radius = dotRadius, center = Offset(cx, size.height - dotRadius))
    }
}
