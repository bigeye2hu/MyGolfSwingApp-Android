package com.mygolfswing.app.data.json

import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoAccent
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.model.V2MemoSource
import com.mygolfswing.app.data.model.V2MemoThumbnailKind
import com.mygolfswing.app.data.model.V2PinState
import com.mygolfswing.app.data.model.V2Topic
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class V2JsonTest {

    @Test
    fun encodeMemoUsesIosRawValues() {
        val memo = V2Memo(
            id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            kind = V2MemoKind.TEXT,
            source = V2MemoSource.TEXT_INPUT,
            text = "练习下杆节奏",
            topic = V2Topic.SHORT_GAME,
            pinState = V2PinState.CANCELLING,
            createdAt = fixedDate,
            updatedAt = fixedDate,
            accent = V2MemoAccent.COACH_NOTE,
            thumbnailKind = V2MemoThumbnailKind.GREEN_GRASS,
        )

        val json = V2Json.encodeMemo(memo)

        assertEquals("text", json.string("kind"))
        assertEquals("textInput", json.string("source"))
        assertEquals("shortGame", json.string("topic"))
        assertEquals("cancelling", json.string("pinState"))
        assertEquals("coachNote", json.string("accent"))
        assertEquals("greenGrass", json.string("thumbnailKind"))
    }

    @Test
    fun decodeMemoAcceptsLegacySnakeCaseValues() {
        val json = JsonObject(
            mapOf(
                "id" to JsonPrimitive("00000000-0000-0000-0000-000000000002"),
                "kind" to JsonPrimitive("mixed"),
                "source" to JsonPrimitive("self_record"),
                "text" to JsonPrimitive("保留旧缓存兼容"),
                "topic" to JsonPrimitive("short_game"),
                "tags" to JsonArray(emptyList()),
                "tagIds" to JsonArray(emptyList()),
                "pinState" to JsonPrimitive("none"),
                "linkedGoalIds" to JsonArray(emptyList()),
                "createdAt" to JsonPrimitive(fixedMillis),
                "updatedAt" to JsonPrimitive(fixedMillis),
                "thumbnailKind" to JsonPrimitive("green_grass"),
                "accent" to JsonPrimitive("coach_note"),
            ),
        )

        val memo = V2Json.decodeMemo(json)

        assertEquals(V2MemoSource.SELF_RECORD, memo.source)
        assertEquals(V2Topic.SHORT_GAME, memo.topic)
        assertEquals(V2MemoThumbnailKind.GREEN_GRASS, memo.thumbnailKind)
        assertEquals(V2MemoAccent.COACH_NOTE, memo.accent)
    }

    @Test
    fun encodeGoalUsesIosRawValues() {
        val goal = V2Goal(
            id = UUID.fromString("00000000-0000-0000-0000-000000000003"),
            title = "一号木稳定性",
            iconType = V2Goal.IconType.CLUB,
            topic = V2Topic.SWING,
            metric = V2Goal.Metric(V2Goal.Metric.Kind.COUNT, 50.0, "球"),
            progress = V2Goal.Progress(current = 2.0),
            targetSessions = 5,
            clubKind = V2Goal.ClubKind.FAIRWAY_WOOD,
            pinState = V2PinState.PINNED,
            status = V2Goal.Status.ACTIVE,
            priority = V2Goal.Priority.CRITICAL,
            cadence = V2Goal.Cadence.PerWeek(3),
            startedAt = fixedDate,
            updatedAt = fixedDate,
        )

        val json = V2Json.encodeGoal(goal)
        val metric = json["metric"] as JsonObject
        val cadence = json["cadence"] as JsonObject

        assertEquals("club", json.string("iconType"))
        assertEquals("swing", json.string("topic"))
        assertEquals("count", metric.string("kind"))
        assertEquals("fairwayWood", json.string("clubKind"))
        assertEquals("pinned", json.string("pinState"))
        assertEquals("active", json.string("status"))
        assertEquals("critical", json.string("priority"))
        assertEquals("perWeek", cadence.string("kind"))
    }

    @Test
    fun decodeGoalAcceptsLegacySnakeCaseValues() {
        val json = JsonObject(
            mapOf(
                "id" to JsonPrimitive("00000000-0000-0000-0000-000000000004"),
                "title" to JsonPrimitive("短杆距离控制"),
                "iconType" to JsonPrimitive("target"),
                "topic" to JsonPrimitive("short_game"),
                "metric" to JsonObject(
                    mapOf(
                        "kind" to JsonPrimitive("distance"),
                        "target" to JsonPrimitive(0.3),
                        "unit" to JsonPrimitive("米"),
                    ),
                ),
                "progress" to JsonObject(
                    mapOf(
                        "current" to JsonPrimitive(0.0),
                        "lastIncrementAt" to JsonPrimitive(null as Long?),
                        "checkInDates" to JsonArray(emptyList()),
                    ),
                ),
                "clubKind" to JsonPrimitive("green_reading"),
                "pinState" to JsonPrimitive("none"),
                "status" to JsonPrimitive("active"),
                "priority" to JsonPrimitive("normal"),
                "derivedFromMemoIds" to JsonArray(emptyList()),
                "todayFocus" to JsonPrimitive(false),
                "completionCelebrated" to JsonPrimitive(false),
                "startedAt" to JsonPrimitive(fixedMillis),
                "updatedAt" to JsonPrimitive(fixedMillis),
            ),
        )

        val goal = V2Json.decodeGoal(json)

        assertEquals(V2Topic.SHORT_GAME, goal.topic)
        assertEquals(V2Goal.ClubKind.GREEN_READING, goal.clubKind)
        assertEquals(V2Goal.Metric.Kind.DISTANCE, goal.metric.kind)
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private companion object {
        val fixedDate: LocalDateTime = LocalDateTime.of(2026, 6, 4, 9, 0)
        val fixedMillis: Long = fixedDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
