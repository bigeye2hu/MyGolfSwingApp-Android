package com.mygolfswing.app.data.video

import android.media.MediaCodec
import android.media.MediaExtractor
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import kotlin.io.path.createTempDirectory

class V2VideoFileStoreTest {

    @Test
    fun normalizedRemoteVideoDownloadUrlKeepsHttps() {
        assertEquals(
            "https://example.com/video.mp4",
            normalizedRemoteVideoDownloadUrl(" https://example.com/video.mp4 "),
        )
    }

    @Test
    fun normalizedRemoteVideoDownloadUrlUpgradesHttpForReleaseCleartextPolicy() {
        assertEquals(
            "https://example.com/video.mp4",
            normalizedRemoteVideoDownloadUrl("http://example.com/video.mp4"),
        )
    }

    @Test
    fun normalizedRemoteVideoDownloadUrlRejectsNonHttpSchemes() {
        assertNull(normalizedRemoteVideoDownloadUrl("file:///tmp/video.mp4"))
        assertNull(normalizedRemoteVideoDownloadUrl("content://media/external/video/1"))
    }

    @Test
    fun sourceVideoSizeIsAllowedMatchesIosTwoGbImportLimit() {
        assertTrue(sourceVideoSizeIsAllowed(null))
        assertTrue(sourceVideoSizeIsAllowed(0L))
        assertTrue(sourceVideoSizeIsAllowed(MAX_IMPORT_VIDEO_SIZE_BYTES))
        assertFalse(sourceVideoSizeIsAllowed(MAX_IMPORT_VIDEO_SIZE_BYTES + 1L))
    }

    @Test
    fun localVideoFileIsUsableForImportRejectsMissingEmptyDirectoryAndOversizedFiles() {
        val root = createTempDirectory("v2-video-local-usable").toFile()
        val missing = File(root, "missing.mp4")
        val empty = File(root, "empty.mp4").apply { writeBytes(ByteArray(0)) }
        val directory = File(root, "dir.mp4").apply { mkdirs() }
        val oversized = File(root, "oversized.mp4").apply {
            RandomAccessFile(this, "rw").use { it.setLength(MAX_IMPORT_VIDEO_SIZE_BYTES + 1L) }
        }
        val valid = File(root, "valid.mp4").apply { writeBytes(byteArrayOf(1)) }

        assertFalse(localVideoFileIsUsableForImport(missing))
        assertFalse(localVideoFileIsUsableForImport(empty))
        assertFalse(localVideoFileIsUsableForImport(directory))
        assertFalse(localVideoFileIsUsableForImport(oversized))
        assertTrue(localVideoFileIsUsableForImport(valid))

        root.deleteRecursively()
    }

    @Test
    fun importedVideoDurationMsRejectsNonFiniteNonPositiveAndOverflowingValues() {
        assertNull(importedVideoDurationMs(Double.NaN))
        assertNull(importedVideoDurationMs(Double.POSITIVE_INFINITY))
        assertNull(importedVideoDurationMs(Double.NEGATIVE_INFINITY))
        assertNull(importedVideoDurationMs(0.0))
        assertNull(importedVideoDurationMs(-1.0))
        assertNull(importedVideoDurationMs(MAX_VIDEO_TRIM_DURATION_MS.toDouble() / 1_000.0 + 1.0))
    }

    @Test
    fun importedVideoDurationMsKeepsFinitePositiveDurations() {
        assertEquals(1L, importedVideoDurationMs(0.0004))
        assertEquals(1_500L, importedVideoDurationMs(1.5))
        assertEquals(9_001L, importedVideoDurationMs(9.001))
    }

    @Test
    fun normalizedVideoTrimRangeClampsToMinimumDuration() {
        val range = normalizedVideoTrimRange(
            durationMs = 10_000L,
            startMs = 9_900L,
            endMs = 10_000L,
        )

        assertEquals(VideoTrimRange(startMs = 9_500L, endMs = 10_000L), range)
        assertEquals(MIN_VIDEO_TRIM_DURATION_MS, range?.durationMs)
    }

    @Test
    fun normalizedVideoTrimRangeRepairsInvertedUserSelection() {
        assertEquals(
            VideoTrimRange(startMs = 5_000L, endMs = 5_500L),
            normalizedVideoTrimRange(durationMs = 10_000L, startMs = 5_000L, endMs = 1_000L),
        )
    }

    @Test
    fun normalizedVideoTrimRangeKeepsTinyVideosWhole() {
        assertEquals(
            VideoTrimRange(startMs = 0L, endMs = 320L),
            normalizedVideoTrimRange(durationMs = 320L, startMs = 100L, endMs = 120L),
        )
    }

    @Test
    fun videoTrimKeepsFullSourceUsesTolerance() {
        assertTrue(
            videoTrimKeepsFullSource(
                durationMs = 10_000L,
                range = VideoTrimRange(startMs = 50L, endMs = 9_950L),
            ),
        )
        assertFalse(
            videoTrimKeepsFullSource(
                durationMs = 10_000L,
                range = VideoTrimRange(startMs = 250L, endMs = 9_950L),
            ),
        )
    }

    @Test
    fun shouldStopTrimmedTrackCopyUsesHalfOpenEndBoundary() {
        assertTrue(shouldStopTrimmedTrackCopy(sampleTimeUs = -1L, endUs = 1_000L))
        assertFalse(shouldStopTrimmedTrackCopy(sampleTimeUs = 999L, endUs = 1_000L))
        assertTrue(shouldStopTrimmedTrackCopy(sampleTimeUs = 1_000L, endUs = 1_000L))
        assertTrue(shouldStopTrimmedTrackCopy(sampleTimeUs = 1_001L, endUs = 1_000L))
    }

    @Test
    fun supportedVideoExtensionPreservesIosSupportedContainers() {
        assertEquals("mp4", supportedVideoExtensionFromName("clip.MP4"))
        assertEquals("m4v", supportedVideoExtensionFromName("clip.m4v"))
        assertEquals("mov", supportedVideoExtensionFromName("clip.MOV"))
        assertEquals("mov", supportedVideoExtensionFromName("clip.qt"))
        assertEquals("mov", supportedVideoExtensionFromName("https://example.com/clip.mov?token=hidden"))
        assertNull(supportedVideoExtensionFromName("clip.avi"))
        assertNull(supportedVideoExtensionFromName("clip"))
    }

    @Test
    fun videoFileNameKeepsSupportedSourceExtensionOrFallsBackToMp4() {
        assertEquals("memo_20260729_120000.mov", videoFileName("memo", "20260729_120000", "source.MOV"))
        assertEquals("download_20260729_120000.m4v", videoFileName("download", "20260729_120000", "https://example.com/source.m4v?download=1"))
        assertEquals("memo_20260729_120000.mp4", videoFileName("memo", "20260729_120000", "source.avi"))
    }

    @Test
    fun runVideoFileStoreCatchingKeepsOrdinaryFailuresHandled() {
        val error = IllegalStateException("copy failed")
        val result = runVideoFileStoreCatching<Unit> { throw error }

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runVideoFileStoreCatchingPreservesCoroutineCancellation() {
        try {
            runVideoFileStoreCatching<Unit> { throw CancellationException("video import disposed") }
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("video import disposed", e.message)
        }
    }

    @Test
    fun archiveLocalVideoFileMovesCameraTempFileWhenPossible() {
        val root = createTempDirectory("v2-video-archive").toFile()
        val source = File(root, "cache/capture.mp4").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        val target = File(root, "files/videos/memo.mp4")

        assertTrue(archiveLocalVideoFile(source, target))
        assertFalse(source.exists())
        assertEquals(4L, target.length())

        root.deleteRecursively()
    }

    @Test
    fun archiveLocalVideoFileAcceptsAlreadyArchivedSource() {
        val root = createTempDirectory("v2-video-archive-same").toFile()
        val file = File(root, "files/videos/memo.mp4").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }

        assertTrue(archiveLocalVideoFile(file, file))
        assertTrue(file.exists())
        assertEquals(3L, file.length())

        root.deleteRecursively()
    }

    @Test
    fun archiveLocalVideoFilePreservesMovTargetExtension() {
        val root = createTempDirectory("v2-video-archive-mov").toFile()
        val source = File(root, "cache/capture.mov").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        }
        val target = File(root, "files/videos/${videoFileName("memo", "20260729_120000", source.name)}")

        assertTrue(archiveLocalVideoFile(source, target))
        assertEquals("memo_20260729_120000.mov", target.name)
        assertFalse(source.exists())
        assertEquals(5L, target.length())

        root.deleteRecursively()
    }

    @Test
    fun muxerSampleFlagsMapExtractorFlagsToCodecBufferFlags() {
        assertEquals(
            MediaCodec.BUFFER_FLAG_KEY_FRAME,
            V2VideoFileStore.muxerSampleFlags(MediaExtractor.SAMPLE_FLAG_SYNC),
        )
        assertEquals(
            MediaCodec.BUFFER_FLAG_KEY_FRAME or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME,
            V2VideoFileStore.muxerSampleFlags(
                MediaExtractor.SAMPLE_FLAG_SYNC or MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME,
            ),
        )
        assertEquals(0, V2VideoFileStore.muxerSampleFlags(MediaExtractor.SAMPLE_FLAG_ENCRYPTED))
    }

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
    fun deleteMediaFilesCountingFreedBytesPreservesProtectedFiles() {
        val root = createTempDirectory("v2-video-clear-protected").toFile()
        val videos = File(root, "videos").apply { mkdirs() }
        val thumbs = File(root, "thumbs").apply { mkdirs() }
        val protectedVideo = File(videos, "keep.mp4").apply { writeBytes(ByteArray(10)) }
        val protectedThumb = File(thumbs, "keep.jpg").apply { writeBytes(ByteArray(7)) }
        val ordinaryVideo = File(videos, "delete.mp4").apply { writeBytes(ByteArray(13)) }
        val ordinaryThumb = File(thumbs, "delete.jpg").apply { writeBytes(ByteArray(5)) }

        val freed = deleteMediaFilesCountingFreedBytes(
            root,
            protectedAbsolutePaths = setOf(protectedVideo.absolutePath, protectedThumb.absolutePath),
        )

        assertEquals(18L, freed)
        assertTrue(protectedVideo.exists())
        assertTrue(protectedThumb.exists())
        assertFalse(ordinaryVideo.exists())
        assertFalse(ordinaryThumb.exists())
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
