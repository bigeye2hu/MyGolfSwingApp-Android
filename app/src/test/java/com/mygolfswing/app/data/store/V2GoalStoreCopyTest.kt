package com.mygolfswing.app.data.store

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2GoalStoreCopyTest {

    @Test
    fun goalMockSeedIsDebugOnlyAndOnlyForEmptyGoalLists() {
        assertFalse(shouldUseGoalMockSeed(hasExistingGoals = false, isDebuggable = false))
        assertFalse(shouldUseGoalMockSeed(hasExistingGoals = true, isDebuggable = true))
        assertTrue(shouldUseGoalMockSeed(hasExistingGoals = false, isDebuggable = true))
    }
}
