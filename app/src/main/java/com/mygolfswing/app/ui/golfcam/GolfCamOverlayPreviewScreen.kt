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
import androidx.compose.material.icons.filled.ArrowBack
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
import com.mygolfswing.app.ui.golfcam.model.GolfCamSpeedConfig
import com.mygolfswing.app.ui.golfcam.model.NormalizedPoint
import com.mygolfswing.app.ui.golfcam.model.TrackStyle
import com.mygolfswing.app.ui.golfcam.model.TrackStyleSelection
import com.mygolfswing.app.ui.golfcam.model.TrajectoryAlgorithmKind
import com.mygolfswing.app.ui.golfcam.model.TrajectoryMotionModel
import com.mygolfswing.app.ui.golfcam.model.TrajectoryTiming
import com.mygolfswing.app.ui.golfcam.render.TrajectoryAnimationOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

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

    val impactMs = impactTimeMs
    val flightTimeMs = (selection.flightCurve.landingPoint.flightTimeSec * 1000)
        .toLong()
        .coerceAtLeast(500L)
    val timing = TrajectoryTiming(
        impactTimeMs = impactMs,
        landingTimeMs = impactMs + flightTimeMs,
        fps = 30.0,
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
        // 视频播放层（点击切换播放/暂停）
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = false } },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { playing = !playing }
                },
        )

        // 轨迹叠加层
        TrajectoryAnimationOverlay(
            bezier = selection.flightCurve.bezierPath,
            timing = timing,
            style = selection.style,
            playing = playing,
            elapsedMs = elapsedMs,
        )

        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
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
    val fps = 30

    val model = TrajectoryMotionModel(
        bezier = selection.flightCurve.bezierPath,
        timing = timing,
        params = TrajectoryMotionModel.parametersFromStyle(selection.style),
    )

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
        drawTrajectoryOnCanvas(c, model, selection.style, elapsedSec, width.toFloat(), height.toFloat(), timing)

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
    model: TrajectoryMotionModel,
    style: TrackStyle,
    elapsedSec: Double,
    width: Float,
    height: Float,
    timing: TrajectoryTiming,
) {
    val headProgress = if (style.activeTrajectoryAlgorithm == TrajectoryAlgorithmKind.GOLF_CAM_LIKE) {
        GolfCamSpeedConfig.progress(elapsedSec / timing.totalDurationSec, style.speedLevel)
    } else {
        model.progressAtTime(elapsedSec)
    }.toFloat()
        .coerceIn(0f, 1f)
    if (headProgress <= 0f) return

    val tailProgress = (headProgress - (style.tailHalfLife / 100.0 / timing.totalDurationSec).toFloat() * 3f).coerceAtLeast(0f)
    val tuning = style.renderTuning
    val bandAlpha = trackBandAlpha(style)
    val baseStrokeWidth = 8f * (style.widthPct / 50.0).toFloat()
    val taperPower = style.renderTuning.widthTaperPower.takeIf { it > 0f } ?: style.taperPower

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.BUTT
    }

    val steps = 120
    fun drawSegmentPass(widthMultiplier: Float, alphaFor: (tailFade: Float, globalProgress: Float) -> Float) {
        for (i in 0 until steps) {
            val t0 = tailProgress + (headProgress - tailProgress) * (i.toFloat() / steps)
            val t1 = tailProgress + (headProgress - tailProgress) * ((i + 1).toFloat() / steps)
            if (t1 <= tailProgress || t0 >= headProgress) continue

            val p0 = model.bezier.point(t0)
            val p1 = model.bezier.point(t1)
            val colorProgress = t0.coerceIn(0f, 1f)
            val localProgress = (t0 - tailProgress) / (headProgress - tailProgress).coerceAtLeast(0.001f)
            val alpha = (alphaFor(localProgress.coerceIn(0f, 1f), colorProgress) * 255f).toInt().coerceIn(0, 255)
            if (alpha <= 0) continue
            val taper = Math.pow((1f - colorProgress).toDouble(), taperPower.toDouble()).toFloat().coerceIn(0.05f, 1f)

            paint.color = interpolateTrackColor(style.colorPreset.gradientColors, colorProgress, alpha)
            paint.strokeWidth = baseStrokeWidth * taper * widthMultiplier
            canvas.drawLine(p0.x * width, p0.y * height, p1.x * width, p1.y * height, paint)
        }
    }

    if (tuning.showOuterGlow) {
        val glowAlpha = (style.colorAlpha * style.glowOpacity * tuning.outerGlowMultiplier)
            .toFloat()
            .coerceIn(0f, 0.70f)
        val glowWidth = 1.35f + tuning.glowBlurMultiplier.coerceIn(0.25f, 1.20f)
        drawSegmentPass(widthMultiplier = glowWidth) { tailFade, _ ->
            glowAlpha * (0.10f + 0.30f * Math.pow(tailFade.toDouble(), 1.55).toFloat())
        }
    }

    if (tuning.showBodyBand) {
        drawSegmentPass(widthMultiplier = 1f) { tailFade, _ ->
            bandAlpha * (0.02f + 0.20f * Math.pow(tailFade.toDouble(), 1.55).toFloat())
        }
        drawSegmentPass(widthMultiplier = 1f) { tailFade, _ ->
            bandAlpha * (tuning.bodyAlphaBase + tuning.bodyAlphaScale * Math.pow(tailFade.toDouble(), 1.60).toFloat())
        }
    }

    if (tuning.showHighlightCore) {
        drawSegmentPass(widthMultiplier = tuning.highlightWidthRatio.coerceIn(0.30f, 0.85f)) { tailFade, progress ->
            val weight = highlightWeight(progress, tuning.highlightStart, tuning.highlightPeak, tuning.highlightEnd, tuning.highlightFalloffPower)
            if (weight <= 0.001f || tailFade <= 0.001f) {
                0f
            } else {
                minOf(
                    tuning.highlightAlphaMax,
                    bandAlpha * tuning.highlightStrength * (0.14f + 0.62f * weight),
                ) * weight * Math.pow(tailFade.toDouble(), 1.60).toFloat()
            }
        }
    }

    val head = model.bezier.point(headProgress)
    val headX = head.x * width
    val headY = head.y * height
    val glowAlpha = style.glowOpacity.toFloat().coerceIn(0f, 1f)
    val glowColor = interpolateTrackColor(style.colorPreset.gradientColors, 1f, (255 * glowAlpha).toInt().coerceIn(0, 255))
    val glowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.style = android.graphics.Paint.Style.FILL
    }
    glowPaint.color = glowColor
    canvas.drawCircle(headX, headY, baseStrokeWidth * 2.5f * 1.8f, glowPaint)
    glowPaint.alpha = (190 * glowAlpha).toInt().coerceIn(0, 255)
    canvas.drawCircle(headX, headY, baseStrokeWidth * 2.5f, glowPaint)
    glowPaint.color = android.graphics.Color.argb((230 * glowAlpha).toInt().coerceIn(0, 255), 255, 255, 255)
    canvas.drawCircle(headX, headY, baseStrokeWidth, glowPaint)

    if (style.showDistance) {
        val displayDist = GolfCamSpeedConfig.displayDistance(
            totalDistance = style.totalDistance,
            elapsedSec = elapsedSec,
            totalDurationSec = timing.totalDurationSec,
            speedLevel = style.speedLevel,
        )
        drawDistanceLabelOnCanvas(canvas, headX, headY, displayDist, style.distanceUnit.abbreviation, width, height)
    }
}

private fun trackBandAlpha(style: TrackStyle): Float =
    (style.colorAlpha * style.colorPreset.bandAlpha * style.renderTuning.bandAlphaMultiplier).toFloat().coerceIn(0f, 1f)

private fun interpolateTrackColor(colors: List<Long>, progress: Float, alpha: Int): Int {
    val effectiveColors = if (colors.isEmpty()) listOf(0xFFFFFFFF) else colors
    if (effectiveColors.size == 1) {
        val c = effectiveColors.first()
        return android.graphics.Color.argb(alpha, ((c ushr 16) and 0xFF).toInt(), ((c ushr 8) and 0xFF).toInt(), (c and 0xFF).toInt())
    }

    val scaled = progress.coerceIn(0f, 1f) * (effectiveColors.size - 1)
    val index = scaled.toInt().coerceIn(0, effectiveColors.size - 2)
    val fraction = scaled - index
    val a = effectiveColors[index]
    val b = effectiveColors[index + 1]
    fun channel(shift: Int): Int {
        val av = ((a ushr shift) and 0xFF).toInt()
        val bv = ((b ushr shift) and 0xFF).toInt()
        return (av + (bv - av) * fraction).toInt().coerceIn(0, 255)
    }
    return android.graphics.Color.argb(alpha, channel(16), channel(8), channel(0))
}

private fun highlightWeight(progress: Float, start: Float, peak: Float, end: Float, falloffPower: Float): Float {
    if (progress < start || progress > end) return 0f
    return if (progress <= peak) {
        smoothStep(start, peak, progress)
    } else {
        val t = smoothStep(peak, end, progress)
        Math.pow((1f - t).coerceIn(0f, 1f).toDouble(), falloffPower.toDouble()).toFloat()
    }
}

private fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
    if (kotlin.math.abs(edge1 - edge0) <= 0.0001f) return if (value >= edge1) 1f else 0f
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun drawDistanceLabelOnCanvas(
    canvas: AndroidCanvas,
    x: Float,
    y: Float,
    distance: Int,
    unit: String,
    width: Float,
    height: Float,
) {
    if (distance <= 0) return

    val scale = max(width, height) / 720f
    val text = "$distance$unit"
    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 13f * scale.coerceAtLeast(1f)
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val metrics = textPaint.fontMetrics
    val textWidth = textPaint.measureText(text)
    val textHeight = metrics.descent - metrics.ascent
    val paddingX = 6f * scale
    val paddingY = 4f * scale
    val labelWidth = textWidth + paddingX * 2f
    val labelHeight = textHeight + paddingY * 2f
    val left = (x - labelWidth / 2f).coerceIn(0f, (width - labelWidth).coerceAtLeast(0f))
    val top = (y - labelHeight - 16f * scale).coerceIn(0f, (height - labelHeight).coerceAtLeast(0f))

    val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(115, 0, 0, 0)
        style = android.graphics.Paint.Style.FILL
    }
    val rect = android.graphics.RectF(left, top, left + labelWidth, top + labelHeight)
    canvas.drawRoundRect(rect, 6f * scale, 6f * scale, bgPaint)
    canvas.drawText(text, left + paddingX, top + paddingY - metrics.ascent, textPaint)
}
