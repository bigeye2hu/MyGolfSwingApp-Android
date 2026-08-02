package com.mygolfswing.app.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LaunchPlaceholderAuditTest {

    @Test
    fun launchPlaceholdersAreIntentionalAndDocumented() {
        val sourceFiles = mainKotlinFiles()

        val emptyClickHandlers = sourceFiles.flatMap { file ->
            emptyClickRegex.findAll(file.readText()).map { match ->
                SourceHit(file.path, match.value, contextAround(file.readText(), match.range))
            }
        }
        assertTrue(
            "Unexpected empty onClick handlers:\n${emptyClickHandlers.joinToString("\n\n") { it.describe() }}",
            emptyClickHandlers.isEmpty(),
        )

        val placeholderHits = sourceFiles.flatMap { file ->
            launchPlaceholderPhrases.flatMap { phrase ->
                Regex.escape(phrase).toRegex().findAll(file.readText()).map { match ->
                    SourceHit(file.path, phrase, contextAround(file.readText(), match.range))
                }
            }
        }
        val unexpectedPlaceholders = placeholderHits.filterNot(::isAllowedPlaceholderPhrase)
        assertTrue(
            "Unexpected launch placeholder copy:\n${unexpectedPlaceholders.joinToString("\n\n") { it.describe() }}",
            unexpectedPlaceholders.isEmpty(),
        )
        assertEquals(
            "Known placeholder count changed; update this audit with a documented launch decision.",
            4,
            placeholderHits.size,
        )
    }

    private fun mainKotlinFiles(): List<File> {
        val root = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).first { it.isDirectory }
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
    }

    private fun isAllowedPlaceholderPhrase(hit: SourceHit): Boolean {
        return when (hit.value) {
            "AI 推荐目标功能尚未接入。" ->
                hit.path.endsWith("V2NewGoalScreen.kt")
            "敬请期待" ->
                hit.path.endsWith("V2ProfileSettingsScreen.kt") && hit.context.contains("通知设置")
            "语音输入暂未接入" ->
                hit.path.endsWith("V2MemoCreateTextScreen.kt")
            else -> false
        }
    }

    private fun contextAround(text: String, range: IntRange): String {
        val start = (range.first - 220).coerceAtLeast(0)
        val end = (range.last + 220).coerceAtMost(text.lastIndex)
        return text.substring(start..end)
    }

    private data class SourceHit(
        val path: String,
        val value: String,
        val context: String,
    ) {
        fun describe(): String = "$path\n$value\n${context.trim()}"
    }

    private companion object {
        val emptyClickRegex = Regex("""onClick\s*=\s*\{\s*}""")
        val launchPlaceholderPhrases = listOf(
            "AI 推荐目标功能尚未接入。",
            "敬请期待",
            "语音输入暂未接入",
            "正在设计中",
            "还没设计完",
            "等 AI 页设计完成后接入",
        )
    }
}
