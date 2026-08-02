package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.db.GoalDao
import com.mygolfswing.app.data.db.GoalEntity
import com.mygolfswing.app.data.json.V2Json
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import com.mygolfswing.app.data.ServiceLocator
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
import java.time.ZoneId

/**
 * Goal 仓库（对齐 iOS `V2GoalCloudSyncService` + 本地落盘）。
 *
 * 写：本地 upsert + 异步推送到云函数 `saveV2Goal`
 * 读：以本地 [GoalDao] 为唯一可观察源，调 [pullFromCloud] 时合并云端最新数据
 */
class V2GoalRepository(
    private val dao: GoalDao,
    private val net: V2NetClient,
    private val session: V2SessionStore,
    private val scope: CoroutineScope,
) {
    /** 当前用户本地 Flow（按 updatedAt 倒序）；未登录时不暴露任何历史数据。 */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAll(): Flow<List<V2Goal>> =
        session.state.flatMapLatest { current ->
            val uid = current.userId.orEmpty()
            if (uid.isBlank()) return@flatMapLatest flowOf(emptyList())
            dao.observeAll(uid).map { rows ->
                rows.mapNotNull { row -> runCatching { decode(row) }.getOrNull() }
            }
        }

    suspend fun get(id: String): V2Goal? = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext null
        dao.get(id, uid)?.let { runCatching { decode(it) }.getOrNull() }
    }

    /** 创建 / 更新（标记为 dirty，后台异步推云） */
    suspend fun upsert(goal: V2Goal, pushToCloud: Boolean = true) = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        val entity = encode(goal, ownerUserId = uid, dirty = pushToCloud)
        dao.upsert(entity)
        if (pushToCloud) scope.launch { pushSingle(goal) }
    }

    suspend fun delete(goalId: String) = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        dao.delete(goalId, uid)
        scope.launch {
            if (!deleteCloudGoal(uid, goalId)) {
                session.addPendingDelete(BUCKET_V2_GOAL, uid, goalId)
            }
        }
    }

    suspend fun bulkReplaceLocal(goals: List<V2Goal>) = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        dao.upsertAll(goals.map { encode(it, ownerUserId = uid, dirty = false) })
    }

    // ============ 云同步 ============

    /**
     * 拉取云端并与本地合并（对齐 iOS `V2GoalCloudSyncService.syncFromCloud`）。
     *
     * - Last-Write-Wins：同 id 比较 `updatedAt`
     * - 云端有、本地无 → 写入本地
     * - 本地有、云端无 → dirty 则上传；已同步（dirty=false）则删本地（云端物理删除）
     * - 拉取成功后移除仍留在本地的演示目标（避免与 iOS 真实目标混显）
     */
    suspend fun syncFromCloud(): Result<Int> = runGoalSyncCatching {
        val uid = session.current().userId ?: error("未登录")
        flushPendingDeletes(uid)
        val resp = net.cloudApi.dataSync(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive("getV2Goals"),
                    "userId" to JsonPrimitive(uid),
                    "includeDeleted" to JsonPrimitive(true),
                ),
            ),
        )
        if (!resp.success) error(resp.error ?: "拉取目标失败")

        val pendingDeleteIds = session.pendingDeletes(BUCKET_V2_GOAL, uid)
        val cloudRows = goalRowArray(resp.data).mapNotNull { it as? JsonObject }
        val activeRows = cloudRows
            .filterNot { isDeletedRow(it) }
            .filterNot { isPendingCloudGoalDelete(it, pendingDeleteIds) }
        val rowsForMerge = if (hasDuplicateGoalIds(activeRows)) {
            debugInfo("duplicate goalId detected, running repairV2Goals")
            repairCloudGoals(uid)
            val repaired = net.cloudApi.dataSync(
                JsonObject(
                    mapOf(
                        "action" to JsonPrimitive("getV2Goals"),
                        "userId" to JsonPrimitive(uid),
                        "includeDeleted" to JsonPrimitive(true),
                    ),
                ),
            )
            if (!repaired.success) error(repaired.error ?: "拉取目标失败")
            goalRowArray(repaired.data).mapNotNull { it as? JsonObject }
                .filterNot { isDeletedRow(it) }
                .filterNot { isPendingCloudGoalDelete(it, pendingDeleteIds) }
        } else {
            activeRows
        }
        val deletedCloudById = deletedGoalMap(cloudRows)
        val cloudEntities = dedupeGoalEntities(parseCloudGoalEntities(rowsForMerge, ownerUserId = uid))
        debugInfo("getV2Goals: parsed ${cloudEntities.size} active goals, ${deletedCloudById.size} deleted for userId=${uid.take(8)}…")

        withContext(Dispatchers.IO) {
            mergeCloudGoals(uid, cloudEntities, deletedCloudById)
            cloudEntities.size
        }
    }

    /** @deprecated 请用 [syncFromCloud] */
    suspend fun pullFromCloud(): Result<Int> = syncFromCloud()

    private suspend fun mergeCloudGoals(
        ownerUserId: String,
        cloudEntities: List<GoalEntity>,
        deletedCloudById: Map<String, Long>,
    ) {
        val cloudById = cloudEntities.associateBy { it.id }
        val localRows = dao.all(ownerUserId)
        val localById = localRows.associateBy { it.id }

        val toUpsert = mutableListOf<GoalEntity>()
        val toUpload = mutableListOf<V2Goal>()
        val toDelete = mutableListOf<String>()

        for (cloud in cloudEntities) {
            val local = localById[cloud.id]
            if (local == null) {
                toUpsert.add(cloud)
            } else if (local.dirty) {
                // 本地有未上云改动（含刚打卡）：不以云端覆盖，优先上传本地
                runCatching { toUpload.add(decode(local)) }
            } else if (cloud.updatedAt >= local.updatedAt) {
                toUpsert.add(cloud)
            } else {
                runCatching { toUpload.add(decode(local)) }
            }
        }

        for (local in localRows) {
            if (local.id in cloudById) continue
            val goal = runCatching { decode(local) }.getOrNull()
            if (shouldDeleteLocalDemoGoal(
                    goalTitle = goal?.title,
                    localGoalId = local.id,
                    isDirty = local.dirty,
                    cloudGoalIds = cloudById.keys,
                )
            ) {
                toDelete.add(local.id)
                continue
            }
            val deletedAt = deletedCloudById[local.id]
            if (deletedAt != null) {
                if (deletedAt >= local.updatedAt) {
                    toDelete.add(local.id)
                } else if (local.dirty) {
                    goal?.let { toUpload.add(it) }
                }
                continue
            }
            if (local.dirty) {
                // 本地有改动、云端无 → 待上传（含离线新建）
                goal?.let { toUpload.add(it) }
            } else {
                // 已同步过（dirty=false）但云端不存在且无删除墓碑 → 云端已物理删除，清本地
                toDelete.add(local.id)
            }
        }

        toDelete.forEach { dao.delete(it, ownerUserId) }
        if (toUpsert.isNotEmpty()) dao.upsertAll(toUpsert)

        // 登录态下移除演示 mock（避免与 iOS 同步的真实目标混显）
        dao.all(ownerUserId).filter { row ->
            shouldDeleteLocalDemoGoal(
                goalTitle = runCatching { decode(row).title }.getOrNull(),
                localGoalId = row.id,
                isDirty = row.dirty,
                cloudGoalIds = cloudById.keys,
            )
        }.forEach { dao.delete(it.id, ownerUserId) }

        toUpload.distinctBy { it.id }.forEach { pushSingle(it) }
    }

    private fun parseCloudGoalEntities(rows: List<JsonObject>, ownerUserId: String): List<GoalEntity> =
        rows.mapNotNull { row ->
            runCatching { cloudRowToEntity(row, ownerUserId) }
                .onFailure { e ->
                    debugWarn("decode cloud goal row failed: ${e.message}")
                }
                .getOrNull()
        }

    private fun goalRowArray(data: JsonElement?): JsonArray = when (data) {
        is JsonArray -> data
        is JsonObject -> data["goals"]?.jsonArray
            ?: data["items"]?.jsonArray
            ?: JsonArray(emptyList())
        else -> JsonArray(emptyList())
    }

    private fun isDeletedRow(row: JsonObject): Boolean =
        row["isDeleted"]?.jsonPrimitive?.booleanOrNull == true

    private fun isPendingCloudGoalDelete(row: JsonObject, pendingDeleteIds: Set<String>): Boolean =
        row["goalId"]?.jsonPrimitive?.contentOrNull.isPendingCloudDelete(pendingDeleteIds) ||
            row["id"]?.jsonPrimitive?.contentOrNull.isPendingCloudDelete(pendingDeleteIds)

    private fun hasDuplicateGoalIds(rows: List<JsonObject>): Boolean {
        val seen = mutableSetOf<String>()
        for (row in rows) {
            val id = row["goalId"]?.jsonPrimitive?.contentOrNull ?: continue
            if (!seen.add(id)) return true
        }
        return false
    }

    private suspend fun repairCloudGoals(uid: String): Boolean = runGoalCloudBooleanCatching {
        val resp = net.cloudApi.dataSync(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive("repairV2Goals"),
                    "userId" to JsonPrimitive(uid),
                ),
            ),
        )
        if (resp.success) {
            debugInfo("repairV2Goals completed for userId=${uid.take(8)}…")
        } else {
            debugWarn("repairV2Goals failed: ${resp.error}")
        }
        resp.success
    }

    /** 同一 goalId 多行时保留 updatedAt 最新的一条（对齐 iOS latestGoalMap）。 */
    private fun dedupeGoalEntities(entities: List<GoalEntity>): List<GoalEntity> {
        val byId = linkedMapOf<String, GoalEntity>()
        var duplicateCount = 0
        for (entity in entities) {
            val existing = byId[entity.id]
            if (existing == null) {
                byId[entity.id] = entity
            } else {
                duplicateCount += 1
                if (entity.updatedAt >= existing.updatedAt) {
                    byId[entity.id] = entity
                }
            }
        }
        if (duplicateCount > 0) {
            debugWarn("cloud goals had $duplicateCount duplicate goalId rows, kept latest updatedAt")
        }
        return byId.values.toList()
    }

    private fun debugInfo(message: String) {
        if (ServiceLocator.isDebuggable) Log.i(TAG, message)
    }

    private fun debugWarn(message: String) {
        if (ServiceLocator.isDebuggable) Log.w(TAG, message)
    }

    private fun deletedGoalMap(rows: List<JsonObject>): Map<String, Long> = buildMap {
        rows.filter(::isDeletedRow).forEach { row ->
            val id = row["goalId"]?.jsonPrimitive?.contentOrNull
                ?: row["id"]?.jsonPrimitive?.contentOrNull
                ?: return@forEach
            val deletedAt = epochMillisFromElement(row["deletedAt"])
                ?: epochMillisFromElement(row["updatedAt"])
                ?: System.currentTimeMillis()
            val existing = this[id]
            if (existing == null || deletedAt > existing) put(id, deletedAt)
        }
    }

    private fun cloudRowToEntity(o: JsonObject, ownerUserId: String): GoalEntity {
        val (goalObj, goalJsonStored) = parseGoalJsonFromRow(o)
        val goal = V2Json.decodeGoal(goalObj)
        val updatedAt = epochMillisFromElement(o["updatedAt"])
            ?: goal.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return GoalEntity(
            id = goal.id.toString(),
            ownerUserId = ownerUserId,
            goalJson = goalJsonStored,
            status = goal.status.name.lowercase(),
            priority = goal.priority.name.lowercase(),
            pinState = goal.pinState.name.lowercase(),
            todayFocus = goal.todayFocus,
            updatedAt = updatedAt,
            startedAt = goal.startedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            dirty = false,
        )
    }

    /**
     * 解析云端 goal 行里的 `goalJson`（对齐 iOS `decodeCloudRow`）。
     *
     * CloudBase / HTTP 反序列化后常见三种形态：
     * 1. `goalJson` 为 **JsonObject**（JSON 字符串被解析成对象）—— 当前 Android 误用 jsonPrimitive 会失败
     * 2. `goalJson` 为 **字符串**（标准形态）
     * 3. 字段误写成纯数字时间戳 —— 跳过，打日志
     */
    private fun parseGoalJsonFromRow(o: JsonObject): Pair<JsonObject, String> {
        val raw = o["goalJson"] ?: o["goalJSON"]
        when (raw) {
            is JsonObject -> {
                val text = net.json.encodeToString(JsonObject.serializer(), raw)
                return raw to text
            }
            is JsonPrimitive -> {
                val text = raw.content.trim()
                when {
                    text.startsWith("{") -> {
                        val obj = net.json.parseToJsonElement(text).jsonObject
                        return obj to text
                    }
                    text.all { it.isDigit() || it == '.' || it == '-' } -> {
                        val goalId = o["goalId"]?.jsonPrimitive?.contentOrNull
                        error("goalJson is timestamp-like ($text), goalId=$goalId — row keys=${o.keys}")
                    }
                    else -> error("goalJson is not JSON object: ${text.take(80)}")
                }
            }
            null -> {
                if (o.containsKey("title") && o.containsKey("metric") && o.containsKey("id")) {
                    val obj = JsonObject(
                        o.filter { (key, _) -> key !in ROW_META_KEYS },
                    )
                    val text = net.json.encodeToString(JsonObject.serializer(), obj)
                    return obj to text
                }
                error("missing goalJson, keys=${o.keys}")
            }
            else -> error("unsupported goalJson type: ${raw::class.simpleName}")
        }
    }

    private fun epochMillisFromElement(el: JsonElement?): Long? {
        val p = el as? JsonPrimitive ?: return null
        return p.longOrNull ?: p.doubleOrNull?.toLong()
    }

    private val ROW_META_KEYS = setOf(
        "_id", "userId", "goalId", "goalJson", "goalJSON",
        "status", "updatedAt", "createdAt", "cloudUpdatedAt",
    )

    /** 把本地所有 dirty 的目标推送到云端 */
    suspend fun pushDirty(): Result<Int> = runGoalSyncCatching {
        val uid = session.current().userId ?: error("未登录")
        val rows = withContext(Dispatchers.IO) { dao.dirty(uid) }
        var ok = 0
        rows.forEach { row ->
            if (saveGoalRowToCloud(uid, row)) {
                withContext(Dispatchers.IO) { dao.markClean(row.id, uid) }
                ok += 1
            }
        }
        ok
    }

    private suspend fun pushSingle(goal: V2Goal) {
        val uid = session.current().userId ?: return
        val jsonText = net.json.encodeToString(JsonObject.serializer(), V2Json.encodeGoal(goal))
        val row = GoalEntity(
            id = goal.id.toString(),
            ownerUserId = uid,
            goalJson = jsonText,
            status = goal.status.name.lowercase(),
            priority = goal.priority.name.lowercase(),
            pinState = goal.pinState.name.lowercase(),
            todayFocus = goal.todayFocus,
            updatedAt = goal.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            startedAt = goal.startedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            dirty = true,
        )
        if (saveGoalRowToCloud(uid, row)) {
            withContext(Dispatchers.IO) { dao.markClean(goal.id.toString(), uid) }
        }
    }

    private suspend fun saveGoalRowToCloud(uid: String, row: GoalEntity): Boolean = runGoalCloudBooleanCatching {
        val resp = net.cloudApi.dataSync(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive("saveV2Goal"),
                    "userId" to JsonPrimitive(uid),
                    "goalId" to JsonPrimitive(row.id),
                    "goalJson" to JsonPrimitive(row.goalJson),
                    "status" to JsonPrimitive(row.status),
                    "updatedAt" to JsonPrimitive(row.updatedAt),
                ),
            )
        )
        resp.success
    }

    private suspend fun flushPendingDeletes(uid: String) {
        session.pendingDeletes(BUCKET_V2_GOAL, uid).forEach { id ->
            if (deleteCloudGoal(uid, id)) {
                session.removePendingDelete(BUCKET_V2_GOAL, uid, id)
            }
        }
    }

    private suspend fun deleteCloudGoal(uid: String, goalId: String): Boolean = runGoalCloudBooleanCatching {
        val resp = net.cloudApi.dataSync(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive("deleteV2Goal"),
                    "userId" to JsonPrimitive(uid),
                    "goalId" to JsonPrimitive(goalId),
                ),
            ),
        )
        resp.success
    }

    // ============ 编解码 ============

    private fun encode(goal: V2Goal, ownerUserId: String, dirty: Boolean): GoalEntity {
        val jsonObj = V2Json.encodeGoal(goal)
        return GoalEntity(
            id = goal.id.toString(),
            ownerUserId = ownerUserId,
            goalJson = net.json.encodeToString(JsonObject.serializer(), jsonObj),
            status = goal.status.name.lowercase(),
            priority = goal.priority.name.lowercase(),
            pinState = goal.pinState.name.lowercase(),
            todayFocus = goal.todayFocus,
            updatedAt = goal.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            startedAt = goal.startedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            dirty = dirty,
        )
    }

    private fun decode(row: GoalEntity): V2Goal {
        val obj = net.json.parseToJsonElement(row.goalJson).jsonObject
        return V2Json.decodeGoal(obj)
    }

    private companion object {
        const val TAG = "V2GoalRepository"
        const val BUCKET_V2_GOAL = "v2Goal"
    }
}

private val DEMO_GOAL_TITLES = setOf(
    "用 7 号铁打 50 球",
    "练习 40 分钟挥杆基础",
    "推杆 5 米精度训练",
    "每日复盘心法",
)

internal fun shouldDeleteLocalDemoGoal(
    goalTitle: String?,
    localGoalId: String,
    isDirty: Boolean,
    cloudGoalIds: Set<String>,
): Boolean =
    !isDirty &&
        localGoalId !in cloudGoalIds &&
        goalTitle in DEMO_GOAL_TITLES

internal suspend inline fun <T> runGoalSyncCatching(
    crossinline block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}

internal suspend inline fun runGoalCloudBooleanCatching(
    crossinline block: suspend () -> Boolean,
): Boolean = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    false
}
