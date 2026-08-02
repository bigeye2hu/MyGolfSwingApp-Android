package com.mygolfswing.app.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2VideoDownloadRepositoryTest {

    @Test
    fun runDownloadCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        val error = IllegalStateException("download backend failed")
        val result = runDownloadCatching<Unit> { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runDownloadCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runDownloadCatching<Unit> { throw CancellationException("download repository disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("download repository disposed", e.message)
        }
    }
}
