package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.db.AIChatDao
import com.mygolfswing.app.data.db.AIMessageEntity
import com.mygolfswing.app.data.db.AISessionEntity
import com.mygolfswing.app.data.model.V2AIMessageRole
import com.mygolfswing.app.data.model.V2AISession
import com.mygolfswing.app.data.model.V2ChatMessage
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class V2AIChatRepository(
    private val dao: AIChatDao,
    private val session: V2SessionStore,
) {

    suspend fun loadAllSessions(): List<V2AISession> = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext emptyList()
        dao.listSessions(uid).map { row ->
            val messages = dao.listMessages(row.id, uid).map { decodeMessage(it) }
            V2AISession(
                id = UUID.fromString(row.id),
                title = row.title,
                messages = messages.ifEmpty { listOf(welcome()) },
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
                    isStreaming = false,
                    createdAt = msg.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                ),
            )
        }
    }

    suspend fun deleteSession(sessionId: UUID) = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        dao.deleteSession(sessionId.toString(), uid)
    }

    suspend fun clearCurrentUser() = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        dao.clearAll(uid)
    }

    private fun decodeMessage(row: AIMessageEntity) = V2ChatMessage(
        id = UUID.fromString(row.id),
        role = V2AIMessageRole.entries.first { it.name.equals(row.role, ignoreCase = true) },
        text = row.text,
        isStreaming = row.isStreaming,
        createdAt = java.time.Instant.ofEpochMilli(row.createdAt)
            .atZone(ZoneId.systemDefault()).toLocalDateTime(),
    )

    private fun welcome() = V2ChatMessage(
        role = V2AIMessageRole.ASSISTANT,
        text = "嗨，我是你的 AI 高尔夫教练。今天想聊挥杆、推杆、还是练球计划？",
    )
}
