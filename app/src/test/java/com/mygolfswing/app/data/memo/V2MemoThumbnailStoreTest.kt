package com.mygolfswing.app.data.memo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class V2MemoThumbnailStoreTest {

    @Test
    fun onlineThumbnailGuessUsesSharedYoutubeUrlParsing() {
        val expected = "https://img.youtube.com/vi/abcdefghijk/hqdefault.jpg"

        assertEquals(expected, V2MemoThumbnailStore.onlineThumbnailGuess("https://www.youtube.com/watch?si=sharecode&v=abcdefghijk"))
        assertEquals(expected, V2MemoThumbnailStore.onlineThumbnailGuess("https://www.youtube.com/shorts/abcdefghijk?feature=share"))
        assertEquals(expected, V2MemoThumbnailStore.onlineThumbnailGuess("https://youtu.be/abcdefghijk?si=sharecode"))
    }

    @Test
    fun onlineThumbnailGuessReturnsNullForNonYoutubeLinks() {
        assertNull(V2MemoThumbnailStore.onlineThumbnailGuess("https://www.bilibili.com/video/BV1abc12345/"))
        assertNull(V2MemoThumbnailStore.onlineThumbnailGuess(null))
    }
}
