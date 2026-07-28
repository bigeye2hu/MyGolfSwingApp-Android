package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.ServiceLocator
import com.mygolfswing.app.data.store.V2UserScopedCache
import kotlinx.coroutines.CancellationException
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
 * 云端走 `accountManagement` 云函数；本地清 Room 记录，已下载到设备的视频文件保留。
 */
class V2AccountDeletionRepository {
    suspend fun deleteAccount(
        onStep: (V2AccountDeletionStep) -> Unit = {},
    ): Result<Unit> = try {
        val uid = withContext(Dispatchers.IO) {
            ServiceLocator.session.current().userId
        } ?: error("未登录")

        onStep(V2AccountDeletionStep.PREPARING)
        delay(300)

        onStep(V2AccountDeletionStep.CLOUD_DATA)
        withContext(Dispatchers.IO) {
            callAccountManagement("deleteUserData", uid)
        }

        onStep(V2AccountDeletionStep.LOCAL_CONVERSATIONS)
        withContext(Dispatchers.IO) {
            ServiceLocator.db.aiChat().clearAll(uid)
        }

        onStep(V2AccountDeletionStep.LOCAL_MEMOS)
        withContext(Dispatchers.IO) {
            ServiceLocator.db.memos().clearForUser(uid)
        }

        onStep(V2AccountDeletionStep.LOCAL_SWINGS)
        withContext(Dispatchers.IO) {
            ServiceLocator.db.swingSessions().clear(uid)
        }

        onStep(V2AccountDeletionStep.DELETE_ACCOUNT)
        withContext(Dispatchers.IO) {
            callAccountManagement("deleteAccount", uid)
        }

        withContext(Dispatchers.IO) {
            ServiceLocator.db.goals().clear(uid)
            ServiceLocator.db.golfProfile().delete(uid)
            ServiceLocator.db.plazaSaved().clear(uid)

            V2UserScopedCache.onLogout()
            ServiceLocator.authRepo.logout()
            ServiceLocator.creditsRepo.reset()
        }

        onStep(V2AccountDeletionStep.COMPLETED)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
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
