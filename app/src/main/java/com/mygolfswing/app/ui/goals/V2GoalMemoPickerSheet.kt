package com.mygolfswing.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.ui.common.V2Format
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.memo.V2MemoThumbnail
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("选择 Memo", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827), modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DSColors.shadowGray, fontWeight = FontWeight.SemiBold)
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = DSColors.shadowGray) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "清除搜索",
                            tint = DSColors.shadowGray,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { query = "" }
                                .padding(4.dp),
                        )
                    }
                },
                placeholder = { Text("搜索 Memo（标题 / 内容 / 话题）") },
                shape = RoundedCornerShape(14.dp),
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .v2NavigationBarPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("已选 ${localSelected.size} 条", fontSize = 13.sp, color = DSColors.shadowGray)
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        onConfirm(localSelected.intersect(allowedIds))
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        if (localSelected.isEmpty()) "完成" else "完成 (${localSelected.size})",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoPickerRow(memo: V2Memo, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) DSColors.fairway.copy(alpha = 0.06f) else DSColors.cardWhite.copy(alpha = 0.8f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        V2MemoThumbnail(
            memo = memo,
            modifier = Modifier
                .size(width = 72.dp, height = 54.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                memo.goalMemoDisplayTitle(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.inkGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${memo.topic.displayName} · ${V2Format.timelineLabel(memo.createdAt)}",
                fontSize = 11.sp,
                color = DSColors.shadowGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            memo.goalMemoSourceLabel()?.let { source ->
                Text(
                    source,
                    fontSize = 11.sp,
                    color = DSColors.fairway,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DSColors.fairway.copy(alpha = 0.08f))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
            memo.text?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 12.sp, color = DSColors.shadowGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        MemoPickerSelectionMark(selected = selected)
    }
}

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

private fun V2Memo.matchesGoalMemoPickerQuery(query: String): Boolean {
    val tokens = query
        .trim()
        .lowercase()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (tokens.isEmpty()) return true
    val haystack = listOfNotNull(
        displayTitle,
        title,
        text,
        topic.displayName,
        metaText,
        trainingMethod,
        platform,
    )
        .plus(tags)
        .joinToString(" ")
        .lowercase()
    return tokens.all { haystack.contains(it) }
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
