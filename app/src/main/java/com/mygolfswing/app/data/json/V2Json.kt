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
        o["iconType"] = JsonPrimitive(goal.iconType.rawValue)
        o["topic"] = JsonPrimitive(goal.topic.rawValue)
        o["metric"] = JsonObject(
            mapOf(
                "kind" to JsonPrimitive(goal.metric.kind.rawValue),
                "target" to JsonPrimitive(goal.metric.target),
                "unit" to (goal.metric.unit?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?)),
            ),
        )
        o["progress"] = encodeProgress(goal.progress)
        goal.targetSessions?.let { o["targetSessions"] = JsonPrimitive(it) }
        goal.sessionUnit?.let { o["sessionUnit"] = JsonPrimitive(it) }
        goal.clubKind?.let { o["clubKind"] = JsonPrimitive(it.rawValue) }
        o["pinState"] = JsonPrimitive(goal.pinState.rawValue)
        o["status"] = JsonPrimitive(goal.status.rawValue)
        o["priority"] = JsonPrimitive(goal.priority.rawValue)
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
                kind = goalMetricKindFromRaw(m["kind"]!!.jsonPrimitive.content),
                target = m["target"]!!.jsonPrimitive.double,
                unit = m["unit"]?.jsonPrimitive?.contentOrNull,
            )
        }
        val progress = json["progress"]?.jsonObject?.let { decodeProgress(it) } ?: V2Goal.Progress()
        val cadence = json["cadence"]?.jsonObject?.let { decodeCadence(it) }

        return V2Goal(
            id = UUID.fromString(json["id"]!!.jsonPrimitive.content),
            title = json["title"]!!.jsonPrimitive.content,
            iconType = goalIconTypeFromRaw(json["iconType"]!!.jsonPrimitive.content),
            topic = topicFromRaw(json["topic"]!!.jsonPrimitive.content),
            metric = metric,
            progress = progress,
            targetSessions = json["targetSessions"]?.jsonPrimitive?.intOrNull,
            sessionUnit = json["sessionUnit"]?.jsonPrimitive?.contentOrNull,
            clubKind = json["clubKind"]?.jsonPrimitive?.contentOrNull?.let { goalClubKindFromRaw(it) },
            pinState = json["pinState"]?.jsonPrimitive?.contentOrNull?.let { pinStateFromRaw(it) } ?: V2PinState.NONE,
            status = goalStatusFromRaw(json["status"]!!.jsonPrimitive.content),
            priority = goalPriorityFromRaw(json["priority"]!!.jsonPrimitive.content),
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
        o["kind"] = JsonPrimitive(memo.kind.rawValue)
        o["source"] = JsonPrimitive(memo.source.rawValue)
        memo.videoUrl?.let { o["videoUrl"] = JsonPrimitive(it) }
        memo.thumbnailUrl?.let { o["thumbnailUrl"] = JsonPrimitive(it) }
        memo.durationSec?.let { o["durationSec"] = JsonPrimitive(it) }
        memo.text?.let { o["text"] = JsonPrimitive(it) }
        memo.title?.let { o["title"] = JsonPrimitive(it) }
        o["topic"] = JsonPrimitive(memo.topic.rawValue)
        o["tags"] = JsonArray(memo.tags.map { JsonPrimitive(it) })
        o["tagIds"] = JsonArray(memo.tagIds.map { JsonPrimitive(it) })
        o["pinState"] = JsonPrimitive(memo.pinState.rawValue)
        o["linkedGoalIds"] = JsonArray(memo.linkedGoalIds.map { JsonPrimitive(it.toString()) })
        o["createdAt"] = JsonPrimitive(memo.createdAt.toEpochMillis())
        o["updatedAt"] = JsonPrimitive(memo.updatedAt.toEpochMillis())
        o["isNew"] = JsonPrimitive(memo.isNew)
        memo.accent?.let { o["accent"] = JsonPrimitive(it.rawValue) }
        memo.displayTitle?.let { o["displayTitle"] = JsonPrimitive(it) }
        memo.metaText?.let { o["metaText"] = JsonPrimitive(it) }
        memo.subText?.let { o["subText"] = JsonPrimitive(it) }
        o["thumbnailKind"] = JsonPrimitive(memo.thumbnailKind.rawValue)
        memo.trainingMethod?.let { o["trainingMethod"] = JsonPrimitive(it) }
        o["isOnlineVideo"] = JsonPrimitive(memo.isOnlineVideo)
        memo.platform?.let { o["platform"] = JsonPrimitive(it) }
        memo.plazaVideoId?.let { o["plazaVideoId"] = JsonPrimitive(it.toString()) }
        o["priorityRaw"] = JsonPrimitive(memo.priorityRaw)
        return JsonObject(o)
    }

    fun decodeMemo(json: JsonObject): V2Memo = V2Memo(
        id = UUID.fromString(json["id"]!!.jsonPrimitive.content),
        kind = memoKindFromRaw(json["kind"]!!.jsonPrimitive.content),
        source = memoSourceFromRaw(json["source"]!!.jsonPrimitive.content),
        videoUrl = json["videoUrl"]?.jsonPrimitive?.contentOrNull,
        thumbnailUrl = json["thumbnailUrl"]?.jsonPrimitive?.contentOrNull,
        durationSec = json["durationSec"]?.jsonPrimitive?.doubleOrNull,
        text = json["text"]?.jsonPrimitive?.contentOrNull,
        title = json["title"]?.jsonPrimitive?.contentOrNull,
        topic = topicFromRaw(json["topic"]!!.jsonPrimitive.content),
        tags = json["tags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
        tagIds = json["tagIds"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
        pinState = json["pinState"]?.jsonPrimitive?.contentOrNull?.let { pinStateFromRaw(it) } ?: V2PinState.NONE,
        linkedGoalIds = json["linkedGoalIds"]?.jsonArray?.map { UUID.fromString(it.jsonPrimitive.content) } ?: emptyList(),
        createdAt = json["createdAt"]?.jsonPrimitive?.longOrNull?.toLocalDateTime() ?: LocalDateTime.now(),
        updatedAt = json["updatedAt"]?.jsonPrimitive?.longOrNull?.toLocalDateTime() ?: LocalDateTime.now(),
        isNew = json["isNew"]?.jsonPrimitive?.booleanOrNull ?: false,
        accent = json["accent"]?.jsonPrimitive?.contentOrNull?.let { memoAccentFromRaw(it) },
        displayTitle = json["displayTitle"]?.jsonPrimitive?.contentOrNull,
        metaText = json["metaText"]?.jsonPrimitive?.contentOrNull,
        subText = json["subText"]?.jsonPrimitive?.contentOrNull,
        thumbnailKind = json["thumbnailKind"]?.jsonPrimitive?.contentOrNull?.let { memoThumbnailKindFromRaw(it) }
            ?: V2MemoThumbnailKind.FAIRWAY,
        trainingMethod = json["trainingMethod"]?.jsonPrimitive?.contentOrNull,
        isOnlineVideo = json["isOnlineVideo"]?.jsonPrimitive?.booleanOrNull ?: false,
        platform = json["platform"]?.jsonPrimitive?.contentOrNull,
        plazaVideoId = json["plazaVideoId"]?.jsonPrimitive?.contentOrNull?.let { runCatching { UUID.fromString(it) }.getOrNull() },
        priorityRaw = json["priorityRaw"]?.jsonPrimitive?.contentOrNull ?: "medium",
    )

    /** iOS `JSONEncoder.dateEncodingStrategy = .millisecondsSince1970` 可能编码为 Long 或 Double */
    private fun JsonObject.epochMillis(key: String): Long? = this[key]?.epochMillis()

    private fun JsonElement.epochMillis(): Long? {
        val p = this as? JsonPrimitive ?: return null
        return p.longOrNull ?: p.doubleOrNull?.toLong()
    }

    private val V2PinState.rawValue: String
        get() = when (this) {
            V2PinState.NONE -> "none"
            V2PinState.PINNED -> "pinned"
            V2PinState.CANCELLING -> "cancelling"
        }

    private val V2MemoSource.rawValue: String
        get() = when (this) {
            V2MemoSource.SELF_RECORD -> "selfRecord"
            V2MemoSource.PLAZA_SAVE -> "plazaSave"
            V2MemoSource.TEXT_INPUT -> "textInput"
            V2MemoSource.SHARE -> "share"
        }

    private val V2MemoKind.rawValue: String
        get() = when (this) {
            V2MemoKind.VIDEO -> "video"
            V2MemoKind.TEXT -> "text"
            V2MemoKind.MIXED -> "mixed"
        }

    private val V2MemoThumbnailKind.rawValue: String
        get() = when (this) {
            V2MemoThumbnailKind.FAIRWAY -> "fairway"
            V2MemoThumbnailKind.SAND -> "sand"
            V2MemoThumbnailKind.GREEN_GRASS -> "greenGrass"
            V2MemoThumbnailKind.SUNSET -> "sunset"
            V2MemoThumbnailKind.MIST -> "mist"
            V2MemoThumbnailKind.FOREST -> "forest"
        }

    private val V2MemoAccent.rawValue: String
        get() = when (this) {
            V2MemoAccent.BREAKTHROUGH -> "breakthrough"
            V2MemoAccent.INSIGHT -> "insight"
            V2MemoAccent.REFLECTION -> "reflection"
            V2MemoAccent.COACH_NOTE -> "coachNote"
        }

    private val V2Topic.rawValue: String
        get() = when (this) {
            V2Topic.ALL -> "all"
            V2Topic.SWING -> "swing"
            V2Topic.PUTTING -> "putting"
            V2Topic.SHORT_GAME -> "shortGame"
            V2Topic.SAND -> "sand"
            V2Topic.MIND -> "mind"
            V2Topic.FITNESS -> "fitness"
            V2Topic.GEAR -> "gear"
        }

    private val V2Goal.IconType.rawValue: String
        get() = when (this) {
            V2Goal.IconType.TARGET -> "target"
            V2Goal.IconType.CLUB -> "club"
            V2Goal.IconType.PUTT -> "putt"
            V2Goal.IconType.SAND -> "sand"
            V2Goal.IconType.TIP -> "tip"
        }

    private val V2Goal.ClubKind.rawValue: String
        get() = when (this) {
            V2Goal.ClubKind.DRIVER -> "driver"
            V2Goal.ClubKind.FAIRWAY_WOOD -> "fairwayWood"
            V2Goal.ClubKind.IRON -> "iron"
            V2Goal.ClubKind.WEDGE -> "wedge"
            V2Goal.ClubKind.PUTTER -> "putter"
            V2Goal.ClubKind.SAND -> "sand"
            V2Goal.ClubKind.CHIP -> "chip"
            V2Goal.ClubKind.GREEN_READING -> "greenReading"
            V2Goal.ClubKind.OTHER -> "other"
        }

    private val V2Goal.Status.rawValue: String
        get() = when (this) {
            V2Goal.Status.ACTIVE -> "active"
            V2Goal.Status.PAUSED -> "paused"
            V2Goal.Status.COMPLETED -> "completed"
            V2Goal.Status.ARCHIVED -> "archived"
        }

    private val V2Goal.Priority.rawValue: String
        get() = when (this) {
            V2Goal.Priority.CRITICAL -> "critical"
            V2Goal.Priority.HIGH -> "high"
            V2Goal.Priority.NORMAL -> "normal"
            V2Goal.Priority.LOW -> "low"
        }

    private val V2Goal.Metric.Kind.rawValue: String
        get() = when (this) {
            V2Goal.Metric.Kind.COUNT -> "count"
            V2Goal.Metric.Kind.DURATION -> "duration"
            V2Goal.Metric.Kind.DISTANCE -> "distance"
            V2Goal.Metric.Kind.FREE -> "free"
        }

    private fun pinStateFromRaw(raw: String): V2PinState = when (raw.normalizedEnumKey()) {
        "none" -> V2PinState.NONE
        "pinned" -> V2PinState.PINNED
        "cancelling" -> V2PinState.CANCELLING
        else -> unknownRaw("V2PinState", raw)
    }

    private fun memoSourceFromRaw(raw: String): V2MemoSource = when (raw.normalizedEnumKey()) {
        "selfrecord" -> V2MemoSource.SELF_RECORD
        "plazasave" -> V2MemoSource.PLAZA_SAVE
        "textinput" -> V2MemoSource.TEXT_INPUT
        "share" -> V2MemoSource.SHARE
        else -> unknownRaw("V2MemoSource", raw)
    }

    private fun memoKindFromRaw(raw: String): V2MemoKind = when (raw.normalizedEnumKey()) {
        "video" -> V2MemoKind.VIDEO
        "text" -> V2MemoKind.TEXT
        "mixed" -> V2MemoKind.MIXED
        else -> unknownRaw("V2MemoKind", raw)
    }

    private fun memoThumbnailKindFromRaw(raw: String): V2MemoThumbnailKind = when (raw.normalizedEnumKey()) {
        "fairway" -> V2MemoThumbnailKind.FAIRWAY
        "sand" -> V2MemoThumbnailKind.SAND
        "greengrass" -> V2MemoThumbnailKind.GREEN_GRASS
        "sunset" -> V2MemoThumbnailKind.SUNSET
        "mist" -> V2MemoThumbnailKind.MIST
        "forest" -> V2MemoThumbnailKind.FOREST
        else -> unknownRaw("V2MemoThumbnailKind", raw)
    }

    private fun memoAccentFromRaw(raw: String): V2MemoAccent = when (raw.normalizedEnumKey()) {
        "breakthrough" -> V2MemoAccent.BREAKTHROUGH
        "insight" -> V2MemoAccent.INSIGHT
        "reflection" -> V2MemoAccent.REFLECTION
        "coachnote" -> V2MemoAccent.COACH_NOTE
        else -> unknownRaw("V2MemoAccent", raw)
    }

    private fun topicFromRaw(raw: String): V2Topic = when (raw.normalizedEnumKey()) {
        "all" -> V2Topic.ALL
        "swing" -> V2Topic.SWING
        "putting" -> V2Topic.PUTTING
        "shortgame" -> V2Topic.SHORT_GAME
        "sand" -> V2Topic.SAND
        "mind" -> V2Topic.MIND
        "fitness" -> V2Topic.FITNESS
        "gear" -> V2Topic.GEAR
        else -> unknownRaw("V2Topic", raw)
    }

    private fun goalIconTypeFromRaw(raw: String): V2Goal.IconType = when (raw.normalizedEnumKey()) {
        "target" -> V2Goal.IconType.TARGET
        "club" -> V2Goal.IconType.CLUB
        "putt" -> V2Goal.IconType.PUTT
        "sand" -> V2Goal.IconType.SAND
        "tip" -> V2Goal.IconType.TIP
        else -> unknownRaw("V2Goal.IconType", raw)
    }

    private fun goalClubKindFromRaw(raw: String): V2Goal.ClubKind = when (raw.normalizedEnumKey()) {
        "driver" -> V2Goal.ClubKind.DRIVER
        "fairwaywood" -> V2Goal.ClubKind.FAIRWAY_WOOD
        "iron" -> V2Goal.ClubKind.IRON
        "wedge" -> V2Goal.ClubKind.WEDGE
        "putter" -> V2Goal.ClubKind.PUTTER
        "sand" -> V2Goal.ClubKind.SAND
        "chip" -> V2Goal.ClubKind.CHIP
        "greenreading" -> V2Goal.ClubKind.GREEN_READING
        "other" -> V2Goal.ClubKind.OTHER
        else -> unknownRaw("V2Goal.ClubKind", raw)
    }

    private fun goalStatusFromRaw(raw: String): V2Goal.Status = when (raw.normalizedEnumKey()) {
        "active" -> V2Goal.Status.ACTIVE
        "paused" -> V2Goal.Status.PAUSED
        "completed" -> V2Goal.Status.COMPLETED
        "archived" -> V2Goal.Status.ARCHIVED
        else -> unknownRaw("V2Goal.Status", raw)
    }

    private fun goalPriorityFromRaw(raw: String): V2Goal.Priority = when (raw.normalizedEnumKey()) {
        "critical" -> V2Goal.Priority.CRITICAL
        "high" -> V2Goal.Priority.HIGH
        "normal" -> V2Goal.Priority.NORMAL
        "low" -> V2Goal.Priority.LOW
        else -> unknownRaw("V2Goal.Priority", raw)
    }

    private fun goalMetricKindFromRaw(raw: String): V2Goal.Metric.Kind = when (raw.normalizedEnumKey()) {
        "count" -> V2Goal.Metric.Kind.COUNT
        "duration" -> V2Goal.Metric.Kind.DURATION
        "distance" -> V2Goal.Metric.Kind.DISTANCE
        "free" -> V2Goal.Metric.Kind.FREE
        else -> unknownRaw("V2Goal.Metric.Kind", raw)
    }

    private fun String.normalizedEnumKey(): String =
        filter { it != '_' && it != '-' }.lowercase()

    private fun <T> unknownRaw(typeName: String, raw: String): T =
        error("Unknown enum value '$raw' in $typeName")
}
