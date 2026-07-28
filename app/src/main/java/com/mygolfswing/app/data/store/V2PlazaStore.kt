package com.mygolfswing.app.data.store

import android.content.Context
import android.content.pm.ApplicationInfo
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2PlazaCategoryChip
import com.mygolfswing.app.data.model.V2PlazaCoach
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.data.model.V2Topic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * V2 广场 Store（对齐 iOS `VideoPlazaManager`）。
 *
 * 已切换为：
 * - 列表数据由 [com.mygolfswing.app.data.repo.V2PlazaRepository.fetchVideos] 拉取
 * - 收藏状态从 Room `v2_plaza_saved` 表合并
 * - Debug 包网络请求失败时回退 mock 数据，方便开发调试
 * - Release 包不展示 mock，避免上线用户误把示例内容当成真实广场资源
 */
object V2PlazaStore {

    private val repo get() = ServiceLocator.plazaRepo
    private val scope get() = ServiceLocator.appScope
    private const val PAGE_SIZE = 30
    private const val WATCH_HISTORY_PREFS = "v2_plaza_watch_history"
    private const val SAVED_VIDEO_PREFS = "v2_plaza_saved_videos"
    private const val LIKED_VIDEO_PREFS = "v2_plaza_liked_videos"
    private const val WATCH_HISTORY_LIMIT = 50
    private val allowMockFallback: Boolean
        get() = ServiceLocator.appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    private val _videos = MutableStateFlow<List<V2PlazaVideo>>(emptyList())
    val videos: StateFlow<List<V2PlazaVideo>> = _videos.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow("all")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _heroVideo = MutableStateFlow<V2PlazaVideo?>(null)
    val heroVideo: StateFlow<V2PlazaVideo?> = _heroVideo.asStateFlow()

    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _isUsingMock = MutableStateFlow(false)
    val isUsingMock: StateFlow<Boolean> = _isUsingMock.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _savedIds = MutableStateFlow<Set<String>>(emptySet())
    val savedIds: StateFlow<Set<String>> = _savedIds.asStateFlow()

    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    val likedIds: StateFlow<Set<String>> = _likedIds.asStateFlow()

    private val _savedVideoCache = MutableStateFlow<List<V2PlazaVideo>>(emptyList())
    val savedVideoCache: StateFlow<List<V2PlazaVideo>> = _savedVideoCache.asStateFlow()

    private val _showSavedOnly = MutableStateFlow(false)
    val showSavedOnly: StateFlow<Boolean> = _showSavedOnly.asStateFlow()

    private val _watchHistory = MutableStateFlow<List<V2PlazaWatchHistoryItem>>(emptyList())
    val watchHistory: StateFlow<List<V2PlazaWatchHistoryItem>> = _watchHistory.asStateFlow()

    private var nextOffset = 0
    private var watchHistoryUserId: String = ""
    private var savedVideoUserId: String = ""
    private var likedVideoUserId: String = ""

    private val defaultCategories = listOf(
        V2PlazaCategoryChip("all", "全部", V2Topic.ALL),
        V2PlazaCategoryChip("swing", "挥杆", V2Topic.SWING),
        V2PlazaCategoryChip("putting", "推杆", V2Topic.PUTTING),
        V2PlazaCategoryChip("short_game", "短杆", V2Topic.SHORT_GAME),
        V2PlazaCategoryChip("sand", "沙坑", V2Topic.SAND),
        V2PlazaCategoryChip("mind", "心法", V2Topic.MIND),
        V2PlazaCategoryChip("fitness", "体能", V2Topic.FITNESS),
        V2PlazaCategoryChip("gear", "装备", V2Topic.GEAR),
    )

    private val _categories = MutableStateFlow(defaultCategories)
    val categories: StateFlow<List<V2PlazaCategoryChip>> = _categories.asStateFlow()

    private val _allCategories = MutableStateFlow(defaultCategories)

    fun resetUserState() {
        _savedIds.value = emptySet()
        _likedIds.value = emptySet()
        _savedVideoCache.value = emptyList()
        _videos.value = emptyList()
        _heroVideo.value = null
        _searchKeyword.value = ""
        _isSearchActive.value = false
        _lastError.value = null
        _isUsingMock.value = false
        _showSavedOnly.value = false
        _watchHistory.value = emptyList()
        watchHistoryUserId = ""
        savedVideoUserId = ""
        likedVideoUserId = ""
        _isLoadingMore.value = false
        _hasMore.value = true
        nextOffset = 0
    }

    @Volatile private var collecting = false

    fun startObserving() {
        if (collecting) return
        collecting = true
        scope.launch {
            repo.savedIds.onEach { ids ->
                _savedIds.value = ids
                _videos.value = _videos.value.map { it.copy(isSaved = ids.contains(it.saveKey())) }
                _heroVideo.value = _heroVideo.value?.let { it.copy(isSaved = ids.contains(it.saveKey())) }
                pruneSavedVideoCache(ids)
            }.collect()
        }
        scope.launch {
            ServiceLocator.session.state
                .map { it.userId.orEmpty() }
                .distinctUntilChanged()
                .onEach {
                    loadPersistedWatchHistory(it)
                    loadPersistedSavedVideos(it)
                    loadPersistedLikedVideos(it)
                }
                .collect()
        }
    }

    fun selectCategory(id: String) {
        _showSavedOnly.value = false
        _selectedCategoryId.value = id
        refresh()
    }

    fun setSearchKeyword(keyword: String) {
        _showSavedOnly.value = false
        _searchKeyword.value = keyword
        _isSearchActive.value = keyword.isNotBlank()
        refresh()
    }

    fun clearSearch() {
        _showSavedOnly.value = false
        _searchKeyword.value = ""
        _isSearchActive.value = false
        refresh()
    }

    fun toggleSavedOnly() {
        _showSavedOnly.value = !_showSavedOnly.value
        if (_showSavedOnly.value) {
            _searchKeyword.value = ""
            _isSearchActive.value = false
            _selectedCategoryId.value = "all"
        }
    }

    fun toggleSave(videoId: UUID): Boolean? {
        if (savedVideoUserId.isBlank()) {
            _lastError.value = "请先登录后再收藏"
            return null
        }
        val video = _videos.value.firstOrNull { it.id == videoId }
            ?: _heroVideo.value?.takeIf { it.id == videoId }
            ?: run {
                _lastError.value = "视频状态异常"
                return null
            }
        val saveKey = video.saveKey()
        val nowSaved = _savedIds.value.contains(saveKey)
        _savedIds.value = if (nowSaved) _savedIds.value - saveKey else _savedIds.value + saveKey
        if (nowSaved) removeSavedVideoCache(saveKey) else addSavedVideoCache(video.copy(isSaved = true))
        applySaveState(videoId = videoId, saved = !nowSaved)
        scope.launch {
            runPlazaStoreCatching {
                if (nowSaved) repo.unsave(saveKey) else repo.save(saveKey)
            }.onFailure { e ->
                _lastError.value = plazaStoreError("收藏同步失败，请稍后重试", e.message)
                _savedIds.value = if (nowSaved) _savedIds.value + saveKey else _savedIds.value - saveKey
                if (nowSaved) addSavedVideoCache(video.copy(isSaved = true)) else removeSavedVideoCache(saveKey)
                applySaveState(videoId = videoId, saved = nowSaved)
            }
        }
        return !nowSaved
    }

    fun toggleLike(videoId: UUID): Boolean? {
        if (likedVideoUserId.isBlank()) {
            _lastError.value = "请先登录后再点赞"
            return null
        }
        val video = _videos.value.firstOrNull { it.id == videoId }
            ?: _heroVideo.value?.takeIf { it.id == videoId }
            ?: run {
                _lastError.value = "视频状态异常"
                return null
            }
        val likeKey = video.saveKey()
        val wasLiked = _likedIds.value.contains(likeKey)
        val nowLiked = !wasLiked
        _likedIds.value = if (nowLiked) _likedIds.value + likeKey else _likedIds.value - likeKey
        persistLikedVideos()
        scope.launch {
            runPlazaStoreCatching {
                if (nowLiked) repo.like(likeKey).getOrThrow() else repo.unlike(likeKey).getOrThrow()
            }.onFailure { e ->
                _lastError.value = plazaStoreError("点赞同步失败，请稍后重试", e.message)
                _likedIds.value = if (wasLiked) _likedIds.value + likeKey else _likedIds.value - likeKey
                persistLikedVideos()
            }
        }
        return nowLiked
    }

    private fun applySaveState(videoId: UUID, saved: Boolean) {
        _videos.value = _videos.value.map { v ->
            if (v.id != videoId) v
            else v.copy(
                isSaved = saved,
                saveCount = if (saved && !v.isSaved) v.saveCount + 1
                else if (!saved && v.isSaved) (v.saveCount - 1).coerceAtLeast(0)
                else v.saveCount,
            )
        }
        _heroVideo.value = _heroVideo.value?.let { v ->
            if (v.id != videoId) v
            else v.copy(
                isSaved = saved,
                saveCount = if (saved && !v.isSaved) v.saveCount + 1
                else if (!saved && v.isSaved) (v.saveCount - 1).coerceAtLeast(0)
                else v.saveCount,
            )
        }
    }

    /** 首页「广场推荐」：按发布时间倒序取前 6 条（对齐 iOS `V2HomeView.plazaRecommends`） */
    fun homeRecommendations(): List<V2PlazaVideo> =
        _videos.value.sortedByDescending { it.publishedAtMs }.take(6)

    fun filteredVideos(): List<V2PlazaVideo> {
        val keyword = _searchKeyword.value.trim()
        if (keyword.isBlank()) return _videos.value
        val tokens = keyword
            .split(Regex("\\s+"))
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return _videos.value
        return _videos.value.filter { video ->
            val haystack = listOfNotNull(
                video.title,
                video.coach.displayName,
                video.categoryDisplayName,
                video.topic.displayName,
                video.sourceCategoryId,
                video.sourceParentCategoryId,
            ).joinToString(" ").lowercase()
            tokens.all { token -> haystack.contains(token) }
        }
    }

    fun markWatched(video: V2PlazaVideo, progress: Double = 0.0) {
        if (watchHistoryUserId.isBlank()) return
        val old = _watchHistory.value.firstOrNull { it.saveKey == video.saveKey() }
        val item = V2PlazaWatchHistoryItem(
            videoId = video.id,
            saveKey = video.saveKey(),
            sourceVideoId = video.sourceVideoId,
            sourceCoachId = video.sourceCoachId,
            sourceCategoryId = video.sourceCategoryId,
            sourceParentCategoryId = video.sourceParentCategoryId,
            title = video.title,
            coachName = video.coach.displayName,
            coachInitial = video.coach.initialChar,
            coachTint = video.coach.avatarTint.name,
            thumbnailUrl = video.thumbnailUrl,
            thumbnailKind = video.thumbnailKind.name,
            videoUrl = video.videoUrl,
            topic = video.topic.name,
            categoryDisplayName = video.categoryDisplayName,
            viewCount = video.viewCount,
            saveCount = video.saveCount,
            durationSec = video.durationSec,
            progress = progress.takeIf { it > 0.0 } ?: old?.progress ?: 0.0,
            watchedAtMs = System.currentTimeMillis(),
        )
        _watchHistory.value = (listOf(item) + _watchHistory.value.filterNot { it.saveKey == item.saveKey }).take(WATCH_HISTORY_LIMIT)
        persistWatchHistory()
    }

    fun updateWatchProgress(video: V2PlazaVideo, progress: Double) {
        if (watchHistoryUserId.isBlank()) return
        val normalized = progress.coerceIn(0.0, 1.0)
        _watchHistory.value = _watchHistory.value.map { item ->
            if (item.saveKey != video.saveKey()) item
            else item.copy(progress = normalized, watchedAtMs = System.currentTimeMillis())
        }
        persistWatchHistory()
    }

    fun videoForPlayback(videoId: UUID): V2PlazaVideo? {
        val cached = (listOfNotNull(_heroVideo.value) + _videos.value + _savedVideoCache.value)
            .firstOrNull { it.id == videoId }
        if (cached != null) return cached
        val history = _watchHistory.value.firstOrNull { it.videoId == videoId } ?: return null
        return history.toPlaybackVideo(_savedIds.value.contains(history.saveKey))
    }

    fun removeWatchHistory(saveKey: String) {
        _watchHistory.value = _watchHistory.value.filterNot { it.saveKey == saveKey }
        persistWatchHistory()
    }

    fun clearWatchHistory() {
        _watchHistory.value = emptyList()
        persistWatchHistory()
    }

    fun mergeTransientVideos(videos: List<V2PlazaVideo>) {
        if (videos.isEmpty()) return
        val saved = _savedIds.value
        val merged = (_videos.value + videos.map { it.copy(isSaved = saved.contains(it.saveKey()) || it.isSaved) })
            .distinctBy { it.saveKey() }
        _videos.value = _heroVideo.value?.let { h -> merged.filter { it.saveKey() != h.saveKey() } } ?: merged
        cacheSavedVideosFrom(merged)
    }

    fun refresh() {
        scope.launch {
            _isLoading.value = true
            _isLoadingMore.value = false
            _hasMore.value = true
            nextOffset = 0
            try {
                repo.fetchCategories().onSuccess { remote ->
                    if (remote.isNotEmpty()) {
                        val all = listOf(V2PlazaCategoryChip("all", "全部", V2Topic.ALL)) + remote
                        _allCategories.value = all
                        _categories.value = all.filter { it.id == "all" || it.isTopLevel }
                        if (_categories.value.none { it.id == _selectedCategoryId.value }) {
                            _selectedCategoryId.value = "all"
                        }
                    }
                }
                val selected = selectedCategory()
                val cid = selected?.takeIf { it.id != "all" && !it.isTopLevel }?.id
                val parentCid = selected?.takeIf { it.id != "all" && it.isTopLevel }?.id
                val keyword = _searchKeyword.value.trim().takeIf { it.isNotBlank() }
                val result = repo.fetchVideos(
                    limit = PAGE_SIZE,
                    offset = 0,
                    categoryId = cid,
                    parentCategoryId = parentCid.takeIf { keyword == null },
                    keyword = keyword,
                )
                result.onSuccess { list ->
                    _lastError.value = null
                    _isUsingMock.value = false
                    val saved = _savedIds.value
                    val merged = list.map { it.copy(isSaved = saved.contains(it.saveKey())) }
                    cacheSavedVideosFrom(merged)
                    val hero = pickHero(merged)
                    _heroVideo.value = hero
                    _videos.value = hero?.let { h -> merged.filter { it.saveKey() != h.saveKey() } } ?: merged
                    nextOffset = list.size
                    _hasMore.value = list.size >= PAGE_SIZE
                }.onFailure { e ->
                    _lastError.value = plazaStoreError("广场内容加载失败，请下拉重试", e.message)
                    _hasMore.value = false
                    if (_videos.value.isEmpty() && allowMockFallback) {
                        resetWithMockData()
                        _isUsingMock.value = true
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _lastError.value = plazaStoreError("广场内容加载失败，请下拉重试", e.message)
                _hasMore.value = false
                if (_videos.value.isEmpty() && allowMockFallback) {
                    resetWithMockData()
                    _isUsingMock.value = true
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreIfNeeded(anchorVideoId: UUID? = null) {
        val anchor = anchorVideoId ?: return
        if (_isLoading.value || _isLoadingMore.value || !_hasMore.value || _isUsingMock.value) return
        if (_videos.value.lastOrNull()?.id != anchor) return
        scope.launch {
            _isLoadingMore.value = true
            try {
                val selected = selectedCategory()
                val cid = selected?.takeIf { it.id != "all" && !it.isTopLevel }?.id
                val parentCid = selected?.takeIf { it.id != "all" && it.isTopLevel }?.id
                val keyword = _searchKeyword.value.trim().takeIf { it.isNotBlank() }
                val result = repo.fetchVideos(
                    limit = PAGE_SIZE,
                    offset = nextOffset,
                    categoryId = cid,
                    parentCategoryId = parentCid.takeIf { keyword == null },
                    keyword = keyword,
                )
                result.onSuccess { more ->
                    _lastError.value = null
                    nextOffset += more.size
                    _hasMore.value = more.size >= PAGE_SIZE
                    val saved = _savedIds.value
                    val appended = more.map { it.copy(isSaved = saved.contains(it.saveKey())) }
                    val current = listOfNotNull(_heroVideo.value) + _videos.value
                    val merged = (current + appended).distinctBy { it.saveKey() }
                    cacheSavedVideosFrom(merged)
                    val hero = _heroVideo.value?.let { h -> merged.firstOrNull { it.saveKey() == h.saveKey() } } ?: pickHero(merged)
                    _heroVideo.value = hero
                    _videos.value = hero?.let { h -> merged.filter { it.saveKey() != h.saveKey() } } ?: merged
                }.onFailure { e ->
                    _lastError.value = plazaStoreError("更多内容加载失败，请稍后重试", e.message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _lastError.value = plazaStoreError("更多内容加载失败，请稍后重试", e.message)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private fun plazaStoreError(base: String, debugDetail: String?): String {
        val detail = debugDetail.takeIf { ServiceLocator.isDebuggable && !it.isNullOrBlank() }
        return if (detail == null) base else "$base（$detail）"
    }

    private suspend inline fun <T> runPlazaStoreCatching(
        crossinline block: suspend () -> T,
    ): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

    fun resetWithMockData() {
        val sampleVideo = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        val sample = listOf(
            V2PlazaVideo(
                title = "7 号铁中铁基础挥杆 · 4 拍节奏",
                durationSec = 152.0,
                topic = V2Topic.SWING,
                categoryDisplayName = "挥杆",
                coach = V2PlazaCoach("Coach Lee", "L", V2PlazaCoach.AvatarTint.GREEN),
                viewCount = 5234,
                saveCount = 612,
                thumbnailKind = V2PlazaVideo.ThumbnailKind.GREEN_SWING,
                videoUrl = sampleVideo,
                isNew = true,
            ),
            V2PlazaVideo(
                title = "推杆距离感 4 步法",
                durationSec = 178.0,
                topic = V2Topic.PUTTING,
                categoryDisplayName = "推杆",
                coach = V2PlazaCoach("张教练", "张", V2PlazaCoach.AvatarTint.GOLD),
                viewCount = 3128,
                saveCount = 412,
                thumbnailKind = V2PlazaVideo.ThumbnailKind.MINT_TIP,
                videoUrl = sampleVideo,
            ),
            V2PlazaVideo(
                title = "沙坑救球角度全解",
                durationSec = 210.0,
                topic = V2Topic.SAND,
                categoryDisplayName = "沙坑",
                coach = V2PlazaCoach("Pro Sam", "S", V2PlazaCoach.AvatarTint.SAND),
                viewCount = 2745,
                saveCount = 318,
                thumbnailKind = V2PlazaVideo.ThumbnailKind.SAND_WARM,
                videoUrl = sampleVideo,
            ),
            V2PlazaVideo(
                title = "切杆三种着地方式",
                durationSec = 135.0,
                topic = V2Topic.SHORT_GAME,
                categoryDisplayName = "短杆",
                coach = V2PlazaCoach("Coach Jin", "J", V2PlazaCoach.AvatarTint.MIST),
                viewCount = 1942,
                saveCount = 230,
                thumbnailKind = V2PlazaVideo.ThumbnailKind.SHORT_AMBER,
                videoUrl = sampleVideo,
            ),
            V2PlazaVideo(
                title = "高尔夫核心力量 5 分钟",
                durationSec = 296.0,
                topic = V2Topic.FITNESS,
                categoryDisplayName = "体能",
                coach = V2PlazaCoach("J Trainer", "T", V2PlazaCoach.AvatarTint.GOLD),
                viewCount = 8324,
                saveCount = 1023,
                thumbnailKind = V2PlazaVideo.ThumbnailKind.FITNESS_ROSE,
                videoUrl = sampleVideo,
                isNew = true,
            ),
            V2PlazaVideo(
                title = "球场战术心法",
                durationSec = 174.0,
                topic = V2Topic.MIND,
                categoryDisplayName = "心法",
                coach = V2PlazaCoach("禅师", "禅", V2PlazaCoach.AvatarTint.MIST),
                viewCount = 1503,
                saveCount = 168,
                thumbnailKind = V2PlazaVideo.ThumbnailKind.MINT_TIP,
                videoUrl = sampleVideo,
            ),
            V2PlazaVideo(
                title = "球杆选购避坑指南",
                durationSec = 220.0,
                topic = V2Topic.GEAR,
                categoryDisplayName = "装备",
                coach = V2PlazaCoach("器材君", "器", V2PlazaCoach.AvatarTint.SAND),
                viewCount = 6042,
                saveCount = 740,
                thumbnailKind = V2PlazaVideo.ThumbnailKind.EARTH_GEAR,
                videoUrl = sampleVideo,
            ),
        )
        val hero = pickHero(sample)
        _heroVideo.value = hero
        _videos.value = hero?.let { h -> sample.filter { it.saveKey() != h.saveKey() } } ?: sample
        _hasMore.value = false
        nextOffset = sample.size
    }

    private fun selectedCategory(): V2PlazaCategoryChip? {
        val selectedId = _selectedCategoryId.value
        return _allCategories.value.firstOrNull { it.id == selectedId }
            ?: _categories.value.firstOrNull { it.id == selectedId }
    }

    private fun pickHero(videos: List<V2PlazaVideo>): V2PlazaVideo? {
        if (_selectedCategoryId.value != "all" || _isSearchActive.value || videos.isEmpty()) return null
        val pool = videos.sortedByDescending { it.publishedAtMs }.take(10)
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val idx = day.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) } % pool.size
        return pool[idx].copy(isFeaturedToday = true)
    }

    private fun loadPersistedSavedVideos(userId: String) {
        savedVideoUserId = userId
        if (userId.isBlank()) {
            _savedVideoCache.value = emptyList()
            return
        }
        val raw = savedVideoPrefs().getString(savedVideoKey(userId), null)
        val saved = _savedIds.value
        _savedVideoCache.value = raw
            ?.let { parseSavedVideos(it) }
            ?.map { it.copy(isSaved = true) }
            ?.filter { saved.isEmpty() || saved.contains(it.saveKey()) }
            ?.distinctBy { it.saveKey() }
            ?: emptyList()
    }

    private fun addSavedVideoCache(video: V2PlazaVideo) {
        _savedVideoCache.value = (listOf(video.copy(isSaved = true)) + _savedVideoCache.value.filterNot { it.saveKey() == video.saveKey() })
            .take(100)
        persistSavedVideos()
    }

    private fun removeSavedVideoCache(saveKey: String) {
        _savedVideoCache.value = _savedVideoCache.value.filterNot { it.saveKey() == saveKey }
        persistSavedVideos()
    }

    private fun pruneSavedVideoCache(savedIds: Set<String>) {
        if (savedIds.isEmpty()) {
            _savedVideoCache.value = emptyList()
        } else {
            _savedVideoCache.value = _savedVideoCache.value.filter { savedIds.contains(it.saveKey()) }
        }
        persistSavedVideos()
    }

    private fun cacheSavedVideosFrom(videos: List<V2PlazaVideo>) {
        val saved = _savedIds.value
        val incoming = videos.filter { it.isSaved || saved.contains(it.saveKey()) }
        if (incoming.isEmpty()) return
        val next = (incoming.map { it.copy(isSaved = true) } + _savedVideoCache.value)
            .distinctBy { it.saveKey() }
            .take(100)
        if (next != _savedVideoCache.value) {
            _savedVideoCache.value = next
            persistSavedVideos()
        }
    }

    private fun persistSavedVideos() {
        val userId = savedVideoUserId.takeIf { it.isNotBlank() } ?: return
        savedVideoPrefs()
            .edit()
            .putString(savedVideoKey(userId), encodeSavedVideos(_savedVideoCache.value.take(100)))
            .apply()
    }

    private fun savedVideoPrefs() =
        ServiceLocator.appContext.getSharedPreferences(SAVED_VIDEO_PREFS, Context.MODE_PRIVATE)

    private fun savedVideoKey(userId: String): String = "saved_$userId"

    private fun loadPersistedLikedVideos(userId: String) {
        likedVideoUserId = userId
        if (userId.isBlank()) {
            _likedIds.value = emptySet()
            return
        }
        _likedIds.value = likedVideoPrefs()
            .getStringSet(likedVideoKey(userId), emptySet())
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    }

    private fun persistLikedVideos() {
        val userId = likedVideoUserId.takeIf { it.isNotBlank() } ?: return
        likedVideoPrefs()
            .edit()
            .putStringSet(likedVideoKey(userId), _likedIds.value)
            .apply()
    }

    private fun likedVideoPrefs() =
        ServiceLocator.appContext.getSharedPreferences(LIKED_VIDEO_PREFS, Context.MODE_PRIVATE)

    private fun likedVideoKey(userId: String): String = "liked_$userId"

    private fun encodeSavedVideos(videos: List<V2PlazaVideo>): String =
        JsonArray(videos.map { encodeSavedVideo(it) }).toString()

    private fun encodeSavedVideo(video: V2PlazaVideo): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(video.id.toString()),
            "sourceVideoId" to JsonPrimitive(video.sourceVideoId),
            "sourceCoachId" to JsonPrimitive(video.sourceCoachId),
            "sourceCategoryId" to JsonPrimitive(video.sourceCategoryId),
            "sourceParentCategoryId" to JsonPrimitive(video.sourceParentCategoryId),
            "title" to JsonPrimitive(video.title),
            "durationSec" to JsonPrimitive(video.durationSec),
            "topic" to JsonPrimitive(video.topic.name),
            "categoryDisplayName" to JsonPrimitive(video.categoryDisplayName),
            "coachName" to JsonPrimitive(video.coach.displayName),
            "coachInitial" to JsonPrimitive(video.coach.initialChar),
            "coachTint" to JsonPrimitive(video.coach.avatarTint.name),
            "viewCount" to JsonPrimitive(video.viewCount),
            "saveCount" to JsonPrimitive(video.saveCount),
            "thumbnailKind" to JsonPrimitive(video.thumbnailKind.name),
            "thumbnailUrl" to JsonPrimitive(video.thumbnailUrl),
            "videoUrl" to JsonPrimitive(video.videoUrl),
            "publishedAtMs" to JsonPrimitive(video.publishedAtMs),
            "isNew" to JsonPrimitive(video.isNew),
        ),
    )

    private fun parseSavedVideos(raw: String): List<V2PlazaVideo> = runCatching {
        ServiceLocator.net.json.parseToJsonElement(raw).jsonArray.mapNotNull { element ->
            runCatching { decodeSavedVideo(element.jsonObject) }.getOrNull()
        }
    }.getOrDefault(emptyList())

    private fun decodeSavedVideo(o: JsonObject): V2PlazaVideo {
        val title = o["title"]?.jsonPrimitive?.contentOrNull ?: return fallbackSavedVideo(o)
        val topic = enumValueOrDefault<V2Topic>(o["topic"]?.jsonPrimitive?.contentOrNull, V2Topic.SWING)
        val tint = enumValueOrDefault<V2PlazaCoach.AvatarTint>(o["coachTint"]?.jsonPrimitive?.contentOrNull, V2PlazaCoach.AvatarTint.GREEN)
        val thumb = enumValueOrDefault<V2PlazaVideo.ThumbnailKind>(o["thumbnailKind"]?.jsonPrimitive?.contentOrNull, V2PlazaVideo.ThumbnailKind.GREEN_SWING)
        val id = o["id"]?.jsonPrimitive?.contentOrNull
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: UUID.nameUUIDFromBytes((o["sourceVideoId"]?.jsonPrimitive?.contentOrNull ?: title).toByteArray())
        return V2PlazaVideo(
            id = id,
            sourceVideoId = o["sourceVideoId"]?.jsonPrimitive?.contentOrNull,
            sourceCoachId = o["sourceCoachId"]?.jsonPrimitive?.contentOrNull,
            sourceCategoryId = o["sourceCategoryId"]?.jsonPrimitive?.contentOrNull,
            sourceParentCategoryId = o["sourceParentCategoryId"]?.jsonPrimitive?.contentOrNull,
            title = title,
            durationSec = o["durationSec"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            topic = topic,
            categoryDisplayName = o["categoryDisplayName"]?.jsonPrimitive?.contentOrNull,
            coach = V2PlazaCoach(
                displayName = o["coachName"]?.jsonPrimitive?.contentOrNull ?: "教练",
                initialChar = o["coachInitial"]?.jsonPrimitive?.contentOrNull ?: "C",
                avatarTint = tint,
            ),
            viewCount = o["viewCount"]?.jsonPrimitive?.intOrNull ?: 0,
            saveCount = o["saveCount"]?.jsonPrimitive?.intOrNull ?: 0,
            thumbnailKind = thumb,
            thumbnailUrl = o["thumbnailUrl"]?.jsonPrimitive?.contentOrNull,
            videoUrl = o["videoUrl"]?.jsonPrimitive?.contentOrNull ?: "",
            publishedAtMs = o["publishedAtMs"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
            isNew = o["isNew"]?.jsonPrimitive?.booleanOrNull ?: false,
            isSaved = true,
        )
    }

    private fun fallbackSavedVideo(o: JsonObject): V2PlazaVideo {
        val saveKey = o["saveKey"]?.jsonPrimitive?.contentOrNull ?: UUID.randomUUID().toString()
        return V2PlazaVideo(
            id = UUID.nameUUIDFromBytes(saveKey.toByteArray()),
            sourceVideoId = saveKey,
            title = "已保存视频",
            durationSec = 0.0,
            topic = V2Topic.SWING,
            coach = V2PlazaCoach("教练", "C", V2PlazaCoach.AvatarTint.GREEN),
            viewCount = 0,
            saveCount = 0,
            thumbnailKind = V2PlazaVideo.ThumbnailKind.GREEN_SWING,
            videoUrl = "",
            isSaved = true,
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, default: T): T =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private fun loadPersistedWatchHistory(userId: String) {
        watchHistoryUserId = userId
        if (userId.isBlank()) {
            _watchHistory.value = emptyList()
            return
        }
        val raw = watchHistoryPrefs().getString(watchHistoryKey(userId), null)
        _watchHistory.value = raw
            ?.let { parseWatchHistory(it) }
            ?.sortedByDescending { it.watchedAtMs }
            ?.take(WATCH_HISTORY_LIMIT)
            ?: emptyList()
    }

    private fun persistWatchHistory() {
        val userId = watchHistoryUserId.takeIf { it.isNotBlank() } ?: return
        watchHistoryPrefs()
            .edit()
            .putString(watchHistoryKey(userId), encodeWatchHistory(_watchHistory.value.take(WATCH_HISTORY_LIMIT)))
            .apply()
    }

    private fun watchHistoryPrefs() =
        ServiceLocator.appContext.getSharedPreferences(WATCH_HISTORY_PREFS, Context.MODE_PRIVATE)

    private fun watchHistoryKey(userId: String): String = "history_$userId"

    private fun encodeWatchHistory(items: List<V2PlazaWatchHistoryItem>): String =
        JsonArray(
            items.map { item ->
                JsonObject(
                    mapOf(
                        "videoId" to JsonPrimitive(item.videoId.toString()),
                        "saveKey" to JsonPrimitive(item.saveKey),
                        "sourceVideoId" to JsonPrimitive(item.sourceVideoId),
                        "sourceCoachId" to JsonPrimitive(item.sourceCoachId),
                        "sourceCategoryId" to JsonPrimitive(item.sourceCategoryId),
                        "sourceParentCategoryId" to JsonPrimitive(item.sourceParentCategoryId),
                        "title" to JsonPrimitive(item.title),
                        "coachName" to JsonPrimitive(item.coachName),
                        "coachInitial" to JsonPrimitive(item.coachInitial),
                        "coachTint" to JsonPrimitive(item.coachTint),
                        "thumbnailUrl" to JsonPrimitive(item.thumbnailUrl),
                        "thumbnailKind" to JsonPrimitive(item.thumbnailKind),
                        "videoUrl" to JsonPrimitive(item.videoUrl),
                        "topic" to JsonPrimitive(item.topic),
                        "categoryDisplayName" to JsonPrimitive(item.categoryDisplayName),
                        "viewCount" to JsonPrimitive(item.viewCount),
                        "saveCount" to JsonPrimitive(item.saveCount),
                        "durationSec" to JsonPrimitive(item.durationSec),
                        "progress" to JsonPrimitive(item.progress),
                        "watchedAtMs" to JsonPrimitive(item.watchedAtMs),
                    ),
                )
            },
        ).toString()

    private fun parseWatchHistory(raw: String): List<V2PlazaWatchHistoryItem> = runCatching {
        ServiceLocator.net.json.parseToJsonElement(raw).jsonArray.mapNotNull { element ->
            runCatching {
                val o = element.jsonObject
                V2PlazaWatchHistoryItem(
                    videoId = UUID.fromString(o["videoId"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null),
                    saveKey = o["saveKey"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null,
                    sourceVideoId = o["sourceVideoId"]?.jsonPrimitive?.contentOrNull,
                    sourceCoachId = o["sourceCoachId"]?.jsonPrimitive?.contentOrNull,
                    sourceCategoryId = o["sourceCategoryId"]?.jsonPrimitive?.contentOrNull,
                    sourceParentCategoryId = o["sourceParentCategoryId"]?.jsonPrimitive?.contentOrNull,
                    title = o["title"]?.jsonPrimitive?.contentOrNull ?: "广场视频",
                    coachName = o["coachName"]?.jsonPrimitive?.contentOrNull ?: "教练",
                    coachInitial = o["coachInitial"]?.jsonPrimitive?.contentOrNull ?: "C",
                    coachTint = o["coachTint"]?.jsonPrimitive?.contentOrNull ?: V2PlazaCoach.AvatarTint.GREEN.name,
                    thumbnailUrl = o["thumbnailUrl"]?.jsonPrimitive?.contentOrNull,
                    thumbnailKind = o["thumbnailKind"]?.jsonPrimitive?.contentOrNull ?: V2PlazaVideo.ThumbnailKind.GREEN_SWING.name,
                    videoUrl = o["videoUrl"]?.jsonPrimitive?.contentOrNull ?: "",
                    topic = o["topic"]?.jsonPrimitive?.contentOrNull ?: V2Topic.SWING.name,
                    categoryDisplayName = o["categoryDisplayName"]?.jsonPrimitive?.contentOrNull,
                    viewCount = o["viewCount"]?.jsonPrimitive?.intOrNull ?: 0,
                    saveCount = o["saveCount"]?.jsonPrimitive?.intOrNull ?: 0,
                    durationSec = o["durationSec"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    progress = o["progress"]?.jsonPrimitive?.doubleOrNull?.coerceIn(0.0, 1.0) ?: 0.0,
                    watchedAtMs = o["watchedAtMs"]?.jsonPrimitive?.longOrNull ?: 0L,
                )
            }.getOrNull()
        }
    }.getOrDefault(emptyList())
}

data class V2PlazaWatchHistoryItem(
    val videoId: UUID,
    val saveKey: String,
    val sourceVideoId: String? = null,
    val sourceCoachId: String? = null,
    val sourceCategoryId: String? = null,
    val sourceParentCategoryId: String? = null,
    val title: String,
    val coachName: String,
    val coachInitial: String = "C",
    val coachTint: String = V2PlazaCoach.AvatarTint.GREEN.name,
    val thumbnailUrl: String?,
    val thumbnailKind: String = V2PlazaVideo.ThumbnailKind.GREEN_SWING.name,
    val videoUrl: String = "",
    val topic: String = V2Topic.SWING.name,
    val categoryDisplayName: String? = null,
    val viewCount: Int = 0,
    val saveCount: Int = 0,
    val durationSec: Double,
    val progress: Double,
    val watchedAtMs: Long,
) {
    fun toPlaybackVideo(isSaved: Boolean): V2PlazaVideo? {
        if (videoUrl.isBlank()) return null
        val topicValue = enumValueOrDefault(topic, V2Topic.SWING)
        val tint = enumValueOrDefault(coachTint, V2PlazaCoach.AvatarTint.GREEN)
        val thumb = enumValueOrDefault(thumbnailKind, V2PlazaVideo.ThumbnailKind.GREEN_SWING)
        return V2PlazaVideo(
            id = videoId,
            sourceVideoId = sourceVideoId ?: saveKey,
            sourceCoachId = sourceCoachId,
            sourceCategoryId = sourceCategoryId,
            sourceParentCategoryId = sourceParentCategoryId,
            title = title,
            durationSec = durationSec,
            topic = topicValue,
            categoryDisplayName = categoryDisplayName,
            coach = V2PlazaCoach(coachName, coachInitial.takeIf { it.isNotBlank() } ?: "C", tint),
            viewCount = viewCount,
            saveCount = saveCount,
            thumbnailKind = thumb,
            thumbnailUrl = thumbnailUrl,
            videoUrl = videoUrl,
            isSaved = isSaved,
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, default: T): T =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
