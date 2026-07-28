package com.mygolfswing.app.ui.profile

import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.store.V2UserProfile

/**
 * 开发者工具可见性（仅调试包内部测试账号）。
 *
 * 「云端一致性」等调试入口只对 debuggable build 的 [CLOUD_AUDIT_USERNAME] 开放，
 * 避免 release 包暴露 Android-only 工程诊断入口。
 */
object V2DeveloperToolsAccess {

    private const val CLOUD_AUDIT_USERNAME = "test1"

    fun isCloudAuditEnabled(profile: V2UserProfile): Boolean {
        if (!ServiceLocator.isDebuggable) return false
        if (!profile.isLoggedIn) return false
        val displayUsername = profile.username.removePrefix("@").trim()
        return displayUsername.equals(CLOUD_AUDIT_USERNAME, ignoreCase = true)
    }
}
