package com.mygolfswing.app.ui.interaction

import android.media.MediaPlayer
import android.content.Intent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset
import com.mygolfswing.app.data.coach.V2CoachWorkspaceItem
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.common.copyPlainTextToClipboard
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val ContentPanel = Color(0xFFFCFBF7)
internal val ContentText = Color(0xFF173C30)
internal val ContentSecondary = Color(0xFF66766F)
internal val ContentSoft = Color(0xFFEBF0E7)
internal val ContentInput = Color(0xFFF9F6F0)
internal val ContentControl = Color(0xFFEEF3EF)
internal val ContentDivider = Color(0xFFE4E8E1)
internal val ContentDisabled = Color(0xFFBCCDC2)
internal val ContentHandle = Color(0xFFC6CCC6)
internal val ContentMediaMatte = Color(0xFF17261F)
internal val ContentDiscussionWell = Color(0xFFEDEEEC)
internal val ContentSectionLine = Color(0xFFDCDCDC)

private fun contentTextStyle(size: Int, line: Int, weight: FontWeight, color: Color) = TextStyle(
    color = color,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

internal enum class TeachingMediaKind { VOICE, IMAGE, VIDEO, MATERIAL, UNKNOWN }

internal fun teachingMediaKind(asset: V2CoachWorkspaceAsset): TeachingMediaKind {
    val kind = asset.sourceKind.lowercase()
    val url = asset.videoUrl?.substringBefore('?')?.lowercase().orEmpty()
    val audio = url.endsWith(".m4a") || url.endsWith(".mp3") || url.endsWith(".aac") ||
        url.endsWith(".wav") || url.endsWith(".ogg")
    val image = kind in setOf("image", "photo", "picture") ||
        listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".heic").any(url::endsWith)
    return when {
        asset.isVoice || audio -> TeachingMediaKind.VOICE
        image -> TeachingMediaKind.IMAGE
        asset.savedToLibrary && kind !in setOf("analysis_recording", "coach_recording", "voice_recording") ->
            TeachingMediaKind.MATERIAL
        url.isNotBlank() || !asset.coverUrl.isNullOrBlank() || asset.durationSeconds != null -> TeachingMediaKind.VIDEO
        else -> TeachingMediaKind.UNKNOWN
    }
}

internal fun teachingItemIsAudioOnly(item: V2CoachWorkspaceItem): Boolean {
    val assets = listOfNotNull(item.primaryAsset) + item.attachments
    if (assets.isNotEmpty()) return assets.all { teachingMediaKind(it) == TeachingMediaKind.VOICE }
    return teachingUrlIsAudio(item.videoUrl)
}

internal fun teachingContentTime(raw: String?): String {
    val instant = workspaceThreadInstant(raw) ?: return ""
    val date = instant.atZone(ZoneId.systemDefault())
    val pattern = if (date.year == java.time.Year.now(ZoneId.systemDefault()).value) {
        "M月d日 HH:mm"
    } else {
        "yyyy年M月d日 HH:mm"
    }
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.CHINA))
}

internal fun teachingClock(seconds: Double?): String? {
    val total = seconds?.takeIf { it.isFinite() && it >= 0.0 }?.toInt() ?: return null
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

internal fun teachingQuotePreview(item: V2CoachWorkspaceItem?): String {
    if (item == null) return "原内容已不可用"
    V2CoachWorkspaceItem.visibleCopy(item.text)?.let { return it }
    val asset = item.primaryAsset
    return when (asset?.let(::teachingMediaKind)) {
        TeachingMediaKind.VOICE -> "[语音]" + (teachingClock(asset.durationSeconds)?.let { " $it" } ?: "")
        TeachingMediaKind.VIDEO -> "[视频]"
        TeachingMediaKind.IMAGE -> "[图片]"
        TeachingMediaKind.MATERIAL -> asset.title.ifBlank { "引用素材" }
        else -> if (teachingUrlIsAudio(item.videoUrl)) {
            "[语音]" + (teachingClock(item.durationSeconds)?.let { " $it" } ?: "")
        } else if (item.videoUrl != null || item.coverUrl != null) {
            "[视频]"
        } else {
            "查看被回复的消息"
        }
    }
}

internal class TeachingVoicePlayer {
    var activeUrl by mutableStateOf<String?>(null)
        private set
    private var player: MediaPlayer? = null

    fun toggle(url: String) {
        if (url.isBlank()) return
        if (activeUrl == url) {
            release()
            return
        }
        release()
        val created = MediaPlayer()
        player = created
        runCatching {
            created.setDataSource(url)
            created.setOnPreparedListener {
                if (player !== created) return@setOnPreparedListener
                it.start()
                activeUrl = url
            }
            created.setOnCompletionListener { if (player === created) release() }
            created.setOnErrorListener { _, _, _ ->
                if (player === created) release()
                true
            }
            created.prepareAsync()
        }.onFailure { release() }
    }

    fun release() {
        activeUrl = null
        player?.runCatching {
            setOnPreparedListener(null)
            setOnCompletionListener(null)
            setOnErrorListener(null)
            reset()
            release()
        }
        player = null
    }
}

@Composable
internal fun TeachingContentMessage(
    item: V2CoachWorkspaceItem,
    senderName: String,
    origin: Boolean = false,
    voicePlayer: TeachingVoicePlayer,
    referencedItem: V2CoachWorkspaceItem? = null,
    onQuote: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onOpenReference: (() -> Unit)? = null,
    onSaveAsset: ((V2CoachWorkspaceAsset) -> Unit)? = null,
    savingAssetIds: Set<String> = emptySet(),
    onPlayAsset: ((V2CoachWorkspaceAsset) -> Unit)? = null,
    bringIntoView: Boolean = false,
    onBroughtIntoView: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val playWorkspaceVideo = LocalPlayWorkspaceVideo.current
    val preparingPlaybackId = LocalPreparingWorkspacePlaybackId.current
    var menuExpanded by remember(item.itemId) { mutableStateOf(false) }
    val bringIntoViewRequester = remember(item.itemId) { BringIntoViewRequester() }
    androidx.compose.runtime.LaunchedEffect(bringIntoView) {
        if (bringIntoView) {
            kotlinx.coroutines.delay(50)
            bringIntoViewRequester.bringIntoView()
            onBroughtIntoView()
        }
    }
    val visibleText = V2CoachWorkspaceItem.visibleCopy(item.text).orEmpty()
    val assets = buildList {
        item.primaryAsset?.let(::add)
        item.attachments.filter { it.assetId != item.primaryAsset?.assetId }.forEach(::add)
    }
    Row(
        modifier.fillMaxWidth().bringIntoViewRequester(bringIntoViewRequester),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        V2UserAvatar(
            avatar = item.sender?.avatar,
            modifier = Modifier.size(34.dp),
            fallbackText = senderName,
            fallbackFontSize = 12.sp,
            placeholderIconSize = 18.dp,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(end = 108.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        senderName,
                        style = contentTextStyle(14, 20, FontWeight.Medium, ContentText),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (origin) {
                        Text(
                            "原文",
                            style = contentTextStyle(12, 18, FontWeight.Normal, ContentSecondary),
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ContentSoft)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                Text(
                    teachingContentTime(item.createdAt),
                    style = contentTextStyle(12, 18, FontWeight.Normal, ContentSecondary),
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                )
            }
            Box {
                Column(
                    Modifier.fillMaxWidth().pointerInput(item.itemId) {
                        detectTapGestures(onLongPress = { menuExpanded = true })
                    },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (item.replyToItemId != null) {
                        TeachingQuoteBar(
                            name = referencedItem?.sender?.nickname?.trim()?.takeIf(String::isNotEmpty)
                                ?: if (referencedItem?.senderRole == "coach") "教练" else "对方",
                            preview = teachingQuotePreview(referencedItem),
                            onOpen = onOpenReference,
                        )
                    }
                    if (item.isRemovedContent) {
                        Text("内容已删除", style = contentTextStyle(16, 24, FontWeight.Normal, ContentSecondary))
                    } else if (visibleText.isNotEmpty()) {
                        Text(visibleText, style = contentTextStyle(16, 24, FontWeight.Normal, ContentText))
                    }
                    if (!origin && !item.isRemovedContent && !teachingItemIsAudioOnly(item) && assets.isEmpty() &&
                        (item.videoUrl != null || item.coverUrl != null)
                    ) {
                        TeachingMediaPreview(
                            coverUrl = item.coverUrl,
                            width = item.width,
                            height = item.height,
                            duration = item.durationSeconds,
                            preparing = preparingPlaybackId == item.itemId,
                            video = !teachingUrlIsAudio(item.videoUrl),
                            onClick = { playWorkspaceVideo(item.itemId, item.videoUrl) },
                        )
                    }
                    if (!origin) assets.forEach { asset ->
                        when (teachingMediaKind(asset)) {
                            TeachingMediaKind.VOICE -> TeachingVoiceBar(
                                duration = asset.durationSeconds,
                                playing = voicePlayer.activeUrl == asset.videoUrl && !asset.videoUrl.isNullOrBlank(),
                                onToggle = { asset.videoUrl?.let(voicePlayer::toggle) },
                            )
                            TeachingMediaKind.IMAGE -> TeachingMediaPreview(
                                coverUrl = asset.coverUrl ?: asset.videoUrl,
                                width = asset.width,
                                height = asset.height,
                                duration = null,
                                preparing = false,
                                video = false,
                                onClick = {
                                    val url = asset.videoUrl ?: asset.coverUrl
                                    if (!url.isNullOrBlank()) {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
                                    }
                                },
                            )
                            TeachingMediaKind.MATERIAL -> TeachingMaterialChip(asset, onPlayAsset)
                            TeachingMediaKind.VIDEO -> TeachingMediaPreview(
                                coverUrl = asset.coverUrl,
                                width = asset.width,
                                height = asset.height,
                                duration = asset.durationSeconds,
                                preparing = preparingPlaybackId == asset.assetId || preparingPlaybackId == item.itemId,
                                video = true,
                                onClick = {
                                    onPlayAsset?.invoke(asset)
                                    playWorkspaceVideo(item.itemId, asset.videoUrl)
                                },
                            )
                            TeachingMediaKind.UNKNOWN -> Text(
                                "附件暂时无法预览",
                                style = contentTextStyle(14, 20, FontWeight.Normal, ContentSecondary),
                            )
                        }
                    }
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    onQuote?.let { quote ->
                        DropdownMenuItem(text = { Text("回复") }, onClick = { menuExpanded = false; quote() })
                    }
                    if (visibleText.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("复制") },
                            onClick = {
                                menuExpanded = false
                                context.copyPlainTextToClipboard("交流内容", visibleText)
                            },
                        )
                    }
                    val primary = item.primaryAsset
                    if (primary != null && onSaveAsset != null && !primary.savedToLibrary &&
                        primary.sourceKind.lowercase() == "album"
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (primary.assetId in savingAssetIds) "保存中…" else "保存到素材") },
                            enabled = primary.assetId !in savingAssetIds,
                            onClick = { menuExpanded = false; onSaveAsset(primary) },
                        )
                    }
                    onDelete?.let { delete ->
                        DropdownMenuItem(text = { Text("删除") }, onClick = { menuExpanded = false; delete() })
                    }
                }
            }
        }
    }
}

@Composable
private fun TeachingQuoteBar(name: String, preview: String, onOpen: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ContentSoft)
            .clickable(enabled = onOpen != null) { onOpen?.invoke() }.padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.width(2.dp).height(36.dp).background(ContentText.copy(alpha = 0.35f)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("回复 $name", style = contentTextStyle(12, 18, FontWeight.Normal, ContentSecondary), maxLines = 1)
            Text(preview, style = contentTextStyle(14, 20, FontWeight.Medium, ContentText), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun TeachingVoiceBar(duration: Double?, playing: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier.width(144.dp).height(48.dp).clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.fillMaxWidth().height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, ContentSectionLine, RoundedCornerShape(10.dp))
                .background(ContentSoft)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeachingWaveIcon(playing = playing, modifier = Modifier.size(20.dp))
            Spacer(Modifier.weight(1f))
            teachingClock(duration)?.let {
                Text(it, style = contentTextStyle(14, 20, FontWeight.Normal, ContentSecondary), maxLines = 1)
            }
        }
    }
}

@Composable
internal fun TeachingWaveIcon(playing: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "teaching-wave")
    val animated by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(680), RepeatMode.Reverse),
        label = "teaching-wave-shift",
    )
    val shift = if (playing) animated else 0f
    Canvas(modifier) {
        val color = Color(0xFF226B53)
        val bases = floatArrayOf(0.42f, 0.72f, 1f, 0.68f, 0.4f)
        val barWidth = size.width / 9f
        bases.forEachIndexed { index, base ->
            val amount = if (playing) {
                val wobble = if (index % 2 == 0) shift else 1f - shift
                base * (0.58f + 0.42f * wobble)
            } else {
                base
            }
            val barHeight = size.height * amount
            val left = index * (barWidth + barWidth)
            drawRoundRect(
                color = color,
                topLeft = Offset(left, (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@Composable
private fun TeachingMediaPreview(
    coverUrl: String?,
    width: Int?,
    height: Int?,
    duration: Double?,
    preparing: Boolean,
    video: Boolean,
    onClick: () -> Unit,
) {
    BoxWithConstraints {
        val ratio = if (width != null && height != null && width > 0 && height > 0) {
            width.toFloat() / height.toFloat()
        } else {
            16f / 9f
        }
        val frameWidth = minOf(maxWidth, 280.dp, 240.dp * ratio)
        val frameHeight = frameWidth / ratio
        Box(
            Modifier.size(frameWidth, frameHeight).clip(RoundedCornerShape(12.dp)).background(ContentSoft).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (!coverUrl.isNullOrBlank()) {
                AsyncImage(coverUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            if (video) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.46f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, if (preparing) "正在打开" else "播放", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                teachingClock(duration)?.let {
                    Text(
                        it,
                        style = contentTextStyle(12, 18, FontWeight.Normal, Color.White),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                            .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TeachingMaterialChip(
    asset: V2CoachWorkspaceAsset,
    onPlayAsset: ((V2CoachWorkspaceAsset) -> Unit)?,
) {
    val playWorkspaceVideo = LocalPlayWorkspaceVideo.current
    val meta = asset.tags.firstOrNull { it.isNotBlank() }
    Row(
        Modifier.widthIn(max = 320.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ContentSoft)
            .clickable {
                onPlayAsset?.invoke(asset)
                playWorkspaceVideo(asset.assetId, asset.videoUrl)
            }
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 72.dp, height = 54.dp).clip(RoundedCornerShape(8.dp)).background(ContentControl)) {
            asset.coverUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                asset.title.ifBlank { "引用素材" },
                style = contentTextStyle(14, 20, FontWeight.Medium, ContentText),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta != null) {
                Text(meta, style = contentTextStyle(12, 18, FontWeight.Normal, ContentSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun TeachingAttachmentTray(
    onQuoteMaterial: () -> Unit,
    onPickMedia: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp)).background(ContentSoft).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TeachingTrayTile("引用素材", Icons.Filled.Folder, Modifier.weight(1f), onQuoteMaterial)
        TeachingTrayTile("视频或图片", Icons.Filled.Image, Modifier.weight(1f), onPickMedia)
    }
}

@Composable
private fun TeachingTrayTile(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(ContentPanel).clickable(onClick = onClick)
            .height(88.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = com.mygolfswing.app.ui.coach.CoachGreen, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(title, style = contentTextStyle(14, 20, FontWeight.Medium, ContentText))
    }
}

private fun teachingUrlIsAudio(url: String?): Boolean {
    val value = url?.substringBefore('?')?.lowercase().orEmpty()
    return value.endsWith(".m4a") || value.endsWith(".mp3") || value.endsWith(".aac") ||
        value.endsWith(".wav") || value.endsWith(".ogg")
}
