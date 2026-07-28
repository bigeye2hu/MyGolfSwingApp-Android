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

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    fun loadProducts() {
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
                val missing = next.count { it.productDetails == null }
                _state.value = _state.value.copy(
                    products = next,
                    loadingProducts = false,
                    error = if (missing == next.size) {
                        result.userFacingMessage("未能加载积分商品，请确认 Google Play 已登录并稍后刷新")
                    } else {
                        null
                    },
                    message = if (missing in 1 until next.size) "部分商品暂不可购买" else null,
                )
                restorePendingPurchases(showEmptyMessage = false)
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
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val updatedPurchases = purchases.orEmpty()
                if (updatedPurchases.isEmpty()) {
                    _state.value = _state.value.copy(
                        purchasingProductId = null,
                        message = "购买未完成，如已支付请点“刷新/恢复”同步订单",
                    )
                } else {
                    updatedPurchases.forEach(::handlePurchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                _state.value = _state.value.copy(purchasingProductId = null, message = "已取消购买")
            else ->
                _state.value = _state.value.copy(
                    purchasingProductId = null,
                    error = billingResult.userFacingMessage("购买失败，请稍后重试"),
                )
        }
    }

    fun clearNotice() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    fun close() {
        runCatching { billingClient.endConnection() }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (!processingPurchaseTokens.add(purchase.purchaseToken)) return
        val productId = purchase.products.firstOrNull().orEmpty()
        val catalogProduct = V2CreditProductCatalog.productFor(productId)
        if (catalogProduct == null) {
            processingPurchaseTokens.remove(purchase.purchaseToken)
            _state.value = _state.value.copy(
                purchasingProductId = null,
                error = releaseSafeError(
                    base = "积分商品信息异常，请点“刷新/恢复”后重试",
                    debugDetail = productId.takeIf { it.isNotBlank() }?.let { "未知积分商品：$it" },
                ),
            )
            return
        }
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            processingPurchaseTokens.remove(purchase.purchaseToken)
            _state.value = _state.value.copy(purchasingProductId = null, message = "购买待确认，到账后会同步积分")
            return
        }
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            processingPurchaseTokens.remove(purchase.purchaseToken)
            return
        }
        scope.launch {
            val quantity = purchase.quantity.coerceAtLeast(1)
            val transactionId = purchase.orderId?.takeIf { it.isNotBlank() } ?: purchase.purchaseToken
            val orderLabel = purchase.supportOrderLabel()
            val grant = ServiceLocator.creditsRepo.addCreditsFromIap(
                amount = catalogProduct.credits * quantity,
                transactionId = transactionId,
                productId = productId,
                purchaseToken = purchase.purchaseToken,
                packageName = appContext.packageName,
                quantity = quantity,
            )
            grant.onSuccess { balance ->
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

    private fun consumePurchase(purchaseToken: String) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.consumeAsync(params) { result, _ ->
            processingPurchaseTokens.remove(purchaseToken)
            if (!result.isOk) {
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
        val base = when (responseCode) {
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.NETWORK_ERROR ->
                "Google Play 购买服务暂时不可用，请检查网络后重试"
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
                "当前设备暂不可使用 Google Play 购买，请确认已登录 Google Play"
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED ->
                "当前 Google Play 版本暂不支持此购买功能，请更新后再试"
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
                "该积分商品暂不可购买，请稍后刷新"
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                "已有未完成订单，请点“刷新/恢复”同步积分"
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED ->
                "未找到可恢复的积分订单"
            BillingClient.BillingResponseCode.DEVELOPER_ERROR,
            BillingClient.BillingResponseCode.ERROR ->
                fallback
            BillingClient.BillingResponseCode.USER_CANCELED ->
                "已取消购买"
            else -> fallback
        }
        return releaseSafeError(base, debugMessage)
    }

    private fun creditGrantFailureMessage(cause: Throwable, orderLabel: String): String {
        return releaseSafeError(
            base = "购买成功但积分发放失败，请点“刷新/恢复”重试；如仍未到账，请联系客服处理订单。$orderLabel",
            debugDetail = cause.message,
        )
    }

    private fun releaseSafeError(base: String, debugDetail: String?): String {
        val detail = debugDetail.takeIf { ServiceLocator.isDebuggable && !it.isNullOrBlank() }
        return if (detail == null) base else "$base（$detail）"
    }

    private fun okBillingResult(): BillingResult =
        BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build()

    private fun Purchase.supportOrderLabel(): String {
        val orderId = orderId?.takeIf { it.isNotBlank() }
        return if (orderId != null) {
            "订单号：$orderId"
        } else {
            "订单凭据尾号：${purchaseToken.takeLast(8)}"
        }
    }

    private fun currentObfuscatedAccountId(): String? {
        val userId = V2ProfileStore.profile.value.userId?.takeIf { it.isNotBlank() } ?: return null
        val input = "${appContext.packageName}:$userId"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
