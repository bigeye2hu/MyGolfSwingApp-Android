package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.cardWhite)
            .border(0.5.dp, DSColors.borderLine, RoundedCornerShape(14.dp))
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            V2MemoThumbnail(memo = memo, modifier = Modifier.fillMaxSize())
            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
            memo.durationSec?.takeIf { it > 0 }?.let { sec ->
                val t = sec.toInt()
                Text(
                    String.format(Locale.ROOT, "%d:%02d", t / 60, t % 60),
                    color = Color.White,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(0.65f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            SourceChip(chipText, chipStyle)
            Text(
                memo.displayTitle ?: memo.title ?: "无标题",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.inkGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            memo.text?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 11.sp, color = DSColors.shadowGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(metaText, fontSize = 10.sp, color = DSColors.shadowGray, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun SourceChip(text: String, style: V2VideoSourceChipStyle) {
    val (bg, fg, border) = when (style) {
        V2VideoSourceChipStyle.MUTED -> Triple(DSColors.sandBeige, DSColors.shadowGray, DSColors.listCardBorder)
        V2VideoSourceChipStyle.GOLD -> Triple(DSColors.goldBright.copy(0.12f), DSColors.goldBright, DSColors.goldBright.copy(0.4f))
        V2VideoSourceChipStyle.LEAF -> Triple(DSColors.chipGreenBg, DSColors.leafGreen, DSColors.leafGreen.copy(0.3f))
    }
    Text(
        text,
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 2.dp),
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
