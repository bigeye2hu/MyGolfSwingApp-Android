package com.mygolfswing.app.ui.golfcam.model

import kotlin.math.ceil

object GolfCamFrameTimeline {
    private const val DEFAULT_SAFE_FRAME_RATE = 30.0

    fun safeFrameRate(frameRate: Double): Double {
        return if (!frameRate.isFinite()) {
            DEFAULT_SAFE_FRAME_RATE
        } else {
            frameRate.coerceIn(1.0, 120.0)
        }
    }

    fun frameIndexAt(relativeSeconds: Double, frameRate: Double): Int {
        val seconds = relativeSeconds.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        return boundedFrameCount(seconds * safeFrameRate(frameRate), minimum = 0)
    }

    fun timeMsForFrame(trimStartMs: Long, frameIndex: Int, frameRate: Double): Long {
        val frameSec = frameIndex.coerceAtLeast(0) / safeFrameRate(frameRate)
        return saturatingAdd(trimStartMs, boundedMilliseconds(frameSec * 1000.0))
    }

    fun timeMsAfter(startMs: Long, offsetMs: Long): Long {
        return saturatingAdd(startMs, offsetMs.coerceAtLeast(0L))
    }

    fun timeMsAfterSeconds(startMs: Long, offsetSeconds: Double): Long {
        val seconds = offsetSeconds.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        return timeMsAfter(startMs, boundedMilliseconds(seconds * 1000.0))
    }

    fun totalFrames(durationMs: Long, frameRate: Double): Int {
        val durationSec = durationMs.coerceAtLeast(0L) / 1000.0
        return boundedFrameCount(ceil(durationSec * safeFrameRate(frameRate)), minimum = 1)
    }

    fun totalFramesForRange(startMs: Long, endMs: Long, frameRate: Double): Int {
        return totalFrames(durationMsForRange(startMs, endMs), frameRate)
    }

    fun lastFrameIndexForRange(startMs: Long, endMs: Long, frameRate: Double): Int {
        return (totalFramesForRange(startMs, endMs, frameRate) - 1).coerceAtLeast(0)
    }

    fun lastFrameTimeMsForRange(startMs: Long, endMs: Long, frameRate: Double): Long {
        val safeEndExclusiveMs = endExclusiveMsAfterStart(startMs, endMs)
        val lastFrameTime = timeMsForFrame(
            trimStartMs = startMs,
            frameIndex = lastFrameIndexForRange(startMs, safeEndExclusiveMs, frameRate),
            frameRate = frameRate,
        )
        return lastFrameTime.coerceIn(startMs, (safeEndExclusiveMs - 1L).coerceAtLeast(startMs))
    }

    fun durationMsForRange(startMs: Long, endMs: Long): Long {
        if (endMs <= startMs) return 1L
        val duration = endMs - startMs
        return if (duration > 0L) duration else Long.MAX_VALUE
    }

    fun nonNegativeDeltaMs(startMs: Long, endMs: Long): Long {
        if (endMs <= startMs) return 0L
        val delta = endMs - startMs
        return if (delta >= 0L) delta else Long.MAX_VALUE
    }

    fun endExclusiveMsAfterStart(startMs: Long, endMs: Long): Long {
        if (endMs > startMs) return endMs
        return if (startMs == Long.MAX_VALUE) Long.MAX_VALUE else startMs + 1L
    }

    private fun boundedFrameCount(value: Double, minimum: Int): Int {
        return when {
            !value.isFinite() || value <= minimum.toDouble() -> minimum
            value >= Int.MAX_VALUE.toDouble() -> Int.MAX_VALUE
            else -> value.toInt().coerceAtLeast(minimum)
        }
    }

    private fun boundedMilliseconds(value: Double): Long {
        return when {
            !value.isFinite() || value <= 0.0 -> 0L
            value >= Long.MAX_VALUE.toDouble() -> Long.MAX_VALUE
            else -> value.toLong()
        }
    }

    private fun saturatingAdd(baseMs: Long, offsetMs: Long): Long {
        if (offsetMs <= 0L) return baseMs
        return if (baseMs > Long.MAX_VALUE - offsetMs) Long.MAX_VALUE else baseMs + offsetMs
    }

}
