package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator
import kotlinx.coroutines.CancellationException

internal fun swingStoreErrorMessage(base: String, debugDetail: String?): String {
    return swingStoreErrorMessage(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun swingStoreErrorMessage(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

internal fun swingStoreThrowableError(base: String, error: Throwable): String {
    if (error is CancellationException) throw error
    return swingStoreErrorMessage(base, error.message)
}
