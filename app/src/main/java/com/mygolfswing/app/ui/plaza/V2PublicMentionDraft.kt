package com.mygolfswing.app.ui.plaza

/**
 * Keeps account IDs only for @ tokens inserted through the public mention picker.
 * Editing any part of an inserted token revokes its account association, matching iOS.
 */
internal data class V2PublicMentionDraft(
    val text: String = "",
    val mentions: List<Mention> = emptyList(),
) {
    internal data class Mention(
        val userId: String,
        val start: Int,
        val length: Int,
    )

    val userIds: List<String>
        get() = mentions.map(Mention::userId)

    fun updateText(value: String): V2PublicMentionDraft {
        if (value == text) return this

        var prefix = 0
        while (prefix < minOf(text.length, value.length) && text[prefix] == value[prefix]) prefix += 1

        var suffix = 0
        while (
            suffix < minOf(text.length, value.length) - prefix &&
            text[text.length - suffix - 1] == value[value.length - suffix - 1]
        ) {
            suffix += 1
        }

        val oldEnd = text.length - suffix
        val delta = value.length - text.length
        val retained = mentions.mapNotNull { mention ->
            val mentionEnd = mention.start + mention.length
            when {
                oldEnd <= mention.start -> mention.copy(start = mention.start + delta)
                prefix >= mentionEnd -> mention
                else -> null
            }
        }
        return copy(text = value, mentions = retained)
    }

    fun select(userId: String, name: String): Selection {
        if (userId in userIds) return Selection(this, accepted = true)
        if (mentions.size >= MAX_MENTIONS) return Selection(this, accepted = false)

        val cleanName = name.trim().removePrefix("@").ifBlank { "用户" }
        val token = "@$cleanName"
        val separator = if (text.isEmpty() || text.last().isWhitespace()) "" else " "
        val start = text.length + separator.length
        return Selection(
            draft = copy(
                text = text + separator + token + " ",
                mentions = mentions + Mention(userId = userId, start = start, length = token.length),
            ),
            accepted = true,
        )
    }

    internal data class Selection(val draft: V2PublicMentionDraft, val accepted: Boolean)

    private companion object {
        const val MAX_MENTIONS = 5
    }
}
