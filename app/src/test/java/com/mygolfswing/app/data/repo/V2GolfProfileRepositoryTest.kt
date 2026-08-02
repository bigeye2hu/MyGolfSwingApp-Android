package com.mygolfswing.app.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2GolfProfileRepositoryTest {

    @Test
    fun golfProfileCloudFailureResultKeepsOrdinaryFailuresHandled() {
        val error = IllegalStateException("cloud profile payload malformed")
        val result = golfProfileCloudFailureResult<String>(error)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun golfProfileCloudFailureResultPreservesCoroutineCancellation() {
        try {
            golfProfileCloudFailureResult<String>(CancellationException("archive screen disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("archive screen disposed", e.message)
        }
    }

    @Test
    fun runGolfProfileSideEffectCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        val error = IllegalStateException("saveUserGolfProfile failed uid=/users/private")
        val result = runGolfProfileSideEffectCatching { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runGolfProfileSideEffectCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runGolfProfileSideEffectCatching { throw CancellationException("profile upload disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("profile upload disposed", e.message)
        }
    }
}
