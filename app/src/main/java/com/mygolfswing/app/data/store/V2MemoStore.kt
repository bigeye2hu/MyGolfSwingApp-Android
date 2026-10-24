package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.CancellationException
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

    private val pendingDeletedMemoIds = mutableSetOf<UUID>()
    private val pendingUpsertMemoIds = mutableSetOf<UUID>()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun resetInMemory() {
        _allMemos.value = emptyList()
        pendingDeletedMemoIds.clear()
        pendingUpsertMemoIds.clear()
        _isSyncing.value = false
    }

    internal fun replaceInMemoryForTesting(memos: List<V2Memo>) {
        _allMemos.value = memos.sortedByDescending { it.createdAt }
        pendingDeletedMemoIds.clear()
        pendingUpsertMemoIds.clear()
        _isSyncing.value = false
    }

    @Volatile private var collecting = false

    fun startObserving() {
        if (collecting) return
        collecting = true
        scope.launch {
            combine(repo.observeAll(), V2GoalStore.goals) { memos, goals ->
                val linkedGoalIdsByMemo = goals
                    .flatMap { goal -> goal.derivedFromMemoIds.map { memoId -> memoId to goal.id } }
                    .groupBy({ it.first }, { it.second })
                memos.map { memo ->
                    memo.copy(linkedGoalIds = linkedGoalIdsByMemo[memo.id].orEmpty().distinct())
                }
            }.onEach { latest -> _allMemos.value = mergeObservedMemos(latest) }.collect()
        }
    }

    private fun mergeObservedMemos(observed: List<V2Memo>): List<V2Memo> {
        val pendingDeleted = pendingDeletedMemoIds.toSet()
        val pendingUpserts = pendingUpsertMemoIds.toSet()
        val visibleObserved = if (pendingDeleted.isEmpty()) observed else observed.filterNot { it.id in pendingDeleted }
        val memoryById = _allMemos.value.associateBy { it.id }
        if (memoryById.isEmpty()) return visibleObserved
        val observedIds = visibleObserved.map { it.id }.toSet()
        val merged = visibleObserved.map { row ->
            val mem = memoryById[row.id] ?: return@map row
            if (mem.updatedAt.isAfter(row.updatedAt)) {
                mem
            } else {
                row
            }
        }
        val memoryOnly = _allMemos.value.filter { memo ->
            memo.id in pendingUpserts && memo.id !in observedIds && memo.id !in pendingDeleted
        }
        return (memoryOnly + merged).sortedByDescending { it.createdAt }
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
        pendingUpsertMemoIds += memo.id
        _allMemos.value = (listOf(memo) + _allMemos.value.filterNot { it.id == memo.id })
            .sortedByDescending { it.createdAt }
        scope.launch {
            try {
                repo.upsert(memo)
            } finally {
                pendingUpsertMemoIds -= memo.id
            }
        }
    }

    suspend fun createAndWait(memo: V2Memo): UUID {
        repo.upsert(memo)
        _allMemos.value = (listOf(memo) + _allMemos.value.filterNot { it.id == memo.id })
            .sortedByDescending { it.createdAt }
        return memo.id
    }

    fun update(memo: V2Memo) {
        val updated = memo.copy(updatedAt = LocalDateTime.now())
        pendingUpsertMemoIds += updated.id
        _allMemos.value = (listOf(updated) + _allMemos.value.filterNot { it.id == updated.id })
            .sortedByDescending { it.createdAt }
        scope.launch {
            try {
                repo.upsert(updated)
            } finally {
                pendingUpsertMemoIds -= updated.id
            }
        }
    }

    suspend fun updateAndWait(memo: V2Memo): UUID {
        val updated = memo.copy(updatedAt = LocalDateTime.now())
        repo.upsert(updated)
        _allMemos.value = (listOf(updated) + _allMemos.value.filterNot { it.id == updated.id })
            .sortedByDescending { it.createdAt }
        return updated.id
    }

    fun delete(memoId: UUID) {
        pendingDeletedMemoIds += memoId
        _allMemos.value = _allMemos.value.filterNot { it.id == memoId }
        scope.launch {
            try {
                repo.delete(memoId.toString())
            } finally {
                pendingDeletedMemoIds -= memoId
            }
        }
    }

    suspend fun deleteAndWait(memoId: UUID) {
        repo.delete(memoId.toString())
        _allMemos.value = _allMemos.value.filterNot { it.id == memoId }
    }

    fun pullFromCloud() {
        scope.launch {
            _isSyncing.value = true
            try {
                runMemoStoreSyncCatching { repo.deleteLocalDemoMemos() }
                runMemoStoreSyncCatching { repo.pullFromCloud() }
                runMemoStoreSyncCatching { repo.pushDirty() }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun deleteLocalDemoMemos() {
        scope.launch { repo.deleteLocalDemoMemos() }
    }

}

internal suspend fun <T> runMemoStoreSyncCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: Throwable) {
    if (e is CancellationException) throw e
    Result.failure(e)
}
