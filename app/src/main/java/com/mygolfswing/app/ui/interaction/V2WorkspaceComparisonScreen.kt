package com.mygolfswing.app.ui.interaction

import androidx.annotation.OptIn
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.V2DrawingOverlay
import com.mygolfswing.app.ui.common.V2DrawingQuickActions
import com.mygolfswing.app.ui.common.V2DrawingState
import com.mygolfswing.app.ui.common.V2DrawingToolsPanel
import com.mygolfswing.app.ui.common.v2FullScreenInsets
import com.mygolfswing.app.ui.designsystem.DSColors
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

internal val V2_COMPARISON_RATES = listOf(0.25f, 0.5f, 1f, 1.5f, 2f)

internal fun nextWorkspaceComparisonRate(current: Float): Float {
    val index = V2_COMPARISON_RATES.indexOfFirst { it == current }.takeIf { it >= 0 } ?: 2
    return V2_COMPARISON_RATES[(index + 1) % V2_COMPARISON_RATES.size]
}

internal fun linkedComparisonTargetMs(sourceMs: Long, offsetMs: Long, durationMs: Long): Long =
    (sourceMs + offsetMs).coerceIn(0L, durationMs.coerceAtLeast(0L))

@OptIn(UnstableApi::class)
@Composable
internal fun V2WorkspaceComparisonScreen(
    topFile: File,
    topTitle: String,
    bottomFile: File,
    bottomTitle: String,
    preparingReplacement: Boolean = false,
    recordingPhase: V2AnalysisRecordingPhase? = null,
    recordingUsesMicrophone: Boolean = true,
    recordingElapsedText: String = "00:00",
    onRequestRecording: () -> Unit = {},
    onPauseRecording: () -> Unit = {},
    onResumeRecording: () -> Unit = {},
    onFinishRecording: () -> Unit = {},
    onCancelRecording: () -> Unit = {},
    onClose: () -> Unit,
    onReplace: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topPlayer = remember(topFile.absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(topFile.toURI().toString()))
            prepare()
        }
    }
    val bottomPlayer = remember(bottomFile.absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(bottomFile.toURI().toString()))
            prepare()
        }
    }
    var topDurationMs by remember(topFile.absolutePath) { mutableLongStateOf(1L) }
    var bottomDurationMs by remember(bottomFile.absolutePath) { mutableLongStateOf(1L) }
    var topPositionMs by remember(topFile.absolutePath) { mutableLongStateOf(0L) }
    var bottomPositionMs by remember(bottomFile.absolutePath) { mutableLongStateOf(0L) }
    var topPlaying by remember { mutableStateOf(false) }
    var bottomPlaying by remember { mutableStateOf(false) }
    var topRate by remember { mutableFloatStateOf(1f) }
    var bottomRate by remember { mutableFloatStateOf(1f) }
    var linked by remember { mutableStateOf(false) }
    var linkOffsetMs by remember { mutableLongStateOf(0L) }
    var overlayMode by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var drawingVisible by remember { mutableStateOf(false) }
    var topSliderMs by remember { mutableFloatStateOf(0f) }
    var bottomSliderMs by remember { mutableFloatStateOf(0f) }
    var topScrubbing by remember { mutableStateOf(false) }
    var bottomScrubbing by remember { mutableStateOf(false) }
    var playbackError by remember(topFile.absolutePath, bottomFile.absolutePath) { mutableStateOf<String?>(null) }
    var comparisonNotice by remember(topFile.absolutePath, bottomFile.absolutePath) { mutableStateOf<String?>(null) }
    var savingFrame by remember(topFile.absolutePath) { mutableStateOf(false) }
    var showDiscardRecordingConfirm by remember { mutableStateOf(false) }
    val drawingState = remember(topFile.absolutePath, bottomFile.absolutePath) { V2DrawingState() }
    val recordingActive = recordingPhase in setOf(
        V2AnalysisRecordingPhase.STARTING,
        V2AnalysisRecordingPhase.RECORDING,
        V2AnalysisRecordingPhase.PAUSED,
    )

    fun requestClose() {
        if (recordingActive) showDiscardRecordingConfirm = true else onClose()
    }

    fun captureTopFrame() {
        if (savingFrame) return
        savingFrame = true
        comparisonNotice = "正在保存截图…"
        scope.launch {
            comparisonNotice = saveWorkspaceAnalysisFrameToGallery(context, topFile, topPositionMs)
            savingFrame = false
        }
    }

    fun seekTop(targetMs: Long, exact: Boolean) {
        val safe = targetMs.coerceIn(0L, topDurationMs)
        topPlayer.setSeekParameters(if (exact) SeekParameters.EXACT else SeekParameters(50_000L, 50_000L))
        topPlayer.seekTo(safe)
        topPositionMs = safe
        topSliderMs = safe.toFloat()
        if (linked) {
            val bottomTarget = linkedComparisonTargetMs(safe, linkOffsetMs, bottomDurationMs)
            bottomPlayer.setSeekParameters(if (exact) SeekParameters.EXACT else SeekParameters(50_000L, 50_000L))
            bottomPlayer.seekTo(bottomTarget)
            bottomPositionMs = bottomTarget
            bottomSliderMs = bottomTarget.toFloat()
        }
    }

    fun seekBottom(targetMs: Long, exact: Boolean) {
        val safe = targetMs.coerceIn(0L, bottomDurationMs)
        bottomPlayer.setSeekParameters(if (exact) SeekParameters.EXACT else SeekParameters(50_000L, 50_000L))
        bottomPlayer.seekTo(safe)
        bottomPositionMs = safe
        bottomSliderMs = safe.toFloat()
        if (linked) {
            val topTarget = linkedComparisonTargetMs(safe, -linkOffsetMs, topDurationMs)
            topPlayer.setSeekParameters(if (exact) SeekParameters.EXACT else SeekParameters(50_000L, 50_000L))
            topPlayer.seekTo(topTarget)
            topPositionMs = topTarget
            topSliderMs = topTarget.toFloat()
        }
    }

    fun setLinkedPlayback(playing: Boolean) {
        topPlaying = playing
        bottomPlaying = playing
        if (playing) {
            topPlayer.play()
            bottomPlayer.play()
        } else {
            topPlayer.pause()
            bottomPlayer.pause()
        }
    }

    V2BackHandler(onBack = ::requestClose)

    DisposableEffect(topPlayer, bottomPlayer) {
        val topListener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) { topPlaying = value }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    topPlaying = false
                    if (linked) setLinkedPlayback(false)
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                playbackError = "上方视频暂时无法播放，请重新选择"
                topPlaying = false
            }
        }
        val bottomListener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) { bottomPlaying = value }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    bottomPlaying = false
                    if (linked) setLinkedPlayback(false)
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                playbackError = "对比视频暂时无法播放，请重新选择"
                bottomPlaying = false
            }
        }
        topPlayer.addListener(topListener)
        bottomPlayer.addListener(bottomListener)
        onDispose {
            topPlayer.removeListener(topListener)
            bottomPlayer.removeListener(bottomListener)
            topPlayer.release()
            bottomPlayer.release()
        }
    }

    LaunchedEffect(topPlayer, bottomPlayer) {
        while (true) {
            topPlayer.duration.takeIf { it > 0L }?.let { topDurationMs = it }
            bottomPlayer.duration.takeIf { it > 0L }?.let { bottomDurationMs = it }
            if (!topScrubbing) {
                topPositionMs = topPlayer.currentPosition.coerceAtLeast(0L)
                topSliderMs = topPositionMs.toFloat()
            }
            if (!bottomScrubbing) {
                bottomPositionMs = bottomPlayer.currentPosition.coerceAtLeast(0L)
                bottomSliderMs = bottomPositionMs.toFloat()
            }
            delay(80)
        }
    }
    LaunchedEffect(topRate, linked) {
        topPlayer.playbackParameters = PlaybackParameters(topRate)
        if (linked) {
            bottomRate = topRate
            bottomPlayer.playbackParameters = PlaybackParameters(topRate)
        }
    }
    LaunchedEffect(bottomRate, linked) {
        if (!linked) bottomPlayer.playbackParameters = PlaybackParameters(bottomRate)
    }
    LaunchedEffect(muted) {
        val volume = if (muted) 0f else 1f
        topPlayer.volume = volume
        bottomPlayer.volume = volume
    }
    LaunchedEffect(comparisonNotice) {
        if (comparisonNotice != null && !savingFrame) {
            delay(1_800)
            comparisonNotice = null
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black).v2FullScreenInsets()) {
        Column(Modifier.fillMaxSize()) {
            WorkspaceComparisonTopBar(
                overlayMode = overlayMode,
                recordingPhase = recordingPhase,
                recordingUsesMicrophone = recordingUsesMicrophone,
                recordingElapsedText = recordingElapsedText,
                savingFrame = savingFrame,
                onClose = ::requestClose,
                onCaptureFrame = ::captureTopFrame,
                onRequestRecording = onRequestRecording,
                onPauseRecording = onPauseRecording,
                onResumeRecording = onResumeRecording,
                onFinishRecording = onFinishRecording,
                onToggleOverlay = {
                    overlayMode = !overlayMode
                    if (overlayMode) {
                        linkOffsetMs = bottomPositionMs - topPositionMs
                        bottomRate = topRate
                        linked = true
                        val shouldPlay = topPlaying || bottomPlaying
                        setLinkedPlayback(shouldPlay)
                        comparisonNotice = "已叠在一起。点左上角关闭对比"
                    } else {
                        comparisonNotice = "已恢复上下对比"
                    }
                },
                onReplace = {
                    setLinkedPlayback(false)
                    onReplace()
                },
            )

            Box(Modifier.fillMaxWidth().weight(1f)) {
                if (overlayMode) {
                    Box(Modifier.fillMaxSize()) {
                        WorkspaceComparisonVideoPane(
                            player = topPlayer,
                            label = "当前",
                            title = topTitle,
                            modifier = Modifier.fillMaxSize().alpha(0.58f),
                        )
                        WorkspaceComparisonVideoPane(
                            player = bottomPlayer,
                            label = "对比",
                            title = bottomTitle,
                            modifier = Modifier.fillMaxSize(),
                            headerAlignment = Alignment.BottomStart,
                        )
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        WorkspaceComparisonVideoPane(
                            player = topPlayer,
                            label = "当前",
                            title = topTitle,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                        Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.13f)))
                        WorkspaceComparisonVideoPane(
                            player = bottomPlayer,
                            label = "对比",
                            title = bottomTitle,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                    }
                }

                V2DrawingOverlay(
                    state = drawingState,
                    modifier = Modifier.fillMaxSize(),
                    interactionEnabled = drawingVisible,
                )
                if (drawingState.strokes.isNotEmpty() && !drawingVisible) {
                    V2DrawingQuickActions(
                        state = drawingState,
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                    )
                }
                if (drawingVisible) {
                    V2DrawingToolsPanel(
                        state = drawingState,
                        onClose = { drawingVisible = false; drawingState.clearSelection() },
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 62.dp, end = 8.dp),
                    )
                }
                if (!recordingActive) {
                    Column(
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        WorkspaceComparisonCircleButton(
                            icon = Icons.Filled.Edit,
                            description = if (drawingVisible) "关闭绘图工具" else "打开绘图工具",
                            active = drawingVisible,
                        ) { drawingVisible = !drawingVisible }
                        WorkspaceComparisonCircleButton(
                            icon = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            description = if (muted) "打开声音" else "静音",
                            active = muted,
                        ) { muted = !muted }
                    }
                }
            }

            WorkspaceComparisonControls(
                topPositionMs = topSliderMs,
                topDurationMs = topDurationMs,
                bottomPositionMs = bottomSliderMs,
                bottomDurationMs = bottomDurationMs,
                topPlaying = topPlaying,
                bottomPlaying = bottomPlaying,
                topRate = topRate,
                bottomRate = bottomRate,
                linked = linked,
                overlayMode = overlayMode,
                onTopSeek = { value ->
                    topScrubbing = true
                    topPlayer.pause()
                    if (linked) bottomPlayer.pause()
                    seekTop(value.roundToLong(), exact = false)
                },
                onTopSeekFinished = {
                    seekTop(topSliderMs.roundToLong(), exact = true)
                    topScrubbing = false
                },
                onBottomSeek = { value ->
                    bottomScrubbing = true
                    bottomPlayer.pause()
                    if (linked) topPlayer.pause()
                    seekBottom(value.roundToLong(), exact = false)
                },
                onBottomSeekFinished = {
                    seekBottom(bottomSliderMs.roundToLong(), exact = true)
                    bottomScrubbing = false
                },
                onToggleLink = {
                    linked = !linked
                    if (linked) {
                        linkOffsetMs = bottomPositionMs - topPositionMs
                        bottomRate = topRate
                        val shouldPlay = topPlaying || bottomPlaying
                        setLinkedPlayback(shouldPlay)
                    } else {
                        linkOffsetMs = 0L
                    }
                },
                onToggleTopPlay = {
                    if (topPlaying) topPlayer.pause() else topPlayer.play()
                },
                onToggleBottomPlay = {
                    if (bottomPlaying) bottomPlayer.pause() else bottomPlayer.play()
                },
                onToggleLinkedPlay = { setLinkedPlayback(!(topPlaying || bottomPlaying)) },
                onCycleTopRate = { topRate = nextWorkspaceComparisonRate(topRate) },
                onCycleBottomRate = { bottomRate = nextWorkspaceComparisonRate(bottomRate) },
            )
        }

        playbackError?.let { message ->
            Text(
                message,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 62.dp)
                    .clip(CircleShape).background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        comparisonNotice?.let { message ->
            Text(
                message,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 62.dp)
                    .clip(CircleShape).background(Color.Black.copy(alpha = .78f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        if (preparingReplacement) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = .62f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    Text("正在处理对比视频…", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showDiscardRecordingConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardRecordingConfirm = false },
            title = { Text("正在录屏") },
            text = { Text("当前录屏尚未完成，返回会放弃这段录屏。") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardRecordingConfirm = false
                    onCancelRecording()
                    onClose()
                }) { Text("放弃录屏并返回", color = Color(0xFFD74A43), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardRecordingConfirm = false }) { Text("继续录制") }
            },
        )
    }
}

@Composable
private fun WorkspaceComparisonTopBar(
    overlayMode: Boolean,
    recordingPhase: V2AnalysisRecordingPhase?,
    recordingUsesMicrophone: Boolean,
    recordingElapsedText: String,
    savingFrame: Boolean,
    onClose: () -> Unit,
    onCaptureFrame: () -> Unit,
    onRequestRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onFinishRecording: () -> Unit,
    onToggleOverlay: () -> Unit,
    onReplace: () -> Unit,
) {
    val recordingActive = recordingPhase in setOf(
        V2AnalysisRecordingPhase.STARTING,
        V2AnalysisRecordingPhase.RECORDING,
        V2AnalysisRecordingPhase.PAUSED,
    )
    Row(
        Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        WorkspaceComparisonCircleButton(
            Icons.Filled.Close,
            if (recordingActive) "取消录屏并返回" else "关闭对比",
            onClick = onClose,
        )
        if (recordingActive) {
            WorkspaceComparisonCircleButton(Icons.Filled.StopCircle, "停止录屏", onClick = onFinishRecording)
            WorkspaceComparisonCircleButton(
                if (recordingPhase == V2AnalysisRecordingPhase.PAUSED) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                if (recordingPhase == V2AnalysisRecordingPhase.PAUSED) "继续录制" else "暂停录制",
                active = recordingPhase == V2AnalysisRecordingPhase.PAUSED,
                onClick = if (recordingPhase == V2AnalysisRecordingPhase.PAUSED) onResumeRecording else onPauseRecording,
            )
            Column(
                modifier = Modifier.width(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (recordingPhase == V2AnalysisRecordingPhase.STARTING) "准备中" else if (recordingUsesMicrophone) "● 口播" else "● 录屏",
                    color = Color(0xFFFF6B63),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                if (recordingPhase != V2AnalysisRecordingPhase.STARTING) {
                    Text(
                        recordingElapsedText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
            WorkspaceComparisonVsButton(overlayMode = overlayMode, enabled = false, onClick = onToggleOverlay)
            WorkspaceComparisonCircleButton(Icons.Filled.VideoLibrary, "录屏时不能更换视频", enabled = false, onClick = onReplace)
        } else {
            WorkspaceComparisonCircleButton(
                Icons.Filled.CameraAlt,
                if (savingFrame) "正在保存截图" else "保存上方当前帧截图",
                enabled = !savingFrame,
                onClick = onCaptureFrame,
            )
            WorkspaceComparisonCircleButton(Icons.Filled.Mic, "开始屏幕录制", onClick = onRequestRecording)
            WorkspaceComparisonVsButton(overlayMode = overlayMode, enabled = true, onClick = onToggleOverlay)
            WorkspaceComparisonCircleButton(Icons.Filled.VideoLibrary, "更换对比视频", onClick = onReplace)
        }
    }
}

@Composable
private fun WorkspaceComparisonVsButton(
    overlayMode: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(width = 42.dp, height = 34.dp).clip(RoundedCornerShape(9.dp))
            .background(if (overlayMode) Color.White else Color.White.copy(alpha = 0.13f))
            .clickable(enabled = enabled, onClick = onClick).alpha(if (enabled) 1f else .35f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "VS",
            color = if (overlayMode) Color.Black else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun WorkspaceComparisonVideoPane(
    player: ExoPlayer,
    label: String,
    title: String,
    modifier: Modifier,
    headerAlignment: Alignment = Alignment.TopStart,
) {
    Box(modifier.background(Color.Black)) {
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
        Row(
            modifier = Modifier.align(headerAlignment).fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        if (headerAlignment == Alignment.TopStart) {
                            listOf(Color.Black.copy(alpha = .62f), Color.Transparent)
                        } else {
                            listOf(Color.Transparent, Color.Black.copy(alpha = .62f))
                        },
                    ),
                ).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = Color(0xFFFFD33D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(title.ifBlank { if (label == "当前") "学员视频" else "对比视频" }, color = Color.White,
                fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WorkspaceComparisonControls(
    topPositionMs: Float,
    topDurationMs: Long,
    bottomPositionMs: Float,
    bottomDurationMs: Long,
    topPlaying: Boolean,
    bottomPlaying: Boolean,
    topRate: Float,
    bottomRate: Float,
    linked: Boolean,
    overlayMode: Boolean,
    onTopSeek: (Float) -> Unit,
    onTopSeekFinished: () -> Unit,
    onBottomSeek: (Float) -> Unit,
    onBottomSeekFinished: () -> Unit,
    onToggleLink: () -> Unit,
    onToggleTopPlay: () -> Unit,
    onToggleBottomPlay: () -> Unit,
    onToggleLinkedPlay: () -> Unit,
    onCycleTopRate: () -> Unit,
    onCycleBottomRate: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = .82f), Color.Black)),
        ).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (overlayMode) {
            ComparisonSliderRow("同步", topPositionMs, topDurationMs, onTopSeek, onTopSeekFinished)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                WorkspaceComparisonPlayButton(topPlaying || bottomPlaying, "同步播放两个视频", onToggleLinkedPlay)
                Spacer(Modifier.width(14.dp))
                WorkspaceComparisonRateButton(topRate, "切换同步播放速度", onCycleTopRate)
            }
        } else {
        ComparisonSliderRow("上方", topPositionMs, topDurationMs, onTopSeek, onTopSeekFinished)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Text(
                if (linked) "同步" else "独立",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape)
                    .background(if (linked) DSColors.fairway else Color.White.copy(alpha = .13f))
                    .clickable(onClick = onToggleLink).padding(horizontal = 14.dp, vertical = 7.dp),
            )
            Spacer(Modifier.weight(1f))
        }
        ComparisonSliderRow("下方", bottomPositionMs, bottomDurationMs, onBottomSeek, onBottomSeekFinished)
        if (linked) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                WorkspaceComparisonPlayButton(topPlaying || bottomPlaying, "同步播放两个视频", onToggleLinkedPlay)
                Spacer(Modifier.width(14.dp))
                WorkspaceComparisonRateButton(topRate, "切换同步播放速度", onCycleTopRate)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WorkspaceComparisonPlayButton(topPlaying, if (topPlaying) "暂停上方" else "播放上方", onToggleTopPlay)
                WorkspaceComparisonRateButton(topRate, "切换上方速度", onCycleTopRate)
                Spacer(Modifier.weight(1f))
                WorkspaceComparisonPlayButton(bottomPlaying, if (bottomPlaying) "暂停下方" else "播放下方", onToggleBottomPlay)
                WorkspaceComparisonRateButton(bottomRate, "切换下方速度", onCycleBottomRate)
            }
        }
        }
    }
}

@Composable
private fun ComparisonSliderRow(
    label: String,
    value: Float,
    durationMs: Long,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = .68f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Slider(
            value = value.coerceIn(0f, durationMs.coerceAtLeast(1L).toFloat()),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = .25f),
            ),
            modifier = Modifier.weight(1f).height(30.dp),
        )
        Text(
            formatWorkspaceComparisonTime(value.roundToLong()),
            color = Color.White.copy(alpha = .7f),
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun WorkspaceComparisonPlayButton(playing: Boolean, description: String, onClick: () -> Unit) {
    Icon(
        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
        description,
        tint = Color.White,
        modifier = Modifier.size(46.dp).clip(CircleShape).clickable(onClick = onClick).padding(8.dp),
    )
}

@Composable
private fun WorkspaceComparisonRateButton(rate: Float, description: String, onClick: () -> Unit) {
    Text(
        text = if (rate == 1f) "1x" else "${rate}x",
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.size(width = 48.dp, height = 42.dp).clip(CircleShape)
            .clickable(onClick = onClick).padding(top = 12.dp),
    )
}

@Composable
private fun WorkspaceComparisonCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Icon(
        icon,
        description,
        tint = if (active) Color(0xFFFFD33D) else Color.White.copy(alpha = if (enabled) 1f else .35f),
        modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = .45f))
            .clickable(enabled = enabled, onClick = onClick).padding(10.dp),
    )
}

internal fun formatWorkspaceComparisonTime(timeMs: Long): String {
    val safe = timeMs.coerceAtLeast(0L)
    return "%02d.%02d".format(safe / 1_000L, (safe % 1_000L) / 10L)
}
