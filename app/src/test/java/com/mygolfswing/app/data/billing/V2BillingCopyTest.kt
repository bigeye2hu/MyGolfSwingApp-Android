package com.mygolfswing.app.data.billing

import com.android.billingclient.api.BillingClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2BillingCopyTest {
    @Test
    fun releaseBillingMessageHidesGooglePlayDebugDetail() {
        val message = V2BillingCopy.billingMessage(
            responseCode = BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            debugMessage = "java.io.IOException: binder died",
            fallback = "购买失败，请稍后重试",
            isDebuggable = false,
        )

        assertEquals("Google Play 购买服务暂时不可用，请检查网络后重试", message)
        assertFalse(message.contains("binder died"))
        assertFalse(message.contains("IOException"))
    }

    @Test
    fun debugBillingMessageKeepsDiagnosticDetail() {
        val message = V2BillingCopy.billingMessage(
            responseCode = BillingClient.BillingResponseCode.DEVELOPER_ERROR,
            debugMessage = "ProductDetails missing offer token",
            fallback = "购买失败，请稍后重试",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("购买失败，请稍后重试"))
        assertTrue(message.contains("ProductDetails missing offer token"))
    }

    @Test
    fun releaseCreditGrantFailureKeepsSupportLabelButHidesBackendDetail() {
        val message = V2BillingCopy.creditGrantFailureMessage(
            causeMessage = "500 duplicate purchase_token stacktrace",
            orderLabel = "订单凭据尾号：1234ABCD",
            isDebuggable = false,
        )

        assertTrue(message.contains("订单凭据尾号：1234ABCD"))
        assertFalse(message.contains("duplicate purchase_token"))
        assertFalse(message.contains("stacktrace"))
    }

    @Test
    fun supportOrderLabelPrefersOrderIdAndFallsBackToTokenSuffix() {
        assertEquals(
            "订单号：GPA.1234-5678-9012-34567",
            V2BillingCopy.supportOrderLabel(
                orderId = "GPA.1234-5678-9012-34567",
                purchaseToken = "token-token-token",
            ),
        )
        assertEquals(
            "订单凭据尾号：CDEF1234",
            V2BillingCopy.supportOrderLabel(
                orderId = "",
                purchaseToken = "purchase-token-ABCDEF1234",
            ),
        )
    }

    @Test
    fun creditProductCatalogMatchesPlayConsoleChecklist() {
        assertEquals(
            listOf(
                "com.mygolfswingapp.credits.100",
                "com.mygolfswingapp.credits.300",
                "com.mygolfswingapp.credits.1000",
            ),
            V2CreditProductCatalog.products.map { it.productId },
        )
        assertEquals(listOf(100, 300, 1000), V2CreditProductCatalog.products.map { it.credits })
    }

    @Test
    fun billingCreditProductIdFindsCatalogProductBeyondFirstCallbackEntry() {
        assertEquals(
            "com.mygolfswingapp.credits.300",
            billingCreditProductId(
                listOf(
                    "unknown.product",
                    "com.mygolfswingapp.credits.300",
                    "com.mygolfswingapp.credits.1000",
                ),
            ),
        )
        assertEquals(null, billingCreditProductId(listOf("unknown.product")))
    }

    @Test
    fun billingObfuscatedAccountIdIsStableAndDoesNotExposeRawUserId() {
        val id = billingObfuscatedAccountId(
            packageName = "com.mygolfswing.app",
            userId = "tencent-user-private-123",
        )

        assertEquals(
            id,
            billingObfuscatedAccountId(
                packageName = " com.mygolfswing.app ",
                userId = " tencent-user-private-123 ",
            ),
        )
        assertEquals(64, id?.length)
        assertTrue(id.orEmpty().matches(Regex("[0-9a-f]{64}")))
        assertFalse(id.orEmpty().contains("tencent-user-private-123"))
        assertFalse(id.orEmpty().contains("com.mygolfswing.app"))
    }

    @Test
    fun billingObfuscatedAccountIdRejectsMissingInputs() {
        assertEquals(null, billingObfuscatedAccountId("com.mygolfswing.app", null))
        assertEquals(null, billingObfuscatedAccountId("com.mygolfswing.app", " "))
        assertEquals(null, billingObfuscatedAccountId(" ", "user-1"))
    }

    @Test
    fun grantedTokenRecordNormalizesDeduplicatesAndCapsPendingConsumeTokens() {
        val existing = (1..MAX_GRANTED_PURCHASE_TOKENS).fold(emptySet<String>()) { records, index ->
            billingGrantedTokenRecordsAfterRecord(records, "token-$index")
        }.reversedIterationOrder()

        val updated = billingGrantedTokenRecordsAfterRecord(existing, " token-new ")
        val tokens = billingGrantedTokensFromRecords(updated)

        assertEquals(MAX_GRANTED_PURCHASE_TOKENS, updated.size)
        assertTrue("token-new" in tokens)
        assertFalse("token-1" in tokens)
        assertEquals(
            updated,
            billingGrantedTokenRecordsAfterRecord(updated, "token-new"),
        )
    }

    @Test
    fun grantedTokenForgetRemovesOnlyMatchingNormalizedToken() {
        val records = billingGrantedTokenRecordsAfterRecord(
            billingGrantedTokenRecordsAfterRecord(setOf(" "), " token-a "),
            "token-b",
        )

        val updated = billingGrantedTokenRecordsAfterForget(
            records,
            "token-a",
        )

        assertEquals(setOf("token-b"), billingGrantedTokensFromRecords(updated))
    }

    @Test
    fun grantedTokenRecordIgnoresBlankToken() {
        val existing = setOf("token-a")

        assertEquals(existing, billingGrantedTokenRecordsAfterRecord(existing, " "))
    }

    @Test
    fun runBillingCloseCatchingKeepsCloseFailuresIsolated() {
        val error = IllegalStateException("BillingClient endConnection failed")
        val result = runBillingCloseCatching { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runBillingCloseCatchingReportsSuccessfulClose() {
        var closed = false
        val result = runBillingCloseCatching { closed = true }

        assertTrue(result.isSuccess)
        assertTrue(closed)
    }

    private fun Set<String>.reversedIterationOrder(): Set<String> =
        toList().asReversed().toCollection(linkedSetOf())
}
