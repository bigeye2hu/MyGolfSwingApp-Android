package com.mygolfswing.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        GoalEntity::class,
        MemoEntity::class,
        AISessionEntity::class,
        AIMessageEntity::class,
        PlazaSavedEntity::class,
        SwingSessionEntity::class,
        GolfProfileEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class V2Database : RoomDatabase() {
    abstract fun goals(): GoalDao
    abstract fun memos(): MemoDao
    abstract fun aiChat(): AIChatDao
    abstract fun plazaSaved(): PlazaSavedDao
    abstract fun swingSessions(): SwingSessionDao
    abstract fun golfProfile(): GolfProfileDao

    companion object {
        @Volatile private var instance: V2Database? = null

        fun get(context: Context): V2Database = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                V2Database::class.java,
                "v2-mygolfswing.db",
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
