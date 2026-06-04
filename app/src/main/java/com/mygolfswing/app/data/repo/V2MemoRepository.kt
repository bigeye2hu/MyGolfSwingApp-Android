package com.mygolfswing.app.data.repo

import android.content.Context
import com.mygolfswing.app.data.db.MemoDao
import com.mygolfswing.app.data.db.MemoEntity
import com.mygolfswing.app.data.json.V2Json
import com.mygolfswing.app.data.memo.V2MemoBridge
import com.mygolfswing.app.data.memo.V2MemoThumbnailStore
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoAccent
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.model.V2MemoSource
import com.mygolfswing.app.data.model.V2PinState
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import com.mygolfswing.app.data.store.V2CategoryStore
import com.mygolfswing.app.data.video.V2VideoThumbnailUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
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
import java.util.UUID

internal fun remoteThumbnailUrlForUpload(thumbnailUrl: String?): String? {
    val trimmed = thumbnailUrl?.trim() ?: return null
    return trimmed.takeIf { V2MemoBridge.isRemoteUrl(it) }
}

/**
 * Memo 仓库（对齐 iOS `VideoMemoCloudSyncService` + `InsightCardCloudSyncService`）。
 */
class V2MemoRepository(
    context: Context,
    private val dao: MemoDao,
    private val net: V2NetClient,
    private val session: V2SessionStore,
    private val scope: CoroutineScope,
) {
    private val appContext: Context = context.applicationContext

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAll(): Flow<List<V2Memo>> =
        session.state.flatMapLatest { current ->
            val uid = current.userId.orEmpty()
            if (uid.isBlank()) return@flatMapLatest flowOf(emptyList())
            dao.observeAll(uid).map { rows ->
                rows.mapNotNull { row -> runCatching { decode(row) }.getOrNull() }
            }
        }

    suspend fun get(id: String): V2Memo? = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext null
        dao.get(id, uid)?.let { runCatching { decode(it) }.getOrNull() }
    }

    suspend fun upsert(memo: V2Memo, pushToCloud: Boolean = true) = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        dao.upsert(encode(memo, ownerUserId = uid, dirty = pushToCloud))
        if (pushToCloud) scope.launch { pushSingle(memo) }
    }

    suspend fun delete(memoId: String) = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        val memo = dao.get(memoId, uid)?.let { runCatching { decode(it) }.getOrNull() }
        dao.delete(memoId, uid)
        scope.launch {
            val bucket = if (memo?.kind == V2MemoKind.TEXT) BUCKET_INSIGHT_CARD else BUCKET_VIDEO_MEMO
            if (!deleteCloudMemo(uid, memoId, bucket)) {
                session.addPendingDelete(bucket, uid, memoId)
            }
        }
    }

    suspend fun bulkReplaceLocal(memos: List<V2Memo>) = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        dao.upsertAll(memos.map { encode(it, ownerUserId = uid, dirty = false) })
    }

    suspend fun deleteLocalDemoMemos() = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        dao.getAll(uid).forEach { row ->
            val memo = runCatching { decode(row) }.getOrNull() ?: return@forEach
            if (memo.isDemoMemo()) dao.delete(row.id, uid)
        }
    }

    suspend fun pushDirty() = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        dao.dirty(uid).mapNotNull { runCatching { decode(it) }.getOrNull() }.forEach { pushSingle(it) }
    }

    suspend fun pullFromCloud(): Result<Int> = runCatching {
        val uid = session.current().userId ?: error("未登录")
        flushPendingDeletes(uid)
        var merged = pullVideoMemos(uid)
        merged += pullInsightCards(uid)
        recoverMissingThumbnails()
        merged
    }

    private suspend fun flushPendingDeletes(uid: String) {
        flushPendingDeletes(uid, BUCKET_VIDEO_MEMO)
        flushPendingDeletes(uid, BUCKET_INSIGHT_CARD)
    }

    private suspend fun flushPendingDeletes(uid: String, bucket: String) {
        session.pendingDeletes(bucket, uid).forEach { id ->
            if (deleteCloudMemo(uid, id, bucket)) {
                session.removePendingDelete(bucket, uid, id)
            }
        }
    }

    private suspend fun deleteCloudMemo(uid: String, id: String, bucket: String): Boolean {
        val isText = bucket == BUCKET_INSIGHT_CARD
        val action = if (isText) "deleteInsightCard" else "deleteVideoMemo"
        val idKey = if (isText) "cardId" else "memoId"
        return runCatching {
            val resp = net.cloudApi.dataSync(
                JsonObject(
                    mapOf(
                        "action" to JsonPrimitive(action),
                        "userId" to JsonPrimitive(uid),
                        idKey to JsonPrimitive(id),
                    ),
                ),
            )
            resp.success
        }.getOrDefault(false)
    }

    /**
     * 云同步后补齐缺失封面：遍历本地 memo，逐个尝试 thumb 文件 / 在线猜测 / 本地抽帧，
     * 命中后写回 thumbnailUrl（dirty=false，不重复上传）。对齐 iOS 同步后的封面补齐。
     */
    suspend fun recoverMissingThumbnails() = withContext(Dispatchers.IO) {
        val uid = session.current().userId ?: return@withContext
        dao.getAll(uid).forEach { row ->
            val memo = runCatching { decode(row) }.getOrNull() ?: return@forEach
            val updated = V2MemoThumbnailStore.recoverThumbnail(appContext, memo, uid) ?: return@forEach
            dao.upsert(encode(updated, ownerUserId = uid, dirty = false))
        }
    }

    private suspend fun pullVideoMemos(uid: String): Int {
        val resp = net.cloudApi.dataSync(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive("getVideoMemos"),
                    "userId" to JsonPrimitive(uid),
                    "includeDeleted" to JsonPrimitive(true),
                ),
            ),
        )
        if (!resp.success) error(resp.error ?: "拉取视频 Memo 失败")
        val list = (resp.data as? JsonArray) ?: JsonArray(emptyList())
        var merged = 0
        for (element in list) {
            val cloud = element.jsonObject
            val memoId = cloud["memoId"]?.jsonPrimitive?.contentOrNull
            if (cloud["isDeleted"]?.jsonPrimitive?.booleanOrNull == true) {
                applyCloudDeletedMemo(memoId, cloud)
                continue
            }
            val memo = parseCloudVideoMemo(cloud, uid) ?: continue
            if (mergeIfNewer(memo, cloud)) merged++
        }
        return merged
    }

    private suspend fun pullInsightCards(uid: String): Int {
        val resp = net.cloudApi.dataSync(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive("getInsightCards"),
                    "userId" to JsonPrimitive(uid),
                    "includeDeleted" to JsonPrimitive(true),
                ),
            ),
        )
        if (!resp.success) error(resp.error ?: "拉取随手记失败")
        val list = (resp.data as? JsonArray) ?: JsonArray(emptyList())
        var merged = 0
        for (element in list) {
            val cloud = element.jsonObject
            val cardId = cloud["cardId"]?.jsonPrimitive?.contentOrNull
            if (cloud["isDeleted"]?.jsonPrimitive?.booleanOrNull == true) {
                applyCloudDeletedMemo(cardId, cloud)
                continue
            }
            val memo = parseCloudInsightCard(cloud) ?: continue
            if (mergeIfNewer(memo, cloud)) merged++
        }
        return merged
    }

    private suspend fun applyCloudDeletedMemo(id: String?, cloud: JsonObject) {
        val normalizedId = id?.takeIf { it.isNotBlank() } ?: return
        val uid = session.current().userId ?: return
        val localRow = dao.get(normalizedId, uid) ?: return
        val deletedAt = parseCloudMillis(cloud["deletedAt"])
            ?: parseCloudMillis(cloud["updatedAt"])
            ?: System.currentTimeMillis()
        if (deletedAt >= localRow.updatedAt) {
            dao.delete(normalizedId, uid)
        }
    }

    private suspend fun mergeIfNewer(memo: V2Memo, cloud: JsonObject): Boolean {
        val uid = session.current().userId ?: return false
        val localRow = dao.get(memo.id.toString(), uid)
        if (localRow == null) {
            dao.upsert(encode(memo, ownerUserId = uid, dirty = false))
            return true
        }
        val localMemo = decode(localRow)
        val cloudUpdated = cloudUpdatedMillis(cloud)
        val localUpdated = localMemo.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val shouldCorrectImportedTextTime = memo.kind == V2MemoKind.TEXT &&
            !localRow.dirty &&
            hasMeaningfullyDifferentCloudCreatedAt(localRow, cloud)
        if (cloudUpdated >= localUpdated || shouldCorrectImportedTextTime) {
            dao.upsert(encode(memo, ownerUserId = uid, dirty = false))
            return true
        }
        return false
    }

    private fun hasMeaningfullyDifferentCloudCreatedAt(localRow: MemoEntity, cloud: JsonObject): Boolean {
        val cloudCreated = parseCloudMillis(cloud["cardCreatedAt"])
            ?: parseCloudMillis(cloud["createdAt"])
            ?: return false
        return kotlin.math.abs(localRow.createdAt - cloudCreated) > 60_000L
    }

    private fun parseCloudVideoMemo(o: JsonObject, ownerUserId: String): V2Memo? {
        val memoIdStr = o["memoId"]?.jsonPrimitive?.contentOrNull ?: return null
        val id = runCatching { UUID.fromString(memoIdStr) }.getOrNull() ?: return null
        val title = o["title"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: "视频 Memo"
        val body = o["memoDescription"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val isOnline = o["isOnlineVideo"]?.jsonPrimitive?.booleanOrNull == true
        val trainingMethod = o["trainingMethod"]?.jsonPrimitive?.contentOrNull
        val platform = o["platform"]?.jsonPrimitive?.contentOrNull
        val plazaId = o["plazaVideoId"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val videoUrl = when {
            isOnline -> o["onlinePlayURL"]?.jsonPrimitive?.contentOrNull
                ?: o["videoPath"]?.jsonPrimitive?.contentOrNull
            else -> {
                val local = o["localFilePath"]?.jsonPrimitive?.contentOrNull
                val remote = o["videoPath"]?.jsonPrimitive?.contentOrNull
                local?.takeIf { it.isNotBlank() } ?: remote?.takeIf { it.isNotBlank() }
            }
        }
        val duration = o["duration"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
        val tagIds = parseTagsJson(o["tagIds"]?.jsonPrimitive?.contentOrNull)
        val tags = parseTagsJson(o["tags"]?.jsonPrimitive?.contentOrNull)
        val createdMs = parseCloudMillis(o["memoCreatedDate"]) ?: System.currentTimeMillis()
        val updatedMs = cloudUpdatedMillis(o)
        val createdAt = java.time.Instant.ofEpochMilli(createdMs).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val updatedAt = java.time.Instant.ofEpochMilli(updatedMs).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val source = when {
            plazaId != null -> V2MemoSource.PLAZA_SAVE
            else -> V2MemoSource.SELF_RECORD
        }
        val cloudThumbUrl = firstString(
            o,
            "thumbnailUrl",
            "thumbnailURL",
            "thumbnail",
            "coverUrl",
            "coverURL",
            "cover",
            "pic",
            "image",
        )
        val cloudThumbBase64Url = o["thumbnailBase64"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?.let { V2MemoThumbnailStore.saveFromBase64(appContext, id, it, ownerUserId) }
        val base = if (body.isNotBlank()) {
            V2Memo.mixedMemo(displayTitle = title, body = body, videoUrl = videoUrl, durationSec = duration, tags = tags)
        } else {
            V2Memo.videoMemo(displayTitle = title, videoUrl = videoUrl, durationSec = duration, tags = tags)
        }
        return base.copy(
            id = id,
            tagIds = tagIds,
            source = source,
            createdAt = createdAt,
            updatedAt = updatedAt,
            trainingMethod = trainingMethod,
            isOnlineVideo = isOnline,
            platform = platform,
            plazaVideoId = plazaId,
            thumbnailUrl = cloudThumbBase64Url ?: cloudThumbUrl ?: base.thumbnailUrl,
            pinState = if (o["isFavorite"]?.jsonPrimitive?.booleanOrNull == true) V2PinState.PINNED else V2PinState.NONE,
        )
    }

    private fun parseCloudInsightCard(o: JsonObject): V2Memo? {
        val cardId = o["cardId"]?.jsonPrimitive?.contentOrNull ?: return null
        val id = runCatching { UUID.fromString(cardId) }.getOrNull() ?: return null
        val status = o["status"]?.jsonPrimitive?.contentOrNull ?: "confirmed"
        if (status != "confirmed") return null
        val title = o["title"]?.jsonPrimitive?.contentOrNull ?: return null
        val summary = o["summary"]?.jsonPrimitive?.contentOrNull ?: ""
        val source = o["source"]?.jsonPrimitive?.contentOrNull ?: "manual"
        if (source == "welcome" || source == "tutorial") return null
        val updatedMs = parseCloudMillis(o["cardUpdatedAt"])
            ?: parseCloudMillis(o["updatedAt"])
            ?: System.currentTimeMillis()
        val createdMs = parseCloudMillis(o["cardCreatedAt"])
            ?: parseCloudMillis(o["createdAt"])
            ?: updatedMs
        val createdAt = java.time.Instant.ofEpochMilli(createdMs).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val updatedAt = java.time.Instant.ofEpochMilli(updatedMs).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val tags = parseTagsJson(o["tagNames"]?.jsonPrimitive?.contentOrNull)
        val priority = o["priority"]?.jsonPrimitive?.contentOrNull ?: "medium"
        val pinnedAt = parseCloudMillis(o["pinnedAt"])
        return V2Memo.textMemo(title = title, body = summary, tags = tags).copy(
            id = id,
            createdAt = createdAt,
            updatedAt = updatedAt,
            metaText = null,
            priorityRaw = priority,
            pinState = if (pinnedAt != null) V2PinState.PINNED else V2PinState.NONE,
        )
    }

    private fun V2Memo.isDemoMemo(): Boolean {
        val title = title ?: displayTitle
        val body = text.orEmpty()
        return when (title) {
            "握把压力的微调" -> body == "今天发现握把太用力会让手腕僵硬，松一点反而更顺。下次保持 6 分力试试。"
            "推杆距离感的练习" -> body == "5 米推杆把节奏拉长，先 1 下半挥杆，再回正。今天命中率明显提升。"
            "教练点评：上杆速度" -> body == "教练说上杆太快了，建议加一拍。回去录一段慢动作对比。"
            "下杆节奏练习 · 7号铁" -> body == "录了 3 球。第 2 球节奏最稳，标记为 ⭐。"
            "沙坑救球角度" -> body == "杆面打开 30°，瞄球后 5cm 落地，效果不错。"
            else -> false
        }
    }

    private fun parseTagsJson(raw: String?): List<String> = runCatching {
        if (raw.isNullOrBlank()) return emptyList()
        net.json.parseToJsonElement(raw).jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }
    }.getOrDefault(emptyList())

    private fun cloudUpdatedMillis(o: JsonObject): Long =
        parseCloudMillis(o["lastModifiedDate"])
            ?: parseCloudMillis(o["cardUpdatedAt"])
            ?: parseCloudMillis(o["updatedAt"])
            ?: System.currentTimeMillis()

    private fun parseCloudMillis(element: kotlinx.serialization.json.JsonElement?): Long? {
        if (element == null) return null
        return runCatching {
            when (element) {
                is JsonObject -> element["\$date"]?.jsonPrimitive?.contentOrNull
                    ?.let { java.time.Instant.parse(it).toEpochMilli() }
                else -> element.jsonPrimitive.longOrNull
                    ?: element.jsonPrimitive.doubleOrNull?.toLong()
                    ?: element.jsonPrimitive.contentOrNull?.toLongOrNull()
                    ?: java.time.Instant.parse(element.jsonPrimitive.contentOrNull).toEpochMilli()
            }
        }.getOrNull()
    }

    private suspend fun pushSingle(memo: V2Memo) {
        val uid = session.current().userId ?: return
        val ok = runCatching {
            when (memo.kind) {
                V2MemoKind.TEXT -> {
                    val resp = net.cloudApi.dataSync(
                        JsonObject(
                            mapOf(
                                "action" to JsonPrimitive("saveInsightCard"),
                                "userId" to JsonPrimitive(uid),
                                "cardId" to JsonPrimitive(memo.id.toString()),
                                "cardData" to buildInsightCardData(memo),
                            ),
                        ),
                    )
                    resp.success
                }
                else -> {
                    val resp = net.cloudApi.dataSync(
                        JsonObject(
                            mapOf(
                                "action" to JsonPrimitive("saveVideoMemo"),
                                "userId" to JsonPrimitive(uid),
                                "memoId" to JsonPrimitive(memo.id.toString()),
                                "memoData" to buildVideoMemoData(memo),
                            ),
                        ),
                    )
                    resp.success
                }
            }
        }.getOrDefault(false)
        if (ok) withContext(Dispatchers.IO) { dao.markClean(memo.id.toString(), uid) }
    }

    private fun buildVideoMemoData(memo: V2Memo): JsonObject {
        val createdMs = memo.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val updatedMs = memo.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val tagsJson = JsonArray(memo.tags.map { JsonPrimitive(it) }).toString()
        val tagIdsJson = JsonArray(memo.tagIds.map { JsonPrimitive(it) }).toString()
        val parentCategoryId = memo.tagIds.firstOrNull()?.let { V2CategoryStore.groupForItemId(it)?.id } ?: "technical_details"
        val subcategoryId = memo.tagIds.firstOrNull() ?: "default"
        val isOnline = memo.isOnlineVideo
        val thumbPath = if (isOnline) null else memo.videoUrl?.removePrefix("file://")
        val thumbnailBase64 = V2VideoThumbnailUtil.jpegBase64ForUpload(thumbPath)
        val remoteThumbnailUrl = remoteThumbnailUrlForUpload(memo.thumbnailUrl)
        return JsonObject(
            buildMap {
                put("title", JsonPrimitive(memo.displayTitle ?: memo.title ?: ""))
                put("memoDescription", JsonPrimitive(memo.text ?: ""))
                put("trainingMethod", JsonPrimitive(memo.trainingMethod ?: ""))
                put("parentCategoryId", JsonPrimitive(parentCategoryId))
                put("subcategoryId", JsonPrimitive(subcategoryId))
                put("tags", JsonPrimitive(tagsJson))
                put("tagIds", JsonPrimitive(tagIdsJson))
                put("category", JsonPrimitive("training"))
                put("categoryId", JsonPrimitive(""))
                put("customCategory", JsonPrimitive(""))
                put("isFavorite", JsonPrimitive(memo.isPinned))
                put("videoPath", JsonPrimitive(memo.videoUrl ?: ""))
                put("originalURL", JsonPrimitive(""))
                put("localFilePath", JsonPrimitive(if (isOnline) "" else (memo.videoUrl ?: "")))
                put("fileSize", JsonPrimitive(0))
                put("duration", JsonPrimitive(memo.durationSec ?: 0.0))
                put("priority", JsonPrimitive(0))
                put("memoCreatedDate", JsonPrimitive(createdMs))
                put("lastModifiedDate", JsonPrimitive(updatedMs))
                put("isOnlineVideo", JsonPrimitive(isOnline))
                put("onlinePlayURL", JsonPrimitive(if (isOnline) (memo.videoUrl ?: "") else ""))
                put("platform", JsonPrimitive(memo.platform ?: ""))
                put("plazaVideoId", JsonPrimitive(memo.plazaVideoId?.toString() ?: ""))
                put("photoLibraryAssetId", JsonPrimitive(""))
                put("recordedLocationName", JsonPrimitive(""))
                put("thumbnailBase64", JsonPrimitive(thumbnailBase64))
                remoteThumbnailUrl?.let {
                    put("thumbnailUrl", JsonPrimitive(it))
                    put("thumbnailURL", JsonPrimitive(it))
                }
            },
        )
    }

    private fun buildInsightCardData(memo: V2Memo): JsonObject {
        val createdMs = memo.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val updatedMs = memo.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val tagNamesJson = JsonArray(memo.tags.map { JsonPrimitive(it) }).toString()
        val pinnedMs = if (memo.isPinned) updatedMs else null
        return JsonObject(
            buildMap {
                put("title", JsonPrimitive(memo.title ?: memo.displayTitle ?: ""))
                put("summary", JsonPrimitive(memo.text ?: ""))
                put("priority", JsonPrimitive(memo.priorityRaw))
                put("status", JsonPrimitive("confirmed"))
                put("source", JsonPrimitive("manual"))
                put("cardCreatedAt", JsonPrimitive(createdMs))
                put("cardUpdatedAt", JsonPrimitive(updatedMs))
                pinnedMs?.let { put("pinnedAt", JsonPrimitive(it)) }
                put("sourceConversationId", JsonPrimitive(""))
                put("attachedVideoIds", JsonPrimitive("[]"))
                put("attachedImageIds", JsonPrimitive("[]"))
                put("tagNames", JsonPrimitive(tagNamesJson))
                put("parentCategoryId", JsonPrimitive(""))
            },
        )
    }

    private fun encode(memo: V2Memo, ownerUserId: String, dirty: Boolean): MemoEntity {
        val jsonObj = V2Json.encodeMemo(memo)
        return MemoEntity(
            id = memo.id.toString(),
            ownerUserId = ownerUserId,
            memoJson = net.json.encodeToString(JsonObject.serializer(), jsonObj),
            kind = memo.kind.name.lowercase(),
            topic = memo.topic.name.lowercase(),
            pinState = memo.pinState.name.lowercase(),
            updatedAt = memo.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            createdAt = memo.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            dirty = dirty,
        )
    }

    private fun decode(row: MemoEntity): V2Memo {
        val obj = net.json.parseToJsonElement(row.memoJson).jsonObject
        return V2Json.decodeMemo(obj)
    }

    private fun firstString(o: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            val value = runCatching {
                o[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            }.getOrNull()
            if (value != null) return value
        }
        return null
    }

    private companion object {
        const val BUCKET_VIDEO_MEMO = "videoMemo"
        const val BUCKET_INSIGHT_CARD = "insightCard"
    }
}
