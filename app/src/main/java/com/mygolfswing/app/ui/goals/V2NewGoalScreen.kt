package com.mygolfswing.app.ui.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Keyboard
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2Topic
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.common.V2RoomBackground
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
    persistGoal: suspend (goal: V2Goal, isEdit: Boolean) -> Unit = { goal, isEdit ->
        if (isEdit) V2GoalStore.updateAndWait(goal) else V2GoalStore.createAndWait(goal)
    },
    deleteGoalById: suspend (UUID) -> Unit = V2GoalStore::deleteAndWait,
) {
    val goals by V2GoalStore.goals.collectAsState()
    val existing = editingGoalId?.let { id -> goals.firstOrNull { it.id == id } }
    val isEditMode = editingGoalId != null
    val memos by V2MemoStore.allMemos.collectAsState()

    var name by rememberSaveable(editingGoalId) { mutableStateOf(existing?.title ?: "") }
    var clubKindName by rememberSaveable(editingGoalId) {
        mutableStateOf((existing?.clubKind ?: V2Goal.ClubKind.DRIVER).name)
    }
    var metricKindName by rememberSaveable(editingGoalId) {
        mutableStateOf((existing?.metric?.kind ?: V2Goal.Metric.Kind.COUNT).name)
    }
    var countSubKindName by rememberSaveable(editingGoalId) {
        mutableStateOf(countSubKindOf(existing?.metric).name)
    }
    var targetAmount by rememberSaveable(editingGoalId) {
        mutableDoubleStateOf(
            existing?.metric?.target
                ?.takeIf(Double::isFinite)
                ?.toInt()
                ?.coerceAtLeast(1)
                ?.toDouble()
                ?: defaultTargetFor(V2Goal.Metric.Kind.COUNT, CountSubKind.TIMES),
        )
    }
    var cadenceCategoryName by rememberSaveable(editingGoalId) {
        mutableStateOf((cadenceCategoryOf(existing?.cadence) ?: CadenceCategory.PER_WEEK).name)
    }
    var perCountValue by rememberSaveable(editingGoalId) {
        mutableIntStateOf(perCountValueOf(existing?.cadence))
    }
    var rangeMonths by rememberSaveable(editingGoalId) {
        mutableIntStateOf(rangeMonthsValueOf(existing?.cadence))
    }
    var rangeCustomSelected by rememberSaveable(editingGoalId) { mutableStateOf(false) }
    var priorityName by rememberSaveable(editingGoalId) {
        mutableStateOf((existing?.priority ?: V2Goal.Priority.CRITICAL).name)
    }
    var linkedMemoIdTexts by rememberSaveable(editingGoalId) {
        mutableStateOf(existing?.derivedFromMemoIds?.map(UUID::toString) ?: emptyList())
    }
    var showMemoPicker by rememberSaveable(editingGoalId) { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable(editingGoalId) { mutableStateOf(false) }
    var showAIRecommendInfo by rememberSaveable(editingGoalId) { mutableStateOf(false) }
    var showAIBanner by rememberSaveable(editingGoalId) { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var saveError by rememberSaveable(editingGoalId) { mutableStateOf<String?>(null) }
    var deleteError by rememberSaveable(editingGoalId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val clubKind = V2Goal.ClubKind.valueOf(clubKindName)
    val metricKind = V2Goal.Metric.Kind.valueOf(metricKindName)
    val countSubKind = CountSubKind.valueOf(countSubKindName)
    val cadenceCategory = CadenceCategory.valueOf(cadenceCategoryName)
    val priority = V2Goal.Priority.valueOf(priorityName)
    val linkedMemoIds = remember(linkedMemoIdTexts) {
        linkedMemoIdTexts.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }.toSet()
    }

    val canSubmit = canSubmitGoalForm(
        name = name,
        targetAmount = targetAmount,
        cadenceCategory = cadenceCategory,
    ) && (!isEditMode || existing != null) && !saving && !deleting
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
                persistGoal(goal, isEditMode)
                onClose()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                saveError = goalThrowableError("保存失败，请稍后重试", e)
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
                deleteGoalById(goal.id)
                showDeleteConfirm = false
                onDeleted()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                showDeleteConfirm = false
                deleteError = goalThrowableError("删除失败，请稍后重试", e)
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
                    0.0f to Color(0xFFE8F5E8),
                    0.3f to Color(0xFFF2F7F5),
                    0.65f to Color(0xFFF5F5F0),
                    1.0f to Color(0xFFEDE8E0),
                ),
            ),
    ) {
        V2RoomBackground(Modifier.fillMaxSize())
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
                    .testTag("goal-form-scroll")
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 60.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
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
                    ClubChipsField(selection = clubKind, onSelect = { clubKindName = it.name })
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
                            onCategory = { cadenceCategoryName = it.name },
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
                                metricKindName = it.name
                                if (it != V2Goal.Metric.Kind.COUNT) countSubKindName = CountSubKind.TIMES.name
                                targetAmount = defaultTargetFor(it, countSubKind)
                            },
                            onCountSubKind = {
                                countSubKindName = it.name
                                metricKindName = V2Goal.Metric.Kind.COUNT.name
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
                            onCategory = { cadenceCategoryName = it.name },
                            onPerCount = { perCountValue = it },
                            onMonths = {
                                rangeMonths = it
                                rangeCustomSelected = false
                            },
                            onCustomRange = {
                                cadenceCategoryName = CadenceCategory.WITHIN_MONTHS.name
                                if (rangeMonths == 1 || rangeMonths == 3) rangeMonths = 1
                                rangeCustomSelected = true
                            },
                        )
                    }
                    DividerLine()
                    PriorityField(selection = priority, onSelect = { priorityName = it.name })
                    DividerLine()
                    MemoLinkSection(
                        linkedMemos = linkedMemos,
                        onTap = { showMemoPicker = true },
                        onRemove = { id -> linkedMemoIdTexts = linkedMemoIdTexts - id.toString() },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SaveButton(
                        canSubmit = canSubmit,
                        saving = saving,
                        isEditMode = isEditMode,
                        onSave = ::saveGoal,
                    )

                    if (existing != null) {
                        DeleteButton(
                            onDelete = { showDeleteConfirm = true },
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        if (existing != null && showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { if (!deleting) showDeleteConfirm = false },
                title = { Text("删除目标") },
                text = {
                    Text("确定要删除「${existing.title}」吗？删除后无法恢复，关联的打卡记录也会一并清除。")
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
                        Text("好的", color = DSColors.fairway)
                    }
                },
            )
        }

        if (showMemoPicker) {
            V2GoalMemoPickerSheet(
                selectedIds = linkedMemoIds,
                onDismiss = { showMemoPicker = false },
                onConfirm = { linkedMemoIdTexts = it.map(UUID::toString) },
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

internal enum class CadenceCategory(val displayName: String) {
    PER_WEEK("按次数"),
    WITHIN_MONTHS("按时间段"),
    CONTINUOUS("永久练习"),
}

internal enum class CountSubKind(val displayName: String, val unit: String) {
    TIMES("次数", "次"),
    BALLS("球数", "球"),
}

/** Mirrors the validation in both iOS goal view models. */
internal fun canSubmitGoalForm(
    name: String,
    targetAmount: Double,
    cadenceCategory: CadenceCategory,
): Boolean {
    if (name.trim().isEmpty()) return false
    if (cadenceCategory != CadenceCategory.CONTINUOUS && (!targetAmount.isFinite() || targetAmount <= 0.0)) {
        return false
    }
    return true
}

private fun cadenceCategoryOf(c: V2Goal.Cadence?): CadenceCategory? = when (c) {
    // Current iOS folds the retired consecutive-days cadence into the editable
    // "按次数" category, then writes it back as PerWeek on save.
    is V2Goal.Cadence.DaysStreak -> CadenceCategory.PER_WEEK
    is V2Goal.Cadence.PerWeek -> CadenceCategory.PER_WEEK
    is V2Goal.Cadence.WithinMonths -> CadenceCategory.WITHIN_MONTHS
    V2Goal.Cadence.Continuous -> CadenceCategory.CONTINUOUS
    null -> null
}

private fun perCountValueOf(cadence: V2Goal.Cadence?): Int = when (cadence) {
    is V2Goal.Cadence.DaysStreak -> cadence.days.coerceIn(1, 30)
    is V2Goal.Cadence.PerWeek -> cadence.times.coerceIn(1, 30)
    else -> 10
}

/** Current iOS maps any legacy month value below 3 to 1 month, otherwise 3 months. */
private fun rangeMonthsValueOf(cadence: V2Goal.Cadence?): Int =
    (cadence as? V2Goal.Cadence.WithinMonths)?.months?.let { if (it >= 3) 3 else 1 } ?: 1

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

internal fun buildGoalForSave(
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
        existing.copy(
            title = name.trim(),
            iconType = clubKind.iconType,
            topic = clubKind.topic,
            // The completion-method control is locked in edit mode, but iOS
            // still rebuilds the metric from the submitted cadence. In
            // particular, a legacy count goal switched to continuous must be
            // normalized to a free metric with a zero target.
            metric = metric,
            clubKind = clubKind,
            priority = priority,
            cadence = cadence,
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
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827),
            letterSpacing = (-0.45).sp,
        )
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
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.75f), shape = RoundedCornerShape(24.dp)),
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
                .height(40.dp)
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
                .clickable(enabled = canSubmit, onClick = onSave)
                .testTag("goal-save-action"),
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
private fun DeleteButton(onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.flagRed.copy(alpha = 0.08f))
            .border(width = 1.dp, color = DSColors.flagRed.copy(alpha = 0.30f), shape = RoundedCornerShape(14.dp))
            .clickable { onDelete() }
            .padding(horizontal = 16.dp)
            .testTag("goal-delete-action"),
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
    val presets = targetPresetsFor(kind, countSubKind)
    val range = targetRangeFor(kind, countSubKind)
    val step = targetStepFor(kind, countSubKind)
    val unit = unitFor(kind, countSubKind) ?: ""
    var showPicker by rememberSaveable { mutableStateOf(false) }

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
            .testTag("goal-target-amount")
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

internal enum class GoalTargetPickerMode {
    SINGLE_INT,
    TIME_HM,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoalTargetValuePickerSheet(
    value: Int,
    range: IntRange,
    mode: GoalTargetPickerMode,
    unit: String,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val clampedValue = value.coerceIn(range.first, range.last)
    var draftInt by rememberSaveable(value, range.first, range.last) { mutableIntStateOf(clampedValue) }
    var draftHours by rememberSaveable(value, range.first, range.last) { mutableIntStateOf(clampedValue / 60) }
    var draftMinutes by rememberSaveable(value, range.first, range.last) { mutableIntStateOf(clampedValue % 60) }
    var useKeyboard by rememberSaveable { mutableStateOf(false) }
    var keyboardText by rememberSaveable(value, range.first, range.last) { mutableStateOf(clampedValue.toString()) }

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DSColors.warmWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Text("取消", color = DSColors.shadowGray, fontWeight = FontWeight.Medium)
                }
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF121726),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                androidx.compose.material3.IconButton(
                    onClick = {
                        if (useKeyboard) {
                            writeKeyboardBackToDraft()
                            useKeyboard = false
                        } else {
                            keyboardText = currentDraftAsInt().toString()
                            useKeyboard = true
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterEnd).size(44.dp),
                ) {
                    Icon(
                        imageVector = if (useKeyboard) Icons.Filled.GridView else Icons.Filled.Keyboard,
                        contentDescription = if (useKeyboard) "切换为滚轮" else "切换为键盘",
                        tint = DSColors.fairway,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
            ) {
                if (useKeyboard) {
                    GoalTargetKeyboardInput(
                        value = keyboardText,
                        range = range,
                        unit = unit,
                        focusRequester = focusRequester,
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
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text("取消", color = DSColors.shadowGray, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        if (useKeyboard) writeKeyboardBackToDraft()
                        onConfirm(selectedValue)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("goal-target-confirm")
                        .background(
                            Brush.linearGradient(listOf(DSColors.fairway, Color(0xFF40916B))),
                            RoundedCornerShape(12.dp),
                        ),
                ) {
                    Text("确定", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    LaunchedEffect(useKeyboard) {
        if (useKeyboard) {
            delay(100)
            focusRequester.requestFocus()
        } else {
            writeKeyboardBackToDraft()
        }
    }
}

@Composable
private fun GoalTargetKeyboardInput(
    value: String,
    range: IntRange,
    unit: String,
    focusRequester: FocusRequester,
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
                    .testTag("goal-target-keyboard-input")
                    .focusRequester(focusRequester)
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
            text = "范围 ${range.first}–${range.last}${unit}",
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GoalPickerWheel(
            values = values,
            selected = value,
            onSelect = onValue,
            testTag = "goal-single-wheel",
            modifier = Modifier.weight(1f),
        )
        if (unit.isNotBlank()) {
            Text(
                text = unit,
                color = Color(0xFF4B5563),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 4.dp),
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
            .height(180.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GoalPickerWheel(
            modifier = Modifier.weight(1f),
            values = (0..maxHours).toList(),
            selected = hours.coerceIn(0, maxHours),
            onSelect = onHours,
            testTag = "goal-hours-wheel",
        )
        Text("小时", color = Color(0xFF4B5563), fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(end = 8.dp))
        GoalPickerWheel(
            modifier = Modifier.weight(1f),
            values = (0..59).toList(),
            selected = minutes.coerceIn(0, 59),
            onSelect = onMinutes,
            testTag = "goal-minutes-wheel",
        )
        Text("分钟", color = Color(0xFF4B5563), fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GoalPickerWheel(
    values: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val initialIndex = remember(values, selected) { values.indexOf(selected).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(listState)
    val scope = rememberCoroutineScope()
    val centeredIndex by remember(listState, values) {
        derivedStateOf {
            val layout = listState.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs(item.offset + item.size / 2 - center)
            }?.index ?: initialIndex
        }
    }

    LaunchedEffect(centeredIndex, values) {
        values.getOrNull(centeredIndex)?.let(onSelect)
    }

    Box(modifier = modifier.height(180.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DSColors.mistGreen.copy(alpha = 0.7f))
                .border(1.dp, DSColors.fairway.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag(testTag),
            state = listState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(vertical = 68.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(values, key = { it }) { item ->
                val isSelected = item == selected
                Text(
                    text = item.toString(),
                    color = if (isSelected) Color(0xFF121726) else DSColors.shadowGray,
                    fontSize = if (isSelected) 21.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .alpha(if (isSelected) 1f else 0.52f)
                        .clickable {
                            scope.launch { listState.animateScrollToItem(values.indexOf(item)) }
                        }
                        .padding(top = if (isSelected) 8.dp else 11.dp),
                )
            }
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
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
            .semantics {
                this.contentDescription = contentDescription
                onClick {
                    currentOnClick()
                    true
                }
            }
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
    var showPerCountPicker by rememberSaveable { mutableStateOf(false) }
    val perCountSelected = category == CadenceCategory.PER_WEEK
    val timeRangeSelected = category == CadenceCategory.WITHIN_MONTHS
    val continuousSelected = category == CadenceCategory.CONTINUOUS
    fun canEdit(target: CadenceCategory): Boolean = !lockCategory || when (target) {
        CadenceCategory.PER_WEEK -> perCountSelected
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
                            contentDescription = "减少次数",
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
                            contentDescription = "增加次数",
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
            .testTag("goal-cadence-count")
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

internal fun goalSaveError(base: String, debugDetail: String?): String {
    return goalSaveError(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun goalSaveError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

internal fun goalThrowableError(base: String, error: Throwable): String {
    if (error is CancellationException) throw error
    return goalSaveError(base, error.message)
}
