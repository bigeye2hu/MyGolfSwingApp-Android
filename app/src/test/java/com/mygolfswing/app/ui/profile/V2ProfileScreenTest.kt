package com.mygolfswing.app.ui.profile

import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2Topic
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2ProfileScreenTest {

    @Test
    fun profileStatsHidePrivateDataWhenLoggedOut() {
        val goal = demoNamedRealGoal()
        val memo = demoNamedRealMemo()

        assertTrue(profileDisplayGoals(isLoggedIn = false, goals = listOf(goal)).isEmpty())
        assertTrue(profileDisplayMemos(isLoggedIn = false, memos = listOf(memo)).isEmpty())
    }

    @Test
    fun profileStatsKeepUserContentThatSharesOldDemoTitles() {
        val goal = demoNamedRealGoal()
        val memo = demoNamedRealMemo()

        assertEquals(listOf(goal), profileDisplayGoals(isLoggedIn = true, goals = listOf(goal)))
        assertEquals(listOf(memo), profileDisplayMemos(isLoggedIn = true, memos = listOf(memo)))
    }

    @Test
    fun profileLogoutThrowableErrorKeepsOrdinaryFailuresHandled() {
        assertEquals(
            "退出登录失败，请稍后重试",
            profileLogoutThrowableError(IllegalStateException("logout failed token=/users/private")),
        )
    }

    @Test
    fun profileLogoutThrowableErrorPreservesCoroutineCancellation() {
        try {
            profileLogoutThrowableError(CancellationException("profile logout disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("profile logout disposed", e.message)
        }
    }

    private fun demoNamedRealGoal(): V2Goal =
        V2Goal.count(
            title = "每日复盘心法",
            iconType = V2Goal.IconType.TIP,
            topic = V2Topic.MIND,
            target = 21.0,
            unit = "天",
        )

    private fun demoNamedRealMemo(): V2Memo =
        V2Memo.textMemo(
            title = "握把压力的微调",
            body = "这是用户自己记录的真实练习内容，不能只因为标题像旧演示数据就隐藏。",
            topic = V2Topic.SWING,
        )
}
