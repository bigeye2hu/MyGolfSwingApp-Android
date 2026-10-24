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
    val isFlyingLeft: Boolean,
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
        val rawHeadProgress = if (style.activeTrajectoryAlgorithm == TrajectoryAlgorithmKind.GOLF_CAM_LIKE) {
            GolfCamSpeedConfig.trajectoryHeadProgress(
                elapsedSec = elapsedSec,
                totalDurationSec = timing.totalDurationSec,
                speedLevel = style.speedLevel,
                bezier = bezier,
                arcTable = arcTable,
                style = style,
            )
        } else {
            legacyProgressAtTime?.invoke(elapsedSec)
                ?: TrajectoryMotionModel(
                    bezier = bezier,
                    timing = timing,
                    params = TrajectoryMotionModel.parametersFromStyle(style),
                    arcTableOverride = arcTable,
                ).progressAtTime(elapsedSec)
        }
        val headProgress = rawHeadProgress.takeIf { it.isFinite() }?.toFloat()?.coerceIn(0f, 1f) ?: 0f

        if (headProgress <= 0f) return null

        val apexProgress = arcTable.apexProgress()
        val tailProgress = GolfCamSpeedConfig.visibleTailStartProgress(
            headProgress = headProgress.toDouble(),
            tailHalfLife = style.tailHalfLife,
            activeAlgorithm = style.activeTrajectoryAlgorithm,
            apexProgress = apexProgress.toDouble(),
        ).toFloat()
        val distance = if (style.showDistance) {
            if (style.activeTrajectoryAlgorithm == TrajectoryAlgorithmKind.GOLF_CAM_LIKE) {
                GolfCamSpeedConfig.trajectoryDisplayDistance(
                    totalDistance = style.totalDistance,
                    elapsedSec = elapsedSec,
                    totalDurationSec = timing.totalDurationSec,
                    speedLevel = style.speedLevel,
                    bezier = bezier,
                    arcTable = arcTable,
                    style = style,
                    frameRate = timing.fps,
                )
            } else {
                GolfCamSpeedConfig.displayDistance(
                    totalDistance = style.totalDistance,
                    elapsedSec = elapsedSec,
                    totalDurationSec = timing.totalDurationSec,
                    speedLevel = style.speedLevel,
                    frameRate = timing.fps,
                )
            }
        } else {
            null
        }

        val headPoint = bezier.pointAtArcProgress(headProgress, arcTable)
        val previousPoint = if (headProgress > 0.01f) {
            bezier.pointAtArcProgress((headProgress - 0.01f).coerceAtLeast(0f), arcTable)
        } else {
            null
        }

        return TrajectoryOverlayFrameSnapshot(
            headProgress = headProgress,
            tailProgress = tailProgress,
            apexProgress = apexProgress,
            headPoint = headPoint,
            isFlyingLeft = DistanceLabelRenderSpec.isFlyingLeft(
                currentX = headPoint.x,
                previousX = previousPoint?.x,
            ),
            distance = distance,
        )
    }
}
