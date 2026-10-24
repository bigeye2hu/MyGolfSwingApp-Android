package com.mygolfswing.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.memo.V2MemoThumbnail
import java.time.format.DateTimeFormatter
import java.util.UUID

/** 目标关联 Memo 多选（对齐 iOS `V2NewGoalMemoPickerSheet`） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2GoalMemoPickerSheet(
    selectedIds: Set<UUID>,
    onDismiss: () -> Unit,
    onConfirm: (Set<UUID>) -> Unit,
) {
    val memos by V2MemoStore.allMemos.collectAsState()
    val allowedMemos = remember(memos) {
        memos
            .filter { it.kind != V2MemoKind.TEXT }
            .sortedByDescending { it.createdAt }
    }
    val allowedIds = remember(allowedMemos) { allowedMemos.map { it.id }.toSet() }
    var query by remember { mutableStateOf("") }
    var localSelected by remember(selectedIds, allowedIds) { mutableStateOf(selectedIds.intersect(allowedIds)) }
    val filtered = remember(allowedMemos, query) {
        allowedMemos.filter { it.matchesGoalMemoPickerQuery(query) }
    }
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DSColors.warmWhite,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // SwiftUI `.sheet` without a custom detent opens at the large
                // presentation height. Keep the picker equally spacious so
                // search results and the fixed confirmation bar do not feel
                // like a compact Android-only bottom sheet.
                .fillMaxHeight(0.92f)
                .testTag("goal-memo-picker"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 88.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 12.dp),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Text("取消", color = DSColors.shadowGray, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "选择 Memo",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                GoalMemoPickerSearchBar(
                    value = query,
                    onValueChange = { query = it },
                    focusManager = focusManager,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 12.dp),
                )

                if (filtered.isEmpty()) {
                    GoalMemoPickerEmptyState(
                        hasAnyVideoMemo = allowedMemos.isNotEmpty(),
                        query = query,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filtered, key = { it.id }) { memo ->
                            MemoPickerRow(
                                memo = memo,
                                selected = memo.id in localSelected,
                                onToggle = {
                                    val next = localSelected.toMutableSet()
                                    if (memo.id in next) next.remove(memo.id) else next.add(memo.id)
                                    localSelected = next
                                },
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.94f)),
            ) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.06f)))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .v2NavigationBarPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("已选 ${localSelected.size} 条", fontSize = 13.sp, color = DSColors.shadowGray)
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(50),
                                ambientColor = DSColors.fairway.copy(alpha = 0.32f),
                                spotColor = DSColors.fairway.copy(alpha = 0.32f),
                            )
                            .clip(RoundedCornerShape(50))
                            .background(Brush.linearGradient(listOf(DSColors.fairway, Color(0xFF40916B))))
                            .clickable {
                                onConfirm(localSelected.intersect(allowedIds))
                                onDismiss()
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .testTag("goal-memo-picker-confirm"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "完成",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalMemoPickerSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.85f))
            .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp)
            .testTag("goal-memo-picker-search"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(13.dp),
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    "搜索 Memo（标题 / 内容 / 话题）",
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Color(0xFF111827), fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth().testTag("goal-memo-picker-search-input"),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "清除搜索",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") }
                    .padding(2.dp),
            )
        }
    }
}

@Composable
private fun MemoPickerRow(memo: V2Memo, selected: Boolean, onToggle: () -> Unit) {
    val rowShape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(if (selected) DSColors.fairway.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.70f))
            .border(
                1.dp,
                if (selected) DSColors.fairway.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.06f),
                rowShape,
            )
            .clickable(onClick = onToggle)
            .padding(10.dp)
            .testTag("goal-memo-picker-row-${memo.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GoalMemoPickerThumbnail(memo)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                memo.goalMemoDisplayTitle(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF383F52),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    memo.topic.displayName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = DSColors.fairway,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DSColors.fairway.copy(alpha = 0.10f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                Text(
                    memo.createdAt.format(goalMemoPickerDateTimeFormatter),
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        MemoPickerSelectionMark(selected = selected)
    }
}

@Composable
private fun GoalMemoPickerThumbnail(memo: V2Memo) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .width(110.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.60f))
            .border(1.dp, Color.White.copy(alpha = 0.55f), shape)
            .testTag("goal-memo-picker-thumbnail-${memo.id}"),
    ) {
        V2MemoThumbnail(
            memo = memo,
            modifier = Modifier
                .width(110.dp)
                .height(68.dp),
            showKindPlaceholderIcon = true,
        )
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                memo.goalMemoDisplayTitle(),
                color = Color(0xFF383F52),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                memo.createdAt.format(goalMemoPickerShortDateFormatter),
                color = Color(0xFF9CA3AF),
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}

private val goalMemoPickerDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
private val goalMemoPickerShortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd")

@Composable
private fun MemoPickerSelectionMark(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (selected) DSColors.fairway else Color.Transparent)
            .border(1.5.dp, if (selected) Color.Transparent else Color.Black.copy(alpha = 0.20f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

internal fun V2Memo.matchesGoalMemoPickerQuery(query: String): Boolean {
    val normalized = query.trim().lowercase()
    if (normalized.isEmpty()) return true
    val title = displayTitle.orEmpty().lowercase()
    val body = text.orEmpty().lowercase()
    val topicRawValue = topic.name.lowercase().replace("_", "")
    return title.contains(normalized) || body.contains(normalized) || topicRawValue.contains(normalized)
}

@Composable
private fun GoalMemoPickerEmptyState(
    hasAnyVideoMemo: Boolean,
    query: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = DSColors.shadowGray.copy(alpha = 0.45f), modifier = Modifier.size(36.dp))
            Text(
                text = when {
                    query.isNotBlank() -> "没有匹配的 Memo"
                    !hasAnyVideoMemo -> "暂无 Memo 可选"
                    else -> "暂无 Memo 可选"
                },
                fontSize = 14.sp,
                color = DSColors.shadowGray,
            )
        }
    }
}
