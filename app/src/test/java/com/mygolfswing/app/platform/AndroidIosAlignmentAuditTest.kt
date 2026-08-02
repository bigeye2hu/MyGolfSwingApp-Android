package com.mygolfswing.app.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class AndroidIosAlignmentAuditTest {

    @Test
    fun iosAlignmentCheckerAcceptsAuditWithCurrentHeadAndDirtyParityFiles() {
        val tempDir = createTempDirectory("android-ios-alignment-pass").toFile()
        val iosRepo = createSyntheticIosRepo(tempDir)
        val dirtySwift = File(iosRepo, "MyGolfSwingApp/Views/GolfCam/TrackAnimationLayer.swift")
        val dirtyTs = File(iosRepo, "tools/trajectory-lab/core/algorithm.ts")
        val dirtyProject = File(iosRepo, "MyGolfSwingApp.xcodeproj/project.pbxproj")
        dirtySwift.appendText("\n// dynamic start speed cap\n")
        dirtyTs.appendText("\nexport const dynamicStartSpeedLimit = true;\n")
        dirtyProject.appendText("\n// GolfCam v8 target membership\n")
        val audit = File(tempDir, "android-ios-alignment-audit.md").apply {
            writeText(
                """
                iOS HEAD ${git(iosRepo, "rev-parse", "--short=8", "HEAD").output.trim()}
                Dirty parity files: TrackAnimationLayer.swift, tools/trajectory-lab/core/algorithm.ts, and project.pbxproj.
                """.trimIndent(),
            )
        }

        val result = runIosAlignmentChecker(iosRepo, audit)

        assertTrue(result.output, result.success)
        assertTrue(result.output.contains("dirtyParityFiles=3"))
    }

    @Test
    fun iosAlignmentCheckerRejectsAuditMissingDirtySourceFile() {
        val tempDir = createTempDirectory("android-ios-alignment-fail").toFile()
        val iosRepo = createSyntheticIosRepo(tempDir)
        File(iosRepo, "MyGolfSwingApp/Views/GolfCam/TrackAnimationLayer.swift")
            .appendText("\n// dirty iOS change\n")
        val audit = File(tempDir, "android-ios-alignment-audit.md").apply {
            writeText("iOS HEAD ${git(iosRepo, "rev-parse", "--short=8", "HEAD").output.trim()}\n")
        }

        val result = runIosAlignmentChecker(iosRepo, audit)

        assertFalse("Alignment checker should fail when a dirty iOS source file is not documented", result.success)
        assertTrue(result.output.contains("Audit must mention dirty iOS parity file"))
        assertTrue(result.output.contains("TrackAnimationLayer.swift"))
    }

    @Test
    fun iosAlignmentCheckerRejectsAuditMissingDirtyProjectFile() {
        val tempDir = createTempDirectory("android-ios-alignment-project-fail").toFile()
        val iosRepo = createSyntheticIosRepo(tempDir)
        File(iosRepo, "MyGolfSwingApp.xcodeproj/project.pbxproj")
            .appendText("\n// added source membership\n")
        val audit = File(tempDir, "android-ios-alignment-audit.md").apply {
            writeText("iOS HEAD ${git(iosRepo, "rev-parse", "--short=8", "HEAD").output.trim()}\n")
        }

        val result = runIosAlignmentChecker(iosRepo, audit)

        assertFalse("Alignment checker should fail when a dirty iOS project file is not documented", result.success)
        assertTrue(result.output.contains("Audit must mention dirty iOS parity file"))
        assertTrue(result.output.contains("project.pbxproj"))
    }

    @Test
    fun iosAlignmentCheckerIsDocumentedFromAuditAndReadiness() {
        val script = repoFile("scripts/android_check_ios_alignment.sh")
        val audit = repoFile("docs/android-ios-alignment-audit.md").readText()
        val readiness = repoFile("docs/android-release-readiness.md").readText()

        assertTrue("Missing iOS alignment checker script", script.isFile)
        assertTrue(script.readText().contains("project.pbxproj"))
        assertTrue(audit.contains("scripts/android_check_ios_alignment.sh"))
        assertTrue(audit.contains("project.pbxproj"))
        assertTrue(readiness.contains("scripts/android_check_ios_alignment.sh"))
    }

    private fun createSyntheticIosRepo(parent: File): File {
        val repo = File(parent, "MyGolfSwingApp").apply { mkdirs() }
        File(repo, "MyGolfSwingApp/Views/GolfCam").mkdirs()
        File(repo, "MyGolfSwingApp.xcodeproj").mkdirs()
        File(repo, "tools/trajectory-lab/core").mkdirs()
        File(repo, "MyGolfSwingApp/Views/GolfCam/TrackAnimationLayer.swift")
            .writeText("final class TrackAnimationLayer {}\n")
        File(repo, "MyGolfSwingApp.xcodeproj/project.pbxproj")
            .writeText("// !$*UTF8*$!\n")
        File(repo, "tools/trajectory-lab/core/algorithm.ts")
            .writeText("export const algorithm = 'v8';\n")
        git(repo, "init")
        git(repo, "add", ".")
        git(repo, "-c", "user.name=Codex", "-c", "user.email=codex@example.com", "commit", "-m", "initial ios fixture")
        return repo
    }

    private fun runIosAlignmentChecker(iosRepo: File, audit: File): CommandResult =
        command(
            repoFile("scripts/android_check_ios_alignment.sh").absolutePath,
            "--ios-repo=${iosRepo.absolutePath}",
            "--audit=${audit.absolutePath}",
        )

    private fun git(repo: File, vararg args: String): CommandResult =
        command("git", "-C", repo.absolutePath, *args)

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
        listOf(File("."), File("..")).first { File(it, "scripts/android_check_ios_alignment.sh").isFile }

    private data class CommandResult(
        val success: Boolean,
        val output: String,
    )
}
