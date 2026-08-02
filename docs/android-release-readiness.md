# Android Release Readiness

Date: 2026-07-28

## Local Gate

Use the lightweight status snapshot when deciding what to verify next:

```bash
scripts/android_launch_dirty_summary.sh
scripts/android_launch_dirty_summary.sh --grouped-paths --max-paths=0
scripts/android_launch_dirty_summary.sh --fail-if-dirty
scripts/android_launch_status.sh
scripts/android_launch_status.sh --evidence=path/to/android-launch-evidence.env
scripts/android_launch_status.sh --strict --min-version-code=<next Play versionCode> --evidence=path/to/android-launch-evidence.env
```

Use `scripts/android_launch_dirty_summary.sh` before signed handoff cleanup to group dirty paths by git status and launch-review area. `dirtyFiles` is counted with untracked directories expanded to individual files, matching the signed-candidate source gate more closely than a collapsed directory count. Add `--grouped-paths --max-paths=0` when you need the full dirty-path cleanup list grouped by product/runtime, tests, release docs/scripts, and build/repo config. `--fail-if-dirty` is the lightweight source cleanup gate: it exits non-zero until the worktree reaches `dirtyFiles=0`, without running Gradle or mutating files.

`--strict` is the lightweight handoff check: it fails until the source tree is clean, release metadata/artifacts are present and signed, versionCode satisfies `--min-version-code`, iOS alignment is checked/current, and the external Play/Billing/device evidence file passes. The status script checks APK signatures with `apksigner`, requires AAB `META-INF` signature files before accepting `jarsigner`, and does this without rerunning Gradle.

Use the one-command preflight before preparing a release candidate:

```bash
scripts/android_release_preflight.sh
```

For the exact signed build that will be handed to Play Console, require signing and a version-code floor:

```bash
MYGOLFSWING_VERSION_CODE=2 \
MYGOLFSWING_VERSION_NAME=2.5 \
MYGOLFSWING_RELEASE_STORE_FILE=/absolute/path/outside/repo/release.keystore \
MYGOLFSWING_RELEASE_STORE_PASSWORD=... \
MYGOLFSWING_RELEASE_KEY_ALIAS=... \
MYGOLFSWING_RELEASE_KEY_PASSWORD=... \
scripts/android_release_preflight.sh --require-clean-worktree --require-signed --min-version-code=2 --min-target-sdk=36
```

`--require-signed` also implies the clean-worktree gate; the command keeps `--require-clean-worktree` explicit so release handoffs show every required gate at the call site. It uses Android Studio's bundled JBR when the system Java runtime is not installed, prints the source `branch`, `commit`, and `dirtyFiles` count, runs the unit-test/lint/build gate, forces `lintRelease` to regenerate its report instead of trusting a stale Gradle output, prints a machine-readable release lint summary, reads the actual APK metadata for `applicationId` / `versionCode` / `versionName`, verifies the merged release manifest, checks the built `targetSdk` against the Play upload floor (`36` by default), fails on release lint warnings outside the dependency/SDK freshness allowlist, checks release artifacts for debug-only coroutine probe resources, prints SHA-256 digests for both artifacts, verifies that both release artifacts are present and non-empty, verifies the APK with `apksigner`, rejects AABs without `META-INF` signature files before running `jarsigner`, parses `jarsigner -verify -verbose` to confirm signed entries, prints the AAB signer certificate SHA-256, and prints per-artifact signature markers that the evidence tools require. The project Gradle daemon is configured with `-Xmx4096m` so release AAB packaging has enough heap for the preflight gate:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew lintRelease --rerun-tasks
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleRelease bundleRelease
```

Current local output without signing credentials:

- `app/build/outputs/apk/release/app-release-unsigned.apk`
- `app/build/outputs/bundle/release/app-release.aab`

Local verification snapshot on 2026-07-29:

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" scripts/android_release_preflight.sh` passed against the current development worktree.
- The preflight ran `testDebugUnitTest`, regenerated `lintRelease`, built `assembleRelease` / `bundleRelease`, verified merged release manifest constraints, checked release artifacts for debug coroutine probe resources, and enforced the release lint warning allowlist.
- Release metadata from the verified output was `applicationId=com.mygolfswing.app`, `versionCode=2`, `versionName=2.5`, `targetSdk=36`.
- Unsigned local APK: `app/build/outputs/apk/release/app-release-unsigned.apk`, SHA-256 `9d0d6132618400821850858e585736476be656c312f53259316b75f23ab2c11d`.
- Unsigned local AAB: `app/build/outputs/bundle/release/app-release.aab`, SHA-256 `f7096ecce81142dceed59529e22f6547d1d76f32998a2b522ce2c345c2569363`.
- The same run reported `0 errors, 22 warnings`; the remaining warnings are limited to the dependency/SDK freshness allowlist.
- This snapshot proves the current unsigned local release build path is healthy. It does not close the signed-candidate gate: strict launch readiness still requires `dirtyFiles=0`, a signed APK/AAB produced with the real release keystore, Play Console upload evidence, Billing evidence, backend validation evidence, and real-device QA evidence.

## Release Versioning

The release build accepts either Gradle properties or environment variables:

```bash
MYGOLFSWING_VERSION_CODE=2
MYGOLFSWING_VERSION_NAME=2.5
```

Defaults remain `versionCode = 2` and `versionName = 2.5` when no values are provided.
If `MYGOLFSWING_VERSION_CODE` is provided, it must be a positive integer; invalid values fail Gradle configuration instead of silently falling back to the default `2`.

## Release Signing

Do not commit keystores or passwords. The repo ignores common signing containers (`*.jks`, `*.keystore`, `*.p12`, `*.pfx`), and the release preflight fails if any of those files are already tracked by git. `MYGOLFSWING_RELEASE_STORE_FILE` must be an absolute path outside this repository, so an ignored local keystore under the project tree cannot be used for a release candidate. Provide all four values through Gradle properties or environment variables when building a signed release:

```bash
MYGOLFSWING_RELEASE_STORE_FILE=/absolute/path/outside/repo/release.keystore
MYGOLFSWING_RELEASE_STORE_PASSWORD=...
MYGOLFSWING_RELEASE_KEY_ALIAS=...
MYGOLFSWING_RELEASE_KEY_PASSWORD=...
```

When no signing values are provided, Gradle keeps producing the unsigned local release artifact. When any signing value is provided, all four are required; partial signing configuration fails Gradle configuration instead of silently producing an unsigned release. When all four are present, the `release` build type uses the injected signing config only after verifying the keystore path is absolute and outside the repository.

Smoke evidence on 2026-07-28:

- A temporary keystore outside the Android repository passed signed release preflight with `validateSigningRelease`, `signReleaseBundle`, APK/AAB signature verification, and injected version metadata.
- Injected `MYGOLFSWING_VERSION_CODE=2` and `MYGOLFSWING_VERSION_NAME=2.5` appeared in `app/build/outputs/apk/release/output-metadata.json`.
- A follow-up default `assembleRelease` restored the local unsigned artifact with `versionCode = 2` and `versionName = 2.5`.
- Partial signing config was verified to fail before packaging and list the missing signing variables.
- Release signing config now rejects relative keystore paths and absolute keystore paths under the repository before packaging.
- Invalid `MYGOLFSWING_VERSION_CODE=0` was verified to fail Gradle configuration before packaging.
- Default `bundleRelease` produces `app/build/outputs/bundle/release/app-release.aab`.
- The same temporary signing and version inputs passed `bundleRelease`, including `validateSigningRelease` and `signReleaseBundle`, then a follow-up default `bundleRelease` restored the local default AAB output.
- `scripts/android_release_preflight.sh` was verified against the default local unsigned release outputs.
- `scripts/android_release_preflight.sh` was also verified with temporary signing and injected `MYGOLFSWING_VERSION_CODE=2` / `MYGOLFSWING_VERSION_NAME=2.5`; it read the signed APK path from metadata, reported `applicationId=com.mygolfswing.app`, and then a follow-up default preflight restored `versionCode = 2`, `versionName = 2.5`, and the unsigned APK output.
- `scripts/android_release_preflight.sh --require-signed --min-version-code=1` was verified to fail against the default unsigned output, while `--require-signed --min-version-code=2` passed with temporary signing and injected `versionCode = 2`; the signed gate now verifies the release APK with Android SDK `apksigner`, requires AAB `META-INF` signature files before accepting parsed JDK `jarsigner` output, prints the AAB signer certificate SHA-256 plus per-artifact signature markers, and then a follow-up default preflight restores the local unsigned outputs.
- `scripts/android_release_preflight.sh --require-signed` now also enables the clean-worktree gate, so signed release-candidate checks cannot be run from dirty source; default unsigned preflight remains available for local development smoke tests.
- `scripts/android_release_preflight.sh` now prints SHA-256 digests for the exact APK and AAB it verifies; this was smoke-tested for both default unsigned output and temporary signed release-candidate output, then restored to the default unsigned output.
- `scripts/android_release_preflight.sh` now prints source `branch`, `commit`, and `dirtyFiles`; `--require-clean-worktree` was verified to fail fast against the current dirty worktree, while default local preflight still allows development builds.
- `scripts/android_launch_dirty_summary.sh` now provides a no-Gradle source cleanup snapshot that groups dirty paths by status, launch-review area, and recommended review order; status, dirty summary, and preflight source gates all count dirty paths with `--untracked-files=all`, so untracked test/script directories are expanded instead of hidden behind one directory entry; `--fail-if-dirty` mirrors the clean-source handoff gate for quick checks before running heavier release scripts.
- `scripts/android_release_preflight.sh` now forces `lintRelease --rerun-tasks` before packaging and prints `Release lint summary: errors=0, warnings=N`, so dependency or catalog changes cannot leave the handoff log tied to a stale release lint report.
- `scripts/android_release_preflight.sh` now fails if git is tracking release signing container files (`*.jks`, `*.keystore`, `*.p12`, `*.pfx`), and `.gitignore` explicitly ignores those patterns for future signing smoke tests or local upload keys.
- `scripts/android_release_preflight.sh` now verifies the built merged release manifest after `assembleRelease` / `bundleRelease`, including package/version alignment with APK metadata, Play-current targetSdk floor, release debuggability disabled, `testOnly` disabled, cleartext traffic disabled, backup disabled, no legacy broad external-storage permissions, Google Play Billing permission present, the private FileProvider shape, and an exported-component allowlist limited to `MainActivity` plus AndroidX's `android.permission.DUMP`-protected `ProfileInstallReceiver`.
- Root startup/login background sync cancellation boundaries are covered by `V2RootScreenTest`: post-auth and bootstrapping profile refresh can degrade ordinary cloud failures without blocking the app shell, while coroutine cancellation is rethrown so disposed root work is not swallowed as a completed startup sync.
- Profile refresh cancellation boundaries are covered by `V2ProfileStoreTest`: user-info and credits refresh failures are handled as degradable background refresh failures with loading state cleanup, while coroutine cancellation is rethrown instead of being treated as a completed profile refresh.
- Release packaging now excludes `DebugProbesKt.bin`, and `scripts/android_release_preflight.sh` fails if either the release APK or AAB contains that coroutine debug probe resource.
- Release lint warning scope is now enforced by `scripts/android_release_preflight.sh`: only `OldTargetApi`, `GradleDependency`, `AndroidGradlePluginVersion`, and `NewerVersionAvailable` freshness warnings are allowed to remain; any new functional, security, resource, manifest, API, or Compose warning id fails the release preflight.
- Network logging release safety is covered by `V2NetClientTest`: release builds do not install the OkHttp logging interceptor, while debuggable builds are limited to `BASIC` logging and cannot switch to `HEADERS` or `BODY` without a failing test.
- Release endpoint hygiene is covered by `ReleaseEndpointAuditTest`: production API constants and built-in AI provider defaults must use HTTPS/non-dev hosts, launch source/assets cannot embed real cleartext URLs or local/staging endpoints, bundled launch assets are scanned for credential-looking material, and the AI mock provider cannot be referenced from launch source outside its declaration plus the `V2AIChatStore` debug-only helper gate.
- Auth release-copy helpers are covered by `V2AuthComponentsTest`, including release builds hiding raw backend/exception/path detail, blank diagnostics being ignored, and debug builds preserving diagnostic detail.
- Billing release-copy and purchase-launch helpers are covered by `V2BillingCopyTest`, including release builds hiding Google Play/backend debug detail, debug builds preserving diagnostics, support order labels, the three Play product ids, the SHA-256 obfuscated account id used for Google Play `BillingFlowParams`, the local granted-token cache that prevents consume-failure restore from re-calling the grant endpoint, and BillingClient close-failure isolation.
- Billing purchase-page unavailable-state routing is covered by `V2CreditsPurchaseAvailabilityTest`: loading, missing purchase `Activity`, no Play products returned, partial Play product configuration, and all-products-ready states resolve to the intended recovery card instead of silently disabling every purchase button.
- Billing grant payload shape is covered by `V2CreditsRepositoryTest`: `/credits/add-iap` receives the Google Play product id, purchase token, package name, quantity, `google_play` platform, transaction id, reason, and amount fields expected by the launch QA checklist.
- Credits reset/calibration release-copy helpers are covered by `V2CreditsResetCopyTest`, including release builds hiding balance endpoint, token, user-path, cache-file, and exception diagnostics while debug builds preserve troubleshooting detail; refresh/reset thrown-error rendering preserves coroutine cancellation.
- Account safety release-copy helpers are covered by `V2AccountSafetyCopyTest`, including account-security and account-deletion release builds hiding raw token/path/backend detail while debug builds preserve diagnostics; thrown-error helpers preserve coroutine cancellation so dismissed dialogs/deletion flows do not surface false failures, and `V2AccountDeletionRepositoryTest` locks the repository failure helper to rethrow cancellation instead of wrapping deletion disposal as a failed delete result.
- Feedback release-copy helpers are covered by `V2FeedbackCopyTest`, including release builds hiding primary cloud-function and legacy fallback HTTP/exception diagnostics while debug builds preserve the combined channel detail; submit failure rendering preserves coroutine cancellation so dismissed feedback flows do not show stale failure copy, and `V2FeedbackRepositoryTest` locks the legacy fallback response success/error/combined-failure decision.
- Profile edit release-copy helpers are covered by `V2ProfileEditCopyTest`, including avatar upload, gallery picker, camera/FileProvider launch, and profile-save release builds hiding raw backend, path, and exception diagnostics; avatar thrown-error handling preserves coroutine cancellation while ordinary failures stay localized.
- Golf profile persistence cancellation boundaries are covered by `V2GolfProfileStoreTest` and `V2GolfProfileRepositoryTest`: local profile persist and best-effort cloud upload failures remain handled/degradable, while coroutine cancellation is rethrown so disposed profile/archive jobs are not converted into stale background sync failures.
- Profile settings QR gallery-save release-copy helpers are covered by `V2ProfileSettingsCopyTest`, including release builds hiding MediaStore, content-URI, token, local-path, and exception diagnostics while debug builds preserve troubleshooting detail; QR image save now also has an outer failure catch for thrown MediaStore operations and preserves coroutine cancellation through the gallery-save helper.
- AI chat status/automatic-archive release-copy helpers are covered by `V2AIChatCopyTest`, including release builds hiding provider endpoint, model, API-key, session id, user-path, local-file, and reflection diagnostics while debug builds preserve troubleshooting detail; the send-before-context refresh helper keeps local profile reload failures degradable while preserving coroutine cancellation; the mock-provider fallback is explicit and debug-only, with source-level coverage in `ReleaseEndpointAuditTest` so release chat traffic cannot silently fall back to demo replies.
- AI service config release-copy helpers are covered by `V2AIServiceConfigCopyTest`, including release builds hiding provider, model, API-key, base-URL, backend, and local-path diagnostics while debug builds preserve troubleshooting detail; save thrown-error handling preserves coroutine cancellation so leaving the settings surface cannot show a false save failure.
- AI archive manual-sync release-copy helpers are covered by `V2AIArchiveCopyTest`, including release builds hiding provider/backend endpoint, session id, API-key, local database path, and exception diagnostics while debug builds preserve troubleshooting detail.
- GolfCam export release-copy helpers are covered by `GolfCamExportCopyTest`, including release builds hiding codec, temp-file, MediaStore, content-URI, and user-path diagnostics while debug builds preserve troubleshooting detail; export/render and gallery-save failure helpers preserve coroutine cancellation so leaving the preview/save flow cannot surface a stale export failure.
- Online download release-copy helpers are covered by `V2OnlineDownloadCopyTest`, including fetch, cancel, confirm, and local-download failure paths hiding URL, token, job id, backend endpoint, user-path, and local-file diagnostics while debug builds preserve troubleshooting detail; failure-result rendering preserves coroutine cancellation so cancelled jobs do not show stale retry guidance, pending-task restore payload parsing rejects malformed/incomplete data while preserving saved task metadata, and `V2VideoDownloadRepositoryTest` locks the download repository Result helper to rethrow cancellation instead of wrapping disposed task calls as failed backend results.
- Goal create/edit release-copy helpers are covered by `V2GoalCopyTest`, including save and delete failure paths hiding local database, backend endpoint/status, user-path, and goal-id diagnostics while debug builds preserve troubleshooting detail; save/delete thrown-error rendering preserves coroutine cancellation.
- Text Memo create/detail release-copy helpers are covered by `V2TextMemoCopyTest`, including create-save, detail-save, and delete failure paths hiding database, backend endpoint/status, memo-id, tag-id, user-path, and local-path diagnostics while debug builds preserve troubleshooting detail.
- Video Memo creation release-copy helpers are covered by `V2VideoMemoCopyTest`, including camera launch/adopt, album import, and save failure paths hiding activity, content-URI, local-file, database, memo-id, and user-path diagnostics while debug builds preserve troubleshooting detail; Video Memo player gallery save helpers are covered by `V2MemoMediaTest`, preserving coroutine cancellation for current-frame screenshots and local-video exports so leaving the player cannot surface stale gallery failure copy.
- Text/video Memo secondary-list chip filtering is covered by `V2MemoSecondaryListFilterTest`; the top-right search affordance stays as the latest iOS disabled placeholder, while category/source filters remain locally testable.
- Memo cloud-sync cancellation boundaries are covered by `V2MemoStoreTest` and `V2MemoRepositoryTest`: local demo cleanup, cloud pull, and dirty push can degrade ordinary sync failures without blocking the Memo tab, while coroutine cancellation is rethrown so disposed login/list refresh work is not swallowed as a completed background sync.
- Swing recording release-copy helpers are covered by `V2SwingCopyTest`, including camera-bind and save failure paths hiding CameraX, device id, local-file, and clip diagnostics while debug builds preserve troubleshooting detail; camera/save thrown-error rendering preserves coroutine cancellation.
- Swing library/detail rollback release-copy helpers are covered by `V2SwingStoreCopyTest`, including retention-save and delete failure paths hiding Room operation, clip id, user-path, local-video, and thumbnail diagnostics while debug builds preserve troubleshooting detail; the store throwable helper also rethrows coroutine cancellation so disposed Swing jobs do not surface stale rollback errors.
- Plaza release-copy helpers are covered by `V2PlazaCopyTest`, including list refresh/load-more, like/save sync, coach-profile load, comment/report, and feedback failure paths hiding backend, endpoint, token, user-path, video-id, comment-id, and coach-id diagnostics while debug builds preserve troubleshooting detail; saved-video/watch-history cache parsing now skips malformed rows, preserves legacy saved placeholders, clamps restored progress, and treats malformed payloads as empty account-scoped lists; shared throwable rendering now preserves coroutine cancellation across coach/player/store failure surfaces, and `V2PlazaRepositoryTest` locks the repository Result helper to rethrow cancellation instead of wrapping it as a failed cloud result.
- FileProvider paths now cover camera temp videos, avatar temp images, and user-scoped local Memo videos under `filesDir/users/`; `FileProviderPathsTest` locks these share/capture paths for launch.
- Manifest release surface is covered by `ManifestReleaseSurfaceTest`, including release debuggability disabled, `testOnly` disabled, cleartext traffic disabled, app backup disabled, only `MainActivity` exported for launcher/share entry, optional camera hardware, no legacy broad external-storage permissions, and private grant-only FileProvider metadata.
- Android-only developer diagnostics are covered by `V2DeveloperToolsAccessTest`: release builds never enable the cloud-consistency audit entry, while debug builds still require the logged-in internal `test1` account.
- Android legal assets are covered by `LegalAssetsReleaseTest`: bundled privacy policy and user agreement must be substantive, non-placeholder documents, use Google Play purchase/refund language for Android billing, name the Android Tencent CloudBase / Tencent IM service providers, and avoid iOS/App Store/LeanCloud legal copy in the Android package.
- Release networking keeps `usesCleartextTraffic=false`; remote video downloads now normalize legacy `http://` inputs to `https://` before OkHttp and reject non-http schemes, with `V2VideoFileStoreTest` coverage.
- Home replay-analysis import now mirrors the iOS post-import trim-confirm path before a selected album video becomes a swing clip; `V2VideoFileStoreTest` covers the shared 2GB limit, trim-range clamping, whole-source passthrough detection, and remote-download URL normalization while device QA still needs to verify actual MediaMuxer output on representative videos.
- Home replay-analysis source QA is now explicit in launch evidence through `QA_HOME_ANALYSIS_SOURCE_EVIDENCE`, covering the iOS-aligned source sheet paths for realtime recording, album trim confirmation, and existing swing-library material.
- Storage management file cleanup now has `V2StorageManagementScreenTest` coverage for cache-size accounting, preserving temporary videos when only app cache is cleared, deleting only temporary video directories when requested, and stable failure copy without raw paths or exception text.
- Launch placeholder/source audit is covered by `LaunchPlaceholderAuditTest`; release source now has no allowed empty click handlers. The purchase in-flight overlay consumes background taps through pointer input, while the online-download processing CTA, profile edit wheel current row, profile notification row, and iOS-aligned Memo secondary-list search affordances are disabled without empty click handlers. Documented placeholder copy is limited to decisions such as the static notification row, the iOS AI-goal stub, disabled text voice affordance, and GolfCam deferred badges.
- The unused `V2PlaceholderTab` design-in-progress component was removed from the launch source tree, and `LaunchPlaceholderAuditTest` now also blocks generic design-placeholder copy such as `正在设计中`, `还没设计完`, and old AI placeholder notes unless a future release decision explicitly documents them.
- Launch docs script references are covered by `DocsScriptReferenceTest`; Markdown launch/checklist docs can only reference `scripts/*.sh` files that are present in the current repo, preventing QA handoff notes from pointing to missing GolfCam evidence tooling.
- External launch evidence now has a preflight-log initializer plus a checker: run `scripts/android_init_launch_evidence.sh --preflight-log=path/to/preflight.log --output=path/to/android-launch-evidence.env`, attach the Play upload proof, Billing product/test-purchase/backend-validation proof, and device QA matrix, then run `scripts/android_check_launch_evidence.sh path/to/android-launch-evidence.env`. The initializer copies only release artifact fields from the saved log, rejects unsigned/dirty/incomplete, Play-stale targetSdk, and lint-error preflight logs, verifies the logged local APK/AAB still exist and match the logged SHA-256 digests, requires AAB `META-INF` signature files plus `jarsigner` verification, binds the AAB signer certificate SHA-256, and leaves external gates blank; the checker binds evidence back to the saved preflight log's source state, release metadata, targetSdk, lint warning count/allowlist result, APK/AAB SHA-256 digests, AAB signer certificate SHA-256, AAB `META-INF` signature files, and AAB `jarsigner` verification, resolves local evidence file paths from the evidence folder before falling back to the repo root, reads text proof files for the same keyword/release checks as inline values, rejects binary files used alone for semantically text-validated evidence fields, rejects unknown evidence keys, validates Play signing setup evidence for app id, Play App Signing, upload key, and the exact `RELEASE_AAB_SIGNER_SHA256` certificate fingerprint, requires `RELEASE_VERSION_CODE` to be greater than `PLAY_LATEST_VERSION_CODE_BEFORE_UPLOAD`, validates Play upload track/tester-account shape and traceable Play release reference, requires `PLAY_UPLOAD_EVIDENCE` to be an independent non-empty Play Console evidence file instead of the APK/AAB/preflight log or a link to one of them, requires text Play upload proof to mention the exact app id, versionCode, versionName, track, and release id, requires Play release-notes evidence to mention the exact `RELEASE_VERSION_NAME`, requires Play upload metadata evidence to mention the exact app id, versionCode, versionName, targetSdk, track, and release id, requires Play internal install evidence to mention the tester account, app id, versionCode, versionName, track, and install/open proof, requires Billing product-console evidence to mention all three product IDs plus active/price status, requires price-loaded proof to mention Google Play Billing/localized prices for the 100 / 300 / 1000 credit tiers, requires Android 12+/API 31+ and Android 14+/API 34+ device-version evidence, requires `DEVICE_QA_MATRIX_EVIDENCE` to mention both OS/API rows plus pass/fail notes for camera, gallery/MediaStore, GolfCam export, Billing, login continuation, and Plaza cloud actions, requires each device QA scenario note to mention the tested action instead of generic pass text, requires the Billing success order label to be a Google Play `GPA.` order id or app-safe purchase-token suffix, requires a traceable server grant log/request reference, verifies the Billing balance delta matches the purchased product's 100 / 300 / 1000 credit amount, requires grant payload proof to mention the product id, purchase token, package name, quantity, `google_play` platform, transaction id, and amount fields, requires consume/restore/cancel/failure-copy evidence to describe the actual Billing scenario instead of generic pass text, requires backend proof to mention server-side Google Play purchase-token validation before grant plus package/app id matching, product id matching, purchased/paid state validation, duplicate/replay handling, and no-duplicate credit outcome, and rejects common secret-looking values in both evidence fields and text-like local evidence files so misspelled fields, vague backend proof, impossible purchase evidence, stale Play upload proof, or leaked credentials cannot be ignored. `AndroidLaunchEvidenceTest` keeps the scripts, template, and launch docs in sync.
- Low-risk KTX lint cleanup for support contact intents, video-frame capture, SharedPreferences-backed draft/cache/config writes, GolfCam export canvas save/restore, GolfCam thumbnail scaling, and profile QR bitmap generation reduced the release lint output to dependency/SDK freshness warnings only; the QR-code generator dependency was then bumped from ZXing `3.5.3` to the patch release `3.5.4`, and Android test-only dependencies were bumped to `androidx.test.ext:junit 1.3.0` plus `espresso-core 3.7.0`, reducing current local release lint to `0 errors, 22 warnings`.

## External Launch Gates

- Play Console app signing / upload key setup and first signed AAB upload.
- Billing product activation for the configured credit SKUs.
- Real Google Play test purchases covering success, cancel, restore, grant failure, and consume failure.
- Backend receipt/token validation policy for credit grants.
- Device QA for camera/media permissions, GolfCam preview/export, gallery save, login continuations, Plaza cloud actions, and release failure copy.
- Use `docs/android-launch-qa-checklist.md` plus `docs/android-launch-evidence.template.env` to capture the external evidence that cannot be proven by local Gradle tasks; initialize the draft with `scripts/android_init_launch_evidence.sh`, then run `scripts/android_check_launch_evidence.sh` before treating the external gates as closed.
- Run `scripts/android_check_ios_alignment.sh` whenever iOS changes are pulled or edited, then update `docs/android-ios-alignment-audit.md` before claiming Android has caught up to the current iOS HEAD.

## Non-Blocking Cleanup

- Lint currently reports 22 dependency/SDK freshness warnings after the local release gate passes; the preflight now fails if any other lint warning category appears.
- Treat AGP/Gradle/Kotlin/AndroidX/Media3/compileSdk upgrades beyond the current Play targetSdk floor as a separate compatibility pass, not a last-minute release-candidate change.
