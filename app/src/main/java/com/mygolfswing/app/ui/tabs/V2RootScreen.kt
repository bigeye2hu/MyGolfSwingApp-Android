package com.mygolfswing.app.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.mygolfswing.app.ui.common.V2BackHandler
import com.mygolfswing.app.ui.common.rememberV2AppNavigation
import com.mygolfswing.app.ui.common.V2SplashScreen
import com.mygolfswing.app.ui.memo.V2OnlineVideoSelection
import com.mygolfswing.app.ui.memo.V2OnlineVideoSelectorScreen
import com.mygolfswing.app.ui.swing.V2JustSwingMainScreen
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.store.V2GoalStore
import com.mygolfswing.app.data.store.V2GolfProfileStore
import com.mygolfswing.app.data.store.V2MemoStore
import com.mygolfswing.app.data.store.V2PlazaStore
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.ui.ai.V2AIChatScreen
import com.mygolfswing.app.ui.auth.V2ChangePasswordScreen
import com.mygolfswing.app.ui.auth.V2LoginScreen
import com.mygolfswing.app.ui.auth.V2RegisterScreen
import com.mygolfswing.app.ui.auth.V2ResetPasswordScreen
import com.mygolfswing.app.ui.feedback.V2FeedbackScreen
import com.mygolfswing.app.ui.legal.V2LegalDocType
import com.mygolfswing.app.ui.legal.V2LegalScreen
import com.mygolfswing.app.ui.settings.V2AIServiceConfigScreen
import com.mygolfswing.app.ui.settings.V2StorageManagementScreen
import com.mygolfswing.app.ui.designsystem.DSColors
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
import com.mygolfswing.app.ui.memo.V2MemoAllListScreen
import com.mygolfswing.app.ui.memo.V2MemoListScreen
import com.mygolfswing.app.ui.memo.V2QuickAddSheet
import com.mygolfswing.app.ui.memo.V2TextMemoListScreen
import com.mygolfswing.app.ui.memo.V2VideoMemoListScreen
import com.mygolfswing.app.ui.memo.V2VideoMemoPlayerScreen
import com.mygolfswing.app.ui.plaza.V2PlazaPlayerScreen
import com.mygolfswing.app.ui.plaza.V2PlazaScreen
import com.mygolfswing.app.ui.profile.V2AIArchiveScreen
import com.mygolfswing.app.ui.profile.V2EditProfileScreen
import com.mygolfswing.app.ui.profile.V2ProfileCloudConsistencyAuditScreen
import com.mygolfswing.app.ui.profile.V2ProfileScreen
import com.mygolfswing.app.ui.swing.V2JustSwingCalendarScreen
import com.mygolfswing.app.ui.swing.V2SwingDayDetailScreen
import com.mygolfswing.app.ui.theme.MyGolfSwingAppAndroidTheme
import kotlinx.coroutines.launch
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
    data object NewVideoMemo : Destination()
    data class MemoDetail(val id: UUID) : Destination()
    data class VideoMemoPlay(val id: UUID) : Destination()
    data class PlazaPlay(val id: UUID) : Destination()
    data object AllMemos : Destination()
    data object VideoMemoList : Destination()
    data object TextMemoList : Destination()
    data object SwingCalendar : Destination()
    data class SwingDayDetail(val day: LocalDate) : Destination()
    data object JustSwingMain : Destination()
    data object OnlineVideoPicker : Destination()
    data object AIArchive : Destination()
    data object Login : Destination()
    data object Register : Destination()
    data object ResetPassword : Destination()
    data object EditProfile : Destination()
    data object ChangePassword : Destination()
    data object StorageManagement : Destination()
    data object AIConfig : Destination()
    data object CloudConsistencyAudit : Destination()
    data object Feedback : Destination()
    data class Legal(val type: V2LegalDocType) : Destination()
}

/**
 * V2 Android 根容器（对齐 iOS `V2RootView`）。
 */
@Composable
fun V2RootScreen(modifier: Modifier = Modifier) {
    var selectedTab by rememberSaveable { mutableStateOf(V2Tab.HOME) }
    val nav = rememberV2AppNavigation<Destination>(Destination.Tabs)
    var showQuickAdd by remember { mutableStateOf(false) }
    var bootstrapping by remember { mutableStateOf(true) }
    var pendingOnlinePick by remember { mutableStateOf<V2OnlineVideoSelection?>(null) }

    LaunchedEffect(Unit) {
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
                ServiceLocator.authRepo.logout()
                nav.resetTo(Destination.Login)
            } else {
                V2PlazaStore.refresh()
                V2GoalStore.syncFromCloud()
                ServiceLocator.goalRepo.pushDirty()
                ServiceLocator.creditsRepo.fetchBalance(force = true)
                V2MemoStore.pullFromCloud()
                runCatching { V2GolfProfileStore.refreshFromCloud() }
            }
        } else {
            nav.resetTo(Destination.Login)
        }
        bootstrapping = false
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
                }
            },
            onShowQuickAdd = { showQuickAdd = true },
            onOpenAllGoals = { nav.navigateTo(Destination.AllGoals) },
            onOpenGoalDetail = { nav.navigateTo(Destination.GoalDetail(it)) },
            onCreateGoal = { nav.navigateTo(Destination.NewGoal()) },
            onCreateTextMemo = { nav.navigateTo(Destination.NewTextMemo) },
            onCreateVideoMemo = { nav.navigateTo(Destination.NewVideoMemo) },
            onOpenAllMemos = { nav.navigateTo(Destination.AllMemos) },
            onOpenVideoMemoList = { nav.navigateTo(Destination.VideoMemoList) },
            onOpenTextMemoList = { nav.navigateTo(Destination.TextMemoList) },
            onOpenSwingCalendar = { nav.navigateTo(Destination.SwingCalendar) },
            onOpenSwingDay = { nav.navigateTo(Destination.SwingDayDetail(it)) },
            onOpenJustSwing = { nav.navigateTo(Destination.JustSwingMain) },
            onOpenAIArchive = { nav.navigateTo(Destination.AIArchive) },
            onOpenAIConfig = { nav.navigateTo(Destination.AIConfig) },
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
            onRequestLogin = { nav.navigateTo(Destination.Login) },
            onOpenEditProfile = { nav.navigateTo(Destination.EditProfile) },
            onOpenAccountSecurity = { nav.navigateTo(Destination.ChangePassword) },
            onOpenStorage = { nav.navigateTo(Destination.StorageManagement) },
            onOpenCloudAudit = { nav.navigateTo(Destination.CloudConsistencyAudit) },
            onOpenFeedback = { nav.navigateTo(Destination.Feedback) },
            onOpenTerms = { nav.navigateTo(Destination.Legal(V2LegalDocType.TERMS)) },
            onOpenPrivacy = { nav.navigateTo(Destination.Legal(V2LegalDocType.PRIVACY)) },
            onLogoutComplete = {
                showQuickAdd = false
                selectedTab = V2Tab.HOME
                nav.resetTo(Destination.Login)
            },
            modifier = modifier,
        )
        Destination.AllGoals -> V2AllGoalsScreen(
            onBack = { nav.navigateBack() },
            onCreate = { nav.navigateTo(Destination.NewGoal()) },
            onSelectGoal = { nav.navigateTo(Destination.GoalDetail(it)) },
        )
        is Destination.NewGoal -> V2NewGoalScreen(
            editingGoalId = d.editingId,
            onClose = { nav.navigateBack() },
            onDeleted = { nav.resetToRoot() },
        )
        is Destination.GoalDetail -> V2GoalDetailScreen(
            goalId = d.id,
            onBack = { nav.navigateBack() },
            onEdit = { nav.navigateTo(Destination.NewGoal(editingId = d.id)) },
        )
        Destination.NewTextMemo -> V2MemoCreateTextScreen(
            onClose = { nav.navigateBack() },
        )
        Destination.NewVideoMemo -> V2MemoCreateVideoScreen(
            onClose = { nav.navigateBack() },
            onOpenOnlinePicker = { nav.navigateTo(Destination.OnlineVideoPicker) },
            pendingOnline = pendingOnlinePick,
            onPendingOnlineConsumed = { pendingOnlinePick = null },
        )
        Destination.OnlineVideoPicker -> V2OnlineVideoSelectorScreen(
            onBack = { nav.navigateBack() },
            onConfirm = { pick ->
                pendingOnlinePick = pick
                nav.navigateBack()
            },
        )
        Destination.JustSwingMain -> V2JustSwingMainScreen(
            onBack = { nav.navigateBack() },
        )
        is Destination.MemoDetail -> V2MemoDetailScreen(
            memoId = d.id,
            onBack = { nav.navigateBack() },
        )
        is Destination.VideoMemoPlay -> V2VideoMemoPlayerScreen(
            memoId = d.id,
            onBack = { nav.navigateBack() },
        )
        is Destination.PlazaPlay -> V2PlazaPlayerScreen(
            videoId = d.id,
            onClose = { nav.navigateBack() },
        )
        Destination.VideoMemoList -> V2VideoMemoListScreen(
            onBack = { nav.navigateBack() },
            onOpenMemo = { memoId ->
                nav.navigateTo(Destination.VideoMemoPlay(memoId))
            },
        )
        Destination.TextMemoList -> V2TextMemoListScreen(
            onBack = { nav.navigateBack() },
            onOpenMemo = { memoId ->
                nav.navigateTo(Destination.MemoDetail(memoId))
            },
        )
        Destination.SwingCalendar -> V2JustSwingCalendarScreen(
            onBack = { nav.navigateBack() },
            onOpenDay = { nav.navigateTo(Destination.SwingDayDetail(it)) },
        )
        is Destination.SwingDayDetail -> V2SwingDayDetailScreen(
            day = d.day,
            onBack = { nav.navigateBack() },
        )
        Destination.AIArchive -> V2AIArchiveScreen(
            onBack = { nav.navigateBack() },
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
        )
        Destination.Login -> V2LoginScreen(
            onClose = null,
            onLoginSuccess = {
                nav.resetToRoot()
                V2ProfileStore.refresh(force = true)
                V2PlazaStore.refresh()
                V2MemoStore.pullFromCloud()
                V2GoalStore.syncFromCloud()
                ServiceLocator.appScope.launch {
                    ServiceLocator.creditsRepo.fetchBalance(force = true)
                    runCatching { V2GolfProfileStore.refreshFromCloud() }
                }
            },
            onOpenRegister = { nav.navigateTo(Destination.Register) },
            onOpenResetPassword = { nav.navigateTo(Destination.ResetPassword) },
        )
        Destination.Register -> V2RegisterScreen(
            onBack = { nav.navigateBack() },
            onRegisterSuccess = {
                nav.resetToRoot()
                V2ProfileStore.refresh(force = true)
            },
        )
        Destination.ResetPassword -> V2ResetPasswordScreen(
            onBack = { nav.navigateBack() },
            onResetSuccess = {
                nav.resetTo(Destination.Login)
            },
        )
        Destination.EditProfile -> V2EditProfileScreen(
            onBack = { nav.navigateBack() },
            onSaved = {
                nav.navigateBack()
                V2ProfileStore.refresh(force = true)
            },
        )
        Destination.ChangePassword -> V2ChangePasswordScreen(
            onBack = { nav.navigateBack() },
            onChangeSuccess = { nav.navigateBack() },
        )
        Destination.StorageManagement -> V2StorageManagementScreen(
            onBack = { nav.navigateBack() },
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
            onCreateGoal = { nav.navigateTo(Destination.NewGoal()) },
            onCreateVideoMemo = { nav.navigateTo(Destination.NewVideoMemo) },
            onCreateTextMemo = { nav.navigateTo(Destination.NewTextMemo) },
            onCreateSwingClip = {
                showQuickAdd = false
                nav.navigateTo(Destination.JustSwingMain)
            },
            onDismiss = { showQuickAdd = false },
        )
    }

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
    onOpenSwingDay: (LocalDate) -> Unit,
    onOpenJustSwing: () -> Unit,
    onOpenAIArchive: () -> Unit,
    onOpenAIConfig: () -> Unit,
    onSwitchToAI: () -> Unit,
    onOpenMemoDetail: (UUID) -> Unit,
    onOpenPlazaVideo: (UUID) -> Unit,
    onRequestLogin: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenAccountSecurity: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenCloudAudit: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onLogoutComplete: () -> Unit,
    modifier: Modifier = Modifier,
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
                    onOpenPlazaVideo = onOpenPlazaVideo,
                    onAddTap = onShowQuickAdd,
                )
                V2Tab.MEMO -> V2MemoListScreen(
                    onCreateMemo = onShowQuickAdd,
                    onCreateVideoMemo = onCreateVideoMemo,
                    onCreateTextMemo = onCreateTextMemo,
                    onOpenMemo = onOpenMemoDetail,
                    onOpenVideoList = onOpenVideoMemoList,
                    onOpenTextList = onOpenTextMemoList,
                    onOpenSwingCalendar = onOpenSwingCalendar,
                    onOpenSwingDay = onOpenSwingDay,
                    onOpenJustSwing = onOpenJustSwing,
                    onOpenAllMemos = onOpenAllMemos,
                    onSwitchToAI = onSwitchToAI,
                )
                V2Tab.AI -> V2AIChatScreen(
                    onSwitchToHome = { onSelectTab(V2Tab.HOME) },
                    onOpenConfig = onOpenAIConfig,
                )
                V2Tab.PLAZA -> V2PlazaScreen(
                    onOpenVideo = onOpenPlazaVideo,
                )
                V2Tab.ME -> V2ProfileScreen(
                    onRequestLogin = onRequestLogin,
                    onOpenEditProfile = onOpenEditProfile,
                    onOpenAccountSecurity = onOpenAccountSecurity,
                    onOpenStorage = onOpenStorage,
                    onOpenAIConfig = onOpenAIConfig,
                    onOpenAIArchive = onOpenAIArchive,
                    onOpenFeedback = onOpenFeedback,
                    onOpenTerms = onOpenTerms,
                    onOpenPrivacy = onOpenPrivacy,
                    onOpenCloudAudit = onOpenCloudAudit,
                    onLogoutComplete = onLogoutComplete,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun V2RootScreenPreview() {
    MyGolfSwingAppAndroidTheme {
        V2RootScreen()
    }
}
