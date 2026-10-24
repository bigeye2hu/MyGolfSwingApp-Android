package com.mygolfswing.app.ui.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.repo.V2AppUpdateKind
import com.mygolfswing.app.data.repo.V2AppUpdatePresentation
import com.mygolfswing.app.ui.designsystem.DSColors

/** Current iOS `AppUpdatePromptView` hierarchy, with the Android store label retained. */
@Composable
fun V2AppUpdatePrompt(
    presentation: V2AppUpdatePresentation,
    onUpdate: () -> Unit,
    onRemindLater: () -> Unit,
    onContinueLocally: () -> Unit,
) {
    val required = presentation.kind == V2AppUpdateKind.REQUIRED
    BackHandler { if (!required) onRemindLater() }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f))
            .pointerInput(Unit) { detectTapGestures(onTap = { consumeUpdateScrimClick() }) }
            .semantics {
                paneTitle = "应用更新"
                isTraversalGroup = true
            }
            .testTag("app-update-prompt"),
        contentAlignment = Alignment.Center,
    ) {
        val cardShape = RoundedCornerShape(24.dp)
        Column(
            modifier = Modifier.padding(horizontal = 28.dp).widthIn(max = 430.dp).fillMaxWidth()
                .shadow(
                    elevation = 24.dp,
                    shape = cardShape,
                    ambientColor = Color.Black.copy(alpha = 0.20f),
                    spotColor = Color.Black.copy(alpha = 0.20f),
                )
                .clip(cardShape).background(DSColors.cardWhite)
                .border(1.dp, DSColors.fairway.copy(alpha = 0.10f), cardShape),
        ) {
            UpdateArtwork(required = required)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)
                    .padding(top = 18.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        presentation.policy.title,
                        color = DSColors.inkGreen,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "v${presentation.policy.latestVersion}",
                        color = DSColors.fairway,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(CircleShape).background(DSColors.mistGreen)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Text(
                    presentation.policy.message,
                    color = DSColors.shadowGray,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (presentation.policy.releaseNotes.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(DSColors.sandBeige.copy(alpha = 0.65f)).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            null,
                            tint = DSColors.trophyGold,
                            modifier = Modifier.padding(top = 2.dp).size(16.dp),
                        )
                        Text(
                            presentation.policy.releaseNotes,
                            color = DSColors.inkGreen.copy(alpha = 0.82f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Button(
                    onClick = onUpdate,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("app-update-primary"),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                ) {
                    Icon(Icons.Filled.DownloadForOffline, null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("前往 Google Play 更新", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                UpdateSecondaryAction(
                    required = required,
                    allowLocalAccess = presentation.policy.allowLocalAccess,
                    onRemindLater = onRemindLater,
                    onContinueLocally = onContinueLocally,
                )
            }
        }
    }
}

/** Consumes modal-scrim taps so they cannot reach the page underneath. */
private fun consumeUpdateScrimClick() = Unit

@Composable
private fun UpdateArtwork(required: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().height(112.dp)
            .background(Brush.linearGradient(listOf(DSColors.deepGreen, DSColors.fairway))),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(106.dp).offset(x = 112.dp, y = (-28).dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.12f)),
        )
        Icon(
            if (required) Icons.Filled.Shield else Icons.Filled.SportsGolf,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(42.dp),
        )
    }
}

@Composable
private fun UpdateSecondaryAction(
    required: Boolean,
    allowLocalAccess: Boolean,
    onRemindLater: () -> Unit,
    onContinueLocally: () -> Unit,
) {
    when {
        required && allowLocalAccess -> TextButton(
            onClick = onContinueLocally,
            modifier = Modifier.height(30.dp).testTag("app-update-local-access"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text("暂时只查看本地内容", color = DSColors.shadowGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        required -> Text(
            "此版本已无法安全连接在线服务，请先完成更新",
            color = DSColors.flagRed,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("app-update-required-message"),
        )
        else -> TextButton(
            onClick = onRemindLater,
            modifier = Modifier.height(30.dp).testTag("app-update-remind-later"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text("稍后提醒我", color = DSColors.shadowGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** iOS' required-update local-only shell: online areas are replaced by the local Memo surface. */
@Composable
fun V2LocalAccessContent(
    onUpdate: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(DSColors.warmWhite).testTag("app-update-local-content")) {
        Row(
            modifier = Modifier.fillMaxWidth().background(DSColors.sandBeige).statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Shield, null, tint = DSColors.trophyGold, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("当前为基础使用模式", color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "本地 Memo 与视频仍可查看，更新后恢复全部在线功能",
                    color = DSColors.shadowGray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
            TextButton(onClick = onUpdate, modifier = Modifier.testTag("app-update-return")) {
                Text("去更新", color = DSColors.fairway, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}
