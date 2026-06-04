package com.mygolfswing.app.data.repo

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
