package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator
import kotlinx.coroutines.CancellationException

internal fun plazaReleaseSafeError(base: String, debugDetail: String?): String {
    return plazaReleaseSafeError(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun plazaReleaseSafeError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

internal fun plazaThrowableError(base: String, error: Throwable): String {
    if (error is CancellationException) throw error
    return plazaReleaseSafeError(base, error.message)
}

internal fun shouldUsePlazaMockFallback(hasExistingVideos: Boolean, isDebuggable: Boolean): Boolean =
    !hasExistingVideos && isDebuggable
