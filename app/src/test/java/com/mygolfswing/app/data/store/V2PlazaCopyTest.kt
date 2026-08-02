package com.mygolfswing.app.data.store

import com.mygolfswing.app.data.model.V2PlazaCoach
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.data.model.V2Topic
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V2PlazaCopyTest {

    @Test
    fun releasePlazaListErrorHidesBackendDiagnostics() {
        val message = plazaReleaseSafeError(
            base = "广场内容加载失败，请下拉重试",
            debugDetail = "V2PlazaRepository.fetchVideos HTTP 500 category=private videoId=video-secret",
            isDebuggable = false,
        )

        assertEquals("广场内容加载失败，请下拉重试", message)
        assertFalse(message.contains("V2PlazaRepository"))
        assertFalse(message.contains("HTTP 500"))
        assertFalse(message.contains("video-secret"))
    }

    @Test
    fun releasePlazaInteractionErrorHidesUserAndVideoDiagnostics() {
        val message = plazaReleaseSafeError(
            base = "收藏同步失败，请稍后重试",
            debugDetail = "toggleSave failed uid=/users/private-user videoId=video-private token=abc123",
            isDebuggable = false,
        )

        assertEquals("收藏同步失败，请稍后重试", message)
        assertFalse(message.contains("toggleSave"))
        assertFalse(message.contains("/users/"))
        assertFalse(message.contains("video-private"))
        assertFalse(message.contains("abc123"))
    }

    @Test
    fun releaseCoachProfileErrorHidesCoachDiagnostics() {
        val message = plazaReleaseSafeError(
            base = "教练主页加载失败，请稍后重试",
            debugDetail = "fetchCoachProfile coachId=coach-secret endpoint=/video-plaza/coaches/private",
            isDebuggable = false,
        )

        assertEquals("教练主页加载失败，请稍后重试", message)
        assertFalse(message.contains("fetchCoachProfile"))
        assertFalse(message.contains("coach-secret"))
        assertFalse(message.contains("/video-plaza/"))
    }

    @Test
    fun releasePlazaPlayerErrorHidesCommentAndFeedbackDiagnostics() {
        val message = plazaReleaseSafeError(
            base = "举报失败，请稍后重试",
            debugDetail = "reportComment failed commentId=comment-private videoId=video-secret uid=/users/private-user",
            isDebuggable = false,
        )

        assertEquals("举报失败，请稍后重试", message)
        assertFalse(message.contains("reportComment"))
        assertFalse(message.contains("comment-private"))
        assertFalse(message.contains("video-secret"))
        assertFalse(message.contains("/users/"))
    }

    @Test
    fun debugPlazaErrorKeepsDiagnostics() {
        val message = plazaReleaseSafeError(
            base = "评论加载失败，请稍后重试",
            debugDetail = "comments endpoint returned malformed payload",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("评论加载失败，请稍后重试"))
        assertTrue(message.contains("comments endpoint returned malformed payload"))
    }

    @Test
    fun plazaErrorIgnoresBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "评论加载失败，请稍后重试",
            plazaReleaseSafeError("评论加载失败，请稍后重试", " \n\t ", isDebuggable = true),
        )
    }

    @Test
    fun plazaMockFallbackIsDebugOnlyAndOnlyForEmptyLists() {
        assertFalse(shouldUsePlazaMockFallback(hasExistingVideos = false, isDebuggable = false))
        assertFalse(shouldUsePlazaMockFallback(hasExistingVideos = true, isDebuggable = true))
        assertTrue(shouldUsePlazaMockFallback(hasExistingVideos = false, isDebuggable = true))
    }

    @Test
    fun plazaThrowableErrorKeepsOrdinaryFailuresHandled() {
        assertEquals(
            "举报失败，请稍后重试",
            plazaThrowableError(
                "举报失败，请稍后重试",
                IllegalStateException("reportComment failed commentId=secret videoId=private"),
            ),
        )
    }

    @Test
    fun plazaThrowableErrorPreservesCoroutineCancellation() {
        try {
            plazaThrowableError("评论加载失败，请稍后重试", CancellationException("plaza player disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("plaza player disposed", e.message)
        }
    }

    @Test
    fun parseSavedVideosRestoresValidItemsAndSkipsBadCacheRows() {
        val videos = V2PlazaStore.parseSavedVideos(
            """
            [
              {
                "id": "11111111-1111-1111-1111-111111111111",
                "sourceVideoId": "cloud-video-1",
                "sourceCoachId": "coach-1",
                "sourceCategoryId": "cat-swing",
                "sourceParentCategoryId": "cat-root",
                "title": "稳定下杆节奏",
                "durationSec": 42.5,
                "topic": "SWING",
                "categoryDisplayName": "挥杆",
                "coachName": "Coach Lee",
                "coachInitial": "L",
                "coachTint": "GOLD",
                "viewCount": 1234,
                "saveCount": 56,
                "thumbnailKind": "MINT_TIP",
                "thumbnailUrl": "https://cdn.example.com/thumb.jpg",
                "videoUrl": "https://cdn.example.com/video.mp4",
                "publishedAtMs": 1700000000000,
                "isNew": true
              },
              "bad-row",
              { "saveKey": "legacy-save-key" }
            ]
            """.trimIndent(),
        )

        assertEquals(2, videos.size)
        val restored = videos.first()
        assertEquals("cloud-video-1", restored.sourceVideoId)
        assertEquals("稳定下杆节奏", restored.title)
        assertEquals(V2Topic.SWING, restored.topic)
        assertEquals(V2PlazaCoach.AvatarTint.GOLD, restored.coach.avatarTint)
        assertEquals(V2PlazaVideo.ThumbnailKind.MINT_TIP, restored.thumbnailKind)
        assertEquals("https://cdn.example.com/video.mp4", restored.videoUrl)
        assertTrue(restored.isSaved)
        assertTrue(restored.isNew)

        val legacy = videos.last()
        assertEquals("已保存视频", legacy.title)
        assertEquals("legacy-save-key", legacy.sourceVideoId)
        assertTrue(legacy.isSaved)
    }

    @Test
    fun parseWatchHistoryRestoresValidItemsAndRejectsIncompleteRows() {
        val history = V2PlazaStore.parseWatchHistory(
            """
            [
              {
                "videoId": "22222222-2222-2222-2222-222222222222",
                "saveKey": "cloud-video-2",
                "sourceVideoId": "cloud-video-2",
                "sourceCoachId": "coach-2",
                "sourceCategoryId": "cat-putting",
                "sourceParentCategoryId": "cat-root",
                "title": "推杆距离控制",
                "coachName": "张教练",
                "coachInitial": "张",
                "coachTint": "SAND",
                "thumbnailUrl": "https://cdn.example.com/putting.jpg",
                "thumbnailKind": "SAND_WARM",
                "videoUrl": "https://cdn.example.com/putting.mp4",
                "topic": "PUTTING",
                "categoryDisplayName": "推杆",
                "viewCount": 88,
                "saveCount": 9,
                "durationSec": 75.0,
                "progress": 1.8,
                "watchedAtMs": 1700000012345
              },
              { "videoId": "not-a-uuid", "saveKey": "bad" },
              { "videoId": "33333333-3333-3333-3333-333333333333" }
            ]
            """.trimIndent(),
        )

        assertEquals(1, history.size)
        val restored = history.single()
        assertEquals("22222222-2222-2222-2222-222222222222", restored.videoId.toString())
        assertEquals("cloud-video-2", restored.saveKey)
        assertEquals("推杆距离控制", restored.title)
        assertEquals("SAND", restored.coachTint)
        assertEquals("SAND_WARM", restored.thumbnailKind)
        assertEquals("PUTTING", restored.topic)
        assertEquals(1.0, restored.progress, 0.0)
    }

    @Test
    fun plazaCacheParsersTreatMalformedPayloadsAsEmpty() {
        assertTrue(V2PlazaStore.parseSavedVideos("not-json").isEmpty())
        assertTrue(V2PlazaStore.parseWatchHistory("""{"items":[]}""").isEmpty())
    }
}
