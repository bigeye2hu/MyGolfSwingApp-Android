package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.R
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.repo.V2PhoneAccountConflict
import com.mygolfswing.app.data.repo.V2PhoneBindingStatus
import com.mygolfswing.app.data.repo.isValidAuthPhoneNumber
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.auth.authUserError
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun V2NicknameEditorDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var nickname by remember { mutableStateOf(initial) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { if (!loading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.60f))
                .clickable(enabled = !loading, onClick = onDismiss)
                .padding(horizontal = 24.dp)
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            val consumeCardTap = { Unit }
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
                    .shadow(20.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.30f))
                    .clip(RoundedCornerShape(20.dp))
                    .background(DSColors.cardWhite)
                    .border(1.dp, DSColors.fairway.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
                    .clickable(onClick = consumeCardTap)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("修改昵称", color = DSColors.inkGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it; error = null },
                    placeholder = { Text("输入新昵称", fontSize = 16.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    textStyle = TextStyle(fontSize = 16.sp, color = DSColors.inkGreen),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Gray.copy(alpha = 0.20f),
                        unfocusedContainerColor = Color.Gray.copy(alpha = 0.20f),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = DSColors.fairway,
                    ),
                )
                error?.let {
                    Text(it, color = DSColors.flagRed, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompactAction(
                        label = "取消",
                        foreground = DSColors.inkGreen,
                        background = Color.Gray.copy(alpha = 0.30f),
                        enabled = !loading,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 44,
                    )
                    CompactAction(
                        label = if (loading) "更新中..." else "确认",
                        foreground = Color.White,
                        background = if (nickname.isEmpty()) DSColors.shadowGray.copy(alpha = 0.40f) else DSColors.fairway,
                        enabled = nickname.isNotEmpty() && !loading,
                        onClick = {
                            scope.launch {
                                loading = true
                                try {
                                    ServiceLocator.authRepo.updateNickname(nickname.trim())
                                        .onSuccess {
                                            V2ProfileStore.refresh(force = true)
                                            onSaved()
                                        }
                                        .onFailure { error = accountSecurityError("更新失败，请稍后重试", it.message) }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Throwable) {
                                    error = accountSecurityThrowableError("更新失败，请稍后重试", e)
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        height = 44,
                    )
                }
            }
        }
    }
}

@Composable
fun V2PhoneBindDialog(
    hasPhone: Boolean,
    currentPhone: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    initialConflict: V2PhoneBindingStatus.Conflict? = null,
) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var merging by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var conflict by remember(initialConflict) { mutableStateOf(initialConflict) }
    val scope = rememberCoroutineScope()
    val busy = submitting || merging
    val title = if (hasPhone) "换绑手机号" else "绑定手机号"
    val subtitle = if (hasPhone && !currentPhone.isNullOrBlank()) {
        "当前 ${currentPhone.maskPhone()}，验证新手机号后替换"
    } else {
        "验证手机号后完成账号绑定"
    }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1_000)
            countdown -= 1
        }
    }

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f))
                .clickable(enabled = !busy, onClick = onDismiss)
                .padding(horizontal = 24.dp)
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            val consumeCardTap = { Unit }
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .shadow(
                        24.dp,
                        RoundedCornerShape(24.dp),
                        ambientColor = DSColors.deepGreen.copy(alpha = 0.20f),
                        spotColor = DSColors.deepGreen.copy(alpha = 0.20f),
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, DSColors.fairway.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                    .clickable(onClick = consumeCardTap),
            ) {
                BrandedPhoneHeader(title = title, subtitle = subtitle)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DSColors.cardWhite)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    val currentConflict = conflict
                    if (currentConflict == null) {
                        PhoneBindingForm(
                            phone = phone,
                            onPhoneChange = { phone = it.filter(Char::isDigit).take(11); error = null },
                            code = code,
                            onCodeChange = { code = it.filter(Char::isDigit).take(6); error = null },
                            countdown = countdown,
                            sending = sending,
                            submitting = submitting,
                            onSendCode = {
                                if (!isValidAuthPhoneNumber(phone)) {
                                    error = "请输入正确的手机号"
                                } else {
                                    scope.launch {
                                        sending = true
                                        error = null
                                        try {
                                            ServiceLocator.authRepo.sendSmsCode(phone)
                                                .onSuccess { countdown = 60 }
                                                .onFailure { error = accountSecurityError("验证码发送失败，请稍后重试", it.message) }
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Throwable) {
                                            error = accountSecurityThrowableError("验证码发送失败，请稍后重试", e)
                                        } finally {
                                            sending = false
                                        }
                                    }
                                }
                            },
                            onContinue = {
                                scope.launch {
                                    submitting = true
                                    error = null
                                    try {
                                        ServiceLocator.authRepo.checkPhoneBindingStatus(phone, code)
                                            .onSuccess { status ->
                                                when (status) {
                                                    is V2PhoneBindingStatus.Available -> {
                                                        ServiceLocator.authRepo.bindPhoneNumberWithToken(
                                                            status.phoneNumber,
                                                            status.bindingToken,
                                                        ).onSuccess {
                                                            V2ProfileStore.refresh(force = true)
                                                            onSaved()
                                                        }.onFailure {
                                                            error = accountSecurityError("手机号绑定失败，请稍后重试", it.message)
                                                        }
                                                    }
                                                    is V2PhoneBindingStatus.AlreadyBound -> {
                                                        V2ProfileStore.refresh(force = true)
                                                        onSaved()
                                                    }
                                                    is V2PhoneBindingStatus.Conflict -> conflict = status
                                                }
                                            }
                                            .onFailure { error = accountSecurityError("手机号校验失败，请稍后重试", it.message) }
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Throwable) {
                                        error = accountSecurityThrowableError("手机号校验失败，请稍后重试", e)
                                    } finally {
                                        submitting = false
                                    }
                                }
                            },
                        )
                    } else {
                        PhoneConflictPanel(
                            conflict = currentConflict.user,
                            fallbackPhone = currentConflict.phoneNumber,
                            merging = merging,
                            onConfirm = {
                                scope.launch {
                                    merging = true
                                    error = null
                                    try {
                                        ServiceLocator.authRepo.confirmMergePhoneAccount(
                                            phone = currentConflict.phoneNumber,
                                            conflictUserId = currentConflict.user.userId,
                                            mergeToken = currentConflict.mergeToken,
                                        ).onSuccess {
                                            V2ProfileStore.refresh(force = true)
                                            onSaved()
                                        }.onFailure {
                                            error = accountSecurityError("账号合并失败，请稍后重试", it.message)
                                        }
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Throwable) {
                                        error = accountSecurityThrowableError("账号合并失败，请稍后重试", e)
                                    } finally {
                                        merging = false
                                    }
                                }
                            },
                            onChooseAnother = {
                                conflict = null
                                error = null
                            },
                        )
                    }

                    error?.let { BindingError(it) }

                    if (conflict == null) {
                        CompactAction(
                            label = "取消",
                            foreground = DSColors.inkGreen,
                            background = DSColors.mistGreen,
                            enabled = !busy,
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            height = 48,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandedPhoneHeader(title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxWidth().height(116.dp)) {
        Image(
            painter = painterResource(R.drawable.golf_course_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            DSColors.deepGreen.copy(alpha = 0.76f),
                            DSColors.fairway.copy(alpha = 0.48f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Filled.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PhoneBindingForm(
    phone: String,
    onPhoneChange: (String) -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    countdown: Int,
    sending: Boolean,
    submitting: Boolean,
    onSendCode: () -> Unit,
    onContinue: () -> Unit,
) {
    val canSend = phone.length == 11 && countdown == 0 && !sending
    val canSubmit = phone.length == 11 && code.length >= 4 && !submitting
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF9F6EF))
                .border(1.dp, DSColors.borderLine, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("+86", color = DSColors.fairway, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            PlainField(
                value = phone,
                onValueChange = onPhoneChange,
                placeholder = "输入 11 位手机号",
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF9F6EF))
                .border(1.dp, DSColors.borderLine, RoundedCornerShape(14.dp))
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlainField(
                value = code,
                onValueChange = onCodeChange,
                placeholder = "输入验证码",
                keyboardType = KeyboardType.NumberPassword,
                modifier = Modifier.weight(1f),
            )
            CompactAction(
                label = if (countdown > 0) "${countdown}s" else "获取验证码",
                foreground = Color.White,
                background = if (canSend) DSColors.fairway else DSColors.shadowGray.copy(alpha = 0.35f),
                enabled = canSend,
                loading = sending,
                onClick = onSendCode,
                modifier = Modifier.width(98.dp),
                height = 40,
                fontSize = 13,
            )
        }
        CompactAction(
            label = "继续",
            foreground = if (canSubmit || submitting) Color.White else DSColors.fairway,
            background = if (canSubmit || submitting) DSColors.fairway else DSColors.mistGreen,
            enabled = canSubmit,
            loading = submitting,
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            height = 50,
            fontSize = 16,
        )
    }
}

@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = VisualTransformation.None,
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, color = DSColors.shadowGray.copy(alpha = 0.70f), fontSize = 16.sp)
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun PhoneConflictPanel(
    conflict: V2PhoneAccountConflict,
    fallbackPhone: String,
    merging: Boolean,
    onConfirm: () -> Unit,
    onChooseAnother: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("这个手机号已经有一个账号", color = DSColors.inkGreen, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DSColors.mistGreen)
                .border(1.dp, DSColors.fairway.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConflictSummaryRow("手机号", conflict.maskedPhone ?: fallbackPhone.maskPhone())
            ConflictSummaryRow("昵称", conflict.nickname?.takeIf(String::isNotBlank) ?: "未完善")
            ConflictSummaryRow("账号密码", if (conflict.hasPassword) "已设置" else "未设置")
            ConflictSummaryRow("业务数据", "${conflict.businessDataCount} 条")
        }
        Text(
            "确认后，会把该手机号账号的数据并入当前已登录账号；当前账号继续保留原用户名和密码。失败时不会改变两个账号的绑定状态。",
            color = DSColors.shadowGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.sp,
        )
        CompactAction(
            label = "确认合并并绑定",
            foreground = Color.White,
            background = Color(0xFF2D6A4F),
            enabled = !merging,
            loading = merging,
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            height = 50,
            fontSize = 16,
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(44.dp).clickable(enabled = !merging, onClick = onChooseAnother),
            contentAlignment = Alignment.Center,
        ) {
            Text("换一个手机号", color = DSColors.fairway, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ConflictSummaryRow(title: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = DSColors.shadowGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(
            value,
            color = DSColors.inkGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun BindingError(message: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Error, contentDescription = null, tint = DSColors.flagRed, modifier = Modifier.size(14.dp))
        Text(message, color = DSColors.flagRed, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CompactAction(
    label: String,
    foreground: Color,
    background: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Int,
    fontSize: Int = 15,
    loading: Boolean = false,
) {
    Box(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(if (height == 40) 12.dp else if (height == 44) 12.dp else 14.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Text(label, color = foreground, fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun String.maskPhone(): String {
    if (length <= 7) return this
    return "${take(3)}****${takeLast(4)}"
}

internal fun accountSecurityError(base: String, debugDetail: String?): String {
    return accountSecurityError(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun accountSecurityError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    return authUserError(base, debugDetail, isDebuggable)
}

internal fun accountSecurityThrowableError(base: String, error: Throwable): String {
    if (error is CancellationException) throw error
    return accountSecurityError(base, error.message)
}
