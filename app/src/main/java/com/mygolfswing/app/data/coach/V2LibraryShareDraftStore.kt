package com.mygolfswing.app.data.coach

import android.content.Context
import androidx.core.content.edit

internal data class V2LibraryShareDraft(
    val connectionIds: List<String> = emptyList(),
    val note: String = "",
    val batchId: String = "",
    val savedAtMillis: Long = 0L,
) {
    val hasContent: Boolean
        get() = connectionIds.isNotEmpty() || note.isNotBlank()
}

/** Account/asset-scoped recovery for the private-library recipient sheet. */
internal class V2LibraryShareDraftStore(
    context: Context,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val preferences = context.applicationContext
        .getSharedPreferences("v2_library_share_drafts", Context.MODE_PRIVATE)

    fun load(ownerUserId: String, assetId: String): V2LibraryShareDraft {
        val key = draftKey(ownerUserId, assetId)
        val draft = V2LibraryShareDraft(
            connectionIds = preferences.getStringSet("$key.connections", emptySet()).orEmpty().sorted(),
            note = preferences.getString("$key.note", "").orEmpty(),
            batchId = preferences.getString("$key.batch", "").orEmpty(),
            savedAtMillis = preferences.getLong("$key.saved_at", 0L),
        )
        if (!draft.hasContent) return V2LibraryShareDraft()
        val savedAt = draft.savedAtMillis.takeIf { it > 0L } ?: nowMillis().also { migratedAt ->
            preferences.edit { putLong("$key.saved_at", migratedAt) }
        }
        if (!isWorkspaceDraftRestorable(savedAt, nowMillis())) {
            clear(ownerUserId, assetId)
            return V2LibraryShareDraft()
        }
        return draft.copy(savedAtMillis = savedAt)
    }

    fun save(ownerUserId: String, assetId: String, draft: V2LibraryShareDraft) {
        if (!draft.hasContent) {
            clear(ownerUserId, assetId)
            return
        }
        val key = draftKey(ownerUserId, assetId)
        val previous = load(ownerUserId, assetId)
        val contentUnchanged = previous.hasContent &&
            previous.connectionIds == draft.connectionIds.sorted() &&
            previous.note == draft.note && previous.batchId == draft.batchId
        val savedAt = if (contentUnchanged) previous.savedAtMillis else nowMillis()
        preferences.edit {
            putStringSet("$key.connections", draft.connectionIds.toSet())
            putString("$key.note", draft.note)
            putString("$key.batch", draft.batchId)
            putLong("$key.saved_at", savedAt)
            putStringSet(indexKey(ownerUserId), indexedAssetIds(ownerUserId) + assetId)
        }
    }

    fun clear(ownerUserId: String, assetId: String) {
        val key = draftKey(ownerUserId, assetId)
        val remaining = indexedAssetIds(ownerUserId) - assetId
        preferences.edit {
            remove("$key.connections")
            remove("$key.note")
            remove("$key.batch")
            remove("$key.saved_at")
            if (remaining.isEmpty()) remove(indexKey(ownerUserId))
            else putStringSet(indexKey(ownerUserId), remaining)
        }
    }

    /** Recipient selections and notes are private working data and never survive logout. */
    fun clearAll() {
        preferences.edit { clear() }
    }

    fun hasDraft(ownerUserId: String, assetId: String): Boolean = load(ownerUserId, assetId).hasContent

    fun hasAnyDraft(ownerUserId: String): Boolean {
        val liveAssetIds = indexedAssetIds(ownerUserId).filterTo(mutableSetOf()) { assetId ->
            load(ownerUserId, assetId).hasContent
        }
        if (liveAssetIds != indexedAssetIds(ownerUserId)) {
            preferences.edit {
                if (liveAssetIds.isEmpty()) remove(indexKey(ownerUserId))
                else putStringSet(indexKey(ownerUserId), liveAssetIds)
            }
        }
        return liveAssetIds.isNotEmpty()
    }

    private fun indexedAssetIds(ownerUserId: String): Set<String> =
        preferences.getStringSet(indexKey(ownerUserId), emptySet()).orEmpty()

    private fun indexKey(ownerUserId: String): String =
        workspaceDraftStorageKey("library_share_index", ownerUserId, ownerUserId)

    private fun draftKey(ownerUserId: String, assetId: String): String =
        workspaceDraftStorageKey("library_share", ownerUserId, assetId)
}
