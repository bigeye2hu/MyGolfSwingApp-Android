package com.mygolfswing.app.ui.swing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class V2SwingMediaTest {

    @Test
    fun swingMediaImageModelReturnsFileUriOnlyWhenLocalFileExists() {
        val root = createTempDirectory("v2-swing-media").toFile()
        val file = File(root, "clip.jpg").apply { writeBytes(ByteArray(3)) }

        assertEquals("file://${file.absolutePath}", swingMediaImageModel(file.absolutePath))
        assertNull(swingMediaImageModel(File(root, "missing.jpg").absolutePath))

        root.deleteRecursively()
    }

    @Test
    fun swingMediaImageModelKeepsRemoteUrls() {
        assertEquals("https://example.com/thumb.jpg", swingMediaImageModel(" https://example.com/thumb.jpg "))
        assertEquals("http://example.com/thumb.jpg", swingMediaImageModel("http://example.com/thumb.jpg"))
    }

    @Test
    fun swingClipPreviewModelFallsBackToExistingVideoPath() {
        val root = createTempDirectory("v2-swing-preview").toFile()
        val video = File(root, "clip.mp4").apply { writeBytes(ByteArray(5)) }

        assertEquals(
            "file://${video.absolutePath}",
            swingClipPreviewModel(File(root, "missing.jpg").absolutePath, video.absolutePath),
        )

        root.deleteRecursively()
    }
}
