package com.mygolfswing.app.ui.memo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class V2MemoMediaTest {

    @Test
    fun memoVideoPlayableAcceptsRemoteUrls() {
        assertTrue(memoVideoPlayable(" https://example.com/video.mp4 "))
        assertTrue(memoVideoPlayable("http://example.com/video.mp4"))
    }

    @Test
    fun memoVideoPlayableAcceptsExistingLocalFilesOnly() {
        val root = createTempDirectory("v2-memo-media").toFile()
        val file = File(root, "video.mp4").apply { writeBytes(ByteArray(4)) }

        assertTrue(memoVideoPlayable(file.absolutePath))
        assertTrue(memoVideoPlayable("file://${file.absolutePath}"))
        assertFalse(memoVideoPlayable(File(root, "missing.mp4").absolutePath))

        root.deleteRecursively()
    }

    @Test
    fun memoVideoPlayableRejectsBlankValues() {
        assertFalse(memoVideoPlayable(null))
        assertFalse(memoVideoPlayable(" "))
    }
}
