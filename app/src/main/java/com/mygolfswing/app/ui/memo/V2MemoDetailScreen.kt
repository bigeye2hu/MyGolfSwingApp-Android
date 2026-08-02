package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoSource
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.store.V2CategoryStore
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val TextMemoTitleMaxLength = 24
private const val TextMemoBodyMaxLength = 240

/**
 * 文字 Memo 详情/编辑（对齐 iOS `V2TextMemoDetailView`）。
 */
@Composable
fun V2MemoDetailScreen(
    memoId: UUID,
    onBack: () -> Unit = {},
) {
    val memos by V2MemoStore.allMemos.collectAsState()
    val goals by V2GoalStore.goals.collectAsState()
    val memo = memos.firstOrNull { it.id == memoId }

    if (memo == null) {
        V2MissingEntityScreen(message = "Memo 不存在", onBack = onBack, title = "随手记")
        return
    }

    var title by remember(memo.id) { mutableStateOf(memo.title ?: memo.displayTitle ?: "") }
    var body by remember(memo.id) { mutableStateOf(memo.text ?: "") }
    var selectedTagIds by remember(memo.id) {
        mutableStateOf(
            if (memo.tagIds.isNotEmpty()) {
                memo.tagIds.toSet()
            } else {
                memo.tags.mapNotNull { name ->
                    V2CategoryStore.getAllItems().firstOrNull { it.name == name }?.id
                }.toSet()
            },
        )
    }
    var selectedGoalIds by remember(memo.id) { mutableStateOf(memo.linkedGoalIds.toSet()) }
    var priorityRaw by remember(memo.id) { mutableStateOf(normalizedTextMemoPriority(memo.priorityRaw)) }
    var dirty by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var showAttachmentPlan by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val canSave = title.trim().isNotEmpty() && body.trim().isNotEmpty() && !saving
    fun attemptBack() {
        if (saving || deleting) return
        if (dirty) showDiscardConfirm = true else onBack()
    }

    fun saveChanges() {
        if (!canSave) return
        val tagNames = V2CategoryStore.tagNamesFor(selectedTagIds)
        val trimmedTitle = title.trim()
        val trimmedBody = body.trim()
        val sourceHint = if (memo.isAiInsightMemo()) "ai" else null
        val updated = memo.copy(
            title = trimmedTitle,
            displayTitle = trimmedTitle,
            text = trimmedBody,
            tags = tagNames,
            tagIds = selectedTagIds.toList(),
            linkedGoalIds = selectedGoalIds.toList(),
            topic = V2MemoBridge.inferTopic(null, tagNames, trimmedTitle),
            accent = V2MemoBridge.inferAccent(sourceHint, priorityRaw, tagNames),
            priorityRaw = priorityRaw,
        )
        scope.launch {
            saving = true
            saveError = null
            try {
                V2MemoStore.updateAndWait(updated)
                V2GoalStore.replaceMemoLinks(memo.id, selectedGoalIds)
                dirty = false
                onBack()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                saveError = memoTextThrowableError("保存失败，请稍后重试", e)
            } finally {
                saving = false
            }
        }
    }

    fun deleteMemo() {
        if (deleting) return
        scope.launch {
            deleting = true
            deleteError = null
            try {
                V2MemoStore.deleteAndWait(memo.id)
                onBack()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                showDeleteConfirm = false
                deleteError = memoTextThrowableError("删除失败，请稍后重试", e)
            } finally {
                deleting = false
            }
        }
    }

    V2BackHandler { attemptBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailNavBar(
                onBack = ::attemptBack,
                onDelete = { showDeleteConfirm = true },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .v2NavigationBarPadding()
                    .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
                verticalArrangement = Arrangement.spacedBy(DSSpacing.md),
            ) {
                TextMemoPriorityHeader(
                    priorityRaw = priorityRaw,
                    parentCategoryName = parentCategoryNameFor(selectedTagIds),
                    onCycle = {
                        priorityRaw = nextTextMemoPriority(priorityRaw)
                        dirty = true
                    },
                )
                BasicTextField(
                    value = title,
                    onValueChange = {
                        title = it.take(TextMemoTitleMaxLength)
                        dirty = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 1.dp),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1A1A1A),
                    ),
                    cursorBrush = SolidColor(DSColors.fairway),
                    decorationBox = { inner ->
                        if (title.isEmpty()) {
                            Text(
                                "标题",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFC5C5C5),
                            )
                        }
                        inner()
                    },
                )
                TextMemoSubtitle(memo)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = Color.Black.copy(alpha = 0.07f),
                            spotColor = Color.Black.copy(alpha = 0.07f),
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .background(DSColors.cardWhite)
                        .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
                        .padding(20.dp),
                ) {
                    BasicTextField(
                        value = body,
                        onValueChange = {
                            body = it.take(TextMemoBodyMaxLength)
                            dirty = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp),
                        textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF1A1A1A)),
                        cursorBrush = SolidColor(DSColors.fairway),
                        decorationBox = { inner ->
                            if (body.isEmpty()) {
                                Text(
                                    "开始写今天的感悟…",
                                    fontSize = 14.sp,
                                    color = Color(0xFFC5C5C5),
                                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                                )
                            }
                            inner()
                        },
                    )
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        Spacer(Modifier.weight(1f))
                        Text("${body.length}字", fontSize = 11.sp, color = DSColors.shadowGray)
                    }
                }
                TextMemoAttachmentsSection(onAddClick = { showAttachmentPlan = true })
                V2MemoTagPickerField(
                    selectedTagIds = selectedTagIds,
                    onSelectedTagIdsChange = { selectedTagIds = it; dirty = true },
                    detailInlineStyle = true,
                )
                LinkedGoalsEditor(
                    goals = goals.filter { it.status == V2Goal.Status.ACTIVE },
                    selectedGoalIds = selectedGoalIds,
                    onToggleGoal = { goalId ->
                        selectedGoalIds = if (goalId in selectedGoalIds) {
                            selectedGoalIds - goalId
                        } else {
                            selectedGoalIds + goalId
                        }
                        dirty = true
                    },
                )
                TextMemoMetaInfo(memo)
                TextMemoDecorativeFooter()
            }

            if (dirty) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .v2NavigationBarPadding()
                        .background(DSColors.warmWhite.copy(alpha = 0.95f))
                        .drawBehind {
                            drawLine(
                                color = Color.Black.copy(alpha = 0.06f),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                        .padding(DSSpacing.lg)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = DSColors.fairway.copy(alpha = 0.4f),
                            spotColor = DSColors.fairway.copy(alpha = 0.4f),
                        )
                        .clip(RoundedCornerShape(50))
                        .background(if (canSave || saving) DSColors.fairway else DSColors.fairway.copy(alpha = 0.4f))
                        .clickable(enabled = canSave, onClick = ::saveChanges)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Icon(
                                Icons.Filled.CheckCircle,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        Text(
                            if (saving) "保存中…" else "保存",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
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

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("放弃未保存的修改？") },
            text = { Text("已修改的内容将丢失。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        onBack()
                    },
                ) {
                    Text("放弃", color = DSColors.flagRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("继续编辑")
                }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!deleting) showDeleteConfirm = false },
            title = { Text("删除这条 Memo？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = ::deleteMemo,
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

    if (showAttachmentPlan) {
        AlertDialog(
            onDismissRequest = { showAttachmentPlan = false },
            title = { Text("附件替代方案") },
            text = { Text("当前文字 Memo 会优先保留正文、标签、优先级和目标关联；需要带视频复盘时，可以创建视频 Memo，或在目标详情里关联已有 Memo。") },
            confirmButton = {
                TextButton(onClick = { showAttachmentPlan = false }) {
                    Text("知道了", color = DSColors.fairway)
                }
            },
        )
    }
}

@Composable
private fun DetailNavBar(
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .background(DSColors.cardWhite.copy(alpha = 0.92f))
            .padding(horizontal = DSSpacing.lg)
            .padding(top = 12.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "关闭",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1A1A),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(50))
                .clickable(onClick = onBack)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Spacer(Modifier.weight(1f))
        Text("心得详情", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
        Spacer(Modifier.weight(1f))
        DeleteBtn(onClick = onDelete)
    }
}

@Composable
private fun TextMemoPriorityHeader(
    priorityRaw: String,
    parentCategoryName: String?,
    onCycle: () -> Unit,
) {
    val color = textMemoPriorityColor(priorityRaw)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.12f))
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(50))
                .clickable(onClick = onCycle)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("●", fontSize = 11.sp, color = color)
            Text(
                textMemoPriorityLabel(priorityRaw),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
        if (!parentCategoryName.isNullOrBlank()) {
            Text(
                parentCategoryName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.fairway,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFE8F2EC))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun TextMemoAttachmentsSection(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("附件", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DSColors.deepGreen)
            Spacer(Modifier.weight(1f))
            Text(
                "+ 添加",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.fairway.copy(alpha = 0.45f),
                modifier = Modifier
                    .clickable(onClick = onAddClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DSColors.mistGreen.copy(alpha = 0.4f))
                .clickable(onClick = onAddClick)
                .drawBehind {
                    val stroke = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    )
                    drawRoundRect(
                        color = Color(0xFFB5CFC1),
                        style = stroke,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = DSColors.shadowGray.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp),
                )
                Text("点击添加视频或图片", fontSize = 12.sp, color = DSColors.shadowGray)
            }
        }
    }
}

@Composable
private fun TextMemoSubtitle(memo: V2Memo) {
    val fmt = remember { DateTimeFormatter.ofPattern("yyyy年M月d日") }
    Text(
        "${fmt.format(memo.createdAt)} · ${textMemoSourceLabel(memo)}",
        fontSize = 12.sp,
        color = DSColors.shadowGray,
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun TextMemoMetaInfo(memo: V2Memo) {
    val fmt = remember { DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm") }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("创建于 ${fmt.format(memo.createdAt)}", fontSize = 11.sp, color = DSColors.shadowGray)
        Text("更新于 ${fmt.format(memo.updatedAt)}", fontSize = 11.sp, color = DSColors.shadowGray)
    }
}

@Composable
private fun TextMemoDecorativeFooter() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
                .height(1.dp)
                .background(DSColors.trophyGold.copy(alpha = 0.2f)),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(1.dp)
                    .background(DSColors.trophyGold.copy(alpha = 0.4f)),
            )
            Text("每一次用心的记录，都是进步的印记。", fontSize = 12.sp, color = DSColors.deepGreen.copy(alpha = 0.6f))
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(1.dp)
                    .background(DSColors.trophyGold.copy(alpha = 0.4f)),
            )
        }
        Text("— 挥呗Memo —", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = DSColors.shadowGray.copy(alpha = 0.6f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinkedGoalsEditor(
    goals: List<V2Goal>,
    selectedGoalIds: Set<UUID>,
    onToggleGoal: (UUID) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("关联到目标", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DSColors.deepGreen)
        if (goals.isEmpty()) {
            Text("还没有进行中的目标", fontSize = 12.sp, color = DSColors.shadowGray)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                goals.forEach { goal ->
                    GoalChip(
                        goal = goal,
                        selected = goal.id in selectedGoalIds,
                        onClick = { onToggleGoal(goal.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalChip(goal: V2Goal, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(50),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f),
            )
            .clip(RoundedCornerShape(50))
            .background(DSColors.cardWhite)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) DSColors.fairway else Color.Black.copy(alpha = 0.08f),
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(goalPriorityColor(goal.priority)),
        )
        Text(
            goal.title,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color(0xFF1A1A1A) else DSColors.shadowGray,
        )
    }
}

private fun goalPriorityColor(priority: V2Goal.Priority): Color = when (priority) {
    V2Goal.Priority.CRITICAL -> Color(0xFFEF4444)
    V2Goal.Priority.HIGH -> Color(0xFFF97316)
    V2Goal.Priority.NORMAL -> DSColors.fairway
    V2Goal.Priority.LOW -> Color(0xFF9CA3AF)
}

private fun normalizedTextMemoPriority(raw: String): String = when (raw.lowercase()) {
    "low" -> "low"
    "high" -> "high"
    else -> "medium"
}

private fun nextTextMemoPriority(raw: String): String = when (normalizedTextMemoPriority(raw)) {
    "low" -> "medium"
    "medium" -> "high"
    else -> "low"
}

private fun textMemoPriorityLabel(raw: String): String = when (normalizedTextMemoPriority(raw)) {
    "low" -> "低优先级"
    "high" -> "高优先级"
    else -> "中优先级"
}

private fun textMemoPriorityColor(raw: String): Color = when (normalizedTextMemoPriority(raw)) {
    "low" -> Color(0xFF10B981)
    "high" -> Color(0xFFDC2626)
    else -> Color(0xFFC9920A)
}

private fun textMemoSourceLabel(memo: V2Memo): String = when {
    memo.isAiInsightMemo() -> "AI 生成"
    memo.source == V2MemoSource.SHARE -> "好友分享"
    else -> "训练日记"
}

private fun parentCategoryNameFor(tagIds: Set<String>): String? =
    tagIds.firstNotNullOfOrNull { tagId ->
        V2CategoryStore.groupForItemId(tagId)?.name
    }

@Composable
private fun DeleteBtn(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(DSColors.flagRed.copy(alpha = 0.08f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.DeleteOutline, null, tint = DSColors.flagRed, modifier = Modifier.size(14.dp))
    }
}
