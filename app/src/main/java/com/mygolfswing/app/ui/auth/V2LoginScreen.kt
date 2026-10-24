package com.mygolfswing.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.repo.isValidAuthPhoneNumber
import com.mygolfswing.app.data.repo.isValidAuthSmsCode
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.CancellationException
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
    onClose: (() -> Unit)? = null,
    onLoginSuccess: () -> Unit,
    onGuest: () -> Unit,
    onOpenRegister: () -> Unit,
    onOpenResetPassword: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    var mode by remember { mutableStateOf(LoginMode.SMS) }
    var phone by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pwVisible by remember { mutableStateOf(false) }
    var agreed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sendingCode by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
        Hero(onClose = onClose)
        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .offset(y = (-40).dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
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
                sendingCode = sendingCode,
                onSendSms = {
                    if (!isValidAuthPhoneNumber(phone)) {
                        error = "请输入正确的手机号"
                        return@Card
                    }
                    scope.launch {
                        sendingCode = true
                        try {
                            val r = ServiceLocator.authRepo.sendSmsCode(phone)
                            r.onSuccess { countdown = 60 }
                                .onFailure { error = authUserError("验证码发送失败，请稍后重试", it.message) }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            error = authThrowableError("验证码发送失败，请稍后重试", e)
                        } finally {
                            sendingCode = false
                        }
                    }
                },
                onForgot = onOpenResetPassword,
                error = error,
                loading = submitting,
                onSubmit = {
                    val validationError = when (mode) {
                        LoginMode.SMS -> when {
                            !isValidAuthPhoneNumber(phone) -> "请输入正确的手机号"
                            !isValidAuthSmsCode(smsCode) -> "请输入 6 位验证码"
                            else -> null
                        }
                        LoginMode.PASSWORD -> when {
                            username.isBlank() -> "请输入手机号、用户名或邮箱"
                            password.isBlank() -> "请输入密码"
                            password.length < 6 -> "密码至少需要6位"
                            else -> null
                        }
                    }
                    if (validationError != null) {
                        error = validationError
                        return@Card
                    }
                    if (!agreed) {
                        error = "请勾选下方用户协议"
                        return@Card
                    }
                    scope.launch {
                        submitting = true
                        try {
                            val r = when (mode) {
                                LoginMode.SMS -> ServiceLocator.authRepo.loginByPhone(phone, smsCode)
                                LoginMode.PASSWORD -> ServiceLocator.authRepo.loginWithUsername(username, password)
                            }
                            r.onSuccess { onLoginSuccess() }
                                .onFailure { error = authUserError("登录失败，请检查账号信息后重试", it.message) }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            error = authThrowableError("登录失败，请检查账号信息后重试", e)
                        } finally {
                            submitting = false
                        }
                    }
                },
                onGuest = onGuest,
                onOpenRegister = onOpenRegister,
            )
            LoginAgreementRow(
                agreed = agreed,
                onAgreedChange = { agreed = it },
                onOpenTerms = onOpenTerms,
                onOpenPrivacy = onOpenPrivacy,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
            Spacer(Modifier.height(36.dp).navigationBarsPadding())
        }
    }
}

@Composable
private fun Hero(onClose: (() -> Unit)?) {
    AuthHeroBackground(height = 360.dp, bottomFadeHeight = 100.dp) {
        if (onClose != null) {
            TextButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .v2StatusBarPadding()
                    .padding(top = 8.dp, end = 16.dp),
            ) {
                Text("关闭", color = DSColors.warmWhite, style = DSTypography.body)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 112.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AuthLogoBadge(size = 80.dp, imageSize = 78.dp)
            Text(
                "挥呗",
                color = DSColors.warmWhite,
                style = DSTypography.titleLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold),
            )
            Text(
                "Where Every Swing Matters",
                color = DSColors.warmWhite.copy(alpha = 0.82f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
            )
            HeroValueRibbon(Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun HeroValueRibbon(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                RoundedCornerShape(50),
            )
            .height(30.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        HeroValueChip(Icons.Filled.VideoLibrary, "优质视频")
        HeroValueChip(Icons.Filled.Description, "练球心得")
        HeroValueChip(Icons.Filled.AutoAwesome, "AI 助手")
    }
}

@Composable
private fun HeroValueChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(9.dp))
        Text(
            text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun Card(
    modifier: Modifier = Modifier,
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
    sendingCode: Boolean,
    onSendSms: () -> Unit,
    onForgot: () -> Unit,
    error: String?,
    loading: Boolean,
    onSubmit: () -> Unit,
    onGuest: () -> Unit,
    onOpenRegister: () -> Unit,
) {
    AuthCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ModeTabs(mode = mode, onChange = onModeChange)
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AuthPalette.divider),
            )
            Box(Modifier.padding(top = 20.dp)) {
                when (mode) {
                    LoginMode.SMS -> SmsFields(
                        phone = phone,
                        onPhoneChange = onPhoneChange,
                        smsCode = smsCode,
                        onSmsCodeChange = onSmsCodeChange,
                        countdown = countdown,
                        sendingCode = sendingCode,
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
            }
            if (!error.isNullOrBlank()) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Error, null, tint = DSColors.flagRed, modifier = Modifier.size(12.dp))
                    Text(error, color = DSColors.flagRed, fontSize = 12.sp)
                }
            }
            AuthPrimaryButton(
                text = "登录 / 注册",
                loading = loading,
                onClick = onSubmit,
                modifier = Modifier
                    .padding(top = if (error.isNullOrBlank()) 16.dp else 12.dp)
                    .testTag("login-submit"),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (mode == LoginMode.PASSWORD) "还没有账号？" else "不想登录？",
                    color = AuthPalette.placeholder,
                    fontSize = 14.sp,
                )
                Text(
                    text = if (mode == LoginMode.PASSWORD) "现在注册" else "先看看",
                    color = AuthPalette.heroMid2,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable(onClick = if (mode == LoginMode.PASSWORD) onOpenRegister else onGuest)
                        .testTag(if (mode == LoginMode.PASSWORD) "login-register" else "login-guest"),
                )
            }
        }
    }
}

@Composable
private fun ModeTabs(mode: LoginMode, onChange: (LoginMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
    ) {
        TabPill("验证码登录", mode == LoginMode.SMS, Modifier.weight(1f)) { onChange(LoginMode.SMS) }
        TabPill("密码登录", mode == LoginMode.PASSWORD, Modifier.weight(1f)) { onChange(LoginMode.PASSWORD) }
    }
}

@Composable
private fun TabPill(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.height(44.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                color = if (selected) DSColors.fairway else AuthPalette.placeholder,
                style = if (selected) DSTypography.bodyBold else DSTypography.body,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(2.dp)
                    .background(if (selected) DSColors.fairway else Color.Transparent, RoundedCornerShape(1.dp)),
            )
        }
    }
}

@Composable
private fun SmsFields(
    phone: String,
    onPhoneChange: (String) -> Unit,
    smsCode: String,
    onSmsCodeChange: (String) -> Unit,
    countdown: Int,
    sendingCode: Boolean,
    onSendSms: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AuthFieldLabel("手机号")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .height(50.dp)
                        .background(DSColors.cardWhite, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, AuthPalette.goldAccent), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text("🇨🇳", fontSize = 16.sp)
                        Text(
                            "+86",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DSColors.fairway,
                        )
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = DSColors.fairway,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                LoginBasicField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    placeholder = "请输入手机号",
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 0.dp),
                    keyboardType = KeyboardType.Phone,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AuthFieldLabel("验证码")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LoginBasicField(
                    value = smsCode,
                    onValueChange = onSmsCodeChange,
                    placeholder = "请输入验证码",
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 0.dp),
                    keyboardType = KeyboardType.NumberPassword,
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onSendSms,
                    enabled = isValidAuthPhoneNumber(phone) && countdown == 0 && !sendingCode,
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (countdown > 0) AuthPalette.placeholder else DSColors.fairway,
                        contentColor = DSColors.warmWhite,
                        disabledContainerColor = AuthPalette.placeholder,
                        disabledContentColor = DSColors.warmWhite,
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .width(116.dp),
                ) {
                    if (sendingCode) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(
                            text = if (countdown > 0) "${countdown}s" else "获取验证码",
                            maxLines = 1,
                            softWrap = false,
                            fontSize = if (countdown > 0) 14.sp else 12.sp,
                            fontWeight = if (countdown > 0) FontWeight.SemiBold else FontWeight.Bold,
                        )
                    }
                }
            }
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AuthFieldLabel("账号")
            LoginInputRow {
                Icon(Icons.Filled.Person, contentDescription = null, tint = AuthPalette.placeholder, modifier = Modifier.size(14.dp))
                LoginBasicField(
                    value = username,
                    onValueChange = onUsernameChange,
                    placeholder = "手机号 / 用户名 / 邮箱",
                    keyboardType = KeyboardType.Ascii,
                    modifier = Modifier.weight(1f).testTag("login-account"),
                    hasContainer = false,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AuthFieldLabel("密码")
                Spacer(Modifier.weight(1f))
                Text(
                    "忘记密码?",
                    color = AuthPalette.goldAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onForgot),
                )
            }
            LoginInputRow {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = AuthPalette.placeholder, modifier = Modifier.size(14.dp))
                LoginBasicField(
                    value = password,
                    onValueChange = onPasswordChange,
                    placeholder = "请输入密码",
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.weight(1f).testTag("login-password"),
                    hasContainer = false,
                )
                Box(
                    modifier = Modifier.size(32.dp).clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (pwVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (pwVisible) "隐藏密码" else "显示密码",
                        tint = AuthPalette.placeholder,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginInputRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AuthPalette.inputBg)
            .border(1.dp, AuthPalette.divider, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun LoginBasicField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    hasContainer: Boolean = true,
) {
    val field: @Composable () -> Unit = {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(color = AuthPalette.labelText, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(DSColors.fairway),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = AuthPalette.placeholder, fontSize = 14.sp, maxLines = 1)
                    }
                    innerTextField()
                }
            },
        )
    }
    if (hasContainer) {
        Box(modifier = modifier) { LoginInputRow { field() } }
    } else {
        Box(modifier = modifier) { field() }
    }
}

@Composable
private fun LoginAgreementRow(
    agreed: Boolean,
    onAgreedChange: (Boolean) -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = AuthPalette.goldAccent,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline,
        ),
    )
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (agreed) AuthPalette.heroMid2 else Color.Transparent)
                .border(
                    1.5.dp,
                    if (agreed) AuthPalette.heroMid2 else AuthPalette.heroMid2.copy(alpha = 0.4f),
                    RoundedCornerShape(4.dp),
                )
                .clickable { onAgreedChange(!agreed) }
                .testTag("login-agreement"),
            contentAlignment = Alignment.Center,
        ) {
            if (agreed) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            modifier = Modifier.weight(1f),
            color = AuthPalette.labelText,
            fontSize = 12.sp,
            text = buildAnnotatedString {
                append("我已阅读并同意")
                withLink(LinkAnnotation.Clickable("terms", linkStyle) { onOpenTerms() }) {
                    append("《用户协议》")
                }
                append("和")
                withLink(LinkAnnotation.Clickable("privacy", linkStyle) { onOpenPrivacy() }) {
                    append("《隐私政策》")
                }
            },
        )
    }
}
