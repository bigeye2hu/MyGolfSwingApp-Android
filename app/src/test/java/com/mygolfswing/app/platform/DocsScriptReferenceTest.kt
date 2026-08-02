package com.mygolfswing.app.platform

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DocsScriptReferenceTest {

    @Test
    fun documentedShellScriptsExistInRepo() {
        val docs = docsFiles()
        val references = docs.flatMap { file ->
            scriptReferenceRegex.findAll(file.readText()).map { match ->
                ScriptReference(path = match.value, docPath = file.path)
            }
        }.distinct().sortedWith(compareBy({ it.path }, { it.docPath }))

        val missing = references.filterNot { File(it.path).isFile }
        assertTrue(
            "Docs reference shell scripts that are not present:\n${missing.joinToString("\n") { "${it.docPath}: ${it.path}" }}",
            missing.isEmpty(),
        )
    }

    @Test
    fun launchDocsDoNotUseStaleCombinedLintCommands() {
        val stalePatterns = listOf(
            "testDebugUnitTest lintRelease",
            "lintRelease assembleRelease bundleRelease",
        )
        val hits = docsFiles().flatMap { file ->
            val text = file.readText()
            stalePatterns
                .filter { pattern -> text.contains(pattern) }
                .map { pattern -> "${file.path}: $pattern" }
        }

        assertTrue(
            "Docs must keep release lint as a forced fresh step before packaging:\n${hits.joinToString("\n")}",
            hits.isEmpty(),
        )
    }

    private fun docsFiles(): List<File> {
        val root = File("docs").takeIf { it.isDirectory } ?: return emptyList()
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "md" }
            .toList()
    }

    private data class ScriptReference(
        val path: String,
        val docPath: String,
    )

    private companion object {
        val scriptReferenceRegex = Regex("""scripts/[A-Za-z0-9_.-]+\.sh""")
    }
}
