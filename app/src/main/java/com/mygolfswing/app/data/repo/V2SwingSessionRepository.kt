package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.db.SwingSessionDao
import com.mygolfswing.app.data.db.SwingSessionEntity
import com.mygolfswing.app.data.model.V2SwingClip
import com.mygolfswing.app.data.model.V2SwingClipRetention
import com.mygolfswing.app.data.model.V2SwingSession
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
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
import java.time.temporal.ChronoUnit
import java.io.File
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

    suspend fun updateClipRetention(clipId: UUID, retention: V2SwingClipRetention) = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        val rows = dao.observeAll(uid).first()
        rows.forEach { row ->
            val decoded = decode(row) ?: return@forEach
            if (decoded.clips.none { it.id == clipId }) return@forEach
            val updated = decoded.copy(
                clips = decoded.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(retention = retention) else clip
                },
            )
            dao.upsert(encode(updated))
            return@withContext
        }
    }

    suspend fun deleteClips(clipIds: Set<UUID>) = withContext(Dispatchers.IO) {
        if (clipIds.isEmpty()) return@withContext
        val uid = session.current().userId ?: return@withContext
        val rows = dao.observeAll(uid).first()
        rows.forEach { row ->
            val decoded = decode(row) ?: return@forEach
            val removed = decoded.clips.filter { it.id in clipIds }
            if (removed.isEmpty()) return@forEach
            removed.forEach { it.deleteFiles() }
            val updatedClips = decoded.clips.filterNot { it.id in clipIds }
            if (updatedClips.isEmpty()) {
                dao.deleteById(decoded.id.toString())
            } else {
                dao.upsert(encode(decoded.copy(clips = updatedClips)))
            }
        }
    }

    suspend fun cleanupExpiredTemporaryClips(retentionDays: Long = 14) = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        val cutoff = LocalDateTime.now().minus(retentionDays, ChronoUnit.DAYS)
        val rows = dao.observeAll(uid).first()
        rows.forEach { row ->
            val decoded = decode(row) ?: return@forEach
            val expired = decoded.clips.filter { it.retention == V2SwingClipRetention.TEMPORARY && it.timestamp.isBefore(cutoff) }
            if (expired.isEmpty()) return@forEach
            expired.forEach { it.deleteFiles() }
            val updatedClips = decoded.clips - expired.toSet()
            if (updatedClips.isEmpty()) {
                dao.deleteById(decoded.id.toString())
            } else {
                dao.upsert(encode(decoded.copy(clips = updatedClips)))
            }
        }
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
                        "retention" to JsonPrimitive(c.retention.wire),
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
                retention = V2SwingClipRetention.fromWire(o["retention"]?.jsonPrimitive?.content),
            )
        }
        V2SwingSession(id = id, userId = userId, date = date, locationName = location, clips = clips)
    }.getOrNull()

    private fun V2SwingClip.deleteFiles() {
        runCatching { File(videoPath.removePrefix("file://")).delete() }
        thumbnailPath?.let { runCatching { File(it.removePrefix("file://")).delete() } }
    }
}
