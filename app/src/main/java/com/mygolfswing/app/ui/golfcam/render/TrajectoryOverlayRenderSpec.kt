package com.mygolfswing.app.ui.golfcam.render

internal data class HeadGlowAlphas(
    val outer: Float,
    val body: Float,
    val core: Float,
)

internal object TrajectoryOverlayRenderSpec {
    fun headGlowAlphas(glowOpacity: Float): HeadGlowAlphas {
        val opacity = glowOpacity.coerceIn(0f, 1f)
        return HeadGlowAlphas(
            outer = 0.35f * opacity,
            body = 0.75f * opacity,
            core = 0.90f * opacity,
        )
    }
}
