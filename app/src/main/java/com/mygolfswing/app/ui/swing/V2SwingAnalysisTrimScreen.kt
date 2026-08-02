package com.mygolfswing.app.ui.swing

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
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
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import kotlin.math.roundToLong

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun V2SwingAnalysisTrimScreen(
    imported: V2VideoFileStore.ImportResult,
    saving: Boolean,
    error: String?,
    onCancel: () -> Unit,
    onUse: (startMs: Long, endMs: Long) -> Unit,
) {
    val context = LocalContext.current
    val videoUri = remember(imported.absolutePath) { Uri.fromFile(File(imported.absolutePath)) }
    val durationMs = remember(imported.durationSec) { swingAnalysisTrimDurationMs(imported.durationSec) }
    var startMs by remember(imported.absolutePath) { mutableLongStateOf(0L) }
    var endMs by remember(imported.absolutePath) { mutableLongStateOf(durationMs) }
    var isPlaying by remember { mutableStateOf(true) }
    val canTrim = durationMs > MIN_VIDEO_TRIM_DURATION_MS
    val selectedRange = normalizedVideoTrimRange(durationMs, startMs, endMs)
        ?: return

    val player = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            playWhenReady = true
            prepare()
        }
    }

    V2BackHandler(enabled = !saving) {
        onCancel()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, startMs, endMs) {
        player.seekTo(startMs)
        while (true) {
            val position = player.currentPosition
            if (position < startMs || position >= endMs) {
                player.seekTo(startMs)
            }
            isPlaying = player.isPlaying
            delay(80)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .v2FullScreenInsets(),
    ) {
        Column(Modifier.fillMaxSize()) {
            TrimHeader(
                saving = saving,
                onCancel = onCancel,
                onUse = { onUse(selectedRange.startMs, selectedRange.endMs) },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .background(Color.Black),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    update = { it.player = player },
                )
            }
            TrimControls(
                durationMs = durationMs,
                startMs = selectedRange.startMs,
                endMs = selectedRange.endMs,
                canTrim = canTrim,
                isPlaying = isPlaying,
                saving = saving,
                error = error,
                onStartChange = { next ->
                    normalizedVideoTrimRange(durationMs, next, endMs)?.let { safe ->
                        startMs = safe.startMs
                        endMs = safe.endMs
                        player.seekTo(safe.startMs)
                    }
                },
                onEndChange = { next ->
                    normalizedVideoTrimRange(durationMs, startMs, next)?.let { safe ->
                        startMs = safe.startMs
                        endMs = safe.endMs
                        player.seekTo(safe.startMs)
                    }
                },
                onTogglePlay = {
                    if (player.isPlaying) player.pause() else player.play()
                },
                onUseWhole = { onUse(0L, durationMs) },
            )
        }
        if (saving) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.46f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = DSColors.fairway)
            }
        }
    }
}

@Composable
private fun TrimHeader(
    saving: Boolean,
    onCancel: () -> Unit,
    onUse: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onCancel, enabled = !saving) {
            Text("重新选择", color = Color.White.copy(alpha = if (saving) 0.38f else 0.88f))
        }
        Spacer(Modifier.weight(1f))
        Text("修剪视频", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onUse,
            enabled = !saving,
            colors = ButtonDefaults.buttonColors(
                containerColor = DSColors.fairway,
                disabledContainerColor = Color.White.copy(alpha = 0.16f),
            ),
            shape = RoundedCornerShape(22.dp),
        ) {
            Text("使用", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TrimControls(
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    canTrim: Boolean,
    isPlaying: Boolean,
    saving: Boolean,
    error: String?,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onUseWhole: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "已选取 ${formatSwingAnalysisTrimTime(endMs - startMs)} / 共 ${formatSwingAnalysisTrimTime(durationMs)}",
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        if (canTrim) {
            TrimSliderRow(
                label = "起点",
                valueMs = startMs,
                valueRange = 0f..(endMs - MIN_VIDEO_TRIM_DURATION_MS).coerceAtLeast(0L).toFloat(),
                enabled = !saving,
                onChange = onStartChange,
            )
            TrimSliderRow(
                label = "终点",
                valueMs = endMs,
                valueRange = (startMs + MIN_VIDEO_TRIM_DURATION_MS).coerceAtMost(durationMs).toFloat()..durationMs.toFloat(),
                enabled = !saving,
                onChange = onEndChange,
            )
        }
        error?.let {
            Text(
                text = it,
                color = DSColors.flagRed,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onTogglePlay,
                enabled = !saving,
                modifier = Modifier.height(40.dp),
            ) {
                Text(if (isPlaying) "暂停" else "播放", color = Color.White.copy(alpha = 0.86f))
            }
            if (canTrim) {
                TextButton(
                    onClick = onUseWhole,
                    enabled = !saving,
                    modifier = Modifier.height(40.dp),
                ) {
                    Text("使用整段", color = Color.White.copy(alpha = 0.66f))
                }
            }
        }
    }
}

@Composable
private fun TrimSliderRow(
    label: String,
    valueMs: Long,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onChange: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White.copy(alpha = 0.66f), fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(
                formatSwingAnalysisTrimTime(valueMs),
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Slider(
            value = valueMs.toFloat().coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = { onChange(it.roundToLong()) },
            valueRange = valueRange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = DSColors.fairway,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f),
                disabledThumbColor = Color.White.copy(alpha = 0.38f),
                disabledActiveTrackColor = DSColors.fairway.copy(alpha = 0.38f),
            ),
        )
    }
}

private fun formatSwingAnalysisTrimTime(ms: Long): String {
    val totalTenths = (ms.coerceAtLeast(0L) / 100L)
    val minutes = totalTenths / 600L
    val seconds = (totalTenths % 600L) / 10L
    val tenths = totalTenths % 10L
    return String.format(Locale.US, "%d:%02d.%d", minutes, seconds, tenths)
}

internal fun swingAnalysisTrimDurationMs(durationSec: Double): Long =
    importedVideoDurationMs(durationSec) ?: 1L
