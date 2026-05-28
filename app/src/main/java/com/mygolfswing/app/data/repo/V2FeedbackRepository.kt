package com.mygolfswing.app.data.repo

import android.os.Build
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** 意见反馈（对齐 iOS `FeedbackView` → `accountManagement.submitFeedback`） */
class V2FeedbackRepository(
    private val net: V2NetClient,
    private val session: V2SessionStore,
) {
    suspend fun submit(
        type: String,
        content: String,
        contact: String?,
    ): Result<Unit> = runCatching {
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
}
