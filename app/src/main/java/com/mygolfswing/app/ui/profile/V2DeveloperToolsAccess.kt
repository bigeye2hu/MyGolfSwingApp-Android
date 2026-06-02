package com.mygolfswing.app.ui.profile

import com.mygolfswing.app.data.store.V2UserProfile

/**
 * 开发者工具可见性（仅内部测试账号）。
 *
 * 「云端一致性」等调试入口只对 [CLOUD_AUDIT_USERNAME] 开放。
 */
object V2DeveloperToolsAccess {

    private const val CLOUD_AUDIT_USERNAME = "test1"

    fun isCloudAuditEnabled(profile: V2UserProfile): Boolean {
        if (!profile.isLoggedIn) return false
        val displayUsername = profile.username.removePrefix("@").trim()
        return displayUsername.equals(CLOUD_AUDIT_USERNAME, ignoreCase = true)
    }
}
