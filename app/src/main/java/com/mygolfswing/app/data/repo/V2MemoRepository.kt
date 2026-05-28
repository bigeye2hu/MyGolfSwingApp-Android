package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.db.MemoDao
import com.mygolfswing.app.data.db.MemoEntity
import com.mygolfswing.app.data.json.V2Json
import com.mygolfswing.app.data.model.V2Memo
import com.mygolfswing.app.data.model.V2MemoAccent
import com.mygolfswing.app.data.model.V2MemoKind
import com.mygolfswing.app.data.model.V2MemoSource
import com.mygolfswing.app.data.model.V2PinState
import com.mygolfswing.app.data.net.V2NetClient
import com.mygolfswing.app.data.session.V2SessionStore
import com.mygolfswing.app.data.video.V2VideoThumbnailUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.ZoneId
import java.util.UUID

/**
 * Memo 仓库（对齐 iOS `VideoMemoCloudSyncService` + `InsightCardCloudSyncService`）。
 */
class V2MemoRepository(
    private val dao: MemoDao,
    private val net: V2NetClient,
    private val session: V2SessionStore,
    private val scope: CoroutineScope,
) {
    fun observeAll(): Flow<List<V2Memo>> =
        dao.observeAll().map { rows ->
            rows.mapNotNull { row -> runCatching { decode(row) }.getOrNull() }
        }

    suspend fun get(id: String): V2Memo? = withContext(Dispatchers.IO) {
        dao.get(id)?.let { runCatching { decode(it) }.getOrNull() }
    }

    suspend fun upsert(memo: V2Memo, pushToCloud: Boolean = true) = withContext(Dispatchers.IO) {
        dao.upsert(encode(memo, dirty = pushToCloud))
        if (pushToCloud) scope.launch { pushSingle(memo) }
    }

    suspend fun delete(memoId: String) = withContext(Dispatchers.IO) {
        val memo = dao.get(memoId)?.let { runCatching { decode(it) }.getOrNull() }
        dao.delete(memoId)
        scope.launch {
            val uid = session.current().userId ?: return@launch
            val action = if (memo?.kind == V2MemoKind.TEXT) "deleteInsightCard" else "deleteVideoMemo"
            runCatching {
                net.cloudApi.dataSync(
                    JsonObject(
                        mapOf(
                            "action" to JsonPrimitive(action),
                            "userId" to JsonPrimitive(uid),
                            if (memo?.kind == V2MemoKind.TEXT) {
                                "cardId" to JsonPrimitive(memoId)
                            } else {
                                "memoId" to JsonPrimitive(memoId)
                            },
                        ),
                    ),
                )
            }
        }
    }

    suspend fun bulkReplaceLocal(memos: List<V2Memo>) = withContext(Dispatchers.IO) {
        dao.upsertAll(memos.map { encode(it, dirty = false) })
    }

    suspend fun pushDirty() = withContext(Dispatchers.IO) {
        dao.dirty().mapNotNull { runCatching { decode(it) }.getOrNull() }.forEach { pushSingle(it) }
    }

    suspend fun pullFromCloud(): Result<Int> = runCatching {
        val uid = session.current().userId ?: error("未登录")
        var merged = pullVideoMemos(uid)
        merged += pullInsightCards(uid)
        merged
    }

    private suspend fun pullVideoMemos(uid: String): Int {
        val resp = net.cloudApi.dataSync(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive("getVideoMemos"),
                    "userId" to JsonPrimitive(uid),
                ),
            ),
        )
        if (!resp.success) error(resp.error ?: "拉取视频 Memo 失败")
        val list = (resp.data as? JsonArray) ?: JsonArray(emptyList())
        var merged = 0
        for (element in list) {
            val cloud = element.jsonObject
            val memo = parseCloudVideoMemo(cloud) ?: continue
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
                ),
            ),
        )
        if (!resp.success) error(resp.error ?: "拉取随手记失败")
        val list = (resp.data as? JsonArray) ?: JsonArray(emptyList())
        var merged = 0
        for (element in list) {
            val cloud = element.jsonObject
            val memo = parseCloudInsightCard(cloud) ?: continue
            if (mergeIfNewer(memo, cloud)) merged++
        }
        return merged
    }

    private suspend fun mergeIfNewer(memo: V2Memo, cloud: JsonObject): Boolean {
        val localRow = dao.get(memo.id.toString())
        if (localRow == null) {
            dao.upsert(encode(memo, dirty = false))
            return true
        }
        val localMemo = decode(localRow)
        val cloudUpdated = cloudUpdatedMillis(cloud)
        val localUpdated = localMemo.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (cloudUpdated >= localUpdated) {
            dao.upsert(encode(memo, dirty = false))
            return true
        }
        return false
    }

    private fun parseCloudVideoMemo(o: JsonObject): V2Memo? {
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
        val tags = parseTagsJson(o["tags"]?.jsonPrimitive?.contentOrNull)
        val createdMs = parseCloudMillis(o["memoCreatedDate"]) ?: System.currentTimeMillis()
        val updatedMs = cloudUpdatedMillis(o)
        val createdAt = java.time.Instant.ofEpochMilli(createdMs).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val updatedAt = java.time.Instant.ofEpochMilli(updatedMs).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val source = when {
            plazaId != null -> V2MemoSource.PLAZA_SAVE
            else -> V2MemoSource.SELF_RECORD
        }
        val base = if (body.isNotBlank()) {
            V2Memo.mixedMemo(displayTitle = title, body = body, videoUrl = videoUrl, durationSec = duration, tags = tags)
        } else {
            V2Memo.videoMemo(displayTitle = title, videoUrl = videoUrl, durationSec = duration, tags = tags)
        }
        return base.copy(
            id = id,
            source = source,
            createdAt = createdAt,
            updatedAt = updatedAt,
            trainingMethod = trainingMethod,
            isOnlineVideo = isOnline,
            platform = platform,
            plazaVideoId = plazaId,
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
        val createdMs = parseCloudMillis(o["cardCreatedAt"]) ?: System.currentTimeMillis()
        val updatedMs = parseCloudMillis(o["cardUpdatedAt"]) ?: createdMs
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
            element.jsonPrimitive.longOrNull
                ?: element.jsonPrimitive.contentOrNull?.toLongOrNull()
                ?: java.time.Instant.parse(element.jsonPrimitive.contentOrNull).toEpochMilli()
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
        if (ok) withContext(Dispatchers.IO) { dao.markClean(memo.id.toString()) }
    }

    private fun buildVideoMemoData(memo: V2Memo): JsonObject {
        val createdMs = memo.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val updatedMs = memo.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val tagsJson = JsonArray(memo.tags.map { JsonPrimitive(it) }).toString()
        val isOnline = memo.isOnlineVideo
        val thumbPath = if (isOnline) null else memo.videoUrl?.removePrefix("file://")
        val thumbnailBase64 = V2VideoThumbnailUtil.jpegBase64ForUpload(thumbPath)
        return JsonObject(
            mapOf(
                "title" to JsonPrimitive(memo.displayTitle ?: memo.title ?: ""),
                "memoDescription" to JsonPrimitive(memo.text ?: ""),
                "trainingMethod" to JsonPrimitive(memo.trainingMethod ?: ""),
                "parentCategoryId" to JsonPrimitive(""),
                "subcategoryId" to JsonPrimitive(""),
                "tags" to JsonPrimitive(tagsJson),
                "category" to JsonPrimitive("training"),
                "categoryId" to JsonPrimitive(""),
                "customCategory" to JsonPrimitive(""),
                "isFavorite" to JsonPrimitive(memo.isPinned),
                "videoPath" to JsonPrimitive(memo.videoUrl ?: ""),
                "originalURL" to JsonPrimitive(""),
                "localFilePath" to JsonPrimitive(if (isOnline) "" else (memo.videoUrl ?: "")),
                "fileSize" to JsonPrimitive(0),
                "duration" to JsonPrimitive(memo.durationSec ?: 0.0),
                "priority" to JsonPrimitive(0),
                "memoCreatedDate" to JsonPrimitive(createdMs),
                "lastModifiedDate" to JsonPrimitive(updatedMs),
                "isOnlineVideo" to JsonPrimitive(isOnline),
                "onlinePlayURL" to JsonPrimitive(if (isOnline) (memo.videoUrl ?: "") else ""),
                "platform" to JsonPrimitive(memo.platform ?: ""),
                "plazaVideoId" to JsonPrimitive(memo.plazaVideoId?.toString() ?: ""),
                "photoLibraryAssetId" to JsonPrimitive(""),
                "recordedLocationName" to JsonPrimitive(""),
                "thumbnailBase64" to JsonPrimitive(thumbnailBase64),
            ),
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

    private fun encode(memo: V2Memo, dirty: Boolean): MemoEntity {
        val jsonObj = V2Json.encodeMemo(memo)
        return MemoEntity(
            id = memo.id.toString(),
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
}
