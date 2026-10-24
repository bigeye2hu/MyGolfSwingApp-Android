package com.mygolfswing.app.data.download

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class V2DownloadStatisticsSnapshot(
    val todayDownloadCount: Int = 0,
    val last30DaysDownloadCount: Int = 0,
    val totalDownloadCount: Int = 0,
    val totalDownloadSizeBytes: Long = 0L,
) {
    val formattedTotalSize: String get() = formatDownloadTotalSize(totalDownloadSizeBytes)
}

internal data class V2LegacyDownloadRecord(
    val date: LocalDate,
    val fileSizeBytes: Long,
)

internal fun formatDownloadTotalSize(sizeBytes: Long): String {
    val sizeInMb = sizeBytes.coerceAtLeast(0L).toDouble() / 1024.0 / 1024.0
    return if (sizeInMb >= 1024.0) {
        String.format(Locale.US, "%.1fGB", sizeInMb / 1024.0)
    } else {
        String.format(Locale.US, "%.1fMB", sizeInMb)
    }
}

internal fun decodeDailyDownloadCounts(raw: String): Map<LocalDate, Int> =
    raw.lineSequence().mapNotNull { row ->
        val separator = row.indexOf('=')
        if (separator <= 0 || separator >= row.lastIndex) return@mapNotNull null
        val date = runCatching { LocalDate.parse(row.substring(0, separator)) }.getOrNull() ?: return@mapNotNull null
        val count = row.substring(separator + 1).toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        date to count
    }.toMap()

internal fun encodeDailyDownloadCounts(counts: Map<LocalDate, Int>): String =
    counts.filterValues { it > 0 }.toSortedMap().entries.joinToString("\n") { (date, count) -> "$date=$count" }

internal fun recentDownloadCounts(
    counts: Map<LocalDate, Int>,
    today: LocalDate,
): Map<LocalDate, Int> {
    val oldest = today.minusDays(29)
    return counts.filterKeys { date -> !date.isBefore(oldest) && !date.isAfter(today) }
}

internal fun downloadStatisticsSnapshot(
    totalDownloadCount: Int,
    totalDownloadSizeBytes: Long,
    dailyCounts: Map<LocalDate, Int>,
    today: LocalDate,
): V2DownloadStatisticsSnapshot {
    val recent = recentDownloadCounts(dailyCounts, today)
    return V2DownloadStatisticsSnapshot(
        todayDownloadCount = recent[today] ?: 0,
        last30DaysDownloadCount = recent.values.sum(),
        totalDownloadCount = totalDownloadCount.coerceAtLeast(0),
        totalDownloadSizeBytes = totalDownloadSizeBytes.coerceAtLeast(0L),
    )
}

/** Android counterpart of iOS `DownloadStatisticsManager`. */
internal object V2DownloadStatisticsStore {
    private const val PREFS_NAME = "v2_download_statistics"
    private const val KEY_TOTAL_COUNT = "total_count"
    private const val KEY_TOTAL_SIZE = "total_size_bytes"
    private const val KEY_DAILY_COUNTS = "daily_counts"
    private const val KEY_LEGACY_MIGRATED = "legacy_memos_migrated"

    private val lock = Any()
    private val mutableStats = MutableStateFlow(V2DownloadStatisticsSnapshot())
    val stats: StateFlow<V2DownloadStatisticsSnapshot> = mutableStats.asStateFlow()

    fun refresh(context: Context, today: LocalDate = LocalDate.now()): V2DownloadStatisticsSnapshot = synchronized(lock) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val decoded = decodeDailyDownloadCounts(prefs.getString(KEY_DAILY_COUNTS, "").orEmpty())
        val recent = recentDownloadCounts(decoded, today)
        if (recent != decoded) prefs.edit { putString(KEY_DAILY_COUNTS, encodeDailyDownloadCounts(recent)) }
        downloadStatisticsSnapshot(
            totalDownloadCount = prefs.getInt(KEY_TOTAL_COUNT, 0),
            totalDownloadSizeBytes = prefs.getLong(KEY_TOTAL_SIZE, 0L),
            dailyCounts = recent,
            today = today,
        ).also { mutableStats.value = it }
    }

    fun recordDownload(
        context: Context,
        fileSizeBytes: Long,
        today: LocalDate = LocalDate.now(),
    ): V2DownloadStatisticsSnapshot = synchronized(lock) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val daily = recentDownloadCounts(
            decodeDailyDownloadCounts(prefs.getString(KEY_DAILY_COUNTS, "").orEmpty()),
            today,
        ).toMutableMap()
        daily[today] = (daily[today] ?: 0) + 1
        val totalCount = prefs.getInt(KEY_TOTAL_COUNT, 0) + 1
        val totalSize = (prefs.getLong(KEY_TOTAL_SIZE, 0L) + fileSizeBytes.coerceAtLeast(0L)).coerceAtLeast(0L)
        prefs.edit {
            putInt(KEY_TOTAL_COUNT, totalCount)
            putLong(KEY_TOTAL_SIZE, totalSize)
            putString(KEY_DAILY_COUNTS, encodeDailyDownloadCounts(daily))
            putBoolean(KEY_LEGACY_MIGRATED, true)
        }
        downloadStatisticsSnapshot(totalCount, totalSize, daily, today).also { mutableStats.value = it }
    }

    fun migrateLegacyMemosIfNeeded(
        context: Context,
        records: List<V2LegacyDownloadRecord>,
        today: LocalDate = LocalDate.now(),
    ): V2DownloadStatisticsSnapshot = synchronized(lock) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_LEGACY_MIGRATED, false)) {
            val daily = recentDownloadCounts(
                records.groupingBy(V2LegacyDownloadRecord::date).eachCount(),
                today,
            )
            prefs.edit {
                putInt(KEY_TOTAL_COUNT, records.size)
                putLong(KEY_TOTAL_SIZE, records.sumOf { it.fileSizeBytes.coerceAtLeast(0L) })
                putString(KEY_DAILY_COUNTS, encodeDailyDownloadCounts(daily))
                putBoolean(KEY_LEGACY_MIGRATED, true)
            }
        }
        val daily = recentDownloadCounts(
            decodeDailyDownloadCounts(prefs.getString(KEY_DAILY_COUNTS, "").orEmpty()),
            today,
        )
        downloadStatisticsSnapshot(
            totalDownloadCount = prefs.getInt(KEY_TOTAL_COUNT, 0),
            totalDownloadSizeBytes = prefs.getLong(KEY_TOTAL_SIZE, 0L),
            dailyCounts = daily,
            today = today,
        ).also { mutableStats.value = it }
    }
}
