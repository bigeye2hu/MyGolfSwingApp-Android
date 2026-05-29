package com.mygolfswing.app.ui.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.store.V2AIChatStore
import com.mygolfswing.app.ui.designsystem.DSColors
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2AIHistorySheet(onDismiss: () -> Unit) {
    val sessions by V2AIChatStore.sessions.collectAsState()
    val current by V2AIChatStore.currentSession.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "对话历史",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = DSColors.inkGreen,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        LazyColumn {
            item {
                SessionRow("新建对话", selected = false) {
                    V2AIChatStore.startNewSession()
                    onDismiss()
                }
            }
            item {
                SessionRow("当前：${current.title}", selected = true) {}
            }
            items(sessions, key = { it.id }) { s ->
                SessionRow(s.title, selected = false) {
                    V2AIChatStore.switchSession(s.id)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun SessionRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        title,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        color = if (selected) DSColors.fairway else DSColors.inkGreen,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
    )
}
