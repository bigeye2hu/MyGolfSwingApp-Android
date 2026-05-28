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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.data.store.V2ProfileStore
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
    onAddTap: () -> Unit = {},
) {
    val profile by V2ProfileStore.profile.collectAsState()
    val goals by V2GoalStore.goals.collectAsState()
    val memos by V2MemoStore.allMemos.collectAsState()
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

    val activeCount = V2GoalStore.activeCount
    val isEmpty = activeCount == 0
    val headerName = profile.headerDisplayName()
    val recentMemos = remember(memos) { memos.sortedByDescending { it.createdAt }.take(6) }

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

            V2HomeMemoSection(
                memos = recentMemos,
                onSeeAll = onSwitchToMemo,
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
private fun GoalsBlock(
    onCreateGoal: () -> Unit,
    onOpenAllGoals: () -> Unit,
    onOpenGoalDetail: (UUID) -> Unit,
) {
    val focused = V2GoalStore.focusedGoal
    val others = V2GoalStore.listedGoalsForHome()
    val remaining = V2GoalStore.remainingGoalCountForHome()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        focused?.let { g ->
            V2GoalHeroCard(
                goal = g,
                linkedMemoCount = V2GoalStore.linkedMemoCount(g),
                linkedVideoCount = V2GoalStore.linkedVideoCount(g),
                onEdit = onOpenAllGoals,
                onComplete = { V2GoalStore.checkIn(g.id) },
                onTap = { onOpenGoalDetail(g.id) },
            )
        }
        others.forEach { g ->
            V2GoalRow(
                goal = g,
                onTap = { onOpenGoalDetail(g.id) },
                onComplete = { V2GoalStore.checkIn(g.id) },
            )
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

