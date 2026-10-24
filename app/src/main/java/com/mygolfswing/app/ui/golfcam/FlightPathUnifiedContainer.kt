package com.mygolfswing.app.ui.golfcam

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn as AndroidXOptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.CandidateImpactFrame
import com.mygolfswing.app.ui.golfcam.model.FlightCurveSelection
import com.mygolfswing.app.ui.golfcam.model.GolfCamFrameTimeline
import com.mygolfswing.app.ui.golfcam.model.GolfCamStep
import com.mygolfswing.app.ui.golfcam.model.GolfCamVideoConfig
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.ImpactCandidate
import com.mygolfswing.app.ui.golfcam.model.ImpactFrameSelection
import com.mygolfswing.app.ui.golfcam.model.ImpactPositionSelection
import com.mygolfswing.app.ui.golfcam.model.LandingOutput
import com.mygolfswing.app.ui.golfcam.model.NormalizedPoint
import com.mygolfswing.app.ui.golfcam.model.TrackStyleSelection
import com.mygolfswing.app.ui.golfcam.steps.FlightCurveStepTool
import com.mygolfswing.app.ui.golfcam.steps.FLIGHT_CURVE_STEP_TOOL_PANEL_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.GolfCamThumbnailCache
import com.mygolfswing.app.ui.golfcam.steps.ImpactFrameStepTool
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_FRAME_STEP_TOOL_PANEL_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.ImpactPositionStepTool
import com.mygolfswing.app.ui.golfcam.steps.IMPACT_POSITION_STEP_TOOL_PANEL_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.LandingStepTool
import com.mygolfswing.app.ui.golfcam.steps.LANDING_STEP_TOOL_PANEL_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.TrackStyleStepTool
import com.mygolfswing.app.ui.golfcam.steps.TRACK_STYLE_STEP_TOOL_PANEL_HEIGHT_DP
import com.mygolfswing.app.ui.golfcam.steps.TrimStepTool
import com.mygolfswing.app.ui.golfcam.steps.golfCamThumbnailPrioritySampleTimeMs
import com.mygolfswing.app.ui.golfcam.steps.golfCamTrimImpactPreloadProgressFraction
import com.mygolfswing.app.ui.golfcam.steps.golfCamTrimImpactPreloadSampleTimesMs
import com.mygolfswing.app.ui.golfcam.steps.landingSeekTimeMs
import com.mygolfswing.app.ui.golfcam.steps.landingOutputForPositionChange
import com.mygolfswing.app.ui.golfcam.steps.landingOutputForTimeChange
import com.mygolfswing.app.ui.golfcam.steps.shouldWaitForTrimImpactThumbnailPreload
import com.mygolfswing.app.ui.golfcam.steps.validImpactCandidateFramesForTrimRange
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal data class ImpactCandidateDetectionKey(
    val trimStartMs: Long,
    val trimEndMs: Long,
    val frameRate: Double,
)

internal const val THUMBNAIL_PRELOAD_OVERLAY_BACKGROUND_ALPHA = 0.70f
internal const val THUMBNAIL_PRELOAD_OVERLAY_CARD_CORNER_DP = 20
internal const val THUMBNAIL_PRELOAD_OVERLAY_CARD_HORIZONTAL_PADDING_DP = 32
internal const val THUMBNAIL_PRELOAD_OVERLAY_CARD_VERTICAL_PADDING_DP = 32
internal const val THUMBNAIL_PRELOAD_OVERLAY_ICON_SIZE_DP = 60
internal const val THUMBNAIL_PRELOAD_OVERLAY_CONTENT_SPACING_DP = 20
internal const val THUMBNAIL_PRELOAD_OVERLAY_TITLE_FONT_SP = 18
internal const val THUMBNAIL_PRELOAD_OVERLAY_SUBTITLE_FONT_SP = 14
internal const val THUMBNAIL_PRELOAD_OVERLAY_PROGRESS_WIDTH_DP = 200
internal const val THUMBNAIL_PRELOAD_OVERLAY_PROGRESS_HEIGHT_DP = 8
internal const val THUMBNAIL_PRELOAD_OVERLAY_PROGRESS_TEXT_FONT_SP = 16
internal const val TRIM_STEP_TOOL_PANEL_HEIGHT_DP = 150

internal fun stepToolPanelHeightDp(step: GolfCamStep): Int =
    when (step) {
        GolfCamStep.TRIM -> TRIM_STEP_TOOL_PANEL_HEIGHT_DP
        GolfCamStep.IMPACT_FRAME -> IMPACT_FRAME_STEP_TOOL_PANEL_HEIGHT_DP
        GolfCamStep.IMPACT_POSITION -> IMPACT_POSITION_STEP_TOOL_PANEL_HEIGHT_DP
        GolfCamStep.LANDING -> LANDING_STEP_TOOL_PANEL_HEIGHT_DP
        GolfCamStep.FLIGHT_CURVE -> FLIGHT_CURVE_STEP_TOOL_PANEL_HEIGHT_DP
        GolfCamStep.TRACK_STYLE -> TRACK_STYLE_STEP_TOOL_PANEL_HEIGHT_DP
    }

internal fun stepToolPanelPaddingDp(step: GolfCamStep): Pair<Int, Int> =
    if (
        step == GolfCamStep.TRIM ||
        step == GolfCamStep.IMPACT_FRAME ||
        step == GolfCamStep.IMPACT_POSITION ||
        step == GolfCamStep.LANDING ||
        step == GolfCamStep.FLIGHT_CURVE ||
        step == GolfCamStep.TRACK_STYLE
    ) {
        0 to 0
    } else {
        16 to 12
    }

internal fun thumbnailPreloadProgressPercentText(progress: Float): String {
    return "${(progress.coerceIn(0f, 1f) * 100f).roundToInt()}%"
}

internal fun isTrimImpactThumbnailPreloadNavigation(
    currentStep: GolfCamStep,
    nextStep: GolfCamStep,
): Boolean {
    return currentStep == GolfCamStep.TRIM && nextStep == GolfCamStep.IMPACT_FRAME
}

internal fun shouldStartTrimImpactThumbnailPreloadGate(
    currentStep: GolfCamStep,
    nextStep: GolfCamStep,
    isGateRunning: Boolean,
): Boolean {
    return isTrimImpactThumbnailPreloadNavigation(currentStep, nextStep) && !isGateRunning
}

internal fun shouldReuseImpactCandidates(
    currentKey: ImpactCandidateDetectionKey?,
    nextKey: ImpactCandidateDetectionKey,
    candidateFrames: List<CandidateImpactFrame>,
): Boolean {
    return candidateFrames.isNotEmpty() && currentKey == nextKey
}

internal fun shouldClearImpactCandidatesForNewDetection(
    currentKey: ImpactCandidateDetectionKey?,
    nextKey: ImpactCandidateDetectionKey,
    candidateFrames: List<CandidateImpactFrame>,
): Boolean {
    return candidateFrames.isNotEmpty() && currentKey != nextKey
}

internal fun impactCandidatesForTrimRange(
    rawCandidates: List<ImpactCandidate>,
    trimStartMs: Long,
    trimEndMs: Long,
    frameRate: Double,
): List<CandidateImpactFrame> {
    val safeEndMs = GolfCamFrameTimeline.endExclusiveMsAfterStart(trimStartMs, trimEndMs)
    val lastAnchorMs = GolfCamFrameTimeline.lastFrameTimeMsForRange(trimStartMs, safeEndMs, frameRate)
    val maxRelativeSec = GolfCamFrameTimeline.nonNegativeDeltaMs(trimStartMs, lastAnchorMs) / 1000.0
    val trimStartSec = trimStartMs / 1000.0
    val trimEndSec = safeEndMs / 1000.0
    val lastFrame = GolfCamFrameTimeline.lastFrameIndexForRange(trimStartMs, safeEndMs, frameRate)
    fun finiteConfidence(confidence: Float): Float {
        return (confidence.takeIf { it.isFinite() } ?: 0f).coerceIn(0f, 1f)
    }
    return rawCandidates
        .filter { raw -> raw.timeSec.isFinite() && raw.timeSec >= trimStartSec && raw.timeSec < trimEndSec }
        .map { raw ->
            val relTime = (raw.timeSec - trimStartSec).coerceIn(0.0, maxRelativeSec)
            val frameIndex = if (relTime >= maxRelativeSec) {
                lastFrame
            } else {
                GolfCamFrameTimeline.frameIndexAt(relTime, frameRate).coerceIn(0, lastFrame)
            }
            CandidateImpactFrame(
                frameIndex = frameIndex,
                confidence = finiteConfidence(raw.confidence),
                timeSec = relTime,
            )
        }
        .sortedByDescending { it.confidence }
}

internal fun curveFrameSeekTimeMs(
    trimStartMs: Long,
    trimEndMs: Long,
    frameIndex: Int,
    frameRate: Double,
): Long {
    val safeEndMs = GolfCamFrameTimeline.endExclusiveMsAfterStart(trimStartMs, trimEndMs)
    val lastFrame = GolfCamFrameTimeline.lastFrameIndexForRange(trimStartMs, safeEndMs, frameRate)
    val clampedFrame = frameIndex.coerceIn(0, lastFrame)
    val targetMs = GolfCamFrameTimeline.timeMsForFrame(trimStartMs, clampedFrame, frameRate)
    val lastAnchorMs = GolfCamFrameTimeline.lastFrameTimeMsForRange(trimStartMs, safeEndMs, frameRate)
    return targetMs.coerceIn(trimStartMs, lastAnchorMs)
}

internal fun videoConfigWithPlayerTiming(
    current: GolfCamVideoConfig?,
    durationMs: Long,
    frameRate: Double,
): GolfCamVideoConfig {
    val safeDurationMs = durationMs.coerceAtLeast(1L)
    val safeFrameRate = GolfCamFrameTimeline.safeFrameRate(frameRate)
    return current?.copy(durationMs = safeDurationMs, frameRate = safeFrameRate)
        ?: GolfCamVideoConfig(
            durationMs = safeDurationMs,
            frameRate = safeFrameRate,
            visualWidth = 1,
            visualHeight = 1,
        )
}

internal fun golfCamContainerThumbnailPreloadDurationMs(
    durationMs: Long,
    videoConfig: GolfCamVideoConfig?,
): Long? {
    return golfCamThumbnailPreloadDurationMs(videoConfig)
        ?: durationMs.takeIf { it >= 500L }
}

/**
 * 六步统一轨迹编辑容器（对齐 iOS FlightPathUnifiedContainer）。
 * 整体为米白浅色主题，步骤指示器为圆形徽章 + 连接线。
 */
@Composable
fun FlightPathUnifiedContainer(
    videoUri: Uri,
    state: GolfCamWorkflowState,
    initialStep: GolfCamStep = GolfCamStep.TRIM,
    onComplete: (TrackStyleSelection) -> Unit,
    onReselect: (() -> Unit)? = null,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(initialStep) }
    // FlightCurve 帧导航状态
    var flightCurveFrameIndex by remember { mutableIntStateOf(0) }
    var flightCurveTotalFrames by remember { mutableIntStateOf(0) }
    var durationMs by remember(videoUri) { mutableLongStateOf(state.videoConfig?.durationMs ?: 0L) }
    var videoFrameRate by remember(videoUri) { mutableDoubleStateOf(state.videoConfig?.frameRate ?: 30.0) }
    var trimPreviewPositionMs by remember { mutableLongStateOf(0L) }
    // 音频检测候选帧
    val restoredCandidateFrames = state.impactFrame?.candidateFrames.orEmpty()
    val restoredTrim = state.trimResult
    var candidateFrames by remember(videoUri) { mutableStateOf(restoredCandidateFrames) }
    var candidateDetectionKey by remember(videoUri) {
        mutableStateOf(
            if (restoredCandidateFrames.isNotEmpty() && restoredTrim != null) {
                ImpactCandidateDetectionKey(
                    trimStartMs = restoredTrim.startMs,
                    trimEndMs = restoredTrim.endMs,
                    frameRate = GolfCamFrameTimeline.safeFrameRate(state.videoConfig?.frameRate ?: 30.0),
                )
            } else {
                null
            },
        )
    }
    val impactFrameDraftIndex = remember(videoUri, restoredTrim?.startMs, restoredTrim?.endMs) {
        mutableIntStateOf(state.impactFrame?.frameIndex ?: 0)
    }
    var showHelp by remember { mutableStateOf(false) }
    var isPreloadingThumbnails by remember { mutableStateOf(false) }
    var isTrimImpactThumbnailPreloadGateRunning by remember { mutableStateOf(false) }
    var thumbnailPreloadProgress by remember { mutableFloatStateOf(0f) }
    var trackStyleSpeedPreviewMode by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) { onDispose { player.release() } }

    val openCurrentStep = coerceCurrentWorkflowStep(currentStep, state)
    LaunchedEffect(currentStep, openCurrentStep) {
        if (currentStep != openCurrentStep) {
            currentStep = openCurrentStep
        }
    }

    DisposableEffect(player) {
        @AndroidXOptIn(UnstableApi::class)
        fun updateFrameRate() {
            val frameRate = player.videoFormat?.frameRate ?: return
            if (frameRate.isFinite() && frameRate > 1f) {
                videoFrameRate = GolfCamFrameTimeline.safeFrameRate(frameRate.toDouble())
                state.updateVideoConfig(
                    videoConfigWithPlayerTiming(
                        current = state.videoConfig,
                        durationMs = durationMs,
                        frameRate = videoFrameRate,
                    ),
                )
            }
        }
        updateFrameRate()
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && durationMs <= 0L) {
                    durationMs = player.duration.coerceAtLeast(1L)
                    state.updateVideoConfig(
                        videoConfigWithPlayerTiming(
                            current = state.videoConfig,
                            durationMs = durationMs,
                            frameRate = videoFrameRate,
                        ),
                    )
                }
                updateFrameRate()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                updateFrameRate()
            }
        }
        player.addListener(listener)
        if (player.playbackState == Player.STATE_READY) {
            durationMs = player.duration.coerceAtLeast(1L)
            updateFrameRate()
        }
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(currentStep, player) {
        while (currentStep == GolfCamStep.TRIM) {
            trimPreviewPositionMs = player.currentPosition.coerceAtLeast(0L)
            delay(100L)
        }
    }

    LaunchedEffect(currentStep, state.impactFrame?.timeMs, player) {
        if (currentStep == GolfCamStep.IMPACT_POSITION) {
            state.impactFrame?.let { impactFrame ->
                player.seekTo(impactFrame.timeMs.coerceAtLeast(0L))
                player.pause()
            }
        }
    }

    LaunchedEffect(videoUri, golfCamContainerThumbnailPreloadDurationMs(durationMs, state.videoConfig)) {
        val preloadDurationMs = golfCamContainerThumbnailPreloadDurationMs(durationMs, state.videoConfig)
            ?: return@LaunchedEffect
        GolfCamThumbnailCache.preloadVideo(context, videoUri, preloadDurationMs)
    }

    fun goToNextStep() {
        val nextStep = currentStep.next ?: currentStep
        if (!isTrimImpactThumbnailPreloadNavigation(currentStep, nextStep)) {
            currentStep = nextStep
            return
        }
        if (!shouldStartTrimImpactThumbnailPreloadGate(currentStep, nextStep, isTrimImpactThumbnailPreloadGateRunning)) return
        val trim = state.trimResult
        if (trim == null) {
            currentStep = nextStep
            return
        }
        isTrimImpactThumbnailPreloadGateRunning = true
        scope.launch {
            try {
                player.pause()
                val sampleTimesMs = golfCamTrimImpactPreloadSampleTimesMs(
                    trimStartMs = trim.startMs,
                    trimEndMs = trim.endMs,
                    frameRate = videoFrameRate,
                )
                val exactCachedSampleTimes = GolfCamThumbnailCache.cachedExactSampleTimes(videoUri, sampleTimesMs)
                val loadedSampleTimes = exactCachedSampleTimes.toMutableSet()
                thumbnailPreloadProgress = golfCamTrimImpactPreloadProgressFraction(
                    exactCachedCount = loadedSampleTimes.size,
                    totalCount = sampleTimesMs.size,
                )
                isPreloadingThumbnails = true

                // Match iOS: thumbnail preparation and impact-sound detection run together,
                // and step 2 opens only after both tasks have finished.
                val impactDetection = async {
                    val nextDetectionKey = ImpactCandidateDetectionKey(trim.startMs, trim.endMs, videoFrameRate)
                    if (!shouldReuseImpactCandidates(candidateDetectionKey, nextDetectionKey, candidateFrames)) {
                        if (shouldClearImpactCandidatesForNewDetection(candidateDetectionKey, nextDetectionKey, candidateFrames)) {
                            val previousKey = candidateDetectionKey
                            candidateFrames = if (
                                previousKey?.trimStartMs == nextDetectionKey.trimStartMs &&
                                previousKey.trimEndMs == nextDetectionKey.trimEndMs
                            ) {
                                validImpactCandidateFramesForTrimRange(
                                    candidateFrames = candidateFrames,
                                    trimStartMs = trim.startMs,
                                    trimEndMs = trim.endMs,
                                    frameRate = videoFrameRate,
                                )
                            } else {
                                emptyList()
                            }
                        }
                        val rawCandidates = AudioImpactDetector.detect(
                            context = context,
                            uri = videoUri,
                            startSec = trim.startMs / 1000.0,
                            endSec = trim.endMs / 1000.0,
                        )
                        val detectedCandidates = impactCandidatesForTrimRange(
                            rawCandidates = rawCandidates,
                            trimStartMs = trim.startMs,
                            trimEndMs = trim.endMs,
                            frameRate = videoFrameRate,
                        )
                        if (detectedCandidates.isNotEmpty() || candidateFrames.isEmpty()) {
                            candidateFrames = detectedCandidates
                        }
                        candidateDetectionKey = nextDetectionKey
                    }
                }

                if (shouldWaitForTrimImpactThumbnailPreload(exactCachedSampleTimes.size, sampleTimesMs.size)) {
                    GolfCamThumbnailCache.loadTimelineThumbnails(
                        context = context,
                        videoUri = videoUri,
                        sampleTimesMs = sampleTimesMs,
                        priorityTimeMs = golfCamThumbnailPrioritySampleTimeMs(
                            sampleTimesMs,
                            state.impactFrame?.timeMs ?: trim.startMs,
                        ),
                        skipCachedSampleTimesMs = exactCachedSampleTimes,
                        frameOption = MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    ) { index, _ ->
                        loadedSampleTimes += sampleTimesMs.getOrNull(index)?.coerceAtLeast(0L) ?: return@loadTimelineThumbnails
                        thumbnailPreloadProgress = golfCamTrimImpactPreloadProgressFraction(
                            exactCachedCount = loadedSampleTimes.size,
                            totalCount = sampleTimesMs.size,
                        )
                    }
                    thumbnailPreloadProgress = 1f
                } else {
                    thumbnailPreloadProgress = 1f
                }
                impactDetection.await()
                currentStep = nextStep
            } finally {
                isPreloadingThumbnails = false
                isTrimImpactThumbnailPreloadGateRunning = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── 顶部导航栏 ──
            StepNavBar(
                currentStep = currentStep,
                onBack = onCancel,
                onHelp = { showHelp = true },
            )

            // ── 步骤进度指示器 ──
            StepBadgeRow(
                currentStep = currentStep,
                isStepCompleted = state::hasCompletedWorkflowStep,
                canOpenStep = state::canOpenWorkflowStep,
                onStepTap = { step ->
                    if (state.canOpenWorkflowStep(step)) currentStep = step
                },
            )

            // ── 视频预览区 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                UnifiedVideoPreview(
                    player = player,
                    currentStep = currentStep,
                    state = state,
                    videoFrameRate = videoFrameRate,
                    trackStyleSpeedPreviewMode = trackStyleSpeedPreviewMode,
                    candidateFrames = candidateFrames,
                    onCandidateFrameSelected = { candidate ->
                        val trim = state.trimResult
                        val startMs = trim?.startMs ?: 0L
                        val endMs = trim?.endMs ?: durationMs
                        val lastAnchorMs = GolfCamFrameTimeline.lastFrameTimeMsForRange(startMs, endMs, videoFrameRate)
                        val timeMs = GolfCamFrameTimeline.timeMsAfterSeconds(startMs, candidate.timeSec)
                            .coerceIn(startMs, lastAnchorMs)
                        player.seekTo(timeMs)
                        player.pause()
                        impactFrameDraftIndex.intValue = candidate.frameIndex
                        state.updateImpactFrame(
                            ImpactFrameSelection(
                                frameIndex = candidate.frameIndex,
                                timeMs = timeMs,
                                candidateFrames = candidateFrames,
                            ),
                        )
                    },
                    onImpactPositionChange = { nx, ny ->
                        state.updateImpactPosition(ImpactPositionSelection(nx, ny))
                    },
                    onLandingChange = { nx, ny ->
                        state.updateLanding(landingOutputForPositionChange(state.landing, nx, ny))
                    },
                    onFlightCurveChange = { cp1, cp2 ->
                        val impact = state.impactPosition ?: return@UnifiedVideoPreview
                        val landing = state.landing ?: return@UnifiedVideoPreview
                        state.updateFlightCurve(
                            FlightCurveSelection(
                                impactPosition = impact,
                                landingPoint = landing,
                                controlPoint1 = cp1,
                                controlPoint2 = cp2,
                                currentFrameIndex = flightCurveFrameIndex,
                            ),
                        )
                    },
                )
            }

            // ── 步骤工具区 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stepToolPanelHeightDp(currentStep).dp)
                    .background(Color.White)
                    .padding(
                        horizontal = stepToolPanelPaddingDp(currentStep).first.dp,
                        vertical = stepToolPanelPaddingDp(currentStep).second.dp,
                    )
                    .testTag("golfcam-step-tool-panel-${currentStep.name.lowercase()}"),
                contentAlignment = Alignment.Center,
            ) {
                when (currentStep) {
                    GolfCamStep.TRIM -> TrimStepTool(
                        videoUri = videoUri,
                        durationMs = durationMs,
                        savedTrimResult = state.trimResult,
                        currentTimeMs = trimPreviewPositionMs,
                        onSeek = { timeMs ->
                            trimPreviewPositionMs = timeMs
                            player.seekTo(timeMs)
                        },
                        onResult = { state.updateTrimResult(it) },
                    )
                    GolfCamStep.IMPACT_FRAME -> ImpactFrameStepTool(
                        player = player,
                        videoUri = videoUri,
                        durationMs = durationMs,
                        state = state,
                        candidateFrames = candidateFrames,
                        frameRate = videoFrameRate,
                        frameSelectionState = impactFrameDraftIndex,
                        onResult = { state.updateImpactFrame(it) },
                    )
                    GolfCamStep.IMPACT_POSITION -> ImpactPositionStepTool(state = state)
                    GolfCamStep.LANDING -> LandingStepTool(
                        videoUri = videoUri,
                        state = state,
                        frameRate = videoFrameRate,
                        onFlightTimeChange = { sec ->
                            state.updateLanding(landingOutputForTimeChange(state.landing, sec))
                            val trimStartMs = state.trimResult?.startMs ?: 0L
                            val impactMs = state.impactFrame?.timeMs ?: state.trimResult?.startMs ?: 0L
                            val clipEndMs = state.trimResult?.endMs ?: durationMs
                            player.seekTo(
                                landingSeekTimeMs(
                                    trimStartMs = trimStartMs,
                                    impactTimeMs = impactMs,
                                    flightTimeSec = sec,
                                    clipEndMs = clipEndMs,
                                    frameRate = videoFrameRate,
                                ),
                            )
                            player.pause()
                        },
                    )
                    GolfCamStep.FLIGHT_CURVE -> {
                        val trimStartMs = state.trimResult?.startMs ?: 0L
                        val trimEndMs = GolfCamFrameTimeline.endExclusiveMsAfterStart(
                            trimStartMs,
                            state.trimResult?.endMs?.takeIf { it > trimStartMs } ?: durationMs,
                        )
                        val fps = videoFrameRate
                        fun seekCurveFrame(frameIndex: Int) {
                            val target = curveFrameSeekTimeMs(
                                trimStartMs = trimStartMs,
                                trimEndMs = trimEndMs,
                                frameIndex = frameIndex,
                                frameRate = fps,
                            )
                            player.seekTo(target)
                            player.pause()
                        }
                        fun persistCurveFrameIndex() {
                            state.flightCurve?.let { current ->
                                state.updateFlightCurve(current.copy(currentFrameIndex = flightCurveFrameIndex))
                            }
                        }
                        // 进入曲线步骤时初始化帧数
                        LaunchedEffect(trimStartMs, trimEndMs, state.impactFrame, fps) {
                            if (durationMs > 0L) {
                                flightCurveTotalFrames = GolfCamFrameTimeline.totalFramesForRange(trimStartMs, trimEndMs, fps)
                                flightCurveFrameIndex = (state.impactFrame?.frameIndex ?: flightCurveFrameIndex)
                                    .coerceIn(0, (flightCurveTotalFrames - 1).coerceAtLeast(0))
                                seekCurveFrame(flightCurveFrameIndex)
                            }
                        }
                        FlightCurveStepTool(
                            videoUri = videoUri,
                            trimStartMs = trimStartMs,
                            state = state,
                            currentFrameIndex = flightCurveFrameIndex,
                            totalFrames = flightCurveTotalFrames,
                            frameRate = fps,
                            onFrameSelected = { frameIndex ->
                                flightCurveFrameIndex = frameIndex.coerceIn(0, (flightCurveTotalFrames - 1).coerceAtLeast(0))
                                seekCurveFrame(flightCurveFrameIndex)
                                persistCurveFrameIndex()
                            },
                            onImpactFrame = {
                                flightCurveFrameIndex = (state.impactFrame?.frameIndex ?: 0)
                                    .coerceIn(0, (flightCurveTotalFrames - 1).coerceAtLeast(0))
                                seekCurveFrame(flightCurveFrameIndex)
                                persistCurveFrameIndex()
                            },
                            onPrevFrame = {
                                if (flightCurveFrameIndex > 0) {
                                    flightCurveFrameIndex--
                                    seekCurveFrame(flightCurveFrameIndex)
                                    persistCurveFrameIndex()
                                }
                            },
                            onNextFrame = {
                                if (flightCurveFrameIndex < flightCurveTotalFrames - 1) {
                                    flightCurveFrameIndex++
                                    seekCurveFrame(flightCurveFrameIndex)
                                    persistCurveFrameIndex()
                                }
                            },
                        )
                    }
                    GolfCamStep.TRACK_STYLE -> TrackStyleStepTool(
                        state = state,
                        onResult = { state.updateTrackStyle(it) },
                        onSpeedPreviewModeChanged = { trackStyleSpeedPreviewMode = it },
                    )
                }
            }

            // ── 底部三栏导航栏 ──
            StepBottomBar(
                currentStep = currentStep,
                state = state,
                onLeftAction = {
                    when {
                        currentStep.isFirst && onReselect != null -> onReselect()
                        currentStep.isFirst -> onCancel()
                        else -> currentStep = currentStep.prev ?: currentStep
                    }
                },
                onNext = ::goToNextStep,
                onComplete = { state.trackStyle?.let { onComplete(it) } },
            )
        }

        if (showHelp) {
            StepHelpPopCard(
                step = currentStep,
                onDismiss = { showHelp = false },
            )
        }

        if (isPreloadingThumbnails) {
            ThumbnailPreloadOverlay(progress = thumbnailPreloadProgress)
        }
    }
}

// ─── 顶部导航栏 ──────────────────────────────────────────────────────────────────

@Composable
private fun StepNavBar(
    currentStep: GolfCamStep,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, ambientColor = DSColors.deepGreen.copy(alpha = 0.05f), spotColor = DSColors.deepGreen.copy(alpha = 0.05f))
            .background(DSColors.cardWhite)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Filled.Home,
                contentDescription = "返回",
                tint = DSColors.inkGreen,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = currentStep.title,
            color = DSColors.inkGreen,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onHelp, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = "帮助",
                tint = DSColors.inkGreen,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ThumbnailPreloadOverlay(
    progress: Float,
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("golfcam-thumbnail-preload-overlay")
            .background(Color.Black.copy(alpha = THUMBNAIL_PRELOAD_OVERLAY_BACKGROUND_ALPHA))
            .clickable { },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(THUMBNAIL_PRELOAD_OVERLAY_CARD_CORNER_DP.dp),
                    ambientColor = DSColors.fairway.copy(alpha = 0.30f),
                    spotColor = DSColors.fairway.copy(alpha = 0.30f),
                )
                .clip(RoundedCornerShape(THUMBNAIL_PRELOAD_OVERLAY_CARD_CORNER_DP.dp))
                .background(Color(0xFF1C1C1E))
                .padding(
                    horizontal = THUMBNAIL_PRELOAD_OVERLAY_CARD_HORIZONTAL_PADDING_DP.dp,
                    vertical = THUMBNAIL_PRELOAD_OVERLAY_CARD_VERTICAL_PADDING_DP.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(THUMBNAIL_PRELOAD_OVERLAY_CONTENT_SPACING_DP.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(THUMBNAIL_PRELOAD_OVERLAY_ICON_SIZE_DP.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(DSColors.fairway, DSColors.deepGreen))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = "正在准备击球帧",
                color = Color.White,
                fontSize = THUMBNAIL_PRELOAD_OVERLAY_TITLE_FONT_SP.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "优化缩略图并识别击球声音",
                color = Color.White.copy(alpha = 0.80f),
                fontSize = THUMBNAIL_PRELOAD_OVERLAY_SUBTITLE_FONT_SP.sp,
                textAlign = TextAlign.Center,
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(THUMBNAIL_PRELOAD_OVERLAY_PROGRESS_WIDTH_DP.dp)
                        .height(THUMBNAIL_PRELOAD_OVERLAY_PROGRESS_HEIGHT_DP.dp)
                        .clip(RoundedCornerShape((THUMBNAIL_PRELOAD_OVERLAY_PROGRESS_HEIGHT_DP / 2).dp))
                        .background(Color.White.copy(alpha = 0.20f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(safeProgress)
                            .height(THUMBNAIL_PRELOAD_OVERLAY_PROGRESS_HEIGHT_DP.dp)
                            .clip(RoundedCornerShape((THUMBNAIL_PRELOAD_OVERLAY_PROGRESS_HEIGHT_DP / 2).dp))
                            .background(Brush.linearGradient(listOf(DSColors.fairway, DSColors.deepGreen))),
                    )
                }
                Text(
                    text = thumbnailPreloadProgressPercentText(safeProgress),
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = THUMBNAIL_PRELOAD_OVERLAY_PROGRESS_TEXT_FONT_SP.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ─── 步骤圆形徽章进度条 ────────────────────────────────────────────────────────────

@Composable
private fun StepBadgeRow(
    currentStep: GolfCamStep,
    isStepCompleted: (GolfCamStep) -> Boolean,
    canOpenStep: (GolfCamStep) -> Boolean,
    onStepTap: (GolfCamStep) -> Unit,
) {
    val steps = GolfCamStep.values()
    val circleSize = 28.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.cardWhite)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = step == currentStep
            val isDone = !isActive && isStepCompleted(step)
            val canTap = canOpenStep(step)

            // 步骤圆形徽章（占用固定宽度，均分）
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(44.dp)
                    .clickable(enabled = canTap) { onStepTap(step) },
            ) {
                Box(
                    modifier = Modifier
                        .size(circleSize)
                        .clip(CircleShape)
                        .background(
                            when {
                                isActive -> DSColors.fairway.copy(alpha = 0.16f)
                                isDone -> DSColors.fairway
                                else -> DSColors.mistGreen
                            },
                        )
                        .then(
                            if (isActive) Modifier.border(2.dp, DSColors.fairway, CircleShape)
                            else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        isDone -> Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        isActive -> Icon(
                            step.icon,
                            contentDescription = null,
                            tint = DSColors.fairway,
                            modifier = Modifier.size(12.dp),
                        )
                        else -> Text(
                            text = "${step.index + 1}",
                            color = if (canTap) DSColors.shadowGray else DSColors.shadowGray.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.shortTitle,
                    color = when {
                        isActive -> DSColors.inkGreen
                        isDone -> DSColors.fairway
                        !canTap -> DSColors.shadowGray.copy(alpha = 0.45f)
                        else -> DSColors.shadowGray
                    },
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }

            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = circleSize / 2 - 1.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            if (isDone) DSColors.fairway else DSColors.mistGreen,
                        ),
                )
            }
        }
    }
}

internal fun GolfCamWorkflowState.hasCompletedWorkflowStep(step: GolfCamStep): Boolean {
    return when (step) {
        GolfCamStep.TRIM -> hasCompletedTrim
        GolfCamStep.IMPACT_FRAME -> hasCompletedImpactFrame
        GolfCamStep.IMPACT_POSITION -> hasCompletedImpactPosition
        GolfCamStep.LANDING -> hasCompletedLanding
        GolfCamStep.FLIGHT_CURVE -> hasCompletedFlightCurve
        GolfCamStep.TRACK_STYLE -> hasCompletedTrackStyle
    }
}

internal fun GolfCamWorkflowState.canOpenWorkflowStep(step: GolfCamStep): Boolean {
    return when (step) {
        GolfCamStep.TRIM -> true
        GolfCamStep.IMPACT_FRAME -> hasCompletedTrim
        GolfCamStep.IMPACT_POSITION -> hasCompletedImpactFrame
        GolfCamStep.LANDING -> hasCompletedImpactPosition
        GolfCamStep.FLIGHT_CURVE -> hasCompletedLanding
        GolfCamStep.TRACK_STYLE -> hasCompletedFlightCurve
    }
}

internal fun coerceCurrentWorkflowStep(
    currentStep: GolfCamStep,
    state: GolfCamWorkflowState,
): GolfCamStep {
    return if (state.canOpenWorkflowStep(currentStep)) currentStep else state.resumeStep
}

// ─── 底部三栏固定导航栏 ──────────────────────────────────────────────────────────

@Composable
private fun StepBottomBar(
    currentStep: GolfCamStep,
    state: GolfCamWorkflowState,
    onLeftAction: () -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit,
) {
    val canContinue = state.canContinueFrom(currentStep)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, ambientColor = DSColors.deepGreen.copy(alpha = 0.08f), spotColor = DSColors.deepGreen.copy(alpha = 0.08f))
            .background(DSColors.cardWhite)
            .navigationBarsPadding()
            .padding(top = 1.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DSColors.mistGreen.copy(alpha = 0.7f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FooterActionButton(
                text = if (currentStep.isFirst) "重新选择" else "上一步",
                icon = if (currentStep.isFirst) Icons.Filled.Refresh else Icons.AutoMirrored.Filled.ArrowBack,
                enabled = true,
                primary = false,
                onClick = onLeftAction,
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "${currentStep.index + 1} / 6",
                    color = DSColors.inkGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    currentStep.title,
                    color = DSColors.shadowGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                )
            }

            FooterActionButton(
                text = if (currentStep.isLast) "完成" else "下一步",
                icon = if (currentStep.isLast) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
                enabled = canContinue,
                primary = true,
                onClick = {
                    if (currentStep.isLast) onComplete() else onNext()
                },
            )
        }
    }
}

@Composable
private fun FooterActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val background = if (primary && enabled) {
        Brush.linearGradient(listOf(DSColors.fairway, DSColors.deepGreen))
    } else {
        Brush.linearGradient(listOf(DSColors.mistGreen, DSColors.mistGreen))
    }
    val contentColor = when {
        primary && enabled -> Color.White
        primary -> DSColors.shadowGray
        else -> DSColors.inkGreen
    }
    Row(
        modifier = Modifier
            .width(118.dp)
            .height(44.dp)
            .clip(shape)
            .background(background)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (!primary) {
            Icon(icon, null, tint = contentColor.copy(alpha = if (enabled) 1f else 0.45f), modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text,
            color = contentColor.copy(alpha = if (enabled) 1f else 0.55f),
            fontSize = 14.sp,
            fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Medium,
        )
        if (primary) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(icon, null, tint = contentColor.copy(alpha = if (enabled) 1f else 0.55f), modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun StepHelpPopCard(
    step: GolfCamStep,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2C2C2E))
                .clickable { }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(step.icon, null, tint = DSColors.fairway, modifier = Modifier.size(18.dp))
                Text(step.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, null, tint = Color.White.copy(alpha = 0.68f), modifier = Modifier.size(13.dp))
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                step.helpItems().forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(DSColors.fairway.copy(alpha = 0.8f)),
                        )
                        Text(item, color = Color(0xFFD1D1D6), fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

private fun GolfCamWorkflowState.canContinueFrom(step: GolfCamStep): Boolean {
    return when (step) {
        GolfCamStep.TRIM -> hasCompletedTrim
        GolfCamStep.IMPACT_FRAME -> hasCompletedImpactFrame
        GolfCamStep.IMPACT_POSITION -> hasCompletedImpactPosition
        GolfCamStep.LANDING -> hasCompletedLanding
        GolfCamStep.FLIGHT_CURVE -> hasCompletedFlightCurve
        GolfCamStep.TRACK_STYLE -> hasCompletedTrackStyle
    }
}

private fun GolfCamStep.helpItems(): List<String> = when (this) {
    GolfCamStep.TRIM -> listOf("拖动两端手柄选择有效片段", "建议保留从挥杆准备到球落地的完整过程")
    GolfCamStep.IMPACT_FRAME -> listOf("拖动缩略图时间轴定位", "使用左右按钮微调", "系统会自动检测候选击球帧")
    GolfCamStep.IMPACT_POSITION -> listOf("拖动标记点到球的位置", "尽量精确标记球的中心")
    GolfCamStep.LANDING -> listOf("拖动时间轴设置飞行时间", "在画面中标记落点位置", "预览曲线会实时更新")
    GolfCamStep.FLIGHT_CURVE -> listOf("拖动 P1/P2 控制点调整曲线形状", "预览线会实时显示", "点击击球帧可快速回到击球时刻")
    GolfCamStep.TRACK_STYLE -> listOf("选择颜色方案", "调整宽度、速度与拖尾长度", "距离标签会同步到预览和导出")
}
