package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.model.V2SwingClip
import com.mygolfswing.app.data.model.V2SwingClipRetention
import com.mygolfswing.app.data.model.V2SwingDayItem
import com.mygolfswing.app.data.model.V2SwingSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

/** 挥杆记录 Store（本地 Room，对齐 iOS SwiftData `SwingSession` 主页展示） */
object V2SwingSessionStore {

    private val repo get() = ServiceLocator.swingRepo
    private val scope get() = ServiceLocator.appScope

    private val _sessions = MutableStateFlow<List<V2SwingSession>>(emptyList())
    val sessions: StateFlow<List<V2SwingSession>> = _sessions.asStateFlow()

    private val _dayItems = MutableStateFlow<List<V2SwingDayItem>>(emptyList())
    val dayItems: StateFlow<List<V2SwingDayItem>> = _dayItems.asStateFlow()

    private val _allDayItems = MutableStateFlow<List<V2SwingDayItem>>(emptyList())
    val allDayItems: StateFlow<List<V2SwingDayItem>> = _allDayItems.asStateFlow()

    val totalClipCount: Int get() = _sessions.value.sumOf { it.clips.size }

    fun permanentMediaPaths(): Set<String> =
        _sessions.value
            .flatMap { it.clips }
            .filter { it.retention.isPermanent }
            .flatMap { clip -> listOfNotNull(clip.videoPath, clip.thumbnailPath) }
            .mapNotNull { V2MemoBridge.localVideoAbsolutePath(it) }
            .toSet()

    fun resetInMemory() {
        _sessions.value = emptyList()
        _dayItems.value = emptyList()
        _allDayItems.value = emptyList()
    }

    fun clipsForDay(day: LocalDate): List<V2SwingClip> =
        _sessions.value
            .filter { it.date == day }
            .flatMap { it.clips }
            .sortedBy { it.timestamp }

    @Volatile private var collecting = false

    fun startObserving() {
        if (collecting) return
        collecting = true
        scope.launch {
            repo.observeAll().onEach { list ->
                _sessions.value = list
                _dayItems.value = buildDayItems(list, limit = 2)
                _allDayItems.value = buildDayItems(list, limit = 365)
            }.collect()
        }
    }

    fun addClipFromVideoImport(videoPath: String, thumbnailPath: String?, durationSec: Double) {
        scope.launch {
            val uid = ServiceLocator.session.current().userId ?: return@launch
            repo.addClip(uid, videoPath, thumbnailPath, durationSec)
        }
    }

    fun updateClipRetention(clipId: UUID, retention: V2SwingClipRetention) {
        scope.launch { repo.updateClipRetention(clipId, retention) }
    }

    fun deleteClips(clipIds: Set<UUID>) {
        scope.launch { repo.deleteClips(clipIds) }
    }

    fun cleanupExpiredTemporaryClips() {
        scope.launch { repo.cleanupExpiredTemporaryClips() }
    }

    private fun buildDayItems(sessions: List<V2SwingSession>, limit: Int): List<V2SwingDayItem> {
        val today = LocalDate.now()
        val byDay = sessions.flatMap { s -> s.clips.map { c -> s.date to c } }
            .groupBy({ it.first }, { it.second })
            .toSortedMap(compareByDescending { it })
        val zh = Locale.CHINA
        val monthFmt = DateTimeFormatter.ofPattern("M月", zh)
        return byDay.entries.take(limit).map { (day, clips) ->
            val sorted = clips.sortedBy { it.timestamp }
            val mins = (sorted.sumOf { it.durationSec } / 60).toInt().coerceAtLeast(1)
            val rel = when (ChronoUnit.DAYS.between(day, today)) {
                0L -> "今天"
                1L -> "昨天"
                else -> "${ChronoUnit.DAYS.between(day, today)}天前"
            }
            val timeRange = if (sorted.size >= 2) {
                val f = DateTimeFormatter.ofPattern("HH:mm", zh)
                "${f.format(sorted.first().timestamp)} – ${f.format(sorted.last().timestamp)}"
            } else sorted.firstOrNull()?.let {
                DateTimeFormatter.ofPattern("HH:mm", zh).format(it.timestamp)
            }
            V2SwingDayItem(
                id = UUID.nameUUIDFromBytes(day.toString().toByteArray()),
                day = day,
                dayNumber = day.dayOfMonth.toString(),
                monthLabel = monthFmt.format(day),
                relativeText = rel,
                title = "${clips.size} 个挥杆 · $mins 分钟",
                location = sessions.firstOrNull { it.date == day }?.locationName,
                timeRange = timeRange,
                clipCount = clips.size,
                clipPreviewUrls = sorted.take(3).mapNotNull { it.thumbnailPath ?: it.videoPath },
                extraClipCount = (clips.size - 3).coerceAtLeast(0),
            )
        }
    }
}
