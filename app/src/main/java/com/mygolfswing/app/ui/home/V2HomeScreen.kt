package com.mygolfswing.app.ui.home

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mygolfswing.app.data.model.V2Goal
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.R
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2SwingClipRetention
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.ui.common.headerDisplayName
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * V2 首页（对齐 iOS `V2HomeView`）。
 *
 * - 空态：[V2HomeEmptyCard]
 * - 有目标态：Hero 卡 + 列表 + 管理入口
 * - 横滑段：Memo / Plaza
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
    onOpenSwingCalendar: () -> Unit = onSwitchToMemo,
    onOpenGrowthPool: () -> Unit = onOpenSwingCalendar,
    onOpenJustSwing: () -> Unit = onOpenSwingCalendar,
    onAddTap: () -> Unit = {},
    onOpenTrajectory: () -> Unit = {},
    onOpenAnalysis: () -> Unit = {},
    onOpenOnlineDownload: () -> Unit = {},
) {
    val context = LocalContext.current
    val profile by V2ProfileStore.profile.collectAsState()
    val goals by V2GoalStore.goals.collectAsState()
    val memos by V2MemoStore.allMemos.collectAsState()
    val memoSyncing by V2MemoStore.isSyncing.collectAsState()
    val swingSessions by V2SwingSessionStore.sessions.collectAsState()
    val plazaVideos by V2PlazaStore.videos.collectAsState()
    val plazaLoading by V2PlazaStore.isLoading.collectAsState()
    val plazaRecommends = remember(plazaVideos) {
        plazaVideos.sortedByDescending { it.publishedAtMs }.take(6)
    }

    LaunchedEffect(Unit) {
        if (V2PlazaStore.videos.value.isEmpty()) V2PlazaStore.refresh()
    }

    LaunchedEffect(profile.isLoggedIn) {
        if (profile.isLoggedIn) {
            V2ProfileStore.refresh(force = false)
            V2GoalStore.syncFromCloud()
        }
    }

    var showDevSwitcher by remember { mutableStateOf(false) }
    var forceEmptyState by remember { mutableStateOf(readHomeForceEmptyState(context)) }
    fun updateForceEmptyState(enabled: Boolean) {
        forceEmptyState = enabled
        saveHomeForceEmptyState(context, enabled)
    }

    val activeCount = remember(goals) {
        goals.count { it.status == V2Goal.Status.ACTIVE }
    }
    val showForcedEmptyState = ServiceLocator.isDebuggable && forceEmptyState
    val isEmpty = activeCount == 0 || showForcedEmptyState
    val headerName = profile.headerDisplayName()
    val recentMemos = remember(memos) { memos.sortedByDescending { it.createdAt }.take(6) }
    val promotedSwingClips = remember(swingSessions) {
        swingSessions
            .flatMap { it.clips }
            .filter { it.retention == V2SwingClipRetention.PROMOTED }
            .sortedByDescending { it.timestamp }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = DSSpacing.sm, bottom = DSSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(DSSpacing.xl),
        ) {
            V2HomeHeader(
                goalCount = if (isEmpty) 0 else activeCount,
                displayName = headerName,
                avatarUrl = profile.avatar,
                onAddTap = onAddTap,
                onAvatarLongPress = if (ServiceLocator.isDebuggable) {
                    { showDevSwitcher = true }
                } else {
                    null
                },
            )

            if (isEmpty) {
                V2HomeEmptyCard(
                    onCreateGoal = onCreateGoal,
                    onBrowsePlaza = onSwitchToPlaza,
                    modifier = Modifier.padding(horizontal = DSSpacing.lg),
                )
            } else {
                GoalsBlock(
                    onCreateGoal = onCreateGoal,
                    onOpenAllGoals = onOpenAllGoals,
                    onOpenGoalDetail = onOpenGoalDetail,
                )
            }

            V2HomeToolEntryGrid(
                onRecordTap = onOpenJustSwing,
                onTrajectoryTap = onOpenTrajectory,
                onAnalysisTap = onOpenAnalysis,
                onDownloadTap = onOpenOnlineDownload,
            )

            if (promotedSwingClips.isNotEmpty()) {
                V2HomeGrowthPoolSummary(
                    promotedCount = promotedSwingClips.size,
                    latestTitle = V2HomeGrowthPoolTitle(promotedSwingClips.first().durationSec),
                    onTap = onOpenGrowthPool,
                    modifier = Modifier.padding(horizontal = DSSpacing.lg),
                )
            }

            V2HomeMemoSection(
                memos = recentMemos,
                isLoading = memoSyncing && recentMemos.isEmpty(),
                onSeeAll = onSwitchToMemo,
                onMemoTap = { onOpenMemo(it.id) },
            )

            V2HomePlazaSection(
                videos = plazaRecommends,
                isLoading = plazaLoading,
                onSeeAll = onSwitchToPlaza,
                onVideoTap = { onOpenPlazaVideo(it.id) },
            )

            V2HomeDecorativeFooter(modifier = Modifier.padding(horizontal = DSSpacing.lg))
        }
    }

    if (showDevSwitcher && ServiceLocator.isDebuggable) {
        V2HomeDevSwitcherDialog(
            forceEmptyState = forceEmptyState,
            onForceEmptyStateChange = ::updateForceEmptyState,
            onDismiss = { showDevSwitcher = false },
        )
    }
}

@Composable
private fun V2HomeDevSwitcherDialog(
    forceEmptyState: Boolean,
    onForceEmptyStateChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("V2 开发者切换", color = DSColors.inkGreen, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "仅调试包可见，用于核对 iOS 导航图中的隐藏 HOME-004 入口。",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("强制空状态主页", style = DSTypography.bodyBold, color = DSColors.inkGreen)
                        Text("隐藏已有目标卡夹，用于检查首页空态视觉。", style = DSTypography.caption, color = DSColors.shadowGray)
                    }
                    Switch(checked = forceEmptyState, onCheckedChange = onForceEmptyStateChange)
                }
                Text(
                    "Android 当前上线包只有 V2 主体验，因此不提供 iOS 开发期的「切回 V1」运行时开关。",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = DSColors.fairway, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = DSColors.cardWhite,
    )
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

// ─── 常用工具横滑栏（对齐 iOS V2HomeToolBar）──────────────────────────────────

private data class ToolEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    @param:DrawableRes val iconRes: Int,
    val accent: androidx.compose.ui.graphics.Color,
    val onClick: () -> Unit,
)

private val defaultHomeToolIds = listOf("record", "analysis", "create", "download")

private fun readHomeForceEmptyState(context: Context): Boolean =
    context.getSharedPreferences("v2_home_dev_switcher", Context.MODE_PRIVATE)
        .getBoolean("force_empty_state", false)

private fun saveHomeForceEmptyState(context: Context, enabled: Boolean) {
    context.getSharedPreferences("v2_home_dev_switcher", Context.MODE_PRIVATE)
        .edit {
            putBoolean("force_empty_state", enabled)
        }
}

private fun readHomeToolOrder(context: Context): List<String> {
    val prefs = context.getSharedPreferences("v2_home_quick_tools", Context.MODE_PRIVATE)
    val stored = prefs.getString("order", null)
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it in defaultHomeToolIds }
        .orEmpty()
    return (stored + defaultHomeToolIds).distinct().filter { it in defaultHomeToolIds }
}

private fun readHomeToolEnabledIds(context: Context): Set<String> {
    val prefs = context.getSharedPreferences("v2_home_quick_tools", Context.MODE_PRIVATE)
    val stored = prefs.getString("enabled", null) ?: return defaultHomeToolIds.toSet()
    return stored.split(",").map { it.trim() }.filter { it in defaultHomeToolIds }.toSet()
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
private fun V2HomeToolEntryGrid(
    onRecordTap: () -> Unit,
    onTrajectoryTap: () -> Unit,
    onAnalysisTap: () -> Unit,
    onDownloadTap: () -> Unit,
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
        ToolEntry("record", "挥杆记录", "每日练习记录", R.drawable.tool_swing, androidx.compose.ui.graphics.Color(0xFFA855F7), onRecordTap),
        ToolEntry("analysis", "回看分析", "逐帧分析回看", R.drawable.tool_analyze, androidx.compose.ui.graphics.Color(0xFF3B82F6), onAnalysisTap),
        ToolEntry("create", "轨迹绘制", "飞行轨迹绘制", R.drawable.tool_path, androidx.compose.ui.graphics.Color(0xFF52B788), onTrajectoryTap),
        ToolEntry("download", "视频下载", "第三方视频下载", R.drawable.tool_download, androidx.compose.ui.graphics.Color(0xFF22C55E), onDownloadTap),
    )
    val entriesById = entries.associateBy { it.id }
    val orderedEntries = order.mapNotNull(entriesById::get)
    val visibleEntries = orderedEntries.filter { it.id in enabledIds }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "常用工具",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DSColors.inkGreen,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.sandBeige)
                    .clickable { showConfig = true }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Filled.Tune, null, tint = DSColors.shadowGray, modifier = Modifier.size(14.dp))
                Text(
                    text = "自定义",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DSColors.shadowGray,
                )
            }
        }

        if (visibleEntries.isEmpty()) {
            Text(
                text = "点击「自定义」添加常用工具",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                visibleEntries.forEach { entry ->
                    ToolEntryButton(entry = entry)
                }
            }
        }
    }

    if (showConfig) {
        ModalBottomSheet(
            onDismissRequest = { showConfig = false },
            sheetState = configSheetState,
            containerColor = DSColors.warmWhite,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null,
        ) {
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "自定义工具栏",
                        color = DSColors.inkGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showConfig = false }) {
                        Text("完成", color = DSColors.fairway, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(
                    "打开开关控制首页显示，使用箭头调整顺序。",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    order.forEachIndexed { index, id ->
                        val entry = entriesById[id] ?: return@forEachIndexed
                        ToolConfigRow(
                            entry = entry,
                            enabled = id in enabledIds,
                            canMoveUp = index > 0,
                            canMoveDown = index < order.lastIndex,
                            onEnabledChange = { enabled ->
                                updateConfig(
                                    nextEnabledIds = if (enabled) enabledIds + id else enabledIds - id,
                                )
                            },
                            onMoveUp = {
                                if (index > 0) {
                                    updateConfig(order.toMutableList().also {
                                        val item = it.removeAt(index)
                                        it.add(index - 1, item)
                                    })
                                }
                            },
                            onMoveDown = {
                                if (index < order.lastIndex) {
                                    updateConfig(order.toMutableList().also {
                                        val item = it.removeAt(index)
                                        it.add(index + 1, item)
                                    })
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolEntryButton(entry: ToolEntry, modifier: Modifier = Modifier) {
    var titleFontSize by remember(entry.title) { mutableStateOf(13.sp) }
    Column(
        modifier = modifier
            .width(90.dp)
            .height(88.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = DSColors.deepGreen.copy(alpha = 0.07f),
                spotColor = DSColors.deepGreen.copy(alpha = 0.07f),
            )
            .background(DSColors.cardWhite, shape = RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = entry.accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = entry.onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(entry.accent.copy(alpha = 0.14f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = entry.iconRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = entry.title,
            fontSize = titleFontSize,
            fontWeight = FontWeight.Bold,
            color = DSColors.inkGreen,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (result.hasVisualOverflow && titleFontSize.value > 11f) {
                    titleFontSize = (titleFontSize.value - 0.5f).sp
                }
            },
        )
    }
}

@Composable
private fun ToolConfigRow(
    entry: ToolEntry,
    enabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(entry.accent.copy(alpha = if (enabled) 0.14f else 0.06f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = entry.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
                    .alpha(if (enabled) 1f else 0.35f),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                entry.title,
                color = if (enabled) DSColors.inkGreen else DSColors.shadowGray,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(entry.subtitle, color = DSColors.shadowGray.copy(alpha = 0.72f), fontSize = 12.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                null,
                tint = DSColors.shadowGray.copy(alpha = if (canMoveUp) 1f else 0.24f),
                modifier = Modifier.size(22.dp).clickable(enabled = canMoveUp, onClick = onMoveUp),
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                null,
                tint = DSColors.shadowGray.copy(alpha = if (canMoveDown) 1f else 0.24f),
                modifier = Modifier.size(22.dp).clickable(enabled = canMoveDown, onClick = onMoveDown),
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = entry.accent,
                uncheckedThumbColor = androidx.compose.ui.graphics.Color.White,
                uncheckedTrackColor = DSColors.shadowGray.copy(alpha = 0.38f),
            ),
        )
    }
}
