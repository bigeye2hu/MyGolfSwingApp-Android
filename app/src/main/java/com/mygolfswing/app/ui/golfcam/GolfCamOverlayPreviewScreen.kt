package com.mygolfswing.app.ui.golfcam

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.mygolfswing.app.ui.golfcam.model.GolfCamTrailGeometry
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
import kotlin.math.max
import kotlin.math.roundToInt

private data class VideoPlaybackConfig(
    val frameRate: Double = 30.0,
    val aspectRatio: Float = 9f / 16f,
)

/**
 * 预览 + 导出屏（对齐 iOS OverlayPreviewView）。
 * 播放带轨迹叠加的视频，可导出为 MP4 保存到相册。
 */
@Composable
fun GolfCamOverlayPreviewScreen(
    videoUri: Uri,
    selection: TrackStyleSelection,
    impactTimeMs: Long = 0L,
    onDismiss: () -> Unit,
    onReEdit: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var playing by remember { mutableStateOf(true) }  // 进入预览页自动播放
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var exporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportDone by remember { mutableStateOf(false) }
    var videoConfig by remember { mutableStateOf(VideoPlaybackConfig()) }

    LaunchedEffect(videoUri) {
        videoConfig = withContext(Dispatchers.IO) {
            readVideoPlaybackConfig(context, videoUri)
        }
    }

    val impactMs = impactTimeMs
    val flightTimeMs = (selection.flightCurve.landingPoint.flightTimeSec * 1000)
        .toLong()
        .coerceAtLeast(500L)
    val timing = TrajectoryTiming(
        impactTimeMs = impactMs,
        landingTimeMs = impactMs + flightTimeMs,
        fps = videoConfig.frameRate,
    )

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    LaunchedEffect(playing) {
        if (playing) player.play() else player.pause()
    }
    LaunchedEffect(playing) {
        while (playing) {
            elapsedMs = (player.currentPosition - impactMs).coerceAtLeast(0L)
            delay(16L)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val displaySize = VideoDisplayGeometry.fit(
                containerWidth = maxWidth.value,
                containerHeight = maxHeight.value,
                videoAspectRatio = videoConfig.aspectRatio,
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(displaySize.width.dp, displaySize.height.dp),
            ) {
                AndroidView(
                    factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = false } },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { playing = !playing }
                        },
                )

                TrajectoryAnimationOverlay(
                    bezier = selection.flightCurve.bezierPath,
                    timing = timing,
                    style = selection.style,
                    playing = playing,
                    elapsedMs = elapsedMs,
                )
            }
        }

        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text("预览", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 4.dp))
        }

        // 底部操作栏
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (exporting) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("正在导出...", color = Color.White, fontSize = 13.sp)
                    LinearProgressIndicator(
                        progress = { exportProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = DSColors.fairway,
                        trackColor = Color.White.copy(alpha = 0.2f),
                    )
                }
            } else if (exportDone) {
                Text("✅ 已保存到相册", color = DSColors.fairway, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onReEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("重新编辑", color = Color.White)
                }
                Button(
                    onClick = {
                        if (!exporting && !exportDone) {
                            scope.launch {
                                exporting = true
                                exportProgress = 0f
                                try {
                                    exportVideoWithTrajectory(
                                        context = context,
                                        videoUri = videoUri,
                                        selection = selection,
                                        timing = timing,
                                        onProgress = { exportProgress = it },
                                    )
                                    exportDone = true
                                } finally {
                                    exporting = false
                                }
                            }
                        }
                    },
                    enabled = !exporting && !exportDone,
                    colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Download, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("保存到相册", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── 视频导出（逐帧渲染轨迹叠加）─────────────────────────────────────────────────

private suspend fun exportVideoWithTrajectory(
    context: Context,
    videoUri: Uri,
    selection: TrackStyleSelection,
    timing: TrajectoryTiming,
    onProgress: (Float) -> Unit,
) = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(context, videoUri)

    val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 1280
    val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 720
    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 3000L
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
    var trackIndex = -1
    var muxerStarted = false

    val totalFrames = (durationMs * fps / 1000).toInt().coerceAtLeast(1)
    val bufferInfo = MediaCodec.BufferInfo()

    // 逐帧渲染
    for (frameIdx in 0 until totalFrames) {
        val frameTimeMs = frameIdx * 1000L / fps
        val elapsedSec = (frameTimeMs - timing.impactTimeMs).toDouble() / 1000.0

        // 提取原始视频帧
        val frameBitmap = retriever.getFrameAtTime(
            frameTimeMs * 1000L,
            MediaMetadataRetriever.OPTION_CLOSEST,
        )

        val c = surface.lockCanvas(null)
        if (frameBitmap != null) {
            c.drawBitmap(frameBitmap, 0f, 0f, null)
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
                        trackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                }
                outIdx >= 0 -> {
                    val buf = encoder.getOutputBuffer(outIdx) ?: run {
                        encoder.releaseOutputBuffer(outIdx, false); outputDone = true; continue
                    }
                    bufferInfo.presentationTimeUs = frameTimeMs * 1000L
                    if (muxerStarted && bufferInfo.size > 0 &&
                        bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
                    ) {
                        muxer.writeSampleData(trackIndex, buf, bufferInfo)
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
                muxer.writeSampleData(trackIndex, buf, bufferInfo)
            }
            encoder.releaseOutputBuffer(outIdx, false)
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) eosReached = true
        } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && !muxerStarted) {
            trackIndex = muxer.addTrack(encoder.outputFormat)
            muxer.start(); muxerStarted = true
        }
    }

    encoder.stop(); encoder.release()
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
    val tuning = style.renderTuning

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
    val headGlowRadius = GolfCamTrailGeometry.strokeWidth(
        widthPct = style.widthPct,
        videoHeight = height,
        globalProgress = snapshot.headProgress,
        apexProgress = snapshot.apexProgress,
        tuning = tuning,
    ) * 1.25f
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
    val aspectRatio = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 9
            val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 16
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0
            val width = if (rotation == 90 || rotation == 270) rawHeight else rawWidth
            val height = if (rotation == 90 || rotation == 270) rawWidth else rawHeight
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
                )
            }
        }
        30.0
    } catch (_: Exception) {
        30.0
    } finally {
        extractor.release()
    }
    return VideoPlaybackConfig(frameRate = frameRate, aspectRatio = aspectRatio)
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
    unitPaint.color = android.graphics.Color.argb(shadowAlpha, 0, 0, 0)
    valuePaint.color = android.graphics.Color.argb(shadowAlpha, 0, 0, 0)
    canvas.drawText(unit, layout.unitLeft + shadowOffset, layout.unitTop - unitMetrics.ascent + shadowOffset, unitPaint)
    canvas.drawText(valueText, layout.valueLeft + shadowOffset, layout.valueTop - valueMetrics.ascent + shadowOffset, valuePaint)

    unitPaint.color = android.graphics.Color.WHITE
    valuePaint.color = android.graphics.Color.WHITE
    canvas.drawText(unit, layout.unitLeft, layout.unitTop - unitMetrics.ascent, unitPaint)
    canvas.drawText(valueText, layout.valueLeft, layout.valueTop - valueMetrics.ascent, valuePaint)
}
