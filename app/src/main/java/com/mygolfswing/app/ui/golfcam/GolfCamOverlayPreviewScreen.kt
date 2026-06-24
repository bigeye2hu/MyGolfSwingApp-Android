package com.mygolfswing.app.ui.golfcam

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.ArcLengthTable
import com.mygolfswing.app.ui.golfcam.model.BezierPath
import com.mygolfswing.app.ui.golfcam.model.NormalizedPoint
import com.mygolfswing.app.ui.golfcam.model.TrackStyle
import com.mygolfswing.app.ui.golfcam.model.TrackStyleSelection
import com.mygolfswing.app.ui.golfcam.model.TrajectoryAlgorithmKind
import com.mygolfswing.app.ui.golfcam.model.TrajectoryMotionModel
import com.mygolfswing.app.ui.golfcam.model.TrajectoryTiming
import com.mygolfswing.app.ui.golfcam.render.DistanceLabelRenderSpec
import com.mygolfswing.app.ui.golfcam.render.DistanceLabelTextMetrics
import com.mygolfswing.app.ui.golfcam.render.TrajectoryAnimationOverlay
import com.mygolfswing.app.ui.golfcam.render.TrajectoryOverlayFrameSampler
import com.mygolfswing.app.ui.golfcam.render.TrajectoryOverlayRenderSpec
import com.mygolfswing.app.ui.golfcam.render.TrajectoryRibbonRenderer
import com.mygolfswing.app.ui.golfcam.render.VideoDisplayGeometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt

private data class VideoPlaybackConfig(
    val frameRate: Double = 30.0,
    val aspectRatio: Float = 9f / 16f,
    val visualWidth: Int = 1080,
    val visualHeight: Int = 1920,
)

internal data class ExportVideoDimensions(
    val width: Int,
    val height: Int,
)

internal enum class ExportResolution(val displayName: String, val landscapeWidth: Int, val landscapeHeight: Int) {
    HD_1080P("1080p", 1920, 1080),
    UHD_4K("4K", 3840, 2160);

    fun dimensions(isPortrait: Boolean): ExportVideoDimensions {
        return if (isPortrait) {
            ExportVideoDimensions(width = landscapeHeight, height = landscapeWidth)
        } else {
            ExportVideoDimensions(width = landscapeWidth, height = landscapeHeight)
        }
    }

    fun displaySize(isPortrait: Boolean): String {
        val size = dimensions(isPortrait)
        return "${size.width} × ${size.height}"
    }
}

internal data class ExportTrimRange(
    val startMs: Long,
    val endMs: Long,
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(1L)
}

private data class AudioTrackInfo(
    val sourceTrackIndex: Int,
    val format: MediaFormat,
)

internal fun exportVideoDimensions(rawWidth: Int, rawHeight: Int, rotationDegrees: Int): ExportVideoDimensions {
    val safeWidth = rawWidth.coerceAtLeast(1)
    val safeHeight = rawHeight.coerceAtLeast(1)
    return if (rotationDegrees == 90 || rotationDegrees == 270) {
        ExportVideoDimensions(width = safeHeight, height = safeWidth)
    } else {
        ExportVideoDimensions(width = safeWidth, height = safeHeight)
    }
}

internal fun targetExportVideoDimensions(resolution: ExportResolution, isPortrait: Boolean): ExportVideoDimensions {
    return resolution.dimensions(isPortrait)
}

internal fun effectiveExportFrameRotation(
    metadataRotationDegrees: Int,
    frameWidth: Int,
    frameHeight: Int,
    exportWidth: Int,
    exportHeight: Int,
): Int {
    val rotation = ((metadataRotationDegrees % 360) + 360) % 360
    val frameAlreadyVisual = (rotation == 90 || rotation == 270) &&
        frameWidth == exportWidth &&
        frameHeight == exportHeight
    return if (frameAlreadyVisual) 0 else rotation
}

internal fun isAudioMimeType(mime: String?): Boolean = mime?.startsWith("audio/") == true

internal fun exportTrimRange(
    trimStartMs: Long,
    trimEndMs: Long?,
    sourceDurationMs: Long,
): ExportTrimRange {
    val safeDuration = sourceDurationMs.coerceAtLeast(1L)
    val start = trimStartMs.coerceIn(0L, (safeDuration - 1L).coerceAtLeast(0L))
    val end = (trimEndMs ?: safeDuration).coerceIn(start + 1L, safeDuration)
    return ExportTrimRange(startMs = start, endMs = end)
}

/**
 * 预览 + 导出屏（对齐 iOS OverlayPreviewView）。
 * 播放带轨迹叠加的视频，可导出为 MP4 保存到相册。
 */
@Composable
fun GolfCamOverlayPreviewScreen(
    videoUri: Uri,
    selection: TrackStyleSelection,
    impactTimeMs: Long = 0L,
    trimStartMs: Long = 0L,
    trimEndMs: Long? = null,
    onDismiss: () -> Unit,
    onReEdit: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var playing by remember { mutableStateOf(true) }  // 进入预览页自动播放
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var previewPositionMs by remember { mutableLongStateOf(0L) }
    var exporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportDone by remember { mutableStateOf(false) }
    var videoConfig by remember { mutableStateOf(VideoPlaybackConfig()) }
    var showPlaybackButton by remember { mutableStateOf(true) }
    var showExportSettings by remember { mutableStateOf(false) }
    var selectedExportResolution by remember { mutableStateOf(ExportResolution.HD_1080P) }

    LaunchedEffect(videoUri) {
        videoConfig = withContext(Dispatchers.IO) {
            readVideoPlaybackConfig(context, videoUri)
        }
    }

    val previewImpactMs = (impactTimeMs - trimStartMs).coerceAtLeast(0L)
    val flightTimeMs = (selection.flightCurve.landingPoint.flightTimeSec * 1000)
        .toLong()
        .coerceAtLeast(500L)
    val timing = TrajectoryTiming(
        impactTimeMs = previewImpactMs,
        landingTimeMs = previewImpactMs + flightTimeMs,
        fps = videoConfig.frameRate,
    )

    val player = remember(videoUri, trimStartMs, trimEndMs) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = if (trimStartMs > 0L || trimEndMs != null) {
                MediaItem.Builder()
                    .setUri(videoUri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(trimStartMs.coerceAtLeast(0L))
                            .apply { trimEndMs?.let { setEndPositionMs(it.coerceAtLeast(trimStartMs + 1L)) } }
                            .build(),
                    )
                    .build()
            } else {
                MediaItem.fromUri(videoUri)
            }
            setMediaItem(mediaItem)
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    LaunchedEffect(playing) {
        if (playing) player.play() else player.pause()
    }
    LaunchedEffect(player, previewImpactMs) {
        while (true) {
            previewPositionMs = player.currentPosition.coerceAtLeast(0L)
            elapsedMs = (previewPositionMs - previewImpactMs).coerceAtLeast(0L)
            delay(33L)
        }
    }
    LaunchedEffect(playing, showPlaybackButton) {
        if (playing && showPlaybackButton) {
            delay(3000L)
            showPlaybackButton = false
        }
    }

    val clippedDurationMs = remember(trimStartMs, trimEndMs, player.duration) {
        val fromTrim = trimEndMs?.let { (it - trimStartMs).coerceAtLeast(1L) }
        val playerDuration = player.duration.takeIf { it > 0L }
        fromTrim ?: playerDuration ?: 1L
    }
    val playbackProgress = (previewPositionMs.toFloat() / clippedDurationMs.toFloat()).coerceIn(0f, 1f)
    val impactProgress = (previewImpactMs.toFloat() / clippedDurationMs.toFloat()).coerceIn(0f, 1f)
    val landingProgress = ((previewImpactMs + flightTimeMs).toFloat() / clippedDurationMs.toFloat()).coerceIn(0f, 1f)
    val isPortrait = videoConfig.visualHeight >= videoConfig.visualWidth
    val isSource4K = max(videoConfig.visualWidth, videoConfig.visualHeight) >= 3800

    Box(modifier = Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        Column(modifier = Modifier.fillMaxSize()) {
            PreviewTopBar(
                selectedResolution = selectedExportResolution,
                onDismiss = onDismiss,
                onSettings = { showExportSettings = true },
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
            val displaySize = VideoDisplayGeometry.fit(
                containerWidth = maxWidth.value,
                containerHeight = maxHeight.value,
                videoAspectRatio = videoConfig.aspectRatio,
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(displaySize.width.dp, displaySize.height.dp)
                    .shadow(16.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.12f))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black),
            ) {
                AndroidView(
                    factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = false } },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                showPlaybackButton = !showPlaybackButton
                            }
                        },
                )

                TrajectoryAnimationOverlay(
                    bezier = selection.flightCurve.bezierPath,
                    timing = timing,
                    style = selection.style,
                    playing = playing,
                    elapsedMs = elapsedMs,
                )

                if (showPlaybackButton) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.42f))
                            .clickable {
                                playing = !playing
                                showPlaybackButton = true
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
            }
        }

            PreviewBottomPanel(
                playbackProgress = playbackProgress,
                impactProgress = impactProgress,
                landingProgress = landingProgress,
                currentTimeMs = previewPositionMs,
                totalTimeMs = clippedDurationMs,
                playing = playing,
                exporting = exporting,
                exportProgress = exportProgress,
                exportDone = exportDone,
                onTogglePlay = {
                    playing = !playing
                    showPlaybackButton = true
                },
                onEditFlightPath = onReEdit,
                onExport = {
                    if (!exporting && !exportDone) {
                        scope.launch {
                            exporting = true
                            exportProgress = 0f
                            player.pause()
                            playing = false
                            showPlaybackButton = true
                            try {
                                exportVideoWithTrajectory(
                                    context = context,
                                    videoUri = videoUri,
                                    selection = selection,
                                    timing = timing,
                                    trimStartMs = trimStartMs,
                                    trimEndMs = trimEndMs,
                                    exportResolution = selectedExportResolution,
                                    isPortrait = isPortrait,
                                    onProgress = { exportProgress = it },
                                )
                                exportDone = true
                            } finally {
                                exporting = false
                            }
                        }
                    }
                },
            )
        }

        if (showExportSettings) {
            ExportSettingsOverlay(
                selectedResolution = selectedExportResolution,
                isSource4K = isSource4K,
                isPortrait = isPortrait,
                onSelect = { selectedExportResolution = it },
                onDismiss = { showExportSettings = false },
            )
        }
    }
}

@Composable
private fun PreviewTopBar(
    selectedResolution: ExportResolution,
    onDismiss: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(DSColors.warmWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(DSColors.mistGreen.copy(alpha = 0.76f)),
        ) {
            Icon(Icons.Filled.Home, contentDescription = null, tint = DSColors.inkGreen, modifier = Modifier.size(21.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("轨迹总览", color = DSColors.inkGreen, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (selectedResolution == ExportResolution.UHD_4K) "4K" else "HD",
                color = if (selectedResolution == ExportResolution.UHD_4K) Color.Black else Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (selectedResolution == ExportResolution.UHD_4K) DSColors.trophyGold else DSColors.fairway)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(DSColors.mistGreen.copy(alpha = 0.76f)),
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = DSColors.fairway, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ExportSettingsOverlay(
    selectedResolution: ExportResolution,
    isSource4K: Boolean,
    isPortrait: Boolean,
    onSelect: (ExportResolution) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.34f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(DSColors.warmWhite)
                .clickable {}
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DSColors.mistGreen.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, tint = DSColors.fairway, modifier = Modifier.size(25.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("导出设置", color = DSColors.inkGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("选择导出视频的分辨率", color = DSColors.shadowGray, fontSize = 14.sp)
            }
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExportResolutionOption(
                    resolution = ExportResolution.HD_1080P,
                    selected = selectedResolution == ExportResolution.HD_1080P,
                    available = true,
                    reason = null,
                    isPortrait = isPortrait,
                    onSelect = onSelect,
                )
                ExportResolutionOption(
                    resolution = ExportResolution.UHD_4K,
                    selected = selectedResolution == ExportResolution.UHD_4K,
                    available = isSource4K,
                    reason = if (isSource4K) null else "需要4K源视频",
                    isPortrait = isPortrait,
                    onSelect = onSelect,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("当前选择:", color = DSColors.shadowGray, fontSize = 14.sp)
                Text(selectedResolution.displayName, color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "完成",
                color = DSColors.fairway,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DSColors.mistGreen.copy(alpha = 0.72f))
                    .clickable { onDismiss() }
                    .padding(horizontal = 28.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun ExportResolutionOption(
    resolution: ExportResolution,
    selected: Boolean,
    available: Boolean,
    reason: String?,
    isPortrait: Boolean,
    onSelect: (ExportResolution) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) DSColors.mistGreen.copy(alpha = 0.92f) else DSColors.cardWhite)
            .clickable(enabled = available) { onSelect(resolution) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    resolution.displayName,
                    color = if (available) DSColors.inkGreen else DSColors.shadowGray.copy(alpha = 0.55f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (resolution == ExportResolution.UHD_4K) {
                    Text(
                        "4K",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (available) DSColors.trophyGold else DSColors.shadowGray.copy(alpha = 0.45f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                resolution.displaySize(isPortrait),
                color = if (available) DSColors.shadowGray else DSColors.shadowGray.copy(alpha = 0.55f),
                fontSize = 12.sp,
            )
            reason?.let {
                Text(it, color = Color(0xFFB95D4D), fontSize = 11.sp)
            }
        }
        Text(
            when {
                selected -> "✓"
                available -> "○"
                else -> "锁"
            },
            color = if (selected) DSColors.fairway else if (available) DSColors.shadowGray.copy(alpha = 0.45f) else DSColors.shadowGray.copy(alpha = 0.55f),
            fontSize = if (selected) 24.sp else 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PreviewBottomPanel(
    playbackProgress: Float,
    impactProgress: Float,
    landingProgress: Float,
    currentTimeMs: Long,
    totalTimeMs: Long,
    playing: Boolean,
    exporting: Boolean,
    exportProgress: Float,
    exportDone: Boolean,
    onTogglePlay: () -> Unit,
    onEditFlightPath: () -> Unit,
    onExport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp), ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.08f))
            .background(DSColors.cardWhite)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PreviewProgressBar(
            playbackProgress = playbackProgress,
            impactProgress = impactProgress,
            landingProgress = landingProgress,
            currentTimeMs = currentTimeMs,
            totalTimeMs = totalTimeMs,
            playing = playing,
            onTogglePlay = onTogglePlay,
        )

        if (exporting) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("正在导出视频...", color = DSColors.inkGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                LinearProgressIndicator(
                    progress = { exportProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = DSColors.fairway,
                    trackColor = DSColors.mistGreen,
                )
            }
        } else if (exportDone) {
            Text("已保存到相册", color = DSColors.fairway, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PreviewFunctionButton(
                title = "飞行轨迹",
                icon = Icons.AutoMirrored.Filled.ShowChart,
                tint = DSColors.fairway,
                onClick = onEditFlightPath,
                modifier = Modifier.weight(1f),
            )
            PreviewFunctionButton(
                title = "杆头轨迹",
                icon = Icons.Filled.Edit,
                tint = Color(0xFF3E8FA5),
                devBadge = true,
                enabled = false,
                modifier = Modifier.weight(1f),
            )
            PreviewFunctionButton(
                title = "裁剪",
                icon = Icons.Filled.ContentCut,
                tint = DSColors.trophyGold,
                devBadge = true,
                enabled = false,
                modifier = Modifier.weight(1f),
            )
            PreviewFunctionButton(
                title = "导出",
                icon = Icons.Filled.Download,
                tint = DSColors.fairway,
                primary = true,
                enabled = !exporting && !exportDone,
                onClick = onExport,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PreviewProgressBar(
    playbackProgress: Float,
    impactProgress: Float,
    landingProgress: Float,
    currentTimeMs: Long,
    totalTimeMs: Long,
    playing: Boolean,
    onTogglePlay: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(32.dp)) {
            val markerWidth = 42.dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DSColors.shadowGray.copy(alpha = 0.28f)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(playbackProgress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(DSColors.fairway, DSColors.leafGreen))),
            )
            KeyTimeMarker(
                label = "击球",
                progress = impactProgress,
                color = DSColors.trophyGold,
                markerWidth = markerWidth,
                containerWidth = maxWidth,
            )
            KeyTimeMarker(
                label = "落点",
                progress = landingProgress,
                color = DSColors.fairway,
                markerWidth = markerWidth,
                containerWidth = maxWidth,
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TimeLabel(title = "当前", value = formatPreviewTime(currentTimeMs), alignment = TextAlign.Start)
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DSColors.fairway)
                    .clickable { onTogglePlay() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                Text(if (playing) "播放中" else "点击播放", color = Color.White, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            TimeLabel(title = "总时长", value = formatPreviewTime(totalTimeMs), alignment = TextAlign.End)
        }
    }
}

@Composable
private fun KeyTimeMarker(
    label: String,
    progress: Float,
    color: Color,
    markerWidth: androidx.compose.ui.unit.Dp,
    containerWidth: androidx.compose.ui.unit.Dp,
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    val left = (containerWidth - markerWidth) * safeProgress
    Column(
        modifier = Modifier
            .offset(x = left.coerceAtLeast(0.dp))
            .width(markerWidth)
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(DSColors.cardWhite.copy(alpha = 0.96f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
        Box(modifier = Modifier.size(width = 2.dp, height = 12.dp).background(color))
    }
}

@Composable
private fun TimeLabel(title: String, value: String, alignment: TextAlign) {
    Column(horizontalAlignment = if (alignment == TextAlign.End) Alignment.End else Alignment.Start) {
        Text(title, color = DSColors.shadowGray, fontSize = 9.sp)
        Text(value, color = DSColors.inkGreen.copy(alpha = 0.78f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PreviewFunctionButton(
    title: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    devBadge: Boolean = false,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (primary) DSColors.mistGreen.copy(alpha = 0.85f) else Color(0xFFF3F7F0))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 11.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (primary) {
                            Brush.linearGradient(listOf(tint, DSColors.deepGreen))
                        } else {
                            Brush.linearGradient(listOf(tint.copy(alpha = 0.14f), tint.copy(alpha = 0.14f)))
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (!enabled) DSColors.shadowGray.copy(alpha = 0.45f) else if (primary) Color.White else tint,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (devBadge) {
                Text(
                    "稍后",
                    color = DSColors.trophyGold,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(DSColors.trophyGold.copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            title,
            color = if (enabled) DSColors.inkGreen else DSColors.shadowGray.copy(alpha = 0.45f),
            fontSize = 12.sp,
            fontWeight = if (primary) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatPreviewTime(ms: Long): String {
    return "%.1fs".format(ms.coerceAtLeast(0L) / 1000.0)
}

// ─── 视频导出（逐帧渲染轨迹叠加）─────────────────────────────────────────────────

private suspend fun exportVideoWithTrajectory(
    context: Context,
    videoUri: Uri,
    selection: TrackStyleSelection,
    timing: TrajectoryTiming,
    trimStartMs: Long,
    trimEndMs: Long?,
    exportResolution: ExportResolution,
    isPortrait: Boolean,
    onProgress: (Float) -> Unit,
) = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(context, videoUri)

    val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 1280
    val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 720
    val rotationDegrees = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0
    val exportSize = targetExportVideoDimensions(exportResolution, isPortrait)
    val width = exportSize.width
    val height = exportSize.height
    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 3000L
    val trimRange = exportTrimRange(
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        sourceDurationMs = durationMs,
    )
    val fps = timing.fps.roundToInt().coerceIn(1, 120)

    val bezier = selection.flightCurve.bezierPath
    val legacyProgressAtTime = if (selection.style.activeTrajectoryAlgorithm == TrajectoryAlgorithmKind.GOLF_CAM_LIKE) {
        null
    } else {
        val model = TrajectoryMotionModel(
            bezier = bezier,
            timing = timing,
            params = TrajectoryMotionModel.parametersFromStyle(selection.style),
        )
        model::progressAtTime
    }
    val arcTable = ArcLengthTable(bezier)

    // 输出文件
    val outputFile = File(context.cacheDir, "trajectory_export_${System.currentTimeMillis()}.mp4")

    val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000)
        setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }

    val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    val surface = encoder.createInputSurface()
    encoder.start()

    val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    val audioTrackInfo = findAudioTrackInfo(context, videoUri)
    val audioMuxerTrackIndex = audioTrackInfo?.let { info ->
        runCatching { muxer.addTrack(info.format) }.getOrNull()
    } ?: -1
    var videoTrackIndex = -1
    var muxerStarted = false

    val totalFrames = (trimRange.durationMs * fps / 1000).toInt().coerceAtLeast(1)
    val bufferInfo = MediaCodec.BufferInfo()

    // 逐帧渲染
    for (frameIdx in 0 until totalFrames) {
        val outputFrameTimeMs = frameIdx * 1000L / fps
        val sourceFrameTimeMs = trimRange.startMs + outputFrameTimeMs
        val elapsedSec = (outputFrameTimeMs - timing.impactTimeMs).toDouble() / 1000.0

        // 提取原始视频帧
        val frameBitmap = retriever.getFrameAtTime(
            sourceFrameTimeMs * 1000L,
            MediaMetadataRetriever.OPTION_CLOSEST,
        )

        val c = surface.lockCanvas(null)
        if (frameBitmap != null) {
            drawVideoFrameOnCanvas(
                canvas = c,
                frameBitmap = frameBitmap,
                width = width,
                height = height,
                rotationDegrees = rotationDegrees,
            )
            frameBitmap.recycle()
        } else {
            c.drawColor(android.graphics.Color.BLACK)
        }

        // 绘制轨迹叠加
        drawTrajectoryOnCanvas(c, bezier, arcTable, selection.style, elapsedSec, width.toFloat(), height.toFloat(), timing, legacyProgressAtTime)

        surface.unlockCanvasAndPost(c)

        // 从编码器取输出
        var outputDone = false
        while (!outputDone) {
            val outIdx = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
            when {
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!muxerStarted) {
                        videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                }
                outIdx >= 0 -> {
                    val buf = encoder.getOutputBuffer(outIdx) ?: run {
                        encoder.releaseOutputBuffer(outIdx, false); outputDone = true; continue
                    }
                    bufferInfo.presentationTimeUs = outputFrameTimeMs * 1000L
                    if (muxerStarted && bufferInfo.size > 0 &&
                        bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
                    ) {
                        muxer.writeSampleData(videoTrackIndex, buf, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outIdx, false)
                    outputDone = true
                }
                else -> outputDone = true
            }
        }

        onProgress(frameIdx.toFloat() / totalFrames)
    }

    // 信号 EOS
    encoder.signalEndOfInputStream()
    var eosReached = false
    while (!eosReached) {
        val outIdx = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
        if (outIdx >= 0) {
            val buf = encoder.getOutputBuffer(outIdx)
            if (buf != null && muxerStarted && bufferInfo.size > 0 &&
                bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
            ) {
                muxer.writeSampleData(videoTrackIndex, buf, bufferInfo)
            }
            encoder.releaseOutputBuffer(outIdx, false)
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) eosReached = true
        } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && !muxerStarted) {
            videoTrackIndex = muxer.addTrack(encoder.outputFormat)
            muxer.start(); muxerStarted = true
        }
    }

    encoder.stop(); encoder.release()
    if (muxerStarted && audioTrackInfo != null && audioMuxerTrackIndex >= 0) {
        copyAudioTrackToMuxer(
            context = context,
            videoUri = videoUri,
            audioTrackInfo = audioTrackInfo,
            muxer = muxer,
            muxerTrackIndex = audioMuxerTrackIndex,
            trimStartUs = trimRange.startMs * 1000L,
            trimEndUs = trimRange.endMs * 1000L,
        )
    }
    if (muxerStarted) muxer.stop()
    muxer.release()
    retriever.release()

    // 写入相册
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, "GolfSwing_${System.currentTimeMillis()}.mp4")
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/GolfSwing")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
    }
    val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let { destUri ->
        context.contentResolver.openOutputStream(destUri)?.use { out ->
            outputFile.inputStream().copyTo(out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(destUri, values, null, null)
        }
    }
    outputFile.delete()
    onProgress(1f)
}

private fun drawVideoFrameOnCanvas(
    canvas: AndroidCanvas,
    frameBitmap: Bitmap,
    width: Int,
    height: Int,
    rotationDegrees: Int,
) {
    val src = Rect(0, 0, frameBitmap.width, frameBitmap.height)
    val rotation = effectiveExportFrameRotation(
        metadataRotationDegrees = rotationDegrees,
        frameWidth = frameBitmap.width,
        frameHeight = frameBitmap.height,
        exportWidth = width,
        exportHeight = height,
    )

    canvas.save()
    when (rotation) {
        90 -> {
            canvas.translate(width.toFloat(), 0f)
            canvas.rotate(90f)
            canvas.drawBitmap(frameBitmap, src, Rect(0, 0, height, width), null)
        }
        180 -> {
            canvas.translate(width.toFloat(), height.toFloat())
            canvas.rotate(180f)
            canvas.drawBitmap(frameBitmap, src, Rect(0, 0, width, height), null)
        }
        270 -> {
            canvas.translate(0f, height.toFloat())
            canvas.rotate(270f)
            canvas.drawBitmap(frameBitmap, src, Rect(0, 0, height, width), null)
        }
        else -> {
            canvas.drawBitmap(frameBitmap, src, Rect(0, 0, width, height), null)
        }
    }
    canvas.restore()
}

private fun findAudioTrackInfo(context: Context, videoUri: Uri): AudioTrackInfo? {
    val extractor = MediaExtractor()
    return try {
        extractor.setDataSource(context, videoUri, null)
        for (trackIndex in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(trackIndex)
            if (isAudioMimeType(format.getString(MediaFormat.KEY_MIME))) {
                return AudioTrackInfo(sourceTrackIndex = trackIndex, format = format)
            }
        }
        null
    } catch (_: Exception) {
        null
    } finally {
        extractor.release()
    }
}

private fun copyAudioTrackToMuxer(
    context: Context,
    videoUri: Uri,
    audioTrackInfo: AudioTrackInfo,
    muxer: MediaMuxer,
    muxerTrackIndex: Int,
    trimStartUs: Long,
    trimEndUs: Long,
) {
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(context, videoUri, null)
        extractor.selectTrack(audioTrackInfo.sourceTrackIndex)
        extractor.seekTo(trimStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        val maxInputSize = if (audioTrackInfo.format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            audioTrackInfo.format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            256 * 1024
        }.coerceAtLeast(16 * 1024)
        val buffer = ByteBuffer.allocate(maxInputSize)
        val info = MediaCodec.BufferInfo()

        while (true) {
            val sampleTimeUs = extractor.sampleTime
            if (sampleTimeUs < 0 || sampleTimeUs > trimEndUs) break
            if (sampleTimeUs < trimStartUs) {
                extractor.advance()
                continue
            }
            buffer.clear()
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize <= 0) break
            info.set(0, sampleSize, sampleTimeUs - trimStartUs, extractor.sampleFlags)
            muxer.writeSampleData(muxerTrackIndex, buffer, info)
            extractor.advance()
        }
    } catch (_: Exception) {
        // Audio passthrough is best-effort; video export should still succeed.
    } finally {
        extractor.release()
    }
}

private fun drawTrajectoryOnCanvas(
    canvas: AndroidCanvas,
    bezier: BezierPath,
    arcTable: ArcLengthTable,
    style: TrackStyle,
    elapsedSec: Double,
    width: Float,
    height: Float,
    timing: TrajectoryTiming,
    legacyProgressAtTime: ((Double) -> Double)?,
) {
    val snapshot = TrajectoryOverlayFrameSampler.sample(
        bezier = bezier,
        arcTable = arcTable,
        timing = timing,
        style = style,
        elapsedSec = elapsedSec,
        legacyProgressAtTime = legacyProgressAtTime,
    ) ?: return
    TrajectoryRibbonRenderer.draw(
        canvas = canvas,
        bezier = bezier,
        arcTable = arcTable,
        headProgress = snapshot.headProgress,
        tailProgress = snapshot.tailProgress,
        style = style,
        width = width,
        height = height,
    )

    val headX = snapshot.headPoint.x * width
    val headY = snapshot.headPoint.y * height
    if (TrajectoryOverlayRenderSpec.shouldDrawFlyingHead(snapshot.headProgress)) {
        val headGlowRadius = TrajectoryOverlayRenderSpec.headGlowRadius(
            style = style,
            videoHeight = height,
            headProgress = snapshot.headProgress,
            apexProgress = snapshot.apexProgress,
        )
        val glowAlpha = style.glowOpacity.toFloat().coerceIn(0f, 1f)
        val glowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.style = android.graphics.Paint.Style.FILL
        }
        val glowAlphas = TrajectoryOverlayRenderSpec.headGlowAlphas(glowAlpha)
        glowPaint.color = trackColorWithAlpha(style.primaryColorArgb(), glowAlphas.outer)
        canvas.drawCircle(headX, headY, headGlowRadius * 1.8f, glowPaint)
        glowPaint.color = trackColorWithAlpha(style.primaryColorArgb(), glowAlphas.body)
        canvas.drawCircle(headX, headY, headGlowRadius, glowPaint)
        glowPaint.color = android.graphics.Color.argb((255 * glowAlphas.core).toInt().coerceIn(0, 255), 255, 255, 255)
        canvas.drawCircle(headX, headY, headGlowRadius * 0.4f, glowPaint)
    }

    snapshot.distance?.let { displayDist ->
        drawDistanceLabelOnCanvas(
            canvas = canvas,
            x = headX,
            y = headY,
            isFlyingLeft = snapshot.isFlyingLeft,
            distance = displayDist,
            unit = style.distanceUnit.abbreviation,
            height = height,
        )
    }
}

private fun readVideoPlaybackConfig(context: Context, videoUri: Uri): VideoPlaybackConfig {
    var visualWidth = 1080
    var visualHeight = 1920
    val aspectRatio = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 9
            val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 16
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0
            val width = if (rotation == 90 || rotation == 270) rawHeight else rawWidth
            val height = if (rotation == 90 || rotation == 270) rawWidth else rawHeight
            visualWidth = width.coerceAtLeast(1)
            visualHeight = height.coerceAtLeast(1)
            width.toFloat() / height.coerceAtLeast(1).toFloat()
        } finally {
            retriever.release()
        }
    }.getOrDefault(9f / 16f).coerceAtLeast(0.01f)

    val extractor = MediaExtractor()
    val frameRate = try {
        extractor.setDataSource(context, videoUri, null)
        for (trackIndex in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/") && format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                return VideoPlaybackConfig(
                    frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE).coerceIn(1, 120).toDouble(),
                    aspectRatio = aspectRatio,
                    visualWidth = visualWidth,
                    visualHeight = visualHeight,
                )
            }
        }
        30.0
    } catch (_: Exception) {
        30.0
    } finally {
        extractor.release()
    }
    return VideoPlaybackConfig(frameRate = frameRate, aspectRatio = aspectRatio, visualWidth = visualWidth, visualHeight = visualHeight)
}

private fun trackColorWithAlpha(argb: Long, alpha: Float): Int {
    return android.graphics.Color.argb(
        (alpha.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255),
        ((argb ushr 16) and 0xFF).toInt(),
        ((argb ushr 8) and 0xFF).toInt(),
        (argb and 0xFF).toInt(),
    )
}

private fun drawDistanceLabelOnCanvas(
    canvas: AndroidCanvas,
    x: Float,
    y: Float,
    isFlyingLeft: Boolean,
    distance: Int,
    unit: String,
    height: Float,
) {
    if (distance <= 0) return

    val unitPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = DistanceLabelRenderSpec.unitFontPx(height)
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val valuePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = DistanceLabelRenderSpec.valueFontPx(height)
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    }
    val unitMetrics = unitPaint.fontMetrics
    val valueText = distance.toString()
    val valueMetrics = valuePaint.fontMetrics
    val layout = DistanceLabelRenderSpec.layout(
        headX = x,
        headY = y,
        videoHeight = height,
        isFlyingLeft = isFlyingLeft,
        metrics = DistanceLabelTextMetrics(
            unitWidth = unitPaint.measureText(unit),
            unitHeight = unitMetrics.descent - unitMetrics.ascent,
            valueWidth = valuePaint.measureText(valueText),
            valueHeight = valueMetrics.descent - valueMetrics.ascent,
        ),
    )
    val shadowOffset = DistanceLabelRenderSpec.shadowOffsetPx(height)
    val shadowAlpha = (DistanceLabelRenderSpec.SHADOW_ALPHA * 255f).toInt().coerceIn(0, 255)
    val shadowBlur = DistanceLabelRenderSpec.shadowBlurRadiusPx(height)
    val shadowColor = android.graphics.Color.argb(shadowAlpha, 0, 0, 0)

    unitPaint.color = android.graphics.Color.WHITE
    valuePaint.color = android.graphics.Color.WHITE
    unitPaint.setShadowLayer(shadowBlur, shadowOffset, shadowOffset, shadowColor)
    valuePaint.setShadowLayer(shadowBlur, shadowOffset, shadowOffset, shadowColor)
    canvas.drawText(unit, layout.unitLeft, layout.unitTop - unitMetrics.ascent, unitPaint)
    canvas.drawText(valueText, layout.valueLeft, layout.valueTop - valueMetrics.ascent, valuePaint)
    unitPaint.clearShadowLayer()
    valuePaint.clearShadowLayer()
}
