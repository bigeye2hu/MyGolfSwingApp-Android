package com.mygolfswing.app.data.coach

import com.mygolfswing.app.data.ServiceLocator
import kotlinx.coroutines.CancellationException

/** An error whose message is intentionally safe and actionable for workspace users. */
internal open class V2CoachUserFacingException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

internal fun coachThrowableError(base: String, error: Throwable): String =
    coachThrowableError(base, error, ServiceLocator.isDebuggable)

internal fun coachThrowableError(
    base: String,
    error: Throwable,
    isDebuggable: Boolean,
): String {
    if (error is CancellationException) throw error
    if (error is V2CoachUserFacingException) return error.message?.takeIf(String::isNotBlank) ?: base
    val detail = error.message?.takeIf { isDebuggable && it.isNotBlank() }
    return if (detail == null) base else "$base（$detail）"
}
