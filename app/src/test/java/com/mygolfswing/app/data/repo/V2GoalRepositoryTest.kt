package com.mygolfswing.app.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2GoalRepositoryTest {

    @Test
    fun demoGoalCleanupOnlyDeletesCleanLocalRowsMissingFromCloud() {
        assertTrue(
            shouldDeleteLocalDemoGoal(
                goalTitle = "每日复盘心法",
                localGoalId = "local-demo-id",
                isDirty = false,
                cloudGoalIds = emptySet(),
            ),
        )

        assertFalse(
            shouldDeleteLocalDemoGoal(
                goalTitle = "每日复盘心法",
                localGoalId = "cloud-goal-id",
                isDirty = false,
                cloudGoalIds = setOf("cloud-goal-id"),
            ),
        )

        assertFalse(
            shouldDeleteLocalDemoGoal(
                goalTitle = "每日复盘心法",
                localGoalId = "offline-user-goal-id",
                isDirty = true,
                cloudGoalIds = emptySet(),
            ),
        )
    }

    @Test
    fun demoGoalCleanupIgnoresOrdinaryGoalTitles() {
        assertFalse(
            shouldDeleteLocalDemoGoal(
                goalTitle = "修正下杆节奏",
                localGoalId = "local-goal-id",
                isDirty = false,
                cloudGoalIds = emptySet(),
            ),
        )
    }

    @Test
    fun runGoalSyncCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        val error = IllegalStateException("goal sync failed")
        val result = runGoalSyncCatching<Int> { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runGoalSyncCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runGoalSyncCatching<Int> { throw CancellationException("goal sync disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("goal sync disposed", e.message)
        }
    }

    @Test
    fun runGoalCloudBooleanCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        assertFalse(
            runGoalCloudBooleanCatching {
                throw IllegalStateException("goal cloud write failed")
            },
        )
    }

    @Test
    fun runGoalCloudBooleanCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runGoalCloudBooleanCatching { throw CancellationException("goal cloud disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("goal cloud disposed", e.message)
        }
    }
}
