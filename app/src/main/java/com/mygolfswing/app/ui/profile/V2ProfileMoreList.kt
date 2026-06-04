package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing

@Composable
fun V2ProfileMoreList(
    modifier: Modifier = Modifier,
    onStorageTap: () -> Unit = {},
    onAIConfigTap: () -> Unit = {},
    onFeedbackTap: () -> Unit = {},
    onUserAgreementTap: () -> Unit = {},
    onPrivacyPolicyTap: () -> Unit = {},
    onAccountSecurityTap: () -> Unit = {},
    onLogoutTap: () -> Unit = {},
    onCloudAuditTap: () -> Unit = {},
    isLoggedIn: Boolean = true,
    isTestUser: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MoreFunctionsCard(
            onStorageTap = onStorageTap,
            onAIConfigTap = onAIConfigTap,
            onFeedbackTap = onFeedbackTap,
            onUserAgreementTap = onUserAgreementTap,
            onPrivacyPolicyTap = onPrivacyPolicyTap,
        )
        AccountSecurityCard(onTap = onAccountSecurityTap)
        if (isTestUser) {
            DeveloperToolsCard(onCloudAuditTap = onCloudAuditTap)
        }
        if (isLoggedIn) {
            LogoutCard(onTap = onLogoutTap)
        } else {
            LoginEntryCard(onTap = onLogoutTap)
        }
    }
}

@Composable
private fun DeveloperToolsCard(onCloudAuditTap: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(DSColors.cardWhite),
    ) {
        Text(
            text = "开发者工具",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF39C12),
            modifier = Modifier.padding(horizontal = DSSpacing.lg, vertical = DSSpacing.lg),
        )
        Row(modifier = Modifier.clickable { onCloudAuditTap() }) {
            Item(
                icon = Icons.Filled.CloudSync,
                bg = DSColors.fairway,
                title = "云端一致性",
                subtitle = "检查本机与 dataSync 数据差异",
                showDivider = false,
            )
        }
    }
}

@Composable
private fun MoreFunctionsCard(
    onStorageTap: () -> Unit,
    onAIConfigTap: () -> Unit,
    onFeedbackTap: () -> Unit,
    onUserAgreementTap: () -> Unit,
    onPrivacyPolicyTap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(DSColors.cardWhite),
    ) {
        Text(
            text = "更多功能",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DSColors.inkGreen,
            modifier = Modifier.padding(horizontal = DSSpacing.lg, vertical = DSSpacing.lg),
        )

        Row(
            modifier = Modifier
                .clickable { onStorageTap() },
        ) {
            Item(icon = Icons.Filled.SdStorage, bg = Color(0xFF9B59B6), title = "存储空间", subtitle = "管理本地缓存", showDivider = true)
        }
        Row(modifier = Modifier.clickable { onAIConfigTap() }) {
            Item(icon = Icons.Filled.Memory, bg = DSColors.fairway, title = "AI 接口配置", subtitle = "挥呗 AI", showDivider = true)
        }
        Row(modifier = Modifier.clickable { onFeedbackTap() }) {
            Item(icon = Icons.Filled.ChatBubbleOutline, bg = Color(0xFFF39C12), title = "意见反馈", subtitle = "帮助我们做得更好", showDivider = true)
        }
        Row(modifier = Modifier.clickable { onUserAgreementTap() }) {
            Item(icon = Icons.Filled.Description, bg = Color(0xFF8E8E93), title = "用户协议", subtitle = "了解服务条款", showDivider = true)
        }
        Row(modifier = Modifier.clickable { onPrivacyPolicyTap() }) {
            Item(icon = Icons.Filled.PrivacyTip, bg = Color(0xFF3D5A80), title = "隐私政策", subtitle = "数据安全与隐私保护", showDivider = false)
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
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DSSpacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
                Text(text = subtitle, fontSize = 11.sp, color = DSColors.shadowGray)
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = DSColors.shadowGray.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp),
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .padding(start = 64.dp)
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(Color(0x10000000)),
            )
        }
    }
}

@Composable
private fun AccountSecurityCard(onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .clickable { onTap() }
            .padding(horizontal = DSSpacing.lg, vertical = 18.dp),
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
                modifier = Modifier.size(18.dp),
            )
        }
        Text(text = "账号与安全", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen, modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = DSColors.shadowGray.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun LogoutCard(onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .clickable { onTap() }
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "退出登录", color = DSColors.flagRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LoginEntryCard(onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.fairway)
            .clickable { onTap() }
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "立即登录 / 注册", color = DSColors.warmWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
