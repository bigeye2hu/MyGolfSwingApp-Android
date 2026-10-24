@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mygolfswing.app.ui.swing

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.R
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.data.video.deleteFileCountingFreedBytes
import com.mygolfswing.app.ui.common.isPermissionPermanentlyDenied
import com.mygolfswing.app.ui.common.openAppSettings
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/** 挥杆录制入口（对齐 iOS `JustSwingMainView`：进入后停留在相机页连续录制）。 */
@Composable
fun V2JustSwingMainScreen(
    onBack: () -> Unit,
    onOpenClips: () -> Unit = {},
    onClipSaved: (UUID) -> Unit = {},
    sourceLabel: String? = null,
    isLoggedIn: Boolean = true,
    onRequestLogin: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findSwingActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var hasAudioPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var savedToast by remember { mutableStateOf(false) }
    var pendingClose by remember { mutableStateOf(false) }
    var cameraPermissionDeniedPermanently by remember { mutableStateOf(false) }
    var audioPermissionDeniedPermanently by remember { mutableStateOf(false) }
    var sessionClipCount by remember { mutableIntStateOf(0) }
    var latestClipPreviewModel by remember { mutableStateOf<String?>(null) }
    var showLatestClipThumbnail by remember { mutableStateOf(false) }
    var cameraZoomFactor by remember { mutableFloatStateOf(1f) }
    var minCameraZoomFactor by remember { mutableFloatStateOf(1f) }
    var maxCameraZoomFactor by remember { mutableFloatStateOf(1f) }
    var showZoomIndicator by remember { mutableStateOf(false) }
    var zoomIndicatorRequest by remember { mutableIntStateOf(0) }
    var recordingElapsedMs by remember { mutableLongStateOf(0L) }
    var recordingNotice by remember { mutableStateOf<String?>(null) }

    DisposableEffect(activity) {
        val window = activity?.window
        val decorView = window?.decorView
        val wasStatusBarVisible = decorView?.let {
            ViewCompat.getRootWindowInsets(it)?.isVisible(WindowInsetsCompat.Type.statusBars())
        } ?: true
        val controller = if (window != null && decorView != null) {
            WindowInsetsControllerCompat(window, decorView)
        } else {
            null
        }
        activity?.hideSwingStatusBar()
        onDispose {
            if (wasStatusBarVisible) controller?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        hasCameraPermission = grants[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasAudioPermission = grants[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        cameraPermissionDeniedPermanently = !hasCameraPermission &&
            context.isPermissionPermanentlyDenied(Manifest.permission.CAMERA)
        audioPermissionDeniedPermanently = !hasAudioPermission &&
            context.isPermissionPermanentlyDenied(Manifest.permission.RECORD_AUDIO)
        error = when {
            !hasCameraPermission -> "需要相机权限才能录制挥杆"
            !hasAudioPermission -> "需要麦克风权限才能录制挥杆视频声音"
            else -> null
        }
    }

    fun requestRecordingPermissions() {
        runCatching {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }.onFailure {
            error = "无法请求录制权限，请稍后重试"
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            if (!hasCameraPermission || !hasAudioPermission) requestRecordingPermissions()
        } else {
            activeRecording?.stop()
            videoCapture = null
            boundCamera = null
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }

    DisposableEffect(lifecycleOwner, isLoggedIn) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isLoggedIn) {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                hasCameraPermission = granted
                hasAudioPermission = audioGranted
                if (granted && audioGranted) {
                    cameraPermissionDeniedPermanently = false
                    audioPermissionDeniedPermanently = false
                    error = null
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isLoggedIn, hasCameraPermission, hasAudioPermission, lensFacing) {
        if (!isLoggedIn || !hasCameraPermission || !hasAudioPermission) return@LaunchedEffect
        bindSwingCamera(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            lensFacing = lensFacing,
            onReady = { capture, camera ->
                videoCapture = capture
                boundCamera = camera
                val zoomState = camera.cameraInfo.zoomState.value
                minCameraZoomFactor = zoomState?.minZoomRatio ?: 1f
                maxCameraZoomFactor = minOf(zoomState?.maxZoomRatio ?: 1f, 6f)
                cameraZoomFactor = (zoomState?.zoomRatio ?: minCameraZoomFactor)
                    .coerceIn(minCameraZoomFactor, maxCameraZoomFactor)
                error = null
            },
            onError = {
                videoCapture = null
                boundCamera = null
                error = it
            },
        )
    }

    LaunchedEffect(recordingNotice) {
        val current = recordingNotice ?: return@LaunchedEffect
        delay(6_000)
        if (recordingNotice == current) recordingNotice = null
    }
    LaunchedEffect(savedToast) {
        if (savedToast) {
            delay(2_000)
            savedToast = false
        }
    }
    LaunchedEffect(showLatestClipThumbnail, latestClipPreviewModel) {
        if (showLatestClipThumbnail && latestClipPreviewModel != null) {
            delay(4_000)
            showLatestClipThumbnail = false
        }
    }
    LaunchedEffect(activeRecording != null) {
        if (activeRecording != null) {
            showLatestClipThumbnail = false
            val startedAt = android.os.SystemClock.elapsedRealtime()
            while (true) {
                recordingElapsedMs = android.os.SystemClock.elapsedRealtime() - startedAt
                delay(250)
            }
        } else {
            recordingElapsedMs = 0L
        }
    }
    LaunchedEffect(zoomIndicatorRequest) {
        if (zoomIndicatorRequest == 0) return@LaunchedEffect
        delay(1_100)
        showZoomIndicator = false
    }
    LaunchedEffect(boundCamera) {
        if (boundCamera == null) return@LaunchedEffect
        delay(250)
        activity?.hideSwingStatusBar()
        delay(750)
        activity?.hideSwingStatusBar()
    }

    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.close()
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }

    fun closeScreen() {
        if (pendingClose) return
        val recording = activeRecording
        if (recording != null) {
            pendingClose = true
            recording.stop()
        } else if (saving) {
            pendingClose = true
        } else {
            onBack()
        }
    }

    // Keep this screen alive until CameraX has finalized and the last clip has
    // been adopted by the account-scoped store. A raw system-back pop would
    // otherwise dispose the save coroutine before it can finish.
    BackHandler(onBack = ::closeScreen)

    fun toggleCamera() {
        if (activeRecording != null || saving) return
        showZoomIndicator = false
        cameraZoomFactor = 1f
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
    }

    fun saveRecordedFile(file: File, reachedLimit: Boolean = false, interrupted: Boolean = false) {
        scope.launch {
            saving = true
            var result: V2VideoFileStore.ImportResult? = null
            try {
                if (interrupted && !swingVideoIsPlayable(file)) {
                    deleteFileCountingFreedBytes(file)
                    error = "录制失败，请重试"
                    return@launch
                }
                result = V2VideoFileStore.adoptLocalFile(context, file)
                if (result == null) {
                    deleteFileCountingFreedBytes(file)
                    error = "录制失败，请重试"
                } else {
                    val clipId = V2SwingSessionStore.addClipFromVideoImportAndWait(
                        videoPath = result.absolutePath,
                        thumbnailPath = result.thumbnailPath,
                        durationSec = result.durationSec,
                        sourceLabel = sourceLabel,
                    )
                    if (clipId == null) {
                        cleanupImportedSwingClip(result)
                        error = "录制保存失败，请登录后重试"
                    } else {
                        val hasAudioTrack = swingVideoHasAudioTrack(File(result.absolutePath))
                        sessionClipCount += 1
                        latestClipPreviewModel = swingClipPreviewModel(result.thumbnailPath, result.absolutePath)
                        showLatestClipThumbnail = latestClipPreviewModel != null
                        savedToast = !reachedLimit && !interrupted
                        recordingNotice = when {
                            reachedLimit -> SWING_RECORDING_LIMIT_NOTICE
                            interrupted -> SWING_RECORDING_INTERRUPTED_NOTICE
                            else -> null
                        }
                        error = if (reachedLimit || interrupted || hasAudioTrack) {
                            null
                        } else {
                            "视频已保存，仅缺少声音。请检查麦克风权限是否开启，以及录制时是否连接了耳机或手表。"
                        }
                        onClipSaved(clipId)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                deleteFileCountingFreedBytes(file)
                result?.let(::cleanupImportedSwingClip)
                error = swingCameraThrowableError("录制保存失败，请稍后重试", e)
            } finally {
                saving = false
                if (pendingClose) onBack()
            }
        }
    }

    fun toggleRecording() {
        val currentRecording = activeRecording
        if (currentRecording != null) {
            currentRecording.stop()
            return
        }
        if (!isLoggedIn) {
            error = "登录后才能保存挥杆记录"
            onRequestLogin()
            return
        }
        val capture = videoCapture ?: run {
            error = "相机还在启动，请稍后再试"
            return
        }
        startSwingRecording(
            context = context,
            videoCapture = capture,
            onStarted = {
                activeRecording = it
                savedToast = false
                recordingNotice = null
                error = null
            },
            onFinished = { file, reachedLimit, interrupted ->
                activeRecording = null
                saveRecordedFile(file, reachedLimit, interrupted)
            },
            onError = { message ->
                activeRecording = null
                error = message
                if (pendingClose) onBack()
            },
        )
    }

    if (!isLoggedIn) {
        GuestSwingPreview(
            onClose = onBack,
            onLogin = onRequestLogin,
        )
        return
    }

    val cameraReady = boundCamera != null && videoCapture != null
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission && hasAudioPermission && cameraReady) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
                    .pointerInput(boundCamera, minCameraZoomFactor, maxCameraZoomFactor) {
                        detectTransformGestures { _, _, zoom, _ ->
                            val camera = boundCamera ?: return@detectTransformGestures
                            if (maxCameraZoomFactor <= minCameraZoomFactor) return@detectTransformGestures
                            val factor = swingZoomFactor(
                                current = cameraZoomFactor,
                                gestureScale = zoom,
                                minimum = minCameraZoomFactor,
                                maximum = maxCameraZoomFactor,
                            )
                            camera.cameraControl.setZoomRatio(factor)
                            cameraZoomFactor = factor
                            showZoomIndicator = true
                            zoomIndicatorRequest += 1
                        }
                    }
                    .semantics {
                        contentDescription = if (maxCameraZoomFactor > minCameraZoomFactor) {
                            "支持双指缩放的相机预览"
                        } else {
                            "当前镜头不支持缩放的相机预览"
                        }
                    }
                    .testTag("swing-camera-preview"),
            )
        } else if (hasCameraPermission && hasAudioPermission) {
            SwingCameraLoadingState(errorMessage = error)
        } else {
            PermissionState(
                permissionLabel = when {
                    !hasCameraPermission && !hasAudioPermission -> "相机和麦克风"
                    !hasCameraPermission -> "相机"
                    else -> "麦克风"
                },
                showSettingsAction = cameraPermissionDeniedPermanently || audioPermissionDeniedPermanently,
                errorMessage = error,
                onRequest = { requestRecordingPermissions() },
                onOpenSettings = {
                    if (!context.openAppSettings()) {
                        error = "系统设置暂时无法打开，请稍后重试"
                    }
                },
            )
        }

        if (cameraReady) {
            TopOverlay(
                clipCount = sessionClipCount,
                isRecording = activeRecording != null,
                recordingClock = swingRecordingClockText(recordingElapsedMs),
                onClose = ::closeScreen,
                onSwitchCamera = ::toggleCamera,
                onOpenClips = onOpenClips,
            )
            RecordingModeSwitcher(
                isRecording = activeRecording != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility.only(WindowInsetsSides.Top))
                    .padding(top = 72.dp),
            )
            LocalRecordingBadge(
                isRecording = activeRecording != null,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .v2NavigationBarPadding()
                    .padding(start = 20.dp, bottom = 100.dp),
            )
            if (showLatestClipThumbnail && latestClipPreviewModel != null) {
                LatestClipThumbnail(
                    model = latestClipPreviewModel,
                    clipCount = sessionClipCount,
                    onOpenClips = onOpenClips,
                    onDismiss = { showLatestClipThumbnail = false },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .v2NavigationBarPadding()
                        .padding(end = 20.dp, bottom = 100.dp),
                )
            }
            if (showZoomIndicator && maxCameraZoomFactor > minCameraZoomFactor) {
                SwingZoomIndicator(
                    zoomFactor = cameraZoomFactor,
                    modifier = Modifier.align(Alignment.BottomCenter).v2NavigationBarPadding().padding(bottom = 196.dp),
                )
            }
            BottomOverlay(
                isRecording = activeRecording != null,
                saving = saving,
                savedToast = savedToast,
                error = error,
                notice = recordingNotice,
                onRecordTap = ::toggleRecording,
                onOpenClips = onOpenClips,
                onRequestLogin = null,
            )
        }
    }
}

@Composable
internal fun SwingCameraLoadingState(errorMessage: String? = null) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 2.dp,
            modifier = Modifier.size(22.dp),
        )
        Text(
            if (errorMessage.isNullOrBlank()) "正在启动相机..." else errorMessage,
            color = if (errorMessage.isNullOrBlank()) Color.White.copy(alpha = 0.60f) else Color(0xFFFFD9D9),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
internal fun SwingZoomIndicator(
    zoomFactor: Float,
    modifier: Modifier = Modifier,
) {
    Text(
        swingZoomLabel(zoomFactor),
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.58f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

internal fun swingZoomLabel(factor: Float): String =
    String.format(java.util.Locale.US, "%.1fx", factor)

internal fun swingZoomFactor(
    current: Float,
    gestureScale: Float,
    minimum: Float,
    maximum: Float,
): Float {
    val low = minimum.takeIf { it.isFinite() && it > 0f } ?: 1f
    val high = maximum.takeIf { it.isFinite() && it >= low } ?: low
    val base = current.takeIf { it.isFinite() } ?: low
    val scale = gestureScale.takeIf { it.isFinite() && it > 0f } ?: 1f
    return (base * scale).coerceIn(low, high)
}

@Composable
internal fun GuestSwingPreview(
    onClose: () -> Unit,
    onLogin: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 12.dp)
                .height(38.dp),
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterStart).size(38.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "关闭挥杆记录预览", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text(
                "挥杆记录预览",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
            Spacer(Modifier.align(Alignment.CenterEnd).size(38.dp))
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            GuestSwingHero()

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "先看看挥杆记录怎么保存",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "游客不会启动相机或生成记录；登录后再录制、保存缩略图，并把历史归档到账号。",
                    color = Color.White.copy(alpha = 0.66f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GuestSwingFeature(
                    imageRes = R.drawable.v2_quickadd_swing,
                    icon = Icons.Filled.PhoneIphone,
                    title = "手机自录",
                    subtitle = "登录后开始",
                    modifier = Modifier.weight(1f),
                )
                GuestSwingFeature(
                    imageRes = R.drawable.v2_quickadd_video,
                    icon = Icons.Filled.Watch,
                    title = "Watch 遥控",
                    subtitle = "远程触发",
                    modifier = Modifier.weight(1f),
                )
                GuestSwingFeature(
                    imageRes = R.drawable.homepage_analysis,
                    icon = Icons.Filled.CalendarMonth,
                    title = "历史回看",
                    subtitle = "账号归档",
                    modifier = Modifier.weight(1f),
                )
            }

            GuestSwingFlowCard(Modifier.padding(horizontal = 24.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth().v2NavigationBarPadding().padding(horizontal = 24.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp).clip(CircleShape).background(Color.White)
                    .clickable(onClick = onLogin),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.LockOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                Spacer(Modifier.size(8.dp))
                Text("登录/注册后开始记录", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(42.dp).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("继续浏览", color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun GuestSwingHero() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(178.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(26.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.v2_quickadd_swing),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.04f),
                        Color.Black.copy(alpha = 0.22f),
                        Color.Black.copy(alpha = 0.72f),
                    ),
                ),
            ),
        )
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.height(26.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.90f))
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color.Black.copy(alpha = 0.82f), modifier = Modifier.size(11.dp))
                    Text("游客预览", color = Color.Black.copy(alpha = 0.82f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.38f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            Text("相机与历史只在登录后开启", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
                "这里展示流程，不创建本地挥杆数据",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun GuestSwingFeature(
    imageRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.height(108.dp).clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp)),
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.06f),
                        Color.Black.copy(alpha = 0.30f),
                        Color.Black.copy(alpha = 0.76f),
                    ),
                ),
            ),
        )
        Column(Modifier.fillMaxSize().padding(9.dp)) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = Color.White.copy(alpha = 0.72f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun GuestSwingFlowCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, tint = Color.Black.copy(alpha = 0.82f), modifier = Modifier.size(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("登录后记录流程", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "游客只看流程，不会请求相机权限或创建本地视频。",
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GuestSwingFlowStep("1", "打开相机", "手机 / Watch", Modifier.weight(1f))
            GuestSwingFlowStep("2", "自动保存", "缩略图与片段", Modifier.weight(1f))
            GuestSwingFlowStep("3", "回看筛选", "收藏 / 成长池", Modifier.weight(1f))
        }
    }
}

@Composable
private fun GuestSwingFlowStep(
    index: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.24f)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.86f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(index, color = Color.Black.copy(alpha = 0.82f), fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = Color.White.copy(alpha = 0.64f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun LatestClipThumbnail(
    model: String?,
    clipCount: Int,
    onOpenClips: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(width = 64.dp, height = 80.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(width = 56.dp, height = 72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .border(2.dp, Color.White.copy(alpha = 0.60f), RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenClips),
        ) {
            LatestClipThumbnailPlaceholder(Modifier.fillMaxSize())
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (clipCount > 0) {
                Text(
                    text = clipCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(DSColors.flagRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "关闭缩略图", tint = Color.White, modifier = Modifier.size(9.dp))
        }
    }
}

@Composable
private fun LatestClipThumbnailPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color(0xFFD1D1D6)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.SportsGolf,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun LocalRecordingBadge(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.50f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.PhoneIphone,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.80f),
            modifier = Modifier.size(12.dp),
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isRecording) DSColors.flagRed else DSColors.fairway),
        )
        Text(
            if (isRecording) "本机录制中" else "手机就绪",
            color = Color.White.copy(alpha = 0.80f),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun RecordingModeSwitcher(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .alpha(if (isRecording) 0.45f else 1f)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .padding(4.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White)
                .height(30.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.PhoneIphone,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(12.dp),
            )
            Text("手机", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PermissionState(
    permissionLabel: String,
    showSettingsAction: Boolean,
    errorMessage: String?,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("需要${permissionLabel}权限才能录制挥杆", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(
            if (showSettingsAction) "请在系统设置中开启${permissionLabel}权限。" else "授权后会直接进入拍摄模式并保留现场声音。",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
        )
        errorMessage?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                color = Color(0xFFFFD9D9),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }
        IconButton(
            onClick = if (showSettingsAction) onOpenSettings else onRequest,
            modifier = Modifier.size(64.dp).clip(CircleShape).background(DSColors.fairway),
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White)
        }
        Text(
            if (showSettingsAction) "打开设置" else "授权录制",
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
internal fun swingRecordingClockText(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun TopOverlay(
    clipCount: Int,
    isRecording: Boolean,
    recordingClock: String,
    onClose: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenClips: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility.only(WindowInsetsSides.Top))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CircleIconButton(icon = Icons.Filled.Close, onClick = onClose)
        Spacer(Modifier.weight(1f))
        if (isRecording) {
            StatusPill(text = "录制中 $recordingClock", color = DSColors.flagRed)
        } else if (clipCount > 0) {
            StatusPill(text = "$clipCount 段已录", color = Color.Black)
        }
        Spacer(Modifier.weight(1f))
        CircleIconButton(icon = Icons.Filled.Cameraswitch, onClick = onSwitchCamera, enabled = !isRecording)
        Spacer(Modifier.size(8.dp))
        CircleIconButton(icon = Icons.AutoMirrored.Filled.FormatListBulleted, onClick = onOpenClips, enabled = !isRecording)
    }
}

@Composable
private fun BottomOverlay(
    isRecording: Boolean,
    saving: Boolean,
    savedToast: Boolean,
    error: String?,
    notice: String?,
    onRecordTap: () -> Unit,
    onOpenClips: () -> Unit,
    onRequestLogin: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .v2NavigationBarPadding()
            .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        when {
            error != null -> ErrorPill(text = error, onRequestLogin = onRequestLogin)
            notice != null -> Text(
                text = notice,
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
            savedToast -> SavedClipToast(onOpenClips = onOpenClips)
            saving -> Text(
                text = "正在保存…",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        Text(
            text = SWING_RECORDING_LIMIT_HINT,
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp),
        )

        Box(
            modifier = Modifier
                .padding(top = 18.dp)
                .size(80.dp)
                .clip(CircleShape)
                .semantics {
                    contentDescription = if (isRecording) "停止录制" else "开始录制"
                }
                .testTag("swing-record-button")
                .clickable(enabled = !saving, onClick = onRecordTap),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(76.dp).border(4.dp, Color.White, CircleShape))
            if (isRecording) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DSColors.flagRed),
                )
            } else {
                Box(
                    Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(DSColors.flagRed),
                )
            }
        }
    }
}

@Composable
private fun ErrorPill(
    text: String,
    onRequestLogin: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(DSColors.flagRed.copy(alpha = 0.75f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = text, color = Color.White, fontSize = 13.sp)
        if (onRequestLogin != null) {
            Text(
                text = "去登录",
                color = Color(0xFFFFD166),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onRequestLogin),
            )
        }
    }
}

@Composable
private fun SavedClipToast(onOpenClips: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.70f))
            .clickable(onClick = onOpenClips)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
        Text("视频已保存", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = if (enabled) 0.50f else 0.25f)),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = if (enabled) 1f else 0.35f), modifier = Modifier.size(16.dp))
    }
}

private fun bindSwingCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    lensFacing: Int,
    onReady: (VideoCapture<Recorder>, Camera) -> Unit,
    onError: (String) -> Unit,
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener(
        {
            val provider = runCatching { cameraProviderFuture.get() }.getOrNull()
            if (provider == null) {
                onError("相机启动失败")
                return@addListener
            }
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.higherQualityOrLowerThan(Quality.HD)))
                .build()
            val capture = VideoCapture.withOutput(recorder)
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            }.onSuccess { camera ->
                onReady(capture, camera)
            }.onFailure {
                onError(swingCameraThrowableError("无法绑定相机，请检查相机权限或稍后重试", it))
            }
        },
        ContextCompat.getMainExecutor(context),
    )
}

internal fun swingCameraError(base: String, debugDetail: String?): String {
    return swingCameraError(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun swingCameraError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

internal fun swingCameraThrowableError(base: String, error: Throwable): String {
    if (error is CancellationException) throw error
    return swingCameraError(base, error.message)
}

internal fun swingVideoHasAudioTrack(file: File): Boolean {
    if (!file.isFile || file.length() <= 0L) return false
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            ?.equals("yes", ignoreCase = true) == true
    } catch (_: Throwable) {
        false
    } finally {
        runCatching { retriever.release() }
    }
}

private fun cleanupImportedSwingClip(result: V2VideoFileStore.ImportResult) {
    deleteFileCountingFreedBytes(File(result.absolutePath))
    result.thumbnailPath?.let { deleteFileCountingFreedBytes(File(it)) }
}

@SuppressLint("MissingPermission")
private fun startSwingRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    onStarted: (Recording) -> Unit,
    onFinished: (File, Boolean, Boolean) -> Unit,
    onError: (String) -> Unit,
) {
    val file = V2VideoFileStore.createCameraOutputFile(context)
    val outputOptions = FileOutputOptions.Builder(file)
        .setDurationLimitMillis(SWING_RECORDING_MAX_DURATION_MS)
        .setFileSizeLimit(SWING_RECORDING_MAX_BYTES)
        .build()
    val recording = runCatching {
        videoCapture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> Unit
                    is VideoRecordEvent.Finalize -> {
                        val reachedLimit = swingRecordingReachedLimit(event.error)
                        val interrupted = event.hasError() && !reachedLimit
                        if (!file.isFile || file.length() <= 0L) {
                            deleteFileCountingFreedBytes(file)
                            onError("录制失败，请重试")
                        } else {
                            onFinished(file, reachedLimit, interrupted)
                        }
                    }
                }
            }
    }.getOrElse {
        deleteFileCountingFreedBytes(file)
        onError("录制启动失败，请重试")
        return
    }
    onStarted(recording)
}

internal const val SWING_RECORDING_MAX_DURATION_MS = 10 * 60 * 1000L
internal const val SWING_RECORDING_MAX_BYTES = 1024L * 1024L * 1024L
internal const val SWING_RECORDING_LIMIT_HINT = "单段最长 10 分钟或 1GB"
internal const val SWING_RECORDING_LIMIT_NOTICE = "已达到单段录制上限，这段已保存"
internal const val SWING_RECORDING_INTERRUPTED_NOTICE = "录制已中断，这段已保存"

internal fun swingVideoIsPlayable(file: File): Boolean {
    if (!file.isFile || file.length() <= 0L) return false
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        durationMs > 0L
    } catch (_: Throwable) {
        false
    } finally {
        runCatching { retriever.release() }
    }
}

internal fun swingRecordingReachedLimit(error: Int): Boolean {
    return error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED ||
        error == VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED
}

private tailrec fun Context.findSwingActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findSwingActivity()
    else -> null
}

private fun Activity.hideSwingStatusBar() {
    WindowInsetsControllerCompat(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.statusBars())
    }
}
