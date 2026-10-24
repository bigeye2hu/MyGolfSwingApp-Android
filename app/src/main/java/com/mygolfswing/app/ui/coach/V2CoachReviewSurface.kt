package com.mygolfswing.app.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.coach.V2CoachReviewFocus
import com.mygolfswing.app.data.coach.V2CoachWorkspaceItem
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.designsystem.DSColors

@Composable
internal fun V2CoachReviewFixedChrome(
    selectedFilter: String,
    selectedFocus: V2CoachReviewFocus,
    pendingCount: Int,
    respondedCount: Int,
    localDraftCount: Int,
    onFilterChanged: (String) -> Unit,
    onFocusChanged: (V2CoachReviewFocus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(196.dp)) {
            CoachCourseHeaderImage(
                CoachCourseHeader.COMMENT,
                Modifier.fillMaxSize(),
            )
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ReviewHeader(
                    selectedFilter = selectedFilter,
                    selectedFocus = selectedFocus,
                    onFilterChanged = onFilterChanged,
                    onFocusChanged = onFocusChanged,
                )
                ReviewSummary(
                    selectedFilter = selectedFilter,
                    pendingCount = pendingCount,
                    respondedCount = respondedCount,
                    localDraftCount = localDraftCount,
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            V2CoachReviewFilterBar(
                selectedFilter = selectedFilter,
                pendingCount = pendingCount,
                respondedCount = respondedCount,
                onSelected = onFilterChanged,
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    reviewQueueOrderDescription(selectedFilter, selectedFocus),
                    color = DSColors.shadowGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).testTag("coach-review-order"),
                )
                if (selectedFilter == "pending" && selectedFocus != V2CoachReviewFocus.ALL) {
                    Text(
                        "清除筛选",
                        color = DSColors.fairway,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(CircleShape).clickable { onFocusChanged(V2CoachReviewFocus.ALL) }
                            .padding(horizontal = 8.dp, vertical = 4.dp).testTag("coach-review-clear-focus"),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewHeader(
    selectedFilter: String,
    selectedFocus: V2CoachReviewFocus,
    onFilterChanged: (String) -> Unit,
    onFocusChanged: (V2CoachReviewFocus) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp).testTag("coach-review-fixed-header"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("点评", color = DSColors.inkGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Box {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(DSColors.cardWhite)
                    .border(1.dp, DSColors.borderLine, CircleShape)
                    .clickable(role = Role.Button) { expanded = true }.testTag("coach-review-filter-menu"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Tune, "筛选点评内容", tint = DSColors.inkGreen, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = DSColors.cardWhite,
            ) {
                if (selectedFilter == "pending") {
                    V2CoachReviewFocus.entries.forEach { focus ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    focus.title,
                                    color = if (focus == selectedFocus) DSColors.fairway else DSColors.inkGreen,
                                    fontWeight = if (focus == selectedFocus) FontWeight.Bold else FontWeight.Medium,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    reviewFocusIcon(focus),
                                    null,
                                    tint = if (focus == selectedFocus) DSColors.fairway else DSColors.shadowGray,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = {
                                onFocusChanged(focus)
                                expanded = false
                            },
                        )
                    }
                } else {
                    DropdownMenuItem(
                        text = { Text("查看待回复", color = DSColors.inkGreen, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Filled.ChatBubble, null, tint = DSColors.fairway) },
                        onClick = {
                            onFilterChanged("pending")
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun reviewFocusIcon(focus: V2CoachReviewFocus): ImageVector = when (focus) {
    V2CoachReviewFocus.ALL -> Icons.Filled.Tune
    V2CoachReviewFocus.NEW -> Icons.Filled.AddCircle
    V2CoachReviewFocus.VIEWED -> Icons.Filled.Visibility
    V2CoachReviewFocus.DRAFT -> Icons.Filled.Edit
}

@Composable
private fun ReviewSummary(
    selectedFilter: String,
    pendingCount: Int,
    respondedCount: Int,
    localDraftCount: Int,
) {
    val responded = selectedFilter == "responded"
    val count = if (responded) respondedCount else pendingCount
    Row(
        Modifier.fillMaxWidth().height(108.dp)
            .shadow(16.dp, RoundedCornerShape(18.dp), ambientColor = DSColors.deepGreen.copy(alpha = 0.10f))
            .clip(RoundedCornerShape(18.dp)).background(DSColors.cardWhite.copy(alpha = 0.78f))
            .padding(horizontal = 18.dp).testTag("coach-review-summary"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(count.toString(), color = DSColors.inkGreen, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (responded) "条已回复" else "条待回复",
                    color = DSColors.inkGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Text(
                reviewSummaryTitle(selectedFilter, count, localDraftCount),
                color = DSColors.shadowGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            if (!responded && localDraftCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Edit, null, tint = DSColors.trophyGold, modifier = Modifier.size(11.dp))
                    Text(
                        "其中 $localDraftCount 条有本机草稿",
                        color = DSColors.trophyGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(DSColors.mistGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (responded) Icons.Filled.CheckCircle else Icons.Filled.ChatBubble,
                null,
                tint = if (responded) DSColors.deepGreen else DSColors.trophyGold,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
internal fun V2CoachReviewFilterBar(
    selectedFilter: String,
    pendingCount: Int,
    respondedCount: Int,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.72f)).padding(4.dp)
            .testTag("coach-review-primary-filters"),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf("pending" to ("待回复" to pendingCount), "responded" to ("已回复" to respondedCount)).forEach { entry ->
            val value = entry.first
            val title = entry.second.first
            val count = entry.second.second
            val selected = selectedFilter == value
            Row(
                Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (selected) DSColors.cardWhite else Color.Transparent)
                    .clickable(role = Role.Button) { onSelected(value) }
                    .testTag("coach-review-filter-$value"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    title,
                    color = if (selected) DSColors.inkGreen else DSColors.shadowGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(4.dp))
                Box(
                    Modifier.height(19.dp).clip(CircleShape)
                        .background(if (selected) DSColors.deepGreen else DSColors.fairway.copy(alpha = 0.12f))
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        count.toString(),
                        color = if (selected) Color.White else DSColors.fairway,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
internal fun V2CoachReviewSyncWarning(
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp))
            .background(DSColors.trophyGold.copy(alpha = 0.16f)).padding(horizontal = 12.dp)
            .testTag("coach-review-sync-warning"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(Icons.Filled.CloudSync, null, tint = DSColors.trophyGold, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("列表暂时没有更新", color = DSColors.inkGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("已有内容仍可继续处理，重新同步不会影响草稿", color = DSColors.shadowGray, fontSize = 9.sp, maxLines = 1)
        }
        Text(
            "重试",
            color = DSColors.fairway,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onRetry).padding(5.dp),
        )
        Box(
            Modifier.size(24.dp).clip(CircleShape).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, "关闭同步提示", tint = DSColors.shadowGray, modifier = Modifier.size(11.dp))
        }
    }
}

@Composable
internal fun V2CoachReviewEmptyState(
    selectedFilter: String,
    pendingCount: Int,
    hasActiveStudents: Boolean,
    onSync: () -> Unit,
    onAddStudent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val waiting = selectedFilter == "pending" && pendingCount > 0
    val responded = selectedFilter == "responded"
    val title = when {
        waiting -> "还有 $pendingCount 条内容正在同步"
        responded -> "还没有已回复内容"
        hasActiveStudents -> "暂时没有待回复内容"
        else -> "先添加一位学员"
    }
    val detail = when {
        waiting -> "数字已经更新，内容列表稍后就会出现"
        responded -> "回复学员后，会自动保存在这里"
        hasActiveStudents -> "新的练习或学员回复会自动出现"
        else -> "先连接一位学员，新的练习会自动出现在这里"
    }
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 42.dp).testTag("coach-review-empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(66.dp).clip(CircleShape).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
            Icon(
                when {
                    waiting -> Icons.Filled.Refresh
                    responded -> Icons.Filled.CheckCircle
                    hasActiveStudents -> Icons.Filled.ChatBubble
                    else -> Icons.Filled.AddCircle
                },
                null,
                tint = if (waiting || responded) DSColors.fairway else DSColors.shadowGray,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(title, color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(detail, color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        if (waiting || (!responded && !hasActiveStudents)) {
            Row(
                Modifier.height(38.dp).clip(CircleShape).background(DSColors.fairway)
                    .clickable(role = Role.Button, onClick = if (waiting) onSync else onAddStudent)
                    .padding(horizontal = 16.dp).testTag(if (waiting) "coach-review-sync" else "coach-review-add-student"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    if (waiting) Icons.Filled.Refresh else Icons.Filled.AddCircle,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
                Text(if (waiting) "立即同步" else "添加学员", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
internal fun V2CoachReviewLoadErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(20.dp))
            .padding(24.dp).testTag("coach-review-load-error"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.CloudSync, null, tint = DSColors.fairway, modifier = Modifier.size(27.dp))
        }
        Text("学员内容暂时没有同步下来", color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            "已经写过的回复和草稿不会丢失，重新同步后会回到这里。",
            color = DSColors.shadowGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Row(
            Modifier.height(40.dp).clip(CircleShape).background(DSColors.fairway)
                .clickable(role = Role.Button, onClick = onRetry).padding(horizontal = 18.dp)
                .testTag("coach-review-reload"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Filled.Refresh, null, tint = Color.White, modifier = Modifier.size(15.dp))
            Text("重新加载", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun V2CoachReviewMissingConnectionCard(
    item: V2CoachWorkspaceItem,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(19.dp)).padding(13.dp)
            .testTag("coach-review-missing-connection-${item.itemId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        V2UserAvatar(
            avatar = item.sender?.avatar,
            fallbackText = item.sender?.nickname,
            fallbackColor = DSColors.sandBeige,
            modifier = Modifier.size(46.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (item.senderRole == "student") item.sender?.nickname ?: "学员内容" else "待回复的学员内容",
                color = DSColors.inkGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text("正在同步教学连接，内容不会丢失", color = DSColors.shadowGray, fontSize = 11.sp)
        }
        Text(
            "重试",
            color = DSColors.fairway,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onRetry).padding(6.dp),
        )
    }
}

internal fun reviewSummaryTitle(selectedFilter: String, count: Int, localDraftCount: Int): String = when {
    selectedFilter == "responded" && count == 0 -> "还没有回复过学员"
    selectedFilter == "responded" -> "最近回复的交流"
    localDraftCount > 0 -> "先继续未发送的回复"
    count > 0 -> "从最早的学员内容开始"
    else -> "目前没有遗漏的回复"
}

internal fun reviewQueueOrderDescription(selectedFilter: String, focus: V2CoachReviewFocus): String = when {
    selectedFilter == "responded" -> "按回复时间排序 · 最新优先"
    focus != V2CoachReviewFocus.ALL -> "${focus.title} · 最早优先"
    else -> "按提交时间排序 · 最早优先"
}

internal fun shouldRequestNextCoachReviewPage(
    visibleIndex: Int,
    itemCount: Int,
    nextCursor: String?,
): Boolean = itemCount > 0 && visibleIndex == itemCount - 1 && !nextCursor.isNullOrBlank()

internal fun shouldRefreshCoachReviewAfterReconnect(previous: Boolean?, current: Boolean): Boolean =
    previous == false && current
