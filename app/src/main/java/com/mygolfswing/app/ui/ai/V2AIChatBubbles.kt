package com.mygolfswing.app.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2ChatMessage
import com.mygolfswing.app.ui.designsystem.DSColors
import java.time.format.DateTimeFormatter

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun V2AIChatBubbleAssistant(message: V2ChatMessage, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AIAvatar()

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFF6EFD8), Color(0xFFF4ECDA)),
                        ),
                    )
                    .border(width = 0.5.dp, color = DSColors.goldBright.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                val display = if (message.text.isEmpty() && message.isStreaming) "…" else message.text
                Text(
                    text = display + if (message.isStreaming && message.text.isNotEmpty()) " ▍" else "",
                    fontSize = 14.sp,
                    color = DSColors.inkGreen,
                    lineHeight = 20.sp,
                )
            }
            Text(
                text = timeFmt.format(message.createdAt),
                fontSize = 10.sp,
                color = DSColors.shadowGray,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Spacer(modifier = Modifier.widthIn(min = 40.dp))
    }
}

@Composable
fun V2AIChatBubbleUser(message: V2ChatMessage, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Spacer(modifier = Modifier.widthIn(min = 60.dp))
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DSColors.fairway)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(text = message.text, fontSize = 14.sp, color = Color.White, lineHeight = 20.sp)
            }
            Text(
                text = timeFmt.format(message.createdAt),
                fontSize = 10.sp,
                color = DSColors.shadowGray,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
    }
}

@Composable
private fun AIAvatar() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(DSColors.fairway, DSColors.leafGreen)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "AI", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}
