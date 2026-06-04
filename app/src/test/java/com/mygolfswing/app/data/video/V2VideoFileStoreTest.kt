package com.mygolfswing.app.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class V2VideoFileStoreTest {

    @Test
    fun calculateMediaBytesCountsVideosAndThumbsWithoutCreatingMissingDirs() {
        val root = createTempDirectory("v2-video-size").toFile()
        val videos = File(root, "videos").apply { mkdirs() }
        val thumbs = File(root, "thumbs").apply { mkdirs() }
        File(videos, "a.mp4").writeBytes(ByteArray(10))
        File(thumbs, "a.jpg").writeBytes(ByteArray(7))

        assertEquals(17L, V2VideoFileStore.calculateMediaBytes(root))

        root.deleteRecursively()
    }

    @Test
    fun calculateMediaBytesDoesNotCreateMissingDirs() {
        val root = createTempDirectory("v2-video-size-missing").toFile()
        root.deleteRecursively()

        assertEquals(0L, V2VideoFileStore.calculateMediaBytes(root))
        assertFalse(root.exists())
    }

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

    @Test
    fun deleteFileCountingFreedBytesReturnsActualDeletedBytes() {
        val root = createTempDirectory("v2-video-temp-file").toFile()
        val file = File(root, "capture.mp4")
        file.writeBytes(ByteArray(11))

        assertEquals(11L, deleteFileCountingFreedBytes(file))
        assertFalse(file.exists())

        root.deleteRecursively()
    }

    @Test
    fun deleteFileCountingFreedBytesIgnoresMissingFiles() {
        val root = createTempDirectory("v2-video-temp-file-missing").toFile()
        val missing = File(root, "missing.mp4")

        assertEquals(0L, deleteFileCountingFreedBytes(missing))

        root.deleteRecursively()
    }
}
