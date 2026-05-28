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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
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

    /** 拉取云端 → 写入本地（云端新者覆盖本地） */
    suspend fun pullFromCloud(): Result<Int> = runCatching {
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
        val list: JsonArray = (resp.data as? JsonArray) ?: JsonArray(emptyList())
        val parsed = list.mapNotNull { e ->
            runCatching {
                val o = e.jsonObject
                val goalJsonStr = o["goalJson"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
                val goalObj = net.json.parseToJsonElement(goalJsonStr).jsonObject
                val goal = V2Json.decodeGoal(goalObj)
                val updatedAt = o["updatedAt"]?.jsonPrimitive?.longOrNull
                    ?: goal.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                GoalEntity(
                    id = goal.id.toString(),
                    goalJson = goalJsonStr,
                    status = goal.status.name.lowercase(),
                    priority = goal.priority.name.lowercase(),
                    pinState = goal.pinState.name.lowercase(),
                    todayFocus = goal.todayFocus,
                    updatedAt = updatedAt,
                    startedAt = goal.startedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    dirty = false,
                )
            }.getOrNull()
        }
        withContext(Dispatchers.IO) { dao.upsertAll(parsed) }
        parsed.size
    }

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
}
