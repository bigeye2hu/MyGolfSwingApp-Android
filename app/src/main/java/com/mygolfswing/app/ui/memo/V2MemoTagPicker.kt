package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.mygolfswing.app.data.model.V2Topic
import com.mygolfswing.app.ui.designsystem.DSColors

private val presetTags = listOf("挥杆", "推杆", "短杆", "沙坑", "握把", "节奏", "教练点评", "7号铁")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun V2MemoTagPicker(
    selected: List<String>,
    onChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presetTags.forEach { tag ->
            val on = tag in selected
            Text(
                tag,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (on) Color.White else DSColors.inkGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) DSColors.fairway else DSColors.cardWhite)
                    .border(1.dp, if (on) DSColors.fairway else DSColors.listCardBorder, RoundedCornerShape(50))
                    .clickable {
                        onChange(if (on) selected - tag else selected + tag)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        V2Topic.entries.filter { it != V2Topic.ALL }.forEach { t ->
            val tag = t.displayName
            if (tag !in presetTags) {
                val on = tag in selected
                Text(
                    tag,
                    fontSize = 12.sp,
                    color = if (on) Color.White else DSColors.inkGreen,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (on) DSColors.fairway else DSColors.mistGreen)
                        .clickable { onChange(if (on) selected - tag else selected + tag) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
