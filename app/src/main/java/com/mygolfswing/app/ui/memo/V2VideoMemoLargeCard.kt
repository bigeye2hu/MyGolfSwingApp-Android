package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.ui.designsystem.DSColors
import java.util.Locale

enum class V2VideoSourceChipStyle { MUTED, GOLD, LEAF }

@Composable
fun V2VideoMemoLargeCard(
    memo: V2Memo,
    chipText: String,
    chipStyle: V2VideoSourceChipStyle,
    metaText: String,
    modifier: Modifier = Modifier,
) {
    val playable = remember(memo.videoUrl) { memoVideoPlayable(memo.videoUrl) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(109.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f),
            )
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.listCardBorder, RoundedCornerShape(14.dp)),
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            V2MemoThumbnail(memo = memo, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                        ),
                    ),
            )
            if (playable) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp).offset(x = 1.dp),
                    )
                }
            } else {
                Text(
                    "文件缺失",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(0.52f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
            memo.durationSec?.takeIf { playable && it > 0 }?.let { sec ->
                val t = sec.toInt()
                Text(
                    String.format(Locale.ROOT, "%d:%02d", t / 60, t % 60),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 8.dp, start = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(0.7f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(12.dp)) {
            Text(
                memo.displayTitle ?: memo.title ?: "无标题",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.fairway,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            memo.text?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            SourceChip(chipText, chipStyle)
            Text(
                metaText,
                fontSize = 11.sp,
                color = Color(0xFF999999),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SourceChip(text: String, style: V2VideoSourceChipStyle) {
    val (bg, fg, border) = when (style) {
        V2VideoSourceChipStyle.MUTED -> Triple(DSColors.listMutedBg, DSColors.fairway, Color.Transparent)
        V2VideoSourceChipStyle.GOLD -> Triple(Color.Transparent, DSColors.goldBright, DSColors.goldBright)
        V2VideoSourceChipStyle.LEAF -> Triple(DSColors.leafGreen.copy(alpha = 0.12f), DSColors.leafGreen, Color.Transparent)
    }
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

fun videoSourceChipFor(memo: V2Memo): Pair<String, V2VideoSourceChipStyle> {
    if (memo.isPlazaVideo) return "球场视频" to V2VideoSourceChipStyle.LEAF
    if (memo.isOnlineVideo) return "在线" to V2VideoSourceChipStyle.GOLD
    return when (memo.trainingMethod) {
        "拍摄" -> "拍摄" to V2VideoSourceChipStyle.MUTED
        "本地上传" -> "本地" to V2VideoSourceChipStyle.MUTED
        else -> (memo.trainingMethod?.takeIf { it.isNotBlank() } ?: "本地") to V2VideoSourceChipStyle.MUTED
    }
}

fun matchesVideoFilter(memo: V2Memo, filterId: String): Boolean = when (filterId) {
    "all" -> true
    "shoot" -> memo.trainingMethod == "拍摄"
    "upload" -> memo.trainingMethod == "本地上传"
    "online" -> memo.isOnlineVideo && !memo.isPlazaVideo
    "plaza" -> memo.isPlazaVideo
    else -> true
}
