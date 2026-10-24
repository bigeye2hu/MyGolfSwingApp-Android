package com.mygolfswing.app.data.friend

import com.mygolfswing.app.data.coach.V2CoachConnection
import com.mygolfswing.app.data.repo.V2AuthRepository
import com.mygolfswing.app.data.repo.V2CoachWorkspaceRepository
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.CancellationException

/** 对齐 iOS：从腾讯 IM 读取真实好友，再由工作区后端校验并创建好友空间。 */
class V2FriendSpaceSyncRepository(
    private val session: V2SessionStore,
    private val auth: V2AuthRepository,
    private val friends: V2TencentFriendRepository,
    private val workspace: V2CoachWorkspaceRepository,
) {
    suspend fun syncOrLoadConnections(): Result<List<V2CoachConnection>> {
        val synchronized = try {
            auth.ensureAuthSession()
            friends.friendUserIds(
                session = session.current(),
                renewExpiredSession = {
                    auth.refreshUserSig().getOrThrow()
                    session.current()
                },
            ).mapCatching { friendUserIds ->
                workspace.syncFriendSpaces(friendUserIds).getOrThrow()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
        if (synchronized.isSuccess) return synchronized

        // 与 iOS 一致：好友系统不可用时，仍展示已存在的好友/教练空间。
        return workspace.studentConnections()
    }
}
