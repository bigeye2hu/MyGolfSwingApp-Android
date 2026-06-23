package com.mygolfswing.app.ui.golfcam.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import com.mygolfswing.app.ui.golfcam.model.GolfCamSpeedConfig
import com.mygolfswing.app.ui.golfcam.model.GolfCamTrailGeometry
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
        drawHeadGlow(
            bezier = bezier,
            arcTable = arcTable,
            headProgress = 1f,
            color = Color(style.primaryColorArgb()),
            radius = style.headGlowRadius(videoHeight = size.height, headProgress = 1f, apexProgress = arcTable.apexProgress()),
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

    val totalDurSec = timing.totalDurationSec
    val arcTable = remember(bezier) { ArcLengthTable(bezier) }

    val elapsedSec = elapsedMs / 1000.0
    val headProgress = if (style.activeTrajectoryAlgorithm == TrajectoryAlgorithmKind.GOLF_CAM_LIKE) {
        GolfCamSpeedConfig.progress(elapsedSec / totalDurSec, style.speedLevel)
    } else {
        model.progressAtTime(elapsedSec)
    }.toFloat().coerceIn(0f, 1f)

    val tailProgress = remember(headProgress, style.tailHalfLife, style.activeTrajectoryAlgorithm, arcTable) {
        GolfCamSpeedConfig.visibleTailStartProgress(
            headProgress = headProgress.toDouble(),
            tailHalfLife = style.tailHalfLife,
            activeAlgorithm = style.activeTrajectoryAlgorithm,
            apexProgress = arcTable.apexProgress().toDouble(),
        ).toFloat()
    }

    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {
        drawTrajectoryPath(
            bezier = bezier,
            arcTable = arcTable,
            headProgress = headProgress,
            tailProgress = tailProgress,
            style = style,
        )
        if (headProgress > 0f && headProgress < 1f) {
            drawHeadGlow(
                bezier = bezier,
                arcTable = arcTable,
                headProgress = headProgress,
                color = Color(style.primaryColorArgb()),
                radius = style.headGlowRadius(videoHeight = size.height, headProgress = headProgress, apexProgress = arcTable.apexProgress()),
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

private fun TrackStyle.headGlowRadius(videoHeight: Float, headProgress: Float, apexProgress: Float): Float =
    GolfCamTrailGeometry.strokeWidth(
        widthPct = widthPct,
        videoHeight = videoHeight,
        globalProgress = headProgress,
        apexProgress = apexProgress,
        tuning = renderTuning,
    ) * 1.25f
