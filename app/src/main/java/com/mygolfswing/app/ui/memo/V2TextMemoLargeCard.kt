package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mygolfswing.app.data.model.V2MemoAccent
import com.mygolfswing.app.data.model.V2Topic
import com.mygolfswing.app.ui.designsystem.DSColors

@Composable
fun V2TextMemoLargeCard(memo: V2Memo, modifier: Modifier = Modifier) {
    val (main, sub) = V2MemoListGrouping.dateBlockText(memo.createdAt)
    val title = memo.displayTitle ?: memo.title ?: ""
    val body = memo.text.orEmpty().takeIf { it.isNotBlank() && it != title }
    val quote = memo.subText?.takeIf { it.isNotBlank() }
    val hasTopChips = memo.accent != null || memo.isPinned
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
            .border(1.dp, DSColors.listCardBorder, RoundedCornerShape(14.dp)),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(DSColors.listMutedBg),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(main, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DSColors.fairway)
                Text(
                    sub,
                    fontSize = 10.sp,
                    color = Color(0xFF888888),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(DSColors.listCardBorder),
        )
        Column(modifier = Modifier.weight(1f).padding(12.dp)) {
            if (hasTopChips) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    memo.accent?.let { TextMemoTopChip(memo = memo, accent = it) }
                    if (memo.isPinned) {
                        BorderedTextMemoChip(
                            label = "★",
                            color = DSColors.goldBright,
                            borderColor = DSColors.goldBright,
                        )
                    }
                }
            }
            Text(
                text = title.ifBlank { memo.text.orEmpty() },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B2A21),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = if (hasTopChips) Modifier.padding(top = 8.dp) else Modifier,
            )
            body?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF555555),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            quote?.let {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(DSColors.goldBright),
                    )
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF777777),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${memo.wordCount} 字",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF999999),
                )
                Spacer(Modifier.weight(1f))
                if (memo.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        memo.tags.take(3).forEach { tag ->
                            Text(
                                tag,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = DSColors.fairway,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DSColors.chipGreenBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextMemoTopChip(memo: V2Memo, accent: V2MemoAccent) {
    val isAiInsight = memo.isAiInsightMemo()
    val label = when {
        accent == V2MemoAccent.BREAKTHROUGH -> "突破"
        accent == V2MemoAccent.INSIGHT && isAiInsight -> "AI 洞察"
        accent == V2MemoAccent.INSIGHT -> "心法"
        else -> memo.accentDisplayLabel()
    }
    val (color, borderColor) = when {
        accent == V2MemoAccent.BREAKTHROUGH -> DSColors.goldBright to DSColors.goldBright
        accent == V2MemoAccent.INSIGHT && isAiInsight -> DSColors.leafGreen to DSColors.leafGreen
        else -> DSColors.fairway to DSColors.listCardBorder
    }
    BorderedTextMemoChip(label = label, color = color, borderColor = borderColor)
}

@Composable
private fun BorderedTextMemoChip(label: String, color: Color, borderColor: Color) {
    Text(
        label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

fun matchesTextFilter(memo: V2Memo, filterId: String): Boolean = when (filterId) {
    "all" -> true
    "mind" -> memo.topic == V2Topic.MIND || memo.tags.any { it.contains("心法") }
    "reflection" -> memo.accent == V2MemoAccent.REFLECTION
    "breakthrough" -> memo.accent == V2MemoAccent.BREAKTHROUGH
    "ai" -> memo.isAiInsightMemo()
    "pinned" -> memo.isPinned
    else -> true
}
