package com.mygolfswing.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
) {
    val goals by V2GoalStore.goals.collectAsState()
    val activeGoals = remember(goals) {
        goals.filter { it.status == V2Goal.Status.ACTIVE }
    }
    val focused = remember(activeGoals) {
        activeGoals.firstOrNull { it.todayFocus } ?: activeGoals.firstOrNull()
    }
    val focusedIndex = remember(activeGoals, focused) {
        activeGoals.indexOfFirst { it.id == focused?.id }.takeIf { it >= 0 } ?: 0
    }
    val others = remember(activeGoals, focused) {
        activeGoals.filter { it.id != focused?.id }.take(2)
    }
    val remaining = remember(activeGoals, focused) {
        val visible = activeGoals.count { it.id != focused?.id }
        (visible - 2).coerceAtLeast(0)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        focused?.let { g ->
            key(g.id, g.status, g.progress.current, g.progress.lastIncrementAt, g.hasCheckedInToday) {
                V2GoalHeroCard(
                    goal = g,
                    style = V2GoalHeroStyle.cycled(focusedIndex),
                    linkedMemoCount = V2GoalStore.linkedMemoCount(g),
                    linkedVideoCount = V2GoalStore.linkedVideoCount(g),
                    onEdit = onOpenAllGoals,
                    onComplete = { V2GoalStore.checkIn(g.id) },
                    onTap = { onOpenGoalDetail(g.id) },
                )
            }
        }
        others.forEach { g ->
            key(g.id, g.status, g.progress.current, g.progress.lastIncrementAt, g.hasCheckedInToday) {
                V2GoalRow(
                    goal = g,
                    onTap = { onOpenGoalDetail(g.id) },
                    onComplete = { V2GoalStore.checkIn(g.id) },
                )
            }
        }
        ManageAllGoalsFooter(
            remaining = remaining,
            onTap = onOpenAllGoals,
        )
    }
}

@Composable
private fun ManageAllGoalsFooter(remaining: Int, onTap: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (remaining > 0) {
            Text(
                text = "还有 $remaining 个目标在管理页",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable { onTap() },
        ) {
            Text(text = "管理所有目标", style = DSTypography.cardTitle, color = DSColors.deepGreen)
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = DSColors.deepGreen,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
