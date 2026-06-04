package com.mygolfswing.app.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class V2SwingSessionRepositoryTest {

    @Test
    fun localSwingMediaFileNormalizesFileSchemeAndWhitespace() {
        assertEquals("/tmp/swing.mp4", localSwingMediaFile(" FILE:///tmp/swing.mp4 ")?.path)
        assertEquals("/tmp/swing.mp4", localSwingMediaFile("/tmp/swing.mp4")?.path)
    }

    @Test
    fun localSwingMediaFileRejectsRemoteUrls() {
        assertNull(localSwingMediaFile("https://example.com/swing.mp4"))
        assertNull(localSwingMediaFile(" HTTPS://example.com/swing.mp4 "))
        assertNull(localSwingMediaFile(null))
    }
}
