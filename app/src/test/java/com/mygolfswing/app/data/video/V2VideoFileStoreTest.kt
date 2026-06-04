package com.mygolfswing.app.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class V2VideoFileStoreTest {

    @Test
    fun deleteRecursivelyCountingFreedBytesReturnsActualDeletedBytes() {
        val root = createTempDirectory("v2-video-clear").toFile()
        val videos = File(root, "videos").apply { mkdirs() }
        val thumbs = File(root, "thumbs").apply { mkdirs() }
        File(videos, "a.mp4").writeBytes(ByteArray(10))
        File(thumbs, "a.jpg").writeBytes(ByteArray(7))

        val freed = deleteRecursivelyCountingFreedBytes(videos, thumbs)

        assertEquals(17L, freed)
        assertFalse(videos.exists())
        assertFalse(thumbs.exists())
        root.deleteRecursively()
    }

    @Test
    fun deleteRecursivelyCountingFreedBytesIgnoresMissingDirs() {
        val root = createTempDirectory("v2-video-clear-missing").toFile()
        val missing = File(root, "missing")

        assertEquals(0L, deleteRecursivelyCountingFreedBytes(missing))

        root.deleteRecursively()
    }
}
