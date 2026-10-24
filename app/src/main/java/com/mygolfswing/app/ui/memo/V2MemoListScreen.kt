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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.V2GolfProfileStore
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import java.util.UUID

private const val ROW_PREVIEW_LIMIT = 6

/**
 * Memo Tab 主页（对齐 iOS `V2MemoView`：分区横滑 + 顶部新建，非扁平筛选列表）。
 */
@Composable
fun V2MemoListScreen(
    isLoggedIn: Boolean,
    onCreateMemo: () -> Unit,
    onCreateVideoMemo: () -> Unit,
    onCreateTextMemo: () -> Unit,
    onOpenMemo: (UUID) -> Unit,
    onOpenVideoList: () -> Unit,
    onOpenTextList: () -> Unit,
    onOpenSwingCalendar: () -> Unit,
    onOpenSwingClip: (UUID) -> Unit = {},
    onOpenJustSwing: () -> Unit = onOpenSwingCalendar,
    onOpenAllMemos: () -> Unit,
    onSwitchToAI: () -> Unit = {},
    onRequestLogin: () -> Unit = {},
) {
    val allMemos by V2MemoStore.allMemos.collectAsState()
    val videoRow = remember(allMemos, isLoggedIn) {
        if (isLoggedIn) V2MemoStore.videoRowMemos.take(ROW_PREVIEW_LIMIT) else V2GuestMemoSamples.mixed
    }
    val textRow = remember(allMemos, isLoggedIn) {
        if (isLoggedIn) V2MemoStore.textMemos.take(ROW_PREVIEW_LIMIT) else V2GuestMemoSamples.text
    }
    val totalVideo = remember(allMemos, isLoggedIn) { if (isLoggedIn) V2MemoStore.totalVideoRowCount else videoRow.size }
    val totalText = remember(allMemos, isLoggedIn) { if (isLoggedIn) V2MemoStore.totalTextCount else textRow.size }
    val swingItems by V2SwingSessionStore.dayItems.collectAsState()
    val displaySwingItems = remember(swingItems, isLoggedIn) { if (isLoggedIn) swingItems else listOf(V2GuestMemoSamples.swingDay) }
    val totalSwingClips = remember(swingItems, isLoggedIn) { if (isLoggedIn) V2SwingSessionStore.totalClipCount else V2GuestMemoSamples.swingDay.clipCount }
    val totalCount = remember(allMemos, isLoggedIn) { if (isLoggedIn) V2MemoStore.totalCount else videoRow.size + textRow.size }
    val golfProfile by V2GolfProfileStore.profile.collectAsState()
    var previewingGuestMemo by remember { mutableStateOf<V2Memo?>(null) }

    V2RoomPage(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
        item {
            V2MemoHomeHeader(totalCount = totalCount, onCreate = onCreateMemo)
        }
        if (isLoggedIn) {
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
                            guestPreview = !isLoggedIn,
                            modifier = Modifier.clickable {
                                if (isLoggedIn) onOpenMemo(memo.id) else previewingGuestMemo = memo
                            },
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
                            modifier = Modifier.clickable {
                                if (isLoggedIn) onOpenMemo(memo.id) else previewingGuestMemo = memo
                            },
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
                visibleCount = displaySwingItems.sumOf { it.clipCount },
                totalCount = totalSwingClips,
                onMore = onOpenSwingCalendar,
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            if (displaySwingItems.isEmpty()) {
                V2MemoSectionEmptyCard(V2MemoSectionEmptyKind.SWING, onTap = onOpenJustSwing)
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = DSSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    displaySwingItems.forEach { item ->
                        V2MemoSwingSessionCard(
                            item = item,
                            onTap = { if (isLoggedIn) onOpenSwingCalendar() else onRequestLogin() },
                            onClipTap = { if (isLoggedIn) onOpenSwingClip(it) else onRequestLogin() },
                            guestPreview = !isLoggedIn,
                        )
                    }
                }
            }
        }

        item {
            V2MemoHomeFooter(totalCount = totalCount, onViewAll = onOpenAllMemos)
        }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    previewingGuestMemo?.let { memo ->
        V2GuestMemoPreviewSheet(
            memo = memo,
            onDismiss = { previewingGuestMemo = null },
            onLogin = onRequestLogin,
        )
    }
}
