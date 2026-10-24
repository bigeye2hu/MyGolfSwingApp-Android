package com.mygolfswing.app.ui.coach

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.exoplayer.ExoPlayer
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.coach.V2CoachConnection
import com.mygolfswing.app.data.coach.V2CoachLibraryAsset
import com.mygolfswing.app.data.coach.V2CoachWorkspaceItem
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.designsystem.DSColors
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun V2CoachLibraryDetailDialog(
    asset: V2CoachLibraryAsset,
    recentItems: List<V2CoachWorkspaceItem>,
    connections: List<V2CoachConnection>,
    hasShareDraft: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onAssetLoaded: (V2CoachLibraryAsset) -> Unit,
    onRemoved: (V2CoachLibraryAsset) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var displayedAsset by remember(asset.assetId) { mutableStateOf(asset) }
    var loading by remember(asset.assetId) { mutableStateOf(asset.videoUrl.isNullOrBlank()) }
    var removing by remember(asset.assetId) { mutableStateOf(false) }
    var error by remember(asset.assetId) { mutableStateOf<String?>(null) }

    LaunchedEffect(asset.assetId) {
        if (!asset.videoUrl.isNullOrBlank()) return@LaunchedEffect
        ServiceLocator.coachWorkspaceRepo.coachAsset(asset.assetId)
            .onSuccess { loaded ->
                displayedAsset = loaded
                onAssetLoaded(loaded)
            }
            .onFailure { error = "素材暂时无法加载，请稍后重试" }
        loading = false
    }

    Dialog(
        onDismissRequest = { if (!removing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        V2CoachLibraryDetailContent(
            asset = displayedAsset,
            recentDeliveries = coachLibraryDetailDeliveries(displayedAsset.assetId, recentItems),
            connections = connections,
            loading = loading,
            removing = removing,
            error = error,
            hasShareDraft = hasShareDraft,
            onBack = onDismiss,
            onShare = onShare,
            onEdit = onEdit,
            onRemove = {
                if (!removing) {
                    removing = true
                    error = null
                    scope.launch {
                        try {
                            ServiceLocator.coachWorkspaceRepo.updateLibraryAsset(
                                assetId = displayedAsset.assetId,
                                savedToLibrary = false,
                            ).onSuccess {
                                onRemoved(displayedAsset)
                                onDismiss()
                            }.onFailure {
                                error = "暂时无法移出素材，请稍后重试"
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } finally {
                            removing = false
                        }
                    }
                }
            },
        )
    }
}

@Composable
internal fun V2CoachLibraryDetailContent(
    asset: V2CoachLibraryAsset,
    recentDeliveries: List<V2CoachWorkspaceItem>,
    connections: List<V2CoachConnection>,
    loading: Boolean,
    removing: Boolean,
    error: String?,
    hasShareDraft: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    var showsAllRecipients by remember { mutableStateOf(false) }
    var resolvedAspectRatio by remember(asset.assetId) { mutableStateOf<Float?>(null) }
    val teachingNote = coachLibraryLatestTeachingNote(asset, recentDeliveries)
    val shownDeliveries = if (showsAllRecipients) recentDeliveries else recentDeliveries.take(3)
    val connectionNames = connections.associate { connection ->
        connection.connectionId to (connection.student?.nickname ?: "学员")
    }

    Box(modifier.fillMaxSize().background(DSColors.warmWhite)) {
        V2RoomBackground(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(36.dp)) {
                    Text(
                        "素材详情",
                        color = DSColors.inkGreen,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center).testTag("coach-library-detail-title"),
                    )
                    Box(
                        Modifier.align(Alignment.CenterStart).size(36.dp).clip(CircleShape)
                            .clickable(enabled = !removing, onClick = onBack).testTag("coach-library-detail-back"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "返回", tint = DSColors.inkGreen, modifier = Modifier.size(21.dp))
                    }
                    Box(Modifier.align(Alignment.CenterEnd)) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).clickable { showMenu = true }
                                .testTag("coach-library-detail-menu"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.MoreHoriz, "更多操作", tint = DSColors.inkGreen, modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("编辑素材") },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                onClick = { showMenu = false; onEdit() },
                            )
                            DropdownMenuItem(
                                text = { Text("移出素材库", color = DSColors.flagRed) },
                                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = DSColors.flagRed) },
                                onClick = { showMenu = false; showRemoveConfirmation = true },
                            )
                        }
                    }
                }

                CoachLibraryMediaStage(
                    asset = asset,
                    loading = loading,
                    resolvedAspectRatio = resolvedAspectRatio,
                    onAspectRatioResolved = { resolvedAspectRatio = it },
                )

                Text(
                    asset.title.ifBlank { "教学素材" },
                    color = DSColors.inkGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("coach-library-detail-name"),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOfNotNull(
                        asset.tags.firstOrNull() ?: "未分类",
                        "被复用 ${asset.useCount} 次",
                        coachLibraryDuration(asset.durationSeconds),
                        coachLibraryShortDate(asset.createdAt),
                    ).forEach { label ->
                        Text(
                            label,
                            color = DSColors.shadowGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }

                if (asset.tags.isNotEmpty()) {
                    CoachLibraryDetailNoteCard(
                        icon = Icons.Filled.GpsFixed,
                        title = "适用场景",
                        text = asset.tags.joinToString("、"),
                    )
                }
                teachingNote?.let { note ->
                    CoachLibraryDetailNoteCard(
                        icon = Icons.Filled.CheckBox,
                        title = "教学备注",
                        text = note,
                    )
                }
                error?.let {
                    Text(it, color = DSColors.flagRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("最近发送给", color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        if (recentDeliveries.size > 3) {
                            Text(
                                if (showsAllRecipients) "收起" else "查看更多",
                                color = DSColors.fairway,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable {
                                    showsAllRecipients = !showsAllRecipients
                                }.padding(horizontal = 7.dp, vertical = 5.dp),
                            )
                        }
                    }
                    if (shownDeliveries.isEmpty()) {
                        Text("还没有发给学员", color = DSColors.shadowGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    } else {
                        shownDeliveries.forEach { delivery ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        connectionNames[delivery.connectionId] ?: "学员",
                                        color = DSColors.inkGreen,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        coachLibraryShortDate(delivery.createdAt)?.let { "发送于 $it" } ?: "已发送",
                                        color = DSColors.shadowGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                Text(
                                    if (delivery.studentViewedAt == null) "已发送" else "已查看",
                                    color = if (delivery.studentViewedAt == null) DSColors.shadowGray else DSColors.fairway,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                        .background(DSColors.mistGreen.copy(alpha = 0.7f))
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().background(DSColors.warmWhite)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CoachLibraryDetailAction(
                    label = if (hasShareDraft) "继续发送" else "发送给学员",
                    icon = Icons.AutoMirrored.Filled.Send,
                    emphasized = true,
                    enabled = !removing,
                    onClick = onShare,
                    modifier = Modifier.weight(1f).testTag("coach-library-detail-share"),
                )
                CoachLibraryDetailAction(
                    label = "编辑素材",
                    icon = Icons.Filled.Edit,
                    enabled = !removing,
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).testTag("coach-library-detail-edit"),
                )
            }
        }
    }

    if (showRemoveConfirmation) {
        V2CoachLibraryRemoveConfirmationSheet(
            busy = removing,
            confirmTag = "coach-library-detail-remove-confirm",
            onConfirm = { showRemoveConfirmation = false; onRemove() },
            onDismiss = { showRemoveConfirmation = false },
        )
    }
}

@Composable
private fun CoachLibraryMediaStage(
    asset: V2CoachLibraryAsset,
    loading: Boolean,
    resolvedAspectRatio: Float?,
    onAspectRatioResolved: (Float) -> Unit,
) {
    val ratio = coachLibraryMediaAspectRatio(resolvedAspectRatio, asset.width, asset.height)
    Box(
        Modifier.fillMaxWidth().aspectRatio(ratio).clip(RoundedCornerShape(16.dp))
            .background(Color.Black).testTag("coach-library-detail-media"),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !asset.videoUrl.isNullOrBlank() -> CoachLibraryInlineVideo(
                url = asset.videoUrl,
                onAspectRatioResolved = onAspectRatioResolved,
            )
            else -> V2CoachMediaCover(
                asset = asset,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        if (loading) {
            CircularProgressIndicator(Modifier.size(30.dp), color = Color.White, strokeWidth = 2.dp)
        }
    }
}

@Composable
@androidx.annotation.OptIn(UnstableApi::class)
internal fun CoachLibraryInlineVideo(
    url: String,
    onAspectRatioResolved: (Float) -> Unit = {},
    playWhenReady: Boolean = false,
    useController: Boolean = true,
) {
    val context = LocalContext.current
    val currentOnAspectRatioResolved by rememberUpdatedState(onAspectRatioResolved)
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            this.playWhenReady = playWhenReady
            prepare()
        }
    }
    LaunchedEffect(player, playWhenReady) {
        if (playWhenReady) player.play()
    }
    DisposableEffect(player) {
        fun reportVideoSize(videoSize: VideoSize) {
            if (videoSize.width <= 0 || videoSize.height <= 0) return
            val ratio = videoSize.width.toFloat() * videoSize.pixelWidthHeightRatio / videoSize.height.toFloat()
            if (ratio.isFinite() && ratio > 0f) currentOnAspectRatioResolved(ratio)
        }
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) = reportVideoSize(videoSize)
        }
        player.addListener(listener)
        reportVideoSize(player.videoSize)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                this.useController = useController
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        update = {
            it.player = player
            it.useController = useController
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun CoachLibraryDetailNoteCard(icon: ImageVector, title: String, text: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(14.dp)).padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(14.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text, color = DSColors.shadowGray, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun CoachLibraryDetailAction(
    label: String,
    icon: ImageVector,
    emphasized: Boolean = false,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier.height(44.dp).alpha(if (enabled) 1f else 0.55f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (emphasized) DSColors.deepGreen else DSColors.cardWhite)
            .then(
                if (emphasized) Modifier else Modifier.border(1.dp, DSColors.borderLine, RoundedCornerShape(12.dp)),
            )
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (emphasized) Color.White else DSColors.deepGreen, modifier = Modifier.size(17.dp))
        Spacer(Modifier.size(7.dp))
        Text(
            label,
            color = if (emphasized) Color.White else DSColors.deepGreen,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

internal fun coachLibraryDetailDeliveries(
    assetId: String,
    items: List<V2CoachWorkspaceItem>,
): List<V2CoachWorkspaceItem> = items.asSequence()
    .filter { item ->
        item.deletedAt == null && (
            item.primaryAsset?.assetId == assetId || item.attachments.any { it.assetId == assetId }
        )
    }
    .distinctBy(V2CoachWorkspaceItem::itemId)
    .sortedByDescending { item -> coachLibraryEpoch(item.createdAt) }
    .toList()

internal fun coachLibraryLatestTeachingNote(
    asset: V2CoachLibraryAsset,
    deliveries: List<V2CoachWorkspaceItem>,
): String? = deliveries.firstNotNullOfOrNull { item ->
    V2CoachWorkspaceItem.visibleCopy(item.text)?.takeIf { it != asset.title }
}

internal fun coachLibraryDuration(seconds: Double?): String? {
    val value = seconds?.takeIf { it.isFinite() && it > 0 }?.toInt() ?: return null
    return String.format(Locale.ROOT, "%d:%02d", value / 60, value % 60)
}

internal fun coachLibraryMediaAspectRatio(
    resolvedAspectRatio: Float?,
    width: Int?,
    height: Int?,
): Float {
    val metadataRatio = if ((width ?: 0) > 0 && (height ?: 0) > 0) {
        width!!.toFloat() / height!!.toFloat()
    } else {
        null
    }
    return (resolvedAspectRatio?.takeIf { it.isFinite() && it > 0f } ?: metadataRatio ?: (16f / 9f))
        .coerceAtLeast(0.2f)
}

internal fun coachLibraryShortDate(value: String?): String? {
    val instant = runCatching { value?.let(Instant::parse) }.getOrNull()
        ?: runCatching { value?.let(OffsetDateTime::parse)?.toInstant() }.getOrNull()
        ?: return null
    return DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)
        .withZone(ZoneId.systemDefault()).format(instant)
}

private fun coachLibraryEpoch(value: String?): Long =
    runCatching { value?.let(Instant::parse)?.toEpochMilli() }.getOrNull()
        ?: runCatching { value?.let(OffsetDateTime::parse)?.toInstant()?.toEpochMilli() }.getOrNull()
        ?: Long.MIN_VALUE
