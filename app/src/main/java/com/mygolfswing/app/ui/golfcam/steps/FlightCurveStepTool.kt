package com.mygolfswing.app.ui.golfcam.steps

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Adjust
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.GolfCamFrameTimeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private const val MAX_CURVE_THUMBS = 60

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
    val cp1 = state.flightCurveControlPoint1
    val cp2 = state.flightCurveControlPoint2
    val p1x = (cp1?.first ?: 0.3f) * 100f
    val p1y = (cp1?.second ?: 0.3f) * 100f
    val p2x = (cp2?.first ?: 0.7f) * 100f
    val p2y = (cp2?.second ?: 0.2f) * 100f
    val currentSec = currentFrameIndex / safeFrameRate

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
                Text(
                    if (totalFrames > 0) "帧 ${currentFrameIndex + 1}/$totalFrames" else "飞行曲线",
                    color = DSColors.inkGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "%.2fs".format(currentSec),
                    color = DSColors.shadowGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            ControlPointReadout(label = "P1", xPct = p1x, yPct = p1y)
            ControlPointReadout(label = "P2", xPct = p2x, yPct = p2y)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
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
                currentFrameIndex = currentFrameIndex,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CurveActionChip(icon = Icons.Filled.TouchApp, label = "拖动控制点")
            CurveActionChip(icon = Icons.Filled.Adjust, label = "匹配弹道")
            Spacer(modifier = Modifier.weight(1f))
            CurveActionChip(icon = Icons.Filled.Bolt, label = "击球帧", active = true, onClick = { onImpactFrame?.invoke() })
        }
    }
}

@Composable
private fun ControlPointReadout(label: String, xPct: Float, yPct: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White)
                .background(DSColors.fairway.copy(alpha = 0.34f)),
        )
        Text(label, color = DSColors.inkGreen, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Text(
            "(%.0f%%,%.0f%%)".format(xPct, yPct),
            color = DSColors.shadowGray,
            fontSize = 9.sp,
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
            .width(50.dp)
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
            modifier = Modifier.size(16.dp),
        )
        Text(
            label,
            color = contentColor.copy(alpha = if (enabled) 0.72f else 1f),
            fontSize = 9.sp,
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) DSColors.fairway.copy(alpha = 0.15f) else DSColors.mistGreen.copy(alpha = 0.55f))
            .then(
                if (active) {
                    Modifier.border(1.dp, DSColors.fairway.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                } else {
                    Modifier
                },
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = if (active) DSColors.fairway else DSColors.shadowGray, modifier = Modifier.size(12.dp))
        Text(
            label,
            color = if (active) DSColors.fairway else DSColors.shadowGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CurveThumbnailTimeline(
    videoUri: Uri,
    trimStartMs: Long,
    currentFrameIndex: Int,
    totalFrames: Int,
    frameRate: Double,
    onFrameSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val frameStep = max(1, ceil(totalFrames / MAX_CURVE_THUMBS.toDouble()).toInt())
    var thumbnails by remember(videoUri, trimStartMs, totalFrames, frameStep, frameRate) {
        mutableStateOf<List<CurveThumb>>(emptyList())
    }

    LaunchedEffect(videoUri, trimStartMs, totalFrames, frameStep, frameRate) {
        if (totalFrames <= 0) return@LaunchedEffect
        thumbnails = withContext(Dispatchers.IO) {
            loadCurveThumbnails(context, videoUri, trimStartMs, totalFrames, frameStep, frameRate)
        }
    }

    LazyRow(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF2F2F2F)),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items(thumbnails) { thumb ->
            val selected = kotlin.math.abs(thumb.frameIndex - currentFrameIndex) < frameStep
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .fillMaxHeight()
                    .clickable { onFrameSelected(thumb.frameIndex) },
            ) {
                if (thumb.bitmap != null) {
                    Image(
                        bitmap = thumb.bitmap,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (selected) 1f else 0.72f,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color(0xFF3A3A3A)),
                    )
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(DSColors.fairway),
                    )
                }
            }
        }
    }
}

private data class CurveThumb(val frameIndex: Int, val bitmap: ImageBitmap?)

private fun loadCurveThumbnails(
    context: android.content.Context,
    videoUri: Uri,
    trimStartMs: Long,
    totalFrames: Int,
    frameStep: Int,
    frameRate: Double,
): List<CurveThumb> {
    val retriever = MediaMetadataRetriever()
    val safeFrameRate = GolfCamFrameTimeline.safeFrameRate(frameRate)
    return try {
        retriever.setDataSource(context, videoUri)
        val frames = buildList {
            var frame = 0
            while (frame < totalFrames) {
                add(frame)
                frame += frameStep
            }
        }
        frames.map { frameIndex ->
            val timeMs = GolfCamFrameTimeline.timeMsForFrame(trimStartMs, frameIndex, safeFrameRate)
            val bitmap = retriever.getFrameAtTime(timeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
                ?.toCurveBitmap()
            CurveThumb(frameIndex = min(frameIndex, totalFrames - 1), bitmap = bitmap)
        }
    } catch (_: Throwable) {
        emptyList()
    } finally {
        retriever.release()
    }
}

private fun Bitmap.toCurveBitmap(): ImageBitmap {
    val scaled = Bitmap.createScaledBitmap(this, 64, 90, true)
    if (scaled !== this) recycle()
    return scaled.asImageBitmap()
}
