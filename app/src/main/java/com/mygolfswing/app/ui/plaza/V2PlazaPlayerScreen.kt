package com.mygolfswing.app.ui.plaza

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoSource
import com.mygolfswing.app.data.repo.V2PlazaComment
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.plazaReleaseSafeError
import com.mygolfswing.app.data.store.plazaThrowableError
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class V2PlazaPlayerLoginTarget {
    COMMENTS,
    DOWNLOAD,
    FEEDBACK,
}

/**
 * 广场视频播放页（对齐 iOS `VideoPlazaPlayerPage`）。
 */
@OptIn(UnstableApi::class)
@Composable
fun V2PlazaPlayerScreen(
    videoId: UUID,
    onClose: () -> Unit = {},
    onOpenCoach: (String) -> Unit = {},
    onMemoCreated: (UUID) -> Unit = {},
    onDownloadVideo: (String) -> Unit = {},
    onRequestLogin: () -> Unit = {},
    onRequestLoginForTarget: (V2PlazaPlayerLoginTarget, String?) -> Unit = { _, _ -> onRequestLogin() },
    loginActionRequest: V2PlazaPlayerLoginTarget? = null,
    onLoginActionConsumed: () -> Unit = {},
) {
    val videos by V2PlazaStore.videos.collectAsState()
    val hero by V2PlazaStore.heroVideo.collectAsState()
    val savedVideoCache by V2PlazaStore.savedVideoCache.collectAsState()
    val watchHistory by V2PlazaStore.watchHistory.collectAsState()
    val likedIds by V2PlazaStore.likedIds.collectAsState()
    val plazaError by V2PlazaStore.lastError.collectAsState()
    val profile by V2ProfileStore.profile.collectAsState()
    val video = remember(hero, videos, savedVideoCache, watchHistory, videoId) {
        V2PlazaStore.videoForPlayback(videoId)
    }
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var showPlayPauseIcon by remember { mutableStateOf(false) }
    var currentTime by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var playbackError by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var wasPlayingBeforeSeek by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var comments by remember(videoId) { mutableStateOf<List<V2PlazaComment>>(emptyList()) }
    var commentsLoading by remember { mutableStateOf(false) }
    var commentSubmitting by remember { mutableStateOf(false) }
    var commentDraft by remember { mutableStateOf("") }
    var commentError by remember { mutableStateOf<String?>(null) }
    var commentReportTarget by remember { mutableStateOf<V2PlazaComment?>(null) }
    var commentReportSubmittingId by remember { mutableStateOf<UUID?>(null) }
    var reportSubmitting by remember { mutableStateOf(false) }
    var reportDetail by remember { mutableStateOf("") }
    var reportReason by remember { mutableStateOf("copyright") }
    var reportResult by remember { mutableStateOf<PlazaSubmitResult?>(null) }
    var claimType by remember { mutableStateOf("creator") }
    var claimContact by remember { mutableStateOf("") }
    var claimPlatformLink by remember { mutableStateOf("") }
    var memoCreating by remember { mutableStateOf(false) }
    var showMemoSaveSheet by remember { mutableStateOf(false) }
    var selectedMemoTags by remember(videoId) { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    if (video == null) {
        V2MissingEntityScreen(message = "视频不存在", onBack = onClose, title = "广场视频")
        return
    }
    if (video.videoUrl.isBlank()) {
        PlazaPlaybackErrorOverlay(
            title = video.title,
            coachName = video.coach.displayName,
            unavailableReason = PlazaPlaybackUnavailableReason.NoSource,
            onClose = onClose,
            onRetry = { V2PlazaStore.refresh() },
        )
        return
    }
    val cloudVideoId = video.sourceVideoId?.takeIf { it.isNotBlank() } ?: video.saveKey()
    val liked = likedIds.contains(cloudVideoId)

    fun openMemoSaveSheet() {
        if (memoCreating) return
        if (!profile.isLoggedIn) {
            toastMessage = "登录后可创建 Memo"
            onRequestLogin()
            return
        }
        if (video.videoUrl.isBlank()) {
            toastMessage = "当前视频没有可用播放地址"
            return
        }
        selectedMemoTags = defaultPlazaMemoTags(video).toSet()
        showMemoSaveSheet = true
    }

    fun createMemoFromVideo(tags: List<String> = defaultPlazaMemoTags(video)) {
        scope.launch {
            memoCreating = true
            try {
                val result = createOrFindPlazaVideoMemo(video, tags)
                showMemoSaveSheet = false
                toastMessage = if (result.created) "已创建练球 Memo" else "已打开已有 Memo"
                onMemoCreated(result.memoId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toastMessage = plazaThrowableError("创建 Memo 失败，请稍后重试", e)
            } finally {
                memoCreating = false
            }
        }
    }

    LaunchedEffect(cloudVideoId) {
        V2PlazaStore.markWatched(video)
    }

    LaunchedEffect(plazaError) {
        val message = plazaError ?: return@LaunchedEffect
        toastMessage = plazaUserError("广场数据暂时不可用，请稍后重试", message)
    }

    fun loadComments() {
        scope.launch {
            commentsLoading = true
            commentError = null
            try {
                ServiceLocator.plazaRepo.fetchComments(cloudVideoId)
                    .onSuccess { comments = it }
                    .onFailure { commentError = plazaThrowableError("评论加载失败，请稍后重试", it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                commentError = plazaThrowableError("评论加载失败，请稍后重试", e)
            } finally {
                commentsLoading = false
            }
        }
    }

    LaunchedEffect(loginActionRequest, profile.isLoggedIn, video.id) {
        if (!profile.isLoggedIn) return@LaunchedEffect
        when (loginActionRequest) {
            V2PlazaPlayerLoginTarget.COMMENTS -> {
                showComments = true
                loadComments()
                onLoginActionConsumed()
            }
            V2PlazaPlayerLoginTarget.FEEDBACK -> {
                showReport = true
                onLoginActionConsumed()
            }
            V2PlazaPlayerLoginTarget.DOWNLOAD,
            null -> Unit
        }
    }

    val exoPlayer = remember(video.videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.videoUrl))
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = true
                isPlaying = false
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    exoPlayer.seekTo(0)
                    exoPlayer.play()
                    currentTime = 0f
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentTime = (exoPlayer.currentPosition / 1000f).coerceAtLeast(0f)
            val durMs = exoPlayer.duration
            if (durMs > 0) duration = durMs / 1000f
            isPlaying = exoPlayer.isPlaying
            delay(100)
        }
    }

    LaunchedEffect(cloudVideoId, duration) {
        while (duration > 0f) {
            val progress = (currentTime / duration).coerceIn(0f, 1f).toDouble()
            V2PlazaStore.updateWatchProgress(video, progress)
            delay(1_000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            update = { it.player = exoPlayer },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                        isPlaying = false
                    } else {
                        exoPlayer.play()
                        isPlaying = true
                    }
                    showPlayPauseIcon = true
                },
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f)),
                    ),
                ),
        )

        TopBar(onClose = onClose, modifier = Modifier.align(Alignment.TopCenter))

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BottomLeftInfo(
                    coachName = video.coach.displayName,
                    coachId = video.sourceCoachId,
                    title = video.title,
                    category = video.categoryDisplayName ?: video.topic.displayName,
                    viewCount = video.viewCount,
                    onOpenCoach = onOpenCoach,
                    modifier = Modifier.weight(1f),
                )
                RightSidebar(
                    liked = liked,
                    saved = video.isSaved,
                    saveCount = video.saveCount,
                    memoCreating = memoCreating,
                    onLike = {
                        if (!profile.isLoggedIn) {
                            toastMessage = "登录后可点赞"
                            onRequestLogin()
                        } else {
                            when (V2PlazaStore.toggleLike(video.id)) {
                                true -> toastMessage = "已点赞"
                                false -> toastMessage = "已取消点赞"
                                null -> Unit
                            }
                        }
                    },
                    onSave = {
                        if (!profile.isLoggedIn) {
                            toastMessage = "登录后可收藏"
                            onRequestLogin()
                        } else {
                            when (V2PlazaStore.toggleSave(video.id)) {
                                true -> toastMessage = "已收藏"
                                false -> toastMessage = "已取消收藏"
                                null -> Unit
                            }
                        }
                    },
                    onComment = {
                        if (!profile.isLoggedIn) {
                            toastMessage = "登录后可评论"
                            onRequestLoginForTarget(V2PlazaPlayerLoginTarget.COMMENTS, null)
                        } else {
                            showComments = true
                            loadComments()
                        }
                    },
                    onDownload = {
                        if (!profile.isLoggedIn) {
                            toastMessage = "登录后可下载视频"
                            onRequestLoginForTarget(V2PlazaPlayerLoginTarget.DOWNLOAD, video.videoUrl)
                        } else if (video.videoUrl.isBlank()) {
                            toastMessage = "当前视频没有可用下载地址"
                        } else {
                            onDownloadVideo(video.videoUrl)
                        }
                    },
                    onCreateMemo = ::openMemoSaveSheet,
                    onFeedback = {
                        if (!profile.isLoggedIn) {
                            toastMessage = "登录后可提交反馈"
                            onRequestLoginForTarget(V2PlazaPlayerLoginTarget.FEEDBACK, null)
                        } else {
                            showReport = true
                        }
                    },
                )
            }
            PlazaProgressBar(
                currentTime = currentTime,
                duration = duration,
                onSeekStart = {
                    wasPlayingBeforeSeek = exoPlayer.isPlaying
                    exoPlayer.pause()
                },
                onSeekFinished = {
                    exoPlayer.seekTo((it * 1000).toLong())
                    currentTime = it
                    if (wasPlayingBeforeSeek) exoPlayer.play()
                },
            )
        }

        AnimatedVisibility(
            visible = showPlayPauseIcon,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.size(56.dp),
                )
            }
            LaunchedEffect(showPlayPauseIcon) {
                if (showPlayPauseIcon) {
                    delay(800)
                    showPlayPauseIcon = false
                }
            }
        }

        if (playbackError) {
            PlazaPlaybackErrorOverlay(
                title = video.title,
                coachName = video.coach.displayName,
                unavailableReason = PlazaPlaybackUnavailableReason.PlaybackError,
                hasOnlineSource = video.videoUrl.isNotBlank(),
                memoCreating = memoCreating,
                onClose = onClose,
                onRetry = {
                    playbackError = false
                    exoPlayer.setMediaItem(MediaItem.fromUri(video.videoUrl))
                    exoPlayer.prepare()
                    exoPlayer.play()
                    isPlaying = true
                },
                onCreateMemo = ::openMemoSaveSheet,
            )
        }

        toastMessage?.let { toast ->
            Toast(text = toast, modifier = Modifier.align(Alignment.TopCenter))
            LaunchedEffect(toast) {
                delay(1600)
                toastMessage = null
            }
        }
    }

    if (showMemoSaveSheet) {
        PlazaSaveMemoDialog(
            video = video,
            selectedTags = selectedMemoTags,
            saving = memoCreating,
            onTagToggle = { tag ->
                selectedMemoTags = if (tag in selectedMemoTags) selectedMemoTags - tag else selectedMemoTags + tag
            },
            onDismiss = { if (!memoCreating) showMemoSaveSheet = false },
            onConfirm = { createMemoFromVideo(selectedMemoTags.toList()) },
        )
    }

    if (showComments) {
        CommentsDialog(
            comments = comments,
            loading = commentsLoading,
            submitting = commentSubmitting,
            draft = commentDraft,
            error = commentError,
            currentUserId = profile.userId,
            reportingCommentId = commentReportSubmittingId,
            onDraftChange = {
                commentDraft = it
                commentError = null
            },
            onDismiss = {
                showComments = false
                commentError = null
            },
            onReportComment = { comment ->
                if (!profile.isLoggedIn) {
                    showComments = false
                    toastMessage = "登录后可举报评论"
                    onRequestLogin()
                } else {
                    commentReportTarget = comment
                }
            },
            onSend = {
                val text = commentDraft.trim()
                if (text.isBlank()) return@CommentsDialog
                if (!profile.isLoggedIn) {
                    showComments = false
                    toastMessage = "登录后可评论"
                    onRequestLogin()
                } else {
                    scope.launch {
                        if (commentSubmitting) return@launch
                        commentSubmitting = true
                        commentError = null
                        try {
                            ServiceLocator.plazaRepo.addComment(cloudVideoId, text)
                                .onSuccess {
                                    comments = listOf(it) + comments
                                    commentDraft = ""
                                }
                                .onFailure { commentError = plazaThrowableError("发送失败，请重试", it) }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            commentError = plazaThrowableError("发送失败，请重试", e)
                        } finally {
                            commentSubmitting = false
                        }
                    }
                }
            },
            onReload = ::loadComments,
        )
    }

    commentReportTarget?.let { target ->
        AlertDialog(
            onDismissRequest = {
                if (commentReportSubmittingId == null) commentReportTarget = null
            },
            title = { Text("举报评论", color = DSColors.inkGreen, fontWeight = FontWeight.Bold) },
            text = { Text("确定要举报这条评论吗？我们会在审核后处理。", color = DSColors.shadowGray) },
            confirmButton = {
                TextButton(
                    enabled = commentReportSubmittingId == null,
                    onClick = {
                        scope.launch {
                            commentReportSubmittingId = target.id
                            commentError = null
                            try {
                                ServiceLocator.plazaRepo.reportComment(
                                    commentId = target.id.toString(),
                                    videoId = cloudVideoId,
                                ).onSuccess {
                                    commentReportTarget = null
                                    toastMessage = "举报已提交，感谢反馈"
                                }.onFailure {
                                    commentReportTarget = null
                                    commentError = plazaThrowableError("举报失败，请稍后重试", it)
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Throwable) {
                                commentReportTarget = null
                                commentError = plazaThrowableError("举报失败，请稍后重试", e)
                            } finally {
                                commentReportSubmittingId = null
                            }
                        }
                    },
                ) {
                    Text(
                        if (commentReportSubmittingId == target.id) "提交中..." else "确认举报",
                        color = DSColors.flagRed,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = commentReportSubmittingId == null,
                    onClick = { commentReportTarget = null },
                ) {
                    Text("取消", color = DSColors.shadowGray)
                }
            },
        )
    }

    if (showReport) {
        ReportDialog(
            reason = reportReason,
            detail = reportDetail,
            claimType = claimType,
            claimContact = claimContact,
            claimPlatformLink = claimPlatformLink,
            result = reportResult,
            onReasonChange = { reportReason = it; reportResult = null },
            onDetailChange = { reportDetail = it; reportResult = null },
            onClaimTypeChange = { claimType = it; reportResult = null },
            onClaimContactChange = { claimContact = it; reportResult = null },
            onClaimPlatformLinkChange = { claimPlatformLink = it; reportResult = null },
            onDismiss = {
                if (!reportSubmitting) {
                    showReport = false
                    reportResult = null
                }
            },
            submitting = reportSubmitting,
            onSubmit = {
                if (!profile.isLoggedIn) {
                    showReport = false
                    toastMessage = "登录后可提交反馈"
                    onRequestLogin()
                } else {
                    scope.launch {
                        if (reportSubmitting) return@launch
                        if (reportReason == "claim" && claimContact.trim().isBlank()) {
                            reportResult = PlazaSubmitResult(success = false, message = "请填写联系方式")
                            return@launch
                        }
                        reportResult = null
                        reportSubmitting = true
                        try {
                            val result = if (reportReason == "claim") {
                                ServiceLocator.plazaRepo.claimVideo(
                                    videoId = cloudVideoId,
                                    claimType = claimType,
                                    contactInfo = claimContact.trim(),
                                    platformLink = claimPlatformLink.trim(),
                                    detail = reportDetail.trim(),
                                )
                            } else {
                                ServiceLocator.plazaRepo.reportVideo(cloudVideoId, reportReason, reportDetail.trim())
                            }
                            result
                                .onSuccess {
                                    reportResult = PlazaSubmitResult(
                                        success = true,
                                        message = if (reportReason == "claim") "认领申请已提交，感谢反馈" else "举报已提交，感谢反馈",
                                    )
                                    reportDetail = ""
                                    claimContact = ""
                                    claimPlatformLink = ""
                                    reportSubmitting = false
                                    delay(1200)
                                    showReport = false
                                    reportResult = null
                                }
                                .onFailure {
                                    reportResult = PlazaSubmitResult(
                                        success = false,
                                        message = plazaThrowableError("提交失败，请稍后重试", it),
                                    )
                                }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            reportResult = PlazaSubmitResult(
                                success = false,
                                message = plazaThrowableError("提交失败，请稍后重试", e),
                            )
                        } finally {
                            reportSubmitting = false
                        }
                    }
                }
            },
        )
    }
}

private enum class PlazaPlaybackUnavailableReason {
    NoSource,
    PlaybackError,
}

private data class PlazaSubmitResult(
    val success: Boolean,
    val message: String,
)

@Composable
private fun PlazaPlaybackErrorOverlay(
    title: String? = null,
    coachName: String? = null,
    unavailableReason: PlazaPlaybackUnavailableReason,
    hasOnlineSource: Boolean = false,
    memoCreating: Boolean = false,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onCreateMemo: (() -> Unit)? = null,
) {
    val isPlaybackError = unavailableReason == PlazaPlaybackUnavailableReason.PlaybackError
    val heading = if (isPlaybackError) "无法播放此视频" else "暂无可播放的视频源"
    val message = if (isPlaybackError) {
        "在线地址可能已失效，或当前网络无法访问视频。可先连接网络，打开「视频广场」刷新列表后再重试。"
    } else {
        "这条广场视频当前没有可用播放地址。请连接网络后刷新广场列表，再回到这里重试。"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1F2937).copy(alpha = 0.96f))
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.VideoLibrary,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(38.dp),
            )
            Text(heading, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (!title.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF111827).copy(alpha = 0.72f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                    )
                    if (!coachName.isNullOrBlank()) {
                        Text(
                            text = "@$coachName",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Text(
                message,
                color = Color(0xFFD1D5DB),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                PlaybackOverlayAction(
                    text = "重试",
                    background = DSColors.fairway,
                    foreground = Color.White,
                    onClick = onRetry,
                )
                if (hasOnlineSource && onCreateMemo != null) {
                    PlaybackOverlayAction(
                        text = if (memoCreating) "创建中..." else "创建 Memo 复盘",
                        background = Color(0xFF374151),
                        foreground = Color(0xFFE9D5FF),
                        enabled = !memoCreating,
                        onClick = onCreateMemo,
                    )
                }
                PlaybackOverlayAction(
                    text = "返回",
                    background = Color.Transparent,
                    foreground = Color.White.copy(alpha = 0.75f),
                    onClick = onClose,
                )
            }
        }
    }
}

@Composable
private fun PlaybackOverlayAction(
    text: String,
    background: Color,
    foreground: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) background else background.copy(alpha = 0.45f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) foreground else foreground.copy(alpha = 0.55f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TopBar(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .v2StatusBarPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(icon = Icons.Filled.Close, contentDescription = "关闭", onClick = onClose)
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.Verified, null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
            Text("正版授权", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BottomLeftInfo(
    coachName: String,
    coachId: String?,
    title: String,
    category: String,
    viewCount: Int,
    onOpenCoach: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.clickable(enabled = !coachId.isNullOrBlank()) {
                coachId?.takeIf { it.isNotBlank() }?.let(onOpenCoach)
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("@$coachName", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (!coachId.isNullOrBlank()) {
                Text(
                    "主页",
                    color = Color(0xFF10B981),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF10B981).copy(alpha = 0.16f))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
        )
        Text(
            text = "#$category",
            color = Color(0xFFC084FC),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "${v2PlazaCompactCount(viewCount)} 播放",
            color = Color(0xFF9CA3AF),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun RightSidebar(
    liked: Boolean,
    saved: Boolean,
    saveCount: Int,
    memoCreating: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onComment: () -> Unit,
    onDownload: () -> Unit,
    onCreateMemo: () -> Unit,
    onFeedback: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        SidebarButton(
            icon = Icons.Filled.Favorite,
            label = "喜欢",
            tint = if (liked) Color(0xFFEF4444) else Color.White,
            onClick = onLike,
        )
        SidebarButton(
            icon = Icons.Filled.ChatBubbleOutline,
            label = "评论",
            tint = Color.White,
            onClick = onComment,
        )
        SidebarButton(
            icon = if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            label = if (saved) "已保存" else v2PlazaCompactCount(saveCount),
            tint = if (saved) Color(0xFFF59E0B) else Color.White,
            onClick = onSave,
        )
        SidebarButton(
            icon = Icons.Filled.Download,
            label = "下载",
            tint = Color.White,
            onClick = onDownload,
        )
        SidebarButton(
            icon = Icons.Filled.VideoLibrary,
            label = if (memoCreating) "创建中" else "Memo",
            tint = Color.White,
            enabled = !memoCreating,
            onClick = onCreateMemo,
        )
        SidebarButton(
            icon = Icons.Filled.Flag,
            label = "反馈",
            tint = Color.White,
            onClick = onFeedback,
        )
    }
}

@Composable
private fun SidebarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(50.dp)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (enabled) tint else Color(0xFF9CA3AF), modifier = Modifier.size(24.dp))
        Text(label, color = if (enabled) Color.White else Color(0xFF9CA3AF), fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

private data class PlazaMemoResult(
    val memoId: UUID,
    val created: Boolean,
)

private fun defaultPlazaMemoTags(video: com.mygolfswing.app.data.model.V2PlazaVideo): List<String> {
    val category = video.categoryDisplayName?.takeIf { it.isNotBlank() } ?: video.topic.displayName
    return listOf(category, "广场视频", "在线视频").distinct()
}

@Composable
private fun PlazaSaveMemoDialog(
    video: com.mygolfswing.app.data.model.V2PlazaVideo,
    selectedTags: Set<String>,
    saving: Boolean,
    onTagToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val tagOptions = remember(video.id) { defaultPlazaMemoTags(video) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存到视频备忘录", color = DSColors.inkGreen, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    video.title.ifBlank { "广场视频" },
                    color = DSColors.inkGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "选择要写入 Memo 的标签，保存后可在视频 Memo 中继续慢放、对比和记录练球心得。",
                    color = DSColors.shadowGray,
                    fontSize = 12.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    tagOptions.forEach { tag ->
                        val selected = tag in selectedTags
                        Text(
                            tag,
                            color = if (selected) Color.White else DSColors.fairway,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) DSColors.fairway else DSColors.mistGreen)
                                .clickable(enabled = !saving) { onTagToggle(tag) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = onConfirm) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (saving) {
                        CircularProgressIndicator(color = DSColors.fairway, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    }
                    Text(if (saving) "保存中..." else "保存", color = DSColors.fairway, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) {
                Text("取消", color = DSColors.shadowGray)
            }
        },
        containerColor = DSColors.cardWhite,
    )
}

private suspend fun createOrFindPlazaVideoMemo(
    video: com.mygolfswing.app.data.model.V2PlazaVideo,
    tags: List<String> = defaultPlazaMemoTags(video),
): PlazaMemoResult {
    V2MemoStore.allMemos.value.firstOrNull { memo ->
        memo.plazaVideoId == video.id ||
            (memo.source == V2MemoSource.PLAZA_SAVE && memo.videoUrl == video.videoUrl && video.videoUrl.isNotBlank())
    }?.let { existing ->
        return PlazaMemoResult(existing.id, created = false)
    }

    val resolvedTags = tags.map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { defaultPlazaMemoTags(video) }.distinct()
    val memo = V2Memo.mixedMemo(
        displayTitle = video.title.ifBlank { "广场视频" }.take(36),
        body = "来自${video.coach.displayName}的广场视频，可用于慢放、对比和复盘。",
        videoUrl = video.videoUrl,
        durationSec = video.durationSec.takeIf { it > 0.0 },
        topic = video.topic,
        tags = resolvedTags,
        metaText = "广场视频 · ${video.coach.displayName}",
    ).apply {
        source = V2MemoSource.PLAZA_SAVE
        thumbnailUrl = video.thumbnailUrl
        trainingMethod = "广场视频"
        isOnlineVideo = true
        platform = "广场"
        plazaVideoId = video.id
        thumbnailKind = V2MemoBridge.thumbnailKindFor(video.topic)
    }
    return PlazaMemoResult(V2MemoStore.createAndWait(memo), created = true)
}

@Composable
private fun PlazaProgressBar(
    currentTime: Float,
    duration: Float,
    onSeekStart: () -> Unit,
    onSeekFinished: (Float) -> Unit,
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekTime by remember { mutableFloatStateOf(0f) }
    val maxDuration = duration.coerceAtLeast(0.1f)
    val value = if (isSeeking) seekTime else currentTime.coerceIn(0f, maxDuration)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .v2NavigationBarPadding(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPlazaPlaybackTime(value),
                color = Color.White.copy(alpha = 0.84f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatPlazaPlaybackTime(duration),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Slider(
            value = value,
            onValueChange = {
                if (!isSeeking) {
                    isSeeking = true
                    onSeekStart()
                }
                seekTime = it
            },
            onValueChangeFinished = {
                onSeekFinished(seekTime)
                isSeeking = false
            },
            valueRange = 0f..maxDuration,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
            modifier = Modifier.height(24.dp),
        )
    }
}

private fun formatPlazaPlaybackTime(seconds: Float): String {
    if (!seconds.isFinite() || seconds <= 0f) return "0:00"
    val total = seconds.toInt().coerceAtLeast(0)
    val minutes = total / 60
    val secs = (total % 60).toString().padStart(2, '0')
    return "$minutes:$secs"
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Toast(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .v2StatusBarPadding()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Composable
private fun CommentsDialog(
    comments: List<V2PlazaComment>,
    loading: Boolean,
    submitting: Boolean,
    draft: String,
    error: String?,
    currentUserId: String?,
    reportingCommentId: UUID?,
    onDraftChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onReportComment: (V2PlazaComment) -> Unit,
    onSend: () -> Unit,
    onReload: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier
            .imePadding()
            .v2NavigationBarPadding(),
        onDismissRequest = onDismiss,
        title = { Text("评论 (${comments.size})", color = DSColors.inkGreen, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 200) onDraftChange(it) },
                    placeholder = { Text("写下你的想法…") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (loading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = DSColors.fairway, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("加载中…", color = DSColors.shadowGray, fontSize = 12.sp)
                    }
                }
                if (!error.isNullOrBlank()) {
                    CommentErrorBanner(message = error)
                }
                if (!loading && comments.isEmpty()) {
                    Text("还没有评论，来写第一条。", color = DSColors.shadowGray, fontSize = 13.sp)
                } else if (comments.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            CommentRow(
                                comment = comment,
                                canReport = currentUserId.isNullOrBlank() || comment.userId != currentUserId,
                                reporting = reportingCommentId == comment.id,
                                onReport = { onReportComment(comment) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !submitting && draft.isNotBlank(), onClick = onSend) {
                Text(if (submitting) "发送中..." else "发送", color = DSColors.fairway)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReload) { Text("刷新", color = DSColors.shadowGray) }
                TextButton(onClick = onDismiss) { Text("关闭", color = DSColors.shadowGray) }
            }
        },
    )
}

@Composable
private fun CommentRow(
    comment: V2PlazaComment,
    canReport: Boolean,
    reporting: Boolean,
    onReport: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(DSColors.mistGreen),
            contentAlignment = Alignment.Center,
        ) {
            if (!comment.userAvatar.isNullOrBlank()) {
                AsyncImage(
                    model = comment.userAvatar,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    comment.userName.firstOrNull()?.toString().orEmpty().ifBlank { "用" },
                    color = DSColors.fairway,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    comment.userName,
                    color = DSColors.inkGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatCommentTime(comment.createdAt),
                    color = DSColors.shadowGray.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                )
                if (canReport) {
                    Text(
                        if (reporting) "提交中..." else "举报",
                        color = if (reporting) DSColors.shadowGray else DSColors.flagRed,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = !reporting, onClick = onReport)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(comment.content, color = DSColors.shadowGray, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CommentErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DSColors.flagRed.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(DSColors.flagRed),
        )
        Text(
            message,
            color = DSColors.flagRed,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatCommentTime(createdAt: LocalDateTime): String {
    val now = LocalDateTime.now()
    val elapsed = Duration.between(createdAt, now)
    return when {
        elapsed.isNegative || elapsed.toMinutes() < 1 -> "刚刚"
        elapsed.toMinutes() < 60 -> "${elapsed.toMinutes()}分钟前"
        elapsed.toHours() < 24 -> "${elapsed.toHours()}小时前"
        createdAt.toLocalDate() == LocalDate.now().minusDays(1) -> "昨天"
        else -> "${createdAt.monthValue}月${createdAt.dayOfMonth}日"
    }
}

@Composable
private fun ReportDialog(
    reason: String,
    detail: String,
    claimType: String,
    claimContact: String,
    claimPlatformLink: String,
    result: PlazaSubmitResult?,
    onReasonChange: (String) -> Unit,
    onDetailChange: (String) -> Unit,
    onClaimTypeChange: (String) -> Unit,
    onClaimContactChange: (String) -> Unit,
    onClaimPlatformLinkChange: (String) -> Unit,
    onDismiss: () -> Unit,
    submitting: Boolean,
    onSubmit: () -> Unit,
) {
    val reasons = listOf(
        "copyright" to "侵权/虚假信息",
        "spam" to "垃圾广告",
        "harassment" to "骚扰/仇恨言论",
        "violence" to "暴力/危险行为",
        "claim" to "内容认领",
        "other" to "其他",
    )
    val claimTypes = listOf(
        "creator" to "我是视频创作者",
        "copyright_owner" to "我是版权方",
        "coach_self" to "我是教练本人",
    )
    AlertDialog(
        modifier = Modifier
            .imePadding()
            .v2NavigationBarPadding(),
        onDismissRequest = onDismiss,
        title = { Text("举报 / 认领", color = DSColors.inkGreen, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                reasons.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (reason == value) DSColors.mistGreen else Color.Transparent)
                            .clickable { onReasonChange(value) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (reason == value) DSColors.fairway else DSColors.shadowGray.copy(alpha = 0.35f)),
                        )
                        Text(label, color = DSColors.inkGreen, fontSize = 13.sp)
                    }
                }
                if (reason == "claim") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("认领身份", color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        claimTypes.forEach { (value, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (claimType == value) DSColors.mistGreen else Color.Transparent)
                                    .clickable { onClaimTypeChange(value) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (claimType == value) DSColors.fairway else DSColors.shadowGray.copy(alpha = 0.35f)),
                                )
                                Text(label, color = DSColors.inkGreen, fontSize = 13.sp)
                            }
                        }
                        OutlinedTextField(
                            value = claimContact,
                            onValueChange = { if (it.length <= 120) onClaimContactChange(it) },
                            label = { Text("联系方式") },
                            placeholder = { Text("微信、邮箱或手机号") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = claimPlatformLink,
                            onValueChange = { if (it.length <= 200) onClaimPlatformLinkChange(it) },
                            label = { Text("平台主页/原视频链接") },
                            placeholder = { Text("可选，用于加快核验") },
                            minLines = 1,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                OutlinedTextField(
                    value = detail,
                    onValueChange = { if (it.length <= 300) onDetailChange(it) },
                    label = { Text(if (reason == "claim") "认领说明" else "补充说明") },
                    placeholder = { Text(if (reason == "claim") "可补充版权、账号或合作信息" else "可选，帮助我们更快处理") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                result?.let {
                    ReportResultBanner(result = it)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !submitting, onClick = onSubmit) {
                Text(
                    when {
                        submitting -> "提交中..."
                        reason == "claim" -> "提交认领"
                        else -> "提交举报"
                    },
                    color = if (reason == "claim") DSColors.fairway else DSColors.flagRed,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = DSColors.shadowGray) }
        },
    )
}

@Composable
private fun ReportResultBanner(result: PlazaSubmitResult) {
    val tint = if (result.success) DSColors.fairway else DSColors.flagRed
    val bg = if (result.success) DSColors.mistGreen else DSColors.flagRed.copy(alpha = 0.10f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(tint),
        )
        Text(
            result.message,
            color = tint,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun plazaUserError(base: String, debugDetail: String?): String {
    return plazaReleaseSafeError(base, debugDetail)
}
