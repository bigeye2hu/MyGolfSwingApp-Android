package com.mygolfswing.app.data.json

import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoAccent
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.model.V2MemoSource
import com.mygolfswing.app.data.model.V2MemoThumbnailKind
import com.mygolfswing.app.data.model.V2PinState
import com.mygolfswing.app.data.model.V2Topic
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * V2 数据模型 ↔ JSON 字符串编解码（对齐 iOS Codable 协议）。
 *
 * 关键约定（与 iOS 端 `V2Goal` / `VideoMemo` 编码保持一致）：
 * - 所有 `LocalDateTime` 编码为「毫秒时间戳」(Long)
 * - `UUID` 编码为字符串
 * - `Cadence` 用 「discriminator + payload」的形式：
 *   - `{ "kind": "daysStreak", "value": 7 }`
 *   - `{ "kind": "perWeek", "value": 5 }`
 *   - `{ "kind": "withinMonths", "value": 1 }`
 *   - `{ "kind": "continuous" }`
 *
 * 该文件只负责本地存储；云同步用 `dataSync` 时，`goalJson` 字段直接复用这套编码。
 */
object V2Json {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun LocalDateTime.toEpochMillis(): Long =
        atZone(zone).toInstant().toEpochMilli()

    private fun Long.toLocalDateTime(): LocalDateTime =
        java.time.Instant.ofEpochMilli(this).atZone(zone).toLocalDateTime()

    // =============================================================
    // V2Goal
    // =============================================================

    fun encodeGoal(goal: V2Goal): JsonObject {
        val o = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
        o["id"] = JsonPrimitive(goal.id.toString())
        o["title"] = JsonPrimitive(goal.title)
        o["iconType"] = JsonPrimitive(goal.iconType.name.lowercase())
        o["topic"] = JsonPrimitive(goal.topic.name.lowercase())
        o["metric"] = JsonObject(
            mapOf(
                "kind" to JsonPrimitive(goal.metric.kind.name.lowercase()),
                "target" to JsonPrimitive(goal.metric.target),
                "unit" to (goal.metric.unit?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?)),
            ),
        )
        o["progress"] = encodeProgress(goal.progress)
        goal.targetSessions?.let { o["targetSessions"] = JsonPrimitive(it) }
        goal.sessionUnit?.let { o["sessionUnit"] = JsonPrimitive(it) }
        goal.clubKind?.let { o["clubKind"] = JsonPrimitive(it.name.lowercase()) }
        o["pinState"] = JsonPrimitive(goal.pinState.name.lowercase())
        o["status"] = JsonPrimitive(goal.status.name.lowercase())
        o["priority"] = JsonPrimitive(goal.priority.name.lowercase())
        goal.cadence?.let { o["cadence"] = encodeCadence(it) }
        o["derivedFromMemoIds"] = JsonArray(goal.derivedFromMemoIds.map { JsonPrimitive(it.toString()) })
        o["todayFocus"] = JsonPrimitive(goal.todayFocus)
        o["completionCelebrated"] = JsonPrimitive(goal.completionCelebrated)
        o["startedAt"] = JsonPrimitive(goal.startedAt.toEpochMillis())
        goal.completedAt?.let { o["completedAt"] = JsonPrimitive(it.toEpochMillis()) }
        o["updatedAt"] = JsonPrimitive(goal.updatedAt.toEpochMillis())
        return JsonObject(o)
    }

    fun decodeGoal(json: JsonObject): V2Goal {
        val metric = json["metric"]!!.jsonObject.let { m ->
            V2Goal.Metric(
                kind = enumByName(m["kind"]!!.jsonPrimitive.content, V2Goal.Metric.Kind.entries),
                target = m["target"]!!.jsonPrimitive.double,
                unit = m["unit"]?.jsonPrimitive?.contentOrNull,
            )
        }
        val progress = json["progress"]?.jsonObject?.let { decodeProgress(it) } ?: V2Goal.Progress()
        val cadence = json["cadence"]?.jsonObject?.let { decodeCadence(it) }

        return V2Goal(
            id = UUID.fromString(json["id"]!!.jsonPrimitive.content),
            title = json["title"]!!.jsonPrimitive.content,
            iconType = enumByName(json["iconType"]!!.jsonPrimitive.content, V2Goal.IconType.entries),
            topic = enumByName(json["topic"]!!.jsonPrimitive.content, V2Topic.entries),
            metric = metric,
            progress = progress,
            targetSessions = json["targetSessions"]?.jsonPrimitive?.intOrNull,
            sessionUnit = json["sessionUnit"]?.jsonPrimitive?.contentOrNull,
            clubKind = json["clubKind"]?.jsonPrimitive?.contentOrNull?.let { enumByName(it, V2Goal.ClubKind.entries) },
            pinState = json["pinState"]?.jsonPrimitive?.contentOrNull?.let { enumByName(it, V2PinState.entries) } ?: V2PinState.NONE,
            status = enumByName(json["status"]!!.jsonPrimitive.content, V2Goal.Status.entries),
            priority = enumByName(json["priority"]!!.jsonPrimitive.content, V2Goal.Priority.entries),
            cadence = cadence,
            derivedFromMemoIds = json["derivedFromMemoIds"]?.jsonArray?.map { UUID.fromString(it.jsonPrimitive.content) } ?: emptyList(),
            todayFocus = json["todayFocus"]?.jsonPrimitive?.booleanOrNull ?: false,
            completionCelebrated = json["completionCelebrated"]?.jsonPrimitive?.booleanOrNull ?: false,
            startedAt = json.epochMillis("startedAt")?.toLocalDateTime() ?: LocalDateTime.now(),
            completedAt = json.epochMillis("completedAt")?.toLocalDateTime(),
            updatedAt = json.epochMillis("updatedAt")?.toLocalDateTime() ?: LocalDateTime.now(),
        )
    }

    private fun encodeProgress(p: V2Goal.Progress): JsonObject = JsonObject(
        mapOf(
            "current" to JsonPrimitive(p.current),
            "lastIncrementAt" to (p.lastIncrementAt?.let { JsonPrimitive(it.toEpochMillis()) } ?: JsonPrimitive(null as Long?)),
            "checkInDates" to JsonArray(p.checkInDates.map { JsonPrimitive(it.toEpochMillis()) }),
        ),
    )

    private fun decodeProgress(o: JsonObject): V2Goal.Progress = V2Goal.Progress(
        current = o["current"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
        lastIncrementAt = o.epochMillis("lastIncrementAt")?.toLocalDateTime(),
        checkInDates = o["checkInDates"]?.jsonArray?.mapNotNull { it.epochMillis()?.toLocalDateTime() } ?: emptyList(),
    )

    private fun encodeCadence(c: V2Goal.Cadence): JsonObject = when (c) {
        is V2Goal.Cadence.DaysStreak -> JsonObject(
            mapOf("kind" to JsonPrimitive("daysStreak"), "value" to JsonPrimitive(c.days)),
        )
        is V2Goal.Cadence.PerWeek -> JsonObject(
            mapOf("kind" to JsonPrimitive("perWeek"), "value" to JsonPrimitive(c.times)),
        )
        is V2Goal.Cadence.WithinMonths -> JsonObject(
            mapOf("kind" to JsonPrimitive("withinMonths"), "value" to JsonPrimitive(c.months)),
        )
        V2Goal.Cadence.Continuous -> JsonObject(
            mapOf("kind" to JsonPrimitive("continuous")),
        )
    }

    private fun decodeCadence(o: JsonObject): V2Goal.Cadence? {
        val kind = o["kind"]?.jsonPrimitive?.contentOrNull ?: return null
        return when (kind) {
            "daysStreak" -> V2Goal.Cadence.DaysStreak(o["value"]?.jsonPrimitive?.intOrNull ?: 1)
            "perWeek" -> V2Goal.Cadence.PerWeek(o["value"]?.jsonPrimitive?.intOrNull ?: 1)
            "withinMonths" -> V2Goal.Cadence.WithinMonths(o["value"]?.jsonPrimitive?.intOrNull ?: 1)
            "continuous" -> V2Goal.Cadence.Continuous
            else -> null
        }
    }

    // =============================================================
    // V2Memo
    // =============================================================

    fun encodeMemo(memo: V2Memo): JsonObject {
        val o = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
        o["id"] = JsonPrimitive(memo.id.toString())
        o["kind"] = JsonPrimitive(memo.kind.name.lowercase())
        o["source"] = JsonPrimitive(memo.source.name.lowercase())
        memo.videoUrl?.let { o["videoUrl"] = JsonPrimitive(it) }
        memo.thumbnailUrl?.let { o["thumbnailUrl"] = JsonPrimitive(it) }
        memo.durationSec?.let { o["durationSec"] = JsonPrimitive(it) }
        memo.text?.let { o["text"] = JsonPrimitive(it) }
        memo.title?.let { o["title"] = JsonPrimitive(it) }
        o["topic"] = JsonPrimitive(memo.topic.name.lowercase())
        o["tags"] = JsonArray(memo.tags.map { JsonPrimitive(it) })
        o["tagIds"] = JsonArray(memo.tagIds.map { JsonPrimitive(it) })
        o["pinState"] = JsonPrimitive(memo.pinState.name.lowercase())
        o["linkedGoalIds"] = JsonArray(memo.linkedGoalIds.map { JsonPrimitive(it.toString()) })
        o["createdAt"] = JsonPrimitive(memo.createdAt.toEpochMillis())
        o["updatedAt"] = JsonPrimitive(memo.updatedAt.toEpochMillis())
        o["isNew"] = JsonPrimitive(memo.isNew)
        memo.accent?.let { o["accent"] = JsonPrimitive(it.name.lowercase()) }
        memo.displayTitle?.let { o["displayTitle"] = JsonPrimitive(it) }
        memo.metaText?.let { o["metaText"] = JsonPrimitive(it) }
        memo.subText?.let { o["subText"] = JsonPrimitive(it) }
        o["thumbnailKind"] = JsonPrimitive(memo.thumbnailKind.name.lowercase())
        memo.trainingMethod?.let { o["trainingMethod"] = JsonPrimitive(it) }
        o["isOnlineVideo"] = JsonPrimitive(memo.isOnlineVideo)
        memo.platform?.let { o["platform"] = JsonPrimitive(it) }
        memo.plazaVideoId?.let { o["plazaVideoId"] = JsonPrimitive(it.toString()) }
        o["priorityRaw"] = JsonPrimitive(memo.priorityRaw)
        return JsonObject(o)
    }

    fun decodeMemo(json: JsonObject): V2Memo = V2Memo(
        id = UUID.fromString(json["id"]!!.jsonPrimitive.content),
        kind = enumByName(json["kind"]!!.jsonPrimitive.content, V2MemoKind.entries),
        source = enumByName(json["source"]!!.jsonPrimitive.content, V2MemoSource.entries),
        videoUrl = json["videoUrl"]?.jsonPrimitive?.contentOrNull,
        thumbnailUrl = json["thumbnailUrl"]?.jsonPrimitive?.contentOrNull,
        durationSec = json["durationSec"]?.jsonPrimitive?.doubleOrNull,
        text = json["text"]?.jsonPrimitive?.contentOrNull,
        title = json["title"]?.jsonPrimitive?.contentOrNull,
        topic = enumByName(json["topic"]!!.jsonPrimitive.content, V2Topic.entries),
        tags = json["tags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
        tagIds = json["tagIds"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
        pinState = json["pinState"]?.jsonPrimitive?.contentOrNull?.let { enumByName(it, V2PinState.entries) } ?: V2PinState.NONE,
        linkedGoalIds = json["linkedGoalIds"]?.jsonArray?.map { UUID.fromString(it.jsonPrimitive.content) } ?: emptyList(),
        createdAt = json["createdAt"]?.jsonPrimitive?.longOrNull?.toLocalDateTime() ?: LocalDateTime.now(),
        updatedAt = json["updatedAt"]?.jsonPrimitive?.longOrNull?.toLocalDateTime() ?: LocalDateTime.now(),
        isNew = json["isNew"]?.jsonPrimitive?.booleanOrNull ?: false,
        accent = json["accent"]?.jsonPrimitive?.contentOrNull?.let { enumByName(it, V2MemoAccent.entries) },
        displayTitle = json["displayTitle"]?.jsonPrimitive?.contentOrNull,
        metaText = json["metaText"]?.jsonPrimitive?.contentOrNull,
        subText = json["subText"]?.jsonPrimitive?.contentOrNull,
        thumbnailKind = json["thumbnailKind"]?.jsonPrimitive?.contentOrNull?.let { enumByName(it, V2MemoThumbnailKind.entries) }
            ?: V2MemoThumbnailKind.FAIRWAY,
        trainingMethod = json["trainingMethod"]?.jsonPrimitive?.contentOrNull,
        isOnlineVideo = json["isOnlineVideo"]?.jsonPrimitive?.booleanOrNull ?: false,
        platform = json["platform"]?.jsonPrimitive?.contentOrNull,
        plazaVideoId = json["plazaVideoId"]?.jsonPrimitive?.contentOrNull?.let { runCatching { UUID.fromString(it) }.getOrNull() },
        priorityRaw = json["priorityRaw"]?.jsonPrimitive?.contentOrNull ?: "medium",
    )

    private inline fun <reified E : Enum<E>> enumByName(name: String, entries: List<E>): E {
        return entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: error("Unknown enum value '$name' in ${E::class.simpleName}")
    }

    /** iOS `JSONEncoder.dateEncodingStrategy = .millisecondsSince1970` 可能编码为 Long 或 Double */
    private fun JsonObject.epochMillis(key: String): Long? = this[key]?.epochMillis()

    private fun JsonElement.epochMillis(): Long? {
        val p = this as? JsonPrimitive ?: return null
        return p.longOrNull ?: p.doubleOrNull?.toLong()
    }
}
