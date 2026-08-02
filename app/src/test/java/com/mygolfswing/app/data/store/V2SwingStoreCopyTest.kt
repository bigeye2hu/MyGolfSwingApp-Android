package com.mygolfswing.app.data.store

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2SwingStoreCopyTest {

    @Test
    fun releaseSwingStoreErrorHidesPersistenceDiagnostics() {
        val message = swingStoreErrorMessage(
            base = "挥杆状态保存失败，请稍后重试",
            debugDetail = "SQLiteException updateClipRetention clipId=4d2f-clip user=/users/private-user",
            isDebuggable = false,
        )

        assertEquals("挥杆状态保存失败，请稍后重试", message)
        assertFalse(message.contains("SQLiteException"))
        assertFalse(message.contains("updateClipRetention"))
        assertFalse(message.contains("4d2f-clip"))
        assertFalse(message.contains("/users/"))
    }

    @Test
    fun releaseSwingStoreErrorHidesLocalMediaDiagnostics() {
        val message = swingStoreErrorMessage(
            base = "删除挥杆失败，请稍后重试",
            debugDetail = "deleteClips failed /data/user/0/com.mygolfswing.app/files/users/u123/swing/private.mp4 thumb=/cache/swing/private.jpg",
            isDebuggable = false,
        )

        assertEquals("删除挥杆失败，请稍后重试", message)
        assertFalse(message.contains("deleteClips"))
        assertFalse(message.contains("/data/user/0"))
        assertFalse(message.contains("private.mp4"))
        assertFalse(message.contains("private.jpg"))
    }

    @Test
    fun debugSwingStoreErrorKeepsDiagnostics() {
        val message = swingStoreErrorMessage(
            base = "删除挥杆失败，请稍后重试",
            debugDetail = "Room transaction rolled back",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("删除挥杆失败，请稍后重试"))
        assertTrue(message.contains("Room transaction rolled back"))
    }

    @Test
    fun swingStoreErrorIgnoresBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "挥杆状态保存失败，请稍后重试",
            swingStoreErrorMessage("挥杆状态保存失败，请稍后重试", " \n\t ", isDebuggable = true),
        )
        assertEquals(
            "删除挥杆失败，请稍后重试",
            swingStoreErrorMessage("删除挥杆失败，请稍后重试", "", isDebuggable = false),
        )
    }

    @Test
    fun swingStoreThrowableErrorKeepsOrdinaryFailuresHandled() {
        assertEquals(
            "删除挥杆失败，请稍后重试",
            swingStoreThrowableError(
                "删除挥杆失败，请稍后重试",
                IllegalStateException("deleteClips failed clipId=secret local=/data/private.mp4"),
            ),
        )
    }

    @Test
    fun swingStoreThrowableErrorPreservesCoroutineCancellation() {
        try {
            swingStoreThrowableError("挥杆状态保存失败，请稍后重试", CancellationException("swing store disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("swing store disposed", e.message)
        }
    }
}
