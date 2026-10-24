package com.mygolfswing.app.ui.interaction

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mygolfswing.app.data.friend.decodeV2QrArgb
import com.mygolfswing.app.data.friend.decodeV2QrLuma
import com.mygolfswing.app.data.friend.parseV2FriendQrUserId
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal fun V2FriendQrScannerDialog(
    onDismiss: () -> Unit,
    onScanned: (String) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        V2QrScannerScreen(
            title = "扫一扫",
            instruction = "将挥呗用户二维码放入框内",
            invalidMessage = "没有识别到挥呗用户二维码",
            allowTorch = false,
            validate = { content -> content?.let(::parseV2FriendQrUserId) },
            onDismiss = onDismiss,
            onScanned = onScanned,
        )
    }
}

@Composable
internal fun V2QrScannerScreen(
    title: String,
    instruction: String,
    invalidMessage: String,
    allowTorch: Boolean,
    inviteStyle: Boolean = false,
    validate: (String?) -> String?,
    onDismiss: () -> Unit,
    onScanned: (String) -> Unit,
    cameraPermissionOverride: Boolean? = null,
    previewOverride: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val delivered = remember { AtomicBoolean(false) }
    var torchOn by remember { mutableStateOf(false) }
    var activeCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            cameraPermissionOverride ?: (
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                ),
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    val scanningEnabled = remember { AtomicBoolean(true) }

    fun deliverQrContent(content: String?, fromAlbum: Boolean = false) {
        val validated = validate(content)
        if (validated == null) {
            error = if (fromAlbum && inviteStyle && content == null) {
                "这张图片里没有邀请二维码"
            } else {
                invalidMessage
            }
            return
        }
        if (delivered.compareAndSet(false, true)) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onScanned(validated)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission && !inviteStyle) error = "需要相机权限才能扫码；也可以从相册识别"
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val content = withContext(Dispatchers.IO) { decodeQrFromImage(context, uri) }
            deliverQrContent(content, fromAlbum = true)
        }
    }

    LaunchedEffect(Unit, previewOverride) {
        if (previewOverride != null) return@LaunchedEffect
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (inviteStyle && cameraPermissionOverride == null) {
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    if (hasCameraPermission && previewOverride == null) {
        DisposableEffect(lifecycleOwner, previewView) {
            val analysisExecutor = Executors.newSingleThreadExecutor()
            val providerFuture = ProcessCameraProvider.getInstance(context)
            val mainExecutor = ContextCompat.getMainExecutor(context)
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(analysisExecutor) { image ->
                val content = runCatching { decodeQrFromCameraFrame(image) }.getOrNull()
                image.close()
                if (content == null || !scanningEnabled.get()) return@setAnalyzer
                val validated = validate(content)
                if (validated != null && delivered.compareAndSet(false, true)) {
                    mainExecutor.execute {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onScanned(validated)
                    }
                } else if (inviteStyle && validated == null && scanningEnabled.compareAndSet(true, false)) {
                    mainExecutor.execute { error = invalidMessage }
                }
            }
            providerFuture.addListener(
                {
                    runCatching {
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        provider.unbindAll()
                        activeCamera = provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                        activeCamera?.cameraControl?.enableTorch(torchOn)
                    }.onFailure { error = "相机暂时无法启动，请从相册识别" }
                },
                mainExecutor,
            )
            onDispose {
                analysis.clearAnalyzer()
                analysisExecutor.shutdownNow()
                activeCamera = null
                if (providerFuture.isDone) runCatching { providerFuture.get().unbindAll() }
            }
        }
    }

    LaunchedEffect(torchOn, activeCamera) {
        activeCamera?.cameraControl?.enableTorch(torchOn)
    }

    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize().background(Color.Black).testTag("qr-scanner-screen")) {
        if (hasCameraPermission) {
            if (previewOverride != null) {
                previewOverride()
            } else {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            }
            if (!inviteStyle) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        } else if (inviteStyle) {
            InviteScannerPermissionDenied(
                onOpenSettings = { openV2QrScannerAppSettings(context) },
            )
        }

        if (inviteStyle) InviteScannerMask()

        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                .then(if (inviteStyle) Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
                else Modifier.padding(horizontal = 12.dp, vertical = 18.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScannerRoundButton(
                icon = Icons.Filled.Close,
                contentDescription = "关闭",
                onClick = onDismiss,
                inviteStyle = inviteStyle,
            )
            Text(
                title,
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = if (inviteStyle) 17.sp else 18.sp,
                fontWeight = if (inviteStyle) FontWeight.SemiBold else FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (allowTorch && (hasCameraPermission || inviteStyle)) {
                ScannerRoundButton(
                    icon = if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = if (torchOn) "关闭手电筒" else "打开手电筒",
                    onClick = { torchOn = !torchOn },
                    inviteStyle = inviteStyle,
                )
            } else {
                Spacer(Modifier.size(if (inviteStyle) 40.dp else 48.dp))
            }
        }

        if (inviteStyle) {
            if (hasCameraPermission) {
                Text(
                    instruction,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 116.dp)
                        .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 36.dp)
                    .size(width = 72.dp, height = 64.dp).clickable { galleryLauncher.launch("image/*") }
                    .testTag("invite-scanner-album"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text("相册", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Box(
                    modifier = Modifier.size(268.dp).border(3.dp, DSColors.trophyGold, RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!hasCameraPermission) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(72.dp))
                    }
                }
                Text(
                    if (hasCameraPermission) instruction else "相机权限未开启",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                error?.let { Text(it, color = Color(0xFFFFC7BE), fontSize = 13.sp) }
                if (!hasCameraPermission) {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                    ) { Text("允许相机扫码") }
                }
            }
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 36.dp, vertical = 36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DSColors.deepGreen),
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("从相册识别", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (inviteStyle && error != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                error = null
                scanningEnabled.set(true)
            },
            title = { Text("提示") },
            text = { Text(error.orEmpty()) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    error = null
                    scanningEnabled.set(true)
                }) { Text("确定") }
            },
        )
    }
}

@Composable
private fun ScannerRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    inviteStyle: Boolean,
) {
    Box(
        modifier = Modifier.size(if (inviteStyle) 40.dp else 48.dp).clip(CircleShape)
            .background(Color.Black.copy(alpha = if (inviteStyle) 0.35f else 0.45f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(if (inviteStyle) 16.dp else 24.dp),
        )
    }
}

@Composable
private fun InviteScannerMask() {
    Canvas(
        modifier = Modifier.fillMaxSize().graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }.testTag("invite-scanner-window"),
    ) {
        val side = minOf(size.width, size.height) * 0.68f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f - 36.dp.toPx()
        val corner = 18.dp.toPx()
        drawRect(Color.Black.copy(alpha = 0.45f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            blendMode = BlendMode.Clear,
        )
        drawRoundRect(
            color = DSColors.fairway,
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

@Composable
private fun InviteScannerPermissionDenied(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(42.dp))
        Text("需要相机权限才能扫码", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(Color.White)
                .clickable(onClick = onOpenSettings).padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("去设置", color = DSColors.deepGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

internal fun openV2QrScannerAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

internal fun decodeQrFromImage(context: Context, uri: Uri): String? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > 2_048 || bounds.outHeight / sampleSize > 2_048) sampleSize *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: return null
    return try {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        decodeV2QrArgb(bitmap.width, bitmap.height, pixels)
    } finally {
        bitmap.recycle()
    }
}

internal fun decodeQrFromCameraFrame(image: ImageProxy): String? {
    val plane = image.planes.firstOrNull() ?: return null
    val width = image.width
    val height = image.height
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    if (rowStride <= 0 || pixelStride <= 0) return null
    val buffer = plane.buffer.duplicate()
    val base = buffer.position()
    val lastIndex = base.toLong() + (height - 1L) * rowStride + (width - 1L) * pixelStride
    if (lastIndex >= buffer.limit()) return null
    val luma = ByteArray(width * height)
    var output = 0
    for (row in 0 until height) {
        val rowStart = base + row * rowStride
        for (column in 0 until width) {
            luma[output++] = buffer.get(rowStart + column * pixelStride)
        }
    }
    return decodeV2QrLuma(width, height, luma)
}
