package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.coach.V2MemoDiscussionIndex
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import java.time.LocalDateTime
import java.util.UUID

/** 视频笔记二级列表（对齐 iOS `V2VideoMemoListView`） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun V2VideoMemoListScreen(
    onBack: () -> Unit,
    isGuest: Boolean = false,
    onOpenMemo: (UUID) -> Unit,
    onOpenDiscussion: (connectionId: String, itemId: String) -> Unit = { _, _ -> },
    onCreateVideoMemo: () -> Unit = {},
    onRequestLogin: () -> Unit = {},
) {
    val all by V2MemoStore.allMemos.collectAsState()
    val discussionIndex by V2MemoDiscussionIndex.state.collectAsState()
    var filterId by rememberSaveable { mutableStateOf("all") }
    var previewingGuestMemo by remember { mutableStateOf<V2Memo?>(null) }
    val videos = remember(all, isGuest) {
        if (isGuest) {
            V2GuestMemoSamples.mixed
        } else {
            all.filter { it.kind == V2MemoKind.VIDEO || it.kind == V2MemoKind.MIXED }
                .sortedByDescending { it.createdAt }
        }
    }
    val filtered = remember(videos, filterId, isGuest) {
        if (isGuest) videos else videoMemoListVisibleMemos(videos, filterId)
    }
    val stats = remember(videos, isGuest) {
        if (isGuest) return@remember "共 ${videos.size} 条视频笔记"
        val now = LocalDateTime.now()
        val weekAgo = now.minusDays(6)
        val weekNew = videos.count { it.createdAt >= weekAgo }
        val totalMin = (videos.sumOf { (it.durationSec ?: 0.0) } / 60).toInt()
        "${videos.size} 条 · 本周新增 $weekNew · 总时长 $totalMin 分钟"
    }
    val groups = remember(filtered) {
        val order = mutableListOf<String>()
        val bucket = linkedMapOf<String, List<com.mygolfswing.app.data.model.V2Memo>>()
        filtered.forEach { m ->
            val key = V2MemoListGrouping.headerKey(m.createdAt)
            if (!bucket.containsKey(key)) order.add(key)
            bucket[key] = bucket[key].orEmpty() + m
        }
        order.map { it to bucket[it].orEmpty() }
    }
    val filters = listOf(
        "all" to "全部", "shoot" to "拍摄", "upload" to "本地上传",
        "online" to "在线", "plaza" to "球场视频",
    )

    V2RoomPage(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().v2NavigationBarPadding(),
        ) {
        V2MemoListNavHeader(
            title = "视频笔记",
            onBack = onBack,
            trailing = {
                V2MemoListHeaderActionButton(
                    icon = Icons.Filled.Search,
                    contentDescription = "搜索",
                    enabled = false,
                )
            },
        )
        V2MemoStatsBar(stats)
        if (!isGuest) {
            Spacer(Modifier.height(12.dp))
            V2MemoFilterChips(filters, filterId) { filterId = it }
        }
        Spacer(Modifier.height(12.dp))
        if (filtered.isEmpty()) {
            VideoMemoListEmptyState(
                filterId = filterId,
                hasAnyVideo = videos.isNotEmpty(),
                onClearFilter = { filterId = "all" },
            )
        } else {
            LazyColumn(Modifier.weight(1f)) {
                groups.forEachIndexed { index, (header, items) ->
                    if (index > 0) {
                        item {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    stickyHeader { V2MemoListSectionHeader(header) }
                    items(items, key = { it.id }) { memo ->
                        val (chip, style) = videoSourceChipFor(memo)
                        val prefix = V2MemoListGrouping.metaPrefix(memo.createdAt)
                        val meta = memo.trainingMethod?.takeIf { it.isNotBlank() }?.let { "$prefix · $it" } ?: prefix
                        V2VideoMemoLargeCard(
                            memo = memo,
                            chipText = chip,
                            chipStyle = style,
                            metaText = meta,
                            guestPreview = isGuest,
                            discussionUpdates = if (isGuest) emptyList() else discussionIndex[memo.id].orEmpty(),
                            onOpen = {
                                if (isGuest) previewingGuestMemo = memo else onOpenMemo(memo.id)
                            },
                            onOpenDiscussion = onOpenDiscussion,
                            modifier = Modifier
                                .padding(horizontal = DSSpacing.lg)
                                .padding(top = 12.dp),
                        )
                    }
                }
                item {
                    V2MemoNoMoreFooter()
                }
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

internal fun videoMemoListVisibleMemos(
    memos: List<V2Memo>,
    filterId: String,
): List<V2Memo> = memos
    .filter { matchesVideoFilter(it, filterId) }

@Composable
private fun VideoMemoListEmptyState(
    filterId: String,
    hasAnyVideo: Boolean,
    onClearFilter: () -> Unit,
) {
    val isAll = filterId == "all" && !hasAnyVideo
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = DSSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        if (isAll) {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                tint = DSColors.shadowGray,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("memo-video-empty-tray"),
            )
        } else {
            V2MemoEmptyFilterGlyph(testTag = "memo-video-empty-filter")
        }
        Spacer(Modifier.height(if (isAll) 8.dp else 12.dp))
        Text(
            text = when {
                isAll -> "暂无视频笔记"
                else -> "当前筛选下没有视频笔记"
            },
            color = DSColors.inkGreen,
            fontSize = if (isAll) 14.sp else 13.sp,
            fontWeight = FontWeight.Medium,
        )
        if (isAll) {
            Spacer(Modifier.height(8.dp))
            Text(
                "回主页用「+ 新建 Memo」录一段",
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        } else {
            Spacer(Modifier.height(12.dp))
            Text(
                "清除筛选",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.fairway)
                    .clickable(onClick = onClearFilter)
                    .padding(horizontal = 16.dp)
                    .height(32.dp)
                    .wrapContentHeight(Alignment.CenterVertically),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}
