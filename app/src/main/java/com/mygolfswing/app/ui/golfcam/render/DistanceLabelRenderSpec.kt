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
    const val DIRECTION_THRESHOLD_X = 0.0005f
    const val SHADOW_ALPHA = 0.8f

    private fun finiteFloat(value: Float, fallback: Float): Float {
        return value.takeIf { it.isFinite() } ?: fallback
    }

    fun scale(videoHeight: Float): Float = finiteFloat(videoHeight, fallback = 1f).coerceAtLeast(1f) / 1080f
    fun unitFontPx(videoHeight: Float): Float = 11f * scale(videoHeight)
    fun valueFontPx(videoHeight: Float): Float = 24f * scale(videoHeight)
    fun lineSpacingPx(videoHeight: Float): Float = 2f * scale(videoHeight)
    fun shadowOffsetPx(videoHeight: Float): Float = 1f * scale(videoHeight)
    fun shadowBlurRadiusPx(videoHeight: Float): Float = 3f * scale(videoHeight)

    fun isFlyingLeft(currentX: Float, previousX: Float?): Boolean {
        val current = currentX.takeIf { it.isFinite() } ?: return true
        val previous = previousX?.takeIf { it.isFinite() } ?: return true
        val dx = current - previous
        return if (kotlin.math.abs(dx) > DIRECTION_THRESHOLD_X) dx < 0f else true
    }

    fun layout(
        headX: Float,
        headY: Float,
        videoHeight: Float,
        isFlyingLeft: Boolean,
        metrics: DistanceLabelTextMetrics,
    ): DistanceLabelLayout {
        val safeHeight = finiteFloat(videoHeight, fallback = 1f).coerceAtLeast(1f)
        val safeHeadX = finiteFloat(headX, fallback = 0f)
        val safeHeadY = finiteFloat(headY, fallback = 0f)
        val safeMetrics = DistanceLabelTextMetrics(
            unitWidth = finiteFloat(metrics.unitWidth, fallback = 0f).coerceAtLeast(0f),
            unitHeight = finiteFloat(metrics.unitHeight, fallback = 0f).coerceAtLeast(0f),
            valueWidth = finiteFloat(metrics.valueWidth, fallback = 0f).coerceAtLeast(0f),
            valueHeight = finiteFloat(metrics.valueHeight, fallback = 0f).coerceAtLeast(0f),
        )
        val offset = safeHeight * OFFSET_RATIO
        val labelX = safeHeadX + if (isFlyingLeft) -offset else offset
        val labelY = safeHeadY - offset
        val lineSpacing = lineSpacingPx(safeHeight)
        val totalHeight = safeMetrics.unitHeight + lineSpacing + safeMetrics.valueHeight
        val unitLeft = labelX - safeMetrics.unitWidth / 2f
        val unitTop = labelY - totalHeight / 2f
        val valueLeft = labelX - safeMetrics.valueWidth / 2f
        val valueTop = unitTop + safeMetrics.unitHeight + lineSpacing
        return DistanceLabelLayout(
            unitLeft = unitLeft,
            unitTop = unitTop,
            valueLeft = valueLeft,
            valueTop = valueTop,
        )
    }
}
