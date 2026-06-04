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
}
