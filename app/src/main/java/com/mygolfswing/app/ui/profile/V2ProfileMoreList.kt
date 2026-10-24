package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing

private fun Modifier.profileListCardShadow(shape: RoundedCornerShape): Modifier = shadow(
    elevation = 8.dp,
    shape = shape,
    ambientColor = DSColors.fairway.copy(alpha = 0.08f),
    spotColor = DSColors.fairway.copy(alpha = 0.08f),
)

@Composable
fun V2ProfileMoreList(
    modifier: Modifier = Modifier,
    accountSecuritySubtitle: String = "",
    onStorageTap: () -> Unit = {},
    onResetCreditsTap: () -> Unit = {},
    onAIConfigTap: () -> Unit = {},
    onFeedbackTap: () -> Unit = {},
    onUserAgreementTap: () -> Unit = {},
    onPrivacyPolicyTap: () -> Unit = {},
    onAccountSecurityTap: () -> Unit = {},
    onLogoutTap: () -> Unit = {},
    isLoggedIn: Boolean = true,
    showsRoleSwitcher: Boolean = false,
    accountActionsDisabled: Boolean = false,
    onRoleSwitchTap: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MoreFunctionsCard(
            onStorageTap = onStorageTap,
            onResetCreditsTap = onResetCreditsTap,
            onAIConfigTap = onAIConfigTap,
            onFeedbackTap = onFeedbackTap,
            onUserAgreementTap = onUserAgreementTap,
            onPrivacyPolicyTap = onPrivacyPolicyTap,
        )
        AccountSecurityCard(subtitle = accountSecuritySubtitle, onTap = onAccountSecurityTap)
        if (showsRoleSwitcher) {
            RoleSwitcherCard(
                accountActionsDisabled = accountActionsDisabled,
                onTap = onRoleSwitchTap,
            )
        }
        if (isLoggedIn) {
            LogoutCard(accountActionsDisabled = accountActionsDisabled, onTap = onLogoutTap)
        } else {
            LoginEntryCard(onTap = onLogoutTap)
        }
    }
}

@Composable
internal fun RoleSwitcherCard(
    accountActionsDisabled: Boolean,
    onTap: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .profileListCardShadow(RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.goldBright.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .clickable(enabled = !accountActionsDisabled, onClick = onTap)
            .alpha(if (accountActionsDisabled) 0.62f else 1f)
            .padding(horizontal = DSSpacing.lg)
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DSSpacing.md),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(DSColors.deepGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.SwapHoriz, null, tint = DSColors.goldBright, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("切换身份", color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                if (accountActionsDisabled) "视频发送完成后即可切换" else "选择练习者版或教练版",
                color = DSColors.shadowGray,
                fontSize = 11.sp,
            )
        }
        Text(
            "双身份",
            color = DSColors.fairway,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(DSColors.mistGreen)
                .padding(horizontal = 8.dp, vertical = 5.dp),
        )
        Icon(Icons.Filled.ChevronRight, null, tint = DSColors.shadowGray.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
    }
}

@Composable
private fun MoreFunctionsCard(
    onStorageTap: () -> Unit,
    onResetCreditsTap: () -> Unit,
    onAIConfigTap: () -> Unit,
    onFeedbackTap: () -> Unit,
    onUserAgreementTap: () -> Unit,
    onPrivacyPolicyTap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .profileListCardShadow(RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(DSColors.cardWhite),
    ) {
        Text(
            text = "更多功能",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DSColors.inkGreen,
            modifier = Modifier
                .padding(horizontal = DSSpacing.lg)
                .padding(top = DSSpacing.lg, bottom = DSSpacing.md),
        )

        Row(
            modifier = Modifier
                .clickable { onStorageTap() },
        ) {
            Item(
                icon = Icons.Filled.Storage,
                bg = Color(0xFF9B59B6),
                title = "存储空间",
                subtitle = "管理本地缓存",
                showDivider = true,
                iconStyle = V2ProfileMenuIconStyle.INTERNAL_DRIVE,
            )
        }
        Row(modifier = Modifier.clickable { onResetCreditsTap() }) {
            Item(
                icon = Icons.Filled.Refresh,
                bg = DSColors.deepGreen,
                title = "积分重置",
                subtitle = "校准余额与本地缓存",
                showDivider = true,
                iconStyle = V2ProfileMenuIconStyle.FILLED_CIRCLE_REFRESH,
            )
        }
        Row(modifier = Modifier.clickable { onAIConfigTap() }) {
            Item(icon = Icons.Filled.Memory, bg = DSColors.fairway, title = "AI 接口配置", subtitle = "挥呗 AI", showDivider = true)
        }
        Row(modifier = Modifier.clickable { onFeedbackTap() }) {
            Item(icon = Icons.Filled.Forum, bg = Color(0xFFF39C12), title = "意见反馈", subtitle = "帮助我们做得更好", showDivider = true)
        }
        Row(modifier = Modifier.clickable { onUserAgreementTap() }) {
            Item(icon = Icons.Filled.Description, bg = Color(0xFF8E8E93), title = "用户协议", subtitle = "了解服务条款", showDivider = true)
        }
        Row(modifier = Modifier.clickable { onPrivacyPolicyTap() }) {
            Item(
                icon = Icons.Filled.Shield,
                bg = Color(0xFF3D5A80),
                title = "隐私政策",
                subtitle = "数据安全与隐私保护",
                showDivider = false,
                iconStyle = V2ProfileMenuIconStyle.LOCK_SHIELD,
            )
        }
    }
}

@Composable
private fun Item(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    title: String,
    subtitle: String,
    showDivider: Boolean,
    iconStyle: V2ProfileMenuIconStyle = V2ProfileMenuIconStyle.STANDARD,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DSSpacing.md),
        ) {
            V2ProfileMenuIconTile(icon = icon, background = bg, style = iconStyle)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
                Text(text = subtitle, fontSize = 12.sp, color = DSColors.shadowGray)
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = DSColors.shadowGray.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp),
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .padding(start = DSSpacing.lg + 40.dp + DSSpacing.md)
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(Color(0xFFF0F7F4)),
            )
        }
    }
}

@Composable
private fun AccountSecurityCard(subtitle: String, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .profileListCardShadow(RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .clickable { onTap() }
            .padding(horizontal = DSSpacing.lg)
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DSSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DSColors.mistGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = DSColors.fairway,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "账号与安全", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = DSColors.shadowGray,
                    maxLines = 1,
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = DSColors.shadowGray.copy(alpha = 0.7f),
            modifier = Modifier.size(13.dp),
        )
    }
}

@Composable
private fun LogoutCard(accountActionsDisabled: Boolean, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .profileListCardShadow(RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .clickable(enabled = !accountActionsDisabled) { onTap() }
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (accountActionsDisabled) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = DSColors.shadowGray,
                strokeWidth = 2.dp,
            )
        }
        Text(
            text = if (accountActionsDisabled) "内容发送中，暂不可退出" else "退出登录",
            color = if (accountActionsDisabled) DSColors.shadowGray else DSColors.flagRed,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = if (accountActionsDisabled) 8.dp else 0.dp),
        )
    }
}

@Composable
private fun LoginEntryCard(onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .profileListCardShadow(RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.fairway)
            .clickable { onTap() }
            .height(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "立即登录 / 注册", color = DSColors.warmWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
