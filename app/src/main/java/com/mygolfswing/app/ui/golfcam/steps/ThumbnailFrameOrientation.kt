package com.mygolfswing.app.ui.golfcam.steps

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import com.mygolfswing.app.ui.golfcam.normalizedVideoRotation

internal data class ThumbnailVideoMetadata(
    val rawWidth: Int,
    val rawHeight: Int,
    val rotationDegrees: Int,
)

internal fun MediaMetadataRetriever.thumbnailVideoMetadata(): ThumbnailVideoMetadata {
    return ThumbnailVideoMetadata(
        rawWidth = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
        rawHeight = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0,
        rotationDegrees = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0,
    )
}

internal fun thumbnailRotationDegrees(
    metadataRotationDegrees: Int,
    rawWidth: Int,
    rawHeight: Int,
    frameWidth: Int,
    frameHeight: Int,
): Int {
    val rotation = normalizedVideoRotation(metadataRotationDegrees)
    val visualWidth = if (rotation == 90 || rotation == 270) rawHeight else rawWidth
    val visualHeight = if (rotation == 90 || rotation == 270) rawWidth else rawHeight
    val alreadyVisual = rawWidth > 0 &&
        rawHeight > 0 &&
        (rotation == 90 || rotation == 270) &&
        frameWidth > 0 &&
        frameHeight > 0 &&
        visualWidth > 0 &&
        visualHeight > 0 &&
        (frameHeight > frameWidth) == (visualHeight > visualWidth)
    return if (alreadyVisual) 0 else rotation
}

internal fun Bitmap.rotatedToVisualOrientation(metadata: ThumbnailVideoMetadata): Bitmap {
    val rotation = thumbnailRotationDegrees(
        metadataRotationDegrees = metadata.rotationDegrees,
        rawWidth = metadata.rawWidth,
        rawHeight = metadata.rawHeight,
        frameWidth = width,
        frameHeight = height,
    )
    if (rotation == 0) return this
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
