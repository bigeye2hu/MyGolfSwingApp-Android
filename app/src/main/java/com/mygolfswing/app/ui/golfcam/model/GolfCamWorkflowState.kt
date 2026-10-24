package com.mygolfswing.app.ui.golfcam.model

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.abs
import kotlin.math.ceil

private const val MIN_VALID_LANDING_FLIGHT_TIME_SEC = 0.5
private const val MAX_VALID_LANDING_FLIGHT_TIME_SEC = 8.0
private const val MIN_VALID_CURVE_CONTROL_X = -0.35f
private const val MAX_VALID_CURVE_CONTROL_X = 1.35f
private const val MIN_VALID_CURVE_CONTROL_Y = -0.8f
private const val MAX_VALID_CURVE_CONTROL_Y = 1.4f
private const val MIN_VALID_STYLE_WIDTH_PCT = 5.0
private const val MAX_VALID_STYLE_WIDTH_PCT = 160.0
private const val MIN_VALID_STYLE_TAIL_HALF_LIFE = 5.0
private const val MAX_VALID_STYLE_TAIL_HALF_LIFE = 100.0

private fun finiteFloatIn(value: Float, min: Float, max: Float, fallback: Float): Float {
    return value.takeIf { it.isFinite() }?.coerceIn(min, max) ?: fallback.coerceIn(min, max)
}

private fun finiteDoubleIn(value: Double, min: Double, max: Double, fallback: Double): Double {
    return value.takeIf { it.isFinite() }?.coerceIn(min, max) ?: fallback.coerceIn(min, max)
}

internal fun clampedImpactPositionSelection(next: ImpactPositionSelection): ImpactPositionSelection {
    return ImpactPositionSelection(
        normalizedX = finiteFloatIn(next.normalizedX, 0f, 1f, fallback = 0.5f),
        normalizedY = finiteFloatIn(next.normalizedY, 0f, 1f, fallback = 0.5f),
    )
}

internal fun clampedLandingOutput(next: LandingOutput): LandingOutput {
    return LandingOutput(
        normalizedX = finiteFloatIn(next.normalizedX, 0f, 1f, fallback = 0.5f),
        normalizedY = finiteFloatIn(next.normalizedY, 0f, 1f, fallback = 0.5f),
        flightTimeSec = finiteDoubleIn(
            value = next.flightTimeSec,
            min = MIN_VALID_LANDING_FLIGHT_TIME_SEC,
            max = MAX_VALID_LANDING_FLIGHT_TIME_SEC,
            fallback = 6.0,
        ),
    )
}

internal fun clampedFlightCurveSelection(
    next: FlightCurveSelection,
    currentImpactPosition: ImpactPositionSelection? = null,
    currentLanding: LandingOutput? = null,
): FlightCurveSelection {
    return FlightCurveSelection(
        impactPosition = clampedImpactPositionSelection(currentImpactPosition ?: next.impactPosition),
        landingPoint = clampedLandingOutput(currentLanding ?: next.landingPoint),
        controlPoint1 = clampedFlightCurveControlPoint(next.controlPoint1),
        controlPoint2 = clampedFlightCurveControlPoint(next.controlPoint2),
        currentFrameIndex = next.currentFrameIndex.coerceAtLeast(0),
    )
}

private fun clampedFlightCurveControlPoint(point: NormalizedPoint): NormalizedPoint {
    return NormalizedPoint(
        x = finiteFloatIn(point.x, MIN_VALID_CURVE_CONTROL_X, MAX_VALID_CURVE_CONTROL_X, fallback = 0.5f),
        y = finiteFloatIn(point.y, MIN_VALID_CURVE_CONTROL_Y, MAX_VALID_CURVE_CONTROL_Y, fallback = 0.5f),
    )
}

internal fun clampedTrackStyleSelection(
    next: TrackStyleSelection,
    currentFlightCurve: FlightCurveSelection? = null,
): TrackStyleSelection {
    return TrackStyleSelection(
        style = clampedTrackStyle(next.style),
        flightCurve = currentFlightCurve?.let(::clampedFlightCurveSelection)
            ?: clampedFlightCurveSelection(next.flightCurve),
    )
}

internal fun clampedTrackStyle(style: TrackStyle): TrackStyle {
    return style.copy(
        colorPreset = clampedTrackColorPreset(style.colorPreset),
        colorAlpha = finiteDoubleIn(style.colorAlpha, 0.0, 1.0, fallback = 0.75),
        glowOpacity = finiteDoubleIn(style.glowOpacity, 0.0, 1.0, fallback = 0.7),
        widthPct = finiteDoubleIn(style.widthPct, MIN_VALID_STYLE_WIDTH_PCT, MAX_VALID_STYLE_WIDTH_PCT, fallback = 40.0),
        tailHalfLife = finiteDoubleIn(
            style.tailHalfLife,
            MIN_VALID_STYLE_TAIL_HALF_LIFE,
            MAX_VALID_STYLE_TAIL_HALF_LIFE,
            fallback = 70.0,
        ),
        totalDistance = GolfCamDistanceConfig.clamp(style.totalDistance),
        speedLevel = GolfCamSpeedConfig.clamp(style.speedLevel),
        activeTrajectoryAlgorithm = TrajectoryAlgorithmKind.GOLF_CAM_LIKE,
        compareTrajectoryAlgorithms = false,
        trajectoryPreset = TrajectoryPresetKind.GOLF_CAM_LIKE,
        motionLaunchBias = TrajectoryMotionParameters.standard.launchBias,
        motionDrag = TrajectoryMotionParameters.standard.drag,
        motionLiftHold = TrajectoryMotionParameters.standard.liftHold,
        motionDescentAcceleration = TrajectoryMotionParameters.standard.descentAcceleration,
        motionTailBoost = TrajectoryMotionParameters.standard.tailBoost,
        motionAnchorTime = null,
        v4VMin = null,
        v4VMax = null,
        v4PLeft = null,
        v4PRight = null,
        v4CustomSPeak = null,
        effectType = TrackEffectType.GRADIENT,
        renderTuning = style.renderTuning.clampedForGolfCamProduction(),
    )
}

private fun clampedTrackColorPreset(preset: TrackColorPreset): TrackColorPreset {
    return preset.copy(
        glowBlur = preset.glowBlur.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f,
        glowAlpha = finiteFloatIn(preset.glowAlpha, 0f, 1f, fallback = 0.7f),
        bandAlpha = finiteFloatIn(preset.bandAlpha, 0f, 1f, fallback = 1f),
    )
}

/**
 * 六步统一工作流共享状态（对齐 iOS GolfCamWorkflowState）。
 * 每步完成后写入对应字段；容器读取字段决定当前可跳转到哪步。
 */
class GolfCamWorkflowState {
    private companion object {
        const val MIN_VALID_TRIM_DURATION_MS = 500L
    }

    var videoUri: Uri? by mutableStateOf(null)
    var videoConfig: GolfCamVideoConfig? by mutableStateOf(null)
    var trimResult: TrimResult? by mutableStateOf(null)
    var impactFrame: ImpactFrameSelection? by mutableStateOf(null)
    var impactPosition: ImpactPositionSelection? by mutableStateOf(null)
    var landing: LandingOutput? by mutableStateOf(null)
    var flightCurve: FlightCurveSelection? by mutableStateOf(null)
    var trackStyle: TrackStyleSelection? by mutableStateOf(null)

    fun clearVideoOnly() {
        videoUri = null
        videoConfig = null
        trimResult = null
        impactFrame = null
        impactPosition = null
        landing = null
        flightCurve = null
        trackStyle = null
    }

    fun clearAll() = clearVideoOnly()

    fun updateVideoConfig(next: GolfCamVideoConfig) {
        if (videoConfig == next) return
        videoConfig = next
        pruneInvalidDownstreamState()
    }

    fun updateTrimResult(next: TrimResult) {
        if (trimResult == next) return
        trimResult = next
        impactFrame = null
        impactPosition = null
        landing = null
        flightCurve = null
        trackStyle = null
    }

    fun updateImpactFrame(next: ImpactFrameSelection) {
        if (impactFrame == next) return
        val previous = impactFrame
        impactFrame = next
        if (previous?.frameIndex == next.frameIndex && previous.timeMs == next.timeMs) return
        impactPosition = null
        landing = null
        flightCurve = null
        trackStyle = null
    }

    fun updateImpactPosition(next: ImpactPositionSelection) {
        val clamped = clampedImpactPositionSelection(next)
        if (impactPosition == clamped) return
        impactPosition = clamped
        landing = null
        flightCurve = null
        trackStyle = null
    }

    fun updateLanding(next: LandingOutput) {
        val clamped = clampedLandingOutput(next)
        if (landing == clamped) return
        landing = clamped
        flightCurve = null
        trackStyle = null
    }

    fun updateFlightCurve(next: FlightCurveSelection) {
        val clamped = clampedFlightCurveSelection(
            next = next,
            currentImpactPosition = impactPosition,
            currentLanding = landing,
        )
        if (flightCurve == clamped) return
        flightCurve = clamped
        trackStyle = null
    }

    fun updateTrackStyle(next: TrackStyleSelection) {
        trackStyle = clampedTrackStyleSelection(
            next = next,
            currentFlightCurve = flightCurve,
        )
    }

    internal fun pruneInvalidDownstreamState() {
        if (!hasCompletedTrim) {
            impactFrame = null
            impactPosition = null
            landing = null
            flightCurve = null
            trackStyle = null
            return
        }
        if (!hasCompletedImpactFrame) {
            impactPosition = null
            landing = null
            flightCurve = null
            trackStyle = null
            return
        }
        if (!hasCompletedImpactPosition) {
            landing = null
            flightCurve = null
            trackStyle = null
            return
        }
        if (!hasCompletedLanding) {
            flightCurve = null
            trackStyle = null
            return
        }
        if (!hasCompletedFlightCurve) {
            trackStyle = null
            return
        }
        if (!hasCompletedTrackStyle) {
            trackStyle = null
        }
    }

    /** 当前已完成到哪步（用于展示进度和限制可跳转范围） */
    val completedStep: GolfCamStep
        get() = when {
            hasCompletedTrackStyle -> GolfCamStep.TRACK_STYLE
            hasCompletedFlightCurve -> GolfCamStep.FLIGHT_CURVE
            hasCompletedLanding -> GolfCamStep.LANDING
            hasCompletedImpactPosition -> GolfCamStep.IMPACT_POSITION
            hasCompletedImpactFrame -> GolfCamStep.IMPACT_FRAME
            hasCompletedTrim -> GolfCamStep.TRIM
            else -> GolfCamStep.TRIM
        }

    /** 断点续编时应进入的步骤：完整流程进样式页，未完成流程进下一待办步骤。 */
    val resumeStep: GolfCamStep
        get() = when {
            !hasCompletedTrim -> GolfCamStep.TRIM
            !hasCompletedImpactFrame -> GolfCamStep.IMPACT_FRAME
            !hasCompletedImpactPosition -> GolfCamStep.IMPACT_POSITION
            !hasCompletedLanding -> GolfCamStep.LANDING
            !hasCompletedFlightCurve -> GolfCamStep.FLIGHT_CURVE
            !hasCompletedTrackStyle -> GolfCamStep.TRACK_STYLE
            else -> GolfCamStep.TRACK_STYLE
        }

    /** 是否已完成全部 6 步可进入预览 */
    val isComplete: Boolean get() = hasCompletedTrackStyle

    val hasCompletedTrim: Boolean
        get() = trimResult?.let { trim ->
            val durationMs = videoConfig?.durationMs
            if (durationMs != null && durationMs < MIN_VALID_TRIM_DURATION_MS) return@let false
            val hasKnownVideoDuration = durationMs != null && durationMs >= MIN_VALID_TRIM_DURATION_MS
            trim.startMs >= 0L &&
                trim.endMs - trim.startMs >= MIN_VALID_TRIM_DURATION_MS &&
                (!hasKnownVideoDuration || (trim.startMs < durationMs!! && trim.endMs <= durationMs))
        } == true
    val hasCompletedImpactFrame: Boolean
        get() {
            val trim = trimResult?.takeIf { hasCompletedTrim } ?: return false
            val impact = impactFrame ?: return false
            if (impact.frameIndex < 0 || impact.timeMs < trim.startMs || impact.timeMs >= trim.endMs) return false
            val fps = videoConfig?.frameRate ?: return true
            if (fps !in 1.0..120.0) return false
            val totalFrames = GolfCamFrameTimeline.totalFramesForRange(trim.startMs, trim.endMs, fps)
            if (impact.frameIndex > (totalFrames - 1).coerceAtLeast(0)) return false
            val expectedTimeMs = GolfCamFrameTimeline.timeMsForFrame(trim.startMs, impact.frameIndex, fps)
                .coerceIn(trim.startMs, GolfCamFrameTimeline.lastFrameTimeMsForRange(trim.startMs, trim.endMs, fps))
            val frameToleranceMs = ceil(1000.0 / GolfCamFrameTimeline.safeFrameRate(fps)).toLong().coerceAtLeast(1L)
            return abs(impact.timeMs - expectedTimeMs) <= frameToleranceMs
        }
    val hasCompletedImpactPosition: Boolean
        get() = hasCompletedImpactFrame && impactPosition?.let {
            isNormalizedCoordinate(it.normalizedX) && isNormalizedCoordinate(it.normalizedY)
        } == true
    val hasCompletedLanding: Boolean
        get() = hasCompletedImpactPosition && landing?.let {
            isNormalizedCoordinate(it.normalizedX) &&
                isNormalizedCoordinate(it.normalizedY) &&
                it.flightTimeSec in MIN_VALID_LANDING_FLIGHT_TIME_SEC..MAX_VALID_LANDING_FLIGHT_TIME_SEC
        } == true
    val hasCompletedFlightCurve: Boolean
        get() = hasCompletedLanding &&
            flightCurve?.impactPosition == impactPosition &&
            flightCurve?.landingPoint == landing &&
            flightCurve?.controlPoint1?.let(::isValidCurveControlPoint) == true &&
            flightCurve?.controlPoint2?.let(::isValidCurveControlPoint) == true
    val hasCompletedTrackStyle: Boolean
        get() = hasCompletedFlightCurve &&
            trackStyle?.flightCurve == flightCurve &&
            trackStyle?.style?.let(::isValidTrackStyle) == true

    /** 飞行曲线控制点 1（归一化坐标，用于 FlightCurveStepTool 显示） */
    val flightCurveControlPoint1: Pair<Float, Float>?
        get() = flightCurve?.controlPoint1?.let { it.x to it.y }

    /** 飞行曲线控制点 2（归一化坐标，用于 FlightCurveStepTool 显示） */
    val flightCurveControlPoint2: Pair<Float, Float>?
        get() = flightCurve?.controlPoint2?.let { it.x to it.y }

    private fun isNormalizedCoordinate(value: Float): Boolean = value in 0f..1f

    private fun isValidCurveControlPoint(point: NormalizedPoint): Boolean {
        return point.x in MIN_VALID_CURVE_CONTROL_X..MAX_VALID_CURVE_CONTROL_X &&
            point.y in MIN_VALID_CURVE_CONTROL_Y..MAX_VALID_CURVE_CONTROL_Y
    }

    private fun isValidTrackStyle(style: TrackStyle): Boolean {
        return style == clampedTrackStyle(style)
    }
}

enum class GolfCamStep(
    val index: Int,
    val title: String,
    val shortTitle: String,
    val icon: ImageVector,
) {
    TRIM(0, "裁剪视频", "裁剪", Icons.Filled.ContentCut),
    IMPACT_FRAME(1, "击球帧", "击球帧", Icons.Filled.VideoFile),
    IMPACT_POSITION(2, "击球位置", "击球点", Icons.Filled.GpsFixed),
    LANDING(3, "落点", "落点", Icons.Filled.Flag),
    FLIGHT_CURVE(4, "曲线", "曲线", Icons.AutoMirrored.Filled.ShowChart),
    TRACK_STYLE(5, "轨迹样式", "样式", Icons.Filled.Palette),
    ;

    val isFirst: Boolean get() = index == 0
    val isLast: Boolean get() = index == TRACK_STYLE.index
    val next: GolfCamStep? get() = values().getOrNull(index + 1)
    val prev: GolfCamStep? get() = values().getOrNull(index - 1)
}
