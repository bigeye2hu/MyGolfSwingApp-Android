package com.mygolfswing.app.data.coach

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

@Serializable
internal data class V2WorkspaceDraftAttachment(
    val assetId: String? = null,
    val videoPath: String? = null,
    val coverPath: String? = null,
    val uploadId: String,
    val requestId: String? = null,
    val sourceKind: String = "album",
    val title: String = "关联教学视频",
    val durationSeconds: Double? = null,
    val coverTimeSeconds: Double? = null,
    val sourceRecordId: String? = null,
    val saveToLibrary: Boolean = false,
)

internal data class V2WorkspaceDraft(
    val title: String = "",
    val text: String = "",
    val contentMode: String? = null,
    val requiresCoachReview: Boolean = false,
    val saveToLibrary: Boolean = false,
    val replyToItemId: String? = null,
    val replyAtSecondsText: String = "",
    val primaryAttachment: V2WorkspaceDraftAttachment? = null,
    /** Legacy cloud-only format retained for one-way migration. */
    val relatedAssetIds: List<String> = emptyList(),
    val relatedAttachments: List<V2WorkspaceDraftAttachment> = emptyList(),
    val savedAtMillis: Long = 0L,
)

internal const val V2_WORKSPACE_DRAFT_LIFETIME_MILLIS = 24L * 60L * 60L * 1_000L

internal fun isWorkspaceDraftRestorable(savedAtMillis: Long, nowMillis: Long): Boolean =
    savedAtMillis <= 0L || nowMillis - savedAtMillis < V2_WORKSPACE_DRAFT_LIFETIME_MILLIS

private fun V2WorkspaceDraft.hasContent(): Boolean = title.isNotBlank() || text.isNotBlank() || requiresCoachReview ||
    saveToLibrary || !replyToItemId.isNullOrBlank() || replyAtSecondsText.isNotBlank() ||
    primaryAttachment != null || relatedAssetIds.isNotEmpty() || relatedAttachments.isNotEmpty()

private fun V2WorkspaceDraft.contentEquals(other: V2WorkspaceDraft): Boolean =
    copy(savedAtMillis = 0L) == other.copy(savedAtMillis = 0L)

internal fun workspaceDraftStorageKey(kind: String, ownerUserId: String, subjectId: String): String {
    val source = "$kind\u0000$ownerUserId\u0000$subjectId"
    val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray())
    return "${kind}_${digest.take(16).joinToString("") { "%02x".format(it) }}"
}

/** Account-scoped, app-private recovery for unsent workspace text and reply context. */
internal class V2WorkspaceDraftStore(
    context: Context,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val appContext = context.applicationContext
    private val preferences = appContext
        .getSharedPreferences("v2_workspace_text_drafts", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun loadRoot(ownerUserId: String, connectionId: String): V2WorkspaceDraft =
        load(workspaceDraftStorageKey("root", ownerUserId, connectionId))

    fun saveRoot(
        ownerUserId: String,
        connectionId: String,
        text: String,
        contentMode: V2WorkspaceContentMode,
        requiresCoachReview: Boolean,
        saveToLibrary: Boolean = false,
        primaryAttachment: V2WorkspaceDraftAttachment? = null,
    ) = save(
        key = workspaceDraftStorageKey("root", ownerUserId, connectionId),
        draft = V2WorkspaceDraft(
            text = text,
            contentMode = contentMode.wireValue,
            requiresCoachReview = requiresCoachReview,
            saveToLibrary = saveToLibrary,
            primaryAttachment = primaryAttachment,
        ),
    )

    fun clearRoot(ownerUserId: String, connectionId: String) =
        clear(workspaceDraftStorageKey("root", ownerUserId, connectionId))

    fun loadThread(ownerUserId: String, itemId: String): V2WorkspaceDraft =
        load(workspaceDraftStorageKey("thread", ownerUserId, itemId))

    fun saveThread(
        ownerUserId: String,
        connectionId: String,
        itemId: String,
        title: String = "",
        text: String,
        replyToItemId: String?,
        replyAtSecondsText: String,
        saveToLibrary: Boolean = false,
        primaryAttachment: V2WorkspaceDraftAttachment? = null,
        relatedAssetIds: List<String> = emptyList(),
        relatedAttachments: List<V2WorkspaceDraftAttachment> = emptyList(),
    ) {
        val migratedCloudAttachments = relatedAssetIds.distinct().map { assetId ->
            V2WorkspaceDraftAttachment(
                assetId = assetId,
                uploadId = "existing_$assetId",
                sourceKind = "library_asset",
            )
        }
        val boundedAttachments = (relatedAttachments + migratedCloudAttachments)
            .distinctBy { it.assetId ?: it.uploadId }
            .take(V2_MAX_RELATED_MEDIA)
        save(
            key = workspaceDraftStorageKey("thread", ownerUserId, itemId),
            draft = V2WorkspaceDraft(
                title = title,
                text = text,
                replyToItemId = replyToItemId,
                replyAtSecondsText = replyAtSecondsText,
                saveToLibrary = saveToLibrary,
                primaryAttachment = primaryAttachment,
                relatedAttachments = boundedAttachments,
            ),
        )
        val indexKey = threadIndexKey(ownerUserId, connectionId)
        val itemIds = preferences.getStringSet(indexKey, emptySet()).orEmpty()
        val hasDraft = title.isNotBlank() || text.isNotBlank() || !replyToItemId.isNullOrBlank() ||
            replyAtSecondsText.isNotBlank() || saveToLibrary || primaryAttachment != null || boundedAttachments.isNotEmpty()
        val updatedIds = if (hasDraft) itemIds + itemId else itemIds - itemId
        preferences.edit {
            if (updatedIds.isEmpty()) remove(indexKey) else putStringSet(indexKey, updatedIds)
        }
    }

    fun clearThread(
        ownerUserId: String,
        connectionId: String,
        itemId: String,
        deleteLocalMedia: Boolean = true,
    ) {
        clear(workspaceDraftStorageKey("thread", ownerUserId, itemId), deleteLocalMedia)
        val indexKey = threadIndexKey(ownerUserId, connectionId)
        val remaining = preferences.getStringSet(indexKey, emptySet()).orEmpty() - itemId
        preferences.edit {
            if (remaining.isEmpty()) remove(indexKey) else putStringSet(indexKey, remaining)
        }
    }

    fun clearConnection(ownerUserId: String, connectionId: String) {
        clearRoot(ownerUserId, connectionId)
        val indexKey = threadIndexKey(ownerUserId, connectionId)
        preferences.getStringSet(indexKey, emptySet()).orEmpty().forEach { itemId ->
            clear(workspaceDraftStorageKey("thread", ownerUserId, itemId))
        }
        preferences.edit { remove(indexKey) }
    }

    /** Logout is a privacy boundary: discard every account's unsent workspace text. */
    fun clearAll() {
        preferences.all.keys.filter { it.endsWith(".primary_media") }.forEach { mediaKey ->
            decodeAttachment(preferences.getString(mediaKey, null))?.let { deleteDraftMedia(listOf(it)) }
        }
        preferences.all.keys.filter { it.endsWith(".related_media") }.forEach { mediaKey ->
            decodeAttachments(preferences.getString(mediaKey, null)).let(::deleteDraftMedia)
        }
        preferences.edit { clear() }
    }

    fun hasConnectionDraft(ownerUserId: String, connectionId: String): Boolean {
        val root = loadRoot(ownerUserId, connectionId)
        return root.hasContent() || liveThreadDrafts(ownerUserId, connectionId).isNotEmpty()
    }

    fun latestConnectionDraftSavedAtMillis(ownerUserId: String, connectionId: String): Long? =
        (listOf(loadRoot(ownerUserId, connectionId)) + liveThreadDrafts(ownerUserId, connectionId))
            .filter(V2WorkspaceDraft::hasContent)
            .maxOfOrNull(V2WorkspaceDraft::savedAtMillis)
            ?.takeIf { it > 0L }

    fun threadDraftItemIds(ownerUserId: String, connectionId: String): Set<String> {
        val indexKey = threadIndexKey(ownerUserId, connectionId)
        val itemIds = preferences.getStringSet(indexKey, emptySet()).orEmpty()
        val liveIds = itemIds.filterTo(mutableSetOf()) { itemId ->
            loadThread(ownerUserId, itemId).hasContent()
        }
        if (liveIds != itemIds) {
            preferences.edit {
                if (liveIds.isEmpty()) remove(indexKey) else putStringSet(indexKey, liveIds)
            }
        }
        return liveIds
    }

    private fun threadIndexKey(ownerUserId: String, connectionId: String): String =
        workspaceDraftStorageKey("thread_index", ownerUserId, connectionId)

    private fun liveThreadDrafts(ownerUserId: String, connectionId: String): List<V2WorkspaceDraft> {
        val indexKey = threadIndexKey(ownerUserId, connectionId)
        val itemIds = preferences.getStringSet(indexKey, emptySet()).orEmpty()
        val draftsByItemId = itemIds.associateWith { itemId -> loadThread(ownerUserId, itemId) }
        val liveIds = draftsByItemId.filterValues(V2WorkspaceDraft::hasContent).keys
        if (liveIds != itemIds) {
            preferences.edit {
                if (liveIds.isEmpty()) remove(indexKey) else putStringSet(indexKey, liveIds)
            }
        }
        return liveIds.mapNotNull(draftsByItemId::get)
    }

    private fun load(key: String): V2WorkspaceDraft {
        var draft = loadPersisted(key)
        if (!draft.hasContent()) return V2WorkspaceDraft()
        if (draft.savedAtMillis <= 0L) {
            // Legacy drafts did not carry a timestamp. Start their one-time migration window now
            // instead of deleting content whose original save time cannot be proven.
            draft = draft.copy(savedAtMillis = nowMillis())
            preferences.edit { putLong("$key.saved_at", draft.savedAtMillis) }
        }
        if (!isWorkspaceDraftRestorable(draft.savedAtMillis, nowMillis())) {
            clear(key)
            return V2WorkspaceDraft()
        }
        return draft
    }

    private fun loadPersisted(key: String): V2WorkspaceDraft {
        val legacyAssetIds = preferences.getString("$key.related_assets", "").orEmpty()
            .split('\u0000').filter(String::isNotBlank).distinct().take(V2_MAX_RELATED_MEDIA)
        val persistedAttachments = decodeAttachments(preferences.getString("$key.related_media", null))
        val migratedAttachments = legacyAssetIds.map { assetId ->
            V2WorkspaceDraftAttachment(
                assetId = assetId,
                uploadId = "existing_$assetId",
                sourceKind = "library_asset",
            )
        }
        return V2WorkspaceDraft(
            title = preferences.getString("$key.title", "").orEmpty(),
            text = preferences.getString("$key.text", "").orEmpty(),
            contentMode = preferences.getString("$key.mode", null),
            requiresCoachReview = preferences.getBoolean("$key.review", false),
            saveToLibrary = preferences.getBoolean("$key.library", false),
            replyToItemId = preferences.getString("$key.reply", null),
            replyAtSecondsText = preferences.getString("$key.time", "").orEmpty(),
            primaryAttachment = decodeAttachment(preferences.getString("$key.primary_media", null)),
            relatedAttachments = (persistedAttachments + migratedAttachments)
                .distinctBy { it.assetId ?: it.uploadId }
                .take(V2_MAX_RELATED_MEDIA),
            savedAtMillis = preferences.getLong("$key.saved_at", 0L),
        )
    }

    private fun save(key: String, draft: V2WorkspaceDraft) {
        val empty = !draft.hasContent()
        val previous = loadPersisted(key)
        val retainedPaths = (listOfNotNull(draft.primaryAttachment) + draft.relatedAttachments)
            .flatMap { listOfNotNull(it.videoPath, it.coverPath) }.toSet()
        deleteDraftPaths(
            (listOfNotNull(previous.primaryAttachment) + previous.relatedAttachments)
                .flatMap { listOfNotNull(it.videoPath, it.coverPath) }
                .filterNot(retainedPaths::contains),
        )
        val savedAtMillis = when {
            empty -> 0L
            previous.hasContent() && previous.contentEquals(draft) -> previous.savedAtMillis
            else -> nowMillis()
        }
        preferences.edit {
            if (empty) {
                remove("$key.title")
                remove("$key.text")
                remove("$key.mode")
                remove("$key.review")
                remove("$key.library")
                remove("$key.reply")
                remove("$key.time")
                remove("$key.primary_media")
                remove("$key.related_assets")
                remove("$key.related_media")
                remove("$key.saved_at")
            } else {
                putString("$key.title", draft.title)
                putString("$key.text", draft.text)
                putString("$key.mode", draft.contentMode)
                putBoolean("$key.review", draft.requiresCoachReview)
                putBoolean("$key.library", draft.saveToLibrary)
                putString("$key.reply", draft.replyToItemId)
                putString("$key.time", draft.replyAtSecondsText)
                if (draft.primaryAttachment == null) {
                    remove("$key.primary_media")
                } else {
                    putString(
                        "$key.primary_media",
                        json.encodeToString(V2WorkspaceDraftAttachment.serializer(), draft.primaryAttachment),
                    )
                }
                remove("$key.related_assets")
                putString(
                    "$key.related_media",
                    json.encodeToString(
                        ListSerializer(V2WorkspaceDraftAttachment.serializer()),
                        draft.relatedAttachments.take(V2_MAX_RELATED_MEDIA),
                    ),
                )
                putLong("$key.saved_at", savedAtMillis)
            }
        }
    }

    private fun clear(key: String, deleteLocalMedia: Boolean = true) {
        if (deleteLocalMedia) {
            val persisted = loadPersisted(key)
            deleteDraftMedia(listOfNotNull(persisted.primaryAttachment) + persisted.relatedAttachments)
        }
        preferences.edit {
            remove("$key.title")
            remove("$key.text")
            remove("$key.mode")
            remove("$key.review")
            remove("$key.library")
            remove("$key.reply")
            remove("$key.time")
            remove("$key.primary_media")
            remove("$key.related_assets")
            remove("$key.related_media")
            remove("$key.saved_at")
        }
    }

    private fun decodeAttachments(value: String?): List<V2WorkspaceDraftAttachment> = runCatching {
        json.decodeFromString(ListSerializer(V2WorkspaceDraftAttachment.serializer()), value ?: "[]")
    }.getOrDefault(emptyList()).take(V2_MAX_RELATED_MEDIA)

    private fun decodeAttachment(value: String?): V2WorkspaceDraftAttachment? = value?.let { encoded ->
        runCatching { json.decodeFromString(V2WorkspaceDraftAttachment.serializer(), encoded) }.getOrNull()
    }

    private fun deleteDraftMedia(attachments: List<V2WorkspaceDraftAttachment>) {
        deleteDraftPaths(attachments.flatMap { listOfNotNull(it.videoPath, it.coverPath) })
    }

    private fun deleteDraftPaths(paths: List<String>) {
        val cacheRoot = runCatching { appContext.cacheDir.canonicalFile }.getOrNull() ?: return
        paths.distinct().forEach { path ->
            val candidate = runCatching { File(path).canonicalFile }.getOrNull() ?: return@forEach
            if (candidate.path.startsWith(cacheRoot.path + File.separator)) candidate.delete()
        }
    }
}
