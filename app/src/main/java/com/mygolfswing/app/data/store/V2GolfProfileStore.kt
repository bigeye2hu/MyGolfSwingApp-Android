package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2GolfProfileData
import com.mygolfswing.app.data.model.V2ProfileDates
import com.mygolfswing.app.data.model.V2ProfileItem
import com.mygolfswing.app.data.model.V2ProfileSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * 用户高尔夫档案 Store（对齐 iOS `UserGolfProfileManager`）。
 */
object V2GolfProfileStore {

    private val repo get() = ServiceLocator.golfProfileRepo
    private val scope get() = ServiceLocator.appScope

    private val _profile = MutableStateFlow(V2GolfProfileData())
    val profile: StateFlow<V2GolfProfileData> = _profile.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val isProfileEmpty: Boolean get() = _profile.value.isEmpty

    @Volatile private var collecting = false

    fun resetInMemory() {
        _profile.value = V2GolfProfileData()
        _isSyncing.value = false
    }

    fun startObserving() {
        if (collecting) return
        collecting = true
        scope.launch {
            ServiceLocator.session.state.collect { session ->
                if (session.isLoggedIn) {
                    reloadFromLocal()
                } else {
                    resetInMemory()
                }
            }
        }
    }

    suspend fun reloadFromLocal() {
        _profile.value = repo.getLocal() ?: V2GolfProfileData()
    }

    suspend fun refreshFromCloud() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        try {
            repo.pullFromCloud().onSuccess { cloud ->
                if (cloud != null) _profile.value = cloud
                else reloadFromLocal()
            }
        } finally {
            _isSyncing.value = false
        }
    }

    fun addItem(
        section: String,
        content: String,
        source: V2ProfileSource = V2ProfileSource.USER_MANUAL,
        evictOldestAiWhenFull: Boolean = false,
    ): Boolean {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return false
        val limit = V2GolfProfileData.sectionLimits[section] ?: 5
        val item = V2ProfileItem(content = trimmed, source = source.wire)
        val current = _profile.value
        val updated = when (section) {
            "currentFocus" -> current.copy(
                currentFocus = insertItem(current.currentFocus, item, limit, evictOldestAiWhenFull),
            )
            "painPoints" -> current.copy(
                painPoints = insertItem(current.painPoints, item, limit, evictOldestAiWhenFull),
            )
            "strengths" -> current.copy(
                strengths = insertItem(current.strengths, item, limit, evictOldestAiWhenFull),
            )
            "trainingGoals" -> {
                if (current.trainingGoals.size >= limit) return false
                current.copy(trainingGoals = listOf(trimmed) + current.trainingGoals)
            }
            "coachNotes" -> {
                if (current.coachNotes.size >= limit) return false
                current.copy(coachNotes = listOf(trimmed) + current.coachNotes)
            }
            else -> return false
        }
        if (updated == current) return false
        _profile.value = updated
        persist()
        return true
    }

    fun removeItem(section: String, index: Int) {
        val current = _profile.value
        val updated = when (section) {
            "currentFocus" -> {
                if (index !in current.currentFocus.indices) return
                current.copy(currentFocus = current.currentFocus.toMutableList().also { it.removeAt(index) })
            }
            "painPoints" -> {
                if (index !in current.painPoints.indices) return
                current.copy(painPoints = current.painPoints.toMutableList().also { it.removeAt(index) })
            }
            "strengths" -> {
                if (index !in current.strengths.indices) return
                current.copy(strengths = current.strengths.toMutableList().also { it.removeAt(index) })
            }
            "trainingGoals" -> {
                if (index !in current.trainingGoals.indices) return
                current.copy(trainingGoals = current.trainingGoals.toMutableList().also { it.removeAt(index) })
            }
            "coachNotes" -> {
                if (index !in current.coachNotes.indices) return
                current.copy(coachNotes = current.coachNotes.toMutableList().also { it.removeAt(index) })
            }
            else -> return
        }
        _profile.value = updated
        persist()
    }

    fun updateStatusSummary(summary: String) {
        val trimmed = summary.trim()
        if (trimmed.isEmpty()) return
        _profile.value = _profile.value.copy(
            statusSummary = trimmed,
            summaryUpdatedAt = V2ProfileDates.nowIso(),
        )
        persist()
    }

    fun buildProfilePrompt(): String? {
        val p = _profile.value
        val parts = mutableListOf<String>()
        if (p.currentFocus.isNotEmpty()) {
            parts += "训练重点: " + p.currentFocus.joinToString("、") { it.content }
        }
        if (p.painPoints.isNotEmpty()) {
            parts += "痛点: " + p.painPoints.joinToString("、") { it.content }
        }
        if (p.strengths.isNotEmpty()) {
            parts += "优势: " + p.strengths.joinToString("、") { it.content }
        }
        if (p.trainingGoals.isNotEmpty()) {
            parts += "目标: " + p.trainingGoals.joinToString("、")
        }
        if (p.coachNotes.isNotEmpty()) {
            parts += "教练要求: " + p.coachNotes.joinToString("、")
        }
        p.statusSummary?.takeIf { it.isNotBlank() }?.let { parts += "状态: $it" }
        if (parts.isEmpty()) return null
        return "📋 用户档案：\n" + parts.joinToString("\n")
    }

    private fun insertItem(
        items: List<V2ProfileItem>,
        newItem: V2ProfileItem,
        limit: Int,
        evictOldestAi: Boolean,
    ): List<V2ProfileItem> {
        if (items.size < limit) return listOf(newItem) + items
        if (!evictOldestAi) return items
        val evictIdx = items.indices
            .filter { V2ProfileSource.fromWire(items[it].source) == V2ProfileSource.AI_INFERRED }
            .minByOrNull { V2ProfileDates.parseIsoOrNull(items[it].addedAt) ?: Instant.EPOCH }
        if (evictIdx == null) return items
        val mutable = items.toMutableList()
        mutable.removeAt(evictIdx)
        return listOf(newItem) + mutable
    }

    private fun persist() {
        val snapshot = _profile.value
        scope.launch {
            runGolfProfilePersistCatching { repo.saveAndSync(snapshot) }
        }
    }
}

internal suspend fun runGolfProfilePersistCatching(block: suspend () -> Unit): Result<Unit> = try {
    block()
    Result.success(Unit)
} catch (e: Throwable) {
    if (e is CancellationException) throw e
    Result.failure(e)
}
