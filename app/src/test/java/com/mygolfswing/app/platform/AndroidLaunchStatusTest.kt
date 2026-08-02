package com.mygolfswing.app.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AndroidLaunchStatusTest {

    @Test
    fun launchStatusSummarizesLocalStateWithoutRunningPreflight() {
        val result = command(repoFile("scripts/android_launch_status.sh").absolutePath, "--skip-ios")

        assertTrue(result.output, result.success)
        assertTrue(result.output.contains("== Android launch status =="))
        assertTrue(result.output.contains("Source state:"))
        assertTrue(result.output.contains("Release metadata:"))
        assertTrue(result.output.contains("Signature:"))
        assertTrue(result.output.contains("iOS alignment: skipped"))
        assertTrue(result.output.contains("External evidence: not checked"))
        assertTrue(result.output.contains("android_release_preflight.sh --require-clean-worktree --require-signed"))
    }

    @Test
    fun launchStatusStrictModeFailsWithoutExternalEvidence() {
        val result = command(repoFile("scripts/android_launch_status.sh").absolutePath, "--skip-ios", "--strict")

        assertFalse("Strict launch status should fail without external launch evidence", result.success)
        assertTrue(result.output.contains("Launch readiness: attention needed"))
        assertTrue(result.output.contains("Strict launch readiness requires signed release artifacts"))
        assertTrue(result.output.contains("unsigned AAB") || result.output.contains("missing META-INF signature files"))
        assertTrue(result.output.contains("iOS alignment audit cannot be skipped"))
        assertTrue(result.output.contains("External launch evidence file is required"))
    }

    @Test
    fun launchStatusStrictModeHonorsMinimumVersionCode() {
        val result = command(
            repoFile("scripts/android_launch_status.sh").absolutePath,
            "--skip-ios",
            "--strict",
            "--min-version-code=3",
        )

        assertFalse("Strict launch status should fail when the built versionCode is below the requested floor", result.success)
        assertTrue(result.output.contains("Release versionCode must be >= 3"))
    }

    @Test
    fun launchStatusIsDocumentedFromChecklistAndReadiness() {
        val script = repoFile("scripts/android_launch_status.sh")
        val checklist = repoFile("docs/android-launch-qa-checklist.md").readText()
        val readiness = repoFile("docs/android-release-readiness.md").readText()

        assertTrue("Missing Android launch status script", script.isFile)
        assertTrue(script.readText().contains("apksigner"))
        assertTrue(script.readText().contains("jarsigner"))
        assertTrue(script.readText().contains("aab_jarsigner_verified"))
        assertTrue(script.readText().contains("META-INF"))
        assertTrue(script.readText().contains("--min-version-code"))
        assertTrue(checklist.contains("scripts/android_launch_status.sh"))
        assertTrue(checklist.contains("scripts/android_launch_status.sh --strict --min-version-code=<next Play versionCode> --evidence=path/to/android-launch-evidence.env"))
        assertTrue(readiness.contains("scripts/android_launch_status.sh"))
        assertTrue(readiness.contains("scripts/android_launch_status.sh --strict --min-version-code=<next Play versionCode> --evidence=path/to/android-launch-evidence.env"))
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
