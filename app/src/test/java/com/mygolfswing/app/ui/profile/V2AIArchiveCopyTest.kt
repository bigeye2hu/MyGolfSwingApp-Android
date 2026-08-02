package com.mygolfswing.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2AIArchiveCopyTest {

    @Test
    fun releaseAIArchiveSyncErrorHidesRawDiagnostics() {
        val message = aiArchiveSyncError(
            base = "同步失败，请稍后重试",
            debugDetail = "HTTP 500 /ai/profile-reflect session=private-session uid=/users/u123 apiKey=sk-secret",
            isDebuggable = false,
        )

        assertEquals("同步失败，请稍后重试", message)
        assertFalse(message.contains("HTTP 500"))
        assertFalse(message.contains("/ai/profile-reflect"))
        assertFalse(message.contains("private-session"))
        assertFalse(message.contains("sk-secret"))
    }

    @Test
    fun releaseAIArchiveSyncErrorHidesLocalPathDiagnostics() {
        val message = aiArchiveSyncError(
            base = "同步失败，请稍后重试",
            debugDetail = "SQLiteException reading /data/user/0/com.mygolfswing.app/databases/ai_profile.db",
            isDebuggable = false,
        )

        assertEquals("同步失败，请稍后重试", message)
        assertFalse(message.contains("SQLiteException"))
        assertFalse(message.contains("/data/user/0"))
        assertFalse(message.contains("ai_profile.db"))
    }

    @Test
    fun debugAIArchiveSyncErrorKeepsDiagnostics() {
        val message = aiArchiveSyncError(
            base = "同步失败，请稍后重试",
            debugDetail = "profile reflector parse failed",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("同步失败，请稍后重试"))
        assertTrue(message.contains("profile reflector parse failed"))
    }

    @Test
    fun aiArchiveSyncErrorIgnoresBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "同步失败，请稍后重试",
            aiArchiveSyncError("同步失败，请稍后重试", " \n\t ", isDebuggable = true),
        )
        assertEquals(
            "同步失败，请稍后重试",
            aiArchiveSyncError("同步失败，请稍后重试", "", isDebuggable = false),
        )
    }
}
