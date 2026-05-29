package com.mygolfswing.app.ui.plaza

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * 广场视频播放页（对齐 iOS `VideoPlazaPlayerPage`）。
 */
@OptIn(UnstableApi::class)
@Composable
fun V2PlazaPlayerScreen(
    videoId: UUID,
    onClose: () -> Unit = {},
) {
    val videos by V2PlazaStore.videos.collectAsState()
    val hero by V2PlazaStore.heroVideo.collectAsState()
    val video = hero?.takeIf { it.id == videoId } ?: videos.firstOrNull { it.id == videoId }
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var showPlayPauseIcon by remember { mutableStateOf(false) }
    var currentTime by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var playbackError by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var liked by remember(videoId) { mutableStateOf(false) }
    var wasPlayingBeforeSeek by remember { mutableStateOf(false) }

    if (video == null) {
        V2MissingEntityScreen(message = "视频不存在", onBack = onClose, title = "广场视频")
        return
    }
    if (video.videoUrl.isBlank()) {
        PlazaPlaybackErrorOverlay(onClose = onClose, onRetry = { V2PlazaStore.refresh() })
        return
    }

    val exoPlayer = remember(video.videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.videoUrl))
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = true
                isPlaying = false
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    exoPlayer.seekTo(0)
                    exoPlayer.play()
                    currentTime = 0f
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentTime = (exoPlayer.currentPosition / 1000f).coerceAtLeast(0f)
            val durMs = exoPlayer.duration
            if (durMs > 0) duration = durMs / 1000f
            isPlaying = exoPlayer.isPlaying
            delay(100)
        }
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
                    useController = false
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            update = { it.player = exoPlayer },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                        isPlaying = false
                    } else {
                        exoPlayer.play()
                        isPlaying = true
                    }
                    showPlayPauseIcon = true
                },
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f)),
                    ),
                ),
        )

        TopBar(onClose = onClose, modifier = Modifier.align(Alignment.TopCenter))

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BottomLeftInfo(
                    coachName = video.coach.displayName,
                    title = video.title,
                    category = video.categoryDisplayName ?: video.topic.displayName,
                    viewCount = video.viewCount,
                    modifier = Modifier.weight(1f),
                )
                RightSidebar(
                    liked = liked,
                    saved = video.isSaved,
                    saveCount = video.saveCount,
                    onLike = {
                        liked = !liked
                        toastMessage = if (liked) "已点赞" else "已取消点赞"
                    },
                    onSave = {
                        V2PlazaStore.toggleSave(video.id)
                        toastMessage = if (video.isSaved) "已取消收藏" else "已收藏"
                    },
                    onComment = { toastMessage = "评论功能开发中" },
                    onFeedback = { toastMessage = "反馈功能开发中" },
                )
            }
            PlazaProgressBar(
                currentTime = currentTime,
                duration = duration,
                onSeekStart = {
                    wasPlayingBeforeSeek = exoPlayer.isPlaying
                    exoPlayer.pause()
                },
                onSeekFinished = {
                    exoPlayer.seekTo((it * 1000).toLong())
                    currentTime = it
                    if (wasPlayingBeforeSeek) exoPlayer.play()
                },
            )
        }

        AnimatedVisibility(
            visible = showPlayPauseIcon,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.size(56.dp),
                )
            }
            LaunchedEffect(showPlayPauseIcon) {
                if (showPlayPauseIcon) {
                    delay(800)
                    showPlayPauseIcon = false
                }
            }
        }

        if (playbackError) {
            PlazaPlaybackErrorOverlay(
                onClose = onClose,
                onRetry = {
                    playbackError = false
                    exoPlayer.setMediaItem(MediaItem.fromUri(video.videoUrl))
                    exoPlayer.prepare()
                    exoPlayer.play()
                    isPlaying = true
                },
            )
        }

        toastMessage?.let { toast ->
            Toast(text = toast, modifier = Modifier.align(Alignment.TopCenter))
            LaunchedEffect(toast) {
                delay(1600)
                toastMessage = null
            }
        }
    }
}

@Composable
private fun PlazaPlaybackErrorOverlay(onClose: () -> Unit, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF1F2937).copy(alpha = 0.96f))
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("视频暂时无法加载", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "可能是网络问题，或这条广场视频源已不可用。请检查网络后重试。",
                color = Color(0xFFD1D5DB),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DSColors.fairway)
                    .clickable(onClick = onRetry),
                contentAlignment = Alignment.Center,
            ) {
                Text("重试", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("返回", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TopBar(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(icon = Icons.Filled.Close, contentDescription = "关闭", onClick = onClose)
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.Verified, null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
            Text("正版授权", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BottomLeftInfo(
    coachName: String,
    title: String,
    category: String,
    viewCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("@$coachName", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
        )
        Text(
            text = "#$category",
            color = Color(0xFFC084FC),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "${formatCount(viewCount)} 播放",
            color = Color(0xFF9CA3AF),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun RightSidebar(
    liked: Boolean,
    saved: Boolean,
    saveCount: Int,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onComment: () -> Unit,
    onFeedback: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SidebarButton(
            icon = Icons.Filled.Favorite,
            label = "喜欢",
            tint = if (liked) Color(0xFFEF4444) else Color.White,
            onClick = onLike,
        )
        SidebarButton(
            icon = Icons.Filled.ChatBubbleOutline,
            label = "评论",
            tint = Color.White,
            onClick = onComment,
        )
        SidebarButton(
            icon = if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            label = if (saved) "已保存" else formatCount(saveCount),
            tint = if (saved) Color(0xFFF59E0B) else Color.White,
            onClick = onSave,
        )
        SidebarButton(
            icon = Icons.Filled.Flag,
            label = "反馈",
            tint = Color.White,
            onClick = onFeedback,
        )
    }
}

@Composable
private fun SidebarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(56.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PlazaProgressBar(
    currentTime: Float,
    duration: Float,
    onSeekStart: () -> Unit,
    onSeekFinished: (Float) -> Unit,
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekTime by remember { mutableFloatStateOf(0f) }
    val maxDuration = duration.coerceAtLeast(0.1f)
    val value = if (isSeeking) seekTime else currentTime.coerceIn(0f, maxDuration)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Slider(
            value = value,
            onValueChange = {
                if (!isSeeking) {
                    isSeeking = true
                    onSeekStart()
                }
                seekTime = it
            },
            onValueChangeFinished = {
                onSeekFinished(seekTime)
                isSeeking = false
            },
            valueRange = 0f..maxDuration,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
            modifier = Modifier.height(24.dp),
        )
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Toast(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .v2StatusBarPadding()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

private fun formatCount(n: Int): String = when {
    n >= 10_000 -> "%.1fw".format(n / 10_000.0)
    n >= 1_000 -> "%.1fk".format(n / 1_000.0)
    else -> n.toString()
}
