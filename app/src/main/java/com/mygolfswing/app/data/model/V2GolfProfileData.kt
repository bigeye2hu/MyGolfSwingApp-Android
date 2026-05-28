package com.mygolfswing.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * AI 训练档案数据结构（对齐 iOS `GolfProfileData` / `ProfileItem` / `ProfileSource`）。
 * 云端以 `profileDataRaw` JSON 字符串存储，与 iOS 互通。
 */
@Serializable
data class V2GolfProfileData(
    val currentFocus: List<V2ProfileItem> = emptyList(),
    val painPoints: List<V2ProfileItem> = emptyList(),
    val strengths: List<V2ProfileItem> = emptyList(),
    val trainingGoals: List<String> = emptyList(),
    @SerialName("coachNotes") val coachNotes: List<String> = emptyList(),
    val statusSummary: String? = null,
    val summaryUpdatedAt: String? = null,
) {
    val isEmpty: Boolean
        get() = currentFocus.isEmpty() && painPoints.isEmpty() && strengths.isEmpty() &&
            trainingGoals.isEmpty() && coachNotes.isEmpty() && statusSummary.isNullOrBlank()

    companion object {
        val sectionNames = mapOf(
            "currentFocus" to "训练重点",
            "painPoints" to "主要痛点",
            "strengths" to "优势项",
            "trainingGoals" to "训练目标",
            "coachNotes" to "教练指导",
        )

        val sectionLimits = mapOf(
            "currentFocus" to 5,
            "painPoints" to 5,
            "strengths" to 5,
            "trainingGoals" to 3,
            "coachNotes" to 5,
        )
    }
}

@Serializable
data class V2ProfileItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val source: String = V2ProfileSource.USER_MANUAL.wire,
    val addedAt: String = V2ProfileDates.nowIso(),
    val note: String? = null,
)

enum class V2ProfileSource(val wire: String, val displayName: String) {
    AI_INFERRED("ai", "AI 推断"),
    USER_MANUAL("user", "手动填写"),
    COACH_ADVICE("coach", "教练建议"),
    USER_BOOKMARKED("bookmarked", "用户标记");

    companion object {
        fun fromWire(wire: String?): V2ProfileSource =
            entries.firstOrNull { it.wire == wire } ?: USER_MANUAL
    }
}

object V2ProfileDates {
    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

    fun nowIso(): String = formatter.format(Instant.now())

    fun parseIsoOrNull(text: String?): Instant? = runCatching {
        if (text.isNullOrBlank()) return@runCatching null
        Instant.parse(text)
    }.getOrNull()

    fun relativeLabel(text: String?): String? {
        val instant = parseIsoOrNull(text) ?: return null
        val mins = java.time.Duration.between(instant, Instant.now()).toMinutes()
        return when {
            mins < 1 -> "刚刚"
            mins < 60 -> "${mins} 分钟前"
            mins < 60 * 24 -> "${mins / 60} 小时前"
            mins < 60 * 24 * 7 -> "${mins / (60 * 24)} 天前"
            else -> "${mins / (60 * 24 * 30)} 个月前"
        }
    }
}
