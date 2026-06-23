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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.CandidateImpactFrame
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.ImpactFrameSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

private const val FPS = 30.0
private const val THUMB_CELL_COUNT = 60       // 最多 60 张缩略图
private val CELL_W: Dp = 32.dp
private val CELL_H: Dp = 45.dp
private val CELL_GAP: Dp = 2.dp

/**
 * 步骤 2：击球帧选择。
 * UI：缩略图帧轨道（吸附居中）+ 候选帧金色小点 + 上一帧/下一帧按钮
 */
@Composable
fun ImpactFrameStepTool(
    player: ExoPlayer,
    videoUri: Uri,
    durationMs: Long,
    state: GolfCamWorkflowState,
    candidateFrames: List<CandidateImpactFrame>,
    onResult: (ImpactFrameSelection) -> Unit,
) {
    val startMs = state.trimResult?.startMs ?: 0L
    val endMs = (state.trimResult?.endMs?.takeIf { it > 0L } ?: durationMs).coerceAtLeast(startMs + 1L)
    val trimDurationMs = (endMs - startMs).coerceAtLeast(1L)
    val totalFrames = (trimDurationMs / 1000.0 * FPS).roundToInt().coerceAtLeast(1)

    // 初始帧：优先已保存 → 最高候选 → 中间帧
    val initialFrame = remember(state.impactFrame, candidateFrames) {
        state.impactFrame?.frameIndex
            ?: candidateFrames.firstOrNull()?.frameIndex
            ?: (totalFrames / 2)
    }

    var currentFrame by remember(durationMs) { mutableIntStateOf(initialFrame) }

    // step：多少个真实帧 对应 1个缩略图格
    val step = (totalFrames.toFloat() / THUMB_CELL_COUNT).coerceAtLeast(1f)
    val thumbCount = minOf(THUMB_CELL_COUNT, totalFrames)

    // 通知父层
    LaunchedEffect(currentFrame, durationMs) {
        if (durationMs > 0L) {
            val timeMs = startMs + (currentFrame / FPS * 1000).toLong()
            player.seekTo(timeMs.coerceIn(startMs, endMs))
            player.pause()
            onResult(ImpactFrameSelection(frameIndex = currentFrame, timeMs = timeMs, candidateFrames = candidateFrames))
        }
    }

    if (durationMs <= 0L) {
        LoadingRow()
        return
    }

    val elapsedSec = currentFrame / FPS
    val absFrameIndex = (startMs / 1000.0 * FPS).roundToInt() + currentFrame  // 原始视频帧号

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── 帧信息行 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("距离开始", color = DSColors.shadowGray, fontSize = 10.sp)
                Text(
                    "${"%.2f".format(elapsedSec)}s",
                    color = DSColors.inkGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("当前帧", color = DSColors.shadowGray, fontSize = 10.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(DSColors.fairway, RoundedCornerShape(2.dp)),
                    )
                    Text(
                        "$absFrameIndex/${(durationMs / 1000.0 * FPS).roundToInt()}",
                        color = DSColors.inkGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
            Spacer(modifier = Modifier.width(48.dp))
        }

        // ── 缩略图帧轨道 ──
        FrameFilmstrip(
            videoUri = videoUri,
            startMs = startMs,
            trimDurationMs = trimDurationMs,
            totalFrames = totalFrames,
            thumbCount = thumbCount,
            step = step,
            currentFrame = currentFrame,
            candidateFrames = candidateFrames,
            onFrameChanged = { currentFrame = it },
        )

        // ── 提示文字 ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .border(1.5.dp, DSColors.shadowGray, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.size(5.dp).background(DSColors.shadowGray, CircleShape))
            }
            Text(
                "滑动选择球杆击中球的瞬间，上方视频同步跳帧",
                color = DSColors.shadowGray,
                fontSize = 11.sp,
            )
        }
    }
}

// ─── 帧轨道（缩略图 + 候选金点 + 箭头）────────────────────────────────────────

@Composable
private fun FrameFilmstrip(
    videoUri: Uri,
    startMs: Long,
    trimDurationMs: Long,
    totalFrames: Int,
    thumbCount: Int,
    step: Float,
    currentFrame: Int,
    candidateFrames: List<CandidateImpactFrame>,
    onFrameChanged: (Int) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // 缩略图缓存
    var thumbnails by remember(videoUri) { mutableStateOf<List<android.graphics.Bitmap?>>(List(thumbCount) { null }) }

    LaunchedEffect(videoUri, thumbCount, trimDurationMs) {
        if (thumbCount <= 0 || trimDurationMs <= 0) return@LaunchedEffect
        thumbnails = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                (0 until thumbCount).map { i ->
                    val frameMs = startMs + i.toLong() * trimDurationMs / (thumbCount - 1).coerceAtLeast(1)
                    val bmp = retriever.getFrameAtTime(frameMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    bmp?.let {
                        val h = 180; val w = (it.width.toFloat() / it.height * h).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(it, w, h, false)
                    }
                }
            } catch (_: Exception) {
                List(thumbCount) { null }
            } finally {
                retriever.release()
            }
        }
    }

    // 当前缩略图格（thumbIndex）对应的真实帧 index
    fun thumbIndexToFrame(thumbIdx: Int): Int = (thumbIdx * step).roundToInt().coerceIn(0, totalFrames - 1)
    fun frameToThumbIndex(frameIdx: Int): Int = (frameIdx / step).roundToInt().coerceIn(0, thumbCount - 1)

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = frameToThumbIndex(currentFrame))

    // 用户滑动时更新帧
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@snapshotFlow -1
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
            visibleItems.minByOrNull { abs((it.offset + it.size / 2f) - viewportCenter) }?.index ?: -1
        }.distinctUntilChanged()
         .collect { thumbIdx ->
             if (thumbIdx >= 0 && !listState.isScrollInProgress.not()) {
                 onFrameChanged(thumbIndexToFrame(thumbIdx))
             }
         }
    }

    // 代码改帧时滚动轨道
    LaunchedEffect(currentFrame) {
        val targetThumb = frameToThumbIndex(currentFrame)
        if (!listState.isScrollInProgress) {
            scope.launch { listState.animateScrollToItem(targetThumb) }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellWPx = with(density) { CELL_W.toPx() }
        val sidePad = (maxWidth - CELL_W) / 2  // 使第一/最后一张可滚到中心

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 上一帧
            NavButton(icon = Icons.Filled.ChevronLeft, label = "上一帧", enabled = currentFrame > 0) {
                val newFrame = (currentFrame - 1).coerceAtLeast(0)
                onFrameChanged(newFrame)
            }

            // 缩略图条
            Box(modifier = Modifier.weight(1f)) {
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = sidePad),
                    horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CELL_H + 8.dp)  // 留空间给金点
                        .clip(RoundedCornerShape(6.dp)),
                ) {
                    itemsIndexed(thumbnails) { thumbIdx, bmp ->
                        val frameIdx = thumbIndexToFrame(thumbIdx)
                        val isSelected = abs(frameIdx - currentFrame) < step / 2f
                        val isCandidate = candidateFrames.any { abs(it.frameIndex - frameIdx) < step }

                        Box(
                            modifier = Modifier
                                .width(CELL_W)
                                .height(CELL_H + 8.dp)
                                .clickable { onFrameChanged(frameIdx) },
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            // 缩略图
                            val imgBitmap = bmp?.asImageBitmap()
                            Box(
                                modifier = Modifier
                                    .width(CELL_W)
                                    .height(CELL_H)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF1A1A1A))
                                    .then(
                                        if (isSelected) Modifier.border(
                                            2.dp, DSColors.fairway, RoundedCornerShape(3.dp),
                                        ) else Modifier,
                                    ),
                            ) {
                                if (imgBitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = imgBitmap,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize().then(
                                            if (!isSelected) Modifier.drawBehind {
                                                drawRect(Color.Black.copy(alpha = 0.35f))
                                            } else Modifier,
                                        ),
                                    )
                                }
                            }

                            // 候选帧金色小圆点
                            if (isCandidate) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .size(7.dp)
                                        .background(Color(0xFFFFD700), CircleShape),
                                )
                            }
                        }
                    }
                }

                // 中心固定指示线
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(2.dp)
                        .height(CELL_H)
                        .background(DSColors.fairway.copy(alpha = 0.0f)),  // 指示线由选中 cell 边框替代
                )
            }

            // 下一帧
            NavButton(icon = Icons.Filled.ChevronRight, label = "下一帧", enabled = currentFrame < totalFrames - 1) {
                val newFrame = (currentFrame + 1).coerceAtMost(totalFrames - 1)
                onFrameChanged(newFrame)
            }
        }
    }
}

// ─── 箭头按钮 ─────────────────────────────────────────────────────────────────

@Composable
private fun NavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(36.dp)
            .height(CELL_H)
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) DSColors.mistGreen else DSColors.mistGreen.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) DSColors.inkGreen else DSColors.shadowGray,
            modifier = Modifier.size(18.dp),
        )
        Text(
            label,
            color = if (enabled) DSColors.inkGreen else DSColors.shadowGray,
            fontSize = 7.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun LoadingRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = DSColors.fairway, strokeWidth = 2.dp)
        Text("正在加载视频...", color = DSColors.shadowGray, fontSize = 13.sp)
    }
}
