package com.mygolfswing.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class V2AIServiceTest {

    @Test
    fun normalizedAIBaseUrlAcceptsHttpsAndAddsTrailingSlash() {
        assertEquals("https://api.example.com/v1/", normalizedAIBaseUrl(" https://api.example.com/v1 "))
        assertEquals("https://api.example.com/v1/", normalizedAIBaseUrl("https://api.example.com/v1/"))
    }

    @Test
    fun normalizedAIBaseUrlRejectsCleartextOrMalformedUrls() {
        assertNull(normalizedAIBaseUrl("http://api.example.com/v1"))
        assertNull(normalizedAIBaseUrl("api.example.com/v1"))
        assertNull(normalizedAIBaseUrl(" "))
        assertNull(normalizedAIBaseUrl(null))
    }
}
