package com.mygolfswing.app.ui.golfcam

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GolfCamExportCopyTest {

    @Test
    fun releaseExportErrorHidesCodecAndTempFileDiagnostics() {
        val message = exportUserError(
            base = "当前设备暂时无法完成视频编码，请降低清晰度后重试",
            debugDetail = "MediaCodec.CodecException encoder failed for /data/user/0/com.mygolfswing.app/cache/export-preview.mp4",
            isDebuggable = false,
        )

        assertEquals("当前设备暂时无法完成视频编码，请降低清晰度后重试", message)
        assertFalse(message.contains("MediaCodec"))
        assertFalse(message.contains("CodecException"))
        assertFalse(message.contains("/data/"))
        assertFalse(message.contains("export-preview.mp4"))
    }

    @Test
    fun releaseExportSaveErrorHidesMediaStoreDiagnostics() {
        val message = exportUserError(
            base = "系统相册暂时不可写，请检查存储空间或权限后重试",
            debugDetail = "MediaStore insert denied uri=content://media/external/video/media token=/users/private",
            isDebuggable = false,
        )

        assertEquals("系统相册暂时不可写，请检查存储空间或权限后重试", message)
        assertFalse(message.contains("MediaStore"))
        assertFalse(message.contains("content://"))
        assertFalse(message.contains("/users/"))
    }

    @Test
    fun debugExportErrorKeepsDiagnostics() {
        val message = exportUserError(
            base = "视频导出失败，请稍后重试",
            debugDetail = "muxer track missing",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("视频导出失败，请稍后重试"))
        assertTrue(message.contains("muxer track missing"))
    }

    @Test
    fun exportErrorIgnoresBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "视频导出失败，请稍后重试",
            exportUserError("视频导出失败，请稍后重试", " \n\t ", isDebuggable = true),
        )
    }

    @Test
    fun exportFailureMessagePreservesCoroutineCancellation() {
        try {
            exportFailureMessage(CancellationException("golfcam export disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("golfcam export disposed", e.message)
        }
    }

    @Test
    fun exportSaveFailureMessagePreservesCoroutineCancellation() {
        try {
            exportSaveFailureMessage(CancellationException("golfcam gallery save disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("golfcam gallery save disposed", e.message)
        }
    }
}
