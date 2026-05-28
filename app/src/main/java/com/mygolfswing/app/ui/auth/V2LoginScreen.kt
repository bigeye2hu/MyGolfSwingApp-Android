package com.mygolfswing.app.ui.auth

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SportsGolf
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class LoginMode { SMS, PASSWORD }

/**
 * 登录主页（对齐 iOS `PhoneLoginPage`）。
 *
 * - Hero 区：球场绿渐变 + Logo + 「挥呗」 + 副标语
 * - 主卡片：两个 tab（验证码登录 / 密码登录），切换平滑
 * - SMS 模式：+86 手机号 + 6 位验证码 + 倒计时 60s
 * - 密码模式：用户名/邮箱 + 密码（眼睛切换） + 忘记密码
 * - 底部：用户协议勾选 + 注册入口
 */
@Composable
fun V2LoginScreen(
    onClose: () -> Unit,
    onLoginSuccess: () -> Unit,
    onOpenRegister: () -> Unit,
    onOpenResetPassword: () -> Unit,
) {
    var mode by remember { mutableStateOf(LoginMode.SMS) }
    var phone by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pwVisible by remember { mutableStateOf(false) }
    var agreed by remember { mutableStateOf(true) }
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
            .background(DSColors.warmWhite)
            .verticalScroll(rememberScrollState()),
    ) {
        Hero(onClose = onClose)
        Card(
            mode = mode,
            onModeChange = { mode = it; error = null },
            phone = phone,
            onPhoneChange = { phone = it.filter { c -> c.isDigit() }.take(11); error = null },
            smsCode = smsCode,
            onSmsCodeChange = { smsCode = it.filter { c -> c.isDigit() }.take(6); error = null },
            username = username,
            onUsernameChange = { username = it; error = null },
            password = password,
            onPasswordChange = { password = it; error = null },
            pwVisible = pwVisible,
            onTogglePwVisibility = { pwVisible = !pwVisible },
            countdown = countdown,
            onSendSms = {
                if (phone.length != 11) {
                    error = "请输入 11 位手机号"
                    return@Card
                }
                scope.launch {
                    loading = true
                    val r = ServiceLocator.authRepo.sendSmsCode(phone)
                    loading = false
                    r.onSuccess { countdown = 60 }
                        .onFailure { error = it.message ?: "发送失败" }
                }
            },
            onForgot = onOpenResetPassword,
            error = error,
            agreed = agreed,
            onAgreedChange = { agreed = it },
            loading = loading,
            onSubmit = {
                if (!agreed) {
                    error = "请先同意《用户协议》《隐私政策》"
                    return@Card
                }
                scope.launch {
                    loading = true
                    val r = when (mode) {
                        LoginMode.SMS -> {
                            if (phone.length != 11) {
                                error = "请输入 11 位手机号"; loading = false; return@launch
                            }
                            if (smsCode.length < 4) {
                                error = "请输入验证码"; loading = false; return@launch
                            }
                            ServiceLocator.authRepo.loginByPhone(phone, smsCode)
                        }
                        LoginMode.PASSWORD -> {
                            if (username.isBlank() || password.length < 6) {
                                error = "请输入正确的用户名和密码（至少 6 位）"; loading = false; return@launch
                            }
                            ServiceLocator.authRepo.loginWithUsername(username, password)
                        }
                    }
                    loading = false
                    r.onSuccess { onLoginSuccess() }
                        .onFailure { error = it.message ?: "登录失败" }
                }
            },
            onOpenRegister = onOpenRegister,
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun Hero(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(DSColors.deepGreen, DSColors.fairway),
                ),
            ),
    ) {
        // 右上角关闭按钮
        TextButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .v2StatusBarPadding()
                .padding(top = 8.dp, end = 16.dp),
        ) {
            Text("跳过", color = DSColors.warmWhite, style = DSTypography.body)
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(DSColors.warmWhite.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.SportsGolf,
                    contentDescription = null,
                    tint = DSColors.warmWhite,
                    modifier = Modifier.size(38.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "挥呗",
                color = DSColors.warmWhite,
                style = DSTypography.titleLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Where Every Swing Matters",
                color = DSColors.warmWhite.copy(alpha = 0.85f),
                style = DSTypography.caption,
            )
        }
    }
}

@Composable
private fun Card(
    mode: LoginMode,
    onModeChange: (LoginMode) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    smsCode: String,
    onSmsCodeChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    pwVisible: Boolean,
    onTogglePwVisibility: () -> Unit,
    countdown: Int,
    onSendSms: () -> Unit,
    onForgot: () -> Unit,
    error: String?,
    agreed: Boolean,
    onAgreedChange: (Boolean) -> Unit,
    loading: Boolean,
    onSubmit: () -> Unit,
    onOpenRegister: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = DSSpacing.lg)
            .padding(top = 16.dp)
            .fillMaxWidth()
            .background(DSColors.cardWhite, RoundedCornerShape(24.dp))
            .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ModeTabs(mode = mode, onChange = onModeChange)
        Spacer(Modifier.height(4.dp))
        when (mode) {
            LoginMode.SMS -> SmsFields(
                phone = phone,
                onPhoneChange = onPhoneChange,
                smsCode = smsCode,
                onSmsCodeChange = onSmsCodeChange,
                countdown = countdown,
                onSendSms = onSendSms,
            )
            LoginMode.PASSWORD -> PasswordFields(
                username = username,
                onUsernameChange = onUsernameChange,
                password = password,
                onPasswordChange = onPasswordChange,
                pwVisible = pwVisible,
                onToggle = onTogglePwVisibility,
                onForgot = onForgot,
            )
        }
        if (!error.isNullOrBlank()) {
            Text(error, color = DSColors.flagRed, style = DSTypography.caption)
        }
        Button(
            onClick = onSubmit,
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
                Text(
                    text = if (mode == LoginMode.SMS) "登录 / 注册" else "登录",
                    style = DSTypography.bodyBold,
                )
            }
        }
        TermsRow(agreed = agreed, onChange = onAgreedChange)
        if (mode == LoginMode.PASSWORD) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("没有账号？", style = DSTypography.caption, color = DSColors.shadowGray)
                TextButton(onClick = onOpenRegister) {
                    Text("立即注册", color = DSColors.fairway, style = DSTypography.bodyBold)
                }
            }
        }
    }
}

@Composable
private fun ModeTabs(mode: LoginMode, onChange: (LoginMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.warmWhite, RoundedCornerShape(12.dp))
            .padding(4.dp),
    ) {
        TabPill("验证码登录", mode == LoginMode.SMS, Modifier.weight(1f)) { onChange(LoginMode.SMS) }
        TabPill("密码登录", mode == LoginMode.PASSWORD, Modifier.weight(1f)) { onChange(LoginMode.PASSWORD) }
    }
}

@Composable
private fun TabPill(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(
                if (selected) DSColors.cardWhite else Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) DSColors.inkGreen else DSColors.shadowGray,
            style = if (selected) DSTypography.bodyBold else DSTypography.body,
        )
    }
}

@Composable
private fun SmsFields(
    phone: String,
    onPhoneChange: (String) -> Unit,
    smsCode: String,
    onSmsCodeChange: (String) -> Unit,
    countdown: Int,
    onSendSms: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .height(56.dp)
                .background(DSColors.warmWhite, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, DSColors.borderLine), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("🇨🇳 +86", style = DSTypography.body, color = DSColors.inkGreen)
        }
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            placeholder = { Text("11 位手机号") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = textFieldColors(),
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = smsCode,
            onValueChange = onSmsCodeChange,
            placeholder = { Text("6 位验证码") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            colors = textFieldColors(),
        )
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = onSendSms,
            enabled = phone.length == 11 && countdown == 0,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(56.dp),
            border = BorderStroke(1.dp, DSColors.fairway),
        ) {
            Text(
                if (countdown > 0) "${countdown}s" else "获取验证码",
                color = if (countdown > 0) DSColors.shadowGray else DSColors.fairway,
                style = DSTypography.body,
            )
        }
    }
}

@Composable
private fun PasswordFields(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    pwVisible: Boolean,
    onToggle: () -> Unit,
    onForgot: () -> Unit,
) {
    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        placeholder = { Text("用户名或邮箱") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors(),
    )
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        placeholder = { Text("密码") },
        singleLine = true,
        visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = DSColors.shadowGray) },
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(
                    if (pwVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = null,
                    tint = DSColors.shadowGray,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = textFieldColors(),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onForgot) {
            Text("忘记密码?", color = DSColors.fairway, style = DSTypography.caption)
        }
    }
}

@Composable
private fun TermsRow(agreed: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    if (agreed) DSColors.fairway else DSColors.warmWhite,
                    CircleShape,
                )
                .border(
                    BorderStroke(1.dp, if (agreed) DSColors.fairway else DSColors.shadowGray),
                    CircleShape,
                )
                .clickable { onChange(!agreed) },
            contentAlignment = Alignment.Center,
        ) {
            if (agreed) Text("✓", color = DSColors.warmWhite, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "我已阅读并同意 《用户协议》《隐私政策》",
            style = DSTypography.caption,
            color = DSColors.shadowGray,
        )
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DSColors.fairway,
    cursorColor = DSColors.fairway,
)

