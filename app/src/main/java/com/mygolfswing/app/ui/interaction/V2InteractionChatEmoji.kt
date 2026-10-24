package com.mygolfswing.app.ui.interaction

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val tuiEmojiPattern = Regex("\\[TUIEmoji_[^\\]]+\\]")
private const val MESSAGE_EMOJI_RECENTS_PREFERENCES = "v2_chat_message_emoji_recents"
private const val MESSAGE_EMOJI_RECENTS_KEY = "ids"

internal fun chatEmojiForCode(code: String): V2ChatReactionEmoji? =
    v2ChatReactionEmojis.firstOrNull { it.reactionId == code }

internal fun chatEmojiDrawable(emoji: V2ChatReactionEmoji): Int =
    v2ChatReactionDrawables[emoji.assetIndex]

internal fun localizeChatEmojiCodes(text: String): String = tuiEmojiPattern.replace(text) { match ->
    chatEmojiForCode(match.value)?.localizedName ?: match.value
}

internal fun appendChatEmojiCode(draft: String, code: String, maxLength: Int = 2_000): String =
    (draft + code).take(maxLength)

internal fun deleteLastChatEmojiOrCharacter(draft: String): String {
    if (draft.isEmpty()) return draft
    val trailingEmoji = tuiEmojiPattern.findAll(draft).lastOrNull()?.takeIf { it.range.last == draft.lastIndex }
    return if (trailingEmoji != null && chatEmojiForCode(trailingEmoji.value) != null) {
        draft.removeRange(trailingEmoji.range)
    } else {
        draft.dropLast(1)
    }
}

internal fun recentMessageEmojis(context: Context): List<V2ChatReactionEmoji> {
    val ids = context.getSharedPreferences(MESSAGE_EMOJI_RECENTS_PREFERENCES, Context.MODE_PRIVATE)
        .getString(MESSAGE_EMOJI_RECENTS_KEY, null)
        ?.split('|')
        .orEmpty()
    val byId = v2ChatReactionEmojis.associateBy(V2ChatReactionEmoji::reactionId)
    return ids.mapNotNull(byId::get).distinctBy(V2ChatReactionEmoji::reactionId).take(8)
}

internal fun rememberMessageEmoji(context: Context, code: String): List<V2ChatReactionEmoji> {
    val preferences = context.getSharedPreferences(MESSAGE_EMOJI_RECENTS_PREFERENCES, Context.MODE_PRIVATE)
    val previous = preferences.getString(MESSAGE_EMOJI_RECENTS_KEY, null)?.split('|').orEmpty()
    preferences.edit().putString(
        MESSAGE_EMOJI_RECENTS_KEY,
        (listOf(code) + previous).distinct().take(8).joinToString("|"),
    ).apply()
    return recentMessageEmojis(context)
}

/**
 * Keeps the Tencent-compatible raw code in the draft while presenting a compact emoji to the
 * editor. Message bubbles use the original TUI artwork below.
 */
internal class V2ChatEmojiVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val matches = tuiEmojiPattern.findAll(original)
            .mapNotNull { match -> chatEmojiForCode(match.value)?.let { match to it } }
            .toList()
        if (matches.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val transformed = StringBuilder()
        val originalToTransformed = IntArray(original.length + 1)
        val transformedToOriginal = mutableListOf(0)
        var originalIndex = 0

        fun appendPlain(until: Int) {
            while (originalIndex < until) {
                originalToTransformed[originalIndex] = transformed.length
                transformed.append(original[originalIndex])
                originalIndex += 1
                originalToTransformed[originalIndex] = transformed.length
                transformedToOriginal.add(originalIndex)
            }
        }

        matches.forEach { (match, emoji) ->
            appendPlain(match.range.first)
            val rawEnd = match.range.last + 1
            val displayStart = transformed.length
            for (offset in match.range.first until rawEnd) originalToTransformed[offset] = displayStart
            emoji.glyph.forEachIndexed { index, char ->
                transformed.append(char)
                transformedToOriginal.add(if (index == emoji.glyph.lastIndex) rawEnd else match.range.first)
            }
            originalIndex = rawEnd
            originalToTransformed[rawEnd] = transformed.length
        }
        appendPlain(original.length)

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                originalToTransformed[offset.coerceIn(0, original.length)]

            override fun transformedToOriginal(offset: Int): Int =
                transformedToOriginal[offset.coerceIn(0, transformedToOriginal.lastIndex)]
        }
        return TransformedText(AnnotatedString(transformed.toString()), mapping)
    }
}

@Composable
internal fun V2ChatEmojiText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(text) {
        val inline = linkedMapOf<String, V2ChatReactionEmoji>()
        var cursor = 0
        var sequence = 0
        val annotated = buildAnnotatedString {
            tuiEmojiPattern.findAll(text).forEach { match ->
                if (match.range.first > cursor) append(text.substring(cursor, match.range.first))
                val emoji = chatEmojiForCode(match.value)
                if (emoji == null) {
                    append(match.value)
                } else {
                    val key = "tui-emoji-${emoji.assetIndex}-${sequence++}"
                    appendInlineContent(key, emoji.glyph)
                    inline[key] = emoji
                }
                cursor = match.range.last + 1
            }
            if (cursor < text.length) append(text.substring(cursor))
        }
        annotated to inline
    }
    val inlineContent = parsed.second.mapValues { (_, emoji) ->
        InlineTextContent(
            placeholder = Placeholder(
                width = 1.28.em,
                height = 1.28.em,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            ),
        ) {
            Image(
                painter = painterResource(chatEmojiDrawable(emoji)),
                contentDescription = emoji.glyph,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(22.dp).testTag("chat-inline-emoji-${emoji.assetIndex}"),
            )
        }
    }
    Text(
        text = parsed.first,
        inlineContent = inlineContent,
        color = color,
        fontSize = fontSize,
        modifier = modifier,
    )
}
