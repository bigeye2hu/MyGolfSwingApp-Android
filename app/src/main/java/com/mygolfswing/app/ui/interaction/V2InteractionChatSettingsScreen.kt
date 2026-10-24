package com.mygolfswing.app.ui.interaction

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.coach.coachThrowableError
import com.mygolfswing.app.data.friend.V2C2CSettings
import com.mygolfswing.app.data.friend.V2FriendRequestOutcome
import com.mygolfswing.app.data.friend.V2IMConversation
import com.mygolfswing.app.data.session.V2Session
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.launch

@Composable
internal fun V2InteractionChatSettingsScreen(
    conversation: V2IMConversation,
    onBack: () -> Unit,
    onRemarkChanged: (String) -> Unit = {},
    onContactDeleted: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var settings by remember(conversation.userId) { mutableStateOf<V2C2CSettings?>(null) }
    var loading by remember(conversation.userId) { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var showRemark by rememberSaveable { mutableStateOf(false) }
    var remarkDraft by rememberSaveable { mutableStateOf("") }
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var showReport by rememberSaveable { mutableStateOf(false) }
    var showAddFriend by rememberSaveable { mutableStateOf(false) }
    var addFriendDraft by rememberSaveable { mutableStateOf("") }

    V2BackHandler(onBack = onBack)

    suspend fun renewTencentSession(): V2Session {
        ServiceLocator.authRepo.refreshUserSig().getOrThrow()
        return ServiceLocator.session.current()
    }

    suspend fun reload() {
        loading = true
        error = null
        ServiceLocator.tencentFriendRepo.c2cSettings(
            session = ServiceLocator.session.current(),
            userId = conversation.userId,
            renewExpiredSession = ::renewTencentSession,
        ).onSuccess { settings = it }
            .onFailure { error = coachThrowableError("聊天设置暂时无法加载", it) }
        loading = false
    }

    fun mutate(
        optimistic: (V2C2CSettings) -> V2C2CSettings,
        operation: suspend () -> Result<Unit>,
        failureCopy: String,
    ) {
        val previous = settings ?: return
        settings = optimistic(previous)
        saving = true
        error = null
        scope.launch {
            operation().onFailure {
                settings = previous
                error = coachThrowableError(failureCopy, it)
            }
            saving = false
        }
    }

    LaunchedEffect(conversation.userId) { reload() }

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            V2NavTopBarCentered(title = "详细资料", onBack = onBack)
            when {
                loading && settings == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DSColors.fairway)
                }
                settings == null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(error ?: "聊天设置暂时无法加载", color = DSColors.shadowGray, textAlign = TextAlign.Center)
                    Text("重新加载", color = DSColors.fairway, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { scope.launch { reload() } }.padding(12.dp))
                }
                else -> V2InteractionChatSettingsContent(
                    conversation = conversation,
                    settings = settings!!,
                    saving = saving,
                    error = error,
                    notice = notice,
                    onRemark = {
                        remarkDraft = settings!!.remark
                        showRemark = true
                    },
                    onMuted = { value ->
                        mutate(
                            optimistic = { it.copy(isMuted = value) },
                            operation = {
                                ServiceLocator.tencentFriendRepo.setC2CMuted(
                                    ServiceLocator.session.current(), conversation.userId, value, ::renewTencentSession,
                                )
                            },
                            failureCopy = "消息免打扰暂时无法更新",
                        )
                    },
                    onPinned = { value ->
                        mutate(
                            optimistic = { it.copy(isPinned = value) },
                            operation = {
                                ServiceLocator.tencentFriendRepo.setC2CPinned(
                                    ServiceLocator.session.current(), conversation.userId, value, ::renewTencentSession,
                                )
                            },
                            failureCopy = "置顶聊天暂时无法更新",
                        )
                    },
                    onBlocked = { value ->
                        mutate(
                            optimistic = { it.copy(isBlocked = value) },
                            operation = {
                                ServiceLocator.tencentFriendRepo.setBlocked(
                                    ServiceLocator.session.current(), conversation.userId, value, ::renewTencentSession,
                                )
                            },
                            failureCopy = "黑名单状态暂时无法更新",
                        )
                    },
                    onReport = { showReport = true },
                    onClearHistory = { showClearConfirmation = true },
                    onDeleteFriend = { showDeleteConfirmation = true },
                    onAddFriend = {
                        scope.launch {
                            val current = ServiceLocator.session.current()
                            addFriendDraft = "你好，我是 ${current.nickname ?: current.username ?: "用户"}"
                            showAddFriend = true
                        }
                    },
                )
            }
        }
    }

    if (showRemark) {
        AlertDialog(
            onDismissRequest = { if (!saving) showRemark = false },
            title = { Text("设置备注名") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = remarkDraft,
                        onValueChange = { remarkDraft = it },
                        singleLine = true,
                        placeholder = { Text("请输入备注名") },
                    )
                    Text("备注名仅自己可见", color = DSColors.shadowGray, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        saving = true
                        scope.launch {
                            ServiceLocator.tencentFriendRepo.setFriendRemark(
                                ServiceLocator.session.current(), conversation.userId, remarkDraft, ::renewTencentSession,
                            ).onSuccess {
                                settings = settings?.copy(remark = remarkDraft.trim())
                                onRemarkChanged(remarkDraft.trim())
                                notice = "备注已保存"
                                showRemark = false
                            }.onFailure { error = coachThrowableError("备注暂时无法保存", it) }
                            saving = false
                        }
                    },
                    enabled = !saving,
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showRemark = false }, enabled = !saving) { Text("取消") } },
        )
    }
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("清空聊天记录") },
            text = { Text("确定清空聊天记录？") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirmation = false
                    saving = true
                    scope.launch {
                        ServiceLocator.tencentFriendRepo.clearC2CHistory(
                            ServiceLocator.session.current(), conversation.userId, ::renewTencentSession,
                        ).onSuccess { notice = "聊天记录已清空" }
                            .onFailure { error = coachThrowableError("聊天记录暂时无法清空", it) }
                        saving = false
                    }
                }) { Text("清空", color = Color(0xFFB23B35)) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirmation = false }) { Text("取消") } },
        )
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("删除好友") },
            text = { Text("确定要删除该好友吗？删除后将无法收到对方消息。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    saving = true
                    scope.launch {
                        ServiceLocator.tencentFriendRepo.deleteFriend(
                            ServiceLocator.session.current(), conversation.userId, ::renewTencentSession,
                        ).onSuccess { onContactDeleted() }
                            .onFailure { error = coachThrowableError("删除好友失败", it) }
                        saving = false
                    }
                }) { Text("删除", color = Color(0xFFB23B35)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("取消") } },
        )
    }
    if (showReport) {
        V2ChatReportSheet(
            userName = conversation.title,
            submitting = saving,
            onDismiss = { showReport = false },
            onSubmit = { reason, category, detail, block ->
                saving = true
                scope.launch {
                    ServiceLocator.authRepo.reportUser(conversation.userId, reason, category, detail)
                        .onSuccess {
                            if (block) {
                                ServiceLocator.tencentFriendRepo.setBlocked(
                                    ServiceLocator.session.current(), conversation.userId, true, ::renewTencentSession,
                                ).onSuccess {
                                    settings = settings?.copy(isBlocked = true)
                                    notice = "举报已提交，该用户已被屏蔽"
                                    showReport = false
                                }.onFailure {
                                    error = coachThrowableError("举报已提交，但拉黑失败", it)
                                }
                            } else {
                                notice = "举报已提交，我们将在24小时内处理"
                                showReport = false
                            }
                        }.onFailure { error = coachThrowableError("举报提交失败", it) }
                    saving = false
                }
            },
        )
    }
    if (showAddFriend) {
        V2AddFriendSheet(
            displayName = settings?.remark?.ifBlank { conversation.title } ?: conversation.title,
            message = addFriendDraft,
            submitting = saving,
            onMessageChange = { addFriendDraft = it },
            onDismiss = { showAddFriend = false },
            onSend = {
                showAddFriend = false
                saving = true
                scope.launch {
                    ServiceLocator.tencentFriendRepo.sendFriendRequest(
                        session = ServiceLocator.session.current(),
                        targetUserId = conversation.userId,
                        renewExpiredSession = ::renewTencentSession,
                        addWording = addFriendDraft,
                    ).onSuccess { outcome ->
                        notice = when (outcome) {
                            V2FriendRequestOutcome.SENT -> "好友请求已发送，等待对方确认"
                            V2FriendRequestOutcome.ALREADY_FRIENDS -> "对方已经是你的好友"
                            V2FriendRequestOutcome.ACCEPTED -> "好友申请已接受"
                        }
                        if (outcome == V2FriendRequestOutcome.ALREADY_FRIENDS ||
                            outcome == V2FriendRequestOutcome.ACCEPTED
                        ) settings = settings?.copy(isFriend = true)
                    }.onFailure { error = coachThrowableError("好友请求发送失败", it) }
                    saving = false
                }
            },
        )
    }
}

@Composable
internal fun V2InteractionChatSettingsContent(
    conversation: V2IMConversation,
    settings: V2C2CSettings,
    saving: Boolean,
    error: String?,
    notice: String?,
    onRemark: () -> Unit,
    onMuted: (Boolean) -> Unit,
    onPinned: (Boolean) -> Unit,
    onBlocked: (Boolean) -> Unit,
    onReport: () -> Unit,
    onClearHistory: () -> Unit,
    onDeleteFriend: () -> Unit,
    onAddFriend: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).v2NavigationBarPadding()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(DSColors.deepGreen)
                .padding(vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            V2UserAvatar(
                avatar = conversation.avatar,
                modifier = Modifier.size(82.dp),
                fallbackText = settings.remark.ifBlank { conversation.title },
                fallbackColor = DSColors.sandBeige,
            )
            Text(
                settings.remark.ifBlank { conversation.title },
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        error?.let { SettingsNotice(it, Color(0xFFFFE8E4), Color(0xFF9C3830)) }
        notice?.let { SettingsNotice(it, DSColors.mistGreen, DSColors.deepGreen) }
        SettingsGroup {
            SettingsNavigationRow("备注名", settings.remark.ifBlank { "未设置" }, saving, onRemark)
        }
        SettingsGroup {
            SettingsSwitchRow("消息免打扰", settings.isMuted, saving, onMuted)
            SettingsDivider()
            SettingsSwitchRow("置顶聊天", settings.isPinned, saving, onPinned)
        }
        SettingsGroup {
            SettingsSwitchRow("加入黑名单", settings.isBlocked, saving, onBlocked)
        }
        SettingsGroup {
            SettingsButtonRow("举报用户", Color(0xFFC07A28), saving, onReport)
        }
        SettingsGroup {
            SettingsButtonRow("清空聊天记录", Color(0xFFB23B35), saving, onClearHistory)
            SettingsDivider()
            if (settings.isFriend) {
                SettingsButtonRow("删除好友", Color(0xFFB23B35), saving, onDeleteFriend)
            } else {
                SettingsButtonRow("添加好友", DSColors.fairway, saving, onAddFriend, Icons.Filled.PersonAdd)
            }
        }
        if (saving) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(16.dp), color = DSColors.fairway, strokeWidth = 2.dp)
                Text("正在保存…", color = DSColors.shadowGray, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.96f))) { content() }
}

@Composable
private fun SettingsNavigationRow(title: String, subtitle: String, disabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = !disabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = DSColors.deepGreen, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(subtitle, color = DSColors.shadowGray, fontSize = 13.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = DSColors.shadowGray, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsSwitchRow(title: String, checked: Boolean, disabled: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = !disabled,
                role = Role.Switch,
                onValueChange = onChecked,
            )
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = DSColors.deepGreen, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = !disabled)
    }
}

@Composable
private fun SettingsButtonRow(
    title: String,
    color: Color,
    disabled: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = !disabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, null, tint = color, modifier = Modifier.size(18.dp).padding(end = 4.dp)) }
        Text(title, color = color, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = DSColors.shadowGray, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(DSColors.borderLine))
}

@Composable
private fun SettingsNotice(text: String, background: Color, foreground: Color) {
    Text(text, color = foreground, fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(background).padding(12.dp))
}

private data class ReportReason(val title: String, val category: String, val symbol: String)

@Composable
internal fun V2ChatReportSheet(
    userName: String,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String?, Boolean) -> Unit,
) {
    val reasons = remember {
        listOf(
            ReportReason("骚扰辱骂", "harassment", "!"),
            ReportReason("垃圾信息", "spam", "⌫"),
            ReportReason("不当内容", "inappropriate", "◉"),
            ReportReason("欺诈诱导", "fraud", "¥"),
            ReportReason("暴力威胁", "violence", "♨"),
            ReportReason("其他原因", "other", "…"),
        )
    }
    var selectedCategory by rememberSaveable { mutableStateOf(reasons.first().category) }
    val selected = reasons.firstOrNull { it.category == selectedCategory } ?: reasons.first()
    var detail by rememberSaveable { mutableStateOf("") }
    Dialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding().imePadding()) {
            Box(Modifier.fillMaxWidth().height(40.dp)) {
                Text(
                    "取消",
                    color = Color(0xFFC17BFF),
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.CenterStart).clickable(enabled = !submitting, onClick = onDismiss)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                )
                Text(
                    "举报用户",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.06f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(50.dp).clip(CircleShape).background(Color.Red.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(userName.take(1).uppercase(), color = Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("举报对象", color = Color.Gray, fontSize = 12.sp)
                        Text(userName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFF9800).copy(alpha = 0.12f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("⚠", color = Color(0xFFFFA726), fontSize = 24.sp)
                    Text(
                        "请选择举报原因，我们将在24小时内处理您的举报",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("举报原因", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    reasons.chunked(2).forEach { rowReasons ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowReasons.forEach { reason ->
                                val active = selected == reason
                                Row(
                                    Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                        .background(if (active) Color(0xFF9C45D8) else Color.White.copy(alpha = 0.08f))
                                        .border(
                                            1.dp,
                                            if (active) Color(0xFFB15CE8) else Color.Transparent,
                                            RoundedCornerShape(8.dp),
                                        )
                                        .clickable(enabled = !submitting) { selectedCategory = reason.category }
                                        .padding(horizontal = 12.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(reason.symbol, color = if (active) Color.White else Color.Gray, fontSize = 14.sp)
                                    Text("  ${reason.title}", color = if (active) Color.White else Color.Gray, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("详细描述（可选）", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = detail,
                        onValueChange = { detail = it.take(500) },
                        placeholder = { Text("请描述该用户的违规行为...", color = Color.Gray.copy(alpha = 0.55f)) },
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth().height(116.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.08f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                            focusedBorderColor = Color.White.copy(alpha = 0.24f),
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = Color.White,
                        ),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onSubmit(selected.title, selected.category, detail, false) },
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C45D8)),
                    ) {
                        if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("⚑  提交举报", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onSubmit(selected.title, selected.category, detail, true) },
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB23B35)),
                    ) { Text("⊘  举报并拉黑", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
internal fun V2AddFriendSheet(
    displayName: String,
    message: String,
    submitting: Boolean,
    onMessageChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        V2RoomPage(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(40.dp)) {
                    Text(
                        "取消",
                        color = DSColors.fairway,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.CenterStart).clickable(enabled = !submitting, onClick = onDismiss)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                    )
                    Text(
                        "添加好友",
                        color = DSColors.deepGreen,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "添加 $displayName 为好友",
                        color = DSColors.deepGreen,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    Text(
                        "对方需要确认后才能成为好友",
                        color = DSColors.shadowGray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 10.dp, bottom = 22.dp),
                    )
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("验证信息", color = DSColors.shadowGray, fontSize = 14.sp)
                        OutlinedTextField(
                            value = message,
                            onValueChange = onMessageChange,
                            placeholder = { Text("请输入验证信息") },
                            enabled = !submitting,
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            maxLines = 4,
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onSend,
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                ) {
                    if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("发送好友请求", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
