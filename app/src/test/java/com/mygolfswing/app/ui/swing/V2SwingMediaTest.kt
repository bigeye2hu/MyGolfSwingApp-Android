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
        assertEquals("https://example.com/thumb.jpg", swingMediaImageModel("http://example.com/thumb.jpg"))
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

    @Test
    fun swingReplayPlayersUseFitResizeModeToAvoidCroppingAnalysisFootage() {
        listOf(
            "V2SwingClipDetailScreen.kt",
            "V2SwingAnalysisTrimScreen.kt",
            "V2SwingDayDetailScreen.kt",
        ).forEach { fileName ->
            val source = repoSwingFile(fileName).readText()
            assertEquals(
                "$fileName should keep local swing footage fully visible like iOS .resizeAspect",
                1,
                Regex("resizeMode\\s*=\\s*AspectRatioFrameLayout\\.RESIZE_MODE_FIT").findAll(source).count(),
            )
        }
    }

    @Test
    fun swingAnalysisTrimDurationFallsBackForInvalidMetadata() {
        assertEquals(1L, swingAnalysisTrimDurationMs(Double.NaN))
        assertEquals(1L, swingAnalysisTrimDurationMs(Double.POSITIVE_INFINITY))
        assertEquals(1L, swingAnalysisTrimDurationMs(0.0))
        assertEquals(1_500L, swingAnalysisTrimDurationMs(1.5))
    }

    @Test
    fun swingGalleryVideoSpecPreservesSupportedContainerMimeTypes() {
        assertEquals(
            SwingGalleryVideoSpec(displayName = "just_swing_clip.mov", mimeType = "video/quicktime"),
            swingGalleryVideoSpec(File("/tmp/source.MOV"), "just_swing_clip"),
        )
        assertEquals(
            SwingGalleryVideoSpec(displayName = "just_swing_clip.m4v", mimeType = "video/x-m4v"),
            swingGalleryVideoSpec(File("/tmp/source.m4v"), "just_swing_clip"),
        )
        assertEquals(
            SwingGalleryVideoSpec(displayName = "just_swing_clip.mov", mimeType = "video/quicktime"),
            swingGalleryVideoSpec(File("/tmp/source.qt"), "just_swing_clip"),
        )
        assertEquals(
            SwingGalleryVideoSpec(displayName = "just_swing_clip.mp4", mimeType = "video/mp4"),
            swingGalleryVideoSpec(File("/tmp/source.avi"), "just_swing_clip"),
        )
    }

    private fun repoSwingFile(fileName: String): File =
        listOf(
            File("app/src/main/java/com/mygolfswing/app/ui/swing/$fileName"),
            File("../app/src/main/java/com/mygolfswing/app/ui/swing/$fileName"),
        ).first { it.exists() }
}
