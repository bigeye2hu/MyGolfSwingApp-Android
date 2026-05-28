package com.mygolfswing.app.ui.auth

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.launch

/**
 * 注册页（对齐 iOS `RegisterPage`）。
 *
 * 字段：
 * - 头像（5 个预设 emoji 选其一，自动填充昵称）
 * - 用户名（字母开头，3–20 字符）
 * - 昵称
 * - 密码（至少 8 位，含字母 + 数字）
 * - 确认密码
 */
private data class PresetAvatar(val key: String, val emoji: String, val name: String)

private val presetAvatars = listOf(
    PresetAvatar("xiaoqie", "🐧", "小切"),
    PresetAvatar("koala", "🐨", "考拉"),
    PresetAvatar("fox", "🦊", "狐狸"),
    PresetAvatar("bear", "🐻", "小熊"),
    PresetAvatar("dino", "🦖", "恐龙"),
)

@Composable
fun V2RegisterScreen(
    onBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
) {
    var avatarKey by remember { mutableStateOf<String?>(null) }
    var username by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite)
            .verticalScroll(rememberScrollState()),
    ) {
        V2NavTopBarCentered(title = "创建账户", onBack = onBack)

        Column(
            modifier = Modifier
                .padding(horizontal = DSSpacing.lg)
                .padding(top = 4.dp, bottom = 40.dp)
                .fillMaxWidth()
                .background(DSColors.cardWhite, RoundedCornerShape(20.dp))
                .padding(DSSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("选择头像", style = DSTypography.bodyBold, color = DSColors.inkGreen)
            AvatarPicker(
                selectedKey = avatarKey,
                onPick = { p ->
                    avatarKey = p.key
                    if (nickname.isBlank()) {
                        nickname = "${p.name}${(1000..9999).random()}"
                    }
                },
            )

            LabeledField("用户名", "字母开头，3–20 字符") {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; error = null },
                    placeholder = { Text("例：golfer_lee") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
            }
            LabeledField("昵称", null) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it; error = null },
                    placeholder = { Text("展示给球友的名字") },
                    singleLine = true,
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
                    visualTransformation = PasswordVisualTransformation(),
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
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
            }

            if (!error.isNullOrBlank()) {
                Text(error!!, color = DSColors.flagRed, style = DSTypography.caption)
            }

            Button(
                onClick = {
                    val v = validate(avatarKey, username, nickname, password, confirmPassword)
                    if (v != null) {
                        error = v
                        return@Button
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
                    Text("创建账户", style = DSTypography.bodyBold)
                }
            }

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
    }
}

@Composable
private fun AvatarPicker(selectedKey: String?, onPick: (PresetAvatar) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(items = presetAvatars, key = { it.key }) { a ->
            val selected = a.key == selectedKey
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            if (selected) DSColors.mistGreen else DSColors.warmWhite,
                            CircleShape,
                        )
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) DSColors.fairway else DSColors.borderLine,
                            shape = CircleShape,
                        )
                        .clickable { onPick(a) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(a.emoji, fontSize = 28.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    a.name,
                    style = DSTypography.caption,
                    color = if (selected) DSColors.fairway else DSColors.shadowGray,
                )
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, hint: String?, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = DSTypography.bodyBold, color = DSColors.inkGreen)
        content()
        if (hint != null) {
            Text(hint, style = DSTypography.caption, color = DSColors.shadowGray)
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DSColors.fairway,
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

