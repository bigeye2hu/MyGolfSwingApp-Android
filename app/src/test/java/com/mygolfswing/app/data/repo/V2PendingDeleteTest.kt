package com.mygolfswing.app.data.repo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2PendingDeleteTest {

    @Test
    fun isPendingCloudDeleteNormalizesIds() {
        val pending = setOf(" ABC-123 ", "def-456")

        assertTrue("abc-123".isPendingCloudDelete(pending))
        assertTrue(" DEF-456 ".isPendingCloudDelete(pending))
    }

    @Test
    fun isPendingCloudDeleteRejectsMissingOrBlankIds() {
        val pending = setOf("abc-123")

        assertFalse(null.isPendingCloudDelete(pending))
        assertFalse(" ".isPendingCloudDelete(pending))
        assertFalse("other".isPendingCloudDelete(pending))
    }
}
