package com.mygolfswing.app.ui.golfcam.render

internal data class VideoDisplaySize(
    val width: Float,
    val height: Float,
)

internal object VideoDisplayGeometry {
    fun fit(containerWidth: Float, containerHeight: Float, videoAspectRatio: Float): VideoDisplaySize {
        val safeWidth = containerWidth.coerceAtLeast(1f)
        val safeHeight = containerHeight.coerceAtLeast(1f)
        val aspect = videoAspectRatio.coerceAtLeast(0.01f)
        val widthBasedHeight = safeWidth / aspect
        val heightBasedWidth = safeHeight * aspect
        return if (widthBasedHeight <= safeHeight) {
            VideoDisplaySize(width = safeWidth, height = widthBasedHeight)
        } else {
            VideoDisplaySize(width = heightBasedWidth, height = safeHeight)
        }
    }
}
