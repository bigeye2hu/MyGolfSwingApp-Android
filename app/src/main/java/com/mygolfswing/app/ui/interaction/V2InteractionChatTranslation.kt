package com.mygolfswing.app.ui.interaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.designsystem.DSColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun V2ChatTranslationBubble(
    translatedText: String?,
    isMine: Boolean,
    translating: Boolean,
    onHide: () -> Unit,
    onForward: () -> Unit,
    onCopy: () -> Unit,
) {
    var menuExpanded by remember(translatedText) { mutableStateOf(false) }
    val foreground = if (isMine) Color.White else DSColors.deepGreen
    val secondary = if (isMine) Color.White.copy(alpha = 0.60f) else DSColors.shadowGray
    val background = if (isMine) DSColors.fairway else Color.White.copy(alpha = 0.96f)
    Box {
        Column(
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(background)
                .combinedClickable(
                    onClick = { menuExpanded = false },
                    onLongClick = { if (!translating && !translatedText.isNullOrBlank()) menuExpanded = true },
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("chat-translation-bubble"),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            if (translating) {
                CircularProgressIndicator(
                    color = foreground,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp).testTag("chat-translation-progress"),
                )
            } else {
                V2ChatEmojiText(
                    text = translatedText.orEmpty(),
                    color = foreground,
                    fontSize = 14.sp,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = secondary, modifier = Modifier.size(10.dp))
                    Text("由腾讯云 IM 提供翻译支持", color = secondary, fontSize = 10.sp)
                }
            }
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = Color.White,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                V2TranslationAction(Icons.Filled.VisibilityOff, "隐藏") {
                    menuExpanded = false
                    onHide()
                }
                V2TranslationAction(Icons.AutoMirrored.Filled.Reply, "转发") {
                    menuExpanded = false
                    onForward()
                }
                V2TranslationAction(Icons.Filled.ContentCopy, "复制") {
                    menuExpanded = false
                    onCopy()
                }
            }
        }
    }
}

@Composable
private fun V2TranslationAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(42.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = label, tint = DSColors.fairway, modifier = Modifier.size(18.dp))
        Text(label, color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
