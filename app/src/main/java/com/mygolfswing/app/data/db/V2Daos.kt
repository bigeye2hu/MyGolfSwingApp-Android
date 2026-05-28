package com.mygolfswing.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM v2_goal ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM v2_goal WHERE id = :id")
    suspend fun get(id: String): GoalEntity?

    @Query("SELECT * FROM v2_goal WHERE dirty = 1")
    suspend fun dirty(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<GoalEntity>)

    @Query("DELETE FROM v2_goal WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE v2_goal SET dirty = 0 WHERE id = :id")
    suspend fun markClean(id: String)

    @Query("DELETE FROM v2_goal")
    suspend fun clear()
}

@Dao
interface MemoDao {
    @Query("SELECT * FROM v2_memo ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM v2_memo WHERE id = :id")
    suspend fun get(id: String): MemoEntity?

    @Query("SELECT * FROM v2_memo WHERE dirty = 1")
    suspend fun dirty(): List<MemoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MemoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<MemoEntity>)

    @Query("DELETE FROM v2_memo WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE v2_memo SET dirty = 0 WHERE id = :id")
    suspend fun markClean(id: String)

    @Query("DELETE FROM v2_memo")
    suspend fun clear()
}

@Dao
interface AIChatDao {
    @Query("SELECT * FROM v2_ai_session ORDER BY updatedAt DESC")
    fun observeSessions(): Flow<List<AISessionEntity>>

    @Query("SELECT * FROM v2_ai_session ORDER BY updatedAt DESC")
    suspend fun listSessions(): List<AISessionEntity>

    @Query("SELECT * FROM v2_ai_message WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun observeMessages(sessionId: String): Flow<List<AIMessageEntity>>

    @Query("SELECT * FROM v2_ai_message WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun listMessages(sessionId: String): List<AIMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: AISessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: AIMessageEntity)

    @Query("UPDATE v2_ai_message SET text = :text, isStreaming = :streaming WHERE id = :id")
    suspend fun updateMessage(id: String, text: String, streaming: Boolean)

    @Query("DELETE FROM v2_ai_session WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Transaction
    @Query("DELETE FROM v2_ai_session")
    suspend fun clearAll()
}

@Dao
interface PlazaSavedDao {
    @Query("SELECT * FROM v2_plaza_saved ORDER BY savedAt DESC")
    fun observe(): Flow<List<PlazaSavedEntity>>

    @Query("SELECT videoId FROM v2_plaza_saved")
    suspend fun listIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(saved: PlazaSavedEntity)

    @Query("DELETE FROM v2_plaza_saved WHERE videoId = :videoId")
    suspend fun remove(videoId: String)

    @Query("DELETE FROM v2_plaza_saved")
    suspend fun clear()
}

@Dao
interface SwingSessionDao {
    @Query("SELECT * FROM v2_swing_session ORDER BY dayEpoch DESC")
    fun observeAll(): Flow<List<SwingSessionEntity>>

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
