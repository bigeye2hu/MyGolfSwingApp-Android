package com.mygolfswing.app.data

import android.content.Context
import android.content.pm.ApplicationInfo
import com.mygolfswing.app.data.ai.V2AIService
import com.mygolfswing.app.data.db.V2Database
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.friend.V2FriendSpaceSyncRepository
import com.mygolfswing.app.data.friend.V2TencentFriendRepository
import com.mygolfswing.app.data.repo.V2AIChatRepository
import com.mygolfswing.app.data.repo.V2AppUpdateRepository
import com.mygolfswing.app.data.repo.V2CategoryRepository
import com.mygolfswing.app.data.repo.V2CoachWorkspaceRepository
import com.mygolfswing.app.data.repo.V2CreditsRepository
import com.mygolfswing.app.data.repo.V2FeedbackRepository
import com.mygolfswing.app.data.repo.V2GoalRepository
import com.mygolfswing.app.data.repo.V2MemoRepository
import com.mygolfswing.app.data.repo.V2GolfProfileRepository
import com.mygolfswing.app.data.repo.V2ProfileCloudConsistencyAuditRepository
import com.mygolfswing.app.data.repo.V2PlazaRepository
import com.mygolfswing.app.data.repo.V2SwingSessionRepository
import com.mygolfswing.app.data.repo.V2VideoDownloadRepository
import com.mygolfswing.app.data.repo.V2AuthRepository
import com.mygolfswing.app.data.session.V2SessionStore
import com.mygolfswing.app.data.role.V2RoleStore
import com.mygolfswing.app.data.coach.V2CoachDeliveryQueue
import com.mygolfswing.app.data.coach.V2LibraryShareDraftStore
import com.mygolfswing.app.data.coach.V2WorkspaceDraftStore
import com.mygolfswing.app.data.download.V2AsyncDownloadQueue
import com.mygolfswing.app.data.store.V2CategoryStore
import com.mygolfswing.app.data.video.V2VideoFileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 轻量依赖注入容器：进程内单例，[V2App.onCreate] 调用 [init] 完成构造。
 *
 * 暴露 4 个 Repository（Goal / Memo / Plaza / Auth）+ [V2AIService] + [V2SessionStore]，
 * 上层 Compose 直接调用静态属性即可。
 */
object ServiceLocator {

    lateinit var db: V2Database
        private set
    lateinit var session: V2SessionStore
        private set
    lateinit var roleStore: V2RoleStore
        private set
    lateinit var net: V2NetClient
        private set

    lateinit var goalRepo: V2GoalRepository
        private set
    lateinit var memoRepo: V2MemoRepository
        private set
    lateinit var plazaRepo: V2PlazaRepository
        private set
    lateinit var authRepo: V2AuthRepository
        private set
    lateinit var creditsRepo: V2CreditsRepository
        private set
    lateinit var feedbackRepo: V2FeedbackRepository
        private set
    lateinit var videoDownloadRepo: V2VideoDownloadRepository
        private set
    lateinit var swingRepo: V2SwingSessionRepository
        private set
    lateinit var golfProfileRepo: V2GolfProfileRepository
        private set
    lateinit var aiChatRepo: V2AIChatRepository
        private set
    lateinit var cloudConsistencyAuditRepo: V2ProfileCloudConsistencyAuditRepository
        private set
    lateinit var categoryRepo: V2CategoryRepository
        private set
    lateinit var coachWorkspaceRepo: V2CoachWorkspaceRepository
        private set
    lateinit var appUpdateRepo: V2AppUpdateRepository
        private set
    lateinit var coachDeliveryQueue: V2CoachDeliveryQueue
        private set
    lateinit var asyncDownloadQueue: V2AsyncDownloadQueue
        private set
    lateinit var tencentFriendRepo: V2TencentFriendRepository
        private set
    lateinit var friendSpaceSyncRepo: V2FriendSpaceSyncRepository
        private set
    lateinit var appContext: Context
        private set

    var isDebuggable: Boolean = false
        private set

    lateinit var aiService: V2AIService
        private set

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Synchronized
    fun init(context: Context) {
        if (::db.isInitialized) return
        appContext = context.applicationContext
        db = V2Database.get(context)
        session = V2SessionStore(context)
        roleStore = V2RoleStore(context)
        isDebuggable = appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        net = V2NetClient(session, isDebuggable)

        tencentFriendRepo = V2TencentFriendRepository(appContext, isDebuggable)
        authRepo = V2AuthRepository(net, session, onLogout = {
            val ownerUserId = session.current().userId
            runCatching { tencentFriendRepo.logout() }
            if (!ownerUserId.isNullOrBlank()) {
                runCatching { coachDeliveryQueue.clearOwner(ownerUserId) }
                runCatching { asyncDownloadQueue.clearOwner(ownerUserId) }
                runCatching { V2VideoFileStore.clearWorkspaceAnalysisCache(appContext, ownerUserId) }
            }
            V2WorkspaceDraftStore(appContext).clearAll()
            V2LibraryShareDraftStore(appContext).clearAll()
        })
        creditsRepo = V2CreditsRepository(net, session)
        feedbackRepo = V2FeedbackRepository(net, session)
        videoDownloadRepo = V2VideoDownloadRepository(net, session)
        goalRepo = V2GoalRepository(db.goals(), net, session, appScope)
        memoRepo = V2MemoRepository(context.applicationContext, db.memos(), net, session, appScope)
        plazaRepo = V2PlazaRepository(db.plazaSaved(), net, session)
        swingRepo = V2SwingSessionRepository(db.swingSessions(), net, session)
        golfProfileRepo = V2GolfProfileRepository(db.golfProfile(), net, session)
        aiChatRepo = V2AIChatRepository(db.aiChat(), session)
        cloudConsistencyAuditRepo = V2ProfileCloudConsistencyAuditRepository(db.goals(), db.memos(), db.aiChat(), net, session)
        categoryRepo = V2CategoryRepository(appContext, net)
        coachWorkspaceRepo = V2CoachWorkspaceRepository(net, authRepo)
        friendSpaceSyncRepo = V2FriendSpaceSyncRepository(session, authRepo, tencentFriendRepo, coachWorkspaceRepo)
        appUpdateRepo = V2AppUpdateRepository(appContext, net)
        coachDeliveryQueue = V2CoachDeliveryQueue(appContext)
        asyncDownloadQueue = V2AsyncDownloadQueue(appContext)
        appScope.launch { asyncDownloadQueue.recover(session.current().userId) }
        V2CategoryStore.init(categoryRepo)
        aiService = V2AIService(net, session)
    }
}
