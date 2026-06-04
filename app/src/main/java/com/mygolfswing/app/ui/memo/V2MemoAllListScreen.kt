package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.model.V2Topic
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.ui.common.V2NavTopBar
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import java.util.UUID

/** 全部 Memo 扁平列表（带筛选），从主页「查看全部」进入 */
private enum class MemoFilter(val display: String) {
    ALL("全部"), TEXT("文字"), VIDEO("视频"), PINNED("置顶"),
}

@Composable
fun V2MemoAllListScreen(
    onBack: () -> Unit,
    onOpenMemo: (UUID) -> Unit,
) {
    val memos by V2MemoStore.allMemos.collectAsState()
    var filter by rememberSaveable { mutableStateOf(MemoFilter.ALL) }
    var topic by rememberSaveable { mutableStateOf(V2Topic.ALL) }

    val filtered = remember(memos, filter, topic) {
        memos
            .filter { m ->
                when (filter) {
                    MemoFilter.ALL -> true
                    MemoFilter.TEXT -> m.kind == V2MemoKind.TEXT
                    MemoFilter.VIDEO -> m.kind == V2MemoKind.VIDEO || m.kind == V2MemoKind.MIXED
                    MemoFilter.PINNED -> m.isPinned
                }
            }
            .filter { topic == V2Topic.ALL || it.topic == topic }
            .sortedByDescending { it.createdAt }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        V2NavTopBar(title = "全部 Memo", onBack = onBack)
        AllListStatsBar(
            total = memos.size,
            text = memos.count { it.kind == V2MemoKind.TEXT },
            video = memos.count { it.hasVideo },
        )
        AllListFilterRow(filter = filter, onSelect = { filter = it })
        LazyRow(
            contentPadding = PaddingValues(horizontal = DSSpacing.lg, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(V2Topic.entries, key = { it.name }) { t ->
                AllListChip(label = t.displayName, selected = t == topic) { topic = t }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
            verticalArrangement = Arrangement.spacedBy(DSSpacing.md),
        ) {
            items(filtered, key = { it.id }) { memo ->
                AllListRowCard(memo = memo, onTap = { onOpenMemo(memo.id) })
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AllListStatsBar(total: Int, text: Int, video: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AllListStatPill("$total", "总条数", DSColors.fairway)
        AllListStatPill("$text", "文字", Color(0xFF3B82F6))
        AllListStatPill("$video", "视频", Color(0xFFF59E0B))
    }
}

@Composable
private fun AllListStatPill(value: String, label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.75f))
    }
}

@Composable
private fun AllListFilterRow(filter: MemoFilter, onSelect: (MemoFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MemoFilter.entries.forEach { f ->
            AllListChip(label = f.display, selected = f == filter) { onSelect(f) }
        }
    }
}

@Composable
private fun AllListChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) DSColors.fairway else DSColors.cardWhite
    val fg = if (selected) Color.White else DSColors.inkGreen
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, if (selected) DSColors.fairway else DSColors.listCardBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun AllListRowCard(memo: V2Memo, onTap: () -> Unit) {
    val playable = remember(memo.videoUrl) { memoVideoPlayable(memo.videoUrl) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(16.dp))
            .clickable(onClick = onTap)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (memo.hasVideo) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DSColors.deepGreen),
                contentAlignment = Alignment.Center,
            ) {
                V2MemoThumbnail(memo = memo, modifier = Modifier.fillMaxSize())
                if (playable) {
                    Icon(Icons.Filled.PlayCircle, null, tint = Color.White.copy(0.85f), modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        "文件缺失",
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(0.54f))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(memo.topic.displayName, fontSize = 11.sp, color = DSColors.fairway, fontWeight = FontWeight.SemiBold)
            Text(
                memo.displayTitle ?: memo.title ?: "无标题",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.inkGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            memo.text?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 12.sp, color = DSColors.shadowGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (memo.isPinned) {
                Icon(Icons.Filled.Star, null, tint = DSColors.trophyGold, modifier = Modifier.size(14.dp))
            }
        }
    }
}
