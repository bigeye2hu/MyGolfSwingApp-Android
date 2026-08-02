package com.mygolfswing.app.data.store

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2MemoStoreTest {

    @Test
    fun runMemoStoreSyncCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        val error = IllegalStateException("memo pull failed endpoint=/users/private/memos")
        val result = runMemoStoreSyncCatching { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runMemoStoreSyncCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runMemoStoreSyncCatching { throw CancellationException("memo store disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("memo store disposed", e.message)
        }
    }
}
