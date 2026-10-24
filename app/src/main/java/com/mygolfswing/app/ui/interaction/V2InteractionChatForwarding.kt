package com.mygolfswing.app.ui.interaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.friend.V2IMConversation
import com.mygolfswing.app.data.friend.V2IMMessage
import com.mygolfswing.app.data.friend.V2IMMessageKind
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors

internal enum class V2ChatForwardMode { SEPARATE, MERGED }

internal const val V2_CHAT_SEPARATE_FORWARD_LIMIT = 30

internal fun chatForwardAbstract(message: V2IMMessage): String = when (message.kind) {
    V2IMMessageKind.TEXT -> message.text.ifBlank { "[消息]" }
    V2IMMessageKind.IMAGE -> "[图片]"
    V2IMMessageKind.VIDEO -> "[视频]"
    V2IMMessageKind.VOICE -> "[语音]"
    V2IMMessageKind.FILE -> "[文件]"
    else -> message.text.ifBlank { "[消息]" }
}

internal fun chatMergedForwardTitle(
    messages: List<V2IMMessage>,
    currentUserId: String?,
    ownName: String,
    peerName: String,
): String {
    val names = messages.asSequence().map { message ->
        if (message.senderUserId == currentUserId || message.isMine) ownName else peerName
    }.filter(String::isNotBlank).distinct().take(2).toList()
    return when (names.size) {
        2 -> "${names[0]}和${names[1]}的聊天记录"
        1 -> "${names[0]}的聊天记录"
        else -> "聊天记录"
    }
}

internal fun chatForwardAbstracts(
    messages: List<V2IMMessage>,
    currentUserId: String?,
    ownName: String,
    peerName: String,
): List<String> = messages.take(4).map { message ->
    val sender = if (message.senderUserId == currentUserId || message.isMine) ownName else peerName
    "$sender: ${chatForwardAbstract(message)}"
}

@Composable
internal fun V2ChatSelectionCircle(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(20.dp).clip(CircleShape)
            .then(
                if (selected) Modifier.background(DSColors.fairway)
                else Modifier.border(1.5.dp, DSColors.shadowGray, CircleShape),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
internal fun V2ChatMultiSelectBottomBar(
    selectedCount: Int,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White)
            .testTag("chat-multi-select-bottom-bar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onForward, enabled = selectedCount > 0) {
                Icon(Icons.AutoMirrored.Filled.Reply, "转发", tint = DSColors.fairway, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, enabled = selectedCount > 0) {
                Icon(Icons.Filled.Delete, "删除", tint = DSColors.fairway, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Text("$selectedCount 条选中", color = DSColors.inkGreen, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(
            "取消",
            color = DSColors.fairway,
            fontSize = 14.sp,
            modifier = Modifier.clickable(onClick = onCancel).padding(horizontal = 16.dp, vertical = 18.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2ChatForwardTypeSheet(
    onSeparate: () -> Unit,
    onMerged: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DSColors.warmWhite,
        dragHandle = null,
        modifier = Modifier.testTag("chat-forward-type-sheet"),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)) {
                Text(
                    "逐条转发",
                    color = DSColors.fairway,
                    fontSize = 17.sp,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onSeparate)
                        .padding(vertical = 18.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                HorizontalDivider(color = Color.Black.copy(alpha = 0.08f))
                Text(
                    "合并转发",
                    color = DSColors.fairway,
                    fontSize = 17.sp,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onMerged)
                        .padding(vertical = 18.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "取消",
                color = DSColors.fairway,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
                    .clickable(onClick = onDismiss).padding(vertical = 18.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
internal fun V2ChatForwardTargetSelector(
    conversations: List<V2IMConversation>,
    selectedUserIds: Set<String>,
    loading: Boolean,
    sending: Boolean,
    error: String?,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    V2BackHandler(enabled = !sending, onBack = onDismiss)
    Column(
        modifier = Modifier.fillMaxSize().background(DSColors.warmWhite).v2StatusBarPadding()
            .testTag("chat-forward-target-selector"),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(52.dp).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text("选择会话", color = DSColors.inkGreen, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "取消",
                color = DSColors.fairway,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
        Text(
            "最近聊天",
            color = DSColors.shadowGray,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 8.dp),
        )
        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DSColors.fairway)
            }
            conversations.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(error ?: "暂无最近聊天", color = DSColors.shadowGray, fontSize = 14.sp)
            }
            else -> androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            ) {
                items(conversations.size, key = { conversations[it].userId }) { index ->
                    val item = conversations[index]
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color.White)
                            .clickable { onToggle(item.userId) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("chat-forward-target-${item.userId}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        V2UserAvatar(
                            avatar = item.avatar,
                            fallbackText = item.title,
                            fallbackColor = DSColors.mistGreen,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(item.title.ifBlank { "未命名会话" }, color = DSColors.inkGreen, fontSize = 16.sp)
                        Spacer(Modifier.weight(1f))
                        V2ChatSelectionCircle(selected = item.userId in selectedUserIds)
                    }
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.05f), modifier = Modifier.padding(start = 68.dp))
                }
            }
        }
        error?.takeIf { conversations.isNotEmpty() }?.let {
            Text(it, color = DSColors.flagRed, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        if (selectedUserIds.isNotEmpty()) {
            HorizontalDivider(color = Color.Black.copy(alpha = 0.08f))
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp).background(Color.White),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    conversations.filter { it.userId in selectedUserIds }.forEach { item ->
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(DSColors.mistGreen)
                                .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(item.title, color = DSColors.inkGreen, fontSize = 14.sp, maxLines = 1)
                            Icon(
                                Icons.Filled.Close,
                                "取消选择${item.title}",
                                tint = DSColors.shadowGray,
                                modifier = Modifier.size(24.dp).clickable { onToggle(item.userId) }.padding(4.dp),
                            )
                        }
                    }
                }
                Button(
                    onClick = onConfirm,
                    enabled = !sending,
                    colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(end = 16.dp).height(40.dp).testTag("chat-forward-send"),
                ) {
                    if (sending) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    else Text("发送 (${selectedUserIds.size})", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}
