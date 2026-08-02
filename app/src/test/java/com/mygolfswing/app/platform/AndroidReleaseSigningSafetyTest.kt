package com.mygolfswing.app.platform

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AndroidReleaseSigningSafetyTest {

    @Test
    fun releaseSigningMaterialIsIgnoredAndRejectedFromSourceControl() {
        val gitignore = repoFile(".gitignore").readText()
        val buildGradle = repoFile("app/build.gradle.kts").readText()
        val preflight = repoFile("scripts/android_release_preflight.sh").readText()
        val readiness = repoFile("docs/android-release-readiness.md").readText()

        listOf("*.jks", "*.keystore", "*.p12", "*.pfx").forEach { pattern ->
            assertTrue(".gitignore must ignore $pattern", gitignore.contains(pattern))
        }

        assertTrue(buildGradle.contains("MYGOLFSWING_RELEASE_STORE_FILE must be an absolute path outside the repository"))
        assertTrue(buildGradle.contains("MYGOLFSWING_RELEASE_STORE_FILE must point outside the repository"))
        assertTrue(buildGradle.contains("Incomplete release signing config. Missing:"))
        assertTrue(preflight.contains("git ls-files | grep -E '\\.(jks|keystore|p12|pfx)$'"))
        assertTrue(preflight.contains("Release signing material is tracked by git"))
        assertTrue(readiness.contains("Do not commit keystores or passwords"))
        assertTrue(readiness.contains("MYGOLFSWING_RELEASE_STORE_FILE"))
        assertTrue(readiness.contains("absolute path"))
        assertTrue(readiness.contains("outside"))
        assertTrue(readiness.contains("repository"))
    }

    private fun repoFile(path: String): File =
        File(repoRoot(), path)

    private fun repoRoot(): File =
        listOf(File("."), File("..")).first { File(it, "scripts/android_release_preflight.sh").isFile }
}
