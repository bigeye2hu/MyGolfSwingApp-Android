package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoAccent
import com.mygolfswing.app.ui.designsystem.DSColors

@Composable
fun V2TextMemoLargeCard(memo: V2Memo, modifier: Modifier = Modifier) {
    val (main, sub) = V2MemoListGrouping.dateBlockText(memo.createdAt)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.cardWhite)
            .border(0.5.dp, DSColors.borderLine, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
            Text(main, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
            Text(sub, fontSize = 10.sp, color = DSColors.shadowGray)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                memo.accent?.let { AccentChip(it) }
                if (memo.isPinned) {
                    Text("★", fontSize = 10.sp, color = DSColors.trophyGold)
                }
            }
            Text(
                memo.text ?: memo.title ?: "",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.inkGreen,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    memo.metaText ?: V2MemoListGrouping.metaPrefix(memo.createdAt),
                    fontSize = 10.sp,
                    color = DSColors.shadowGray,
                )
                Text("${memo.wordCount}字", fontSize = 10.sp, color = DSColors.shadowGray)
            }
            if (memo.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    memo.tags.take(3).forEach { tag ->
                        Text(
                            tag,
                            fontSize = 9.sp,
                            color = DSColors.fairway,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(DSColors.mistGreen)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccentChip(accent: V2MemoAccent) {
    Text(
        accent.displayName,
        fontSize = 8.sp,
        fontWeight = FontWeight.SemiBold,
        color = DSColors.goldBright,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DSColors.goldBright.copy(0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

fun matchesTextFilter(memo: V2Memo, filterId: String): Boolean = when (filterId) {
    "all" -> true
    "insight" -> memo.accent == V2MemoAccent.INSIGHT
    "reflection" -> memo.accent == V2MemoAccent.REFLECTION
    "breakthrough" -> memo.accent == V2MemoAccent.BREAKTHROUGH
    "coach" -> memo.accent == V2MemoAccent.COACH_NOTE
    "pinned" -> memo.isPinned
    else -> true
}
