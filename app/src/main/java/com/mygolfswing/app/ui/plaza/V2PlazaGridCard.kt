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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.ui.designsystem.DSColors

/**
 * 广场 2 列网格视频卡（对齐 iOS `V2PlazaVideoGridCard`）。
 */
@Composable
@android.annotation.SuppressLint("ModifierParameter")
fun V2PlazaGridCard(
    video: V2PlazaVideo,
    onTap: () -> Unit,
    onSaveTap: () -> Unit,
    isGuest: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .border(width = 1.dp, color = Color.Black.copy(alpha = 0.05f), shape = RoundedCornerShape(16.dp))
            .clickable { onTap() },
    ) {
        Thumbnail(video = video, isGuest = isGuest, onSaveTap = onSaveTap)
        InfoBlock(video = video)
    }
}

@Composable
private fun Thumbnail(video: V2PlazaVideo, isGuest: Boolean, onSaveTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
    ) {
        V2PlazaThumbnailBackground(
            video = video,
            modifier = Modifier.fillMaxSize(),
            heroScrim = false,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            video.categoryDisplayName?.let { TopicChip(text = it) }
            Spacer(modifier = Modifier.weight(1f))
            if (video.isNew) NewBadge()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SaveButton(saved = video.isSaved, isGuest = isGuest, onTap = onSaveTap)
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
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(50),
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.18f),
            )
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
private fun SaveButton(saved: Boolean, isGuest: Boolean, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                when {
                    saved -> DSColors.trophyGold
                    isGuest -> DSColors.deepGreen.copy(alpha = 0.82f)
                    else -> Color.Black.copy(alpha = 0.55f)
                },
            )
            .clickable { onTap() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            contentDescription = if (saved) "已收藏" else "收藏",
            tint = Color.White,
            modifier = Modifier.size(10.dp),
        )
        if (isGuest && !saved) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = DSColors.deepGreen,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp),
            )
        }
    }
}

@Composable
private fun InfoBlock(video: V2PlazaVideo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(122.dp)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            text = video.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = DSColors.inkGreen,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            V2PlazaCoachAvatar(coach = video.coach, size = 16.dp, fontSize = 7.sp)
            Text(
                text = video.coach.displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = DSColors.shadowGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row {
            StatLabel(
                icon = if (video.viewCount >= V2_PLAZA_PUBLIC_VIEWS_THRESHOLD) Icons.Filled.PlayCircleOutline else Icons.Filled.AutoAwesome,
                text = video.publicGridPrimaryMetaText(),
            )
            Spacer(modifier = Modifier.weight(1f))
            StatLabel(
                icon = if (video.publicGridUsesBookmark()) Icons.Filled.BookmarkBorder else Icons.Filled.Schedule,
                text = video.publicGridSecondaryMetaText(),
            )
        }
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
