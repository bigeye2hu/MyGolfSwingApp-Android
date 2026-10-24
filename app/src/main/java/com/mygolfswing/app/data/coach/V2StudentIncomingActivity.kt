package com.mygolfswing.app.data.coach

import java.time.Instant

/** Lightweight in-app route notice derived from unread or relationship changes. */
data class V2StudentIncomingActivity(
    val id: String,
    val connectionId: String,
    /** Conversation target; null means the relationship changed and the history space should open. */
    val itemId: String?,
    val coachName: String,
    val title: String,
    val detail: String,
    val hasVideo: Boolean,
)

/**
 * Mirrors iOS `StudentIncomingActivityResolver.resolveConnectionChange`: cold-start data only
 * establishes a baseline, and only a peer-initiated active -> disconnected transition surfaces.
 */
fun resolveStudentConnectionActivity(
    previous: List<V2CoachConnection>?,
    current: List<V2CoachConnection>,
    currentUserId: String?,
): V2StudentIncomingActivity? {
    previous ?: return null
    val previousStatus = previous.associate { it.connectionId to it.status }
    return current.asSequence()
        .filter { connection ->
            connection.status == "disconnected" &&
                previousStatus[connection.connectionId] == "active" &&
                connection.disconnectedByUserId != null &&
                connection.disconnectedByUserId != currentUserId
        }
        .map { connection ->
            val coachName = connection.coach?.nickname.normalizedCoachName()
            V2StudentIncomingActivity(
                id = "disconnected-${connection.connectionId}-${connection.disconnectedAt.orEmpty()}",
                connectionId = connection.connectionId,
                itemId = null,
                coachName = coachName,
                title = "已解除连接",
                detail = "历史交流仍可查看，当前空间已变为只读",
                hasVideo = false,
            ) to studentActivityInstant(connection.disconnectedAt)
        }
        .maxByOrNull { it.second }
        ?.first
}

/**
 * Mirrors iOS' cold-start-safe unread resolver: existing unread establishes a baseline,
 * while only a count increase or a newer activity on the same item raises a banner.
 */
fun resolveStudentIncomingActivity(
    previous: V2StudentUnreadSnapshot?,
    current: V2StudentUnreadSnapshot,
    coachNamesByConnection: Map<String, String> = emptyMap(),
): V2StudentIncomingActivity? {
    previous ?: return null

    val increasedConnections = current.byConnection.mapNotNullTo(mutableSetOf()) { (connectionId, count) ->
        connectionId.takeIf { count > (previous.byConnection[connectionId] ?: 0) }
    }
    val previousItems = previous.recentItems.associateBy(V2CoachWorkspaceItem::itemId)
    val candidates = current.recentItems.mapNotNull { item ->
        if (!item.isIncomingFromCoach()) return@mapNotNull null
        val previousItem = previousItems[item.itemId]
        val advanced = previousItem != null && previousItem.activityKey() != item.activityKey()
        if (item.connectionId !in increasedConnections && !advanced) return@mapNotNull null
        item.toIncomingActivity(coachNamesByConnection[item.connectionId])
    }.toMutableList()

    increasedConnections.forEach { connectionId ->
        if (candidates.any { it.connectionId == connectionId }) return@forEach
        val itemId = current.unreadItemIdByConnection[connectionId].orEmpty()
        if (itemId.isBlank()) return@forEach
        val coachName = coachNamesByConnection[connectionId].normalizedCoachName()
        candidates += V2StudentIncomingActivity(
            id = "$connectionId-$itemId-${current.byConnection[connectionId] ?: 0}-${current.refreshedAt.orEmpty()}",
            connectionId = connectionId,
            itemId = itemId,
            coachName = coachName,
            title = "发来新的教学内容",
            detail = "点击进入你们的教学交流",
            hasVideo = false,
        )
    }

    return candidates.maxByOrNull { notice ->
        current.recentItems.firstOrNull { it.itemId == notice.itemId }
            ?.let { it.latestReplyAt ?: it.latestActivityAt ?: it.createdAt }
            .orEmpty()
    }
}

private fun V2CoachWorkspaceItem.isIncomingFromCoach(): Boolean =
    (latestReplySenderRole ?: senderRole) == "coach" && !isRemovedContent

private fun V2CoachWorkspaceItem.toIncomingActivity(coachName: String?): V2StudentIncomingActivity {
    val reply = latestReplySenderRole == "coach"
    val video = if (reply) latestReplyHasVideo ?: hasVideoContent else hasVideoContent
    val noticeTitle = when {
        reply && video -> "发来一段视频反馈"
        reply -> "回复了你的练习"
        video -> "发来一段教学视频"
        else -> "发来一条教学内容"
    }
    val preview = V2CoachWorkspaceItem.visibleCopy(latestReplyText ?: text ?: title)?.take(80).orEmpty()
    return V2StudentIncomingActivity(
        id = "$connectionId-$itemId-${activityKey()}",
        connectionId = connectionId,
        itemId = itemId,
        coachName = coachName.normalizedCoachName(),
        title = noticeTitle,
        detail = preview.ifBlank { "点击进入你们的教学交流" },
        hasVideo = video,
    )
}

private fun V2CoachWorkspaceItem.activityKey(): String = listOfNotNull(
    latestReplyAt,
    latestActivityAt,
    latestReplySenderRole,
    latestReplyText,
    replyCount.toString(),
).joinToString("|")

private fun String?.normalizedCoachName(): String = this?.trim().orEmpty().ifBlank { "教练" }

private fun studentActivityInstant(value: String?): Instant =
    value?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: Instant.MIN
