package com.mygolfswing.app.ui.tabs

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygolfswing.app.ui.designsystem.DSColors

internal enum class V2RootAuthPrompt(
    val title: String,
    val message: String,
) {
    MEMO(
        title = "创建 Memo 需要先登录",
        message = "登录后可以查看、创建和管理自己的 Memo，练习记录会同步保存到账号里。",
    ),
    INTERACTION(
        title = "账号功能需要先登录",
        message = "登录后可以进入挥呗连接空间，与教练共享练习视频和课后总结。",
    ),
    PROFILE(
        title = "管理个人资料需要先登录",
        message = "登录后可以查看个人主页、训练档案、积分和账号设置。",
    ),
}

internal fun protectedTabAuthPrompt(tab: V2Tab, isLoggedIn: Boolean): V2RootAuthPrompt? {
    if (isLoggedIn) return null
    return when (tab) {
        V2Tab.HOME, V2Tab.PLAZA -> null
        V2Tab.MEMO -> V2RootAuthPrompt.MEMO
        V2Tab.INTERACTION -> V2RootAuthPrompt.INTERACTION
        V2Tab.ME -> V2RootAuthPrompt.PROFILE
    }
}

@Composable
internal fun V2RootAuthPromptDialog(
    prompt: V2RootAuthPrompt,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f))
                .clickable(onClick = onCancel).testTag("root-auth-prompt-scrim"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().widthIn(max = 350.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = Color.Black.copy(alpha = 0.24f),
                        spotColor = Color.Black.copy(alpha = 0.24f),
                    )
                    .clip(RoundedCornerShape(26.dp)).background(DSColors.cardWhite)
                    .border(1.dp, DSColors.fairway.copy(alpha = 0.16f), RoundedCornerShape(26.dp))
                    .clickable(onClick = ::consumeRootAuthPromptCardTap)
                    .padding(18.dp).testTag("root-auth-prompt"),
            ) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    V2RootAuthPromptIcon(prompt)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            prompt.title,
                            color = DSColors.inkGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            "登录后解锁完整体验",
                            color = DSColors.fairway,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(DSColors.mistGreen)
                            .clickable(onClick = onCancel).testTag("root-auth-prompt-close"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, "关闭", tint = DSColors.shadowGray, modifier = Modifier.size(13.dp))
                    }
                }

                Text(
                    prompt.message,
                    color = DSColors.shadowGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    V2RootAuthBenefit(Icons.Filled.CloudUpload, "云端同步", Modifier.weight(1f))
                    V2RootAuthBenefit(Icons.Filled.Security, "安全保存", Modifier.weight(1f))
                    V2RootAuthBenefit(Icons.Filled.SportsGolf, "练习档案", Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp))
                            .background(DSColors.mistGreen).clickable(onClick = onCancel)
                            .testTag("root-auth-prompt-cancel"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("再逛逛", color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp))
                            .background(DSColors.fairway).clickable(onClick = onConfirm)
                            .testTag("root-auth-prompt-confirm"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Filled.Key, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("去登录", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

private fun consumeRootAuthPromptCardTap() = Unit

@Composable
private fun V2RootAuthPromptIcon(prompt: V2RootAuthPrompt) {
    val icon = when (prompt) {
        V2RootAuthPrompt.MEMO -> Icons.Filled.Layers
        V2RootAuthPrompt.INTERACTION -> Icons.Filled.Tune
        V2RootAuthPrompt.PROFILE -> Icons.Filled.Person
    }
    Box(Modifier.size(50.dp)) {
        Box(
            modifier = Modifier.fillMaxSize().clip(CircleShape)
                .background(DSColors.fairway.copy(alpha = 0.14f))
                .border(1.dp, DSColors.fairway.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(19.dp))
        }
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp).size(17.dp)
                .clip(CircleShape).background(DSColors.fairway).border(1.4.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.size(7.dp))
        }
    }
}

@Composable
private fun V2RootAuthBenefit(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(30.dp).clip(CircleShape).background(DSColors.mistGreen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(10.dp))
        Spacer(Modifier.size(5.dp))
        Text(
            text,
            color = DSColors.fairway,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
