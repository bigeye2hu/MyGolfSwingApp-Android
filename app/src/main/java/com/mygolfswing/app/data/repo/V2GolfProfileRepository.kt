package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.db.GolfProfileDao
import com.mygolfswing.app.data.db.GolfProfileEntity
import com.mygolfswing.app.data.model.V2GolfProfileData
import com.mygolfswing.app.data.model.V2ProfileItem
import com.mygolfswing.app.data.model.V2ProfileSource
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * AI 训练档案仓库（对齐 iOS `UserGolfProfileCloudSyncService`）。
 *
 * - 本地 Room 存 `profileDataRaw` 等价 JSON（[GolfProfileEntity.profileJson]）
 * - 云端 `getUserGolfProfile` / `saveUserGolfProfile`，Last-Write-Wins
 */
class V2GolfProfileRepository(
    private val dao: GolfProfileDao,
    private val net: V2NetClient,
    private val session: V2SessionStore,
) {
    suspend fun getLocal(): V2GolfProfileData? = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext null
        dao.get(uid)?.let { decodeProfileJson(it.profileJson) }
    }

    suspend fun getLocalUpdatedAt(): Long? = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext null
        dao.get(uid)?.updatedAt
    }

    suspend fun saveLocal(data: V2GolfProfileData, updatedAt: Long = System.currentTimeMillis()) {
        val uid = session.current().userId ?: return
        withContext(Dispatchers.IO) {
            dao.upsert(
                GolfProfileEntity(
                    userId = uid,
                    profileJson = encodeProfileJson(data),
                    updatedAt = updatedAt,
                ),
            )
        }
    }

    suspend fun pullFromCloud(): Result<V2GolfProfileData?> = try {
        Result.success(pullFromCloudOrThrow())
    } catch (e: Throwable) {
        golfProfileCloudFailureResult(e)
    }

    private suspend fun pullFromCloudOrThrow(): V2GolfProfileData? {
        val uid = session.current().userId ?: error("未登录")
        val resp = net.cloudApi.dataSync(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive("getUserGolfProfile"),
                    "userId" to JsonPrimitive(uid),
                ),
            ),
        )
        if (!resp.success) error(resp.error ?: "拉取训练档案失败")

        val data = when (val el = resp.data) {
            is JsonObject -> el
            else -> return getLocal()
        }

        val raw = data["profileDataRaw"]?.jsonPrimitive?.contentOrNull
        if (raw.isNullOrBlank()) {
            uploadLocalIfNonEmpty(uid, reason = "云端无记录")
            return getLocal()
        }

        val cloudData = decodeProfileJson(raw)
        val cloudUpdatedAt = parseTimestampMs(data["updatedAt"]) ?: 0L
        val localRow = withContext(Dispatchers.IO) { dao.get(uid) }

        return if (localRow == null) {
            withContext(Dispatchers.IO) {
                dao.upsert(
                    GolfProfileEntity(
                        userId = uid,
                        profileJson = encodeProfileJson(cloudData),
                        updatedAt = cloudUpdatedAt.coerceAtLeast(System.currentTimeMillis()),
                    ),
                )
            }
            cloudData
        } else if (cloudUpdatedAt > localRow.updatedAt) {
            withContext(Dispatchers.IO) {
                dao.upsert(
                    localRow.copy(
                        profileJson = encodeProfileJson(cloudData),
                        updatedAt = cloudUpdatedAt,
                    ),
                )
            }
            cloudData
        } else {
            decodeProfileJson(localRow.profileJson)
        }
    }

    suspend fun pushToCloud(data: V2GolfProfileData, updatedAt: Long = System.currentTimeMillis()) {
        val uid = session.current().userId ?: return
        val raw = encodeProfileJson(data)
        if (raw.isBlank() || data.isEmpty) return
        val resp = net.cloudApi.dataSync(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive("saveUserGolfProfile"),
                    "userId" to JsonPrimitive(uid),
                    "profileDataRaw" to JsonPrimitive(raw),
                    "updatedAt" to JsonPrimitive(updatedAt.toDouble()),
                ),
            ),
        )
        if (!resp.success) error(resp.error ?: "上传训练档案失败")
    }

    /** 保存本地并异步上传（对齐 iOS `saveProfile`） */
    suspend fun saveAndSync(data: V2GolfProfileData) {
        val updatedAt = System.currentTimeMillis()
        saveLocal(data, updatedAt)
        runGolfProfileSideEffectCatching { pushToCloud(data, updatedAt) }
    }

    private suspend fun uploadLocalIfNonEmpty(userId: String, reason: String) {
        val local = withContext(Dispatchers.IO) { dao.get(userId) } ?: return
        val data = decodeProfileJson(local.profileJson)
        if (data.isEmpty) return
        runGolfProfileSideEffectCatching { pushToCloud(data, local.updatedAt) }
    }

    private fun encodeProfileJson(data: V2GolfProfileData): String =
        net.json.encodeToString(V2GolfProfileData.serializer(), data)

    private fun decodeProfileJson(json: String): V2GolfProfileData = runCatching {
        val el = net.json.parseToJsonElement(json).jsonObject
        when {
            el.containsKey("currentFocus") || el.containsKey("painPoints") ->
                net.json.decodeFromString(V2GolfProfileData.serializer(), json)
            el.containsKey("focusAreas") -> migrateLegacyFlat(el)
            else -> V2GolfProfileData()
        }
    }.getOrDefault(V2GolfProfileData())

    /** 旧版 Android 扁平字段 → 结构化档案（一次性迁移） */
    private fun migrateLegacyFlat(o: JsonObject): V2GolfProfileData {
        fun lines(key: String): List<V2ProfileItem> =
            o[key]?.jsonPrimitive?.contentOrNull
                ?.split("\n", "、", "；", ";")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.map { V2ProfileItem(content = it, source = V2ProfileSource.USER_MANUAL.wire) }
                .orEmpty()

        return V2GolfProfileData(
            currentFocus = lines("focusAreas"),
            painPoints = lines("painPoints"),
            strengths = lines("strengths"),
            trainingGoals = o["trainingGoals"]?.jsonPrimitive?.contentOrNull
                ?.split("\n")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
            coachNotes = o["coachGuidance"]?.jsonPrimitive?.contentOrNull
                ?.split("\n")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
        )
    }

    private fun parseTimestampMs(value: kotlinx.serialization.json.JsonElement?): Long? {
        if (value == null) return null
        value.jsonPrimitive.longOrNull?.let { return it }
        value.jsonPrimitive.doubleOrNull?.let { return it.toLong() }
        val str = value.jsonPrimitive.contentOrNull ?: return null
        str.toLongOrNull()?.let { return it }
        return runCatching { java.time.Instant.parse(str).toEpochMilli() }.getOrNull()
    }
}

internal fun <T> golfProfileCloudFailureResult(error: Throwable): Result<T> {
    if (error is CancellationException) throw error
    return Result.failure(error)
}

internal suspend fun runGolfProfileSideEffectCatching(block: suspend () -> Unit): Result<Unit> = try {
    block()
    Result.success(Unit)
} catch (e: Throwable) {
    if (e is CancellationException) throw e
    Result.failure(e)
}
