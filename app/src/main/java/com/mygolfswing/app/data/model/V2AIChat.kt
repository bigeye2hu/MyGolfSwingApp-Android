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
    /** 嵌入附件（视频引用 / 已保存 Memo / 广场推荐），对齐 iOS V2ChatAttachment。 */
    val attachments: List<V2ChatAttachment> = emptyList(),
    /** 是否仍在生成（用于显示打字光标） */
    val isStreaming: Boolean = false,
)

sealed interface V2ChatAttachment {
    data class VideoRef(
        val title: String,
        val durationSec: Double,
        val statusLabel: String,
        val thumbnailKind: V2ChatThumbnailKind = V2ChatThumbnailKind.FAIRWAY,
    ) : V2ChatAttachment

    data class MemoSaved(
        val title: String,
        val summary: String,
        val tags: List<String> = emptyList(),
        val isImportant: Boolean = false,
    ) : V2ChatAttachment

    data class PlazaRecommend(
        val title: String,
        val summary: String,
        val durationSec: Double,
        val statusLabel: String,
    ) : V2ChatAttachment
}

enum class V2ChatThumbnailKind {
    FAIRWAY,
    SAND,
    GREEN_GRASS,
    SUNSET,
    MIST,
    FOREST,
}

data class V2AISession(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val messages: List<V2ChatMessage> = emptyList(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
