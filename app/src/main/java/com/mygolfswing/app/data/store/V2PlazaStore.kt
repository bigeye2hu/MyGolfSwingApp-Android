package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2PlazaCategoryChip
import com.mygolfswing.app.data.model.V2PlazaCoach
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.data.model.V2Topic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
 * - 若网络请求失败（未登录 / 无网络），回退到 mock 数据，保证 UI 不空
 */
object V2PlazaStore {

    private val repo get() = ServiceLocator.plazaRepo
    private val scope get() = ServiceLocator.appScope

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

    private val _isUsingMock = MutableStateFlow(false)
    val isUsingMock: StateFlow<Boolean> = _isUsingMock.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _savedIds = MutableStateFlow<Set<String>>(emptySet())

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
        _videos.value = emptyList()
        _heroVideo.value = null
        _searchKeyword.value = ""
        _isSearchActive.value = false
        _lastError.value = null
        _isUsingMock.value = false
    }

    @Volatile private var collecting = false

    fun startObserving() {
        if (collecting) return
        collecting = true
        scope.launch {
            repo.savedIds.onEach { ids ->
                _savedIds.value = ids
                _videos.value = _videos.value.map { it.copy(isSaved = ids.contains(it.saveKey())) }
            }.collect()
        }
    }

    fun selectCategory(id: String) {
        _selectedCategoryId.value = id
        refresh()
    }

    fun setSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
        _isSearchActive.value = keyword.isNotBlank()
        refresh()
    }

    fun clearSearch() {
        _searchKeyword.value = ""
        _isSearchActive.value = false
        refresh()
    }

    fun toggleSave(videoId: UUID) {
        val video = _videos.value.firstOrNull { it.id == videoId }
            ?: _heroVideo.value?.takeIf { it.id == videoId }
            ?: return
        val saveKey = video.saveKey()
        val nowSaved = _savedIds.value.contains(saveKey)
        scope.launch {
            if (nowSaved) repo.unsave(saveKey) else repo.save(saveKey)
        }
        _videos.value = _videos.value.map { v ->
            if (v.id != videoId) v
            else v.copy(
                isSaved = !nowSaved,
                saveCount = if (!nowSaved) v.saveCount + 1 else (v.saveCount - 1).coerceAtLeast(0),
            )
        }
        _heroVideo.value = _heroVideo.value?.let { v ->
            if (v.id != videoId) v
            else v.copy(
                isSaved = !nowSaved,
                saveCount = if (!nowSaved) v.saveCount + 1 else (v.saveCount - 1).coerceAtLeast(0),
            )
        }
    }

    /** 首页「广场推荐」：按发布时间倒序取前 6 条（对齐 iOS `V2HomeView.plazaRecommends`） */
    fun homeRecommendations(): List<V2PlazaVideo> =
        _videos.value.sortedByDescending { it.publishedAtMs }.take(6)

    fun filteredVideos(): List<V2PlazaVideo> = _videos.value

    fun refresh() {
        scope.launch {
            _isLoading.value = true
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
                limit = 30,
                categoryId = cid,
                parentCategoryId = parentCid.takeIf { keyword == null },
                keyword = keyword,
            )
            _isLoading.value = false
            result.onSuccess { list ->
                _lastError.value = null
                _isUsingMock.value = false
                val saved = _savedIds.value
                val merged = list.map { it.copy(isSaved = saved.contains(it.saveKey())) }
                val hero = pickHero(merged)
                _heroVideo.value = hero
                _videos.value = hero?.let { h -> merged.filter { it.saveKey() != h.saveKey() } } ?: merged
            }.onFailure { e ->
                _lastError.value = e.message
                if (_videos.value.isEmpty()) {
                    resetWithMockData()
                    _isUsingMock.value = true
                }
            }
        }
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
}
