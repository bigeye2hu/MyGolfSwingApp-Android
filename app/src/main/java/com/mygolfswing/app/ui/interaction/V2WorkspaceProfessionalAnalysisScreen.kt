package com.mygolfswing.app.ui.interaction

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.TextureView
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.R
import com.mygolfswing.app.data.video.WorkspacePlaybackCache
import androidx.compose.ui.viewinterop.AndroidView
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.V2DrawingOverlay
import com.mygolfswing.app.ui.common.V2DrawingState
import com.mygolfswing.app.ui.common.V2DrawingTool
import com.mygolfswing.app.ui.common.V2DrawingToolsPanel
import com.mygolfswing.app.ui.common.V2SelectionArrowIcon
import com.mygolfswing.app.ui.common.v2SelectionToolTint
import com.mygolfswing.app.ui.common.v2FullScreenInsets
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.player.ReviewFrameScrubber
import com.mygolfswing.app.ui.player.SmoothFrameSeeker
import com.mygolfswing.app.ui.player.StandaloneTimelineScrubFeel
import com.mygolfswing.app.ui.player.VideoPlayerChrome
import com.mygolfswing.app.ui.player.VideoPlayerProgressBar
import com.mygolfswing.app.ui.player.VideoPlayerTool
import com.mygolfswing.app.ui.swing.V2SwingAnalysisTrimScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal enum class V2ProfessionalAnalysisMode {
    REPLY,
    STANDALONE_REVIEW,
}

internal fun standaloneSelfieClampedPosition(
    basePosition: Offset,
    proposedOffset: Offset,
    containerSize: Size,
    halfPreviewWidth: Float,
    halfPreviewHeight: Float,
    horizontalMargin: Float,
    topMargin: Float,
    bottomMargin: Float,
): Offset {
    val minimumX = halfPreviewWidth + horizontalMargin
    val maximumX = maxOf(minimumX, containerSize.width - halfPreviewWidth - horizontalMargin)
    val minimumY = halfPreviewHeight + topMargin
    val maximumY = maxOf(minimumY, containerSize.height - halfPreviewHeight - bottomMargin)
    return Offset(
        x = (basePosition.x + proposedOffset.x).coerceIn(minimumX, maximumX),
        y = (basePosition.y + proposedOffset.y).coerceIn(minimumY, maximumY),
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2WorkspaceProfessionalAnalysisScreen(
    sourceFile: File? = null,
    mediaUri: String? = null,
    title: String,
    subtitle: String? = null,
    startsWithRecordingOptions: Boolean = false,
    startsPlaying: Boolean = false,
    continuousPlayback: Boolean = false,
    mode: V2ProfessionalAnalysisMode = V2ProfessionalAnalysisMode.REPLY,
    onSwitchPlayer: (() -> Unit)? = null,
    onCancel: () -> Unit,
    onRecordingReady: (File) -> Unit,
) {
    val localFile = sourceFile?.takeIf { it.isFile && it.length() > 0L }
    val playbackUri = mediaUri?.takeIf { it.isNotBlank() } ?: localFile?.toURI()?.toString()
    if (playbackUri == null) {
        LaunchedEffect(Unit) { onCancel() }
        return
    }
    val sourceKey = localFile?.absolutePath ?: playbackUri
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recorderState by V2WorkspaceAnalysisRecorder.state.collectAsState()
    var activeRecordingId by remember(sourceKey) { mutableStateOf<String?>(null) }
    var error by remember(sourceKey) { mutableStateOf<String?>(null) }
    var drawingVisible by remember { mutableStateOf(false) }
    var playbackRate by remember { mutableFloatStateOf(PROFESSIONAL_ANALYSIS_RATES.first()) }
    var currentMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) }
    var playbackNotice by remember { mutableStateOf<String?>(null) }
    var isFlipped by remember(sourceKey) { mutableStateOf(false) }
    var showPose by remember(sourceKey) { mutableStateOf(false) }
    var includeStudentAudio by remember(sourceKey) { mutableStateOf(startsPlaying) }
    var posePoints by remember(sourceKey) { mutableStateOf(emptyList<V2PosePoint>()) }
    var poseAnalyzer by remember(sourceKey) { mutableStateOf<V2VideoPoseAnalyzer?>(null) }
    var comparisonPreparing by remember(sourceKey) { mutableStateOf(false) }
    var comparisonTrimVideo by remember(sourceKey) { mutableStateOf<V2PreparedWorkspaceVideo?>(null) }
    var comparisonVideo by remember(sourceKey) { mutableStateOf<V2PreparedWorkspaceVideo?>(null) }
    var comparisonTrimSaving by remember(sourceKey) { mutableStateOf(false) }
    var comparisonTrimError by remember(sourceKey) { mutableStateOf<String?>(null) }
    var resumeAfterComparison by remember(sourceKey) { mutableStateOf(false) }
    var showRecordingOptions by remember(sourceKey) { mutableStateOf(startsWithRecordingOptions) }
    var showOverflowMenu by remember(sourceKey) { mutableStateOf(false) }
    var showDiscardRecordingConfirm by remember(sourceKey) { mutableStateOf(false) }
    var controlsVisible by remember(sourceKey) { mutableStateOf(true) }
    var showSelfie by remember(sourceKey) { mutableStateOf(false) }
    var selfieSettledOffset by remember(sourceKey) { mutableStateOf(Offset.Zero) }
    var hasCameraPermission by remember(sourceKey) {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var pendingCaptureIncludesMicrophone by remember(sourceKey) { mutableStateOf(true) }
    var completedRecording by remember(sourceKey) { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()
    val drawingState = remember(sourceKey) { V2DrawingState() }
    val standaloneReview = mode == V2ProfessionalAnalysisMode.STANDALONE_REVIEW
    val selfiePreview = remember(sourceKey) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var playbackReady by remember(sourceKey) { mutableStateOf(false) }
    var playbackScrubbing by remember(sourceKey) { mutableStateOf(false) }
    var resumeAfterPlaybackScrub by remember(sourceKey) { mutableStateOf(false) }
    var frameMode by remember(sourceKey) { mutableStateOf(false) }
    var playbackCached by remember(sourceKey) {
        mutableStateOf(playbackUri?.let { !WorkspacePlaybackCache.isRemote(it) } ?: (localFile != null))
    }
    var videoTexture by remember(sourceKey) { mutableStateOf<TextureView?>(null) }
    val exportedPlaybackFile = remember(sourceKey) { mutableStateOf<File?>(null) }
    val showContinuousBar = continuousPlayback && !frameMode
    val player = remember(sourceKey) {
        val remote = WorkspacePlaybackCache.isRemote(playbackUri)
        val builder = if (remote) {
            ExoPlayer.Builder(context).setLoadControl(WorkspacePlaybackCache.loadControl())
        } else {
            ExoPlayer.Builder(context)
        }
        builder.build().apply {
            if (remote) {
                setMediaSource(WorkspacePlaybackCache.createMediaSource(context, playbackUri))
            } else {
                setMediaItem(MediaItem.fromUri(playbackUri))
            }
            playWhenReady = startsPlaying
            volume = if (startsPlaying) 1f else 0f
            playbackParameters = PlaybackParameters(playbackRate)
            if (!continuousPlayback) setSeekParameters(SeekParameters.EXACT)
            prepare()
        }
    }
    val frameSeeker = remember(player) { SmoothFrameSeeker(player) }
    val isRecording = activeRecordingId != null && recorderState.recordingId == activeRecordingId &&
        recorderState.phase in setOf(
            V2AnalysisRecordingPhase.STARTING,
            V2AnalysisRecordingPhase.RECORDING,
            V2AnalysisRecordingPhase.PAUSED,
        )
    var recordingClockMs by remember(sourceKey) {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }
    LaunchedEffect(isRecording, recorderState.phase, recorderState.phaseStartedElapsedRealtimeMs) {
        recordingClockMs = SystemClock.elapsedRealtime()
        while (isRecording && recorderState.phase == V2AnalysisRecordingPhase.RECORDING) {
            delay(250)
            recordingClockMs = SystemClock.elapsedRealtime()
        }
    }
    val recordingElapsedText = formatWorkspaceRecordingElapsed(
        currentWorkspaceRecordingElapsedMs(recorderState, recordingClockMs),
    )
    val latestComparisonTrimVideo = rememberUpdatedState(comparisonTrimVideo)
    val latestComparisonVideo = rememberUpdatedState(comparisonVideo)
    val latestCompletedRecording = rememberUpdatedState(completedRecording)
    val completedRecordingTransferred = remember(sourceKey) { AtomicBoolean(false) }

    fun cancelAndClose() {
        if (isRecording) V2WorkspaceAnalysisRecorder.cancel(context)
        activeRecordingId?.let(V2WorkspaceAnalysisRecorder::consume)
        activeRecordingId = null
        onCancel()
    }

    val captureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            runCatching {
                V2WorkspaceAnalysisRecorder.start(
                    context = context,
                    resultCode = result.resultCode,
                    captureData = data,
                    includeMicrophone = pendingCaptureIncludesMicrophone,
                )
            }
                .onSuccess { activeRecordingId = it; error = null }
                .onFailure { error = "无法启动录屏，请稍后重试" }
        } else {
            error = "未开始录屏，原视频和画线不会受影响"
        }
    }
    fun requestScreenCapture(includeMicrophone: Boolean) {
        pendingCaptureIncludesMicrophone = includeMicrophone
        val manager = context.getSystemService(MediaProjectionManager::class.java)
        runCatching { captureLauncher.launch(manager.createScreenCaptureIntent()) }
            .onFailure { error = "无法打开系统录屏授权，请稍后重试" }
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) requestScreenCapture(includeMicrophone = true) else error = "需要麦克风权限才能录制口播分析"
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (granted) {
            showSelfie = true
        } else {
            error = "需要相机权限才能显示教练自拍小窗"
        }
    }
    fun startRecording(includeMicrophone: Boolean) {
        showRecordingOptions = false
        error = null
        if (!includeMicrophone) {
            requestScreenCapture(includeMicrophone = false)
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            requestScreenCapture(includeMicrophone = true)
        } else {
            runCatching { microphonePermission.launch(Manifest.permission.RECORD_AUDIO) }
                .onFailure {
                    error = "无法申请麦克风权限，可以改用仅录屏"
                    showRecordingOptions = true
                }
        }
    }

    fun cancelActiveRecording() {
        val recordingId = activeRecordingId
        if (recordingId != null) {
            V2WorkspaceAnalysisRecorder.cancel(context)
            V2WorkspaceAnalysisRecorder.consume(recordingId)
        }
        activeRecordingId = null
    }

    fun toggleSelfiePreview() {
        if (showSelfie) {
            showSelfie = false
            playbackNotice = "自拍预览已关闭"
        } else if (hasCameraPermission) {
            showSelfie = true
            playbackNotice = "自拍预览已开启"
        } else {
            runCatching { cameraPermission.launch(Manifest.permission.CAMERA) }
                .onFailure { error = "无法申请相机权限，请稍后重试" }
        }
    }

    fun resumeOriginalPlayerIfNeeded() {
        if (resumeAfterComparison) player.play()
        resumeAfterComparison = false
    }

    val comparisonPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
            if (comparisonVideo == null) resumeOriginalPlayerIfNeeded()
        } else {
            comparisonPreparing = true
            comparisonTrimError = null
            scope.launch {
                try {
                    comparisonTrimVideo = prepareWorkspaceVideo(
                        context = context,
                        uri = uri,
                        uploadId = UUID.randomUUID().toString(),
                        requestId = UUID.randomUUID().toString(),
                        sourceKind = "comparison",
                    )
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    error = "无法准备对比视频，请重新选择"
                    if (comparisonVideo == null) resumeOriginalPlayerIfNeeded()
                } finally {
                    comparisonPreparing = false
                }
            }
        }
    }

    fun openComparisonPicker() {
        scope.launch {
            val file = localFile ?: run {
                if (!continuousPlayback || !playbackCached) {
                    playbackNotice = if (continuousPlayback) "待缓存完毕" else "当前视频不是本地文件，暂时不能对比"
                    return@launch
                }
                comparisonPreparing = true
                try {
                    withContext(Dispatchers.IO) { WorkspacePlaybackCache.exportFile(context, playbackUri) }
                        ?.also { exportedPlaybackFile.value = it }
                } finally {
                    comparisonPreparing = false
                }
            }
            if (file == null) {
                playbackNotice = "待缓存完毕"
                return@launch
            }
            if (comparisonVideo == null) {
                resumeAfterComparison = player.isPlaying
                player.pause()
            }
            runCatching {
                comparisonPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            }.onFailure {
                error = "无法打开视频选择器，请稍后重试"
                if (comparisonVideo == null) resumeOriginalPlayerIfNeeded()
            }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) playbackReady = true
            }

            override fun onPlayerError(playbackError: PlaybackException) {
                error = "视频暂时无法播放，请稍后重试"
            }
        }
        player.addListener(listener)
        onDispose {
            frameSeeker.dispose()
            player.removeListener(listener)
            player.release()
        }
    }
    DisposableEffect(poseAnalyzer) {
        val activeAnalyzer = poseAnalyzer
        onDispose { activeAnalyzer?.close() }
    }
    DisposableEffect(sourceKey) {
        onDispose {
            latestComparisonTrimVideo.value?.deleteTemporaryFiles()
            latestComparisonVideo.value?.deleteTemporaryFiles()
            exportedPlaybackFile.value?.delete()
            if (!completedRecordingTransferred.get()) latestCompletedRecording.value?.delete()
        }
    }
    DisposableEffect(activeRecordingId) {
        val recordingId = activeRecordingId
        onDispose {
            val state = V2WorkspaceAnalysisRecorder.state.value
            if (recordingId != null && state.recordingId == recordingId &&
                state.phase in setOf(
                    V2AnalysisRecordingPhase.STARTING,
                    V2AnalysisRecordingPhase.RECORDING,
                    V2AnalysisRecordingPhase.PAUSED,
                )
            ) {
                V2WorkspaceAnalysisRecorder.cancel(context)
            }
        }
    }
    LaunchedEffect(playbackUri) {
        val uri = playbackUri ?: return@LaunchedEffect
        if (!WorkspacePlaybackCache.isRemote(uri)) {
            playbackCached = true
            return@LaunchedEffect
        }
        while (isActive && !playbackCached) {
            playbackCached = WorkspacePlaybackCache.isFullyCached(context, uri)
            if (!playbackCached) delay(500)
        }
    }
    LaunchedEffect(frameMode) {
        if (!continuousPlayback) return@LaunchedEffect
        player.setSeekParameters(if (frameMode) SeekParameters.EXACT else SeekParameters.DEFAULT)
    }
    LaunchedEffect(player, playbackRate) {
        player.playbackParameters = PlaybackParameters(playbackRate)
    }
    LaunchedEffect(player, includeStudentAudio) {
        player.volume = if (includeStudentAudio) 1f else 0f
    }
    LaunchedEffect(playbackNotice) {
        if (playbackNotice != null) {
            delay(1_600)
            playbackNotice = null
        }
    }
    LaunchedEffect(player, sourceKey) {
        delay(16_000)
        if (!playbackReady && player.playbackState != Player.STATE_READY && error == null) {
            error = "视频暂时无法播放，请稍后重试"
            player.playWhenReady = false
        }
    }
    LaunchedEffect(player) {
        while (true) {
            if (!frameSeeker.isScrubbing && !playbackScrubbing) {
                currentMs = player.currentPosition.coerceAtLeast(0L)
            }
            player.duration.takeIf { it > 0L }?.let { durationMs = it }
            delay(80)
        }
    }
    LaunchedEffect(showPose, poseAnalyzer) {
        if (showPose) {
            poseAnalyzer?.requestPose(currentMs, force = true) { posePoints = it }
        } else {
            posePoints = emptyList()
        }
    }
    LaunchedEffect(showPose, currentMs, poseAnalyzer) {
        if (showPose) poseAnalyzer?.requestPose(currentMs) { posePoints = it }
    }
    LaunchedEffect(activeRecordingId, recorderState) {
        val recordingId = activeRecordingId ?: return@LaunchedEffect
        if (recorderState.recordingId != recordingId) return@LaunchedEffect
        when (recorderState.phase) {
            V2AnalysisRecordingPhase.FINISHED -> {
                val file = recorderState.outputPath?.let(::File)?.takeIf { it.isFile && it.length() > 0L }
                V2WorkspaceAnalysisRecorder.consume(recordingId)
                activeRecordingId = null
                if (file != null) {
                    completedRecordingTransferred.set(false)
                    completedRecording = file
                } else {
                    error = "录屏文件不可用，请重新录制"
                }
            }
            V2AnalysisRecordingPhase.FAILED -> {
                error = recorderState.error ?: "录屏失败，请重新录制"
                V2WorkspaceAnalysisRecorder.consume(recordingId)
                activeRecordingId = null
            }
            else -> Unit
        }
    }
    DisposableEffect(showSelfie, hasCameraPermission, lifecycleOwner, selfiePreview) {
        var provider: ProcessCameraProvider? = null
        if (showSelfie && hasCameraPermission) {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener(
                {
                    provider = runCatching { providerFuture.get() }.getOrNull()
                    val cameraProvider = provider
                    if (cameraProvider == null) {
                        showSelfie = false
                        error = "自拍预览暂时无法启动"
                    } else {
                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(selfiePreview.surfaceProvider)
                        }
                        runCatching {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_FRONT_CAMERA,
                                preview,
                            )
                        }.onFailure {
                            showSelfie = false
                            error = "自拍预览暂时无法启动"
                        }
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
        onDispose {
            runCatching { provider?.unbindAll() }
        }
    }

    if (showRecordingOptions) {
        ModalBottomSheet(
            onDismissRequest = { showRecordingOptions = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF101211),
            contentColor = Color.White,
        ) {
            ProfessionalRecordingModeSheetContent(
                replyFlow = !standaloneReview,
                showSelfie = showSelfie,
                onToggleSelfie = ::toggleSelfiePreview,
                onStartWithMicrophone = { startRecording(includeMicrophone = true) },
                onStartScreenOnly = { startRecording(includeMicrophone = false) },
                onCancel = { showRecordingOptions = false },
            )
        }
    }
    if (showDiscardRecordingConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardRecordingConfirm = false },
            title = { Text("正在录屏") },
            text = { Text("当前录屏尚未保存。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardRecordingConfirm = false
                        cancelActiveRecording()
                        onCancel()
                    },
                ) { Text("放弃录屏并返回", color = Color(0xFFD74A43), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardRecordingConfirm = false }) { Text("继续录制") }
            },
        )
    }
    completedRecording?.let { recording ->
        V2WorkspaceRecordingPreviewScreen(
            recording = recording,
            replyFlow = !standaloneReview,
            onDiscardAndRecordAgain = {
                recording.delete()
                completedRecording = null
                showRecordingOptions = true
            },
            onKeepInDraft = {
                completedRecordingTransferred.set(true)
                completedRecording = null
                onRecordingReady(recording)
            },
            onUse = {
                completedRecordingTransferred.set(true)
                completedRecording = null
                onRecordingReady(recording)
            },
        )
        return
    }

    comparisonTrimVideo?.let { prepared ->
        V2SwingAnalysisTrimScreen(
            imported = prepared.asVideoImport(),
            saving = comparisonTrimSaving,
            error = comparisonTrimError,
            secondaryActionTitle = "不裁剪",
            onCancel = {
                prepared.deleteTemporaryFiles()
                comparisonTrimVideo = null
                comparisonTrimError = null
                if (comparisonVideo == null) resumeOriginalPlayerIfNeeded()
            },
            onUse = { startMs, endMs ->
                if (comparisonTrimSaving) return@V2SwingAnalysisTrimScreen
                comparisonTrimSaving = true
                comparisonTrimError = null
                scope.launch {
                    try {
                        val ready = trimPreparedWorkspaceVideo(context, prepared, startMs, endMs)
                        val previous = comparisonVideo
                        comparisonVideo = ready
                        comparisonTrimVideo = null
                        if (previous != null && previous !== ready) previous.deleteTemporaryFiles()
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Throwable) {
                        comparisonTrimError = "视频裁剪失败，请重新选择或使用整段"
                    } finally {
                        comparisonTrimSaving = false
                    }
                }
            },
        )
        return
    }

    comparisonVideo?.let { prepared ->
        V2WorkspaceComparisonScreen(
            topFile = localFile ?: exportedPlaybackFile.value ?: return@let,
            topTitle = title.ifBlank { "学员视频" },
            bottomFile = prepared.file,
            bottomTitle = "对比视频",
            preparingReplacement = comparisonPreparing,
            recordingPhase = recorderState.phase.takeIf { recorderState.recordingId == activeRecordingId },
            recordingUsesMicrophone = recorderState.includeMicrophone,
            recordingElapsedText = recordingElapsedText,
            onRequestRecording = { showRecordingOptions = true },
            onPauseRecording = { V2WorkspaceAnalysisRecorder.pause(context) },
            onResumeRecording = { V2WorkspaceAnalysisRecorder.resume(context) },
            onFinishRecording = { V2WorkspaceAnalysisRecorder.finish(context) },
            onCancelRecording = ::cancelActiveRecording,
            onClose = {
                prepared.deleteTemporaryFiles()
                comparisonVideo = null
                resumeOriginalPlayerIfNeeded()
            },
            onReplace = ::openComparisonPicker,
        )
        return
    }

    val activePhase = recorderState.phase.takeIf { recorderState.recordingId == activeRecordingId }
    fun toggleFlip() {
        showOverflowMenu = false
        isFlipped = !isFlipped
        playbackNotice = if (isFlipped) "已水平翻转" else "已恢复原方向"
    }
    fun togglePose() {
        if (showPose) {
            showPose = false
            posePoints = emptyList()
            playbackNotice = "姿态识别已关闭"
            return
        }
        val file = localFile
        if (file == null) {
            error = "当前视频不是本地文件，暂时不能识别姿态"
            return
        }
        val analyzer = poseAnalyzer ?: runCatching { V2VideoPoseAnalyzer(file) }
            .onFailure { error = "人体骨架暂时不可用，请稍后重试" }
            .getOrNull()
            ?.also { poseAnalyzer = it }
        if (analyzer != null) {
            showPose = true
            playbackNotice = "姿态识别已开启"
        }
    }
    fun captureCurrentFrame() {
        val file = localFile
        if (file != null) {
            scope.launch {
                playbackNotice = saveProfessionalAnalysisFrame(context, file, currentMs, isFlipped)
            }
            return
        }
        val shot = videoTexture?.bitmap
        if (shot == null) {
            playbackNotice = "当前帧暂时无法保存"
            return
        }
        scope.launch {
            playbackNotice = saveAnalysisFrameBitmap(context, shot, isFlipped)
        }
    }
    V2BackHandler(onBack = {
        if (isRecording) showDiscardRecordingConfirm = true else cancelAndClose()
    })
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val chromeTop = statusTop + FrameChromeBelowStatus + FrameChromeButton
    Box(
        Modifier.fillMaxSize().background(Color.Black).navigationBarsPadding()
            .testTag(if (standaloneReview) "standalone_analysis_review" else "professional_analysis_reply"),
    ) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(R.layout.v2_analysis_player_view, null) as PlayerView).apply {
                        this.player = player
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        (videoSurfaceView as? TextureView)?.let { texture ->
                            videoTexture = texture
                            texture.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                                mirrorAnalysisTexture(view as TextureView, view.getTag(R.id.analysis_video_flipped) == true)
                            }
                        }
                    }
                },
                update = { view ->
                    if (view.player !== player) view.player = player
                    val texture = view.videoSurfaceView as? TextureView ?: return@AndroidView
                    if (videoTexture !== texture) videoTexture = texture
                    if (texture.getTag(R.id.analysis_video_flipped) == isFlipped) return@AndroidView
                    texture.setTag(R.id.analysis_video_flipped, isFlipped)
                    mirrorAnalysisTexture(texture, isFlipped)
                },
            )
            if (!playbackReady && error == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(36.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            }
            Box(Modifier.fillMaxSize().graphicsLayer { scaleX = if (isFlipped) -1f else 1f }) {
                if (showPose) {
                    V2VideoPoseOverlay(points = posePoints, modifier = Modifier.fillMaxSize())
                }
                V2DrawingOverlay(
                    state = drawingState,
                    modifier = Modifier.fillMaxSize(),
                    interactionEnabled = drawingVisible,
                )
            }
        }

        if (!drawingVisible) {
            Box(
                Modifier.fillMaxSize().padding(
                    top = chromeTop,
                    bottom = if (standaloneReview) if (showContinuousBar) 96.dp else 176.dp else 360.dp,
                )
                    .clickable {
                        if (showOverflowMenu) showOverflowMenu = false else controlsVisible = !controlsVisible
                    },
            )
        }

        if (showSelfie) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val containerWidthPx = constraints.maxWidth.toFloat()
                val containerHeightPx = constraints.maxHeight.toFloat()
                val previewWidthPx = with(density) { 126.dp.toPx() }
                val previewHeightPx = with(density) { 178.dp.toPx() }
                val basePosition = Offset(
                    x = with(density) { 82.dp.toPx() },
                    y = maxOf(containerHeightPx - with(density) { 292.dp.toPx() }, with(density) { 420.dp.toPx() }),
                )
                val clampedPosition = standaloneSelfieClampedPosition(
                    basePosition = basePosition,
                    proposedOffset = selfieSettledOffset,
                    containerSize = Size(containerWidthPx, containerHeightPx),
                    halfPreviewWidth = previewWidthPx / 2f,
                    halfPreviewHeight = previewHeightPx / 2f,
                    horizontalMargin = with(density) { 8.dp.toPx() },
                    topMargin = with(density) { 58.dp.toPx() },
                    bottomMargin = with(density) { 138.dp.toPx() },
                )
                Box(
                    modifier = Modifier.offset {
                        IntOffset(
                            x = (clampedPosition.x - previewWidthPx / 2f).roundToInt(),
                            y = (clampedPosition.y - previewHeightPx / 2f).roundToInt(),
                        )
                    }.size(width = 126.dp, height = 178.dp)
                        .shadow(10.dp, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = .20f), RoundedCornerShape(10.dp))
                        .semantics {
                            contentDescription = "教练前置摄像头小窗，可以拖动到不遮挡挥杆的位置"
                        }
                        .testTag("standalone-selfie-preview")
                        .pointerInput(containerWidthPx, containerHeightPx) {
                            detectDragGestures { change, dragAmount ->
                                val proposed = Offset(
                                    x = selfieSettledOffset.x + dragAmount.x,
                                    y = selfieSettledOffset.y + dragAmount.y,
                                )
                                val settledPosition = standaloneSelfieClampedPosition(
                                    basePosition = basePosition,
                                    proposedOffset = proposed,
                                    containerSize = Size(containerWidthPx, containerHeightPx),
                                    halfPreviewWidth = previewWidthPx / 2f,
                                    halfPreviewHeight = previewHeightPx / 2f,
                                    horizontalMargin = with(density) { 8.dp.toPx() },
                                    topMargin = with(density) { 58.dp.toPx() },
                                    bottomMargin = with(density) { 138.dp.toPx() },
                                )
                                selfieSettledOffset = settledPosition - basePosition
                                change.consume()
                            }
                        },
                ) {
                    AndroidView(
                        factory = { selfiePreview },
                        update = { },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(25.dp)
                            .clip(CircleShape).background(Color.Black.copy(alpha = .48f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.OpenWith,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = .82f),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }

        if (standaloneReview && (!isRecording || continuousPlayback)) {
            if (controlsVisible) {
                StandaloneAnalysisTopBar(
                    isFlipped = isFlipped,
                    onBack = ::cancelAndClose,
                    onCaptureFrame = ::captureCurrentFrame,
                    onRecord = {
                        if (continuousPlayback) {
                            if (isRecording) V2WorkspaceAnalysisRecorder.finish(context) else startRecording(includeMicrophone = false)
                        } else {
                            showRecordingOptions = true
                        }
                    },
                    onCompare = {
                        if (continuousPlayback && !playbackCached) playbackNotice = "待缓存完毕"
                        else if (!comparisonPreparing) openComparisonPicker()
                    },
                    onFlip = ::toggleFlip,
                    onSwitchPlayer = onSwitchPlayer,
                    interactionPlayback = continuousPlayback,
                    frameMode = frameMode,
                    toolsReady = playbackCached,
                    recording = isRecording,
                    onFrameMode = {
                        if (!playbackCached) playbackNotice = "待缓存完毕"
                        else frameMode = !frameMode
                    },
                )
            }
            StandaloneAnalysisBottomControls(
                currentMs = currentMs,
                durationMs = durationMs,
                playbackRate = playbackRate,
                isPlaying = player.isPlaying,
                scrubFeel = StandaloneTimelineScrubFeel.PRECISE,
                transportVisible = controlsVisible,
                continuousPlayback = showContinuousBar,
                showDragTime = playbackScrubbing,
                onSeek = { targetMs ->
                    currentMs = targetMs
                    if (!showContinuousBar) frameSeeker.moveTo(targetMs)
                },
                onScrubStart = {
                    if (showContinuousBar) {
                        resumeAfterPlaybackScrub = player.isPlaying
                        playbackScrubbing = true
                        player.pause()
                    } else {
                        frameSeeker.begin(33_000L)
                    }
                },
                onScrubEnd = { frameSeeker.end() },
                onPlaybackRelease = { targetMs ->
                    currentMs = targetMs
                    playbackScrubbing = false
                    player.seekTo(targetMs)
                    if (resumeAfterPlaybackScrub) player.play()
                },
                onRestart = {
                    player.pause()
                    player.seekTo(0L)
                },
                onStepPrevious = {
                    player.pause()
                    player.seekTo((player.currentPosition - 33L).coerceAtLeast(0L))
                },
                onTogglePlayback = {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        val duration = player.duration
                        val ended = player.playbackState == Player.STATE_ENDED ||
                            (duration > 0L && player.currentPosition >= duration - 200L)
                        if (ended) {
                            player.seekTo(0L)
                            currentMs = 0L
                        }
                        player.play()
                    }
                },
                onStepNext = {
                    player.pause()
                    player.seekTo((player.currentPosition + 33L).coerceAtMost(durationMs))
                },
                onCycleRate = {
                    val rates = PROFESSIONAL_ANALYSIS_RATES
                    val index = rates.indexOf(playbackRate).takeIf { it >= 0 } ?: 0
                    playbackRate = rates[(index + 1) % rates.size]
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        if (controlsVisible) {
            if (standaloneReview && (!isRecording || continuousPlayback)) {
                StandaloneAnalysisSideTools(
                    drawingState = drawingState,
                    drawingVisible = drawingVisible,
                    showSelfie = showSelfie,
                    topInset = chromeTop + FrameChromeToColumns,
                    onToggleSelfie = ::toggleSelfiePreview,
                    onToggleDrawing = { drawingVisible = !drawingVisible },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ProfessionalAnalysisTopBar(
                title = title,
                subtitle = subtitle,
                recording = isRecording,
                paused = activePhase == V2AnalysisRecordingPhase.PAUSED,
                recordingElapsedText = recordingElapsedText,
                showOverflowMenu = showOverflowMenu,
                isFlipped = isFlipped,
                onBack = {
                    if (isRecording) showDiscardRecordingConfirm = true else cancelAndClose()
                },
                onStop = { V2WorkspaceAnalysisRecorder.finish(context) },
                onToggleRecordingPause = {
                    if (activePhase == V2AnalysisRecordingPhase.PAUSED) {
                        V2WorkspaceAnalysisRecorder.resume(context)
                    } else {
                        V2WorkspaceAnalysisRecorder.pause(context)
                    }
                },
                onUnavailableWhileRecording = { playbackNotice = "请先停止录屏" },
                onToggleMenu = { showOverflowMenu = !showOverflowMenu },
                    onFlip = ::toggleFlip,
                )

                ProfessionalAnalysisReplyControls(
                currentMs = currentMs,
                durationMs = durationMs,
                playbackRate = playbackRate,
                drawingState = drawingState,
                drawingVisible = drawingVisible,
                isFlipped = isFlipped,
                showPose = showPose,
                comparisonPreparing = comparisonPreparing,
                includeStudentAudio = includeStudentAudio,
                isRecording = isRecording,
                recordingPaused = activePhase == V2AnalysisRecordingPhase.PAUSED,
                showSelfie = showSelfie,
                onSeek = { targetMs ->
                    currentMs = targetMs
                    frameSeeker.moveTo(targetMs)
                },
                onScrubStart = { frameSeeker.begin(33_000L) },
                onScrubEnd = { frameSeeker.end() },
                onStepPrevious = {
                    player.pause()
                    player.seekTo((player.currentPosition - 33L).coerceAtLeast(0L))
                },
                onStepNext = {
                    player.pause()
                    player.seekTo((player.currentPosition + 33L).coerceAtMost(durationMs))
                },
                onRate = { rate ->
                    playbackRate = rate
                    playbackNotice = if (rate == 1f) "已恢复正常速度 1x" else "慢放已开启 · ${rate}x"
                },
                onDrawingTool = { tool ->
                    drawingState.currentTool = tool
                    drawingVisible = true
                },
                onUndo = drawingState::undo,
                onClear = drawingState::clearAll,
                    onFlip = ::toggleFlip,
                    onPose = ::togglePose,
                onCompare = { if (!comparisonPreparing) openComparisonPicker() },
                onToggleStudentAudio = {
                    includeStudentAudio = !includeStudentAudio
                    playbackNotice = if (includeStudentAudio) "学员视频原声已开启" else "学员视频原声已关闭"
                },
                onRecord = {
                    when {
                        !isRecording -> showRecordingOptions = true
                        activePhase == V2AnalysisRecordingPhase.PAUSED -> V2WorkspaceAnalysisRecorder.resume(context)
                        else -> V2WorkspaceAnalysisRecorder.pause(context)
                    }
                },
                onToggleSelfie = ::toggleSelfiePreview,
                statusText = when {
                    activePhase == V2AnalysisRecordingPhase.PAUSED -> "录制已暂停 · 点击中央按钮继续"
                    isRecording -> "正在口播录制 · 可继续播放、慢放和标注"
                    else -> "待机中 · 点击录制开始口播分析"
                },
                error = error,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        playbackNotice?.let { notice ->
            Text(
                notice,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 116.dp)
                    .clip(CircleShape).background(Color.Black.copy(alpha = .78f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        if (comparisonPreparing) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = .62f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    Text("正在处理对比视频…", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun V2WorkspaceRecordingPreviewScreen(
    recording: File,
    replyFlow: Boolean = true,
    onDiscardAndRecordAgain: () -> Unit,
    onKeepInDraft: () -> Unit,
    onUse: () -> Unit,
) {
    val context = LocalContext.current
    val validRecording = remember(recording.absolutePath, recording.length()) {
        recording.isFile && recording.length() > 0L
    }
    val player = remember(recording.absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            if (validRecording) {
                setMediaItem(MediaItem.fromUri(recording.toURI().toString()))
                playWhenReady = true
                prepare()
            }
        }
    }
    val previewSeeker = remember(player) { SmoothFrameSeeker(player) }
    var currentMs by remember(recording.absolutePath) { mutableLongStateOf(0L) }
    var durationMs by remember(recording.absolutePath) { mutableLongStateOf(0L) }
    var isPlaying by remember(recording.absolutePath) { mutableStateOf(validRecording) }
    var showDiscardConfirm by remember(recording.absolutePath) { mutableStateOf(false) }
    var didChooseUse by remember(recording.absolutePath) { mutableStateOf(false) }

    LaunchedEffect(player, validRecording) {
        if (!validRecording) return@LaunchedEffect
        while (true) {
            if (!previewSeeker.isScrubbing) {
                currentMs = player.currentPosition.coerceAtLeast(0L)
                isPlaying = player.isPlaying
            }
            player.duration.takeIf { it > 0L }?.let { durationMs = it }
            delay(50)
        }
    }
    DisposableEffect(player) {
        onDispose {
            previewSeeker.dispose()
            player.release()
        }
    }

    fun requestClose() {
        if (validRecording) showDiscardConfirm = true else onDiscardAndRecordAgain()
    }
    fun chooseUse(action: () -> Unit) {
        if (didChooseUse) return
        didChooseUse = true
        player.pause()
        isPlaying = false
        action()
    }

    V2BackHandler(onBack = ::requestClose)
    Box(
        Modifier.fillMaxSize().background(Color.Black).v2FullScreenInsets()
            .testTag("professional_recording_preview"),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(84.dp).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfessionalCircleButton(Icons.Filled.Close, "关闭录屏预览", ::requestClose)
                Spacer(Modifier.weight(1f))
                Text(
                    if (replyFlow) "确认口播回复" else "录屏预览",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    formatProfessionalAnalysisTime(durationMs),
                    color = DSColors.fairway,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(58.dp),
                )
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (validRecording) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize().clickable {
                            if (player.isPlaying) player.pause() else {
                                if (durationMs > 0L && currentMs >= durationMs - 40L) player.seekTo(0L)
                                player.play()
                            }
                        },
                        factory = { viewContext ->
                            PlayerView(viewContext).apply {
                                this.player = player
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        update = { it.player = player },
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(Icons.Filled.VideocamOff, null, tint = Color.White.copy(alpha = .86f), modifier = Modifier.size(40.dp))
                        Text("录屏无法预览", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (recording.exists()) "录屏文件为空，请重新录制。" else "录屏文件不存在或已被移除。",
                            color = Color.White.copy(alpha = .66f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 36.dp),
                        )
                    }
                }
            }
            if (validRecording) {
                Column(
                    Modifier.fillMaxWidth().height(if (replyFlow) 178.dp else 148.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0f), Color.Black.copy(alpha = .55f), Color.Black.copy(alpha = .95f)),
                            ),
                        )
                        .padding(top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (replyFlow) {
                        Text(
                            "✓  确认后回到当前对话，再由你决定是否发送",
                            color = Color.White.copy(alpha = .72f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 22.dp),
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
                        Text(
                            formatProfessionalAnalysisTime(currentMs),
                            color = Color.White.copy(alpha = .64f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            formatProfessionalAnalysisTime(durationMs),
                            color = Color.White.copy(alpha = .64f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    VideoPlayerProgressBar(
                        tool = VideoPlayerTool.Frame,
                        progress = currentMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat(),
                        onSeekFraction = { fraction ->
                            currentMs = (fraction * durationMs.coerceAtLeast(1L)).roundToLong()
                            previewSeeker.moveTo(currentMs)
                        },
                        onScrubStart = { previewSeeker.begin(33_000L) },
                        onScrubEnd = { previewSeeker.end() },
                        modifier = Modifier.fillMaxWidth().height(22.dp).padding(horizontal = 22.dp),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "重置",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).height(44.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = .16f)).clickable {
                                    player.pause()
                                    player.seekTo(0L)
                                    currentMs = 0L
                                    isPlaying = false
                                }.padding(top = 12.dp),
                        )
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            if (isPlaying) "暂停录屏预览" else "播放录屏预览",
                            tint = Color.Black,
                            modifier = Modifier.size(46.dp).clip(CircleShape).background(Color.White)
                                .clickable {
                                    if (player.isPlaying) player.pause() else {
                                        if (durationMs > 0L && currentMs >= durationMs - 40L) player.seekTo(0L)
                                        player.play()
                                    }
                                }.padding(15.dp),
                        )
                        Text(
                            if (replyFlow) "加入当前回复  ›" else "保存到相册  ›",
                            color = if (didChooseUse) Color.White.copy(alpha = .55f) else Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).height(44.dp).clip(CircleShape)
                                .background(if (didChooseUse) Color.White.copy(alpha = .10f) else DSColors.fairway)
                                .clickable(enabled = !didChooseUse) { chooseUse(onUse) }.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("放弃这段录屏？") },
            text = {
                Text(
                    if (replyFlow) "保留后会回到当前对话。只有点发送，学员才会看到。"
                    else "可以先保留录屏，稍后继续保存。",
                )
            },
            confirmButton = {
                TextButton(onClick = { chooseUse(onKeepInDraft) }) {
                    Text(if (replyFlow) "保留到回复草稿" else "保留并返回")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showDiscardConfirm = false
                            player.pause()
                            onDiscardAndRecordAgain()
                        },
                    ) {
                        Text(
                            if (replyFlow) "放弃并重新录制" else "放弃并返回",
                            color = Color(0xFFD74A43),
                        )
                    }
                    TextButton(onClick = { showDiscardConfirm = false }) { Text("继续预览") }
                }
            },
        )
    }
}

@Composable
private fun ProfessionalRecordingModeSheetContent(
    replyFlow: Boolean,
    showSelfie: Boolean,
    onToggleSelfie: () -> Unit,
    onStartWithMicrophone: () -> Unit,
    onStartScreenOnly: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Filled.Mic,
                null,
                tint = DSColors.fairway,
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(DSColors.fairway.copy(alpha = .18f)).padding(12.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (replyFlow) "录一段口播回复" else "开始录制分析",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (replyFlow) "录下视频、标注和你的讲解，结束后直接回到当前学员对话。"
                    else "会录下视频、标注和播放操作。下一步将出现系统录屏确认。",
                    color = Color.White.copy(alpha = .62f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = .07f)).clickable(onClick = onToggleSelfie)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Icon(
                Icons.Filled.Person,
                null,
                tint = if (showSelfie) DSColors.fairway else Color.White.copy(alpha = .76f),
                modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = .09f)).padding(9.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("显示教练小窗", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("可选，录屏里会同时看到你的讲解画面", color = Color.White.copy(alpha = .58f), fontSize = 11.sp)
            }
            Text(if (showSelfie) "●" else "○", color = if (showSelfie) DSColors.fairway else Color.White.copy(alpha = .42f), fontSize = 20.sp)
        }
        Button(
            onClick = onStartWithMicrophone,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (replyFlow) "开始口播分析" else "录屏与麦克风",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (replyFlow) "默认关闭学员原声，避免干扰讲解" else "适合边讲解边标注",
                    color = Color.White.copy(alpha = .72f),
                    fontSize = 10.sp,
                )
            }
        }
        Button(
            onClick = onStartScreenOnly,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = .07f)),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("仅录屏", color = DSColors.fairway, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("只记录画面和操作", color = Color.White.copy(alpha = .58f), fontSize = 10.sp)
            }
        }
        if (replyFlow) {
            Text(
                "🔒 录制结果会先回到回复草稿，不会自动发送",
                color = Color.White.copy(alpha = .62f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(42.dp)) {
            Text("取消", color = Color.White.copy(alpha = .72f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

internal val PROFESSIONAL_ANALYSIS_RATES = listOf(1f, 0.5f, 0.25f)

internal fun formatProfessionalAnalysisTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

internal fun formatStandaloneAnalysisPreciseTime(milliseconds: Long): String =
    java.lang.String.format(java.util.Locale.US, "%05.2f", milliseconds.coerceAtLeast(0L) / 1_000.0)

internal fun formatProfessionalAnalysisRate(rate: Float): String =
    rate.toString().removeSuffix(".0") + "×"

private fun mirrorAnalysisTexture(texture: TextureView, flipped: Boolean) {
    val matrix = android.graphics.Matrix()
    if (flipped) {
        val width = texture.width.toFloat()
        val height = texture.height.toFloat()
        if (width <= 0f || height <= 0f) return
        matrix.setScale(-1f, 1f, width / 2f, height / 2f)
    }
    texture.setTransform(matrix)
}

internal suspend fun saveProfessionalAnalysisFrame(
    context: Context,
    sourceFile: File,
    positionMs: Long,
    flippedHorizontally: Boolean = false,
): String = withContext(Dispatchers.IO) {
    if (!sourceFile.isFile || sourceFile.length() <= 0L) return@withContext "当前帧暂时无法保存"
    val bitmap = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(sourceFile.absolutePath)
            retriever.getFrameAtTime(positionMs.coerceAtLeast(0L) * 1_000L, MediaMetadataRetriever.OPTION_CLOSEST)
        } finally {
            retriever.release()
        }
    }.getOrNull() ?: return@withContext "当前帧暂时无法保存"
    saveAnalysisFrameBitmap(context, bitmap, flippedHorizontally)
}

internal suspend fun saveAnalysisFrameBitmap(
    context: Context,
    bitmap: Bitmap,
    flippedHorizontally: Boolean = false,
): String = withContext(Dispatchers.IO) {
    val frame = if (!flippedHorizontally) {
        bitmap
    } else {
        val matrix = android.graphics.Matrix().apply { preScale(-1f, 1f) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }
    val resolver = context.contentResolver
    val name = "huibei_frame_${DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Huibei")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return@withContext "系统相册暂时不可写"
    val wrote = runCatching {
        resolver.openOutputStream(uri)?.use { output ->
            check(frame.compress(Bitmap.CompressFormat.JPEG, 94, output))
        } ?: error("missing output stream")
    }.isSuccess
    frame.recycle()
    if (!wrote) {
        runCatching { resolver.delete(uri, null, null) }
        return@withContext "当前帧保存失败，请稍后重试"
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val publish = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
        if (runCatching { resolver.update(uri, publish, null, null) }.isFailure) {
            runCatching { resolver.delete(uri, null, null) }
            return@withContext "当前帧保存失败，请稍后重试"
        }
    }
    "当前帧已保存到相册"
}

private val FrameSideButton = 40.dp
private val FrameSideInset = 12.dp
private val FrameChromeButton = 32.dp
private val FrameChromeGlyph = 16.dp
private val FrameChromeInset = FrameSideInset + (FrameSideButton - FrameChromeButton) / 2
private val FrameChromeBelowStatus = 4.dp
private val FrameChromeToColumns = 12.dp

@Composable
internal fun StandaloneAnalysisTopBar(
    isFlipped: Boolean,
    onBack: () -> Unit,
    onCaptureFrame: () -> Unit,
    onRecord: () -> Unit,
    onCompare: () -> Unit,
    onFlip: () -> Unit,
    onSwitchPlayer: (() -> Unit)?,
    interactionPlayback: Boolean = false,
    frameMode: Boolean = false,
    toolsReady: Boolean = true,
    recording: Boolean = false,
    onFrameMode: () -> Unit = {},
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        Modifier.fillMaxWidth()
            .height(statusTop + FrameChromeBelowStatus + FrameChromeButton)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = .55f), Color.Black.copy(alpha = .12f), Color.Transparent),
                ),
            ),
    ) {
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = FrameChromeInset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
        StandaloneTopIcon(Icons.Filled.Close, "返回", onBack)
        StandaloneTopIcon(Icons.Filled.CameraAlt, "保存当前帧截图", onCaptureFrame)
        Box(
            Modifier.size(FrameChromeButton).clip(CircleShape)
                .background(if (recording) Color(0xFFD74A43) else DSColors.fairway)
                .clickable(onClick = onRecord),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (recording) Icons.Filled.StopCircle else Icons.Filled.Mic,
                if (recording) "停止录屏" else "开始屏幕录制",
                tint = Color.White,
                modifier = Modifier.size(FrameChromeGlyph),
            )
        }
        Box(
            Modifier.size(FrameChromeButton).clip(CircleShape).background(Color.Black.copy(alpha = .42f))
                .border(1.dp, Color.White.copy(alpha = .16f), CircleShape)
                .graphicsLayer { alpha = if (interactionPlayback && !toolsReady) 0.4f else 1f }
                .clickable(onClick = onCompare),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "VS",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
        if (interactionPlayback) {
            StandaloneTopIcon(
                Icons.Filled.Sync,
                "切换逐帧",
                onFrameMode,
                tint = if (frameMode) DSColors.fairway else Color.White,
                dimmed = !toolsReady,
            )
        } else {
            StandaloneTopIcon(
                Icons.Filled.Flip,
                "水平翻转",
                onFlip,
                tint = if (isFlipped) DSColors.fairway else Color.White,
            )
            onSwitchPlayer?.let {
                StandaloneTopIcon(Icons.Filled.Sync, "切换到逐帧", it)
            }
        }
        }
    }
}

@Composable
private fun StandaloneTopIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
    dimmed: Boolean = false,
) {
    Box(
        Modifier.size(FrameChromeButton).clip(CircleShape).background(Color.Black.copy(alpha = .42f))
            .border(1.dp, Color.White.copy(alpha = .16f), CircleShape)
            .graphicsLayer { alpha = if (dimmed) 0.4f else 1f }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(FrameChromeGlyph))
    }
}

@Composable
internal fun StandaloneScrubFeelControl(
    selected: StandaloneTimelineScrubFeel,
    onSelect: (StandaloneTimelineScrubFeel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.clip(CircleShape).background(Color.Black.copy(alpha = .45f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("手感", color = Color.White.copy(alpha = .68f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        StandaloneTimelineScrubFeel.entries.forEach { feel ->
            Text(
                feel.title,
                color = if (selected == feel) Color.Black else Color.White.copy(alpha = .86f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.size(width = 34.dp, height = 26.dp).clip(CircleShape)
                    .background(if (selected == feel) Color.White else Color.White.copy(alpha = .16f))
                    .semantics { this.selected = selected == feel }
                    .clickable { onSelect(feel) }.padding(top = 5.dp),
            )
        }
    }
}

@Composable
internal fun StandaloneAnalysisBottomControls(
    currentMs: Long,
    durationMs: Long,
    playbackRate: Float,
    isPlaying: Boolean,
    scrubFeel: StandaloneTimelineScrubFeel,
    transportVisible: Boolean = true,
    continuousPlayback: Boolean = false,
    showDragTime: Boolean = false,
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
    onPlaybackRelease: (Long) -> Unit = {},
    onRestart: () -> Unit,
    onStepPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onStepNext: () -> Unit,
    onCycleRate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    Column(
        modifier.fillMaxWidth().height(if (transportVisible) if (continuousPlayback) 96.dp else 176.dp else 60.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = .36f), Color.Black.copy(alpha = .64f)),
                ),
            )
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (transportVisible) {
            if (!continuousPlayback) {
                StandaloneTimeBubble(
                    currentMs = currentMs,
                    durationMs = safeDuration,
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                )
            }
            if (continuousPlayback) {
                ContinuousPlaybackBar(
                    currentMs = currentMs,
                    durationMs = safeDuration,
                    showDragTime = showDragTime,
                    onSeek = onSeek,
                    onScrubStart = onScrubStart,
                    onPlaybackRelease = onPlaybackRelease,
                )
            } else {
                ReviewFrameScrubber(
                    currentMs = currentMs,
                    durationMs = safeDuration,
                    scrubFeel = scrubFeel,
                    showProgress = true,
                    onSeek = onSeek,
                    onScrubStart = onScrubStart,
                    onScrubEnd = onScrubEnd,
                )
            }
        } else if (continuousPlayback) {
            ContinuousPlaybackBar(
                currentMs = currentMs,
                durationMs = safeDuration,
                showDragTime = showDragTime,
                onSeek = onSeek,
                onScrubStart = onScrubStart,
                onPlaybackRelease = onPlaybackRelease,
            )
        } else {
            ReviewFrameScrubber(
                currentMs = currentMs,
                durationMs = safeDuration,
                scrubFeel = scrubFeel,
                showProgress = false,
                onSeek = onSeek,
                onScrubStart = onScrubStart,
                onScrubEnd = onScrubEnd,
            )
        }
        if (transportVisible) Row(
            Modifier.fillMaxWidth().height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StandaloneBottomIcon(Icons.Filled.Replay, "回到开头", onRestart)
            if (!continuousPlayback) StandaloneBottomIcon(Icons.Filled.SkipPrevious, "上一帧", onStepPrevious)
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(Color.White).clickable(onClick = onTogglePlayback),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (isPlaying) "暂停播放" else "播放视频",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp),
                )
            }
            if (!continuousPlayback) StandaloneBottomIcon(Icons.Filled.SkipNext, "下一帧", onStepNext)
            Box(
                Modifier.size(width = 44.dp, height = 36.dp).clickable(onClick = onCycleRate),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    formatProfessionalAnalysisRate(playbackRate),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ContinuousPlaybackBar(
    currentMs: Long,
    durationMs: Long,
    showDragTime: Boolean,
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit,
    onPlaybackRelease: (Long) -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        if (showDragTime) {
            Text(
                "${formatProfessionalAnalysisTime(currentMs)} / ${formatProfessionalAnalysisTime(durationMs)}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-36).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        VideoPlayerProgressBar(
            tool = VideoPlayerTool.Playback,
            progress = currentMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat(),
            onScrubStart = onScrubStart,
            onSeekFraction = { fraction -> onSeek((fraction * durationMs).roundToLong()) },
            onScrubEnd = { fraction -> onPlaybackRelease((fraction * durationMs).roundToLong()) },
            modifier = Modifier.fillMaxWidth().height(24.dp),
        )
    }
}

@Composable
private fun StandaloneTimeBubble(
    currentMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val bubbleWidth = 84.dp
        val progress = (currentMs.toDouble() / durationMs.coerceAtLeast(1L).toDouble()).coerceIn(0.0, 1.0)
        val center = (maxWidth * progress.toFloat()).coerceIn(bubbleWidth / 2, maxWidth - bubbleWidth / 2)
        Text(
            formatStandaloneAnalysisPreciseTime(currentMs),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(x = center - bubbleWidth / 2).width(bubbleWidth)
                .clip(CircleShape).background(Color.Black.copy(alpha = .78f))
                .padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun StandaloneBottomIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(Modifier.size(40.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun StandaloneAnalysisSideTools(
    drawingState: V2DrawingState,
    drawingVisible: Boolean,
    showSelfie: Boolean,
    topInset: Dp = 120.dp,
    onToggleSelfie: () -> Unit,
    onToggleDrawing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Column(
            Modifier.align(Alignment.TopStart).padding(start = FrameSideInset, top = topInset),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StandaloneSideIcon(Icons.Filled.Person, "切换自拍预览", showSelfie, onToggleSelfie)
            StandaloneSideIcon(
                V2SelectionArrowIcon,
                "箭头",
                drawingState.currentTool == V2DrawingTool.Select,
                onClick = { drawingState.currentTool = V2DrawingTool.Select },
                iconTint = v2SelectionToolTint(
                    drawingState.currentTool == V2DrawingTool.Select,
                    drawingState.currentColor,
                ),
            )
            StandaloneSideIcon(Icons.AutoMirrored.Filled.Undo, "撤销上一笔标记", false, drawingState::undo)
            StandaloneSideIcon(Icons.Filled.LayersClear, "清空所有标记", false, drawingState::clearAll)
        }
        if (drawingVisible) {
            V2DrawingToolsPanel(
                state = drawingState,
                onClose = onToggleDrawing,
                showSelectTool = false,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = topInset, end = FrameSideInset),
            )
        } else {
            StandaloneSideIcon(
                Icons.Filled.Edit,
                "打开绘图工具",
                false,
                onToggleDrawing,
                Modifier.align(Alignment.TopEnd).padding(top = topInset, end = FrameSideInset),
            )
        }
    }
}

@Composable
private fun StandaloneSideIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
) {
    Box(
        modifier.size(FrameSideButton).clip(CircleShape).background(Color.Black.copy(alpha = .48f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            description,
            tint = iconTint ?: if (selected) DSColors.fairway else Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun ProfessionalAnalysisTopBar(
    title: String,
    subtitle: String?,
    recording: Boolean,
    paused: Boolean,
    recordingElapsedText: String,
    showOverflowMenu: Boolean,
    isFlipped: Boolean,
    onBack: () -> Unit,
    onStop: () -> Unit,
    onToggleRecordingPause: () -> Unit,
    onUnavailableWhileRecording: () -> Unit,
    onToggleMenu: () -> Unit,
    onFlip: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(104.dp).background(Color.Black.copy(alpha = .42f))
            .padding(horizontal = 23.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfessionalCircleButton(Icons.Filled.Close, if (recording) "取消录屏并返回" else "返回", onBack)
        if (recording) {
            Spacer(Modifier.weight(1f))
            ProfessionalCircleButton(Icons.Filled.StopCircle, "停止录屏", onStop, tint = Color(0xFFD74A43))
            Spacer(Modifier.weight(1f))
            Column(
                Modifier.width(76.dp).clickable(onClick = onToggleRecordingPause),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    if (paused) "继续录制" else "暂停录制",
                    tint = if (paused) DSColors.fairway else Color.White,
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = .42f))
                        .border(1.5.dp, if (paused) DSColors.fairway else Color.White.copy(alpha = .9f), CircleShape)
                        .padding(12.dp),
                )
                Text(
                    (if (paused) "○ " else "● ") + recordingElapsedText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "VS",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onUnavailableWhileRecording)
                    .padding(top = 13.dp),
            )
            Spacer(Modifier.weight(1f))
            ProfessionalCircleButton(Icons.Filled.MoreVert, "更多工具", onUnavailableWhileRecording)
        } else {
            Spacer(Modifier.weight(1f))
            Column(
                Modifier.width(210.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    subtitle?.takeIf(String::isNotBlank) ?: "学员",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    title.ifBlank { "学员视频" },
                    color = Color.White.copy(alpha = .62f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.weight(1f))
            Box {
                ProfessionalCircleButton(Icons.Filled.MoreVert, "更多工具", onToggleMenu)
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = onToggleMenu,
                    modifier = Modifier.width(148.dp).background(Color(0xEB111111)),
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "水平翻转",
                                color = if (isFlipped) DSColors.fairway else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.SwapHoriz,
                                null,
                                tint = if (isFlipped) DSColors.fairway else Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        onClick = onFlip,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfessionalCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
) {
    Icon(
        icon,
        description,
        tint = tint,
        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = .13f))
            .clickable(onClick = onClick).padding(12.dp),
    )
}

@Composable
internal fun ProfessionalAnalysisReplyControls(
    currentMs: Long,
    durationMs: Long,
    playbackRate: Float,
    drawingState: V2DrawingState,
    drawingVisible: Boolean,
    isFlipped: Boolean,
    showPose: Boolean,
    comparisonPreparing: Boolean,
    includeStudentAudio: Boolean,
    isRecording: Boolean,
    recordingPaused: Boolean,
    showSelfie: Boolean,
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit = {},
    onScrubEnd: () -> Unit = {},
    onStepPrevious: () -> Unit,
    onStepNext: () -> Unit,
    onRate: (Float) -> Unit,
    onDrawingTool: (V2DrawingTool) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onFlip: () -> Unit,
    onPose: () -> Unit,
    onCompare: () -> Unit,
    onToggleStudentAudio: () -> Unit,
    onRecord: () -> Unit,
    onToggleSelfie: () -> Unit,
    statusText: String,
    error: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().height(360.dp).background(Color.Black.copy(alpha = .94f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfessionalCompactControl(Icons.Filled.SkipPrevious, "上一帧", onStepPrevious)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                VideoPlayerProgressBar(
                    tool = VideoPlayerTool.Frame,
                    progress = currentMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat(),
                    onSeekFraction = { fraction -> onSeek((fraction * durationMs.coerceAtLeast(1L)).roundToLong()) },
                    onScrubStart = onScrubStart,
                    onScrubEnd = { onScrubEnd() },
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        formatProfessionalAnalysisTime(currentMs),
                        color = Color.White.copy(alpha = .62f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        formatProfessionalAnalysisTime(durationMs),
                        color = Color.White.copy(alpha = .62f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            ProfessionalCompactControl(Icons.Filled.SkipNext, "下一帧", onStepNext)
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            PROFESSIONAL_ANALYSIS_RATES.forEach { rate ->
                Text(
                    formatProfessionalAnalysisRate(rate),
                    color = if (playbackRate == rate) Color.Black else Color.White.copy(alpha = .82f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.size(width = 58.dp, height = 36.dp).clip(CircleShape)
                        .background(if (playbackRate == rate) DSColors.fairway else Color.White.copy(alpha = .10f))
                        .clickable { onRate(rate) }
                        .padding(top = 10.dp),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = .09f)).padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfessionalDrawingControl(Icons.Filled.HorizontalRule, "杆身或目标线", drawingState.currentTool == V2DrawingTool.Line && drawingVisible) {
                onDrawingTool(V2DrawingTool.Line)
            }
            ProfessionalDrawingControl(Icons.Filled.Draw, "挥杆轨迹曲线", drawingState.currentTool == V2DrawingTool.Freehand && drawingVisible) {
                onDrawingTool(V2DrawingTool.Freehand)
            }
            ProfessionalDrawingControl(Icons.Filled.SouthWest, "挥杆方向箭头", drawingState.currentTool == V2DrawingTool.Arrow && drawingVisible) {
                onDrawingTool(V2DrawingTool.Arrow)
            }
            ProfessionalDrawingControl(Icons.Filled.ChangeHistory, "身体和杆面角度", drawingState.currentTool == V2DrawingTool.Angle && drawingVisible) {
                onDrawingTool(V2DrawingTool.Angle)
            }
            ProfessionalDrawingControl(
                V2SelectionArrowIcon,
                "选择和调整标记",
                drawingState.currentTool == V2DrawingTool.Select && drawingVisible,
                iconTint = v2SelectionToolTint(
                    drawingState.currentTool == V2DrawingTool.Select && drawingVisible,
                    drawingState.currentColor,
                ),
            ) {
                onDrawingTool(V2DrawingTool.Select)
            }
            ProfessionalDrawingControl(Icons.AutoMirrored.Filled.Undo, "撤销上一笔标记", false, onClick = onUndo)
            ProfessionalDrawingControl(Icons.Filled.LayersClear, "清空所有标记", false, onClick = onClear)
        }

        Row(
            Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = .09f)),
        ) {
            AnalysisUtilityButton(Icons.Filled.SwapHoriz, "水平翻转", isFlipped, Modifier.weight(1f), onFlip)
            AnalysisUtilityButton(Icons.Filled.AccessibilityNew, "人体骨架", showPose, Modifier.weight(1f), onPose)
            AnalysisUtilityButton(Icons.AutoMirrored.Filled.CompareArrows, "双视频对比", comparisonPreparing, Modifier.weight(1f), onCompare)
        }

        Row(
            Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfessionalRoundControl(
                if (includeStudentAudio) Icons.AutoMirrored.Filled.VolumeUp else Icons.Filled.Mic,
                if (includeStudentAudio) "关闭学员视频原声" else "开启学员视频原声",
                includeStudentAudio,
                onToggleStudentAudio,
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(58.dp).clip(CircleShape)
                    .background(DSColors.fairway.copy(alpha = if (isRecording) .72f else 1f))
                    .border(4.dp, Color.White.copy(alpha = .28f), CircleShape)
                    .semantics {
                        contentDescription = if (isRecording) {
                            if (recordingPaused) "继续录制" else "暂停录制"
                        } else {
                            "开始口播分析"
                        }
                    }
                    .clickable(onClick = onRecord),
                contentAlignment = Alignment.Center,
            ) {
                if (recordingPaused) {
                    Icon(Icons.Filled.PlayArrow, "继续录制", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            ProfessionalRoundControl(Icons.Filled.Person, "切换自拍预览", showSelfie, onToggleSelfie)
        }

        Text(
            error ?: statusText,
            color = if (error == null) Color.White.copy(alpha = .54f) else Color(0xFFFF8B83),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProfessionalCompactControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Icon(
        icon,
        description,
        tint = Color.White,
        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = .10f))
            .clickable(onClick = onClick).padding(9.dp),
    )
}

@Composable
private fun ProfessionalDrawingControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean,
    iconTint: Color? = null,
    onClick: () -> Unit,
) {
    Icon(
        icon,
        description,
        tint = iconTint ?: if (selected) DSColors.fairway else Color.White.copy(alpha = .88f),
        modifier = Modifier.size(width = 36.dp, height = 38.dp).clip(RoundedCornerShape(10.dp))
            .background(if (selected) DSColors.fairway.copy(alpha = .12f) else Color.Transparent)
            .clickable(onClick = onClick).padding(11.dp),
    )
}

@Composable
private fun ProfessionalRoundControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Icon(
        icon,
        description,
        tint = if (selected) DSColors.fairway else Color.White,
        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = .10f))
            .clickable(onClick = onClick).padding(14.dp),
    )
}

@Composable
private fun AnalysisUtilityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 7.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (active) Color(0xFFFFD33D) else Color.White.copy(alpha = .88f),
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.size(5.dp))
        Text(
            label,
            color = if (active) Color(0xFFFFD33D) else Color.White.copy(alpha = .88f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
