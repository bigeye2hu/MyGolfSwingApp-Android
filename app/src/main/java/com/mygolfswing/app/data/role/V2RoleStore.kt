package com.mygolfswing.app.data.role

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mygolfswing.app.data.session.V2Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

private val Context.roleDataStore by preferencesDataStore(name = "v2_experience_role")

enum class V2ExperienceRole(val title: String, val subtitle: String) {
    STUDENT("练习者版", "记录练习、整理 Memo 与连接教练"),
    COACH("教练版", "查看学员动态并完成课后跟进"),
}

enum class V2CoachAuthorizationStatus(val displayText: String) {
    NONE("未申请教练认证"),
    PENDING("教练认证审核中"),
    APPROVED("教练身份已认证"),
    SUSPENDED("教练权限已暂停"),
    REVOKED("教练权限已撤销");

    val canUseCoachExperience: Boolean get() = this == APPROVED

    companion object {
        fun fromServer(value: String?): V2CoachAuthorizationStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
    }
}

data class V2RoleState(
    val userId: String? = null,
    val activeRole: V2ExperienceRole = V2ExperienceRole.STUDENT,
    val authorizationStatus: V2CoachAuthorizationStatus = V2CoachAuthorizationStatus.NONE,
    val isCoachPreviewEnabled: Boolean = false,
    val initialized: Boolean = false,
) {
    val canUseCoachExperience: Boolean
        get() = authorizationStatus.canUseCoachExperience || isCoachPreviewEnabled
}

/**
 * 对齐 iOS `AppRoleManager`：账号权限与当前产品体验分开保存。
 * 教练权限永远以服务端 session 为准；本地只记住该账号上次选择的体验。
 */
class V2RoleStore(context: Context) {
    private val dataStore = context.roleDataStore
    private val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    private val _state = MutableStateFlow(V2RoleState())
    val state: StateFlow<V2RoleState> = _state.asStateFlow()

    suspend fun synchronize(session: V2Session) {
        val userId = session.userId
        if (!session.isLoggedIn || userId.isNullOrBlank()) {
            _state.value = V2RoleState(initialized = true)
            return
        }
        val authorization = V2CoachAuthorizationStatus.fromServer(session.coachAuthorizationStatus)
        val preferences = dataStore.data.first()
        val saved = preferences[roleKey(userId)]
            ?.let { raw -> V2ExperienceRole.entries.firstOrNull { it.name == raw } }
            ?: V2ExperienceRole.STUDENT
        val previewEnabled = isDebuggable && (preferences[previewKey(userId)] ?: false)
        val canUseCoachExperience = authorization.canUseCoachExperience || previewEnabled
        val active = if (saved == V2ExperienceRole.COACH && canUseCoachExperience) {
            V2ExperienceRole.COACH
        } else {
            V2ExperienceRole.STUDENT
        }
        _state.value = V2RoleState(
            userId = userId,
            activeRole = active,
            authorizationStatus = authorization,
            isCoachPreviewEnabled = previewEnabled,
            initialized = true,
        )
        if (saved != active) persist(userId, active)
    }

    suspend fun select(role: V2ExperienceRole): Boolean {
        val current = _state.value
        val userId = current.userId ?: return false
        if (role == V2ExperienceRole.COACH && !current.canUseCoachExperience) return false
        persist(userId, role)
        _state.value = current.copy(activeRole = role)
        return true
    }

    suspend fun prepareForFreshLoginChoice() {
        val current = _state.value
        if (!current.authorizationStatus.canUseCoachExperience || current.userId == null) return
        persist(current.userId, V2ExperienceRole.STUDENT)
        _state.value = current.copy(activeRole = V2ExperienceRole.STUDENT)
    }

    /** Debug-only counterpart of iOS `setCoachPreviewEnabled`. */
    suspend fun setCoachPreviewEnabled(enabled: Boolean): Boolean {
        if (!isDebuggable) return false
        val current = _state.value
        val userId = current.userId ?: return false
        dataStore.edit { it[previewKey(userId)] = enabled }
        val activeRole = if (
            !enabled &&
            !current.authorizationStatus.canUseCoachExperience &&
            current.activeRole == V2ExperienceRole.COACH
        ) {
            V2ExperienceRole.STUDENT
        } else {
            current.activeRole
        }
        if (activeRole != current.activeRole) persist(userId, activeRole)
        _state.value = current.copy(
            activeRole = activeRole,
            isCoachPreviewEnabled = enabled,
        )
        return true
    }

    private suspend fun persist(userId: String, role: V2ExperienceRole) {
        dataStore.edit { it[roleKey(userId)] = role.name }
    }

    private fun roleKey(userId: String) = stringPreferencesKey("active_role_$userId")

    private fun previewKey(userId: String) = booleanPreferencesKey("coach_experience_preview_$userId")
}
