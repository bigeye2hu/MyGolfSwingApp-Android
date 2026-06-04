package com.mygolfswing.app.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import java.util.Locale

/**
 * 主页「广场推荐」横滑段（对齐 iOS `V2HomePlazaSection`）。
 *
 * - 数据来自 [V2PlazaStore.homeRecommendations]（真实云端 coverURL）
 * - 加载中且列表为空时显示 skeleton，避免空白闪屏
 */
@Composable
fun V2HomePlazaSection(
    videos: List<V2PlazaVideo>,
    isLoading: Boolean = false,
    onSeeAll: () -> Unit = {},
    onVideoTap: (V2PlazaVideo) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DSSpacing.md),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DSSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.PlayArrow, null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
                Text("广场推荐", style = DSTypography.titleLarge, color = DSColors.inkGreen)
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.clickable(onClick = onSeeAll),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("全部", style = DSTypography.cardTitle, color = DSColors.fairway)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = DSColors.fairway, modifier = Modifier.size(14.dp))
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = DSSpacing.lg),
        ) {
            if (videos.isEmpty()) {
                items(3) { V2HomePlazaSkeletonCard() }
            } else {
                items(videos, key = { it.id }) { video ->
                    V2HomePlazaCard(video = video, onTap = { onVideoTap(video) })
                }
            }
        }
    }
}

@Composable
private fun V2HomePlazaCard(video: V2PlazaVideo, onTap: () -> Unit) {
    val chip = when {
        video.isFeaturedToday -> "精选"
        video.isNew -> "新发布"
        else -> video.categoryDisplayName ?: "推荐"
    }
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onTap),
    ) {
        PlazaThumbnail(video = video, modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    chip,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(0.45f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    formatDuration(video.durationSec),
                    fontSize = 10.sp,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(0.45f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(0.55f)),
                        ),
                    )
                    .padding(10.dp),
            ) {
                Text(
                    video.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${video.coach.displayName} · ${formatViewCount(video.viewCount)} 次观看",
                    fontSize = 11.sp,
                    color = Color.White.copy(0.8f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PlazaThumbnail(video: V2PlazaVideo, modifier: Modifier = Modifier) {
    val cover = video.thumbnailUrl
    if (!cover.isNullOrBlank()) {
        AsyncImage(
            model = cover,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(video.thumbnailKind.gradient),
            ),
        )
    }
}

@Composable
private fun V2HomePlazaSkeletonCard() {
    val transition = rememberInfiniteTransition(label = "plaza_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        DSColors.fairway.copy(alpha),
                        DSColors.deepGreen.copy(alpha + 0.1f),
                    ),
                ),
            ),
    )
}

private fun formatDuration(sec: Double): String {
    val total = sec.toInt().coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return String.format(Locale.ROOT, "%d:%02d", m, s)
}

private fun formatViewCount(count: Int): String = when {
    count >= 10_000 -> String.format(Locale.ROOT, "%.1fw", count / 10_000f)
    count >= 1_000 -> String.format(Locale.ROOT, "%.1fk", count / 1_000f)
    else -> count.toString()
}
