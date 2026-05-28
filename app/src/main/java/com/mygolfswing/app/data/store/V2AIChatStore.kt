package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.ai.V2AIMockProvider
import com.mygolfswing.app.data.ai.V2AIProvider
import com.mygolfswing.app.data.model.V2AIMessageRole
import com.mygolfswing.app.data.model.V2AISession
import com.mygolfswing.app.data.model.V2ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

/**
 * V2 AI 对话 Store（对齐 iOS `V2AIChatViewModel` + `AIConversationManager`）。
 *
 * 一个全局当前会话 + 历史会话列表，UI 通过 [currentSession] / [statusBar] 订阅。
 */
object V2AIChatStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * 备用 mock provider（无网络 / 调试时使用）。
     * 真实调用走 [ServiceLocator.aiService]。当 [useMockProvider] = true 时降级。
     */
    var provider: V2AIProvider = V2AIMockProvider
    var useMockProvider: Boolean = false

    private val _sessions = MutableStateFlow<List<V2AISession>>(emptyList())
    val sessions: StateFlow<List<V2AISession>> = _sessions.asStateFlow()

    private val _currentSession = MutableStateFlow(
        V2AISession(title = "新对话", messages = listOf(welcomeMessage())),
    )
    val currentSession: StateFlow<V2AISession> = _currentSession.asStateFlow()

    private val _statusBar = MutableStateFlow<String?>(null)
    val statusBar: StateFlow<String?> = _statusBar.asStateFlow()

    private var streamJob: Job? = null

    val isReplying: Boolean get() = streamJob?.isActive == true

    private fun welcomeMessage(): V2ChatMessage = V2ChatMessage(
        role = V2AIMessageRole.ASSISTANT,
        text = "嗨，我是你的 AI 高尔夫教练。今天想聊挥杆、推杆、还是练球计划？",
    )

    fun loadPersisted() {
        scope.launch {
            val list = runCatching { ServiceLocator.aiChatRepo.loadAllSessions() }.getOrDefault(emptyList())
            if (list.isNotEmpty()) {
                _currentSession.value = list.first()
                _sessions.value = list.drop(1)
            }
        }
    }

    fun startNewSession() {
        streamJob?.cancel()
        _statusBar.value = null
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
        _statusBar.value = null
        _sessions.update { it.filterNot { s -> s.id == toId } }
        _currentSession.value = target
    }

    /** 用户发送 + 流式回复（同步更新 currentSession） */
    fun send(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || isReplying) return

        val userMsg = V2ChatMessage(role = V2AIMessageRole.USER, text = trimmed)
        val placeholder = V2ChatMessage(role = V2AIMessageRole.ASSISTANT, text = "", isStreaming = true)
        _currentSession.update { s ->
            s.copy(
                title = if (s.messages.size <= 1) trimmed.take(16) else s.title,
                messages = s.messages + userMsg + placeholder,
                updatedAt = LocalDateTime.now(),
            )
        }
        _statusBar.value = "AI 正在思考…"

        streamJob = scope.launch {
            val history = _currentSession.value.messages
                .filter { it.role != V2AIMessageRole.SYSTEM }
                .map { (if (it.role == V2AIMessageRole.USER) "user" else "assistant") to it.text }

            val buffer = StringBuilder()
            try {
                val flow = if (useMockProvider) {
                    provider.streamReply(trimmed, history)
                } else {
                    ServiceLocator.aiService.streamChat(history)
                }
                flow.collect { chunk ->
                    buffer.append(chunk)
                    appendToLast(buffer.toString(), streaming = true)
                }
                appendToLast(buffer.toString(), streaming = false)
            } catch (e: Exception) {
                // 真实 Provider 失败时，自动降级到 mock（保证 UI 仍能演示）
                if (!useMockProvider) {
                    try {
                        provider.streamReply(trimmed, history).collect { chunk ->
                            buffer.append(chunk)
                            appendToLast(buffer.toString(), streaming = true)
                        }
                        appendToLast(buffer.toString(), streaming = false)
                    } catch (e2: Exception) {
                        appendToLast("（生成失败：${e2.message ?: "未知错误"}）", streaming = false)
                    }
                } else {
                    appendToLast("（生成失败：${e.message ?: "未知错误"}）", streaming = false)
                }
            } finally {
                _statusBar.value = null
                persistCurrent()
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
        _statusBar.value = null
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
