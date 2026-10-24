package com.mygolfswing.app.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.R
import com.mygolfswing.app.ui.designsystem.DSLayout

enum class V2PageArtwork(@param:DrawableRes internal val resourceId: Int) {
    FAIRWAY(R.drawable.page_back_1),
    GOLF_BAG(R.drawable.page_back_2),
    GOLFER(R.drawable.page_back_3),
}

enum class V2PageArtworkTone {
    FADE_TO_WARM_BOTTOM,
    WARM_LIGHT,
    WHITE_MIST,
    DARK_GREEN,
    BOTTOM_GREEN,
}

/** Shared light fairway backdrop used by the latest iOS interaction and chat pages. */
@Composable
fun V2RoomBackground(
    modifier: Modifier = Modifier,
    imageHeight: androidx.compose.ui.unit.Dp = DSLayout.roomBackgroundHeight,
    topFadeHeight: androidx.compose.ui.unit.Dp = DSLayout.roomBackgroundFadeHeight,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxWidth().height(imageHeight).align(Alignment.BottomCenter),
        ) {
            Image(
                painter = painterResource(R.drawable.room_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier.fillMaxWidth().height(topFadeHeight).align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFFAF7F0), Color(0x00FAF7F0)),
                        ),
                    ),
            )
        }
    }
}

/** Full-page wrapper matching iOS `v2RoomPageBackground()`. */
@Composable
fun V2RoomPage(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.background(Color(0xFFFAF7F0))) {
        V2RoomBackground(Modifier.fillMaxSize())
        content()
    }
}

@Composable
fun V2PageArtworkBackground(
    artwork: V2PageArtwork,
    tone: V2PageArtworkTone,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.CenterEnd,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(artwork.resourceId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = alignment,
            alpha = if (tone == V2PageArtworkTone.WHITE_MIST) 0.88f else 1f,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier.matchParentSize().background(
                when (tone) {
                    V2PageArtworkTone.FADE_TO_WARM_BOTTOM -> Brush.verticalGradient(
                        0.00f to Color(0xFFFAF7F0).copy(alpha = 0.22f),
                        0.28f to Color(0xFFFAF7F0).copy(alpha = 0.12f),
                        0.62f to Color(0xFFFAF7F0).copy(alpha = 0.46f),
                        1.00f to Color(0xFFFAF7F0),
                    )
                    V2PageArtworkTone.WARM_LIGHT -> Brush.horizontalGradient(
                        0.00f to Color(0xFFF9F7F1).copy(alpha = 0.96f),
                        0.34f to Color(0xFFF9F7F1).copy(alpha = 0.72f),
                        0.72f to Color(0xFFF9F7F1).copy(alpha = 0.08f),
                        1.00f to Color.Transparent,
                    )
                    V2PageArtworkTone.WHITE_MIST -> Brush.horizontalGradient(
                        0.00f to Color.White.copy(alpha = 0.74f),
                        0.50f to Color.White.copy(alpha = 0.24f),
                        1.00f to Color(0xFFE4EEE8).copy(alpha = 0.08f),
                    )
                    V2PageArtworkTone.DARK_GREEN -> Brush.horizontalGradient(
                        0.00f to Color(0xFF122117).copy(alpha = 0.92f),
                        0.46f to Color(0xFF14291C).copy(alpha = 0.62f),
                        1.00f to Color(0xFF1A2E1F).copy(alpha = 0.22f),
                    )
                    V2PageArtworkTone.BOTTOM_GREEN -> Brush.verticalGradient(
                        0.00f to Color(0xFF1B4332).copy(alpha = 0.20f),
                        1.00f to Color(0xFF1B4332).copy(alpha = 0.80f),
                    )
                },
            ),
        )
        if (tone == V2PageArtworkTone.WARM_LIGHT) {
            Box(
                modifier = Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        1.00f to Color(0xFFF9F7F1).copy(alpha = 0.20f),
                    ),
                ),
            )
        }
        content()
    }
}
