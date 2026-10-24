package com.mygolfswing.app.ui.coach

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.mygolfswing.app.data.coach.V2CoachLibraryAsset
import com.mygolfswing.app.data.video.V2VideoThumbnailLoader
import com.mygolfswing.app.ui.designsystem.DSColors

/** Android counterpart of iOS `CoachMediaCover`: cover URL, video frame, then golf fallback. */
@Composable
internal fun V2CoachMediaCover(
    asset: V2CoachLibraryAsset,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onRemoteLoadFailed: (() -> Unit)? = null,
) {
    var fallbackFrame by remember(asset.assetId) { mutableStateOf<Bitmap?>(null) }
    var coverLoadFailed by remember(asset.assetId, asset.coverUrl) { mutableStateOf(false) }
    var reportedCoverFailure by remember(asset.assetId, asset.coverUrl) { mutableStateOf(false) }

    LaunchedEffect(asset.assetId, asset.coverUrl, asset.videoUrl, coverLoadFailed) {
        fallbackFrame = if (asset.coverUrl.isNullOrBlank() || coverLoadFailed) {
            V2VideoThumbnailLoader.loadFirstFrame(asset.videoUrl, allowRemote = true)
        } else {
            null
        }
    }

    Box(
        modifier.background(
            Brush.linearGradient(
                listOf(DSColors.deepGreen, DSColors.fairway.copy(alpha = 0.82f)),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.SportsGolf,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.78f),
            modifier = Modifier.fillMaxSize(0.24f),
        )
        fallbackFrame?.let { frame ->
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
        if (!asset.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = asset.coverUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onError = {
                    coverLoadFailed = true
                    if (!reportedCoverFailure) {
                        reportedCoverFailure = true
                        onRemoteLoadFailed?.invoke()
                    }
                },
            )
        }
    }
}
