package com.mygolfswing.app.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.mygolfswing.app.data.model.V2Goal
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    onAddTap: () -> Unit = {},
    onOpenTrajectory: () -> Unit = {},
    onOpenAnalysis: () -> Unit = {},
    onOpenOnlineDownload: () -> Unit = {},
) {
    val profile by V2ProfileStore.profile.collectAsState()
    val goals by V2GoalStore.goals.collectAsState()
    val memos by V2MemoStore.allMemos.collectAsState()
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

    val activeCount = remember(goals) {
        goals.count { it.status == V2Goal.Status.ACTIVE }
    }
    val isEmpty = activeCount == 0
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
                    onOpenTrajectory = onOpenTrajectory,
                    onOpenAnalysis = onOpenAnalysis,
                    onOpenOnlineDownload = onOpenOnlineDownload,
                    onSwitchToPlaza = onSwitchToPlaza,
                )
            }

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
    onOpenTrajectory: () -> Unit = {},
    onOpenAnalysis: () -> Unit = {},
    onOpenOnlineDownload: () -> Unit = {},
    onSwitchToPlaza: () -> Unit = {},
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

        V2HomeToolEntryGrid(
            onTrajectoryTap = onOpenTrajectory,
            onAnalysisTap = onOpenAnalysis,
            onDownloadTap = onOpenOnlineDownload,
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

    val pagerState = rememberPagerState(pageCount = { goals.size })

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp),
        ) {
            // 叠牌背景：最多展示后面 2 张，偏移 + 缩小 + 半透明
            for (offset in minOf(goals.size - 1, 2) downTo 1) {
                val nextIndex = (pagerState.currentPage + offset) % goals.size
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
                val goal = goals[page]
                key(goal.id, goal.status, goal.progress.current, goal.progress.lastIncrementAt, goal.hasCheckedInToday) {
                    V2GoalHeroCard(
                        goal = goal,
                        style = V2GoalHeroStyle.cycled(page),
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
                    val isSelected = pagerState.currentPage == index
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

// ─── 常用工具入口网格（对齐 iOS V2HomeToolEntryGrid）────────────────────────────

private data class ToolEntry(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: androidx.compose.ui.graphics.Color,
    val onClick: () -> Unit,
)

@Composable
private fun V2HomeToolEntryGrid(
    onTrajectoryTap: () -> Unit,
    onAnalysisTap: () -> Unit,
    onDownloadTap: () -> Unit,
) {
    val entries = listOf(
        ToolEntry("分析工具", "动作对比", Icons.Filled.BarChart, androidx.compose.ui.graphics.Color(0xFF3B82F6), onAnalysisTap),
        ToolEntry("轨迹绘制", "球路标注", Icons.Filled.Edit, androidx.compose.ui.graphics.Color(0xFF52B788), onTrajectoryTap),
        ToolEntry("在线下载", "导入视频", Icons.Filled.Download, DSColors.fairway, onDownloadTap),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "常用工具",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DSColors.inkGreen,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "常用功能入口",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            entries.forEach { entry ->
                ToolEntryButton(
                    entry = entry,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ToolEntryButton(entry: ToolEntry, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = DSColors.deepGreen.copy(alpha = 0.07f),
            )
            .background(DSColors.cardWhite, shape = RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = entry.accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = entry.onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(entry.accent.copy(alpha = 0.14f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = entry.accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = entry.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.inkGreen,
                maxLines = 1,
            )
            Text(
                text = entry.subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = DSColors.shadowGray,
                maxLines = 1,
            )
        }
    }
}
