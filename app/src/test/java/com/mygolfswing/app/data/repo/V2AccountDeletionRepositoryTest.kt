package com.mygolfswing.app.data.repo

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2AccountDeletionRepositoryTest {

    @Test
    fun accountDeletionFailureResultKeepsOrdinaryFailuresHandled() {
        val error = IllegalStateException("account deletion cleanup failed")
        val result = accountDeletionFailureResult<Unit>(error)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun accountDeletionFailureResultPreservesCoroutineCancellation() {
        try {
            accountDeletionFailureResult<Unit>(CancellationException("account deletion disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("account deletion disposed", e.message)
        }
    }
}
