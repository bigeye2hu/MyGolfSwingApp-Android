# Android Launch QA Checklist

Date: 2026-07-28

This checklist tracks evidence that cannot be proven by the local Gradle gate alone.

Save the signed release preflight output, create an evidence draft, fill every external key, and run the checker:

```bash
scripts/android_launch_dirty_summary.sh
scripts/android_launch_dirty_summary.sh --grouped-paths --max-paths=0
scripts/android_launch_dirty_summary.sh --fail-if-dirty
scripts/android_launch_status.sh
scripts/android_launch_status.sh --strict --min-version-code=<next Play versionCode> --evidence=path/to/android-launch-evidence.env
scripts/android_init_launch_evidence.sh --preflight-log=path/to/preflight.log --output=path/to/android-launch-evidence.env
scripts/android_check_launch_evidence.sh path/to/android-launch-evidence.env
```

Use `scripts/android_launch_dirty_summary.sh` before the signed-candidate handoff to see whether remaining dirty paths are product/runtime code, tests, release docs/scripts, build config, or other cleanup. Use `scripts/android_launch_dirty_summary.sh --grouped-paths --max-paths=0` for the full grouped cleanup list, and `scripts/android_launch_dirty_summary.sh --fail-if-dirty` when a local handoff step should fail until `dirtyFiles=0`, without running Gradle.
Use `scripts/android_launch_status.sh --evidence=path/to/android-launch-evidence.env` for a lightweight snapshot of the current source state, last built release artifacts, artifact signature status, iOS alignment audit, and external evidence result before rerunning the full build gate. Use `scripts/android_launch_status.sh --strict --min-version-code=<next Play versionCode> --evidence=path/to/android-launch-evidence.env` when a handoff script should fail unless the source state is clean, release artifacts are signed, versionCode is high enough for Play, iOS alignment is checked, and external evidence is ready.
The initializer writes the same key set as `docs/android-launch-evidence.template.env`, using the preflight log for build-artifact fields and leaving external evidence fields blank. It rejects unsigned, dirty, incomplete, Play-stale targetSdk, lint-error, missing-artifact, SHA-mismatched, AAB-signature-missing, or AAB-jarsigner-failing preflight logs; rerun `scripts/android_release_preflight.sh --require-clean-worktree --require-signed --min-version-code=<next Play versionCode> --min-target-sdk=36` for the exact release candidate if initialization fails.
Do not add ad-hoc keys or secret-looking values; the checker rejects unknown fields and common password/private-key/API-key/bearer-token patterns in evidence values and text-like local evidence files so typoed evidence slots and leaked credentials cannot be silently ignored. Do not treat the external launch gates below as complete until this evidence check passes for the exact signed release candidate uploaded to Play Console.
The checker compares the evidence file against the saved preflight log's source branch, commit, dirty-file count, release metadata, targetSdk, lint warning count/allowlist result, APK/AAB SHA-256 digests, AAB signer certificate SHA-256, AAB `META-INF` signature files, and AAB `jarsigner` verification, so keep the log from the same signed build that was uploaded or handed to testers.
For screenshots, Play upload notes, and other local proof files, paths may be absolute, relative to the evidence file's folder, or relative to the Android repo root. Text proof files are read for the same keyword/release checks as inline values; screenshots or other binary proof should be paired with the metadata fields in `docs/android-launch-evidence.template.env`. Fields with semantic keyword checks must be inline text or a non-empty text evidence file; binary proof alone is not accepted for those fields. If `PLAY_UPLOAD_EVIDENCE` is a text file, it must mention the app id, versionCode, versionName, track, and Play release id for this exact upload.

## Build Artifact Gate

- Run `scripts/android_release_preflight.sh --require-clean-worktree --require-signed --min-version-code=<next Play versionCode> --min-target-sdk=36` before every release candidate handoff.
- Run `scripts/android_launch_dirty_summary.sh --fail-if-dirty` first when cleaning up a release-candidate branch; it should pass before the signed preflight can pass.
- Treat `--require-signed` as a signed-candidate gate: it also requires a clean git worktree, while the command keeps `--require-clean-worktree` explicit for checklist readability.
- Confirm the script reports the expected source `branch`, `commit`, and `dirtyFiles=0`.
- Confirm the script reports `applicationId=com.mygolfswing.app`.
- Confirm the script reports `targetSdk=36` or higher.
- Record the highest existing Play Console `versionCode` in `PLAY_LATEST_VERSION_CODE_BEFORE_UPLOAD`; the evidence checker requires the reported `RELEASE_VERSION_CODE` to be higher.
- Confirm the reported `versionName` matches the intended release notes.
- Confirm the script reports `Release APK signature verified by apksigner.`, `Release AAB signature verified by jarsigner.`, and `Signed artifact check passed.`; for AABs, this requires both `META-INF` signature files and parsed `jarsigner -verify -verbose` proof because `jarsigner` can return success for an unsigned bundle.
- Confirm the script reports `Release AAB signer SHA-256: ...`, and compare that fingerprint against Play Console upload-key/App Signing setup evidence.
- Confirm the script reports `Merged release manifest check passed.`
- Confirm the script reports `Release artifact debug-resource check passed.`
- Confirm the script reports `Release lint warning allowlist check passed.`
- Confirm the APK and AAB paths printed by the script are non-empty.
- Record the source branch/commit and SHA-256 digest printed for the uploaded AAB and any APK handed to testers.
- For Play upload, build with all four `MYGOLFSWING_RELEASE_*` signing values plus the intended `MYGOLFSWING_VERSION_CODE` and `MYGOLFSWING_VERSION_NAME`; `MYGOLFSWING_RELEASE_STORE_FILE` must be an absolute path outside the Android repository.

## Play Console Gate

- Play App Signing is enabled for `com.mygolfswing.app`.
- The upload key used by the release machine is registered in Play Console.
- `PLAY_SIGNING_SETUP_EVIDENCE` mentions the app id, Play App Signing, upload key registration, and the exact `RELEASE_AAB_SIGNER_SHA256` certificate fingerprint.
- `PLAY_LATEST_VERSION_CODE_BEFORE_UPLOAD` records the highest existing Play Console versionCode before upload, using `0` only for the first-ever Play upload.
- The signed `app-release.aab` uploads to an internal testing track without certificate, package-name, version-code, or target-SDK rejection.
- Record a traceable Play release/version/track reference in `PLAY_UPLOAD_RELEASE_ID`; do not use generic `ok` / `pass` text.
- The internal testing release notes match `MYGOLFSWING_VERSION_NAME`; `PLAY_RELEASE_NOTES_EVIDENCE` must mention the exact `RELEASE_VERSION_NAME`.
- Record Play upload metadata in `PLAY_UPLOAD_METADATA_EVIDENCE` that mentions the exact app id, versionCode, versionName, targetSdk, track, and `PLAY_UPLOAD_RELEASE_ID` for this build.
- Attach `PLAY_UPLOAD_EVIDENCE` as an independent non-empty Play Console proof file; do not point it at the APK, AAB, or saved preflight log.
- At least one email-like tester account can install the internal-track build from Google Play; record that account in `PLAY_INSTALL_TESTER_ACCOUNT`.
- Record `PLAY_INTERNAL_INSTALL_EVIDENCE` with that tester account, app id, versionCode, versionName, track, and install/open proof for this Play build.

## Billing Gate

- Product IDs are active in Play Console:
  - `com.mygolfswingapp.credits.100`
  - `com.mygolfswingapp.credits.300`
  - `com.mygolfswingapp.credits.1000`
- `BILLING_PRODUCTS_CONSOLE_EVIDENCE` mentions all three product IDs, active/available status, and price/localized amount status.
- The purchase page loads Google Play localized prices for all three products; `BILLING_PRICES_LOADED_EVIDENCE` must mention Google Play Billing/localized prices and the 100 / 300 / 1000 credit tiers.
- Logged-out users are routed through login before reaching purchase recovery; `BILLING_LOGIN_GATE_EVIDENCE` must mention both login and returning/continuing to purchase.
- A successful test purchase calls `/credits/add-iap` with product id, purchase token, package name, quantity, platform, transaction id, and amount.
- `BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE` must explicitly list those grant fields (`product_id`, `purchase_token`, `package_name`, `quantity`, `google_play`, `transaction_id`, `amount`) without recording the raw full token.
- Record a traceable successful order label as either the Google Play `GPA.` order id or the app-safe purchase-token suffix label; do not paste the full purchase token into launch evidence.
- Record a traceable server grant log/request reference in `BILLING_SUCCESS_SERVER_GRANT_LOG`; do not use generic `ok` / `pass` text.
- A successful grant increases the server balance by the purchased product's exact credit amount (100 / 300 / 1000) and refreshes the profile balance in the app; record the same purchase's before/after values in `BILLING_SUCCESS_BALANCE_BEFORE` and `BILLING_SUCCESS_BALANCE_AFTER`.
- The purchase is consumed after a successful grant; `BILLING_CONSUMED_EVIDENCE` must mention both consumption and the successful grant/balance refresh.
- A paid but unconsumed order can be restored after leaving and re-entering the purchase page; `BILLING_RESTORE_EVIDENCE` must mention restore/recovery and the unconsumed/interrupted purchase.
- User-cancel is tested through Google Play and `BILLING_CANCEL_EVIDENCE` records the cancel handling/copy.
- Backend evidence must show server-side Google Play purchase-token validation before credit grant, package/app id matching, product id matching, purchased/paid state validation, and duplicate/replay handling for the same purchase/order so credits are not granted twice.
- Product-unavailable, grant-failure, and consume-failure states show stable release copy without raw Google Play or backend exception text; `BILLING_FAILURE_COPY_EVIDENCE` must mention all three failure families plus release-safe/raw-hidden copy.

## Device QA Gate

- Record concrete device OS evidence: `DEVICE_ANDROID12_PLUS` must mention Android 12+ or API 31+, `DEVICE_ANDROID14_PLUS` must mention Android 14+ or API 34+, and `DEVICE_QA_MATRIX_EVIDENCE` must mention Android 12/API 31 plus Android 14/API 34 pass/fail notes for camera, gallery, GolfCam export, Billing, login continuation, and Plaza cloud actions.
- Each `QA_*_EVIDENCE` note must mention the scenario-specific action being tested; generic `ok` / `pass` text is not enough.
- Fresh install can log in by SMS/password and returns to the original gated destination; `QA_LOGIN_CONTINUATION_EVIDENCE` must mention fresh install, SMS login, password login, and the original gated destination.
- Camera permission denial and permanent denial are recoverable from swing recording and video-Memo capture; `QA_CAMERA_PERMISSION_EVIDENCE` must mention denial, permanent-denial/settings recovery, swing recording, and video-Memo camera capture.
- Swing recording saves a local clip and opens replay analysis; `QA_SWING_RECORDING_EVIDENCE` must mention local clip save and replay analysis.
- Home `回看分析` opens the current two-source sheet, and QA covers both iOS-aligned paths: realtime recording plus album import with trim confirmation. Existing clips remain in the separate `挥杆记录` archive and must not reappear as a third source-sheet choice.
- GolfCam imports a video, completes the overlay workflow, previews the result, and exports to the gallery; `QA_GOLFCAM_EXPORT_EVIDENCE` must mention import, workflow completion, preview, export/save, and gallery/MediaStore.
- Video Memo creation supports local, camera, and online sources on at least one Android 12+ and one Android 14+ device; `QA_VIDEO_MEMO_SOURCES_EVIDENCE` must mention all three source paths.
- Plaza feed loads, opens a player, saves/favorites/comments when logged in, and asks for login before account-scoped mutations; `QA_PLAZA_CLOUD_EVIDENCE` must mention feed load, player open, logged-in save/favorite/comment actions, and logged-out login gating.
- Low-credit online download opens the credit purchase path, then returns to continue after successful credit grant; `QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE` must mention online download, purchase path, successful credit grant/balance refresh, and returning to continue.
- Profile avatar update, QR image save, feedback submission, and account-security flows keep actions above keyboard/system navigation; `QA_PROFILE_KEYBOARD_EVIDENCE` must mention avatar update, QR image save, feedback submission, account security, keyboard/input, and navigation-bar reachability.

## Evidence To Capture

- `scripts/android_release_preflight.sh` output for the exact release candidate, including source branch/commit and APK/AAB SHA-256 digests.
- `scripts/android_init_launch_evidence.sh` draft filled from that exact preflight log, with remaining Play/Billing/device fields completed from real external evidence.
- Play Console internal-track upload result screenshot or release id.
- Test purchase order id or token suffix, server grant log id, and resulting balance.
- Restore-flow evidence for an interrupted purchase.
- `DEVICE_QA_MATRIX_EVIDENCE` device/OS matrix with pass/fail notes for camera, gallery, GolfCam export, Billing, login continuation, and Plaza cloud actions.
