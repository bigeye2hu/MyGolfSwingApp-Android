package com.mygolfswing.app.ui.plaza

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.data.store.V2PlazaWatchHistoryItem
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.launch
import java.util.UUID

enum class V2PlazaLoginTarget {
    SAVED_COLLECTION,
    WATCH_HISTORY,
}

/**
 * V2 广场主页（对齐 iOS `V2PlazaView`）。
 */
@Composable
fun V2PlazaScreen(
    onOpenVideo: (UUID) -> Unit = {},
    onOpenWatchHistory: () -> Unit = {},
    onRequestLogin: () -> Unit = {},
    onRequestLoginForTarget: (V2PlazaLoginTarget) -> Unit = { onRequestLogin() },
) {
    LaunchedEffect(Unit) {
        V2PlazaStore.refresh()
    }

    val videos by V2PlazaStore.videos.collectAsState()
    val categories by V2PlazaStore.categories.collectAsState()
    val selectedCat by V2PlazaStore.selectedCategoryId.collectAsState()
    val isLoading by V2PlazaStore.isLoading.collectAsState()
    val isLoadingMore by V2PlazaStore.isLoadingMore.collectAsState()
    val hasMore by V2PlazaStore.hasMore.collectAsState()
    val isUsingMock by V2PlazaStore.isUsingMock.collectAsState()
    val lastError by V2PlazaStore.lastError.collectAsState()
    val searchKeyword by V2PlazaStore.searchKeyword.collectAsState()
    val hero by V2PlazaStore.heroVideo.collectAsState()
    val watchHistory by V2PlazaStore.watchHistory.collectAsState()
    val profile by V2ProfileStore.profile.collectAsState()
    val savedIds by V2PlazaStore.savedIds.collectAsState()
    val savedVideoCache by V2PlazaStore.savedVideoCache.collectAsState()
    val savedOnly by V2PlazaStore.showSavedOnly.collectAsState()
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var savedSearchKeyword by remember { mutableStateOf("") }
    var savedCategoryFilterId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(profile.isLoggedIn, savedOnly) {
        if (!profile.isLoggedIn && savedOnly) {
            savedSearchKeyword = ""
            savedCategoryFilterId = null
            V2PlazaStore.toggleSavedOnly()
        }
    }
    val allVisibleVideos = (listOfNotNull(hero) + videos + savedVideoCache).distinctBy { it.saveKey() }
    val savedVideos = allVisibleVideos.filter { savedIds.contains(it.saveKey()) || it.isSaved }
    val savedFilterChips = remember(savedVideos) {
        savedVideos
            .map { it.savedFilterChip() }
            .distinctBy { it.id }
            .sortedBy { it.label }
    }
    val selectedSavedFilter = savedFilterChips.firstOrNull { it.id == savedCategoryFilterId }
    LaunchedEffect(savedFilterChips, savedCategoryFilterId) {
        if (savedCategoryFilterId != null && savedFilterChips.none { it.id == savedCategoryFilterId }) {
            savedCategoryFilterId = null
        }
    }
    val filteredSavedVideos = remember(savedVideos, savedSearchKeyword, savedCategoryFilterId) {
        savedVideos.filter {
            it.matchesPlazaKeyword(savedSearchKeyword) && it.matchesSavedFilter(savedCategoryFilterId)
        }
    }
    val effectiveSavedOnly = profile.isLoggedIn && savedOnly
    val filtered = if (effectiveSavedOnly) filteredSavedVideos else V2PlazaStore.filteredVideos()
    val selectedCategoryName = categories.firstOrNull { it.id == selectedCat }?.name ?: "该分类"
    val activeSearchKeyword = if (effectiveSavedOnly) savedSearchKeyword else searchKeyword
    val hasSearch = activeSearchKeyword.isNotBlank()
    val hasSavedFilter = effectiveSavedOnly && selectedSavedFilter != null
    val hasActiveFilter = hasSearch || hasSavedFilter
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlazaHeader(
                searchKeyword = activeSearchKeyword,
                savedOnly = effectiveSavedOnly,
                searchExpanded = searchExpanded || activeSearchKeyword.isNotBlank(),
                savedCount = if (profile.isLoggedIn) savedIds.size else 0,
                onSearchChange = {
                    if (effectiveSavedOnly) {
                        savedSearchKeyword = it
                    } else {
                        V2PlazaStore.setSearchKeyword(it)
                    }
                },
                onClearSearch = {
                    if (effectiveSavedOnly) {
                        savedSearchKeyword = ""
                    } else {
                        V2PlazaStore.clearSearch()
                    }
                    searchExpanded = false
                },
                onToggleSearch = {
                    searchExpanded = !searchExpanded
                },
                onToggleSavedOnly = {
                    if (profile.isLoggedIn) {
                        V2PlazaStore.toggleSavedOnly()
                    } else {
                        onRequestLoginForTarget(V2PlazaLoginTarget.SAVED_COLLECTION)
                    }
                },
            )
            if (isUsingMock) {
                MockDataBanner()
            }
            if (watchHistory.isNotEmpty()) {
                WatchHistorySection(
                    items = watchHistory,
                    onOpen = onOpenVideo,
                    onOpenAll = onOpenWatchHistory,
                )
            }
            if (effectiveSavedOnly) {
                SavedCollectionBanner(
                    savedCount = savedVideos.size,
                    visibleCount = filteredSavedVideos.size,
                    keyword = savedSearchKeyword,
                    filterLabel = selectedSavedFilter?.label,
                    onClearFilters = {
                        savedSearchKeyword = ""
                        savedCategoryFilterId = null
                    },
                )
                if (savedFilterChips.isNotEmpty()) {
                    SavedCollectionFilterBar(
                        filters = savedFilterChips,
                        selectedId = savedCategoryFilterId,
                        onSelect = { savedCategoryFilterId = it },
                    )
                }
            } else if (hasSearch) {
                SearchResultBanner(
                    keyword = searchKeyword,
                    count = filtered.size,
                    isLoading = isLoading,
                    onClear = { V2PlazaStore.clearSearch() },
                )
            } else {
                hero?.let { V2PlazaHeroCard(video = it, onTap = { onOpenVideo(it.id) }) }
                CategoryBar(
                    categories = categories,
                    selected = selectedCat,
                    onSelect = { V2PlazaStore.selectCategory(it) },
                )
            }

            if (isLoading && videos.isEmpty()) {
                LoadingState()
            } else if (filtered.isEmpty()) {
                EmptyState(
                    isLoading = isLoading,
                    hasError = lastError != null,
                    savedOnly = effectiveSavedOnly,
                    hasSearch = hasActiveFilter,
                    searchKeyword = activeSearchKeyword.takeIf { it.isNotBlank() } ?: selectedSavedFilter?.label.orEmpty(),
                    categoryName = selectedCategoryName,
                    onRetry = { V2PlazaStore.refresh() },
                    onSavedEmptyAction = if (effectiveSavedOnly && !hasActiveFilter) {
                        { V2PlazaStore.toggleSavedOnly() }
                    } else {
                        null
                    },
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(
                        horizontal = DSSpacing.lg,
                        vertical = DSSpacing.sm,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(filtered, key = { it.id }) { video ->
                        LaunchedEffect(video.id, filtered.lastOrNull()?.id) {
                            if (!effectiveSavedOnly) V2PlazaStore.loadMoreIfNeeded(video.id)
                        }
                        V2PlazaGridCard(
                            video = video,
                            onTap = { onOpenVideo(video.id) },
                            onSaveTap = {
                                if (profile.isLoggedIn) V2PlazaStore.toggleSave(video.id) else onRequestLogin()
                            },
                        )
                    }
                    items(
                        items = listOf("load_more_footer"),
                        key = { it },
                        span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
                    ) {
                        LoadMoreFooter(
                            isLoadingMore = isLoadingMore,
                            hasMore = hasMore && !savedOnly,
                            hasVideos = filtered.isNotEmpty(),
                            onScrollTop = {
                                coroutineScope.launch {
                                    gridState.animateScrollToItem(0)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlazaHeader() {
    PlazaHeader(
        searchKeyword = "",
        savedOnly = false,
        searchExpanded = false,
        savedCount = 0,
        onSearchChange = {},
        onClearSearch = {},
        onToggleSearch = {},
        onToggleSavedOnly = {},
    )
}

@Composable
private fun PlazaHeader(
    searchKeyword: String,
    savedOnly: Boolean,
    searchExpanded: Boolean,
    savedCount: Int,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleSearch: () -> Unit,
    onToggleSavedOnly: () -> Unit,
) {
    val showSearchField = searchExpanded || savedOnly || searchKeyword.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.warmWhite)
            .padding(horizontal = DSSpacing.lg)
            .padding(top = DSSpacing.sm, bottom = DSSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Eco,
                        contentDescription = null,
                        tint = DSColors.fairway,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "视频广场",
                        style = DSTypography.titleLarge,
                        color = DSColors.inkGreen,
                    )
                }
                Text(
                    text = "精选教学资源",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                    modifier = Modifier.padding(start = 28.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            HeaderIconButton(
                icon = Icons.Filled.Search,
                contentDescription = "搜索",
                selected = showSearchField && searchKeyword.isNotBlank(),
                onClick = onToggleSearch,
            )
            Spacer(modifier = Modifier.width(8.dp))
            HeaderIconButton(
                icon = if (savedOnly) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = "已保存",
                selected = savedOnly,
                badge = savedCount.takeIf { it > 0 }?.coerceAtMost(99)?.toString(),
                onClick = onToggleSavedOnly,
            )
        }

        if (showSearchField) {
            TextField(
                value = searchKeyword,
                onValueChange = onSearchChange,
                singleLine = true,
                textStyle = DSTypography.caption.copy(color = DSColors.inkGreen),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = DSColors.shadowGray,
                        modifier = Modifier.size(16.dp),
                    )
                },
                trailingIcon = {
                    if (searchKeyword.isNotBlank()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "清除搜索",
                                tint = DSColors.shadowGray,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                },
                placeholder = {
                    Text(
                        if (savedOnly) "搜索已保存视频、教练或关键词" else "搜索视频、教练或关键词",
                        style = DSTypography.caption,
                        color = DSColors.shadowGray,
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DSColors.cardWhite,
                    unfocusedContainerColor = DSColors.cardWhite,
                    disabledContainerColor = DSColors.cardWhite,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(width = 1.dp, color = Color.Black.copy(alpha = 0.06f), shape = RoundedCornerShape(18.dp)),
            )
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean,
    badge: String? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) DSColors.fairway else DSColors.warmWhite)
            .border(width = 1.dp, color = Color.Black.copy(alpha = 0.06f), shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) Color.White else DSColors.inkGreen,
            modifier = Modifier.size(18.dp),
        )
        badge?.let {
            Text(
                text = it,
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(DSColors.trophyGold)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun CategoryBar(
    categories: List<com.mygolfswing.app.data.model.V2PlazaCategoryChip>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = DSSpacing.lg, vertical = DSSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(categories, key = { it.id }) { chip ->
            CategoryPill(
                label = chip.name,
                selected = chip.id == selected,
                onClick = { onSelect(chip.id) },
            )
        }
    }
}

@Composable
private fun CategoryPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val fg = if (selected) Color.White else DSColors.inkGreen.copy(alpha = 0.7f)
    val border = if (selected) Color.Transparent else Color.Black.copy(alpha = 0.06f)
    val shadowColor = if (selected) DSColors.fairway.copy(alpha = 0.25f) else Color.Transparent
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .shadow(
                elevation = if (selected) 3.dp else 0.dp,
                shape = RoundedCornerShape(50),
                ambientColor = shadowColor,
                spotColor = shadowColor,
            )
            .clip(RoundedCornerShape(50))
            .background(if (selected) DSColors.fairway else DSColors.sandBeige)
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun WatchHistorySection(
    items: List<V2PlazaWatchHistoryItem>,
    onOpen: (UUID) -> Unit,
    onOpenAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DSSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "最近看过",
                style = DSTypography.caption,
                color = DSColors.inkGreen,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "全部 ${items.size}",
                style = DSTypography.caption,
                color = DSColors.fairway,
                modifier = Modifier.clickable(onClick = onOpenAll),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = DSSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.saveKey }) { item ->
                WatchHistoryChip(item = item, onClick = { onOpen(item.videoId) })
            }
        }
    }
}

@Composable
private fun WatchHistoryChip(item: V2PlazaWatchHistoryItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.cardWhite)
            .border(width = 1.dp, color = Color.Black.copy(alpha = 0.06f), shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(DSColors.fairway.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(DSColors.fairway),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(0.62f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = item.title,
                color = DSColors.inkGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = historyProgressLabel(item.progress, item.coachName),
                color = DSColors.shadowGray,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
    }
}

private fun historyProgressLabel(progress: Double, coachName: String): String = when {
    progress >= 0.95 -> "$coachName · 已看完"
    progress > 0.0 -> "$coachName · 已看 ${(progress * 100).toInt()}%"
    else -> coachName
}

@Composable
private fun SavedCollectionBanner(
    savedCount: Int,
    visibleCount: Int,
    keyword: String,
    filterLabel: String?,
    onClearFilters: () -> Unit,
) {
    val hasSearch = keyword.isNotBlank()
    val hasFilter = !filterLabel.isNullOrBlank()
    val hasActiveFilter = hasSearch || hasFilter
    val titleSuffix = listOfNotNull(
        keyword.takeIf { it.isNotBlank() }?.let { "搜索：$it" },
        filterLabel?.takeIf { it.isNotBlank() }?.let { "分类：$it" },
    ).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DSColors.cardWhite)
            .border(width = 1.dp, color = DSColors.borderLine, shape = RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(DSColors.trophyGold.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Bookmark, null, tint = DSColors.trophyGold, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (hasActiveFilter) "已保存 · $titleSuffix" else "我的已保存",
                style = DSTypography.bodyBold,
                color = DSColors.inkGreen,
                maxLines = 1,
            )
            Text(
                if (hasActiveFilter) "匹配 $visibleCount / 共 $savedCount 条收藏" else "快速回到收藏过的广场教学视频",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
                maxLines = 1,
            )
        }
        Text(
            text = if (hasActiveFilter) "清除" else "$savedCount 条",
            style = DSTypography.caption,
            color = DSColors.trophyGold,
            fontWeight = FontWeight.SemiBold,
            modifier = if (hasActiveFilter) {
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.trophyGold.copy(alpha = 0.12f))
                    .clickable(onClick = onClearFilters)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            } else {
                Modifier
            },
        )
    }
}

@Composable
private fun SavedCollectionFilterBar(
    filters: List<SavedPlazaFilterChip>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = DSSpacing.lg, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            CategoryPill(label = "全部", selected = selectedId == null, onClick = { onSelect(null) })
        }
        items(filters, key = { it.id }) { chip ->
            CategoryPill(
                label = chip.label,
                selected = chip.id == selectedId,
                onClick = { onSelect(chip.id) },
            )
        }
    }
}

@Composable
private fun SearchResultBanner(
    keyword: String,
    count: Int,
    isLoading: Boolean,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DSColors.cardWhite)
            .border(width = 1.dp, color = DSColors.borderLine, shape = RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(DSColors.fairway.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Search, null, tint = DSColors.fairway, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("搜索：$keyword", style = DSTypography.bodyBold, color = DSColors.inkGreen)
            Text(
                if (isLoading) "正在搜索广场视频…" else "找到 $count 条相关内容",
                style = DSTypography.caption,
                color = DSColors.shadowGray,
            )
        }
        Text(
            text = "清除",
            style = DSTypography.caption,
            color = DSColors.fairway,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(DSColors.mistGreen)
                .clickable(onClick = onClear)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = DSColors.fairway, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
        Text(
            "正在加载视频…",
            style = DSTypography.caption,
            color = DSColors.shadowGray,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun MockDataBanner() {
    Text(
        text = "当前为离线示例数据，请检查网络后下拉刷新",
        fontSize = 12.sp,
        color = DSColors.shadowGray,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg, vertical = 4.dp),
    )
}

@Composable
private fun EmptyState(
    isLoading: Boolean,
    hasError: Boolean,
    savedOnly: Boolean,
    hasSearch: Boolean,
    searchKeyword: String,
    categoryName: String,
    onRetry: () -> Unit,
    onSavedEmptyAction: (() -> Unit)? = null,
) {
    val isCategoryEmpty = !isLoading && !hasError && !hasSearch && !savedOnly
    val title = when {
        isLoading -> "正在加载视频"
        hasError -> "加载失败"
        hasSearch -> "没有找到相关视频"
        savedOnly -> "还没有保存的视频"
        else -> "「$categoryName」暂时没有视频"
    }
    val message = when {
        isLoading -> "正在加载视频…"
        hasError -> "视频加载失败，请检查网络后重试"
        hasSearch -> "没有找到「$searchKeyword」相关视频"
        savedOnly -> "点卡片上的书签加入这里，之后可以快速回看"
        else -> "换一个分类试试"
    }
    val icon = when {
        hasError -> Icons.Filled.ErrorOutline
        savedOnly -> Icons.Filled.BookmarkBorder
        isCategoryEmpty -> Icons.Filled.CheckBoxOutlineBlank
        else -> Icons.Filled.Search
    }
    val iconTint = if (hasError) DSColors.flagRed.copy(alpha = 0.7f) else DSColors.shadowGray.copy(alpha = 0.75f)
    val titleColor = if (hasError || isCategoryEmpty) DSColors.shadowGray else DSColors.inkGreen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        if (!isLoading) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(if (isCategoryEmpty) 36.dp else 30.dp),
            )
        }
        Text(
            text = title,
            color = titleColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = message,
            color = DSColors.shadowGray.copy(alpha = 0.78f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        if (hasError && !isLoading) {
            Text(
                text = "重试",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.fairway)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        if (savedOnly && !hasSearch && onSavedEmptyAction != null) {
            Text(
                text = "去广场看看",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.fairway)
                    .clickable(onClick = onSavedEmptyAction)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun LoadMoreFooter(
    isLoadingMore: Boolean,
    hasMore: Boolean,
    hasVideos: Boolean,
    onScrollTop: () -> Unit,
) {
    if (!hasVideos) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DSSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoadingMore) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(color = DSColors.fairway, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("加载更多…", color = DSColors.shadowGray, fontSize = 12.sp)
            }
        } else if (!hasMore) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(1.dp)
                            .background(DSColors.shadowGray.copy(alpha = 0.35f)),
                    )
                    Text(
                        text = "已经到底了",
                        style = DSTypography.caption,
                        color = DSColors.shadowGray,
                    )
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(1.dp)
                            .background(DSColors.shadowGray.copy(alpha = 0.35f)),
                    )
                }
                Row(
                    modifier = Modifier.clickable(onClick = onScrollTop),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "回到顶部",
                        style = DSTypography.cardTitle,
                        color = DSColors.deepGreen,
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = null,
                        tint = DSColors.deepGreen,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }
    }
}

/** 通过 video id 找到视频对象（详情页用） */
fun findPlazaVideoById(id: UUID): V2PlazaVideo? =
    V2PlazaStore.heroVideo.value?.takeIf { it.id == id }
        ?: V2PlazaStore.videos.value.firstOrNull { it.id == id }

private data class SavedPlazaFilterChip(
    val id: String,
    val label: String,
)

private fun V2PlazaVideo.savedFilterChip(): SavedPlazaFilterChip {
    val label = categoryDisplayName?.takeIf { it.isNotBlank() } ?: topic.displayName
    val id = sourceCategoryId?.takeIf { it.isNotBlank() } ?: label
    return SavedPlazaFilterChip(id = id, label = label)
}

private fun V2PlazaVideo.matchesSavedFilter(filterId: String?): Boolean {
    if (filterId.isNullOrBlank()) return true
    val chip = savedFilterChip()
    return chip.id == filterId || chip.label == filterId
}

private fun V2PlazaVideo.matchesPlazaKeyword(keyword: String): Boolean {
    val tokens = keyword
        .trim()
        .split(Regex("\\s+"))
        .map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
    if (tokens.isEmpty()) return true
    val haystack = listOfNotNull(
        title,
        coach.displayName,
        categoryDisplayName,
        topic.displayName,
        sourceCategoryId,
        sourceParentCategoryId,
    ).joinToString(" ").lowercase()
    return tokens.all { haystack.contains(it) }
}
