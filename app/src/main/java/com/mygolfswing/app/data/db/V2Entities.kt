package com.mygolfswing.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room 表设计（对齐 iOS 端：本地落盘 + 云同步缓存）。
 *
 * 设计原则：
 * - **Goal / Memo 把完整对象序列化为 JSON 存进单字段**（`goalJson` / `memoJson`），
 *   方便 V2Goal/V2Memo 在不破坏 Room schema 的前提下持续演进；
 *   云同步直接复用同一份 JSON 上传。
 * - 同时把 `updatedAt / status` 等关键字段冗余存出来，方便建索引、做"未同步"过滤。
 * - 所有时间统一用 epoch ms（Long）。
 */

// ============ Goal ============

@Entity(tableName = "v2_goal")
data class GoalEntity(
    @PrimaryKey val id: String,
    val goalJson: String,
    val status: String,
    val priority: String,
    val pinState: String,
    val todayFocus: Boolean,
    val updatedAt: Long,
    val startedAt: Long,
    val dirty: Boolean = true,
)

// ============ Memo ============

@Entity(tableName = "v2_memo", indices = [Index(value = ["updatedAt"])])
data class MemoEntity(
    @PrimaryKey val id: String,
    val memoJson: String,
    val kind: String,
    val topic: String,
    val pinState: String,
    val updatedAt: Long,
    val createdAt: Long,
    val dirty: Boolean = true,
)

// ============ AI Chat ============

@Entity(tableName = "v2_ai_session")
data class AISessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val updatedAt: Long,
)

@Entity(
    tableName = "v2_ai_message",
    foreignKeys = [
        ForeignKey(
            entity = AISessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionId", "createdAt"])],
)
data class AIMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val text: String,
    val isStreaming: Boolean,
    val createdAt: Long,
)

// ============ Plaza 收藏 ============

@Entity(tableName = "v2_plaza_saved")
data class PlazaSavedEntity(
    @PrimaryKey val videoId: String,
    val savedAt: Long,
)

@Entity(tableName = "v2_swing_session", indices = [Index(value = ["userId", "dayEpoch"])])
data class SwingSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val dayEpoch: Long,
    val sessionJson: String,
    val updatedAt: Long,
)

@Entity(tableName = "v2_golf_profile")
data class GolfProfileEntity(
    @PrimaryKey val userId: String,
    val profileJson: String,
    val updatedAt: Long,
)
