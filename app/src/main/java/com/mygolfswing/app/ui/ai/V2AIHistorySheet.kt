package com.mygolfswing.app.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.Forum as ForumOutlined
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2AIMessageRole
import com.mygolfswing.app.data.model.V2AISession
import com.mygolfswing.app.data.model.V2ChatAttachment
import com.mygolfswing.app.data.model.V2ChatMessage
import com.mygolfswing.app.data.store.V2AIChatStore
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2AIHistorySheet(onDismiss: () -> Unit) {
    val sessions by V2AIChatStore.sessions.collectAsState()
    val current by V2AIChatStore.currentSession.collectAsState()
    val status by V2AIChatStore.statusBar.collectAsState()
    val historySessions = remember(current, sessions) { aiHistorySessions(current, sessions) }
    val historyStatus = remember(status) { status?.takeIf(::isAIHistoryStatus) }

    V2AIHistorySheetContent(
        historySessions = historySessions,
        currentSessionId = current.id,
        historyStatus = historyStatus,
        onDismiss = onDismiss,
        onNew = {
            V2AIChatStore.startNewSession()
            onDismiss()
        },
        onOpenSession = { sessionId ->
            if (sessionId != current.id) V2AIChatStore.switchSession(sessionId)
            onDismiss()
        },
        onDeleteSession = { sessionId ->
            V2AIChatStore.deleteSession(sessionId)
            if (sessionId == current.id) onDismiss()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2AIHistorySheetContent(
    historySessions: List<V2AISession>,
    currentSessionId: java.util.UUID?,
    historyStatus: String?,
    onDismiss: () -> Unit,
    onNew: () -> Unit,
    onOpenSession: (java.util.UUID) -> Unit,
    onDeleteSession: (java.util.UUID) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(),
        sheetState = sheetState,
        containerColor = DSColors.warmWhite,
        dragHandle = { AIHistoryDragHandle() },
    ) {
        Box(Modifier.fillMaxSize()) {
            V2RoomBackground(Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize()) {
                AIHistoryHeader(
                    onClose = onDismiss,
                    onNew = onNew,
                )
                historyStatus?.let { AIHistoryStatusBanner(it) }
                if (historySessions.isEmpty()) {
                    AIHistoryEmptyState(
                        expanded = sheetState.currentValue == SheetValue.Expanded,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .v2NavigationBarPadding(),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .v2NavigationBarPadding(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                    ) {
                        items(historySessions, key = { it.id }) { session ->
                            val selected = session.id == currentSessionId
                            AISessionCard(
                                session = session,
                                selected = selected,
                                onOpen = { onOpenSession(session.id) },
                                onDelete = { onDeleteSession(session.id) },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
        }
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
                .semantics { contentDescription = "新建对话" }
                .clickable(onClick = onNew)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            AIHistoryNewConversationIcon(Modifier.size(16.dp))
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(DSColors.fairway.copy(alpha = 0.06f)),
    )
}

@Composable
private fun AIHistoryNewConversationIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.105f
        drawRoundRect(
            color = DSColors.fairway,
            topLeft = Offset(size.width * 0.08f, size.height * 0.20f),
            size = Size(size.width * 0.64f, size.height * 0.70f),
            cornerRadius = CornerRadius(size.minDimension * 0.08f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = DSColors.fairway,
            start = Offset(size.width * 0.40f, size.height * 0.72f),
            end = Offset(size.width * 0.91f, size.height * 0.20f),
            strokeWidth = stroke * 1.2f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = DSColors.fairway,
            start = Offset(size.width * 0.36f, size.height * 0.77f),
            end = Offset(size.width * 0.51f, size.height * 0.73f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun AIHistoryStatusBanner(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DSColors.flagRed.copy(alpha = 0.08f))
            .border(1.dp, DSColors.flagRed.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = DSColors.flagRed, modifier = Modifier.size(16.dp))
        Text(
            text,
            color = DSColors.flagRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
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

    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
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
                .semantics {
                    contentDescription = if (selected) "当前对话：${session.title}" else "对话：${session.title}"
                }
                .combinedClickable(
                    enabled = true,
                    onClick = onOpen,
                    onLongClick = { if (onDelete != null) menuExpanded = true },
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
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
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            containerColor = DSColors.cardWhite,
        ) {
            DropdownMenuItem(
                text = { Text("删除", color = DSColors.flagRed, fontWeight = FontWeight.Medium) },
                leadingIcon = {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = DSColors.flagRed)
                },
                onClick = {
                    menuExpanded = false
                    onDelete?.invoke()
                },
            )
        }
    }
}

@Composable
private fun AIHistoryEmptyState(
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(if (expanded) 1f else 0.5f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Icon(Icons.Outlined.ForumOutlined, contentDescription = null, tint = DSColors.shadowGray.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
            Text("还没有对话记录", color = DSColors.shadowGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

internal fun aiHistorySessions(
    current: V2AISession,
    sessions: List<V2AISession>,
): List<V2AISession> =
    (listOf(current) + sessions)
        .distinctBy(V2AISession::id)
        .sortedByDescending(V2AISession::updatedAt)

internal fun isAIHistoryStatus(status: String): Boolean =
    status.startsWith("历史会话") ||
        status.startsWith("删除对话") ||
        status.startsWith("新对话")

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
