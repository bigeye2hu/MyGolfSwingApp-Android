package com.mygolfswing.app.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.ui.designsystem.DSColors
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
    val sorted = memos.sortedByDescending { it.createdAt }
    var localSelected by remember(selectedIds) { mutableStateOf(selectedIds) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("选择关联 Memo", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            TextButton(onClick = { onConfirm(localSelected); onDismiss() }) {
                Text("完成", color = DSColors.fairway, fontWeight = FontWeight.SemiBold)
            }
        }
        if (sorted.isEmpty()) {
            Text(
                "还没有 Memo，先去记录练习吧",
                modifier = Modifier.padding(24.dp),
                color = DSColors.shadowGray,
                fontSize = 14.sp,
            )
        } else {
            LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
                items(sorted, key = { it.id }) { memo ->
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
}

@Composable
private fun MemoPickerRow(memo: V2Memo, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        androidx.compose.material3.Icon(
            imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (selected) DSColors.fairway else DSColors.shadowGray,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                memo.displayTitle ?: memo.title ?: "无标题",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.inkGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            memo.text?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 12.sp, color = DSColors.shadowGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
