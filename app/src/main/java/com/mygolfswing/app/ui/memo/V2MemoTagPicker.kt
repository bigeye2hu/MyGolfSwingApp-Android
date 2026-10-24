package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2CategoryGroup
import com.mygolfswing.app.data.store.V2CategoryStore
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.designsystem.DSColors

/**
 * Memo 表单中的标签入口。点击后打开底部 Sheet，对齐 iOS `MEMO-013 标签选择 / 新建标签` 路径。
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun V2MemoTagPickerField(
    selectedTagIds: Set<String>,
    onSelectedTagIdsChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    detailInlineStyle: Boolean = false,
) {
    val groups by V2CategoryStore.groups.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    val selectedNames = remember(groups, selectedTagIds) {
        groups.flatMap { it.items }
            .filter { it.id in selectedTagIds }
            .map { it.name }
    }

    if (detailInlineStyle) {
        V2MemoDetailInlineTags(
            groups = groups,
            selectedTagIds = selectedTagIds,
            onSelectedTagIdsChange = onSelectedTagIdsChange,
            modifier = modifier,
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DSColors.cardWhite)
                .border(1.dp, DSColors.borderLine, RoundedCornerShape(14.dp))
                .clickable { showSheet = true }
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("标签", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
                Spacer(Modifier.weight(1f))
                Text(
                    if (selectedNames.isEmpty()) "选择标签" else "管理 ${selectedNames.size}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DSColors.fairway,
                )
            }
            if (selectedNames.isEmpty()) {
                Text("未选择标签，点这里打开标签选择器", fontSize = 12.sp, color = DSColors.shadowGray)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    selectedNames.take(6).forEach { name ->
                        TagSummaryChip(name)
                    }
                    if (selectedNames.size > 6) {
                        TagSummaryChip("+${selectedNames.size - 6}")
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .v2NavigationBarPadding()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("标签选择", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
                        Text("选择已有标签，或新建自定义标签", fontSize = 12.sp, color = DSColors.shadowGray)
                    }
                    TextButton(onClick = { showSheet = false }) {
                        Text("完成", color = DSColors.fairway)
                    }
                }
                V2MemoTagPicker(
                    selectedTagIds = selectedTagIds,
                    onSelectedTagIdsChange = onSelectedTagIdsChange,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun V2MemoDetailInlineTags(
    groups: List<V2CategoryGroup>,
    selectedTagIds: Set<String>,
    onSelectedTagIdsChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nonEmpty = groups.filter { it.items.isNotEmpty() }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("标签", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DSColors.deepGreen)
            Spacer(Modifier.weight(1f))
            Text(
                "选择下方标签",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.fairway,
            )
        }
        if (nonEmpty.isEmpty()) {
            Text("暂无标签，请先在分类管理里创建", fontSize = 12.sp, color = DSColors.shadowGray)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                nonEmpty.forEach { group ->
                    V2MemoDetailTagGroup(
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun V2MemoDetailTagGroup(
    group: V2CategoryGroup,
    selectedTagIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = group.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
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
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color.White else Color(0xFF2D4A38),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) DSColors.fairway else Color(0xFFE8EDE9))
                        .semantics { this.selected = selected }
                        .clickable { onToggle(item.id) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TagSummaryChip(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = DSColors.fairway,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(ColorTagSelectedBg)
            .border(1.dp, DSColors.fairway.copy(alpha = 0.32f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

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
    var showCreate by rememberSaveable { mutableStateOf(false) }

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
        V2MemoTagCreateSheet(
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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                memoCategoryGroupIcon(group.icon),
                contentDescription = null,
                tint = DSColors.shadowGray,
                modifier = Modifier.size(11.dp),
            )
            Text(
                text = group.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.shadowGray,
            )
        }
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
                        .semantics { this.selected = selected }
                        .clickable { onToggle(item.id) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V2MemoTagCreateSheet(
    groups: List<V2CategoryGroup>,
    onDismiss: () -> Unit,
    onCreate: (name: String, groupId: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedGroupId by rememberSaveable { mutableStateOf(groups.firstOrNull()?.id.orEmpty()) }
    val canCreate = name.isNotBlank() && selectedGroupId.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DSColors.warmWhite,
        dragHandle = null,
    ) {
        V2RoomPage(Modifier.fillMaxWidth()) {
            Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .v2NavigationBarPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DSColors.shadowGray.copy(alpha = 0.45f))
                    .align(Alignment.CenterHorizontally),
            )

            Text(
                text = "新建标签",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.inkGreen,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("标签名", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DSColors.shadowGray)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DSColors.cardWhite)
                        .border(1.dp, DSColors.borderLine, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = DSColors.inkGreen),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (name.isEmpty()) {
                                Text("如：球场实战、室内练习…", fontSize = 15.sp, color = DSColors.shadowGray)
                            }
                            inner()
                        },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("所属分类", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DSColors.shadowGray)
                if (groups.isEmpty()) {
                    Text("当前没有分类组，请先在 V1 分类管理里创建", fontSize = 12.sp, color = DSColors.flagRed)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        groups.forEach { group ->
                            CategoryCreateGroupChip(
                                group = group,
                                selected = group.id == selectedGroupId,
                                onClick = { selectedGroupId = group.id },
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "取消",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DSColors.inkGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(DSColors.sandBeige)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp),
                )
                Text(
                    text = "创建",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(if (canCreate) DSColors.fairway else DSColors.fairway.copy(alpha = 0.4f))
                        .clickable(enabled = canCreate) {
                            onCreate(name.trim(), selectedGroupId)
                        }
                        .padding(vertical = 14.dp),
                )
            }
        }
        }
    }
}

@Composable
private fun CategoryCreateGroupChip(
    group: V2CategoryGroup,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) DSColors.fairway else DSColors.cardWhite)
            .border(1.dp, if (selected) DSColors.fairway else DSColors.borderLine, CircleShape)
            .semantics { this.selected = selected }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            memoCategoryGroupIcon(group.icon),
            contentDescription = null,
            tint = if (selected) Color.White else DSColors.shadowGray,
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = group.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else DSColors.inkGreen,
        )
    }
}

private val ColorTagSelectedBg = androidx.compose.ui.graphics.Color(0xFFE8F5E9)
private val ColorTagNormalBg = androidx.compose.ui.graphics.Color(0xFFF5F5F5)

private fun memoCategoryGroupIcon(iconName: String?): ImageVector =
    when (iconName) {
        "figure.golf" -> Icons.Filled.SportsGolf
        "figure.stand" -> Icons.Filled.AccessibilityNew
        "flag.fill" -> Icons.Filled.Flag
        "timer" -> Icons.Filled.Timer
        "list.bullet" -> Icons.AutoMirrored.Filled.FormatListBulleted
        "person.fill" -> Icons.Filled.Person
        else -> Icons.Filled.Folder
    }
