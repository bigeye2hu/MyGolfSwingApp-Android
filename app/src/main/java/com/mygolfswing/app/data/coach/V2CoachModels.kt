package com.mygolfswing.app.data.coach

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object V2CoachShareRequestIds {
    fun upload(memoId: String, batchId: String): String = "memo_${memoId}_$batchId"
    fun post(memoId: String, batchId: String, connectionId: String): String =
        "space_share_${memoId}_${batchId}_$connectionId"
}

data class V2CoachUserSummary(
    val userId: String,
    val nickname: String,
    val avatar: String? = null,
    val handicap: Double? = null,
    val golfAge: Int? = null,
    val homeCourse: String? = null,
    val coachAuthorizationStatus: String? = null,
    val golfStartYear: Int? = null,
    val dominantHand: String? = null,
    val golfBrands: List<String> = emptyList(),
    val bio: String? = null,
)

/** Account-owned golf facts shown read-only in a coach's relationship profile. */
fun V2CoachUserSummary.coachAccountFacts(ageLabel: String? = null): List<String> = buildList {
    handicap?.let { add(String.format(Locale.CHINA, "差点 %.1f", it)) }
    when {
        golfAge != null -> add("球龄 $golfAge 年")
        golfStartYear != null -> add("$golfStartYear 年起")
    }
    ageLabel?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
    dominantHand?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
    homeCourse?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
    golfBrands.firstOrNull { it.isNotBlank() }?.trim()?.let(::add)
}.take(5)

data class V2CoachConnection(
    val connectionId: String,
    val status: String,
    val student: V2CoachUserSummary?,
    val coach: V2CoachUserSummary? = null,
    val peer: V2CoachUserSummary? = null,
    val relationshipType: String = "coach",
    val privateNote: String? = null,
    val trainingStage: String? = null,
    val primaryGoal: String? = null,
    val trainingFocus: String? = null,
    val focusTags: List<String> = emptyList(),
    val lessonMode: String? = null,
    val trainingGoalTags: List<String> = emptyList(),
    val ageTag: String? = null,
    val weeklyPracticeGoal: Int? = null,
    val cadence: String? = null,
    val communicationPreference: String? = null,
    val profileCompletedAt: String? = null,
    val category: String? = null,
    val pinned: Boolean = false,
    val followUpAt: String? = null,
    val lastActivityAt: String? = null,
    val createdAt: String? = null,
    val acceptedAt: String? = null,
    val disconnectedAt: String? = null,
    val disconnectedByUserId: String? = null,
    val perspective: String? = null,
    val trainingFocusUpdatedAt: String? = null,
    val preferencesUpdatedAt: String? = null,
) {
    val isCoachingProfileComplete: Boolean
        get() = !trainingStage.isNullOrBlank()

    val resolvedLessonMode: V2CoachLessonMode
        get() = V2CoachLessonMode.fromWireValue(lessonMode)

    /** Keeps legacy connections useful until their note is migrated into the dedicated field. */
    val resolvedTrainingFocus: String?
        get() {
            trainingFocus?.trim()?.takeIf(String::isNotEmpty)?.let { return it }
            val legacy = privateNote?.trim().orEmpty()
                .removePrefix("最近重点：")
                .removePrefix("当前重点：")
                .trim()
            return legacy.takeIf(String::isNotEmpty)
        }
}

data class V2SpacePublicSource(
    val commentId: String,
    val videoId: String,
    val authorName: String,
    val commentCreatedAt: Double,
    val available: Boolean,
    val quotedText: String,
)

data class V2SpaceDirectMessage(
    val messageId: String,
    val connectionId: String,
    val senderUserId: String,
    val text: String,
    val createdAt: String?,
    val isMine: Boolean,
    val type: String?,
    val source: V2SpacePublicSource? = null,
)

data class V2SpaceConversationPage(
    val messages: List<V2SpaceDirectMessage>,
    val hasUnread: Boolean,
    val readPosition: String,
    val nextCursor: String?,
    val latestCursor: String?,
    val hasMoreNew: Boolean,
)

fun sortStudentSpaces(
    connections: List<V2CoachConnection>,
    unreadByConnection: Map<String, Int>,
    latestActivityByConnection: Map<String, String> = emptyMap(),
): List<V2CoachConnection> = connections.sortedWith(
    compareByDescending<V2CoachConnection> { it.pinned }
        .thenByDescending {
            workspaceActivitySortEpoch(
                latestActivityByConnection[it.connectionId]
                    ?: it.lastActivityAt
                    ?: it.acceptedAt
                    ?: it.createdAt,
            )
        }
        .thenByDescending { unreadByConnection[it.connectionId] ?: 0 }
        .thenBy(V2CoachConnection::connectionId),
)

fun sortWorkspaceSwitcherConnections(
    connections: List<V2CoachConnection>,
    currentConnectionId: String,
): List<V2CoachConnection> = connections.asSequence()
    .filter { it.status == "active" }
    .sortedWith(
        compareByDescending<V2CoachConnection> { it.connectionId == currentConnectionId }
            .thenByDescending(V2CoachConnection::pinned)
            .thenByDescending {
                workspaceActivitySortEpoch(it.lastActivityAt ?: it.acceptedAt ?: it.createdAt)
            }
            .thenBy(V2CoachConnection::connectionId),
    )
    .toList()

fun latestStudentSpaceActivityByConnection(
    items: List<V2CoachWorkspaceItem>,
): Map<String, String> = latestStudentSpaceItemsByConnection(items).mapValues { (_, item) ->
    item.latestReplyAt ?: item.latestActivityAt ?: item.createdAt.orEmpty()
}

fun latestStudentSpaceItemsByConnection(
    items: List<V2CoachWorkspaceItem>,
): Map<String, V2CoachWorkspaceItem> = items.asSequence()
    .filter { it.parentItemId == null && !it.isRemovedContent }
    .groupBy(V2CoachWorkspaceItem::connectionId)
    .mapValues { (_, values) ->
        values.maxByOrNull { item ->
            workspaceActivitySortEpoch(item.latestReplyAt ?: item.latestActivityAt ?: item.createdAt)
        }!!
    }

data class V2WorkspaceContentCounts(
    val practice: Int,
    val watching: Int,
)

/** Deduplicates the same root returned by dashboard/review previews before counting like iOS. */
fun workspaceContentCounts(
    items: List<V2CoachWorkspaceItem>,
    connectionId: String,
): V2WorkspaceContentCounts {
    val roots = LinkedHashMap<String, V2CoachWorkspaceItem>()
    items.asSequence()
        .filter { it.connectionId == connectionId && it.parentItemId == null && !it.isRemovedContent }
        .forEach { roots[it.itemId] = it }
    return V2WorkspaceContentCounts(
        practice = roots.values.count { it.contentMode == V2WorkspaceContentMode.PRACTICE },
        watching = roots.values.count { it.contentMode == V2WorkspaceContentMode.WATCHING },
    )
}

private fun workspaceActivitySortEpoch(value: String?): Long {
    if (value.isNullOrBlank()) return Long.MIN_VALUE
    return runCatching { java.time.OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        .recoverCatching { java.time.Instant.parse(value).toEpochMilli() }
        .getOrDefault(Long.MIN_VALUE)
}

fun sortCoachStudentConnections(
    connections: List<V2CoachConnection>,
    dashboard: V2CoachDashboard,
    draftConnectionIds: Set<String> = emptySet(),
    nowEpochMillis: Long = System.currentTimeMillis(),
): List<V2CoachConnection> {
    val latestItemEpochByConnection = latestStudentSpaceItemsByConnection(
        dashboard.recentItems + dashboard.studentPreviewItems,
    ).mapValues { (_, item) ->
        workspaceActivitySortEpoch(item.latestReplyAt ?: item.latestActivityAt ?: item.createdAt)
    }
    val activityStateByConnection = dashboard.connectionStates.associateBy(V2CoachConnectionActivitySnapshot::connectionId)

    fun priority(connection: V2CoachConnection): Int = when {
        connection.status != "active" -> 4
        connection.connectionId in draftConnectionIds -> 0
        (dashboard.newByConnection[connection.connectionId] ?: 0) > 0 -> 1
        (dashboard.pendingByConnection[connection.connectionId] ?: 0) > 0 -> 2
        else -> 3
    }

    fun followUpPriority(connection: V2CoachConnection): Int {
        val epoch = workspaceActivitySortEpoch(connection.followUpAt)
        return when {
            epoch == Long.MIN_VALUE -> 2
            epoch <= nowEpochMillis -> 0
            else -> 1
        }
    }

    fun activityEpoch(connection: V2CoachConnection): Long = maxOf(
        latestItemEpochByConnection[connection.connectionId] ?: Long.MIN_VALUE,
        workspaceActivitySortEpoch(activityStateByConnection[connection.connectionId]?.latestActivityAt),
        workspaceActivitySortEpoch(connection.lastActivityAt ?: connection.acceptedAt ?: connection.createdAt),
    )

    return connections.withIndex().sortedWith { left, right ->
        compareValues(priority(left.value), priority(right.value)).takeIf { it != 0 }
            ?: compareValues(right.value.pinned, left.value.pinned).takeIf { it != 0 }
            ?: compareValues(followUpPriority(left.value), followUpPriority(right.value)).takeIf { it != 0 }
            ?: compareValues(activityEpoch(right.value), activityEpoch(left.value)).takeIf { it != 0 }
            ?: compareValues(left.index, right.index)
    }.map(IndexedValue<V2CoachConnection>::value)
}

fun sortWorkspaceItems(
    items: List<V2CoachWorkspaceItem>,
    newestFirst: Boolean,
): List<V2CoachWorkspaceItem> {
    val comparator = compareBy<V2CoachWorkspaceItem> { it.latestActivityAt ?: it.createdAt.orEmpty() }
    return if (newestFirst) items.sortedWith(comparator.reversed()) else items.sortedWith(comparator)
}

fun isCoachHomeQuestion(item: V2CoachWorkspaceItem): Boolean {
    if (item.isRemovedContent) return false
    if (item.hasStudentFollowUp == true) return true
    val senderRole = item.latestReplySenderRole ?: item.senderRole
    if (senderRole != "student") return false
    val text = V2CoachWorkspaceItem.visibleCopy(item.latestReplyText ?: item.text).orEmpty()
    if (text.isEmpty()) return false
    return listOf("？", "?", "请问", "怎么", "如何", "为什么", "能不能", "可以吗", "是不是", "对吗", "帮我", "看看", "教练")
        .any { signal -> text.contains(signal, ignoreCase = true) }
}

fun coachHomePriorityItems(
    items: List<V2CoachWorkspaceItem>,
    scopedConnectionIds: Set<String>,
    limit: Int = 5,
): List<V2CoachWorkspaceItem> = items.asSequence()
    .filter { item ->
        item.parentItemId == null &&
            !item.isRemovedContent &&
            item.connectionId in scopedConnectionIds &&
            item.reviewState != "responded" &&
            (item.requiresCoachReview == true || item.hasStudentFollowUp == true ||
                item.latestReplySenderRole == "student")
    }
    .distinctBy(V2CoachWorkspaceItem::itemId)
    .sortedWith(
        compareByDescending<V2CoachWorkspaceItem>(::isCoachHomeQuestion)
            .thenByDescending {
                workspaceActivitySortEpoch(
                    it.reviewWaitingSince ?: it.latestReplyAt ?: it.latestActivityAt ?: it.createdAt,
                )
            }
            .thenBy(V2CoachWorkspaceItem::itemId),
    )
    .take(limit.coerceAtLeast(0))
    .toList()

fun coachHomeVisibleConnections(
    connections: List<V2CoachConnection>,
    connectionStates: List<V2CoachConnectionActivitySnapshot>,
    priorityItems: List<V2CoachWorkspaceItem>,
    limit: Int = 4,
): List<V2CoachConnection> {
    val busyConnectionIds = priorityItems.take(2).mapTo(mutableSetOf(), V2CoachWorkspaceItem::connectionId)
    val active = connections.filter { it.status == "active" }
    val remaining = active.filterNot { it.connectionId in busyConnectionIds }
    val source = remaining.ifEmpty { active }
    val states = connectionStates.associateBy(V2CoachConnectionActivitySnapshot::connectionId)
    return source.sortedWith(
        compareByDescending<V2CoachConnection> {
            workspaceActivitySortEpoch(states[it.connectionId]?.lastPracticeAt)
        }.thenByDescending {
            states[it.connectionId]?.weeklyPracticeCount ?: 0
        }.thenBy {
            it.student?.nickname.orEmpty()
        },
    ).take(limit.coerceAtLeast(0))
}

data class V2CoachProfileInput(
    val privateNote: String,
    val pinned: Boolean,
    val category: String?,
    val followUpAt: String?,
    val trainingStage: String,
    val primaryGoal: String,
    val trainingFocus: String,
    val focusTags: List<String>,
    val lessonMode: V2CoachLessonMode,
    val trainingGoals: List<V2CoachTrainingGoal>,
    val studentAge: V2CoachStudentAge?,
    val weeklyPracticeGoal: Int?,
    val cadence: String?,
    val communicationPreference: String?,
) {
    val isComplete: Boolean
        get() = trainingStage.isNotBlank()
}

enum class V2CoachLessonMode(val wireValue: String, val title: String) {
    FIXED("fixed", "固定上课"),
    FLEXIBLE("flexible", "灵活上课"),
    SINGLE("single", "单次指导");

    companion object {
        fun fromWireValue(value: String?): V2CoachLessonMode =
            entries.firstOrNull { it.wireValue == value } ?: FIXED
    }
}

enum class V2CoachLessonScope(val title: String, val lessonMode: V2CoachLessonMode?) {
    ALL("全部", null),
    FIXED(V2CoachLessonMode.FIXED.title, V2CoachLessonMode.FIXED),
    FLEXIBLE(V2CoachLessonMode.FLEXIBLE.title, V2CoachLessonMode.FLEXIBLE),
    SINGLE(V2CoachLessonMode.SINGLE.title, V2CoachLessonMode.SINGLE),
}

enum class V2CoachTrainingGoal(val wireValue: String, val title: String) {
    BEGINNER("beginner", "入门学习"),
    BREAK_100_90("break_100_90", "破百 / 破90"),
    BREAK_80("break_80", "破80"),
}

enum class V2CoachStudentAge(val wireValue: String, val title: String) {
    ADULT("adult", "成人"),
    YOUTH("youth", "青少年"),
}

enum class V2CoachConnectionStatus(val wireValue: String, val title: String) {
    ACTIVE("active", "正在连接"),
    HISTORY("disconnected", "历史连接"),
}

fun V2CoachConnection.matchesCoachFilters(
    lessonScope: V2CoachLessonScope,
    trainingGoal: V2CoachTrainingGoal? = null,
    studentAge: V2CoachStudentAge? = null,
    connectionStatus: V2CoachConnectionStatus? = null,
    query: String = "",
): Boolean {
    if (lessonScope.lessonMode != null && resolvedLessonMode != lessonScope.lessonMode) return false
    if (trainingGoal != null && trainingGoal.wireValue !in trainingGoalTags) return false
    if (studentAge != null && ageTag != studentAge.wireValue) return false
    if (connectionStatus == V2CoachConnectionStatus.ACTIVE && status != "active") return false
    if (connectionStatus == V2CoachConnectionStatus.HISTORY && status == "active") return false
    val needle = query.trim()
    if (needle.isEmpty()) return true
    return listOfNotNull(
        student?.nickname,
        student?.userId,
        privateNote,
        trainingStage,
        primaryGoal,
        resolvedTrainingFocus,
        category,
    ).any { it.contains(needle, ignoreCase = true) }
}

fun V2CoachConnection.hasCoachPendingWork(
    dashboard: V2CoachDashboard,
    draftConnectionIds: Set<String> = emptySet(),
): Boolean {
    if (status != "active") return false
    val activity = dashboard.connectionStates.firstOrNull { it.connectionId == connectionId }
    val pendingCount = activity?.pendingCount
        ?: dashboard.pendingByConnection[connectionId]
        ?: 0
    return pendingCount > 0 || connectionId in draftConnectionIds
}

fun coachStudentConversationPreview(
    activity: V2CoachConnectionActivitySnapshot?,
    item: V2CoachWorkspaceItem?,
): String {
    V2CoachWorkspaceItem.visibleCopy(activity?.latestText)?.let { return it }
    V2CoachWorkspaceItem.visibleCopy(item?.latestReplyText)?.let { return it }
    return when (item?.latestReplySenderRole) {
        "student" -> if (item.latestActivityHasVideoContent) "学员发来一段视频" else "学员补充了内容"
        "coach" -> if (item.latestActivityHasVideoContent) "你发出一段视频" else "你回复了学员"
        else -> V2CoachWorkspaceItem.visibleCopy(item?.text)
            ?: V2CoachWorkspaceItem.visibleCopy(item?.title)
            ?: if (item?.hasVideoContent == true || activity?.latestHasVideo == true) "视频交流" else ""
    }
}

/** The compact summary used by the coach home grid, matching iOS activity-state wording. */
fun coachHomeStudentRecentSummary(
    activity: V2CoachConnectionActivitySnapshot?,
    item: V2CoachWorkspaceItem?,
    profileComplete: Boolean,
): String {
    val fallback = if (profileComplete) "等待首次练习" else "教学档案待完善"
    if (activity == null) return coachStudentConversationPreview(null, item).ifBlank { fallback }
    val preview = V2CoachWorkspaceItem.visibleCopy(activity.latestText).orEmpty()
    return when (activity.state) {
        "new" -> preview.ifBlank { "学员发来新内容" }
        "student_follow_up" -> preview.ifBlank { "学员继续回复了这条交流" }
        "viewed" -> preview.ifBlank { "已经查看，等待回复" }
        "coach_sent_unread" -> "已回复 · 学员未查看"
        "coach_sent_viewed" -> "已回复 · 学员已查看"
        else -> preview.ifBlank { fallback }
    }
}

fun coachStudentActivityLine(
    connection: V2CoachConnection,
    activity: V2CoachConnectionActivitySnapshot?,
    priorityItem: V2CoachWorkspaceItem?,
    hasDraft: Boolean,
    failedQueuedCount: Int,
): String {
    val base = when {
        connection.status != "active" -> "连接已解除 · 历史只读"
        failedQueuedCount > 0 -> "${failedQueuedCount}条内容发送失败，点开后继续"
        hasDraft -> if (priorityItem?.latestActivityHasVideoContent == true) {
            "视频点评草稿未发送"
        } else {
            "点评草稿未发送"
        }
        priorityItem?.reviewState == "new" -> if (priorityItem.latestActivityHasVideoContent) {
            "提交了新的练习视频"
        } else {
            "提交了新的文字反馈"
        }
        priorityItem != null && (activity?.pendingCount ?: 0) > 0 -> {
            val preview = coachStudentConversationPreview(activity = null, item = priorityItem)
            if (preview.isBlank()) "练习已查看，等待你回复" else "${preview}已查看，等待你回复"
        }
        (activity?.newCount ?: 0) > 0 -> if (activity?.latestHasVideo == true) {
            "提交了新的练习视频"
        } else {
            "提交了新的文字反馈"
        }
        else -> coachStudentConversationPreview(activity, priorityItem).ifBlank {
            if (!connection.isCoachingProfileComplete) "教学档案待完善" else "已建立连接，等待首次练习"
        }
    }
    val hasPrimaryTask = hasDraft || (activity?.newCount ?: 0) > 0 || (activity?.pendingCount ?: 0) > 0
    val extraTodoCount = if (hasPrimaryTask) ((activity?.pendingCount ?: 0) - 1).coerceAtLeast(0) else 0
    return if (extraTodoCount > 0) "$base · 另有${extraTodoCount}项待办" else base
}

const val V2_COACH_PROFILE_PROMPT_DEFERRAL_MILLIS = 24L * 60L * 60L * 1_000L

fun shouldPresentCoachProfilePrompt(deferredAtMillis: Long?, nowMillis: Long): Boolean =
    deferredAtMillis == null || nowMillis - deferredAtMillis >= V2_COACH_PROFILE_PROMPT_DEFERRAL_MILLIS

fun coachStudentRelativeTime(
    raw: String?,
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    val instant = raw?.let { value ->
        runCatching { OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { Instant.parse(value) }
            .getOrNull()
    } ?: return ""
    val seconds = java.time.Duration.between(instant, now).seconds.coerceAtLeast(0)
    val date = instant.atZone(zoneId)
    val current = now.atZone(zoneId)
    return when {
        seconds < 60 -> "刚刚"
        seconds < 3_600 -> "${seconds / 60}分钟前"
        date.toLocalDate() == current.toLocalDate() -> "${(seconds / 3_600).coerceAtLeast(1)}小时前"
        date.toLocalDate() == current.toLocalDate().minusDays(1) ->
            date.format(DateTimeFormatter.ofPattern("'昨天' HH:mm", Locale.CHINA))
        seconds < 7 * 86_400 -> "${(seconds / 86_400).coerceAtLeast(2)}天前"
        date.year == current.year -> date.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))
        else -> date.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA))
    }
}

enum class V2CoachReviewCardTone {
    ERROR,
    ATTENTION,
    ACTIVE,
    COMPLETE,
}

data class V2CoachReviewCardPresentation(
    val studentName: String,
    val studentAvatar: String?,
    val pinned: Boolean,
    val metadata: String,
    val coachingFocus: String?,
    val statusTitle: String,
    val tone: V2CoachReviewCardTone,
    val originalText: String,
    val latestExchangeText: String,
    val respondedSenderLabel: String,
    val compactResponded: Boolean,
    val footerText: String,
)

fun coachReviewCardPresentation(
    item: V2CoachWorkspaceItem,
    connection: V2CoachConnection?,
    hasLocalDraft: Boolean,
    deliveryJob: V2CoachDeliveryJob?,
    now: Instant = Instant.now(),
): V2CoachReviewCardPresentation {
    val failedDelivery = deliveryJob?.lastError?.isNotBlank() == true
    val workingDelivery = deliveryJob != null && !failedDelivery && !deliveryJob.pausedByUser
    val statusTitle = when {
        failedDelivery -> "发送失败"
        deliveryJob?.pausedByUser == true -> "已暂停"
        workingDelivery -> "发送中"
        hasLocalDraft -> "草稿"
        item.reviewOverdue == true -> "已超24小时"
        item.hasStudentFollowUp == true -> "学员追问"
        item.reviewState == "responded" -> "已回复"
        item.reviewState == "viewed" -> "已查看"
        else -> "新内容"
    }
    val tone = when {
        failedDelivery -> V2CoachReviewCardTone.ERROR
        deliveryJob?.pausedByUser == true || hasLocalDraft || item.reviewOverdue == true ||
            item.hasStudentFollowUp == true || item.reviewState == "new" -> V2CoachReviewCardTone.ATTENTION
        workingDelivery || item.reviewState == "viewed" -> V2CoachReviewCardTone.ACTIVE
        else -> V2CoachReviewCardTone.COMPLETE
    }
    val title = V2CoachWorkspaceItem.visibleCopy(item.title).orEmpty()
    val date = coachStudentRelativeTime(
        item.reviewWaitingSince ?: item.latestActivityAt ?: item.createdAt,
        now = now,
    )
    val metadata = listOf(date, title).filter(String::isNotBlank).joinToString(" · ")
    val originalText = V2CoachWorkspaceItem.visibleCopy(item.text)
        ?: title.takeIf(String::isNotEmpty)
        ?: if (item.hasVideoContent) "学员提交了一段练习视频" else "新的练习内容"
    val latestReply = V2CoachWorkspaceItem.visibleCopy(item.latestReplyText).orEmpty()
    val latestExchangeText = when {
        failedDelivery -> "有回复发送失败，可进入交流后重试"
        deliveryJob?.pausedByUser == true -> "回复发送已暂停，可进入交流后继续"
        workingDelivery -> "回复正在发送"
        hasLocalDraft -> "本机回复草稿待继续"
        latestReply.isNotEmpty() -> {
            val sender = if (item.latestReplySenderRole == "student") "学员" else "你"
            "$sender：$latestReply"
        }
        else -> originalText
    }
    val compactResponded = item.reviewState == "responded" && !hasLocalDraft && deliveryJob == null
    val footerText = when {
        failedDelivery -> "进入交流并重试"
        workingDelivery || deliveryJob?.pausedByUser == true -> "查看发送进度"
        hasLocalDraft -> "继续编写回复"
        item.reviewState == "responded" -> "查看交流记录"
        else -> "点击进入交流"
    }
    return V2CoachReviewCardPresentation(
        studentName = connection?.student?.nickname?.takeIf(String::isNotBlank)
            ?: item.sender?.nickname?.takeIf(String::isNotBlank)
            ?: "挥呗学员",
        studentAvatar = connection?.student?.avatar ?: item.sender?.avatar,
        pinned = connection?.pinned == true,
        metadata = metadata,
        coachingFocus = (item.coachingFocus ?: connection?.resolvedTrainingFocus)
            ?.trim()?.takeIf(String::isNotEmpty),
        statusTitle = statusTitle,
        tone = tone,
        originalText = originalText,
        latestExchangeText = latestExchangeText,
        respondedSenderLabel = if (item.latestReplySenderRole == "student") "学员" else "你",
        compactResponded = compactResponded,
        footerText = footerText,
    )
}

enum class V2CoachReviewFocus(val title: String) {
    ALL("全部待回复"),
    NEW("只看新内容"),
    VIEWED("只看已查看"),
    DRAFT("只看有草稿"),
}

fun coachPendingReviewItems(
    queueItems: List<V2CoachWorkspaceItem>,
    draftCandidates: List<V2CoachWorkspaceItem>,
    draftItemIds: Set<String>,
    focus: V2CoachReviewFocus,
): List<V2CoachWorkspaceItem> {
    val itemsById = linkedMapOf<String, V2CoachWorkspaceItem>()
    queueItems.filter { it.parentItemId == null && !it.isRemovedContent }
        .forEach { itemsById[it.itemId] = it }
    draftCandidates.filter { it.parentItemId == null && !it.isRemovedContent && it.itemId in draftItemIds }
        .forEach { itemsById[it.itemId] = it }
    return itemsById.values.asSequence()
        .filter { item ->
            val hasDraft = item.itemId in draftItemIds
            when (focus) {
                V2CoachReviewFocus.ALL -> true
                V2CoachReviewFocus.NEW -> item.reviewState == "new" && !hasDraft
                V2CoachReviewFocus.VIEWED -> item.reviewState == "viewed" && !hasDraft
                V2CoachReviewFocus.DRAFT -> hasDraft
            }
        }
        .sortedWith(
            compareByDescending<V2CoachWorkspaceItem> { it.itemId in draftItemIds }
                .thenBy { if (it.reviewState == "new") 0 else 1 }
                .thenBy {
                    workspaceActivitySortEpoch(
                        it.reviewWaitingSince ?: it.latestReplyAt ?: it.latestActivityAt ?: it.createdAt,
                    )
                }
                .thenBy(V2CoachWorkspaceItem::itemId),
        )
        .toList()
}

data class V2CoachInvitePreview(
    val inviteId: String,
    val expiresAt: String? = null,
    val coach: V2CoachUserSummary,
)

data class V2PendingCoachInvite(
    val inviteId: String,
    val inviteToken: String,
    val inviteCode: String? = null,
)

data class V2CoachWorkspaceItem(
    val itemId: String,
    val connectionId: String,
    val senderUserId: String? = null,
    val senderRole: String? = null,
    val kind: String? = null,
    val parentItemId: String? = null,
    val replyToItemId: String? = null,
    val replyAtSeconds: Double? = null,
    val title: String? = null,
    val text: String? = null,
    val createdAt: String? = null,
    val latestActivityAt: String? = null,
    val latestReplyAt: String? = null,
    val reviewState: String? = null,
    val videoUrl: String? = null,
    val coverUrl: String? = null,
    val durationSeconds: Double? = null,
    val hasVideo: Boolean? = null,
    val primaryAsset: V2CoachWorkspaceAsset? = null,
    val attachments: List<V2CoachWorkspaceAsset> = emptyList(),
    val replyCount: Int = 0,
    val latestReplyText: String? = null,
    val latestReplySenderRole: String? = null,
    val latestReplyHasVideo: Boolean? = null,
    val studentHasUnread: Boolean = false,
    val deletedAt: String? = null,
    val isMine: Boolean = false,
    val coachingFocus: String? = null,
    val fileSizeBytes: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val requiresCoachReview: Boolean? = null,
    val coachViewedAt: String? = null,
    val coachRespondedAt: String? = null,
    val coachLastViewedReplyId: String? = null,
    val studentLastViewedReplyId: String? = null,
    val studentViewedAt: String? = null,
    val latestReplyCoverUrl: String? = null,
    val latestReplyDurationSeconds: Double? = null,
    val sender: V2CoachUserSummary? = null,
    val reviewWaitingSince: String? = null,
    val hasStudentFollowUp: Boolean? = null,
    val reviewOverdue: Boolean? = null,
    val sourceRecordId: String? = null,
) {
    companion object {
        /** Mirrors iOS: server deletion placeholders are not user-visible conversation copy. */
        fun visibleCopy(raw: String?): String? = raw?.trim()
            ?.takeIf { it.isNotEmpty() && it !in setOf("内容已删除", "原内容已删除") }
    }

    val isRemovedContent: Boolean
        get() = deletedAt != null || title?.trim() in setOf("内容已删除", "原内容已删除")

    val resolvedKind: String
        get() = kind ?: if (senderRole == "coach") "coach_post" else "student_practice"

    val contentMode: V2WorkspaceContentMode
        get() = if (resolvedKind == "student_practice") {
            V2WorkspaceContentMode.PRACTICE
        } else {
            V2WorkspaceContentMode.WATCHING
        }

    fun matchesContentMode(mode: V2WorkspaceContentMode): Boolean =
        parentItemId == null && !isRemovedContent && contentMode == mode

    val hasVideoContent: Boolean
        get() = (primaryAsset != null && !primaryAsset.isVoice) ||
            attachments.any { !it.isVoice } || hasVideo == true || videoUrl != null || coverUrl != null ||
            durationSeconds != null || fileSizeBytes != null || width != null || height != null

    val latestActivityHasVideoContent: Boolean
        get() = if (latestReplySenderRole == null) hasVideoContent
        else latestReplyHasVideo ?: hasVideoContent

    val latestActivityCoverUrl: String?
        get() = if (latestReplySenderRole == null) {
            primaryAsset?.coverUrl ?: coverUrl
        } else {
            latestReplyCoverUrl ?: if (latestReplyHasVideo == null) primaryAsset?.coverUrl ?: coverUrl else null
        }

    val latestActivityDurationSeconds: Double?
        get() = if (latestReplySenderRole == null) durationSeconds
        else latestReplyDurationSeconds.takeIf { latestReplyHasVideo == true }
}

fun workspaceItemStatusText(
    item: V2CoachWorkspaceItem,
    coachPerspective: Boolean,
    friendSpace: Boolean,
): String? {
    if (friendSpace) return if (item.studentHasUnread) "新互动" else null
    if (!coachPerspective) {
        if (item.studentHasUnread) {
            return when {
                item.resolvedKind == "coach_post" && item.latestReplySenderRole == "coach" -> "新回复"
                item.resolvedKind == "coach_post" -> "新内容"
                else -> "教练新回复"
            }
        }
        if (item.resolvedKind == "coach_post") {
            if (item.latestReplySenderRole == "student" && item.reviewState != "responded") {
                return if (item.reviewState == "viewed") "教练已看" else "等待教练"
            }
            return "教练分享"
        }
        return when (item.reviewState) {
            "new" -> "等待教练"
            "viewed" -> "教练已看"
            "responded" -> "已回复"
            else -> null
        }
    }

    if (item.resolvedKind == "coach_post") {
        if (item.latestReplySenderRole == "student" && item.reviewState != "responded") {
            return if (item.reviewState == "viewed") "回复已看" else "学员新回复"
        }
        return if (item.studentHasUnread) "等待学员查看" else "学员已查看"
    }
    return when (item.reviewState) {
        "new" -> "新内容"
        "viewed" -> "待回复"
        "responded" -> if (item.studentHasUnread) "等待学员查看" else "学员已查看"
        else -> null
    }
}

enum class V2WorkspaceContentMode(val wireValue: String, val title: String) {
    PRACTICE("practice", "在练"),
    WATCHING("watching", "在看"),
}

/** Only a learner in a coach relationship can explicitly create a mandatory review task. */
fun workspaceRequiresCoachReview(
    coachPerspective: Boolean,
    relationshipType: String,
    requested: Boolean,
): Boolean = !coachPerspective && relationshipType != "friend" && requested

data class V2CoachWorkspaceAsset(
    val assetId: String,
    val sourceKind: String = "album",
    val title: String = "",
    val videoUrl: String? = null,
    val coverUrl: String? = null,
    val durationSeconds: Double? = null,
    val savedToLibrary: Boolean = false,
    val ownerUserId: String? = null,
    val tags: List<String> = emptyList(),
    val fileSizeBytes: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val useCount: Int? = null,
    val playbackCount: Int? = null,
    val uploadDurationSeconds: Double? = null,
    val createdAt: String? = null,
    val lastUsedAt: String? = null,
) {
    val isVoice: Boolean
        get() = sourceKind.lowercase() in setOf("voice", "audio", "voice_recording")
}

data class V2CoachThread(
    val item: V2CoachWorkspaceItem,
    val replies: List<V2CoachWorkspaceItem> = emptyList(),
    val version: String? = null,
    val checkVersion: String? = null,
    val nextCursor: String? = null,
) {
    fun discussionResourceEntries(): List<V2CoachDiscussionResource> {
        val entries = linkedMapOf<String, V2CoachDiscussionResource>()
        replies.filterNot(V2CoachWorkspaceItem::isRemovedContent).forEach { reply ->
            (listOfNotNull(reply.primaryAsset) + reply.attachments)
                .filter { asset -> !asset.isVoice && asset.savedToLibrary }
                .forEach { asset ->
                    val existing = entries[asset.assetId]
                    entries[asset.assetId] = if (existing == null) {
                        V2CoachDiscussionResource(asset = asset, messageIds = listOf(reply.itemId))
                    } else {
                        existing.copy(messageIds = (existing.messageIds + reply.itemId).distinct())
                    }
                }
        }
        return entries.values.toList()
    }

    /** Mirrors iOS: only explicitly saved/referenced reply media belongs in “资料”. */
    fun discussionResources(): List<V2CoachWorkspaceAsset> =
        discussionResourceEntries().map(V2CoachDiscussionResource::asset)
}

data class V2CoachDiscussionResource(
    val asset: V2CoachWorkspaceAsset,
    val messageIds: List<String>,
)

/**
 * Keeps optimistic local sends while accepting newer server state. Remote copies win for the
 * same reply so deletion and receipt updates cannot be resurrected by an older local object.
 */
fun mergeWorkspaceThreads(
    remote: V2CoachThread,
    local: V2CoachThread?,
    pagingOlder: Boolean = false,
): V2CoachThread {
    if (local == null) return remote
    val remoteActivity = coachTimestamp(remote.item.latestActivityAt ?: remote.item.createdAt)
    val localActivity = coachTimestamp(local.item.latestActivityAt ?: local.item.createdAt)
    val root = when {
        remote.item.deletedAt != null -> remote.item
        remoteActivity > localActivity -> remote.item
        remoteActivity == localActivity && coachReviewRank(remote.item.reviewState) >=
            coachReviewRank(local.item.reviewState) -> remote.item
        else -> local.item
    }
    val repliesById = LinkedHashMap<String, V2CoachWorkspaceItem>()
    local.replies.forEach { repliesById[it.itemId] = it }
    remote.replies.forEach { repliesById[it.itemId] = it }
    return V2CoachThread(
        item = root,
        replies = repliesById.values.sortedBy { coachTimestamp(it.createdAt) },
        version = remote.version,
        checkVersion = remote.checkVersion,
        nextCursor = if (
            pagingOlder || local.replies.none { old -> remote.replies.any { it.itemId == old.itemId } }
        ) remote.nextCursor else local.nextCursor,
    )
}

private fun coachTimestamp(value: String?): Instant =
    value?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: Instant.MIN

private fun coachReviewRank(value: String?): Int = when (value) {
    "responded" -> 2
    "viewed" -> 1
    else -> 0
}

data class V2WorkspaceFeedUpdate(
    /** Null means the server confirmed that the caller's version is still current. */
    val items: List<V2CoachWorkspaceItem>?,
    val nextCursor: String?,
    val version: String?,
)

/** Refreshes the first page without throwing away older pages already reached by the user. */
fun mergeWorkspaceFeedPage(
    current: List<V2CoachWorkspaceItem>,
    incoming: List<V2CoachWorkspaceItem>,
    firstPage: Boolean,
): List<V2CoachWorkspaceItem> {
    val incomingIds = incoming.mapTo(HashSet(), V2CoachWorkspaceItem::itemId)
    val retained = current.filterNot { it.itemId in incomingIds }
    return if (firstPage) incoming + retained else retained + incoming
}

data class V2CoachLibraryAsset(
    val assetId: String,
    val title: String,
    val videoUrl: String? = null,
    val coverUrl: String? = null,
    val durationSeconds: Double? = null,
    val tags: List<String> = emptyList(),
    val useCount: Int = 0,
    val createdAt: String? = null,
    val lastUsedAt: String? = null,
    val updatedAt: String? = null,
    val ownerUserId: String? = null,
    val sourceKind: String = "album",
    val fileSizeBytes: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val playbackCount: Int? = null,
    val uploadDurationSeconds: Double? = null,
)

data class V2CoachQuickReply(
    val quickReplyId: String,
    val title: String = "",
    val text: String,
    val useCount: Int = 0,
    val lastUsedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class V2CoachUploadGrant(
    val itemId: String,
    val assetId: String,
    val uploadUrl: String,
    val authorization: String,
    val securityToken: String? = null,
    val cosFileId: String? = null,
    val cloudPath: String,
    val fileId: String? = null,
    val contentType: String,
    val expiresAt: String? = null,
    val draftExpiresAt: String? = null,
    val coverUploadUrl: String? = null,
    val coverAuthorization: String? = null,
    val coverSecurityToken: String? = null,
    val coverCosFileId: String? = null,
    val coverCloudPath: String? = null,
    val coverFileId: String? = null,
)

data class V2CoachDashboard(
    val activeStudentCount: Int = 0,
    val newCount: Int = 0,
    val viewedCount: Int = 0,
    val respondedCount: Int = 0,
    val pendingCount: Int = 0,
    val overduePendingCount: Int = 0,
    val recentItems: List<V2CoachWorkspaceItem> = emptyList(),
    val refreshedAt: String? = null,
    val newByConnection: Map<String, Int> = emptyMap(),
    val pendingByConnection: Map<String, Int> = emptyMap(),
    val pendingItemIdByConnection: Map<String, String> = emptyMap(),
    val overdueByConnection: Map<String, Int> = emptyMap(),
    val oldestPendingAt: String? = null,
    val connectionStates: List<V2CoachConnectionActivitySnapshot> = emptyList(),
    val studentPreviewItems: List<V2CoachWorkspaceItem> = emptyList(),
)

data class V2CoachDashboardUpdate(
    val snapshot: V2CoachDashboard?,
    val version: String?,
)

data class V2CoachConnectionActivitySnapshot(
    val connectionId: String,
    val state: String,
    val newCount: Int = 0,
    val pendingCount: Int = 0,
    val overduePendingCount: Int = 0,
    val weeklyPracticeCount: Int? = null,
    val lastPracticeAt: String? = null,
    val sharedContentCount: Int? = null,
    val pendingViewCount: Int? = null,
    val viewedTeachingCount: Int? = null,
    val priorityItemId: String? = null,
    val latestItemId: String? = null,
    val latestActivityAt: String? = null,
    val latestText: String = "",
    val latestSenderRole: String? = null,
    val latestHasVideo: Boolean = false,
    val studentHasUnread: Boolean = false,
    val studentViewedAt: String? = null,
)

data class V2CoachMetrics(
    val receivedCount: Int = 0,
    val readyAssetCount: Int = 0,
    val totalVideoBytes: Double = 0.0,
    val totalUploadCount: Int = 0,
    val failedUploadCount: Int = 0,
    val firstViewAverageSeconds: Double = 0.0,
    val feedbackCompletionRate: Double = 0.0,
    val feedbackAverageSeconds: Double = 0.0,
    val averageVideoBytes: Double = 0.0,
    val averageUploadSeconds: Double = 0.0,
    val uploadFailureRate: Double = 0.0,
    val playbackCount: Int = 0,
    val assetReuseRate: Double = 0.0,
    val totalSourceVideoBytes: Double = 0.0,
    val compressionSavingsRate: Double = 0.0,
    val averageProcessingSeconds: Double = 0.0,
    val directUploadRate: Double = 0.0,
    val cellularUploadCount: Int = 0,
    val optimizedUploadCount: Int = 0,
)

data class V2CoachInvite(
    val inviteId: String,
    val inviteCode: String,
    val inviteToken: String,
    val inviteUrl: String,
    val expiresAt: String,
    val coach: V2CoachUserSummary,
)

data class V2CoachInviteStatus(
    val inviteId: String,
    val status: String,
    val expiresAt: String? = null,
    val usedAt: String? = null,
    val student: V2CoachUserSummary? = null,
) {
    val isUsed: Boolean get() = status == "used"
    val isExpired: Boolean get() = status == "expired"
}

data class V2StudentUnreadSnapshot(
    val total: Int = 0,
    val byConnection: Map<String, Int> = emptyMap(),
    val unreadItemIdByConnection: Map<String, String> = emptyMap(),
    val recentItems: List<V2CoachWorkspaceItem> = emptyList(),
    val refreshedAt: String? = null,
)

data class V2StudentUnreadUpdate(
    val snapshot: V2StudentUnreadSnapshot?,
    val version: String?,
)

data class V2CoachPage<T>(
    val values: List<T>,
    val nextCursor: String? = null,
    val total: Int? = null,
    val version: String? = null,
    val unchanged: Boolean = false,
)

data class V2CoachSnapshot(
    val dashboard: V2CoachDashboard = V2CoachDashboard(),
    val connections: List<V2CoachConnection> = emptyList(),
    val reviewQueue: List<V2CoachWorkspaceItem> = emptyList(),
    val library: List<V2CoachLibraryAsset> = emptyList(),
    val reviewNextCursor: String? = null,
    val reviewTotal: Int? = null,
    val reviewVersion: String? = null,
    val libraryNextCursor: String? = null,
    val dashboardVersion: String? = null,
)
