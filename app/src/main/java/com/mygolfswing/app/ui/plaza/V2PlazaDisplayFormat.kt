package com.mygolfswing.app.ui.plaza

import com.mygolfswing.app.data.model.V2PlazaVideo
import java.util.Locale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

const val V2_PLAZA_PUBLIC_VIEWS_THRESHOLD: Int = 50
const val V2_PLAZA_PUBLIC_FAVORITES_THRESHOLD: Int = 5

fun v2PlazaCompactCount(count: Int): String = when {
    count >= 10_000 -> String.format(Locale.US, "%.1f万", count / 10_000.0)
    count >= 1_000 -> String.format(Locale.US, "%.1fk", count / 1_000.0)
    else -> count.toString()
}

fun V2PlazaVideo.publicHomeMetaText(): String =
    if (viewCount >= V2_PLAZA_PUBLIC_VIEWS_THRESHOLD) {
        "${v2PlazaCompactCount(viewCount)} 次观看"
    } else {
        categoryDisplayName ?: "精选推荐"
    }

fun V2PlazaVideo.publicGridPrimaryMetaText(): String =
    if (viewCount >= V2_PLAZA_PUBLIC_VIEWS_THRESHOLD) {
        v2PlazaCompactCount(viewCount)
    } else {
        categoryDisplayName ?: "精选推荐"
    }

fun V2PlazaVideo.publicGridUsesBookmark(): Boolean =
    isSaved || saveCount >= V2_PLAZA_PUBLIC_FAVORITES_THRESHOLD

fun V2PlazaVideo.publicGridSecondaryMetaText(
    nowMs: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    if (isSaved) return "已收藏"
    if (saveCount >= V2_PLAZA_PUBLIC_FAVORITES_THRESHOLD) return "${v2PlazaCompactCount(saveCount)}人收藏"
    if (publishedAtMs <= 0L) return "精选"

    val published = Instant.ofEpochMilli(publishedAtMs).atZone(zoneId).toLocalDate()
    val today = Instant.ofEpochMilli(nowMs).atZone(zoneId).toLocalDate()
    if (published.isAfter(today)) return "刚刚"
    val days = ChronoUnit.DAYS.between(published, today)
    return when (days) {
        0L -> "今天"
        1L -> "昨天"
        in 2L..6L -> "${days}天前"
        else -> published.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))
    }
}
