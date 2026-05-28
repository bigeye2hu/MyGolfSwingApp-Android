package com.mygolfswing.app.data.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID

/**
 * V2 Goal 数据模型（对齐 iOS `V2Goal.swift`）。
 *
 * - 完整复刻 iOS 的 metric / progress / cadence / status / priority / clubKind 等子结构
 * - 计算属性（ratio / stage / progressDisplay / metricDisplay 等）也保持同名同语义
 */
data class V2Goal(
    val id: UUID = UUID.randomUUID(),
    var title: String,
    var iconType: IconType,
    var topic: V2Topic,
    var metric: Metric,
    var progress: Progress = Progress(),

    /** 目标总打卡次数（nil 表示无总次数限制：free / 永久 类目标） */
    var targetSessions: Int? = null,
    /** 打卡单位（"次" / "天" / null）。null 时默认按"次"展示。 */
    var sessionUnit: String? = null,
    /** 关联的球杆 / 类型分类（来自新建目标 9 chip） */
    var clubKind: ClubKind? = null,

    var pinState: V2PinState = V2PinState.NONE,
    var status: Status = Status.ACTIVE,
    var priority: Priority = Priority.NORMAL,
    var cadence: Cadence? = null,

    /** 派生来源 Memo ID 列表（多对多） */
    var derivedFromMemoIds: List<UUID> = emptyList(),
    /** 是否进入主页 Hero 聚焦卡（同一时刻仅一个 todayFocus = true） */
    var todayFocus: Boolean = false,
    /** 完成庆祝动画是否已播放 */
    var completionCelebrated: Boolean = false,

    val startedAt: LocalDateTime = LocalDateTime.now(),
    var completedAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {

    // ============ 嵌套类型 ============

    enum class IconType { TARGET, CLUB, PUTT, SAND, TIP }

    enum class ClubKind(val displayName: String) {
        DRIVER("一号木"),
        FAIRWAY_WOOD("球道木"),
        IRON("铁杆"),
        WEDGE("挖起杆"),
        PUTTER("推杆"),
        SAND("沙坑"),
        CHIP("切杆"),
        GREEN_READING("果岭阅读"),
        OTHER("其他");

        val topic: V2Topic
            get() = when (this) {
                DRIVER, FAIRWAY_WOOD, IRON, OTHER -> V2Topic.SWING
                WEDGE, CHIP -> V2Topic.SHORT_GAME
                PUTTER, GREEN_READING -> V2Topic.PUTTING
                SAND -> V2Topic.SAND
            }

        val iconType: IconType
            get() = when (this) {
                DRIVER, FAIRWAY_WOOD, IRON, WEDGE, CHIP -> IconType.CLUB
                PUTTER, GREEN_READING -> IconType.PUTT
                SAND -> IconType.SAND
                OTHER -> IconType.TARGET
            }
    }

    enum class Status(val displayText: String) {
        ACTIVE("目标进行中"),
        PAUSED("已暂停"),
        COMPLETED("已完成"),
        ARCHIVED("已归档"),
    }

    enum class Priority(
        val displayName: String,
        val shortName: String,
        val emoji: String,
    ) {
        CRITICAL("最高优先级", "最高", "🔴"),
        HIGH("高优先级", "高", "🟠"),
        NORMAL("中优先级", "中", "🟡"),
        LOW("低优先级", "低", "⚪"),
    }

    /**
     * 训练节奏 / 时间范围（密封类等价 iOS Swift enum with associated values）
     */
    sealed class Cadence {
        data class DaysStreak(val days: Int) : Cadence()
        data class PerWeek(val times: Int) : Cadence()
        data class WithinMonths(val months: Int) : Cadence()
        data object Continuous : Cadence()

        val displayText: String
            get() = when (this) {
                is DaysStreak -> "${days}天连续练习"
                is PerWeek -> "每周${times}次"
                is WithinMonths -> "${months}个月"
                Continuous -> "永久"
            }
    }

    data class Metric(
        val kind: Kind,
        val target: Double,
        val unit: String? = null,
    ) {
        enum class Kind { COUNT, DURATION, DISTANCE, FREE }
    }

    data class Progress(
        var current: Double = 0.0,
        var lastIncrementAt: LocalDateTime? = null,
        var checkInDates: List<LocalDateTime> = emptyList(),
    )

    enum class Stage { SILENT, NEAR_COMPLETE, CELEBRATION }

    // ============ 计算属性 ============

    /** 完成比例 0...1，free 类型恒为 0 */
    val ratio: Double
        get() {
            targetSessions?.takeIf { it > 0 }?.let {
                return (progress.current / it.toDouble()).coerceAtMost(1.0)
            }
            if (metric.kind == Metric.Kind.FREE) return 0.0
            if (metric.target <= 0) return 0.0
            return (progress.current / metric.target).coerceAtMost(1.0)
        }

    val stage: Stage
        get() {
            if (status == Status.COMPLETED) return Stage.CELEBRATION
            val r = ratio
            return when {
                r >= 1.0 -> Stage.CELEBRATION
                r >= 0.8 -> Stage.NEAR_COMPLETE
                else -> Stage.SILENT
            }
        }

    val isPinned: Boolean get() = pinState == V2PinState.PINNED || pinState == V2PinState.CANCELLING
    val isCompleted: Boolean get() = status == Status.COMPLETED

    /** 今天是否已打卡 */
    val hasCheckedInToday: Boolean
        get() {
            val last = progress.lastIncrementAt ?: return false
            return last.toLocalDate() == LocalDate.now()
        }

    /** "3 / 5 次" 或 "已 X 次" */
    val sessionProgressDisplay: String
        get() {
            val curr = progress.current.toInt()
            val unit = sessionUnit ?: "次"
            val total = targetSessions
            return if (total != null) "$curr / $total $unit" else "已 $curr $unit"
        }

    /** "50 球" / "40 分钟" / "±0.3 米" / null(free) */
    val metricDisplay: String?
        get() = when (metric.kind) {
            Metric.Kind.COUNT -> "${formatNumber(metric.target)}${metric.unit ?: "球"}"
            Metric.Kind.DURATION -> "${formatNumber(metric.target)} ${metric.unit ?: "分钟"}"
            Metric.Kind.DISTANCE -> "±${formatNumber(metric.target)} ${metric.unit ?: "米"}"
            Metric.Kind.FREE -> null
        }

    /** 球杆 / 类型标签 */
    val typeLabel: String
        get() = clubKind?.displayName ?: topic.displayName

    /** 本周打卡总次数 */
    val weekCheckInCount: Int
        get() {
            if (progress.checkInDates.isEmpty()) return 0
            val today = LocalDate.now()
            val weekField = WeekFields.of(Locale.CHINA).weekOfWeekBasedYear()
            val currentWeek = today.get(weekField)
            val currentYear = today.year
            return progress.checkInDates.count {
                val d = it.toLocalDate()
                d.year == currentYear && d.get(weekField) == currentWeek
            }
        }

    /** 连续打卡天数 */
    val currentStreakDays: Int
        get() {
            if (progress.checkInDates.isEmpty()) return 0
            val checkedDays: Set<LocalDate> = progress.checkInDates.map { it.toLocalDate() }.toSet()
            val today = LocalDate.now()
            var probe: LocalDate = when {
                checkedDays.contains(today) -> today
                checkedDays.contains(today.minusDays(1)) -> today.minusDays(1)
                else -> return 0
            }
            var streak = 0
            while (checkedDays.contains(probe)) {
                streak += 1
                probe = probe.minusDays(1)
            }
            return streak
        }

    /** 距完成还差 N 次（仅在有 targetSessions 时返回） */
    val remainingSessions: Int?
        get() {
            val total = targetSessions ?: return null
            return (total - progress.current.toInt()).coerceAtLeast(0)
        }

    val deadlineDisplay: String
        get() {
            (cadence as? Cadence.WithinMonths)?.let { c ->
                val date = startedAt.plusMonths(c.months.toLong())
                return "%d.%02d.%02d".format(date.year, date.monthValue, date.dayOfMonth)
            }
            return "长期目标"
        }

    val metricKindDisplay: String
        get() = when (metric.kind) {
            Metric.Kind.COUNT -> "次数"
            Metric.Kind.DURATION -> "时长"
            Metric.Kind.DISTANCE -> "精度"
            Metric.Kind.FREE -> "自由"
        }

    val targetSessionsDisplay: String
        get() = targetSessions?.let { "$it ${sessionUnit ?: "次"}" } ?: "无限制"

    private fun formatNumber(v: Double): String =
        if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)

    companion object {
        /** 新建次数类目标 */
        fun count(
            title: String,
            iconType: IconType = IconType.TARGET,
            topic: V2Topic,
            target: Double,
            unit: String = "球",
            priority: Priority = Priority.NORMAL,
            cadence: Cadence? = null,
            targetSessions: Int? = null,
            sessionUnit: String? = null,
            clubKind: ClubKind? = null,
        ) = V2Goal(
            title = title,
            iconType = iconType,
            topic = topic,
            metric = Metric(Metric.Kind.COUNT, target, unit),
            targetSessions = targetSessions,
            sessionUnit = sessionUnit,
            clubKind = clubKind,
            priority = priority,
            cadence = cadence,
        )

        fun duration(
            title: String,
            iconType: IconType = IconType.CLUB,
            topic: V2Topic,
            targetMinutes: Double,
            priority: Priority = Priority.NORMAL,
            cadence: Cadence? = null,
            targetSessions: Int? = null,
            sessionUnit: String? = null,
            clubKind: ClubKind? = null,
        ) = V2Goal(
            title = title,
            iconType = iconType,
            topic = topic,
            metric = Metric(Metric.Kind.DURATION, targetMinutes, "分钟"),
            targetSessions = targetSessions,
            sessionUnit = sessionUnit,
            clubKind = clubKind,
            priority = priority,
            cadence = cadence,
        )

        fun distance(
            title: String,
            iconType: IconType = IconType.PUTT,
            topic: V2Topic,
            precisionMeters: Double,
            priority: Priority = Priority.NORMAL,
            cadence: Cadence? = null,
            targetSessions: Int? = null,
            sessionUnit: String? = null,
            clubKind: ClubKind? = null,
        ) = V2Goal(
            title = title,
            iconType = iconType,
            topic = topic,
            metric = Metric(Metric.Kind.DISTANCE, precisionMeters, "米"),
            targetSessions = targetSessions,
            sessionUnit = sessionUnit,
            clubKind = clubKind,
            priority = priority,
            cadence = cadence,
        )

        fun free(
            title: String,
            iconType: IconType = IconType.TIP,
            topic: V2Topic,
            priority: Priority = Priority.NORMAL,
            clubKind: ClubKind? = null,
        ) = V2Goal(
            title = title,
            iconType = iconType,
            topic = topic,
            metric = Metric(Metric.Kind.FREE, 0.0, null),
            cadence = Cadence.Continuous,
            priority = priority,
            clubKind = clubKind,
        )
    }
}

/** 用于 ChronoUnit.DAYS 等辅助计算（保留扩展点） */
internal fun daysBetween(a: LocalDate, b: LocalDate): Long = ChronoUnit.DAYS.between(a, b)
