package com.mygolfswing.app.data

import android.content.Context
import com.mygolfswing.app.data.ai.V2AIService
import com.mygolfswing.app.data.db.V2Database
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.repo.V2AIChatRepository
import com.mygolfswing.app.data.repo.V2AuthRepository
import com.mygolfswing.app.data.repo.V2CreditsRepository
import com.mygolfswing.app.data.repo.V2FeedbackRepository
import com.mygolfswing.app.data.repo.V2GoalRepository
import com.mygolfswing.app.data.repo.V2MemoRepository
import com.mygolfswing.app.data.repo.V2GolfProfileRepository
import com.mygolfswing.app.data.repo.V2ProfileCloudConsistencyAuditRepository
import com.mygolfswing.app.data.repo.V2PlazaRepository
import com.mygolfswing.app.data.repo.V2SwingSessionRepository
import com.mygolfswing.app.data.repo.V2VideoDownloadRepository
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
    lateinit var aiService: V2AIService
        private set

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Synchronized
    fun init(context: Context) {
        if (::db.isInitialized) return
        db = V2Database.get(context)
        session = V2SessionStore(context)
        net = V2NetClient(session)

        authRepo = V2AuthRepository(net, session)
        creditsRepo = V2CreditsRepository(net, session)
        feedbackRepo = V2FeedbackRepository(net, session)
        videoDownloadRepo = V2VideoDownloadRepository(net, session)
        goalRepo = V2GoalRepository(db.goals(), net, session, appScope)
        memoRepo = V2MemoRepository(context.applicationContext, db.memos(), net, session, appScope)
        plazaRepo = V2PlazaRepository(db.plazaSaved(), net, session)
        swingRepo = V2SwingSessionRepository(db.swingSessions(), net)
        golfProfileRepo = V2GolfProfileRepository(db.golfProfile(), net, session)
        aiChatRepo = V2AIChatRepository(db.aiChat())
        cloudConsistencyAuditRepo = V2ProfileCloudConsistencyAuditRepository(db.goals(), db.memos(), db.aiChat(), net, session)
        aiService = V2AIService(net, session)
    }
}
