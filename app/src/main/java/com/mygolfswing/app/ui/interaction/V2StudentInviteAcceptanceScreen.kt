package com.mygolfswing.app.ui.interaction

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.coach.V2CoachConnection
import com.mygolfswing.app.data.coach.V2CoachInvitePreview
import com.mygolfswing.app.data.coach.V2PendingCoachInvite
import com.mygolfswing.app.data.coach.coachThrowableError
import com.mygolfswing.app.data.coach.parseCoachInviteLink
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun normalizeCoachInviteCode(value: String): String = value
    .uppercase()
    .filter(Char::isLetterOrDigit)
    .take(8)

internal fun validScannedCoachInvite(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (parseCoachInviteLink(value) != null) return value
    return normalizeCoachInviteCode(value).takeIf { it.length == 8 }
}

@Composable
internal fun V2StudentInviteAcceptanceDialog(
    pendingInvite: V2PendingCoachInvite? = null,
    onDismiss: () -> Unit,
    onAccepted: (V2CoachConnection) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        V2StudentInviteAcceptanceScreen(
            pendingInvite = pendingInvite,
            onDismiss = onDismiss,
            onAccepted = onAccepted,
        )
    }
}

@Composable
internal fun V2StudentInviteAcceptanceScreen(
    pendingInvite: V2PendingCoachInvite? = null,
    onDismiss: () -> Unit,
    onAccepted: (V2CoachConnection) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var code by remember(pendingInvite) { mutableStateOf(normalizeCoachInviteCode(pendingInvite?.inviteCode.orEmpty())) }
    var activePendingInvite by remember(pendingInvite) { mutableStateOf(pendingInvite) }
    var usesPendingLink by remember(pendingInvite) { mutableStateOf(pendingInvite != null) }
    var preview by remember(pendingInvite) { mutableStateOf<V2CoachInvitePreview?>(null) }
    var loading by remember(pendingInvite) { mutableStateOf(false) }
    var error by remember(pendingInvite) { mutableStateOf<String?>(null) }
    var showScanner by remember { mutableStateOf(false) }
    var nowEpochMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun previewCode() {
        if (code.length != 8 || loading) return
        loading = true
        error = null
        scope.launch {
            ServiceLocator.coachWorkspaceRepo.previewInvite(code)
                .onSuccess {
                    preview = it
                    usesPendingLink = false
                    activePendingInvite = null
                }
                .onFailure { error = coachThrowableError("邀请码无效或已过期", it) }
            loading = false
        }
    }

    fun previewPending(invite: V2PendingCoachInvite) {
        loading = true
        error = null
        activePendingInvite = invite
        usesPendingLink = true
        code = normalizeCoachInviteCode(invite.inviteCode.orEmpty())
        scope.launch {
            ServiceLocator.coachWorkspaceRepo.previewInvite(invite)
                .onSuccess { preview = it }
                .onFailure { error = coachThrowableError("邀请链接无效或已过期", it) }
            loading = false
        }
    }

    fun useAnotherCode() {
        preview = null
        activePendingInvite = null
        usesPendingLink = false
        code = ""
        error = null
    }

    LaunchedEffect(pendingInvite) {
        pendingInvite?.let(::previewPending)
    }

    LaunchedEffect(preview?.inviteId) {
        while (preview != null) {
            delay(30_000)
            nowEpochMillis = System.currentTimeMillis()
        }
    }

    BackHandler {
        if (loading) return@BackHandler
        onDismiss()
    }

    Box(
        Modifier.fillMaxSize().background(DSColors.warmWhite).testTag("student_invite_acceptance"),
    ) {
        V2RoomBackground(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        ) {
            Box(
                Modifier.fillMaxWidth().height(40.dp).background(DSColors.warmWhite.copy(alpha = 0.96f)),
            ) {
                Text(
                    "确认教练邀请",
                    color = DSColors.inkGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center),
                )
                Text(
                    "取消",
                    color = if (loading) DSColors.shadowGray.copy(alpha = 0.45f) else DSColors.deepGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterStart).clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !loading, onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(DSColors.mistGreen),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.ManageAccounts, contentDescription = null, tint = DSColors.fairway, modifier = Modifier.size(34.dp))
                    }
                    Text(
                        "认证教练邀请你进入挥呗教学空间",
                        color = DSColors.inkGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }

                if (preview == null) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                            .background(DSColors.cardWhite).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { error = null; showScanner = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("扫描教练二维码", fontWeight = FontWeight.Bold)
                        }
                        Text("或输入 8 位连接码", color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = normalizeCoachInviteCode(it); error = null },
                            placeholder = { Text("例如 A7K9M2QP") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = DSColors.inkGreen,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 3.sp,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DSColors.fairway,
                                unfocusedBorderColor = DSColors.borderLine,
                                focusedContainerColor = DSColors.cardWhite,
                                unfocusedContainerColor = DSColors.cardWhite,
                                cursorColor = DSColors.fairway,
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("student_invite_code"),
                        )
                        Button(
                            onClick = ::previewCode,
                            enabled = code.length == 8 && !loading,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DSColors.fairway,
                                disabledContainerColor = DSColors.fairway.copy(alpha = 0.45f),
                            ),
                        ) {
                            if (loading) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.size(8.dp))
                            }
                            Text("查看教练信息", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    StudentInviteCoachPreview(preview = preview!!)
                    if (studentInviteIsExpired(preview!!.expiresAt, Instant.ofEpochMilli(nowEpochMillis))) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                                .background(Color(0xFFF4EAD0)).padding(15.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Error, contentDescription = null, tint = Color(0xFFAF7A23))
                            Column(Modifier.weight(1f)) {
                                Text("这份邀请已经失效", color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("请让教练重新生成邀请，或输入另一组连接码", color = DSColors.shadowGray, fontSize = 11.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                loading = true
                                error = null
                                scope.launch {
                                    val result = if (usesPendingLink && activePendingInvite != null) {
                                        ServiceLocator.coachWorkspaceRepo.acceptInvite(activePendingInvite!!)
                                    } else {
                                        ServiceLocator.coachWorkspaceRepo.acceptInvite(code)
                                    }
                                    result.onSuccess(onAccepted)
                                        .onFailure { error = coachThrowableError("暂时无法建立连接，请稍后重试", it) }
                                    loading = false
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                        ) {
                            if (loading) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.size(8.dp))
                            }
                            Text("确认连接", fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = ::useAnotherCode, enabled = !loading) {
                        Text("使用其他连接码", color = DSColors.fairway, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                error?.let {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = Color(0xFFB23B35), modifier = Modifier.size(17.dp))
                        Text(it, color = Color(0xFFB23B35), fontSize = 12.sp)
                    }
                }
                Text(
                    "只有你主动确认后才会建立连接。教学内容默认仅对你和教练可见。",
                    color = DSColors.shadowGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp),
                )
            }
        }
    }

    if (showScanner) {
        Dialog(
            onDismissRequest = { showScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            V2QrScannerScreen(
                title = "扫描邀请",
                instruction = "对准教练邀请页上的二维码",
                invalidMessage = "这不是挥呗的教练邀请二维码",
                allowTorch = true,
                inviteStyle = true,
                validate = ::validScannedCoachInvite,
                onDismiss = { showScanner = false },
                onScanned = { raw ->
                    showScanner = false
                    parseCoachInviteLink(raw)?.let(::previewPending) ?: run {
                        code = normalizeCoachInviteCode(raw)
                        previewCode()
                    }
                },
            )
        }
    }
}

@Composable
private fun StudentInviteCoachPreview(preview: V2CoachInvitePreview) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(22.dp)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        V2UserAvatar(
            avatar = preview.coach.avatar,
            modifier = Modifier.size(72.dp),
            fallbackFontSize = 25.sp,
            placeholderIconSize = 38.dp,
            fallbackText = preview.coach.nickname,
            fallbackColor = DSColors.sandBeige,
        )
        Text(preview.coach.nickname, color = DSColors.inkGreen, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
            Text("教练身份已认证", color = DSColors.fairway, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AccessTime, contentDescription = null, tint = DSColors.shadowGray, modifier = Modifier.size(15.dp))
            Text(studentInviteExpiryText(preview.expiresAt), color = DSColors.shadowGray, fontSize = 11.sp)
        }
    }
}

internal fun studentInviteIsExpired(expiresAt: String?, now: Instant = Instant.now()): Boolean =
    parseStudentInviteInstant(expiresAt)?.let { !it.isAfter(now) } == true

internal fun studentInviteExpiryText(expiresAt: String?): String {
    val expiry = parseStudentInviteInstant(expiresAt) ?: return "限时邀请"
    val formatter = DateTimeFormatter.ofPattern("M月d日 HH:mm").withZone(ZoneId.systemDefault())
    return "邀请有效至 ${formatter.format(expiry)}"
}

private fun parseStudentInviteInstant(value: String?): Instant? =
    value?.let { runCatching { Instant.parse(it) }.getOrNull() }
