package com.mygolfswing.app.ui.plaza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.mygolfswing.app.data.model.V2PlazaVideo

/**
 * 广场视频封面层：渐变兜底 + 云端 [V2PlazaVideo.thumbnailUrl]（对齐 iOS `thumbnailBackground`）。
 */
@Composable
fun V2PlazaThumbnailBackground(
    video: V2PlazaVideo,
    modifier: Modifier = Modifier,
    heroScrim: Boolean = false,
) {
    Box(
        modifier = modifier.background(Brush.linearGradient(video.thumbnailKind.gradient)),
    ) {
        val url = video.thumbnailUrl?.trim()
        if (!url.isNullOrEmpty()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        if (heroScrim) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.10f),
                                Color.Black.copy(alpha = 0.65f),
                            ),
                        ),
                    ),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.7f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.3f),
                        ),
                    ),
            )
        }
    }
}
