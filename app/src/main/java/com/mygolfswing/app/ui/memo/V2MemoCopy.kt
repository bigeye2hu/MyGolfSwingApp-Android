package com.mygolfswing.app.ui.memo

import com.mygolfswing.app.data.ServiceLocator
import kotlinx.coroutines.CancellationException

internal fun memoTextUserError(base: String, debugDetail: String?): String {
    return memoTextUserError(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun memoTextUserError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

internal fun memoTextThrowableError(base: String, error: Throwable): String {
    if (error is CancellationException) throw error
    return memoTextUserError(base, error.message)
}
