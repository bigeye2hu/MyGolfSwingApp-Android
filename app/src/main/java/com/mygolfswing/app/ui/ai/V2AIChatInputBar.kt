package com.mygolfswing.app.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors

@Composable
fun V2AIChatInputBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "向 AI 教练提问…",
    disabledPlaceholder: String = "登录后向 AI 教练提问…",
    onDisabledClick: () -> Unit = {},
) {
    val canSend = enabled && draft.trim().isNotEmpty()
    val canTapAction = canSend || !enabled
    val actionColor = if (canSend) DSColors.fairway else DSColors.fairway.copy(alpha = 0.4f)
    val actionShadowColor = DSColors.fairway.copy(alpha = 0.25f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DSColors.warmWhite)
            .imePadding()
            .v2NavigationBarPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(DSColors.borderLine),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.cardWhite)
                    .border(width = 0.5.dp, color = DSColors.borderLine, shape = RoundedCornerShape(50))
                    .clickable(enabled = !enabled, onClick = onDisabledClick)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                if (draft.isEmpty()) {
                    Text(
                        text = if (enabled) placeholder else disabledPlaceholder,
                        fontSize = 14.sp,
                        color = DSColors.shadowGray,
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai-chat-input"),
                    enabled = enabled,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = if (enabled) DSColors.inkGreen else DSColors.shadowGray,
                    ),
                    cursorBrush = SolidColor(DSColors.fairway),
                    maxLines = 4,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        ambientColor = actionShadowColor,
                        spotColor = actionShadowColor,
                    )
                    .clip(CircleShape)
                    .background(actionColor)
                    .testTag("ai-chat-send")
                    .clickable(enabled = canTapAction) {
                        if (!enabled) onDisabledClick() else onSend()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
