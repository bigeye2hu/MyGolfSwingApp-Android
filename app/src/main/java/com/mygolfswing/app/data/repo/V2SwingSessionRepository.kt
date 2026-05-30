package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.db.SwingSessionDao
import com.mygolfswing.app.data.db.SwingSessionEntity
import com.mygolfswing.app.data.model.V2SwingClip
import com.mygolfswing.app.data.model.V2SwingSession
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class V2SwingSessionRepository(
    private val dao: SwingSessionDao,
    private val net: V2NetClient,
    private val session: V2SessionStore,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAll(): Flow<List<V2SwingSession>> =
        session.state.flatMapLatest { current ->
            val uid = current.userId.orEmpty()
            if (uid.isBlank()) return@flatMapLatest flowOf(emptyList())
            dao.observeAll(uid).map { rows -> rows.mapNotNull { decode(it) } }
        }

    suspend fun addClip(userId: String, videoPath: String, thumbnailPath: String?, durationSec: Double) =
        withContext(Dispatchers.IO) {
            val today = LocalDate.now()
            val existing = dao.getByDay(userId, today.toEpochDay())
            val session = existing?.let { decode(it) } ?: V2SwingSession(userId = userId, date = today)
            val clip = V2SwingClip(
                sessionId = session.id,
                videoPath = videoPath,
                thumbnailPath = thumbnailPath,
                durationSec = durationSec,
            )
            val updated = session.copy(clips = session.clips + clip)
            dao.upsert(encode(updated))
        }

    private fun encode(s: V2SwingSession): SwingSessionEntity {
        val clipsJson = JsonArray(
            s.clips.map { c ->
                JsonObject(
                    mapOf(
                        "id" to JsonPrimitive(c.id.toString()),
                        "videoPath" to JsonPrimitive(c.videoPath),
                        "thumbnailPath" to JsonPrimitive(c.thumbnailPath ?: ""),
                        "timestamp" to JsonPrimitive(c.timestamp.toString()),
                        "durationSec" to JsonPrimitive(c.durationSec),
                    ),
                )
            },
        )
        val root = JsonObject(
            mapOf(
                "id" to JsonPrimitive(s.id.toString()),
                "userId" to JsonPrimitive(s.userId),
                "date" to JsonPrimitive(s.date.toString()),
                "locationName" to JsonPrimitive(s.locationName ?: ""),
                "clips" to clipsJson,
            ),
        )
        return SwingSessionEntity(
            id = s.id.toString(),
            userId = s.userId,
            dayEpoch = s.date.toEpochDay(),
            sessionJson = net.json.encodeToString(JsonObject.serializer(), root),
            updatedAt = System.currentTimeMillis(),
        )
    }

    private fun decode(row: SwingSessionEntity): V2SwingSession? = runCatching {
        val root = net.json.parseToJsonElement(row.sessionJson).jsonObject
        val id = UUID.fromString(root["id"]!!.jsonPrimitive.content)
        val userId = root["userId"]!!.jsonPrimitive.content
        val date = LocalDate.parse(root["date"]!!.jsonPrimitive.content)
        val location = root["locationName"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        val clips = root["clips"]!!.jsonArray.map { el ->
            val o = el.jsonObject
            V2SwingClip(
                id = UUID.fromString(o["id"]!!.jsonPrimitive.content),
                sessionId = id,
                videoPath = o["videoPath"]!!.jsonPrimitive.content,
                thumbnailPath = o["thumbnailPath"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                timestamp = LocalDateTime.parse(o["timestamp"]!!.jsonPrimitive.content),
                durationSec = o["durationSec"]!!.jsonPrimitive.content.toDouble(),
            )
        }
        V2SwingSession(id = id, userId = userId, date = date, locationName = location, clips = clips)
    }.getOrNull()
}
