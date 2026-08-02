package com.mygolfswing.app.ui.profile

import com.mygolfswing.app.data.store.V2UserProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2DeveloperToolsAccessTest {

    @Test
    fun cloudAuditIsNeverEnabledForReleaseBuilds() {
        val profile = V2UserProfile(
            userId = "internal-user",
            username = "@test1",
            isLoggedIn = true,
        )

        assertFalse(V2DeveloperToolsAccess.isCloudAuditEnabled(profile, isDebuggable = false))
    }

    @Test
    fun cloudAuditRequiresLoggedInInternalDebugAccount() {
        assertTrue(
            V2DeveloperToolsAccess.isCloudAuditEnabled(
                V2UserProfile(userId = "internal-user", username = " @TeSt1 ", isLoggedIn = true),
                isDebuggable = true,
            ),
        )
        assertFalse(
            V2DeveloperToolsAccess.isCloudAuditEnabled(
                V2UserProfile(userId = "internal-user", username = "@other", isLoggedIn = true),
                isDebuggable = true,
            ),
        )
        assertFalse(
            V2DeveloperToolsAccess.isCloudAuditEnabled(
                V2UserProfile(userId = "internal-user", username = "@test1", isLoggedIn = false),
                isDebuggable = true,
            ),
        )
    }
}
