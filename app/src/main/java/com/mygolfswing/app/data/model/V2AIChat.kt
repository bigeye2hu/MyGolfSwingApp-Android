package com.mygolfswing.app.data.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * V2 AI 对话模型（对齐 iOS `V2ChatMessage` + `V2AIMessageRole`）。
 */
enum class V2AIMessageRole { USER, ASSISTANT, SYSTEM }

data class V2ChatMessage(
    val id: UUID = UUID.randomUUID(),
    val role: V2AIMessageRole,
    val text: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    /** 是否仍在生成（用于显示打字光标） */
    val isStreaming: Boolean = false,
)

data class V2AISession(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val messages: List<V2ChatMessage> = emptyList(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
