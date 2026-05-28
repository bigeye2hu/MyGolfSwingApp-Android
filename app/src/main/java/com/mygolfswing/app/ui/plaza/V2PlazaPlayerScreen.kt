package com.mygolfswing.app.ui.plaza

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import java.util.UUID

/**
 * 广场视频播放页（对齐 iOS `VideoPlazaPlayerPage` 精简版）。
 *
 * 使用 Media3 ExoPlayer 播放视频，支持暂停 / 进度跟随。
 */
@OptIn(UnstableApi::class)
@Composable
fun V2PlazaPlayerScreen(
    videoId: UUID,
    onClose: () -> Unit = {},
) {
    val videos by V2PlazaStore.videos.collectAsState()
    val video = videos.firstOrNull { it.id == videoId }
    val context = LocalContext.current

    if (video == null) {
        V2MissingEntityScreen(message = "视频不存在", onBack = onClose, title = "广场视频")
        return
    }

    val exoPlayer = remember(video.videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.videoUrl))
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
        )

        TopBar(
            title = video.title,
            sourceLabel = video.coach.displayName,
            saved = video.isSaved,
            onClose = onClose,
            onSave = { V2PlazaStore.toggleSave(video.id) },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        BottomMeta(
            title = video.title,
            meta = "${video.coach.displayName} · ${video.categoryDisplayName ?: video.topic.displayName}",
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun TopBar(
    title: String,
    sourceLabel: String,
    saved: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "视频",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = DSColors.fairway,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "来自 $sourceLabel", fontSize = 10.sp, color = DSColors.trophyGold)
            }
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (saved) DSColors.trophyGold else Color.White.copy(alpha = 0.18f))
                .clickable { onSave() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = "收藏",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun BottomMeta(title: String, meta: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2)
        Text(text = meta, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
    }
}
