package com.mygolfswing.app.ui.golfcam.render

import com.mygolfswing.app.ui.golfcam.model.GolfCamTrailGeometry
import com.mygolfswing.app.ui.golfcam.model.TrackStyle

internal data class HeadGlowAlphas(
    val outer: Float,
    val body: Float,
    val core: Float,
)

internal object TrajectoryOverlayRenderSpec {
    fun shouldDrawFlyingHead(headProgress: Float): Boolean {
        return headProgress.isFinite() && headProgress >= 0f && headProgress < 1f
    }

    fun headGlowRadius(
        style: TrackStyle,
        videoHeight: Float,
        headProgress: Float,
        apexProgress: Float,
    ): Float {
        return GolfCamTrailGeometry.strokeWidth(
            widthPct = style.widthPct,
            videoHeight = videoHeight,
            globalProgress = headProgress,
            apexProgress = apexProgress,
            tuning = style.renderTuning,
        ) * 1.25f
    }

    fun headGlowAlphas(glowOpacity: Float): HeadGlowAlphas {
        val opacity = (glowOpacity.takeIf { it.isFinite() } ?: 0f).coerceIn(0f, 1f)
        return HeadGlowAlphas(
            outer = 0.35f * opacity,
            body = 0.75f * opacity,
            core = 0.9f * opacity,
        )
    }

    fun alphaByte(alpha: Float): Int {
        return ((alpha.takeIf { it.isFinite() } ?: 0f).coerceIn(0f, 1f) * 255f)
            .toInt()
            .coerceIn(0, 255)
    }
}
