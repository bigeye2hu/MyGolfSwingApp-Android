package com.mygolfswing.app.data.repo

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
}
