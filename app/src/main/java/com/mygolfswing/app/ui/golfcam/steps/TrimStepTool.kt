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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

internal const val TRIM_THUMBNAIL_COUNT = 15
internal const val TRIM_SAFE_MARGIN_DP = 28
internal const val TRIM_HANDLE_WIDTH_DP = 12
internal const val TRIM_BAR_HEIGHT_DP = 56
internal const val TRIM_HANDLE_TOUCH_EXTRA_DP = 16
internal const val TRIM_DIM_OVERLAY_ALPHA = 0.60f
internal const val TRIM_SELECTION_BORDER_STROKE_PX = 1.5f
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
    return golfCamThumbnailSampleTimesMs(
        startMs = 0L,
        endMs = durationMs,
        maxThumbnails = thumbnailCount,
    ).map { sampleTimeMs -> golfCamThumbnailRetrieverTimeUs(sampleTimeMs) }
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
        // 时间标签行（与 trim bar 对齐，同样缩进）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SAFE_MARGIN),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeLabel("开始", formatMs((startFraction * durationMs).toLong()), Alignment.Start)
            TimeLabel(
                "时长",
                formatMs(((endFraction - startFraction) * durationMs).toLong()),
                Alignment.CenterHorizontally,
                highlight = true,
            )
            TimeLabel("结束", formatMs((endFraction * durationMs).toLong()), Alignment.End)
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
            modifier = Modifier.padding(horizontal = SAFE_MARGIN),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                Icons.Filled.ContentCut,
                contentDescription = null,
                tint = DSColors.shadowGray,
                modifier = Modifier.size(12.dp),
            )
            Text("拖动两端把手选择挥杆片段", color = DSColors.shadowGray, fontSize = 11.sp)
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
            frameOption = MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
        ) { index, bitmap ->
            thumbnails = golfCamFillMissingThumbnailSlotsFromNearest(
                thumbnails.toMutableList().also { current ->
                    if (index in current.indices) current[index] = bitmap.asImageBitmap()
                },
            )
        }
    }

    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SAFE_MARGIN),
    ) {
        val barWidthPx = with(density) { maxWidth.toPx() }
        val handleWPx = with(density) { HANDLE_W.toPx() }
        val touchZone = with(density) { (HANDLE_W + HANDLE_TOUCH_EXTRA).toPx() }
        val playheadTouchZone = with(density) { 14.dp.toPx() }
        val playheadFraction = trimPlayheadFraction(durationMs, currentTimeMs, startFraction, endFraction)

        // 记录当前拖哪个把手
        var activeDrag by remember { mutableStateOf<DragTarget?>(null) }

        // 条体（缩略图 + 遮罩 + 边框）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .clip(RoundedCornerShape(TRIM_STRIP_CORNER_RADIUS_DP.dp))
                .background(Color(0xFF111111))
                .drawBehind {
                    if (thumbnails.isNotEmpty()) drawThumbnails(thumbnails, size)
                    drawDimOverlay(startFraction, endFraction, size)
                    drawSelectionBorder(startFraction, endFraction, size, handleWPx)
                    playheadFraction?.let { drawPlayhead(it, size) }
                }
                .pointerInput(barWidthPx, touchZone, playheadFraction, startFraction, endFraction) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val startX = startFraction * barWidthPx
                            val endX = endFraction * barWidthPx
                            val playheadX = playheadFraction?.let { it * barWidthPx }
                            activeDrag = when {
                                abs(offset.x - startX) <= touchZone -> DragTarget.START
                                abs(offset.x - endX) <= touchZone -> DragTarget.END
                                playheadX != null && abs(offset.x - playheadX) <= playheadTouchZone -> DragTarget.PLAYHEAD
                                else -> null
                            }
                        },
                        onDrag = { change, _ ->
                            val target = activeDrag ?: return@detectDragGestures
                            change.consume()
                            val newFraction = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                            when (target) {
                                DragTarget.START ->
                                    onStartChange(clampTrimStartFraction(newFraction, endFraction, durationMs))
                                DragTarget.END ->
                                    onEndChange(clampTrimEndFraction(newFraction, startFraction, durationMs))
                                DragTarget.PLAYHEAD ->
                                    onPlayheadSeek(trimPlayheadSeekTimeMs(durationMs, newFraction, startFraction, endFraction))
                            }
                        },
                        onDragEnd = { activeDrag = null },
                        onDragCancel = { activeDrag = null },
                    )
                },
        )

        // 左把手（仅视觉，交互在条体上）
        TrimHandle(
            modifier = Modifier
                .width(HANDLE_W)
                .height(BAR_HEIGHT)
                .offset { IntOffset((startFraction * barWidthPx - handleWPx / 2f).toInt(), 0) }
                .zIndex(1f),
            isLeft = true,
        )

        // 右把手
        TrimHandle(
            modifier = Modifier
                .width(HANDLE_W)
                .height(BAR_HEIGHT)
                .offset { IntOffset((endFraction * barWidthPx - handleWPx / 2f).toInt(), 0) }
                .zIndex(1f),
            isLeft = false,
        )
    }
}

// ─── 把手外观（iOS 风格：白色窄竖条，两条细 grip 线）────────────────────────────

@Composable
private fun TrimHandle(
    modifier: Modifier,
    isLeft: Boolean,
) {
    val shape = if (isLeft)
        RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp)
    else
        RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(DSColors.fairway)
            .drawBehind {
                val cx = size.width / 2f
                val lineH = size.height * 0.36f
                val top = (size.height - lineH) / 2f
                val white = Color.White.copy(alpha = 0.8f)
                drawLine(white, Offset(cx - 2.5f, top), Offset(cx - 2.5f, top + lineH), strokeWidth = 1.5f)
                drawLine(white, Offset(cx + 2.5f, top), Offset(cx + 2.5f, top + lineH), strokeWidth = 1.5f)
            },
    )
}

// ─── Canvas 绘制 ──────────────────────────────────────────────────────────────

private fun DrawScope.drawThumbnails(thumbnails: List<ImageBitmap?>, canvasSize: Size) {
    val thumbW = canvasSize.width / thumbnails.size
    thumbnails.forEachIndexed { i, bmp ->
        if (bmp != null) {
            drawImage(
                image = bmp,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(bmp.width, bmp.height),
                dstOffset = IntOffset((i * thumbW).toInt(), 0),
                dstSize = IntSize((thumbW + 1).toInt(), canvasSize.height.toInt()),
                filterQuality = FilterQuality.Low,
            )
        }
    }
}

private fun DrawScope.drawDimOverlay(startFraction: Float, endFraction: Float, canvasSize: Size) {
    val startX = startFraction * canvasSize.width
    val endX = endFraction * canvasSize.width
    val dim = Color.Black.copy(alpha = TRIM_DIM_OVERLAY_ALPHA)
    if (startX > 0f) drawRect(dim, Offset.Zero, Size(startX, canvasSize.height))
    if (endX < canvasSize.width) drawRect(dim, Offset(endX, 0f), Size(canvasSize.width - endX, canvasSize.height))
}

private fun DrawScope.drawSelectionBorder(
    startFraction: Float,
    endFraction: Float,
    canvasSize: Size,
    handleWPx: Float,
) {
    val startX = startFraction * canvasSize.width
    val endX = endFraction * canvasSize.width
    val stroke = TRIM_SELECTION_BORDER_STROKE_PX
    val selectedStartX = startX.coerceIn(0f, canvasSize.width)
    val selectedEndX = endX.coerceIn(selectedStartX, canvasSize.width)
    val selectedWidth = max(handleWPx * 2f, selectedEndX - selectedStartX)
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
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
    )
}

private fun DrawScope.drawPlayhead(fraction: Float, canvasSize: Size) {
    val x = fraction.coerceIn(0f, 1f) * canvasSize.width
    val top = 2.dp.toPx()
    val bottom = canvasSize.height - 2.dp.toPx()
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
private fun TimeLabel(
    title: String,
    time: String,
    alignment: Alignment.Horizontal,
    highlight: Boolean = false,
) {
    Column(horizontalAlignment = alignment) {
        Text(title, color = DSColors.shadowGray, fontSize = 10.sp)
        Text(
            time,
            color = if (highlight) DSColors.fairway else DSColors.inkGreen,
            fontSize = 12.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun formatMs(ms: Long): String = formatTrimTimeLabel(ms)
