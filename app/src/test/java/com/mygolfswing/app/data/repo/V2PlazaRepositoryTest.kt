package com.mygolfswing.app.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2PlazaRepositoryTest {

    @Test
    fun runPlazaCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        val error = IllegalStateException("plaza cloud failed")
        val result = runPlazaCatching<Unit> { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runPlazaCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runPlazaCatching<Unit> { throw CancellationException("plaza repository disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("plaza repository disposed", e.message)
        }
    }
}
