package com.mygolfswing.app.ui.golfcam.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.mygolfswing.app.ui.golfcam.model.NormalizedPoint
import kotlin.math.abs
import kotlin.math.pow

internal object GolfCamCurveSpec {
    const val SEGMENTS = 180
    const val SHADOW_ALPHA = 0.14f
    const val SHADOW_WIDTH_ADD = 1.1f
    val STROKE_CAP: StrokeCap = StrokeCap.Butt

    private const val CONTROL_P1_X = 0.08f
    private const val CONTROL_P2_X = 0.32f
    private const val CONTROL_P1_Y = 0.56f
    private const val CONTROL_BASE_LIFT = 0.26f
    private const val CONTROL_X_LIFT = 0.26f
    private const val CONTROL_MIN_X = -0.35f
    private const val CONTROL_MAX_X = 1.35f
    private const val CONTROL_MIN_Y = -0.8f
    private const val CONTROL_MAX_Y = 1.4f

    fun suggestedControlPoints(
        start: NormalizedPoint,
        end: NormalizedPoint,
    ): Pair<NormalizedPoint, NormalizedPoint> {
        val safeStart = normalizedPointOrCenter(start)
        val safeEnd = normalizedPointOrCenter(end)
        val dx = safeEnd.x - safeStart.x
        val horizontal = abs(dx)
        if (horizontal < 0.01f) {
            val lift = 0.14f
            return clampPoint(NormalizedPoint(safeStart.x, safeStart.y - lift * 0.55f)) to
                clampPoint(NormalizedPoint(safeStart.x, minOf(safeStart.y, safeEnd.y) - lift))
        }

        val topReference = minOf(safeStart.y, safeEnd.y)
        val lift = maxOf(CONTROL_BASE_LIFT, CONTROL_BASE_LIFT + horizontal * CONTROL_X_LIFT)
        val handleY = maxOf(0.04f, topReference - lift)

        val p2 = NormalizedPoint(
            x = safeStart.x + dx * CONTROL_P2_X,
            y = handleY,
        )
        val p1 = NormalizedPoint(
            x = safeStart.x + dx * CONTROL_P1_X,
            y = safeStart.y + (handleY - safeStart.y) * CONTROL_P1_Y,
        )
        return clampPoint(p1) to clampPoint(p2)
    }

    fun clampPoint(point: NormalizedPoint): NormalizedPoint {
        return NormalizedPoint(
            x = finiteFloat(point.x, fallback = 0.5f).coerceIn(CONTROL_MIN_X, CONTROL_MAX_X),
            y = finiteFloat(point.y, fallback = 0.5f).coerceIn(CONTROL_MIN_Y, CONTROL_MAX_Y),
        )
    }

    fun clampDraggedControlPoint(point: NormalizedPoint): NormalizedPoint {
        return clampPoint(point)
    }

    fun foregroundWidth(index: Int, segments: Int = SEGMENTS): Float {
        val t = segmentProgress(index, segments).toFloat()
        val launchFullness = (1f - t).toDouble().pow(1.05).toFloat()
        return 1.15f + launchFullness * 5.35f
    }

    fun segmentColor(index: Int, segments: Int = SEGMENTS): Color {
        val t = segmentProgress(index, segments)
        val brightness = t.pow(0.85)
        val opacity = 0.24 + brightness * 0.72
        val color = if (t < 0.42) {
            interpolateRgb(Rgb(0.08, 0.23, 0.18), Rgb(0.25, 0.62, 0.49), t / 0.42)
        } else {
            interpolateRgb(Rgb(0.25, 0.62, 0.49), Rgb(0.55, 0.96, 0.82), (t - 0.42) / 0.58)
        }
        return Color(
            red = color.r.toFloat(),
            green = color.g.toFloat(),
            blue = color.b.toFloat(),
            alpha = opacity.toFloat(),
        )
    }

    private data class Rgb(val r: Double, val g: Double, val b: Double)

    private fun finiteFloat(value: Float, fallback: Float): Float {
        return value.takeIf { it.isFinite() } ?: fallback
    }

    private fun normalizedPointOrCenter(point: NormalizedPoint): NormalizedPoint {
        return NormalizedPoint(
            x = finiteFloat(point.x, fallback = 0.5f).coerceIn(0f, 1f),
            y = finiteFloat(point.y, fallback = 0.5f).coerceIn(0f, 1f),
        )
    }

    private fun segmentProgress(index: Int, segments: Int): Double {
        val last = maxOf(1, segments - 1)
        return (index.toDouble() / last.toDouble()).coerceIn(0.0, 1.0)
    }

    private fun interpolateRgb(a: Rgb, b: Rgb, amount: Double): Rgb {
        val t = amount.coerceIn(0.0, 1.0)
        return Rgb(
            r = a.r + (b.r - a.r) * t,
            g = a.g + (b.g - a.g) * t,
            b = a.b + (b.b - a.b) * t,
        )
    }
}
