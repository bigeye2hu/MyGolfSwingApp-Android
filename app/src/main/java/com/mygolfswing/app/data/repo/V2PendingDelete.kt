package com.mygolfswing.app.data.repo

internal fun String?.isPendingCloudDelete(pendingDeleteIds: Set<String>): Boolean {
    val normalized = this?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return false
    return normalized in pendingDeleteIds.mapTo(mutableSetOf()) { it.trim().lowercase() }
}
