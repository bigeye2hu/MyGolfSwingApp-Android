package com.mygolfswing.app.ui.auth

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.ui.common.V2PresetAvatar
import com.mygolfswing.app.ui.common.V2PresetAvatars
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 注册页（对齐 iOS `RegisterPage`）。
 *
 * 字段：
 * - 头像（5 个预设 + 相册自定义，key 与 iOS `Avatar_*` 一致，自动填充昵称）
 * - 用户名（字母开头，3–20 字符）
 * - 昵称
 * - 密码（至少 8 位，含字母 + 数字）
 * - 确认密码
 */
@Composable
fun V2RegisterScreen(
    onBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onGuest: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val defaultAvatar = remember { V2PresetAvatars.all.first() }
    var avatarKey by remember { mutableStateOf<String?>(defaultAvatar.assetKey) }
    var customAvatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var readingCustomAvatar by remember { mutableStateOf(false) }
    var pendingCustomAvatarUpload by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf(generatedNickname(defaultAvatar.displayName)) }
    var nicknameIsAutoGenerated by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var pwVisible by remember { mutableStateOf(false) }
    var confirmPwVisible by remember { mutableStateOf(false) }
    var agreed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            readingCustomAvatar = true
            error = null
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    decodeRegisterAvatar(context.contentResolver, uri)
                }
                if (bitmap == null) {
                    error = "无法读取这张图片，请重新选择"
                } else {
                    customAvatarBitmap = bitmap
                    avatarKey = null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                error = "无法读取这张图片，请重新选择"
            } finally {
                readingCustomAvatar = false
            }
        }
    }

    fun openGalleryPicker() {
        runCatching {
            galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }.onFailure {
            error = "无法打开相册，请稍后重试"
        }
    }

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
        Hero(onBack = onBack)

        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .offset(y = (-42).dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RegisterFormCard(
                avatarKey = avatarKey,
                customAvatarBitmap = customAvatarBitmap,
                readingCustomAvatar = readingCustomAvatar,
                onPickAvatar = { preset ->
                    customAvatarBitmap = null
                    avatarKey = preset.assetKey
                    if (nicknameIsAutoGenerated || nickname.isBlank()) {
                        nickname = generatedNickname(preset.displayName)
                        nicknameIsAutoGenerated = true
                    }
                    error = null
                },
                onPickCustom = ::openGalleryPicker,
                username = username,
                onUsernameChange = { username = it; error = null },
                nickname = nickname,
                onNicknameChange = {
                    nickname = it
                    if (it.isNotEmpty() && !isGeneratedNickname(it)) nicknameIsAutoGenerated = false
                    error = null
                },
                nicknameIsAutoGenerated = nicknameIsAutoGenerated,
                onRegenerateNickname = {
                    val selected = V2PresetAvatars.all.firstOrNull { it.assetKey == avatarKey } ?: defaultAvatar
                    nickname = generatedNickname(selected.displayName)
                    nicknameIsAutoGenerated = true
                    error = null
                },
                password = password,
                onPasswordChange = { password = it; error = null },
                passwordVisible = pwVisible,
                onTogglePassword = { pwVisible = !pwVisible },
                confirmPassword = confirmPassword,
                onConfirmPasswordChange = { confirmPassword = it; error = null },
                confirmPasswordVisible = confirmPwVisible,
                onToggleConfirmPassword = { confirmPwVisible = !confirmPwVisible },
                error = error,
            )

            RegisterAgreementRow(
                agreed = agreed,
                onAgreedChange = { agreed = it; error = null },
                onOpenTerms = onOpenTerms,
                onOpenPrivacy = onOpenPrivacy,
            )

            RegisterCreateButton(
                text = if (pendingCustomAvatarUpload) "重试上传头像" else "创建账户",
                loading = loading,
                enabled = !readingCustomAvatar,
                visualAlpha = if (agreed && !readingCustomAvatar) 1f else 0.55f,
                onClick = {
                    val v = validate(avatarKey != null || customAvatarBitmap != null, username, nickname, password, confirmPassword)
                    if (v != null) {
                        error = v
                        return@RegisterCreateButton
                    }
                    if (!agreed) {
                        error = "请先阅读并同意服务条款和隐私政策"
                        return@RegisterCreateButton
                    }
                    scope.launch {
                        loading = true
                        try {
                            if (pendingCustomAvatarUpload) {
                                val customAvatar = customAvatarBitmap
                                if (customAvatar != null) {
                                    val uploadResult = ServiceLocator.authRepo.uploadAvatar(customAvatar)
                                    if (uploadResult.isFailure) {
                                        error = "头像上传失败，请检查网络后重试"
                                        return@launch
                                    }
                                }
                                pendingCustomAvatarUpload = false
                                onRegisterSuccess()
                                return@launch
                            }

                            val r = ServiceLocator.authRepo.registerWithUsername(
                                username = username.trim(),
                                password = password,
                                nickname = nickname.trim(),
                                avatar = avatarKey,
                            )
                            if (r.isFailure) {
                                error = authUserError("注册失败，请检查账号信息后重试", r.exceptionOrNull()?.message)
                                return@launch
                            }

                            val customAvatar = customAvatarBitmap
                            if (customAvatar != null) {
                                val uploadResult = ServiceLocator.authRepo.uploadAvatar(customAvatar)
                                if (uploadResult.isFailure) {
                                    pendingCustomAvatarUpload = true
                                    error = "账号已创建，但头像上传失败。请点击“重试上传头像”"
                                    return@launch
                                }
                            }
                            onRegisterSuccess()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            error = authThrowableError("注册失败，请检查账号信息后重试", e)
                        } finally {
                            loading = false
                        }
                    }
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("已有账户？", fontSize = 14.sp, color = AuthPalette.placeholder)
                Text(
                    "立即登录",
                    color = DSColors.fairway,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onBack).padding(vertical = 6.dp),
                )
            }

            Button(
                onClick = onGuest,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DSColors.mistGreen,
                    contentColor = DSColors.fairway,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Text("先以游客身份浏览", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
            Spacer(Modifier.height(28.dp).navigationBarsPadding())
        }
    }
}

@Composable
private fun RegisterFormCard(
    avatarKey: String?,
    customAvatarBitmap: Bitmap?,
    readingCustomAvatar: Boolean,
    onPickAvatar: (V2PresetAvatar) -> Unit,
    onPickCustom: () -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    nicknameIsAutoGenerated: Boolean,
    onRegenerateNickname: () -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    confirmPasswordVisible: Boolean,
    onToggleConfirmPassword: () -> Unit,
    error: String?,
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
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RegisterFieldLabel("选择头像")
                Text(
                    "已为你选好，可更换",
                    color = AuthPalette.heroMid2.copy(alpha = 0.80f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            AvatarPicker(
                selectedKey = avatarKey,
                customAvatarBitmap = customAvatarBitmap,
                readingCustomAvatar = readingCustomAvatar,
                onPick = onPickAvatar,
                onPickCustom = onPickCustom,
            )
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(AuthPalette.divider))

        RegisterLabeledField(label = "用户名", required = true) {
            RegisterInputRow {
                Icon(Icons.Outlined.Person, null, tint = AuthPalette.placeholder, modifier = Modifier.size(14.dp))
                RegisterBasicField(
                    value = username,
                    onValueChange = onUsernameChange,
                    placeholder = "字母开头，3-20位字母/数字/下划线",
                    keyboardType = KeyboardType.Ascii,
                    modifier = Modifier.weight(1f).testTag("register-username"),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RegisterFieldLabel("昵称")
                Text(
                    "(选择头像后自动生成)",
                    color = AuthPalette.heroMid2.copy(alpha = 0.80f),
                    fontSize = 11.sp,
                )
            }
            RegisterInputRow {
                Icon(Icons.Filled.AutoAwesome, null, tint = AuthPalette.placeholder, modifier = Modifier.size(14.dp))
                RegisterBasicField(
                    value = nickname,
                    onValueChange = onNicknameChange,
                    placeholder = "选择头像后自动生成，也可自定义",
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.weight(1f).testTag("register-nickname"),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    if (nicknameIsAutoGenerated) Icons.Filled.AutoAwesome else Icons.Filled.Verified,
                    null,
                    tint = if (nicknameIsAutoGenerated) AuthPalette.goldAccent else AuthPalette.heroMid2,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    if (nicknameIsAutoGenerated) "已帮你生成昵称，可直接注册" else "已使用你的自定义昵称，换头像不会覆盖",
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                if (nicknameIsAutoGenerated) {
                    Text(
                        "换一个",
                        color = AuthPalette.heroMid2,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onRegenerateNickname)
                            .padding(horizontal = 2.dp, vertical = 4.dp)
                            .testTag("register-regenerate-nickname"),
                    )
                }
            }
        }

        RegisterPasswordField(
            label = "密码",
            required = true,
            value = password,
            onValueChange = onPasswordChange,
            visible = passwordVisible,
            onToggleVisible = onTogglePassword,
            placeholder = "8位以上，包含字母和数字",
            tag = "register-password",
        )
        RegisterPasswordField(
            label = "确认密码",
            required = false,
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            visible = confirmPasswordVisible,
            onToggleVisible = onToggleConfirmPassword,
            placeholder = "再次输入密码",
            tag = "register-confirm-password",
        )

        if (!error.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Error, null, tint = DSColors.flagRed, modifier = Modifier.size(12.dp))
                Text(error, color = DSColors.flagRed, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RegisterLabeledField(
    label: String,
    required: Boolean,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RegisterFieldLabel(label)
            if (required) Text("*", color = DSColors.flagRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun RegisterPasswordField(
    label: String,
    required: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    placeholder: String,
    tag: String,
) {
    RegisterLabeledField(label, required) {
        RegisterInputRow {
            Icon(Icons.Filled.Lock, null, tint = AuthPalette.placeholder, modifier = Modifier.size(14.dp))
            RegisterBasicField(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                keyboardType = KeyboardType.Password,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.weight(1f).testTag(tag),
            )
            IconButton(onClick = onToggleVisible, modifier = Modifier.size(32.dp)) {
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
private fun RegisterInputRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AuthPalette.inputBg)
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun RegisterBasicField(
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
        textStyle = TextStyle(color = AuthPalette.labelText, fontSize = 14.sp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        cursorBrush = SolidColor(DSColors.fairway),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, color = AuthPalette.placeholder, fontSize = 13.sp, maxLines = 1)
                innerTextField()
            }
        },
    )
}

@Composable
private fun RegisterFieldLabel(text: String) {
    Text(text, color = AuthPalette.labelText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun RegisterAgreementRow(
    agreed: Boolean,
    onAgreedChange: (Boolean) -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (agreed) AuthPalette.heroMid2 else Color.Transparent)
                .border(
                    1.5.dp,
                    if (agreed) AuthPalette.heroMid2 else AuthPalette.heroMid2.copy(alpha = 0.40f),
                    RoundedCornerShape(4.dp),
                )
                .clickable { onAgreedChange(!agreed) }
                .testTag("register-agreement"),
            contentAlignment = Alignment.Center,
        ) {
            if (agreed) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
        }
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text("我已阅读并同意", color = AuthPalette.labelText, fontSize = 12.sp)
            Text(
                "《用户协议》",
                color = AuthPalette.goldAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onOpenTerms),
            )
            Text("和", color = AuthPalette.labelText, fontSize = 12.sp)
            Text(
                "《隐私政策》",
                color = AuthPalette.goldAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onOpenPrivacy),
            )
        }
    }
}

@Composable
private fun RegisterCreateButton(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    visualAlpha: Float,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthPalette.heroMid2,
            contentColor = Color.White,
            disabledContainerColor = AuthPalette.heroMid2,
            disabledContentColor = Color.White,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = 7.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = AuthPalette.heroMid2.copy(alpha = 0.30f),
                spotColor = AuthPalette.heroMid2.copy(alpha = 0.30f),
            )
            .alpha(visualAlpha)
            .testTag("register-create"),
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.14f), CircleShape),
        ) {
            Icon(
                Icons.Filled.ChevronLeft,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(15.dp),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 86.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "挥呗",
                color = DSColors.warmWhite,
                style = DSTypography.titleLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold),
            )
            Text(
                "让每一次练习都有迹可循",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
            )
            RegisterHeroValueRibbon(Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun RegisterHeroValueRibbon(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)), RoundedCornerShape(50))
            .height(30.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        RegisterHeroValueChip(Icons.Filled.VideoLibrary, "优质视频")
        RegisterHeroValueChip(Icons.Filled.Description, "练球心得")
        RegisterHeroValueChip(Icons.Filled.AutoAwesome, "AI 助手")
    }
}

@Composable
private fun RegisterHeroValueChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(9.dp))
        Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun AvatarPicker(
    selectedKey: String?,
    customAvatarBitmap: Bitmap?,
    readingCustomAvatar: Boolean,
    onPick: (V2PresetAvatar) -> Unit,
    onPickCustom: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val avatarSize = minOf(42.dp, ((maxWidth.value - 40f) / 6f).dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
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
                            .size(avatarSize)
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
                        alpha = if (selected) 1f else 0.58f,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = avatar.displayName,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) DSColors.fairway else AuthPalette.placeholder.copy(alpha = 0.62f),
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(enabled = !readingCustomAvatar, onClick = onPickCustom),
            ) {
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(AuthPalette.inputBg)
                        .border(
                            width = if (customAvatarBitmap == null) 1.dp else 2.5.dp,
                            color = if (customAvatarBitmap == null) AuthPalette.divider else DSColors.fairway,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (customAvatarBitmap != null) {
                        Image(
                            bitmap = customAvatarBitmap.asImageBitmap(),
                            contentDescription = "自定义头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "从相册选择自定义头像",
                            tint = DSColors.fairway,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    if (readingCustomAvatar) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "自定义",
                    fontSize = 11.sp,
                    fontWeight = if (customAvatarBitmap == null) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (customAvatarBitmap == null) AuthPalette.placeholder else DSColors.fairway,
                )
            }
        }
    }
}

private fun validate(
    hasAvatar: Boolean,
    username: String,
    nickname: String,
    password: String,
    confirmPassword: String,
): String? {
    if (!hasAvatar) return "请选择一个头像"
    if (username.isBlank()) return "请输入用户名"
    if (!username.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]{2,19}$"))) return "用户名需以字母开头，3-20位字母、数字或下划线"
    if (nickname.isBlank()) return "请输入昵称"
    if (password.length < 8) return "密码至少 8 位"
    if (!password.any { it.isLetter() } || !password.any { it.isDigit() }) return "密码需同时包含字母和数字"
    if (password != confirmPassword) return "两次输入的密码不一致"
    return null
}

private fun generatedNickname(avatarDisplayName: String): String =
    "$avatarDisplayName${(0..9999).random().toString().padStart(4, '0')}"

private fun isGeneratedNickname(value: String): Boolean = V2PresetAvatars.all.any { avatar ->
    value.startsWith(avatar.displayName) && value.length <= avatar.displayName.length + 4
}

internal fun decodeRegisterAvatar(
    contentResolver: ContentResolver,
    uri: Uri,
    maxSide: Int = 2_048,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxSide) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}
