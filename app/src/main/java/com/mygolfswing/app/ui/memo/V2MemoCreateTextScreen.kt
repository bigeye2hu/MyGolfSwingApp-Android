package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.store.V2CategoryStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.ui.common.v2StatusBarPadding

/**
 * 创建文字 Memo 页（对齐 iOS `V2MemoCreateTextView`）。
 */
@Composable
fun V2MemoCreateTextScreen(
    onClose: () -> Unit = {},
) {
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var selectedTagIds by remember { mutableStateOf(setOf<String>()) }

    val canSave = title.isNotBlank() || body.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    0.0f to Color(0xFFF0F8F4),
                    0.5f to Color(0xFFF5F5F0),
                    1.0f to Color(0xFFFAF7F0),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CreateMemoNavBar(
                canSave = canSave,
                onClose = onClose,
                onSave = {
                    val tagNames = V2CategoryStore.tagNamesFor(selectedTagIds)
                    val topic = V2MemoBridge.inferTopic(null, tagNames, title)
                    val memo = V2Memo.textMemo(
                        title = title.ifBlank { body.lineSequence().firstOrNull()?.take(20) ?: "新 Memo" },
                        body = body,
                        topic = topic,
                        tags = tagNames,
                    ).copy(tagIds = selectedTagIds.toList())
                    V2MemoStore.create(memo)
                    onClose()
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DSPadding, vertical = DSPadding),
                verticalArrangement = Arrangement.spacedBy(DSPadding),
            ) {
                FormCard {
                    SectionLabel("标题（可选）")
                    OutlineField(
                        text = title,
                        onTextChange = { if (it.length <= 60) title = it },
                        placeholder = "如：握把压力的微调",
                    )

                    Divider()

                    SectionLabel("正文")
                    OutlineField(
                        text = body,
                        onTextChange = { body = it },
                        placeholder = "记下今天的心得 / 观察 / 教练点评……",
                        minHeight = 160.dp,
                        singleLine = false,
                    )

                    Divider()

                    V2MemoTagPicker(
                        selectedTagIds = selectedTagIds,
                        onSelectedTagIdsChange = { selectedTagIds = it },
                    )
                }

                SubmitButton(canSubmit = canSave, onSubmit = {
                    val tagNames = V2CategoryStore.tagNamesFor(selectedTagIds)
                    val topic = V2MemoBridge.inferTopic(null, tagNames, title)
                    val memo = V2Memo.textMemo(
                        title = title.ifBlank { body.lineSequence().firstOrNull()?.take(20) ?: "新 Memo" },
                        body = body,
                        topic = topic,
                        tags = tagNames,
                    ).copy(tagIds = selectedTagIds.toList())
                    V2MemoStore.create(memo)
                    onClose()
                })
            }
        }
    }
}

private val DSPadding = 16.dp

// =================================================================
// 子组件
// =================================================================

@Composable
private fun CreateMemoNavBar(
    canSave: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .background(Color.White.copy(alpha = 0.92f))
            .padding(horizontal = DSPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(width = 1.dp, color = Color(0xFFE5E7EB), shape = CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭",
                tint = Color(0xFF1B2A21),
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "新 Memo", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A21))
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "保存",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (canSave) Color(0xFF2D6A4F) else Color(0xFF9CA3AF),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(enabled = canSave) { onSave() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.75f), shape = RoundedCornerShape(24.dp))
            .padding(vertical = 4.dp),
    ) {
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1B2A21),
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .background(Color.Black.copy(alpha = 0.06f)),
    )
}

@Composable
private fun OutlineField(
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    minHeight: androidx.compose.ui.unit.Dp = 44.dp,
    singleLine: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFAF7F0))
            .border(width = 1.dp, color = Color(0xFFE5E7EB), shape = RoundedCornerShape(12.dp))
            .defaultMinSize(minHeight = minHeight)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (text.isEmpty()) {
            Text(text = placeholder, fontSize = 14.sp, color = Color(0xFFB8BEC4))
        }
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF1B2A21)),
            cursorBrush = SolidColor(Color(0xFF2D6A4F)),
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
        )
    }
}

@Composable
private fun SubmitButton(canSubmit: Boolean, onSubmit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(elevation = if (canSubmit) 10.dp else 0.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        if (canSubmit) Color(0xFF2D6A4F) else Color(0xFFB8BEC4),
                        if (canSubmit) Color(0xFF40916C) else Color(0xFFD1D5DB),
                    ),
                ),
            )
            .clickable(enabled = canSubmit, onClick = onSubmit),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp).padding(end = 4.dp),
        )
        Text(text = "保存 Memo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
