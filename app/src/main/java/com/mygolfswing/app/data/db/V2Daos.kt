package com.mygolfswing.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM v2_goal WHERE ownerUserId = :ownerUserId ORDER BY updatedAt DESC")
    fun observeAll(ownerUserId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM v2_goal WHERE ownerUserId = :ownerUserId")
    suspend fun all(ownerUserId: String): List<GoalEntity>

    @Query("SELECT * FROM v2_goal WHERE id = :id AND ownerUserId = :ownerUserId")
    suspend fun get(id: String, ownerUserId: String): GoalEntity?

    @Query("SELECT * FROM v2_goal WHERE dirty = 1 AND ownerUserId = :ownerUserId")
    suspend fun dirty(ownerUserId: String): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<GoalEntity>)

    @Query("DELETE FROM v2_goal WHERE id = :id AND ownerUserId = :ownerUserId")
    suspend fun delete(id: String, ownerUserId: String)

    @Query("UPDATE v2_goal SET dirty = 0 WHERE id = :id AND ownerUserId = :ownerUserId")
    suspend fun markClean(id: String, ownerUserId: String)

    @Query("DELETE FROM v2_goal WHERE ownerUserId = :ownerUserId")
    suspend fun clear(ownerUserId: String)
}

@Dao
interface MemoDao {
    @Query("SELECT * FROM v2_memo ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM v2_memo WHERE ownerUserId = :ownerUserId ORDER BY updatedAt DESC")
    fun observeAll(ownerUserId: String): Flow<List<MemoEntity>>

    @Query("SELECT * FROM v2_memo WHERE id = :id")
    suspend fun get(id: String): MemoEntity?

    @Query("SELECT * FROM v2_memo WHERE id = :id AND ownerUserId = :ownerUserId")
    suspend fun get(id: String, ownerUserId: String): MemoEntity?

    @Query("SELECT * FROM v2_memo WHERE dirty = 1")
    suspend fun dirty(): List<MemoEntity>

    @Query("SELECT * FROM v2_memo WHERE dirty = 1 AND ownerUserId = :ownerUserId")
    suspend fun dirty(ownerUserId: String): List<MemoEntity>

    @Query("SELECT * FROM v2_memo ORDER BY updatedAt DESC")
    suspend fun getAll(): List<MemoEntity>

    @Query("SELECT * FROM v2_memo WHERE ownerUserId = :ownerUserId ORDER BY updatedAt DESC")
    suspend fun getAll(ownerUserId: String): List<MemoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MemoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<MemoEntity>)

    @Query("DELETE FROM v2_memo WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM v2_memo WHERE id = :id AND ownerUserId = :ownerUserId")
    suspend fun delete(id: String, ownerUserId: String)

    @Query("UPDATE v2_memo SET dirty = 0 WHERE id = :id AND ownerUserId = :ownerUserId")
    suspend fun markClean(id: String, ownerUserId: String)

    @Query("DELETE FROM v2_memo")
    suspend fun clear()
}

@Dao
interface AIChatDao {
    @Query("SELECT * FROM v2_ai_session WHERE ownerUserId = :ownerUserId ORDER BY updatedAt DESC")
    fun observeSessions(ownerUserId: String): Flow<List<AISessionEntity>>

    @Query("SELECT * FROM v2_ai_session WHERE ownerUserId = :ownerUserId ORDER BY updatedAt DESC")
    suspend fun listSessions(ownerUserId: String): List<AISessionEntity>

    @Query("SELECT * FROM v2_ai_message WHERE sessionId = :sessionId AND ownerUserId = :ownerUserId ORDER BY createdAt ASC")
    fun observeMessages(sessionId: String, ownerUserId: String): Flow<List<AIMessageEntity>>

    @Query("SELECT * FROM v2_ai_message WHERE sessionId = :sessionId AND ownerUserId = :ownerUserId ORDER BY createdAt ASC")
    suspend fun listMessages(sessionId: String, ownerUserId: String): List<AIMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: AISessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: AIMessageEntity)

    @Query("UPDATE v2_ai_message SET text = :text, isStreaming = :streaming WHERE id = :id AND ownerUserId = :ownerUserId")
    suspend fun updateMessage(id: String, ownerUserId: String, text: String, streaming: Boolean)

    @Query("DELETE FROM v2_ai_session WHERE id = :id AND ownerUserId = :ownerUserId")
    suspend fun deleteSession(id: String, ownerUserId: String)

    @Transaction
    @Query("DELETE FROM v2_ai_session WHERE ownerUserId = :ownerUserId")
    suspend fun clearAll(ownerUserId: String)
}

@Dao
interface PlazaSavedDao {
    @Query("SELECT * FROM v2_plaza_saved WHERE userId = :userId ORDER BY savedAt DESC")
    fun observe(userId: String): Flow<List<PlazaSavedEntity>>

    @Query("SELECT videoId FROM v2_plaza_saved WHERE userId = :userId")
    suspend fun listIds(userId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(saved: PlazaSavedEntity)

    @Query("DELETE FROM v2_plaza_saved WHERE videoId = :videoId AND userId = :userId")
    suspend fun remove(videoId: String, userId: String)

    @Query("DELETE FROM v2_plaza_saved WHERE userId = :userId")
    suspend fun clear(userId: String)
}

@Dao
interface SwingSessionDao {
    @Query("SELECT * FROM v2_swing_session WHERE userId = :userId ORDER BY dayEpoch DESC")
    fun observeAll(userId: String): Flow<List<SwingSessionEntity>>

    @Query("SELECT * FROM v2_swing_session WHERE userId = :userId AND dayEpoch = :dayEpoch LIMIT 1")
    suspend fun getByDay(userId: String, dayEpoch: Long): SwingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SwingSessionEntity)
}

@Dao
interface GolfProfileDao {
    @Query("SELECT * FROM v2_golf_profile WHERE userId = :userId LIMIT 1")
    suspend fun get(userId: String): GolfProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GolfProfileEntity)
}
