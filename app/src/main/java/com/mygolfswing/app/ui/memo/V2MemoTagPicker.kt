package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2CategoryGroup
import com.mygolfswing.app.data.store.V2CategoryStore
import com.mygolfswing.app.ui.designsystem.DSColors

/**
 * Memo 表单标签选择（对齐 iOS `V2MemoTagPicker`）。
 *
 * - 按一级分类组分段展示（如「挥杆阶段」「身体部位」）
 * - 已选值为 `CategoryItem.id`
 */
@Composable
fun V2MemoTagPicker(
    selectedTagIds: Set<String>,
    onSelectedTagIdsChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups by V2CategoryStore.groups.collectAsState()
    var showCreate by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "标签",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.inkGreen,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "+ 新建",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DSColors.fairway,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { showCreate = true }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }

        val nonEmpty = groups.filter { it.items.isNotEmpty() }
        if (nonEmpty.isEmpty()) {
            Text(
                text = "暂无标签，点 \"+ 新建\" 添加一个",
                fontSize = 12.sp,
                color = DSColors.shadowGray,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            nonEmpty.forEach { group ->
                CategoryGroupSection(
                    group = group,
                    selectedTagIds = selectedTagIds,
                    onToggle = { id ->
                        onSelectedTagIdsChange(
                            if (id in selectedTagIds) selectedTagIds - id else selectedTagIds + id,
                        )
                    },
                )
            }
        }
    }

    if (showCreate) {
        V2MemoTagCreateDialog(
            groups = groups,
            onDismiss = { showCreate = false },
            onCreate = { name, groupId ->
                V2CategoryStore.addCustomNode(name, groupId)
                showCreate = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryGroupSection(
    group: V2CategoryGroup,
    selectedTagIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = group.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = DSColors.shadowGray,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            group.items.forEach { item ->
                val selected = item.id in selectedTagIds
                Text(
                    text = item.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) DSColors.fairway else DSColors.inkGreen,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) ColorTagSelectedBg else ColorTagNormalBg)
                        .border(
                            1.dp,
                            if (selected) DSColors.fairway else DSColors.borderLine,
                            RoundedCornerShape(50),
                        )
                        .clickable { onToggle(item.id) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun V2MemoTagCreateDialog(
    groups: List<V2CategoryGroup>,
    onDismiss: () -> Unit,
    onCreate: (name: String, groupId: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf(groups.firstOrNull()?.id.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建标签") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("如：球场实战、室内练习…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("所属分类", fontSize = 12.sp, color = DSColors.shadowGray)
                if (groups.isEmpty()) {
                    Text("当前没有分类组", fontSize = 12.sp, color = DSColors.flagRed)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        groups.forEach { group ->
                            val selected = group.id == selectedGroupId
                            Text(
                                text = group.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) DSColors.warmWhite else DSColors.inkGreen,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) DSColors.fairway else DSColors.cardWhite)
                                    .border(
                                        1.dp,
                                        if (selected) DSColors.fairway else DSColors.borderLine,
                                        RoundedCornerShape(50),
                                    )
                                    .clickable { selectedGroupId = group.id }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && selectedGroupId.isNotBlank()) {
                        onCreate(name.trim(), selectedGroupId)
                    }
                },
                enabled = name.isNotBlank() && selectedGroupId.isNotBlank(),
            ) {
                Text("创建", color = DSColors.fairway)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private val ColorTagSelectedBg = androidx.compose.ui.graphics.Color(0xFFE8F5E9)
private val ColorTagNormalBg = androidx.compose.ui.graphics.Color(0xFFF5F5F5)
