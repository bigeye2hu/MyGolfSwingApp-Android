package com.mygolfswing.app.ui.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2Topic
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.memo.V2MemoThumbnail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
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
    val memos by V2MemoStore.allMemos.collectAsState()

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
    var rangeCustomSelected by rememberSaveable {
        mutableStateOf(
            ((existing?.cadence as? V2Goal.Cadence.WithinMonths)?.months ?: 1) !in listOf(1, 3),
        )
    }
    var priority by remember { mutableStateOf(existing?.priority ?: V2Goal.Priority.CRITICAL) }
    var linkedMemoIds by remember {
        mutableStateOf(existing?.derivedFromMemoIds?.toSet() ?: emptySet())
    }
    var showMemoPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAIRecommendInfo by remember { mutableStateOf(false) }
    var showAIBanner by rememberSaveable { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val canSubmit = name.trim().isNotEmpty() && !saving && !deleting
    val memoPool = remember(memos) { memos.sortedByDescending { it.createdAt } }
    val linkedMemos = remember(memoPool, linkedMemoIds) {
        memoPool.filter { it.id in linkedMemoIds }
    }
    fun saveGoal() {
        if (!canSubmit) return
        scope.launch {
            saving = true
            saveError = null
            try {
                val goal = buildGoalForSave(
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
                    linkedMemoIds = linkedMemos.map { it.id },
                )
                if (existing == null) {
                    V2GoalStore.createAndWait(goal)
                } else {
                    V2GoalStore.updateAndWait(goal)
                }
                onClose()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                saveError = goalSaveError("保存失败，请稍后重试", e.message)
            } finally {
                saving = false
            }
        }
    }
    fun deleteGoal(goal: V2Goal) {
        if (deleting) return
        scope.launch {
            deleting = true
            deleteError = null
            try {
                V2GoalStore.deleteAndWait(goal.id)
                showDeleteConfirm = false
                onDeleted()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                showDeleteConfirm = false
                deleteError = goalSaveError("删除失败，请稍后重试", e.message)
            } finally {
                deleting = false
            }
        }
    }

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
                saving = saving,
                onClose = { if (!saving && !deleting) onClose() },
                onSave = ::saveGoal,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = DSSpacing.md),
                verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
            ) {
                if (!isEditMode && showAIBanner) {
                    AIBanner(
                        onClick = { showAIRecommendInfo = true },
                        onDismiss = { showAIBanner = false },
                    )
                }

                FormCard {
                    NameField(text = name, onTextChange = { name = it })
                    DividerLine()
                    ClubChipsField(selection = clubKind, onSelect = { clubKind = it })
                    DividerLine()
                    if (existing != null) {
                        MetricKindField(
                            selection = metricKind,
                            countSubKind = countSubKind,
                            readOnly = true,
                            onSelect = {},
                            onCountSubKind = {},
                        )
                        DividerLine()
                        TargetStepperField(
                            kind = existing.metric.kind,
                            countSubKind = countSubKind,
                            amount = targetAmount,
                            onAmountChange = { targetAmount = it },
                        )
                        DividerLine()
                        CadenceField(
                            category = cadenceCategory,
                            perCount = perCountValue,
                            months = rangeMonths,
                            rangeCustomSelected = rangeCustomSelected,
                            lockCategory = true,
                            onCategory = { cadenceCategory = it },
                            onPerCount = { perCountValue = it },
                            onMonths = {
                                rangeMonths = it
                                rangeCustomSelected = false
                            },
                            onCustomRange = {
                                if (cadenceCategory == CadenceCategory.WITHIN_MONTHS) {
                                    if (rangeMonths == 1 || rangeMonths == 3) rangeMonths = 1
                                    rangeCustomSelected = true
                                }
                            },
                        )
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
                            rangeCustomSelected = rangeCustomSelected,
                            onCategory = { cadenceCategory = it },
                            onPerCount = { perCountValue = it },
                            onMonths = {
                                rangeMonths = it
                                rangeCustomSelected = false
                            },
                            onCustomRange = {
                                cadenceCategory = CadenceCategory.WITHIN_MONTHS
                                if (rangeMonths == 1 || rangeMonths == 3) rangeMonths = 1
                                rangeCustomSelected = true
                            },
                        )
                    }
                    DividerLine()
                    PriorityField(selection = priority, onSelect = { priority = it })
                    DividerLine()
                    MemoLinkSection(
                        linkedMemos = linkedMemos,
                        onTap = { showMemoPicker = true },
                        onRemove = { id -> linkedMemoIds = linkedMemoIds - id },
                    )
                }

                SaveButton(
                    canSubmit = canSubmit,
                    saving = saving,
                    isEditMode = isEditMode,
                    onSave = ::saveGoal,
                )

                if (existing != null) {
                    DeleteButton(
                        onDelete = { showDeleteConfirm = true },
                    )
                }
            }
        }

        if (existing != null && showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { if (!deleting) showDeleteConfirm = false },
                title = { Text("删除目标") },
                text = {
                    Text("确定要删除「${existing.title}」吗？删除后无法恢复，目标里的打卡进度和 Memo 关联也会一并清除。")
                },
                confirmButton = {
                    TextButton(
                        enabled = !deleting,
                        onClick = { deleteGoal(existing) },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (deleting) {
                                CircularProgressIndicator(
                                    color = DSColors.flagRed,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            Text(if (deleting) "删除中..." else "删除", color = DSColors.flagRed)
                        }
                    }
                },
                dismissButton = {
                    TextButton(enabled = !deleting, onClick = { showDeleteConfirm = false }) {
                        Text("取消")
                    }
                },
            )
        }

        saveError?.let { message ->
            AlertDialog(
                onDismissRequest = { saveError = null },
                title = { Text("保存失败") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { saveError = null }) {
                        Text("好", color = DSColors.fairway)
                    }
                },
            )
        }

        deleteError?.let { message ->
            AlertDialog(
                onDismissRequest = { deleteError = null },
                title = { Text("删除失败") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { deleteError = null }) {
                        Text("好", color = DSColors.fairway)
                    }
                },
            )
        }

        if (showAIRecommendInfo) {
            AlertDialog(
                onDismissRequest = { showAIRecommendInfo = false },
                title = { Text("AI 教练推荐") },
                text = {
                    Text(
                        "AI 推荐目标功能尚未接入。",
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showAIRecommendInfo = false }) {
                        Text("知道了", color = DSColors.fairway)
                    }
                },
            )
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
private fun MemoLinkSection(
    linkedMemos: List<V2Memo>,
    onTap: () -> Unit,
    onRemove: (UUID) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabelText(
                text = "关联 Memo 记录",
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.fairway.copy(alpha = 0.10f))
                    .border(1.dp, DSColors.fairway.copy(alpha = 0.22f), RoundedCornerShape(50))
                    .clickable(onClick = onTap)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Add, null, tint = DSColors.fairway, modifier = Modifier.size(11.dp))
                Text(
                    if (linkedMemos.isEmpty()) "选择已有 Memo" else "继续添加",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DSColors.fairway,
                )
            }
        }

        if (linkedMemos.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(linkedMemos, key = { it.id }) { memo ->
                    GoalMemoThumb(
                        memo = memo,
                        onRemove = { onRemove(memo.id) },
                    )
                }
            }
        }

        Text(
            if (linkedMemos.isEmpty()) {
                "暂无关联，可点击右上「+ 选择已有 Memo」绑定参考记录"
            } else {
                "关联的练习记录将作为该目标的参考数据"
            },
            fontSize = 12.sp,
            color = DSColors.shadowGray,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun GoalMemoThumb(memo: V2Memo, onRemove: () -> Unit) {
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
                text = memo.createdAt.format(MemoThumbDateFormatter),
                fontSize = 12.sp,
                color = DSColors.shadowGray,
                maxLines = 1,
            )
        }
    }
}

private val MemoThumbDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd")

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

private fun buildGoalForSave(
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
    linkedMemoIds: List<UUID>,
): V2Goal {
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
    val derivedTargetSessions = when (cadenceCategory) {
        CadenceCategory.DAYS_STREAK,
        CadenceCategory.PER_WEEK -> perCountValue
        CadenceCategory.WITHIN_MONTHS -> rangeMonths * 4
        CadenceCategory.CONTINUOUS -> null
    }

    return if (existing == null) {
        V2Goal(
            title = name.trim(),
            iconType = clubKind.iconType,
            topic = clubKind.topic,
            metric = metric,
            clubKind = clubKind,
            priority = priority,
            cadence = cadence,
            targetSessions = derivedTargetSessions,
            sessionUnit = "次",
            derivedFromMemoIds = linkedMemoIds,
        )
    } else {
        val lockedMetric = existing.metric.copy(target = targetAmount)
        existing.copy(
            title = name.trim(),
            iconType = clubKind.iconType,
            topic = clubKind.topic,
            metric = lockedMetric,
            clubKind = clubKind,
            priority = priority,
            targetSessions = derivedTargetSessions,
            derivedFromMemoIds = linkedMemoIds,
        )
    }
}

// =================================================================
// NavBar / Banner / Save / Delete
// =================================================================

@Composable
private fun NewGoalNavBar(
    title: String,
    canSave: Boolean,
    saving: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = DSSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(
                    elevation = 1.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.05f),
                )
                .clip(CircleShape)
                .background(DSColors.cardWhite.copy(alpha = 0.88f))
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.75f), shape = CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭",
                tint = Color(0xFF4A545F),
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "保存",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier
                .shadow(
                    elevation = if (canSave) 6.dp else 0.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = DSColors.fairway.copy(alpha = 0.32f),
                    spotColor = DSColors.fairway.copy(alpha = 0.32f),
                )
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.linearGradient(
                        listOf(
                            DSColors.fairway,
                            Color(0xFF40916B),
                        ),
                    ),
                )
                .alpha(if (canSave) 1f else 0.4f)
                .clickable(enabled = canSave) { onSave() }
                .padding(horizontal = DSSpacing.lg, vertical = 8.dp),
        )
    }
}

@Composable
private fun AIBanner(
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DSSpacing.Radius.md))
            .background(
                Brush.linearGradient(
                    listOf(
                        DSColors.fairway.copy(alpha = 0.10f),
                        Color(0xFF40916B).copy(alpha = 0.16f),
                    ),
                ),
            )
            .border(1.dp, DSColors.fairway.copy(alpha = 0.22f), RoundedCornerShape(DSSpacing.Radius.md))
            .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "✨", fontSize = 16.sp)
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AI 教练为你推荐了目标",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DSColors.fairway,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = " · 查看推荐 →",
                    fontSize = 12.sp,
                    color = DSColors.fairway,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(DSColors.fairway.copy(alpha = 0.12f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭 AI 推荐",
                tint = DSColors.fairway,
                modifier = Modifier.size(9.dp),
            )
        }
    }
}

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(DSColors.cardWhite.copy(alpha = 0.88f))
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
    saving: Boolean,
    isEditMode: Boolean,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isEditMode) "修改将立即生效，已完成的打卡记录不会变化" else "目标创建后将出现在首页今日聚焦中",
            fontSize = 12.sp,
            color = Color(0xFF9CA3AF),
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = if (canSubmit) 11.dp else 0.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = DSColors.fairway.copy(alpha = if (canSubmit) 0.40f else 0f),
                    spotColor = DSColors.fairway.copy(alpha = if (canSubmit) 0.40f else 0f),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            DSColors.fairway,
                            Color(0xFF40916B),
                        ),
                    ),
                )
                .alpha(if (canSubmit) 1f else 0.4f)
                .clickable(enabled = canSubmit, onClick = onSave),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            if (saving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(14.dp)
                        .alpha(0.8f),
                )
            }
            Text(
                text = if (saving) "保存中..." else if (isEditMode) "保存修改" else "创建目标",
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
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.flagRed.copy(alpha = 0.08f))
            .border(width = 1.dp, color = DSColors.flagRed.copy(alpha = 0.30f), shape = RoundedCornerShape(14.dp))
            .clickable { onDelete() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = Icons.Filled.DeleteOutline,
            contentDescription = null,
            tint = DSColors.flagRed,
            modifier = Modifier.size(13.dp),
        )
        Text("删除目标", color = DSColors.flagRed, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// =================================================================
// 表单字段
// =================================================================

@Composable
private fun SectionLabel(text: String) {
    SectionLabelText(
        text = text,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun SectionLabelText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF6B7280),
        letterSpacing = 0.77.sp,
        modifier = modifier,
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
                .background(Color.White.copy(alpha = 0.60f))
                .border(width = 1.dp, color = Color.Black.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp))
                .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
        ) {
            if (text.isEmpty()) {
                Text(
                    text = "描述你想练习的内容…",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9CA3AF),
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= 30) onTextChange(it) },
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1C2129),
                ),
                cursorBrush = SolidColor(DSColors.fairway),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClubChipsField(selection: V2Goal.ClubKind, onSelect: (V2Goal.ClubKind) -> Unit) {
    Column {
        SectionLabel("球杆 / 类型")
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            V2Goal.ClubKind.entries.forEach { k ->
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
    readOnly: Boolean = false,
) {
    Column {
        SectionLabel("完成方式")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (readOnly) 0.55f else 1f)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricOptionCard(
                label = CountSubKind.TIMES.displayName,
                selected = selection == V2Goal.Metric.Kind.COUNT && countSubKind == CountSubKind.TIMES,
                enabled = !readOnly,
                icon = Icons.Filled.CheckCircle,
                onClick = { onCountSubKind(CountSubKind.TIMES) },
                modifier = Modifier.weight(1f),
            )
            MetricOptionCard(
                label = CountSubKind.BALLS.displayName,
                selected = selection == V2Goal.Metric.Kind.COUNT && countSubKind == CountSubKind.BALLS,
                enabled = !readOnly,
                emoji = "⛳",
                onClick = { onCountSubKind(CountSubKind.BALLS) },
                modifier = Modifier.weight(1f),
            )
            MetricOptionCard(
                label = "时长",
                selected = selection == V2Goal.Metric.Kind.DURATION,
                enabled = !readOnly,
                emoji = "⏱",
                onClick = { onSelect(V2Goal.Metric.Kind.DURATION) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MetricOptionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    emoji: String? = null,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) {
                    Brush.linearGradient(
                        listOf(
                            DSColors.fairway.copy(alpha = 0.14f),
                            Color(0xFF40916B).copy(alpha = 0.20f),
                        ),
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.68f),
                            Color.White.copy(alpha = 0.68f),
                        ),
                    )
                },
            )
            .border(
                width = 1.dp,
                color = if (selected) DSColors.fairway else Color.Black.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (selected) DSColors.fairway.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            when {
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) DSColors.fairway else Color(0xFF6B7280),
                    modifier = Modifier.size(14.dp),
                )
                emoji != null -> Text(text = emoji, fontSize = 16.sp)
            }
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) DSColors.fairway else Color(0xFF6B7280),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

    val presets = targetPresetsFor(kind, countSubKind)
    val range = targetRangeFor(kind, countSubKind)
    val step = targetStepFor(kind, countSubKind)
    val unit = unitFor(kind, countSubKind) ?: ""
    var showPicker by remember { mutableStateOf(false) }

    Column {
        SectionLabel("目标数量")
        if (presets.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(presets) { preset ->
                    ChipPill(
                        label = preset.toString(),
                        selected = amount == preset.toDouble(),
                        onClick = { onAmountChange(preset.toDouble()) },
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TargetStepperButton(
                icon = Icons.Filled.Remove,
                onClick = { onAmountChange((amount - step).coerceAtLeast(range.start)) },
                onFastClick = { onAmountChange((amount - 5.0).coerceAtLeast(range.start)) },
            )
            TargetAmountDisplay(
                amount = amount,
                unit = unit,
                modifier = Modifier.weight(1f),
                onClick = { showPicker = true },
            )
            TargetStepperButton(
                icon = Icons.Filled.Add,
                onClick = { onAmountChange((amount + step).coerceAtMost(range.endInclusive)) },
                onFastClick = { onAmountChange((amount + 5.0).coerceAtMost(range.endInclusive)) },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showPicker) {
        GoalTargetValuePickerSheet(
            value = amount.toInt(),
            range = targetIntRangeFor(kind, countSubKind),
            mode = if (kind == V2Goal.Metric.Kind.DURATION) GoalTargetPickerMode.TIME_HM else GoalTargetPickerMode.SINGLE_INT,
            unit = if (kind == V2Goal.Metric.Kind.DURATION) "" else unit,
            title = if (kind == V2Goal.Metric.Kind.DURATION) "选择时长" else "选择数量",
            onDismiss = { showPicker = false },
            onConfirm = {
                onAmountChange(it.toDouble())
                showPicker = false
            },
        )
    }
}

private fun targetPresetsFor(kind: V2Goal.Metric.Kind, countSubKind: CountSubKind): List<Int> = when (kind) {
    V2Goal.Metric.Kind.COUNT -> if (countSubKind == CountSubKind.BALLS) {
        listOf(30, 50, 100, 200, 300)
    } else {
        listOf(10, 20, 30, 50)
    }
    V2Goal.Metric.Kind.DURATION -> listOf(15, 30, 45, 60, 90)
    V2Goal.Metric.Kind.DISTANCE -> listOf(50, 100, 200, 300, 500)
    V2Goal.Metric.Kind.FREE -> emptyList()
}

private fun targetRangeFor(kind: V2Goal.Metric.Kind, countSubKind: CountSubKind): ClosedFloatingPointRange<Double> = when (kind) {
    V2Goal.Metric.Kind.COUNT -> 1.0..if (countSubKind == CountSubKind.BALLS) 500.0 else 200.0
    V2Goal.Metric.Kind.DURATION -> 1.0..600.0
    V2Goal.Metric.Kind.DISTANCE -> 1.0..1000.0
    V2Goal.Metric.Kind.FREE -> 1.0..9999.0
}

private fun targetIntRangeFor(kind: V2Goal.Metric.Kind, countSubKind: CountSubKind): IntRange = when (kind) {
    V2Goal.Metric.Kind.COUNT -> 1..if (countSubKind == CountSubKind.BALLS) 500 else 200
    V2Goal.Metric.Kind.DURATION -> 1..600
    V2Goal.Metric.Kind.DISTANCE -> 1..1000
    V2Goal.Metric.Kind.FREE -> 1..9999
}

private fun targetStepFor(kind: V2Goal.Metric.Kind, countSubKind: CountSubKind): Double = when (kind) {
    V2Goal.Metric.Kind.COUNT -> 1.0
    V2Goal.Metric.Kind.DURATION -> 1.0
    V2Goal.Metric.Kind.DISTANCE -> 1.0
    V2Goal.Metric.Kind.FREE -> 1.0
}

private fun formatTarget(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)

@Composable
private fun TargetStepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    onFastClick: () -> Unit,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnFastClick by rememberUpdatedState(onFastClick)
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(
                elevation = 5.dp,
                shape = CircleShape,
                ambientColor = DSColors.fairway.copy(alpha = 0.32f),
                spotColor = DSColors.fairway.copy(alpha = 0.32f),
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        DSColors.fairway,
                        Color(0xFF40916B),
                    ),
                ),
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        currentOnClick()
                        coroutineScope {
                            val startedAt = System.currentTimeMillis()
                            val repeatJob = launch {
                                delay(500)
                                while (true) {
                                    if (System.currentTimeMillis() - startedAt > 2_000) {
                                        currentOnFastClick()
                                    } else {
                                        currentOnClick()
                                    }
                                    delay(100)
                                }
                            }
                            try {
                                tryAwaitRelease()
                            } finally {
                                repeatJob.cancel()
                            }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun TargetAmountDisplay(
    amount: Double,
    unit: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text(
                text = formatTarget(amount),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                modifier = Modifier.padding(horizontal = 2.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 1.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DSColors.fairway.copy(alpha = 0.25f)),
            )
        }
        if (unit.isNotBlank()) {
            Text(
                text = unit,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.shadowGray,
                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
            )
        }
    }
}

private enum class GoalTargetPickerMode {
    SINGLE_INT,
    TIME_HM,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalTargetValuePickerSheet(
    value: Int,
    range: IntRange,
    mode: GoalTargetPickerMode,
    unit: String,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val clampedValue = value.coerceIn(range.first, range.last)
    var draftInt by remember(value, range) { mutableIntStateOf(clampedValue) }
    var draftHours by remember(value, range) { mutableIntStateOf(clampedValue / 60) }
    var draftMinutes by remember(value, range) { mutableIntStateOf(clampedValue % 60) }
    var useKeyboard by remember { mutableStateOf(false) }
    var keyboardText by remember(value, range) { mutableStateOf(clampedValue.toString()) }

    fun currentDraftAsInt(): Int = when (mode) {
        GoalTargetPickerMode.SINGLE_INT -> draftInt
        GoalTargetPickerMode.TIME_HM -> draftHours * 60 + draftMinutes
    }.coerceIn(range.first, range.last)

    fun keyboardDraftAsInt(): Int = keyboardText
        .toIntOrNull()
        ?.coerceIn(range.first, range.last)
        ?: currentDraftAsInt()

    fun writeKeyboardBackToDraft() {
        val next = keyboardDraftAsInt()
        when (mode) {
            GoalTargetPickerMode.SINGLE_INT -> draftInt = next
            GoalTargetPickerMode.TIME_HM -> {
                draftHours = next / 60
                draftMinutes = next % 60
            }
        }
    }

    val selectedValue = if (useKeyboard) {
        keyboardDraftAsInt()
    } else {
        currentDraftAsInt()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DSColors.warmWhite)
                .padding(horizontal = 16.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DSColors.shadowGray, fontWeight = FontWeight.Medium)
                }
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DSColors.inkGreen,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                TextButton(
                    onClick = {
                        if (useKeyboard) {
                            writeKeyboardBackToDraft()
                            useKeyboard = false
                        } else {
                            keyboardText = currentDraftAsInt().toString()
                            useKeyboard = true
                        }
                    },
                    modifier = Modifier.width(72.dp),
                ) {
                    Text(
                        text = if (useKeyboard) "滚轮" else "键盘",
                        color = DSColors.fairway,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (useKeyboard) {
                GoalTargetKeyboardInput(
                    value = keyboardText,
                    range = range,
                    unit = if (mode == GoalTargetPickerMode.TIME_HM) "分钟" else unit,
                    onValueChange = {
                        keyboardText = it.filter(Char::isDigit).take(range.last.toString().length)
                    },
                )
            } else {
                if (mode == GoalTargetPickerMode.TIME_HM) {
                    TimeHmPicker(
                        hours = draftHours,
                        minutes = draftMinutes,
                        range = range,
                        onHours = { draftHours = it },
                        onMinutes = { draftMinutes = it },
                    )
                } else {
                    SingleIntPicker(
                        value = draftInt,
                        range = range,
                        unit = unit,
                        onValue = { draftInt = it },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("取消", color = DSColors.shadowGray, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        if (useKeyboard) writeKeyboardBackToDraft()
                        onConfirm(selectedValue)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("确定", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GoalTargetKeyboardInput(
    value: String,
    range: IntRange,
    unit: String,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    color = DSColors.inkGreen,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                ),
                cursorBrush = SolidColor(DSColors.fairway),
                modifier = Modifier
                    .width(140.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DSColors.cardWhite)
                    .border(1.5.dp, DSColors.fairway, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            if (unit.isNotBlank()) {
                Text(
                    text = unit,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4B5563),
                )
            }
        }
        Text(
            text = "范围 ${range.first}-${range.last}${unit}",
            fontSize = 12.sp,
            color = DSColors.shadowGray,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SingleIntPicker(
    value: Int,
    range: IntRange,
    unit: String,
    onValue: (Int) -> Unit,
) {
    val values = remember(range) { range.toList() }
    val initialIndex = remember(values, value) { values.indexOf(value).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (initialIndex - 2).coerceAtLeast(0))
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(values, key = { it }) { item ->
            PickerNumberRow(
                selected = item == value,
                label = if (unit.isBlank()) item.toString() else "$item $unit",
                onClick = { onValue(item) },
            )
        }
    }
}

@Composable
private fun TimeHmPicker(
    hours: Int,
    minutes: Int,
    range: IntRange,
    onHours: (Int) -> Unit,
    onMinutes: (Int) -> Unit,
) {
    val maxHours = (range.last / 60).coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PickerNumberColumn(
            modifier = Modifier.weight(1f),
            values = (0..maxHours).toList(),
            selected = hours.coerceIn(0, maxHours),
            suffix = "小时",
            onSelect = onHours,
        )
        PickerNumberColumn(
            modifier = Modifier.weight(1f),
            values = (0..59).toList(),
            selected = minutes.coerceIn(0, 59),
            suffix = "分钟",
            onSelect = onMinutes,
        )
    }
}

@Composable
private fun PickerNumberColumn(
    values: List<Int>,
    selected: Int,
    suffix: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialIndex = remember(values, selected) { values.indexOf(selected).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (initialIndex - 2).coerceAtLeast(0))
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(values, key = { it }) { value ->
            PickerNumberRow(
                selected = value == selected,
                label = "$value $suffix",
                onClick = { onSelect(value) },
            )
        }
    }
}

@Composable
private fun PickerNumberRow(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        fontSize = if (selected) 20.sp else 16.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) DSColors.inkGreen else DSColors.shadowGray,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) DSColors.fairway.copy(alpha = 0.10f) else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) DSColors.fairway.copy(alpha = 0.22f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    onFastClick: () -> Unit,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnFastClick by rememberUpdatedState(onFastClick)
    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = DSColors.fairway.copy(alpha = 0.32f),
                spotColor = DSColors.fairway.copy(alpha = 0.32f),
            )
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(DSColors.fairway, Color(0xFF40916B))))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        currentOnClick()
                        coroutineScope {
                            val startedAt = System.currentTimeMillis()
                            val repeatJob = launch {
                                delay(500)
                                while (true) {
                                    if (System.currentTimeMillis() - startedAt > 2_000) {
                                        currentOnFastClick()
                                    } else {
                                        currentOnClick()
                                    }
                                    delay(100)
                                }
                            }
                            try {
                                tryAwaitRelease()
                            } finally {
                                repeatJob.cancel()
                            }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
    }
}

@Composable
private fun CadenceField(
    category: CadenceCategory,
    perCount: Int,
    months: Int,
    rangeCustomSelected: Boolean,
    lockCategory: Boolean = false,
    onCategory: (CadenceCategory) -> Unit,
    onPerCount: (Int) -> Unit,
    onMonths: (Int) -> Unit,
    onCustomRange: () -> Unit,
) {
    var showPerCountPicker by remember { mutableStateOf(false) }
    val perCountSelected = category == CadenceCategory.PER_WEEK || category == CadenceCategory.DAYS_STREAK
    val timeRangeSelected = category == CadenceCategory.WITHIN_MONTHS
    val continuousSelected = category == CadenceCategory.CONTINUOUS
    fun canEdit(target: CadenceCategory): Boolean = !lockCategory || when (target) {
        CadenceCategory.PER_WEEK, CadenceCategory.DAYS_STREAK -> perCountSelected
        CadenceCategory.WITHIN_MONTHS -> timeRangeSelected
        CadenceCategory.CONTINUOUS -> continuousSelected
    }
    Column {
        SectionLabel("练习频率")
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CadenceOptionCard(
                selected = perCountSelected,
                enabled = canEdit(CadenceCategory.PER_WEEK),
                onClick = { if (!lockCategory) onCategory(CadenceCategory.PER_WEEK) },
            ) {
                CadenceHeader(
                    selected = perCountSelected,
                    title = "按次数",
                    trailing = if (perCountSelected) {
                        "共 $perCount 次"
                    } else {
                        null
                    },
                )
                if (perCountSelected) {
                    DividerLineCompact()
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StepperButton(
                            icon = Icons.Filled.Remove,
                            onClick = { onPerCount((perCount - 1).coerceAtLeast(1)) },
                            onFastClick = { onPerCount((perCount - 5).coerceAtLeast(1)) },
                        )
                        CadenceCountDisplay(
                            count = perCount,
                            modifier = Modifier.weight(1f),
                            onClick = { showPerCountPicker = true },
                        )
                        StepperButton(
                            icon = Icons.Filled.Add,
                            onClick = { onPerCount((perCount + 1).coerceAtMost(30)) },
                            onFastClick = { onPerCount((perCount + 5).coerceAtMost(30)) },
                        )
                    }
                }
            }

            CadenceOptionCard(
                selected = timeRangeSelected,
                enabled = canEdit(CadenceCategory.WITHIN_MONTHS),
                onClick = { if (!lockCategory) onCategory(CadenceCategory.WITHIN_MONTHS) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CadenceRadioDot(selected = timeRangeSelected)
                    Text(
                        text = "按时间段",
                        fontSize = 14.sp,
                        fontWeight = if (timeRangeSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (timeRangeSelected) DSColors.fairway else Color(0xFF4B5563),
                        modifier = Modifier.padding(start = 10.dp).weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MonthRangeChip(
                            label = "1个月",
                            selected = timeRangeSelected && months == 1 && !rangeCustomSelected,
                            enabled = canEdit(CadenceCategory.WITHIN_MONTHS),
                            onClick = {
                                if (!lockCategory) onCategory(CadenceCategory.WITHIN_MONTHS)
                                onMonths(1)
                            },
                        )
                        MonthRangeChip(
                            label = "3个月",
                            selected = timeRangeSelected && months == 3 && !rangeCustomSelected,
                            enabled = canEdit(CadenceCategory.WITHIN_MONTHS),
                            onClick = {
                                if (!lockCategory) onCategory(CadenceCategory.WITHIN_MONTHS)
                                onMonths(3)
                            },
                        )
                        MonthRangeChip(
                            label = "自定义",
                            selected = timeRangeSelected && rangeCustomSelected,
                            enabled = canEdit(CadenceCategory.WITHIN_MONTHS),
                            onClick = onCustomRange,
                        )
                    }
                }
            }

            CadenceOptionCard(
                selected = continuousSelected,
                enabled = canEdit(CadenceCategory.CONTINUOUS),
                onClick = { if (!lockCategory) onCategory(CadenceCategory.CONTINUOUS) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CadenceRadioDot(selected = continuousSelected)
                    Text(
                        text = "永久练习",
                        fontSize = 14.sp,
                        fontWeight = if (continuousSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (continuousSelected) DSColors.fairway else Color(0xFF4B5563),
                        modifier = Modifier.padding(start = 10.dp),
                    )
                    Text(
                        text = "无截止日期，持续追踪",
                        fontSize = 12.sp,
                        color = DSColors.shadowGray,
                        modifier = Modifier.padding(start = 10.dp).weight(1f),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    if (showPerCountPicker) {
        GoalTargetValuePickerSheet(
            value = perCount,
            range = 1..30,
            mode = GoalTargetPickerMode.SINGLE_INT,
            unit = "次",
            title = "选择次数",
            onDismiss = { showPerCountPicker = false },
            onConfirm = {
                onPerCount(it.coerceIn(1, 30))
                showPerCountPicker = false
            },
        )
    }
}

@Composable
private fun CadenceCountDisplay(
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                Text(
                    text = count.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 1.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DSColors.fairway.copy(alpha = 0.25f)),
                )
            }
            Text(
                text = "次",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.shadowGray,
                modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun CadenceOptionCard(
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) {
                    Brush.linearGradient(
                        listOf(
                            DSColors.fairway.copy(alpha = 0.12f),
                            Color(0xFF40916B).copy(alpha = 0.18f),
                        ),
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.65f),
                        ),
                    )
                },
            )
            .border(1.dp, if (selected) DSColors.fairway else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        content()
    }
}

@Composable
private fun CadenceHeader(selected: Boolean, title: String, trailing: String?) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CadenceRadioDot(selected = selected)
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) DSColors.fairway else Color(0xFF4B5563),
            modifier = Modifier.padding(start = 10.dp).weight(1f),
        )
        trailing?.let {
            Text(
                text = it,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = DSColors.fairway.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun CadenceRadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (selected) DSColors.fairway else Color.Transparent)
            .border(2.dp, if (selected) DSColors.fairway else Color.Black.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(9.dp),
            )
        }
    }
}

@Composable
private fun MonthRangeChip(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) Color.White else DSColors.shadowGray.copy(alpha = if (enabled) 1f else 0.65f),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) DSColors.fairway else Color.Black.copy(alpha = 0.05f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun DividerLineCompact() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DSColors.fairway.copy(alpha = 0.14f)),
    )
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
                PriorityCard(
                    priority = p,
                    selected = p == selection,
                    onClick = { onSelect(p) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = "同优先级目标按创建时间排序",
            fontSize = 12.sp,
            color = DSColors.shadowGray,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PriorityCard(
    priority: V2Goal.Priority,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = priorityColor(priority)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) color.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.65f))
            .border(if (selected) 2.dp else 1.dp, if (selected) color else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(priority.emoji, fontSize = 16.sp)
        Text(
            text = priority.shortName,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) color else Color(0xFF6B7280),
            letterSpacing = if (selected) 0.sp else 6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun priorityColor(priority: V2Goal.Priority): Color = when (priority) {
    V2Goal.Priority.CRITICAL -> Color(0xFFDC2626)
    V2Goal.Priority.HIGH -> Color(0xFFF97316)
    V2Goal.Priority.NORMAL -> Color(0xFFFBBF24)
    V2Goal.Priority.LOW -> Color(0xFF6B7280)
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
    val bg = if (selected) {
        Brush.linearGradient(listOf(DSColors.fairway, Color(0xFF40916B)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.70f), Color.White.copy(alpha = 0.70f)))
    }
    val fg = if (selected) Color.White else Color(0xFF545454)
    val border = if (selected) Color.Transparent else Color.Black.copy(alpha = 0.09f)
    Box(
        modifier = modifier
            .shadow(
                elevation = if (selected) 5.dp else 0.dp,
                shape = RoundedCornerShape(50),
                ambientColor = DSColors.fairway.copy(alpha = 0.32f),
                spotColor = DSColors.fairway.copy(alpha = 0.32f),
            )
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(BorderStroke(width = 1.dp, color = border), shape = RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
        )
    }
}

private fun goalSaveError(base: String, debugDetail: String?): String {
    val detail = debugDetail.takeIf { ServiceLocator.isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}
