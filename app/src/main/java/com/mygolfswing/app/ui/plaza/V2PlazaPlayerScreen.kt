package com.mygolfswing.app.ui.plaza

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.coach.V2CoachConnection
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.model.V2CategoryGroup
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoSource
import com.mygolfswing.app.data.repo.V2PlazaComment
import com.mygolfswing.app.data.store.V2CategoryStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.plazaReleaseSafeError
import com.mygolfswing.app.data.store.plazaThrowableError
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.common.V2MissingEntityScreen
import com.mygolfswing.app.ui.common.V2ModalTopBarCentered
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.player.VideoPlayerProgressBar
import com.mygolfswing.app.ui.player.VideoPlayerTool
import com.mygolfswing.app.ui.designsystem.DSLayout
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
    val memoList by V2MemoStore.allMemos.collectAsState()
    val memoCategoryGroups by V2CategoryStore.groups.collectAsState()
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
    var commentMentionDraft by remember(videoId) { mutableStateOf(V2PublicMentionDraft()) }
    var commentRequestId by remember(videoId) { mutableStateOf(UUID.randomUUID().toString()) }
    var lastCommentAttemptBody by remember(videoId) { mutableStateOf<String?>(null) }
    var showMentionPicker by remember { mutableStateOf(false) }
    var mentionPeople by remember { mutableStateOf<List<V2PublicMentionPerson>>(emptyList()) }
    var mentionPeopleLoading by remember { mutableStateOf(false) }
    var commentError by remember { mutableStateOf<String?>(null) }
    var commentReportTarget by remember { mutableStateOf<V2PlazaComment?>(null) }
    var commentReportSubmittingId by remember { mutableStateOf<UUID?>(null) }
    var reportSubmitting by remember { mutableStateOf(false) }
    var reportDetail by remember { mutableStateOf("") }
    var feedbackTab by remember { mutableStateOf("report") }
    var reportReason by remember { mutableStateOf("") }
    var reportResult by remember { mutableStateOf<PlazaSubmitResult?>(null) }
    var claimType by remember { mutableStateOf("") }
    var claimContact by remember { mutableStateOf("") }
    var claimPlatformLink by remember { mutableStateOf("") }
    var memoCreating by remember { mutableStateOf(false) }
    var showMemoSaveSheet by remember { mutableStateOf(false) }
    var selectedMemoGroupId by remember(videoId) { mutableStateOf<String?>(null) }
    var selectedMemoTagIds by remember(videoId) { mutableStateOf<Set<String>>(emptySet()) }
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
    val existingPlazaMemo = remember(memoList, video.id, video.videoUrl) {
        findExistingPlazaMemo(memoList, video)
    }

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
        selectedMemoGroupId = memoCategoryGroups.firstOrNull()?.id
        selectedMemoTagIds = emptySet()
        showMemoSaveSheet = true
    }

    fun saveOrRemoveMemo() {
        scope.launch {
            memoCreating = true
            try {
                val existing = existingPlazaMemo
                if (existing != null) {
                    V2MemoStore.deleteAndWait(existing.id)
                    showMemoSaveSheet = false
                    toastMessage = "已从视频备忘录移除"
                } else {
                    val selectedItems = memoCategoryGroups
                        .flatMap(V2CategoryGroup::items)
                        .filter { it.id in selectedMemoTagIds }
                    val result = createOrFindPlazaVideoMemo(
                        video = video,
                        tagIds = selectedItems.map { it.id },
                        tags = selectedItems.map { it.name },
                    )
                    showMemoSaveSheet = false
                    toastMessage = if (result.created) "已保存到视频备忘录" else "已存到视频备忘录"
                    onMemoCreated(result.memoId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toastMessage = plazaThrowableError("保存 Memo 失败，请稍后重试", e)
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

    fun openMentionPicker() {
        showMentionPicker = true
        scope.launch {
            mentionPeopleLoading = true
            val studentConnections = ServiceLocator.friendSpaceSyncRepo
                .syncOrLoadConnections()
                .getOrDefault(emptyList())
            val coachConnections = runCatching {
                ServiceLocator.coachWorkspaceRepo.connections()
            }.getOrDefault(emptyList())
            mentionPeople = publicMentionPeople(
                connections = studentConnections + coachConnections,
                currentUserId = profile.userId,
            )
            mentionPeopleLoading = false
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
                    memoSaved = existingPlazaMemo != null,
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
                        showComments = true
                        loadComments()
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
            groups = memoCategoryGroups,
            selectedGroupId = selectedMemoGroupId,
            selectedTagIds = selectedMemoTagIds,
            alreadySaved = existingPlazaMemo != null,
            saving = memoCreating,
            onGroupSelect = { groupId -> selectedMemoGroupId = groupId },
            onTagToggle = { tagId ->
                selectedMemoTagIds = when {
                    tagId in selectedMemoTagIds -> selectedMemoTagIds - tagId
                    selectedMemoTagIds.size < 5 -> selectedMemoTagIds + tagId
                    else -> selectedMemoTagIds
                }
            },
            onDismiss = { if (!memoCreating) showMemoSaveSheet = false },
            onConfirm = ::saveOrRemoveMemo,
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
            isLoggedIn = profile.isLoggedIn,
            reportingCommentId = commentReportSubmittingId,
            onDraftChange = {
                val next = commentMentionDraft.updateText(it)
                commentMentionDraft = next
                commentDraft = next.text
                commentError = null
            },
            onMention = ::openMentionPicker,
            onLoginRequest = {
                showComments = false
                onRequestLoginForTarget(V2PlazaPlayerLoginTarget.COMMENTS, null)
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
                            val mentions = commentMentionDraft.updateText(commentDraft).userIds
                            val bodyKey = buildString {
                                append(text)
                                append('\n')
                                append(mentions.joinToString(","))
                            }
                            if (lastCommentAttemptBody != bodyKey) {
                                commentRequestId = UUID.randomUUID().toString()
                                lastCommentAttemptBody = bodyKey
                            }
                            ServiceLocator.plazaRepo.addComment(
                                videoId = cloudVideoId,
                                content = text,
                                mentionUserIds = mentions,
                                clientRequestId = commentRequestId,
                            )
                                .onSuccess {
                                    comments = listOf(it) + comments
                                    commentDraft = ""
                                    commentMentionDraft = V2PublicMentionDraft()
                                    lastCommentAttemptBody = null
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
        )
    }

    if (showMentionPicker) {
        V2PublicMentionPickerDialog(
            people = mentionPeople,
            loading = mentionPeopleLoading,
            onDismiss = { showMentionPicker = false },
            onSelect = { person ->
                val selection = commentMentionDraft
                    .updateText(commentDraft)
                    .select(person.userId, person.name)
                if (selection.accepted) {
                    commentMentionDraft = selection.draft
                    commentDraft = selection.draft.text
                    commentError = null
                    showMentionPicker = false
                } else {
                    commentError = "一次最多提及 5 位已连接的人"
                    showMentionPicker = false
                }
            },
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
            videoTitle = video.title,
            selectedTab = feedbackTab,
            reason = reportReason,
            detail = reportDetail,
            claimType = claimType,
            claimContact = claimContact,
            claimPlatformLink = claimPlatformLink,
            result = reportResult,
            onTabChange = { feedbackTab = it; reportResult = null },
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
                        if (feedbackTab == "claim" && (claimType.isBlank() || claimContact.trim().isBlank())) {
                            reportResult = PlazaSubmitResult(success = false, message = "请填写联系方式")
                            return@launch
                        }
                        if (feedbackTab == "report" && reportReason.isBlank()) return@launch
                        reportResult = null
                        reportSubmitting = true
                        try {
                            val result = if (feedbackTab == "claim") {
                                ServiceLocator.plazaRepo.claimVideo(
                                    videoId = cloudVideoId,
                                    claimType = claimType,
                                    contactInfo = claimContact.trim(),
                                    platformLink = claimPlatformLink.trim(),
                                    detail = reportDetail.trim(),
                                )
                            } else {
                                val selected = plazaVideoReportReasons.first { it.value == reportReason }
                                ServiceLocator.plazaRepo.reportVideo(
                                    videoId = cloudVideoId,
                                    reason = selected.value,
                                    reasonText = selected.label,
                                    detail = reportDetail.trim(),
                                    priority = selected.priority,
                                )
                            }
                            result
                                .onSuccess {
                                    reportResult = PlazaSubmitResult(
                                        success = true,
                                        message = if (feedbackTab == "claim") "认领申请已提交，我们会尽快联系您" else "举报已提交，感谢反馈",
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

internal data class PlazaSubmitResult(
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
            .height(40.dp)
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
    memoSaved: Boolean,
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
            label = when {
                memoCreating -> "处理中"
                memoSaved -> "已存 Memo"
                else -> "存 Memo"
            },
            tint = if (memoSaved) Color(0xFFF59E0B) else Color.White,
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

internal fun findExistingPlazaMemo(
    memos: List<V2Memo>,
    video: com.mygolfswing.app.data.model.V2PlazaVideo,
): V2Memo? = memos.firstOrNull { memo ->
    memo.plazaVideoId == video.id ||
        (memo.source == V2MemoSource.PLAZA_SAVE && memo.videoUrl == video.videoUrl && video.videoUrl.isNotBlank())
}

@kotlin.OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PlazaSaveMemoDialog(
    video: com.mygolfswing.app.data.model.V2PlazaVideo,
    groups: List<V2CategoryGroup>,
    selectedGroupId: String?,
    selectedTagIds: Set<String>,
    alreadySaved: Boolean,
    saving: Boolean,
    onGroupSelect: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId } ?: groups.firstOrNull()
    val availableTags = selectedGroup?.items.orEmpty()
    val selectedItems = groups.flatMap(V2CategoryGroup::items).filter { it.id in selectedTagIds }

    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        sheetState = sheetState,
        containerColor = PlazaCommentsBackground,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                Modifier.padding(vertical = 10.dp).size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50)).background(Color(0xFF6B7280)),
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "保存到视频备忘录",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "关闭",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(30.dp).clip(CircleShape)
                        .clickable(enabled = !saving, onClick = onDismiss).padding(4.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1F2937)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(100.dp).height(75.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF374151)),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            video.title.ifBlank { "广场视频" },
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                        )
                        Text(video.coach.displayName, color = Color(0xFF9CA3AF), fontSize = 12.sp)
                        Text(
                            formatPlazaPlaybackTime(video.durationSec.toFloat()),
                            color = Color(0xFF6B7280),
                            fontSize = 11.sp,
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1F2937)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("选择分类", color = Color(0xFF9CA3AF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    if (groups.isEmpty()) {
                        Text("暂无可用分类", color = Color(0xFF6B7280), fontSize = 13.sp)
                    } else {
                        groups.chunked(3).forEach { rowGroups ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                rowGroups.forEach { group ->
                                    val selected = group.id == selectedGroup?.id
                                    val tint = plazaCategoryColor(group.items.firstOrNull()?.color)
                                    Column(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                            .background(if (selected) tint.copy(alpha = 0.15f) else Color(0xFF111827))
                                            .border(
                                                if (selected) 2.dp else 1.dp,
                                                if (selected) tint else Color(0xFF374151),
                                                RoundedCornerShape(12.dp),
                                            )
                                            .clickable(enabled = !saving) { onGroupSelect(group.id) }
                                            .padding(vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier.size(44.dp).clip(CircleShape)
                                                .background(if (selected) tint else Color(0xFF374151)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                group.name.firstOrNull()?.toString().orEmpty().ifBlank { "类" },
                                                color = if (selected) Color.White else Color(0xFF9CA3AF),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        Text(
                                            group.name,
                                            color = if (selected) Color.White else Color(0xFF9CA3AF),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                        )
                                    }
                                }
                                repeat(3 - rowGroups.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1F2937)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("选择标签", color = Color(0xFF9CA3AF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            "已选 ${selectedTagIds.size}/5",
                            color = if (selectedTagIds.size >= 5) Color(0xFFF59E0B) else Color(0xFF6B7280),
                            fontSize = 12.sp,
                        )
                    }
                    if (availableTags.isEmpty()) {
                        Text(
                            "该分类下暂无标签",
                            color = Color(0xFF6B7280),
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        )
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableTags.forEach { tag ->
                                val selected = tag.id in selectedTagIds
                                val tint = plazaCategoryColor(tag.color)
                                Text(
                                    tag.name,
                                    color = if (selected) Color.White else tint,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                        .background(if (selected) tint else tint.copy(alpha = 0.12f))
                                        .border(
                                            1.dp,
                                            if (selected) Color.Transparent else tint.copy(alpha = 0.30f),
                                            RoundedCornerShape(50),
                                        )
                                        .clickable(
                                            enabled = !saving && (selected || selectedTagIds.size < 5),
                                        ) { onTagToggle(tag.id) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                )
                            }
                        }
                    }
                }

                if (selectedItems.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1F2937)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("已选标签", color = Color(0xFF9CA3AF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedItems.forEach { tag ->
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                        .background(plazaCategoryColor(tag.color))
                                        .clickable(enabled = !saving) { onTagToggle(tag.id) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(tag.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("×", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().v2NavigationBarPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp).height(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (alreadySaved) {
                            Brush.horizontalGradient(listOf(Color(0xFF6B7280), Color(0xFF4B5563)))
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFF9333EA), Color(0xFF2563EB)))
                        },
                    )
                    .clickable(enabled = !saving, onClick = onConfirm),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(
                        when {
                            saving -> if (alreadySaved) "移除中…" else "保存中…"
                            alreadySaved -> "从备忘录移除"
                            else -> "保存到视频备忘录"
                        },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private fun plazaCategoryColor(raw: String?): Color = runCatching {
    val hex = raw?.trim().orEmpty().removePrefix("#").ifBlank { "10B981" }
    Color(android.graphics.Color.parseColor("#$hex"))
}.getOrDefault(Color(0xFF10B981))

private suspend fun createOrFindPlazaVideoMemo(
    video: com.mygolfswing.app.data.model.V2PlazaVideo,
    tagIds: List<String> = emptyList(),
    tags: List<String> = emptyList(),
): PlazaMemoResult {
    V2MemoStore.allMemos.value.firstOrNull { memo ->
        memo.plazaVideoId == video.id ||
            (memo.source == V2MemoSource.PLAZA_SAVE && memo.videoUrl == video.videoUrl && video.videoUrl.isNotBlank())
    }?.let { existing ->
        return PlazaMemoResult(existing.id, created = false)
    }

    val resolvedTagIds = tagIds.map(String::trim).filter(String::isNotBlank).distinct().take(5)
    val resolvedTags = tags.map(String::trim).filter(String::isNotBlank).distinct().take(5)
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
        this.tagIds = resolvedTagIds
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
        VideoPlayerProgressBar(
            tool = VideoPlayerTool.Playback,
            progress = value / maxDuration,
            onScrubStart = {
                isSeeking = true
                onSeekStart()
            },
            onSeekFraction = { fraction -> seekTime = fraction * maxDuration },
            onScrubEnd = { fraction ->
                onSeekFinished(fraction * maxDuration)
                isSeeking = false
            },
            modifier = Modifier.fillMaxWidth().height(24.dp),
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

internal data class V2PublicMentionPerson(val userId: String, val name: String)

internal fun publicMentionPeople(
    connections: List<V2CoachConnection>,
    currentUserId: String?,
): List<V2PublicMentionPerson> = connections
    .asSequence()
    .filter { it.status == "active" }
    .mapNotNull { connection ->
        val person = listOfNotNull(connection.peer, connection.coach, connection.student)
            .firstOrNull { it.userId != currentUserId }
            ?: return@mapNotNull null
        V2PublicMentionPerson(userId = person.userId, name = person.nickname)
    }
    .distinctBy(V2PublicMentionPerson::userId)
    .toList()

@Composable
internal fun V2PublicMentionPickerDialog(
    people: List<V2PublicMentionPerson>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onSelect: (V2PublicMentionPerson) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(people, query) {
        val term = query.trim()
        if (term.isEmpty()) people else people.filter { it.name.contains(term, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        V2RoomPage(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                V2ModalTopBarCentered(title = "@ 提及", onCancel = onDismiss)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .v2NavigationBarPadding()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "提及这个人，不会把私密空间或已有回复开放给他。",
                        color = DSColors.deepGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DSColors.mistGreen.copy(alpha = 0.7f))
                            .padding(12.dp),
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = DSColors.inkGreen,
                            fontSize = 14.sp,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DSLayout.searchFieldHeight)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DSColors.cardWhite)
                            .padding(horizontal = 12.dp)
                            .testTag("public-mention-search"),
                        decorationBox = { innerField ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text("搜索教练或好友", color = DSColors.shadowGray, fontSize = 14.sp)
                                }
                                innerField()
                            }
                        },
                    )
                    when {
                        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = DSColors.fairway)
                        }
                        filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            Text(
                                text = "还没有可提及的教练或好友",
                                color = DSColors.shadowGray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 24.dp),
                            )
                        }
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filtered, key = V2PublicMentionPerson::userId) { person ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(DSColors.cardWhite)
                                        .clickable { onSelect(person) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = person.name,
                                        color = DSColors.inkGreen,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = "提及",
                                        color = DSColors.fairway,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommentsDialog(
    comments: List<V2PlazaComment>,
    loading: Boolean,
    submitting: Boolean,
    draft: String,
    error: String?,
    currentUserId: String?,
    isLoggedIn: Boolean,
    reportingCommentId: UUID?,
    onDraftChange: (String) -> Unit,
    onMention: () -> Unit,
    onLoginRequest: () -> Unit,
    onDismiss: () -> Unit,
    onReportComment: (V2PlazaComment) -> Unit,
    onSend: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PlazaCommentsBackground,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                Modifier.padding(vertical = 10.dp).size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50)).background(Color(0xFF6B7280)),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f).imePadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "评论 (${comments.size})",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF374151)))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading -> CircularProgressIndicator(
                        color = Color(0xFFC084FC),
                        modifier = Modifier.align(Alignment.Center).size(28.dp),
                        strokeWidth = 2.dp,
                    )
                    comments.isEmpty() -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Filled.ChatBubbleOutline,
                            contentDescription = null,
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(36.dp),
                        )
                        Text("暂无评论", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                        Text(
                            if (isLoggedIn) "来发表第一条评论吧" else "登录后可以参与讨论",
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp,
                        )
                    }
                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(comments, key = { it.id }) { comment ->
                            CommentRow(
                                comment = comment,
                                canReport = currentUserId.isNullOrBlank() || comment.userId != currentUserId,
                                reporting = reportingCommentId == comment.id,
                                onReport = { onReportComment(comment) },
                            )
                            Box(
                                Modifier.fillMaxWidth().padding(start = 58.dp).height(1.dp)
                                    .background(Color(0xFF1F2937)),
                            )
                        }
                    }
                }
            }

            if (!error.isNullOrBlank()) {
                Text(
                    text = error,
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF374151)))

            if (isLoggedIn) {
                Row(
                    modifier = Modifier.fillMaxWidth().v2NavigationBarPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(Color(0xFF374151))
                            .clickable(enabled = !submitting, onClick = onMention),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("@", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier.weight(1f).height(40.dp)
                            .clip(RoundedCornerShape(20.dp)).background(Color(0xFF1F2937))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        BasicTextField(
                            value = draft,
                            onValueChange = { if (it.length <= 200) onDraftChange(it) },
                            enabled = !submitting,
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (draft.isEmpty()) {
                                    Text("公开评论，所有人可见…", color = Color(0xFF6B7280), fontSize = 14.sp)
                                }
                                inner()
                            },
                        )
                    }
                    if (draft.isNotBlank()) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF9333EA), Color(0xFF2563EB)),
                                    ),
                                )
                                .clickable(enabled = !submitting, onClick = onSend),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (submitting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "发送评论",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().v2NavigationBarPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(42.dp).clip(RoundedCornerShape(21.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF1F2937), Color(0xFF243B32))),
                        )
                        .clickable(onClick = onLoginRequest)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("●", color = Color.White, fontSize = 9.sp)
                    Text(
                        "登录后参与讨论",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text("›", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private val PlazaCommentsBackground = Color(0xFF111111)

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
                .background(Color(0xFF374151)),
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
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    comment.userName,
                    color = Color(0xFFC084FC),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatCommentTime(comment.createdAt),
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp,
                )
                if (canReport) {
                    Text(
                        if (reporting) "提交中..." else "举报",
                        color = if (reporting) Color(0xFF6B7280) else Color(0xFFEF4444),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = !reporting, onClick = onReport)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(comment.content, color = Color(0xFFD1D5DB), fontSize = 14.sp)
        }
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

internal data class PlazaVideoReportReason(
    val value: String,
    val label: String,
    val color: Color,
    val priority: Int,
)

internal val plazaVideoReportReasons = listOf(
    PlazaVideoReportReason("copyright", "侵权/虚假信息", Color(0xFFF59E0B), 5),
    PlazaVideoReportReason("pornography", "色情/裸露", Color(0xFFEF4444), 8),
    PlazaVideoReportReason("violence", "暴力/危险行为", Color(0xFFDC2626), 7),
    PlazaVideoReportReason("harassment", "骚扰/仇恨言论", Color(0xFFF97316), 6),
    PlazaVideoReportReason("child_abuse", "儿童侵害", Color(0xFFB91C1C), 10),
    PlazaVideoReportReason("spam", "垃圾广告/违规", Color(0xFF6B7280), 3),
    PlazaVideoReportReason("other", "其他", Color(0xFF9CA3AF), 1),
)

private data class PlazaClaimOption(val value: String, val label: String, val color: Color)

private val plazaClaimOptions = listOf(
    PlazaClaimOption("creator", "我是视频创作者", Color(0xFF3B82F6)),
    PlazaClaimOption("copyright_owner", "我是版权方", Color(0xFFF59E0B)),
    PlazaClaimOption("coach_self", "我是教练本人", Color(0xFF10B981)),
)

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportDialog(
    videoTitle: String,
    selectedTab: String,
    reason: String,
    detail: String,
    claimType: String,
    claimContact: String,
    claimPlatformLink: String,
    result: PlazaSubmitResult?,
    onTabChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onDetailChange: (String) -> Unit,
    onClaimTypeChange: (String) -> Unit,
    onClaimContactChange: (String) -> Unit,
    onClaimPlatformLinkChange: (String) -> Unit,
    onDismiss: () -> Unit,
    submitting: Boolean,
    onSubmit: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val claimMode = selectedTab == "claim"
    val canSubmit = if (claimMode) {
        claimType.isNotBlank() && claimContact.trim().isNotEmpty() && !submitting
    } else {
        reason.isNotBlank() && !submitting
    }

    ModalBottomSheet(
        onDismissRequest = { if (!submitting) onDismiss() },
        sheetState = sheetState,
        containerColor = PlazaCommentsBackground,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                Modifier.padding(vertical = 10.dp).size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50)).background(Color(0xFF6B7280)),
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "取消",
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50))
                        .clickable(enabled = !submitting, onClick = onDismiss)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                )
                Text(
                    "反馈",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.width(36.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp)).background(Color(0xFF1F2937)).padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                listOf("report" to "举报视频", "claim" to "内容认领").forEach { (value, label) ->
                    val selected = selectedTab == value
                    Text(
                        label,
                        color = if (selected) Color.White else Color(0xFF9CA3AF),
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color(0xFF374151) else Color.Transparent)
                            .clickable(enabled = !submitting) { onTabChange(value) }
                            .padding(vertical = 9.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("「${videoTitle.ifBlank { "广场视频" }}」", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text(
                        if (claimMode) {
                            "如果您是此视频的原创作者或版权方，可以提交认领申请，我们会尽快与您联系确认合作事宜"
                        } else {
                            "请选择举报原因，我们会在24小时内处理"
                        },
                        color = Color(0xFF9CA3AF),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }

                if (claimMode) {
                    plazaClaimOptions.forEach { option ->
                        PlazaFeedbackChoiceRow(
                            label = option.label,
                            tint = option.color,
                            selected = claimType == option.value,
                            enabled = !submitting,
                            onClick = { onClaimTypeChange(option.value) },
                        )
                    }
                    if (claimType.isNotBlank()) {
                        PlazaFeedbackField(
                            label = "联系方式",
                            required = true,
                            value = claimContact,
                            placeholder = "微信号 / 手机号 / 邮箱",
                            singleLine = true,
                            maxLength = 120,
                            onValueChange = onClaimContactChange,
                        )
                        PlazaFeedbackField(
                            label = "平台主页链接（选填，用于验证身份）",
                            value = claimPlatformLink,
                            placeholder = "抖音/B站/小红书等平台主页链接",
                            singleLine = true,
                            maxLength = 200,
                            onValueChange = onClaimPlatformLinkChange,
                        )
                        PlazaFeedbackField(
                            label = "补充说明（选填）",
                            value = detail,
                            placeholder = "可补充版权、账号或合作信息",
                            maxLength = 300,
                            onValueChange = onDetailChange,
                        )
                    }
                } else {
                    plazaVideoReportReasons.forEach { option ->
                        PlazaFeedbackChoiceRow(
                            label = option.label,
                            tint = option.color,
                            selected = reason == option.value,
                            enabled = !submitting,
                            onClick = { onReasonChange(option.value) },
                        )
                    }
                    if (reason.isNotBlank()) {
                        PlazaFeedbackField(
                            label = "补充说明（可选）",
                            value = detail,
                            placeholder = "可选，帮助我们更快处理",
                            maxLength = 300,
                            onValueChange = onDetailChange,
                        )
                    }
                }

                result?.let { ReportResultBanner(result = it) }

                Box(
                    modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                !canSubmit -> Color(0xFF374151)
                                claimMode -> Color(0xFF3B82F6)
                                else -> Color(0xFFEF4444)
                            },
                        )
                        .clickable(enabled = canSubmit, onClick = onSubmit),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (submitting) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(if (claimMode) "✋" else "⚑", color = Color.White, fontSize = 14.sp)
                        }
                        Text(
                            if (claimMode) "提交认领" else "提交举报",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Text(
                    if (claimMode) {
                        "提交后我们会在48小时内与您联系确认。请确保联系方式有效，虚假认领可能导致账号受限。"
                    } else {
                        "恶意举报可能导致账号受限。我们将严格保密举报者信息。"
                    },
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ReportResultBanner(result: PlazaSubmitResult) {
    val tint = if (result.success) Color(0xFF10B981) else Color(0xFFEF4444)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 12.dp, vertical = 12.dp),
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

@Composable
private fun PlazaFeedbackChoiceRow(
    label: String,
    tint: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (selected) tint.copy(alpha = 0.10f) else Color(0xFF1A1A1A))
            .border(
                1.dp,
                if (selected) tint.copy(alpha = 0.50f) else Color(0xFF2A2A2A),
                RoundedCornerShape(12.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Text("◆", color = if (selected) tint else Color(0xFF6B7280), fontSize = 16.sp)
        }
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape)
                .border(2.dp, if (selected) tint else Color(0xFF374151), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(tint))
        }
    }
}

@Composable
private fun PlazaFeedbackField(
    label: String,
    value: String,
    placeholder: String,
    maxLength: Int,
    onValueChange: (String) -> Unit,
    required: Boolean = false,
    singleLine: Boolean = false,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = Color(0xFF9CA3AF), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (required) Text("*", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(if (singleLine) 44.dp else 100.dp)
                .clip(RoundedCornerShape(10.dp)).background(Color(0xFF1A1A1A))
                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(10.dp))
                .padding(12.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = { if (it.length <= maxLength) onValueChange(it) },
                singleLine = singleLine,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, color = Color(0xFF6B7280), fontSize = 14.sp)
                    inner()
                },
            )
        }
    }
}

private fun plazaUserError(base: String, debugDetail: String?): String {
    return plazaReleaseSafeError(base, debugDetail)
}
