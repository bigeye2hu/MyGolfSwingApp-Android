package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.db.PlazaSavedDao
import com.mygolfswing.app.data.db.PlazaSavedEntity
import com.mygolfswing.app.data.model.V2PlazaCategoryChip
import com.mygolfswing.app.data.model.V2PlazaCoach
import com.mygolfswing.app.data.model.V2PlazaCoachLink
import com.mygolfswing.app.data.model.V2PlazaCoachProfile
import com.mygolfswing.app.data.model.V2PlazaVideo
import com.mygolfswing.app.data.model.V2Topic
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
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
        coachId: String? = null,
        keyword: String? = null,
    ): Result<List<V2PlazaVideo>> = runPlazaCatching {
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
                coachId?.takeIf { it.isNotBlank() }?.let { put("coachId", JsonPrimitive(it)) }
                keyword?.takeIf { it.isNotBlank() }?.let { put("keyword", JsonPrimitive(it)) }
                session.current().userId?.let { put("userId", JsonPrimitive(it)) }
            },
        )
        val resp = net.cloudApi.videoPlaza(body)
        val success = resp["success"]?.jsonPrimitive?.boolean == true
        if (!success) error(resp["error"]?.jsonPrimitive?.contentOrNull ?: "获取广场视频失败")
        val arr = resp["videos"]?.jsonArray ?: return@runPlazaCatching emptyList()
        val decoded = arr.mapNotNull { runCatching { decodeVideo(it.jsonObject) }.getOrNull() }
        hydrateCoaches(decoded)
    }

    suspend fun fetchCategories(): Result<List<V2PlazaCategoryChip>> = runPlazaCatching {
        val resp = net.cloudApi.videoPlaza(JsonObject(mapOf("action" to JsonPrimitive("getCategories"))))
        val arr = resp["categories"]?.jsonArray ?: return@runPlazaCatching emptyList()
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

    suspend fun fetchCoachProfile(coachId: String): Result<V2PlazaCoachProfile> = runPlazaCatching {
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
        decodeCoach(resp["coach"]?.jsonObject ?: error("教练不存在"))
    }

    suspend fun fetchCoachVideos(coachId: String, limit: Int = 30, offset: Int = 0): Result<List<V2PlazaVideo>> =
        fetchVideos(limit = limit, offset = offset, coachId = coachId)

    suspend fun save(videoId: String) = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: error("请先登录后再收藏")
        callAuthedVideoAction("favoriteVideo", videoId)
        savedDao.save(PlazaSavedEntity(uid, videoId, System.currentTimeMillis()))
    }

    suspend fun unsave(videoId: String) = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: error("请先登录后再取消收藏")
        callAuthedVideoAction("unfavoriteVideo", videoId)
        savedDao.remove(videoId, uid)
    }

    suspend fun like(videoId: String): Result<Unit> = runPlazaCatching {
        callAuthedVideoAction("likeVideo", videoId)
    }

    suspend fun unlike(videoId: String): Result<Unit> = runPlazaCatching {
        callAuthedVideoAction("unlikeVideo", videoId)
    }

    suspend fun fetchComments(videoId: String, limit: Int = 30, offset: Int = 0): Result<List<V2PlazaComment>> = runPlazaCatching {
        val resp = net.cloudApi.videoPlaza(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive("getComments"),
                    "videoId" to JsonPrimitive(videoId),
                    "limit" to JsonPrimitive(limit),
                    "offset" to JsonPrimitive(offset),
                ),
            ),
        )
        val success = resp["success"]?.jsonPrimitive?.boolean != false
        if (!success) error(resp["error"]?.jsonPrimitive?.contentOrNull ?: "获取评论失败")
        val arr = firstArray(
            resp,
            "comments",
            "items",
            "list",
            "data.comments",
            "data.items",
            "data.list",
        ) ?: return@runPlazaCatching emptyList()
        arr.mapNotNull { runCatching { decodeComment(it.jsonObject) }.getOrNull() }
    }

    suspend fun addComment(videoId: String, content: String): Result<V2PlazaComment> = runPlazaCatching {
        val current = session.current()
        val uid = current.userId ?: error("请先登录后再评论")
        val body = JsonObject(
            mapOf(
                "action" to JsonPrimitive("addComment"),
                "videoId" to JsonPrimitive(videoId),
                "userId" to JsonPrimitive(uid),
                "userName" to JsonPrimitive(current.nickname ?: current.username ?: "用户"),
                "userAvatar" to JsonPrimitive(current.avatar ?: ""),
                "content" to JsonPrimitive(content),
            ),
        )
        val resp = net.cloudApi.videoPlaza(body)
        val success = resp["success"]?.jsonPrimitive?.boolean != false
        if (!success) error(resp["error"]?.jsonPrimitive?.contentOrNull ?: "评论失败")
        val commentObj = firstObject(resp, "comment", "data.comment", "data", "result.comment")
        if (commentObj != null) {
            decodeComment(commentObj)
        } else {
            V2PlazaComment(
                id = UUID.randomUUID(),
                videoId = videoId,
                userId = uid,
                userName = current.nickname ?: current.username ?: "我",
                userAvatar = current.avatar,
                content = content,
                createdAt = LocalDateTime.now(),
            )
        }
    }

    suspend fun reportVideo(videoId: String, reason: String, detail: String): Result<Unit> = runPlazaCatching {
        val uid = session.current().userId ?: error("请先登录后再举报")
        val body = JsonObject(
            mapOf(
                "action" to JsonPrimitive("reportVideo"),
                "videoId" to JsonPrimitive(videoId),
                "userId" to JsonPrimitive(uid),
                "reason" to JsonPrimitive(reason),
                "reasonText" to JsonPrimitive(reason),
                "detail" to JsonPrimitive(detail),
                "priority" to JsonPrimitive(if (reason == "child_abuse" || reason == "pornography") 8 else 3),
            ),
        )
        val resp = net.cloudApi.videoPlaza(body)
        val success = resp["success"]?.jsonPrimitive?.boolean != false
        if (!success) error(resp["error"]?.jsonPrimitive?.contentOrNull ?: "举报失败")
    }

    suspend fun reportComment(commentId: String, videoId: String, reason: String = "用户举报"): Result<Unit> = runPlazaCatching {
        val uid = session.current().userId ?: error("请先登录后再举报")
        val body = JsonObject(
            mapOf(
                "action" to JsonPrimitive("reportComment"),
                "commentId" to JsonPrimitive(commentId),
                "videoId" to JsonPrimitive(videoId),
                "userId" to JsonPrimitive(uid),
                "reason" to JsonPrimitive(reason),
            ),
        )
        val resp = net.cloudApi.videoPlaza(body)
        val success = resp["success"]?.jsonPrimitive?.boolean != false
        if (!success) error(resp["error"]?.jsonPrimitive?.contentOrNull ?: "举报失败")
    }

    suspend fun claimVideo(
        videoId: String,
        claimType: String,
        contactInfo: String,
        platformLink: String,
        detail: String,
    ): Result<Unit> = runPlazaCatching {
        val uid = session.current().userId ?: error("请先登录后再认领")
        val body = JsonObject(
            mapOf(
                "action" to JsonPrimitive("claimVideo"),
                "videoId" to JsonPrimitive(videoId),
                "userId" to JsonPrimitive(uid),
                "claimType" to JsonPrimitive(claimType),
                "contactInfo" to JsonPrimitive(contactInfo),
                "platformLink" to JsonPrimitive(platformLink),
                "detail" to JsonPrimitive(detail),
            ),
        )
        val resp = net.cloudApi.videoPlaza(body)
        val success = resp["success"]?.jsonPrimitive?.boolean != false
        if (!success) error(resp["error"]?.jsonPrimitive?.contentOrNull ?: "认领提交失败")
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

    private suspend fun fetchCoachName(coachId: String): Result<String> = runPlazaCatching {
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

    private suspend fun callAuthedVideoAction(action: String, videoId: String) {
        val uid = session.current().userId ?: error("请先登录")
        val resp = net.cloudApi.videoPlaza(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive(action),
                    "videoId" to JsonPrimitive(videoId),
                    "userId" to JsonPrimitive(uid),
                ),
            ),
        )
        val success = resp["success"]?.jsonPrimitive?.boolean != false
        if (!success) error(resp["error"]?.jsonPrimitive?.contentOrNull ?: "操作失败")
    }

    private fun decodeComment(o: JsonObject): V2PlazaComment {
        val rawId = o.string("id", "commentId") ?: UUID.randomUUID().toString()
        val id = runCatching { UUID.fromString(rawId) }.getOrElse { UUID.nameUUIDFromBytes(rawId.toByteArray()) }
        val createdAtMs = o["createdAt"]?.jsonPrimitive?.longOrNull
            ?: o["created_at"]?.jsonPrimitive?.longOrNull
            ?: System.currentTimeMillis()
        val normalizedMs = if (createdAtMs < 10_000_000_000L) createdAtMs * 1000L else createdAtMs
        return V2PlazaComment(
            id = id,
            videoId = o.string("videoId") ?: "",
            userId = o.string("userId") ?: "",
            userName = o.string("userName", "nickname", "username") ?: "用户",
            userAvatar = o.string("userAvatar", "avatar"),
            content = o.string("content") ?: "",
            createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(normalizedMs), ZoneId.systemDefault()),
        )
    }

    private fun decodeCoach(o: JsonObject): V2PlazaCoachProfile {
        val id = o.string("id", "coachId") ?: error("missing coachId")
        val name = o.string("name", "coachName", "displayName") ?: "教练"
        val links = (o["externalLinks"] as? JsonArray)
            ?.mapNotNull { item ->
                runCatching {
                    val link = item.jsonObject
                    V2PlazaCoachLink(
                        platform = link.string("platform") ?: "link",
                        url = link.string("url") ?: "",
                        label = link.string("label") ?: link.string("platform") ?: "链接",
                        displayName = link.string("displayName") ?: "",
                    )
                }.getOrNull()
            }
            ?.filter { it.url.isNotBlank() || it.displayName.isNotBlank() }
            ?: emptyList()
        return V2PlazaCoachProfile(
            id = id,
            name = name,
            avatarUrl = o.string("avatarURL", "avatarUrl", "avatar"),
            bannerUrl = o.string("bannerURL", "bannerUrl", "banner"),
            certification = o.string("certification"),
            bio = o.string("bio", "description", "intro"),
            region = o.string("region"),
            tier = o.string("tier"),
            badges = o.stringArray("badges"),
            externalLinks = links,
        )
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

    private fun firstArray(o: JsonObject, vararg paths: String): JsonArray? =
        paths.firstNotNullOfOrNull { path ->
            nested(o, path) as? JsonArray
        }

    private fun firstObject(o: JsonObject, vararg paths: String): JsonObject? =
        paths.firstNotNullOfOrNull { path ->
            nested(o, path) as? JsonObject
        }

    private fun nested(o: JsonObject, path: String): JsonElement? =
        path.split(".").fold(o as JsonElement?) { current, key ->
            (current as? JsonObject)?.get(key)
        }

    private fun JsonObject.stringArray(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?: this[key]?.jsonPrimitive?.contentOrNull
                ?.split(",", "|")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
            ?: emptyList()

}

internal suspend inline fun <T> runPlazaCatching(
    crossinline block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}

data class V2PlazaComment(
    val id: UUID,
    val videoId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val content: String,
    val createdAt: LocalDateTime,
)
