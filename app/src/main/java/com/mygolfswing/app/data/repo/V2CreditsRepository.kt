package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 积分仓库（对齐 iOS `CreditsManager` + `CreditsService`）。
 *
 * - `GET /credits/balance`（download 服务器，需 X-TC-UID/UserSig 头）
 * - 5 分钟缓存，超过缓存才请求
 * - 拉到后同时写到 [V2SessionStore]，方便其它界面订阅
 */
class V2CreditsRepository(
    private val net: V2NetClient,
    private val session: V2SessionStore,
) {

    private val lastFetchAtByUser = mutableMapOf<String, Long>()
    private val cacheTtlMs: Long = 5 * 60 * 1000

    /**
     * 拉取积分余额。
     * @param force true 表示忽略缓存
     * @return 可用余额（int），失败时返回 null
     */
    suspend fun fetchBalance(force: Boolean = false): Int? {
        val uid = session.current().userId ?: return null
        @Suppress("UNUSED_VARIABLE") val _uid = uid
        val now = System.currentTimeMillis()
        if (!force && now - (lastFetchAtByUser[uid] ?: 0L) < cacheTtlMs) {
            return session.current().credits
        }
        return runCreditsCatching {
            val resp = net.downloadApi.creditsBalance()
            val available = resp["credits_available"]?.jsonPrimitive?.intOrNull
                ?: resp["credits_balance"]?.jsonPrimitive?.intOrNull
                ?: 0
            session.updateCredits(available)
            lastFetchAtByUser[uid] = System.currentTimeMillis()
            available
        }.getOrNull()
    }

    /** 清除本地积分请求缓存后，强制以服务端余额校准本机显示。 */
    suspend fun resetLocalCacheAndRefresh(): Int? {
        reset()
        return fetchBalance(force = true)
    }

    suspend fun addCreditsFromIap(
        amount: Int,
        transactionId: String,
        productId: String,
        purchaseToken: String,
        packageName: String,
        quantity: Int,
    ): Result<Int> = runCreditsCatching {
        require(session.current().isLoggedIn) { "请先登录后再充值积分" }
        require(amount > 0) { "充值积分数量异常" }
        require(transactionId.isNotBlank()) { "订单号为空" }
        require(productId.isNotBlank()) { "商品编号为空" }
        require(purchaseToken.isNotBlank()) { "购买凭据为空" }
        require(packageName.isNotBlank()) { "应用包名为空" }

        val resp = net.downloadApi.addCreditsFromIap(
            iapCreditGrantPayload(
                amount = amount,
                transactionId = transactionId,
                productId = productId,
                purchaseToken = purchaseToken,
                packageName = packageName,
                quantity = quantity,
            ),
        )
        val available = resp["credits_available"]?.jsonPrimitive?.intOrNull
            ?: resp["credits_balance"]?.jsonPrimitive?.intOrNull
            ?: resp["available_balance"]?.jsonPrimitive?.intOrNull
            ?: amount
        session.updateCredits(available)
        session.current().userId?.let { lastFetchAtByUser[it] = System.currentTimeMillis() }
        available
    }

    /** 注销时强制清缓存 */
    fun reset() {
        lastFetchAtByUser.clear()
    }
}

internal fun iapCreditGrantPayload(
    amount: Int,
    transactionId: String,
    productId: String,
    purchaseToken: String,
    packageName: String,
    quantity: Int,
): JsonObject =
    JsonObject(
        mapOf(
            "amount" to JsonPrimitive(amount),
            "transaction_id" to JsonPrimitive(transactionId),
            "product_id" to JsonPrimitive(productId),
            "purchase_token" to JsonPrimitive(purchaseToken),
            "package_name" to JsonPrimitive(packageName),
            "quantity" to JsonPrimitive(quantity.coerceAtLeast(1)),
            "platform" to JsonPrimitive("google_play"),
            "reason" to JsonPrimitive("Google Play购买"),
        ),
    )

internal suspend inline fun <T> runCreditsCatching(
    crossinline block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
