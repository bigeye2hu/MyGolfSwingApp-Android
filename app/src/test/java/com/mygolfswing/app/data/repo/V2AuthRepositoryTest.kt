package com.mygolfswing.app.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2AuthRepositoryTest {

    @Test
    fun runAuthCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        val error = IllegalStateException("auth request failed")
        val result = runAuthCatching<Unit> { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runAuthCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runAuthCatching<Unit> { throw CancellationException("auth screen disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("auth screen disposed", e.message)
        }
    }
}
