package com.mygolfswing.app.ui.settings

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2AIServiceConfigCopyTest {

    @Test
    fun releaseAIConfigErrorHidesProviderAndCredentialDiagnostics() {
        val message = aiConfigError(
            base = "保存失败，请稍后重试",
            debugDetail = "OpenAI provider rejected apiKey=sk-private-key baseUrl=https://proxy.internal/v1 model=gpt-5",
            isDebuggable = false,
        )

        assertEquals("保存失败，请稍后重试", message)
        assertFalse(message.contains("OpenAI"))
        assertFalse(message.contains("sk-private-key"))
        assertFalse(message.contains("proxy.internal"))
        assertFalse(message.contains("gpt-5"))
    }

    @Test
    fun releaseAIConfigErrorHidesBackendAndPathDiagnostics() {
        val message = aiConfigError(
            base = "保存失败，请稍后重试",
            debugDetail = "java.io.IOException: write /data/user/0/com.mygolfswing.app/shared_prefs/ai_config.xml failed",
            isDebuggable = false,
        )

        assertEquals("保存失败，请稍后重试", message)
        assertFalse(message.contains("IOException"))
        assertFalse(message.contains("/data/"))
        assertFalse(message.contains("ai_config.xml"))
    }

    @Test
    fun debugAIConfigErrorKeepsDiagnostics() {
        val message = aiConfigError(
            base = "保存失败，请稍后重试",
            debugDetail = "provider verification returned HTTP 401",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("保存失败，请稍后重试"))
        assertTrue(message.contains("provider verification returned HTTP 401"))
    }

    @Test
    fun aiConfigErrorIgnoresBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "保存失败，请稍后重试",
            aiConfigError("保存失败，请稍后重试", "  \n\t  ", isDebuggable = true),
        )
    }

    @Test
    fun aiConfigThrowableErrorKeepsOrdinaryFailuresHandled() {
        assertEquals(
            "保存失败，请稍后重试",
            aiConfigThrowableError(
                "保存失败，请稍后重试",
                IllegalStateException("provider write failed apiKey=sk-private baseUrl=https://proxy.internal/v1"),
            ),
        )
    }

    @Test
    fun aiConfigThrowableErrorPreservesCoroutineCancellation() {
        try {
            aiConfigThrowableError("保存失败，请稍后重试", CancellationException("ai config disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("ai config disposed", e.message)
        }
    }
}
