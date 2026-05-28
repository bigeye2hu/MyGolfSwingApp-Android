package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing

@Composable
fun V2MemoStatsBar(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = DSColors.shadowGray,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DSColors.listMutedBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
fun V2MemoFilterChips(
    filters: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { (id, label) ->
            val selected = id == selectedId
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) Color.White else DSColors.inkGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) DSColors.fairway else DSColors.cardWhite)
                    .border(1.dp, if (selected) DSColors.fairway else DSColors.listCardBorder, RoundedCornerShape(50))
                    .clickable { onSelect(id) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
fun V2MemoListSectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.listMutedBg)
            .padding(horizontal = DSSpacing.lg, vertical = 8.dp),
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
    }
}
