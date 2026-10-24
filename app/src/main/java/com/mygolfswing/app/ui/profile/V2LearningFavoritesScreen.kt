package com.mygolfswing.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors

/** 个人学习收藏页，对齐 iOS `V2LearningFavoritesView`. */
@Composable
fun V2LearningFavoritesScreen(
    onBack: () -> Unit,
) {
    val videos by V2PlazaStore.favoriteVideos.collectAsState()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { V2PlazaStore.refreshFavoriteVideos() }

    V2LearningFavoritesContent(
        videos = videos,
        query = query,
        onQueryChange = { query = it },
        onBack = onBack,
    )
}

@Composable
internal fun V2LearningFavoritesContent(
    videos: List<V2PlazaVideo>,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    val trimmed = query.trim()
    val filtered = remember(videos, trimmed) {
        if (trimmed.isEmpty()) videos
        else videos.filter { it.title.contains(trimmed, ignoreCase = true) }
    }

    V2RoomPage(Modifier.fillMaxSize().testTag("learning-favorites-screen")) {
        Column(Modifier.fillMaxSize()) {
            V2NavTopBarCentered(title = "学习收藏", onBack = onBack)
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .v2NavigationBarPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp))
                        .background(DSColors.cardWhite).padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text("搜索收藏的视频或笔记", color = DSColors.shadowGray, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(color = DSColors.inkGreen, fontSize = 14.sp),
                        cursorBrush = SolidColor(DSColors.fairway),
                        modifier = Modifier.fillMaxWidth().testTag("learning-favorites-search"),
                    )
                }

                when {
                    filtered.isEmpty() -> Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            v2LearningFavoritesEmptyCopy(query),
                            color = DSColors.inkGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    else -> filtered.forEach { video ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(DSColors.cardWhite).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(width = 72.dp, height = 52.dp)
                                    .clip(RoundedCornerShape(10.dp)).background(DSColors.mistGreen),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, tint = DSColors.fairway, modifier = Modifier.size(21.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    video.title.ifBlank { "公开视频" },
                                    color = DSColors.inkGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text("视频", color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun v2LearningFavoritesEmptyCopy(query: String): String =
    if (query.isEmpty()) "还没有学习收藏" else "没有匹配的收藏"
