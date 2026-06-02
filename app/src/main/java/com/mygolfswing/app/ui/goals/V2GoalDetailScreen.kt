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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * 目标详情页（对齐 iOS `V2GoalDetailView`）。
 */
@Composable
fun V2GoalDetailScreen(
    goalId: UUID,
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {},
) {
    var showMemoPicker by remember { mutableStateOf(false) }
    val goals by V2GoalStore.goals.collectAsState()
    val goal = goals.firstOrNull { it.id == goalId }

    if (goal == null) {
        V2MissingEntityScreen(message = "目标不存在", onBack = onBack, title = "目标详情")
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    0.0f to Color(0xFFF0F8F4),
                    0.4f to Color(0xFFF5F5F0),
                    1.0f to Color(0xFFFAF7F0),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailNavBar(onBack = onBack, onEdit = onEdit)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = DSSpacing.lg,
                    vertical = DSSpacing.md,
                ),
                verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
            ) {
                item { InfoCard(goal = goal) }
                item { ProgressCard(goal = goal) }
                item { TimelineCard(goal = goal) }
                item { MemoCard(goal = goal, onManage = { showMemoPicker = true }) }
                item { Spacer(modifier = Modifier.height(96.dp)) }
            }

            DetailBottomBar(goal = goal)
        }

        if (showMemoPicker) {
            V2GoalMemoPickerSheet(
                selectedIds = goal.derivedFromMemoIds.toSet(),
                onDismiss = { showMemoPicker = false },
                onConfirm = { ids ->
                    V2GoalStore.replaceLinkedMemos(goal.id, ids.toList())
                    showMemoPicker = false
                },
            )
        }
    }
}

// =================================================================
// 子组件
// =================================================================

@Composable
private fun DetailNavBar(onBack: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .background(DSColors.cardWhite.copy(alpha = 0.92f))
            .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIcon(icon = Icons.Filled.ArrowBack, onClick = onBack)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "目标详情", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
        Spacer(modifier = Modifier.weight(1f))
        CircleIcon(icon = Icons.Filled.MoreHoriz, onClick = onEdit)
    }
}

@Composable
private fun CircleIcon(
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

@Composable
private fun InfoCard(goal: V2Goal) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF1B4332), Color(0xFF2D6A4F))),
            )
            .padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "${goal.priority.emoji} ${goal.priority.shortName}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFFE082),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Text(
                text = goal.topic.displayName,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.10f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = goal.status.displayText,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.75f),
            )
        }
        Text(
            text = goal.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = "${goal.typeLabel} · ${goal.metricDisplay ?: "自由练习"}",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.85f),
        )
        goal.cadence?.let { c ->
            Text(text = "节奏：${c.displayText}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
        }
        Text(text = "截止：${goal.deadlineDisplay}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun ProgressCard(goal: V2Goal) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(DSColors.cardWhite.copy(alpha = 0.92f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(20.dp))
            .padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("整体进度", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = goal.sessionProgressDisplay,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.fairway,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(DSColors.fairway.copy(alpha = 0.12f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(goal.ratio.toFloat())
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(DSColors.fairway, DSColors.leafGreen))),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressStat(value = "${goal.currentStreakDays}", label = "连续天数")
            ProgressStat(value = "${goal.weekCheckInCount}", label = "本周打卡")
            ProgressStat(value = goal.remainingSessions?.toString() ?: "∞", label = "剩余")
        }
    }
}

@Composable
private fun ProgressStat(value: String, label: String) {
    Column(
        modifier = Modifier
            .padding(end = DSSpacing.lg),
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
        Text(label, fontSize = 11.sp, color = DSColors.shadowGray)
    }
}

@Composable
private fun TimelineCard(goal: V2Goal) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(DSColors.cardWhite.copy(alpha = 0.92f))
            .padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("打卡时间线", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
        if (goal.progress.checkInDates.isEmpty()) {
            Text("还没有打卡记录，今天开始 →", fontSize = 12.sp, color = DSColors.shadowGray)
        } else {
            val fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm")
            goal.progress.checkInDates
                .sortedDescending()
                .take(8)
                .forEach { d ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DSColors.fairway),
                        )
                        Text(fmt.format(d), fontSize = 13.sp, color = DSColors.inkGreen)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("+1 次", fontSize = 12.sp, color = DSColors.shadowGray)
                    }
                }
        }
    }
}

@Composable
private fun MemoCard(goal: V2Goal, onManage: () -> Unit) {
    val linked = V2GoalStore.linkedMemos(goal)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(DSColors.cardWhite.copy(alpha = 0.92f))
            .padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.EditNote,
                contentDescription = null,
                tint = DSColors.fairway,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "  关联的 Memo",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.inkGreen,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (linked.isEmpty()) "关联" else "管理",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.fairway,
                modifier = Modifier.clickable(onClick = onManage),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "${linked.size}", fontSize = 13.sp, color = DSColors.shadowGray)
        }
        if (linked.isEmpty()) {
            Text("还没有关联 Memo，记录练习心得后可手动关联", fontSize = 12.sp, color = DSColors.shadowGray)
        } else {
            linked.take(5).forEach { m ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = m.displayTitle ?: m.title ?: "无标题",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DSColors.inkGreen,
                    )
                    m.text?.takeIf { it.isNotBlank() }?.let {
                        Text(text = it, fontSize = 12.sp, color = DSColors.shadowGray, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailBottomBar(goal: V2Goal) {
    val isActive = goal.status == V2Goal.Status.ACTIVE
    val canCheckIn = isActive && !goal.hasCheckedInToday
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.cardWhite.copy(alpha = 0.96f))
            .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SecondaryBtn(
            text = if (isActive) "暂停" else "继续",
            icon = if (isActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            onClick = {
                if (isActive) V2GoalStore.pause(goal.id) else V2GoalStore.resume(goal.id)
            },
            modifier = Modifier.weight(1f),
        )
        PrimaryBtn(
            text = when {
                goal.status == V2Goal.Status.COMPLETED -> "已完成"
                goal.hasCheckedInToday -> "今日已打"
                isActive -> "打卡 +1"
                else -> "已暂停"
            },
            icon = when {
                goal.status == V2Goal.Status.COMPLETED -> Icons.Filled.EmojiEvents
                goal.hasCheckedInToday -> Icons.Filled.CheckCircle
                else -> Icons.Filled.CheckCircle
            },
            enabled = canCheckIn,
            onClick = { V2GoalStore.checkIn(goal.id) },
            modifier = Modifier.weight(1.4f),
        )
    }
}

@Composable
private fun SecondaryBtn(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.fairway.copy(alpha = 0.08f))
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DSColors.fairway,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = DSColors.fairway, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PrimaryBtn(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (enabled) Brush.horizontalGradient(listOf(DSColors.fairway, DSColors.leafGreen))
    else Brush.horizontalGradient(listOf(DSColors.shadowGray.copy(alpha = 0.5f), DSColors.shadowGray.copy(alpha = 0.4f)))
    Row(
        modifier = modifier
            .height(48.dp)
            .shadow(elevation = if (enabled) 8.dp else 0.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
