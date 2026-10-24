package com.mygolfswing.app.ui.plaza

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun V2PlazaHeroCard(
    video: V2PlazaVideo,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.12f),
                spotColor = DSColors.fairway.copy(alpha = 0.12f),
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onTap),
    ) {
        V2PlazaThumbnailBackground(
            video = video,
            modifier = Modifier.fillMaxSize(),
            heroScrim = true,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .size(180.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            DSColors.trophyGold.copy(alpha = 0.22f),
                            DSColors.trophyGold.copy(alpha = 0f),
                        ),
                    ),
                ),
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(DSSpacing.lg),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                TodayFeaturedChip(video.publishedAtMs)
                Spacer(modifier = Modifier.weight(1f))
                DurationChip(video.durationSec.toInt())
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        video.title,
                        style = DSTypography.visualHeroTitle,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        V2PlazaCoachAvatar(
                            coach = video.coach,
                            size = 18.dp,
                            fontSize = 8.sp,
                            borderColor = Color.White.copy(alpha = 0.75f),
                            borderWidth = 1.5.dp,
                        )
                        Text(
                            video.coach.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                PlayButton()
            }
        }
    }
}

@Composable
private fun TodayFeaturedChip(publishedAtMs: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.28f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(9.dp),
        )
        Text("今日精选", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = 0.25.sp)
        Text("· ${monthDayString(publishedAtMs)}", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun DurationChip(seconds: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(8.dp))
        Text(formatDuration(seconds), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun PlayButton() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.15f),
            )
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            null,
            tint = DSColors.deepGreen,
            modifier = Modifier
                .size(14.dp)
                .offset(x = 1.dp),
        )
    }
}

private fun monthDayString(ms: Long): String =
    SimpleDateFormat("M月d日", Locale.CHINA).format(Date(ms))

private fun formatDuration(seconds: Int): String {
    val total = seconds.coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
