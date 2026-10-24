package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.db.AIChatDao
import com.mygolfswing.app.data.db.AIMessageEntity
import com.mygolfswing.app.data.db.AISessionEntity
import com.mygolfswing.app.data.model.V2AIMessageRole
import com.mygolfswing.app.data.model.V2AISession
import com.mygolfswing.app.data.model.V2ChatAttachment
import com.mygolfswing.app.data.model.V2ChatMessage
import com.mygolfswing.app.data.model.V2ChatThumbnailKind
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class V2AIChatRepository(
    private val dao: AIChatDao,
    private val session: V2SessionStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadAllSessions(): List<V2AISession> = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext emptyList()
        dao.listSessions(uid).map { row ->
            val messages = dao.listMessages(row.id, uid).map { decodeMessage(it) }
            V2AISession(
                id = UUID.fromString(row.id),
                title = row.title,
                messages = messages,
                updatedAt = java.time.Instant.ofEpochMilli(row.updatedAt)
                    .atZone(ZoneId.systemDefault()).toLocalDateTime(),
            )
        }
    }

    suspend fun saveSession(session: V2AISession) = withContext(Dispatchers.IO) {
        val uid = this@V2AIChatRepository.session.current().userId ?: return@withContext
        val updatedMs = session.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dao.upsertSession(AISessionEntity(session.id.toString(), uid, session.title, updatedMs))
        session.messages.filter { !it.isStreaming }.forEach { msg ->
            dao.upsertMessage(
                AIMessageEntity(
                    id = msg.id.toString(),
                    ownerUserId = uid,
                    sessionId = session.id.toString(),
                    role = msg.role.name.lowercase(),
                    text = msg.text,
                    attachmentsJson = encodeAttachments(msg.attachments),
                    isStreaming = false,
                    createdAt = msg.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                ),
            )
        }
    }

    suspend fun deleteSession(sessionId: UUID) = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        dao.deleteSessionWithMessages(sessionId.toString(), uid)
    }

    suspend fun clearCurrentUser() = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        dao.clearAll(uid)
    }

    private fun decodeMessage(row: AIMessageEntity) = V2ChatMessage(
        id = UUID.fromString(row.id),
        role = V2AIMessageRole.entries.first { it.name.equals(row.role, ignoreCase = true) },
        text = row.text,
        attachments = decodeAttachments(row.attachmentsJson),
        isStreaming = row.isStreaming,
        createdAt = java.time.Instant.ofEpochMilli(row.createdAt)
            .atZone(ZoneId.systemDefault()).toLocalDateTime(),
    )

    private fun encodeAttachments(attachments: List<V2ChatAttachment>): String {
        if (attachments.isEmpty()) return "[]"
        val elements = attachments.map { attachment ->
            when (attachment) {
                is V2ChatAttachment.VideoRef -> JsonObject(
                    mapOf(
                        "type" to JsonPrimitive("videoRef"),
                        "title" to JsonPrimitive(attachment.title),
                        "durationSec" to JsonPrimitive(attachment.durationSec),
                        "statusLabel" to JsonPrimitive(attachment.statusLabel),
                        "thumbnailKind" to JsonPrimitive(attachment.thumbnailKind.name),
                    ),
                )
                is V2ChatAttachment.MemoSaved -> JsonObject(
                    mapOf(
                        "type" to JsonPrimitive("memoSaved"),
                        "title" to JsonPrimitive(attachment.title),
                        "summary" to JsonPrimitive(attachment.summary),
                        "tags" to JsonArray(attachment.tags.map(::JsonPrimitive)),
                        "isImportant" to JsonPrimitive(attachment.isImportant),
                    ),
                )
                is V2ChatAttachment.PlazaRecommend -> JsonObject(
                    mapOf(
                        "type" to JsonPrimitive("plazaRecommend"),
                        "title" to JsonPrimitive(attachment.title),
                        "summary" to JsonPrimitive(attachment.summary),
                        "durationSec" to JsonPrimitive(attachment.durationSec),
                        "statusLabel" to JsonPrimitive(attachment.statusLabel),
                    ),
                )
            }
        }
        return json.encodeToString(JsonArray.serializer(), JsonArray(elements))
    }

    private fun decodeAttachments(raw: String): List<V2ChatAttachment> = runCatching {
        json.parseToJsonElement(raw).jsonArray.mapNotNull(::decodeAttachment)
    }.getOrDefault(emptyList())

    private fun decodeAttachment(element: JsonElement): V2ChatAttachment? {
        val obj = element.jsonObject
        val type = obj.string("type") ?: return null
        return when (type) {
            "videoRef" -> V2ChatAttachment.VideoRef(
                title = obj.string("title") ?: return null,
                durationSec = obj.double("durationSec") ?: 0.0,
                statusLabel = obj.string("statusLabel") ?: "在线",
                thumbnailKind = obj.string("thumbnailKind")
                    ?.let { runCatching { V2ChatThumbnailKind.valueOf(it) }.getOrNull() }
                    ?: V2ChatThumbnailKind.FAIRWAY,
            )
            "memoSaved" -> V2ChatAttachment.MemoSaved(
                title = obj.string("title") ?: return null,
                summary = obj.string("summary") ?: "",
                tags = obj["tags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
                isImportant = obj["isImportant"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
            )
            "plazaRecommend" -> V2ChatAttachment.PlazaRecommend(
                title = obj.string("title") ?: return null,
                summary = obj.string("summary") ?: "",
                durationSec = obj.double("durationSec") ?: 0.0,
                statusLabel = obj.string("statusLabel") ?: "在线",
            )
            else -> null
        }
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.double(key: String): Double? =
        this[key]?.jsonPrimitive?.doubleOrNull

}
