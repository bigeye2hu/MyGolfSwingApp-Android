package com.mygolfswing.app.data.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/** 单日挥杆聚合卡（对齐 iOS `V2SwingSessionRowItem`） */
data class V2SwingDayItem(
    val id: UUID,
    val day: LocalDate,
    val dayNumber: String,
    val monthLabel: String,
    val relativeText: String,
    val title: String,
    val location: String? = null,
    val timeRange: String? = null,
    val clipCount: Int,
    val clipPreviewUrls: List<String> = emptyList(),
    val extraClipCount: Int = 0,
)

data class V2SwingClip(
    val id: UUID = UUID.randomUUID(),
    val sessionId: UUID,
    val videoPath: String,
    val thumbnailPath: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val durationSec: Double = 0.0,
    val retention: V2SwingClipRetention = V2SwingClipRetention.TEMPORARY,
)

enum class V2SwingClipRetention(val wire: String) {
    TEMPORARY("temporary"),
    FAVORITED("favorited"),
    PROMOTED("promoted"),
    ;

    val isPermanent: Boolean get() = this == FAVORITED || this == PROMOTED

    companion object {
        fun fromWire(value: String?): V2SwingClipRetention {
            val normalized = value?.trim()?.takeIf { it.isNotBlank() } ?: return TEMPORARY
            return entries.firstOrNull { it.wire.equals(normalized, ignoreCase = true) } ?: TEMPORARY
        }
    }
}

data class V2SwingSession(
    val id: UUID = UUID.randomUUID(),
    val userId: String,
    val date: LocalDate,
    val locationName: String? = null,
    val clips: List<V2SwingClip> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
