package com.mygolfswing.app.ui.interaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.coach.V2CoachQuickReply
import com.mygolfswing.app.data.coach.coachThrowableError
import com.mygolfswing.app.data.coach.filterQuickReplies
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.launch

@Composable
internal fun V2CoachQuickRepliesSheet(
    replies: List<V2CoachQuickReply>,
    onRepliesChanged: (List<V2CoachQuickReply>) -> Unit,
    onSelect: (V2CoachQuickReply) -> Unit,
    onDismiss: () -> Unit,
    loadReplies: suspend () -> Result<List<V2CoachQuickReply>> = { ServiceLocator.coachWorkspaceRepo.quickReplies() },
    saveReply: suspend (String?, String, String) -> Result<V2CoachQuickReply> = { id, title, text ->
        ServiceLocator.coachWorkspaceRepo.saveQuickReply(id, title, text)
    },
    deleteReply: suspend (String) -> Result<Unit> = { id -> ServiceLocator.coachWorkspaceRepo.deleteQuickReply(id) },
) {
    val scope = rememberCoroutineScope()
    var localReplies by remember(replies) { mutableStateOf(replies) }
    var query by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editorReply by remember { mutableStateOf<V2CoachQuickReply?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<V2CoachQuickReply?>(null) }
    val visibleReplies = localReplies.filterQuickReplies(query)

    fun load() {
        if (loading) return
        scope.launch {
            loading = true
            error = null
            loadReplies()
                .onSuccess { loaded ->
                    localReplies = loaded
                    onRepliesChanged(loaded)
                }
                .onFailure { error = coachThrowableError("常用回复暂时无法加载，请重试", it) }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    QuickReplyBottomSheetFrame(
        testTag = "quick-replies-sheet",
        onDismiss = { if (!busy) onDismiss() },
    ) {
        Box(Modifier.fillMaxSize()) {
            V2RoomBackground(Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize()) {
                QuickReplySheetHeader(title = "常用回复", leadingTitle = "取消", onLeading = onDismiss)
                Box(Modifier.weight(1f)) {
                    when {
                        loading && localReplies.isEmpty() -> QuickReplyLoading()
                        localReplies.isEmpty() -> QuickReplyEmpty(
                            error = error,
                            onCreate = { creating = true },
                            onRetry = ::load,
                            modifier = Modifier.fillMaxSize(),
                        )
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            item { QuickReplySearch(query = query, onQueryChanged = { query = it }) }
                            error?.let { message -> item { QuickReplyError(message, ::load) } }
                            if (visibleReplies.isEmpty()) {
                                item {
                                    Text(
                                        "没有找到匹配的常用回复",
                                        color = DSColors.shadowGray,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                                    )
                                }
                            } else {
                                items(visibleReplies, key = V2CoachQuickReply::quickReplyId) { reply ->
                                    QuickReplyCard(
                                        reply = reply,
                                        enabled = !busy,
                                        onSelect = { onSelect(reply); onDismiss() },
                                        onEdit = { editorReply = reply },
                                        onDelete = { deleteCandidate = reply },
                                    )
                                }
                            }
                        }
                    }
                }
                Box(
                    Modifier.fillMaxWidth().background(DSColors.warmWhite)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Button(
                        onClick = { creating = true },
                        enabled = localReplies.size < 30 && !busy,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = DSColors.deepGreen),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("quick-replies-create"),
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                        Text("新建常用回复", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 7.dp))
                    }
                }
            }
        }
    }

    if (creating || editorReply != null) {
        V2CoachQuickReplyEditorSheet(
            quickReply = editorReply,
            saveReply = saveReply,
            onSaved = { saved ->
                localReplies = listOf(saved) + localReplies.filterNot { it.quickReplyId == saved.quickReplyId }
                onRepliesChanged(localReplies)
                editorReply = null
                creating = false
            },
            onDismiss = {
                editorReply = null
                creating = false
            },
        )
    }

    deleteCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { if (!busy) deleteCandidate = null },
            title = { Text("删除这条常用回复？") },
            text = { Text("只会从你的常用回复中移除，不会影响已经发送的消息。") },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            deleteReply(candidate.quickReplyId)
                                .onSuccess {
                                    localReplies = localReplies.filterNot { it.quickReplyId == candidate.quickReplyId }
                                    onRepliesChanged(localReplies)
                                    deleteCandidate = null
                                }
                                .onFailure { error = coachThrowableError("删除失败，请重试", it) }
                            busy = false
                        }
                    },
                ) { Text(if (busy) "删除中…" else "删除", color = DSColors.flagRed) }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { deleteCandidate = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun QuickReplyBottomSheetFrame(
    testTag: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss,
                ),
        ) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(620.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(DSColors.warmWhite)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .navigationBarsPadding()
                    .testTag(testTag),
            ) {
                Box(Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 38.dp, height = 4.dp).clip(CircleShape).background(DSColors.borderLine))
                }
                Box(Modifier.weight(1f)) { content() }
            }
        }
    }
}

@Composable
private fun QuickReplySheetHeader(
    title: String,
    leadingTitle: String,
    onLeading: () -> Unit,
    trailingTitle: String? = null,
    trailingEnabled: Boolean = true,
    onTrailing: () -> Unit = {},
) {
    Box(Modifier.fillMaxWidth().height(52.dp).background(Color.White.copy(alpha = 0.92f))) {
        Text(
            leadingTitle,
            color = DSColors.fairway,
            fontSize = 15.sp,
            modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onLeading)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        )
        Text(title, color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        trailingTitle?.let {
            Text(
                it,
                color = if (trailingEnabled) DSColors.fairway else DSColors.shadowGray.copy(alpha = 0.45f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterEnd).clickable(enabled = trailingEnabled, onClick = onTrailing)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun QuickReplyLoading(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = DSColors.fairway, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        Text("正在同步常用回复…", color = DSColors.shadowGray, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun QuickReplyEmpty(error: String?, onCreate: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(70.dp).clip(CircleShape).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Textsms, null, tint = DSColors.fairway, modifier = Modifier.size(28.dp))
        }
        Text("还没有常用回复", color = DSColors.inkGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
        Text(
            "保存经常重复输入的指导，使用时仍可以继续修改。",
            color = DSColors.shadowGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        Button(
            onClick = onCreate,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
            modifier = Modifier.padding(top = 14.dp).height(40.dp),
        ) { Text("新建第一条", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        error?.let { QuickReplyError(it, onRetry, Modifier.padding(top = 14.dp)) }
    }
}

@Composable
private fun QuickReplySearch(query: String, onQueryChanged: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(14.dp)).background(DSColors.cardWhite)
            .padding(horizontal = 14.dp).testTag("quick-replies-search"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Search, null, tint = DSColors.shadowGray, modifier = Modifier.size(17.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            singleLine = true,
            textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 14.sp),
            cursorBrush = SolidColor(DSColors.fairway),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) Text("搜索回复内容", color = DSColors.shadowGray, fontSize = 14.sp)
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            Icon(
                Icons.Filled.Close,
                "清除搜索",
                tint = DSColors.shadowGray,
                modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onQueryChanged("") }.padding(2.dp),
            )
        }
    }
}

@Composable
private fun QuickReplyCard(
    reply: V2CoachQuickReply,
    enabled: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(17.dp))
            .clickable(enabled = enabled, onClick = onSelect).padding(13.dp)
            .testTag("quick-reply-${reply.quickReplyId}"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Textsms, null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    reply.title.ifBlank { "常用回复" },
                    color = DSColors.inkGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (reply.useCount > 0) {
                    Text(
                        "用过 ${reply.useCount} 次",
                        color = DSColors.fairway,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.height(20.dp).clip(CircleShape).background(DSColors.mistGreen.copy(alpha = 0.8f))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }
            Text(reply.text, color = DSColors.shadowGray, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Box {
            Icon(
                Icons.Filled.MoreHoriz,
                "更多操作",
                tint = DSColors.shadowGray,
                modifier = Modifier.size(34.dp).clip(CircleShape).clickable(enabled = enabled) { menuExpanded = true }.padding(9.dp),
            )
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("编辑") },
                    leadingIcon = { Icon(Icons.Filled.Edit, null) },
                    onClick = { menuExpanded = false; onEdit() },
                )
                DropdownMenuItem(
                    text = { Text("删除", color = DSColors.flagRed) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = DSColors.flagRed) },
                    onClick = { menuExpanded = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun QuickReplyError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color(0xFFF4E7C4).copy(alpha = 0.54f))
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(Icons.Filled.Error, null, tint = DSColors.trophyGold, modifier = Modifier.size(15.dp))
        Text(message, color = DSColors.trophyGold, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text("重试", color = DSColors.trophyGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onRetry).padding(4.dp))
    }
}

@Composable
private fun V2CoachQuickReplyEditorSheet(
    quickReply: V2CoachQuickReply?,
    saveReply: suspend (String?, String, String) -> Result<V2CoachQuickReply>,
    onSaved: (V2CoachQuickReply) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var title by remember(quickReply?.quickReplyId) { mutableStateOf(quickReply?.title.orEmpty()) }
    var text by remember(quickReply?.quickReplyId) { mutableStateOf(quickReply?.text.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun save() {
        if (busy || text.isBlank()) return
        busy = true
        error = null
        scope.launch {
            saveReply(quickReply?.quickReplyId, title, text)
                .onSuccess(onSaved)
                .onFailure { error = coachThrowableError("常用回复保存失败，请重试", it) }
            busy = false
        }
    }

    QuickReplyBottomSheetFrame(
        testTag = "quick-reply-editor-sheet",
        onDismiss = { if (!busy) onDismiss() },
    ) {
        Box(Modifier.fillMaxSize()) {
            V2RoomBackground(Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize()) {
                QuickReplySheetHeader(
                    title = if (quickReply == null) "新建常用回复" else "编辑常用回复",
                    leadingTitle = "取消",
                    onLeading = onDismiss,
                    trailingTitle = if (busy) "保存中…" else "保存",
                    trailingEnabled = !busy && text.isNotBlank(),
                    onTrailing = ::save,
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        QuickReplyFieldSection(
                            label = "标题",
                            count = "${title.length}/20",
                        ) {
                            QuickReplyTextField(
                                value = title,
                                onValueChanged = { title = it.take(20); error = null },
                                placeholder = "例如：保持脊柱角度",
                                singleLine = true,
                                modifier = Modifier.height(44.dp).testTag("quick-reply-title"),
                            )
                        }
                    }
                    item {
                        QuickReplyFieldSection(
                            label = "回复内容",
                            count = "${text.length}/1000",
                            countFontSize = 9,
                        ) {
                            QuickReplyTextField(
                                value = text,
                                onValueChanged = { text = it.take(1_000); error = null },
                                placeholder = "写下经常使用的专业建议…",
                                singleLine = false,
                                modifier = Modifier.heightIn(min = 160.dp).testTag("quick-reply-text"),
                            )
                        }
                    }
                    if (text.isNotBlank()) item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("预览效果", color = DSColors.inkGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .background(DSColors.mistGreen.copy(alpha = 0.7f)).padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (title.isNotBlank()) Text(title, color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text, color = DSColors.shadowGray, fontSize = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Lock, null, tint = DSColors.shadowGray, modifier = Modifier.size(12.dp))
                            Text("常用回复只对你可见，选择后不会自动发送。", color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    error?.let { message -> item { QuickReplyError(message, ::save) } }
                }
            }
        }
    }
}

@Composable
private fun QuickReplyFieldSection(
    label: String,
    count: String,
    countFontSize: Int = 11,
    field: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row {
            Text(label, color = DSColors.inkGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(count, color = DSColors.shadowGray, fontSize = countFontSize.sp, fontWeight = FontWeight.Medium)
        }
        field()
    }
}

@Composable
private fun QuickReplyTextField(
    value: String,
    onValueChanged: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChanged,
        singleLine = singleLine,
        textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 14.sp),
        cursorBrush = SolidColor(DSColors.fairway),
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(if (singleLine) 14.dp else 16.dp))
            .background(DSColors.cardWhite).border(1.dp, DSColors.borderLine, RoundedCornerShape(if (singleLine) 14.dp else 16.dp))
            .padding(horizontal = 12.dp, vertical = if (singleLine) 12.dp else 10.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = DSColors.shadowGray, fontSize = 14.sp)
                inner()
            }
        },
    )
}
