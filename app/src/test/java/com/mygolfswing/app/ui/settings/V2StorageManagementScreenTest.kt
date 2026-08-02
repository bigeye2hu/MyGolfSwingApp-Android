package com.mygolfswing.app.ui.settings

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2StorageManagementScreenTest {

    @Test
    fun cacheSizeExcludesTemporaryVideoDirectories() {
        withTempRoot { root ->
            val cacheDir = File(root, "cache")
            val externalCacheDir = File(root, "external-cache")
            writeSizedFile(cacheDir, "images/thumb.bin", 10)
            writeSizedFile(cacheDir, "videos/temp.mp4", 90)
            writeSizedFile(externalCacheDir, "http/body.cache", 20)
            writeSizedFile(externalCacheDir, "videos/external-temp.mp4", 80)

            assertEquals(30L, calculateCacheBytes(cacheDir, externalCacheDir))
            assertEquals(170L, calculateTemporaryVideoBytes(cacheDir, externalCacheDir))
        }
    }

    @Test
    fun clearAppCachePreservesTemporaryVideosAndReportsFreedBytes() {
        withTempRoot { root ->
            val cacheDir = File(root, "cache")
            val externalCacheDir = File(root, "external-cache")
            val cacheFile = writeSizedFile(cacheDir, "images/thumb.bin", 10)
            val temporaryVideo = writeSizedFile(cacheDir, "videos/temp.mp4", 90)
            val externalCacheFile = writeSizedFile(externalCacheDir, "http/body.cache", 20)
            val externalTemporaryVideo = writeSizedFile(externalCacheDir, "videos/external-temp.mp4", 80)

            assertEquals(30L, clearAppCache(cacheDir, externalCacheDir))

            assertFalse(cacheFile.exists())
            assertFalse(externalCacheFile.exists())
            assertTrue(temporaryVideo.exists())
            assertTrue(externalTemporaryVideo.exists())
            assertEquals(170L, calculateTemporaryVideoBytes(cacheDir, externalCacheDir))
        }
    }

    @Test
    fun clearTemporaryVideosDeletesOnlyVideoDirectoriesAndReportsFreedBytes() {
        withTempRoot { root ->
            val cacheDir = File(root, "cache")
            val externalCacheDir = File(root, "external-cache")
            val cacheFile = writeSizedFile(cacheDir, "images/thumb.bin", 10)
            val temporaryVideo = writeSizedFile(cacheDir, "videos/temp.mp4", 90)
            val externalCacheFile = writeSizedFile(externalCacheDir, "http/body.cache", 20)
            val externalTemporaryVideo = writeSizedFile(externalCacheDir, "videos/external-temp.mp4", 80)

            assertEquals(170L, clearTemporaryVideos(cacheDir, externalCacheDir))

            assertTrue(cacheFile.exists())
            assertTrue(externalCacheFile.exists())
            assertFalse(temporaryVideo.exists())
            assertFalse(externalTemporaryVideo.exists())
            assertEquals(30L, calculateCacheBytes(cacheDir, externalCacheDir))
        }
    }

    @Test
    fun storageFailureMessagesStayReleaseSafeAndStable() {
        val rawPath = "/data/user/0/com.mygolfswing.app/cache/videos/private.mp4"
        val rawException = "java.io.IOException: permission denied"

        listOf(
            storageOperationFailureMessage(StorageOperationErrorKind.REFRESH),
            storageOperationFailureMessage(StorageOperationErrorKind.CACHE_CLEAR),
            storageOperationFailureMessage(StorageOperationErrorKind.VIDEO_CLEAR),
        ).forEach { message ->
            assertFalse(message.contains(rawPath))
            assertFalse(message.contains(rawException))
            assertFalse(message.contains("Exception"))
            assertFalse(message.contains("/data/"))
            assertTrue(message.endsWith("请稍后重试"))
        }
    }

    private fun withTempRoot(block: (File) -> Unit) {
        val root = Files.createTempDirectory("v2-storage-management-test").toFile()
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun writeSizedFile(root: File, relativePath: String, bytes: Int): File {
        val file = File(root, relativePath)
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(bytes) { 1 })
        return file
    }
}
