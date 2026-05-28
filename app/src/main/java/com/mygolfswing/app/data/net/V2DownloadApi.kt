package com.mygolfswing.app.data.net

import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * download.mygolfai.com.cn 上的 REST 接口（积分 / 下载任务 / AI 透传）。
 *
 * 当前 Android 端实际只用到了 `credits/balance` 与 `ai/quota`，下载流程暂未接入。
 */
interface V2DownloadApi {

    @GET("credits/balance")
    suspend fun creditsBalance(): JsonObject

    @GET("ai/quota")
    suspend fun aiQuota(): JsonObject

    @POST("ai/chat")
    suspend fun aiChat(@Body body: JsonObject): JsonObject
}
