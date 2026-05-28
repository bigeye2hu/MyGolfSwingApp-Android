package com.mygolfswing.app.data.session

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "v2_session")

/**
 * 用 DataStore Preferences 保存登录会话（对齐 iOS `UserSessionManager`）。
 *
 * 字段集合参考 iOS [TencentCloudAuthService] 的 `CloudFunctionData`，
 * Android 端只保留我们 UI 真正用到的字段；其余字段（followers / cover 等）暂不存。
 */
class V2SessionStore(context: Context) {
    private val dataStore = context.sessionDataStore

    val state: Flow<V2Session> = dataStore.data.map { prefs -> prefs.toSession() }

    suspend fun current(): V2Session = dataStore.data.first().toSession()

    /** 登录成功后写入 session（user_id + sig 必填，其它有则覆盖） */
    suspend fun login(session: V2Session) {
        dataStore.edit { prefs ->
            prefs.writeString(K_USER_ID, session.userId)
            prefs.writeString(K_USER_SIG, session.userSig)
            session.sdkAppId?.takeIf { it != 0 }?.let { prefs[K_SDK_APP_ID] = it }
            prefs.writeString(K_NICKNAME, session.nickname)
            prefs.writeString(K_AVATAR, session.avatar)
            prefs.writeString(K_EMAIL, session.email)
            prefs.writeString(K_PHONE, session.phone)
            prefs.writeString(K_USERNAME, session.username)
            prefs.writeString(K_GENDER, session.gender)
            prefs.writeString(K_BIRTHDAY, session.birthday)
            prefs.writeString(K_LOCATION, session.location)
            prefs.writeString(K_BIO, session.bio)
            prefs.writeString(K_HANDICAP, session.handicap)
            prefs.writeString(K_HOME_COURSE, session.homeCourse)
            prefs.writeString(K_DOMINANT_HAND, session.dominantHand)
            session.golfStartYear?.let { prefs[K_GOLF_START_YEAR] = it }
            prefs.writeString(K_GOLF_BRANDS, session.golfBrands.joinToString("|"))
            session.hasPassword?.let { prefs[K_HAS_PASSWORD] = it }
            session.credits?.let { prefs[K_CREDITS] = it }
        }
    }

    /** 仅更新 profile 子集（保留 userSig / userId 等） */
    suspend fun mergeProfile(patch: V2Session.() -> V2Session) {
        login(current().patch())
    }

    suspend fun updateUserSig(userSig: String) {
        dataStore.edit { it[K_USER_SIG] = userSig }
    }

    suspend fun updateCredits(credits: Int) {
        dataStore.edit { it[K_CREDITS] = credits }
    }

    suspend fun logout() {
        dataStore.edit { it.clear() }
    }

    // ============ AI Provider ============

    val aiProviderId: Flow<String?> = dataStore.data.map { it[K_AI_PROVIDER_ID] }
    val aiApiKey: Flow<String?> = dataStore.data.map { it[K_AI_API_KEY] }
    val aiBaseUrl: Flow<String?> = dataStore.data.map { it[K_AI_BASE_URL] }
    val aiModel: Flow<String?> = dataStore.data.map { it[K_AI_MODEL] }

    suspend fun setAIProvider(id: String, baseUrl: String?, apiKey: String?, model: String?) {
        dataStore.edit { prefs ->
            prefs[K_AI_PROVIDER_ID] = id
            baseUrl?.let { prefs[K_AI_BASE_URL] = it } ?: prefs.remove(K_AI_BASE_URL)
            apiKey?.let { prefs[K_AI_API_KEY] = it } ?: prefs.remove(K_AI_API_KEY)
            model?.let { prefs[K_AI_MODEL] = it } ?: prefs.remove(K_AI_MODEL)
        }
    }

    companion object {
        private val K_USER_ID = stringPreferencesKey("userId")
        private val K_USER_SIG = stringPreferencesKey("userSig")
        private val K_SDK_APP_ID = intPreferencesKey("sdkAppId")
        private val K_NICKNAME = stringPreferencesKey("nickname")
        private val K_AVATAR = stringPreferencesKey("avatar")
        private val K_EMAIL = stringPreferencesKey("email")
        private val K_PHONE = stringPreferencesKey("phone")
        private val K_USERNAME = stringPreferencesKey("username")
        private val K_GENDER = stringPreferencesKey("gender")
        private val K_BIRTHDAY = stringPreferencesKey("birthday")
        private val K_LOCATION = stringPreferencesKey("location")
        private val K_BIO = stringPreferencesKey("bio")
        private val K_HANDICAP = stringPreferencesKey("handicap")
        private val K_HOME_COURSE = stringPreferencesKey("homeCourse")
        private val K_DOMINANT_HAND = stringPreferencesKey("dominantHand")
        private val K_GOLF_START_YEAR = intPreferencesKey("golfStartYear")
        private val K_GOLF_BRANDS = stringPreferencesKey("golfBrands")
        private val K_HAS_PASSWORD = booleanPreferencesKey("hasPassword")
        private val K_CREDITS = intPreferencesKey("credits")

        private val K_AI_PROVIDER_ID = stringPreferencesKey("ai_provider_id")
        private val K_AI_API_KEY = stringPreferencesKey("ai_api_key")
        private val K_AI_BASE_URL = stringPreferencesKey("ai_base_url")
        private val K_AI_MODEL = stringPreferencesKey("ai_model")
    }
}

data class V2Session(
    val userId: String? = null,
    val userSig: String? = null,
    val sdkAppId: Int? = null,
    val username: String? = null,
    val nickname: String? = null,
    val avatar: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    val birthday: String? = null,
    val location: String? = null,
    val bio: String? = null,
    val handicap: String? = null,
    val homeCourse: String? = null,
    val dominantHand: String? = null,
    val golfStartYear: Int? = null,
    val golfBrands: List<String> = emptyList(),
    val hasPassword: Boolean? = null,
    val credits: Int? = null,
) {
    val isLoggedIn: Boolean get() = !userId.isNullOrBlank() && !userSig.isNullOrBlank()

    companion object {
        val EMPTY = V2Session()
    }
}

private fun Preferences.toSession(): V2Session = V2Session(
    userId = readString(stringPreferencesKey("userId")),
    userSig = readString(stringPreferencesKey("userSig")),
    sdkAppId = this[intPreferencesKey("sdkAppId")]?.takeIf { it != 0 },
    username = readString(stringPreferencesKey("username")),
    nickname = readString(stringPreferencesKey("nickname")),
    avatar = readString(stringPreferencesKey("avatar")),
    email = readString(stringPreferencesKey("email")),
    phone = readString(stringPreferencesKey("phone")),
    gender = readString(stringPreferencesKey("gender")),
    birthday = readString(stringPreferencesKey("birthday")),
    location = readString(stringPreferencesKey("location")),
    bio = readString(stringPreferencesKey("bio")),
    handicap = readString(stringPreferencesKey("handicap")),
    homeCourse = readString(stringPreferencesKey("homeCourse")),
    dominantHand = readString(stringPreferencesKey("dominantHand")),
    golfStartYear = this[intPreferencesKey("golfStartYear")]?.takeIf { it > 0 },
    golfBrands = readString(stringPreferencesKey("golfBrands"))?.split("|")?.filter { it.isNotBlank() } ?: emptyList(),
    hasPassword = this[booleanPreferencesKey("hasPassword")],
    credits = this[intPreferencesKey("credits")],
)

private fun Preferences.readString(key: Preferences.Key<String>): String? =
    this[key]?.takeIf { it.isNotBlank() }

private fun androidx.datastore.preferences.core.MutablePreferences.writeString(
    key: Preferences.Key<String>,
    value: String?,
) {
    if (value.isNullOrBlank()) remove(key) else this[key] = value
}
