package com.mygolfswing.app.ui.interaction

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.coach.coachThrowableError
import com.mygolfswing.app.data.friend.V2IMConversation
import com.mygolfswing.app.data.friend.V2IMDeliveryState
import com.mygolfswing.app.data.friend.V2IMMessage
import com.mygolfswing.app.data.friend.V2IMMessageKind
import com.mygolfswing.app.data.session.V2Session
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.common.copyPlainTextToClipboard
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.io.File
import java.util.Locale
import java.util.UUID
import java.time.temporal.WeekFields

@Composable
internal fun V2InteractionChatScreen(
    conversation: V2IMConversation,
    subtitle: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val currentSession by ServiceLocator.session.state.collectAsState(initial = V2Session())
    var messages by remember(conversation.userId) { mutableStateOf<List<V2IMMessage>>(emptyList()) }
    var loading by remember(conversation.userId) { mutableStateOf(true) }
    var showLoadingIndicator by remember(conversation.userId) { mutableStateOf(false) }
    var hasMoreOlderMessages by remember(conversation.userId) { mutableStateOf(false) }
    var loadingOlderMessages by remember(conversation.userId) { mutableStateOf(false) }
    var initialScrollComplete by remember(conversation.userId) { mutableStateOf(false) }
    var activeSends by remember(conversation.userId) { mutableIntStateOf(0) }
    val sending = activeSends > 0
    var uploadProgress by remember(conversation.userId) { mutableStateOf<Int?>(null) }
    var error by remember(conversation.userId) { mutableStateOf<String?>(null) }
    var fileDownloadProgress by remember(conversation.userId) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var draft by rememberSaveable(conversation.userId) { mutableStateOf("") }
    var showSettings by rememberSaveable(conversation.userId) { mutableStateOf(false) }
    var displayTitle by remember(conversation.userId) { mutableStateOf(conversation.title) }
    var previewMessage by remember(conversation.userId) { mutableStateOf<V2IMMessage?>(null) }
    var mergedDetailMessage by remember(conversation.userId) { mutableStateOf<V2IMMessage?>(null) }
    var mergedDetailMessages by remember(conversation.userId) { mutableStateOf<List<V2IMMessage>>(emptyList()) }
    var mergedDetailLoading by remember(conversation.userId) { mutableStateOf(false) }
    var mergedDetailError by remember(conversation.userId) { mutableStateOf<String?>(null) }
    var messageActionInFlight by remember(conversation.userId) { mutableStateOf<String?>(null) }
    var reactionDetailMessageId by remember(conversation.userId) { mutableStateOf<String?>(null) }
    var translatingMessageIds by remember(conversation.userId) { mutableStateOf<Set<String>>(emptySet()) }
    var multiSelectMode by remember(conversation.userId) { mutableStateOf(false) }
    var selectedMessageIds by remember(conversation.userId) { mutableStateOf<Set<String>>(emptySet()) }
    var showForwardTypeDialog by remember(conversation.userId) { mutableStateOf(false) }
    var showSeparateForwardLimitAlert by remember(conversation.userId) { mutableStateOf(false) }
    var showForwardSelector by remember(conversation.userId) { mutableStateOf(false) }
    var forwardMessageIds by remember(conversation.userId) { mutableStateOf<List<String>>(emptyList()) }
    var forwardPlainText by remember(conversation.userId) { mutableStateOf<String?>(null) }
    var forwardMode by remember(conversation.userId) { mutableStateOf(V2ChatForwardMode.SEPARATE) }
    var forwardTargets by remember(conversation.userId) { mutableStateOf<List<V2IMConversation>>(emptyList()) }
    var selectedForwardTargets by remember(conversation.userId) { mutableStateOf<Set<String>>(emptySet()) }
    var forwardTargetsLoading by remember(conversation.userId) { mutableStateOf(false) }
    var forwardTargetsError by remember(conversation.userId) { mutableStateOf<String?>(null) }
    var forwarding by remember(conversation.userId) { mutableStateOf(false) }

    suspend fun renewTencentSession(): V2Session {
        ServiceLocator.authRepo.refreshUserSig().getOrThrow()
        return ServiceLocator.session.current()
    }

    suspend fun refresh(replaceHistory: Boolean): Boolean {
        var succeeded = false
        ServiceLocator.tencentFriendRepo.conversationMessagePage(
            session = ServiceLocator.session.current(),
            userId = conversation.userId,
            pageSize = 20,
            renewExpiredSession = ::renewTencentSession,
        ).onSuccess { page ->
            messages = mergeChatMessagePage(messages, page.messages, replaceHistory)
            if (replaceHistory) hasMoreOlderMessages = page.hasMoreOlder
            succeeded = true
        }
        return succeeded
    }

    suspend fun loadOlderMessages() {
        if (!hasMoreOlderMessages || loadingOlderMessages) return
        val oldestMessageId = messages.firstOrNull { !it.messageId.startsWith("local-") }?.messageId ?: return
        loadingOlderMessages = true
        ServiceLocator.tencentFriendRepo.conversationMessagePage(
            session = ServiceLocator.session.current(),
            userId = conversation.userId,
            beforeMessageId = oldestMessageId,
            pageSize = 20,
            renewExpiredSession = ::renewTencentSession,
        ).onSuccess { page ->
            messages = mergeChatMessagePage(messages, page.messages, replaceHistory = false)
            hasMoreOlderMessages = page.hasMoreOlder
        }
        loadingOlderMessages = false
    }

    fun appendMessage(message: V2IMMessage) {
        messages = (messages + message).distinctBy(V2IMMessage::messageId)
            .sortedBy(V2IMMessage::timeEpochSeconds)
    }

    fun replaceMessage(messageId: String, replacement: V2IMMessage) {
        messages = messages.map { message -> if (message.messageId == messageId) replacement else message }
            .distinctBy(V2IMMessage::messageId)
            .sortedBy(V2IMMessage::timeEpochSeconds)
    }

    fun updateDelivery(messageId: String, state: V2IMDeliveryState, failure: String? = null) {
        messages = messages.map { message ->
            if (message.messageId == messageId) message.copy(deliveryState = state, failureMessage = failure) else message
        }
    }

    fun beginSend() {
        activeSends += 1
        uploadProgress = 0
        error = null
    }

    fun finishSend() {
        activeSends = (activeSends - 1).coerceAtLeast(0)
        if (activeSends == 0) uploadProgress = null
    }

    suspend fun deliverLocalMessage(message: V2IMMessage): Result<V2IMMessage> {
        val progress: (Int) -> Unit = { value -> scope.launch { uploadProgress = value } }
        return when (message.kind) {
            V2IMMessageKind.TEXT -> ServiceLocator.tencentFriendRepo.sendTextMessage(
                ServiceLocator.session.current(), conversation.userId, message.text, ::renewTencentSession,
            )
            V2IMMessageKind.IMAGE -> ServiceLocator.tencentFriendRepo.sendImageMessage(
                ServiceLocator.session.current(), conversation.userId, requireNotNull(message.mediaUrl),
                ::renewTencentSession, progress,
            )
            V2IMMessageKind.VIDEO -> ServiceLocator.tencentFriendRepo.sendVideoMessage(
                ServiceLocator.session.current(), conversation.userId, requireNotNull(message.mediaUrl),
                requireNotNull(message.thumbnailUrl), message.durationSeconds ?: 1,
                ::renewTencentSession, progress,
            )
            V2IMMessageKind.VOICE -> ServiceLocator.tencentFriendRepo.sendVoiceMessage(
                ServiceLocator.session.current(), conversation.userId, requireNotNull(message.mediaUrl),
                message.durationSeconds ?: 1, ::renewTencentSession, progress,
            )
            V2IMMessageKind.FILE -> ServiceLocator.tencentFriendRepo.sendFileMessage(
                ServiceLocator.session.current(), conversation.userId, requireNotNull(message.mediaUrl),
                message.fileName ?: File(message.mediaUrl).name, ::renewTencentSession, progress,
            )
            else -> Result.failure(IllegalArgumentException("该消息暂时无法重发"))
        }
    }

    fun sendOptimistically(localMessage: V2IMMessage, append: Boolean = true) {
        if (append) appendMessage(localMessage) else updateDelivery(localMessage.messageId, V2IMDeliveryState.SENDING)
        beginSend()
        scope.launch {
            deliverLocalMessage(localMessage).onSuccess { sent ->
                replaceMessage(localMessage.messageId, sent)
            }.onFailure { throwable ->
                val prefix = when (localMessage.kind) {
                    V2IMMessageKind.TEXT -> "消息发送失败，请重试"
                    V2IMMessageKind.VOICE -> "语音消息发送失败，请重试"
                    else -> "媒体消息发送失败，请重试"
                }
                val failure = coachThrowableError(prefix, throwable)
                updateDelivery(localMessage.messageId, V2IMDeliveryState.FAILED, failure)
            }
            finishSend()
        }
    }

    fun sendPreparedMedia(media: V2PreparedChatMedia) {
        sendOptimistically(
            V2IMMessage(
                messageId = "local-${UUID.randomUUID()}",
                senderUserId = currentSession.userId.orEmpty(),
                timeEpochSeconds = Instant.now().epochSecond,
                isMine = true,
                kind = when (media.kind) {
                    V2PreparedChatMediaKind.IMAGE -> V2IMMessageKind.IMAGE
                    V2PreparedChatMediaKind.VIDEO -> V2IMMessageKind.VIDEO
                    V2PreparedChatMediaKind.FILE -> V2IMMessageKind.FILE
                },
                mediaUrl = media.file.absolutePath,
                thumbnailUrl = when (media.kind) {
                    V2PreparedChatMediaKind.IMAGE -> media.file.absolutePath
                    V2PreparedChatMediaKind.VIDEO -> media.snapshotFile?.absolutePath
                    V2PreparedChatMediaKind.FILE -> null
                },
                fileName = media.displayName,
                fileSizeBytes = media.sizeBytes,
                durationSeconds = media.durationSeconds,
                deliveryState = V2IMDeliveryState.SENDING,
            ),
        )
    }

    fun sendVoice(recording: V2WorkspaceVoiceRecording) {
        sendOptimistically(
            V2IMMessage(
                messageId = "local-${UUID.randomUUID()}",
                senderUserId = currentSession.userId.orEmpty(),
                timeEpochSeconds = Instant.now().epochSecond,
                isMine = true,
                kind = V2IMMessageKind.VOICE,
                mediaUrl = recording.file.absolutePath,
                durationSeconds = kotlin.math.ceil(recording.durationSeconds).toInt().coerceAtLeast(1),
                deliveryState = V2IMDeliveryState.SENDING,
            ),
        )
    }

    fun retryMessage(message: V2IMMessage) {
        if (message.deliveryState != V2IMDeliveryState.FAILED) return
        sendOptimistically(message.copy(deliveryState = V2IMDeliveryState.SENDING, failureMessage = null), append = false)
    }

    fun loadMergedMessage(message: V2IMMessage) {
        mergedDetailMessage = message
        mergedDetailMessages = emptyList()
        mergedDetailError = null
        if (message.mergedLayersOverLimit) {
            mergedDetailLoading = false
            mergedDetailError = "聊天记录层级过多，暂时无法展开"
            return
        }
        mergedDetailLoading = true
        scope.launch {
            ServiceLocator.tencentFriendRepo.mergedMessageContents(
                session = ServiceLocator.session.current(),
                messageId = message.messageId,
                renewExpiredSession = ::renewTencentSession,
            ).onSuccess {
                mergedDetailMessages = it
                mergedDetailError = null
            }.onFailure {
                mergedDetailError = coachThrowableError("聊天记录暂时无法加载", it)
            }
            mergedDetailLoading = false
        }
    }

    fun openLocalFile(message: V2IMMessage) {
        val value = message.mediaUrl
        if (value.isNullOrBlank()) {
            error = "文件地址暂时无法加载"
            return
        }
        runCatching {
            val uri = when {
                value.startsWith("/") -> FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(value),
                )
                value.startsWith("file:") -> FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(requireNotNull(value.toUri().path)),
                )
                else -> value.toUri()
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, chatFileMimeType(message.fileName ?: value))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "打开文件"))
        }.onFailure { error = "没有可用的应用可以打开这个文件" }
    }

    fun downloadAndOpenFile(message: V2IMMessage) {
        if (message.messageId in fileDownloadProgress) return
        fileDownloadProgress = fileDownloadProgress + (message.messageId to 0)
        error = null
        scope.launch {
            ServiceLocator.tencentFriendRepo.downloadFileMessage(
                session = ServiceLocator.session.current(),
                messageId = message.messageId,
                renewExpiredSession = ::renewTencentSession,
                onProgress = { progress ->
                    scope.launch {
                        if (message.messageId in fileDownloadProgress) {
                            fileDownloadProgress = fileDownloadProgress + (message.messageId to progress)
                        }
                    }
                },
            ).onSuccess { path ->
                val downloaded = message.copy(mediaUrl = path)
                messages = messages.map { if (it.messageId == message.messageId) downloaded else it }
                fileDownloadProgress = fileDownloadProgress - message.messageId
                openLocalFile(downloaded)
            }.onFailure {
                fileDownloadProgress = fileDownloadProgress - message.messageId
                error = coachThrowableError("文件下载失败，请重试", it)
            }
        }
    }

    fun openMessageMedia(message: V2IMMessage) {
        if (message.kind == V2IMMessageKind.MERGED) {
            loadMergedMessage(message)
            return
        }
        if (message.kind != V2IMMessageKind.FILE) {
            previewMessage = message
            return
        }
        if (isLocalChatFileSource(message.mediaUrl)) openLocalFile(message) else downloadAndOpenFile(message)
    }

    fun copyText(value: String) {
        if (value.isBlank()) return
        context.copyPlainTextToClipboard("聊天消息", value)
    }

    fun copyMessage(message: V2IMMessage) = copyText(message.text)

    fun deleteMessage(message: V2IMMessage) {
        if (messageActionInFlight != null) return
        messageActionInFlight = message.messageId
        scope.launch {
            ServiceLocator.tencentFriendRepo.deleteMessage(
                session = ServiceLocator.session.current(),
                messageId = message.messageId,
                renewExpiredSession = ::renewTencentSession,
            ).onSuccess {
                messages = messages.filterNot { it.messageId == message.messageId }
                error = null
            }.onFailure {
                error = coachThrowableError("消息删除失败，请重试", it)
            }
            messageActionInFlight = null
        }
    }

    fun recallMessage(message: V2IMMessage) {
        if (messageActionInFlight != null || !canRecallChatMessage(message)) return
        messageActionInFlight = message.messageId
        scope.launch {
            ServiceLocator.tencentFriendRepo.recallMessage(
                session = ServiceLocator.session.current(),
                messageId = message.messageId,
                renewExpiredSession = ::renewTencentSession,
            ).onSuccess {
                messages = messages.map {
                    if (it.messageId == message.messageId) {
                        it.copy(
                            text = "你撤回了一条消息",
                            kind = V2IMMessageKind.SYSTEM,
                            mediaUrl = null,
                            thumbnailUrl = null,
                            fileName = null,
                            fileSizeBytes = null,
                            durationSeconds = null,
                        )
                    } else it
                }
                error = null
            }.onFailure {
                error = coachThrowableError("消息撤回失败，请重试", it)
            }
            messageActionInFlight = null
        }
    }

    fun setReaction(message: V2IMMessage, reactionId: String, shouldReactOverride: Boolean? = null) {
        if (messageActionInFlight != null) return
        val current = messages.firstOrNull { it.messageId == message.messageId } ?: message
        val shouldReact = shouldReactOverride
            ?: (current.reactions.firstOrNull { it.reactionId == reactionId }?.reactedByMe != true)
        val previousReactions = current.reactions
        messageActionInFlight = message.messageId
        messages = messages.map {
            if (it.messageId == message.messageId) {
                it.copy(reactions = toggleChatReaction(it.reactions, reactionId, shouldReact))
            } else it
        }
        scope.launch {
            ServiceLocator.tencentFriendRepo.setMessageReaction(
                session = ServiceLocator.session.current(),
                messageId = message.messageId,
                reactionId = reactionId,
                shouldReact = shouldReact,
                renewExpiredSession = ::renewTencentSession,
            ).onSuccess {
                error = null
            }.onFailure {
                messages = messages.map { candidate ->
                    if (candidate.messageId == message.messageId) candidate.copy(reactions = previousReactions) else candidate
                }
                error = coachThrowableError("表情回应失败，请重试", it)
            }
            messageActionInFlight = null
        }
    }

    fun exitMultiSelect() {
        multiSelectMode = false
        selectedMessageIds = emptySet()
    }

    fun openForwardSelector(messageIds: List<String>, mode: V2ChatForwardMode) {
        forwardMessageIds = messageIds.distinct()
        forwardPlainText = null
        forwardMode = mode
        selectedForwardTargets = emptySet()
        forwardTargetsError = null
        showForwardSelector = true
    }

    fun openTextForwardSelector(text: String) {
        val value = text.trim()
        if (value.isEmpty()) return
        forwardMessageIds = emptyList()
        forwardPlainText = value
        forwardMode = V2ChatForwardMode.SEPARATE
        selectedForwardTargets = emptySet()
        forwardTargetsError = null
        showForwardSelector = true
    }

    fun translateMessage(message: V2IMMessage) {
        if (message.kind != V2IMMessageKind.TEXT || message.text.isBlank() || message.messageId in translatingMessageIds) return
        translatingMessageIds = translatingMessageIds + message.messageId
        scope.launch {
            ServiceLocator.tencentFriendRepo.translateText(
                session = ServiceLocator.session.current(),
                text = message.text,
                targetLanguage = "en",
                renewExpiredSession = ::renewTencentSession,
            ).onSuccess { translated ->
                messages = messages.map { candidate ->
                    if (candidate.messageId == message.messageId) candidate.copy(translatedText = translated) else candidate
                }
                error = null
            }.onFailure {
                error = coachThrowableError("翻译失败", it)
            }
            translatingMessageIds = translatingMessageIds - message.messageId
        }
    }

    fun deleteSelectedMessages() {
        val ids = selectedMessageIds.toList()
        if (ids.isEmpty() || messageActionInFlight != null) return
        messageActionInFlight = ids.first()
        scope.launch {
            ServiceLocator.tencentFriendRepo.deleteMessages(
                session = ServiceLocator.session.current(),
                messageIds = ids,
                renewExpiredSession = ::renewTencentSession,
            ).onSuccess {
                messages = messages.filterNot { it.messageId in ids }
                error = null
                exitMultiSelect()
            }.onFailure {
                error = coachThrowableError("消息删除失败，请重试", it)
            }
            messageActionInFlight = null
        }
    }

    fun forwardSelectedMessages() {
        val ids = selectedMessageIds.toList()
        if (ids.isEmpty()) return
        showForwardTypeDialog = true
    }

    fun sendForward() {
        val generatedText = forwardPlainText
        if ((forwardMessageIds.isEmpty() && generatedText.isNullOrBlank()) || selectedForwardTargets.isEmpty() || forwarding) return
        val sourceIds = forwardMessageIds.toList()
        val targetIds = selectedForwardTargets.toList()
        val sourceMessages = messages.filter { it.messageId in sourceIds }
        val selectedMode = forwardMode
        val ownName = currentSession.nickname ?: currentSession.username ?: "我"
        forwarding = true
        forwardTargetsError = null
        showForwardSelector = false
        exitMultiSelect()
        scope.launch {
            val result = if (!generatedText.isNullOrBlank()) {
                ServiceLocator.tencentFriendRepo.sendTextToUsers(
                    session = ServiceLocator.session.current(),
                    targetUserIds = targetIds,
                    text = generatedText,
                    renewExpiredSession = ::renewTencentSession,
                )
            } else {
                ServiceLocator.tencentFriendRepo.forwardMessages(
                    session = ServiceLocator.session.current(),
                    messageIds = sourceIds,
                    targetUserIds = targetIds,
                    merged = selectedMode == V2ChatForwardMode.MERGED,
                    mergedTitle = chatMergedForwardTitle(
                        sourceMessages, currentSession.userId, ownName, displayTitle,
                    ),
                    abstractList = chatForwardAbstracts(
                        sourceMessages, currentSession.userId, ownName, displayTitle,
                    ),
                    renewExpiredSession = ::renewTencentSession,
                )
            }
            result.onSuccess {
                Toast.makeText(context, "已发送", Toast.LENGTH_SHORT).show()
            }.onFailure {
                error = coachThrowableError("消息转发失败，请重试", it)
                Toast.makeText(context, "消息转发失败，请重试", Toast.LENGTH_SHORT).show()
            }
            forwarding = false
        }
    }

    LaunchedEffect(conversation.userId) {
        loading = true
        showLoadingIndicator = false
        initialScrollComplete = false
        val loadingIndicatorJob = launch {
            delay(500)
            if (loading && messages.isEmpty()) showLoadingIndicator = true
        }
        val firstLoad = withTimeoutOrNull(3_000) { refresh(replaceHistory = true) }
        if (firstLoad == null && messages.isEmpty()) {
            withTimeoutOrNull(3_000) { refresh(replaceHistory = true) }
        }
        loading = false
        showLoadingIndicator = false
        loadingIndicatorJob.cancel()
        while (true) {
            delay(15_000)
            refresh(replaceHistory = false)
        }
    }
    LaunchedEffect(messages.lastOrNull()?.messageId) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
            initialScrollComplete = true
        } else if (!loading) {
            initialScrollComplete = true
        }
    }
    LaunchedEffect(listState, initialScrollComplete, hasMoreOlderMessages) {
        if (!initialScrollComplete || !hasMoreOlderMessages) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex == 0 }
            .distinctUntilChanged()
            .collect { atTop -> if (atTop) loadOlderMessages() }
    }
    LaunchedEffect(showForwardSelector) {
        if (!showForwardSelector) return@LaunchedEffect
        forwardTargetsLoading = true
        forwardTargetsError = null
        ServiceLocator.tencentFriendRepo.conversations(
            session = ServiceLocator.session.current(),
            renewExpiredSession = ::renewTencentSession,
        ).onSuccess {
            forwardTargets = it
        }.onFailure {
            forwardTargetsError = coachThrowableError("最近聊天暂时无法加载", it)
        }
        forwardTargetsLoading = false
    }

    if (showForwardSelector) {
        V2ChatForwardTargetSelector(
            conversations = forwardTargets,
            selectedUserIds = selectedForwardTargets,
            loading = forwardTargetsLoading,
            sending = forwarding,
            error = forwardTargetsError,
            onToggle = { userId ->
                selectedForwardTargets = if (userId in selectedForwardTargets) {
                    selectedForwardTargets - userId
                } else {
                    selectedForwardTargets + userId
                }
            },
            onDismiss = { if (!forwarding) showForwardSelector = false },
            onConfirm = ::sendForward,
        )
        return
    }

    if (showSettings) {
        V2InteractionChatSettingsScreen(
            conversation = conversation.copy(title = displayTitle),
            onBack = { showSettings = false },
            onRemarkChanged = { remark -> if (remark.isNotBlank()) displayTitle = remark },
            onContactDeleted = onBack,
        )
        return
    }

    previewMessage?.let { message ->
        V2InteractionChatMediaPreview(message = message, onClose = { previewMessage = null })
        return
    }

    mergedDetailMessage?.let { message ->
        V2MergedMessageDetailScreen(
            title = message.mergedTitle ?: "聊天记录",
            messages = mergedDetailMessages,
            loading = mergedDetailLoading,
            error = mergedDetailError,
            ownAvatar = currentSession.avatar,
            ownName = currentSession.nickname ?: currentSession.username ?: "我",
            onBack = {
                mergedDetailMessage = null
                mergedDetailMessages = emptyList()
                mergedDetailError = null
            },
            onRetry = { loadMergedMessage(message) },
            onOpenMessage = ::openMessageMedia,
        )
        return
    }

    V2BackHandler {
        if (multiSelectMode) {
            multiSelectMode = false
            selectedMessageIds = emptySet()
        } else {
            onBack()
        }
    }

    Box(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        V2RoomBackground(
            modifier = Modifier.fillMaxSize(),
            imageHeight = 320.dp,
            topFadeHeight = 120.dp,
        )
        Column(Modifier.fillMaxSize()) {
            V2InteractionChatHeader(
                title = displayTitle,
                subtitle = subtitle,
                onBack = onBack,
                onSettings = { showSettings = true },
            )

            when {
                showLoadingIndicator && messages.isEmpty() -> V2InteractionChatLoadingOverlay(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                messages.isEmpty() -> V2InteractionChatEmptySurface(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    if (loadingOlderMessages) {
                        item(key = "older-message-loading") {
                            Box(
                                Modifier.fillMaxWidth().height(40.dp).testTag("chat-loading-older"),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = DSColors.shadowGray,
                                    strokeWidth = 1.5.dp,
                                )
                            }
                        }
                    }
                    itemsIndexed(messages, key = { _, message -> message.messageId }) { index, message ->
                        V2InteractionChatMessageRow(
                            message = message,
                            timeSeparator = chatTimeSeparator(messages, index),
                            peerAvatar = conversation.avatar,
                            peerName = displayTitle,
                            ownAvatar = currentSession.avatar,
                            ownName = currentSession.nickname ?: currentSession.username ?: "我",
                            onOpenMedia = ::openMessageMedia,
                            actionsEnabled = messageActionInFlight == null,
                            onCopy = ::copyMessage,
                            onDelete = ::deleteMessage,
                            onRecall = ::recallMessage,
                            multiSelectMode = multiSelectMode,
                            selected = message.messageId in selectedMessageIds,
                            onToggleSelection = { selected ->
                                selectedMessageIds = if (selected.messageId in selectedMessageIds) {
                                    selectedMessageIds - selected.messageId
                                } else {
                                    selectedMessageIds + selected.messageId
                                }
                            },
                            onForward = { selected ->
                                openForwardSelector(listOf(selected.messageId), V2ChatForwardMode.SEPARATE)
                            },
                            onEnterMultiSelect = { selected ->
                                multiSelectMode = true
                                selectedMessageIds = setOf(selected.messageId)
                            },
                            translating = message.messageId in translatingMessageIds,
                            onTranslate = ::translateMessage,
                            onHideTranslation = { selected ->
                                messages = messages.map { candidate ->
                                    if (candidate.messageId == selected.messageId) candidate.copy(translatedText = null) else candidate
                                }
                            },
                            onForwardTranslation = { selected ->
                                selected.translatedText?.let(::openTextForwardSelector)
                            },
                            onCopyTranslation = { selected ->
                                selected.translatedText?.let(::copyText)
                            },
                            onToggleReaction = { message, reactionId -> setReaction(message, reactionId) },
                            onShowReactionDetails = { reactionDetailMessageId = it.messageId },
                            onRetry = ::retryMessage,
                            fileDownloadProgress = fileDownloadProgress[message.messageId],
                            onDownloadFile = if (message.kind == V2IMMessageKind.FILE) {
                                { openMessageMedia(message) }
                            } else null,
                        )
                    }
                }
            }

            error?.let {
                Text(it, color = Color(0xFFB23B35), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            if (multiSelectMode) {
                V2ChatMultiSelectBottomBar(
                    selectedCount = selectedMessageIds.size,
                    onForward = ::forwardSelectedMessages,
                    onDelete = ::deleteSelectedMessages,
                    onCancel = ::exitMultiSelect,
                )
            } else {
                V2InteractionChatComposer(
                    draft = draft,
                    onDraftChange = { draft = it },
                    busy = sending,
                    uploadProgress = uploadProgress,
                    onSendText = {
                        val value = draft.trim()
                        if (value.isEmpty()) return@V2InteractionChatComposer
                        draft = ""
                        sendOptimistically(
                            V2IMMessage(
                                messageId = "local-${UUID.randomUUID()}",
                                senderUserId = currentSession.userId.orEmpty(),
                                text = value,
                                timeEpochSeconds = Instant.now().epochSecond,
                                isMine = true,
                                deliveryState = V2IMDeliveryState.SENDING,
                            ),
                        )
                    },
                    onSendMedia = ::sendPreparedMedia,
                    onSendVoice = ::sendVoice,
                    onError = { error = it },
                )
            }
        }
    }

    if (showForwardTypeDialog) {
        V2ChatForwardTypeSheet(
            onSeparate = {
                showForwardTypeDialog = false
                if (selectedMessageIds.size > V2_CHAT_SEPARATE_FORWARD_LIMIT) {
                    showSeparateForwardLimitAlert = true
                } else {
                    openForwardSelector(selectedMessageIds.toList(), V2ChatForwardMode.SEPARATE)
                }
            },
            onMerged = {
                showForwardTypeDialog = false
                openForwardSelector(selectedMessageIds.toList(), V2ChatForwardMode.MERGED)
            },
            onDismiss = { showForwardTypeDialog = false },
        )
    }

    if (showSeparateForwardLimitAlert) {
        AlertDialog(
            onDismissRequest = { showSeparateForwardLimitAlert = false },
            title = { Text("转发消息过多，暂不支持逐条转发", color = DSColors.inkGreen) },
            confirmButton = {
                TextButton(onClick = {
                    showSeparateForwardLimitAlert = false
                    openForwardSelector(selectedMessageIds.toList(), V2ChatForwardMode.MERGED)
                }) { Text("合并转发", color = DSColors.fairway) }
            },
            dismissButton = {
                TextButton(onClick = { showSeparateForwardLimitAlert = false }) { Text("取消", color = DSColors.shadowGray) }
            },
            containerColor = Color.White,
        )
    }

    reactionDetailMessageId?.let { messageId ->
        messages.firstOrNull { it.messageId == messageId && it.reactions.isNotEmpty() }?.let { message ->
            V2ChatReactionDetailSheet(
                message = message,
                currentUserId = currentSession.userId,
                onDismiss = { reactionDetailMessageId = null },
                loadUsers = { reactionId ->
                    ServiceLocator.tencentFriendRepo.messageReactionUsers(
                        session = ServiceLocator.session.current(),
                        messageId = message.messageId,
                        reactionId = reactionId,
                        renewExpiredSession = ::renewTencentSession,
                    )
                },
                onRemoveMine = { reactionId -> setReaction(message, reactionId, shouldReactOverride = false) },
            )
        }
    }
}

@Composable
internal fun V2InteractionChatEmptySurface(modifier: Modifier = Modifier) {
    Box(modifier.testTag("interaction-chat-empty"))
}

@Composable
internal fun V2InteractionChatLoadingOverlay(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.testTag("interaction-chat-loading"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = DSColors.shadowGray,
            strokeWidth = 2.dp,
        )
        Text(
            "loading...",
            color = DSColors.shadowGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
internal fun V2InteractionChatHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().v2StatusBarPadding().testTag("interaction-chat-header")) {
        Box(Modifier.fillMaxWidth().height(46.dp), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onSettings)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    title,
                    color = DSColors.inkGreen,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    subtitle,
                    color = DSColors.shadowGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = DSColors.fairway,
                    modifier = Modifier.size(36.dp).clickable(onClick = onBack).padding(9.dp),
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(36.dp).clickable(onClick = onSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.size(22.dp).border(1.25.dp, DSColors.fairway, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.MoreHoriz,
                            contentDescription = "聊天设置",
                            tint = DSColors.fairway,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.06f)))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun V2InteractionChatMessageRow(
    message: V2IMMessage,
    timeSeparator: String?,
    peerAvatar: String?,
    peerName: String,
    ownAvatar: String?,
    ownName: String,
    onOpenMedia: (V2IMMessage) -> Unit,
    actionsEnabled: Boolean = true,
    onCopy: (V2IMMessage) -> Unit = {},
    onDelete: (V2IMMessage) -> Unit = {},
    onRecall: (V2IMMessage) -> Unit = {},
    multiSelectMode: Boolean = false,
    selected: Boolean = false,
    onToggleSelection: (V2IMMessage) -> Unit = {},
    onForward: (V2IMMessage) -> Unit = {},
    onEnterMultiSelect: (V2IMMessage) -> Unit = {},
    translating: Boolean = false,
    onTranslate: (V2IMMessage) -> Unit = {},
    onHideTranslation: (V2IMMessage) -> Unit = {},
    onForwardTranslation: (V2IMMessage) -> Unit = {},
    onCopyTranslation: (V2IMMessage) -> Unit = {},
    onToggleReaction: (V2IMMessage, String) -> Unit = { _, _ -> },
    onShowReactionDetails: (V2IMMessage) -> Unit = {},
    onRetry: (V2IMMessage) -> Unit = {},
    reciprocalBubbleBackground: Color? = null,
    fileDownloadProgress: Int? = null,
    onDownloadFile: (() -> Unit)? = null,
) {
    var actionsExpanded by remember(message.messageId) { mutableStateOf(false) }
    var showFullReactionPicker by remember(message.messageId) { mutableStateOf(false) }
    var showResendConfirmation by remember(message.messageId) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        timeSeparator?.let { time ->
            Text(
                text = time,
                color = DSColors.shadowGray,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp)
                    .testTag("chat-time-${message.messageId}"),
            )
        }
        if (message.kind == V2IMMessageKind.SYSTEM) {
            Text(
                message.text,
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 4.dp),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .then(if (multiSelectMode) Modifier.clickable { onToggleSelection(message) } else Modifier)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                if (multiSelectMode) {
                    Box(
                        modifier = Modifier.size(28.dp).testTag("chat-message-checkbox-${message.messageId}"),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        V2ChatSelectionCircle(selected = selected)
                    }
                    Spacer(Modifier.size(8.dp))
                }
                if (!message.isMine) {
                    V2UserAvatar(
                        avatar = peerAvatar,
                        modifier = Modifier.size(36.dp).testTag("chat-peer-avatar-${message.messageId}"),
                        fallbackText = peerName,
                        fallbackColor = DSColors.mistGreen,
                    )
                    Spacer(Modifier.size(12.dp))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Column(horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start) {
                Row(verticalAlignment = Alignment.Bottom) {
                    if (message.isMine) {
                        V2ChatDeliveryStatusIcon(
                            message = message,
                            onRetry = { showResendConfirmation = true },
                        )
                    }
                    Column(horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start) {
                    Box(Modifier.graphicsLayer { alpha = if (message.deliveryState == V2IMDeliveryState.SENDING) 0.7f else 1f }) {
                        V2InteractionChatMessageBubble(
                            message = message,
                            onOpenMedia = onOpenMedia,
                            onLongPress = {
                                if (actionsEnabled && !multiSelectMode && message.deliveryState == V2IMDeliveryState.SENT) {
                                    actionsExpanded = true
                                }
                            },
                            onTap = if (multiSelectMode) ({ onToggleSelection(message) }) else null,
                            reciprocalBackground = reciprocalBubbleBackground,
                            fileDownloadProgress = fileDownloadProgress,
                            onDownloadFile = onDownloadFile,
                        )
                        DropdownMenu(
                            expanded = actionsExpanded,
                            onDismissRequest = { actionsExpanded = false },
                            shape = RoundedCornerShape(14.dp),
                            containerColor = Color.White,
                        ) {
                            V2ChatReactionQuickPicker(
                                refreshKey = actionsExpanded,
                                onSelect = { reactionId ->
                                    actionsExpanded = false
                                    onToggleReaction(message, reactionId)
                                },
                                onExpand = {
                                    actionsExpanded = false
                                    showFullReactionPicker = true
                                },
                            )
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp)
                                    .background(Color.Black.copy(alpha = 0.06f)),
                            )
                            FlowRow(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                                maxItemsInEachRow = 5,
                            ) {
                                if (message.kind == V2IMMessageKind.TEXT && message.text.isNotBlank()) {
                                    ChatMessageActionButton(Icons.Filled.ContentCopy, "复制") {
                                        actionsExpanded = false
                                        onCopy(message)
                                    }
                                }
                                if (canRecallChatMessage(message)) {
                                    ChatMessageActionButton(Icons.AutoMirrored.Filled.Undo, "撤回") {
                                        actionsExpanded = false
                                        onRecall(message)
                                    }
                                }
                                ChatMessageActionButton(Icons.Filled.Delete, "删除") {
                                    actionsExpanded = false
                                    onDelete(message)
                                }
                                ChatMessageActionButton(Icons.AutoMirrored.Filled.Reply, "转发") {
                                    actionsExpanded = false
                                    onForward(message)
                                }
                                if (message.kind == V2IMMessageKind.TEXT && message.text.isNotBlank() && message.translatedText == null) {
                                    ChatMessageActionButton(Icons.Filled.Translate, "翻译") {
                                        actionsExpanded = false
                                        onTranslate(message)
                                    }
                                }
                                ChatMessageActionButton(Icons.Filled.CheckCircle, "多选") {
                                    actionsExpanded = false
                                    onEnterMultiSelect(message)
                                }
                            }
                        }
                    }
                    if (message.kind == V2IMMessageKind.TEXT && (translating || !message.translatedText.isNullOrBlank())) {
                        Spacer(Modifier.height(6.dp))
                        V2ChatTranslationBubble(
                            translatedText = message.translatedText,
                            isMine = message.isMine,
                            translating = translating,
                            onHide = { onHideTranslation(message) },
                            onForward = { onForwardTranslation(message) },
                            onCopy = { onCopyTranslation(message) },
                        )
                    }
                    V2ChatReactionBar(
                        reactions = message.reactions,
                        onClick = { onShowReactionDetails(message) },
                    )
                    }
                    if (!message.isMine) {
                        V2ChatDeliveryStatusIcon(
                            message = message,
                            onRetry = { showResendConfirmation = true },
                        )
                    }
                }
                if (message.deliveryState == V2IMDeliveryState.VIOLATION) {
                    Text(
                        "涉及敏感内容",
                        color = Color(0xFFD63C32),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp).testTag("chat-violation-hint-${message.messageId}"),
                    )
                }
                }
                if (message.isMine) {
                    Spacer(Modifier.size(12.dp))
                    V2UserAvatar(
                        avatar = ownAvatar,
                        modifier = Modifier.size(36.dp).testTag("chat-own-avatar-${message.messageId}"),
                        fallbackText = ownName,
                        fallbackColor = DSColors.mistGreen,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    if (showFullReactionPicker) {
        V2ChatReactionPickerSheet(
            onDismiss = { showFullReactionPicker = false },
            onSelect = { reactionId ->
                showFullReactionPicker = false
                onToggleReaction(message, reactionId)
            },
        )
    }
    if (showResendConfirmation) {
        AlertDialog(
            onDismissRequest = { showResendConfirmation = false },
            title = { Text("确定重发此消息吗? ", color = DSColors.inkGreen) },
            confirmButton = {
                TextButton(onClick = {
                    showResendConfirmation = false
                    onRetry(message)
                }) { Text("确定", color = DSColors.fairway) }
            },
            dismissButton = {
                TextButton(onClick = { showResendConfirmation = false }) {
                    Text("取消", color = DSColors.shadowGray)
                }
            },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun V2ChatDeliveryStatusIcon(
    message: V2IMMessage,
    onRetry: () -> Unit,
) {
    if (message.deliveryState == V2IMDeliveryState.SENT) return
    Box(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp).size(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (message.deliveryState) {
            V2IMDeliveryState.SENDING -> CircularProgressIndicator(
                modifier = Modifier.size(15.dp).testTag("chat-sending-${message.messageId}"),
                color = DSColors.shadowGray,
                strokeWidth = 1.5.dp,
            )
            V2IMDeliveryState.FAILED -> Icon(
                Icons.Filled.Error,
                contentDescription = "消息发送失败，点击重试",
                tint = Color(0xFFD63C32),
                modifier = Modifier.size(18.dp).testTag("chat-failed-${message.messageId}").clickable(onClick = onRetry),
            )
            V2IMDeliveryState.VIOLATION -> Icon(
                Icons.Filled.Error,
                contentDescription = "消息内容违规，发送失败",
                tint = Color(0xFFD63C32),
                modifier = Modifier.size(18.dp).testTag("chat-violation-${message.messageId}"),
            )
            V2IMDeliveryState.SENT -> Unit
        }
    }
}

@Composable
private fun ChatMessageActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(42.dp).height(42.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = label, tint = DSColors.fairway, modifier = Modifier.size(18.dp))
        Text(label, color = DSColors.shadowGray, fontSize = 10.sp)
    }
}

internal fun chatFileMimeType(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "").lowercase(Locale.US)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        ?: "application/octet-stream"
}

internal fun canRecallChatMessage(
    message: V2IMMessage,
    nowEpochSeconds: Long = Instant.now().epochSecond,
): Boolean {
    val age = nowEpochSeconds - message.timeEpochSeconds
    return message.isMine && message.kind != V2IMMessageKind.SYSTEM && age in 0L..<120L
}

internal fun mergeChatMessagePage(
    current: List<V2IMMessage>,
    incoming: List<V2IMMessage>,
    replaceHistory: Boolean,
): List<V2IMMessage> {
    val translations = current.mapNotNull { message ->
        message.translatedText?.let { translated -> message.messageId to translated }
    }.toMap()
    val normalizedIncoming = incoming.map { message ->
        message.copy(translatedText = translations[message.messageId] ?: message.translatedText)
    }
    val retained = if (replaceHistory) {
        current.filter { message ->
            message.messageId.startsWith("local-") && message.deliveryState != V2IMDeliveryState.SENT
        }
    } else {
        current
    }
    return (retained + normalizedIncoming).associateBy(V2IMMessage::messageId).values
        .sortedWith(compareBy(V2IMMessage::timeEpochSeconds).thenBy(V2IMMessage::messageId))
}

internal fun chatTimeSeparator(
    messages: List<V2IMMessage>,
    index: Int,
    nowEpochSeconds: Long = Instant.now().epochSecond,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String? {
    val message = messages.getOrNull(index) ?: return null
    if (index > 0) {
        val previous = messages.getOrNull(index - 1) ?: return null
        if (message.timeEpochSeconds - previous.timeEpochSeconds <= 60L) return null
    }
    return runCatching {
        val date = Instant.ofEpochSecond(message.timeEpochSeconds).atZone(zoneId)
        val now = Instant.ofEpochSecond(nowEpochSeconds).atZone(zoneId)
        when {
            date.toLocalDate() == now.toLocalDate() -> date.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA))
            date.year == now.year && date.month == now.month &&
                date.get(WeekFields.of(DayOfWeek.SATURDAY, 1).weekOfMonth()) ==
                now.get(WeekFields.of(DayOfWeek.SATURDAY, 1).weekOfMonth()) -> when (date.dayOfWeek) {
                    DayOfWeek.MONDAY -> "星期一"
                    DayOfWeek.TUESDAY -> "星期二"
                    DayOfWeek.WEDNESDAY -> "星期三"
                    DayOfWeek.THURSDAY -> "星期四"
                    DayOfWeek.FRIDAY -> "星期五"
                    DayOfWeek.SATURDAY -> "星期六"
                    DayOfWeek.SUNDAY -> "星期日"
                }
            date.year == now.year -> date.format(DateTimeFormatter.ofPattern("MM/dd", Locale.CHINA))
            else -> date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.CHINA))
        }
    }.getOrNull()
}
