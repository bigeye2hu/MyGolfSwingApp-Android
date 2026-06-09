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
private val TwilightBlueAccent = Color(0xFF38BDF8)
private val WarmSunsetAccent = Color(0xFFF97316)
private val MistyDawnAccent = Color(0xFF86EFAC)

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
    style: V2GoalHeroStyle = V2GoalHeroStyle.ClassicFairway,
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
                shape = RoundedCornerShape(style.cornerRadius),
                spotColor = style.shadowColor,
            )
            .clip(RoundedCornerShape(style.cornerRadius))
            .border(width = 1.dp, color = style.borderColor, shape = RoundedCornerShape(style.cornerRadius))
            .background(Color.Transparent)
            .height(206.dp),
    ) {
        GolfScapeBackground(style = style, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HeaderRow(style = style, onEdit = onEdit)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTap),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = goal.title,
                    color = style.primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                MetricRow(goal = goal, style = style)
                ProgressRow(goal = goal, style = style)
            }

            FooterRow(
                goal = goal,
                style = style,
                linkedMemoCount = linkedMemoCount,
                linkedVideoCount = linkedVideoCount,
                onComplete = onComplete,
            )
        }
    }
}

@Composable
private fun GolfScapeBackground(style: V2GoalHeroStyle, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        when (style) {
            V2GoalHeroStyle.ClassicFairway -> {
                val gradientStart = Offset(constraints.maxWidth * 0.25f, constraints.maxHeight.toFloat())
                val gradientEnd = Offset(constraints.maxWidth * 0.75f, 0f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1B4332), Color(0xFF2D6A4F)),
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
                                    Color(0xFFD8E2DC).copy(alpha = 0.18f),
                                    Color(0xFF2D6A4F).copy(alpha = 0.5f),
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
                                0.0f to Color(0xFF1B4332).copy(alpha = 0.72f),
                                0.55f to Color(0xFF1B4332).copy(alpha = 0.10f),
                                1.0f to Color.Transparent,
                            ),
                        ),
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = style.backgroundColors,
                                start = Offset.Zero,
                                end = Offset(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(style.glowColor, Color.Transparent),
                                center = Offset(
                                    x = constraints.maxWidth * style.glowCenterX,
                                    y = constraints.maxHeight * style.glowCenterY,
                                ),
                                radius = constraints.maxWidth.coerceAtLeast(constraints.maxHeight) * 0.8f,
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(style.verticalOverlay),
                        ),
                )
                style.topWash?.let { wash ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(wash, Color.Transparent),
                                ),
                            ),
                    )
                }
                style.bottomWash?.let { wash ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, wash),
                                ),
                            ),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)),
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(style: V2GoalHeroStyle, onEdit: () -> Unit) {
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
            fontSize = 13.sp,
            color = style.secondaryText,
            modifier = Modifier.clickable { onEdit() },
        )
    }
}

@Composable
private fun MetricRow(goal: V2Goal, style: V2GoalHeroStyle) {
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
            color = style.primaryText,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.125.sp,
        )
        if (unitText != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = unitText,
                color = style.mutedText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 7.sp,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(26.dp)
                .background(style.accent.copy(alpha = 0.5f)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = subtitleText,
            color = style.secondaryText,
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
private fun ProgressRow(goal: V2Goal, style: V2GoalHeroStyle) {
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
                .background(style.progressTrack),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(maxWidth * ratio)
                    .clip(RoundedCornerShape(50))
                    .background(style.accent),
            )
        }
        Text(
            text = goal.sessionProgressDisplay,
            color = style.primaryText,
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
    style: V2GoalHeroStyle,
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
                color = style.secondaryText,
                fontSize = 12.sp,
                maxLines = 1,
            )
            Text(
                text = "·",
                color = style.mutedText,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
            )
            Text(
                text = "🎬 $linkedVideoCount 个视频",
                color = style.secondaryText,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        HeroCheckInButton(goal = goal, style = style, onComplete = onComplete)
    }
}

/** iOS `checkInButton`：文案与图标始终为白色；仅背景色随状态变化 */
@Composable
private fun HeroCheckInButton(goal: V2Goal, style: V2GoalHeroStyle, onComplete: () -> Unit) {
    val state = heroButtonState(goal)
    val shape = RoundedCornerShape(12.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .shadow(
                elevation = if (state == HeroButtonState.Active) 6.dp else 0.dp,
                shape = shape,
                spotColor = style.buttonShadow,
            )
            .clip(shape)
            .background(state.backgroundColor(style))
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
            tint = style.buttonText,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = state.title,
            color = style.buttonText,
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

    fun backgroundColor(style: V2GoalHeroStyle): Color = when (this) {
        Active -> style.accent
        CheckedInToday -> Color.White.copy(alpha = 0.18f)
        AllDone -> style.accent.copy(alpha = 0.55f)
        Paused -> Color.White.copy(alpha = 0.18f)
    }
}

private fun heroButtonState(goal: V2Goal): HeroButtonState = when {
    goal.status == V2Goal.Status.COMPLETED -> HeroButtonState.AllDone
    goal.status == V2Goal.Status.PAUSED -> HeroButtonState.Paused
    goal.hasCheckedInToday -> HeroButtonState.CheckedInToday
    else -> HeroButtonState.Active
}

enum class V2GoalHeroStyle {
    TwilightBlue,
    WarmSunset,
    MistyDawn,
    ClassicFairway,
    ;

    val accent: Color
        get() = when (this) {
            TwilightBlue -> TwilightBlueAccent
            WarmSunset -> WarmSunsetAccent
            MistyDawn -> MistyDawnAccent
            ClassicFairway -> Gold
        }

    val primaryText: Color
        get() = when (this) {
            WarmSunset -> Color(0xFFFFF5E8)
            MistyDawn -> Color(0xFFF0FFF4)
            else -> Color.White
        }

    val secondaryText: Color
        get() = when (this) {
            ClassicFairway -> Color.White.copy(alpha = 0.68f)
            TwilightBlue -> Color.White.copy(alpha = 0.75f)
            WarmSunset -> Color(0xFFFFF0DB).copy(alpha = 0.72f)
            MistyDawn -> Color(0xFFE6FFE8).copy(alpha = 0.68f)
        }

    val mutedText: Color
        get() = when (this) {
            ClassicFairway -> Color.White.copy(alpha = 0.48f)
            TwilightBlue -> Color.White.copy(alpha = 0.55f)
            WarmSunset -> Color(0xFFFFF0DB).copy(alpha = 0.52f)
            MistyDawn -> Color(0xFFE6FFE8).copy(alpha = 0.48f)
        }

    val buttonText: Color
        get() = when (this) {
            MistyDawn -> Color(0xFF1E2218)
            else -> Color.White
        }

    val progressTrack: Color
        get() = when (this) {
            ClassicFairway -> Color.White.copy(alpha = 0.18f)
            TwilightBlue -> Color.White.copy(alpha = 0.22f)
            WarmSunset -> Color.White.copy(alpha = 0.20f)
            MistyDawn -> Color(0xFFD8E2DC).copy(alpha = 0.26f)
        }

    val borderColor: Color
        get() = when (this) {
            ClassicFairway -> Color.White.copy(alpha = 0.12f)
            else -> Color.White.copy(alpha = 0.08f)
        }

    val shadowColor: Color
        get() = when (this) {
            TwilightBlue -> Color(0xFF0A2A5E).copy(alpha = 0.60f)
            WarmSunset -> Color(0xFF7B2D00).copy(alpha = 0.60f)
            MistyDawn -> Color(0xFF2D3D2A).copy(alpha = 0.55f)
            ClassicFairway -> Color(0xFF2D6A4F).copy(alpha = 0.24f)
        }

    val buttonShadow: Color
        get() = when (this) {
            ClassicFairway -> Gold.copy(alpha = 0.40f)
            TwilightBlue -> TwilightBlueAccent.copy(alpha = 0.45f)
            WarmSunset -> WarmSunsetAccent.copy(alpha = 0.45f)
            MistyDawn -> MistyDawnAccent.copy(alpha = 0.40f)
        }

    val backgroundColors: List<Color>
        get() = when (this) {
            TwilightBlue -> listOf(Color(0xFF0A2A5E), Color(0xFF1565C0), Color(0xFF0A1E4A))
            WarmSunset -> listOf(Color(0xFF7B2D00), Color(0xFFC75000), Color(0xFF4A1800))
            MistyDawn -> listOf(Color(0xFF2D3D2A), Color(0xFF4A5C42), Color(0xFF1E2E1C))
            ClassicFairway -> listOf(Color(0xFF1B4332), Color(0xFF2D6A4F))
        }

    val verticalOverlay: List<Color>
        get() = when (this) {
            TwilightBlue -> listOf(Color(0xFF0A1E4A).copy(alpha = 0.88f), Color(0xFF1565C0).copy(alpha = 0.20f), Color(0xFF0A2A5E).copy(alpha = 0.75f))
            WarmSunset -> listOf(Color(0xFF7B2D00).copy(alpha = 0.88f), Color(0xFFC75000).copy(alpha = 0.18f), Color(0xFF3C0A50).copy(alpha = 0.65f))
            MistyDawn -> listOf(Color(0xFF1E2E1C).copy(alpha = 0.88f), Color(0xFF4A5C42).copy(alpha = 0.15f), Color(0xFF2D3D2A).copy(alpha = 0.72f))
            ClassicFairway -> emptyList()
        }

    val glowColor: Color
        get() = when (this) {
            TwilightBlue -> TwilightBlueAccent.copy(alpha = 0.24f)
            WarmSunset -> Color(0xFFFFA028).copy(alpha = 0.32f)
            MistyDawn -> MistyDawnAccent.copy(alpha = 0.14f)
            ClassicFairway -> Color.Transparent
        }

    val glowCenterX: Float
        get() = when (this) {
            MistyDawn, WarmSunset -> 0.5f
            else -> 0.5f
        }

    val glowCenterY: Float
        get() = when (this) {
            TwilightBlue -> 0.85f
            WarmSunset -> 0.55f
            MistyDawn -> 0.4f
            ClassicFairway -> 0.5f
        }

    val topWash: Color?
        get() = when (this) {
            MistyDawn -> Color(0xFFB0BEC5).copy(alpha = 0.18f)
            else -> null
        }

    val bottomWash: Color?
        get() = when (this) {
            WarmSunset -> Color(0xFF46145A).copy(alpha = 0.55f)
            MistyDawn -> MistyDawnAccent.copy(alpha = 0.10f)
            else -> null
        }

    val cornerRadius = 16.dp

    companion object {
        fun cycled(index: Int): V2GoalHeroStyle {
            val styles = listOf(ClassicFairway, TwilightBlue, WarmSunset, MistyDawn)
            val normalized = ((index % styles.size) + styles.size) % styles.size
            return styles[normalized]
        }
    }
}
