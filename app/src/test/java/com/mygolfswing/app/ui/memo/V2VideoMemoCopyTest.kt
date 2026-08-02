package com.mygolfswing.app.ui.memo

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2VideoMemoCopyTest {

    @Test
    fun releaseVideoMemoErrorHidesCameraDiagnostics() {
        val message = memoVideoSaveError(
            base = "无法打开相机，请稍后重试或改从相册选择",
            debugDetail = "ActivityNotFoundException CaptureVideo file=/data/user/0/com.mygolfswing.app/cache/videos/private.mp4",
            isDebuggable = false,
        )

        assertEquals("无法打开相机，请稍后重试或改从相册选择", message)
        assertFalse(message.contains("ActivityNotFoundException"))
        assertFalse(message.contains("CaptureVideo"))
        assertFalse(message.contains("/data/"))
        assertFalse(message.contains("private.mp4"))
    }

    @Test
    fun releaseVideoMemoErrorHidesAlbumImportDiagnostics() {
        val message = memoVideoSaveError(
            base = "无法读取视频，请检查权限或换一个视频",
            debugDetail = "SecurityException importVideo content://media/external/video/media/42 token=/users/private",
            isDebuggable = false,
        )

        assertEquals("无法读取视频，请检查权限或换一个视频", message)
        assertFalse(message.contains("SecurityException"))
        assertFalse(message.contains("importVideo"))
        assertFalse(message.contains("content://"))
        assertFalse(message.contains("/users/"))
    }

    @Test
    fun releaseVideoMemoErrorHidesSaveDiagnostics() {
        val message = memoVideoSaveError(
            base = "保存失败，请稍后重试",
            debugDetail = "RoomSQLiteException create memo failed memoId=memo-private local=/data/user/0/com.mygolfswing.app/files/users/u1/video.mp4",
            isDebuggable = false,
        )

        assertEquals("保存失败，请稍后重试", message)
        assertFalse(message.contains("RoomSQLiteException"))
        assertFalse(message.contains("memo-private"))
        assertFalse(message.contains("/data/"))
        assertFalse(message.contains("video.mp4"))
    }

    @Test
    fun debugVideoMemoErrorKeepsDiagnostics() {
        val message = memoVideoSaveError(
            base = "保存失败，请稍后重试",
            debugDetail = "thumbnail generation failed",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("保存失败，请稍后重试"))
        assertTrue(message.contains("thumbnail generation failed"))
    }

    @Test
    fun videoMemoErrorIgnoresBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "保存失败，请稍后重试",
            memoVideoSaveError("保存失败，请稍后重试", " \n\t ", isDebuggable = true),
        )
    }

    @Test
    fun memoVideoThrowableErrorKeepsOrdinaryFailuresHandled() {
        assertEquals(
            "保存失败，请稍后重试",
            memoVideoThrowableError("保存失败，请稍后重试", IllegalStateException("Room write failed")),
        )
    }

    @Test
    fun memoVideoThrowableErrorPreservesCoroutineCancellation() {
        try {
            memoVideoThrowableError("保存失败，请稍后重试", CancellationException("video memo screen disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("video memo screen disposed", e.message)
        }
    }
}
