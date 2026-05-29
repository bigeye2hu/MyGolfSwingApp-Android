package com.mygolfswing.app.data.ai

import com.mygolfswing.app.data.net.V2ApiConfig
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
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

    data class ModelOption(val id: String, val displayName: String, val trait: String)

    enum class ProviderId(
        val id: String,
        val displayName: String,
        val defaultBase: String?,
        val defaultModel: String,
        val description: String,
        val keyHint: String,
        val models: List<ModelOption>,
    ) {
        BUILTIN(
            "builtin",
            "挥呗 AI",
            V2ApiConfig.BUILTIN_AI_BASE,
            "qwen-plus",
            "内置免费模型，每天30条",
            "无需配置，开箱即用",
            listOf(ModelOption("qwen-plus", "Qwen Plus", "均衡推荐")),
        ),
        DASHSCOPE(
            "dashscope",
            "通义千问",
            "https://dashscope.aliyuncs.com/compatible-mode/v1/",
            "qwen-plus",
            "阿里云通义千问，国内推荐",
            "阿里云控制台 → 百炼平台 → API 密钥管理",
            listOf(
                ModelOption("qwen-turbo", "Qwen Turbo", "快速响应，适合简单问答"),
                ModelOption("qwen-plus", "Qwen Plus", "均衡推荐，性价比高"),
                ModelOption("qwen-max", "Qwen Max", "最强能力，复杂任务"),
            ),
        ),
        DEEPSEEK(
            "deepseek",
            "DeepSeek",
            "https://api.deepseek.com/v1/",
            "deepseek-chat",
            "性价比高，推理能力强",
            "platform.deepseek.com → API Keys",
            listOf(
                ModelOption("deepseek-chat", "DeepSeek Chat", "通用对话，速度快"),
                ModelOption("deepseek-reasoner", "DeepSeek Reasoner", "深度推理，逻辑强"),
            ),
        ),
        MOONSHOT(
            "moonshot",
            "Moonshot",
            "https://api.moonshot.cn/v1/",
            "moonshot-v1-8k",
            "月之暗面 Kimi，长文本理解强",
            "platform.moonshot.cn → API Key 管理",
            listOf(
                ModelOption("moonshot-v1-8k", "Kimi 8K", "8K 上下文"),
                ModelOption("moonshot-v1-32k", "Kimi 32K", "32K 上下文"),
                ModelOption("moonshot-v1-128k", "Kimi 128K", "128K 超长上下文"),
            ),
        ),
        MINIMAX(
            "minimax",
            "MiniMax",
            "https://api.minimaxi.com/v1/",
            "MiniMax-M2.5-highspeed",
            "海螺AI，长上下文能力强",
            "platform.minimaxi.com → 接口密钥 → 创建 API Key",
            listOf(
                ModelOption("MiniMax-M2.5-highspeed", "M2.5 极速", "快速响应"),
                ModelOption("MiniMax-M2.5", "M2.5 标准", "深度推理，较慢"),
            ),
        ),
        OPENAI(
            "openai",
            "OpenAI",
            "https://api.openai.com/v1/",
            "gpt-4o-mini",
            "需要科学上网",
            "platform.openai.com → API Keys（需科学上网）",
            listOf(
                ModelOption("gpt-4o-mini", "GPT-4o Mini", "轻量快速，性价比高"),
                ModelOption("gpt-4o", "GPT-4o", "旗舰模型，能力最强"),
                ModelOption("gpt-3.5-turbo", "GPT-3.5 Turbo", "经典模型，速度快"),
            ),
        ),
        CUSTOM(
            "custom",
            "自定义",
            null,
            "",
            "任意 OpenAI 兼容接口",
            "请参考对应平台文档获取 API Key",
            emptyList(),
        );

        fun modelOrDefault(model: String?): String {
            val trimmed = model?.takeIf { it.isNotBlank() }
            if (this == CUSTOM) return trimmed ?: defaultModel
            return models.firstOrNull { it.id == trimmed }?.id ?: defaultModel
        }

        companion object {
            fun from(id: String?): ProviderId = entries.firstOrNull { it.id == id } ?: BUILTIN
        }
    }

    suspend fun currentProvider(): ProviderId =
        ProviderId.from(session.aiProviderId.first())

    suspend fun currentModel(): String {
        val id = currentProvider()
        return id.modelOrDefault(session.aiModel.first())
    }

    private suspend fun currentRequestModel(providerId: ProviderId): String {
        return providerId.modelOrDefault(session.aiModel.first())
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
        val model = currentRequestModel(providerId)

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
            buildMap {
                put("model", JsonPrimitive(model))
                put("messages", messages)
                put("stream", JsonPrimitive(true))
                put(
                    "stream_options",
                    JsonObject(mapOf("include_usage" to JsonPrimitive(true))),
                )
            },
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
            val errorBody = response.body?.string().orEmpty()
            response.close()
            error("AI 请求失败：HTTP ${response.code}${errorBody.takeIf { it.isNotBlank() }?.let { " - ${it.take(120)}" } ?: ""}")
        }
        val source = response.body?.source() ?: error("AI 响应体为空")
        val rawPayloads = mutableListOf<String>()
        val rawBody = StringBuilder()
        var emittedAny = false
        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank()) continue
                if (!line.startsWith("data:")) {
                    rawPayloads += line
                    rawBody.append(line)
                    continue
                }
                val payload = line.removePrefix("data:").trim()
                if (payload == "[DONE]") break
                rawPayloads += payload
                rawBody.append(payload)
                val chunk = parseChunk(payload) ?: continue
                if (chunk.isNotEmpty()) {
                    emittedAny = true
                    emit(chunk)
                }
            }
            if (!emittedAny) {
                val fallback = sequenceOf(rawBody.toString())
                    .plus(rawPayloads.asSequence())
                    .mapNotNull { parseChunk(it) ?: parseFullContent(it) ?: parsePlainContent(it) }
                    .firstOrNull { it.isNotBlank() }
                if (fallback != null) {
                    emit(fallback)
                } else {
                    val raw = rawBody.toString().ifBlank { rawPayloads.joinToString("\n") }
                    if (raw.isBlank()) {
                        error("AI 响应为空：服务器返回 200 但没有内容")
                    } else {
                        error("AI 响应无可显示内容：${raw.take(240)}")
                    }
                }
            }
        } finally {
            response.close()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 非流式单次请求（对齐 iOS `AIService.generateSummary`），用于档案 AI 反射等。
     */
    suspend fun generateSummary(prompt: String): String = withContext(Dispatchers.IO) {
        val providerId = ProviderId.from(session.aiProviderId.first())
        val apiKey = session.aiApiKey.first()
        if (providerId != ProviderId.BUILTIN && apiKey.isNullOrBlank()) {
            error("请先配置 AI API Key")
        }
        val baseUrl = session.aiBaseUrl.first()?.takeIf { it.isNotBlank() } ?: providerId.defaultBase
        val model = currentRequestModel(providerId)

        val messages = JsonArray(
            listOf(
                JsonObject(
                    mapOf(
                        "role" to JsonPrimitive("system"),
                        "content" to JsonPrimitive("你是一个专业的对话摘要助手，擅长提取关键信息。"),
                    ),
                ),
                JsonObject(
                    mapOf(
                        "role" to JsonPrimitive("user"),
                        "content" to JsonPrimitive(prompt),
                    ),
                ),
            ),
        )
        val body = JsonObject(
            buildMap {
                put("model", JsonPrimitive(model))
                put("messages", messages)
                put("stream", JsonPrimitive(false))
                put("stream_options", JsonNull)
            },
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
        if (providerId == ProviderId.BUILTIN) {
            s.userId?.let { builder.header("X-TC-UID", it) }
            s.userSig?.let { builder.header("X-TC-UserSig", it) }
            builder.header("X-Internal", "true")
        } else {
            apiKey?.takeIf { it.isNotBlank() }?.let { builder.header("Authorization", "Bearer $it") }
        }
        val response = net.httpClient.newCall(builder.build()).execute()
        response.use { resp ->
            if (!resp.isSuccessful) error("AI 请求失败：HTTP ${resp.code}")
            val payload = resp.body?.string() ?: error("AI 响应体为空")
            parseFullContent(payload) ?: error("AI 响应无内容")
        }
    }

    private fun parseFullContent(payload: String): String? = runCatching {
        val obj = net.json.parseToJsonElement(payload).jsonObject
        val ok = obj["ok"]?.jsonPrimitive?.booleanOrNullSafe()
        if (ok == false) {
            firstPrimitiveString(obj, "error", "message", "detail")?.let { return@runCatching "（生成失败：$it）" }
        }
        firstPrimitiveString(obj, "content", "text", "answer", "reply", "response", "result", "output")?.let { return@runCatching it }
        firstPrimitiveString(obj, "message")?.let { msg ->
            if (msg.isNotBlank() && !msg.equals("ok", ignoreCase = true)) return@runCatching msg
        }

        obj["data"]?.let { data ->
            when (data) {
                is JsonPrimitive -> data.contentOrNull?.takeIf { it.isNotBlank() }?.let { return@runCatching it }
                is JsonObject -> {
                    firstPrimitiveString(data, "content", "text", "answer", "reply", "response", "message", "result", "output")
                        ?.let { return@runCatching it }
                    parseChoices(data)?.let { return@runCatching it }
                }
                else -> Unit
            }
        }

        val choices = obj["choices"]?.jsonArray ?: return@runCatching null
        val first = choices.firstOrNull()?.jsonObject ?: return@runCatching null
        val message = first["message"]?.jsonObject ?: first["delta"]?.jsonObject
        message?.get("content")?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    private fun parseChunk(payload: String): String? = runCatching {
        val obj = net.json.parseToJsonElement(payload).jsonObject
        val ok = obj["ok"]?.jsonPrimitive?.booleanOrNullSafe()
        if (ok == false) {
            firstPrimitiveString(obj, "error", "message", "detail")?.let { return@runCatching "（生成失败：$it）" }
        }
        firstPrimitiveString(obj, "delta", "content", "text", "answer", "reply", "response", "result", "output")?.let { return@runCatching it }
        parseChoices(obj)
    }.getOrNull()

    private fun parseChoices(obj: JsonObject): String? {
        val choices = obj["choices"]?.jsonArray ?: return null
        val first = choices.firstOrNull()?.jsonObject ?: return null
        val delta = first["delta"] as? JsonObject
        val message = first["message"] as? JsonObject
        firstPrimitiveString(delta, "content")?.let { return it }
        firstPrimitiveString(message, "content")?.let { return it }
        return firstPrimitiveString(first, "text", "content")
    }

    private fun parsePlainContent(payload: String): String? {
        val trimmed = payload.trim()
        if (trimmed.isBlank() || trimmed.startsWith("{") || trimmed.startsWith("[")) return null
        return trimmed
    }

    private fun firstPrimitiveString(obj: JsonObject?, vararg keys: String): String? {
        if (obj == null) return null
        for (key in keys) {
            val value: JsonElement = obj[key] ?: continue
            val text = (value as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            if (text != null) return text
        }
        return null
    }

    private fun JsonPrimitive.booleanOrNullSafe(): Boolean? =
        runCatching { boolean }.getOrNull()
}
