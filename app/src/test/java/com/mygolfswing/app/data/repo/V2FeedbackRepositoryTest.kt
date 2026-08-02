package com.mygolfswing.app.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2FeedbackRepositoryTest {

    @Test
    fun feedbackSubmitFailureKeepsOrdinaryFailuresHandled() {
        val error = IllegalStateException("cloud and legacy feedback failed")
        val result = feedbackSubmitFailure<Unit>(error)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun feedbackSubmitFailurePreservesCoroutineCancellation() {
        try {
            feedbackSubmitFailure<Unit>(CancellationException("feedback screen disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("feedback screen disposed", e.message)
        }
    }

    @Test
    fun legacyFeedbackFailureMessageAcceptsExplicitServerSuccess() {
        val failure = legacyFeedbackFailureMessage(
            isHttpSuccessful = false,
            httpMessage = "Internal Server Error",
            parsed = JsonObject(mapOf("success" to JsonPrimitive(true))),
            cloudError = IllegalStateException("cloud failed"),
        )

        assertNull(failure)
    }

    @Test
    fun legacyFeedbackFailureMessageAcceptsHttpSuccessWithoutServerError() {
        val failure = legacyFeedbackFailureMessage(
            isHttpSuccessful = true,
            httpMessage = "OK",
            parsed = JsonObject(emptyMap()),
            cloudError = null,
        )

        assertNull(failure)
    }

    @Test
    fun legacyFeedbackFailureMessageUsesExplicitServerError() {
        val failure = legacyFeedbackFailureMessage(
            isHttpSuccessful = true,
            httpMessage = "OK",
            parsed = JsonObject(
                mapOf(
                    "success" to JsonPrimitive(false),
                    "message" to JsonPrimitive("legacy rejected content"),
                ),
            ),
            cloudError = null,
        )

        assertEquals("legacy rejected content", failure)
    }

    @Test
    fun legacyFeedbackFailureMessageCombinesCloudAndLegacyFailures() {
        val failure = legacyFeedbackFailureMessage(
            isHttpSuccessful = false,
            httpMessage = "Bad Gateway",
            parsed = null,
            cloudError = IllegalStateException("cloud submit failed"),
        )

        assertEquals("cloud submit failed；备用通道：Bad Gateway", failure)
    }
}
