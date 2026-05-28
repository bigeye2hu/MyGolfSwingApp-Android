package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoAccent
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.model.V2MemoThumbnailKind
import com.mygolfswing.app.data.model.V2Topic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

/**
 * V2 Memo Store（对齐 iOS `V2MemoViewModel`）。已切换为 Room 持久化。
 */
object V2MemoStore {

    private val repo get() = ServiceLocator.memoRepo
    private val scope get() = ServiceLocator.appScope

    private val _allMemos = MutableStateFlow<List<V2Memo>>(emptyList())
    val allMemos: StateFlow<List<V2Memo>> = _allMemos.asStateFlow()

    @Volatile private var collecting = false

    fun startObserving() {
        if (collecting) return
        collecting = true
        scope.launch {
            repo.observeAll().onEach { latest -> _allMemos.value = latest }.collect()
        }
    }

    val textMemos: List<V2Memo>
        get() = _allMemos.value.filter { it.kind == V2MemoKind.TEXT }
            .sortedByDescending { it.createdAt }

    val mixedMemos: List<V2Memo>
        get() = _allMemos.value.filter { it.kind == V2MemoKind.MIXED }
            .sortedByDescending { it.createdAt }

    /** 主页「视频笔记」横滑 row（VIDEO + MIXED，对齐 iOS 视频备忘录） */
    val videoRowMemos: List<V2Memo>
        get() = _allMemos.value
            .filter { it.kind == V2MemoKind.VIDEO || it.kind == V2MemoKind.MIXED }
            .sortedByDescending { it.createdAt }

    val videoMemos: List<V2Memo>
        get() = _allMemos.value.filter { it.kind == V2MemoKind.VIDEO }
            .sortedByDescending { it.createdAt }

    val totalVideoRowCount: Int
        get() = _allMemos.value.count { it.kind == V2MemoKind.VIDEO || it.kind == V2MemoKind.MIXED }

    val totalTextCount: Int
        get() = _allMemos.value.count { it.kind == V2MemoKind.TEXT }

    val totalCount: Int
        get() = totalVideoRowCount + totalTextCount

    fun memoById(id: UUID): V2Memo? = _allMemos.value.firstOrNull { it.id == id }

    fun recentMemos(limit: Int = 6): List<V2Memo> =
        _allMemos.value.sortedByDescending { it.createdAt }.take(limit)

    fun create(memo: V2Memo) {
        scope.launch { repo.upsert(memo) }
    }

    fun update(memo: V2Memo) {
        scope.launch { repo.upsert(memo.copy(updatedAt = LocalDateTime.now())) }
    }

    fun delete(memoId: UUID) {
        scope.launch { repo.delete(memoId.toString()) }
    }

    fun pullFromCloud() {
        scope.launch {
            runCatching { repo.pullFromCloud() }
            runCatching { repo.pushDirty() }
        }
    }

    fun resetWithMockData() {
        scope.launch {
            val now = LocalDateTime.now()
            val memos = listOf(
                V2Memo.textMemo(
                    title = "握把压力的微调",
                    body = "今天发现握把太用力会让手腕僵硬，松一点反而更顺。下次保持 6 分力试试。",
                    tags = listOf("挥杆", "握把"),
                    accent = V2MemoAccent.INSIGHT,
                    subText = "练习场",
                ).copy(createdAt = now.minusHours(2), updatedAt = now.minusHours(2), metaText = "今天 14:30 · 7号铁"),
                V2Memo.textMemo(
                    title = "推杆距离感的练习",
                    body = "5 米推杆把节奏拉长，先 1 下半挥杆，再回正。今天命中率明显提升。",
                    tags = listOf("推杆", "节奏"),
                    accent = V2MemoAccent.BREAKTHROUGH,
                    topic = V2Topic.PUTTING,
                ).copy(createdAt = now.minusDays(1), updatedAt = now.minusDays(1), metaText = "昨天 09:20 · 推杆"),
                V2Memo.textMemo(
                    title = "教练点评：上杆速度",
                    body = "教练说上杆太快了，建议加一拍。回去录一段慢动作对比。",
                    tags = listOf("教练点评"),
                    accent = V2MemoAccent.COACH_NOTE,
                ).copy(createdAt = now.minusDays(2), updatedAt = now.minusDays(2), metaText = "前天 18:42"),
                V2Memo.mixedMemo(
                    displayTitle = "下杆节奏练习 · 7号铁",
                    body = "录了 3 球。第 2 球节奏最稳，标记为 ⭐。",
                    durationSec = 12.5,
                    topic = V2Topic.SWING,
                    tags = listOf("挥杆", "7号铁"),
                    metaText = "今天 14:32 · 7号铁 · 50球",
                    thumbnailKind = V2MemoThumbnailKind.FAIRWAY,
                ).copy(createdAt = now.minusHours(1), updatedAt = now.minusHours(1)),
                V2Memo.mixedMemo(
                    displayTitle = "沙坑救球角度",
                    body = "杆面打开 30°，瞄球后 5cm 落地，效果不错。",
                    durationSec = 8.0,
                    topic = V2Topic.SAND,
                    tags = listOf("沙坑"),
                    metaText = "昨天 16:10 · 沙坑",
                    thumbnailKind = V2MemoThumbnailKind.SAND,
                ).copy(createdAt = now.minusDays(1).minusHours(3), updatedAt = now.minusDays(1).minusHours(3)),
            )
            repo.bulkReplaceLocal(memos)
        }
    }
}
