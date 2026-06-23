package com.mygolfswing.app.ui.golfcam.model

object GolfCamFrameTimeline {
    fun safeFrameRate(frameRate: Double): Double = frameRate.coerceIn(1.0, 120.0)

    fun frameIndexAt(relativeSeconds: Double, frameRate: Double): Int {
        return (relativeSeconds.coerceAtLeast(0.0) * safeFrameRate(frameRate)).toInt().coerceAtLeast(0)
    }

    fun timeMsForFrame(trimStartMs: Long, frameIndex: Int, frameRate: Double): Long {
        val frameSec = frameIndex.coerceAtLeast(0) / safeFrameRate(frameRate)
        return trimStartMs + (frameSec * 1000.0).toLong()
    }

    fun totalFrames(durationMs: Long, frameRate: Double): Int {
        val durationSec = durationMs.coerceAtLeast(0L) / 1000.0
        return (durationSec * safeFrameRate(frameRate)).toInt().coerceAtLeast(1)
    }
}
