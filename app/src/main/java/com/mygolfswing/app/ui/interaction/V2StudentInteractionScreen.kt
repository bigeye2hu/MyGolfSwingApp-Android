package com.mygolfswing.app.ui.interaction

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.R
import com.mygolfswing.app.data.coach.V2CoachConnection
import com.mygolfswing.app.data.coach.V2CellularUploadConfirmationRequired
import com.mygolfswing.app.data.coach.V2CoachDeliveryJob
import com.mygolfswing.app.data.coach.V2CoachDeliveryPhase
import com.mygolfswing.app.data.coach.V2CoachDiscussionResource
import com.mygolfswing.app.data.coach.V2CoachQueuedAttachment
import com.mygolfswing.app.data.coach.V2_MAX_RELATED_MEDIA
import com.mygolfswing.app.data.coach.V2CoachInvitePreview
import com.mygolfswing.app.data.coach.V2CoachLibraryAsset
import com.mygolfswing.app.data.coach.isLibraryVideoAsset
import com.mygolfswing.app.data.coach.V2CoachPage
import com.mygolfswing.app.data.coach.V2CoachQuickReply
import com.mygolfswing.app.data.coach.V2CoachStudentAge
import com.mygolfswing.app.data.coach.V2CoachTrainingGoal
import com.mygolfswing.app.data.coach.V2CoachThread
import com.mygolfswing.app.data.coach.V2CoachUserFacingException
import com.mygolfswing.app.data.coach.V2CoachWorkspaceItem
import com.mygolfswing.app.data.coach.V2PendingCoachInvite
import com.mygolfswing.app.data.coach.V2StudentUnreadSnapshot
import com.mygolfswing.app.data.coach.V2WorkspaceContentMode
import com.mygolfswing.app.data.coach.V2_WORKSPACE_DRAFT_LIFETIME_MILLIS
import com.mygolfswing.app.data.coach.V2WorkspaceDraftStore
import com.mygolfswing.app.data.coach.V2WorkspaceDraftAttachment
import com.mygolfswing.app.data.coach.sortStudentSpaces
import com.mygolfswing.app.data.coach.latestStudentSpaceActivityByConnection
import com.mygolfswing.app.data.coach.latestStudentSpaceItemsByConnection
import com.mygolfswing.app.data.coach.sortWorkspaceItems
import com.mygolfswing.app.data.coach.sortWorkspaceSwitcherConnections
import com.mygolfswing.app.data.coach.workspaceRequiresCoachReview
import com.mygolfswing.app.data.coach.jobsForOwner
import com.mygolfswing.app.data.coach.mergeWorkspaceThreads
import com.mygolfswing.app.data.coach.mergeWorkspaceFeedPage
import com.mygolfswing.app.data.coach.coachThrowableError
import com.mygolfswing.app.data.coach.coachStudentRelativeTime
import com.mygolfswing.app.data.coach.filterQuickReplies
import com.mygolfswing.app.data.friend.V2FriendRequestOutcome
import com.mygolfswing.app.data.friend.V2FriendSearchUser
import com.mygolfswing.app.data.friend.V2IMConversation
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.session.V2Session
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSLayout
import com.mygolfswing.app.ui.common.V2PageArtwork
import com.mygolfswing.app.ui.common.V2PageArtworkBackground
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.memo.V2MemoDetailScreen
import com.mygolfswing.app.ui.memo.V2VideoMemoPlayerScreen
import com.mygolfswing.app.ui.common.V2PageArtworkTone
import com.mygolfswing.app.ui.common.V2NavTopBarCentered
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.common.V2RoomPage
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.common.copyPlainTextToClipboard
import com.mygolfswing.app.ui.common.v2FullScreenInsets
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.v2StatusBarPadding
import com.mygolfswing.app.ui.coach.CoachCourseHeader
import com.mygolfswing.app.ui.coach.CoachCourseHeaderImage
import com.mygolfswing.app.ui.coach.CoachBody
import com.mygolfswing.app.ui.coach.CoachGreen
import com.mygolfswing.app.ui.coach.CoachInk
import com.mygolfswing.app.ui.coach.CoachLine
import com.mygolfswing.app.ui.coach.CoachMeta
import com.mygolfswing.app.ui.coach.CoachPaper
import com.mygolfswing.app.ui.coach.CoachSendIdle
import com.mygolfswing.app.ui.coach.CoachSendIdleText
import com.mygolfswing.app.ui.coach.CoachPracticeListSheet
import com.mygolfswing.app.ui.coach.CoachStudentOrderDialog
import com.mygolfswing.app.ui.coach.CoachStudentSwitcherDrawer
import com.mygolfswing.app.ui.golfcam.saveExportedVideoToGallery
import com.mygolfswing.app.ui.swing.V2SwingAnalysisTrimScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private enum class SpaceFilter(val label: String) { ALL("全部"), COACH("教练"), FRIEND("好友") }
internal enum class WorkspaceThreadTab(val title: String) {
    CONTENT("内容"),
    DISCUSSION("交流"),
    RESOURCES("资料"),
}

internal enum class WorkspacePrimaryVideoReplacementPolicy {
    USE_DIRECTLY,
    DEMOTE_CURRENT_TO_RELATED,
    ASK_BEFORE_REPLACING,
}

internal fun workspacePrimaryVideoReplacementPolicy(
    currentSourceKind: String?,
    replacementSourceKind: String,
    coachPerspective: Boolean,
    relatedCount: Int,
): WorkspacePrimaryVideoReplacementPolicy = when {
    currentSourceKind == null -> WorkspacePrimaryVideoReplacementPolicy.USE_DIRECTLY
    coachPerspective &&
        replacementSourceKind == "analysis_recording" &&
        currentSourceKind != "analysis_recording" &&
        currentSourceKind != "voice_recording" &&
        relatedCount < V2_MAX_RELATED_MEDIA -> WorkspacePrimaryVideoReplacementPolicy.DEMOTE_CURRENT_TO_RELATED
    else -> WorkspacePrimaryVideoReplacementPolicy.ASK_BEFORE_REPLACING
}

internal fun workspaceVoiceInputTitle(
    requestingPermission: Boolean,
    recording: Boolean,
    cancelling: Boolean,
): String = when {
    requestingPermission -> "正在请求麦克风权限…"
    recording && cancelling -> "松开取消"
    recording -> "松开发送 · 上滑取消"
    else -> "按住说话"
}

internal fun workspaceComposerSentNotice(
    peerName: String,
    attachmentCount: Int,
    includedVoice: Boolean,
): String {
    val attachmentText = when {
        attachmentCount <= 0 -> ""
        includedVoice -> " · 含语音"
        else -> " · 含${attachmentCount}段视频"
    }
    return "已送达 $peerName$attachmentText"
}

internal fun workspaceComposerQueuedNotice(attachmentCount: Int, includedVoice: Boolean): String =
    when {
        includedVoice -> "已加入发送队列（含语音），可以继续处理下一条"
        attachmentCount > 0 -> "已加入发送队列（含${attachmentCount}段视频），可以继续处理下一条"
        else -> "已加入发送队列，可以继续处理下一条"
    }

internal enum class V2WorkspacePreparedVideoRoute {
    STUDENT_CONFIRM,
    TRIM,
    COACH_COMPOSER,
}

internal fun appendWorkspaceQuickReplyDraft(existing: String, incoming: String, limit: Int = 2_000): String {
    val trimmedIncoming = incoming.trim()
    if (trimmedIncoming.isBlank()) return existing.take(limit)
    val trimmedExisting = existing.trim()
    return (if (trimmedExisting.isBlank()) trimmedIncoming else "$trimmedExisting\n$trimmedIncoming").take(limit)
}

internal fun workspacePostSuccessMessage(
    friendSpace: Boolean,
    coachPerspective: Boolean,
    hasMedia: Boolean,
): String = when {
    !hasMedia -> "消息已送达，正在打开交流"
    friendSpace -> "视频已送达，正在打开交流"
    coachPerspective -> "视频已送达，正在打开交流"
    else -> "练习已送达，正在打开交流"
}

internal fun workspaceErrorIsContentSync(message: String): Boolean = listOf(
    "同步", "加载", "读取", "刷新", "打开这条交流", "更早的空间内容",
).any(message::contains)

internal fun workspaceDisconnectionBannerText(
    connection: V2CoachConnection,
    currentUserId: String?,
    peerName: String?,
): String {
    val disconnectedBy = connection.disconnectedByUserId?.trim().orEmpty()
    val actor = when {
        disconnectedBy.isBlank() -> "连接已解除"
        disconnectedBy == currentUserId?.trim() -> "你已解除连接"
        else -> "${peerName?.trim().orEmpty().ifBlank { "对方" }}已解除连接"
    }
    return "$actor，历史内容仍可查看；重新连接后可继续互动"
}

internal fun v2WorkspacePreparedVideoRoute(
    coachPerspective: Boolean,
    captureSource: Boolean,
    hasDuration: Boolean,
): V2WorkspacePreparedVideoRoute = when {
    !coachPerspective && captureSource -> V2WorkspacePreparedVideoRoute.STUDENT_CONFIRM
    hasDuration -> V2WorkspacePreparedVideoRoute.TRIM
    coachPerspective -> V2WorkspacePreparedVideoRoute.COACH_COMPOSER
    else -> V2WorkspacePreparedVideoRoute.STUDENT_CONFIRM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2StudentInteractionScreen(
    onOpenMemo: () -> Unit,
    onRequestLogin: () -> Unit,
    unread: V2StudentUnreadSnapshot = V2StudentUnreadSnapshot(),
    initialConnectionId: String? = null,
    initialConversationItemId: String? = null,
    onInitialRouteOpened: () -> Unit = {},
    pendingInvite: V2PendingCoachInvite? = null,
    onPendingInviteConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session by ServiceLocator.session.state.collectAsState(initial = V2Session())
    var connections by remember { mutableStateOf<List<V2CoachConnection>>(emptyList()) }
    var conversations by remember { mutableStateOf<List<V2IMConversation>>(emptyList()) }
    var friendRequestCount by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showInvite by remember { mutableStateOf(false) }
    var showConnectionOptions by remember { mutableStateOf(false) }
    var showAddFriend by remember { mutableStateOf(false) }
    var showContacts by remember { mutableStateOf(false) }
    var connectionNotice by remember { mutableStateOf<String?>(null) }
    var selectedConnection by remember { mutableStateOf<V2CoachConnection?>(null) }
    var selectedChat by remember { mutableStateOf<V2IMConversation?>(null) }
    var routedItem by remember { mutableStateOf<V2CoachWorkspaceItem?>(null) }
    var routedItemId by remember { mutableStateOf<String?>(null) }
    var showCoachOrder by rememberSaveable { mutableStateOf(false) }
    var coachOrderRevision by remember { mutableIntStateOf(0) }
    var manuallyRefreshing by remember { mutableStateOf(false) }

    suspend fun renewTencentSession(): V2Session {
        ServiceLocator.authRepo.refreshUserSig().getOrThrow()
        return ServiceLocator.session.current()
    }

    suspend fun reloadNow(holdEmptyResult: Boolean = true) {
        val startedAt = SystemClock.elapsedRealtime()
        val currentSession = ServiceLocator.session.current()
        if (!currentSession.isLoggedIn) {
            loading = false
            connections = emptyList()
            conversations = emptyList()
            return
        }
        if (connections.none { it.status == "active" && it.relationshipType != "friend" }) loading = true
        error = null
        ServiceLocator.friendSpaceSyncRepo.syncOrLoadConnections()
            .onSuccess { connections = it }
            .onFailure { error = coachThrowableError("教学空间暂时无法同步，请稍后重试", it) }
        ServiceLocator.tencentFriendRepo.conversations(
            session = ServiceLocator.session.current(),
            renewExpiredSession = ::renewTencentSession,
        ).onSuccess { conversations = it }
        ServiceLocator.tencentFriendRepo.incomingFriendRequests(
            session = ServiceLocator.session.current(),
            renewExpiredSession = ::renewTencentSession,
        ).onSuccess { friendRequestCount = it.size }
        if (holdEmptyResult && connections.none { it.status == "active" && it.relationshipType != "friend" }) {
            val remaining = 700L - (SystemClock.elapsedRealtime() - startedAt)
            if (remaining > 0L) delay(remaining)
        }
        loading = false
    }

    fun reload(holdEmptyResult: Boolean = true) {
        scope.launch { reloadNow(holdEmptyResult) }
    }

    fun refreshAll() {
        if (manuallyRefreshing) return
        scope.launch {
            manuallyRefreshing = true
            try {
                reloadNow(holdEmptyResult = true)
            } finally {
                manuallyRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { reloadNow() }
    LaunchedEffect(pendingInvite) {
        if (pendingInvite != null) {
            selectedConnection = null
            showInvite = true
        }
    }
    LaunchedEffect(initialConnectionId, initialConversationItemId, connections) {
        val connectionId = initialConnectionId ?: return@LaunchedEffect
        val connection = connections.firstOrNull { it.connectionId == connectionId } ?: return@LaunchedEffect
        routedItemId = initialConversationItemId
        routedItem = unread.recentItems.firstOrNull { it.itemId == initialConversationItemId }
        selectedConnection = connection
        onInitialRouteOpened()
    }

    selectedConnection?.let { connection ->
        V2WorkspaceScreen(
            connection = connection,
            onOpenMemo = onOpenMemo,
            availableConnections = connections.filter { it.relationshipType != "friend" },
            switcherItems = unread.recentItems,
            onSwitchConnection = { next ->
                selectedConnection = next
                routedItem = null
                routedItemId = null
            },
            onConnectionUpdated = { updated ->
                connections = connections.map { current ->
                    if (current.connectionId == updated.connectionId) updated else current
                }
                selectedConnection = updated
            },
            onBack = {
                selectedConnection = null
                routedItem = null
                routedItemId = null
            },
            onDisconnected = {
                selectedConnection = null
                routedItem = null
                routedItemId = null
                reload()
            },
            initialItem = routedItem,
            initialItemId = routedItemId,
        )
        return
    }

    selectedChat?.let { chat ->
        val coachIds = connections.asSequence()
            .filter { it.status == "active" && it.relationshipType != "friend" }
            .mapNotNull { it.coach?.userId }
            .toSet()
        V2InteractionChatScreen(
            conversation = chat,
            subtitle = if (chat.userId in coachIds) "我的教练" else "好友私聊",
            onBack = {
                selectedChat = null
                reload(holdEmptyResult = false)
            },
        )
        return
    }

    val coachConnections = remember(connections, session.userId, coachOrderRevision) {
        applyInteractionCoachOrder(
            connections = connections.filter { it.status == "active" && it.relationshipType != "friend" },
            savedOrder = loadInteractionCoachOrder(context, session.userId),
        )
    }
    val shownCoaches = coachConnections
    val latestItemsByConnection = latestStudentSpaceItemsByConnection(unread.recentItems)
    val coachOrder = shownCoaches.mapIndexedNotNull { index, connection ->
        connection.coach?.userId?.let { it to index }
    }.toMap()
    val coachByUserId = shownCoaches.mapNotNull { connection ->
        connection.coach?.userId?.let { it to connection.coach }
    }.toMap()
    val mergedChats = buildList {
        conversations.forEach { conversation ->
            val coach = coachByUserId[conversation.userId]
            add(
                conversation.copy(
                    title = coach?.nickname ?: conversation.title,
                    avatar = conversation.avatar?.takeIf(String::isNotBlank) ?: coach?.avatar,
                    isCoach = coach != null,
                ),
            )
        }
        val existing = conversations.mapTo(mutableSetOf(), V2IMConversation::userId)
        shownCoaches.forEach { connection ->
            val coach = connection.coach ?: return@forEach
            if (coach.userId !in existing) {
                add(
                    V2IMConversation(
                        userId = coach.userId,
                        title = coach.nickname,
                        avatar = coach.avatar,
                        preview = "已连接，可以开始聊天",
                        isCoach = true,
                    ),
                )
            }
        }
    }.sortedWith(
        compareBy<V2IMConversation> { it.userId !in coachByUserId }
            .thenBy { coachOrder[it.userId] ?: Int.MAX_VALUE }
            .thenByDescending { it.timeEpochSeconds ?: Long.MIN_VALUE },
    )

    Box(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        V2RoomBackground(Modifier.fillMaxSize())
        PullToRefreshBox(
            isRefreshing = manuallyRefreshing,
            onRefresh = ::refreshAll,
            modifier = Modifier.fillMaxSize().testTag("student-interaction-pull-refresh"),
        ) {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("挥呗", color = DSColors.deepGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("和更好的人，一起打好球", color = DSColors.shadowGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(
                        modifier = Modifier.height(DSLayout.compactButtonHeight).clip(RoundedCornerShape(50)).background(DSColors.fairway)
                            .clickable {
                                scope.launch {
                                    if (ServiceLocator.session.current().isLoggedIn) showConnectionOptions = true else onRequestLogin()
                                }
                            }.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Text("连接", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            if (error != null && shownCoaches.isNotEmpty()) item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.94f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Filled.WifiOff, null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("教学空间暂时没有更新", color = DSColors.deepGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("当前仍显示上次内容", color = DSColors.shadowGray, fontSize = 11.sp)
                    }
                    Text("重新同步", color = DSColors.fairway, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { reload(holdEmptyResult = false) })
                }
            }
            when {
                loading && shownCoaches.isEmpty() -> item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.94f))
                            .padding(horizontal = 22.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = DSColors.fairway, strokeWidth = 2.dp)
                        Text("正在同步教学空间", color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("连接和最近对话会显示在这里。", color = DSColors.shadowGray, fontSize = 13.sp)
                    }
                }
                error != null && shownCoaches.isEmpty() -> item {
                    InteractionCoachStatusCard(
                        title = "教学空间暂时没同步下来",
                        detail = "已有连接和历史内容不会丢失。",
                        action = "重新同步",
                        onAction = { reload() },
                    )
                }
                shownCoaches.isEmpty() -> item {
                    InteractionEmptyCoachCard(
                        onConnect = { showInvite = true },
                    )
                }
                else -> {
                    item {
                        InteractionCoachCardSection(
                            connections = shownCoaches,
                            unreadByConnection = unread.byConnection,
                            latestItemsByConnection = latestItemsByConnection,
                            onOrder = { showCoachOrder = true },
                            onOpen = { connection ->
                                routedItem = null
                                routedItemId = null
                                selectedConnection = connection
                            },
                        )
                    }
                }
            }
            connectionNotice?.let { notice -> item {
                Text(
                    notice,
                    color = DSColors.deepGreen,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(DSColors.mistGreen).padding(12.dp),
                )
            } }
            item {
                V2InteractionConversationSection(
                    rows = mergedChats,
                    friendRequestCount = friendRequestCount,
                    onContacts = { showContacts = true },
                    onOpen = { selectedChat = it },
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Shield, null, tint = DSColors.shadowGray, modifier = Modifier.size(14.dp))
                    Text(
                        "每个空间独立可见，不会把一段关系中的交流带到另一段关系",
                        color = DSColors.shadowGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 5.dp),
                    )
                }
            }
          }
        }
    }

    if (showInvite) {
        V2StudentInviteAcceptanceDialog(
            pendingInvite = pendingInvite,
            onDismiss = {
                showInvite = false
                if (pendingInvite != null) onPendingInviteConsumed()
            },
            onAccepted = { connection ->
                showInvite = false
                if (pendingInvite != null) onPendingInviteConsumed()
                connections = listOf(connection) + connections.filterNot { it.connectionId == connection.connectionId }
                selectedConnection = connection
            },
        )
    }
    if (showConnectionOptions) {
        ConnectionChoiceDialog(
            onDismiss = { showConnectionOptions = false },
            onConnectCoach = {
                showConnectionOptions = false
                showInvite = true
            },
            onAddFriend = {
                showConnectionOptions = false
                showAddFriend = true
            },
        )
    }
    if (showAddFriend) {
        AddFriendDialog(
            currentUserId = session.userId,
            onDismiss = { showAddFriend = false },
            onCompleted = { outcome ->
                showAddFriend = false
                connectionNotice = when (outcome) {
                    V2FriendRequestOutcome.SENT -> "好友请求已发送；对方接受后就可以在下方聊天"
                    V2FriendRequestOutcome.ALREADY_FRIENDS -> "你们已经是好友，可以在下方开始聊天"
                    V2FriendRequestOutcome.ACCEPTED -> "好友申请已接受，可以开始聊天"
                }
                reload(holdEmptyResult = false)
            },
        )
    }
    if (showContacts) {
        V2InteractionContactsDialog(
            currentUserId = session.userId,
            onDismiss = {
                showContacts = false
                reload(holdEmptyResult = false)
            },
            onOpenChat = { conversation ->
                showContacts = false
                selectedChat = conversation
            },
            onChanged = { reload(holdEmptyResult = false) },
        )
    }
    if (showCoachOrder) {
        CoachStudentOrderDialog(
            connections = shownCoaches,
            coachPerspective = false,
            onDismiss = { showCoachOrder = false },
            onSave = { ordered ->
                saveInteractionCoachOrder(context, session.userId, ordered.map(V2CoachConnection::connectionId))
                coachOrderRevision += 1
                showCoachOrder = false
            },
        )
    }
}

@Composable
private fun InteractionCoachStatusCard(
    title: String,
    detail: String,
    action: String,
    onAction: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.94f))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(DSColors.mistGreen.copy(alpha = 0.70f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.WifiOff, null, tint = DSColors.shadowGray, modifier = Modifier.size(26.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = DSColors.deepGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(detail, color = DSColors.shadowGray, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        Button(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
            shape = RoundedCornerShape(23.dp),
        ) { Text(action, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
internal fun InteractionEmptyCoachCard(
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier.fillMaxWidth().clip(shape).background(Color.White.copy(alpha = 0.94f))
            .padding(horizontal = 18.dp, vertical = 16.dp).testTag("interaction-empty-coach-card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(DSColors.mistGreen.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Groups,
                contentDescription = null,
                tint = DSColors.fairway.copy(alpha = 0.72f),
                modifier = Modifier.size(28.dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "还没有连接教练",
                color = DSColors.deepGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                "连接教练之后，就能在这里进入教学空间。",
                color = DSColors.shadowGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = onConnect,
            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("interaction-empty-connect"),
            colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
            shape = RoundedCornerShape(23.dp),
        ) {
            Text("建立连接", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun InteractionCoachCardSection(
    connections: List<V2CoachConnection>,
    unreadByConnection: Map<String, Int>,
    latestItemsByConnection: Map<String, V2CoachWorkspaceItem>,
    onOrder: () -> Unit,
    onOpen: (V2CoachConnection) -> Unit,
    modifier: Modifier = Modifier,
    coachPerspective: Boolean = false,
    orderTitle: String = "教练顺序",
    relationshipLabel: String = "我的教练",
    feedbackLabel: String = "有新反馈",
    nameFallback: String = "挥呗教练",
    previewByConnection: ((V2CoachConnection) -> String)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag("interaction-coach-card-section"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (connections.size >= 2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onOrder)
                        .padding(horizontal = 4.dp, vertical = 5.dp).testTag("interaction-coach-order"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(Icons.Filled.SwapVert, contentDescription = null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
                    Text(orderTitle, color = DSColors.fairway, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        when (connections.size) {
            0 -> Unit
            1 -> {
                val connection = connections.first()
                InteractionCoachCard(
                    connection = connection,
                    unreadCount = unreadByConnection[connection.connectionId] ?: 0,
                    latestItem = latestItemsByConnection[connection.connectionId],
                    onClick = { onOpen(connection) },
                    coachPerspective = coachPerspective,
                    relationshipLabel = relationshipLabel,
                    feedbackLabel = feedbackLabel,
                    nameFallback = nameFallback,
                    previewOverride = previewByConnection?.invoke(connection),
                )
            }
            2 -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                connections.forEach { connection ->
                    InteractionCoachCard(
                        connection = connection,
                        unreadCount = unreadByConnection[connection.connectionId] ?: 0,
                        latestItem = latestItemsByConnection[connection.connectionId],
                        compact = true,
                        onClick = { onOpen(connection) },
                        modifier = Modifier.weight(1f),
                        coachPerspective = coachPerspective,
                        relationshipLabel = relationshipLabel,
                        feedbackLabel = feedbackLabel,
                        nameFallback = nameFallback,
                        previewOverride = previewByConnection?.invoke(connection),
                    )
                }
            }
            else -> BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cardWidth = maxWidth * 0.84f
                val listState = rememberLazyListState()
                val snapFlingBehavior = rememberSnapFlingBehavior(listState)
                val visibleIndex by remember {
                    derivedStateOf {
                        val layout = listState.layoutInfo
                        layout.visibleItemsInfo.maxByOrNull { item ->
                            val visibleStart = maxOf(item.offset, layout.viewportStartOffset)
                            val visibleEnd = minOf(item.offset + item.size, layout.viewportEndOffset)
                            (visibleEnd - visibleStart).coerceAtLeast(0)
                        }?.index ?: 0
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LazyRow(
                        state = listState,
                        flingBehavior = snapFlingBehavior,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("interaction-coach-carousel"),
                    ) {
                        itemsIndexed(connections, key = { _, connection -> connection.connectionId }) { _, connection ->
                            InteractionCoachCard(
                                connection = connection,
                                unreadCount = unreadByConnection[connection.connectionId] ?: 0,
                                latestItem = latestItemsByConnection[connection.connectionId],
                                onClick = { onOpen(connection) },
                                modifier = Modifier.width(cardWidth),
                                coachPerspective = coachPerspective,
                                relationshipLabel = relationshipLabel,
                                feedbackLabel = feedbackLabel,
                                nameFallback = nameFallback,
                                previewOverride = previewByConnection?.invoke(connection),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().testTag("interaction-coach-page-dots"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        connections.forEachIndexed { index, _ ->
                            Box(
                                Modifier.padding(horizontal = 3.dp).size(6.dp).clip(CircleShape)
                                    .background(if (index == visibleIndex) DSColors.fairway else DSColors.mistGreen)
                                    .semantics { selected = index == visibleIndex }
                                    .testTag("interaction-coach-page-dot-$index"),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun InteractionCoachCard(
    connection: V2CoachConnection,
    unreadCount: Int,
    latestItem: V2CoachWorkspaceItem?,
    onClick: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    coachPerspective: Boolean = false,
    relationshipLabel: String = "我的教练",
    feedbackLabel: String = "有新反馈",
    nameFallback: String = "挥呗教练",
    previewOverride: String? = null,
) {
    val person = if (coachPerspective) connection.student else connection.coach ?: connection.peer
    val name = person?.nickname ?: nameFallback
    val bio = person?.bio?.trim().orEmpty()
    val subtitle = if (bio.isEmpty()) relationshipLabel else "$relationshipLabel · $bio"
    val preview = previewOverride ?: if (latestItem == null) "点进教学空间，查看练习和反馈。"
    else latestSpacePreview(connection, latestItem)
    val focus = connection.resolvedTrainingFocus ?: "已连接"
    val goal = connection.primaryGoal?.trim().orEmpty()
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier.fillMaxWidth().clip(shape).background(Color.White.copy(alpha = 0.96f))
            .clickable(onClick = onClick).padding(if (compact) 12.dp else 16.dp)
            .testTag(if (coachPerspective) "coach-student-space-${connection.connectionId}" else "interaction-coach-card"),
        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(if (compact) 48.dp else 64.dp).clip(CircleShape)
                    .border(2.dp, DSColors.trophyGold, CircleShape).padding(2.dp),
            ) {
                V2UserAvatar(
                    avatar = person?.avatar,
                    modifier = Modifier.fillMaxSize(),
                    fallbackFontSize = if (compact) 17.sp else 22.sp,
                    placeholderIconSize = if (compact) 27.dp else 34.dp,
                    fallbackText = name,
                    fallbackColor = DSColors.sandBeige,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        name,
                        color = DSColors.deepGreen,
                        fontSize = if (compact) 15.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (unreadCount > 0) {
                        Text(feedbackLabel, color = DSColors.flagRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    subtitle,
                    color = DSColors.fairway,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!compact) {
                    Text(
                        preview,
                        color = DSColors.shadowGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text("›", color = DSColors.shadowGray, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InteractionCoachMetric(
                title = "当前重点",
                value = focus,
                icon = Icons.Filled.GpsFixed,
                modifier = Modifier.weight(1f),
            )
            if (goal.isNotEmpty()) {
                InteractionCoachMetric(
                    title = "当前目标",
                    value = goal,
                    icon = Icons.Filled.Flag,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun InteractionCoachMetric(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(DSColors.mistGreen.copy(alpha = 0.70f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = DSColors.fairway, modifier = Modifier.size(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text(
                value,
                color = DSColors.deepGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun V2InteractionConversationSection(
    rows: List<V2IMConversation>,
    friendRequestCount: Int,
    primaryBadge: String = "我的教练",
    emptyMessage: String = "还没有对话。添加好友，或和教练连接后就可以在这里聊天。",
    onContacts: () -> Unit,
    onOpen: (V2IMConversation) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("最近对话", color = DSColors.deepGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onContacts).padding(horizontal = 4.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("联系人", color = DSColors.fairway, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (friendRequestCount > 0) {
                    Text(
                        if (friendRequestCount > 99) "99+" else "$friendRequestCount",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.heightIn(min = 16.dp).widthIn(min = 16.dp)
                            .clip(RoundedCornerShape(9.dp)).background(DSColors.flagRed)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = DSColors.fairway,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
        if (rows.isEmpty()) {
            Text(
                emptyMessage,
                color = DSColors.shadowGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.94f)).padding(16.dp),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.96f)).padding(horizontal = 14.dp),
            ) {
                rows.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(row) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        V2UserAvatar(
                            row.avatar,
                            Modifier.size(46.dp),
                            fallbackText = row.title,
                            fallbackColor = DSColors.sandBeige,
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(row.title, color = DSColors.deepGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (row.isCoach) primaryBadge else "好友",
                                    color = if (row.isCoach) DSColors.fairway else Color(0xFFC4A35A),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                        .background(if (row.isCoach) DSColors.mistGreen else Color(0xFFF8F1DE))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                            Text(row.preview, color = DSColors.shadowGray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            imConversationTime(row.timeEpochSeconds)?.let {
                                Text(it, color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            if (row.unreadCount > 0) {
                                Text(
                                    if (row.unreadCount > 99) "99+" else "${row.unreadCount}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.heightIn(min = 18.dp).widthIn(min = 18.dp)
                                        .clip(RoundedCornerShape(10.dp)).background(DSColors.flagRed)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    if (index != rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(DSColors.borderLine))
                }
            }
        }
    }
}

private fun imConversationTime(epochSeconds: Long?): String? {
    epochSeconds ?: return null
    val date = java.time.Instant.ofEpochSecond(epochSeconds).atZone(java.time.ZoneId.systemDefault())
    val now = java.time.ZonedDateTime.now(date.zone)
    val time = date.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.CHINA))
    return when (date.toLocalDate()) {
        now.toLocalDate() -> "今天 $time"
        now.toLocalDate().minusDays(1) -> "昨天 $time"
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("M月d日", java.util.Locale.CHINA))
    }
}

@Composable
private fun V2StudentInteractionLegacyScreen(
    onOpenMemo: () -> Unit,
    onRequestLogin: () -> Unit,
    unread: V2StudentUnreadSnapshot = V2StudentUnreadSnapshot(),
    initialConnectionId: String? = null,
    initialConversationItemId: String? = null,
    onInitialRouteOpened: () -> Unit = {},
    pendingInvite: V2PendingCoachInvite? = null,
    onPendingInviteConsumed: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val deliveryState by ServiceLocator.coachDeliveryQueue.state.collectAsState()
    val session by ServiceLocator.session.state.collectAsState(initial = V2Session())
    var connections by remember { mutableStateOf<List<V2CoachConnection>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf(SpaceFilter.ALL) }
    var showInvite by remember { mutableStateOf(false) }
    var showConnectionOptions by remember { mutableStateOf(false) }
    var showAddFriend by remember { mutableStateOf(false) }
    var connectionNotice by remember { mutableStateOf<String?>(null) }
    var selectedConnection by remember { mutableStateOf<V2CoachConnection?>(null) }
    var routedItem by remember { mutableStateOf<V2CoachWorkspaceItem?>(null) }
    var routedItemId by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            val session = ServiceLocator.session.current()
            if (!session.isLoggedIn) {
                loading = false
                connections = emptyList()
                return@launch
            }
            loading = true
            error = null
            ServiceLocator.friendSpaceSyncRepo.syncOrLoadConnections()
                .onSuccess { connections = it }
                .onFailure { error = coachThrowableError("教学空间暂时无法同步，请稍后重试", it) }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }
    LaunchedEffect(pendingInvite) {
        if (pendingInvite != null) {
            selectedConnection = null
            showInvite = true
        }
    }
    LaunchedEffect(initialConnectionId, initialConversationItemId, connections) {
        val connectionId = initialConnectionId ?: return@LaunchedEffect
        val connection = connections.firstOrNull { it.connectionId == connectionId } ?: return@LaunchedEffect
        routedItemId = initialConversationItemId
        routedItem = unread.recentItems.firstOrNull { it.itemId == initialConversationItemId }
        selectedConnection = connection
        onInitialRouteOpened()
    }
    selectedConnection?.let { connection ->
        V2WorkspaceScreen(
            connection = connection,
            onOpenMemo = onOpenMemo,
            availableConnections = connections,
            switcherItems = unread.recentItems,
            onSwitchConnection = { next ->
                selectedConnection = next
                routedItem = null
                routedItemId = null
            },
            onConnectionUpdated = { updated ->
                connections = connections.map { current ->
                    if (current.connectionId == updated.connectionId) updated else current
                }
                selectedConnection = updated
            },
            onBack = {
                selectedConnection = null
                routedItem = null
                routedItemId = null
            },
            onDisconnected = {
                selectedConnection = null
                routedItem = null
                routedItemId = null
                reload()
            },
            initialItem = routedItem,
            initialItemId = routedItemId,
        )
        return
    }
    val filteredConnections = connections.filter {
        when (filter) {
            SpaceFilter.ALL -> true
            SpaceFilter.COACH -> it.relationshipType != "friend"
            SpaceFilter.FRIEND -> it.relationshipType == "friend"
        }
    }
    val shown = sortStudentSpaces(
        connections = filteredConnections.filter { it.status == "active" },
        unreadByConnection = unread.byConnection,
        latestActivityByConnection = latestStudentSpaceActivityByConnection(unread.recentItems),
    )
    val history = filteredConnections.filter { it.status == "disconnected" }
        .sortedByDescending { it.disconnectedAt ?: it.lastActivityAt.orEmpty() }
    val latestItemsByConnection = latestStudentSpaceItemsByConnection(unread.recentItems)

    Box(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        V2RoomBackground(Modifier.fillMaxSize())
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("挥呗空间", color = DSColors.deepGreen, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("和教练、好友一起练，也一起看", color = DSColors.shadowGray, fontSize = 13.sp)
                }
                Icon(
                    Icons.Filled.Refresh,
                    "刷新",
                    tint = DSColors.deepGreen,
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = ::reload).padding(9.dp),
                )
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(22.dp)).background(DSColors.fairway)
                        .clickable {
                            scope.launch {
                                if (ServiceLocator.session.current().isLoggedIn) showConnectionOptions = true else onRequestLogin()
                            }
                        }.padding(horizontal = 13.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(17.dp))
                    Text("连接", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Color.White)
                    .clickable(onClick = onOpenMemo).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = DSColors.fairway)
                }
                Column(Modifier.weight(1f)) {
                    Text("我的练习与学习记录", color = DSColors.deepGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("从已有记录选择内容，分别分享到不同空间", color = DSColors.shadowGray, fontSize = 11.sp)
                }
                Text("›", color = DSColors.fairway, fontSize = 24.sp)
            }
        }
        error?.let { message -> item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFEED7)).padding(12.dp)) {
                Text(message, color = Color(0xFF815728), fontSize = 12.sp)
            }
        } }
        deliveryState.message?.let { message -> item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DSColors.mistGreen).padding(12.dp)) {
                Text(message, color = DSColors.deepGreen, fontSize = 12.sp)
            }
        } }
        connectionNotice?.let { message -> item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DSColors.mistGreen).padding(12.dp)) {
                Text(message, color = DSColors.deepGreen, fontSize = 12.sp)
            }
        } }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFFECE9E1)).padding(3.dp)) {
                SpaceFilter.entries.forEach { option ->
                    Text(
                        option.label,
                        color = if (filter == option) Color.White else DSColors.shadowGray,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                            .background(if (filter == option) DSColors.fairway else Color.Transparent)
                            .clickable { filter = option }.padding(vertical = 9.dp),
                    )
                }
            }
        }
        when {
            loading -> item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = DSColors.fairway) } }
            shown.isEmpty() -> item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Filled.Link, null, tint = DSColors.fairway, modifier = Modifier.size(34.dp))
                    Text(
                        when (filter) {
                            SpaceFilter.FRIEND -> "还没有好友空间"
                            SpaceFilter.COACH -> "还没有连接教练"
                            SpaceFilter.ALL -> "还没有活跃空间"
                        },
                        color = DSColors.deepGreen,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (filter == SpaceFilter.FRIEND) "成为腾讯 IM 好友后，好友空间会自动同步"
                        else "输入教练发来的邀请码，建立你们的专属私密空间",
                        color = DSColors.shadowGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                    if (filter != SpaceFilter.FRIEND) {
                        Button(onClick = {
                            scope.launch { if (ServiceLocator.session.current().isLoggedIn) showInvite = true else onRequestLogin() }
                        }, colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway)) { Text("连接我的教练") }
                    }
                }
            }
            else -> {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("我的空间", color = DSColors.deepGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("共 ${shown.size} 个", color = DSColors.shadowGray, fontSize = 11.sp)
                    }
                }
                items(shown, key = { it.connectionId }) { connection ->
                    StudentSpaceCard(
                        connection = connection,
                        unreadCount = unread.byConnection[connection.connectionId] ?: 0,
                        latestItem = latestItemsByConnection[connection.connectionId],
                        onTogglePin = {
                            scope.launch {
                                ServiceLocator.coachWorkspaceRepo.updateSpacePinned(connection.connectionId, !connection.pinned)
                                    .onSuccess { updated ->
                                        connections = connections.map { if (it.connectionId == updated.connectionId) updated else it }
                                    }
                                    .onFailure { error = coachThrowableError("暂时无法更新置顶状态，请稍后重试", it) }
                            }
                        },
                        onClick = {
                            routedItem = null
                            routedItemId = null
                            selectedConnection = connection
                        },
                        onOpenUnread = {
                            val targetId = unread.unreadItemIdByConnection[connection.connectionId]
                                ?: latestItemsByConnection[connection.connectionId]?.itemId
                            routedItemId = targetId
                            routedItem = unread.recentItems.firstOrNull { it.itemId == targetId }
                            selectedConnection = connection
                        },
                    )
                }
            }
        }
        if (history.isNotEmpty()) {
            item { Text("历史空间", color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            items(history, key = { "history_${it.connectionId}" }) { connection ->
                HistoricalSpaceCard(connection, session.userId) {
                    routedItem = null
                    routedItemId = null
                    selectedConnection = connection
                }
            }
        }
        item {
            Text(
                "每个空间只对你和对方可见。解除连接后，历史交流仍可查看，但空间会变为只读。",
                color = DSColors.shadowGray,
                fontSize = 11.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
    }
    }

    if (showInvite) {
        V2StudentInviteAcceptanceDialog(
            pendingInvite = pendingInvite,
            onDismiss = {
                showInvite = false
                if (pendingInvite != null) onPendingInviteConsumed()
            },
            onAccepted = { connection ->
                showInvite = false
                if (pendingInvite != null) onPendingInviteConsumed()
                connections = listOf(connection) + connections.filterNot { it.connectionId == connection.connectionId }
                selectedConnection = connection
            },
        )
    }

    if (showConnectionOptions) {
        ConnectionChoiceDialog(
            onDismiss = { showConnectionOptions = false },
            onConnectCoach = {
                showConnectionOptions = false
                showInvite = true
            },
            onAddFriend = {
                showConnectionOptions = false
                showAddFriend = true
            },
        )
    }

    if (showAddFriend) {
        AddFriendDialog(
            currentUserId = session.userId,
            onDismiss = { showAddFriend = false },
            onCompleted = { outcome ->
                showAddFriend = false
                connectionNotice = when (outcome) {
                    V2FriendRequestOutcome.SENT -> "好友请求已发送；对方接受后会自动建立独立空间"
                    V2FriendRequestOutcome.ALREADY_FRIENDS -> "你们已经是好友，正在同步好友空间"
                    V2FriendRequestOutcome.ACCEPTED -> "好友申请已接受，正在建立独立空间"
                }
                reload()
            },
        )
    }

}

@Composable
private fun ConnectionChoiceDialog(
    onDismiss: () -> Unit,
    onConnectCoach: () -> Unit,
    onAddFriend: () -> Unit,
) {
    V2ConnectionChoiceSheet(
        primaryActionTitle = "连接教练",
        message = "连接教练会进入教学空间。添加好友后可以在下方聊天。",
        onPrimaryAction = onConnectCoach,
        onAddFriend = onAddFriend,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun V2ConnectionChoiceSheet(
    primaryActionTitle: String,
    message: String,
    onPrimaryAction: () -> Unit,
    onAddFriend: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().testTag("connection-choice-sheet"),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().v2NavigationBarPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 13.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "建立新的联系",
                            color = DSColors.shadowGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            message,
                            color = DSColors.shadowGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(DSColors.borderLine))
                    Text(
                        primaryActionTitle,
                        color = DSColors.fairway,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                            .clickable(onClick = onPrimaryAction).padding(vertical = 12.dp)
                            .testTag("connection-choice-primary"),
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(DSColors.borderLine))
                    Text(
                        "添加好友",
                        color = DSColors.fairway,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                            .clickable(onClick = onAddFriend).padding(vertical = 12.dp)
                            .testTag("connection-choice-add-friend"),
                    )
                }
                Text(
                    "取消",
                    color = DSColors.fairway,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color.White).clickable(onClick = onDismiss).padding(vertical = 12.dp)
                        .testTag("connection-choice-cancel"),
                )
            }
        }
    }
}

@Composable
internal fun AddFriendDialog(
    currentUserId: String?,
    initialScannedUserId: String? = null,
    onDismiss: () -> Unit,
    onCompleted: (V2FriendRequestOutcome) -> Unit,
    onOpenExistingFriend: ((V2FriendSearchUser) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var keyword by rememberSaveable { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<V2FriendSearchUser?>(null) }
    var friendIds by remember(currentUserId) { mutableStateOf<Set<String>>(emptySet()) }
    var message by remember { mutableStateOf<String?>(null) }
    var showQrScanner by remember { mutableStateOf(false) }

    suspend fun renewTencentSession(): V2Session {
        ServiceLocator.authRepo.refreshUserSig().getOrThrow()
        return ServiceLocator.session.current()
    }

    LaunchedEffect(Unit) {
        ServiceLocator.tencentFriendRepo.friendUserIds(
            session = ServiceLocator.session.current(),
            renewExpiredSession = ::renewTencentSession,
        ).onSuccess { friendIds = it.toSet() }
    }

    fun search() {
        if (searching || sending) return
        val value = keyword.trim()
        if (value.isEmpty()) {
            message = "请输入对方的用户名或手机号"
            return
        }
        searching = true
        result = null
        message = null
        scope.launch {
            ServiceLocator.authRepo.searchFriend(value)
                .onSuccess { user ->
                    result = user
                    message = if (user == null) "未找到该用户" else null
                }
                .onFailure { message = coachThrowableError("搜索失败，请稍后重试", it) }
            searching = false
        }
    }

    fun searchScannedUser(userId: String) {
        if (searching || sending) return
        if (userId == currentUserId) {
            result = null
            message = "不能添加自己为好友"
            return
        }
        searching = true
        result = null
        message = null
        scope.launch {
            ServiceLocator.authRepo.friendPublicInfo(userId)
                .onSuccess { user -> result = user }
                .onFailure {
                    // Match iOS: a valid Huibei QR still exposes its embedded user id
                    // when public-profile lookup is temporarily unavailable.
                    result = V2FriendSearchUser(
                        userId = userId,
                        username = userId,
                        nickname = null,
                        avatar = null,
                    )
                    message = "已读取二维码；公开资料暂时无法加载，可继续发送好友请求"
                }
            searching = false
        }
    }

    LaunchedEffect(initialScannedUserId) {
        initialScannedUserId?.takeIf { it.isNotBlank() }?.let(::searchScannedUser)
    }

    fun send(user: V2FriendSearchUser) {
        if (sending || user.userId == currentUserId) return
        sending = true
        message = null
        scope.launch {
            ServiceLocator.tencentFriendRepo.sendFriendRequest(
                session = ServiceLocator.session.current(),
                targetUserId = user.userId,
                renewExpiredSession = ::renewTencentSession,
            ).onSuccess { outcome ->
                if (outcome == V2FriendRequestOutcome.ALREADY_FRIENDS) {
                    friendIds = friendIds + user.userId
                } else {
                    onCompleted(outcome)
                }
            }
                .onFailure { message = coachThrowableError("好友请求发送失败，请稍后重试", it) }
            sending = false
        }
    }

    if (showQrScanner) {
        V2FriendQrScannerDialog(
            onDismiss = { showQrScanner = false },
            onScanned = { userId ->
                showQrScanner = false
                searchScannedUser(userId)
            },
        )
    }

    Dialog(
        onDismissRequest = { if (!sending) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        V2RoomPage(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxWidth().v2StatusBarPadding().height(DSLayout.topBarHeight)
                        .testTag("add-friend-top-bar")
                        .background(DSColors.warmWhite.copy(alpha = 0.94f)),
                ) {
                    Box(
                        Modifier.align(Alignment.CenterStart).size(44.dp).clickable(
                            enabled = !sending,
                            onClick = onDismiss,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, "关闭添加好友", tint = DSColors.inkGreen, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        "添加好友",
                        color = DSColors.inkGreen,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).v2NavigationBarPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White)
                            .border(1.dp, DSColors.borderLine, RoundedCornerShape(12.dp))
                            .clickable(enabled = !searching && !sending) { showQrScanner = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(DSColors.mistGreen),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, null, tint = DSColors.fairway, modifier = Modifier.size(22.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("扫一扫", color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("扫描二维码添加好友", color = DSColors.shadowGray, fontSize = 12.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = DSColors.shadowGray, modifier = Modifier.size(18.dp))
                    }

                    Box(Modifier.fillMaxWidth().height(1.dp).background(DSColors.borderLine))
                    Text("输入对方的用户名或手机号", color = DSColors.shadowGray, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Person, null, tint = DSColors.shadowGray, modifier = Modifier.size(15.dp))
                            Text("用户名", color = DSColors.shadowGray, fontSize = 12.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Phone, null, tint = DSColors.shadowGray, modifier = Modifier.size(15.dp))
                            Text("手机号", color = DSColors.shadowGray, fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = keyword,
                        onValueChange = {
                            keyword = it
                            result = null
                            message = null
                        },
                        enabled = !searching && !sending,
                        singleLine = true,
                        placeholder = { Text("用户名 / 手机号", color = DSColors.shadowGray) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = DSColors.shadowGray) },
                        trailingIcon = if (keyword.isNotEmpty()) {
                            {
                                Icon(
                                    Icons.Filled.Cancel,
                                    "清空搜索内容",
                                    tint = DSColors.shadowGray,
                                    modifier = Modifier.clickable {
                                        keyword = ""
                                        result = null
                                        message = null
                                    },
                                )
                            }
                        } else null,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White.copy(alpha = 0.72f),
                            focusedIndicatorColor = DSColors.fairway,
                            unfocusedIndicatorColor = DSColors.borderLine,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = ::search,
                        enabled = keyword.isNotBlank() && !searching && !sending,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DSColors.fairway,
                            disabledContainerColor = DSColors.fairway.copy(alpha = 0.50f),
                            disabledContentColor = Color.White,
                        ),
                    ) {
                        if (searching) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Search, null, modifier = Modifier.size(19.dp))
                            Text("搜索", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                        }
                    }

                    result?.let { user ->
                        val alreadyFriend = user.userId in friendIds
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            V2UserAvatar(
                                user.avatar,
                                Modifier.size(48.dp),
                                fallbackFontSize = 17.sp,
                                fallbackText = user.displayName,
                                fallbackColor = DSColors.sandBeige,
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(user.displayName, color = DSColors.inkGreen, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                    if (alreadyFriend) {
                                        Text(
                                            "已是好友",
                                            color = DSColors.fairway,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.clip(CircleShape).background(DSColors.mistGreen)
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                                Text("用户名: ${user.username}", color = DSColors.shadowGray, fontSize = 13.sp)
                            }
                            if (alreadyFriend && onOpenExistingFriend != null) {
                                Box(
                                    Modifier.size(44.dp).clip(CircleShape).background(DSColors.mistGreen)
                                        .clickable { onOpenExistingFriend(user) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.ChatBubble, "开始聊天", tint = DSColors.fairway, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        if (!alreadyFriend) {
                            Button(
                                onClick = { send(user) },
                                enabled = !sending && user.userId != currentUserId,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                            ) {
                                if (sending) {
                                    CircularProgressIndicator(Modifier.size(17.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(19.dp))
                                }
                                Text(
                                    if (user.userId == currentUserId) "当前账号" else if (sending) "发送中..." else "添加好友",
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    } ?: run {
                        if (!searching && message == null) {
                            Column(
                                Modifier.fillMaxWidth().padding(top = 34.dp, bottom = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(Modifier.size(64.dp).clip(CircleShape).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Search, null, tint = DSColors.fairway, modifier = Modifier.size(30.dp))
                                }
                                Text("搜索好友开始聊天", color = DSColors.shadowGray, fontSize = 14.sp)
                            }
                        }
                    }
                    message?.let { Text(it, color = Color(0xFFB1443A), fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
private fun StudentSpaceCard(
    connection: V2CoachConnection,
    unreadCount: Int,
    latestItem: V2CoachWorkspaceItem?,
    onTogglePin: () -> Unit,
    onClick: () -> Unit,
    onOpenUnread: () -> Unit,
    hasDraft: Boolean = false,
    onOpenMemo: () -> Unit = {},
) {
    if (latestItem == null) {
        StudentSpaceEmptySummaryCard(
            connection = connection,
            hasDraft = hasDraft,
            onTogglePin = onTogglePin,
            onOpenSpace = onClick,
            onOpenMemo = onOpenMemo,
        )
        return
    }
    val person = connection.coach ?: connection.peer
    val hasVideo = latestItem?.latestActivityHasVideoContent == true
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
            .border(
                width = if (unreadCount > 0) 1.3.dp else 1.dp,
                color = if (unreadCount > 0) DSColors.trophyGold.copy(alpha = 0.55f) else Color(0xFFE4E2DC),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        V2UserAvatar(
            avatar = person?.avatar,
            modifier = Modifier.size(52.dp),
            fallbackFontSize = 20.sp,
            placeholderIconSize = 30.dp,
            fallbackText = person?.nickname
                ?: if (connection.relationshipType == "friend") "挥呗好友" else "挥呗教练",
            fallbackColor = DSColors.sandBeige,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    person?.nickname ?: if (connection.relationshipType == "friend") "挥呗好友" else "挥呗教练",
                    color = DSColors.deepGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    if (connection.relationshipType == "friend") "好友" else "教练",
                    color = if (connection.relationshipType == "friend") DSColors.fairway else DSColors.trophyGold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).background(DSColors.mistGreen).padding(horizontal = 7.dp, vertical = 3.dp),
                )
                if (connection.pinned) {
                    Icon(Icons.Filled.PushPin, contentDescription = "已置顶", tint = DSColors.shadowGray, modifier = Modifier.size(12.dp))
                }
                Text(
                    spaceActivityText(latestItem, connection),
                    color = DSColors.shadowGray,
                    fontSize = 9.sp,
                    maxLines = 1,
                )
            }
            Text(
                latestSpaceTitle(latestItem),
                color = DSColors.deepGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                latestSpacePreview(connection, latestItem),
                color = DSColors.shadowGray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (unreadCount > 0) {
                Text(
                    "$unreadCount 条新互动  →",
                    color = Color(0xFFD95C4F),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenUnread)
                        .padding(horizontal = 2.dp, vertical = 5.dp),
                )
            }
        }
        if (hasVideo) {
            Box(
                modifier = Modifier.size(width = 62.dp, height = 54.dp).clip(RoundedCornerShape(12.dp))
                    .background(DSColors.deepGreen),
                contentAlignment = Alignment.Center,
            ) {
                latestItem?.latestActivityCoverUrl?.let { cover ->
                    AsyncImage(
                        model = cover,
                        contentDescription = "最近视频封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Text("▶", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text("›", color = DSColors.fairway, fontSize = 24.sp)
        }
        Icon(
            Icons.Filled.PushPin,
            if (connection.pinned) "取消置顶" else "置顶空间",
            tint = if (connection.pinned) DSColors.trophyGold else DSColors.shadowGray.copy(alpha = 0.55f),
            modifier = Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onTogglePin).padding(7.dp),
        )
    }
}

@Composable
internal fun StudentSpaceEmptySummaryCard(
    connection: V2CoachConnection,
    hasDraft: Boolean,
    onTogglePin: () -> Unit,
    onOpenSpace: () -> Unit,
    onOpenMemo: () -> Unit,
) {
    val person = connection.coach ?: connection.peer
    val displayName = person?.nickname
        ?: if (connection.relationshipType == "friend") "挥呗好友" else "挥呗教练"
    val cardShape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier.fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = cardShape,
                ambientColor = DSColors.deepGreen.copy(alpha = 0.05f),
                spotColor = DSColors.deepGreen.copy(alpha = 0.05f),
            )
            .clip(cardShape)
            .background(Color.White.copy(alpha = 0.96f))
            .padding(14.dp)
            .testTag("student-space-empty-summary"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenSpace).padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box {
                V2UserAvatar(
                    avatar = person?.avatar,
                    modifier = Modifier.size(48.dp),
                    fallbackFontSize = 18.sp,
                    placeholderIconSize = 28.dp,
                    fallbackText = displayName,
                    fallbackColor = DSColors.sandBeige,
                )
                Box(
                    Modifier.align(Alignment.BottomEnd).size(10.dp).clip(CircleShape)
                        .background(DSColors.fairway).border(2.dp, Color.White, CircleShape),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        displayName,
                        color = DSColors.deepGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (connection.relationshipType == "friend") "好友" else "教练",
                        color = if (connection.relationshipType == "friend") DSColors.fairway else DSColors.trophyGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(CircleShape).background(DSColors.mistGreen)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                    if (connection.pinned) {
                        Icon(Icons.Filled.PushPin, contentDescription = "已置顶", tint = DSColors.shadowGray, modifier = Modifier.size(12.dp))
                    }
                }
                Text(
                    if (connection.relationshipType == "friend") "一起分享练习" else "已连接",
                    color = DSColors.shadowGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            Icon(
                Icons.Filled.PushPin,
                if (connection.pinned) "取消置顶" else "置顶空间",
                tint = if (connection.pinned) DSColors.trophyGold else DSColors.shadowGray.copy(alpha = 0.55f),
                modifier = Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onTogglePin).padding(8.dp),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.golf_ball_icon),
                contentDescription = null,
                tint = DSColors.fairway.copy(alpha = 0.85f),
                modifier = Modifier.size(42.dp),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("还没有最近对话", color = DSColors.deepGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (hasDraft) "有未发送内容，点进去可以继续。"
                    else "先发一段练习，之后这里会显示你们最近的交流。",
                    color = DSColors.shadowGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
            Button(
                onClick = onOpenSpace,
                modifier = Modifier.fillMaxWidth().height(40.dp).testTag("student-space-empty-primary"),
                colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                shape = RoundedCornerShape(23.dp),
            ) {
                Text(
                    if (hasDraft) "继续未发送内容" else "发练习内容",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                "先记录自己的练习",
                color = DSColors.fairway,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpenMemo)
                    .padding(horizontal = 12.dp, vertical = 7.dp).testTag("student-space-empty-memo"),
            )
        }
    }
}

private fun latestSpaceTitle(item: V2CoachWorkspaceItem?): String {
    if (item == null) return "分享第一条练习或学习内容"
    V2CoachWorkspaceItem.visibleCopy(item.title)?.let { return it }
    return if (item.resolvedKind == "coach_post") "最近分享的参考内容" else "最近分享的练习"
}

private fun latestSpacePreview(connection: V2CoachConnection, item: V2CoachWorkspaceItem?): String {
    if (item == null) {
        return if (connection.relationshipType == "friend") {
            connection.peer?.bio?.trim()?.takeIf(String::isNotEmpty) ?: "一起分享练习与学习内容"
        } else {
            "进入空间开始交流"
        }
    }
    V2CoachWorkspaceItem.visibleCopy(item.latestReplyText)?.let { reply ->
        if (connection.relationshipType == "friend") return "最近交流：$reply"
        val person = connection.coach ?: connection.peer
        val speaker = if (item.latestReplySenderRole == "student" && item.isMine) "你"
        else person?.nickname ?: "对方"
        return "$speaker：$reply"
    }
    V2CoachWorkspaceItem.visibleCopy(item.text)?.let { return it }
    return if (item.hasVideoContent) "视频内容" else "进入查看交流"
}

private fun spaceActivityText(item: V2CoachWorkspaceItem?, connection: V2CoachConnection): String {
    val raw = item?.latestReplyAt ?: item?.latestActivityAt ?: item?.createdAt
        ?: connection.lastActivityAt ?: connection.acceptedAt ?: connection.createdAt
        ?: return ""
    val instant = runCatching { java.time.OffsetDateTime.parse(raw).toInstant() }
        .recoverCatching { java.time.Instant.parse(raw) }
        .getOrNull() ?: return ""
    val now = java.time.ZonedDateTime.now()
    val date = instant.atZone(now.zone)
    val seconds = java.time.Duration.between(instant, now.toInstant()).seconds
    return when {
        seconds in 0L..<60L -> "刚刚"
        seconds in 60L..<3_600L -> "${(seconds / 60).coerceAtLeast(1)}分钟前"
        date.toLocalDate() == now.toLocalDate() -> date.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("M月d日"))
    }
}

@Composable
private fun HistoricalSpaceCard(connection: V2CoachConnection, currentUserId: String?, onClick: () -> Unit) {
    val person = connection.coach ?: connection.peer
    val actor = when (connection.disconnectedByUserId) {
        null -> null
        currentUserId -> "你"
        else -> if (connection.relationshipType == "friend") "对方" else "教练"
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
            .clickable(onClick = onClick).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        V2UserAvatar(
            avatar = person?.avatar,
            modifier = Modifier.size(42.dp),
            fallbackFontSize = 16.sp,
            placeholderIconSize = 26.dp,
            fallbackText = person?.nickname
                ?: if (connection.relationshipType == "friend") "挥呗好友" else "挥呗教练",
            fallbackColor = DSColors.sandBeige,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                person?.nickname ?: if (connection.relationshipType == "friend") "挥呗好友" else "挥呗教练",
                color = DSColors.deepGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                actor?.let { "${it}已解除连接 · 历史内容只读" } ?: "连接已解除 · 历史内容只读",
                color = DSColors.shadowGray,
                fontSize = 10.sp,
            )
        }
        Text("›", color = DSColors.shadowGray, fontSize = 22.sp)
    }
}

internal fun workspaceDisconnectDialogTitle(friendSpace: Boolean, coachPerspective: Boolean): String = when {
    friendSpace -> "解除好友空间？"
    coachPerspective -> "解除学员连接？"
    else -> "解除与教练的连接？"
}

internal fun workspaceDisconnectDialogMessage(hasConnectionDrafts: Boolean): String =
    if (hasConnectionDrafts) {
        "已有交流会继续保留为只读；当前空间里的未发送草稿会被删除。"
    } else {
        "解除后双方不能再发送新内容，已有交流会继续保留为只读。"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2WorkspaceDisconnectConfirmationSheet(
    title: String,
    message: String,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    V2WorkspaceConfirmationSheet(
        title = title,
        message = message,
        confirmLabel = "解除连接",
        busyLabel = "处理中…",
        cancelLabel = "取消",
        busy = busy,
        testTag = "workspace-disconnect-confirmation-sheet",
        confirmTestTag = "workspace-disconnect-confirm",
        cancelTestTag = "workspace-disconnect-cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2WorkspaceConfirmationSheet(
    title: String,
    message: String,
    confirmLabel: String,
    busyLabel: String = confirmLabel,
    cancelLabel: String,
    busy: Boolean,
    testTag: String,
    confirmTestTag: String = "$testTag-confirm",
    cancelTestTag: String = "$testTag-cancel",
    destructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.34f),
        dragHandle = null,
        tonalElevation = 0.dp,
        modifier = Modifier.testTag(testTag),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().v2NavigationBarPadding()
                .padding(horizontal = 12.dp).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color.White),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        title,
                        color = Color(0xFF6B7280),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        message,
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E7EB)))
                Box(
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                        .clickable(enabled = !busy, onClick = onConfirm)
                        .testTag(confirmTestTag),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (busy) busyLabel else confirmLabel,
                        color = if (destructive) DSColors.flagRed else DSColors.fairway,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Color.White)
                    .clickable(enabled = !busy, onClick = onDismiss)
                    .testTag(cancelTestTag),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    cancelLabel,
                    color = DSColors.fairway,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun WorkspaceStatusStack(
    successMessage: String?,
    errorMessage: String?,
    hasRecoverableDraft: Boolean,
    busy: Boolean,
    onContinueSend: () -> Unit,
    onRetrySync: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (successMessage == null && errorMessage == null) return
    Column(
        modifier = modifier.fillMaxWidth().testTag("workspace-status-stack"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        successMessage?.let { message ->
            Row(
                modifier = Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(14.dp))
                    .background(DSColors.mistGreen.copy(alpha = 0.72f)).padding(horizontal = 13.dp)
                    .testTag("workspace-success-banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = DSColors.deepGreen, modifier = Modifier.size(16.dp))
                Text(message, color = DSColors.deepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        errorMessage?.let { message ->
            val contentSyncError = workspaceErrorIsContentSync(message)
            val recoverableDelivery = !contentSyncError && hasRecoverableDraft
            val accent = if (recoverableDelivery) DSColors.trophyGold else DSColors.flagRed
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(
                        if (recoverableDelivery) DSColors.sandBeige.copy(alpha = 0.58f)
                        else DSColors.mistGreen.copy(alpha = 0.58f),
                    )
                    .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                    .padding(11.dp).testTag("workspace-error-banner"),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    if (recoverableDelivery) Icons.Filled.Refresh
                    else if (contentSyncError) Icons.Filled.WifiOff else Icons.Filled.Cancel,
                    null,
                    tint = accent,
                    modifier = Modifier.padding(top = 1.dp).size(16.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        when {
                            recoverableDelivery -> "发送暂停，内容已保留"
                            contentSyncError -> "内容暂时没有同步成功"
                            else -> "这次操作没有完成"
                        },
                        color = DSColors.inkGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        message,
                        color = DSColors.shadowGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                when {
                    recoverableDelivery -> Text(
                        "继续发送",
                        color = DSColors.fairway,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.height(30.dp).clip(CircleShape)
                            .background(DSColors.cardWhite.copy(alpha = 0.92f))
                            .clickable(enabled = !busy, onClick = onContinueSend)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("workspace-error-continue"),
                    )
                    contentSyncError -> Text(
                        "重新同步",
                        color = DSColors.fairway,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = !busy, onClick = onRetrySync)
                            .padding(horizontal = 4.dp, vertical = 7.dp)
                            .testTag("workspace-error-retry"),
                    )
                    else -> Box(
                        Modifier.size(28.dp).clip(CircleShape).background(DSColors.cardWhite.copy(alpha = 0.92f))
                            .clickable(enabled = !busy, onClick = onDismissError)
                            .testTag("workspace-error-dismiss"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, "关闭提示", tint = DSColors.shadowGray, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun WorkspaceContentToolbar(
    itemCount: Int,
    newestFirst: Boolean,
    compact: Boolean,
    onSelectNewestFirst: (Boolean) -> Unit,
    onToggleLayout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth().testTag("workspace-content-toolbar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("练习内容", color = DSColors.inkGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (itemCount > 0) {
            Text("${itemCount}条", color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.weight(1f))
        Box {
            Row(
                modifier = Modifier.height(32.dp).clip(RoundedCornerShape(10.dp))
                    .clickable { sortMenuExpanded = true }.padding(horizontal = 5.dp)
                    .testTag("workspace-content-sort"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Filled.FilterList, null, tint = DSColors.shadowGray, modifier = Modifier.size(13.dp))
                Text(
                    if (newestFirst) "按时间" else "最早",
                    color = DSColors.shadowGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                listOf(true to "最新在前", false to "最早在前").forEach { (candidateNewest, title) ->
                    DropdownMenuItem(
                        text = { Text(title) },
                        leadingIcon = {
                            Icon(
                                if (newestFirst == candidateNewest) Icons.Filled.Check else Icons.Filled.Schedule,
                                null,
                                tint = if (newestFirst == candidateNewest) DSColors.fairway else DSColors.shadowGray,
                            )
                        },
                        onClick = {
                            sortMenuExpanded = false
                            onSelectNewestFirst(candidateNewest)
                        },
                    )
                }
            }
        }
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape)
                .background(DSColors.mistGreen.copy(alpha = 0.80f))
                .clickable(onClick = onToggleLayout)
                .semantics {
                    contentDescription = if (compact) "当前为紧凑列表，点击切换视角"
                    else "当前为舒展卡片，点击切换视角"
                }
                .testTag("workspace-content-layout"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (compact) Icons.AutoMirrored.Filled.FormatListBulleted else Icons.Filled.ViewAgenda,
                null,
                tint = DSColors.fairway,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
internal fun WorkspaceReadOnlyBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.55f)).padding(14.dp)
            .testTag("workspace-read-only-banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Lock, null, tint = DSColors.shadowGray, modifier = Modifier.size(15.dp))
        Text(
            text,
            color = DSColors.shadowGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CoachCompactSpaceTopBar(
    title: String,
    detail: String?,
    avatar: String?,
    active: Boolean,
    busy: Boolean,
    pinned: Boolean,
    onBack: () -> Unit,
    onSwitchSpace: () -> Unit,
    onEditProfile: (() -> Unit)?,
    onTogglePinned: () -> Unit,
    onDisconnect: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).testTag("workspace-top-bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.88f))
                .clickable(onClick = onBack).testTag("workspace-top-back"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = CoachGreen, modifier = Modifier.size(18.dp))
        }
        V2UserAvatar(avatar, Modifier.size(36.dp), fallbackText = title, fallbackFontSize = 14.sp)
        Column(Modifier.weight(1f)) {
            Text(title, color = CoachGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            detail?.let {
                Text(it, color = DSColors.shadowGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            "切换学员",
            color = CoachGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.9f))
                .clickable(enabled = !busy, onClick = onSwitchSpace)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("workspace-top-switcher"),
        )
        if (active) {
            Box {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f))
                        .clickable(enabled = !busy) { menuExpanded = true }.testTag("workspace-top-more"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MoreHoriz, "更多", tint = CoachGreen, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    onEditProfile?.let { editProfile ->
                        DropdownMenuItem(
                            text = { Text("编辑教学档案") },
                            onClick = { menuExpanded = false; editProfile() },
                        )
                        DropdownMenuItem(
                            text = { Text(if (pinned) "取消重点学员" else "设为重点学员") },
                            onClick = { menuExpanded = false; onTogglePinned() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("解除连接", color = DSColors.flagRed) },
                        onClick = { menuExpanded = false; onDisconnect() },
                    )
                }
            }
        }
    }
}

@Composable
internal fun WorkspaceTopBar(
    title: String,
    spaceType: String,
    active: Boolean,
    busy: Boolean,
    pinned: Boolean,
    onBack: () -> Unit,
    onSwitchSpace: () -> Unit,
    onEditProfile: (() -> Unit)?,
    onTogglePinned: () -> Unit,
    onDisconnect: () -> Unit,
    coachCompact: Boolean = false,
    avatar: String? = null,
    detail: String? = null,
) {
    if (coachCompact) {
        CoachCompactSpaceTopBar(
            title = title,
            detail = detail,
            avatar = avatar,
            active = active,
            busy = busy,
            pinned = pinned,
            onBack = onBack,
            onSwitchSpace = onSwitchSpace,
            onEditProfile = onEditProfile,
            onTogglePinned = onTogglePinned,
            onDisconnect = onDisconnect,
        )
        return
    }
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = DSLayout.pageHorizontal)
            .padding(top = DSLayout.headerContentTop, bottom = DSLayout.headerContentBottom)
            .testTag("workspace-top-bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(DSLayout.iconButton).clip(CircleShape).background(DSColors.cardWhite)
                .clickable(onClick = onBack).testTag("workspace-top-back"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                "返回",
                tint = DSColors.inkGreen,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                .clickable(enabled = !busy, onClick = onSwitchSpace)
                .semantics { contentDescription = "切换挥呗空间" }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("workspace-top-switcher"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    title,
                    color = DSColors.inkGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    null,
                    tint = DSColors.fairway,
                    modifier = Modifier.size(13.dp),
                )
            }
            Text(
                spaceType,
                color = DSColors.shadowGray,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.weight(1f))

        if (active) {
            Box {
                Box(
                    Modifier.size(DSLayout.iconButton).clip(CircleShape).background(DSColors.cardWhite)
                        .clickable(enabled = !busy) { menuExpanded = true }
                        .testTag("workspace-top-more"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.MoreHoriz,
                        "更多空间操作",
                        tint = DSColors.inkGreen,
                        modifier = Modifier.size(22.dp),
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    onEditProfile?.let { editProfile ->
                        DropdownMenuItem(
                            text = { Text("编辑教学档案") },
                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                            onClick = {
                                menuExpanded = false
                                editProfile()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (pinned) "取消重点学员" else "设为重点学员") },
                            leadingIcon = { Icon(Icons.Filled.PushPin, null) },
                            onClick = {
                                menuExpanded = false
                                onTogglePinned()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("解除连接", color = DSColors.flagRed) },
                        leadingIcon = { Icon(Icons.Filled.LinkOff, null, tint = DSColors.flagRed) },
                        onClick = {
                            menuExpanded = false
                            onDisconnect()
                        },
                    )
                }
            }
        } else {
            Spacer(Modifier.size(40.dp))
        }
    }
}

@Composable
internal fun WorkspaceStudentOverviewCard(
    connection: V2CoachConnection,
    coachPerspective: Boolean,
    onEditProfile: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val person = if (coachPerspective) connection.student else connection.coach ?: connection.peer
    val name = person?.nickname?.trim().orEmpty().ifBlank {
        if (connection.relationshipType == "friend") "好友空间"
        else if (coachPerspective) "学员空间" else "教练空间"
    }
    val active = connection.status == "active"
    val age = V2CoachStudentAge.entries.firstOrNull { it.wireValue == connection.ageTag }?.title
    val relationshipText = if (connection.relationshipType == "friend") {
        person?.bio?.trim().orEmpty().ifBlank { "好友空间 · 一起练习与分享" }
    } else {
        buildString {
            append(if (coachPerspective) "一对一教学空间" else "与认证教练的私有交流")
            append(" · ")
            append(connection.resolvedLessonMode.title)
            age?.let { append(" · $it") }
        }
    }
    val focus = connection.resolvedTrainingFocus?.trim().orEmpty()
        .ifBlank { connection.trainingStage?.trim().orEmpty() }
    val goal = connection.trainingGoalTags.firstNotNullOfOrNull { wireValue ->
        V2CoachTrainingGoal.entries.firstOrNull { it.wireValue == wireValue }?.title
    } ?: connection.primaryGoal?.trim().orEmpty()
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier.fillMaxWidth().shadow(16.dp, shape, ambientColor = DSColors.deepGreen.copy(alpha = 0.10f),
            spotColor = DSColors.deepGreen.copy(alpha = 0.10f))
            .clip(shape).background(DSColors.cardWhite.copy(alpha = 0.82f)).padding(16.dp)
            .testTag("workspace-student-overview"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box {
                Box(
                    modifier = Modifier.size(62.dp).clip(CircleShape)
                        .border(2.dp, if (active) DSColors.fairway else DSColors.shadowGray, CircleShape)
                        .padding(3.dp),
                ) {
                    V2UserAvatar(
                        avatar = person?.avatar,
                        fallbackText = name,
                        fallbackColor = DSColors.sandBeige,
                        fallbackFontSize = 21.sp,
                        placeholderIconSize = 33.dp,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).size(14.dp).clip(CircleShape)
                        .background(if (active) DSColors.fairway else DSColors.shadowGray)
                        .border(2.5.dp, DSColors.cardWhite, CircleShape),
                )
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        name,
                        color = DSColors.inkGreen,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!active) {
                        Text(
                            "历史空间",
                            color = DSColors.shadowGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(CircleShape).background(DSColors.cardWhite.copy(alpha = 0.78f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                }
                Text(
                    relationshipText,
                    color = DSColors.shadowGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (coachPerspective && active && onEditProfile != null) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(DSColors.cardWhite.copy(alpha = 0.80f))
                        .clickable(onClick = onEditProfile).testTag("workspace-overview-edit"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Edit, "编辑教学档案", tint = DSColors.fairway, modifier = Modifier.size(15.dp))
                }
            }
        }

        if (connection.relationshipType != "friend" && (focus.isNotBlank() || goal.isNotBlank())) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (focus.isNotBlank()) {
                    WorkspaceOverviewPill(
                        title = "当前重点",
                        value = focus,
                        icon = Icons.Filled.GpsFixed,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (goal.isNotBlank()) {
                    WorkspaceOverviewPill(
                        title = "当前目标",
                        value = goal,
                        icon = Icons.Filled.Flag,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceOverviewPill(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(44.dp).clip(CircleShape).background(DSColors.cardWhite)
            .border(1.dp, DSColors.listCardBorder, CircleShape).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(11.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, color = DSColors.shadowGray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            Text(
                value,
                color = DSColors.inkGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2WorkspaceScreen(
    connection: V2CoachConnection,
    onBack: () -> Unit,
    onOpenMemo: () -> Unit = {},
    onDisconnected: () -> Unit = onBack,
    coachPerspective: Boolean = false,
    initialItem: V2CoachWorkspaceItem? = null,
    initialItemId: String? = initialItem?.itemId,
    availableConnections: List<V2CoachConnection> = listOf(connection),
    switcherItems: List<V2CoachWorkspaceItem> = emptyList(),
    onSwitchConnection: (V2CoachConnection) -> Unit = {},
    onAddSpace: (() -> Unit)? = null,
    onEditConnection: ((V2CoachConnection) -> Unit)? = null,
    onConnectionUpdated: (V2CoachConnection) -> Unit = {},
) {
    val context = LocalContext.current
    val workspaceLifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val composerFocusRequester = remember(connection.connectionId) { FocusRequester() }
    val workspaceSession by ServiceLocator.session.state.collectAsState(initial = V2Session())
    val deliveryState by ServiceLocator.coachDeliveryQueue.state.collectAsState()
    val ownerDeliveryJobs = deliveryState.jobsForOwner(workspaceSession.userId)
    val ownerDeliveryCount = ownerDeliveryJobs.size
    val draftOwnerUserId = workspaceSession.userId.orEmpty()
    val draftStore = remember(context) { V2WorkspaceDraftStore(context) }
    var threadDraftRevision by remember(connection.connectionId) { mutableIntStateOf(0) }
    val restoredDraft = remember(connection.connectionId, draftOwnerUserId) {
        if (draftOwnerUserId.isBlank()) com.mygolfswing.app.data.coach.V2WorkspaceDraft()
        else draftStore.loadRoot(draftOwnerUserId, connection.connectionId)
    }
    val person = if (coachPerspective) connection.student else connection.coach ?: connection.peer
    var feed by remember(connection.connectionId) { mutableStateOf<List<V2CoachWorkspaceItem>>(emptyList()) }
    var feedVersion by remember(connection.connectionId) { mutableStateOf<String?>(null) }
    var feedNextCursor by remember(connection.connectionId) { mutableStateOf<String?>(null) }
    var loadingMore by remember(connection.connectionId) { mutableStateOf(false) }
    var loading by remember(connection.connectionId) { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var draft by rememberSaveable(connection.connectionId, draftOwnerUserId) { mutableStateOf(restoredDraft.text) }
    var showNewConversationComposer by rememberSaveable(connection.connectionId, draftOwnerUserId) {
        mutableStateOf(restoredDraft.text.isNotBlank() || restoredDraft.primaryAttachment != null)
    }
    var requestCoachReview by rememberSaveable(connection.connectionId, draftOwnerUserId) {
        mutableStateOf(restoredDraft.requiresCoachReview)
    }
    var saveVideoToLibrary by rememberSaveable(connection.connectionId, draftOwnerUserId) {
        mutableStateOf(restoredDraft.saveToLibrary && coachPerspective)
    }
    var error by remember { mutableStateOf<String?>(null) }
    var postSuccessMessage by remember(connection.connectionId) { mutableStateOf<String?>(null) }
    var postSuccessToken by remember(connection.connectionId) { mutableIntStateOf(0) }
    var selectedItem by remember(connection.connectionId, initialItem?.itemId) { mutableStateOf(initialItem) }
    var pendingVideo by remember(connection.connectionId, draftOwnerUserId) {
        mutableStateOf(
            restoredDraft.primaryAttachment?.let { attachment ->
                restorePreparedWorkspaceVideo(attachment, fallbackRequestPrefix = "workspace")
            },
        )
    }
    var pendingVideoTitle by remember(connection.connectionId, draftOwnerUserId) {
        mutableStateOf(
            restoredDraft.primaryAttachment?.let { attachment ->
                attachment.title.takeIf { it.isNotBlank() && it != "关联教学视频" }
                    ?: when (attachment.sourceKind) {
                        "student_recording" -> "挥杆视频"
                        "personal_record" -> "练习记录"
                        else -> "练习视频"
                    }
            },
        )
    }
    var showStudentSendConfirm by rememberSaveable(connection.connectionId, draftOwnerUserId) {
        mutableStateOf(!coachPerspective && restoredDraft.primaryAttachment != null)
    }
    var trimmingVideo by remember(connection.connectionId) { mutableStateOf<V2PreparedWorkspaceVideo?>(null) }
    var trimSaving by remember(connection.connectionId) { mutableStateOf(false) }
    var trimError by remember(connection.connectionId) { mutableStateOf<String?>(null) }
    var coverPickingVideo by remember(connection.connectionId) { mutableStateOf<V2PreparedWorkspaceVideo?>(null) }
    var coverSaving by remember(connection.connectionId) { mutableStateOf(false) }
    var coverError by remember(connection.connectionId) { mutableStateOf<String?>(null) }
    var pendingCellularJobId by remember(connection.connectionId) { mutableStateOf<String?>(null) }
    var pendingCellularFileSize by remember(connection.connectionId) { mutableLongStateOf(0L) }
    var pendingCellularClearsDraft by remember(connection.connectionId) { mutableStateOf(false) }
    var pendingStudentCellularJob by remember(connection.connectionId) { mutableStateOf<V2CoachDeliveryJob?>(null) }
    var pendingCameraFile by remember(connection.connectionId) { mutableStateOf<File?>(null) }
    var showQuickPostTools by rememberSaveable(connection.connectionId) { mutableStateOf(false) }
    var showRootQuickReplies by rememberSaveable(connection.connectionId) { mutableStateOf(false) }
    var rootQuickReplies by remember(connection.connectionId) { mutableStateOf<List<V2CoachQuickReply>>(emptyList()) }
    var showPersonalRecordPicker by remember(connection.connectionId) { mutableStateOf(false) }
    var showCoachLibraryPicker by remember(connection.connectionId) { mutableStateOf(false) }
    var coachLibrary by remember(connection.connectionId) { mutableStateOf<List<V2CoachLibraryAsset>>(emptyList()) }
    var coachLibraryLoading by remember(connection.connectionId) { mutableStateOf(false) }
    val rootDeliveryJobs = ownerDeliveryJobs.filter { job ->
        job.connectionId == connection.connectionId && job.parentItemId.isNullOrBlank()
    }
    val activeRootDeliveryJob = rootDeliveryJobs.firstOrNull { job ->
        deliveryState.activeJobId == job.jobId
    }
    val retainedRootDeliveryJob = rootDeliveryJobs
        .asSequence()
        .filter { job -> job.pausedByUser || !job.lastError.isNullOrBlank() }
        .maxByOrNull(V2CoachDeliveryJob::createdAtMillis)
    val personalRecords by remember { ServiceLocator.memoRepo.observeAll() }.collectAsState(initial = emptyList())
    val contentDisplayPreferences = remember(context) {
        context.getSharedPreferences("v2_student_space_display", Context.MODE_PRIVATE)
    }
    val displayPerspective = if (coachPerspective) "coach" else "student"
    val contentLayoutPreferenceKey =
        "student_space_content_layout_${displayPerspective}_${connection.connectionId}"
    val contentSortPreferenceKey =
        "student_space_content_sort_${displayPerspective}_${connection.connectionId}"
    var compactFeed by rememberSaveable(contentLayoutPreferenceKey) {
        mutableStateOf(contentDisplayPreferences.getString(contentLayoutPreferenceKey, "comfortable") == "compact")
    }
    var newestFirst by rememberSaveable(contentSortPreferenceKey) {
        mutableStateOf(contentDisplayPreferences.getString(contentSortPreferenceKey, "newest") != "oldest")
    }
    var teachingFilter by rememberSaveable(connection.connectionId) { mutableStateOf("all") }
    var openUnreadForItemId by remember(connection.connectionId) { mutableStateOf<String?>(null) }
    val feedListState = rememberLazyListState()
    val feedScrollMemory = remember { mutableMapOf<String, Pair<Int, Int>>() }
    var feedScrollConnectionId by remember { mutableStateOf(connection.connectionId) }
    if (feedScrollConnectionId != connection.connectionId) {
        feedScrollMemory[feedScrollConnectionId] =
            feedListState.firstVisibleItemIndex to feedListState.firstVisibleItemScrollOffset
        feedScrollConnectionId = connection.connectionId
    }
    LaunchedEffect(connection.connectionId) {
        val saved = feedScrollMemory[connection.connectionId] ?: return@LaunchedEffect
        feedListState.scrollToItem(saved.first, saved.second)
    }
    var selectedContentMode by rememberSaveable(connection.connectionId) {
        mutableStateOf(initialItem?.contentMode ?: V2WorkspaceContentMode.PRACTICE)
    }
    var showDisconnectConfirmation by remember(connection.connectionId) { mutableStateOf(false) }
    var showSpaceSwitcher by remember(connection.connectionId) { mutableStateOf(false) }
    var showStudentPostMenu by remember(connection.connectionId) { mutableStateOf(false) }
    var showCoachContentSourceMenu by remember(connection.connectionId) { mutableStateOf(false) }
    var coachEditLibraryAsset by remember(connection.connectionId) { mutableStateOf<V2CoachLibraryAsset?>(null) }
    var showIMChat by remember(connection.connectionId) { mutableStateOf(false) }
    var disconnecting by remember(connection.connectionId) { mutableStateOf(false) }
    var pinning by remember(connection.connectionId) { mutableStateOf(false) }
    var headerPinned by remember(connection.connectionId, connection.pinned) { mutableStateOf(connection.pinned) }
    var feedPlayback by remember(connection.connectionId) { mutableStateOf<WorkspaceAnalysisPlayback?>(null) }
    var preparingFeedPlaybackId by remember(connection.connectionId) { mutableStateOf<String?>(null) }

    LaunchedEffect(
        draftOwnerUserId,
        draft,
        requestCoachReview,
        saveVideoToLibrary,
        pendingVideo?.uploadId,
        pendingVideo?.cover?.absolutePath,
        pendingVideoTitle,
    ) {
        if (draftOwnerUserId.isNotBlank()) {
            draftStore.saveRoot(
                draftOwnerUserId,
                connection.connectionId,
                draft,
                V2WorkspaceContentMode.PRACTICE,
                requestCoachReview,
                saveVideoToLibrary && coachPerspective,
                pendingVideo?.let { prepared ->
                    V2WorkspaceDraftAttachment(
                        videoPath = prepared.file.absolutePath,
                        coverPath = prepared.cover?.absolutePath,
                        uploadId = prepared.uploadId,
                        requestId = prepared.requestId,
                        sourceKind = prepared.sourceKind,
                        title = pendingVideoTitle?.takeIf(String::isNotBlank)
                            ?: if (prepared.sourceKind == "student_recording") "挥杆视频" else "练习视频",
                        durationSeconds = prepared.durationSeconds,
                        coverTimeSeconds = prepared.coverTimeSeconds,
                        sourceRecordId = prepared.sourceRecordId,
                        saveToLibrary = saveVideoToLibrary && coachPerspective,
                    )
                },
            )
        }
    }

    fun clearSentRootDraft(ownerUserId: String) {
        draft = ""
        showQuickPostTools = false
        requestCoachReview = false
        saveVideoToLibrary = false
        draftStore.clearRoot(ownerUserId, connection.connectionId)
        pendingVideo = null
        pendingVideoTitle = null
        showStudentSendConfirm = false
        showNewConversationComposer = false
    }

    fun showPostSuccess(item: V2CoachWorkspaceItem, hasMedia: Boolean) {
        val token = postSuccessToken + 1
        postSuccessToken = token
        postSuccessMessage = workspacePostSuccessMessage(
            friendSpace = connection.relationshipType == "friend",
            coachPerspective = coachPerspective,
            hasMedia = hasMedia,
        )
        scope.launch {
            delay(220)
            if (postSuccessToken == token) selectedItem = item
            delay(2_780)
            if (postSuccessToken == token) postSuccessMessage = null
        }
    }

    fun retryRootDelivery(job: V2CoachDeliveryJob) {
        if (sending || deliveryState.activeJobId != null) return
        ServiceLocator.coachDeliveryQueue.cellularConfirmationFor(job)?.let { confirmation ->
            pendingCellularJobId = confirmation.jobId
            pendingCellularFileSize = confirmation.fileSizeBytes
            pendingCellularClearsDraft = false
            error = null
            return
        }
        sending = true
        error = null
        scope.launch {
            ServiceLocator.coachDeliveryQueue.deliver(job.jobId, ServiceLocator.coachWorkspaceRepo)
                .onSuccess { result ->
                    val item = result.requireWorkspaceItem()
                    feed = (listOf(item) + feed).distinctBy(V2CoachWorkspaceItem::itemId)
                    showPostSuccess(item, hasMedia = job.sourceKind != "text")
                }
                .onFailure { throwable ->
                    if (throwable is V2CellularUploadConfirmationRequired) {
                        pendingCellularJobId = throwable.jobId
                        pendingCellularFileSize = throwable.fileSizeBytes
                        pendingCellularClearsDraft = false
                    } else {
                        error = coachThrowableError("发送仍未完成，内容继续保留在队列中", throwable)
                    }
                }
            sending = false
        }
    }

    fun sendVideo(
        prepared: V2PreparedWorkspaceVideo,
        cellularApproved: Boolean = false,
        note: String = "",
        tags: List<String> = emptyList(),
    ) {
        if (sending) return
        val description = if (coachPerspective) note.trim() else draft.trim()
        if (description.isEmpty()) return
        sending = true
        error = null
        scope.launch {
            var enqueued = false
            var deliveryJob: V2CoachDeliveryJob? = null
            try {
                val ownerUserId = ServiceLocator.session.current().userId
                    ?: error("登录状态已失效，请重新登录")
                val job = V2CoachDeliveryJob(
                    jobId = prepared.requestId,
                    ownerUserId = ownerUserId,
                    connectionId = connection.connectionId,
                    peerName = person?.nickname.orEmpty(),
                    title = pendingVideoTitle?.takeIf(String::isNotBlank) ?: "练习视频",
                    text = description.take(STUDENT_SPACE_SEND_NOTE_LIMIT),
                    tags = if (coachPerspective) tags else emptyList(),
                    videoPath = prepared.file.absolutePath,
                    coverPath = prepared.cover?.absolutePath,
                    uploadId = prepared.uploadId,
                    requestId = prepared.requestId,
                    durationSeconds = prepared.durationSeconds,
                    requiresCoachReview = workspaceRequiresCoachReview(
                        coachPerspective,
                        connection.relationshipType,
                        requestCoachReview,
                    ),
                    contentMode = V2WorkspaceContentMode.PRACTICE.wireValue,
                    sourceKind = prepared.sourceKind,
                    sourceRecordId = prepared.sourceRecordId,
                    saveToLibrary = saveVideoToLibrary && coachPerspective,
                    cellularUploadApproved = cellularApproved,
                )
                deliveryJob = job
                if (!cellularApproved && !coachPerspective && showStudentSendConfirm) {
                    ServiceLocator.coachDeliveryQueue.cellularConfirmationFor(job)?.let { confirmation ->
                        pendingStudentCellularJob = job
                        pendingCellularFileSize = confirmation.fileSizeBytes
                        return@launch
                    }
                }
                ServiceLocator.coachDeliveryQueue.enqueue(job)
                enqueued = true
                val item = ServiceLocator.coachDeliveryQueue.deliver(
                    job.jobId,
                    ServiceLocator.coachWorkspaceRepo,
                ).getOrThrow().requireWorkspaceItem()
                feed = listOf(item) + feed
                clearSentRootDraft(ownerUserId)
                showPostSuccess(item, hasMedia = true)
            } catch (throwable: Throwable) {
                if (throwable is V2CellularUploadConfirmationRequired) {
                    val studentDraftJob = deliveryJob?.takeIf { !coachPerspective && showStudentSendConfirm }
                    if (studentDraftJob != null) {
                        ServiceLocator.coachDeliveryQueue.withdrawPreservingFiles(studentDraftJob.jobId)
                        pendingStudentCellularJob = studentDraftJob
                        pendingCellularFileSize = throwable.fileSizeBytes
                        error = null
                    } else {
                        pendingCellularJobId = throwable.jobId
                        pendingCellularFileSize = throwable.fileSizeBytes
                        pendingCellularClearsDraft = false
                        error = null
                    }
                } else if (enqueued) {
                    error = "视频已进入发送队列，网络恢复后会自动继续"
                } else {
                    error = coachThrowableError("视频暂时无法进入发送队列，请重试", throwable)
                    pendingVideo = prepared
                }
            } finally {
                sending = false
            }
        }
    }

    fun preparePersonalRecord(memo: V2Memo) {
        if (sending) return
        sending = true
        showPersonalRecordPicker = false
        error = null
        scope.launch {
            var source: PreparedShareVideo? = null
            var queuedFile: File? = null
            var cover: File? = null
            try {
                val prepared = prepareShareVideo(context, memo)
                source = prepared
                queuedFile = withContext(Dispatchers.IO) {
                    val extension = prepared.file.extension.lowercase().takeIf { it in setOf("mp4", "mov", "m4v") } ?: "mp4"
                    File.createTempFile("space-record-", ".$extension", context.cacheDir).also { target ->
                        prepared.file.copyTo(target, overwrite = true)
                    }
                }
                cover = createShareCover(context, queuedFile)
                val requestId = "space_record_${memo.id}_${UUID.randomUUID()}"
                pendingVideoTitle = memo.displayTitle?.takeIf(String::isNotBlank)
                    ?: memo.title?.takeIf(String::isNotBlank)
                    ?: "练习记录"
                pendingVideo = V2PreparedWorkspaceVideo(
                    file = queuedFile,
                    cover = cover,
                    durationSeconds = memo.durationSec,
                    uploadId = "record_${memo.id}_${UUID.randomUUID()}",
                    requestId = requestId,
                    sourceKind = "personal_record",
                    sourceRecordId = memo.id.toString(),
                )
                queuedFile = null
                cover = null
                draft = (memo.text?.trim()?.takeIf { it.isNotEmpty() }
                    ?: memo.displayTitle?.trim()?.takeIf { it.isNotEmpty() && it !in teachingAutoTitles }
                    ?: memo.title?.trim()?.takeIf { it.isNotEmpty() && it !in teachingAutoTitles }
                    ?: "").take(STUDENT_SPACE_SEND_NOTE_LIMIT)
                requestCoachReview = !coachPerspective && connection.relationshipType != "friend"
                showNewConversationComposer = false
                showStudentSendConfirm = !coachPerspective
            } catch (throwable: Throwable) {
                error = coachThrowableError("无法准备这条练习记录，请稍后重试", throwable)
            } finally {
                source?.takeIf(PreparedShareVideo::temporary)?.file?.delete()
                queuedFile?.delete()
                cover?.delete()
                sending = false
            }
        }
    }

    fun confirmCellularUpload() {
        val jobId = pendingCellularJobId ?: return
        if (sending) return
        val clearComposer = pendingCellularClearsDraft
        pendingCellularJobId = null
        sending = true
        error = null
        scope.launch {
            try {
                if (!ServiceLocator.coachDeliveryQueue.approveCellularUpload(jobId)) {
                    throw IllegalStateException("待发送内容已经完成或失效")
                }
                val item = ServiceLocator.coachDeliveryQueue.deliver(
                    jobId,
                    ServiceLocator.coachWorkspaceRepo,
                ).getOrThrow().requireWorkspaceItem()
                feed = listOf(item) + feed
                requestCoachReview = false
                if (clearComposer) {
                    draft = ""
                    pendingVideo = null
                    pendingVideoTitle = null
                    saveVideoToLibrary = false
                    if (draftOwnerUserId.isNotBlank()) {
                        draftStore.clearRoot(draftOwnerUserId, connection.connectionId)
                    }
                } else if (!coachPerspective && showStudentSendConfirm && draftOwnerUserId.isNotBlank()) {
                    clearSentRootDraft(draftOwnerUserId)
                }
                showPostSuccess(item, hasMedia = true)
            } catch (throwable: Throwable) {
                error = coachThrowableError("视频暂未送达，已保留在发送队列", throwable)
            } finally {
                sending = false
            }
        }
    }

    fun prepareAndSendVideo(
        uri: android.net.Uri,
        capturedFile: File? = null,
        captureSource: Boolean = capturedFile != null,
    ) {
        if (sending) return
        sending = true
        error = null
        scope.launch {
            try {
                val prepared = prepareWorkspaceVideo(
                    context = context,
                    uri = uri,
                    uploadId = "workspace_${connection.connectionId}_${UUID.randomUUID()}",
                    requestId = "workspace_post_${UUID.randomUUID()}",
                    sourceKind = when {
                        !captureSource -> "album"
                        coachPerspective -> "coach_recording"
                        else -> "student_recording"
                    },
                )
                pendingVideo = prepared
                coachEditLibraryAsset = null
                pendingVideoTitle = if (captureSource) "挥杆视频" else "练习视频"
                requestCoachReview = !coachPerspective && connection.relationshipType != "friend"
                when (
                    v2WorkspacePreparedVideoRoute(
                        coachPerspective = coachPerspective,
                        captureSource = captureSource,
                        hasDuration = prepared.durationSeconds?.takeIf { it.isFinite() && it > 0.0 } != null,
                    )
                ) {
                    V2WorkspacePreparedVideoRoute.STUDENT_CONFIRM -> {
                        trimmingVideo = null
                        showNewConversationComposer = false
                        showStudentSendConfirm = true
                    }
                    V2WorkspacePreparedVideoRoute.TRIM -> {
                        showStudentSendConfirm = false
                        showNewConversationComposer = false
                        trimmingVideo = prepared
                    }
                    V2WorkspacePreparedVideoRoute.COACH_COMPOSER -> {
                        trimmingVideo = null
                        showStudentSendConfirm = false
                        showNewConversationComposer = false
                    }
                }
            } catch (throwable: Throwable) {
                error = coachThrowableError("无法准备所选视频，请重新选择", throwable)
            } finally {
                capturedFile?.delete()
                sending = false
            }
        }
    }

    fun shareCoachLibraryAsset(asset: V2CoachLibraryAsset, note: String = "") {
        if (sending || note.isBlank()) return
        sending = true
        error = null
        scope.launch {
            ServiceLocator.coachWorkspaceRepo.createAssetPost(
                connectionId = connection.connectionId,
                title = asset.title,
                text = note.trim().take(1_000),
                assetId = asset.assetId,
                clientRequestId = "workspace_library_${asset.assetId}_${UUID.randomUUID()}",
                requiresCoachReview = false,
                contentMode = V2WorkspaceContentMode.PRACTICE,
            ).onSuccess { item ->
                feed = listOf(item) + feed
                draft = ""
                saveVideoToLibrary = false
                showNewConversationComposer = false
                if (draftOwnerUserId.isNotBlank()) draftStore.clearRoot(draftOwnerUserId, connection.connectionId)
                showCoachLibraryPicker = false
                coachEditLibraryAsset = null
                showPostSuccess(item, hasMedia = true)
            }.onFailure { throwable ->
                error = coachThrowableError("素材暂时无法发送，请重试", throwable)
            }
            sending = false
        }
    }

    fun openCoachLibrary() {
        showQuickPostTools = false
        showCoachLibraryPicker = true
        coachLibraryLoading = false
        error = null
    }

    fun useRootQuickReply(quickReply: V2CoachQuickReply) {
        val updatedDraft = appendWorkspaceQuickReplyDraft(draft, quickReply.text)
        if (updatedDraft == draft && quickReply.text.isBlank()) return
        draft = updatedDraft
        rootQuickReplies = listOf(quickReply) + rootQuickReplies.filterNot {
            it.quickReplyId == quickReply.quickReplyId
        }
        showRootQuickReplies = false
        showQuickPostTools = false
        error = null
        scope.launch {
            ServiceLocator.coachWorkspaceRepo.markQuickReplyUsed(quickReply.quickReplyId)
                .onSuccess { confirmed ->
                    rootQuickReplies = listOf(confirmed) + rootQuickReplies.filterNot {
                        it.quickReplyId == confirmed.quickReplyId
                    }
                }
        }
        composerFocusRequester.requestFocus()
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            prepareAndSendVideo(uri)
        }
    }

    val cameraFallbackVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            prepareAndSendVideo(uri, captureSource = true)
        }
    }

    fun launchCameraFallbackPicker() {
        error = null
        runCatching {
            cameraFallbackVideoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
            )
        }.onFailure { fallbackThrowable ->
            error = coachThrowableError("无法打开相机或相册，请稍后重试", fallbackThrowable)
            if (coachPerspective) showCoachContentSourceMenu = true else showStudentPostMenu = true
        }
    }

    fun launchWorkspaceVideoPicker() {
        showQuickPostTools = false
        showPersonalRecordPicker = false
        runCatching {
            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }.onFailure { throwable ->
            error = coachThrowableError("无法打开相册，请稍后重试或改用拍摄", throwable)
            if (coachPerspective) showCoachContentSourceMenu = true else showQuickPostTools = true
        }
    }

    val captureVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { captured ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (captured && file != null) {
            prepareAndSendVideo(file.toUri(), capturedFile = file)
        } else {
            file?.delete()
        }
    }

    fun launchCamera() {
        var createdFile: File? = null
        runCatching {
            val (file, uri) = V2VideoFileStore.createCameraOutputUri(context)
            createdFile = file
            pendingCameraFile = file
            captureVideo.launch(uri)
        }
            .onFailure {
                pendingCameraFile = null
                createdFile?.delete()
                launchCameraFallbackPicker()
            }
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        } else {
            error = "需要相机权限才能拍摄视频"
            if (coachPerspective) showCoachContentSourceMenu = true else showStudentPostMenu = true
        }
    }

    fun requestCamera() {
        showQuickPostTools = false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            runCatching { cameraPermission.launch(Manifest.permission.CAMERA) }
                .onFailure { throwable ->
                    error = coachThrowableError("无法请求相机权限，请改从相册选择", throwable)
                    if (coachPerspective) showCoachContentSourceMenu = true else showStudentPostMenu = true
                }
        }
    }

    fun playFeedItem(item: V2CoachWorkspaceItem) {
        if (!item.hasVideoContent) {
            selectedItem = item
            return
        }
        val direct = workspaceStreamPlaybackUrl(item.primaryAsset?.videoUrl ?: item.videoUrl)
        val title = workspaceThreadSourceTitle(item, connection.relationshipType == "friend")
        fun present(uri: String) {
            preparingFeedPlaybackId = null
            feedPlayback = WorkspaceAnalysisPlayback(
                uri = uri,
                title = title,
                subtitle = person?.nickname,
            )
        }
        if (direct != null) {
            present(direct)
            return
        }
        if (preparingFeedPlaybackId != null) return
        preparingFeedPlaybackId = item.itemId
        scope.launch {
            val refreshed = ServiceLocator.coachWorkspaceRepo
                .workspaceThread(connection.connectionId, item.itemId)
                .getOrNull()
            val resolved = workspaceStreamPlaybackUrl(
                refreshed?.item?.primaryAsset?.videoUrl ?: refreshed?.item?.videoUrl,
            )
            if (resolved != null) {
                present(resolved)
            } else {
                preparingFeedPlaybackId = null
                error = "暂时拿不到这段视频，请稍后重试"
            }
        }
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            ServiceLocator.coachWorkspaceRepo.workspaceFeedUpdate(connection.connectionId)
                .onSuccess { update ->
                    update.items?.let { items ->
                        feed = mergeWorkspaceFeedPage(feed, items, firstPage = true)
                        feedNextCursor = update.nextCursor
                    }
                    update.version?.let { feedVersion = it }
                }
                .onFailure { error = coachThrowableError("空间内容暂时无法同步，请稍后重试", it) }
            loading = false
        }
    }
    fun loadMoreFeed() {
        val cursor = feedNextCursor ?: return
        if (loadingMore) return
        loadingMore = true
        error = null
        scope.launch {
            ServiceLocator.coachWorkspaceRepo.workspaceFeedUpdate(
                connectionId = connection.connectionId,
                cursor = cursor,
                limit = 20,
            ).onSuccess { update ->
                update.items?.let { items ->
                    feed = mergeWorkspaceFeedPage(feed, items, firstPage = false)
                    feedNextCursor = update.nextCursor
                }
            }.onFailure { throwable ->
                error = coachThrowableError("更早的空间内容暂时无法加载，请稍后重试", throwable)
            }
            loadingMore = false
        }
    }
    LaunchedEffect(workspaceLifecycleOwner, connection.connectionId) {
        workspaceLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            loading = true
            error = null
            ServiceLocator.coachWorkspaceRepo.workspaceFeedUpdate(
                connectionId = connection.connectionId,
                knownVersion = feedVersion,
            ).onSuccess { update ->
                update.items?.let { items ->
                    feed = mergeWorkspaceFeedPage(feed, items, firstPage = true)
                    feedNextCursor = update.nextCursor
                }
                update.version?.let { feedVersion = it }
            }.onFailure { error = coachThrowableError("空间内容暂时无法同步，请稍后重试", it) }
            loading = false
            while (true) {
                delay(30_000)
                ServiceLocator.coachWorkspaceRepo.workspaceFeedUpdate(
                    connectionId = connection.connectionId,
                    knownVersion = feedVersion,
                ).onSuccess { update ->
                    update.items?.let { items ->
                        feed = mergeWorkspaceFeedPage(feed, items, firstPage = true)
                        feedNextCursor = update.nextCursor
                    }
                    update.version?.let { feedVersion = it }
                }.onFailure { throwable ->
                    error = coachThrowableError("空间内容暂时无法同步，请稍后重试", throwable)
                }
            }
        }
    }
    LaunchedEffect(connection.connectionId, initialItemId) {
        val targetId = initialItemId ?: return@LaunchedEffect
        if (selectedItem?.itemId == targetId) return@LaunchedEffect
        ServiceLocator.coachWorkspaceRepo.workspaceThread(connection.connectionId, targetId)
            .onSuccess { thread ->
                selectedItem = thread.item
                feed = listOf(thread.item) + feed.filterNot { it.itemId == thread.item.itemId }
            }
            .onFailure { throwable ->
                error = coachThrowableError("暂时无法打开这条交流，请稍后重试", throwable)
            }
    }
    val shownFeed = sortWorkspaceItems(
        items = feed,
        newestFirst = newestFirst,
    )
    val teachingSpace = connection.relationshipType != "friend"
    val timelineItems = if (!teachingSpace) {
        shownFeed
    } else {
        shownFeed.filter { item ->
            when (teachingFilter) {
                "review" -> teachingSpaceAsksForReview(item)
                "fresh" -> teachingSpaceHasFreshReply(item, coachPerspective)
                else -> true
            }
        }
    }
    val timelineGroups = if (teachingSpace) {
        teachingSpaceDayGroups(timelineItems, newestFirst)
    } else {
        emptyList()
    }
    val shouldShowNewConversationComposer = !coachPerspective &&
        (showNewConversationComposer || draft.isNotBlank() || pendingVideo != null)
    val threadDraftItemIds = remember(
        draftOwnerUserId,
        connection.connectionId,
        threadDraftRevision,
        selectedItem?.itemId,
    ) {
        if (draftOwnerUserId.isBlank()) emptySet()
        else draftStore.threadDraftItemIds(draftOwnerUserId, connection.connectionId)
    }
    val hasConnectionDrafts = draft.isNotBlank() || pendingVideo != null || threadDraftItemIds.isNotEmpty() ||
        (draftOwnerUserId.isNotBlank() && draftStore.hasConnectionDraft(draftOwnerUserId, connection.connectionId))
    fun sendText() {
        val text = draft.trim()
        if (text.isBlank() || sending) return
        sending = true
        error = null
        scope.launch {
            var enqueued = false
            try {
                val ownerUserId = ServiceLocator.session.current().userId
                    ?: error("登录状态已失效，请重新登录")
                val requestId = "workspace_text_${UUID.randomUUID()}"
                val job = V2CoachDeliveryJob(
                    jobId = requestId,
                    ownerUserId = ownerUserId,
                    connectionId = connection.connectionId,
                    peerName = person?.nickname.orEmpty(),
                    uploadId = "text_$requestId",
                    requestId = requestId,
                    title = "练习记录",
                    text = text,
                    requiresCoachReview = workspaceRequiresCoachReview(
                        coachPerspective,
                        connection.relationshipType,
                        requestCoachReview,
                    ),
                    contentMode = V2WorkspaceContentMode.PRACTICE.wireValue,
                    sourceKind = "text",
                )
                ServiceLocator.coachDeliveryQueue.enqueue(job)
                enqueued = true
                draft = ""
                requestCoachReview = false
                saveVideoToLibrary = false
                draftStore.clearRoot(ownerUserId, connection.connectionId)
                showNewConversationComposer = false
                val item = ServiceLocator.coachDeliveryQueue.deliver(
                    job.jobId,
                    ServiceLocator.coachWorkspaceRepo,
                ).getOrThrow().requireWorkspaceItem()
                feed = listOf(item) + feed
                showPostSuccess(item, hasMedia = false)
            } catch (throwable: Throwable) {
                error = if (enqueued) {
                    "文字内容已进入发送队列，网络恢复后会自动继续"
                } else {
                    coachThrowableError("发送失败，请重试", throwable)
                }
            } finally {
                sending = false
            }
        }
    }

    if (showIMChat && person != null) {
        V2InteractionChatScreen(
            conversation = V2IMConversation(
                userId = person.userId,
                title = person.nickname,
                avatar = person.avatar,
                preview = "已连接，可以开始聊天",
                isCoach = !coachPerspective && connection.relationshipType != "friend",
            ),
            subtitle = when {
                connection.relationshipType == "friend" -> "好友私聊"
                coachPerspective -> "我的学员"
                else -> "我的教练"
            },
            onBack = { showIMChat = false },
        )
        return
    }

    if (showStudentSendConfirm && !coachPerspective && pendingVideo != null) {
        val prepared = pendingVideo ?: return
        val captured = prepared.sourceKind == "student_recording"
        fun discardStudentSendDraft() {
            prepared.deleteTemporaryFiles()
            pendingVideo = null
            pendingVideoTitle = null
            draft = ""
            requestCoachReview = false
            showStudentSendConfirm = false
            showNewConversationComposer = false
            if (draftOwnerUserId.isNotBlank()) {
                draftStore.clearRoot(draftOwnerUserId, connection.connectionId)
            }
        }
        V2StudentSpaceSendConfirmScreen(
            prepared = prepared,
            isCapture = captured,
            friendSpace = connection.relationshipType == "friend",
            note = draft.take(STUDENT_SPACE_SEND_NOTE_LIMIT),
            wantsReview = requestCoachReview,
            sending = sending,
            error = error,
            onBack = ::discardStudentSendDraft,
            onRetake = {
                discardStudentSendDraft()
                requestCamera()
            },
            onNoteChange = { draft = it.take(STUDENT_SPACE_SEND_NOTE_LIMIT); error = null },
            onReviewChange = { requestCoachReview = it },
            onSend = { sendVideo(prepared) },
            showCellularConfirmation = pendingStudentCellularJob != null || pendingCellularJobId != null,
            onConfirmCellular = {
                if (pendingStudentCellularJob != null) {
                    pendingStudentCellularJob = null
                    sendVideo(prepared, cellularApproved = true)
                } else {
                    confirmCellularUpload()
                }
            },
            onDismissCellular = {
                pendingStudentCellularJob = null
                pendingCellularJobId = null
            },
        )
        return
    }

    feedPlayback?.let { playback ->
        V2WorkspaceProfessionalAnalysisScreen(
            mediaUri = playback.uri,
            title = playback.title,
            subtitle = playback.subtitle,
            startsPlaying = true,
            continuousPlayback = true,
            mode = V2ProfessionalAnalysisMode.STANDALONE_REVIEW,
            onCancel = { feedPlayback = null },
            onRecordingReady = { _ -> },
        )
        return
    }

    V2BackHandler(onBack = onBack)

    val coachTeachingSpace = coachPerspective && connection.relationshipType != "friend"
        Box(Modifier.fillMaxSize().background(if (teachingSpace) CoachPaper else DSColors.warmWhite)) {
        if (!teachingSpace) {
            V2RoomBackground(Modifier.fillMaxSize())
        }
        Column(
            Modifier.fillMaxSize().then(
                if (coachPerspective && !teachingSpace) Modifier.v2StatusBarPadding() else Modifier,
            ),
        ) {
            val topBar = @Composable {
            WorkspaceTopBar(
                title = person?.nickname
                    ?: if (connection.relationshipType == "friend") "好友空间"
                    else if (coachPerspective) "学员空间" else "教练空间",
                spaceType = if (connection.relationshipType == "friend") "好友空间" else "教学空间",
                active = connection.status == "active",
                busy = sending || disconnecting || pinning,
                pinned = headerPinned,
                onBack = onBack,
                onSwitchSpace = { showSpaceSwitcher = true },
                coachCompact = coachTeachingSpace,
                avatar = person?.avatar,
                detail = connection.resolvedTrainingFocus?.trim()?.takeIf { it.isNotEmpty() },
                onEditProfile = if (coachPerspective && onEditConnection != null) {
                    { onEditConnection(connection) }
                } else null,
                onTogglePinned = {
                    pinning = true
                    error = null
                    scope.launch {
                        ServiceLocator.coachWorkspaceRepo.updateCoachPinned(
                            connection.copy(pinned = headerPinned),
                            !headerPinned,
                        ).onSuccess { updated ->
                            headerPinned = updated.pinned
                            onConnectionUpdated(updated)
                        }.onFailure { throwable ->
                            error = coachThrowableError("暂时无法更新重点学员状态，请稍后重试", throwable)
                        }
                        pinning = false
                    }
                },
                onDisconnect = {
                    if (ownerDeliveryCount > 0) {
                        error = "仍有 $ownerDeliveryCount 条内容正在发送，请完成后再解除连接"
                    } else {
                        showDisconnectConfirmation = true
                    }
                },
            )
            }

            if (teachingSpace) {
                TeachingSpaceHeader(
                    connection = connection,
                    name = person?.nickname?.trim().orEmpty().ifBlank {
                        if (coachPerspective) "学员" else "教练"
                    },
                    avatar = person?.avatar,
                    coachPerspective = coachPerspective,
                    active = connection.status == "active",
                    busy = sending || disconnecting || pinning,
                    pinnedStudent = headerPinned,
                    canSwitch = availableConnections.count { it.relationshipType != "friend" && it.status == "active" } > 1,
                    onBack = onBack,
                    onSwitch = { showSpaceSwitcher = true },
                    onEditProfile = if (coachPerspective && connection.status == "active" && onEditConnection != null) {
                        { onEditConnection(connection.copy(pinned = headerPinned)) }
                    } else null,
                    onTogglePinnedStudent = {
                        pinning = true
                        error = null
                        scope.launch {
                            ServiceLocator.coachWorkspaceRepo.updateCoachPinned(
                                connection.copy(pinned = headerPinned),
                                !headerPinned,
                            ).onSuccess { updated ->
                                headerPinned = updated.pinned
                                onConnectionUpdated(updated)
                            }.onFailure { throwable ->
                                error = coachThrowableError("暂时无法更新重点学员状态，请稍后重试", throwable)
                            }
                            pinning = false
                        }
                    },
                    onDisconnect = {
                        if (ownerDeliveryCount > 0) {
                            error = "仍有 $ownerDeliveryCount 条内容正在发送，请完成后再解除连接"
                        } else {
                            showDisconnectConfirmation = true
                        }
                    },
                )
            } else {
            V2PageArtworkBackground(
                artwork = V2PageArtwork.GOLFER,
                tone = V2PageArtworkTone.FADE_TO_WARM_BOTTOM,
                modifier = Modifier.fillMaxWidth().testTag("workspace-header-artwork"),
                alignment = Alignment.CenterEnd,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    topBar()
                    WorkspaceStudentOverviewCard(
                        connection = connection.copy(pinned = headerPinned),
                        coachPerspective = coachPerspective,
                        onEditProfile = if (
                            coachPerspective && connection.status == "active" && onEditConnection != null
                        ) {
                            { onEditConnection(connection.copy(pinned = headerPinned)) }
                        } else null,
                        modifier = Modifier.padding(horizontal = DSLayout.pageHorizontal)
                            .padding(bottom = DSLayout.pageHorizontal),
                    )
                }
            }
            }

            WorkspaceStatusStack(
                successMessage = postSuccessMessage,
                errorMessage = error,
                hasRecoverableDraft = draft.isNotBlank() || pendingVideo != null,
                busy = sending,
                onContinueSend = { pendingVideo?.let(::sendVideo) ?: sendText() },
                onRetrySync = ::reload,
                onDismissError = { error = null },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )

        when {
            connection.status != "active" -> {
                WorkspaceReadOnlyBanner(
                    text = workspaceDisconnectionBannerText(
                        connection = connection,
                        currentUserId = workspaceSession.userId,
                        peerName = person?.nickname,
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            !coachPerspective && activeRootDeliveryJob != null -> {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    WorkspaceComposerDeliveryPreview(
                        job = activeRootDeliveryJob,
                        phase = deliveryState.activePhase,
                        progress = deliveryState.activeProgress,
                        peerName = person?.nickname.orEmpty().ifBlank { "教练" },
                        onStop = {
                            scope.launch {
                                ServiceLocator.coachDeliveryQueue.stop(activeRootDeliveryJob.jobId)
                            }
                        },
                    )
                }
            }
            !coachPerspective && retainedRootDeliveryJob != null -> {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    WorkspaceComposerRecoveryNotice(
                        job = retainedRootDeliveryJob,
                        onRetry = { retryRootDelivery(retainedRootDeliveryJob) },
                    )
                }
            }
            shouldShowNewConversationComposer -> {
                val composerTextLimit = if (!coachPerspective && pendingVideo != null) 300 else 2_000
                WorkspaceRootComposerCard(
                    coachPerspective = coachPerspective,
                    friendSpace = connection.relationshipType == "friend",
                    draft = draft,
                    onDraftChange = { draft = it.take(composerTextLimit); error = null },
                    textLimit = composerTextLimit,
                    requestCoachReview = requestCoachReview,
                    onRequestCoachReviewChange = { requestCoachReview = it },
                    pendingVideo = pendingVideo != null,
                    customCoverSelected = pendingVideo?.coverTimeSeconds != null,
                    sending = sending,
                    toolsExpanded = showQuickPostTools,
                    focusRequester = composerFocusRequester,
                    onCollapse = {
                        showQuickPostTools = false
                        showNewConversationComposer = false
                    },
                    onToggleTools = { showQuickPostTools = !showQuickPostTools },
                    onInputFocused = { showQuickPostTools = false },
                    onCamera = {
                        showQuickPostTools = false
                        requestCamera()
                    },
                    onGallery = {
                        showQuickPostTools = false
                        launchWorkspaceVideoPicker()
                    },
                    onRoleLibrary = {
                        showQuickPostTools = false
                        if (coachPerspective) openCoachLibrary() else showPersonalRecordPicker = true
                    },
                    onQuickReplyOrRecord = {
                        showQuickPostTools = false
                        if (coachPerspective) showRootQuickReplies = true else showPersonalRecordPicker = true
                    },
                    onSelectCover = {
                        pendingVideo?.let { prepared ->
                            coverError = null
                            coverPickingVideo = prepared
                        }
                    },
                    onRemoveVideo = {
                        pendingVideo?.deleteTemporaryFiles()
                        pendingVideo = null
                        pendingVideoTitle = null
                        saveVideoToLibrary = false
                    },
                    onSend = {
                        if (coachPerspective) pendingVideo?.let(::sendVideo) else pendingVideo?.let(::sendVideo) ?: sendText()
                    },
                )
            }
        }

        if (teachingSpace && coachPerspective) {
            TeachingSpaceFilterBar(selected = teachingFilter, onSelect = { teachingFilter = it })
        }

        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = ::reload,
            modifier = Modifier.weight(1f).testTag("workspace-pull-refresh"),
        ) {
            LazyColumn(
                state = feedListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = if (shownFeed.isEmpty()) 4.dp else 10.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(if (teachingSpace) 0.dp else if (compactFeed) 8.dp else 14.dp),
            ) {
                when {
                    loading -> item {
                        Column(
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(color = DSColors.fairway)
                            Text(
                                if (connection.relationshipType == "friend") "正在读取空间内容…" else "正在读取教学内容…",
                                color = DSColors.shadowGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                    teachingSpace && error != null && shownFeed.isEmpty() -> item {
                        TeachingSpaceLoadFailure(onRetry = ::reload)
                    }
                    teachingSpace && timelineItems.isEmpty() -> item {
                        if (shownFeed.isEmpty()) {
                            TeachingSpaceEmptyNote()
                        } else {
                            Text(
                                "这个筛选下还没有内容",
                                color = DSColors.shadowGray,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth().padding(top = 36.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    teachingSpace -> teachingSpaceFeed(
                        groups = timelineGroups,
                        coachPerspective = coachPerspective,
                        peerName = person?.nickname.orEmpty(),
                        newestFirst = newestFirst,
                        onToggleSort = {
                            newestFirst = !newestFirst
                            contentDisplayPreferences.edit {
                                putString(contentSortPreferenceKey, if (newestFirst) "newest" else "oldest")
                            }
                        },
                        preparingItemId = preparingFeedPlaybackId,
                        onOpen = {
                            openUnreadForItemId = null
                            selectedItem = it
                        },
                        onOpenFresh = {
                            openUnreadForItemId = it.itemId
                            selectedItem = it
                        },
                        onPlay = { playFeedItem(it) },
                    )
                    shownFeed.isEmpty() -> item {
                        when {
                            connection.status != "active" -> WorkspaceCurrentEmptyStateCard(
                                coachPerspective = coachPerspective,
                                friendSpace = connection.relationshipType == "friend",
                            )
                            selectedContentMode == V2WorkspaceContentMode.WATCHING -> WorkspaceWatchingEmptyStateCard(
                                coachPerspective = coachPerspective,
                                friendSpace = connection.relationshipType == "friend",
                            )
                            else -> WorkspaceCurrentEmptyStateCard(
                                coachPerspective = coachPerspective,
                                friendSpace = connection.relationshipType == "friend",
                            )
                        }
                    }
                    else -> {
                        item(key = "workspace-content-toolbar") {
                            WorkspaceContentToolbar(
                                itemCount = shownFeed.size,
                                newestFirst = newestFirst,
                                compact = compactFeed,
                                onSelectNewestFirst = { selectedNewestFirst ->
                                    newestFirst = selectedNewestFirst
                                    contentDisplayPreferences.edit {
                                        putString(contentSortPreferenceKey, if (selectedNewestFirst) "newest" else "oldest")
                                    }
                                },
                                onToggleLayout = {
                                    compactFeed = !compactFeed
                                    contentDisplayPreferences.edit {
                                        putString(contentLayoutPreferenceKey, if (compactFeed) "compact" else "comfortable")
                                    }
                                },
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                        }
                        itemsIndexed(shownFeed, key = { _, item -> item.itemId }) { index, item ->
                            SpaceFeedCard(
                                item = item,
                                compact = compactFeed,
                                coachPerspective = coachPerspective,
                                friendSpace = connection.relationshipType == "friend",
                                hasReplyDraft = item.itemId in threadDraftItemIds,
                                onClick = { selectedItem = item },
                                onPlay = { playFeedItem(item) },
                            )
                            if (index == shownFeed.lastIndex) {
                                val nextCursor = feedNextCursor
                                LaunchedEffect(item.itemId, nextCursor) {
                                    if (nextCursor != null) loadMoreFeed()
                                }
                            }
                        }
                    }
                }
                if (teachingSpace && !loading && timelineItems.isNotEmpty()) {
                    item(key = "teaching-load-more") {
                        val nextCursor = feedNextCursor
                        val tailId = timelineItems.lastOrNull()?.itemId
                        LaunchedEffect(tailId, nextCursor) {
                            if (nextCursor != null) loadMoreFeed()
                        }
                    }
                }
                if (!loading && loadingMore) item {
                    Box(
                        modifier = Modifier.fillParentMaxWidth().padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = DSColors.fairway, strokeWidth = 2.dp)
                    }
                }
            }
        }

        if (
            connection.status == "active" &&
            !shouldShowNewConversationComposer
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().background(if (teachingSpace) CoachPaper else DSColors.warmWhite)
                    .then(if (coachPerspective) Modifier.v2NavigationBarPadding() else Modifier)
                    .padding(horizontal = DSLayout.pageHorizontal)
                    .padding(top = DSLayout.headerContentTop, bottom = DSLayout.headerContentTop),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(DSLayout.primaryButtonHeight).clip(CircleShape).background(Color.White)
                        .border(1.dp, DSColors.listCardBorder, CircleShape)
                        .clickable(enabled = person != null) { showIMChat = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.ChatBubble, "打开对话", tint = DSColors.fairway, modifier = Modifier.size(18.dp))
                }
                Row(
                    modifier = Modifier.weight(1f).height(DSLayout.primaryButtonHeight)
                        .testTag("workspace-post-button")
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (teachingSpace || coachPerspective) CoachGreen else DSColors.deepGreen).clickable(enabled = !sending) {
                            if (coachPerspective) {
                                showCoachContentSourceMenu = true
                            } else {
                                showStudentPostMenu = true
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(
                        "发内容",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 7.dp),
                    )
                }
            }
        } else if (coachPerspective) {
            Spacer(Modifier.fillMaxWidth().background(DSColors.warmWhite).v2NavigationBarPadding())
        }

        }
    }

    selectedItem?.let { item ->
        ThreadDialog(
            connectionId = connection.connectionId,
            peerName = person?.nickname.orEmpty(),
            peerAvatar = person?.avatar,
            friendSpace = connection.relationshipType == "friend",
            connectionStatus = connection.status,
            source = item,
            coachPerspective = coachPerspective,
            readOnly = connection.status != "active",
            onOpenTeachingProfile = if (coachPerspective && connection.relationshipType != "friend") {
                onEditConnection?.let { editConnection ->
                    {
                        selectedItem = null
                        editConnection(connection)
                    }
                }
            } else null,
            onDismiss = { selectedItem = null },
            practiceItems = shownFeed,
            onSelectPractice = { selectedItem = it },
            focusUnreadReply = openUnreadForItemId == item.itemId,
        ) {
            threadDraftRevision++
            reload()
        }
    }
    if (showSpaceSwitcher && coachPerspective && connection.relationshipType != "friend") {
        CoachStudentSwitcherDrawer(
            connections = availableConnections,
            currentConnectionId = connection.connectionId,
            workspaceItems = (switcherItems + feed).distinctBy(V2CoachWorkspaceItem::itemId),
            onDismiss = { showSpaceSwitcher = false },
            onSelect = { candidate ->
                showSpaceSwitcher = false
                selectedItem = null
                onSwitchConnection(candidate)
            },
        )
    } else if (showSpaceSwitcher) {
        val switcherConnections = sortWorkspaceSwitcherConnections(
            availableConnections,
            currentConnectionId = connection.connectionId,
        )
        WorkspaceSwitcherDialog(
            connections = switcherConnections,
            currentConnectionId = connection.connectionId,
            coachPerspective = coachPerspective,
            session = workspaceSession,
            workspaceItems = (switcherItems + feed).distinctBy(V2CoachWorkspaceItem::itemId),
            onDismiss = { showSpaceSwitcher = false },
            onSelect = { candidate ->
                showSpaceSwitcher = false
                onSwitchConnection(candidate)
            },
            onAddSpace = onAddSpace?.let { action ->
                {
                    showSpaceSwitcher = false
                    action()
                }
            },
        )
    }
    if (showPersonalRecordPicker) {
        PersonalRecordPickerDialog(
            records = personalRecords.filter(V2Memo::hasVideo).sortedByDescending(V2Memo::createdAt),
            sending = sending,
            onDismiss = { showPersonalRecordPicker = false },
            onSelect = ::preparePersonalRecord,
            onOpenMemo = {
                showPersonalRecordPicker = false
                onOpenMemo()
            },
        )
    }
    if (showStudentPostMenu) {
        StudentSpacePostMenuDialog(
            friendSpace = connection.relationshipType == "friend",
            onCapture = {
                showStudentPostMenu = false
                requestCamera()
            },
            onPickRecord = {
                showStudentPostMenu = false
                showPersonalRecordPicker = true
            },
            onDismiss = { showStudentPostMenu = false },
        )
    }
    if (showCoachContentSourceMenu) {
        CoachContentSourceSheet(
            onLibrary = {
                showCoachContentSourceMenu = false
                openCoachLibrary()
            },
            onAlbum = {
                showCoachContentSourceMenu = false
                coachEditLibraryAsset = null
                launchWorkspaceVideoPicker()
            },
            onDismiss = { showCoachContentSourceMenu = false },
        )
    }
    val coachTeachingVideo = pendingVideo?.takeIf {
        coachPerspective && trimmingVideo == null && coachEditLibraryAsset == null
    }
    val coachTeachingAsset = coachEditLibraryAsset?.takeIf { coachPerspective && trimmingVideo == null }
    if (coachTeachingVideo != null || coachTeachingAsset != null) {
        val inheritedTags = coachTeachingAsset?.tags.orEmpty().map(String::trim).filter(String::isNotEmpty)
        CoachTeachingContentEditor(
            coverModel = coachTeachingVideo?.cover ?: coachTeachingAsset?.coverUrl,
            mediaSource = coachTeachingVideo?.file?.toUri()?.toString() ?: coachTeachingAsset?.videoUrl,
            durationSeconds = coachTeachingVideo?.durationSeconds ?: coachTeachingAsset?.durationSeconds,
            sending = sending,
            sendLabel = if (connection.relationshipType == "friend") "发送给好友" else "发送给学员",
            notePrompt = if (connection.relationshipType == "friend") "这条内容想让对方重点看什么？" else "这条内容想让学员重点看什么？",
            initialNote = coachTeachingAsset?.title?.trim().orEmpty(),
            initialCategories = inheritedTags.filter { it in coachTeachingCategories }.toSet(),
            initialTags = inheritedTags.filter { it !in coachTeachingCategories },
            onCancel = {
                if (sending) return@CoachTeachingContentEditor
                coachEditLibraryAsset = null
                pendingVideo?.deleteTemporaryFiles()
                pendingVideo = null
                pendingVideoTitle = null
            },
            onSend = { note, categories, tags ->
                val description = note.trim()
                if (description.isEmpty()) return@CoachTeachingContentEditor
                val labels = (categories + tags).map(String::trim).filter(String::isNotEmpty).distinct().take(5)
                val video = coachTeachingVideo
                val asset = coachTeachingAsset
                if (video != null) {
                    sendVideo(video, note = description, tags = labels)
                } else if (asset != null) {
                    shareCoachLibraryAsset(asset, description)
                }
            },
        )
    }
    if (showRootQuickReplies && coachPerspective) {
        V2CoachQuickRepliesSheet(
            replies = rootQuickReplies,
            onRepliesChanged = { rootQuickReplies = it },
            onSelect = ::useRootQuickReply,
            onDismiss = { showRootQuickReplies = false },
        )
    }
    if (showCoachLibraryPicker) {
        CoachLibraryPickerDialog(
            assets = coachLibrary,
            loading = coachLibraryLoading,
            sending = sending,
            onDismiss = { if (!sending) showCoachLibraryPicker = false },
            onSelect = { asset ->
                showCoachLibraryPicker = false
                pendingVideo?.deleteTemporaryFiles()
                pendingVideo = null
                pendingVideoTitle = null
                coachEditLibraryAsset = asset
            },
            onAssetsChanged = { coachLibrary = it },
            teachingPick = true,
        )
    }

    if (pendingCellularJobId != null) {
        V2CellularUploadConfirmationDialog(
            fileSizeBytes = pendingCellularFileSize,
            busy = sending,
            sendMode = true,
            onConfirm = ::confirmCellularUpload,
            onDismiss = {
                pendingCellularJobId = null
                error = "视频已保留，连接 Wi-Fi 后会自动发送"
            },
        )
    }

    if (showDisconnectConfirmation) {
        V2WorkspaceDisconnectConfirmationSheet(
            title = workspaceDisconnectDialogTitle(
                friendSpace = connection.relationshipType == "friend",
                coachPerspective = coachPerspective,
            ),
            message = workspaceDisconnectDialogMessage(hasConnectionDrafts),
            busy = disconnecting,
            onConfirm = {
                disconnecting = true
                scope.launch {
                    ServiceLocator.coachWorkspaceRepo.disconnectConnection(connection.connectionId)
                        .onSuccess {
                            if (draftOwnerUserId.isNotBlank()) {
                                draftStore.clearConnection(draftOwnerUserId, connection.connectionId)
                            }
                            showDisconnectConfirmation = false
                            onDisconnected()
                        }
                        .onFailure { throwable ->
                            error = coachThrowableError("解除连接失败，请稍后重试", throwable)
                        }
                    disconnecting = false
                }
            },
            onDismiss = { showDisconnectConfirmation = false },
        )
    }

    trimmingVideo?.let { prepared ->
        V2SwingAnalysisTrimScreen(
            imported = prepared.asVideoImport(),
            saving = trimSaving,
            error = trimError,
            secondaryActionTitle = "不裁剪",
            onCancel = {
                prepared.deleteTemporaryFiles()
                if (pendingVideo == prepared) {
                    pendingVideo = null
                    pendingVideoTitle = null
                }
                trimmingVideo = null
                trimError = null
                showStudentSendConfirm = false
                if (coachPerspective) showCoachContentSourceMenu = true else showStudentPostMenu = true
            },
            onUse = { startMs, endMs ->
                if (trimSaving) return@V2SwingAnalysisTrimScreen
                trimSaving = true
                trimError = null
                scope.launch {
                    try {
                        val ready = trimPreparedWorkspaceVideo(context, prepared, startMs, endMs)
                        pendingVideo = ready
                        trimmingVideo = null
                        if (coachPerspective) {
                            coachEditLibraryAsset = null
                        } else {
                            showNewConversationComposer = false
                            showStudentSendConfirm = true
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (throwable: Throwable) {
                        trimError = coachThrowableError("视频裁剪失败，请重新选择或使用整段", throwable)
                    } finally {
                        trimSaving = false
                    }
                }
            },
            extraControls = if (coachPerspective) ({
                WorkspaceSaveToLibraryToggle(
                    checked = saveVideoToLibrary,
                    onCheckedChange = { saveVideoToLibrary = it },
                )
            }) else null,
        )
    }

    coverPickingVideo?.let { prepared ->
        V2WorkspaceVideoCoverPickerScreen(
            prepared = prepared,
            saving = coverSaving,
            error = coverError,
            onSkip = {
                if (!coverSaving) {
                    coverPickingVideo = null
                    coverError = null
                }
            },
            onConfirm = { selectedSeconds ->
                if (!coverSaving) {
                    coverSaving = true
                    coverError = null
                    scope.launch {
                        try {
                            val updated = replaceWorkspaceVideoCover(prepared, selectedSeconds)
                            if (pendingVideo?.uploadId == prepared.uploadId) pendingVideo = updated
                            coverPickingVideo = null
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (throwable: Throwable) {
                            coverError = coachThrowableError("封面暂时无法保存，请稍后重试", throwable)
                        } finally {
                            coverSaving = false
                        }
                    }
                }
            },
        )
    }
}

private val coachTeachingCategories = listOf("挥杆分析", "动作示范", "站位", "节奏", "下杆")

@Composable
internal fun CoachContentSourceSheet(
    onLibrary: () -> Unit,
    onAlbum: () -> Unit,
    onDismiss: () -> Unit,
) {
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val sheetInteraction = remember { MutableInteractionSource() }
    val scrimInteraction = remember { MutableInteractionSource() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            Modifier.fillMaxSize().testTag("coach-content-source-scrim")
                .clickable(interactionSource = scrimInteraction, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp)
                    .offset { IntOffset(0, dragOffsetPx.roundToInt()) }
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(DSColors.warmWhite)
                    .navigationBarsPadding()
                    .clickable(interactionSource = sheetInteraction, indication = null) { dragOffsetPx = 0f }
                    .pointerInput(onDismiss) {
                        detectVerticalDragGestures(
                            onDragCancel = { dragOffsetPx = 0f },
                            onDragEnd = {
                                if (dragOffsetPx >= 72.dp.toPx()) onDismiss() else dragOffsetPx = 0f
                            },
                        ) { change, amount ->
                            change.consume()
                            dragOffsetPx = (dragOffsetPx + amount).coerceAtLeast(0f)
                        }
                    }
                    .testTag("coach-content-source-sheet"),
            ) {
                Box(
                    Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                        .size(width = 36.dp, height = 5.dp).clip(CircleShape)
                        .background(DSColors.shadowGray.copy(alpha = 0.34f)),
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("选择内容来源", color = DSColors.deepGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    StudentSpacePostMenuRow(
                        title = "从素材选择",
                        detail = "从私有素材库中选择教学视频",
                        icon = Icons.Filled.Videocam,
                        onClick = onLibrary,
                    )
                    StudentSpacePostMenuRow(
                        title = "从相册选择",
                        detail = "从手机相册导入新视频",
                        icon = Icons.Filled.Image,
                        onClick = onAlbum,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp))
                            .background(DSColors.mistGreen.copy(alpha = 0.70f)).clickable(onClick = onDismiss),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text("取消", color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CoachTeachingContentEditor(
    coverModel: Any?,
    mediaSource: String?,
    durationSeconds: Double?,
    sending: Boolean,
    sendLabel: String,
    notePrompt: String,
    initialNote: String = "",
    initialCategories: Set<String> = emptySet(),
    initialTags: List<String> = emptyList(),
    onCancel: () -> Unit,
    onSend: (note: String, categories: List<String>, tags: List<String>) -> Unit,
) {
    var note by remember(initialNote) { mutableStateOf(initialNote.take(STUDENT_SPACE_SEND_NOTE_LIMIT)) }
    var categories by remember(initialCategories) { mutableStateOf(initialCategories) }
    var tags by remember(initialTags) { mutableStateOf(initialTags) }
    var tagDraft by remember { mutableStateOf("") }
    var playing by remember(mediaSource) { mutableStateOf(false) }
    Dialog(
        onDismissRequest = { if (!sending) onCancel() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        V2RoomPage(Modifier.fillMaxSize().testTag("coach-teaching-editor")) {
            Column(Modifier.fillMaxSize()) {
                V2NavTopBarCentered(title = "编辑教学内容", onBack = { if (!sending) onCancel() })
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black)
                            .aspectRatio(16f / 9f),
                    ) {
                        if (playing && !mediaSource.isNullOrBlank()) {
                            WorkspaceComposerRelatedMediaPreview(
                                mediaSource = mediaSource,
                                title = "教学视频",
                                aspectRatio = 16f / 9f,
                            )
                        } else {
                            if (coverModel != null) {
                                AsyncImage(coverModel, "教学视频", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Box(
                                Modifier.align(Alignment.Center).size(54.dp).clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.92f))
                                    .clickable(enabled = !mediaSource.isNullOrBlank()) { playing = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.PlayArrow, "播放", tint = DSColors.deepGreen, modifier = Modifier.size(32.dp))
                            }
                            durationSeconds?.takeIf { it > 0 }?.let { duration ->
                                Text(
                                    formatWorkspaceDuration(duration),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                                        .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.45f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("内容说明", color = DSColors.deepGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("必填", color = DSColors.shadowGray, fontSize = 12.sp)
                            Text("${note.length}/$STUDENT_SPACE_SEND_NOTE_LIMIT", color = DSColors.shadowGray, fontSize = 12.sp)
                        }
                        BasicTextField(
                            value = note,
                            onValueChange = { note = it.take(STUDENT_SPACE_SEND_NOTE_LIMIT) },
                            enabled = !sending,
                            textStyle = androidx.compose.ui.text.TextStyle(color = DSColors.inkGreen, fontSize = 14.sp, lineHeight = 20.sp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp).clip(RoundedCornerShape(14.dp))
                                .background(Color.White).border(1.dp, DSColors.borderLine, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("coach-teaching-note"),
                            decorationBox = { inner ->
                                Box {
                                    if (note.isBlank()) Text(notePrompt, color = DSColors.shadowGray, fontSize = 14.sp)
                                    inner()
                                }
                            },
                        )
                        Text(
                            "写一句话就行，大约 10～40 字，也可以写得更具体。",
                            color = DSColors.shadowGray,
                            fontSize = 12.sp,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("分类", color = DSColors.deepGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("选填。分享已有素材时会带上原来的分类。", color = DSColors.shadowGray, fontSize = 12.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            coachTeachingCategories.forEach { category ->
                                val selected = category in categories
                                Text(
                                    category,
                                    color = if (selected) Color.White else DSColors.deepGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) DSColors.deepGreen else Color.White)
                                        .border(1.dp, if (selected) DSColors.deepGreen else DSColors.borderLine, RoundedCornerShape(16.dp))
                                        .clickable(enabled = !sending) {
                                            categories = if (selected) categories - category else categories + category
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("标签", color = DSColors.deepGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            tags.forEach { tag ->
                                Text(
                                    tag,
                                    color = DSColors.deepGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(DSColors.mistGreen)
                                        .clickable(enabled = !sending) { tags = tags - tag }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                            if (tags.size < 5) {
                                Row(
                                    Modifier.height(36.dp).clip(RoundedCornerShape(16.dp)).background(Color.White)
                                        .border(1.dp, DSColors.borderLine, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    BasicTextField(
                                        value = tagDraft,
                                        onValueChange = { tagDraft = it.take(12) },
                                        enabled = !sending,
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(color = DSColors.inkGreen, fontSize = 13.sp),
                                        modifier = Modifier.width(72.dp),
                                        decorationBox = { inner ->
                                            Box {
                                                if (tagDraft.isBlank()) Text("添加标签", color = DSColors.shadowGray, fontSize = 13.sp)
                                                inner()
                                            }
                                        },
                                    )
                                    Text(
                                        "添加",
                                        color = DSColors.fairway,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable(enabled = !sending && tagDraft.isNotBlank()) {
                                            val next = tagDraft.trim()
                                            if (next.isNotEmpty() && next !in tags) tags = tags + next
                                            tagDraft = ""
                                        }.padding(start = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().background(DSColors.warmWhite).v2NavigationBarPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp))
                            .background(if (sending || note.isBlank()) DSColors.borderLine else DSColors.deepGreen)
                            .clickable(enabled = !sending && note.isNotBlank()) { onSend(note, categories.toList(), tags) }
                            .testTag("coach-teaching-send"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (sending) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(sendLabel, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Color.White)
                            .border(1.dp, DSColors.borderLine, RoundedCornerShape(22.dp))
                            .clickable(enabled = !sending, onClick = onCancel),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text("取消", color = DSColors.deepGreen, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun StudentSpacePostMenuDialog(
    friendSpace: Boolean,
    onCapture: () -> Unit,
    onPickRecord: () -> Unit,
    onDismiss: () -> Unit,
) {
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val sheetInteraction = remember { MutableInteractionSource() }
    val scrimInteraction = remember { MutableInteractionSource() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            Modifier.fillMaxSize().testTag("student-post-menu-scrim")
                .clickable(
                    interactionSource = scrimInteraction,
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 340.dp)
                    .offset { IntOffset(0, dragOffsetPx.roundToInt()) }
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(DSColors.warmWhite)
                    .navigationBarsPadding()
                    .clickable(
                        interactionSource = sheetInteraction,
                        indication = null,
                        onClick = { dragOffsetPx = 0f },
                    )
                    .pointerInput(onDismiss) {
                        detectVerticalDragGestures(
                            onDragCancel = { dragOffsetPx = 0f },
                            onDragEnd = {
                                if (dragOffsetPx >= 72.dp.toPx()) onDismiss()
                                else dragOffsetPx = 0f
                            },
                        ) { change, amount ->
                            change.consume()
                            dragOffsetPx = (dragOffsetPx + amount).coerceAtLeast(0f)
                        }
                    }
                    .testTag("student-post-menu-sheet"),
            ) {
                Box(
                    Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                        .size(width = 36.dp, height = 5.dp).clip(CircleShape)
                        .background(DSColors.shadowGray.copy(alpha = 0.34f))
                        .testTag("student-post-menu-drag-indicator"),
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("发内容", color = DSColors.deepGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (friendSpace) "发到当前好友空间，只在这段关系里可见。"
                            else "发给这位教练，只出现在当前教学空间。",
                            color = DSColors.shadowGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    StudentSpacePostMenuRow(
                        title = "拍挥杆视频",
                        detail = if (friendSpace) "现场拍一段，发到这个空间" else "现场拍一段，发给教练",
                        icon = Icons.Filled.Videocam,
                        onClick = onCapture,
                    )
                    StudentSpacePostMenuRow(
                        title = "选择练习记录",
                        detail = "从已保存的视频里选一条",
                        icon = Icons.Filled.PlayArrow,
                        showPlayRectangle = true,
                        onClick = onPickRecord,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp))
                            .background(DSColors.mistGreen.copy(alpha = 0.70f)).clickable(onClick = onDismiss),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text("取消", color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentSpacePostMenuRow(
    title: String,
    detail: String,
    icon: ImageVector,
    showPlayRectangle: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
            .border(1.dp, DSColors.borderLine.copy(alpha = 0.70f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(DSColors.mistGreen.copy(alpha = 0.80f)),
            contentAlignment = Alignment.Center,
        ) {
            if (showPlayRectangle) {
                Box(
                    Modifier.size(width = 17.dp, height = 13.dp).clip(RoundedCornerShape(2.dp))
                        .background(DSColors.fairway),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(9.dp))
                }
            } else {
                Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(16.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = DSColors.shadowGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = DSColors.shadowGray.copy(alpha = 0.70f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkspaceSwitcherDialog(
    connections: List<V2CoachConnection>,
    currentConnectionId: String,
    coachPerspective: Boolean,
    session: V2Session,
    workspaceItems: List<V2CoachWorkspaceItem>,
    onDismiss: () -> Unit,
    onSelect: (V2CoachConnection) -> Unit,
    onAddSpace: (() -> Unit)?,
) {
    val teachingConnections = connections.filter { it.relationshipType != "friend" }
    val friendConnections = connections.filter { it.relationshipType == "friend" }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DSColors.warmWhite,
        scrimColor = Color.Black.copy(alpha = 0.34f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = DSColors.shadowGray.copy(alpha = 0.45f)) },
    ) {
        Box(
            Modifier.fillMaxWidth().fillMaxHeight(0.90f).background(DSColors.warmWhite)
                .testTag("workspace-switcher-sheet"),
        ) {
            V2RoomBackground(Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize().v2NavigationBarPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 2.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(Color.White)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = DSColors.deepGreen,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    "切换挥呗空间",
                    color = DSColors.deepGreen,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(32.dp))
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .background(Color.White).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.size(46.dp).clip(CircleShape)
                                .border(1.5.dp, Color(0xFFC5A64A), CircleShape).padding(2.dp),
                        ) {
                            V2UserAvatar(
                                avatar = session.avatar,
                                modifier = Modifier.fillMaxSize(),
                                fallbackFontSize = 17.sp,
                                placeholderIconSize = 27.dp,
                                fallbackText = session.nickname ?: session.username ?: "挥呗用户",
                                fallbackColor = DSColors.sandBeige,
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                session.nickname?.takeIf(String::isNotBlank)
                                    ?: session.username?.takeIf(String::isNotBlank)
                                    ?: "挥呗用户",
                                color = DSColors.deepGreen,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (coachPerspective) "教练身份" else "学员身份",
                                color = DSColors.shadowGray,
                                fontSize = 12.sp,
                            )
                        }
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "当前身份",
                            tint = DSColors.fairway,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                if (teachingConnections.isNotEmpty()) {
                    item {
                        WorkspaceSwitcherSectionTitle(
                            if (coachPerspective) "我的学员" else "我的教练",
                            teachingConnections.size,
                        )
                    }
                    items(teachingConnections, key = { "teaching_${it.connectionId}" }) { candidate ->
                        WorkspaceSwitcherRow(
                            connection = candidate,
                            current = candidate.connectionId == currentConnectionId,
                            coachPerspective = coachPerspective,
                            workspaceItems = workspaceItems,
                            onClick = { onSelect(candidate) },
                        )
                    }
                }
                if (friendConnections.isNotEmpty()) {
                    item { WorkspaceSwitcherSectionTitle("我的好友", friendConnections.size) }
                    items(friendConnections, key = { "friend_${it.connectionId}" }) { candidate ->
                        WorkspaceSwitcherRow(
                            connection = candidate,
                            current = candidate.connectionId == currentConnectionId,
                            coachPerspective = coachPerspective,
                            workspaceItems = workspaceItems,
                            onClick = { onSelect(candidate) },
                        )
                    }
                }
            }
            onAddSpace?.let { action ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)
                        .height(48.dp).clip(RoundedCornerShape(24.dp))
                        .border(1.5.dp, DSColors.deepGreen, RoundedCornerShape(24.dp))
                        .clickable(onClick = action),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Add, null, tint = DSColors.deepGreen, modifier = Modifier.size(20.dp))
                    Text(
                        "添加新空间",
                        color = DSColors.deepGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 7.dp),
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun WorkspaceSwitcherSectionTitle(title: String, count: Int) {
    Text(
        "$title（$count）",
        color = DSColors.shadowGray,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun WorkspaceSwitcherRow(
    connection: V2CoachConnection,
    current: Boolean,
    coachPerspective: Boolean,
    workspaceItems: List<V2CoachWorkspaceItem>,
    onClick: () -> Unit,
) {
    val person = if (coachPerspective) connection.student else connection.coach ?: connection.peer
    val latest = workspaceItems.asSequence()
        .filter { it.connectionId == connection.connectionId && it.parentItemId == null && !it.isRemovedContent }
        .maxByOrNull { item -> item.latestActivityAt ?: item.createdAt.orEmpty() }
    val preview = V2CoachWorkspaceItem.visibleCopy(latest?.title)
        ?: if (connection.relationshipType == "friend") "好友空间" else "教学空间"
    val ringColor = if (connection.relationshipType == "friend") {
        DSColors.fairway.copy(alpha = 0.45f)
    } else {
        Color(0xFFC5A64A)
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
            .border(
                1.dp,
                if (current) DSColors.fairway.copy(alpha = 0.45f) else DSColors.borderLine,
                RoundedCornerShape(16.dp),
            )
            .clickable(enabled = !current, onClick = onClick).padding(13.dp)
            .semantics { selected = current }
            .testTag("workspace-switcher-row-${connection.connectionId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).border(1.5.dp, ringColor, CircleShape).padding(2.dp),
        ) {
            V2UserAvatar(
                person?.avatar,
                Modifier.fillMaxSize(),
                fallbackFontSize = 17.sp,
                placeholderIconSize = 27.dp,
                fallbackText = person?.nickname
                    ?: if (connection.relationshipType == "friend") "挥呗好友"
                    else if (coachPerspective) "挥呗学员" else "挥呗教练",
                fallbackColor = DSColors.sandBeige,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                person?.nickname
                    ?: if (connection.relationshipType == "friend") "挥呗好友" else if (coachPerspective) "挥呗学员" else "挥呗教练",
                color = DSColors.deepGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                preview,
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                coachStudentRelativeTime(connection.lastActivityAt ?: connection.acceptedAt),
                color = DSColors.shadowGray,
                fontSize = 11.sp,
            )
            if (current) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "当前空间",
                    tint = DSColors.fairway,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
/**
 * 空态仍保留「在练 / 在看」切换。当前 iOS `practicePage` 空态是「第一条交流会保存在这里」，
 * 这是页面版本差异，不按屏幕适配去改它的尺寸。
 */
internal fun WorkspaceTeachingHero(
    selectedContentMode: V2WorkspaceContentMode,
    coachPerspective: Boolean,
    friendSpace: Boolean,
    onSelectMode: (V2WorkspaceContentMode) -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
    ) {
        V2PageArtworkBackground(
            artwork = V2PageArtwork.FAIRWAY,
            tone = V2PageArtworkTone.WHITE_MIST,
            modifier = Modifier.fillMaxWidth().height(268.dp).clip(RoundedCornerShape(24.dp))
                .testTag("workspace-teaching-hero"),
            alignment = Alignment.CenterEnd,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (friendSpace) "好友空间" else "教学空间",
                        color = DSColors.deepGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (coachPerspective) "教练视角" else "学员视角",
                        color = DSColors.shadowGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.weight(1.75f).height(48.dp).clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.90f)).padding(4.dp),
                    ) {
                        listOf(V2WorkspaceContentMode.PRACTICE, V2WorkspaceContentMode.WATCHING).forEach { mode ->
                            val selected = selectedContentMode == mode
                            Box(
                                modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) DSColors.fairway else Color.Transparent)
                                    .clickable { onSelectMode(mode) }
                                    .testTag("workspace-mode-${mode.wireValue}"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    mode.title,
                                    color = if (selected) Color.White else DSColors.deepGreen,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.88f))
                            .clickable { onSelectMode(V2WorkspaceContentMode.WATCHING) }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            if (coachPerspective) "教学素材" else "去看看学习内容",
                            color = DSColors.deepGreen.copy(alpha = 0.76f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(" ›", color = DSColors.deepGreen.copy(alpha = 0.76f), fontSize = 17.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun WorkspaceCurrentEmptyStateCard(
    coachPerspective: Boolean,
    friendSpace: Boolean,
) {
    val title = when {
        friendSpace -> "这是你们的专属分享空间"
        coachPerspective -> "这是你和学员的专属练习空间"
        else -> "这是你和教练的专属练习空间"
    }
    val description = when {
        friendSpace -> "练习、下场记录和想说的话会整理在这里，只在这段关系里可见，不会带到其他空间。"
        coachPerspective -> "学员发来的挥杆视频、练习记录和提问会整理在这里，方便持续跟进、集中反馈，也不会和其他关系混在一起。"
        else -> "你发给教练的挥杆视频和练习记录会整理在这里，方便持续跟进，也不会和其他关系混在一起。"
    }
    val footnote = when {
        friendSpace -> "从一条记录开始，慢慢留下来"
        coachPerspective -> "从一条练习开始，逐步形成这份专属教学记录"
        else -> "从一条练习开始，逐步形成你的专属教学记录"
    }
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = Modifier.fillMaxWidth().clip(shape).background(DSColors.cardWhite)
            .border(1.dp, DSColors.borderLine.copy(alpha = 0.62f), shape)
            .padding(horizontal = 22.dp, vertical = 22.dp)
            .testTag("workspace-current-empty-card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.room_bg_2),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().height(190.dp),
        )
        Text(
            title,
            color = DSColors.inkGreen,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            description,
            color = DSColors.shadowGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(28.dp, 1.dp).background(DSColors.listCardBorder))
            Text(
                footnote,
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Box(Modifier.size(28.dp, 1.dp).background(DSColors.listCardBorder))
        }
    }
}

@Composable
internal fun WorkspaceWatchingEmptyStateCard(
    coachPerspective: Boolean,
    friendSpace: Boolean,
) {
    val cardShape = RoundedCornerShape(22.dp)
    val title = when {
        friendSpace -> "这是你们的专属学习空间"
        coachPerspective -> "这是你和学员的专属学习空间"
        else -> "这是你和教练的专属学习空间"
    }
    val description = when {
        friendSpace -> "彼此分享的示范视频和学习资料会集中保存在这里，只在这段关系里可见。"
        coachPerspective -> "你分享给学员的示范视频和教学资料会整理在这里，方便围绕练习重点持续讲解。"
        else -> "教练分享的示范视频和学习资料会集中保存在这里，方便随时回看，也不会和其他空间混在一起。"
    }
    val footnote = when {
        friendSpace -> "从一条分享开始，慢慢留下来"
        coachPerspective -> "从一份教学素材开始，逐步形成专属学习记录"
        else -> "教练分享后，会集中留在这个空间里"
    }
    Column(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, cardShape).clip(cardShape)
            .background(Color.White).border(1.dp, DSColors.borderLine, cardShape)
            .testTag("workspace-watching-empty-card")
            .padding(horizontal = 22.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.room_bg_2),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().height(190.dp),
        )
        Text(
            title,
            color = DSColors.inkGreen,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            description,
            color = DSColors.shadowGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(28.dp, 1.dp).background(DSColors.borderLine))
            Text(
                footnote,
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Box(Modifier.size(28.dp, 1.dp).background(DSColors.borderLine))
        }
    }
}

@Composable
internal fun WorkspaceEmptyStateCard(
    coachPerspective: Boolean,
    friendSpace: Boolean,
    onUploadPractice: (() -> Unit)? = null,
    onStartFromLibrary: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(22.dp)
    val title = when {
        friendSpace -> "开始你们的第一条分享记录"
        coachPerspective -> "等待学员的第一条挥杆记录"
        else -> "开始你的第一条挥杆记录"
    }
    val description = when {
        friendSpace -> "上传练习视频后，你们可以在这里交流动作、保存反馈，一起留下每次进步。"
        coachPerspective -> "学员上传练习视频后，你可以在这里查看挥杆动作并提供专业指导。"
        else -> "上传练习视频后，教练可以在这里查看你的挥杆动作并提供专业指导，帮助你持续进步。"
    }

    Column(
        modifier = modifier.fillMaxWidth()
            .shadow(
                elevation = 5.dp,
                shape = cardShape,
                ambientColor = DSColors.deepGreen.copy(alpha = 0.07f),
                spotColor = DSColors.deepGreen.copy(alpha = 0.10f),
            )
            .clip(cardShape)
            .background(Color.White)
            .border(1.dp, DSColors.borderLine, cardShape)
            .testTag("workspace-guided-empty-card")
            .padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        WorkspaceEmptySwingIllustration(
            modifier = Modifier.fillMaxWidth(0.58f).height(76.dp),
        )
        Text(
            "—  还没有练习内容  —",
            color = DSColors.shadowGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Text(
            title,
            color = DSColors.deepGreen,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            description,
            color = DSColors.shadowGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
        )

        onUploadPractice?.let { action ->
            Row(
                modifier = Modifier.fillMaxWidth(0.72f).height(44.dp).clip(RoundedCornerShape(22.dp))
                    .background(DSColors.fairway).clickable(onClick = action)
                    .testTag("workspace-empty-upload"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.CloudUpload, null, tint = Color.White, modifier = Modifier.size(21.dp))
                Text(
                    if (coachPerspective) "发布练习内容" else "上传练习视频",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 9.dp),
                )
            }
        }
        onStartFromLibrary?.let { action ->
            Row(
                modifier = Modifier.fillMaxWidth(0.72f).height(44.dp).clip(RoundedCornerShape(22.dp))
                    .border(1.2.dp, DSColors.fairway, RoundedCornerShape(22.dp))
                    .clickable(onClick = action)
                    .testTag("workspace-empty-library"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Videocam, null, tint = DSColors.fairway, modifier = Modifier.size(21.dp))
                Text(
                    if (coachPerspective) "从教学素材开始" else "从素材开始",
                    color = DSColors.fairway,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 9.dp),
                )
            }
        }

        val benefits = when {
            friendSpace -> listOf(
                Triple("记录挥杆", "留住每一次进步", Icons.Filled.BarChart),
                Triple("交流动作", "彼此分享建议", Icons.Outlined.Person),
                Triple("共同进步", "一起看见变化", Icons.AutoMirrored.Filled.ShowChart),
            )
            coachPerspective -> listOf(
                Triple("查看挥杆", "快速了解动作", Icons.Filled.BarChart),
                Triple("提供指导", "给出专业点评", Icons.Outlined.Person),
                Triple("持续跟进", "看见学员进步", Icons.AutoMirrored.Filled.ShowChart),
            )
            else -> listOf(
                Triple("记录挥杆", "留住每一次进步", Icons.Filled.BarChart),
                Triple("获得指导", "教练专业点评", Icons.Outlined.Person),
                Triple("持续改进", "看见更好的自己", Icons.AutoMirrored.Filled.ShowChart),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(DSColors.borderLine))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WorkspaceEmptyBenefit(benefits[0].third, benefits[0].first, benefits[0].second, Modifier.weight(1f))
            Box(Modifier.size(1.dp, 44.dp).background(DSColors.borderLine))
            WorkspaceEmptyBenefit(benefits[1].third, benefits[1].first, benefits[1].second, Modifier.weight(1f))
            Box(Modifier.size(1.dp, 44.dp).background(DSColors.borderLine))
            WorkspaceEmptyBenefit(benefits[2].third, benefits[2].first, benefits[2].second, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WorkspaceEmptySwingIllustration(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.testTag("workspace-empty-swing-illustration")) {
        val w = size.width
        val h = size.height

        // The quiet mint landscape keeps the illustration decorative, while the
        // club and ball explain the empty state at a glance without another label.
        drawOval(
            color = Color(0xFFEAF5EC),
            topLeft = Offset(w * 0.20f, h * 0.20f),
            size = Size(w * 0.60f, h * 0.58f),
        )
        drawCircle(Color(0xFFE3F1E6), radius = h * 0.22f, center = Offset(w * 0.69f, h * 0.57f))
        drawCircle(Color(0xFFF1F8F2), radius = h * 0.18f, center = Offset(w * 0.38f, h * 0.59f))
        drawOval(
            color = Color(0xFFC8E5CD),
            topLeft = Offset(w * 0.14f, h * 0.72f),
            size = Size(w * 0.72f, h * 0.14f),
        )

        val grass = Color(0xFF72A77C)
        listOf(
            0.23f to 0.68f,
            0.31f to 0.70f,
            0.69f to 0.68f,
            0.78f to 0.71f,
        ).forEach { (x, y) ->
            drawLine(
                color = grass,
                start = Offset(w * x, h * 0.80f),
                end = Offset(w * (x - 0.012f), h * y),
                strokeWidth = h * 0.018f,
                cap = StrokeCap.Round,
            )
        }

        drawLine(
            color = Color(0xFF52686C),
            start = Offset(w * 0.72f, h * 0.10f),
            end = Offset(w * 0.55f, h * 0.66f),
            strokeWidth = h * 0.045f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = DSColors.deepGreen,
            start = Offset(w * 0.55f, h * 0.67f),
            end = Offset(w * 0.45f, h * 0.71f),
            strokeWidth = h * 0.13f,
            cap = StrokeCap.Round,
        )

        val ballCenter = Offset(w * 0.515f, h * 0.70f)
        drawCircle(
            Color(0xFFD6E4DA),
            radius = h * 0.105f,
            center = Offset(ballCenter.x, ballCenter.y + h * 0.018f),
        )
        drawCircle(Color.White, radius = h * 0.098f, center = ballCenter)
        drawCircle(Color(0xFFD9E5DE), radius = h * 0.012f, center = Offset(ballCenter.x - h * 0.030f, ballCenter.y - h * 0.025f))
        drawCircle(Color(0xFFD9E5DE), radius = h * 0.010f, center = Offset(ballCenter.x + h * 0.032f, ballCenter.y + h * 0.008f))

        val impact = Color(0xFF9EC8A7)
        drawLine(impact, Offset(w * 0.39f, h * 0.52f), Offset(w * 0.36f, h * 0.42f), h * 0.018f, StrokeCap.Round)
        drawLine(impact, Offset(w * 0.33f, h * 0.58f), Offset(w * 0.27f, h * 0.53f), h * 0.018f, StrokeCap.Round)
    }
}

@Composable
private fun WorkspaceEmptyBenefit(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(48.dp).padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(DSColors.mistGreen.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                color = DSColors.deepGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                subtitle,
                color = DSColors.shadowGray,
                fontSize = 8.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun WorkspaceRootComposerCard(
    coachPerspective: Boolean,
    friendSpace: Boolean,
    draft: String,
    onDraftChange: (String) -> Unit,
    textLimit: Int,
    requestCoachReview: Boolean,
    onRequestCoachReviewChange: (Boolean) -> Unit,
    pendingVideo: Boolean,
    customCoverSelected: Boolean,
    sending: Boolean,
    toolsExpanded: Boolean,
    focusRequester: FocusRequester,
    onCollapse: () -> Unit,
    onToggleTools: () -> Unit,
    onInputFocused: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRoleLibrary: () -> Unit,
    onQuickReplyOrRecord: () -> Unit,
    onSelectCover: () -> Unit,
    onRemoveVideo: () -> Unit,
    onSend: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp)).background(Color.White)
            .border(1.dp, DSColors.borderLine.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    if (coachPerspective) Icons.Filled.Videocam else Icons.Filled.ChatBubble,
                    null,
                    tint = DSColors.fairway,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    when {
                        friendSpace -> "发给好友"
                        coachPerspective -> "发给学员的视频"
                        else -> "发给教练"
                    },
                    color = DSColors.fairway,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (!pendingVideo && !sending && (coachPerspective || draft.isBlank())) {
                TextButton(onClick = onCollapse, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("收起", color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (pendingVideo) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                    .background(DSColors.mistGreen.copy(alpha = 0.58f)).padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Videocam, null, tint = DSColors.fairway, modifier = Modifier.size(18.dp))
                Text(
                    "视频已添加",
                    color = DSColors.deepGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 7.dp),
                )
                TextButton(onClick = onSelectCover, enabled = !sending) {
                    Text(if (customCoverSelected) "换封面" else "选封面", fontSize = 10.sp)
                }
                TextButton(onClick = onRemoveVideo, enabled = !sending) {
                    Text("移除", color = DSColors.flagRed, fontSize = 10.sp)
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(DSColors.mistGreen.copy(alpha = 0.72f))
                        .clickable(enabled = !sending, onClick = onToggleTools)
                        .testTag("workspace-root-toggle-tools"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (toolsExpanded) Icons.Filled.Close else Icons.Filled.Add,
                        if (toolsExpanded) "收起添加选项" else "添加视频",
                        tint = DSColors.fairway,
                        modifier = Modifier.size(19.dp),
                    )
                }
                if (pendingVideo && !toolsExpanded) {
                    Box(
                        Modifier.offset(x = 3.dp, y = (-3).dp).size(17.dp).clip(CircleShape)
                            .background(DSColors.trophyGold).border(2.dp, DSColors.cardWhite, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("1", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (!coachPerspective) {
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                enabled = !sending,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = DSColors.inkGreen,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                ),
                modifier = Modifier.weight(1f).heightIn(min = 42.dp).clip(RoundedCornerShape(15.dp))
                    .background(DSColors.warmWhite)
                    .border(0.8.dp, DSColors.borderLine.copy(alpha = 0.7f), RoundedCornerShape(15.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused) onInputFocused() }
                    .testTag("workspace-root-composer-input"),
                maxLines = 4,
                decorationBox = { innerTextField ->
                    Box(Modifier.fillMaxWidth()) {
                        if (draft.isBlank()) {
                            Text(
                                if (friendSpace) "给好友发一条新内容…" else "发一条练习或消息…",
                                color = DSColors.shadowGray,
                                fontSize = 13.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            } else {
                Spacer(Modifier.weight(1f))
            }
            val canSend = !sending && if (coachPerspective) pendingVideo else draft.isNotBlank() || pendingVideo
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(if (canSend) DSColors.fairway else DSColors.borderLine)
                    .clickable(enabled = canSend, onClick = onSend)
                    .testTag("workspace-root-send"),
                contentAlignment = Alignment.Center,
            ) {
                if (sending) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.ArrowUpward, "发送", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (!coachPerspective && !friendSpace) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("需要教练点评", color = DSColors.deepGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("关闭后只记在这个空间，不进入教练的点评队列", color = DSColors.shadowGray, fontSize = 9.sp)
                }
                Switch(
                    checked = requestCoachReview,
                    onCheckedChange = onRequestCoachReviewChange,
                    enabled = !sending,
                )
            }
        }
        if (toolsExpanded && !sending) {
            Row(
                Modifier.fillMaxWidth().testTag("workspace-root-source-tools"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (coachPerspective) {
                    WorkspaceQuickPostSourceButton(
                        label = "相册",
                        icon = Icons.Filled.PhotoLibrary,
                        testTag = "workspace-root-source-album",
                        modifier = Modifier.weight(1f),
                        onClick = onGallery,
                    )
                    WorkspaceQuickPostSourceButton(
                        label = "素材库",
                        icon = Icons.AutoMirrored.Filled.LibraryBooks,
                        testTag = "workspace-root-source-library",
                        modifier = Modifier.weight(1f),
                        onClick = onRoleLibrary,
                    )
                } else {
                WorkspaceQuickPostSourceButton(
                    label = "拍练习",
                    icon = Icons.Filled.Videocam,
                    testTag = "workspace-root-source-camera",
                    modifier = Modifier.weight(1f),
                    onClick = onCamera,
                )
                WorkspaceQuickPostSourceButton(
                    label = "相册",
                    icon = Icons.Filled.PhotoLibrary,
                    testTag = "workspace-root-source-album",
                    modifier = Modifier.weight(1f),
                    onClick = onGallery,
                )
                WorkspaceQuickPostSourceButton(
                    label = "记录",
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    testTag = "workspace-root-source-record",
                    modifier = Modifier.weight(1f),
                    onClick = onRoleLibrary,
                )
                }
            }
        }
        if (!coachPerspective && pendingVideo) {
            Text(
                "${draft.length}/$textLimit",
                color = DSColors.shadowGray,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun WorkspaceQuickPostSourceButton(
    label: String,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier.height(42.dp).clip(RoundedCornerShape(12.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.70f))
            .clickable(onClick = onClick).testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(14.dp))
        Text(
            label,
            color = DSColors.fairway,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun WorkspaceSourceOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DSColors.mistGreen)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(22.dp))
        Text(title, color = DSColors.deepGreen, fontWeight = FontWeight.Bold)
    }
}

internal fun coachTeachingLibraryAssets(
    assets: List<V2CoachLibraryAsset>,
    filter: String,
): List<V2CoachLibraryAsset> = when (filter) {
    "视频" -> assets.filter(V2CoachLibraryAsset::isLibraryVideoAsset)
    "常用" -> assets.filter { it.useCount > 0 }
    "最近" -> assets.sortedByDescending { it.lastUsedAt ?: it.updatedAt ?: it.createdAt.orEmpty() }
    else -> assets
}

@Composable
private fun CoachTeachingLibraryCard(
    asset: V2CoachLibraryAsset,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
            .border(1.dp, if (selected) DSColors.fairway else DSColors.borderLine.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp)
            .testTag("coach-library-grid-${asset.assetId}"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(12.dp)).background(DSColors.mistGreen),
        ) {
            AsyncImage(asset.coverUrl, asset.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp).clip(CircleShape).background(DSColors.fairway),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, "已选择", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            asset.durationSeconds?.takeIf { it > 0 }?.let { duration ->
                Text(
                    formatWorkspaceDuration(duration),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                        .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            asset.title.ifBlank { "教学素材" },
            color = DSColors.deepGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (asset.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                asset.tags.take(2).forEach { tag ->
                    Text(
                        tag,
                        color = DSColors.fairway,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(DSColors.mistGreen.copy(alpha = 0.7f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Text("使用 ${asset.useCount} 次", color = DSColors.shadowGray, fontSize = 11.sp)
    }
}

@Composable
internal fun CoachLibraryPickerDialog(
    assets: List<V2CoachLibraryAsset>,
    loading: Boolean,
    sending: Boolean,
    onDismiss: () -> Unit,
    onSelect: (V2CoachLibraryAsset) -> Unit,
    onAssetsChanged: (List<V2CoachLibraryAsset>) -> Unit = {},
    selectedAssetIds: Set<String> = emptySet(),
    maxSelectedCount: Int = 1,
    multiSelect: Boolean = false,
    loadLibraryPage: suspend (query: String, cursor: String?) -> Result<V2CoachPage<V2CoachLibraryAsset>> = { queryValue, cursorValue ->
        runCatching {
            ServiceLocator.coachWorkspaceRepo.libraryPage(query = queryValue, cursor = cursorValue, limit = 20)
        }
    },
    loadLibraryAsset: suspend (assetId: String) -> Result<V2CoachLibraryAsset> = { assetId ->
        ServiceLocator.coachWorkspaceRepo.coachAsset(assetId)
    },
    teachingPick: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var shownAssets by remember { mutableStateOf(assets) }
    var pickerLoading by remember { mutableStateOf(loading) }
    var pickerLoadingMore by remember { mutableStateOf(false) }
    var pickerError by remember { mutableStateOf<String?>(null) }
    var loadRevision by remember { mutableIntStateOf(0) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var expandedAssetId by rememberSaveable { mutableStateOf<String?>(null) }
    var loadingPreviewAssetId by remember { mutableStateOf<String?>(null) }
    var libraryFilter by rememberSaveable { mutableStateOf("全部") }
    var pickedAssetId by rememberSaveable { mutableStateOf<String?>(null) }

    fun replaceAsset(updated: V2CoachLibraryAsset) {
        shownAssets = shownAssets.map { current ->
            if (current.assetId == updated.assetId) updated else current
        }
        onAssetsChanged(shownAssets)
    }

    fun loadMore() {
        val cursor = nextCursor ?: return
        if (pickerLoading || pickerLoadingMore) return
        pickerLoadingMore = true
        scope.launch {
            loadLibraryPage(query, cursor).onSuccess { page ->
                shownAssets = (shownAssets + page.values).distinctBy(V2CoachLibraryAsset::assetId)
                nextCursor = page.nextCursor
                pickerError = null
                onAssetsChanged(shownAssets)
            }.onFailure { throwable ->
                pickerError = coachThrowableError("素材库暂时无法加载，请稍后重试", throwable)
            }
            pickerLoadingMore = false
        }
    }

    fun togglePreview(asset: V2CoachLibraryAsset) {
        if (expandedAssetId == asset.assetId) {
            expandedAssetId = null
            return
        }
        if (!asset.videoUrl.isNullOrBlank()) {
            expandedAssetId = asset.assetId
            return
        }
        if (loadingPreviewAssetId != null) return
        loadingPreviewAssetId = asset.assetId
        scope.launch {
            loadLibraryAsset(asset.assetId)
                .onSuccess { refreshed ->
                    replaceAsset(refreshed)
                    if (!refreshed.videoUrl.isNullOrBlank()) expandedAssetId = refreshed.assetId
                }
                .onFailure {
                    pickerError = "素材预览暂时无法加载，可稍后重试"
                }
            loadingPreviewAssetId = null
        }
    }

    LaunchedEffect(assets) {
        if (query.isBlank() && assets.isNotEmpty()) shownAssets = assets
    }
    LaunchedEffect(query, loadRevision) {
        delay(250)
        pickerLoading = true
        pickerError = null
        loadLibraryPage(query, null).onSuccess { page ->
            shownAssets = page.values
            nextCursor = page.nextCursor
            onAssetsChanged(page.values)
        }.onFailure { throwable ->
            pickerError = coachThrowableError("素材库暂时无法加载，请稍后重试", throwable)
        }
        pickerLoading = false
    }

    val visibleAssets = if (teachingPick) coachTeachingLibraryAssets(shownAssets, libraryFilter) else shownAssets
    val selectionHint = when {
        selectedAssetIds.size >= maxSelectedCount -> "已选满，可点减号调整"
        multiSelect && selectedAssetIds.isEmpty() -> "可连续选择"
        multiSelect -> "点减号可移除"
        else -> "带勾素材已加入"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        V2RoomPage(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                V2NavTopBarCentered(
                    title = if (teachingPick) "选择素材" else "选择教学素材",
                    onBack = onDismiss,
                )
                if (pickerError != null && shownAssets.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("coach-library-picker-stale"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Refresh, null, tint = DSColors.inkGreen, modifier = Modifier.size(16.dp))
                        Text("当前显示上次的素材", color = DSColors.inkGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text(
                            "重试",
                            color = DSColors.fairway,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(enabled = !pickerLoading) { loadRevision++ }.padding(6.dp),
                        )
                    }
                }

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!teachingPick && selectedAssetIds.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(38.dp)
                                .clip(RoundedCornerShape(12.dp)).background(DSColors.mistGreen.copy(alpha = 0.68f))
                                .padding(horizontal = 12.dp).testTag("coach-library-picker-selection-summary"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = DSColors.fairway, modifier = Modifier.size(17.dp))
                            Text("本次内容已加入 ${selectedAssetIds.size} 条素材", color = DSColors.inkGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(selectionHint, color = DSColors.shadowGray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            .height(44.dp).clip(RoundedCornerShape(14.dp)).background(DSColors.cardWhite)
                            .border(1.dp, DSColors.borderLine, RoundedCornerShape(14.dp)).padding(horizontal = 13.dp)
                            .testTag("coach-library-picker-search"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Icon(Icons.Filled.Search, null, tint = DSColors.shadowGray, modifier = Modifier.size(18.dp))
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    if (teachingPick) "搜索素材名称、标签或内容" else "搜索标题或标签（如：下杆、挥杆轨迹）",
                                    color = DSColors.shadowGray,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it.take(60) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = DSColors.inkGreen, fontSize = 13.sp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("coach-library-picker-search-field"),
                            )
                        }
                        if (query.isNotEmpty()) {
                            Icon(
                                Icons.Filled.Cancel,
                                "清除搜索",
                                tint = DSColors.shadowGray.copy(alpha = 0.75f),
                                modifier = Modifier.size(18.dp).clickable { query = "" },
                            )
                        }
                    }
                    if (teachingPick) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("全部", "视频", "常用", "最近").forEach { filter ->
                                val selected = libraryFilter == filter
                                Text(
                                    filter,
                                    color = if (selected) Color.White else DSColors.deepGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) DSColors.deepGreen else Color.White)
                                        .border(1.dp, if (selected) DSColors.deepGreen else DSColors.borderLine, RoundedCornerShape(16.dp))
                                        .clickable { libraryFilter = filter }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                        .testTag("coach-library-filter-$filter"),
                                )
                            }
                        }
                    }

                    when {
                        pickerLoading && shownAssets.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(Modifier.size(22.dp), color = DSColors.fairway, strokeWidth = 2.dp)
                                Text("正在读取素材…", color = DSColors.shadowGray, fontSize = 13.sp)
                            }
                        }
                        pickerError != null && shownAssets.isEmpty() -> Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 26.dp)
                                .clip(RoundedCornerShape(20.dp)).background(DSColors.cardWhite).padding(horizontal = 18.dp, vertical = 26.dp)
                                .testTag("coach-library-picker-unavailable"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(13.dp),
                        ) {
                            Box(Modifier.size(62.dp).clip(CircleShape).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Refresh, null, tint = DSColors.fairway, modifier = Modifier.size(27.dp))
                            }
                            Text("素材暂时没有同步下来", color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("已经保存的素材不会丢失；也可以先返回，直接从相册发送。", color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                            Text("点击重新加载即可继续", color = DSColors.shadowGray.copy(alpha = 0.72f), fontSize = 9.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { loadRevision++ },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = DSColors.fairway),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                ) { Text("重新加载", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                TextButton(onClick = onDismiss) { Text("返回用相册", color = DSColors.fairway, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                        shownAssets.isEmpty() -> Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 28.dp)
                                .clip(RoundedCornerShape(20.dp)).background(DSColors.cardWhite).padding(vertical = 28.dp)
                                .testTag("coach-library-picker-empty"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(Modifier.size(62.dp).clip(CircleShape).background(DSColors.mistGreen), contentAlignment = Alignment.Center) {
                                Icon(if (query.isEmpty()) Icons.AutoMirrored.Filled.LibraryBooks else Icons.Filled.Search, null, tint = DSColors.fairway, modifier = Modifier.size(26.dp))
                            }
                            Text(if (query.isEmpty()) "还没有私有素材" else "没有找到素材", color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (query.isEmpty()) "可先返回并从相册发送，勾选“存素材”后即可反复使用。" else "换个标题或标签试试。",
                                color = DSColors.shadowGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(max = 250.dp),
                            )
                            if (query.isNotEmpty()) TextButton(onClick = { query = "" }) { Text("清除搜索", color = DSColors.fairway, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                        teachingPick && visibleAssets.isEmpty() -> Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 28.dp)
                                .clip(RoundedCornerShape(20.dp)).background(DSColors.cardWhite).padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("这个分类下没有素材", color = DSColors.inkGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        teachingPick -> LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize().testTag("coach-library-picker-grid"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            lazyGridItems(visibleAssets, key = V2CoachLibraryAsset::assetId) { asset ->
                                val selected = asset.assetId == pickedAssetId
                                CoachTeachingLibraryCard(
                                    asset = asset,
                                    selected = selected,
                                    enabled = !sending,
                                    onClick = {
                                        pickedAssetId = if (selected) null else asset.assetId
                                    },
                                )
                                if (asset.assetId == visibleAssets.lastOrNull()?.assetId && nextCursor != null) {
                                    LaunchedEffect(asset.assetId, nextCursor) { loadMore() }
                                }
                            }
                            if (pickerLoadingMore) {
                                item(span = { GridItemSpan(2) }) {
                                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(Modifier.size(20.dp), color = DSColors.fairway, strokeWidth = 2.dp)
                                    }
                                }
                            }
                        }
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize().testTag("coach-library-picker-list"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(shownAssets, key = V2CoachLibraryAsset::assetId) { asset ->
                                val selected = asset.assetId in selectedAssetIds
                                val atCapacity = selectedAssetIds.size >= maxSelectedCount
                                val expanded = expandedAssetId == asset.assetId
                                Column(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(DSColors.cardWhite)
                                        .border(1.dp, if (selected) DSColors.fairway.copy(alpha = 0.5f) else DSColors.borderLine, RoundedCornerShape(17.dp))
                                        .padding(11.dp).testTag("coach-library-picker-asset-${asset.assetId}"),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    if (expanded && !asset.videoUrl.isNullOrBlank()) {
                                        WorkspaceComposerRelatedMediaPreview(
                                            mediaSource = requireNotNull(asset.videoUrl),
                                            title = asset.title.ifBlank { "教学素材" },
                                            aspectRatio = if (
                                                asset.width != null && asset.width > 0 &&
                                                asset.height != null && asset.height > 0
                                            ) {
                                                asset.width.toFloat() / asset.height.toFloat()
                                            } else {
                                                16f / 9f
                                            },
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(
                                            Modifier.size(width = 88.dp, height = 66.dp).clip(RoundedCornerShape(12.dp))
                                                .background(DSColors.borderLine).clickable(enabled = loadingPreviewAssetId == null || loadingPreviewAssetId == asset.assetId) { togglePreview(asset) }
                                                .testTag("coach-library-picker-preview-${asset.assetId}"),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            AsyncImage(asset.coverUrl, asset.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                            Box(Modifier.size(29.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.48f)), contentAlignment = Alignment.Center) {
                                                if (loadingPreviewAssetId == asset.assetId) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 1.5.dp)
                                                else Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(17.dp))
                                            }
                                            asset.durationSeconds?.takeIf { it > 0 }?.let { duration ->
                                                Text(
                                                    formatWorkspaceDuration(duration),
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp)
                                                        .clip(CircleShape).background(Color.Black.copy(alpha = 0.58f)).padding(horizontal = 5.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                        Row(
                                            Modifier.weight(1f).clickable(
                                                enabled = !sending && (selected || !atCapacity),
                                            ) {
                                                onSelect(asset)
                                                if (!multiSelect) onDismiss()
                                            }.padding(vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Text(asset.title.ifBlank { "教学素材" }, color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                if (asset.tags.isNotEmpty()) Text(asset.tags.joinToString("  ") { "#$it" }, color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                if (asset.useCount > 0) Text("已使用 ${asset.useCount} 次", color = DSColors.shadowGray.copy(alpha = 0.82f), fontSize = 9.sp)
                                            }
                                            Icon(
                                                when {
                                                    selected && multiSelect -> Icons.Filled.RemoveCircle
                                                    selected -> Icons.Filled.CheckCircle
                                                    atCapacity -> Icons.Filled.Cancel
                                                    else -> Icons.Filled.Add
                                                },
                                                if (selected && multiSelect) "移除素材" else "选择素材",
                                                tint = if (atCapacity && !selected) DSColors.shadowGray else DSColors.fairway,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                }
                                if (asset.assetId == shownAssets.lastOrNull()?.assetId && nextCursor != null) {
                                    LaunchedEffect(asset.assetId, nextCursor) { loadMore() }
                                }
                            }
                            if (pickerLoadingMore) item("loading-more") {
                                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(20.dp), color = DSColors.fairway, strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
                if (teachingPick) {
                    val picked = shownAssets.firstOrNull { it.assetId == pickedAssetId }
                    Box(
                        Modifier.fillMaxWidth().background(DSColors.warmWhite).v2NavigationBarPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp))
                                .background(if (picked == null || sending) DSColors.borderLine else DSColors.deepGreen)
                                .clickable(enabled = picked != null && !sending) {
                                    picked?.let(onSelect)
                                }
                                .testTag("coach-library-picker-next"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "下一步（已选择${if (picked == null) 0 else 1}个）",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                } else if (multiSelect) {
                    Row(
                        Modifier.fillMaxWidth().background(DSColors.cardWhite).padding(horizontal = 16.dp, vertical = 10.dp)
                            .v2NavigationBarPadding().testTag("coach-library-picker-done-bar"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("已选择 ${selectedAssetIds.size} 个素材", color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Button(
                            onClick = onDismiss,
                            enabled = !sending,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = DSColors.deepGreen),
                            contentPadding = PaddingValues(horizontal = 22.dp),
                            modifier = Modifier.height(44.dp).testTag("coach-library-picker-done"),
                        ) { Text("完成选择", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                    }
                } else {
                    Spacer(Modifier.v2NavigationBarPadding())
                }
            }
        }
    }
}

@Composable
internal fun PersonalRecordPickerDialog(
    records: List<V2Memo>,
    sending: Boolean,
    onDismiss: () -> Unit,
    onSelect: (V2Memo) -> Unit,
    onOpenMemo: () -> Unit,
) {
    var unavailableMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { if (!sending) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        V2RoomPage(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.fillMaxWidth().v2StatusBarPadding().height(64.dp),
                ) {
                    Text(
                        "取消",
                        color = DSColors.deepGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.CenterStart).clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !sending, onClick = onDismiss)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text("选择练习记录", color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "从已保存的视频里选一条，发到这个空间",
                            color = DSColors.shadowGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            if (records.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().v2NavigationBarPadding().padding(horizontal = 28.dp)
                        .testTag("personal-record-picker-empty"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                ) {
                    Icon(Icons.Filled.Folder, null, tint = DSColors.fairway, modifier = Modifier.size(34.dp))
                    Text(
                        "还没有练习记录",
                        color = DSColors.deepGreen,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "先在 Memo 里保存一条视频。",
                        color = DSColors.shadowGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onOpenMemo,
                        enabled = !sending,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = DSColors.deepGreen),
                        contentPadding = PaddingValues(horizontal = 22.dp),
                        modifier = Modifier.height(44.dp),
                    ) {
                        Text("去 Memo", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().v2NavigationBarPadding(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(records, key = { it.id }) { memo ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DSColors.cardWhite)
                                .border(1.dp, DSColors.borderLine, RoundedCornerShape(16.dp))
                                .clickable(enabled = !sending) {
                                    if (memoHasShareableVideoSource(memo)) {
                                        onSelect(memo)
                                    } else {
                                        unavailableMessage = "这条记录的视频不在本机，暂时不能发送。"
                                    }
                                }.padding(12.dp)
                                .testTag("personal-record-picker-row-${memo.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(76.dp, 56.dp).clip(RoundedCornerShape(11.dp))
                                    .background(Brush.linearGradient(listOf(DSColors.deepGreen, DSColors.fairway)))
                                    .testTag("personal-record-picker-thumbnail-${memo.id}"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                                AsyncImage(
                                    model = memo.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    memo.displayTitle?.takeIf(String::isNotBlank) ?: memo.title?.takeIf(String::isNotBlank) ?: "练习记录",
                                    color = DSColors.deepGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    formatPersonalRecordPickerMeta(memo),
                                    color = DSColors.shadowGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }

    unavailableMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { unavailableMessage = null },
            title = {
                Text(
                    "暂时不能发送",
                    color = DSColors.inkGreen,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = { Text(message, color = DSColors.shadowGray) },
            confirmButton = {
                TextButton(
                    onClick = { unavailableMessage = null },
                    modifier = Modifier.testTag("personal-record-picker-unavailable-confirm"),
                ) {
                    Text("知道了", color = DSColors.fairway, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DSColors.cardWhite,
            modifier = Modifier.testTag("personal-record-picker-unavailable"),
        )
    }
}

internal fun formatPersonalRecordPickerMeta(memo: V2Memo): String {
    val date = memo.createdAt.format(
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", java.util.Locale.CHINA),
    )
    val duration = when {
        memo.durationSec?.takeIf { it.isFinite() && it > 0.0 } != null ->
            formatWorkspaceDuration(requireNotNull(memo.durationSec))
        memo.isOnlineVideo -> ""
        else -> "00:00"
    }
    return "$date · $duration"
}

@Composable
internal fun V2CitationPickerDialog(
    records: List<V2Memo>,
    onDismiss: () -> Unit,
    onInsert: (String) -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf<UUID?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        V2RoomPage(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.fillMaxWidth().v2StatusBarPadding().height(DSLayout.topBarHeight)
                        .testTag("citation-picker-top-bar")
                        .background(DSColors.warmWhite.copy(alpha = 0.96f)),
                ) {
                    Text(
                        "取消",
                        color = DSColors.deepGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.CenterStart).clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss).padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    Text(
                        "选择引用内容",
                        color = DSColors.deepGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    Text(
                        "使用",
                        color = if (selectedId != null) DSColors.fairway else DSColors.shadowGray.copy(alpha = 0.45f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.CenterEnd).clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = selectedId != null) {
                                val selected = records.firstOrNull { it.id == selectedId } ?: return@clickable
                                val title = selected.displayTitle?.takeIf(String::isNotBlank)
                                    ?: selected.title?.takeIf(String::isNotBlank)
                                    ?: "练习记录"
                                onInsert(title)
                            }.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().v2NavigationBarPadding(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            "引用会留在当前讨论或普通对话里，不会另开一个主题。",
                            color = DSColors.deepGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(DSColors.mistGreen.copy(alpha = 0.70f)).padding(12.dp),
                        )
                    }
                    if (records.isEmpty()) {
                        item {
                            Text(
                                "还没有可引用的个人记录",
                                color = DSColors.shadowGray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            )
                        }
                    } else {
                        items(records, key = { it.id }) { memo ->
                            val selected = selectedId == memo.id
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .background(Color.White).clickable { selectedId = memo.id }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    memo.displayTitle?.takeIf(String::isNotBlank)
                                        ?: memo.title?.takeIf(String::isNotBlank)
                                        ?: "练习记录",
                                    color = DSColors.deepGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (selected) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "已选择",
                                        tint = DSColors.fairway,
                                        modifier = Modifier.size(20.dp),
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

@Composable
internal fun SpaceFeedCard(
    item: V2CoachWorkspaceItem,
    compact: Boolean,
    coachPerspective: Boolean,
    friendSpace: Boolean,
    hasReplyDraft: Boolean = false,
    onClick: () -> Unit,
    onPlay: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    val hasNewInteraction = if (friendSpace || !coachPerspective) {
        item.studentHasUnread
    } else {
        item.reviewState == "new"
    }
    val accent = when {
        hasReplyDraft -> Color(0xFFB98516)
        hasNewInteraction -> DSColors.fairway
        else -> DSColors.borderLine
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = if (hasReplyDraft || hasNewInteraction) 0.10f else 0.045f),
                spotColor = accent.copy(alpha = if (hasReplyDraft || hasNewInteraction) 0.10f else 0.045f),
            )
            .clip(shape)
            .background(Color.White)
            .border(
                width = if (hasReplyDraft || hasNewInteraction) 1.5.dp else 1.dp,
                color = accent.copy(alpha = if (hasReplyDraft || hasNewInteraction) 0.72f else 0.20f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .testTag("workspace-feed-card-${item.itemId}"),
    ) {
        val showCoachSentBadge = !coachPerspective && !friendSpace &&
            (item.senderRole == "coach" || item.resolvedKind == "coach_post" || item.resolvedKind == "coach_feedback")
        val mediaWidth = if (compact) 132.dp else maxOf(138.dp, maxWidth * 0.49f)
        Row(
            modifier = Modifier.fillMaxWidth().height(if (compact) 98.dp else 150.dp)
                .padding(if (compact) 10.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 11.dp else 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WorkspaceFeedMediaPreview(
                item = item,
                compact = compact,
                showCoachSentBadge = showCoachSentBadge,
                modifier = Modifier.width(mediaWidth).height(if (compact) 78.dp else 126.dp)
                    .clickable(onClick = onPlay ?: onClick),
            )
            WorkspaceFeedCardDetails(
                item = item,
                compact = compact,
                friendSpace = friendSpace,
                hasReplyDraft = hasReplyDraft,
                hasNewInteraction = hasNewInteraction,
                showCoachSentBadge = showCoachSentBadge,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WorkspaceFeedMediaPreview(
    item: V2CoachWorkspaceItem,
    compact: Boolean,
    showCoachSentBadge: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(if (compact) 12.dp else 15.dp)
    val cover = item.primaryAsset?.coverUrl?.takeIf(String::isNotBlank)
        ?: item.coverUrl?.takeIf(String::isNotBlank)
    val duration = item.primaryAsset?.durationSeconds ?: item.durationSeconds
    Box(
        modifier = modifier.clip(shape).background(
            Brush.linearGradient(listOf(DSColors.mistGreen, Color.White)),
        ).testTag("workspace-feed-media-${item.itemId}"),
        contentAlignment = Alignment.Center,
    ) {
        if (cover != null) {
            AsyncImage(
                model = cover,
                contentDescription = "视频封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (!item.hasVideoContent) {
            Icon(
                Icons.Filled.ChatBubble,
                contentDescription = null,
                tint = DSColors.fairway,
                modifier = Modifier.size(24.dp),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f))),
            ),
        )
        if (showCoachSentBadge) {
            Text(
                "教练发来",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                    .height(18.dp).clip(RoundedCornerShape(9.dp))
                    .background(DSColors.fairway).padding(horizontal = 6.dp)
                    .testTag("workspace-feed-coach-sent-${item.itemId}"),
            )
        }
        if (item.hasVideoContent) {
            Box(
                modifier = Modifier.size(if (compact) 36.dp else 44.dp).clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "播放视频",
                    tint = Color.White,
                    modifier = Modifier.size(if (compact) 20.dp else 25.dp).offset(x = 1.dp),
                )
            }
        }
        duration?.takeIf { it.isFinite() && it > 0.0 }?.let { seconds ->
            Text(
                workspaceFeedDuration(seconds),
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                    .height(18.dp).clip(RoundedCornerShape(9.dp))
                    .background(Color.Black.copy(alpha = 0.56f)).padding(horizontal = 6.dp),
            )
        }
    }
}

@Composable
private fun WorkspaceFeedCardDetails(
    item: V2CoachWorkspaceItem,
    compact: Boolean,
    friendSpace: Boolean,
    hasReplyDraft: Boolean,
    hasNewInteraction: Boolean,
    showCoachSentBadge: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val title = workspaceFeedContentTitle(item, friendSpace)
    val metadata = workspaceFeedMetadata(item, coachSent = showCoachSentBadge)
    val preview = V2CoachWorkspaceItem.visibleCopy(item.text).orEmpty()
    val badge = when {
        hasReplyDraft -> Triple("继续未发送", Icons.Filled.Refresh, Color(0xFFB98516))
        hasNewInteraction -> Triple(
            if (item.replyCount > 1) "${item.replyCount}条新回复" else "新回复",
            Icons.Filled.ChatBubble,
            DSColors.fairway,
        )
        item.replyCount > 0 -> Triple("${item.replyCount}条交流", Icons.Filled.ChatBubble, DSColors.fairway)
        else -> null
    }
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                title,
                color = DSColors.inkGreen,
                fontSize = if (compact) 14.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = DSColors.shadowGray,
                modifier = Modifier.padding(top = 3.dp).size(14.dp),
            )
        }
        Text(
            metadata,
            color = DSColors.shadowGray,
            fontSize = if (compact) 9.sp else 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = if (compact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!compact && preview.isNotEmpty()) {
            Text(
                preview,
                color = DSColors.shadowGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.weight(1f))
        badge?.let { (text, icon, color) ->
            Row(
                modifier = Modifier.height(25.dp).clip(RoundedCornerShape(13.dp))
                    .background(color.copy(alpha = 0.10f)).padding(horizontal = 8.dp)
                    .testTag("workspace-feed-badge-${item.itemId}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
                Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

internal fun workspaceFeedContentTitle(item: V2CoachWorkspaceItem, friendSpace: Boolean): String {
    V2CoachWorkspaceItem.visibleCopy(item.title)?.let { return it }
    if (friendSpace) {
        return if (item.resolvedKind == "coach_post") "参考内容"
        else if (item.hasVideoContent) "练习视频" else "空间消息"
    }
    if (item.senderRole == "coach") return "教练分享"
    return if (item.hasVideoContent) "复练视频" else "学员消息"
}

internal fun workspaceFeedMetadata(item: V2CoachWorkspaceItem, coachSent: Boolean = false): String {
    val kind = when {
        coachSent -> "教练发来"
        item.resolvedKind == "student_practice" -> "挥杆练习"
        else -> "参考内容"
    }
    val parts = mutableListOf(
        workspaceFeedDate(item.createdAt),
        kind,
    )
    parts += item.primaryAsset?.tags.orEmpty().map(String::trim).filter(String::isNotEmpty).take(2)
    return parts.joinToString(" · ")
}

internal fun workspaceFeedDate(
    raw: String?,
    now: java.time.Instant = java.time.Instant.now(),
    zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault(),
): String {
    val instant = raw?.let { value ->
        runCatching { java.time.OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { java.time.Instant.parse(value) }
            .getOrNull()
    } ?: return "刚刚"
    val current = now.atZone(zoneId)
    val date = instant.atZone(zoneId)
    val pattern = if (date.toLocalDate() == current.toLocalDate()) "'今天' HH:mm" else "M月d日 HH:mm"
    return date.format(java.time.format.DateTimeFormatter.ofPattern(pattern, java.util.Locale.CHINA))
}

private fun workspaceFeedDuration(seconds: Double): String {
    val value = seconds.takeIf { it.isFinite() }?.roundToInt()?.coerceAtLeast(0) ?: 0
    return "%d:%02d".format(value / 60, value % 60)
}

@Composable
internal fun WorkspaceThreadTopBar(
    title: String,
    subtitle: String,
    peerName: String,
    peerAvatar: String?,
    selectedTab: WorkspaceThreadTab,
    replyCount: Int,
    canDeleteRoot: Boolean,
    onBack: () -> Unit,
    onSelectContent: () -> Unit,
    onSelectDiscussion: () -> Unit,
    onDeleteRoot: () -> Unit,
    onOpenTeachingProfile: (() -> Unit)? = null,
    barColor: Color = DSColors.warmWhite,
    showsPeerAvatar: Boolean = true,
    onOpenPracticeList: (() -> Unit)? = null,
    barHeight: androidx.compose.ui.unit.Dp = DSLayout.topBarHeight,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val hasMenuAction = canDeleteRoot || onOpenTeachingProfile != null
    Box(
        modifier = Modifier.fillMaxWidth().v2StatusBarPadding().height(barHeight)
            .background(barColor).testTag("workspace-thread-top-bar"),
    ) {
        Box(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(36.dp)
                .clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = CoachInk,
                modifier = Modifier.size(24.dp),
            )
        }
        Row(
            modifier = Modifier.align(Alignment.Center).widthIn(max = if (onOpenPracticeList != null) 148.dp else 236.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (showsPeerAvatar) {
                V2UserAvatar(
                    peerAvatar,
                    Modifier.size(32.dp).border(1.dp, DSColors.trophyGold, CircleShape),
                    fallbackFontSize = 12.sp,
                    placeholderIconSize = 19.dp,
                    fallbackText = peerName,
                    fallbackColor = DSColors.sandBeige,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    title,
                    color = ContentText,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        color = CoachMeta,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onOpenPracticeList != null) {
                Box(
                    Modifier.size(48.dp).clickable(onClick = onOpenPracticeList),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = "练习列表",
                        tint = ContentText,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .clickable(enabled = hasMenuAction) { menuExpanded = true }
                    .testTag("workspace-thread-more"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "更多空间操作",
                    tint = if (hasMenuAction) CoachInk else CoachMeta.copy(alpha = 0.45f),
                    modifier = Modifier.size(23.dp),
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    onOpenTeachingProfile?.let { openProfile ->
                        DropdownMenuItem(
                            text = { Text("教学档案") },
                            leadingIcon = { Icon(Icons.Filled.Description, null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                menuExpanded = false
                                openProfile()
                            },
                        )
                    }
                    if (canDeleteRoot) {
                    DropdownMenuItem(
                        text = { Text("删除这条动态", color = DSColors.flagRed) },
                        onClick = {
                            menuExpanded = false
                            onDeleteRoot()
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun WorkspaceThreadTabBar(
    selectedTab: WorkspaceThreadTab,
    resourceCount: Int,
    onSelect: (WorkspaceThreadTab) -> Unit,
    discussionCount: Int = 0,
) {
    val tabs = listOf(WorkspaceThreadTab.DISCUSSION, WorkspaceThreadTab.CONTENT)
    Row(
        modifier = Modifier.fillMaxWidth().height(47.dp).background(Color.Transparent)
            .padding(start = 12.dp, top = 8.dp, end = 12.dp).testTag("workspace-thread-tabs"),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedTab == tab
            Column(
                modifier = Modifier.weight(1f).fillMaxSize().clickable { onSelect(tab) }
                    .semantics { selected = isSelected }
                    .testTag("workspace-thread-tab-${tab.name.lowercase()}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tab.title,
                        color = if (isSelected) DSColors.inkGreen else DSColors.shadowGray,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                    val count = when (tab) {
                        WorkspaceThreadTab.DISCUSSION -> discussionCount
                        WorkspaceThreadTab.CONTENT -> resourceCount
                        WorkspaceThreadTab.RESOURCES -> 0
                    }
                    if (count > 0) {
                        Text(
                            count.toString(),
                            color = if (isSelected) CoachGreen else DSColors.shadowGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Box(
                    Modifier.size(28.dp, 3.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (isSelected) DSColors.deepGreen else Color.Transparent),
                )
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.Black.copy(alpha = 0.0275f)))
}

internal fun workspaceThreadTitle(peerName: String, coachPerspective: Boolean, friendSpace: Boolean): String {
    if (!coachPerspective && !friendSpace) return "练习详情"
    return peerName.trim().ifEmpty { if (friendSpace) "好友交流" else "教学交流" }
}

internal fun workspaceThreadSubtitle(
    friendSpace: Boolean,
    connectionStatus: String,
    source: V2CoachWorkspaceItem,
): String {
    if (connectionStatus != "active") {
        return if (friendSpace) "历史好友空间 · 只读" else "历史教学空间 · 只读"
    }
    val title = V2CoachWorkspaceItem.visibleCopy(source.title)
    val date = workspaceFeedDate(source.createdAt ?: source.latestActivityAt)
    val summary = listOfNotNull(title, date).joinToString(" · ")
    return if (friendSpace) "私密好友空间 · $summary" else summary.ifBlank { date }
}

@Composable
private fun ThreadDialog(
    connectionId: String,
    peerName: String,
    peerAvatar: String?,
    friendSpace: Boolean,
    connectionStatus: String,
    source: V2CoachWorkspaceItem,
    coachPerspective: Boolean,
    readOnly: Boolean,
    onOpenTeachingProfile: (() -> Unit)?,
    onDismiss: () -> Unit,
    practiceItems: List<V2CoachWorkspaceItem> = emptyList(),
    onSelectPractice: (V2CoachWorkspaceItem) -> Unit = {},
    focusUnreadReply: Boolean = false,
    onChanged: () -> Unit,
) {
    val context = LocalContext.current
    val networkConnected = rememberQueueNetworkConnected()
    val threadLifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val threadSession by ServiceLocator.session.state.collectAsState(initial = V2Session())
    val draftOwnerUserId = threadSession.userId.orEmpty()
    val draftStore = remember(context) { V2WorkspaceDraftStore(context) }
    val deliveryState by ServiceLocator.coachDeliveryQueue.state.collectAsState()
    val restoredDraft = remember(source.itemId, draftOwnerUserId) {
        if (draftOwnerUserId.isBlank()) com.mygolfswing.app.data.coach.V2WorkspaceDraft()
        else draftStore.loadThread(draftOwnerUserId, source.itemId)
    }
    val restoredDraftHasVisibleContent = remember(source.itemId, draftOwnerUserId) {
        restoredDraft.text.isNotBlank() ||
            restoredDraft.primaryAttachment != null ||
            restoredDraft.relatedAttachments.isNotEmpty()
    }
    var thread by remember(source.itemId) { mutableStateOf<V2CoachThread?>(null) }
    var loading by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var draftSavedAtMillis by remember(source.itemId, draftOwnerUserId) {
        mutableLongStateOf(restoredDraft.savedAtMillis)
    }
    var feedbackTitle by rememberSaveable(source.itemId, draftOwnerUserId) { mutableStateOf(restoredDraft.title) }
    var draft by rememberSaveable(source.itemId, draftOwnerUserId) { mutableStateOf(restoredDraft.text) }
    var saveVideoToLibrary by rememberSaveable(source.itemId, draftOwnerUserId) {
        mutableStateOf(restoredDraft.saveToLibrary && coachPerspective)
    }
    var error by remember { mutableStateOf<String?>(null) }
    var composerNotice by remember(source.itemId) { mutableStateOf<String?>(null) }
    var composerNoticeToken by remember(source.itemId) { mutableIntStateOf(0) }
    var showRestoredDraftNotice by remember(source.itemId, draftOwnerUserId) {
        mutableStateOf(restoredDraftHasVisibleContent)
    }
    var observedNetworkConnected by remember(source.itemId) { mutableStateOf<Boolean?>(null) }
    var threadSyncError by remember(source.itemId) { mutableStateOf<String?>(null) }
    var pendingVideo by remember(source.itemId, draftOwnerUserId) {
        mutableStateOf(
            restoredDraft.primaryAttachment?.let { attachment ->
                restorePreparedWorkspaceVideo(attachment, fallbackRequestPrefix = "reply")
            },
        )
    }
    var pendingPrimaryVideoReplacement by remember(source.itemId) {
        mutableStateOf<V2PreparedWorkspaceVideo?>(null)
    }
    var trimmingVideo by remember(source.itemId) { mutableStateOf<V2PreparedWorkspaceVideo?>(null) }
    var trimSaving by remember(source.itemId) { mutableStateOf(false) }
    var trimError by remember(source.itemId) { mutableStateOf<String?>(null) }
    var analysisPlayback by remember { mutableStateOf<WorkspaceAnalysisPlayback?>(null) }
    var preparingPlaybackId by remember { mutableStateOf<String?>(null) }
    var playbackNotice by remember { mutableStateOf<String?>(null) }
    var coverPickingVideo by remember(source.itemId) { mutableStateOf<V2PreparedWorkspaceVideo?>(null) }
    var coverSaving by remember(source.itemId) { mutableStateOf(false) }
    var coverError by remember(source.itemId) { mutableStateOf<String?>(null) }
    var showReplyVideoSource by remember(source.itemId) { mutableStateOf(false) }
    var pendingReplyCameraFile by remember(source.itemId) { mutableStateOf<File?>(null) }
    var pendingCellularJobId by remember(source.itemId) { mutableStateOf<String?>(null) }
    var pendingCellularReplyJob by remember(source.itemId) { mutableStateOf<V2CoachDeliveryJob?>(null) }
    var pendingCellularFileSize by remember(source.itemId) { mutableLongStateOf(0L) }
    val deliveryPeerName = peerName.ifBlank {
        if (friendSpace) "好友" else if (coachPerspective) "学员" else "教练"
    }
    var selectedTab by remember(source.itemId) { mutableStateOf(WorkspaceThreadTab.DISCUSSION) }
    var showPracticeList by remember(source.itemId) { mutableStateOf(false) }
    var replyToItemId by rememberSaveable(source.itemId, draftOwnerUserId) { mutableStateOf(restoredDraft.replyToItemId) }
    var replyAtSecondsText by rememberSaveable(source.itemId, draftOwnerUserId) { mutableStateOf(restoredDraft.replyAtSecondsText) }
    val voiceRecorder = remember(context, source.itemId) { V2WorkspaceVoiceRecorder(context.applicationContext) }
    var recordingVoice by remember(source.itemId) { mutableStateOf(false) }
    var requestingVoicePermission by remember(source.itemId) { mutableStateOf(false) }
    var voicePressActive by remember(source.itemId) { mutableStateOf(false) }
    var cancellingVoice by remember(source.itemId) { mutableStateOf(false) }
    var savingAssetIds by remember(source.itemId) { mutableStateOf<Set<String>>(emptySet()) }
    var saveAssetNotice by remember(source.itemId) { mutableStateOf<String?>(null) }
    var pendingDeleteItem by remember(source.itemId) { mutableStateOf<V2CoachWorkspaceItem?>(null) }
    var deletingItemId by remember(source.itemId) { mutableStateOf<String?>(null) }
    var quickReplies by remember(source.itemId) { mutableStateOf<List<V2CoachQuickReply>>(emptyList()) }
    var quickRepliesLoading by remember(source.itemId) { mutableStateOf(false) }
    var showQuickReplies by remember(source.itemId) { mutableStateOf(false) }
    var showCitationPicker by remember(source.itemId) { mutableStateOf(false) }
    var showReplyTools by remember(source.itemId) { mutableStateOf(false) }
    var showDiscardReplyDraftConfirmation by remember(source.itemId) { mutableStateOf(false) }
    var prefersVoiceInput by rememberSaveable(source.itemId) { mutableStateOf(false) }
    val citationRecords by V2MemoStore.allMemos.collectAsState()
    var replyLibrary by remember(source.itemId) { mutableStateOf<List<V2CoachLibraryAsset>>(emptyList()) }
    var relatedLibraryAssets by remember(source.itemId, draftOwnerUserId) {
        mutableStateOf(
            restoredDraft.relatedAttachments.mapNotNull(V2WorkspaceDraftAttachment::assetId).map { assetId ->
                V2CoachLibraryAsset(assetId = assetId, title = "教学素材")
            },
        )
    }
    var relatedLocalVideos by remember(source.itemId, draftOwnerUserId) {
        mutableStateOf(
            restoredDraft.relatedAttachments.mapNotNull { attachment ->
                if (!attachment.assetId.isNullOrBlank()) return@mapNotNull null
                val video = attachment.videoPath?.let(::File)?.takeIf(File::isFile) ?: return@mapNotNull null
                val cover = attachment.coverPath?.let(::File)?.takeIf(File::isFile) ?: return@mapNotNull null
                V2PreparedWorkspaceVideo(
                    file = video,
                    cover = cover,
                    durationSeconds = attachment.durationSeconds,
                    uploadId = attachment.uploadId,
                    requestId = attachment.requestId ?: "related_${attachment.uploadId}",
                    sourceKind = attachment.sourceKind,
                    coverTimeSeconds = attachment.coverTimeSeconds,
                )
            },
        )
    }
    var expandedRelatedMediaId by remember(source.itemId) { mutableStateOf<String?>(null) }
    var loadingRelatedMediaId by remember(source.itemId) { mutableStateOf<String?>(null) }
    var replyLibraryLoading by remember(source.itemId) { mutableStateOf(false) }
    var showReplyLibrary by remember(source.itemId) { mutableStateOf(false) }
    var analysisPreparingItemId by remember(source.itemId) { mutableStateOf<String?>(null) }
    var analysisPreparationProgress by remember(source.itemId) { mutableStateOf<Double?>(null) }
    var analysisPreparationJob by remember(source.itemId) { mutableStateOf<Job?>(null) }
    var analysisSource by remember(source.itemId) { mutableStateOf<V2VideoFileStore.ImportResult?>(null) }
    var analysisTitle by remember(source.itemId) { mutableStateOf("学员视频") }
    var analysisSubtitle by remember(source.itemId) { mutableStateOf<String?>(null) }
    var analysisStartsWithRecordingOptions by remember(source.itemId) { mutableStateOf(false) }
    val threadScrollState = rememberScrollState()
    val threadBottomThresholdPx = with(LocalDensity.current) { 96.dp.roundToPx() }
    var hasCompletedInitialThreadSync by remember(source.itemId) { mutableStateOf(false) }
    var loadingOlderReplies by remember(source.itemId) { mutableStateOf(false) }
    var observedVisibleReplyCount by remember(source.itemId) { mutableIntStateOf(-1) }
    var unseenIncomingReplyCount by remember(source.itemId) { mutableIntStateOf(0) }
    var scrollToReplyItemId by remember(source.itemId) { mutableStateOf<String?>(null) }
    var focusedUnread by remember(source.itemId) { mutableStateOf(false) }
    var teachingPanel by remember(source.itemId) { mutableStateOf(TeachingDetailPanel.DISCUSSION) }
    var summaryExpanded by remember(source.itemId) { mutableStateOf(false) }
    var composerFocused by remember(source.itemId) { mutableStateOf(false) }
    var viewingMemoId by remember(source.itemId) { mutableStateOf<UUID?>(null) }
    var openDiscussionAtTop by remember(source.itemId) { mutableIntStateOf(0) }
    LaunchedEffect(thread?.replies?.size, focusUnreadReply) {
        if (!focusUnreadReply || focusedUnread) return@LaunchedEffect
        val loaded = thread ?: return@LaunchedEffect
        val replies = loaded.replies.filterNot(V2CoachWorkspaceItem::isRemovedContent)
        val index = workspaceFirstUnreadReplyIndex(loaded.item, replies, coachPerspective, friendSpace)
        val incomingRole = if (coachPerspective) "student" else "coach"
        val id = index?.let { replies.getOrNull(it)?.itemId }
            ?: replies.lastOrNull { reply -> reply.senderRole == incomingRole }?.itemId
        if (id != null) {
            scrollToReplyItemId = id
            if (!friendSpace) teachingPanel = TeachingDetailPanel.DISCUSSION
            focusedUnread = true
        }
    }

    fun isNearThreadBottom(): Boolean = workspaceIsNearThreadBottom(
        value = threadScrollState.value,
        maxValue = threadScrollState.maxValue,
        thresholdPx = threadBottomThresholdPx,
    )

    suspend fun markLatestThreadViewedIfNeeded() {
        if (selectedTab != WorkspaceThreadTab.DISCUSSION) return
        val currentThread = thread ?: return
        val latestReply = currentThread.replies.lastOrNull { !it.isRemovedContent }
        if (
            hasCompletedInitialThreadSync &&
            !isNearThreadBottom() &&
            latestReply != null &&
            !workspaceReplyIsMine(latestReply, coachPerspective, friendSpace)
        ) return
        ServiceLocator.coachWorkspaceRepo.markViewed(source.itemId)
            .onSuccess { onChanged() }
    }

    fun presentAnalysisPlayback(uri: String, lookupId: String) {
        val items = thread?.let { listOf(it.item) + it.replies }.orEmpty()
        val item = items.firstOrNull { candidate ->
            candidate.itemId == lookupId ||
                candidate.primaryAsset?.assetId == lookupId ||
                candidate.attachments.any { it.assetId == lookupId }
        }
        val asset = item?.attachments?.firstOrNull { it.assetId == lookupId }
            ?: item?.primaryAsset?.takeIf { it.assetId == lookupId }
        val title = asset?.title?.takeIf { it.isNotBlank() }
            ?: item?.title?.takeIf { it.isNotBlank() }
            ?: "视频"
        playbackNotice = null
        analysisPlayback = WorkspaceAnalysisPlayback(
            uri = uri,
            title = title,
            subtitle = peerName.takeIf { it.isNotBlank() },
        )
    }

    fun requestWorkspacePlayback(lookupId: String, url: String?) {
        val direct = workspaceStreamPlaybackUrl(url) ?: workspacePlaybackUrlInThread(thread, lookupId)
        if (direct != null) {
            presentAnalysisPlayback(direct, lookupId)
            return
        }
        if (preparingPlaybackId != null) return
        preparingPlaybackId = lookupId
        playbackNotice = null
        scope.launch {
            val refreshed = ServiceLocator.coachWorkspaceRepo
                .workspaceThread(connectionId, source.itemId)
                .getOrNull()
            if (refreshed != null) thread = mergeWorkspaceThreads(refreshed, thread)
            val resolved = workspacePlaybackUrlInThread(thread, lookupId)
            preparingPlaybackId = null
            if (resolved != null) {
                presentAnalysisPlayback(resolved, lookupId)
            } else {
                playbackNotice = "暂时拿不到这段视频，请稍后重试"
            }
        }
    }

    fun retryThreadSync() {
        if (loading) return
        loading = true
        threadSyncError = null
        scope.launch {
            ServiceLocator.coachWorkspaceRepo.workspaceThread(connectionId, source.itemId)
                .onSuccess { refreshed ->
                    thread = mergeWorkspaceThreads(refreshed, thread)
                    markLatestThreadViewedIfNeeded()
                }
                .onFailure { throwable ->
                    threadSyncError = coachThrowableError("交流内容暂时无法加载，请稍后重试", throwable)
                }
            loading = false
            hasCompletedInitialThreadSync = true
        }
    }

    fun loadOlderReplies() {
        val cursor = thread?.nextCursor?.takeIf(String::isNotBlank) ?: return
        if (loadingOlderReplies) return
        loadingOlderReplies = true
        threadSyncError = null
        scope.launch {
            ServiceLocator.coachWorkspaceRepo.workspaceThread(
                connectionId = connectionId,
                itemId = source.itemId,
                cursor = cursor,
            ).onSuccess { olderPage ->
                thread = mergeWorkspaceThreads(olderPage, thread, pagingOlder = true)
            }.onFailure {
                threadSyncError = "较早的回复未能加载，请重试"
            }
            loadingOlderReplies = false
        }
    }

    LaunchedEffect(showRestoredDraftNotice) {
        if (showRestoredDraftNotice) {
            delay(3_000L)
            showRestoredDraftNotice = false
        }
    }

    LaunchedEffect(
        draftOwnerUserId,
        feedbackTitle,
        draft,
        replyToItemId,
        replyAtSecondsText,
        saveVideoToLibrary,
        pendingVideo?.uploadId,
        pendingVideo?.cover?.absolutePath,
        relatedLibraryAssets.map(V2CoachLibraryAsset::assetId),
        relatedLocalVideos.map(V2PreparedWorkspaceVideo::uploadId),
    ) {
        if (draftOwnerUserId.isNotBlank()) {
            draftStore.saveThread(
                ownerUserId = draftOwnerUserId,
                connectionId = connectionId,
                itemId = source.itemId,
                title = feedbackTitle,
                text = draft,
                replyToItemId = replyToItemId,
                replyAtSecondsText = replyAtSecondsText,
                saveToLibrary = saveVideoToLibrary && coachPerspective,
                primaryAttachment = pendingVideo?.let { prepared ->
                    V2WorkspaceDraftAttachment(
                        videoPath = prepared.file.absolutePath,
                        coverPath = prepared.cover?.absolutePath,
                        uploadId = prepared.uploadId,
                        requestId = prepared.requestId,
                        sourceKind = prepared.sourceKind,
                        durationSeconds = prepared.durationSeconds,
                        coverTimeSeconds = prepared.coverTimeSeconds,
                        saveToLibrary = saveVideoToLibrary && coachPerspective,
                    )
                },
                relatedAttachments = buildList {
                    addAll(relatedLibraryAssets.map { asset ->
                        V2WorkspaceDraftAttachment(
                            assetId = asset.assetId,
                            uploadId = "existing_${asset.assetId}",
                            sourceKind = "library_asset",
                            title = asset.title.ifBlank { "关联教学视频" },
                        )
                    })
                    addAll(relatedLocalVideos.map { prepared ->
                        V2WorkspaceDraftAttachment(
                            videoPath = prepared.file.absolutePath,
                            coverPath = prepared.cover?.absolutePath,
                            uploadId = prepared.uploadId,
                            requestId = prepared.requestId,
                            sourceKind = prepared.sourceKind,
                            durationSeconds = prepared.durationSeconds,
                            coverTimeSeconds = prepared.coverTimeSeconds,
                        )
                    })
                }.take(V2_MAX_RELATED_MEDIA),
            )
            draftSavedAtMillis = draftStore.loadThread(draftOwnerUserId, source.itemId).savedAtMillis
        }
    }

    val threadDeliveryJobs = deliveryState.jobs
        .asSequence()
        .filter { job ->
            job.ownerUserId == draftOwnerUserId &&
                job.connectionId == connectionId &&
                job.parentItemId == source.itemId
        }
        .toList()
    val activeThreadDeliveryJob = threadDeliveryJobs.firstOrNull { job ->
        deliveryState.activeJobId == job.jobId
    }
    val latestThreadDeliveryJob = threadDeliveryJobs
        .asSequence()
        .filterNot { job -> job.jobId == activeThreadDeliveryJob?.jobId }
        .maxByOrNull(V2CoachDeliveryJob::createdAtMillis)

    LaunchedEffect(source.itemId, draftOwnerUserId, coachPerspective) {
        val restoredAssetIds = restoredDraft.relatedAttachments.mapNotNull(V2WorkspaceDraftAttachment::assetId)
        if (!coachPerspective || restoredAssetIds.isEmpty()) return@LaunchedEffect
        runCatching { ServiceLocator.coachWorkspaceRepo.library() }.onSuccess { assets ->
            replyLibrary = assets
            val byId = assets.associateBy(V2CoachLibraryAsset::assetId)
            relatedLibraryAssets = restoredAssetIds.map { assetId ->
                byId[assetId] ?: V2CoachLibraryAsset(assetId = assetId, title = "教学素材")
            }
        }
    }

    fun queuedRelatedAttachments(): List<V2CoachQueuedAttachment> = buildList {
        addAll(relatedLocalVideos.map { prepared ->
            V2CoachQueuedAttachment(
                videoPath = prepared.file.absolutePath,
                coverPath = prepared.cover?.absolutePath,
                uploadId = prepared.uploadId,
                sourceKind = prepared.sourceKind,
                durationSeconds = prepared.durationSeconds,
            )
        })
        addAll(relatedLibraryAssets.distinctBy(V2CoachLibraryAsset::assetId).map { asset ->
            V2CoachQueuedAttachment(
                existingAssetId = asset.assetId,
                uploadId = "existing_${asset.assetId}",
                sourceKind = "library_asset",
                title = asset.title.ifBlank { "关联教学视频" },
            )
        })
    }.take(V2_MAX_RELATED_MEDIA)

    fun clearRelatedMediaState() {
        relatedLibraryAssets = emptyList()
        relatedLocalVideos = emptyList()
        expandedRelatedMediaId = null
        loadingRelatedMediaId = null
    }

    fun showTransientComposerNotice(message: String) {
        composerNoticeToken += 1
        val token = composerNoticeToken
        composerNotice = message
        scope.launch {
            delay(3_000L)
            if (composerNoticeToken == token) composerNotice = null
        }
    }

    LaunchedEffect(networkConnected) {
        val wasConnected = observedNetworkConnected
        observedNetworkConnected = networkConnected
        val hasDraft = draft.isNotBlank() || pendingVideo != null ||
            relatedLibraryAssets.isNotEmpty() || relatedLocalVideos.isNotEmpty()
        if (wasConnected == false && networkConnected && hasDraft && activeThreadDeliveryJob == null) {
            val retainedFailure = !latestThreadDeliveryJob?.lastError.isNullOrBlank() ||
                error?.contains("网络") == true
            if (retainedFailure) {
                error = "网络已恢复，文字和视频都还在，可以继续发送"
            } else {
                showTransientComposerNotice("网络已恢复，可以继续发送")
            }
        }
    }

    fun usePrimaryVideo(replacement: V2PreparedWorkspaceVideo) {
        val previous = pendingVideo
        if (previous != null && previous.uploadId != replacement.uploadId) {
            previous.deleteTemporaryFiles()
        }
        pendingVideo = replacement
        pendingPrimaryVideoReplacement = null
        saveVideoToLibrary = false
        error = null
        selectedTab = WorkspaceThreadTab.DISCUSSION
        when (replacement.sourceKind) {
            "analysis_recording" -> showTransientComposerNotice("口播已加入草稿，点发送后学员才能看到")
            "voice_recording" -> showTransientComposerNotice("语音已加入，点发送后对方才能听到")
        }
    }

    fun discardPendingPrimaryVideoReplacement() {
        pendingPrimaryVideoReplacement?.deleteTemporaryFiles()
        pendingPrimaryVideoReplacement = null
    }

    fun offerPrimaryVideoReplacement(replacement: V2PreparedWorkspaceVideo) {
        val current = pendingVideo
        when (
            workspacePrimaryVideoReplacementPolicy(
                currentSourceKind = current?.sourceKind,
                replacementSourceKind = replacement.sourceKind,
                coachPerspective = coachPerspective,
                relatedCount = relatedLibraryAssets.size + relatedLocalVideos.size,
            )
        ) {
            WorkspacePrimaryVideoReplacementPolicy.USE_DIRECTLY -> usePrimaryVideo(replacement)
            WorkspacePrimaryVideoReplacementPolicy.DEMOTE_CURRENT_TO_RELATED -> {
                if (current != null) {
                    relatedLocalVideos = (relatedLocalVideos + current).distinctBy(V2PreparedWorkspaceVideo::uploadId)
                        .take(V2_MAX_RELATED_MEDIA)
                    pendingVideo = null
                }
                usePrimaryVideo(replacement)
            }
            WorkspacePrimaryVideoReplacementPolicy.ASK_BEFORE_REPLACING -> {
                discardPendingPrimaryVideoReplacement()
                pendingPrimaryVideoReplacement = replacement
            }
        }
    }

    fun discardComposerDraft() {
        discardPendingPrimaryVideoReplacement()
        pendingVideo?.deleteTemporaryFiles()
        relatedLocalVideos.forEach(V2PreparedWorkspaceVideo::deleteTemporaryFiles)
        feedbackTitle = ""
        draft = ""
        pendingVideo = null
        replyToItemId = null
        replyAtSecondsText = ""
        saveVideoToLibrary = false
        relatedLibraryAssets = emptyList()
        relatedLocalVideos = emptyList()
        expandedRelatedMediaId = null
        loadingRelatedMediaId = null
        error = null
        if (draftOwnerUserId.isNotBlank()) {
            draftStore.clearThread(draftOwnerUserId, connectionId, source.itemId)
        }
        draftSavedAtMillis = 0L
    }

    fun retryQueuedReply(job: V2CoachDeliveryJob) {
        if (sending || deliveryState.activeJobId != null) return
        ServiceLocator.coachDeliveryQueue.cellularConfirmationFor(job)?.let { confirmation ->
            pendingCellularJobId = confirmation.jobId
            pendingCellularFileSize = confirmation.fileSizeBytes
            error = null
            return
        }
        if (coachPerspective) {
            showTransientComposerNotice(
                workspaceComposerQueuedNotice(
                    attachmentCount = (if (job.sourceKind == "text") 0 else 1) + job.attachments.size,
                    includedVoice = job.sourceKind == "voice_recording",
                ),
            )
            scope.launch {
                ServiceLocator.coachDeliveryQueue.deliver(job.jobId, ServiceLocator.coachWorkspaceRepo)
                    .onSuccess { result ->
                        val reply = result.requireWorkspaceItem()
                        thread = thread?.let { current ->
                            current.copy(replies = (current.replies + reply).distinctBy(V2CoachWorkspaceItem::itemId))
                        }
                        onChanged()
                    }
                    .onFailure { throwable ->
                        if (throwable is V2CellularUploadConfirmationRequired) {
                            pendingCellularJobId = throwable.jobId
                            pendingCellularFileSize = throwable.fileSizeBytes
                        }
                    }
            }
            return
        }
        sending = true
        error = null
        scope.launch {
            ServiceLocator.coachDeliveryQueue.deliver(job.jobId, ServiceLocator.coachWorkspaceRepo)
                .onSuccess { result ->
                    val reply = result.requireWorkspaceItem()
                    thread = thread?.let { current ->
                        current.copy(replies = (current.replies + reply).distinctBy(V2CoachWorkspaceItem::itemId))
                    }
                    onChanged()
                    showTransientComposerNotice(
                        workspaceComposerSentNotice(
                            peerName = deliveryPeerName,
                            attachmentCount = (if (job.sourceKind == "text") 0 else 1) + job.attachments.size,
                            includedVoice = job.sourceKind == "voice_recording",
                        ),
                    )
                }
                .onFailure { throwable ->
                    if (throwable is V2CellularUploadConfirmationRequired) {
                        pendingCellularJobId = throwable.jobId
                        pendingCellularFileSize = throwable.fileSizeBytes
                        error = null
                    } else {
                        error = coachThrowableError("发送仍未完成，内容继续保留在队列中", throwable)
                    }
                }
            sending = false
        }
    }

    fun saveAsset(asset: com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset) {
        if (!coachPerspective || asset.savedToLibrary || asset.assetId in savingAssetIds) return
        savingAssetIds = savingAssetIds + asset.assetId
        scope.launch {
            try {
                val result = ServiceLocator.coachWorkspaceRepo.updateLibraryAsset(
                    asset.assetId,
                    savedToLibrary = true,
                )
                result.onSuccess {
                    fun markSaved(item: V2CoachWorkspaceItem): V2CoachWorkspaceItem = item.copy(
                        primaryAsset = item.primaryAsset?.let { current ->
                            if (current.assetId == asset.assetId) current.copy(savedToLibrary = true) else current
                        },
                        attachments = item.attachments.map { current ->
                            if (current.assetId == asset.assetId) current.copy(savedToLibrary = true) else current
                        },
                    )
                    thread = thread?.let { current ->
                        current.copy(item = markSaved(current.item), replies = current.replies.map(::markSaved))
                    }
                    ServiceLocator.coachWorkspaceRepo.workspaceThread(connectionId, source.itemId)
                        .onSuccess { refreshed -> thread = mergeWorkspaceThreads(refreshed, thread) }
                    saveAssetNotice = "已保存到私有素材，并同步到本页“资料”。"
                }.onFailure { throwable ->
                    saveAssetNotice = coachThrowableError("暂时无法保存，请稍后重试。", throwable)
                }
            } finally {
                savingAssetIds = savingAssetIds - asset.assetId
            }
        }
    }

    fun close() {
        if (sending || trimSaving) return
        discardPendingPrimaryVideoReplacement()
        analysisPreparationJob?.cancel()
        analysisPreparationJob = null
        analysisSource?.let { prepared ->
            File(prepared.absolutePath).delete()
            prepared.thumbnailPath?.let(::File)?.delete()
        }
        analysisSource = null
        if (recordingVoice) {
            voiceRecorder.stop(cancel = true)
            recordingVoice = false
        }
        pendingVideo = null
        onDismiss()
    }

    fun prepareProfessionalAnalysis(item: V2CoachWorkspaceItem, startsWithRecordingOptions: Boolean = false) {
        if (!coachPerspective || sending || analysisPreparingItemId != null) return
        val videoUrl = item.primaryAsset?.takeUnless { it.isVoice }?.videoUrl ?: item.videoUrl
        if (videoUrl.isNullOrBlank()) {
            error = "这段视频暂时无法用于专业分析，请刷新后重试"
            return
        }
        analysisPreparationJob?.cancel()
        analysisPreparingItemId = item.itemId
        analysisPreparationProgress = 0.0
        error = null
        analysisTitle = item.title?.takeIf(String::isNotBlank) ?: "学员视频"
        analysisSubtitle = item.sender?.nickname?.takeIf(String::isNotBlank)
        analysisStartsWithRecordingOptions = startsWithRecordingOptions
        analysisPreparationJob = scope.launch {
            try {
                val imported = V2VideoFileStore.downloadWorkspaceAnalysisVideo(context, videoUrl) { progress ->
                    scope.launch { analysisPreparationProgress = progress.takeIf { it.isFinite() && it >= 0.0 } }
                } ?: throw V2CoachUserFacingException("分析视频准备失败，请刷新后重试")
                analysisSource = imported
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                error = coachThrowableError("分析视频准备失败，请稍后重试", throwable)
            } finally {
                analysisPreparingItemId = null
                analysisPreparationProgress = null
                analysisPreparationJob = null
            }
        }
    }

    fun cancelProfessionalAnalysisPreparation() {
        analysisPreparationJob?.cancel()
        analysisPreparationJob = null
        analysisPreparingItemId = null
        analysisPreparationProgress = null
        error = "已取消准备，学员原视频不会受影响"
    }

    fun acceptProfessionalAnalysisRecording(recording: File) {
        sending = true
        error = null
        scope.launch {
            try {
                val prepared = prepareWorkspaceVideo(
                    context = context,
                    uri = recording.toUri(),
                    uploadId = "analysis_${source.itemId}_${UUID.randomUUID()}",
                    requestId = "workspace_analysis_reply_${UUID.randomUUID()}",
                    sourceKind = "analysis_recording",
                )
                offerPrimaryVideoReplacement(prepared)
                analysisSource?.let { downloaded ->
                    File(downloaded.absolutePath).delete()
                    downloaded.thumbnailPath?.let(::File)?.delete()
                }
                analysisSource = null
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                error = coachThrowableError("无法保存专业分析录屏，请重新录制", throwable)
            } finally {
                recording.delete()
                sending = false
            }
        }
    }

    fun deleteItem(item: V2CoachWorkspaceItem) {
        if (deletingItemId != null) return
        val deletingRoot = item.parentItemId == null
        deletingItemId = item.itemId
        error = null
        scope.launch {
            val result = ServiceLocator.coachWorkspaceRepo.deleteWorkspaceItem(connectionId, item.itemId)
            result
                .onSuccess {
                    if (draftOwnerUserId.isNotBlank() && deletingRoot) {
                        draftStore.clearThread(draftOwnerUserId, connectionId, source.itemId)
                    }
                    if (!deletingRoot) {
                        ServiceLocator.coachWorkspaceRepo.workspaceThread(connectionId, source.itemId)
                            .onSuccess { refreshed -> thread = refreshed }
                            .onFailure {
                                thread = thread?.let { current ->
                                    current.copy(replies = current.replies.map { reply ->
                                        if (reply.itemId == item.itemId) reply.copy(
                                            title = "内容已删除",
                                            text = "",
                                            videoUrl = null,
                                            coverUrl = null,
                                            hasVideo = false,
                                            primaryAsset = null,
                                            attachments = emptyList(),
                                            deletedAt = "deleted",
                                        ) else reply
                                    })
                                }
                            }
                    }
                    onChanged()
                }
                .onFailure { throwable -> error = coachThrowableError("删除失败，请稍后重试", throwable) }
            pendingDeleteItem = null
            deletingItemId = null
            if (result.isSuccess && deletingRoot) onDismiss()
        }
    }

    fun useQuickReply(quickReply: V2CoachQuickReply) {
        if (coachPerspective && feedbackTitle.isBlank()) feedbackTitle = quickReply.title.take(80)
        draft = if (draft.isBlank()) quickReply.text else "$draft\n${quickReply.text}"
        quickReplies = listOf(quickReply) + quickReplies.filterNot { it.quickReplyId == quickReply.quickReplyId }
        scope.launch {
            ServiceLocator.coachWorkspaceRepo.markQuickReplyUsed(quickReply.quickReplyId)
                .onSuccess { confirmed ->
                    quickReplies = listOf(confirmed) + quickReplies.filterNot {
                        it.quickReplyId == confirmed.quickReplyId
                    }
                }
        }
    }

    fun recordPlayback(asset: com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset) {
        scope.launch {
            ServiceLocator.coachWorkspaceRepo.recordAssetPlayback(connectionId, asset.assetId)
        }
    }

    fun startVoiceRecording() {
        if (!voicePressActive || recordingVoice || requestingVoicePermission) return
        voiceRecorder.start()
            .onSuccess {
                recordingVoice = true
                error = null
            }
            .onFailure {
                voicePressActive = false
                recordingVoice = false
                error = coachThrowableError("无法开始录音，请检查麦克风后重试", it)
            }
    }

    fun finishVoiceRecording(cancelled: Boolean) {
        voicePressActive = false
        cancellingVoice = false
        if (!recordingVoice) return
        recordingVoice = false
        voiceRecorder.stop(cancel = cancelled)
            .onSuccess { saved ->
                if (saved == null) {
                    if (!cancelled) error = "语音时间太短，请按住后再说话"
                } else {
                    val requestId = "workspace_voice_${UUID.randomUUID()}"
                    offerPrimaryVideoReplacement(
                        V2PreparedWorkspaceVideo(
                            file = saved.file,
                            cover = null,
                            durationSeconds = saved.durationSeconds,
                            uploadId = "voice_${source.itemId}_${UUID.randomUUID()}",
                            requestId = requestId,
                            sourceKind = "voice_recording",
                        ),
                    )
                    prefersVoiceInput = false
                }
            }
            .onFailure { error = coachThrowableError("无法完成录音，请重新录制", it) }
    }

    val voicePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        requestingVoicePermission = false
        if (granted && voicePressActive) startVoiceRecording()
        else if (!granted) {
            voicePressActive = false
            error = "需要麦克风权限才能发送语音"
        }
    }

    fun beginVoicePress() {
        if (sending || requestingVoicePermission || recordingVoice) return
        voicePressActive = true
        cancellingVoice = false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecording()
        } else {
            requestingVoicePermission = true
            runCatching { voicePermission.launch(Manifest.permission.RECORD_AUDIO) }
                .onFailure {
                    requestingVoicePermission = false
                    voicePressActive = false
                    error = coachThrowableError("无法请求麦克风权限，请稍后重试", it)
                }
        }
    }

    DisposableEffect(voiceRecorder) {
        onDispose {
            if (voiceRecorder.isRecording) voiceRecorder.stop(cancel = true)
        }
    }

    fun clearReplyComposerAfterQueueAcceptance(ownerUserId: String) {
        feedbackTitle = ""
        draft = ""
        pendingVideo = null
        replyToItemId = null
        replyAtSecondsText = ""
        saveVideoToLibrary = false
        clearRelatedMediaState()
        draftStore.clearThread(
            ownerUserId = ownerUserId,
            connectionId = connectionId,
            itemId = source.itemId,
            deleteLocalMedia = false,
        )
    }

    fun appendDeliveredReply(reply: V2CoachWorkspaceItem) {
        thread = thread?.let { current ->
            current.copy(replies = (current.replies + reply).distinctBy(V2CoachWorkspaceItem::itemId))
        }
        onChanged()
    }

    fun startCoachQueuedDelivery(job: V2CoachDeliveryJob) {
        scope.launch {
            ServiceLocator.coachDeliveryQueue.deliver(job.jobId, ServiceLocator.coachWorkspaceRepo)
                .onSuccess { result -> appendDeliveredReply(result.requireWorkspaceItem()) }
                .onFailure { throwable ->
                    if (throwable is V2CellularUploadConfirmationRequired) {
                        pendingCellularJobId = throwable.jobId
                        pendingCellularFileSize = throwable.fileSizeBytes
                    }
                }
            // A failure remains owned by the persisted queue and its recovery card/global strip.
            // The composer deliberately stays editable so the coach can prepare the next reply.
        }
    }

    fun sendVideoReply(prepared: V2PreparedWorkspaceVideo) {
        if (sending) return
        val replyAtSeconds = replyAtSecondsText.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..86_400.0 }
        if (replyAtSecondsText.isNotBlank() && replyAtSeconds == null) {
            error = "视频时间需要填写0到86400之间的秒数"
            return
        }
        sending = true
        error = null
        scope.launch {
            var enqueued = false
            try {
                val ownerUserId = ServiceLocator.session.current().userId
                    ?: error("登录状态已失效，请重新登录")
                val job = V2CoachDeliveryJob(
                    jobId = prepared.requestId,
                    ownerUserId = ownerUserId,
                    connectionId = connectionId,
                    peerName = peerName,
                    parentItemId = source.itemId,
                    videoPath = prepared.file.absolutePath,
                    coverPath = prepared.cover?.absolutePath,
                    uploadId = prepared.uploadId,
                    requestId = prepared.requestId,
                    title = feedbackTitle.trim().ifBlank {
                        if (prepared.sourceKind == "voice_recording") "语音消息" else "视频回复"
                    },
                    text = draft,
                    durationSeconds = prepared.durationSeconds,
                    replyToItemId = replyToItemId,
                    replyAtSeconds = replyAtSeconds,
                    sourceKind = prepared.sourceKind,
                    saveToLibrary = coachPerspective &&
                        prepared.sourceKind != "album" &&
                        prepared.sourceKind != "voice_recording" &&
                        saveVideoToLibrary,
                    attachments = queuedRelatedAttachments(),
                )
                ServiceLocator.coachDeliveryQueue.cellularConfirmationFor(job)?.let { confirmation ->
                    pendingCellularReplyJob = job
                    pendingCellularFileSize = confirmation.fileSizeBytes
                    return@launch
                }
                ServiceLocator.coachDeliveryQueue.enqueue(job)
                enqueued = true
                clearReplyComposerAfterQueueAcceptance(ownerUserId)
                if (coachPerspective) {
                    showTransientComposerNotice(
                        workspaceComposerQueuedNotice(
                            attachmentCount = 1 + job.attachments.size,
                            includedVoice = prepared.sourceKind == "voice_recording",
                        ),
                    )
                    sending = false
                    startCoachQueuedDelivery(job)
                    return@launch
                }
                val reply = ServiceLocator.coachDeliveryQueue.deliver(
                    job.jobId,
                    ServiceLocator.coachWorkspaceRepo,
                ).getOrThrow().requireWorkspaceItem()
                appendDeliveredReply(reply)
                showTransientComposerNotice(
                    workspaceComposerSentNotice(
                        peerName = deliveryPeerName,
                        attachmentCount = 1 + job.attachments.size,
                        includedVoice = prepared.sourceKind == "voice_recording",
                    ),
                )
            } catch (throwable: Throwable) {
                if (throwable is V2CellularUploadConfirmationRequired) {
                    pendingCellularJobId = throwable.jobId
                    pendingCellularFileSize = throwable.fileSizeBytes
                    error = null
                } else if (enqueued) {
                    error = "视频回复已进入发送队列，网络恢复后会自动继续"
                } else {
                    error = coachThrowableError("视频回复暂时无法进入发送队列，请重试", throwable)
                    pendingVideo = prepared
                }
            } finally {
                sending = false
            }
        }
    }

    fun sendTextReply() {
        if (sending) return
        val text = draft.trim()
        if (text.isBlank() && relatedLibraryAssets.isEmpty() && relatedLocalVideos.isEmpty()) return
        val replyAtSeconds = replyAtSecondsText.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it in 0.0..86_400.0 }
        if (replyAtSecondsText.isNotBlank() && replyAtSeconds == null) {
            error = "视频时间需要填写0到86400之间的秒数"
            return
        }
        sending = true
        error = null
        scope.launch {
            var enqueued = false
            try {
                val ownerUserId = ServiceLocator.session.current().userId
                    ?: error("登录状态已失效，请重新登录")
                val requestId = "workspace_text_reply_${UUID.randomUUID()}"
                val job = V2CoachDeliveryJob(
                    jobId = requestId,
                    ownerUserId = ownerUserId,
                    connectionId = connectionId,
                    peerName = peerName,
                    parentItemId = source.itemId,
                    replyToItemId = replyToItemId,
                    replyAtSeconds = replyAtSeconds,
                    uploadId = "text_$requestId",
                    requestId = requestId,
                    title = feedbackTitle.trim().ifBlank { "文字回复" },
                    text = text,
                    sourceKind = "text",
                    attachments = queuedRelatedAttachments(),
                )
                ServiceLocator.coachDeliveryQueue.cellularConfirmationFor(job)?.let { confirmation ->
                    pendingCellularReplyJob = job
                    pendingCellularFileSize = confirmation.fileSizeBytes
                    return@launch
                }
                ServiceLocator.coachDeliveryQueue.enqueue(job)
                enqueued = true
                clearReplyComposerAfterQueueAcceptance(ownerUserId)
                if (coachPerspective) {
                    showTransientComposerNotice(
                        workspaceComposerQueuedNotice(
                            attachmentCount = job.attachments.size,
                            includedVoice = false,
                        ),
                    )
                    sending = false
                    startCoachQueuedDelivery(job)
                    return@launch
                }
                val reply = ServiceLocator.coachDeliveryQueue.deliver(
                    job.jobId,
                    ServiceLocator.coachWorkspaceRepo,
                ).getOrThrow().requireWorkspaceItem()
                appendDeliveredReply(reply)
                showTransientComposerNotice(
                    workspaceComposerSentNotice(
                        peerName = deliveryPeerName,
                        attachmentCount = job.attachments.size,
                        includedVoice = false,
                    ),
                )
            } catch (throwable: Throwable) {
                error = if (enqueued) "文字回复已进入发送队列，网络恢复后会自动继续"
                else coachThrowableError("回复失败，请重试", throwable)
            } finally {
                sending = false
            }
        }
    }

    fun prepareReplyVideo(uri: android.net.Uri, capturedFile: File? = null) {
        if (sending || trimSaving) return
        sending = true
        error = null
        trimError = null
        scope.launch {
            try {
                val prepared = prepareWorkspaceVideo(
                    context = context,
                    uri = uri,
                    uploadId = "reply_${source.itemId}_${UUID.randomUUID()}",
                    requestId = "workspace_reply_${UUID.randomUUID()}",
                    sourceKind = when {
                        capturedFile == null -> "album"
                        coachPerspective -> "coach_recording"
                        else -> "student_recording"
                    },
                )
                offerPrimaryVideoReplacement(prepared)
            } catch (throwable: Throwable) {
                error = coachThrowableError("无法准备所选视频，请重新选择", throwable)
            } finally {
                capturedFile?.delete()
                sending = false
            }
        }
    }

    fun launchReplyCamera(capture: androidx.activity.result.ActivityResultLauncher<android.net.Uri>) {
        showReplyVideoSource = false
        var createdFile: File? = null
        runCatching {
            val (file, uri) = V2VideoFileStore.createCameraOutputUri(context)
            createdFile = file
            pendingReplyCameraFile = file
            capture.launch(uri)
        }.onFailure { throwable ->
            pendingReplyCameraFile = null
            createdFile?.delete()
            error = coachThrowableError("无法打开相机，请改从相册选择", throwable)
            showReplyVideoSource = true
        }
    }

    fun confirmCellularReply() {
        val preparedJob = pendingCellularReplyJob
        val queuedJobId = pendingCellularJobId
        if (preparedJob == null && queuedJobId == null) return
        if (sending) return
        sending = true
        error = null
        scope.launch {
            try {
                val approvedJob = if (preparedJob != null) {
                    preparedJob.copy(cellularUploadApproved = true).also { accepted ->
                        ServiceLocator.coachDeliveryQueue.enqueue(accepted)
                        clearReplyComposerAfterQueueAcceptance(accepted.ownerUserId)
                    }
                } else {
                    val jobId = requireNotNull(queuedJobId)
                    val queued = threadDeliveryJobs.firstOrNull { it.jobId == jobId }
                        ?: throw IllegalStateException("待发送回复已经完成或失效")
                    if (!ServiceLocator.coachDeliveryQueue.approveCellularUpload(jobId)) {
                        throw IllegalStateException("待发送回复已经完成或失效")
                    }
                    queued.copy(cellularUploadApproved = true)
                }
                pendingCellularReplyJob = null
                pendingCellularJobId = null
                if (coachPerspective) {
                    showTransientComposerNotice(
                        workspaceComposerQueuedNotice(
                            attachmentCount = (if (approvedJob.sourceKind == "text") 0 else 1) + approvedJob.attachments.size,
                            includedVoice = approvedJob.sourceKind == "voice_recording",
                        ),
                    )
                    sending = false
                    startCoachQueuedDelivery(approvedJob)
                    return@launch
                }
                val reply = ServiceLocator.coachDeliveryQueue.deliver(
                    approvedJob.jobId,
                    ServiceLocator.coachWorkspaceRepo,
                ).getOrThrow().requireWorkspaceItem()
                appendDeliveredReply(reply)
                showTransientComposerNotice(
                    workspaceComposerSentNotice(
                        peerName = deliveryPeerName,
                        attachmentCount = (if (approvedJob.sourceKind == "text") 0 else 1) + approvedJob.attachments.size,
                        includedVoice = approvedJob.sourceKind == "voice_recording",
                    ),
                )
            } catch (throwable: Throwable) {
                pendingCellularReplyJob = null
                pendingCellularJobId = null
                error = coachThrowableError(
                    if (preparedJob == null) "视频回复暂未送达，已保留在发送队列"
                    else "未能保存发送任务，当前内容仍在草稿中",
                    throwable,
                )
            } finally {
                sending = false
            }
        }
    }

    fun openReplyLibrary() {
        if (!coachPerspective || sending) return
        showReplyLibrary = true
        replyLibraryLoading = false
        error = null
    }

    fun toggleRelatedLibraryAsset(asset: V2CoachLibraryAsset) {
        val mediaId = "asset:${asset.assetId}"
        val selected = relatedLibraryAssets.any { it.assetId == asset.assetId }
        if (!selected && relatedLibraryAssets.size + relatedLocalVideos.size >= V2_MAX_RELATED_MEDIA) {
            error = "补充视频最多选择 $V2_MAX_RELATED_MEDIA 条"
            return
        }
        relatedLibraryAssets = if (selected) {
            if (expandedRelatedMediaId == mediaId) expandedRelatedMediaId = null
            if (loadingRelatedMediaId == mediaId) loadingRelatedMediaId = null
            relatedLibraryAssets.filterNot { it.assetId == asset.assetId }
        } else {
            (relatedLibraryAssets + asset).distinctBy(V2CoachLibraryAsset::assetId)
        }
        error = null
    }

    fun toggleRelatedLibraryPreview(asset: V2CoachLibraryAsset) {
        val mediaId = "asset:${asset.assetId}"
        if (expandedRelatedMediaId == mediaId) {
            expandedRelatedMediaId = null
            return
        }
        if (!asset.videoUrl.isNullOrBlank()) {
            expandedRelatedMediaId = mediaId
            return
        }
        if (loadingRelatedMediaId != null) return
        loadingRelatedMediaId = mediaId
        scope.launch {
            runCatching { ServiceLocator.coachWorkspaceRepo.library() }
                .onSuccess { refreshed ->
                    replyLibrary = refreshed
                    val resolved = refreshed.firstOrNull { it.assetId == asset.assetId }
                    if (resolved != null) {
                        relatedLibraryAssets = relatedLibraryAssets.map { current ->
                            if (current.assetId == resolved.assetId) resolved else current
                        }
                    }
                    if (!resolved?.videoUrl.isNullOrBlank()) {
                        expandedRelatedMediaId = mediaId
                    } else {
                        error = "补充视频暂时无法预览，仍可直接发送或稍后重试"
                    }
                }
                .onFailure {
                    error = "补充视频暂时无法预览，仍可直接发送或稍后重试"
                }
            loadingRelatedMediaId = null
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::prepareReplyVideo)
    }

    fun launchReplyVideoPicker() {
        showReplyVideoSource = false
        runCatching {
            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }.onFailure { throwable ->
            error = coachThrowableError("无法打开相册，请稍后重试或改用拍摄", throwable)
            showReplyVideoSource = true
        }
    }

    val captureReplyVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { captured ->
        val file = pendingReplyCameraFile
        pendingReplyCameraFile = null
        if (captured && file != null) prepareReplyVideo(file.toUri(), file)
        else file?.delete()
    }
    val replyCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchReplyCamera(captureReplyVideo)
        else error = "需要相机权限才能拍摄视频回复"
    }
    fun requestReplyCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchReplyCamera(captureReplyVideo)
        } else {
            runCatching { replyCameraPermission.launch(Manifest.permission.CAMERA) }
                .onFailure { throwable ->
                    error = coachThrowableError("无法请求相机权限，请改从相册选择", throwable)
                    showReplyVideoSource = true
                }
        }
    }

    LaunchedEffect(threadLifecycleOwner, source.itemId) {
        val liveConversationStartedAt = SystemClock.elapsedRealtime()
        var lastFullPlaybackRefreshAt = liveConversationStartedAt
        threadLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val current = thread
            ServiceLocator.coachWorkspaceRepo.workspaceThreadUpdate(
                connectionId = connectionId,
                itemId = source.itemId,
                knownVersion = current?.version,
                knownCheckVersion = current?.checkVersion,
            ).onSuccess { refreshed ->
                if (refreshed != null) {
                    thread = mergeWorkspaceThreads(refreshed, current)
                    markLatestThreadViewedIfNeeded()
                }
                threadSyncError = null
            }.onFailure {
                if (thread == null) {
                    threadSyncError = coachThrowableError("交流内容暂时无法加载，请稍后重试", it)
                }
            }
            loading = false
            hasCompletedInitialThreadSync = true

            while (true) {
                val activeExchange = SystemClock.elapsedRealtime() - liveConversationStartedAt < 120_000L
                delay(if (activeExchange) 8_000L else 20_000L)
                val latest = thread ?: continue
                val refreshPlayback = SystemClock.elapsedRealtime() - lastFullPlaybackRefreshAt >= 60L * 60L * 1_000L
                ServiceLocator.coachWorkspaceRepo.workspaceThreadUpdate(
                    connectionId = connectionId,
                    itemId = source.itemId,
                    knownVersion = latest.version.takeUnless { refreshPlayback },
                    knownCheckVersion = latest.checkVersion.takeUnless { refreshPlayback },
                ).onSuccess { refreshed ->
                    if (refreshed != null) {
                        thread = mergeWorkspaceThreads(refreshed, latest)
                        markLatestThreadViewedIfNeeded()
                    }
                    threadSyncError = null
                    if (refreshPlayback) lastFullPlaybackRefreshAt = SystemClock.elapsedRealtime()
                }.onFailure {
                    // 已有交流可继续阅读时，周期校验失败保持安静；首次载入和
                    // 用户主动加载旧回复仍由各自的恢复入口承担。
                }
            }
        }
    }

    LaunchedEffect(source.itemId, coachPerspective) {
        if (!coachPerspective) return@LaunchedEffect
        quickRepliesLoading = true
        ServiceLocator.coachWorkspaceRepo.quickReplies()
            .onSuccess { quickReplies = it }
            .onFailure { throwable -> error = coachThrowableError("常用回复暂时无法同步，请稍后重试", throwable) }
        quickRepliesLoading = false
    }

    LaunchedEffect(source.itemId, selectedTab, thread?.replies?.size) {
        val currentThread = thread ?: return@LaunchedEffect
        val visibleReplies = currentThread.replies.filterNot(V2CoachWorkspaceItem::isRemovedContent)
        val newCount = visibleReplies.size
        val oldCount = observedVisibleReplyCount
        observedVisibleReplyCount = newCount
        if (oldCount < 0) {
            if (selectedTab == WorkspaceThreadTab.DISCUSSION && newCount > 0) {
                delay(50)
                threadScrollState.scrollTo(threadScrollState.maxValue)
            }
            return@LaunchedEffect
        }
        if (selectedTab != WorkspaceThreadTab.DISCUSSION || newCount <= oldCount) return@LaunchedEffect
        val newestReplyIsMine = visibleReplies.lastOrNull()?.let {
            workspaceReplyIsMine(it, coachPerspective, friendSpace)
        } == true
        if (isNearThreadBottom() || newestReplyIsMine) {
            unseenIncomingReplyCount = 0
            delay(50)
            threadScrollState.animateScrollTo(threadScrollState.maxValue)
        } else {
            unseenIncomingReplyCount += newCount - oldCount
        }
    }

    LaunchedEffect(source.itemId, selectedTab, threadScrollState) {
        if (selectedTab != WorkspaceThreadTab.DISCUSSION) return@LaunchedEffect
        snapshotFlow { isNearThreadBottom() }
            .distinctUntilChanged()
            .collect { nearBottom ->
                if (nearBottom) {
                    unseenIncomingReplyCount = 0
                    markLatestThreadViewedIfNeeded()
                }
            }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == WorkspaceThreadTab.DISCUSSION) markLatestThreadViewedIfNeeded()
    }

    LaunchedEffect(Unit) { V2MemoStore.startObserving() }

    if (trimmingVideo == null && analysisSource == null && coverPickingVideo == null && analysisPlayback == null) {
        CompositionLocalProvider(
            LocalPlayWorkspaceVideo provides { lookupId, url -> requestWorkspacePlayback(lookupId, url) },
            LocalPreparingWorkspacePlaybackId provides preparingPlaybackId,
        ) {
        Dialog(
            onDismissRequest = ::close,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            val dialogView = LocalView.current
            SideEffect {
                (dialogView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING,
                )
            }
            val current = thread
            val displayedSource = current?.item ?: source
            val resources = current?.discussionResourceEntries().orEmpty()
            val visibleReplyCount = current?.replies?.count { !it.isRemovedContent } ?: source.replyCount
            val teachingDetail = !friendSpace
            val panelFocusManager = LocalFocusManager.current
            val panelKeyboard = LocalSoftwareKeyboardController.current
            V2BackHandler(enabled = teachingDetail && (teachingPanel == TeachingDetailPanel.MATERIALS || composerFocused)) {
                if (composerFocused) {
                    panelFocusManager.clearFocus(force = true)
                    panelKeyboard?.hide()
                    composerFocused = false
                } else {
                    teachingPanel = TeachingDetailPanel.DISCUSSION
                }
            }
            LaunchedEffect(openDiscussionAtTop) {
                if (openDiscussionAtTop > 0 && teachingPanel == TeachingDetailPanel.DISCUSSION) {
                    threadScrollState.scrollTo(0)
                }
            }
            Box(Modifier.fillMaxSize().background(CoachPaper)) {
                Column(Modifier.fillMaxSize().imePadding()) {
                    WorkspaceThreadTopBar(
                        title = if (teachingDetail) {
                            peerName.trim().ifEmpty { "教学交流" }
                        } else {
                            workspaceThreadTitle(peerName, coachPerspective, friendSpace)
                        },
                        subtitle = if (teachingDetail) {
                            if (connectionStatus != "active") "只读" else ""
                        } else {
                            workspaceThreadSubtitle(
                                friendSpace = friendSpace,
                                connectionStatus = connectionStatus,
                                source = displayedSource,
                            )
                        },
                        peerName = peerName,
                        peerAvatar = peerAvatar,
                        selectedTab = selectedTab,
                        replyCount = visibleReplyCount,
                        canDeleteRoot = displayedSource.isMine && !displayedSource.isRemovedContent,
                        onBack = ::close,
                        onSelectContent = { selectedTab = WorkspaceThreadTab.CONTENT },
                        onSelectDiscussion = { selectedTab = WorkspaceThreadTab.DISCUSSION },
                        onDeleteRoot = { pendingDeleteItem = displayedSource },
                        onOpenTeachingProfile = onOpenTeachingProfile,
                        barColor = CoachPaper,
                        showsPeerAvatar = !teachingDetail,
                        onOpenPracticeList = if (teachingDetail) {
                            { showPracticeList = true }
                        } else {
                            null
                        },
                        barHeight = if (teachingDetail) 56.dp else DSLayout.topBarHeight,
                    )
                    playbackNotice?.let { notice ->
                        Text(
                            notice,
                            color = Color(0xFFB23B35),
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (teachingDetail && !displayedSource.isRemovedContent) {
                            val visibleReplies = current?.replies
                                ?.filterNot(V2CoachWorkspaceItem::isRemovedContent).orEmpty()
                            val authorName = when {
                                displayedSource.senderRole == "coach" && coachPerspective -> "我"
                                displayedSource.senderRole == "coach" -> "教练"
                                coachPerspective -> peerName.ifBlank { "学员" }
                                else -> "我"
                            }
                            val linkedMemo = displayedSource.sourceRecordId?.let { raw ->
                                runCatching { UUID.fromString(raw) }.getOrNull()
                            }?.let { id -> citationRecords.firstOrNull { it.id == id } }
                            Column(Modifier.fillMaxSize()) {
                                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                                val preferredVideo = if (composerFocused) 180.dp else 240.dp
                                val videoHeight = preferredVideo.coerceAtMost((maxHeight - 160.dp).coerceAtLeast(0.dp))
                                Column(Modifier.fillMaxSize()) {
                                if (videoHeight > 0.dp) {
                                    TeachingThreadVideoHeader(
                                        item = displayedSource,
                                        height = videoHeight,
                                        onPlayAsset = ::recordPlayback,
                                    )
                                }
                                Column(
                                    Modifier.weight(1f).fillMaxWidth().verticalScroll(threadScrollState),
                                ) {
                                    TeachingThreadSummary(
                                        item = displayedSource,
                                        authorName = authorName,
                                        statusLabel = teachingSpaceRowStatus(displayedSource, coachPerspective),
                                        showMemo = linkedMemo != null,
                                        expanded = summaryExpanded,
                                        onOpenFullText = { summaryExpanded = true },
                                        onOpenMemo = { linkedMemo?.let { viewingMemoId = it.id } },
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp, bottom = 8.dp),
                                    )
                                    TeachingDetailPanelBody(
                                        panel = teachingPanel,
                                        onSelect = { teachingPanel = it },
                                        authorName = authorName,
                                        originalText = teachingThreadSummaryText(displayedSource),
                                        replies = visibleReplies,
                                        loading = current == null,
                                        syncError = threadSyncError,
                                        loadingOlder = loadingOlderReplies,
                                        hasOlder = current?.nextCursor != null,
                                        onLoadOlder = ::loadOlderReplies,
                                        onRetrySync = ::retryThreadSync,
                                        friendSpace = friendSpace,
                                        coachPerspective = coachPerspective,
                                        conversationReadOnly = readOnly || displayedSource.isRemovedContent,
                                        displayedSource = displayedSource,
                                        allMessages = listOf(displayedSource).filterNot(V2CoachWorkspaceItem::isRemovedContent) + visibleReplies,
                                        savingAssetIds = savingAssetIds,
                                        scrollToReplyItemId = scrollToReplyItemId,
                                        onConsumeScrollTarget = { scrollToReplyItemId = null },
                                        onQuote = { replyToItemId = it; replyAtSecondsText = "" },
                                        onDelete = { pendingDeleteItem = it },
                                        onSaveAsset = if (coachPerspective) ::saveAsset else null,
                                        onPlayAsset = ::recordPlayback,
                                        resources = current?.discussionResourceEntries().orEmpty(),
                                        canAddResource = coachPerspective && !readOnly && displayedSource.deletedAt == null,
                                        onAddResource = ::openReplyLibrary,
                                        onOpenResourceDiscussion = { messageId ->
                                            teachingPanel = TeachingDetailPanel.DISCUSSION
                                            scrollToReplyItemId = messageId
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = 12.dp)
                                            .padding(bottom = 12.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, ContentSectionLine, RoundedCornerShape(12.dp))
                                            .background(ContentDiscussionWell),
                                    )
                                }
                                }
                                }
                            }
                        } else Column(
                            Modifier.fillMaxSize().verticalScroll(threadScrollState)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val visibleReplies = current?.replies
                                ?.filterNot(V2CoachWorkspaceItem::isRemovedContent).orEmpty()
                            val allMessages = listOf(displayedSource)
                                .filterNot(V2CoachWorkspaceItem::isRemovedContent) + visibleReplies
                            val conversationReadOnly = readOnly || displayedSource.isRemovedContent
                            if (displayedSource.isRemovedContent) {
                                Text(
                                    "这条内容已删除",
                                    color = DSColors.shadowGray,
                                    fontSize = 13.sp,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                                    textAlign = TextAlign.Center,
                                )
                            } else {
                                    Text(
                                        "交流",
                                        color = CoachInk,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                    if (current?.nextCursor != null) {
                                        WorkspaceLoadOlderRepliesButton(
                                            loading = loadingOlderReplies,
                                            onClick = ::loadOlderReplies,
                                        )
                                    }
                                    if (current == null) {
                                        WorkspaceThreadDiscussionSyncState(
                                            errorMessage = threadSyncError,
                                            onRetry = ::retryThreadSync,
                                        )
                                    } else if (visibleReplies.isEmpty()) {
                                        WorkspaceDiscussionEmptyState(videoContext = !friendSpace)
                                    } else {
                                        val unreadBoundary = workspaceFirstUnreadReplyIndex(
                                            source = source,
                                            replies = visibleReplies,
                                            coachPerspective = coachPerspective,
                                            friendSpace = friendSpace,
                                        )
                                        val unreadCount = unreadBoundary?.let { boundary ->
                                            workspaceUnreadReplyCount(
                                                replies = visibleReplies,
                                                boundary = boundary,
                                                coachPerspective = coachPerspective,
                                                friendSpace = friendSpace,
                                            )
                                        } ?: 0
                                        visibleReplies.forEachIndexed { index, reply ->
                                            if (workspaceShouldShowReplyTimeDivider(displayedSource, visibleReplies, index) && unreadBoundary != index) {
                                                WorkspaceReplyTimeDivider(reply.createdAt)
                                            }
                                            if (unreadBoundary == index) {
                                                WorkspaceNewReplyDivider(
                                                    count = unreadCount,
                                                    coachPerspective = coachPerspective,
                                                    friendSpace = friendSpace,
                                                )
                                            }
                                            val isMine = workspaceReplyIsMine(reply, coachPerspective, friendSpace)
                                            val referenced = allMessages.firstOrNull { it.itemId == reply.replyToItemId }
                                            ThreadMessage(
                                                item = reply,
                                                referencedItem = referenced,
                                                isMine = isMine,
                                                friendSpace = friendSpace,
                                                showsIdentity = workspaceMessageStartsGroup(index, visibleReplies, friendSpace),
                                                deliveryStatusText = workspaceReplyDeliveryStatus(
                                                    root = displayedSource,
                                                    reply = reply,
                                                    index = index,
                                                    replies = visibleReplies,
                                                    coachPerspective = coachPerspective,
                                                    friendSpace = friendSpace,
                                                ),
                                                savingAssetIds = savingAssetIds,
                                                onSaveAsset = if (coachPerspective) ::saveAsset else null,
                                                onPlayAsset = ::recordPlayback,
                                                onDelete = if (reply.isMine && reply.deletedAt == null) {
                                                    { pendingDeleteItem = reply }
                                                } else null,
                                                onQuote = if (!conversationReadOnly && !isMine && reply.deletedAt == null) {
                                                    {
                                                        replyToItemId = reply.itemId
                                                        replyAtSecondsText = ""
                                                    }
                                                } else null,
                                                onOpenReference = referenced?.let { target ->
                                                    if (target.parentItemId == null) {
                                                        { scope.launch { threadScrollState.animateScrollTo(0) } }
                                                    } else {
                                                        {
                                                            selectedTab = WorkspaceThreadTab.DISCUSSION
                                                            scrollToReplyItemId = target.itemId
                                                        }
                                                    }
                                                },
                                                bringIntoView = scrollToReplyItemId == reply.itemId,
                                                onBroughtIntoView = {
                                                    if (scrollToReplyItemId == reply.itemId) scrollToReplyItemId = null
                                                },
                                            )
                                        }
                                    }
                                    if (current != null) {
                                        threadSyncError?.let {
                                            Text(it, color = DSColors.flagRed, fontSize = 11.sp)
                                        }
                                    }
                                    if (conversationReadOnly) {
                                        Text(
                                            if (displayedSource.deletedAt != null) "原内容已删除，历史交流仅可查看" else "连接已解除，历史交流仅可查看",
                                            color = DSColors.shadowGray,
                                            fontSize = 12.sp,
                                        )
                                    }
                                    val related = current?.discussionResourceEntries().orEmpty()
                                    Row(
                                        Modifier.fillMaxWidth().padding(top = 8.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White)
                                            .border(1.dp, CoachLine, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.LibraryBooks,
                                            contentDescription = null,
                                            tint = CoachGreen,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Text(
                                            "相关资料",
                                            color = CoachInk,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (coachPerspective && !conversationReadOnly) {
                                            Row(
                                                Modifier.clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFE7F2EC))
                                                    .border(1.dp, CoachGreen, RoundedCornerShape(10.dp))
                                                    .clickable { openReplyLibrary() }
                                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            ) {
                                                Icon(
                                                    Icons.Filled.Add,
                                                    contentDescription = null,
                                                    tint = CoachGreen,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Text(
                                                    "添加资料",
                                                    color = CoachGreen,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                            }
                                        }
                                    }
                                    if (related.isNotEmpty()) {
                                        Row(
                                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            related.forEach { resource ->
                                                TeachingThreadMaterialCard(
                                                    resource = resource,
                                                    onOpenDiscussion = { messageId ->
                                                        scrollToReplyItemId = messageId
                                                    },
                                                    onPlayAsset = ::recordPlayback,
                                                )
                                            }
                                        }
                                    }
                            }
                            if (selectedTab != WorkspaceThreadTab.DISCUSSION || conversationReadOnly) {
                                error?.let { Text(it, color = Color(0xFFB23B35), fontSize = 12.sp) }
                            }
                        }
                        if (selectedTab == WorkspaceThreadTab.DISCUSSION && unseenIncomingReplyCount > 0) {
                            WorkspaceNewRepliesButton(
                                count = unseenIncomingReplyCount,
                                onClick = {
                                    unseenIncomingReplyCount = 0
                                    scope.launch {
                                        threadScrollState.animateScrollTo(threadScrollState.maxValue)
                                        markLatestThreadViewedIfNeeded()
                                    }
                                },
                                modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
                            )
                        }
                    }
                    val conversationReadOnly = readOnly || thread?.item?.deletedAt != null
                    if (teachingDetail && !displayedSource.isRemovedContent && showReplyTools) {
                        TeachingAttachmentTray(
                            onQuoteMaterial = {
                                showReplyTools = false
                                if (coachPerspective) openReplyLibrary()
                                else {
                                    replyToItemId = source.itemId
                                    replyAtSecondsText = ""
                                }
                            },
                            onPickMedia = {
                                showReplyTools = false
                                launchReplyVideoPicker()
                            },
                        )
                    }
                    if (selectedTab == WorkspaceThreadTab.DISCUSSION && !conversationReadOnly) {
                        val composerMessages = thread?.let { value -> listOf(value.item) + value.replies }.orEmpty()
                        val composerReplyTarget = composerMessages.firstOrNull { it.itemId == replyToItemId }
                        val attachmentCount = (if (pendingVideo != null) 1 else 0) +
                            relatedLibraryAssets.size + relatedLocalVideos.size
                        WorkspaceInlineReplyComposer(
                            leadingContent = {
                                WorkspaceComposerMediaDraft(
                                    primaryVideo = pendingVideo,
                                    relatedLocalVideos = relatedLocalVideos,
                                    relatedLibraryAssets = relatedLibraryAssets,
                                    coachPerspective = coachPerspective,
                                    saveVideoToLibrary = saveVideoToLibrary,
                                    editingLocked = sending,
                                    expandedRelatedMediaId = expandedRelatedMediaId,
                                    loadingRelatedMediaId = loadingRelatedMediaId,
                                    onTrimPrimary = { prepared ->
                                        trimError = null
                                        trimmingVideo = prepared
                                    },
                                    onPickPrimaryCover = { prepared ->
                                        coverError = null
                                        coverPickingVideo = prepared
                                    },
                                    onToggleSavePrimary = { saveVideoToLibrary = !saveVideoToLibrary },
                                    onRemovePrimary = { prepared ->
                                        prepared.deleteTemporaryFiles()
                                        pendingVideo = null
                                        saveVideoToLibrary = false
                                    },
                                    onRemoveLocalRelated = { prepared ->
                                        val mediaId = "local:${prepared.uploadId}"
                                        if (expandedRelatedMediaId == mediaId) expandedRelatedMediaId = null
                                        if (loadingRelatedMediaId == mediaId) loadingRelatedMediaId = null
                                        prepared.deleteTemporaryFiles()
                                        relatedLocalVideos = relatedLocalVideos.filterNot {
                                            it.uploadId == prepared.uploadId
                                        }
                                    },
                                    onRemoveLibraryRelated = ::toggleRelatedLibraryAsset,
                                    onToggleLocalRelatedPreview = { prepared ->
                                        val mediaId = "local:${prepared.uploadId}"
                                        expandedRelatedMediaId = if (expandedRelatedMediaId == mediaId) null else mediaId
                                    },
                                    onToggleLibraryRelatedPreview = ::toggleRelatedLibraryPreview,
                                    onAddRelated = ::openReplyLibrary,
                                )
                                if (analysisPreparingItemId != null) {
                                    WorkspaceComposerAnalysisPreparation(
                                        progress = analysisPreparationProgress,
                                        onCancel = ::cancelProfessionalAnalysisPreparation,
                                    )
                                }
                            },
                            text = draft,
                            onTextChange = {
                                draft = it.take(2_000)
                                error = null
                            },
                            placeholder = if (friendSpace) "回复好友…" else "说点什么…",
                            replyTarget = composerReplyTarget,
                            replyAtSecondsText = replyAtSecondsText,
                            onReplyAtSecondsChange = {
                                replyAtSecondsText = it.filter { char -> char.isDigit() || char == '.' }.take(8)
                                error = null
                            },
                            onCancelReply = {
                                replyToItemId = null
                                replyAtSecondsText = ""
                            },
                            prefersVoiceInput = prefersVoiceInput,
                            recordingVoice = recordingVoice,
                            requestingVoicePermission = requestingVoicePermission,
                            cancellingVoice = cancellingVoice,
                            onToggleInputMode = { prefersVoiceInput = !prefersVoiceInput },
                            onInputFocusChanged = { focused ->
                                composerFocused = focused
                                if (focused && teachingDetail) {
                                    teachingPanel = TeachingDetailPanel.DISCUSSION
                                    showReplyTools = false
                                }
                            },
                            onVoicePressStart = ::beginVoicePress,
                            onVoicePressCancelChange = { cancellingVoice = it },
                            onVoicePressEnd = ::finishVoiceRecording,
                            attachmentCount = attachmentCount,
                            onToggleTools = {
                                if (teachingDetail && !displayedSource.isRemovedContent) {
                                    val opening = !showReplyTools
                                    showReplyTools = opening
                                    if (opening) {
                                        panelFocusManager.clearFocus(force = true)
                                        panelKeyboard?.hide()
                                        composerFocused = false
                                    }
                                } else {
                                    showReplyTools = true
                                }
                            },
                            deliveryPeerName = deliveryPeerName,
                            noticeMessage = composerNotice,
                            restoredDraftNotice = showRestoredDraftNotice,
                            errorMessage = error,
                            onDismissError = { error = null },
                            draftSavedAtMillis = draftSavedAtMillis,
                            activeDeliveryJob = activeThreadDeliveryJob.takeIf { !coachPerspective },
                            activeDeliveryPhase = deliveryState.activePhase.takeIf {
                                activeThreadDeliveryJob != null && !coachPerspective
                            },
                            activeDeliveryProgress = deliveryState.activeProgress,
                            queuedDeliveryJob = latestThreadDeliveryJob?.takeIf { job ->
                                activeThreadDeliveryJob == null && (job.pausedByUser || !job.lastError.isNullOrBlank())
                            },
                            networkConnected = networkConnected,
                            deliveryQueueBusy = deliveryState.activeJobId != null || deliveryState.jobs.any { job ->
                                job.ownerUserId == draftOwnerUserId &&
                                    !job.pausedByUser && job.lastError.isNullOrBlank()
                            },
                            coachPerspective = coachPerspective,
                            onStopDelivery = activeThreadDeliveryJob?.takeIf { !coachPerspective }?.let { job ->
                                {
                                    scope.launch {
                                        ServiceLocator.coachDeliveryQueue.stop(job.jobId)
                                    }
                                }
                            },
                            onRetryDelivery = latestThreadDeliveryJob?.let { job ->
                                { retryQueuedReply(job) }
                            },
                            onClearDraft = { showDiscardReplyDraftConfirmation = true },
                            canSend = thread != null && !sending &&
                                (draft.isNotBlank() || pendingVideo != null || relatedLibraryAssets.isNotEmpty() || relatedLocalVideos.isNotEmpty()),
                            sending = sending,
                            studentPrivacyNote = !coachPerspective && !friendSpace,
                            onSend = { pendingVideo?.let(::sendVideoReply) ?: sendTextReply() },
                            contentDiscussion = teachingDetail && !displayedSource.isRemovedContent,
                            attachmentTrayOpen = showReplyTools,
                        )
                    } else {
                        Row(
                            Modifier.fillMaxWidth().background(DSColors.cardWhite).v2NavigationBarPadding()
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (selectedTab == WorkspaceThreadTab.DISCUSSION && conversationReadOnly) {
                                Text(
                                    if (thread?.item?.isRemovedContent == true) "原内容已删除，历史交流仅可查看"
                                    else "连接已解除，历史交流仅可查看",
                                    color = DSColors.shadowGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                viewingMemoId?.let { memoId ->
                    citationRecords.firstOrNull { it.id == memoId }?.let { memo ->
                        TeachingLinkedMemoOverlay(memo = memo, onBack = { viewingMemoId = null })
                    }
                }
            }
        }
        }
    }

    if (showReplyTools && (friendSpace || source.isRemovedContent)) {
        WorkspaceReplyToolsSheet(
            coachPerspective = coachPerspective,
            sourceIsPractice = source.resolvedKind == "student_practice",
            materialLimitReached = relatedLibraryAssets.size + relatedLocalVideos.size >= V2_MAX_RELATED_MEDIA,
            onCamera = {
                showReplyTools = false
                requestReplyCamera()
            },
            onGallery = {
                showReplyTools = false
                launchReplyVideoPicker()
            },
            onMaterial = {
                showReplyTools = false
                if (coachPerspective) openReplyLibrary()
                else {
                    replyToItemId = source.itemId
                    replyAtSecondsText = ""
                    scope.launch { threadScrollState.animateScrollTo(threadScrollState.maxValue) }
                }
            },
            onReferenceSource = {
                showReplyTools = false
                replyToItemId = source.itemId
                replyAtSecondsText = ""
            },
            onDismiss = { showReplyTools = false },
        )
    }

    if (showDiscardReplyDraftConfirmation) {
        V2WorkspaceConfirmationSheet(
            title = "清空这份草稿？",
            message = "文字和本机视频会从这份未发送内容中移除；已经发送的交流不受影响。",
            confirmLabel = "清空草稿",
            cancelLabel = "继续编辑",
            busy = false,
            testTag = "workspace-discard-reply-draft-sheet",
            onConfirm = {
                showDiscardReplyDraftConfirmation = false
                discardComposerDraft()
            },
            onDismiss = { showDiscardReplyDraftConfirmation = false },
        )
    }

    if (pendingPrimaryVideoReplacement != null) {
        V2WorkspaceConfirmationSheet(
            title = "替换当前视频？",
            message = "替换后，当前主视频会从这份草稿中移除；已经关联的教学素材仍会保留。",
            confirmLabel = "替换视频",
            cancelLabel = "保留当前视频",
            busy = false,
            destructive = false,
            testTag = "workspace-primary-video-replacement-sheet",
            onConfirm = {
                pendingPrimaryVideoReplacement?.let(::usePrimaryVideo)
            },
            onDismiss = ::discardPendingPrimaryVideoReplacement,
        )
    }

    if (showCitationPicker) {
        V2CitationPickerDialog(
            records = citationRecords.filter { it.kind == V2MemoKind.VIDEO },
            onDismiss = { showCitationPicker = false },
            onInsert = { title ->
                val line = "引用《$title》"
                draft = if (draft.isBlank()) line else "$draft\n$line"
                showCitationPicker = false
            },
        )
    }

    pendingDeleteItem?.let { item ->
        V2WorkspaceConfirmationSheet(
            title = if (item.parentItemId == null) "删除这条动态？" else "删除这条回复？",
            message = if (item.parentItemId == null) {
                "这条内容会从空间里移除。已经发出的交流记录也会一起不再显示。"
            } else {
                "删除后会保留一个占位，其他人将不能再查看原内容。"
            },
            confirmLabel = "删除",
            busyLabel = "删除中…",
            cancelLabel = "取消",
            busy = deletingItemId != null,
            testTag = "workspace-delete-item-sheet",
            onConfirm = { deleteItem(item) },
            onDismiss = { pendingDeleteItem = null },
        )
    }

    if (showQuickReplies) {
        V2CoachQuickRepliesSheet(
            replies = quickReplies,
            onRepliesChanged = { quickReplies = it },
            onSelect = ::useQuickReply,
            onDismiss = { showQuickReplies = false },
        )
    }

    saveAssetNotice?.let { notice ->
        WorkspaceAssetSaveResultDialog(
            message = notice,
            onDismiss = { saveAssetNotice = null },
        )
    }

    if (showReplyVideoSource) {
        WorkspaceReplyVideoSourceDialog(
            onCamera = ::requestReplyCamera,
            onGallery = ::launchReplyVideoPicker,
            onDismiss = { showReplyVideoSource = false },
        )
    }

    if (showReplyLibrary) {
        CoachLibraryPickerDialog(
            assets = replyLibrary,
            loading = replyLibraryLoading,
            sending = sending,
            onDismiss = { if (!sending) showReplyLibrary = false },
            onSelect = ::toggleRelatedLibraryAsset,
            onAssetsChanged = { replyLibrary = it },
            selectedAssetIds = relatedLibraryAssets.mapTo(mutableSetOf(), V2CoachLibraryAsset::assetId),
            maxSelectedCount = (V2_MAX_RELATED_MEDIA - relatedLocalVideos.size).coerceAtLeast(0),
            multiSelect = true,
        )
    }

    if (pendingCellularReplyJob != null || pendingCellularJobId != null) {
        V2CellularUploadConfirmationDialog(
            fileSizeBytes = pendingCellularFileSize,
            busy = sending,
            sendMode = true,
            onConfirm = ::confirmCellularReply,
            onDismiss = {
                pendingCellularReplyJob = null
                pendingCellularJobId = null
                error = null
            },
        )
    }

    trimmingVideo?.let { prepared ->
        V2SwingAnalysisTrimScreen(
            imported = prepared.asVideoImport(),
            saving = trimSaving,
            error = trimError,
            secondaryActionTitle = "不裁剪",
            onCancel = {
                trimmingVideo = null
                trimError = null
                selectedTab = WorkspaceThreadTab.DISCUSSION
            },
            onUse = { startMs, endMs ->
                if (trimSaving) return@V2SwingAnalysisTrimScreen
                trimSaving = true
                trimError = null
                scope.launch {
                    try {
                        val ready = trimPreparedWorkspaceVideo(context, prepared, startMs, endMs)
                        pendingVideo = ready
                        trimmingVideo = null
                        selectedTab = WorkspaceThreadTab.DISCUSSION
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (throwable: Throwable) {
                        trimError = coachThrowableError("视频裁剪失败，请重新选择或使用整段", throwable)
                    } finally {
                        trimSaving = false
                    }
                }
            },
            extraControls = if (coachPerspective && prepared.sourceKind != "album") ({
                WorkspaceSaveToLibraryToggle(
                    checked = saveVideoToLibrary,
                    onCheckedChange = { saveVideoToLibrary = it },
                )
            }) else null,
        )
    }

    coverPickingVideo?.let { prepared ->
        V2WorkspaceVideoCoverPickerScreen(
            prepared = prepared,
            saving = coverSaving,
            error = coverError,
            onSkip = {
                if (!coverSaving) {
                    coverPickingVideo = null
                    coverError = null
                }
            },
            onConfirm = { selectedSeconds ->
                if (!coverSaving) {
                    coverSaving = true
                    coverError = null
                    scope.launch {
                        try {
                            val updated = replaceWorkspaceVideoCover(prepared, selectedSeconds)
                            if (pendingVideo?.uploadId == prepared.uploadId) pendingVideo = updated
                            coverPickingVideo = null
                            showTransientComposerNotice("封面已更新，发送时会同步给学员")
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (throwable: Throwable) {
                            coverError = coachThrowableError("封面暂时无法保存，请稍后重试", throwable)
                        } finally {
                            coverSaving = false
                        }
                    }
                }
            },
        )
    }

    if (showPracticeList) {
        CoachPracticeListSheet(
            studentName = peerName,
            items = practiceItems,
            currentItemId = source.itemId,
            onDismiss = { showPracticeList = false },
            onSelect = { item ->
                showPracticeList = false
                onSelectPractice(item)
            },
        )
    }

    analysisPlayback?.let { playback ->
        V2WorkspaceProfessionalAnalysisScreen(
            mediaUri = playback.uri,
            title = playback.title,
            subtitle = playback.subtitle,
            startsPlaying = true,
            continuousPlayback = true,
            mode = V2ProfessionalAnalysisMode.STANDALONE_REVIEW,
            onCancel = { analysisPlayback = null },
            onRecordingReady = { recording ->
                scope.launch {
                    val saved = runCatching { saveExportedVideoToGallery(context, recording) }.isSuccess
                    recording.delete()
                    android.widget.Toast.makeText(
                        context,
                        if (saved) "录屏已保存到相册" else "录屏暂时无法保存",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        )
    }

    analysisSource?.let { imported ->
        V2WorkspaceProfessionalAnalysisScreen(
            sourceFile = File(imported.absolutePath),
            title = analysisTitle,
            subtitle = analysisSubtitle,
            startsWithRecordingOptions = analysisStartsWithRecordingOptions,
            onCancel = {
                File(imported.absolutePath).delete()
                imported.thumbnailPath?.let(::File)?.delete()
                analysisSource = null
            },
            onRecordingReady = ::acceptProfessionalAnalysisRecording,
        )
    }
}

@Composable
internal fun WorkspaceComposerMediaDraft(
    primaryVideo: V2PreparedWorkspaceVideo?,
    relatedLocalVideos: List<V2PreparedWorkspaceVideo>,
    relatedLibraryAssets: List<V2CoachLibraryAsset>,
    coachPerspective: Boolean,
    saveVideoToLibrary: Boolean,
    editingLocked: Boolean,
    expandedRelatedMediaId: String? = null,
    loadingRelatedMediaId: String? = null,
    onTrimPrimary: (V2PreparedWorkspaceVideo) -> Unit,
    onPickPrimaryCover: (V2PreparedWorkspaceVideo) -> Unit,
    onToggleSavePrimary: () -> Unit,
    onRemovePrimary: (V2PreparedWorkspaceVideo) -> Unit,
    onRemoveLocalRelated: (V2PreparedWorkspaceVideo) -> Unit,
    onRemoveLibraryRelated: (V2CoachLibraryAsset) -> Unit,
    onToggleLocalRelatedPreview: (V2PreparedWorkspaceVideo) -> Unit = {},
    onToggleLibraryRelatedPreview: (V2CoachLibraryAsset) -> Unit = {},
    onAddRelated: (() -> Unit)? = null,
) {
    primaryVideo?.let { prepared ->
        if (prepared.sourceKind == "voice_recording") {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(DSColors.mistGreen.copy(alpha = 0.58f)).padding(10.dp)
                    .testTag("workspace-composer-voice-draft"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(DSColors.fairway),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.GraphicEq, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("语音消息", color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "发送时上传 · ${formatBytes(prepared.file.length())}",
                        color = DSColors.shadowGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Icon(
                    Icons.Filled.Cancel,
                    "移除语音",
                    tint = DSColors.shadowGray,
                    modifier = Modifier.size(20.dp).clickable(enabled = !editingLocked) { onRemovePrimary(prepared) },
                )
            }
        } else {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(DSColors.mistGreen.copy(alpha = 0.52f)).padding(11.dp)
                    .testTag("workspace-composer-primary-media"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AsyncImage(
                        model = prepared.cover,
                        contentDescription = "待发送视频封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(width = 52.dp, height = 42.dp)
                            .clip(RoundedCornerShape(10.dp)).background(DSColors.borderLine),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            when (prepared.sourceKind) {
                                "analysis_recording" -> "口播分析视频"
                                "student_recording" -> "复练视频"
                                else -> if (coachPerspective) "教学视频" else "练习视频"
                            },
                            color = DSColors.inkGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            "待上传 · ${formatWorkspaceDuration(prepared.durationSeconds ?: 0.0)} · 发送前可调整",
                            color = DSColors.shadowGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Icon(
                        Icons.Filled.Cancel,
                        "移除视频",
                        tint = DSColors.shadowGray,
                        modifier = Modifier.size(20.dp).clickable(enabled = !editingLocked) { onRemovePrimary(prepared) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    WorkspaceComposerMiniAction("裁剪", enabled = !editingLocked) { onTrimPrimary(prepared) }
                    WorkspaceComposerMiniAction(
                        if (prepared.coverTimeSeconds == null) "选封面" else "换封面",
                        emphasized = prepared.coverTimeSeconds != null,
                        enabled = !editingLocked,
                    ) { onPickPrimaryCover(prepared) }
                    if (coachPerspective && prepared.sourceKind != "album") {
                        WorkspaceComposerMiniAction(
                            "存素材",
                            emphasized = saveVideoToLibrary,
                            enabled = !editingLocked,
                            onClick = onToggleSavePrimary,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    val relatedCount = relatedLocalVideos.size + relatedLibraryAssets.size
    if (relatedCount > 0) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFFF4DC).copy(alpha = 0.72f)).padding(11.dp)
                .testTag("workspace-composer-related-media"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VideoLibrary, null, tint = DSColors.fairway, modifier = Modifier.size(15.dp))
                Text(
                    "补充视频",
                    color = DSColors.fairway,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp).weight(1f),
                )
                Text("$relatedCount/$V2_MAX_RELATED_MEDIA", color = DSColors.shadowGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            val expandedLocal = relatedLocalVideos.firstOrNull {
                expandedRelatedMediaId == "local:${it.uploadId}"
            }
            val expandedAsset = relatedLibraryAssets.firstOrNull {
                expandedRelatedMediaId == "asset:${it.assetId}"
            }
            val expandedSource = expandedLocal?.file?.toUri()?.toString() ?: expandedAsset?.videoUrl
            if (!expandedSource.isNullOrBlank()) {
                WorkspaceComposerRelatedMediaPreview(
                    mediaSource = expandedSource,
                    title = expandedAsset?.title?.ifBlank { "补充视频" } ?: "补充视频",
                    aspectRatio = expandedAsset?.let { asset ->
                        val width = asset.width?.takeIf { it > 0 }
                        val height = asset.height?.takeIf { it > 0 }
                        if (width != null && height != null) width.toFloat() / height.toFloat() else null
                    } ?: (16f / 9f),
                )
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                relatedLocalVideos.forEach { prepared ->
                    val mediaId = "local:${prepared.uploadId}"
                    WorkspaceComposerRelatedMediaCard(
                        cover = prepared.cover,
                        title = "相册视频",
                        enabled = !editingLocked,
                        expanded = expandedRelatedMediaId == mediaId,
                        loading = loadingRelatedMediaId == mediaId,
                        onPreview = { onToggleLocalRelatedPreview(prepared) },
                        onRemove = { onRemoveLocalRelated(prepared) },
                    )
                }
                relatedLibraryAssets.forEach { asset ->
                    val mediaId = "asset:${asset.assetId}"
                    WorkspaceComposerRelatedMediaCard(
                        cover = asset.coverUrl,
                        title = asset.title.ifBlank { "教学素材" },
                        enabled = !editingLocked,
                        expanded = expandedRelatedMediaId == mediaId,
                        loading = loadingRelatedMediaId == mediaId,
                        onPreview = { onToggleLibraryRelatedPreview(asset) },
                        onRemove = { onRemoveLibraryRelated(asset) },
                    )
                }
                if (relatedCount < V2_MAX_RELATED_MEDIA && onAddRelated != null) {
                    Column(
                        Modifier.size(width = 78.dp, height = 82.dp).clip(RoundedCornerShape(11.dp))
                            .background(DSColors.cardWhite.copy(alpha = 0.92f))
                            .drawBehind {
                                drawRoundRect(
                                    color = DSColors.borderLine,
                                    cornerRadius = CornerRadius(11.dp.toPx()),
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                                        ),
                                    ),
                                )
                            }
                            .clickable(enabled = !editingLocked, onClick = onAddRelated)
                            .testTag("workspace-composer-add-related"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Filled.Add, null, tint = DSColors.fairway, modifier = Modifier.size(15.dp))
                        Text("再加一条", color = DSColors.fairway, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceComposerMiniAction(
    title: String,
    emphasized: Boolean = false,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        title,
        color = when {
            !enabled -> DSColors.shadowGray.copy(alpha = 0.55f)
            emphasized -> DSColors.trophyGold
            else -> DSColors.fairway
        },
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.height(26.dp).clip(RoundedCornerShape(13.dp))
            .background(DSColors.cardWhite.copy(alpha = 0.90f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp),
    )
}

@Composable
private fun WorkspaceComposerRelatedMediaCard(
    cover: Any?,
    title: String,
    enabled: Boolean,
    expanded: Boolean,
    loading: Boolean,
    onPreview: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(Modifier.width(92.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            Modifier.clickable(enabled = enabled && !loading, onClick = onPreview)
                .semantics {
                    contentDescription = if (expanded) "收起补充视频预览" else "预览补充视频"
                },
        ) {
            AsyncImage(
                model = cover,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 92.dp, height = 58.dp)
                    .clip(RoundedCornerShape(10.dp)).background(DSColors.borderLine),
            )
            Box(
                Modifier.align(Alignment.Center).size(27.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center,
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(15.dp), color = Color.White, strokeWidth = 1.5.dp)
                } else {
                    Icon(
                        if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title,
                color = DSColors.inkGreen,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.Cancel,
                "移除$title",
                tint = DSColors.shadowGray,
                modifier = Modifier.size(14.dp).clickable(enabled = enabled, onClick = onRemove),
            )
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
internal val LocalPlayWorkspaceVideo = staticCompositionLocalOf<(String, String?) -> Unit> { { _, _ -> } }

internal val LocalPreparingWorkspacePlaybackId = staticCompositionLocalOf<String?> { null }

internal fun workspaceStreamPlaybackUrl(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return null
    val lower = value.lowercase()
    return when {
        lower.startsWith("https://") || lower.startsWith("http://") ||
            lower.startsWith("file:") || lower.startsWith("content:") ||
            value.startsWith("/") -> value
        else -> null
    }
}

private data class WorkspaceAnalysisPlayback(
    val uri: String,
    val title: String,
    val subtitle: String?,
)

internal fun workspacePlaybackUrlInThread(thread: V2CoachThread?, lookupId: String): String? {
    if (thread == null || lookupId.isBlank()) return null
    val items = listOf(thread.item) + thread.replies
    items.forEach { item ->
        if (item.itemId == lookupId) {
            val asset = item.primaryAsset?.takeUnless { it.isVoice }
            return workspaceStreamPlaybackUrl(asset?.videoUrl ?: item.videoUrl)
        }
        (listOfNotNull(item.primaryAsset) + item.attachments)
            .firstOrNull { it.assetId == lookupId }
            ?.videoUrl
            ?.let(::workspaceStreamPlaybackUrl)
            ?.let { return it }
    }
    return null
}

@Composable
private fun WorkspaceComposerRelatedMediaPreview(
    mediaSource: String,
    title: String,
    aspectRatio: Float,
) {
    val context = LocalContext.current
    val player = remember(mediaSource) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(mediaSource))
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color.Black).testTag("workspace-composer-related-preview"),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio.coerceAtLeast(1.25f)),
        )
        Text(
            title,
            color = Color.White.copy(alpha = 0.86f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.92f))
                .padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
internal fun WorkspaceComposerAnalysisPreparation(progress: Double?, onCancel: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.58f)).padding(11.dp)
            .testTag("workspace-composer-analysis-preparing"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                progress?.let { "正在准备专业分析 ${(it * 100).toInt()}%" } ?: "正在准备专业分析…",
                color = DSColors.deepGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text("取消", color = DSColors.flagRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onCancel))
        }
        progress?.let {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { it.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = DSColors.fairway,
            )
        }
    }
}

@Composable
private fun WorkspaceComposerDeliveryPreview(
    job: V2CoachDeliveryJob,
    phase: V2CoachDeliveryPhase?,
    progress: Double,
    peerName: String,
    onStop: (() -> Unit)?,
) {
    Column(Modifier.fillMaxWidth().testTag("workspace-composer-delivery-preview"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Column(
                Modifier.widthIn(max = 286.dp).clip(RoundedCornerShape(16.dp))
                    .background(DSColors.mistGreen.copy(alpha = 0.82f)).padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Lock, null, tint = DSColors.shadowGray, modifier = Modifier.size(11.dp))
                    Text("发送给 $peerName · 仅双方可见", color = DSColors.shadowGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                if (job.replyToItemId != null) {
                    Text("↩ 回复引用内容", color = DSColors.fairway, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                job.text.trim().takeIf(String::isNotEmpty)?.let {
                    Text(it, color = DSColors.inkGreen, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                }
                if (!job.videoPath.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AsyncImage(
                            model = job.coverPath?.let(::File),
                            contentDescription = "发送中的视频",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(width = 48.dp, height = 38.dp)
                                .clip(RoundedCornerShape(9.dp)).background(DSColors.deepGreen),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                when (job.sourceKind) {
                                    "analysis_recording" -> "口播分析视频"
                                    "student_recording" -> "复练视频"
                                    "voice_recording" -> "语音消息"
                                    else -> "教学视频"
                                },
                                color = DSColors.fairway,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(workspaceDeliveryStateText(phase, progress), color = DSColors.shadowGray, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (job.attachments.isNotEmpty()) {
                    Text("▣ 另有 ${job.attachments.size} 条关联教学素材", color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            if (phase == V2CoachDeliveryPhase.UPLOADING) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress.toFloat().coerceIn(0f, 1f) },
                    color = DSColors.fairway,
                    modifier = Modifier.weight(1f),
                )
            } else {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = DSColors.fairway)
            }
            Text(workspaceDeliveryStateText(phase, progress), color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            onStop?.let { stop ->
                Row(
                    Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(DSColors.mistGreen)
                        .clickable(onClick = stop).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Filled.StopCircle, null, tint = DSColors.flagRed, modifier = Modifier.size(14.dp))
                    Text("停止", color = DSColors.flagRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

internal fun workspaceDeliveryStateText(phase: V2CoachDeliveryPhase?, progress: Double): String = when (phase) {
    V2CoachDeliveryPhase.PREPARING -> "正在准备"
    V2CoachDeliveryPhase.UPLOADING -> "上传中 ${(progress.coerceIn(0.0, 1.0) * 100).toInt()}%"
    V2CoachDeliveryPhase.PUBLISHING -> "正在确认"
    null -> "正在发送"
}

@Composable
private fun WorkspaceComposerRecoveryNotice(job: V2CoachDeliveryJob, onRetry: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF4DC).copy(alpha = 0.72f))
            .border(1.dp, DSColors.trophyGold.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(11.dp).testTag("workspace-composer-delivery-recovery"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.Refresh, null, tint = DSColors.trophyGold, modifier = Modifier.size(17.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("发送暂停，内容已保留", color = DSColors.inkGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                job.lastError?.takeIf(String::isNotBlank) ?: "网络恢复后可以继续发送",
                color = DSColors.shadowGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
            )
        }
        onRetry?.let { retry ->
            Text(
                "继续发送",
                color = DSColors.fairway,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.height(30.dp).clip(RoundedCornerShape(15.dp))
                    .background(DSColors.cardWhite.copy(alpha = 0.92f)).clickable(onClick = retry)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun WorkspaceComposerLocalErrorNotice(message: String, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.58f))
            .border(1.dp, DSColors.flagRed.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(11.dp).testTag("workspace-composer-local-error"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.Cancel, null, tint = DSColors.flagRed, modifier = Modifier.padding(top = 1.dp).size(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("这次操作没有完成", color = DSColors.inkGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                message,
                color = DSColors.shadowGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
            )
        }
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(DSColors.cardWhite.copy(alpha = 0.92f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, "关闭提示", tint = DSColors.shadowGray, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun WorkspaceComposerDraftPersistenceRow(
    peerName: String,
    savedAtMillis: Long,
    attachmentCount: Int,
    onClear: () -> Unit,
) {
    var nowMillis by remember(savedAtMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(savedAtMillis) {
        while (true) {
            delay(60_000L)
            nowMillis = System.currentTimeMillis()
        }
    }
    Row(
        Modifier.fillMaxWidth().testTag("workspace-composer-draft-persistence"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.Lock, null, tint = DSColors.shadowGray, modifier = Modifier.size(11.dp))
        Text("仅你和${peerName}可见", color = DSColors.shadowGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("·", color = DSColors.shadowGray, fontSize = 9.sp)
        Text(
            workspaceDraftPersistenceText(savedAtMillis, nowMillis),
            color = DSColors.shadowGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.Ellipsis,
        )
        if (attachmentCount > 0) {
            Text("${attachmentCount}段视频", color = DSColors.fairway, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Text("清空", color = DSColors.shadowGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onClear))
    }
}

internal fun workspaceDraftPersistenceText(savedAtMillis: Long, nowMillis: Long): String {
    if (savedAtMillis <= 0L) return "正在保存草稿"
    val remaining = V2_WORKSPACE_DRAFT_LIFETIME_MILLIS - (nowMillis - savedAtMillis)
    if (remaining <= 0L) return "草稿已超过保留时间"
    if (remaining < 60_000L) return "草稿即将到期"
    if (remaining < 3_600_000L) return "草稿${(remaining + 59_999L) / 60_000L}分钟内可恢复"
    return "草稿${(remaining + 3_599_999L) / 3_600_000L}小时内可恢复"
}

@Composable
internal fun WorkspaceInlineReplyComposer(
    leadingContent: @Composable () -> Unit = {},
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String = "说点什么…",
    replyTarget: V2CoachWorkspaceItem?,
    replyAtSecondsText: String,
    onReplyAtSecondsChange: (String) -> Unit,
    onCancelReply: () -> Unit,
    prefersVoiceInput: Boolean,
    recordingVoice: Boolean,
    requestingVoicePermission: Boolean = false,
    cancellingVoice: Boolean = false,
    onToggleInputMode: () -> Unit,
    onInputFocusChanged: (Boolean) -> Unit = {},
    onVoicePressStart: () -> Unit,
    onVoicePressCancelChange: (Boolean) -> Unit,
    onVoicePressEnd: (Boolean) -> Unit,
    attachmentCount: Int,
    onToggleTools: () -> Unit,
    deliveryPeerName: String = "对方",
    noticeMessage: String? = null,
    restoredDraftNotice: Boolean = false,
    errorMessage: String? = null,
    onDismissError: (() -> Unit)? = null,
    draftSavedAtMillis: Long = 0L,
    activeDeliveryJob: V2CoachDeliveryJob? = null,
    activeDeliveryPhase: V2CoachDeliveryPhase? = null,
    activeDeliveryProgress: Double = 0.0,
    queuedDeliveryJob: V2CoachDeliveryJob? = null,
    networkConnected: Boolean = true,
    deliveryQueueBusy: Boolean = false,
    coachPerspective: Boolean = false,
    onStopDelivery: (() -> Unit)? = null,
    onRetryDelivery: (() -> Unit)? = null,
    onClearDraft: (() -> Unit)? = null,
    canSend: Boolean,
    sending: Boolean,
    studentPrivacyNote: Boolean,
    onSend: () -> Unit,
    contentDiscussion: Boolean = false,
    attachmentTrayOpen: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val voiceCancelThresholdPx = with(LocalDensity.current) { 52.dp.toPx() }
    Column(
        Modifier.fillMaxWidth().background(if (contentDiscussion) CoachPaper else Color.White)
            .testTag("workspace-thread-inline-composer"),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(if (contentDiscussion) ContentDivider else CoachLine))
        Column(
            Modifier.navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        noticeMessage?.let { message ->
            Row(
                Modifier.fillMaxWidth().testTag("workspace-composer-transient-notice"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = DSColors.deepGreen, modifier = Modifier.size(13.dp))
                Text(message, color = DSColors.deepGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (restoredDraftNotice) {
            Row(
                Modifier.fillMaxWidth().testTag("workspace-composer-restored-draft-notice"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Refresh, null, tint = DSColors.fairway, modifier = Modifier.size(13.dp))
                Text("已恢复上次未发送的内容", color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
        if (activeDeliveryJob == null) leadingContent()

        if (activeDeliveryJob == null) replyTarget?.let { target ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(DSColors.mistGreen.copy(alpha = 0.72f)).padding(horizontal = 11.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.size(width = 3.dp, height = 28.dp).clip(RoundedCornerShape(2.dp))
                            .background(DSColors.trophyGold),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("正在回复", color = DSColors.fairway, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            workspaceReplyReferencePreview(target),
                            color = DSColors.inkGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        Icons.Filled.Close,
                        "取消回复",
                        tint = DSColors.shadowGray,
                        modifier = Modifier.size(18.dp).clickable(onClick = onCancelReply).padding(2.dp),
                    )
                }
                if (target.hasVideoContent) {
                    BasicTextField(
                        value = replyAtSecondsText,
                        onValueChange = onReplyAtSecondsChange,
                        enabled = !sending,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = DSColors.inkGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(10.dp))
                            .background(DSColors.cardWhite).padding(horizontal = 10.dp)
                            .testTag("workspace-thread-reply-seconds"),
                        decorationBox = { innerTextField ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                if (replyAtSecondsText.isBlank()) {
                                    Text("时间点（秒，可选）", color = DSColors.shadowGray, fontSize = 10.sp)
                                }
                                innerTextField()
                            }
                        },
                    )
                }
            }
        }

        if (activeDeliveryJob != null) {
            WorkspaceComposerDeliveryPreview(
                job = activeDeliveryJob,
                phase = activeDeliveryPhase,
                progress = activeDeliveryProgress,
                peerName = deliveryPeerName,
                onStop = onStopDelivery,
            )
        } else if (contentDiscussion) {
            val editing = text.isNotBlank() || attachmentCount > 0
            val voiceMode = prefersVoiceInput && !editing && !attachmentTrayOpen
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(48.dp).clickable(enabled = !sending) {
                        when {
                            voiceMode -> onToggleInputMode()
                            editing -> onToggleTools()
                            attachmentTrayOpen -> {
                                onToggleTools()
                                if (!prefersVoiceInput) onToggleInputMode()
                            }
                            else -> onToggleInputMode()
                        }
                    }.then(if (editing) Modifier.testTag("workspace-thread-add-content") else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (voiceMode) {
                        Icon(Icons.Filled.Keyboard, "切换到文字输入", tint = CoachGreen, modifier = Modifier.size(22.dp))
                    } else if (editing && attachmentTrayOpen) {
                        Icon(Icons.Filled.Close, "关闭", tint = CoachGreen, modifier = Modifier.size(22.dp))
                    } else if (editing) {
                        Icon(Icons.Filled.Add, "添加内容", tint = CoachGreen, modifier = Modifier.size(22.dp))
                    } else {
                        TeachingWaveIcon(playing = false, modifier = Modifier.size(22.dp))
                    }
                }
                if (voiceMode) {
                    Box(
                        Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(ContentInput)
                            .border(1.dp, ContentDivider, RoundedCornerShape(12.dp))
                            .then(
                                if (sending) Modifier else Modifier.pointerInput(voiceCancelThresholdPx) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        down.consume()
                                        onVoicePressStart()
                                        var shouldCancel = false
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            val nextShouldCancel = change.position.y - down.position.y < -voiceCancelThresholdPx
                                            if (nextShouldCancel != shouldCancel) {
                                                shouldCancel = nextShouldCancel
                                                onVoicePressCancelChange(shouldCancel)
                                            }
                                            change.consume()
                                            if (!change.pressed) {
                                                onVoicePressEnd(shouldCancel)
                                                break
                                            }
                                        }
                                    }
                                },
                            )
                            .testTag("workspace-thread-voice-input"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            workspaceVoiceInputTitle(
                                requestingPermission = requestingVoicePermission,
                                recording = recordingVoice,
                                cancelling = cancellingVoice,
                            ),
                            color = if (cancellingVoice) DSColors.flagRed else ContentText,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        )
                    }
                } else {
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        enabled = !sending,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = ContentText,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        maxLines = 4,
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(12.dp))
                            .background(ContentInput)
                            .border(1.dp, ContentDivider, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .onFocusChanged { onInputFocusChanged(it.isFocused) }
                            .testTag("workspace-thread-composer-input"),
                        decorationBox = { innerTextField ->
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                if (text.isBlank()) {
                                    Text(placeholder, color = ContentSecondary, fontSize = 15.sp, lineHeight = 22.sp)
                                }
                                innerTextField()
                            }
                        },
                    )
                }
                if (editing) {
                    Box(
                        Modifier.padding(start = 8.dp).height(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (canSend) CoachGreen else ContentDisabled)
                            .clickable(enabled = canSend && !sending, onClick = onSend)
                            .padding(horizontal = 14.dp)
                            .testTag("workspace-thread-send-reply"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (sending) "发送中" else "发送",
                            color = Color.White,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else {
                    Box(
                        Modifier.size(48.dp).clickable(enabled = !sending, onClick = onToggleTools)
                            .testTag("workspace-thread-add-content"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (attachmentTrayOpen) Icons.Filled.Close else Icons.Filled.Add,
                            if (attachmentTrayOpen) "关闭" else "添加内容",
                            tint = CoachGreen,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        } else Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
            Box(
                Modifier.size(48.dp)
                    .clickable(enabled = !sending) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        onToggleInputMode()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (prefersVoiceInput) Icons.Filled.Keyboard else Icons.Filled.Mic,
                    if (prefersVoiceInput) "切换到文字输入" else "切换到语音输入",
                    tint = CoachGreen,
                    modifier = Modifier.size(22.dp),
                )
            }

            if (prefersVoiceInput) {
                Box(
                    Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, CoachLine, RoundedCornerShape(14.dp))
                        .then(
                            if (sending) Modifier else Modifier.pointerInput(voiceCancelThresholdPx) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()
                                    onVoicePressStart()
                                    var shouldCancel = false
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        val nextShouldCancel = change.position.y - down.position.y < -voiceCancelThresholdPx
                                        if (nextShouldCancel != shouldCancel) {
                                            shouldCancel = nextShouldCancel
                                            onVoicePressCancelChange(shouldCancel)
                                        }
                                        change.consume()
                                        if (!change.pressed) {
                                            onVoicePressEnd(shouldCancel)
                                            break
                                        }
                                    }
                                }
                            },
                        )
                        .testTag("workspace-thread-voice-input"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        workspaceVoiceInputTitle(
                            requestingPermission = requestingVoicePermission,
                            recording = recordingVoice,
                            cancelling = cancellingVoice,
                        ),
                        color = if (cancellingVoice) DSColors.flagRed else DSColors.inkGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    enabled = !sending,
                    textStyle = androidx.compose.ui.text.TextStyle(color = CoachInk, fontSize = 15.sp, lineHeight = 20.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    maxLines = 5,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, CoachLine, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 13.dp)
                        .onFocusChanged { onInputFocusChanged(it.isFocused) }
                        .testTag("workspace-thread-composer-input"),
                    decorationBox = { innerTextField ->
                        Box(Modifier.fillMaxWidth()) {
                            if (text.isBlank()) {
                                Text(placeholder, color = CoachMeta, fontSize = 15.sp)
                            }
                            innerTextField()
                        }
                    },
                )
            }

            Box(
                Modifier.size(48.dp)
                    .clickable(enabled = !sending) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        onToggleTools()
                    }.testTag("workspace-thread-add-content"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, "添加更多内容", tint = CoachGreen, modifier = Modifier.size(22.dp))
                if (attachmentCount > 0) {
                    Box(
                        Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp).size(17.dp)
                            .clip(CircleShape).background(DSColors.trophyGold)
                            .border(2.dp, DSColors.cardWhite, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(attachmentCount.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(
                Modifier.height(48.dp).widthIn(min = 64.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (canSend) CoachGreen else CoachSendIdle)
                    .clickable(enabled = canSend && !sending, onClick = onSend)
                    .padding(horizontal = 14.dp)
                    .testTag("workspace-thread-send-reply"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (sending) "发送中" else "发送",
                    color = if (canSend) Color.White else CoachSendIdleText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            }

        if (activeDeliveryJob == null && deliveryQueueBusy) {
            Row(
                Modifier.fillMaxWidth().testTag("workspace-composer-queue-busy"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier.size(13.dp).clip(CircleShape).background(DSColors.shadowGray),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.ArrowUpward, null, tint = Color.White, modifier = Modifier.size(9.dp))
                }
                Text(
                    if (coachPerspective) "已有内容正在发送，这条可继续加入队列"
                    else "另一项内容正在发送，可以先准备这条草稿",
                    color = DSColors.shadowGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else if (activeDeliveryJob == null && queuedDeliveryJob == null && !networkConnected) {
            Row(
                Modifier.fillMaxWidth().testTag("workspace-composer-offline-notice"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.WifiOff, null, tint = DSColors.shadowGray, modifier = Modifier.size(13.dp))
                Text(
                    "当前离线，回复已保留，网络恢复后可发送",
                    color = DSColors.shadowGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        queuedDeliveryJob?.let { queued ->
            WorkspaceComposerRecoveryNotice(
                job = queued,
                onRetry = onRetryDelivery,
            )
        }

        val hasDraft = text.isNotBlank() || attachmentCount > 0 || replyTarget != null
        if (activeDeliveryJob == null && hasDraft && onClearDraft != null) {
            WorkspaceComposerDraftPersistenceRow(
                peerName = deliveryPeerName,
                savedAtMillis = draftSavedAtMillis,
                attachmentCount = attachmentCount,
                onClear = onClearDraft,
            )
        }

        if (queuedDeliveryJob == null && errorMessage != null && onDismissError != null) {
            WorkspaceComposerLocalErrorNotice(errorMessage, onDismissError)
        }

        if (studentPrivacyNote) {
            Text(
                "仅限本次练习的补充讨论，不会发布到教学空间",
                color = DSColors.shadowGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkspaceReplyToolsSheet(
    coachPerspective: Boolean,
    sourceIsPractice: Boolean,
    materialLimitReached: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onMaterial: () -> Unit,
    onReferenceSource: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DSColors.warmWhite,
        contentColor = DSColors.deepGreen,
        scrimColor = Color.Black.copy(alpha = 0.30f),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = DSColors.shadowGray.copy(alpha = 0.32f))
        },
        tonalElevation = 0.dp,
        modifier = Modifier.testTag("workspace-thread-reply-tools"),
    ) {
        Column(
            Modifier.fillMaxWidth().v2NavigationBarPadding()
                .padding(horizontal = 16.dp).padding(top = 2.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("选择引用来源", color = DSColors.inkGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("拍摄、选择视频，或引用已有内容", color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WorkspaceReplyToolCard(
                    title = if (coachPerspective) "拍摄视频" else "拍摄练习",
                    subtitle = "使用相机现场录制",
                    icon = Icons.Filled.CameraAlt,
                    modifier = Modifier.weight(1f),
                    onClick = onCamera,
                )
                WorkspaceReplyToolCard(
                    title = "从相册选择",
                    subtitle = "选择已有照片或视频",
                    icon = Icons.Filled.PhotoLibrary,
                    modifier = Modifier.weight(1f),
                    onClick = onGallery,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WorkspaceReplyToolCard(
                    title = if (coachPerspective) {
                        if (materialLimitReached) "素材已选满" else "引用教学素材"
                    } else {
                        "引用视频画面"
                    },
                    subtitle = if (coachPerspective) "从我的素材库选择" else "选择当前播放时间点",
                    icon = if (coachPerspective) Icons.AutoMirrored.Filled.LibraryBooks else Icons.Filled.Link,
                    modifier = Modifier.weight(1f),
                    enabled = !coachPerspective || !materialLimitReached,
                    onClick = onMaterial,
                )
                WorkspaceReplyToolCard(
                    title = if (sourceIsPractice) "引用练习记录" else "引用教学内容",
                    subtitle = if (sourceIsPractice) "引用本次练习" else "引用本次教学",
                    icon = Icons.Filled.Folder,
                    modifier = Modifier.weight(1f),
                    onClick = onReferenceSource,
                )
            }
            Box(
                Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(15.dp))
                    .background(DSColors.cardWhite).clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("取消", color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WorkspaceReplyToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier.height(72.dp).clip(RoundedCornerShape(15.dp))
            .background(DSColors.cardWhite)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp).then(if (enabled) Modifier else Modifier.background(Color.White.copy(alpha = 0.55f))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(35.dp).clip(CircleShape).background(DSColors.mistGreen.copy(alpha = 0.78f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (enabled) DSColors.fairway else DSColors.shadowGray, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title,
                color = if (enabled) DSColors.deepGreen else DSColors.shadowGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(subtitle, color = DSColors.shadowGray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun WorkspaceThreadContentCard(
    item: V2CoachWorkspaceItem,
    friendSpace: Boolean,
    onPlayAsset: (com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset) -> Unit,
) {
    var expanded by rememberSaveable(item.itemId) { mutableStateOf(false) }
    val primaryVideo = item.primaryAsset?.takeUnless { it.isVoice }
    val playableUrl = primaryVideo?.videoUrl ?: item.videoUrl
    val coverUrl = primaryVideo?.coverUrl ?: item.coverUrl
    val duration = primaryVideo?.durationSeconds ?: item.durationSeconds
    val tags = workspaceThreadSourceTags(item)
    val playWorkspaceVideo = LocalPlayWorkspaceVideo.current
    val preparingPlayback = LocalPreparingWorkspacePlaybackId.current == item.itemId
    var coverAspect by remember(item.itemId, coverUrl) {
        mutableFloatStateOf(workspaceThreadVideoAspectRatio(item))
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(DSColors.cardWhite)
            .border(0.8.dp, Color.Black.copy(alpha = 0.0275f), RoundedCornerShape(22.dp))
            .padding(16.dp).testTag("workspace-thread-content-card"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (item.isRemovedContent) {
            Column(
                Modifier.fillMaxWidth().height(132.dp).clip(RoundedCornerShape(17.dp))
                    .background(DSColors.mistGreen.copy(alpha = 0.42f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Delete, null, tint = DSColors.shadowGray, modifier = Modifier.size(24.dp))
                Text(
                    "原内容已删除",
                    color = DSColors.shadowGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else if (item.hasVideoContent) {
            Box(
                Modifier.fillMaxWidth()
                    .aspectRatio(coverAspect)
                    .clip(RoundedCornerShape(17.dp)).background(Color.Black.copy(alpha = 0.90f))
                    .clickable(enabled = !preparingPlayback) {
                        primaryVideo?.let(onPlayAsset)
                        playWorkspaceVideo(item.itemId, playableUrl)
                    }.testTag("workspace-thread-source-video"),
                contentAlignment = Alignment.Center,
            ) {
                coverUrl?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = "原内容视频封面",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        onSuccess = { state ->
                            val width = state.result.drawable.intrinsicWidth
                            val height = state.result.drawable.intrinsicHeight
                            if (width > 0 && height > 0) {
                                coverAspect = width.toFloat() / height.toFloat()
                            }
                        },
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))
                }
                Box(
                    Modifier.size(58.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.46f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (preparingPlayback) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            if (playableUrl.isNullOrBlank()) Icons.Filled.Refresh else Icons.Filled.PlayArrow,
                            contentDescription = if (playableUrl.isNullOrBlank()) "刷新视频" else "播放原视频",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                duration?.let {
                    Text(
                        formatWorkspaceDuration(it),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(9.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.58f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                workspaceThreadSourceTitle(item, friendSpace),
                color = DSColors.inkGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                workspaceThreadSourceMetadata(item, friendSpace),
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        if (!item.isRemovedContent && tags.isNotEmpty()) {
            Row(
                Modifier.height(29.dp).clip(RoundedCornerShape(15.dp))
                    .background(DSColors.mistGreen.copy(alpha = 0.72f)).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                tags.take(3).forEach { tag ->
                    Text(tag, color = DSColors.deepGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                if (tags.size > 3) {
                    Text("+${tags.size - 3}", color = DSColors.deepGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (!item.isRemovedContent) {
            item.coachingFocus?.trim()?.takeIf(String::isNotEmpty)?.let { focus ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 34.dp).clip(RoundedCornerShape(11.dp))
                        .background(DSColors.trophyGold.copy(alpha = 0.13f)).padding(horizontal = 11.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.PushPin, null, tint = DSColors.trophyGold, modifier = Modifier.size(12.dp))
                    Text("练习关注：$focus", color = DSColors.trophyGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        V2CoachWorkspaceItem.visibleCopy(item.text)?.let { note ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    note,
                    color = DSColors.inkGreen.copy(alpha = 0.86f),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.length > 48) {
                    Text(
                        if (expanded) "收起" else "展开",
                        color = DSColors.fairway,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { expanded = !expanded }.padding(vertical = 2.dp)
                            .testTag("workspace-thread-source-note-toggle"),
                    )
                }
            }
        }
    }
}

internal fun workspaceThreadVideoAspectRatio(item: V2CoachWorkspaceItem): Float {
    val width = item.primaryAsset?.width ?: item.width ?: 0
    val height = item.primaryAsset?.height ?: item.height ?: 0
    return if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 16f / 9f
}

internal fun workspaceThreadSourceTitle(item: V2CoachWorkspaceItem, friendSpace: Boolean): String {
    V2CoachWorkspaceItem.visibleCopy(item.title)?.let { return it }
    if (friendSpace) return if (item.resolvedKind == "student_practice") "练习记录" else "分享内容"
    return if (item.senderRole == "coach") "教练分享" else "复练视频"
}

internal fun workspaceThreadSourceMetadata(item: V2CoachWorkspaceItem, friendSpace: Boolean): String {
    val author = item.sender?.nickname?.trim()?.takeIf(String::isNotEmpty)
        ?: if (friendSpace) if (item.isMine) "我" else "好友"
        else if (item.senderRole == "coach") "教练" else "学员"
    return "记录者 $author · ${workspaceFeedDate(item.createdAt)}"
}

internal fun workspaceThreadSourceTags(item: V2CoachWorkspaceItem): List<String> =
    item.primaryAsset?.tags.orEmpty().map(String::trim).filter(String::isNotEmpty).distinct()

@Composable
private fun CoachFeedbackSourceSummary(source: V2CoachWorkspaceItem) {
    val sourceCover = source.primaryAsset?.coverUrl ?: source.coverUrl
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Color.White).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            V2UserAvatar(
                avatar = source.sender?.avatar,
                modifier = Modifier.size(44.dp),
                fallbackFontSize = 17.sp,
                placeholderIconSize = 25.dp,
                fallbackText = source.sender?.nickname ?: "挥呗学员",
                fallbackColor = DSColors.sandBeige,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    source.sender?.nickname?.takeIf(String::isNotBlank) ?: "挥呗学员",
                    color = DSColors.deepGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    coachStudentRelativeTime(source.createdAt).ifBlank { "刚刚" },
                    color = DSColors.shadowGray,
                    fontSize = 11.sp,
                )
            }
        }
        if (source.hasVideoContent) {
            Box(
                Modifier.fillMaxWidth().height(168.dp).clip(RoundedCornerShape(16.dp))
                    .background(DSColors.deepGreen),
                contentAlignment = Alignment.Center,
            ) {
                sourceCover?.let { cover ->
                    AsyncImage(
                        model = cover,
                        contentDescription = "学员练习视频封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.38f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("▶", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        V2CoachWorkspaceItem.visibleCopy(source.text)?.let { quote ->
            Text(
                quote,
                color = DSColors.deepGreen,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CoachFeedbackTool(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.height(64.dp).clip(RoundedCornerShape(14.dp))
            .background(if (enabled) Color.White else DSColors.borderLine.copy(alpha = 0.48f))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) DSColors.fairway else DSColors.shadowGray,
            modifier = Modifier.size(17.dp),
        )
        Text(
            title,
            color = if (enabled) DSColors.deepGreen else DSColors.shadowGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 1,
        )
    }
}

@Composable
internal fun WorkspaceAssetSaveResultDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "素材保存结果",
                color = DSColors.inkGreen,
                fontWeight = FontWeight.Bold,
            )
        },
        text = { Text(message, color = DSColors.shadowGray) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", color = DSColors.fairway, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DSColors.cardWhite,
        modifier = Modifier.testTag("workspace-asset-save-result"),
    )
}

@Composable
private fun WorkspaceReplyVideoSourceDialog(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择视频回复来源") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WorkspaceSourceOption(Icons.Filled.CameraAlt, "拍摄视频", onCamera)
                WorkspaceSourceOption(Icons.Filled.PhotoLibrary, "从相册选择", onGallery)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun WorkspaceSaveToLibraryToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("同时保存到私有素材", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("下次可直接复用，不会重复上传", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

internal fun threadDiscussionVideos(thread: V2CoachThread?): List<V2CoachDiscussionResource> {
    if (thread == null) return emptyList()
    val entries = linkedMapOf<String, V2CoachDiscussionResource>()
    thread.replies.filterNot(V2CoachWorkspaceItem::isRemovedContent).forEach { reply ->
        (listOfNotNull(reply.primaryAsset) + reply.attachments)
            .filter { asset -> !asset.isVoice && (asset.videoUrl != null || asset.coverUrl != null) }
            .forEach { asset ->
                val existing = entries[asset.assetId]
                entries[asset.assetId] = if (existing == null) {
                    V2CoachDiscussionResource(asset = asset, messageIds = listOf(reply.itemId))
                } else {
                    existing.copy(messageIds = (existing.messageIds + reply.itemId).distinct())
                }
            }
    }
    return entries.values.toList()
}

private enum class TeachingDetailPanel { DISCUSSION, MATERIALS }

private val teachingAutoTitles = setOf(
    "练习视频",
    "挥杆视频",
    "复练视频",
    "教练分享",
    "练习记录",
    "分享内容",
    "空间消息",
)

internal fun teachingThreadSummaryText(item: V2CoachWorkspaceItem): String? {
    V2CoachWorkspaceItem.visibleCopy(item.text)?.let { return it }
    val title = V2CoachWorkspaceItem.visibleCopy(item.title) ?: return null
    return title.takeUnless { it in teachingAutoTitles }
}

internal fun teachingThreadContentTags(item: V2CoachWorkspaceItem): List<String> {
    val tags = workspaceThreadSourceTags(item).toMutableList()
    item.coachingFocus?.trim()?.takeIf { it.isNotEmpty() && it !in tags }?.let(tags::add)
    return tags
}

@Composable
private fun TeachingThreadVideoHeader(
    item: V2CoachWorkspaceItem,
    height: androidx.compose.ui.unit.Dp,
    onPlayAsset: (com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset) -> Unit,
) {
    val playWorkspaceVideo = LocalPlayWorkspaceVideo.current
    val cover = item.primaryAsset?.coverUrl ?: item.coverUrl
    val playable = item.primaryAsset?.videoUrl ?: item.videoUrl
    val duration = item.primaryAsset?.durationSeconds ?: item.durationSeconds
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(ContentMediaMatte)
            .clickable {
                item.primaryAsset?.let(onPlayAsset)
                playWorkspaceVideo(item.itemId, playable)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (!cover.isNullOrBlank()) {
            AsyncImage(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        Icon(
            Icons.Filled.PlayArrow,
            "播放",
            tint = Color.White,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
        )
        duration?.takeIf { it.isFinite() && it > 0.0 }?.let {
            Text(
                teachingClock(it) ?: workspaceFeedDuration(it),
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                    .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun TeachingThreadSummary(
    item: V2CoachWorkspaceItem,
    authorName: String,
    statusLabel: String?,
    showMemo: Boolean,
    expanded: Boolean,
    onOpenFullText: () -> Unit,
    onOpenMemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = teachingThreadSummaryText(item)
    var overflow by remember(summary) { mutableStateOf(false) }
    val tags = teachingThreadContentTags(item)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        summary?.let { text ->
            Text(
                text,
                color = ContentText,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { overflow = it.hasVisualOverflow },
            )
            if (overflow && !expanded) {
                Row(
                    Modifier.clickable(onClick = onOpenFullText),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("全文", color = CoachGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = CoachGreen,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        if (tags.isNotEmpty()) {
            Text(
                tags.joinToString("  "),
                color = CoachGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE7F2EC))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "$authorName · ${teachingContentTime(item.createdAt)}",
                color = ContentSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            statusLabel?.let { label ->
                Text(
                    label,
                    color = if (label == "求点评") Color(0xFF8A6232) else CoachGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(CircleShape)
                        .background(if (label == "求点评") Color(0xFFF3E6D0) else Color.White)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (showMemo) {
                Text(
                    "查看 Memo",
                    color = CoachGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onOpenMemo),
                )
            }
        }
    }
}

@Composable
private fun TeachingDiscussionEntry(
    preview: String?,
    onOpenDiscussion: () -> Unit,
    onOpenMaterials: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clickable(onClick = onOpenDiscussion),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("交流", color = CoachInk, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "展开交流",
                    tint = CoachMeta,
                    modifier = Modifier.padding(start = 2.dp).size(18.dp),
                )
            }
            Text(
                "资料",
                color = CoachGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onOpenMaterials).padding(vertical = 4.dp),
            )
        }
        preview?.let { text ->
            Text(
                text,
                color = CoachBody,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDiscussion),
            )
        }
    }
}

@Composable
private fun TeachingDetailPanelBody(
    panel: TeachingDetailPanel,
    onSelect: (TeachingDetailPanel) -> Unit,
    authorName: String,
    originalText: String?,
    replies: List<V2CoachWorkspaceItem>,
    loading: Boolean,
    syncError: String?,
    loadingOlder: Boolean,
    hasOlder: Boolean,
    onLoadOlder: () -> Unit,
    onRetrySync: () -> Unit,
    friendSpace: Boolean,
    coachPerspective: Boolean,
    conversationReadOnly: Boolean,
    displayedSource: V2CoachWorkspaceItem,
    allMessages: List<V2CoachWorkspaceItem>,
    savingAssetIds: Set<String>,
    scrollToReplyItemId: String?,
    onConsumeScrollTarget: () -> Unit,
    onQuote: (String) -> Unit,
    onDelete: (V2CoachWorkspaceItem) -> Unit,
    onSaveAsset: ((com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset) -> Unit)?,
    onPlayAsset: (com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset) -> Unit,
    resources: List<V2CoachDiscussionResource>,
    canAddResource: Boolean,
    onAddResource: () -> Unit,
    onOpenResourceDiscussion: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val voicePlayer = remember { TeachingVoicePlayer() }
    DisposableEffect(voicePlayer) {
        onDispose { voicePlayer.release() }
    }
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().height(44.dp).padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeachingPanelTab(
                title = "交流",
                selected = panel == TeachingDetailPanel.DISCUSSION,
                onClick = { onSelect(TeachingDetailPanel.DISCUSSION) },
            )
            Spacer(Modifier.width(12.dp))
            TeachingPanelTab(
                title = "资料",
                selected = panel == TeachingDetailPanel.MATERIALS,
                onClick = { onSelect(TeachingDetailPanel.MATERIALS) },
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(ContentSectionLine))
        when (panel) {
            TeachingDetailPanel.DISCUSSION -> Column(
                Modifier.fillMaxWidth().padding(start = 12.dp, top = 2.dp, bottom = 4.dp),
            ) {
                if (hasOlder) {
                    WorkspaceLoadOlderRepliesButton(loading = loadingOlder, onClick = onLoadOlder)
                }
                if (loading) {
                    WorkspaceThreadDiscussionSyncState(errorMessage = syncError, onRetry = onRetrySync)
                } else {
                    if (!originalText.isNullOrBlank()) {
                        TeachingContentMessage(
                            item = displayedSource,
                            senderName = authorName,
                            origin = true,
                            voicePlayer = voicePlayer,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                        if (replies.isNotEmpty()) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(ContentSectionLine))
                        }
                    }
                    if (replies.isNotEmpty()) {
                        val unreadBoundary = workspaceFirstUnreadReplyIndex(
                            source = displayedSource,
                            replies = replies,
                            coachPerspective = coachPerspective,
                            friendSpace = friendSpace,
                        )
                        val unreadCount = unreadBoundary?.let { boundary ->
                            workspaceUnreadReplyCount(
                                replies = replies,
                                boundary = boundary,
                                coachPerspective = coachPerspective,
                                friendSpace = friendSpace,
                            )
                        } ?: 0
                        replies.forEachIndexed { index, reply ->
                            if (unreadBoundary == index) {
                                WorkspaceNewReplyDivider(
                                    count = unreadCount,
                                    coachPerspective = coachPerspective,
                                    friendSpace = friendSpace,
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                            val isMine = workspaceReplyIsMine(reply, coachPerspective, friendSpace)
                            val referenced = allMessages.firstOrNull { it.itemId == reply.replyToItemId }
                            val senderName = reply.sender?.nickname?.trim()?.takeIf(String::isNotEmpty)
                                ?: if (isMine) "我" else if (reply.senderRole == "coach") "教练" else "学员"
                            TeachingContentMessage(
                                item = reply,
                                senderName = senderName,
                                voicePlayer = voicePlayer,
                                referencedItem = referenced,
                                savingAssetIds = savingAssetIds,
                                onSaveAsset = onSaveAsset,
                                onPlayAsset = onPlayAsset,
                                onDelete = if (reply.isMine && reply.deletedAt == null) {
                                    { onDelete(reply) }
                                } else null,
                                onQuote = if (!conversationReadOnly && !isMine && reply.deletedAt == null) {
                                    { onQuote(reply.itemId) }
                                } else null,
                                onOpenReference = referenced?.let { target ->
                                    {
                                        if (target.parentItemId == null) {
                                            onSelect(TeachingDetailPanel.DISCUSSION)
                                        }
                                        onOpenResourceDiscussion(target.itemId)
                                    }
                                },
                                bringIntoView = scrollToReplyItemId == reply.itemId,
                                onBroughtIntoView = {
                                    if (scrollToReplyItemId == reply.itemId) onConsumeScrollTarget()
                                },
                                modifier = Modifier.padding(vertical = 10.dp),
                            )
                            if (index < replies.lastIndex) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(ContentSectionLine))
                            }
                        }
                    }
                    syncError?.let { Text(it, color = DSColors.flagRed, fontSize = 11.sp) }
                }
            }
            TeachingDetailPanel.MATERIALS -> Column(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("相关资料", color = CoachInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (resources.isEmpty()) {
                    Text("还没有资料", color = CoachMeta, fontSize = 13.sp)
                } else {
                    resources.forEach { resource ->
                        TeachingMaterialRow(
                            resource = resource,
                            onOpenDiscussion = onOpenResourceDiscussion,
                            onPlayAsset = onPlayAsset,
                        )
                    }
                }
                if (canAddResource) {
                    Row(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE7F2EC))
                            .border(1.dp, CoachGreen, RoundedCornerShape(10.dp))
                            .clickable(onClick = onAddResource)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = CoachGreen, modifier = Modifier.size(16.dp))
                        Text("添加资料", color = CoachGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TeachingPanelTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier.width(64.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            title,
            color = if (selected) CoachGreen else ContentSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Box(
            Modifier.padding(top = 4.dp).size(width = 40.dp, height = 3.dp)
                .background(if (selected) CoachGreen else Color.Transparent),
        )
    }
}

@Composable
private fun TeachingMaterialRow(
    resource: V2CoachDiscussionResource,
    onOpenDiscussion: (String?) -> Unit,
    onPlayAsset: (com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset) -> Unit,
) {
    val asset = resource.asset
    val playWorkspaceVideo = LocalPlayWorkspaceVideo.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(width = 72.dp, height = 48.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE7EFE8))
                .clickable {
                    onPlayAsset(asset)
                    playWorkspaceVideo(asset.assetId, asset.videoUrl)
                },
        ) {
            asset.coverUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
            Icon(Icons.Filled.PlayArrow, "播放", tint = Color.White, modifier = Modifier.align(Alignment.Center).size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                asset.title.ifBlank { "相关资料" },
                color = CoachInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "查看交流位置",
                color = CoachGreen,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onOpenDiscussion(resource.messageIds.firstOrNull()) },
            )
        }
    }
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun TeachingLinkedMemoOverlay(
    memo: V2Memo,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.White)) {
        if (memo.hasVideo) {
            V2VideoMemoPlayerScreen(memoId = memo.id, onBack = onBack)
        } else {
            V2MemoDetailScreen(memoId = memo.id, onBack = onBack)
        }
    }
}

@Composable
private fun TeachingThreadMaterialCard(
    resource: V2CoachDiscussionResource,
    onOpenDiscussion: (String?) -> Unit,
    onPlayAsset: (com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset) -> Unit,
) {
    val asset = resource.asset
    val playWorkspaceVideo = LocalPlayWorkspaceVideo.current
    Column(Modifier.width(132.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(10.dp)).background(DSColors.mistGreen)
                .clickable {
                    onPlayAsset(asset)
                    playWorkspaceVideo(asset.assetId, asset.videoUrl)
                },
        ) {
            asset.coverUrl?.let {
                AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Icon(Icons.Filled.PlayArrow, "播放", tint = Color.White, modifier = Modifier.align(Alignment.Center).size(22.dp))
        }
        Text(
            asset.title.ifBlank { "相关资料" },
            color = DSColors.inkGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "查看交流位置",
            color = CoachGreen,
            fontSize = 11.sp,
            modifier = Modifier.clickable { onOpenDiscussion(resource.messageIds.firstOrNull()) },
        )
    }
}

@Composable
private fun CoachThreadPracticeBar(
    item: V2CoachWorkspaceItem,
    onOpenList: () -> Unit,
    onPlayAsset: (com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset) -> Unit,
) {
    val playWorkspaceVideo = LocalPlayWorkspaceVideo.current
    val cover = item.primaryAsset?.coverUrl ?: item.coverUrl
    val playable = item.primaryAsset?.videoUrl ?: item.videoUrl
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(width = 86.dp, height = 56.dp).clip(RoundedCornerShape(10.dp)).background(DSColors.mistGreen)
                .clickable {
                    item.primaryAsset?.let(onPlayAsset)
                    playWorkspaceVideo(item.itemId, playable)
                },
            contentAlignment = Alignment.Center,
        ) {
            if (!cover.isNullOrBlank()) {
                AsyncImage(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Icon(Icons.Filled.PlayArrow, "播放", tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                workspaceThreadSourceTitle(item, friendSpace = false),
                color = DSColors.inkGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(workspaceFeedDate(item.createdAt), color = DSColors.shadowGray, fontSize = 11.sp)
        }
        Text(
            "练习列表",
            color = CoachGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(CircleShape).background(CoachPaper).clickable(onClick = onOpenList)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun CoachRelatedVideoRow(
    resource: V2CoachDiscussionResource,
    onOpenDiscussion: (String?) -> Unit,
    onPlayAsset: (com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset) -> Unit,
) {
    val asset = resource.asset
    val playWorkspaceVideo = LocalPlayWorkspaceVideo.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(width = 86.dp, height = 56.dp).clip(RoundedCornerShape(10.dp)).background(DSColors.mistGreen)
                .clickable {
                    onPlayAsset(asset)
                    playWorkspaceVideo(asset.assetId, asset.videoUrl)
                },
        ) {
            asset.coverUrl?.let {
                AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                asset.title.ifBlank { "相关视频" },
                color = DSColors.inkGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "查看交流位置",
                color = CoachGreen,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onOpenDiscussion(resource.messageIds.firstOrNull()) },
            )
        }
    }
}

@Composable
internal fun ThreadResourceList(
    resources: List<V2CoachDiscussionResource>,
    loading: Boolean,
    onPlayAsset: (com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset) -> Unit,
    onBackToDiscussion: (String?) -> Unit,
) {
    if (resources.isEmpty()) {
        if (loading) {
            Row(
                Modifier.fillMaxWidth().padding(top = 30.dp).clip(RoundedCornerShape(18.dp))
                    .background(DSColors.cardWhite)
                    .border(0.8.dp, Color.Black.copy(alpha = 0.0275f), RoundedCornerShape(18.dp))
                    .padding(18.dp).testTag("workspace-thread-resources-loading"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(Modifier.size(22.dp), color = DSColors.fairway, strokeWidth = 2.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("正在整理交流资料", color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("引用素材和主动保存的视频会出现在这里", color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            Column(
                Modifier.fillMaxWidth().padding(top = 30.dp).clip(RoundedCornerShape(22.dp))
                    .background(DSColors.cardWhite)
                    .border(0.8.dp, Color.Black.copy(alpha = 0.0275f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 20.dp, vertical = 28.dp)
                    .testTag("workspace-thread-resources-empty"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(62.dp).clip(CircleShape).background(DSColors.mistGreen.copy(alpha = 0.78f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, tint = DSColors.fairway, modifier = Modifier.size(24.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("把参考放在一起", color = DSColors.inkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "这里只汇总本条讨论引用过的素材，不会列出整个个人库。",
                        color = DSColors.shadowGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Row(
                    Modifier.height(38.dp).clip(RoundedCornerShape(19.dp))
                        .background(DSColors.mistGreen.copy(alpha = 0.76f))
                        .clickable { onBackToDiscussion(null) }.padding(horizontal = 16.dp)
                        .testTag("workspace-thread-resources-empty-action"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Add, null, tint = DSColors.fairway, modifier = Modifier.size(15.dp))
                    Text("引用资料", color = DSColors.fairway, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }
    Column(
        Modifier.fillMaxWidth().testTag("workspace-thread-resource-grid"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        resources.chunked(2).forEach { rowResources ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowResources.forEach { resource ->
                    WorkspaceThreadResourceCard(
                        resource = resource,
                        onPlayAsset = onPlayAsset,
                        onBackToDiscussion = onBackToDiscussion,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowResources.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WorkspaceThreadResourceCard(
    resource: V2CoachDiscussionResource,
    onPlayAsset: (com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset) -> Unit,
    onBackToDiscussion: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val asset = resource.asset
    val playWorkspaceVideo = LocalPlayWorkspaceVideo.current
    val preparingPlayback = LocalPreparingWorkspacePlaybackId.current == asset.assetId
    var menuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier.shadow(8.dp, RoundedCornerShape(17.dp), ambientColor = DSColors.deepGreen.copy(alpha = 0.045f))
            .clip(RoundedCornerShape(17.dp)).background(DSColors.cardWhite)
            .border(1.dp, DSColors.fairway.copy(alpha = 0.28f), RoundedCornerShape(17.dp))
            .padding(10.dp).testTag("workspace-thread-resource-${asset.assetId}"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp))
                .background(DSColors.mistGreen)
                .clickable(enabled = !preparingPlayback) {
                    onPlayAsset(asset)
                    playWorkspaceVideo(asset.assetId, asset.videoUrl)
                },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = asset.coverUrl,
                contentDescription = "交流资料封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.46f)),
                contentAlignment = Alignment.Center,
            ) {
                if (preparingPlayback) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(
                        if (asset.videoUrl.isNullOrBlank()) Icons.Filled.Refresh else Icons.Filled.PlayArrow,
                        if (asset.videoUrl.isNullOrBlank()) "刷新视频" else "播放资料",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            asset.durationSeconds?.let { duration ->
                Text(
                    formatWorkspaceDuration(duration),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp)
                        .height(22.dp).clip(RoundedCornerShape(11.dp))
                        .background(Color.Black.copy(alpha = 0.58f)).padding(horizontal = 7.dp, vertical = 5.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    workspaceThreadResourceTitle(asset),
                    color = DSColors.inkGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    workspaceThreadResourceSourceLabel(asset),
                    color = DSColors.fairway,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Box {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "资料操作",
                    tint = DSColors.shadowGray,
                    modifier = Modifier.size(24.dp).clickable { menuExpanded = true }.padding(4.dp)
                        .testTag("workspace-thread-resource-menu-${asset.assetId}"),
                )
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    resource.messageIds.forEachIndexed { index, messageId ->
                        DropdownMenuItem(
                            text = {
                                Text(if (resource.messageIds.size > 1) "回到第 ${index + 1} 次引用" else "回到交流")
                            },
                            leadingIcon = { Icon(Icons.Filled.ChatBubble, null, modifier = Modifier.size(17.dp)) },
                            onClick = {
                                menuExpanded = false
                                onBackToDiscussion(messageId)
                            },
                        )
                    }
                }
            }
        }
    }
}

internal fun workspaceThreadResourceSourceLabel(
    asset: com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset,
): String = when (asset.sourceKind.lowercase()) {
    "analysis_recording" -> "视频讲解"
    "coach_recording" -> "交流视频"
    else -> "引用素材"
}

internal fun workspaceThreadResourceTitle(
    asset: com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset,
): String {
    asset.title.trim().takeIf(String::isNotEmpty)?.let { return it }
    val date = asset.createdAt?.let { raw ->
        runCatching { java.time.OffsetDateTime.parse(raw).toInstant() }
            .recoverCatching { java.time.Instant.parse(raw) }.getOrNull()
    } ?: return "交流视频"
    return date.atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("M月d日'视频'", java.util.Locale.CHINA))
}

private fun formatWorkspaceDuration(seconds: Double): String {
    val total = seconds.takeIf { it.isFinite() && it >= 0 }?.toInt() ?: 0
    return "%02d:%02d".format(total / 60, total % 60)
}

@Composable
internal fun ThreadMessage(
    item: V2CoachWorkspaceItem,
    referencedItem: V2CoachWorkspaceItem? = null,
    isMine: Boolean = false,
    friendSpace: Boolean = false,
    showsIdentity: Boolean = true,
    deliveryStatusText: String? = null,
    savingAssetIds: Set<String> = emptySet(),
    onSaveAsset: ((com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset) -> Unit)? = null,
    onPlayAsset: ((com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset) -> Unit)? = null,
    onQuote: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onOpenReference: (() -> Unit)? = null,
    bringIntoView: Boolean = false,
    onBroughtIntoView: () -> Unit = {},
) {
    val context = LocalContext.current
    val playWorkspaceVideo = LocalPlayWorkspaceVideo.current
    val preparingPlaybackId = LocalPreparingWorkspacePlaybackId.current
    val primaryVideo = item.primaryAsset?.takeUnless { it.isVoice }
    val playableUrl = primaryVideo?.videoUrl ?: item.videoUrl
    var expanded by rememberSaveable(item.itemId) { mutableStateOf(false) }
    var menuExpanded by remember(item.itemId) { mutableStateOf(false) }
    val bringIntoViewRequester = remember(item.itemId) { BringIntoViewRequester() }
    LaunchedEffect(bringIntoView) {
        if (bringIntoView) {
            delay(50)
            bringIntoViewRequester.bringIntoView()
            onBroughtIntoView()
        }
    }
    val visibleText = V2CoachWorkspaceItem.visibleCopy(item.text)
        ?: V2CoachWorkspaceItem.visibleCopy(item.title).orEmpty()
    val senderName = item.sender?.nickname?.trim()?.takeIf(String::isNotEmpty)
        ?: if (friendSpace) if (isMine) "我" else "好友"
        else if (item.senderRole == "coach") "教练" else "学员"

    Row(
        Modifier.fillMaxWidth().bringIntoViewRequester(bringIntoViewRequester)
            .testTag("workspace-thread-message-${item.itemId}"),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isMine) Spacer(Modifier.weight(1f).widthIn(min = 44.dp))
        if (!isMine) {
            WorkspaceMessageAvatar(item = item, name = senderName, visible = showsIdentity)
        }

        Column(
            Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (showsIdentity) {
                Text(
                    "$senderName  ${coachStudentRelativeTime(item.createdAt).ifBlank { "刚刚" }}",
                    color = DSColors.shadowGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Box {
                Column(
                    Modifier.clip(RoundedCornerShape(17.dp))
                        .background(if (isMine) DSColors.deepGreen else DSColors.cardWhite)
                        .then(
                            if (isMine) Modifier else Modifier.border(
                                0.8.dp,
                                DSColors.listCardBorder.copy(alpha = 0.8f),
                                RoundedCornerShape(17.dp),
                            ),
                        )
                        .pointerInput(item.itemId) {
                            detectTapGestures(onLongPress = { menuExpanded = true })
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("workspace-thread-message-bubble-${item.itemId}"),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    if (item.isRemovedContent) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Delete, null, tint = DSColors.shadowGray, modifier = Modifier.size(15.dp))
                            Text("内容已删除", color = DSColors.shadowGray, fontSize = 14.sp)
                        }
                    } else {
                        if (item.replyToItemId != null) {
                            WorkspaceReplyReference(
                                item = item,
                                referencedItem = referencedItem,
                                isMine = isMine,
                                onOpen = onOpenReference,
                            )
                        }
                        if (visibleText.isNotEmpty()) {
                            Text(
                                visibleText,
                                color = if (isMine) Color.White else DSColors.inkGreen,
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                maxLines = if (expanded) Int.MAX_VALUE else 8,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (visibleText.length > 220) {
                                Text(
                                    if (expanded) "收起" else "展开",
                                    color = if (isMine) Color.White.copy(alpha = 0.78f) else DSColors.fairway,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { expanded = !expanded },
                                )
                            }
                        }

                        item.primaryAsset?.takeIf { it.isVoice }?.let { voice ->
                            WorkspaceVoiceMessage(voice = voice, isMine = isMine)
                        }
                        if (primaryVideo != null) {
                            if (workspaceAssetUsesCompactPresentation(primaryVideo)) {
                                WorkspaceDiscussionCompactVideoCard(
                                    asset = primaryVideo,
                                    sourceLabel = if (primaryVideo.sourceKind == "album") "交流视频"
                                    else if (friendSpace) "参考素材" else "教学素材",
                                    fallbackTitle = if (primaryVideo.sourceKind == "album") "相册视频"
                                    else if (friendSpace) "参考素材" else "教学素材",
                                    onPlayAsset = onPlayAsset,
                                )
                            } else {
                                WorkspaceDiscussionDirectVideoCard(
                                    coverUrl = primaryVideo.coverUrl ?: item.coverUrl,
                                    playableUrl = playableUrl,
                                    duration = primaryVideo.durationSeconds ?: item.durationSeconds,
                                    preparing = preparingPlaybackId == item.itemId,
                                    onPlay = {
                                        onPlayAsset?.invoke(primaryVideo)
                                        playWorkspaceVideo(item.itemId, playableUrl)
                                    },
                                )
                            }
                        }                         else if (item.hasVideoContent && !teachingItemIsAudioOnly(item)) {
                            WorkspaceDiscussionDirectVideoCard(
                                coverUrl = item.coverUrl,
                                playableUrl = item.videoUrl,
                                duration = item.durationSeconds,
                                preparing = preparingPlaybackId == item.itemId,
                                onPlay = {
                                    playWorkspaceVideo(item.itemId, item.videoUrl)
                                },
                            )
                        }
                        item.attachments.filterNot { it.isVoice }.forEach { asset ->
                            WorkspaceDiscussionCompactVideoCard(
                                asset = asset,
                                sourceLabel = if (friendSpace) "参考素材" else "教学素材",
                                fallbackTitle = if (friendSpace) "参考素材" else "教学素材",
                                onPlayAsset = onPlayAsset,
                            )
                        }
                    }
                }

                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    onQuote?.let { quote ->
                        DropdownMenuItem(
                            text = { Text("回复") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(17.dp)) },
                            onClick = { menuExpanded = false; quote() },
                        )
                    }
                    if (visibleText.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("复制") },
                            leadingIcon = { Icon(Icons.Filled.Description, null, modifier = Modifier.size(17.dp)) },
                            onClick = {
                                menuExpanded = false
                                context.copyPlainTextToClipboard("交流内容", visibleText)
                            },
                        )
                    }
                    if (primaryVideo != null && onSaveAsset != null && !primaryVideo.savedToLibrary &&
                        primaryVideo.sourceKind.lowercase() == "album"
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (primaryVideo.assetId in savingAssetIds) "保存中…" else "保存到素材") },
                            leadingIcon = { Icon(Icons.Filled.Folder, null, modifier = Modifier.size(17.dp)) },
                            enabled = primaryVideo.assetId !in savingAssetIds,
                            onClick = { menuExpanded = false; onSaveAsset(primaryVideo) },
                        )
                    }
                    onDelete?.let { delete ->
                        DropdownMenuItem(
                            text = { Text("删除", color = DSColors.flagRed) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = DSColors.flagRed, modifier = Modifier.size(17.dp)) },
                            onClick = { menuExpanded = false; delete() },
                        )
                    }
                }
            }

            deliveryStatusText?.let {
                Text(
                    it,
                    color = DSColors.shadowGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }

        if (isMine) {
            WorkspaceMessageAvatar(item = item, name = senderName, visible = showsIdentity)
        }
        if (!isMine) Spacer(Modifier.weight(1f).widthIn(min = 44.dp))
    }
}

@Composable
private fun WorkspaceMessageAvatar(item: V2CoachWorkspaceItem, name: String, visible: Boolean) {
    if (!visible) {
        Spacer(Modifier.size(width = 30.dp, height = 1.dp))
        return
    }
    val ring = if (item.senderRole == "coach") DSColors.fairway else DSColors.trophyGold
    Box(
        Modifier.size(30.dp).border(1.dp, ring, CircleShape).padding(1.dp),
        contentAlignment = Alignment.Center,
    ) {
        V2UserAvatar(
            avatar = item.sender?.avatar,
            modifier = Modifier.fillMaxSize(),
            fallbackText = name,
            fallbackFontSize = 10.sp,
            placeholderIconSize = 18.dp,
        )
    }
}

@Composable
private fun WorkspaceReplyReference(
    item: V2CoachWorkspaceItem,
    referencedItem: V2CoachWorkspaceItem?,
    isMine: Boolean,
    onOpen: (() -> Unit)?,
) {
    val title = workspaceReplyReferenceTitle(item, referencedItem)
    val preview = workspaceReplyReferencePreview(referencedItem)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
            .background(if (isMine) Color.White.copy(alpha = 0.10f) else DSColors.mistGreen.copy(alpha = 0.55f))
            .clickable(enabled = onOpen != null) { onOpen?.invoke() }.padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier.size(width = 3.dp, height = 30.dp).clip(RoundedCornerShape(2.dp))
                .background(if (isMine) Color.White.copy(alpha = 0.65f) else DSColors.trophyGold),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = if (isMine) Color.White.copy(alpha = 0.78f) else DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                preview,
                color = if (isMine) Color.White.copy(alpha = 0.78f) else DSColors.fairway,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = if (isMine) Color.White.copy(alpha = 0.78f) else DSColors.fairway,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun WorkspaceVoiceMessage(
    voice: com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset,
    isMine: Boolean,
) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clickable(enabled = !voice.videoUrl.isNullOrBlank()) {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, voice.videoUrl!!.toUri())) }
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(if (isMine) Color.White else DSColors.fairway),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                "播放语音",
                tint = if (isMine) DSColors.deepGreen else Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Box(
            Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp))
                .background(if (isMine) Color.White.copy(alpha = 0.55f) else DSColors.mistGreen),
        )
        Text(
            voice.durationSeconds?.let(::formatWorkspaceDuration) ?: "语音消息",
            color = if (isMine) Color.White.copy(alpha = 0.80f) else DSColors.shadowGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun WorkspaceDiscussionDirectVideoCard(
    coverUrl: String?,
    playableUrl: String?,
    duration: Double?,
    preparing: Boolean = false,
    onPlay: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.90f)).clickable(enabled = !preparing, onClick = onPlay),
        contentAlignment = Alignment.Center,
    ) {
        coverUrl?.let {
            AsyncImage(it, "视频回复封面", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))
        }
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.46f)), contentAlignment = Alignment.Center) {
            if (preparing) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(
                    if (playableUrl.isNullOrBlank()) Icons.Filled.Refresh else Icons.Filled.PlayArrow,
                    if (playableUrl.isNullOrBlank()) "刷新视频" else "播放视频",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        duration?.let {
            Text(
                formatWorkspaceDuration(it),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp).height(22.dp)
                    .clip(RoundedCornerShape(11.dp)).background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 7.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun WorkspaceDiscussionCompactVideoCard(
    asset: com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset,
    sourceLabel: String,
    fallbackTitle: String,
    onPlayAsset: ((com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset) -> Unit)?,
) {
    val playWorkspaceVideo = LocalPlayWorkspaceVideo.current
    val preparingPlayback = LocalPreparingWorkspacePlaybackId.current == asset.assetId
    Column(
        Modifier.width(204.dp).clip(RoundedCornerShape(12.dp)).background(DSColors.cardWhite)
            .border(0.8.dp, Color.Black.copy(alpha = 0.035f), RoundedCornerShape(12.dp))
            .clickable(enabled = !preparingPlayback) {
                onPlayAsset?.invoke(asset)
                playWorkspaceVideo(asset.assetId, asset.videoUrl)
            },
    ) {
        Box(
            Modifier.fillMaxWidth().height(115.dp).background(DSColors.mistGreen),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(asset.coverUrl, sourceLabel, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.08f)))
            Box(Modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.48f)), contentAlignment = Alignment.Center) {
                if (preparingPlayback) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 1.5.dp)
                } else {
                    Icon(
                        if (asset.videoUrl.isNullOrBlank()) Icons.Filled.Refresh else Icons.Filled.PlayArrow,
                        if (asset.videoUrl.isNullOrBlank()) "刷新视频" else "播放视频",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
            asset.durationSeconds?.let {
                Text(
                    formatWorkspaceDuration(it),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).height(19.dp)
                        .clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.58f))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    asset.title.trim().ifEmpty { fallbackTitle },
                    color = DSColors.inkGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(sourceLabel, color = DSColors.shadowGray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = DSColors.fairway, modifier = Modifier.size(13.dp))
        }
    }
}

internal fun workspaceAssetUsesCompactPresentation(
    asset: com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset,
): Boolean {
    val kind = asset.sourceKind.lowercase()
    val referenced = asset.savedToLibrary && kind !in setOf("analysis_recording", "coach_recording", "voice_recording")
    return referenced || (kind == "album" && !asset.savedToLibrary)
}

internal fun workspaceReplyReferenceTitle(
    item: V2CoachWorkspaceItem,
    referencedItem: V2CoachWorkspaceItem?,
): String {
    if (referencedItem == null) return "引用内容已失效"
    val base = when {
        referencedItem.parentItemId == null && referencedItem.hasVideoContent -> "原视频"
        !referencedItem.title.isNullOrBlank() -> referencedItem.title.trim()
        referencedItem.hasVideoContent -> "交流视频"
        else -> "引用消息"
    }
    return item.replyAtSeconds?.let { "$base · ${formatWorkspaceDuration(it)}" } ?: base
}

internal fun workspaceReplyReferencePreview(referencedItem: V2CoachWorkspaceItem?): String {
    if (referencedItem == null) return "原内容已不可用"
    V2CoachWorkspaceItem.visibleCopy(referencedItem.text)?.let { return it }
    return if (referencedItem.hasVideoContent) "点击查看对应视频" else "查看被回复的消息"
}

internal fun workspaceMessageStartsGroup(
    index: Int,
    replies: List<V2CoachWorkspaceItem>,
    friendSpace: Boolean,
): Boolean {
    if (index !in replies.indices || index == 0) return true
    val current = replies[index]
    val previous = replies[index - 1]
    val sameSender = if (friendSpace) current.isMine == previous.isMine else current.senderRole == previous.senderRole
    if (!sameSender) return true
    val currentAt = workspaceThreadInstant(current.createdAt) ?: return false
    val previousAt = workspaceThreadInstant(previous.createdAt) ?: return false
    return java.time.Duration.between(previousAt, currentAt).seconds >= 5 * 60
}

internal fun workspaceShouldShowReplyTimeDivider(
    root: V2CoachWorkspaceItem,
    replies: List<V2CoachWorkspaceItem>,
    index: Int,
): Boolean {
    if (index !in replies.indices) return false
    val current = workspaceThreadInstant(replies[index].createdAt) ?: return false
    val previous = workspaceThreadInstant(if (index == 0) root.createdAt else replies[index - 1].createdAt) ?: return true
    val zone = java.time.ZoneId.systemDefault()
    return java.time.Duration.between(previous, current).seconds >= 30 * 60 ||
        previous.atZone(zone).toLocalDate() != current.atZone(zone).toLocalDate()
}

internal fun workspaceConversationDateText(
    raw: String?,
    now: java.time.Instant = java.time.Instant.now(),
    zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
): String {
    val value = workspaceThreadInstant(raw) ?: return ""
    val date = value.atZone(zone)
    val current = now.atZone(zone)
    val pattern = when {
        date.toLocalDate() == current.toLocalDate() -> "'今天' HH:mm"
        date.toLocalDate() == current.toLocalDate().minusDays(1) -> "'昨天' HH:mm"
        date.year == current.year -> "M月d日 HH:mm"
        else -> "yyyy年M月d日 HH:mm"
    }
    return date.format(java.time.format.DateTimeFormatter.ofPattern(pattern, java.util.Locale.CHINA))
}

internal fun workspaceReplyDeliveryStatus(
    root: V2CoachWorkspaceItem,
    reply: V2CoachWorkspaceItem,
    index: Int,
    replies: List<V2CoachWorkspaceItem>,
    coachPerspective: Boolean,
    friendSpace: Boolean,
): String? {
    if (!reply.isMine || index != replies.lastIndex || reply.isRemovedContent) return null
    if (friendSpace) return "已发送"
    if (coachPerspective) {
        if (reply.senderRole != "coach") return null
        return if (root.studentHasUnread) "已送达"
        else "学员已查看${workspaceReceiptTimeSuffix(root.studentViewedAt)}"
    }
    if (reply.senderRole == "coach") return null
    return when (root.reviewState) {
        "new", null -> "已送达"
        "viewed" -> "教练已查看${workspaceReceiptTimeSuffix(root.coachViewedAt)}"
        "responded" -> "教练已回复"
        else -> null
    }
}

internal fun workspaceFirstUnreadReplyIndex(
    source: V2CoachWorkspaceItem,
    replies: List<V2CoachWorkspaceItem>,
    coachPerspective: Boolean,
    friendSpace: Boolean,
): Int? {
    val openedWithUnread = if (coachPerspective) {
        source.reviewState == "new" && (source.senderRole == "student" || source.latestReplySenderRole == "student")
    } else source.studentHasUnread
    if (!openedWithUnread) return null
    val cursor = if (coachPerspective) source.coachLastViewedReplyId else source.studentLastViewedReplyId
    val start = when {
        cursor == source.itemId -> 0
        cursor != null -> replies.indexOfFirst { it.itemId == cursor }.takeIf { it >= 0 }?.plus(1) ?: 0
        else -> 0
    }
    val incomingRole = if (coachPerspective) "student" else "coach"
    return replies.indices.drop(start).firstOrNull { index ->
        !replies[index].isRemovedContent && if (friendSpace) !replies[index].isMine else replies[index].senderRole == incomingRole
    }
}

internal fun workspaceUnreadReplyCount(
    replies: List<V2CoachWorkspaceItem>,
    boundary: Int,
    coachPerspective: Boolean,
    friendSpace: Boolean,
): Int {
    val incomingRole = if (coachPerspective) "student" else "coach"
    return replies.indices.drop(boundary).count { index ->
        !replies[index].isRemovedContent && if (friendSpace) !replies[index].isMine else replies[index].senderRole == incomingRole
    }
}

internal fun workspaceIsNearThreadBottom(value: Int, maxValue: Int, thresholdPx: Int): Boolean =
    maxValue - value <= thresholdPx.coerceAtLeast(0)

internal fun workspaceReplyIsMine(
    reply: V2CoachWorkspaceItem,
    coachPerspective: Boolean,
    friendSpace: Boolean,
): Boolean = if (friendSpace) {
    reply.isMine
} else {
    (coachPerspective && reply.senderRole == "coach") ||
        (!coachPerspective && reply.senderRole != "coach")
}

internal fun workspaceThreadInstant(raw: String?): java.time.Instant? = raw?.let { value ->
    runCatching { java.time.OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { java.time.Instant.parse(value) }.getOrNull()
}

private fun workspaceReceiptTimeSuffix(raw: String?): String {
    val value = workspaceThreadInstant(raw) ?: return ""
    val zone = java.time.ZoneId.systemDefault()
    val date = value.atZone(zone)
    val now = java.time.ZonedDateTime.now(zone)
    val text = when {
        date.toLocalDate() == now.toLocalDate() -> date.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        date.toLocalDate() == now.toLocalDate().minusDays(1) -> "昨天 ${date.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}"
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("M/d HH:mm"))
    }
    return " · $text"
}

@Composable
internal fun WorkspaceDiscussionEmptyState(videoContext: Boolean = true) {
    Column(
        Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp).testTag("workspace-thread-discussion-empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (videoContext) {
            Image(
                painter = painterResource(R.drawable.teaching_discussion_empty),
                contentDescription = null,
                modifier = Modifier.width(110.dp).aspectRatio(120f / 96f),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            "还没有交流",
            color = CoachInk,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            if (videoContext) "围绕这段视频，说说你的想法" else "发一条消息，开始聊聊",
            color = CoachMeta,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
internal fun WorkspaceThreadDiscussionSyncState(
    errorMessage: String?,
    onRetry: () -> Unit,
) {
    if (errorMessage == null) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp)
                .testTag("workspace-thread-discussion-loading"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            CircularProgressIndicator(Modifier.size(18.dp), color = DSColors.fairway, strokeWidth = 2.dp)
            Text(
                "正在同步交流记录…",
                color = DSColors.shadowGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        return
    }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF4DC).copy(alpha = 0.72f))
            .border(1.dp, DSColors.trophyGold.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(11.dp).testTag("workspace-thread-discussion-sync-error"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.Refresh, null, tint = DSColors.trophyGold, modifier = Modifier.size(17.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "交流记录暂时没有同步下来",
                color = DSColors.inkGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "原内容和已写草稿仍然保留，重新同步后即可继续",
                color = DSColors.shadowGray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
            )
        }
        Text(
            "重试",
            color = DSColors.fairway,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.height(30.dp).clip(RoundedCornerShape(15.dp))
                .background(DSColors.cardWhite.copy(alpha = 0.92f))
                .clickable(onClick = onRetry).padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
internal fun WorkspaceLoadOlderRepliesButton(
    loading: Boolean,
    onClick: () -> Unit,
) {
    Text(
        "加载更早的回复",
        color = DSColors.fairway.copy(alpha = if (loading) 0.55f else 1f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.clickable(enabled = !loading, onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag("workspace-thread-load-older-replies"),
    )
}

@Composable
internal fun WorkspaceNewRepliesButton(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    Row(
        modifier = modifier.shadow(5.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(DSColors.fairway)
            .clickable(onClick = onClick)
            .height(36.dp)
            .padding(horizontal = 13.dp)
            .testTag("workspace-thread-new-replies"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(17.dp),
        )
        Text(
            if (count > 1) "$count 条新回复" else "新回复",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun WorkspaceReplyTimeDivider(raw: String?) {
    Text(
        workspaceConversationDateText(raw),
        color = DSColors.shadowGray,
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun WorkspaceNewReplyDivider(count: Int, coachPerspective: Boolean, friendSpace: Boolean) {
    val label = if (count > 1) "以下有 $count 条新回复"
    else if (friendSpace) "好友的新回复" else if (coachPerspective) "学员的新回复" else "教练的新回复"
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(DSColors.trophyGold.copy(alpha = 0.35f)))
        Text("✦  $label", color = DSColors.trophyGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.weight(1f).height(1.dp).background(DSColors.trophyGold.copy(alpha = 0.35f)))
    }
}
