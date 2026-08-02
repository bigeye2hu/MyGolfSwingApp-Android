package com.mygolfswing.app.ui.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.rememberV2AppNavigation
import com.mygolfswing.app.ui.common.V2SplashScreen
import com.mygolfswing.app.ui.memo.V2OnlineVideoSelection
import com.mygolfswing.app.ui.memo.V2OnlineVideoSelectorScreen
import com.mygolfswing.app.ui.swing.V2JustSwingMainScreen
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.session.V2Session
import com.mygolfswing.app.data.model.V2SwingClipRetention
import com.mygolfswing.app.data.store.V2CategoryStore
import com.mygolfswing.app.data.store.V2UserScopedCache
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.data.store.V2GolfProfileStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.ai.V2AIChatScreen
import com.mygolfswing.app.ui.auth.V2ChangePasswordScreen
import com.mygolfswing.app.ui.profile.V2AccountDeletionScreen
import com.mygolfswing.app.ui.profile.V2AccountSecurityScreen
import com.mygolfswing.app.ui.auth.V2LoginScreen
import com.mygolfswing.app.ui.auth.V2RegisterScreen
import com.mygolfswing.app.ui.auth.V2ResetPasswordScreen
import com.mygolfswing.app.ui.feedback.V2FeedbackScreen
import com.mygolfswing.app.ui.legal.V2LegalDocType
import com.mygolfswing.app.ui.legal.V2LegalScreen
import com.mygolfswing.app.ui.settings.V2AIServiceConfigScreen
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
import com.mygolfswing.app.data.store.V2AIChatStore
import com.mygolfswing.app.data.store.V2SwingSessionStore
import com.mygolfswing.app.data.video.V2OnlineVideoMetadataFetcher
import com.mygolfswing.app.data.video.V2VideoFileStore
import com.mygolfswing.app.data.video.deleteFileCountingFreedBytes
import com.mygolfswing.app.ui.memo.V2MemoAllListScreen
import com.mygolfswing.app.ui.memo.V2MemoListScreen
import com.mygolfswing.app.ui.memo.V2QuickAddSheet
import com.mygolfswing.app.ui.memo.V2TextMemoListScreen
import com.mygolfswing.app.ui.memo.V2VideoMemoListScreen
import com.mygolfswing.app.ui.memo.V2VideoMemoPlayerScreen
import com.mygolfswing.app.ui.plaza.V2CoachProfileScreen
import com.mygolfswing.app.ui.plaza.V2PlazaLoginTarget
import com.mygolfswing.app.ui.plaza.V2PlazaPlayerLoginTarget
import com.mygolfswing.app.ui.plaza.V2PlazaPlayerScreen
import com.mygolfswing.app.ui.plaza.V2PlazaScreen
import com.mygolfswing.app.ui.plaza.V2PlazaWatchHistoryScreen
import com.mygolfswing.app.ui.profile.V2AIArchiveScreen
import com.mygolfswing.app.ui.profile.V2CreditsPurchaseScreen
import com.mygolfswing.app.ui.profile.V2CreditsResetScreen
import com.mygolfswing.app.ui.profile.V2EditProfileScreen
import com.mygolfswing.app.ui.profile.V2ProfileCloudConsistencyAuditScreen
import com.mygolfswing.app.ui.profile.V2ProfileLoginTarget
import com.mygolfswing.app.ui.profile.V2ProfileScreen
import com.mygolfswing.app.ui.profile.V2ProfileSettingsScreen
import com.mygolfswing.app.ui.swing.V2JustSwingCalendarScreen
import com.mygolfswing.app.ui.swing.V2SwingAnalysisTrimScreen
import com.mygolfswing.app.ui.swing.V2SwingClipDetailScreen
import com.mygolfswing.app.ui.swing.V2SwingDayDetailScreen
import com.mygolfswing.app.ui.theme.MyGolfSwingAppAndroidTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.UUID

/**
 * 应用内导航目的地（轻量栈，无需引入 Navigation Compose）。
 */
private sealed class Destination {
    data object Tabs : Destination()
    data object AllGoals : Destination()
    data class NewGoal(val editingId: UUID? = null) : Destination()
    data class GoalDetail(val id: UUID) : Destination()
    data object NewTextMemo : Destination()
    data class NewVideoMemo(
        val editingId: UUID? = null,
        val allowSourceReplacement: Boolean = false,
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
    data class SwingClipDetail(val clipId: UUID) : Destination()
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
    data object ProfileSettings : Destination()
    data object Login : Destination()
    data object Register : Destination()
    data object ResetPassword : Destination()
    data object CompleteProfile : Destination()
    data object EditProfile : Destination()
    data object AccountSecurity : Destination()
    data object AccountDeletion : Destination()
    data object ChangePassword : Destination()
    data object StorageManagement : Destination()
    data object CreditsReset : Destination()
    data object CreditsPurchase : Destination()
    data object AIConfig : Destination()
    data object CloudConsistencyAudit : Destination()
    data object Feedback : Destination()
    data class Legal(val type: V2LegalDocType) : Destination()
}

/**
 * V2 Android 根容器（对齐 iOS `V2RootView`）。
 */
@Composable
fun V2RootScreen(
    modifier: Modifier = Modifier,
    sharedVideoUrl: String? = null,
    onSharedVideoUrlConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableStateOf(V2Tab.HOME) }
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
    var profileQrRequestKey by remember { mutableIntStateOf(0) }
    var aiHistoryRequestKey by remember { mutableIntStateOf(0) }
    val profile by V2ProfileStore.profile.collectAsState()
    val currentSession by ServiceLocator.session.state.collectAsState(initial = V2Session())

    val analysisVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) {
            analysisImporting = false
            showAnalysisSource = true
            return@rememberLauncherForActivityResult
        }
        showAnalysisSource = true
        scope.launch {
            analysisImporting = true
            analysisImportError = null
            analysisTrimError = null
            var imported: V2VideoFileStore.ImportResult? = null
            try {
                imported = V2VideoFileStore.importVideo(context, uri)
                if (imported == null) {
                    analysisImportError = "无法读取视频，请检查权限或换一个视频"
                    showAnalysisSource = true
                } else {
                    showAnalysisSource = false
                    pendingAnalysisImport = imported
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                imported?.let(::cleanupImportedAnalysisClip)
                analysisImportError = "无法导入视频，请稍后重试，或改用实时拍摄 / 素材库"
                showAnalysisSource = true
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

    fun completePostAuthRefresh() {
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
        val hasPendingSharedVideo = pendingSharedVideoUrl != null
        val shouldOpenCompleteProfile = continueDestination == null &&
            plazaTarget == null &&
            onlineMemoUrl == null &&
            !openAIHistory &&
            plazaPlayerTarget == null &&
            !openProfileQr &&
            !openAnalysisSource &&
            !hasPendingSharedVideo &&
            currentSession.nickname.isNullOrBlank()
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
        showQuickAdd = false
        selectedTab = returnTab ?: V2Tab.HOME
        if (returnDestination == null) {
            nav.resetToRoot()
        } else {
            nav.returnTo(returnDestination)
        }
        when (plazaTarget) {
            V2PlazaLoginTarget.SAVED_COLLECTION -> {
                selectedTab = V2Tab.PLAZA
                if (!V2PlazaStore.showSavedOnly.value) V2PlazaStore.toggleSavedOnly()
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
            selectedTab = V2Tab.AI
            aiHistoryRequestKey += 1
        }
        if (openProfileQr) {
            profileQrRequestKey += 1
        }
        if (openAnalysisSource) {
            selectedTab = V2Tab.HOME
            showAnalysisSource = true
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
        if (session.isLoggedIn) {
            val userOk = ServiceLocator.authRepo.fetchUserInfo().isSuccess
            if (!userOk) {
                V2ProfileStore.resetUserScopedCaches()
                ServiceLocator.authRepo.logout()
                nav.resetTo(Destination.Login)
            } else {
                V2PlazaStore.refresh()
                V2GoalStore.syncFromCloud()
                session.userId?.let { uid -> V2CategoryStore.loadForUser(uid) }
                ServiceLocator.goalRepo.pushDirty()
                ServiceLocator.creditsRepo.fetchBalance(force = true)
                V2MemoStore.pullFromCloud()
                V2SwingSessionStore.cleanupExpiredTemporaryClips()
                runRootBackgroundSyncCatching { V2GolfProfileStore.refreshFromCloud() }
            }
        } else {
            nav.resetTo(Destination.Login)
        }
        val remainingSplashMs = 1_500L - (System.currentTimeMillis() - splashStartedAt)
        if (remainingSplashMs > 0) delay(remainingSplashMs)
        bootstrapping = false
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

    if (bootstrapping) {
        V2SplashScreen()
        return
    }

    V2BackHandler(enabled = nav.canGoBack && nav.current != Destination.Login) { nav.navigateBack() }

    when (val d = nav.current) {
        Destination.Tabs -> TabsScaffold(
            selectedTab = selectedTab,
            onSelectTab = { tab ->
                if (tab == V2Tab.AI) {
                    // AI 是中间凸起按钮，单独处理：直接切到 AI 全屏（无底栏）
                    selectedTab = V2Tab.AI
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
            onOpenAllMemos = { nav.navigateTo(Destination.AllMemos) },
            onOpenVideoMemoList = { nav.navigateTo(Destination.VideoMemoList) },
            onOpenTextMemoList = { nav.navigateTo(Destination.TextMemoList) },
            onOpenSwingCalendar = { requireLoginForAccountContent(Destination.SwingCalendar()) },
            onOpenSwingClip = { nav.navigateTo(Destination.SwingClipDetail(it)) },
            onOpenGrowthPool = { requireLoginForAccountContent(Destination.SwingCalendar(initialFilter = V2SwingClipRetention.PROMOTED)) },
            onOpenJustSwing = { requireLoginForAccountContent(Destination.JustSwingMain()) },
            onOpenAIArchive = { nav.navigateTo(Destination.AIArchive) },
            onOpenProfileSettings = { nav.navigateTo(Destination.ProfileSettings) },
            onOpenAIConfig = { nav.navigateTo(Destination.AIConfig) },
            onRequestLoginForAIHistory = ::openLoginForAIHistory,
            aiHistoryRequestKey = aiHistoryRequestKey,
            onSwitchToAI = { selectedTab = V2Tab.AI },
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
            onOpenPlazaWatchHistory = { nav.navigateTo(Destination.PlazaWatchHistory) },
            onRequestLogin = { openLogin() },
            onRequestLoginForProfileTarget = ::openLoginForProfileTarget,
            onRequestLoginForPlazaTarget = ::openLoginForPlazaTarget,
            onOpenEditProfile = { nav.navigateTo(Destination.EditProfile) },
            onOpenAccountSecurity = { nav.navigateTo(Destination.AccountSecurity) },
            onOpenStorage = { nav.navigateTo(Destination.StorageManagement) },
            onOpenResetCredits = { nav.navigateTo(Destination.CreditsReset) },
            onOpenCloudAudit = { nav.navigateTo(Destination.CloudConsistencyAudit) },
            onOpenFeedback = { nav.navigateTo(Destination.Feedback) },
            onOpenTerms = { nav.navigateTo(Destination.Legal(V2LegalDocType.TERMS)) },
            onOpenPrivacy = { nav.navigateTo(Destination.Legal(V2LegalDocType.PRIVACY)) },
            onLogoutComplete = {
                showQuickAdd = false
                showAnalysisSource = false
                cancelPendingAnalysisTrim(showSourceAgain = false)
                selectedTab = V2Tab.HOME
                nav.resetTo(Destination.Login)
            },
            onOpenTrajectory = { showGolfCamTrajectory = true },
            onOpenAnalysis = {
                if (profile.isLoggedIn) {
                    showAnalysisSource = true
                } else {
                    postLoginOpenAnalysisSource = true
                    openLogin(returnTo = null)
                }
            },
            onOpenOnlineDownload = { nav.navigateTo(Destination.OnlineDownload()) },
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
        )
        is Destination.NewVideoMemo -> V2MemoCreateVideoScreen(
            onClose = { nav.navigateBack() },
            onSaved = if (d.openPlayerAfterSave) {
                { memoId -> nav.resetTo(Destination.VideoMemoPlay(memoId)) }
            } else {
                null
            },
            onOpenOnlinePicker = { nav.navigateTo(Destination.OnlineVideoPicker()) },
            pendingOnline = pendingOnlinePick,
            onPendingOnlineConsumed = { pendingOnlinePick = null },
            editingMemoId = d.editingId,
            allowSourceReplacement = d.allowSourceReplacement,
        )
        is Destination.OnlineVideoPicker -> V2OnlineVideoSelectorScreen(
            onBack = { nav.navigateBack() },
            onConfirm = { pick ->
                pendingOnlinePick = pick
                nav.navigateBack()
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
            onOpenCreditsReset = { nav.navigateTo(Destination.CreditsReset) },
            onOpenCreditsPurchase = { nav.navigateTo(Destination.CreditsPurchase) },
            onRequestLoginForOnlineMemo = ::openLoginForOnlineMemo,
            onOpenOnlineMemo = { url ->
                pendingOnlinePick = null
                nav.navigateTo(Destination.NewVideoMemo())
                nav.navigateTo(Destination.OnlineVideoPicker(initialUrl = url))
            },
        )
        Destination.DownloadHistory -> V2DownloadHistoryScreen(
            onBack = { nav.navigateBack() },
            onStartDownload = { nav.navigateBack() },
            isLoggedIn = profile.isLoggedIn,
            onRequestLogin = { openLogin(continueTo = Destination.DownloadHistory) },
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
        )
        is Destination.JustSwingMain -> V2JustSwingMainScreen(
            onBack = { nav.navigateBack() },
            onOpenClips = { requireLoginForAccountContent(Destination.SwingCalendar()) },
            isLoggedIn = profile.isLoggedIn,
            onClipSaved = { clipId ->
                if (d.openClipAfterSave) nav.navigateTo(Destination.SwingClipDetail(clipId))
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
            onEditSource = { nav.navigateTo(Destination.NewVideoMemo(editingId = it, allowSourceReplacement = true)) },
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
            onRequestLogin = { openLogin() },
        )
        Destination.PlazaWatchHistory -> V2PlazaWatchHistoryScreen(
            onBack = { nav.navigateBack() },
            onOpenVideo = { nav.navigateTo(Destination.PlazaPlay(it)) },
            onRequestLogin = { openLoginForPlazaTarget(V2PlazaLoginTarget.WATCH_HISTORY) },
        )
        Destination.VideoMemoList -> V2VideoMemoListScreen(
            onBack = { nav.navigateBack() },
            onOpenMemo = { memoId ->
                nav.navigateTo(Destination.VideoMemoPlay(memoId))
            },
            onCreateVideoMemo = { requireLoginForAccountContent(Destination.NewVideoMemo()) },
        )
        Destination.TextMemoList -> V2TextMemoListScreen(
            onBack = { nav.navigateBack() },
            onOpenMemo = { memoId ->
                nav.navigateTo(Destination.MemoDetail(memoId))
            },
            onCreateTextMemo = { requireLoginForAccountContent(Destination.NewTextMemo) },
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
            onBack = { nav.navigateBack() },
            onMemoCreated = { nav.navigateTo(Destination.VideoMemoPlay(it)) },
        )
        Destination.AIArchive -> V2AIArchiveScreen(
            onBack = { nav.navigateBack() },
            onRequestLogin = { openLogin() },
        )
        Destination.ProfileSettings -> V2ProfileSettingsScreen(
            onBack = { nav.navigateBack() },
            onRequestLogin = { openLogin() },
            onRequestLoginForQr = ::openLoginForProfileQr,
            qrRequestKey = profileQrRequestKey,
        )
        Destination.AllMemos -> V2MemoAllListScreen(
            onBack = { nav.navigateBack() },
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
            onCreateMemo = { showQuickAdd = true },
            onCreateVideoMemo = { requireLoginForAccountContent(Destination.NewVideoMemo()) },
            onCreateTextMemo = { requireLoginForAccountContent(Destination.NewTextMemo) },
        )
        Destination.Login -> V2LoginScreen(
            onClose = null,
            onLoginSuccess = { completePostAuthRefresh() },
            onOpenRegister = { nav.navigateTo(Destination.Register) },
            onOpenResetPassword = { nav.navigateTo(Destination.ResetPassword) },
            onOpenTerms = { nav.navigateTo(Destination.Legal(V2LegalDocType.TERMS)) },
            onOpenPrivacy = { nav.navigateTo(Destination.Legal(V2LegalDocType.PRIVACY)) },
        )
        Destination.Register -> V2RegisterScreen(
            onBack = { nav.navigateBack() },
            onRegisterSuccess = { completePostAuthRefresh() },
            onOpenTerms = { nav.navigateTo(Destination.Legal(V2LegalDocType.TERMS)) },
            onOpenPrivacy = { nav.navigateTo(Destination.Legal(V2LegalDocType.PRIVACY)) },
        )
        Destination.ResetPassword -> V2ResetPasswordScreen(
            onBack = { nav.navigateBack() },
            onResetSuccess = { completePostAuthRefresh() },
        )
        Destination.CompleteProfile -> V2EditProfileScreen(
            title = "完善资料",
            onBack = { nav.navigateBack() },
            onSaved = {
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
            onOpenChangePassword = { nav.navigateTo(Destination.ChangePassword) },
            onOpenTerms = { nav.navigateTo(Destination.Legal(V2LegalDocType.TERMS)) },
            onOpenPrivacy = { nav.navigateTo(Destination.Legal(V2LegalDocType.PRIVACY)) },
            onOpenCloudAudit = { nav.navigateTo(Destination.CloudConsistencyAudit) },
            onOpenAccountDeletion = { nav.navigateTo(Destination.AccountDeletion) },
            onLogoutComplete = {
                showQuickAdd = false
                selectedTab = V2Tab.HOME
                nav.resetTo(Destination.Login)
            },
        )
        Destination.AccountDeletion -> V2AccountDeletionScreen(
            onBack = { nav.navigateBack() },
            onDeletionComplete = {
                selectedTab = V2Tab.HOME
                nav.resetTo(Destination.Login)
            },
            onRequestLogin = { openLogin() },
        )
        Destination.ChangePassword -> V2ChangePasswordScreen(
            onBack = { nav.navigateBack() },
            onChangeSuccess = { nav.navigateBack() },
        )
        Destination.StorageManagement -> V2StorageManagementScreen(
            onBack = { nav.navigateBack() },
        )
        Destination.CreditsReset -> V2CreditsResetScreen(
            onBack = { nav.navigateBack() },
            onRequestLogin = { openLogin() },
            onOpenPurchase = { nav.navigateTo(Destination.CreditsPurchase) },
        )
        Destination.CreditsPurchase -> V2CreditsPurchaseScreen(
            onBack = { nav.navigateBack() },
            onRequestLogin = { openLogin() },
        )
        Destination.AIConfig -> V2AIServiceConfigScreen(
            onBack = { nav.navigateBack() },
        )
        Destination.CloudConsistencyAudit -> V2ProfileCloudConsistencyAuditScreen(
            onBack = { nav.navigateBack() },
        )
        Destination.Feedback -> V2FeedbackScreen(
            onBack = { nav.navigateBack() },
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
        AnalysisSourceDialog(
            importing = analysisImporting,
            error = analysisImportError,
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
            onPickAlbum = {
                if (!profile.isLoggedIn) {
                    postLoginOpenAnalysisSource = true
                    openLogin(returnTo = null)
                } else {
                    analysisImportError = null
                    analysisImporting = true
                    showAnalysisSource = false
                    runCatching {
                        analysisVideoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                        )
                    }.onFailure {
                        analysisImporting = false
                        showAnalysisSource = true
                        analysisImportError = "无法打开相册，请稍后重试，或改用实时拍摄 / 素材库"
                    }
                }
            },
            onOpenLibrary = {
                showAnalysisSource = false
                analysisImportError = null
                requireLoginForAccountContent(Destination.SwingCalendar())
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
                    var prepared: V2VideoFileStore.ImportResult? = null
                    try {
                        prepared = V2VideoFileStore.trimImportedVideo(context, imported, startMs, endMs)
                        val finalImport = prepared ?: run {
                            analysisTrimError = "视频裁剪失败，请重新选择或直接使用原视频。"
                            return@launch
                        }
                        val clipId = V2SwingSessionStore.addClipFromVideoImportAndWait(
                            videoPath = finalImport.absolutePath,
                            thumbnailPath = finalImport.thumbnailPath,
                            durationSec = finalImport.durationSec,
                            sourceLabel = "首页回看分析导入",
                        )
                        if (clipId == null) {
                            if (finalImport.absolutePath != imported.absolutePath) {
                                cleanupImportedAnalysisClip(finalImport)
                            }
                            analysisTrimError = "无法保存为挥杆素材，请登录后重试"
                            return@launch
                        }
                        if (finalImport.absolutePath != imported.absolutePath) {
                            cleanupImportedAnalysisClip(imported)
                        }
                        pendingAnalysisImport = null
                        analysisTrimError = null
                        showAnalysisSource = false
                        nav.navigateTo(Destination.SwingClipDetail(clipId))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        prepared
                            ?.takeIf { it.absolutePath != imported.absolutePath }
                            ?.let(::cleanupImportedAnalysisClip)
                        analysisTrimError = "视频裁剪失败，请重新选择或直接使用原视频。"
                    } finally {
                        analysisTrimSaving = false
                    }
                }
            },
        )
    }

}

private fun cleanupImportedAnalysisClip(result: V2VideoFileStore.ImportResult) {
    deleteFileCountingFreedBytes(File(result.absolutePath))
    result.thumbnailPath?.let { deleteFileCountingFreedBytes(File(it)) }
}

@Composable
private fun TabsScaffold(
    selectedTab: V2Tab,
    onSelectTab: (V2Tab) -> Unit,
    onShowQuickAdd: () -> Unit,
    onOpenAllGoals: () -> Unit,
    onOpenGoalDetail: (UUID) -> Unit,
    onCreateGoal: () -> Unit,
    onCreateTextMemo: () -> Unit,
    onCreateVideoMemo: () -> Unit,
    onOpenAllMemos: () -> Unit,
    onOpenVideoMemoList: () -> Unit,
    onOpenTextMemoList: () -> Unit,
    onOpenSwingCalendar: () -> Unit,
    onOpenSwingClip: (UUID) -> Unit,
    onOpenGrowthPool: () -> Unit,
    onOpenJustSwing: () -> Unit,
    onOpenAIArchive: () -> Unit,
    onOpenProfileSettings: () -> Unit,
    onOpenAIConfig: () -> Unit,
    onRequestLoginForAIHistory: () -> Unit,
    aiHistoryRequestKey: Int,
    onSwitchToAI: () -> Unit,
    onOpenMemoDetail: (UUID) -> Unit,
    onOpenPlazaVideo: (UUID) -> Unit,
    onOpenPlazaWatchHistory: () -> Unit,
    onRequestLogin: () -> Unit,
    onRequestLoginForProfileTarget: (V2ProfileLoginTarget) -> Unit,
    onRequestLoginForPlazaTarget: (V2PlazaLoginTarget) -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenAccountSecurity: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenResetCredits: () -> Unit,
    onOpenCloudAudit: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onLogoutComplete: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTrajectory: () -> Unit = {},
    onOpenAnalysis: () -> Unit = {},
    onOpenOnlineDownload: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DSColors.warmWhite,
        bottomBar = {
            V2BottomTabBar(
                selected = selectedTab,
                onSelect = onSelectTab,
            )
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
                    onOpenPlazaVideo = onOpenPlazaVideo,
                    onOpenSwingCalendar = onOpenSwingCalendar,
                    onOpenGrowthPool = onOpenGrowthPool,
                    onOpenJustSwing = onOpenJustSwing,
                    onAddTap = onShowQuickAdd,
                    onOpenTrajectory = onOpenTrajectory,
                    onOpenAnalysis = onOpenAnalysis,
                    onOpenOnlineDownload = onOpenOnlineDownload,
                )
                V2Tab.MEMO -> V2MemoListScreen(
                    onCreateMemo = onShowQuickAdd,
                    onCreateVideoMemo = onCreateVideoMemo,
                    onCreateTextMemo = onCreateTextMemo,
                    onOpenMemo = onOpenMemoDetail,
                    onOpenVideoList = onOpenVideoMemoList,
                    onOpenTextList = onOpenTextMemoList,
                    onOpenSwingCalendar = onOpenSwingCalendar,
                    onOpenSwingClip = onOpenSwingClip,
                    onOpenJustSwing = onOpenJustSwing,
                    onOpenAllMemos = onOpenAllMemos,
                    onSwitchToAI = onSwitchToAI,
                )
                V2Tab.AI -> V2AIChatScreen(
                    onSwitchToHome = { onSelectTab(V2Tab.HOME) },
                    onOpenConfig = onOpenAIConfig,
                    onRequestLogin = onRequestLogin,
                    onRequestLoginForHistory = onRequestLoginForAIHistory,
                    historyRequestKey = aiHistoryRequestKey,
                )
                V2Tab.PLAZA -> V2PlazaScreen(
                    onOpenVideo = onOpenPlazaVideo,
                    onOpenWatchHistory = onOpenPlazaWatchHistory,
                    onRequestLogin = onRequestLogin,
                    onRequestLoginForTarget = onRequestLoginForPlazaTarget,
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
                    onOpenCloudAudit = onOpenCloudAudit,
                    onOpenAllGoals = onOpenAllGoals,
                    onOpenVideoMemoList = onOpenVideoMemoList,
                    onOpenSwingCalendar = onOpenSwingCalendar,
                    onLogoutComplete = onLogoutComplete,
                )
            }
        }
    }
}

@Composable
private fun AnalysisSourceDialog(
    importing: Boolean,
    error: String?,
    onRecord: () -> Unit,
    onPickAlbum: () -> Unit,
    onOpenLibrary: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
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
                    style = DSTypography.titleMedium,
                    color = DSColors.inkGreen,
                )
                Text(
                    text = "选择视频来源，进入帧级分析工具",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                AnalysisSourceOption(
                    title = "实时拍摄",
                    subtitle = "使用摄像头录制挥杆动作",
                    icon = Icons.Filled.CameraAlt,
                    prominent = true,
                    enabled = !importing,
                    onClick = onRecord,
                )
                AnalysisSourceOption(
                    title = if (importing) "正在读取相册视频" else "从相册选择",
                    subtitle = if (importing) "大视频可能需要几秒钟，请稍候" else "导入 MP4 或 MOV，选择后可先裁剪关键片段",
                    icon = Icons.Filled.VideoLibrary,
                    prominent = false,
                    enabled = !importing,
                    onClick = onPickAlbum,
                )
                AnalysisSourceOption(
                    title = "已有素材库",
                    subtitle = "打开挥杆记录或成长池，继续逐帧回看",
                    icon = Icons.Filled.FolderOpen,
                    prominent = false,
                    enabled = !importing,
                    onClick = onOpenLibrary,
                )
                error?.let {
                    Text(
                        text = it,
                        style = DSTypography.caption,
                        color = DSColors.flagRed,
                    )
                }
                Text(
                    text = "支持 MP4、MOV 格式，最大 2GB；建议选择清晰的正面或侧面挥杆视频",
                    style = DSTypography.caption,
                    color = DSColors.shadowGray,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !importing) {
                Text("取消", color = DSColors.fairway)
            }
        },
        containerColor = DSColors.warmWhite,
    )
}

@Composable
private fun AnalysisSourceOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    prominent: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val contentAlpha = if (enabled) 1f else 0.58f
    val shape = RoundedCornerShape(18.dp)
    val background = if (prominent) {
        Brush.linearGradient(listOf(DSColors.deepGreen, DSColors.fairway))
    } else {
        Brush.linearGradient(listOf(DSColors.cardWhite, DSColors.cardWhite))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(shape)
            .background(background)
            .border(
                width = 1.dp,
                color = if (prominent) Color.Transparent else DSColors.borderLine,
                shape = shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = DSSpacing.md),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (prominent) Color.White.copy(alpha = 0.16f) else DSColors.mistGreen),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = (if (prominent) Color.White else DSColors.fairway).copy(alpha = contentAlpha),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = (if (prominent) Color.White else DSColors.inkGreen).copy(alpha = contentAlpha),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = (if (prominent) Color.White.copy(alpha = 0.72f) else DSColors.shadowGray).copy(alpha = contentAlpha),
                style = DSTypography.caption,
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = (if (prominent) Color.White.copy(alpha = 0.72f) else DSColors.shadowGray).copy(alpha = contentAlpha),
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

@Preview(showBackground = true)
@Composable
private fun V2RootScreenPreview() {
    MyGolfSwingAppAndroidTheme {
        V2RootScreen()
    }
}
