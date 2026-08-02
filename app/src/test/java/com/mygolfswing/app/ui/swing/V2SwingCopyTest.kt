package com.mygolfswing.app.ui.swing

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2SwingCopyTest {

    @Test
    fun releaseSwingCameraErrorHidesCameraXDiagnostics() {
        val message = swingCameraError(
            base = "无法绑定相机，请检查相机权限或稍后重试",
            debugDetail = "CameraX BindException lensFacing=BACK device=private-camera-id",
            isDebuggable = false,
        )

        assertEquals("无法绑定相机，请检查相机权限或稍后重试", message)
        assertFalse(message.contains("CameraX"))
        assertFalse(message.contains("BindException"))
        assertFalse(message.contains("private-camera-id"))
    }

    @Test
    fun releaseSwingSaveErrorHidesFileAndClipDiagnostics() {
        val message = swingCameraError(
            base = "录制保存失败，请稍后重试",
            debugDetail = "adoptLocalFile failed clipId=clip-secret local=/data/user/0/com.mygolfswing.app/cache/swing/private.mp4",
            isDebuggable = false,
        )

        assertEquals("录制保存失败，请稍后重试", message)
        assertFalse(message.contains("adoptLocalFile"))
        assertFalse(message.contains("clip-secret"))
        assertFalse(message.contains("/data/"))
        assertFalse(message.contains("private.mp4"))
    }

    @Test
    fun debugSwingCameraErrorKeepsDiagnostics() {
        val message = swingCameraError(
            base = "录制保存失败，请稍后重试",
            debugDetail = "thumbnail adoption failed",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("录制保存失败，请稍后重试"))
        assertTrue(message.contains("thumbnail adoption failed"))
    }

    @Test
    fun swingCameraErrorIgnoresBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "录制保存失败，请稍后重试",
            swingCameraError("录制保存失败，请稍后重试", " \n\t ", isDebuggable = true),
        )
    }

    @Test
    fun swingCameraThrowableErrorKeepsOrdinaryFailuresHandled() {
        assertEquals(
            "录制保存失败，请稍后重试",
            swingCameraThrowableError(
                "录制保存失败，请稍后重试",
                IllegalStateException("adoptLocalFile failed clipId=private local=/data/private.mp4"),
            ),
        )
    }

    @Test
    fun swingCameraThrowableErrorPreservesCoroutineCancellation() {
        try {
            swingCameraThrowableError("无法绑定相机，请检查相机权限或稍后重试", CancellationException("swing camera disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("swing camera disposed", e.message)
        }
    }
}
