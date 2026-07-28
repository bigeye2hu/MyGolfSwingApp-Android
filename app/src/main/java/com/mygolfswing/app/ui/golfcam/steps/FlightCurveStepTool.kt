package com.mygolfswing.app.ui.golfcam.steps

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.GolfCamFrameTimeline
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.NormalizedPoint
import com.mygolfswing.app.ui.golfcam.render.GolfCamCurveSpec
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MAX_CURVE_THUMBS = 60
internal const val CURVE_CONTROL_HIT_TARGET_DP = 88f
internal const val CURVE_TOOL_VERTICAL_SPACING_DP = 6
internal const val CURVE_TOOL_VERTICAL_PADDING_DP = 8
internal const val CURVE_INFO_TITLE_FONT_SP = 11
internal const val CURVE_INFO_TIME_FONT_SP = 10
internal const val CURVE_CONTROL_DOT_SIZE_DP = 6
internal const val CURVE_CONTROL_DOT_STROKE_DP = 1
internal const val CURVE_CONTROL_READOUT_SPACING_DP = 4
internal const val CURVE_CONTROL_LABEL_FONT_SP = 10
internal const val CURVE_CONTROL_VALUE_FONT_SP = 9
internal const val CURVE_TIMELINE_HEIGHT_DP = 50
internal const val CURVE_TIMELINE_CELL_WIDTH_DP = 32
internal const val CURVE_TIMELINE_IMAGE_HEIGHT_DP = 45
internal const val CURVE_TIMELINE_CELL_SPACING_DP = 0
internal const val CURVE_TIMELINE_IMAGE_CORNER_RADIUS_DP = 2
internal const val CURVE_TIMELINE_SELECTION_HEIGHT_DP = 3
internal const val CURVE_TIMELINE_SELECTION_CORNER_RADIUS_DP = 1
internal const val CURVE_TIMELINE_UNSELECTED_ALPHA = 0.7f
internal const val CURVE_FRAME_BUTTON_WIDTH_DP = 50
internal const val CURVE_FRAME_BUTTON_ICON_SIZE_DP = 12
internal const val CURVE_FRAME_BUTTON_LABEL_FONT_SP = 9
internal const val CURVE_BOTTOM_ROW_SPACING_DP = 8
internal const val CURVE_HINT_ICON_SIZE_DP = 12
internal const val CURVE_HINT_FONT_SP = 11
internal const val CURVE_IMPACT_CHIP_CORNER_RADIUS_DP = 7
internal const val CURVE_IMPACT_CHIP_HORIZONTAL_PADDING_DP = 9
internal const val CURVE_IMPACT_CHIP_VERTICAL_PADDING_DP = 5
internal const val CURVE_IMPACT_CHIP_ICON_SIZE_DP = 9
internal const val CURVE_IMPACT_CHIP_LABEL_FONT_SP = 10

internal fun formatCurveTimeLabel(seconds: Double): String {
    val safeSeconds = seconds.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
    return String.format(Locale.US, "%.2fs", safeSeconds)
}

internal fun formatCurveControlPointLabel(xPct: Float, yPct: Float): String {
    val safeX = xPct.takeIf { it.isFinite() }?.coerceIn(0f, 100f) ?: 50f
    val safeY = yPct.takeIf { it.isFinite() }?.coerceIn(-100f, 150f) ?: 0f
    return String.format(Locale.US, "(%.0f%%,%.0f%%)", safeX, safeY)
}

internal fun curveThumbnailFrameIndices(
    totalFrames: Int,
    maxThumbs: Int = MAX_CURVE_THUMBS,
): List<Int> {
    if (totalFrames <= 0 || maxThumbs <= 0) return emptyList()
    if (totalFrames <= maxThumbs) return (0 until totalFrames).toList()
    val lastFrame = totalFrames - 1
    return (0 until maxThumbs)
        .map { index ->
            (index * lastFrame.toDouble() / (maxThumbs - 1)).roundToInt().coerceIn(0, lastFrame)
        }
        .distinct()
}

internal fun nearestCurveThumbnailIndex(frameIndex: Int, frameIndices: List<Int>): Int {
    if (frameIndices.isEmpty()) return 0
    return frameIndices.indices.minBy { index -> golfCamFrameIndexDistance(frameIndices[index], frameIndex) }
}

internal fun golfCamFrameIndexDistance(firstFrame: Int, secondFrame: Int): Long {
    return if (firstFrame >= secondFrame) {
        firstFrame.toLong() - secondFrame.toLong()
    } else {
        secondFrame.toLong() - firstFrame.toLong()
    }
}

internal fun curveTimelineFrameIndex(frameIndex: Int, totalFrames: Int): Int {
    val lastFrame = (totalFrames - 1).coerceAtLeast(0)
    return frameIndex.coerceIn(0, lastFrame)
}

internal fun curveThumbnailSampleTimeMs(
    trimStartMs: Long,
    trimEndMs: Long,
    frameRate: Double,
    frameIndex: Int,
): Long {
    val safeEndMs = GolfCamFrameTimeline.endExclusiveMsAfterStart(trimStartMs, trimEndMs)
    return GolfCamFrameTimeline.timeMsForFrame(trimStartMs, frameIndex, frameRate)
        .coerceIn(
            trimStartMs,
            GolfCamFrameTimeline.lastFrameTimeMsForRange(trimStartMs, safeEndMs, frameRate),
        )
}

internal fun flightCurveReadoutControlPoints(state: GolfCamWorkflowState): Pair<NormalizedPoint, NormalizedPoint> {
    state.flightCurve?.let { return it.controlPoint1 to it.controlPoint2 }
    val impact = state.impactPosition
    val landing = state.landing
    if (impact != null && landing != null) {
        return GolfCamCurveSpec.suggestedControlPoints(
            start = NormalizedPoint(impact.normalizedX, impact.normalizedY),
            end = NormalizedPoint(landing.normalizedX, landing.normalizedY),
        )
    }
    return NormalizedPoint(0.3f, 0.3f) to NormalizedPoint(0.7f, 0.2f)
}

/**
 * 步骤 5：飞行曲线调整（对齐 iOS FlightCurveStepTool）。
 * 视频画面负责拖动 P1/P2；工具区负责帧定位、缩略图浏览和击球帧快速回跳。
 */
@Composable
fun FlightCurveStepTool(
    videoUri: Uri,
    trimStartMs: Long,
    state: GolfCamWorkflowState,
    currentFrameIndex: Int = 0,
    totalFrames: Int = 0,
    frameRate: Double = 30.0,
    onFrameSelected: ((Int) -> Unit)? = null,
    onImpactFrame: (() -> Unit)? = null,
    onPrevFrame: (() -> Unit)? = null,
    onNextFrame: (() -> Unit)? = null,
) {
    val safeFrameRate = GolfCamFrameTimeline.safeFrameRate(frameRate)
    val safeCurrentFrameIndex = curveTimelineFrameIndex(currentFrameIndex, totalFrames)
    val (cp1, cp2) = flightCurveReadoutControlPoints(state)
    val trimEndMs = state.trimResult?.endMs
        ?: GolfCamFrameTimeline.timeMsAfterSeconds(
            trimStartMs,
            totalFrames.coerceAtLeast(1) / safeFrameRate,
        )
    val p1x = cp1.x * 100f
    val p1y = cp1.y * 100f
    val p2x = cp2.x * 100f
    val p2y = cp2.y * 100f
    val currentSec = safeCurrentFrameIndex / safeFrameRate

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = CURVE_TOOL_VERTICAL_PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(CURVE_TOOL_VERTICAL_SPACING_DP.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    if (totalFrames > 0) "帧 ${safeCurrentFrameIndex + 1}/$totalFrames" else "飞行曲线",
                    color = DSColors.inkGreen,
                    fontSize = CURVE_INFO_TITLE_FONT_SP.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    formatCurveTimeLabel(currentSec),
                    color = DSColors.shadowGray,
                    fontSize = CURVE_INFO_TIME_FONT_SP.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            ControlPointReadout(label = "P1", xPct = p1x, yPct = p1y)
            ControlPointReadout(label = "P2", xPct = p2x, yPct = p2y)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(CURVE_TIMELINE_HEIGHT_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FrameStepButton(
                enabled = currentFrameIndex > 0,
                icon = Icons.Filled.ChevronLeft,
                label = "上一帧",
                onClick = { onPrevFrame?.invoke() },
            )

            CurveThumbnailTimeline(
                videoUri = videoUri,
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs,
                currentFrameIndex = safeCurrentFrameIndex,
                totalFrames = totalFrames,
                frameRate = safeFrameRate,
                onFrameSelected = { onFrameSelected?.invoke(it) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp),
            )

            FrameStepButton(
                enabled = totalFrames == 0 || currentFrameIndex < totalFrames - 1,
                icon = Icons.Filled.ChevronRight,
                label = "下一帧",
                onClick = { onNextFrame?.invoke() },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CURVE_BOTTOM_ROW_SPACING_DP.dp),
        ) {
            Icon(Icons.Filled.TouchApp, null, tint = DSColors.fairway, modifier = Modifier.size(CURVE_HINT_ICON_SIZE_DP.dp))
            Text(
                "滑动查看球飞行轨迹，调整曲线匹配实际弹道",
                color = DSColors.shadowGray,
                fontSize = CURVE_HINT_FONT_SP.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            CurveActionChip(icon = Icons.Filled.Bolt, label = "击球帧", active = true, onClick = { onImpactFrame?.invoke() })
        }
    }
}

@Composable
private fun ControlPointReadout(label: String, xPct: Float, yPct: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CURVE_CONTROL_READOUT_SPACING_DP.dp),
    ) {
        Box(
            modifier = Modifier
                .size(CURVE_CONTROL_DOT_SIZE_DP.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(CURVE_CONTROL_DOT_STROKE_DP.dp, DSColors.fairway, CircleShape),
        )
        Text(label, color = DSColors.inkGreen, fontSize = CURVE_CONTROL_LABEL_FONT_SP.sp, fontWeight = FontWeight.Medium)
        Text(
            formatCurveControlPointLabel(xPct, yPct),
            color = DSColors.shadowGray,
            fontSize = CURVE_CONTROL_VALUE_FONT_SP.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun FrameStepButton(
    enabled: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) DSColors.inkGreen else DSColors.shadowGray.copy(alpha = 0.42f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(CURVE_FRAME_BUTTON_WIDTH_DP.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) Color.White else DSColors.mistGreen.copy(alpha = 0.52f))
            .border(
                width = 1.dp,
                color = if (enabled) DSColors.fairway.copy(alpha = 0.16f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 4.dp),
    ) {
        Icon(
            icon,
            null,
            tint = contentColor,
            modifier = Modifier.size(CURVE_FRAME_BUTTON_ICON_SIZE_DP.dp),
        )
        Text(
            label,
            color = contentColor.copy(alpha = if (enabled) 0.72f else 1f),
            fontSize = CURVE_FRAME_BUTTON_LABEL_FONT_SP.sp,
        )
    }
}

@Composable
private fun CurveActionChip(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(CURVE_IMPACT_CHIP_CORNER_RADIUS_DP.dp))
            .background(if (active) DSColors.fairway.copy(alpha = 0.15f) else DSColors.mistGreen.copy(alpha = 0.55f))
            .then(
                if (active) {
                    Modifier.border(
                        1.dp,
                        DSColors.fairway.copy(alpha = 0.18f),
                        RoundedCornerShape(CURVE_IMPACT_CHIP_CORNER_RADIUS_DP.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(
                horizontal = CURVE_IMPACT_CHIP_HORIZONTAL_PADDING_DP.dp,
                vertical = CURVE_IMPACT_CHIP_VERTICAL_PADDING_DP.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = if (active) DSColors.fairway else DSColors.shadowGray, modifier = Modifier.size(CURVE_IMPACT_CHIP_ICON_SIZE_DP.dp))
        Text(
            label,
            color = if (active) DSColors.fairway else DSColors.shadowGray,
            fontSize = CURVE_IMPACT_CHIP_LABEL_FONT_SP.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CurveThumbnailTimeline(
    videoUri: Uri,
    trimStartMs: Long,
    trimEndMs: Long,
    currentFrameIndex: Int,
    totalFrames: Int,
    frameRate: Double,
    onFrameSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val safeFrameRate = GolfCamFrameTimeline.safeFrameRate(frameRate)
    val thumbnailFrames = remember(totalFrames) { curveThumbnailFrameIndices(totalFrames) }
    val thumbnailSampleTimesMs = remember(trimStartMs, trimEndMs, safeFrameRate, thumbnailFrames) {
        thumbnailFrames.map { frameIndex ->
            curveThumbnailSampleTimeMs(
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs,
                frameRate = safeFrameRate,
                frameIndex = frameIndex,
            )
        }
    }
    var thumbnails by remember(videoUri, thumbnailFrames, thumbnailSampleTimesMs) {
        val cached = GolfCamThumbnailCache.cachedTimelineThumbnails(videoUri, thumbnailSampleTimesMs)
        mutableStateOf(
            thumbnailFrames.mapIndexed { index, frameIndex ->
                CurveThumb(frameIndex = frameIndex, bitmap = cached.getOrNull(index)?.asImageBitmap())
            },
        )
    }
    val priorityTimeMs = curveThumbnailSampleTimeMs(
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        frameRate = safeFrameRate,
        frameIndex = currentFrameIndex,
    )
    val priorityThumbnailIndex = remember(thumbnailSampleTimesMs, priorityTimeMs) {
        golfCamThumbnailPriorityIndex(thumbnailSampleTimesMs, priorityTimeMs)
    }
    val prioritySampleTimeMs = remember(thumbnailSampleTimesMs, priorityThumbnailIndex, priorityTimeMs) {
        golfCamThumbnailPrioritySampleTimeMs(thumbnailSampleTimesMs, priorityTimeMs)
    }

    LaunchedEffect(videoUri, thumbnailFrames, thumbnailSampleTimesMs, priorityThumbnailIndex) {
        if (thumbnailFrames.isEmpty()) return@LaunchedEffect
        val exactCachedSampleTimes = GolfCamThumbnailCache.cachedExactSampleTimes(videoUri, thumbnailSampleTimesMs)
        val cached = GolfCamThumbnailCache.cachedTimelineThumbnails(videoUri, thumbnailSampleTimesMs)
        thumbnails = thumbnailFrames.mapIndexed { index, frameIndex ->
            CurveThumb(frameIndex = frameIndex, bitmap = cached.getOrNull(index)?.asImageBitmap())
        }
        GolfCamThumbnailCache.loadTimelineThumbnails(
            context = context,
            videoUri = videoUri,
            sampleTimesMs = thumbnailSampleTimesMs,
            priorityTimeMs = prioritySampleTimeMs,
            skipCachedSampleTimesMs = exactCachedSampleTimes,
        ) { index, bitmap ->
            thumbnails = fillMissingCurveThumbnailsFromNearest(thumbnails.toMutableList().also { current ->
                if (index in current.indices) {
                    current[index] = current[index].copy(bitmap = bitmap.asImageBitmap())
                }
            })
        }
    }

    fun frameToThumbIndex(frameIndex: Int): Int {
        if (thumbnails.isEmpty()) return 0
        return nearestCurveThumbnailIndex(frameIndex, thumbnails.map { it.frameIndex })
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = frameToThumbIndex(currentFrameIndex))

    LaunchedEffect(listState, thumbnails) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@snapshotFlow -1
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
            visibleItems.minByOrNull { abs((it.offset + it.size / 2f) - viewportCenter) }?.index ?: -1
        }.distinctUntilChanged()
            .collect { thumbIndex ->
                if (thumbIndex in thumbnails.indices && listState.isScrollInProgress) {
                    onFrameSelected(thumbnails[thumbIndex].frameIndex)
                }
            }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF2F2F2F)),
    ) {
        val cellWidth = CURVE_TIMELINE_CELL_WIDTH_DP.dp
        val sidePad = ((maxWidth - cellWidth) / 2).coerceAtLeast(0.dp)
        val sidePadPx = with(LocalDensity.current) { sidePad.toPx().toInt() }
        val snapFlingBehavior = rememberSnapFlingBehavior(listState)

        LaunchedEffect(currentFrameIndex, sidePadPx, thumbnails.size) {
            if (thumbnails.isNotEmpty() && !listState.isScrollInProgress) {
                listState.animateScrollToItem(frameToThumbIndex(currentFrameIndex), scrollOffset = -sidePadPx)
            }
        }

        LazyRow(
            state = listState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(horizontal = sidePad),
            horizontalArrangement = Arrangement.spacedBy(CURVE_TIMELINE_CELL_SPACING_DP.dp),
            modifier = Modifier.matchParentSize(),
        ) {
            itemsIndexed(thumbnails) { thumbIndex, thumb ->
                val selected = thumbIndex == frameToThumbIndex(currentFrameIndex)
                Box(
                    modifier = Modifier
                        .width(cellWidth)
                        .fillMaxHeight()
                        .clickable { onFrameSelected(thumb.frameIndex) },
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(CURVE_TIMELINE_IMAGE_HEIGHT_DP.dp)
                            .clip(RoundedCornerShape(CURVE_TIMELINE_IMAGE_CORNER_RADIUS_DP.dp))
                            .background(Color(0xFF3A3A3A)),
                    ) {
                        if (thumb.bitmap != null) {
                            Image(
                                bitmap = thumb.bitmap,
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop,
                                alpha = if (selected) 1f else CURVE_TIMELINE_UNSELECTED_ALPHA,
                            )
                        }
                    }
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(CURVE_TIMELINE_SELECTION_HEIGHT_DP.dp)
                                .clip(RoundedCornerShape(CURVE_TIMELINE_SELECTION_CORNER_RADIUS_DP.dp))
                                .background(DSColors.fairway),
                        )
                    }
                }
            }
        }
    }
}

private data class CurveThumb(val frameIndex: Int, val bitmap: ImageBitmap?)

private fun fillMissingCurveThumbnailsFromNearest(thumbnails: List<CurveThumb>): List<CurveThumb> {
    val filledBitmaps = golfCamFillMissingThumbnailSlotsFromNearest(thumbnails.map { it.bitmap })
    return thumbnails.mapIndexed { index, thumb ->
        thumb.copy(bitmap = filledBitmaps.getOrNull(index))
    }
}
