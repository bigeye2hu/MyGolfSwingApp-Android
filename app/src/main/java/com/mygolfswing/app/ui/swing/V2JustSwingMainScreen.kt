package com.mygolfswing.app.ui.swing

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mygolfswing.app.data.ServiceLocator
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
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var savedToast by remember { mutableStateOf(false) }
    var pendingClose by remember { mutableStateOf(false) }
    var cameraPermissionDeniedPermanently by remember { mutableStateOf(false) }
    var sessionClipCount by remember { mutableIntStateOf(0) }
    var latestClipPreviewModel by remember { mutableStateOf<String?>(null) }
    var showLatestClipThumbnail by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        cameraPermissionDeniedPermanently = !granted && context.isPermissionPermanentlyDenied(Manifest.permission.CAMERA)
        if (!granted) error = "需要相机权限才能录制挥杆"
    }

    fun requestCameraPermission() {
        runCatching {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }.onFailure {
            error = "无法请求相机权限，请稍后重试"
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) requestCameraPermission()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                hasCameraPermission = granted
                if (granted) {
                    cameraPermissionDeniedPermanently = false
                    error = null
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(hasCameraPermission, lensFacing) {
        if (!hasCameraPermission) return@LaunchedEffect
        bindSwingCamera(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            lensFacing = lensFacing,
            onReady = {
                videoCapture = it
                error = null
            },
            onError = { error = it },
        )
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
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.close()
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }

    fun closeScreen() {
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

    fun toggleCamera() {
        if (activeRecording != null || saving) return
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
    }

    fun saveRecordedFile(file: File) {
        scope.launch {
            saving = true
            var result: V2VideoFileStore.ImportResult? = null
            try {
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
                        sessionClipCount += 1
                        latestClipPreviewModel = swingClipPreviewModel(result.thumbnailPath, result.absolutePath)
                        showLatestClipThumbnail = latestClipPreviewModel != null
                        savedToast = true
                        error = null
                        onClipSaved(clipId)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                deleteFileCountingFreedBytes(file)
                result?.let(::cleanupImportedSwingClip)
                error = swingCameraError("录制保存失败，请稍后重试", e.message)
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
                error = null
            },
            onFinished = { file ->
                activeRecording = null
                saveRecordedFile(file)
            },
            onError = { message ->
                activeRecording = null
                error = message
                if (pendingClose) onBack()
            },
        )
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            PermissionState(
                showSettingsAction = cameraPermissionDeniedPermanently,
                errorMessage = error,
                onRequest = { requestCameraPermission() },
                onOpenSettings = {
                    if (!context.openAppSettings()) {
                        error = "系统设置暂时无法打开，请稍后重试"
                    }
                },
            )
        }

        TopOverlay(
            clipCount = sessionClipCount,
            isRecording = activeRecording != null,
            onClose = ::closeScreen,
            onSwitchCamera = ::toggleCamera,
            onOpenClips = onOpenClips,
        )

        if (hasCameraPermission) {
            RecordingModeSwitcher(
                isRecording = activeRecording != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
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
        }

        BottomOverlay(
            isRecording = activeRecording != null,
            saving = saving,
            savedToast = savedToast,
            error = error,
            onRecordTap = ::toggleRecording,
            onOpenClips = onOpenClips,
            onRequestLogin = onRequestLogin.takeUnless { isLoggedIn },
        )
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
        Text("需要相机权限才能录制挥杆", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(
            if (showSettingsAction) "请在系统设置中开启相机权限。" else "授权后会直接进入拍摄模式。",
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
            if (showSettingsAction) "打开设置" else "授权相机",
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun TopOverlay(
    clipCount: Int,
    isRecording: Boolean,
    onClose: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenClips: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CircleIconButton(icon = Icons.Filled.Close, onClick = onClose)
        Spacer(Modifier.weight(1f))
        if (isRecording) {
            StatusPill(text = "录制中", color = DSColors.flagRed)
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
        if (!isRecording && !saving && !savedToast && error == null) {
            RecordingModePill()
        }

        when {
            error != null -> ErrorPill(text = error, onRequestLogin = onRequestLogin)
            savedToast -> SavedClipToast(onOpenClips = onOpenClips)
            saving -> Text(
                text = "正在保存…",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 18.dp)
                .size(80.dp)
                .clip(CircleShape)
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
private fun RecordingModePill() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.52f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF8FE3B1),
            modifier = Modifier.size(14.dp),
        )
        Text(
            "手机录制模式 · 支持逐帧回看",
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
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
    onReady: (VideoCapture<Recorder>) -> Unit,
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
            }.onSuccess {
                onReady(capture)
            }.onFailure {
                onError(swingCameraError("无法绑定相机，请检查相机权限或稍后重试", it.message))
            }
        },
        ContextCompat.getMainExecutor(context),
    )
}

private fun swingCameraError(base: String, debugDetail: String?): String {
    val detail = debugDetail.takeIf { ServiceLocator.isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
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
    onFinished: (File) -> Unit,
    onError: (String) -> Unit,
) {
    val file = V2VideoFileStore.createCameraOutputFile(context)
    val outputOptions = FileOutputOptions.Builder(file).build()
    val recording = runCatching {
        videoCapture.output
            .prepareRecording(context, outputOptions)
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> Unit
                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            deleteFileCountingFreedBytes(file)
                            onError("录制失败，请重试")
                        } else {
                            onFinished(file)
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
