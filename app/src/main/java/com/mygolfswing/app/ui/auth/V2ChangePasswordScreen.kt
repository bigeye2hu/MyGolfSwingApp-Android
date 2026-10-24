package com.mygolfswing.app.ui.auth

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.R
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 兼容旧导航入口；当前账号设置页直接在原页面上显示 [V2ChangePasswordDialog]，
 * 与 iOS `ProfileSettingsPage.passwordSetupAlert` 的模态层级一致。
 */
@Composable
fun V2ChangePasswordScreen(
    onBack: () -> Unit,
    onChangeSuccess: () -> Unit,
) {
    V2RoomPage(Modifier.fillMaxSize()) { }
    V2ChangePasswordDialog(onDismiss = onBack, onChangeSuccess = onChangeSuccess)
}

@Composable
fun V2ChangePasswordDialog(
    onDismiss: () -> Unit,
    onChangeSuccess: () -> Unit,
) {
    val profile by V2ProfileStore.profile.collectAsState()
    val hasPassword = profile.hasPassword
    val phone = profile.phone

    var oldPassword by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isSendingCode by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var succeeded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1_000)
            countdown -= 1
        }
    }
    LaunchedEffect(succeeded) {
        if (succeeded) {
            delay(1_500)
            onChangeSuccess()
        }
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f))
                .clickable(enabled = !isSaving, onClick = onDismiss)
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            val consumeCardTap = { Unit }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .padding(horizontal = 24.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = DSColors.deepGreen.copy(alpha = 0.20f),
                        spotColor = DSColors.deepGreen.copy(alpha = 0.20f),
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, DSColors.fairway.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                    .clickable(onClick = consumeCardTap),
            ) {
                PasswordDialogHeader(hasPassword = hasPassword)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DSColors.cardWhite)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    if (hasPassword) {
                        PasswordSetupField("当前密码") {
                            PasswordInput(
                                value = oldPassword,
                                onValueChange = { oldPassword = it; error = null },
                                placeholder = "请输入当前密码",
                            )
                        }
                    } else {
                        PasswordSetupField("验证手机号 ${phone.orEmpty().maskMiddle()}") {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PasswordInput(
                                    value = smsCode,
                                    onValueChange = {
                                        smsCode = it.filter(Char::isDigit).take(6)
                                        error = null
                                    },
                                    placeholder = "请输入验证码",
                                    keyboardType = KeyboardType.NumberPassword,
                                    secure = false,
                                    modifier = Modifier.weight(1f),
                                )
                                PasswordAction(
                                    label = if (countdown > 0) "${countdown}s" else "获取验证码",
                                    foreground = if (countdown > 0) DSColors.shadowGray else DSColors.fairway,
                                    background = DSColors.mistGreen,
                                    enabled = countdown == 0 && !isSendingCode && !isSaving,
                                    onClick = {
                                        if (phone.isNullOrBlank()) {
                                            error = "未绑定手机号"
                                        } else {
                                            scope.launch {
                                                isSendingCode = true
                                                error = null
                                                try {
                                                    ServiceLocator.authRepo.sendSmsCode(phone)
                                                        .onSuccess { countdown = 60 }
                                                        .onFailure {
                                                            error = authUserError("验证码发送失败，请稍后重试", it.message)
                                                        }
                                                } catch (e: CancellationException) {
                                                    throw e
                                                } catch (e: Throwable) {
                                                    error = authThrowableError("验证码发送失败，请稍后重试", e)
                                                } finally {
                                                    isSendingCode = false
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.width(86.dp),
                                )
                            }
                        }
                    }

                    PasswordSetupField("新密码") {
                        PasswordInput(
                            value = newPassword,
                            onValueChange = { newPassword = it; error = null },
                            placeholder = "8位以上，包含字母和数字",
                        )
                    }
                    PasswordSetupField("确认新密码") {
                        PasswordInput(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; error = null },
                            placeholder = "再次输入新密码",
                        )
                    }

                    error?.let { message ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Error, contentDescription = null, tint = DSColors.flagRed, modifier = Modifier.size(14.dp))
                            Text(message, color = DSColors.flagRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (succeeded) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = DSColors.fairway, modifier = Modifier.size(15.dp))
                            Text("密码设置成功", color = DSColors.fairway, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PasswordAction(
                            label = "取消",
                            foreground = DSColors.inkGreen,
                            background = DSColors.mistGreen,
                            enabled = !isSaving,
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        )
                        PasswordAction(
                            label = "确认",
                            foreground = Color.White,
                            background = DSColors.fairway,
                            enabled = !isSaving && !succeeded,
                            loading = isSaving,
                            onClick = {
                                val validationError = if (hasPassword) {
                                    validateChange(oldPassword, newPassword, confirmPassword)
                                } else {
                                    validateSet(smsCode, newPassword, confirmPassword, phone)
                                }
                                if (validationError != null) {
                                    error = validationError
                                } else {
                                    scope.launch {
                                        isSaving = true
                                        error = null
                                        try {
                                            val result = if (hasPassword) {
                                                ServiceLocator.authRepo.updatePassword(oldPassword, newPassword)
                                            } else {
                                                ServiceLocator.authRepo.setPasswordWithSms(phone!!, smsCode, newPassword)
                                            }
                                            result.onSuccess {
                                                V2ProfileStore.refresh(force = true)
                                                succeeded = true
                                            }.onFailure {
                                                error = authUserError("操作失败，请检查输入后重试", it.message)
                                            }
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Throwable) {
                                            error = authThrowableError("操作失败，请检查输入后重试", e)
                                        } finally {
                                            isSaving = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordDialogHeader(hasPassword: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().height(116.dp)) {
        Image(
            painter = painterResource(R.drawable.golf_course_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
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
            Icon(Icons.Filled.Key, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            Text(
                if (hasPassword) "修改登录密码" else "设置登录密码",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                if (hasPassword) "验证当前密码后设置新密码" else "验证绑定手机号后设置登录密码",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PasswordSetupField(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun PasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Password,
    secure: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 15.sp) },
        singleLine = true,
        visualTransformation = if (secure) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth().height(48.dp),
        textStyle = TextStyle(fontSize = 15.sp, color = DSColors.inkGreen),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF9F6EF),
            unfocusedContainerColor = Color(0xFFF9F6EF),
            focusedBorderColor = DSColors.fairway.copy(alpha = 0.10f),
            unfocusedBorderColor = DSColors.fairway.copy(alpha = 0.10f),
            cursorColor = DSColors.fairway,
            focusedTextColor = DSColors.inkGreen,
            unfocusedTextColor = DSColors.inkGreen,
            focusedPlaceholderColor = DSColors.shadowGray.copy(alpha = 0.7f),
            unfocusedPlaceholderColor = DSColors.shadowGray.copy(alpha = 0.7f),
        ),
    )
}

@Composable
private fun PasswordAction(
    label: String,
    foreground: Color,
    background: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background.copy(alpha = if (enabled || loading) 1f else 0.55f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(label, color = foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

internal fun validateChange(oldPassword: String, newPassword: String, confirmPassword: String): String? {
    if (newPassword.length < 8) return "新密码至少 8 位"
    if (!newPassword.any(Char::isLetter) || !newPassword.any(Char::isDigit)) return "密码需包含字母和数字"
    if (newPassword != confirmPassword) return "两次输入的新密码不一致"
    if (oldPassword.isBlank()) return "请输入当前密码"
    return null
}

internal fun validateSet(smsCode: String, newPassword: String, confirmPassword: String, phone: String?): String? {
    if (newPassword.length < 8) return "密码至少 8 位"
    if (!newPassword.any(Char::isLetter) || !newPassword.any(Char::isDigit)) return "密码需包含字母和数字"
    if (newPassword != confirmPassword) return "两次输入的新密码不一致"
    if (smsCode.length != 6) return "请输入6位验证码"
    if (phone.isNullOrBlank()) return "未绑定手机号，无法设置密码"
    return null
}

private fun String.maskMiddle(): String {
    if (length <= 7) return this
    return "${take(3)}****${takeLast(4)}"
}
