package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.db.PlazaSavedDao
import com.mygolfswing.app.data.db.PlazaSavedEntity
import com.mygolfswing.app.data.model.V2PlazaCategoryChip
import com.mygolfswing.app.data.model.V2PlazaCoach
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.data.model.V2Topic
import com.mygolfswing.app.data.net.V2NetClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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
) {
    val savedIds: Flow<Set<String>> = savedDao.observe().map { it.map { e -> e.videoId }.toSet() }

    suspend fun fetchVideos(
        limit: Int = 20,
        offset: Int = 0,
        sortBy: String = "popular",
        categoryId: String? = null,
        keyword: String? = null,
    ): Result<List<V2PlazaVideo>> = runCatching {
        val body = JsonObject(
            buildMap {
                put("action", JsonPrimitive("getPlazaVideos"))
                put("limit", JsonPrimitive(limit))
                put("offset", JsonPrimitive(offset))
                put("sortBy", JsonPrimitive(sortBy))
                categoryId?.takeIf { it.isNotBlank() && it != "all" }?.let { put("categoryId", JsonPrimitive(it)) }
                keyword?.takeIf { it.isNotBlank() }?.let { put("keyword", JsonPrimitive(it)) }
            },
        )
        val resp = net.cloudApi.videoPlaza(body)
        val success = resp["success"]?.jsonPrimitive?.boolean == true
        if (!success) error(resp["error"]?.jsonPrimitive?.contentOrNull ?: "获取广场视频失败")
        val arr = resp["videos"]?.jsonArray ?: return@runCatching emptyList()
        arr.mapNotNull { runCatching { decodeVideo(it.jsonObject) }.getOrNull() }
    }

    suspend fun fetchCategories(): Result<List<V2PlazaCategoryChip>> = runCatching {
        val resp = net.cloudApi.videoPlaza(JsonObject(mapOf("action" to JsonPrimitive("getCategories"))))
        val arr = resp["categories"]?.jsonArray ?: return@runCatching emptyList()
        arr.mapNotNull { e ->
            runCatching {
                val o = e.jsonObject
                val id = o["categoryId"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
                V2PlazaCategoryChip(
                    id = id,
                    name = o["name"]?.jsonPrimitive?.contentOrNull ?: id,
                    topic = topicFromCategoryId(id),
                )
            }.getOrNull()
        }
    }

    suspend fun save(videoId: String) = withContext(Dispatchers.IO) {
        savedDao.save(PlazaSavedEntity(videoId, System.currentTimeMillis()))
    }

    suspend fun unsave(videoId: String) = withContext(Dispatchers.IO) {
        savedDao.remove(videoId)
    }

    private fun decodeVideo(o: JsonObject): V2PlazaVideo {
        val videoIdStr = o["videoId"]?.jsonPrimitive?.contentOrNull ?: error("missing videoId")
        val stats = o["stats"]?.jsonObject
        val categoryId = o["categoryId"]?.jsonPrimitive?.contentOrNull
        val thumbs = V2PlazaVideo.ThumbnailKind.entries
        val avatarTints = V2PlazaCoach.AvatarTint.entries
        val deterministic = videoIdStr.hashCode()
        val thumb = thumbs[(deterministic.ushr(1)) % thumbs.size]
        val coachName = o["coachName"]?.jsonPrimitive?.contentOrNull ?: "教练"
        val initial = coachName.firstOrNull()?.toString() ?: "C"
        val tint = avatarTints[(coachName.hashCode().ushr(1)) % avatarTints.size]
        val publishMs = o["publishTime"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
        val cover = o["coverURL"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        val catName = o["categoryName"]?.jsonPrimitive?.contentOrNull
            ?: topicFromCategoryId(categoryId).displayName
        return V2PlazaVideo(
            id = UUID.nameUUIDFromBytes(videoIdStr.toByteArray()),
            sourceVideoId = videoIdStr,
            title = o["title"]?.jsonPrimitive?.contentOrNull ?: "",
            durationSec = o["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            topic = topicFromCategoryId(categoryId),
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
}
