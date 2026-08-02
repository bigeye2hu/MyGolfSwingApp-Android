package com.mygolfswing.app.data.net

import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Test

class V2NetClientTest {

    @Test
    fun releaseBuildDoesNotInstallHttpLogging() {
        assertNull(networkLoggingLevel(enableHttpLogging = false))
    }

    @Test
    fun debugBuildUsesBasicHttpLoggingOnly() {
        val level = networkLoggingLevel(enableHttpLogging = true)

        assertEquals(HttpLoggingInterceptor.Level.BASIC, level)
        assertNotEquals(HttpLoggingInterceptor.Level.BODY, level)
        assertNotEquals(HttpLoggingInterceptor.Level.HEADERS, level)
    }
}
