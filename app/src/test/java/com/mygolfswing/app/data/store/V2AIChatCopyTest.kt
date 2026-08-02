package com.mygolfswing.app.data.store

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2AIChatCopyTest {

    @Test
    fun releaseAIChatErrorHidesProviderDiagnostics() {
        val message = aiChatUserError(
            base = "档案同步失败，请稍后重试",
            debugDetail = "DashScope HTTP 500 /compatible-mode/v1/chat/completions apiKey=sk-secret model=qwen-plus",
            isDebuggable = false,
        )

        assertEquals("档案同步失败，请稍后重试", message)
        assertFalse(message.contains("DashScope"))
        assertFalse(message.contains("HTTP 500"))
        assertFalse(message.contains("/compatible-mode/"))
        assertFalse(message.contains("sk-secret"))
        assertFalse(message.contains("qwen-plus"))
    }

    @Test
    fun releaseAIChatErrorHidesArchiveSyncDiagnostics() {
        val message = aiChatUserError(
            base = "档案同步失败，请稍后重试",
            debugDetail = "reflectAll failed session=private-session uid=/users/u123 file=/data/user/0/com.mygolfswing.app/profile.json",
            isDebuggable = false,
        )

        assertEquals("档案同步失败，请稍后重试", message)
        assertFalse(message.contains("reflectAll"))
        assertFalse(message.contains("private-session"))
        assertFalse(message.contains("/users/"))
        assertFalse(message.contains("/data/user/0"))
    }

    @Test
    fun debugAIChatErrorKeepsDiagnostics() {
        val message = aiChatUserError(
            base = "请求失败，请稍后重试",
            debugDetail = "stream timed out after provider reconnect",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("请求失败，请稍后重试"))
        assertTrue(message.contains("stream timed out after provider reconnect"))
    }

    @Test
    fun aiChatErrorIgnoresBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "请求失败，请稍后重试",
            aiChatUserError("请求失败，请稍后重试", " \n\t ", isDebuggable = true),
        )
        assertEquals(
            "请求失败，请稍后重试",
            aiChatUserError("请求失败，请稍后重试", "", isDebuggable = false),
        )
    }

    @Test
    fun aiChatMockProviderIsDebugOnlyAndExplicit() {
        assertFalse(shouldUseAIChatMockProvider(requestedMockProvider = true, isDebuggable = false))
        assertFalse(shouldUseAIChatMockProvider(requestedMockProvider = false, isDebuggable = true))
        assertTrue(shouldUseAIChatMockProvider(requestedMockProvider = true, isDebuggable = true))
    }

    @Test
    fun aiChatReflectionCancellationOnlyClearsActiveReflectionStatus() {
        assertTrue(shouldClearAIChatReflectionStatusOnCancellation(V2AIChatStore.AI_REFLECTION_STATUS))
        assertFalse(shouldClearAIChatReflectionStatusOnCancellation("请求失败，请稍后重试"))
        assertFalse(shouldClearAIChatReflectionStatusOnCancellation(null))
    }

    @Test
    fun runAIQuotaCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        val error = IllegalStateException("quota refresh failed")
        val result = runAIQuotaCatching<Unit> { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runAIQuotaCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runAIQuotaCatching<Unit> { throw CancellationException("ai quota disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("ai quota disposed", e.message)
        }
    }

    @Test
    fun runAITrainingContextCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        val error = IllegalStateException("profile reload failed /data/user/private")
        val result = runAITrainingContextCatching<Unit> { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runAITrainingContextCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runAITrainingContextCatching<Unit> { throw CancellationException("ai send disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("ai send disposed", e.message)
        }
    }
}
