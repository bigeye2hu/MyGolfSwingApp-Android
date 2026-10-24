package com.mygolfswing.app.ui.memo

import android.content.Context
import android.content.ContentValues
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoAccent
import com.mygolfswing.app.data.model.V2MemoThumbnailKind
import com.mygolfswing.app.data.model.V2Topic
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.video.V2OnlineVideoMetadataFetcher
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.V2DrawingOverlay
import com.mygolfswing.app.ui.common.V2DrawingQuickActions
import com.mygolfswing.app.ui.common.V2DrawingState
import com.mygolfswing.app.ui.common.V2DrawingToolsPanel
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.player.VideoPlayerChrome
import com.mygolfswing.app.ui.player.ReviewFrameScrubber
import com.mygolfswing.app.ui.player.SmoothFrameSeeker
import com.mygolfswing.app.ui.interaction.V2ShareMemoToSpacesScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil

/**
 * 视频 Memo 播放器（对齐 iOS `V2VideoMemoPlayerView`）。
 */
@OptIn(UnstableApi::class)
@Composable
fun V2VideoMemoPlayerScreen(
    memoId: UUID,
    onBack: () -> Unit = {},
    onEdit: (UUID) -> Unit = {},
    onMemoCreated: (UUID) -> Unit = {},
    deleteMemo: suspend (UUID) -> Unit = { V2MemoStore.deleteAndWait(it) },
) {
    V2BackHandler(onBack = onBack)
    val memos by V2MemoStore.allMemos.collectAsState()
    val memo = memos.firstOrNull { it.id == memoId }
    val context = LocalContext.current
    var shouldPlay by rememberSaveable(memoId) { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(shouldPlay) }
    var showPlayPauseIcon by remember { mutableStateOf(false) }
    var showToolsPanel by rememberSaveable(memoId) { mutableStateOf(false) }
    var showMoreTools by rememberSaveable(memoId) { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable(memoId) { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var playerNotice by remember { mutableStateOf<String?>(null) }
    var showComparison by rememberSaveable(memoId) { mutableStateOf(false) }
    var showShareToSpaces by rememberSaveable(memoId) { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf(false) }
    var currentTime by rememberSaveable(memoId) { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var currentRateIndex by rememberSaveable(memoId) { mutableIntStateOf(0) }
    val playbackRates = remember { listOf(1f, 0.5f, 0.25f) }
    val drawingState = remember { V2DrawingState() }
    var wasPlayingBeforeTimelineScrub by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (memo == null) {
        V2MissingEntityScreen(message = "视频不存在", onBack = onBack, title = "视频 Memo")
        return
    }

    val comparisonCandidates = remember(memos, memo.id) {
        memos
            .filter { it.id != memo.id && it.hasVideo }
            .sortedByDescending { it.createdAt }
    }
    if (showComparison) {
        VideoMemoComparisonEntryScreen(
            primaryMemo = memo,
            candidates = comparisonCandidates,
            onClose = { showComparison = false },
        )
        return
    }

    val playbackUrl = remember(memo.videoUrl) { resolvePlaybackUrl(memo.videoUrl) }
    var isLoading by remember(playbackUrl) { mutableStateOf(true) }
    var playbackStarted by remember(playbackUrl) { mutableStateOf(false) }
    if (playbackUrl == null) {
        VideoPlaybackUnavailableOverlay(
            state = V2PlaybackState.UnavailableNoSource,
            onBack = onBack,
            onRetry = null,
        )
        return
    }

    if (showShareToSpaces) {
        V2ShareMemoToSpacesScreen(memo = memo, onBack = { showShareToSpaces = false })
        return
    }

    val player = remember(playbackUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(playbackUrl))
            playWhenReady = shouldPlay
            playbackParameters = PlaybackParameters(playbackRates[currentRateIndex])
            if (currentTime > 0f) {
                seekTo((currentTime * 1000).toLong())
            }
            prepare()
        }
    }
    val frameSeeker = remember(player) { SmoothFrameSeeker(player) }
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
                        if (!playbackStarted) isLoading = true
                    }

                    Player.STATE_READY -> {
                        playbackStarted = true
                        isLoading = false
                    }

                    Player.STATE_ENDED -> {
                        isLoading = false
                        player.pause()
                        player.seekTo(0)
                        player.playbackParameters = PlaybackParameters(playbackRates[currentRateIndex])
                        currentTime = 0f
                        shouldPlay = false
                        isPlaying = false
                        showPlayPauseIcon = true
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose {
            frameSeeker.dispose()
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(player) {
        while (true) {
            if (!frameSeeker.isScrubbing) {
                currentTime = (player.currentPosition / 1000f).coerceAtLeast(0f)
                isPlaying = player.isPlaying
            }
            val durMs = player.duration
            if (durMs > 0) duration = durMs / 1000f
            delay(100)
        }
    }
    LaunchedEffect(playbackError) {
        if (playbackError) {
            showToolsPanel = false
        }
    }

    fun showCenterIcon() {
        showPlayPauseIcon = true
    }

    fun togglePlayback() {
        if (player.isPlaying) {
            player.pause()
            shouldPlay = false
            isPlaying = false
        } else {
            player.playbackParameters = PlaybackParameters(playbackRates[currentRateIndex])
            player.play()
            shouldPlay = true
            isPlaying = true
        }
        showCenterIcon()
    }

    fun stepPlayback(deltaMs: Long) {
        val durationMs = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        val target = (player.currentPosition + deltaMs).coerceIn(0L, durationMs)
        player.seekTo(target)
        currentTime = target / 1000f
    }

    fun showPlayerNotice(text: String) {
        scope.launch {
            playerNotice = text
            delay(1600)
            if (playerNotice == text) {
                playerNotice = null
            }
        }
    }

    fun startTimelineScrub() {
        wasPlayingBeforeTimelineScrub = player.isPlaying
        if (player.isPlaying) {
            player.pause()
            isPlaying = false
        }
    }

    fun finishTimelineScrub() {
        if (wasPlayingBeforeTimelineScrub && !playbackError) {
            player.playbackParameters = PlaybackParameters(playbackRates[currentRateIndex])
            player.play()
            isPlaying = true
        }
        wasPlayingBeforeTimelineScrub = false
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

        if (isLoading && !playbackError && !frameSeeker.isScrubbing) {
            VideoPlaybackLoadingOverlay()
        }

        if (playbackError) {
            VideoPlaybackUnavailableOverlay(
                state = V2PlaybackState.UnavailablePlaybackError,
                onBack = onBack,
                onRetry = {
                    playbackError = false
                    playbackStarted = false
                    isLoading = true
                    player.setMediaItem(MediaItem.fromUri(playbackUrl))
                    player.prepare()
                    player.playbackParameters = PlaybackParameters(playbackRates[currentRateIndex])
                    player.play()
                    shouldPlay = true
                    isPlaying = true
                },
            )
        }

        VideoMemoCentralTapArea(
            enabled = !isLoading && !playbackError && !showToolsPanel,
            onTap = {
                if (showMoreTools) {
                    showMoreTools = false
                } else {
                    togglePlayback()
                }
            },
        )

        V2DrawingOverlay(
            state = drawingState,
            modifier = Modifier.fillMaxSize(),
            interactionEnabled = showToolsPanel,
        )
        if (drawingState.strokes.isNotEmpty()) {
            V2DrawingQuickActions(
                state = drawingState,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .v2StatusBarPadding()
                    .padding(top = 96.dp, start = 14.dp),
            )
        }

        TopBar(
            showToolsPanel = showToolsPanel,
            drawingEnabled = !playbackError,
            onBack = onBack,
            onToggleDrawing = { showToolsPanel = !showToolsPanel },
        )

        if (showToolsPanel) {
            V2DrawingToolsPanel(
                state = drawingState,
                onClose = { showToolsPanel = false },
                showSelectTool = false,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .v2StatusBarPadding()
                    .padding(top = 62.dp, end = 12.dp),
            )
        }

        if (!playbackError) BottomInfoAndProgress(
            title = memo.displayTitle ?: memo.title ?: "无标题",
            description = memo.text.orEmpty(),
            sourceLabel = memo.trainingMethod?.takeIf { it.isNotBlank() } ?: "视频备忘录",
            currentTime = currentTime,
            duration = duration,
            isPlaying = isPlaying,
            speedText = playbackRates[currentRateIndex].speedText(),
            isSlow = currentRateIndex != 0,
            showMoreTools = showMoreTools,
            controlsLocked = showToolsPanel,
            onSeek = { seconds ->
                frameSeeker.moveTo((seconds * 1000f).toLong())
                currentTime = seconds
            },
            onTimelineScrubStart = {
                startTimelineScrub()
                frameSeeker.begin(33_000L)
            },
            onTimelineScrubEnd = {
                frameSeeker.end()
                finishTimelineScrub()
            },
            onStepBackward = { stepPlayback(-5_000L) },
            onStepForward = { stepPlayback(5_000L) },
            onTogglePlay = { togglePlayback() },
            onSpeed = {
                currentRateIndex = (currentRateIndex + 1) % playbackRates.size
                player.playbackParameters = PlaybackParameters(playbackRates[currentRateIndex])
                if (player.isPlaying) player.play()
                val rateText = playbackRates[currentRateIndex].speedText()
                showPlayerNotice(if (currentRateIndex == 0) "已恢复正常速度 1x" else "慢放已开启 · $rateText")
            },
            onToggleMore = { showMoreTools = !showMoreTools },
            onShare = {
                showMoreTools = false
                if (!shareVideoMemoInfo(context, memo, playbackUrl)) {
                    showPlayerNotice("分享面板暂时无法打开，请稍后重试")
                }
            },
            onShareToSpaces = {
                showMoreTools = false
                player.pause()
                isPlaying = false
                scope.launch {
                    if (ServiceLocator.session.current().isLoggedIn) showShareToSpaces = true
                    else showPlayerNotice("登录后即可分享到挥呗空间")
                }
            },
            onExport = {
                showMoreTools = false
                scope.launch {
                    showPlayerNotice("正在导出视频...")
                    val message = saveVideoMemoToGallery(context, memo, playbackUrl)
                    showPlayerNotice(message)
                }
            },
            onEdit = {
                showMoreTools = false
                player.pause()
                isPlaying = false
                onEdit(memo.id)
            },
            onCompare = {
                showMoreTools = false
                player.pause()
                isPlaying = false
                showComparison = true
            },
            onDelete = {
                showMoreTools = false
                showDeleteConfirm = true
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .v2NavigationBarPadding(),
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

        playerNotice?.let { notice ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .v2StatusBarPadding()
                    .padding(top = 18.dp, start = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    notice,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showDeleteConfirm) {
        VideoMemoDeleteConfirmDialog(
            deleting = deleting,
            onDismiss = { if (!deleting) showDeleteConfirm = false },
            onConfirm = {
                if (deleting) return@VideoMemoDeleteConfirmDialog
                scope.launch {
                    deleting = true
                    deleteError = null
                    try {
                        deleteMemo(memo.id)
                        showDeleteConfirm = false
                        onBack()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        showDeleteConfirm = false
                        deleteError = "删除失败，请稍后重试"
                    } finally {
                        deleting = false
                    }
                }
            },
        )
    }

    deleteError?.let { message ->
        VideoMemoDeleteFailureDialog(
            message = message,
            onDismiss = { deleteError = null },
        )
    }

}

@Composable
internal fun VideoMemoCentralTapArea(
    enabled: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val centralHeight = (maxHeight - 300.dp).coerceAtLeast(1.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(centralHeight)
                .offset(y = 86.dp)
                .testTag("video-memo-central-tap-area")
                .then(if (enabled) Modifier.clickable(onClick = onTap) else Modifier),
        )
    }
}

internal enum class V2PlaybackState {
    UnavailableNoSource,
    UnavailablePlaybackError,
}

private enum class TimelineScrubFeel(
    val title: String,
    val pixelsPerSecond: Float,
) {
    Balanced("稳", 180f),
    Precise("细", 280f),
    Frame("帧", 420f),
}

private val TimelineScrubFeelSaver = Saver<TimelineScrubFeel, String>(
    save = { it.name },
    restore = { saved -> TimelineScrubFeel.entries.firstOrNull { it.name == saved } ?: TimelineScrubFeel.Precise },
)

private const val VIDEO_MEMO_FLING_TARGET_FACTOR = 0.22f
private const val VIDEO_MEMO_MIN_FLING_VELOCITY_PX = 35f

internal fun videoMemoScrubTimeAfterDelta(
    currentTime: Float,
    contentDeltaPx: Float,
    pixelsPerSecondPx: Float,
    duration: Float,
): Float {
    val safeDuration = duration.takeIf { it.isFinite() && it > 0f } ?: return 0f
    val safeCurrent = currentTime.takeIf { it.isFinite() }?.coerceIn(0f, safeDuration) ?: return 0f
    if (!contentDeltaPx.isFinite() || !pixelsPerSecondPx.isFinite() || pixelsPerSecondPx <= 0f) {
        return safeCurrent
    }
    return (safeCurrent + contentDeltaPx / pixelsPerSecondPx).coerceIn(0f, safeDuration)
}

internal fun videoMemoTimelineBarHeightRatio(index: Int): Float = when (Math.floorMod(index, 4)) {
    0 -> 1f
    1, 3 -> 14f / 34f
    else -> 24f / 34f
}

private fun positiveTimelineRemainder(value: Float, modulus: Float): Float {
    if (!value.isFinite() || !modulus.isFinite() || modulus <= 0f) return 0f
    return ((value % modulus) + modulus) % modulus
}

@OptIn(UnstableApi::class)
@Composable
internal fun VideoMemoComparisonEntryScreen(
    primaryMemo: V2Memo,
    candidates: List<V2Memo>,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val primaryUrl = remember(primaryMemo.videoUrl) { resolvePlaybackUrl(primaryMemo.videoUrl) }
    var selectedMemoId by rememberSaveable(primaryMemo.id) { mutableStateOf<String?>(null) }
    val selectedMemo = remember(candidates, selectedMemoId) {
        candidates.firstOrNull { it.id.toString() == selectedMemoId }
    }
    var showPicker by rememberSaveable(primaryMemo.id) { mutableStateOf(false) }
    var isPlaying by rememberSaveable(primaryMemo.id) { mutableStateOf(false) }
    var isMuted by rememberSaveable(primaryMemo.id) { mutableStateOf(false) }
    var showDrawingTools by rememberSaveable(primaryMemo.id) { mutableStateOf(false) }
    var currentTime by rememberSaveable(primaryMemo.id) { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var notice by remember { mutableStateOf<String?>(null) }
    val drawingState = remember { V2DrawingState() }

    selectedMemo?.let { selected ->
        IosVideoMemoComparisonScreen(
            primaryMemo = primaryMemo,
            secondaryMemo = selected,
            onClose = onClose,
            onReselectComparison = {
                selectedMemoId = null
                showPicker = true
            },
        )
        return
    }

    if (primaryUrl == null) {
        VideoPlaybackUnavailableOverlay(
            state = V2PlaybackState.UnavailableNoSource,
            onBack = onClose,
            onRetry = null,
        )
        return
    }

    val player = remember(primaryUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(primaryUrl))
            playWhenReady = isPlaying
            volume = if (isMuted) 0f else 1f
            if (currentTime > 0f) seekTo((currentTime * 1000).toLong())
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    LaunchedEffect(isPlaying, player) {
        if (isPlaying) player.play() else player.pause()
    }
    LaunchedEffect(isMuted, player) {
        player.volume = if (isMuted) 0f else 1f
    }
    LaunchedEffect(player) {
        while (true) {
            currentTime = (player.currentPosition / 1000f).coerceAtLeast(0f)
            player.duration.takeIf { it > 0 }?.let { duration = it / 1000f }
            delay(120)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        useController = false
                        this.player = player
                    }
                },
                update = { it.player = player },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { isPlaying = !isPlaying }
                    .testTag("video-comparison-primary"),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(1.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 3.dp,
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFFAF52DE), Color(0xFF0A84FF)),
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { showPicker = true }
                    .testTag("video-comparison-empty-selection"),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Filled.VideoLibrary,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        "点击选择对比视频",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            ComparisonWaitingControlBar(
                currentTime = currentTime,
                duration = duration,
                onSeek = { seconds ->
                    player.seekTo((seconds * 1000).toLong())
                    currentTime = seconds
                },
            )
        }

        ComparisonOverlayButton(
            icon = Icons.Filled.Close,
            contentDescription = "返回",
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .v2StatusBarPadding()
                .padding(start = 16.dp, top = 16.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .v2StatusBarPadding()
                .padding(end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComparisonOverlayButton(
                icon = Icons.Filled.Share,
                contentDescription = "分享对比视频",
                onClick = {
                    player.pause()
                    isPlaying = false
                    if (!shareVideoMemoInfo(context, primaryMemo, primaryUrl)) {
                        notice = "无法打开分享面板"
                    }
                },
            )
            ComparisonOverlayButton(
                icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isMuted) "打开声音" else "静音",
                onClick = { isMuted = !isMuted },
            )
            ComparisonOverlayButton(
                icon = Icons.Filled.Edit,
                contentDescription = if (showDrawingTools) "关闭绘图工具" else "打开绘图工具",
                onClick = { showDrawingTools = !showDrawingTools },
            )
        }

        if (showDrawingTools) {
            V2DrawingOverlay(
                state = drawingState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp, end = 92.dp, bottom = 180.dp),
            )
            V2DrawingToolsPanel(
                state = drawingState,
                onClose = { showDrawingTools = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 150.dp),
            )
        }

        notice?.let { message ->
            Text(
                message,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .v2StatusBarPadding()
                    .padding(top = 56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.76f))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }

    if (showPicker) {
        VideoComparisonPickerDialog(
            candidates = candidates,
            onDismiss = { showPicker = false },
            onSelect = { selected ->
                showPicker = false
                selectedMemoId = selected.id.toString()
            },
        )
    }
}

@Composable
private fun ComparisonWaitingControlBar(
    currentTime: Float,
    duration: Float,
    onSeek: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.8f))
            .v2NavigationBarPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                formatComparisonTime(currentTime),
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color(0xFFAF52DE), Color(0xFF0A84FF)),
                        ),
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            ScrubFeelProgressBar(
                currentTime = currentTime,
                duration = duration.coerceAtLeast(1f),
                scrubFeel = TimelineScrubFeel.Precise,
                accent = Color(0xFF0A84FF),
                onSeek = onSeek,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("同步", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6B5CCF).copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.CompareArrows, null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(14.dp))
            }
        }
        Text(
            "先选择对比视频",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun ComparisonOverlayButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun IosVideoMemoComparisonScreen(
    primaryMemo: V2Memo,
    secondaryMemo: V2Memo,
    onClose: () -> Unit,
    onReselectComparison: () -> Unit,
) {
    val context = LocalContext.current
    val primaryUrl = remember(primaryMemo.videoUrl) { resolvePlaybackUrl(primaryMemo.videoUrl) }
    val secondaryUrl = remember(secondaryMemo.videoUrl) { resolvePlaybackUrl(secondaryMemo.videoUrl) }

    if (primaryUrl == null) {
        VideoPlaybackUnavailableOverlay(
            state = V2PlaybackState.UnavailableNoSource,
            onBack = onClose,
            onRetry = null,
        )
        return
    }

    var topIsPlaying by rememberSaveable(primaryMemo.id, secondaryMemo.id) { mutableStateOf(false) }
    var primaryTime by rememberSaveable(primaryMemo.id, secondaryMemo.id) { mutableFloatStateOf(0f) }
    var secondaryTime by rememberSaveable(primaryMemo.id, secondaryMemo.id) { mutableFloatStateOf(0f) }
    var primaryDuration by remember(primaryMemo.id) {
        mutableFloatStateOf(primaryMemo.durationSec?.toFloat()?.coerceAtLeast(0f) ?: 0f)
    }
    var secondaryDuration by remember(secondaryMemo.id) {
        mutableFloatStateOf(secondaryMemo.durationSec?.toFloat()?.coerceAtLeast(0f) ?: 0f)
    }
    var syncSeek by rememberSaveable(primaryMemo.id, secondaryMemo.id) { mutableStateOf(false) }
    var showDrawingTools by rememberSaveable(primaryMemo.id, secondaryMemo.id) { mutableStateOf(false) }
    var selectedScrubFeel by rememberSaveable(
        primaryMemo.id,
        secondaryMemo.id,
        stateSaver = TimelineScrubFeelSaver,
    ) { mutableStateOf(TimelineScrubFeel.Precise) }
    var isMuted by rememberSaveable(primaryMemo.id, secondaryMemo.id) { mutableStateOf(false) }
    var comparisonNotice by remember { mutableStateOf<String?>(null) }
    var primaryPlaybackError by remember { mutableStateOf<String?>(null) }
    var secondaryPlaybackError by remember(secondaryMemo.id) {
        mutableStateOf(if (secondaryUrl == null) "所选对比视频文件不存在或无法读取。" else null)
    }
    val drawingState = remember { V2DrawingState() }
    val scope = rememberCoroutineScope()

    val primaryPlayer = remember(primaryUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(primaryUrl))
            playWhenReady = topIsPlaying
            volume = if (isMuted) 0f else 1f
            if (primaryTime > 0f) seekTo((primaryTime * 1000).toLong())
            prepare()
        }
    }
    val secondaryPlayer = remember(secondaryUrl) {
        secondaryUrl?.let { url ->
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                volume = if (isMuted) 0f else 1f
                if (secondaryTime > 0f) seekTo((secondaryTime * 1000).toLong())
                prepare()
            }
        }
    }

    fun showNotice(text: String) {
        scope.launch {
            comparisonNotice = text
            delay(1600)
            if (comparisonNotice == text) comparisonNotice = null
        }
    }
    fun seekBoth(seconds: Float) {
        val primaryTarget = seconds.coerceIn(0f, primaryDuration.coerceAtLeast(0f))
        val secondaryTarget = seconds.coerceIn(0f, secondaryDuration.coerceAtLeast(0f))
        primaryPlayer.seekTo((primaryTarget * 1000).toLong())
        secondaryPlayer?.seekTo((secondaryTarget * 1000).toLong())
        primaryTime = primaryTarget
        secondaryTime = secondaryTarget
    }

    DisposableEffect(primaryPlayer, secondaryPlayer) {
        val primaryListener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                primaryPlaybackError = "原视频文件无法读取。"
                topIsPlaying = false
            }
        }
        val secondaryListener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                secondaryPlaybackError = "所选对比视频文件无法读取。"
                syncSeek = false
            }
        }
        primaryPlayer.addListener(primaryListener)
        secondaryPlayer?.addListener(secondaryListener)
        onDispose {
            primaryPlayer.removeListener(primaryListener)
            secondaryPlayer?.removeListener(secondaryListener)
            primaryPlayer.release()
            secondaryPlayer?.release()
        }
    }
    LaunchedEffect(topIsPlaying, primaryPlayer) {
        if (topIsPlaying) primaryPlayer.play() else primaryPlayer.pause()
    }
    LaunchedEffect(isMuted, primaryPlayer, secondaryPlayer) {
        val volume = if (isMuted) 0f else 1f
        primaryPlayer.volume = volume
        secondaryPlayer?.volume = volume
    }
    LaunchedEffect(primaryPlayer, secondaryPlayer) {
        while (true) {
            primaryTime = (primaryPlayer.currentPosition / 1000f).coerceAtLeast(0f)
            primaryPlayer.duration.takeIf { it > 0 }?.let { primaryDuration = it / 1000f }
            secondaryPlayer?.let { player ->
                secondaryTime = (player.currentPosition / 1000f).coerceAtLeast(0f)
                player.duration.takeIf { it > 0 }?.let {
                    secondaryDuration = it / 1000f
                    secondaryPlaybackError = null
                }
            }
            delay(120)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize()) {
            if (primaryPlaybackError == null) {
                IosComparisonPlayerSurface(
                    player = primaryPlayer,
                    onTap = { topIsPlaying = !topIsPlaying },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            } else {
                IosComparisonUnavailablePanel(
                    title = "源视频无法打开",
                    message = primaryPlaybackError.orEmpty(),
                    actionTitle = "返回",
                    onAction = onClose,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
            if (secondaryPlayer != null && secondaryPlaybackError == null) {
                IosComparisonPlayerSurface(
                    player = secondaryPlayer,
                    onTap = {},
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            } else {
                IosComparisonUnavailablePanel(
                    title = "对比视频无法打开",
                    message = secondaryPlaybackError ?: "所选对比视频文件无法读取。",
                    actionTitle = "重新选择",
                    onAction = onReselectComparison,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
            IosActiveComparisonControlBar(
                primaryTime = primaryTime,
                primaryDuration = primaryDuration,
                secondaryTime = secondaryTime,
                secondaryDuration = secondaryDuration,
                scrubFeel = selectedScrubFeel,
                synced = syncSeek,
                canSync = secondaryPlayer != null && primaryDuration > 0f && secondaryDuration > 0f,
                onToggleSync = {
                    if (secondaryPlayer == null || primaryDuration <= 0f || secondaryDuration <= 0f) {
                        showNotice("请先选择可播放的对比视频")
                    } else {
                        syncSeek = !syncSeek
                    }
                },
                onSeekPrimary = { seconds ->
                    if (syncSeek) seekBoth(seconds) else {
                        primaryPlayer.seekTo((seconds * 1000).toLong())
                        primaryTime = seconds
                    }
                },
                onSeekSecondary = { seconds ->
                    if (syncSeek) seekBoth(seconds) else {
                        secondaryPlayer?.seekTo((seconds * 1000).toLong())
                        secondaryTime = seconds
                    }
                },
                onSeekBoth = ::seekBoth,
            )
        }

        ComparisonOverlayButton(
            icon = Icons.Filled.Close,
            contentDescription = "返回",
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .v2StatusBarPadding()
                .padding(start = 16.dp, top = 16.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .v2StatusBarPadding()
                .padding(end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComparisonOverlayButton(
                icon = Icons.Filled.Share,
                contentDescription = "分享对比视频",
                onClick = {
                    primaryPlayer.pause()
                    secondaryPlayer?.pause()
                    topIsPlaying = false
                    val shared = if (secondaryUrl != null) {
                        shareVideoMemoComparison(
                            context = context,
                            primaryMemo = primaryMemo,
                            primaryPlaybackUrl = primaryUrl,
                            secondaryMemo = secondaryMemo,
                            secondaryPlaybackUrl = secondaryUrl,
                        )
                    } else {
                        shareVideoMemoInfo(context, primaryMemo, primaryUrl)
                    }
                    if (!shared) showNotice("无法打开分享面板")
                },
            )
            ComparisonOverlayButton(
                icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isMuted) "打开声音" else "静音",
                onClick = { isMuted = !isMuted },
            )
            ComparisonOverlayButton(
                icon = Icons.Filled.Edit,
                contentDescription = if (showDrawingTools) "关闭绘图工具" else "打开绘图工具",
                onClick = { showDrawingTools = !showDrawingTools },
            )
        }
        if (showDrawingTools) {
            V2DrawingOverlay(
                state = drawingState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp, end = 92.dp, bottom = 180.dp),
            )
            V2DrawingToolsPanel(
                state = drawingState,
                onClose = { showDrawingTools = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 150.dp),
            )
        }

        comparisonNotice?.let { message ->
            Text(
                message,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .v2StatusBarPadding()
                    .padding(top = 56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.76f))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun IosComparisonPlayerSurface(
    player: ExoPlayer,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Color.Black).clickable(onClick = onTap)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    this.player = player
                }
            },
            update = { it.player = player },
        )
    }
}

@Composable
private fun IosComparisonUnavailablePanel(
    title: String,
    message: String,
    actionTitle: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.VideoLibrary,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(34.dp),
            )
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                message,
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 28.dp),
            )
            Text(
                actionTitle,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun IosActiveComparisonControlBar(
    primaryTime: Float,
    primaryDuration: Float,
    secondaryTime: Float,
    secondaryDuration: Float,
    scrubFeel: TimelineScrubFeel,
    synced: Boolean,
    canSync: Boolean,
    onToggleSync: () -> Unit,
    onSeekPrimary: (Float) -> Unit,
    onSeekSecondary: (Float) -> Unit,
    onSeekBoth: (Float) -> Unit,
) {
    if (synced) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.8f))
                .v2NavigationBarPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                IosComparisonTimeChip("上方视频: ${formatComparisonTime(primaryTime)}")
                Spacer(Modifier.weight(1f))
                IosComparisonTimeChip("下方视频: ${formatComparisonTime(secondaryTime)}")
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IosComparisonSyncButton(
                    linked = true,
                    enabled = canSync,
                    label = "解锁",
                    contentDescription = "取消同步拖动条",
                    onClick = onToggleSync,
                )
                ScrubFeelProgressBar(
                    currentTime = primaryTime,
                    duration = maxOf(primaryDuration, secondaryDuration).coerceAtLeast(1f),
                    scrubFeel = scrubFeel,
                    accent = Color(0xFF0A84FF),
                    onSeek = onSeekBoth,
                    modifier = Modifier.weight(1f).padding(bottom = 6.dp),
                )
                Spacer(Modifier.width(52.dp))
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.8f))
                .v2NavigationBarPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IosComparisonTimelineColumn(
                time = primaryTime,
                duration = primaryDuration,
                scrubFeel = scrubFeel,
                onSeek = onSeekPrimary,
                modifier = Modifier.weight(1f),
            )
            IosComparisonSyncButton(
                linked = false,
                enabled = canSync,
                label = "同步",
                contentDescription = "同步上下视频拖动条",
                onClick = onToggleSync,
            )
            IosComparisonTimelineColumn(
                time = secondaryTime,
                duration = secondaryDuration,
                scrubFeel = scrubFeel,
                onSeek = onSeekSecondary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun IosComparisonTimelineColumn(
    time: Float,
    duration: Float,
    scrubFeel: TimelineScrubFeel,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IosComparisonTimeChip(formatComparisonTime(time))
        ScrubFeelProgressBar(
            currentTime = time,
            duration = duration.coerceAtLeast(1f),
            scrubFeel = scrubFeel,
            accent = Color(0xFF0A84FF),
            onSeek = onSeek,
        )
    }
}

@Composable
private fun IosComparisonTimeChip(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(Color(0xFFAF52DE), Color(0xFF0A84FF)),
                ),
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun IosComparisonSyncButton(
    linked: Boolean,
    enabled: Boolean,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Color.White.copy(alpha = if (enabled) 0.8f else 0.35f), fontSize = 10.sp)
        Box(
            modifier = Modifier
                .size(if (linked) 36.dp else 32.dp)
                .clip(CircleShape)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(Color(0xFFAF52DE), Color(0xFF0A84FF)),
                    ),
                    alpha = if (enabled) 1f else 0.35f,
                )
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.CompareArrows,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoMemoComparisonScreen(
    primaryMemo: V2Memo,
    secondaryMemo: V2Memo,
    candidates: List<V2Memo>,
    onClose: () -> Unit,
    onReplaceComparison: (V2Memo) -> Unit,
    onMemoCreated: (UUID) -> Unit,
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
    var isOverlayMode by remember { mutableStateOf(false) }
    var showReplacePicker by remember { mutableStateOf(false) }
    var savingComparisonMemo by remember { mutableStateOf(false) }
    var selectedScrubFeel by remember { mutableStateOf(TimelineScrubFeel.Precise) }
    var isMuted by remember { mutableStateOf(false) }
    var comparisonNotice by remember { mutableStateOf<String?>(null) }
    val comparisonDrawingState = remember { V2DrawingState() }
    val scope = rememberCoroutineScope()
    val replaceCandidates = remember(candidates, secondaryMemo.id) {
        candidates.filter { it.id != secondaryMemo.id }
    }

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
    fun seekBothVideos(seconds: Float) {
        val primaryTarget = seconds.coerceIn(0f, primaryDuration.coerceAtLeast(0f))
        val secondaryTarget = seconds.coerceIn(0f, secondaryDuration.coerceAtLeast(0f))
        primaryPlayer.seekTo((primaryTarget * 1000).toLong())
        secondaryPlayer.seekTo((secondaryTarget * 1000).toLong())
        primaryTime = primaryTarget
        secondaryTime = secondaryTarget
    }
    fun showComparisonNotice(text: String) {
        scope.launch {
            comparisonNotice = text
            delay(1600)
            if (comparisonNotice == text) {
                comparisonNotice = null
            }
        }
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
    LaunchedEffect(isMuted, primaryPlayer, secondaryPlayer) {
        val targetVolume = if (isMuted) 0f else 1f
        primaryPlayer.volume = targetVolume
        secondaryPlayer.volume = targetVolume
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
    LaunchedEffect(secondaryMemo.id) {
        secondaryTime = 0f
        secondaryDuration = 0f
        comparisonEnded = false
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
                    CircleIconButton(
                        icon = Icons.Filled.Share,
                        contentDescription = "分享对比视频",
                        onClick = {
                            primaryPlayer.pause()
                            secondaryPlayer.pause()
                            isPlaying = false
                            if (!shareVideoMemoComparison(
                                    context = context,
                                    primaryMemo = primaryMemo,
                                    primaryPlaybackUrl = primaryUrl,
                                    secondaryMemo = secondaryMemo,
                                    secondaryPlaybackUrl = secondaryUrl,
                                )
                            ) {
                                showComparisonNotice("分享面板暂时无法打开，请稍后重试")
                            }
                        },
                    )
                    CircleIconButton(
                        icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "打开声音" else "静音",
                        tint = if (isMuted) DSColors.goldBright else Color.White,
                        onClick = { isMuted = !isMuted },
                    )
                    CircleIconButton(
                        icon = Icons.Filled.Save,
                        contentDescription = "保存对比复盘 Memo",
                        tint = if (savingComparisonMemo) Color.White.copy(alpha = 0.38f) else Color.White,
                        onClick = {
                            if (savingComparisonMemo) return@CircleIconButton
                            scope.launch {
                                savingComparisonMemo = true
                                try {
                                    val memoId = V2MemoStore.createAndWait(
                                        buildComparisonReviewMemo(
                                            primaryMemo = primaryMemo,
                                            secondaryMemo = secondaryMemo,
                                            primaryTime = primaryTime,
                                            primaryDuration = primaryDuration,
                                            secondaryTime = secondaryTime,
                                            secondaryDuration = secondaryDuration,
                                            isOverlayMode = isOverlayMode,
                                            syncSeek = syncSeek,
                                            scrubFeel = selectedScrubFeel,
                                        ),
                                    )
                                    showComparisonNotice("已保存对比复盘 Memo")
                                    delay(450)
                                    onMemoCreated(memoId)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Throwable) {
                                    showComparisonNotice("保存复盘 Memo 失败，请稍后重试")
                                } finally {
                                    savingComparisonMemo = false
                                }
                            }
                        },
                    )
                    CircleIconButton(
                        icon = Icons.Filled.PhotoCamera,
                        contentDescription = "保存当前帧截图",
                        onClick = {
                            scope.launch {
                                val message = saveVideoFrameToGallery(
                                    context = context,
                                    playbackUrl = primaryUrl,
                                    timeSec = primaryTime,
                                    displayName = "memo_compare_${primaryMemo.id}_${System.currentTimeMillis()}.png",
                                )
                                showComparisonNotice(message)
                            }
                        },
                    )
                    SyncSeekToggle(enabled = syncSeek, onToggle = { syncSeek = !syncSeek })
                    CircleIconButton(
                        icon = Icons.Filled.VideoLibrary,
                        contentDescription = "更换对比视频",
                        onClick = {
                            primaryPlayer.pause()
                            secondaryPlayer.pause()
                            isPlaying = false
                            showReplacePicker = true
                        },
                    )
                    CircleIconButton(
                        icon = Icons.Filled.Edit,
                        contentDescription = "画线工具",
                        tint = if (showDrawingTools) DSColors.goldBright else Color.White,
                        onClick = { showDrawingTools = !showDrawingTools },
                    )
                }
            }

            if (isOverlayMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    ComparisonVideoPane(
                        player = primaryPlayer,
                        title = primaryMemo.displayTitle ?: primaryMemo.title ?: "当前视频",
                        label = "当前",
                        time = primaryTime,
                        duration = primaryDuration,
                        scrubFeel = selectedScrubFeel,
                        showControls = false,
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
                            .fillMaxSize()
                            .alpha(0.58f),
                    )
                    ComparisonVideoPane(
                        player = secondaryPlayer,
                        title = secondaryMemo.displayTitle ?: secondaryMemo.title ?: "对比视频",
                        label = "对比",
                        time = secondaryTime,
                        duration = secondaryDuration,
                        scrubFeel = selectedScrubFeel,
                        showControls = !syncSeek,
                        onSeek = { seconds ->
                            secondaryPlayer.seekTo((seconds * 1000).toLong())
                            secondaryTime = seconds
                            if (syncSeek) {
                                val syncedSeconds = seconds.coerceIn(0f, primaryDuration.coerceAtLeast(0f))
                                primaryPlayer.seekTo((syncedSeconds * 1000).toLong())
                                primaryTime = syncedSeconds
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                ComparisonVideoPane(
                    player = primaryPlayer,
                    title = primaryMemo.displayTitle ?: primaryMemo.title ?: "当前视频",
                    label = "当前",
                    time = primaryTime,
                    duration = primaryDuration,
                    scrubFeel = selectedScrubFeel,
                    showControls = !syncSeek,
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
                    scrubFeel = selectedScrubFeel,
                    showControls = !syncSeek,
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
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                if (syncSeek) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "上方 ${formatComparisonTime(primaryTime)}",
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "同步拖动条",
                            color = DSColors.goldBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "下方 ${formatComparisonTime(secondaryTime)}",
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 11.sp,
                        )
                    }
                    ScrubFeelProgressBar(
                        currentTime = minOf(primaryTime, secondaryTime),
                        duration = maxOf(primaryDuration, secondaryDuration).coerceAtLeast(1f),
                        scrubFeel = selectedScrubFeel,
                        accent = DSColors.goldBright,
                        onSeek = ::seekBothVideos,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${formatComparisonTime(primaryTime)} / ${formatComparisonTime(primaryDuration)}",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    VSToggleButton(enabled = isOverlayMode, onToggle = { isOverlayMode = !isOverlayMode })
                    Spacer(Modifier.width(10.dp))
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
                        "${formatComparisonTime(secondaryTime)} / ${formatComparisonTime(secondaryDuration)}",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp,
                    )
                }
            }
        }

        if (showDrawingTools) {
            V2DrawingOverlay(
                state = comparisonDrawingState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 84.dp, end = 92.dp, bottom = 150.dp),
            )
            V2DrawingToolsPanel(
                state = comparisonDrawingState,
                onClose = { showDrawingTools = false },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 82.dp),
            )
        }

        comparisonNotice?.let { notice ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .v2StatusBarPadding()
                    .padding(top = 58.dp, start = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    notice,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showReplacePicker) {
        VideoComparisonPickerDialog(
            candidates = replaceCandidates,
            onDismiss = { showReplacePicker = false },
            onSelect = { selected ->
                showReplacePicker = false
                onReplaceComparison(selected)
            },
        )
    }
}

@Composable
private fun VSToggleButton(enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) Color.White else Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "VS",
            color = if (enabled) Color.Black.copy(alpha = 0.86f) else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
        )
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
    scrubFeel: TimelineScrubFeel,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    showControls: Boolean = true,
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
            Text("${formatComparisonTime(time)} / ${formatComparisonTime(duration)}", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
        }
        if (showControls) {
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
                ScrubFeelProgressBar(
                    currentTime = time,
                    duration = duration,
                    scrubFeel = scrubFeel,
                    accent = DSColors.goldBright,
                    onSeek = onSeek,
                )
            }
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
                text = "加载中…",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
internal fun VideoPlaybackUnavailableOverlay(
    state: V2PlaybackState,
    onBack: () -> Unit,
    onRetry: (() -> Unit)?,
) {
    val isNoSource = state == V2PlaybackState.UnavailableNoSource
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF1F2937).copy(alpha = 0.96f))
                .border(1.dp, Color(0xFF374151).copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = if (isNoSource) Icons.Filled.PhoneAndroid else Icons.Filled.WifiOff,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.padding(top = 4.dp).size(38.dp),
            )
            Text(
                text = if (isNoSource) "视频不在本机" else "视频暂时无法加载",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (isNoSource) {
                    "你可能正在另一台设备查看这条记录。\n视频源文件只保存在录制它的设备上，跨设备播放暂未支持。\n标题、笔记与标签已自动同步过来。"
                } else {
                    "可能是网络问题，或这条视频源已不可用。"
                },
                color = Color(0xFFD1D5DB),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
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
                    .height(if (onRetry == null) 46.dp else 44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (onRetry == null) DSColors.fairway else Color.Transparent)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "返回",
                    color = Color.White.copy(alpha = if (onRetry == null) 1f else 0.75f),
                    fontSize = if (onRetry == null) 16.sp else 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
internal fun VideoMemoDeleteConfirmDialog(
    deleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable(enabled = !deleting, onClick = onDismiss),
            )
            Column(
                modifier = Modifier
                    .width(270.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF2F2F7).copy(alpha = 0.98f)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        "删除备忘录",
                        color = Color(0xFF1C1C1E),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "确定删除这个视频备忘录吗？此操作不可撤销。",
                        color = Color(0xFF3A3A3C),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD1D1D6)))
                Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxSize()
                            .clickable(enabled = !deleting, onClick = onDismiss)
                            .testTag("video-memo-delete-cancel"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "取消",
                            color = Color(0xFF007AFF),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Box(Modifier.width(1.dp).fillMaxSize().background(Color(0xFFD1D1D6)))
                    Row(
                        modifier = Modifier.weight(1f).fillMaxSize()
                            .clickable(enabled = !deleting, onClick = onConfirm)
                            .testTag("video-memo-delete-confirm"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (deleting) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF3B30),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            if (deleting) "删除中…" else "删除",
                            color = Color(0xFFFF3B30),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun VideoMemoDeleteFailureDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable(onClick = onDismiss),
            )
            Column(
                modifier = Modifier.width(270.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF2F2F7).copy(alpha = 0.98f))
                    .testTag("video-memo-delete-failure-dialog"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        "删除失败",
                        color = Color(0xFF1C1C1E),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        message,
                        color = Color(0xFF3A3A3C),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD1D1D6)))
                Box(
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                        .clickable(onClick = onDismiss)
                        .testTag("video-memo-delete-failure-dismiss"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "好",
                        color = Color(0xFF007AFF),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
internal fun VideoComparisonPickerDialog(
    candidates: List<V2Memo>,
    onDismiss: () -> Unit,
    onSelect: (V2Memo) -> Unit,
) {
    var selectedMemoId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedMemo = remember(candidates, selectedMemoId) {
        candidates.firstOrNull { it.id.toString() == selectedMemoId }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
            V2RoomBackground(Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .v2StatusBarPadding()
                    .v2NavigationBarPadding(),
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭", tint = DSColors.fairway)
                }
                Spacer(Modifier.weight(1f))
                Text("选择对比视频", color = DSColors.inkGreen, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(40.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DSColors.borderLine),
            )
            if (candidates.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideoLibrary,
                        contentDescription = null,
                        tint = DSColors.shadowGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("暂无可对比的视频", color = DSColors.shadowGray, fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { Spacer(Modifier.height(6.dp)) }
                    items(candidates, key = { it.id }) { memo ->
                        VideoComparisonCandidateRow(
                            memo = memo,
                            selected = selectedMemo?.id == memo.id,
                            onClick = { selectedMemoId = memo.id.toString() },
                        )
                    }
                    item { Spacer(Modifier.height(76.dp)) }
                }
                if (selectedMemo != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 16.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(DSColors.fairway, DSColors.deepGreen),
                                ),
                            )
                            .clickable { selectedMemo?.let(onSelect) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("确认选择", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun VideoComparisonCandidateRow(memo: V2Memo, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .semantics { this.selected = selected }
            .background(DSColors.cardWhite)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) DSColors.fairway else DSColors.listCardBorder,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DSColors.fairway.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            V2MemoThumbnail(memo = memo, modifier = Modifier.fillMaxSize())
        }
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
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
        if (selected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "已选择",
                tint = DSColors.fairway,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, DSColors.listCardBorder, CircleShape),
            )
        }
    }
}

@Composable
private fun TopBar(
    showToolsPanel: Boolean,
    drawingEnabled: Boolean,
    onBack: () -> Unit,
    onToggleDrawing: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            icon = Icons.Filled.Close,
            contentDescription = "返回",
            iconSize = 16.dp,
            onClick = onBack,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(DSColors.inkGreen)
                .border(1.dp, DSColors.fairway, RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.VideoLibrary, null, tint = Color.White, modifier = Modifier.size(12.dp))
            Text("视频Memo", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.weight(1f))
        CircleIconButton(
            icon = Icons.Filled.Edit,
            contentDescription = if (showToolsPanel) "关闭绘图工具" else "打开绘图工具",
            iconSize = 22.dp,
            tint = when {
                !drawingEnabled -> Color.White.copy(alpha = 0.35f)
                showToolsPanel -> DSColors.goldBright
                else -> Color.White
            },
            enabled = drawingEnabled,
            onClick = onToggleDrawing,
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
    isPlaying: Boolean,
    speedText: String,
    isSlow: Boolean,
    showMoreTools: Boolean,
    controlsLocked: Boolean,
    onSeek: (Float) -> Unit,
    onTimelineScrubStart: () -> Unit,
    onTimelineScrubEnd: () -> Unit,
    onStepBackward: () -> Unit,
    onStepForward: () -> Unit,
    onTogglePlay: () -> Unit,
    onSpeed: () -> Unit,
    onToggleMore: () -> Unit,
    onShare: () -> Unit,
    onShareToSpaces: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onCompare: () -> Unit,
    onDelete: () -> Unit,
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
            ReviewFrameScrubber(
                currentMs = (currentTime * 1000f).toLong().coerceAtLeast(0L),
                durationMs = (duration * 1000f).toLong().coerceAtLeast(1L),
                onSeek = { ms -> onSeek(ms / 1000f) },
                onScrubStart = onTimelineScrubStart,
                onScrubEnd = onTimelineScrubEnd,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            )
            BottomMemoPlaybackControls(
                isPlaying = isPlaying,
                speedText = speedText,
                isSlow = isSlow,
                showMoreTools = showMoreTools,
                controlsLocked = controlsLocked,
                onStepBackward = onStepBackward,
                onStepForward = onStepForward,
                onTogglePlay = onTogglePlay,
                onSpeed = onSpeed,
                onToggleMore = onToggleMore,
                onShare = onShare,
                onShareToSpaces = onShareToSpaces,
                onExport = onExport,
                onEdit = onEdit,
                onCompare = onCompare,
                onDelete = onDelete,
            )
        }
    }
}


@Composable
private fun ScrubFeelControl(
    selected: TimelineScrubFeel,
    onSelected: (TimelineScrubFeel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.42f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("手感", color = Color.White.copy(alpha = 0.68f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        TimelineScrubFeel.entries.forEach { feel ->
            Text(
                text = feel.title,
                color = if (selected == feel) Color.Black else Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .size(width = 34.dp, height = 26.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected == feel) Color.White else Color.White.copy(alpha = 0.14f))
                    .semantics { this.selected = selected == feel }
                    .clickable { onSelected(feel) }
                    .padding(top = 5.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ScrubFeelProgressBar(
    currentTime: Float,
    duration: Float,
    scrubFeel: TimelineScrubFeel,
    accent: Color,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (duration > 0f) (currentTime / duration).coerceIn(0f, 1f) else 0f
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(duration, currentTime, scrubFeel) {
                var startTime = currentTime
                var accumulatedX = 0f
                detectDragGestures(
                    onDragStart = {
                        startTime = currentTime
                        accumulatedX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        accumulatedX += dragAmount.x
                        val target = (startTime + accumulatedX / scrubFeel.pixelsPerSecond)
                            .coerceIn(0f, duration.coerceAtLeast(0f))
                        onSeek(target)
                        change.consume()
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.28f)))
        Box(
            Modifier
                .fillMaxWidth(progress)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(accent),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = (maxWidth - 16.dp) * progress)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
internal fun BottomMemoPlaybackControls(
    isPlaying: Boolean,
    speedText: String,
    isSlow: Boolean,
    showMoreTools: Boolean,
    controlsLocked: Boolean,
    onStepBackward: () -> Unit,
    onStepForward: () -> Unit,
    onTogglePlay: () -> Unit,
    onSpeed: () -> Unit,
    onToggleMore: () -> Unit,
    onShare: () -> Unit,
    onShareToSpaces: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onCompare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = showMoreTools,
            enter = fadeIn() + slideInHorizontally { it / 3 },
            exit = fadeOut() + slideOutHorizontally { it / 3 },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 2.dp, bottom = 58.dp),
        ) {
            Column(
                modifier = Modifier
                    .width(142.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C1C1E).copy(alpha = 0.95f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(vertical = 2.dp),
            ) {
                CompactMoreItem(icon = Icons.Filled.Groups, label = "分享到空间", onClick = onShareToSpaces)
                MoreToolsDivider()
                CompactMoreItem(icon = Icons.Filled.Share, label = "系统分享", onClick = onShare)
                MoreToolsDivider()
                CompactMoreItem(icon = Icons.Filled.Download, label = "导出", onClick = onExport)
                MoreToolsDivider()
                CompactMoreItem(icon = Icons.AutoMirrored.Filled.CompareArrows, label = "对比", onClick = onCompare)
                MoreToolsDivider()
                CompactMoreItem(icon = Icons.Filled.Folder, label = "分类", onClick = onEdit)
                MoreToolsDivider()
                CompactMoreItem(icon = Icons.Filled.DeleteOutline, label = "删除", onClick = onDelete)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(enabled = !controlsLocked, onClick = onSpeed),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        speedText,
                        color = if (isSlow) DSColors.goldBright else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BottomControlIconButton(
                    icon = Icons.Filled.Replay5,
                    contentDescription = "后退 5 秒",
                    enabled = !controlsLocked,
                    onClick = onStepBackward,
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BottomControlIconButton(
                    icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停视频" else "播放视频",
                    emphasized = true,
                    enabled = !controlsLocked,
                    onClick = onTogglePlay,
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BottomControlIconButton(
                    icon = Icons.Filled.Forward5,
                    contentDescription = "前进 5 秒",
                    enabled = !controlsLocked,
                    onClick = onStepForward,
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BottomControlIconButton(
                    icon = if (showMoreTools) Icons.Filled.Close else Icons.Filled.MoreHoriz,
                    contentDescription = if (showMoreTools) "关闭更多工具" else "打开更多工具",
                    outlinedCircle = true,
                    enabled = !controlsLocked,
                    onClick = onToggleMore,
                )
            }
        }
    }
}

@Composable
private fun CompactMoreItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun MoreToolsDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.2f)))
}

@Composable
private fun BottomControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    emphasized: Boolean = false,
    outlinedCircle: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = if (emphasized) 54.dp else 44.dp, height = if (emphasized) 54.dp else 44.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            emphasized -> {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (enabled) 1f else 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            outlinedCircle -> {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .border(1.5.dp, Color.White.copy(alpha = if (enabled) 1f else 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription,
                        tint = Color.White.copy(alpha = if (enabled) 1f else 0.35f),
                        modifier = Modifier.size(if (icon == Icons.Filled.MoreHoriz) 15.dp else 12.dp),
                    )
                }
            }
            else -> {
                Icon(
                    icon,
                    contentDescription,
                    tint = Color.White.copy(alpha = if (enabled) 1f else 0.35f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = if (enabled) 0.4f else 0.22f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

private fun Float.speedText(): String = when {
    this == 1f -> "1x"
    this == 0.5f -> "0.5x"
    this == 0.25f -> "0.25x"
    else -> "${this}x"
}

private suspend fun saveVideoFrameToGallery(
    context: Context,
    playbackUrl: String,
    timeSec: Float,
    displayName: String,
): String = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    var pendingUri: Uri? = null
    try {
        retriever.setDataSource(context, playbackUrl.toUri())
        val bitmap = retriever.getFrameAtTime(
            (timeSec.coerceAtLeast(0f) * 1_000_000L).toLong(),
            MediaMetadataRetriever.OPTION_CLOSEST,
        ) ?: return@withContext "当前帧暂时无法生成截图"

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyGolfSwing")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext "系统相册暂时不可写，请稍后重试"
        pendingUri = uri

        val wrote = resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } == true
        bitmap.recycle()
        if (!wrote) {
            resolver.delete(uri, null, null)
            return@withContext "截图写入失败，请稍后重试"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        "已保存当前帧截图"
    } catch (error: CancellationException) {
        pendingUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
        throw error
    } catch (error: Throwable) {
        pendingUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
        videoMemoGalleryFailureMessage("截图保存失败，请稍后重试", error)
    } finally {
        runCatching { retriever.release() }
    }
}

private suspend fun saveVideoMemoToGallery(
    context: Context,
    memo: V2Memo,
    playbackUrl: String,
): String = withContext(Dispatchers.IO) {
    val sourcePath = V2MemoBridge.localVideoAbsolutePath(memo.videoUrl)
        ?: playbackUrl.takeIf { it.startsWith("file://", ignoreCase = true) }?.removePrefix("file://")
        ?: return@withContext "只有本地视频可以保存到相册"
    val sourceFile = File(sourcePath)
    if (!sourceFile.exists() || !sourceFile.isFile || sourceFile.length() <= 0L) {
        return@withContext "本地视频文件不存在，无法保存到相册"
    }

    val resolver = context.contentResolver
    val gallerySpec = videoMemoGalleryVideoSpec(sourceFile)
    val displayBase = (memo.displayTitle ?: memo.title ?: "video_memo")
        .replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), "_")
        .take(42)
        .ifBlank { "video_memo" }
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, "${displayBase}_${System.currentTimeMillis()}.${gallerySpec.extension}")
        put(MediaStore.Video.Media.MIME_TYPE, gallerySpec.mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/MyGolfSwing")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
    }
    var pendingUri: Uri? = null
    try {
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext "系统相册暂时不可写，请稍后重试"
        pendingUri = uri
        val wrote = resolver.openOutputStream(uri)?.use { out ->
            sourceFile.inputStream().use { input -> input.copyTo(out) }
        } != null
        if (!wrote) {
            resolver.delete(uri, null, null)
            return@withContext "视频写入相册失败，请稍后重试"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        "视频已保存到相册"
    } catch (error: CancellationException) {
        pendingUri?.let { runCatching { resolver.delete(it, null, null) } }
        throw error
    } catch (error: Throwable) {
        pendingUri?.let { runCatching { resolver.delete(it, null, null) } }
        videoMemoGalleryFailureMessage("保存到相册失败，请稍后重试", error)
    }
}

internal fun videoMemoGalleryFailureMessage(base: String, error: Throwable): String {
    if (error is CancellationException) throw error
    return base
}

internal data class VideoMemoGalleryVideoSpec(
    val extension: String,
    val mimeType: String,
)

internal fun videoMemoGalleryVideoSpec(source: File): VideoMemoGalleryVideoSpec {
    val extension = when (source.extension.lowercase(Locale.ROOT)) {
        "mov", "qt" -> "mov"
        "m4v" -> "m4v"
        "webm" -> "webm"
        else -> "mp4"
    }
    val mimeType = when (extension) {
        "mov" -> "video/quicktime"
        "m4v" -> "video/x-m4v"
        "webm" -> "video/webm"
        else -> "video/mp4"
    }
    return VideoMemoGalleryVideoSpec(extension = extension, mimeType = mimeType)
}

private fun buildComparisonReviewMemo(
    primaryMemo: V2Memo,
    secondaryMemo: V2Memo,
    primaryTime: Float,
    primaryDuration: Float,
    secondaryTime: Float,
    secondaryDuration: Float,
    isOverlayMode: Boolean,
    syncSeek: Boolean,
    scrubFeel: TimelineScrubFeel,
): V2Memo {
    val now = LocalDateTime.now()
    val title = "视频对比复盘 · ${DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA).format(now)}"
    val primaryTitle = primaryMemo.displayTitle ?: primaryMemo.title ?: "当前视频"
    val secondaryTitle = secondaryMemo.displayTitle ?: secondaryMemo.title ?: "对比视频"
    val body = buildString {
        append("对比对象：")
        append(primaryTitle)
        append(" vs ")
        append(secondaryTitle)
        append("\n")
        append("当前视频时间：${formatComparisonTime(primaryTime)} / ${formatComparisonTime(primaryDuration)}")
        append("\n")
        append("对比视频时间：${formatComparisonTime(secondaryTime)} / ${formatComparisonTime(secondaryDuration)}")
        append("\n")
        append("查看模式：${if (isOverlayMode) "VS 叠放" else "上下分屏"} · ${if (syncSeek) "同步拖动" else "独立拖动"} · 手感 ${scrubFeel.title}")
        append("\n\n")
        append("观察记录：\n")
        append("- 击球准备：\n")
        append("- 上杆 / 下杆节奏：\n")
        append("- 触球瞬间：\n")
        append("- 下一次练习重点：")
    }
    return V2Memo.textMemo(
        title = title,
        body = body,
        topic = V2Topic.SWING,
        tags = listOf("视频对比", "复盘"),
        accent = V2MemoAccent.REFLECTION,
        subText = "$primaryTitle vs $secondaryTitle",
    ).copy(
        isNew = true,
        metaText = V2MemoBridge.relativeMetaText(now, "视频对比"),
        thumbnailKind = V2MemoThumbnailKind.FAIRWAY,
    )
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

private fun shareVideoMemoInfo(context: Context, memo: V2Memo, playbackUrl: String): Boolean {
    val title = memo.displayTitle ?: memo.title ?: "视频 Memo"
    localVideoFileForShare(memo, playbackUrl)?.let { file ->
        shareUriForFile(context, file)?.let { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = videoMimeFor(file)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, title, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            return runCatching {
                context.startActivity(Intent.createChooser(intent, "分享视频"))
            }.isSuccess
        }
    }
    val remoteUrl = playbackUrl.takeIf { V2MemoBridge.isRemoteUrl(it) }
    val shareText = buildVideoMemoShareText(title, memo, remoteUrl)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    return runCatching {
        context.startActivity(Intent.createChooser(intent, "分享视频信息"))
    }.isSuccess
}

private fun shareVideoMemoComparison(
    context: Context,
    primaryMemo: V2Memo,
    primaryPlaybackUrl: String,
    secondaryMemo: V2Memo,
    secondaryPlaybackUrl: String,
): Boolean {
    val primaryTitle = primaryMemo.displayTitle ?: primaryMemo.title ?: "当前视频"
    val secondaryTitle = secondaryMemo.displayTitle ?: secondaryMemo.title ?: "对比视频"
    val title = "MyGolfSwing 对比视频：$primaryTitle / $secondaryTitle"
    val shareFiles = listOfNotNull(
        localVideoFileForShare(primaryMemo, primaryPlaybackUrl),
        localVideoFileForShare(secondaryMemo, secondaryPlaybackUrl),
    )
    val shareUris = shareFiles.mapNotNull { file -> shareUriForFile(context, file) }
    val shareText = buildVideoMemoComparisonShareText(
        title = title,
        primaryTitle = primaryTitle,
        primaryPlaybackUrl = primaryPlaybackUrl,
        secondaryTitle = secondaryTitle,
        secondaryPlaybackUrl = secondaryPlaybackUrl,
        sharedLocalCount = shareUris.size,
    )

    if (shareUris.isNotEmpty()) {
        val clipData = ClipData.newUri(context.contentResolver, title, shareUris.first())
        shareUris.drop(1).forEach { uri -> clipData.addItem(ClipData.Item(uri)) }
        val intent = Intent(if (shareUris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND).apply {
            type = if (shareUris.size > 1) "video/*" else videoMimeFor(shareFiles.first())
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareText)
            if (shareUris.size > 1) {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(shareUris))
            } else {
                putExtra(Intent.EXTRA_STREAM, shareUris.first())
            }
            this.clipData = clipData
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(Intent.createChooser(intent, "分享对比视频"))
        }.isSuccess
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    return runCatching {
        context.startActivity(Intent.createChooser(intent, "分享对比视频信息"))
    }.isSuccess
}

private fun localVideoFileForShare(memo: V2Memo, playbackUrl: String): File? {
    val localPath = V2MemoBridge.localVideoAbsolutePath(memo.videoUrl)
        ?: playbackUrl.takeIf { it.startsWith("file://", ignoreCase = true) }?.removePrefix("file://")
    return localPath
        ?.let(::File)
        ?.takeIf { it.exists() && it.isFile && it.length() > 0L }
}

private fun shareUriForFile(context: Context, file: File): Uri? {
    return runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}

private fun videoMimeFor(file: File): String {
    return when (file.extension.lowercase(Locale.ROOT)) {
        "mov", "qt" -> "video/quicktime"
        "m4v" -> "video/x-m4v"
        "webm" -> "video/webm"
        else -> "video/mp4"
    }
}

private fun buildVideoMemoShareText(title: String, memo: V2Memo, remoteUrl: String?): String {
    return buildList {
        add(title)
        memo.trainingMethod?.takeIf { it.isNotBlank() }?.let { add("训练方式：$it") }
        memo.metaText?.takeIf { it.isNotBlank() }?.let { add("记录信息：$it") }
        memo.durationSec?.takeIf { it > 0 }?.let { add("时长：${formatTime(it.toFloat())}") }
        memo.text?.takeIf { it.isNotBlank() }?.let { add("笔记：$it") }
        if (remoteUrl != null) {
            add("视频链接：$remoteUrl")
        } else {
            add("视频来源：本机视频")
        }
    }.joinToString("\n")
}

private fun buildVideoMemoComparisonShareText(
    title: String,
    primaryTitle: String,
    primaryPlaybackUrl: String,
    secondaryTitle: String,
    secondaryPlaybackUrl: String,
    sharedLocalCount: Int,
): String {
    return buildList {
        add(title)
        add("当前视频：$primaryTitle")
        add("对比视频：$secondaryTitle")
        if (sharedLocalCount > 0) {
            add("已附上本机可分享的视频文件：$sharedLocalCount 个")
        }
        primaryPlaybackUrl.takeIf { V2MemoBridge.isRemoteUrl(it) }?.let { add("当前视频链接：$it") }
        secondaryPlaybackUrl.takeIf { V2MemoBridge.isRemoteUrl(it) }?.let { add("对比视频链接：$it") }
    }.joinToString("\n")
}

private fun formatTime(seconds: Float): String {
    val safe = seconds.coerceAtLeast(0f)
    val total = safe.toInt()
    val minutes = total / 60
    val secs = total % 60
    val tenths = ((safe - total) * 10).roundToInt().coerceIn(0, 9)
    return "%d:%02d.%d".format(minutes, secs, tenths)
}

private fun formatComparisonTime(seconds: Float): String {
    val safe = seconds.coerceAtLeast(0f)
    val total = safe.toInt()
    val minutes = total / 60
    val secs = total % 60
    val millis = ((safe - total) * 1000).toInt().coerceIn(0, 999)
    return "%d:%02d.%03d".format(minutes, secs, millis)
}
