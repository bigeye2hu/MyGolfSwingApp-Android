package com.mygolfswing.app.data.friend

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class V2FriendSearchUser(
    val userId: String,
    val username: String,
    val nickname: String?,
    val avatar: String?,
) {
    val displayName: String get() = nickname?.takeIf(String::isNotBlank) ?: username
}

data class V2IncomingFriendRequest(
    val userId: String,
    val nickname: String?,
    val avatar: String?,
    val wording: String?,
    val addTimeSeconds: Long,
) {
    val displayName: String get() = nickname?.takeIf(String::isNotBlank) ?: userId
}

internal fun JsonObject.toFriendSearchUserOrNull(): V2FriendSearchUser? {
    fun value(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
        this[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)
    }
    val userId = value("userId", "userID", "_id") ?: return null
    return V2FriendSearchUser(
        userId = userId,
        username = value("username") ?: userId,
        nickname = value("nickname", "nickName"),
        avatar = value("avatar", "avatarUrl", "avatarURL", "faceUrl"),
    )
}
