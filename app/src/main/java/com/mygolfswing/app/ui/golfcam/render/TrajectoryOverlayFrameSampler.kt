package com.mygolfswing.app.ui.golfcam.render

import com.mygolfswing.app.ui.golfcam.model.ArcLengthTable
import com.mygolfswing.app.ui.golfcam.model.BezierPath
import com.mygolfswing.app.ui.golfcam.model.GolfCamSpeedConfig
import com.mygolfswing.app.ui.golfcam.model.NormalizedPoint
import com.mygolfswing.app.ui.golfcam.model.TrackStyle
import com.mygolfswing.app.ui.golfcam.model.TrajectoryAlgorithmKind
import com.mygolfswing.app.ui.golfcam.model.TrajectoryMotionModel
import com.mygolfswing.app.ui.golfcam.model.TrajectoryTiming

internal data class TrajectoryOverlayFrameSnapshot(
    val headProgress: Float,
    val tailProgress: Float,
    val apexProgress: Float,
    val headPoint: NormalizedPoint,
    val distance: Int?,
)

internal object TrajectoryOverlayFrameSampler {
    fun sample(
        bezier: BezierPath,
        arcTable: ArcLengthTable,
        timing: TrajectoryTiming,
        style: TrackStyle,
        elapsedSec: Double,
        legacyProgressAtTime: ((Double) -> Double)? = null,
    ): TrajectoryOverlayFrameSnapshot? {
        val headProgress = if (style.activeTrajectoryAlgorithm == TrajectoryAlgorithmKind.GOLF_CAM_LIKE) {
            GolfCamSpeedConfig.progress(elapsedSec / timing.totalDurationSec, style.speedLevel)
        } else {
            legacyProgressAtTime?.invoke(elapsedSec)
                ?: TrajectoryMotionModel(
                    bezier = bezier,
                    timing = timing,
                    params = TrajectoryMotionModel.parametersFromStyle(style),
                ).progressAtTime(elapsedSec)
        }.toFloat().coerceIn(0f, 1f)

        if (headProgress <= 0f) return null

        val apexProgress = arcTable.apexProgress()
        val tailProgress = GolfCamSpeedConfig.visibleTailStartProgress(
            headProgress = headProgress.toDouble(),
            tailHalfLife = style.tailHalfLife,
            activeAlgorithm = style.activeTrajectoryAlgorithm,
            apexProgress = apexProgress.toDouble(),
        ).toFloat()
        val distance = if (style.showDistance) {
            GolfCamSpeedConfig.displayDistance(
                totalDistance = style.totalDistance,
                elapsedSec = elapsedSec,
                totalDurationSec = timing.totalDurationSec,
                speedLevel = style.speedLevel,
                frameRate = timing.fps,
            )
        } else {
            null
        }

        return TrajectoryOverlayFrameSnapshot(
            headProgress = headProgress,
            tailProgress = tailProgress,
            apexProgress = apexProgress,
            headPoint = bezier.pointAtArcProgress(headProgress, arcTable),
            distance = distance,
        )
    }
}
