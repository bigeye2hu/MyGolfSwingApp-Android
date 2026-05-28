package com.mygolfswing.app.data.repo

import android.graphics.Bitmap
import android.util.Base64
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2Session
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream

/**
 * Auth / 用户信息仓库（对齐 iOS `TencentCloudAuthService`）。
 *
 * 所有动作都走 `POST /userAuth { action, ... }`；
 * 登录 / 注册 / 自动登录后把 [V2Session] 落盘到 [V2SessionStore]。
 */
class V2AuthRepository(
    private val net: V2NetClient,
    private val session: V2SessionStore,
) {

    // ============ 登录 / 注册 ============

    suspend fun loginWithUsername(username: String, password: String): Result<V2Session> = runCatching {
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
    ): Result<V2Session> = runCatching {
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

    suspend fun sendSmsCode(phone: String, purpose: SmsPurpose = SmsPurpose.LOGIN): Result<Unit> = runCatching {
        call(
            "sendSmsCode",
            "phoneNumber" to phone,
            "purpose" to purpose.code,
        )
        Unit
    }

    suspend fun loginByPhone(phone: String, code: String): Result<V2Session> = runCatching {
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

    suspend fun updatePassword(oldPassword: String, newPassword: String): Result<Unit> = runCatching {
        val uid = session.current().userId ?: error("未登录")
        call(
            "updatePassword",
            "userId" to uid,
            "oldPassword" to oldPassword,
            "newPassword" to newPassword,
        )
        session.mergeProfile { copy(hasPassword = true) }
    }

    suspend fun setPasswordWithSms(phone: String, code: String, newPassword: String): Result<Unit> = runCatching {
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

    suspend fun resetPasswordWithSms(phone: String, code: String, newPassword: String): Result<Unit> = runCatching {
        call(
            "resetPasswordWithSms",
            "phoneNumber" to phone,
            "code" to code,
            "newPassword" to newPassword,
        )
    }

    // ============ 用户资料 ============

    suspend fun refreshUserSig(): Result<String> = runCatching {
        val uid = session.current().userId ?: error("未登录")
        val data = call("getUserSig", "userId" to uid)
        val sig = data["userSig"]?.jsonPrimitive?.contentOrNull ?: error("返回无 userSig")
        session.updateUserSig(sig)
        sig
    }

    suspend fun fetchUserInfo(): Result<V2Session> = runCatching {
        val uid = session.current().userId ?: error("未登录")
        val data = call("getUserInfo", "userId" to uid)
        val merged = session.current().mergedWith(data)
        session.login(merged)
        merged
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
        golfStartYear: Int? = null,
        homeCourse: String? = null,
        dominantHand: String? = null,
        golfBrands: List<String>? = null,
        email: String? = null,
    ): Result<V2Session> = runCatching {
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
     * 上传头像（对齐 iOS）。
     *
     * - 压到 ≤ 50KB JPEG，先 quality=0.6，每次失败减 0.1
     * - 返回云端的 `avatarUrl`，并自动写到 session
     */
    suspend fun uploadAvatar(bitmap: Bitmap): Result<String> = runCatching {
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
        session.logout()
    }

    // ============ 内部 ============

    private suspend fun call(action: String, vararg fields: Pair<String, Any?>): JsonObject {
        val body = buildMap<String, Any?> {
            put("action", action)
            fields.forEach { (k, v) -> put(k, v) }
        }
        return callMap(body)
    }

    private suspend fun callMap(body: Map<String, Any?>): JsonObject {
        val resp = net.cloudApi.userAuth(JsonObject(body.toJsonMap()))
        if (!resp.success) error(resp.error ?: "请求失败")
        return resp.data ?: JsonObject(emptyMap())
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

    enum class SmsPurpose(val code: String) {
        LOGIN("login"),
        RESET_PASSWORD("reset_password"),
        SET_PASSWORD("set_password"),
    }
}

// ============ JsonObject → V2Session 工具 ============

private fun JsonObject.toSession(): V2Session = V2Session(
    userId = this["userId"]?.jsonPrimitive?.contentOrNull,
    userSig = this["userSig"]?.jsonPrimitive?.contentOrNull,
    sdkAppId = this["sdkAppId"]?.jsonPrimitive?.intOrNull,
    nickname = this["nickname"]?.jsonPrimitive?.contentOrNull,
    avatar = this["avatar"]?.jsonPrimitive?.contentOrNull,
    email = this["email"]?.jsonPrimitive?.contentOrNull,
    phone = this["phone"]?.jsonPrimitive?.contentOrNull,
    username = this["username"]?.jsonPrimitive?.contentOrNull,
    gender = this["gender"]?.jsonPrimitive?.contentOrNull,
    birthday = this["birthday"]?.jsonPrimitive?.contentOrNull,
    location = this["location"]?.jsonPrimitive?.contentOrNull,
    bio = this["bio"]?.jsonPrimitive?.contentOrNull,
    handicap = this["handicap"]?.jsonPrimitive?.contentOrNull,
    homeCourse = this["homeCourse"]?.jsonPrimitive?.contentOrNull,
    dominantHand = this["dominantHand"]?.jsonPrimitive?.contentOrNull,
    golfStartYear = this["golfStartYear"]?.jsonPrimitive?.intOrNull,
    golfBrands = this["golfBrands"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
    hasPassword = this["hasPassword"]?.jsonPrimitive?.booleanOrNull,
)

private fun V2Session.mergedWith(data: JsonObject): V2Session = copy(
    nickname = data["nickname"]?.jsonPrimitive?.contentOrNull ?: nickname,
    avatar = data["avatar"]?.jsonPrimitive?.contentOrNull ?: avatar,
    email = data["email"]?.jsonPrimitive?.contentOrNull ?: email,
    phone = data["phone"]?.jsonPrimitive?.contentOrNull ?: phone,
    username = data["username"]?.jsonPrimitive?.contentOrNull ?: username,
    gender = data["gender"]?.jsonPrimitive?.contentOrNull ?: gender,
    birthday = data["birthday"]?.jsonPrimitive?.contentOrNull ?: birthday,
    location = data["location"]?.jsonPrimitive?.contentOrNull ?: location,
    bio = data["bio"]?.jsonPrimitive?.contentOrNull ?: bio,
    handicap = data["handicap"]?.jsonPrimitive?.contentOrNull ?: handicap,
    homeCourse = data["homeCourse"]?.jsonPrimitive?.contentOrNull ?: homeCourse,
    dominantHand = data["dominantHand"]?.jsonPrimitive?.contentOrNull ?: dominantHand,
    golfStartYear = data["golfStartYear"]?.jsonPrimitive?.intOrNull ?: golfStartYear,
    golfBrands = data["golfBrands"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: golfBrands,
    hasPassword = data["hasPassword"]?.jsonPrimitive?.booleanOrNull ?: hasPassword,
)

/** 工具：bitmap 等比缩放到最长边 ≤ [maxSide] */
internal fun resizeBitmap(src: Bitmap, maxSide: Int): Bitmap {
    val w = src.width
    val h = src.height
    val longest = maxOf(w, h)
    if (longest <= maxSide) return src
    val scale = maxSide.toFloat() / longest
    return Bitmap.createScaledBitmap(src, (w * scale).toInt(), (h * scale).toInt(), true)
}
