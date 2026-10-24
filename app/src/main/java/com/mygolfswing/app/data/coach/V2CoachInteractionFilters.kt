package com.mygolfswing.app.data.coach

enum class V2CoachDeliveryListFilter {
    ACTIVE,
    FAILED,
}

private const val WAITING_FOR_WIFI_PREFIX = "大视频正在等待 Wi-Fi"

fun V2CoachDeliveryJob.isFailedQueueJob(): Boolean {
    if (pausedByUser) return true
    val message = lastError?.trim().orEmpty()
    return message.isNotEmpty() && !message.startsWith(WAITING_FOR_WIFI_PREFIX)
}

fun List<V2CoachDeliveryJob>.filterForQueue(
    filter: V2CoachDeliveryListFilter,
): List<V2CoachDeliveryJob> = when (filter) {
    V2CoachDeliveryListFilter.ACTIVE -> filterNot(V2CoachDeliveryJob::isFailedQueueJob)
    V2CoachDeliveryListFilter.FAILED -> filter(V2CoachDeliveryJob::isFailedQueueJob)
}

fun List<V2CoachQuickReply>.filterQuickReplies(query: String): List<V2CoachQuickReply> {
    val keyword = query.trim()
    if (keyword.isEmpty()) return this
    return filter { reply ->
        reply.title.contains(keyword, ignoreCase = true) ||
            reply.text.contains(keyword, ignoreCase = true)
    }
}
