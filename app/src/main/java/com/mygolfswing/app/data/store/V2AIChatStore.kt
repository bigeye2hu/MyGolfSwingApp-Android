package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.ai.V2AIMockProvider
import com.mygolfswing.app.data.ai.V2AIProvider
import com.mygolfswing.app.data.ai.V2AIService
import com.mygolfswing.app.data.ai.V2ProfileReflector
import com.mygolfswing.app.data.ai.profileReflectionFailureResult
import com.mygolfswing.app.data.model.V2AIMessageRole
import com.mygolfswing.app.data.model.V2AISession
import com.mygolfswing.app.data.model.V2ChatMessage
import com.mygolfswing.app.data.model.V2Memo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * V2 AI 对话 Store（对齐 iOS `V2AIChatViewModel` + `AIConversationManager`）。
 *
 * 一个全局当前会话 + 历史会话列表，UI 通过 [currentSession] / [statusBar] 订阅。
 */
object V2AIChatStore {

    private const val STREAM_TIMEOUT_MS = 75_000L
    internal const val AI_REFLECTION_STATUS = "正在沉淀到训练档案…"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * 备用 mock provider（无网络 / 调试时使用）。
     * 真实调用走 [ServiceLocator.aiService]。当 [useMockProvider] = true 时降级。
     */
    var provider: V2AIProvider = V2AIMockProvider
    var useMockProvider: Boolean = false

    private val _providerName = MutableStateFlow("挥呗 AI")
    val providerName: StateFlow<String> = _providerName.asStateFlow()

    private val _modelName = MutableStateFlow("default")
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    private val _modelDisplay = MutableStateFlow("default")
    val modelDisplay: StateFlow<String> = _modelDisplay.asStateFlow()

    private val _requestOpenConfig = MutableStateFlow(false)
    val requestOpenConfig: StateFlow<Boolean> = _requestOpenConfig.asStateFlow()

    private val _sessions = MutableStateFlow<List<V2AISession>>(emptyList())
    val sessions: StateFlow<List<V2AISession>> = _sessions.asStateFlow()

    private val _currentSession = MutableStateFlow(
        V2AISession(title = "新对话", messages = listOf(welcomeMessage())),
    )
    val currentSession: StateFlow<V2AISession> = _currentSession.asStateFlow()

    private val _statusBar = MutableStateFlow<String?>(null)
    val statusBar: StateFlow<String?> = _statusBar.asStateFlow()

    private val _recordedBanner = MutableStateFlow<String?>(null)
    val recordedBanner: StateFlow<String?> = _recordedBanner.asStateFlow()

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    private var streamJob: Job? = null
    private var statusAutoHideJob: Job? = null
    @Volatile private var observingSession = false

    val isReplying: Boolean get() = streamJob?.isActive == true

    private fun welcomeMessage(): V2ChatMessage = V2ChatMessage(
        role = V2AIMessageRole.ASSISTANT,
        text = "嗨，我是你的 AI 高尔夫教练。今天想聊挥杆、推杆、还是练球计划？",
    )

    fun loadPersisted() {
        if (observingSession) return
        observingSession = true
        scope.launch {
            ServiceLocator.session.state.collect { session ->
                streamJob?.cancel()
                setStatusBar(null)
                _requestOpenConfig.value = false
                refreshConfigState()
                if (!session.isLoggedIn) {
                    resetInMemory()
                    return@collect
                }
                val list = try {
                    ServiceLocator.aiChatRepo.loadAllSessions()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    setStatusBar(aiUserError("历史会话加载失败，请稍后重试", e.message), autoHideMs = 4_000L)
                    return@collect
                }
                if (list.isNotEmpty()) {
                    _currentSession.value = list.first()
                    _sessions.value = list.drop(1)
                } else {
                    resetInMemory()
                }
            }
        }
    }

    fun refreshConfigState() {
        scope.launch {
            val providerId = ServiceLocator.aiService.currentProvider()
            val model = ServiceLocator.aiService.currentModel()
            _providerName.value = providerId.displayName
            _modelName.value = model
            _modelDisplay.value = modelDisplayFor(providerId, model)
        }
    }

    fun consumeOpenConfigRequest() {
        _requestOpenConfig.value = false
    }

    fun resetInMemory() {
        streamJob?.cancel()
        _sessions.value = emptyList()
        _currentSession.value = V2AISession(title = "新对话", messages = listOf(welcomeMessage()))
        _draft.value = ""
        setStatusBar(null)
        _requestOpenConfig.value = false
    }

    fun startNewSession() {
        streamJob?.cancel()
        setStatusBar(null)
        _draft.value = ""
        val cur = _currentSession.value
        if (cur.messages.size > 1) {
            scope.launch { ServiceLocator.aiChatRepo.saveSession(cur) }
            _sessions.update { listOf(cur) + it }
        }
        _currentSession.value = V2AISession(title = "新对话", messages = listOf(welcomeMessage()))
    }

    fun switchSession(toId: UUID) {
        val target = _sessions.value.firstOrNull { it.id == toId } ?: return
        streamJob?.cancel()
        setStatusBar(null)
        val current = _currentSession.value
        if (current.hasPersistableMessages()) {
            scope.launch { ServiceLocator.aiChatRepo.saveSession(current) }
        }
        _sessions.update { list ->
            val retained = list.filterNot { s -> s.id == toId || s.id == current.id }
            if (current.hasPersistableMessages()) {
                (listOf(current) + retained).sortedByDescending { it.updatedAt }
            } else {
                retained
            }
        }
        _currentSession.value = target
    }

    fun updateDraft(value: String) {
        _draft.value = value
    }

    fun clearDraftIf(value: String) {
        if (_draft.value == value) _draft.value = ""
    }

    fun deleteSession(sessionId: UUID) {
        val previous = _sessions.value
        previous.firstOrNull { it.id == sessionId } ?: return
        _sessions.value = previous.filterNot { it.id == sessionId }
        scope.launch {
            try {
                ServiceLocator.aiChatRepo.deleteSession(sessionId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _sessions.value = previous
                setStatusBar(aiUserError("删除对话失败，请稍后重试", e.message), autoHideMs = 4_000L)
            }
        }
    }

    /** 用户发送 + 流式回复（同步更新 currentSession） */
    fun send(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || isReplying) return

        streamJob = scope.launch {
            val providerId = ServiceLocator.aiService.currentProvider()
            val apiKey = ServiceLocator.session.aiApiKey.first()
            if (providerId != V2AIService.ProviderId.BUILTIN && apiKey.isNullOrBlank()) {
                setStatusBar("请先完成 AI 配置", autoHideMs = 4_000L)
                _requestOpenConfig.value = true
                return@launch
            }

            refreshConfigState()
            refreshTrainingContextBeforeSend()
            val userMsg = V2ChatMessage(role = V2AIMessageRole.USER, text = trimmed)
            val placeholder = V2ChatMessage(role = V2AIMessageRole.ASSISTANT, text = "", isStreaming = true)
            val outboundHistory = (_currentSession.value.messages + userMsg)
                .filter { it.role != V2AIMessageRole.SYSTEM }
                .filterNot { it.role == V2AIMessageRole.ASSISTANT && it.text.isBlank() }
                .map { (if (it.role == V2AIMessageRole.USER) "user" else "assistant") to it.text }
                .let { history ->
                    buildUserContextSystemMessage()?.let { system -> listOf("system" to system) + history }
                        ?: history
                }

            _currentSession.update { s ->
                s.copy(
                    title = if (s.messages.size <= 1) trimmed.take(16) else s.title,
                    messages = s.messages
                        .filterNot { it.role == V2AIMessageRole.ASSISTANT && it.text.isBlank() && !it.isStreaming }
                        .plus(userMsg)
                        .plus(placeholder),
                    updatedAt = LocalDateTime.now(),
                )
            }
            setStatusBar("AI 正在思考…")

            val buffer = StringBuilder()
            try {
                val flow = if (shouldUseAIChatMockProvider(useMockProvider, ServiceLocator.isDebuggable)) {
                    provider.streamReply(trimmed, outboundHistory)
                } else {
                    ServiceLocator.aiService.streamChat(outboundHistory)
                }
                withTimeout(STREAM_TIMEOUT_MS) {
                    flow.collect { chunk ->
                        buffer.append(chunk)
                        appendToLast(buffer.toString(), streaming = true)
                    }
                }
                appendToLast(buffer.toString(), streaming = false)
            } catch (e: TimeoutCancellationException) {
                appendToLast(
                    buffer.toString().ifBlank { "（请求超时，请检查 AI 配置或网络）" },
                    streaming = false,
                )
                setStatusBar("请求超时", autoHideMs = 3_000L)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appendToLast("（${aiUserError("生成失败，请检查 AI 配置或稍后重试", e.message)}）", streaming = false)
                setStatusBar("AI 请求失败，请检查配置", autoHideMs = 4_000L)
            } finally {
                clearStatusBarIf("AI 正在思考…")
                if (providerId == V2AIService.ProviderId.BUILTIN) refreshConfigState()
                persistCurrent()
                if (buffer.isNotBlank()) triggerReplyReflectionIfDue()
            }
        }
    }

    private fun persistCurrent() {
        scope.launch { ServiceLocator.aiChatRepo.saveSession(_currentSession.value) }
    }

    private fun appendToLast(text: String, streaming: Boolean) {
        _currentSession.update { s ->
            val newMessages = s.messages.toMutableList()
            val lastIdx = newMessages.indexOfLast { it.role == V2AIMessageRole.ASSISTANT }
            if (lastIdx >= 0) {
                newMessages[lastIdx] = newMessages[lastIdx].copy(text = text, isStreaming = streaming)
            }
            s.copy(messages = newMessages, updatedAt = LocalDateTime.now())
        }
    }

    fun stopStreaming() {
        streamJob?.cancel()
        appendToLast(
            _currentSession.value.messages.lastOrNull()?.text?.takeIf { it.isNotEmpty() } ?: "（已停止）",
            streaming = false,
        )
        setStatusBar(null)
    }

    fun triggerReflectionIfNeeded() {
        val session = _currentSession.value
        val hasConversation = session.messages.count {
            (it.role == V2AIMessageRole.USER || it.role == V2AIMessageRole.ASSISTANT) &&
                it.text.isNotBlank() &&
                !it.isStreaming
        } >= 2
        if (!hasConversation) return
        launchReflection()
    }

    fun consumeRecordedBanner() {
        _recordedBanner.value = null
    }

    private fun triggerReplyReflectionIfDue() {
        val userTurns = _currentSession.value.messages.count {
            it.role == V2AIMessageRole.USER && it.text.isNotBlank()
        }
        if (userTurns == 1 || userTurns % 5 == 0) {
            launchReflection()
        }
    }

    private fun launchReflection() {
        scope.launch {
            setStatusBar(AI_REFLECTION_STATUS)
            val result = try {
                V2ProfileReflector.reflectAll(_currentSession.value.id)
            } catch (e: CancellationException) {
                if (shouldClearAIChatReflectionStatusOnCancellation(_statusBar.value)) {
                    setStatusBar(null)
                }
                throw e
            } catch (e: Throwable) {
                profileReflectionFailureResult(e)
            }
            when (result) {
                is V2ProfileReflector.Result.Updated -> {
                    _recordedBanner.value = when {
                        result.itemsAdded > 0 -> "已记录到档案 · +${result.itemsAdded} 条心得"
                        result.summaryChanged -> "已记录到档案 · 状态已更新"
                        else -> "已记录到档案"
                    }
                    setStatusBar(null)
                }
                is V2ProfileReflector.Result.Failed -> {
                    setStatusBar(aiUserError("档案同步失败，请稍后重试", result.message), autoHideMs = 4_000L)
                }
                V2ProfileReflector.Result.Empty,
                V2ProfileReflector.Result.NoChange,
                V2ProfileReflector.Result.Skipped -> {
                    setStatusBar(null)
                }
            }
        }
    }

    private fun setStatusBar(text: String?, autoHideMs: Long? = null) {
        statusAutoHideJob?.cancel()
        statusAutoHideJob = null
        _statusBar.value = text
        if (text != null && autoHideMs != null) {
            statusAutoHideJob = scope.launch {
                delay(autoHideMs)
                if (_statusBar.value == text) {
                    _statusBar.value = null
                    statusAutoHideJob = null
                }
            }
        }
    }

    private fun aiUserError(base: String, debugDetail: String?): String {
        return aiChatUserError(base, debugDetail)
    }

    private fun clearStatusBarIf(text: String) {
        if (_statusBar.value == text) setStatusBar(null)
    }

    private suspend fun modelDisplayFor(providerId: V2AIService.ProviderId, model: String): String {
        if (providerId != V2AIService.ProviderId.BUILTIN) return model
        val quota = runAIQuotaCatching { ServiceLocator.net.downloadApi.aiQuota() }.getOrNull()
        val remaining = quota?.get("remaining")?.jsonPrimitive?.intOrNull
        val limit = quota?.get("daily_limit")?.jsonPrimitive?.intOrNull
        if (remaining != null && limit != null) {
            return if (remaining <= 0) "$model · 今日额度已用完" else "$model ($remaining/$limit)"
        }
        return model
    }

    private fun buildUserContextSystemMessage(): String? {
        val parts = mutableListOf<String>()
        parts += "你是用户的高尔夫训练教练。回答要结合用户当前训练档案、目标、近期 Memo 和练习记录；不要逐条复述内部上下文，只在建议中自然引用相关事实。如果信息不足，先给可执行的观察/练习建议。"
        buildAccountProfilePrompt()?.let { parts += it }
        V2GolfProfileStore.buildProfilePrompt()?.let { parts += it }

        val activeGoals = V2GoalStore.activeGoals.take(5)
        if (activeGoals.isNotEmpty()) {
            parts += "当前目标: " + activeGoals.joinToString("；") { goal ->
                val details = listOfNotNull(
                    goal.typeLabel,
                    goal.sessionProgressDisplay,
                    goal.metricDisplay?.let { "指标$it" },
                    goal.cadence?.displayText,
                    "本周打卡${goal.weekCheckInCount}次".takeIf { goal.weekCheckInCount > 0 },
                    goal.priority.shortName,
                )
                "${goal.title}(${details.joinToString(" / ")})"
            }
        }

        val recentMemos = V2MemoStore.recentMemos(limit = 6)
        if (recentMemos.isNotEmpty()) {
            parts += "近期 Memo: " + recentMemos.joinToString("；") { it.toAiContextLine() }
        }

        buildSwingPracticePrompt()?.let { parts += it }

        return parts.takeIf { it.size > 1 }?.joinToString("\n")
    }

    private suspend fun refreshTrainingContextBeforeSend() {
        runAITrainingContextCatching { V2GolfProfileStore.reloadFromLocal() }
    }

    private fun buildAccountProfilePrompt(): String? {
        val p = V2ProfileStore.profile.value
        if (!p.isLoggedIn) return null
        val fields = mutableListOf<String>()
        p.nickname.takeIf { it.isMeaningfulProfileValue() && it != "高尔夫球友" }?.let { fields += "昵称$it" }
        p.handicap.takeIf { it.isMeaningfulProfileValue() }?.let { fields += "差点$it" }
        p.golfYears.takeIf { it.isMeaningfulProfileValue() }?.let { fields += "球龄$it" }
        p.dominantHand.takeIf { it.isMeaningfulProfileValue() }?.let { fields += "惯用手$it" }
        p.homeCourse.takeIf { it.isMeaningfulProfileValue() }?.let { fields += "主场$it" }
        p.location?.takeIf { it.isMeaningfulProfileValue() }?.let { fields += "地区$it" }
        val brands = p.golfBrands.takeIf { it.isNotEmpty() }?.joinToString("、") ?: p.clubBrand.takeIf { it.isMeaningfulProfileValue() }
        brands?.let { fields += "球具$it" }
        p.bio?.takeIf { it.isMeaningfulProfileValue() }?.let { fields += "简介${it.cleanAiSnippet(40)}" }
        return fields.takeIf { it.isNotEmpty() }?.joinToString("；", prefix = "用户资料: ")
    }

    private fun buildSwingPracticePrompt(): String? {
        val sessions = V2SwingSessionStore.sessions.value
        val clips = sessions.flatMap { it.clips }
        if (clips.isEmpty()) return null
        val today = LocalDate.now()
        val weekStart = today.minusDays(6)
        val monthStart = today.minusDays(29)
        val weekClips = clips.count { !it.timestamp.toLocalDate().isBefore(weekStart) }
        val monthClips = clips.count { !it.timestamp.toLocalDate().isBefore(monthStart) }
        val promoted = clips.count { it.retention.name == "PROMOTED" }
        val favorited = clips.count { it.retention.name == "FAVORITED" }
        val latest = clips.maxByOrNull { it.timestamp }
        val activeDays = clips.map { it.timestamp.toLocalDate() }.distinct().size
        val sourceSummary = clips
            .mapNotNull { it.sourceLabel?.takeIf(String::isNotBlank) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString("、") { "${it.key}${it.value}条" }
            .takeIf { it.isNotBlank() }

        val details = mutableListOf(
            "累计${clips.size}条挥杆视频",
            "覆盖${activeDays}个练习日",
            "近7天${weekClips}条",
            "近30天${monthClips}条",
        )
        if (promoted > 0) details += "成长池${promoted}条"
        if (favorited > 0) details += "收藏${favorited}条"
        latest?.let {
            val latestDay = it.timestamp.toLocalDate()
            val daysAgo = ChronoUnit.DAYS.between(latestDay, today)
            val dayText = when (daysAgo) {
                0L -> "今天"
                1L -> "昨天"
                else -> "${daysAgo}天前"
            }
            details += "最近一次$dayText(${formatAiDuration(it.durationSec) ?: "未知时长"})"
        }
        sourceSummary?.let { details += "来源$it" }
        return details.joinToString("，", prefix = "练习记录: ", postfix = "。")
    }

    private fun V2Memo.toAiContextLine(): String {
        val title = displayTitle ?: title ?: text?.cleanAiSnippet(18) ?: "未命名"
        val kind = when {
            hasVideo && hasText -> "视频+文字"
            hasVideo -> "视频"
            else -> "文字"
        }
        val details = mutableListOf(kind, topic.displayName)
        formatAiDuration(durationSec)?.let { details += it }
        trainingMethod?.takeIf { it.isMeaningfulProfileValue() }?.let { details += it }
        platform?.takeIf { it.isMeaningfulProfileValue() }?.let { details += it }
        if (isOnlineVideo) details += "在线"
        if (isPlazaVideo) details += "广场"
        tags.take(3).takeIf { it.isNotEmpty() }?.let { details += "标签:${it.joinToString("/")}" }
        if (linkedGoalIds.isNotEmpty()) details += "关联目标${linkedGoalIds.size}个"
        val note = text?.takeIf { it.isNotBlank() && it != title }?.cleanAiSnippet(56)
        return buildString {
            append(title.cleanAiSnippet(24))
            append("[")
            append(details.distinct().joinToString("/"))
            append("]")
            if (note != null) append(": ").append(note)
        }
    }

    private fun String.cleanAiSnippet(maxLength: Int): String {
        val cleaned = trim().replace(Regex("\\s+"), " ")
        return if (cleaned.length <= maxLength) cleaned else cleaned.take(maxLength) + "..."
    }

    private fun String.isMeaningfulProfileValue(): Boolean {
        val value = trim()
        return value.isNotEmpty() &&
            value != "未登录" &&
            value != "未设置" &&
            value != "保密" &&
            value != "—" &&
            value != "-"
    }

    private fun formatAiDuration(seconds: Double?): String? {
        val total = seconds?.toInt()?.takeIf { it > 0 } ?: return null
        val minutes = total / 60
        val remain = total % 60
        return if (minutes > 0) "${minutes}分${remain}秒" else "${total}秒"
    }

    private fun V2AISession.hasPersistableMessages(): Boolean =
        messages.any { msg ->
            msg.role != V2AIMessageRole.SYSTEM &&
                msg.text.isNotBlank() &&
                !msg.isStreaming
        }
}

/** 简洁 update 扩展（与其它 store 一致） */
private inline fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    while (true) {
        val current = value
        val next = transform(current)
        if (compareAndSet(current, next)) return
    }
}

internal fun shouldClearAIChatReflectionStatusOnCancellation(currentStatus: String?): Boolean =
    currentStatus == V2AIChatStore.AI_REFLECTION_STATUS

internal suspend inline fun <T> runAIQuotaCatching(
    crossinline block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}

internal suspend inline fun <T> runAITrainingContextCatching(
    crossinline block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
