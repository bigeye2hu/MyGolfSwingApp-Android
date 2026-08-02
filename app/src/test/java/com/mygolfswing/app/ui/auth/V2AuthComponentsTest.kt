package com.mygolfswing.app.ui.auth

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class V2AuthComponentsTest {

    @Test
    fun releaseAuthErrorHidesRawBackendAndExceptionDetail() {
        val message = authUserError(
            base = "登录失败，请检查账号信息后重试",
            debugDetail = "java.net.SocketTimeoutException: /data/user/0/com.mygolfswing.app token expired",
            isDebuggable = false,
        )

        assertEquals("登录失败，请检查账号信息后重试", message)
        assertFalse(message.contains("SocketTimeoutException"))
        assertFalse(message.contains("/data/"))
        assertFalse(message.contains("token expired"))
    }

    @Test
    fun releaseAuthErrorIgnoresBlankDebugDetail() {
        assertEquals(
            "验证码发送失败，请稍后重试",
            authUserError(
                base = "验证码发送失败，请稍后重试",
                debugDetail = "  \n\t  ",
                isDebuggable = false,
            ),
        )
    }

    @Test
    fun debugAuthErrorKeepsDiagnosticDetail() {
        val message = authUserError(
            base = "重置失败，请检查验证码和新密码后重试",
            debugDetail = "SMS provider rejected code",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("重置失败，请检查验证码和新密码后重试"))
        assertTrue(message.contains("SMS provider rejected code"))
    }

    @Test
    fun debugAuthErrorStillDropsBlankDebugDetail() {
        assertEquals(
            "注册失败，请检查账号信息后重试",
            authUserError(
                base = "注册失败，请检查账号信息后重试",
                debugDetail = "",
                isDebuggable = true,
            ),
        )
    }

    @Test
    fun authThrowableErrorKeepsOrdinaryFailuresHandled() {
        val message = authThrowableError(
            base = "操作失败，请检查输入后重试",
            error = IllegalStateException("backend rejected password"),
        )

        assertEquals("操作失败，请检查输入后重试", message)
    }

    @Test
    fun authThrowableErrorPreservesCoroutineCancellation() {
        try {
            authThrowableError(
                base = "操作失败，请检查输入后重试",
                error = CancellationException("change password disposed"),
            )
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("change password disposed", e.message)
        }
    }

    @Test
    fun authScreensUseThrowableHelperForThrownRepositoryFailures() {
        listOf(
            "V2LoginScreen.kt",
            "V2RegisterScreen.kt",
            "V2ResetPasswordScreen.kt",
            "V2ChangePasswordScreen.kt",
        ).forEach { fileName ->
            val source = repoAuthFile(fileName).readText()
            assertTrue(
                "$fileName should route thrown auth failures through authThrowableError",
                source.contains("authThrowableError("),
            )
            assertFalse(
                "$fileName should not map caught Throwable directly through e.message",
                source.contains("authUserError(\"") && source.contains("catch (e: Throwable)") && source.contains("e.message)"),
            )
        }
    }

    private fun repoAuthFile(fileName: String): File =
        listOf(
            File("app/src/main/java/com/mygolfswing/app/ui/auth/$fileName"),
            File("../app/src/main/java/com/mygolfswing/app/ui/auth/$fileName"),
        ).first { it.exists() }
}
