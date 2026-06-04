package com.mygolfswing.app.data.repo

import kotlinx.serialization.json.Json
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

    @Test
    fun decodeSwingSessionJsonSkipsMalformedClipsWithoutDroppingSession() {
        val session = decodeSwingSessionJson(
            """
            {
              "id": "00000000-0000-0000-0000-000000000100",
              "userId": "user-1",
              "date": "2026-06-04",
              "locationName": "练习场",
              "clips": [
                {
                  "id": "00000000-0000-0000-0000-000000000101",
                  "videoPath": "/tmp/good.mp4",
                  "thumbnailPath": "/tmp/good.jpg",
                  "timestamp": "2026-06-04T09:30:00",
                  "durationSec": 4.5,
                  "retention": "temporary"
                },
                {
                  "id": "00000000-0000-0000-0000-000000000102",
                  "thumbnailPath": "/tmp/missing-video.jpg",
                  "timestamp": "2026-06-04T09:31:00",
                  "durationSec": 4.0
                }
              ]
            }
            """.trimIndent(),
            Json,
        )

        assertEquals("user-1", session?.userId)
        assertEquals(1, session?.clips?.size)
        assertEquals("/tmp/good.mp4", session?.clips?.single()?.videoPath)
    }
}
