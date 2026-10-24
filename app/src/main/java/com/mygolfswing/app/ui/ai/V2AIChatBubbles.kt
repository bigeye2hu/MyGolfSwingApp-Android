package com.mygolfswing.app.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2ChatAttachment
import com.mygolfswing.app.data.model.V2ChatMessage
import com.mygolfswing.app.data.model.V2ChatThumbnailKind
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
            if (message.text.isNotEmpty() || message.isStreaming) {
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
                        .testTag("ai-assistant-text-bubble")
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
            }
            message.attachments.forEach { attachment ->
                V2AIChatAttachmentView(attachment = attachment)
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
private fun V2AIChatAttachmentView(attachment: V2ChatAttachment) {
    when (attachment) {
        is V2ChatAttachment.VideoRef -> V2AIChatVideoRefCard(attachment)
        is V2ChatAttachment.MemoSaved -> V2AIChatMemoSavedCard(attachment)
        is V2ChatAttachment.PlazaRecommend -> V2AIChatPlazaCard(attachment)
    }
}

@Composable
private fun V2AIChatVideoRefCard(payload: V2ChatAttachment.VideoRef) {
    Row(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFAF7F0))
            .border(0.5.dp, DSColors.borderLine, RoundedCornerShape(12.dp))
            .testTag("ai-attachment-video-ref")
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(thumbnailBrush(payload.thumbnailKind)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(13.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(payload.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                androidx.compose.material3.Icon(Icons.Filled.Schedule, null, tint = DSColors.shadowGray, modifier = Modifier.size(9.dp))
                Text(durationString(payload.durationSec), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = DSColors.shadowGray)
                Text("·", fontSize = 10.sp, color = DSColors.shadowGray)
                Text(payload.statusLabel, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = DSColors.shadowGray)
            }
        }
    }
}

@Composable
private fun V2AIChatMemoSavedCard(payload: V2ChatAttachment.MemoSaved) {
    Column(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = DSColors.goldBright.copy(alpha = 0.08f),
                spotColor = DSColors.goldBright.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.goldBright.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .testTag("ai-attachment-memo-saved")
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            androidx.compose.material3.Icon(Icons.Filled.CheckCircle, null, tint = DSColors.goldBright, modifier = Modifier.size(13.dp))
            Text("心得已记录", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
            Spacer(modifier = Modifier.weight(1f))
            if (payload.isImportant) {
                Text(
                    "重要",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DSColors.goldBright)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Text(payload.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
        Text(payload.summary, fontSize = 12.sp, color = Color(0xFF6C757D), maxLines = 3, lineHeight = 17.sp)
        if (payload.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                payload.tags.forEach { tag ->
                    Text(
                        "# $tag",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DSColors.fairway,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DSColors.mistGreen)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun V2AIChatPlazaCard(payload: V2ChatAttachment.PlazaRecommend) {
    Column(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f),
            )
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.cardWhite)
            .border(0.5.dp, DSColors.borderLine, RoundedCornerShape(14.dp))
            .testTag("ai-attachment-plaza-recommend")
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(DSColors.fairway),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(9.dp))
            }
            Text("相关视频", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                payload.statusLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = DSColors.fairway,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DSColors.mistGreen)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Text(payload.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
        Text(payload.summary, fontSize = 12.sp, color = Color(0xFF6C757D), maxLines = 2, lineHeight = 17.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                androidx.compose.material3.Icon(Icons.Filled.Schedule, null, tint = DSColors.shadowGray, modifier = Modifier.size(9.dp))
                Text(durationString(payload.durationSec), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = DSColors.shadowGray)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("推荐参考", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DSColors.fairway)
        }
    }
}

private fun durationString(sec: Double): String {
    val total = sec.toInt().coerceAtLeast(0)
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

private fun thumbnailBrush(kind: V2ChatThumbnailKind): Brush {
    val colors = when (kind) {
        V2ChatThumbnailKind.FAIRWAY -> listOf(Color(0xFF1B4332), Color(0xFF40916C))
        V2ChatThumbnailKind.SAND -> listOf(Color(0xFFA77B0E), Color(0xFFF1C77B))
        V2ChatThumbnailKind.GREEN_GRASS -> listOf(Color(0xFF2D6A4F), Color(0xFF74C69D))
        V2ChatThumbnailKind.SUNSET -> listOf(Color(0xFFA73121), Color(0xFFE8B36F))
        V2ChatThumbnailKind.MIST -> listOf(Color(0xFF495057), Color(0xFF9CA3AF))
        V2ChatThumbnailKind.FOREST -> listOf(Color(0xFF081C15), Color(0xFF2D6A4F))
    }
    return Brush.linearGradient(colors)
}

@Composable
private fun AIAvatar() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = DSColors.fairway.copy(alpha = 0.20f),
                spotColor = DSColors.fairway.copy(alpha = 0.20f),
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(Color(0xFFD8E2DC), DSColors.fairway)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White),
                )
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(11.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(DSColors.fairway),
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(DSColors.fairway),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(7.dp)
                            .height(1.dp)
                            .clip(RoundedCornerShape(50))
                            .background(DSColors.fairway.copy(alpha = 0.40f)),
                    )
                }
            }
        }
    }
}
