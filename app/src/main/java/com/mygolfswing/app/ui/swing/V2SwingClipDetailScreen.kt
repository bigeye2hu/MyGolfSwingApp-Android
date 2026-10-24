package com.mygolfswing.app.ui.swing

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.model.V2SwingClip
import com.mygolfswing.app.data.model.V2SwingClipRetention
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.V2DrawingOverlay
import com.mygolfswing.app.ui.common.V2DrawingState
import com.mygolfswing.app.ui.common.V2DrawingToolsPanel
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.common.v2FullScreenInsets
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.player.ReviewFrameScrubber
import com.mygolfswing.app.ui.player.SmoothFrameSeeker
import com.mygolfswing.app.ui.golfcam.model.ImpactCandidate
import com.mygolfswing.app.ui.interaction.V2ProfessionalAnalysisMode
import com.mygolfswing.app.ui.interaction.V2WorkspaceProfessionalAnalysisScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

private enum class BottomActionStyle {
    Framed,
    Plain,
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun V2SwingClipDetailScreen(
    clipId: UUID,
    showCloseButton: Boolean = false,
    onBack: () -> Unit,
) {
    V2BackHandler(onBack = onBack)
    val sessions by V2SwingSessionStore.sessions.collectAsState()
    val swingError by V2SwingSessionStore.lastError.collectAsState()
    val clip = sessions.flatMap { it.clips }.firstOrNull { it.id == clipId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var currentTimeSec by remember { mutableFloatStateOf(0f) }
    var durationSec by remember { mutableFloatStateOf(0f) }
    var playbackRate by remember { mutableFloatStateOf(1f) }
    var showDrawingTools by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showPromoteConfirm by remember { mutableStateOf(false) }
    var showDemoteConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showClearDrawingConfirm by remember { mutableStateOf(false) }
    var showsReviewPlayer by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var metadata by remember(clipId) { mutableStateOf(SwingVideoMetadata()) }
    val drawingState = remember { V2DrawingState() }

    if (clip == null) {
        V2MissingEntityScreen(message = "挥杆视频不存在", onBack = onBack, title = "挥杆详情")
        return
    }

    val uri = remember(clip.videoPath) { mediaUriFor(clip.videoPath) }
    if (uri == null) {
        V2MissingEntityScreen(message = "本地挥杆视频文件不存在", onBack = onBack, title = "挥杆详情")
        return
    }
    val reviewSourceFile = remember(clip.videoPath) {
        V2MemoBridge.localVideoAbsolutePath(clip.videoPath)?.let(::File)?.takeIf { it.isFile && it.length() > 0L }
    }
    if (showsReviewPlayer && reviewSourceFile != null) {
        V2WorkspaceProfessionalAnalysisScreen(
            sourceFile = reviewSourceFile,
            title = "回看",
            mode = V2ProfessionalAnalysisMode.STANDALONE_REVIEW,
            onSwitchPlayer = { showsReviewPlayer = false },
            onCancel = onBack,
            onRecordingReady = { recording ->
                scope.launch {
                    val result = saveClipToGallery(context, clip.copy(videoPath = recording.absolutePath))
                    android.widget.Toast.makeText(context, result.message, android.widget.Toast.LENGTH_SHORT).show()
                    recording.delete()
                }
            },
        )
        return
    }
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            playWhenReady = false
            prepare()
        }
    }
    val frameSeeker = remember(player) { SmoothFrameSeeker(player) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    player.pause()
                    player.seekTo(0)
                    isPlaying = false
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
                currentTimeSec = (player.currentPosition / 1000f).coerceAtLeast(0f)
                isPlaying = player.isPlaying
            }
            val dur = player.duration
            if (dur > 0) durationSec = dur / 1000f
            delay(80)
        }
    }
    LaunchedEffect(uri) {
        metadata = loadSwingVideoMetadata(context, uri)
    }
    LaunchedEffect(playbackRate) {
        player.playbackParameters = PlaybackParameters(playbackRate)
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2_000)
            toast = null
        }
    }
    LaunchedEffect(swingError) {
        val message = swingError ?: return@LaunchedEffect
        toast = message
        V2SwingSessionStore.consumeLastError()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize().v2FullScreenInsets()) {
            DetailTopBar(
                onBack = onBack,
                onHelp = { showHelp = true },
                title = "挥杆详情",
                showBackLabel = !showCloseButton,
                switchAccessibilityLabel = "切换到回看",
                onSwitchPlayer = {
                    if (reviewSourceFile == null) {
                        toast = "这段视频暂时不能回看"
                    } else {
                        showsReviewPlayer = true
                        showDrawingTools = false
                        drawingState.clearSelection()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    update = { it.player = player },
                )
                V2DrawingOverlay(
                    state = drawingState,
                    modifier = Modifier.fillMaxSize(),
                    interactionEnabled = showDrawingTools,
                )
                if (showDrawingTools) {
                    V2DrawingToolsPanel(
                        state = drawingState,
                        onClose = {
                            showDrawingTools = false
                            drawingState.clearSelection()
                            if (drawingState.strokes.isNotEmpty()) {
                                toast = "标记已保留，可继续播放"
                            }
                        },
	                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
	                    )
	                }
            }
                ControlsPanel(
                clip = clip,
                isPlaying = isPlaying,
                currentTime = currentTimeSec,
                duration = durationSec,
                metadata = metadata,
                playbackRate = playbackRate,
                showDetails = showDetails,
                showDrawingTools = showDrawingTools,
                hasDrawings = drawingState.strokes.isNotEmpty(),
                onTogglePlay = {
                    if (player.isPlaying) player.pause() else player.play()
                },
                onStepFrames = { deltaFrames ->
                    player.pause()
                    val targetMs = metadata.stepTargetMs(
                        positionMs = player.currentPosition,
                        deltaFrames = deltaFrames,
                        durationSec = durationSec,
                    )
                    player.setSeekParameters(SeekParameters.EXACT)
                    player.seekTo(targetMs)
                    currentTimeSec = targetMs / 1000f
                    isPlaying = false
                },
                onSeekToFrame = { frameIndex ->
                    val clampedFrame = frameIndex.coerceIn(0, metadata.totalFrames(durationSec).coerceAtLeast(1) - 1)
                    val targetMs = (clampedFrame * metadata.frameDurationMs).roundToInt().toLong()
                    frameSeeker.moveTo(targetMs.coerceIn(0L, player.duration.coerceAtLeast(0L)))
                    currentTimeSec = targetMs / 1000f
                    isPlaying = false
                },
                onFrameScrubStart = {
                    val toleranceUs = (metadata.frameDurationMs.coerceAtLeast(1f) * 1000f).roundToInt().toLong()
                    frameSeeker.begin(toleranceUs)
                    isPlaying = false
                },
                onFrameScrubEnd = { frameIndex ->
                    val clampedFrame = frameIndex.coerceIn(0, metadata.totalFrames(durationSec).coerceAtLeast(1) - 1)
                    val targetMs = (clampedFrame * metadata.frameDurationMs).roundToInt().toLong()
                    frameSeeker.end()
                    player.seekTo(targetMs.coerceIn(0L, player.duration.coerceAtLeast(0L)))
                    currentTimeSec = targetMs / 1000f
                },
                onRate = {
                    playbackRate = it
                    toast = if (it == 1f) "已恢复正常速度" else "慢放 ${swingRateText(it)}"
                },
                onFavorite = {
                    if (clip.retention == V2SwingClipRetention.PROMOTED) return@ControlsPanel
                    val next = if (clip.retention == V2SwingClipRetention.FAVORITED) {
                        V2SwingClipRetention.TEMPORARY
                    } else {
                        V2SwingClipRetention.FAVORITED
                    }
                    V2SwingSessionStore.updateClipRetention(clip.id, next)
                },
                onPromote = {
                    if (clip.retention == V2SwingClipRetention.PROMOTED) {
                        showDemoteConfirm = true
                    } else {
                        showPromoteConfirm = true
                    }
                },
                onSave = {
                    scope.launch {
                        toast = saveClipToGallery(context, clip).message
                    }
                },
                onInfo = { showDetails = !showDetails },
                onToggleDrawing = {
                    showDrawingTools = !showDrawingTools
                    if (!showDrawingTools) {
                        drawingState.clearSelection()
                        if (drawingState.strokes.isNotEmpty()) {
                            toast = "标记已保留，可继续播放"
                        }
                    }
                },
                onRequestClearDrawing = { showClearDrawingConfirm = true },
                onDelete = { showDeleteConfirm = true },
                )
        }
        toast?.let {
            Text(
                it,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
                    .clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除视频") },
            text = { Text("确定要删除这段挥杆视频吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    V2SwingSessionStore.deleteClips(setOf(clip.id))
                    showDeleteConfirm = false
                    onBack()
                }) { Text("删除", color = DSColors.flagRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }

    if (showHelp) {
        SwingClipHelpSheet(onDismiss = { showHelp = false })
    }

    if (showClearDrawingConfirm) {
        ClearDrawingConfirmationSheet(
            onConfirm = {
                drawingState.clearAll()
                showClearDrawingConfirm = false
                toast = "标记已清空"
            },
            onDismiss = { showClearDrawingConfirm = false },
        )
    }

    if (showPromoteConfirm) {
        AlertDialog(
            onDismissRequest = { showPromoteConfirm = false },
            title = { Text("存入成长池") },
            text = { Text("将此挥杆视频同步到成长池（视频备忘录）中？") },
            confirmButton = {
                TextButton(onClick = {
                    V2SwingSessionStore.updateClipRetention(clip.id, V2SwingClipRetention.PROMOTED)
                    showPromoteConfirm = false
                    toast = "已存入成长池"
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showPromoteConfirm = false }) { Text("取消") }
            },
        )
    }

    if (showDemoteConfirm) {
        AlertDialog(
            onDismissRequest = { showDemoteConfirm = false },
            title = { Text("移出成长池") },
            text = { Text("从成长池中移除，视频将保留在收藏中。") },
            confirmButton = {
                TextButton(onClick = {
                    V2SwingSessionStore.updateClipRetention(clip.id, V2SwingClipRetention.FAVORITED)
                    showDemoteConfirm = false
                    toast = "已移出成长池，保留为收藏"
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showDemoteConfirm = false }) { Text("取消") }
            },
        )
    }
}

@Composable
internal fun DetailTopBar(
    onBack: () -> Unit,
    onHelp: () -> Unit,
    title: String,
    showBackLabel: Boolean = true,
    switchAccessibilityLabel: String,
    onSwitchPlayer: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .height(44.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = if (showBackLabel) null else "关闭",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            if (showBackLabel) {
                Text("返回", color = Color.White, fontSize = 16.sp)
            }
        }
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onHelp, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "使用说明",
                    tint = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier.size(16.dp),
                )
            }
            IconButton(onClick = onSwitchPlayer, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.Sync,
                    contentDescription = switchAccessibilityLabel,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
internal fun ControlsPanel(
    clip: V2SwingClip,
    isPlaying: Boolean,
    currentTime: Float,
    duration: Float,
    metadata: SwingVideoMetadata,
    playbackRate: Float,
    showDetails: Boolean,
    showDrawingTools: Boolean,
    hasDrawings: Boolean,
    onTogglePlay: () -> Unit,
    onStepFrames: (Int) -> Unit,
    onSeekToFrame: (Int) -> Unit,
    onFrameScrubStart: () -> Unit,
    onFrameScrubEnd: (Int) -> Unit,
    onRate: (Float) -> Unit,
    onFavorite: () -> Unit,
    onPromote: () -> Unit,
    onSave: () -> Unit,
    onInfo: () -> Unit,
    onToggleDrawing: () -> Unit,
    onRequestClearDrawing: () -> Unit,
    onDelete: () -> Unit,
) {
    val totalFrames = metadata.totalFrames(duration)
    val currentFrame = metadata.currentFrame(currentTime, duration)
    Column(Modifier.fillMaxWidth().background(Color.Black)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${"%.2f".format(currentTime)}s", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.weight(1f))
            Text("帧 $currentFrame/$totalFrames", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.weight(1f))
            Text("${"%.2f".format(duration)}s", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton(Icons.Filled.SkipPrevious, "-10") { onStepFrames(-10) }
            StepButton(Icons.Filled.Replay5, "-3") { onStepFrames(-3) }
            StepButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "-1") { onStepFrames(-1) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onTogglePlay, modifier = Modifier.size(44.dp)) {
                Icon(
                    if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            StepButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "+1") { onStepFrames(1) }
            StepButton(Icons.Filled.Forward5, "+3") { onStepFrames(3) }
            StepButton(Icons.Filled.SkipNext, "+10") { onStepFrames(10) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(0.1f, 0.25f, 0.5f, 1f).forEach { rate ->
                val isActive = playbackRate == rate
                Text(
                    swingRateText(rate),
                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(if (isActive) DSColors.fairway.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.10f))
                        .clickable { onRate(rate) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            DrawingChip(active = showDrawingTools, onClick = onToggleDrawing)
            if (hasDrawings && !showDrawingTools) {
                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = 28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DSColors.flagRed.copy(alpha = 0.14f))
                        .clickable(onClick = onRequestClearDrawing),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.LayersClear,
                        contentDescription = "清空绘制标记",
                        tint = DSColors.flagRed.copy(alpha = 0.90f),
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
        }
        val durationMs = (duration * 1000f).roundToInt().toLong().coerceAtLeast(1L)
        val shownMs = (currentTime * 1000f).roundToInt().toLong().coerceIn(0L, durationMs)
        val scrubTargetMs = remember(durationMs) { longArrayOf(shownMs) }
        ReviewFrameScrubber(
            currentMs = shownMs,
            durationMs = durationMs,
            onSeek = { ms ->
                scrubTargetMs[0] = ms
                val lastFrame = (totalFrames - 1).coerceAtLeast(0)
                val frame = ((ms.toFloat() / durationMs) * lastFrame).roundToInt().coerceIn(0, lastFrame)
                onSeekToFrame(frame)
            },
            onScrubStart = onFrameScrubStart,
            onScrubEnd = {
                val lastFrame = (totalFrames - 1).coerceAtLeast(0)
                val frame = ((scrubTargetMs[0].toFloat() / durationMs) * lastFrame).roundToInt().coerceIn(0, lastFrame)
                onFrameScrubEnd(frame)
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.10f)))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val isPromoted = clip.retention == V2SwingClipRetention.PROMOTED
                BottomAction(
                    icon = if (clip.retention == V2SwingClipRetention.FAVORITED || isPromoted) Icons.Filled.Star else Icons.Filled.StarBorder,
                    label = "收藏",
                    active = clip.retention == V2SwingClipRetention.FAVORITED || isPromoted,
                    enabled = !isPromoted,
                    onClick = onFavorite,
                )
                Spacer(Modifier.weight(1f))
                BottomAction(Icons.Filled.Download, "保存", false, style = BottomActionStyle.Plain, onClick = onSave)
                BottomAction(Icons.Filled.Info, "详情", showDetails, style = BottomActionStyle.Plain, activeColor = DSColors.fairway, onClick = onInfo)
                BottomAction(Icons.Filled.DeleteOutline, "删除", false, style = BottomActionStyle.Plain, onClick = onDelete, danger = true)
            }
        }
        if (showDetails) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DetailRow("时间", clip.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA)))
                DetailRow("时长", swingDetailDurationLabel(if (duration > 0) duration.toDouble() else clip.durationSec))
                DetailRow("帧率", metadata.fpsLabel)
                DetailRow("大小", metadata.fileSizeLabel)
                DetailRow("状态", when (clip.retention) {
                    V2SwingClipRetention.TEMPORARY -> "普通"
                    V2SwingClipRetention.FAVORITED -> "收藏"
                    V2SwingClipRetention.PROMOTED -> "成长池"
                })
            }
        }
    }
}

internal fun videoFrameFromScrubPosition(positionX: Float, width: Float, totalFrames: Int): Int {
    val safeFrames = totalFrames.coerceAtLeast(1)
    if (!positionX.isFinite() || !width.isFinite() || width <= 0f) return 0
    val lastFrame = safeFrames - 1
    return ((positionX / width).coerceIn(0f, 1f) * lastFrame).roundToInt().coerceIn(0, lastFrame)
}

@Composable
internal fun DrawingChip(active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) DSColors.fairway.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = null,
            tint = if (active) DSColors.fairway else Color.White,
            modifier = Modifier.size(14.dp),
        )
        Text("绘制", color = if (active) DSColors.fairway else Color.White, fontSize = 12.sp)
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClearDrawingConfirmationSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF4F3F1),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "清空画面上的所有标记？",
                    color = DSColors.shadowGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(DSColors.borderLine))
                Text(
                    "清空标记",
                    color = DSColors.flagRed,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onConfirm)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                )
            }
            Text(
                "取消",
                color = DSColors.fairway,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
                    .clickable(onClick = onDismiss).padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwingClipHelpSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "功能说明",
                    color = Color(0xFF111827),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                ) {
                    Text("完成", color = DSColors.fairway, fontSize = 16.sp)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E7EB)))
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .testTag("swing-help-content").padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HelpRow(
                    icon = Icons.Filled.PlayCircle,
                    iconColor = Color(0xFF111827),
                    title = "播放与逐帧",
                    body = "中间按钮播放/暂停，左右箭头逐帧前进后退（±1/±3/±10 帧），底部速率按钮可直接切换 0.1x ~ 1x 慢放。",
                )
                HelpRow(
                    icon = Icons.Filled.Edit,
                    iconColor = DSColors.fairway,
                    title = "绘制工具",
                    body = "在视频画面上画线标注挥杆姿态，支持直线、自由画笔和颜色选择，方便分析动作细节。",
                )
                HelpRow(
                    icon = Icons.Filled.Star,
                    iconColor = DSColors.trophyGold,
                    title = "收藏",
                    body = "点击底部星标按钮收藏视频，收藏后永久保留，不会被自动清理。",
                )
                HelpRow(
                    icon = Icons.Filled.Download,
                    iconColor = Color(0xFF3B82F6),
                    title = "保存到相册",
                    body = "将视频导出到系统相册，永久保存在设备中，不受 App 数据清理影响。",
                )
                HelpRow(
                    icon = Icons.Filled.DeleteOutline,
                    iconColor = DSColors.flagRed,
                    title = "删除",
                    body = "永久删除此挥杆视频，操作不可撤销。",
                )
            }
        }
    }
}

@Composable
private fun HelpRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    body: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Color(0xFF111827), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(body, color = Color(0xFF6B7280), fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
internal fun StepButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun BottomAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean = true,
    style: BottomActionStyle = BottomActionStyle.Framed,
    activeColor: Color = DSColors.trophyGold,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    val isFramed = style == BottomActionStyle.Framed
    val color = when {
        danger -> DSColors.flagRed.copy(alpha = 0.70f)
        active -> activeColor
        isFramed -> Color.White.copy(alpha = 0.40f)
        else -> Color.White.copy(alpha = 0.60f)
    }
    val shape = RoundedCornerShape(8.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(if (isFramed) 36.dp else 32.dp)
            .clip(shape)
            .background(if (isFramed && active) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(if (isFramed && active) 1.5.dp else 0.dp, if (isFramed && active) color.copy(alpha = 0.60f) else Color.Transparent, shape)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(if (isFramed) 16.dp else 17.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

private data class GallerySaveResult(
    val saved: Boolean,
    val message: String,
)

private suspend fun saveClipToGallery(context: android.content.Context, clip: V2SwingClip): GallerySaveResult = withContext(Dispatchers.IO) {
    fun fail(message: String, uri: Uri? = null): GallerySaveResult {
        uri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
        return GallerySaveResult(saved = false, message = message)
    }

    runCatching {
        val src = V2MemoBridge.localVideoAbsolutePath(clip.videoPath)?.let(::File)
            ?: return@withContext fail("视频文件已不在本机，无法保存到相册")
        if (!src.exists() || !src.isFile) return@withContext fail("视频文件已不在本机，无法保存到相册")
        val resolver = context.contentResolver
        val gallerySpec = swingGalleryVideoSpec(src, "just_swing_${clip.id}")
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, gallerySpec.displayName)
            put(MediaStore.Video.Media.MIME_TYPE, gallerySpec.mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/JustSwing")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext fail("系统相册暂时不可写，请检查存储空间后重试")
        val wrote = runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                src.inputStream().use { input -> input.copyTo(out) }
            } ?: error("无法写入系统相册")
        }.isSuccess
        if (!wrote) return@withContext fail("无法写入系统相册，请稍后重试", uri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            val published = runCatching { resolver.update(uri, values, null, null) }.isSuccess
            if (!published) return@withContext fail("相册保存未完成，请稍后重试", uri)
        }
        GallerySaveResult(saved = true, message = "已保存到相册")
    }.getOrElse {
        GallerySaveResult(saved = false, message = "保存到相册失败，请稍后重试")
    }
}

internal data class SwingGalleryVideoSpec(
    val displayName: String,
    val mimeType: String,
)

internal fun swingGalleryVideoSpec(source: File, displayBase: String): SwingGalleryVideoSpec {
    val extension = when (source.extension.lowercase(Locale.US)) {
        "mov", "qt" -> "mov"
        "m4v" -> "m4v"
        else -> "mp4"
    }
    val mimeType = when (extension) {
        "mov" -> "video/quicktime"
        "m4v" -> "video/x-m4v"
        else -> "video/mp4"
    }
    return SwingGalleryVideoSpec(
        displayName = "${displayBase}.${extension}",
        mimeType = mimeType,
    )
}

internal data class SwingVideoMetadata(
    val fps: Float = 30f,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
) {
    private val safeFps: Float
        get() = fps.takeIf { it.isFinite() && it > 0f } ?: 30f

    val frameDurationMs: Float get() = 1000f / safeFps

    val fpsLabel: String get() =
        if (fps > 0f) "${"%.0f".format(fps)} fps" else "—"

    val fileSizeLabel: String get() = when {
        fileSizeBytes <= 0L -> "—"
        fileSizeBytes < 1024 * 1024 -> "${fileSizeBytes / 1024} KB"
        else -> "%.1f MB".format(fileSizeBytes / (1024.0 * 1024.0))
    }

    fun totalFrames(durationSec: Float): Int {
        val effectiveDuration = if (durationMs > 0L) durationMs / 1000f else durationSec
        return (effectiveDuration.coerceAtLeast(0f) * safeFps).toInt().coerceAtLeast(1)
    }

    fun currentFrame(currentTimeSec: Float, durationSec: Float): Int =
        (currentTimeSec.coerceAtLeast(0f) * safeFps).toInt().plus(1).coerceIn(1, totalFrames(durationSec))

    fun effectiveDurationMs(durationSec: Float): Long =
        (durationMs.takeIf { it > 0L } ?: (durationSec.coerceAtLeast(0f) * 1000f).roundToInt().toLong())
            .coerceAtLeast(0L)

    fun stepTargetMs(positionMs: Long, deltaFrames: Int, durationSec: Float): Long {
        val total = totalFrames(durationSec)
        val currentIndex = (positionMs.coerceAtLeast(0L) / frameDurationMs).toInt().coerceIn(0, total - 1)
        val targetIndex = (currentIndex + deltaFrames).coerceIn(0, total - 1)
        return (targetIndex * frameDurationMs).roundToInt().toLong()
            .coerceIn(0L, effectiveDurationMs(durationSec))
    }
}

internal data class SwingImpactDetectionUiResult(
    val candidates: List<ImpactCandidate>,
    val showMarkers: Boolean,
)

internal fun swingImpactDetectionUiResult(candidates: List<ImpactCandidate>): SwingImpactDetectionUiResult =
    SwingImpactDetectionUiResult(
        candidates = candidates.take(3),
        // Match iOS: detection only reveals the toggle; markers require an explicit tap.
        showMarkers = false,
    )

internal suspend fun loadSwingVideoMetadata(context: Context, uri: Uri): SwingVideoMetadata = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    runCatching {
        retriever.setDataSource(context, uri)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val fps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            ?.toFloatOrNull()
            ?.takeIf { it.isFinite() && it > 0f }
            ?: 30f
        val fileSize = if (uri.scheme == "file") {
            uri.path?.let(::File)?.takeIf { it.exists() }?.length() ?: 0L
        } else {
            0L
        }
        SwingVideoMetadata(fps = fps, durationMs = durationMs, fileSizeBytes = fileSize)
    }.getOrDefault(SwingVideoMetadata()).also {
        runCatching { retriever.release() }
    }
}

private fun mediaUriFor(pathOrUrl: String): Uri? {
    if (V2MemoBridge.isRemoteUrl(pathOrUrl)) return pathOrUrl.trim().toUri()
    val path = V2MemoBridge.localVideoAbsolutePath(pathOrUrl) ?: return null
    val file = File(path)
    return if (file.exists() && file.isFile) Uri.fromFile(file) else null
}

private fun swingDetailDurationLabel(sec: Double): String = "%.1f 秒".format(Locale.CHINA, sec.coerceAtLeast(0.0))

internal fun swingRateText(rate: Float): String = when (rate) {
    1f -> "1x"
    0.1f -> ".1x"
    0.25f -> ".25x"
    0.5f -> ".5x"
    else -> "%.1fx".format(Locale.US, rate)
}
