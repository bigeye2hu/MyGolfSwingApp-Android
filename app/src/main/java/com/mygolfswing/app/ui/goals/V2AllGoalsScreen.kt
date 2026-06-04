package com.mygolfswing.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import java.util.UUID

/**
 * 「全部目标」页主视图（对齐 iOS `V2AllGoalsView`）。
 */
@Composable
fun V2AllGoalsScreen(
    onBack: () -> Unit = {},
    onCreate: () -> Unit = {},
    onSelectGoal: (UUID) -> Unit = {},
) {
    val goals by V2GoalStore.goals.collectAsState()
    var completedExpanded by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    0.0f to Color(0xFFF0F8F4),
                    0.3f to Color(0xFFE8F3EF),
                    0.7f to Color(0xFFF5F5F0),
                    1.0f to Color(0xFFFAF7F0),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AllGoalsNavBar(onBack = onBack, onCreate = onCreate)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(DSSpacing.sm),
            ) {
                item {
                    Box(
                        modifier = Modifier.padding(
                            horizontal = DSSpacing.lg,
                            vertical = DSSpacing.md,
                        ),
                    ) {
                        AllGoalsStatsBento(
                            activeCount = V2GoalStore.activeCount,
                            completedCount = V2GoalStore.completedCount,
                            weeklyCheckIn = V2GoalStore.weeklyCheckInCount,
                            totalMemo = V2GoalStore.totalLinkedMemoCount,
                        )
                    }
                }

                V2GoalStore.groupedActiveByPriority.forEach { group ->
                    item {
                        AllGoalsPriorityHeader(
                            priority = group.priority,
                            count = group.goals.size,
                            modifier = Modifier.padding(
                                horizontal = DSSpacing.lg,
                                vertical = DSSpacing.sm,
                            ),
                        )
                    }
                    items(group.goals, key = { it.id }) { goal ->
                        AllGoalsCard(
                            goal = goal,
                            linkedMemoCount = V2GoalStore.linkedMemoCount(goal),
                            onCheckIn = { V2GoalStore.checkIn(goal.id) },
                            onTap = { onSelectGoal(goal.id) },
                            modifier = Modifier.padding(
                                horizontal = DSSpacing.lg,
                                vertical = 4.dp,
                            ),
                        )
                    }
                }

                if (V2GoalStore.completedCount > 0) {
                    item {
                        AllGoalsCompletedRow(
                            count = V2GoalStore.completedCount,
                            expanded = completedExpanded,
                            onToggle = { completedExpanded = !completedExpanded },
                            modifier = Modifier.padding(
                                horizontal = DSSpacing.lg,
                                vertical = DSSpacing.sm,
                            ),
                        )
                    }
                    if (completedExpanded) {
                        items(V2GoalStore.completedGoals, key = { it.id }) { goal ->
                            Box(
                                modifier = Modifier.padding(
                                    horizontal = DSSpacing.lg,
                                    vertical = 4.dp,
                                ),
                            ) {
                                AllGoalsCard(
                                    goal = goal,
                                    linkedMemoCount = V2GoalStore.linkedMemoCount(goal),
                                    onCheckIn = null,
                                    onTap = { onSelectGoal(goal.id) },
                                    dimmed = true,
                                )
                            }
                        }
                    }
                }
            }
        }

        AllGoalsCreateFab(
            onTap = onCreate,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = DSSpacing.xl),
        )
    }
}

// =================================================================
// NavBar
// =================================================================

@Composable
private fun AllGoalsNavBar(onBack: () -> Unit, onCreate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .background(DSColors.cardWhite.copy(alpha = 0.92f))
            .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "全部目标",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = DSColors.inkGreen,
        )
        Spacer(modifier = Modifier.weight(1f))
        CircleIconButton(icon = Icons.Filled.Add, onClick = onCreate)
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(DSColors.fairway.copy(alpha = 0.08f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DSColors.fairway,
            modifier = Modifier.size(16.dp),
        )
    }
}

// =================================================================
// Stats Bento
// =================================================================

@Composable
private fun AllGoalsStatsBento(
    activeCount: Int,
    completedCount: Int,
    weeklyCheckIn: Int,
    totalMemo: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(DSSpacing.Radius.md))
            .clip(RoundedCornerShape(DSSpacing.Radius.md))
            .background(DSColors.cardWhite.copy(alpha = 0.78f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(DSSpacing.Radius.md))
            .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatColumn(value = "$activeCount", label = "进行中", color = DSColors.fairway, modifier = Modifier.weight(1f))
        StatDivider()
        StatColumn(value = "$completedCount", label = "已完成", color = Color(0xFF9CA3AF), modifier = Modifier.weight(1f))
        StatDivider()
        StatColumn(value = "$weeklyCheckIn", label = "本周打卡", color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
        StatDivider()
        StatColumn(value = "$totalMemo", label = "累计Memo", color = DSColors.leafGreen, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatColumn(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 12.sp, color = Color(0xFF6C757D))
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(Color(0xFFE5E7EB)),
    )
}

// =================================================================
// Priority Header
// =================================================================

@Composable
private fun AllGoalsPriorityHeader(
    priority: V2Goal.Priority,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val dot = priorityDotColor(priority)
    val div = priorityDividerColor(priority)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DSSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Text(
            text = priority.displayName,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4B5563),
        )
        Text(
            text = "${count}个目标",
            fontSize = 12.sp,
            color = Color(0xFF9CA3AF),
        )
        Box(
            modifier = Modifier
                .height(1.dp)
                .weight(1f)
                .background(div),
        )
    }
}

private fun priorityDotColor(p: V2Goal.Priority): Color = when (p) {
    V2Goal.Priority.CRITICAL -> Color(0xFFEF4444)
    V2Goal.Priority.HIGH -> Color(0xFFFBBF24)
    V2Goal.Priority.NORMAL -> Color(0xFF4ADE80)
    V2Goal.Priority.LOW -> Color(0xFF9CA3AF)
}

private fun priorityDividerColor(p: V2Goal.Priority): Color = when (p) {
    V2Goal.Priority.CRITICAL -> Color(0xFFFEE2E2)
    V2Goal.Priority.HIGH -> Color(0xFFFEF3C7)
    V2Goal.Priority.NORMAL -> Color(0xFFDCFCE7)
    V2Goal.Priority.LOW -> Color(0xFFF3F4F6)
}

// =================================================================
// Goal Card
// =================================================================

@Composable
private fun AllGoalsCard(
    goal: V2Goal,
    linkedMemoCount: Int,
    onCheckIn: (() -> Unit)?,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(DSSpacing.Radius.md))
            .clip(RoundedCornerShape(DSSpacing.Radius.md))
            .background(DSColors.cardWhite.copy(alpha = if (dimmed) 0.55f else 0.78f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(DSSpacing.Radius.md))
            .clickable { onTap() }
            .padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            CardTitleBlock(goal = goal, modifier = Modifier.weight(1f))
            if (onCheckIn != null) {
                CardCheckInButton(goal = goal, onClick = onCheckIn)
            }
        }

        CardProgressBar(goal = goal)

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MemoCountChip(count = linkedMemoCount)
            Spacer(modifier = Modifier.weight(1f))
            val trailing = if (goal.metric.kind == V2Goal.Metric.Kind.FREE) {
                "永久目标"
            } else {
                "完成度 ${(goal.ratio * 100).toInt()}%"
            }
            Text(text = trailing, fontSize = 12.sp, color = Color(0xFF9CA3AF))
        }
    }
}

@Composable
private fun CardTitleBlock(goal: V2Goal, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(priorityDotColor(goal.priority)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = goal.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DSColors.inkGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                TopicChip(text = goal.topic.displayName)
            }
            MetaLine(goal = goal)
        }
    }
}

@Composable
private fun TopicChip(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF40916C),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DSColors.fairway.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun MetaLine(goal: V2Goal) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        goal.cadence?.let { c ->
            Text(text = c.displayText, fontSize = 12.sp, color = Color(0xFF6C757D))
            Text("·", fontSize = 12.sp, color = Color(0xFFD1D5DB))
        }
        Text(
            text = goal.sessionProgressDisplay,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = DSColors.fairway,
        )
        StatusChip(goal = goal)
    }
}

@Composable
private fun StatusChip(goal: V2Goal) {
    val (text, bg, fg) = when {
        goal.status == V2Goal.Status.COMPLETED -> Triple("已完成", Color(0xFFEDEFF2), Color(0xFF6C757D))
        goal.hasCheckedInToday -> Triple("今日已打", Color(0xFFFFF3C8), Color(0xFFB7800F))
        goal.metric.kind == V2Goal.Metric.Kind.FREE -> Triple("持续进行", Color(0xFFEFFCEC), Color(0xFF179C49))
        else -> Triple("进行中", Color(0xFFEFF3FE), Color(0xFF3B82F6))
    }
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun CardCheckInButton(goal: V2Goal, onClick: () -> Unit) {
    val enabled = goal.status == V2Goal.Status.ACTIVE && !goal.hasCheckedInToday
    val bg = when {
        goal.status == V2Goal.Status.COMPLETED -> Color(0xFFFFF3C8)
        goal.status == V2Goal.Status.PAUSED -> Color(0xFFEDEFF2)
        goal.hasCheckedInToday -> Color(0xFFEFFCEC)
        else -> DSColors.fairway
    }
    val fg = if (enabled) Color.White else when {
        goal.status == V2Goal.Status.COMPLETED -> Color(0xFFB7800F)
        goal.hasCheckedInToday -> DSColors.fairway
        else -> Color(0xFF6C757D)
    }
    val title = when {
        goal.status == V2Goal.Status.COMPLETED -> "完成"
        goal.status == V2Goal.Status.PAUSED -> "暂停"
        goal.hasCheckedInToday -> "已打"
        else -> "打卡"
    }

    Column(
        modifier = Modifier
            .size(48.dp)
            .shadow(elevation = if (enabled) 4.dp else 0.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun CardProgressBar(goal: V2Goal) {
    val ratio = (if (goal.metric.kind == V2Goal.Metric.Kind.FREE) 1.0 else goal.ratio).toFloat()
    val colors = when (goal.priority) {
        V2Goal.Priority.HIGH -> listOf(Color(0xFFFBBF24), Color(0xFFFCD34D))
        else -> listOf(DSColors.fairway, DSColors.leafGreen)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(DSColors.fairway.copy(alpha = 0.12f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(ratio)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(colors)),
        )
    }
}

@Composable
private fun MemoCountChip(count: Int) {
    val active = count > 0
    val bg = if (active) DSColors.fairway.copy(alpha = 0.10f) else Color(0xFFE0E3E7)
    val fg = if (active) DSColors.fairway else Color(0xFF9CA3AF)
    val text = if (active) "${count}条Memo" else "暂无Memo"
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

// =================================================================
// Completed Row
// =================================================================

@Composable
private fun AllGoalsCompletedRow(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(DSSpacing.Radius.md))
            .clip(RoundedCornerShape(DSSpacing.Radius.md))
            .background(DSColors.cardWhite.copy(alpha = 0.65f))
            .border(
                width = 1.dp,
                color = Color(0xFFC7DBD0),
                shape = RoundedCornerShape(DSSpacing.Radius.md),
            )
            .clickable { onToggle() }
            .padding(horizontal = DSSpacing.lg, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E3E7)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = "已完成",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6C757D),
            )
            Text(
                text = "$count",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF9CA3AF),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFF2F3F5))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (expanded) "点击收起" else "点击展开",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF),
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// =================================================================
// Create FAB
// =================================================================

@Composable
private fun AllGoalsCreateFab(onTap: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .shadow(elevation = 14.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(listOf(DSColors.fairway, DSColors.leafGreen)),
            )
            .clickable { onTap() }
            .padding(horizontal = DSSpacing.xl, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DSSpacing.sm),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Text(text = "新建目标", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
