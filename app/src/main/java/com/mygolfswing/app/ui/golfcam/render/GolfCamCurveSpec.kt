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
    private const val SUGGEST_MIN_X = 0.02f
    private const val SUGGEST_MAX_X = 0.98f
    private const val SUGGEST_MIN_Y = -0.5f
    private const val SUGGEST_MAX_Y = 1.2f
    private const val DRAG_MIN_X = 0f
    private const val DRAG_MAX_X = 1f
    private const val DRAG_MIN_Y = -1f
    private const val DRAG_MAX_Y = 1.5f

    fun suggestedControlPoints(
        start: NormalizedPoint,
        end: NormalizedPoint,
    ): Pair<NormalizedPoint, NormalizedPoint> {
        val dx = end.x - start.x
        val horizontal = abs(dx)
        if (horizontal < 0.01f) {
            val lift = 0.14f
            return clampPoint(NormalizedPoint(start.x, start.y - lift * 0.55f)) to
                clampPoint(NormalizedPoint(start.x, minOf(start.y, end.y) - lift))
        }

        val topReference = minOf(start.y, end.y)
        val lift = maxOf(CONTROL_BASE_LIFT, CONTROL_BASE_LIFT + horizontal * CONTROL_X_LIFT)
        val handleY = maxOf(0.04f, topReference - lift)

        val p2 = NormalizedPoint(
            x = start.x + dx * CONTROL_P2_X,
            y = handleY,
        )
        val p1 = NormalizedPoint(
            x = start.x + dx * CONTROL_P1_X,
            y = start.y + (handleY - start.y) * CONTROL_P1_Y,
        )
        return clampPoint(p1) to clampPoint(p2)
    }

    fun clampPoint(point: NormalizedPoint): NormalizedPoint {
        return NormalizedPoint(
            x = point.x.coerceIn(SUGGEST_MIN_X, SUGGEST_MAX_X),
            y = point.y.coerceIn(SUGGEST_MIN_Y, SUGGEST_MAX_Y),
        )
    }

    fun clampDraggedControlPoint(point: NormalizedPoint): NormalizedPoint {
        return NormalizedPoint(
            x = point.x.coerceIn(DRAG_MIN_X, DRAG_MAX_X),
            y = point.y.coerceIn(DRAG_MIN_Y, DRAG_MAX_Y),
        )
    }

    fun foregroundWidth(index: Int, segments: Int = SEGMENTS): Float {
        val t = index.toFloat() / maxOf(1, segments - 1)
        val launchFullness = (1f - t).toDouble().pow(1.05).toFloat()
        return 1.15f + launchFullness * 5.35f
    }

    fun segmentColor(index: Int, segments: Int = SEGMENTS): Color {
        val t = index.toDouble() / maxOf(1, segments - 1)
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

    private fun interpolateRgb(a: Rgb, b: Rgb, amount: Double): Rgb {
        val t = amount.coerceIn(0.0, 1.0)
        return Rgb(
            r = a.r + (b.r - a.r) * t,
            g = a.g + (b.g - a.g) * t,
            b = a.b + (b.b - a.b) * t,
        )
    }
}
