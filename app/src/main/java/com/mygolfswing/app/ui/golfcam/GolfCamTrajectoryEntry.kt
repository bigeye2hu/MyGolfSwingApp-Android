package com.mygolfswing.app.ui.golfcam

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.R
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.GolfCamVideoConfig
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowState
import com.mygolfswing.app.ui.golfcam.model.GolfCamWorkflowStateStore
import com.mygolfswing.app.ui.golfcam.model.TrackStyleSelection
import com.mygolfswing.app.ui.golfcam.steps.GolfCamThumbnailCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val MIN_PRELOAD_VIDEO_DURATION_MS = 500L

internal const val GOLF_CAM_IMPORT_RESTORE_MESSAGE = "正在恢复上次编辑进度"
internal const val GOLF_CAM_IMPORT_DETECT_MESSAGE = "正在识别视频比例与帧率"
internal const val GOLF_CAM_IMPORT_GENERATE_PREVIEW_MESSAGE = "正在生成预览"
internal const val GOLF_CAM_IMPORT_VIDEO_CONFIG_ERROR_MESSAGE = "无法读取视频参数，请换一个视频或稍后重试"
internal const val GOLF_CAM_PICKER_LAUNCH_ERROR_MESSAGE = "无法打开相册，请稍后重试"

internal fun launchGolfCamVideoPicker(launch: () -> Unit): String? = try {
    launch()
    null
} catch (_: Throwable) {
    GOLF_CAM_PICKER_LAUNCH_ERROR_MESSAGE
}

internal sealed interface GolfCamVideoImportOutcome {
    data class Ready(val restoredPreviewReady: Boolean) : GolfCamVideoImportOutcome
    data class Failure(val message: String) : GolfCamVideoImportOutcome
}

internal suspend fun prepareGolfCamVideoImport(
    state: GolfCamWorkflowState,
    resetForNewVideo: Boolean,
    onStage: (String) -> Unit,
    assignVideo: () -> Unit,
    restore: (GolfCamWorkflowState) -> Boolean,
    detect: suspend () -> GolfCamVideoConfig,
): GolfCamVideoImportOutcome = try {
    if (resetForNewVideo) state.clearAll()
    assignVideo()
    onStage(GOLF_CAM_IMPORT_RESTORE_MESSAGE)
    val restored = restore(state)
    if (shouldDetectGolfCamVideoConfig(state.videoConfig)) {
        onStage(GOLF_CAM_IMPORT_DETECT_MESSAGE)
        state.updateVideoConfig(detect())
    }
    val restoredPreviewReady = restored && previewStateFromWorkflow(state) != null
    if (restoredPreviewReady) onStage(GOLF_CAM_IMPORT_GENERATE_PREVIEW_MESSAGE)
    GolfCamVideoImportOutcome.Ready(restoredPreviewReady)
} catch (error: CancellationException) {
    throw error
} catch (_: Throwable) {
    state.clearAll()
    GolfCamVideoImportOutcome.Failure(GOLF_CAM_IMPORT_VIDEO_CONFIG_ERROR_MESSAGE)
}

internal fun shouldDetectGolfCamVideoConfig(config: GolfCamVideoConfig?): Boolean {
    return config == null ||
        config.durationMs < 500L ||
        config.frameRate !in 1.0..120.0 ||
        config.visualWidth <= 1 ||
        config.visualHeight <= 1
}

internal fun golfCamThumbnailPreloadDurationMs(config: GolfCamVideoConfig?): Long? {
    return config?.durationMs?.takeIf { it >= MIN_PRELOAD_VIDEO_DURATION_MS }
}

internal fun workflowPreviewVideoConfig(config: GolfCamVideoConfig?): GolfCamVideoConfig? {
    val source = config ?: return null
    if (source.durationMs < MIN_PRELOAD_VIDEO_DURATION_MS) return null
    if (shouldDetectGolfCamVideoConfig(source)) return null
    val visualWidth = source.visualWidth
    val visualHeight = source.visualHeight
    val frameRate = source.frameRate.takeIf { it in 1.0..120.0 } ?: 30.0
    return source.copy(
        frameRate = frameRate,
        visualWidth = visualWidth,
        visualHeight = visualHeight,
    )
}

internal data class GolfCamEntryPreviewState(
    val selection: TrackStyleSelection,
    val videoConfig: GolfCamVideoConfig,
    val impactTimeMs: Long,
    val trimStartMs: Long,
    val trimEndMs: Long?,
)

internal fun previewStateFromWorkflow(state: GolfCamWorkflowState): GolfCamEntryPreviewState? {
    val videoConfig = workflowPreviewVideoConfig(state.videoConfig) ?: return null
    if (!state.isComplete) return null
    val trim = state.trimResult ?: return null
    val impact = state.impactFrame ?: return null
    val selection = state.trackStyle ?: return null
    return GolfCamEntryPreviewState(
        selection = selection,
        videoConfig = videoConfig,
        impactTimeMs = impact.timeMs,
        trimStartMs = trim.startMs,
        trimEndMs = trim.endMs,
    )
}

/**
 * GolfCam 轨迹绘制主入口（对齐 iOS V2TrajectorySourceSheet + handleTrajectoryVideoSelected）。
 *
 * 状态机：
 * - 无视频 → 显示「选择视频」底部弹窗
 * - 有视频、未完成 → 进入 FlightPathUnifiedContainer
 * - 有视频、已完成 → 进入 OverlayPreviewScreen（导出预览）
 */
@Composable
fun GolfCamTrajectoryEntry(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { GolfCamWorkflowState() }
    var pickedUri: Uri? by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pickerError by remember { mutableStateOf<String?>(null) }
    val previewState = previewStateFromWorkflow(state)

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            pickerError = null
            statusMessage = GOLF_CAM_IMPORT_RESTORE_MESSAGE
            scope.launch {
                when (
                    val outcome = prepareGolfCamVideoImport(
                        state = state,
                        resetForNewVideo = pickedUri != uri,
                        onStage = { statusMessage = it },
                        assignVideo = { state.videoUri = uri },
                        restore = { target -> GolfCamWorkflowStateStore.restore(uri, target) },
                        detect = {
                            withContext(Dispatchers.IO) {
                                detectGolfCamVideoConfig(context, uri)
                            }
                        },
                    )
                ) {
                    is GolfCamVideoImportOutcome.Ready -> {
                        pickedUri = uri
                        showPreview = outcome.restoredPreviewReady
                        statusMessage = null
                    }
                    is GolfCamVideoImportOutcome.Failure -> {
                        pickedUri = null
                        showPreview = false
                        statusMessage = null
                        pickerError = outcome.message
                    }
                }
            }
        }
    }

    fun openVideoPicker(showInlineError: Boolean = true) {
        pickerError = null
        val launchError = launchGolfCamVideoPicker {
            videoPicker.launch(arrayOf("video/*"))
        }
        if (launchError != null) {
            statusMessage = null
            if (showInlineError) {
                pickerError = launchError
            } else {
                Toast.makeText(context, launchError, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(
        pickedUri,
        state.trimResult,
        state.videoConfig,
        state.impactFrame,
        state.impactPosition,
        state.landing,
        state.flightCurve,
        state.trackStyle,
    ) {
        val uri = pickedUri ?: return@LaunchedEffect
        if (state.videoConfig != null ||
            state.trimResult != null ||
            state.impactFrame != null ||
            state.impactPosition != null ||
            state.landing != null ||
            state.flightCurve != null ||
            state.trackStyle != null
        ) {
            GolfCamWorkflowStateStore.save(uri, state)
        }
    }

    LaunchedEffect(pickedUri, golfCamThumbnailPreloadDurationMs(state.videoConfig)) {
        val uri = pickedUri ?: return@LaunchedEffect
        val preloadDurationMs = golfCamThumbnailPreloadDurationMs(state.videoConfig) ?: return@LaunchedEffect
        GolfCamThumbnailCache.preloadVideo(context, uri, preloadDurationMs)
    }

    when {
        showPreview && pickedUri != null && previewState != null -> {
            val uri = pickedUri ?: return
            GolfCamOverlayPreviewScreen(
                videoUri = uri,
                selection = previewState.selection,
                initialVideoConfig = previewState.videoConfig,
                impactTimeMs = previewState.impactTimeMs,
                trimStartMs = previewState.trimStartMs,
                trimEndMs = previewState.trimEndMs,
                onDismiss = onDismiss,
                onReEdit = { showPreview = false },
            )
        }
        pickedUri != null -> {
            val uri = pickedUri ?: return
            FlightPathUnifiedContainer(
                videoUri = uri,
                state = state,
                initialStep = state.resumeStep,
                onComplete = { selection ->
                    state.updateTrackStyle(selection)
                    showPreview = previewStateFromWorkflow(state) != null
                },
                onReselect = {
                    openVideoPicker(showInlineError = false)
                },
                onCancel = onDismiss,
            )
        }
        else -> {
            TrajectorySourceSheet(
                statusMessage = statusMessage,
                errorMessage = pickerError,
                onPickVideo = { openVideoPicker() },
                onDismiss = onDismiss,
            )
        }
    }
}

// ─── 底部弹窗（对齐 iOS V2TrajectorySourceSheet）────────────────────────────────

@Composable
internal fun TrajectorySourceSheet(
    statusMessage: String?,
    errorMessage: String?,
    onPickVideo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isBusy = statusMessage != null

    // 进入动画
    var launched by remember { mutableStateOf(false) }
    val dimAlpha by animateFloatAsState(
        targetValue = if (launched) 0.42f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "dim",
    )
    val sheetOffsetFraction by animateFloatAsState(
        targetValue = if (launched) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 350f),
        label = "sheetOffset",
    )
    LaunchedEffect(Unit) { launched = true }

    // 拖动偏移
    var dragDelta by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── 半透明遮罩 ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimAlpha))
                .then(
                    if (!isBusy) Modifier.clickable(onClick = onDismiss) else Modifier,
                ),
        )

        // ── 底部 Sheet ──
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        SheetCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset { IntOffset(0, ((sheetOffsetFraction * 560 + dragDelta.coerceAtLeast(0f)) * density).roundToInt()) }
                .then(
                    if (!isBusy) Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragDelta > 80f) onDismiss() else dragDelta = 0f
                            },
                            onDragCancel = { dragDelta = 0f },
                        ) { _, delta -> dragDelta = (dragDelta + delta).coerceAtLeast(0f) }
                    } else Modifier,
                ),
            statusMessage = statusMessage,
            errorMessage = errorMessage,
            isBusy = isBusy,
            onPickVideo = onPickVideo,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun SheetCard(
    modifier: Modifier,
    statusMessage: String?,
    errorMessage: String?,
    isBusy: Boolean,
    onPickVideo: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 28.dp,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                ambientColor = DSColors.deepGreen.copy(alpha = 0.18f),
                spotColor = DSColors.deepGreen.copy(alpha = 0.18f),
            )
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(DSColors.warmWhite)
            .testTag("golfcam-source-sheet")
            .navigationBarsPadding(),
    ) {
        // 拖动手柄
        Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.75f)),
            )
        }

        // Hero 背景图
        HeroHeader(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))

        // 内容区
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isBusy) {
                ProcessingCard(statusMessage = statusMessage)
            } else {
                SourceButton(
                    title = "从相册选择",
                    subtitle = "导入击球视频，进入裁剪、击球点、落点与轨迹样式调整",
                    onClick = onPickVideo,
                )
                errorMessage?.let { PickerErrorStrip(it) }
                InfoStrip()
            }
        }

        // 取消按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 16.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DSColors.sandBeige.copy(alpha = if (isBusy) 0.58f else 1f))
                .then(if (!isBusy) Modifier.clickable(onClick = onDismiss) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (isBusy) "正在处理" else "取消",
                color = if (isBusy) DSColors.shadowGray else DSColors.inkGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─── Hero 头部 ────────────────────────────────────────────────────────────────

@Composable
private fun HeroHeader(modifier: Modifier = Modifier) {
    Box(
            modifier = modifier
                .fillMaxWidth()
                .height(156.dp)
                .clip(RoundedCornerShape(22.dp))
                .testTag("golfcam-source-hero"),
        contentAlignment = Alignment.BottomStart,
    ) {
        Image(
            painter = painterResource(id = R.drawable.v2_hero_grass_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // 渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DSColors.deepGreen.copy(alpha = 0.24f),
                            DSColors.deepGreen.copy(alpha = 0.82f),
                        ),
                    ),
                ),
        )
        // 文字内容
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // 球路标注徽章
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.TravelExplore, null, tint = Color.White, modifier = Modifier.size(13.dp))
                Text("球路标注", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("创建飞行轨迹", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "按 V1 完整流程完成裁剪、击球帧、落点与分享预览",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

// ─── 从相册选择按钮 ───────────────────────────────────────────────────────────

@Composable
private fun SourceButton(title: String, subtitle: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 96.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = DSColors.deepGreen.copy(alpha = 0.16f),
                spotColor = DSColors.deepGreen.copy(alpha = 0.16f),
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(DSColors.deepGreen, DSColors.fairway)))
            .clickable(onClick = onClick)
            .testTag("golfcam-source-album")
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 3,
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

// ─── 信息条 ─────────────────────────────────────────────────────────────────

@Composable
private fun PickerErrorStrip(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.flagRed.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.ErrorOutline, null, tint = DSColors.flagRed, modifier = Modifier.size(14.dp))
        Text(
            message,
            color = DSColors.flagRed,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun InfoStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.82f))
            .testTag("golfcam-source-info")
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.AutoAwesome, null, tint = DSColors.shadowGray, modifier = Modifier.size(12.dp))
        Text(
            "系统会自动识别视频比例与帧率，并恢复同一视频的上次编辑进度",
            color = DSColors.shadowGray,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

// ─── 处理中卡片 ───────────────────────────────────────────────────────────────

@Composable
private fun ProcessingCard(statusMessage: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 92.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = DSColors.deepGreen.copy(alpha = 0.06f),
                spotColor = DSColors.deepGreen.copy(alpha = 0.06f),
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, DSColors.listCardBorder, RoundedCornerShape(20.dp))
            .testTag("golfcam-source-processing")
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DSColors.mistGreen),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = DSColors.fairway,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                statusMessage ?: "正在读取相册视频",
                color = DSColors.inkGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "大视频可能需要几秒钟，完成后会自动进入轨迹绘制",
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}
