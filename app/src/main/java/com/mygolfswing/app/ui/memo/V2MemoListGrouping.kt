package com.mygolfswing.app.ui.memo

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** 对齐 iOS `V2MemoListGrouping` */
object V2MemoListGrouping {

  private val zh = Locale.CHINA
  private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", zh)
  private val dayFmt = DateTimeFormatter.ofPattern("M月d日", zh)
  private val dayTimeFmt = DateTimeFormatter.ofPattern("M月d日 HH:mm", zh)
  private val yearDayTimeFmt = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", zh)
  private val monthFmt = DateTimeFormatter.ofPattern("M月", zh)
  private val yearMonthFmt = DateTimeFormatter.ofPattern("yyyy年M月", zh)
  private val weekdayFmt = DateTimeFormatter.ofPattern("EEE", zh)

  fun headerKey(date: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
    val d = date.toLocalDate()
    val today = now.toLocalDate()
    if (d == today) return "今天"
    if (d == today.minusDays(1)) return "昨天"
    val days = ChronoUnit.DAYS.between(d, today)
    if (days in 2..7) return "本周（2-7天）"
    if (d.year == today.year && d.month == today.month) return "本月"
    return if (d.year == today.year) monthFmt.format(d) else yearMonthFmt.format(d)
  }

  fun dateBlockText(date: LocalDateTime): Pair<String, String> =
    dayFmt.format(date) to weekdayFmt.format(date)

  fun metaPrefix(date: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
    val d = date.toLocalDate()
    val today = now.toLocalDate()
    val time = timeFmt.format(date)
    return when (d) {
      today -> "今天 $time"
      today.minusDays(1) -> "昨天 $time"
      else -> {
        if (d.year == today.year) dayTimeFmt.format(date) else yearDayTimeFmt.format(date)
      }
    }
  }
}
