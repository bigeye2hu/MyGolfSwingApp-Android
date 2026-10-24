package com.mygolfswing.app.ui.player

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import kotlin.math.abs

/**
 * 回看 / 逐帧拖动时的 seek。对齐 iOS `CoachFrameSeekCoordinator`：
 * 一次只解码一帧，这一帧要画出来才跟下一帧，松手后精确落下。
 * 页面不要自己再写一套 seek。
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class SmoothFrameSeeker(
    private val player: ExoPlayer,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var active = false
    private var listenerAttached = false
    private var inFlight = false
    private var frameAccepted = false
    private var pendingMs: Long? = null
    private var lastRequestedMs: Long? = null
    private var inFlightTargetMs = Long.MIN_VALUE
    private var positionBeforeSeekMs = Long.MIN_VALUE
    private var seekStartedAtMs = 0L
    private var flightToken = 0
    private var fallback: Runnable? = null
    private val frameListener = VideoFrameMetadataListener { presentationTimeUs, _, _, _ ->
        acceptPresentedFrame(presentationTimeUs / 1_000L)
    }

    val isScrubbing: Boolean get() = active

    fun begin(toleranceUs: Long) {
        if (!listenerAttached) {
            listenerAttached = true
            player.setVideoFrameMetadataListener(frameListener)
            player.setForegroundMode(true)
        }
        active = true
        inFlight = false
        frameAccepted = false
        pendingMs = null
        lastRequestedMs = null
        inFlightTargetMs = Long.MIN_VALUE
        positionBeforeSeekMs = Long.MIN_VALUE
        flightToken += 1
        handler.removeCallbacksAndMessages(null)
        fallback = null
        val tolerance = (toleranceUs.coerceAtLeast(1L) * 3 / 4).coerceAtLeast(IOS_MIN_INTERACTIVE_TOLERANCE_US)
        player.pause()
        player.setSeekParameters(SeekParameters(tolerance, tolerance))
    }

    fun moveTo(targetMs: Long) {
        val bounded = targetMs.coerceAtLeast(0L)
        lastRequestedMs = bounded
        if (!active) {
            player.seekTo(bounded)
            return
        }
        pendingMs = bounded
        if (!inFlight) dispatchPending()
    }

    fun end() {
        val landing = lastRequestedMs
        stopInteraction()
        player.setSeekParameters(SeekParameters.EXACT)
        if (landing != null) player.seekTo(landing)
    }

    fun dispose() {
        lastRequestedMs = null
        stopInteraction()
        if (listenerAttached) {
            listenerAttached = false
            player.clearVideoFrameMetadataListener(frameListener)
            player.setForegroundMode(false)
        }
    }

    private fun stopInteraction() {
        active = false
        inFlight = false
        frameAccepted = false
        pendingMs = null
        inFlightTargetMs = Long.MIN_VALUE
        flightToken += 1
        handler.removeCallbacksAndMessages(null)
        fallback = null
    }

    private fun acceptPresentedFrame(presentedMs: Long) {
        handler.post {
            if (!active || !inFlight || frameAccepted) return@post
            if (isFrameStillOnScreen(presentedMs)) return@post
            frameAccepted = true
            clearFallback()
            val token = flightToken
            handler.postDelayed({
                if (token != flightToken || !active) return@postDelayed
                finishFlight()
            }, FRAME_HOLD_MS)
        }
    }

    private fun isFrameStillOnScreen(presentedMs: Long): Boolean {
        val before = positionBeforeSeekMs
        val target = inFlightTargetMs
        if (before == Long.MIN_VALUE || target == Long.MIN_VALUE) return false
        val ageMs = SystemClock.uptimeMillis() - seekStartedAtMs
        return ageMs < STALE_FRAME_WINDOW_MS &&
            abs(presentedMs - before) <= 12L &&
            abs(target - before) > 40L
    }

    private fun finishFlight() {
        if (!active || !inFlight) return
        inFlight = false
        frameAccepted = false
        dispatchPending()
    }

    private fun dispatchPending() {
        val targetMs = pendingMs ?: return
        if (abs(targetMs - player.currentPosition) <= 8L) {
            pendingMs = null
            return
        }
        pendingMs = null
        inFlight = true
        frameAccepted = false
        positionBeforeSeekMs = player.currentPosition.coerceAtLeast(0L)
        inFlightTargetMs = targetMs
        seekStartedAtMs = SystemClock.uptimeMillis()
        val token = ++flightToken
        player.seekTo(targetMs)
        clearFallback()
        val runnable = Runnable {
            if (!active || token != flightToken || frameAccepted) return@Runnable
            finishFlight()
        }
        fallback = runnable
        handler.postDelayed(runnable, SEEK_FALLBACK_MS)
    }

    private fun clearFallback() {
        fallback?.let(handler::removeCallbacks)
        fallback = null
    }

    private companion object {
        const val IOS_MIN_INTERACTIVE_TOLERANCE_US = 25_000L
        const val STALE_FRAME_WINDOW_MS = 24L
        const val FRAME_HOLD_MS = 36L
        const val SEEK_FALLBACK_MS = 720L
    }
}
