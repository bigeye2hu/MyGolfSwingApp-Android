package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.coach.V2MemoDiscussionIndex
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.model.V2SwingDayItem
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import java.time.LocalDateTime
import java.util.UUID

/** 全部 Memo 二级页：对齐 iOS，按时间混合展示视频、随手记和挥杆记录。 */
@Composable
fun V2MemoAllListScreen(
    onBack: () -> Unit,
    isGuest: Boolean = false,
    onOpenMemo: (UUID) -> Unit,
    onOpenDiscussion: (connectionId: String, itemId: String) -> Unit = { _, _ -> },
    onOpenSwingCalendar: () -> Unit = {},
    onOpenSwingClip: (UUID) -> Unit = {},
    onRequestLogin: () -> Unit = {},
) {
    val storedMemos by V2MemoStore.allMemos.collectAsState()
    val discussionIndex by V2MemoDiscussionIndex.state.collectAsState()
    val storedSwingDays by V2SwingSessionStore.allDayItems.collectAsState()
    var previewingGuestMemo by remember { mutableStateOf<V2Memo?>(null) }

    val memos = remember(storedMemos, isGuest) {
        if (isGuest) V2GuestMemoSamples.all else storedMemos.sortedByDescending { it.createdAt }
    }
    val swingDays = remember(storedSwingDays, isGuest) {
        if (isGuest) emptyList() else storedSwingDays
    }
    val rows = remember(memos, swingDays) {
        buildAllMemoRows(memos = memos, swingDays = swingDays)
    }
    val groups = remember(rows) {
        rows.groupBy { V2MemoListGrouping.headerKey(it.timestamp) }
            .map { (header, items) -> header to items }
    }
    val videoCount = remember(memos) { memos.count(V2Memo::hasVideo) }
    val textCount = remember(memos) { memos.count { it.kind == V2MemoKind.TEXT } }
    val swingCount = remember(swingDays) { swingDays.sumOf(V2SwingDayItem::clipCount) }
    val statsText = remember(isGuest, rows, videoCount, textCount, swingCount) {
        if (isGuest) {
            "共 ${rows.size} 条 Memo"
        } else {
            "共 ${videoCount + textCount + swingCount} 条 Memo · 视频 $videoCount · 随手记 $textCount · 挥杆 $swingCount"
        }
    }

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().v2NavigationBarPadding()) {
            V2MemoListNavHeader(
                title = "全部 Memo",
                onBack = onBack,
                trailing = {},
            )
            V2MemoStatsBar(statsText)
            Spacer(Modifier.height(12.dp))

            if (rows.isEmpty()) {
                AllMemoListEmptyState(Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    groups.forEachIndexed { groupIndex, (header, items) ->
                        if (groupIndex > 0) item { Spacer(Modifier.height(8.dp)) }
                        stickyHeader { V2MemoListSectionHeader(header) }
                        items(
                            count = items.size,
                            key = { index -> items[index].stableKey },
                        ) { index ->
                            when (val row = items[index]) {
                                is AllMemoRow.Video -> {
                                    val (chip, style) = videoSourceChipFor(row.memo)
                                    V2VideoMemoLargeCard(
                                        memo = row.memo,
                                        chipText = chip,
                                        chipStyle = style,
                                        metaText = V2MemoListGrouping.metaPrefix(row.memo.createdAt),
                                        guestPreview = isGuest,
                                        discussionUpdates = if (isGuest) emptyList() else discussionIndex[row.memo.id].orEmpty(),
                                        onOpen = {
                                            if (isGuest) previewingGuestMemo = row.memo
                                            else onOpenMemo(row.memo.id)
                                        },
                                        onOpenDiscussion = onOpenDiscussion,
                                        modifier = Modifier
                                            .padding(horizontal = DSSpacing.lg)
                                            .padding(top = 12.dp),
                                    )
                                }

                                is AllMemoRow.Text -> V2TextMemoLargeCard(
                                    memo = row.memo,
                                    modifier = Modifier
                                        .padding(horizontal = DSSpacing.lg)
                                        .padding(top = 12.dp)
                                        .clickable {
                                            if (isGuest) previewingGuestMemo = row.memo
                                            else onOpenMemo(row.memo.id)
                                        },
                                )

                                is AllMemoRow.Swing -> V2MemoSwingSessionCard(
                                    item = row.item,
                                    onTap = onOpenSwingCalendar,
                                    onClipTap = onOpenSwingClip,
                                    modifier = Modifier
                                        .padding(horizontal = DSSpacing.lg)
                                        .padding(top = 12.dp)
                                        .testTag("all-memo-swing-day-${row.item.id}"),
                                )
                            }
                        }
                    }
                    item {
                        V2MemoNoMoreFooter(Modifier.padding(top = 16.dp))
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
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

private sealed interface AllMemoRow {
    val timestamp: LocalDateTime
    val stableKey: String

    data class Video(val memo: V2Memo) : AllMemoRow {
        override val timestamp: LocalDateTime = memo.createdAt
        override val stableKey: String = "video-${memo.id}"
    }

    data class Text(val memo: V2Memo) : AllMemoRow {
        override val timestamp: LocalDateTime = memo.createdAt
        override val stableKey: String = "text-${memo.id}"
    }

    data class Swing(val item: V2SwingDayItem) : AllMemoRow {
        override val timestamp: LocalDateTime = item.day.atStartOfDay()
        override val stableKey: String = "swing-${item.id}"
    }
}

private fun buildAllMemoRows(
    memos: List<V2Memo>,
    swingDays: List<V2SwingDayItem>,
): List<AllMemoRow> = buildList {
    memos.forEach { memo ->
        add(if (memo.kind == V2MemoKind.TEXT) AllMemoRow.Text(memo) else AllMemoRow.Video(memo))
    }
    swingDays.forEach { add(AllMemoRow.Swing(it)) }
}.sortedByDescending(AllMemoRow::timestamp)

@Composable
private fun AllMemoListEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.Layers,
            contentDescription = null,
            tint = DSColors.shadowGray,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "暂无 Memo",
            color = DSColors.inkGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "回主页用「+ 新建 Memo」记录一次练习",
            color = DSColors.shadowGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.weight(1f))
    }
}
