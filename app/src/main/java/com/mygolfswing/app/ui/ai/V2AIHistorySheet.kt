package com.mygolfswing.app.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.outlined.Forum as ForumOutlined
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2AIMessageRole
import com.mygolfswing.app.data.model.V2AISession
import com.mygolfswing.app.data.model.V2ChatAttachment
import com.mygolfswing.app.data.model.V2ChatMessage
import com.mygolfswing.app.data.store.V2AIChatStore
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2AIHistorySheet(onDismiss: () -> Unit) {
    val sessions by V2AIChatStore.sessions.collectAsState()
    val current by V2AIChatStore.currentSession.collectAsState()
    var pendingDelete by remember { mutableStateOf<V2AISession?>(null) }
    val currentHasConversation = remember(current.messages) {
        current.messages.any { it.role == V2AIMessageRole.USER }
    }
    val hasAnySession = currentHasConversation || sessions.isNotEmpty()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(),
        sheetState = sheetState,
        containerColor = DSColors.warmWhite,
        dragHandle = { AIHistoryDragHandle() },
    ) {
        AIHistoryHeader(
            onClose = onDismiss,
            onNew = {
                V2AIChatStore.startNewSession()
                onDismiss()
            },
        )
        if (!hasAnySession) {
            AIHistoryEmptyState(
                modifier = Modifier
                    .fillMaxWidth()
                    .v2NavigationBarPadding()
                    .padding(vertical = 72.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .v2NavigationBarPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            ) {
                if (currentHasConversation) {
                    item {
                        AISessionCard(
                            session = current,
                            selected = true,
                            onOpen = {},
                            onDelete = null,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                if (sessions.isNotEmpty()) {
                    items(sessions, key = { it.id }) { session ->
                        AISessionCard(
                            session = session,
                            selected = false,
                            onOpen = {
                                V2AIChatStore.switchSession(session.id)
                                onDismiss()
                            },
                            onDelete = { pendingDelete = session },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除对话记录") },
            text = { Text("确定要删除「${session.title}」吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        V2AIChatStore.deleteSession(session.id)
                        pendingDelete = null
                    },
                ) {
                    Text("删除", color = DSColors.flagRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun AIHistoryDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 8.dp, bottom = 8.dp)
            .width(36.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.20f)),
    )
}

@Composable
private fun AIHistoryHeader(onClose: () -> Unit, onNew: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "关闭",
            color = DSColors.shadowGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onClose)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
        Text(
            "对话记录",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = DSColors.inkGreen,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(24.dp)
                .clickable(onClick = onNew)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.EditNote, contentDescription = "新建对话", tint = DSColors.fairway, modifier = Modifier.size(16.dp))
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(DSColors.fairway.copy(alpha = 0.06f)),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AISessionCard(
    session: V2AISession,
    selected: Boolean,
    onOpen: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val preview = remember(session.messages) { session.previewText() }
    val messageCount = remember(session.messages) { session.messages.count { it.role != V2AIMessageRole.SYSTEM } }
    val time = remember(session.updatedAt) { session.timeLabel() }
    val borderColor = if (selected) DSColors.fairway else DSColors.fairway.copy(alpha = 0.08f)
    val background = if (selected) Color(0xFFD8E2DC).copy(alpha = 0.30f) else DSColors.cardWhite
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = shape,
                        ambientColor = DSColors.fairway.copy(alpha = 0.10f),
                        spotColor = DSColors.fairway.copy(alpha = 0.10f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .combinedClickable(
                enabled = !selected || onDelete != null,
                onClick = {
                    if (!selected) onOpen()
                },
                onLongClick = onDelete,
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    session.title,
                    color = if (selected) DSColors.fairway else DSColors.inkGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (selected) {
                    Text(
                        "当前",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DSColors.fairway)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Text(
                    "${messageCount}条",
                    color = DSColors.shadowGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (preview.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DSColors.fairway),
                        )
                    }
                    Text(
                        preview,
                        color = DSColors.shadowGray,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(time.day, color = DSColors.fairway, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(time.clock, color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun AIHistoryEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.ForumOutlined, contentDescription = null, tint = DSColors.shadowGray.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
        Text("还没有对话记录", color = DSColors.shadowGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

private fun V2AISession.previewText(): String =
    messages.asReversed()
        .firstOrNull { it.role != V2AIMessageRole.SYSTEM && it.previewText().isNotBlank() }
        ?.previewText()
        .orEmpty()

private fun V2ChatMessage.previewText(): String =
    text.takeIf { it.isNotBlank() }
        ?: attachments.firstOrNull()?.previewText()
        ?: ""

private fun V2ChatAttachment.previewText(): String = when (this) {
    is V2ChatAttachment.VideoRef -> "视频引用：$title"
    is V2ChatAttachment.MemoSaved -> "心得已记录：$title"
    is V2ChatAttachment.PlazaRecommend -> "相关视频：$title"
}

private data class HistoryTimeLabel(val day: String, val clock: String)

private fun V2AISession.timeLabel(): HistoryTimeLabel {
    val day = updatedAt.toLocalDate()
    val date = when (day) {
        LocalDate.now() -> "今天"
        LocalDate.now().minusDays(1) -> "昨天"
        else -> day.format(DateTimeFormatter.ofPattern("M月d日"))
    }
    return HistoryTimeLabel(date, updatedAt.format(DateTimeFormatter.ofPattern("HH:mm")))
}
