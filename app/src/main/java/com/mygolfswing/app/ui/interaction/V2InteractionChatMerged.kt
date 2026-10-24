package com.mygolfswing.app.ui.interaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.friend.V2IMMessage
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors

/** Read-only detail for Tencent merged-forward messages, matching iOS ChatUIKit. */
@Composable
internal fun V2MergedMessageDetailScreen(
    title: String,
    messages: List<V2IMMessage>,
    loading: Boolean,
    error: String?,
    ownAvatar: String?,
    ownName: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenMessage: (V2IMMessage) -> Unit,
) {
    V2BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize().v2StatusBarPadding()) {
            Box(
                Modifier.fillMaxWidth().height(56.dp).background(Color.White.copy(alpha = 0.94f))
                    .testTag("merged-message-header"),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onBack)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = DSColors.deepGreen,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    text = title.ifBlank { "聊天记录" },
                    color = DSColors.deepGreen,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 64.dp)
                        .testTag("merged-message-title"),
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.06f)))

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DSColors.fairway)
                }
                error != null -> Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(error, color = DSColors.shadowGray, fontSize = 14.sp)
                    Text(
                        "重试",
                        color = DSColors.fairway,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp).clickable(onClick = onRetry)
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
                messages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无消息", color = DSColors.shadowGray, fontSize = 15.sp)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                ) {
                    itemsIndexed(messages, key = { _, message -> message.messageId }) { index, message ->
                        V2InteractionChatMessageRow(
                            message = message,
                            timeSeparator = chatTimeSeparator(messages, index),
                            peerAvatar = message.senderAvatar,
                            peerName = message.senderName ?: message.senderUserId,
                            ownAvatar = ownAvatar,
                            ownName = ownName,
                            onOpenMedia = onOpenMessage,
                            actionsEnabled = false,
                            reciprocalBubbleBackground = Color(0xFFF2F3F5),
                        )
                    }
                }
            }
        }
    }
}
