package com.mygolfswing.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.foundation.clickable
import com.mygolfswing.app.ui.common.V2Format
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.designsystem.DSColors

/**
 * V2 主页头部（对齐 iOS `V2HomeHeader`）。
 *
 * - 左：48dp 头像（金色 2.5dp 边）
 * - 中：「问候，用户名」+ 日期 · 今日还有 N 个目标 chip
 * - 右：42dp + 添加按钮（绿描边）
 */
@Composable
fun V2HomeHeader(
    goalCount: Int,
    displayName: String,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    onAddTap: () -> Unit = {},
) {
    val ringYellow = Color(0xFFE0A82E)
    val titleInk = Color(0xFF1B2A21)
    val dateGreen = DSColors.fairway
    val dotGray = Color(0xFF9AAB9F)
    val mutedGray = Color(0xFF6C757D)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                .border(width = 2.5.dp, color = ringYellow, shape = CircleShape),
        ) {
            V2UserAvatar(
                avatar = avatarUrl,
                modifier = Modifier.fillMaxSize(),
                emojiFontSize = 22.sp,
                placeholderIconSize = 28.dp,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = "${V2Format.greeting()}，$displayName",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = titleInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = V2Format.headerDate(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = dateGreen,
                )
                Text(text = "·", fontSize = 11.sp, color = dotGray)
                Text(text = "今日还有", fontSize = 11.sp, color = mutedGray)
                Text(
                    text = "$goalCount 个目标",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ringYellow,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ringYellow.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color.White)
                .border(width = 1.5.dp, color = DSColors.fairway, shape = CircleShape)
                .clickable { onAddTap() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "添加",
                tint = DSColors.fairway,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
