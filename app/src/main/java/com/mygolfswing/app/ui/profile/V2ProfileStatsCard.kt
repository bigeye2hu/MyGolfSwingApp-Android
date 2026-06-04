package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors

@Composable
fun V2ProfileStatsCard(
    activeGoals: Int,
    completedGoals: Int,
    videoMemoCount: Int,
    swingCount: Int,
    modifier: Modifier = Modifier,
    onTapActive: () -> Unit = {},
    onTapCompleted: () -> Unit = {},
    onTapVideoMemo: () -> Unit = {},
    onTapSwing: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(206.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite),
    ) {
        Row(modifier = Modifier.weight(1f)) {
            StatCell(
                value = "$activeGoals",
                label = "进行中目标",
                isPrimary = true,
                rightBorder = true,
                bottomBorder = true,
                onTap = onTapActive,
                modifier = Modifier.weight(1f),
            )
            StatCell(
                value = "$completedGoals",
                label = "完成目标",
                isPrimary = false,
                rightBorder = false,
                bottomBorder = true,
                onTap = onTapCompleted,
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.weight(1f)) {
            StatCell(
                value = "$videoMemoCount",
                label = "视频笔记",
                isPrimary = false,
                rightBorder = true,
                bottomBorder = false,
                onTap = onTapVideoMemo,
                modifier = Modifier.weight(1f),
            )
            StatCell(
                value = compact(swingCount),
                label = "挥杆数",
                isPrimary = false,
                rightBorder = false,
                bottomBorder = false,
                onTap = onTapSwing,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun compact(n: Int): String = when {
    n >= 10_000 -> "%.1fw".format(n / 10_000.0)
    n >= 1_000 -> "%.1fk".format(n / 1_000.0)
    else -> n.toString()
}

@Composable
private fun StatCell(
    value: String,
    label: String,
    isPrimary: Boolean,
    rightBorder: Boolean,
    bottomBorder: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { onTap() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) DSColors.fairway else DSColors.inkGreen,
                maxLines = 1,
            )
            Text(text = label, fontSize = 11.sp, color = DSColors.shadowGray)
        }
        // 右下小箭头
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = DSColors.shadowGray.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
                .size(12.dp),
        )
        // 右描边
        if (rightBorder) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(1.dp)
                    .fillMaxSize()
                    .background(Color(0x14000000)),
            )
        }
        // 底描边
        if (bottomBorder) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(Color(0x14000000)),
            )
        }
    }
}
