package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator

internal fun aiChatUserError(base: String, debugDetail: String?): String {
    return aiChatUserError(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun aiChatUserError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

internal fun shouldUseAIChatMockProvider(requestedMockProvider: Boolean, isDebuggable: Boolean): Boolean =
    requestedMockProvider && isDebuggable
