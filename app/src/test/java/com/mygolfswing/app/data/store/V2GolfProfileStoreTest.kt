package com.mygolfswing.app.data.store

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2GolfProfileStoreTest {

    @Test
    fun runGolfProfilePersistCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        val error = IllegalStateException("profile local save failed /data/user/private")
        val result = runGolfProfilePersistCatching { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runGolfProfilePersistCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runGolfProfilePersistCatching { throw CancellationException("profile store disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("profile store disposed", e.message)
        }
    }
}
