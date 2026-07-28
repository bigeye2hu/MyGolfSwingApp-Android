package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSTypography
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

    AlertDialog(
        modifier = Modifier
            .imePadding()
            .v2NavigationBarPadding(),
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("修改昵称", style = DSTypography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it; error = null },
                    placeholder = { Text("输入新昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = nicknameFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
                error?.let { Text(it, color = DSColors.flagRed, style = DSTypography.caption) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = nickname.isNotBlank() && !loading,
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
                            error = accountSecurityError("更新失败，请稍后重试", e.message)
                        } finally {
                            loading = false
                        }
                    }
                },
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("确认", color = DSColors.fairway)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text("取消", color = DSColors.shadowGray)
            }
        },
    )
}

@Composable
fun V2PhoneBindDialog(
    hasPhone: Boolean,
    currentPhone: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    AlertDialog(
        modifier = Modifier
            .imePadding()
            .v2NavigationBarPadding(),
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(if (hasPhone) "换绑手机号" else "绑定手机号", style = DSTypography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasPhone && !currentPhone.isNullOrBlank()) {
                    Text(
                        "当前手机号 ${currentPhone.maskPhone()}",
                        style = DSTypography.caption,
                        color = DSColors.shadowGray,
                    )
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() }.take(11); error = null },
                    placeholder = { Text("手机号") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = nicknameFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter { c -> c.isDigit() }.take(6); error = null },
                        placeholder = { Text("验证码") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.weight(1f),
                        colors = nicknameFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    OutlinedButton(
                        onClick = {
                            if (phone.length != 11) {
                                error = "请输入正确的手机号"
                                return@OutlinedButton
                            }
                            scope.launch {
                                sending = true
                                try {
                                    ServiceLocator.authRepo.sendSmsCode(phone)
                                        .onSuccess { countdown = 60 }
                                        .onFailure { error = accountSecurityError("验证码发送失败，请稍后重试", it.message) }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Throwable) {
                                    error = accountSecurityError("验证码发送失败，请稍后重试", e.message)
                                } finally {
                                    sending = false
                                }
                            }
                        },
                        enabled = phone.length == 11 && countdown == 0 && !sending,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DSColors.fairway),
                        modifier = Modifier.padding(start = 8.dp).height(56.dp),
                    ) {
                        Text(
                            if (countdown > 0) "${countdown}s" else "获取验证码",
                            color = if (countdown > 0) DSColors.shadowGray else DSColors.fairway,
                            fontSize = 13.sp,
                        )
                    }
                }
                error?.let { Text(it, color = DSColors.flagRed, style = DSTypography.caption) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = phone.length == 11 && code.length >= 4 && !loading,
                onClick = {
                    scope.launch {
                        loading = true
                        try {
                            ServiceLocator.authRepo.bindPhoneNumber(phone, code)
                                .onSuccess {
                                    V2ProfileStore.refresh(force = true)
                                    onSaved()
                                }
                                .onFailure { error = accountSecurityError("绑定失败，请检查验证码后重试", it.message) }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            error = accountSecurityError("绑定失败，请检查验证码后重试", e.message)
                        } finally {
                            loading = false
                        }
                    }
                },
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (hasPhone) "确认换绑" else "确认绑定", color = DSColors.fairway)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text("取消", color = DSColors.shadowGray)
            }
        },
    )
}

@Composable
private fun nicknameFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = DSColors.mistGreen,
    unfocusedContainerColor = DSColors.mistGreen,
    disabledContainerColor = DSColors.mistGreen,
    focusedBorderColor = DSColors.fairway,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    cursorColor = DSColors.fairway,
    focusedTextColor = DSColors.inkGreen,
    unfocusedTextColor = DSColors.inkGreen,
    focusedPlaceholderColor = DSColors.shadowGray.copy(alpha = 0.7f),
    unfocusedPlaceholderColor = DSColors.shadowGray.copy(alpha = 0.7f),
)

private fun String.maskPhone(): String {
    if (length <= 7) return this
    return "${take(3)}****${takeLast(4)}"
}

private fun accountSecurityError(base: String, debugDetail: String?): String {
    val detail = debugDetail.takeIf { ServiceLocator.isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}
