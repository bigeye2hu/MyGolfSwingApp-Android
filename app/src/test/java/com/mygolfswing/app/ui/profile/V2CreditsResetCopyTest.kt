package com.mygolfswing.app.ui.profile

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2CreditsResetCopyTest {

    @Test
    fun releaseCreditsResetErrorHidesRawBalanceDiagnostics() {
        val message = creditsResetError(
            base = "服务端余额同步失败，请稍后重试",
            debugDetail = "HTTP 500 /credits/balance uid=/users/private-user-id token=secret-token",
            isDebuggable = false,
        )

        assertEquals("服务端余额同步失败，请稍后重试", message)
        assertFalse(message.contains("HTTP 500"))
        assertFalse(message.contains("/credits/balance"))
        assertFalse(message.contains("private-user-id"))
        assertFalse(message.contains("secret-token"))
    }

    @Test
    fun releaseCreditsResetErrorHidesRawCacheDiagnostics() {
        val message = creditsResetError(
            base = "服务端余额同步失败，请稍后重试",
            debugDetail = "java.io.IOException: failed to clear /data/user/0/com.mygolfswing.app/files/users/u123/credits.json",
            isDebuggable = false,
        )

        assertEquals("服务端余额同步失败，请稍后重试", message)
        assertFalse(message.contains("IOException"))
        assertFalse(message.contains("/data/user/0"))
        assertFalse(message.contains("credits.json"))
    }

    @Test
    fun debugCreditsResetErrorKeepsDiagnostics() {
        val message = creditsResetError(
            base = "服务端余额同步失败，请稍后重试",
            debugDetail = "balance refresh timed out after profile refresh",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("服务端余额同步失败，请稍后重试"))
        assertTrue(message.contains("balance refresh timed out after profile refresh"))
    }

    @Test
    fun creditsResetErrorIgnoresBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "服务端余额同步失败，请稍后重试",
            creditsResetError("服务端余额同步失败，请稍后重试", " \n\t ", isDebuggable = true),
        )
        assertEquals(
            "服务端余额同步失败，请稍后重试",
            creditsResetError("服务端余额同步失败，请稍后重试", "", isDebuggable = false),
        )
    }

    @Test
    fun creditsResetThrowableErrorKeepsOrdinaryFailuresHandled() {
        assertEquals(
            "服务端余额同步失败，请稍后重试",
            creditsResetThrowableError(
                "服务端余额同步失败，请稍后重试",
                IllegalStateException("balance refresh failed token=secret local=/data/private"),
            ),
        )
    }

    @Test
    fun creditsResetThrowableErrorPreservesCoroutineCancellation() {
        try {
            creditsResetThrowableError("服务端余额同步失败，请稍后重试", CancellationException("credits reset disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("credits reset disposed", e.message)
        }
    }
}
