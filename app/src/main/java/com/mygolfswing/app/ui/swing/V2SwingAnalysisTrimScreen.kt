package com.mygolfswing.app.ui.swing

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.data.video.MIN_VIDEO_TRIM_DURATION_MS
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.data.video.importedVideoDurationMs
import com.mygolfswing.app.data.video.normalizedVideoTrimRange
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.v2FullScreenInsets
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.golfcam.steps.GolfCamThumbnailCache
import com.mygolfswing.app.ui.golfcam.steps.golfCamFillMissingThumbnailSlotsFromNearest
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private const val ANALYSIS_TRIM_THUMBNAIL_COUNT = 15
private const val ANALYSIS_TRIM_HANDLE_WIDTH_DP = 12
private const val ANALYSIS_TRIM_BAR_HEIGHT_DP = 64

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun V2SwingAnalysisTrimScreen(
    imported: V2VideoFileStore.ImportResult,
    saving: Boolean,
    error: String?,
    secondaryActionTitle: String = "重新选择",
    onCancel: () -> Unit,
    onUse: (startMs: Long, endMs: Long) -> Unit,
    extraControls: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val videoUri = remember(imported.absolutePath) { Uri.fromFile(File(imported.absolutePath)) }
    val validDurationMs = remember(imported.durationSec) { importedVideoDurationMs(imported.durationSec) }
    val durationMs = validDurationMs ?: 1L
    var startMs by remember(imported.absolutePath) { mutableLongStateOf(0L) }
    var endMs by remember(imported.absolutePath, durationMs) { mutableLongStateOf(durationMs) }
    var currentPositionMs by remember(imported.absolutePath) { mutableLongStateOf(0L) }
    var playerReady by remember(imported.absolutePath) { mutableStateOf(false) }
    var loadError by remember(imported.absolutePath) {
        mutableStateOf(if (validDurationMs == null) "无法读取视频时长，请重新选择视频。" else null)
    }
    val selectedRange = normalizedVideoTrimRange(durationMs, startMs, endMs)

    val player = remember(videoUri, validDurationMs) {
        validDurationMs?.let {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUri))
                playWhenReady = true
                prepare()
            }
        }
    }

    V2BackHandler(enabled = !saving) { onCancel() }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !playerReady) {
                    playerReady = true
                    loadError = null
                    player?.seekTo(0L)
                    player?.play()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playerReady = false
                loadError = "视频加载失败，请重新选择视频。"
            }
        }
        player?.addListener(listener)
        if (player?.playbackState == Player.STATE_READY) {
            playerReady = true
            loadError = null
            player.seekTo(0L)
            player.play()
        }
        onDispose {
            player?.removeListener(listener)
            player?.release()
        }
    }

    LaunchedEffect(player, playerReady, startMs, endMs) {
        if (player == null || !playerReady) return@LaunchedEffect
        while (true) {
            val position = player.currentPosition.coerceAtLeast(0L)
            currentPositionMs = position
            if (position < startMs || position >= endMs) {
                player.seekTo(startMs)
                currentPositionMs = startMs
            }
            delay(33L)
        }
    }

    V2SwingAnalysisTrimContent(
        durationMs = durationMs,
        startMs = selectedRange?.startMs ?: 0L,
        endMs = selectedRange?.endMs ?: durationMs,
        currentPositionMs = currentPositionMs,
        ready = playerReady && selectedRange != null,
        saving = saving,
        loadError = loadError,
        error = error,
        secondaryActionTitle = secondaryActionTitle,
        videoUri = videoUri,
        onCancel = onCancel,
        onUse = { selectedRange?.let { onUse(it.startMs, it.endMs) } },
        onStartChange = { next ->
            normalizedVideoTrimRange(durationMs, next, endMs)?.let { safe ->
                player?.pause()
                startMs = safe.startMs
                endMs = safe.endMs
                currentPositionMs = safe.startMs
                player?.seekTo(safe.startMs)
            }
        },
        onEndChange = { next ->
            normalizedVideoTrimRange(durationMs, startMs, next)?.let { safe ->
                player?.pause()
                startMs = safe.startMs
                endMs = safe.endMs
                currentPositionMs = safe.endMs
                player?.seekTo(safe.endMs)
            }
        },
        onPlayheadSeek = { next ->
            currentPositionMs = next
            player?.seekTo(next)
        },
        onTrimEditingEnded = { player?.play() },
        preview = {
            player?.let { activePlayer ->
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = activePlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    update = { it.player = activePlayer },
                )
            }
        },
        extraControls = extraControls,
    )
}

@Composable
internal fun V2SwingAnalysisTrimContent(
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    currentPositionMs: Long,
    ready: Boolean,
    saving: Boolean,
    loadError: String?,
    error: String?,
    secondaryActionTitle: String = "重新选择",
    videoUri: Uri? = null,
    thumbnailOverride: List<ImageBitmap?>? = null,
    onCancel: () -> Unit,
    onUse: () -> Unit,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit,
    onPlayheadSeek: (Long) -> Unit,
    onTrimEditingEnded: () -> Unit,
    preview: @Composable () -> Unit,
    extraControls: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .v2FullScreenInsets(),
    ) {
        Column(Modifier.fillMaxSize()) {
            AnalysisTrimHeader(enabled = !saving, onCancel = onCancel)
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    ready -> preview()
                    loadError != null -> AnalysisTrimLoadError(loadError)
                    else -> CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (ready) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = analysisTrimSelectionSummary(startMs, endMs, durationMs),
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                    )
                    AnalysisTrimTimeline(
                        videoUri = videoUri,
                        durationMs = durationMs,
                        startMs = startMs,
                        endMs = endMs,
                        currentPositionMs = currentPositionMs,
                        enabled = !saving,
                        thumbnailOverride = thumbnailOverride,
                        onStartChange = onStartChange,
                        onEndChange = onEndChange,
                        onPlayheadSeek = onPlayheadSeek,
                        onTrimEditingEnded = onTrimEditingEnded,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(64.dp),
                    )
                    extraControls?.invoke()
                }
            }
            error?.let {
                Text(
                    text = it,
                    color = DSColors.flagRed,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
                )
            }
            AnalysisTrimBottomBar(
                secondaryActionTitle = secondaryActionTitle,
                ready = ready,
                saving = saving,
                onCancel = onCancel,
                onUse = onUse,
            )
        }
    }
}

@Composable
private fun AnalysisTrimHeader(enabled: Boolean, onCancel: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "取消",
            color = Color.White.copy(alpha = if (enabled) 1f else 0.38f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(enabled = enabled, onClick = onCancel),
        )
        Spacer(Modifier.weight(1f))
        Text("修剪视频", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(
            "取消",
            color = Color.Transparent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

@Composable
private fun AnalysisTrimLoadError(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 40.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(30.dp),
        )
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AnalysisTrimBottomBar(
    secondaryActionTitle: String,
    ready: Boolean,
    saving: Boolean,
    onCancel: () -> Unit,
    onUse: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(Color.White.copy(alpha = 0.14f))
                .clickable(enabled = !saving, onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                secondaryActionTitle,
                color = Color.White.copy(alpha = if (saving) 0.38f else 0.85f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(if (ready) DSColors.fairway else Color.White.copy(alpha = 0.14f))
                .clickable(enabled = ready && !saving, onClick = onUse),
            contentAlignment = Alignment.Center,
        ) {
            if (saving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("使用", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private enum class AnalysisTrimDragTarget { START, END, PLAYHEAD }

@Composable
private fun AnalysisTrimTimeline(
    videoUri: Uri?,
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    currentPositionMs: Long,
    enabled: Boolean,
    thumbnailOverride: List<ImageBitmap?>?,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit,
    onPlayheadSeek: (Long) -> Unit,
    onTrimEditingEnded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sampleTimes = remember(durationMs) { analysisTrimThumbnailSampleTimesMs(durationMs) }
    var thumbnails by remember(videoUri, sampleTimes, thumbnailOverride) {
        mutableStateOf(
            thumbnailOverride ?: videoUri?.let { uri ->
                GolfCamThumbnailCache.cachedTimelineThumbnails(uri, sampleTimes).map { it?.asImageBitmap() }
            }.orEmpty(),
        )
    }

    LaunchedEffect(videoUri, sampleTimes, thumbnailOverride) {
        if (videoUri == null || thumbnailOverride != null || sampleTimes.isEmpty()) return@LaunchedEffect
        val exactCached = GolfCamThumbnailCache.cachedExactSampleTimes(videoUri, sampleTimes)
        thumbnails = GolfCamThumbnailCache.cachedTimelineThumbnails(videoUri, sampleTimes).map { it?.asImageBitmap() }
        GolfCamThumbnailCache.loadTimelineThumbnails(
            context = context,
            videoUri = videoUri,
            sampleTimesMs = sampleTimes,
            priorityTimeMs = startMs,
            skipCachedSampleTimesMs = exactCached,
            frameOption = MediaMetadataRetriever.OPTION_CLOSEST,
        ) { index, bitmap ->
            thumbnails = golfCamFillMissingThumbnailSlotsFromNearest(
                thumbnails.toMutableList().also { current ->
                    if (index in current.indices) current[index] = bitmap.asImageBitmap()
                },
            )
        }
    }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val minDuration = MIN_VIDEO_TRIM_DURATION_MS.coerceAtMost(safeDuration)
    val startRatio = (startMs.toFloat() / safeDuration).coerceIn(0f, 1f)
    val endRatio = (endMs.toFloat() / safeDuration).coerceIn(startRatio, 1f)
    val playheadRatio = (currentPositionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
        .takeIf { currentPositionMs in startMs..endMs }
    val density = LocalDensity.current
    val latestStartMs by rememberUpdatedState(startMs)
    val latestEndMs by rememberUpdatedState(endMs)
    val latestCurrentPositionMs by rememberUpdatedState(currentPositionMs)
    val latestOnStartChange by rememberUpdatedState(onStartChange)
    val latestOnEndChange by rememberUpdatedState(onEndChange)
    val latestOnPlayheadSeek by rememberUpdatedState(onPlayheadSeek)
    val latestOnTrimEditingEnded by rememberUpdatedState(onTrimEditingEnded)

    BoxWithConstraints(modifier = modifier.semantics { contentDescription = "视频裁剪时间轴" }) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val stripWidthDp = maxWidth - (ANALYSIS_TRIM_HANDLE_WIDTH_DP * 2).dp
        val handlePx = with(density) { ANALYSIS_TRIM_HANDLE_WIDTH_DP.dp.toPx() }
        val innerWidthPx = (totalWidthPx - handlePx * 2f).coerceAtLeast(1f)
        val touchRadiusPx = with(density) { 20.dp.toPx() }
        var activeTarget by remember { mutableStateOf<AnalysisTrimDragTarget?>(null) }
        var initialTimeMs by remember { mutableLongStateOf(0L) }
        var accumulatedX by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled, totalWidthPx, safeDuration) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { position ->
                            val currentStartRatio = (latestStartMs.toFloat() / safeDuration).coerceIn(0f, 1f)
                            val currentEndRatio = (latestEndMs.toFloat() / safeDuration).coerceIn(currentStartRatio, 1f)
                            val currentPlayheadRatio = (latestCurrentPositionMs.toFloat() / safeDuration)
                                .coerceIn(0f, 1f)
                                .takeIf { latestCurrentPositionMs in latestStartMs..latestEndMs }
                            val startCenter = innerWidthPx * currentStartRatio + handlePx / 2f
                            val endCenter = innerWidthPx * currentEndRatio + handlePx * 1.5f
                            val playheadX = currentPlayheadRatio?.let { handlePx + innerWidthPx * it }
                            activeTarget = when {
                                abs(position.x - startCenter) <= touchRadiusPx -> AnalysisTrimDragTarget.START
                                abs(position.x - endCenter) <= touchRadiusPx -> AnalysisTrimDragTarget.END
                                playheadX != null && abs(position.x - playheadX) <= touchRadiusPx -> AnalysisTrimDragTarget.PLAYHEAD
                                else -> null
                            }
                            initialTimeMs = when (activeTarget) {
                                AnalysisTrimDragTarget.START -> latestStartMs
                                AnalysisTrimDragTarget.END -> latestEndMs
                                AnalysisTrimDragTarget.PLAYHEAD -> latestCurrentPositionMs
                                null -> 0L
                            }
                            accumulatedX = 0f
                        },
                        onDrag = { change, dragAmount ->
                            val target = activeTarget ?: return@detectDragGestures
                            change.consume()
                            accumulatedX += dragAmount.x
                            val deltaMs = (accumulatedX / innerWidthPx * safeDuration).roundToLong()
                            when (target) {
                                AnalysisTrimDragTarget.START -> latestOnStartChange(
                                    (initialTimeMs + deltaMs).coerceIn(0L, (latestEndMs - minDuration).coerceAtLeast(0L)),
                                )
                                AnalysisTrimDragTarget.END -> latestOnEndChange(
                                    (initialTimeMs + deltaMs).coerceIn((latestStartMs + minDuration).coerceAtMost(safeDuration), safeDuration),
                                )
                                AnalysisTrimDragTarget.PLAYHEAD -> latestOnPlayheadSeek(
                                    (initialTimeMs + deltaMs).coerceIn(latestStartMs, latestEndMs),
                                )
                            }
                        },
                        onDragEnd = {
                            if (activeTarget == AnalysisTrimDragTarget.START || activeTarget == AnalysisTrimDragTarget.END) {
                                latestOnTrimEditingEnded()
                            }
                            activeTarget = null
                        },
                        onDragCancel = { activeTarget = null },
                    )
                },
        ) {
            Row(
                modifier = Modifier
                    .offset(x = ANALYSIS_TRIM_HANDLE_WIDTH_DP.dp)
                    .width(stripWidthDp)
                    .height(ANALYSIS_TRIM_BAR_HEIGHT_DP.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(DSColors.mistGreen, DSColors.mistGreen.copy(alpha = 0.68f), DSColors.mistGreen),
                        ),
                    ),
            ) {
                if (thumbnails.isNotEmpty() && thumbnails.any { it != null }) {
                    thumbnails.forEach { thumbnail ->
                        if (thumbnail != null) {
                            Image(
                                bitmap = thumbnail,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.Low,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                            )
                        } else {
                            Spacer(Modifier.weight(1f).fillMaxSize())
                        }
                    }
                }
            }

            Canvas(Modifier.fillMaxSize()) {
                val stripX = handlePx
                val stripWidth = (size.width - handlePx * 2f).coerceAtLeast(1f)
                val endX = stripX + stripWidth * endRatio
                val mask = Color.Black.copy(alpha = 0.6f)
                if (startRatio > 0f) drawRect(mask, Offset(stripX, 0f), Size(stripWidth * startRatio, size.height))
                if (endRatio < 1f) drawRect(mask, Offset(endX, 0f), Size(stripWidth * (1f - endRatio), size.height))

                val selectionLeft = stripWidth * startRatio
                val selectionWidth = (stripWidth * (endRatio - startRatio) + handlePx * 2f).coerceAtLeast(handlePx * 2f)
                val corner = 6.dp.toPx()
                val safeSelectionWidth = selectionWidth.coerceAtMost(size.width - selectionLeft)
                drawRoundRect(
                    DSColors.fairway.copy(alpha = 0.08f),
                    topLeft = Offset(selectionLeft, 0f),
                    size = Size(safeSelectionWidth, size.height),
                    cornerRadius = CornerRadius(corner),
                )
                drawRoundRect(
                    DSColors.fairway.copy(alpha = 0.86f),
                    topLeft = Offset(selectionLeft + 0.75f, 0.75f),
                    size = Size((safeSelectionWidth - 1.5f).coerceAtLeast(0f), (size.height - 1.5f).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius(corner),
                    style = Stroke(width = 1.5f),
                )

                playheadRatio?.let { ratio ->
                    val x = stripX + stripWidth * ratio
                    val top = 2.dp.toPx()
                    val bottom = size.height - 2.dp.toPx()
                    drawLine(Color.Black.copy(alpha = 0.5f), Offset(x, top), Offset(x, bottom), 4.dp.toPx())
                    drawLine(Color.White, Offset(x, top), Offset(x, bottom), 2.dp.toPx())
                }
            }

            AnalysisTrimHandle(modifier = Modifier.offset { IntOffset((innerWidthPx * startRatio).toInt(), 0) })
            AnalysisTrimHandle(modifier = Modifier.offset { IntOffset((innerWidthPx * endRatio + handlePx).toInt(), 0) })
        }
    }
}

@Composable
private fun AnalysisTrimHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(ANALYSIS_TRIM_HANDLE_WIDTH_DP.dp)
            .height(ANALYSIS_TRIM_BAR_HEIGHT_DP.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.verticalGradient(listOf(DSColors.fairway, DSColors.deepGreen))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = Stroke(width = 0.8.dp.toPx()),
            )
            val top = (size.height - 12.dp.toPx()) / 2f
            val bottom = top + 12.dp.toPx()
            drawLine(Color.White.copy(alpha = 0.78f), Offset(size.width / 2f - 2.5.dp.toPx(), top), Offset(size.width / 2f - 2.5.dp.toPx(), bottom), 1.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.78f), Offset(size.width / 2f + 2.5.dp.toPx(), top), Offset(size.width / 2f + 2.5.dp.toPx(), bottom), 1.dp.toPx())
        }
    }
}

internal fun analysisTrimThumbnailSampleTimesMs(
    durationMs: Long,
    count: Int = ANALYSIS_TRIM_THUMBNAIL_COUNT,
): List<Long> {
    if (durationMs <= 0L || count <= 0) return emptyList()
    val interval = durationMs.toDouble() / count.toDouble()
    return (0 until count).map { index ->
        ((index.toDouble() + 0.5) * interval)
            .roundToLong()
            .coerceIn(0L, (durationMs - 1L).coerceAtLeast(0L))
    }
}

internal fun analysisTrimSelectionSummary(startMs: Long, endMs: Long, durationMs: Long): String =
    String.format(
        Locale.US,
        "已选取 %.1f 秒 / 共 %.1f 秒",
        (endMs - startMs).coerceAtLeast(0L) / 1_000.0,
        durationMs.coerceAtLeast(0L) / 1_000.0,
    )

internal fun swingAnalysisTrimDurationMs(durationSec: Double): Long =
    importedVideoDurationMs(durationSec) ?: 1L
