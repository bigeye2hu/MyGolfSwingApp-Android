package com.mygolfswing.app.ui.profile

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors

private const val NotificationPreferences = "v2_notification_privacy"
private const val DirectMessageKey = "v2_notify_direct_message"
private const val PracticeReplyKey = "v2_notify_practice_reply"
private const val PublicMentionKey = "v2_notify_public_mention"
private const val AchievementKey = "v2_notify_achievement"

/** Android counterpart of iOS `V2NotificationPrivacyView`. */
@Composable
internal fun V2NotificationPrivacyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(NotificationPreferences, Context.MODE_PRIVATE)
    }

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            V2NavTopBarCentered(title = "通知与隐私", onBack = onBack)
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .v2NavigationBarPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SectionLabel("通知")
                NotificationToggleRow(
                    title = "新私密消息",
                    detail = "普通对话",
                    initialValue = preferences.getBoolean(DirectMessageKey, true),
                    onValueChange = { preferences.edit().putBoolean(DirectMessageKey, it).apply() },
                )
                NotificationToggleRow(
                    title = "练习讨论",
                    detail = "某条内容下的新回复",
                    initialValue = preferences.getBoolean(PracticeReplyKey, true),
                    onValueChange = { preferences.edit().putBoolean(PracticeReplyKey, it).apply() },
                )
                NotificationToggleRow(
                    title = "公开提及",
                    detail = "公开评论里的 @",
                    initialValue = preferences.getBoolean(PublicMentionKey, true),
                    onValueChange = { preferences.edit().putBoolean(PublicMentionKey, it).apply() },
                )
                NotificationToggleRow(
                    title = "成果展示正文",
                    detail = "不并入上面三类未读",
                    initialValue = preferences.getBoolean(AchievementKey, false),
                    onValueChange = { preferences.edit().putBoolean(AchievementKey, it).apply() },
                )

                SectionLabel("隐私", Modifier.padding(top = 8.dp))
                NotificationInfoRow("连接与权限", "私密空间不能通过 @ 开放给第三个人")
                NotificationInfoRow("默认只保留你能分享的内容", "请教另一位教练时，不携带其他空间的回复")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = DSColors.shadowGray,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}

@Composable
private fun NotificationToggleRow(
    title: String,
    detail: String,
    initialValue: Boolean,
    onValueChange: (Boolean) -> Unit,
) {
    var checked by remember(initialValue) { mutableStateOf(initialValue) }
    Row(
        modifier = Modifier.fillMaxWidth().background(DSColors.cardWhite, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = DSColors.shadowGray, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                onValueChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = DSColors.cardWhite,
                checkedTrackColor = DSColors.fairway,
                uncheckedThumbColor = DSColors.cardWhite,
                // SwiftUI `Toggle.tint` only colors the active track; the inactive
                // track keeps the native iOS neutral gray rather than a brand tint.
                uncheckedTrackColor = Color(0xFFE9E9EA),
                uncheckedBorderColor = Color.Transparent,
            ),
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

@Composable
private fun NotificationInfoRow(title: String, detail: String) {
    Column(
        modifier = Modifier.fillMaxWidth().background(DSColors.cardWhite, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = DSColors.shadowGray, fontSize = 12.sp)
    }
}
