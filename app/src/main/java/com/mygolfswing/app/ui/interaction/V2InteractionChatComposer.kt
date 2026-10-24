package com.mygolfswing.app.ui.interaction

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

private enum class ChatComposerSheet { MEDIA, CAMERA, EMOJI }
internal const val V2_CHAT_VOICE_LONG_PRESS_MILLIS = 200L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2InteractionChatComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    busy: Boolean,
    uploadProgress: Int?,
    onSendText: () -> Unit,
    onSendMedia: (V2PreparedChatMedia) -> Unit,
    onSendVoice: (V2WorkspaceVoiceRecording) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputFocusRequester = remember { FocusRequester() }
    val recorder = remember { V2WorkspaceVoiceRecorder(context.applicationContext, "chat_voice_messages") }
    var localPreparing by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<ChatComposerSheet?>(null) }
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var pendingVideoFile by remember { mutableStateOf<File?>(null) }
    var requestedCaptureKind by remember { mutableStateOf<V2PreparedChatMediaKind?>(null) }
    var recording by remember { mutableStateOf(false) }
    var cancellingRecording by remember { mutableStateOf(false) }
    var recordingStartedAt by remember { mutableLongStateOf(0L) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    var amplitude by remember { mutableIntStateOf(0) }
    var inputFocused by remember { mutableStateOf(false) }
    val disabled = busy || localPreparing

    fun reportFailure(prefix: String, error: Throwable?) {
        onError(error?.message?.takeIf(String::isNotBlank)?.let { "$prefix：$it" } ?: prefix)
    }

    fun prepareUris(uris: List<Uri>, forceFile: Boolean = false) {
        if (uris.isEmpty()) return
        localPreparing = true
        scope.launch {
            try {
                uris.take(9).forEach { uri ->
                    onSendMedia(V2ChatMediaFiles.prepare(context, uri, forceFile))
                }
            } catch (error: Throwable) {
                reportFailure("文件暂时无法发送", error)
            } finally {
                localPreparing = false
            }
        }
    }

    fun finishCapture(file: File?, kind: V2PreparedChatMediaKind, ok: Boolean) {
        if (!ok || file == null) {
            file?.delete()
            return
        }
        localPreparing = true
        scope.launch {
            try {
                val mime = if (kind == V2PreparedChatMediaKind.IMAGE) "image/jpeg" else "video/mp4"
                onSendMedia(V2ChatMediaFiles.inspectCapture(file, kind, mime))
            } catch (error: Throwable) {
                file.delete()
                reportFailure("拍摄内容暂时无法发送", error)
            } finally {
                localPreparing = false
            }
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(9)) { uris ->
        prepareUris(uris)
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { prepareUris(listOf(it), forceFile = true) }
    }
    val photoCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = pendingPhotoFile
        pendingPhotoFile = null
        finishCapture(file, V2PreparedChatMediaKind.IMAGE, ok)
    }
    val videoCamera = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        val file = pendingVideoFile
        pendingVideoFile = null
        finishCapture(file, V2PreparedChatMediaKind.VIDEO, ok)
    }

    fun launchCaptureNow(kind: V2PreparedChatMediaKind) {
        runCatching {
            val file = V2ChatMediaFiles.createCaptureFile(context, kind)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            if (kind == V2PreparedChatMediaKind.IMAGE) {
                pendingPhotoFile = file
                photoCamera.launch(uri)
            } else {
                pendingVideoFile = file
                videoCamera.launch(uri)
            }
        }.onFailure { reportFailure("无法打开相机，请检查权限后重试", it) }
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val kind = requestedCaptureKind
        requestedCaptureKind = null
        if (granted && kind != null) launchCaptureNow(kind)
        else if (!granted) onError("需要相机权限才能拍摄聊天内容")
    }

    fun launchCapture(kind: V2PreparedChatMediaKind) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCaptureNow(kind)
        } else {
            requestedCaptureKind = kind
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    fun stopRecording(cancel: Boolean) {
        if (!recorder.isRecording) {
            recording = false
            cancellingRecording = false
            return
        }
        recorder.stop(cancel).onSuccess { result ->
            recording = false
            cancellingRecording = false
            recordingStartedAt = 0L
            elapsedMillis = 0L
            amplitude = 0
            if (result != null && result.durationSeconds >= 1.0) {
                onSendVoice(result)
            } else {
                result?.file?.delete()
                if (!cancel) onError("语音录制时长太短")
            }
        }.onFailure {
            recording = false
            cancellingRecording = false
            reportFailure("录音暂时无法完成", it)
        }
    }

    fun startRecording(): Boolean = recorder.start().fold(
        onSuccess = {
            recording = true
            cancellingRecording = false
            recordingStartedAt = SystemClock.elapsedRealtime()
            true
        },
        onFailure = {
            reportFailure("无法开始录音，请检查麦克风权限", it)
            false
        },
    )

    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) Toast.makeText(context, "麦克风权限已开启，请再次按住录音", Toast.LENGTH_SHORT).show()
        else onError("需要麦克风权限才能发送语音")
    }

    fun beginVoiceGesture(): Boolean {
        return if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
            false
        }
    }

    LaunchedEffect(recording, recordingStartedAt) {
        while (recording) {
            elapsedMillis = (SystemClock.elapsedRealtime() - recordingStartedAt).coerceAtLeast(0L)
            amplitude = recorder.currentAmplitude
            if (elapsedMillis >= 60_000L) {
                Toast.makeText(context, "语音录制时长达到上限", Toast.LENGTH_SHORT).show()
                stopRecording(cancel = false)
                break
            }
            delay(100L)
        }
    }
    DisposableEffect(recorder) {
        onDispose { if (recorder.isRecording) recorder.stop(cancel = true) }
    }

    Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
        if (uploadProgress != null || localPreparing) {
            LinearProgressIndicator(
                progress = { (uploadProgress ?: 8).coerceIn(0, 100) / 100f },
                color = DSColors.fairway,
                trackColor = DSColors.mistGreen,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                if (localPreparing) "正在准备媒体…" else "正在发送… ${uploadProgress ?: 0}%",
                color = DSColors.shadowGray,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp),
            )
        }
        Box(Modifier.fillMaxWidth().heightIn(min = if (recording) 100.dp else 0.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                ChatInputIcon(Icons.Filled.AddCircleOutline, "更多发送方式", disabled) { sheet = ChatComposerSheet.MEDIA }
                Box(Modifier.weight(1f)) {
                    val showRichDraft = !inputFocused && draft.contains("[TUIEmoji_")
                    TextField(
                        value = draft,
                        onValueChange = { onDraftChange(it.take(2_000)) },
                        enabled = !disabled && !recording,
                        placeholder = { Text("发消息…", color = DSColors.shadowGray) },
                        modifier = Modifier.fillMaxWidth().focusRequester(inputFocusRequester)
                            .onFocusChanged { inputFocused = it.isFocused }
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
                        trailingIcon = {
                            Icon(
                                if (sheet == ChatComposerSheet.EMOJI) Icons.Filled.Keyboard else Icons.Filled.SentimentSatisfiedAlt,
                                contentDescription = if (sheet == ChatComposerSheet.EMOJI) "返回键盘" else "表情",
                                tint = DSColors.fairway,
                                modifier = Modifier.clickable(enabled = !disabled) {
                                    if (sheet == ChatComposerSheet.EMOJI) {
                                        sheet = null
                                        inputFocusRequester.requestFocus()
                                        keyboardController?.show()
                                    } else {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        sheet = ChatComposerSheet.EMOJI
                                    }
                                }.padding(4.dp),
                            )
                        },
                        visualTransformation = remember { V2ChatEmojiVisualTransformation() },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = if (showRichDraft) Color.Transparent else DSColors.deepGreen,
                            unfocusedTextColor = if (showRichDraft) Color.Transparent else DSColors.deepGreen,
                            disabledTextColor = if (showRichDraft) Color.Transparent else DSColors.shadowGray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        maxLines = 5,
                    )
                    if (showRichDraft) {
                        Box(
                            modifier = Modifier.matchParentSize().padding(start = 16.dp, end = 52.dp, top = 8.dp, bottom = 8.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            V2ChatEmojiText(
                                text = draft,
                                color = DSColors.deepGreen,
                                fontSize = 16.sp,
                                modifier = Modifier.testTag("chat-rich-draft-preview"),
                            )
                        }
                    }
                }
                V2ChatVoiceInputIcon(
                    disabled = disabled,
                    onStart = ::beginVoiceGesture,
                    onCancelChange = { cancellingRecording = it },
                    onEnd = ::stopRecording,
                )
                if (draft.isBlank()) {
                    ChatInputIcon(Icons.Filled.CameraAlt, "拍摄", disabled) { sheet = ChatComposerSheet.CAMERA }
                } else {
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape)
                            .background(if (!disabled) DSColors.fairway else DSColors.mistGreen)
                            .clickable(enabled = !disabled, onClick = onSendText),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Filled.Send, "发送消息", tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                }
            }
            if (recording) {
                V2ChatVoiceRecordingPanel(
                    elapsedMillis = elapsedMillis,
                    amplitude = amplitude,
                    cancelling = cancellingRecording,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        if (sheet == ChatComposerSheet.EMOJI) {
            V2ChatMessageEmojiPicker(
                draft = draft,
                disabled = disabled,
                onDraftChange = onDraftChange,
                onSend = {
                    if (draft.isNotBlank()) {
                        onSendText()
                        sheet = null
                    }
                },
            )
        }
    }

    sheet?.takeIf { it != ChatComposerSheet.EMOJI }?.let { active ->
        ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = DSColors.warmWhite) {
            when (active) {
                ChatComposerSheet.MEDIA -> {
                    ChatSheetTitle("发送内容", "支持图片、视频和文件，一次最多选择9项媒体")
                    ChatSheetAction(Icons.Filled.PhotoLibrary, "从相册选择") {
                        sheet = null
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    }
                    ChatSheetAction(Icons.Filled.CameraAlt, "拍照或拍视频") { sheet = ChatComposerSheet.CAMERA }
                    ChatSheetAction(Icons.AutoMirrored.Filled.InsertDriveFile, "选择文件（最大1GB）") {
                        sheet = null
                        filePicker.launch(arrayOf("*/*"))
                    }
                }
                ChatComposerSheet.CAMERA -> {
                    ChatSheetTitle("拍摄", "拍摄完成后会直接发送给对方")
                    ChatSheetAction(Icons.Filled.CameraAlt, "拍照") {
                        sheet = null
                        launchCapture(V2PreparedChatMediaKind.IMAGE)
                    }
                    ChatSheetAction(Icons.Filled.Videocam, "拍摄视频") {
                        sheet = null
                        launchCapture(V2PreparedChatMediaKind.VIDEO)
                    }
                }
                ChatComposerSheet.EMOJI -> Unit
            }
            Box(Modifier.fillMaxWidth().padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun V2ChatMessageEmojiPicker(
    draft: String,
    disabled: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val context = LocalContext.current
    var recent by remember(context) { mutableStateOf(recentMessageEmojis(context)) }
    Box(
        modifier = Modifier.fillMaxWidth().height(300.dp).background(DSColors.warmWhite)
            .testTag("chat-message-emoji-picker"),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.fillMaxWidth().height(300.dp)
                .padding(start = 16.dp, end = 16.dp, bottom = 62.dp),
        ) {
            if (recent.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "最近使用",
                        color = DSColors.shadowGray,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 8.dp, bottom = 4.dp),
                    )
                }
                items(recent, key = { "recent-${it.reactionId}" }) { emoji ->
                    V2MessageEmojiCell(emoji) {
                        recent = rememberMessageEmoji(context, emoji.reactionId)
                        onDraftChange(appendChatEmojiCode(draft, emoji.reactionId))
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "全部表情",
                    color = DSColors.shadowGray,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            items(v2ChatReactionEmojis, key = { "all-${it.reactionId}" }) { emoji ->
                V2MessageEmojiCell(emoji) {
                    recent = rememberMessageEmoji(context, emoji.reactionId)
                    onDraftChange(appendChatEmojiCode(draft, emoji.reactionId))
                }
            }
        }
        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.size(width = 50.dp, height = 30.dp).clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .clickable(enabled = !disabled && draft.isNotEmpty()) {
                        onDraftChange(deleteLastChatEmojiOrCharacter(draft))
                    }
                    .testTag("chat-message-emoji-delete"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "删除",
                    tint = DSColors.deepGreen,
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(
                modifier = Modifier.size(width = 50.dp, height = 30.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (!disabled && draft.isNotBlank()) DSColors.fairway.copy(alpha = 0.8f) else DSColors.mistGreen)
                    .clickable(enabled = !disabled && draft.isNotBlank(), onClick = onSend)
                    .testTag("chat-message-emoji-send"),
                contentAlignment = Alignment.Center,
            ) {
                Text("发送", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun V2MessageEmojiCell(emoji: V2ChatReactionEmoji, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clickable(onClick = onClick)
            .testTag("chat-message-emoji-${emoji.reactionId}"),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(chatEmojiDrawable(emoji)),
            contentDescription = emoji.glyph,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(32.dp),
        )
    }
}

internal fun chatVoiceRecordingHint(cancelling: Boolean): String =
    if (cancelling) "松开取消发送" else "向上滑动取消或松开发送"

internal fun chatVoiceRecordingDuration(elapsedMillis: Long): String {
    val totalSeconds = (elapsedMillis.coerceAtLeast(0L) / 1_000L).coerceAtMost(60L)
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

@Composable
internal fun V2ChatVoiceRecordingPanel(
    elapsedMillis: Long,
    amplitude: Int,
    cancelling: Boolean,
    modifier: Modifier = Modifier,
) {
    val waveformStrength = (amplitude.coerceAtLeast(0) / 32_767f).coerceIn(0f, 1f)
    Column(
        modifier = modifier.fillMaxWidth().height(100.dp).background(Color.White)
            .testTag("chat-voice-recording-panel"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            chatVoiceRecordingHint(cancelling),
            color = if (cancelling) DSColors.flagRed else DSColors.shadowGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 10.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (cancelling) DSColors.flagRed else DSColors.fairway)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                chatVoiceRecordingDuration(elapsedMillis),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Canvas(Modifier.weight(1f).height(20.dp).padding(start = 24.dp)) {
                val bars = 30
                val gap = size.width / bars
                repeat(bars) { index ->
                    val wave = 0.35f + 0.65f * kotlin.math.abs(kotlin.math.sin((index + elapsedMillis / 100.0) * 0.72)).toFloat()
                    val height = 6.dp.toPx() + 12.dp.toPx() * wave * (0.28f + waveformStrength * 0.72f)
                    drawRoundRect(
                        color = Color.White,
                        topLeft = androidx.compose.ui.geometry.Offset(index * gap, (size.height - height) / 2f),
                        size = androidx.compose.ui.geometry.Size(2.dp.toPx(), height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
                    )
                }
            }
        }
    }
}

@Composable
internal fun V2ChatVoiceInputIcon(
    disabled: Boolean,
    onStart: () -> Boolean,
    onCancelChange: (Boolean) -> Unit,
    onEnd: (Boolean) -> Unit,
    longPressMillis: Long = V2_CHAT_VOICE_LONG_PRESS_MILLIS,
) {
    val cancelThresholdPx = with(LocalDensity.current) { 20.dp.toPx() }
    Icon(
        Icons.Filled.Mic,
        contentDescription = null,
        tint = if (disabled) DSColors.shadowGray.copy(alpha = 0.45f) else DSColors.fairway,
        modifier = Modifier.size(42.dp).clip(CircleShape).semantics { contentDescription = "录制语音" }
            .pointerInput(disabled, cancelThresholdPx) {
                if (disabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val releasedBeforeLongPress = withTimeoutOrNull(longPressMillis) { waitForUpOrCancellation() }
                    if (releasedBeforeLongPress != null) return@awaitEachGesture
                    if (!onStart()) return@awaitEachGesture
                    var cancelling = false
                    var ended = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val nextCancelling = change.position.y - down.position.y < -cancelThresholdPx
                            if (nextCancelling != cancelling) {
                                cancelling = nextCancelling
                                onCancelChange(cancelling)
                            }
                            change.consume()
                            if (!change.pressed) {
                                ended = true
                                onEnd(cancelling)
                                break
                            }
                        }
                    } finally {
                        if (!ended) onEnd(true)
                    }
                }
            }.padding(9.dp),
    )
}

@Composable
private fun ChatInputIcon(
    image: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    disabled: Boolean,
    onClick: () -> Unit,
) {
    Icon(
        image,
        contentDescription = description,
        tint = if (disabled) DSColors.shadowGray.copy(alpha = 0.45f) else DSColors.fairway,
        modifier = Modifier.size(36.dp).clip(CircleShape).clickable(enabled = !disabled, onClick = onClick).padding(6.dp),
    )
}

@Composable
private fun ChatSheetTitle(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp)) {
        Text(title, color = DSColors.deepGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = DSColors.shadowGray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ChatSheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(22.dp))
        }
        Text(title, color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}
