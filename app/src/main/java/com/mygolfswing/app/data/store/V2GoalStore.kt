package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2Topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

/**
 * V2 目标 Store（对齐 iOS `V2GoalStore`）。
 *
 * 现已切换为 **Room 持久化**：
 * - [goals] 来自 [V2GoalRepository.observeAll]
 * - 所有 mutation 方法保持同名 + 同步签名（不阻塞 UI）；
 *   内部把目标 [com.mygolfswing.app.data.repo.V2GoalRepository.upsert] 投递到 appScope
 *
 * 这样上层 Compose 不用感知 suspend / Flow，迁移成本最小。
 */
object V2GoalStore {

    private val repo get() = ServiceLocator.goalRepo
    private val scope: CoroutineScope get() = ServiceLocator.appScope

    /** 内存快照（由 Room Flow 喂入）。计算属性都基于此 */
    private val _goals = MutableStateFlow<List<V2Goal>>(emptyList())
    val goals: StateFlow<List<V2Goal>> = _goals.asStateFlow()

    private val pendingDeletedGoalIds = mutableSetOf<UUID>()
    @Volatile private var inMemoryTestingSnapshotActive = false

    private val memoStore = V2MemoStore

    fun resetInMemory() {
        inMemoryTestingSnapshotActive = false
        _goals.value = emptyList()
        pendingDeletedGoalIds.clear()
    }

    /** 仅供 UI 自动化用例注入稳定快照，不触发 Room 或云端写入。 */
    internal fun replaceInMemoryForTesting(goals: List<V2Goal>) {
        // Instrumented tests run inside the real Application, where the Room
        // collector is already active. Ignore Room emissions until reset so a
        // deterministic page snapshot is not replaced midway through a test.
        inMemoryTestingSnapshotActive = true
        _goals.value = goals
        pendingDeletedGoalIds.clear()
    }

    /**
     * 进程启动时（[com.mygolfswing.app.V2App.onCreate]）应调用一次以启动收集。
     * 多次调用安全（启动后内部协程已活）。
     */
    @Volatile private var collecting = false

    fun startObserving() {
        if (collecting) return
        collecting = true
        scope.launch {
            repo.observeAll().collect { latest ->
                if (!inMemoryTestingSnapshotActive) {
                    _goals.value = mergeRoomGoals(latest)
                }
            }
        }
    }

    /**
     * Room 推送可能早于 [upsert] 完成，避免覆盖 [checkIn] 等操作的乐观更新（对齐 iOS 先 save 再 reload）。
     */
    private fun mergeRoomGoals(room: List<V2Goal>): List<V2Goal> {
        val pendingDeleted = pendingDeletedGoalIds.toSet()
        val visibleRoom = if (pendingDeleted.isEmpty()) room else room.filterNot { it.id in pendingDeleted }
        val memoryById = _goals.value.associateBy { it.id }
        if (memoryById.isEmpty()) return visibleRoom
        return visibleRoom.map { row ->
            val mem = memoryById[row.id] ?: return@map row
            if (mem.updatedAt.isAfter(row.updatedAt)) mem else row
        }
    }

    // ============ 派生属性 ============

    val activeGoals: List<V2Goal>
        get() = _goals.value.filter { it.status == V2Goal.Status.ACTIVE }

    val completedGoals: List<V2Goal>
        get() = _goals.value.filter { it.status == V2Goal.Status.COMPLETED }

    val focusedGoal: V2Goal?
        get() = activeGoals.firstOrNull { it.todayFocus } ?: activeGoals.firstOrNull()

    val activeCount: Int get() = activeGoals.size
    val completedCount: Int get() = completedGoals.size

    val totalLinkedMemoCount: Int
        get() = _goals.value.sumOf { it.derivedFromMemoIds.size }

    val weeklyCheckInCount: Int
        get() = _goals.value.sumOf { it.weekCheckInCount }

    data class PriorityGroup(val priority: V2Goal.Priority, val goals: List<V2Goal>)

    val groupedActiveByPriority: List<PriorityGroup>
        get() {
            val active = activeGoals
            val critical = active.filter { it.priority == V2Goal.Priority.CRITICAL }
            val high = active.filter { it.priority == V2Goal.Priority.HIGH }
            val normalOrLow = active.filter {
                it.priority == V2Goal.Priority.NORMAL || it.priority == V2Goal.Priority.LOW
            }
            return listOfNotNull(
                if (critical.isNotEmpty()) PriorityGroup(V2Goal.Priority.CRITICAL, critical) else null,
                if (high.isNotEmpty()) PriorityGroup(V2Goal.Priority.HIGH, high) else null,
                if (normalOrLow.isNotEmpty()) PriorityGroup(V2Goal.Priority.NORMAL, normalOrLow) else null,
            )
        }

    fun listedGoalsForHome(maxRows: Int = 2): List<V2Goal> {
        val active = activeGoals
        val focused = focusedGoal
        return active.filter { it.id != focused?.id }.take(maxRows)
    }

    fun remainingGoalCountForHome(maxRows: Int = 2): Int {
        val active = activeGoals
        val focused = focusedGoal
        val visible = active.filter { it.id != focused?.id }.size
        return (visible - maxRows).coerceAtLeast(0)
    }

    fun linkedMemoCount(goal: V2Goal): Int = goal.derivedFromMemoIds.size

    fun linkedVideoCount(goal: V2Goal): Int {
        val ids = goal.derivedFromMemoIds.toSet()
        return memoStore.allMemos.value.count { it.id in ids && it.hasVideo }
    }

    fun linkedMemos(goal: V2Goal): List<V2Memo> {
        val ids = goal.derivedFromMemoIds.toSet()
        return memoStore.allMemos.value.filter { it.id in ids }
    }

    // ============ 增删改（异步持久化，UI 端无需 await） ============

    fun create(goal: V2Goal) {
        val incoming = if (goal.todayFocus) {
            goal
        } else if (_goals.value.none { it.todayFocus }) {
            goal.copy(todayFocus = true)
        } else {
            goal
        }
        scope.launch {
            // 如果新目标是 focus，先把其它取消（异步批量）
            if (incoming.todayFocus) {
                _goals.value.filter { it.todayFocus }.forEach {
                    repo.upsert(it.copy(todayFocus = false, updatedAt = LocalDateTime.now()))
                }
            }
            repo.upsert(incoming)
        }
    }

    suspend fun createAndWait(goal: V2Goal): UUID {
        val incoming = if (goal.todayFocus) {
            goal
        } else if (_goals.value.none { it.todayFocus }) {
            goal.copy(todayFocus = true)
        } else {
            goal
        }
        if (incoming.todayFocus) {
            _goals.value.filter { it.todayFocus }.forEach {
                repo.upsert(it.copy(todayFocus = false, updatedAt = LocalDateTime.now()))
            }
        }
        repo.upsert(incoming)
        val goals = _goals.value.map {
            if (incoming.todayFocus && it.todayFocus && it.id != incoming.id) {
                it.copy(todayFocus = false, updatedAt = LocalDateTime.now())
            } else {
                it
            }
        }
        _goals.value = listOf(incoming) + goals.filterNot { it.id == incoming.id }
        return incoming.id
    }

    fun update(goal: V2Goal) {
        val now = LocalDateTime.now()
        val updated = recalculateGoalStatusAfterEdit(goal.copy(updatedAt = now), now)
        _goals.value = _goals.value.map { if (it.id == updated.id) updated else it }
        scope.launch {
            repo.upsert(updated)
        }
    }

    suspend fun updateAndWait(goal: V2Goal): UUID {
        val now = LocalDateTime.now()
        val updated = recalculateGoalStatusAfterEdit(goal.copy(updatedAt = now), now)
        repo.upsert(updated)
        _goals.value = _goals.value.map { if (it.id == updated.id) updated else it }
        return updated.id
    }

    fun delete(goalId: UUID) {
        pendingDeletedGoalIds += goalId
        _goals.value = _goals.value.filterNot { it.id == goalId }
        scope.launch {
            try {
                repo.delete(goalId.toString())
            } finally {
                pendingDeletedGoalIds -= goalId
            }
        }
    }

    suspend fun deleteAndWait(goalId: UUID) {
        repo.delete(goalId.toString())
        _goals.value = _goals.value.filterNot { it.id == goalId }
    }

    /**
     * 打卡 +1（对齐 iOS `V2GoalStore.checkIn`）。
     *
     * @return 是否成功打卡（今日已打 / 非 active 返回 false）
     */
    fun checkIn(goalId: UUID): Boolean {
        val current = _goals.value.firstOrNull { it.id == goalId } ?: return false
        if (current.status != V2Goal.Status.ACTIVE) return false
        if (current.hasCheckedInToday) return false
        val now = LocalDateTime.now()
        val newCurrent = current.progress.current + 1
        val newProgress = current.progress.copy(
            current = newCurrent,
            lastIncrementAt = now,
            checkInDates = current.progress.checkInDates + now,
        )
        val isComplete = isGoalReached(current, newCurrent)
        val updated = current.copy(
            progress = newProgress,
            status = if (isComplete) V2Goal.Status.COMPLETED else current.status,
            completedAt = if (isComplete) now else current.completedAt,
            updatedAt = now,
        )
        // 乐观更新 UI（iOS 同步 reload）；Room Flow 随后会再对齐一次
        _goals.value = _goals.value.map { if (it.id == goalId) updated else it }
        scope.launch { repo.upsert(updated) }
        return true
    }

    /** 完成判定：优先 targetSessions，否则回退 metric.target（与 iOS 一致） */
    private fun isGoalReached(goal: V2Goal, progressCurrent: Double): Boolean {
        goal.targetSessions?.takeIf { it > 0 }?.let { total ->
            return progressCurrent >= total
        }
        if (goal.metric.kind == V2Goal.Metric.Kind.FREE) return false
        if (goal.metric.target <= 0) return false
        return progressCurrent >= goal.metric.target
    }

    fun pause(goalId: UUID) {
        val g = _goals.value.firstOrNull { it.id == goalId } ?: return
        if (g.status != V2Goal.Status.ACTIVE) return
        val updated = g.copy(status = V2Goal.Status.PAUSED, updatedAt = LocalDateTime.now())
        _goals.value = _goals.value.map { if (it.id == goalId) updated else it }
        scope.launch { repo.upsert(updated) }
    }

    fun resume(goalId: UUID) {
        val g = _goals.value.firstOrNull { it.id == goalId } ?: return
        if (g.status != V2Goal.Status.PAUSED) return
        val updated = g.copy(status = V2Goal.Status.ACTIVE, updatedAt = LocalDateTime.now())
        _goals.value = _goals.value.map { if (it.id == goalId) updated else it }
        scope.launch { repo.upsert(updated) }
    }

    fun setFocused(goalId: UUID) {
        val now = LocalDateTime.now()
        val changed = _goals.value.mapNotNull { g ->
            val want = g.id == goalId
            if (g.todayFocus == want) {
                null
            } else {
                g.copy(todayFocus = want, updatedAt = now)
            }
        }
        if (changed.isEmpty()) return
        val changedById = changed.associateBy { it.id }
        _goals.value = _goals.value.map { changedById[it.id] ?: it }
        scope.launch { changed.forEach { repo.upsert(it) } }
    }

    fun linkMemo(memoId: UUID, toGoalId: UUID) {
        val g = _goals.value.firstOrNull { it.id == toGoalId } ?: return
        val updated = g.copy(
            derivedFromMemoIds = (g.derivedFromMemoIds + memoId).distinct(),
            updatedAt = LocalDateTime.now(),
        )
        if (updated.derivedFromMemoIds == g.derivedFromMemoIds) return
        _goals.value = _goals.value.map { if (it.id == toGoalId) updated else it }
        scope.launch { repo.upsert(updated) }
    }

    fun unlinkMemo(memoId: UUID, fromGoalId: UUID) {
        val g = _goals.value.firstOrNull { it.id == fromGoalId } ?: return
        val updated = g.copy(
            derivedFromMemoIds = g.derivedFromMemoIds.filterNot { id -> id == memoId },
            updatedAt = LocalDateTime.now(),
        )
        if (updated.derivedFromMemoIds == g.derivedFromMemoIds) return
        _goals.value = _goals.value.map { if (it.id == fromGoalId) updated else it }
        scope.launch { repo.upsert(updated) }
    }

    fun replaceLinkedMemos(goalId: UUID, memoIds: List<UUID>) {
        val g = _goals.value.firstOrNull { it.id == goalId } ?: return
        val updated = g.copy(
            derivedFromMemoIds = memoIds.distinct(),
            updatedAt = LocalDateTime.now(),
        )
        if (updated.derivedFromMemoIds == g.derivedFromMemoIds) return
        _goals.value = _goals.value.map { if (it.id == goalId) updated else it }
        scope.launch { repo.upsert(updated) }
    }

    fun replaceMemoLinks(memoId: UUID, goalIds: Set<UUID>) {
        val selected = goalIds.toSet()
        val now = LocalDateTime.now()
        val changed = _goals.value.mapNotNull { goal ->
            val hasMemo = memoId in goal.derivedFromMemoIds
            val shouldHaveMemo = goal.id in selected
            if (hasMemo == shouldHaveMemo) return@mapNotNull null
            goal.copy(
                derivedFromMemoIds = if (shouldHaveMemo) {
                    (goal.derivedFromMemoIds + memoId).distinct()
                } else {
                    goal.derivedFromMemoIds.filterNot { it == memoId }
                },
                updatedAt = now,
            )
        }
        if (changed.isEmpty()) return
        val changedById = changed.associateBy { it.id }
        _goals.value = _goals.value.map { changedById[it.id] ?: it }
        scope.launch { changed.forEach { repo.upsert(it) } }
    }

    /** 登录后从云端拉取并合并（对齐 iOS `V2GoalStore.setup` 内的 cloud sync） */
    fun syncFromCloud() {
        scope.launch {
            ServiceLocator.goalRepo.syncFromCloud()
                .onSuccess { count ->
                    if (ServiceLocator.isDebuggable) {
                        android.util.Log.i("V2GoalStore", "云端目标同步完成：合并 $count 条")
                    }
                }
                .onFailure { e ->
                    if (ServiceLocator.isDebuggable) {
                        android.util.Log.w("V2GoalStore", "云端目标同步失败: ${e.message}")
                    }
                }
        }
    }

    private fun resetWithMockData() {
        scope.launch {
            if (!shouldUseGoalMockSeed(
                    hasExistingGoals = _goals.value.isNotEmpty(),
                    isDebuggable = ServiceLocator.isDebuggable,
                )
            ) {
                return@launch
            }
            val now = LocalDateTime.now()
            val mocks = buildList {
                add(
                    V2Goal.count(
                        title = "用 7 号铁打 50 球",
                        iconType = V2Goal.IconType.TARGET,
                        topic = V2Topic.SWING,
                        target = 50.0,
                        unit = "球",
                        priority = V2Goal.Priority.CRITICAL,
                        cadence = V2Goal.Cadence.PerWeek(5),
                        targetSessions = 5,
                        sessionUnit = "次",
                        clubKind = V2Goal.ClubKind.IRON,
                    ).copy(
                        todayFocus = true,
                        progress = V2Goal.Progress(
                            current = 3.0,
                            lastIncrementAt = now,
                            checkInDates = listOf(now, now.minusDays(1), now.minusDays(3)),
                        ),
                    ),
                )
                add(
                    V2Goal.duration(
                        title = "练习 40 分钟挥杆基础",
                        iconType = V2Goal.IconType.CLUB,
                        topic = V2Topic.SWING,
                        targetMinutes = 40.0,
                        priority = V2Goal.Priority.HIGH,
                        cadence = V2Goal.Cadence.PerWeek(3),
                        targetSessions = 3,
                        sessionUnit = "次",
                        clubKind = V2Goal.ClubKind.IRON,
                    ).copy(
                        progress = V2Goal.Progress(
                            current = 2.0,
                            lastIncrementAt = now.minusDays(1),
                            checkInDates = listOf(now.minusDays(1), now.minusDays(2)),
                        ),
                    ),
                )
                add(
                    V2Goal.distance(
                        title = "推杆 5 米精度训练",
                        iconType = V2Goal.IconType.PUTT,
                        topic = V2Topic.PUTTING,
                        precisionMeters = 0.3,
                        priority = V2Goal.Priority.CRITICAL,
                        cadence = V2Goal.Cadence.WithinMonths(1),
                        targetSessions = 4,
                        sessionUnit = "次",
                        clubKind = V2Goal.ClubKind.PUTTER,
                    ).copy(
                        progress = V2Goal.Progress(
                            current = 1.0,
                            lastIncrementAt = now.minusDays(4),
                            checkInDates = listOf(now.minusDays(4)),
                        ),
                    ),
                )
                add(
                    V2Goal.free(
                        title = "每日复盘心法",
                        iconType = V2Goal.IconType.TIP,
                        topic = V2Topic.MIND,
                        priority = V2Goal.Priority.NORMAL,
                    ),
                )
            }
            repo.bulkReplaceLocal(mocks)
        }
    }
}

/**
 * Re-evaluates only the active/completed boundary after an edit, matching iOS
 * `V2GoalStore.update`. Paused and archived goals retain their explicit state.
 */
internal fun recalculateGoalStatusAfterEdit(
    goal: V2Goal,
    now: LocalDateTime,
): V2Goal {
    val reached = goal.targetSessions
        ?.takeIf { it > 0 }
        ?.let { goal.progress.current >= it }
        ?: (
            goal.metric.kind != V2Goal.Metric.Kind.FREE &&
                goal.metric.target > 0 &&
                goal.progress.current >= goal.metric.target
            )
    return when {
        reached && goal.status == V2Goal.Status.ACTIVE -> goal.copy(
            status = V2Goal.Status.COMPLETED,
            completedAt = now,
        )
        !reached && goal.status == V2Goal.Status.COMPLETED -> goal.copy(
            status = V2Goal.Status.ACTIVE,
            completedAt = null,
            completionCelebrated = false,
        )
        else -> goal
    }
}

internal fun shouldUseGoalMockSeed(hasExistingGoals: Boolean, isDebuggable: Boolean): Boolean =
    !hasExistingGoals && isDebuggable
