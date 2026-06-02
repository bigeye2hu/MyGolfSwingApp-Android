package com.mygolfswing.app.data.repo

import android.content.Context
import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.store.V2UserScopedCache
import com.mygolfswing.app.data.store.V2ProfileStore
import com.mygolfswing.app.data.video.V2VideoFileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** 账号删除步骤（对齐 iOS `DeletionStep`） */
enum class V2AccountDeletionStep(val label: String) {
    PREPARING("准备删除数据..."),
    CLOUD_DATA("清理云端数据..."),
    LOCAL_CONVERSATIONS("清理 AI 对话记录..."),
    LOCAL_MEMOS("清理视频备忘录..."),
    LOCAL_SWINGS("清理挥杆记录..."),
    DELETE_ACCOUNT("删除账号..."),
    COMPLETED("删除完成"),
    ;

    val progress: Float
        get() = ordinal.toFloat() / (entries.size - 1).coerceAtLeast(1)
}

/**
 * 账号删除（对齐 iOS `AccountDeletionService`）。
 *
 * 云端走 `accountManagement` 云函数；本地清 Room + 用户文件目录。
 */
class V2AccountDeletionRepository(
    private val context: Context,
) {
    suspend fun deleteAccount(
        onStep: (V2AccountDeletionStep) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = ServiceLocator.session.current().userId ?: error("未登录")

            onStep(V2AccountDeletionStep.PREPARING)
            delay(300)

            onStep(V2AccountDeletionStep.CLOUD_DATA)
            callAccountManagement("deleteUserData", uid)

            onStep(V2AccountDeletionStep.LOCAL_CONVERSATIONS)
            ServiceLocator.db.aiChat().clearAll(uid)

            onStep(V2AccountDeletionStep.LOCAL_MEMOS)
            ServiceLocator.db.memos().clearForUser(uid)

            onStep(V2AccountDeletionStep.LOCAL_SWINGS)
            ServiceLocator.db.swingSessions().clear(uid)
            V2VideoFileStore.deleteUserDirectory(context, uid)

            onStep(V2AccountDeletionStep.DELETE_ACCOUNT)
            callAccountManagement("deleteAccount", uid)

            ServiceLocator.db.goals().clear(uid)
            ServiceLocator.db.golfProfile().delete(uid)
            ServiceLocator.db.plazaSaved().clear(uid)

            V2UserScopedCache.onLogout()
            ServiceLocator.authRepo.logout()
            ServiceLocator.creditsRepo.reset()

            onStep(V2AccountDeletionStep.COMPLETED)
        }
    }

    private suspend fun callAccountManagement(action: String, userId: String) {
        val resp = ServiceLocator.net.cloudApi.accountManagement(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive(action),
                    "userId" to JsonPrimitive(userId),
                ),
            ),
        )
        if (!resp.success) error(resp.error ?: "删除失败")
    }
}
