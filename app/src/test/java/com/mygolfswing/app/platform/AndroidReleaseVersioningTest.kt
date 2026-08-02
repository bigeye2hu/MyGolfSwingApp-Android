package com.mygolfswing.app.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AndroidReleaseVersioningTest {

    @Test
    fun gradleDefaultReleaseVersionMatchesCurrentAndroidLaunchCandidate() {
        val gradle = repoFile("app/build.gradle.kts").readText()

        assertTrue(gradle.contains("val releaseVersionCode = releaseProperty(\"MYGOLFSWING_VERSION_CODE\")"))
        assertTrue(gradle.contains("} ?: 2"))
        assertTrue(gradle.contains("val releaseVersionName = releaseProperty(\"MYGOLFSWING_VERSION_NAME\") ?: \"2.5\""))
        assertTrue(gradle.contains("versionCode = releaseVersionCode"))
        assertTrue(gradle.contains("versionName = releaseVersionName"))
    }

    @Test
    fun launchDocsPinTheSameDefaultReleaseVersion() {
        val readiness = repoFile("docs/android-release-readiness.md").readText()
        val iosAudit = repoFile("docs/android-ios-alignment-audit.md").readText()

        assertTrue(readiness.contains("MYGOLFSWING_VERSION_CODE=2"))
        assertTrue(readiness.contains("MYGOLFSWING_VERSION_NAME=2.5"))
        assertTrue(readiness.contains("Defaults remain `versionCode = 2` and `versionName = 2.5`"))
        assertTrue(iosAudit.contains("versionCode = 2"))
        assertTrue(iosAudit.contains("versionName = 2.5"))
    }

    @Test
    fun launchStatusNoLongerReportsVersionCodeTwoAsTooLow() {
        val result = command(
            repoFile("scripts/android_launch_status.sh").absolutePath,
            "--skip-ios",
            "--strict",
            "--min-version-code=2",
        )

        assertFalse("Strict launch status should still fail on unsigned/external gates", result.success)
        assertTrue(result.output.contains("Release metadata: applicationId=com.mygolfswing.app, versionCode=2, versionName=2.5, targetSdk=36"))
        assertFalse(result.output.contains("Release versionCode must be >= 2"))
    }

    private fun command(vararg args: String): CommandResult {
        val process = ProcessBuilder(*args)
            .redirectErrorStream(true)
            .directory(repoRoot())
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        return CommandResult(code == 0, output)
    }

    private fun repoFile(path: String): File =
        listOf(File(path), File("../$path")).first { it.exists() }

    private fun repoRoot(): File =
        listOf(File("."), File("..")).first { File(it, "scripts/android_launch_status.sh").isFile }

    private data class CommandResult(
        val success: Boolean,
        val output: String,
    )
}
