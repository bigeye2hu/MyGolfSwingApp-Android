package com.mygolfswing.app.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.ui.common.V2NavTopBar
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import java.time.LocalDateTime
import java.util.UUID

/** 随手记二级列表（对齐 iOS `V2TextMemoListView`） */
@Composable
fun V2TextMemoListScreen(onBack: () -> Unit, onOpenMemo: (UUID) -> Unit) {
    val all by V2MemoStore.allMemos.collectAsState()
    var filterId by rememberSaveable { mutableStateOf("all") }
    val texts = remember(all) { all.filter { it.kind == V2MemoKind.TEXT }.sortedByDescending { it.createdAt } }
    val filtered = remember(texts, filterId) { texts.filter { matchesTextFilter(it, filterId) } }
    val stats = remember(texts) {
        val now = LocalDateTime.now()
        val weekAgo = now.minusDays(6)
        val weekNew = texts.count { it.createdAt >= weekAgo }
        val words = texts.sumOf { it.wordCount }
        "${texts.size} 条 · 本周新增 $weekNew · 共 $words 字"
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
        "all" to "全部", "insight" to "心法", "reflection" to "反思",
        "breakthrough" to "突破", "coach" to "教练点评", "pinned" to "★已收藏",
    )

    Column(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        V2NavTopBar(title = "随手记", onBack = onBack)
        V2MemoStatsBar(stats)
        V2MemoFilterChips(filters, filterId) { filterId = it }
        if (filtered.isEmpty()) {
            Text(
                if (filterId == "all") "还没有随手记" else "没有符合筛选的记录",
                modifier = Modifier.padding(DSSpacing.xl),
                color = DSColors.shadowGray,
            )
        } else {
            LazyColumn(Modifier.weight(1f)) {
                groups.forEach { (header, items) ->
                    item { V2MemoListSectionHeader(header) }
                    items(items, key = { it.id }) { memo ->
                        V2TextMemoLargeCard(
                            memo = memo,
                            modifier = Modifier
                                .padding(horizontal = DSSpacing.lg, vertical = 6.dp)
                                .clickable { onOpenMemo(memo.id) },
                        )
                    }
                }
                item {
                    Text("— 没有更多了 —", modifier = Modifier.padding(24.dp), fontSize = 12.sp, color = DSColors.shadowGray)
                }
            }
        }
    }
}
