package com.mygolfswing.app.ui.plaza

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircle
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.ui.designsystem.DSColors

/**
 * 广场 2 列网格视频卡（对齐 iOS `V2PlazaVideoGridCard`）。
 */
@Composable
fun V2PlazaGridCard(
    video: V2PlazaVideo,
    onTap: () -> Unit,
    onSaveTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .border(width = 1.dp, color = Color.Black.copy(alpha = 0.05f), shape = RoundedCornerShape(16.dp))
            .clickable { onTap() },
    ) {
        Thumbnail(video = video, onSaveTap = onSaveTap)
        InfoBlock(video = video)
    }
}

@Composable
private fun Thumbnail(video: V2PlazaVideo, onSaveTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .background(Brush.linearGradient(video.thumbnailKind.gradient)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.7f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.3f),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(40.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            TopicChip(text = video.categoryDisplayName ?: video.topic.displayName)
            Spacer(modifier = Modifier.weight(1f))
            if (video.isNew) NewBadge()
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SaveButton(saved = video.isSaved, onTap = onSaveTap)
            Spacer(modifier = Modifier.weight(1f))
            DurationChip(seconds = video.durationSec.toInt())
        }
    }
}

@Composable
private fun TopicChip(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = DSColors.fairway,
        modifier = Modifier
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(DSColors.sandBeige)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun NewBadge() {
    Text(
        text = "NEW",
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        letterSpacing = 0.5.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DSColors.flagRed)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun DurationChip(seconds: Int) {
    val mm = seconds / 60
    val ss = seconds % 60
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(7.dp),
        )
        Text(
            text = "%d:%02d".format(mm, ss),
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SaveButton(saved: Boolean, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (saved) DSColors.trophyGold else Color.Black.copy(alpha = 0.55f))
            .clickable { onTap() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            contentDescription = if (saved) "已收藏" else "收藏",
            tint = Color.White,
            modifier = Modifier.size(10.dp),
        )
    }
}

@Composable
private fun InfoBlock(video: V2PlazaVideo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = video.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = DSColors.inkGreen,
            maxLines = 2,
            minLines = 2,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CoachAvatar(coach = video.coach)
            Text(
                text = video.coach.displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = DSColors.shadowGray,
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row {
            StatLabel(icon = Icons.Filled.PlayArrow, text = formatCount(video.viewCount))
            Spacer(modifier = Modifier.weight(1f))
            StatLabel(icon = Icons.Filled.BookmarkBorder, text = formatCount(video.saveCount))
        }
    }
}

@Composable
private fun CoachAvatar(coach: com.mygolfswing.app.data.model.V2PlazaCoach) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(coach.avatarTint.gradient)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = coach.initialChar, color = Color.White, fontSize = 6.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DSColors.shadowGray,
            modifier = Modifier.size(9.dp),
        )
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = DSColors.shadowGray)
    }
}

private fun formatCount(n: Int): String = when {
    n >= 10_000 -> "%.1fw".format(n / 10_000.0)
    n >= 1_000 -> "%.1fk".format(n / 1_000.0)
    else -> n.toString()
}
