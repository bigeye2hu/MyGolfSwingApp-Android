package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.V2GolfProfileStore
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import java.util.UUID

private const val ROW_PREVIEW_LIMIT = 6

/**
 * Memo Tab 主页（对齐 iOS `V2MemoView`：分区横滑 + 顶部新建，非扁平筛选列表）。
 */
@Composable
fun V2MemoListScreen(
    onCreateMemo: () -> Unit,
    onCreateVideoMemo: () -> Unit,
    onCreateTextMemo: () -> Unit,
    onOpenMemo: (UUID) -> Unit,
    onOpenVideoList: () -> Unit,
    onOpenTextList: () -> Unit,
    onOpenSwingCalendar: () -> Unit,
    onOpenJustSwing: () -> Unit = onOpenSwingCalendar,
    onOpenAllMemos: () -> Unit,
    onSwitchToAI: () -> Unit = {},
) {
    val allMemos by V2MemoStore.allMemos.collectAsState()
    val videoRow = remember(allMemos) { V2MemoStore.videoRowMemos.take(ROW_PREVIEW_LIMIT) }
    val textRow = remember(allMemos) { V2MemoStore.textMemos.take(ROW_PREVIEW_LIMIT) }
    val totalVideo = remember(allMemos) { V2MemoStore.totalVideoRowCount }
    val totalText = remember(allMemos) { V2MemoStore.totalTextCount }
    val swingItems by V2SwingSessionStore.dayItems.collectAsState()
    val totalSwingClips = remember(swingItems) { V2SwingSessionStore.totalClipCount }
    val totalCount = remember(allMemos, totalSwingClips) { V2MemoStore.totalCount + totalSwingClips }
    val golfProfile by V2GolfProfileStore.profile.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            V2MemoHomeHeader(totalCount = totalCount, onCreate = onCreateMemo)
        }
        item {
            V2MemoHomeAIInsightCard(
                memoSampleCount = totalCount,
                statusSummary = golfProfile.statusSummary,
                currentFocus = golfProfile.currentFocus.firstOrNull()?.content,
                painPoint = golfProfile.painPoints.firstOrNull()?.content,
                trainingGoal = golfProfile.trainingGoals.firstOrNull(),
                onContinueChat = onSwitchToAI,
            )
        }

        // 视频笔记
        item { Spacer(Modifier.height(24.dp)) }
        item {
            V2MemoHomeSectionHeader(
                icon = V2MemoHomeIcons.videoNotes,
                title = "视频笔记",
                visibleCount = videoRow.size,
                totalCount = totalVideo,
                onMore = onOpenVideoList,
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            if (videoRow.isEmpty()) {
                V2MemoSectionEmptyCard(V2MemoSectionEmptyKind.MIXED, onTap = onCreateVideoMemo)
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = DSSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(videoRow, key = { it.id }) { memo ->
                        V2MemoMixedCard(
                            memo = memo,
                            modifier = Modifier
                                .clickable { onOpenMemo(memo.id) }
                                .padding(bottom = 4.dp),
                        )
                    }
                }
            }
        }

        // 随手记
        item { Spacer(Modifier.height(20.dp)) }
        item {
            V2MemoHomeSectionHeader(
                icon = V2MemoHomeIcons.textNotes,
                title = "随手记",
                visibleCount = textRow.size,
                totalCount = totalText,
                onMore = onOpenTextList,
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            if (textRow.isEmpty()) {
                V2MemoSectionEmptyCard(V2MemoSectionEmptyKind.TEXT, onTap = onCreateTextMemo)
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = DSSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(textRow, key = { it.id }) { memo ->
                        V2MemoTextCard(
                            memo = memo,
                            modifier = Modifier
                                .clickable { onOpenMemo(memo.id) }
                                .padding(bottom = 4.dp),
                        )
                    }
                }
            }
        }

        // 挥杆记录
        item { Spacer(Modifier.height(20.dp)) }
        item {
            V2MemoHomeSectionHeader(
                icon = V2MemoHomeIcons.swingRecords,
                title = "挥杆记录",
                visibleCount = swingItems.sumOf { it.clipCount },
                totalCount = totalSwingClips,
                onMore = onOpenSwingCalendar,
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            if (swingItems.isEmpty()) {
                V2MemoSectionEmptyCard(V2MemoSectionEmptyKind.SWING, onTap = onOpenJustSwing)
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = DSSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    swingItems.forEach { item ->
                        V2MemoSwingSessionCard(item = item, onTap = onOpenSwingCalendar)
                    }
                }
            }
        }

        item {
            V2MemoHomeFooter(totalCount = totalCount, onViewAll = onOpenAllMemos)
        }
    }
}
