package com.mygolfswing.app.data.net

import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * download.mygolfai.com.cn 上的 REST 接口（积分 / 下载任务 / AI 透传）。
 *
 * 下载流程对齐 iOS `CreditsBasedDownloadService`：
 * start 冻结积分并返回视频信息，confirm 扣积分，cancel 返还冻结积分。
 */
interface V2DownloadApi {

    @GET("credits/balance")
    suspend fun creditsBalance(): JsonObject

    @GET("ai/quota")
    suspend fun aiQuota(): JsonObject

    @POST("ai/chat")
    suspend fun aiChat(@Body body: JsonObject): JsonObject

    @POST("downloads/start")
    suspend fun startDownload(@Body body: JsonObject): JsonObject

    @POST("downloads/confirm")
    suspend fun confirmDownload(@Body body: JsonObject): JsonObject

    @POST("downloads/cancel")
    suspend fun cancelDownload(@Body body: JsonObject): JsonObject
}
