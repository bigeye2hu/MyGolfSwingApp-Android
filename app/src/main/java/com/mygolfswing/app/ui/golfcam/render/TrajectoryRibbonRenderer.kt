package com.mygolfswing.app.ui.golfcam.render

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.mygolfswing.app.ui.golfcam.model.ArcLengthTable
import com.mygolfswing.app.ui.golfcam.model.BezierPath
import com.mygolfswing.app.ui.golfcam.model.GolfCamTrailGeometry
import com.mygolfswing.app.ui.golfcam.model.TrackRenderTuning
import com.mygolfswing.app.ui.golfcam.model.TrackStyle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

internal object TrajectoryRibbonRenderer {
    fun draw(
        canvas: Canvas,
        bezier: BezierPath,
        arcTable: ArcLengthTable,
        headProgress: Float,
        tailProgress: Float,
        style: TrackStyle,
        width: Float,
        height: Float,
    ) {
        val head = headProgress.coerceIn(0f, 1f)
        val tail = tailProgress.coerceIn(0f, head)
        if (head <= tail || width <= 0f || height <= 0f) return

        val tuning = style.renderTuning
        if (!tuning.showOuterGlow && !tuning.showBodyBand && !tuning.showHighlightCore) return

        val samples = buildSamples(bezier, arcTable, head, tail, style, width, height, tuning)
        if (samples.size < 2) return
        val edges = buildEdges(samples) ?: return

        val baseColor = style.primaryColorArgb()
        val baseOpacity = (style.colorAlpha * alphaOf(baseColor)).toFloat().coerceIn(0f, 1f)
        if (baseOpacity <= 0f) return

        if (tuning.showOuterGlow) {
            drawGlow(canvas, edges, baseColor, baseOpacity, style, tuning)
        }
        if (tuning.showBodyBand) {
            drawBodyBand(canvas, edges, baseColor, style, tuning)
        }
        if (tuning.showHighlightCore) {
            drawHighlight(canvas, edges, baseColor, style, tuning)
        }
    }

    internal fun bodyBandAlpha(style: TrackStyle, tuning: TrackRenderTuning = style.renderTuning): Float {
        val baseColor = style.primaryColorArgb()
        val baseOpacity = (style.colorAlpha * alphaOf(baseColor)).toFloat().coerceIn(0f, 1f)
        val presetBandAlpha = style.colorPreset.bandAlpha.coerceIn(0f, 1f)
        return (baseOpacity * presetBandAlpha * tuning.bandAlphaMultiplier).coerceIn(0f, 1f)
    }

    private fun buildSamples(
        bezier: BezierPath,
        arcTable: ArcLengthTable,
        headProgress: Float,
        tailProgress: Float,
        style: TrackStyle,
        width: Float,
        height: Float,
        tuning: TrackRenderTuning,
    ): List<RibbonSample> {
        val samples = ArrayList<RibbonSample>(GolfCamTrailGeometry.SEGMENT_COUNT + 1)
        val apexProgress = arcTable.apexProgress()
        for (i in 0..GolfCamTrailGeometry.SEGMENT_COUNT) {
            val frac = i.toFloat() / GolfCamTrailGeometry.SEGMENT_COUNT
            val progress = tailProgress + (headProgress - tailProgress) * frac
            val point = bezier.pointAtArcProgress(progress, arcTable)
            val fullWidth = GolfCamTrailGeometry.strokeWidth(
                widthPct = style.widthPct,
                videoHeight = height,
                globalProgress = progress,
                apexProgress = apexProgress,
                tuning = tuning,
            )
            samples += RibbonSample(
                x = point.x * width,
                y = point.y * height,
                progress = progress,
                halfWidth = (fullWidth / 2f).coerceAtLeast(0.05f),
            )
        }
        return samples
    }

    private fun buildEdges(samples: List<RibbonSample>): RibbonEdges? {
        if (samples.size < 2) return null
        val left = ArrayList<Point>(samples.size)
        val right = ArrayList<Point>(samples.size)

        for (i in samples.indices) {
            val tangent = tangentAt(i, samples)
            val normalX = -tangent.y
            val normalY = tangent.x
            val sample = samples[i]
            left += Point(
                x = sample.x + normalX * sample.halfWidth,
                y = sample.y + normalY * sample.halfWidth,
                progress = sample.progress,
            )
            right += Point(
                x = sample.x - normalX * sample.halfWidth,
                y = sample.y - normalY * sample.halfWidth,
                progress = sample.progress,
            )
        }
        return RibbonEdges(left = left, right = right)
    }

    private fun tangentAt(index: Int, samples: List<RibbonSample>): Point {
        val first: RibbonSample
        val second: RibbonSample
        if (index == 0) {
            first = samples[0]
            second = samples[1]
        } else if (index == samples.lastIndex) {
            first = samples[index - 1]
            second = samples[index]
        } else {
            first = samples[index - 1]
            second = samples[index + 1]
        }
        val dx = second.x - first.x
        val dy = second.y - first.y
        val length = sqrt(dx * dx + dy * dy)
        if (length < 1e-6f) return Point(0f, -1f, 0f)
        return Point(dx / length, dy / length, 0f)
    }

    private fun drawGlow(
        canvas: Canvas,
        edges: RibbonEdges,
        baseColor: Long,
        baseOpacity: Float,
        style: TrackStyle,
        tuning: TrackRenderTuning,
    ) {
        val path = makeRibbonPath(edges)
        val blurAlpha = (baseOpacity * style.glowOpacity.toFloat() * tuning.outerGlowMultiplier).coerceIn(0f, 0.70f)
        val fillAlpha = (baseOpacity * style.glowOpacity.toFloat() * tuning.fillGlowMultiplier).coerceIn(0f, 0.30f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
        }
        if (blurAlpha > 0f) {
            paint.color = colorWithAlpha(baseColor, blurAlpha)
            paint.maskFilter = BlurMaskFilter(max(2f, style.colorPreset.glowBlur * tuning.glowBlurMultiplier), BlurMaskFilter.Blur.NORMAL)
            canvas.drawPath(path, paint)
            paint.maskFilter = null
        }
        if (fillAlpha > 0f) {
            paint.color = colorWithAlpha(baseColor, fillAlpha)
            canvas.drawPath(path, paint)
        }
    }

    private fun drawBodyBand(
        canvas: Canvas,
        edges: RibbonEdges,
        baseColor: Long,
        style: TrackStyle,
        tuning: TrackRenderTuning,
    ) {
        val bandAlpha = bodyBandAlpha(style, tuning)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
        }
        val last = max(1, edges.left.lastIndex)

        forEachSegment(edges) { index, path ->
            val tailFade = tailEnvelope(index, last, power = 1.55f)
            val alpha = bandAlpha * (0.02f + 0.20f * tailFade)
            paint.color = colorWithAlpha(baseColor, alpha.coerceIn(0f, 1f))
            canvas.drawPath(path, paint)
        }

        forEachSegment(edges) { index, path ->
            val tailFade = tailEnvelope(index, last)
            val alpha = bandAlpha * (tuning.bodyAlphaBase + tuning.bodyAlphaScale * tailFade)
            paint.color = colorWithAlpha(baseColor, alpha.coerceIn(0f, 1f))
            canvas.drawPath(path, paint)
        }
    }

    private fun drawHighlight(
        canvas: Canvas,
        edges: RibbonEdges,
        baseColor: Long,
        style: TrackStyle,
        tuning: TrackRenderTuning,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
        }
        val bandAlpha = bodyBandAlpha(style, tuning)
        val last = max(1, edges.left.lastIndex)
        forEachSegment(edges) { index, path ->
            val progress = (edges.left[index - 1].progress + edges.left[index].progress) * 0.5f
            val weight = highlightWeight(progress, tuning.highlightStart, tuning.highlightPeak, tuning.highlightEnd, tuning.highlightFalloffPower)
            val tailFade = tailEnvelope(index, last)
            if (weight <= 0.001f || tailFade <= 0.001f) return@forEachSegment

            val alpha = minOf(
                tuning.highlightAlphaMax,
                bandAlpha * tuning.highlightStrength * (0.14f + 0.62f * weight),
            ) * weight * tailFade
            paint.color = colorWithAlpha(baseColor, alpha.coerceIn(0f, 1f))
            canvas.drawPath(path, paint)
        }
    }

    private fun forEachSegment(edges: RibbonEdges, draw: (index: Int, path: Path) -> Unit) {
        for (i in 1..edges.left.lastIndex) {
            val path = Path().apply {
                moveTo(edges.left[i - 1].x, edges.left[i - 1].y)
                lineTo(edges.left[i].x, edges.left[i].y)
                lineTo(edges.right[i].x, edges.right[i].y)
                lineTo(edges.right[i - 1].x, edges.right[i - 1].y)
                close()
            }
            draw(i, path)
        }
    }

    private fun makeRibbonPath(edges: RibbonEdges): Path {
        return Path().apply {
            moveTo(edges.left.first().x, edges.left.first().y)
            for (i in 1..edges.left.lastIndex) {
                lineTo(edges.left[i].x, edges.left[i].y)
            }
            for (i in edges.right.lastIndex downTo 0) {
                lineTo(edges.right[i].x, edges.right[i].y)
            }
            close()
        }
    }

    private fun tailEnvelope(index: Int, last: Int, power: Float = 1.6f): Float {
        val local = (index.toFloat() / last.toFloat()).coerceIn(0f, 1f)
        return local.toDouble().pow(power.toDouble()).toFloat()
    }

    private fun highlightWeight(progress: Float, start: Float, peak: Float, end: Float, falloffPower: Float): Float {
        if (progress < start || progress > end) return 0f
        return if (progress <= peak) {
            smoothStep(start, peak, progress)
        } else {
            val t = smoothStep(peak, end, progress)
            (1f - t).coerceIn(0f, 1f).toDouble().pow(falloffPower.toDouble()).toFloat()
        }
    }

    private fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
        if (abs(edge1 - edge0) <= 0.0001f) return if (value >= edge1) 1f else 0f
        val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun alphaOf(argb: Long): Double = ((argb ushr 24) and 0xFF).toDouble() / 255.0

    private fun colorWithAlpha(argb: Long, alpha: Float): Int {
        return Color.argb(
            (alpha.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255),
            ((argb ushr 16) and 0xFF).toInt(),
            ((argb ushr 8) and 0xFF).toInt(),
            (argb and 0xFF).toInt(),
        )
    }

    private data class RibbonSample(
        val x: Float,
        val y: Float,
        val progress: Float,
        val halfWidth: Float,
    )

    private data class Point(
        val x: Float,
        val y: Float,
        val progress: Float,
    )

    private data class RibbonEdges(
        val left: List<Point>,
        val right: List<Point>,
    )
}
