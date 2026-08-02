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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoAccent
import com.mygolfswing.app.data.model.V2MemoThumbnailKind
import com.mygolfswing.app.data.model.V2SwingClip
import com.mygolfswing.app.data.model.V2SwingClipRetention
import com.mygolfswing.app.data.model.V2Topic
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.ui.common.V2DrawingOverlay
import com.mygolfswing.app.ui.common.V2DrawingState
import com.mygolfswing.app.ui.common.V2DrawingToolsPanel
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.common.v2FullScreenInsets
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.AudioImpactDetector
import com.mygolfswing.app.ui.golfcam.model.ImpactCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

private enum class SwingReplayScrubFeel(
    val title: String,
    val pointsPerFrame: Float,
) {
    Balanced("稳", 5f),
    Precise("细", 9f),
    Frame("帧", 15f),
}

private enum class BottomActionStyle {
    Framed,
    Plain,
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun V2SwingClipDetailScreen(
    clipId: UUID,
    onBack: () -> Unit,
    onMemoCreated: (UUID) -> Unit = {},
) {
    val sessions by V2SwingSessionStore.sessions.collectAsState()
    val swingError by V2SwingSessionStore.lastError.collectAsState()
    val clip = sessions.flatMap { it.clips }.firstOrNull { it.id == clipId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(true) }
    var currentTimeSec by remember { mutableFloatStateOf(0f) }
    var durationSec by remember { mutableFloatStateOf(0f) }
    var playbackRate by remember { mutableFloatStateOf(1f) }
    var showDrawingTools by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showAnalysisActions by remember { mutableStateOf(false) }
    var showImpactMarkers by remember { mutableStateOf(false) }
    var isDetectingImpact by remember { mutableStateOf(false) }
    var impactCandidates by remember(clipId) { mutableStateOf<List<ImpactCandidate>>(emptyList()) }
    var showHelp by remember { mutableStateOf(false) }
    var showPromoteConfirm by remember { mutableStateOf(false) }
    var showDemoteConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var analysisNotice by remember { mutableStateOf<Pair<String, String>?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var metadata by remember(clipId) { mutableStateOf(SwingVideoMetadata()) }
    var selectedScrubFeel by remember { mutableStateOf(SwingReplayScrubFeel.Precise) }
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
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            playWhenReady = true
            prepare()
        }
    }
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
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(player) {
        while (true) {
            currentTimeSec = (player.currentPosition / 1000f).coerceAtLeast(0f)
            val dur = player.duration
            if (dur > 0) durationSec = dur / 1000f
            isPlaying = player.isPlaying
            delay(80)
        }
    }
    LaunchedEffect(uri) {
        metadata = loadSwingVideoMetadata(context, uri)
    }
    LaunchedEffect(uri) {
        isDetectingImpact = true
        val candidates = AudioImpactDetector.detect(context, uri).take(3)
        impactCandidates = candidates
        showImpactMarkers = candidates.isNotEmpty()
        isDetectingImpact = false
    }
    LaunchedEffect(playbackRate) {
        player.playbackParameters = PlaybackParameters(playbackRate)
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(1_800)
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
            )
            SwingScrubFeelControl(
                selected = selectedScrubFeel,
                onSelected = { selectedScrubFeel = it },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 4.dp),
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
                if (showDrawingTools) {
                    V2DrawingOverlay(state = drawingState, modifier = Modifier.fillMaxSize())
                    V2DrawingToolsPanel(
                        state = drawingState,
                        onClose = {
                            showDrawingTools = false
                            drawingState.clearSelection()
                        },
	                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
	                    )
	                }
                if (showImpactMarkers && impactCandidates.isNotEmpty()) {
                    ImpactMarkerOverlay(
                        candidates = impactCandidates,
                        onSelect = { candidate ->
                            player.pause()
                            val targetMs = (candidate.timeSec * 1000.0).roundToInt().toLong()
                            player.seekTo(targetMs.coerceIn(0L, player.duration.coerceAtLeast(0L)))
                            currentTimeSec = candidate.timeSec.toFloat().coerceAtLeast(0f)
                            isPlaying = false
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 10.dp, top = 10.dp),
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
                scrubFeel = selectedScrubFeel,
                showDetails = showDetails,
                showAnalysisActions = showAnalysisActions,
                showDrawingTools = showDrawingTools,
                impactCandidates = impactCandidates,
                isDetectingImpact = isDetectingImpact,
                showImpactMarkers = showImpactMarkers,
                onTogglePlay = {
                    if (player.isPlaying) player.pause() else player.play()
                },
                onStepFrames = { deltaFrames ->
                    player.pause()
                    val deltaMs = (deltaFrames * metadata.frameDurationMs).roundToInt().toLong()
                    player.seekTo((player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L)))
                },
                onSeekToFrame = { frameIndex ->
                    player.pause()
                    val clampedFrame = frameIndex.coerceIn(0, metadata.totalFrames(durationSec).coerceAtLeast(1) - 1)
                    val targetMs = (clampedFrame * metadata.frameDurationMs).roundToInt().toLong()
                    player.seekTo(targetMs.coerceIn(0L, player.duration.coerceAtLeast(0L)))
                    currentTimeSec = targetMs / 1000f
                    isPlaying = false
                },
                onSeek = { progress ->
                    val target = (durationSec * progress * 1000).roundToInt().toLong()
                    player.seekTo(target)
                },
                onRate = { playbackRate = it },
                onToggleImpactMarkers = {
                    if (impactCandidates.isNotEmpty()) showImpactMarkers = !showImpactMarkers
                },
                onSeekToImpact = { candidate ->
                    player.pause()
                    val targetMs = (candidate.timeSec * 1000.0).roundToInt().toLong()
                    player.seekTo(targetMs.coerceIn(0L, player.duration.coerceAtLeast(0L)))
                    currentTimeSec = candidate.timeSec.toFloat().coerceAtLeast(0f)
                    isPlaying = false
                    showImpactMarkers = true
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
                onToggleAnalysis = { showAnalysisActions = !showAnalysisActions },
                onToggleDrawing = {
                    showDrawingTools = !showDrawingTools
                    if (!showDrawingTools) {
                        drawingState.clearSelection()
                    } else {
                        player.pause()
                    }
                },
                onOpenDrawing = {
                    showAnalysisActions = false
                    showDrawingTools = true
                    player.pause()
                },
                onShowPosturePlan = {
                    analysisNotice = "姿态分析替代方案" to
                        "Android 版已接入音频击球帧候选。当前可先用「击球帧」快速定位击球时刻，再用画线逐帧分析检查站位、脊柱角度、杆面和挥杆平面。"
                },
                onShowWatchPlan = {
                    analysisNotice = "穿戴联动说明" to
                        "Android 穿戴设备节奏和远程触发需要单独适配。当前挥杆播放器已支持帧步进、慢放、画线和成长池，适合先完成上线版回看分析。"
                },
                onCreateMemo = {
                    scope.launch {
                        val memo = buildSwingReviewMemo(
                            clip = clip,
                            durationSec = if (durationSec > 0f) durationSec.toDouble() else clip.durationSec,
                            metadata = metadata,
                        )
                        V2MemoStore.createAndWait(memo)
                        toast = "已创建复盘 Memo"
                        onMemoCreated(memo.id)
                    }
                },
                onDelete = { showDeleteConfirm = true },
            )
        }
        toast?.let {
            Text(
                it,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 76.dp)
                    .clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除这段挥杆？") },
            text = { Text("将删除视频文件和缩略图，此操作不可撤销。") },
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
        HelpDialog(onDismiss = { showHelp = false })
    }

    analysisNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { analysisNotice = null },
            title = { Text(notice.first) },
            text = { Text(notice.second) },
            confirmButton = {
                TextButton(onClick = { analysisNotice = null }) { Text("知道了") }
            },
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
private fun DetailTopBar(
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Text("返回", color = Color.White, fontSize = 16.sp)
        }
        Text("挥杆详情", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        IconButton(
            onClick = onHelp,
            modifier = Modifier.align(Alignment.CenterEnd).size(40.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = "功能说明",
                tint = Color.White.copy(alpha = 0.70f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ControlsPanel(
    clip: V2SwingClip,
    isPlaying: Boolean,
    currentTime: Float,
    duration: Float,
    metadata: SwingVideoMetadata,
    playbackRate: Float,
    scrubFeel: SwingReplayScrubFeel,
    showDetails: Boolean,
    showAnalysisActions: Boolean,
    showDrawingTools: Boolean,
    impactCandidates: List<ImpactCandidate>,
    isDetectingImpact: Boolean,
    showImpactMarkers: Boolean,
    onTogglePlay: () -> Unit,
    onStepFrames: (Int) -> Unit,
    onSeekToFrame: (Int) -> Unit,
    onSeek: (Float) -> Unit,
    onRate: (Float) -> Unit,
    onToggleImpactMarkers: () -> Unit,
    onSeekToImpact: (ImpactCandidate) -> Unit,
    onFavorite: () -> Unit,
    onPromote: () -> Unit,
    onSave: () -> Unit,
    onInfo: () -> Unit,
    onToggleAnalysis: () -> Unit,
    onToggleDrawing: () -> Unit,
    onOpenDrawing: () -> Unit,
    onShowPosturePlan: () -> Unit,
    onShowWatchPlan: () -> Unit,
    onCreateMemo: () -> Unit,
    onDelete: () -> Unit,
) {
    val totalFrames = metadata.totalFrames(duration)
    val currentFrame = metadata.currentFrame(currentTime, duration)
    Column(
        Modifier.fillMaxWidth().background(Color.Black).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${"%.1f".format(currentTime)}s", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.weight(1f))
            Text("帧 $currentFrame / $totalFrames", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.weight(1f))
            Text("${"%.1f".format(duration)}s", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(0.1f, 0.25f, 0.5f, 1f).forEach { rate ->
                val isActive = playbackRate == rate
                Text(
                    swingRateText(rate),
                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                        .background(if (isActive) DSColors.fairway.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.10f))
                        .clickable { onRate(rate) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            ImpactChip(
                candidates = impactCandidates,
                detecting = isDetectingImpact,
                active = showImpactMarkers,
                onToggle = onToggleImpactMarkers,
                onPrimaryCandidate = {
                    impactCandidates.firstOrNull()?.let(onSeekToImpact)
                },
            )
            DrawingChip(active = showDrawingTools, onClick = onToggleDrawing)
        }
        SwingFrameScrubBar(
            currentFrame = currentFrame,
            totalFrames = totalFrames,
            duration = duration,
            impactCandidates = impactCandidates,
            showImpactMarkers = showImpactMarkers,
            scrubFeel = scrubFeel,
            onSeekToFrame = onSeekToFrame,
        )
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.10f)))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
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
                BottomAction(
                    if (isPromoted) Icons.Filled.CheckCircle else Icons.Filled.ArrowCircleUp,
                    "成长池",
                    clip.retention == V2SwingClipRetention.PROMOTED,
                    activeColor = DSColors.fairway,
                    onClick = onPromote,
                )
                Spacer(Modifier.weight(1f))
                BottomAction(
                    Icons.Filled.Edit,
                    "分析",
                    showAnalysisActions,
                    activeColor = DSColors.fairway,
                    onClick = onToggleAnalysis,
                )
                BottomAction(Icons.Filled.Download, "保存", false, style = BottomActionStyle.Plain, onClick = onSave)
                BottomAction(Icons.Filled.Info, "详情", showDetails, style = BottomActionStyle.Plain, activeColor = DSColors.fairway, onClick = onInfo)
                BottomAction(Icons.Filled.DeleteOutline, "删除", false, style = BottomActionStyle.Plain, onClick = onDelete, danger = true)
            }
        }
        if (showAnalysisActions) {
            AnalysisActionsPanel(
                metadata = metadata,
                totalFrames = totalFrames,
                onCreateMemo = onCreateMemo,
                onOpenDrawing = onOpenDrawing,
                onShowDetails = onInfo,
                onShowPosturePlan = onShowPosturePlan,
                onShowWatchPlan = onShowWatchPlan,
            )
        }
        if (showDetails) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DetailRow("时间", clip.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA)))
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

@Composable
private fun SwingScrubFeelControl(
    selected: SwingReplayScrubFeel,
    onSelected: (SwingReplayScrubFeel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("手感", color = Color.White.copy(alpha = 0.68f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        SwingReplayScrubFeel.entries.forEach { feel ->
            Text(
                text = feel.title,
                color = if (selected == feel) Color.Black else Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .size(width = 34.dp, height = 26.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected == feel) Color.White else Color.White.copy(alpha = 0.14f))
                    .clickable { onSelected(feel) }
                    .padding(top = 5.dp),
            )
        }
    }
}

@Composable
private fun SwingFrameScrubBar(
    currentFrame: Int,
    totalFrames: Int,
    duration: Float,
    impactCandidates: List<ImpactCandidate>,
    showImpactMarkers: Boolean,
    scrubFeel: SwingReplayScrubFeel,
    onSeekToFrame: (Int) -> Unit,
) {
    val safeTotal = totalFrames.coerceAtLeast(1)
    val progress = (currentFrame / safeTotal.toFloat()).coerceIn(0f, 1f)
    var startFrame by remember { mutableIntStateOf(currentFrame) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(currentFrame, totalFrames, scrubFeel) {
                var accumulatedX = 0f
                detectDragGestures(
                    onDragStart = {
                        startFrame = currentFrame
                        accumulatedX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        accumulatedX += dragAmount.x
                        val frameDelta = (accumulatedX / scrubFeel.pointsPerFrame).roundToInt()
                        onSeekToFrame(startFrame + frameDelta)
                        change.consume()
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.15f)))
        Box(
            Modifier
                .fillMaxWidth(progress)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.60f)),
        )
        if (showImpactMarkers && duration > 0f) {
            impactCandidates.forEach { candidate ->
                val markerProgress = (candidate.timeSec.toFloat() / duration).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .offset(x = maxWidth * markerProgress - 3.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(DSColors.trophyGold),
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth(progress.coerceAtLeast(0.001f)),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                Modifier
                    .size(16.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

@Composable
private fun AnalysisActionsPanel(
    metadata: SwingVideoMetadata,
    totalFrames: Int,
    onCreateMemo: () -> Unit,
    onOpenDrawing: () -> Unit,
    onShowDetails: () -> Unit,
    onShowPosturePlan: () -> Unit,
    onShowWatchPlan: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("回看分析", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        AnalysisActionRow(
            title = "生成复盘 Memo",
            body = "带入这段视频、帧率和复盘提示，方便继续补充训练观察。",
            value = "可用",
            onClick = onCreateMemo,
        )
        AnalysisActionRow(
            title = "画线逐帧分析",
            body = "进入标注工具，暂停视频后检查站位、杆面和挥杆平面。",
            value = "可用",
            onClick = onOpenDrawing,
        )
        AnalysisActionRow(
            title = "技术元数据",
            body = "${metadata.fpsLabel} · $totalFrames 帧 · ${metadata.fileSizeLabel}",
            value = "详情",
            onClick = onShowDetails,
        )
        AnalysisActionRow(
            title = "姿态参考",
            body = "用击球帧候选和画线逐帧，完成上线版姿态复盘。",
            value = "替代方案",
            onClick = onShowPosturePlan,
        )
        AnalysisActionRow(
            title = "穿戴联动",
            body = "查看 Android 版当前可用的手机录制与回看分析路径。",
            value = "说明",
            onClick = onShowWatchPlan,
        )
    }
}

@Composable
private fun ImpactMarkerOverlay(
    candidates: List<ImpactCandidate>,
    onSelect: (ImpactCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        candidates.forEachIndexed { index, candidate ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.60f))
                    .clickable { onSelect(candidate) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = DSColors.trophyGold,
                    modifier = Modifier.size(10.dp),
                )
                Text(
                    "击球${index + 1}",
                    color = DSColors.trophyGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ImpactChip(
    candidates: List<ImpactCandidate>,
    detecting: Boolean,
    active: Boolean,
    onToggle: () -> Unit,
    onPrimaryCandidate: () -> Unit,
) {
    when {
        detecting -> {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = DSColors.trophyGold,
                    strokeWidth = 2.dp,
                )
            }
        }

        candidates.isNotEmpty() -> {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) DSColors.trophyGold.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.15f))
                        .clickable(onClick = onToggle)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = if (active) DSColors.trophyGold else Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        "击球帧",
                        color = if (active) DSColors.trophyGold else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    "跳转",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(onClick = onPrimaryCandidate)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun DrawingChip(active: Boolean, onClick: () -> Unit) {
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

@Composable
private fun AnalysisActionRow(
    title: String,
    body: String,
    value: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = if (enabled) 0.07f else 0.035f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                color = Color.White.copy(alpha = if (enabled) 0.94f else 0.42f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                body,
                color = Color.White.copy(alpha = if (enabled) 0.56f else 0.32f),
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
        Text(
            value,
            color = if (enabled) DSColors.trophyGold else Color.White.copy(alpha = 0.34f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = if (enabled) 0.08f else 0.04f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("功能说明", color = DSColors.inkGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("完成", color = DSColors.fairway, fontWeight = FontWeight.SemiBold)
                }
            }
            HelpRow(
                icon = Icons.Filled.PlayCircle,
                iconColor = Color(0xFF111827),
                title = "播放与逐帧",
                body = "中间按钮播放/暂停，左右箭头逐帧前进后退（±1/±3/±10 帧），底部速率按钮可直接切换 .1x 到 1x 慢放。",
            )
            HelpRow(
                icon = Icons.Filled.Edit,
                iconColor = DSColors.fairway,
                title = "绘制工具",
                body = "在视频画面上画线标注挥杆姿态，支持直线、自由画笔和颜色选择，方便分析动作细节。",
            )
            HelpRow(
                icon = Icons.Filled.Bolt,
                iconColor = DSColors.trophyGold,
                title = "击球帧检测",
                body = "自动通过音频分析检测击球时刻，检测到后可点击「击球帧」按钮在画面左侧显示候选帧并一键跳转。",
            )
            HelpRow(
                icon = Icons.Filled.Star,
                iconColor = DSColors.trophyGold,
                title = "收藏",
                body = "点击底部星标按钮收藏视频，收藏后永久保留，不会被自动清理。",
            )
            HelpRow(
                icon = Icons.Filled.ArrowCircleUp,
                iconColor = DSColors.fairway,
                title = "存入成长池",
                body = "将视频同步到成长池（视频备忘录），可在首页成长池中查看和管理。",
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
private fun StepButton(icon: ImageVector, label: String, onClick: () -> Unit) {
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

private fun buildSwingReviewMemo(
    clip: V2SwingClip,
    durationSec: Double,
    metadata: SwingVideoMetadata,
): V2Memo {
    val title = "挥杆回看 · ${clip.timestamp.format(DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA))}"
    val body = buildString {
        append("来自挥杆回看。")
        append("先标记站位、上杆顶点、击球瞬间和收杆，再补充本次训练目标与下一次改进动作。")
        append("\n\n")
        append("技术信息：${durationShort(durationSec)} · ${metadata.fpsLabel}")
        if (metadata.fileSizeLabel != "—") append(" · ${metadata.fileSizeLabel}")
        clip.sourceLabel?.takeIf { it.isNotBlank() }?.let { append("\n来源：$it") }
    }
    return V2Memo.mixedMemo(
        displayTitle = title,
        body = body,
        videoUrl = clip.videoPath,
        durationSec = durationSec,
        topic = V2Topic.SWING,
        tags = listOf("挥杆回看", "逐帧分析"),
        metaText = V2MemoBridge.relativeMetaText(clip.timestamp, "挥杆回看 · ${metadata.fpsLabel}"),
        thumbnailKind = V2MemoThumbnailKind.FAIRWAY,
    ).copy(
        isNew = true,
        accent = V2MemoAccent.REFLECTION,
        trainingMethod = "挥杆回看",
    )
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

private data class SwingVideoMetadata(
    val fps: Float = 30f,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
) {
    val frameDurationMs: Float get() = 1000f / fps.coerceAtLeast(1f)

    val fpsLabel: String get() =
        if (fps > 0f) "${"%.0f".format(fps)} fps" else "—"

    val fileSizeLabel: String get() = when {
        fileSizeBytes <= 0L -> "—"
        fileSizeBytes < 1024 * 1024 -> "${fileSizeBytes / 1024} KB"
        else -> "%.1f MB".format(fileSizeBytes / (1024.0 * 1024.0))
    }

    fun totalFrames(durationSec: Float): Int {
        val effectiveDuration = if (durationMs > 0L) durationMs / 1000f else durationSec
        return (effectiveDuration * fps).roundToInt().coerceAtLeast(1)
    }

    fun currentFrame(currentTimeSec: Float, durationSec: Float): Int =
        (currentTimeSec * fps).roundToInt().plus(1).coerceIn(1, totalFrames(durationSec))
}

private suspend fun loadSwingVideoMetadata(context: Context, uri: Uri): SwingVideoMetadata = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    runCatching {
        retriever.setDataSource(context, uri)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val fps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            ?.toFloatOrNull()
            ?.takeIf { it > 0f }
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

private fun durationShort(sec: Double): String {
    val t = sec.toInt().coerceAtLeast(0)
    return if (t < 60) "${t}s" else "%d:%02d".format(t / 60, t % 60)
}

private fun swingDetailDurationLabel(sec: Double): String = "%.1f 秒".format(Locale.CHINA, sec.coerceAtLeast(0.0))

private fun swingRateText(rate: Float): String = when (rate) {
    1f -> "1x"
    0.1f -> ".1x"
    0.25f -> ".25x"
    0.5f -> ".5x"
    else -> "%.1fx".format(Locale.US, rate)
}
