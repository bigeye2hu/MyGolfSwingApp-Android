package com.mygolfswing.app.data.coach

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal fun parseCoachInviteLink(rawUrl: String?): V2PendingCoachInvite? {
    val uri = runCatching { URI(rawUrl?.trim().orEmpty()) }.getOrNull() ?: return null
    val query = parseParameters(uri.rawQuery)
    val fragment = parseParameters(uri.rawFragment)
    val inviteId = when {
        uri.scheme.equals("https", true) && uri.host.equals("www.mygolfai.com.cn", true) -> {
            val parts = uri.path.orEmpty().split('/').filter(String::isNotBlank)
            parts.takeIf { it.size == 2 && it[0] == "invite" }?.get(1)
        }
        uri.scheme.equals("huibei", true) && uri.host.equals("invite", true) -> query["inviteId"]
        else -> null
    }?.takeIf(String::isNotBlank) ?: return null
    val token = (fragment["token"] ?: query["token"])?.takeIf(String::isNotBlank) ?: return null
    return V2PendingCoachInvite(
        inviteId = decode(inviteId),
        inviteToken = token,
        inviteCode = fragment["code"] ?: query["code"],
    )
}

private fun parseParameters(raw: String?): Map<String, String> = raw.orEmpty()
    .split('&')
    .mapNotNull { part ->
        val index = part.indexOf('=')
        if (index <= 0) null else decode(part.substring(0, index)) to decode(part.substring(index + 1))
    }
    .toMap()

private fun decode(value: String): String = runCatching {
    URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}.getOrDefault(value)
