package com.mygolfswing.app.ui.profile

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2ProfileEditCopyTest {

    @Test
    fun releaseProfileEditErrorHidesAvatarUploadDiagnostics() {
        val message = profileEditError(
            base = "头像上传失败，请换一张图片或稍后重试",
            debugDetail = "java.io.IOException: uploadAvatar failed token=/users/private/avatar.png",
            isDebuggable = false,
        )

        assertEquals("头像上传失败，请换一张图片或稍后重试", message)
        assertFalse(message.contains("IOException"))
        assertFalse(message.contains("uploadAvatar"))
        assertFalse(message.contains("/users/"))
    }

    @Test
    fun releaseProfileEditErrorHidesPickerAndCameraDiagnostics() {
        val galleryMessage = profileEditError(
            base = "无法打开相册，请稍后重试或改用拍照",
            debugDetail = "ActivityNotFoundException: PickVisualMedia unavailable",
            isDebuggable = false,
        )
        val cameraMessage = profileEditError(
            base = "无法打开相机，请检查权限或改从相册选择",
            debugDetail = "FileProvider failed for /data/user/0/com.mygolfswing.app/cache/avatars/private.jpg",
            isDebuggable = false,
        )

        assertEquals("无法打开相册，请稍后重试或改用拍照", galleryMessage)
        assertEquals("无法打开相机，请检查权限或改从相册选择", cameraMessage)
        assertFalse(galleryMessage.contains("ActivityNotFoundException"))
        assertFalse(cameraMessage.contains("FileProvider"))
        assertFalse(cameraMessage.contains("/data/"))
    }

    @Test
    fun releaseProfileEditErrorHidesSaveDiagnostics() {
        val message = profileEditError(
            base = "保存失败，请稍后重试",
            debugDetail = "HTTP 500 updateUserInfo failed for uid=private-user",
            isDebuggable = false,
        )

        assertEquals("保存失败，请稍后重试", message)
        assertFalse(message.contains("HTTP 500"))
        assertFalse(message.contains("private-user"))
    }

    @Test
    fun debugProfileEditErrorKeepsDiagnostics() {
        val message = profileEditError(
            base = "保存失败，请稍后重试",
            debugDetail = "nickname validation rejected",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("保存失败，请稍后重试"))
        assertTrue(message.contains("nickname validation rejected"))
    }

    @Test
    fun profileEditErrorIgnoresBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "保存失败，请稍后重试",
            profileEditError("保存失败，请稍后重试", "  \n\t  ", isDebuggable = true),
        )
    }

    @Test
    fun profileEditThrowableErrorKeepsOrdinaryFailuresHandled() {
        assertEquals(
            "头像上传失败，请换一张图片或稍后重试",
            profileEditThrowableError("头像上传失败，请换一张图片或稍后重试", IllegalStateException("avatar /data/private.jpg")),
        )
    }

    @Test
    fun profileEditThrowableErrorPreservesCoroutineCancellation() {
        try {
            profileEditThrowableError("头像上传失败，请换一张图片或稍后重试", CancellationException("profile edit disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("profile edit disposed", e.message)
        }
    }
}
