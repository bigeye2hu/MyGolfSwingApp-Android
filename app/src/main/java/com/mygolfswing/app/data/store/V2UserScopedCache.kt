package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator
import kotlinx.coroutines.launch

/**
 * 用户隔离缓存协调器。
 *
 * - **本地持久化**（Room / SharedPreferences / `files/users/{userId}`）按 userId 分区，
 *   供同账号再次登录时离线读取，不会跨账号混读（DAO 查询均带 userId 条件）。
 * - **内存态**（各 Store 的 StateFlow）在登出或切换账号时必须立即清空，
 *   避免上一账号数据在 UI 上短暂残留。
 */
object V2UserScopedCache {

    @Volatile private var boundUserId: String? = null
    @Volatile private var sessionGuardStarted = false

    /** 进程内调用一次：监听 session，在 userId 变化时自动隔离内存缓存。 */
    fun startSessionGuard() {
        if (sessionGuardStarted) return
        sessionGuardStarted = true
        ServiceLocator.appScope.launch {
            ServiceLocator.session.state.collect { session ->
                val activeId = session.userId?.takeIf { session.isLoggedIn }
                onActiveUserChanged(activeId)
            }
        }
    }

    /**
     * 清空所有内存态用户数据（不删 Room / 文件缓存）。
     * 登出、token 失效、账号切换前调用。
     */
    fun clearActiveUserMemory() {
        V2GoalStore.resetInMemory()
        V2MemoStore.resetInMemory()
        V2SwingSessionStore.resetInMemory()
        V2GolfProfileStore.resetInMemory()
        V2AIChatStore.resetInMemory()
        V2PlazaStore.resetUserState()
        V2CategoryStore.clearUserData()
        ServiceLocator.creditsRepo.reset()
    }

    /** 登出流程：清内存 + 解除当前绑定（本地分区缓存保留）。 */
    fun onLogout() {
        clearActiveUserMemory()
        boundUserId = null
    }

    private fun onActiveUserChanged(activeUserId: String?) {
        val previous = boundUserId
        if (activeUserId == previous) return
        // 离开某账号（登出）或 A→B 直切：先清内存，再绑定新 userId
        if (previous != null || activeUserId == null) {
            clearActiveUserMemory()
        }
        boundUserId = activeUserId
    }
}
