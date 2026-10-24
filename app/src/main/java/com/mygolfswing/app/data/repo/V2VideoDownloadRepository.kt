package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.net.V2ApiConfig
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 基于积分的视频下载仓库（对齐 iOS `CreditsBasedDownloadService`）。
 *
 * 这里只封装 download server 的任务生命周期；实际“保存成本地文件/生成 Memo”的 UI 流程
 * 由后续页面决定，避免把下载任务和 Memo 创建强耦合。
 */
class V2VideoDownloadRepository(
    private val net: V2NetClient,
    private val session: V2SessionStore,
) {
    /** Mirrors iOS `TikTokGolfScraperClient.checkConnection()` against the download host root. */
    suspend fun checkServiceConnection(): Boolean = try {
        val request = Request.Builder()
            .url(V2ApiConfig.DOWNLOAD_SERVICE_HEALTH)
            .get()
            .build()
        net.httpClient.newCall(request).awaitResponse().use { response ->
            isDownloadServiceHealthy(response.code)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Throwable) {
        false
    }

    suspend fun start(url: String): Result<V2DownloadStartResult> = runDownloadCatching {
        require(session.current().isLoggedIn) { "请先登录后再下载视频" }
        val resp = net.downloadApi.startDownload(JsonObject(mapOf("url" to JsonPrimitive(url))))
        V2DownloadStartResult(
            jobId = resp.string("job_id") ?: error("下载任务未返回 job_id"),
            platform = resp.string("platform"),
            costCredits = resp["cost_credits"]?.jsonPrimitive?.intOrNull ?: 0,
            status = resp.string("status") ?: "",
            message = resp.string("message") ?: "",
            videoInfo = resp["video_info"],
        )
    }

    suspend fun confirm(jobId: String): Result<V2DownloadConfirmResult> = runDownloadCatching {
        require(session.current().isLoggedIn) { "请先登录后再确认下载" }
        val resp = net.downloadApi.confirmDownload(JsonObject(mapOf("job_id" to JsonPrimitive(jobId))))
        V2DownloadConfirmResult(
            success = resp["success"]?.jsonPrimitive?.booleanOrNull ?: true,
            message = resp.string("message") ?: "",
            creditsDeducted = resp["credits_deducted"]?.jsonPrimitive?.intOrNull ?: 0,
            videoInfo = resp["video_info"],
        )
    }

    suspend fun cancel(jobId: String): Result<V2DownloadCancelResult> = runDownloadCatching {
        require(session.current().isLoggedIn) { "请先登录后再取消下载" }
        val resp = net.downloadApi.cancelDownload(JsonObject(mapOf("job_id" to JsonPrimitive(jobId))))
        V2DownloadCancelResult(
            success = resp["success"]?.jsonPrimitive?.booleanOrNull ?: true,
            message = resp.string("message") ?: "",
            creditsRefunded = resp["credits_refunded"]?.jsonPrimitive?.intOrNull ?: 0,
        )
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

}

internal fun isDownloadServiceHealthy(statusCode: Int?): Boolean = statusCode == 200

private suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) continuation.resume(response) else response.close()
        }
    })
}

internal suspend inline fun <T> runDownloadCatching(
    crossinline block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}

data class V2DownloadStartResult(
    val jobId: String,
    val platform: String?,
    val costCredits: Int,
    val status: String,
    val message: String,
    val videoInfo: JsonElement?,
)

data class V2DownloadConfirmResult(
    val success: Boolean,
    val message: String,
    val creditsDeducted: Int,
    val videoInfo: JsonElement?,
)

data class V2DownloadCancelResult(
    val success: Boolean,
    val message: String,
    val creditsRefunded: Int,
)
