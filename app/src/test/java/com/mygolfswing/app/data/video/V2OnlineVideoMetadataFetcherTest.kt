package com.mygolfswing.app.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class V2OnlineVideoMetadataFetcherTest {

    @Test
    fun cleanVideoUrlExtractsUrlFromShareText() {
        val shareText = "这个动作不错 https://www.bilibili.com/video/BV1abc12345/ 复制后打开"

        val cleaned = V2OnlineVideoMetadataFetcher.cleanVideoUrl(shareText)

        assertEquals("https://www.bilibili.com/video/BV1abc12345/", cleaned)
        assertEquals("哔哩哔哩", V2OnlineVideoMetadataFetcher.detectPlatform(cleaned))
    }

    @Test
    fun cleanVideoUrlTrimsTrailingPunctuation() {
        val cleaned = V2OnlineVideoMetadataFetcher.cleanVideoUrl("https://youtu.be/abcdefghijk。")

        assertEquals("https://youtu.be/abcdefghijk", cleaned)
        assertEquals("YouTube", V2OnlineVideoMetadataFetcher.detectPlatform(cleaned))
    }

    @Test
    fun cleanVideoUrlUpgradesHttpToHttps() {
        val cleaned = V2OnlineVideoMetadataFetcher.cleanVideoUrl("看这个 http://www.xiaohongshu.com/explore/abc123，")

        assertEquals("https://www.xiaohongshu.com/explore/abc123", cleaned)
        assertEquals("小红书", V2OnlineVideoMetadataFetcher.detectPlatform(cleaned))
    }

    @Test
    fun normalizePlayableUrlUpgradesLegacyHttp() {
        val normalized = V2OnlineVideoMetadataFetcher.normalizePlayableUrl("http://example.com/video.mp4")

        assertEquals("https://example.com/video.mp4", normalized)
    }
}
