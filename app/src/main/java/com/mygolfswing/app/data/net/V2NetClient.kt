package com.mygolfswing.app.data.net

import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 全局网络客户端。
 *
 * - **CloudBase 云函数**走 [cloudApi]：所有 POST 都带 `action` 字段，body 用 JsonObject
 * - **Download 服务器**走 [downloadApi]：自动注入 `X-TC-UID / X-TC-UserSig` 头
 * - **AI 流式**单独用 [streamingClient]（无读超时，便于 SSE 长连接）
 */
class V2NetClient(private val session: V2SessionStore) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val tcAuthInterceptor = Interceptor { chain ->
        val s = runBlocking { session.current() }
        val builder = chain.request().newBuilder()
        s.userId?.let { builder.header("X-TC-UID", it) }
        s.userSig?.let { builder.header("X-TC-UserSig", it) }
        chain.proceed(builder.build())
    }

    private val baseClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    private val authedClient: OkHttpClient = baseClient.newBuilder()
        .addInterceptor(tcAuthInterceptor)
        .build()

    /** SSE / 流式：禁用读超时 */
    val streamingClient: OkHttpClient = baseClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .addInterceptor(tcAuthInterceptor)
        .build()

    @OptIn(ExperimentalSerializationApi::class)
    private val converter = json.asConverterFactory("application/json".toMediaType())

    val cloudApi: V2CloudApi = Retrofit.Builder()
        .baseUrl(V2ApiConfig.CLOUD_BASE_HOST)
        .client(authedClient)
        .addConverterFactory(converter)
        .build()
        .create(V2CloudApi::class.java)

    val downloadApi: V2DownloadApi = Retrofit.Builder()
        .baseUrl(V2ApiConfig.DOWNLOAD_SERVER_BASE)
        .client(authedClient)
        .addConverterFactory(converter)
        .build()
        .create(V2DownloadApi::class.java)

    /** 当前用户 session 快照 */
    suspend fun currentSession() = session.current()

    /** 暴露给上层捕获响应 ok 字段的 helper */
    fun unwrap(response: Response): String? = response.body?.string()
}
