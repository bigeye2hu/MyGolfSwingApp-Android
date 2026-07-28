package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mygolfswing.app.data.model.V2SwingDayItem
import com.mygolfswing.app.ui.designsystem.DSColors
import java.util.UUID

@Composable
fun V2MemoSwingSessionCard(
    item: V2SwingDayItem,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    onClipTap: (UUID) -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f),
            )
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.warmWhite)
            .border(1.dp, DSColors.listCardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onTap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(DSColors.listMutedBg)
                .padding(vertical = 12.dp),
        ) {
            Text(
                item.dayNumber,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DSColors.fairway,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.monthLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.fairway.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.relativeText,
                fontSize = 10.sp,
                color = DSColors.shadowGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(DSColors.listCardBorder),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.isInProgress) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .border(4.dp, Color.Red.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(Color.Red))
                        }
                    }
                    Text(
                        item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DSColors.inkGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                item.location?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = DSColors.shadowGray,
                            modifier = Modifier.size(10.dp),
                        )
                        Text(
                            it,
                            fontSize = 12.sp,
                            color = DSColors.shadowGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (item.extraLocationCount > 0) {
                            Text(
                                "+${item.extraLocationCount}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DSColors.fairway,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DSColors.listMutedBg)
                                    .border(0.5.dp, DSColors.listCardBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                item.timeRange?.let {
                    SwingMetaLine(icon = Icons.Filled.Schedule, text = it)
                }
                if (item.sessionCount >= 2) {
                    SwingMetaLine(icon = Icons.Filled.Layers, text = "${item.sessionCount} 次会话")
                }
            }
            V2SwingClipThumbnailStack(item = item, onClipTap = onClipTap)
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = DSColors.shadowGray,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun SwingMetaLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            icon,
            contentDescription = null,
            tint = DSColors.shadowGray,
            modifier = Modifier.size(10.dp),
        )
        Text(
            text,
            fontSize = 12.sp,
            color = DSColors.shadowGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun V2SwingClipThumbnailStack(item: V2SwingDayItem, onClipTap: (UUID) -> Unit) {
    val urls = item.clipPreviewUrls.take(3)
    if (urls.isEmpty()) return
    val stackWidth = 44.dp + ((urls.size - 1) * 36).dp
    Box(modifier = Modifier.width(stackWidth).height(44.dp)) {
        urls.forEachIndexed { index, url ->
            val clipId = item.clipPreviewIds.getOrNull(index)
            Box(
                modifier = Modifier
                    .offset(x = (index * 36).dp)
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                    .then(if (clipId != null) Modifier.clickable { onClipTap(clipId) } else Modifier),
            ) {
                SwingClipThumbnailFallback(Modifier.fillMaxSize())
                AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                if (index == urls.lastIndex && item.extraClipCount > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+${item.extraClipCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwingClipThumbnailFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    DSColors.fairway.copy(alpha = 0.85f),
                    DSColors.leafGreen.copy(alpha = 0.65f),
                ),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(12.dp),
        )
    }
}
