package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.db.PlazaSavedDao
import com.mygolfswing.app.data.db.PlazaSavedEntity
import com.mygolfswing.app.data.model.V2PlazaCategoryChip
import com.mygolfswing.app.data.model.V2PlazaCoach
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.data.model.V2Topic
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.UUID

/**
 * 视频广场仓库（对齐 iOS `VideoPlazaCloudService`）。
 *
 * - 列表：`POST /videoPlaza { action: getPlazaVideos, limit, offset, sortBy }`
 * - 分类：`getCategories`
 * - 本地表只缓存"我收藏过的视频 id"
 *
 * 远端 `videoId` 是字符串，模型层用 UUID；用 `UUID.nameUUIDFromBytes` 做确定性转换。
 */
class V2PlazaRepository(
    private val savedDao: PlazaSavedDao,
    private val net: V2NetClient,
    private val session: V2SessionStore,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val savedIds: Flow<Set<String>> = session.state.flatMapLatest { current ->
        val uid = current.userId.orEmpty()
        if (uid.isBlank()) return@flatMapLatest flowOf(emptySet())
        savedDao.observe(uid).map { it.map { e -> e.videoId }.toSet() }
    }

    private var cachedCategories: List<V2PlazaCategoryChip> = emptyList()
    private val coachNameCache = mutableMapOf<String, String>()

    suspend fun fetchVideos(
        limit: Int = 20,
        offset: Int = 0,
        sortBy: String = "popular",
        categoryId: String? = null,
        parentCategoryId: String? = null,
        keyword: String? = null,
    ): Result<List<V2PlazaVideo>> = runCatching {
        val body = JsonObject(
            buildMap {
                put("action", JsonPrimitive("getPlazaVideos"))
                put("limit", JsonPrimitive(limit))
                put("offset", JsonPrimitive(offset))
                put("sortBy", JsonPrimitive(sortBy))
                val parent = parentCategoryId?.takeIf { it.isNotBlank() && it != "all" }
                val category = categoryId?.takeIf { it.isNotBlank() && it != "all" }
                if (parent != null) {
                    put("parentCategoryId", JsonPrimitive(parent))
                } else if (category != null) {
                    put("categoryId", JsonPrimitive(category))
                }
                keyword?.takeIf { it.isNotBlank() }?.let { put("keyword", JsonPrimitive(it)) }
                session.current().userId?.let { put("userId", JsonPrimitive(it)) }
            },
        )
        val resp = net.cloudApi.videoPlaza(body)
        val success = resp["success"]?.jsonPrimitive?.boolean == true
        if (!success) error(resp["error"]?.jsonPrimitive?.contentOrNull ?: "获取广场视频失败")
        val arr = resp["videos"]?.jsonArray ?: return@runCatching emptyList()
        val decoded = arr.mapNotNull { runCatching { decodeVideo(it.jsonObject) }.getOrNull() }
        hydrateCoaches(decoded)
    }

    suspend fun fetchCategories(): Result<List<V2PlazaCategoryChip>> = runCatching {
        val resp = net.cloudApi.videoPlaza(JsonObject(mapOf("action" to JsonPrimitive("getCategories"))))
        val arr = resp["categories"]?.jsonArray ?: return@runCatching emptyList()
        arr.mapNotNull { e ->
            runCatching {
                val o = e.jsonObject
                val id = o.string("categoryId", "id") ?: return@runCatching null
                V2PlazaCategoryChip(
                    id = id,
                    name = o.string("name") ?: id,
                    topic = topicFromCategoryId(id),
                    sortOrder = o["sortOrder"]?.jsonPrimitive?.intOrNull ?: 0,
                    parentId = o.string("parentId") ?: "",
                )
            }.getOrNull()
        }.sortedWith(compareBy<V2PlazaCategoryChip> { it.sortOrder }.thenBy { it.name })
            .also { cachedCategories = it }
    }

    suspend fun save(videoId: String) = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        savedDao.save(PlazaSavedEntity(uid, videoId, System.currentTimeMillis()))
    }

    suspend fun unsave(videoId: String) = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        savedDao.remove(videoId, uid)
    }

    private fun decodeVideo(o: JsonObject): V2PlazaVideo {
        val videoIdStr = o["videoId"]?.jsonPrimitive?.contentOrNull ?: error("missing videoId")
        val stats = o["stats"]?.jsonObject
        val categoryId = o.string("categoryId")
        val parentCategoryId = o.string("parentCategoryId")
        val tags = o.stringArray("tags")
        val avatarTints = V2PlazaCoach.AvatarTint.entries
        val categoryName = o.string("categoryName") ?: categoryNameFor(categoryId)
        val parentCategoryName = o.string("parentCategoryName") ?: categoryNameFor(parentCategoryId)
        val topic = inferTopic(categoryName ?: parentCategoryName, tags, o.string("title") ?: "")
        val thumb = thumbnailKindFor(topic)
        val coachId = o.string("coachId")
        val coachName = o.string("coachName", "coach") ?: coachId?.let { coachNameCache[it] }?.let { "Coach $it" } ?: "教练"
        val initial = coachName.firstOrNull()?.toString() ?: "C"
        val tintKey = coachId ?: coachName
        val tint = avatarTints[(tintKey.hashCode().ushr(1)) % avatarTints.size]
        val publishMs = o["publishTime"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
        val cover = (
            o["coverURL"]?.jsonPrimitive?.contentOrNull
                ?: o["coverUrl"]?.jsonPrimitive?.contentOrNull
                ?: o["thumbnailURL"]?.jsonPrimitive?.contentOrNull
            )?.takeIf { it.isNotBlank() }
        val catName = when {
            !categoryName.isNullOrBlank() && categoryName != parentCategoryName -> categoryName
            !parentCategoryName.isNullOrBlank() -> parentCategoryName
            else -> topic.displayName
        }
        return V2PlazaVideo(
            id = UUID.nameUUIDFromBytes(videoIdStr.toByteArray()),
            sourceVideoId = videoIdStr,
            sourceCoachId = coachId,
            sourceCategoryId = categoryId,
            sourceParentCategoryId = parentCategoryId,
            title = o.string("title") ?: "",
            durationSec = o["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            topic = topic,
            categoryDisplayName = catName,
            coach = V2PlazaCoach(coachName, initial, tint),
            viewCount = stats?.get("views")?.jsonPrimitive?.intOrNull ?: 0,
            saveCount = stats?.get("favorites")?.jsonPrimitive?.intOrNull ?: 0,
            thumbnailKind = thumb,
            thumbnailUrl = cover,
            videoUrl = o["vodURL"]?.jsonPrimitive?.contentOrNull ?: "",
            publishedAtMs = publishMs,
            isNew = (System.currentTimeMillis() - publishMs) < 7L * 24 * 3600 * 1000,
        )
    }

    private suspend fun hydrateCoaches(videos: List<V2PlazaVideo>): List<V2PlazaVideo> {
        val missingIds = videos.mapNotNull { it.sourceCoachId?.takeIf(String::isNotBlank) }
            .distinct()
            .filterNot { coachNameCache.containsKey(it) }
        missingIds.forEach { id ->
            fetchCoachName(id).getOrNull()?.let { coachNameCache[id] = it }
        }
        return videos.map { video ->
            val coachId = video.sourceCoachId
            val name = coachId?.let { coachNameCache[it] } ?: return@map video
            val displayName = "Coach $name"
            video.copy(
                coach = V2PlazaCoach(
                    displayName = displayName,
                    initialChar = name.firstOrNull()?.toString() ?: "?",
                    avatarTint = video.coach.avatarTint,
                ),
            )
        }
    }

    private suspend fun fetchCoachName(coachId: String): Result<String> = runCatching {
        val resp = net.cloudApi.videoPlaza(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive("getCoachProfile"),
                    "coachId" to JsonPrimitive(coachId),
                ),
            ),
        )
        val success = resp["success"]?.jsonPrimitive?.boolean != false
        if (!success) error(resp["error"]?.jsonPrimitive?.contentOrNull ?: "获取教练失败")
        val coach = resp["coach"]?.jsonObject ?: error("教练不存在")
        coach.string("name") ?: error("missing coach name")
    }

    private fun categoryNameFor(id: String?): String? =
        cachedCategories.firstOrNull { it.id == id }?.name

    private fun inferTopic(categoryName: String?, tags: List<String>, title: String): V2Topic {
        val blob = (listOf(categoryName.orEmpty()) + tags + title).joinToString(" ").lowercase()
        return when {
            blob.contains("推杆") || blob.contains("推球") || blob.contains("putt") -> V2Topic.PUTTING
            blob.contains("沙坑") || blob.contains("bunker") || blob.contains("sand") -> V2Topic.SAND
            blob.contains("短杆") || blob.contains("切杆") || blob.contains("劈杆") || blob.contains("chip") -> V2Topic.SHORT_GAME
            blob.contains("心法") || blob.contains("心态") || blob.contains("mind") -> V2Topic.MIND
            blob.contains("体能") || blob.contains("力量") || blob.contains("拉伸") || blob.contains("fitness") -> V2Topic.FITNESS
            blob.contains("装备") || blob.contains("球杆") || blob.contains("gear") || blob.contains("club") -> V2Topic.GEAR
            else -> V2Topic.SWING
        }
    }

    private fun thumbnailKindFor(topic: V2Topic): V2PlazaVideo.ThumbnailKind = when (topic) {
        V2Topic.SWING, V2Topic.ALL -> V2PlazaVideo.ThumbnailKind.GREEN_SWING
        V2Topic.PUTTING -> V2PlazaVideo.ThumbnailKind.MINT_TIP
        V2Topic.SHORT_GAME -> V2PlazaVideo.ThumbnailKind.SHORT_AMBER
        V2Topic.SAND -> V2PlazaVideo.ThumbnailKind.SAND_WARM
        V2Topic.MIND -> V2PlazaVideo.ThumbnailKind.MINT_TIP
        V2Topic.FITNESS -> V2PlazaVideo.ThumbnailKind.FITNESS_ROSE
        V2Topic.GEAR -> V2PlazaVideo.ThumbnailKind.EARTH_GEAR
    }

    private fun topicFromCategoryId(id: String?): V2Topic = when (id) {
        "swing" -> V2Topic.SWING
        "putting" -> V2Topic.PUTTING
        "short_game" -> V2Topic.SHORT_GAME
        "sand" -> V2Topic.SAND
        "mind" -> V2Topic.MIND
        "fitness" -> V2Topic.FITNESS
        "gear" -> V2Topic.GEAR
        else -> V2Topic.ALL
    }

    private fun JsonObject.string(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key -> this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } }

    private fun JsonObject.stringArray(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?: this[key]?.jsonPrimitive?.contentOrNull
                ?.split(",", "|")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
            ?: emptyList()
}
