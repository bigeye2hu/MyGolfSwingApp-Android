package com.mygolfswing.app.data.repo

import android.os.Build
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant

/** 意见反馈（对齐 iOS `FeedbackView` → `accountManagement.submitFeedback`，失败时兜 legacy API）。 */
class V2FeedbackRepository(
    private val net: V2NetClient,
    private val session: V2SessionStore,
) {
    suspend fun submit(
        type: String,
        content: String,
        contact: String?,
    ): Result<Unit> = runCatching {
        val cloudResult = runCatching { submitToCloud(type, content, contact) }
        if (cloudResult.isSuccess) return@runCatching
        submitToLegacy(type, content, contact, cloudResult.exceptionOrNull())
    }

    private suspend fun submitToCloud(
        type: String,
        content: String,
        contact: String?,
    ) {
        val uid = session.current().userId
        val resp = net.cloudApi.accountManagement(
            JsonObject(
                buildMap {
                    put("action", JsonPrimitive("submitFeedback"))
                    uid?.let { put("userId", JsonPrimitive(it)) }
                    put("type", JsonPrimitive(type))
                    put("content", JsonPrimitive(content.trim()))
                    contact?.takeIf { it.isNotBlank() }?.let { put("contact", JsonPrimitive(it)) }
                    put(
                        "deviceInfo",
                        JsonObject(
                            mapOf(
                                "platform" to JsonPrimitive("android"),
                                "model" to JsonPrimitive(Build.MODEL),
                                "sdk" to JsonPrimitive(Build.VERSION.SDK_INT),
                            ),
                        ),
                    )
                },
            ),
        )
        if (!resp.success) error(resp.error ?: "提交失败")
    }

    private suspend fun submitToLegacy(
        type: String,
        content: String,
        contact: String?,
        cloudError: Throwable?,
    ) {
        val uid = session.current().userId
        val body = JsonObject(
            buildMap {
                put("type", JsonPrimitive(type))
                put("content", JsonPrimitive(content.trim()))
                contact?.takeIf { it.isNotBlank() }?.let { put("contact", JsonPrimitive(it)) }
                uid?.let { put("user_id", JsonPrimitive(it)) }
                put("timestamp", JsonPrimitive(Instant.now().toString()))
                put(
                    "device_info",
                    JsonObject(
                        mapOf(
                            "platform" to JsonPrimitive("android"),
                            "sdk" to JsonPrimitive(Build.VERSION.SDK_INT),
                            "device_model" to JsonPrimitive(Build.MODEL),
                        ),
                    ),
                )
            },
        )
        val jsonBody = net.json
            .encodeToString(JsonObject.serializer(), body)
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(LEGACY_FEEDBACK_URL)
            .post(jsonBody)
            .header("Content-Type", "application/json")
            .build()
        net.httpClient.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            val parsed = responseText.takeIf { it.isNotBlank() }
                ?.let { runCatching { net.json.parseToJsonElement(it) }.getOrNull() }
            if (parsed.explicitServerSuccess() || response.isSuccessful && parsed.explicitServerError() == null) {
                return
            }
            val legacyMessage = parsed.explicitServerError()
                ?: response.message.takeIf { it.isNotBlank() }
                ?: "备用反馈通道提交失败"
            val cloudMessage = cloudError?.message?.takeIf { it.isNotBlank() }
            error(if (cloudMessage == null) legacyMessage else "$cloudMessage；备用通道：$legacyMessage")
        }
    }

    private fun JsonElement?.explicitServerSuccess(): Boolean {
        val obj = this as? JsonObject ?: return false
        return obj["success"]?.jsonPrimitive?.booleanOrNull == true
    }

    private fun JsonElement?.explicitServerError(): String? {
        val obj = this as? JsonObject ?: return null
        if (obj["success"]?.jsonPrimitive?.booleanOrNull == false) {
            return obj["error"]?.jsonPrimitive?.contentOrNull
                ?: obj["message"]?.jsonPrimitive?.contentOrNull
                ?: "备用反馈通道返回失败"
        }
        return obj["error"]?.jsonPrimitive?.contentOrNull
            ?: obj["message"]?.jsonPrimitive?.contentOrNull
    }

    private companion object {
        const val LEGACY_FEEDBACK_URL = "https://download.mygolfai.com.cn/api/feedback"
    }
}
