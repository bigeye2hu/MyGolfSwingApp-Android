package com.mygolfswing.app.data.billing

import com.android.billingclient.api.BillingClient

internal object V2BillingCopy {
    fun billingMessage(
        responseCode: Int,
        debugMessage: String?,
        fallback: String,
        isDebuggable: Boolean,
    ): String {
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
            BillingClient.BillingResponseCode.USER_CANCELED ->
                "已取消购买"
            BillingClient.BillingResponseCode.DEVELOPER_ERROR,
            BillingClient.BillingResponseCode.ERROR ->
                fallback
            else -> fallback
        }
        return releaseSafeError(base, debugMessage, isDebuggable)
    }

    fun creditGrantFailureMessage(
        causeMessage: String?,
        orderLabel: String,
        isDebuggable: Boolean,
    ): String = releaseSafeError(
        base = "购买成功但积分发放失败，请点“刷新/恢复”重试；如仍未到账，请联系客服处理订单。$orderLabel",
        debugDetail = causeMessage,
        isDebuggable = isDebuggable,
    )

    fun releaseSafeError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
        val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
        return if (detail == null) base else "$base（$detail）"
    }

    fun supportOrderLabel(orderId: String?, purchaseToken: String): String {
        val normalizedOrderId = orderId?.takeIf { it.isNotBlank() }
        return if (normalizedOrderId != null) {
            "订单号：$normalizedOrderId"
        } else {
            "订单凭据尾号：${purchaseToken.takeLast(8)}"
        }
    }
}
