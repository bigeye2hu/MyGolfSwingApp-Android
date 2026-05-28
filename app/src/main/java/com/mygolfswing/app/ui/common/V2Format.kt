package com.mygolfswing.app.ui.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * V2 通用日期 / 数字格式化工具（对齐 iOS `V2DateFormat`）。
 */
object V2Format {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** 早上好 / 上午好 / 下午好 / 晚上好 */
    fun greeting(at: LocalTime = LocalTime.now()): String = when (at.hour) {
        in 0..4 -> "夜深了"
        in 5..8 -> "早上好"
        in 9..11 -> "上午好"
        in 12..17 -> "下午好"
        else -> "晚上好"
    }

    /** 主页头部日期："3月15日 周三" */
    fun headerDate(date: LocalDate = LocalDate.now()): String {
        val fmt = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)
        return date.format(fmt)
    }

    /** "今天 14:30" / "昨天 09:20" / "5月15日 18:42" */
    fun timelineLabel(dt: LocalDateTime): String {
        val today = LocalDate.now()
        val d = dt.toLocalDate()
        val timeStr = dt.toLocalTime().format(timeFormatter)
        return when (d) {
            today -> "今天 $timeStr"
            today.minusDays(1) -> "昨天 $timeStr"
            else -> {
                val dayFmt = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
                "${d.format(dayFmt)} $timeStr"
            }
        }
    }

    fun formatNumber(v: Double): String =
        if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)
}
