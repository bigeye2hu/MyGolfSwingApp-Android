package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.db.AIChatDao
import com.mygolfswing.app.data.db.AIMessageEntity
import com.mygolfswing.app.data.db.AISessionEntity
import com.mygolfswing.app.data.model.V2AIMessageRole
import com.mygolfswing.app.data.model.V2AISession
import com.mygolfswing.app.data.model.V2ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class V2AIChatRepository(private val dao: AIChatDao) {

    suspend fun loadAllSessions(): List<V2AISession> = withContext(Dispatchers.IO) {
        dao.listSessions().map { row ->
            val messages = dao.listMessages(row.id).map { decodeMessage(it) }
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
        val updatedMs = session.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dao.upsertSession(AISessionEntity(session.id.toString(), session.title, updatedMs))
        session.messages.filter { !it.isStreaming }.forEach { msg ->
            dao.upsertMessage(
                AIMessageEntity(
                    id = msg.id.toString(),
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
        dao.deleteSession(sessionId.toString())
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
