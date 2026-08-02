package com.mygolfswing.app.ui.feedback

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2FeedbackCopyTest {

    @Test
    fun releaseFeedbackErrorHidesPrimaryAndLegacyChannelDiagnostics() {
        val message = feedbackSubmitError(
            base = "提交失败，反馈通道暂不可用，请稍后重试",
            debugDetail = "accountManagement.submitFeedback HTTP 500；备用通道：java.net.SocketTimeoutException token=/users/private",
            isDebuggable = false,
        )

        assertEquals("提交失败，反馈通道暂不可用，请稍后重试", message)
        assertFalse(message.contains("accountManagement"))
        assertFalse(message.contains("HTTP 500"))
        assertFalse(message.contains("SocketTimeoutException"))
        assertFalse(message.contains("/users/"))
    }

    @Test
    fun debugFeedbackErrorKeepsCombinedChannelDiagnostics() {
        val message = feedbackSubmitError(
            base = "提交失败，反馈通道暂不可用，请稍后重试",
            debugDetail = "cloud rejected payload；备用通道：legacy 502",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("提交失败，反馈通道暂不可用，请稍后重试"))
        assertTrue(message.contains("cloud rejected payload"))
        assertTrue(message.contains("备用通道：legacy 502"))
    }

    @Test
    fun feedbackErrorIgnoresBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "提交失败，反馈通道暂不可用，请稍后重试",
            feedbackSubmitError(
                base = "提交失败，反馈通道暂不可用，请稍后重试",
                debugDetail = "  \n\t  ",
                isDebuggable = true,
            ),
        )
    }

    @Test
    fun feedbackThrowableErrorKeepsOrdinaryFailuresHandled() {
        assertEquals(
            "提交失败，反馈通道暂不可用，请稍后重试",
            feedbackSubmitThrowableError(
                "提交失败，反馈通道暂不可用，请稍后重试",
                IllegalStateException("feedback backend failed token=/users/private"),
            ),
        )
    }

    @Test
    fun feedbackThrowableErrorPreservesCoroutineCancellation() {
        try {
            feedbackSubmitThrowableError("提交失败，反馈通道暂不可用，请稍后重试", CancellationException("feedback disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("feedback disposed", e.message)
        }
    }
}
