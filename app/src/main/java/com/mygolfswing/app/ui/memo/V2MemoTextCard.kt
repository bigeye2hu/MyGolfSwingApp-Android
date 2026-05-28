package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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

/** 随手记横滑卡（对齐 iOS `V2MemoTextCard`，200×145） */
@Composable
fun V2MemoTextCard(memo: V2Memo, modifier: Modifier = Modifier) {
    Box(modifier = modifier.width(200.dp).height(145.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .shadow(2.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(DSColors.sandBeige)
                .border(0.5.dp, DSColors.borderLine, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row {
                Text(
                    "文字",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DSColors.fairway,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DSColors.mistGreen)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                memo.accent?.let { acc ->
                    Spacer(Modifier.width(4.dp))
                    Text(
                        acc.displayName,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DSColors.goldBright,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(DSColors.goldBright.copy(0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                text = memo.text ?: memo.title ?: "",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.inkGreen,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            memo.subText?.takeIf { it.isNotBlank() }?.let { sub ->
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .width(2.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(DSColors.goldBright),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = sub,
                        fontSize = 10.sp,
                        color = DSColors.inkGreen.copy(0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = memo.metaText ?: "最近",
                    fontSize = 9.sp,
                    color = DSColors.shadowGray,
                )
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.Edit, null, tint = DSColors.shadowGray, modifier = Modifier.size(8.dp))
                Spacer(Modifier.width(2.dp))
                Text("${memo.wordCount}字", fontSize = 9.sp, color = DSColors.shadowGray)
            }
        }
        if (memo.isNew) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(DSColors.goldBright)
                    .border(1.dp, DSColors.sandBeige, CircleShape),
            )
        }
    }
}
