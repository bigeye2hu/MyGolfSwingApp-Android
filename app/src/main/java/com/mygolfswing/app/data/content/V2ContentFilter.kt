package com.mygolfswing.app.data.content

import android.content.Context
import com.mygolfswing.app.R
import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal data class V2ContentFilterResult(
    val isClean: Boolean,
    val matchedWords: List<String>,
    val matchedCategory: String?,
    val priority: Int,
)

@Serializable
private data class V2SensitiveWordCategory(
    val name: String,
    val priority: Int,
    val words: List<String>,
)

@Serializable
private data class V2SensitiveWordsData(
    val categories: Map<String, V2SensitiveWordCategory>,
    val whitelist: List<String>,
)

/**
 * Android counterpart of iOS `ContentFilterService`.
 *
 * The parser and matching rules intentionally preserve the iOS implementation:
 * case-insensitive substring matching, the same bundled dictionary, and the same
 * whitelist rule for a sensitive fragment contained by an allowed phrase.
 */
internal class V2ContentFilter private constructor(
    private val sensitiveWords: Map<String, WordInfo>,
    private val whitelist: Set<String>,
) {
    private data class WordInfo(val category: String, val priority: Int)

    fun check(text: String): V2ContentFilterResult {
        if (text.isEmpty()) return V2ContentFilterResult(true, emptyList(), null, 0)

        val normalizedText = text.lowercase(Locale.ROOT)
        val matchedWords = mutableListOf<String>()
        var highestPriority = 0
        var matchedCategory: String? = null

        sensitiveWords.forEach { (word, info) ->
            if (word in normalizedText) {
                val isWhitelisted = whitelist.any { allowedPhrase ->
                    allowedPhrase in normalizedText && word in allowedPhrase
                }
                if (!isWhitelisted) {
                    matchedWords += word
                    if (info.priority >= highestPriority) {
                        highestPriority = info.priority
                        matchedCategory = info.category
                    }
                }
            }
        }

        return V2ContentFilterResult(
            isClean = matchedWords.isEmpty(),
            matchedWords = matchedWords,
            matchedCategory = matchedCategory,
            priority = highestPriority,
        )
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(source: String): V2ContentFilter {
            val data = json.decodeFromString<V2SensitiveWordsData>(source)
            val words = buildMap {
                data.categories.values.forEach { category ->
                    category.words.forEach { word ->
                        put(
                            word.lowercase(Locale.ROOT),
                            WordInfo(category = category.name, priority = category.priority),
                        )
                    }
                }
            }
            return V2ContentFilter(
                sensitiveWords = words,
                whitelist = data.whitelist.mapTo(linkedSetOf()) { it.lowercase(Locale.ROOT) },
            )
        }
    }
}

internal object V2ContentFilterService {
    @Volatile
    private var cached: V2ContentFilter? = null

    fun check(context: Context, text: String): V2ContentFilterResult = instance(context).check(text)

    private fun instance(context: Context): V2ContentFilter = cached ?: synchronized(this) {
        cached ?: context.applicationContext.resources
            .openRawResource(R.raw.sensitive_words)
            .bufferedReader()
            .use { V2ContentFilter.fromJson(it.readText()) }
            .also { cached = it }
    }
}
