package com.mygolfswing.app.ui.golfcam.render

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.golfcam.model.ArcLengthTable
import com.mygolfswing.app.ui.golfcam.model.BezierPath
import com.mygolfswing.app.ui.golfcam.model.GolfCamSpeedConfig
import com.mygolfswing.app.ui.golfcam.model.NormalizedPoint
import com.mygolfswing.app.ui.golfcam.model.TrackStyle
import com.mygolfswing.app.ui.golfcam.model.TrajectoryAlgorithmKind
import com.mygolfswing.app.ui.golfcam.model.TrajectoryMotionModel
import com.mygolfswing.app.ui.golfcam.model.TrajectoryTiming
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.sqrt

// ─── 轨迹动画层 Composable（对齐 iOS TrackAnimationLayerView）────────────────────

/**
 * 静态叠加层：显示已完成的轨迹全路径（用于预览/编辑步骤）。
 */
@Composable
fun TrajectoryStaticOverlay(
    bezier: BezierPath,
    style: TrackStyle,
    modifier: Modifier = Modifier,
) {
    val gradientColors = style.colorPreset.gradientColors.map { Color(it) }
    val baseWidth = 8f * (style.widthPct / 50.0).toFloat()
    val arcTable = remember(bezier) { ArcLengthTable(bezier) }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawTrajectoryPath(
            bezier = bezier,
            arcTable = arcTable,
            headProgress = 1f,
            tailProgress = 0f,
            gradientColors = gradientColors,
            baseStrokeWidth = baseWidth,
            style = style,
        )
        drawHeadGlow(
            bezier = bezier,
            arcTable = arcTable,
            headProgress = 1f,
            color = gradientColors.last(),
            radius = baseWidth * 2.5f,
            alphaMultiplier = style.glowOpacity.toFloat().coerceIn(0f, 1f),
        )
    }
}

/**
 * 动画叠加层：球头按照 V5 速度曲线沿贝塞尔路径运动，尾迹按半衰期消退。
 *
 * @param bezier        归一化贝塞尔路径
 * @param timing        击球帧到落点的时间区间
 * @param style         轨迹样式（颜色/宽度/速度等）
 * @param playing       是否正在播放
 * @param elapsedMs     当前播放位置（相对 impactTimeMs 的偏移）
 */
@Composable
fun TrajectoryAnimationOverlay(
    bezier: BezierPath,
    timing: TrajectoryTiming,
    style: TrackStyle,
    playing: Boolean,
    elapsedMs: Long,
    modifier: Modifier = Modifier,
) {
    val model = remember(bezier, timing, style) {
        TrajectoryMotionModel(
            bezier = bezier,
            timing = timing,
            params = TrajectoryMotionModel.parametersFromStyle(style),
        )
    }

    val gradientColors = style.colorPreset.gradientColors.map { Color(it) }
    val baseWidth = 8f * (style.widthPct / 50.0).toFloat()
    val totalDurSec = timing.totalDurationSec
    val arcTable = remember(bezier) { ArcLengthTable(bezier) }

    val elapsedSec = elapsedMs / 1000.0
    val headProgress = if (style.activeTrajectoryAlgorithm == TrajectoryAlgorithmKind.GOLF_CAM_LIKE) {
        GolfCamSpeedConfig.progress(elapsedSec / totalDurSec, style.speedLevel)
    } else {
        model.progressAtTime(elapsedSec)
    }.toFloat().coerceIn(0f, 1f)

    val tailProgress = remember(headProgress, style.tailHalfLife, totalDurSec) {
        // tailHalfLife 范围 5~100，映射为占总时长的百分比
        val halfLifeProgress = (style.tailHalfLife / 100.0 / totalDurSec).toFloat()
        max(0f, headProgress - halfLifeProgress * 3f)
    }

    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {
        drawTrajectoryPath(
            bezier = bezier,
            arcTable = arcTable,
            headProgress = headProgress,
            tailProgress = tailProgress,
            gradientColors = gradientColors,
            baseStrokeWidth = baseWidth,
            style = style,
        )
        if (headProgress > 0f && headProgress < 1f) {
            drawHeadGlow(
                bezier = bezier,
                arcTable = arcTable,
                headProgress = headProgress,
                color = gradientColors.last(),
                radius = baseWidth * 2.5f,
                alphaMultiplier = style.glowOpacity.toFloat().coerceIn(0f, 1f),
            )
        }
        if (style.showDistance && headProgress > 0f) {
            val displayDist = GolfCamSpeedConfig.displayDistance(
                totalDistance = style.totalDistance,
                elapsedSec = elapsedMs / 1000.0,
                totalDurationSec = totalDurSec,
                speedLevel = style.speedLevel,
            )
            drawDistanceLabel(
                bezier = bezier,
                arcTable = arcTable,
                headProgress = headProgress,
                distance = displayDist,
                unit = style.distanceUnit.abbreviation,
                textMeasurer = textMeasurer,
            )
        }
    }
}

// ─── 内部绘制函数 ───────────────────────────────────────────────────────────────

private fun DrawScope.drawTrajectoryPath(
    bezier: BezierPath,
    arcTable: ArcLengthTable,
    headProgress: Float,
    tailProgress: Float,
    gradientColors: List<Color>,
    baseStrokeWidth: Float,
    style: TrackStyle,
    steps: Int = 120,
) {
    if (headProgress <= 0f) return
    val tuning = style.renderTuning
    if (!tuning.showBodyBand && !tuning.showHighlightCore && !tuning.showOuterGlow) return

    val effectiveColors = if (gradientColors.size < 2) {
        listOf(gradientColors.firstOrNull() ?: Color.White, Color.White)
    } else gradientColors
    val bandAlpha = style.trackBandAlpha()
    val taperPower = style.effectiveTaperPower()

    fun drawSegmentPass(widthMultiplier: Float, alphaFor: (tailFade: Float, globalProgress: Float) -> Float) {
        for (i in 0 until steps) {
            val t0 = tailProgress + (headProgress - tailProgress) * (i.toFloat() / steps)
            val t1 = tailProgress + (headProgress - tailProgress) * ((i + 1).toFloat() / steps)
            if (t1 <= tailProgress || t0 >= headProgress) continue

            val localProgress = (t0 - tailProgress) / max(headProgress - tailProgress, 1e-6f)
            val colorProgress = t0.coerceIn(0f, 1f)
            val taper = (1f - colorProgress).toDouble().pow(taperPower.toDouble()).toFloat().coerceIn(0.05f, 1f)
            val strokeWidth = baseStrokeWidth * taper * widthMultiplier

            val colorIdx = (colorProgress * (effectiveColors.size - 1)).toInt().coerceIn(0, effectiveColors.size - 2)
            val colorFrac = colorProgress * (effectiveColors.size - 1) - colorIdx
            val segColor = lerpColor(effectiveColors[colorIdx], effectiveColors[colorIdx + 1], colorFrac)
            val tailFade = localProgress.coerceIn(0f, 1f)
            val alpha = alphaFor(tailFade, colorProgress).coerceIn(0f, 1f)
            if (alpha <= 0.001f) continue

            val p0 = bezier.pointAtArcProgress(t0, arcTable)
            val p1 = bezier.pointAtArcProgress(t1, arcTable)
            drawLine(
                color = segColor.copy(alpha = alpha),
                start = Offset(p0.x * size.width, p0.y * size.height),
                end = Offset(p1.x * size.width, p1.y * size.height),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Butt,
            )
        }
    }

    if (tuning.showOuterGlow) {
        val glowAlpha = (style.colorAlpha * style.glowOpacity * tuning.outerGlowMultiplier)
            .toFloat()
            .coerceIn(0f, 0.70f)
        val glowWidth = 1.35f + tuning.glowBlurMultiplier.coerceIn(0.25f, 1.20f)
        drawSegmentPass(widthMultiplier = glowWidth) { tailFade, _ ->
            glowAlpha * (0.10f + 0.30f * tailFade.toDouble().pow(1.55).toFloat())
        }
    }

    if (tuning.showBodyBand) {
        drawSegmentPass(widthMultiplier = 1f) { tailFade, _ ->
            bandAlpha * (0.02f + 0.20f * tailFade.toDouble().pow(1.55).toFloat())
        }
        drawSegmentPass(widthMultiplier = 1f) { tailFade, _ ->
            bandAlpha * (tuning.bodyAlphaBase + tuning.bodyAlphaScale * tailFade.toDouble().pow(1.60).toFloat())
        }
    }

    if (tuning.showHighlightCore) {
        drawSegmentPass(widthMultiplier = tuning.highlightWidthRatio.coerceIn(0.30f, 0.85f)) { tailFade, progress ->
            val weight = highlightWeight(progress, tuning.highlightStart, tuning.highlightPeak, tuning.highlightEnd, tuning.highlightFalloffPower)
            if (weight <= 0.001f || tailFade <= 0.001f) {
                0f
            } else {
                minOf(
                    tuning.highlightAlphaMax,
                    bandAlpha * tuning.highlightStrength * (0.14f + 0.62f * weight),
                ) * weight * tailFade.toDouble().pow(1.60).toFloat()
            }
        }
    }
}

private fun DrawScope.drawHeadGlow(
    bezier: BezierPath,
    arcTable: ArcLengthTable,
    headProgress: Float,
    color: Color,
    radius: Float,
    alphaMultiplier: Float,
) {
    val pt = bezier.pointAtArcProgress(headProgress, arcTable)
    val center = Offset(pt.x * size.width, pt.y * size.height)

    drawCircle(color = color.copy(alpha = 0.35f * alphaMultiplier), radius = radius * 1.8f, center = center)
    drawCircle(color = color.copy(alpha = 0.75f * alphaMultiplier), radius = radius, center = center)
    drawCircle(color = Color.White.copy(alpha = 0.9f * alphaMultiplier), radius = radius * 0.4f, center = center)
}

private fun DrawScope.drawDistanceLabel(
    bezier: BezierPath,
    arcTable: ArcLengthTable,
    headProgress: Float,
    distance: Int,
    unit: String,
    textMeasurer: TextMeasurer,
) {
    if (distance <= 0) return
    val pt = bezier.pointAtArcProgress(headProgress, arcTable)
    val center = Offset(pt.x * size.width, pt.y * size.height)

    val text = "$distance$unit"
    val style = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
    val measured = textMeasurer.measure(text, style)
    val offsetX = center.x - measured.size.width / 2f
    val offsetY = center.y - measured.size.height - 16f

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.45f),
        topLeft = Offset(offsetX - 6f, offsetY - 4f),
        size = androidx.compose.ui.geometry.Size(
            (measured.size.width + 12f).toFloat(),
            (measured.size.height + 8f).toFloat(),
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f),
    )
    drawText(measured, topLeft = Offset(offsetX, offsetY))
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val tc = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * tc,
        green = a.green + (b.green - a.green) * tc,
        blue = a.blue + (b.blue - a.blue) * tc,
        alpha = a.alpha + (b.alpha - a.alpha) * tc,
    )
}

private fun TrackStyle.trackBandAlpha(): Float =
    (colorAlpha * colorPreset.bandAlpha * renderTuning.bandAlphaMultiplier).toFloat().coerceIn(0f, 1f)

private fun TrackStyle.effectiveTaperPower(): Float =
    renderTuning.widthTaperPower.takeIf { it > 0f } ?: taperPower

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
    if (kotlin.math.abs(edge1 - edge0) <= 0.0001f) return if (value >= edge1) 1f else 0f
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun Double.pow(exp: Double): Double = Math.pow(this, exp)
