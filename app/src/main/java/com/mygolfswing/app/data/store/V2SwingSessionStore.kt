package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.model.V2SwingClip
import com.mygolfswing.app.data.model.V2SwingClipRetention
import com.mygolfswing.app.data.model.V2SwingDayItem
import com.mygolfswing.app.data.model.V2SwingSession
import kotlinx.coroutines.CancellationException
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

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val pendingClipRetentions = mutableMapOf<UUID, V2SwingClipRetention>()
    private val pendingDeletedClipIds = mutableSetOf<UUID>()

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
        pendingClipRetentions.clear()
        pendingDeletedClipIds.clear()
        _lastError.value = null
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
                setSessions(applyPendingMutations(list))
            }.collect()
        }
    }

    fun addClipFromVideoImport(
        videoPath: String,
        thumbnailPath: String?,
        durationSec: Double,
        sourceLabel: String? = null,
    ) {
        scope.launch {
            addClipFromVideoImportAndWait(videoPath, thumbnailPath, durationSec, sourceLabel)
        }
    }

    suspend fun addClipFromVideoImportAndWait(
        videoPath: String,
        thumbnailPath: String?,
        durationSec: Double,
        sourceLabel: String? = null,
    ): UUID? {
        val uid = ServiceLocator.session.current().userId ?: return null
        return repo.addClip(uid, videoPath, thumbnailPath, durationSec, sourceLabel)
    }

    fun updateClipRetention(clipId: UUID, retention: V2SwingClipRetention) {
        val previousSessions = _sessions.value
        pendingClipRetentions[clipId] = retention
        _lastError.value = null
        setSessions(
            _sessions.value.map { session ->
                session.copy(
                    clips = session.clips.map { clip ->
                        if (clip.id == clipId) clip.copy(retention = retention) else clip
                    },
                )
            },
        )
        scope.launch {
            try {
                repo.updateClipRetention(clipId, retention)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                pendingClipRetentions.remove(clipId)
                setSessions(previousSessions)
                _lastError.value = swingStoreError("挥杆状态保存失败，请稍后重试", e.message)
            } finally {
                pendingClipRetentions.remove(clipId)
            }
        }
    }

    fun deleteClips(clipIds: Set<UUID>) {
        if (clipIds.isEmpty()) return
        val previousSessions = _sessions.value
        pendingDeletedClipIds += clipIds
        _lastError.value = null
        setSessions(removeClips(_sessions.value, clipIds))
        scope.launch {
            try {
                repo.deleteClips(clipIds)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                pendingDeletedClipIds -= clipIds
                setSessions(previousSessions)
                _lastError.value = swingStoreError("删除挥杆失败，请稍后重试", e.message)
            } finally {
                pendingDeletedClipIds -= clipIds
            }
        }
    }

    fun cleanupExpiredTemporaryClips() {
        scope.launch { repo.cleanupExpiredTemporaryClips() }
    }

    fun consumeLastError() {
        _lastError.value = null
    }

    private fun setSessions(sessions: List<V2SwingSession>) {
        _sessions.value = sessions
        _dayItems.value = buildDayItems(sessions, limit = 2)
        _allDayItems.value = buildDayItems(sessions, limit = 365)
    }

    private fun applyPendingMutations(sessions: List<V2SwingSession>): List<V2SwingSession> {
        val deletedIds = pendingDeletedClipIds.toSet()
        val retentionById = pendingClipRetentions.toMap()
        if (deletedIds.isEmpty() && retentionById.isEmpty()) return sessions
        return sessions.mapNotNull { session ->
            val clips = session.clips
                .filterNot { it.id in deletedIds }
                .map { clip ->
                    retentionById[clip.id]?.let { clip.copy(retention = it) } ?: clip
                }
            session.copy(clips = clips).takeIf { it.clips.isNotEmpty() }
        }
    }

    private fun removeClips(sessions: List<V2SwingSession>, clipIds: Set<UUID>): List<V2SwingSession> =
        sessions.mapNotNull { session ->
            val clips = session.clips.filterNot { it.id in clipIds }
            session.copy(clips = clips).takeIf { it.clips.isNotEmpty() }
        }

    private fun swingStoreError(base: String, debugDetail: String?): String {
        val detail = debugDetail.takeIf { ServiceLocator.isDebuggable && !it.isNullOrBlank() }
        return if (detail == null) base else "$base（$detail）"
    }

    private fun buildDayItems(sessions: List<V2SwingSession>, limit: Int): List<V2SwingDayItem> {
        val today = LocalDate.now()
        val sessionsByDay = sessions.groupBy { it.date }
        val byDay = sessions.flatMap { s -> s.clips.map { c -> s.date to c } }
            .groupBy({ it.first }, { it.second })
            .toSortedMap(compareByDescending { it })
        val zh = Locale.CHINA
        val monthFmt = DateTimeFormatter.ofPattern("M月", zh)
        return byDay.entries.take(limit).map { (day, clips) ->
            val sorted = clips.sortedBy { it.timestamp }
            val daySessions = sessionsByDay[day].orEmpty()
            val locations = daySessions.mapNotNull { it.locationName?.takeIf(String::isNotBlank) }.distinct()
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
                location = locations.firstOrNull(),
                extraLocationCount = (locations.size - 1).coerceAtLeast(0),
                timeRange = timeRange,
                isInProgress = false,
                sessionCount = daySessions.size.coerceAtLeast(1),
                clipCount = clips.size,
                clipPreviewIds = sorted.take(3).map { it.id },
                clipPreviewUrls = sorted.take(3).mapNotNull { it.thumbnailPath ?: it.videoPath },
                extraClipCount = (clips.size - 3).coerceAtLeast(0),
            )
        }
    }
}
