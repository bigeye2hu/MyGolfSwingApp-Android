package com.mygolfswing.app.ui.golfcam

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.mygolfswing.app.ui.golfcam.model.GolfCamFrameTimeline
import com.mygolfswing.app.ui.golfcam.model.GolfCamVideoConfig

internal fun visualVideoDimensions(rawWidth: Int, rawHeight: Int, rotationDegrees: Int): Pair<Int, Int> {
    val width = rawWidth.coerceAtLeast(1)
    val height = rawHeight.coerceAtLeast(1)
    val rotation = normalizedVideoRotation(rotationDegrees)
    return if (rotation == 90 || rotation == 270) {
        height to width
    } else {
        width to height
    }
}

internal fun normalizedVideoRotation(rotationDegrees: Int): Int {
    val normalized = ((rotationDegrees % 360) + 360) % 360
    return if (normalized == 90 || normalized == 180 || normalized == 270) {
        normalized
    } else {
        0
    }
}

internal fun golfCamVideoConfigFromMetadata(
    rawDurationMs: String?,
    rawWidth: String?,
    rawHeight: String?,
    rawRotationDegrees: String?,
    frameRate: Double,
): GolfCamVideoConfig {
    val rotationDegrees = normalizedVideoRotation(rawRotationDegrees?.toIntOrNull() ?: 0)
    val visualSize = visualVideoDimensions(
        rawWidth = rawWidth?.toIntOrNull()?.takeIf { it > 0 } ?: 1,
        rawHeight = rawHeight?.toIntOrNull()?.takeIf { it > 0 } ?: 1,
        rotationDegrees = rotationDegrees,
    )
    return GolfCamVideoConfig(
        durationMs = (rawDurationMs?.toLongOrNull() ?: 1L).coerceAtLeast(1L),
        frameRate = GolfCamFrameTimeline.safeFrameRate(frameRate),
        visualWidth = visualSize.first,
        visualHeight = visualSize.second,
        rotationDegrees = rotationDegrees,
    )
}

internal fun detectGolfCamVideoConfig(context: Context, videoUri: Uri): GolfCamVideoConfig {
    var durationMs = 1L
    var visualWidth = 1
    var visualHeight = 1
    var rotationDegrees = 0

    runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val config = golfCamVideoConfigFromMetadata(
                rawDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION),
                rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH),
                rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT),
                rawRotationDegrees = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION),
                frameRate = 30.0,
            )
            durationMs = config.durationMs
            visualWidth = config.visualWidth
            visualHeight = config.visualHeight
            rotationDegrees = config.rotationDegrees
        } finally {
            retriever.release()
        }
    }

    val frameRate = detectVideoFrameRate(context, videoUri) ?: 30.0
    return GolfCamVideoConfig(
        durationMs = durationMs,
        frameRate = GolfCamFrameTimeline.safeFrameRate(frameRate),
        visualWidth = visualWidth,
        visualHeight = visualHeight,
        rotationDegrees = rotationDegrees,
    )
}

private fun detectVideoFrameRate(context: Context, videoUri: Uri): Double? {
    val extractor = MediaExtractor()
    return try {
        extractor.setDataSource(context, videoUri, null)
        for (trackIndex in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/")) {
                val containerFrameRate = if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                    format.getInteger(MediaFormat.KEY_FRAME_RATE).coerceIn(1, 120).toDouble()
                } else {
                    null
                }
                extractor.selectTrack(trackIndex)
                return estimateFrameRateFromSamples(extractor) ?: containerFrameRate
            }
        }
        null
    } catch (_: Exception) {
        null
    } finally {
        extractor.release()
    }
}

internal fun estimateFrameRateFromPresentationTimes(sampleTimesUs: List<Long>): Double? {
    val deltas = sampleTimesUs
        .distinct()
        .sorted()
        .zipWithNext { previous, next -> next - previous }
        .filter { it > 0L }
        .sorted()
    if (deltas.isEmpty()) return null
    val middle = deltas.size / 2
    val medianDeltaUs = if (deltas.size % 2 == 0) {
        deltas[middle - 1] / 2.0 + deltas[middle] / 2.0
    } else {
        deltas[middle].toDouble()
    }.takeIf { it > 0.0 } ?: return null
    return (1_000_000.0 / medianDeltaUs).takeIf { it in 1.0..120.0 }
}

private fun estimateFrameRateFromSamples(extractor: MediaExtractor, sampleLimit: Int = 90): Double? {
    val sampleTimes = buildList {
        while (size < sampleLimit) {
            val sampleTime = extractor.sampleTime
            if (sampleTime < 0L) break
            add(sampleTime)
            if (!extractor.advance()) break
        }
    }
    return estimateFrameRateFromPresentationTimes(sampleTimes)
}
