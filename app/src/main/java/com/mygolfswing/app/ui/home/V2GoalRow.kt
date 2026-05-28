package com.mygolfswing.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.ui.designsystem.DSColors

/**
 * 主页目标列表行（对齐 iOS `V2GoalRow`）。
 *
 * - 左：彩色圆形图标 + SF Symbol 类似图
 * - 中：标题 + 副标题（球杆 · 单次量 · 打卡进度）
 * - 右：深绿胶囊「打卡」按钮（60×32）
 */
@Composable
fun V2GoalRow(
    goal: V2Goal,
    onTap: () -> Unit = {},
    onComplete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(width = 1.dp, color = Color.Black.copy(alpha = 0.05f), shape = RoundedCornerShape(16.dp))
            .clickable { onTap() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBox(goal = goal)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = goal.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B2A21),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildSubtitle(goal),
                fontSize = 11.sp,
                color = Color(0xFF6C757D),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        CompleteButton(goal = goal, onComplete = onComplete)
    }
}

private fun buildSubtitle(goal: V2Goal): String = buildList {
    add(goal.typeLabel)
    goal.metricDisplay?.let { add(it) }
    add(goal.sessionProgressDisplay)
}.joinToString(" · ")

@Composable
private fun IconBox(goal: V2Goal) {
    val (bg, fg, icon) = iconStyle(goal.iconType)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(20.dp),
        )
    }
}

private data class IconStyle(val bg: Color, val fg: Color, val icon: ImageVector)

private fun iconStyle(type: V2Goal.IconType): IconStyle = when (type) {
    V2Goal.IconType.TARGET -> IconStyle(Color(0xFFE9DCC2), Color(0xFF946B2E), Icons.Filled.Adjust)
    V2Goal.IconType.CLUB -> IconStyle(Color(0xFFE9DCC2), Color(0xFF946B2E), Icons.Filled.SportsGolf)
    V2Goal.IconType.PUTT -> IconStyle(Color(0xFFD8E2DC), DSColors.fairway, Icons.Filled.GolfCourse)
    V2Goal.IconType.SAND -> IconStyle(Color(0xFFF7E3BD), Color(0xFF946B2E), Icons.Filled.WbSunny)
    V2Goal.IconType.TIP -> IconStyle(Color(0xFFF6E3BD), Color(0xFF946B2E), Icons.Filled.Lightbulb)
}

@Composable
private fun CompleteButton(goal: V2Goal, onComplete: () -> Unit) {
    data class ButtonStyle(
        val title: String,
        val icon: ImageVector?,
        val background: Color,
        val foreground: Color,
        val enabled: Boolean,
    )

    val style = when {
        goal.status == V2Goal.Status.COMPLETED ->
            ButtonStyle("已完成", Icons.Filled.EmojiEvents, Color(0xFFEDEFF2), Color(0xFF6C757D), false)
        goal.status == V2Goal.Status.PAUSED ->
            ButtonStyle("已暂停", Icons.Filled.Pause, Color(0xFFEDEFF2), Color(0xFF6C757D), false)
        goal.hasCheckedInToday ->
            ButtonStyle("今日已打", Icons.Filled.CheckCircle, DSColors.fairway.copy(alpha = 0.12f), DSColors.fairway, false)
        else ->
            ButtonStyle("打卡", Icons.Filled.Check, DSColors.fairway, Color.White, true)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .shadow(elevation = if (style.enabled) 4.dp else 0.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(style.background)
            .clickable(enabled = style.enabled, onClick = onComplete)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = style.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = style.foreground,
        )
    }
}
