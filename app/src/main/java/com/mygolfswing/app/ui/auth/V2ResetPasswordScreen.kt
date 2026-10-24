package com.mygolfswing.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.R
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.CancellationException
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
    var sendingCode by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
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
        V2RoomBackground(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            ResetHero(onBack = onBack)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .offset(y = (-32).dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ResetContentCard(
                    phone = phone,
                    onPhoneChange = { phone = it.filter(Char::isDigit).take(11); error = null },
                    code = code,
                    onCodeChange = { code = it.filter(Char::isDigit).take(6); error = null },
                    newPassword = newPw,
                    onNewPasswordChange = { newPw = it; error = null },
                    confirmPassword = confirmPw,
                    onConfirmPasswordChange = { confirmPw = it; error = null },
                    passwordVisible = pwVisible,
                    onTogglePassword = { pwVisible = !pwVisible },
                    confirmVisible = confirmPwVisible,
                    onToggleConfirm = { confirmPwVisible = !confirmPwVisible },
                    countdown = countdown,
                    sendingCode = sendingCode,
                    submitting = submitting,
                    error = error,
                    onSendCode = {
                        if (phone.length != 11) {
                            error = "请输入正确的手机号"
                        } else {
                            scope.launch {
                                sendingCode = true
                                error = null
                                try {
                                    ServiceLocator.authRepo.sendSmsCode(phone)
                                        .onSuccess { countdown = 60 }
                                        .onFailure { error = authUserError("验证码发送失败，请稍后重试", it.message) }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Throwable) {
                                    error = authThrowableError("验证码发送失败，请稍后重试", e)
                                } finally {
                                    sendingCode = false
                                }
                            }
                        }
                    },
                    onSubmit = submit@{
                        val validation = resetPasswordValidationError(phone, code, newPw, confirmPw)
                        if (validation != null) {
                            error = validation
                            return@submit
                        }
                        scope.launch {
                            submitting = true
                            error = null
                            try {
                                val reset = ServiceLocator.authRepo.resetPasswordWithSms(phone, code, newPw)
                                if (reset.isSuccess) {
                                    ServiceLocator.authRepo.loginWithUsername(phone, newPw)
                                        .onSuccess { showSuccess = true }
                                        .onFailure { error = "重置成功，请回到登录页用新密码登录" }
                                } else {
                                    error = authUserError(
                                        "重置失败，请检查验证码和新密码后重试",
                                        reset.exceptionOrNull()?.message,
                                    )
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Throwable) {
                                error = authThrowableError("重置失败，请检查验证码和新密码后重试", e)
                            } finally {
                                submitting = false
                            }
                        }
                    },
                )
                ResetTipsCard()
            }
            Spacer(Modifier.height(28.dp).navigationBarsPadding())
        }
        if (showSuccess) {
            ResetSuccessOverlay()
        }
    }
}

@Composable
private fun ResetHero(onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(260.dp)) {
        Image(
            painter = painterResource(R.drawable.golf_course_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AuthPalette.heroTop.copy(alpha = 0.54f),
                            DSColors.warmWhite.copy(alpha = 0.62f),
                            DSColors.warmWhite,
                        ),
                    ),
                ),
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 54.dp, start = 20.dp)
                .size(36.dp)
                .background(Color.Black.copy(alpha = 0.28f), CircleShape),
        ) {
            Icon(
                Icons.Filled.ChevronLeft,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "重置密码",
                color = Color.White,
                style = TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    shadow = Shadow(AuthPalette.heroTop.copy(alpha = 0.45f), Offset(0f, 2f), 5f),
                ),
            )
            Text(
                "通过绑定的手机号 + 短信验证码",
                color = Color.White.copy(alpha = 0.82f),
                style = TextStyle(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    shadow = Shadow(AuthPalette.heroTop.copy(alpha = 0.30f), Offset(0f, 1f), 3f),
                ),
            )
        }
    }
}

@Composable
private fun ResetContentCard(
    phone: String,
    onPhoneChange: (String) -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    confirmVisible: Boolean,
    onToggleConfirm: () -> Unit,
    countdown: Int,
    sendingCode: Boolean,
    submitting: Boolean,
    error: String?,
    onSendCode: () -> Unit,
    onSubmit: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.18f),
            )
            .background(Color.White, shape)
            .border(1.dp, AuthPalette.heroMid2.copy(alpha = 0.12f), shape)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ResetFieldLabel("手机号")
            ResetInputShell {
                Text(
                    "+86",
                    color = DSColors.fairway,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                ResetInputDivider()
                ResetBasicField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    placeholder = "请输入手机号",
                    keyboardType = KeyboardType.Phone,
                    modifier = Modifier.weight(1f).testTag("reset-phone"),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ResetFieldLabel("验证码")
            ResetInputShell {
                ResetBasicField(
                    value = code,
                    onValueChange = onCodeChange,
                    placeholder = "请输入 6 位验证码",
                    keyboardType = KeyboardType.NumberPassword,
                    modifier = Modifier.weight(1f).testTag("reset-code"),
                )
                ResetInputDivider()
                val sendEnabled = phone.length == 11 && countdown == 0 && !sendingCode
                Box(
                    modifier = Modifier
                        .widthIn(min = 76.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = sendEnabled, onClick = onSendCode)
                        .testTag("reset-send-code"),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        sendingCode -> CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                        countdown > 0 -> Text(
                            "${countdown}s",
                            color = AuthPalette.placeholder,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        else -> Text(
                            "获取验证码",
                            color = AuthPalette.goldAccent.copy(alpha = if (sendEnabled) 1f else 0.55f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        ResetPasswordField(
            label = "新密码",
            value = newPassword,
            onValueChange = onNewPasswordChange,
            visible = passwordVisible,
            onToggleVisible = onTogglePassword,
            placeholder = "8 位以上，包含字母和数字",
            tag = "reset-new-password",
        )
        ResetPasswordField(
            label = "确认新密码",
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            visible = confirmVisible,
            onToggleVisible = onToggleConfirm,
            placeholder = "再次输入新密码",
            tag = "reset-confirm-password",
        )

        if (!error.isNullOrBlank()) {
            Row(
                modifier = Modifier.offset(y = (-4).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Error, null, tint = DSColors.flagRed, modifier = Modifier.size(12.dp))
                Text(error, color = DSColors.flagRed, fontSize = 12.sp)
            }
        }

        Button(
            onClick = onSubmit,
            enabled = !submitting,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AuthPalette.heroMid2,
                contentColor = Color.White,
                disabledContainerColor = AuthPalette.heroMid2,
                disabledContentColor = Color.White,
            ),
            modifier = Modifier
                .padding(top = if (error.isNullOrBlank()) 4.dp else 0.dp)
                .fillMaxWidth()
                .height(54.dp)
                .shadow(
                    elevation = 7.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = AuthPalette.heroMid2.copy(alpha = 0.30f),
                    spotColor = AuthPalette.heroMid2.copy(alpha = 0.30f),
                )
                .testTag("reset-submit"),
        ) {
            if (submitting) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text("重置密码并登录", fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
    }
}

@Composable
private fun ResetPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    placeholder: String,
    tag: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ResetFieldLabel(label)
        ResetInputShell(spacing = 12.dp) {
            Icon(Icons.Filled.Lock, null, tint = AuthPalette.placeholder, modifier = Modifier.size(14.dp))
            ResetBasicField(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                keyboardType = KeyboardType.Password,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.weight(1f).testTag(tag),
            )
            IconButton(onClick = onToggleVisible, modifier = Modifier.size(34.dp)) {
                Icon(
                    if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (visible) "隐藏$label" else "显示$label",
                    tint = AuthPalette.placeholder,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun ResetInputShell(
    spacing: Dp = 10.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AuthPalette.inputBg)
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

@Composable
private fun ResetInputDivider() {
    Box(Modifier.width(1.dp).height(22.dp).background(Color.White.copy(alpha = 0.22f)))
}

@Composable
private fun ResetBasicField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = TextStyle(color = AuthPalette.labelText, fontSize = 15.sp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(DSColors.fairway),
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

@Composable
private fun ResetFieldLabel(text: String) {
    Text(text, color = AuthPalette.labelText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun ResetTipsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(14.dp))
            .border(1.dp, AuthPalette.heroMid2.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Info, null, tint = AuthPalette.goldAccent, modifier = Modifier.size(12.dp))
            Text("温馨提示", color = AuthPalette.labelText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text("• 如果账号未绑定手机号，请联系客服重置密码", color = Color(0xFF6B7280), fontSize = 11.5.sp)
        Text("• 客服邮箱：372931693@qq.com", color = Color(0xFF6B7280), fontSize = 11.5.sp)
        Text("• 客服电话：二胡 18851310088", color = Color(0xFF6B7280), fontSize = 11.5.sp)
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
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .widthIn(max = 300.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color.Black.copy(alpha = 0.22f),
                    spotColor = Color.Black.copy(alpha = 0.22f),
                )
                .background(DSColors.cardWhite, RoundedCornerShape(22.dp))
                .border(1.dp, AuthPalette.heroMid2.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
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
            Text("密码重置成功！", color = AuthPalette.labelText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("已自动登录，正在进入主页…", color = AuthPalette.placeholder, fontSize = 13.sp)
            CircularProgressIndicator(color = DSColors.fairway, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        }
    }
}

internal fun resetPasswordValidationError(phone: String, code: String, newPw: String, confirmPw: String): String? {
    if (phone.length != 11) return "请输入正确的手机号"
    if (code.length != 6) return "请输入 6 位验证码"
    if (newPw.length < 8) return "新密码至少 8 位"
    if (!newPw.any { it.isLetter() } || !newPw.any { it.isDigit() }) return "密码需同时包含字母和数字"
    if (newPw != confirmPw) return "两次输入的密码不一致"
    return null
}
