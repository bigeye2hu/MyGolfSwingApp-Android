package com.mygolfswing.app.data.coach

import java.time.Instant

data class V2CoachIncomingActivity(
    val id: String,
    val connectionId: String,
    val itemId: String?,
    val studentName: String,
    val title: String,
    val detail: String,
    val hasVideo: Boolean,
)

/** Resolves a newly accepted/disconnected relationship without replaying cold-start history. */
fun resolveCoachConnectionActivity(
    previous: List<V2CoachConnection>?,
    current: List<V2CoachConnection>,
    currentUserId: String?,
): V2CoachIncomingActivity? {
    previous ?: return null
    val oldStatus = previous.associate { it.connectionId to it.status }
    return current.mapNotNull { connection ->
        val studentName = connection.student?.nickname?.trim().takeUnless { it.isNullOrEmpty() } ?: "学员"
        when {
            connection.status == "active" && oldStatus[connection.connectionId] != "active" -> {
                val eventTime = connection.acceptedAt ?: connection.createdAt
                V2CoachIncomingActivity(
                    id = "connection-${connection.connectionId}-${eventTime ?: "active"}",
                    connectionId = connection.connectionId,
                    itemId = null,
                    studentName = studentName,
                    title = "已完成连接",
                    detail = "现在可以直接发送课后总结或教学视频",
                    hasVideo = false,
                ) to coachInstant(eventTime)
            }
            connection.status == "disconnected" && oldStatus[connection.connectionId] == "active" &&
                connection.disconnectedByUserId != null && connection.disconnectedByUserId != currentUserId -> {
                V2CoachIncomingActivity(
                    id = "disconnected-${connection.connectionId}-${connection.disconnectedAt.orEmpty()}",
                    connectionId = connection.connectionId,
                    itemId = null,
                    studentName = studentName,
                    title = "已解除连接",
                    detail = "历史交流仍可查看，当前空间已变为只读",
                    hasVideo = false,
                ) to coachInstant(connection.disconnectedAt)
            }
            else -> null
        }
    }.maxByOrNull { it.second }?.first
}

/** Resolves newly arrived student roots/follow-ups from the lightweight dashboard snapshots. */
fun resolveCoachIncomingActivity(
    previous: V2CoachDashboard?,
    current: V2CoachDashboard,
    studentNamesByConnection: Map<String, String>,
): V2CoachIncomingActivity? {
    previous ?: return null
    val previousStates = previous.connectionStates.associateBy(V2CoachConnectionActivitySnapshot::connectionId)
    val currentStates = current.connectionStates.associateBy(V2CoachConnectionActivitySnapshot::connectionId)
    val increasedConnections = currentStates.mapNotNullTo(HashSet()) { (connectionId, state) ->
        connectionId.takeIf { state.newCount > (previousStates[connectionId]?.newCount ?: 0) }
    }
    val previousItems = previous.recentItems.associateBy(V2CoachWorkspaceItem::itemId)
    val candidates = mutableListOf<Pair<V2CoachIncomingActivity, Instant>>()

    current.recentItems.filter(::isCoachIncomingItem).forEach { item ->
        val oldItem = previousItems[item.itemId]
        val advanced = oldItem?.let { coachActivityKey(it) != coachActivityKey(item) } == true
        if (item.connectionId in increasedConnections || advanced) {
            candidates += makeCoachNotice(
                connectionId = item.connectionId,
                itemId = item.itemId,
                studentName = studentNamesByConnection[item.connectionId],
                hasVideo = item.latestActivityHasVideoContent,
                isFollowUp = item.latestReplySenderRole == "student",
                preview = V2CoachWorkspaceItem.visibleCopy(item.latestReplyText ?: item.text).orEmpty(),
                activityKey = coachActivityKey(item),
            ) to coachInstant(item.latestReplyAt ?: item.latestActivityAt ?: item.createdAt)
        }
    }

    increasedConnections.filterNot { connectionId ->
        candidates.any { it.first.connectionId == connectionId }
    }.forEach { connectionId ->
        val state = currentStates[connectionId] ?: return@forEach
        val itemId = state.priorityItemId ?: state.latestItemId ?: return@forEach
        candidates += makeCoachNotice(
            connectionId = connectionId,
            itemId = itemId,
            studentName = studentNamesByConnection[connectionId],
            hasVideo = state.latestHasVideo,
            isFollowUp = state.state == "student_follow_up",
            preview = V2CoachWorkspaceItem.visibleCopy(state.latestText).orEmpty(),
            activityKey = "${state.newCount}-${state.latestActivityAt.orEmpty()}",
        ) to coachInstant(state.latestActivityAt)
    }
    return candidates.maxByOrNull { it.second }?.first
}

private fun isCoachIncomingItem(item: V2CoachWorkspaceItem): Boolean =
    item.reviewState == "new" &&
        (item.latestReplySenderRole ?: item.senderRole) == "student" &&
        !item.isRemovedContent

private fun makeCoachNotice(
    connectionId: String,
    itemId: String,
    studentName: String?,
    hasVideo: Boolean,
    isFollowUp: Boolean,
    preview: String,
    activityKey: String,
): V2CoachIncomingActivity {
    val name = studentName?.trim().takeUnless { it.isNullOrEmpty() } ?: "学员"
    val title = when {
        hasVideo && isFollowUp -> "补充了一段练习视频"
        hasVideo -> "发来一段复练视频"
        isFollowUp -> "补充了一条回复"
        else -> "发来一条练习消息"
    }
    return V2CoachIncomingActivity(
        id = "$connectionId-$itemId-$activityKey",
        connectionId = connectionId,
        itemId = itemId,
        studentName = name,
        title = title,
        detail = V2CoachWorkspaceItem.visibleCopy(preview)?.take(80) ?: "点击进入教学交流",
        hasVideo = hasVideo,
    )
}

private fun coachActivityKey(item: V2CoachWorkspaceItem): String = listOfNotNull(
    item.latestReplyAt,
    item.latestActivityAt,
    item.latestReplySenderRole,
    item.latestReplyText,
    item.replyCount.toString(),
).joinToString("|")

private fun coachInstant(value: String?): Instant =
    value?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: Instant.MIN
