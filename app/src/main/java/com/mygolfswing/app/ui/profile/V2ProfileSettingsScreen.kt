package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography

/**
 * V2「我」页右上角精简设置页，对齐 iOS `V2ProfileSettingsView`。
 */
@Composable
fun V2ProfileSettingsScreen(
    onBack: () -> Unit,
    onRequestLogin: () -> Unit,
) {
    val profile by V2ProfileStore.profile.collectAsState()
    var showQrDialog by remember { mutableStateOf(false) }

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
                .padding(horizontal = DSSpacing.lg)
                .padding(top = DSSpacing.md, bottom = DSSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(DSSpacing.md),
        ) {
            SettingsCard(
                icon = Icons.Filled.QrCode,
                iconBg = DSColors.fairway,
                title = "我的二维码",
                subtitle = "分享给球友加好友",
                enabled = profile.isLoggedIn,
                onClick = {
                    if (profile.isLoggedIn) showQrDialog = true else onRequestLogin()
                },
            )
            SettingsCard(
                icon = Icons.Filled.Notifications,
                iconBg = Color(0xFFF39C12),
                title = "通知设置",
                subtitle = "敬请期待",
                enabled = false,
                onClick = {},
            )
            AboutCard()
        }
    }

    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("我的二维码", style = DSTypography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = profile.nickname.ifBlank { "高尔夫球友" },
                        style = DSTypography.bodyBold,
                        color = DSColors.inkGreen,
                    )
                    Text(
                        text = profile.username,
                        style = DSTypography.caption,
                        color = DSColors.shadowGray,
                    )
                    Text(
                        text = "二维码名片功能会和好友体系一起补齐；当前先保留与 iOS 一致的设置入口。",
                        style = DSTypography.body,
                        color = DSColors.shadowGray,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text("知道了", color = DSColors.fairway)
                }
            },
        )
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.62f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = if (enabled) 8.dp else 0.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite.copy(alpha = if (enabled) 1f else 0.68f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = DSSpacing.lg)
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DSSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg.copy(alpha = alpha)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = alpha),
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.inkGreen.copy(alpha = alpha),
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = DSColors.shadowGray.copy(alpha = alpha),
            )
        }
        if (enabled) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = DSColors.shadowGray.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    val packageInfo = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionText = remember(packageInfo) {
        val versionName = packageInfo.versionName ?: "1.0"
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        "v$versionName ($versionCode)"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .padding(vertical = DSSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = DSColors.fairway,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "挥呗",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DSColors.fairway,
        )
        Text(
            text = "Where Every Swing Matters",
            fontSize = 11.sp,
            color = DSColors.shadowGray,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = versionText,
            fontSize = 11.sp,
            color = DSColors.shadowGray.copy(alpha = 0.7f),
        )
    }
}
