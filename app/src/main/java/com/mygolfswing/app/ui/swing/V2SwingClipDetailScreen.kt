package com.mygolfswing.app.ui.swing

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.data.model.V2SwingClip
import com.mygolfswing.app.data.model.V2SwingClipRetention
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun V2SwingClipDetailScreen(
    clipId: UUID,
    onBack: () -> Unit,
) {
    val sessions by V2SwingSessionStore.sessions.collectAsState()
    val clip = sessions.flatMap { it.clips }.firstOrNull { it.id == clipId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(true) }
    var currentTimeSec by remember { mutableFloatStateOf(0f) }
    var durationSec by remember { mutableFloatStateOf(0f) }
    var playbackRate by remember { mutableFloatStateOf(1f) }
    var showDetails by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    if (clip == null) {
        V2MissingEntityScreen(message = "挥杆视频不存在", onBack = onBack, title = "挥杆详情")
        return
    }

    val uri = remember(clip.videoPath) { mediaUriFor(clip.videoPath) }
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    player.pause()
                    player.seekTo(0)
                    isPlaying = false
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(player) {
        while (true) {
            currentTimeSec = (player.currentPosition / 1000f).coerceAtLeast(0f)
            val dur = player.duration
            if (dur > 0) durationSec = dur / 1000f
            isPlaying = player.isPlaying
            delay(80)
        }
    }
    LaunchedEffect(playbackRate) {
        player.playbackParameters = PlaybackParameters(playbackRate)
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(1_800)
            toast = null
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize()) {
            DetailTopBar(onBack = onBack)
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                    }
                },
                update = { it.player = player },
            )
            ControlsPanel(
                clip = clip,
                isPlaying = isPlaying,
                currentTime = currentTimeSec,
                duration = durationSec,
                playbackRate = playbackRate,
                showDetails = showDetails,
                onTogglePlay = {
                    if (player.isPlaying) player.pause() else player.play()
                },
                onStep = { deltaMs ->
                    player.pause()
                    player.seekTo((player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L)))
                },
                onSeek = { progress ->
                    val target = (durationSec * progress * 1000).roundToInt().toLong()
                    player.seekTo(target)
                },
                onRate = { playbackRate = it },
                onFavorite = {
                    val next = if (clip.retention == V2SwingClipRetention.FAVORITED) {
                        V2SwingClipRetention.TEMPORARY
                    } else {
                        V2SwingClipRetention.FAVORITED
                    }
                    V2SwingSessionStore.updateClipRetention(clip.id, next)
                },
                onPromote = {
                    val next = if (clip.retention == V2SwingClipRetention.PROMOTED) {
                        V2SwingClipRetention.FAVORITED
                    } else {
                        V2SwingClipRetention.PROMOTED
                    }
                    V2SwingSessionStore.updateClipRetention(clip.id, next)
                },
                onSave = {
                    scope.launch {
                        val ok = saveClipToGallery(context, clip)
                        toast = if (ok) "已保存到相册" else "保存失败"
                    }
                },
                onInfo = { showDetails = !showDetails },
                onDelete = { showDeleteConfirm = true },
            )
        }
        toast?.let {
            Text(
                it,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 76.dp)
                    .clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除这段挥杆？") },
            text = { Text("将删除视频文件和缩略图，此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    V2SwingSessionStore.deleteClips(setOf(clip.id))
                    showDeleteConfirm = false
                    onBack()
                }) { Text("删除", color = DSColors.flagRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
        }
        Text("挥杆详情", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun ControlsPanel(
    clip: V2SwingClip,
    isPlaying: Boolean,
    currentTime: Float,
    duration: Float,
    playbackRate: Float,
    showDetails: Boolean,
    onTogglePlay: () -> Unit,
    onStep: (Long) -> Unit,
    onSeek: (Float) -> Unit,
    onRate: (Float) -> Unit,
    onFavorite: () -> Unit,
    onPromote: () -> Unit,
    onSave: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(Color.Black).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${"%.1f".format(currentTime)}s", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text("${"%.1f".format(duration)}s", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
        Slider(
            value = if (duration > 0) (currentTime / duration).coerceIn(0f, 1f) else 0f,
            onValueChange = onSeek,
        )
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StepButton("-10") { onStep(-10_000) }
            StepButton("-3") { onStep(-3_000) }
            StepButton("-1") { onStep(-1_000) }
            IconButton(onClick = onTogglePlay, modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.White)) {
                Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(30.dp))
            }
            StepButton("+1") { onStep(1_000) }
            StepButton("+3") { onStep(3_000) }
            StepButton("+10") { onStep(10_000) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(0.1f, 0.25f, 0.5f, 1f).forEach { rate ->
                Text(
                    "${rate}x".replace(".0", ""),
                    color = if (playbackRate == rate) Color.Black else Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                        .background(if (playbackRate == rate) Color.White else Color.White.copy(alpha = 0.12f))
                        .clickable { onRate(rate) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BottomAction(
                icon = if (clip.retention == V2SwingClipRetention.FAVORITED) Icons.Filled.Star else Icons.Filled.StarBorder,
                label = "收藏",
                active = clip.retention == V2SwingClipRetention.FAVORITED,
                onClick = onFavorite,
            )
            BottomAction(Icons.Filled.Upgrade, "成长池", clip.retention == V2SwingClipRetention.PROMOTED, onPromote)
            Spacer(Modifier.weight(1f))
            BottomAction(Icons.Filled.Download, "保存", false, onSave)
            BottomAction(Icons.Filled.Info, "详情", showDetails, onInfo)
            BottomAction(Icons.Filled.DeleteOutline, "删除", false, onDelete, danger = true)
        }
        if (showDetails) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.08f)).padding(12.dp)) {
                DetailRow("时间", clip.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA)))
                DetailRow("时长", durationShort(clip.durationSec))
                DetailRow("状态", when (clip.retention) {
                    V2SwingClipRetention.TEMPORARY -> "普通（14天后自动清理）"
                    V2SwingClipRetention.FAVORITED -> "收藏（永久保留）"
                    V2SwingClipRetention.PROMOTED -> "成长池（永久保留）"
                })
            }
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onClick).padding(8.dp))
}

@Composable
private fun BottomAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 5.dp, vertical = 4.dp),
    ) {
        val color = when {
            danger -> DSColors.flagRed
            active -> DSColors.trophyGold
            else -> Color.White.copy(alpha = 0.82f)
        }
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(label, color = color, fontSize = 10.sp)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = Color.White.copy(alpha = 0.86f), fontSize = 12.sp)
    }
}

private suspend fun saveClipToGallery(context: android.content.Context, clip: V2SwingClip): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val src = File(clip.videoPath.removePrefix("file://"))
        if (!src.exists()) return@withContext false
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "just_swing_${clip.id}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/JustSwing")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
        resolver.openOutputStream(uri)?.use { out ->
            src.inputStream().use { input -> input.copyTo(out) }
        } ?: return@withContext false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    }.getOrDefault(false)
}

private fun mediaUriFor(pathOrUrl: String): Uri {
    if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) return pathOrUrl.toUri()
    return Uri.fromFile(File(pathOrUrl.removePrefix("file://")))
}

private fun durationShort(sec: Double): String {
    val t = sec.toInt().coerceAtLeast(0)
    return if (t < 60) "${t}s" else "%d:%02d".format(t / 60, t % 60)
}
