package com.mygolfswing.app.ui.golfcam.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.golfcam.model.ArcLengthTable
import com.mygolfswing.app.ui.golfcam.model.BezierPath
import com.mygolfswing.app.ui.golfcam.model.NormalizedPoint
import com.mygolfswing.app.ui.golfcam.model.TrackStyle
import com.mygolfswing.app.ui.golfcam.model.TrajectoryAlgorithmKind
import com.mygolfswing.app.ui.golfcam.model.TrajectoryMotionModel
import com.mygolfswing.app.ui.golfcam.model.TrajectoryTiming

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
    val arcTable = remember(bezier) { ArcLengthTable(bezier) }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawTrajectoryPath(
            bezier = bezier,
            arcTable = arcTable,
            headProgress = 1f,
            tailProgress = 0f,
            style = style,
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
    val legacyProgressAtTime = remember(bezier, timing, style) {
        if (style.activeTrajectoryAlgorithm == TrajectoryAlgorithmKind.GOLF_CAM_LIKE) {
            null
        } else {
            val model = TrajectoryMotionModel(
                bezier = bezier,
                timing = timing,
                params = TrajectoryMotionModel.parametersFromStyle(style),
            )
            model::progressAtTime
        }
    }

    val arcTable = remember(bezier) { ArcLengthTable(bezier) }

    val elapsedSec = elapsedMs / 1000.0
    val snapshot = TrajectoryOverlayFrameSampler.sample(
        bezier = bezier,
        arcTable = arcTable,
        timing = timing,
        style = style,
        elapsedSec = elapsedSec,
        legacyProgressAtTime = legacyProgressAtTime,
    )

    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {
        snapshot?.let { frame ->
            drawTrajectoryPath(
                bezier = bezier,
                arcTable = arcTable,
                headProgress = frame.headProgress,
                tailProgress = frame.tailProgress,
                style = style,
            )
            if (TrajectoryOverlayRenderSpec.shouldDrawFlyingHead(frame.headProgress)) {
                drawHeadGlow(
                    headPoint = frame.headPoint,
                    color = Color(style.primaryColorArgb()),
                    radius = TrajectoryOverlayRenderSpec.headGlowRadius(
                        style = style,
                        videoHeight = size.height,
                        headProgress = frame.headProgress,
                        apexProgress = frame.apexProgress,
                    ),
                    alphaMultiplier = style.glowOpacity.toFloat(),
                )
            }
            frame.distance?.let { displayDist ->
                drawDistanceLabel(
                    headPoint = frame.headPoint,
                    isFlyingLeft = frame.isFlyingLeft,
                    distance = displayDist,
                    unit = style.distanceUnit.abbreviation,
                    textMeasurer = textMeasurer,
                )
            }
        }
    }
}

// ─── 内部绘制函数 ───────────────────────────────────────────────────────────────

private fun DrawScope.drawTrajectoryPath(
    bezier: BezierPath,
    arcTable: ArcLengthTable,
    headProgress: Float,
    tailProgress: Float,
    style: TrackStyle,
) {
    if (headProgress <= 0f) return
    drawIntoCanvas { canvas ->
        TrajectoryRibbonRenderer.draw(
            canvas = canvas.nativeCanvas,
            bezier = bezier,
            arcTable = arcTable,
            headProgress = headProgress,
            tailProgress = tailProgress,
            style = style,
            width = size.width,
            height = size.height,
        )
    }
}

private fun DrawScope.drawHeadGlow(
    headPoint: NormalizedPoint,
    color: Color,
    radius: Float,
    alphaMultiplier: Float,
) {
    val center = Offset(headPoint.x * size.width, headPoint.y * size.height)
    val alphas = TrajectoryOverlayRenderSpec.headGlowAlphas(alphaMultiplier)

    drawCircle(color = color.copy(alpha = alphas.outer), radius = radius * 1.8f, center = center)
    drawCircle(color = color.copy(alpha = alphas.body), radius = radius, center = center)
    drawCircle(color = Color.White.copy(alpha = alphas.core), radius = radius * 0.4f, center = center)
}

private fun DrawScope.drawDistanceLabel(
    headPoint: NormalizedPoint,
    isFlyingLeft: Boolean,
    distance: Int,
    unit: String,
    textMeasurer: TextMeasurer,
) {
    if (distance <= 0) return
    val center = Offset(headPoint.x * size.width, headPoint.y * size.height)
    val shadow = Shadow(
        color = Color.Black.copy(alpha = DistanceLabelRenderSpec.SHADOW_ALPHA),
        offset = Offset(DistanceLabelRenderSpec.shadowOffsetPx(size.height), DistanceLabelRenderSpec.shadowOffsetPx(size.height)),
        blurRadius = DistanceLabelRenderSpec.shadowBlurRadiusPx(size.height),
    )
    val unitStyle = TextStyle(
        fontSize = DistanceLabelRenderSpec.unitFontPx(size.height).toSp(),
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        shadow = shadow,
    )
    val valueStyle = TextStyle(
        fontSize = DistanceLabelRenderSpec.valueFontPx(size.height).toSp(),
        fontWeight = FontWeight.Normal,
        color = Color.White,
        shadow = shadow,
    )
    val unitMeasured = textMeasurer.measure(unit, unitStyle)
    val valueText = distance.toString()
    val valueMeasured = textMeasurer.measure(valueText, valueStyle)
    val layout = DistanceLabelRenderSpec.layout(
        headX = center.x,
        headY = center.y,
        videoHeight = size.height,
        isFlyingLeft = isFlyingLeft,
        metrics = DistanceLabelTextMetrics(
            unitWidth = unitMeasured.size.width.toFloat(),
            unitHeight = unitMeasured.size.height.toFloat(),
            valueWidth = valueMeasured.size.width.toFloat(),
            valueHeight = valueMeasured.size.height.toFloat(),
        ),
    )
    drawText(unitMeasured, topLeft = Offset(layout.unitLeft, layout.unitTop))
    drawText(valueMeasured, topLeft = Offset(layout.valueLeft, layout.valueTop))
}
