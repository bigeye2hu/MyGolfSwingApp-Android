package com.mygolfswing.app.ui.interaction

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.mygolfswing.app.data.friend.V2IMMessage
import com.mygolfswing.app.data.friend.V2IMMessageKind
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.designsystem.DSColors
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay

private object V2ChatVoicePlaybackCoordinator {
    var currentMessageId by mutableStateOf<String?>(null)
}

@Composable
internal fun V2InteractionChatMessageBubble(
    message: V2IMMessage,
    onOpenMedia: (V2IMMessage) -> Unit,
    onLongPress: () -> Unit = {},
    onTap: (() -> Unit)? = null,
    reciprocalBackground: Color? = null,
    fileDownloadProgress: Int? = null,
    onDownloadFile: (() -> Unit)? = null,
) {
    val foreground = if (message.isMine) Color.White else DSColors.deepGreen
    val background = if (message.isMine) DSColors.fairway else reciprocalBackground ?: Color.White.copy(alpha = 0.96f)
    val bubble = Modifier.widthIn(max = 286.dp).clip(chatBubbleShape(message.isMine)).background(background)
    when (message.kind) {
        V2IMMessageKind.SYSTEM -> Text(
            message.text,
            color = DSColors.shadowGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
        V2IMMessageKind.TEXT -> V2ChatEmojiText(
            text = message.text,
            color = foreground,
            fontSize = 16.sp,
            modifier = bubble.chatMessageGesture(message, onOpenMedia, onLongPress, onTap)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
        V2IMMessageKind.IMAGE -> Box(
            bubble.size(width = 210.dp, height = 164.dp).semantics { contentDescription = "图片消息" }
                .chatMessageGesture(message, onOpenMedia, onLongPress, onTap),
            contentAlignment = Alignment.Center,
        ) {
            val model = chatMediaModel(message.thumbnailUrl ?: message.mediaUrl)
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = "图片消息",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Filled.Photo, contentDescription = null, tint = foreground, modifier = Modifier.size(38.dp))
            }
        }
        V2IMMessageKind.VIDEO -> Box(
            bubble.size(width = 210.dp, height = 142.dp).chatMessageGesture(message, onOpenMedia, onLongPress, onTap),
            contentAlignment = Alignment.Center,
        ) {
            val model = chatMediaModel(message.thumbnailUrl)
            if (model != null) {
                AsyncImage(model, "视频消息", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))
            }
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.48f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "播放视频", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            message.durationSeconds?.let {
                Text(formatDuration(it), color = Color.White, fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                        .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        V2IMMessageKind.VOICE -> V2ChatVoiceMessageBubble(
            message = message,
            foreground = foreground,
            modifier = bubble.chatMessageGesture(message, onOpenMedia, onLongPress, onTap),
        )
        V2IMMessageKind.FILE -> V2ChatFileMessageBubble(
            message = message,
            background = background,
            reciprocalBackground = reciprocalBackground,
            downloadProgress = fileDownloadProgress,
            onDownload = onTap ?: onDownloadFile ?: { onOpenMedia(message) },
            modifier = Modifier.chatMessageGesture(message, onOpenMedia, onLongPress, onTap),
        )
        V2IMMessageKind.MERGED -> Column(
            modifier = Modifier.widthIn(min = 230.dp, max = 250.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF2F3F5))
                .testTag("chat-merged-message-${message.messageId}")
                .chatMessageGesture(message, onOpenMedia, onLongPress, onTap),
        ) {
            Text(
                text = message.mergedTitle ?: "聊天记录",
                color = Color(0xFF1C1C1E),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                message.mergedAbstracts.take(4).forEach { abstract ->
                    Text(
                        text = localizeChatEmojiCodes(abstract),
                        color = Color(0xFF666666),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    .heightIn(min = 1.dp, max = 1.dp).background(Color.Black.copy(alpha = 0.08f)),
            )
            Text(
                text = if (message.mergedLayersOverLimit) "聊天记录·无法继续展开" else "聊天记录",
                color = Color(0xFF888888),
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        V2IMMessageKind.UNSUPPORTED -> Text(
            "[暂不支持的消息]",
            color = foreground.copy(alpha = 0.72f),
            fontSize = 13.sp,
            modifier = bubble.chatMessageGesture(message, onOpenMedia, onLongPress, onTap)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

internal enum class V2ChatFileIconKind { DOCUMENT, TABLE, PRESENTATION, ARCHIVE, TEXT, IMAGE, AUDIO, VIDEO }

internal fun chatFileIconKind(fileName: String?): V2ChatFileIconKind = when (
    fileName.orEmpty().substringAfterLast('.', "").lowercase(Locale.US)
) {
    "xls", "xlsx" -> V2ChatFileIconKind.TABLE
    "ppt", "pptx" -> V2ChatFileIconKind.PRESENTATION
    "zip", "rar", "7z" -> V2ChatFileIconKind.ARCHIVE
    "txt" -> V2ChatFileIconKind.TEXT
    "jpg", "jpeg", "png", "gif", "webp" -> V2ChatFileIconKind.IMAGE
    "mp3", "wav", "aac", "flac" -> V2ChatFileIconKind.AUDIO
    "mp4", "mov", "avi", "mkv" -> V2ChatFileIconKind.VIDEO
    else -> V2ChatFileIconKind.DOCUMENT
}

internal fun isLocalChatFileSource(source: String?): Boolean {
    val value = source?.trim().orEmpty()
    return value.startsWith("/") || value.startsWith("file:") || value.startsWith("content:")
}

@Composable
private fun V2ChatFileMessageBubble(
    message: V2IMMessage,
    background: Color,
    reciprocalBackground: Color?,
    downloadProgress: Int?,
    onDownload: () -> Unit,
    modifier: Modifier,
) {
    val needsDownload = !isLocalChatFileSource(message.mediaUrl)
    val downloadControl: @Composable () -> Unit = {
        Box(
            modifier = Modifier.size(32.dp).then(
                if (downloadProgress == null) Modifier.clickable(onClick = onDownload) else Modifier,
            ).testTag("chat-file-download-${message.messageId}"),
            contentAlignment = Alignment.Center,
        ) {
            if (downloadProgress != null) {
                CircularProgressIndicator(
                    progress = { downloadProgress.coerceIn(0, 100) / 100f },
                    color = DSColors.fairway,
                    trackColor = DSColors.mistGreen,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp).semantics {
                        contentDescription = "文件下载中 ${downloadProgress.coerceIn(0, 100)}%"
                    },
                )
            } else {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = "下载文件",
                    tint = DSColors.fairway,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
    val icon = when (chatFileIconKind(message.fileName)) {
        V2ChatFileIconKind.DOCUMENT -> Icons.AutoMirrored.Filled.InsertDriveFile
        V2ChatFileIconKind.TABLE -> Icons.Filled.TableChart
        V2ChatFileIconKind.PRESENTATION -> Icons.Filled.BarChart
        V2ChatFileIconKind.ARCHIVE -> Icons.Filled.Archive
        V2ChatFileIconKind.TEXT -> Icons.Filled.Description
        V2ChatFileIconKind.IMAGE -> Icons.Filled.Photo
        V2ChatFileIconKind.AUDIO -> Icons.Filled.MusicNote
        V2ChatFileIconKind.VIDEO -> Icons.Filled.Movie
    }
    val card = @Composable {
        Row(
            modifier = Modifier.widthIn(max = 250.dp)
                .clip(chatBubbleShape(message.isMine))
                .background(if (message.isMine) background else reciprocalBackground ?: Color.White.copy(alpha = 0.96f))
                .then(modifier)
                .padding(12.dp)
                .testTag("chat-file-bubble-${message.messageId}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (message.isMine) DSColors.fairway else DSColors.inkGreen,
                    modifier = Modifier.size(30.dp),
                )
            }
            Column(Modifier.widthIn(max = 190.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    message.fileName ?: "未知文件",
                    color = DSColors.inkGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatBytes(message.fileSizeBytes ?: 0L),
                    color = DSColors.shadowGray,
                    fontSize = 12.sp,
                )
            }
        }
    }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (message.isMine && needsDownload) downloadControl()
        card()
        if (!message.isMine && needsDownload) downloadControl()
        if (!needsDownload) Spacer(Modifier.size(0.dp))
    }
}

internal fun chatBubbleShape(isMine: Boolean): RoundedCornerShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomEnd = if (isMine) 0.dp else 18.dp,
    bottomStart = if (isMine) 18.dp else 0.dp,
)

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.chatMessageGesture(
    message: V2IMMessage,
    onOpenMedia: (V2IMMessage) -> Unit,
    onLongPress: () -> Unit,
    onTap: (() -> Unit)?,
): Modifier = combinedClickable(
    onClick = {
        if (onTap != null) {
            onTap()
        } else if (
            message.kind in setOf(V2IMMessageKind.IMAGE, V2IMMessageKind.VIDEO, V2IMMessageKind.MERGED) ||
            (message.kind == V2IMMessageKind.FILE && isLocalChatFileSource(message.mediaUrl))
        ) {
            onOpenMedia(message)
        }
    },
    onLongClick = onLongPress,
)

internal fun chatVoicePlaybackDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(Locale.US, safe / 60, safe % 60)
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun V2ChatVoiceMessageBubble(
    message: V2IMMessage,
    foreground: Color,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val mediaUri = remember(message.mediaUrl) { message.mediaUrl?.let(::chatMediaUri) }
    val player = remember(message.messageId, mediaUri) {
        ExoPlayer.Builder(context).build().apply {
            if (mediaUri != null) {
                setMediaItem(MediaItem.fromUri(mediaUri))
                prepare()
            }
        }
    }
    var isPlaying by remember(message.messageId) { mutableStateOf(false) }
    var elapsedMillis by remember(message.messageId) { mutableLongStateOf(0L) }
    val currentMessageId = V2ChatVoicePlaybackCoordinator.currentMessageId
    val fullDuration = message.durationSeconds?.coerceAtLeast(0) ?: 0
    val displaySeconds = if (isPlaying) (elapsedMillis / 1_000L).toInt() else fullDuration

    DisposableEffect(player, message.messageId) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                    elapsedMillis = 0L
                    player.seekTo(0L)
                    if (V2ChatVoicePlaybackCoordinator.currentMessageId == message.messageId) {
                        V2ChatVoicePlaybackCoordinator.currentMessageId = null
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose {
            if (V2ChatVoicePlaybackCoordinator.currentMessageId == message.messageId) {
                V2ChatVoicePlaybackCoordinator.currentMessageId = null
            }
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(currentMessageId, message.messageId) {
        if (currentMessageId != message.messageId && player.isPlaying) player.pause()
    }
    LaunchedEffect(isPlaying, player) {
        while (isPlaying) {
            elapsedMillis = player.currentPosition.coerceAtLeast(0L)
            delay(100L)
        }
    }

    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("chat-voice-bubble-${message.messageId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(32.dp).clickable(enabled = mediaUri != null) {
                if (isPlaying) {
                    player.pause()
                    V2ChatVoicePlaybackCoordinator.currentMessageId = null
                } else {
                    V2ChatVoicePlaybackCoordinator.currentMessageId = message.messageId
                    if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
                    player.play()
                }
            }.testTag("chat-voice-play-${message.messageId}"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "暂停语音" else "播放语音",
                tint = foreground,
                modifier = Modifier.size(20.dp),
            )
        }
        V2ChatVoiceWaveform(isPlaying = isPlaying, color = foreground)
        Text(
            chatVoicePlaybackDuration(displaySeconds),
            color = foreground.copy(alpha = 0.74f),
            fontSize = 12.sp,
            modifier = Modifier.testTag("chat-voice-duration-${message.messageId}"),
        )
    }
}

@Composable
private fun V2ChatVoiceWaveform(isPlaying: Boolean, color: Color) {
    val heights = listOf(8, 16, 12, 20, 10, 14, 17, 8)
    val transition = rememberInfiniteTransition(label = "chat-voice-waveform")
    Row(
        modifier = Modifier.heightIn(min = 24.dp).testTag("chat-voice-waveform"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        heights.forEachIndexed { index, height ->
            val animatedScale by transition.animateFloat(
                initialValue = 0.35f + (index % 3) * 0.12f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400 + index * 70, delayMillis = index * 30),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "chat-voice-wave-$index",
            )
            Box(
                Modifier.size(width = 2.dp, height = (height * if (isPlaying) animatedScale else 1f).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color.copy(alpha = if (isPlaying) 0.82f else 0.60f)),
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun V2InteractionChatMediaPreview(
    message: V2IMMessage,
    onClose: () -> Unit,
) {
    V2BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        when (message.kind) {
            V2IMMessageKind.SYSTEM -> Unit
            V2IMMessageKind.IMAGE -> AsyncImage(
                model = chatMediaModel(message.mediaUrl ?: message.thumbnailUrl),
                contentDescription = "查看图片",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            V2IMMessageKind.VIDEO, V2IMMessageKind.VOICE -> ChatMediaPlayer(message.mediaUrl)
            else -> Unit
        }
        Icon(
            Icons.Filled.Close,
            contentDescription = "关闭预览",
            tint = Color.White,
            modifier = Modifier.align(Alignment.TopEnd).padding(18.dp).size(44.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onClose).padding(10.dp),
        )
        if (message.kind == V2IMMessageKind.VOICE) {
            Text(
                "语音消息 · ${message.durationSeconds ?: 0}秒",
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun ChatMediaPlayer(source: String?) {
    val context = LocalContext.current
    val mediaUri = remember(source) { source?.let(::chatMediaUri) }
    val player = remember(mediaUri) {
        ExoPlayer.Builder(context).build().apply {
            if (mediaUri != null) setMediaItem(MediaItem.fromUri(mediaUri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        factory = {
            PlayerView(it).apply {
                this.player = player
                useController = true
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
        },
        modifier = Modifier.fillMaxSize().heightIn(min = 240.dp),
    )
}

private fun chatMediaModel(value: String?): Any? = value?.takeIf(String::isNotBlank)?.let {
    if (it.startsWith("/")) File(it) else it
}

private fun chatMediaUri(value: String): Uri = if (value.startsWith("/")) Uri.fromFile(File(value)) else value.toUri()

private fun formatDuration(seconds: Int): String = "%d:%02d".format(Locale.US, seconds / 60, seconds % 60)

internal fun formatBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    return when {
        safe >= 1_000_000_000L -> formatAdaptiveFileUnit(safe / 1_000_000_000.0, "GB")
        safe >= 1_000_000L -> formatAdaptiveFileUnit(safe / 1_000_000.0, "MB")
        safe >= 1_000L -> "${((safe + 500L) / 1_000L)} KB"
        else -> "$safe B"
    }
}

private fun formatAdaptiveFileUnit(value: Double, suffix: String): String {
    val number = if (value < 100.0) "%.1f".format(Locale.US, value).removeSuffix(".0")
    else "%.0f".format(Locale.US, value)
    return "$number $suffix"
}
