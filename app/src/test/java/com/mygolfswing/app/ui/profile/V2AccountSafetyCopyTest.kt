package com.mygolfswing.app.ui.profile

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2AccountSafetyCopyTest {

    @Test
    fun releaseAccountSecurityErrorHidesRawDiagnostics() {
        val message = accountSecurityError(
            base = "绑定失败，请检查验证码后重试",
            debugDetail = "java.lang.IllegalStateException: phone token leaked /data/user/0/com.mygolfswing.app",
            isDebuggable = false,
        )

        assertEquals("绑定失败，请检查验证码后重试", message)
        assertFalse(message.contains("IllegalStateException"))
        assertFalse(message.contains("phone token"))
        assertFalse(message.contains("/data/"))
    }

    @Test
    fun debugAccountSecurityErrorKeepsDiagnostics() {
        val message = accountSecurityError(
            base = "验证码发送失败，请稍后重试",
            debugDetail = "sms gateway returned 429",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("验证码发送失败，请稍后重试"))
        assertTrue(message.contains("sms gateway returned 429"))
    }

    @Test
    fun releaseAccountDeletionErrorHidesRawDiagnostics() {
        val message = accountDeletionError(
            base = "删除失败，请稍后重试；如多次失败，请联系客服协助处理",
            debugDetail = "HTTP 500 deleteUser failed for uid=/users/private-user-id",
            isDebuggable = false,
        )

        assertEquals("删除失败，请稍后重试；如多次失败，请联系客服协助处理", message)
        assertFalse(message.contains("HTTP 500"))
        assertFalse(message.contains("private-user-id"))
        assertFalse(message.contains("/users/"))
    }

    @Test
    fun debugAccountDeletionErrorKeepsDiagnostics() {
        val message = accountDeletionError(
            base = "删除失败，请稍后重试；如多次失败，请联系客服协助处理",
            debugDetail = "cloud cleanup timeout",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("删除失败，请稍后重试；如多次失败，请联系客服协助处理"))
        assertTrue(message.contains("cloud cleanup timeout"))
    }

    @Test
    fun accountSafetyErrorsIgnoreBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "更新失败，请稍后重试",
            accountSecurityError("更新失败，请稍后重试", " \n\t ", isDebuggable = true),
        )
        assertEquals(
            "删除失败，请稍后重试；如多次失败，请联系客服协助处理",
            accountDeletionError(
                "删除失败，请稍后重试；如多次失败，请联系客服协助处理",
                "",
                isDebuggable = true,
            ),
        )
    }

    @Test
    fun accountSecurityThrowableErrorKeepsOrdinaryFailuresHandled() {
        assertEquals(
            "绑定失败，请检查验证码后重试",
            accountSecurityThrowableError(
                "绑定失败，请检查验证码后重试",
                IllegalStateException("bind phone failed token=/users/private"),
            ),
        )
    }

    @Test
    fun accountSecurityThrowableErrorPreservesCoroutineCancellation() {
        try {
            accountSecurityThrowableError("更新失败，请稍后重试", CancellationException("account dialog disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("account dialog disposed", e.message)
        }
    }

    @Test
    fun accountDeletionThrowableErrorKeepsOrdinaryFailuresHandled() {
        assertEquals(
            "删除失败，请稍后重试；如多次失败，请联系客服协助处理",
            accountDeletionThrowableError(
                "删除失败，请稍后重试；如多次失败，请联系客服协助处理",
                IllegalStateException("delete account failed uid=/users/private"),
            ),
        )
    }

    @Test
    fun accountDeletionThrowableErrorPreservesCoroutineCancellation() {
        try {
            accountDeletionThrowableError(
                "删除失败，请稍后重试；如多次失败，请联系客服协助处理",
                CancellationException("account deletion disposed"),
            )
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("account deletion disposed", e.message)
        }
    }
}
