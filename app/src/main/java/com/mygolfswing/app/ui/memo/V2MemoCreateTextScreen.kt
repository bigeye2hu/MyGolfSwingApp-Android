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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.store.V2CategoryStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 创建文字 Memo 页（对齐 iOS `V2MemoCreateTextView`）。
 */
@Composable
fun V2MemoCreateTextScreen(
    onClose: () -> Unit = {},
    onSaved: () -> Unit = onClose,
    persistCreatedMemo: (suspend (V2Memo) -> Unit)? = null,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var selectedTagIds by rememberSaveable(
        stateSaver = listSaver<Set<String>, String>(
            save = { it.toList() },
            restore = { it.toSet() },
        ),
    ) { mutableStateOf(emptySet<String>()) }
    var showDiscardConfirm by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val canSave = title.trim().isNotEmpty() && body.trim().isNotEmpty() && !saving
    val hasDraft = title.isNotBlank() ||
        body.isNotBlank() ||
        selectedTagIds.isNotEmpty()
    fun attemptClose() {
        if (saving) return
        if (hasDraft) showDiscardConfirm = true else onClose()
    }
    fun saveMemo() {
        if (saving || !canSave) return
        saving = true
        saveError = null
        val draftTitle = title.trim()
        val draftBody = body.trim()
        val draftTagIds = selectedTagIds.toList()
        scope.launch {
            try {
                val tagNames = V2CategoryStore.tagNamesFor(draftTagIds.toSet())
                val topic = V2MemoBridge.inferTopic(null, tagNames, draftTitle)
                val memo = V2Memo.textMemo(
                    title = draftTitle,
                    body = draftBody,
                    topic = topic,
                    tags = tagNames,
                ).copy(tagIds = draftTagIds)
                if (persistCreatedMemo != null) {
                    persistCreatedMemo(memo)
                } else {
                    V2MemoStore.createAndWait(memo)
                }
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                saveError = memoTextThrowableError("保存失败，请稍后重试", e)
            } finally {
                saving = false
            }
        }
    }

    V2BackHandler { attemptClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TextMemoCreateSurface),
    ) {
        V2RoomBackground(Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {
            CreateMemoNavBar(
                onClose = ::attemptClose,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = TextMemoPageHorizontalPadding)
                    .padding(top = TextMemoContentTopPadding, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(DSPadding),
            ) {
                TitleInputSection(
                    title = title,
                    onTitleChange = { title = it.take(TextMemoTitleMaxLength) },
                )
                BodyInputCard(
                    body = body,
                    onBodyChange = { body = it.take(TextMemoBodyMaxLength) },
                )
                AttachVideoReferenceRow()
                V2MemoTagPicker(
                    selectedTagIds = selectedTagIds,
                    onSelectedTagIdsChange = { selectedTagIds = it },
                )
            }
        }

        TextMemoBottomSaveBar(
            canSubmit = canSave,
            saving = saving,
            onSubmit = ::saveMemo,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    saveError?.let { message ->
        VideoMemoOperationFailureDialog(
            title = "保存失败",
            message = message,
            primaryLabel = "好",
            onPrimary = { saveError = null },
            onDismiss = { saveError = null },
        )
    }

    if (showDiscardConfirm) {
        VideoMemoDiscardSheet(
            editing = false,
            onContinue = { showDiscardConfirm = false },
            onDiscard = {
                showDiscardConfirm = false
                onClose()
            },
        )
    }

}

private val DSPadding = 16.dp
private val TextMemoPageHorizontalPadding = 20.dp
private val TextMemoContentTopPadding = 28.dp
private val TextMemoCreateSurface = Color(0xFFFAF7F0)
private const val TextMemoTitleMaxLength = 24
private const val TextMemoBodyMaxLength = 240

// =================================================================
// 子组件
// =================================================================

@Composable
private fun CreateMemoNavBar(
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .background(TextMemoCreateSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TextMemoPageHorizontalPadding)
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
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
                    modifier = Modifier.size(13.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "记一个发现", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A21))
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(32.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black.copy(alpha = 0.06f)),
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
private fun TitleInputSection(
    title: String,
    onTitleChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth().testTag("text-memo-title-field"),
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B2A21),
            ),
            cursorBrush = SolidColor(Color(0xFF2D6A4F)),
            decorationBox = { inner ->
                if (title.isEmpty()) {
                    Text(
                        "标题",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC5C5C5),
                    )
                }
                inner()
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE5E7EB)),
        )
    }
}

@Composable
private fun BodyInputCard(
    body: String,
    onBodyChange: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 260.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(18.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = body,
                onValueChange = onBodyChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 200.dp)
                    .padding(end = 44.dp)
                    .testTag("text-memo-body-field"),
                textStyle = TextStyle(fontSize = 15.sp, color = Color(0xFF1B2A21)),
                cursorBrush = SolidColor(Color(0xFF2D6A4F)),
                decorationBox = { inner ->
                    if (body.isEmpty()) {
                        Text(
                            "开始写今天的感悟…",
                            fontSize = 15.sp,
                            color = Color(0xFFC5C5C5),
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                        )
                    }
                    inner()
                },
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Spacer(Modifier.weight(1f))
                Text(
                    "${body.length}/$TextMemoBodyMaxLength",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 8.dp, y = 8.dp)
                .size(48.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.12f),
                )
                .clip(CircleShape)
                .background(Color(0xFFE8F2EC).copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "语音输入暂未接入",
                tint = Color(0xFF2D6A4F).copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
        }
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
    showVoicePlaceholder: Boolean = false,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = if (showVoicePlaceholder) 44.dp else 0.dp),
            singleLine = singleLine,
        )
        if (showVoicePlaceholder) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp)
                    .size(48.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.12f),
                    )
                    .clip(CircleShape)
                    .background(Color(0xFFE8F2EC).copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "语音输入暂未接入",
                    tint = Color(0xFF2D6A4F).copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AttachVideoReferenceRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0.7f)
            .semantics { contentDescription = "挂视频参考暂未接入" }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFAF7F0))
            .border(width = 1.dp, color = Color(0xFFE5E7EB), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Videocam,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(14.dp),
        )
        Text("挂个视频参考？", fontSize = 15.sp, color = Color(0xFF9CA3AF))
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = Color(0xFF9CA3AF).copy(alpha = 0.6f),
            modifier = Modifier.size(11.dp),
        )
    }
}

@Composable
private fun TextMemoBottomSaveBar(
    canSubmit: Boolean,
    saving: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .background(TextMemoCreateSurface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE5E7EB)),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "先保存。没有字数目标，也不会自动分享",
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF2D6A4F).copy(alpha = if (canSubmit) 1f else 0.4f))
                    .testTag("text-memo-save")
                    .clickable(enabled = canSubmit, onClick = onSubmit),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = if (saving) "保存中…" else "保存",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}
