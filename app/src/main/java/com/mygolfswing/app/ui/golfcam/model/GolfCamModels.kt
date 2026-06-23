package com.mygolfswing.app.ui.golfcam.model

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

// ─── 几何基础 ──────────────────────────────────────────────────────────────────

data class NormalizedPoint(val x: Float, val y: Float) {
    fun toOffset(width: Float, height: Float) = Offset(x * width, y * height)
}

/** 三次贝塞尔路径（归一化坐标 0~1）*/
data class BezierPath(
    val p0: NormalizedPoint,
    val p1: NormalizedPoint,
    val p2: NormalizedPoint,
    val p3: NormalizedPoint,
) {
    fun point(t: Float): NormalizedPoint {
        val mt = 1f - t
        return NormalizedPoint(
            x = mt * mt * mt * p0.x + 3 * mt * mt * t * p1.x + 3 * mt * t * t * p2.x + t * t * t * p3.x,
            y = mt * mt * mt * p0.y + 3 * mt * mt * t * p1.y + 3 * mt * t * t * p2.y + t * t * t * p3.y,
        )
    }

    fun pointAtArcProgress(progress: Float, arcTable: ArcLengthTable): NormalizedPoint {
        if (arcTable.totalLength <= 0f) return point(progress.coerceIn(0f, 1f))
        val u = arcTable.uAtArcLength(arcTable.totalLength * progress.coerceIn(0f, 1f))
        return point(u)
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
    private val uSamples = FloatArray(sampleCount + 1) { it.toFloat() / sampleCount }
    val arcLengths = FloatArray(sampleCount + 1)

    val totalLength: Float

    init {
        var prev = bezier.point(0f)
        for (i in 1..sampleCount) {
            val curr = bezier.point(uSamples[i])
            val dx = curr.x - prev.x
            val dy = curr.y - prev.y
            arcLengths[i] = arcLengths[i - 1] + kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            prev = curr
        }
        totalLength = arcLengths[sampleCount]
    }

    fun uAtArcLength(s: Float): Float {
        val target = s.coerceIn(0f, totalLength)
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
        val target = u.coerceIn(0f, 1f)
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
        if (totalLength <= 0f) return 0.6f
        return (arcLengthAtU(apexU()) / totalLength).coerceIn(0.05f, 0.95f)
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

    fun clamp(v: Double) = v.coerceIn(MIN_LEVEL, MAX_LEVEL)
    fun normalized(v: Double) = (clamp(v) - MIN_LEVEL) / (MAX_LEVEL - MIN_LEVEL)
    fun percentText(v: Double) = "${(clamp(v) * 100).toInt()}%"

    private fun calibratedGamma(v: Double): Double {
        val x = normalized(v)
        val low = 1.01; val mid = 0.383; val high = 0.150
        val a = 2.0 * (high + low - 2.0 * mid)
        val b = high - low - a
        return max(0.05, a * x * x + b * x + low)
    }

    fun progress(timeProgress: Double, speedLevel: Double): Double {
        val tau = timeProgress.coerceIn(0.0, 1.0)
        if (tau <= 0.0) return 0.0
        if (tau >= 1.0) return 1.0
        return tau.pow(calibratedGamma(speedLevel)).coerceIn(0.0, 1.0)
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
        val dur = max(totalDurationSec, 1e-6)
        val elapsed = max(0.0, elapsedSec)
        if (elapsed >= dur) return 1.0
        val fps = if (frameRate > 1.0) frameRate else 30.0
        val frameIndex = (elapsed * fps).toInt()
        val sampledFrame = frameIndex - (frameIndex % max(updateIntervalFrames, 1))
        val sampledTime = sampledFrame / fps
        return progress(sampledTime / dur, speedLevel)
    }

    fun displayDistance(totalDistance: Double, elapsedSec: Double, totalDurationSec: Double, speedLevel: Double): Int {
        val p = displayProgress(elapsedSec, totalDurationSec, speedLevel)
        val final = totalDistance.roundToInt()
        if (p >= 1.0) return max(0, final)
        return max(0, min(final, (totalDistance * p).toInt()))
    }

    fun visibleTailStartProgress(
        headProgress: Double,
        tailHalfLife: Double,
        activeAlgorithm: TrajectoryAlgorithmKind,
        apexProgress: Double,
    ): Double {
        val head = headProgress.coerceIn(0.0, 1.0)
        val rawTailPercent = (tailHalfLife / 100.0).coerceIn(0.0, 1.0)
        if (rawTailPercent >= 0.995) return 0.0

        if (activeAlgorithm != TrajectoryAlgorithmKind.GOLF_CAM_LIKE) {
            val tailLength = min(head, rawTailPercent)
            return max(0.0, head - tailLength)
        }

        val apex = apexProgress.coerceIn(0.05, 0.95)
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
}

// ─── 轨迹样式（对齐 iOS TrackStyle）──────────────────────────────────────────────

data class TrackColorPreset(
    val name: String,
    val gradientColors: List<Long>,
    val glowBlur: Float = 30f,
    val glowAlpha: Float = 0.68f,
    val bandAlpha: Float = 1f,
)

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

enum class DistanceUnit(val displayName: String, val abbreviation: String) {
    YARDS("码", "YDS"),
    METERS("米", "M");

    fun toggle(): DistanceUnit = if (this == YARDS) METERS else YARDS
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

data class TrackStyle(
    val colorPreset: TrackColorPreset = defaultTrackColorPresets.getOrElse(2) { defaultTrackColorPresets.first() },
    val colorAlpha: Double = 0.75,
    val glowOpacity: Double = colorPreset.glowAlpha.toDouble(),
    val widthPct: Double = 70.0,
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
)

object GolfCamTrailGeometry {
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
        val safeHeight = videoHeight.coerceAtLeast(1f)
        val pct = (widthPct.toFloat().coerceIn(0f, 160f)) / 100f
        val headHalfWidth = safeHeight * TRAIL_WIDTH_HEAD_RATIO * GLOW_WIDTH_MULTIPLIER
        val minTailHalfWidth = safeHeight * TAIL_WIDTH_MIN_RATIO * GLOW_WIDTH_MULTIPLIER
        val maxTailHalfWidth = safeHeight * TAIL_WIDTH_MAX_RATIO * GLOW_WIDTH_MULTIPLIER
        val tailHalfWidth = minTailHalfWidth + (maxTailHalfWidth - minTailHalfWidth) * pct
        val p = globalProgress.coerceIn(0f, 1f)
        val taper = (1f - p).toDouble().pow(tuning.widthTaperPower.toDouble()).toFloat()
        val halfWidth = headHalfWidth + (tailHalfWidth - headHalfWidth) * taper
        val apex = apexProgress.coerceIn(0.05f, 0.95f)
        val radius = tuning.apexThinRadius.coerceAtLeast(0.001f)
        val distance = (abs(p - apex) / radius).coerceIn(0f, 1f)
        val apexWeight = 1f - distance * distance * (3f - 2f * distance)
        val thinFactor = 1f - tuning.apexThinStrength * apexWeight
        return (2f * halfWidth * thinFactor).coerceAtLeast(0.10f)
    }
}

// ─── 工作流步骤输出（对齐 iOS GolfCamWorkflowState 各步骤）───────────────────────

data class TrimResult(val startMs: Long, val endMs: Long)

/** 音频检测原始候选（绝对时间） */
data class ImpactCandidate(
    val timeSec: Double,
    val confidence: Float,  // 0-1，相对最高分归一化
) {
    val confidencePercent: Int get() = (confidence * 100).toInt()
}

/** UI 层候选帧（相对裁剪片段的帧索引） */
data class CandidateImpactFrame(
    val frameIndex: Int,
    val confidence: Float,
    val timeSec: Double,
) {
    val confidencePercent: Int get() = (confidence * 100).toInt()
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
