package com.mygolfswing.app.ui.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PrivacyTip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.launch

/**
 * 账号与安全 / 设置页（对齐 iOS `ProfileSettingsPage`）。
 *
 * 从 V2「我」页「账号与安全」进入；密码修改在子页 [com.mygolfswing.app.ui.auth.V2ChangePasswordScreen]。
 */
@Composable
fun V2AccountSecurityScreen(
    onBack: () -> Unit,
    onOpenChangePassword: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenCloudAudit: () -> Unit,
    onOpenAccountDeletion: () -> Unit,
    onLogoutComplete: () -> Unit,
) {
    val profile by V2ProfileStore.profile.collectAsState()
    var confirmLogout by remember { mutableStateOf(false) }
    var showNicknameEditor by remember { mutableStateOf(false) }
    var showPhoneEditor by remember { mutableStateOf(false) }
    var loggingOut by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isTestUser = V2DeveloperToolsAccess.isCloudAuditEnabled(profile)

    LaunchedEffect(Unit) {
        V2ProfileStore.refresh(force = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        V2NavTopBarCentered(title = "设置", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                onEditPhone = { showPhoneEditor = true },
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionCaption("账号与安全")
                SettingsGroup {
                    SettingsActionRow(
                        icon = Icons.Filled.Key,
                        title = "设置登录密码",
                        trailingText = if (profile.hasPassword) "已设置" else "未设置",
                        trailingColor = if (profile.hasPassword) Color(0xFF34C759) else Color(0xFFFF9500),
                        onClick = onOpenChangePassword,
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
                        icon = Icons.Filled.PrivacyTip,
                        title = "隐私政策",
                        onClick = onOpenPrivacy,
                    )
                }
            }

            if (isTestUser) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "开发者工具",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF9500),
                    )
                    SettingsGroup {
                        SettingsActionRow(
                            icon = Icons.Filled.CloudSync,
                            title = "云端一致性",
                            onClick = onOpenCloudAudit,
                        )
                    }
                }
            }

            ActionCard(onClick = { if (!loggingOut) confirmLogout = true }) {
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
                    text = if (loggingOut) "退出中..." else "退出登录",
                    fontSize = 16.sp,
                    color = if (loggingOut) DSColors.shadowGray else DSColors.fairway,
                    modifier = Modifier.weight(1f),
                )
                if (!loggingOut) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = DSColors.shadowGray.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            ActionCard(onClick = onOpenAccountDeletion) {
                SettingsIconBox(icon = Icons.Filled.DeleteOutline, tint = DSColors.flagRed)
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

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("确认退出登录？", style = DSTypography.titleMedium) },
            text = {
                Text(
                    "退出后本地视频与笔记将保留在本机，云端数据不会被删除。",
                    style = DSTypography.body,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    scope.launch {
                        loggingOut = true
                        V2ProfileStore.logoutAndWait()
                        loggingOut = false
                        onLogoutComplete()
                    }
                }) {
                    Text("退出", color = DSColors.flagRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text("取消", color = DSColors.shadowGray)
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
            },
        )
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
                imageVector = Icons.Filled.Person,
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
                icon = Icons.Filled.AlternateEmail,
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
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = DSColors.shadowGray.copy(alpha = 0.7f),
                modifier = Modifier.size(12.dp),
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
        SettingsIconBox(icon = icon)
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
private fun SettingsIconBox(icon: ImageVector, tint: Color = DSColors.fairway) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DSColors.mistGreen),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
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
