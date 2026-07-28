package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * V2 用户档案 Store（对齐 iOS `V2ProfileViewModel`）。
 *
 * - `nickname / username / avatar / isLoggedIn / credits` 等字段直接绑定 [V2SessionStore]
 * - 高尔夫子卡片（差点 / 球龄 / 惯用手 / 球杆品牌）也从 session 派生，
 *   未登录或未填写则展示「未设置」
 * - 拉取最新积分 / 用户信息由 [refresh] 触发，UI 通过 [isRefreshing] 控制 loading
 */
data class V2UserProfile(
    val userId: String? = null,
    val nickname: String = "未登录",
    val username: String = "@guest",
    val avatar: String? = null,
    val membershipTier: String = "普通会员",
    val credits: Int = 0,
    val handicap: String = "未设置",
    val golfYears: String = "未设置",
    val dominantHand: String = "未设置",
    val clubBrand: String = "未设置",
    val gender: String = "保密",
    val birthday: String? = null,
    val location: String? = null,
    val bio: String? = null,
    val homeCourse: String = "未设置",
    val email: String? = null,
    val phone: String? = null,
    val golfBrands: List<String> = emptyList(),
    val hasPassword: Boolean = false,
    val createdAt: String? = null,
    val isLoggedIn: Boolean = false,
)

object V2ProfileStore {

    private val session get() = ServiceLocator.session
    private val scope get() = ServiceLocator.appScope

    private val _profile = MutableStateFlow(V2UserProfile())
    val profile: StateFlow<V2UserProfile> = _profile.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    @Volatile private var collecting = false

    fun startObserving() {
        if (collecting) return
        collecting = true
        scope.launch {
            session.state.onEach { s ->
                _profile.value = V2UserProfile(
                    userId = s.userId,
                    nickname = s.nickname ?: s.username ?: if (s.isLoggedIn) "高尔夫球友" else "未登录",
                    username = buildUsernameDisplay(s.username, s.phone, s.userId),
                    avatar = s.avatar,
                    membershipTier = s.membershipTier?.takeIf { it.isNotBlank() }
                        ?: if (s.isLoggedIn) "普通会员" else "游客",
                    credits = s.credits ?: 0,
                    handicap = formatHandicap(s.handicap),
                    golfYears = formatGolfYears(s.golfStartYear, s.golfAge),
                    dominantHand = s.dominantHand?.takeIf { it.isNotBlank() } ?: "未设置",
                    clubBrand = s.golfBrands.firstOrNull() ?: "未设置",
                    gender = s.gender?.takeIf { it.isNotBlank() } ?: "保密",
                    birthday = s.birthday,
                    location = s.location,
                    bio = s.bio,
                    homeCourse = s.homeCourse?.takeIf { it.isNotBlank() } ?: "未设置",
                    email = s.email,
                    phone = s.phone,
                    golfBrands = s.golfBrands,
                    hasPassword = s.hasPassword ?: false,
                    createdAt = formatCreatedAt(s.createdAt),
                    isLoggedIn = s.isLoggedIn,
                )
            }.collect()
        }
    }

    fun update(transform: (V2UserProfile) -> V2UserProfile) {
        _profile.value = transform(_profile.value)
    }

    /** 拉云端最新用户信息 + 积分（5min 缓存内不重复请求） */
    fun refresh(force: Boolean = false) {
        scope.launch {
            if (!session.current().isLoggedIn) return@launch
            _isRefreshing.value = true
            try {
                ServiceLocator.authRepo.fetchUserInfo()
                ServiceLocator.creditsRepo.fetchBalance(force = force)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun logout() {
        scope.launch { logoutAndWait() }
    }

    suspend fun logoutAndWait() {
        V2UserScopedCache.onLogout()
        ServiceLocator.authRepo.logout()
    }

    fun resetUserScopedCaches() {
        V2UserScopedCache.clearActiveUserMemory()
    }

    // ============ 派生展示 ============

    private fun buildUsernameDisplay(username: String?, phone: String?, userId: String?): String {
        val raw = username ?: phone ?: userId?.take(8) ?: "guest"
        return "@$raw"
    }

    private fun formatHandicap(handicap: String?): String {
        val value = handicap?.takeIf { it.isNotBlank() }?.toDoubleOrNull() ?: return "—"
        return "%.1f".format(value)
    }

    private fun formatGolfYears(startYear: Int?, golfAge: Int?): String {
        if (startYear == null || startYear < 1900) {
            return golfAge?.takeIf { it > 0 }?.let { "$it 年" } ?: "—"
        }
        val years = LocalDate.now().year - startYear
        if (years <= 0) return "—"
        return "$years 年"
    }

    private fun formatCreatedAt(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val instant = java.time.Instant.parse(raw)
            val date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            DateTimeFormatter.ofPattern("yyyy年M月d日").format(date)
        }.getOrElse {
            runCatching {
                val date = LocalDate.parse(raw.take(10))
                DateTimeFormatter.ofPattern("yyyy年M月d日").format(date)
            }.getOrNull() ?: raw
        }
    }

    @Suppress("unused")
    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
}
