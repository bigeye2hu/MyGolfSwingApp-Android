package com.mygolfswing.app.platform

import com.mygolfswing.app.data.ai.V2AIService
import com.mygolfswing.app.data.net.V2ApiConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URI

class ReleaseEndpointAuditTest {

    @Test
    fun productionApiEndpointsUseHttpsAndNonDevHosts() {
        val endpoints = listOf(
            V2ApiConfig.CLOUD_BASE_HOST,
            V2ApiConfig.DOWNLOAD_SERVER_BASE,
            V2ApiConfig.BUILTIN_AI_BASE,
        )

        endpoints.forEach { endpoint ->
            val uri = URI(endpoint)
            assertTrue("Release endpoint must use HTTPS: $endpoint", uri.scheme.equals("https", ignoreCase = true))
            assertFalse("Release endpoint must not use a local/dev host: $endpoint", hasDevHost(uri.host.orEmpty()))
        }
    }

    @Test
    fun aiProviderDefaultsUseHttpsAndNonDevHosts() {
        V2AIService.ProviderId.entries
            .mapNotNull { it.defaultBase }
            .forEach { endpoint ->
                val uri = URI(endpoint)
                assertTrue("AI provider default must use HTTPS: $endpoint", uri.scheme.equals("https", ignoreCase = true))
                assertFalse("AI provider default must not use a local/dev host: $endpoint", hasDevHost(uri.host.orEmpty()))
            }
    }

    @Test
    fun launchSourceAndAssetsDoNotEmbedCleartextOrDevEndpoints() {
        val hits = launchFiles().flatMap { file ->
            val text = file.readText()
            cleartextUrlRegex.findAll(text)
                .filter { it.value.length > "http://".length }
                .map { match -> SourceHit(file.path, match.value, contextAround(text, match.range)) } +
                devEndpointRegex.findAll(text)
                    .map { match -> SourceHit(file.path, match.value, contextAround(text, match.range)) }
        }

        assertTrue(
            "Unexpected cleartext/dev endpoint references in launch source or assets:\n" +
                hits.joinToString("\n\n") { it.describe() },
            hits.isEmpty(),
        )
    }

    @Test
    fun launchAssetsDoNotEmbedCredentialMaterial() {
        val hits = assetFiles().flatMap { file ->
            val text = file.readText()
            assetCredentialRegex.findAll(text)
                .map { match -> SourceHit(file.path, match.value, contextAround(text, match.range)) }
        }

        assertTrue(
            "Unexpected credential-looking material in launch assets:\n" +
                hits.joinToString("\n\n") { it.describe() },
            hits.isEmpty(),
        )
    }

    @Test
    fun launchSourceKeepsAIMockProviderDebugOnly() {
        val sourceFiles = mainKotlinFiles()
        val mockReferences = sourceFiles.flatMap { file ->
            val text = file.readText()
            aiMockProviderRegex.findAll(text)
                .filterNot { match -> isAllowedAIMockProviderReference(file, text, match.range) }
                .map { match -> SourceHit(file.path, match.value, contextAround(text, match.range)) }
        }

        assertTrue(
            "Unexpected AI mock provider reference in launch source:\n" +
                mockReferences.joinToString("\n\n") { it.describe() },
            mockReferences.isEmpty(),
        )

        val aiChatStore = sourceFiles.first { it.path.endsWith("V2AIChatStore.kt") }
        val aiChatStoreText = aiChatStore.readText()
        assertTrue(
            "AI mock provider must remain gated by explicit debug-only helper",
            aiChatStoreText.contains("shouldUseAIChatMockProvider(useMockProvider, ServiceLocator.isDebuggable)"),
        )

        val mockStreamCalls = aiMockStreamRegex.findAll(aiChatStoreText)
            .map { match -> SourceHit(aiChatStore.path, match.value, contextAround(aiChatStoreText, match.range)) }
            .toList()
        assertTrue(
            "AI mock stream calls must stay inside the debug-only helper gate:\n" +
                mockStreamCalls.joinToString("\n\n") { it.describe() },
            mockStreamCalls.size == 1 &&
                mockStreamCalls.single().context.contains(
                    "shouldUseAIChatMockProvider(useMockProvider, ServiceLocator.isDebuggable)",
                ),
        )
    }

    private fun launchFiles(): List<File> = mainKotlinFiles() + assetFiles()

    private fun mainKotlinFiles(): List<File> =
        existingRoot("src/main/java", "app/src/main/java")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    private fun assetFiles(): List<File> =
        existingRoot("src/main/assets", "app/src/main/assets")
            .walkTopDown()
            .filter { it.isFile && it.extension in setOf("json", "md", "txt") }
            .toList()

    private fun existingRoot(vararg candidates: String): File =
        candidates.map(::File).first { it.exists() }

    private fun hasDevHost(host: String): Boolean =
        host.equals("localhost", ignoreCase = true) ||
            host == "127.0.0.1" ||
            host == "0.0.0.0" ||
            host == "10.0.2.2" ||
            host.contains("staging", ignoreCase = true)

    private fun isAllowedAIMockProviderReference(file: File, text: String, range: IntRange): Boolean {
        if (file.path.endsWith("V2AIProvider.kt")) return true
        if (!file.path.endsWith("V2AIChatStore.kt")) return false
        val context = contextAround(text, range)
        return context.contains("import com.mygolfswing.app.data.ai.V2AIMockProvider") ||
            context.contains("var provider: V2AIProvider = V2AIMockProvider")
    }

    private fun contextAround(text: String, range: IntRange): String {
        val start = (range.first - 180).coerceAtLeast(0)
        val end = (range.last + 180).coerceAtMost(text.lastIndex)
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
        val cleartextUrlRegex = Regex("""http://[^\s"'`)>,]+""", RegexOption.IGNORE_CASE)
        val devEndpointRegex = Regex("""(?i)\b(localhost|127\.0\.0\.1|0\.0\.0\.0|10\.0\.2\.2|staging)\b""")
        val assetCredentialRegex = Regex(
            """(?i)(BEGIN (RSA )?PRIVATE KEY|AKIA[0-9A-Z]{16}|api[_-]?key\s*[:=]|secret\s*[:=]|token\s*[:=])""",
        )
        val aiMockProviderRegex = Regex("""\bV2AIMockProvider\b""")
        val aiMockStreamRegex = Regex("""\bprovider\.streamReply\(""")
    }
}
