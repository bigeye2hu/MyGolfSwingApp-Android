package com.mygolfswing.app.data.repo

import android.os.Build
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.CancellationException
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
    ): Result<Unit> = try {
        val cloudResult = try {
            submitToCloud(type, content, contact)
            null
        } catch (e: Throwable) {
            feedbackSubmitFailure<Unit>(e).exceptionOrNull()
        }
        if (cloudResult == null) {
            Result.success(Unit)
        } else {
            submitToLegacy(type, content, contact, cloudResult)
            Result.success(Unit)
        }
    } catch (e: Throwable) {
        feedbackSubmitFailure(e)
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
            val failure = legacyFeedbackFailureMessage(
                isHttpSuccessful = response.isSuccessful,
                httpMessage = response.message,
                parsed = parsed,
                cloudError = cloudError,
            )
            if (failure != null) {
                error(failure)
            }
        }
    }

    private companion object {
        const val LEGACY_FEEDBACK_URL = "https://download.mygolfai.com.cn/api/feedback"
    }
}

internal fun <T> feedbackSubmitFailure(error: Throwable): Result<T> {
    if (error is CancellationException) throw error
    return Result.failure(error)
}

internal fun legacyFeedbackFailureMessage(
    isHttpSuccessful: Boolean,
    httpMessage: String,
    parsed: JsonElement?,
    cloudError: Throwable?,
): String? {
    if (parsed.explicitFeedbackServerSuccess() || isHttpSuccessful && parsed.explicitFeedbackServerError() == null) {
        return null
    }
    val legacyMessage = parsed.explicitFeedbackServerError()
        ?: httpMessage.takeIf { it.isNotBlank() }
        ?: "备用反馈通道提交失败"
    val cloudMessage = cloudError?.message?.takeIf { it.isNotBlank() }
    return if (cloudMessage == null) legacyMessage else "$cloudMessage；备用通道：$legacyMessage"
}

private fun JsonElement?.explicitFeedbackServerSuccess(): Boolean {
    val obj = this as? JsonObject ?: return false
    return obj["success"]?.jsonPrimitive?.booleanOrNull == true
}

private fun JsonElement?.explicitFeedbackServerError(): String? {
    val obj = this as? JsonObject ?: return null
    if (obj["success"]?.jsonPrimitive?.booleanOrNull == false) {
        return obj["error"]?.jsonPrimitive?.contentOrNull
            ?: obj["message"]?.jsonPrimitive?.contentOrNull
            ?: "备用反馈通道返回失败"
    }
    return obj["error"]?.jsonPrimitive?.contentOrNull
        ?: obj["message"]?.jsonPrimitive?.contentOrNull
}
