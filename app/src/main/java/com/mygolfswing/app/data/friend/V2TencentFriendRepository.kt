package com.mygolfswing.app.data.friend

import android.content.Context
import com.mygolfswing.app.data.net.V2ApiConfig
import com.mygolfswing.app.data.session.V2Session
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMConversationResult
import com.tencent.imsdk.v2.V2TIMDownloadCallback
import com.tencent.imsdk.v2.V2TIMFriendAddApplication
import com.tencent.imsdk.v2.V2TIMFriendApplication
import com.tencent.imsdk.v2.V2TIMFriendInfo
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMMergerElem
import com.tencent.imsdk.v2.V2TIMMessage
import com.tencent.imsdk.v2.V2TIMSendCallback
import com.tencent.imsdk.v2.V2TIMSDKConfig
import com.tencent.imsdk.v2.V2TIMSDKListener
import com.tencent.imsdk.v2.V2TIMValueCallback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 腾讯 IM 好友关系的最小无 UI 接入。
 *
 * 挥呗空间仍由 coachWorkspace 云函数负责权限校验；这里仅把 SDK 中真实好友 ID
 * 交给后端做双向声明/IM REST 复核，客户端列表本身不授予空间访问权限。
 */
class V2TencentFriendRepository(
    context: Context,
    private val isDebuggable: Boolean,
) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private val manager: V2TIMManager get() = V2TIMManager.getInstance()
    private var initializedSdkAppId: Int? = null
    private var sdkListenerRegistered = false
    private val rawMessagesById = ConcurrentHashMap<String, V2TIMMessage>()
    private val _sessionEvents = MutableSharedFlow<V2TencentSessionEvent>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    internal val sessionEvents: SharedFlow<V2TencentSessionEvent> = _sessionEvents.asSharedFlow()
    private val sdkSessionListener = createTencentSdkSessionListener { event ->
        _sessionEvents.tryEmit(event)
    }

    suspend fun friendUserIds(
        session: V2Session,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<List<String>> = try {
        Result.success(
            mutex.withLock {
                var activeSession = session.requireTencentCredentials()
                ensureInitialized(activeSession.sdkAppId ?: V2ApiConfig.IM_SDK_APP_ID)
                try {
                    ensureLoggedIn(activeSession)
                    fetchFriendUserIds(activeSession.userId.orEmpty())
                } catch (error: V2TencentIMException) {
                    if (!error.isExpiredUserSig) throw error
                    activeSession = renewExpiredSession().requireTencentCredentials()
                    ensureInitialized(activeSession.sdkAppId ?: V2ApiConfig.IM_SDK_APP_ID)
                    ensureLoggedIn(activeSession, forceRelogin = true)
                    fetchFriendUserIds(activeSession.userId.orEmpty())
                }
            },
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }

    suspend fun sendFriendRequest(
        session: V2Session,
        targetUserId: String,
        renewExpiredSession: suspend () -> V2Session,
        addWording: String = "你好，我想加你为好友",
    ): Result<V2FriendRequestOutcome> = try {
        Result.success(
            mutex.withLock {
                val target = targetUserId.trim()
                require(target.isNotEmpty() && target != session.userId) { "不能添加当前账号" }
                var activeSession = session.requireTencentCredentials()
                ensureInitialized(activeSession.sdkAppId ?: V2ApiConfig.IM_SDK_APP_ID)
                try {
                    ensureLoggedIn(activeSession)
                    awaitAddFriend(target, addWording)
                } catch (error: V2TencentIMException) {
                    if (!error.isExpiredUserSig) throw error
                    activeSession = renewExpiredSession().requireTencentCredentials()
                    ensureInitialized(activeSession.sdkAppId ?: V2ApiConfig.IM_SDK_APP_ID)
                    ensureLoggedIn(activeSession, forceRelogin = true)
                    awaitAddFriend(target, addWording)
                }
            },
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }

    suspend fun incomingFriendRequests(
        session: V2Session,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<List<V2IncomingFriendRequest>> = withTencentSession(session, renewExpiredSession) {
        awaitIncomingFriendRequests()
    }

    /** Read-only destination used by the contacts page, matching iOS ContactList. */
    suspend fun blockedUserIds(
        session: V2Session,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<List<String>> = withTencentSession(session, renewExpiredSession) {
        awaitBlackListUserIds().toList().sorted()
    }

    /** The C2C list shown under “最近对话”; teaching-space unread remains separate. */
    suspend fun conversations(
        session: V2Session,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<List<V2IMConversation>> = withTencentSession(session, renewExpiredSession) {
        awaitConversationList()
    }

    suspend fun conversationMessages(
        session: V2Session,
        userId: String,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<List<V2IMMessage>> = conversationMessagePage(
        session = session,
        userId = userId,
        beforeMessageId = null,
        pageSize = 50,
        renewExpiredSession = renewExpiredSession,
    ).map(V2IMMessagePage::messages)

    suspend fun conversationMessagePage(
        session: V2Session,
        userId: String,
        beforeMessageId: String? = null,
        pageSize: Int = 20,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<V2IMMessagePage> = withTencentSession(session, renewExpiredSession) {
        val target = userId.trim()
        require(target.isNotEmpty() && target != session.userId) { "聊天对象不可用" }
        val count = pageSize.coerceIn(1, 100)
        val startMessage = beforeMessageId?.takeIf(String::isNotBlank)?.let { messageId ->
            rawMessagesById[messageId] ?: resolveRawMessage(messageId)
        }
        val messages = awaitC2CHistory(target, count, startMessage)
        if (beforeMessageId == null) awaitMarkC2CRead(target)
        messages.forEach { message -> rawMessagesById[rawMessageId(message)] = message }
        val reactionsByMessageId = try {
            awaitMessageReactions(messages)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            // Reaction availability must not prevent the conversation itself from loading.
            emptyMap()
        }
        val mapped = coroutineScope {
            messages.map { message ->
                async { toIMMessage(message, reactionsByMessageId[rawMessageId(message)].orEmpty()) }
            }.awaitAll().filterNotNull()
        }.sortedBy(V2IMMessage::timeEpochSeconds)
        V2IMMessagePage(
            messages = mapped,
            hasMoreOlder = messages.size >= count,
        )
    }

    /** Downloads the real messages behind a Tencent merged-forward card. */
    suspend fun mergedMessageContents(
        session: V2Session,
        messageId: String,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<List<V2IMMessage>> = withTencentSession(session, renewExpiredSession) {
        val source = resolveRawMessage(messageId)
        val merger = source.mergerElem ?: throw IllegalStateException("该消息不是聊天记录")
        require(!merger.isLayersOverLimit) { "聊天记录层级过多，暂时无法展开" }
        val messages = awaitDownloadMergerMessages(merger)
        messages.forEach { message -> rawMessagesById[rawMessageId(message)] = message }
        val reactionsByMessageId = try {
            awaitMessageReactions(messages)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            emptyMap()
        }
        coroutineScope {
            messages.map { message ->
                async { toIMMessage(message, reactionsByMessageId[rawMessageId(message)].orEmpty()) }
            }.awaitAll().filterNotNull()
        }.sortedBy(V2IMMessage::timeEpochSeconds)
    }

    /** Downloads a Tencent file element into the same private chat cache used by iOS QuickLook. */
    suspend fun downloadFileMessage(
        session: V2Session,
        messageId: String,
        renewExpiredSession: suspend () -> V2Session,
        onProgress: (Int) -> Unit = {},
    ): Result<String> = withTencentSession(session, renewExpiredSession) {
        val source = resolveRawMessage(messageId)
        val element = source.fileElem ?: throw IllegalStateException("该消息不是文件")
        val target = chatFileCacheTarget(appContext.cacheDir, messageId, element.fileName)
        target.parentFile?.mkdirs()
        val expectedBytes = element.fileSize.toLong().coerceAtLeast(0L)
        if (target.isFile && target.length() > 0L && (expectedBytes == 0L || target.length() == expectedBytes)) {
            return@withTencentSession target.absolutePath
        }

        val partial = File(target.parentFile, "${target.name}.part")
        partial.delete()
        try {
            awaitDownloadFile(element, partial, onProgress)
            require(partial.isFile && partial.length() > 0L) { "文件下载结果为空" }
            if (expectedBytes > 0L) require(partial.length() == expectedBytes) { "文件下载不完整" }
            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            target.absolutePath
        } catch (error: Throwable) {
            partial.delete()
            throw error
        }
    }

    suspend fun sendTextMessage(
        session: V2Session,
        userId: String,
        text: String,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<V2IMMessage> = withTencentSession(session, renewExpiredSession) {
        val target = userId.trim()
        val value = text.trim().take(2_000)
        require(target.isNotEmpty() && target != session.userId) { "聊天对象不可用" }
        require(value.isNotEmpty()) { "请输入消息" }
        toIMMessage(awaitSendC2CText(target, value))
            ?: throw IllegalStateException("消息已发送，但返回内容不完整")
    }

    /** Delete a single message through the same Tencent operation used by iOS ChatUIKit. */
    suspend fun deleteMessage(
        session: V2Session,
        messageId: String,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<Unit> = withTencentSession(session, renewExpiredSession) {
        val message = resolveRawMessage(messageId)
        awaitDeleteMessages(listOf(message))
        rawMessagesById.remove(messageId)
    }

    /** Delete all selected local conversation messages in one Tencent operation, matching iOS multi-select. */
    suspend fun deleteMessages(
        session: V2Session,
        messageIds: List<String>,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<Unit> = withTencentSession(session, renewExpiredSession) {
        val ids = messageIds.map(String::trim).filter(String::isNotEmpty).distinct()
        require(ids.isNotEmpty()) { "请选择消息" }
        val messages = ids.map { resolveRawMessage(it) }
        awaitDeleteMessages(messages)
        ids.forEach(rawMessagesById::remove)
    }

    /**
     * Forward selected C2C messages through Tencent IM. Separate forwarding preserves each original
     * message; merged forwarding sends one chat-history card, just like iOS ChatUIKit.
     */
    suspend fun forwardMessages(
        session: V2Session,
        messageIds: List<String>,
        targetUserIds: List<String>,
        merged: Boolean,
        mergedTitle: String,
        abstractList: List<String>,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<Unit> = withTencentSession(session, renewExpiredSession) {
        val ids = messageIds.map(String::trim).filter(String::isNotEmpty).distinct()
        val targets = targetUserIds.map(String::trim).filter(String::isNotEmpty).distinct()
        require(ids.isNotEmpty()) { "请选择消息" }
        require(targets.isNotEmpty()) { "请选择会话" }
        require(merged || ids.size <= 30) { "转发消息过多，暂不支持逐条转发" }
        val sourceMessages = ids.map { resolveRawMessage(it) }
        val messageManager = V2TIMManager.getMessageManager()
        for (target in targets) {
            require(target != session.userId) { "不能转发给当前账号" }
            if (merged) {
                val message = messageManager.createMergerMessage(
                    sourceMessages,
                    mergedTitle.ifBlank { "聊天记录" },
                    abstractList.take(4),
                    "Merged messages",
                )
                awaitSendC2CMessage(target, message) {}
            } else {
                sourceMessages.forEach { source ->
                    awaitSendC2CMessage(target, messageManager.createForwardMessage(source)) {}
                }
            }
        }
    }

    /** Forward generated auxiliary text (for example a translation) as a new text message. */
    suspend fun sendTextToUsers(
        session: V2Session,
        targetUserIds: List<String>,
        text: String,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<Unit> = withTencentSession(session, renewExpiredSession) {
        val targets = targetUserIds.map(String::trim).filter(String::isNotEmpty).distinct()
        val value = text.trim().take(2_000)
        require(targets.isNotEmpty()) { "请选择会话" }
        require(value.isNotEmpty()) { "没有可转发的文字" }
        targets.forEach { target ->
            require(target != session.userId) { "不能转发给当前账号" }
            awaitSendC2CText(target, value)
        }
    }

    /** Recall is available only for the sender and remains subject to Tencent's server window. */
    suspend fun recallMessage(
        session: V2Session,
        messageId: String,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<Unit> = withTencentSession(session, renewExpiredSession) {
        val message = resolveRawMessage(messageId)
        require(message.isSelf) { "只能撤回自己发送的消息" }
        awaitRecallMessage(message)
    }

    /** Uses Tencent's cloud reaction state, matching the reaction picker enabled by iOS ChatUIKit. */
    suspend fun setMessageReaction(
        session: V2Session,
        messageId: String,
        reactionId: String,
        shouldReact: Boolean,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<Unit> = withTencentSession(session, renewExpiredSession) {
        val normalizedReactionId = reactionId.trim()
        require(normalizedReactionId.startsWith("[TUIEmoji_") && normalizedReactionId.endsWith("]")) {
            "表情回应不可用"
        }
        val message = resolveRawMessage(messageId)
        awaitSetMessageReaction(message, normalizedReactionId, shouldReact)
    }

    suspend fun messageReactionUsers(
        session: V2Session,
        messageId: String,
        reactionId: String,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<List<V2IMReactionUser>> = withTencentSession(session, renewExpiredSession) {
        awaitMessageReactionUsers(resolveRawMessage(messageId), reactionId)
    }

    /** Translate plain message text with the same Tencent IM service used by iOS ChatUIKit. */
    suspend fun translateText(
        session: V2Session,
        text: String,
        targetLanguage: String = "en",
        renewExpiredSession: suspend () -> V2Session,
    ): Result<String> = withTencentSession(session, renewExpiredSession) {
        val source = text.trim()
        require(source.isNotEmpty()) { "没有可翻译的文字" }
        awaitTranslateText(source, targetLanguage.ifBlank { "en" })
    }

    suspend fun sendImageMessage(
        session: V2Session,
        userId: String,
        absolutePath: String,
        renewExpiredSession: suspend () -> V2Session,
        onProgress: (Int) -> Unit = {},
    ): Result<V2IMMessage> = sendMediaMessage(session, userId, renewExpiredSession, onProgress) {
        V2TIMManager.getMessageManager().createImageMessage(absolutePath)
    }

    suspend fun sendVideoMessage(
        session: V2Session,
        userId: String,
        absolutePath: String,
        snapshotPath: String,
        durationSeconds: Int,
        renewExpiredSession: suspend () -> V2Session,
        onProgress: (Int) -> Unit = {},
    ): Result<V2IMMessage> = sendMediaMessage(session, userId, renewExpiredSession, onProgress) {
        V2TIMManager.getMessageManager().createVideoMessage(
            absolutePath,
            "mp4",
            durationSeconds.coerceAtLeast(1),
            snapshotPath,
        )
    }

    suspend fun sendVoiceMessage(
        session: V2Session,
        userId: String,
        absolutePath: String,
        durationSeconds: Int,
        renewExpiredSession: suspend () -> V2Session,
        onProgress: (Int) -> Unit = {},
    ): Result<V2IMMessage> = sendMediaMessage(session, userId, renewExpiredSession, onProgress) {
        V2TIMManager.getMessageManager().createSoundMessage(absolutePath, durationSeconds.coerceAtLeast(1))
    }

    suspend fun sendFileMessage(
        session: V2Session,
        userId: String,
        absolutePath: String,
        fileName: String,
        renewExpiredSession: suspend () -> V2Session,
        onProgress: (Int) -> Unit = {},
    ): Result<V2IMMessage> = sendMediaMessage(session, userId, renewExpiredSession, onProgress) {
        V2TIMManager.getMessageManager().createFileMessage(absolutePath, fileName.take(255))
    }

    private suspend fun sendMediaMessage(
        session: V2Session,
        userId: String,
        renewExpiredSession: suspend () -> V2Session,
        onProgress: (Int) -> Unit,
        create: () -> V2TIMMessage,
    ): Result<V2IMMessage> = withTencentSession(session, renewExpiredSession) {
        val target = userId.trim()
        require(target.isNotEmpty() && target != session.userId) { "聊天对象不可用" }
        toIMMessage(awaitSendC2CMessage(target, create(), onProgress))
            ?: throw IllegalStateException("消息已发送，但返回内容不完整")
    }

    suspend fun c2cSettings(
        session: V2Session,
        userId: String,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<V2C2CSettings> = withTencentSession(session, renewExpiredSession) {
        val target = userId.trim()
        require(target.isNotEmpty() && target != session.userId) { "聊天对象不可用" }
        val friendInfo = awaitFriendInfo(target)
        val conversation = runCatching { awaitConversation("c2c_$target") }.getOrNull()
        val receiveOption = runCatching { awaitC2CReceiveOption(target) }.getOrNull()
        val blocked = runCatching { awaitBlackListUserIds() }.getOrDefault(emptySet())
        V2C2CSettings(
            userId = target,
            remark = friendInfo?.friendRemark?.trim().orEmpty(),
            isFriend = friendInfo != null,
            isMuted = receiveOption != null &&
                receiveOption != V2TIMMessage.V2TIM_RECEIVE_MESSAGE,
            isPinned = conversation?.isPinned == true,
            isBlocked = target in blocked,
        )
    }

    suspend fun setFriendRemark(
        session: V2Session,
        userId: String,
        remark: String,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<Unit> = withTencentSession(session, renewExpiredSession) {
        awaitSetFriendRemark(userId.trim(), remark.trim().take(40))
    }

    suspend fun setC2CMuted(
        session: V2Session,
        userId: String,
        muted: Boolean,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<Unit> = withTencentSession(session, renewExpiredSession) {
        awaitSetC2CReceiveOption(
            userId.trim(),
            if (muted) V2TIMMessage.V2TIM_RECEIVE_NOT_NOTIFY_MESSAGE else V2TIMMessage.V2TIM_RECEIVE_MESSAGE,
        )
    }

    suspend fun setC2CPinned(
        session: V2Session,
        userId: String,
        pinned: Boolean,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<Unit> = withTencentSession(session, renewExpiredSession) {
        awaitPinConversation("c2c_${userId.trim()}", pinned)
    }

    suspend fun setBlocked(
        session: V2Session,
        userId: String,
        blocked: Boolean,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<Unit> = withTencentSession(session, renewExpiredSession) {
        awaitSetBlocked(userId.trim(), blocked)
    }

    suspend fun clearC2CHistory(
        session: V2Session,
        userId: String,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<Unit> = withTencentSession(session, renewExpiredSession) {
        awaitClearC2CHistory(userId.trim())
    }

    suspend fun deleteFriend(
        session: V2Session,
        userId: String,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<Unit> = withTencentSession(session, renewExpiredSession) {
        val target = userId.trim()
        awaitDeleteFriend(target)
        runCatching { awaitDeleteConversation("c2c_$target") }
        Unit
    }

    suspend fun acceptFriendRequest(
        session: V2Session,
        targetUserId: String,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<V2FriendRequestOutcome> = withTencentSession(session, renewExpiredSession) {
        val application = awaitIncomingFriendApplications().firstOrNull { it.userID == targetUserId }
            ?: throw IllegalStateException("这条好友申请已经处理，请刷新")
        awaitAcceptFriend(application)
        V2FriendRequestOutcome.ACCEPTED
    }

    suspend fun rejectFriendRequest(
        session: V2Session,
        targetUserId: String,
        renewExpiredSession: suspend () -> V2Session,
    ): Result<Unit> = withTencentSession(session, renewExpiredSession) {
        val application = awaitIncomingFriendApplications().firstOrNull { it.userID == targetUserId }
            ?: throw IllegalStateException("这条好友申请已经处理，请刷新")
        awaitRejectFriend(application)
    }

    suspend fun logout() {
        mutex.withLock {
            if (initializedSdkAppId == null || manager.loginStatus == V2TIMManager.V2TIM_STATUS_LOGOUT) return
            runCatching { awaitLogout() }
        }
    }

    /** Proactively reconnect after the SDK reports an expired UserSig. */
    suspend fun reconnect(session: V2Session): Result<Unit> = try {
        Result.success(
            mutex.withLock {
                val activeSession = session.requireTencentCredentials()
                ensureInitialized(activeSession.sdkAppId ?: V2ApiConfig.IM_SDK_APP_ID)
                ensureLoggedIn(activeSession, forceRelogin = true)
            },
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }

    private fun ensureInitialized(sdkAppId: Int) {
        require(sdkAppId > 0) { "腾讯 IM 配置不可用" }
        if (initializedSdkAppId == sdkAppId) return
        if (initializedSdkAppId != null) {
            if (sdkListenerRegistered) manager.removeIMSDKListener(sdkSessionListener)
            manager.unInitSDK()
            sdkListenerRegistered = false
        }
        val config = V2TIMSDKConfig().apply {
            logLevel = if (isDebuggable) V2TIMSDKConfig.V2TIM_LOG_INFO else V2TIMSDKConfig.V2TIM_LOG_WARN
        }
        check(manager.initSDK(appContext, sdkAppId, config)) { "腾讯 IM 初始化失败" }
        manager.addIMSDKListener(sdkSessionListener)
        sdkListenerRegistered = true
        initializedSdkAppId = sdkAppId
    }

    private suspend fun ensureLoggedIn(session: V2Session, forceRelogin: Boolean = false) {
        val userId = session.userId.orEmpty()
        val userSig = session.userSig.orEmpty()
        val alreadyLoggedIn = manager.loginStatus == V2TIMManager.V2TIM_STATUS_LOGINED &&
            manager.loginUser == userId
        if (alreadyLoggedIn && !forceRelogin) return
        if (manager.loginStatus != V2TIMManager.V2TIM_STATUS_LOGOUT) awaitLogout()
        awaitLogin(userId, userSig)
    }

    private suspend fun awaitLogin(userId: String, userSig: String) = suspendCancellableCoroutine { continuation ->
        manager.login(userId, userSig, object : V2TIMCallback {
            override fun onSuccess() {
                if (continuation.isActive) continuation.resume(Unit)
            }

            override fun onError(code: Int, desc: String?) {
                if (continuation.isActive) continuation.resumeWithException(V2TencentIMException(code, desc))
            }
        })
    }

    private suspend fun awaitLogout() = suspendCancellableCoroutine { continuation ->
        manager.logout(object : V2TIMCallback {
            override fun onSuccess() {
                if (continuation.isActive) continuation.resume(Unit)
            }

            override fun onError(code: Int, desc: String?) {
                if (continuation.isActive) continuation.resumeWithException(V2TencentIMException(code, desc))
            }
        })
    }

    private suspend fun fetchFriendUserIds(currentUserId: String): List<String> =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getFriendshipManager().getFriendList(
                object : V2TIMValueCallback<List<V2TIMFriendInfo>> {
                    override fun onSuccess(friends: List<V2TIMFriendInfo>?) {
                        if (!continuation.isActive) return
                        continuation.resume(
                            normalizeTencentFriendUserIds(
                                rawUserIds = friends.orEmpty().map(V2TIMFriendInfo::getUserID),
                                currentUserId = currentUserId,
                            ),
                        )
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) continuation.resumeWithException(V2TencentIMException(code, desc))
                    }
                },
            )
        }

    private suspend fun awaitConversationList(): List<V2IMConversation> {
        val rows = mutableListOf<V2TIMConversation>()
        var nextSeq = 0L
        var pageCount = 0
        do {
            val page = awaitConversationPage(nextSeq)
            rows += page.conversationList.orEmpty()
            nextSeq = page.nextSeq
            pageCount += 1
        } while (!page.isFinished && pageCount < 5)
        return rows.asSequence()
            .filter { it.type == V2TIMConversation.V2TIM_C2C }
            .mapNotNull { conversation ->
                val userId = conversation.userID?.trim().orEmpty()
                if (userId.isEmpty() || conversation.conversationID != "c2c_$userId") return@mapNotNull null
                val lastMessage = conversation.lastMessage
                V2IMConversation(
                    userId = userId,
                    title = conversation.showName?.trim()?.takeIf(String::isNotEmpty) ?: userId,
                    avatar = conversation.faceUrl?.trim()?.takeIf(String::isNotEmpty),
                    preview = if (lastMessage?.status == V2TIMMessage.V2TIM_MSG_STATUS_LOCAL_REVOKED) {
                        if (lastMessage.isSelf) "你撤回了一条消息" else "对方撤回了一条消息"
                    } else {
                        conversationPreview(
                            text = lastMessage?.textElem?.text,
                            hasImage = lastMessage?.imageElem != null,
                            hasVideo = lastMessage?.videoElem != null,
                            hasVoice = lastMessage?.soundElem != null,
                            hasFile = lastMessage?.fileElem != null,
                            hasMerged = lastMessage?.mergerElem != null,
                            hasMessage = lastMessage != null,
                        )
                    },
                    timeEpochSeconds = lastMessage?.timestamp?.takeIf { it > 0L },
                    unreadCount = conversation.unreadCount.coerceAtLeast(0),
                )
            }
            .sortedWith(compareByDescending<V2IMConversation> { it.timeEpochSeconds ?: Long.MIN_VALUE })
            .toList()
    }

    private suspend fun awaitConversationPage(nextSeq: Long): V2TIMConversationResult =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getConversationManager().getConversationList(
                nextSeq,
                100,
                object : V2TIMValueCallback<V2TIMConversationResult> {
                    override fun onSuccess(result: V2TIMConversationResult?) {
                        if (!continuation.isActive) return
                        if (result != null) continuation.resume(result)
                        else continuation.resumeWithException(IllegalStateException("最近对话返回为空"))
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(V2TencentIMException(code, desc, "最近对话暂时无法同步"))
                        }
                    }
                },
            )
        }

    private suspend fun awaitC2CHistory(
        userId: String,
        count: Int,
        startMessage: V2TIMMessage?,
    ): List<V2TIMMessage> =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getMessageManager().getC2CHistoryMessageList(
                userId,
                count,
                startMessage,
                object : V2TIMValueCallback<List<V2TIMMessage>> {
                    override fun onSuccess(messages: List<V2TIMMessage>?) {
                        if (continuation.isActive) continuation.resume(messages.orEmpty())
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(V2TencentIMException(code, desc, "聊天记录暂时无法加载"))
                        }
                    }
                },
            )
        }

    private suspend fun awaitMarkC2CRead(userId: String) = suspendCancellableCoroutine { continuation ->
        V2TIMManager.getMessageManager().markC2CMessageAsRead(userId, object : V2TIMCallback {
            override fun onSuccess() {
                if (continuation.isActive) continuation.resume(Unit)
            }

            override fun onError(code: Int, desc: String?) {
                if (continuation.isActive) {
                    continuation.resumeWithException(V2TencentIMException(code, desc, "消息暂时无法标记已读"))
                }
            }
        })
    }

    private suspend fun awaitSendC2CText(userId: String, text: String): V2TIMMessage =
        suspendCancellableCoroutine { continuation ->
            manager.sendC2CTextMessage(text, userId, object : V2TIMValueCallback<V2TIMMessage> {
                override fun onSuccess(message: V2TIMMessage?) {
                    if (!continuation.isActive) return
                    if (message != null) continuation.resume(message)
                    else continuation.resumeWithException(IllegalStateException("消息发送结果为空"))
                }

                override fun onError(code: Int, desc: String?) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(V2TencentIMException(code, desc, "消息发送失败"))
                    }
                }
            })
        }

    private suspend fun awaitSendC2CMessage(
        userId: String,
        message: V2TIMMessage,
        onProgress: (Int) -> Unit,
    ): V2TIMMessage = suspendCancellableCoroutine { continuation ->
        V2TIMManager.getMessageManager().sendMessage(
            message,
            userId,
            "",
            V2TIMMessage.V2TIM_PRIORITY_DEFAULT,
            false,
            null,
            object : V2TIMSendCallback<V2TIMMessage> {
                override fun onProgress(progress: Int) {
                    onProgress(progress.coerceIn(0, 100))
                }

                override fun onSuccess(value: V2TIMMessage?) {
                    if (!continuation.isActive) return
                    if (value != null) continuation.resume(value)
                    else continuation.resumeWithException(IllegalStateException("媒体消息发送结果为空"))
                }

                override fun onError(code: Int, desc: String?) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(V2TencentIMException(code, desc, "媒体消息发送失败"))
                    }
                }
            },
        )
    }

    private suspend fun awaitFriendInfo(userId: String): V2TIMFriendInfo? =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getFriendshipManager().getFriendsInfo(
                listOf(userId),
                object : V2TIMValueCallback<List<com.tencent.imsdk.v2.V2TIMFriendInfoResult>> {
                    override fun onSuccess(results: List<com.tencent.imsdk.v2.V2TIMFriendInfoResult>?) {
                        if (!continuation.isActive) return
                        val result = results.orEmpty().firstOrNull()
                        if (result == null || result.resultCode != 0 || result.relation == 0) continuation.resume(null)
                        else continuation.resume(result.friendInfo)
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) continuation.resumeWithException(V2TencentIMException(code, desc, "好友资料暂时无法加载"))
                    }
                },
            )
        }

    private suspend fun awaitConversation(conversationId: String): V2TIMConversation =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getConversationManager().getConversation(
                conversationId,
                object : V2TIMValueCallback<V2TIMConversation> {
                    override fun onSuccess(conversation: V2TIMConversation?) {
                        if (!continuation.isActive) return
                        if (conversation != null) continuation.resume(conversation)
                        else continuation.resumeWithException(IllegalStateException("会话资料返回为空"))
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) continuation.resumeWithException(V2TencentIMException(code, desc, "会话设置暂时无法加载"))
                    }
                },
            )
        }

    private suspend fun awaitC2CReceiveOption(userId: String): Int =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getMessageManager().getC2CReceiveMessageOpt(
                listOf(userId),
                object : V2TIMValueCallback<List<com.tencent.imsdk.v2.V2TIMReceiveMessageOptInfo>> {
                    override fun onSuccess(values: List<com.tencent.imsdk.v2.V2TIMReceiveMessageOptInfo>?) {
                        if (!continuation.isActive) return
                        continuation.resume(values.orEmpty().firstOrNull()?.c2CReceiveMessageOpt ?: V2TIMMessage.V2TIM_RECEIVE_MESSAGE)
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) continuation.resumeWithException(V2TencentIMException(code, desc, "消息免打扰状态暂时无法加载"))
                    }
                },
            )
        }

    private suspend fun awaitBlackListUserIds(): Set<String> =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getFriendshipManager().getBlackList(
                object : V2TIMValueCallback<List<V2TIMFriendInfo>> {
                    override fun onSuccess(values: List<V2TIMFriendInfo>?) {
                        if (continuation.isActive) continuation.resume(values.orEmpty().mapNotNull { it.userID }.toSet())
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) continuation.resumeWithException(V2TencentIMException(code, desc, "黑名单状态暂时无法加载"))
                    }
                },
            )
        }

    private suspend fun awaitSetFriendRemark(userId: String, remark: String) =
        suspendCancellableCoroutine { continuation ->
            val info = V2TIMFriendInfo().apply {
                setUserID(userId)
                setFriendRemark(remark)
            }
            V2TIMManager.getFriendshipManager().setFriendInfo(info, callback(continuation, "备注暂时无法保存"))
        }

    private suspend fun awaitSetC2CReceiveOption(userId: String, option: Int) =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getMessageManager().setC2CReceiveMessageOpt(
                listOf(userId), option, callback(continuation, "消息免打扰暂时无法更新"),
            )
        }

    private suspend fun awaitPinConversation(conversationId: String, pinned: Boolean) =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getConversationManager().pinConversation(
                conversationId, pinned, callback(continuation, "置顶聊天暂时无法更新"),
            )
        }

    private suspend fun awaitSetBlocked(userId: String, blocked: Boolean) =
        suspendCancellableCoroutine { continuation ->
            val manager = V2TIMManager.getFriendshipManager()
            val callback = object : V2TIMValueCallback<List<com.tencent.imsdk.v2.V2TIMFriendOperationResult>> {
                override fun onSuccess(results: List<com.tencent.imsdk.v2.V2TIMFriendOperationResult>?) {
                    if (!continuation.isActive) return
                    val failure = results.orEmpty().firstOrNull { it.resultCode != 0 }
                    if (failure == null) continuation.resume(Unit)
                    else continuation.resumeWithException(V2TencentIMException(failure.resultCode, failure.resultInfo, "黑名单状态暂时无法更新"))
                }

                override fun onError(code: Int, desc: String?) {
                    if (continuation.isActive) continuation.resumeWithException(V2TencentIMException(code, desc, "黑名单状态暂时无法更新"))
                }
            }
            if (blocked) manager.addToBlackList(listOf(userId), callback)
            else manager.deleteFromBlackList(listOf(userId), callback)
        }

    private suspend fun awaitClearC2CHistory(userId: String) =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getMessageManager().clearC2CHistoryMessage(
                userId, callback(continuation, "聊天记录暂时无法清空"),
            )
        }

    private suspend fun resolveRawMessage(messageId: String): V2TIMMessage {
        rawMessagesById[messageId]?.let { return it }
        val resolved = awaitFindMessages(listOf(messageId)).firstOrNull { it.msgID == messageId }
            ?: throw IllegalStateException("消息已不存在")
        rawMessagesById[messageId] = resolved
        return resolved
    }

    private suspend fun awaitFindMessages(messageIds: List<String>): List<V2TIMMessage> =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getMessageManager().findMessages(
                messageIds,
                object : V2TIMValueCallback<List<V2TIMMessage>> {
                    override fun onSuccess(messages: List<V2TIMMessage>?) {
                        if (continuation.isActive) continuation.resume(messages.orEmpty())
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(V2TencentIMException(code, desc, "消息暂时无法定位"))
                        }
                    }
                },
            )
        }

    private suspend fun awaitDownloadMergerMessages(element: V2TIMMergerElem): List<V2TIMMessage> =
        suspendCancellableCoroutine { continuation ->
            element.downloadMergerMessage(
                object : V2TIMValueCallback<List<V2TIMMessage>> {
                    override fun onSuccess(messages: List<V2TIMMessage>?) {
                        if (continuation.isActive) continuation.resume(messages.orEmpty())
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(V2TencentIMException(code, desc, "聊天记录暂时无法加载"))
                        }
                    }
                },
            )
        }

    private suspend fun awaitDeleteMessages(messages: List<V2TIMMessage>) =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getMessageManager().deleteMessages(
                messages,
                callback(continuation, "消息删除失败"),
            )
        }

    private suspend fun awaitRecallMessage(message: V2TIMMessage) =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getMessageManager().revokeMessage(
                message,
                callback(continuation, "消息撤回失败"),
            )
        }

    private suspend fun awaitSetMessageReaction(
        message: V2TIMMessage,
        reactionId: String,
        shouldReact: Boolean,
    ) = suspendCancellableCoroutine { continuation ->
        val callback = callback(continuation, if (shouldReact) "添加表情回应失败" else "移除表情回应失败")
        val messageManager = V2TIMManager.getMessageManager()
        if (shouldReact) messageManager.addMessageReaction(message, reactionId, callback)
        else messageManager.removeMessageReaction(message, reactionId, callback)
    }

    private suspend fun awaitTranslateText(source: String, targetLanguage: String): String =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getMessageManager().translateText(
                listOf(source),
                "",
                targetLanguage,
                object : V2TIMValueCallback<HashMap<String, String>> {
                    override fun onSuccess(values: HashMap<String, String>?) {
                        if (!continuation.isActive) return
                        val translated = values?.get(source)?.takeIf(String::isNotBlank)
                            ?: values?.values?.firstOrNull(String::isNotBlank)
                        if (translated != null) continuation.resume(translated)
                        else continuation.resumeWithException(IllegalStateException("翻译结果为空"))
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(V2TencentIMException(code, desc, "翻译失败"))
                        }
                    }
                },
            )
        }

    private suspend fun awaitMessageReactions(
        messages: List<V2TIMMessage>,
    ): Map<String, List<V2IMReaction>> {
        if (messages.isEmpty()) return emptyMap()
        val results = suspendCancellableCoroutine { continuation ->
            V2TIMManager.getMessageManager().getMessageReactions(
                messages,
                3,
                object : V2TIMValueCallback<List<com.tencent.imsdk.v2.V2TIMMessageReactionResult>> {
                    override fun onSuccess(values: List<com.tencent.imsdk.v2.V2TIMMessageReactionResult>?) {
                        if (continuation.isActive) continuation.resume(values.orEmpty())
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(V2TencentIMException(code, desc, "表情回应暂时无法同步"))
                        }
                    }
                },
            )
        }
        return results.asSequence()
            .filter { it.resultCode == 0 && !it.messageID.isNullOrBlank() }
            .associate { result ->
                result.messageID to result.reactionList.orEmpty().mapNotNull { reaction ->
                    val id = reaction.reactionID?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                    V2IMReaction(
                        reactionId = id,
                        totalUserCount = reaction.totalUserCount.coerceAtLeast(0),
                        reactedByMe = reaction.reactedByMyself,
                    )
                }
            }
    }

    private suspend fun awaitMessageReactionUsers(
        message: V2TIMMessage,
        reactionId: String,
    ): List<V2IMReactionUser> = suspendCancellableCoroutine { continuation ->
        V2TIMManager.getMessageManager().getAllUserListOfMessageReaction(
            message,
            reactionId,
            0,
            100,
            object : V2TIMValueCallback<com.tencent.imsdk.v2.V2TIMMessageReactionUserResult> {
                override fun onSuccess(result: com.tencent.imsdk.v2.V2TIMMessageReactionUserResult?) {
                    if (!continuation.isActive) return
                    continuation.resume(
                        result?.userInfoList.orEmpty().mapNotNull { user ->
                            val userId = user.userID?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                            V2IMReactionUser(
                                userId = userId,
                                nickname = user.nickName?.takeIf(String::isNotBlank),
                                avatar = user.faceUrl?.takeIf(String::isNotBlank),
                            )
                        },
                    )
                }

                override fun onError(code: Int, desc: String?) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(V2TencentIMException(code, desc, "回应成员暂时无法加载"))
                    }
                }
            },
        )
    }

    private suspend fun awaitDeleteFriend(userId: String) =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getFriendshipManager().deleteFromFriendList(
                listOf(userId),
                V2TIMFriendInfo.V2TIM_FRIEND_TYPE_BOTH,
                object : V2TIMValueCallback<List<com.tencent.imsdk.v2.V2TIMFriendOperationResult>> {
                    override fun onSuccess(results: List<com.tencent.imsdk.v2.V2TIMFriendOperationResult>?) {
                        if (!continuation.isActive) return
                        val failure = results.orEmpty().firstOrNull { it.resultCode != 0 }
                        if (failure == null) continuation.resume(Unit)
                        else continuation.resumeWithException(V2TencentIMException(failure.resultCode, failure.resultInfo, "删除好友失败"))
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) continuation.resumeWithException(V2TencentIMException(code, desc, "删除好友失败"))
                    }
                },
            )
        }

    private suspend fun awaitDeleteConversation(conversationId: String) =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getConversationManager().deleteConversation(
                conversationId, callback(continuation, "会话暂时无法删除"),
            )
        }

    private fun callback(
        continuation: kotlinx.coroutines.CancellableContinuation<Unit>,
        operation: String,
    ) = object : V2TIMCallback {
        override fun onSuccess() {
            if (continuation.isActive) continuation.resume(Unit)
        }

        override fun onError(code: Int, desc: String?) {
            if (continuation.isActive) continuation.resumeWithException(V2TencentIMException(code, desc, operation))
        }
    }

    private fun rawMessageId(message: V2TIMMessage): String {
        val text = message.textElem?.text?.trim().orEmpty()
        return message.msgID?.takeIf(String::isNotBlank)
            ?: "${message.sender.orEmpty()}_${message.timestamp}_${message.elemType}_${text.hashCode()}"
    }

    private suspend fun toIMMessage(
        message: V2TIMMessage,
        reactions: List<V2IMReaction> = emptyList(),
    ): V2IMMessage? {
        val text = message.textElem?.text?.trim().orEmpty()
        val messageId = rawMessageId(message)
        rawMessagesById[messageId] = message
        val isRecalled = message.status == V2TIMMessage.V2TIM_MSG_STATUS_LOCAL_REVOKED
        val image = message.imageElem
        val video = message.videoElem
        val voice = message.soundElem
        val file = message.fileElem
        val merger = message.mergerElem
        val kind = when {
            isRecalled -> V2IMMessageKind.SYSTEM
            text.isNotEmpty() -> V2IMMessageKind.TEXT
            image != null -> V2IMMessageKind.IMAGE
            video != null -> V2IMMessageKind.VIDEO
            voice != null -> V2IMMessageKind.VOICE
            file != null -> V2IMMessageKind.FILE
            merger != null -> V2IMMessageKind.MERGED
            else -> V2IMMessageKind.UNSUPPORTED
        }
        val imageVariants = image?.imageList.orEmpty()
        val imageOrigin = imageVariants.firstOrNull { it.type == com.tencent.imsdk.v2.V2TIMImageElem.V2TIM_IMAGE_TYPE_ORIGIN }
            ?: imageVariants.firstOrNull()
        val imageThumb = imageVariants.firstOrNull { it.type == com.tencent.imsdk.v2.V2TIMImageElem.V2TIM_IMAGE_TYPE_THUMB }
            ?: imageOrigin
        val mediaUrl = when (kind) {
            V2IMMessageKind.IMAGE -> image?.path?.takeIf(String::isNotBlank) ?: imageOrigin?.url?.takeIf(String::isNotBlank)
            V2IMMessageKind.VIDEO -> video?.videoPath?.takeIf(String::isNotBlank)
                ?: video?.let { runCatching { awaitVideoUrl(it) }.getOrNull() }
            V2IMMessageKind.VOICE -> voice?.path?.takeIf(String::isNotBlank)
                ?: voice?.let { runCatching { awaitVoiceUrl(it) }.getOrNull() }
            V2IMMessageKind.FILE -> file?.path?.takeIf(String::isNotBlank)
                ?: file?.let {
                    cachedChatFile(messageId, it.fileName, it.fileSize.toLong())?.absolutePath
                        ?: runCatching { awaitFileUrl(it) }.getOrNull()
                }
            else -> null
        }
        val thumbnailUrl = when (kind) {
            V2IMMessageKind.IMAGE -> imageThumb?.url?.takeIf(String::isNotBlank) ?: mediaUrl
            V2IMMessageKind.VIDEO -> video?.snapshotPath?.takeIf(String::isNotBlank)
                ?: video?.let { runCatching { awaitVideoSnapshotUrl(it) }.getOrNull() }
            else -> null
        }
        return V2IMMessage(
            messageId = messageId,
            senderUserId = message.sender.orEmpty(),
            text = if (isRecalled) {
                if (message.isSelf) "你撤回了一条消息" else "对方撤回了一条消息"
            } else text,
            timeEpochSeconds = message.timestamp,
            isMine = message.isSelf,
            kind = kind,
            mediaUrl = mediaUrl,
            thumbnailUrl = thumbnailUrl,
            fileName = file?.fileName?.takeIf(String::isNotBlank),
            fileSizeBytes = file?.fileSize?.toLong()?.takeIf { it > 0L },
            durationSeconds = when (kind) {
                V2IMMessageKind.VIDEO -> video?.duration
                V2IMMessageKind.VOICE -> voice?.duration
                else -> null
            }?.takeIf { it > 0 },
            reactions = reactions,
            senderName = sequenceOf(message.nameCard, message.friendRemark, message.nickName, message.sender)
                .firstOrNull { !it.isNullOrBlank() },
            senderAvatar = message.faceUrl?.takeIf(String::isNotBlank),
            mergedTitle = merger?.title?.takeIf(String::isNotBlank),
            mergedAbstracts = merger?.abstractList.orEmpty().filter(String::isNotBlank).take(4),
            mergedLayersOverLimit = merger?.isLayersOverLimit == true,
            deliveryState = when {
                message.hasRiskContent() -> V2IMDeliveryState.VIOLATION
                message.status == V2TIMMessage.V2TIM_MSG_STATUS_SENDING -> V2IMDeliveryState.SENDING
                message.status == V2TIMMessage.V2TIM_MSG_STATUS_SEND_FAIL -> V2IMDeliveryState.FAILED
                else -> V2IMDeliveryState.SENT
            },
            failureMessage = when {
                message.hasRiskContent() -> "消息内容违规，发送失败"
                message.status == V2TIMMessage.V2TIM_MSG_STATUS_SEND_FAIL -> "消息发送失败，请重试"
                else -> null
            },
        )
    }

    private suspend fun awaitVideoUrl(element: com.tencent.imsdk.v2.V2TIMVideoElem): String =
        suspendCancellableCoroutine { continuation ->
            element.getVideoUrl(stringCallback(continuation, "视频地址暂时无法加载"))
        }

    private suspend fun awaitVideoSnapshotUrl(element: com.tencent.imsdk.v2.V2TIMVideoElem): String =
        suspendCancellableCoroutine { continuation ->
            element.getSnapshotUrl(stringCallback(continuation, "视频封面暂时无法加载"))
        }

    private suspend fun awaitVoiceUrl(element: com.tencent.imsdk.v2.V2TIMSoundElem): String =
        suspendCancellableCoroutine { continuation ->
            element.getUrl(stringCallback(continuation, "语音暂时无法加载"))
        }

    private suspend fun awaitFileUrl(element: com.tencent.imsdk.v2.V2TIMFileElem): String =
        suspendCancellableCoroutine { continuation ->
            element.getUrl(stringCallback(continuation, "文件暂时无法加载"))
        }

    private suspend fun awaitDownloadFile(
        element: com.tencent.imsdk.v2.V2TIMFileElem,
        destination: File,
        onProgress: (Int) -> Unit,
    ) = suspendCancellableCoroutine { continuation ->
        element.downloadFile(
            destination.absolutePath,
            object : V2TIMDownloadCallback {
                override fun onProgress(progressInfo: com.tencent.imsdk.v2.V2TIMElem.V2ProgressInfo?) {
                    val total = progressInfo?.totalSize ?: 0L
                    val current = progressInfo?.currentSize ?: 0L
                    if (total > 0L) {
                        runCatching { onProgress(((current * 100L) / total).toInt().coerceIn(0, 99)) }
                    }
                }

                override fun onSuccess() {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onError(code: Int, desc: String?) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(V2TencentIMException(code, desc, "文件下载失败"))
                    }
                }
            },
        )
    }

    private fun cachedChatFile(messageId: String, fileName: String?, expectedBytes: Long): File? =
        chatFileCacheTarget(appContext.cacheDir, messageId, fileName).takeIf {
            it.isFile && it.length() > 0L && (expectedBytes <= 0L || it.length() == expectedBytes)
        }

    private fun stringCallback(
        continuation: kotlinx.coroutines.CancellableContinuation<String>,
        operation: String,
    ) = object : V2TIMValueCallback<String> {
        override fun onSuccess(value: String?) {
            if (!continuation.isActive) return
            if (!value.isNullOrBlank()) continuation.resume(value)
            else continuation.resumeWithException(IllegalStateException(operation))
        }

        override fun onError(code: Int, desc: String?) {
            if (continuation.isActive) continuation.resumeWithException(V2TencentIMException(code, desc, operation))
        }
    }

    private suspend fun awaitAddFriend(targetUserId: String, addWording: String): V2FriendRequestOutcome =
        suspendCancellableCoroutine { continuation ->
            val application = V2TIMFriendAddApplication(targetUserId).apply {
                setAddType(V2TIMFriendInfo.V2TIM_FRIEND_TYPE_BOTH)
                setAddSource("Android_App")
                setAddWording(addWording.trim().ifBlank { "你好，我想加你为好友" })
            }
            V2TIMManager.getFriendshipManager().addFriend(
                application,
                object : V2TIMValueCallback<com.tencent.imsdk.v2.V2TIMFriendOperationResult> {
                    override fun onSuccess(result: com.tencent.imsdk.v2.V2TIMFriendOperationResult?) {
                        if (!continuation.isActive) return
                        when (result?.resultCode ?: 0) {
                            0, 30539 -> continuation.resume(V2FriendRequestOutcome.SENT)
                            30515 -> continuation.resume(V2FriendRequestOutcome.ALREADY_FRIENDS)
                            else -> continuation.resumeWithException(
                                V2TencentIMException(result?.resultCode ?: -1, result?.resultInfo, "好友请求发送失败"),
                            )
                        }
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(V2TencentIMException(code, desc, "好友请求发送失败"))
                        }
                    }
                },
            )
        }

    private suspend fun awaitIncomingFriendRequests(): List<V2IncomingFriendRequest> =
        awaitIncomingFriendApplications().map { application ->
            V2IncomingFriendRequest(
                userId = application.userID.orEmpty(),
                nickname = application.nickname,
                avatar = application.faceUrl,
                wording = application.addWording,
                addTimeSeconds = application.addTime,
            )
        }.filter { it.userId.isNotBlank() }

    private suspend fun awaitIncomingFriendApplications(): List<V2TIMFriendApplication> =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getFriendshipManager().getFriendApplicationList(
                object : V2TIMValueCallback<com.tencent.imsdk.v2.V2TIMFriendApplicationResult> {
                    override fun onSuccess(result: com.tencent.imsdk.v2.V2TIMFriendApplicationResult?) {
                        if (!continuation.isActive) return
                        continuation.resume(
                            result?.friendApplicationList.orEmpty().filter {
                                it.type == V2TIMFriendApplication.V2TIM_FRIEND_APPLICATION_COME_IN ||
                                    it.type == V2TIMFriendApplication.V2TIM_FRIEND_APPLICATION_BOTH
                            },
                        )
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(V2TencentIMException(code, desc, "好友申请暂时无法加载"))
                        }
                    }
                },
            )
        }

    private suspend fun awaitAcceptFriend(application: V2TIMFriendApplication) =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getFriendshipManager().acceptFriendApplication(
                application,
                V2TIMFriendApplication.V2TIM_FRIEND_ACCEPT_AGREE_AND_ADD,
                object : V2TIMValueCallback<com.tencent.imsdk.v2.V2TIMFriendOperationResult> {
                    override fun onSuccess(result: com.tencent.imsdk.v2.V2TIMFriendOperationResult?) {
                        if (!continuation.isActive) return
                        val code = result?.resultCode ?: 0
                        if (code == 0 || code == 30515) continuation.resume(Unit)
                        else continuation.resumeWithException(V2TencentIMException(code, result?.resultInfo, "接受好友申请失败"))
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) continuation.resumeWithException(V2TencentIMException(code, desc, "接受好友申请失败"))
                    }
                },
            )
        }

    private suspend fun awaitRejectFriend(application: V2TIMFriendApplication) =
        suspendCancellableCoroutine { continuation ->
            V2TIMManager.getFriendshipManager().refuseFriendApplication(
                application,
                object : V2TIMValueCallback<com.tencent.imsdk.v2.V2TIMFriendOperationResult> {
                    override fun onSuccess(result: com.tencent.imsdk.v2.V2TIMFriendOperationResult?) {
                        if (!continuation.isActive) return
                        val code = result?.resultCode ?: 0
                        if (code == 0) continuation.resume(Unit)
                        else continuation.resumeWithException(V2TencentIMException(code, result?.resultInfo, "拒绝好友申请失败"))
                    }

                    override fun onError(code: Int, desc: String?) {
                        if (continuation.isActive) continuation.resumeWithException(V2TencentIMException(code, desc, "拒绝好友申请失败"))
                    }
                },
            )
        }

    private suspend fun <T> withTencentSession(
        session: V2Session,
        renewExpiredSession: suspend () -> V2Session,
        block: suspend () -> T,
    ): Result<T> = try {
        Result.success(
            mutex.withLock {
                var activeSession = session.requireTencentCredentials()
                ensureInitialized(activeSession.sdkAppId ?: V2ApiConfig.IM_SDK_APP_ID)
                try {
                    ensureLoggedIn(activeSession)
                    block()
                } catch (error: V2TencentIMException) {
                    if (!error.isExpiredUserSig) throw error
                    activeSession = renewExpiredSession().requireTencentCredentials()
                    ensureInitialized(activeSession.sdkAppId ?: V2ApiConfig.IM_SDK_APP_ID)
                    ensureLoggedIn(activeSession, forceRelogin = true)
                    block()
                }
            },
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
}

internal enum class V2TencentSessionEvent { KICKED_OFFLINE, USER_SIG_EXPIRED }

internal fun createTencentSdkSessionListener(
    onEvent: (V2TencentSessionEvent) -> Unit,
): V2TIMSDKListener = object : V2TIMSDKListener() {
    override fun onKickedOffline() {
        onEvent(V2TencentSessionEvent.KICKED_OFFLINE)
    }

    override fun onUserSigExpired() {
        onEvent(V2TencentSessionEvent.USER_SIG_EXPIRED)
    }
}

internal fun chatFileCacheTarget(cacheDir: File, messageId: String, fileName: String?): File {
    val safeMessageId = messageId.replace(Regex("[^A-Za-z0-9._-]"), "_").take(72).ifBlank { "message" }
    val safeFileName = fileName.orEmpty().substringAfterLast('/').substringAfterLast('\\')
        .replace(Regex("[^A-Za-z0-9._()\u4e00-\u9fff-]"), "_")
        .trim('.', '_')
        .take(96)
        .ifBlank { "file" }
    return File(File(cacheDir, "chat_media/files"), "${safeMessageId}_$safeFileName")
}

enum class V2FriendRequestOutcome { SENT, ALREADY_FRIENDS, ACCEPTED }

internal class V2TencentIMException(
    val code: Int,
    description: String?,
    operation: String = "好友列表暂时无法同步",
) : IllegalStateException(
    description?.takeIf(String::isNotBlank)?.let { "$operation：$it（$code）" }
        ?: "$operation（腾讯 IM $code）",
) {
    val isExpiredUserSig: Boolean get() = code == 6206 || code == 70001
}

internal fun normalizeTencentFriendUserIds(
    rawUserIds: List<String?>,
    currentUserId: String?,
    limit: Int = 200,
): List<String> {
    val ownId = currentUserId?.trim().orEmpty()
    return rawUserIds.asSequence()
        .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
        .filterNot { it == ownId }
        .distinct()
        .take(limit.coerceAtLeast(0))
        .toList()
}

private fun V2Session.requireTencentCredentials(): V2Session {
    require(!userId.isNullOrBlank() && !userSig.isNullOrBlank()) { "请先登录后再同步好友空间" }
    return this
}
