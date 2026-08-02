package com.mygolfswing.app.data.store

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2ProfileStoreTest {

    @Test
    fun runProfileRefreshCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        val error = IllegalStateException("profile refresh failed endpoint=/users/private")
        val result = runProfileRefreshCatching { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runProfileRefreshCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runProfileRefreshCatching { throw CancellationException("profile refresh disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("profile refresh disposed", e.message)
        }
    }
}
