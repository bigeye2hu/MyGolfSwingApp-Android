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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import java.util.UUID

/**
 * V2 广场主页（对齐 iOS `V2PlazaView`）。
 */
@Composable
fun V2PlazaScreen(
    onOpenVideo: (UUID) -> Unit = {},
) {
    LaunchedEffect(Unit) {
        V2PlazaStore.refresh()
    }

    val videos by V2PlazaStore.videos.collectAsState()
    val categories by V2PlazaStore.categories.collectAsState()
    val selectedCat by V2PlazaStore.selectedCategoryId.collectAsState()
    val isLoading by V2PlazaStore.isLoading.collectAsState()
    val isUsingMock by V2PlazaStore.isUsingMock.collectAsState()
    val searchKeyword by V2PlazaStore.searchKeyword.collectAsState()
    val hero by V2PlazaStore.heroVideo.collectAsState()
    val filtered = videos

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlazaHeader(
                searchKeyword = searchKeyword,
                onSearchChange = { V2PlazaStore.setSearchKeyword(it) },
                onClearSearch = { V2PlazaStore.clearSearch() },
            )
            if (isUsingMock) {
                MockDataBanner()
            }
            hero?.let { V2PlazaHeroCard(video = it, onTap = { onOpenVideo(it.id) }) }
            CategoryBar(
                categories = categories,
                selected = selectedCat,
                onSelect = { V2PlazaStore.selectCategory(it) },
            )

            if (isLoading && videos.isEmpty()) {
                LoadingState()
            } else if (filtered.isEmpty()) {
                EmptyState(isLoading = isLoading)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        horizontal = DSSpacing.lg,
                        vertical = DSSpacing.sm,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(filtered, key = { it.id }) { video ->
                        V2PlazaGridCard(
                            video = video,
                            onTap = { onOpenVideo(video.id) },
                            onSaveTap = { V2PlazaStore.toggleSave(video.id) },
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
        onSearchChange = {},
        onClearSearch = {},
    )
}

@Composable
private fun PlazaHeader(
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
) {
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
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(DSColors.fairway),
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
                    modifier = Modifier.padding(start = 24.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DSColors.warmWhite)
                    .border(width = 1.dp, color = Color.Black.copy(alpha = 0.06f), shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "搜索",
                    tint = DSColors.inkGreen,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

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
                Text("搜索视频、教练或关键词", style = DSTypography.caption, color = DSColors.shadowGray)
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

@Composable
private fun CategoryBar(
    categories: List<com.mygolfswing.app.data.model.V2PlazaCategoryChip>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = DSSpacing.lg, vertical = DSSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
    val bg = if (selected) DSColors.fairway else DSColors.cardWhite
    val fg = if (selected) Color.White else DSColors.inkGreen.copy(alpha = 0.7f)
    val border = if (selected) Color.Transparent else Color.Black.copy(alpha = 0.06f)
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        color = fg,
        modifier = Modifier
            .shadow(if (selected) 3.dp else 0.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(if (selected) bg else DSColors.sandBeige)
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = DSColors.fairway)
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
private fun EmptyState(isLoading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isLoading) "加载中…" else "该分类暂无视频，换一个试试",
            color = DSColors.shadowGray,
            fontSize = 13.sp,
        )
    }
}

/** 通过 video id 找到视频对象（详情页用） */
fun findPlazaVideoById(id: UUID): V2PlazaVideo? =
    V2PlazaStore.heroVideo.value?.takeIf { it.id == id }
        ?: V2PlazaStore.videos.value.firstOrNull { it.id == id }
