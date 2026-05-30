package com.mygolfswing.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.R
import com.mygolfswing.app.data.model.V2Goal

private val Gold = Color(0xFFE0A82E)

/**
 * 主页 Hero 聚焦目标卡（对齐 iOS `V2GoalHeroCard`）。
 */
@Composable
fun V2GoalHeroCard(
    goal: V2Goal,
    linkedMemoCount: Int,
    linkedVideoCount: Int,
    onEdit: () -> Unit = {},
    onComplete: () -> Unit = {},
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 210.dp)
            .shadow(elevation = 20.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable { onTap() },
    ) {
        GolfScapeBackground(modifier = Modifier.matchParentSize())

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeaderRow(onEdit = onEdit)

            Text(
                text = goal.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            MetricRow(goal = goal)
            ProgressRow(goal = goal)
            FooterRow(
                goal = goal,
                linkedMemoCount = linkedMemoCount,
                linkedVideoCount = linkedVideoCount,
                onComplete = onComplete,
            )
        }
    }
}

/**
 * Hero 卡背景（对齐 iOS `golfScapeBackground` 四层叠加）。
 * 背景层使用 [Modifier.matchParentSize]，避免图片 intrinsic 尺寸撑大卡片。
 */
@Composable
private fun GolfScapeBackground(modifier: Modifier = Modifier) {
    val darkGreen = Color(0xFF1B4332)
    val midGreen = Color(0xFF2D6A4F)
    val lightOverlay = Color(0xFFD8E2DC)

    BoxWithConstraints(modifier = modifier) {
        val gradientStart = Offset(constraints.maxWidth * 0.25f, constraints.maxHeight.toFloat())
        val gradientEnd = Offset(constraints.maxWidth * 0.75f, 0f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(darkGreen, midGreen),
                        start = gradientStart,
                        end = gradientEnd,
                    ),
                ),
        )
        Image(
            painter = painterResource(id = R.drawable.v2_hero_grass_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomEnd,
            alpha = 0.45f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            lightOverlay.copy(alpha = 0.18f),
                            midGreen.copy(alpha = 0.5f),
                        ),
                        start = gradientStart,
                        end = gradientEnd,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to darkGreen.copy(alpha = 0.72f),
                        0.55f to darkGreen.copy(alpha = 0.10f),
                        1.0f to Color.Transparent,
                    ),
                ),
        )
    }
}

@Composable
private fun HeaderRow(onEdit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Gold.copy(alpha = 0.2f))
                    .border(width = 1.dp, color = Gold, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = "今日聚焦",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
                letterSpacing = 0.6.sp,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "编辑",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.clickable { onEdit() },
        )
    }
}

@Composable
private fun MetricRow(goal: V2Goal) {
    val targetText = when {
        goal.metric.kind == V2Goal.Metric.Kind.FREE -> "∞"
        goal.metric.target % 1.0 == 0.0 -> goal.metric.target.toInt().toString()
        else -> "%.1f".format(goal.metric.target)
    }
    val unitText = when (goal.metric.kind) {
        V2Goal.Metric.Kind.FREE -> "永久"
        else -> goal.metric.unit?.takeIf { it.isNotEmpty() }
    }
    val subtitleText = run {
        val club = goal.typeLabel
        val extra = when (goal.metric.kind) {
            V2Goal.Metric.Kind.COUNT -> "中铁练习"
            V2Goal.Metric.Kind.DURATION -> "基础打磨"
            V2Goal.Metric.Kind.DISTANCE -> "精度训练"
            V2Goal.Metric.Kind.FREE -> "自由练习"
        }
        "$club · $extra"
    }

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = targetText,
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.125.sp,
        )
        if (unitText != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = unitText,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp)
                .background(Gold.copy(alpha = 0.5f)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = subtitleText,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .padding(bottom = 6.dp)
                .weight(1f, fill = false),
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProgressRow(goal: V2Goal) {
    val ratio = goal.ratio.coerceIn(0.0, 1.0).toFloat()
    Row(verticalAlignment = Alignment.CenterVertically) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.2f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(maxWidth * ratio)
                    .clip(RoundedCornerShape(50))
                    .background(Gold),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = goal.sessionProgressDisplay,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FooterRow(
    goal: V2Goal,
    linkedMemoCount: Int,
    linkedVideoCount: Int,
    onComplete: () -> Unit,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "📚 $linkedMemoCount 条 Memo",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
            Text(text = "·", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
            Text(
                text = "🎬 $linkedVideoCount 个视频",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
        }
        CheckInButton(goal = goal, onComplete = onComplete)
    }
}

@Composable
private fun CheckInButton(goal: V2Goal, onComplete: () -> Unit) {
    data class ButtonStyle(
        val title: String,
        val icon: ImageVector,
        val background: Color,
        val enabled: Boolean,
    )

    val style = when {
        goal.status == V2Goal.Status.COMPLETED ->
            ButtonStyle("已完成", Icons.Filled.EmojiEvents, Gold.copy(alpha = 0.55f), false)
        goal.status == V2Goal.Status.PAUSED ->
            ButtonStyle("已暂停", Icons.Filled.Pause, Color.White.copy(alpha = 0.18f), false)
        goal.hasCheckedInToday ->
            ButtonStyle("今日已打", Icons.Filled.CheckCircle, Color.White.copy(alpha = 0.18f), false)
        else ->
            ButtonStyle("打卡", Icons.Filled.Check, Gold, true)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .shadow(elevation = if (style.enabled) 6.dp else 0.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(style.background)
            .clickable(enabled = style.enabled, onClick = onComplete)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = style.title,
            tint = Color.White,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = style.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
