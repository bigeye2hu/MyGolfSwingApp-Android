package com.mygolfswing.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 4,
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
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE v2_memo ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                db.execSQL("DROP INDEX IF EXISTS index_v2_memo_updatedAt")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_v2_memo_ownerUserId_updatedAt ON v2_memo(ownerUserId, updatedAt)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE v2_goal ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_v2_goal_ownerUserId_updatedAt ON v2_goal(ownerUserId, updatedAt)")

                db.execSQL("ALTER TABLE v2_ai_session ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE v2_ai_message ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_v2_ai_session_ownerUserId_updatedAt ON v2_ai_session(ownerUserId, updatedAt)")
                db.execSQL("DROP INDEX IF EXISTS index_v2_ai_message_sessionId_createdAt")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_v2_ai_message_ownerUserId_sessionId_createdAt ON v2_ai_message(ownerUserId, sessionId, createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_v2_ai_message_sessionId ON v2_ai_message(sessionId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS v2_plaza_saved_new (
                        userId TEXT NOT NULL,
                        videoId TEXT NOT NULL,
                        savedAt INTEGER NOT NULL,
                        PRIMARY KEY(userId, videoId)
                    )
                    """.trimIndent(),
                )
                db.execSQL("INSERT OR IGNORE INTO v2_plaza_saved_new(userId, videoId, savedAt) SELECT '', videoId, savedAt FROM v2_plaza_saved")
                db.execSQL("DROP TABLE v2_plaza_saved")
                db.execSQL("ALTER TABLE v2_plaza_saved_new RENAME TO v2_plaza_saved")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_v2_plaza_saved_userId_savedAt ON v2_plaza_saved(userId, savedAt)")
            }
        }
    }
}
