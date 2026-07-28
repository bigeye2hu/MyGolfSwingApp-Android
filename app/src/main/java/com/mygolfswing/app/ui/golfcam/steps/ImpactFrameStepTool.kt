package com.mygolfswing.app.ui.golfcam.steps

import android.net.Uri
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import com.mygolfswing.app.ui.golfcam.model.GolfCamFrameTimeline
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.ImpactFrameSelection
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val IMPACT_FRAME_THUMBNAIL_COUNT = 60       // 最多 60 张缩略图
internal const val IMPACT_FRAME_THUMBNAIL_CELL_WIDTH_DP = 32
internal const val IMPACT_FRAME_THUMBNAIL_CELL_HEIGHT_DP = 45
internal const val IMPACT_FRAME_TIMELINE_HEIGHT_DP = 50
internal const val IMPACT_FRAME_THUMBNAIL_GAP_DP = 2
internal const val IMPACT_FRAME_THUMBNAIL_CORNER_RADIUS_DP = 4
internal const val IMPACT_FRAME_THUMBNAIL_SELECTED_BORDER_DP = 2
internal const val IMPACT_FRAME_THUMBNAIL_SELECTED_SCALE = 1.1f
internal const val IMPACT_FRAME_THUMBNAIL_UNSELECTED_ALPHA = 0.6f
internal const val IMPACT_FRAME_CANDIDATE_DOT_SIZE_DP = 6
internal const val IMPACT_FRAME_CANDIDATE_DOT_TOP_PADDING_DP = 2
internal const val IMPACT_FRAME_NAV_BUTTON_WIDTH_DP = 44
internal const val IMPACT_FRAME_NAV_BUTTON_HEIGHT_DP = 50
private val CELL_W: Dp = IMPACT_FRAME_THUMBNAIL_CELL_WIDTH_DP.dp
private val CELL_H: Dp = IMPACT_FRAME_THUMBNAIL_CELL_HEIGHT_DP.dp
private val TIMELINE_H: Dp = IMPACT_FRAME_TIMELINE_HEIGHT_DP.dp
private val CELL_GAP: Dp = IMPACT_FRAME_THUMBNAIL_GAP_DP.dp

internal fun initialImpactFrameIndex(
    savedFrameIndex: Int?,
    candidateFrames: List<CandidateImpactFrame>,
    totalFrames: Int,
): Int {
    val lastFrame = (totalFrames - 1).coerceAtLeast(0)
    return (savedFrameIndex ?: candidateFrames.maxByOrNull { finiteCandidateConfidence(it.confidence) }?.frameIndex ?: (totalFrames / 2))
        .coerceIn(0, lastFrame)
}

internal fun asyncImpactCandidateReplacementFrame(
    savedFrameIndexOnEntry: Int?,
    currentFrame: Int,
    candidateFrames: List<CandidateImpactFrame>,
    totalFrames: Int,
): Int? {
    if (savedFrameIndexOnEntry != null || candidateFrames.isEmpty()) return null
    val lastFrame = (totalFrames - 1).coerceAtLeast(0)
    val autoMiddleFrame = (totalFrames / 2).coerceIn(0, lastFrame)
    if (currentFrame.coerceIn(0, lastFrame) != autoMiddleFrame) return null
    return candidateFrames.maxByOrNull { finiteCandidateConfidence(it.confidence) }?.frameIndex?.coerceIn(0, lastFrame)
}

internal fun impactFrameDisplayText(currentFrame: Int, totalFrames: Int): String {
    val safeTotal = totalFrames.coerceAtLeast(1)
    val oneBasedFrame = currentFrame.coerceIn(0, safeTotal - 1) + 1
    return "$oneBasedFrame/$safeTotal"
}

internal fun impactFrameStepFrame(currentFrame: Int, totalFrames: Int, deltaFrames: Int): Int {
    val lastFrame = (totalFrames - 1).coerceAtLeast(0)
    return (currentFrame + deltaFrames).coerceIn(0, lastFrame)
}

internal fun formatImpactFrameTimeLabel(seconds: Double): String {
    val safeSeconds = seconds.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
    return String.format(Locale.US, "%.2fs", safeSeconds)
}

internal fun shouldUpdateImpactFrameSelection(
    currentFrame: Int,
    durationMs: Long,
    frameRate: Double,
    candidateFrames: List<CandidateImpactFrame>,
): Boolean {
    return currentFrame >= 0 && durationMs > 0L && frameRate.isFinite() && frameRate > 0.0
}

private fun finiteCandidateConfidence(confidence: Float): Float {
    return (confidence.takeIf { it.isFinite() } ?: 0f).coerceIn(0f, 1f)
}

internal fun validImpactCandidateFramesForTrimRange(
    candidateFrames: List<CandidateImpactFrame>,
    trimStartMs: Long,
    trimEndMs: Long,
    frameRate: Double,
): List<CandidateImpactFrame> {
    val safeEndMs = GolfCamFrameTimeline.endExclusiveMsAfterStart(trimStartMs, trimEndMs)
    val lastFrame = GolfCamFrameTimeline.lastFrameIndexForRange(trimStartMs, safeEndMs, frameRate)
    val lastAnchorMs = GolfCamFrameTimeline.lastFrameTimeMsForRange(trimStartMs, safeEndMs, frameRate)
    val maxRelativeSec = GolfCamFrameTimeline.nonNegativeDeltaMs(trimStartMs, lastAnchorMs) / 1000.0
    return candidateFrames.mapNotNull { candidate ->
        val relTime = candidate.timeSec
        if (!relTime.isFinite()) return@mapNotNull null
        if (relTime !in 0.0..maxRelativeSec) return@mapNotNull null
        val frameIndex = if (relTime >= maxRelativeSec) {
            lastFrame
        } else {
            GolfCamFrameTimeline.frameIndexAt(relTime, frameRate).coerceIn(0, lastFrame)
        }
        CandidateImpactFrame(
            frameIndex = frameIndex,
            confidence = finiteCandidateConfidence(candidate.confidence),
            timeSec = relTime,
        )
    }
}

internal fun impactFrameSelectionForFrame(
    trimStartMs: Long,
    trimEndMs: Long,
    currentFrame: Int,
    durationMs: Long,
    frameRate: Double,
    candidateFrames: List<CandidateImpactFrame>,
): ImpactFrameSelection? {
    if (!shouldUpdateImpactFrameSelection(currentFrame, durationMs, frameRate, candidateFrames)) return null
    val safeEndMs = GolfCamFrameTimeline.endExclusiveMsAfterStart(trimStartMs, trimEndMs)
    val lastFrame = GolfCamFrameTimeline.lastFrameIndexForRange(trimStartMs, safeEndMs, frameRate)
    val clampedFrame = currentFrame.coerceIn(0, lastFrame)
    val lastAnchorMs = GolfCamFrameTimeline.lastFrameTimeMsForRange(trimStartMs, safeEndMs, frameRate)
    val timeMs = GolfCamFrameTimeline.timeMsForFrame(trimStartMs, clampedFrame, frameRate)
        .coerceIn(trimStartMs, lastAnchorMs)
    return ImpactFrameSelection(
        frameIndex = clampedFrame,
        timeMs = timeMs,
        candidateFrames = validImpactCandidateFramesForTrimRange(
            candidateFrames = candidateFrames,
            trimStartMs = trimStartMs,
            trimEndMs = safeEndMs,
            frameRate = frameRate,
        ),
    )
}

internal fun impactFrameThumbnailFrameIndices(
    totalFrames: Int,
    maxThumbs: Int = IMPACT_FRAME_THUMBNAIL_COUNT,
): List<Int> {
    val safeTotal = totalFrames.coerceAtLeast(1)
    val thumbCount = maxThumbs.coerceIn(1, safeTotal)
    if (thumbCount == 1) return listOf(0)
    val lastFrame = safeTotal - 1
    return (0 until thumbCount).map { index ->
        (index * lastFrame.toDouble() / (thumbCount - 1))
            .roundToInt()
            .coerceIn(0, lastFrame)
    }
}

internal fun impactFrameThumbnailSampleTimeMs(
    trimStartMs: Long,
    trimEndMs: Long,
    frameRate: Double,
    thumbIndex: Int,
    maxThumbs: Int = IMPACT_FRAME_THUMBNAIL_COUNT,
): Long {
    val safeEndMs = GolfCamFrameTimeline.endExclusiveMsAfterStart(trimStartMs, trimEndMs)
    val totalFrames = GolfCamFrameTimeline.totalFramesForRange(trimStartMs, safeEndMs, frameRate)
    val frames = impactFrameThumbnailFrameIndices(totalFrames, maxThumbs)
    val frameIndex = frames[thumbIndex.coerceIn(0, frames.lastIndex)]
    return GolfCamFrameTimeline.timeMsForFrame(trimStartMs, frameIndex, frameRate)
        .coerceIn(
            trimStartMs,
            GolfCamFrameTimeline.lastFrameTimeMsForRange(trimStartMs, safeEndMs, frameRate),
        )
}

internal fun nearestImpactFrameThumbnailIndex(
    frameIndex: Int,
    totalFrames: Int,
    thumbnailFrames: List<Int>,
): Int {
    if (thumbnailFrames.isEmpty()) return 0
    val lastFrame = (totalFrames - 1).coerceAtLeast(0)
    val safeFrame = frameIndex.coerceIn(0, lastFrame)
    return thumbnailFrames
        .indices
        .minByOrNull { index -> golfCamFrameIndexDistance(thumbnailFrames[index], safeFrame) }
        ?: 0
}

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
    frameRate: Double = 30.0,
    onResult: (ImpactFrameSelection) -> Unit,
) {
    val safeFrameRate = GolfCamFrameTimeline.safeFrameRate(frameRate)
    val startMs = state.trimResult?.startMs ?: 0L
    val endMs = GolfCamFrameTimeline.endExclusiveMsAfterStart(
        startMs,
        state.trimResult?.endMs?.takeIf { it > 0L } ?: durationMs,
    )
    val trimDurationMs = (endMs - startMs).coerceAtLeast(1L)
    val totalFrames = GolfCamFrameTimeline.totalFrames(trimDurationMs, safeFrameRate)
    val savedFrameIndexOnEntry = remember(startMs, endMs) { state.impactFrame?.frameIndex }

    // 初始帧：优先已保存 → 最高候选 → 中间帧
    val initialFrame = remember(savedFrameIndexOnEntry, candidateFrames, totalFrames) {
        initialImpactFrameIndex(
            savedFrameIndex = savedFrameIndexOnEntry,
            candidateFrames = candidateFrames,
            totalFrames = totalFrames,
        )
    }

    var currentFrame by remember(startMs, endMs, safeFrameRate, totalFrames, savedFrameIndexOnEntry) {
        mutableIntStateOf(initialFrame)
    }
    LaunchedEffect(savedFrameIndexOnEntry, candidateFrames, totalFrames) {
        asyncImpactCandidateReplacementFrame(
            savedFrameIndexOnEntry = savedFrameIndexOnEntry,
            currentFrame = currentFrame,
            candidateFrames = candidateFrames,
            totalFrames = totalFrames,
        )?.let { currentFrame = it }
    }

    // step：多少个真实帧 对应 1个缩略图格
    val thumbnailFrames = remember(totalFrames) {
        impactFrameThumbnailFrameIndices(totalFrames)
    }
    val thumbCount = thumbnailFrames.size

    // 通知父层
    LaunchedEffect(startMs, endMs, currentFrame, durationMs, safeFrameRate, candidateFrames) {
        impactFrameSelectionForFrame(
            trimStartMs = startMs,
            trimEndMs = endMs,
            currentFrame = currentFrame,
            durationMs = durationMs,
            frameRate = safeFrameRate,
            candidateFrames = candidateFrames,
        )?.let { selection ->
            player.seekTo(selection.timeMs)
            player.pause()
            onResult(selection)
        }
    }

    if (durationMs <= 0L) {
        LoadingRow()
        return
    }

    val elapsedSec = currentFrame / safeFrameRate

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
                    formatImpactFrameTimeLabel(elapsedSec),
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
                        impactFrameDisplayText(currentFrame, totalFrames),
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
            thumbnailFrames = thumbnailFrames,
            frameRate = safeFrameRate,
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
                "滑动选择球杆击中球的瞬间，或点击上方候选帧",
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
    thumbnailFrames: List<Int>,
    frameRate: Double,
    currentFrame: Int,
    candidateFrames: List<CandidateImpactFrame>,
    onFrameChanged: (Int) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val thumbnailSampleTimesMs = remember(startMs, trimDurationMs, thumbCount, frameRate) {
        (0 until thumbCount).map { thumbIndex ->
            impactFrameThumbnailSampleTimeMs(
                trimStartMs = startMs,
                trimEndMs = GolfCamFrameTimeline.timeMsAfter(startMs, trimDurationMs),
                frameRate = frameRate,
                thumbIndex = thumbIndex,
                maxThumbs = thumbCount,
            )
        }
    }

    var thumbnails by remember(videoUri, thumbnailSampleTimesMs) {
        mutableStateOf(GolfCamThumbnailCache.cachedTimelineThumbnails(videoUri, thumbnailSampleTimesMs))
    }
    val priorityTimeMs = GolfCamFrameTimeline.timeMsForFrame(startMs, currentFrame, frameRate)
    val priorityThumbnailIndex = remember(thumbnailSampleTimesMs, priorityTimeMs) {
        golfCamThumbnailPriorityIndex(thumbnailSampleTimesMs, priorityTimeMs)
    }
    val prioritySampleTimeMs = remember(thumbnailSampleTimesMs, priorityThumbnailIndex, priorityTimeMs) {
        golfCamThumbnailPrioritySampleTimeMs(thumbnailSampleTimesMs, priorityTimeMs)
    }

    LaunchedEffect(videoUri, thumbnailSampleTimesMs, priorityThumbnailIndex) {
        if (thumbCount <= 0 || trimDurationMs <= 0) return@LaunchedEffect
        val exactCachedSampleTimes = GolfCamThumbnailCache.cachedExactSampleTimes(videoUri, thumbnailSampleTimesMs)
        thumbnails = GolfCamThumbnailCache.cachedTimelineThumbnails(videoUri, thumbnailSampleTimesMs)
        GolfCamThumbnailCache.loadTimelineThumbnails(
            context = context,
            videoUri = videoUri,
            sampleTimesMs = thumbnailSampleTimesMs,
            priorityTimeMs = prioritySampleTimeMs,
            skipCachedSampleTimesMs = exactCachedSampleTimes,
        ) { index, bitmap ->
            thumbnails = golfCamFillMissingThumbnailSlotsFromNearest(
                thumbnails.toMutableList().also { current ->
                    if (index in current.indices) current[index] = bitmap
                },
            )
        }
    }

    val displayThumbnails = remember(thumbnails, thumbCount) {
        if (thumbnails.size == thumbCount) {
            thumbnails
        } else {
            List(thumbCount) { index -> thumbnails.getOrNull(index) }
        }
    }

    // 当前缩略图格（thumbIndex）对应的真实帧 index
    fun thumbIndexToFrame(thumbIdx: Int): Int {
        return thumbnailFrames.getOrElse(thumbIdx.coerceIn(0, thumbCount - 1)) { 0 }
    }
    fun frameToThumbIndex(frameIdx: Int): Int {
        return nearestImpactFrameThumbnailIndex(frameIdx, totalFrames, thumbnailFrames)
    }

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

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val sidePad = ((maxWidth - CELL_W) / 2).coerceAtLeast(0.dp)  // 使第一/最后一张可滚到中心
        val sidePadPx = with(density) { sidePad.toPx().toInt() }
        val snapFlingBehavior = rememberSnapFlingBehavior(listState)

        // 代码改帧时滚动轨道，并和 iOS .centeredHorizontally 一样把目标缩略图居中。
        LaunchedEffect(currentFrame, sidePadPx) {
            val targetThumb = frameToThumbIndex(currentFrame)
            if (!listState.isScrollInProgress) {
                listState.animateScrollToItem(targetThumb, scrollOffset = -sidePadPx)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 上一帧
            NavButton(icon = Icons.Filled.ChevronLeft, label = "上一帧", enabled = currentFrame > 0) {
                onFrameChanged(impactFrameStepFrame(currentFrame, totalFrames, deltaFrames = -1))
            }

            // 缩略图条
            Box(modifier = Modifier.weight(1f)) {
                LazyRow(
                    state = listState,
                    flingBehavior = snapFlingBehavior,
                    contentPadding = PaddingValues(horizontal = sidePad),
                    horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TIMELINE_H)
                        .clip(RoundedCornerShape(6.dp)),
                ) {
                    itemsIndexed(displayThumbnails) { thumbIdx, bmp ->
                        val frameIdx = thumbIndexToFrame(thumbIdx)
                        val selectedThumb = frameToThumbIndex(currentFrame)
                        val isSelected = thumbIdx == selectedThumb
                        val isCandidate = candidateFrames.any { frameToThumbIndex(it.frameIndex) == thumbIdx }

                        Box(
                            modifier = Modifier
                                .width(CELL_W)
                                .height(TIMELINE_H)
                                .clickable { onFrameChanged(frameIdx) },
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            // 缩略图
                            val imgBitmap = bmp?.asImageBitmap()
                            Box(
                                modifier = Modifier
                                    .width(CELL_W)
                                    .height(CELL_H)
                                    .graphicsLayer {
                                        alpha = if (isSelected) 1f else IMPACT_FRAME_THUMBNAIL_UNSELECTED_ALPHA
                                        scaleX = if (isSelected) IMPACT_FRAME_THUMBNAIL_SELECTED_SCALE else 1f
                                        scaleY = if (isSelected) IMPACT_FRAME_THUMBNAIL_SELECTED_SCALE else 1f
                                    }
                                    .clip(RoundedCornerShape(IMPACT_FRAME_THUMBNAIL_CORNER_RADIUS_DP.dp))
                                    .background(Color(0xFF1A1A1A))
                                    .then(
                                        if (isSelected) Modifier.border(
                                            IMPACT_FRAME_THUMBNAIL_SELECTED_BORDER_DP.dp,
                                            DSColors.fairway,
                                            RoundedCornerShape(IMPACT_FRAME_THUMBNAIL_CORNER_RADIUS_DP.dp),
                                        ) else Modifier,
                                    ),
                            ) {
                                if (imgBitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = imgBitmap,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize(),
                                    )
                                }
                            }

                            // 候选帧金色小圆点
                            if (isCandidate) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = IMPACT_FRAME_CANDIDATE_DOT_TOP_PADDING_DP.dp)
                                        .size(IMPACT_FRAME_CANDIDATE_DOT_SIZE_DP.dp)
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
                onFrameChanged(impactFrameStepFrame(currentFrame, totalFrames, deltaFrames = 1))
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
            .width(IMPACT_FRAME_NAV_BUTTON_WIDTH_DP.dp)
            .height(IMPACT_FRAME_NAV_BUTTON_HEIGHT_DP.dp)
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
