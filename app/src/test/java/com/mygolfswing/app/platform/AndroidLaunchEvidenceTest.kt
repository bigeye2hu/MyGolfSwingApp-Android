package com.mygolfswing.app.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory

class AndroidLaunchEvidenceTest {

    @Test
    fun launchEvidenceTemplateCoversEveryScriptRequiredKey() {
        val script = repoFile("scripts/android_check_launch_evidence.sh")
        val initScript = repoFile("scripts/android_init_launch_evidence.sh")
        val template = repoFile("docs/android-launch-evidence.template.env")

        assertTrue("Missing launch evidence checker script", script.isFile)
        assertTrue("Missing launch evidence init script", initScript.isFile)
        assertTrue("Missing launch evidence template", template.isFile)

        val requiredKeys = requiredKeysFromScript(script.readText())
        val templateKeys = envKeys(template.readText())
        val missing = requiredKeys.filterNot { it in templateKeys }

        assertTrue(
            "Launch evidence template is missing required keys:\n${missing.joinToString("\n")}",
            missing.isEmpty(),
        )
        assertEquals(
            "Launch evidence template contains duplicate keys",
            templateKeys.toSet().size,
            templateKeys.size,
        )
    }

    @Test
    fun launchEvidenceFlowIsDocumentedFromChecklistAndReadiness() {
        val checklist = repoFile("docs/android-launch-qa-checklist.md").readText()
        val readiness = repoFile("docs/android-release-readiness.md").readText()

        listOf(checklist, readiness).forEach { doc ->
            assertTrue(doc.contains("docs/android-launch-evidence.template.env"))
            assertTrue(doc.contains("scripts/android_init_launch_evidence.sh"))
            assertTrue(doc.contains("scripts/android_check_launch_evidence.sh"))
            assertTrue(doc.contains("PLAY_UPLOAD_EVIDENCE"))
            assertTrue(doc.contains("DEVICE_QA_MATRIX_EVIDENCE"))
        }
        assertTrue(checklist.contains("independent non-empty Play Console proof file"))
        assertTrue(checklist.contains("text-like local evidence files"))
        assertTrue(checklist.contains("non-empty text evidence file"))
        assertTrue(checklist.contains("META-INF"))
        assertTrue(readiness.contains("independent non-empty Play Console evidence file"))
        assertTrue(readiness.contains("text-like local evidence files"))
        assertTrue(readiness.contains("semantically text-validated evidence fields"))
        assertTrue(readiness.contains("META-INF"))
    }

    @Test
    fun launchEvidenceTemplatePinsBillingAndHomeAnalysisProof() {
        val template = repoFile("docs/android-launch-evidence.template.env").readText()

        listOf(
            "BILLING_PRODUCT_100_ACTIVE",
            "BILLING_PRODUCT_300_ACTIVE",
            "BILLING_PRODUCT_1000_ACTIVE",
            "BILLING_BACKEND_VALIDATION_POLICY",
            "BILLING_BACKEND_IDEMPOTENCY_EVIDENCE",
            "DEVICE_QA_MATRIX_EVIDENCE",
            "QA_HOME_ANALYSIS_SOURCE_EVIDENCE",
        ).forEach { key ->
            assertTrue("Missing launch evidence key: $key", template.contains("$key="))
        }
    }

    @Test
    fun launchScriptsTrackCurrentPlayTargetSdkGate() {
        val preflightScript = repoFile("scripts/android_release_preflight.sh").readText()
        val initScript = repoFile("scripts/android_init_launch_evidence.sh").readText()
        val checkerScript = repoFile("scripts/android_check_launch_evidence.sh").readText()
        val template = repoFile("docs/android-launch-evidence.template.env").readText()

        assertTrue(preflightScript.contains("--min-target-sdk=N"))
        assertTrue(preflightScript.contains("min_target_sdk=\"36\""))
        assertTrue(initScript.contains("MIN_PLAY_TARGET_SDK=36"))
        assertTrue(preflightScript.contains("lintRelease --rerun-tasks"))
        assertTrue(preflightScript.contains("Release lint warning allowlist check passed."))
        assertTrue(preflightScript.contains("Release lint summary: errors="))
        assertTrue(preflightScript.contains("Release APK signature verified by apksigner."))
        assertTrue(preflightScript.contains("Release AAB signature verified by jarsigner."))
        assertTrue(checkerScript.contains("MIN_PLAY_TARGET_SDK=36"))
        assertTrue(checkerScript.contains("RELEASE_PREFLIGHT_LINT_WARNING_CHECK"))
        assertTrue(checkerScript.contains("RELEASE_LINT_WARNING_COUNT"))
        assertTrue(checkerScript.contains("PLAY_LATEST_VERSION_CODE_BEFORE_UPLOAD"))
        assertTrue(checkerScript.contains("RELEASE_AAB_SIGNER_SHA256"))
        assertTrue(initScript.contains("aab_has_signature_files"))
        assertTrue(checkerScript.contains("aab_has_signature_files"))
        assertTrue(initScript.contains("aab_jarsigner_verified"))
        assertTrue(checkerScript.contains("aab_jarsigner_verified"))
        assertTrue(preflightScript.contains("Release AAB signer SHA-256:"))
        assertTrue(initScript.contains("jarsigner"))
        assertTrue(checkerScript.contains("jarsigner"))
        assertTrue(template.contains("RELEASE_TARGET_SDK="))
        assertTrue(template.contains("RELEASE_PREFLIGHT_LINT_WARNING_CHECK="))
        assertTrue(template.contains("RELEASE_LINT_WARNING_COUNT="))
        assertTrue(template.contains("PLAY_LATEST_VERSION_CODE_BEFORE_UPLOAD="))
        assertTrue(template.contains("RELEASE_AAB_SIGNER_SHA256="))
        assertTrue(template.contains("META-INF"))
    }

    @Test
    fun releaseSigningConfigRequiresStoreFileOutsideRepo() {
        val buildGradle = repoFile("app/build.gradle.kts").readText()

        assertTrue(buildGradle.contains("releaseStoreFileOutsideRepo"))
        assertTrue(buildGradle.contains("must be an absolute path outside the repository"))
        assertTrue(buildGradle.contains("must point outside the repository"))
    }

    @Test
    fun signedReleasePreflightImpliesCleanWorktree() {
        val preflightScript = repoFile("scripts/android_release_preflight.sh").readText()

        assertTrue(preflightScript.contains("--require-signed          Verify release APK/AAB signatures, fail if unsigned,"))
        assertTrue(preflightScript.contains("""if [[ "${'$'}require_signed" == true ]]; then"""))
        assertTrue(preflightScript.contains("require_clean_worktree=true"))
    }

    @Test
    fun launchEvidenceCheckerFailsWhenEvidenceDoesNotMatchPreflightLog() {
        val tempDir = createTempDirectory("android-launch-evidence").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "deadbee")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should fail when preflight log commit differs", result.success)
        assertTrue(result.output.contains("RELEASE_COMMIT does not match RELEASE_PREFLIGHT_LOG"))
    }

    @Test
    fun launchEvidenceCheckerRejectsUnknownKeys() {
        val tempDir = createTempDirectory("android-launch-evidence-unknown").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        files.evidenceFile.appendText("\nPLAY_UPLOAD_RELASE_ID=typo\n")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should fail on unknown keys", result.success)
        assertTrue(result.output.contains("Unknown evidence key: PLAY_UPLOAD_RELASE_ID"))
    }

    @Test
    fun launchEvidenceCheckerRejectsSecretLookingValues() {
        val tempDir = createTempDirectory("android-launch-evidence-secret").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(
            files.evidenceFile,
            "PLAY_UPLOAD_EVIDENCE",
            "MYGOLFSWING_RELEASE_STORE_PASSWORD=super-secret-password",
        )

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject signing password material", result.success)
        assertTrue(result.output.contains("Evidence value may contain secret material: PLAY_UPLOAD_EVIDENCE"))
    }

    @Test
    fun launchEvidenceCheckerRejectsSecretLookingTextEvidenceFiles() {
        val tempDir = createTempDirectory("android-launch-evidence-secret-file").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        val playProof = File(tempDir, "play-upload-proof.txt").apply {
            writeText("Play upload screen exported note\nMYGOLFSWING_RELEASE_KEY_PASSWORD=not-for-evidence\n")
        }
        replaceEvidenceValue(files.evidenceFile, "PLAY_UPLOAD_EVIDENCE", playProof.absolutePath)

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject secret material inside text evidence files", result.success)
        assertTrue(result.output.contains("PLAY_UPLOAD_EVIDENCE file may contain secret material"))
    }

    @Test
    fun launchEvidenceCheckerAllowsPurchaseTokenPolicyWithoutRawSecretSyntax() {
        val tempDir = createTempDirectory("android-launch-evidence-token-policy").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(
            files.evidenceFile,
            "BILLING_BACKEND_VALIDATION_POLICY",
            "server validates Google Play purchase token before grant, matches com.mygolfswing.app package_name product_id and purchased state, and records only token suffix in support notes",
        )

        val result = runEvidenceChecker(files.evidenceFile)

        assertTrue(result.output, result.success)
    }

    @Test
    fun launchEvidenceCheckerRequiresBillingBalanceDeltaToMatchProductCredits() {
        val tempDir = createTempDirectory("android-launch-evidence-billing-delta").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "BILLING_SUCCESS_PRODUCT_ID", "com.mygolfswingapp.credits.300")
        replaceEvidenceValue(files.evidenceFile, "BILLING_SUCCESS_BALANCE_BEFORE", "10")
        replaceEvidenceValue(files.evidenceFile, "BILLING_SUCCESS_BALANCE_AFTER", "110")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject a balance delta that does not match the purchased product", result.success)
        assertTrue(result.output.contains("Billing balance delta must match BILLING_SUCCESS_PRODUCT_ID"))
    }

    @Test
    fun launchEvidenceCheckerRequiresTraceableBillingProductConsoleAndPriceEvidence() {
        val tempDir = createTempDirectory("android-launch-evidence-billing-products").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "BILLING_PRODUCTS_CONSOLE_EVIDENCE", "products active")
        replaceEvidenceValue(files.evidenceFile, "BILLING_PRICES_LOADED_EVIDENCE", "prices visible")
        replaceEvidenceValue(files.evidenceFile, "BILLING_LOGIN_GATE_EVIDENCE", "login ok")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject vague Billing product/price/login-gate proof", result.success)
        assertTrue(result.output.contains("BILLING_PRODUCTS_CONSOLE_EVIDENCE must mention product 'com.mygolfswingapp.credits.100'"))
        assertTrue(result.output.contains("BILLING_PRODUCTS_CONSOLE_EVIDENCE must mention product 'com.mygolfswingapp.credits.300'"))
        assertTrue(result.output.contains("BILLING_PRODUCTS_CONSOLE_EVIDENCE must mention product price/localized amount status"))
        assertTrue(result.output.contains("BILLING_PRICES_LOADED_EVIDENCE must mention Google Play Billing price proof"))
        assertTrue(result.output.contains("BILLING_PRICES_LOADED_EVIDENCE must mention 100 credit tier"))
        assertTrue(result.output.contains("BILLING_PRICES_LOADED_EVIDENCE must mention 1000 credit tier"))
        assertTrue(result.output.contains("BILLING_LOGIN_GATE_EVIDENCE must mention purchase return/continuation proof"))
    }

    @Test
    fun launchEvidenceCheckerRequiresTraceableBillingOrderLabel() {
        val tempDir = createTempDirectory("android-launch-evidence-billing-order").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "BILLING_SUCCESS_ORDER_LABEL", "grant ok")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject vague order labels", result.success)
        assertTrue(result.output.contains("BILLING_SUCCESS_ORDER_LABEL must be a Google Play GPA order id or a purchase-token suffix label"))
    }

    @Test
    fun launchEvidenceCheckerAllowsPurchaseTokenSuffixOrderLabel() {
        val tempDir = createTempDirectory("android-launch-evidence-billing-token-suffix").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "BILLING_SUCCESS_ORDER_LABEL", "订单凭据尾号：ABCDEF1234")

        val result = runEvidenceChecker(files.evidenceFile)

        assertTrue(result.output, result.success)
    }

    @Test
    fun launchEvidenceCheckerRequiresBillingGrantPayloadFields() {
        val tempDir = createTempDirectory("android-launch-evidence-grant-payload").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE", "payload ok")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject vague grant-payload evidence", result.success)
        assertTrue(result.output.contains("BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE must mention product id"))
        assertTrue(result.output.contains("BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE must mention purchase token"))
        assertTrue(result.output.contains("BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE must mention amount"))
    }

    @Test
    fun launchEvidenceCheckerRequiresBillingRecoveryAndFailureScenarioProof() {
        val tempDir = createTempDirectory("android-launch-evidence-billing-scenarios").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "BILLING_CONSUMED_EVIDENCE", "ok")
        replaceEvidenceValue(files.evidenceFile, "BILLING_RESTORE_EVIDENCE", "ok")
        replaceEvidenceValue(files.evidenceFile, "BILLING_CANCEL_EVIDENCE", "ok")
        replaceEvidenceValue(files.evidenceFile, "BILLING_FAILURE_COPY_EVIDENCE", "ok")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject vague Billing scenario proof", result.success)
        assertTrue(result.output.contains("BILLING_CONSUMED_EVIDENCE must mention consume/consumed proof"))
        assertTrue(result.output.contains("BILLING_RESTORE_EVIDENCE must mention restore/recovery proof"))
        assertTrue(result.output.contains("BILLING_CANCEL_EVIDENCE must mention user-cancel proof"))
        assertTrue(result.output.contains("BILLING_FAILURE_COPY_EVIDENCE must mention product-unavailable copy"))
    }

    @Test
    fun launchEvidenceCheckerRejectsVagueBillingBackendProof() {
        val tempDir = createTempDirectory("android-launch-evidence-billing-backend").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "BILLING_BACKEND_VALIDATION_POLICY", "ok")
        replaceEvidenceValue(files.evidenceFile, "BILLING_BACKEND_IDEMPOTENCY_EVIDENCE", "pass")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject vague backend validation/idempotency proof", result.success)
        assertTrue(result.output.contains("BILLING_BACKEND_VALIDATION_POLICY must mention Google Play purchase token validation"))
        assertTrue(result.output.contains("BILLING_BACKEND_IDEMPOTENCY_EVIDENCE must describe duplicate/replay handling"))
    }

    @Test
    fun launchEvidenceCheckerRequiresBackendValidationToMatchPackageProductAndPurchaseState() {
        val tempDir = createTempDirectory("android-launch-evidence-billing-backend-strict").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(
            files.evidenceFile,
            "BILLING_BACKEND_VALIDATION_POLICY",
            "server validates Google Play purchase token before grant",
        )
        replaceEvidenceValue(
            files.evidenceFile,
            "BILLING_BACKEND_IDEMPOTENCY_EVIDENCE",
            "duplicate purchase token replay returned grant log",
        )

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject backend proof that does not describe matching rules", result.success)
        assertTrue(result.output.contains("BILLING_BACKEND_VALIDATION_POLICY must describe package/app id matching"))
        assertTrue(result.output.contains("BILLING_BACKEND_VALIDATION_POLICY must describe product id matching"))
        assertTrue(result.output.contains("BILLING_BACKEND_VALIDATION_POLICY must describe purchased/paid state validation"))
        assertTrue(result.output.contains("BILLING_BACKEND_IDEMPOTENCY_EVIDENCE must describe no-duplicate credit grant outcome"))
    }

    @Test
    fun launchEvidenceCheckerRejectsInvalidPlayTrackAndTesterAccount() {
        val tempDir = createTempDirectory("android-launch-evidence-play-track").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "PLAY_UPLOAD_TRACK", "done")
        replaceEvidenceValue(files.evidenceFile, "PLAY_INSTALL_TESTER_ACCOUNT", "tester account")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject unsupported Play track/tester evidence", result.success)
        assertTrue(result.output.contains("PLAY_UPLOAD_TRACK must be one of"))
        assertTrue(result.output.contains("PLAY_INSTALL_TESTER_ACCOUNT must be an email-like tester account"))
    }

    @Test
    fun launchEvidenceCheckerRequiresTraceablePlaySigningSetupEvidence() {
        val tempDir = createTempDirectory("android-launch-evidence-play-signing").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "PLAY_SIGNING_SETUP_EVIDENCE", "signing ok")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject vague Play signing setup evidence", result.success)
        assertTrue(result.output.contains("PLAY_SIGNING_SETUP_EVIDENCE must mention RELEASE_APPLICATION_ID 'com.mygolfswing.app'"))
        assertTrue(result.output.contains("PLAY_SIGNING_SETUP_EVIDENCE must mention Play App Signing"))
        assertTrue(result.output.contains("PLAY_SIGNING_SETUP_EVIDENCE must mention upload key registration"))
        assertTrue(result.output.contains("PLAY_SIGNING_SETUP_EVIDENCE must mention certificate fingerprint/SHA proof"))
        assertTrue(result.output.contains("PLAY_SIGNING_SETUP_EVIDENCE must include RELEASE_AAB_SIGNER_SHA256"))
    }

    @Test
    fun launchEvidenceCheckerRequiresPlaySigningSetupToNameAabSignerFingerprint() {
        val tempDir = createTempDirectory("android-launch-evidence-play-signing-fingerprint").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(
            files.evidenceFile,
            "PLAY_SIGNING_SETUP_EVIDENCE",
            "Play App Signing enabled for com.mygolfswing.app with upload key registered and certificate SHA-256 fingerprint recorded",
        )

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should require Play signing proof to include the exact AAB signer fingerprint", result.success)
        assertTrue(result.output.contains("PLAY_SIGNING_SETUP_EVIDENCE must include RELEASE_AAB_SIGNER_SHA256 '${files.aabSignerSha}'"))
    }

    @Test
    fun launchEvidenceCheckerRequiresTraceablePlayReleaseAndGrantLog() {
        val tempDir = createTempDirectory("android-launch-evidence-traceable-refs").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "PLAY_UPLOAD_RELEASE_ID", "ok")
        replaceEvidenceValue(files.evidenceFile, "BILLING_SUCCESS_SERVER_GRANT_LOG", "pass")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject vague Play release and server grant references", result.success)
        assertTrue(result.output.contains("PLAY_UPLOAD_RELEASE_ID must be a traceable Play release/version/track reference"))
        assertTrue(result.output.contains("BILLING_SUCCESS_SERVER_GRANT_LOG must be a traceable server grant log/request reference"))
    }

    @Test
    fun launchEvidenceCheckerRequiresPlayReleaseNotesToMentionVersionName() {
        val tempDir = createTempDirectory("android-launch-evidence-release-notes").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "PLAY_RELEASE_NOTES_EVIDENCE", "internal testing notes matched")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject release notes that do not mention the versionName", result.success)
        assertTrue(result.output.contains("PLAY_RELEASE_NOTES_EVIDENCE must mention RELEASE_VERSION_NAME '2.5'"))
    }

    @Test
    fun launchEvidenceCheckerRequiresPlayUploadMetadataToMatchRelease() {
        val tempDir = createTempDirectory("android-launch-evidence-play-upload-metadata").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "PLAY_UPLOAD_METADATA_EVIDENCE", "uploaded ok")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject Play upload metadata that cannot be tied to the release", result.success)
        assertTrue(result.output.contains("PLAY_UPLOAD_METADATA_EVIDENCE must mention RELEASE_APPLICATION_ID 'com.mygolfswing.app'"))
        assertTrue(result.output.contains("PLAY_UPLOAD_METADATA_EVIDENCE must mention RELEASE_VERSION_CODE '2'"))
        assertTrue(result.output.contains("PLAY_UPLOAD_METADATA_EVIDENCE must mention RELEASE_VERSION_NAME '2.5'"))
        assertTrue(result.output.contains("PLAY_UPLOAD_METADATA_EVIDENCE must mention RELEASE_TARGET_SDK '36'"))
        assertTrue(result.output.contains("PLAY_UPLOAD_METADATA_EVIDENCE must mention PLAY_UPLOAD_TRACK 'internal-testing'"))
        assertTrue(result.output.contains("PLAY_UPLOAD_METADATA_EVIDENCE must mention PLAY_UPLOAD_RELEASE_ID 'release-123'"))
    }

    @Test
    fun launchEvidenceCheckerRequiresVersionCodeAbovePlayLatest() {
        val tempDir = createTempDirectory("android-launch-evidence-play-version-code").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "PLAY_LATEST_VERSION_CODE_BEFORE_UPLOAD", "2")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject a Play upload candidate whose versionCode is not newer", result.success)
        assertTrue(result.output.contains("RELEASE_VERSION_CODE must be greater than PLAY_LATEST_VERSION_CODE_BEFORE_UPLOAD"))
    }

    @Test
    fun launchEvidenceCheckerRequiresPlayInternalInstallEvidenceToMatchRelease() {
        val tempDir = createTempDirectory("android-launch-evidence-play-install").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "PLAY_INTERNAL_INSTALL_EVIDENCE", "installed")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject Play install evidence that cannot be tied to the release", result.success)
        assertTrue(result.output.contains("PLAY_INTERNAL_INSTALL_EVIDENCE must mention PLAY_INSTALL_TESTER_ACCOUNT 'tester@example.com'"))
        assertTrue(result.output.contains("PLAY_INTERNAL_INSTALL_EVIDENCE must mention RELEASE_APPLICATION_ID 'com.mygolfswing.app'"))
        assertTrue(result.output.contains("PLAY_INTERNAL_INSTALL_EVIDENCE must mention RELEASE_VERSION_CODE '2'"))
        assertTrue(result.output.contains("PLAY_INTERNAL_INSTALL_EVIDENCE must mention RELEASE_VERSION_NAME '2.5'"))
        assertTrue(result.output.contains("PLAY_INTERNAL_INSTALL_EVIDENCE must mention PLAY_UPLOAD_TRACK 'internal-testing'"))
    }

    @Test
    fun launchEvidenceCheckerResolvesRelativeEvidenceFilesNextToEvidenceFile() {
        val tempDir = createTempDirectory("android-launch-evidence-relative").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "PLAY_UPLOAD_EVIDENCE", "play-upload.txt")

        val result = runEvidenceChecker(files.evidenceFile)

        assertTrue(result.output, result.success)
    }

    @Test
    fun launchEvidenceCheckerReadsTextEvidenceFilesForScenarioTerms() {
        val tempDir = createTempDirectory("android-launch-evidence-text-proof").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        val homeAnalysisProof = File(tempDir, "home-analysis-proof.txt").apply {
            writeText(
                """
                Home analysis source sheet QA for com.mygolfswing.app versionCode 2 versionName 2.5:
                realtime recording path passed, album trim confirmation passed, and existing swing library source passed.
                """.trimIndent(),
            )
        }
        replaceEvidenceValue(files.evidenceFile, "QA_HOME_ANALYSIS_SOURCE_EVIDENCE", homeAnalysisProof.absolutePath)

        val result = runEvidenceChecker(files.evidenceFile)

        assertTrue(result.output, result.success)
    }

    @Test
    fun launchEvidenceCheckerRejectsBinaryProofForTextValidatedEvidence() {
        val tempDir = createTempDirectory("android-launch-evidence-binary-text-proof").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        val binaryProof = File(
            tempDir,
            "device QA matrix Android 12 API 31 Android 14 API 34 pass camera gallery MediaStore GolfCam export Billing login continuation Plaza cloud.png",
        ).apply {
            writeBytes(byteArrayOf(0, 1, 2, 3, 4))
        }
        replaceEvidenceValue(files.evidenceFile, "DEVICE_QA_MATRIX_EVIDENCE", binaryProof.absolutePath)

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject binary files for text-validated evidence keys", result.success)
        assertTrue(
            result.output,
            result.output.contains("DEVICE_QA_MATRIX_EVIDENCE must be inline text or a non-empty text evidence file"),
        )
    }

    @Test
    fun launchEvidenceCheckerRequiresPlayUploadTextProofToMatchRelease() {
        val tempDir = createTempDirectory("android-launch-evidence-play-proof-text").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        val stalePlayProof = File(tempDir, "stale-play-upload-proof.txt").apply {
            writeText("Play Console upload proof for com.example.other versionCode 1 versionName 0.9 track closed-testing release old-7")
        }
        replaceEvidenceValue(files.evidenceFile, "PLAY_UPLOAD_EVIDENCE", stalePlayProof.absolutePath)

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject Play proof files from another release", result.success)
        assertTrue(result.output.contains("PLAY_UPLOAD_EVIDENCE text file must mention RELEASE_APPLICATION_ID 'com.mygolfswing.app'"))
        assertTrue(result.output.contains("PLAY_UPLOAD_EVIDENCE text file must mention RELEASE_VERSION_CODE '2'"))
        assertTrue(result.output.contains("PLAY_UPLOAD_EVIDENCE text file must mention RELEASE_VERSION_NAME '2.5'"))
        assertTrue(result.output.contains("PLAY_UPLOAD_EVIDENCE text file must mention PLAY_UPLOAD_TRACK 'internal-testing'"))
        assertTrue(result.output.contains("PLAY_UPLOAD_EVIDENCE text file must mention PLAY_UPLOAD_RELEASE_ID 'release-123'"))
    }

    @Test
    fun launchEvidenceCheckerScansTextProofFilesForSecretsAcrossEvidenceKeys() {
        val tempDir = createTempDirectory("android-launch-evidence-secret-any-file").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        val restoreProof = File(tempDir, "billing-restore-proof.txt").apply {
            writeText("restore recovered an interrupted unconsumed purchase\napi_key=not-for-launch-evidence\n")
        }
        replaceEvidenceValue(files.evidenceFile, "BILLING_RESTORE_EVIDENCE", restoreProof.absolutePath)

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should scan local text evidence files for secrets", result.success)
        assertTrue(result.output.contains("BILLING_RESTORE_EVIDENCE file may contain secret material"))
    }

    @Test
    fun launchEvidenceCheckerRejectsBuildArtifactsAsPlayUploadProof() {
        val tempDir = createTempDirectory("android-launch-evidence-play-proof").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "PLAY_UPLOAD_EVIDENCE", files.preflightLog.absolutePath)

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should require independent Play Console upload proof", result.success)
        assertTrue(result.output.contains("PLAY_UPLOAD_EVIDENCE must be an independent Play Console proof file"))
        assertTrue(result.output.contains("not the same file as RELEASE_PREFLIGHT_LOG"))
    }

    @Test
    fun launchEvidenceCheckerRejectsBuildArtifactSymlinksAsPlayUploadProof() {
        val tempDir = createTempDirectory("android-launch-evidence-play-proof-link").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        val linkedPlayProof = File(tempDir, "play-upload-link.txt")
        Files.createSymbolicLink(linkedPlayProof.toPath(), files.preflightLog.toPath())
        replaceEvidenceValue(files.evidenceFile, "PLAY_UPLOAD_EVIDENCE", linkedPlayProof.absolutePath)

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject links to build artifacts as Play Console upload proof", result.success)
        assertTrue(result.output.contains("PLAY_UPLOAD_EVIDENCE must be an independent Play Console proof file"))
        assertTrue(result.output.contains("not the same file as RELEASE_PREFLIGHT_LOG"))
    }

    @Test
    fun launchEvidenceCheckerRequiresAndroidDeviceVersionEvidence() {
        val tempDir = createTempDirectory("android-launch-evidence-device-version").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "DEVICE_ANDROID12_PLUS", "pass")
        replaceEvidenceValue(files.evidenceFile, "DEVICE_ANDROID14_PLUS", "Android 13 device passed")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject vague or too-old device evidence", result.success)
        assertTrue(result.output.contains("DEVICE_ANDROID12_PLUS must mention Android 12+ or API 31+ device evidence"))
        assertTrue(result.output.contains("DEVICE_ANDROID14_PLUS must mention Android 14+ or API 34+ device evidence"))
    }

    @Test
    fun launchEvidenceCheckerRequiresDeviceQaMatrixAcrossOsAndScenarios() {
        val tempDir = createTempDirectory("android-launch-evidence-device-matrix").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "DEVICE_QA_MATRIX_EVIDENCE", "Android devices checked")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject a vague device matrix", result.success)
        assertTrue(result.output.contains("DEVICE_QA_MATRIX_EVIDENCE must mention Android 12/API 31 matrix proof"))
        assertTrue(result.output.contains("DEVICE_QA_MATRIX_EVIDENCE must mention Android 14/API 34 matrix proof"))
        assertTrue(result.output.contains("DEVICE_QA_MATRIX_EVIDENCE must mention pass/fail notes"))
        assertTrue(result.output.contains("DEVICE_QA_MATRIX_EVIDENCE must mention camera matrix proof"))
        assertTrue(result.output.contains("DEVICE_QA_MATRIX_EVIDENCE must mention gallery matrix proof"))
        assertTrue(result.output.contains("DEVICE_QA_MATRIX_EVIDENCE must mention GolfCam export matrix proof"))
        assertTrue(result.output.contains("DEVICE_QA_MATRIX_EVIDENCE must mention Billing matrix proof"))
        assertTrue(result.output.contains("DEVICE_QA_MATRIX_EVIDENCE must mention login continuation matrix proof"))
        assertTrue(result.output.contains("DEVICE_QA_MATRIX_EVIDENCE must mention Plaza cloud matrix proof"))
    }

    @Test
    fun launchEvidenceCheckerRejectsGenericQaScenarioEvidence() {
        val tempDir = createTempDirectory("android-launch-evidence-qa-scenarios").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "QA_LOGIN_CONTINUATION_EVIDENCE", "passed")
        replaceEvidenceValue(files.evidenceFile, "QA_CAMERA_PERMISSION_EVIDENCE", "passed")
        replaceEvidenceValue(files.evidenceFile, "QA_SWING_RECORDING_EVIDENCE", "passed")
        replaceEvidenceValue(files.evidenceFile, "QA_HOME_ANALYSIS_SOURCE_EVIDENCE", "passed")
        replaceEvidenceValue(files.evidenceFile, "QA_GOLFCAM_EXPORT_EVIDENCE", "passed")
        replaceEvidenceValue(files.evidenceFile, "QA_PLAZA_CLOUD_EVIDENCE", "passed")
        replaceEvidenceValue(files.evidenceFile, "QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE", "passed")
        replaceEvidenceValue(files.evidenceFile, "QA_PROFILE_KEYBOARD_EVIDENCE", "passed")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject generic QA scenario notes", result.success)
        assertTrue(result.output.contains("QA_LOGIN_CONTINUATION_EVIDENCE must mention login/sign-in proof"))
        assertTrue(result.output.contains("QA_LOGIN_CONTINUATION_EVIDENCE must mention fresh-install proof"))
        assertTrue(result.output.contains("QA_CAMERA_PERMISSION_EVIDENCE must mention camera proof"))
        assertTrue(result.output.contains("QA_CAMERA_PERMISSION_EVIDENCE must mention permission denial proof"))
        assertTrue(result.output.contains("QA_SWING_RECORDING_EVIDENCE must mention local clip save proof"))
        assertTrue(result.output.contains("QA_HOME_ANALYSIS_SOURCE_EVIDENCE must mention album trim source proof"))
        assertTrue(result.output.contains("QA_GOLFCAM_EXPORT_EVIDENCE must mention video import proof"))
        assertTrue(result.output.contains("QA_GOLFCAM_EXPORT_EVIDENCE must mention export/save proof"))
        assertTrue(result.output.contains("QA_PLAZA_CLOUD_EVIDENCE must mention feed load proof"))
        assertTrue(result.output.contains("QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE must mention billing/purchase proof"))
        assertTrue(result.output.contains("QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE must mention successful purchase/grant proof"))
        assertTrue(result.output.contains("QA_PROFILE_KEYBOARD_EVIDENCE must mention avatar-update proof"))
        assertTrue(result.output.contains("QA_PROFILE_KEYBOARD_EVIDENCE must mention keyboard/input proof"))
    }

    @Test
    fun launchEvidenceCheckerRequiresVideoMemoLocalCameraAndOnlineSources() {
        val tempDir = createTempDirectory("android-launch-evidence-video-memo-sources").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(
            files.evidenceFile,
            "QA_VIDEO_MEMO_SOURCES_EVIDENCE",
            "video memo sources included local source only",
        )

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should require all launch-critical video Memo source paths", result.success)
        assertTrue(result.output.contains("QA_VIDEO_MEMO_SOURCES_EVIDENCE must mention camera source proof"))
        assertTrue(result.output.contains("QA_VIDEO_MEMO_SOURCES_EVIDENCE must mention online/cloud source proof"))
    }

    @Test
    fun launchEvidenceCheckerRequiresCameraPermissionDenialRecoveryContexts() {
        val tempDir = createTempDirectory("android-launch-evidence-camera-permission-contexts").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(
            files.evidenceFile,
            "QA_CAMERA_PERMISSION_EVIDENCE",
            "camera permission allowed after prompt",
        )

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should require denial/permanent-denial recovery evidence", result.success)
        assertTrue(result.output.contains("QA_CAMERA_PERMISSION_EVIDENCE must mention permission denial proof"))
        assertTrue(result.output.contains("QA_CAMERA_PERMISSION_EVIDENCE must mention permanent-denial/settings recovery proof"))
        assertTrue(result.output.contains("QA_CAMERA_PERMISSION_EVIDENCE must mention swing-recording permission proof"))
        assertTrue(result.output.contains("QA_CAMERA_PERMISSION_EVIDENCE must mention video-Memo camera-capture permission proof"))
    }

    @Test
    fun launchEvidenceCheckerRequiresLoginContinuationEntryVariants() {
        val tempDir = createTempDirectory("android-launch-evidence-login-continuation-contexts").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(
            files.evidenceFile,
            "QA_LOGIN_CONTINUATION_EVIDENCE",
            "login continuation returned after sign in",
        )

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should require fresh install, SMS, password, and original destination proof", result.success)
        assertTrue(result.output.contains("QA_LOGIN_CONTINUATION_EVIDENCE must mention fresh-install proof"))
        assertTrue(result.output.contains("QA_LOGIN_CONTINUATION_EVIDENCE must mention SMS login proof"))
        assertTrue(result.output.contains("QA_LOGIN_CONTINUATION_EVIDENCE must mention password login proof"))
        assertTrue(result.output.contains("QA_LOGIN_CONTINUATION_EVIDENCE must mention original gated destination proof"))
    }

    @Test
    fun launchEvidenceCheckerRequiresSwingRecordingSaveAndReplayAnalysis() {
        val tempDir = createTempDirectory("android-launch-evidence-swing-recording-contexts").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "QA_SWING_RECORDING_EVIDENCE", "swing recording passed")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should require saved local clip and replay-analysis proof", result.success)
        assertTrue(result.output.contains("QA_SWING_RECORDING_EVIDENCE must mention local clip save proof"))
        assertTrue(result.output.contains("QA_SWING_RECORDING_EVIDENCE must mention replay-analysis proof"))
    }

    @Test
    fun launchEvidenceCheckerRequiresGolfCamImportWorkflowPreviewAndGalleryExport() {
        val tempDir = createTempDirectory("android-launch-evidence-golfcam-contexts").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "QA_GOLFCAM_EXPORT_EVIDENCE", "golfcam export saved")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should require the full GolfCam import/workflow/preview/gallery path", result.success)
        assertTrue(result.output.contains("QA_GOLFCAM_EXPORT_EVIDENCE must mention video import proof"))
        assertTrue(result.output.contains("QA_GOLFCAM_EXPORT_EVIDENCE must mention workflow completion proof"))
        assertTrue(result.output.contains("QA_GOLFCAM_EXPORT_EVIDENCE must mention preview proof"))
        assertTrue(result.output.contains("QA_GOLFCAM_EXPORT_EVIDENCE must mention gallery/MediaStore proof"))
    }

    @Test
    fun launchEvidenceCheckerRequiresPlazaFeedPlayerInteractionsAndLoginGate() {
        val tempDir = createTempDirectory("android-launch-evidence-plaza-contexts").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "QA_PLAZA_CLOUD_EVIDENCE", "plaza cloud passed")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should require the launch-critical Plaza cloud interaction path", result.success)
        assertTrue(result.output.contains("QA_PLAZA_CLOUD_EVIDENCE must mention feed load proof"))
        assertTrue(result.output.contains("QA_PLAZA_CLOUD_EVIDENCE must mention player-open proof"))
        assertTrue(result.output.contains("QA_PLAZA_CLOUD_EVIDENCE must mention logged-in save/favorite/comment proof"))
        assertTrue(result.output.contains("QA_PLAZA_CLOUD_EVIDENCE must mention logged-out login-gate proof"))
    }

    @Test
    fun launchEvidenceCheckerRequiresLowCreditGrantAndContinuation() {
        val tempDir = createTempDirectory("android-launch-evidence-low-credit-contexts").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(
            files.evidenceFile,
            "QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE",
            "low credit billing continuation passed",
        )

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should require low-credit download, grant, and continuation proof", result.success)
        assertTrue(result.output.contains("QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE must mention online-download proof"))
        assertTrue(result.output.contains("QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE must mention successful purchase/grant proof"))
        assertTrue(result.output.contains("QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE must mention credit-grant/balance proof"))
    }

    @Test
    fun launchEvidenceCheckerRequiresProfileKeyboardAndAccountActions() {
        val tempDir = createTempDirectory("android-launch-evidence-profile-contexts").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "QA_PROFILE_KEYBOARD_EVIDENCE", "profile keyboard passed")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should require profile actions plus keyboard/navigation reachability proof", result.success)
        assertTrue(result.output.contains("QA_PROFILE_KEYBOARD_EVIDENCE must mention avatar-update proof"))
        assertTrue(result.output.contains("QA_PROFILE_KEYBOARD_EVIDENCE must mention QR image save proof"))
        assertTrue(result.output.contains("QA_PROFILE_KEYBOARD_EVIDENCE must mention feedback-submit proof"))
        assertTrue(result.output.contains("QA_PROFILE_KEYBOARD_EVIDENCE must mention account-security proof"))
        assertTrue(result.output.contains("QA_PROFILE_KEYBOARD_EVIDENCE must mention navigation-bar reachability proof"))
    }

    @Test
    fun launchEvidenceCheckerRequiresCurrentPlayTargetSdk() {
        val tempDir = createTempDirectory("android-launch-evidence-target-sdk").toFile()
        val files = createSyntheticLaunchEvidenceFiles(
            tempDir,
            logCommit = "6961d5d",
            evidenceCommit = "6961d5d",
            targetSdk = 35,
        )

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject Play-stale targetSdk evidence", result.success)
        assertTrue(result.output.contains("RELEASE_TARGET_SDK must be >= 36"))
    }

    @Test
    fun launchEvidenceCheckerRequiresLintWarningCountToMatchPreflightLog() {
        val tempDir = createTempDirectory("android-launch-evidence-lint-count").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(files.evidenceFile, "RELEASE_LINT_WARNING_COUNT", "23")

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject stale or hand-edited lint warning counts", result.success)
        assertTrue(result.output.contains("RELEASE_LINT_WARNING_COUNT does not match RELEASE_PREFLIGHT_LOG"))
    }

    @Test
    fun launchEvidenceCheckerRequiresZeroLintErrorsInPreflightLog() {
        val tempDir = createTempDirectory("android-launch-evidence-lint-errors").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        files.preflightLog.writeText(
            files.preflightLog.readText()
                .replace("0 errors, 22 warnings", "1 errors, 22 warnings")
                .replace("Release lint summary: errors=0, warnings=22", "Release lint summary: errors=1, warnings=22"),
        )

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject impossible passed logs with lint errors", result.success)
        assertTrue(result.output.contains("RELEASE_PREFLIGHT_LOG lint error count must be 0"))
    }

    @Test
    fun launchEvidenceCheckerRequiresPerArtifactSignatureMarkersInPreflightLog() {
        val tempDir = createTempDirectory("android-launch-evidence-signature-markers").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        files.preflightLog.writeText(
            files.preflightLog.readText().replace("Release AAB signature verified by jarsigner.\n", ""),
        )

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject old signed preflight logs without per-artifact signature proof", result.success)
        assertTrue(result.output.contains("RELEASE_PREFLIGHT_LOG is missing: Release AAB signature verified by jarsigner."))
    }

    @Test
    fun launchEvidenceCheckerRequiresAabSignerFingerprintToMatchArtifactAndLog() {
        val tempDir = createTempDirectory("android-launch-evidence-aab-signer").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        replaceEvidenceValue(
            files.evidenceFile,
            "RELEASE_AAB_SIGNER_SHA256",
            "00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF",
        )

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject stale or mismatched AAB signer fingerprints", result.success)
        assertTrue(result.output.contains("RELEASE_AAB_SIGNER_SHA256 does not match RELEASE_AAB_PATH"))
        assertTrue(result.output.contains("RELEASE_PREFLIGHT_LOG does not contain RELEASE_AAB_SIGNER_SHA256"))
    }

    @Test
    fun launchEvidenceCheckerRejectsAabWithoutSignatureFiles() {
        val tempDir = createTempDirectory("android-launch-evidence-unsigned-aab").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        writeAabFixture(files.aab, signed = false)
        val unsignedAabSha = sha256(files.aab.readBytes())
        replaceEvidenceValue(files.evidenceFile, "RELEASE_AAB_SHA256", unsignedAabSha)
        files.preflightLog.writeText(files.preflightLog.readText().replace(files.aabSha, unsignedAabSha))

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject release evidence that points at an unsigned AAB", result.success)
        assertTrue(result.output.contains("RELEASE_AAB_PATH must contain META-INF signature files"))
    }

    @Test
    fun launchEvidenceCheckerRejectsAabWithFakeSignatureFiles() {
        val tempDir = createTempDirectory("android-launch-evidence-fake-signed-aab").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        writeFakeSignedAabFixture(files.aab)
        val fakeAabSha = sha256(files.aab.readBytes())
        replaceEvidenceValue(files.evidenceFile, "RELEASE_AAB_SHA256", fakeAabSha)
        files.preflightLog.writeText(files.preflightLog.readText().replace(files.aabSha, fakeAabSha))

        val result = runEvidenceChecker(files.evidenceFile)

        assertFalse("Evidence checker should reject AABs with fake signature files", result.success)
        assertTrue(result.output.contains("RELEASE_AAB_PATH jarsigner verification failed"))
    }

    @Test
    fun launchEvidenceInitScriptCopiesOnlyPreflightFieldsIntoDraft() {
        val tempDir = createTempDirectory("android-launch-evidence-init").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        val draft = File(tempDir, "draft/android-launch-evidence.env")

        val initResult = runLaunchEvidenceInit(files.preflightLog, draft)

        assertTrue(initResult.output, initResult.success)
        val values = envMap(draft.readText())
        assertEquals("main", values["RELEASE_BRANCH"])
        assertEquals("6961d5d", values["RELEASE_COMMIT"])
        assertEquals("0", values["RELEASE_DIRTY_FILES"])
        assertEquals("com.mygolfswing.app", values["RELEASE_APPLICATION_ID"])
        assertEquals("2", values["RELEASE_VERSION_CODE"])
        assertEquals("2.5", values["RELEASE_VERSION_NAME"])
        assertEquals("36", values["RELEASE_TARGET_SDK"])
        assertEquals(files.apk.absolutePath, values["RELEASE_APK_PATH"])
        assertEquals(files.apkSha, values["RELEASE_APK_SHA256"])
        assertEquals(files.aab.absolutePath, values["RELEASE_AAB_PATH"])
        assertEquals(files.aabSha, values["RELEASE_AAB_SHA256"])
        assertEquals(files.aabSignerSha, values["RELEASE_AAB_SIGNER_SHA256"])
        assertEquals(files.preflightLog.absolutePath, values["RELEASE_PREFLIGHT_LOG"])
        assertEquals("true", values["RELEASE_PREFLIGHT_SIGNED_CHECK"])
        assertEquals("true", values["RELEASE_PREFLIGHT_MANIFEST_CHECK"])
        assertEquals("true", values["RELEASE_PREFLIGHT_DEBUG_RESOURCE_CHECK"])
        assertEquals("true", values["RELEASE_PREFLIGHT_LINT_WARNING_CHECK"])
        assertEquals("22", values["RELEASE_LINT_WARNING_COUNT"])
        assertEquals("", values["PLAY_APP_SIGNING_ENABLED"])
        assertEquals("", values["PLAY_LATEST_VERSION_CODE_BEFORE_UPLOAD"])
        assertEquals("", values["BILLING_PRODUCT_100_ACTIVE"])
        assertEquals("", values["DEVICE_QA_MATRIX_EVIDENCE"])
        assertEquals("", values["DEVICE_ANDROID12_PLUS"])
    }

    @Test
    fun launchEvidenceInitScriptRequiresSignedCleanPassingPreflightLog() {
        val tempDir = createTempDirectory("android-launch-evidence-init-strict").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        val invalidPreflightLog = File(tempDir, "unsigned-dirty-preflight.log").apply {
            writeText(
                files.preflightLog.readText()
                    .replace("Source state: branch=main, commit=6961d5d, dirtyFiles=0", "Source state: branch=main, commit=6961d5d, dirtyFiles=3")
                    .replace("Signed artifact check passed.\n", ""),
            )
        }
        val draft = File(tempDir, "draft/android-launch-evidence.env")

        val initResult = runLaunchEvidenceInit(invalidPreflightLog, draft)

        assertFalse("Launch evidence init should reject unsigned or dirty preflight logs", initResult.success)
        assertTrue(initResult.output.contains("expected dirtyFiles=0, got '3'"))
        assertTrue(initResult.output.contains("Preflight log is missing signed artifact verification"))
    }

    @Test
    fun launchEvidenceInitScriptRequiresZeroLintErrors() {
        val tempDir = createTempDirectory("android-launch-evidence-init-lint-errors").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        val invalidPreflightLog = File(tempDir, "lint-error-preflight.log").apply {
            writeText(
                files.preflightLog.readText()
                    .replace("0 errors, 22 warnings", "1 errors, 22 warnings")
                    .replace("Release lint summary: errors=0, warnings=22", "Release lint summary: errors=1, warnings=22"),
            )
        }
        val draft = File(tempDir, "draft/android-launch-evidence.env")

        val initResult = runLaunchEvidenceInit(invalidPreflightLog, draft)

        assertFalse("Launch evidence init should reject preflight logs with lint errors", initResult.success)
        assertTrue(initResult.output.contains("Preflight log must have zero release lint errors"))
    }

    @Test
    fun launchEvidenceInitScriptRequiresPerArtifactSignatureMarkers() {
        val tempDir = createTempDirectory("android-launch-evidence-init-signature-markers").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        files.preflightLog.writeText(
            files.preflightLog.readText().replace("Release APK signature verified by apksigner.\n", ""),
        )
        val draft = File(tempDir, "draft/android-launch-evidence.env")

        val initResult = runLaunchEvidenceInit(files.preflightLog, draft)

        assertFalse("Launch evidence init should reject old signed preflight logs without APK signature proof", initResult.success)
        assertTrue(initResult.output.contains("Preflight log is missing required success marker: Release APK signature verified by apksigner."))
    }

    @Test
    fun launchEvidenceInitScriptRequiresSignedAabArtifact() {
        val tempDir = createTempDirectory("android-launch-evidence-init-unsigned-aab").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        writeAabFixture(files.aab, signed = false)
        val unsignedAabSha = sha256(files.aab.readBytes())
        files.preflightLog.writeText(files.preflightLog.readText().replace(files.aabSha, unsignedAabSha))
        val draft = File(tempDir, "draft/android-launch-evidence.env")

        val initResult = runLaunchEvidenceInit(files.preflightLog, draft)

        assertFalse("Launch evidence init should reject an AAB artifact without bundle signature files", initResult.success)
        assertTrue(initResult.output.contains("AAB artifact from preflight log must contain META-INF signature files"))
    }

    @Test
    fun launchEvidenceInitScriptRejectsFakeSignedAabArtifact() {
        val tempDir = createTempDirectory("android-launch-evidence-init-fake-signed-aab").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        writeFakeSignedAabFixture(files.aab)
        val fakeAabSha = sha256(files.aab.readBytes())
        files.preflightLog.writeText(files.preflightLog.readText().replace(files.aabSha, fakeAabSha))
        val draft = File(tempDir, "draft/android-launch-evidence.env")

        val initResult = runLaunchEvidenceInit(files.preflightLog, draft)

        assertFalse("Launch evidence init should reject an AAB artifact with fake signature files", initResult.success)
        assertTrue(initResult.output.contains("AAB artifact jarsigner verification failed"))
    }

    @Test
    fun launchEvidenceInitScriptRequiresCurrentPlayTargetSdk() {
        val tempDir = createTempDirectory("android-launch-evidence-init-target-sdk").toFile()
        val files = createSyntheticLaunchEvidenceFiles(
            tempDir,
            logCommit = "6961d5d",
            evidenceCommit = "6961d5d",
            targetSdk = 35,
        )
        val draft = File(tempDir, "draft/android-launch-evidence.env")

        val initResult = runLaunchEvidenceInit(files.preflightLog, draft)

        assertFalse("Launch evidence init should reject Play-stale targetSdk logs", initResult.success)
        assertTrue(initResult.output.contains("Preflight log targetSdk must be >= 36"))
    }

    @Test
    fun launchEvidenceInitScriptRequiresLoggedArtifactsToMatchSha() {
        val tempDir = createTempDirectory("android-launch-evidence-init-sha").toFile()
        val files = createSyntheticLaunchEvidenceFiles(tempDir, logCommit = "6961d5d", evidenceCommit = "6961d5d")
        files.aab.writeText("overwritten aab artifact\n")
        val draft = File(tempDir, "draft/android-launch-evidence.env")

        val initResult = runLaunchEvidenceInit(files.preflightLog, draft)

        assertFalse("Launch evidence init should reject stale artifact paths or hashes", initResult.success)
        assertTrue(initResult.output.contains("AAB artifact SHA-256 does not match preflight log"))
    }

    private fun requiredKeysFromScript(text: String): List<String> {
        val arrayBody = Regex("""REQUIRED_KEYS=\(\n([\s\S]*?)\n\)""")
            .find(text)
            ?.groupValues
            ?.get(1)
            .orEmpty()
        return arrayBody
            .lineSequence()
            .map { it.trim() }
            .filter { it.matches(Regex("""[A-Z0-9_]+""")) }
            .toList()
    }

    private fun envKeys(text: String): List<String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line -> Regex("""^([A-Z0-9_]+)=""").find(line)?.groupValues?.get(1) }
            .toList()

    private fun repoFile(path: String): File =
        listOf(File(path), File("../$path")).first { it.exists() }

    private fun repoRoot(): File =
        listOf(File("."), File("..")).first { File(it, "scripts/android_check_launch_evidence.sh").isFile }

    private fun runEvidenceChecker(evidenceFile: File): ProcessResult {
        val process = ProcessBuilder(
            repoFile("scripts/android_check_launch_evidence.sh").absolutePath,
            evidenceFile.absolutePath,
        )
            .directory(repoRoot())
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return ProcessResult(success = exitCode == 0, output = output)
    }

    private fun runLaunchEvidenceInit(preflightLog: File, output: File): ProcessResult {
        val process = ProcessBuilder(
            repoFile("scripts/android_init_launch_evidence.sh").absolutePath,
            "--preflight-log=${preflightLog.absolutePath}",
            "--output=${output.absolutePath}",
        )
            .directory(repoRoot())
            .redirectErrorStream(true)
            .start()
        val text = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return ProcessResult(success = exitCode == 0, output = text)
    }

    private fun createSyntheticLaunchEvidenceFiles(
        tempDir: File,
        logCommit: String,
        evidenceCommit: String,
        targetSdk: Int = 36,
    ): SyntheticLaunchEvidenceFiles {
        val apk = File(tempDir, "release.apk").apply { writeText("fake apk artifact\n") }
        val aab = File(tempDir, "release.aab").also { writeAabFixture(it, signed = true) }
        val playEvidence = File(tempDir, "play-upload.txt").apply {
            writeText("Play Console upload proof for com.mygolfswing.app versionCode 2 versionName 2.5 track internal-testing release release-123\n")
        }
        val apkSha = sha256(apk.readBytes())
        val aabSha = sha256(aab.readBytes())
        val aabSignerSha = aabSignerSha256(aab)
        val preflightLog = File(tempDir, "preflight.log").apply {
            writeText(
                """
                == Android release preflight ==
                Source state: branch=main, commit=$logCommit, dirtyFiles=0
                Release metadata: applicationId=com.mygolfswing.app, versionCode=2, versionName=2.5
                Release SDK: minSdk=26, targetSdk=$targetSdk
                Release APK signature verified by apksigner.
                Release AAB signature verified by jarsigner.
                Release AAB signer SHA-256: $aabSignerSha
                Signed artifact check passed.
                Merged release manifest check passed.
                OK ${apk.absolutePath} (${apk.length()} bytes)
                SHA-256 ${apk.absolutePath} $apkSha
                OK ${aab.absolutePath} (${aab.length()} bytes)
                SHA-256 ${aab.absolutePath} $aabSha
                Release artifact debug-resource check passed.
                Release lint warning allowlist check passed.
                0 errors, 22 warnings
                Release lint summary: errors=0, warnings=22
                Release preflight passed.
                """.trimIndent(),
            )
        }
        val evidenceFile = File(tempDir, "android-launch-evidence.env").apply {
            writeText(
                """
                RELEASE_BRANCH=main
                RELEASE_COMMIT=$evidenceCommit
                RELEASE_DIRTY_FILES=0
                RELEASE_APPLICATION_ID=com.mygolfswing.app
                RELEASE_VERSION_CODE=2
                RELEASE_VERSION_NAME=2.5
                RELEASE_TARGET_SDK=$targetSdk
                RELEASE_APK_PATH=${apk.absolutePath}
                RELEASE_APK_SHA256=$apkSha
                RELEASE_AAB_PATH=${aab.absolutePath}
                RELEASE_AAB_SHA256=$aabSha
                RELEASE_AAB_SIGNER_SHA256=$aabSignerSha
                RELEASE_PREFLIGHT_LOG=${preflightLog.absolutePath}
                RELEASE_PREFLIGHT_SIGNED_CHECK=true
                RELEASE_PREFLIGHT_MANIFEST_CHECK=true
                RELEASE_PREFLIGHT_DEBUG_RESOURCE_CHECK=true
                RELEASE_PREFLIGHT_LINT_WARNING_CHECK=true
                RELEASE_LINT_WARNING_COUNT=22
                PLAY_APP_SIGNING_ENABLED=true
                PLAY_UPLOAD_KEY_REGISTERED=true
                PLAY_SIGNING_SETUP_EVIDENCE=Play App Signing enabled for com.mygolfswing.app with upload key registered and certificate SHA-256 fingerprint $aabSignerSha recorded
                PLAY_LATEST_VERSION_CODE_BEFORE_UPLOAD=1
                PLAY_UPLOAD_TRACK=internal-testing
                PLAY_UPLOAD_RELEASE_ID=release-123
                PLAY_RELEASE_NOTES_EVIDENCE=Play internal testing notes mention 2.5
                PLAY_UPLOAD_METADATA_EVIDENCE=Play upload metadata package com.mygolfswing.app versionCode 2 versionName 2.5 targetSdk 36 track internal-testing release release-123
                PLAY_UPLOAD_EVIDENCE=${playEvidence.absolutePath}
                PLAY_INTERNAL_INSTALL_TESTED=true
                PLAY_INTERNAL_INSTALL_EVIDENCE=tester@example.com installed and opened com.mygolfswing.app from internal-testing versionCode 2 versionName 2.5
                PLAY_INSTALL_TESTER_ACCOUNT=tester@example.com
                BILLING_PRODUCT_100_ACTIVE=true
                BILLING_PRODUCT_300_ACTIVE=true
                BILLING_PRODUCT_1000_ACTIVE=true
                BILLING_PRODUCTS_CONSOLE_EVIDENCE=Play Console active products com.mygolfswingapp.credits.100 active price configured, com.mygolfswingapp.credits.300 active price configured, com.mygolfswingapp.credits.1000 active price configured
                BILLING_PRICES_LOADED_EVIDENCE=Google Play Billing localized prices loaded for 100 300 1000 credit tiers
                BILLING_LOGIN_GATE_EVIDENCE=login gate returns to purchase
                BILLING_SUCCESS_PRODUCT_ID=com.mygolfswingapp.credits.300
                BILLING_SUCCESS_ORDER_LABEL=GPA.1234-5678
                BILLING_SUCCESS_SERVER_GRANT_LOG=grant-log-123
                BILLING_SUCCESS_BALANCE_BEFORE=10
                BILLING_SUCCESS_BALANCE_AFTER=310
                BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE=payload verified product_id purchase_token package_name quantity platform google_play transaction_id amount
                BILLING_CONSUMED_EVIDENCE=consume completed after successful grant and balance refresh
                BILLING_RESTORE_EVIDENCE=restore recovered an interrupted unconsumed purchase after page re-entry
                BILLING_CANCEL_EVIDENCE=user canceled purchase and cancel copy shown
                BILLING_FAILURE_COPY_EVIDENCE=release-safe copy verified for product-unavailable grant-failure and consume-failure with raw hidden
                BILLING_BACKEND_VALIDATION_POLICY=server validates Google Play purchase token before grant and matches com.mygolfswing.app package_name product_id and purchased state
                BILLING_BACKEND_IDEMPOTENCY_EVIDENCE=duplicate transaction replay for same purchase token returned same grant id with no double credits
                DEVICE_ANDROID12_PLUS=Android 12 device passed
                DEVICE_ANDROID14_PLUS=Android 14 device passed
                DEVICE_QA_MATRIX_EVIDENCE=Device QA matrix: Android 12/API 31 pass and Android 14/API 34 pass notes cover camera, gallery MediaStore, GolfCam export, Billing purchase, login continuation return, and Plaza cloud actions
                QA_LOGIN_CONTINUATION_EVIDENCE=fresh install SMS login and password login returned continuation to the original gated destination
                QA_CAMERA_PERMISSION_EVIDENCE=camera permission denial and permanent settings recovery passed for swing recording and video memo camera capture
                QA_SWING_RECORDING_EVIDENCE=swing recording saved a local clip and opened replay analysis
                QA_HOME_ANALYSIS_SOURCE_EVIDENCE=home analysis source sheet exercised realtime recording, album trim, and existing library entry
                QA_GOLFCAM_EXPORT_EVIDENCE=golfcam imported a video, completed the workflow steps, previewed the render, and exported saved result to gallery MediaStore
                QA_VIDEO_MEMO_SOURCES_EVIDENCE=video memo sources included local source, camera capture source, and online cloud source checks
                QA_PLAZA_CLOUD_EVIDENCE=plaza cloud feed loaded, player opened, logged-in favorite save comment actions passed, and logged-out login-gate blocked account mutations
                QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE=low credit online download opened billing purchase path, successful credit grant refreshed balance, and returned continuation to continue
                QA_PROFILE_KEYBOARD_EVIDENCE=profile avatar update, QR code image save, feedback submit, account security, keyboard input, and bottom navigation-bar reachable actions passed
                """.trimIndent(),
            )
        }
        return SyntheticLaunchEvidenceFiles(
            evidenceFile = evidenceFile,
            preflightLog = preflightLog,
            apk = apk,
            aab = aab,
            apkSha = apkSha,
            aabSha = aabSha,
            aabSignerSha = aabSignerSha,
        )
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { "%02x".format(it.toInt() and 0xff) }

    private fun replaceEvidenceValue(file: File, key: String, value: String) {
        val next = file.readLines().joinToString("\n") { line ->
            if (line.startsWith("$key=")) "$key=$value" else line
        }
        file.writeText("$next\n")
    }

    private fun writeAabFixture(file: File, signed: Boolean) {
        if (signed) {
            file.writeBytes(signedAabFixtureBytes)
        } else {
            writeUnsignedAabFixture(file)
        }
    }

    private fun writeFakeSignedAabFixture(file: File) {
        ZipOutputStream(file.outputStream()).use { zip ->
            fun addEntry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }

            addEntry("BundleConfig.pb", "bundle-config")
            addEntry("base/manifest/AndroidManifest.xml", "<manifest />")
            addEntry("META-INF/CERT.SF", "fake signature file")
            addEntry("META-INF/CERT.RSA", "fake signature block")
        }
    }

    private fun aabSignerSha256(file: File): String {
        val process = ProcessBuilder(javaTool("keytool"), "-printcert", "-jarfile", file.absolutePath)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        require(exitCode == 0) { "keytool -printcert failed with exit $exitCode:\n$output" }
        return Regex("""SHA256:\s*(([0-9A-Fa-f]{2}:){31}[0-9A-Fa-f]{2})""")
            .find(output)
            ?.groupValues
            ?.get(1)
            ?.uppercase()
            ?: error("Missing SHA256 fingerprint in keytool output:\n$output")
    }

    companion object {
        private val signedAabFixtureBytes: ByteArray by lazy {
            val tempDir = createTempDirectory("android-launch-signed-aab-fixture").toFile()
            val aab = File(tempDir, "release.aab")
            val keystore = File(tempDir, "fixture.jks")
            writeUnsignedAabFixture(aab)
            runJavaTool(
                "keytool",
                "-genkeypair",
                "-alias",
                "fixture",
                "-keyalg",
                "RSA",
                "-keysize",
                "2048",
                "-validity",
                "365",
                "-keystore",
                keystore.absolutePath,
                "-storepass",
                "changeit",
                "-keypass",
                "changeit",
                "-dname",
                "CN=AndroidLaunchEvidenceTest,O=Codex,C=US",
            )
            runJavaTool(
                "jarsigner",
                "-keystore",
                keystore.absolutePath,
                "-storepass",
                "changeit",
                "-keypass",
                "changeit",
                aab.absolutePath,
                "fixture",
            )
            aab.readBytes()
        }

        private fun writeUnsignedAabFixture(file: File) {
            ZipOutputStream(file.outputStream()).use { zip ->
                fun addEntry(name: String, content: String) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(content.toByteArray())
                    zip.closeEntry()
                }

                addEntry("BundleConfig.pb", "bundle-config")
                addEntry("base/manifest/AndroidManifest.xml", "<manifest />")
            }
        }

        private fun runJavaTool(toolName: String, vararg args: String) {
            val tool = javaTool(toolName)
            val process = ProcessBuilder(tool, *args)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw IllegalStateException("$toolName failed with exit $exitCode:\n$output")
            }
        }

        private fun javaTool(toolName: String): String {
            val javaHome = System.getenv("JAVA_HOME")?.takeIf { it.isNotBlank() }
            val candidates = listOfNotNull(
                javaHome?.let { File(it, "bin/$toolName") },
                File("/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/$toolName"),
            )
            return candidates.firstOrNull { it.isFile && it.canExecute() }?.absolutePath ?: toolName
        }
    }

    private data class ProcessResult(val success: Boolean, val output: String)
    private data class SyntheticLaunchEvidenceFiles(
        val evidenceFile: File,
        val preflightLog: File,
        val apk: File,
        val aab: File,
        val apkSha: String,
        val aabSha: String,
        val aabSignerSha: String,
    )

    private fun envMap(text: String): Map<String, String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
            .associate { line ->
                val key = line.substringBefore("=")
                val rawValue = line.substringAfter("=")
                key to rawValue.trim('"')
            }
}
