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
            prefs.writeOptionalInt(K_SDK_APP_ID, session.sdkAppId?.takeIf { it != 0 })
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
            prefs.writeString(K_MEMBERSHIP_TIER, session.membershipTier)
            prefs.writeOptionalInt(K_GOLF_AGE, session.golfAge)
            prefs.writeOptionalInt(K_GOLF_START_YEAR, session.golfStartYear)
            prefs.writeString(K_GOLF_BRANDS, session.golfBrands.joinToString("|"))
            prefs.writeOptionalBoolean(K_HAS_PASSWORD, session.hasPassword)
            prefs.writeOptionalInt(K_CREDITS, session.credits)
            prefs.writeString(K_CREATED_AT, session.createdAt)
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

    suspend fun pendingDeletes(bucket: String, userId: String): Set<String> {
        val key = pendingDeleteKey(bucket, userId)
        return dataStore.data.first()[key]
            ?.split("|")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    }

    suspend fun addPendingDelete(bucket: String, userId: String, id: String) {
        val normalized = id.lowercase()
        val key = pendingDeleteKey(bucket, userId)
        dataStore.edit { prefs ->
            val next = (prefs[key]
                ?.split("|")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet()) + normalized
            prefs[key] = next.joinToString("|")
        }
    }

    suspend fun removePendingDelete(bucket: String, userId: String, id: String) {
        val normalized = id.lowercase()
        val key = pendingDeleteKey(bucket, userId)
        dataStore.edit { prefs ->
            val next = (prefs[key]
                ?.split("|")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet()) - normalized
            if (next.isEmpty()) {
                prefs.remove(key)
            } else {
                prefs[key] = next.joinToString("|")
            }
        }
    }

    suspend fun logout() {
        dataStore.edit { it.clear() }
    }

    // ============ AI Provider ============

    val aiProviderId: Flow<String?> = dataStore.data.map { prefs ->
        prefs.readUserScopedString(K_AI_PROVIDER_ID.name) ?: prefs[K_AI_PROVIDER_ID]
    }
    val aiApiKey: Flow<String?> = dataStore.data.map { prefs ->
        prefs.readUserScopedString(K_AI_API_KEY.name) ?: prefs[K_AI_API_KEY]
    }
    val aiBaseUrl: Flow<String?> = dataStore.data.map { prefs ->
        prefs.readUserScopedString(K_AI_BASE_URL.name) ?: prefs[K_AI_BASE_URL]
    }
    val aiModel: Flow<String?> = dataStore.data.map { prefs ->
        prefs.readUserScopedString(K_AI_MODEL.name) ?: prefs[K_AI_MODEL]
    }

    suspend fun setAIProvider(
        id: String,
        baseUrl: String?,
        apiKey: String?,
        model: String?,
        verified: Boolean = true,
    ) {
        dataStore.edit { prefs ->
            prefs.writeAIString(K_AI_PROVIDER_ID, id)
            prefs.writeAIString(K_AI_BASE_URL, baseUrl)
            prefs.writeAIString(K_AI_MODEL, model)

            if (id == "builtin") {
                prefs.writeAIString(K_AI_API_KEY, null)
                prefs.writeProviderVerified(id, true)
            } else {
                prefs.writeAIString(K_AI_API_KEY, apiKey)
                apiKey?.takeIf { it.isNotBlank() }?.let { prefs.writeProviderApiKey(id, it) }
                prefs.writeProviderVerified(id, verified && !apiKey.isNullOrBlank())
            }

            model?.takeIf { it.isNotBlank() }?.let { prefs.writeProviderModel(id, it) }
        }
    }

    suspend fun storedAIKey(providerId: String): String? =
        dataStore.data.first().readProviderApiKey(providerId)

    suspend fun storedAIModel(providerId: String): String? =
        dataStore.data.first().readProviderModel(providerId)

    suspend fun isAIProviderVerified(providerId: String): Boolean {
        if (providerId == "builtin") return true
        val prefs = dataStore.data.first()
        return !prefs.readProviderApiKey(providerId).isNullOrBlank() && prefs.readProviderVerified(providerId)
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
        private val K_MEMBERSHIP_TIER = stringPreferencesKey("membershipTier")
        private val K_GOLF_AGE = intPreferencesKey("golfAge")
        private val K_GOLF_START_YEAR = intPreferencesKey("golfStartYear")
        private val K_GOLF_BRANDS = stringPreferencesKey("golfBrands")
        private val K_HAS_PASSWORD = booleanPreferencesKey("hasPassword")
        private val K_CREDITS = intPreferencesKey("credits")
        private val K_CREATED_AT = stringPreferencesKey("createdAt")

        private val K_AI_PROVIDER_ID = stringPreferencesKey("ai_provider_id")
        private val K_AI_API_KEY = stringPreferencesKey("ai_api_key")
        private val K_AI_BASE_URL = stringPreferencesKey("ai_base_url")
        private val K_AI_MODEL = stringPreferencesKey("ai_model")

        fun pendingDeleteKey(bucket: String, userId: String): Preferences.Key<String> =
            stringPreferencesKey("pending_delete_${bucket}_$userId")
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
    val membershipTier: String? = null,
    val golfAge: Int? = null,
    val golfStartYear: Int? = null,
    val golfBrands: List<String> = emptyList(),
    val hasPassword: Boolean? = null,
    val credits: Int? = null,
    val createdAt: String? = null,
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
    membershipTier = readString(stringPreferencesKey("membershipTier")),
    golfAge = this[intPreferencesKey("golfAge")]?.takeIf { it > 0 },
    golfStartYear = this[intPreferencesKey("golfStartYear")]?.takeIf { it > 0 },
    golfBrands = readString(stringPreferencesKey("golfBrands"))?.split("|")?.filter { it.isNotBlank() } ?: emptyList(),
    hasPassword = this[booleanPreferencesKey("hasPassword")],
    credits = this[intPreferencesKey("credits")],
    createdAt = readString(stringPreferencesKey("createdAt")),
)

private fun Preferences.readString(key: Preferences.Key<String>): String? =
    this[key]?.takeIf { it.isNotBlank() }

private fun Preferences.readUserScopedString(baseKey: String): String? {
    val userId = readString(stringPreferencesKey("userId")) ?: return null
    return readString(stringPreferencesKey("${baseKey}_$userId"))
}

private fun Preferences.providerApiKeyKey(providerId: String): Preferences.Key<String>? {
    val userId = readString(stringPreferencesKey("userId")) ?: return null
    return stringPreferencesKey("ai_apikey_${userId}_$providerId")
}

private fun Preferences.providerModelKey(providerId: String): Preferences.Key<String>? {
    val userId = readString(stringPreferencesKey("userId")) ?: return null
    return stringPreferencesKey("ai_model_${userId}_$providerId")
}

private fun Preferences.providerVerifiedKey(providerId: String): Preferences.Key<Boolean>? {
    val userId = readString(stringPreferencesKey("userId")) ?: return null
    return booleanPreferencesKey("ai_verified_${userId}_$providerId")
}

private fun Preferences.readProviderApiKey(providerId: String): String? =
    providerApiKeyKey(providerId)?.let { readString(it) }

private fun Preferences.readProviderModel(providerId: String): String? =
    providerModelKey(providerId)?.let { readString(it) }

private fun Preferences.readProviderVerified(providerId: String): Boolean =
    providerVerifiedKey(providerId)?.let { this[it] } == true

private fun androidx.datastore.preferences.core.MutablePreferences.writeString(
    key: Preferences.Key<String>,
    value: String?,
) {
    if (value.isNullOrBlank()) remove(key) else this[key] = value
}

private fun androidx.datastore.preferences.core.MutablePreferences.writeOptionalInt(
    key: Preferences.Key<Int>,
    value: Int?,
) {
    if (value == null) remove(key) else this[key] = value
}

private fun androidx.datastore.preferences.core.MutablePreferences.writeOptionalBoolean(
    key: Preferences.Key<Boolean>,
    value: Boolean?,
) {
    if (value == null) remove(key) else this[key] = value
}

private fun androidx.datastore.preferences.core.MutablePreferences.writeAIString(
    key: Preferences.Key<String>,
    value: String?,
) {
    writeString(key, value)
    val userId = readString(stringPreferencesKey("userId")) ?: return
    writeString(stringPreferencesKey("${key.name}_$userId"), value)
}

private fun androidx.datastore.preferences.core.MutablePreferences.writeProviderApiKey(
    providerId: String,
    value: String,
) {
    providerApiKeyKey(providerId)?.let { writeString(it, value) }
}

private fun androidx.datastore.preferences.core.MutablePreferences.writeProviderModel(
    providerId: String,
    value: String,
) {
    providerModelKey(providerId)?.let { writeString(it, value) }
}

private fun androidx.datastore.preferences.core.MutablePreferences.writeProviderVerified(
    providerId: String,
    value: Boolean,
) {
    providerVerifiedKey(providerId)?.let {
        if (value) this[it] = true else remove(it)
    }
}
