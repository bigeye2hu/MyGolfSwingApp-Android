package com.mygolfswing.app.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AndroidLaunchDirtySummaryTest {

    @Test
    fun dirtySummaryReportsReviewBucketsWithoutRunningGradle() {
        val result = command(repoFile("scripts/android_launch_dirty_summary.sh").absolutePath, "--max-paths=10")

        assertTrue(result.output, result.success)
        assertTrue(result.output.contains("== Android launch dirty summary =="))
        assertTrue(result.output.contains("Source state:"))
        assertTrue(result.output.contains("dirtyFiles="))
        assertTrue(result.output.contains("Gradle").not())

        if (dirtyFiles(result.output) == 0) {
            assertTrue(result.output.contains("Dirty gate: clean"))
            assertTrue(result.output.contains("Launch source tree is clean."))
        } else {
            assertTrue(result.output.contains("Dirty gate: development"))
            assertTrue(result.output.contains("Status counts:"))
            assertTrue(result.output.contains("Area counts:"))
            assertTrue(result.output.contains("Review order:"))
            assertTrue(result.output.contains("Dirty paths (first 10):"))
            assertTrue(result.output.contains("Next cleanup gate: scripts/android_launch_dirty_summary.sh --fail-if-dirty"))
        }
    }

    @Test
    fun dirtySummaryFailIfDirtyMatchesCurrentSourceState() {
        val result = command(repoFile("scripts/android_launch_dirty_summary.sh").absolutePath, "--fail-if-dirty", "--max-paths=0")

        if (dirtyFiles(result.output) == 0) {
            assertTrue(result.output, result.success)
            assertTrue(result.output.contains("Launch source tree is clean."))
        } else {
            assertFalse("Fail-if-dirty should fail while the worktree has dirty paths", result.success)
            assertTrue(result.output.contains("Dirty readiness: attention needed"))
        }
    }

    @Test
    fun dirtySummaryCanListAllPathsGroupedByLaunchReviewOrder() {
        val result = command(repoFile("scripts/android_launch_dirty_summary.sh").absolutePath, "--max-paths=0", "--grouped-paths")

        assertTrue(result.output, result.success)
        if (dirtyFiles(result.output) == 0) {
            assertTrue(result.output.contains("Launch source tree is clean."))
        } else {
            assertTrue(result.output.contains("Grouped dirty paths:"))
            assertTrue(
                result.output.contains("Product/runtime (") ||
                    result.output.contains("Tests (") ||
                    result.output.contains("Release docs/scripts (") ||
                    result.output.contains("Build/repo config (") ||
                    result.output.contains("Other ("),
            )
            assertFalse(result.output.contains("Dirty paths (first"))
        }
    }

    @Test
    fun dirtySummaryIsDocumentedFromChecklistAndReadiness() {
        val script = repoFile("scripts/android_launch_dirty_summary.sh")
        val checklist = repoFile("docs/android-launch-qa-checklist.md").readText()
        val readiness = repoFile("docs/android-release-readiness.md").readText()

        assertTrue("Missing Android launch dirty summary script", script.isFile)
        assertTrue(script.readText().contains("--fail-if-dirty"))
        assertTrue(script.readText().contains("--grouped-paths"))
        assertTrue(script.readText().contains("--untracked-files=all"))
        assertTrue(script.readText().contains("Review order:"))
        assertTrue(checklist.contains("scripts/android_launch_dirty_summary.sh"))
        assertTrue(checklist.contains("scripts/android_launch_dirty_summary.sh --fail-if-dirty"))
        assertTrue(checklist.contains("scripts/android_launch_dirty_summary.sh --grouped-paths --max-paths=0"))
        assertTrue(readiness.contains("scripts/android_launch_dirty_summary.sh"))
        assertTrue(readiness.contains("scripts/android_launch_dirty_summary.sh --fail-if-dirty"))
        assertTrue(readiness.contains("scripts/android_launch_dirty_summary.sh --grouped-paths --max-paths=0"))
    }

    @Test
    fun launchSourceGatesExpandUntrackedDirectoriesForDirtyCounts() {
        val statusScript = repoFile("scripts/android_launch_status.sh").readText()
        val preflightScript = repoFile("scripts/android_release_preflight.sh").readText()
        val dirtySummaryScript = repoFile("scripts/android_launch_dirty_summary.sh").readText()

        listOf(statusScript, preflightScript, dirtySummaryScript).forEach { script ->
            assertTrue(script.contains("git status --porcelain=v1 --untracked-files=all"))
        }
        assertFalse(statusScript.contains("git status --porcelain | wc -l"))
        assertFalse(preflightScript.contains("git status --porcelain | wc -l"))
        assertFalse(dirtySummaryScript.contains("git status --porcelain=v1 | wc -l"))
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

    private fun dirtyFiles(output: String): Int {
        val match = Regex("""Source state: .*dirtyFiles=(\d+)""").find(output)
        return requireNotNull(match) { output }.groupValues[1].toInt()
    }

    private data class CommandResult(
        val success: Boolean,
        val output: String,
    )
}
