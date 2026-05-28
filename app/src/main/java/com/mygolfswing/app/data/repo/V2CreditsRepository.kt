package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
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

    @Volatile private var lastFetchAt: Long = 0L
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
        if (!force && now - lastFetchAt < cacheTtlMs) {
            return session.current().credits
        }
        return runCatching {
            val resp = net.downloadApi.creditsBalance()
            val available = resp["credits_available"]?.jsonPrimitive?.intOrNull
                ?: resp["credits_balance"]?.jsonPrimitive?.intOrNull
                ?: 0
            session.updateCredits(available)
            lastFetchAt = System.currentTimeMillis()
            available
        }.getOrNull()
    }

    /** 注销时强制清缓存 */
    fun reset() {
        lastFetchAt = 0L
    }
}
