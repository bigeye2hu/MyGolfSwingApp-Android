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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.data.video.deleteFileCountingFreedBytes
import com.mygolfswing.app.ui.common.isPermissionPermanentlyDenied
import com.mygolfswing.app.ui.common.openAppSettings
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/** 挥杆录制入口（对齐 iOS `JustSwingMainView`：进入后停留在相机页连续录制）。 */
@Composable
fun V2JustSwingMainScreen(onBack: () -> Unit) {
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        cameraPermissionDeniedPermanently = !granted && context.isPermissionPermanentlyDenied(Manifest.permission.CAMERA)
        if (!granted) error = "需要相机权限才能录制挥杆"
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
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
            delay(1_500)
            savedToast = false
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
            val result = V2VideoFileStore.adoptLocalFile(context, file)
            if (result == null) {
                deleteFileCountingFreedBytes(file)
                error = "录制失败，请重试"
            } else {
                V2SwingSessionStore.addClipFromVideoImport(result.absolutePath, result.thumbnailPath, result.durationSec)
                sessionClipCount += 1
                savedToast = true
                error = null
            }
            saving = false
            if (pendingClose) onBack()
        }
    }

    fun toggleRecording() {
        val currentRecording = activeRecording
        if (currentRecording != null) {
            currentRecording.stop()
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
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = { context.openAppSettings() },
            )
        }

        TopOverlay(
            clipCount = sessionClipCount,
            isRecording = activeRecording != null,
            onClose = ::closeScreen,
            onSwitchCamera = ::toggleCamera,
        )

        BottomOverlay(
            isRecording = activeRecording != null,
            saving = saving,
            savedToast = savedToast,
            error = error,
            onRecordTap = ::toggleRecording,
        )
    }
}

@Composable
private fun PermissionState(
    showSettingsAction: Boolean,
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
    }
}

@Composable
private fun BottomOverlay(
    isRecording: Boolean,
    saving: Boolean,
    savedToast: Boolean,
    error: String?,
    onRecordTap: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        when {
            error != null -> Text(
                text = error,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(DSColors.flagRed.copy(alpha = 0.75f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
            savedToast -> Text(
                text = "视频已保存",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
            saving -> Text(
                text = "正在保存…",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        IconButton(
            onClick = onRecordTap,
            enabled = !saving,
            modifier = Modifier.padding(top = 18.dp).size(78.dp).clip(CircleShape)
                .border(4.dp, Color.White, CircleShape)
                .background(if (isRecording) Color.White else DSColors.flagRed),
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.CameraAlt,
                contentDescription = if (isRecording) "停止录制" else "开始录制",
                tint = if (isRecording) DSColors.flagRed else Color.White,
                modifier = Modifier.size(34.dp),
            )
        }

        Text(
            text = if (isRecording) "点击停止" else "点击录制挥杆",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
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
        modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = if (enabled) 0.55f else 0.25f)),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = if (enabled) 1f else 0.35f))
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
                onError("无法绑定相机：${it.message ?: "未知错误"}")
            }
        },
        ContextCompat.getMainExecutor(context),
    )
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
