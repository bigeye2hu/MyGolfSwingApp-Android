package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.db.AIChatDao
import com.mygolfswing.app.data.db.GoalDao
import com.mygolfswing.app.data.db.MemoDao
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.Instant

internal fun parseAuditMillis(element: JsonElement?): Long? {
    if (element == null) return null
    return when (element) {
        is JsonObject -> parseAuditMillis(element["\$date"])
        else -> runCatching {
            val primitive = element.jsonPrimitive
            primitive.longOrNull
                ?: primitive.doubleOrNull?.toLong()
                ?: primitive.contentOrNull?.trim()?.takeIf { it.isNotBlank() }?.let { raw ->
                    raw.toLongOrNull() ?: Instant.parse(raw).toEpochMilli()
                }
        }.getOrNull()
    }
}

class V2ProfileCloudConsistencyAuditRepository(
    private val goalDao: GoalDao,
    private val memoDao: MemoDao,
    private val aiChatDao: AIChatDao,
    private val net: V2NetClient,
    private val session: V2SessionStore,
) {

    suspend fun runAudit(): List<V2ProfileCloudConsistencyResult> {
        val uid = session.current().userId ?: error("当前用户未登录，无法检查云端数据")
        return coroutineScope {
            val userInfo = async { auditUserInfo(uid) }
            val video = async { auditVideoMemos(uid) }
            val insights = async { auditInsightCards(uid) }
            val goals = async { auditV2Goals(uid) }
            val ai = async { auditAIConversations(uid) }
            listOf(userInfo.await(), video.await(), insights.await(), goals.await(), ai.await())
        }
    }

    private suspend fun auditUserInfo(uid: String): V2ProfileCloudConsistencyResult {
        val local = session.current()
        val cloud = userInfo(uid)
        val fields = listOf(
            UserInfoField("nickname", local.nickname, firstString(cloud, "nickname", "nickName")),
            UserInfoField("phone", local.phone, firstString(cloud, "phone")),
            UserInfoField("handicap", local.handicap, firstString(cloud, "handicap")),
            UserInfoField("golfAge", local.golfAge?.toString(), firstString(cloud, "golfAge", "golf_age")),
            UserInfoField("golfStartYear", local.golfStartYear?.toString(), firstString(cloud, "golfStartYear", "golf_start_year")),
            UserInfoField("dominantHand", local.dominantHand, firstString(cloud, "dominantHand", "dominant_hand")),
            UserInfoField("homeCourse", local.homeCourse, firstString(cloud, "homeCourse", "home_course")),
            UserInfoField("golfBrands", local.golfBrands.joinToString(", ").takeIf { it.isNotBlank() }, stringList(cloud, "golfBrands", "golf_brands").joinToString(", ").takeIf { it.isNotBlank() }),
        )
        val missingCloud = fields.filter { it.cloud.isNullOrBlank() }
        val different = fields.filter { !it.cloud.isNullOrBlank() && normalize(it.local) != normalize(it.cloud) }
        val issues = buildList {
            add(
                V2ProfileCloudConsistencyIssue(
                    title = "云端 getUserInfo 字段",
                    detail = fields.joinToString("；") { "${it.name}: 云端=${it.cloud ?: "空"} / 本机=${it.local ?: "空"}" },
                    tone = V2ProfileCloudConsistencyIssueTone.INFO,
                ),
            )
            if (missingCloud.isNotEmpty()) {
                add(
                    V2ProfileCloudConsistencyIssue(
                        title = "云端缺少 ${missingCloud.size} 个用户资料字段",
                        detail = missingCloud.joinToString("、") { it.name },
                        tone = V2ProfileCloudConsistencyIssueTone.WARNING,
                    ),
                )
            }
            if (different.isNotEmpty()) {
                add(
                    V2ProfileCloudConsistencyIssue(
                        title = "本机与云端不一致 ${different.size} 个字段",
                        detail = different.joinToString("；") { "${it.name}: 云端=${it.cloud} / 本机=${it.local ?: "空"}" },
                        tone = V2ProfileCloudConsistencyIssueTone.DANGER,
                    ),
                )
            }
        }
        return V2ProfileCloudConsistencyResult(
            name = "用户资料",
            localCount = fields.count { !it.local.isNullOrBlank() },
            cloudActiveCount = fields.count { !it.cloud.isNullOrBlank() },
            cloudDeletedCount = 0,
            localDuplicateCount = 0,
            cloudDuplicateCount = 0,
            issues = issues,
        )
    }

    private suspend fun auditVideoMemos(uid: String): V2ProfileCloudConsistencyResult {
        val local = withContext(Dispatchers.IO) {
            memoDao.getAll(uid)
                .filterNot { it.kind.equals("text", ignoreCase = true) }
                .map {
                    RecordSnapshot(
                        rawId = it.id,
                        title = localMemoTitle(it.memoJson, fallback = "视频 Memo"),
                        updatedAt = it.updatedAt,
                        isDeleted = false,
                    )
                }
        }
        val cloud = dataSyncRows("getVideoMemos", uid).mapNotNull { row ->
            val id = row["memoId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            RecordSnapshot(
                rawId = id,
                title = firstString(row, "title", "displayTitle") ?: "(无标题视频 memo)",
                updatedAt = parseAuditMillis(row["lastModifiedDate"]) ?: parseAuditMillis(row["updatedAt"]),
                isDeleted = row["isDeleted"]?.jsonPrimitive?.booleanOrNull == true,
            )
        }
        return makeResult(name = "视频 Memo", local = local, cloud = cloud)
    }

    private suspend fun auditInsightCards(uid: String): V2ProfileCloudConsistencyResult {
        val local = withContext(Dispatchers.IO) {
            memoDao.getAll(uid)
                .filter { it.kind.equals("text", ignoreCase = true) }
                .map {
                    RecordSnapshot(
                        rawId = it.id,
                        title = localMemoTitle(it.memoJson, fallback = "心得卡片"),
                        updatedAt = it.updatedAt,
                        isDeleted = false,
                    )
                }
        }
        val cloud = dataSyncRows("getInsightCards", uid).mapNotNull { row ->
            val id = row["cardId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            RecordSnapshot(
                rawId = id,
                title = firstString(row, "title") ?: "(无标题心得)",
                updatedAt = parseAuditMillis(row["cardUpdatedAt"]) ?: parseAuditMillis(row["updatedAt"]),
                isDeleted = row["isDeleted"]?.jsonPrimitive?.booleanOrNull == true,
            )
        }
        return makeResult(name = "心得卡片", local = local, cloud = cloud)
    }

    private suspend fun auditV2Goals(uid: String): V2ProfileCloudConsistencyResult {
        val local = withContext(Dispatchers.IO) {
            goalDao.all(uid).map {
                RecordSnapshot(
                    rawId = it.id,
                    title = localGoalTitle(it.goalJson),
                    updatedAt = it.updatedAt,
                    isDeleted = false,
                )
            }
        }
        val cloud = dataSyncRows("getV2Goals", uid).mapNotNull { row ->
            val id = row["goalId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            RecordSnapshot(
                rawId = id,
                title = cloudGoalTitle(row),
                updatedAt = parseAuditMillis(row["updatedAt"]),
                isDeleted = row["isDeleted"]?.jsonPrimitive?.booleanOrNull == true,
            )
        }
        return makeResult(name = "V2 目标", local = local, cloud = cloud)
    }

    private suspend fun auditAIConversations(uid: String): V2ProfileCloudConsistencyResult {
        val local = withContext(Dispatchers.IO) {
            aiChatDao.listSessions(uid).map {
                RecordSnapshot(
                    rawId = it.id,
                    title = it.title,
                    updatedAt = it.updatedAt,
                    isDeleted = false,
                )
            }
        }
        val cloudById = linkedMapOf<String, RecordSnapshot>()
        val deletedIds = mutableSetOf<String>()
        dataSyncRows("getAIConversationChunks", uid).forEach { row ->
            val id = row["conversationId"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            if (row["isDeleted"]?.jsonPrimitive?.booleanOrNull == true) {
                deletedIds += id.lowercase()
                cloudById[id.lowercase()] = RecordSnapshot(
                    rawId = id,
                    title = "(已删除对话)",
                    updatedAt = parseAuditMillis(row["deletedAt"]) ?: parseAuditMillis(row["updatedAt"]),
                    isDeleted = true,
                )
                return@forEach
            }
            if (id.lowercase() in deletedIds) return@forEach
            val snapshot = RecordSnapshot(
                rawId = id,
                title = firstString(row, "title") ?: "(对话分片)",
                updatedAt = parseAuditMillis(row["conversationUpdatedAt"]) ?: parseAuditMillis(row["updatedAt"]),
                isDeleted = false,
            )
            val existing = cloudById[snapshot.id]
            if (existing == null || (snapshot.updatedAt ?: Long.MIN_VALUE) > (existing.updatedAt ?: Long.MIN_VALUE)) {
                cloudById[snapshot.id] = snapshot
            }
        }
        return makeResult(name = "AI 对话", local = local, cloud = cloudById.values.toList())
    }

    private suspend fun dataSyncRows(action: String, uid: String): List<JsonObject> {
        val response = net.cloudApi.dataSync(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive(action),
                    "userId" to JsonPrimitive(uid),
                    "includeDeleted" to JsonPrimitive(true),
                ),
            ),
        )
        if (!response.success) error(response.error ?: "$action 检查失败")
        return rowArray(response.data).mapNotNull { it as? JsonObject }
    }

    private suspend fun userInfo(uid: String): JsonObject {
        val response = net.cloudApi.userAuth(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive("getUserInfo"),
                    "userId" to JsonPrimitive(uid),
                ),
            ),
        )
        if (!response.success) error(response.error ?: "getUserInfo 检查失败")
        return response.data ?: JsonObject(emptyMap())
    }

    private fun rowArray(data: JsonElement?): JsonArray = when (data) {
        is JsonArray -> data
        is JsonObject -> data["items"]?.jsonArray
            ?: data["rows"]?.jsonArray
            ?: data["data"]?.jsonArray
            ?: data["memos"]?.jsonArray
            ?: data["cards"]?.jsonArray
            ?: data["goals"]?.jsonArray
            ?: data["chunks"]?.jsonArray
            ?: JsonArray(emptyList())
        else -> JsonArray(emptyList())
    }

    private fun makeResult(
        name: String,
        local: List<RecordSnapshot>,
        cloud: List<RecordSnapshot>,
    ): V2ProfileCloudConsistencyResult {
        val localGroups = local.groupBy { it.id }
        val localDuplicateCount = localGroups.values.sumOf { (it.size - 1).coerceAtLeast(0) }
        val localById = latestRecordMap(local)
        val cloudGroups = cloud.groupBy { it.id }
        val cloudDuplicateCount = cloudGroups.values.sumOf { (it.size - 1).coerceAtLeast(0) }

        val cloudActiveById = linkedMapOf<String, RecordSnapshot>()
        val cloudDeletedById = linkedMapOf<String, RecordSnapshot>()
        cloudGroups.forEach { (id, records) ->
            val latest = records.maxByOrNull { it.updatedAt ?: Long.MIN_VALUE } ?: return@forEach
            if (latest.isDeleted) cloudDeletedById[id] = latest else cloudActiveById[id] = latest
        }

        val issues = mutableListOf<V2ProfileCloudConsistencyIssue>()
        val localOnly = localById.keys.filter { it !in cloudActiveById && it !in cloudDeletedById }
        if (localOnly.isNotEmpty()) {
            issues += makeIssue(
                title = "仅本机存在 ${localOnly.size} 条",
                ids = localOnly,
                lookup = localById,
                tone = V2ProfileCloudConsistencyIssueTone.INFO,
                prefix = "这些记录下次可能会被上传到云端",
            )
        }

        val cloudOnly = cloudActiveById.keys.filter { it !in localById }
        if (cloudOnly.isNotEmpty()) {
            issues += makeIssue(
                title = "云端有效但本机缺失 ${cloudOnly.size} 条",
                ids = cloudOnly,
                lookup = cloudActiveById,
                tone = V2ProfileCloudConsistencyIssueTone.DANGER,
                prefix = "这些记录会在新设备或重新同步时恢复到本机",
            )
        }

        val localButCloudDeleted = localById.keys.filter { it in cloudDeletedById }
        if (localButCloudDeleted.isNotEmpty()) {
            issues += makeIssue(
                title = "云端已删除但本机仍存在 ${localButCloudDeleted.size} 条",
                ids = localButCloudDeleted,
                lookup = localById,
                tone = V2ProfileCloudConsistencyIssueTone.WARNING,
                prefix = "这些记录可能来自当前设备未应用删除标记",
            )
        }

        if (localDuplicateCount > 0) {
            issues += V2ProfileCloudConsistencyIssue(
                title = "本机重复 ID $localDuplicateCount 条",
                detail = "当前设备同一业务 ID 有多条记录，需要检查本地存储是否重复写入。",
                tone = V2ProfileCloudConsistencyIssueTone.MUTED,
            )
        }

        if (cloudDuplicateCount > 0) {
            issues += V2ProfileCloudConsistencyIssue(
                title = "云端重复 ID $cloudDuplicateCount 条",
                detail = "云端同一业务 ID 有多条记录，dataSync 会逐步收敛，但建议关注。",
                tone = V2ProfileCloudConsistencyIssueTone.MUTED,
            )
        }

        return V2ProfileCloudConsistencyResult(
            name = name,
            localCount = local.size,
            cloudActiveCount = cloudActiveById.size,
            cloudDeletedCount = cloudDeletedById.size,
            localDuplicateCount = localDuplicateCount,
            cloudDuplicateCount = cloudDuplicateCount,
            issues = issues,
        )
    }

    private fun latestRecordMap(records: List<RecordSnapshot>): Map<String, RecordSnapshot> {
        val result = linkedMapOf<String, RecordSnapshot>()
        records.forEach { record ->
            val existing = result[record.id]
            if (existing == null || (record.updatedAt ?: Long.MIN_VALUE) > (existing.updatedAt ?: Long.MIN_VALUE)) {
                result[record.id] = record
            }
        }
        return result
    }

    private fun makeIssue(
        title: String,
        ids: List<String>,
        lookup: Map<String, RecordSnapshot>,
        tone: V2ProfileCloudConsistencyIssueTone,
        prefix: String,
    ): V2ProfileCloudConsistencyIssue {
        val examples = ids.take(5).mapNotNull { id ->
            lookup[id]?.let { "${it.title} [${it.rawId.take(8)}]" }
        }.joinToString("、")
        val suffix = if (ids.size > 5) " 等" else ""
        return V2ProfileCloudConsistencyIssue(
            title = title,
            detail = "$prefix：$examples$suffix",
            tone = tone,
        )
    }

    private fun localMemoTitle(rawJson: String, fallback: String): String {
        val obj = runCatching { net.json.parseToJsonElement(rawJson).jsonObject }.getOrNull()
        return obj?.let { firstString(it, "displayTitle", "title") } ?: fallback
    }

    private fun localGoalTitle(rawJson: String): String {
        val obj = runCatching { net.json.parseToJsonElement(rawJson).jsonObject }.getOrNull()
        return obj?.let { firstString(it, "title") } ?: "(无标题目标)"
    }

    private fun cloudGoalTitle(row: JsonObject): String {
        val raw = row["goalJson"] ?: row["goalJSON"]
        val obj = when (raw) {
            is JsonObject -> raw
            is JsonPrimitive -> raw.contentOrNull
                ?.let { runCatching { net.json.parseToJsonElement(it).jsonObject }.getOrNull() }
            else -> null
        }
        return obj?.let { firstString(it, "title") } ?: firstString(row, "title") ?: "(无标题目标)"
    }

    private fun firstString(obj: JsonObject, vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            obj[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        }

    private fun stringList(obj: JsonObject, vararg keys: String): List<String> {
        keys.forEach { key ->
            val value = obj[key] ?: return@forEach
            val list = when (value) {
                is JsonArray -> value.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.takeIf { text -> text.isNotBlank() } }
                else -> value.jsonPrimitive.contentOrNull
                    ?.split("|", ",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    .orEmpty()
            }
            if (list.isNotEmpty()) return list
        }
        return emptyList()
    }

    private fun normalize(value: String?): String =
        value?.trim().orEmpty()

    private data class RecordSnapshot(
        val rawId: String,
        val title: String,
        val updatedAt: Long?,
        val isDeleted: Boolean,
    ) {
        val id: String = rawId.lowercase()
    }

    private data class UserInfoField(
        val name: String,
        val local: String?,
        val cloud: String?,
    )
}

data class V2ProfileCloudConsistencyResult(
    val name: String,
    val localCount: Int,
    val cloudActiveCount: Int,
    val cloudDeletedCount: Int,
    val localDuplicateCount: Int,
    val cloudDuplicateCount: Int,
    val issues: List<V2ProfileCloudConsistencyIssue>,
) {
    val isHealthy: Boolean get() = issues.isEmpty()
    val statusText: String get() = if (isHealthy) "正常" else "需关注"
    val summary: String get() = if (isHealthy) {
        "本机与云端一致，未发现会导致复活或丢失的差异。"
    } else {
        "发现 ${issues.size} 类差异，请优先处理“云端有效但本机缺失”和“云端已删但本机仍存在”。"
    }
}

data class V2ProfileCloudConsistencyIssue(
    val title: String,
    val detail: String,
    val tone: V2ProfileCloudConsistencyIssueTone,
)

enum class V2ProfileCloudConsistencyIssueTone {
    INFO,
    DANGER,
    WARNING,
    MUTED,
}
