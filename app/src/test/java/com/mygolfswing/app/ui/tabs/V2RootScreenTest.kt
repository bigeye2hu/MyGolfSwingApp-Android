package com.mygolfswing.app.ui.tabs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2RootScreenTest {

    @Test
    fun runRootBackgroundSyncCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        val error = IllegalStateException("root profile refresh failed /users/private")
        val result = runRootBackgroundSyncCatching { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runRootBackgroundSyncCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runRootBackgroundSyncCatching { throw CancellationException("root disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("root disposed", e.message)
        }
    }
}
