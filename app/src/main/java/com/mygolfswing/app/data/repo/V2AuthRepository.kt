package com.mygolfswing.app.data.repo

import android.graphics.Bitmap
import android.util.Base64
import androidx.core.graphics.scale
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.friend.V2FriendSearchUser
import com.mygolfswing.app.data.friend.toFriendSearchUserOrNull
import com.mygolfswing.app.data.session.V2Session
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.temporal.ChronoUnit

sealed interface V2PhoneBindingStatus {
    data class Available(val phoneNumber: String, val bindingToken: String) : V2PhoneBindingStatus
    data class AlreadyBound(val phoneNumber: String) : V2PhoneBindingStatus
    data class Conflict(
        val phoneNumber: String,
        val mergeToken: String,
        val user: V2PhoneAccountConflict,
    ) : V2PhoneBindingStatus
}

data class V2PhoneAccountConflict(
    val userId: String,
    val maskedPhone: String?,
    val nickname: String?,
    val hasPassword: Boolean,
    val businessDataCount: Int,
)

/**
 * Auth / 用户信息仓库（对齐 iOS `TencentCloudAuthService`）。
 *
 * 所有动作都走 `POST /userAuth { action, ... }`；
 * 登录 / 注册 / 自动登录后把 [V2Session] 落盘到 [V2SessionStore]。
 */
class V2AuthRepository(
    private val net: V2NetClient,
    private val session: V2SessionStore,
    private val onLogout: suspend () -> Unit = {},
) {
    private val authRefreshMutex = Mutex()
    private val authUpgradeMutex = Mutex()

    // ============ 登录 / 注册 ============

    suspend fun loginWithUsername(username: String, password: String): Result<V2Session> = runAuthCatching {
        val data = call(
            "login",
            "username" to username,
            "password" to password,
        )
        val s = data.toSession().copy(username = username)
        session.login(s)
        s
    }

    suspend fun registerWithUsername(
        username: String,
        password: String,
        nickname: String? = null,
        avatar: String? = null,
    ): Result<V2Session> = runAuthCatching {
        val data = call(
            "register",
            "username" to username,
            "password" to password,
            "nickname" to nickname,
            "avatar" to avatar,
        )
        val s = data.toSession().copy(username = username)
        session.login(s)
        s
    }

    suspend fun sendSmsCode(phone: String): Result<Unit> = runAuthCatching {
        require(isValidAuthPhoneNumber(phone)) { "请输入正确的手机号" }
        call(
            "sendSmsCode",
            "phoneNumber" to phone,
        )
        Unit
    }

    suspend fun loginByPhone(phone: String, code: String): Result<V2Session> = runAuthCatching {
        require(isValidAuthPhoneNumber(phone)) { "请输入正确的手机号" }
        require(isValidAuthSmsCode(code)) { "请输入6位验证码" }
        val data = call(
            "loginByPhone",
            "phoneNumber" to phone,
            "code" to code,
        )
        val s = data.toSession()
        session.login(s)
        s
    }

    // ============ 密码 ============

    suspend fun updatePassword(oldPassword: String, newPassword: String): Result<Unit> = runAuthCatching {
        val uid = session.current().userId ?: error("未登录")
        call(
            "updatePassword",
            "userId" to uid,
            "oldPassword" to oldPassword,
            "newPassword" to newPassword,
        )
        session.mergeProfile { copy(hasPassword = true) }
    }

    suspend fun setPasswordWithSms(phone: String, code: String, newPassword: String): Result<Unit> = runAuthCatching {
        require(isValidAuthPhoneNumber(phone)) { "请输入正确的手机号" }
        require(isValidAuthSmsCode(code)) { "请输入6位验证码" }
        val uid = session.current().userId ?: error("未登录")
        call(
            "setPasswordWithSms",
            "userId" to uid,
            "phoneNumber" to phone,
            "code" to code,
            "newPassword" to newPassword,
        )
        session.mergeProfile { copy(hasPassword = true) }
    }

    suspend fun resetPasswordWithSms(phone: String, code: String, newPassword: String): Result<Unit> = runAuthCatching {
        require(isValidAuthPhoneNumber(phone)) { "请输入正确的手机号" }
        require(isValidAuthSmsCode(code)) { "请输入6位验证码" }
        call(
            "resetPasswordWithSms",
            "phoneNumber" to phone,
            "code" to code,
            "newPassword" to newPassword,
        )
    }

    // ============ 用户资料 ============

    suspend fun refreshUserSig(): Result<String> = runAuthCatching {
        val uid = session.current().userId ?: error("未登录")
        val data = call("getUserSig", "userId" to uid)
        val sig = data["userSig"]?.jsonPrimitive?.contentOrNull ?: error("返回无 userSig")
        session.updateUserSig(sig)
        sig
    }

    suspend fun fetchUserInfo(): Result<V2Session> = runAuthCatching {
        ensureAuthSession()
        val uid = session.current().userId ?: error("未登录")
        val data = call("getUserInfo", "userId" to uid)
        val merged = session.current().mergedWith(data)
        session.login(merged)
        merged
    }

    /** Search the same public account directory used by iOS' add-friend sheet. */
    suspend fun searchFriend(keyword: String): Result<V2FriendSearchUser?> = runAuthCatching {
        val current = session.current()
        require(current.isLoggedIn) { "请先登录后再添加好友" }
        val normalized = keyword.trim()
        require(normalized.isNotEmpty()) { "请输入用户名或手机号" }
        call(
            "searchUser",
            "keyword" to normalized.take(80),
            "currentUserId" to current.userId,
        ).toFriendSearchUserOrNull()
    }

    /** Resolve the user-id embedded by iOS and Android `huibei://user/...` QR codes. */
    suspend fun friendPublicInfo(userId: String): Result<V2FriendSearchUser> = runAuthCatching {
        val current = session.current()
        require(current.isLoggedIn) { "请先登录后再添加好友" }
        val target = userId.trim()
        require(target.isNotEmpty() && target.length <= 128) { "好友二维码无效" }
        require(target != current.userId) { "不能添加自己为好友" }
        call(
            "getPublicUserInfo",
            "targetUserId" to target,
        ).toFriendSearchUserOrNull() ?: error("用户公开资料不完整")
    }

    /** Same userAuth report action used by iOS' chat settings report sheet. */
    suspend fun reportUser(
        reportedUserId: String,
        reason: String,
        category: String,
        detail: String? = null,
    ): Result<Unit> = runAuthCatching {
        val current = session.current()
        require(current.isLoggedIn) { "请先登录后再举报" }
        val target = reportedUserId.trim()
        require(target.isNotEmpty() && target != current.userId) { "举报对象不可用" }
        call(
            "reportUser",
            "reporterId" to current.userId,
            "reportedUserId" to target,
            "reason" to reason.take(80),
            "category" to category.take(40),
            "priority" to "normal",
            "detail" to detail.orEmpty().trim().take(500),
        )
        Unit
    }

    /**
     * 增量更新 profile 字段。
     * @param patch 仅传需要更新的字段，未传字段保持后端原值；传 null 显式清空。
     */
    suspend fun updateUserInfo(
        nickname: String? = null,
        avatar: String? = null,
        gender: String? = null,
        birthday: String? = null,
        location: String? = null,
        bio: String? = null,
        handicap: String? = null,
        golfAge: Int? = null,
        golfStartYear: Int? = null,
        homeCourse: String? = null,
        dominantHand: String? = null,
        golfBrands: List<String>? = null,
        email: String? = null,
    ): Result<V2Session> = runAuthCatching {
        val uid = session.current().userId ?: error("未登录")
        val body = buildMap<String, Any?> {
            put("action", "updateUserInfo")
            put("userId", uid)
            put("nickname", nickname)
            put("avatar", avatar)
            put("gender", gender)
            put("birthday", birthday)
            put("location", location)
            put("bio", bio)
            put("handicap", handicap)
            put("golfAge", golfAge)
            put("golfStartYear", golfStartYear)
            put("homeCourse", homeCourse)
            put("dominantHand", dominantHand)
            put("golfBrands", golfBrands)
            put("email", email)
        }
        callMap(body)
        // 不依赖后端返回的 data，本地直接合并
        val updated = session.current().let { s ->
            s.copy(
                nickname = nickname ?: s.nickname,
                avatar = avatar ?: s.avatar,
                gender = gender ?: s.gender,
                birthday = birthday ?: s.birthday,
                location = location ?: s.location,
                bio = bio ?: s.bio,
                handicap = handicap ?: s.handicap,
                golfAge = golfAge ?: s.golfAge,
                golfStartYear = golfStartYear ?: s.golfStartYear,
                homeCourse = homeCourse ?: s.homeCourse,
                dominantHand = dominantHand ?: s.dominantHand,
                golfBrands = golfBrands ?: s.golfBrands,
                email = email ?: s.email,
            )
        }
        session.login(updated)
        updated
    }

    /**
     * 完善资料第二步的显式覆盖更新。
     *
     * 该步骤允许用户把性别、差点、球龄或惯用手重新设为“暂无”；普通的可选参数
     * 无法区分“未传”与“明确清空”，所以这里与 iOS 一样始终提交这组字段并同步清空本地会话。
     */
    suspend fun updateCompleteProfileOptional(
        gender: String?,
        handicap: String?,
        golfAge: Int?,
        golfStartYear: Int?,
        dominantHand: String?,
        golfBrands: List<String>,
    ): Result<V2Session> = runAuthCatching {
        val current = session.current()
        val uid = current.userId ?: error("未登录")
        callMap(
            mapOf(
                "action" to "updateUserInfo",
                "userId" to uid,
                "gender" to gender,
                "handicap" to handicap,
                "golfAge" to golfAge,
                "golfStartYear" to golfStartYear,
                "dominantHand" to dominantHand,
                "golfBrands" to golfBrands,
            ),
        )
        val updated = current.withCompleteProfileOptional(
            gender = gender,
            handicap = handicap,
            golfAge = golfAge,
            golfStartYear = golfStartYear,
            dominantHand = dominantHand,
            golfBrands = golfBrands,
        )
        session.login(updated)
        updated
    }

    /**
     * 上传头像（对齐 iOS）。
     *
     * - 压到 ≤ 50KB JPEG，先 quality=0.6，每次失败减 0.1
     * - 返回云端的 `avatarUrl`，并自动写到 session
     */
    suspend fun uploadAvatar(bitmap: Bitmap): Result<String> = runAuthCatching {
        val uid = session.current().userId ?: error("未登录")
        val resized = resizeBitmap(bitmap, maxSide = 300)
        var quality = 60
        var bytes: ByteArray
        do {
            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            bytes = baos.toByteArray()
            quality -= 10
        } while (bytes.size > 50 * 1024 && quality >= 10)
        if (bytes.size > 70 * 1024) error("图片过大无法上传，请重试")

        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val data = call(
            "uploadAvatar",
            "userId" to uid,
            "imageBase64" to base64,
            "fileExt" to "jpg",
        )
        val url = data["avatarUrl"]?.jsonPrimitive?.contentOrNull ?: error("上传后未返回 avatarUrl")
        session.mergeProfile { copy(avatar = url) }
        url
    }

    suspend fun logout() {
        runCatching {
            if (!session.current().authToken.isNullOrBlank()) {
                callMap(mapOf("action" to "revokeAuthSession"), allowSessionRecovery = false)
            }
        }
        runCatching { onLogout() }
        session.logout()
    }

    /**
     * 对齐 iOS 的统一账号会话：升级旧 UserSig 会话，并在访问令牌临近过期时
     * 使用旋转 refresh token 静默续期。失败由调用方按登录失效处理。
     */
    suspend fun ensureAuthSession() {
        val current = session.current()
        if (!current.isLoggedIn) return
        if (current.refreshToken.isNullOrBlank()) {
            authUpgradeMutex.withLock {
                val latest = session.current()
                if (!latest.refreshToken.isNullOrBlank()) return@withLock
                val action = if (latest.authToken.isNullOrBlank()) "migrateSession" else "upgradeAuthSession"
                val body = buildMap<String, Any?> {
                    put("action", action)
                    put("userId", latest.userId)
                    put("deviceId", session.deviceId())
                    if (action == "migrateSession") put("userSig", latest.userSig)
                }
                val response = net.cloudApi.userAuth(JsonObject(body.toJsonMap()))
                if (!response.success && response.error.orEmpty().contains("unknown action", ignoreCase = true)) {
                    return@withLock
                }
                if (!response.success) error(response.error ?: "登录状态已失效，请重新登录")
                persistAuthFields(response.data)
            }
            return
        }
        if (shouldRefresh(current.authTokenExpiresAt)) refreshAuthSession(force = false)
    }

    suspend fun recoverRejectedAuthSession() {
        val current = session.current()
        if (!current.refreshToken.isNullOrBlank()) {
            refreshAuthSession(force = true)
        } else {
            ensureAuthSession()
        }
    }

    suspend fun bindPhoneNumber(phone: String, code: String): Result<V2Session> = runAuthCatching {
        require(isValidAuthPhoneNumber(phone)) { "请输入正确的手机号" }
        require(isValidAuthSmsCode(code)) { "请输入6位验证码" }
        val uid = session.current().userId ?: error("未登录")
        call(
            "bindPhoneNumber",
            "userId" to uid,
            "phoneNumber" to phone,
            "code" to code,
        )
        val updated = session.current().copy(phone = phone)
        session.login(updated)
        updated
    }

    suspend fun checkPhoneBindingStatus(phone: String, code: String): Result<V2PhoneBindingStatus> = runAuthCatching {
        require(isValidAuthPhoneNumber(phone)) { "请输入正确的手机号" }
        require(code.length >= 4 && code.all(Char::isDigit)) { "请输入验证码" }
        val uid = session.current().userId ?: error("未登录")
        val data = call(
            "checkPhoneBindingStatus",
            "userId" to uid,
            "phoneNumber" to phone,
            "code" to code,
        )
        parsePhoneBindingStatus(data, phone)
    }

    suspend fun bindPhoneNumberWithToken(phone: String, bindingToken: String): Result<V2Session> = runAuthCatching {
        require(isValidAuthPhoneNumber(phone)) { "请输入正确的手机号" }
        require(bindingToken.isNotBlank()) { "绑定凭证缺失，请重新获取验证码" }
        val uid = session.current().userId ?: error("未登录")
        val data = call(
            "bindPhoneNumber",
            "userId" to uid,
            "phoneNumber" to phone,
            "bindingToken" to bindingToken,
        )
        persistBoundPhone(data, phone)
    }

    suspend fun confirmMergePhoneAccount(
        phone: String,
        conflictUserId: String,
        mergeToken: String,
    ): Result<V2Session> = runAuthCatching {
        require(isValidAuthPhoneNumber(phone)) { "请输入正确的手机号" }
        require(conflictUserId.isNotBlank() && mergeToken.isNotBlank()) { "账号合并信息缺失，请重新验证手机号" }
        val uid = session.current().userId ?: error("未登录")
        val data = call(
            "confirmMergePhoneAccount",
            "userId" to uid,
            "phoneNumber" to phone,
            "conflictUserId" to conflictUserId,
            "mergeToken" to mergeToken,
        )
        persistBoundPhone(data, phone)
    }

    private suspend fun persistBoundPhone(data: JsonObject, fallbackPhone: String): V2Session {
        val current = session.current()
        val updated = current.copy(
            phone = data.stringField("phone") ?: fallbackPhone,
            username = data.stringField("username") ?: current.username,
            nickname = data.stringField("nickname") ?: current.nickname,
            avatar = data.stringField("avatar", "avatarUrl") ?: current.avatar,
            hasPassword = data["hasPassword"]?.jsonPrimitive?.booleanOrNull ?: current.hasPassword,
        )
        session.login(updated)
        return updated
    }

    suspend fun updateNickname(nickname: String): Result<V2Session> =
        updateUserInfo(nickname = nickname)

    // ============ 内部 ============

    private suspend fun call(action: String, vararg fields: Pair<String, Any?>): JsonObject {
        val body = buildMap<String, Any?> {
            put("action", action)
            fields.forEach { (k, v) -> put(k, v) }
        }
        return callMap(body)
    }

    private suspend fun callMap(
        body: Map<String, Any?>,
        allowSessionRecovery: Boolean = true,
    ): JsonObject {
        val action = body["action"]?.toString().orEmpty()
        if (allowSessionRecovery && action !in SESSION_ISSUING_ACTIONS && action != "refreshAuthSession") {
            ensureAuthSession()
        }
        val requestBody = withAuthSessionDeviceMetadata(body, session.deviceId())
        val resp = net.cloudApi.userAuth(JsonObject(requestBody.toJsonMap()))
        if (!resp.success && allowSessionRecovery &&
            resp.code in setOf("SESSION_INVALID", "SESSION_REQUIRED") &&
            !session.current().refreshToken.isNullOrBlank()
        ) {
            refreshAuthSession(force = true)
            return callMap(body, allowSessionRecovery = false)
        }
        if (!resp.success) error(resp.error ?: "请求失败")
        persistAuthFields(resp.data)
        return resp.data ?: JsonObject(emptyMap())
    }

    private suspend fun refreshAuthSession(force: Boolean) = authRefreshMutex.withLock {
        val current = session.current()
        if (!force && !shouldRefresh(current.authTokenExpiresAt)) return@withLock
        val refreshToken = current.refreshToken ?: error("登录已过期，请重新登录")
        val response = net.cloudApi.userAuth(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive("refreshAuthSession"),
                    "refreshToken" to JsonPrimitive(refreshToken),
                    "deviceId" to JsonPrimitive(session.deviceId()),
                ),
            ),
        )
        if (!response.success) error(response.error ?: "登录已过期，请重新登录")
        persistAuthFields(response.data)
    }

    private suspend fun persistAuthFields(data: JsonObject?) {
        data ?: return
        val accessToken = data.stringField("authToken") ?: return
        val current = session.current()
        session.login(
            current.copy(
                userId = data.stringField("userId") ?: current.userId,
                authToken = accessToken,
                authTokenExpiresAt = data.stringField("authTokenExpiresAt") ?: current.authTokenExpiresAt,
                refreshToken = data.stringField("refreshToken") ?: current.refreshToken,
                refreshTokenExpiresAt = data.stringField("refreshTokenExpiresAt") ?: current.refreshTokenExpiresAt,
                coachAuthorizationStatus = data.stringField("coachAuthorizationStatus")
                    ?: current.coachAuthorizationStatus,
            ),
        )
    }

    private fun shouldRefresh(expiresAt: String?): Boolean {
        val expiry = runCatching { expiresAt?.let(Instant::parse) }.getOrNull() ?: return true
        return expiry.isBefore(Instant.now().plus(24, ChronoUnit.HOURS))
    }

    private fun Map<String, Any?>.toJsonMap(): Map<String, kotlinx.serialization.json.JsonElement> = buildMap {
        this@toJsonMap.forEach { (k, v) ->
            val element = when (v) {
                null -> return@forEach
                is String -> JsonPrimitive(v)
                is Boolean -> JsonPrimitive(v)
                is Int -> JsonPrimitive(v)
                is Long -> JsonPrimitive(v)
                is Double -> JsonPrimitive(v)
                is List<*> -> JsonArray(v.map { JsonPrimitive(it?.toString() ?: "") })
                else -> JsonPrimitive(v.toString())
            }
            put(k, element)
        }
    }

    private companion object {
        val SESSION_ISSUING_ACTIONS = setOf(
            "register", "login", "loginByPhone", "loginByAliyunFusionToken",
            "migrateSession", "upgradeAuthSession",
        )
    }
}

internal fun isValidAuthPhoneNumber(phone: String): Boolean =
    phone.length == 11 && phone[0] == '1' && phone[1] in '3'..'9' && phone.all(Char::isDigit)

internal fun isValidAuthSmsCode(code: String): Boolean =
    code.length == 6 && code.all(Char::isDigit)

internal fun parsePhoneBindingStatus(data: JsonObject, fallbackPhone: String): V2PhoneBindingStatus {
    val phone = data.stringField("phone") ?: fallbackPhone
    return when (data.stringField("bindingStatus")) {
        "available" -> V2PhoneBindingStatus.Available(
            phoneNumber = phone,
            bindingToken = data.stringField("bindingToken")
                ?: error("绑定凭证缺失，请重新获取验证码"),
        )
        "already_bound" -> V2PhoneBindingStatus.AlreadyBound(phone)
        "conflict" -> {
            val conflict = data["conflictUser"] as? JsonObject
                ?: error("账号合并信息缺失，请重新验证手机号")
            V2PhoneBindingStatus.Conflict(
                phoneNumber = phone,
                mergeToken = data.stringField("mergeToken")
                    ?: error("账号合并信息缺失，请重新验证手机号"),
                user = V2PhoneAccountConflict(
                    userId = conflict.stringField("userId")
                        ?: error("账号合并信息缺失，请重新验证手机号"),
                    maskedPhone = conflict.stringField("maskedPhone"),
                    nickname = conflict.stringField("nickname"),
                    hasPassword = conflict["hasPassword"]?.jsonPrimitive?.booleanOrNull == true,
                    businessDataCount = conflict.intField("businessDataCount") ?: 0,
                ),
            )
        }
        else -> error("未知的手机号绑定状态")
    }
}

internal fun withAuthSessionDeviceMetadata(
    body: Map<String, Any?>,
    deviceId: String,
): Map<String, Any?> = if (body["action"]?.toString() in setOf(
        "register",
        "login",
        "loginByPhone",
        "loginByAliyunFusionToken",
        "migrateSession",
        "upgradeAuthSession",
    )
) {
    body + ("deviceId" to deviceId)
} else {
    body
}

// ============ JsonObject → V2Session 工具 ============

private fun JsonObject.stringField(vararg keys: String): String? {
    for (key in keys) {
        val v = this[key]?.jsonPrimitive?.contentOrNull?.trim()
        if (!v.isNullOrEmpty()) return v
    }
    return null
}

private fun JsonObject.intField(vararg keys: String): Int? {
    for (key in keys) {
        val primitive = this[key]?.jsonPrimitive ?: continue
        val value = primitive.intOrNull ?: primitive.contentOrNull?.trim()?.toIntOrNull()
        if (value != null) return value
    }
    return null
}

private fun JsonObject.stringListField(vararg keys: String): List<String> {
    for (key in keys) {
        val value = this[key] ?: continue
        val list = when (value) {
            is JsonArray -> value.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.takeIf { text -> text.isNotBlank() } }
            else -> value.jsonPrimitive.contentOrNull
                ?.split("|", ",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()
        }
        if (list.isNotEmpty()) return list
    }
    return emptyList()
}

private fun JsonObject.toSession(): V2Session = V2Session(
    userId = stringField("userId"),
    userSig = stringField("userSig"),
    sdkAppId = this["sdkAppId"]?.jsonPrimitive?.intOrNull,
    nickname = stringField("nickname", "nickName"),
    avatar = stringField("avatar", "avatarUrl", "avatarURL"),
    email = this["email"]?.jsonPrimitive?.contentOrNull,
    phone = this["phone"]?.jsonPrimitive?.contentOrNull,
    username = this["username"]?.jsonPrimitive?.contentOrNull,
    gender = this["gender"]?.jsonPrimitive?.contentOrNull,
    birthday = this["birthday"]?.jsonPrimitive?.contentOrNull,
    location = this["location"]?.jsonPrimitive?.contentOrNull,
    bio = this["bio"]?.jsonPrimitive?.contentOrNull,
    handicap = this["handicap"]?.jsonPrimitive?.contentOrNull,
    golfAge = intField("golfAge", "golf_age"),
    homeCourse = stringField("homeCourse", "home_course"),
    dominantHand = stringField("dominantHand", "dominant_hand"),
    membershipTier = stringField("membershipTier", "memberTier", "memberLevel", "membershipLevel", "vipLevel"),
    golfStartYear = intField("golfStartYear", "golf_start_year"),
    golfBrands = stringListField("golfBrands", "golf_brands"),
    hasPassword = this["hasPassword"]?.jsonPrimitive?.booleanOrNull,
    createdAt = stringField("createdAt"),
    authToken = stringField("authToken"),
    authTokenExpiresAt = stringField("authTokenExpiresAt"),
    refreshToken = stringField("refreshToken"),
    refreshTokenExpiresAt = stringField("refreshTokenExpiresAt"),
    coachAuthorizationStatus = stringField("coachAuthorizationStatus"),
)

private fun V2Session.mergedWith(data: JsonObject): V2Session = copy(
    nickname = data.stringField("nickname", "nickName") ?: nickname,
    avatar = data.stringField("avatar", "avatarUrl", "avatarURL") ?: avatar,
    email = data["email"]?.jsonPrimitive?.contentOrNull ?: email,
    phone = data["phone"]?.jsonPrimitive?.contentOrNull ?: phone,
    username = data["username"]?.jsonPrimitive?.contentOrNull ?: username,
    gender = data["gender"]?.jsonPrimitive?.contentOrNull ?: gender,
    birthday = data["birthday"]?.jsonPrimitive?.contentOrNull ?: birthday,
    location = data["location"]?.jsonPrimitive?.contentOrNull ?: location,
    bio = data["bio"]?.jsonPrimitive?.contentOrNull ?: bio,
    handicap = data["handicap"]?.jsonPrimitive?.contentOrNull ?: handicap,
    golfAge = data.intField("golfAge", "golf_age") ?: golfAge,
    homeCourse = data.stringField("homeCourse", "home_course") ?: homeCourse,
    dominantHand = data.stringField("dominantHand", "dominant_hand") ?: dominantHand,
    membershipTier = data.stringField("membershipTier", "memberTier", "memberLevel", "membershipLevel", "vipLevel")
        ?: membershipTier,
    golfStartYear = data.intField("golfStartYear", "golf_start_year") ?: golfStartYear,
    golfBrands = data.stringListField("golfBrands", "golf_brands").takeIf { it.isNotEmpty() } ?: golfBrands,
    hasPassword = data["hasPassword"]?.jsonPrimitive?.booleanOrNull ?: hasPassword,
    createdAt = data.stringField("createdAt") ?: createdAt,
    authToken = data.stringField("authToken") ?: authToken,
    authTokenExpiresAt = data.stringField("authTokenExpiresAt") ?: authTokenExpiresAt,
    refreshToken = data.stringField("refreshToken") ?: refreshToken,
    refreshTokenExpiresAt = data.stringField("refreshTokenExpiresAt") ?: refreshTokenExpiresAt,
    coachAuthorizationStatus = data.stringField("coachAuthorizationStatus") ?: coachAuthorizationStatus,
)

internal suspend inline fun <T> runAuthCatching(
    crossinline block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}

internal fun V2Session.withCompleteProfileOptional(
    gender: String?,
    handicap: String?,
    golfAge: Int?,
    golfStartYear: Int?,
    dominantHand: String?,
    golfBrands: List<String>,
): V2Session = copy(
    gender = gender,
    handicap = handicap,
    golfAge = golfAge,
    golfStartYear = golfStartYear,
    dominantHand = dominantHand,
    golfBrands = golfBrands,
)

/** 工具：bitmap 等比缩放到最长边 ≤ [maxSide] */
internal fun resizeBitmap(src: Bitmap, maxSide: Int): Bitmap {
    val w = src.width
    val h = src.height
    val longest = maxOf(w, h)
    if (longest <= maxSide) return src
    val scale = maxSide.toFloat() / longest
    return src.scale((w * scale).toInt(), (h * scale).toInt())
}
