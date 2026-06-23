package com.mygolfswing.app.ui.golfcam.steps

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Flag
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MIN_FLIGHT_SEC = 0.5
private const val MAX_FLIGHT_SEC = 8.0
private const val LANDING_THUMB_COUNT = 12
private val VIRTUAL_BLUE = Color(0xFF4285F4)

/**
 * 步骤 4：落点选择（对齐 iOS LandingStepTool）。
 * 工具区显示飞行时间、落点坐标、中心固定的缩略图时间轴和虚拟段提示；
 * 落点位置仍在上方视频画面中拖动。
 */
@Composable
fun LandingStepTool(
    videoUri: Uri,
    state: GolfCamWorkflowState = GolfCamWorkflowState(),
    onFlightTimeChange: ((Double) -> Unit)? = null,
) {
    val landing = state.landing
    val impactTimeMs = state.impactFrame?.timeMs ?: state.trimResult?.startMs ?: 0L
    val clipEndMs = state.trimResult?.endMs ?: 0L
    val tTail = ((clipEndMs - impactTimeMs) / 1000.0).takeIf { it.isFinite() }
        ?.coerceAtLeast(MIN_FLIGHT_SEC)
        ?.coerceAtMost(MAX_FLIGHT_SEC)
        ?: MAX_FLIGHT_SEC

    val xPct = (landing?.normalizedX ?: 0.5f) * 100f
    val yPct = (landing?.normalizedY ?: 0.5f) * 100f
    val flightTime = landing?.flightTimeSec ?: 4.5
    var sliderValue by remember(flightTime) { mutableDoubleStateOf(flightTime.coerceIn(MIN_FLIGHT_SEC, MAX_FLIGHT_SEC)) }
    val isInVirtualSegment = sliderValue > tTail + 0.02

    LaunchedEffect(Unit) {
        if (landing == null) onFlightTimeChange?.invoke(sliderValue)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text("飞行时间", color = DSColors.shadowGray, fontSize = 10.sp)
                Text(
                    "${"%.1f".format(sliderValue)}s",
                    color = if (isInVirtualSegment) VIRTUAL_BLUE else DSColors.inkGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Filled.Flag, null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
                Text("落点", color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (isInVirtualSegment) {
                    Text(
                        "虚拟段",
                        color = VIRTUAL_BLUE,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(VIRTUAL_BLUE.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("位置", color = DSColors.shadowGray, fontSize = 10.sp)
                Text(
                    "${"%.0f".format(xPct)}%, ${"%.0f".format(yPct)}%",
                    color = DSColors.inkGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        LandingThumbnailTimeline(
            videoUri = videoUri,
            impactTimeMs = impactTimeMs,
            clipEndMs = clipEndMs,
            flightTimeSec = sliderValue,
            tTailSec = tTail,
            onTimeChanged = { newTime ->
                sliderValue = newTime
                onFlightTimeChange?.invoke(newTime)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.LocationOn, null, tint = DSColors.fairway, modifier = Modifier.size(14.dp))
            Text(
                "滑动时间轴选择落点时间，拖动画面中的落点标记",
                color = DSColors.shadowGray,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun LandingThumbnailTimeline(
    videoUri: Uri,
    impactTimeMs: Long,
    clipEndMs: Long,
    flightTimeSec: Double,
    tTailSec: Double,
    onTimeChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var thumbnails by remember(videoUri, impactTimeMs, clipEndMs) { mutableStateOf<List<ImageBitmap?>>(emptyList()) }

    LaunchedEffect(videoUri, impactTimeMs, clipEndMs) {
        thumbnails = withContext(Dispatchers.IO) {
            loadLandingThumbnails(
                context = context,
                videoUri = videoUri,
                impactTimeMs = impactTimeMs,
                clipEndMs = clipEndMs,
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF262626)),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val pixelsPerSecond = with(density) { 80.dp.toPx() }
        val thumbWidthPx = with(density) { 32.dp.toPx() }
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
                alpha = if (virtual) 0.58f else 1f,
            )
        }
        if (virtual) {
            drawRect(
                color = VIRTUAL_BLUE.copy(alpha = 0.34f),
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
    drawRect(color = color, topLeft = Offset(x - 1f, 0f), size = Size(2f, size.height))
    if (label != null) {
        drawContext.canvas.nativeCanvas.drawText(
            label,
            x + 5f,
            14f,
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = android.graphics.Color.argb(210, 255, 255, 255)
                textSize = 24f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            },
        )
    }
}

@Composable
private fun CenterTimeIndicator(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val barTop = 4f
        val barBottom = size.height - 4f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(cx - 2f, barTop),
            size = Size(4f, barBottom - barTop),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
        )
        drawCircle(Color.White, radius = 4f, center = Offset(cx, 4f))
        drawCircle(Color.White, radius = 4f, center = Offset(cx, size.height - 4f))
    }
}

private fun loadLandingThumbnails(
    context: android.content.Context,
    videoUri: Uri,
    impactTimeMs: Long,
    clipEndMs: Long,
): List<ImageBitmap?> {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, videoUri)
        List(LANDING_THUMB_COUNT) { index ->
            val relSec = index * MAX_FLIGHT_SEC / (LANDING_THUMB_COUNT - 1)
            val targetMs = impactTimeMs + (relSec * 1000).toLong()
            val safeMs = if (clipEndMs > 33L) min(targetMs, clipEndMs - 33L) else targetMs
            retriever.getFrameAtTime(
                max(0L, safeMs) * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST,
            )?.toLandingBitmap()
        }
    } catch (_: Throwable) {
        emptyList()
    } finally {
        retriever.release()
    }
}

private fun Bitmap.toLandingBitmap(): ImageBitmap {
    val scaled = Bitmap.createScaledBitmap(this, 64, 112, true)
    if (scaled !== this) recycle()
    return scaled.asImageBitmap()
}
