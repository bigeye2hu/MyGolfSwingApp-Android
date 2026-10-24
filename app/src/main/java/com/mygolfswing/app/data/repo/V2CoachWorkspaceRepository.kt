package com.mygolfswing.app.data.repo

import com.mygolfswing.app.data.coach.V2CoachConnection
import com.mygolfswing.app.data.coach.V2CoachConnectionActivitySnapshot
import com.mygolfswing.app.data.coach.V2CoachDashboard
import com.mygolfswing.app.data.coach.V2CoachDashboardUpdate
import com.mygolfswing.app.data.coach.V2CoachInvite
import com.mygolfswing.app.data.coach.V2CoachInvitePreview
import com.mygolfswing.app.data.coach.V2CoachInviteStatus
import com.mygolfswing.app.data.coach.V2CoachLibraryAsset
import com.mygolfswing.app.data.coach.V2CoachMetrics
import com.mygolfswing.app.data.coach.V2CoachPage
import com.mygolfswing.app.data.coach.V2CoachQuickReply
import com.mygolfswing.app.data.coach.V2CoachProfileInput
import com.mygolfswing.app.data.coach.V2CoachSnapshot
import com.mygolfswing.app.data.coach.V2CoachUserSummary
import com.mygolfswing.app.data.coach.V2CoachWorkspaceItem
import com.mygolfswing.app.data.coach.V2CoachWorkspaceAsset
import com.mygolfswing.app.data.coach.V2CoachThread
import com.mygolfswing.app.data.coach.V2CoachUploadGrant
import com.mygolfswing.app.data.coach.V2CoachUserFacingException
import com.mygolfswing.app.data.coach.V2PendingCoachInvite
import com.mygolfswing.app.data.coach.V2StudentUnreadSnapshot
import com.mygolfswing.app.data.coach.V2StudentUnreadUpdate
import com.mygolfswing.app.data.coach.V2SpaceConversationPage
import com.mygolfswing.app.data.coach.V2SpaceDirectMessage
import com.mygolfswing.app.data.coach.V2SpacePublicSource
import com.mygolfswing.app.data.coach.V2WorkspaceContentMode
import com.mygolfswing.app.data.coach.V2WorkspaceFeedUpdate
import com.mygolfswing.app.data.net.V2NetClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer

class V2CoachWorkspaceRepository(
    private val net: V2NetClient,
    private val auth: V2AuthRepository,
) {

    suspend fun loadSnapshot(): Result<V2CoachSnapshot> = coachCatching {
        coroutineScope {
            val dashboard = async { dashboardUpdate() }
            val connections = async { connections() }
            val review = async { reviewQueuePage(limit = 50) }
            val library = async { libraryPage(limit = 50) }
            val reviewPage = review.await()
            val libraryPage = library.await()
            val dashboardUpdate = dashboard.await()
            V2CoachSnapshot(
                dashboard = dashboardUpdate.snapshot ?: V2CoachDashboard(),
                connections = connections.await(),
                reviewQueue = reviewPage.values,
                library = libraryPage.values,
                reviewNextCursor = reviewPage.nextCursor,
                reviewTotal = reviewPage.total,
                reviewVersion = reviewPage.version,
                libraryNextCursor = libraryPage.nextCursor,
                dashboardVersion = dashboardUpdate.version,
            )
        }
    }

    suspend fun dashboard(): V2CoachDashboard =
        dashboardUpdate().snapshot ?: V2CoachDashboard()

    suspend fun dashboardUpdate(knownVersion: String? = null): V2CoachDashboardUpdate {
        val fields = if (knownVersion.isNullOrBlank()) emptyArray()
        else arrayOf("knownDashboardVersion" to knownVersion)
        val data = call("getCoachDashboard", *fields)
        return V2CoachDashboardUpdate(
            snapshot = if (data.bool("unchanged")) null
            else (data["dashboard"].objectOrNull() ?: JsonObject(emptyMap())).toCoachDashboard(),
            version = data.string("dashboardVersion") ?: knownVersion,
        )
    }

    suspend fun coachMetrics(): Result<V2CoachMetrics> = coachCatching {
        val data = call("getCoachMetrics")["metrics"].objectOrNull() ?: JsonObject(emptyMap())
        V2CoachMetrics(
            receivedCount = data.int("receivedCount"),
            readyAssetCount = data.int("readyAssetCount"),
            totalVideoBytes = data.nonNegativeDouble("totalVideoBytes"),
            totalUploadCount = data.int("totalUploadCount"),
            failedUploadCount = data.int("failedUploadCount"),
            firstViewAverageSeconds = data.nonNegativeDouble("firstViewAverageSeconds"),
            feedbackCompletionRate = data.nonNegativeDouble("feedbackCompletionRate"),
            feedbackAverageSeconds = data.nonNegativeDouble("feedbackAverageSeconds"),
            averageVideoBytes = data.nonNegativeDouble("averageVideoBytes"),
            averageUploadSeconds = data.nonNegativeDouble("averageUploadSeconds"),
            uploadFailureRate = data.nonNegativeDouble("uploadFailureRate"),
            playbackCount = data.int("playbackCount"),
            assetReuseRate = data.nonNegativeDouble("assetReuseRate"),
            totalSourceVideoBytes = data.nonNegativeDouble("totalSourceVideoBytes"),
            compressionSavingsRate = data.nonNegativeDouble("compressionSavingsRate"),
            averageProcessingSeconds = data.nonNegativeDouble("averageProcessingSeconds"),
            directUploadRate = data.nonNegativeDouble("directUploadRate"),
            cellularUploadCount = data.int("cellularUploadCount"),
            optimizedUploadCount = data.int("optimizedUploadCount"),
        )
    }

    suspend fun connections(): List<V2CoachConnection> =
        call("listMyConnections", "perspective" to "coach", "includeDisconnected" to true)
            .array("connections")
            .mapNotNull { it.objectOrNull()?.toConnection() }

    suspend fun studentConnections(): Result<List<V2CoachConnection>> = coachCatching {
        call("listMyConnections", "perspective" to "student", "includeDisconnected" to true)
            .array("connections")
            .mapNotNull { it.objectOrNull()?.toConnection() }
    }

    suspend fun syncFriendSpaces(friendUserIds: List<String>): Result<List<V2CoachConnection>> = coachCatching {
        call("syncFriendSpaces", "friendUserIds" to friendUserIds.distinct().take(200))
            .array("connections")
            .mapNotNull { it.objectOrNull()?.toConnection() }
    }

    suspend fun spaceSource(connectionId: String, messageId: String): Result<V2SpacePublicSource> = coachCatching {
        val data = call("getSpaceSource", "connectionId" to connectionId, "messageId" to messageId)
        data["publicSource"].objectOrNull()?.toSpacePublicSource()
            ?: userError("引用内容暂时不可用")
    }

    suspend fun listSpaceConversation(
        connectionId: String,
        cursor: String? = null,
        afterCursor: String? = null,
    ): Result<V2SpaceConversationPage> = coachCatching {
        val fields = buildList<Pair<String, Any?>> {
            add("connectionId" to connectionId)
            cursor?.takeIf(String::isNotBlank)?.let { add("cursor" to it) }
            afterCursor?.takeIf(String::isNotBlank)?.let { add("afterCursor" to it) }
        }
        val data = call("listSpaceConversation", *fields.toTypedArray())
        V2SpaceConversationPage(
            messages = data.array("directMessages").mapNotNull { it.objectOrNull()?.toSpaceDirectMessage() },
            hasUnread = data.bool("directHasUnread"),
            readPosition = data.string("directReadPosition")
                ?: userError("对话读取位置不完整，请刷新重试"),
            nextCursor = data.string("nextCursor"),
            latestCursor = data.string("directLatestCursor"),
            hasMoreNew = data.bool("directHasMoreNew"),
        )
    }

    suspend fun sendSpaceMessage(
        connectionId: String,
        text: String,
        clientRequestId: String = UUID.randomUUID().toString(),
    ): Result<V2SpaceDirectMessage> = coachCatching {
        val data = call(
            "sendSpaceMessage",
            "connectionId" to connectionId,
            "clientRequestId" to clientRequestId,
            "text" to text.trim().take(2_000),
        )
        data.array("directMessages").firstOrNull()?.objectOrNull()?.toSpaceDirectMessage()
            ?: userError("消息发送结果不完整，请刷新确认")
    }

    suspend fun markSpaceConversationRead(connectionId: String, readPosition: String): Result<Boolean> = coachCatching {
        call(
            "markSpaceConversationRead",
            "connectionId" to connectionId,
            "readPosition" to readPosition,
        ).bool("directHasUnread")
    }

    suspend fun updateSpacePinned(connectionId: String, pinned: Boolean): Result<V2CoachConnection> = coachCatching {
        val data = call("updateSpacePinned", "connectionId" to connectionId, "pinned" to pinned)
        data["connection"].objectOrNull()?.toConnection() ?: userError("空间更新结果不完整，请重试")
    }

    suspend fun studentUnread(knownVersion: String? = null): Result<V2StudentUnreadUpdate> = coachCatching {
        val fields = if (knownVersion.isNullOrBlank()) emptyArray()
        else arrayOf("knownStudentUnreadVersion" to knownVersion)
        val data = call("getStudentUnreadCounts", *fields)
        val version = data.string("studentUnreadVersion") ?: knownVersion
        if (data.bool("unchanged")) {
            V2StudentUnreadUpdate(snapshot = null, version = version)
        } else {
            val unread = data["studentUnread"].objectOrNull() ?: JsonObject(emptyMap())
            V2StudentUnreadUpdate(
                snapshot = V2StudentUnreadSnapshot(
                    total = unread.int("total"),
                    byConnection = unread.intMap("byConnection"),
                    unreadItemIdByConnection = unread.stringMap("unreadItemIdByConnection"),
                    recentItems = unread.array("recentItems").mapNotNull { it.objectOrNull()?.toWorkspaceItem() },
                    refreshedAt = unread.string("refreshedAt"),
                ),
                version = version,
            )
        }
    }

    suspend fun previewInvite(inviteCode: String): Result<V2CoachInvitePreview> = coachCatching {
        val data = call("previewInviteCode", "inviteCode" to inviteCode.trim().uppercase())
        val coach = data["coach"].objectOrNull()?.toUser() ?: userError("邀请信息不完整，请重新打开")
        V2CoachInvitePreview(
            inviteId = data.string("inviteId") ?: userError("邀请信息不完整，请重新打开"),
            expiresAt = data.string("expiresAt"),
            coach = coach,
        )
    }

    suspend fun previewInvite(invite: V2PendingCoachInvite): Result<V2CoachInvitePreview> = coachCatching {
        val data = call("getInvitePreview", "inviteId" to invite.inviteId, "inviteToken" to invite.inviteToken)
        val coach = data["coach"].objectOrNull()?.toUser() ?: userError("邀请信息不完整，请重新打开")
        V2CoachInvitePreview(
            inviteId = data.string("inviteId") ?: invite.inviteId,
            expiresAt = data.string("expiresAt"),
            coach = coach,
        )
    }

    suspend fun acceptInvite(inviteCode: String): Result<V2CoachConnection> = coachCatching {
        val data = call("acceptInviteCode", "inviteCode" to inviteCode.trim().uppercase())
        data["connection"].objectOrNull()?.toConnection() ?: userError("连接信息不完整，请刷新后重试")
    }

    suspend fun acceptInvite(invite: V2PendingCoachInvite): Result<V2CoachConnection> = coachCatching {
        val data = call(
            "acceptInviteCode",
            "inviteId" to invite.inviteId,
            "inviteToken" to invite.inviteToken,
        )
        data["connection"].objectOrNull()?.toConnection() ?: userError("连接信息不完整，请刷新后重试")
    }

    suspend fun workspaceFeed(connectionId: String): Result<List<V2CoachWorkspaceItem>> =
        workspaceFeedUpdate(connectionId).map { update -> update.items.orEmpty() }

    suspend fun workspaceFeedUpdate(
        connectionId: String,
        knownVersion: String? = null,
        cursor: String? = null,
        limit: Int = 50,
    ): Result<V2WorkspaceFeedUpdate> = coachCatching {
        val fields = buildList<Pair<String, Any?>> {
            add("connectionId" to connectionId)
            add("limit" to limit.coerceIn(1, 50))
            cursor?.takeIf(String::isNotBlank)?.let { add("cursor" to it) }
            if (cursor.isNullOrBlank()) {
                knownVersion?.takeIf(String::isNotBlank)?.let { add("knownFeedVersion" to it) }
            }
        }
        val data = call("listWorkspaceFeed", *fields.toTypedArray())
        V2WorkspaceFeedUpdate(
            items = if (data.bool("unchanged")) null else data.array("items")
                .mapNotNull { it.objectOrNull()?.toWorkspaceItem() },
            nextCursor = data.string("nextCursor"),
            version = data.string("feedVersion") ?: knownVersion.takeIf { cursor.isNullOrBlank() },
        )
    }

    suspend fun workspaceThread(
        connectionId: String,
        itemId: String,
        cursor: String? = null,
    ): Result<V2CoachThread> =
        workspaceThreadUpdate(connectionId, itemId, cursor = cursor).map { thread ->
            thread ?: userError("交流内容不可用，请刷新后重试")
        }

    suspend fun workspaceThreadUpdate(
        connectionId: String,
        itemId: String,
        knownVersion: String? = null,
        knownCheckVersion: String? = null,
        cursor: String? = null,
    ): Result<V2CoachThread?> = coachCatching {
        val fields = buildList<Pair<String, Any?>> {
            add("connectionId" to connectionId)
            add("itemId" to itemId)
            cursor?.takeIf(String::isNotBlank)?.let { add("cursor" to it) }
            if (cursor.isNullOrBlank()) {
                knownVersion?.takeIf(String::isNotBlank)?.let { add("knownThreadVersion" to it) }
                knownCheckVersion?.takeIf(String::isNotBlank)?.let { add("knownThreadCheckVersion" to it) }
            }
        }
        val data = call("getWorkspaceThread", *fields.toTypedArray())
        if (data.bool("unchanged")) return@coachCatching null
        data.toCoachThread() ?: userError("交流内容不可用，请刷新后重试")
    }

    suspend fun createTextPost(
        connectionId: String,
        text: String,
        requiresCoachReview: Boolean = true,
        contentMode: V2WorkspaceContentMode = V2WorkspaceContentMode.PRACTICE,
        clientRequestId: String = UUID.randomUUID().toString(),
        attachmentAssetIds: List<String> = emptyList(),
        saveAssetIdsToLibrary: List<String> = emptyList(),
    ): Result<V2CoachWorkspaceItem> = coachCatching {
        val data = call(
            "createWorkspacePost",
            "connectionId" to connectionId,
            "clientRequestId" to clientRequestId,
            "text" to text.trim(),
            "title" to "",
            "attachmentAssetIds" to attachmentAssetIds.distinct().take(3),
            "saveAssetIdsToLibrary" to saveAssetIdsToLibrary.distinct().take(4),
            "requiresCoachReview" to requiresCoachReview,
            "contentMode" to contentMode.wireValue,
        )
        data["item"].objectOrNull()?.toWorkspaceItem() ?: userError("发送结果不完整，请刷新确认")
    }

    suspend fun createAssetPost(
        connectionId: String,
        title: String,
        text: String,
        assetId: String,
        clientRequestId: String,
        requiresCoachReview: Boolean = false,
        contentMode: V2WorkspaceContentMode = V2WorkspaceContentMode.PRACTICE,
        attachmentAssetIds: List<String> = emptyList(),
        saveAssetIdsToLibrary: List<String> = emptyList(),
        sourceRecordId: String? = null,
    ): Result<V2CoachWorkspaceItem> = coachCatching {
        val data = call(
            "createWorkspacePost",
            "connectionId" to connectionId,
            "clientRequestId" to clientRequestId,
            "text" to text.trim().take(1_000),
            "title" to title.trim().take(80),
            "primaryAssetId" to assetId,
            "attachmentAssetIds" to attachmentAssetIds.distinct().take(3),
            "saveAssetIdsToLibrary" to saveAssetIdsToLibrary.distinct().take(4),
            "requiresCoachReview" to requiresCoachReview,
            "contentMode" to contentMode.wireValue,
            "sourceRecordId" to sourceRecordId,
        )
        data["item"].objectOrNull()?.toWorkspaceItem() ?: userError("发送结果不完整，请刷新确认")
    }

    suspend fun uploadWorkspaceVideo(
        connectionId: String?,
        videoFile: File,
        coverFile: File,
        clientUploadId: String,
        title: String,
        tags: List<String>,
        durationSeconds: Double?,
        sourceKind: String = "personal_record",
        savedToLibrary: Boolean = false,
        onUploadProgress: (Double) -> Unit = {},
        onPublishing: () -> Unit = {},
    ): Result<String> = coachCatching {
        if (!videoFile.isFile || videoFile.length() <= 0L) userError("视频文件不可用，请重新选择")
        if (videoFile.length() > 500L * 1024L * 1024L) userError("视频文件需要小于500MB")
        if (!coverFile.isFile || coverFile.length() <= 0L) userError("无法生成视频封面，请重新选择视频")

        val extension = videoFile.extension.lowercase(Locale.ROOT).takeIf { it in setOf("mp4", "mov", "m4v") } ?: "mp4"
        val contentType = when (extension) {
            "mov" -> "video/quicktime"
            "m4v" -> "video/x-m4v"
            else -> "video/mp4"
        }
        val prepare = call(
            "prepareAssetUpload",
            "connectionId" to connectionId,
            "clientUploadId" to clientUploadId,
            "fileExtension" to extension,
            "contentType" to contentType,
            "fileSizeBytes" to videoFile.length(),
            "sourceKind" to sourceKind,
            "title" to title.take(80),
            "tags" to tags.map { it.take(20) }.distinct().take(5),
            "savedToLibrary" to savedToLibrary,
            "durationSeconds" to durationSeconds?.takeIf { it.isFinite() && it > 0 },
            "mediaPreparation" to "direct",
        )
        prepare["asset"].objectOrNull()?.string("assetId", "_id")?.let { return@coachCatching it }
        val grant = prepare["upload"].objectOrNull()?.toUploadGrant()
            ?: userError("暂时无法获取上传通道，请重试")
        val coverUrl = grant.coverUploadUrl ?: userError("暂时无法获取封面上传通道，请重试")
        val coverAuthorization = grant.coverAuthorization ?: userError("暂时无法获取封面上传通道，请重试")
        val coverPath = grant.coverCloudPath ?: userError("暂时无法获取封面上传通道，请重试")

        try {
            uploadMultipart(
                url = grant.uploadUrl,
                authorization = grant.authorization,
                securityToken = grant.securityToken,
                cosFileId = grant.cosFileId,
                cloudPath = grant.cloudPath,
                file = videoFile,
                contentType = grant.contentType,
                onProgress = { onUploadProgress(it * 0.96) },
            )
            uploadMultipart(
                url = coverUrl,
                authorization = coverAuthorization,
                securityToken = grant.coverSecurityToken,
                cosFileId = grant.coverCosFileId,
                cloudPath = coverPath,
                file = coverFile,
                contentType = "image/jpeg",
                onProgress = { onUploadProgress(0.96 + it * 0.04) },
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (uploadError: Throwable) {
            // COS 可能已经接收视频，只是客户端没收到最终响应。先补传封面，
            // 再让服务端校验并完成入库；不能留下指向不存在封面的半成品动态。
            val coverReady = runCatching {
                uploadMultipart(
                    url = coverUrl,
                    authorization = coverAuthorization,
                    securityToken = grant.coverSecurityToken,
                    cosFileId = grant.coverCosFileId,
                    cloudPath = coverPath,
                    file = coverFile,
                    contentType = "image/jpeg",
                    onProgress = { onUploadProgress(0.96 + it * 0.04) },
                )
            }.isSuccess
            if (coverReady) {
                completeAsset(grant.assetId, title, tags, durationSeconds, savedToLibrary).getOrNull()?.let {
                    return@coachCatching it
                }
            }
            throw uploadError
        }

        var lastError: Throwable? = null
        onPublishing()
        for (waitMs in listOf(0L, 500L, 1_500L, 3_000L)) {
            if (waitMs > 0) delay(waitMs)
            val result = completeAsset(grant.assetId, title, tags, durationSeconds, savedToLibrary)
            result.getOrNull()?.let { return@coachCatching it }
            lastError = result.exceptionOrNull()
        }
        throw lastError ?: V2CoachUserFacingException("视频已上传，但暂时无法完成入库，请重试")
    }

    suspend fun uploadWorkspaceAudio(
        connectionId: String,
        audioFile: File,
        clientUploadId: String,
        durationSeconds: Double?,
        onUploadProgress: (Double) -> Unit = {},
        onPublishing: () -> Unit = {},
    ): Result<String> = coachCatching {
        if (!audioFile.isFile || audioFile.length() <= 0L) userError("语音文件不可用，请重新录制")
        if (audioFile.length() > 500L * 1024L * 1024L) userError("语音文件过大，请重新录制")
        val prepare = call(
            "prepareAssetUpload",
            "connectionId" to connectionId,
            "clientUploadId" to clientUploadId,
            "fileExtension" to "m4a",
            "contentType" to "audio/mp4",
            "fileSizeBytes" to audioFile.length(),
            "sourceKind" to "voice_recording",
            "title" to "语音消息",
            "tags" to emptyList<String>(),
            "savedToLibrary" to false,
            "durationSeconds" to durationSeconds?.takeIf { it.isFinite() && it > 0 },
            "mediaPreparation" to "direct",
        )
        prepare["asset"].objectOrNull()?.string("assetId", "_id")?.let { return@coachCatching it }
        val grant = prepare["upload"].objectOrNull()?.toUploadGrant()
            ?: userError("暂时无法获取语音上传通道，请重试")
        uploadMultipart(
            url = grant.uploadUrl,
            authorization = grant.authorization,
            securityToken = grant.securityToken,
            cosFileId = grant.cosFileId,
            cloudPath = grant.cloudPath,
            file = audioFile,
            contentType = "audio/mp4",
            onProgress = onUploadProgress,
        )
        var lastError: Throwable? = null
        onPublishing()
        for (waitMs in listOf(0L, 500L, 1_500L, 3_000L)) {
            if (waitMs > 0) delay(waitMs)
            val result = completeAsset(grant.assetId, "语音消息", emptyList(), durationSeconds, false)
            result.getOrNull()?.let { return@coachCatching it }
            lastError = result.exceptionOrNull()
        }
        throw lastError ?: V2CoachUserFacingException("语音已上传，但暂时无法完成发送，请重试")
    }

    suspend fun shareLibraryAsset(
        assetId: String,
        connectionIds: List<String>,
        title: String,
        text: String,
        clientRequestId: String,
    ): Result<List<V2CoachWorkspaceItem>> = coachCatching {
        if (connectionIds.isEmpty()) userError("请至少选择一位学员")
        val data = call(
            "shareLibraryAsset",
            "assetId" to assetId,
            "connectionIds" to connectionIds.distinct().take(20),
            "clientRequestId" to clientRequestId,
            "title" to title.trim().take(80),
            "text" to text.trim().take(1_000),
        )
        data.array("items").mapNotNull { it.objectOrNull()?.toWorkspaceItem() }
            .takeIf { it.isNotEmpty() } ?: userError("发送结果不完整，请刷新确认")
    }

    suspend fun createTextReply(
        connectionId: String,
        parentItemId: String,
        text: String,
        replyToItemId: String? = null,
        replyAtSeconds: Double? = null,
        clientRequestId: String = UUID.randomUUID().toString(),
        attachmentAssetIds: List<String> = emptyList(),
        saveAssetIdsToLibrary: List<String> = emptyList(),
    ): Result<V2CoachWorkspaceItem> = coachCatching {
        val data = call(
            "createFeedback",
            "connectionId" to connectionId,
            "parentItemId" to parentItemId,
            "clientRequestId" to clientRequestId,
            "text" to text.trim(),
            "title" to "",
            "attachmentAssetIds" to attachmentAssetIds.distinct().take(3),
            "saveAssetIdsToLibrary" to saveAssetIdsToLibrary.distinct().take(4),
            "replyToItemId" to replyToItemId,
            "replyAtSeconds" to replyAtSeconds,
        )
        data["item"].objectOrNull()?.toWorkspaceItem() ?: userError("回复结果不完整，请刷新确认")
    }

    suspend fun createAssetReply(
        connectionId: String,
        parentItemId: String,
        text: String,
        assetId: String,
        clientRequestId: String,
        replyToItemId: String? = null,
        replyAtSeconds: Double? = null,
        attachmentAssetIds: List<String> = emptyList(),
        saveAssetIdsToLibrary: List<String> = emptyList(),
    ): Result<V2CoachWorkspaceItem> = coachCatching {
        val data = call(
            "createFeedback",
            "connectionId" to connectionId,
            "parentItemId" to parentItemId,
            "clientRequestId" to clientRequestId,
            "text" to text.trim().take(1_000),
            "title" to "视频回复",
            "primaryAssetId" to assetId,
            "attachmentAssetIds" to attachmentAssetIds.distinct().take(3),
            "saveAssetIdsToLibrary" to saveAssetIdsToLibrary.distinct().take(4),
            "replyToItemId" to replyToItemId,
            "replyAtSeconds" to replyAtSeconds,
        )
        data["item"].objectOrNull()?.toWorkspaceItem() ?: userError("回复结果不完整，请刷新确认")
    }

    suspend fun markViewed(itemId: String): Result<Unit> = coachCatching {
        call("markWorkspaceItemViewed", "itemId" to itemId)
        Unit
    }

    suspend fun recordAssetPlayback(connectionId: String, assetId: String): Result<Unit> = coachCatching {
        call("recordAssetPlayback", "connectionId" to connectionId, "assetId" to assetId)
        Unit
    }

    suspend fun deleteWorkspaceItem(connectionId: String, itemId: String): Result<Unit> = coachCatching {
        call("deleteWorkspaceItem", "connectionId" to connectionId, "itemId" to itemId)
        Unit
    }

    suspend fun disconnectConnection(connectionId: String): Result<Unit> = coachCatching {
        call("disconnectConnection", "connectionId" to connectionId)
        Unit
    }

    suspend fun updateCoachProfile(
        connectionId: String,
        input: V2CoachProfileInput,
    ): Result<V2CoachConnection> = coachCatching {
        if (input.weeklyPracticeGoal != null && input.weeklyPracticeGoal !in 1..14) {
            userError("每周练习目标需要在1到14次之间")
        }
        val data = call(
            "updateCoachConnectionPreferences",
            "connectionId" to connectionId,
            "coachPrivateNote" to input.privateNote.take(300),
            "coachPinned" to input.pinned,
            "coachCategory" to input.category.toJsonOrNull(),
            "coachFollowUpAt" to input.followUpAt.toJsonOrNull(),
            "coachTrainingStage" to input.trainingStage.take(30),
            "coachPrimaryGoal" to input.primaryGoal.take(100),
            "coachTrainingFocus" to input.trainingFocus.take(120),
            "coachFocusTags" to input.focusTags.map { it.take(20) }.distinct().take(8),
            "coachLessonMode" to input.lessonMode.wireValue,
            "coachTrainingGoalTags" to input.trainingGoals.map { it.wireValue }.distinct().take(3),
            "coachAgeTag" to (input.studentAge?.wireValue ?: JsonNull),
            "coachWeeklyPracticeGoal" to (input.weeklyPracticeGoal ?: JsonNull),
            "coachCadence" to input.cadence.toJsonOrNull(),
            "coachCommunicationPreference" to input.communicationPreference.toJsonOrNull(),
            "coachProfileCompleted" to input.isComplete,
        )
        data["connection"].objectOrNull()?.toConnection() ?: userError("保存结果不完整，请刷新确认")
    }

    /** Lightweight header action that preserves every teaching-profile field server-side. */
    suspend fun updateCoachPinned(
        connection: V2CoachConnection,
        pinned: Boolean,
    ): Result<V2CoachConnection> = coachCatching {
        val data = call(
            "updateCoachConnectionPreferences",
            "connectionId" to connection.connectionId,
            "coachPrivateNote" to connection.privateNote.orEmpty().take(300),
            "coachPinned" to pinned,
            "coachCategory" to connection.category.toJsonOrNull(),
            "coachFollowUpAt" to connection.followUpAt.toJsonOrNull(),
        )
        data["connection"].objectOrNull()?.toConnection() ?: userError("置顶结果不完整，请刷新确认")
    }

    suspend fun reviewQueue(filter: String = "pending"): List<V2CoachWorkspaceItem> =
        reviewQueuePage(filter = filter, limit = 50).values

    suspend fun reviewQueuePage(
        filter: String = "pending",
        cursor: String? = null,
        limit: Int = 20,
        knownVersion: String? = null,
    ): V2CoachPage<V2CoachWorkspaceItem> {
        val normalizedFilter = filter.takeIf { it in setOf("new", "viewed", "responded", "pending", "all") }
            ?: "pending"
        val fields = buildList<Pair<String, Any?>> {
            add("filter" to normalizedFilter)
            add("limit" to limit.coerceIn(1, 50))
            add("previewOnly" to true)
            cursor?.takeIf(String::isNotBlank)?.let { add("cursor" to it) }
            if (cursor.isNullOrBlank()) {
                knownVersion?.takeIf(String::isNotBlank)?.let { add("knownQueueVersion" to it) }
            }
        }
        val data = call(
            "listReviewQueue",
            *fields.toTypedArray(),
        )
        return V2CoachPage(
            values = if (data.bool("unchanged")) emptyList() else data.array("items")
                .mapNotNull { it.objectOrNull()?.toWorkspaceItem() },
            nextCursor = data.string("nextCursor"),
            total = data.nullableInt("total"),
            version = data.string("queueVersion") ?: knownVersion.takeIf { cursor.isNullOrBlank() },
            unchanged = data.bool("unchanged"),
        )
    }

    suspend fun library(query: String = ""): List<V2CoachLibraryAsset> =
        libraryPage(query = query, limit = 50).values

    suspend fun libraryPage(
        query: String = "",
        cursor: String? = null,
        limit: Int = 20,
    ): V2CoachPage<V2CoachLibraryAsset> {
        val fields = buildList<Pair<String, Any?>> {
            add("query" to query.trim().take(60))
            add("limit" to limit.coerceIn(1, 50))
            cursor?.takeIf(String::isNotBlank)?.let { add("cursor" to it) }
        }
        val data = call("listCoachLibrary", *fields.toTypedArray())
        return V2CoachPage(
            values = data.array("assets").mapNotNull { it.objectOrNull()?.toLibraryAsset() },
            nextCursor = data.string("nextCursor"),
        )
    }

    suspend fun coachAsset(assetId: String): Result<V2CoachLibraryAsset> = coachCatching {
        val data = call("getCoachAsset", "assetId" to assetId)
        data["asset"].objectOrNull()?.toLibraryAsset() ?: userError("素材播放信息不完整，请稍后重试")
    }

    suspend fun quickReplies(): Result<List<V2CoachQuickReply>> = coachCatching {
        call("listCoachQuickReplies")
            .array("quickReplies")
            .mapNotNull { it.objectOrNull()?.toQuickReply() }
    }

    suspend fun saveQuickReply(
        quickReplyId: String?,
        title: String,
        text: String,
    ): Result<V2CoachQuickReply> = coachCatching {
        val data = call(
            "saveCoachQuickReply",
            "quickReplyId" to quickReplyId,
            "title" to title.trim().take(40),
            "text" to text.trim().take(1_000),
        )
        data["quickReply"].objectOrNull()?.toQuickReply() ?: userError("常用回复保存结果不完整，请刷新确认")
    }

    suspend fun deleteQuickReply(quickReplyId: String): Result<Unit> = coachCatching {
        call("deleteCoachQuickReply", "quickReplyId" to quickReplyId)
        Unit
    }

    suspend fun markQuickReplyUsed(quickReplyId: String): Result<V2CoachQuickReply> = coachCatching {
        val data = call("markCoachQuickReplyUsed", "quickReplyId" to quickReplyId)
        data["quickReply"].objectOrNull()?.toQuickReply() ?: userError("常用回复更新结果不完整，请刷新确认")
    }

    suspend fun updateLibraryAsset(
        assetId: String,
        title: String? = null,
        tags: List<String>? = null,
        savedToLibrary: Boolean? = null,
    ): Result<V2CoachLibraryAsset> = coachCatching {
        val fields = buildList<Pair<String, Any?>> {
            add("assetId" to assetId)
            title?.let { add("title" to it.trim().take(80)) }
            tags?.let { values -> add("tags" to values.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(5)) }
            savedToLibrary?.let { add("savedToLibrary" to it) }
        }
        val data = call("updateLibraryAsset", *fields.toTypedArray())
        data["asset"].objectOrNull()?.toLibraryAsset() ?: userError("素材更新结果不完整，请刷新确认")
    }

    suspend fun createInvite(): Result<V2CoachInvite> = coachCatching {
        call("createInviteCode").toCoachInvite()
    }

    suspend fun inviteStatus(inviteId: String): Result<V2CoachInviteStatus> = coachCatching {
        val status = call("getInviteStatus", "inviteId" to inviteId)["inviteStatus"].objectOrNull()
            ?: userError("邀请状态不完整，请重新打开")
        V2CoachInviteStatus(
            inviteId = status.string("inviteId") ?: inviteId,
            status = status.string("status") ?: "expired",
            expiresAt = status.string("expiresAt"),
            usedAt = status.string("usedAt"),
            student = status["student"].objectOrNull()?.toUser(),
        )
    }

    private suspend fun call(
        action: String,
        vararg fields: Pair<String, Any?>,
        allowSessionRetry: Boolean = true,
    ): JsonObject {
        auth.ensureAuthSession()
        val body = JsonObject(buildMap {
            put("action", JsonPrimitive(action))
            fields.forEach { (key, value) -> value?.let { put(key, it.toJson()) } }
        })
        val response = net.cloudApi.coachWorkspace(body)
        val code = response.string("code")
        if (allowSessionRetry && code in setOf("SESSION_INVALID", "SESSION_REQUIRED")) {
            auth.recoverRejectedAuthSession()
            return call(action, *fields, allowSessionRetry = false)
        }
        if (response["success"]?.jsonPrimitive?.booleanOrNull != true) {
            throw V2CoachWorkspaceException(
                code = code,
                userMessage = response.string("error") ?: "教练工作台暂时无法加载",
            )
        }
        return response["data"].objectOrNull() ?: JsonObject(emptyMap())
    }

    private suspend fun completeAsset(
        assetId: String,
        title: String,
        tags: List<String>,
        durationSeconds: Double?,
        savedToLibrary: Boolean,
    ): Result<String> = coachCatching {
        val data = call(
            "completeAssetUpload",
            "assetId" to assetId,
            "title" to title.take(80),
            "tags" to tags.map { it.take(20) }.distinct().take(5),
            "savedToLibrary" to savedToLibrary,
            "durationSeconds" to durationSeconds?.takeIf { it.isFinite() && it > 0 },
        )
        data["asset"].objectOrNull()?.string("assetId", "_id") ?: assetId
    }

    private suspend fun uploadMultipart(
        url: String,
        authorization: String,
        securityToken: String?,
        cosFileId: String?,
        cloudPath: String,
        file: File,
        contentType: String,
        onProgress: (Double) -> Unit = {},
    ) {
        onProgress(0.0)
        val fileBody = V2ProgressRequestBody(
            delegate = file.asRequestBody(contentType.toMediaType()),
            onProgress = onProgress,
        )
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("Signature", authorization)
            .apply {
                securityToken?.takeIf(String::isNotBlank)?.let { addFormDataPart("x-cos-security-token", it) }
                cosFileId?.takeIf(String::isNotBlank)?.let { addFormDataPart("x-cos-meta-fileid", it) }
            }
            .addFormDataPart("key", cloudPath)
            .addFormDataPart("file", file.name, fileBody)
            .build()
        val request = Request.Builder().url(url).post(body).build()
        val client = net.httpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .callTimeout(30, TimeUnit.MINUTES)
            .build()
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!continuation.isActive) return
                        if (response.isSuccessful) {
                            onProgress(1.0)
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(
                                V2CoachUserFacingException("视频上传失败，视频仍保留，请重试"),
                            )
                        }
                    }
                }
            })
        }
    }
}

internal class V2ProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (Double) -> Unit,
) : RequestBody() {
    override fun contentType() = delegate.contentType()

    override fun contentLength() = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength().coerceAtLeast(1L)
        var written = 0L
        var lastReported = -1.0
        val countingSink = object : ForwardingSink(sink) {
            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                written += byteCount
                val progress = (written.toDouble() / total.toDouble()).coerceIn(0.0, 1.0)
                if (progress >= 1.0 || progress - lastReported >= 0.005) {
                    lastReported = progress
                    onProgress(progress)
                }
            }
        }.buffer()
        delegate.writeTo(countingSink)
        countingSink.flush()
    }
}

private fun Any.toJson(): JsonElement = when (this) {
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Int -> JsonPrimitive(this)
    is Long -> JsonPrimitive(this)
    is Double -> JsonPrimitive(this)
    is List<*> -> JsonArray(map { it?.toJson() ?: JsonNull })
    else -> JsonPrimitive(toString())
}

private fun String?.toJsonOrNull(): JsonElement =
    this?.trim()?.takeIf { it.isNotEmpty() }?.let(::JsonPrimitive) ?: JsonNull

private fun JsonElement?.objectOrNull(): JsonObject? = this as? JsonObject
private fun JsonObject.string(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
    this[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)
}
private fun JsonObject.int(key: String): Int = this[key]?.jsonPrimitive?.let {
    it.intOrNull ?: it.contentOrNull?.toIntOrNull()
} ?: 0
private fun JsonObject.nullableInt(key: String): Int? = this[key]?.jsonPrimitive?.let {
    it.intOrNull ?: it.contentOrNull?.toIntOrNull()
        ?: it.doubleOrNull?.takeIf(Double::isFinite)?.toInt()
}
private fun JsonObject.double(key: String): Double? = this[key]?.jsonPrimitive?.let {
    it.doubleOrNull ?: it.contentOrNull?.toDoubleOrNull()
}
private fun JsonObject.long(key: String): Long? = this[key]?.jsonPrimitive?.let {
    it.contentOrNull?.toLongOrNull() ?: it.doubleOrNull?.takeIf(Double::isFinite)?.toLong()
}
private fun JsonObject.nonNegativeDouble(key: String): Double =
    double(key)?.takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: 0.0
private fun JsonObject.bool(key: String): Boolean = this[key]?.jsonPrimitive?.booleanOrNull == true
private fun JsonObject.nullableBool(key: String): Boolean? = this[key]?.jsonPrimitive?.let {
    it.booleanOrNull ?: when (it.contentOrNull?.lowercase()) {
        "1", "yes" -> true
        "0", "no" -> false
        else -> null
    }
}
private fun JsonObject.array(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())
private fun JsonObject.stringList(key: String): List<String> = when (val value = this[key]) {
    is JsonArray -> value.mapNotNull { it.jsonPrimitive.contentOrNull }
    is JsonPrimitive -> value.contentOrNull.orEmpty()
        .replace('，', ',')
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
    else -> emptyList()
}
private fun JsonObject.intMap(key: String): Map<String, Int> =
    this[key].objectOrNull()?.mapValues { (_, value) ->
        value.jsonPrimitive.intOrNull ?: value.jsonPrimitive.contentOrNull?.toIntOrNull() ?: 0
    } ?: emptyMap()
private fun JsonObject.stringMap(key: String): Map<String, String> =
    this[key].objectOrNull()?.mapNotNull { (mapKey, value) ->
        value.jsonPrimitive.contentOrNull?.let { mapKey to it }
    }?.toMap() ?: emptyMap()

private fun JsonObject.toUser(): V2CoachUserSummary? {
    val userId = string("userId") ?: return null
    return V2CoachUserSummary(
        userId = userId,
        nickname = string("nickname") ?: "学员",
        avatar = string("avatar"),
        handicap = double("handicap"),
        golfAge = int("golfAge").takeIf { it > 0 },
        homeCourse = string("homeCourse"),
        coachAuthorizationStatus = string("coachAuthorizationStatus"),
        golfStartYear = int("golfStartYear").takeIf { it > 0 },
        dominantHand = string("dominantHand"),
        golfBrands = array("golfBrands").mapNotNull { it.jsonPrimitive.contentOrNull },
        bio = string("bio"),
    )
}

private fun JsonObject.toSpacePublicSource(): V2SpacePublicSource? {
    val commentId = string("commentId") ?: return null
    val videoId = string("videoId") ?: return null
    return V2SpacePublicSource(
        commentId = commentId,
        videoId = videoId,
        authorName = string("authorName").orEmpty(),
        commentCreatedAt = double("commentCreatedAt") ?: 0.0,
        available = bool("available"),
        quotedText = string("quotedText").orEmpty(),
    )
}

private fun JsonObject.toSpaceDirectMessage(): V2SpaceDirectMessage? {
    val messageId = string("messageId", "id", "_id") ?: return null
    val connectionId = string("connectionId") ?: return null
    return V2SpaceDirectMessage(
        messageId = messageId,
        connectionId = connectionId,
        senderUserId = string("senderUserId").orEmpty(),
        text = string("text").orEmpty(),
        createdAt = string("createdAt"),
        isMine = bool("isMine"),
        type = string("type"),
        source = this["source"].objectOrNull()?.toSpacePublicSource(),
    )
}

internal fun JsonObject.toConnection(): V2CoachConnection? {
    val id = string("connectionId") ?: return null
    return V2CoachConnection(
        connectionId = id,
        status = string("status") ?: "active",
        student = this["student"].objectOrNull()?.toUser(),
        coach = this["coach"].objectOrNull()?.toUser(),
        peer = this["peer"].objectOrNull()?.toUser(),
        relationshipType = string("relationshipType") ?: "coach",
        privateNote = string("coachPrivateNote"),
        trainingStage = string("coachTrainingStage"),
        primaryGoal = string("coachPrimaryGoal"),
        trainingFocus = string("coachTrainingFocus"),
        focusTags = array("coachFocusTags").mapNotNull { it.jsonPrimitive.contentOrNull },
        lessonMode = string("coachLessonMode"),
        trainingGoalTags = array("coachTrainingGoalTags").mapNotNull { it.jsonPrimitive.contentOrNull },
        ageTag = string("coachAgeTag"),
        weeklyPracticeGoal = int("coachWeeklyPracticeGoal").takeIf { it > 0 },
        cadence = string("coachCadence"),
        communicationPreference = string("coachCommunicationPreference"),
        profileCompletedAt = string("coachProfileCompletedAt"),
        category = string("coachCategory"),
        pinned = bool("coachPinned") || bool("spacePinned"),
        followUpAt = string("coachFollowUpAt"),
        lastActivityAt = string("lastActivityAt"),
        createdAt = string("createdAt"),
        acceptedAt = string("acceptedAt"),
        disconnectedAt = string("disconnectedAt"),
        disconnectedByUserId = string("disconnectedByUserId"),
        perspective = string("perspective"),
        trainingFocusUpdatedAt = string("coachTrainingFocusUpdatedAt"),
        preferencesUpdatedAt = string("coachPreferencesUpdatedAt"),
    )
}

internal fun JsonObject.toWorkspaceItem(): V2CoachWorkspaceItem? {
    val id = string("itemId", "id", "_id") ?: return null
    val connectionId = string("connectionId") ?: return null
    return V2CoachWorkspaceItem(
        itemId = id,
        connectionId = connectionId,
        senderUserId = string("senderUserId"),
        senderRole = string("senderRole"),
        kind = string("kind"),
        parentItemId = string("parentItemId"),
        replyToItemId = string("replyToItemId"),
        replyAtSeconds = double("replyAtSeconds"),
        title = string("title"),
        text = string("text", "note"),
        createdAt = string("createdAt"),
        latestActivityAt = string("latestActivityAt"),
        latestReplyAt = string("latestReplyAt"),
        reviewState = string("reviewState"),
        videoUrl = string("videoURL", "videoUrl"),
        coverUrl = string("coverURL", "coverUrl"),
        durationSeconds = double("durationSeconds"),
        hasVideo = nullableBool("hasVideo"),
        primaryAsset = this["primaryAsset"].objectOrNull()?.toWorkspaceAsset(),
        attachments = array("attachments").mapNotNull { it.objectOrNull()?.toWorkspaceAsset() },
        replyCount = int("replyCount"),
        latestReplyText = string("latestReplyText"),
        latestReplySenderRole = string("latestReplySenderRole"),
        latestReplyHasVideo = nullableBool("latestReplyHasVideo"),
        studentHasUnread = bool("studentHasUnread"),
        deletedAt = string("deletedAt"),
        isMine = bool("isMine"),
        coachingFocus = string("coachingFocus"),
        fileSizeBytes = long("fileSizeBytes"),
        width = nullableInt("width")?.takeIf { it > 0 },
        height = nullableInt("height")?.takeIf { it > 0 },
        requiresCoachReview = nullableBool("requiresCoachReview"),
        coachViewedAt = string("coachViewedAt"),
        coachRespondedAt = string("coachRespondedAt"),
        coachLastViewedReplyId = string("coachLastViewedReplyId"),
        studentLastViewedReplyId = string("studentLastViewedReplyId"),
        studentViewedAt = string("studentViewedAt"),
        latestReplyCoverUrl = string("latestReplyCoverURL", "latestReplyCoverUrl"),
        latestReplyDurationSeconds = double("latestReplyDurationSeconds"),
        sender = this["sender"].objectOrNull()?.toUser(),
        reviewWaitingSince = string("reviewWaitingSince"),
        hasStudentFollowUp = nullableBool("hasStudentFollowUp"),
        reviewOverdue = nullableBool("reviewOverdue"),
        sourceRecordId = string("sourceRecordId"),
    )
}

internal fun JsonObject.toCoachThread(): V2CoachThread? {
    val item = this["item"].objectOrNull()?.toWorkspaceItem() ?: return null
    return V2CoachThread(
        item = item,
        replies = array("replies").mapNotNull { it.objectOrNull()?.toWorkspaceItem() },
        version = string("threadVersion"),
        checkVersion = string("threadCheckVersion"),
        nextCursor = string("nextCursor"),
    )
}

internal fun JsonObject.toWorkspaceAsset(): V2CoachWorkspaceAsset? {
    val id = string("assetId", "id", "_id") ?: return null
    return V2CoachWorkspaceAsset(
        assetId = id,
        sourceKind = string("sourceKind") ?: "album",
        title = string("title") ?: "",
        videoUrl = string("videoURL", "videoUrl"),
        coverUrl = string("coverURL", "coverUrl"),
        durationSeconds = double("durationSeconds"),
        savedToLibrary = bool("savedToLibrary"),
        ownerUserId = string("ownerUserId"),
        tags = stringList("tags"),
        fileSizeBytes = long("fileSizeBytes"),
        width = nullableInt("width")?.takeIf { it > 0 },
        height = nullableInt("height")?.takeIf { it > 0 },
        useCount = nullableInt("useCount")?.takeIf { it >= 0 },
        playbackCount = nullableInt("playbackCount")?.takeIf { it >= 0 },
        uploadDurationSeconds = double("uploadDurationSeconds"),
        createdAt = string("createdAt"),
        lastUsedAt = string("lastUsedAt"),
    )
}

private fun JsonObject.toLibraryAsset(): V2CoachLibraryAsset? {
    val id = string("assetId", "id", "_id") ?: return null
    return V2CoachLibraryAsset(
        assetId = id,
        title = string("title") ?: "教学素材",
        videoUrl = string("videoURL", "videoUrl"),
        coverUrl = string("coverURL", "coverUrl"),
        durationSeconds = double("durationSeconds"),
        tags = stringList("tags"),
        useCount = int("useCount"),
        createdAt = string("createdAt"),
        lastUsedAt = string("lastUsedAt"),
        updatedAt = string("updatedAt"),
        ownerUserId = string("ownerUserId"),
        sourceKind = string("sourceKind") ?: "album",
        fileSizeBytes = long("fileSizeBytes"),
        width = nullableInt("width")?.takeIf { it > 0 },
        height = nullableInt("height")?.takeIf { it > 0 },
        playbackCount = nullableInt("playbackCount")?.takeIf { it >= 0 },
        uploadDurationSeconds = double("uploadDurationSeconds"),
    )
}

internal fun JsonObject.toConnectionActivitySnapshot(): V2CoachConnectionActivitySnapshot? {
    val connectionId = string("connectionId") ?: return null
    return V2CoachConnectionActivitySnapshot(
        connectionId = connectionId,
        state = string("state") ?: "idle",
        newCount = int("newCount"),
        pendingCount = int("pendingCount"),
        overduePendingCount = int("overduePendingCount"),
        weeklyPracticeCount = nullableInt("weeklyPracticeCount")?.takeIf { it >= 0 },
        lastPracticeAt = string("lastPracticeAt"),
        sharedContentCount = nullableInt("sharedContentCount")?.takeIf { it >= 0 },
        pendingViewCount = nullableInt("pendingViewCount")?.takeIf { it >= 0 },
        viewedTeachingCount = nullableInt("viewedTeachingCount")?.takeIf { it >= 0 },
        priorityItemId = string("priorityItemId"),
        latestItemId = string("latestItemId"),
        latestActivityAt = string("latestActivityAt"),
        latestText = string("latestText").orEmpty(),
        latestSenderRole = string("latestSenderRole"),
        latestHasVideo = bool("latestHasVideo"),
        studentHasUnread = bool("studentHasUnread"),
        studentViewedAt = string("studentViewedAt"),
    )
}

internal fun JsonObject.toCoachDashboard(): V2CoachDashboard {
    val counts = this["counts"].objectOrNull() ?: JsonObject(emptyMap())
    return V2CoachDashboard(
        activeStudentCount = int("activeStudentCount"),
        newCount = counts.int("new"),
        viewedCount = counts.int("viewed"),
        respondedCount = counts.int("responded"),
        pendingCount = counts.int("pending"),
        overduePendingCount = int("overduePendingCount"),
        recentItems = array("recentItems").mapNotNull { it.objectOrNull()?.toWorkspaceItem() },
        refreshedAt = string("refreshedAt"),
        newByConnection = intMap("newByConnection"),
        pendingByConnection = intMap("pendingByConnection"),
        pendingItemIdByConnection = stringMap("pendingItemIdByConnection"),
        overdueByConnection = intMap("overdueByConnection"),
        oldestPendingAt = string("oldestPendingAt"),
        connectionStates = array("connectionStates").mapNotNull {
            it.objectOrNull()?.toConnectionActivitySnapshot()
        },
        studentPreviewItems = array("studentPreviewItems").mapNotNull {
            it.objectOrNull()?.toWorkspaceItem()
        },
    )
}

private fun JsonObject.toQuickReply(): V2CoachQuickReply? {
    val id = string("quickReplyId", "id", "_id") ?: return null
    val text = string("text") ?: return null
    return V2CoachQuickReply(
        quickReplyId = id,
        title = string("title").orEmpty(),
        text = text,
        useCount = int("useCount"),
        lastUsedAt = string("lastUsedAt"),
        createdAt = string("createdAt"),
        updatedAt = string("updatedAt"),
    )
}

internal fun JsonObject.toUploadGrant(): V2CoachUploadGrant? {
    val itemId = string("itemId") ?: return null
    return V2CoachUploadGrant(
        itemId = itemId,
        assetId = string("assetId") ?: itemId,
        uploadUrl = string("uploadURL", "uploadUrl") ?: return null,
        authorization = string("authorization") ?: return null,
        securityToken = string("securityToken"),
        cosFileId = string("cosFileId"),
        cloudPath = string("cloudPath") ?: return null,
        fileId = string("fileID", "fileId"),
        contentType = string("contentType") ?: "video/mp4",
        expiresAt = string("expiresAt"),
        draftExpiresAt = string("draftExpiresAt"),
        coverUploadUrl = string("coverUploadURL", "coverUploadUrl"),
        coverAuthorization = string("coverAuthorization"),
        coverSecurityToken = string("coverSecurityToken"),
        coverCosFileId = string("coverCosFileId"),
        coverCloudPath = string("coverCloudPath"),
        coverFileId = string("coverFileID", "coverFileId"),
    )
}

internal fun JsonObject.toCoachInvite(): V2CoachInvite = V2CoachInvite(
    inviteId = string("inviteId") ?: userError("邀请信息不完整，请重新生成"),
    inviteCode = string("inviteCode") ?: userError("邀请信息不完整，请重新生成"),
    inviteToken = string("inviteToken") ?: userError("邀请信息不完整，请重新生成"),
    inviteUrl = string("inviteURL", "inviteUrl") ?: userError("邀请信息不完整，请重新生成"),
    expiresAt = string("expiresAt") ?: userError("邀请信息不完整，请重新生成"),
    coach = this["coach"].objectOrNull()?.toUser() ?: userError("邀请信息不完整，请重新生成"),
)

private class V2CoachWorkspaceException(
    val code: String?,
    val userMessage: String,
) : V2CoachUserFacingException(userMessage)

private fun userError(message: String): Nothing = throw V2CoachUserFacingException(message)

private suspend inline fun <T> coachCatching(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(
        if (e is V2CoachUserFacingException) e
        else V2CoachUserFacingException("教学空间暂时不可用，请稍后重试", e),
    )
}
