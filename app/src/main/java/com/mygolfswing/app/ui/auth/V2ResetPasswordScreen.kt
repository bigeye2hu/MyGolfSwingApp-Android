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
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 重置密码（对齐 iOS `ForgotPasswordPage`）。
 *
 * 流程：手机号 → 验证码（60s 倒计时）→ 新密码 → 确认密码 → 「重置密码并登录」
 * 成功后调用 [V2AuthRepository.loginWithUsername] 自动登录，关闭页面。
 */
@Composable
fun V2ResetPasswordScreen(
    onBack: () -> Unit,
    onResetSuccess: () -> Unit,
) {
    var phone by remember { mutableStateOf("") }
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
        V2NavTopBarCentered(title = "重置密码", onBack = onBack)
        Column(
            modifier = Modifier
                .padding(horizontal = DSSpacing.lg)
                .padding(top = 4.dp)
                .fillMaxWidth()
                .background(DSColors.cardWhite, RoundedCornerShape(20.dp))
                .padding(DSSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "我们将通过短信验证你的身份，",
                style = DSTypography.body,
                color = DSColors.shadowGray,
            )
            Text(
                "完成后立即用新密码登录。",
                style = DSTypography.body,
                color = DSColors.shadowGray,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .background(DSColors.warmWhite, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🇨🇳 +86", style = DSTypography.body, color = DSColors.inkGreen)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() }.take(11); error = null },
                    placeholder = { Text("11 位手机号") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f),
                    colors = fieldColors(),
                )
            }

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
                        if (phone.length != 11) {
                            error = "请输入 11 位手机号"; return@OutlinedButton
                        }
                        scope.launch {
                            loading = true
                            val r = ServiceLocator.authRepo.sendSmsCode(
                                phone,
                                purpose = com.mygolfswing.app.data.repo.V2AuthRepository.SmsPurpose.RESET_PASSWORD,
                            )
                            loading = false
                            r.onSuccess { countdown = 60 }
                                .onFailure { error = it.message ?: "发送失败" }
                        }
                    },
                    enabled = phone.length == 11 && countdown == 0 && !loading,
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

            OutlinedTextField(
                value = newPw,
                onValueChange = { newPw = it; error = null },
                placeholder = { Text("新密码（至少 8 位）") },
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
                    val v = validate(phone, code, newPw, confirmPw)
                    if (v != null) {
                        error = v; return@Button
                    }
                    scope.launch {
                        loading = true
                        val r = ServiceLocator.authRepo.resetPasswordWithSms(phone, code, newPw)
                        if (r.isSuccess) {
                            // 重置后自动用新密码登录
                            val login = ServiceLocator.authRepo.loginWithUsername(phone, newPw)
                            loading = false
                            login.onSuccess { onResetSuccess() }
                                .onFailure { error = "重置成功，请回到登录页用新密码登录" }
                        } else {
                            loading = false
                            error = r.exceptionOrNull()?.message ?: "重置失败"
                        }
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
                    Text("重置密码并登录", style = DSTypography.bodyBold)
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

private fun validate(phone: String, code: String, newPw: String, confirmPw: String): String? {
    if (phone.length != 11) return "请输入 11 位手机号"
    if (code.length != 6) return "请输入 6 位验证码"
    if (newPw.length < 8) return "新密码至少 8 位"
    if (!newPw.any { it.isLetter() } || !newPw.any { it.isDigit() }) return "密码需同时包含字母和数字"
    if (newPw != confirmPw) return "两次密码不一致"
    return null
}
