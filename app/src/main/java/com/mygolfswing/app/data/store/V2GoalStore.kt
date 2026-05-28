package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2Topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
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

    private val memoStore = V2MemoStore

    /**
     * 进程启动时（[com.mygolfswing.app.V2App.onCreate]）应调用一次以启动收集。
     * 多次调用安全（启动后内部协程已活）。
     */
    @Volatile private var collecting = false

    fun startObserving() {
        if (collecting) return
        collecting = true
        scope.launch {
            repo.observeAll().onEach { latest ->
                _goals.value = latest
            }.stateIn(scope, SharingStarted.Eagerly, emptyList())
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

    fun update(goal: V2Goal) {
        scope.launch {
            repo.upsert(goal.copy(updatedAt = LocalDateTime.now()))
        }
    }

    fun delete(goalId: UUID) {
        scope.launch { repo.delete(goalId.toString()) }
    }

    fun checkIn(goalId: UUID): V2Goal? {
        val current = _goals.value.firstOrNull { it.id == goalId } ?: return null
        if (current.status != V2Goal.Status.ACTIVE) return null
        if (current.hasCheckedInToday) return null
        val now = LocalDateTime.now()
        val newCurrent = current.progress.current + 1
        val newProgress = current.progress.copy(
            current = newCurrent,
            lastIncrementAt = now,
            checkInDates = current.progress.checkInDates + now,
        )
        val total = current.targetSessions
        val isComplete = total != null && newCurrent >= total
        val updated = current.copy(
            progress = newProgress,
            status = if (isComplete) V2Goal.Status.COMPLETED else current.status,
            completedAt = if (isComplete) now else current.completedAt,
            updatedAt = now,
        )
        scope.launch { repo.upsert(updated) }
        return updated
    }

    fun pause(goalId: UUID) {
        val g = _goals.value.firstOrNull { it.id == goalId } ?: return
        scope.launch { repo.upsert(g.copy(status = V2Goal.Status.PAUSED, updatedAt = LocalDateTime.now())) }
    }

    fun resume(goalId: UUID) {
        val g = _goals.value.firstOrNull { it.id == goalId } ?: return
        scope.launch { repo.upsert(g.copy(status = V2Goal.Status.ACTIVE, updatedAt = LocalDateTime.now())) }
    }

    fun setFocused(goalId: UUID) {
        scope.launch {
            _goals.value.forEach { g ->
                val want = g.id == goalId
                if (g.todayFocus != want) {
                    repo.upsert(g.copy(todayFocus = want, updatedAt = LocalDateTime.now()))
                }
            }
        }
    }

    fun linkMemo(memoId: UUID, toGoalId: UUID) {
        val g = _goals.value.firstOrNull { it.id == toGoalId } ?: return
        scope.launch {
            repo.upsert(
                g.copy(
                    derivedFromMemoIds = (g.derivedFromMemoIds + memoId).distinct(),
                    updatedAt = LocalDateTime.now(),
                ),
            )
        }
    }

    fun unlinkMemo(memoId: UUID, fromGoalId: UUID) {
        val g = _goals.value.firstOrNull { it.id == fromGoalId } ?: return
        scope.launch {
            repo.upsert(
                g.copy(
                    derivedFromMemoIds = g.derivedFromMemoIds.filterNot { id -> id == memoId },
                    updatedAt = LocalDateTime.now(),
                ),
            )
        }
    }

    fun replaceLinkedMemos(goalId: UUID, memoIds: List<UUID>) {
        val g = _goals.value.firstOrNull { it.id == goalId } ?: return
        scope.launch {
            repo.upsert(
                g.copy(
                    derivedFromMemoIds = memoIds.distinct(),
                    updatedAt = LocalDateTime.now(),
                ),
            )
        }
    }

    /**
     * 测试 / 演示数据（仅在本地为空时往 DB 写一份）。
     * 上层 [com.mygolfswing.app.ui.tabs.V2RootScreen] 在启动时判空再调。
     */
    fun resetWithMockData() {
        scope.launch {
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
