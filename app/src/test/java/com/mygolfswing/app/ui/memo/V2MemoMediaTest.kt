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

    @Test
    fun deleteDraftLocalMediaRemovesVideoAndThumbnail() {
        val root = createTempDirectory("v2-memo-draft-media").toFile()
        val video = File(root, "draft.mp4").apply { writeBytes(ByteArray(7)) }
        val thumb = File(root, "draft.jpg").apply { writeBytes(ByteArray(3)) }

        val freed = deleteDraftLocalMedia("file://${video.absolutePath}", thumb.absolutePath)

        assertTrue(freed >= 10L)
        assertFalse(video.exists())
        assertFalse(thumb.exists())
        root.deleteRecursively()
    }

    @Test
    fun deleteDraftLocalMediaKeepsProtectedReplacementFiles() {
        val root = createTempDirectory("v2-memo-draft-protected").toFile()
        val video = File(root, "draft.mp4").apply { writeBytes(ByteArray(7)) }
        val thumb = File(root, "draft.jpg").apply { writeBytes(ByteArray(3)) }

        val freed = deleteDraftLocalMedia(
            videoPath = video.absolutePath,
            thumbnailPath = thumb.absolutePath,
            protectedPaths = setOf(video.absolutePath),
        )

        assertTrue(freed >= 3L)
        assertTrue(video.exists())
        assertFalse(thumb.exists())
        root.deleteRecursively()
    }
}
