package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.R
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.auth.V2ChangePasswordDialog
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 账号与安全 / 设置页（对齐 iOS `ProfileSettingsPage`）。
 *
 * 从 V2「我」页「账号与安全」进入；密码修改沿用 iOS 的当前页品牌弹窗。
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun V2AccountSecurityScreen(
    onBack: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenCloudAudit: () -> Unit,
    onOpenAccountDeletion: () -> Unit,
    onLogoutComplete: () -> Unit,
    hasActiveDelivery: Boolean = false,
) {
    val profile by V2ProfileStore.profile.collectAsState()
    var confirmLogout by remember { mutableStateOf(false) }
    var showNicknameEditor by remember { mutableStateOf(false) }
    var showPhoneEditor by remember { mutableStateOf(false) }
    var showPasswordSetup by remember { mutableStateOf(false) }
    var phoneEditorHadPhone by remember { mutableStateOf(false) }
    var loggingOut by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var logoutError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        V2ProfileStore.refresh(force = true)
    }

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AccountSettingsTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .v2NavigationBarPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                BasicInfoSection(
                    username = profile.username.removePrefix("@"),
                    nickname = profile.nickname,
                    email = profile.email,
                    phone = profile.phone,
                    createdAt = profile.createdAt,
                    onEditNickname = { showNicknameEditor = true },
                    onEditPhone = {
                        phoneEditorHadPhone = !profile.phone.isNullOrBlank()
                        showPhoneEditor = true
                    },
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionCaption("账号与安全")
                        SettingsGroup {
                            SettingsActionRow(
                                icon = Icons.Filled.Key,
                                title = "设置登录密码",
                                trailingText = if (profile.hasPassword) "已设置" else "未设置",
                                trailingColor = if (profile.hasPassword) Color(0xFF34C759) else Color(0xFFFF9500),
                                onClick = { showPasswordSetup = true },
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionCaption("关于")
                        SettingsGroup {
                            SettingsActionRow(
                                icon = Icons.Filled.Description,
                                title = "用户协议",
                                onClick = onOpenTerms,
                            )
                            GroupDivider()
                            SettingsActionRow(
                                icon = Icons.Filled.Shield,
                                title = "隐私政策",
                                lockShield = true,
                                onClick = onOpenPrivacy,
                            )
                        }
                    }

                    ActionCard(onClick = {
                        if (!loggingOut && !hasActiveDelivery) confirmLogout = true
                    }) {
                        if (loggingOut) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = DSColors.fairway,
                            )
                        } else {
                            SettingsIconBox(icon = Icons.AutoMirrored.Filled.Logout)
                        }
                        Text(
                            text = when {
                                loggingOut -> "退出中..."
                                hasActiveDelivery -> "内容发送中，暂不可退出"
                                else -> "退出登录"
                            },
                            fontSize = 16.sp,
                            color = if (loggingOut || hasActiveDelivery) DSColors.shadowGray else DSColors.fairway,
                            modifier = Modifier.weight(1f),
                        )
                        if (!loggingOut && !hasActiveDelivery) {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = DSColors.shadowGray.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }

                    ActionCard(onClick = onOpenAccountDeletion) {
                        SettingsIconBox(icon = Icons.Filled.Delete, tint = DSColors.flagRed)
                        Text(
                            text = "删除账号",
                            fontSize = 16.sp,
                            color = DSColors.flagRed,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = DSColors.shadowGray.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }

    if (confirmLogout) {
        ProfileLogoutDialog(
            onDismiss = { confirmLogout = false },
            onConfirm = {
                confirmLogout = false
                scope.launch {
                    loggingOut = true
                    try {
                        V2ProfileStore.logoutAndWait()
                        onLogoutComplete()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        logoutError = profileLogoutThrowableError(e)
                    } finally {
                        loggingOut = false
                    }
                }
            },
        )
    }

    logoutError?.let { message ->
        AlertDialog(
            onDismissRequest = { logoutError = null },
            title = { Text("退出失败", style = DSTypography.titleMedium) },
            text = { Text(message, style = DSTypography.body) },
            confirmButton = {
                TextButton(onClick = { logoutError = null }) {
                    Text("好", color = DSColors.fairway)
                }
            },
        )
    }

    if (showNicknameEditor) {
        V2NicknameEditorDialog(
            initial = profile.nickname,
            onDismiss = { showNicknameEditor = false },
            onSaved = {
                showNicknameEditor = false
                V2ProfileStore.refresh(force = true)
                successMessage = "昵称已更新"
            },
        )
    }

    if (showPhoneEditor) {
        V2PhoneBindDialog(
            hasPhone = !profile.phone.isNullOrBlank(),
            currentPhone = profile.phone,
            onDismiss = { showPhoneEditor = false },
            onSaved = {
                showPhoneEditor = false
                V2ProfileStore.refresh(force = true)
                successMessage = if (phoneEditorHadPhone) "手机号换绑成功" else "手机号绑定成功"
            },
        )
    }

    if (showPasswordSetup) {
        V2ChangePasswordDialog(
            onDismiss = { showPasswordSetup = false },
            onChangeSuccess = { showPasswordSetup = false },
        )
    }

    successMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { successMessage = null },
            title = { Text("操作成功", style = DSTypography.titleMedium) },
            text = { Text(message, style = DSTypography.body) },
            confirmButton = {
                TextButton(onClick = { successMessage = null }) {
                    Text("确定", color = DSColors.fairway)
                }
            },
        )
    }
}

@Composable
private fun AccountSettingsTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .height(44.dp)
            .testTag("account-settings-top-bar")
            .background(DSColors.warmWhite),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Filled.ChevronLeft,
                contentDescription = "返回",
                tint = DSColors.fairway,
                modifier = Modifier.size(16.dp),
            )
            Text("返回", color = DSColors.fairway, fontSize = 16.sp)
        }
        Text(
            "设置",
            style = DSTypography.titleMedium,
            color = DSColors.inkGreen,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
internal fun ProfileLogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val consumeCardTap = { Unit }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
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
                Box(Modifier.fillMaxWidth().height(92.dp)) {
                    Image(
                        painter = painterResource(R.drawable.golf_course_background),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
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
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Text("退出登录", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "退出后仍可重新登录此账号",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DSColors.cardWhite)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            "确认退出当前账号？",
                            color = DSColors.inkGreen,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "本机登录状态将被清除，不会删除账号和云端数据。",
                            color = DSColors.shadowGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LogoutDialogAction(
                            label = "取消",
                            background = DSColors.mistGreen,
                            foreground = DSColors.inkGreen,
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        )
                        LogoutDialogAction(
                            label = "确认退出",
                            background = DSColors.fairway,
                            foreground = Color.White,
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoutDialogAction(
    label: String,
    background: Color,
    foreground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BasicInfoSection(
    username: String,
    nickname: String,
    email: String?,
    phone: String?,
    createdAt: String?,
    onEditNickname: () -> Unit,
    onEditPhone: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.AccountBox,
                contentDescription = null,
                tint = DSColors.inkGreen,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "基本信息",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = DSColors.inkGreen,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = DSColors.fairway.copy(alpha = 0.06f))
                .clip(RoundedCornerShape(16.dp))
                .background(DSColors.cardWhite)
                .border(1.dp, DSColors.borderLine, RoundedCornerShape(16.dp)),
        ) {
            InfoRow(icon = Icons.Filled.AlternateEmail, title = "用户名", subtitle = username.ifBlank { "—" })
            CardDivider()
            InfoRow(
                icon = Icons.Filled.Person,
                title = "昵称",
                subtitle = nickname.ifBlank { "未设置" },
                showArrow = true,
                onClick = onEditNickname,
            )
            CardDivider()
            InfoRow(
                icon = Icons.Filled.Email,
                title = "邮箱",
                subtitle = email?.takeIf { it.isNotBlank() } ?: "未绑定",
            )
            CardDivider()
            PhoneRow(
                phone = phone,
                onClick = onEditPhone,
            )
            CardDivider()
            InfoRow(
                icon = Icons.Filled.CalendarMonth,
                title = "注册时间",
                subtitle = createdAt?.takeIf { it.isNotBlank() } ?: "—",
            )
        }
    }
}

@Composable
private fun PhoneRow(phone: String?, onClick: () -> Unit) {
    val hasPhone = !phone.isNullOrBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsIconBox(icon = Icons.Filled.Phone)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "手机号", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DSColors.inkGreen)
            Text(
                text = if (hasPhone) phone!!.maskPhone() else "未绑定",
                fontSize = 12.sp,
                color = DSColors.shadowGray,
            )
        }
        Text(
            text = if (hasPhone) "换绑" else "绑定",
            fontSize = 12.sp,
            color = DSColors.fairway,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(DSColors.fairway.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = DSColors.shadowGray.copy(alpha = 0.7f),
            modifier = Modifier.size(12.dp),
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    showArrow: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsIconBox(icon = icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DSColors.inkGreen)
            Text(text = subtitle, fontSize = 12.sp, color = DSColors.shadowGray)
        }
        if (showArrow) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "编辑昵称",
                tint = DSColors.fairway,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun SectionCaption(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = DSColors.shadowGray,
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.6f)),
    ) {
        content()
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    trailingColor: Color = DSColors.shadowGray,
    lockShield: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsIconBox(icon = icon, lockShield = lockShield)
        Text(text = title, fontSize = 16.sp, color = DSColors.inkGreen, modifier = Modifier.weight(1f))
        if (trailingText != null) {
            Text(text = trailingText, fontSize = 14.sp, color = trailingColor)
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = DSColors.shadowGray.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun ActionCard(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = DSColors.fairway.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun SettingsIconBox(
    icon: ImageVector,
    tint: Color = DSColors.fairway,
    lockShield: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DSColors.mistGreen),
        contentAlignment = Alignment.Center,
    ) {
        if (lockShield) {
            V2LockShieldGlyph(
                foreground = tint,
                cutout = DSColors.mistGreen,
                modifier = Modifier.size(14.dp),
            )
        } else {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun CardDivider() {
    Box(
        modifier = Modifier
            .padding(start = 52.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(DSColors.borderLine),
    )
}

@Composable
private fun GroupDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DSColors.borderLine.copy(alpha = 0.5f)),
    )
}

private fun String.maskPhone(): String {
    if (length <= 7) return this
    return "${take(3)}****${takeLast(4)}"
}
