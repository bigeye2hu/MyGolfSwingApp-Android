package com.mygolfswing.app.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2MemoRepositoryTest {

    @Test
    fun remoteThumbnailUrlForUploadAcceptsRemoteUrlsIgnoringWhitespaceAndSchemeCase() {
        assertEquals(
            "HTTPS://example.com/thumb.jpg",
            remoteThumbnailUrlForUpload(" HTTPS://example.com/thumb.jpg "),
        )
        assertEquals(
            "http://example.com/thumb.jpg",
            remoteThumbnailUrlForUpload("http://example.com/thumb.jpg"),
        )
    }

    @Test
    fun remoteThumbnailUrlForUploadRejectsLocalOrBlankUrls() {
        assertNull(remoteThumbnailUrlForUpload("file:///tmp/thumb.jpg"))
        assertNull(remoteThumbnailUrlForUpload("/tmp/thumb.jpg"))
        assertNull(remoteThumbnailUrlForUpload(" "))
        assertNull(remoteThumbnailUrlForUpload(null))
    }

    @Test
    fun localThumbnailPathForUploadAcceptsOnlyLocalPaths() {
        assertEquals("/tmp/thumb.jpg", localThumbnailPathForUpload(" file:///tmp/thumb.jpg "))
        assertEquals("/tmp/thumb.jpg", localThumbnailPathForUpload("/tmp/thumb.jpg"))
        assertNull(localThumbnailPathForUpload("https://example.com/thumb.jpg"))
        assertNull(localThumbnailPathForUpload(" "))
        assertNull(localThumbnailPathForUpload(null))
    }

    @Test
    fun parseCloudMemoMillisAcceptsPrimitiveAndMongoDateShapes() {
        assertEquals(1_717_488_000_000L, parseCloudMemoMillis(JsonPrimitive(1_717_488_000_000L)))
        assertEquals(
            1_717_488_000_000L,
            parseCloudMemoMillis(JsonObject(mapOf("\$date" to JsonPrimitive(1_717_488_000_000L)))),
        )
        assertEquals(
            1_717_488_000_000L,
            parseCloudMemoMillis(JsonObject(mapOf("\$date" to JsonPrimitive("2024-06-04T08:00:00Z")))),
        )
    }

    @Test
    fun parseCloudMemoMillisRejectsBlankOrMalformedValues() {
        assertNull(parseCloudMemoMillis(JsonPrimitive(" ")))
        assertNull(parseCloudMemoMillis(JsonPrimitive("not-a-date")))
        assertNull(parseCloudMemoMillis(JsonObject(emptyMap())))
    }

    @Test
    fun localDemoMemoCleanupOnlyDeletesCleanLegacyRows() {
        assertEquals(
            true,
            shouldDeleteLocalDemoMemo(
                title = "握把压力的微调",
                body = "今天发现握把太用力会让手腕僵硬，松一点反而更顺。下次保持 6 分力试试。",
                isDirty = false,
            ),
        )
        assertEquals(
            false,
            shouldDeleteLocalDemoMemo(
                title = "握把压力的微调",
                body = "今天发现握把太用力会让手腕僵硬，松一点反而更顺。下次保持 6 分力试试。",
                isDirty = true,
            ),
        )
    }

    @Test
    fun localDemoMemoCleanupKeepsUserContentWithSimilarTitles() {
        assertEquals(
            false,
            shouldDeleteLocalDemoMemo(
                title = "握把压力的微调",
                body = "这是用户自己的复盘内容，不应只因为标题命中旧演示数据就被清理。",
                isDirty = false,
            ),
        )
    }

    @Test
    fun runMemoSyncCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        val error = IllegalStateException("memo sync failed")
        val result = runMemoSyncCatching<Int> { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runMemoSyncCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runMemoSyncCatching<Int> { throw CancellationException("memo sync disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("memo sync disposed", e.message)
        }
    }

    @Test
    fun runMemoCloudBooleanCatchingKeepsOrdinaryFailuresHandled() = runBlocking {
        assertFalse(
            runMemoCloudBooleanCatching {
                throw IllegalStateException("memo cloud write failed")
            },
        )
    }

    @Test
    fun runMemoCloudBooleanCatchingPreservesCoroutineCancellation() = runBlocking {
        try {
            runMemoCloudBooleanCatching { throw CancellationException("memo cloud disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("memo cloud disposed", e.message)
        }
    }
}
