package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.db.GolfProfileDao
import com.mygolfswing.app.data.db.GolfProfileEntity
import com.mygolfswing.app.data.model.V2GolfProfile
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class V2GolfProfileRepository(
    private val dao: GolfProfileDao,
    private val net: V2NetClient,
    private val session: V2SessionStore,
) {
    suspend fun getLocal(): V2GolfProfile? = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext null
        dao.get(uid)?.let { decode(it) }
    }

    suspend fun pullFromCloud(): Result<V2GolfProfile?> = runCatching {
        val uid = session.current().userId ?: error("未登录")
        val resp = net.cloudApi.dataSync(
            JsonObject(mapOf("action" to JsonPrimitive("getUserGolfProfile"), "userId" to JsonPrimitive(uid))),
        )
        if (!resp.success) error(resp.error ?: "拉取训练档案失败")
        val data = when (val el = resp.data) {
            is kotlinx.serialization.json.JsonObject -> el
            else -> return@runCatching getLocal()
        }
        val profile = V2GolfProfile(
            userId = uid,
            focusAreas = data["focusAreas"]?.jsonPrimitive?.contentOrNull ?: "",
            painPoints = data["painPoints"]?.jsonPrimitive?.contentOrNull ?: "",
            strengths = data["strengths"]?.jsonPrimitive?.contentOrNull ?: "",
            trainingGoals = data["trainingGoals"]?.jsonPrimitive?.contentOrNull ?: "",
            coachGuidance = data["coachGuidance"]?.jsonPrimitive?.contentOrNull ?: "",
        )
        withContext(Dispatchers.IO) {
            dao.upsert(
                GolfProfileEntity(
                    userId = uid,
                    profileJson = net.json.encodeToString(JsonObject.serializer(), encodeJson(profile)),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
        profile
    }

    private fun encodeJson(p: V2GolfProfile) = JsonObject(
        mapOf(
            "focusAreas" to JsonPrimitive(p.focusAreas),
            "painPoints" to JsonPrimitive(p.painPoints),
            "strengths" to JsonPrimitive(p.strengths),
            "trainingGoals" to JsonPrimitive(p.trainingGoals),
            "coachGuidance" to JsonPrimitive(p.coachGuidance),
        ),
    )

    private fun decode(row: GolfProfileEntity): V2GolfProfile {
        val o = net.json.parseToJsonElement(row.profileJson).jsonObject
        return V2GolfProfile(
            userId = row.userId,
            focusAreas = o["focusAreas"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            painPoints = o["painPoints"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            strengths = o["strengths"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            trainingGoals = o["trainingGoals"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            coachGuidance = o["coachGuidance"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        )
    }
}
