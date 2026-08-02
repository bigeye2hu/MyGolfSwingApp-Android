package com.mygolfswing.app.ui.profile

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2ProfileSettingsCopyTest {

    @Test
    fun releaseQrGalleryErrorHidesSystemDiagnostics() {
        val message = profileQrGalleryError(
            base = "保存到相册失败，请稍后重试",
            debugDetail = "SecurityException insert MediaStore.Images failed uri=content://media/external/images/media token=private",
            isDebuggable = false,
        )

        assertEquals("保存到相册失败，请稍后重试", message)
        assertFalse(message.contains("SecurityException"))
        assertFalse(message.contains("MediaStore"))
        assertFalse(message.contains("content://"))
        assertFalse(message.contains("private"))
    }

    @Test
    fun releaseQrGalleryErrorHidesLocalPathDiagnostics() {
        val message = profileQrGalleryError(
            base = "保存到相册失败，请稍后重试",
            debugDetail = "IOException writing /data/user/0/com.mygolfswing.app/cache/qrcode.png",
            isDebuggable = false,
        )

        assertEquals("保存到相册失败，请稍后重试", message)
        assertFalse(message.contains("IOException"))
        assertFalse(message.contains("/data/user/0"))
        assertFalse(message.contains("qrcode.png"))
    }

    @Test
    fun debugQrGalleryErrorKeepsDiagnostics() {
        val message = profileQrGalleryError(
            base = "保存到相册失败，请稍后重试",
            debugDetail = "MediaStore insert returned permission denied",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("保存到相册失败，请稍后重试"))
        assertTrue(message.contains("MediaStore insert returned permission denied"))
    }

    @Test
    fun qrGalleryErrorIgnoresBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "保存到相册失败，请稍后重试",
            profileQrGalleryError("保存到相册失败，请稍后重试", " \n\t ", isDebuggable = true),
        )
        assertEquals(
            "保存到相册失败，请稍后重试",
            profileQrGalleryError("保存到相册失败，请稍后重试", "", isDebuggable = false),
        )
    }

    @Test
    fun qrGalleryThrowableErrorKeepsOrdinaryFailuresHandled() {
        assertEquals(
            "保存到相册失败，请稍后重试",
            profileQrGalleryThrowableError(
                "保存到相册失败，请稍后重试",
                IllegalStateException("MediaStore failed uri=content://private token=secret"),
            ),
        )
    }

    @Test
    fun qrGalleryThrowableErrorPreservesCoroutineCancellation() {
        try {
            profileQrGalleryThrowableError("保存到相册失败，请稍后重试", CancellationException("qr dialog disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("qr dialog disposed", e.message)
        }
    }
}
