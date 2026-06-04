package com.mygolfswing.app.data.memo

import com.mygolfswing.app.data.model.V2MemoAccent
import com.mygolfswing.app.data.model.V2MemoThumbnailKind
import com.mygolfswing.app.data.model.V2Topic
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** V1 云端字段 → V2 展示字段（对齐 iOS `V2MemoBridge`） */
object V2MemoBridge {

    fun inferTopic(category: String?, tags: List<String>, title: String): V2Topic {
        val blob = (listOfNotNull(category) + tags + listOf(title)).joinToString(" ").lowercase()
        return when {
            blob.contains("推杆") || blob.contains("推球") || blob.contains("putt") -> V2Topic.PUTTING
            blob.contains("沙坑") || blob.contains("bunker") || blob.contains("sand") -> V2Topic.SAND
            blob.contains("短杆") || blob.contains("切杆") || blob.contains("劈杆") || blob.contains("chip") -> V2Topic.SHORT_GAME
            blob.contains("心法") || blob.contains("心态") || blob.contains("mind") -> V2Topic.MIND
            blob.contains("体能") || blob.contains("力量") || blob.contains("拉伸") || blob.contains("fitness") -> V2Topic.FITNESS
            blob.contains("装备") || blob.contains("球杆") || blob.contains("gear") || blob.contains("club") -> V2Topic.GEAR
            else -> V2Topic.SWING
        }
    }

    fun thumbnailKindFor(topic: V2Topic): V2MemoThumbnailKind = when (topic) {
        V2Topic.PUTTING, V2Topic.SHORT_GAME -> V2MemoThumbnailKind.GREEN_GRASS
        V2Topic.SAND -> V2MemoThumbnailKind.SAND
        V2Topic.MIND -> V2MemoThumbnailKind.MIST
        V2Topic.FITNESS -> V2MemoThumbnailKind.SUNSET
        V2Topic.GEAR -> V2MemoThumbnailKind.FOREST
        V2Topic.SWING, V2Topic.ALL -> V2MemoThumbnailKind.FAIRWAY
    }

    fun inferAccent(source: String?, priority: String, tags: List<String>): V2MemoAccent? {
        if (source == "ai") return V2MemoAccent.INSIGHT
        if (priority.equals("high", ignoreCase = true)) return V2MemoAccent.BREAKTHROUGH
        if (tags.any { it.contains("心法") }) return V2MemoAccent.INSIGHT
        if (tags.any { it.contains("反思") }) return V2MemoAccent.REFLECTION
        return null
    }

    fun isNewlyCreated(createdAt: LocalDateTime): Boolean =
        ChronoUnit.HOURS.between(createdAt, LocalDateTime.now()) < 24

    fun relativeMetaText(createdAt: LocalDateTime, extra: String? = null): String {
        val zh = Locale.CHINA
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm", zh)
        val timeStr = timeFmt.format(createdAt)
        val today = LocalDateTime.now().toLocalDate()
        val day = createdAt.toLocalDate()
        val prefix = when (ChronoUnit.DAYS.between(day, today)) {
            0L -> "今天 $timeStr"
            1L -> "昨天 $timeStr"
            in 2..6 -> "${ChronoUnit.DAYS.between(day, today)}天前"
            else -> DateTimeFormatter.ofPattern("M月d日", zh).format(createdAt)
        }
        return if (!extra.isNullOrBlank()) "$prefix · $extra" else prefix
    }

    fun videoMemoBody(description: String, trainingMethod: String?, tags: List<String>): String {
        val trimmed = description.trim()
        if (trimmed.isNotEmpty()) return trimmed
        if (!trainingMethod.isNullOrBlank()) return trainingMethod
        if (tags.isNotEmpty()) return tags.joinToString(" · ")
        return "—"
    }

    fun videoMemoExtraMeta(trainingMethod: String?, tags: List<String>): String? {
        val parts = buildList {
            trainingMethod?.takeIf { it.isNotBlank() }?.let { add(it) }
            tags.firstOrNull { it.isNotBlank() }?.let { add(it) }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    /** 规范化本地视频路径为绝对路径（无 file:// 前缀） */
    fun localVideoAbsolutePath(videoUrl: String?): String? {
        if (videoUrl.isNullOrBlank()) return null
        val trimmed = videoUrl.trim()
        if (isRemoteUrl(trimmed)) return null
        return trimmed.removeFileScheme()
    }

    fun coilImageModel(pathOrUrl: String?): String? {
        if (pathOrUrl.isNullOrBlank()) return null
        val t = pathOrUrl.trim()
        if (isRemoteUrl(t)) return t.replaceHttpWithHttps()
        if (t.startsWith(FILE_SCHEME, ignoreCase = true)) return "$FILE_SCHEME${t.drop(FILE_SCHEME.length)}"
        return "file://$t"
    }

    fun isRemoteUrl(value: String?): Boolean {
        val t = value?.trim() ?: return false
        return t.startsWith("http://", ignoreCase = true) ||
            t.startsWith("https://", ignoreCase = true)
    }

    private fun String.removeFileScheme(): String =
        if (startsWith(FILE_SCHEME, ignoreCase = true)) drop(FILE_SCHEME.length) else this

    private fun String.replaceHttpWithHttps(): String =
        if (startsWith("http://", ignoreCase = true)) "https://" + drop("http://".length) else this

    private const val FILE_SCHEME = "file://"
}
