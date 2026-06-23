package com.mygolfswing.app.ui.golfcam.model

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 六步统一工作流共享状态（对齐 iOS GolfCamWorkflowState）。
 * 每步完成后写入对应字段；容器读取字段决定当前可跳转到哪步。
 */
class GolfCamWorkflowState {
    var videoUri: Uri? by mutableStateOf(null)
    var trimResult: TrimResult? by mutableStateOf(null)
    var impactFrame: ImpactFrameSelection? by mutableStateOf(null)
    var impactPosition: ImpactPositionSelection? by mutableStateOf(null)
    var landing: LandingOutput? by mutableStateOf(null)
    var flightCurve: FlightCurveSelection? by mutableStateOf(null)
    var trackStyle: TrackStyleSelection? by mutableStateOf(null)

    fun clearVideoOnly() {
        videoUri = null
        trimResult = null
        impactFrame = null
        impactPosition = null
        landing = null
        flightCurve = null
        trackStyle = null
    }

    fun clearAll() = clearVideoOnly()

    /** 当前已完成到哪步（决定断点续编时恢复到哪一步） */
    val completedStep: GolfCamStep
        get() = when {
            trackStyle != null -> GolfCamStep.TRACK_STYLE
            flightCurve != null -> GolfCamStep.FLIGHT_CURVE
            landing != null -> GolfCamStep.LANDING
            impactPosition != null -> GolfCamStep.IMPACT_POSITION
            impactFrame != null -> GolfCamStep.IMPACT_FRAME
            trimResult != null -> GolfCamStep.TRIM
            else -> GolfCamStep.TRIM
        }

    /** 是否已完成全部 6 步可进入预览 */
    val isComplete: Boolean get() = trackStyle != null

    /** 飞行曲线控制点 1（归一化坐标，用于 FlightCurveStepTool 显示） */
    val flightCurveControlPoint1: Pair<Float, Float>?
        get() = flightCurve?.controlPoint1?.let { it.x to it.y }

    /** 飞行曲线控制点 2（归一化坐标，用于 FlightCurveStepTool 显示） */
    val flightCurveControlPoint2: Pair<Float, Float>?
        get() = flightCurve?.controlPoint2?.let { it.x to it.y }
}

enum class GolfCamStep(
    val index: Int,
    val title: String,
    val shortTitle: String,
    val icon: ImageVector,
) {
    TRIM(0, "裁剪视频", "裁剪", Icons.Filled.ContentCut),
    IMPACT_FRAME(1, "击球帧", "击球帧", Icons.Filled.VideoFile),
    IMPACT_POSITION(2, "击球点", "击球点", Icons.Filled.GpsFixed),
    LANDING(3, "落点", "落点", Icons.Filled.Flag),
    FLIGHT_CURVE(4, "曲线", "曲线", Icons.Filled.ShowChart),
    TRACK_STYLE(5, "轨迹样式", "样式", Icons.Filled.Palette),
    ;

    val isFirst: Boolean get() = index == 0
    val isLast: Boolean get() = index == TRACK_STYLE.index
    val next: GolfCamStep? get() = values().getOrNull(index + 1)
    val prev: GolfCamStep? get() = values().getOrNull(index - 1)
}
