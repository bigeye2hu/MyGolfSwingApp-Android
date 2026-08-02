package com.mygolfswing.app.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2CreditsRepositoryTest {

    @Test
    fun iapCreditGrantPayloadMatchesGooglePlayGrantContract() {
        val payload = iapCreditGrantPayload(
            amount = 300,
            transactionId = "GPA.1234-5678-9012-34567",
            productId = "com.mygolfswingapp.credits.300",
            purchaseToken = "purchase-token-private",
            packageName = "com.mygolfswing.app",
            quantity = 1,
        )

        assertEquals(300, payload["amount"]?.jsonPrimitive?.intOrNull)
        assertEquals("GPA.1234-5678-9012-34567", payload["transaction_id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("com.mygolfswingapp.credits.300", payload["product_id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("purchase-token-private", payload["purchase_token"]?.jsonPrimitive?.contentOrNull)
        assertEquals("com.mygolfswing.app", payload["package_name"]?.jsonPrimitive?.contentOrNull)
        assertEquals(1, payload["quantity"]?.jsonPrimitive?.intOrNull)
        assertEquals("google_play", payload["platform"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Google Play购买", payload["reason"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun iapCreditGrantPayloadClampsQuantityForPlayOneTimeProductCallbacks() {
        val payload = iapCreditGrantPayload(
            amount = 100,
            transactionId = "purchase-token-fallback",
            productId = "com.mygolfswingapp.credits.100",
            purchaseToken = "purchase-token-fallback",
            packageName = "com.mygolfswing.app",
            quantity = 0,
        )

        assertEquals(1, payload["quantity"]?.jsonPrimitive?.intOrNull)
    }

    @Test
    fun runCreditsCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        val error = IllegalStateException("credits refresh failed")
        val result = runCreditsCatching<Int> { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runCreditsCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runCreditsCatching<Int> { throw CancellationException("credits screen disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("credits screen disposed", e.message)
        }
    }
}
