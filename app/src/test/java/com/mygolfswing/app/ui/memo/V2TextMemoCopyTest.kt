package com.mygolfswing.app.ui.memo

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2TextMemoCopyTest {

    @Test
    fun releaseTextMemoCreateErrorHidesPersistenceDiagnostics() {
        val message = memoTextUserError(
            base = "保存失败，请稍后重试",
            debugDetail = "RoomSQLiteException create text memo failed memoId=memo-private uid=/users/private-user",
            isDebuggable = false,
        )

        assertEquals("保存失败，请稍后重试", message)
        assertFalse(message.contains("RoomSQLiteException"))
        assertFalse(message.contains("memo-private"))
        assertFalse(message.contains("/users/"))
    }

    @Test
    fun releaseTextMemoDetailErrorHidesBackendAndLocalPathDiagnostics() {
        val message = memoTextUserError(
            base = "保存失败，请稍后重试",
            debugDetail = "updateMemo HTTP 500 local=/data/user/0/com.mygolfswing.app/databases/v2.db memoId=memo-secret",
            isDebuggable = false,
        )

        assertEquals("保存失败，请稍后重试", message)
        assertFalse(message.contains("updateMemo"))
        assertFalse(message.contains("HTTP 500"))
        assertFalse(message.contains("/data/"))
        assertFalse(message.contains("memo-secret"))
    }

    @Test
    fun releaseTextMemoDeleteErrorHidesDiagnostics() {
        val message = memoTextUserError(
            base = "删除失败，请稍后重试",
            debugDetail = "deleteMemo failed memoId=memo-secret tagIds=[private-tag] uid=/users/private-user",
            isDebuggable = false,
        )

        assertEquals("删除失败，请稍后重试", message)
        assertFalse(message.contains("deleteMemo"))
        assertFalse(message.contains("memo-secret"))
        assertFalse(message.contains("private-tag"))
        assertFalse(message.contains("/users/"))
    }

    @Test
    fun debugTextMemoErrorKeepsDiagnostics() {
        val message = memoTextUserError(
            base = "保存失败，请稍后重试",
            debugDetail = "linked goal update failed",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("保存失败，请稍后重试"))
        assertTrue(message.contains("linked goal update failed"))
    }

    @Test
    fun textMemoErrorIgnoresBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "保存失败，请稍后重试",
            memoTextUserError("保存失败，请稍后重试", " \n\t ", isDebuggable = true),
        )
    }

    @Test
    fun memoTextThrowableErrorKeepsOrdinaryFailuresHandled() {
        assertEquals(
            "保存失败，请稍后重试",
            memoTextThrowableError("保存失败，请稍后重试", IllegalStateException("Room write failed")),
        )
    }

    @Test
    fun memoTextThrowableErrorPreservesCoroutineCancellation() {
        try {
            memoTextThrowableError("保存失败，请稍后重试", CancellationException("text memo screen disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("text memo screen disposed", e.message)
        }
    }
}
