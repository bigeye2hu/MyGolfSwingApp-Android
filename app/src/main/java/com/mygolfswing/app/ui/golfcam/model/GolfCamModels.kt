package com.mygolfswing.app.ui.golfcam.model

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

// ─── 几何基础 ──────────────────────────────────────────────────────────────────

private fun finiteNormalizedCoordinate(value: Float, fallback: Float): Float {
    return value.takeIf { it.isFinite() } ?: fallback
}

data class NormalizedPoint(val x: Float, val y: Float) {
    fun toOffset(width: Float, height: Float) = Offset(
        finiteNormalizedCoordinate(x, 0.5f) * finiteNormalizedCoordinate(width, 1f),
        finiteNormalizedCoordinate(y, 0.5f) * finiteNormalizedCoordinate(height, 1f),
    )
}

/** 三次贝塞尔路径（归一化坐标 0~1）*/
data class BezierPath(
    val p0: NormalizedPoint,
    val p1: NormalizedPoint,
    val p2: NormalizedPoint,
    val p3: NormalizedPoint,
) {
    fun point(t: Float): NormalizedPoint {
        val safeT = finiteNormalizedCoordinate(t, 0f).coerceIn(0f, 1f)
        val safeP0 = p0.finiteOrCenter()
        val safeP1 = p1.finiteOrCenter()
        val safeP2 = p2.finiteOrCenter()
        val safeP3 = p3.finiteOrCenter()
        val mt = 1f - safeT
        return NormalizedPoint(
            x = mt * mt * mt * safeP0.x + 3 * mt * mt * safeT * safeP1.x + 3 * mt * safeT * safeT * safeP2.x + safeT * safeT * safeT * safeP3.x,
            y = mt * mt * mt * safeP0.y + 3 * mt * mt * safeT * safeP1.y + 3 * mt * safeT * safeT * safeP2.y + safeT * safeT * safeT * safeP3.y,
        )
    }

    fun pointAtArcProgress(progress: Float, arcTable: ArcLengthTable): NormalizedPoint {
        val safeProgress = finiteNormalizedCoordinate(progress, 0f).coerceIn(0f, 1f)
        if (!arcTable.totalLength.isFinite() || arcTable.totalLength <= 0f) return point(safeProgress)
        val u = arcTable.uAtArcLength(arcTable.totalLength * safeProgress)
        return point(u)
    }

    private fun NormalizedPoint.finiteOrCenter(): NormalizedPoint {
        return NormalizedPoint(
            x = finiteNormalizedCoordinate(x, 0.5f),
            y = finiteNormalizedCoordinate(y, 0.5f),
        )
    }
}

// ─── 轨迹算法 V5（对齐 iOS TrajectoryMotionModel）──────────────────────────────

enum class TrajectoryPresetKind(val displayName: String) {
    STANDARD("标准弹道"),
    HIGH_ARC("高弹道"),
    LOW_PIERCING("低穿透"),
    SOFT_LANDING("柔和落地"),
    GOLF_CAM_LIKE("GC手感"),
}

data class TrajectoryMotionParameters(
    val launchBias: Double = 0.52,
    val apexProgress: Double? = null,
    val drag: Double = 0.35,
    val liftHold: Double = 0.42,
    val descentAcceleration: Double = 0.55,
    val tailBoost: Double = 0.0,
    val speedK: Double = 0.45,
    val vMin: Double = 1.0,
    val vMax: Double = 15.0,
    val pLeft: Double = 1.7,
    val pRight: Double = 2.3,
    val anchorTime: Double? = null,
    val preset: TrajectoryPresetKind = TrajectoryPresetKind.STANDARD,
) {
    companion object {
        val standard = TrajectoryMotionParameters()

        fun preset(kind: TrajectoryPresetKind): TrajectoryMotionParameters {
            return when (kind) {
                TrajectoryPresetKind.STANDARD -> standard
                TrajectoryPresetKind.HIGH_ARC -> standard.copy(
                    launchBias = 0.62, drag = 0.42, liftHold = 0.72,
                    descentAcceleration = 0.42, tailBoost = 0.12, speedK = 0.38,
                    vMin = standard.vMin * 0.88, pLeft = standard.pLeft * 1.12,
                    pRight = standard.pRight * 0.88, preset = kind,
                )
                TrajectoryPresetKind.LOW_PIERCING -> standard.copy(
                    launchBias = 0.38, drag = 0.22, liftHold = 0.18,
                    descentAcceleration = 0.78, tailBoost = 0.16, speedK = 0.58,
                    vMin = standard.vMin * 1.12, pLeft = standard.pLeft * 0.92,
                    pRight = standard.pRight * 1.18, preset = kind,
                )
                TrajectoryPresetKind.SOFT_LANDING -> standard.copy(
                    launchBias = 0.58, drag = 0.50, liftHold = 0.64,
                    descentAcceleration = 0.28, tailBoost = -0.08, speedK = 0.34,
                    vMin = standard.vMin * 0.82, pLeft = standard.pLeft * 1.05,
                    pRight = standard.pRight * 0.78, preset = kind,
                )
                TrajectoryPresetKind.GOLF_CAM_LIKE -> standard.copy(
                    launchBias = 0.68, drag = 0.44, liftHold = 0.76,
                    descentAcceleration = 0.50, tailBoost = 0.04, speedK = 0.45,
                    vMin = standard.vMin * 0.72, pLeft = standard.pLeft * 1.25,
                    pRight = standard.pRight * 0.82, preset = kind,
                )
            }
        }
    }
}

enum class TrajectoryAlgorithmKind(val displayName: String, val subtitle: String) {
    CURRENT_V5("V4旧算法", "U型尾段偏强"),
    GOLF_CAM_LIKE("V5新版", "立即出线+轻尾段"),
}

data class TrajectoryTiming(
    val impactTimeMs: Long,
    val landingTimeMs: Long,
    val fps: Double,
) {
    val totalDurationMs: Long get() = maxOf(landingTimeMs - impactTimeMs, 1L)
    val totalDurationSec: Double get() = totalDurationMs / 1000.0
    val defaultAnchorTimeSec: Double get() = minOf(0.5, totalDurationSec * 0.18)
}

// ─── 轨迹算法选择（对齐 iOS TrajectoryAlgorithmManager）────────────────────────

enum class KRegion {
    LOW,
    MEDIUM,
    HIGH,
    EXTREME;

    companion object {
        fun from(k: Double): KRegion = when {
            k < 0.35 -> LOW
            k < 0.50 -> MEDIUM
            k < 0.60 -> HIGH
            else -> EXTREME
        }
    }
}

enum class SPeakRegion {
    LOW,
    STANDARD,
    HIGH;

    companion object {
        fun from(sPeak: Double): SPeakRegion = when {
            sPeak < 0.55 -> LOW
            sPeak < 0.70 -> STANDARD
            else -> HIGH
        }
    }
}

enum class TrajectoryAlgorithmType {
    V44_STANDARD,
    V44_LOW_K_PATCH,
    V44_HIGH_K_COMPRESS,
    V45_HIGH_ARC,
    V45_LOW_ARC,
    V44_EXTREME;

    val parameterAdjustments: AlgorithmParameters
        get() = when (this) {
            V44_STANDARD -> AlgorithmParameters(
                kAsymFactor = 3.0,
                pLeftMultiplier = 1.0,
                pRightMultiplier = 1.0,
                vMinFactor = 1.0,
                tailSpeedBoost = 0.0,
            )
            V44_LOW_K_PATCH -> AlgorithmParameters(
                kAsymFactor = 3.0,
                pLeftMultiplier = 0.9,
                pRightMultiplier = 1.1,
                vMinFactor = 1.0,
                tailSpeedBoost = 0.1,
            )
            V44_HIGH_K_COMPRESS -> AlgorithmParameters(
                kAsymFactor = 2.5,
                pLeftMultiplier = 1.1,
                pRightMultiplier = 0.9,
                vMinFactor = 0.9,
                tailSpeedBoost = 0.0,
            )
            V45_HIGH_ARC -> AlgorithmParameters(
                kAsymFactor = 3.5,
                pLeftMultiplier = 1.2,
                pRightMultiplier = 0.8,
                vMinFactor = 0.85,
                tailSpeedBoost = 0.15,
            )
            V45_LOW_ARC -> AlgorithmParameters(
                kAsymFactor = 2.5,
                pLeftMultiplier = 0.85,
                pRightMultiplier = 1.15,
                vMinFactor = 1.1,
                tailSpeedBoost = -0.05,
            )
            V44_EXTREME -> AlgorithmParameters(
                kAsymFactor = 2.0,
                pLeftMultiplier = 1.0,
                pRightMultiplier = 1.0,
                vMinFactor = 1.2,
                tailSpeedBoost = 0.0,
            )
        }
}

data class AlgorithmParameters(
    val kAsymFactor: Double,
    val pLeftMultiplier: Double,
    val pRightMultiplier: Double,
    val vMinFactor: Double,
    val tailSpeedBoost: Double,
)

data class AlgorithmSelection(
    val kRegion: KRegion,
    val sPeakRegion: SPeakRegion,
    val algorithmType: TrajectoryAlgorithmType,
    val parameters: AlgorithmParameters,
)

object TrajectoryAlgorithmManager {
    private val matrix: Map<KRegion, Map<SPeakRegion, TrajectoryAlgorithmType>> = mapOf(
        KRegion.LOW to mapOf(
            SPeakRegion.LOW to TrajectoryAlgorithmType.V45_LOW_ARC,
            SPeakRegion.STANDARD to TrajectoryAlgorithmType.V44_LOW_K_PATCH,
            SPeakRegion.HIGH to TrajectoryAlgorithmType.V45_HIGH_ARC,
        ),
        KRegion.MEDIUM to mapOf(
            SPeakRegion.LOW to TrajectoryAlgorithmType.V45_LOW_ARC,
            SPeakRegion.STANDARD to TrajectoryAlgorithmType.V44_STANDARD,
            SPeakRegion.HIGH to TrajectoryAlgorithmType.V45_HIGH_ARC,
        ),
        KRegion.HIGH to mapOf(
            SPeakRegion.LOW to TrajectoryAlgorithmType.V44_HIGH_K_COMPRESS,
            SPeakRegion.STANDARD to TrajectoryAlgorithmType.V44_HIGH_K_COMPRESS,
            SPeakRegion.HIGH to TrajectoryAlgorithmType.V44_HIGH_K_COMPRESS,
        ),
        KRegion.EXTREME to mapOf(
            SPeakRegion.LOW to TrajectoryAlgorithmType.V44_EXTREME,
            SPeakRegion.STANDARD to TrajectoryAlgorithmType.V44_EXTREME,
            SPeakRegion.HIGH to TrajectoryAlgorithmType.V44_EXTREME,
        ),
    )

    fun selectAlgorithm(k: Double, sPeak: Double): AlgorithmSelection {
        val kRegion = KRegion.from(k)
        val sPeakRegion = SPeakRegion.from(sPeak)
        val algorithmType = matrix[kRegion]?.get(sPeakRegion) ?: TrajectoryAlgorithmType.V44_STANDARD
        return AlgorithmSelection(
            kRegion = kRegion,
            sPeakRegion = sPeakRegion,
            algorithmType = algorithmType,
            parameters = algorithmType.parameterAdjustments,
        )
    }
}

// ─── 弧长表（均匀弧长采样，O(1) 反查）──────────────────────────────────────────

class ArcLengthTable(val bezier: BezierPath, sampleCount: Int = 300) {
    private val safeSampleCount = sampleCount.coerceAtLeast(1)
    private val uSamples = FloatArray(safeSampleCount + 1) { it.toFloat() / safeSampleCount }
    val arcLengths = FloatArray(safeSampleCount + 1)

    val totalLength: Float

    init {
        var prev = bezier.point(0f)
        for (i in 1..safeSampleCount) {
            val curr = bezier.point(uSamples[i])
            val dx = curr.x - prev.x
            val dy = curr.y - prev.y
            arcLengths[i] = arcLengths[i - 1] + kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            prev = curr
        }
        totalLength = arcLengths[safeSampleCount].takeIf { it.isFinite() } ?: 0f
    }

    fun uAtArcLength(s: Float): Float {
        val safeTotal = totalLength.takeIf { it.isFinite() && it > 0f } ?: return 0f
        val target = finiteNormalizedCoordinate(s, 0f).coerceIn(0f, safeTotal)
        var lo = 0; var hi = arcLengths.size - 1
        while (lo < hi - 1) {
            val mid = (lo + hi) / 2
            if (arcLengths[mid] < target) lo = mid else hi = mid
        }
        val ratio = if (arcLengths[hi] == arcLengths[lo]) 0f
        else (target - arcLengths[lo]) / (arcLengths[hi] - arcLengths[lo])
        return uSamples[lo] + ratio * (uSamples[hi] - uSamples[lo])
    }

    fun arcLengthAtU(u: Float): Float {
        val target = finiteNormalizedCoordinate(u, 0f).coerceIn(0f, 1f)
        var lo = 0; var hi = uSamples.size - 1
        while (lo < hi - 1) {
            val mid = (lo + hi) / 2
            if (uSamples[mid] < target) lo = mid else hi = mid
        }
        val ratio = if (uSamples[hi] == uSamples[lo]) 0f
        else (target - uSamples[lo]) / (uSamples[hi] - uSamples[lo])
        return arcLengths[lo] + ratio * (arcLengths[hi] - arcLengths[lo])
    }

    fun apexU(): Float {
        var minY = Float.MAX_VALUE; var minU = 0f
        for (i in uSamples.indices) {
            val pt = bezier.point(uSamples[i])
            if (pt.y < minY) { minY = pt.y; minU = uSamples[i] }
        }
        return minU
    }

    fun apexProgress(): Float {
        if (!totalLength.isFinite() || totalLength <= 0f) return 0.6f
        return (arcLengthAtU(apexU()) / totalLength)
            .takeIf { it.isFinite() }
            ?.coerceIn(0.05f, 0.95f)
            ?: 0.6f
    }
}

// ─── V5 运动模型（对齐 iOS TrajectoryMotionModel）──────────────────────────────

class TrajectoryMotionModel(
    val bezier: BezierPath,
    val timing: TrajectoryTiming,
    val params: TrajectoryMotionParameters,
    sampleCount: Int = 300,
) {
    val arcTable = ArcLengthTable(bezier, sampleCount)

    private val resolvedApex: Double
    private val sSamples: DoubleArray
    private val tSamples: DoubleArray
    private val selection: AlgorithmSelection

    init {
        val apexU = arcTable.apexU()
        val apexArc = arcTable.arcLengthAtU(apexU)
        val realApex = if (arcTable.totalLength > 0) apexArc / arcTable.totalLength else 0.6f
        resolvedApex = (params.apexProgress ?: realApex.toDouble()).coerceIn(0.05, 0.95)

        selection = TrajectoryAlgorithmManager.selectAlgorithm(
            k = params.speedK,
            sPeak = resolvedApex,
        )
        val adjusted = adjustedParameters(params, selection)
        val physicalK = mapUiKToPhysicalK(params.speedK, selection.algorithmType)
        val mode = detectMode(physicalK, resolvedApex)
        val slowPoint = if (mode == "OVERSHOOT") physicalK else resolvedApex
        val anchor = params.anchorTime ?: timing.defaultAnchorTimeSec
        val totalDur = timing.totalDurationSec

        val asymmetryA = solveA(
            targetK = physicalK,
            anchorTime = anchor,
            totalDuration = totalDur,
            slowPoint = slowPoint,
            vMin = adjusted.vMin,
            vMax = adjusted.vMax,
            pLeft = adjusted.pLeft,
            pRight = adjusted.pRight,
            mode = mode,
            selection = selection,
            visualParams = params,
        )

        val (ss, ts) = buildTimeMapping(
            slowPoint = slowPoint,
            vMin = adjusted.vMin,
            vMax = adjusted.vMax,
            pLeft = adjusted.pLeft,
            pRight = adjusted.pRight,
            asymmetryA = asymmetryA,
            mode = mode,
            selection = selection,
            visualParams = params,
            sampleCount = sampleCount,
        )
        sSamples = ss
        tSamples = ts
    }

    fun progressAtTime(elapsedSec: Double): Double {
        val tNorm = (elapsedSec / timing.totalDurationSec).coerceIn(0.0, 1.0)
        return sForTime(tNorm, sSamples, tSamples).coerceIn(0.0, 1.0)
    }

    fun positionAtProgress(s: Double): NormalizedPoint {
        val arcLen = (s * arcTable.totalLength).toFloat()
        val u = arcTable.uAtArcLength(arcLen)
        return bezier.point(u)
    }

    fun positionAtTime(elapsedSec: Double) = positionAtProgress(progressAtTime(elapsedSec))

    companion object {
        private data class AdjustedParams(
            val vMin: Double,
            val vMax: Double,
            val pLeft: Double,
            val pRight: Double,
        )

        fun parametersFromStyle(style: TrackStyle, apexProgress: Double? = null): TrajectoryMotionParameters {
            return TrajectoryMotionParameters.preset(style.trajectoryPreset).copy(
                speedK = style.speedLevel,
                vMin = style.v4VMin ?: TrajectoryMotionParameters.standard.vMin,
                vMax = style.v4VMax ?: TrajectoryMotionParameters.standard.vMax,
                pLeft = style.v4PLeft ?: TrajectoryMotionParameters.standard.pLeft,
                pRight = style.v4PRight ?: TrajectoryMotionParameters.standard.pRight,
                apexProgress = style.v4CustomSPeak ?: apexProgress,
                launchBias = style.motionLaunchBias,
                drag = style.motionDrag,
                liftHold = style.motionLiftHold,
                descentAcceleration = style.motionDescentAcceleration,
                tailBoost = style.motionTailBoost,
                anchorTime = style.motionAnchorTime,
                preset = style.trajectoryPreset,
            )
        }

        private fun adjustedParameters(
            params: TrajectoryMotionParameters,
            selection: AlgorithmSelection,
        ): AdjustedParams {
            val algorithm = selection.parameters
            return AdjustedParams(
                vMin = max(0.1, params.vMin * algorithm.vMinFactor),
                vMax = max(0.2, params.vMax),
                pLeft = max(0.1, params.pLeft * algorithm.pLeftMultiplier),
                pRight = max(0.1, params.pRight * algorithm.pRightMultiplier),
            )
        }

        private fun mapUiKToPhysicalK(kUi: Double, algorithmType: TrajectoryAlgorithmType): Double {
            val kUiLowMin = 0.15
            val kUiLowMax = 0.35
            var kPhysLowMin = 0.31
            var kPhysLowMax = 0.35

            val kUiHighMin = 0.45
            val kUiHighMax = 0.60
            var kPhysHighMin = 0.43
            var kPhysHighMax = 0.44

            when (algorithmType) {
                TrajectoryAlgorithmType.V44_LOW_K_PATCH -> {
                    kPhysLowMin = 0.28
                    kPhysLowMax = 0.36
                }
                TrajectoryAlgorithmType.V44_HIGH_K_COMPRESS -> {
                    kPhysHighMin = 0.42
                    kPhysHighMax = 0.435
                }
                TrajectoryAlgorithmType.V44_EXTREME -> {
                    kPhysHighMin = 0.41
                    kPhysHighMax = 0.43
                }
                else -> Unit
            }

            if (kUi < kUiLowMax) {
                val t = (kUi - kUiLowMin) / (kUiLowMax - kUiLowMin)
                return kPhysLowMin + max(0.0, t).pow(1.8) * (kPhysLowMax - kPhysLowMin)
            }
            if (kUi < kUiHighMin) return kUi
            if (kUi <= kUiHighMax) {
                val t = (kUi - kUiHighMin) / (kUiHighMax - kUiHighMin)
                return kPhysHighMin + t * (kPhysHighMax - kPhysHighMin)
            }
            return kUi
        }

        private fun detectMode(k: Double, sPeak: Double): String {
            val eps = 0.015
            return when {
                k < sPeak - eps -> "NORMAL"
                k > sPeak + eps -> "OVERSHOOT"
                else -> "CRITICAL"
            }
        }

        private fun solveA(
            targetK: Double,
            anchorTime: Double,
            totalDuration: Double,
            slowPoint: Double,
            vMin: Double,
            vMax: Double,
            pLeft: Double,
            pRight: Double,
            mode: String,
            selection: AlgorithmSelection,
            visualParams: TrajectoryMotionParameters,
        ): Double {
            var lo = -1.5
            var hi = 1.5
            val tNorm = anchorTime / max(totalDuration, 1e-6)

            repeat(40) {
                val mid = 0.5 * (lo + hi)
                val mapping = buildTimeMapping(
                    slowPoint = slowPoint,
                    vMin = vMin,
                    vMax = vMax,
                    pLeft = pLeft,
                    pRight = pRight,
                    asymmetryA = mid,
                    mode = mode,
                    selection = selection,
                    visualParams = visualParams,
                    sampleCount = 300,
                )

                val sMatch = sForTime(tNorm, mapping.first, mapping.second)
                if (abs(sMatch - targetK) < 1e-6) return mid
                if (sMatch < targetK) lo = mid else hi = mid
            }

            return 0.5 * (lo + hi)
        }

        private fun buildTimeMapping(
            slowPoint: Double,
            vMin: Double,
            vMax: Double,
            pLeft: Double,
            pRight: Double,
            asymmetryA: Double,
            mode: String,
            selection: AlgorithmSelection,
            visualParams: TrajectoryMotionParameters,
            sampleCount: Int,
        ): Pair<DoubleArray, DoubleArray> {
            val sampleSize = max(40, sampleCount)
            val ds = 1.0 / sampleSize
            val sSamples = DoubleArray(sampleSize + 1) { it.toDouble() / sampleSize }
            val vSamples = DoubleArray(sampleSize + 1) {
                speed(
                    s = sSamples[it],
                    slowPoint = slowPoint,
                    vMin = vMin,
                    vMax = vMax,
                    pLeft = pLeft,
                    pRight = pRight,
                    asymmetryA = asymmetryA,
                    selection = selection,
                    visualParams = visualParams,
                )
            }

            enforceUSpeedShape(vSamples, slowPoint, mode)
            applyVisualTail(
                vSamples = vSamples,
                slowPoint = slowPoint,
                tailBoost = selection.parameters.tailSpeedBoost + visualParams.tailBoost,
            )

            val tSamples = DoubleArray(sampleSize + 1)
            for (k in 1..sampleSize) {
                val vMid = 0.5 * (vSamples[k] + vSamples[k - 1])
                tSamples[k] = tSamples[k - 1] + ds / max(vMid, 1e-6)
            }
            val totalT = tSamples[sampleSize]
            if (totalT > 0) {
                for (k in 0..sampleSize) tSamples[k] /= totalT
            }

            return sSamples to tSamples
        }

        private fun speed(
            s: Double,
            slowPoint: Double,
            vMin: Double,
            vMax: Double,
            pLeft: Double,
            pRight: Double,
            asymmetryA: Double,
            selection: AlgorithmSelection,
            visualParams: TrajectoryMotionParameters,
        ): Double {
            val a = asymmetryA.coerceIn(-1.5, 1.5)
            val launchBoost = 1.0 + (visualParams.launchBias - 0.5).coerceIn(-0.5, 0.5) * 0.8
            val dragDamp = 1.0 - visualParams.drag.coerceIn(0.0, 1.0) * 0.18
            val holdDamp = 1.0 - visualParams.liftHold.coerceIn(0.0, 1.0) * 0.22
            val descentBoost = 1.0 + visualParams.descentAcceleration.coerceIn(0.0, 1.0) * 0.28

            val vStart = vMax * launchBoost
            val vEnd = (vMin + (vMax - vMin) * 0.6) * dragDamp * descentBoost
            val base = baseSpeed(
                s = s,
                slowPoint = slowPoint,
                vMin = vMin * holdDamp,
                vStart = vStart,
                vEnd = vEnd,
                pLeft = pLeft,
                pRight = pRight,
            )

            return if (a >= 0) {
                speedPositiveA(s, slowPoint, vMin, base, a, selection.parameters.kAsymFactor)
            } else {
                speedNegativeA(s, slowPoint, vMin, base, a, selection.parameters.kAsymFactor)
            }
        }

        private fun baseSpeed(
            s: Double,
            slowPoint: Double,
            vMin: Double,
            vStart: Double,
            vEnd: Double,
            pLeft: Double,
            pRight: Double,
        ): Double {
            return if (s <= slowPoint) {
                val t = (slowPoint - s) / max(slowPoint, 1e-6)
                vMin + (vStart - vMin) * t.pow(pLeft)
            } else {
                val t = (s - slowPoint) / max(1.0 - slowPoint, 1e-6)
                vMin + (vEnd - vMin) * t.pow(pRight)
            }
        }

        private fun speedPositiveA(
            s: Double,
            slowPoint: Double,
            vMin: Double,
            base: Double,
            asymmetryA: Double,
            kAsym: Double,
        ): Double {
            val dist: Double
            val sign: Double
            if (s <= slowPoint) {
                dist = (slowPoint - s) / max(slowPoint, 1e-6)
                sign = 1.0
            } else {
                dist = (s - slowPoint) / max(1.0 - slowPoint, 1e-6)
                sign = -1.0
            }

            var delta = kAsym * dist * dist * sign * base
            if (s > slowPoint) delta = max(delta, -0.6 * base)

            var v = base + asymmetryA * delta
            if (s > slowPoint) v = max(vMin * 0.9, v)
            return max(0.1, v)
        }

        private fun speedNegativeA(
            s: Double,
            slowPoint: Double,
            vMin: Double,
            base: Double,
            asymmetryA: Double,
            kAsym: Double,
        ): Double {
            val a = min(1.0, max(0.0, -asymmetryA))
            val x = if (s <= slowPoint) {
                s / max(slowPoint, 1e-6)
            } else {
                (s - slowPoint) / max(1.0 - slowPoint, 1e-6)
            }
            val delta = if (s <= slowPoint) {
                -a * kAsym * x * (1.0 - x) * (base - vMin)
            } else {
                a * kAsym * x.pow(2.0) * base
            }
            return max(0.1, base + delta)
        }

        private fun enforceUSpeedShape(vSamples: DoubleArray, slowPoint: Double, mode: String) {
            if (vSamples.size <= 2) return
            val last = vSamples.size - 1
            val slowIdx = (slowPoint * last).toInt().coerceIn(0, last)
            if (slowIdx >= 1) {
                for (k in 1..slowIdx) {
                    if (vSamples[k] > vSamples[k - 1]) vSamples[k] = vSamples[k - 1]
                }
            }
            if (slowIdx + 1 <= last) {
                for (k in (slowIdx + 1)..last) {
                    if (vSamples[k] < vSamples[k - 1]) vSamples[k] = vSamples[k - 1]
                }
            }

            if (mode == "NORMAL") {
                val vEnd = vSamples[last]
                val vApex = vSamples[slowIdx]
                if (vSamples[0] < vEnd * 0.8) {
                    val scale = (vEnd * 0.8) / max(vSamples[0], 1e-6)
                    for (k in 0..slowIdx) vSamples[k] *= scale
                }
                if (vSamples[0] < vApex * 2.0) {
                    val scale = (vApex * 2.0) / max(vSamples[0], 1e-6)
                    for (k in 0..slowIdx) vSamples[k] *= scale
                }
            }
        }

        private fun applyVisualTail(vSamples: DoubleArray, slowPoint: Double, tailBoost: Double) {
            if (tailBoost == 0.0 || vSamples.size <= 2) return
            val last = vSamples.size - 1
            val slowIdx = (slowPoint * last).toInt().coerceIn(0, last)
            if (slowIdx + 1 > last) return
            for (k in (slowIdx + 1)..last) {
                val progress = (k - slowIdx).toDouble() / max(1, last - slowIdx)
                vSamples[k] *= max(0.2, 1.0 + tailBoost * progress)
            }
        }

        fun sForTime(t: Double, sSamples: DoubleArray, tSamples: DoubleArray): Double {
            val clamped = t.coerceIn(0.0, 1.0)
            var i = 0
            while (i < tSamples.size && tSamples[i] < clamped) i++
            if (i <= 0) return sSamples.first()
            if (i >= tSamples.size) return sSamples.last()
            val ratio = (clamped - tSamples[i - 1]) / max(tSamples[i] - tSamples[i - 1], 1e-6)
            return sSamples[i - 1] + (sSamples[i] - sSamples[i - 1]) * ratio
        }
    }
}

// ─── GolfCam 速度配置（对齐 iOS GolfCamSpeedConfig）──────────────────────────────

object GolfCamSpeedConfig {
    const val MIN_LEVEL = 0.15
    const val MAX_LEVEL = 0.75
    const val DEFAULT_LEVEL = 0.45
    const val STEP = 0.05

    fun clamp(v: Double) = v.takeIf { it.isFinite() }?.coerceIn(MIN_LEVEL, MAX_LEVEL) ?: DEFAULT_LEVEL
    fun normalized(v: Double) = (clamp(v) - MIN_LEVEL) / (MAX_LEVEL - MIN_LEVEL)
    fun percentText(v: Double) = "${(clamp(v) * 100).toInt()}%"

    internal fun golfCamV8StartSpeedLimit(activeDurationSec: Double, speedLevel: Double): Double {
        val activeDuration = max(finiteDouble(activeDurationSec, fallback = 0.0), 0.0)
        val speedNorm = normalized(speedLevel)
        return (V8DepthDensity.START_SPEED_LIMIT_BASE + V8DepthDensity.START_SPEED_LIMIT_DURATION_GAIN * activeDuration) *
            (V8DepthDensity.START_SPEED_LIMIT_SPEED_BASE_WEIGHT + V8DepthDensity.START_SPEED_LIMIT_SPEED_GAIN * speedNorm)
    }

    private fun finiteDouble(value: Double, fallback: Double): Double {
        return value.takeIf { it.isFinite() } ?: fallback
    }

    private fun calibratedGamma(v: Double): Double {
        val x = normalized(v)
        val low = 1.01; val mid = 0.383; val high = 0.150
        val a = 2.0 * (high + low - 2.0 * mid)
        val b = high - low - a
        return max(0.05, a * x * x + b * x + low)
    }

    fun progress(timeProgress: Double, speedLevel: Double): Double {
        val tau = finiteDouble(timeProgress, fallback = 0.0).coerceIn(0.0, 1.0)
        if (tau <= 0.0) return 0.0
        if (tau >= 1.0) return 1.0
        return tau.pow(calibratedGamma(speedLevel)).coerceIn(0.0, 1.0)
    }

    fun trajectoryHeadProgress(
        elapsedSec: Double,
        totalDurationSec: Double,
        speedLevel: Double,
        bezier: BezierPath,
        arcTable: ArcLengthTable,
        style: TrackStyle,
    ): Double {
        val totalDuration = max(finiteDouble(totalDurationSec, fallback = 1e-6), 1e-6)
        val elapsed = finiteDouble(elapsedSec, fallback = 0.0).coerceIn(0.0, totalDuration)
        if (elapsed <= 0.0) return 0.0
        if (elapsed >= totalDuration) return 1.0

        val delayTime = golfCamV8StartDelay(totalDuration)
        if (elapsed <= delayTime) return 0.0
        val activeDuration = max(totalDuration - delayTime, 1e-6)
        val tau = ((elapsed - delayTime) / activeDuration).coerceIn(0.0, 1.0)
        val mapping = golfCamV8HeadMapping(
            totalDuration = totalDuration,
            activeDuration = activeDuration,
            speedLevel = speedLevel,
            bezier = bezier,
            arcTable = arcTable,
            style = style,
        )
        return sForTime(tau, mapping.sSamples, mapping.tSamples).coerceIn(0.0, 1.0)
    }

    fun previewAnchorProgress(v: Double): Double {
        val k = clamp(v)
        if (k < 0.35) {
            val t = (k - 0.15) / (0.35 - 0.15)
            return 0.14 + max(0.0, t).pow(1.55) * (0.30 - 0.14)
        }
        if (k < 0.45) {
            val t = (k - 0.35) / (0.45 - 0.35)
            return 0.30 + t * (0.44 - 0.30)
        }
        if (k <= 0.60) {
            val t = (k - 0.45) / (0.60 - 0.45)
            return 0.44 + t * (0.62 - 0.44)
        }
        val t = (k - 0.60) / (0.75 - 0.60)
        return 0.62 + max(0.0, t) * (0.78 - 0.62)
    }

    fun displayProgress(
        elapsedSec: Double,
        totalDurationSec: Double,
        speedLevel: Double,
        frameRate: Double = 30.0,
        updateIntervalFrames: Int = 2,
    ): Double {
        val dur = max(finiteDouble(totalDurationSec, fallback = 1e-6), 1e-6)
        val elapsed = max(0.0, finiteDouble(elapsedSec, fallback = 0.0))
        if (elapsed >= dur) return 1.0
        val fps = finiteDouble(frameRate, fallback = 30.0).takeIf { it > 1.0 } ?: 30.0
        val frameIndex = (elapsed * fps).toInt()
        val sampledFrame = frameIndex - (frameIndex % max(updateIntervalFrames, 1))
        val sampledTime = sampledFrame / fps
        return progress(sampledTime / dur, speedLevel)
    }

    fun displayDistance(
        totalDistance: Double,
        elapsedSec: Double,
        totalDurationSec: Double,
        speedLevel: Double,
        frameRate: Double = 30.0,
    ): Int {
        val p = displayProgress(elapsedSec, totalDurationSec, speedLevel, frameRate = frameRate)
        val safeDistance = GolfCamDistanceConfig.clamp(totalDistance)
        val final = safeDistance.roundToInt()
        if (p >= 1.0) return max(0, final)
        return max(0, min(final, (safeDistance * p).toInt()))
    }

    fun trajectoryDisplayDistance(
        totalDistance: Double,
        elapsedSec: Double,
        totalDurationSec: Double,
        speedLevel: Double,
        bezier: BezierPath,
        arcTable: ArcLengthTable,
        style: TrackStyle,
        frameRate: Double = 30.0,
        updateIntervalFrames: Int = 2,
    ): Int {
        val dur = max(finiteDouble(totalDurationSec, fallback = 1e-6), 1e-6)
        val elapsed = max(0.0, finiteDouble(elapsedSec, fallback = 0.0))
        val safeDistance = GolfCamDistanceConfig.clamp(totalDistance)
        val final = safeDistance.roundToInt()
        if (elapsed >= dur) return max(0, final)
        val fps = finiteDouble(frameRate, fallback = 30.0).takeIf { it > 1.0 } ?: 30.0
        val frameIndex = (elapsed * fps).toInt()
        val sampledFrame = frameIndex - (frameIndex % max(updateIntervalFrames, 1))
        val sampledTime = sampledFrame / fps
        val p = trajectoryHeadProgress(
            elapsedSec = sampledTime,
            totalDurationSec = dur,
            speedLevel = speedLevel,
            bezier = bezier,
            arcTable = arcTable,
            style = style,
        )
        if (p >= 1.0) return max(0, final)
        return max(0, min(final, (safeDistance * p).toInt()))
    }

    fun visibleTailStartProgress(
        headProgress: Double,
        tailHalfLife: Double,
        activeAlgorithm: TrajectoryAlgorithmKind,
        apexProgress: Double,
    ): Double {
        val head = finiteDouble(headProgress, fallback = 0.0).coerceIn(0.0, 1.0)
        val rawTailPercent = (finiteDouble(tailHalfLife, fallback = 70.0) / 100.0).coerceIn(0.0, 1.0)
        if (rawTailPercent >= 0.995) return 0.0

        if (activeAlgorithm != TrajectoryAlgorithmKind.GOLF_CAM_LIKE) {
            val tailLength = min(head, rawTailPercent)
            return max(0.0, head - tailLength)
        }

        val apex = finiteDouble(apexProgress, fallback = 0.5).coerceIn(0.05, 0.95)
        val visualTailPercent = rawTailPercent.coerceIn(0.16, 0.42)
        val requestedTailStart = max(0.0, head - rawTailPercent)

        if (head <= apex) {
            val movingTail = max(0.0, head - visualTailPercent)
            val warmupLength = max(0.22, min(0.36, apex * 0.42))
            val warmup = (head / warmupLength).coerceIn(0.0, 1.0)
            val smoothWarmup = warmup * warmup * (3.0 - 2.0 * warmup)
            val golfCamTailStart = movingTail * smoothWarmup
            return min(golfCamTailStart, requestedTailStart)
        }

        val preApexStart = max(0.0, apex - visualTailPercent)
        val descent = ((head - apex) / max(1.0 - apex, 0.001)).coerceIn(0.0, 1.0)
        val transition = (descent / 1.12).coerceIn(0.0, 1.0)
        val smoothTransition = transition * transition * (3.0 - 2.0 * transition)
        val landingStart = max(0.0, apex - visualTailPercent * 0.42)
        val golfCamTailStart = preApexStart + (landingStart - preApexStart) * smoothTransition
        return min(golfCamTailStart, requestedTailStart)
    }

    private data class GolfCamV8HeadMapping(
        val sSamples: DoubleArray,
        val tSamples: DoubleArray,
    )

    private data class GolfCamV8HeadCacheKey(
        val totalDurationMs: Long,
        val speedLevelScaled: Int,
        val apexProgressScaled: Int,
        val anchorTimeMs: Long,
        val liftHoldScaled: Int,
        val arcHeightRatioScaled: Int,
        val landingSteepnessScaled: Int,
        val descentArcShareScaled: Int,
        val progressDensityStrengthScaled: Int,
        val tailReleaseStrengthScaled: Int,
        val tailReleaseStartScaled: Int,
        val startSpeedLimitScaled: Int,
        val motionAnchorTimeMs: Long?,
    )

    private var golfCamV8HeadCache: Pair<GolfCamV8HeadCacheKey, GolfCamV8HeadMapping>? = null

    private object V5Rhythm {
        const val HOLD_WIDTH_BASE = 0.2192
        const val HOLD_WIDTH_LIFT_GAIN = 0.08
        const val HOLD_STRENGTH_BASE = 0.723
        const val HOLD_STRENGTH_LIFT_GAIN = 0.20
        const val HOLD_STRENGTH_MAX = 0.92
        const val APEX_MIN_SPEED_BASE = 0.62
        const val APEX_MIN_SPEED_LIFT_REDUCTION = 0.24
        const val APEX_MIN_SPEED_FLOOR = 0.30
        const val END_SPEED_BASE = 2.15
        const val END_SPEED_SPEED_GAIN = 0.70
        const val END_SPEED_HIGH_ARC_GAIN = 0.30
        const val HIGH_ARC_APEX_REFERENCE = 0.62
        const val HIGH_ARC_APEX_RANGE = 0.24
        const val PRE_APEX_POWER_BASE = 1.55
        const val PRE_APEX_POWER_SLOW_GAIN = 0.35
        const val DESCENT_POWER_BASE = 2.35
        const val DESCENT_POWER_SPEED_GAIN = 0.65
        const val DESCENT_POWER_LOW_ARC_GAIN = 0.35
    }

    private object V6Geometry {
        const val HEIGHT_REFERENCE = 0.36
        const val STEEPNESS_REFERENCE = 0.48
        const val HOLD_WIDTH_HEIGHT_GAIN = 0.055
        const val HOLD_STRENGTH_HEIGHT_GAIN = 0.085
        const val END_SPEED_LANDING_GAIN = 0.0
        const val END_SPEED_HEIGHT_DAMPING = 2.0
        const val END_SPEED_DURATION_GAIN = 0.0
        const val DESCENT_POWER_LANDING_GAIN = 0.0
        const val DESCENT_POWER_HEIGHT_DAMPING = 2.0
        const val MIN_END_SPEED = 0.80
        const val MAX_END_SPEED = 1.30
        const val MIN_DESCENT_POWER = 1.00
        const val MAX_DESCENT_POWER = 1.65
    }

    private object V7ApexBound {
        const val APEX_CLAMP_MAX = 0.995
        const val FAST_ANCHOR_CAP_BASE = 0.89
        const val FAST_ANCHOR_ORDINARY_APEX_BOOST = 0.055
        const val FAST_ANCHOR_LATE_APEX_BOOST = 0.21
        const val FAST_ANCHOR_TINY_DESCENT_DAMPING = 0.06
        const val HIGH_SPEED_BOOST_THRESHOLD = 0.55
        const val POST_APEX_HOLD_BASE = 0.06
        const val POST_APEX_HOLD_LIFT_GAIN = 0.08
        const val RELEASE_POWER_BASE = 1.45
        const val RELEASE_POWER_LIFT_GAIN = 0.28
        const val RELEASE_POWER_SPEED_DAMPING = 0.18
        const val TAIL_EASE_BASE = 0.10
        const val TAIL_EASE_LIFT_GAIN = 0.18
        const val SPEED_VALLEY_DRIFT_RATIO = 0.86
    }

    private object V8DepthDensity {
        const val DENSITY_STRENGTH_BASE = 0.18
        const val DENSITY_STRENGTH_LIFT_GAIN = 0.08
        const val DENSITY_STRENGTH_HEIGHT_GAIN = 0.08
        const val DENSITY_STRENGTH_SPEED_DAMPING = 0.04
        const val DENSITY_SATURATION_START = 0.22
        const val DENSITY_SATURATION_END = 0.94
        const val TAIL_RELEASE_STRENGTH_BASE = 0.03
        const val TAIL_RELEASE_STRENGTH_SPEED_GAIN = 0.08
        const val TAIL_RELEASE_STRENGTH_DESCENT_GAIN = 0.03
        const val TAIL_RELEASE_START = 0.86
        const val END_SPEED_MID_BOOST = 0.05
        const val DESCENT_POWER_DAMPING = 0.20
        const val START_SPEED_LIMIT_BASE = 8.0
        const val START_SPEED_LIMIT_DURATION_GAIN = 6.0
        const val START_SPEED_LIMIT_SPEED_BASE_WEIGHT = 0.55
        const val START_SPEED_LIMIT_SPEED_GAIN = 0.45
    }

    private data class GolfCamV8GeometryMetrics(
        val arcHeightRatio: Double,
        val landingSteepness: Double,
        val descentArcShare: Double,
    )

    private fun golfCamV8StartDelay(totalDuration: Double): Double {
        return 0.0
    }

    private fun golfCamV8AnchorTime(totalDuration: Double, delayTime: Double, style: TrackStyle): Double {
        val measuredAnchor = 15.0 / 30.0
        val requested = style.motionAnchorTime ?: measuredAnchor
        val minAnchor = min(totalDuration, delayTime + 1.0 / 30.0)
        val fallback = max(minAnchor, totalDuration * 0.35)
        return requested.coerceAtMost(totalDuration).coerceIn(minAnchor, min(measuredAnchor, fallback))
    }

    private fun golfCamV8HeadMapping(
        totalDuration: Double,
        activeDuration: Double,
        speedLevel: Double,
        bezier: BezierPath,
        arcTable: ArcLengthTable,
        style: TrackStyle,
    ): GolfCamV8HeadMapping {
        val delayTime = golfCamV8StartDelay(totalDuration)
        val anchorTime = golfCamV8AnchorTime(totalDuration, delayTime, style)
        val anchorTau = ((anchorTime - delayTime) / activeDuration).coerceIn(0.02, 0.92)
        val apex = (style.v4CustomSPeak ?: arcTable.apexProgress().toDouble()).coerceIn(0.05, V7ApexBound.APEX_CLAMP_MAX)
        val speedK = clamp(speedLevel)
        val rawTarget = progress(
            timeProgress = max(0.0, anchorTime - delayTime) / activeDuration,
            speedLevel = speedK,
        )
        val speedNorm = normalized(speedK)
        val target = golfCamV8AnchorTarget(rawTarget, apex, speedNorm)
        val fastCrossApexWeight = smoothstep(0.005, 0.055, target - apex)
        val liftHold = finiteDouble(style.motionLiftHold, fallback = TrajectoryMotionParameters.preset(TrajectoryPresetKind.GOLF_CAM_LIKE).liftHold)
            .coerceIn(0.0, 1.0)
        val minSpeed = golfCamV8ApexMinSpeed(liftHold)
        val slowWeight = 1.0 - speedNorm
        val highArcWeight = golfCamV8HighArcWeight(apex)
        var holdWidth = golfCamV8ApexHoldWidth(liftHold)
        var holdStrength = golfCamV8ApexHoldStrength(liftHold)
        var endSpeed = golfCamV8EndSpeed(speedNorm, highArcWeight)
        val pLeft = V5Rhythm.PRE_APEX_POWER_BASE + V5Rhythm.PRE_APEX_POWER_SLOW_GAIN * slowWeight
        var pRight = V5Rhythm.DESCENT_POWER_BASE +
            V5Rhythm.DESCENT_POWER_SPEED_GAIN * speedNorm +
            V5Rhythm.DESCENT_POWER_LOW_ARC_GAIN * (1.0 - highArcWeight)

        val metrics = golfCamV8GeometryMetrics(bezier, arcTable)
        val heightNorm = (metrics.arcHeightRatio / max(V6Geometry.HEIGHT_REFERENCE, 1e-6)).coerceIn(0.0, 1.8)
        val descentShare = metrics.descentArcShare.coerceIn(0.08, 0.82)
        val (adjustedHoldWidth, adjustedHoldStrength, adjustedEndSpeed, adjustedDescentPower) =
            golfCamV8ApplyGeometryAdjustments(activeDuration, holdWidth, holdStrength, endSpeed, pRight, metrics)
        holdWidth = adjustedHoldWidth
        holdStrength = adjustedHoldStrength
        endSpeed = adjustedEndSpeed
        pRight = adjustedDescentPower

        var postApexHoldShare = (
            V7ApexBound.POST_APEX_HOLD_BASE +
                V7ApexBound.POST_APEX_HOLD_LIFT_GAIN * liftHold +
                0.045 * (heightNorm - 0.7).coerceIn(0.0, 0.9) +
                0.03 * (descentShare - 0.36).coerceIn(-0.2, 0.22)
            ).coerceIn(0.06, 0.20)
        var releasePower = (
            V7ApexBound.RELEASE_POWER_BASE +
                V7ApexBound.RELEASE_POWER_LIFT_GAIN * liftHold +
                0.24 * (heightNorm - 0.85).coerceIn(-0.35, 0.55) -
                V7ApexBound.RELEASE_POWER_SPEED_DAMPING * speedNorm
            ).coerceIn(1.20, 1.95)
        val tailEaseDamping = (
            V7ApexBound.TAIL_EASE_BASE +
                V7ApexBound.TAIL_EASE_LIFT_GAIN * liftHold +
                0.12 * (heightNorm - 0.75).coerceIn(0.0, 0.75)
            ).coerceIn(0.08, 0.34)
        val speedValleyProgress = (
            apex + max(0.0, target - apex) * fastCrossApexWeight * V7ApexBound.SPEED_VALLEY_DRIFT_RATIO
            ).coerceIn(apex, 0.995)

        holdWidth *= 1.0 - 0.72 * fastCrossApexWeight
        holdStrength *= 1.0 - 0.78 * fastCrossApexWeight
        postApexHoldShare *= 1.0 - 0.95 * fastCrossApexWeight
        releasePower *= 1.0 - 0.34 * fastCrossApexWeight

        val activeTailEaseDamping = tailEaseDamping * (1.0 - 0.85 * fastCrossApexWeight)
        val crossEndSpeedBoost = 0.85 * fastCrossApexWeight
        endSpeed = (
            endSpeed * (1.0 - activeTailEaseDamping) +
                crossEndSpeedBoost +
                V8DepthDensity.END_SPEED_MID_BOOST * (0.35 + 0.65 * speedNorm)
            ).coerceIn(
            V6Geometry.MIN_END_SPEED,
            if (fastCrossApexWeight > 0.0) 2.20 else min(V6Geometry.MAX_END_SPEED + 0.18, 1.36),
        )
        pRight = (
            pRight + 0.18 + 0.22 * liftHold - 0.60 * fastCrossApexWeight -
                V8DepthDensity.DESCENT_POWER_DAMPING * (0.35 + 0.65 * speedNorm)
            ).coerceIn(1.05, 1.95)
        holdWidth = (holdWidth + postApexHoldShare * descentShare * 0.2).coerceIn(0.06, 0.45)
        holdStrength = (holdStrength + 0.04 * liftHold).coerceIn(0.0, 0.95)

        val progressDensityStrength = (
            V8DepthDensity.DENSITY_STRENGTH_BASE +
                V8DepthDensity.DENSITY_STRENGTH_LIFT_GAIN * liftHold +
                V8DepthDensity.DENSITY_STRENGTH_HEIGHT_GAIN * (heightNorm - 0.75).coerceIn(0.0, 0.85)
            ).coerceIn(0.0, 0.36) * (1.0 - V8DepthDensity.DENSITY_STRENGTH_SPEED_DAMPING * speedNorm)
        val tailReleaseStrength = (
            V8DepthDensity.TAIL_RELEASE_STRENGTH_BASE +
                V8DepthDensity.TAIL_RELEASE_STRENGTH_SPEED_GAIN * speedNorm +
                V8DepthDensity.TAIL_RELEASE_STRENGTH_DESCENT_GAIN * (descentShare - 0.22).coerceIn(0.0, 0.50)
            ).coerceIn(0.0, 0.42)
        val tailReleaseStart = (V8DepthDensity.TAIL_RELEASE_START - 0.03 * fastCrossApexWeight).coerceIn(0.76, 0.92)
        val startSpeedLimit = golfCamV8StartSpeedLimit(activeDuration, speedK)

        val key = GolfCamV8HeadCacheKey(
            totalDurationMs = (totalDuration * 1000.0).roundToLong(),
            speedLevelScaled = (speedK * 10_000.0).roundToInt(),
            apexProgressScaled = (apex * 10_000.0).roundToInt(),
            anchorTimeMs = (anchorTime * 1000.0).roundToLong(),
            liftHoldScaled = (liftHold * 10_000.0).roundToInt(),
            arcHeightRatioScaled = (metrics.arcHeightRatio * 10_000.0).roundToInt(),
            landingSteepnessScaled = (metrics.landingSteepness * 10_000.0).roundToInt(),
            descentArcShareScaled = (metrics.descentArcShare * 10_000.0).roundToInt(),
            progressDensityStrengthScaled = (progressDensityStrength * 10_000.0).roundToInt(),
            tailReleaseStrengthScaled = (tailReleaseStrength * 10_000.0).roundToInt(),
            tailReleaseStartScaled = (tailReleaseStart * 10_000.0).roundToInt(),
            startSpeedLimitScaled = (startSpeedLimit * 10_000.0).roundToInt(),
            motionAnchorTimeMs = style.motionAnchorTime?.let { (it * 1000.0).roundToLong() },
        )
        golfCamV8HeadCache?.let { (cachedKey, cachedMapping) ->
            if (cachedKey == key) return cachedMapping
        }

        val startSpeed = golfCamV8SolveStartSpeed(
            target = target,
            anchorTau = anchorTau,
            apex = apex,
            minSpeed = minSpeed,
            endSpeed = endSpeed,
            pLeft = pLeft,
            pRight = pRight,
            holdWidth = holdWidth,
            holdStrength = holdStrength,
            speedValleyProgress = speedValleyProgress,
            postApexHoldShare = postApexHoldShare,
            releasePower = releasePower,
            progressDensityStrength = progressDensityStrength,
            progressDensityPower = V8DepthDensity.DENSITY_SATURATION_END,
            tailReleaseStrength = tailReleaseStrength,
            tailReleaseStart = tailReleaseStart,
            startSpeedLimit = startSpeedLimit,
        )
        val mapping = golfCamV8BuildMapping(
            apex = speedValleyProgress,
            startSpeed = startSpeed,
            minSpeed = minSpeed,
            endSpeed = endSpeed,
            pLeft = pLeft,
            pRight = pRight,
            holdWidth = holdWidth,
            holdStrength = holdStrength,
            speedValleyProgress = speedValleyProgress,
            postApexHoldShare = postApexHoldShare,
            releasePower = releasePower,
            progressDensityStrength = progressDensityStrength,
            progressDensityPower = V8DepthDensity.DENSITY_SATURATION_END,
            tailReleaseStrength = tailReleaseStrength,
            tailReleaseStart = tailReleaseStart,
            sampleCount = 360,
        )
        golfCamV8HeadCache = key to mapping
        return mapping
    }

    private fun golfCamV8AnchorTarget(rawAnchorTarget: Double, apex: Double, speedNorm: Double): Double {
        val descentShare = (1.0 - apex).coerceIn(0.0, 1.0)
        val ordinaryApexWeight = smoothstep(0.52, 0.70, apex) * (1.0 - smoothstep(0.78, 0.94, apex))
        val lateApexWeight = smoothstep(0.72, 0.94, apex)
        val tinyDescentWeight = ((0.10 - descentShare) / 0.10).coerceIn(0.0, 1.0)
        val ceiling = (
            rawAnchorTarget +
                V7ApexBound.FAST_ANCHOR_ORDINARY_APEX_BOOST * ordinaryApexWeight +
                V7ApexBound.FAST_ANCHOR_LATE_APEX_BOOST * lateApexWeight -
                V7ApexBound.FAST_ANCHOR_TINY_DESCENT_DAMPING * tinyDescentWeight
            ).coerceIn(0.65, V7ApexBound.FAST_ANCHOR_CAP_BASE)
        val boostWeight = smoothstep(V7ApexBound.HIGH_SPEED_BOOST_THRESHOLD, 1.0, speedNorm)
        val boosted = rawAnchorTarget + (ceiling - rawAnchorTarget) * boostWeight
        return boosted.coerceIn(0.02, V7ApexBound.FAST_ANCHOR_CAP_BASE)
    }

    private fun golfCamV8ApexHoldWidth(liftHold: Double): Double {
        return V5Rhythm.HOLD_WIDTH_BASE + V5Rhythm.HOLD_WIDTH_LIFT_GAIN * liftHold
    }

    private fun golfCamV8ApexHoldStrength(liftHold: Double): Double {
        return (V5Rhythm.HOLD_STRENGTH_BASE + V5Rhythm.HOLD_STRENGTH_LIFT_GAIN * liftHold)
            .coerceIn(0.0, V5Rhythm.HOLD_STRENGTH_MAX)
    }

    private fun golfCamV8ApexMinSpeed(liftHold: Double): Double {
        return (V5Rhythm.APEX_MIN_SPEED_BASE - V5Rhythm.APEX_MIN_SPEED_LIFT_REDUCTION * liftHold)
            .coerceIn(V5Rhythm.APEX_MIN_SPEED_FLOOR, V5Rhythm.APEX_MIN_SPEED_BASE)
    }

    private fun golfCamV8EndSpeed(speedNorm: Double, highArcWeight: Double): Double {
        return V5Rhythm.END_SPEED_BASE +
            V5Rhythm.END_SPEED_SPEED_GAIN * speedNorm +
            V5Rhythm.END_SPEED_HIGH_ARC_GAIN * highArcWeight
    }

    private fun golfCamV8HighArcWeight(apex: Double): Double {
        return ((V5Rhythm.HIGH_ARC_APEX_REFERENCE - apex) / V5Rhythm.HIGH_ARC_APEX_RANGE).coerceIn(0.0, 1.0)
    }

    private fun golfCamV8GeometryMetrics(bezier: BezierPath, arcTable: ArcLengthTable): GolfCamV8GeometryMetrics {
        val p0 = bezier.point(0f)
        val p3 = bezier.point(1f)
        val apexPathFraction = arcTable.apexProgress().toDouble().coerceIn(0.0, 1.0)
        val apex = bezier.pointAtArcProgress(apexPathFraction.toFloat(), arcTable)
        val chordDx = (p3.x - p0.x).toDouble()
        val chordDy = (p3.y - p0.y).toDouble()
        val chordLength = max(sqrt(chordDx * chordDx + chordDy * chordDy), 1e-6)
        val apexDx = (apex.x - p0.x).toDouble()
        val apexDy = (apex.y - p0.y).toDouble()
        val signedDistance = (apexDx * chordDy - apexDy * chordDx) / chordLength
        val arcHeightRatio = (abs(signedDistance) / chordLength).coerceIn(0.0, 2.0)
        val tangentDx = 3.0 * (bezier.p3.x - bezier.p2.x).toDouble()
        val tangentDy = 3.0 * (bezier.p3.y - bezier.p2.y).toDouble()
        val tangentLength = max(sqrt(tangentDx * tangentDx + tangentDy * tangentDy), 1e-6)
        return GolfCamV8GeometryMetrics(
            arcHeightRatio = arcHeightRatio,
            landingSteepness = (abs(tangentDy) / tangentLength).coerceIn(0.0, 1.0),
            descentArcShare = (1.0 - apexPathFraction).coerceIn(0.0, 1.0),
        )
    }

    private data class GolfCamV8GeometryAdjustment(
        val holdWidth: Double,
        val holdStrength: Double,
        val endSpeed: Double,
        val descentPower: Double,
    )

    private fun golfCamV8ApplyGeometryAdjustments(
        duration: Double,
        holdWidth: Double,
        holdStrength: Double,
        endSpeed: Double,
        descentPower: Double,
        metrics: GolfCamV8GeometryMetrics,
    ): GolfCamV8GeometryAdjustment {
        val heightNorm = (metrics.arcHeightRatio / max(V6Geometry.HEIGHT_REFERENCE, 1e-6)).coerceIn(0.0, 1.8)
        val steepDelta = metrics.landingSteepness - V6Geometry.STEEPNESS_REFERENCE
        val heightDelta = heightNorm - 1.0
        val durationPressure = ((6.0 - duration) / 2.0).coerceIn(-0.75, 0.75)
        val holdWidthDelta = V6Geometry.HOLD_WIDTH_HEIGHT_GAIN * (heightNorm - 0.75).coerceIn(-0.45, 0.75)
        val holdStrengthDelta = V6Geometry.HOLD_STRENGTH_HEIGHT_GAIN * (heightNorm - 0.85).coerceIn(-0.55, 0.65)
        val endSpeedDelta = V6Geometry.END_SPEED_LANDING_GAIN * steepDelta -
            V6Geometry.END_SPEED_HEIGHT_DAMPING * heightDelta +
            V6Geometry.END_SPEED_DURATION_GAIN * durationPressure
        val descentPowerDelta = V6Geometry.DESCENT_POWER_LANDING_GAIN * steepDelta -
            V6Geometry.DESCENT_POWER_HEIGHT_DAMPING * heightDelta
        return GolfCamV8GeometryAdjustment(
            holdWidth = (holdWidth + holdWidthDelta).coerceIn(0.12, 0.42),
            holdStrength = (holdStrength + holdStrengthDelta).coerceIn(0.0, min(0.95, V5Rhythm.HOLD_STRENGTH_MAX + 0.03)),
            endSpeed = (endSpeed + endSpeedDelta).coerceIn(V6Geometry.MIN_END_SPEED, V6Geometry.MAX_END_SPEED),
            descentPower = (descentPower + descentPowerDelta).coerceIn(V6Geometry.MIN_DESCENT_POWER, V6Geometry.MAX_DESCENT_POWER),
        )
    }

    private fun golfCamV8SolveStartSpeed(
        target: Double,
        anchorTau: Double,
        apex: Double,
        minSpeed: Double,
        endSpeed: Double,
        pLeft: Double,
        pRight: Double,
        holdWidth: Double,
        holdStrength: Double,
        speedValleyProgress: Double,
        postApexHoldShare: Double,
        releasePower: Double,
        progressDensityStrength: Double,
        progressDensityPower: Double,
        tailReleaseStrength: Double,
        tailReleaseStart: Double,
        startSpeedLimit: Double,
    ): Double {
        var low = minSpeed * 1.001
        val limit = if (startSpeedLimit > 0.0) max(startSpeedLimit, low) else Double.POSITIVE_INFINITY
        var high = min(2048.0, limit)
        var expansionAttempts = 0
        while (expansionAttempts < 16) {
            val mapping = golfCamV8BuildMapping(
                apex = apex,
                startSpeed = high,
                minSpeed = minSpeed,
                endSpeed = endSpeed,
                pLeft = pLeft,
                pRight = pRight,
                holdWidth = holdWidth,
                holdStrength = holdStrength,
                speedValleyProgress = speedValleyProgress,
                postApexHoldShare = postApexHoldShare,
                releasePower = releasePower,
                progressDensityStrength = progressDensityStrength,
                progressDensityPower = progressDensityPower,
                tailReleaseStrength = tailReleaseStrength,
                tailReleaseStart = tailReleaseStart,
                sampleCount = 360,
            )
            if (sForTime(anchorTau, mapping.sSamples, mapping.tSamples) >= target || high >= limit) {
                break
            }
            high = min(high * 2.0, limit)
            expansionAttempts += 1
        }
        repeat(48) {
            val mid = 0.5 * (low + high)
            val mapping = golfCamV8BuildMapping(
                apex = apex,
                startSpeed = mid,
                minSpeed = minSpeed,
                endSpeed = endSpeed,
                pLeft = pLeft,
                pRight = pRight,
                holdWidth = holdWidth,
                holdStrength = holdStrength,
                speedValleyProgress = speedValleyProgress,
                postApexHoldShare = postApexHoldShare,
                releasePower = releasePower,
                progressDensityStrength = progressDensityStrength,
                progressDensityPower = progressDensityPower,
                tailReleaseStrength = tailReleaseStrength,
                tailReleaseStart = tailReleaseStart,
                sampleCount = 360,
            )
            val progress = sForTime(anchorTau, mapping.sSamples, mapping.tSamples)
            if (progress < target) low = mid else high = mid
        }
        return 0.5 * (low + high)
    }

    private fun golfCamV8BuildMapping(
        apex: Double,
        startSpeed: Double,
        minSpeed: Double,
        endSpeed: Double,
        pLeft: Double,
        pRight: Double,
        holdWidth: Double,
        holdStrength: Double,
        speedValleyProgress: Double,
        postApexHoldShare: Double,
        releasePower: Double,
        progressDensityStrength: Double,
        progressDensityPower: Double,
        tailReleaseStrength: Double,
        tailReleaseStart: Double,
        sampleCount: Int,
    ): GolfCamV8HeadMapping {
        val count = max(120, sampleCount)
        val ds = 1.0 / count.toDouble()
        val sSamples = DoubleArray(count + 1) { it.toDouble() / count.toDouble() }
        val speedValley = speedValleyProgress.coerceIn(0.02, 0.995)
        val vSamples = DoubleArray(count + 1) { index ->
            golfCamV8Speed(
                progress = sSamples[index],
                speedValley = speedValley,
                startSpeed = startSpeed,
                minSpeed = minSpeed,
                endSpeed = endSpeed,
                pLeft = pLeft,
                pRight = pRight,
                postApexHoldShare = postApexHoldShare,
                releasePower = releasePower,
            )
        }
        golfCamV8ApplyProgressDepthDensity(vSamples, progressDensityStrength, progressDensityPower, tailReleaseStrength, tailReleaseStart)
        golfCamV8ApplyApexHold(vSamples, apex = speedValley, minSpeed = minSpeed, holdWidth = holdWidth, holdStrength = holdStrength)
        golfCamV8EnforceApexMonotonic(vSamples, apex = speedValley)

        val tSamples = DoubleArray(count + 1)
        for (index in 1..count) {
            val vMid = 0.5 * (vSamples[index] + vSamples[index - 1])
            tSamples[index] = tSamples[index - 1] + ds / max(vMid, 1e-6)
        }
        val total = tSamples[count]
        if (total > 0.0) {
            for (index in 0..count) tSamples[index] /= total
        }
        return GolfCamV8HeadMapping(sSamples, tSamples)
    }

    private fun golfCamV8Speed(
        progress: Double,
        speedValley: Double,
        startSpeed: Double,
        minSpeed: Double,
        endSpeed: Double,
        pLeft: Double,
        pRight: Double,
        postApexHoldShare: Double,
        releasePower: Double,
    ): Double {
        val s = progress.coerceIn(0.0, 1.0)
        if (s <= speedValley) {
            val x = (speedValley - s) / max(speedValley, 1e-6)
            return minSpeed + (startSpeed - minSpeed) * x.pow(pLeft)
        }
        var x = (s - speedValley) / max(1.0 - speedValley, 1e-6)
        val holdShare = postApexHoldShare.coerceIn(0.0, 0.35)
        val release = releasePower.coerceIn(0.8, 2.4)
        if (holdShare > 0.0) {
            x = if (x <= holdShare) 0.0 else ((x - holdShare) / max(1.0 - holdShare, 1e-6)).pow(release)
        }
        return minSpeed + (endSpeed - minSpeed) * x.pow(pRight)
    }

    private fun golfCamV8ApplyProgressDepthDensity(
        vSamples: DoubleArray,
        densityStrength: Double,
        densityPower: Double,
        tailReleaseStrength: Double,
        tailReleaseStart: Double,
    ) {
        if (vSamples.size <= 2 || densityStrength <= 0.0) return
        val last = vSamples.size - 1
        val strength = densityStrength.coerceIn(0.0, 0.60)
        val saturationStart = V8DepthDensity.DENSITY_SATURATION_START
        val saturationEnd = densityPower.coerceIn(saturationStart + 0.05, 0.995)
        val releaseStrength = tailReleaseStrength.coerceIn(0.0, 0.60)
        val releaseStart = tailReleaseStart.coerceIn(0.55, 0.92)
        for (index in 0..last) {
            val s = index.toDouble() / last.toDouble()
            val depthWeight = smoothstep(saturationStart, saturationEnd, s)
            val density = 1.0 + strength * depthWeight
            val release = smoothstep(releaseStart, 1.0, s)
            val releaseBoost = 1.0 + releaseStrength * release
            vSamples[index] = vSamples[index] / density * releaseBoost
        }
    }

    private fun golfCamV8ApplyApexHold(
        vSamples: DoubleArray,
        apex: Double,
        minSpeed: Double,
        holdWidth: Double,
        holdStrength: Double,
    ) {
        if (vSamples.size <= 2 || holdWidth <= 1e-6 || holdStrength <= 0.0) return
        val last = vSamples.size - 1
        val width = max(holdWidth, 1e-6)
        val strength = holdStrength.coerceIn(0.0, 0.95)
        for (index in 0..last) {
            val s = index.toDouble() / last.toDouble()
            val d = abs(s - apex) / width
            if (d >= 1.0) continue
            val smooth = d * d * (3.0 - 2.0 * d)
            val weight = (1.0 - smooth) * strength
            vSamples[index] = vSamples[index] * (1.0 - weight) + minSpeed * weight
        }
    }

    private fun golfCamV8EnforceApexMonotonic(vSamples: DoubleArray, apex: Double) {
        if (vSamples.size <= 2) return
        val last = vSamples.size - 1
        val apexIndex = (apex * last.toDouble()).roundToInt().coerceIn(0, last)
        if (apexIndex >= 1) {
            for (index in 1..apexIndex) {
                if (vSamples[index] > vSamples[index - 1]) vSamples[index] = vSamples[index - 1]
            }
        }
        if (apexIndex + 1 <= last) {
            for (index in (apexIndex + 1)..last) {
                if (vSamples[index] < vSamples[index - 1]) vSamples[index] = vSamples[index - 1]
            }
        }
    }

    private fun sForTime(t: Double, sSamples: DoubleArray, tSamples: DoubleArray): Double {
        val clamped = t.coerceIn(0.0, 1.0)
        var i = 0
        while (i < tSamples.size && tSamples[i] < clamped) i++
        if (i <= 0) return sSamples.first()
        if (i >= tSamples.size) return sSamples.last()
        val ratio = (clamped - tSamples[i - 1]) / max(tSamples[i] - tSamples[i - 1], 1e-6)
        return sSamples[i - 1] + (sSamples[i] - sSamples[i - 1]) * ratio
    }

    private fun smoothstep(edge0: Double, edge1: Double, value: Double): Double {
        if (edge0 == edge1) return if (value < edge0) 0.0 else 1.0
        val x = ((value - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
        return x * x * (3.0 - 2.0 * x)
    }
}

// ─── 轨迹样式（对齐 iOS TrackStyle）──────────────────────────────────────────────

object GolfCamDistanceConfig {
    const val MIN_DISTANCE = 10.0
    const val MAX_DISTANCE = 1000.0
    const val DEFAULT_DISTANCE = 150.0

    fun clamp(distance: Double): Double {
        return distance.takeIf { it.isFinite() }?.coerceIn(MIN_DISTANCE, MAX_DISTANCE) ?: DEFAULT_DISTANCE
    }

    fun inputText(raw: String): String {
        return raw.filter { it.isDigit() }
    }

    fun displayText(distance: Double): String = clamp(distance).toInt().toString()

    fun convert(distance: Double, from: DistanceUnit, to: DistanceUnit): Double {
        val converted = when {
            from == to -> distance
            from == DistanceUnit.YARDS && to == DistanceUnit.METERS -> distance * 0.9144
            else -> distance / 0.9144
        }
        return clamp(converted)
    }
}

data class TrackColorPreset(
    val name: String,
    val gradientColors: List<Long>,
    val glowBlur: Float = 30f,
    val glowAlpha: Float = 0.68f,
    val bandAlpha: Float = 1f,
) {
    fun colorAt(progress: Float): Long {
        val colors = gradientColors.ifEmpty { listOf(0xFFFFFFFF) }
        if (colors.size == 1) return colors.first()
        val t = progress.coerceIn(0f, 1f)
        val scaled = t * (colors.size - 1)
        val index = scaled.toInt().coerceIn(0, colors.size - 2)
        val fraction = scaled - index
        return lerpArgb(colors[index], colors[index + 1], fraction)
    }

    fun bandOuterColor(): Long = colorAt(0.7f)
}

val defaultTrackColorPresets = listOf(
    TrackColorPreset("霓虹极光", listOf(0xFFFF6CAE, 0xFFC471F5, 0xFF12C2E9), 30f, 0.70f, 1.00f),
    TrackColorPreset("日落晚霞", listOf(0xFFFF416C, 0xFFFF9A44, 0xFFFFD700), 28f, 0.68f, 1.00f),
    TrackColorPreset("冰焰双生", listOf(0xFF00C6FF, 0xFF6B73FF, 0xFFFF6CAE), 30f, 0.70f, 1.00f),
    TrackColorPreset("翡翠丛林", listOf(0xFF11998E, 0xFF38EF7D, 0xFFB7FF5A), 28f, 0.66f, 1.00f),
    TrackColorPreset("深空星尘", listOf(0xFF4776E6, 0xFF8E54E9, 0xFFF093FB), 30f, 0.68f, 1.00f),
    TrackColorPreset("熔岩橙", listOf(0xFFF7971E, 0xFFFFD200, 0xFFFF6B00), 28f, 0.66f, 1.00f),
    TrackColorPreset("玫瑰荧光", listOf(0xFFFF3F91, 0xFFFF5FA8, 0xFFFF8EC3), 28f, 0.68f, 1.00f),
    TrackColorPreset("湖蓝荧光", listOf(0xFF21B7FF, 0xFF4FD2FF, 0xFF96ECFF), 30f, 0.68f, 1.00f),
    TrackColorPreset("薄荷绿光", listOf(0xFF43E691, 0xFF63F7AE, 0xFF9AFFD0), 30f, 0.68f, 1.00f),
    TrackColorPreset("紫莓荧光", listOf(0xFFA855F7, 0xFFC084FC, 0xFFF0ABFC), 30f, 0.68f, 1.00f),
    TrackColorPreset("杏橙荧光", listOf(0xFFFF9C24, 0xFFFFB742, 0xFFFFD36B), 28f, 0.66f, 1.00f),
    TrackColorPreset("珊瑚蜜桃", listOf(0xFFFF5E72, 0xFFFF7F6E, 0xFFFFB083), 28f, 0.64f, 1.00f),
    TrackColorPreset("青蓝薄荷", listOf(0xFF35DFC7, 0xFF55F3D8, 0xFF8FFFF0), 30f, 0.70f, 1.00f),
    TrackColorPreset("荧光青柠", listOf(0xFFA8DC32, 0xFFC4F94C, 0xFFE2FF83), 28f, 0.60f, 0.94f),
    TrackColorPreset("金柠荧光", listOf(0xFFFFE143, 0xFFDFFF4E, 0xFFB9FF5A), 30f, 0.66f, 1.00f),
    TrackColorPreset("珍珠白", listOf(0xFFC3CCC8, 0xFFF0F6F3, 0xFFFFFFFF), 24f, 0.46f, 0.88f),
)

const val GOLF_CAM_DEFAULT_PRESET_NAME = "青蓝薄荷"
const val GOLF_CAM_DEFAULT_COLOR_HEX = 0xFF55F3D8

fun defaultGolfCamTrackColorPreset(): TrackColorPreset {
    return defaultTrackColorPresets.firstOrNull { it.name == GOLF_CAM_DEFAULT_PRESET_NAME }
        ?: defaultTrackColorPresets.first()
}

enum class DistanceUnit(val displayName: String, val abbreviation: String) {
    YARDS("码", "YDS"),
    METERS("米", "M");

    fun toggle(): DistanceUnit = if (this == YARDS) METERS else YARDS
}

internal fun parseGolfCamDistanceUnit(raw: String?, fallback: DistanceUnit = DistanceUnit.YARDS): DistanceUnit {
    return when (raw?.trim()?.lowercase()) {
        "yards", "yard", "yd", "yds" -> DistanceUnit.YARDS
        "meters", "meter", "m" -> DistanceUnit.METERS
        else -> fallback
    }
}

enum class TrackEffectType(val displayName: String) {
    GRADIENT("渐变光带"),
    PARTICLE("粒子拖尾"),
    FIRE("火焰能量"),
    ELECTRIC("电光闪电"),
}

data class TrackRenderTuning(
    val showOuterGlow: Boolean = false,
    val showBodyBand: Boolean = true,
    val showHighlightCore: Boolean = true,
    val bandAlphaMultiplier: Float = 1.0f,
    val bodyAlphaBase: Float = 0.10f,
    val bodyAlphaScale: Float = 0.98f,
    val glowBlurMultiplier: Float = 0.82f,
    val outerGlowMultiplier: Float = 0.62f,
    val fillGlowMultiplier: Float = 0.22f,
    val highlightWidthRatio: Float = 0.50f,
    val highlightAlphaMax: Float = 0.90f,
    val highlightWhiteLiftBase: Float = 0.00f,
    val highlightWhiteLiftScale: Float = 0.00f,
    val highlightStart: Float = 0.00f,
    val highlightEnd: Float = 0.96f,
    val highlightPeak: Float = 0.35f,
    val highlightStrength: Float = 1.6f,
    val widthTaperPower: Float = 1.36f,
    val apexThinStrength: Float = 0.60f,
    val apexThinRadius: Float = 0.20f,
    val highlightFalloffPower: Float = 0.82f,
)

internal fun TrackRenderTuning.clampedForGolfCamProduction(): TrackRenderTuning {
    fun finite(value: Float, min: Float, max: Float, fallback: Float): Float {
        return value.takeIf { it.isFinite() }?.coerceIn(min, max) ?: fallback.coerceIn(min, max)
    }

    val defaults = TrackRenderTuning()
    var start = highlightStart
    var end = highlightEnd
    var peak = highlightPeak
    if (abs(start - 0.20f) < 0.001f &&
        abs(end - 0.60f) < 0.001f &&
        abs(peak - 0.42f) < 0.001f
    ) {
        start = 0.00f
        end = 0.96f
        peak = 0.32f
    }
    if (abs(start - 0.18f) < 0.001f &&
        abs(end - 0.96f) < 0.001f
    ) {
        start = 0.00f
    }

    start = finite(start, 0.00f, 0.90f, defaults.highlightStart)
    end = finite(end, 0.05f, 1.00f, defaults.highlightEnd)
    if (end <= start + 0.05f) {
        end = min(1.0f, start + 0.05f)
    }
    peak = finite(peak, start, end, defaults.highlightPeak)

    return copy(
        bandAlphaMultiplier = finite(bandAlphaMultiplier, 0.00f, 1.40f, defaults.bandAlphaMultiplier),
        bodyAlphaBase = finite(bodyAlphaBase, 0.00f, 0.16f, defaults.bodyAlphaBase),
        bodyAlphaScale = finite(bodyAlphaScale, 0.50f, 1.20f, defaults.bodyAlphaScale),
        glowBlurMultiplier = finite(glowBlurMultiplier, 0.25f, 1.20f, defaults.glowBlurMultiplier),
        outerGlowMultiplier = finite(outerGlowMultiplier, 0.20f, 1.00f, defaults.outerGlowMultiplier),
        fillGlowMultiplier = finite(fillGlowMultiplier, 0.08f, 0.45f, defaults.fillGlowMultiplier),
        highlightWidthRatio = finite(highlightWidthRatio, 0.30f, 0.85f, defaults.highlightWidthRatio),
        highlightAlphaMax = finite(highlightAlphaMax, 0.00f, 0.90f, defaults.highlightAlphaMax),
        highlightWhiteLiftBase = finite(highlightWhiteLiftBase, 0.00f, 0.00f, defaults.highlightWhiteLiftBase),
        highlightWhiteLiftScale = finite(highlightWhiteLiftScale, 0.00f, 0.00f, defaults.highlightWhiteLiftScale),
        highlightStart = start,
        highlightEnd = end,
        highlightPeak = peak,
        highlightStrength = finite(highlightStrength, 0.00f, 1.60f, defaults.highlightStrength),
        widthTaperPower = finite(widthTaperPower, 0.70f, 2.20f, defaults.widthTaperPower),
        apexThinStrength = finite(apexThinStrength, 0.00f, 0.85f, defaults.apexThinStrength),
        apexThinRadius = finite(apexThinRadius, 0.04f, 0.35f, defaults.apexThinRadius),
        highlightFalloffPower = finite(highlightFalloffPower, 0.45f, 1.80f, defaults.highlightFalloffPower),
    )
}

data class TrackStyle(
    val colorPreset: TrackColorPreset = defaultGolfCamTrackColorPreset(),
    val colorHex: Long? = null,
    val colorAlpha: Double = 0.75,
    val glowOpacity: Double = colorPreset.glowAlpha.toDouble(),
    val widthPct: Double = 40.0,
    val taperPower: Float = 1.8f,
    val tailHalfLife: Double = 70.0,  // 5~100，对齐 iOS StyleControlRange
    val minTail: Float = 0.12f,
    val showDistance: Boolean = false,
    val totalDistance: Double = 150.0,
    val distanceUnit: DistanceUnit = DistanceUnit.YARDS,
    val speedLevel: Double = GolfCamSpeedConfig.DEFAULT_LEVEL,
    val activeTrajectoryAlgorithm: TrajectoryAlgorithmKind = TrajectoryAlgorithmKind.GOLF_CAM_LIKE,
    val compareTrajectoryAlgorithms: Boolean = false,
    val trajectoryPreset: TrajectoryPresetKind = TrajectoryPresetKind.GOLF_CAM_LIKE,
    val motionLaunchBias: Double = TrajectoryMotionParameters.standard.launchBias,
    val motionDrag: Double = TrajectoryMotionParameters.standard.drag,
    val motionLiftHold: Double = TrajectoryMotionParameters.standard.liftHold,
    val motionDescentAcceleration: Double = TrajectoryMotionParameters.standard.descentAcceleration,
    val motionTailBoost: Double = TrajectoryMotionParameters.standard.tailBoost,
    val motionAnchorTime: Double? = null,
    val v4VMin: Double? = null,
    val v4VMax: Double? = null,
    val v4PLeft: Double? = null,
    val v4PRight: Double? = null,
    val v4CustomSPeak: Double? = null,
    val effectType: TrackEffectType = TrackEffectType.GRADIENT,
    val renderTuning: TrackRenderTuning = TrackRenderTuning(),
) {
    fun primaryColorArgb(): Long = colorHex
        ?: if (colorPreset.name == GOLF_CAM_DEFAULT_PRESET_NAME) {
            GOLF_CAM_DEFAULT_COLOR_HEX
        } else {
            colorPreset.bandOuterColor()
        }
}

object GolfCamTrailGeometry {
    const val SEGMENT_COUNT = 360

    private const val TRAIL_WIDTH_HEAD_RATIO = 0.00085f
    private const val TAIL_WIDTH_MIN_RATIO = 0.0040f
    private const val TAIL_WIDTH_MAX_RATIO = 0.0095f
    private const val GLOW_WIDTH_MULTIPLIER = 1.18f

    fun strokeWidth(
        widthPct: Double,
        videoHeight: Float,
        globalProgress: Float,
        apexProgress: Float,
        tuning: TrackRenderTuning,
    ): Float {
        fun finiteFloat(value: Float, fallback: Float): Float {
            return value.takeIf { it.isFinite() } ?: fallback
        }
        fun finiteDouble(value: Double, fallback: Double): Double {
            return value.takeIf { it.isFinite() } ?: fallback
        }

        val safeTuning = tuning.clampedForGolfCamProduction()
        val safeHeight = finiteFloat(videoHeight, fallback = 1f).coerceAtLeast(1f)
        val pct = (finiteDouble(widthPct, fallback = 40.0).toFloat().coerceIn(0f, 160f)) / 100f
        val headHalfWidth = safeHeight * TRAIL_WIDTH_HEAD_RATIO * GLOW_WIDTH_MULTIPLIER
        val minTailHalfWidth = safeHeight * TAIL_WIDTH_MIN_RATIO * GLOW_WIDTH_MULTIPLIER
        val maxTailHalfWidth = safeHeight * TAIL_WIDTH_MAX_RATIO * GLOW_WIDTH_MULTIPLIER
        val tailHalfWidth = minTailHalfWidth + (maxTailHalfWidth - minTailHalfWidth) * pct
        val p = finiteFloat(globalProgress, fallback = 0f).coerceIn(0f, 1f)
        val taper = (1f - p).toDouble().pow(safeTuning.widthTaperPower.toDouble()).toFloat()
        val halfWidth = headHalfWidth + (tailHalfWidth - headHalfWidth) * taper
        val apex = finiteFloat(apexProgress, fallback = 0.5f).coerceIn(0.05f, 0.95f)
        val radius = safeTuning.apexThinRadius.coerceAtLeast(0.001f)
        val distance = (abs(p - apex) / radius).coerceIn(0f, 1f)
        val apexWeight = 1f - distance * distance * (3f - 2f * distance)
        val thinFactor = 1f - safeTuning.apexThinStrength * apexWeight
        return (2f * halfWidth * thinFactor).coerceAtLeast(0.10f)
    }
}

private fun lerpArgb(a: Long, b: Long, t: Float): Long {
    val tc = t.coerceIn(0f, 1f)
    fun channel(color: Long, shift: Int): Int = ((color ushr shift) and 0xFF).toInt()
    fun mix(shift: Int): Int {
        val av = channel(a, shift)
        val bv = channel(b, shift)
        return (av + (bv - av) * tc).toInt().coerceIn(0, 255)
    }
    return (mix(24).toLong() shl 24) or
        (mix(16).toLong() shl 16) or
        (mix(8).toLong() shl 8) or
        mix(0).toLong()
}

// ─── 工作流步骤输出（对齐 iOS GolfCamWorkflowState 各步骤）───────────────────────

data class GolfCamVideoConfig(
    val durationMs: Long = 1L,
    val frameRate: Double = 30.0,
    val visualWidth: Int = 1,
    val visualHeight: Int = 1,
    val rotationDegrees: Int = 0,
) {
    val aspectRatio: Float
        get() = visualWidth.coerceAtLeast(1).toFloat() / visualHeight.coerceAtLeast(1).toFloat()

    val isPortrait: Boolean get() = visualHeight >= visualWidth
}

data class TrimResult(val startMs: Long, val endMs: Long)

/** 音频检测原始候选（绝对时间） */
data class ImpactCandidate(
    val timeSec: Double,
    val confidence: Float,  // 0-1，相对最高分归一化
) {
    val confidencePercent: Int
        get() = ((confidence.takeIf { it.isFinite() } ?: 0f).coerceIn(0f, 1f) * 100).toInt()
}

/** UI 层候选帧（相对裁剪片段的帧索引） */
data class CandidateImpactFrame(
    val frameIndex: Int,
    val confidence: Float,
    val timeSec: Double,
) {
    val confidencePercent: Int
        get() = ((confidence.takeIf { it.isFinite() } ?: 0f).coerceIn(0f, 1f) * 100).toInt()
}

data class ImpactFrameSelection(
    val frameIndex: Int,
    val timeMs: Long,
    val candidateFrames: List<CandidateImpactFrame> = emptyList(),
)

data class ImpactPositionSelection(val normalizedX: Float, val normalizedY: Float)

data class LandingOutput(
    val normalizedX: Float,
    val normalizedY: Float,
    val flightTimeSec: Double = 4.5,  // 飞行时间（秒），对应 iOS landingTime
)

data class FlightCurveSelection(
    val impactPosition: ImpactPositionSelection,
    val landingPoint: LandingOutput,
    val controlPoint1: NormalizedPoint,
    val controlPoint2: NormalizedPoint,
) {
    val bezierPath: BezierPath get() = BezierPath(
        p0 = NormalizedPoint(impactPosition.normalizedX, impactPosition.normalizedY),
        p1 = controlPoint1,
        p2 = controlPoint2,
        p3 = NormalizedPoint(landingPoint.normalizedX, landingPoint.normalizedY),
    )
}

data class TrackStyleSelection(
    val style: TrackStyle,
    val flightCurve: FlightCurveSelection,
)
