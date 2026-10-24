package com.mygolfswing.app.ui.home

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.model.V2PlazaCategoryChip
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.data.model.V2SwingClip
import com.mygolfswing.app.data.coach.V2CoachWorkspaceItem
import com.mygolfswing.app.data.coach.V2StudentUnreadSnapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.R
import com.mygolfswing.app.data.model.V2SwingClipRetention
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.ui.common.headerDisplayName
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.plaza.V2PlazaCoachAvatar
import com.mygolfswing.app.ui.designsystem.DSLayout
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import com.mygolfswing.app.ui.plaza.V2PlazaHeroCard
import com.mygolfswing.app.ui.plaza.v2PlazaCompactCount
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * V2 首页（对齐 iOS `V2HomeView`）。
 *
 * - 最近一次练习优先作为 Hero；没有练习时展示广场精选
 * - 教练未读反馈直接回到对应教学对话
 * - 固定高频工具 + 教学视频搜索、分类和双列内容流
 * - 底部装饰页脚
 */
@Composable
fun V2HomeScreen(
    onCreateGoal: () -> Unit = {},
    onOpenAllGoals: () -> Unit = {},
    onOpenGoalDetail: (UUID) -> Unit = {},
    onSwitchToPlaza: () -> Unit = {},
    onSwitchToMemo: () -> Unit = {},
    onOpenPlazaVideo: (UUID) -> Unit = {},
    onOpenMemo: (UUID) -> Unit = {},
    onOpenSwingClip: (UUID) -> Unit = {},
    onOpenSwingCalendar: () -> Unit = onSwitchToMemo,
    onOpenGrowthPool: () -> Unit = onOpenSwingCalendar,
    onOpenJustSwing: () -> Unit = onOpenSwingCalendar,
    onOpenAI: () -> Unit = {},
    onAddTap: () -> Unit = {},
    onOpenTrajectory: () -> Unit = {},
    onOpenAnalysis: () -> Unit = {},
    onOpenOnlineDownload: () -> Unit = {},
    onRequestLogin: () -> Unit = {},
    studentUnread: V2StudentUnreadSnapshot = V2StudentUnreadSnapshot(),
    coachNamesByConnection: Map<String, String> = emptyMap(),
    coachAvatarsByConnection: Map<String, String?> = emptyMap(),
    activeCoachConnectionIds: List<String> = emptyList(),
    onOpenCoachFeedback: (connectionId: String, itemId: String?) -> Unit = { _, _ -> },
    onRefreshStudentData: suspend () -> Unit = {},
) {
    val context = LocalContext.current
    val profile by V2ProfileStore.profile.collectAsState()
    val goals by V2GoalStore.goals.collectAsState()
    val memos by V2MemoStore.allMemos.collectAsState()
    val memoSyncing by V2MemoStore.isSyncing.collectAsState()
    val swingSessions by V2SwingSessionStore.sessions.collectAsState()
    val plazaVideos by V2PlazaStore.videos.collectAsState()
    val plazaHero by V2PlazaStore.heroVideo.collectAsState()
    val plazaCategories by V2PlazaStore.categories.collectAsState()
    val selectedPlazaCategory by V2PlazaStore.selectedCategoryId.collectAsState()
    val plazaSearchKeyword by V2PlazaStore.searchKeyword.collectAsState()
    val plazaSearchActive by V2PlazaStore.isSearchActive.collectAsState()
    val plazaLoading by V2PlazaStore.isLoading.collectAsState()
    val plazaLoadingMore by V2PlazaStore.isLoadingMore.collectAsState()

    LaunchedEffect(Unit) {
        if (V2PlazaStore.videos.value.isEmpty()) V2PlazaStore.refresh()
    }

    LaunchedEffect(profile.isLoggedIn) {
        if (profile.isLoggedIn) {
            V2ProfileStore.refresh(force = false)
            V2GoalStore.syncFromCloud()
        }
    }

    val headerName = profile.headerDisplayName()
    var homeSearchText by rememberSaveable { mutableStateOf(plazaSearchKeyword) }
    // 显式依赖 collectAsState 的列表，确保视频先到、头像随后补全时都会重组首屏。
    val visiblePlazaVideos = remember(plazaVideos, plazaSearchKeyword) {
        V2PlazaStore.filterPlazaVideos(plazaVideos, plazaSearchKeyword)
    }
    val latestPractice = remember(memos, swingSessions, profile.isLoggedIn) {
        if (profile.isLoggedIn) latestHomePractice(memos, swingSessions.flatMap { it.clips }) else null
    }
    val coachFeedback = remember(
        studentUnread,
        coachNamesByConnection,
        coachAvatarsByConnection,
        activeCoachConnectionIds,
        profile.isLoggedIn,
    ) {
        if (profile.isLoggedIn) {
            resolveHomeCoachFeedback(
                unread = studentUnread,
                coachNamesByConnection = coachNamesByConnection,
                coachAvatarsByConnection = coachAvatarsByConnection,
                activeCoachConnectionIds = activeCoachConnectionIds,
            )
        } else null
    }
    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val homeScrollState = rememberScrollState()
    val paginationThresholdPx = with(LocalDensity.current) { 480.dp.roundToPx() }

    LaunchedEffect(homeScrollState, visiblePlazaVideos.lastOrNull()?.id) {
        snapshotFlow {
            homeScrollState.maxValue > 0 &&
                homeScrollState.value >= homeScrollState.maxValue - paginationThresholdPx
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                visiblePlazaVideos.lastOrNull()?.id?.let(V2PlazaStore::loadMoreIfNeeded)
            }
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            if (!refreshing) {
                refreshing = true
                V2PlazaStore.refresh(refreshCoachProfiles = true)
                refreshScope.launch {
                    try {
                        onRefreshStudentData()
                        delay(250)
                        while (V2PlazaStore.isLoading.value) delay(50)
                    } finally {
                        refreshing = false
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        V2RoomBackground(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(homeScrollState)
                .padding(top = DSSpacing.sm, bottom = DSSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(DSLayout.homeSectionSpacing),
        ) {
            V2HomeHeader(
                displayName = headerName,
                searchText = homeSearchText,
                onSearchTextChange = {
                    homeSearchText = it
                    val keyword = it.trim()
                    when {
                        keyword.isEmpty() && plazaSearchActive -> V2PlazaStore.clearSearch()
                        keyword.isNotEmpty() && keyword != V2PlazaStore.searchKeyword.value -> {
                            V2PlazaStore.setSearchKeyword(keyword)
                        }
                    }
                },
                avatarUrl = profile.avatar,
                isGuest = !profile.isLoggedIn,
                onRecordTap = onOpenJustSwing,
                onSearchSubmit = {
                    val keyword = homeSearchText.trim()
                    if (keyword.isEmpty()) V2PlazaStore.clearSearch()
                    else V2PlazaStore.setSearchKeyword(keyword)
                },
            )

            val homeSearchKeyword = homeSearchText.trim()
            if (homeSearchKeyword.isEmpty()) {
                if (latestPractice != null) {
                    V2HomeContinuePracticeCard(
                        practice = latestPractice,
                        onRecord = onOpenJustSwing,
                        onReview = {
                            when (val target = latestPractice.review) {
                                is HomePracticeReview.Clip -> onOpenSwingClip(target.id)
                                is HomePracticeReview.Memo -> onOpenMemo(target.id)
                            }
                        },
                        modifier = Modifier.padding(horizontal = DSSpacing.lg),
                    )
                } else if (plazaHero != null && !plazaSearchActive) {
                    V2PlazaHeroCard(
                        video = plazaHero!!,
                        onTap = { onOpenPlazaVideo(plazaHero!!.id) },
                        modifier = Modifier.padding(horizontal = DSSpacing.lg),
                    )
                } else if (plazaLoading && visiblePlazaVideos.isEmpty()) {
                    V2HomeHeroLoading(modifier = Modifier.padding(horizontal = DSSpacing.lg))
                }

                coachFeedback?.let { feedback ->
                    V2HomeCoachFeedbackCard(
                        feedback = feedback,
                        onClick = { onOpenCoachFeedback(feedback.connectionId, feedback.itemId) },
                        modifier = Modifier.padding(horizontal = DSSpacing.lg),
                    )
                }

                V2HomeToolShortcutRow(
                    isGuest = !profile.isLoggedIn,
                    onRecordTap = onOpenJustSwing,
                    onHistoryTap = onOpenSwingCalendar,
                    onTrajectoryTap = onOpenTrajectory,
                    onAnalysisTap = onOpenAnalysis,
                    onDownloadTap = onOpenOnlineDownload,
                    onAITap = onOpenAI,
                    onGuestMoreTap = onRequestLogin,
                )

                V2HomeCategoryBar(
                    categories = plazaCategories,
                    selectedId = selectedPlazaCategory,
                    onSelect = V2PlazaStore::selectCategory,
                )

                V2HomeVideoFeed(
                    videos = visiblePlazaVideos,
                    isLoading = plazaLoading,
                    isLoadingMore = plazaLoadingMore,
                    isSearchActive = plazaSearchActive,
                    isGuest = !profile.isLoggedIn,
                    onRequestLogin = onRequestLogin,
                    onOpenVideo = onOpenPlazaVideo,
                )
            } else {
                V2HomeSearchResults(
                    keyword = homeSearchKeyword,
                    videos = visiblePlazaVideos,
                    memos = memos,
                    isLoading = plazaLoading,
                    isLoadingMore = plazaLoadingMore,
                    isGuest = !profile.isLoggedIn,
                    onRequestLogin = onRequestLogin,
                    onOpenVideo = onOpenPlazaVideo,
                    onOpenMemo = onOpenMemo,
                )
            }

            V2HomeDecorativeFooter(modifier = Modifier.padding(horizontal = DSSpacing.lg))
        }
    }

    LaunchedEffect(plazaSearchKeyword) {
        if (plazaSearchKeyword.isEmpty() && homeSearchText.isNotEmpty() && !plazaSearchActive) {
            homeSearchText = ""
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (V2PlazaStore.isSearchActive.value) V2PlazaStore.clearSearch()
        }
    }

}

internal fun homeSearchedMemos(memos: List<V2Memo>, keyword: String): List<V2Memo> {
    val query = keyword.trim()
    if (query.isEmpty()) return emptyList()
    return memos
        .asSequence()
        .filter { it.kind == V2MemoKind.MIXED || it.kind == V2MemoKind.TEXT }
        .filter { memo ->
            memo.displayTitle.orEmpty().contains(query, ignoreCase = true) ||
                memo.text.orEmpty().contains(query, ignoreCase = true)
        }
        .sortedByDescending(V2Memo::createdAt)
        .toList()
}

@Composable
internal fun V2HomeSearchResults(
    keyword: String,
    videos: List<V2PlazaVideo>,
    memos: List<V2Memo>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    isGuest: Boolean,
    onRequestLogin: () -> Unit,
    onOpenVideo: (UUID) -> Unit,
    onOpenMemo: (UUID) -> Unit,
) {
    val searchedMemos = remember(memos, keyword) { homeSearchedMemos(memos, keyword) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "公开视频",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DSColors.inkGreen,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        V2HomeVideoFeed(
            videos = videos,
            isLoading = isLoading,
            isLoadingMore = isLoadingMore,
            isSearchActive = true,
            isGuest = isGuest,
            onRequestLogin = onRequestLogin,
            onOpenVideo = onOpenVideo,
        )
        Text(
            text = "我的记录",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DSColors.inkGreen,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (searchedMemos.isEmpty()) {
            Text(
                text = "没有找到我的记录",
                fontSize = 13.sp,
                color = DSColors.shadowGray,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                searchedMemos.forEach { memo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DSColors.cardWhite)
                            .clickable { onOpenMemo(memo.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = memo.displayTitle ?: "我的记录",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DSColors.inkGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (memo.kind == V2MemoKind.TEXT) {
                                    "随手记 · 仅自己可见"
                                } else {
                                    "视频笔记 · 仅自己可见"
                                },
                                fontSize = 12.sp,
                                color = DSColors.shadowGray,
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = DSColors.shadowGray,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

internal sealed interface HomePracticeReview {
    data class Clip(val id: UUID) : HomePracticeReview
    data class Memo(val id: UUID) : HomePracticeReview
}

internal data class HomePracticePresentation(
    val title: String,
    val timeText: String,
    val thumbnail: String?,
    val date: LocalDateTime,
    val review: HomePracticeReview,
)

internal fun latestHomePractice(
    memos: List<V2Memo>,
    clips: List<V2SwingClip>,
): HomePracticePresentation? {
    val clip = clips.maxByOrNull(V2SwingClip::timestamp)?.let {
        HomePracticePresentation(
            // iOS 优先使用 SwingClip.notes；sourceLabel 是导入入口，不属于用户标题。
            title = it.notes?.trim()?.takeIf(String::isNotEmpty) ?: "挥杆练习",
            timeText = homeRelativeDate(it.timestamp),
            thumbnail = it.thumbnailPath,
            date = it.timestamp,
            review = HomePracticeReview.Clip(it.id),
        )
    }
    // iOS 的 `memoVM.mixedMemos` 是视频备忘录 row，不包含随手记。
    val memo = memos
        .filter { it.kind == V2MemoKind.VIDEO || it.kind == V2MemoKind.MIXED }
        .maxByOrNull(V2Memo::createdAt)
        ?.let {
            HomePracticePresentation(
                title = it.displayTitle?.trim()?.takeIf(String::isNotEmpty)
                    ?: "练习记录",
                timeText = homeRelativeDate(it.createdAt),
                thumbnail = it.thumbnailUrl,
                date = it.createdAt,
                review = HomePracticeReview.Memo(it.id),
            )
        }
    return listOfNotNull(clip, memo).maxByOrNull(HomePracticePresentation::date)
}

internal data class HomeCoachFeedback(
    val connectionId: String,
    val itemId: String?,
    val coachName: String,
    val headline: String,
    val detail: String,
    val avatar: String?,
)

internal fun resolveHomeCoachFeedback(
    unread: V2StudentUnreadSnapshot,
    coachNamesByConnection: Map<String, String>,
    coachAvatarsByConnection: Map<String, String?> = emptyMap(),
    activeCoachConnectionIds: List<String> = coachNamesByConnection.keys.toList(),
): HomeCoachFeedback? {
    val activeCoachConnectionIdSet = activeCoachConnectionIds.toSet()
    val candidates = unread.recentItems.filter { item ->
        item.connectionId in activeCoachConnectionIdSet &&
            item.parentItemId == null && !item.isRemovedContent &&
            (unread.unreadItemIdByConnection[item.connectionId] == item.itemId || item.studentHasUnread)
    }
    val item = candidates.maxByOrNull { coachItemEpoch(it) }
    val connectionId = item?.connectionId
        ?: activeCoachConnectionIds.firstOrNull { (unread.byConnection[it] ?: 0) > 0 }
        ?: return null
    val coachName = coachNamesByConnection[connectionId]?.takeIf(String::isNotBlank) ?: "教练"
    val title = V2CoachWorkspaceItem.visibleCopy(item?.title)
    val preview = V2CoachWorkspaceItem.visibleCopy(item?.latestReplyText)
        ?: V2CoachWorkspaceItem.visibleCopy(item?.text)
        ?: ""
    val headline = title?.let { "$coachName 对你的《$it》有新反馈" } ?: "$coachName 发来新的反馈"
    val detail = if (item == null) {
        "点击查看你们的教学交流"
    } else {
        val time = homeRelativeCoachDate(item.latestReplyAt ?: item.latestActivityAt ?: item.createdAt)
        if (preview.isEmpty()) time else "$time · $preview"
    }
    return HomeCoachFeedback(
        connectionId = connectionId,
        itemId = item?.itemId ?: unread.unreadItemIdByConnection[connectionId],
        coachName = coachName,
        headline = headline,
        detail = detail,
        avatar = coachAvatarsByConnection[connectionId],
    )
}

private fun coachItemEpoch(item: V2CoachWorkspaceItem): Long {
    val raw = item.latestReplyAt ?: item.latestActivityAt ?: item.createdAt ?: return Long.MIN_VALUE
    return runCatching { OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
        .recoverCatching { java.time.Instant.parse(raw).toEpochMilli() }
        .getOrDefault(Long.MIN_VALUE)
}

private fun homeRelativeDate(date: LocalDateTime): String {
    val now = LocalDateTime.now()
    val time = date.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (date.toLocalDate()) {
        now.toLocalDate() -> "今天 $time"
        now.toLocalDate().minusDays(1) -> "昨天 $time"
        else -> date.format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
    }
}

private fun homeRelativeCoachDate(raw: String?): String {
    if (raw.isNullOrBlank()) return "刚刚"
    val local = coachLocalDateTime(raw) ?: return "刚刚"
    return homeRelativeDate(local)
}

internal fun coachLocalDateTime(
    raw: String,
    zoneId: ZoneId = ZoneId.systemDefault(),
): LocalDateTime? = runCatching {
    OffsetDateTime.parse(raw).atZoneSameInstant(zoneId).toLocalDateTime()
}.recoverCatching {
    LocalDateTime.parse(raw)
}.getOrNull()

@Composable
internal fun V2HomeContinuePracticeCard(
    practice: HomePracticePresentation,
    onRecord: () -> Unit,
    onReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(DSColors.deepGreen, DSColors.fairway),
                    ),
                ),
        ) {
            practice.thumbnail?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0.5f to androidx.compose.ui.graphics.Color.Transparent,
                            1f to androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f),
                        ),
                    ),
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("继续你的练习", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f), style = DSTypography.visualChip)
                Text(practice.title, color = androidx.compose.ui.graphics.Color.White, style = DSTypography.visualTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("上次记录：${practice.timeText}", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.86f), style = DSTypography.visualMeta)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomePillButton("记录练习", true, onRecord, Modifier.weight(1f))
            HomePillButton("回看上次", false, onReview, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HomePillButton(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .background(if (filled) DSColors.fairway else DSColors.cardWhite)
            .border(1.dp, if (filled) DSColors.fairway else DSColors.borderLine, RoundedCornerShape(50))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (filled) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(text, color = if (filled) androidx.compose.ui.graphics.Color.White else DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun V2HomeCoachFeedbackCard(
    feedback: HomeCoachFeedback,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        V2UserAvatar(
            avatar = feedback.avatar,
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .border(1.dp, DSColors.mistGreen, CircleShape),
            fallbackFontSize = 15.sp,
            placeholderIconSize = 24.dp,
            fallbackText = feedback.coachName,
            fallbackColor = DSColors.sandBeige,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(feedback.headline, color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (feedback.detail.isNotEmpty()) {
                Text(feedback.detail, color = DSColors.shadowGray, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = DSColors.fairway, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun V2HomeHeroLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(22.dp)).background(DSColors.sandBeige),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.CircularProgressIndicator(color = DSColors.fairway, modifier = Modifier.size(28.dp))
    }
}

@Composable
internal fun V2HomeCategoryBar(
    categories: List<V2PlazaCategoryChip>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            val selected = category.id == selectedId
            Text(
                text = if (category.id == "all") "精选" else category.name,
                color = if (selected) androidx.compose.ui.graphics.Color.White else DSColors.inkGreen.copy(alpha = 0.7f),
                style = DSTypography.visualChip.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .shadow(
                        elevation = if (selected) 3.dp else 0.dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = if (selected) DSColors.fairway.copy(alpha = 0.25f) else androidx.compose.ui.graphics.Color.Transparent,
                        spotColor = if (selected) DSColors.fairway.copy(alpha = 0.25f) else androidx.compose.ui.graphics.Color.Transparent,
                    )
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) DSColors.fairway else DSColors.sandBeige)
                    .border(
                        1.dp,
                        if (selected) androidx.compose.ui.graphics.Color.Transparent else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.06f),
                        RoundedCornerShape(50),
                    )
                    .clickable { onSelect(category.id) }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
internal fun V2HomeVideoFeed(
    videos: List<V2PlazaVideo>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    isSearchActive: Boolean,
    isGuest: Boolean,
    onRequestLogin: () -> Unit,
    onOpenVideo: (UUID) -> Unit,
) {
    when {
        isLoading && videos.isEmpty() -> Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = DSColors.fairway,
                modifier = Modifier.size(28.dp),
            )
        }
        videos.isEmpty() -> Text(
            text = if (isSearchActive) "没有找到相关视频" else "这个分类暂时没有视频",
            color = DSColors.shadowGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        )
        else -> Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            videos.chunked(2).forEach { rowVideos ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowVideos.forEach { video ->
                        V2HomeVideoCard(
                            video = video,
                            isGuest = isGuest,
                            onTap = { onOpenVideo(video.id) },
                            onSaveTap = {
                                if (isGuest) onRequestLogin() else V2PlazaStore.toggleSave(video.id)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowVideos.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (isLoadingMore) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = DSColors.fairway,
                    modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp),
                )
            }
        }
    }
}

/** 封面按卡片宽度强制 3:4。普通 height 会被父级最大高度压扁。 */
private fun Modifier.homeVideoCover(): Modifier = layout { measurable, constraints ->
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
    val safeWidth = width.coerceAtLeast(1)
    val height = (safeWidth * 4f / 3f).toInt().coerceAtLeast(1)
    val placeable = measurable.measure(Constraints.fixed(safeWidth, height))
    layout(safeWidth, height) { placeable.place(0, 0) }
}

@Composable
internal fun V2HomeVideoCard(
    video: V2PlazaVideo,
    isGuest: Boolean,
    onTap: () -> Unit,
    onSaveTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .homeVideoCover()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(14.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(DSColors.deepGreen, DSColors.fairway),
                        ),
                    ),
            ) {
                video.thumbnailUrl?.takeIf(String::isNotBlank)?.let { thumbnail ->
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(9.dp),
                    )
                }
                video.categoryDisplayName?.takeIf(String::isNotBlank)?.let { tag ->
                    Text(
                        text = "# $tag",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.38f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                val durationSec = video.durationSec.toInt().coerceAtLeast(0)
                if (durationSec > 0) {
                    Text(
                        text = "%d:%02d".format(durationSec / 60, durationSec % 60),
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
        Text(
            text = video.title,
            color = DSColors.inkGreen,
            style = DSTypography.visualCardTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            V2PlazaCoachAvatar(coach = video.coach, size = 16.dp, fontSize = 7.sp)
            Text(
                text = video.coach.displayName,
                color = DSColors.shadowGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onSaveTap)
                    .semantics {
                        contentDescription = homeVideoSaveAccessibilityLabel(
                            isGuest = isGuest,
                            isSaved = video.isSaved,
                        )
                    }
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    imageVector = if (video.isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (video.isSaved) DSColors.flagRed else DSColors.shadowGray,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    text = homeVideoSaveText(video.isSaved, video.saveCount),
                    color = if (video.isSaved) DSColors.flagRed else DSColors.shadowGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

internal fun homeVideoSaveText(isSaved: Boolean, saveCount: Int): String =
    if (isSaved && saveCount == 0) "已收藏" else v2PlazaCompactCount(saveCount)

internal fun homeVideoSaveAccessibilityLabel(isGuest: Boolean, isSaved: Boolean): String = when {
    isGuest -> "登录后收藏"
    isSaved -> "已收藏"
    else -> "收藏"
}

@Composable
private fun V2HomeGrowthPoolSummary(
    promotedCount: Int,
    latestTitle: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DSColors.deepGreen, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(DSColors.goldBright.copy(alpha = 0.16f), shape = androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Upgrade, null, tint = DSColors.goldBright, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("成长池", style = DSTypography.titleMedium, color = DSColors.cardWhite)
            Text(
                "$promotedCount 条挥杆已永久保留 · 最新 $latestTitle",
                style = DSTypography.caption,
                color = DSColors.cardWhite.copy(alpha = 0.72f),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("查看", style = DSTypography.caption, color = DSColors.cardWhite.copy(alpha = 0.82f))
            Icon(Icons.Filled.ChevronRight, null, tint = DSColors.cardWhite.copy(alpha = 0.82f), modifier = Modifier.size(15.dp))
        }
    }
}

private fun V2HomeGrowthPoolTitle(durationSec: Double): String {
    val seconds = durationSec.toInt().coerceAtLeast(0)
    return if (seconds > 0) "${seconds}s" else "挥杆视频"
}

@Composable
private fun GoalsBlock(
    onCreateGoal: () -> Unit,
    onOpenAllGoals: () -> Unit,
    onOpenGoalDetail: (UUID) -> Unit,
) {
    val goals by V2GoalStore.goals.collectAsState()
    val activeGoals = remember(goals) {
        goals.filter { it.status == V2Goal.Status.ACTIVE }
    }
    val focused = remember(activeGoals) {
        activeGoals.firstOrNull { it.todayFocus } ?: activeGoals.firstOrNull()
    }
    val homeDeckGoals = remember(activeGoals, focused) {
        if (focused == null) activeGoals
        else listOf(focused) + activeGoals.filter { it.id != focused.id }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
    ) {
        V2GoalCardDeck(
            goals = homeDeckGoals,
            onEdit = { onOpenAllGoals() },
            onCheckIn = { V2GoalStore.checkIn(it.id) },
            onTap = { onOpenGoalDetail(it.id) },
        )
    }
}

@Composable
private fun V2GoalCardDeck(
    goals: List<V2Goal>,
    onEdit: (V2Goal) -> Unit,
    onCheckIn: (V2Goal) -> Unit,
    onTap: (V2Goal) -> Unit,
) {
    if (goals.isEmpty()) return

    val virtualPageCount = if (goals.size > 1) goals.size * 3 else goals.size
    val middleStartPage = if (goals.size > 1) goals.size else 0
    val pagerState = rememberPagerState(
        initialPage = middleStartPage,
        pageCount = { virtualPageCount },
    )
    fun realIndex(page: Int): Int = page % goals.size
    val selectedIndex = realIndex(pagerState.currentPage)

    LaunchedEffect(goals.size) {
        if (goals.size > 1 && pagerState.currentPage !in goals.size until goals.size * 2) {
            pagerState.scrollToPage(goals.size + realIndex(pagerState.currentPage))
        }
    }

    LaunchedEffect(pagerState.currentPage, goals.size) {
        if (goals.size <= 1) return@LaunchedEffect

        val currentPage = pagerState.currentPage
        val firstMiddlePage = goals.size
        val firstTrailingPage = goals.size * 2
        if (currentPage < firstMiddlePage || currentPage >= firstTrailingPage) {
            val targetPage = firstMiddlePage + realIndex(currentPage)
            delay(380)
            if (pagerState.currentPage == currentPage) {
                pagerState.scrollToPage(targetPage)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp),
        ) {
            // 叠牌背景：最多展示后面 2 张，偏移 + 缩小 + 半透明
            for (offset in minOf(goals.size - 1, 2) downTo 1) {
                val nextIndex = (selectedIndex + offset) % goals.size
                val goal = goals[nextIndex]
                val scaleFactor = 1f - offset * 0.045f
                val alpha = if (offset == 1) 0.38f else 0.22f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scaleFactor
                            scaleY = scaleFactor
                            translationX = (offset * 14).dp.toPx()
                            translationY = (offset * 10).dp.toPx()
                            this.alpha = alpha
                        },
                ) {
                    V2GoalHeroCard(
                        goal = goal,
                        style = V2GoalHeroStyle.cycled(nextIndex),
                        linkedMemoCount = V2GoalStore.linkedMemoCount(goal),
                        linkedVideoCount = V2GoalStore.linkedVideoCount(goal),
                    )
                }
            }

            // 可滑动的主卡片层
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(236.dp)
                    .zIndex(1f),
            ) { page ->
                val pageIndex = realIndex(page)
                val goal = goals[pageIndex]
                key(page, goal.id, goal.status, goal.progress.current, goal.progress.lastIncrementAt, goal.hasCheckedInToday) {
                    V2GoalHeroCard(
                        goal = goal,
                        style = V2GoalHeroStyle.cycled(pageIndex),
                        linkedMemoCount = V2GoalStore.linkedMemoCount(goal),
                        linkedVideoCount = V2GoalStore.linkedVideoCount(goal),
                        onEdit = { onEdit(goal) },
                        onComplete = { onCheckIn(goal) },
                        onTap = { onTap(goal) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = if (goals.size > 1) 18.dp else 0.dp),
                    )
                }
            }
        }

        // 分页指示点（多于 1 个目标时显示）
        if (goals.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                goals.indices.forEach { index ->
                    val isSelected = selectedIndex == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 18.dp else 6.dp,
                        animationSpec = tween(200),
                        label = "dot_width_$index",
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(dotWidth)
                            .height(6.dp)
                            .background(
                                color = if (isSelected) DSColors.deepGreen else DSColors.shadowGray.copy(alpha = 0.28f),
                                shape = RoundedCornerShape(50),
                            ),
                    )
                }
            }
        }
    }
}

// ─── 首页快捷入口（对齐 iOS V2HomeToolShortcutRow）────────────────────────────

private data class ToolEntry(
    val id: String,
    val title: String,
    val subtitle: String?,
    @param:DrawableRes val iconRes: Int? = null,
    val iconVector: ImageVector? = null,
    val accent: androidx.compose.ui.graphics.Color,
    val onClick: () -> Unit,
)

internal val defaultHomeToolIds = listOf("record", "history", "analysis", "create", "download", "ai")

internal fun resolvedHomeToolOrder(savedOrder: List<String>, migrateHistory: Boolean): List<String> {
    val validSaved = savedOrder.filter { it in defaultHomeToolIds }
    val resolved = (validSaved + defaultHomeToolIds).distinct().toMutableList()
    if (migrateHistory) {
        resolved.remove("history")
        val recordIndex = resolved.indexOf("record").coerceAtLeast(0)
        resolved.add((recordIndex + 1).coerceAtMost(resolved.size), "history")
    }
    return resolved
}

internal fun resolvedHomeToolEnabledIds(
    savedEnabledIds: Set<String>?,
    migrateHistory: Boolean,
    migrateAI: Boolean,
): Set<String> {
    val enabled = savedEnabledIds
        ?.filter { it in defaultHomeToolIds }
        ?.toMutableSet()
        ?: defaultHomeToolIds.toMutableSet()
    if (migrateHistory) enabled += "history"
    if (migrateAI) enabled += "ai"
    return enabled
}

internal fun reorderedHomeTools(order: List<String>, id: String, direction: Int): List<String> {
    val from = order.indexOf(id)
    if (from < 0 || direction == 0) return order
    val step = if (direction < 0) -1 else 1
    val to = (from + step).coerceIn(order.indices)
    if (from == to) return order
    return order.toMutableList().also {
        val item = it.removeAt(from)
        it.add(to, item)
    }
}

internal fun visibleHomeToolIds(order: List<String>, enabledIds: Set<String>): List<String> =
    order.filter { it in defaultHomeToolIds && it in enabledIds }

private fun ensureHomeToolMigrations(context: Context) {
    val prefs = context.getSharedPreferences("v2_home_quick_tools", Context.MODE_PRIVATE)
    val migrateHistory = !prefs.getBoolean("history_migration_v1", false)
    val migrateAI = !prefs.getBoolean("ai_migration_v1", false)
    if (!migrateHistory && !migrateAI) return

    val storedOrder = prefs.getString("order", null)
        ?.split(",")
        ?.map { it.trim() }
        .orEmpty()
    val storedEnabled = prefs.getString("enabled", null)
        ?.split(",")
        ?.map { it.trim() }
        ?.toSet()
    val order = resolvedHomeToolOrder(storedOrder, migrateHistory)
    val enabled = resolvedHomeToolEnabledIds(storedEnabled, migrateHistory, migrateAI)
    prefs.edit {
        putString("order", order.joinToString(","))
        putString("enabled", enabled.joinToString(","))
        putBoolean("history_migration_v1", true)
        putBoolean("ai_migration_v1", true)
    }
}

private fun readHomeToolOrder(context: Context): List<String> {
    ensureHomeToolMigrations(context)
    val prefs = context.getSharedPreferences("v2_home_quick_tools", Context.MODE_PRIVATE)
    val stored = prefs.getString("order", null)
        ?.split(",")
        ?.map { it.trim() }
        .orEmpty()
    return resolvedHomeToolOrder(stored, migrateHistory = false)
}

private fun readHomeToolEnabledIds(context: Context): Set<String> {
    ensureHomeToolMigrations(context)
    val prefs = context.getSharedPreferences("v2_home_quick_tools", Context.MODE_PRIVATE)
    val stored = prefs.getString("enabled", null) ?: return defaultHomeToolIds.toSet()
    return resolvedHomeToolEnabledIds(
        savedEnabledIds = stored.split(",").map { it.trim() }.toSet(),
        migrateHistory = false,
        migrateAI = false,
    )
}

private fun saveHomeToolConfig(context: Context, order: List<String>, enabledIds: Set<String>) {
    context.getSharedPreferences("v2_home_quick_tools", Context.MODE_PRIVATE)
        .edit {
            putString("order", order.joinToString(","))
            putString("enabled", enabledIds.joinToString(","))
        }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun V2HomeToolShortcutRow(
    isGuest: Boolean,
    onRecordTap: () -> Unit,
    onHistoryTap: () -> Unit,
    onTrajectoryTap: () -> Unit,
    onAnalysisTap: () -> Unit,
    onDownloadTap: () -> Unit,
    onAITap: () -> Unit,
    onGuestMoreTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showConfig by remember { mutableStateOf(false) }
    val configSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var order by remember { mutableStateOf(readHomeToolOrder(context)) }
    var enabledIds by remember { mutableStateOf(readHomeToolEnabledIds(context)) }
    fun updateConfig(nextOrder: List<String> = order, nextEnabledIds: Set<String> = enabledIds) {
        order = nextOrder
        enabledIds = nextEnabledIds
        saveHomeToolConfig(context, nextOrder, nextEnabledIds)
    }

    val entries = listOf(
        ToolEntry("record", "每日练习", "每日练习记录", R.drawable.tool_swing, accent = androidx.compose.ui.graphics.Color(0xFFA855F7), onClick = onRecordTap),
        ToolEntry("history", "挥杆记录", null, iconVector = Icons.Filled.CalendarMonth, accent = androidx.compose.ui.graphics.Color(0xFFF59E0B), onClick = onHistoryTap),
        ToolEntry("analysis", "回看分析", "逐帧分析回看", R.drawable.tool_analyze, accent = androidx.compose.ui.graphics.Color(0xFF3B82F6), onClick = onAnalysisTap),
        ToolEntry("create", "轨迹绘制", "飞行轨迹绘制", R.drawable.tool_path, accent = androidx.compose.ui.graphics.Color(0xFF52B788), onClick = onTrajectoryTap),
        ToolEntry("download", "视频下载", "第三方视频下载", R.drawable.tool_download, accent = androidx.compose.ui.graphics.Color(0xFF22C55E), onClick = onDownloadTap),
        ToolEntry("ai", "AI 教练", null, iconVector = Icons.Filled.AutoAwesome, accent = androidx.compose.ui.graphics.Color(0xFFD4A017), onClick = onAITap),
    )
    val entriesById = entries.associateBy { it.id }
    val shortcutEntries = visibleHomeToolIds(order, enabledIds).mapNotNull(entriesById::get).map { entry ->
        if (entry.id == "record") entry.copy(title = "录挥杆") else entry
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        shortcutEntries.forEach { entry ->
            V2HomeToolShortcut(entry = entry, isGuest = isGuest)
        }
        V2HomeToolShortcut(
            entry = ToolEntry(
                id = "more",
                title = "更多",
                subtitle = null,
                iconVector = Icons.Filled.GridView,
                accent = DSColors.shadowGray,
                onClick = { if (isGuest) onGuestMoreTap() else showConfig = true },
            ),
            isGuest = isGuest,
        )
    }

    if (showConfig) {
        ModalBottomSheet(
            onDismissRequest = { showConfig = false },
            sheetState = configSheetState,
            containerColor = DSColors.warmWhite,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null,
        ) {
            V2RoomPage(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 16.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(DSColors.shadowGray.copy(alpha = 0.28f))
                            .align(Alignment.CenterHorizontally),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    ) {
                        Text(
                            "自定义工具栏",
                            color = DSColors.inkGreen,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        TextButton(
                            onClick = { showConfig = false },
                            modifier = Modifier.align(Alignment.CenterEnd),
                        ) {
                            Text("完成", color = DSColors.fairway, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        "拖动右侧图标可调整顺序",
                        style = DSTypography.caption,
                        color = DSColors.shadowGray,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DSColors.cardWhite),
                    ) {
                        order.forEachIndexed { index, id ->
                            val entry = entriesById[id] ?: return@forEachIndexed
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 68.dp),
                                    color = DSColors.borderLine,
                                )
                            }
                            ToolConfigRow(
                                entry = entry,
                                enabled = id in enabledIds,
                                onEnabledChange = { enabled ->
                                    updateConfig(
                                        nextEnabledIds = if (enabled) enabledIds + id else enabledIds - id,
                                    )
                                },
                                onMove = { direction ->
                                    updateConfig(reorderedHomeTools(order, id, direction))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V2HomeToolShortcut(
    entry: ToolEntry,
    isGuest: Boolean,
) {
    Column(
        modifier = Modifier
            .testTag("home-tool-shortcut-${entry.id}")
            .width(DSLayout.homeToolWidth)
            .clickable(onClick = entry.onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(DSLayout.homeToolCircle),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().clip(CircleShape)
                    .background(entry.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                entry.iconVector?.let {
                    Icon(it, null, tint = entry.accent, modifier = Modifier.size(DSLayout.homeToolIcon))
                } ?: entry.iconRes?.let {
                    Image(painterResource(it), null, modifier = Modifier.size(DSLayout.homeToolImage))
                }
            }
            if (isGuest) {
                Box(
                    modifier = Modifier.align(Alignment.Center).offset(x = 16.dp, y = 16.dp).size(14.dp)
                        .clip(CircleShape).background(DSColors.deepGreen.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Lock, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(7.dp))
                }
            }
        }
        Text(
            entry.title,
            color = DSColors.inkGreen,
            style = DSTypography.tab.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ToolConfigRow(
    entry: ToolEntry,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onMove: (Int) -> Unit,
) {
    val latestOnMove by rememberUpdatedState(onMove)
    Row(
        modifier = Modifier
            .testTag("home-tool-config-row-${entry.id}")
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(entry.accent.copy(alpha = if (enabled) 0.14f else 0.06f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            entry.iconVector?.let { vector ->
                Icon(
                    vector,
                    null,
                    tint = entry.accent,
                    modifier = Modifier.size(17.dp).alpha(if (enabled) 1f else 0.35f),
                )
            } ?: entry.iconRes?.let { res ->
                Image(
                    painter = painterResource(id = res),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp).alpha(if (enabled) 1f else 0.35f),
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                entry.title,
                color = if (enabled) DSColors.inkGreen else DSColors.shadowGray,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            entry.subtitle?.let { subtitle ->
                Text(subtitle, color = DSColors.shadowGray.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            modifier = Modifier.testTag("home-tool-toggle-${entry.id}"),
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = entry.accent,
                uncheckedThumbColor = androidx.compose.ui.graphics.Color.White,
                uncheckedTrackColor = DSColors.shadowGray.copy(alpha = 0.38f),
            ),
        )
        Icon(
            Icons.Filled.DragHandle,
            contentDescription = "拖动${entry.title}排序",
            tint = DSColors.shadowGray.copy(alpha = 0.72f),
            modifier = Modifier
                .size(24.dp)
                .pointerInput(entry.id) {
                    var accumulatedY = 0f
                    val threshold = 44.dp.toPx()
                    detectDragGesturesAfterLongPress(
                        onDragStart = { accumulatedY = 0f },
                        onDragEnd = { accumulatedY = 0f },
                        onDragCancel = { accumulatedY = 0f },
                        onDrag = { change, amount ->
                            change.consume()
                            accumulatedY += amount.y
                            while (accumulatedY <= -threshold) {
                                latestOnMove(-1)
                                accumulatedY += threshold
                            }
                            while (accumulatedY >= threshold) {
                                latestOnMove(1)
                                accumulatedY -= threshold
                            }
                        },
                    )
                },
        )
    }
}
