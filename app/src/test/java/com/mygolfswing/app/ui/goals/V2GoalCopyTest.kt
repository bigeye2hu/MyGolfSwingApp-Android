package com.mygolfswing.app.ui.goals

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2GoalCopyTest {

    @Test
    fun releaseGoalSaveErrorHidesPersistenceDiagnostics() {
        val message = goalSaveError(
            base = "保存失败，请稍后重试",
            debugDetail = "RoomSQLiteException: insert goal failed uid=/users/private-user goalId=goal-private-001",
            isDebuggable = false,
        )

        assertEquals("保存失败，请稍后重试", message)
        assertFalse(message.contains("RoomSQLiteException"))
        assertFalse(message.contains("/users/"))
        assertFalse(message.contains("goal-private-001"))
    }

    @Test
    fun releaseGoalDeleteErrorHidesBackendAndLocalPathDiagnostics() {
        val message = goalSaveError(
            base = "删除失败，请稍后重试",
            debugDetail = "deleteGoal HTTP 500 local=/data/user/0/com.mygolfswing.app/databases/v2.db goalId=goal-secret",
            isDebuggable = false,
        )

        assertEquals("删除失败，请稍后重试", message)
        assertFalse(message.contains("deleteGoal"))
        assertFalse(message.contains("HTTP 500"))
        assertFalse(message.contains("/data/"))
        assertFalse(message.contains("goal-secret"))
    }

    @Test
    fun debugGoalErrorKeepsDiagnostics() {
        val message = goalSaveError(
            base = "保存失败，请稍后重试",
            debugDetail = "linked memo id missing",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("保存失败，请稍后重试"))
        assertTrue(message.contains("linked memo id missing"))
    }

    @Test
    fun goalErrorIgnoresBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "保存失败，请稍后重试",
            goalSaveError("保存失败，请稍后重试", " \n\t ", isDebuggable = true),
        )
    }

    @Test
    fun goalThrowableErrorKeepsOrdinaryFailuresHandled() {
        assertEquals(
            "删除失败，请稍后重试",
            goalThrowableError(
                "删除失败，请稍后重试",
                IllegalStateException("deleteGoal failed goalId=secret local=/data/private.db"),
            ),
        )
    }

    @Test
    fun goalThrowableErrorPreservesCoroutineCancellation() {
        try {
            goalThrowableError("保存失败，请稍后重试", CancellationException("goal editor disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("goal editor disposed", e.message)
        }
    }
}
