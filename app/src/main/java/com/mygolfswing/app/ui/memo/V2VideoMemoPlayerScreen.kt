package com.mygolfswing.app.ui.memo

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.video.V2OnlineVideoMetadataFetcher
import com.mygolfswing.app.ui.common.V2DrawingOverlay
import com.mygolfswing.app.ui.common.V2DrawingState
import com.mygolfswing.app.ui.common.V2DrawingToolsPanel
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt
import java.util.UUID

/**
 * 视频 Memo 播放器（对齐 iOS `V2VideoMemoPlayerView`）。
 */
@OptIn(UnstableApi::class)
@Composable
fun V2VideoMemoPlayerScreen(
    memoId: UUID,
    onBack: () -> Unit = {},
    onEdit: (UUID) -> Unit = {},
) {
    val memos by V2MemoStore.allMemos.collectAsState()
    val memo = memos.firstOrNull { it.id == memoId }
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var showPlayPauseIcon by remember { mutableStateOf(false) }
    var showToolsPanel by remember { mutableStateOf(false) }
    var showMoreTools by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showComparisonPicker by remember { mutableStateOf(false) }
    var comparisonMemo by remember { mutableStateOf<V2Memo?>(null) }
    var playbackError by remember { mutableStateOf(false) }
    var currentTime by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var currentRateIndex by remember { mutableIntStateOf(0) }
    val playbackRates = remember { listOf(1f, 0.5f, 0.25f) }
    val drawingState = remember { V2DrawingState() }

    if (memo == null) {
        V2MissingEntityScreen(message = "视频不存在", onBack = onBack, title = "视频 Memo")
        return
    }

    val comparisonCandidates = remember(memos, memo.id) {
        memos
            .filter { it.id != memo.id && it.hasVideo && resolvePlaybackUrl(it.videoUrl) != null }
            .sortedByDescending { it.createdAt }
    }
    comparisonMemo?.let { selected ->
        VideoMemoComparisonScreen(
            primaryMemo = memo,
            secondaryMemo = selected,
            onClose = { comparisonMemo = null },
        )
        return
    }

    val playbackUrl = remember(memo.videoUrl) { resolvePlaybackUrl(memo.videoUrl) }
    var isLoading by remember(playbackUrl) { mutableStateOf(true) }
    if (playbackUrl == null) {
        VideoPlaybackUnavailableOverlay(
            state = V2PlaybackState.UnavailableNoSource,
            onBack = onBack,
            onRetry = null,
        )
        return
    }

    val player = remember(playbackUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(playbackUrl))
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = true
                isLoading = false
                isPlaying = false
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_IDLE,
                    Player.STATE_BUFFERING -> {
                        isLoading = true
                    }

                    Player.STATE_READY -> {
                        isLoading = false
                    }

                    Player.STATE_ENDED -> {
                        isLoading = false
                        player.pause()
                        player.seekTo(0)
                        player.playbackParameters = PlaybackParameters(playbackRates[currentRateIndex])
                        currentTime = 0f
                        isPlaying = false
                        showPlayPauseIcon = true
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(player) {
        while (true) {
            currentTime = (player.currentPosition / 1000f).coerceAtLeast(0f)
            val durMs = player.duration
            if (durMs > 0) duration = durMs / 1000f
            isPlaying = player.isPlaying
            delay(100)
        }
    }

    fun showCenterIcon() {
        showPlayPauseIcon = true
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
                    this.player = player
                    useController = false
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
        )

        if (isLoading && !playbackError) {
            VideoPlaybackLoadingOverlay()
        }

        if (playbackError) {
            VideoPlaybackUnavailableOverlay(
                state = V2PlaybackState.UnavailablePlaybackError,
                onBack = onBack,
                onRetry = {
                    playbackError = false
                    isLoading = true
                    player.setMediaItem(MediaItem.fromUri(playbackUrl))
                    player.prepare()
                    player.playbackParameters = PlaybackParameters(playbackRates[currentRateIndex])
                    player.play()
                    isPlaying = true
                },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = !showToolsPanel) {
                    if (showMoreTools) {
                        showMoreTools = false
                    } else if (player.isPlaying) {
                        player.pause()
                        isPlaying = false
                        showCenterIcon()
                    } else {
                        player.playbackParameters = PlaybackParameters(playbackRates[currentRateIndex])
                        player.play()
                        isPlaying = true
                        showCenterIcon()
                    }
                },
        )

        if (showToolsPanel) {
            V2DrawingOverlay(
                state = drawingState,
                modifier = Modifier.fillMaxSize(),
            )
        }

        TopBar(
            showToolsPanel = showToolsPanel,
            onBack = onBack,
            onToggleDrawing = { showToolsPanel = !showToolsPanel },
        )

        if (!showToolsPanel && !playbackError) {
            RightFloatingTools(
                speedText = playbackRates[currentRateIndex].speedText(),
                isSlow = currentRateIndex != 0,
                showMoreTools = showMoreTools,
                onSpeed = {
                    currentRateIndex = (currentRateIndex + 1) % playbackRates.size
                    player.playbackParameters = PlaybackParameters(playbackRates[currentRateIndex])
                    if (player.isPlaying) player.play()
                },
                onToggleMore = { showMoreTools = !showMoreTools },
                onEdit = {
                    player.pause()
                    isPlaying = false
                    onEdit(memo.id)
                },
                onCompare = {
                    player.pause()
                    isPlaying = false
                    showMoreTools = false
                    showComparisonPicker = true
                },
                onDelete = { showDeleteConfirm = true },
            )
        } else {
            V2DrawingToolsPanel(
                state = drawingState,
                onClose = { showToolsPanel = false },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 56.dp),
            )
        }

        if (!playbackError) BottomInfoAndProgress(
            title = memo.displayTitle ?: memo.title ?: "无标题",
            description = memo.text.orEmpty(),
            sourceLabel = memo.trainingMethod?.takeIf { it.isNotBlank() } ?: "视频备忘录",
            currentTime = currentTime,
            duration = duration,
            onSeek = { seconds ->
                player.seekTo((seconds * 1000).toLong())
                currentTime = seconds
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )

        AnimatedVisibility(
            visible = showPlayPauseIcon,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center),
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
    }

    if (showDeleteConfirm) {
        VideoMemoDeleteConfirmDialog(
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                V2MemoStore.delete(memo.id)
                onBack()
            },
        )
    }

    if (showComparisonPicker) {
        VideoComparisonPickerDialog(
            candidates = comparisonCandidates,
            onDismiss = { showComparisonPicker = false },
            onSelect = { selected ->
                showComparisonPicker = false
                comparisonMemo = selected
            },
        )
    }
}

private enum class V2PlaybackState {
    UnavailableNoSource,
    UnavailablePlaybackError,
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoMemoComparisonScreen(
    primaryMemo: V2Memo,
    secondaryMemo: V2Memo,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val primaryUrl = remember(primaryMemo.videoUrl) { resolvePlaybackUrl(primaryMemo.videoUrl) }
    val secondaryUrl = remember(secondaryMemo.videoUrl) { resolvePlaybackUrl(secondaryMemo.videoUrl) }

    if (primaryUrl == null || secondaryUrl == null) {
        VideoPlaybackUnavailableOverlay(
            state = V2PlaybackState.UnavailableNoSource,
            onBack = onClose,
            onRetry = null,
        )
        return
    }

    var isPlaying by remember { mutableStateOf(false) }
    var primaryTime by remember { mutableFloatStateOf(0f) }
    var secondaryTime by remember { mutableFloatStateOf(0f) }
    var primaryDuration by remember { mutableFloatStateOf(0f) }
    var secondaryDuration by remember { mutableFloatStateOf(0f) }
    var syncSeek by remember { mutableStateOf(false) }
    var comparisonEnded by remember { mutableStateOf(false) }
    var showDrawingTools by remember { mutableStateOf(false) }
    val comparisonDrawingState = remember { V2DrawingState() }

    val primaryPlayer = remember(primaryUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(primaryUrl))
            prepare()
        }
    }
    val secondaryPlayer = remember(secondaryUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(secondaryUrl))
            prepare()
        }
    }
    fun stopComparisonAtEnd() {
        primaryPlayer.pause()
        secondaryPlayer.pause()
        isPlaying = false
        comparisonEnded = true
    }

    DisposableEffect(primaryPlayer, secondaryPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    stopComparisonAtEnd()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                stopComparisonAtEnd()
            }
        }
        primaryPlayer.addListener(listener)
        secondaryPlayer.addListener(listener)
        onDispose {
            primaryPlayer.removeListener(listener)
            secondaryPlayer.removeListener(listener)
            primaryPlayer.release()
            secondaryPlayer.release()
        }
    }
    LaunchedEffect(isPlaying, primaryPlayer, secondaryPlayer) {
        if (isPlaying) {
            comparisonEnded = false
            primaryPlayer.play()
            secondaryPlayer.play()
        } else {
            primaryPlayer.pause()
            secondaryPlayer.pause()
        }
    }
    LaunchedEffect(primaryPlayer, secondaryPlayer) {
        while (true) {
            primaryTime = (primaryPlayer.currentPosition / 1000f).coerceAtLeast(0f)
            secondaryTime = (secondaryPlayer.currentPosition / 1000f).coerceAtLeast(0f)
            primaryPlayer.duration.takeIf { it > 0 }?.let { primaryDuration = it / 1000f }
            secondaryPlayer.duration.takeIf { it > 0 }?.let { secondaryDuration = it / 1000f }
            delay(120)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .v2StatusBarPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(icon = Icons.Filled.Close, contentDescription = "关闭对比", onClick = onClose)
                Spacer(Modifier.weight(1f))
                Text("视频对比", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SyncSeekToggle(enabled = syncSeek, onToggle = { syncSeek = !syncSeek })
                    CircleIconButton(
                        icon = Icons.Filled.Edit,
                        contentDescription = "画线工具",
                        tint = if (showDrawingTools) DSColors.goldBright else Color.White,
                        onClick = { showDrawingTools = !showDrawingTools },
                    )
                }
            }

            ComparisonVideoPane(
                player = primaryPlayer,
                title = primaryMemo.displayTitle ?: primaryMemo.title ?: "当前视频",
                label = "当前",
                time = primaryTime,
                duration = primaryDuration,
                onSeek = { seconds ->
                    primaryPlayer.seekTo((seconds * 1000).toLong())
                    primaryTime = seconds
                    if (syncSeek) {
                        val syncedSeconds = seconds.coerceIn(0f, secondaryDuration.coerceAtLeast(0f))
                        secondaryPlayer.seekTo((syncedSeconds * 1000).toLong())
                        secondaryTime = syncedSeconds
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.12f)),
            )
            ComparisonVideoPane(
                player = secondaryPlayer,
                title = secondaryMemo.displayTitle ?: secondaryMemo.title ?: "对比视频",
                label = "对比",
                time = secondaryTime,
                duration = secondaryDuration,
                onSeek = { seconds ->
                    secondaryPlayer.seekTo((seconds * 1000).toLong())
                    secondaryTime = seconds
                    if (syncSeek) {
                        val syncedSeconds = seconds.coerceIn(0f, primaryDuration.coerceAtLeast(0f))
                        primaryPlayer.seekTo((syncedSeconds * 1000).toLong())
                        primaryTime = syncedSeconds
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${formatTime(primaryTime)} / ${formatTime(primaryDuration)}",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(DSColors.fairway)
                        .clickable {
                            if (comparisonEnded) {
                                primaryPlayer.seekTo(0)
                                secondaryPlayer.seekTo(0)
                                primaryTime = 0f
                                secondaryTime = 0f
                            }
                            isPlaying = !isPlaying
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${formatTime(secondaryTime)} / ${formatTime(secondaryDuration)}",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                )
            }
        }

        if (showDrawingTools) {
            V2DrawingOverlay(
                state = comparisonDrawingState,
                modifier = Modifier.fillMaxSize(),
            )
            V2DrawingToolsPanel(
                state = comparisonDrawingState,
                onClose = { showDrawingTools = false },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 82.dp),
            )
        }
    }
}

@Composable
private fun SyncSeekToggle(enabled: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) DSColors.fairway else Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.18f else 0.12f), RoundedCornerShape(50))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (enabled) "同步" else "独立",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ComparisonVideoPane(
    player: ExoPlayer,
    title: String,
    label: String,
    time: Float,
    duration: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                }
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.58f), Color.Transparent),
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = DSColors.goldBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text("${formatTime(time)} / ${formatTime(duration)}", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                    ),
                )
                .padding(horizontal = 16.dp)
                .padding(top = 22.dp, bottom = 8.dp),
        ) {
            Slider(
                value = time.coerceIn(0f, duration.coerceAtLeast(0f)),
                onValueChange = onSeek,
                valueRange = 0f..duration.coerceAtLeast(0.01f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = DSColors.goldBright,
                    inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                ),
            )
        }
    }
}

@Composable
private fun VideoPlaybackLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(34.dp),
                color = Color.White.copy(alpha = 0.72f),
                strokeWidth = 2.dp,
            )
            Text(
                text = "加载中...",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun VideoPlaybackUnavailableOverlay(
    state: V2PlaybackState,
    onBack: () -> Unit,
    onRetry: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.98f)),
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
            Text(
                text = if (state == V2PlaybackState.UnavailableNoSource) "视频不在本机" else "视频暂时无法加载",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (state == V2PlaybackState.UnavailableNoSource) {
                    "你可能正在另一台设备查看这条记录。\n视频源文件只保存在录制它的设备上，跨设备播放暂未支持。\n标题、笔记与标签已同步。"
                } else {
                    "可能是网络问题，或这条视频源已不可用。"
                },
                color = Color(0xFFD1D5DB),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            if (onRetry != null) {
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
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (onRetry == null) DSColors.fairway else Color.Transparent)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("返回", color = Color.White.copy(alpha = if (onRetry == null) 1f else 0.75f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun VideoMemoDeleteConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除备忘录") },
        text = { Text("确定删除这个视频备忘录吗？此操作不可撤销。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = DSColors.flagRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun VideoComparisonPickerDialog(
    candidates: List<V2Memo>,
    onDismiss: () -> Unit,
    onSelect: (V2Memo) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("视频对比") },
        text = {
            if (candidates.isEmpty()) {
                Text("还没有另一条可播放的视频 Memo。先保存至少两条本机视频后，就可以进行上下分屏对比。")
            } else {
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(candidates, key = { it.id }) { memo ->
                        VideoComparisonCandidateRow(memo = memo, onClick = { onSelect(memo) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (candidates.isEmpty()) "知道了" else "取消", color = DSColors.fairway)
            }
        },
    )
}

@Composable
private fun VideoComparisonCandidateRow(memo: V2Memo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DSColors.mistGreen)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DSColors.fairway.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PlayArrow, null, tint = DSColors.fairway, modifier = Modifier.size(24.dp))
        }
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                memo.displayTitle ?: memo.title ?: "无标题视频",
                color = DSColors.inkGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                listOfNotNull(
                    memo.trainingMethod?.takeIf { it.isNotBlank() },
                    memo.metaText?.takeIf { it.isNotBlank() },
                    memo.durationSec?.let { formatTime(it.toFloat()) },
                ).joinToString(" · ").ifBlank { "视频 Memo" },
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TopBar(showToolsPanel: Boolean, onBack: () -> Unit, onToggleDrawing: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(icon = Icons.Filled.Close, contentDescription = "关闭", onClick = onBack)
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(DSColors.inkGreen)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(13.dp))
            Text("视频Memo", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.weight(1f))
        CircleIconButton(
            icon = Icons.Filled.Edit,
            contentDescription = "画线工具",
            tint = if (showToolsPanel) DSColors.goldBright else Color.White,
            onClick = onToggleDrawing,
        )
    }
}

@Composable
private fun RightFloatingTools(
    speedText: String,
    isSlow: Boolean,
    showMoreTools: Boolean,
    onSpeed: () -> Unit,
    onToggleMore: () -> Unit,
    onEdit: () -> Unit,
    onCompare: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 12.dp, bottom = 56.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
    ) {
        FloatingTextButton(primary = speedText, secondary = "速度", highlighted = isSlow, onClick = onSpeed)
        AnimatedVisibility(
            visible = showMoreTools,
            enter = fadeIn() + slideInHorizontally { it / 2 },
            exit = fadeOut() + slideOutHorizontally { it / 2 },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End) {
                FloatingIconButton(icon = Icons.AutoMirrored.Filled.CompareArrows, label = "对比", onClick = onCompare)
                FloatingIconButton(icon = Icons.Filled.Folder, label = "分类", onClick = onEdit)
                FloatingIconButton(icon = Icons.Filled.DeleteOutline, label = "删除", onClick = onDelete)
            }
        }
        FloatingIconButton(
            icon = if (showMoreTools) Icons.Filled.Close else Icons.Filled.MoreHoriz,
            label = if (showMoreTools) "收起" else "更多",
            onClick = onToggleMore,
        )
    }
}

@Composable
private fun BottomInfoAndProgress(
    title: String,
    description: String,
    sourceLabel: String,
    currentTime: Float,
    duration: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                    ),
                )
                .padding(start = 16.dp, end = 86.dp, top = 20.dp, bottom = 6.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(sourceLabel, color = DSColors.goldBright, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                if (description.isNotBlank()) {
                    Text(description, color = Color(0xFF9CA3AF), fontSize = 12.sp, maxLines = 2)
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp)
                .padding(bottom = 6.dp),
        ) {
            Row {
                Text(formatTime(currentTime), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text(formatTime(duration), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Slider(
                value = currentTime.coerceIn(0f, duration.coerceAtLeast(0f)),
                onValueChange = onSeek,
                valueRange = 0f..duration.coerceAtLeast(0.1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF3B82F6),
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
                modifier = Modifier.height(24.dp),
            )
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
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
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun FloatingTextButton(primary: String, secondary: String, highlighted: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(primary, color = if (highlighted) DSColors.goldBright else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(secondary, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FloatingIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

private fun Float.speedText(): String = when {
    this == 1f -> "1x"
    this == 0.5f -> "0.5x"
    this == 0.25f -> "0.25x"
    else -> "${this}x"
}

private fun resolvePlaybackUrl(videoUrl: String?): String? {
    if (videoUrl.isNullOrBlank()) return null
    val trimmed = videoUrl.trim()
    if (V2MemoBridge.isRemoteUrl(trimmed)) {
        return V2OnlineVideoMetadataFetcher.normalizePlayableUrl(trimmed)
    }
    val localPath = V2MemoBridge.localVideoAbsolutePath(trimmed) ?: return null
    return if (File(localPath).exists()) "file://$localPath" else null
}

private fun formatTime(seconds: Float): String {
    val safe = seconds.coerceAtLeast(0f)
    val total = safe.toInt()
    val minutes = total / 60
    val secs = total % 60
    val tenths = ((safe - total) * 10).roundToInt().coerceIn(0, 9)
    return "%d:%02d.%d".format(minutes, secs, tenths)
}
