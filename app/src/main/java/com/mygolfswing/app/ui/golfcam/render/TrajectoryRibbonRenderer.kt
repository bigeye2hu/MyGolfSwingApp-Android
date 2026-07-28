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
import com.mygolfswing.app.ui.golfcam.model.clampedTrackStyle
import com.mygolfswing.app.ui.golfcam.model.clampedForGolfCamProduction
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
        val head = finiteFloat(headProgress, fallback = 0f).coerceIn(0f, 1f)
        val tail = finiteFloat(tailProgress, fallback = 0f).coerceIn(0f, head)
        val safeWidth = finiteFloat(width, fallback = 0f)
        val safeHeight = finiteFloat(height, fallback = 0f)
        if (head <= tail || safeWidth <= 0f || safeHeight <= 0f) return

        val safeStyle = clampedTrackStyle(style)
        val tuning = safeStyle.renderTuning
        if (!tuning.showOuterGlow && !tuning.showBodyBand && !tuning.showHighlightCore) return

        val samples = buildSamples(bezier, arcTable, head, tail, safeStyle, safeWidth, safeHeight, tuning)
        if (samples.size < 2) return
        val edges = buildEdges(samples) ?: return

        val baseColor = safeStyle.primaryColorArgb()
        val baseOpacity = baseOpacity(safeStyle, baseColor)
        if (baseOpacity <= 0f) return

        if (tuning.showOuterGlow) {
            drawGlow(canvas, edges, baseColor, baseOpacity, safeStyle, tuning)
        }
        if (tuning.showBodyBand) {
            drawBodyBand(canvas, edges, baseColor, safeStyle, tuning)
        }
        if (tuning.showHighlightCore) {
            drawHighlight(canvas, edges, baseColor, safeStyle, tuning)
        }
    }

    internal fun bodyBandAlpha(style: TrackStyle, tuning: TrackRenderTuning = style.renderTuning): Float {
        val baseColor = style.primaryColorArgb()
        val baseOpacity = baseOpacity(style, baseColor)
        val presetBandAlpha = finiteUnit(style.colorPreset.bandAlpha, fallback = 1f)
        val multiplier = finiteFloat(
            tuning.bandAlphaMultiplier,
            fallback = TrackRenderTuning().bandAlphaMultiplier,
        ).coerceIn(0f, 1.40f)
        return finiteUnit(baseOpacity * presetBandAlpha * multiplier, fallback = 0f)
    }

    internal fun highlightWidthRatio(tuning: TrackRenderTuning): Float {
        return tuning.clampedForGolfCamProduction().highlightWidthRatio
    }

    private fun finiteFloat(value: Float, fallback: Float): Float {
        return value.takeIf { it.isFinite() } ?: fallback
    }

    private fun finiteUnit(value: Float, fallback: Float): Float {
        return finiteFloat(value, fallback).coerceIn(0f, 1f)
    }

    private fun baseOpacity(style: TrackStyle, baseColor: Long): Float {
        return finiteUnit((style.colorAlpha * alphaOf(baseColor)).toFloat(), fallback = 0f)
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
        val glowOpacity = finiteUnit(style.glowOpacity.toFloat(), fallback = 0f)
        val blurAlpha = finiteUnit(baseOpacity * glowOpacity * tuning.outerGlowMultiplier, fallback = 0f)
            .coerceAtMost(0.70f)
        val fillAlpha = finiteUnit(baseOpacity * glowOpacity * tuning.fillGlowMultiplier, fallback = 0f)
            .coerceAtMost(0.30f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
        }
        if (blurAlpha > 0f) {
            paint.color = colorWithAlpha(baseColor, blurAlpha)
            val blurRadius = max(
                2f,
                finiteFloat(style.colorPreset.glowBlur, fallback = 0f) * tuning.glowBlurMultiplier,
            )
            paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
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
            paint.color = colorWithAlpha(baseColor, alpha)
            canvas.drawPath(path, paint)
        }

        forEachSegment(edges) { index, path ->
            val tailFade = tailEnvelope(index, last)
            val alpha = bandAlpha * (tuning.bodyAlphaBase + tuning.bodyAlphaScale * tailFade)
            paint.color = colorWithAlpha(baseColor, alpha)
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
            paint.color = colorWithAlpha(baseColor, alpha)
            canvas.drawPath(highlightSegmentPath(edges, index, highlightWidthRatio(tuning)), paint)
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

    private fun highlightSegmentPath(edges: RibbonEdges, index: Int, widthRatio: Float): Path {
        val ratio = finiteUnit(widthRatio, fallback = TrackRenderTuning().highlightWidthRatio)
        fun inset(point: Point, opposite: Point): Point {
            val centerX = (point.x + opposite.x) * 0.5f
            val centerY = (point.y + opposite.y) * 0.5f
            return Point(
                x = centerX + (point.x - centerX) * ratio,
                y = centerY + (point.y - centerY) * ratio,
                progress = point.progress,
            )
        }
        val left0 = inset(edges.left[index - 1], edges.right[index - 1])
        val left1 = inset(edges.left[index], edges.right[index])
        val right1 = inset(edges.right[index], edges.left[index])
        val right0 = inset(edges.right[index - 1], edges.left[index - 1])
        return Path().apply {
            moveTo(left0.x, left0.y)
            lineTo(left1.x, left1.y)
            lineTo(right1.x, right1.y)
            lineTo(right0.x, right0.y)
            close()
        }
    }

    private fun tailEnvelope(index: Int, last: Int, power: Float = 1.6f): Float {
        val local = finiteUnit(index.toFloat() / last.toFloat(), fallback = 0f)
        val safePower = finiteFloat(power, fallback = 1.6f).coerceAtLeast(0f)
        return local.toDouble().pow(safePower.toDouble()).toFloat()
    }

    private fun highlightWeight(progress: Float, start: Float, peak: Float, end: Float, falloffPower: Float): Float {
        val safeProgress = finiteUnit(progress, fallback = 0f)
        val safeStart = finiteUnit(start, fallback = TrackRenderTuning().highlightStart)
        val safeEnd = finiteUnit(end, fallback = TrackRenderTuning().highlightEnd).coerceAtLeast(safeStart)
        val safePeak = finiteUnit(peak, fallback = TrackRenderTuning().highlightPeak).coerceIn(safeStart, safeEnd)
        val safeFalloffPower = finiteFloat(falloffPower, fallback = TrackRenderTuning().highlightFalloffPower)
            .coerceAtLeast(0f)
        if (safeProgress < safeStart || safeProgress > safeEnd) return 0f
        return if (safeProgress <= safePeak) {
            smoothStep(safeStart, safePeak, safeProgress)
        } else {
            val t = smoothStep(safePeak, safeEnd, safeProgress)
            finiteUnit(1f - t, fallback = 0f).toDouble().pow(safeFalloffPower.toDouble()).toFloat()
        }
    }

    private fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
        val safeEdge0 = finiteUnit(edge0, fallback = 0f)
        val safeEdge1 = finiteUnit(edge1, fallback = safeEdge0)
        val safeValue = finiteUnit(value, fallback = safeEdge0)
        if (abs(safeEdge1 - safeEdge0) <= 0.0001f) return if (safeValue >= safeEdge1) 1f else 0f
        val t = finiteUnit((safeValue - safeEdge0) / (safeEdge1 - safeEdge0), fallback = 0f)
        return t * t * (3f - 2f * t)
    }

    private fun alphaOf(argb: Long): Double = ((argb ushr 24) and 0xFF).toDouble() / 255.0

    private fun colorWithAlpha(argb: Long, alpha: Float): Int {
        return Color.argb(
            (finiteUnit(alpha, fallback = 0f) * 255f).toInt().coerceIn(0, 255),
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
