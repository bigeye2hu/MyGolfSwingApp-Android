package com.mygolfswing.app.ui.ai

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Forum as ForumOutlined
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2AIMessageRole
import com.mygolfswing.app.data.store.V2AIChatStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import java.util.concurrent.atomic.AtomicBoolean

/**
 * V2 AI 对话主页（对齐 iOS `V2AIChatView`）。
 */
@Composable
fun V2AIChatScreen(
    onSwitchToHome: () -> Unit = {},
    onOpenConfig: () -> Unit = {},
    onRequestLogin: () -> Unit = {},
    onRequestLoginForHistory: () -> Unit = { onRequestLogin() },
    historyRequestKey: Int = 0,
) {
    val session by V2AIChatStore.currentSession.collectAsState()
    val status by V2AIChatStore.statusBar.collectAsState()
    val providerName by V2AIChatStore.providerName.collectAsState()
    val modelName by V2AIChatStore.modelName.collectAsState()
    val modelDisplay by V2AIChatStore.modelDisplay.collectAsState()
    val requestOpenConfig by V2AIChatStore.requestOpenConfig.collectAsState()
    val draft by V2AIChatStore.draft.collectAsState()
    val profile by V2ProfileStore.profile.collectAsState()
    var pendingClearDraft by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    val exitReflectionRequested = remember { AtomicBoolean(false) }
    fun triggerReflectionOnce() {
        if (exitReflectionRequested.compareAndSet(false, true)) {
            V2AIChatStore.triggerReflectionIfNeeded()
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(session.messages.size, session.messages.lastOrNull()?.text?.length) {
        if (session.messages.isNotEmpty()) {
            listState.animateScrollToItem(session.messages.size)
        }
    }
    LaunchedEffect(session.messages, pendingClearDraft) {
        val pending = pendingClearDraft ?: return@LaunchedEffect
        val accepted = session.messages.drop(pending.second).any {
            it.role == V2AIMessageRole.USER && it.text == pending.first
        }
        if (accepted) {
            V2AIChatStore.clearDraftIf(pending.first)
            pendingClearDraft = null
        }
    }
    LaunchedEffect(requestOpenConfig) {
        if (requestOpenConfig) {
            onOpenConfig()
            V2AIChatStore.consumeOpenConfigRequest()
        }
    }
    LaunchedEffect(historyRequestKey, profile.isLoggedIn) {
        if (historyRequestKey > 0 && profile.isLoggedIn) {
            showHistory = true
        }
    }
    DisposableEffect(Unit) {
        onDispose { triggerReflectionOnce() }
    }

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        AIChatTopBar(
            modelLabel = modelDisplay,
            isGuest = !profile.isLoggedIn,
            onClose = {
                triggerReflectionOnce()
                onSwitchToHome()
            },
            onHistory = {
                if (profile.isLoggedIn) showHistory = true else onRequestLoginForHistory()
            },
            onConfig = { if (profile.isLoggedIn) onOpenConfig() else onRequestLogin() },
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            item(key = "provider_pill") {
                ProviderPill(
                    provider = providerName,
                    model = modelName,
                )
            }
            if (!profile.isLoggedIn) {
                item(key = "guest_preview") {
                    V2AIChatGuestPreviewCard(
                        onSelectQuestion = V2AIChatStore::updateDraft,
                        onLogin = onRequestLogin,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
                    )
                }
            }
            items(session.messages, key = { it.id }) { msg ->
                when (msg.role) {
                    V2AIMessageRole.ASSISTANT -> V2AIChatBubbleAssistant(message = msg)
                    V2AIMessageRole.USER -> V2AIChatBubbleUser(message = msg)
                    V2AIMessageRole.SYSTEM -> Unit
                }
            }
        }

        status?.let { StatusBar(text = it) }

        if (!profile.isLoggedIn && draft.isNotBlank()) {
            V2AIChatGuestDraftNotice(
                onLogin = onRequestLogin,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }

        V2AIChatInputBar(
            draft = draft,
            onDraftChange = {
                V2AIChatStore.updateDraft(it)
                if (pendingClearDraft != null && it != pendingClearDraft?.first) {
                    pendingClearDraft = null
                }
            },
            onSend = {
                if (!profile.isLoggedIn) {
                    onRequestLogin()
                } else {
                    val text = draft.trim()
                    pendingClearDraft = text to session.messages.size
                    V2AIChatStore.send(text)
                }
            },
            enabled = true,
            placeholder = if (profile.isLoggedIn) "向 AI 教练提问…" else "登录后向 AI 教练提问…",
        )
    }
    }
    if (showHistory) {
        V2AIHistorySheet(onDismiss = { showHistory = false })
    }
}

@Composable
internal fun AIChatTopBar(
    modelLabel: String,
    isGuest: Boolean,
    onClose: () -> Unit,
    onHistory: () -> Unit,
    onConfig: () -> Unit,
) {
    val subtitle = aiChatSubtitle(modelLabel = modelLabel, isGuest = isGuest)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .background(DSColors.warmWhite),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopBarIconButton(icon = Icons.Filled.Close, iconSize = 16.dp, contentDescription = "返回首页", onClick = onClose)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text("AI 教练", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = DSColors.shadowGray,
                    maxLines = 1,
                )
            }
            TopBarIconButton(icon = Icons.Outlined.ForumOutlined, iconSize = 18.dp, contentDescription = "对话记录", onClick = onHistory)
            Spacer(modifier = Modifier.width(12.dp))
            TopBarIconButton(icon = Icons.Filled.Settings, iconSize = 18.dp, contentDescription = "AI 配置", onClick = onConfig)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(DSColors.borderLine),
        )
    }
}

@Composable
private fun TopBarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconSize: androidx.compose.ui.unit.Dp,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = DSColors.shadowGray,
            modifier = Modifier.size(iconSize),
        )
    }
}

internal fun aiChatSubtitle(modelLabel: String, isGuest: Boolean): String {
    val suffix = modelLabel.ifBlank { if (isGuest) "示例问答" else "实时对话" }
    return if (isGuest) {
        "示例问答 · 发送前登录 · $suffix"
    } else {
        "你的专属高尔夫小高 · $suffix"
    }
}

@Composable
internal fun ProviderPill(provider: String, model: String) {
    Row(
        modifier = Modifier
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .testTag("ai-chat-provider-pill")
                .clip(RoundedCornerShape(50))
                .background(DSColors.cardWhite)
                .border(width = 0.5.dp, color = DSColors.borderLine, shape = RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(DSColors.fairway),
            )
            Row {
                Text(text = "$provider: ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
                Text(text = model, fontSize = 12.sp, color = DSColors.shadowGray)
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = DSColors.shadowGray,
                modifier = Modifier.size(9.dp),
            )
        }
    }
}

@Composable
internal fun StatusBar(text: String) {
    val transition = rememberInfiniteTransition(label = "ai_status_dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1_200, easing = LinearEasing)),
        label = "ai_status_phase",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .testTag("ai-chat-status-pill")
                .clip(RoundedCornerShape(50))
                .background(DSColors.goldBright.copy(alpha = 0.08f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.ChatBubble,
                contentDescription = null,
                tint = DSColors.goldBright,
                modifier = Modifier.size(11.dp),
            )
            Text(text = "$text…", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DSColors.inkGreen.copy(alpha = 0.70f))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) { index ->
                    val dotOpacity = statusDotOpacity(phase, index)
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(DSColors.goldBright.copy(alpha = 0.7f * dotOpacity)),
                    )
                }
            }
        }
    }
}

private fun statusDotOpacity(phase: Float, index: Int): Float {
    val offsetPhase = phase + index * 0.33f
    val wave = sin(offsetPhase * PI.toFloat() * 2f)
    return 0.3f + 0.7f * max(0f, wave)
}
