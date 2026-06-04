package com.mygolfswing.app.ui.swing

import android.net.Uri
import androidx.annotation.OptIn
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.model.V2SwingClip
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.common.V2NavTopBar
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** 单日挥杆详情：按天聚合片段，并支持点击缩略图切换播放。 */
@OptIn(UnstableApi::class)
@Composable
fun V2SwingDayDetailScreen(
    day: LocalDate,
    onBack: () -> Unit,
) {
    val sessions by V2SwingSessionStore.sessions.collectAsState()
    val clips = remember(sessions, day) { V2SwingSessionStore.clipsForDay(day) }
    var selectedClipId by remember(day) { mutableStateOf<UUID?>(clips.firstOrNull()?.id) }

    LaunchedEffect(clips) {
        if (clips.none { it.id == selectedClipId }) {
            selectedClipId = clips.firstOrNull()?.id
        }
    }

    val selected = clips.firstOrNull { it.id == selectedClipId }
    if (clips.isEmpty() || selected == null) {
        V2MissingEntityScreen(message = "当天没有挥杆片段", onBack = onBack, title = "挥杆记录")
        return
    }

    val uri = remember(selected.videoPath) { mediaUriFor(selected.videoPath) }
    if (uri == null) {
        V2MissingEntityScreen(message = "本地挥杆视频文件不存在", onBack = onBack, title = "挥杆记录")
        return
    }

    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Column(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        V2NavTopBar(title = day.format(DateTimeFormatter.ofPattern("M月d日 挥杆", Locale.CHINA)), onBack = onBack)

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color.Black),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            update = { it.player = player },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DSSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "${clips.size} 个挥杆片段",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DSColors.inkGreen,
            )
            Text(
                text = selected.timestamp.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)) +
                    durationLabel(selected.durationSec),
                fontSize = 12.sp,
                color = DSColors.shadowGray,
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = DSSpacing.lg),
        ) {
            items(clips, key = { it.id }) { clip ->
                ClipThumb(
                    clip = clip,
                    selected = clip.id == selected.id,
                    onTap = { selectedClipId = clip.id },
                )
            }
        }
    }
}

@Composable
private fun ClipThumb(
    clip: V2SwingClip,
    selected: Boolean,
    onTap: () -> Unit,
) {
    val borderColor = if (selected) DSColors.fairway else DSColors.borderLine
    val previewModel = remember(clip.thumbnailPath, clip.videoPath) {
        swingClipPreviewModel(clip.thumbnailPath, clip.videoPath)
    }
    Box(
        modifier = Modifier
            .size(width = 110.dp, height = 76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DSColors.deepGreen)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onTap),
    ) {
        if (previewModel != null) {
            AsyncImage(
                model = previewModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(26.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
        } else {
            MissingSwingMediaPlaceholder(Modifier.matchParentSize())
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                clip.timestamp.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(Modifier.weight(1f))
            Text(durationShort(clip.durationSec), fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun MissingSwingMediaPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(DSColors.deepGreen),
        contentAlignment = Alignment.Center,
    ) {
        Text("文件缺失", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.72f))
    }
}

private fun mediaUriFor(pathOrUrl: String): Uri? {
    if (V2MemoBridge.isRemoteUrl(pathOrUrl)) return pathOrUrl.trim().toUri()
    val path = V2MemoBridge.localVideoAbsolutePath(pathOrUrl) ?: return null
    val file = File(path)
    return if (file.exists() && file.isFile) Uri.fromFile(file) else null
}

private fun durationLabel(sec: Double): String =
    if (sec > 0) " · ${durationShort(sec)}" else ""

private fun durationShort(sec: Double): String {
    val t = sec.toInt().coerceAtLeast(0)
    return "%d:%02d".format(t / 60, t % 60)
}
