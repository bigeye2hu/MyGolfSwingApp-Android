package com.mygolfswing.app.ui.tabs

import android.content.Intent
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.rememberV2AppNavigation
import com.mygolfswing.app.ui.common.V2SplashScreen
import com.mygolfswing.app.ui.common.V2DeliveryCompletionBar
import com.mygolfswing.app.ui.common.splashRemainingDelayMillis
import com.mygolfswing.app.ui.memo.V2OnlineVideoSelection
import com.mygolfswing.app.ui.memo.V2OnlineVideoSelectorScreen
import com.mygolfswing.app.ui.swing.V2JustSwingMainScreen
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.session.V2Session
import com.mygolfswing.app.data.role.V2ExperienceRole
import com.mygolfswing.app.data.coach.V2PendingCoachInvite
import com.mygolfswing.app.data.coach.V2CoachConnection
import com.mygolfswing.app.data.coach.V2CoachDeliveryJob
import com.mygolfswing.app.data.coach.V2CoachDeliveryPhase
import com.mygolfswing.app.data.coach.V2MemoDiscussionIndex
import com.mygolfswing.app.data.coach.V2StudentIncomingActivity
import com.mygolfswing.app.data.coach.V2StudentUnreadSnapshot
import com.mygolfswing.app.data.coach.resolveStudentIncomingActivity
import com.mygolfswing.app.data.coach.resolveStudentConnectionActivity
import com.mygolfswing.app.data.coach.jobsForOwner
import com.mygolfswing.app.data.coach.completionForOwner
import com.mygolfswing.app.data.friend.V2TencentSessionEvent
import com.mygolfswing.app.data.model.V2SwingClipRetention
import com.mygolfswing.app.data.store.V2CategoryStore
import com.mygolfswing.app.data.store.V2UserScopedCache
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.data.store.V2GolfProfileStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.ai.V2AIChatScreen
import com.mygolfswing.app.ui.profile.V2AccountDeletionScreen
import com.mygolfswing.app.ui.profile.V2AccountSecurityScreen
import com.mygolfswing.app.ui.auth.V2LoginScreen
import com.mygolfswing.app.ui.auth.V2CompleteProfileScreen
import com.mygolfswing.app.ui.auth.V2RegisterScreen
import com.mygolfswing.app.ui.auth.V2ResetPasswordScreen
import com.mygolfswing.app.ui.feedback.V2FeedbackScreen
import com.mygolfswing.app.ui.feedback.V2FeedbackDraft
import com.mygolfswing.app.ui.legal.V2LegalDocType
import com.mygolfswing.app.ui.legal.V2LegalScreen
import com.mygolfswing.app.ui.settings.V2AIServiceConfigScreen
import com.mygolfswing.app.ui.settings.V2AIConfigPresentation
import com.mygolfswing.app.ui.settings.V2DownloadHistoryScreen
import com.mygolfswing.app.ui.settings.V2OnlineDownloadScreen
import com.mygolfswing.app.ui.settings.V2StorageManagementScreen
import com.mygolfswing.app.ui.designsystem.DSColors
import com.mygolfswing.app.ui.designsystem.DSSpacing
import com.mygolfswing.app.ui.designsystem.DSTypography
import com.mygolfswing.app.ui.goals.V2AllGoalsScreen
import com.mygolfswing.app.ui.goals.V2GoalDetailScreen
import com.mygolfswing.app.ui.goals.V2NewGoalScreen
import com.mygolfswing.app.ui.home.V2HomeScreen
import com.mygolfswing.app.ui.memo.V2MemoCreateTextScreen
import com.mygolfswing.app.ui.memo.V2MemoCreateVideoScreen
import com.mygolfswing.app.ui.memo.V2MemoDetailScreen
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.repo.V2AppUpdatePresentation
import com.mygolfswing.app.data.store.V2AIChatStore
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.data.video.V2OnlineVideoMetadataFetcher
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.data.video.deleteFileCountingFreedBytes
import com.mygolfswing.app.ui.memo.V2MemoAllListScreen
import com.mygolfswing.app.ui.memo.V2MemoListScreen
import com.mygolfswing.app.ui.memo.V2MemoRecordBrowserScreen
import com.mygolfswing.app.ui.memo.V2MemoRecordFilter
import kotlinx.coroutines.delay
import com.mygolfswing.app.ui.memo.V2QuickAddSheet
import com.mygolfswing.app.ui.memo.V2TextMemoListScreen
import com.mygolfswing.app.ui.memo.V2VideoMemoListScreen
import com.mygolfswing.app.ui.memo.V2VideoMemoPlayerScreen
import com.mygolfswing.app.ui.plaza.V2CoachProfileScreen
import com.mygolfswing.app.ui.plaza.V2PlazaLoginTarget
import com.mygolfswing.app.ui.plaza.V2PlazaPlayerLoginTarget
import com.mygolfswing.app.ui.plaza.V2PlazaPlayerScreen
import com.mygolfswing.app.ui.plaza.V2PlazaWatchHistoryScreen
import com.mygolfswing.app.ui.profile.V2AIArchiveScreen
import com.mygolfswing.app.ui.profile.V2CreditsPurchaseScreen
import com.mygolfswing.app.ui.profile.V2CreditsResetScreen
import com.mygolfswing.app.ui.profile.V2EditProfileScreen
import com.mygolfswing.app.ui.profile.V2ProfileCloudConsistencyAuditScreen
import com.mygolfswing.app.ui.profile.V2ProfileLoginTarget
import com.mygolfswing.app.ui.profile.V2ProfileScreen
import com.mygolfswing.app.ui.profile.V2ProfileSettingsScreen
import com.mygolfswing.app.ui.profile.V2LearningFavoritesScreen
import com.mygolfswing.app.ui.tools.V2ToolsHomeScreen
import com.mygolfswing.app.ui.swing.V2JustSwingCalendarScreen
import com.mygolfswing.app.ui.swing.V2SwingAnalysisTrimScreen
import com.mygolfswing.app.ui.swing.V2SwingClipDetailScreen
import com.mygolfswing.app.ui.swing.V2SwingDayDetailScreen
import com.mygolfswing.app.ui.theme.MyGolfSwingAppAndroidTheme
import com.mygolfswing.app.ui.coach.V2CoachRootScreen
import com.mygolfswing.app.ui.role.V2RoleSelectionScreen
import com.mygolfswing.app.ui.interaction.V2StudentInteractionScreen
import com.mygolfswing.app.ui.interaction.V2DeliveryQueueDialog
import com.mygolfswing.app.ui.interaction.rememberQueueNetworkConnected
import com.mygolfswing.app.ui.update.V2AppUpdatePrompt
import com.mygolfswing.app.ui.update.V2LocalAccessContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.UUID

/**
 * 应用内导航目的地（轻量栈，无需引入 Navigation Compose）。
 */
private enum class V2AIConfigEntry { PROFILE, CHAT }

private sealed class Destination {
    data object Tabs : Destination()
    data object AllGoals : Destination()
    data class NewGoal(val editingId: UUID? = null) : Destination()
    data class GoalDetail(val id: UUID) : Destination()
    data object NewTextMemo : Destination()
    data class NewVideoMemo(
        val editingId: UUID? = null,
        val openPlayerAfterSave: Boolean = false,
    ) : Destination()
    data class MemoDetail(val id: UUID) : Destination()
    data class VideoMemoPlay(val id: UUID) : Destination()
    data class PlazaPlay(val id: UUID) : Destination()
    data class PlazaCoach(val coachId: String) : Destination()
    data object PlazaWatchHistory : Destination()
    data object AllMemos : Destination()
    data object VideoMemoList : Destination()
    data object TextMemoList : Destination()
    data class SwingCalendar(val initialFilter: V2SwingClipRetention? = null) : Destination()
    data class SwingDayDetail(val day: LocalDate) : Destination()
    data class SwingClipDetail(
        val clipId: UUID,
        val showCloseButton: Boolean = false,
    ) : Destination()
    data class JustSwingMain(
        val openClipAfterSave: Boolean = false,
        val sourceLabel: String? = null,
    ) : Destination()
    data class OnlineVideoPicker(val initialUrl: String = "") : Destination()
    data class OnlineDownload(
        val initialUrl: String = "",
        val allowDirectUrlDownload: Boolean = false,
    ) : Destination()
    data object DownloadHistory : Destination()
    data object AIArchive : Destination()
    data object AIChat : Destination()
    data object ProfileSettings : Destination()
    data object LearningFavorites : Destination()
    data object Login : Destination()
    data object Register : Destination()
    data object ResetPassword : Destination()
    data object CompleteProfile : Destination()
    data object EditProfile : Destination()
    data object AccountSecurity : Destination()
    data object AccountDeletion : Destination()
    data object StorageManagement : Destination()
    data object CreditsReset : Destination()
    data object CreditsPurchase : Destination()
    data class AIConfig(val entry: V2AIConfigEntry) : Destination()
    data object CloudConsistencyAudit : Destination()
    data object Feedback : Destination()
    data class Legal(val type: V2LegalDocType) : Destination()
}

internal data class V2PostAuthContinuationPresence(
    val destination: Boolean = false,
    val plaza: Boolean = false,
    val onlineMemo: Boolean = false,
    val aiHistory: Boolean = false,
    val plazaPlayer: Boolean = false,
    val profileQr: Boolean = false,
    val analysisSource: Boolean = false,
    val trajectory: Boolean = false,
    val sharedVideo: Boolean = false,
    val coachInvite: Boolean = false,
) {
    val hasBusinessContinuation: Boolean
        get() = destination || plaza || onlineMemo || aiHistory || plazaPlayer || profileQr ||
            analysisSource || trajectory || sharedVideo || coachInvite
}

internal fun shouldOpenCompleteProfileAfterAuth(
    forceCompleteProfile: Boolean,
    rawNickname: String?,
    continuation: V2PostAuthContinuationPresence,
): Boolean = !continuation.hasBusinessContinuation &&
    (forceCompleteProfile || rawNickname.isNullOrBlank())

/**
 * V2 Android 根容器（对齐 iOS `V2RootView`）。
 */
@Composable
fun V2RootScreen(
    modifier: Modifier = Modifier,
    sharedVideoUrl: String? = null,
    onSharedVideoUrlConsumed: () -> Unit = {},
    pendingCoachInvite: V2PendingCoachInvite? = null,
    onCoachInviteConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val rootLifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableStateOf(V2Tab.HOME) }
    var memoReturnFilter by remember { mutableStateOf<V2MemoRecordFilter?>(null) }
    var rootInteractionRoute by remember { mutableStateOf<StudentInteractionRoute?>(null) }
    val nav = rememberV2AppNavigation<Destination>(Destination.Tabs)
    var showQuickAdd by remember { mutableStateOf(false) }
    var bootstrapping by remember { mutableStateOf(true) }
    var pendingOnlinePick by remember { mutableStateOf<V2OnlineVideoSelection?>(null) }
    var showGolfCamTrajectory by remember { mutableStateOf(false) }
    var showAnalysisSource by remember { mutableStateOf(false) }
    var analysisImporting by remember { mutableStateOf(false) }
    var analysisImportError by remember { mutableStateOf<String?>(null) }
    var pendingAnalysisImport by remember { mutableStateOf<V2VideoFileStore.ImportResult?>(null) }
    var analysisTrimSaving by remember { mutableStateOf(false) }
    var analysisTrimError by remember { mutableStateOf<String?>(null) }
    var pendingSharedVideoUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var postLoginReturnDestination by remember { mutableStateOf<Destination?>(null) }
    var postLoginReturnTab by remember { mutableStateOf<V2Tab?>(null) }
    var postLoginContinueDestination by remember { mutableStateOf<Destination?>(null) }
    var postLoginPlazaTarget by remember { mutableStateOf<V2PlazaLoginTarget?>(null) }
    var postLoginOnlineMemoUrl by remember { mutableStateOf<String?>(null) }
    var postLoginOpenAIHistory by remember { mutableStateOf(false) }
    var postLoginPlazaPlayerTarget by remember { mutableStateOf<V2PlazaPlayerLoginTarget?>(null) }
    var postLoginPlazaPlayerDownloadUrl by remember { mutableStateOf<String?>(null) }
    var plazaPlayerLoginActionRequest by remember { mutableStateOf<V2PlazaPlayerLoginTarget?>(null) }
    var postLoginOpenProfileQr by remember { mutableStateOf(false) }
    var postLoginOpenAnalysisSource by remember { mutableStateOf(false) }
    var postLoginOpenTrajectory by remember { mutableStateOf(false) }
    var profileQrRequestKey by remember { mutableIntStateOf(0) }
    var aiHistoryRequestKey by remember { mutableIntStateOf(0) }
    var feedbackDraftType by rememberSaveable { mutableStateOf("功能建议") }
    var feedbackDraftContent by rememberSaveable { mutableStateOf("") }
    var feedbackDraftContact by rememberSaveable { mutableStateOf("") }
    var showRoleSelection by rememberSaveable { mutableStateOf(false) }
    var roleSelectionAllowsDismiss by rememberSaveable { mutableStateOf(true) }
    var rootAuthPrompt by remember { mutableStateOf<V2RootAuthPrompt?>(null) }
    var kickOutMessage by remember { mutableStateOf<String?>(null) }
    var appUpdatePresentation by remember { mutableStateOf<V2AppUpdatePresentation?>(null) }
    var localAccessPresentation by remember { mutableStateOf<V2AppUpdatePresentation?>(null) }
    var localAccessDestination by remember { mutableStateOf<Destination?>(null) }
    var foregroundValidationRequest by remember { mutableIntStateOf(0) }
    val profile by V2ProfileStore.profile.collectAsState()
    val currentSession by ServiceLocator.session.state.collectAsState(initial = V2Session())
    val roleState by ServiceLocator.roleStore.state.collectAsState()
    val rootDeliveryState by ServiceLocator.coachDeliveryQueue.state.collectAsState()
    val hasActiveRootDelivery = rootDeliveryState.jobsForOwner(currentSession.userId).isNotEmpty()
    val currentAppVersion = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty().ifBlank { "0" }
    }

    suspend fun validateRootSession(refreshBusinessData: Boolean) {
        val cachedSession = ServiceLocator.session.current()
        if (!cachedSession.isLoggedIn) return
        val validationResult = ServiceLocator.authRepo.fetchUserInfo()
        val validation = classifyRootSessionValidation(validationResult)
        if (validation == V2RootSessionValidation.INVALID) {
            rootAuthPrompt = null
            kickOutMessage = "您的账号已被注销，请重新注册"
            return
        }
        val refreshedSession = ServiceLocator.session.current()
        ServiceLocator.roleStore.synchronize(refreshedSession)
        if (!refreshBusinessData) return
        V2PlazaStore.refresh()
        V2GoalStore.syncFromCloud()
        refreshedSession.userId?.let { uid -> V2CategoryStore.loadForUser(uid) }
        ServiceLocator.goalRepo.pushDirty()
        ServiceLocator.creditsRepo.fetchBalance(force = true)
        V2MemoStore.pullFromCloud()
        V2SwingSessionStore.cleanupExpiredTemporaryClips()
        runRootBackgroundSyncCatching { V2GolfProfileStore.refreshFromCloud() }
    }

    fun confirmKickOut() {
        kickOutMessage = null
        rootAuthPrompt = null
        scope.launch {
            V2ProfileStore.resetUserScopedCaches()
            ServiceLocator.authRepo.logout()
            selectedTab = V2Tab.HOME
            nav.resetToRoot()
            ServiceLocator.roleStore.synchronize(ServiceLocator.session.current())
        }
    }

    LaunchedEffect(currentSession.isLoggedIn) {
        if (currentSession.isLoggedIn) rootAuthPrompt = null
    }

    LaunchedEffect(Unit) {
        ServiceLocator.tencentFriendRepo.sessionEvents.collect { event ->
            when (event) {
                V2TencentSessionEvent.KICKED_OFFLINE -> {
                    rootAuthPrompt = null
                    kickOutMessage = "您的账号已在「其他设备」上登录"
                }
                V2TencentSessionEvent.USER_SIG_EXPIRED -> {
                    ServiceLocator.authRepo.refreshUserSig()
                        .onSuccess {
                            ServiceLocator.tencentFriendRepo.reconnect(ServiceLocator.session.current())
                        }
                }
            }
        }
    }

    LaunchedEffect(bootstrapping) {
        if (bootstrapping) return@LaunchedEffect
        ServiceLocator.appUpdateRepo.check(currentAppVersion)
            .onSuccess { appUpdatePresentation = it }
        // 更新服务异常时静默放行，不能成为应用启动依赖。
    }

    val analysisVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            analysisImporting = false
            if (pendingAnalysisImport == null) showAnalysisSource = true
            return@rememberLauncherForActivityResult
        }
        if (analysisImporting || pendingAnalysisImport != null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        showAnalysisSource = true
        analysisImporting = true
        scope.launch {
            analysisImportError = null
            analysisTrimError = null
            try {
                when (
                    val outcome = importHomeAnalysisVideo {
                        V2VideoFileStore.importVideo(context, uri)
                    }
                ) {
                    is HomeAnalysisImportOutcome.Ready -> {
                        if (pendingAnalysisImport == null) pendingAnalysisImport = outcome.imported
                        showAnalysisSource = false
                    }
                    is HomeAnalysisImportOutcome.Failure -> {
                        analysisImportError = outcome.message
                        showAnalysisSource = true
                    }
                }
            } finally {
                analysisImporting = false
            }
        }
    }

    fun cancelPendingAnalysisTrim(showSourceAgain: Boolean = true) {
        if (analysisTrimSaving) return
        pendingAnalysisImport?.let(::cleanupImportedAnalysisClip)
        pendingAnalysisImport = null
        analysisTrimError = null
        analysisImportError = null
        showAnalysisSource = showSourceAgain
    }

    fun openSharedVideoUrl(rawUrl: String) {
        val cleaned = V2OnlineVideoMetadataFetcher.cleanVideoUrl(rawUrl)
        if (!cleaned.startsWith("http://", ignoreCase = true) && !cleaned.startsWith("https://", ignoreCase = true)) return
        showQuickAdd = false
        showAnalysisSource = false
        selectedTab = V2Tab.HOME
        when (V2OnlineVideoMetadataFetcher.detectPlatform(cleaned)) {
            "抖音" -> nav.navigateTo(Destination.OnlineDownload(initialUrl = cleaned))
            else -> {
                pendingOnlinePick = null
                nav.navigateTo(Destination.NewVideoMemo())
                nav.navigateTo(Destination.OnlineVideoPicker(initialUrl = cleaned))
            }
        }
    }

    fun openLogin(returnTo: Destination? = nav.current, continueTo: Destination? = null) {
        showQuickAdd = false
        showAnalysisSource = false
        analysisImportError = null
        postLoginReturnDestination = returnTo?.takeUnless { it == Destination.Login }
        postLoginReturnTab = selectedTab
        postLoginContinueDestination = continueTo
        nav.navigateTo(Destination.Login)
    }

    fun completePostAuthRefresh(forceCompleteProfile: Boolean = false) {
        val returnDestination = postLoginReturnDestination?.takeUnless { it == Destination.Login }
        val returnTab = postLoginReturnTab
        val continueDestination = postLoginContinueDestination
        val plazaTarget = postLoginPlazaTarget
        val onlineMemoUrl = postLoginOnlineMemoUrl
        val openAIHistory = postLoginOpenAIHistory
        val plazaPlayerTarget = postLoginPlazaPlayerTarget
        val plazaPlayerDownloadUrl = postLoginPlazaPlayerDownloadUrl
        val openProfileQr = postLoginOpenProfileQr
        val openAnalysisSource = postLoginOpenAnalysisSource
        val openTrajectory = postLoginOpenTrajectory
        val hasPendingSharedVideo = pendingSharedVideoUrl != null
        val shouldOpenCompleteProfile = shouldOpenCompleteProfileAfterAuth(
            forceCompleteProfile = forceCompleteProfile,
            rawNickname = currentSession.nickname,
            continuation = V2PostAuthContinuationPresence(
                destination = continueDestination != null,
                plaza = plazaTarget != null,
                onlineMemo = onlineMemoUrl != null,
                aiHistory = openAIHistory,
                plazaPlayer = plazaPlayerTarget != null,
                profileQr = openProfileQr,
                analysisSource = openAnalysisSource,
                trajectory = openTrajectory,
                sharedVideo = hasPendingSharedVideo,
                coachInvite = pendingCoachInvite != null,
            ),
        )
        postLoginReturnDestination = null
        postLoginReturnTab = null
        postLoginContinueDestination = null
        postLoginPlazaTarget = null
        postLoginOnlineMemoUrl = null
        postLoginOpenAIHistory = false
        postLoginPlazaPlayerTarget = null
        postLoginPlazaPlayerDownloadUrl = null
        postLoginOpenProfileQr = false
        postLoginOpenAnalysisSource = false
        postLoginOpenTrajectory = false
        showQuickAdd = false
        selectedTab = returnTab ?: V2Tab.HOME
        if (returnDestination == null) {
            nav.resetToRoot()
        } else {
            nav.returnTo(returnDestination)
        }
        when (plazaTarget) {
            V2PlazaLoginTarget.SAVED_COLLECTION -> {
                nav.navigateTo(Destination.LearningFavorites)
            }
            V2PlazaLoginTarget.WATCH_HISTORY -> nav.navigateTo(Destination.PlazaWatchHistory)
            null -> Unit
        }
        continueDestination?.let { nav.navigateTo(it) }
        onlineMemoUrl?.let { url ->
            pendingOnlinePick = null
            nav.navigateTo(Destination.NewVideoMemo())
            nav.navigateTo(Destination.OnlineVideoPicker(initialUrl = url))
        }
        if (openAIHistory) {
            aiHistoryRequestKey += 1
            nav.navigateTo(Destination.AIChat)
        }
        if (openProfileQr) {
            profileQrRequestKey += 1
        }
        if (openAnalysisSource) {
            selectedTab = V2Tab.HOME
            showAnalysisSource = true
        }
        if (openTrajectory) {
            selectedTab = V2Tab.HOME
            showGolfCamTrajectory = true
        }
        when (plazaPlayerTarget) {
            V2PlazaPlayerLoginTarget.COMMENTS,
            V2PlazaPlayerLoginTarget.FEEDBACK -> plazaPlayerLoginActionRequest = plazaPlayerTarget
            V2PlazaPlayerLoginTarget.DOWNLOAD -> {
                plazaPlayerDownloadUrl?.let { url ->
                    nav.navigateTo(Destination.OnlineDownload(initialUrl = url, allowDirectUrlDownload = true))
                }
            }
            null -> Unit
        }
        if (shouldOpenCompleteProfile) {
            nav.navigateTo(Destination.CompleteProfile)
        }
        scope.launch {
            val latest = ServiceLocator.session.current()
            ServiceLocator.roleStore.synchronize(latest)
            if (pendingCoachInvite != null) {
                ServiceLocator.roleStore.select(V2ExperienceRole.STUDENT)
            } else if (latest.coachAuthorizationStatus.equals("approved", ignoreCase = true)) {
                ServiceLocator.roleStore.prepareForFreshLoginChoice()
                roleSelectionAllowsDismiss = false
                showRoleSelection = true
            }
        }
        V2ProfileStore.refresh(force = true)
        V2PlazaStore.refresh()
        V2MemoStore.pullFromCloud()
        V2GoalStore.syncFromCloud()
        ServiceLocator.appScope.launch {
            ServiceLocator.session.current().userId?.let { V2CategoryStore.loadForUser(it) }
            ServiceLocator.creditsRepo.fetchBalance(force = true)
            runRootBackgroundSyncCatching { V2GolfProfileStore.refreshFromCloud() }
        }
        pendingSharedVideoUrl?.let { url ->
            openSharedVideoUrl(url)
            pendingSharedVideoUrl = null
            onSharedVideoUrlConsumed()
        }
        if (pendingCoachInvite != null) {
            selectedTab = V2Tab.INTERACTION
            nav.resetToRoot()
        }
    }

    fun requireLoginForAccountContent(destination: Destination) {
        showQuickAdd = false
        if (V2ProfileStore.profile.value.isLoggedIn) {
            nav.navigateTo(destination)
        } else {
            openLogin(continueTo = destination)
        }
    }

    fun openLoginForProfileTarget(target: V2ProfileLoginTarget) {
        val destination = when (target) {
            V2ProfileLoginTarget.EDIT_PROFILE -> Destination.EditProfile
            V2ProfileLoginTarget.ALL_GOALS -> Destination.AllGoals
            V2ProfileLoginTarget.VIDEO_MEMOS -> Destination.VideoMemoList
            V2ProfileLoginTarget.SWING_CALENDAR -> Destination.SwingCalendar()
            V2ProfileLoginTarget.AI_ARCHIVE -> Destination.AIArchive
            V2ProfileLoginTarget.CREDITS -> Destination.CreditsReset
            V2ProfileLoginTarget.ACCOUNT_SECURITY -> Destination.AccountSecurity
        }
        openLogin(continueTo = destination)
    }

    fun openLoginForPlazaTarget(target: V2PlazaLoginTarget) {
        postLoginPlazaTarget = when {
            target == V2PlazaLoginTarget.WATCH_HISTORY && nav.current == Destination.PlazaWatchHistory -> null
            else -> target
        }
        openLogin()
    }

    fun openLoginForOnlineMemo(url: String) {
        val cleaned = V2OnlineVideoMetadataFetcher.cleanVideoUrl(url)
        postLoginOnlineMemoUrl = cleaned.takeIf {
            it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)
        }
        openLogin()
    }

    fun openLoginForAIHistory() {
        postLoginOpenAIHistory = true
        openLogin()
    }

    fun openLoginForPlazaPlayerTarget(target: V2PlazaPlayerLoginTarget, url: String?) {
        postLoginPlazaPlayerTarget = target
        postLoginPlazaPlayerDownloadUrl = url?.let(V2OnlineVideoMetadataFetcher::cleanVideoUrl)?.takeIf {
            it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)
        }
        openLogin()
    }

    fun openLoginForProfileQr() {
        postLoginOpenProfileQr = true
        openLogin()
    }

    LaunchedEffect(sharedVideoUrl) {
        sharedVideoUrl
            ?.let(V2OnlineVideoMetadataFetcher::cleanVideoUrl)
            ?.takeIf { it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true) }
            ?.let { pendingSharedVideoUrl = it }
    }

    LaunchedEffect(Unit) {
        val splashStartedAt = System.currentTimeMillis()
        V2UserScopedCache.startSessionGuard()
        V2GoalStore.startObserving()
        V2MemoStore.startObserving()
        V2PlazaStore.startObserving()
        V2ProfileStore.startObserving()
        V2GolfProfileStore.startObserving()
        V2SwingSessionStore.startObserving()
        V2AIChatStore.loadPersisted()
        V2MemoStore.deleteLocalDemoMemos()
        val session = ServiceLocator.session.current()
        if (!session.isLoggedIn) {
            selectedTab = V2Tab.HOME
            nav.resetToRoot()
        }
        ServiceLocator.roleStore.synchronize(session)
        val remainingSplashMs = splashRemainingDelayMillis(
            startedAtMillis = splashStartedAt,
            nowMillis = System.currentTimeMillis(),
        )
        if (remainingSplashMs > 0) delay(remainingSplashMs)
        bootstrapping = false
    }

    // Match iOS V2: the 0.8s splash is only a brand transition. Account validation
    // and cloud refresh begin after the first usable screen is already visible.
    LaunchedEffect(bootstrapping) {
        if (bootstrapping) return@LaunchedEffect
        validateRootSession(refreshBusinessData = true)
    }

    // iOS revalidates the account and update policy whenever the app returns to the foreground.
    V2RootForegroundObserver(
        lifecycleOwner = rootLifecycleOwner,
        enabled = !bootstrapping,
        isLoggedIn = currentSession.isLoggedIn,
        onForeground = { foregroundValidationRequest += 1 },
    )

    LaunchedEffect(foregroundValidationRequest) {
        if (foregroundValidationRequest == 0) return@LaunchedEffect
        ServiceLocator.appUpdateRepo.check(currentAppVersion)
            .onSuccess { appUpdatePresentation = it }
        validateRootSession(refreshBusinessData = false)
    }

    LaunchedEffect(bootstrapping, pendingSharedVideoUrl) {
        val url = pendingSharedVideoUrl ?: return@LaunchedEffect
        if (bootstrapping) return@LaunchedEffect
        if (ServiceLocator.session.current().isLoggedIn) {
            openSharedVideoUrl(url)
            pendingSharedVideoUrl = null
            onSharedVideoUrlConsumed()
        } else {
            nav.resetTo(Destination.Login)
        }
    }

    LaunchedEffect(bootstrapping, pendingCoachInvite, currentSession.isLoggedIn) {
        if (bootstrapping || pendingCoachInvite == null) return@LaunchedEffect
        if (currentSession.isLoggedIn) {
            ServiceLocator.roleStore.select(V2ExperienceRole.STUDENT)
            selectedTab = V2Tab.INTERACTION
            nav.resetToRoot()
        } else {
            openLogin(returnTo = null)
        }
    }

    LaunchedEffect(
        bootstrapping,
        currentSession.userId,
        currentSession.coachAuthorizationStatus,
        currentSession.isLoggedIn,
    ) {
        if (!bootstrapping) ServiceLocator.roleStore.synchronize(currentSession)
    }

    // 系统返回滑不动就退出。没有上一页时停在当前页，离开应用只走底部上滑。
    V2BackHandler { }

    if (bootstrapping) {
        V2SplashScreen()
        return
    }

    localAccessPresentation?.let { requiredUpdate ->
        when (val localDestination = localAccessDestination) {
            is Destination.MemoDetail -> V2MemoDetailScreen(
                memoId = localDestination.id,
                onBack = { localAccessDestination = null },
            )
            is Destination.VideoMemoPlay -> V2VideoMemoPlayerScreen(
                memoId = localDestination.id,
                onBack = { localAccessDestination = null },
                onEdit = {},
                onMemoCreated = {},
            )
            is Destination.SwingClipDetail -> V2SwingClipDetailScreen(
                clipId = localDestination.clipId,
                showCloseButton = localDestination.showCloseButton,
                onBack = { localAccessDestination = null },
            )
            else -> V2LocalAccessContent(
                onUpdate = {
                    appUpdatePresentation = requiredUpdate
                    localAccessPresentation = null
                    localAccessDestination = null
                },
            ) {
                V2MemoRecordBrowserScreen(
                    isLoggedIn = currentSession.isLoggedIn,
                    readOnly = true,
                    applyStatusBarPadding = false,
                    onCreateVideoMemo = {},
                    onCreateTextMemo = {},
                    onOpenMemo = { memoId ->
                        val memo = V2MemoStore.allMemos.value.firstOrNull { it.id == memoId }
                        localAccessDestination = if (memo?.kind == V2MemoKind.TEXT) {
                            Destination.MemoDetail(memoId)
                        } else {
                            Destination.VideoMemoPlay(memoId)
                        }
                    },
                    onOpenSwingClip = {
                        localAccessDestination = Destination.SwingClipDetail(it, showCloseButton = true)
                    },
                    onOpenJustSwing = {},
                    onRequestLogin = {},
                )
            }
        }
        return
    }

    if (showRoleSelection) {
        Box(Modifier.fillMaxSize()) {
            V2RoleSelectionScreen(
                session = currentSession,
                allowsDismiss = roleSelectionAllowsDismiss,
                activeRole = roleState.activeRole,
                onBack = { showRoleSelection = false },
                onSelect = { role ->
                    scope.launch {
                        if (ServiceLocator.roleStore.select(role)) showRoleSelection = false
                    }
                },
            )
            kickOutMessage?.let { message ->
                V2KickOutAlertDialog(message = message, onConfirm = ::confirmKickOut)
            }
            appUpdatePresentation?.let { presentation ->
                V2AppUpdatePrompt(
                    presentation = presentation,
                    onUpdate = { openAndroidStore(context, presentation) },
                    onRemindLater = {
                        ServiceLocator.appUpdateRepo.remindLater(presentation)
                        appUpdatePresentation = null
                    },
                    onContinueLocally = {
                        if (presentation.policy.allowLocalAccess) {
                            localAccessPresentation = presentation
                            localAccessDestination = null
                            appUpdatePresentation = null
                        }
                    },
                )
            }
        }
        return
    }

    if (roleState.activeRole == V2ExperienceRole.COACH && roleState.canUseCoachExperience) {
        Box(Modifier.fillMaxSize()) {
            V2CoachRootScreen(
                onSwitchToStudent = {
                    roleSelectionAllowsDismiss = true
                    showRoleSelection = true
                },
                onLogoutComplete = {
                    selectedTab = V2Tab.HOME
                    nav.resetToRoot()
                },
            )
            kickOutMessage?.let { message ->
                V2KickOutAlertDialog(message = message, onConfirm = ::confirmKickOut)
            }
            appUpdatePresentation?.let { presentation ->
                V2AppUpdatePrompt(
                    presentation = presentation,
                    onUpdate = { openAndroidStore(context, presentation) },
                    onRemindLater = {
                        ServiceLocator.appUpdateRepo.remindLater(presentation)
                        appUpdatePresentation = null
                    },
                    onContinueLocally = {
                        if (presentation.policy.allowLocalAccess) {
                            localAccessPresentation = presentation
                            localAccessDestination = null
                            appUpdatePresentation = null
                        }
                    },
                )
            }
        }
        return
    }

    V2BackHandler(enabled = nav.canGoBack && nav.current != Destination.Login) { nav.navigateBack() }

    when (val d = nav.current) {
        Destination.Tabs -> TabsScaffold(
            selectedTab = selectedTab,
            externalInteractionRoute = rootInteractionRoute,
            onExternalInteractionRouteConsumed = { rootInteractionRoute = null },
            onSelectTab = { tab ->
                val prompt = protectedTabAuthPrompt(tab, currentSession.isLoggedIn)
                if (prompt != null) {
                    rootAuthPrompt = prompt
                } else {
                    selectedTab = tab
                    if (tab == V2Tab.ME) {
                        V2ProfileStore.refresh(force = true)
                    }
                }
            },
            onShowQuickAdd = { showQuickAdd = true },
            onOpenAllGoals = { nav.navigateTo(Destination.AllGoals) },
            onOpenGoalDetail = { nav.navigateTo(Destination.GoalDetail(it)) },
            onCreateGoal = { requireLoginForAccountContent(Destination.NewGoal()) },
            onCreateTextMemo = { requireLoginForAccountContent(Destination.NewTextMemo) },
            onCreateVideoMemo = { requireLoginForAccountContent(Destination.NewVideoMemo()) },
            requestedMemoFilter = memoReturnFilter,
            onRequestedMemoFilterConsumed = { memoReturnFilter = null },
            onOpenAllMemos = { nav.navigateTo(Destination.AllMemos) },
            onOpenVideoMemoList = { nav.navigateTo(Destination.VideoMemoList) },
            onOpenTextMemoList = { nav.navigateTo(Destination.TextMemoList) },
            onOpenSwingCalendar = { requireLoginForAccountContent(Destination.SwingCalendar()) },
            onOpenSwingClip = { clipId, showCloseButton ->
                nav.navigateTo(Destination.SwingClipDetail(clipId, showCloseButton))
            },
            onOpenGrowthPool = { requireLoginForAccountContent(Destination.SwingCalendar(initialFilter = V2SwingClipRetention.PROMOTED)) },
            onOpenJustSwing = { requireLoginForAccountContent(Destination.JustSwingMain()) },
            onOpenAIArchive = { nav.navigateTo(Destination.AIArchive) },
            onOpenProfileSettings = { nav.navigateTo(Destination.ProfileSettings) },
            onOpenAIConfig = { nav.navigateTo(Destination.AIConfig(V2AIConfigEntry.PROFILE)) },
            onSwitchToAI = { nav.navigateTo(Destination.AIChat) },
            onOpenHomeAI = { requireLoginForAccountContent(Destination.AIChat) },
            onOpenMemoDetail = { memo ->
                val m = V2MemoStore.allMemos.value.firstOrNull { it.id == memo }
                nav.navigateTo(
                    if (m?.kind == V2MemoKind.TEXT) {
                        Destination.MemoDetail(memo)
                    } else {
                        Destination.VideoMemoPlay(memo)
                    },
                )
            },
            onOpenPlazaVideo = { nav.navigateTo(Destination.PlazaPlay(it)) },
            onRequestLogin = { openLogin() },
            onRequestLoginForProfileTarget = ::openLoginForProfileTarget,
            onRequestLoginForPlazaTarget = ::openLoginForPlazaTarget,
            onOpenEditProfile = { nav.navigateTo(Destination.EditProfile) },
            onOpenAccountSecurity = { nav.navigateTo(Destination.AccountSecurity) },
            onOpenStorage = { nav.navigateTo(Destination.StorageManagement) },
            onOpenResetCredits = { nav.navigateTo(Destination.CreditsReset) },
            onOpenFeedback = { nav.navigateTo(Destination.Feedback) },
            onOpenTerms = { nav.navigateTo(Destination.Legal(V2LegalDocType.TERMS)) },
            onOpenPrivacy = { nav.navigateTo(Destination.Legal(V2LegalDocType.PRIVACY)) },
            onLogoutComplete = {
                showQuickAdd = false
                showAnalysisSource = false
                cancelPendingAnalysisTrim(showSourceAgain = false)
                selectedTab = V2Tab.HOME
                nav.resetToRoot()
            },
            showRoleSwitcher = roleState.canUseCoachExperience,
            onSwitchRole = {
                roleSelectionAllowsDismiss = true
                showRoleSelection = true
            },
            pendingCoachInvite = pendingCoachInvite,
            onCoachInviteConsumed = onCoachInviteConsumed,
            onOpenTrajectory = {
                if (profile.isLoggedIn) {
                    showGolfCamTrajectory = true
                } else {
                    postLoginOpenTrajectory = true
                    openLogin(returnTo = null)
                }
            },
            onOpenAnalysis = {
                if (profile.isLoggedIn) {
                    showAnalysisSource = true
                } else {
                    postLoginOpenAnalysisSource = true
                    openLogin(returnTo = null)
                }
            },
            onOpenOnlineDownload = {
                requireLoginForAccountContent(Destination.OnlineDownload())
            },
            modifier = modifier,
        )
        Destination.AllGoals -> V2AllGoalsScreen(
            onBack = { nav.navigateBack() },
            onCreate = { requireLoginForAccountContent(Destination.NewGoal()) },
            onSelectGoal = { nav.navigateTo(Destination.GoalDetail(it)) },
        )
        is Destination.NewGoal -> V2NewGoalScreen(
            editingGoalId = d.editingId,
            onClose = { nav.navigateBack() },
            onDeleted = {
                val deletedGoalId = d.editingId
                if (deletedGoalId == null) {
                    nav.resetToRoot()
                } else {
                    nav.navigateBackSkipping { it is Destination.GoalDetail && it.id == deletedGoalId }
                }
            },
        )
        is Destination.GoalDetail -> V2GoalDetailScreen(
            goalId = d.id,
            onBack = { nav.navigateBack() },
            onEdit = { nav.navigateTo(Destination.NewGoal(editingId = d.id)) },
        )
        Destination.NewTextMemo -> V2MemoCreateTextScreen(
            onClose = { nav.navigateBack() },
            onSaved = {
                memoReturnFilter = V2MemoRecordFilter.NOTE
                nav.navigateBack()
            },
        )
        is Destination.NewVideoMemo -> V2MemoCreateVideoScreen(
            onClose = { nav.navigateBack() },
            onSaved = { memoId ->
                memoReturnFilter = V2MemoRecordFilter.VIDEO
                if (d.openPlayerAfterSave) {
                    nav.resetTo(Destination.VideoMemoPlay(memoId))
                } else {
                    nav.navigateBack()
                }
            },
            onOpenOnlinePicker = { nav.navigateTo(Destination.OnlineVideoPicker()) },
            pendingOnline = pendingOnlinePick,
            onPendingOnlineConsumed = { pendingOnlinePick = null },
            editingMemoId = d.editingId,
        )
        is Destination.OnlineVideoPicker -> V2OnlineVideoSelectorScreen(
            onBack = { nav.navigateBack() },
            onConfirm = { pick ->
                pendingOnlinePick = pick
            },
            initialUrl = d.initialUrl,
        )
        is Destination.OnlineDownload -> V2OnlineDownloadScreen(
            onBack = { nav.navigateBack() },
            onMemoCreated = { memoId ->
                nav.navigateTo(Destination.NewVideoMemo(editingId = memoId, openPlayerAfterSave = true))
            },
            initialUrl = d.initialUrl,
            allowDirectUrlDownload = d.allowDirectUrlDownload,
            onRequestLogin = { openLogin() },
            onOpenHistory = {
                if (profile.isLoggedIn) {
                    nav.navigateTo(Destination.DownloadHistory)
                } else {
                    openLogin(continueTo = Destination.DownloadHistory)
                }
            },
            onOpenCreditsPurchase = { nav.navigateTo(Destination.CreditsPurchase) },
        )
        Destination.DownloadHistory -> V2DownloadHistoryScreen(
            onBack = { nav.navigateBack() },
            isLoggedIn = profile.isLoggedIn,
            onRequestLogin = { openLogin(continueTo = Destination.DownloadHistory) },
        )
        is Destination.JustSwingMain -> V2JustSwingMainScreen(
            onBack = { nav.navigateBack() },
            onOpenClips = { requireLoginForAccountContent(Destination.SwingCalendar()) },
            isLoggedIn = profile.isLoggedIn,
            onClipSaved = { clipId ->
                if (d.openClipAfterSave) {
                    nav.navigateTo(
                        Destination.SwingClipDetail(
                            clipId = clipId,
                            showCloseButton = true,
                        ),
                    )
                }
            },
            sourceLabel = d.sourceLabel,
            onRequestLogin = { openLogin(returnTo = null, continueTo = d) },
        )
        is Destination.MemoDetail -> V2MemoDetailScreen(
            memoId = d.id,
            onBack = { nav.navigateBack() },
        )
        is Destination.VideoMemoPlay -> V2VideoMemoPlayerScreen(
            memoId = d.id,
            onBack = { nav.navigateBack() },
            onEdit = { nav.navigateTo(Destination.NewVideoMemo(editingId = it)) },
            onMemoCreated = { nav.navigateTo(Destination.MemoDetail(it)) },
        )
        is Destination.PlazaPlay -> V2PlazaPlayerScreen(
            videoId = d.id,
            onClose = { nav.navigateBack() },
            onOpenCoach = { nav.navigateTo(Destination.PlazaCoach(it)) },
            onMemoCreated = { nav.navigateTo(Destination.VideoMemoPlay(it)) },
            onDownloadVideo = { url ->
                nav.navigateTo(Destination.OnlineDownload(initialUrl = url, allowDirectUrlDownload = true))
            },
            onRequestLogin = { openLogin() },
            onRequestLoginForTarget = ::openLoginForPlazaPlayerTarget,
            loginActionRequest = plazaPlayerLoginActionRequest,
            onLoginActionConsumed = { plazaPlayerLoginActionRequest = null },
        )
        is Destination.PlazaCoach -> V2CoachProfileScreen(
            coachId = d.coachId,
            onBack = { nav.navigateBack() },
            onOpenHome = {
                selectedTab = V2Tab.HOME
                nav.resetTo(Destination.Tabs)
            },
            onOpenVideo = { nav.navigateTo(Destination.PlazaPlay(it)) },
        )
        Destination.PlazaWatchHistory -> V2PlazaWatchHistoryScreen(
            onBack = { nav.navigateBack() },
            onOpenVideo = { nav.navigateTo(Destination.PlazaPlay(it)) },
            onRequestLogin = { openLoginForPlazaTarget(V2PlazaLoginTarget.WATCH_HISTORY) },
        )
        Destination.VideoMemoList -> V2VideoMemoListScreen(
            onBack = { nav.navigateBack() },
            isGuest = !profile.isLoggedIn,
            onOpenMemo = { memoId ->
                nav.navigateTo(Destination.VideoMemoPlay(memoId))
            },
            onCreateVideoMemo = { requireLoginForAccountContent(Destination.NewVideoMemo()) },
            onOpenDiscussion = { connectionId, itemId ->
                rootInteractionRoute = StudentInteractionRoute(connectionId, itemId)
                selectedTab = V2Tab.INTERACTION
                nav.resetTo(Destination.Tabs)
            },
            onRequestLogin = { openLogin(returnTo = Destination.VideoMemoList) },
        )
        Destination.TextMemoList -> V2TextMemoListScreen(
            onBack = { nav.navigateBack() },
            isGuest = !profile.isLoggedIn,
            onOpenMemo = { memoId ->
                nav.navigateTo(Destination.MemoDetail(memoId))
            },
            onCreateTextMemo = { requireLoginForAccountContent(Destination.NewTextMemo) },
            onRequestLogin = { openLogin(returnTo = Destination.TextMemoList) },
        )
        is Destination.SwingCalendar -> V2JustSwingCalendarScreen(
            initialFilter = d.initialFilter,
            onBack = { nav.navigateBack() },
            onOpenClip = { nav.navigateTo(Destination.SwingClipDetail(it)) },
            onStartRecording = { nav.navigateTo(Destination.JustSwingMain()) },
        )
        is Destination.SwingDayDetail -> V2SwingDayDetailScreen(
            day = d.day,
            onBack = { nav.navigateBack() },
        )
        is Destination.SwingClipDetail -> V2SwingClipDetailScreen(
            clipId = d.clipId,
            showCloseButton = d.showCloseButton,
            onBack = { nav.navigateBack() },
        )
        Destination.AIArchive -> V2AIArchiveScreen(
            onBack = { nav.navigateBack() },
            onRequestLogin = { openLogin(returnTo = Destination.AIArchive) },
        )
        Destination.AIChat -> V2AIChatScreen(
            onSwitchToHome = { nav.navigateBack() },
            onOpenConfig = { nav.navigateTo(Destination.AIConfig(V2AIConfigEntry.CHAT)) },
            onRequestLogin = { openLogin(returnTo = Destination.AIChat) },
            onRequestLoginForHistory = ::openLoginForAIHistory,
            historyRequestKey = aiHistoryRequestKey,
        )
        Destination.ProfileSettings -> V2ProfileSettingsScreen(
            onBack = { nav.navigateBack() },
            onRequestLogin = { openLogin() },
            onRequestLoginForQr = ::openLoginForProfileQr,
            onOpenLearningFavorites = {
                requireLoginForAccountContent(Destination.LearningFavorites)
            },
            coachAuthorizationStatus = roleState.authorizationStatus,
            coachPreviewEnabled = roleState.isCoachPreviewEnabled,
            isDebuggable = ServiceLocator.isDebuggable,
            hasActiveDelivery = hasActiveRootDelivery,
            onSwitchToCoach = {
                if (!hasActiveRootDelivery) {
                    scope.launch {
                        if (ServiceLocator.roleStore.select(V2ExperienceRole.COACH)) {
                            nav.navigateBack()
                        }
                    }
                }
            },
            onEnableCoachPreview = {
                if (!hasActiveRootDelivery) {
                    scope.launch {
                        val enabled = ServiceLocator.roleStore.setCoachPreviewEnabled(true)
                        if (enabled && ServiceLocator.roleStore.select(V2ExperienceRole.COACH)) {
                            nav.navigateBack()
                        }
                    }
                }
            },
            qrRequestKey = profileQrRequestKey,
        )
        Destination.LearningFavorites -> V2LearningFavoritesScreen(
            onBack = { nav.navigateBack() },
        )
        Destination.AllMemos -> V2MemoAllListScreen(
            onBack = { nav.navigateBack() },
            isGuest = !profile.isLoggedIn,
            onOpenMemo = { memoId ->
                val m = V2MemoStore.allMemos.value.firstOrNull { it.id == memoId }
                nav.navigateTo(
                    if (m?.kind == V2MemoKind.TEXT) {
                        Destination.MemoDetail(memoId)
                    } else {
                        Destination.VideoMemoPlay(memoId)
                    },
                )
            },
            onOpenSwingCalendar = { nav.navigateTo(Destination.SwingCalendar()) },
            onOpenSwingClip = { nav.navigateTo(Destination.SwingClipDetail(it, showCloseButton = true)) },
            onOpenDiscussion = { connectionId, itemId ->
                rootInteractionRoute = StudentInteractionRoute(connectionId, itemId)
                selectedTab = V2Tab.INTERACTION
                nav.resetTo(Destination.Tabs)
            },
            onRequestLogin = { openLogin(returnTo = Destination.AllMemos) },
        )
        Destination.Login -> V2LoginScreen(
            onClose = null,
            onLoginSuccess = { completePostAuthRefresh() },
            onGuest = {
                postLoginOpenTrajectory = false
                selectedTab = V2Tab.HOME
                nav.resetToRoot()
            },
            onOpenRegister = { nav.navigateTo(Destination.Register) },
            onOpenResetPassword = { nav.navigateTo(Destination.ResetPassword) },
            onOpenTerms = { nav.navigateTo(Destination.Legal(V2LegalDocType.TERMS)) },
            onOpenPrivacy = { nav.navigateTo(Destination.Legal(V2LegalDocType.PRIVACY)) },
        )
        Destination.Register -> V2RegisterScreen(
            onBack = { nav.navigateBack() },
            onRegisterSuccess = { completePostAuthRefresh(forceCompleteProfile = true) },
            onGuest = {
                selectedTab = V2Tab.HOME
                nav.resetToRoot()
            },
            onOpenTerms = { nav.navigateTo(Destination.Legal(V2LegalDocType.TERMS)) },
            onOpenPrivacy = { nav.navigateTo(Destination.Legal(V2LegalDocType.PRIVACY)) },
        )
        Destination.ResetPassword -> V2ResetPasswordScreen(
            onBack = { nav.navigateBack() },
            onResetSuccess = { completePostAuthRefresh() },
        )
        Destination.CompleteProfile -> V2CompleteProfileScreen(
            onComplete = {
                nav.navigateBack()
                V2ProfileStore.refresh(force = true)
            },
        )
        Destination.EditProfile -> V2EditProfileScreen(
            onBack = { nav.navigateBack() },
            onSaved = {
                nav.navigateBack()
                V2ProfileStore.refresh(force = true)
            },
        )
        Destination.AccountSecurity -> V2AccountSecurityScreen(
            onBack = { nav.navigateBack() },
            onOpenTerms = { nav.navigateTo(Destination.Legal(V2LegalDocType.TERMS)) },
            onOpenPrivacy = { nav.navigateTo(Destination.Legal(V2LegalDocType.PRIVACY)) },
            onOpenCloudAudit = { nav.navigateTo(Destination.CloudConsistencyAudit) },
            onOpenAccountDeletion = { nav.navigateTo(Destination.AccountDeletion) },
            onLogoutComplete = {
                showQuickAdd = false
                selectedTab = V2Tab.HOME
                nav.resetToRoot()
            },
            hasActiveDelivery = hasActiveRootDelivery,
        )
        Destination.AccountDeletion -> V2AccountDeletionScreen(
            onBack = { nav.navigateBack() },
            onDeletionComplete = {
                selectedTab = V2Tab.HOME
                nav.resetToRoot()
            },
            onRequestLogin = { openLogin() },
        )
        Destination.StorageManagement -> V2StorageManagementScreen(
            onBack = { nav.navigateBack() },
        )
        Destination.CreditsReset -> V2CreditsResetScreen(
            onBack = { nav.navigateBack() },
            onRequestLogin = { openLogin() },
        )
        Destination.CreditsPurchase -> V2CreditsPurchaseScreen(
            onBack = { nav.navigateBack() },
            onRequestLogin = { openLogin() },
        )
        is Destination.AIConfig -> V2AIServiceConfigScreen(
            onBack = { nav.navigateBack() },
            presentation = when (d.entry) {
                V2AIConfigEntry.PROFILE -> V2AIConfigPresentation.PROFILE
                V2AIConfigEntry.CHAT -> V2AIConfigPresentation.CHAT
            },
        )
        Destination.CloudConsistencyAudit -> V2ProfileCloudConsistencyAuditScreen(
            onBack = { nav.navigateBack() },
        )
        Destination.Feedback -> V2FeedbackScreen(
            onBack = {
                feedbackDraftType = "功能建议"
                feedbackDraftContent = ""
                feedbackDraftContact = ""
                nav.navigateBack()
            },
            onRequestLogin = { openLogin(returnTo = Destination.Feedback) },
            initialDraft = V2FeedbackDraft(
                typeLabel = feedbackDraftType,
                content = feedbackDraftContent,
                contact = feedbackDraftContact,
            ),
            onDraftChange = { draft ->
                feedbackDraftType = draft.typeLabel
                feedbackDraftContent = draft.content
                feedbackDraftContact = draft.contact
            },
        )
        is Destination.Legal -> V2LegalScreen(
            type = d.type,
            onBack = { nav.navigateBack() },
        )
    }

    if (showQuickAdd) {
        val isHomeTab = selectedTab == V2Tab.HOME
        V2QuickAddSheet(
            showCreateGoal = isHomeTab,
            isGuest = !profile.isLoggedIn,
            onCreateGoal = { requireLoginForAccountContent(Destination.NewGoal()) },
            onCreateVideoMemo = { requireLoginForAccountContent(Destination.NewVideoMemo()) },
            onCreateTextMemo = { requireLoginForAccountContent(Destination.NewTextMemo) },
            onCreateSwingClip = {
                showQuickAdd = false
                requireLoginForAccountContent(Destination.JustSwingMain())
            },
            onDismiss = { showQuickAdd = false },
        )
    }

    if (showGolfCamTrajectory) {
        com.mygolfswing.app.ui.golfcam.GolfCamTrajectoryEntry(
            onDismiss = { showGolfCamTrajectory = false },
        )
    }

    if (showAnalysisSource) {
        AnalysisSourceSheet(
            importing = analysisImporting,
            error = analysisImportError,
            onPickAlbum = {
                if (!profile.isLoggedIn) {
                    postLoginOpenAnalysisSource = true
                    openLogin(returnTo = null)
                } else {
                    analysisImportError = null
                    showAnalysisSource = false
                    val launchError = launchHomeAnalysisAlbumPicker {
                        analysisVideoPicker.launch(arrayOf("video/*"))
                    }
                    if (launchError != null) {
                        analysisImporting = false
                        showAnalysisSource = true
                        analysisImportError = launchError
                    }
                }
            },
            onRecord = {
                showAnalysisSource = false
                analysisImportError = null
                requireLoginForAccountContent(
                    Destination.JustSwingMain(
                        openClipAfterSave = true,
                        sourceLabel = "首页回看分析拍摄",
                    ),
                )
            },
            onDismiss = {
                if (!analysisImporting) {
                    showAnalysisSource = false
                    analysisImportError = null
                }
            },
        )
    }

    pendingAnalysisImport?.let { imported ->
        V2SwingAnalysisTrimScreen(
            imported = imported,
            saving = analysisTrimSaving,
            error = analysisTrimError,
            onCancel = { cancelPendingAnalysisTrim(showSourceAgain = true) },
            onUse = { startMs, endMs ->
                if (analysisTrimSaving) return@V2SwingAnalysisTrimScreen
                scope.launch {
                    analysisTrimSaving = true
                    analysisTrimError = null
                    try {
                        when (
                            val outcome = commitHomeAnalysisImport(
                                original = imported,
                                startMs = startMs,
                                endMs = endMs,
                                trim = { source, fromMs, toMs ->
                                    V2VideoFileStore.trimImportedVideo(context, source, fromMs, toMs)
                                },
                                save = { finalImport ->
                                    V2SwingSessionStore.addClipFromVideoImportAndWait(
                                        videoPath = finalImport.absolutePath,
                                        thumbnailPath = finalImport.thumbnailPath,
                                        durationSec = finalImport.durationSec,
                                        sourceLabel = "首页回看分析导入",
                                    )
                                },
                                cleanup = ::cleanupImportedAnalysisClip,
                            )
                        ) {
                            is HomeAnalysisCommitOutcome.Saved -> {
                                pendingAnalysisImport = null
                                analysisTrimError = null
                                showAnalysisSource = false
                                nav.navigateTo(
                                    Destination.SwingClipDetail(
                                        outcome.clipId,
                                        showCloseButton = true,
                                    ),
                                )
                            }
                            is HomeAnalysisCommitOutcome.Failure -> {
                                pendingAnalysisImport = null
                                analysisTrimError = null
                                analysisImportError = outcome.message
                                showAnalysisSource = true
                            }
                        }
                    } finally {
                        analysisTrimSaving = false
                    }
                }
            },
        )
    }

    rootAuthPrompt?.let { prompt ->
        V2RootAuthPromptDialog(
            prompt = prompt,
            onCancel = { rootAuthPrompt = null },
            onConfirm = {
                rootAuthPrompt = null
                selectedTab = V2Tab.HOME
                openLogin(returnTo = null)
            },
        )
    }

    kickOutMessage?.let { message ->
        V2KickOutAlertDialog(message = message, onConfirm = ::confirmKickOut)
    }

    appUpdatePresentation?.let { presentation ->
        V2AppUpdatePrompt(
            presentation = presentation,
            onUpdate = {
                openAndroidStore(context, presentation)
            },
            onRemindLater = {
                ServiceLocator.appUpdateRepo.remindLater(presentation)
                appUpdatePresentation = null
            },
            onContinueLocally = {
                if (presentation.policy.allowLocalAccess) {
                    localAccessPresentation = presentation
                    localAccessDestination = null
                    appUpdatePresentation = null
                }
            },
        )
    }

}

private fun openAndroidStore(context: android.content.Context, presentation: V2AppUpdatePresentation) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, presentation.policy.storeUrl.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}

private fun cleanupImportedAnalysisClip(result: V2VideoFileStore.ImportResult) {
    deleteFileCountingFreedBytes(File(result.absolutePath))
    result.thumbnailPath?.let { deleteFileCountingFreedBytes(File(it)) }
}

internal const val HOME_ANALYSIS_ALBUM_LAUNCH_ERROR =
    "无法打开相册，请稍后重试，或改用实时拍摄"
internal const val HOME_ANALYSIS_IMPORT_READ_ERROR =
    "无法读取视频，请检查权限或换一个视频"
internal const val HOME_ANALYSIS_IMPORT_ERROR =
    "无法导入视频，请稍后重试，或改用实时拍摄"
internal const val HOME_ANALYSIS_PROCESS_ERROR =
    "视频处理失败，请重新选择，或改用实时拍摄"
internal const val HOME_ANALYSIS_SAVE_ERROR =
    "无法保存挥杆素材，请稍后重试，或改用实时拍摄"

internal fun launchHomeAnalysisAlbumPicker(launch: () -> Unit): String? = try {
    launch()
    null
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (_: Throwable) {
    HOME_ANALYSIS_ALBUM_LAUNCH_ERROR
}

internal sealed interface HomeAnalysisImportOutcome {
    data class Ready(val imported: V2VideoFileStore.ImportResult) : HomeAnalysisImportOutcome
    data class Failure(val message: String) : HomeAnalysisImportOutcome
}

internal suspend fun importHomeAnalysisVideo(
    importVideo: suspend () -> V2VideoFileStore.ImportResult?,
): HomeAnalysisImportOutcome = try {
    importVideo()?.let(HomeAnalysisImportOutcome::Ready)
        ?: HomeAnalysisImportOutcome.Failure(HOME_ANALYSIS_IMPORT_READ_ERROR)
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (_: Throwable) {
    HomeAnalysisImportOutcome.Failure(HOME_ANALYSIS_IMPORT_ERROR)
}

internal sealed interface HomeAnalysisCommitOutcome {
    data class Saved(val clipId: UUID) : HomeAnalysisCommitOutcome
    data class Failure(val message: String) : HomeAnalysisCommitOutcome
}

internal suspend fun commitHomeAnalysisImport(
    original: V2VideoFileStore.ImportResult,
    startMs: Long,
    endMs: Long,
    trim: suspend (V2VideoFileStore.ImportResult, Long, Long) -> V2VideoFileStore.ImportResult?,
    save: suspend (V2VideoFileStore.ImportResult) -> UUID?,
    cleanup: (V2VideoFileStore.ImportResult) -> Unit,
): HomeAnalysisCommitOutcome {
    var prepared: V2VideoFileStore.ImportResult? = null

    fun cleanupDistinct(vararg results: V2VideoFileStore.ImportResult?) {
        results.filterNotNull().distinctBy(V2VideoFileStore.ImportResult::absolutePath).forEach { result ->
            runCatching { cleanup(result) }
        }
    }

    return try {
        prepared = trim(original, startMs, endMs)
        val finalImport = prepared ?: run {
            cleanupDistinct(original)
            return HomeAnalysisCommitOutcome.Failure(HOME_ANALYSIS_PROCESS_ERROR)
        }
        val clipId = save(finalImport) ?: run {
            cleanupDistinct(original, finalImport)
            return HomeAnalysisCommitOutcome.Failure(HOME_ANALYSIS_SAVE_ERROR)
        }
        if (finalImport.absolutePath != original.absolutePath) cleanupDistinct(original)
        HomeAnalysisCommitOutcome.Saved(clipId)
    } catch (cancellation: CancellationException) {
        prepared
            ?.takeIf { it.absolutePath != original.absolutePath }
            ?.let { runCatching { cleanup(it) } }
        throw cancellation
    } catch (_: Throwable) {
        cleanupDistinct(original, prepared)
        HomeAnalysisCommitOutcome.Failure(HOME_ANALYSIS_PROCESS_ERROR)
    }
}

@Composable
private fun TabsScaffold(
    selectedTab: V2Tab,
    externalInteractionRoute: StudentInteractionRoute? = null,
    onExternalInteractionRouteConsumed: () -> Unit = {},
    onSelectTab: (V2Tab) -> Unit,
    onShowQuickAdd: () -> Unit,
    onOpenAllGoals: () -> Unit,
    onOpenGoalDetail: (UUID) -> Unit,
    onCreateGoal: () -> Unit,
    onCreateTextMemo: () -> Unit,
    onCreateVideoMemo: () -> Unit,
    requestedMemoFilter: V2MemoRecordFilter? = null,
    onRequestedMemoFilterConsumed: () -> Unit = {},
    onOpenAllMemos: () -> Unit,
    onOpenVideoMemoList: () -> Unit,
    onOpenTextMemoList: () -> Unit,
    onOpenSwingCalendar: () -> Unit,
    onOpenSwingClip: (UUID, Boolean) -> Unit,
    onOpenGrowthPool: () -> Unit,
    onOpenJustSwing: () -> Unit,
    onOpenAIArchive: () -> Unit,
    onOpenProfileSettings: () -> Unit,
    onOpenAIConfig: () -> Unit,
    onSwitchToAI: () -> Unit,
    onOpenHomeAI: () -> Unit,
    onOpenMemoDetail: (UUID) -> Unit,
    onOpenPlazaVideo: (UUID) -> Unit,
    onRequestLogin: () -> Unit,
    onRequestLoginForProfileTarget: (V2ProfileLoginTarget) -> Unit,
    onRequestLoginForPlazaTarget: (V2PlazaLoginTarget) -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenAccountSecurity: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenResetCredits: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onLogoutComplete: () -> Unit,
    showRoleSwitcher: Boolean,
    onSwitchRole: () -> Unit,
    pendingCoachInvite: V2PendingCoachInvite?,
    onCoachInviteConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTrajectory: () -> Unit = {},
    onOpenAnalysis: () -> Unit = {},
    onOpenOnlineDownload: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val studentSession by ServiceLocator.session.state.collectAsState(initial = V2Session())
    val deliveryState by ServiceLocator.coachDeliveryQueue.state.collectAsState()
    val ownerDeliveryJobs = deliveryState.jobsForOwner(studentSession.userId)
    val selectedStudentDeliveryJob = selectStudentDeliveryContinuityJob(
        jobs = ownerDeliveryJobs,
        activeJobId = deliveryState.activeJobId,
    )
    val deliveryCompletion = deliveryState.completionForOwner(studentSession.userId)
        ?.takeUnless { it.opensLibrary }
    var studentUnread by remember(studentSession.userId) { mutableStateOf(V2StudentUnreadSnapshot()) }
    var studentUnreadVersion by remember(studentSession.userId) { mutableStateOf<String?>(null) }
    var previousStudentUnread by remember(studentSession.userId) {
        mutableStateOf<V2StudentUnreadSnapshot?>(null)
    }
    var previousStudentConnections by remember(studentSession.userId) {
        mutableStateOf<List<V2CoachConnection>?>(null)
    }
    var studentConnections by remember(studentSession.userId) {
        mutableStateOf<List<V2CoachConnection>>(emptyList())
    }
    var coachNamesByConnection by remember(studentSession.userId) {
        mutableStateOf<Map<String, String>>(emptyMap())
    }
    var coachAvatarsByConnection by remember(studentSession.userId) {
        mutableStateOf<Map<String, String?>>(emptyMap())
    }
    var activeCoachConnectionIds by remember(studentSession.userId) {
        mutableStateOf<List<String>>(emptyList())
    }
    var incomingActivity by remember(studentSession.userId) {
        mutableStateOf<V2StudentIncomingActivity?>(null)
    }
    var lastPresentedActivityId by remember(studentSession.userId) { mutableStateOf<String?>(null) }
    var pendingInteractionRoute by remember(studentSession.userId) {
        mutableStateOf<StudentInteractionRoute?>(null)
    }
    var showDeliveryQueue by remember(studentSession.userId) { mutableStateOf(false) }
    var showRoleSwitchBlocked by remember(studentSession.userId) { mutableStateOf(false) }
    var dismissedStudentDeliveryJobId by rememberSaveable(studentSession.userId) { mutableStateOf<String?>(null) }
    val deliveryNetworkConnected = rememberQueueNetworkConnected()

    suspend fun refreshStudentWorkspace(forceSnapshot: Boolean, announceActivity: Boolean) {
        ServiceLocator.coachWorkspaceRepo.studentConnections()
            .onSuccess { connections ->
                val connectionNotice = if (announceActivity) {
                    resolveStudentConnectionActivity(
                        previous = previousStudentConnections,
                        current = connections,
                        currentUserId = studentSession.userId,
                    )
                } else null
                previousStudentConnections = connections
                studentConnections = connections
                studentSession.userId?.let { userId ->
                    V2MemoDiscussionIndex.update(userId, connections, studentUnread)
                }
                val activeCoachConnections = connections.filter {
                    it.status == "active" && it.relationshipType != "friend"
                }
                activeCoachConnectionIds = activeCoachConnections.map(V2CoachConnection::connectionId)
                coachNamesByConnection = activeCoachConnections.mapNotNull { connection ->
                    connection.coach?.nickname?.let { connection.connectionId to it }
                }.toMap()
                coachAvatarsByConnection = activeCoachConnections.associate { connection ->
                    connection.connectionId to connection.coach?.avatar
                }
                if (connectionNotice != null && connectionNotice.id != lastPresentedActivityId) {
                    lastPresentedActivityId = connectionNotice.id
                    incomingActivity = connectionNotice
                }
            }
        ServiceLocator.coachWorkspaceRepo.studentUnread(
            knownVersion = if (forceSnapshot) null else studentUnreadVersion,
        ).onSuccess { update ->
            update.snapshot?.let { snapshot ->
                val notice = if (announceActivity) {
                    resolveStudentIncomingActivity(
                        previous = previousStudentUnread,
                        current = snapshot,
                        coachNamesByConnection = coachNamesByConnection,
                    )
                } else null
                previousStudentUnread = snapshot
                studentUnread = snapshot
                studentSession.userId?.let { userId ->
                    V2MemoDiscussionIndex.update(userId, studentConnections, snapshot)
                }
                if (notice != null && notice.id != lastPresentedActivityId) {
                    lastPresentedActivityId = notice.id
                    incomingActivity = notice
                }
            }
            studentUnreadVersion = update.version ?: studentUnreadVersion
        }
    }

    LaunchedEffect(lifecycleOwner, studentSession.userId, studentSession.isLoggedIn) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val session = ServiceLocator.session.current()
                if (session.isLoggedIn) {
                    session.userId?.let { ownerUserId ->
                        ServiceLocator.coachDeliveryQueue.recover(
                            ownerUserId = ownerUserId,
                            repository = ServiceLocator.coachWorkspaceRepo,
                        )
                    }
                    refreshStudentWorkspace(forceSnapshot = false, announceActivity = true)
                } else {
                    studentUnread = V2StudentUnreadSnapshot()
                    studentUnreadVersion = null
                    previousStudentUnread = null
                    previousStudentConnections = null
                    studentConnections = emptyList()
                    V2MemoDiscussionIndex.clear()
                    coachNamesByConnection = emptyMap()
                    coachAvatarsByConnection = emptyMap()
                    activeCoachConnectionIds = emptyList()
                    incomingActivity = null
                    pendingInteractionRoute = null
                }
                delay(60_000)
            }
        }
    }

    LaunchedEffect(incomingActivity?.id) {
        val shownId = incomingActivity?.id ?: return@LaunchedEffect
        delay(8_000)
        if (incomingActivity?.id == shownId) incomingActivity = null
    }

    LaunchedEffect(externalInteractionRoute) {
        externalInteractionRoute?.let { pendingInteractionRoute = it }
    }

    LaunchedEffect(deliveryCompletion?.id) {
        val completionId = deliveryCompletion?.id ?: return@LaunchedEffect
        delay(5_000)
        ServiceLocator.coachDeliveryQueue.dismissCompletion(completionId)
    }

    LaunchedEffect(deliveryState.activeJobId) {
        if (deliveryState.activeJobId == dismissedStudentDeliveryJobId) {
            dismissedStudentDeliveryJobId = null
        }
    }

    V2BackHandler(enabled = selectedTab != V2Tab.HOME) { onSelectTab(V2Tab.HOME) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DSColors.warmWhite,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
        bottomBar = {
            Column {
                incomingActivity?.takeIf { selectedTab != V2Tab.INTERACTION }?.let { notice ->
                    StudentIncomingActivityBar(
                        notice = notice,
                        onOpen = {
                            pendingInteractionRoute = StudentInteractionRoute(notice.connectionId, notice.itemId)
                            incomingActivity = null
                            onSelectTab(V2Tab.INTERACTION)
                        },
                        onDismiss = { incomingActivity = null },
                    )
                }
                if (
                    selectedStudentDeliveryJob != null &&
                    selectedStudentDeliveryJob.jobId != dismissedStudentDeliveryJobId
                ) {
                    val job = selectedStudentDeliveryJob
                    val active = deliveryState.activeJobId == job.jobId
                    StudentUploadContinuityBar(
                        job = job,
                        active = active,
                        phase = deliveryState.activePhase.takeIf { active },
                        progress = deliveryState.activeProgress.takeIf { active } ?: 0.0,
                        networkConnected = deliveryNetworkConnected,
                        onReturn = {
                            if (job.connectionId.isNotBlank()) {
                                pendingInteractionRoute = StudentInteractionRoute(job.connectionId, job.parentItemId)
                                onSelectTab(V2Tab.INTERACTION)
                            } else {
                                showDeliveryQueue = true
                            }
                        },
                        onDismiss = {
                            dismissedStudentDeliveryJobId = job.jobId
                        },
                    )
                } else if (ownerDeliveryJobs.isEmpty() && deliveryCompletion != null) {
                    val completion = deliveryCompletion
                    V2DeliveryCompletionBar(
                        completion = completion,
                        actionLabel = "查看",
                        onAction = {
                            completion.connectionId?.let { connectionId ->
                                pendingInteractionRoute = StudentInteractionRoute(connectionId, completion.itemId)
                                onSelectTab(V2Tab.INTERACTION)
                            }
                            ServiceLocator.coachDeliveryQueue.dismissCompletion(completion.id)
                        },
                        onDismiss = {
                            ServiceLocator.coachDeliveryQueue.dismissCompletion(completion.id)
                        },
                    )
                }
                V2BottomTabBar(
                    selected = selectedTab,
                    onSelect = onSelectTab,
                    interactionBadge = studentUnread.total,
                    hasCoachConnection = activeCoachConnectionIds.isNotEmpty(),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DSColors.warmWhite),
        ) {
            when (selectedTab) {
                V2Tab.HOME -> V2HomeScreen(
                    onCreateGoal = onCreateGoal,
                    onOpenAllGoals = onOpenAllGoals,
                    onOpenGoalDetail = onOpenGoalDetail,
                    onSwitchToPlaza = { onSelectTab(V2Tab.PLAZA) },
                    onSwitchToMemo = { onSelectTab(V2Tab.MEMO) },
                    onOpenMemo = onOpenMemoDetail,
                    onOpenSwingClip = { onOpenSwingClip(it, true) },
                    onOpenPlazaVideo = onOpenPlazaVideo,
                    onOpenSwingCalendar = onOpenSwingCalendar,
                    onOpenGrowthPool = onOpenGrowthPool,
                    onOpenJustSwing = onOpenJustSwing,
                    onOpenAI = onOpenHomeAI,
                    onAddTap = onShowQuickAdd,
                    onOpenTrajectory = onOpenTrajectory,
                    onOpenAnalysis = onOpenAnalysis,
                    onOpenOnlineDownload = onOpenOnlineDownload,
                    onRequestLogin = onRequestLogin,
                    studentUnread = studentUnread,
                    coachNamesByConnection = coachNamesByConnection,
                    coachAvatarsByConnection = coachAvatarsByConnection,
                    activeCoachConnectionIds = activeCoachConnectionIds,
                    onOpenCoachFeedback = { connectionId, itemId ->
                        pendingInteractionRoute = StudentInteractionRoute(connectionId, itemId)
                        onSelectTab(V2Tab.INTERACTION)
                    },
                    onRefreshStudentData = {
                        if (ServiceLocator.session.current().isLoggedIn) {
                            refreshStudentWorkspace(forceSnapshot = true, announceActivity = false)
                        }
                    },
                )
                V2Tab.MEMO -> V2MemoRecordBrowserScreen(
                    isLoggedIn = studentSession.isLoggedIn,
                    applyStatusBarPadding = false,
                    requestedFilter = requestedMemoFilter?.name,
                    onRequestedFilterConsumed = onRequestedMemoFilterConsumed,
                    onCreateVideoMemo = onCreateVideoMemo,
                    onCreateTextMemo = onCreateTextMemo,
                    onOpenMemo = onOpenMemoDetail,
                    onOpenSwingClip = { onOpenSwingClip(it, true) },
                    onOpenJustSwing = onOpenJustSwing,
                    onRequestLogin = onRequestLogin,
                )
                V2Tab.INTERACTION -> V2StudentInteractionScreen(
                    onOpenMemo = { onSelectTab(V2Tab.MEMO) },
                    onRequestLogin = onRequestLogin,
                    unread = studentUnread,
                    initialConnectionId = pendingInteractionRoute?.connectionId,
                    initialConversationItemId = pendingInteractionRoute?.conversationItemId,
                    onInitialRouteOpened = {
                        pendingInteractionRoute = null
                        onExternalInteractionRouteConsumed()
                    },
                    pendingInvite = pendingCoachInvite,
                    onPendingInviteConsumed = onCoachInviteConsumed,
                )
                V2Tab.PLAZA -> V2ToolsHomeScreen(
                    applyStatusBarPadding = false,
                    onRecord = onOpenJustSwing,
                    onAnalysis = onOpenAnalysis,
                    onTrajectory = onOpenTrajectory,
                    onAI = onOpenHomeAI,
                    onDownload = onOpenOnlineDownload,
                )
                V2Tab.ME -> V2ProfileScreen(
                    onRequestLogin = onRequestLogin,
                    onRequestLoginForTarget = onRequestLoginForProfileTarget,
                    onOpenEditProfile = onOpenEditProfile,
                    onOpenAccountSecurity = onOpenAccountSecurity,
                    onOpenStorage = onOpenStorage,
                    onOpenResetCredits = onOpenResetCredits,
                    onOpenAIConfig = onOpenAIConfig,
                    onOpenAIArchive = onOpenAIArchive,
                    onOpenSettings = onOpenProfileSettings,
                    onOpenFeedback = onOpenFeedback,
                    onOpenTerms = onOpenTerms,
                    onOpenPrivacy = onOpenPrivacy,
                    onOpenAllGoals = onOpenAllGoals,
                    onOpenVideoMemoList = onOpenVideoMemoList,
                    onOpenSwingCalendar = onOpenSwingCalendar,
                    onBrowsePlaza = { onSelectTab(V2Tab.HOME) },
                    onBrowseMemo = { onSelectTab(V2Tab.MEMO) },
                    onLogoutComplete = onLogoutComplete,
                    showRoleSwitcher = showRoleSwitcher,
                    roleSwitchBlocked = ownerDeliveryJobs.isNotEmpty(),
                    onSwitchRole = {
                        if (ownerDeliveryJobs.isEmpty()) onSwitchRole()
                        else showRoleSwitchBlocked = true
                    },
                )
            }
        }
    }

    if (showDeliveryQueue && studentSession.userId != null) {
        V2DeliveryQueueDialog(
            ownerUserId = studentSession.userId.orEmpty(),
            onDismiss = { showDeliveryQueue = false },
            onOpenConnection = { connectionId, itemId ->
                pendingInteractionRoute = StudentInteractionRoute(connectionId, itemId)
                onSelectTab(V2Tab.INTERACTION)
            },
        )
    }
    if (showRoleSwitchBlocked) {
        AlertDialog(
            onDismissRequest = { showRoleSwitchBlocked = false },
            title = { Text("内容正在发送") },
            text = {
                Text(
                    "还有 ${ownerDeliveryJobs.size} 条内容待发送，完成后即可切换身份；当前内容和本机草稿不会丢失。",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRoleSwitchBlocked = false
                    showDeliveryQueue = true
                }) { Text("查看发送队列") }
            },
            dismissButton = {
                TextButton(onClick = { showRoleSwitchBlocked = false }) { Text("知道了") }
            },
        )
    }
}

private data class StudentInteractionRoute(
    val connectionId: String,
    val conversationItemId: String?,
)

internal fun selectStudentDeliveryContinuityJob(
    jobs: List<V2CoachDeliveryJob>,
    activeJobId: String?,
): V2CoachDeliveryJob? = jobs.firstOrNull { it.jobId == activeJobId }
    ?: jobs.asSequence()
        .filter { it.pausedByUser || !it.lastError.isNullOrBlank() }
        .maxByOrNull(V2CoachDeliveryJob::createdAtMillis)
    ?: jobs.minByOrNull(V2CoachDeliveryJob::createdAtMillis)

internal fun studentDeliveryContinuityTitle(
    job: V2CoachDeliveryJob,
    active: Boolean,
    phase: V2CoachDeliveryPhase?,
    networkConnected: Boolean,
): String = when {
    !networkConnected && active -> "等待网络，稍后自动继续"
    job.pausedByUser || !job.lastError.isNullOrBlank() -> "发送暂停，内容已保留"
    phase == V2CoachDeliveryPhase.UPLOADING -> "正在上传视频"
    phase == V2CoachDeliveryPhase.PUBLISHING -> "正在发布内容"
    active || phase == V2CoachDeliveryPhase.PREPARING -> "正在准备内容"
    else -> "正在等待发送"
}

internal fun studentDeliveryContinuityDetail(
    job: V2CoachDeliveryJob,
    active: Boolean,
    phase: V2CoachDeliveryPhase?,
    networkConnected: Boolean,
): String = when {
    !networkConnected && active -> "无需停留当前页面，联网后系统会自动续传"
    job.pausedByUser || !job.lastError.isNullOrBlank() ->
        job.lastError?.takeIf(String::isNotBlank) ?: "网络恢复后可以继续发送"
    phase == V2CoachDeliveryPhase.PREPARING -> "正在整理视频，随后会自动上传"
    phase == V2CoachDeliveryPhase.UPLOADING -> "正在上传视频"
    phase == V2CoachDeliveryPhase.PUBLISHING -> "视频已经传完，正在送达到交流中"
    else -> "即将开始，可以继续浏览"
}

internal fun studentDeliveryProgressLabel(
    active: Boolean,
    phase: V2CoachDeliveryPhase?,
    progress: Double,
): String = when {
    !active -> ""
    phase == V2CoachDeliveryPhase.PREPARING || phase == V2CoachDeliveryPhase.UPLOADING ->
        "${(progress.coerceIn(0.0, 1.0) * 100).toInt()}%"
    phase == V2CoachDeliveryPhase.PUBLISHING -> "发送中"
    else -> ""
}

@Composable
internal fun StudentUploadContinuityBar(
    job: V2CoachDeliveryJob,
    active: Boolean,
    phase: V2CoachDeliveryPhase?,
    progress: Double,
    networkConnected: Boolean,
    onReturn: () -> Unit,
    onDismiss: () -> Unit,
) {
    val failed = job.pausedByUser || !job.lastError.isNullOrBlank()
    val progressLabel = studentDeliveryProgressLabel(active, phase, progress)
    Box(
        Modifier.fillMaxWidth().height(44.dp)
            .background(if (failed) Color(0xFFF4E3B8).copy(alpha = 0.76f) else DSColors.cardWhite.copy(alpha = 0.97f))
            .testTag("student-upload-continuity-bar"),
    ) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(DSColors.borderLine).align(Alignment.TopCenter))
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(9.dp),
        ) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                if (failed) {
                    Icon(Icons.Filled.Refresh, null, tint = DSColors.trophyGold, modifier = Modifier.size(18.dp))
                } else {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = DSColors.fairway)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    studentDeliveryContinuityTitle(job, active, phase, networkConnected),
                    color = DSColors.inkGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    studentDeliveryContinuityDetail(job, active, phase, networkConnected),
                    color = DSColors.shadowGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            if (progressLabel.isNotEmpty()) {
                Text(progressLabel, color = DSColors.fairway, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                if (failed) "去重试" else "返回",
                color = DSColors.fairway,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onReturn).padding(vertical = 8.dp),
            )
            if (failed) {
                Box(
                    Modifier.size(24.dp).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, "关闭发送提示", tint = DSColors.shadowGray, modifier = Modifier.size(9.dp))
                }
            }
        }
        if (active) {
            LinearProgressIndicator(
                progress = {
                    if (phase == V2CoachDeliveryPhase.PUBLISHING) 1f
                    else progress.toFloat().coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter),
                color = DSColors.fairway,
                trackColor = Color.Transparent,
            )
        }
    }
}

@Composable
internal fun StudentIncomingActivityBar(
    notice: V2StudentIncomingActivity,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isConnectionNotice = notice.itemId == null
    val accent = if (isConnectionNotice) DSColors.trophyGold else DSColors.fairway
    Box(
        modifier = Modifier.fillMaxWidth().height(52.dp)
            .background(DSColors.cardWhite.copy(alpha = 0.98f))
            .testTag("student-incoming-activity-bar"),
    ) {
        Box(Modifier.fillMaxSize().clickable(onClick = onOpen).padding(horizontal = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(accent.copy(alpha = 0.11f)),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Icon(
                        when {
                            isConnectionNotice -> Icons.Filled.Lock
                            notice.hasVideo -> Icons.Filled.VideoLibrary
                            else -> Icons.Filled.ChatBubble
                        },
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${notice.coachName} · ${notice.title}",
                        color = DSColors.inkGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        notice.detail,
                        color = DSColors.shadowGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
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
            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd)
                .padding(end = 12.dp).size(width = 26.dp, height = 32.dp)
                .clip(CircleShape).clickable(onClick = onDismiss)
                .testTag("student-incoming-activity-dismiss"),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "关闭教练反馈提示",
                tint = DSColors.shadowGray,
                modifier = Modifier.size(10.dp),
            )
        }
        Box(
            Modifier.align(androidx.compose.ui.Alignment.CenterStart)
                .size(width = 3.dp, height = 52.dp)
                .background(DSColors.trophyGold),
        )
        Box(Modifier.align(androidx.compose.ui.Alignment.TopCenter).fillMaxWidth().height(0.5.dp).background(DSColors.borderLine))
    }
}

@Composable
internal fun AnalysisSourceSheet(
    importing: Boolean,
    error: String?,
    onPickAlbum: () -> Unit,
    onRecord: () -> Unit,
    onDismiss: () -> Unit,
) {
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val dismissThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    val sheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    Dialog(
        onDismissRequest = { if (!importing) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !importing,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        enabled = !importing,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismiss,
                    )
                    .testTag("analysis-source-scrim"),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer { translationY = dragOffsetPx }
                    .pointerInput(importing) {
                        detectVerticalDragGestures(
                            onDragCancel = { dragOffsetPx = 0f },
                            onDragEnd = {
                                if (!importing && dragOffsetPx >= dismissThresholdPx) onDismiss()
                                else dragOffsetPx = 0f
                            },
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                if (!importing) dragOffsetPx = (dragOffsetPx + amount).coerceAtLeast(0f)
                            },
                        )
                    }
                    .shadow(
                        elevation = 28.dp,
                        shape = sheetShape,
                        ambientColor = DSColors.deepGreen.copy(alpha = 0.18f),
                        spotColor = DSColors.deepGreen.copy(alpha = 0.18f),
                    )
                    .clip(sheetShape)
                    .background(DSColors.warmWhite)
                    .navigationBarsPadding()
                    .testTag("analysis-source-sheet"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.padding(top = 12.dp).size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp)).background(Color(0xFFD8E2DC)),
                )

                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(DSColors.mistGreen),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Analytics,
                        contentDescription = null,
                        tint = DSColors.fairway,
                        modifier = Modifier.size(25.dp),
                    )
                }
                Text(
                    text = "创建挥杆分析",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = DSColors.inkGreen,
                )
                Text(
                    text = "选择视频来源，进入帧级分析工具",
                    style = DSTypography.body,
                    color = DSColors.shadowGray,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (importing) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(88.dp)
                            .clip(RoundedCornerShape(20.dp)).background(DSColors.cardWhite)
                            .border(1.dp, DSColors.borderLine, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp)
                            .testTag("analysis-source-processing"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(DSColors.mistGreen),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = DSColors.fairway,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("正在读取相册视频", color = DSColors.inkGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("大视频可能需要几秒钟，请稍候", color = DSColors.shadowGray, style = DSTypography.caption)
                        }
                    }
                } else {
                    AnalysisSourceOption(
                        title = "从相册选择",
                        subtitle = "导入已有 MP4 或 MOV 视频",
                        icon = Icons.Filled.VideoLibrary,
                        prominent = true,
                        onClick = onPickAlbum,
                    )
                    AnalysisSourceOption(
                        title = "实时拍摄",
                        subtitle = "现场录制后立即进入分析",
                        icon = Icons.Filled.CameraAlt,
                        prominent = false,
                        onClick = onRecord,
                    )
                }
                error?.let {
                    Text(
                        text = it,
                        style = DSTypography.caption,
                        color = DSColors.flagRed,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(Icons.Outlined.Info, null, tint = DSColors.shadowGray, modifier = Modifier.size(12.dp))
                Text(
                    "支持 MP4、MOV 格式，建议选择清晰的正面或侧面挥杆视频",
                    color = DSColors.shadowGray,
                    style = DSTypography.caption,
                    maxLines = 2,
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)
                    .height(50.dp).clip(RoundedCornerShape(14.dp)).background(DSColors.sandBeige)
                    .clickable(enabled = !importing, onClick = onDismiss)
                    .testTag("analysis-source-cancel"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (importing) "正在处理" else "取消",
                    color = if (importing) DSColors.shadowGray else DSColors.inkGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
}

@Composable
private fun AnalysisSourceOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    prominent: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val background = if (prominent) {
        Brush.linearGradient(listOf(DSColors.deepGreen, DSColors.fairway))
    } else {
        Brush.linearGradient(listOf(DSColors.cardWhite, DSColors.cardWhite))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(shape)
            .background(background)
            .border(
                width = 1.dp,
                color = if (prominent) Color.Transparent else DSColors.borderLine,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = DSSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (prominent) Color.White.copy(alpha = 0.16f) else DSColors.mistGreen),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (prominent) Color.White else DSColors.fairway,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = if (prominent) Color.White else DSColors.inkGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = if (prominent) Color.White.copy(alpha = 0.72f) else DSColors.shadowGray,
                style = DSTypography.caption,
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = if (prominent) Color.White.copy(alpha = 0.72f) else DSColors.shadowGray,
            modifier = Modifier.size(18.dp),
        )
    }
}

internal suspend fun <T> runRootBackgroundSyncCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}

internal enum class V2RootSessionValidation { VALID, INVALID, DEFERRED }

@Composable
internal fun V2RootForegroundObserver(
    lifecycleOwner: LifecycleOwner,
    enabled: Boolean,
    isLoggedIn: Boolean,
    onForeground: () -> Unit,
) {
    val latestEnabled by rememberUpdatedState(enabled)
    val latestIsLoggedIn by rememberUpdatedState(isLoggedIn)
    val latestOnForeground by rememberUpdatedState(onForeground)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && latestEnabled && latestIsLoggedIn) {
                latestOnForeground()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/**
 * iOS keeps the cached account usable on transport failures and only logs out for an explicit
 * account/session rejection. Android uses the same user-facing error vocabulary from userAuth.
 */
internal fun classifyRootSessionValidation(result: Result<V2Session>): V2RootSessionValidation {
    if (result.isSuccess) return V2RootSessionValidation.VALID
    val text = generateSequence(result.exceptionOrNull()) { it.cause }
        .mapNotNull(Throwable::message)
        .joinToString(" | ")
        .lowercase()
    val invalidMarkers = listOf(
        "不存在",
        "not found",
        "无效",
        "失效",
        "请重新登录",
        "登录已过期",
        "session_invalid",
        "session_required",
        "unauthorized",
    )
    return if (invalidMarkers.any(text::contains)) {
        V2RootSessionValidation.INVALID
    } else {
        V2RootSessionValidation.DEFERRED
    }
}

@Preview(showBackground = true)
@Composable
private fun V2RootScreenPreview() {
    MyGolfSwingAppAndroidTheme {
        V2RootScreen()
    }
}
