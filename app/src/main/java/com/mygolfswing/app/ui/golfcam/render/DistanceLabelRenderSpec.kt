package com.mygolfswing.app.ui.golfcam.render

internal data class DistanceLabelTextMetrics(
    val unitWidth: Float,
    val unitHeight: Float,
    val valueWidth: Float,
    val valueHeight: Float,
)

internal data class DistanceLabelLayout(
    val unitLeft: Float,
    val unitTop: Float,
    val valueLeft: Float,
    val valueTop: Float,
)

internal object DistanceLabelRenderSpec {
    const val OFFSET_RATIO = 0.035f
    const val DIRECTION_THRESHOLD_X = 0.5f
    const val SHADOW_ALPHA = 0.8f

    fun scale(videoHeight: Float): Float = (videoHeight / 1080f).coerceAtLeast(1f)
    fun unitFontPx(videoHeight: Float): Float = 11f * scale(videoHeight)
    fun valueFontPx(videoHeight: Float): Float = 24f * scale(videoHeight)
    fun lineSpacingPx(videoHeight: Float): Float = 2f * scale(videoHeight)
    fun shadowOffsetPx(videoHeight: Float): Float = 1f * scale(videoHeight)

    fun isFlyingLeft(currentX: Float, previousX: Float?): Boolean {
        val previous = previousX ?: return true
        val dx = currentX - previous
        return if (kotlin.math.abs(dx) > DIRECTION_THRESHOLD_X) dx < 0f else true
    }

    fun layout(
        headX: Float,
        headY: Float,
        videoHeight: Float,
        isFlyingLeft: Boolean,
        metrics: DistanceLabelTextMetrics,
    ): DistanceLabelLayout {
        val offset = videoHeight * OFFSET_RATIO
        val labelX = headX + if (isFlyingLeft) -offset else offset
        val labelY = headY - offset
        val totalHeight = metrics.unitHeight + lineSpacingPx(videoHeight) + metrics.valueHeight
        val unitLeft = labelX - metrics.unitWidth / 2f
        val unitTop = labelY - totalHeight / 2f
        val valueLeft = labelX - metrics.valueWidth / 2f
        val valueTop = unitTop + metrics.unitHeight + lineSpacingPx(videoHeight)
        return DistanceLabelLayout(
            unitLeft = unitLeft,
            unitTop = unitTop,
            valueLeft = valueLeft,
            valueTop = valueTop,
        )
    }
}
