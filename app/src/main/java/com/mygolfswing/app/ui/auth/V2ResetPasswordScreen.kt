package com.mygolfswing.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.data.ServiceLocator
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
    var pwVisible by remember { mutableStateOf(false) }
    var confirmPwVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var showSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1500)
            onResetSuccess()
        }
    }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Hero(onBack = onBack)
            AuthCard(
                modifier = Modifier
                    .padding(horizontal = DSSpacing.lg)
                    .offset(y = (-32).dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AuthFieldLabel("手机号")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .height(56.dp)
                                .background(DSColors.cardWhite, RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, AuthPalette.goldAccent), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🇨🇳 +86", style = DSTypography.bodyBold, color = DSColors.fairway)
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.filter { c -> c.isDigit() }.take(11); error = null },
                            placeholder = { Text("请输入手机号") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            colors = fieldColors(),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AuthFieldLabel("验证码")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.filter { c -> c.isDigit() }.take(6); error = null },
                            placeholder = { Text("请输入 6 位验证码") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.weight(1f),
                            colors = fieldColors(),
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (phone.length != 11) {
                                    error = "请输入正确的手机号"; return@Button
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (countdown > 0) AuthPalette.placeholder else DSColors.fairway,
                                contentColor = DSColors.warmWhite,
                                disabledContainerColor = AuthPalette.placeholder,
                                disabledContentColor = DSColors.warmWhite,
                            ),
                            modifier = Modifier.height(56.dp).width(110.dp),
                        ) {
                            Text(if (countdown > 0) "${countdown}s" else "获取验证码", style = DSTypography.caption)
                        }
                    }
                }

                PasswordField("新密码", newPw, { newPw = it; error = null }, pwVisible, { pwVisible = !pwVisible }, "8 位以上，包含字母和数字")
                PasswordField("确认新密码", confirmPw, { confirmPw = it; error = null }, confirmPwVisible, { confirmPwVisible = !confirmPwVisible }, "再次输入新密码")

                if (!error.isNullOrBlank()) {
                    Text(error!!, color = DSColors.flagRed, style = DSTypography.caption)
                }

                AuthPrimaryButton(
                    text = "重置密码并登录",
                    loading = loading,
                    onClick = {
                        val v = validate(phone, code, newPw, confirmPw)
                        if (v != null) {
                            error = v; return@AuthPrimaryButton
                        }
                        scope.launch {
                            loading = true
                            val r = ServiceLocator.authRepo.resetPasswordWithSms(phone, code, newPw)
                            if (r.isSuccess) {
                                // 重置后自动用新密码登录
                                val login = ServiceLocator.authRepo.loginWithUsername(phone, newPw)
                                login.onSuccess {
                                    loading = false
                                    showSuccess = true
                                }
                                    .onFailure {
                                        loading = false
                                        error = "重置成功，请回到登录页用新密码登录"
                                    }
                            } else {
                                loading = false
                                error = r.exceptionOrNull()?.message ?: "重置失败"
                            }
                        }
                    },
                )
            }
            TipsCard(
                modifier = Modifier
                    .padding(horizontal = DSSpacing.lg)
                    .offset(y = (-16).dp),
            )
        }
        if (showSuccess) {
            ResetSuccessOverlay()
        }
    }
}

@Composable
private fun Hero(onBack: () -> Unit) {
    AuthHeroBackground(height = 260.dp, bottomFadeHeight = 80.dp) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 54.dp, start = 20.dp)
                .background(DSColors.warmWhite.copy(alpha = 0.18f), CircleShape)
                .border(1.dp, DSColors.warmWhite.copy(alpha = 0.3f), CircleShape),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = DSColors.warmWhite)
        }
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(authHeroBrush, CircleShape)
                    .border(1.5.dp, AuthPalette.goldAccent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.LockReset, contentDescription = null, tint = DSColors.warmWhite, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text("重置密码", color = DSColors.warmWhite, style = DSTypography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text("通过绑定的手机号 + 短信验证码", color = DSColors.warmWhite.copy(alpha = 0.82f), style = DSTypography.caption)
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    placeholder: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AuthFieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = AuthPalette.placeholder) },
            trailingIcon = {
                IconButton(onClick = onToggleVisible) {
                    Icon(
                        if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = AuthPalette.placeholder,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
        )
    }
}

@Composable
private fun TipsCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AuthPalette.goldAccent.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, AuthPalette.goldAccent.copy(alpha = 0.18f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("温馨提示", color = AuthPalette.labelText, style = DSTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        Text("• 如果账号未绑定手机号，请联系客服重置密码", color = AuthPalette.placeholder, style = DSTypography.caption)
        Text("• 客服邮箱：372931693@qq.com", color = AuthPalette.placeholder, style = DSTypography.caption)
        Text("• 客服电话：二胡 18851310088", color = AuthPalette.placeholder, style = DSTypography.caption)
    }
}

@Composable
private fun ResetSuccessOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp, vertical = 28.dp)
                .fillMaxWidth(0.82f)
                .background(DSColors.cardWhite, RoundedCornerShape(22.dp))
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(DSColors.fairway.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = DSColors.fairway,
                    modifier = Modifier.size(48.dp),
                )
            }
            Text("密码重置成功！", color = AuthPalette.labelText, style = DSTypography.bodyBold)
            Text("已自动登录，正在进入主页…", color = AuthPalette.placeholder, style = DSTypography.caption)
            CircularProgressIndicator(color = DSColors.fairway, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DSColors.fairway,
    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
    focusedContainerColor = AuthPalette.inputBg,
    unfocusedContainerColor = AuthPalette.inputBg,
    cursorColor = DSColors.fairway,
)

private fun validate(phone: String, code: String, newPw: String, confirmPw: String): String? {
    if (phone.length != 11) return "请输入正确的手机号"
    if (code.length != 6) return "请输入 6 位验证码"
    if (newPw.length < 8) return "新密码至少 8 位"
    if (!newPw.any { it.isLetter() } || !newPw.any { it.isDigit() }) return "密码需同时包含字母和数字"
    if (newPw != confirmPw) return "两次输入的密码不一致"
    return null
}
