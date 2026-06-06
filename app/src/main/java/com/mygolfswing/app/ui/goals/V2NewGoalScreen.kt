package com.mygolfswing.app.ui.goals

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.model.V2Topic
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import java.util.UUID

/**
 * 新建 / 编辑目标页（对齐 iOS `V2NewGoalView` + `V2EditGoalView`）。
 *
 * 当前阶段：合并新建 / 编辑为同一页面。
 * - editingGoalId == null：新建
 * - 否则：编辑（保存写回 store）
 */
@Composable
fun V2NewGoalScreen(
    editingGoalId: UUID? = null,
    onClose: () -> Unit = {},
    onDeleted: () -> Unit = {},
) {
    val existing = editingGoalId?.let { id -> V2GoalStore.goals.value.firstOrNull { it.id == id } }
    val isEditMode = existing != null

    var name by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var clubKind by remember { mutableStateOf(existing?.clubKind ?: V2Goal.ClubKind.DRIVER) }
    var metricKind by remember { mutableStateOf(existing?.metric?.kind ?: V2Goal.Metric.Kind.COUNT) }
    var countSubKind by remember { mutableStateOf(countSubKindOf(existing?.metric)) }
    var targetAmount by rememberSaveable {
        mutableDoubleStateOf(
            existing?.metric?.target ?: defaultTargetFor(V2Goal.Metric.Kind.COUNT, CountSubKind.TIMES),
        )
    }
    var cadenceCategory by remember { mutableStateOf(cadenceCategoryOf(existing?.cadence) ?: CadenceCategory.PER_WEEK) }
    var perCountValue by rememberSaveable {
        mutableIntStateOf((existing?.cadence as? V2Goal.Cadence.PerWeek)?.times ?: 10)
    }
    var rangeMonths by rememberSaveable {
        mutableIntStateOf((existing?.cadence as? V2Goal.Cadence.WithinMonths)?.months ?: 1)
    }
    var priority by remember { mutableStateOf(existing?.priority ?: V2Goal.Priority.CRITICAL) }
    var targetSessions by rememberSaveable {
        mutableIntStateOf(existing?.targetSessions ?: 10)
    }
    var linkedMemoIds by remember {
        mutableStateOf(existing?.derivedFromMemoIds?.toSet() ?: emptySet())
    }
    var showMemoPicker by remember { mutableStateOf(false) }

    val canSubmit = name.trim().isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    0.0f to Color(0xFFE8F4EA),
                    0.3f to Color(0xFFF1F8F2),
                    0.65f to Color(0xFFF6F6EF),
                    1.0f to Color(0xFFEDE8E0),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NewGoalNavBar(
                title = if (isEditMode) "编辑目标" else "新建目标",
                canSave = canSubmit,
                onClose = onClose,
                onSave = {
                    handleSave(
                        existing = existing,
                        name = name,
                        clubKind = clubKind,
                        metricKind = metricKind,
                        countSubKind = countSubKind,
                        targetAmount = targetAmount,
                        cadenceCategory = cadenceCategory,
                        perCountValue = perCountValue,
                        rangeMonths = rangeMonths,
                        priority = priority,
                        targetSessions = targetSessions,
                        linkedMemoIds = linkedMemoIds.toList(),
                    )
                    onClose()
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
                verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
            ) {
                if (!isEditMode) {
                    AIBanner()
                }

                FormCard {
                    NameField(text = name, onTextChange = { name = it })
                    DividerLine()
                    ClubChipsField(selection = clubKind, onSelect = { clubKind = it })
                    DividerLine()
                    if (existing != null) {
                        LockedMetricRow(goal = existing)
                        DividerLine()
                        TargetStepperField(
                            kind = existing.metric.kind,
                            countSubKind = countSubKind,
                            amount = targetAmount,
                            onAmountChange = { targetAmount = it },
                        )
                        DividerLine()
                        LockedCadenceRow(cadence = existing.cadence ?: V2Goal.Cadence.Continuous)
                    } else {
                        MetricKindField(
                            selection = metricKind,
                            countSubKind = countSubKind,
                            onSelect = {
                                metricKind = it
                                if (it != V2Goal.Metric.Kind.COUNT) countSubKind = CountSubKind.TIMES
                                targetAmount = defaultTargetFor(it, countSubKind)
                            },
                            onCountSubKind = {
                                countSubKind = it
                                metricKind = V2Goal.Metric.Kind.COUNT
                                targetAmount = defaultTargetFor(V2Goal.Metric.Kind.COUNT, it)
                            },
                        )
                        DividerLine()
                        TargetStepperField(
                            kind = metricKind,
                            countSubKind = countSubKind,
                            amount = targetAmount,
                            onAmountChange = { targetAmount = it },
                        )
                        DividerLine()
                        CadenceField(
                            category = cadenceCategory,
                            perCount = perCountValue,
                            months = rangeMonths,
                            onCategory = { cadenceCategory = it },
                            onPerCount = { perCountValue = it },
                            onMonths = { rangeMonths = it },
                        )
                    }
                    DividerLine()
                    TargetSessionsField(
                        value = targetSessions,
                        onChange = { targetSessions = it },
                    )
                    DividerLine()
                    PriorityField(selection = priority, onSelect = { priority = it })
                }

                MemoLinkSection(count = linkedMemoIds.size, onTap = { showMemoPicker = true })

                SaveButton(
                    canSubmit = canSubmit,
                    isEditMode = isEditMode,
                    onSave = {
                        handleSave(
                            existing = existing,
                            name = name,
                            clubKind = clubKind,
                            metricKind = metricKind,
                            countSubKind = countSubKind,
                            targetAmount = targetAmount,
                            cadenceCategory = cadenceCategory,
                            perCountValue = perCountValue,
                            rangeMonths = rangeMonths,
                            priority = priority,
                            targetSessions = targetSessions,
                            linkedMemoIds = linkedMemoIds.toList(),
                        )
                        onClose()
                    },
                )

                if (existing != null) {
                    DeleteButton(
                        onDelete = {
                            V2GoalStore.delete(existing.id)
                            onDeleted()
                        },
                    )
                }
            }
        }

        if (showMemoPicker) {
            V2GoalMemoPickerSheet(
                selectedIds = linkedMemoIds,
                onDismiss = { showMemoPicker = false },
                onConfirm = { linkedMemoIds = it },
            )
        }
    }
}

@Composable
private fun MemoLinkSection(count: Int, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .clickable(onClick = onTap)
            .padding(DSSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("关联 Memo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
            Text(
                if (count == 0) "选择练习记录作为目标依据" else "已选 $count 条",
                fontSize = 12.sp,
                color = DSColors.shadowGray,
            )
        }
        Icon(Icons.Filled.Add, null, tint = DSColors.fairway)
    }
}

// =================================================================
// 状态分类辅助
// =================================================================

private enum class CadenceCategory(val displayName: String) {
    DAYS_STREAK("连续天数"),
    PER_WEEK("按次数"),
    WITHIN_MONTHS("按时间段"),
    CONTINUOUS("永久练习"),
}

private enum class CountSubKind(val displayName: String, val unit: String) {
    TIMES("次数", "次"),
    BALLS("球数", "球"),
}

private fun cadenceCategoryOf(c: V2Goal.Cadence?): CadenceCategory? = when (c) {
    is V2Goal.Cadence.DaysStreak -> CadenceCategory.DAYS_STREAK
    is V2Goal.Cadence.PerWeek -> CadenceCategory.PER_WEEK
    is V2Goal.Cadence.WithinMonths -> CadenceCategory.WITHIN_MONTHS
    V2Goal.Cadence.Continuous -> CadenceCategory.CONTINUOUS
    null -> null
}

private fun countSubKindOf(metric: V2Goal.Metric?): CountSubKind =
    if (metric?.kind == V2Goal.Metric.Kind.COUNT && metric.unit == CountSubKind.BALLS.unit) {
        CountSubKind.BALLS
    } else {
        CountSubKind.TIMES
    }

private fun defaultTargetFor(k: V2Goal.Metric.Kind, countSubKind: CountSubKind): Double = when (k) {
    V2Goal.Metric.Kind.COUNT -> if (countSubKind == CountSubKind.BALLS) 30.0 else 30.0
    V2Goal.Metric.Kind.DURATION -> 30.0
    V2Goal.Metric.Kind.DISTANCE -> 100.0
    V2Goal.Metric.Kind.FREE -> 0.0
}

private fun unitFor(k: V2Goal.Metric.Kind, countSubKind: CountSubKind = CountSubKind.TIMES): String? = when (k) {
    V2Goal.Metric.Kind.COUNT -> countSubKind.unit
    V2Goal.Metric.Kind.DURATION -> "分钟"
    V2Goal.Metric.Kind.DISTANCE -> "米"
    V2Goal.Metric.Kind.FREE -> null
}

private fun handleSave(
    existing: V2Goal?,
    name: String,
    clubKind: V2Goal.ClubKind,
    metricKind: V2Goal.Metric.Kind,
    countSubKind: CountSubKind,
    targetAmount: Double,
    cadenceCategory: CadenceCategory,
    perCountValue: Int,
    rangeMonths: Int,
    priority: V2Goal.Priority,
    targetSessions: Int,
    linkedMemoIds: List<UUID>,
) {
    val cadence: V2Goal.Cadence = when (cadenceCategory) {
        CadenceCategory.DAYS_STREAK -> V2Goal.Cadence.DaysStreak(perCountValue)
        CadenceCategory.PER_WEEK -> V2Goal.Cadence.PerWeek(perCountValue)
        CadenceCategory.WITHIN_MONTHS -> V2Goal.Cadence.WithinMonths(rangeMonths)
        CadenceCategory.CONTINUOUS -> V2Goal.Cadence.Continuous
    }

    val actualMetricKind = if (cadenceCategory == CadenceCategory.CONTINUOUS) {
        V2Goal.Metric.Kind.FREE
    } else {
        metricKind
    }
    val metric = V2Goal.Metric(
        actualMetricKind,
        if (actualMetricKind == V2Goal.Metric.Kind.FREE) 0.0 else targetAmount,
        unitFor(actualMetricKind, countSubKind),
    )

    if (existing == null) {
        val goal = V2Goal(
            title = name.trim(),
            iconType = clubKind.iconType,
            topic = clubKind.topic,
            metric = metric,
            clubKind = clubKind,
            priority = priority,
            cadence = cadence,
            targetSessions = if (actualMetricKind == V2Goal.Metric.Kind.FREE) null else targetSessions,
            sessionUnit = "次",
            derivedFromMemoIds = linkedMemoIds,
        )
        V2GoalStore.create(goal)
    } else {
        val lockedMetric = existing.metric.copy(target = targetAmount)
        val updated = existing.copy(
            title = name.trim(),
            iconType = clubKind.iconType,
            topic = clubKind.topic,
            metric = lockedMetric,
            clubKind = clubKind,
            priority = priority,
            targetSessions = if (existing.metric.kind == V2Goal.Metric.Kind.FREE) null else targetSessions,
            derivedFromMemoIds = linkedMemoIds,
        )
        V2GoalStore.update(updated)
    }
}

// =================================================================
// NavBar / Banner / Save / Delete
// =================================================================

@Composable
private fun NewGoalNavBar(
    title: String,
    canSave: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .background(DSColors.cardWhite.copy(alpha = 0.92f))
            .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DSColors.cardWhite)
                .border(width = 1.dp, color = DSColors.borderLine, shape = CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭",
                tint = DSColors.inkGreen,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "保存",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (canSave) DSColors.fairway else DSColors.shadowGray,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(enabled = canSave) { onSave() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun AIBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFFE0F2EC), Color(0xFFF1F8E9))),
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DSColors.fairway.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lightbulb,
                contentDescription = null,
                tint = DSColors.fairway,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "AI 教练推荐", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
            Text(
                text = "根据你最近 7 条 Memo 推荐：练习 7 号铁 50 球 / 5 次",
                fontSize = 11.sp,
                color = DSColors.shadowGray,
            )
        }
    }
}

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(DSColors.cardWhite.copy(alpha = 0.92f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.75f), shape = RoundedCornerShape(24.dp))
            .padding(vertical = 4.dp),
    ) {
        content()
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .background(Color.Black.copy(alpha = 0.06f)),
    )
}

@Composable
private fun SaveButton(
    canSubmit: Boolean,
    isEditMode: Boolean,
    onSave: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (isEditMode) "修改将立即生效" else "目标创建后将出现在首页今日聚焦中",
            fontSize = 12.sp,
            color = Color(0xFF9CA3AF),
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(elevation = if (canSubmit) 10.dp else 0.dp, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            if (canSubmit) DSColors.fairway else DSColors.fairway.copy(alpha = 0.35f),
                            if (canSubmit) DSColors.leafGreen else DSColors.leafGreen.copy(alpha = 0.35f),
                        ),
                    ),
                )
                .clickable(enabled = canSubmit, onClick = onSave),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 4.dp),
            )
            Text(
                text = if (isEditMode) "保存修改" else "创建目标",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun DeleteButton(onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .border(width = 1.dp, color = DSColors.flagRed.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
            .clickable { onDelete() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text("删除目标", color = DSColors.flagRed, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// =================================================================
// 表单字段
// =================================================================

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = DSColors.inkGreen,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun NameField(text: String, onTextChange: (String) -> Unit) {
    Column {
        SectionLabel("目标名称")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DSColors.warmWhite)
                .border(width = 1.dp, color = DSColors.borderLine, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (text.isEmpty()) {
                Text(
                    text = "如：用 7 号铁打 50 球",
                    fontSize = 15.sp,
                    color = Color(0xFFC5C5C5),
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= 30) onTextChange(it) },
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = DSColors.inkGreen,
                ),
                cursorBrush = SolidColor(DSColors.fairway),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun LockedMetricRow(goal: V2Goal) {
    val label = when (goal.metric.kind) {
        V2Goal.Metric.Kind.COUNT -> "次数"
        V2Goal.Metric.Kind.DURATION -> "时长"
        V2Goal.Metric.Kind.DISTANCE -> "精度"
        V2Goal.Metric.Kind.FREE -> "自由"
    }
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        SectionLabel("完成方式")
        Text(
            text = "$label（创建后不可修改）",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = DSColors.shadowGray,
        )
    }
}

@Composable
private fun LockedCadenceRow(cadence: V2Goal.Cadence) {
    val summary = when (cadence) {
        is V2Goal.Cadence.DaysStreak -> "连续 ${cadence.days} 天"
        is V2Goal.Cadence.PerWeek -> "每周 ${cadence.times} 次"
        is V2Goal.Cadence.WithinMonths -> "${cadence.months} 个月内"
        V2Goal.Cadence.Continuous -> "永久练习"
    }
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        SectionLabel("练习频率")
        Text(
            text = "$summary（创建后不可修改）",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = DSColors.shadowGray,
        )
    }
}

@Composable
private fun ClubChipsField(selection: V2Goal.ClubKind, onSelect: (V2Goal.ClubKind) -> Unit) {
    Column {
        SectionLabel("球杆 / 类型")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(V2Goal.ClubKind.entries.toList()) { k ->
                ChipPill(label = k.displayName, selected = k == selection, onClick = { onSelect(k) })
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MetricKindField(
    selection: V2Goal.Metric.Kind,
    countSubKind: CountSubKind,
    onSelect: (V2Goal.Metric.Kind) -> Unit,
    onCountSubKind: (CountSubKind) -> Unit,
) {
    Column {
        SectionLabel("完成方式")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChipPill(
                label = CountSubKind.TIMES.displayName,
                selected = selection == V2Goal.Metric.Kind.COUNT && countSubKind == CountSubKind.TIMES,
                onClick = { onCountSubKind(CountSubKind.TIMES) },
                modifier = Modifier.weight(1f),
            )
            ChipPill(
                label = CountSubKind.BALLS.displayName,
                selected = selection == V2Goal.Metric.Kind.COUNT && countSubKind == CountSubKind.BALLS,
                onClick = { onCountSubKind(CountSubKind.BALLS) },
                modifier = Modifier.weight(1f),
            )
            ChipPill(
                label = "时长",
                selected = selection == V2Goal.Metric.Kind.DURATION,
                onClick = { onSelect(V2Goal.Metric.Kind.DURATION) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun TargetStepperField(
    kind: V2Goal.Metric.Kind,
    countSubKind: CountSubKind,
    amount: Double,
    onAmountChange: (Double) -> Unit,
) {
    if (kind == V2Goal.Metric.Kind.FREE) {
        Column {
            SectionLabel("目标数量")
            Text(
                text = "自由练习：无单次量约束",
                fontSize = 13.sp,
                color = DSColors.shadowGray,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        return
    }

    val step = when (kind) {
        V2Goal.Metric.Kind.COUNT -> if (countSubKind == CountSubKind.BALLS) 5.0 else 1.0
        V2Goal.Metric.Kind.DURATION -> 5.0
        V2Goal.Metric.Kind.DISTANCE -> 0.1
        else -> 1.0
    }
    val min = if (kind == V2Goal.Metric.Kind.DISTANCE) 0.1 else 1.0
    val max = when (kind) {
        V2Goal.Metric.Kind.COUNT -> 500.0
        V2Goal.Metric.Kind.DURATION -> 240.0
        V2Goal.Metric.Kind.DISTANCE -> 5.0
        else -> 100.0
    }
    val unit = unitFor(kind, countSubKind) ?: ""

    Column {
        SectionLabel("目标数量")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepperButton(
                icon = Icons.Filled.Remove,
                onClick = { onAmountChange((amount - step).coerceAtLeast(min)) },
            )
            Text(
                text = "${formatTarget(amount)} $unit",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.inkGreen,
                modifier = Modifier.weight(1f),
            )
            StepperButton(
                icon = Icons.Filled.Add,
                onClick = { onAmountChange((amount + step).coerceAtMost(max)) },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun formatTarget(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(DSColors.fairway.copy(alpha = 0.08f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DSColors.fairway,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun CadenceField(
    category: CadenceCategory,
    perCount: Int,
    months: Int,
    onCategory: (CadenceCategory) -> Unit,
    onPerCount: (Int) -> Unit,
    onMonths: (Int) -> Unit,
) {
    Column {
        SectionLabel("练习频率")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                CadenceCategory.PER_WEEK,
                CadenceCategory.WITHIN_MONTHS,
                CadenceCategory.CONTINUOUS,
            ).forEach { c ->
                ChipPill(
                    label = c.displayName,
                    selected = c == category,
                    onClick = { onCategory(c) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        when (category) {
            CadenceCategory.DAYS_STREAK, CadenceCategory.PER_WEEK -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StepperButton(
                        icon = Icons.Filled.Remove,
                        onClick = { onPerCount((perCount - 1).coerceAtLeast(1)) },
                    )
                    Text(
                        text = "$perCount ${if (category == CadenceCategory.DAYS_STREAK) "天" else "次/周"}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DSColors.inkGreen,
                        modifier = Modifier.weight(1f),
                    )
                    StepperButton(
                        icon = Icons.Filled.Add,
                        onClick = { onPerCount((perCount + 1).coerceAtMost(30)) },
                    )
                }
            }
            CadenceCategory.WITHIN_MONTHS -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StepperButton(
                        icon = Icons.Filled.Remove,
                        onClick = { onMonths((months - 1).coerceAtLeast(1)) },
                    )
                    Text(
                        text = "$months 个月",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DSColors.inkGreen,
                        modifier = Modifier.weight(1f),
                    )
                    StepperButton(
                        icon = Icons.Filled.Add,
                        onClick = { onMonths((months + 1).coerceAtMost(24)) },
                    )
                }
            }
            CadenceCategory.CONTINUOUS -> {
                Text(
                    text = "永久目标：长期持续打卡，无完成时间",
                    fontSize = 12.sp,
                    color = DSColors.shadowGray,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun TargetSessionsField(value: Int, onChange: (Int) -> Unit) {
    Column {
        SectionLabel("目标总次数")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepperButton(
                icon = Icons.Filled.Remove,
                onClick = { onChange((value - 1).coerceAtLeast(1)) },
            )
            Text(
                text = "$value 次",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.inkGreen,
                modifier = Modifier.weight(1f),
            )
            StepperButton(
                icon = Icons.Filled.Add,
                onClick = { onChange((value + 1).coerceAtMost(99)) },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PriorityField(selection: V2Goal.Priority, onSelect: (V2Goal.Priority) -> Unit) {
    Column {
        SectionLabel("优先级")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            V2Goal.Priority.entries.forEach { p ->
                ChipPill(
                    label = "${p.emoji} ${p.shortName}",
                    selected = p == selection,
                    onClick = { onSelect(p) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// =================================================================
// 通用 chip
// =================================================================

@Composable
private fun ChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) DSColors.fairway else DSColors.cardWhite
    val fg = if (selected) Color.White else DSColors.inkGreen
    val border = if (selected) DSColors.fairway else DSColors.borderLine
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(BorderStroke(width = 1.dp, color = border), shape = RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}
