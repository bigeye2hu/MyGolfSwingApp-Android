package com.mygolfswing.app.data.ai

import com.mygolfswing.app.data.net.V2ApiConfig
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 真实 AI 接入（对齐 iOS `AIService`）。
 *
 * Provider 切换逻辑：
 * - `builtin`（默认）→ `POST {downloadServer}/ai/chat`，带 `X-TC-UID / X-TC-UserSig`
 * - 其它（openai / deepseek / dashscope / kimi / minimax / custom）→ `POST {baseUrl}/chat/completions`，
 *   Header 加 `Authorization: Bearer {apiKey}`
 *
 * 流式：标准 OpenAI SSE，按行解析 `data: {...}`，至 `data: [DONE]` 结束。
 */
class V2AIService(
    private val net: V2NetClient,
    private val session: V2SessionStore,
) {

    enum class ProviderId(val id: String, val displayName: String, val defaultBase: String?, val defaultModel: String) {
        BUILTIN("builtin", "挥呗 AI", V2ApiConfig.BUILTIN_AI_BASE, "default"),
        DASHSCOPE("dashscope", "通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1/", "qwen-plus"),
        DEEPSEEK("deepseek", "DeepSeek", "https://api.deepseek.com/v1/", "deepseek-chat"),
        MOONSHOT("moonshot", "Kimi", "https://api.moonshot.cn/v1/", "moonshot-v1-8k"),
        MINIMAX("minimax", "MiniMax", "https://api.minimaxi.com/v1/", "abab6.5s-chat"),
        OPENAI("openai", "OpenAI", "https://api.openai.com/v1/", "gpt-4o-mini"),
        CUSTOM("custom", "自定义", null, "default");

        companion object {
            fun from(id: String?): ProviderId = entries.firstOrNull { it.id == id } ?: BUILTIN
        }
    }

    suspend fun currentProvider(): ProviderId =
        ProviderId.from(session.aiProviderId.first())

    suspend fun currentModel(): String {
        val id = currentProvider()
        return session.aiModel.first()?.takeIf { it.isNotBlank() } ?: id.defaultModel
    }

    /**
     * 发起一次流式 chat。
     *
     * @param history 历史消息（role, content）。最末一条通常是当前 user prompt
     * @return 流式 Flow<String>，每次 emit 一段增量文本
     */
    fun streamChat(history: List<Pair<String, String>>): Flow<String> = flow {
        val providerId = ProviderId.from(session.aiProviderId.first())
        val apiKey = session.aiApiKey.first()
        val baseUrl = session.aiBaseUrl.first()?.takeIf { it.isNotBlank() } ?: providerId.defaultBase
        val model = session.aiModel.first()?.takeIf { it.isNotBlank() } ?: providerId.defaultModel

        val messages = JsonArray(
            history.map { (role, content) ->
                JsonObject(
                    mapOf(
                        "role" to JsonPrimitive(role),
                        "content" to JsonPrimitive(content),
                    ),
                )
            },
        )

        val body = JsonObject(
            mapOf(
                "model" to JsonPrimitive(model),
                "messages" to messages,
                "stream" to JsonPrimitive(true),
            ),
        )

        val url = when (providerId) {
            ProviderId.BUILTIN -> "${baseUrl?.trimEnd('/')}/chat"
            else -> "${baseUrl?.trimEnd('/')}/chat/completions"
        }

        val s = session.current()
        val builder = Request.Builder()
            .url(url)
            .post(
                net.json.encodeToString(JsonObject.serializer(), body)
                    .toRequestBody("application/json".toMediaType()),
            )
            .header("Accept", "text/event-stream")

        if (providerId == ProviderId.BUILTIN) {
            s.userId?.let { builder.header("X-TC-UID", it) }
            s.userSig?.let { builder.header("X-TC-UserSig", it) }
        } else {
            apiKey?.takeIf { it.isNotBlank() }?.let { builder.header("Authorization", "Bearer $it") }
        }

        val request = builder.build()
        val response = net.streamingClient.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            error("AI 请求失败：HTTP ${response.code}")
        }
        val source = response.body?.source() ?: error("AI 响应体为空")
        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank()) continue
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload == "[DONE]") break
                val chunk = parseChunk(payload) ?: continue
                if (chunk.isNotEmpty()) emit(chunk)
            }
        } finally {
            response.close()
        }
    }

    private fun parseChunk(payload: String): String? = runCatching {
        val obj = net.json.parseToJsonElement(payload).jsonObject
        val choices = obj["choices"]?.jsonArray ?: return@runCatching null
        val first = choices.firstOrNull()?.jsonObject ?: return@runCatching null
        val delta = first["delta"]?.jsonObject ?: first["message"]?.jsonObject
        delta?.get("content")?.jsonPrimitive?.contentOrNull
    }.getOrNull()
}
