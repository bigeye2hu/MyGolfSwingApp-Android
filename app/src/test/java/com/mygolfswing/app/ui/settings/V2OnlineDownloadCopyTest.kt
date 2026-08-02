package com.mygolfswing.app.ui.settings

import com.mygolfswing.app.data.repo.V2DownloadStartResult
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class V2OnlineDownloadCopyTest {

    @Test
    fun releaseDownloadErrorHidesFetchDiagnostics() {
        val message = downloadErrorMessage(
            base = "无法获取视频信息，请检查链接后重试",
            debugDetail = "fetchMetadata failed url=https://v.douyin.com/private-video?token=abc123 HTTP 502",
            isDebuggable = false,
        )

        assertEquals("无法获取视频信息，请检查链接后重试", message)
        assertFalse(message.contains("fetchMetadata"))
        assertFalse(message.contains("https://"))
        assertFalse(message.contains("token=abc123"))
        assertFalse(message.contains("HTTP 502"))
    }

    @Test
    fun releaseDownloadErrorHidesCancelAndJobDiagnostics() {
        val message = downloadErrorMessage(
            base = "取消失败，请稍后重试；如已扣积分，请稍后刷新余额",
            debugDetail = "download.cancel failed jobId=job-private-001 uid=/users/private-user",
            isDebuggable = false,
        )

        assertEquals("取消失败，请稍后重试；如已扣积分，请稍后刷新余额", message)
        assertFalse(message.contains("download.cancel"))
        assertFalse(message.contains("job-private-001"))
        assertFalse(message.contains("/users/"))
    }

    @Test
    fun releaseDownloadErrorHidesConfirmAndLocalFileDiagnostics() {
        val message = downloadErrorMessage(
            base = "下载确认失败，请稍后重试；如不再继续，可取消任务恢复积分",
            debugDetail = "confirmDownload IOException local=/data/user/0/com.mygolfswing.app/files/users/u1/videos/private.mp4",
            isDebuggable = false,
        )

        assertEquals("下载确认失败，请稍后重试；如不再继续，可取消任务恢复积分", message)
        assertFalse(message.contains("confirmDownload"))
        assertFalse(message.contains("IOException"))
        assertFalse(message.contains("/data/"))
        assertFalse(message.contains("private.mp4"))
    }

    @Test
    fun debugDownloadErrorKeepsDiagnostics() {
        val message = downloadErrorMessage(
            base = "无法获取视频信息，请检查链接后重试",
            debugDetail = "metadata parser rejected payload",
            isDebuggable = true,
        )

        assertTrue(message.startsWith("无法获取视频信息，请检查链接后重试"))
        assertTrue(message.contains("metadata parser rejected payload"))
    }

    @Test
    fun downloadErrorIgnoresBlankDiagnosticsInAnyBuild() {
        assertEquals(
            "无法获取视频信息，请检查链接后重试",
            downloadErrorMessage("无法获取视频信息，请检查链接后重试", "  \n\t  ", isDebuggable = true),
        )
    }

    @Test
    fun downloadDraftPersistsOnlyWhenIdleWithoutActiveTaskOrCreatedMemo() {
        assertTrue(
            shouldPersistDownloadDraft(
                phase = DownloadPhase.IDLE,
                startResult = null,
                createdMemoId = null,
            ),
        )
        assertFalse(
            shouldPersistDownloadDraft(
                phase = DownloadPhase.FETCHING,
                startResult = null,
                createdMemoId = null,
            ),
        )
        assertFalse(
            shouldPersistDownloadDraft(
                phase = DownloadPhase.IDLE,
                startResult = V2DownloadStartResult(
                    jobId = "job-1",
                    platform = "抖音",
                    costCredits = 10,
                    status = "ready",
                    message = "ready",
                    videoInfo = null,
                ),
                createdMemoId = null,
            ),
        )
        assertFalse(
            shouldPersistDownloadDraft(
                phase = DownloadPhase.IDLE,
                startResult = null,
                createdMemoId = UUID.randomUUID(),
            ),
        )
    }

    @Test
    fun downloadCancellationRestoresActionablePhase() {
        assertEquals(
            DownloadPhase.IDLE,
            downloadPhaseAfterCancellation(DownloadPhase.FETCHING, hasActiveTask = false),
        )
        assertEquals(
            DownloadPhase.READY,
            downloadPhaseAfterCancellation(DownloadPhase.CONFIRMING, hasActiveTask = true),
        )
        assertEquals(
            DownloadPhase.READY,
            downloadPhaseAfterCancellation(DownloadPhase.DOWNLOADING, hasActiveTask = true),
        )
        assertEquals(
            DownloadPhase.READY,
            downloadPhaseAfterCancellation(DownloadPhase.CANCELLING, hasActiveTask = true),
        )
    }

    @Test
    fun downloadCancellationLeavesNonBusyPhasesUnchanged() {
        assertEquals(
            DownloadPhase.COMPLETED,
            downloadPhaseAfterCancellation(DownloadPhase.COMPLETED, hasActiveTask = true),
        )
        assertEquals(
            DownloadPhase.READY,
            downloadPhaseAfterCancellation(DownloadPhase.READY, hasActiveTask = true),
        )
    }

    @Test
    fun downloadResultFailureKeepsOrdinaryFailuresHandled() {
        val error = IllegalStateException("download start failed")
        val result = downloadResultFailure<Unit>(error)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun downloadThrowableErrorKeepsOrdinaryFailuresHandled() {
        assertEquals(
            "下载确认失败，请稍后重试；如不再继续，可取消任务恢复积分",
            downloadThrowableError(
                "下载确认失败，请稍后重试；如不再继续，可取消任务恢复积分",
                IllegalStateException("confirm failed jobId=job-private local=/data/private.mp4"),
            ),
        )
    }

    @Test
    fun downloadThrowableErrorPreservesCoroutineCancellation() {
        try {
            downloadThrowableError("无法获取视频信息，请检查链接后重试", CancellationException("download result disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("download result disposed", e.message)
        }
    }

    @Test
    fun downloadResultFailurePreservesCoroutineCancellation() {
        try {
            downloadResultFailure<Unit>(CancellationException("download screen disposed"))
            fail("CancellationException should be rethrown")
        } catch (e: CancellationException) {
            assertEquals("download screen disposed", e.message)
        }
    }

    @Test
    fun parsePendingDownloadTaskRestoresRequiredFieldsAndVideoInfo() {
        val task = parsePendingDownloadTask(
            """
            {
              "url": "https://example.com/source",
              "jobId": "job-123",
              "platform": "抖音",
              "status": "ready",
              "message": "等待确认",
              "videoInfo": {
                "title": "练习视频",
                "author": "教练",
                "download_url": "https://cdn.example.com/video.mp4",
                "cover": "https://cdn.example.com/cover.jpg"
              }
            }
            """.trimIndent(),
        )

        requireNotNull(task)
        assertEquals("https://example.com/source", task.url)
        assertEquals("job-123", task.start.jobId)
        assertEquals("抖音", task.start.platform)
        assertEquals(10, task.start.costCredits)
        assertEquals("ready", task.start.status)
        assertEquals("等待确认", task.start.message)
        assertEquals("练习视频", task.info.title)
        assertEquals("教练", task.info.author)
        assertEquals("https://cdn.example.com/video.mp4", task.info.downloadUrl)
        assertEquals("https://cdn.example.com/cover.jpg", task.info.thumbnailUrl)
    }

    @Test
    fun parsePendingDownloadTaskRejectsMalformedOrIncompletePayloads() {
        assertEquals(null, parsePendingDownloadTask("not-json"))
        assertEquals(null, parsePendingDownloadTask("""{"url":"https://example.com/source"}"""))
        assertEquals(null, parsePendingDownloadTask("""{"jobId":"job-123"}"""))
    }

    @Test
    fun parsePendingDownloadTaskUsesStoredCostWhenPresent() {
        val task = parsePendingDownloadTask(
            """
            {
              "url": "https://example.com/source",
              "jobId": "job-123",
              "costCredits": 20,
              "videoInfo": null
            }
            """.trimIndent(),
        )

        requireNotNull(task)
        assertEquals(20, task.start.costCredits)
        assertEquals("下载视频", task.info.title)
    }
}
