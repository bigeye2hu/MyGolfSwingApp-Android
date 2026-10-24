package com.mygolfswing.app.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.ui.designsystem.DSColors

@Composable
internal fun V2KickOutAlertDialog(
    message: String,
    onConfirm: () -> Unit,
) {
    val accountDeleted = message.contains("注销") || message.contains("删除")
    val title = if (accountDeleted) "账号已注销" else "账号已在其他设备登录"
    val detail = message.ifBlank { "您的账号已在其他设备上登录，请重新登录" }
    val bottomTip = if (accountDeleted) "如有疑问，请联系客服" else "为保护账号安全，同一账号仅支持单设备登录"
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.60f))
                .testTag("kick-out-alert-scrim"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().widthIn(max = 320.dp)
                    .shadow(20.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.30f))
                    .clip(RoundedCornerShape(24.dp)).background(DSColors.cardWhite)
                    .border(1.dp, DSColors.fairway.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                    .padding(start = 25.dp, top = 25.dp, end = 25.dp, bottom = 25.dp)
                    .testTag("kick-out-alert"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(
                        Brush.linearGradient(
                            listOf(DSColors.mistGreen, DSColors.fairway.copy(alpha = 0.18f)),
                        ),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (accountDeleted) Icons.Filled.PersonOff else Icons.Filled.PersonRemove,
                        contentDescription = null,
                        tint = if (accountDeleted) DSColors.flagRed else DSColors.fairway,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    title,
                    color = DSColors.inkGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                )
                Text(
                    detail,
                    color = DSColors.shadowGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Shield, null, tint = DSColors.fairway, modifier = Modifier.size(12.dp))
                    Text(bottomTip, color = DSColors.shadowGray, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(48.dp)
                        .clip(CircleShape).background(DSColors.fairway)
                        .clickable(onClick = onConfirm).testTag("kick-out-alert-confirm"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (accountDeleted) "返回首页" else "我知道了",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
