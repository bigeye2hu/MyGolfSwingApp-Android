package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import com.mygolfswing.app.R
import coil.compose.AsyncImage
import com.mygolfswing.app.data.memo.V2MemoThumbnailStore
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.model.V2MemoThumbnailKind
import com.mygolfswing.app.data.video.V2VideoThumbnailLoader

/**
 * Memo 视频封面：thumbnailUrl → 本地抽帧 → 主题照片 → 渐变占位。
 *
 * 主题照片映射与 iOS `V2Memo.v2FallbackCoverAssetName` 一致；远端、Mock 和游客
 * Memo 不再全部退回同一张草地图片。本地视频仍优先现场抽取真实首帧。
 */
@Composable
fun V2MemoThumbnail(
    memo: V2Memo,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    useHeroGrassPlaceholder: Boolean = false,
    showKindPlaceholderIcon: Boolean = false,
) {
    val context = LocalContext.current
    val resolvedUrl = remember(memo.id, memo.thumbnailUrl, memo.videoUrl) {
        V2MemoThumbnailStore.resolveThumbnailUrl(context, memo)
    }
    var fallbackBitmap by remember(memo.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    val fallbackCoverRes = remember(memo.hasVideo, memo.videoUrl, memo.thumbnailKind) {
        memoFallbackCoverDrawableRes(memo)
    }

    LaunchedEffect(memo.id, resolvedUrl, memo.videoUrl) {
        if (resolvedUrl != null || !isLocalMemoVideoReference(memo.videoUrl)) {
            fallbackBitmap = null
            return@LaunchedEffect
        }
        fallbackBitmap = V2VideoThumbnailLoader.loadFirstFrame(memo.videoUrl)
    }
    val fallback = fallbackBitmap

    Box(
        modifier = modifier.background(
            if (useHeroGrassPlaceholder) homeMemoPlaceholderBrush() else thumbnailGradientBrush(memo.thumbnailKind),
        ),
    ) {
        when {
            resolvedUrl != null -> {
                AsyncImage(
                    model = resolvedUrl,
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            fallback != null -> {
                Image(
                    bitmap = fallback.asImageBitmap(),
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            fallbackCoverRes != null -> {
                Image(
                    painter = painterResource(id = fallbackCoverRes),
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            useHeroGrassPlaceholder -> {
                Image(
                    painter = painterResource(id = R.drawable.v2_hero_grass_bg),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.55f),
                )
            }
            showKindPlaceholderIcon -> {
                Icon(
                    imageVector = when (memo.kind) {
                        V2MemoKind.VIDEO -> Icons.Filled.PlayArrow
                        V2MemoKind.TEXT -> Icons.Filled.Description
                        V2MemoKind.MIXED -> Icons.Filled.Videocam
                    },
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.50f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(0.27f),
                )
            }
        }
    }
}

internal fun memoFallbackCoverDrawableRes(memo: V2Memo): Int? {
    if (!memo.hasVideo || isLocalMemoVideoReference(memo.videoUrl)) return null
    return when (memo.thumbnailKind) {
        V2MemoThumbnailKind.FAIRWAY -> R.drawable.v2_quickadd_video
        V2MemoThumbnailKind.GREEN_GRASS -> R.drawable.v2_quickadd_swing
        V2MemoThumbnailKind.SUNSET -> R.drawable.homepage_analysis
        V2MemoThumbnailKind.SAND -> R.drawable.v2_quickadd_goal
        V2MemoThumbnailKind.MIST -> R.drawable.golf_course_background
        V2MemoThumbnailKind.FOREST -> R.drawable.golf_swing_hero
    }
}

internal fun isLocalMemoVideoReference(value: String?): Boolean {
    val normalized = value?.trim()?.lowercase().orEmpty()
    return normalized.startsWith("file:") ||
        normalized.startsWith("content:") ||
        normalized.startsWith("/")
}

private fun homeMemoPlaceholderBrush(): Brush = Brush.linearGradient(
    listOf(
        Color(0xFF1B4332),
        Color(0xFF2D6A4F),
    ),
)

fun thumbnailGradientBrush(kind: V2MemoThumbnailKind): Brush {
    val (a, b) = when (kind) {
        V2MemoThumbnailKind.FAIRWAY -> Color(0xFF2D6A4F) to Color(0xFF95D5B2)
        V2MemoThumbnailKind.SAND -> Color(0xFFC8930B) to Color(0xFFF6E2B3)
        V2MemoThumbnailKind.GREEN_GRASS -> Color(0xFF52B788) to Color(0xFFD8F3DC)
        V2MemoThumbnailKind.SUNSET -> Color(0xFFC0392B) to Color(0xFFF6B26B)
        V2MemoThumbnailKind.MIST -> Color(0xFF6C757D) to Color(0xFFCED4DA)
        V2MemoThumbnailKind.FOREST -> Color(0xFF1B4332) to Color(0xFF2D6A4F)
    }
    return Brush.linearGradient(listOf(a, b))
}
