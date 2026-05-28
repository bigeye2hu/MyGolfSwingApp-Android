package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.mygolfswing.app.data.model.V2PinState
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * 文字 Memo 详情/编辑（对齐 iOS `V2TextMemoDetailView`）。
 */
@Composable
fun V2MemoDetailScreen(
    memoId: UUID,
    onBack: () -> Unit = {},
) {
    val memos by V2MemoStore.allMemos.collectAsState()
    val memo = memos.firstOrNull { it.id == memoId }

    if (memo == null) {
        V2MissingEntityScreen(message = "Memo 不存在", onBack = onBack, title = "随手记")
        return
    }

    var title by remember(memo.id) { mutableStateOf(memo.title ?: memo.displayTitle ?: "") }
    var body by remember(memo.id) { mutableStateOf(memo.text ?: "") }
    var tags by remember(memo.id) { mutableStateOf(memo.tags) }
    var dirty by remember { mutableStateOf(false) }

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
            DetailNavBar(memo = memo, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
                verticalArrangement = Arrangement.spacedBy(DSSpacing.md),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; dirty = true },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("标题") },
                    singleLine = true,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(DSColors.cardWhite)
                        .padding(DSSpacing.md),
                ) {
                    Text("正文", fontSize = 12.sp, color = DSColors.shadowGray)
                    BasicTextField(
                        value = body,
                        onValueChange = { body = it; dirty = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .padding(top = 8.dp),
                        textStyle = TextStyle(fontSize = 15.sp, color = DSColors.inkGreen),
                        cursorBrush = SolidColor(DSColors.fairway),
                    )
                }
                V2MemoTagPicker(selected = tags, onChange = { tags = it; dirty = true })
                val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                Text(
                    "创建于 ${fmt.format(memo.createdAt)} · 更新于 ${fmt.format(memo.updatedAt)}",
                    fontSize = 11.sp,
                    color = DSColors.shadowGray,
                )
            }

            if (dirty) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DSColors.cardWhite)
                        .padding(DSSpacing.lg)
                        .clip(RoundedCornerShape(50))
                        .background(DSColors.fairway)
                        .clickable {
                            val updated = memo.copy(
                                title = title.ifBlank { null },
                                displayTitle = title.ifBlank { memo.displayTitle },
                                text = body,
                                tags = tags,
                            )
                            V2MemoStore.update(updated)
                            dirty = false
                            onBack()
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("保存修改", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DetailNavBar(memo: V2Memo, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .background(DSColors.cardWhite.copy(alpha = 0.92f))
            .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DSColors.fairway.copy(alpha = 0.08f))
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.weight(1f))
        Text("随手记", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = DSColors.inkGreen)
        Spacer(Modifier.weight(1f))
        ToggleStarBtn(memo = memo)
        Spacer(Modifier.width(8.dp))
        DeleteBtn(memo = memo, onAfterDelete = onBack)
    }
}

@Composable
private fun ToggleStarBtn(memo: V2Memo) {
    val active = memo.isPinned
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (active) DSColors.trophyGold.copy(alpha = 0.18f) else DSColors.fairway.copy(alpha = 0.08f))
            .clickable {
                V2MemoStore.update(memo.copy(pinState = if (active) V2PinState.NONE else V2PinState.PINNED))
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (active) Icons.Filled.Star else Icons.Filled.StarBorder,
            null,
            tint = if (active) DSColors.trophyGold else DSColors.fairway,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun DeleteBtn(memo: V2Memo, onAfterDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(DSColors.flagRed.copy(alpha = 0.12f))
            .clickable { V2MemoStore.delete(memo.id); onAfterDelete() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.DeleteOutline, null, tint = DSColors.flagRed, modifier = Modifier.size(16.dp))
    }
}
