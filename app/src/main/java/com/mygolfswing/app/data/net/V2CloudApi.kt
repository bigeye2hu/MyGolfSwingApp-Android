package com.mygolfswing.app.data.net

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 腾讯 CloudBase 云函数（RPC 风格）。
 *
 * 所有函数都是 `POST /{functionName}`，body 是一个 JsonObject，必带 `action` 字段。
 */
interface V2CloudApi {

    @POST("userAuth")
    suspend fun userAuth(@Body body: JsonObject): CloudResponse

    @POST("dataSync")
    suspend fun dataSync(@Body body: JsonObject): DataSyncResponse

    @POST("videoPlaza")
    suspend fun videoPlaza(@Body body: JsonObject): JsonObject

    @POST("moments")
    suspend fun moments(@Body body: JsonObject): JsonObject

    @POST("accountManagement")
    suspend fun accountManagement(@Body body: JsonObject): CloudResponse

    @POST("coachWorkspace")
    suspend fun coachWorkspace(@Body body: JsonObject): JsonObject
}

@kotlinx.serialization.Serializable
data class CloudResponse(
    val success: Boolean = false,
    val code: String? = null,
    val error: String? = null,
    val data: JsonObject? = null,
)

@kotlinx.serialization.Serializable
data class DataSyncResponse(
    val success: Boolean = false,
    val error: String? = null,
    val data: JsonElement? = null,
)
