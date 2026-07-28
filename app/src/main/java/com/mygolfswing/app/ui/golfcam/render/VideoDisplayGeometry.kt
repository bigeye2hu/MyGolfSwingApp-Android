package com.mygolfswing.app.ui.golfcam.render

internal data class VideoDisplaySize(
    val width: Float,
    val height: Float,
)

internal object VideoDisplayGeometry {
    fun fit(containerWidth: Float, containerHeight: Float, videoAspectRatio: Float): VideoDisplaySize {
        fun finiteFloat(value: Float, fallback: Float): Float {
            return value.takeIf { it.isFinite() } ?: fallback
        }

        val safeWidth = finiteFloat(containerWidth, fallback = 1f).coerceAtLeast(1f)
        val safeHeight = finiteFloat(containerHeight, fallback = 1f).coerceAtLeast(1f)
        val aspect = finiteFloat(videoAspectRatio, fallback = 1f).coerceAtLeast(0.01f)
        val widthBasedHeight = safeWidth / aspect
        val heightBasedWidth = safeHeight * aspect
        return if (widthBasedHeight <= safeHeight) {
            VideoDisplaySize(width = safeWidth, height = widthBasedHeight)
        } else {
            VideoDisplaySize(width = heightBasedWidth, height = safeHeight)
        }
    }
}
