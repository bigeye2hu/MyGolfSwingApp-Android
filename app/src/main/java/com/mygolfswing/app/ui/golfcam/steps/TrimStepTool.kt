package com.mygolfswing.app.ui.golfcam.steps

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.GolfCamFrameTimeline
import com.mygolfswing.app.ui.golfcam.model.TrimResult
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

internal const val TRIM_THUMBNAIL_COUNT = 15
internal const val TRIM_SAFE_MARGIN_DP = 16
internal const val TRIM_HANDLE_WIDTH_DP = 12
internal const val TRIM_BAR_HEIGHT_DP = 56
internal const val TRIM_HANDLE_TOUCH_EXTRA_DP = 16
internal const val TRIM_DIM_OVERLAY_ALPHA = 0.60f
internal const val TRIM_SELECTION_BORDER_STROKE_DP = 1.5f
internal const val TRIM_SELECTION_FILL_ALPHA = 0.08f
internal const val TRIM_SELECTION_CORNER_RADIUS_DP = 6
internal const val TRIM_STRIP_CORNER_RADIUS_DP = 4
private const val DEFAULT_TRIM_MAX_MS = 30_000L
private const val MIN_TRIM_DURATION_MS = 500L

// 安全边距：条的左右各留 SAFE_MARGIN，让把手远离屏幕边缘手势区
private val SAFE_MARGIN: Dp = TRIM_SAFE_MARGIN_DP.dp
private val HANDLE_W: Dp = TRIM_HANDLE_WIDTH_DP.dp
private val BAR_HEIGHT: Dp = TRIM_BAR_HEIGHT_DP.dp
private val HANDLE_TOUCH_EXTRA: Dp = TRIM_HANDLE_TOUCH_EXTRA_DP.dp

internal fun defaultTrimEndFraction(durationMs: Long): Float {
    if (durationMs <= 0L) return 1f
    return (min(durationMs, DEFAULT_TRIM_MAX_MS).toFloat() / durationMs).coerceIn(0f, 1f)
}

internal fun minimumTrimFraction(durationMs: Long): Float {
    if (durationMs <= 0L) return 1f
    return (MIN_TRIM_DURATION_MS.toFloat() / durationMs).coerceIn(0f, 1f)
}

internal fun trimFractionOrDefault(fraction: Float, fallback: Float): Float {
    val safeFallback = if (fallback.isFinite()) fallback.coerceIn(0f, 1f) else 0f
    return (fraction.takeIf { it.isFinite() } ?: safeFallback).coerceIn(0f, 1f)
}

internal fun clampTrimStartFraction(newFraction: Float, endFraction: Float, durationMs: Long): Float {
    val safeEnd = trimFractionOrDefault(endFraction, defaultTrimEndFraction(durationMs))
    val maxStart = max(0f, safeEnd - minimumTrimFraction(durationMs))
    return trimFractionOrDefault(newFraction, fallback = 0f).coerceIn(0f, maxStart)
}

internal fun clampTrimEndFraction(newFraction: Float, startFraction: Float, durationMs: Long): Float {
    val safeStart = trimFractionOrDefault(startFraction, fallback = 0f)
    val minEnd = min(1f, safeStart + minimumTrimFraction(durationMs))
    return trimFractionOrDefault(newFraction, fallback = 1f).coerceIn(minEnd, 1f)
}

internal fun initialTrimFractions(durationMs: Long, saved: TrimResult?): Pair<Float, Float> {
    if (durationMs <= 0L) return 0f to 1f
    val minDurationMs = min(durationMs, MIN_TRIM_DURATION_MS)
    val savedStart = saved?.startMs ?: return 0f to defaultTrimEndFraction(durationMs)
    val savedEnd = saved.endMs
    val validSavedRange = savedStart >= 0L &&
        savedEnd <= durationMs &&
        savedEnd - savedStart >= minDurationMs
    if (!validSavedRange) return 0f to defaultTrimEndFraction(durationMs)
    return (savedStart.toFloat() / durationMs).coerceIn(0f, 1f) to
        (savedEnd.toFloat() / durationMs).coerceIn(0f, 1f)
}

internal fun trimSeekTimeMs(durationMs: Long, fraction: Float): Long {
    if (durationMs <= 0L) return 0L
    return (trimFractionOrDefault(fraction, fallback = 0f) * durationMs).toLong().coerceIn(0L, durationMs)
}

internal fun trimPlayheadFraction(
    durationMs: Long,
    currentTimeMs: Long,
    startFraction: Float,
    endFraction: Float,
): Float? {
    if (durationMs <= 0L) return null
    val currentFraction = (currentTimeMs.toFloat() / durationMs).coerceIn(0f, 1f)
    val safeStart = trimFractionOrDefault(startFraction, fallback = 0f)
    val safeEnd = trimFractionOrDefault(endFraction, fallback = 1f).coerceAtLeast(safeStart)
    return if (currentFraction in safeStart..safeEnd) currentFraction else null
}

internal fun trimPlayheadSeekTimeMs(
    durationMs: Long,
    fraction: Float,
    startFraction: Float,
    endFraction: Float,
): Long {
    val safeStart = trimFractionOrDefault(startFraction, fallback = 0f)
    val safeEnd = trimFractionOrDefault(endFraction, fallback = 1f).coerceAtLeast(safeStart)
    val clampedFraction = trimFractionOrDefault(fraction, fallback = safeStart).coerceIn(safeStart, safeEnd)
    return trimSeekTimeMs(durationMs, clampedFraction)
}

internal fun trimResultFromFractions(
    durationMs: Long,
    startFraction: Float,
    endFraction: Float,
): TrimResult? {
    if (durationMs <= 0L) return null
    val minDurationMs = min(durationMs, MIN_TRIM_DURATION_MS)
    var safeEndFraction = trimFractionOrDefault(endFraction, fallback = defaultTrimEndFraction(durationMs))
    val safeStartFraction = clampTrimStartFraction(
        newFraction = trimFractionOrDefault(startFraction, fallback = 0f),
        endFraction = safeEndFraction,
        durationMs = durationMs,
    )
    safeEndFraction = clampTrimEndFraction(
        newFraction = safeEndFraction,
        startFraction = safeStartFraction,
        durationMs = durationMs,
    )
    var startMs = trimSeekTimeMs(durationMs, safeStartFraction)
    var endMs = trimSeekTimeMs(durationMs, safeEndFraction)
    if (endMs - startMs < minDurationMs) {
        val minimumEndMs = GolfCamFrameTimeline.timeMsAfter(startMs, minDurationMs)
        if (minimumEndMs <= durationMs) {
            endMs = minimumEndMs
        } else {
            startMs = (endMs - minDurationMs).coerceAtLeast(0L)
        }
    }
    val minimumEndMs = GolfCamFrameTimeline.timeMsAfter(startMs, minDurationMs)
    return TrimResult(startMs = startMs, endMs = endMs.coerceIn(minimumEndMs, durationMs))
}

internal fun trimThumbnailSampleTimesUs(
    durationMs: Long,
    thumbnailCount: Int = TRIM_THUMBNAIL_COUNT,
): List<Long> {
    if (durationMs <= 0L || thumbnailCount <= 0) return emptyList()
    val count = thumbnailCount.coerceAtMost(TRIM_THUMBNAIL_COUNT)
    val intervalMs = durationMs.toDouble() / count.toDouble()
    return (0 until count).map { index ->
        val midpointMs = ((index.toDouble() + 0.5) * intervalMs)
            .roundToLong()
            .coerceIn(0L, (durationMs - 1L).coerceAtLeast(0L))
        golfCamThumbnailRetrieverTimeUs(midpointMs)
    }
}

internal fun formatTrimTimeLabel(ms: Long): String {
    val totalSec = ms.coerceAtLeast(0L) / 1000.0
    val min = (totalSec / 60).toInt()
    val sec = totalSec % 60
    return String.format(Locale.US, "%d:%04.1f", min, sec)
}

/**
 * 步骤 1：视频裁剪。
 * 设计：缩略图条 + 两端白色把手（iOS 风格），整体左右保留安全边距规避系统手势区。
 */
@Composable
fun TrimStepTool(
    videoUri: Uri,
    durationMs: Long,
    savedTrimResult: TrimResult? = null,
    currentTimeMs: Long = 0L,
    onSeek: ((Long) -> Unit)? = null,
    onResult: (TrimResult) -> Unit,
) {
    val initialFractions = remember(videoUri, durationMs, savedTrimResult?.startMs, savedTrimResult?.endMs) {
        initialTrimFractions(durationMs, savedTrimResult)
    }
    var startFraction by remember(videoUri, durationMs) { mutableFloatStateOf(initialFractions.first) }
    var endFraction by remember(videoUri, durationMs) { mutableFloatStateOf(initialFractions.second) }

    LaunchedEffect(initialFractions) {
        startFraction = initialFractions.first
        endFraction = initialFractions.second
    }

    LaunchedEffect(startFraction, endFraction, durationMs) {
        trimResultFromFractions(
            durationMs = durationMs,
            startFraction = startFraction,
            endFraction = endFraction,
        )?.let(onResult)
    }

    if (durationMs <= 0L) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = DSColors.fairway,
                strokeWidth = 2.dp,
            )
            Text("正在加载视频...", color = DSColors.shadowGray, fontSize = 13.sp)
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 时间标签行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SAFE_MARGIN)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EdgeTimeLabel("开始", formatMs((startFraction * durationMs).toLong()), Alignment.Start)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("时长", color = Color(0xFF6B6B70), fontSize = 10.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = DSColors.fairway,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        formatMs(((endFraction - startFraction) * durationMs).toLong()),
                        color = DSColors.inkGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
            EdgeTimeLabel("结束", formatMs((endFraction * durationMs).toLong()), Alignment.End)
        }

        // 缩略图条 + 可拖拽把手
        VideoTrimBar(
            videoUri = videoUri,
            durationMs = durationMs,
            currentTimeMs = currentTimeMs,
            startFraction = startFraction,
            endFraction = endFraction,
            onStartChange = { fraction ->
                startFraction = fraction
                onSeek?.invoke(trimSeekTimeMs(durationMs, fraction))
            },
            onEndChange = { fraction ->
                endFraction = fraction
                onSeek?.invoke(trimSeekTimeMs(durationMs, fraction))
            },
            onPlayheadSeek = { timeMs -> onSeek?.invoke(timeMs) },
        )

        // 提示文字
        Row(
            modifier = Modifier.padding(horizontal = SAFE_MARGIN).padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.ContentCut,
                contentDescription = null,
                tint = DSColors.fairway,
                modifier = Modifier.size(14.dp),
            )
            Text("拖动两端手柄选择包含挥杆的视频片段", color = DSColors.shadowGray, fontSize = 12.sp)
        }
    }
}

// ─── 主控件 ───────────────────────────────────────────────────────────────────

private enum class DragTarget { START, END, PLAYHEAD }

@Composable
private fun VideoTrimBar(
    videoUri: Uri,
    durationMs: Long,
    currentTimeMs: Long,
    startFraction: Float,
    endFraction: Float,
    onStartChange: (Float) -> Unit,
    onEndChange: (Float) -> Unit,
    onPlayheadSeek: (Long) -> Unit,
) {
    val context = LocalContext.current
    val thumbnailSampleTimesMs = remember(durationMs) {
        trimThumbnailSampleTimesUs(durationMs).map { it / 1000L }
    }
    var thumbnails by remember(videoUri, thumbnailSampleTimesMs) {
        mutableStateOf(
            GolfCamThumbnailCache.cachedTimelineThumbnails(videoUri, thumbnailSampleTimesMs)
                .map { it?.asImageBitmap() },
        )
    }

    LaunchedEffect(videoUri, thumbnailSampleTimesMs) {
        if (durationMs <= 0L) return@LaunchedEffect
        val prioritySampleTimeMs = golfCamThumbnailPrioritySampleTimeMs(
            thumbnailSampleTimesMs,
            currentTimeMs.coerceIn(0L, durationMs),
        )
        val exactCachedSampleTimes = GolfCamThumbnailCache.cachedExactSampleTimes(videoUri, thumbnailSampleTimesMs)
        thumbnails = GolfCamThumbnailCache.cachedTimelineThumbnails(videoUri, thumbnailSampleTimesMs)
            .map { it?.asImageBitmap() }
        GolfCamThumbnailCache.loadTimelineThumbnails(
            context = context,
            videoUri = videoUri,
            sampleTimesMs = thumbnailSampleTimesMs,
            priorityTimeMs = prioritySampleTimeMs,
            skipCachedSampleTimesMs = exactCachedSampleTimes,
            frameOption = MediaMetadataRetriever.OPTION_CLOSEST,
        ) { index, bitmap ->
            thumbnails = golfCamFillMissingThumbnailSlotsFromNearest(
                thumbnails.toMutableList().also { current ->
                    if (index in current.indices) current[index] = bitmap.asImageBitmap()
                },
            )
        }
    }

    val density = LocalDensity.current
    val latestStartFraction by rememberUpdatedState(startFraction)
    val latestEndFraction by rememberUpdatedState(endFraction)
    val latestCurrentTimeMs by rememberUpdatedState(currentTimeMs)
    val latestOnStartChange by rememberUpdatedState(onStartChange)
    val latestOnEndChange by rememberUpdatedState(onEndChange)
    val latestOnPlayheadSeek by rememberUpdatedState(onPlayheadSeek)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SAFE_MARGIN)
            .height(BAR_HEIGHT)
            .semantics { contentDescription = "GolfCam 裁剪时间轴" },
    ) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val handleWPx = with(density) { HANDLE_W.toPx() }
        val innerWidthPx = (totalWidthPx - handleWPx * 2f).coerceAtLeast(1f)
        val touchZone = with(density) { (HANDLE_W + HANDLE_TOUCH_EXTRA).toPx() }
        val playheadTouchZone = with(density) { 14.dp.toPx() }
        val playheadFraction = trimPlayheadFraction(durationMs, currentTimeMs, startFraction, endFraction)

        var activeDrag by remember { mutableStateOf<DragTarget?>(null) }
        var initialFraction by remember { mutableFloatStateOf(0f) }
        var accumulatedX by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .drawBehind {
                    drawTrimStripPlaceholder(
                        stripX = handleWPx,
                        stripWidth = innerWidthPx,
                        stripHeight = size.height,
                    )
                    if (thumbnails.isNotEmpty()) {
                        drawThumbnails(
                            thumbnails = thumbnails,
                            stripX = handleWPx,
                            stripWidth = innerWidthPx,
                            stripHeight = size.height,
                        )
                    }
                    drawDimOverlay(
                        startFraction = startFraction,
                        endFraction = endFraction,
                        stripX = handleWPx,
                        stripWidth = innerWidthPx,
                        stripHeight = size.height,
                    )
                    drawSelectionBorder(
                        startFraction = startFraction,
                        endFraction = endFraction,
                        innerWidth = innerWidthPx,
                        canvasSize = size,
                        handleWidth = handleWPx,
                    )
                    playheadFraction?.let {
                        drawPlayhead(
                            fraction = it,
                            stripX = handleWPx,
                            stripWidth = innerWidthPx,
                            canvasHeight = size.height,
                        )
                    }
                }
                .pointerInput(totalWidthPx, durationMs) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val currentStart = trimFractionOrDefault(latestStartFraction, 0f)
                            val currentEnd = trimFractionOrDefault(latestEndFraction, 1f).coerceAtLeast(currentStart)
                            val currentPlayhead = trimPlayheadFraction(
                                durationMs = durationMs,
                                currentTimeMs = latestCurrentTimeMs,
                                startFraction = currentStart,
                                endFraction = currentEnd,
                            )
                            val startX = innerWidthPx * currentStart + handleWPx / 2f
                            val endX = innerWidthPx * currentEnd + handleWPx * 1.5f
                            val playheadX = currentPlayhead?.let { handleWPx + it * innerWidthPx }
                            activeDrag = when {
                                abs(offset.x - startX) <= touchZone -> DragTarget.START
                                abs(offset.x - endX) <= touchZone -> DragTarget.END
                                playheadX != null && abs(offset.x - playheadX) <= playheadTouchZone -> DragTarget.PLAYHEAD
                                else -> null
                            }
                            initialFraction = when (activeDrag) {
                                DragTarget.START -> currentStart
                                DragTarget.END -> currentEnd
                                DragTarget.PLAYHEAD -> currentPlayhead ?: currentStart
                                null -> 0f
                            }
                            accumulatedX = 0f
                        },
                        onDrag = { change, dragAmount ->
                            val target = activeDrag ?: return@detectDragGestures
                            change.consume()
                            accumulatedX += dragAmount.x
                            val nextFraction = (initialFraction + accumulatedX / innerWidthPx).coerceIn(0f, 1f)
                            when (target) {
                                DragTarget.START ->
                                    latestOnStartChange(
                                        clampTrimStartFraction(nextFraction, latestEndFraction, durationMs),
                                    )
                                DragTarget.END ->
                                    latestOnEndChange(
                                        clampTrimEndFraction(nextFraction, latestStartFraction, durationMs),
                                    )
                                DragTarget.PLAYHEAD ->
                                    latestOnPlayheadSeek(
                                        trimPlayheadSeekTimeMs(
                                            durationMs,
                                            nextFraction,
                                            latestStartFraction,
                                            latestEndFraction,
                                        ),
                                    )
                            }
                        },
                        onDragEnd = { activeDrag = null },
                        onDragCancel = { activeDrag = null },
                    )
                },
        )

        TrimHandle(
            modifier = Modifier
                .width(HANDLE_W)
                .height(BAR_HEIGHT)
                .offset { IntOffset((startFraction * innerWidthPx).toInt(), 0) }
                .zIndex(1f),
        )

        TrimHandle(
            modifier = Modifier
                .width(HANDLE_W)
                .height(BAR_HEIGHT)
                .offset { IntOffset((endFraction * innerWidthPx + handleWPx).toInt(), 0) }
                .zIndex(1f),
        )
    }
}

// ─── 把手外观 ────────────────────────────────────────────────────────────────

@Composable
private fun TrimHandle(modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(TRIM_SELECTION_CORNER_RADIUS_DP.dp))
            .background(Brush.verticalGradient(listOf(DSColors.fairway, DSColors.deepGreen)))
            .drawBehind {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.22f),
                    cornerRadius = CornerRadius(TRIM_SELECTION_CORNER_RADIUS_DP.dp.toPx()),
                    style = Stroke(width = 0.8.dp.toPx()),
                )
                val gripHeight = 12.dp.toPx()
                val top = (size.height - gripHeight) / 2f
                val bottom = top + gripHeight
                val halfSpacing = 2.25.dp.toPx()
                val stroke = 2.5.dp.toPx()
                val white = Color.White.copy(alpha = 0.78f)
                drawLine(white, Offset(size.width / 2f - halfSpacing, top), Offset(size.width / 2f - halfSpacing, bottom), stroke)
                drawLine(white, Offset(size.width / 2f + halfSpacing, top), Offset(size.width / 2f + halfSpacing, bottom), stroke)
            },
    )
}

// ─── Canvas 绘制 ──────────────────────────────────────────────────────────────

private fun DrawScope.drawTrimStripPlaceholder(
    stripX: Float,
    stripWidth: Float,
    stripHeight: Float,
) {
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                DSColors.mistGreen,
                DSColors.mistGreen.copy(alpha = 0.68f),
                DSColors.mistGreen,
            ),
            startX = stripX,
            endX = stripX + stripWidth,
        ),
        topLeft = Offset(stripX, 0f),
        size = Size(stripWidth, stripHeight),
        cornerRadius = CornerRadius(TRIM_STRIP_CORNER_RADIUS_DP.dp.toPx()),
    )
}

private fun DrawScope.drawThumbnails(
    thumbnails: List<ImageBitmap?>,
    stripX: Float,
    stripWidth: Float,
    stripHeight: Float,
) {
    val thumbW = stripWidth / thumbnails.size.coerceAtLeast(1)
    thumbnails.forEachIndexed { i, bmp ->
        if (bmp != null) {
            val targetAspect = thumbW / stripHeight.coerceAtLeast(1f)
            val sourceAspect = bmp.width.toFloat() / bmp.height.coerceAtLeast(1).toFloat()
            val sourceWidth: Int
            val sourceHeight: Int
            val sourceX: Int
            val sourceY: Int
            if (sourceAspect > targetAspect) {
                sourceHeight = bmp.height
                sourceWidth = (sourceHeight * targetAspect).roundToLong().toInt().coerceIn(1, bmp.width)
                sourceX = ((bmp.width - sourceWidth) / 2).coerceAtLeast(0)
                sourceY = 0
            } else {
                sourceWidth = bmp.width
                sourceHeight = (sourceWidth / targetAspect.coerceAtLeast(0.0001f)).roundToLong().toInt().coerceIn(1, bmp.height)
                sourceX = 0
                sourceY = ((bmp.height - sourceHeight) / 2).coerceAtLeast(0)
            }
            drawImage(
                image = bmp,
                srcOffset = IntOffset(sourceX, sourceY),
                srcSize = IntSize(sourceWidth, sourceHeight),
                dstOffset = IntOffset((stripX + i * thumbW).toInt(), 0),
                dstSize = IntSize((thumbW + 1f).toInt(), stripHeight.toInt()),
                filterQuality = FilterQuality.Low,
            )
        }
    }
}

private fun DrawScope.drawDimOverlay(
    startFraction: Float,
    endFraction: Float,
    stripX: Float,
    stripWidth: Float,
    stripHeight: Float,
) {
    val startX = stripX + startFraction * stripWidth
    val endX = stripX + endFraction * stripWidth
    val dim = Color.Black.copy(alpha = TRIM_DIM_OVERLAY_ALPHA)
    if (startFraction > 0f) drawRect(dim, Offset(stripX, 0f), Size(startX - stripX, stripHeight))
    if (endFraction < 1f) drawRect(dim, Offset(endX, 0f), Size(stripX + stripWidth - endX, stripHeight))
}

private fun DrawScope.drawSelectionBorder(
    startFraction: Float,
    endFraction: Float,
    innerWidth: Float,
    canvasSize: Size,
    handleWidth: Float,
) {
    val stroke = TRIM_SELECTION_BORDER_STROKE_DP.dp.toPx()
    val selectedStartX = (startFraction * innerWidth).coerceIn(0f, canvasSize.width)
    val selectedWidth = max(handleWidth * 2f, (endFraction - startFraction) * innerWidth + handleWidth * 2f)
        .coerceAtMost(canvasSize.width - selectedStartX)
    val corner = TRIM_SELECTION_CORNER_RADIUS_DP.dp.toPx()
    val topLeft = Offset(selectedStartX + stroke / 2f, stroke / 2f)
    val size = Size(
        width = (selectedWidth - stroke).coerceAtLeast(0f),
        height = (canvasSize.height - stroke).coerceAtLeast(0f),
    )
    drawRoundRect(
        color = DSColors.fairway.copy(alpha = TRIM_SELECTION_FILL_ALPHA),
        topLeft = Offset(selectedStartX, 0f),
        size = Size(selectedWidth, canvasSize.height),
        cornerRadius = CornerRadius(corner, corner),
    )
    drawRoundRect(
        color = DSColors.fairway.copy(alpha = 0.86f),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = stroke),
    )
}

private fun DrawScope.drawPlayhead(
    fraction: Float,
    stripX: Float,
    stripWidth: Float,
    canvasHeight: Float,
) {
    val x = stripX + fraction.coerceIn(0f, 1f) * stripWidth
    val top = 2.dp.toPx()
    val bottom = canvasHeight - 2.dp.toPx()
    drawLine(
        Color.Black.copy(alpha = 0.50f),
        Offset(x, top),
        Offset(x, bottom),
        strokeWidth = 4.dp.toPx(),
    )
    drawLine(
        Color.White,
        Offset(x, top),
        Offset(x, bottom),
        strokeWidth = 2.dp.toPx(),
    )
}

// ─── 时间标签 ─────────────────────────────────────────────────────────────────

@Composable
private fun EdgeTimeLabel(
    title: String,
    time: String,
    alignment: Alignment.Horizontal,
) {
    Column(horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = Color(0xFF6B6B70), fontSize = 10.sp)
        Text(
            time,
            color = DSColors.shadowGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun formatMs(ms: Long): String = formatTrimTimeLabel(ms)
