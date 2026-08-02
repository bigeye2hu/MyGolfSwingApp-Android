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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.core.graphics.withSave
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.model.ArcLengthTable
import com.mygolfswing.app.ui.golfcam.model.BezierPath
import com.mygolfswing.app.ui.golfcam.model.GolfCamFrameTimeline
import com.mygolfswing.app.ui.golfcam.model.GolfCamVideoConfig
import com.mygolfswing.app.ui.golfcam.model.NormalizedPoint
import com.mygolfswing.app.ui.golfcam.model.TrackStyle
import com.mygolfswing.app.ui.golfcam.model.TrackStyleSelection
import com.mygolfswing.app.ui.golfcam.model.TrajectoryAlgorithmKind
import com.mygolfswing.app.ui.golfcam.model.TrajectoryMotionModel
import com.mygolfswing.app.ui.golfcam.model.TrajectoryTiming
import com.mygolfswing.app.ui.golfcam.model.clampedTrackStyleSelection
import com.mygolfswing.app.ui.golfcam.render.DistanceLabelRenderSpec
import com.mygolfswing.app.ui.golfcam.render.DistanceLabelTextMetrics
import com.mygolfswing.app.ui.golfcam.render.TrajectoryAnimationOverlay
import com.mygolfswing.app.ui.golfcam.render.TrajectoryOverlayFrameSampler
import com.mygolfswing.app.ui.golfcam.render.TrajectoryOverlayRenderSpec
import com.mygolfswing.app.ui.golfcam.render.TrajectoryRibbonRenderer
import com.mygolfswing.app.ui.golfcam.render.VideoDisplayGeometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

internal data class ExportVideoDimensions(
    val width: Int,
    val height: Int,
)

internal data class ExportVideoTarget(
    val resolution: ExportResolution,
    val dimensions: ExportVideoDimensions,
    val visualDimensions: ExportVideoDimensions,
) {
    val isPortrait: Boolean get() = visualDimensions.height >= visualDimensions.width
}

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

internal data class ExportTrimRangeUs(
    val startUs: Long,
    val endUs: Long,
)

internal data class PreviewPlaybackClip(
    val startMs: Long,
    val endMs: Long?,
    val durationMs: Long,
)

internal const val PREVIEW_VIDEO_CARD_CORNER_RADIUS_DP = 18
internal const val PREVIEW_VIDEO_CARD_BORDER_ALPHA = 0.35f
internal const val PREVIEW_VIDEO_CARD_BORDER_WIDTH_DP = 1
internal const val PREVIEW_VIDEO_CARD_SHADOW_RADIUS_DP = 16
internal const val PREVIEW_BOTTOM_PANEL_HEIGHT_DP = 190
internal const val EXPORT_SETTINGS_4K_SOURCE_REQUIRED_REASON = "需要4K源视频"

private data class AudioTrackInfo(
    val sourceTrackIndex: Int,
    val format: MediaFormat,
)

internal fun exportVideoDimensions(rawWidth: Int, rawHeight: Int, rotationDegrees: Int): ExportVideoDimensions {
    val safeWidth = rawWidth.coerceAtLeast(1)
    val safeHeight = rawHeight.coerceAtLeast(1)
    val rotation = normalizedVideoRotation(rotationDegrees)
    return if (rotation == 90 || rotation == 270) {
        ExportVideoDimensions(width = safeHeight, height = safeWidth)
    } else {
        ExportVideoDimensions(width = safeWidth, height = safeHeight)
    }
}

internal fun metadataIntOrDefault(raw: String?, defaultValue: Int): Int {
    return raw?.toIntOrNull() ?: defaultValue
}

internal fun metadataLongOrDefault(raw: String?, defaultValue: Long): Long {
    return raw?.toLongOrNull() ?: defaultValue
}

internal fun targetExportVideoDimensions(resolution: ExportResolution, isPortrait: Boolean): ExportVideoDimensions {
    return resolution.dimensions(isPortrait)
}

internal fun sourceSupportsExportResolution(
    resolution: ExportResolution,
    visualWidth: Int,
    visualHeight: Int,
): Boolean {
    if (resolution == ExportResolution.HD_1080P) return true
    val isPortrait = visualHeight >= visualWidth
    val required = targetExportVideoDimensions(resolution, isPortrait)
    return visualWidth >= required.width && visualHeight >= required.height
}

internal fun coerceExportResolutionForSource(
    selected: ExportResolution,
    visualWidth: Int,
    visualHeight: Int,
): ExportResolution {
    return if (sourceSupportsExportResolution(selected, visualWidth, visualHeight)) {
        selected
    } else {
        ExportResolution.HD_1080P
    }
}

internal fun resolveExportVideoTarget(
    selected: ExportResolution,
    rawWidth: Int,
    rawHeight: Int,
    rotationDegrees: Int,
): ExportVideoTarget {
    val visualDimensions = exportVideoDimensions(
        rawWidth = rawWidth,
        rawHeight = rawHeight,
        rotationDegrees = normalizedVideoRotation(rotationDegrees),
    )
    val resolvedResolution = coerceExportResolutionForSource(
        selected = selected,
        visualWidth = visualDimensions.width,
        visualHeight = visualDimensions.height,
    )
    return ExportVideoTarget(
        resolution = resolvedResolution,
        dimensions = targetExportVideoDimensions(
            resolution = resolvedResolution,
            isPortrait = visualDimensions.height >= visualDimensions.width,
        ),
        visualDimensions = visualDimensions,
    )
}

internal fun targetExportVideoBitrate(dimensions: ExportVideoDimensions): Int {
    val longEdge = max(dimensions.width, dimensions.height)
    return when {
        longEdge >= 3800 -> 30_000_000
        longEdge >= 1900 -> 15_000_000
        else -> 8_000_000
    }
}

internal fun shouldOpenExportPreview(exportedFile: File?): Boolean {
    return isValidExportPreviewFile(exportedFile)
}

internal fun exportSaveFailureMessage(error: Throwable): String {
    if (error is CancellationException) throw error
    return "保存失败：${exportSaveErrorDetail(error)}"
}

internal fun exportFailureMessage(error: Throwable): String {
    if (error is CancellationException) throw error
    return "导出失败：${exportRenderErrorDetail(error)}"
}

private fun exportRenderErrorDetail(error: Throwable): String {
    val raw = error.message?.trim()?.takeIf { it.isNotBlank() }
    val stable = when {
        raw == null -> "视频导出失败，请稍后重试"
        raw.contains("编码", ignoreCase = true) ||
            raw.contains("encoder", ignoreCase = true) ||
            raw.contains("codec", ignoreCase = true) -> "当前设备暂时无法完成视频编码，请降低清晰度后重试"
        raw.contains("预览文件", ignoreCase = true) ||
            raw.contains("preview", ignoreCase = true) ||
            raw.contains("file", ignoreCase = true) -> "导出文件暂时不可用，请重新导出"
        else -> "视频导出失败，请稍后重试"
    }
    return exportUserError(stable, raw)
}

private fun exportSaveErrorDetail(error: Throwable): String {
    val raw = error.message?.trim()?.takeIf { it.isNotBlank() }
    val stable = when {
        raw == null -> "保存到相册失败，请稍后重试"
        raw.contains("预览文件", ignoreCase = true) ||
            raw.contains("preview", ignoreCase = true) ||
            raw.contains("file", ignoreCase = true) -> "导出文件暂时不可用，请重新导出"
        raw.contains("相册", ignoreCase = true) ||
            raw.contains("MediaStore", ignoreCase = true) ||
            raw.contains("权限", ignoreCase = true) ||
            raw.contains("denied", ignoreCase = true) ||
            raw.contains("permission", ignoreCase = true) -> "系统相册暂时不可写，请检查存储空间或权限后重试"
        else -> "保存到相册失败，请稍后重试"
    }
    return exportUserError(stable, raw)
}

internal fun exportUserError(base: String, debugDetail: String?): String {
    return exportUserError(base, debugDetail, ServiceLocator.isDebuggable)
}

internal fun exportUserError(base: String, debugDetail: String?, isDebuggable: Boolean): String {
    val detail = debugDetail.takeIf { isDebuggable && !it.isNullOrBlank() }
    return if (detail == null) base else "$base（$detail）"
}

internal fun shouldAllowExportPreviewDismiss(saving: Boolean): Boolean = !saving

internal fun shouldAllowExportSave(
    saving: Boolean,
    saveMessage: String?,
    previewFile: File?,
): Boolean {
    return !saving && saveMessage == null && isValidExportPreviewFile(previewFile)
}

internal fun exportPreviewSaveButtonAlpha(
    saving: Boolean,
    saveMessage: String?,
    previewFile: File?,
): Float {
    return if (shouldAllowExportSave(saving, saveMessage, previewFile)) 1f else 0.42f
}

internal data class ExportSaveSuccessDisposition(
    val markExportComplete: Boolean,
    val clearPreviewFile: Boolean,
    val clearSaveMessage: Boolean,
    val closeOverview: Boolean,
)

internal fun exportSaveSuccessDisposition(): ExportSaveSuccessDisposition {
    return ExportSaveSuccessDisposition(
        markExportComplete = true,
        clearPreviewFile = true,
        clearSaveMessage = true,
        closeOverview = true,
    )
}

internal fun deleteExportPreviewTempFile(file: File?): Boolean {
    return file?.takeIf { it.exists() }?.delete() ?: false
}

internal fun isValidExportPreviewFile(file: File?): Boolean {
    return file?.isFile == true && file.length() > 0L
}

internal fun mediaStorePendingPublishSucceeded(sdkInt: Int, updatedRows: Int): Boolean {
    return sdkInt < Build.VERSION_CODES.Q || updatedRows > 0
}

internal fun shouldCleanupFailedGalleryInsert(insertedUriPresent: Boolean, saveSucceeded: Boolean): Boolean {
    return insertedUriPresent && !saveSucceeded
}

internal fun effectiveExportFrameRotation(
    metadataRotationDegrees: Int,
    frameWidth: Int,
    frameHeight: Int,
    exportWidth: Int,
    exportHeight: Int,
): Int {
    val rotation = normalizedVideoRotation(metadataRotationDegrees)
    val frameAlreadyVisual = (rotation == 90 || rotation == 270) &&
        frameWidth > 0 &&
        frameHeight > 0 &&
        exportWidth > 0 &&
        exportHeight > 0 &&
        (frameHeight > frameWidth) == (exportHeight > exportWidth)
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

internal fun previewPlaybackClip(
    trimStartMs: Long,
    trimEndMs: Long?,
    sourceDurationMs: Long?,
    playerDurationMs: Long? = null,
): PreviewPlaybackClip {
    val trustedSourceDuration = sourceDurationMs?.takeIf { it >= 500L }
    if (trustedSourceDuration != null) {
        val range = exportTrimRange(trimStartMs, trimEndMs, trustedSourceDuration)
        return PreviewPlaybackClip(
            startMs = range.startMs,
            endMs = range.endMs,
            durationMs = range.durationMs,
        )
    }
    val start = trimStartMs.coerceAtLeast(0L)
    val end = trimEndMs?.let { GolfCamFrameTimeline.endExclusiveMsAfterStart(start, it) }
    val duration = end?.let { GolfCamFrameTimeline.durationMsForRange(start, it) }
        ?: playerDurationMs?.takeIf { it > 0L }
        ?: 1L
    return PreviewPlaybackClip(
        startMs = start,
        endMs = end,
        durationMs = duration.coerceAtLeast(1L),
    )
}

internal fun previewImpactTimeMs(absoluteImpactTimeMs: Long, clipStartMs: Long): Long {
    if (absoluteImpactTimeMs <= clipStartMs) return 0L
    val delta = absoluteImpactTimeMs - clipStartMs
    return if (delta >= 0L) delta else Long.MAX_VALUE
}

internal fun previewProgressFraction(positionMs: Long, durationMs: Long): Float {
    return (positionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
}

internal fun previewProgressDisplayFraction(progress: Float): Float {
    return if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
}

internal fun exportProgressFraction(completedFrames: Int, totalFrames: Int): Float {
    if (totalFrames <= 0) return 0f
    return (completedFrames.coerceAtLeast(0).toFloat() / totalFrames.toFloat()).coerceIn(0f, 1f)
}

internal fun exportTrajectoryElapsedSeconds(outputFrameTimeMs: Long, previewImpactTimeMs: Long): Double {
    return saturatingDelta(outputFrameTimeMs, previewImpactTimeMs).toDouble() / 1000.0
}

internal fun exportFramePresentationTimeUs(frameIndex: Int, fps: Double): Long {
    val safeFps = GolfCamFrameTimeline.safeFrameRate(fps)
    return (frameIndex.coerceAtLeast(0) * 1_000_000.0 / safeFps).toLong()
}

internal fun exportFramePresentationTimeUs(frameIndex: Int, fps: Int): Long {
    return exportFramePresentationTimeUs(frameIndex = frameIndex, fps = fps.toDouble())
}

internal fun exportTrajectoryElapsedSecondsFromUs(outputFrameTimeUs: Long, previewImpactTimeMs: Long): Double {
    val impactTimeUs = exportMillisecondsToMicroseconds(previewImpactTimeMs)
    return saturatingDelta(outputFrameTimeUs, impactTimeUs).toDouble() / 1_000_000.0
}

internal fun exportSourceFrameTimeUs(
    trimStartMs: Long,
    trimEndMs: Long,
    frameIndex: Int,
    fps: Double,
): Long {
    val safeStartMs = trimStartMs.coerceAtLeast(0L)
    val safeEndMs = GolfCamFrameTimeline.endExclusiveMsAfterStart(safeStartMs, trimEndMs)
    val startUs = exportMillisecondsToMicroseconds(safeStartMs)
    val endUs = exportMillisecondsToMicroseconds(safeEndMs)
    val lastSourceUs = if (endUs > startUs) endUs - 1L else startUs
    val targetUs = saturatingAddUs(startUs, exportFramePresentationTimeUs(frameIndex, fps))
    return targetUs.coerceIn(startUs, lastSourceUs)
}

internal fun exportMillisecondsToMicroseconds(ms: Long): Long {
    if (ms <= 0L) return 0L
    return if (ms > Long.MAX_VALUE / 1000L) Long.MAX_VALUE else ms * 1000L
}

internal fun exportTrimRangeUs(range: ExportTrimRange): ExportTrimRangeUs {
    return ExportTrimRangeUs(
        startUs = exportMillisecondsToMicroseconds(range.startMs),
        endUs = exportMillisecondsToMicroseconds(range.endMs),
    )
}

internal fun trimmedAudioPresentationTimeUs(
    sampleTimeUs: Long,
    trimStartUs: Long,
    trimEndUs: Long,
): Long? {
    return when {
        sampleTimeUs < trimStartUs -> null
        sampleTimeUs >= trimEndUs -> null
        else -> saturatingNonNegativeDeltaUs(sampleTimeUs, trimStartUs)
    }
}

private fun saturatingAddUs(baseUs: Long, offsetUs: Long): Long {
    if (offsetUs <= 0L) return baseUs
    return if (baseUs > Long.MAX_VALUE - offsetUs) Long.MAX_VALUE else baseUs + offsetUs
}

private fun saturatingNonNegativeDeltaUs(valueUs: Long, startUs: Long): Long {
    if (valueUs <= startUs) return 0L
    val delta = valueUs - startUs
    return if (delta >= 0L) delta else Long.MAX_VALUE
}

private fun saturatingDelta(value: Long, base: Long): Long {
    val delta = value - base
    return if (((value xor base) and (value xor delta)) < 0L) {
        if (value < base) Long.MIN_VALUE else Long.MAX_VALUE
    } else {
        delta
    }
}

internal fun audioCopyBufferSize(formatMaxInputSize: Int?): Int {
    val fallbackSize = 256 * 1024
    val minimumSize = 16 * 1024
    return (formatMaxInputSize ?: fallbackSize).coerceAtLeast(minimumSize)
}

internal fun audioMuxerPresentationTimeUs(
    sampleTimeUs: Long,
    sampleSize: Int,
    trimStartUs: Long,
    trimEndUs: Long,
): Long? {
    if (sampleSize <= 0) return null
    return trimmedAudioPresentationTimeUs(
        sampleTimeUs = sampleTimeUs,
        trimStartUs = trimStartUs,
        trimEndUs = trimEndUs,
    )
}

internal enum class AudioSampleCopyAction {
    WRITE,
    SKIP,
    END,
}

internal fun audioSampleCopyAction(sampleSize: Int): AudioSampleCopyAction {
    return when {
        sampleSize < 0 -> AudioSampleCopyAction.END
        sampleSize == 0 -> AudioSampleCopyAction.SKIP
        else -> AudioSampleCopyAction.WRITE
    }
}

internal fun audioMuxerSampleFlags(extractorSampleFlags: Int): Int {
    var flags = 0
    if (extractorSampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
        flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
    }
    if (extractorSampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME != 0) {
        flags = flags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
    }
    return flags
}

internal fun exportTotalFrameCount(durationMs: Long, fps: Double): Int {
    return GolfCamFrameTimeline.totalFrames(durationMs.coerceAtLeast(1L), fps)
}

internal fun exportTotalFrameCount(durationMs: Long, fps: Int): Int {
    return exportTotalFrameCount(durationMs = durationMs, fps = fps.toDouble())
}

internal enum class PreviewPanelAction {
    FLIGHT_PATH,
    CLUB_PATH,
    TRIM,
    EXPORT,
}

internal data class PreviewPanelActionState(
    val action: PreviewPanelAction,
    val enabled: Boolean,
    val deferredBadge: Boolean,
)

internal fun previewPanelActionStates(
    exporting: Boolean,
    exportDone: Boolean,
    videoConfigTrusted: Boolean = true,
): List<PreviewPanelActionState> {
    return listOf(
        PreviewPanelActionState(PreviewPanelAction.FLIGHT_PATH, enabled = true, deferredBadge = false),
        PreviewPanelActionState(PreviewPanelAction.CLUB_PATH, enabled = false, deferredBadge = true),
        PreviewPanelActionState(PreviewPanelAction.TRIM, enabled = false, deferredBadge = true),
        PreviewPanelActionState(PreviewPanelAction.EXPORT, enabled = videoConfigTrusted && !exporting && !exportDone, deferredBadge = false),
    )
}

internal fun normalizedPreviewExportSelection(selection: TrackStyleSelection): TrackStyleSelection {
    return clampedTrackStyleSelection(selection)
}

internal fun trustedPreviewVideoConfigOrUnknown(config: GolfCamVideoConfig?): GolfCamVideoConfig {
    return config?.takeUnless(::shouldDetectGolfCamVideoConfig) ?: GolfCamVideoConfig()
}

internal data class ExportSettingsUiState(
    val selectedResolution: ExportResolution,
    val isSource4K: Boolean,
    val isPortrait: Boolean,
    val sourceInfoTrusted: Boolean,
)

internal fun exportSettingsUiState(
    selectedResolution: ExportResolution,
    videoConfig: GolfCamVideoConfig,
): ExportSettingsUiState {
    val trusted = !shouldDetectGolfCamVideoConfig(videoConfig)
    val isPortrait = trusted && videoConfig.visualHeight >= videoConfig.visualWidth
    val source4K = trusted && sourceSupportsExportResolution(
        resolution = ExportResolution.UHD_4K,
        visualWidth = videoConfig.visualWidth,
        visualHeight = videoConfig.visualHeight,
    )
    val resolvedResolution = if (trusted) {
        coerceExportResolutionForSource(
            selected = selectedResolution,
            visualWidth = videoConfig.visualWidth,
            visualHeight = videoConfig.visualHeight,
        )
    } else {
        ExportResolution.HD_1080P
    }
    return ExportSettingsUiState(
        selectedResolution = resolvedResolution,
        isSource4K = source4K,
        isPortrait = isPortrait,
        sourceInfoTrusted = trusted,
    )
}

/**
 * 预览 + 导出屏（对齐 iOS OverlayPreviewView）。
 * 播放带轨迹叠加的视频，可导出为 MP4 保存到相册。
 */
@Composable
fun GolfCamOverlayPreviewScreen(
    videoUri: Uri,
    selection: TrackStyleSelection,
    initialVideoConfig: GolfCamVideoConfig? = null,
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
    var videoConfig by remember { mutableStateOf(trustedPreviewVideoConfigOrUnknown(initialVideoConfig)) }
    var showPlaybackButton by remember { mutableStateOf(true) }
    var showExportSettings by remember { mutableStateOf(false) }
    var selectedExportResolution by remember { mutableStateOf(ExportResolution.HD_1080P) }
    var exportedVideoFile by remember { mutableStateOf<File?>(null) }
    var savingExport by remember { mutableStateOf(false) }
    var exportSaveMessage by remember { mutableStateOf<String?>(null) }
    var exportErrorMessage by remember { mutableStateOf<String?>(null) }
    var deferredPreviewAction by remember { mutableStateOf<PreviewPanelAction?>(null) }
    val previewSelection = remember(selection) { normalizedPreviewExportSelection(selection) }
    val videoConfigTrusted = !shouldDetectGolfCamVideoConfig(videoConfig)
    val exportSettingsState = exportSettingsUiState(selectedExportResolution, videoConfig)

    DisposableEffect(exportedVideoFile) {
        val tempFile = exportedVideoFile
        onDispose { deleteExportPreviewTempFile(tempFile) }
    }

    LaunchedEffect(videoUri, initialVideoConfig) {
        val trustedInitialConfig = initialVideoConfig?.takeUnless(::shouldDetectGolfCamVideoConfig)
        videoConfig = trustedInitialConfig ?: withContext(Dispatchers.IO) { detectGolfCamVideoConfig(context, videoUri) }
    }

    val playbackClip = previewPlaybackClip(
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        sourceDurationMs = videoConfig.durationMs,
    )
    val previewImpactMs = previewImpactTimeMs(impactTimeMs, playbackClip.startMs)
    val timing = previewTrajectoryTiming(
        impactTimeMs = previewImpactMs,
        flightTimeSec = previewSelection.flightCurve.landingPoint.flightTimeSec,
        fps = videoConfig.frameRate,
    )

    val player = remember(videoUri, playbackClip.startMs, playbackClip.endMs) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = if (playbackClip.startMs > 0L || playbackClip.endMs != null) {
                MediaItem.Builder()
                    .setUri(videoUri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(playbackClip.startMs)
                            .apply { playbackClip.endMs?.let { setEndPositionMs(it) } }
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
    DisposableEffect(player) { onDispose { player.release() } }

    LaunchedEffect(player, playing) {
        if (playing) player.play() else player.pause()
    }
    LaunchedEffect(player, previewImpactMs) {
        while (true) {
            previewPositionMs = player.currentPosition.coerceAtLeast(0L)
            elapsedMs = unifiedPreviewElapsedMs(previewPositionMs, previewImpactMs)
            delay(33L)
        }
    }
    LaunchedEffect(playing, showPlaybackButton) {
        if (playing && showPlaybackButton) {
            delay(3000L)
            showPlaybackButton = false
        }
    }

    val clippedDurationMs = remember(trimStartMs, trimEndMs, videoConfig.durationMs, player.duration) {
        previewPlaybackClip(
            trimStartMs = trimStartMs,
            trimEndMs = trimEndMs,
            sourceDurationMs = videoConfig.durationMs,
            playerDurationMs = player.duration,
        ).durationMs
    }
    val playbackProgress = previewProgressFraction(previewPositionMs, clippedDurationMs)
    val impactProgress = previewProgressFraction(previewImpactMs, clippedDurationMs)
    val landingProgress = previewProgressFraction(timing.landingTimeMs, clippedDurationMs)
    LaunchedEffect(exportSettingsState.selectedResolution) {
        if (selectedExportResolution != exportSettingsState.selectedResolution) {
            selectedExportResolution = exportSettingsState.selectedResolution
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        Column(modifier = Modifier.fillMaxSize()) {
            PreviewTopBar(
                selectedResolution = exportSettingsState.selectedResolution,
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
                    .shadow(
                        PREVIEW_VIDEO_CARD_SHADOW_RADIUS_DP.dp,
                        RoundedCornerShape(PREVIEW_VIDEO_CARD_CORNER_RADIUS_DP.dp),
                        ambientColor = Color.Black.copy(alpha = 0.10f),
                        spotColor = Color.Black.copy(alpha = 0.12f),
                    )
                    .clip(RoundedCornerShape(PREVIEW_VIDEO_CARD_CORNER_RADIUS_DP.dp))
                    .background(Color.Black)
                    .border(
                        width = PREVIEW_VIDEO_CARD_BORDER_WIDTH_DP.dp,
                        color = Color.White.copy(alpha = PREVIEW_VIDEO_CARD_BORDER_ALPHA),
                        shape = RoundedCornerShape(PREVIEW_VIDEO_CARD_CORNER_RADIUS_DP.dp),
                    ),
            ) {
                AndroidView(
                    factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = false } },
                    update = { view -> view.player = player },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                showPlaybackButton = !showPlaybackButton
                            }
                        },
                )

                TrajectoryAnimationOverlay(
                    bezier = previewSelection.flightCurve.bezierPath,
                    timing = timing,
                    style = previewSelection.style,
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
                videoConfigTrusted = videoConfigTrusted,
                exportErrorMessage = exportErrorMessage,
                onTogglePlay = {
                    playing = !playing
                    showPlaybackButton = true
                },
                onEditFlightPath = onReEdit,
                onDeferredAction = { deferredPreviewAction = it },
                onExport = {
                    if (videoConfigTrusted && !exporting && !exportDone) {
                        scope.launch {
                            exporting = true
                            exportProgress = 0f
                            exportErrorMessage = null
                            player.pause()
                            playing = false
                            showPlaybackButton = true
                            try {
                                val exportedFile = exportVideoWithTrajectory(
                                    context = context,
                                    videoUri = videoUri,
                                    selection = previewSelection,
                                    timing = timing,
                                    trimStartMs = trimStartMs,
                                    trimEndMs = trimEndMs,
                                    exportResolution = exportSettingsState.selectedResolution,
                                    onProgress = { exportProgress = previewProgressDisplayFraction(it) },
                                )
                                if (!shouldOpenExportPreview(exportedFile)) {
                                    deleteExportPreviewTempFile(exportedFile)
                                    error("导出预览文件不可用")
                                }
                                exportedVideoFile = exportedFile
                                exportSaveMessage = null
                                exportErrorMessage = null
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Throwable) {
                                exportProgress = 0f
                                exportErrorMessage = exportFailureMessage(error)
                            } finally {
                                exporting = false
                            }
                        }
                    }
                },
            )
        }

        deferredPreviewAction?.let { action ->
            PreviewDeferredActionDialog(
                action = action,
                onDismiss = { deferredPreviewAction = null },
                onReEdit = {
                    deferredPreviewAction = null
                    onReEdit()
                },
            )
        }

        if (showExportSettings) {
            ExportSettingsOverlay(
                selectedResolution = exportSettingsState.selectedResolution,
                isSource4K = exportSettingsState.isSource4K,
                isPortrait = exportSettingsState.isPortrait,
                sourceInfoTrusted = exportSettingsState.sourceInfoTrusted,
                onSelect = { requestedResolution ->
                    selectedExportResolution = exportSettingsUiState(requestedResolution, videoConfig).selectedResolution
                },
                onDismiss = { showExportSettings = false },
            )
        }

        exportedVideoFile?.let { file ->
            ExportPreviewOverlay(
                videoFile = file,
                saving = savingExport,
                saveMessage = exportSaveMessage,
                onDismiss = {
                    deleteExportPreviewTempFile(file)
                    exportedVideoFile = null
                    exportSaveMessage = null
                },
                onClearSaveMessage = { exportSaveMessage = null },
                onSave = {
                    if (shouldAllowExportSave(savingExport, exportSaveMessage, file)) {
                        scope.launch {
                            savingExport = true
                            exportSaveMessage = null
                            try {
                                saveExportedVideoToGallery(context, file)
                                val disposition = exportSaveSuccessDisposition()
                                if (disposition.markExportComplete) exportDone = true
                                if (disposition.clearPreviewFile) {
                                    deleteExportPreviewTempFile(file)
                                    exportedVideoFile = null
                                }
                                if (disposition.clearSaveMessage) exportSaveMessage = null
                                if (disposition.closeOverview) onDismiss()
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Throwable) {
                                exportSaveMessage = exportSaveFailureMessage(error)
                            } finally {
                                savingExport = false
                            }
                        }
                    }
                },
            )
        }

        if (exporting) {
            ExportingProgressOverlay(progress = exportProgress)
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
    sourceInfoTrusted: Boolean,
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
                    reason = when {
                        isSource4K -> null
                        !sourceInfoTrusted -> "正在识别源视频信息"
                        else -> EXPORT_SETTINGS_4K_SOURCE_REQUIRED_REASON
                    },
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
private fun ExportPreviewOverlay(
    videoFile: File,
    saving: Boolean,
    saveMessage: String?,
    onDismiss: () -> Unit,
    onClearSaveMessage: () -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember(videoFile) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(videoFile)))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(videoFile) {
        onDispose { player.release() }
    }
    BackHandler(enabled = true) {
        if (shouldAllowExportPreviewDismiss(saving)) onDismiss()
    }
    val saveButtonEnabled = shouldAllowExportSave(saving, saveMessage, videoFile)
    val saveButtonAlpha = exportPreviewSaveButtonAlpha(saving, saveMessage, videoFile)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DSColors.inkGreen),
    ) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = false } },
            update = { view -> view.player = player },
            modifier = Modifier.fillMaxSize(),
        )

        IconButton(
            onClick = onDismiss,
            enabled = shouldAllowExportPreviewDismiss(saving),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(DSColors.inkGreen.copy(alpha = 0.72f)),
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 56.dp, vertical = 40.dp)
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            DSColors.fairway.copy(alpha = saveButtonAlpha),
                            DSColors.deepGreen.copy(alpha = saveButtonAlpha),
                        ),
                    ),
                )
                .clickable(enabled = saveButtonEnabled) { onSave() },
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White.copy(alpha = saveButtonAlpha), modifier = Modifier.size(18.dp))
                Text("保存到相册", color = Color.White.copy(alpha = saveButtonAlpha), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (saving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.60f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(DSColors.inkGreen.copy(alpha = 0.95f))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text("正在保存到相册...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        saveMessage?.let { message ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.46f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(DSColors.cardWhite)
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        "保存结果",
                        color = DSColors.inkGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        message,
                        color = DSColors.shadowGray,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DSColors.inkGreen)
                            .clickable { onClearSaveMessage() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("确定", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportingProgressOverlay(
    progress: Float,
) {
    val safeProgress = previewProgressDisplayFraction(progress)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.46f))
            .clickable { },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1F2F27).copy(alpha = 0.96f))
                .padding(horizontal = 30.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { safeProgress },
                    color = DSColors.fairway,
                    trackColor = Color.White.copy(alpha = 0.18f),
                    strokeWidth = 5.dp,
                    modifier = Modifier.size(58.dp),
                )
                Text(
                    text = "${(safeProgress * 100f).roundToInt()}%",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("正在导出视频", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "请保持本页打开，完成后会进入导出预览",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
            LinearProgressIndicator(
                progress = { safeProgress },
                modifier = Modifier
                    .width(210.dp)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = DSColors.fairway,
                trackColor = Color.White.copy(alpha = 0.16f),
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
    videoConfigTrusted: Boolean,
    exportErrorMessage: String?,
    onTogglePlay: () -> Unit,
    onEditFlightPath: () -> Unit,
    onDeferredAction: (PreviewPanelAction) -> Unit,
    onExport: () -> Unit,
) {
    val actionStates = previewPanelActionStates(
        exporting = exporting,
        exportDone = exportDone,
        videoConfigTrusted = videoConfigTrusted,
    )
        .associateBy { it.action }
    fun stateFor(action: PreviewPanelAction): PreviewPanelActionState {
        return actionStates.getValue(action)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(PREVIEW_BOTTOM_PANEL_HEIGHT_DP.dp)
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
                    progress = { previewProgressDisplayFraction(exportProgress) },
                    modifier = Modifier.fillMaxWidth(),
                    color = DSColors.fairway,
                    trackColor = DSColors.mistGreen,
                )
            }
        } else if (!videoConfigTrusted) {
            Text("正在识别视频信息，完成后可导出", color = DSColors.shadowGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        } else if (exportDone) {
            Text("已保存到相册", color = DSColors.fairway, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        } else if (exportErrorMessage != null) {
            Text(exportErrorMessage, color = Color(0xFFB95D4D), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PreviewFunctionButton(
                title = "飞行轨迹",
                icon = Icons.AutoMirrored.Filled.ShowChart,
                tint = DSColors.fairway,
                enabled = stateFor(PreviewPanelAction.FLIGHT_PATH).enabled,
                onClick = onEditFlightPath,
                modifier = Modifier.weight(1f),
            )
            PreviewFunctionButton(
                title = "杆头轨迹",
                icon = Icons.Filled.Edit,
                tint = Color(0xFF3E8FA5),
                devBadge = stateFor(PreviewPanelAction.CLUB_PATH).deferredBadge,
                enabled = stateFor(PreviewPanelAction.CLUB_PATH).enabled,
                onClick = { onDeferredAction(PreviewPanelAction.CLUB_PATH) },
                modifier = Modifier.weight(1f),
            )
            PreviewFunctionButton(
                title = "裁剪",
                icon = Icons.Filled.ContentCut,
                tint = DSColors.trophyGold,
                devBadge = stateFor(PreviewPanelAction.TRIM).deferredBadge,
                enabled = stateFor(PreviewPanelAction.TRIM).enabled,
                onClick = { onDeferredAction(PreviewPanelAction.TRIM) },
                modifier = Modifier.weight(1f),
            )
            PreviewFunctionButton(
                title = "导出",
                icon = Icons.Filled.Download,
                tint = DSColors.fairway,
                primary = true,
                enabled = stateFor(PreviewPanelAction.EXPORT).enabled,
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
                    .fillMaxWidth(previewProgressDisplayFraction(playbackProgress))
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
    val safeProgress = previewProgressDisplayFraction(progress)
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
    val buttonShape = RoundedCornerShape(16.dp)
    val clickable = enabled || devBadge
    val backgroundColor = when {
        !enabled && !devBadge -> Color(0xFFF7F8F4)
        primary -> DSColors.mistGreen.copy(alpha = 0.85f)
        else -> Color(0xFFF3F7F0)
    }
    val borderColor = when {
        !enabled && !devBadge -> DSColors.shadowGray.copy(alpha = 0.15f)
        primary -> tint.copy(alpha = 0.58f)
        else -> tint.copy(alpha = 0.22f)
    }

    Column(
        modifier = modifier
            .clip(buttonShape)
            .background(backgroundColor)
            .border(
                width = if (primary) 1.4.dp else 1.dp,
                color = borderColor,
                shape = buttonShape,
            )
            .clickable(enabled = clickable) { onClick() }
            .padding(vertical = 11.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(
                        elevation = if (primary && enabled) 8.dp else 0.dp,
                        shape = CircleShape,
                        ambientColor = tint.copy(alpha = 0.24f),
                        spotColor = tint.copy(alpha = 0.24f),
                    )
                    .clip(CircleShape)
                    .background(
                        if (!enabled && !devBadge) {
                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        } else if (primary) {
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
                    tint = if (!enabled && !devBadge) DSColors.shadowGray.copy(alpha = 0.45f) else if (primary) Color.White else tint,
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
                        .offset(x = 9.dp, y = (-5).dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(DSColors.trophyGold.copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            title,
            color = if (enabled || devBadge) DSColors.inkGreen else DSColors.shadowGray.copy(alpha = 0.45f),
            fontSize = 12.sp,
            fontWeight = if (primary) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PreviewDeferredActionDialog(
    action: PreviewPanelAction,
    onDismiss: () -> Unit,
    onReEdit: () -> Unit,
) {
    val title = when (action) {
        PreviewPanelAction.CLUB_PATH -> "杆头轨迹"
        PreviewPanelAction.TRIM -> "裁剪"
        else -> "功能说明"
    }
    val body = when (action) {
        PreviewPanelAction.CLUB_PATH -> "当前预览以飞行轨迹为主。需要调整击球帧、落点、曲线或样式时，可以返回编辑流程继续修改后再导出。"
        PreviewPanelAction.TRIM -> "预览页暂不直接修改裁剪区间。你可以返回编辑流程，在裁剪步骤重新选择有效片段，再回到预览导出。"
        else -> "可以返回编辑流程继续调整后再导出。"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onReEdit) {
                Text("返回编辑", color = DSColors.fairway)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", color = DSColors.shadowGray)
            }
        },
    )
}

internal fun formatPreviewTime(ms: Long): String {
    return String.format(Locale.US, "%.1fs", ms.coerceAtLeast(0L) / 1000.0)
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
    onProgress: (Float) -> Unit,
): File = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    var retrieverReleased = false
    var encoder: MediaCodec? = null
    var encoderStarted = false
    var surface: android.view.Surface? = null
    var muxer: MediaMuxer? = null
    var muxerStarted = false
    var outputFile: File? = null
    var exportSucceeded = false

    try {
        val exportSelection = normalizedPreviewExportSelection(selection)
        retriever.setDataSource(context, videoUri)

        val rawWidth = metadataIntOrDefault(
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH),
            defaultValue = 1280,
        )
        val rawHeight = metadataIntOrDefault(
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT),
            defaultValue = 720,
        )
        val rotationDegrees = metadataIntOrDefault(
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION),
            defaultValue = 0,
        )
        val exportTarget = resolveExportVideoTarget(
            selected = exportResolution,
            rawWidth = rawWidth,
            rawHeight = rawHeight,
            rotationDegrees = rotationDegrees,
        )
        val exportSize = exportTarget.dimensions
        val width = exportSize.width
        val height = exportSize.height
        val durationMs = metadataLongOrDefault(
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION),
            defaultValue = 3000L,
        )
        val trimRange = exportTrimRange(
            trimStartMs = trimStartMs,
            trimEndMs = trimEndMs,
            sourceDurationMs = durationMs,
        )
        val exportFrameRate = GolfCamFrameTimeline.safeFrameRate(timing.fps)
        val encoderFps = exportFrameRate.roundToInt().coerceIn(1, 120)

        val bezier = exportSelection.flightCurve.bezierPath
        val legacyProgressAtTime = if (exportSelection.style.activeTrajectoryAlgorithm == TrajectoryAlgorithmKind.GOLF_CAM_LIKE) {
            null
        } else {
            val model = TrajectoryMotionModel(
                bezier = bezier,
                timing = timing,
                params = TrajectoryMotionModel.parametersFromStyle(exportSelection.style),
            )
            model::progressAtTime
        }
        val arcTable = ArcLengthTable(bezier)

        // 输出文件
        outputFile = File(context.cacheDir, "trajectory_export_${System.currentTimeMillis()}.mp4")

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, targetExportVideoBitrate(exportSize))
            setInteger(MediaFormat.KEY_FRAME_RATE, encoderFps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        surface = encoder.createInputSurface()
        encoder.start()
        encoderStarted = true

        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val audioTrackInfo = findAudioTrackInfo(context, videoUri)
        val audioMuxerTrackIndex = audioTrackInfo?.let { info ->
            runCatching { muxer.addTrack(info.format) }.getOrNull()
        } ?: -1
        var videoTrackIndex = -1

        val totalFrames = exportTotalFrameCount(trimRange.durationMs, exportFrameRate)
        val bufferInfo = MediaCodec.BufferInfo()
        var videoSampleIndex = 0

        // 逐帧渲染
        for (frameIdx in 0 until totalFrames) {
            val outputFrameTimeUs = exportFramePresentationTimeUs(frameIdx, exportFrameRate)
            val sourceFrameTimeUs = exportSourceFrameTimeUs(
                trimStartMs = trimRange.startMs,
                trimEndMs = trimRange.endMs,
                frameIndex = frameIdx,
                fps = exportFrameRate,
            )
            val elapsedSec = exportTrajectoryElapsedSecondsFromUs(outputFrameTimeUs, timing.impactTimeMs)

            // 提取原始视频帧
            val frameBitmap = retriever.getFrameAtTime(
                sourceFrameTimeUs,
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
            drawTrajectoryOnCanvas(c, bezier, arcTable, exportSelection.style, elapsedSec, width.toFloat(), height.toFloat(), timing, legacyProgressAtTime)

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
                        if (muxerStarted && bufferInfo.size > 0 &&
                            bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
                        ) {
                            bufferInfo.presentationTimeUs = exportFramePresentationTimeUs(videoSampleIndex, exportFrameRate)
                            muxer.writeSampleData(videoTrackIndex, buf, bufferInfo)
                            videoSampleIndex++
                        }
                        encoder.releaseOutputBuffer(outIdx, false)
                        outputDone = true
                    }
                    else -> outputDone = true
                }
            }

            onProgress(exportProgressFraction(completedFrames = frameIdx + 1, totalFrames = totalFrames))
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
                    bufferInfo.presentationTimeUs = exportFramePresentationTimeUs(videoSampleIndex, exportFrameRate)
                    muxer.writeSampleData(videoTrackIndex, buf, bufferInfo)
                    videoSampleIndex++
                }
                encoder.releaseOutputBuffer(outIdx, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) eosReached = true
            } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && !muxerStarted) {
                videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                muxer.start(); muxerStarted = true
            }
        }

        encoder.stop()
        encoderStarted = false
        encoder.release()
        encoder = null
        surface.release()
        surface = null

        if (muxerStarted && audioTrackInfo != null && audioMuxerTrackIndex >= 0) {
            val trimRangeUs = exportTrimRangeUs(trimRange)
            copyAudioTrackToMuxer(
                context = context,
                videoUri = videoUri,
                audioTrackInfo = audioTrackInfo,
                muxer = muxer,
                muxerTrackIndex = audioMuxerTrackIndex,
                trimStartUs = trimRangeUs.startUs,
                trimEndUs = trimRangeUs.endUs,
            )
        }
        if (muxerStarted) {
            muxer.stop()
            muxerStarted = false
        }
        muxer.release()
        muxer = null
        retriever.release()
        retrieverReleased = true

        onProgress(1f)
        exportSucceeded = true
        return@withContext outputFile
    } finally {
        if (!exportSucceeded) outputFile?.delete()
        if (encoderStarted) runCatching { encoder?.stop() }
        runCatching { encoder?.release() }
        runCatching { surface?.release() }
        if (muxerStarted) runCatching { muxer?.stop() }
        runCatching { muxer?.release() }
        if (!retrieverReleased) runCatching { retriever.release() }
    }
}

private suspend fun saveExportedVideoToGallery(
    context: Context,
    outputFile: File,
) = withContext(Dispatchers.IO) {
    check(isValidExportPreviewFile(outputFile)) { "导出预览文件不可用" }
    var insertedUri: Uri? = null
    var saveSucceeded = false
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, "GolfSwing_${System.currentTimeMillis()}.mp4")
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/GolfSwing")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
    }
    try {
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建相册文件")
        insertedUri = uri

        context.contentResolver.openOutputStream(uri)?.use { out ->
            outputFile.inputStream().copyTo(out)
        } ?: error("无法写入相册文件")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            val updatedRows = context.contentResolver.update(uri, values, null, null)
            check(mediaStorePendingPublishSucceeded(Build.VERSION.SDK_INT, updatedRows)) {
                "无法完成相册保存"
            }
        }
        saveSucceeded = true
    } catch (error: CancellationException) {
        if (shouldCleanupFailedGalleryInsert(insertedUriPresent = insertedUri != null, saveSucceeded = saveSucceeded)) {
            cleanupFailedGalleryInsert(context, insertedUri)
        }
        throw error
    } catch (error: Throwable) {
        if (shouldCleanupFailedGalleryInsert(insertedUriPresent = insertedUri != null, saveSucceeded = saveSucceeded)) {
            cleanupFailedGalleryInsert(context, insertedUri)
        }
        throw error
    }
}

private fun cleanupFailedGalleryInsert(context: Context, uri: Uri?) {
    if (uri == null) return
    runCatching { context.contentResolver.delete(uri, null, null) }
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

    canvas.withSave {
        when (rotation) {
            90 -> {
                translate(width.toFloat(), 0f)
                rotate(90f)
                drawBitmap(frameBitmap, src, Rect(0, 0, height, width), null)
            }
            180 -> {
                translate(width.toFloat(), height.toFloat())
                rotate(180f)
                drawBitmap(frameBitmap, src, Rect(0, 0, width, height), null)
            }
            270 -> {
                translate(0f, height.toFloat())
                rotate(270f)
                drawBitmap(frameBitmap, src, Rect(0, 0, height, width), null)
            }
            else -> {
                drawBitmap(frameBitmap, src, Rect(0, 0, width, height), null)
            }
        }
    }
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
        val maxInputSize = audioCopyBufferSize(if (audioTrackInfo.format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            audioTrackInfo.format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            null
        })
        val buffer = ByteBuffer.allocate(maxInputSize)
        val info = MediaCodec.BufferInfo()

        while (true) {
            val sampleTimeUs = extractor.sampleTime
            if (sampleTimeUs < 0 || sampleTimeUs >= trimEndUs) break
            val presentationTimeUs = trimmedAudioPresentationTimeUs(
                sampleTimeUs = sampleTimeUs,
                trimStartUs = trimStartUs,
                trimEndUs = trimEndUs,
            )
            if (presentationTimeUs == null) {
                extractor.advance()
                continue
            }
            buffer.clear()
            val sampleSize = extractor.readSampleData(buffer, 0)
            when (audioSampleCopyAction(sampleSize)) {
                AudioSampleCopyAction.END -> break
                AudioSampleCopyAction.SKIP -> {
                    if (!extractor.advance()) break
                    continue
                }
                AudioSampleCopyAction.WRITE -> Unit
            }
            val muxerPresentationTimeUs = audioMuxerPresentationTimeUs(
                sampleTimeUs = sampleTimeUs,
                sampleSize = sampleSize,
                trimStartUs = trimStartUs,
                trimEndUs = trimEndUs,
            ) ?: break
            info.set(0, sampleSize, muxerPresentationTimeUs, audioMuxerSampleFlags(extractor.sampleFlags))
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
        val glowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.style = android.graphics.Paint.Style.FILL
        }
        val glowAlphas = TrajectoryOverlayRenderSpec.headGlowAlphas(style.glowOpacity.toFloat())
        glowPaint.color = trackColorWithAlpha(style.primaryColorArgb(), glowAlphas.outer)
        canvas.drawCircle(headX, headY, headGlowRadius * 1.8f, glowPaint)
        glowPaint.color = trackColorWithAlpha(style.primaryColorArgb(), glowAlphas.body)
        canvas.drawCircle(headX, headY, headGlowRadius, glowPaint)
        glowPaint.color = android.graphics.Color.argb(TrajectoryOverlayRenderSpec.alphaByte(glowAlphas.core), 255, 255, 255)
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

private fun trackColorWithAlpha(argb: Long, alpha: Float): Int {
    return android.graphics.Color.argb(
        TrajectoryOverlayRenderSpec.alphaByte(alpha),
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
