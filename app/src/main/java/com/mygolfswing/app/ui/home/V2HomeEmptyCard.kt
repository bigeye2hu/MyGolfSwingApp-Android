package com.mygolfswing.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography

/**
 * 主页空状态大白卡（对齐 iOS `V2HomeEmptyCard`）。
 */
@Composable
fun V2HomeEmptyCard(
    modifier: Modifier = Modifier,
    onCreateGoal: () -> Unit = {},
    onBrowsePlaza: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(DSSpacing.Radius.lg))
            .clip(RoundedCornerShape(DSSpacing.Radius.lg))
            .background(DSColors.cardWhite)
            .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(DSColors.sandBeige),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Adjust,
                contentDescription = null,
                tint = DSColors.trophyGold,
                modifier = Modifier.size(40.dp),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "还没有目标，开始制定你的第一个？",
                style = DSTypography.titleMedium,
                color = DSColors.inkGreen,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "目标可以是次数、时长、距离精度，也可以是自由练习",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = DSSpacing.md),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(DSColors.deepGreen)
                .clickable { onCreateGoal() }
                .padding(horizontal = DSSpacing.xl, vertical = 14.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = DSColors.cardWhite,
                modifier = Modifier.size(15.dp),
            )
            Text(text = "新建目标", style = DSTypography.button, color = DSColors.cardWhite)
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = DSColors.cardWhite,
                modifier = Modifier.size(13.dp),
            )
        }

        Spacer(modifier = Modifier.height(0.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable { onBrowsePlaza() },
        ) {
            Text(
                text = "浏览广场获取灵感",
                style = DSTypography.cardTitle,
                color = DSColors.shadowGray,
            )
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = DSColors.shadowGray,
                modifier = Modifier.size(11.dp),
            )
        }
    }
}
