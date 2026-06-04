package com.mygolfswing.app.data.video

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/** 在线视频元数据（对齐 iOS `OnlineVideoThumbnailFetcher`） */
data class V2OnlineVideoMetadata(
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val durationSec: Double? = null,
)

object V2OnlineVideoMetadataFetcher {

    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(url: String, platform: String): V2OnlineVideoMetadata = withContext(Dispatchers.IO) {
        when (platform) {
            "哔哩哔哩" -> fetchBilibili(url)
            "YouTube" -> fetchYouTube(url)
            else -> fetchOpenGraph(url)
        }
    }

    fun cleanVideoUrl(input: String): String {
        val trimmed = input.trim()
        val pattern = Pattern.compile("(https?://[^\\s\\u4e00-\\u9fa5]+)")
        val m = pattern.matcher(trimmed)
        val extracted = if (m.find()) m.group(1) ?: trimmed else trimmed
        return extracted.trimEnd(
            ')',
            '）',
            ']',
            '】',
            '.',
            ',',
            '，',
            '。',
            '!',
            '！',
            '?',
            '？',
            ';',
            '；',
        )
    }

    fun detectPlatform(url: String): String = when {
        url.contains("bilibili.com", ignoreCase = true) || url.contains("b23.tv", ignoreCase = true) -> "哔哩哔哩"
        url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true) -> "YouTube"
        url.contains("xiaohongshu.com", ignoreCase = true) || url.contains("xhslink.com", ignoreCase = true) -> "小红书"
        url.contains("douyin.com", ignoreCase = true) || url.contains("iesdouyin.com", ignoreCase = true) -> "抖音"
        else -> "其他"
    }

    private fun fetchBilibili(rawUrl: String): V2OnlineVideoMetadata {
        var finalUrl = rawUrl
        if (rawUrl.contains("b23.tv")) {
            resolveRedirect(rawUrl)?.let { finalUrl = it }
        }
        val bvid = extractBvid(finalUrl) ?: return V2OnlineVideoMetadata()
        val api = "https://api.bilibili.com/x/web-interface/view?bvid=$bvid"
        val body = getString(api) ?: return V2OnlineVideoMetadata()
        return runCatching {
            val data = json.parseToJsonElement(body).jsonObject["data"]?.jsonObject ?: return V2OnlineVideoMetadata()
            val pic = data["pic"]?.jsonPrimitive?.content?.replace("http://", "https://")
            val title = data["title"]?.jsonPrimitive?.content
            val duration = data["duration"]?.jsonPrimitive?.content?.toDoubleOrNull()
            V2OnlineVideoMetadata(title = title, thumbnailUrl = pic, durationSec = duration)
        }.getOrDefault(V2OnlineVideoMetadata())
    }

    private fun fetchYouTube(url: String): V2OnlineVideoMetadata {
        val id = extractYouTubeId(url) ?: return V2OnlineVideoMetadata()
        val thumb = "https://img.youtube.com/vi/$id/hqdefault.jpg"
        val encoded = java.net.URLEncoder.encode(url, Charsets.UTF_8.name())
        val oembed = getString("https://www.youtube.com/oembed?url=$encoded&format=json")
        val title = oembed?.let {
            runCatching { json.parseToJsonElement(it).jsonObject["title"]?.jsonPrimitive?.content }.getOrNull()
        }
        return V2OnlineVideoMetadata(title = title, thumbnailUrl = thumb)
    }

    private fun fetchOpenGraph(url: String): V2OnlineVideoMetadata {
        val html = getString(url, userAgent = true) ?: return V2OnlineVideoMetadata()
        val title = metaContent(html, "og:title") ?: metaContent(html, "twitter:title")
        val image = metaContent(html, "og:image") ?: metaContent(html, "twitter:image")
        return V2OnlineVideoMetadata(title = title, thumbnailUrl = image)
    }

    private fun extractBvid(url: String): String? {
        val m = Pattern.compile("(BV[a-zA-Z0-9]+)").matcher(url)
        return if (m.find()) m.group(1) else null
    }

    private fun extractYouTubeId(url: String): String? {
        val patterns = listOf(
            "(?:youtube\\.com/watch\\?v=)([\\w-]{11})",
            "(?:youtu\\.be/)([\\w-]{11})",
        )
        for (p in patterns) {
            val m = Pattern.compile(p).matcher(url)
            if (m.find()) return m.group(1)
        }
        return null
    }

    private fun resolveRedirect(url: String): String? {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", mobileUa())
            .get()
            .build()
        return runCatching {
            http.newCall(req).execute().use { it.request.url.toString() }
        }.getOrNull()
    }

    private fun getString(url: String, userAgent: Boolean = false): String? {
        val builder = Request.Builder().url(url).get()
        if (userAgent) builder.header("User-Agent", mobileUa())
        return runCatching {
            http.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string()
            }
        }.getOrNull()
    }

    private fun metaContent(html: String, property: String): String? {
        val p = Pattern.compile(
            "<meta[^>]+property=[\"']$property[\"'][^>]+content=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE,
        )
        val m = p.matcher(html)
        if (m.find()) return m.group(1)
        val p2 = Pattern.compile(
            "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+property=[\"']$property[\"']",
            Pattern.CASE_INSENSITIVE,
        )
        val m2 = p2.matcher(html)
        return if (m2.find()) m2.group(1) else null
    }

    private fun mobileUa() =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
}
