package com.mygolfswing.app.ui.player

import android.os.SystemClock
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 回看 / 逐帧唯一的拖动区：上面是进度条，下面是刻度轴。
 * 进度条跟着播放走。刻度轴是滚轮，只在手指拖的时候动，播放时不动。
 * 挥杆详情、回看、互动逐帧、Memo 打开视频都用这个，不要再各写一条轴。
 */
internal enum class StandaloneTimelineScrubFeel(
    val title: String,
) {
    BALANCED("稳"),
    PRECISE("细"),
    FRAME("帧"),
}

internal data class StandaloneTimelineScrubTuning(
    val normalDragDpPerSecond: Double,
    val smallDragDpPerSecond: Double,
    val smallDragVelocityThreshold: Double,
    val velocityCurve: Double,
    val flickVelocityThreshold: Double,
    val normalInertiaFactor: Double,
    val flickInertiaFactor: Double,
) {
    companion object {
        fun recommended(feel: StandaloneTimelineScrubFeel): StandaloneTimelineScrubTuning = when (feel) {
            StandaloneTimelineScrubFeel.BALANCED -> StandaloneTimelineScrubTuning(
                normalDragDpPerSecond = 160.0,
                smallDragDpPerSecond = 380.0,
                smallDragVelocityThreshold = 260.0,
                velocityCurve = 1.25,
                flickVelocityThreshold = 820.0,
                normalInertiaFactor = 0.24,
                flickInertiaFactor = 0.78,
            )
            StandaloneTimelineScrubFeel.PRECISE -> StandaloneTimelineScrubTuning(
                normalDragDpPerSecond = 260.0,
                smallDragDpPerSecond = 520.0,
                smallDragVelocityThreshold = 220.0,
                velocityCurve = 1.35,
                flickVelocityThreshold = 900.0,
                normalInertiaFactor = 0.20,
                flickInertiaFactor = 0.70,
            )
            StandaloneTimelineScrubFeel.FRAME -> StandaloneTimelineScrubTuning(
                normalDragDpPerSecond = 420.0,
                smallDragDpPerSecond = 760.0,
                smallDragVelocityThreshold = 180.0,
                velocityCurve = 1.50,
                flickVelocityThreshold = 1_050.0,
                normalInertiaFactor = 0.16,
                flickInertiaFactor = 0.55,
            )
        }
    }
}

internal fun standaloneTimelineEffectiveDpPerSecond(
    tuning: StandaloneTimelineScrubTuning,
    fingerVelocityDpPerSecond: Double,
): Double {
    val fineVelocity = tuning.smallDragVelocityThreshold.coerceAtLeast(1.0)
    val fastVelocity = tuning.flickVelocityThreshold.coerceAtLeast(fineVelocity + 1.0)
    val rawProgress = ((abs(fingerVelocityDpPerSecond) - fineVelocity) / (fastVelocity - fineVelocity))
        .coerceIn(0.0, 1.0)
    val curvedProgress = rawProgress.pow(tuning.velocityCurve.coerceAtLeast(0.05))
    val smoothProgress = curvedProgress * curvedProgress * (3.0 - 2.0 * curvedProgress)
    return tuning.smallDragDpPerSecond +
        (tuning.normalDragDpPerSecond - tuning.smallDragDpPerSecond) * smoothProgress
}

internal fun standaloneTimelineInertiaFactor(
    tuning: StandaloneTimelineScrubTuning,
    fingerVelocityDpPerSecond: Double,
): Double {
    val fineVelocity = tuning.smallDragVelocityThreshold.coerceAtLeast(1.0)
    val fastVelocity = tuning.flickVelocityThreshold.coerceAtLeast(fineVelocity + 1.0)
    val rawProgress = ((abs(fingerVelocityDpPerSecond) - fineVelocity) / (fastVelocity - fineVelocity))
        .coerceIn(0.0, 1.0)
    val curvedProgress = rawProgress.pow(tuning.velocityCurve.coerceAtLeast(0.05))
    val smoothProgress = curvedProgress * curvedProgress * (3.0 - 2.0 * curvedProgress)
    return tuning.normalInertiaFactor +
        (tuning.flickInertiaFactor - tuning.normalInertiaFactor) * smoothProgress
}

internal fun standaloneTimelineTimeAfterDelta(
    currentMs: Long,
    contentDeltaPx: Float,
    effectivePixelsPerSecond: Float,
    durationMs: Long,
): Long {
    if (!contentDeltaPx.isFinite() || effectivePixelsPerSecond <= 0f || durationMs <= 0L) return currentMs
    return (currentMs + contentDeltaPx / effectivePixelsPerSecond * 1_000f)
        .roundToLong()
        .coerceIn(0L, durationMs)
}

@Composable
internal fun ReviewFrameScrubber(
    currentMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = true,
    scrubFeel: StandaloneTimelineScrubFeel = StandaloneTimelineScrubFeel.PRECISE,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showProgress) {
            val progress = (currentMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
            VideoPlayerProgressBar(
                tool = VideoPlayerTool.Frame,
                progress = progress,
                onSeekFraction = { fraction -> onSeek((fraction * safeDuration).roundToLong()) },
                onScrubStart = onScrubStart,
                onScrubEnd = { onScrubEnd() },
                modifier = Modifier.fillMaxWidth().height(22.dp).testTag("standalone-analysis-progress"),
            )
        }
        ReviewFrameTimeline(
            currentMs = currentMs.coerceIn(0L, safeDuration),
            durationMs = safeDuration,
            scrubFeel = scrubFeel,
            onSeek = onSeek,
            onScrubStart = onScrubStart,
            onScrubEnd = onScrubEnd,
            modifier = Modifier.fillMaxWidth().height(44.dp),
        )
    }
}

@Composable
private fun ReviewFrameTimeline(
    currentMs: Long,
    durationMs: Long,
    scrubFeel: StandaloneTimelineScrubFeel,
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (durationMs <= 0L) return
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val decay = rememberSplineBasedDecay<Float>()
    val velocityTracker = remember { VelocityTracker() }
    val tuning = remember(scrubFeel) { StandaloneTimelineScrubTuning.recommended(scrubFeel) }
    val latestCurrentMs = rememberUpdatedState(currentMs)
    val seekTo = rememberUpdatedState(onSeek)
    val scrubStart = rememberUpdatedState(onScrubStart)
    val scrubEnd = rememberUpdatedState(onScrubEnd)
    val gestureActive = remember { booleanArrayOf(false) }
    val visualPixelsPerSecondPx = with(density) { 160.dp.toPx() }
    val minimumTickStepPx = with(density) { 7.dp.toPx() }
    val tickStepPx = (visualPixelsPerSecondPx / 18f).coerceAtLeast(minimumTickStepPx)
    val tickCyclePx = tickStepPx * 15f
    var targetMs by remember(durationMs) { mutableLongStateOf(currentMs.coerceIn(0L, durationMs)) }
    var visualOffsetPx by remember(durationMs) { mutableFloatStateOf(0f) }
    var flingJob by remember { mutableStateOf<Job?>(null) }
    var lastSeekAtMs by remember(durationMs) { mutableLongStateOf(0L) }

    fun consumeContentDelta(deltaPx: Float, fingerVelocityPxPerSecond: Float, forceSeek: Boolean = false) {
        if (!deltaPx.isFinite() || deltaPx == 0f) return
        val beforeMs = targetMs
        val velocityDpPerSecond = if (density.density > 0f) {
            abs(fingerVelocityPxPerSecond) / density.density.toDouble()
        } else {
            0.0
        }
        val effectivePixelsPerSecond = (
            standaloneTimelineEffectiveDpPerSecond(tuning, velocityDpPerSecond) * density.density
        ).toFloat().coerceAtLeast(1f)
        targetMs = standaloneTimelineTimeAfterDelta(
            currentMs = targetMs,
            contentDeltaPx = deltaPx,
            effectivePixelsPerSecond = effectivePixelsPerSecond,
            durationMs = durationMs,
        )
        visualOffsetPx += (targetMs - beforeMs) / 1000f * visualPixelsPerSecondPx
        val now = SystemClock.elapsedRealtime()
        if (forceSeek || now - lastSeekAtMs >= 16L) {
            lastSeekAtMs = now
            seekTo.value(targetMs)
        }
    }

    fun finishGesture() {
        gestureActive[0] = false
        seekTo.value(targetMs)
        scrubEnd.value()
    }

    DisposableEffect(Unit) {
        onDispose { flingJob?.cancel() }
    }

    Canvas(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = .40f))
            .testTag("standalone-analysis-timeline")
            .pointerInput(durationMs, scrubFeel) {
                detectDragGestures(
                    onDragStart = {
                        flingJob?.cancel()
                        flingJob = null
                        gestureActive[0] = true
                        velocityTracker.resetTracking()
                        val startMs = latestCurrentMs.value.coerceIn(0L, durationMs)
                        targetMs = startMs
                        scrubStart.value()
                        seekTo.value(startMs)
                    },
                    onDragEnd = {
                        lastSeekAtMs = SystemClock.elapsedRealtime()
                        val fingerVelocity = velocityTracker.calculateVelocity().x
                        val velocityDp = abs(fingerVelocity) / density.density
                        val inertia = standaloneTimelineInertiaFactor(tuning, velocityDp.toDouble()).toFloat()
                        val contentVelocity = -fingerVelocity * inertia
                        if (abs(contentVelocity) < 80f) {
                            finishGesture()
                        } else {
                            flingJob = scope.launch {
                                var lastValue = 0f
                                AnimationState(initialValue = 0f, initialVelocity = contentVelocity).animateDecay(decay) {
                                    val delta = value - lastValue
                                    lastValue = value
                                    consumeContentDelta(delta, abs(velocity))
                                }
                                finishGesture()
                                flingJob = null
                            }
                        }
                    },
                    onDragCancel = {
                        flingJob?.cancel()
                        flingJob = null
                        finishGesture()
                    },
                    onDrag = { change, dragAmount ->
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val fingerVelocity = abs(velocityTracker.calculateVelocity().x)
                        consumeContentDelta(-dragAmount.x, fingerVelocity)
                        change.consume()
                    },
                )
            },
    ) {
        val shifted = positiveTimelineRemainder(visualOffsetPx, tickCyclePx)
        val shortTick = with(density) { 13.dp.toPx() }
        val thin = with(density) { 1.dp.toPx() }
        val thick = with(density) { 1.3.dp.toPx() }
        val firstIndex = floor((-shifted) / tickStepPx).toInt() - 2
        val lastIndex = ceil((size.width + shifted) / tickStepPx).toInt() + 2
        for (index in firstIndex..lastIndex) {
            val x = index * tickStepPx - shifted
            if (x < -2f || x > size.width + 2f) continue
            val patternIndex = ((index % 15) + 15) % 15
            val tall = patternIndex % 5 == 0
            val heightRatio = when {
                patternIndex == 0 -> 0.74f
                tall -> 0.60f
                else -> shortTick / size.height.coerceAtLeast(1f)
            }
            val tickHeight = size.height * heightRatio
            val distance = abs(x - size.width / 2f) / (size.width / 2f).coerceAtLeast(1f)
            val alpha = (1f - distance * 0.55f).coerceIn(0.45f, 1f)
            val tick = VideoPlayerChrome.tickColor(heightRatio.coerceIn(0f, 1f))
            drawLine(
                color = tick.copy(alpha = tick.alpha * alpha),
                start = Offset(x, (size.height - tickHeight) / 2f),
                end = Offset(x, (size.height + tickHeight) / 2f),
                strokeWidth = if (tall) thick else thin,
            )
        }
    }
}

private fun positiveTimelineRemainder(value: Float, modulus: Float): Float {
    if (!value.isFinite() || !modulus.isFinite() || modulus <= 0f) return 0f
    return ((value % modulus) + modulus) % modulus
}
