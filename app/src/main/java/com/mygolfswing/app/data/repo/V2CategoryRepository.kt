package com.mygolfswing.app.data.repo

import android.content.Context
import androidx.core.content.edit
import com.mygolfswing.app.data.model.V2CategoryGroup
import com.mygolfswing.app.data.model.V2CategoryItem
import com.mygolfswing.app.data.model.V2UserCategoryData
import com.mygolfswing.app.data.model.V2UserNode
import com.mygolfswing.app.data.model.V2UserNodeGroup
import com.mygolfswing.app.data.model.V2UserTree
import com.mygolfswing.app.data.net.V2NetClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * 用户分类仓库（对齐 iOS `UserCategoryService` + `UnifiedCategoryBridge`）。
 */
class V2CategoryRepository(
    private val appContext: Context,
    private val net: V2NetClient,
) {
    private val prefs by lazy {
        appContext.getSharedPreferences("v2_category_cache", Context.MODE_PRIVATE)
    }

    fun loadDefaultGroups(): List<V2CategoryGroup> =
        parseDefaultSchema(appContext.assets.open("default_categories.json").bufferedReader().readText())
            .nodeGroups
            .map { it.toViewGroup() }

    fun createUserDataFromDefault(userId: String): V2UserCategoryData {
        val schema = parseDefaultSchema(
            appContext.assets.open("default_categories.json").bufferedReader().readText(),
        )
        val now = System.currentTimeMillis()
        return V2UserCategoryData(
            userId = userId,
            schemaVersion = schema.version,
            lastSyncedAt = now,
            nodeGroups = schema.nodeGroups.map { it.toUserNodeGroup(now) },
            trees = schema.trees.map { it.toUserTree(now) },
        )
    }

    suspend fun fetchUserCategory(userId: String): V2UserCategoryData? = withContext(Dispatchers.IO) {
        loadLocalCache(userId)?.let { return@withContext it }
        val resp = runCatching {
            net.cloudApi.dataSync(
                JsonObject(
                    mapOf(
                        "action" to JsonPrimitive("getUserCategory"),
                        "userId" to JsonPrimitive(userId),
                    ),
                ),
            )
        }.getOrNull() ?: return@withContext null
        if (!resp.success) return@withContext null
        val root = resp.data?.jsonObject ?: return@withContext null
        parseUserCategoryDocument(root, userId)?.also { saveLocalCache(it) }
    }

    suspend fun saveUserCategory(data: V2UserCategoryData): Boolean = withContext(Dispatchers.IO) {
        val innerJson = encodeUserCategoryDataJson(data)
        val payload = JsonObject(
            buildMap {
                put("data", JsonPrimitive(innerJson))
                put("schemaVersion", JsonPrimitive(data.schemaVersion))
                put("lastSyncedAt", JsonPrimitive(System.currentTimeMillis()))
            },
        )
        val ok = runCatching {
            net.cloudApi.dataSync(
                JsonObject(
                    mapOf(
                        "action" to JsonPrimitive("saveUserCategory"),
                        "userId" to JsonPrimitive(data.userId),
                        "categoryData" to payload,
                    ),
                ),
            ).success
        }.getOrDefault(false)
        if (ok) saveLocalCache(data.copy(lastSyncedAt = System.currentTimeMillis()))
        ok
    }

    fun groupsFromUserData(data: V2UserCategoryData): List<V2CategoryGroup> {
        val allGroups = (data.nodeGroups + data.customNodeGroups)
            .filter { it.isActive && !it.isDeprecated }
        return allGroups.map { group ->
            V2CategoryGroup(
                id = group.id,
                name = group.name,
                icon = group.icon,
                source = group.source,
                items = group.nodes
                    .filter { it.isActive }
                    .map { node ->
                        V2CategoryItem(
                            id = node.id,
                            name = node.name,
                            color = node.color,
                            groupId = group.id,
                            source = node.source,
                        )
                    },
            )
        }.filter { it.items.isNotEmpty() }
    }

    private fun saveLocalCache(data: V2UserCategoryData) {
        prefs.edit {
            putString(cacheKey(data.userId), encodeUserCategoryDataJson(data))
        }
    }

    private fun loadLocalCache(userId: String): V2UserCategoryData? {
        val raw = prefs.getString(cacheKey(userId), null) ?: return null
        return parseUserCategoryDataJson(raw, userId)
    }

    private fun cacheKey(userId: String) = "user_category_$userId"

    private data class DefaultSchema(
        val version: String,
        val nodeGroups: List<DefaultNodeGroup>,
        val trees: List<DefaultTree>,
    )

    private data class DefaultNodeGroup(
        val id: String,
        val name: String,
        val color: String,
        val icon: String?,
        val bindView: String?,
        val nodes: List<DefaultNode>,
    ) {
        fun toUserNodeGroup(now: Long) = V2UserNodeGroup(
            id = id,
            name = name,
            color = color,
            icon = icon,
            bindView = bindView,
            source = "system",
            isActive = true,
            createdAt = now,
            updatedAt = now,
            nodes = nodes.map { it.toUserNode(now) },
        )

        fun toViewGroup() = V2CategoryGroup(
            id = id,
            name = name,
            icon = icon,
            source = "system",
            items = nodes.map {
                V2CategoryItem(id = it.id, name = it.name, color = it.color, groupId = id, source = "system")
            },
        )
    }

    private data class DefaultNode(
        val id: String,
        val name: String,
        val color: String,
        val hotspotId: String?,
    ) {
        fun toUserNode(now: Long) = V2UserNode(
            id = id,
            name = name,
            color = color,
            hotspotId = hotspotId,
            source = "system",
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
    }

    private data class DefaultTree(
        val id: String,
        val name: String,
        val description: String?,
        val rootNode: String?,
        val childGroup: String?,
        val viewType: String,
        val bindToPages: List<String>,
    ) {
        fun toUserTree(now: Long) = V2UserTree(
            id = id,
            name = name,
            description = description,
            rootNode = rootNode,
            childGroup = childGroup,
            viewType = viewType,
            bindToPages = bindToPages,
            source = "system",
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun parseDefaultSchema(raw: String): DefaultSchema {
        val root = net.json.parseToJsonElement(raw).jsonObject
        val groups = root["nodeGroups"]?.jsonArray.orEmpty().mapNotNull { el ->
            val o = el.jsonObject
            DefaultNodeGroup(
                id = o.string("id") ?: return@mapNotNull null,
                name = o.string("name") ?: return@mapNotNull null,
                color = o.string("color") ?: "#9CA3AF",
                icon = o.string("icon"),
                bindView = o.string("bindView"),
                nodes = o["nodes"]?.jsonArray.orEmpty().mapNotNull { nodeEl ->
                    val n = nodeEl.jsonObject
                    DefaultNode(
                        id = n.string("id") ?: return@mapNotNull null,
                        name = n.string("name") ?: return@mapNotNull null,
                        color = n.string("color") ?: "#9CA3AF",
                        hotspotId = n.string("hotspotId"),
                    )
                },
            )
        }
        val trees = root["trees"]?.jsonArray.orEmpty().mapNotNull { el ->
            val o = el.jsonObject
            val childGroup = o.string("childGroup")
                ?: o["childGroups"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
            DefaultTree(
                id = o.string("id") ?: return@mapNotNull null,
                name = o.string("name") ?: return@mapNotNull null,
                description = o.string("description"),
                rootNode = o.string("rootNode"),
                childGroup = childGroup,
                viewType = o.string("viewType") ?: "list",
                bindToPages = o["bindToPages"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
            )
        }
        return DefaultSchema(
            version = root.string("version") ?: "4.1",
            nodeGroups = groups,
            trees = trees,
        )
    }

    private fun parseUserCategoryDocument(root: JsonObject, userId: String): V2UserCategoryData? {
        val embedded = root.string("data")?.let { parseUserCategoryDataJson(it, userId) }
        if (embedded != null) return embedded
        if (root["nodeGroups"] != null) return parseUserCategoryDataObject(root, userId)
        return null
    }

    private fun parseUserCategoryDataJson(raw: String, userId: String): V2UserCategoryData? =
        runCatching {
            parseUserCategoryDataObject(net.json.parseToJsonElement(raw).jsonObject, userId)
        }.getOrNull()

    private fun parseUserCategoryDataObject(o: JsonObject, fallbackUserId: String): V2UserCategoryData? {
        val userId = o.string("userId") ?: fallbackUserId
        return V2UserCategoryData(
            userId = userId,
            schemaVersion = o.string("schemaVersion") ?: "4.1",
            lastSyncedAt = parseMillis(o["lastSyncedAt"]) ?: System.currentTimeMillis(),
            nodeGroups = parseNodeGroups(o["nodeGroups"]),
            trees = parseTrees(o["trees"]),
            customNodeGroups = parseNodeGroups(o["customNodeGroups"]),
            customTrees = parseTrees(o["customTrees"]),
        )
    }

    private fun parseNodeGroups(el: JsonElement?): List<V2UserNodeGroup> =
        el?.jsonArray.orEmpty().mapNotNull { groupEl ->
            val g = groupEl.jsonObject
            V2UserNodeGroup(
                id = g.string("id") ?: return@mapNotNull null,
                name = g.string("name") ?: return@mapNotNull null,
                color = g.string("color") ?: "#9CA3AF",
                icon = g.string("icon"),
                bindView = g.string("bindView"),
                source = g.string("source") ?: "system",
                isActive = g["isActive"]?.jsonPrimitive?.booleanOrNull ?: true,
                isDeprecated = g["isDeprecated"]?.jsonPrimitive?.booleanOrNull ?: false,
                createdAt = parseMillis(g["createdAt"]) ?: System.currentTimeMillis(),
                updatedAt = parseMillis(g["updatedAt"]) ?: System.currentTimeMillis(),
                nodes = g["nodes"]?.jsonArray.orEmpty().mapNotNull { nodeEl ->
                    val n = nodeEl.jsonObject
                    V2UserNode(
                        id = n.string("id") ?: return@mapNotNull null,
                        name = n.string("name") ?: return@mapNotNull null,
                        color = n.string("color") ?: "#9CA3AF",
                        hotspotId = n.string("hotspotId"),
                        source = n.string("source") ?: "system",
                        isActive = n["isActive"]?.jsonPrimitive?.booleanOrNull ?: true,
                        usageCount = n["usageCount"]?.jsonPrimitive?.intOrNull ?: 0,
                        createdAt = parseMillis(n["createdAt"]) ?: System.currentTimeMillis(),
                        updatedAt = parseMillis(n["updatedAt"]) ?: System.currentTimeMillis(),
                    )
                },
            )
        }

    private fun parseTrees(el: JsonElement?): List<V2UserTree> =
        el?.jsonArray.orEmpty().mapNotNull { treeEl ->
            val t = treeEl.jsonObject
            V2UserTree(
                id = t.string("id") ?: return@mapNotNull null,
                name = t.string("name") ?: return@mapNotNull null,
                description = t.string("description"),
                rootNode = t.string("rootNode"),
                childGroup = t.string("childGroup")
                    ?: t["childGroups"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull,
                customChildren = t["customChildren"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
                viewType = t.string("viewType") ?: "list",
                bindToPages = t["bindToPages"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
                source = t.string("source") ?: "system",
                isActive = t["isActive"]?.jsonPrimitive?.booleanOrNull ?: true,
                createdAt = parseMillis(t["createdAt"]) ?: System.currentTimeMillis(),
                updatedAt = parseMillis(t["updatedAt"]) ?: System.currentTimeMillis(),
            )
        }

    private fun encodeUserCategoryDataJson(data: V2UserCategoryData): String {
        val obj = JsonObject(
            buildMap {
                put("userId", JsonPrimitive(data.userId))
                put("schemaVersion", JsonPrimitive(data.schemaVersion))
                put("lastSyncedAt", JsonPrimitive(data.lastSyncedAt))
                put("nodeGroups", encodeNodeGroups(data.nodeGroups))
                put("trees", encodeTrees(data.trees))
                put("customNodeGroups", encodeNodeGroups(data.customNodeGroups))
                put("customTrees", encodeTrees(data.customTrees))
            },
        )
        return net.json.encodeToString(JsonObject.serializer(), obj)
    }

    private fun encodeNodeGroups(groups: List<V2UserNodeGroup>): JsonArray =
        JsonArray(
            groups.map { g ->
                JsonObject(
                    buildMap {
                        put("id", JsonPrimitive(g.id))
                        put("name", JsonPrimitive(g.name))
                        put("color", JsonPrimitive(g.color))
                        g.icon?.let { put("icon", JsonPrimitive(it)) }
                        g.bindView?.let { put("bindView", JsonPrimitive(it)) }
                        put("source", JsonPrimitive(g.source))
                        put("isActive", JsonPrimitive(g.isActive))
                        put("isDeprecated", JsonPrimitive(g.isDeprecated))
                        put("createdAt", JsonPrimitive(g.createdAt))
                        put("updatedAt", JsonPrimitive(g.updatedAt))
                        put(
                            "nodes",
                            JsonArray(
                                g.nodes.map { n ->
                                    JsonObject(
                                        buildMap {
                                            put("id", JsonPrimitive(n.id))
                                            put("name", JsonPrimitive(n.name))
                                            put("color", JsonPrimitive(n.color))
                                            n.hotspotId?.let { put("hotspotId", JsonPrimitive(it)) }
                                            put("source", JsonPrimitive(n.source))
                                            put("isActive", JsonPrimitive(n.isActive))
                                            put("usageCount", JsonPrimitive(n.usageCount))
                                            put("createdAt", JsonPrimitive(n.createdAt))
                                            put("updatedAt", JsonPrimitive(n.updatedAt))
                                        },
                                    )
                                },
                            ),
                        )
                    },
                )
            },
        )

    private fun encodeTrees(trees: List<V2UserTree>): JsonArray =
        JsonArray(
            trees.map { t ->
                JsonObject(
                    buildMap {
                        put("id", JsonPrimitive(t.id))
                        put("name", JsonPrimitive(t.name))
                        t.description?.let { put("description", JsonPrimitive(it)) }
                        t.rootNode?.let { put("rootNode", JsonPrimitive(it)) }
                        t.childGroup?.let { put("childGroup", JsonPrimitive(it)) }
                        put("viewType", JsonPrimitive(t.viewType))
                        put("source", JsonPrimitive(t.source))
                        put("isActive", JsonPrimitive(t.isActive))
                        put("createdAt", JsonPrimitive(t.createdAt))
                        put("updatedAt", JsonPrimitive(t.updatedAt))
                        put("bindToPages", JsonArray(t.bindToPages.map { JsonPrimitive(it) }))
                        put("customChildren", JsonArray(t.customChildren.map { JsonPrimitive(it) }))
                    },
                )
            },
        )

    private fun parseMillis(el: JsonElement?): Long? {
        if (el == null) return null
        return runCatching {
            when (el) {
                is JsonObject -> el["\$date"]?.jsonPrimitive?.contentOrNull
                    ?.let { java.time.Instant.parse(it).toEpochMilli() }
                else -> el.jsonPrimitive.longOrNull
                    ?: el.jsonPrimitive.contentOrNull?.toLongOrNull()
            }
        }.getOrNull()
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
}
