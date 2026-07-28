package com.mygolfswing.app.ui.goals

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.memo.V2MemoThumbnail
import java.time.LocalDate
import java.time.LocalDateTime
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
    var showAllTimeline by remember { mutableStateOf(false) }
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
                Brush.verticalGradient(
                    0.0f to Color(0xFFE8F5E9),
                    0.30f to Color(0xFFF1F8F4),
                    0.65f to Color(0xFFF5F5F0),
                    1.0f to Color(0xFFEDE8E0),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailNavBar(goal = goal, onBack = onBack, onEdit = onEdit)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = 20.dp,
                    vertical = DSSpacing.md,
                ),
                verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
            ) {
                item { ProgressCard(goal = goal) }
                item { InfoCard(goal = goal) }
                item { TimelineCard(goal = goal, onSeeAll = { showAllTimeline = true }) }
                item { MemoCard(goal = goal, onManage = { showMemoPicker = true }) }
                item { Spacer(modifier = Modifier.height(112.dp)) }
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

        if (showAllTimeline) {
            GoalTimelineSheet(goal = goal, onDismiss = { showAllTimeline = false })
        }
    }
}

// =================================================================
// 子组件
// =================================================================

@Composable
private fun DetailNavBar(goal: V2Goal, onBack: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = DSSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIcon(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = DSSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = goal.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusDotColor(goal.status)),
                )
                Text(
                    text = goal.status.displayText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusDotColor(goal.status),
                    maxLines = 1,
                )
            }
        }
        CircleIcon(icon = Icons.Filled.Edit, onClick = onEdit)
    }
}

private fun statusDotColor(status: V2Goal.Status): Color = when (status) {
    V2Goal.Status.ACTIVE -> DSColors.fairway
    V2Goal.Status.PAUSED -> DSColors.shadowGray
    V2Goal.Status.COMPLETED -> DSColors.trophyGold
    V2Goal.Status.ARCHIVED -> DSColors.shadowGray
}

@Composable
private fun CircleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(
                elevation = 2.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f),
            )
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.88f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.75f), shape = CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4B5563),
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun InfoCard(goal: V2Goal) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 28.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(DSColors.cardWhite.copy(alpha = 0.88f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.75f), shape = RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("目标信息", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                tint = DSColors.shadowGray,
                modifier = Modifier.size(14.dp),
            )
        }

        GoalInfoRow(label = "球杆类型", trailing = {
            Text(
                text = goal.clubKind?.displayName ?: "—",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(listOf(DSColors.fairway, DSColors.leafGreen)))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        })
        GoalInfoDivider()
        GoalInfoRow(label = "完成方式", trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = metricKindIcon(goal.metric.kind),
                    contentDescription = null,
                    tint = Color(0xFF374151),
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    text = goal.metricKindDisplay,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151),
                )
            }
        })
        GoalInfoDivider()
        GoalInfoRow(label = "目标数量", trailing = { PlainGoalInfoText(goal.targetSessionsDisplay) })
        GoalInfoDivider()
        GoalInfoRow(label = "练习频率", trailing = { PlainGoalInfoText(goal.cadence?.displayText ?: "—") })
        GoalInfoDivider()
        GoalInfoRow(label = "优先级", trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(goal.priority.emoji, fontSize = 14.sp)
                Text(
                    text = goal.priority.shortName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = goalPriorityColor(goal.priority),
                )
            }
        })
        GoalInfoDivider()
        GoalInfoRow(label = "截止日期", trailing = {
            Text(
                text = goal.deadlineDisplay,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = DSColors.shadowGray,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.05f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        })
    }
}

@Composable
private fun GoalInfoRow(label: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280),
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun GoalInfoDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.Black.copy(alpha = 0.05f)),
    )
}

@Composable
private fun PlainGoalInfoText(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
}

private fun metricKindIcon(kind: V2Goal.Metric.Kind): androidx.compose.ui.graphics.vector.ImageVector = when (kind) {
    V2Goal.Metric.Kind.COUNT -> Icons.Filled.Numbers
    V2Goal.Metric.Kind.DURATION -> Icons.Filled.Schedule
    V2Goal.Metric.Kind.DISTANCE -> Icons.Filled.Straighten
    V2Goal.Metric.Kind.FREE -> Icons.Filled.AllInclusive
}

private fun goalPriorityColor(priority: V2Goal.Priority): Color = when (priority) {
    V2Goal.Priority.CRITICAL -> Color(0xFFDC2626)
    V2Goal.Priority.HIGH -> Color(0xFFF97316)
    V2Goal.Priority.NORMAL -> Color(0xFFEAB308)
    V2Goal.Priority.LOW -> Color(0xFF6B7280)
}

@Composable
private fun ProgressCard(goal: V2Goal) {
    val unit = goal.sessionUnit ?: "次"
    val currentText = goal.progress.current.toInt().toString()
    val totalText = goal.targetSessions?.toString() ?: "∞"
    val percentText = if (goal.targetSessions == null && goal.metric.kind == V2Goal.Metric.Kind.FREE) {
        "长期进行中"
    } else {
        "${(goal.ratio * 100).toInt()}%"
    }
    val remainingText = goal.remainingSessions
        ?.takeIf { goal.status == V2Goal.Status.ACTIVE && it > 0 }
        ?.let { "距完成还差 $it $unit" }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 28.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(DSColors.cardWhite.copy(alpha = 0.88f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.75f), shape = RoundedCornerShape(24.dp))
            .padding(horizontal = DSSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProgressRing(
            ratio = goal.ratio,
            currentText = currentText,
            totalText = totalText,
            unit = unit,
            percentText = percentText,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
        )
        remainingText?.let {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.fairway.copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(DSColors.fairway),
                )
                Text(text = it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DSColors.fairway)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black.copy(alpha = 0.06f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressStat(
                icon = Icons.Filled.Check,
                value = "${goal.progress.current.toInt()}$unit",
                label = "已完成",
                modifier = Modifier.weight(1f),
            )
            VerticalDivider()
            ProgressStat(
                icon = Icons.Filled.DateRange,
                value = "${goal.weekCheckInCount}$unit",
                label = "本周完成",
                modifier = Modifier.weight(1f),
            )
            VerticalDivider()
            ProgressStat(
                icon = Icons.Filled.LocalFireDepartment,
                value = "${goal.currentStreakDays}天",
                label = "连续天数",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProgressRing(
    ratio: Double,
    currentText: String,
    totalText: String,
    unit: String,
    percentText: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(160.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arcSize = size.copy(width = size.width - stroke, height = size.height - stroke)
            drawArc(
                color = Color(0xFF73A68C).copy(alpha = 0.18f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(DSColors.fairway, DSColors.leafGreen, DSColors.fairway),
                ),
                startAngle = -90f,
                sweepAngle = (ratio.coerceIn(0.001, 1.0) * 360f).toFloat(),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "$currentText/$totalText",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.inkGreen,
                maxLines = 1,
            )
            Text(text = unit, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DSColors.shadowGray)
            Text(
                text = percentText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.fairway,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.fairway.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun ProgressStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = 74.dp)
            .padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(DSColors.fairway.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = DSColors.fairway, modifier = Modifier.size(12.dp))
        }
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen, maxLines = 1)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = DSColors.shadowGray, maxLines = 1)
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(56.dp)
            .background(Color.Black.copy(alpha = 0.06f)),
    )
}

@Composable
private fun TimelineCard(goal: V2Goal, onSeeAll: () -> Unit) {
    val sorted = goal.progress.checkInDates.sortedDescending()
    val items = sorted.take(3)
    val total = sorted.size
    val unit = goal.sessionUnit ?: "次"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 28.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(DSColors.cardWhite.copy(alpha = 0.88f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.75f), shape = RoundedCornerShape(24.dp))
            .padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("练习记录", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("查看全部", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DSColors.fairway)
                Text("→", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DSColors.fairway)
            }
        }
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("还没有打卡记录", fontSize = 12.sp, color = DSColors.shadowGray)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEachIndexed { index, date ->
                    TimelineRow(
                        date = date,
                        sequenceNo = total - index,
                        unit = unit,
                        dotColor = timelineDotColor(index),
                        isLast = index == items.lastIndex,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalTimelineSheet(goal: V2Goal, onDismiss: () -> Unit) {
    val sorted = goal.progress.checkInDates.sortedDescending()
    val total = sorted.size
    val unit = goal.sessionUnit ?: "次"
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DSSpacing.lg)
                .padding(bottom = DSSpacing.lg)
                .v2NavigationBarPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("全部练习记录", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
                    Text("共 $total 条打卡记录", fontSize = 12.sp, color = DSColors.shadowGray)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DSColors.fairway.copy(alpha = 0.08f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭", tint = DSColors.fairway, modifier = Modifier.size(16.dp))
                }
            }
            if (sorted.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("还没有打卡记录", fontSize = 12.sp, color = DSColors.shadowGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(
                        items = sorted.mapIndexed { index, date ->
                            GoalTimelineEntry(
                                date = date,
                                sequenceNo = total - index,
                                colorIndex = index,
                            )
                        },
                        key = { "${it.sequenceNo}-${it.date}" },
                    ) { entry ->
                        TimelineRow(
                            date = entry.date,
                            sequenceNo = entry.sequenceNo,
                            unit = unit,
                            dotColor = timelineDotColor(entry.colorIndex),
                            isLast = entry.colorIndex == sorted.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

private data class GoalTimelineEntry(
    val date: LocalDateTime,
    val sequenceNo: Int,
    val colorIndex: Int,
)

@Composable
private fun TimelineRow(
    date: LocalDateTime,
    sequenceNo: Int,
    unit: String,
    dotColor: Color,
    isLast: Boolean,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.width(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(1.dp)
                        .height(44.dp)
                        .background(DSColors.fairway.copy(alpha = 0.20f)),
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(DSColors.fairway.copy(alpha = 0.05f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "完成第 $sequenceNo $unit",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151),
                    maxLines = 1,
                )
                Text(
                    text = timelineMetaText(date),
                    fontSize = 11.sp,
                    color = DSColors.shadowGray,
                    maxLines = 1,
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DSColors.fairway.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = DSColors.fairway,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

private fun timelineDotColor(index: Int): Color = when (index % 3) {
    0 -> DSColors.fairway
    1 -> DSColors.leafGreen
    else -> Color(0xFF95D5B2)
}

private fun timelineMetaText(date: LocalDateTime): String {
    val time = date.format(DateTimeFormatter.ofPattern("HH:mm"))
    val today = LocalDate.now()
    return when (date.toLocalDate()) {
        today -> "今天 $time"
        today.minusDays(1) -> "昨天 $time"
        else -> "${date.monthValue}月${date.dayOfMonth}日 $time"
    }
}

@Composable
private fun MemoCard(goal: V2Goal, onManage: () -> Unit) {
    val linked = V2GoalStore.linkedMemos(goal).sortedByDescending { it.createdAt }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 28.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(DSColors.cardWhite.copy(alpha = 0.88f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.75f), shape = RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "关联 Memo 记录",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.inkGreen,
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.fairway.copy(alpha = 0.10f))
                    .border(1.dp, DSColors.fairway.copy(alpha = 0.22f), RoundedCornerShape(50))
                    .clickable(onClick = onManage)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = DSColors.fairway,
                    modifier = Modifier.size(10.dp),
                )
                Text(
                    text = "选择已有 Memo",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DSColors.fairway,
                )
            }
        }
        if (linked.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.EditNote,
                    contentDescription = null,
                    tint = DSColors.shadowGray.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp),
                )
                Text("还没有关联任何 Memo", fontSize = 12.sp, color = DSColors.shadowGray)
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(linked, key = { it.id }) { memo ->
                    GoalDetailMemoThumb(
                        memo = memo,
                        onRemove = { V2GoalStore.unlinkMemo(memo.id, goal.id) },
                    )
                }
            }
        }
        Text("关联的练习记录将作为该目标的参考数据", fontSize = 12.sp, color = DSColors.shadowGray)
    }
}

@Composable
private fun GoalDetailMemoThumb(memo: V2Memo, onRemove: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.60f))
            .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
    ) {
        Box {
            V2MemoThumbnail(
                memo = memo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                showKindPlaceholderIcon = true,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "移除关联",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = memo.goalMemoDisplayTitle(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = memo.createdAt.format(MemoDateFormatter),
                fontSize = 12.sp,
                color = DSColors.shadowGray,
                maxLines = 1,
            )
        }
    }
}

private val MemoDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd")

@Composable
private fun DetailBottomBar(goal: V2Goal) {
    val isActive = goal.status == V2Goal.Status.ACTIVE
    val canPauseToggle = goal.status == V2Goal.Status.ACTIVE || goal.status == V2Goal.Status.PAUSED
    val canCheckIn = isActive && !goal.hasCheckedInToday
    val hintText = when (goal.status) {
        V2Goal.Status.ACTIVE -> "保持节奏，每天一点进步"
        V2Goal.Status.PAUSED -> "目标已暂停，点击下方恢复继续打卡"
        V2Goal.Status.COMPLETED -> "🎉 恭喜！目标已完成"
        V2Goal.Status.ARCHIVED -> "目标已归档"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF1F8F4).copy(alpha = 0f),
                        Color(0xFFF1F8F4),
                        Color(0xFFF1F8F4),
                    ),
                ),
            )
            .v2NavigationBarPadding()
            .padding(horizontal = 20.dp)
            .padding(top = DSSpacing.lg, bottom = DSSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = hintText,
            fontSize = 12.sp,
            color = DSColors.shadowGray,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SecondaryBtn(
                text = when (goal.status) {
                    V2Goal.Status.PAUSED -> "恢复目标"
                    V2Goal.Status.COMPLETED -> "已完成"
                    V2Goal.Status.ARCHIVED -> "已归档"
                    else -> "暂停目标"
                },
                icon = if (goal.status == V2Goal.Status.PAUSED) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                enabled = canPauseToggle,
                onClick = {
                    if (isActive) V2GoalStore.pause(goal.id) else V2GoalStore.resume(goal.id)
                },
                modifier = Modifier.weight(1f),
            )
            PrimaryBtn(
                text = when {
                    goal.status == V2Goal.Status.COMPLETED -> "已完成"
                    goal.status == V2Goal.Status.ARCHIVED -> "已归档"
                    goal.hasCheckedInToday -> "今日已练"
                    isActive -> "继续练习"
                    else -> "已暂停"
                },
                icon = when {
                    goal.status == V2Goal.Status.COMPLETED -> Icons.Filled.EmojiEvents
                    goal.hasCheckedInToday -> Icons.Filled.CheckCircle
                    else -> Icons.Filled.ArrowCircleRight
                },
                enabled = canCheckIn,
                onClick = { V2GoalStore.checkIn(goal.id) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SecondaryBtn(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.88f))
            .border(1.dp, Color.Black.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .alpha(if (enabled) 1f else 0.5f)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(12.dp),
        )
        Spacer(modifier = Modifier.width(DSSpacing.sm))
        Text(
            text = text,
            color = Color(0xFF6B7280),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
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
            .height(52.dp)
            .shadow(
                elevation = if (enabled) 11.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = DSColors.fairway.copy(alpha = 0.40f),
                spotColor = DSColors.fairway.copy(alpha = 0.40f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
