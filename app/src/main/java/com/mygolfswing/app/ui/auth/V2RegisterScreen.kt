package com.mygolfswing.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.ui.common.V2PresetAvatar
import com.mygolfswing.app.ui.common.V2PresetAvatars
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.launch

/**
 * 注册页（对齐 iOS `RegisterPage`）。
 *
 * 字段：
 * - 头像（5 个预设，key 与 iOS `Avatar_*` 一致，自动填充昵称）
 * - 用户名（字母开头，3–20 字符）
 * - 昵称
 * - 密码（至少 8 位，含字母 + 数字）
 * - 确认密码
 */
@Composable
fun V2RegisterScreen(
    onBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    var avatarKey by remember { mutableStateOf<String?>(null) }
    var username by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var pwVisible by remember { mutableStateOf(false) }
    var confirmPwVisible by remember { mutableStateOf(false) }
    var agreed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite)
            .verticalScroll(rememberScrollState()),
    ) {
        Hero(onBack = onBack)

        AuthCard(
            modifier = Modifier
                .padding(horizontal = DSSpacing.lg)
                .offset(y = (-40).dp),
        ) {
            AuthFieldLabel("选择头像", required = true)
            AvatarPicker(
                selectedKey = avatarKey,
                onPick = { p ->
                    avatarKey = p.assetKey
                    nickname = "${p.displayName}${(0..9999).random().toString().padStart(4, '0')}"
                },
            )

            LabeledField("用户名", "字母开头，3–20 字符") {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; error = null },
                    placeholder = { Text("字母开头，3-20位字母/数字/下划线") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
            }
            LabeledField("昵称", null) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it; error = null },
                    placeholder = { Text("选择头像后自动生成，也可自定义") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
            }
            LabeledField("密码", "至少 8 位，含字母 + 数字") {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    placeholder = { Text("设置登录密码") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = AuthPalette.placeholder) },
                    trailingIcon = {
                        IconButton(onClick = { pwVisible = !pwVisible }) {
                            Icon(
                                if (pwVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null,
                                tint = AuthPalette.placeholder,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
            }
            LabeledField("确认密码", null) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    placeholder = { Text("再次输入密码") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (confirmPwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = AuthPalette.placeholder) },
                    trailingIcon = {
                        IconButton(onClick = { confirmPwVisible = !confirmPwVisible }) {
                            Icon(
                                if (confirmPwVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null,
                                tint = AuthPalette.placeholder,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
            }

            if (!error.isNullOrBlank()) {
                Text(error!!, color = DSColors.flagRed, style = DSTypography.caption)
            }

            AuthAgreementRow(
                agreed = agreed,
                onAgreedChange = { agreed = it; error = null },
                onOpenTerms = onOpenTerms,
                onOpenPrivacy = onOpenPrivacy,
            )

            AuthPrimaryButton(
                text = "创建账户",
                loading = loading,
                onClick = {
                    if (!agreed) {
                        error = "请先阅读并同意服务条款和隐私政策"
                        return@AuthPrimaryButton
                    }
                    val v = validate(avatarKey, username, nickname, password, confirmPassword)
                    if (v != null) {
                        error = v
                        return@AuthPrimaryButton
                    }
                    scope.launch {
                        loading = true
                        val r = ServiceLocator.authRepo.registerWithUsername(
                            username = username.trim(),
                            password = password,
                            nickname = nickname.trim(),
                            avatar = avatarKey,
                        )
                        loading = false
                        r.onSuccess { onRegisterSuccess() }
                            .onFailure { error = it.message ?: "注册失败" }
                    }
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("已有账户？", style = DSTypography.caption, color = DSColors.shadowGray)
                TextButton(onClick = onBack) {
                    Text("立即登录", color = DSColors.fairway, style = DSTypography.bodyBold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Hero(onBack: () -> Unit) {
    AuthHeroBackground(height = 280.dp, bottomFadeHeight = 80.dp) {
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
        Text(
            "创建账户",
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp),
            color = DSColors.warmWhite,
            style = DSTypography.bodyBold,
        )
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthLogoBadge(size = 72.dp, imageSize = 54.dp, rainbow = false)
            Spacer(Modifier.height(10.dp))
            Text("挥呗", color = DSColors.warmWhite, style = DSTypography.titleLarge.copy(fontSize = 28.sp))
            Spacer(Modifier.height(4.dp))
            Text("开始你的挥呗高尔夫成长之旅", color = DSColors.warmWhite.copy(alpha = 0.82f), style = DSTypography.caption)
        }
    }
}

@Composable
private fun AvatarPicker(selectedKey: String?, onPick: (V2PresetAvatar) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top,
    ) {
        V2PresetAvatars.all.forEach { avatar ->
            val selected = avatar.assetKey == selectedKey
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onPick(avatar) },
            ) {
                Image(
                    painter = painterResource(avatar.drawableRes),
                    contentDescription = avatar.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(48.dp)
                        .then(
                            if (selected) {
                                Modifier.shadow(6.dp, CircleShape, spotColor = DSColors.fairway.copy(alpha = 0.3f))
                            } else {
                                Modifier
                            },
                        )
                        .clip(CircleShape)
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) DSColors.fairway else AuthPalette.divider,
                            shape = CircleShape,
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = avatar.displayName,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) DSColors.fairway else AuthPalette.placeholder,
                )
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, hint: String?, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AuthFieldLabel(label, required = label == "用户名" || label == "密码")
        content()
        if (hint != null) {
            Text(hint, style = DSTypography.caption, color = DSColors.shadowGray)
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

private fun validate(
    avatarKey: String?,
    username: String,
    nickname: String,
    password: String,
    confirmPassword: String,
): String? {
    if (avatarKey == null) return "请选择头像"
    if (!username.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]{2,19}$"))) return "用户名需字母开头，3–20 字符"
    if (nickname.isBlank()) return "请填写昵称"
    if (password.length < 8) return "密码至少 8 位"
    if (!password.any { it.isLetter() } || !password.any { it.isDigit() }) return "密码需同时包含字母和数字"
    if (password != confirmPassword) return "两次密码不一致"
    return null
}
