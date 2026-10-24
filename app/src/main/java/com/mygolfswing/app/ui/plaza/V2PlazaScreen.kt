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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.data.model.V2PlazaSortOption
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID

enum class V2PlazaLoginTarget {
    SAVED_COLLECTION,
    WATCH_HISTORY,
}

/**
 * V2 广场主页（对齐 iOS `V2PlazaView`）。
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun V2PlazaScreen(
    onOpenVideo: (UUID) -> Unit = {},
    onRequestLogin: () -> Unit = {},
    onRequestLoginForTarget: (V2PlazaLoginTarget) -> Unit = { onRequestLogin() },
) {
    LaunchedEffect(Unit) {
        V2PlazaStore.refresh()
    }

    val videos by V2PlazaStore.videos.collectAsState()
    val categories by V2PlazaStore.categories.collectAsState()
    val selectedCat by V2PlazaStore.selectedCategoryId.collectAsState()
    val selectedSort by V2PlazaStore.selectedSort.collectAsState()
    val isLoading by V2PlazaStore.isLoading.collectAsState()
    val isLoadingMore by V2PlazaStore.isLoadingMore.collectAsState()
    val hasMore by V2PlazaStore.hasMore.collectAsState()
    val isUsingMock by V2PlazaStore.isUsingMock.collectAsState()
    val lastError by V2PlazaStore.lastError.collectAsState()
    val searchKeyword by V2PlazaStore.searchKeyword.collectAsState()
    val hero by V2PlazaStore.heroVideo.collectAsState()
    val profile by V2ProfileStore.profile.collectAsState()
    val favoriteVideos by V2PlazaStore.favoriteVideos.collectAsState()
    val isLoadingFavorites by V2PlazaStore.isLoadingFavorites.collectAsState()
    val favoriteErrorMessage by V2PlazaStore.favoriteErrorMessage.collectAsState()
    val favoriteOpenRequest by V2PlazaStore.favoriteOpenRequest.collectAsState()
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchDraft by rememberSaveable { mutableStateOf(searchKeyword) }
    var favoritesVisible by rememberSaveable { mutableStateOf(false) }
    var consumedFavoriteOpenRequest by rememberSaveable { mutableStateOf(0L) }
    LaunchedEffect(profile.isLoggedIn) {
        if (!profile.isLoggedIn) favoritesVisible = false
    }
    LaunchedEffect(profile.isLoggedIn, favoriteOpenRequest) {
        if (profile.isLoggedIn && favoriteOpenRequest > consumedFavoriteOpenRequest) {
            consumedFavoriteOpenRequest = favoriteOpenRequest
            favoritesVisible = true
            V2PlazaStore.refreshFavoriteVideos()
        }
    }
    val filtered = V2PlazaStore.filteredVideos()
    val selectedCategoryName = categories.firstOrNull { it.id == selectedCat }?.name ?: "该分类"
    val hasSearch = searchKeyword.isNotBlank()
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var pullRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            delay(120)
            searchFocusRequester.requestFocus()
        }
    }

    PullToRefreshBox(
        isRefreshing = pullRefreshing,
        onRefresh = {
            if (!pullRefreshing) {
                pullRefreshing = true
                V2PlazaStore.refresh(refreshCoachProfiles = true)
                coroutineScope.launch {
                    delay(250)
                    while (V2PlazaStore.isLoading.value) delay(50)
                    pullRefreshing = false
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
        ) {
            V2RoomBackground(Modifier.fillMaxSize())
            Column(modifier = Modifier.fillMaxSize()) {
            PlazaHeader(
                onSearch = {
                    searchDraft = searchKeyword
                    searchExpanded = true
                },
                onOpenFavorites = {
                    if (profile.isLoggedIn) {
                        favoritesVisible = true
                        V2PlazaStore.refreshFavoriteVideos()
                    } else {
                        onRequestLoginForTarget(V2PlazaLoginTarget.SAVED_COLLECTION)
                    }
                },
            )
            if (searchExpanded || hasSearch) {
                PlazaSearchBar(
                    value = searchDraft,
                    onValueChange = { searchDraft = it },
                    onClear = {
                        searchDraft = ""
                        V2PlazaStore.clearSearch()
                    },
                    onSubmit = {
                        val keyword = searchDraft.trim()
                        if (keyword.isNotEmpty()) {
                            searchDraft = keyword
                            V2PlazaStore.setSearchKeyword(keyword)
                            focusManager.clearFocus()
                        }
                    },
                    onCancel = {
                        searchDraft = ""
                        searchExpanded = false
                        focusManager.clearFocus()
                        V2PlazaStore.clearSearch()
                    },
                    focusRequester = searchFocusRequester,
                    modifier = Modifier.padding(horizontal = DSSpacing.lg),
                )
            }
            if (isUsingMock) {
                MockDataBanner()
            }
            if (hasSearch) {
                SearchResultBanner(
                    keyword = searchKeyword,
                    count = filtered.size,
                )
            } else {
                hero?.let {
                    V2PlazaHeroCard(
                        video = it,
                        onTap = { onOpenVideo(it.id) },
                        modifier = Modifier.padding(horizontal = DSSpacing.lg),
                    )
                }
                SortAndCategoryBar(
                    categories = categories,
                    selected = selectedCat,
                    selectedSort = selectedSort,
                    onSelect = { V2PlazaStore.selectCategory(it) },
                    onSelectSort = V2PlazaStore::selectSort,
                )
            }

            if (isLoading && videos.isEmpty()) {
                LoadingState()
            } else if (filtered.isEmpty()) {
                EmptyState(
                    isLoading = isLoading,
                    errorMessage = lastError,
                    hasSearch = hasSearch,
                    categoryName = selectedCategoryName,
                    onRetry = { V2PlazaStore.refresh() },
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
                            V2PlazaStore.loadMoreIfNeeded(video.id)
                        }
                        V2PlazaGridCard(
                            video = video,
                            isGuest = !profile.isLoggedIn,
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
                            hasMore = hasMore,
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

    if (favoritesVisible) {
        V2PlazaFavoritesSheet(
            videos = favoriteVideos,
            isLoading = isLoadingFavorites,
            errorMessage = favoriteErrorMessage,
            onRefresh = V2PlazaStore::refreshFavoriteVideos,
            onVideoTap = { video ->
                favoritesVisible = false
                onOpenVideo(video.id)
            },
            onFavoriteTap = { video -> V2PlazaStore.toggleSave(video.id) },
            onDismiss = { favoritesVisible = false },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun V2PlazaFavoritesSheet(
    videos: List<V2PlazaVideo>,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onVideoTap: (V2PlazaVideo) -> Unit,
    onFavoriteTap: (V2PlazaVideo) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("plaza-favorites-sheet"),
        containerColor = DSColors.warmWhite,
        contentColor = DSColors.inkGreen,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.shadowGray.copy(alpha = 0.28f)),
            )
        },
    ) {
        V2PlazaFavoritesContent(
            videos = videos,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onRefresh = onRefresh,
            onVideoTap = onVideoTap,
            onFavoriteTap = onFavoriteTap,
            onDismiss = onDismiss,
            modifier = Modifier.fillMaxHeight(0.94f),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun V2PlazaFavoritesContent(
    videos: List<V2PlazaVideo>,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onVideoTap: (V2PlazaVideo) -> Unit,
    onFavoriteTap: (V2PlazaVideo) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DSColors.warmWhite),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DSSpacing.lg, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "我的收藏",
                    color = DSColors.inkGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${videos.size} 个视频",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(DSColors.mistGreen)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭",
                    tint = DSColors.inkGreen,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = isLoading && videos.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when {
                isLoading && videos.isEmpty() -> {
                    PlazaFavoritesLoadingState()
                }
                errorMessage != null && videos.isEmpty() -> {
                    PlazaFavoritesErrorState(
                        message = errorMessage,
                        onRetry = onRefresh,
                    )
                }
                videos.isEmpty() -> {
                    PlazaFavoritesEmptyState()
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = DSSpacing.lg,
                            top = DSSpacing.md,
                            end = DSSpacing.lg,
                            bottom = DSSpacing.xl,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(videos, key = { it.id }) { video ->
                            V2PlazaGridCard(
                                video = video,
                                isGuest = false,
                                onTap = { onVideoTap(video) },
                                onSaveTap = { onFavoriteTap(video) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlazaFavoritesLoadingState() {
    Column(
        modifier = Modifier
            .testTag("plaza-favorites-loading-state")
            .fillMaxWidth()
            .padding(vertical = DSSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            color = DSColors.fairway,
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
        )
        Text("正在加载收藏…", style = DSTypography.caption, color = DSColors.shadowGray)
    }
}

@Composable
private fun PlazaFavoritesErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .testTag("plaza-favorites-error-state")
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.xl, vertical = DSSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = DSColors.flagRed.copy(alpha = 0.75f),
            modifier = Modifier.size(30.dp),
        )
        Text("收藏加载失败", style = DSTypography.cardTitle, color = DSColors.shadowGray)
        Text(
            text = message,
            style = DSTypography.caption,
            color = DSColors.shadowGray.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Text(
            text = "重试",
            style = DSTypography.caption,
            color = DSColors.fairway,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onRetry)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun PlazaFavoritesEmptyState() {
    Column(
        modifier = Modifier
            .testTag("plaza-favorites-empty-state")
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.xl, vertical = DSSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.BookmarkBorder,
            contentDescription = null,
            tint = DSColors.shadowGray.copy(alpha = 0.6f),
            modifier = Modifier.size(38.dp),
        )
        Text("还没有收藏的视频", style = DSTypography.cardTitle, color = DSColors.shadowGray)
        Text(
            text = "看到想复习的教学视频，点卡片上的书签保存到这里",
            style = DSTypography.caption,
            color = DSColors.shadowGray.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlazaHeader() {
    PlazaHeader(
        onSearch = {},
        onOpenFavorites = {},
    )
}

@Composable
internal fun PlazaHeader(
    onSearch: () -> Unit,
    onOpenFavorites: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.warmWhite)
            .padding(horizontal = DSSpacing.lg)
            .padding(top = DSSpacing.sm, bottom = DSSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
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
            icon = Icons.Filled.Bookmark,
            contentDescription = "我的收藏",
            onClick = onOpenFavorites,
        )
        Spacer(modifier = Modifier.width(10.dp))
        HeaderIconButton(
            icon = Icons.Filled.Search,
            contentDescription = "搜索视频",
            onClick = onSearch,
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    badge: String? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(DSColors.warmWhite)
            .border(width = 1.dp, color = Color.Black.copy(alpha = 0.06f), shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = DSColors.inkGreen,
            modifier = Modifier.size(16.dp),
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
internal fun PlazaSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val canSubmit = value.trim().isNotEmpty()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.mistGreen)
            .border(
                width = 1.dp,
                color = DSColors.fairway.copy(alpha = if (focused) 0.32f else 0.12f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = DSColors.shadowGray,
            modifier = Modifier.size(14.dp),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text("搜索视频、标签、教练", color = DSColors.shadowGray, fontSize = 15.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 15.sp),
                cursorBrush = SolidColor(DSColors.fairway),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { if (canSubmit) onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plaza-search-field")
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .onFocusChanged { focused = it.isFocused },
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Filled.Cancel,
                contentDescription = "清除搜索",
                tint = DSColors.shadowGray.copy(alpha = 0.6f),
                modifier = Modifier.size(15.dp).clickable(onClick = onClear),
            )
        }
        Icon(
            imageVector = Icons.Filled.ArrowCircleRight,
            contentDescription = "执行搜索",
            tint = if (canSubmit) DSColors.fairway else DSColors.shadowGray.copy(alpha = 0.45f),
            modifier = Modifier.size(32.dp).clickable(enabled = canSubmit, onClick = onSubmit).padding(5.dp),
        )
        Text(
            text = "取消",
            color = DSColors.fairway,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onCancel),
        )
    }
}

@Composable
private fun SortAndCategoryBar(
    categories: List<com.mygolfswing.app.data.model.V2PlazaCategoryChip>,
    selected: String,
    selectedSort: V2PlazaSortOption,
    onSelect: (String) -> Unit,
    onSelectSort: (V2PlazaSortOption) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .width(112.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(50))
                .background(DSColors.sandBeige)
                .border(1.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(50))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf(V2PlazaSortOption.LATEST, V2PlazaSortOption.POPULAR).forEach { option ->
                val isSelected = option == selectedSort
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) DSColors.fairway else Color.Transparent)
                        .clickable { onSelectSort(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option.displayText,
                        color = if (isSelected) DSColors.cardWhite else DSColors.inkGreen.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
        LazyRow(
            modifier = Modifier.weight(1f),
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
internal fun SearchResultBanner(
    keyword: String,
    count: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "搜索「$keyword」",
            style = DSTypography.caption,
            color = DSColors.shadowGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = "· $count 个视频",
            style = DSTypography.caption,
            color = DSColors.fairway,
        )
        Spacer(Modifier.weight(1f))
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
    errorMessage: String?,
    hasSearch: Boolean,
    categoryName: String,
    onRetry: () -> Unit,
) {
    val hasError = errorMessage != null
    val isCategoryEmpty = !isLoading && !hasError && !hasSearch
    val title = when {
        isLoading -> "正在加载视频"
        hasError -> "加载失败"
        hasSearch -> "没有找到相关视频"
        else -> "「$categoryName」暂时没有视频"
    }
    val message = when {
        isLoading -> "正在加载视频…"
        hasError -> errorMessage.orEmpty()
        hasSearch -> "换个关键词试试"
        else -> "换一个分类试试"
    }
    val icon = when {
        hasError -> Icons.Filled.ErrorOutline
        isCategoryEmpty -> Icons.Filled.CheckBoxOutlineBlank
        else -> Icons.Filled.FindInPage
    }
    val iconTint = if (hasError) DSColors.flagRed.copy(alpha = 0.7f) else DSColors.shadowGray.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DSSpacing.xl, vertical = DSSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!isLoading) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(if (hasError) 28.dp else 36.dp),
            )
        }
        Text(
            text = title,
            style = DSTypography.cardTitle,
            color = DSColors.shadowGray,
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
                style = DSTypography.caption,
                color = DSColors.fairway,
                modifier = Modifier
                    .clickable(onClick = onRetry)
                    .padding(top = 4.dp),
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
