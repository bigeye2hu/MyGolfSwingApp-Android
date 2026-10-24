package com.mygolfswing.app.data.coach

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

data class V2MemoDiscussionUpdate(
    val connectionId: String,
    val itemId: String,
    val peerName: String,
)

/**
 * Memo 列表共用的教学空间未读索引。
 *
 * 与 iOS 一致，只使用根内容的 sourceRecordId 和已经拉取到本机的未读摘要；
 * 列表卡片本身不会再为每条 Memo 发起网络请求。
 */
object V2MemoDiscussionIndex {
    private val mutableState = MutableStateFlow<Map<UUID, List<V2MemoDiscussionUpdate>>>(emptyMap())
    val state: StateFlow<Map<UUID, List<V2MemoDiscussionUpdate>>> = mutableState.asStateFlow()

    fun update(
        userId: String,
        connections: List<V2CoachConnection>,
        unread: V2StudentUnreadSnapshot,
    ) {
        mutableState.value = buildStudentMemoDiscussionIndex(userId, connections, unread)
    }

    fun clear() {
        mutableState.value = emptyMap()
    }
}

internal fun buildStudentMemoDiscussionIndex(
    userId: String,
    connections: List<V2CoachConnection>,
    unread: V2StudentUnreadSnapshot,
): Map<UUID, List<V2MemoDiscussionUpdate>> {
    if (userId.isBlank()) return emptyMap()
    val activeConnections = connections
        .filter { connection ->
            connection.status == "active" && (
                connection.relationshipType == "friend" || listOfNotNull(
                    connection.student?.userId,
                    connection.coach?.userId,
                    connection.peer?.userId,
                ).contains(userId)
            )
        }
        .associateBy(V2CoachConnection::connectionId)

    val freshestRoots = unread.recentItems
        .asSequence()
        .filter { it.parentItemId == null }
        .groupBy(V2CoachWorkspaceItem::itemId)
        .mapValues { (_, copies) -> copies.maxByOrNull(::workspaceItemFreshness)!! }

    val refreshedAt = parseCoachTimestamp(unread.refreshedAt)
    return freshestRoots.values
        .asSequence()
        .filter { !it.isRemovedContent && it.studentHasUnread }
        .mapNotNull { item ->
            val memoId = item.sourceRecordId?.let { raw -> runCatching { UUID.fromString(raw) }.getOrNull() }
                ?: return@mapNotNull null
            val connection = activeConnections[item.connectionId] ?: return@mapNotNull null
            if (
                (unread.byConnection[connection.connectionId] ?: 0) == 0 &&
                refreshedAt != null && refreshedAt >= workspaceItemFreshness(item)
            ) return@mapNotNull null
            val peerName = connection.coach
                ?.takeUnless { it.userId == userId }
                ?.nickname
                ?: connection.peer?.takeUnless { it.userId == userId }?.nickname
                ?: connection.student?.takeUnless { it.userId == userId }?.nickname
                ?: "对方"
            Triple(
                memoId,
                V2MemoDiscussionUpdate(
                    connectionId = connection.connectionId,
                    itemId = item.itemId,
                    peerName = peerName,
                ),
                workspaceItemFreshness(item),
            )
        }
        .groupBy { it.first }
        .mapValues { (_, candidates) ->
            candidates.groupBy { it.second.connectionId }
                .values
                .mapNotNull { copies -> copies.maxByOrNull { it.third }?.second }
                .sortedBy(V2MemoDiscussionUpdate::connectionId)
        }
}

private fun workspaceItemFreshness(item: V2CoachWorkspaceItem): Instant = listOfNotNull(
    item.latestActivityAt,
    item.latestReplyAt,
    item.createdAt,
    item.coachViewedAt,
    item.studentViewedAt,
    item.deletedAt,
).mapNotNull(::parseCoachTimestamp).maxOrNull() ?: Instant.MIN

private fun parseCoachTimestamp(raw: String?): Instant? {
    val value = raw?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return runCatching { Instant.parse(value) }.getOrElse {
        runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
    }
}
