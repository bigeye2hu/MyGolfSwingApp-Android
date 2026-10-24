package com.mygolfswing.app.ui.coach

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.mygolfswing.app.R
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.coach.V2CoachConnection
import com.mygolfswing.app.data.coach.V2CoachConnectionActivitySnapshot
import com.mygolfswing.app.data.coach.V2CellularUploadConfirmationRequired
import com.mygolfswing.app.data.coach.V2CoachDashboard
import com.mygolfswing.app.data.coach.V2CoachDeliveryJob
import com.mygolfswing.app.data.coach.V2CoachDeliveryPhase
import com.mygolfswing.app.data.coach.V2CoachLibraryAsset
import com.mygolfswing.app.data.coach.V2LibraryShareDraftStore
import com.mygolfswing.app.data.coach.V2CoachInviteStatus
import com.mygolfswing.app.data.coach.V2CoachIncomingActivity
import com.mygolfswing.app.data.coach.V2CoachLibrarySort
import com.mygolfswing.app.data.coach.V2CoachLessonMode
import com.mygolfswing.app.data.coach.V2CoachLessonScope
import com.mygolfswing.app.data.coach.V2CoachMetrics
import com.mygolfswing.app.data.coach.V2CoachReviewCardTone
import com.mygolfswing.app.data.coach.V2CoachReviewFocus
import com.mygolfswing.app.data.coach.V2CoachSnapshot
import com.mygolfswing.app.data.coach.V2CoachStudentAge
import com.mygolfswing.app.data.coach.V2CoachTrainingGoal
import com.mygolfswing.app.data.coach.V2CoachWorkspaceItem
import com.mygolfswing.app.data.coach.V2WorkspaceDraftStore
import com.mygolfswing.app.data.coach.V2_DELIVERY_LIBRARY_IMPORT
import com.mygolfswing.app.data.coach.V2_DELIVERY_WORKSPACE
import com.mygolfswing.app.data.coach.coachThrowableError
import com.mygolfswing.app.data.coach.completionForOwner
import com.mygolfswing.app.data.coach.coachStudentActivityLine
import com.mygolfswing.app.data.coach.coachStudentConversationPreview
import com.mygolfswing.app.data.coach.coachHomeStudentRecentSummary
import com.mygolfswing.app.data.coach.coachStudentRelativeTime
import com.mygolfswing.app.data.coach.coachReviewCardPresentation
import com.mygolfswing.app.data.coach.coachPendingReviewItems
import com.mygolfswing.app.data.coach.coachHomePriorityItems
import com.mygolfswing.app.data.coach.coachHomeVisibleConnections
import com.mygolfswing.app.data.coach.isCoachHomeQuestion
import com.mygolfswing.app.data.coach.matchesCoachFilters
import com.mygolfswing.app.data.coach.mergeWorkspaceFeedPage
import com.mygolfswing.app.data.coach.filterAndSortLibrary
import com.mygolfswing.app.data.coach.libraryCategories
import com.mygolfswing.app.data.coach.prioritizeLibraryShareDrafts
import com.mygolfswing.app.data.coach.jobsForOwner
import com.mygolfswing.app.data.coach.hasCoachPendingWork
import com.mygolfswing.app.data.coach.isFailedQueueJob
import com.mygolfswing.app.data.coach.resolveCoachConnectionActivity
import com.mygolfswing.app.data.coach.resolveCoachIncomingActivity
import com.mygolfswing.app.data.coach.latestStudentSpaceItemsByConnection
import com.mygolfswing.app.data.coach.shouldPresentCoachProfilePrompt
import com.mygolfswing.app.data.coach.sortCoachStudentConnections
import com.mygolfswing.app.data.coach.workspaceContentCounts
import com.mygolfswing.app.data.friend.V2IMConversation
import com.mygolfswing.app.data.session.V2Session
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.v2NavigationBarPadding
import com.mygolfswing.app.ui.common.V2PageArtwork
import com.mygolfswing.app.ui.common.V2PageArtworkBackground
import com.mygolfswing.app.ui.common.V2PageArtworkTone
import com.mygolfswing.app.ui.common.V2DeliveryCompletionBar
import com.mygolfswing.app.ui.common.V2RoomBackground
import com.mygolfswing.app.ui.common.V2UserAvatar
import com.mygolfswing.app.ui.common.copyPlainTextToClipboard
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.interaction.V2WorkspaceScreen
import com.mygolfswing.app.ui.interaction.V2InteractionChatScreen
import com.mygolfswing.app.ui.interaction.V2CellularUploadConfirmationDialog
import com.mygolfswing.app.ui.interaction.V2DeliveryQueueDialog
import com.mygolfswing.app.ui.interaction.rememberQueueNetworkConnected
import com.mygolfswing.app.ui.interaction.preparePersistentLibraryImportVideo
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import java.util.UUID
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal enum class CoachTab(val label: String, val icon: ImageVector) {
    HOME("首页", Icons.Filled.Home),
    STUDENTS("学员", Icons.Filled.Groups),
    REVIEW("点评", Icons.Filled.RateReview),
    LIBRARY("素材", Icons.Filled.AutoStories),
    ME("我", Icons.Filled.AccountCircle),
}

@Composable
internal fun CoachBottomTabBar(
    selected: CoachTab,
    reviewBadge: Int,
    libraryHasDraft: Boolean,
    spacesHaveUpdates: Boolean,
    studentsHaveChatUpdates: Boolean,
    onSelected: (CoachTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DSColors.cardWhite.copy(alpha = 0.96f))
            .drawBehind {
                drawRect(DSColors.borderLine, size = androidx.compose.ui.geometry.Size(size.width, 0.5.dp.toPx()))
            }
            .v2NavigationBarPadding()
            .padding(horizontal = 8.dp)
            .padding(top = 10.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoachOrdinaryTab(
                tab = CoachTab.HOME,
                selected = selected == CoachTab.HOME,
                badge = spacesHaveUpdates,
                badgeDescription = "空间有更新",
                modifier = Modifier.weight(1f),
                onClick = { onSelected(CoachTab.HOME) },
            )
            CoachOrdinaryTab(
                tab = CoachTab.STUDENTS,
                selected = selected == CoachTab.STUDENTS,
                badge = spacesHaveUpdates || studentsHaveChatUpdates,
                badgeDescription = "有新消息",
                modifier = Modifier.weight(1f),
                onClick = { onSelected(CoachTab.STUDENTS) },
            )
            Spacer(Modifier.size(width = 66.dp, height = 1.dp))
            CoachOrdinaryTab(
                tab = CoachTab.LIBRARY,
                selected = selected == CoachTab.LIBRARY,
                badge = libraryHasDraft,
                badgeDescription = "有未完成的素材",
                badgeColor = DSColors.trophyGold,
                badgeBorder = true,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(CoachTab.LIBRARY) },
            )
            CoachOrdinaryTab(
                tab = CoachTab.ME,
                selected = selected == CoachTab.ME,
                badge = false,
                badgeDescription = "",
                modifier = Modifier.weight(1f),
                onClick = { onSelected(CoachTab.ME) },
            )
        }

        CoachReviewTab(
            selected = selected == CoachTab.REVIEW,
            badge = reviewBadge,
            onClick = { onSelected(CoachTab.REVIEW) },
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-18).dp),
        )
    }
}

@Composable
private fun CoachOrdinaryTab(
    tab: CoachTab,
    selected: Boolean,
    badge: Boolean,
    badgeDescription: String,
    modifier: Modifier,
    badgeColor: Color = DSColors.flagRed,
    badgeBorder: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (selected) DSColors.fairway else DSColors.shadowGray
    Column(
        modifier = modifier.fillMaxSize().clickable(onClick = onClick)
            .testTag("coach-tab-${tab.name.lowercase()}")
            .semantics {
                contentDescription = tab.label
                this.selected = selected
                stateDescription = listOfNotNull(
                    "当前页面".takeIf { selected },
                    badgeDescription.takeIf { badge },
                ).joinToString("，")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(tab.icon, null, tint = tint, modifier = Modifier.size(19.dp))
            if (badge) {
                Box(
                    Modifier.offset(x = 5.dp, y = (-4).dp).size(8.dp).clip(CircleShape)
                        .background(badgeColor)
                        .then(if (badgeBorder) Modifier.border(1.5.dp, DSColors.cardWhite, CircleShape) else Modifier)
                        .testTag("coach-tab-${tab.name.lowercase()}-badge"),
                )
            }
        }
        Text(tab.label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = tint)
        Box(
            Modifier.size(4.dp).clip(CircleShape)
                .background(if (selected) DSColors.fairway else Color.Transparent)
                .testTag("coach-tab-${tab.name.lowercase()}-selection"),
        )
    }
}

@Composable
private fun CoachReviewTab(
    selected: Boolean,
    badge: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(60.dp)
            .border(1.5.dp, if (selected) DSColors.trophyGold else Color.Transparent, CircleShape)
            .padding(2.dp)
            .clickable(onClick = onClick)
            .testTag("coach-tab-review")
            .semantics {
                contentDescription = "点评"
                this.selected = selected
                stateDescription = listOfNotNull(
                    "当前页面".takeIf { selected },
                    "${badge.coerceAtMost(99)} 条待点评".takeIf { badge > 0 },
                ).joinToString("，")
            },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = DSColors.fairway.copy(alpha = 0.42f),
                spotColor = DSColors.fairway.copy(alpha = 0.42f),
            ).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF3D9A6A), DSColors.fairway)))
                .border(4.dp, DSColors.cardWhite, CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        ) {
            CoachReviewGlyph()
            Text("点评", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        if (badge > 0) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp)
                    .widthIn(min = 20.dp).heightIn(min = 20.dp)
                    .clip(RoundedCornerShape(10.dp)).background(DSColors.flagRed)
                    .border(2.dp, DSColors.cardWhite, RoundedCornerShape(10.dp))
                    .padding(horizontal = if (badge > 9) 5.dp else 0.dp)
                    .testTag("coach-tab-review-badge"),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (badge > 99) "99+" else badge.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CoachReviewGlyph() {
    Canvas(Modifier.size(16.dp)) {
        val stroke = 1.8.dp.toPx()
        drawRoundRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(1.5.dp.toPx(), 3.5.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(10.5.dp.toPx(), 10.5.dp.toPx()),
            cornerRadius = CornerRadius(1.5.dp.toPx()),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = Color.White,
            start = androidx.compose.ui.geometry.Offset(7.dp.toPx(), 11.5.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(14.5.dp.toPx(), 4.dp.toPx()),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.White,
            start = androidx.compose.ui.geometry.Offset(6.5.dp.toPx(), 12.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(6.dp.toPx(), 14.dp.toPx()),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

internal fun coachOutgoingQueueTitle(
    jobs: List<V2CoachDeliveryJob>,
    activeJobId: String?,
): String {
    val failedCount = jobs.count(V2CoachDeliveryJob::isFailedQueueJob)
    if (failedCount > 0) return "${failedCount}条内容发送暂停"
    val active = jobs.firstOrNull { it.jobId == activeJobId }
    if (active != null) return "正在发送给 ${active.peerName.ifBlank { "对方" }}"
    return "发送队列有 ${jobs.size} 条内容"
}

internal fun coachOutgoingQueueDetail(
    jobs: List<V2CoachDeliveryJob>,
    activeJobId: String?,
    activePhase: V2CoachDeliveryPhase?,
    networkConnected: Boolean,
): String {
    if (jobs.any(V2CoachDeliveryJob::isFailedQueueJob)) {
        return "已上传内容不会重传，点此查看并重试"
    }
    if (!networkConnected) return "等待网络，联网后会自动继续"
    val active = jobs.firstOrNull { it.jobId == activeJobId }
    if (active != null) {
        val remaining = (jobs.size - 1).coerceAtLeast(0)
        val phase = activePhase?.displayText ?: "正在发送"
        return if (remaining > 0) "$phase · 后面还有${remaining}条" else "$phase · 可以继续处理其他学员"
    }
    return "即将开始，可以继续处理其他学员"
}

@Composable
internal fun CoachOutgoingQueueBar(
    jobs: List<V2CoachDeliveryJob>,
    activeJobId: String?,
    activePhase: V2CoachDeliveryPhase?,
    activeProgress: Double,
    networkConnected: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = jobs.firstOrNull { it.jobId == activeJobId }
    val hasFailure = jobs.any(V2CoachDeliveryJob::isFailedQueueJob)
    val progress = activeProgress.toFloat().coerceIn(0f, 1f)
    Box(
        modifier = modifier.fillMaxWidth().height(46.dp)
            .background(if (hasFailure) Color(0xFFFFF4DC).copy(alpha = 0.76f) else DSColors.cardWhite.copy(alpha = 0.97f))
            .drawBehind {
                drawRect(DSColors.borderLine, size = androidx.compose.ui.geometry.Size(size.width, 0.5.dp.toPx()))
            }
            .clickable(onClick = onOpen)
            .testTag("coach-outgoing-queue-bar"),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                when {
                    hasFailure && active == null -> Icon(
                        Icons.Filled.Refresh,
                        null,
                        tint = DSColors.trophyGold,
                        modifier = Modifier.size(18.dp),
                    )
                    !networkConnected -> Icon(
                        Icons.Filled.WifiOff,
                        null,
                        tint = DSColors.trophyGold,
                        modifier = Modifier.size(18.dp),
                    )
                    else -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = DSColors.fairway,
                        strokeWidth = 2.dp,
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    coachOutgoingQueueTitle(jobs, activeJobId),
                    color = DSColors.inkGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    coachOutgoingQueueDetail(jobs, activeJobId, activePhase, networkConnected),
                    color = DSColors.shadowGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (active != null && activePhase in setOf(V2CoachDeliveryPhase.PREPARING, V2CoachDeliveryPhase.UPLOADING)) {
                Text(
                    "${(progress * 100).toInt()}%",
                    color = DSColors.fairway,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("查看", color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = DSColors.shadowGray,
                modifier = Modifier.size(12.dp),
            )
        }
        if (active != null && !hasFailure) {
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth(progress.coerceAtLeast(0.02f)).height(2.dp)
                    .background(DSColors.fairway),
            )
        }
    }
}

@Composable
fun V2CoachRootScreen(
    onSwitchToStudent: () -> Unit,
    onLogoutComplete: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selected by rememberSaveable { mutableStateOf(CoachTab.HOME) }
    var snapshot by remember { mutableStateOf(V2CoachSnapshot()) }
    var previousDashboard by remember { mutableStateOf<V2CoachDashboard?>(null) }
    var previousConnections by remember { mutableStateOf<List<V2CoachConnection>?>(null) }
    var incomingActivity by remember { mutableStateOf<V2CoachIncomingActivity?>(null) }
    var lastIncomingActivityId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    var inviteLoading by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf<String?>(null) }
    var inviteUrl by remember { mutableStateOf<String?>(null) }
    var inviteId by remember { mutableStateOf<String?>(null) }
    var inviteExpiresAt by remember { mutableStateOf<String?>(null) }
    var inviteStatus by remember { mutableStateOf<V2CoachInviteStatus?>(null) }
    var selectedConnection by remember { mutableStateOf<V2CoachConnection?>(null) }
    var selectedStudentChat by remember { mutableStateOf<V2IMConversation?>(null) }
    var studentCommunicationCount by remember { mutableIntStateOf(0) }
    var selectedConversation by remember { mutableStateOf<V2CoachWorkspaceItem?>(null) }
    var selectedConversationItemId by remember { mutableStateOf<String?>(null) }
    var editingConnection by remember { mutableStateOf<V2CoachConnection?>(null) }
    var onboardingConnectionId by remember { mutableStateOf<String?>(null) }
    var profilePromptsPresented by remember { mutableStateOf(emptySet<String>()) }
    var showDeliveryQueue by remember { mutableStateOf(false) }
    var libraryDraftRevision by remember { mutableIntStateOf(0) }
    val deliveryState by ServiceLocator.coachDeliveryQueue.state.collectAsState()
    val coachSession by ServiceLocator.session.state.collectAsState(initial = V2Session())
    val coachRoleState by ServiceLocator.roleStore.state.collectAsState()
    val ownerDeliveryJobs = deliveryState.jobsForOwner(coachSession.userId)
    val deliveryCompletion = deliveryState.completionForOwner(coachSession.userId)
    val deliveryNetworkConnected = rememberQueueNetworkConnected()
    val coachPreferences = remember {
        context.getSharedPreferences("v2_coach_preferences", Context.MODE_PRIVATE)
    }
    val profilePromptPreferences = remember {
        context.getSharedPreferences("v2_coach_profile_prompts", Context.MODE_PRIVATE)
    }
    val rootDraftStore = remember(context) { V2WorkspaceDraftStore(context) }
    val libraryShareDraftStore = remember(context) { V2LibraryShareDraftStore(context) }
    val ownerUserId = coachSession.userId.orEmpty()
    val localReplyDraftConnectionByItemId = if (ownerUserId.isBlank()) {
        emptyMap()
    } else {
        buildMap {
            snapshot.connections.forEach { connection ->
                rootDraftStore.threadDraftItemIds(ownerUserId, connection.connectionId).forEach { itemId ->
                    put(itemId, connection.connectionId)
                }
            }
        }
    }
    val localReplyDraftItemIds = localReplyDraftConnectionByItemId.keys
    val pendingReviewItemIds = snapshot.reviewQueue
        .filter { it.reviewState != "responded" }
        .mapTo(mutableSetOf(), V2CoachWorkspaceItem::itemId)
    val reviewWorkCount = snapshot.dashboard.pendingCount +
        (localReplyDraftItemIds - pendingReviewItemIds).size
    val libraryHasDraft = remember(
        libraryDraftRevision,
        ownerDeliveryJobs,
        ownerUserId,
    ) {
        ownerDeliveryJobs.any { it.deliveryType == V2_DELIVERY_LIBRARY_IMPORT } ||
            (ownerUserId.isNotBlank() && libraryShareDraftStore.hasAnyDraft(ownerUserId))
    }
    var lessonScopeName by rememberSaveable {
        mutableStateOf(coachPreferences.getString("lesson_scope", V2CoachLessonScope.ALL.name) ?: V2CoachLessonScope.ALL.name)
    }
    val lessonScope = V2CoachLessonScope.entries.firstOrNull { it.name == lessonScopeName }
        ?: V2CoachLessonScope.ALL

    fun selectLessonScope(value: V2CoachLessonScope) {
        lessonScopeName = value.name
        coachPreferences.edit { putString("lesson_scope", value.name) }
    }

    fun presentIncomingActivity(notice: V2CoachIncomingActivity?) {
        if (notice == null || notice.id == lastIncomingActivityId) return
        lastIncomingActivityId = notice.id
        incomingActivity = notice
    }

    fun noticesFor(updated: V2CoachSnapshot): V2CoachIncomingActivity? {
        val names = updated.connections.mapNotNull { connection ->
            connection.student?.nickname?.let { connection.connectionId to it }
        }.toMap()
        val connectionNotice = resolveCoachConnectionActivity(
            previous = previousConnections,
            current = updated.connections,
            currentUserId = coachSession.userId,
        )
        val dashboardNotice = resolveCoachIncomingActivity(previousDashboard, updated.dashboard, names)
        previousConnections = updated.connections
        previousDashboard = updated.dashboard
        return connectionNotice ?: dashboardNotice
    }

    suspend fun refreshStudentCommunicationCount(dashboard: V2CoachDashboard) {
        val currentSession = ServiceLocator.session.current()
        if (!currentSession.isLoggedIn) {
            studentCommunicationCount = 0
            return
        }
        suspend fun renewTencentSession(): V2Session {
            ServiceLocator.authRepo.refreshUserSig().getOrThrow()
            return ServiceLocator.session.current()
        }
        val workspaceCount = dashboard.connectionStates.sumOf { it.newCount }
            .takeIf { it > 0 }
            ?: dashboard.newByConnection.values.sum()
        val imCount = ServiceLocator.tencentFriendRepo.conversations(
            session = currentSession,
            renewExpiredSession = ::renewTencentSession,
        ).getOrNull()?.sumOf(V2IMConversation::unreadCount) ?: 0
        val requestCount = ServiceLocator.tencentFriendRepo.incomingFriendRequests(
            session = ServiceLocator.session.current(),
            renewExpiredSession = ::renewTencentSession,
        ).getOrNull()?.size ?: 0
        studentCommunicationCount = workspaceCount + imCount + requestCount
    }

    suspend fun reloadNow() {
        loading = true
        error = null
        ServiceLocator.coachWorkspaceRepo.loadSnapshot()
            .onSuccess { updated ->
                val notice = noticesFor(updated)
                snapshot = updated
                presentIncomingActivity(notice)
                refreshStudentCommunicationCount(updated.dashboard)
            }
            .onFailure { error = coachThrowableError("暂时无法加载教练工作台，请稍后重试", it) }
        loading = false
    }

    fun reload() { scope.launch { reloadNow() } }

    fun refreshDashboard() {
        scope.launch {
            runCatching {
                ServiceLocator.coachWorkspaceRepo.dashboardUpdate(snapshot.dashboardVersion)
            }.onSuccess { update ->
                val updatedDashboard = update.snapshot ?: snapshot.dashboard
                val notice = update.snapshot?.let { dashboard ->
                    val names = snapshot.connections.mapNotNull { connection ->
                        connection.student?.nickname?.let { connection.connectionId to it }
                    }.toMap()
                    resolveCoachIncomingActivity(previousDashboard, dashboard, names)
                }
                update.snapshot?.let { previousDashboard = it }
                snapshot = snapshot.copy(
                    dashboard = updatedDashboard,
                    dashboardVersion = update.version ?: snapshot.dashboardVersion,
                )
                presentIncomingActivity(notice)
                refreshStudentCommunicationCount(updatedDashboard)
            }
        }
    }

    fun generateInvite(force: Boolean = false) {
        showInvite = true
        if ((!force && inviteCode != null) || inviteLoading) return
        inviteLoading = true
        error = null
        if (force) {
            inviteId = null
            inviteCode = null
            inviteUrl = null
            inviteExpiresAt = null
            inviteStatus = null
        }
        scope.launch {
            ServiceLocator.coachWorkspaceRepo.createInvite()
                .onSuccess {
                    inviteId = it.inviteId
                    inviteCode = it.inviteCode
                    inviteUrl = it.inviteUrl
                    inviteExpiresAt = it.expiresAt
                }
                .onFailure { error = coachThrowableError("暂时无法生成邀请，请稍后重试", it) }
            inviteLoading = false
        }
    }

    fun openInvite() = generateInvite(force = false)

    fun profilePromptKey(userId: String, connectionId: String): String =
        "relationship_profile_prompt_${userId}_$connectionId"

    LaunchedEffect(coachSession.userId) {
        profilePromptsPresented = emptySet()
        onboardingConnectionId = null
    }

    LaunchedEffect(
        snapshot.connections,
        coachSession.userId,
        loading,
        showInvite,
        showSettings,
        showDeliveryQueue,
        selectedConnection,
        editingConnection,
    ) {
        val ownerUserId = coachSession.userId?.takeIf(String::isNotBlank) ?: return@LaunchedEffect
        if (loading || showInvite || showSettings || showDeliveryQueue ||
            selectedConnection != null || editingConnection != null
        ) return@LaunchedEffect
        val now = System.currentTimeMillis()
        val connection = snapshot.connections.firstOrNull { candidate ->
            val deferredAt = profilePromptPreferences.getLong(
                profilePromptKey(ownerUserId, candidate.connectionId),
                Long.MIN_VALUE,
            ).takeUnless { it == Long.MIN_VALUE }
            candidate.status == "active" &&
                !candidate.isCoachingProfileComplete &&
                candidate.connectionId !in profilePromptsPresented &&
                shouldPresentCoachProfilePrompt(deferredAt, now)
        } ?: return@LaunchedEffect
        profilePromptsPresented = profilePromptsPresented + connection.connectionId
        onboardingConnectionId = connection.connectionId
        editingConnection = connection
    }

    LaunchedEffect(lifecycleOwner, showInvite, inviteId) {
        val activeInviteId = inviteId ?: return@LaunchedEffect
        if (!showInvite) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                ServiceLocator.coachWorkspaceRepo.inviteStatus(activeInviteId)
                    .onSuccess { inviteStatus = it }
                if (inviteStatus?.isUsed == true || inviteStatus?.isExpired == true) {
                    if (inviteStatus?.isUsed == true) reload()
                    break
                }
                delay(2_000)
            }
        }
    }

    editingConnection?.let { connection ->
        val isOnboarding = onboardingConnectionId == connection.connectionId
        V2CoachStudentProfileEditor(
            connection = connection,
            isOnboarding = isOnboarding,
            onBack = {
                if (isOnboarding && !connection.isCoachingProfileComplete) {
                    coachSession.userId?.takeIf(String::isNotBlank)?.let { ownerUserId ->
                        profilePromptPreferences.edit {
                            putLong(
                                profilePromptKey(ownerUserId, connection.connectionId),
                                System.currentTimeMillis(),
                            )
                        }
                    }
                }
                onboardingConnectionId = null
                editingConnection = null
            },
            save = { input -> ServiceLocator.coachWorkspaceRepo.updateCoachProfile(connection.connectionId, input) },
            onSaved = { updated ->
                coachSession.userId?.takeIf(String::isNotBlank)?.let { ownerUserId ->
                    profilePromptPreferences.edit {
                        remove(profilePromptKey(ownerUserId, updated.connectionId))
                    }
                }
                snapshot = snapshot.copy(
                    connections = snapshot.connections.map {
                        if (it.connectionId == updated.connectionId) updated else it
                    },
                )
                onboardingConnectionId = null
                editingConnection = null
                reload()
            },
            onUpdated = { updated ->
                snapshot = snapshot.copy(
                    connections = snapshot.connections.map {
                        if (it.connectionId == updated.connectionId) updated else it
                    },
                )
                editingConnection = updated
                reload()
            },
        )
        return
    }

    selectedStudentChat?.let { chat ->
        V2InteractionChatScreen(
            conversation = chat,
            subtitle = if (chat.isCoach) "我的学员" else "好友私聊",
            onBack = { selectedStudentChat = null },
        )
        return
    }

    selectedConnection?.let { connection ->
        V2WorkspaceScreen(
            connection = connection,
            coachPerspective = true,
            availableConnections = snapshot.connections,
            switcherItems = snapshot.dashboard.recentItems + snapshot.dashboard.studentPreviewItems + snapshot.reviewQueue,
            onSwitchConnection = { next ->
                selectedConnection = next
                selectedConversation = null
                selectedConversationItemId = null
            },
            onAddSpace = {
                selectedConnection = null
                selectedConversation = null
                selectedConversationItemId = null
                openInvite()
            },
            onEditConnection = { target ->
                onboardingConnectionId = null
                editingConnection = target
            },
            onConnectionUpdated = { updated ->
                snapshot = snapshot.copy(
                    connections = snapshot.connections.map { current ->
                        if (current.connectionId == updated.connectionId) updated else current
                    },
                )
                selectedConnection = updated
            },
            initialItem = selectedConversation,
            initialItemId = selectedConversationItemId ?: selectedConversation?.itemId,
            onDisconnected = {
                selectedConnection = null
                selectedConversation = null
                selectedConversationItemId = null
                reload()
            },
            onBack = {
                selectedConnection = null
                selectedConversation = null
                selectedConversationItemId = null
            },
        )
        return
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            ServiceLocator.session.current().userId?.let { ownerUserId ->
                ServiceLocator.coachDeliveryQueue.recover(ownerUserId, ServiceLocator.coachWorkspaceRepo)
            }
            reload()
            var refreshCycle = 0
            while (true) {
                delay(60_000)
                refreshCycle += 1
                if (refreshCycle % 5 == 0) reload() else refreshDashboard()
            }
        }
    }

    LaunchedEffect(incomingActivity?.id) {
        val shownId = incomingActivity?.id ?: return@LaunchedEffect
        delay(8_000)
        if (incomingActivity?.id == shownId) incomingActivity = null
    }

    LaunchedEffect(deliveryCompletion?.id) {
        val completionId = deliveryCompletion?.id ?: return@LaunchedEffect
        delay(5_000)
        ServiceLocator.coachDeliveryQueue.dismissCompletion(completionId)
    }

    V2BackHandler(enabled = selected != CoachTab.HOME) { selected = CoachTab.HOME }

    Scaffold(
        containerColor = Color(0xFFF7F4ED),
        bottomBar = {
            Column {
                incomingActivity?.let { notice ->
                    CoachIncomingActivityBar(
                        notice = notice,
                        onOpen = {
                            snapshot.connections.firstOrNull { it.connectionId == notice.connectionId }?.let { connection ->
                                selectedConversation = null
                                selectedConversationItemId = notice.itemId
                                selectedConnection = connection
                            }
                            incomingActivity = null
                        },
                        onDismiss = { incomingActivity = null },
                    )
                }
                if (ownerDeliveryJobs.isNotEmpty()) {
                    CoachOutgoingQueueBar(
                        jobs = ownerDeliveryJobs,
                        activeJobId = deliveryState.activeJobId,
                        activePhase = deliveryState.activePhase,
                        activeProgress = deliveryState.activeProgress,
                        networkConnected = deliveryNetworkConnected,
                        onOpen = { showDeliveryQueue = true },
                    )
                } else if (deliveryCompletion != null) {
                    val completion = deliveryCompletion
                    V2DeliveryCompletionBar(
                        completion = completion,
                        title = completion.peerName.takeIf(String::isNotBlank)
                            ?.let { "$it · ${completion.title}" }
                            ?: completion.title,
                        actionLabel = if (completion.opensLibrary) "查看素材" else "查看对话",
                        onAction = {
                            if (completion.opensLibrary) {
                                selected = CoachTab.LIBRARY
                            } else {
                                completion.connectionId?.let { connectionId ->
                                    snapshot.connections.firstOrNull { it.connectionId == connectionId }?.let { connection ->
                                        selectedConversation = null
                                        selectedConversationItemId = completion.itemId
                                        selectedConnection = connection
                                    }
                                }
                            }
                            ServiceLocator.coachDeliveryQueue.dismissCompletion(completion.id)
                        },
                        onDismiss = {
                            ServiceLocator.coachDeliveryQueue.dismissCompletion(completion.id)
                        },
                    )
                }
                CoachBottomTabBar(
                    selected = selected,
                    reviewBadge = reviewWorkCount,
                    libraryHasDraft = libraryHasDraft,
                    spacesHaveUpdates = snapshot.dashboard.newCount > 0 ||
                        snapshot.dashboard.connectionStates.any { it.newCount > 0 || it.studentHasUnread },
                    studentsHaveChatUpdates = studentCommunicationCount > 0,
                    onSelected = { selected = it },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            V2RoomBackground(Modifier.fillMaxSize())
            when {
                loading && snapshot == V2CoachSnapshot() -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = DSColors.fairway)
                else -> when (selected) {
                    CoachTab.HOME -> CoachHome(
                        snapshot,
                        error,
                        session = coachSession,
                        refreshing = loading,
                        deliveryMessage = deliveryState.message.takeIf { deliveryCompletion == null },
                        onRefresh = ::reload,
                        onSettings = { showSettings = true },
                        onInvite = ::openInvite,
                        onOpenStudents = { selected = CoachTab.STUDENTS },
                        onOpenStudent = { connection -> selectedConnection = connection },
                        onOpenReview = { selected = CoachTab.REVIEW },
                        onOpenLibrary = { selected = CoachTab.LIBRARY },
                        onOpenItem = { item ->
                            snapshot.connections.firstOrNull { it.connectionId == item.connectionId }?.let {
                                selectedConversation = item
                                selectedConversationItemId = item.itemId
                                selectedConnection = it
                            }
                        },
                    )
                    CoachTab.STUDENTS -> V2CoachStudentsInteractionScreen(
                        connections = snapshot.connections,
                        dashboard = snapshot.dashboard,
                        loadingSpaces = loading,
                        syncError = error,
                        currentUserId = coachSession.userId,
                        onReloadSpaces = ::reloadNow,
                        onInviteStudent = ::openInvite,
                        onOpenStudent = { selectedConnection = it },
                        onOpenChat = { selectedStudentChat = it },
                        onCommunicationCountChanged = { studentCommunicationCount = it },
                    )
                    CoachTab.REVIEW -> CoachReview(
                        items = snapshot.reviewQueue,
                        initialNextCursor = snapshot.reviewNextCursor,
                        initialTotal = snapshot.reviewTotal,
                        initialVersion = snapshot.reviewVersion,
                        pendingWorkCount = reviewWorkCount,
                        initialRespondedCount = snapshot.dashboard.respondedCount,
                        initialError = error,
                        connections = snapshot.connections,
                        localReplyDraftItemIds = localReplyDraftItemIds,
                        localReplyDraftConnectionByItemId = localReplyDraftConnectionByItemId,
                        draftCandidateItems = snapshot.dashboard.recentItems + snapshot.dashboard.studentPreviewItems,
                        deliveryJobs = ownerDeliveryJobs.filter { it.deliveryType == V2_DELIVERY_WORKSPACE },
                        onReload = ::reload,
                        onOpenStudents = { selected = CoachTab.STUDENTS },
                    ) { item ->
                        snapshot.connections.firstOrNull { it.connectionId == item.connectionId }?.let {
                            selectedConversation = item
                            selectedConversationItemId = item.itemId
                            selectedConnection = it
                        }
                    }
                    CoachTab.LIBRARY -> CoachLibrary(
                        assets = snapshot.library,
                        initialNextCursor = snapshot.libraryNextCursor,
                        connections = snapshot.connections,
                        pendingByConnection = snapshot.dashboard.pendingByConnection,
                        importJobs = ownerDeliveryJobs.filter { it.deliveryType == V2_DELIVERY_LIBRARY_IMPORT },
                        activeDeliveryJobId = deliveryState.activeJobId,
                        activeDeliveryPhase = deliveryState.activePhase,
                        activeDeliveryProgress = deliveryState.activeProgress,
                        recentItems = snapshot.reviewQueue + snapshot.dashboard.recentItems,
                        ownerUserId = ownerUserId,
                        connectionsLoading = loading,
                        connectionsError = error,
                        draftStore = libraryShareDraftStore,
                        onDraftChanged = { libraryDraftRevision += 1 },
                        onReload = ::reload,
                        onOpenStudents = { selected = CoachTab.STUDENTS },
                        onOpenSharedConversation = { connection, item ->
                            selectedConversation = item
                            selectedConversationItemId = item.itemId
                            selectedConnection = connection
                        },
                    )
                    CoachTab.ME -> CoachProfile(
                        snapshot = snapshot,
                        deliveryPendingCount = ownerDeliveryJobs.size,
                        onRefresh = ::reload,
                        onOpenSettings = { showSettings = true },
                        onSwitchToStudent = onSwitchToStudent,
                        onLogoutComplete = onLogoutComplete,
                    )
                }
            }
        }
    }

    if (showSettings) {
        V2CoachSettingsDialog(
            session = coachSession,
            roleStatusText = if (
                coachRoleState.isCoachPreviewEnabled &&
                !coachRoleState.authorizationStatus.canUseCoachExperience
            ) {
                "开发版界面预览"
            } else {
                coachRoleState.authorizationStatus.displayText
            },
            deliveryPendingCount = ownerDeliveryJobs.size,
            onDismiss = { showSettings = false },
            onRequestRoleSelection = onSwitchToStudent,
        )
    }
    if (showDeliveryQueue && coachSession.userId != null) {
        V2DeliveryQueueDialog(
            ownerUserId = coachSession.userId.orEmpty(),
            onDismiss = { showDeliveryQueue = false },
            onOpenConnection = { connectionId, itemId ->
                snapshot.connections.firstOrNull { it.connectionId == connectionId }?.let { connection ->
                    selectedConversation = (snapshot.reviewQueue + snapshot.dashboard.recentItems)
                        .firstOrNull { it.itemId == itemId }
                    selectedConversationItemId = itemId
                    selectedConnection = connection
                }
            },
        )
    }
    if (showInvite) {
        CoachInviteDialog(
            loading = inviteLoading,
            inviteCode = inviteCode,
            inviteUrl = inviteUrl,
            expiresAt = inviteExpiresAt,
            status = inviteStatus,
            error = error,
            onGenerateNew = { generateInvite(force = true) },
            onDismiss = { showInvite = false },
        )
    }
}

@Composable
internal fun CoachInviteDialog(
    loading: Boolean,
    inviteCode: String?,
    inviteUrl: String?,
    expiresAt: String?,
    status: V2CoachInviteStatus?,
    error: String?,
    onGenerateNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCode by remember(inviteCode) { mutableStateOf(false) }
    var copiedCode by remember(inviteCode) { mutableStateOf(false) }
    var copiedLink by remember(inviteUrl) { mutableStateOf(false) }
    var saveMessage by remember(inviteUrl) { mutableStateOf<String?>(null) }
    val expired = status?.isExpired == true || inviteHasExpired(expiresAt)
    val qrBitmap = remember(inviteUrl) { inviteUrl?.let(::generateCoachInviteQrBitmap) }
    val legacyQrSave = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val url = inviteUrl ?: return@rememberLauncherForActivityResult
        scope.launch {
            saveMessage = writeCoachInvitePoster(context, uri, url)
        }
    }

    fun copy(label: String, value: String): Boolean =
        context.copyPlainTextToClipboard(label, value)

    fun shareInvite() {
        val value = inviteUrl ?: inviteCode ?: return
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "挥呗教练邀请")
            putExtra(Intent.EXTRA_TEXT, "我邀请你进入挥呗教学空间：$value")
        }
        context.startActivity(Intent.createChooser(share, "分享邀请"))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(DSColors.warmWhite).v2NavigationBarPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                top = 12.dp,
                end = 20.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "关闭邀请页",
                        tint = DSColors.inkGreen,
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.White)
                            .border(1.dp, DSColors.listCardBorder, CircleShape)
                            .clickable(onClick = onDismiss).padding(11.dp),
                    )
                    Text(
                        "邀请学员",
                        color = DSColors.inkGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.size(42.dp))
                }
            }

            item {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("邀请学员加入挥呗", color = DSColors.inkGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("一起让高尔夫变得更简单", color = DSColors.shadowGray, fontSize = 13.sp)
                    Text(
                        "Better Golf\nBetter People",
                        color = Color(0xFFA97822),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            item {
                when {
                    loading -> CoachInviteStateCard {
                        CircularProgressIndicator(Modifier.size(34.dp), color = DSColors.fairway)
                        Text("正在生成专属邀请…", color = DSColors.shadowGray, fontSize = 14.sp)
                    }
                    status?.isUsed == true -> CoachInviteStateCard {
                        Icon(Icons.Filled.CheckCircle, null, tint = DSColors.fairway, modifier = Modifier.size(48.dp))
                        Text("学员已完成连接", color = DSColors.inkGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${status.student?.nickname ?: "新学员"}已经进入你的教学工作台",
                            color = DSColors.shadowGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                        CoachInvitePrimaryButton("查看学员", onDismiss)
                        CoachInviteSecondaryButton(
                            "继续邀请另一位学员",
                            Icons.Filled.Add,
                            onClick = onGenerateNew,
                        )
                    }
                    expired -> CoachInviteStateCard {
                        Text("⏱", fontSize = 34.sp)
                        Text("这份邀请已经失效", color = DSColors.inkGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "旧二维码和连接码不会再建立连接，重新生成后再分享给学员。",
                            color = DSColors.shadowGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                        CoachInvitePrimaryButton("生成新邀请", onGenerateNew)
                    }
                    inviteCode != null -> Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.9f))
                            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().clip(CircleShape).background(DSColors.mistGreen.copy(alpha = 0.7f)).padding(4.dp),
                        ) {
                            CoachInviteModeButton("二维码邀请", !showCode, Modifier.weight(1f)) { showCode = false }
                            CoachInviteModeButton("邀请码", showCode, Modifier.weight(1f)) { showCode = true }
                        }
                        if (!showCode && qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "教学空间邀请二维码",
                                modifier = Modifier.size(226.dp).clip(RoundedCornerShape(16.dp))
                                    .background(Color.White).border(1.dp, DSColors.listCardBorder, RoundedCornerShape(16.dp)).padding(14.dp),
                            )
                            Text(
                                "让学员使用挥呗扫码，立即建立教学关系",
                                color = DSColors.shadowGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            Text("邀请码", color = DSColors.shadowGray, fontSize = 10.sp)
                            Row(
                                Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(14.dp))
                                    .background(DSColors.warmWhite)
                                    .border(1.dp, DSColors.listCardBorder, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    inviteCode,
                                    color = DSColors.inkGreen,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    if (copiedCode) Icons.Filled.CheckCircle else Icons.Filled.ContentCopy,
                                    contentDescription = "复制邀请码",
                                    tint = DSColors.fairway,
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(DSColors.mistGreen)
                                        .clickable { copy("挥呗邀请码", inviteCode); copiedCode = true }.padding(9.dp),
                                )
                            }
                            Text(
                                "让学员在挥呗中输入邀请码，立即建立教学关系",
                                color = DSColors.shadowGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    else -> CoachInviteStateCard {
                        Text(error ?: "暂时无法生成邀请", color = DSColors.shadowGray, fontSize = 14.sp, textAlign = TextAlign.Center)
                        CoachInvitePrimaryButton("重试", onGenerateNew)
                    }
                }
            }

            if (!loading && inviteCode != null && status?.isUsed != true && !expired) {
                item {
                    if (!showCode) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CoachInviteSecondaryButton(
                                saveMessage ?: "保存二维码",
                                Icons.Filled.Download,
                                modifier = Modifier.weight(1f),
                            ) {
                                val url = inviteUrl ?: return@CoachInviteSecondaryButton
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    scope.launch { saveMessage = saveCoachInvitePoster(context, url) }
                                } else {
                                    legacyQrSave.launch("huibei_coach_invite_poster_${System.currentTimeMillis()}.png")
                                }
                            }
                            CoachInvitePrimaryButton("分享邀请", ::shareInvite, Modifier.weight(1f), Icons.Filled.Share)
                        }
                    } else {
                        CoachInviteSecondaryButton("分享邀请", Icons.Filled.Share, Modifier.fillMaxWidth(), ::shareInvite)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CoachInviteSecondaryButton(
                                if (copiedLink) "已复制链接" else "复制链接",
                                Icons.Filled.ContentCopy,
                                Modifier.weight(1f),
                            ) { inviteUrl?.let { copiedLink = copy("挥呗教练邀请", it) } }
                            CoachInviteSecondaryButton(
                                if (copiedCode) "已复制邀请码" else "复制邀请码",
                                Icons.Filled.QrCode2,
                                Modifier.weight(1f),
                            ) { copiedCode = copy("挥呗邀请码", inviteCode) }
                        }
                    }
                    Text(
                        inviteExpiryText(expiresAt),
                        color = DSColors.shadowGray,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    )
                }
            }

            item { CoachInvitePrivacyNote() }
        }
    }
}

@Composable
private fun CoachInviteStateCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun CoachInviteModeButton(title: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Text(
        title,
        color = if (selected) Color.White else DSColors.inkGreen,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier.height(34.dp).clip(CircleShape)
            .background(if (selected) DSColors.deepGreen else Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 8.dp),
    )
}

@Composable
@android.annotation.SuppressLint("ModifierParameter")
private fun CoachInvitePrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = DSColors.deepGreen),
    ) {
        icon?.let { Icon(it, null, modifier = Modifier.size(17.dp)) }
        Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = if (icon == null) 0.dp else 7.dp))
    }
}

@Composable
@android.annotation.SuppressLint("ModifierParameter")
private fun CoachInviteSecondaryButton(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DSColors.deepGreen),
        border = androidx.compose.foundation.BorderStroke(1.dp, DSColors.fairway),
    ) {
        Icon(icon, null, modifier = Modifier.size(17.dp))
        Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 7.dp), maxLines = 1)
    }
}

@Composable
private fun CoachInvitePrivacyNote() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
            .border(1.dp, DSColors.listCardBorder, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("学员加入后可以", color = DSColors.inkGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        listOf(
            "获得专属的教学空间",
            "接收教练布置的练习内容",
            "观看教学视频与学习资料",
            "与教练围绕练习交流，记录成长轨迹",
            "邀请码仅可使用一次，过期后需要重新生成",
        ).forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = DSColors.fairway, modifier = Modifier.size(15.dp))
                Text(line, color = DSColors.shadowGray, fontSize = 12.sp)
            }
        }
    }
}

internal fun generateCoachInviteQrBitmap(
    content: String,
    sizePx: Int = 720,
    errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
): Bitmap? = runCatching {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to errorCorrection,
        EncodeHintType.MARGIN to 1,
    )
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    createBitmap(sizePx, sizePx).also { bitmap ->
        for (y in 0 until sizePx) for (x in 0 until sizePx) {
            bitmap[x, y] = if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
        }
    }
}.getOrNull()

internal fun composeCoachInvitePoster(template: Bitmap, qrBitmap: Bitmap): Bitmap {
    val outputScale = 3f
    val width = (template.width * outputScale).toInt()
    val height = (template.height * outputScale).toInt()
    val output = createBitmap(width, height)
    val canvas = Canvas(output)
    canvas.drawBitmap(
        template,
        null,
        RectF(0f, 0f, width.toFloat(), height.toFloat()),
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
    )

    // These coordinates are measured from the same 576x1024 iOS poster template.
    val qrRect = RectF(197f, 356f, 379f, 538f).apply {
        left *= outputScale
        top *= outputScale
        right *= outputScale
        bottom *= outputScale
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = AndroidColor.WHITE
    canvas.drawRect(
        RectF(qrRect.left - 18f, qrRect.top - 18f, qrRect.right + 18f, qrRect.bottom + 18f),
        paint,
    )
    canvas.drawBitmap(qrBitmap, null, qrRect, Paint())

    val side = qrRect.width() * 0.30f
    val badge = RectF(
        qrRect.centerX() - side / 2f,
        qrRect.centerY() - side / 2f,
        qrRect.centerX() + side / 2f,
        qrRect.centerY() + side / 2f,
    )
    paint.color = AndroidColor.WHITE
    canvas.drawRoundRect(badge, side * 0.22f, side * 0.22f, paint)
    val inset = side * 0.08f
    val inner = RectF(badge.left + inset, badge.top + inset, badge.right - inset, badge.bottom - inset)
    paint.color = AndroidColor.rgb(27, 67, 50)
    canvas.drawRoundRect(inner, side * 0.16f, side * 0.16f, paint)
    paint.color = AndroidColor.WHITE
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = inner.width() * 0.34f
    val baseline = inner.centerY() - (paint.ascent() + paint.descent()) / 2f
    canvas.drawText("挥呗", inner.centerX(), baseline, paint)
    return output
}

private fun buildCoachInvitePoster(context: Context, inviteUrl: String): Bitmap {
    val template = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.coach_invite_poster,
        BitmapFactory.Options().apply { inScaled = false },
    ) ?: error("邀请海报底图不可用")
    val qr = generateCoachInviteQrBitmap(
        content = inviteUrl,
        errorCorrection = ErrorCorrectionLevel.H,
    ) ?: error("二维码生成失败")
    return try {
        composeCoachInvitePoster(template, qr)
    } finally {
        template.recycle()
        qr.recycle()
    }
}

private suspend fun saveCoachInvitePoster(context: Context, inviteUrl: String): String = withContext(Dispatchers.IO) {
    var insertedUri: android.net.Uri? = null
    runCatching {
        val poster = buildCoachInvitePoster(context, inviteUrl)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "huibei_coach_invite_poster_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Huibei")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("系统相册暂时不可写")
        insertedUri = uri
        val saved = try {
            resolver.openOutputStream(uri)?.use { poster.compress(Bitmap.CompressFormat.PNG, 100, it) } == true
        } finally {
            poster.recycle()
        }
        if (!saved) {
            resolver.delete(uri, null, null)
            insertedUri = null
            error("二维码写入失败")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        "已保存"
    }.getOrElse {
        insertedUri?.let { uri -> context.contentResolver.delete(uri, null, null) }
        "保存失败"
    }
}

private suspend fun writeCoachInvitePoster(
    context: Context,
    uri: android.net.Uri,
    inviteUrl: String,
): String = withContext(Dispatchers.IO) {
    runCatching {
        val poster = buildCoachInvitePoster(context, inviteUrl)
        val saved = try {
            context.contentResolver.openOutputStream(uri)?.use {
                poster.compress(Bitmap.CompressFormat.PNG, 100, it)
            } == true
        } finally {
            poster.recycle()
        }
        if (!saved) error("二维码写入失败")
        "已保存"
    }.getOrElse { "保存失败" }
}

@Composable
private fun CoachHome(
    snapshot: V2CoachSnapshot,
    error: String?,
    session: V2Session,
    refreshing: Boolean,
    deliveryMessage: String?,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onInvite: () -> Unit,
    onOpenStudents: () -> Unit,
    onOpenStudent: (V2CoachConnection) -> Unit,
    onOpenReview: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenItem: (V2CoachWorkspaceItem) -> Unit,
) {
    val activeConnections = snapshot.connections.filter { it.status == "active" }
    val activeConnectionIds = activeConnections.mapTo(mutableSetOf()) { it.connectionId }
    val activeStates = snapshot.dashboard.connectionStates.filter { it.connectionId in activeConnectionIds }
    val homeItems = snapshot.reviewQueue + snapshot.dashboard.recentItems + snapshot.dashboard.studentPreviewItems
    val priorityItems = coachHomePriorityItems(homeItems, activeConnectionIds)
    val questionCount = priorityItems.count(::isCoachHomeQuestion)
    val newCount = snapshot.dashboard.newByConnection
        .filterKeys { it in activeConnectionIds }.values.sum()
    val sentUnreadCount = activeStates.sumOf { it.pendingViewCount ?: 0 }
    val visibleStudents = coachHomeVisibleConnections(activeConnections, activeStates, priorityItems)
    val statesByConnection = activeStates.associateBy(V2CoachConnectionActivitySnapshot::connectionId)
    val latestItemsByConnection = latestStudentSpaceItemsByConnection(homeItems)
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("coach-home-scroll"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        item {
            V2CoachHomeHero(
                session = session,
                questionCount = questionCount,
                newCount = newCount,
                sentUnreadCount = sentUnreadCount,
                onNotice = { if (questionCount > 0 || newCount > 0) onOpenReview() else onSettings() },
                onOpenReview = onOpenReview,
                onOpenStudents = onOpenStudents,
            )
        }
        deliveryMessage?.let { message -> item { Box(Modifier.padding(horizontal = 18.dp)) { NoticeCard(message) } } }
        error?.let { message -> item { Box(Modifier.padding(horizontal = 18.dp)) { NoticeCard(message) } } }
        item {
            V2CoachHomeSectionHeader(
                icon = Icons.Filled.Bolt,
                title = "优先关注",
                actionTitle = "查看全部",
                onAction = onOpenReview,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
        }
        if (priorityItems.isEmpty()) {
            item {
                V2CoachHomePriorityEmpty(
                    hasStudents = activeConnections.isNotEmpty(),
                    onInvite = onInvite,
                    onOpenStudents = onOpenStudents,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }
        } else {
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(priorityItems, key = { it.itemId }) { priority ->
                        CoachHomePriorityCard(
                            item = priority,
                            student = snapshot.connections.firstOrNull {
                                it.connectionId == priority.connectionId
                            }?.student,
                            onOpen = { onOpenItem(priority) },
                        )
                    }
                }
            }
        }
        item {
            V2CoachHomeSectionHeader(
                icon = Icons.Filled.Groups,
                title = "我的学员",
                subtitle = "关注学员的练习与成长",
                actionTitle = "全部学员".takeIf { activeConnections.isNotEmpty() },
                onAction = onOpenStudents,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
        }
        when {
            activeConnections.isEmpty() -> item {
                V2CoachHomeInviteFirstStudent(onInvite, Modifier.padding(horizontal = 18.dp))
            }
            else -> {
                items(visibleStudents.chunked(2), key = { row -> row.joinToString("|") { it.connectionId } }) { row ->
                    Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { connection ->
                            CoachHomeStudentCard(
                                connection = connection,
                                activity = statesByConnection[connection.connectionId],
                                latestItem = latestItemsByConnection[connection.connectionId],
                                modifier = Modifier.weight(1f),
                                onOpen = { onOpenStudent(connection) },
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        if (snapshot.library.isNotEmpty()) {
            item {
                V2CoachHomeSectionHeader(
                    icon = Icons.Filled.VideoLibrary,
                    title = "最近分享的素材",
                    subtitle = "用专业内容，陪伴学员持续进步",
                    actionTitle = "查看全部",
                    onAction = onOpenLibrary,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(snapshot.library.take(3), key = { it.assetId }) { asset ->
                        CoachHomeLibraryCard(asset, onOpenLibrary)
                    }
                }
            }
        }
      }
    }
}

@Composable
private fun CoachHomePriorityCard(
    item: V2CoachWorkspaceItem,
    student: com.mygolfswing.app.data.coach.V2CoachUserSummary?,
    onOpen: () -> Unit,
) {
    val question = isCoachHomeQuestion(item)
    val kind = when {
        question -> "提问"
        item.resolvedKind == "coach_post" -> "学习分享"
        else -> "新练习"
    }
    val preview = V2CoachWorkspaceItem.visibleCopy(item.latestReplyText)
        ?: V2CoachWorkspaceItem.visibleCopy(item.text)
        ?: V2CoachWorkspaceItem.visibleCopy(item.title)
        ?: "进入教学空间查看详细内容"
    Column(
        Modifier.size(width = 286.dp, height = 154.dp)
            .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = DSColors.deepGreen.copy(alpha = 0.07f))
            .clip(RoundedCornerShape(18.dp)).background(Color.White)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            V2UserAvatar(
                student?.avatar,
                Modifier.size(32.dp),
                fallbackFontSize = 12.sp,
                placeholderIconSize = 20.dp,
                fallbackText = student?.nickname ?: "学员",
                fallbackColor = DSColors.sandBeige,
            )
            Text(student?.nickname ?: "学员", color = DSColors.deepGreen, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                kind,
                color = when {
                    question -> Color(0xFFC07A28)
                    item.resolvedKind == "coach_post" -> Color(0xFF71877B)
                    else -> DSColors.fairway
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).background(DSColors.mistGreen)
                    .padding(horizontal = 7.dp, vertical = 4.dp),
            )
            Text(
                coachStudentRelativeTime(item.latestReplyAt ?: item.latestActivityAt ?: item.createdAt),
                color = DSColors.shadowGray,
                fontSize = 9.sp,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    if (question) "“$preview”" else preview,
                    color = DSColors.deepGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (item.resolvedKind == "coach_post") "来自学习分享" else "来自练习",
                    color = DSColors.shadowGray,
                    fontSize = 10.sp,
                )
                Text("查看内容 ›", color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (item.latestActivityHasVideoContent) {
                Box(
                    Modifier.size(width = 88.dp, height = 66.dp).clip(RoundedCornerShape(12.dp))
                        .background(DSColors.deepGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    item.latestActivityCoverUrl?.let { cover ->
                        AsyncImage(
                            model = cover,
                            contentDescription = "优先内容视频",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text("▶", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun CoachHomeStudentCard(
    connection: V2CoachConnection,
    activity: V2CoachConnectionActivitySnapshot?,
    latestItem: V2CoachWorkspaceItem?,
    modifier: Modifier,
    onOpen: () -> Unit,
) {
    val weeklyCount = activity?.weeklyPracticeCount ?: 0
    val pendingViewCount = activity?.pendingViewCount ?: 0
    val viewedCount = activity?.viewedTeachingCount ?: 0
    val recent = coachHomeStudentRecentSummary(activity, latestItem, connection.isCoachingProfileComplete)
    val recentTime = coachStudentRelativeTime(activity?.lastPracticeAt ?: activity?.latestActivityAt)
        .replace(" ", "")
    val activityLine = listOf(recentTime, recent).filter(String::isNotBlank).joinToString("  ")
    val status = coachHomeStudentStatusText(activity?.lastPracticeAt, weeklyCount)
    val hasVideoActivity = activity?.latestHasVideo == true || latestItem?.latestActivityHasVideoContent == true
    Column(
        modifier.shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = DSColors.deepGreen.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(18.dp)).background(Color.White)
            .border(1.dp, DSColors.borderLine, RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            V2UserAvatar(
                connection.student?.avatar,
                Modifier.size(36.dp),
                fallbackFontSize = 14.sp,
                placeholderIconSize = 22.dp,
                fallbackText = connection.student?.nickname ?: "学员",
                fallbackColor = DSColors.sandBeige,
            )
            Text(
                connection.student?.nickname ?: "学员",
                color = DSColors.deepGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            status?.let { label ->
                val tint = if (label == "活跃") DSColors.fairway else V2CoachPalette.gold
                Text(
                    label,
                    color = tint,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).background(tint.copy(alpha = 0.12f))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          connection.trainingStage?.trim()?.takeIf(String::isNotEmpty)?.let { stage ->
            Text(
                stage,
                color = DSColors.fairway,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).background(DSColors.mistGreen)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            )
          }
          Text(
              connection.resolvedTrainingFocus?.let { "当前重点：$it" } ?: "训练重点待确认",
              color = if (connection.resolvedTrainingFocus == null) V2CoachPalette.gold else V2CoachPalette.textSecondary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoachHomeStudentStat(
                icon = Icons.Filled.SportsGolf,
                title = "本周练习",
                value = connection.weeklyPracticeGoal?.let { "$weeklyCount/$it" } ?: weeklyCount.toString(),
                modifier = Modifier.weight(1f),
            )
            CoachHomeStudentStat(
                icon = if (pendingViewCount > 0) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                title = if (pendingViewCount > 0) "待查看" else "已查看",
                value = (if (pendingViewCount > 0) pendingViewCount else viewedCount).toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = when {
                    recent.contains("收藏") -> Icons.Filled.Bookmark
                    recent.contains("视频") || hasVideoActivity -> Icons.Filled.VideoLibrary
                    else -> Icons.Filled.ChatBubble
                },
                contentDescription = null,
                tint = DSColors.fairway,
                modifier = Modifier.padding(top = 1.dp).size(10.dp),
            )
            Text(
                activityLine,
                color = V2CoachPalette.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

internal fun coachHomeStudentStatusText(
    lastPracticeAt: String?,
    weeklyPracticeCount: Int,
    now: Instant = Instant.now(),
): String? {
    if (weeklyPracticeCount <= 0) return null
    val last = lastPracticeAt?.let { value ->
        runCatching { Instant.parse(value) }
            .recoverCatching { OffsetDateTime.parse(value).toInstant() }
            .getOrNull()
    }
    return if (last != null && java.time.Duration.between(last, now).seconds < 172_800) "活跃" else "本周活跃"
}

@Composable
private fun CoachHomeStudentStat(icon: ImageVector, title: String, value: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(DSColors.mistGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(11.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(title, color = V2CoachPalette.mutedText, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(value, color = DSColors.inkGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("条", color = V2CoachPalette.mutedText, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

internal fun coachHomeLibraryDurationLabel(durationSeconds: Double?): String {
    val seconds = durationSeconds
        ?.takeIf { it.isFinite() && it > 0 }
        ?.roundToInt()
        ?.coerceAtLeast(0)
        ?: return "视频素材"
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
internal fun CoachHomeLibraryCard(
    asset: V2CoachLibraryAsset,
    onOpen: () -> Unit,
    loadAsset: suspend (String) -> V2CoachLibraryAsset? = { assetId ->
        ServiceLocator.coachWorkspaceRepo.coachAsset(assetId).getOrNull()
    },
) {
    var displayedAsset by remember(asset.assetId) { mutableStateOf(asset) }
    var coverRecoveryRequest by remember(asset.assetId) { mutableIntStateOf(0) }

    LaunchedEffect(asset) {
        displayedAsset = asset
    }
    LaunchedEffect(
        displayedAsset.assetId,
        displayedAsset.coverUrl,
        displayedAsset.videoUrl,
        coverRecoveryRequest,
    ) {
        if (
            (displayedAsset.coverUrl.isNullOrBlank() || coverRecoveryRequest > 0) &&
            displayedAsset.videoUrl.isNullOrBlank()
        ) {
            loadAsset(displayedAsset.assetId)?.let { displayedAsset = it }
        }
    }

    Column(
        Modifier.widthIn(min = 148.dp, max = 148.dp)
            .testTag("coach-home-library-card-${displayedAsset.assetId}")
            .clickable(onClick = onOpen),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(width = 148.dp, height = 88.dp)
                .clip(RoundedCornerShape(12.dp))
                .testTag("coach-home-library-cover-${displayedAsset.assetId}"),
            contentAlignment = Alignment.Center,
        ) {
            V2CoachMediaCover(
                asset = displayedAsset,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onRemoteLoadFailed = { coverRecoveryRequest += 1 },
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                    ),
                ),
            )
            Text(
                coachHomeLibraryDurationLabel(displayedAsset.durationSeconds),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomStart).padding(7.dp)
                    .clip(RoundedCornerShape(5.dp)).background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
        Text(
            displayedAsset.title.ifBlank { "教学素材" },
            color = DSColors.deepGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.size(width = 148.dp, height = 34.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            displayedAsset.tags.take(2).forEach { tag ->
                Text(
                    tag,
                    color = DSColors.fairway,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.clip(CircleShape).background(DSColors.mistGreen)
                        .height(20.dp).padding(horizontal = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun CoachStudents(
    connections: List<V2CoachConnection>,
    dashboard: V2CoachDashboard,
    contentItems: List<V2CoachWorkspaceItem>,
    ownerUserId: String,
    deliveryJobs: List<V2CoachDeliveryJob>,
    lessonScope: V2CoachLessonScope,
    onLessonScopeChange: (V2CoachLessonScope) -> Unit,
    onInvite: () -> Unit,
    onEditStudent: (V2CoachConnection) -> Unit,
    onOpenStudent: (V2CoachConnection) -> Unit,
) {
    val context = LocalContext.current
    val draftStore = remember(context) { V2WorkspaceDraftStore(context) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedGoalName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAgeName by rememberSaveable { mutableStateOf<String?>(null) }
    var onlyPending by rememberSaveable { mutableStateOf(false) }
    var showsHistoricalConnections by rememberSaveable { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showGoalMenu by remember { mutableStateOf(false) }
    var showAgeMenu by remember { mutableStateOf(false) }
    val selectedGoal = V2CoachTrainingGoal.entries.firstOrNull { it.name == selectedGoalName }
    val selectedAge = V2CoachStudentAge.entries.firstOrNull { it.name == selectedAgeName }
    val deliveryJobsByConnection = deliveryJobs.groupBy(V2CoachDeliveryJob::connectionId)
    val draftConnectionIds = connections.mapNotNullTo(mutableSetOf()) { connection ->
        connection.connectionId.takeIf { connectionId ->
            deliveryJobsByConnection[connectionId].orEmpty().any { !it.lastError.isNullOrBlank() } ||
                (ownerUserId.isNotBlank() && draftStore.hasConnectionDraft(ownerUserId, connectionId))
        }
    }
    val pendingConnectionIds = connections.mapNotNullTo(mutableSetOf()) { connection ->
        connection.connectionId.takeIf { connection.hasCoachPendingWork(dashboard, draftConnectionIds) }
    }
    val displayedConnections = sortCoachStudentConnections(connections.filter {
        it.matchesCoachFilters(lessonScope, selectedGoal, selectedAge, query = query) &&
            (!onlyPending || it.connectionId in pendingConnectionIds)
    }, dashboard, draftConnectionIds)
    val activeDisplayedConnections = displayedConnections.filter { it.status == "active" }
    val historicalDisplayedConnections = displayedConnections.filter { it.status != "active" }
    val previewCandidates = dashboard.studentPreviewItems + dashboard.recentItems
    val previewItemsByConnection = latestStudentSpaceItemsByConnection(previewCandidates)
    val previewItemsById = previewCandidates.associateBy(V2CoachWorkspaceItem::itemId)
    val activeFilterCount = listOf(selectedGoal, selectedAge).count { it != null } +
        (if (onlyPending) 1 else 0) +
        (if (lessonScope == V2CoachLessonScope.ALL) 0 else 1)
    val hasAnyFilter = activeFilterCount > 0 || query.isNotBlank()

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            V2PageArtworkBackground(
                artwork = V2PageArtwork.FAIRWAY,
                tone = V2PageArtworkTone.FADE_TO_WARM_BOTTOM,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("我的学员", color = DSColors.deepGreen, fontSize = 25.sp, fontWeight = FontWeight.Black)
                            Text("共 ${connections.count { it.status == "active" }} 位学员", color = DSColors.shadowGray, fontSize = 13.sp)
                        }
                        Icon(Icons.Filled.Add, "添加学员", tint = Color.White, modifier = Modifier.size(40.dp).clip(CircleShape).background(DSColors.fairway).clickable(onClick = onInvite).padding(9.dp))
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("搜索学员姓名 / 备注或账号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LessonScopeBar(lessonScope, onLessonScopeChange)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box {
                            CoachStudentFilterChip(
                                title = selectedGoal?.title ?: "训练目标",
                                selected = selectedGoal != null,
                                onClick = { showGoalMenu = true },
                            )
                            DropdownMenu(expanded = showGoalMenu, onDismissRequest = { showGoalMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("全部目标") },
                                    onClick = { selectedGoalName = null; showGoalMenu = false },
                                )
                                V2CoachTrainingGoal.entries.forEach { goal ->
                                    DropdownMenuItem(
                                        text = { Text(if (selectedGoal == goal) "✓ ${goal.title}" else goal.title) },
                                        onClick = { selectedGoalName = goal.name; showGoalMenu = false },
                                    )
                                }
                            }
                        }
                        Box {
                            CoachStudentFilterChip(
                                title = selectedAge?.title ?: "年龄",
                                selected = selectedAge != null,
                                onClick = { showAgeMenu = true },
                            )
                            DropdownMenu(expanded = showAgeMenu, onDismissRequest = { showAgeMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("全部年龄") },
                                    onClick = { selectedAgeName = null; showAgeMenu = false },
                                )
                                V2CoachStudentAge.entries.forEach { age ->
                                    DropdownMenuItem(
                                        text = { Text(if (selectedAge == age) "✓ ${age.title}" else age.title) },
                                        onClick = { selectedAgeName = age.name; showAgeMenu = false },
                                    )
                                }
                            }
                        }
                        CoachStudentFilterChip(
                            title = "更多筛选",
                            selected = onlyPending,
                            onClick = { showFilters = true },
                        )
                        if (activeFilterCount > 0) {
                            Text("已筛选 $activeFilterCount 项", color = DSColors.fairway, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "显示 ${displayedConnections.size} / ${connections.size} 位学员",
                            color = DSColors.shadowGray,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (hasAnyFilter) {
                            Text(
                                "重置",
                                color = DSColors.fairway,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    query = ""
                                    selectedGoalName = null
                                    selectedAgeName = null
                                    onlyPending = false
                                    onLessonScopeChange(V2CoachLessonScope.ALL)
                                }.padding(8.dp),
                            )
                        }
                    }
                }
            }
        }
        if (displayedConnections.isEmpty()) item {
            EmptyCard(
                when {
                    connections.isEmpty() -> "还没有学员，点击右上角邀请第一位学员"
                    onlyPending && query.isBlank() -> "太棒了，当前没有待处理事项"
                    else -> "没有符合当前范围的学员，请调整搜索或筛选条件"
                },
            )
        }
        items(activeDisplayedConnections, key = { it.connectionId }) { connection ->
            CoachStudentConnectionCard(
                connection = connection,
                dashboard = dashboard,
                ownerUserId = ownerUserId,
                draftStore = draftStore,
                deliveryJobsByConnection = deliveryJobsByConnection,
                draftConnectionIds = draftConnectionIds,
                previewItemsById = previewItemsById,
                previewItemsByConnection = previewItemsByConnection,
                contentItems = contentItems,
                onEditStudent = onEditStudent,
                onOpenStudent = onOpenStudent,
            )
        }
        if (historicalDisplayedConnections.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(Color.White).clickable {
                                showsHistoricalConnections = !showsHistoricalConnections
                            }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("◷", color = DSColors.deepGreen, fontSize = 16.sp)
                        Text(
                            "历史连接（${historicalDisplayedConnections.size}）",
                            color = DSColors.deepGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(if (showsHistoricalConnections) "收起" else "展开", color = DSColors.fairway, fontSize = 11.sp)
                    }
                    Text(
                        "历史教学记录会继续保留，空间仅可查看",
                        color = DSColors.shadowGray,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                }
            }
            if (showsHistoricalConnections) {
                items(historicalDisplayedConnections, key = { it.connectionId }) { connection ->
                    CoachStudentConnectionCard(
                        connection = connection,
                        dashboard = dashboard,
                        ownerUserId = ownerUserId,
                        draftStore = draftStore,
                        deliveryJobsByConnection = deliveryJobsByConnection,
                        draftConnectionIds = draftConnectionIds,
                        previewItemsById = previewItemsById,
                        previewItemsByConnection = previewItemsByConnection,
                        contentItems = contentItems,
                        onEditStudent = onEditStudent,
                        onOpenStudent = onOpenStudent,
                    )
                }
            }
        }
    }

    if (showFilters) {
        CoachStudentFilterDialog(
            initialScope = lessonScope,
            initialGoalName = selectedGoalName,
            initialAgeName = selectedAgeName,
            initialOnlyPending = onlyPending,
            resultCount = { scope, goalName, ageName, pendingOnly ->
                val goal = V2CoachTrainingGoal.entries.firstOrNull { it.name == goalName }
                val age = V2CoachStudentAge.entries.firstOrNull { it.name == ageName }
                connections.count { connection ->
                    connection.matchesCoachFilters(scope, goal, age, query = "") &&
                        (!pendingOnly || connection.connectionId in pendingConnectionIds)
                }
            },
            onApply = { scope, goalName, ageName, pendingOnly ->
                onLessonScopeChange(scope)
                selectedGoalName = goalName
                selectedAgeName = ageName
                onlyPending = pendingOnly
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun CoachStudentFilterChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        "$title ⌄",
        color = if (selected) DSColors.fairway else DSColors.inkGreen,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier.height(30.dp).clip(CircleShape)
            .background(if (selected) DSColors.mistGreen else DSColors.sandBeige.copy(alpha = 0.85f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

@Composable
private fun CoachStudentFilterDialog(
    initialScope: V2CoachLessonScope,
    initialGoalName: String?,
    initialAgeName: String?,
    initialOnlyPending: Boolean,
    resultCount: (V2CoachLessonScope, String?, String?, Boolean) -> Int,
    onApply: (V2CoachLessonScope, String?, String?, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var draftScopeName by rememberSaveable { mutableStateOf(initialScope.name) }
    var draftGoalName by rememberSaveable { mutableStateOf(initialGoalName) }
    var draftAgeName by rememberSaveable { mutableStateOf(initialAgeName) }
    var draftOnlyPending by rememberSaveable { mutableStateOf(initialOnlyPending) }
    val draftScope = V2CoachLessonScope.entries.firstOrNull { it.name == draftScopeName }
        ?: V2CoachLessonScope.ALL
    val count = resultCount(draftScope, draftGoalName, draftAgeName, draftOnlyPending)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f))) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = 720.dp)
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(DSColors.warmWhite),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "关闭筛选",
                        tint = DSColors.inkGreen,
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White)
                            .clickable(onClick = onDismiss).padding(8.dp),
                    )
                    Text(
                        "筛选学员",
                        color = DSColors.inkGreen,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "重置",
                        color = DSColors.fairway,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            draftScopeName = V2CoachLessonScope.ALL.name
                            draftGoalName = null
                            draftAgeName = null
                            draftOnlyPending = false
                        }.padding(6.dp),
                    )
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    FilterChoiceGroup(
                        title = "授课方式",
                        choices = V2CoachLessonScope.entries.map { it.name to it.title },
                        selected = draftScopeName,
                        onSelect = { draftScopeName = it ?: V2CoachLessonScope.ALL.name },
                    )
                    FilterChoiceGroup(
                        title = "训练目标",
                        choices = listOf(null to "不限") + V2CoachTrainingGoal.entries.map { it.name to it.title },
                        selected = draftGoalName,
                        onSelect = { draftGoalName = it },
                    )
                    FilterChoiceGroup(
                        title = "年龄范围",
                        choices = listOf(null to "不限") + V2CoachStudentAge.entries.map { it.name to it.title },
                        selected = draftAgeName,
                        onSelect = { draftAgeName = it },
                    )
                    FilterChoiceGroup(
                        title = "待处理",
                        choices = listOf(null to "不限", "pending" to "只看待处理"),
                        selected = if (draftOnlyPending) "pending" else null,
                        onSelect = { draftOnlyPending = it == "pending" },
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Button(
                    onClick = {
                        onApply(draftScope, draftGoalName, draftAgeName, draftOnlyPending)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = DSColors.deepGreen),
                ) {
                    Text("确认筛选（$count）", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CoachStudentConnectionCard(
    connection: V2CoachConnection,
    dashboard: V2CoachDashboard,
    ownerUserId: String,
    draftStore: V2WorkspaceDraftStore,
    deliveryJobsByConnection: Map<String, List<V2CoachDeliveryJob>>,
    draftConnectionIds: Set<String>,
    previewItemsById: Map<String, V2CoachWorkspaceItem>,
    previewItemsByConnection: Map<String, V2CoachWorkspaceItem>,
    contentItems: List<V2CoachWorkspaceItem>,
    onEditStudent: (V2CoachConnection) -> Unit,
    onOpenStudent: (V2CoachConnection) -> Unit,
) {
    val activity = dashboard.connectionStates.firstOrNull {
        it.connectionId == connection.connectionId
    }
    val resolvedNewCount = activity?.newCount
        ?: dashboard.newByConnection[connection.connectionId]
        ?: 0
    val resolvedPendingCount = activity?.pendingCount
        ?: dashboard.pendingByConnection[connection.connectionId]
        ?: 0
    val effectiveActivity = activity ?: V2CoachConnectionActivitySnapshot(
        connectionId = connection.connectionId,
        state = "",
        newCount = resolvedNewCount,
        pendingCount = resolvedPendingCount,
    )
    val priorityItemId = activity?.priorityItemId
        ?: dashboard.pendingItemIdByConnection[connection.connectionId]
    val previewItem = priorityItemId?.let(previewItemsById::get)
        ?: previewItemsByConnection[connection.connectionId]
    val connectionDeliveryJobs = deliveryJobsByConnection[connection.connectionId].orEmpty()
    val failedQueuedCount = connectionDeliveryJobs.count { !it.lastError.isNullOrBlank() }
    val hasDraft = connection.connectionId in draftConnectionIds
    val compactStatus = coachStudentCompactStatus(
        connection = connection,
        newCount = resolvedNewCount,
        pendingCount = resolvedPendingCount,
        hasDraft = hasDraft,
    )
    val localDraftSavedAt = if (ownerUserId.isNotBlank()) {
        draftStore.latestConnectionDraftSavedAtMillis(ownerUserId, connection.connectionId)
    } else {
        null
    }
    val queuedAt = connectionDeliveryJobs.maxOfOrNull(V2CoachDeliveryJob::createdAtMillis)
    val activityTime = when {
        hasDraft && localDraftSavedAt != null -> Instant.ofEpochMilli(localDraftSavedAt).toString()
        hasDraft && queuedAt != null -> Instant.ofEpochMilli(queuedAt).toString()
        else -> activity?.latestActivityAt ?: previewItem?.latestReplyAt
            ?: previewItem?.latestActivityAt ?: connection.lastActivityAt
    }
    val counts = workspaceContentCounts(contentItems, connection.connectionId)
    val activityLine = coachStudentActivityLine(
        connection = connection,
        activity = effectiveActivity,
        priorityItem = previewItem,
        hasDraft = hasDraft,
        failedQueuedCount = failedQueuedCount,
    )
    val recentUpdate = listOf(coachStudentRelativeTime(activityTime), activityLine)
        .filter(String::isNotBlank)
        .joinToString(" · ")
        .ifBlank { "暂无新动态" }
    val focus = connection.resolvedTrainingFocus?.trim()?.takeIf(String::isNotEmpty) ?: "待确认"
    val cardShape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier.fillMaxWidth()
            .shadow(7.dp, cardShape, ambientColor = DSColors.deepGreen.copy(alpha = 0.045f))
            .clip(cardShape).background(Color.White)
            .border(1.dp, DSColors.fairway.copy(alpha = 0.16f), cardShape)
            .combinedClickable(
                onClick = { onOpenStudent(connection) },
                onLongClick = if (connection.status == "active") {
                    { onEditStudent(connection) }
                } else null,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        V2UserAvatar(
            avatar = connection.student?.avatar,
            modifier = Modifier.size(42.dp),
            fallbackFontSize = 16.sp,
            placeholderIconSize = 25.dp,
            fallbackText = connection.student?.nickname ?: "学员",
            fallbackColor = DSColors.sandBeige,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    connection.student?.nickname ?: "学员",
                    color = DSColors.deepGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    connection.resolvedLessonMode.title,
                    color = DSColors.fairway,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).background(DSColors.mistGreen)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
                Spacer(Modifier.weight(1f))
                if (connection.pinned) {
                    Text(
                        "★ 重点学员",
                        color = DSColors.trophyGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(CircleShape).background(Color(0xFFF5E8C9).copy(alpha = 0.72f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
            Text(
                "当前重点：$focus",
                color = if (focus == "待确认") DSColors.shadowGray else DSColors.inkGreen,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "⛳ 在练 ${counts.practice} 条    ▷ 在看 ${counts.watching} 条",
                color = DSColors.shadowGray,
                fontSize = 10.sp,
                maxLines = 1,
            )
            Text(
                "◷ 最近更新：$recentUpdate",
                color = compactStatus?.second ?: DSColors.shadowGray,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "进入教学空间",
            tint = DSColors.shadowGray,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun coachStudentCompactStatus(
    connection: V2CoachConnection,
    newCount: Int,
    pendingCount: Int,
    hasDraft: Boolean,
): Pair<String, Color>? = when {
    connection.status != "active" -> "历史连接" to DSColors.shadowGray
    hasDraft -> "有未发送内容" to DSColors.trophyGold
    newCount > 0 -> "${newCount}条新互动" to Color(0xFFD95C4F)
    pendingCount > 0 -> "${pendingCount}条待处理" to Color(0xFFC07A28)
    !connection.isCoachingProfileComplete -> "教学档案待完善" to DSColors.fairway
    else -> null
}

private fun coachStudentEmphasis(connection: V2CoachConnection): Pair<String, String>? {
    val focus = connection.resolvedTrainingFocus?.trim()?.takeIf(String::isNotEmpty)
    val goal = connection.primaryGoal?.trim()?.takeIf(String::isNotEmpty)
    return when (connection.resolvedLessonMode) {
        V2CoachLessonMode.FIXED -> focus?.let { "训练重点" to it }
        V2CoachLessonMode.FLEXIBLE -> (goal ?: focus)?.let { "当前目标" to it }
        V2CoachLessonMode.SINGLE -> (goal ?: focus)?.let { "本次目标" to it }
    }
}


private fun coachConnectionActivityLabel(state: String, overduePendingCount: Int): String = when {
    overduePendingCount > 0 -> "$overduePendingCount 条已超24小时"
    state == "new" -> "有新练习"
    state == "viewed" -> "待回复"
    state == "student_follow_up" -> "学员有追问"
    state == "coach_sent_unread" -> "等待学员查看"
    state == "coach_sent_viewed" -> "学员已查看"
    else -> "暂无待办"
}

@Composable
private fun LessonScopeBar(
    selected: V2CoachLessonScope,
    onSelected: (V2CoachLessonScope) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFE4EEE8)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        V2CoachLessonScope.entries.forEach { scope ->
            Text(
                scope.title,
                color = if (selected == scope) Color.White else DSColors.shadowGray,
                fontWeight = if (selected == scope) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .background(if (selected == scope) DSColors.fairway else Color.Transparent)
                    .clickable { onSelected(scope) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun FilterChoiceGroup(
    title: String,
    choices: List<Pair<String?, String>>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = DSColors.deepGreen, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            choices.forEach { (value, label) ->
                Text(
                    label,
                    color = if (selected == value) Color.White else DSColors.deepGreen,
                    fontSize = 12.sp,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                        .background(if (selected == value) DSColors.fairway else Color(0xFFE4EEE8))
                        .clickable { onSelect(value) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CoachReview(
    items: List<V2CoachWorkspaceItem>,
    initialNextCursor: String?,
    initialTotal: Int?,
    initialVersion: String?,
    pendingWorkCount: Int,
    initialRespondedCount: Int,
    initialError: String?,
    connections: List<V2CoachConnection>,
    localReplyDraftItemIds: Set<String>,
    localReplyDraftConnectionByItemId: Map<String, String>,
    draftCandidateItems: List<V2CoachWorkspaceItem>,
    deliveryJobs: List<V2CoachDeliveryJob>,
    onReload: () -> Unit,
    onOpenStudents: () -> Unit,
    onOpenItem: (V2CoachWorkspaceItem) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val reviewNetworkConnected = rememberQueueNetworkConnected()
    var previousReviewNetworkConnected by remember { mutableStateOf<Boolean?>(null) }
    var selectedFilter by rememberSaveable { mutableStateOf("pending") }
    var selectedFocusName by rememberSaveable { mutableStateOf(V2CoachReviewFocus.ALL.name) }
    var pendingItems by remember { mutableStateOf(items) }
    var pendingNextCursor by remember { mutableStateOf(initialNextCursor) }
    var pendingTotal by remember { mutableStateOf(initialTotal) }
    var pendingVersion by remember { mutableStateOf(initialVersion) }
    var respondedItems by remember { mutableStateOf<List<V2CoachWorkspaceItem>>(emptyList()) }
    var respondedNextCursor by remember { mutableStateOf<String?>(null) }
    var respondedTotal by remember { mutableStateOf<Int?>(null) }
    var respondedVersion by remember { mutableStateOf<String?>(null) }
    var respondedLoaded by remember { mutableStateOf(false) }
    var respondedLoading by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(initialError) }
    var reviewRefreshing by remember { mutableStateOf(false) }
    var restoredDraftItems by remember { mutableStateOf<List<V2CoachWorkspaceItem>>(emptyList()) }
    var restoredDraftItemIds by remember { mutableStateOf(emptySet<String>()) }
    val scope = rememberCoroutineScope()
    val selectedFocus = V2CoachReviewFocus.entries.firstOrNull { it.name == selectedFocusName }
        ?: V2CoachReviewFocus.ALL

    LaunchedEffect(items, initialNextCursor, initialTotal, initialVersion) {
        pendingItems = mergeWorkspaceFeedPage(pendingItems, items, firstPage = true)
        pendingNextCursor = initialNextCursor
        pendingTotal = initialTotal
        pendingVersion = initialVersion
    }
    LaunchedEffect(initialError) {
        if (initialError != null) loadError = initialError
    }

    fun loadResponded() {
        if (respondedLoading) return
        respondedLoading = true
        loadError = null
        scope.launch {
            runCatching { ServiceLocator.coachWorkspaceRepo.reviewQueuePage("responded") }
                .onSuccess { page ->
                    respondedItems = page.values
                    respondedNextCursor = page.nextCursor
                    respondedTotal = page.total
                    respondedVersion = page.version
                    respondedLoaded = true
                }
                .onFailure { loadError = coachThrowableError("已回复内容暂时无法加载，请稍后重试", it) }
            respondedLoading = false
        }
    }

    fun loadMore() {
        if (loadingMore) return
        val filter = selectedFilter
        val cursor = if (filter == "pending") pendingNextCursor else respondedNextCursor
        if (cursor == null) return
        loadingMore = true
        loadError = null
        scope.launch {
            runCatching {
                ServiceLocator.coachWorkspaceRepo.reviewQueuePage(filter = filter, cursor = cursor)
            }.onSuccess { page ->
                if (filter == "pending") {
                    pendingItems = mergeWorkspaceFeedPage(pendingItems, page.values, firstPage = false)
                    pendingNextCursor = page.nextCursor
                    page.total?.let { pendingTotal = it }
                } else {
                    respondedItems = mergeWorkspaceFeedPage(respondedItems, page.values, firstPage = false)
                    respondedNextCursor = page.nextCursor
                    page.total?.let { respondedTotal = it }
                }
            }.onFailure { loadError = coachThrowableError("更多点评内容暂时无法加载，请稍后重试", it) }
            loadingMore = false
        }
    }

    fun retryCurrentReview() {
        if (respondedLoading) return
        val filter = selectedFilter
        respondedLoading = true
        reviewRefreshing = true
        loadError = null
        scope.launch {
            runCatching { ServiceLocator.coachWorkspaceRepo.reviewQueuePage(filter) }
                .onSuccess { page ->
                    if (filter == "pending") {
                        pendingItems = mergeWorkspaceFeedPage(pendingItems, page.values, firstPage = true)
                        pendingNextCursor = page.nextCursor
                        pendingTotal = page.total
                        pendingVersion = page.version
                    } else {
                        respondedItems = page.values
                        respondedNextCursor = page.nextCursor
                        respondedTotal = page.total
                        respondedVersion = page.version
                        respondedLoaded = true
                    }
                }
                .onFailure { loadError = coachThrowableError("点评内容暂时无法同步，请稍后重试", it) }
            respondedLoading = false
            reviewRefreshing = false
        }
    }

    LaunchedEffect(selectedFilter) {
        if (selectedFilter == "responded") {
            selectedFocusName = V2CoachReviewFocus.ALL.name
            if (!respondedLoaded) loadResponded()
        }
    }
    LaunchedEffect(reviewNetworkConnected) {
        val wasConnected = previousReviewNetworkConnected
        previousReviewNetworkConnected = reviewNetworkConnected
        if (shouldRefreshCoachReviewAfterReconnect(wasConnected, reviewNetworkConnected)) {
            retryCurrentReview()
            onReload()
        }
    }
    LaunchedEffect(localReplyDraftConnectionByItemId, draftCandidateItems) {
        val activeDraftIds = localReplyDraftConnectionByItemId.keys
        val seeded = draftCandidateItems.filter { it.itemId in activeDraftIds }
        restoredDraftItems = (restoredDraftItems.filter { it.itemId in activeDraftIds } + seeded)
            .distinctBy(V2CoachWorkspaceItem::itemId)
        restoredDraftItemIds = restoredDraftItemIds.intersect(activeDraftIds)
        localReplyDraftConnectionByItemId.forEach { (itemId, connectionId) ->
            if (restoredDraftItems.any { it.itemId == itemId } || itemId in restoredDraftItemIds) return@forEach
            restoredDraftItemIds = restoredDraftItemIds + itemId
            ServiceLocator.coachWorkspaceRepo.workspaceThread(connectionId, itemId)
                .onSuccess { thread ->
                    restoredDraftItems = (restoredDraftItems + thread.item)
                        .distinctBy(V2CoachWorkspaceItem::itemId)
                }
                .onFailure { throwable ->
                    loadError = coachThrowableError("草稿对应的交流暂时无法恢复，请稍后重试", throwable)
                }
        }
    }
    LaunchedEffect(lifecycleOwner, selectedFilter) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(60_000)
                val filter = selectedFilter
                val version = if (filter == "pending") pendingVersion else respondedVersion
                runCatching {
                    ServiceLocator.coachWorkspaceRepo.reviewQueuePage(
                        filter = filter,
                        knownVersion = version,
                    )
                }.onSuccess { page ->
                    if (!page.unchanged) {
                        if (filter == "pending") {
                            pendingItems = mergeWorkspaceFeedPage(pendingItems, page.values, firstPage = true)
                            pendingNextCursor = page.nextCursor
                            pendingTotal = page.total
                            pendingVersion = page.version
                        } else {
                            respondedItems = mergeWorkspaceFeedPage(respondedItems, page.values, firstPage = true)
                            respondedNextCursor = page.nextCursor
                            respondedTotal = page.total
                            respondedVersion = page.version
                            respondedLoaded = true
                        }
                    } else if (filter == "pending") {
                        pendingVersion = page.version ?: pendingVersion
                    } else {
                        respondedVersion = page.version ?: respondedVersion
                    }
                }.onFailure { loadError = coachThrowableError("点评状态暂时无法同步，请稍后重试", it) }
            }
        }
    }
    val shownItems = if (selectedFilter == "pending") {
        coachPendingReviewItems(
            queueItems = pendingItems,
            draftCandidates = draftCandidateItems + restoredDraftItems,
            draftItemIds = localReplyDraftItemIds,
            focus = selectedFocus,
        )
    } else {
        respondedItems
    }
    val shownNextCursor = if (selectedFilter == "pending") pendingNextCursor else respondedNextCursor
    val connectionsById = connections.associateBy(V2CoachConnection::connectionId)
    val respondedCount = respondedTotal ?: initialRespondedCount.coerceAtLeast(respondedItems.size)
    val localDraftCount = localReplyDraftItemIds.size
    val hasActiveStudents = connections.any { it.status == "active" }

    PullToRefreshBox(
        isRefreshing = reviewRefreshing,
        onRefresh = {
            retryCurrentReview()
            onReload()
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(Modifier.fillMaxSize().background(DSColors.warmWhite.copy(alpha = 0.94f))) {
            V2CoachReviewFixedChrome(
            selectedFilter = selectedFilter,
            selectedFocus = selectedFocus,
            pendingCount = pendingWorkCount,
            respondedCount = respondedCount,
            localDraftCount = localDraftCount,
            onFilterChanged = { selectedFilter = it },
            onFocusChanged = { selectedFocusName = it.name },
        )
            LazyColumn(
                modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                top = 12.dp,
                end = 20.dp,
                bottom = 34.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            if (loadError != null && shownItems.isNotEmpty()) {
                item {
                    V2CoachReviewSyncWarning(
                        onRetry = {
                            retryCurrentReview()
                            onReload()
                        },
                        onDismiss = { loadError = null },
                    )
                }
            }
            if (respondedLoading && shownItems.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            CircularProgressIndicator(color = DSColors.fairway)
                            Text("正在读取学员内容…", color = DSColors.shadowGray, fontSize = 11.sp)
                        }
                    }
                }
            } else if (loadError != null && shownItems.isEmpty()) {
                item {
                    V2CoachReviewLoadErrorState(onRetry = ::retryCurrentReview)
                }
            } else if (shownItems.isEmpty()) {
                item {
                    V2CoachReviewEmptyState(
                        selectedFilter = selectedFilter,
                        pendingCount = pendingWorkCount,
                        hasActiveStudents = hasActiveStudents,
                        onSync = {
                            retryCurrentReview()
                            onReload()
                        },
                        onAddStudent = onOpenStudents,
                    )
                }
            } else {
                itemsIndexed(shownItems, key = { _, item -> item.itemId }) { index, item ->
                    val connection = connectionsById[item.connectionId]
                    if (connection != null) {
                        CoachItemCard(
                            item = item,
                            connection = connection,
                            hasLocalDraft = item.itemId in localReplyDraftItemIds,
                            deliveryJob = deliveryJobs.firstOrNull { it.parentItemId == item.itemId },
                        ) { onOpenItem(item) }
                        if (shouldRequestNextCoachReviewPage(index, shownItems.size, shownNextCursor)) {
                            LaunchedEffect(selectedFilter, item.itemId, shownNextCursor) {
                                loadMore()
                            }
                        }
                    } else {
                        V2CoachReviewMissingConnectionCard(
                            item = item,
                            onRetry = onReload,
                        )
                    }
                }
            }
            if (loadingMore) item(key = "coach-review-loading-more") {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        color = DSColors.fairway,
                        strokeWidth = 2.dp,
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun CoachLibrary(
    assets: List<V2CoachLibraryAsset>,
    initialNextCursor: String?,
    connections: List<V2CoachConnection>,
    pendingByConnection: Map<String, Int>,
    importJobs: List<V2CoachDeliveryJob>,
    activeDeliveryJobId: String?,
    activeDeliveryPhase: V2CoachDeliveryPhase?,
    activeDeliveryProgress: Double,
    recentItems: List<V2CoachWorkspaceItem>,
    ownerUserId: String,
    connectionsLoading: Boolean,
    connectionsError: String?,
    draftStore: V2LibraryShareDraftStore,
    onDraftChanged: () -> Unit,
    onReload: () -> Unit,
    onOpenStudents: () -> Unit,
    onOpenSharedConversation: (V2CoachConnection, V2CoachWorkspaceItem) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var selectedAsset by remember { mutableStateOf<V2CoachLibraryAsset?>(null) }
    var openedAsset by remember { mutableStateOf<V2CoachLibraryAsset?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("全部") }
    var editingAsset by remember { mutableStateOf<V2CoachLibraryAsset?>(null) }
    var libraryItems by remember { mutableStateOf(assets) }
    var libraryNextCursor by remember { mutableStateOf(initialNextCursor) }
    var libraryLoading by remember { mutableStateOf(false) }
    var libraryRefreshing by remember { mutableStateOf(false) }
    var libraryLoadingMore by remember { mutableStateOf(false) }
    var libraryError by remember { mutableStateOf<String?>(null) }
    val libraryNetworkConnected = rememberQueueNetworkConnected()
    var previousLibraryNetworkConnected by remember { mutableStateOf<Boolean?>(null) }
    var showImportSource by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCellularJobId by remember { mutableStateOf<String?>(null) }
    var pendingCellularFileSize by remember { mutableLongStateOf(0L) }
    var discardImportJob by remember { mutableStateOf<V2CoachDeliveryJob?>(null) }
    var recentlyImportedAsset by remember { mutableStateOf<V2CoachLibraryAsset?>(null) }
    val activeStudents = connections.filter { it.status == "active" && it.relationshipType != "friend" }
    val searchFocusRequester = remember { FocusRequester() }
    val categories = libraryItems.libraryCategories()
    val libraryShareDraftAssetIds = if (ownerUserId.isBlank()) {
        emptySet()
    } else {
        libraryItems.mapNotNullTo(mutableSetOf()) { asset ->
            asset.assetId.takeIf { draftStore.hasDraft(ownerUserId, asset.assetId) }
        }
    }
    val displayedAssets = libraryItems.filterAndSortLibrary(query, selectedCategory, V2CoachLibrarySort.NEWEST)
        .prioritizeLibraryShareDrafts(libraryShareDraftAssetIds)
    val libraryCategoryCount = libraryItems.flatMap { it.tags }.map(String::trim).filter(String::isNotEmpty).distinct().size
    val activeImportJob = importJobs.firstOrNull { it.jobId == activeDeliveryJobId }
        ?: importJobs.firstOrNull()
    val importUnavailable = uploading || importJobs.isNotEmpty()

    LaunchedEffect(categories) {
        if (selectedCategory !in categories) selectedCategory = "全部"
    }

    LaunchedEffect(query, assets, initialNextCursor) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            val freshIds = assets.mapTo(HashSet(), V2CoachLibraryAsset::assetId)
            libraryItems = assets + libraryItems.filterNot { it.assetId in freshIds }
            libraryNextCursor = initialNextCursor
            libraryLoading = false
            libraryError = null
            return@LaunchedEffect
        }
        delay(250)
        libraryLoading = true
        libraryError = null
        runCatching {
            ServiceLocator.coachWorkspaceRepo.libraryPage(query = normalizedQuery)
        }.onSuccess { page ->
            libraryItems = page.values
            libraryNextCursor = page.nextCursor
            selectedCategory = "全部"
            libraryError = null
        }.onFailure { throwable ->
            libraryError = coachThrowableError("素材搜索暂时无法完成，请稍后重试", throwable)
        }
        libraryLoading = false
    }

    LaunchedEffect(recentlyImportedAsset?.assetId) {
        val importedId = recentlyImportedAsset?.assetId ?: return@LaunchedEffect
        delay(10_000)
        if (recentlyImportedAsset?.assetId == importedId) recentlyImportedAsset = null
    }

    suspend fun finishLibraryImport(assetId: String?) {
        val imported = assetId?.let { id ->
            ServiceLocator.coachWorkspaceRepo.coachAsset(id).getOrNull()
        }
        if (imported != null) {
            libraryItems = listOf(imported) + libraryItems.filterNot { it.assetId == imported.assetId }
            recentlyImportedAsset = imported
            query = ""
            selectedCategory = "全部"
            message = null
        } else {
            message = "素材已保存，并已放到列表顶部"
        }
        onReload()
    }

    fun loadMoreLibrary() {
        val cursor = libraryNextCursor ?: return
        if (libraryLoadingMore) return
        libraryLoadingMore = true
        libraryError = null
        scope.launch {
            runCatching {
                ServiceLocator.coachWorkspaceRepo.libraryPage(
                    query = query,
                    cursor = cursor,
                )
            }.onSuccess { page ->
                val incomingIds = page.values.mapTo(HashSet(), V2CoachLibraryAsset::assetId)
                libraryItems = libraryItems.filterNot { it.assetId in incomingIds } + page.values
                libraryNextCursor = page.nextCursor
                libraryError = null
            }.onFailure { throwable ->
                libraryError = coachThrowableError("更多素材暂时无法加载，请稍后重试", throwable)
            }
            libraryLoadingMore = false
        }
    }

    fun refreshLibrary() {
        if (libraryRefreshing || libraryLoading) return
        val requestedQuery = query.trim()
        libraryRefreshing = true
        libraryError = null
        scope.launch {
            runCatching {
                ServiceLocator.coachWorkspaceRepo.libraryPage(query = requestedQuery)
            }.onSuccess { page ->
                if (query.trim() == requestedQuery) {
                    libraryItems = page.values
                    libraryNextCursor = page.nextCursor
                    libraryError = null
                }
            }.onFailure { throwable ->
                if (query.trim() == requestedQuery) {
                    libraryError = coachThrowableError("素材暂时没有同步下来，请稍后重试", throwable)
                }
            }
            libraryRefreshing = false
        }
    }

    LaunchedEffect(libraryNetworkConnected) {
        val wasConnected = previousLibraryNetworkConnected
        previousLibraryNetworkConnected = libraryNetworkConnected
        if (shouldRefreshCoachLibraryAfterReconnect(wasConnected, libraryNetworkConnected)) {
            refreshLibrary()
        }
    }

    fun importLibraryVideo(uri: android.net.Uri, sourceKind: String, capturedFile: File? = null) {
        if (uploading) return
        uploading = true
        message = null
        scope.launch {
            var prepared: com.mygolfswing.app.ui.interaction.V2PreparedWorkspaceVideo? = null
            var enqueued = false
            try {
                val ownerUserId = ServiceLocator.session.current().userId
                    ?: error("登录状态已失效，请重新登录")
                // Like iOS CoachLibraryDrafts, take ownership before any upload
                // starts. Reliable jobs must not point at cacheDir or a picker
                // grant that can disappear after process death/cache cleanup.
                val preparedVideo = preparePersistentLibraryImportVideo(
                    context = context,
                    uri = uri,
                    ownerUserId = ownerUserId,
                    uploadId = "library_${UUID.randomUUID()}",
                    requestId = "library_prepare_${UUID.randomUUID()}",
                    sourceKind = sourceKind,
                )
                prepared = preparedVideo
                val job = V2CoachDeliveryJob(
                    jobId = preparedVideo.requestId,
                    ownerUserId = ownerUserId,
                    connectionId = "",
                    videoPath = preparedVideo.file.absolutePath,
                    coverPath = requireNotNull(preparedVideo.cover).absolutePath,
                    uploadId = preparedVideo.uploadId,
                    requestId = preparedVideo.requestId,
                    title = defaultCoachLibraryImportTitle(),
                    text = "",
                    durationSeconds = preparedVideo.durationSeconds,
                    sourceKind = sourceKind,
                    deliveryType = V2_DELIVERY_LIBRARY_IMPORT,
                    tags = emptyList(),
                )
                ServiceLocator.coachDeliveryQueue.enqueue(job)
                enqueued = true
                val result = ServiceLocator.coachDeliveryQueue.deliver(
                    job.jobId,
                    ServiceLocator.coachWorkspaceRepo,
                ).getOrThrow()
                finishLibraryImport(result.libraryAssetId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                message = if (throwable is V2CellularUploadConfirmationRequired) {
                    pendingCellularJobId = throwable.jobId
                    pendingCellularFileSize = throwable.fileSizeBytes
                    null
                } else if (enqueued) {
                    "素材已进入可靠队列，网络恢复后会自动继续上传"
                } else {
                    coachThrowableError("素材暂时无法加入上传队列，请重新选择视频", throwable)
                }
            } finally {
                capturedFile?.delete()
                if (!enqueued) prepared?.deleteTemporaryFiles()
                uploading = false
            }
        }
    }

    fun confirmCellularImport() {
        val jobId = pendingCellularJobId ?: return
        if (uploading) return
        uploading = true
        message = null
        scope.launch {
            try {
                if (!ServiceLocator.coachDeliveryQueue.approveCellularUpload(jobId)) {
                    throw IllegalStateException("待上传素材已经完成或失效")
                }
                val result = ServiceLocator.coachDeliveryQueue.deliver(
                    jobId,
                    ServiceLocator.coachWorkspaceRepo,
                ).getOrThrow()
                pendingCellularJobId = null
                finishLibraryImport(result.libraryAssetId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                pendingCellularJobId = null
                message = coachThrowableError("素材暂未上传，已保留在可靠队列", throwable)
            } finally {
                uploading = false
            }
        }
    }

    fun continueLibraryImport(job: V2CoachDeliveryJob) {
        if (uploading || job.jobId == activeDeliveryJobId) return
        uploading = true
        message = null
        scope.launch {
            try {
                val result = ServiceLocator.coachDeliveryQueue.deliver(
                    job.jobId,
                    ServiceLocator.coachWorkspaceRepo,
                ).getOrThrow()
                finishLibraryImport(result.libraryAssetId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                message = if (throwable is V2CellularUploadConfirmationRequired) {
                    pendingCellularJobId = throwable.jobId
                    pendingCellularFileSize = throwable.fileSizeBytes
                    "视频已保留，确认后即可继续上传"
                } else {
                    coachThrowableError("上传暂停，视频已经保留", throwable)
                }
            } finally {
                uploading = false
            }
        }
    }

    fun stopLibraryImport(job: V2CoachDeliveryJob) {
        if (job.jobId != activeDeliveryJobId) return
        scope.launch {
            if (ServiceLocator.coachDeliveryQueue.stop(job.jobId)) {
                message = "上传已停止，视频仍保留在本机"
            }
        }
    }

    fun discardLibraryImport(job: V2CoachDeliveryJob) {
        if (job.jobId == activeDeliveryJobId) return
        uploading = true
        scope.launch {
            try {
                if (ServiceLocator.coachDeliveryQueue.cancel(job.jobId)) {
                    message = "本机未完成素材已删除"
                }
            } finally {
                discardImportJob = null
                uploading = false
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { importLibraryVideo(it, sourceKind = "album") }
    }
    fun launchAlbumPicker() {
        showImportSource = false
        message = null
        runCatching {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }.onFailure { throwable ->
            message = coachThrowableError("无法打开相册，请稍后重试", throwable)
            showImportSource = true
        }
    }
    val captureVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { captured ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (captured && file != null) importLibraryVideo(file.toUri(), "coach_recording", file)
        else file?.delete()
    }
    fun launchCamera() {
        var outputFile: File? = null
        runCatching {
            val (file, uri) = V2VideoFileStore.createCameraOutputUri(context)
            outputFile = file
            pendingCameraFile = file
            captureVideo.launch(uri)
        }.onFailure { throwable ->
            pendingCameraFile = null
            outputFile?.delete()
            message = coachThrowableError("无法打开相机，请改从相册选择", throwable)
            showImportSource = true
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        } else {
            message = "需要相机权限才能拍摄教学素材，也可以从相册添加"
            showImportSource = true
        }
    }
    fun requestCamera() {
        showImportSource = false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            runCatching { cameraPermission.launch(Manifest.permission.CAMERA) }
                .onFailure { throwable ->
                    message = coachThrowableError("无法申请相机权限，请改从相册选择", throwable)
                    showImportSource = true
                }
        }
    }

    PullToRefreshBox(
        isRefreshing = libraryRefreshing,
        onRefresh = ::refreshLibrary,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
            V2RoomBackground(Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize()) {
                V2CoachLibraryHero(
                    assetCount = libraryItems.size,
                    categoryCount = libraryCategoryCount,
                    reuseCount = libraryItems.sumOf { it.useCount },
                    addEnabled = !importUnavailable,
                    dimmed = importUnavailable,
                    onSearch = { searchFocusRequester.requestFocus() },
                    onAdd = { showImportSource = true },
                )
                if (libraryError != null && libraryItems.isNotEmpty()) {
                    V2CoachLibrarySyncWarning(
                        onRetry = ::refreshLibrary,
                        onDismiss = { libraryError = null },
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp),
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        top = 14.dp,
                        end = 16.dp,
                        bottom = 34.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
            recentlyImportedAsset?.let { imported ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    V2CoachLibraryImportSuccessCard(
                        studentCount = activeStudents.size,
                        singleStudentName = activeStudents.singleOrNull()?.student?.nickname,
                        onShare = {
                            message = null
                            selectedAsset = imported
                        },
                        onEdit = { editingAsset = imported },
                        onNeedStudent = onOpenStudents,
                    )
                }
            }
            activeImportJob?.let { job ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    V2CoachLibraryImportCard(
                        job = job,
                        active = job.jobId == activeDeliveryJobId,
                        phase = activeDeliveryPhase.takeIf { job.jobId == activeDeliveryJobId },
                        progress = activeDeliveryProgress.takeIf { job.jobId == activeDeliveryJobId } ?: 0.0,
                        actionBusy = uploading,
                        onContinue = { continueLibraryImport(job) },
                        onStop = { stopLibraryImport(job) },
                        onDiscard = { discardImportJob = job },
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                V2CoachLibrarySearchField(
                    value = query,
                    onValueChange = { query = it },
                    focusRequester = searchFocusRequester,
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                V2CoachLibraryCategoryFilter(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelected = { selectedCategory = it },
                )
            }
            message?.let { notice -> item(span = { GridItemSpan(maxLineSpan) }) { NoticeCard(notice) } }
            if (libraryLoading && libraryItems.isEmpty() && !importUnavailable) item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DSColors.fairway)
                }
            } else if (libraryItems.isEmpty() && importUnavailable) item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    if (uploading) "处理完成后会自动放到这里，可以先切换到其他页面"
                    else "未完成的视频仍在本机，点上方“继续”即可恢复",
                    color = DSColors.shadowGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                )
            } else if (libraryError != null && libraryItems.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                V2CoachLibraryUnavailableState(
                    actionsEnabled = !importUnavailable,
                    onRetry = ::refreshLibrary,
                    onAdd = { showImportSource = true },
                )
            } else if (libraryItems.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                V2CoachLibraryEmptyState(
                    searchActive = query.isNotBlank(),
                    actionsEnabled = !importUnavailable,
                    onAlbum = ::launchAlbumPicker,
                    onCamera = ::requestCamera,
                    onReset = {
                        query = ""
                        selectedCategory = "全部"
                    },
                )
            } else if (displayedAssets.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                V2CoachLibraryNoResultsState(
                    onReset = {
                        query = ""
                        selectedCategory = "全部"
                    },
                )
            }
                    gridItemsIndexed(displayedAssets, key = { _, asset -> asset.assetId }) { index, asset ->
                        V2CoachLibraryGridCard(
                            asset = asset,
                            hasDraft = asset.assetId in libraryShareDraftAssetIds,
                            onOpen = { openedAsset = asset },
                            onShare = {
                                message = null
                                selectedAsset = asset
                            },
                            onCoverNeedsRefresh = {
                                ServiceLocator.coachWorkspaceRepo.coachAsset(asset.assetId)
                                    .onSuccess { loaded ->
                                        libraryItems = libraryItems.map { current ->
                                            if (current.assetId == loaded.assetId) loaded else current
                                        }
                                    }
                            },
                        )
                        if (shouldRequestNextCoachLibraryPage(index, displayedAssets.size, libraryNextCursor)) {
                            LaunchedEffect(query, asset.assetId, libraryNextCursor) {
                                loadMoreLibrary()
                            }
                        }
                    }
                    if (libraryLoadingMore) item(
                        key = "coach-library-loading-more",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                Modifier.size(18.dp),
                                color = DSColors.fairway,
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showImportSource) {
        V2CoachLibraryAddSourceSheet(
            enabled = !importUnavailable,
            onDismiss = { showImportSource = false },
            onCamera = ::requestCamera,
            onAlbum = ::launchAlbumPicker,
        )
    }

    if (pendingCellularJobId != null) {
        V2CellularUploadConfirmationDialog(
            fileSizeBytes = pendingCellularFileSize,
            busy = uploading,
            onConfirm = ::confirmCellularImport,
            onDismiss = {
                pendingCellularJobId = null
                message = "素材已保留，连接 Wi-Fi 后会自动上传"
            },
        )
    }

    discardImportJob?.let { job ->
        V2CoachLibraryDiscardImportSheet(
            busy = uploading,
            onConfirm = { discardLibraryImport(job) },
            onDismiss = { discardImportJob = null },
        )
    }

    openedAsset?.let { originalAsset ->
        val asset = libraryItems.firstOrNull { it.assetId == originalAsset.assetId } ?: originalAsset
        V2CoachLibraryDetailDialog(
            asset = asset,
            recentItems = recentItems,
            connections = connections,
            hasShareDraft = ownerUserId.isNotBlank() && draftStore.hasDraft(ownerUserId, asset.assetId),
            onDismiss = { openedAsset = null },
            onShare = {
                message = null
                selectedAsset = asset
            },
            onEdit = {
                editingAsset = asset
            },
            onAssetLoaded = { loaded ->
                libraryItems = libraryItems.map { current ->
                    if (current.assetId == loaded.assetId) loaded else current
                }
            },
            onRemoved = { removed ->
                libraryItems = libraryItems.filterNot { it.assetId == removed.assetId }
                message = "已移出素材库，历史发送不受影响"
                onReload()
            },
        )
    }

    editingAsset?.let { originalAsset ->
        val asset = libraryItems.firstOrNull { it.assetId == originalAsset.assetId } ?: originalAsset
        V2CoachLibraryEditDialog(
            asset = asset,
            onDismiss = { editingAsset = null },
            onAssetLoaded = { loaded ->
                libraryItems = libraryItems.map { current ->
                    if (current.assetId == loaded.assetId) loaded else current
                }
            },
            onSaved = { updated ->
                libraryItems = libraryItems.map { current ->
                    if (current.assetId == updated.assetId) updated else current
                }
                message = "素材信息已保存"
                onReload()
            },
            onRemoved = { removed ->
                libraryItems = libraryItems.filterNot { it.assetId == removed.assetId }
                if (openedAsset?.assetId == removed.assetId) openedAsset = null
                message = "已移出素材库，历史发送不受影响"
                onReload()
            },
        )
    }

    selectedAsset?.let { originalAsset ->
        val asset = libraryItems.firstOrNull { it.assetId == originalAsset.assetId } ?: originalAsset
        V2CoachLibraryShareDialog(
            asset = asset,
            connections = activeStudents,
            pendingByConnection = pendingByConnection,
            ownerUserId = ownerUserId,
            draftStore = draftStore,
            connectionsLoading = connectionsLoading,
            connectionsError = connectionsError,
            onDismiss = { selectedAsset = null },
            onNeedStudent = onOpenStudents,
            onReloadConnections = onReload,
            onDraftChanged = onDraftChanged,
            onAssetLoaded = { loaded ->
                libraryItems = libraryItems.map { current ->
                    if (current.assetId == loaded.assetId) loaded else current
                }
            },
            onSent = { sentItems ->
                onReload()
                val sentItem = sentItems.singleOrNull()
                val connection = sentItem?.let { item ->
                    activeStudents.firstOrNull { it.connectionId == item.connectionId }
                }
                if (sentItem != null && connection != null) {
                    message = null
                    onOpenSharedConversation(connection, sentItem)
                } else {
                    message = "素材已发给 ${sentItems.size} 位学员"
                }
            },
        )
    }
}

@Composable
internal fun CoachItemCard(
    item: V2CoachWorkspaceItem,
    connection: V2CoachConnection?,
    hasLocalDraft: Boolean,
    deliveryJob: V2CoachDeliveryJob?,
    onClick: () -> Unit,
) {
    val presentation = coachReviewCardPresentation(item, connection, hasLocalDraft, deliveryJob)
    val statusColor = when (presentation.tone) {
        V2CoachReviewCardTone.ERROR -> DSColors.flagRed
        V2CoachReviewCardTone.ATTENTION -> Color(0xFFC07A28)
        V2CoachReviewCardTone.ACTIVE -> DSColors.fairway
        V2CoachReviewCardTone.COMPLETE -> DSColors.deepGreen
    }
    val borderColor = when {
        presentation.tone == V2CoachReviewCardTone.ERROR -> DSColors.flagRed.copy(alpha = 0.72f)
        hasLocalDraft -> DSColors.trophyGold.copy(alpha = 0.72f)
        item.reviewState == "responded" -> Color(0xFFE2E7E3)
        else -> DSColors.fairway.copy(alpha = 0.72f)
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(contentAlignment = Alignment.BottomEnd) {
                V2UserAvatar(
                    avatar = presentation.studentAvatar,
                    modifier = Modifier.size(44.dp),
                    fallbackFontSize = 17.sp,
                    placeholderIconSize = 25.dp,
                    fallbackText = presentation.studentName,
                    fallbackColor = DSColors.sandBeige,
                )
                Box(
                    Modifier.size(10.dp).clip(CircleShape).background(DSColors.fairway)
                        .border(2.dp, Color.White, CircleShape),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        presentation.studentName,
                        color = DSColors.deepGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (presentation.pinned) Text("◆", color = DSColors.trophyGold, fontSize = 9.sp)
                }
                Text(
                    presentation.metadata.ifBlank { "刚刚" },
                    color = DSColors.shadowGray,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                Modifier.clip(CircleShape).background(statusColor.copy(alpha = 0.10f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(statusColor))
                Text(presentation.statusTitle, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        presentation.coachingFocus?.let { focus ->
            Text(
                "◎ 训练重点：$focus",
                color = DSColors.fairway,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clip(CircleShape).background(DSColors.mistGreen.copy(alpha = 0.78f))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
            )
        } ?: Text("训练重点待确认", color = DSColors.trophyGold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)

        if (item.latestActivityHasVideoContent) {
            Box(
                Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(14.dp))
                    .background(DSColors.deepGreen),
                contentAlignment = Alignment.Center,
            ) {
                item.latestActivityCoverUrl?.let { cover ->
                    AsyncImage(
                        model = cover,
                        contentDescription = "点评视频封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.42f)),
                    contentAlignment = Alignment.Center,
                ) { Text("▶", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                Text(
                    item.title?.takeIf(String::isNotBlank) ?: "练习视频",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(11.dp),
                )
            }
        }

        if (presentation.compactResponded) {
            val respondedText = V2CoachWorkspaceItem.visibleCopy(item.latestReplyText)
                ?: presentation.originalText
            if (!item.latestActivityHasVideoContent || respondedText != presentation.originalText) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(DSColors.mistGreen.copy(alpha = 0.5f)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(presentation.respondedSenderLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(respondedText, color = DSColors.deepGreen, fontSize = 15.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            if (!item.latestActivityHasVideoContent) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                        .background(DSColors.mistGreen.copy(alpha = 0.4f)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text("❝", color = DSColors.fairway, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        presentation.originalText,
                        color = DSColors.deepGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFAF8F2)).padding(horizontal = 11.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("最新交流", color = DSColors.shadowGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    presentation.latestExchangeText,
                    color = DSColors.deepGreen,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E7E3)))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasLocalDraft) Text("✎  ", color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(presentation.footerText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("›", color = statusColor, fontSize = 20.sp)
        }
    }
}

@Composable
internal fun CoachProfile(
    snapshot: V2CoachSnapshot,
    deliveryPendingCount: Int,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwitchToStudent: () -> Unit,
    onLogoutComplete: () -> Unit,
    sessionOverride: V2Session? = null,
    initialMetrics: V2CoachMetrics? = null,
    loadMetricsOnStart: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val storedSession by ServiceLocator.session.state.collectAsState(initial = V2Session())
    val session = sessionOverride ?: storedSession
    var metrics by remember(initialMetrics) { mutableStateOf(initialMetrics) }
    var metricsLoading by remember { mutableStateOf(false) }
    var metricsError by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var loggingOut by remember { mutableStateOf(false) }
    var logoutError by remember { mutableStateOf<String?>(null) }

    fun loadMetrics() {
        if (metricsLoading) return
        metricsLoading = true
        metricsError = null
        scope.launch {
            ServiceLocator.coachWorkspaceRepo.coachMetrics()
                .onSuccess { metrics = it }
                .onFailure { metricsError = coachThrowableError("教学数据暂时没有同步，请稍后重试", it) }
            metricsLoading = false
        }
    }

    LaunchedEffect(loadMetricsOnStart) {
        if (loadMetricsOnStart) loadMetrics()
    }

    Box(Modifier.fillMaxSize().background(DSColors.warmWhite)) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                if (!refreshing) {
                    refreshing = true
                    onRefresh()
                    loadMetrics()
                    scope.launch {
                        delay(650)
                        refreshing = false
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            V2RoomBackground(Modifier.fillMaxSize())
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "我",
                    color = DSColors.deepGreen,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                        .background(DSColors.cardWhite).padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.size(76.dp).clip(CircleShape).background(DSColors.mistGreen)
                            .border(2.dp, DSColors.trophyGold, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        V2UserAvatar(
                            avatar = session.avatar,
                            modifier = Modifier.fillMaxSize(),
                            fallbackFontSize = 28.sp,
                            placeholderIconSize = 38.dp,
                            fallbackText = session.nickname ?: session.username ?: "挥呗教练",
                            fallbackColor = DSColors.sandBeige,
                        )
                    }
                    Text(
                        session.nickname ?: session.username ?: "挥呗教练",
                        color = DSColors.deepGreen,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Filled.CheckCircle, null, tint = DSColors.fairway, modifier = Modifier.size(13.dp))
                        Text(
                            if (session.coachAuthorizationStatus == "approved") "已认证教练" else "教练身份",
                            color = DSColors.fairway,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(DSColors.cardWhite)
                        .border(1.dp, DSColors.borderLine, RoundedCornerShape(20.dp))
                        .padding(vertical = 15.dp),
                ) {
                    CoachProfileOverviewMetric(
                        snapshot.connections.count { it.status == "active" },
                        "学员",
                        Icons.Filled.Groups,
                        Modifier.weight(1f),
                    )
                    CoachProfileDivider()
                    CoachProfileOverviewMetric(
                        snapshot.dashboard.pendingCount,
                        "待回复",
                        Icons.Filled.ChatBubble,
                        Modifier.weight(1f),
                    )
                    CoachProfileDivider()
                    CoachProfileOverviewMetric(
                        snapshot.dashboard.respondedCount,
                        "已回复",
                        Icons.Filled.CheckCircle,
                        Modifier.weight(1f),
                    )
                }

                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(DSColors.cardWhite)
                        .border(1.dp, DSColors.borderLine, RoundedCornerShape(20.dp))
                        .padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("教学效率", color = DSColors.deepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("根据真实交流、上传与播放记录累计", color = DSColors.shadowGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                        when {
                            metricsLoading -> CircularProgressIndicator(Modifier.size(20.dp), color = DSColors.fairway, strokeWidth = 2.dp)
                            metrics != null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Filled.CheckCircle, null, tint = DSColors.fairway, modifier = Modifier.size(11.dp))
                                Text("已同步", color = DSColors.fairway, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    val currentMetrics = metrics
                    val values = currentMetrics?.let {
                        listOf(
                            Triple(it.receivedCount.toString(), "收到学员内容", "累计进入点评队列"),
                            Triple(
                                coachPercentage(it.feedbackCompletionRate),
                                "反馈完成率",
                                if (it.receivedCount == 0) "暂无内容" else "已经发送反馈",
                            ),
                            Triple(coachDuration(it.firstViewAverageSeconds), "平均首次查看", "从学员提交开始"),
                            Triple(coachDuration(it.feedbackAverageSeconds), "平均完成反馈", "从学员提交开始"),
                            Triple(
                                coachPercentage(1.0 - it.uploadFailureRate),
                                "视频上传成功率",
                                if (it.averageVideoBytes > 0) "平均每段 ${coachBytes(it.averageVideoBytes)}" else "暂无视频样本",
                            ),
                            Triple(coachPercentage(it.assetReuseRate), "素材复用率", "累计播放 ${it.playbackCount} 次"),
                            Triple(coachShortDuration(it.averageProcessingSeconds), "平均视频处理", "已压缩 ${it.optimizedUploadCount} 段"),
                            Triple(
                                coachPercentage(it.compressionSavingsRate),
                                "压缩节省流量",
                                "直传 ${coachPercentage(it.directUploadRate)} · 蜂窝 ${it.cellularUploadCount} 次",
                            ),
                        )
                    }
                    if (values != null) {
                        values.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                row.forEach { (value, label, hint) ->
                                    CoachTeachingMetric(value, label, hint, Modifier.weight(1f))
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                                .background(DSColors.warmWhite).padding(horizontal = 11.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Storage, null, tint = DSColors.fairway, modifier = Modifier.size(11.dp))
                            Text(
                                coachCloudUsageText(currentMetrics),
                                color = DSColors.shadowGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else if (metricsError != null) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(DSColors.trophyGold.copy(alpha = 0.14f)).padding(11.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Filled.Error, null, tint = DSColors.trophyGold, modifier = Modifier.size(17.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("教学数据暂时没有同步", color = DSColors.deepGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(metricsError.orEmpty(), color = DSColors.shadowGray, fontSize = 9.sp, maxLines = 2)
                            }
                            Text("重试", color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { loadMetrics() }.padding(4.dp))
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = DSColors.fairway, strokeWidth = 2.dp)
                            Text("正在汇总真实教学数据…", color = DSColors.shadowGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                CoachProfileActionRow(
                    Icons.Filled.Settings,
                    "教练版设置",
                    "查看身份状态与切换产品体验",
                    DSColors.trophyGold,
                    onOpenSettings,
                )
                CoachProfileActionRow(
                    Icons.Filled.SwapHoriz,
                    "切换身份",
                    "选择练习者版或教练版",
                    DSColors.fairway,
                    onSwitchToStudent,
                    enabled = deliveryPendingCount == 0,
                )

                Row(
                    Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(18.dp)).background(DSColors.cardWhite)
                        .clickable(enabled = !loggingOut && deliveryPendingCount == 0) { showLogoutConfirmation = true }
                        .testTag("coach-profile-logout"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (loggingOut) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = DSColors.flagRed, strokeWidth = 2.dp)
                        Spacer(Modifier.size(7.dp))
                    }
                    Text(if (loggingOut) "退出中…" else "退出登录", color = DSColors.flagRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                if (deliveryPendingCount > 0) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(Icons.Filled.CloudUpload, null, tint = DSColors.fairway, modifier = Modifier.size(13.dp))
                        Text(
                            "还有${deliveryPendingCount}条内容待发送，完成后即可切换身份或退出",
                            color = DSColors.fairway,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                logoutError?.let { Text(it, color = DSColors.flagRed, fontSize = 11.sp) }
            }
        }

        if (showLogoutConfirmation) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.40f))
                    .clickable(enabled = !loggingOut) { showLogoutConfirmation = false },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.fillMaxWidth().widthIn(max = 360.dp).padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(24.dp)).background(Color.White).clickable { },
                ) {
                    Box(Modifier.fillMaxWidth().height(116.dp), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.golf_course_background),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(Modifier.fillMaxSize().background(DSColors.deepGreen.copy(alpha = 0.72f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Text("退出登录", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("云端教学内容会保留", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                        }
                    }
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("确认退出当前账号？", color = DSColors.deepGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "退出后本机草稿与私有视频缓存会被清理，云端教学内容不会删除。",
                                color = DSColors.shadowGray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { showLogoutConfirmation = false },
                                enabled = !loggingOut,
                                colors = ButtonDefaults.buttonColors(containerColor = DSColors.mistGreen, contentColor = DSColors.deepGreen),
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) { Text("取消", fontWeight = FontWeight.Bold) }
                            Button(
                                onClick = {
                                    showLogoutConfirmation = false
                                    loggingOut = true
                                    scope.launch {
                                        try {
                                            V2ProfileStore.logoutAndWait()
                                            onLogoutComplete()
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (_: Throwable) {
                                            logoutError = "退出登录失败，请稍后重试"
                                        } finally {
                                            loggingOut = false
                                        }
                                    }
                                },
                                enabled = !loggingOut,
                                colors = ButtonDefaults.buttonColors(containerColor = DSColors.flagRed),
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) { Text(if (loggingOut) "退出中…" else "确认退出", fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachProfileOverviewMetric(value: Int, label: String, icon: ImageVector, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, null, tint = DSColors.fairway, modifier = Modifier.size(11.dp))
            Text(value.toString(), color = DSColors.fairway, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = DSColors.shadowGray, fontSize = 10.sp)
    }
}

@Composable
private fun CoachProfileDivider() {
    Box(Modifier.size(width = 1.dp, height = 34.dp).background(Color(0xFFE3E1DA)))
}

@Composable
private fun CoachTeachingMetric(value: String, label: String, hint: String, modifier: Modifier) {
    Column(
        modifier.heightIn(min = 70.dp).clip(RoundedCornerShape(14.dp))
            .background(DSColors.mistGreen.copy(alpha = 0.46f))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, color = DSColors.fairway, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = DSColors.deepGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(hint, color = DSColors.shadowGray, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CoachProfileActionRow(
    icon: ImageVector,
    title: String,
    detail: String,
    accent: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp).alpha(if (enabled) 1f else 0.62f)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(if (title == "切换身份") "coach-profile-role-switch" else "coach-profile-settings")
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = DSColors.deepGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = DSColors.shadowGray, fontSize = 11.sp)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = DSColors.shadowGray.copy(alpha = 0.65f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun CoachIncomingActivityBar(
    notice: V2CoachIncomingActivity,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(52.dp)
            .background(DSColors.cardWhite.copy(alpha = 0.98f))
            .testTag("coach-incoming-activity-bar"),
    ) {
        Box(Modifier.fillMaxSize().clickable(onClick = onOpen).padding(horizontal = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape)
                        .background(V2CoachPalette.mutedGold.copy(alpha = 0.66f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when {
                            notice.itemId == null && notice.title == "已完成连接" -> Icons.Filled.PersonAdd
                            notice.itemId == null -> Icons.Filled.Lock
                            notice.hasVideo -> Icons.Filled.VideoLibrary
                            else -> Icons.Filled.ChatBubble
                        },
                        contentDescription = null,
                        tint = V2CoachPalette.gold,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${notice.studentName} · ${notice.title}",
                        color = DSColors.inkGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        notice.detail,
                        color = DSColors.shadowGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text("查看", color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = DSColors.fairway,
                    modifier = Modifier.size(11.dp),
                )
                Spacer(Modifier.size(26.dp))
            }
        }
        Box(
            modifier = Modifier.align(Alignment.CenterEnd)
                .padding(end = 12.dp).size(width = 26.dp, height = 32.dp)
                .clip(CircleShape).clickable(onClick = onDismiss)
                .testTag("coach-incoming-activity-dismiss"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "关闭新内容提示",
                tint = DSColors.shadowGray,
                modifier = Modifier.size(10.dp),
            )
        }
        Box(
            Modifier.align(Alignment.CenterStart)
                .size(width = 3.dp, height = 52.dp)
                .background(V2CoachPalette.gold),
        )
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(0.5.dp)
                .background(DSColors.borderLine),
        )
    }
}

private fun coachPercentage(value: Double): String = "${(value.coerceIn(0.0, 1.0) * 100).roundToInt()}%"

private fun coachDuration(seconds: Double): String = when {
    seconds <= 0 -> "—"
    seconds < 60 -> "<1分钟"
    seconds < 3_600 -> "${(seconds / 60).roundToInt().coerceAtLeast(1)}分钟"
    seconds < 86_400 -> (seconds / 3_600).let { hours ->
        if (hours < 10) "%.1f小时".format(hours) else "${hours.roundToInt()}小时"
    }
    else -> (seconds / 86_400).let { days ->
        if (days < 10) "%.1f天".format(days) else "${days.roundToInt()}天"
    }
}

private fun coachShortDuration(seconds: Double): String = when {
    seconds <= 0 -> "—"
    seconds < 60 -> "${seconds.roundToInt().coerceAtLeast(1)}秒"
    else -> coachDuration(seconds)
}

private fun coachBytes(bytes: Double): String = when {
    bytes <= 0 -> "0 KB"
    bytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(bytes / (1024 * 1024 * 1024))
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024 * 1024))
    else -> "%.1f KB".format(bytes / 1024)
}

private fun coachCloudUsageText(metrics: V2CoachMetrics): String {
    val uploadSummary = if (metrics.totalUploadCount == 0) {
        "暂无上传样本"
    } else {
        "累计上传 ${metrics.totalUploadCount} 次，平均 ${coachDuration(metrics.averageUploadSeconds)}"
    }
    val sizeSummary = if (metrics.totalSourceVideoBytes > metrics.totalVideoBytes) {
        "原视频 ${coachBytes(metrics.totalSourceVideoBytes)}，云端 ${coachBytes(metrics.totalVideoBytes)}"
    } else {
        "实际占用 ${coachBytes(metrics.totalVideoBytes)}"
    }
    return "云端有效视频 ${metrics.readyAssetCount} 条 · $sizeSummary · $uploadSummary"
}

private fun inviteHasExpired(expiresAt: String?): Boolean =
    runCatching { expiresAt?.let(Instant::parse)?.let { !Instant.now().isBefore(it) } }
        .getOrNull() == true

private fun inviteExpiryText(expiresAt: String?): String {
    val expiry = runCatching { expiresAt?.let(Instant::parse) }.getOrNull()
        ?: return "邀请仅可由一位学员使用"
    val remainingSeconds = expiry.epochSecond - Instant.now().epochSecond
    if (remainingSeconds <= 0) return "邀请已过期，请生成新邀请"
    return when {
        remainingSeconds < 3_600 -> "约 ${(remainingSeconds + 59) / 60} 分钟后失效 · 仅限一位学员"
        remainingSeconds < 86_400 -> "约 ${(remainingSeconds + 3_599) / 3_600} 小时后失效 · 仅限一位学员"
        else -> {
            val value = DateTimeFormatter.ofPattern("M月d日 HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(expiry)
            "有效至 $value · 仅限一位学员"
        }
    }
}

@Composable private fun MetricCard(label: String, value: String, modifier: Modifier, onClick: () -> Unit = {}) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Color.White).clickable(onClick = onClick).padding(14.dp)) {
        Text(value, color = DSColors.deepGreen, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text(label, color = DSColors.shadowGray, fontSize = 12.sp)
    }
}

@Composable private fun ActionCard(title: String, detail: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(18.dp)).background(DSColors.deepGreen).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFFE9C35D), modifier = Modifier.size(26.dp))
        Spacer(Modifier.size(10.dp))
        Column { Text(title, color = Color.White, fontWeight = FontWeight.Bold); Text(detail, color = Color.White.copy(alpha = .7f), fontSize = 10.sp) }
    }
}

@Composable private fun SectionTitle(title: String) = Text(title, color = DSColors.deepGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
@Composable private fun EmptyCard(text: String) = Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(26.dp), contentAlignment = Alignment.Center) { Text(text, color = DSColors.shadowGray, fontSize = 13.sp) }
@Composable private fun NoticeCard(text: String) = Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFEED7)).padding(12.dp)) { Text(text, color = Color(0xFF815728), fontSize = 12.sp) }
