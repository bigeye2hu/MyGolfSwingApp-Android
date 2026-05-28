package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.db.GoalDao
import com.mygolfswing.app.data.db.GoalEntity
import com.mygolfswing.app.data.json.V2Json
import com.mygolfswing.app.data.model.V2Goal
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
    /** 本地全量 Flow（按 updatedAt 倒序） */
    fun observeAll(): Flow<List<V2Goal>> =
        dao.observeAll().map { rows ->
            rows.mapNotNull { row -> runCatching { decode(row) }.getOrNull() }
        }

    suspend fun get(id: String): V2Goal? = withContext(Dispatchers.IO) {
        dao.get(id)?.let { runCatching { decode(it) }.getOrNull() }
    }

    /** 创建 / 更新（标记为 dirty，后台异步推云） */
    suspend fun upsert(goal: V2Goal, pushToCloud: Boolean = true) = withContext(Dispatchers.IO) {
        val entity = encode(goal, dirty = pushToCloud)
        dao.upsert(entity)
        if (pushToCloud) scope.launch { pushSingle(goal) }
    }

    suspend fun delete(goalId: String) = withContext(Dispatchers.IO) {
        dao.delete(goalId)
        scope.launch {
            val uid = session.current().userId ?: return@launch
            runCatching {
                net.cloudApi.dataSync(
                    JsonObject(
                        mapOf(
                            "action" to JsonPrimitive("deleteV2Goal"),
                            "userId" to JsonPrimitive(uid),
                            "goalId" to JsonPrimitive(goalId),
                        ),
                    ),
                )
            }
        }
    }

    suspend fun bulkReplaceLocal(goals: List<V2Goal>) = withContext(Dispatchers.IO) {
        dao.upsertAll(goals.map { encode(it, dirty = false) })
    }

    // ============ 云同步 ============

    /** 内置演示目标标题（未登录 mock；不应与云端真实数据混显） */
    private val demoGoalTitles = setOf(
        "用 7 号铁打 50 球",
        "练习 40 分钟挥杆基础",
        "推杆 5 米精度训练",
        "每日复盘心法",
    )

    /**
     * 拉取云端并与本地合并（对齐 iOS `V2GoalCloudSyncService.syncFromCloud`）。
     *
     * - Last-Write-Wins：同 id 比较 `updatedAt`
     * - 云端有、本地无 → 写入本地
     * - 本地有、云端无 → 非演示数据且较新则上传；演示 mock 则删除
     * - 拉取成功后移除仍留在本地的演示目标（避免与 iOS 真实目标混显）
     */
    suspend fun syncFromCloud(): Result<Int> = runCatching {
        val uid = session.current().userId ?: error("未登录")
        val resp = net.cloudApi.dataSync(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive("getV2Goals"),
                    "userId" to JsonPrimitive(uid),
                ),
            ),
        )
        if (!resp.success) error(resp.error ?: "拉取目标失败")

        val cloudEntities = parseCloudGoalEntities(resp.data)
        Log.i(TAG, "getV2Goals: parsed ${cloudEntities.size} goals for userId=${uid.take(8)}…")

        withContext(Dispatchers.IO) {
            mergeCloudGoals(cloudEntities)
            cloudEntities.size
        }
    }

    /** @deprecated 请用 [syncFromCloud] */
    suspend fun pullFromCloud(): Result<Int> = syncFromCloud()

    private suspend fun mergeCloudGoals(cloudEntities: List<GoalEntity>) {
        val cloudById = cloudEntities.associateBy { it.id }
        val localRows = dao.all()
        val localById = localRows.associateBy { it.id }

        val toUpsert = mutableListOf<GoalEntity>()
        val toUpload = mutableListOf<V2Goal>()
        val toDelete = mutableListOf<String>()

        for (cloud in cloudEntities) {
            val local = localById[cloud.id]
            if (local == null) {
                toUpsert.add(cloud)
            } else if (cloud.updatedAt >= local.updatedAt) {
                toUpsert.add(cloud)
            } else {
                runCatching { toUpload.add(decode(local)) }
            }
        }

        for (local in localRows) {
            if (local.id in cloudById) continue
            val goal = runCatching { decode(local) }.getOrNull()
            if (goal != null && goal.title in demoGoalTitles) {
                toDelete.add(local.id)
                continue
            }
            if (local.dirty) {
                goal?.let { toUpload.add(it) }
            } else {
                goal?.let { toUpload.add(it) }
            }
        }

        toDelete.forEach { dao.delete(it) }
        if (toUpsert.isNotEmpty()) dao.upsertAll(toUpsert)

        // 登录态下移除演示 mock（避免与 iOS 同步的真实目标混显）
        dao.all().filter { row ->
            runCatching { decode(row).title }.getOrNull() in demoGoalTitles
        }.forEach { dao.delete(it.id) }

        toUpload.distinctBy { it.id }.forEach { pushSingle(it) }
    }

    private fun parseCloudGoalEntities(data: JsonElement?): List<GoalEntity> {
        val list = goalRowArray(data)
        return list.mapNotNull { element ->
            runCatching { cloudRowToEntity(element.jsonObject) }
                .onFailure { e ->
                    Log.w(TAG, "decode cloud goal row failed: ${e.message}")
                }
                .getOrNull()
        }
    }

    private fun goalRowArray(data: JsonElement?): JsonArray = when (data) {
        is JsonArray -> data
        is JsonObject -> data["goals"]?.jsonArray
            ?: data["items"]?.jsonArray
            ?: JsonArray(emptyList())
        else -> JsonArray(emptyList())
    }

    private fun cloudRowToEntity(o: JsonObject): GoalEntity {
        val (goalObj, goalJsonStored) = parseGoalJsonFromRow(o)
        val goal = V2Json.decodeGoal(goalObj)
        val updatedAt = epochMillisFromElement(o["updatedAt"])
            ?: goal.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return GoalEntity(
            id = goal.id.toString(),
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
    suspend fun pushDirty(): Result<Int> = runCatching {
        val uid = session.current().userId ?: error("未登录")
        val rows = withContext(Dispatchers.IO) { dao.dirty() }
        var ok = 0
        rows.forEach { row ->
            runCatching {
                net.cloudApi.dataSync(
                    JsonObject(
                        mapOf(
                            "action" to JsonPrimitive("saveV2Goal"),
                            "userId" to JsonPrimitive(uid),
                            "goalId" to JsonPrimitive(row.id),
                            "goalJson" to JsonPrimitive(row.goalJson),
                            "status" to JsonPrimitive(row.status),
                            "updatedAt" to JsonPrimitive(row.updatedAt),
                        ),
                    ),
                )
                withContext(Dispatchers.IO) { dao.markClean(row.id) }
                ok += 1
            }
        }
        ok
    }

    private suspend fun pushSingle(goal: V2Goal) {
        val uid = session.current().userId ?: return
        val jsonText = net.json.encodeToString(JsonObject.serializer(), V2Json.encodeGoal(goal))
        runCatching {
            net.cloudApi.dataSync(
                JsonObject(
                    mapOf(
                        "action" to JsonPrimitive("saveV2Goal"),
                        "userId" to JsonPrimitive(uid),
                        "goalId" to JsonPrimitive(goal.id.toString()),
                        "goalJson" to JsonPrimitive(jsonText),
                        "status" to JsonPrimitive(goal.status.name.lowercase()),
                        "updatedAt" to JsonPrimitive(goal.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()),
                    ),
                ),
            )
            withContext(Dispatchers.IO) { dao.markClean(goal.id.toString()) }
        }
    }

    // ============ 编解码 ============

    private fun encode(goal: V2Goal, dirty: Boolean): GoalEntity {
        val jsonObj = V2Json.encodeGoal(goal)
        return GoalEntity(
            id = goal.id.toString(),
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
    }
}
