package com.mygolfswing.app.data.friend

data class V2IMConversation(
    val userId: String,
    val title: String,
    val avatar: String? = null,
    val preview: String,
    val timeEpochSeconds: Long? = null,
    val unreadCount: Int = 0,
    val isCoach: Boolean = false,
)

data class V2IMMessage(
    val messageId: String,
    val senderUserId: String,
    val text: String = "",
    val timeEpochSeconds: Long,
    val isMine: Boolean,
    val kind: V2IMMessageKind = V2IMMessageKind.TEXT,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val fileName: String? = null,
    val fileSizeBytes: Long? = null,
    val durationSeconds: Int? = null,
    val reactions: List<V2IMReaction> = emptyList(),
    val translatedText: String? = null,
    val senderName: String? = null,
    val senderAvatar: String? = null,
    val mergedTitle: String? = null,
    val mergedAbstracts: List<String> = emptyList(),
    val mergedLayersOverLimit: Boolean = false,
    val deliveryState: V2IMDeliveryState = V2IMDeliveryState.SENT,
    val failureMessage: String? = null,
)

data class V2IMMessagePage(
    val messages: List<V2IMMessage>,
    val hasMoreOlder: Boolean,
)

enum class V2IMDeliveryState {
    SENT,
    SENDING,
    FAILED,
    VIOLATION,
}

data class V2IMReaction(
    val reactionId: String,
    val totalUserCount: Int,
    val reactedByMe: Boolean,
)

data class V2IMReactionUser(
    val userId: String,
    val nickname: String? = null,
    val avatar: String? = null,
)

enum class V2IMMessageKind {
    SYSTEM,
    TEXT,
    IMAGE,
    VIDEO,
    VOICE,
    FILE,
    MERGED,
    UNSUPPORTED,
}

data class V2C2CSettings(
    val userId: String,
    val remark: String = "",
    val isFriend: Boolean = false,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val isBlocked: Boolean = false,
)

internal fun conversationPreview(
    text: String?,
    hasImage: Boolean,
    hasMessage: Boolean,
    hasVideo: Boolean = false,
    hasVoice: Boolean = false,
    hasFile: Boolean = false,
    hasMerged: Boolean = false,
): String = when {
    !text.isNullOrBlank() -> text
    hasImage -> "[图片]"
    hasVideo -> "[视频]"
    hasVoice -> "[语音]"
    hasFile -> "[文件]"
    hasMerged -> "[聊天记录]"
    hasMessage -> "[消息]"
    else -> "开始聊天"
}
