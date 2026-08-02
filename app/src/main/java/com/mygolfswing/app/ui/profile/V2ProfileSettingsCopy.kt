package com.mygolfswing.app.ui.profile

import com.mygolfswing.app.data.ServiceLocator
import kotlinx.coroutines.CancellationException

internal fun profileQrGalleryError(base: String, debugDetail: String?): String {
    return profileQrGalleryError(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun profileQrGalleryError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

internal fun profileQrGalleryThrowableError(base: String, error: Throwable): String {
    if (error is CancellationException) throw error
    return profileQrGalleryError(base, error.message)
}
