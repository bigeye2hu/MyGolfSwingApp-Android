package com.mygolfswing.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import androidx.core.content.edit
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.store.V2ProfileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest
import kotlin.coroutines.resume

data class V2CreditProduct(
    val productId: String,
    val credits: Int,
    val fallbackPrice: String,
    val popular: Boolean = false,
    val productDetails: ProductDetails? = null,
) {
    val displayPrice: String
        get() = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice ?: fallbackPrice
}

data class V2CreditsPurchaseState(
    val products: List<V2CreditProduct> = V2CreditProductCatalog.products,
    val loadingProducts: Boolean = false,
    val purchasingProductId: String? = null,
    val message: String? = null,
    val error: String? = null,
)

object V2CreditProductCatalog {
    val products = listOf(
        V2CreditProduct("com.mygolfswingapp.credits.100", 100, "¥6.00"),
        V2CreditProduct("com.mygolfswingapp.credits.300", 300, "¥18.00", popular = true),
        V2CreditProduct("com.mygolfswingapp.credits.1000", 1000, "¥58.00"),
    )

    fun productFor(productId: String): V2CreditProduct? =
        products.firstOrNull { it.productId == productId }
}

class V2CreditsBillingManager(
    context: Context,
    private val scope: CoroutineScope,
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(V2CreditsPurchaseState())
    val state: StateFlow<V2CreditsPurchaseState> = _state.asStateFlow()
    private val processingPurchaseTokens = mutableSetOf<String>()
    private val grantedPurchaseStore = appContext.getSharedPreferences(
        GRANTED_PURCHASE_PREFS,
        Context.MODE_PRIVATE,
    )

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    fun loadProducts(showEmptyRestoreMessage: Boolean = false) {
        scope.launch(Dispatchers.Main) {
            _state.value = _state.value.copy(loadingProducts = true, error = null, message = null)
            val connected = ensureConnected()
            if (!connected.isOk) {
                _state.value = _state.value.copy(
                    loadingProducts = false,
                    error = connected.userFacingMessage("无法连接 Google Play 购买服务，请检查网络或稍后重试"),
                )
                return@launch
            }
            val productParams = V2CreditProductCatalog.products.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it.productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productParams)
                .build()
            billingClient.queryProductDetailsAsync(params) { result, productDetailsResult ->
                val detailsById = productDetailsResult.productDetailsList.associateBy { it.productId }
                val next = V2CreditProductCatalog.products.map { product ->
                    product.copy(productDetails = detailsById[product.productId])
                }
                _state.value = _state.value.copy(
                    products = next,
                    loadingProducts = false,
                    error = if (!result.isOk) {
                        result.userFacingMessage("未能加载积分商品，请确认 Google Play 已登录并稍后刷新")
                    } else {
                        null
                    },
                    // Missing/partial Play Console configuration is rendered as a non-blocking
                    // recovery card by the purchase page. Do not also present it as a green
                    // success notice or obscure the card with a failure dialog.
                    message = null,
                )
                // Keep product loading and pending-order restore ordered. Running both from the
                // screen at once can race and replace a real restore/grant notice with catalog UI.
                restorePendingPurchases(showEmptyMessage = showEmptyRestoreMessage)
            }
        }
    }

    fun restorePendingPurchases(showEmptyMessage: Boolean = true) {
        scope.launch(Dispatchers.Main) {
            val connected = ensureConnected()
            if (!connected.isOk) {
                _state.value = _state.value.copy(
                    error = connected.userFacingMessage("无法连接 Google Play 购买服务，请检查网络或稍后重试"),
                )
                return@launch
            }
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            billingClient.queryPurchasesAsync(params) { result, purchases ->
                if (!result.isOk) {
                    _state.value = _state.value.copy(
                        error = result.userFacingMessage("未能恢复未完成订单，请稍后再试"),
                    )
                    return@queryPurchasesAsync
                }
                val creditPurchases = purchases.filter { purchase ->
                    purchase.products.any { V2CreditProductCatalog.productFor(it) != null }
                }
                if (creditPurchases.isEmpty()) {
                    if (showEmptyMessage) _state.value = _state.value.copy(message = "没有需要恢复的积分订单")
                    return@queryPurchasesAsync
                }
                _state.value = _state.value.copy(message = "正在恢复 ${creditPurchases.size} 笔未完成订单")
                creditPurchases.forEach(::handlePurchase)
            }
        }
    }

    fun launchPurchase(activity: Activity, productId: String) {
        val product = _state.value.products.firstOrNull { it.productId == productId }
        val details = product?.productDetails
        if (details == null) {
            _state.value = _state.value.copy(error = "商品暂不可购买，请稍后刷新")
            return
        }
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
        currentObfuscatedAccountId()?.let { flowBuilder.setObfuscatedAccountId(it) }
        val flowParams = flowBuilder.build()
        _state.value = _state.value.copy(purchasingProductId = productId, error = null, message = null)
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (!result.isOk) {
            _state.value = _state.value.copy(
                purchasingProductId = null,
                error = result.userFacingMessage("无法打开 Google Play 购买页，请稍后重试"),
            )
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        val updatedPurchases = purchases.orEmpty()
        val terminalState = terminalPurchaseCallbackState(
            current = _state.value,
            responseCode = billingResult.responseCode,
            purchaseCount = updatedPurchases.size,
            failureMessage = billingResult.userFacingMessage("购买失败，请稍后重试"),
        )
        if (terminalState != null) {
            _state.value = terminalState
            return
        }
        updatedPurchases.forEach(::handlePurchase)
    }

    fun clearNotice() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    fun close() {
        runBillingCloseCatching { billingClient.endConnection() }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (!processingPurchaseTokens.add(purchase.purchaseToken)) return
        val productId = billingCreditProductId(purchase.products).orEmpty()
        val catalogProduct = V2CreditProductCatalog.productFor(productId)
        if (catalogProduct == null) {
            processingPurchaseTokens.remove(purchase.purchaseToken)
            _state.value = _state.value.copy(
                purchasingProductId = null,
                error = V2BillingCopy.releaseSafeError(
                    base = "积分商品信息异常，请点“刷新/恢复”后重试",
                    debugDetail = productId.takeIf { it.isNotBlank() }?.let { "未知积分商品：$it" },
                    isDebuggable = ServiceLocator.isDebuggable,
                ),
            )
            return
        }
        when (purchaseStateDisposition(purchase.purchaseState)) {
            V2PurchaseStateDisposition.Pending -> {
                processingPurchaseTokens.remove(purchase.purchaseToken)
                _state.value = _state.value.copy(
                    purchasingProductId = null,
                    message = "购买待确认，到账后会同步积分",
                    error = null,
                )
                return
            }
            V2PurchaseStateDisposition.Incomplete -> {
                processingPurchaseTokens.remove(purchase.purchaseToken)
                _state.value = _state.value.copy(
                    purchasingProductId = null,
                    message = "购买未完成，如已支付请点“刷新/恢复”同步订单",
                    error = null,
                )
                return
            }
            V2PurchaseStateDisposition.Purchased -> Unit
        }
        if (isGrantedPurchaseTokenRecorded(purchase.purchaseToken)) {
            _state.value = _state.value.copy(
                purchasingProductId = null,
                message = "正在完成已发放订单确认",
                error = null,
            )
            consumePurchase(
                purchaseToken = purchase.purchaseToken,
                successMessage = "已完成订单确认，积分已发放",
            )
            return
        }
        scope.launch {
            val quantity = purchase.quantity.coerceAtLeast(1)
            val transactionId = purchase.orderId?.takeIf { it.isNotBlank() } ?: purchase.purchaseToken
            val orderLabel = V2BillingCopy.supportOrderLabel(
                orderId = purchase.orderId,
                purchaseToken = purchase.purchaseToken,
            )
            val grant = ServiceLocator.creditsRepo.addCreditsFromIap(
                amount = catalogProduct.credits * quantity,
                transactionId = transactionId,
                productId = productId,
                purchaseToken = purchase.purchaseToken,
                packageName = appContext.packageName,
                quantity = quantity,
            )
            grant.onSuccess { balance ->
                recordGrantedPurchaseToken(purchase.purchaseToken)
                consumePurchase(purchase.purchaseToken)
                V2ProfileStore.refresh(force = true)
                _state.value = _state.value.copy(
                    purchasingProductId = null,
                    message = "充值成功，当前 $balance 积分",
                    error = null,
                )
            }.onFailure { e ->
                processingPurchaseTokens.remove(purchase.purchaseToken)
                _state.value = _state.value.copy(
                    purchasingProductId = null,
                    error = creditGrantFailureMessage(e, orderLabel),
                )
            }
        }
    }

    private suspend fun ensureConnected(): BillingResult = suspendCancellableCoroutine { cont ->
        if (billingClient.isReady) {
            cont.resume(okBillingResult())
            return@suspendCancellableCoroutine
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (cont.isActive) cont.resume(billingResult)
            }

            override fun onBillingServiceDisconnected() {
                if (cont.isActive) cont.resume(
                    BillingResult.newBuilder()
                        .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
                        .setDebugMessage("Google Play 购买服务已断开")
                        .build(),
                )
            }
        })
    }

    private fun consumePurchase(purchaseToken: String, successMessage: String? = null) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.consumeAsync(params) { result, _ ->
            processingPurchaseTokens.remove(purchaseToken)
            if (result.isOk) {
                forgetGrantedPurchaseToken(purchaseToken)
                if (successMessage != null) {
                    _state.value = _state.value.copy(
                        purchasingProductId = null,
                        message = successMessage,
                        error = null,
                    )
                }
            } else {
                _state.value = _state.value.copy(
                    purchasingProductId = null,
                    error = result.userFacingMessage("积分已发放，但订单消费确认失败；请点“刷新/恢复”再次同步"),
                )
            }
        }
    }

    private val BillingResult.isOk: Boolean
        get() = responseCode == BillingClient.BillingResponseCode.OK

    private fun BillingResult.userFacingMessage(fallback: String): String {
        return V2BillingCopy.billingMessage(
            responseCode = responseCode,
            debugMessage = debugMessage,
            fallback = fallback,
            isDebuggable = ServiceLocator.isDebuggable,
        )
    }

    private fun creditGrantFailureMessage(cause: Throwable, orderLabel: String): String {
        return V2BillingCopy.creditGrantFailureMessage(
            causeMessage = cause.message,
            orderLabel = orderLabel,
            isDebuggable = ServiceLocator.isDebuggable,
        )
    }

    private fun okBillingResult(): BillingResult =
        BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build()

    private fun currentObfuscatedAccountId(): String? {
        val userId = V2ProfileStore.profile.value.userId?.takeIf { it.isNotBlank() } ?: return null
        return billingObfuscatedAccountId(appContext.packageName, userId)
    }

    private fun isGrantedPurchaseTokenRecorded(purchaseToken: String): Boolean =
        purchaseToken.trim().takeIf { it.isNotBlank() } in
            billingGrantedTokensFromRecords(grantedPurchaseTokenRecords())

    private fun recordGrantedPurchaseToken(purchaseToken: String) {
        grantedPurchaseStore.edit {
            putStringSet(
                GRANTED_PURCHASE_TOKENS_KEY,
                billingGrantedTokenRecordsAfterRecord(grantedPurchaseTokenRecords(), purchaseToken),
            )
        }
    }

    private fun forgetGrantedPurchaseToken(purchaseToken: String) {
        grantedPurchaseStore.edit {
            putStringSet(
                GRANTED_PURCHASE_TOKENS_KEY,
                billingGrantedTokenRecordsAfterForget(grantedPurchaseTokenRecords(), purchaseToken),
            )
        }
    }

    private fun grantedPurchaseTokenRecords(): Set<String> =
        grantedPurchaseStore.getStringSet(GRANTED_PURCHASE_TOKENS_KEY, emptySet()).orEmpty()
}

internal fun billingObfuscatedAccountId(packageName: String, userId: String?): String? {
    val normalizedUserId = userId?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val normalizedPackageName = packageName.trim().takeIf { it.isNotBlank() } ?: return null
    val input = "$normalizedPackageName:$normalizedUserId"
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

internal fun billingCreditProductId(products: List<String>): String? =
    products.firstOrNull { V2CreditProductCatalog.productFor(it) != null }

/**
 * Returns a terminal UI state for Billing callbacks that have no purchases to process.
 * A null result means the successful purchase list must continue through grant/consume handling.
 */
internal fun terminalPurchaseCallbackState(
    current: V2CreditsPurchaseState,
    responseCode: Int,
    purchaseCount: Int,
    failureMessage: String,
): V2CreditsPurchaseState? = when {
    responseCode == BillingClient.BillingResponseCode.OK && purchaseCount > 0 -> null
    responseCode == BillingClient.BillingResponseCode.OK -> current.copy(
        purchasingProductId = null,
        message = "购买未完成，如已支付请点“刷新/恢复”同步订单",
        error = null,
    )
    responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> current.copy(
        purchasingProductId = null,
        // Match iOS StoreKit cancellation: close the purchasing posture quietly.
        message = null,
        error = null,
    )
    else -> current.copy(
        purchasingProductId = null,
        message = null,
        error = failureMessage,
    )
}

internal enum class V2PurchaseStateDisposition {
    Purchased,
    Pending,
    Incomplete,
}

internal fun purchaseStateDisposition(purchaseState: Int): V2PurchaseStateDisposition = when (purchaseState) {
    Purchase.PurchaseState.PURCHASED -> V2PurchaseStateDisposition.Purchased
    Purchase.PurchaseState.PENDING -> V2PurchaseStateDisposition.Pending
    else -> V2PurchaseStateDisposition.Incomplete
}

internal inline fun runBillingCloseCatching(block: () -> Unit): Result<Unit> = try {
    block()
    Result.success(Unit)
} catch (e: Throwable) {
    Result.failure(e)
}

private const val GRANTED_PURCHASE_PREFS = "v2_google_play_billing"
private const val GRANTED_PURCHASE_TOKENS_KEY = "granted_purchase_tokens"
internal const val MAX_GRANTED_PURCHASE_TOKENS = 64
private const val GRANTED_PURCHASE_TOKEN_RECORD_SEPARATOR = ":"

private data class BillingGrantedTokenRecord(
    val sequence: Long,
    val token: String,
)

internal fun billingGrantedTokenRecordsAfterRecord(
    existing: Set<String>,
    purchaseToken: String,
): Set<String> {
    val token = purchaseToken.trim().takeIf { it.isNotBlank() } ?: return existing
    val records = normalizedGrantedTokenRecords(existing)
    val existingRecord = records.lastOrNull { it.token == token }
    if (existingRecord != null) {
        return (records.filterNot { it.token == token }.takeLast(MAX_GRANTED_PURCHASE_TOKENS - 1) +
            existingRecord)
            .map { it.serialize() }
            .toSet()
    }
    val nextSequence = records.maxOfOrNull { it.sequence }?.let { last ->
        if (last == Long.MAX_VALUE) 1L else last + 1L
    } ?: 1L
    return (records.filterNot { it.token == token }.takeLast(MAX_GRANTED_PURCHASE_TOKENS - 1) +
        BillingGrantedTokenRecord(nextSequence, token))
        .map { it.serialize() }
        .toSet()
}

internal fun billingGrantedTokenRecordsAfterForget(
    existing: Set<String>,
    purchaseToken: String,
): Set<String> {
    val token = purchaseToken.trim().takeIf { it.isNotBlank() } ?: return existing
    return normalizedGrantedTokenRecords(existing)
        .filterNot { it.token == token }
        .map { it.serialize() }
        .toSet()
}

internal fun billingGrantedTokensFromRecords(records: Set<String>): Set<String> =
    normalizedGrantedTokenRecords(records)
        .map { it.token }
        .toSet()

private fun normalizedGrantedTokenRecords(records: Set<String>): List<BillingGrantedTokenRecord> {
    val latestByToken = mutableMapOf<String, BillingGrantedTokenRecord>()
    records.mapIndexedNotNull { index, raw ->
        val trimmed = raw.trim().takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
        parseGrantedTokenRecord(trimmed)
            ?: BillingGrantedTokenRecord(sequence = index.toLong(), token = trimmed)
    }.sortedBy { it.sequence }.forEach { record ->
        latestByToken[record.token] = record
    }
    return latestByToken.values.sortedBy { it.sequence }
}

private fun parseGrantedTokenRecord(raw: String): BillingGrantedTokenRecord? {
    val separatorIndex = raw.indexOf(GRANTED_PURCHASE_TOKEN_RECORD_SEPARATOR)
    if (separatorIndex <= 0) return null
    val sequence = raw.substring(0, separatorIndex)
        .takeIf { it.all(Char::isDigit) }
        ?.toLongOrNull()
        ?: return null
    val token = raw.substring(separatorIndex + 1).trim().takeIf { it.isNotBlank() } ?: return null
    return BillingGrantedTokenRecord(sequence = sequence, token = token)
}

private fun BillingGrantedTokenRecord.serialize(): String =
    sequence.toString().padStart(19, '0') + GRANTED_PURCHASE_TOKEN_RECORD_SEPARATOR + token
