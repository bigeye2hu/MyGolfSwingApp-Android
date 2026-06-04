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
 * 主页 Hero 聚焦目标卡（对齐 iOS `V2GoalHeroCard.swift`）。
 *
 * 布局要点（与 iOS 一致）：
 * - `ZStack` + 背景层 + 内容 `VStack(spacing: 12)`，**不要** `matchParentSize` + `weight` 撑满，否则 footer 打卡按钮会被 `clip` 裁切。
 * - Footer：`HStack` 左 Memo/视频 + `Spacer` + 右打卡按钮（按钮 intrinsic 宽度，永不被挤没）。
 */
@Composable
fun V2GoalHeroCard(
    goal: V2Goal,
    linkedMemoCount: Int,
    linkedVideoCount: Int,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
    onComplete: () -> Unit = {},
    onTap: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0xFF2D6A4F).copy(alpha = 0.18f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Transparent)
            .defaultMinSize(minHeight = 210.dp),
    ) {
        GolfScapeBackground(modifier = Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeaderRow(onEdit = onEdit)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTap),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
            }

            FooterRow(
                goal = goal,
                linkedMemoCount = linkedMemoCount,
                linkedVideoCount = linkedVideoCount,
                onComplete = onComplete,
            )
        }
    }
}

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
        // iOS ④ 顶 vignette：底为 transparent
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
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(bottom = 6.dp)
                .weight(1f, fill = false),
        )
    }
}

@Composable
private fun ProgressRow(goal: V2Goal) {
    val ratio = goal.ratio.coerceIn(0.0, 1.0).toFloat()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
        Text(
            text = goal.sessionProgressDisplay,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** iOS `footerRow`：左 Memo/视频，Spacer，右打卡（按钮不被挤压） */
@Composable
private fun FooterRow(
    goal: V2Goal,
    linkedMemoCount: Int,
    linkedVideoCount: Int,
    onComplete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "📚 $linkedMemoCount 条 Memo",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
            )
            Text(
                text = "·",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                letterSpacing = 2.sp,
            )
            Text(
                text = "🎬 $linkedVideoCount 个视频",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        HeroCheckInButton(goal = goal, onComplete = onComplete)
    }
}

/** iOS `checkInButton`：文案与图标始终为白色；仅背景色随状态变化 */
@Composable
private fun HeroCheckInButton(goal: V2Goal, onComplete: () -> Unit) {
    val state = heroButtonState(goal)
    val shape = RoundedCornerShape(12.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .shadow(
                elevation = if (state == HeroButtonState.Active) 6.dp else 0.dp,
                shape = shape,
                spotColor = Gold.copy(alpha = 0.4f),
            )
            .clip(shape)
            .background(state.backgroundColor(Gold))
            .then(
                if (state == HeroButtonState.Active) {
                    Modifier.clickable(onClick = onComplete)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = state.icon,
            contentDescription = state.title,
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = state.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** 对齐 iOS `HeroButtonState` */
private enum class HeroButtonState {
    Active,
    CheckedInToday,
    AllDone,
    Paused,
    ;

    val title: String
        get() = when (this) {
            Active -> "打卡"
            CheckedInToday -> "今日已打"
            AllDone -> "已完成"
            Paused -> "已暂停"
        }

    val icon: ImageVector
        get() = when (this) {
            Active -> Icons.Filled.Check
            CheckedInToday -> Icons.Filled.CheckCircle
            AllDone -> Icons.Filled.EmojiEvents
            Paused -> Icons.Filled.Pause
        }

    fun backgroundColor(gold: Color): Color = when (this) {
        Active -> gold
        CheckedInToday -> Color.White.copy(alpha = 0.18f)
        AllDone -> gold.copy(alpha = 0.55f)
        Paused -> Color.White.copy(alpha = 0.18f)
    }
}

private fun heroButtonState(goal: V2Goal): HeroButtonState = when {
    goal.status == V2Goal.Status.COMPLETED -> HeroButtonState.AllDone
    goal.status == V2Goal.Status.PAUSED -> HeroButtonState.Paused
    goal.hasCheckedInToday -> HeroButtonState.CheckedInToday
    else -> HeroButtonState.Active
}
