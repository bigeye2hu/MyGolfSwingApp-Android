package com.mygolfswing.app.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2AIMessageRole
import com.mygolfswing.app.data.store.V2AIChatStore
import com.mygolfswing.app.ui.designsystem.DSColors

/**
 * V2 AI 对话主页（对齐 iOS `V2AIChatView`）。
 */
@Composable
fun V2AIChatScreen(
    onSwitchToHome: () -> Unit = {},
    onOpenConfig: () -> Unit = {},
) {
    val session by V2AIChatStore.currentSession.collectAsState()
    val status by V2AIChatStore.statusBar.collectAsState()
    var draft by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    LaunchedEffect(session.messages.size, session.messages.lastOrNull()?.text?.length) {
        if (session.messages.isNotEmpty()) {
            listState.animateScrollToItem(session.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        AIChatTopBar(
            modelLabel = "${V2AIChatStore.provider.providerName} · ${V2AIChatStore.provider.modelName}",
            onNew = { V2AIChatStore.startNewSession() },
            onHistory = { showHistory = true },
            onConfig = onOpenConfig,
        )

        ProviderPill(provider = V2AIChatStore.provider.providerName, model = V2AIChatStore.provider.modelName)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            items(session.messages, key = { it.id }) { msg ->
                when (msg.role) {
                    V2AIMessageRole.ASSISTANT -> V2AIChatBubbleAssistant(message = msg)
                    V2AIMessageRole.USER -> V2AIChatBubbleUser(message = msg)
                    V2AIMessageRole.SYSTEM -> Unit
                }
            }
        }

        status?.let { StatusBar(text = it) }

        V2AIChatInputBar(
            draft = draft,
            onDraftChange = { draft = it },
            onSend = {
                val text = draft
                draft = ""
                V2AIChatStore.send(text)
            },
            isReplying = V2AIChatStore.isReplying,
            onStop = { V2AIChatStore.stopStreaming() },
        )
    }
    if (showHistory) {
        V2AIHistorySheet(onDismiss = { showHistory = false })
    }
}

@Composable
private fun AIChatTopBar(
    modelLabel: String,
    onNew: () -> Unit,
    onHistory: () -> Unit,
    onConfig: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.cardWhite.copy(alpha = 0.95f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconBtn(icon = Icons.Filled.Add, onClick = onNew, tint = DSColors.fairway)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("AI 教练", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DSColors.inkGreen)
            Text(modelLabel, fontSize = 10.sp, color = DSColors.shadowGray)
        }
        CircleIconBtn(icon = Icons.Filled.HistoryEdu, onClick = onHistory, tint = DSColors.fairway)
        CircleIconBtn(icon = Icons.Filled.Settings, onClick = onConfig, tint = DSColors.fairway)
    }
}

@Composable
private fun CircleIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.10f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ProviderPill(provider: String, model: String) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(DSColors.mistGreen)
                .border(width = 0.5.dp, color = DSColors.fairway.copy(alpha = 0.15f), shape = RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(DSColors.leafGreen),
            )
            Text(text = provider, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DSColors.fairway)
            Text(text = "·", fontSize = 10.sp, color = DSColors.fairway.copy(alpha = 0.4f))
            Text(text = model, fontSize = 10.sp, color = DSColors.fairway.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun StatusBar(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.cardWhite.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(DSColors.trophyGold),
        )
        Text(text = text, fontSize = 11.sp, color = DSColors.shadowGray)
    }
}
