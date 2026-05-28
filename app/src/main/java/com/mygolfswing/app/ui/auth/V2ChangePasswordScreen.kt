package com.mygolfswing.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 修改 / 设置登录密码（对齐 iOS `ProfileSettingsPage` 的密码 sheet）。
 *
 * - profile.hasPassword == true → 「修改登录密码」：当前密码 + 新密码 + 确认
 *   → [V2AuthRepository.updatePassword]
 * - profile.hasPassword == false → 「设置登录密码」：使用绑定手机短信
 *   → [V2AuthRepository.setPasswordWithSms]
 */
@Composable
fun V2ChangePasswordScreen(
    onBack: () -> Unit,
    onChangeSuccess: () -> Unit,
) {
    val profile by V2ProfileStore.profile.collectAsState()
    val hasPassword = profile.hasPassword
    val phone = profile.phone

    val title = if (hasPassword) "修改登录密码" else "设置登录密码"

    var oldPw by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        V2NavTopBarCentered(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .padding(horizontal = DSSpacing.lg)
                .padding(top = 4.dp)
                .fillMaxWidth()
                .background(DSColors.cardWhite, RoundedCornerShape(20.dp))
                .padding(DSSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (hasPassword) {
                Text("请输入当前密码以验证身份", style = DSTypography.caption, color = DSColors.shadowGray)
                OutlinedTextField(
                    value = oldPw,
                    onValueChange = { oldPw = it; error = null },
                    placeholder = { Text("当前密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
            } else {
                Text(
                    "将向绑定手机 ${phone?.maskMiddle() ?: "未绑定"} 发送验证码",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter { c -> c.isDigit() }.take(6); error = null },
                        placeholder = { Text("6 位验证码") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.weight(1f),
                        colors = fieldColors(),
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (phone.isNullOrBlank()) {
                                error = "未绑定手机，无法发送验证码"
                                return@OutlinedButton
                            }
                            scope.launch {
                                loading = true
                                val r = ServiceLocator.authRepo.sendSmsCode(
                                    phone,
                                    purpose = com.mygolfswing.app.data.repo.V2AuthRepository.SmsPurpose.SET_PASSWORD,
                                )
                                loading = false
                                r.onSuccess { countdown = 60 }
                                    .onFailure { error = it.message ?: "发送失败" }
                            }
                        },
                        enabled = !phone.isNullOrBlank() && countdown == 0 && !loading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp),
                        border = BorderStroke(1.dp, DSColors.fairway),
                    ) {
                        Text(
                            if (countdown > 0) "${countdown}s" else "获取验证码",
                            color = if (countdown > 0) DSColors.shadowGray else DSColors.fairway,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = newPw,
                onValueChange = { newPw = it; error = null },
                placeholder = { Text("新密码（至少 8 位，含字母+数字）") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
            )
            OutlinedTextField(
                value = confirmPw,
                onValueChange = { confirmPw = it; error = null },
                placeholder = { Text("确认新密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
            )

            if (!error.isNullOrBlank()) {
                Text(error!!, color = DSColors.flagRed, style = DSTypography.caption)
            }

            Button(
                onClick = {
                    val v = if (hasPassword) {
                        validateChange(oldPw, newPw, confirmPw)
                    } else {
                        validateSet(code, newPw, confirmPw, phone)
                    }
                    if (v != null) {
                        error = v; return@Button
                    }
                    scope.launch {
                        loading = true
                        val r = if (hasPassword) {
                            ServiceLocator.authRepo.updatePassword(oldPw, newPw)
                        } else {
                            ServiceLocator.authRepo.setPasswordWithSms(phone!!, code, newPw)
                        }
                        loading = false
                        r.onSuccess { onChangeSuccess() }
                            .onFailure { error = it.message ?: "操作失败" }
                    }
                },
                enabled = !loading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DSColors.fairway,
                    contentColor = DSColors.warmWhite,
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = DSColors.warmWhite,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(title, style = DSTypography.bodyBold)
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DSColors.fairway,
    cursorColor = DSColors.fairway,
)

private fun validateChange(oldPw: String, newPw: String, confirmPw: String): String? {
    if (oldPw.isBlank()) return "请输入当前密码"
    if (newPw.length < 8) return "新密码至少 8 位"
    if (!newPw.any { it.isLetter() } || !newPw.any { it.isDigit() }) return "密码需包含字母和数字"
    if (newPw != confirmPw) return "两次密码不一致"
    return null
}

private fun validateSet(code: String, newPw: String, confirmPw: String, phone: String?): String? {
    if (phone.isNullOrBlank()) return "未绑定手机，无法设置密码"
    if (code.length != 6) return "请输入 6 位验证码"
    if (newPw.length < 8) return "密码至少 8 位"
    if (!newPw.any { it.isLetter() } || !newPw.any { it.isDigit() }) return "密码需包含字母和数字"
    if (newPw != confirmPw) return "两次密码不一致"
    return null
}

private fun String.maskMiddle(): String {
    if (length <= 7) return this
    return "${substring(0, 3)}****${substring(length - 4)}"
}
