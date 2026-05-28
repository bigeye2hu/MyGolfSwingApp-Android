package com.mygolfswing.app.ui.plaza

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val filtered = remember(videos, selectedCat) { V2PlazaStore.filteredVideos() }
    val hero = filtered.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.warmWhite),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlazaHeader()
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColors.cardWhite.copy(alpha = 0.92f))
            .padding(horizontal = DSSpacing.lg, vertical = DSSpacing.md),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "广场",
            style = DSTypography.display,
            color = DSColors.inkGreen,
        )
        Text(
            text = "教练精选 · 高手分享 · 看一段，练得更明白。",
            style = DSTypography.caption,
            color = DSColors.shadowGray,
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
    val fg = if (selected) Color.White else DSColors.inkGreen
    val border = if (selected) DSColors.fairway else DSColors.borderLine
    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
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
    V2PlazaStore.videos.value.firstOrNull { it.id == id }
