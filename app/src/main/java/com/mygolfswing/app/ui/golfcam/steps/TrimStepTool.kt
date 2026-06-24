package com.mygolfswing.app.ui.golfcam.steps

import android.graphics.Bitmap
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import com.mygolfswing.app.ui.golfcam.model.TrimResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val THUMB_COUNT = 15
private const val DEFAULT_TRIM_MAX_MS = 30_000L
private const val MIN_TRIM_DURATION_MS = 500L

// 安全边距：条的左右各留 SAFE_MARGIN，让把手远离屏幕边缘手势区
private val SAFE_MARGIN: Dp = 28.dp
private val HANDLE_W: Dp = 12.dp
private val BAR_HEIGHT: Dp = 62.dp
private val HANDLE_TOUCH_EXTRA: Dp = 16.dp

internal fun defaultTrimEndFraction(durationMs: Long): Float {
    if (durationMs <= 0L) return 1f
    return (min(durationMs, DEFAULT_TRIM_MAX_MS).toFloat() / durationMs).coerceIn(0f, 1f)
}

internal fun minimumTrimFraction(durationMs: Long): Float {
    if (durationMs <= 0L) return 1f
    return (MIN_TRIM_DURATION_MS.toFloat() / durationMs).coerceIn(0f, 1f)
}

internal fun clampTrimStartFraction(newFraction: Float, endFraction: Float, durationMs: Long): Float {
    val maxStart = max(0f, endFraction - minimumTrimFraction(durationMs))
    return newFraction.coerceIn(0f, maxStart)
}

internal fun clampTrimEndFraction(newFraction: Float, startFraction: Float, durationMs: Long): Float {
    val minEnd = min(1f, startFraction + minimumTrimFraction(durationMs))
    return newFraction.coerceIn(minEnd, 1f)
}

/**
 * 步骤 1：视频裁剪。
 * 设计：缩略图条 + 两端白色把手（iOS 风格），整体左右保留安全边距规避系统手势区。
 */
@Composable
fun TrimStepTool(
    videoUri: Uri,
    durationMs: Long,
    onResult: (TrimResult) -> Unit,
) {
    var startFraction by remember(durationMs) { mutableStateOf(0f) }
    var endFraction by remember(durationMs) { mutableStateOf(defaultTrimEndFraction(durationMs)) }

    LaunchedEffect(startFraction, endFraction, durationMs) {
        if (durationMs > 0L) {
            onResult(
                TrimResult(
                    startMs = (startFraction * durationMs).toLong(),
                    endMs = (endFraction * durationMs).toLong(),
                ),
            )
        }
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
            startFraction = startFraction,
            endFraction = endFraction,
            onStartChange = { startFraction = it },
            onEndChange = { endFraction = it },
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

private enum class DragTarget { START, END }

@Composable
private fun VideoTrimBar(
    videoUri: Uri,
    durationMs: Long,
    startFraction: Float,
    endFraction: Float,
    onStartChange: (Float) -> Unit,
    onEndChange: (Float) -> Unit,
) {
    val context = LocalContext.current
    var thumbnails by remember(videoUri) { mutableStateOf<List<ImageBitmap?>>(emptyList()) }

    LaunchedEffect(videoUri, durationMs) {
        if (durationMs <= 0L) return@LaunchedEffect
        thumbnails = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                (0 until THUMB_COUNT).map { i ->
                    val timeUs = i.toLong() * durationMs * 1000L / (THUMB_COUNT - 1)
                    val bmp = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    bmp?.let {
                        val targetH = 160
                        val targetW = (it.width.toFloat() / it.height * targetH).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(it, targetW, targetH, false).asImageBitmap()
                    }
                }
            } catch (_: Exception) {
                List(THUMB_COUNT) { null }
            } finally {
                retriever.release()
            }
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

        // 记录当前拖哪个把手
        var activeDrag by remember { mutableStateOf<DragTarget?>(null) }

        // 条体（缩略图 + 遮罩 + 边框）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF111111))
                .drawBehind {
                    if (thumbnails.isNotEmpty()) drawThumbnails(thumbnails, size)
                    drawDimOverlay(startFraction, endFraction, size)
                    drawSelectionBorder(startFraction, endFraction, size, handleWPx)
                }
                .pointerInput(barWidthPx, touchZone) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val startX = startFraction * barWidthPx
                            val endX = endFraction * barWidthPx
                            activeDrag = when {
                                abs(offset.x - startX) <= touchZone -> DragTarget.START
                                abs(offset.x - endX) <= touchZone -> DragTarget.END
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
    val dim = Color.Black.copy(alpha = 0.58f)
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
    val stroke = 3f
    // 上下绿色边线（把手之间）
    drawLine(DSColors.fairway, Offset(startX + handleWPx / 2, stroke / 2), Offset(endX - handleWPx / 2, stroke / 2), stroke)
    drawLine(
        DSColors.fairway,
        Offset(startX + handleWPx / 2, canvasSize.height - stroke / 2),
        Offset(endX - handleWPx / 2, canvasSize.height - stroke / 2),
        stroke,
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

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000.0
    val min = (totalSec / 60).toInt()
    val sec = totalSec % 60
    return "%d:%04.1f".format(min, sec)
}
