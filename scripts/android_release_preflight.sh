#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

usage() {
  cat <<'USAGE'
Usage: scripts/android_release_preflight.sh [options]

Options:
  --require-signed          Verify release APK/AAB signatures, fail if unsigned,
                            and require a clean git worktree.
  --require-clean-worktree  Fail if git has uncommitted or untracked changes.
  --min-version-code=N      Fail if the built versionCode is lower than N.
  --min-target-sdk=N        Fail if the built targetSdk is lower than N (default: 36).
  -h, --help                Show this help.

Environment/Gradle properties:
  MYGOLFSWING_VERSION_CODE, MYGOLFSWING_VERSION_NAME
  MYGOLFSWING_RELEASE_STORE_FILE, MYGOLFSWING_RELEASE_STORE_PASSWORD
  MYGOLFSWING_RELEASE_KEY_ALIAS, MYGOLFSWING_RELEASE_KEY_PASSWORD
USAGE
}

require_signed=false
require_clean_worktree=false
min_version_code=""
min_target_sdk="36"
for arg in "$@"; do
  case "$arg" in
    --require-signed)
      require_signed=true
      ;;
    --require-clean-worktree)
      require_clean_worktree=true
      ;;
    --min-version-code=*)
      min_version_code="${arg#--min-version-code=}"
      if [[ ! "$min_version_code" =~ ^[1-9][0-9]*$ ]]; then
        echo "--min-version-code must be a positive integer, got '$min_version_code'" >&2
        exit 2
      fi
      ;;
    --min-target-sdk=*)
      min_target_sdk="${arg#--min-target-sdk=}"
      if [[ ! "$min_target_sdk" =~ ^[1-9][0-9]*$ ]]; then
        echo "--min-target-sdk must be a positive integer, got '$min_target_sdk'" >&2
        exit 2
      fi
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $arg" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ "$require_signed" == true ]]; then
  require_clean_worktree=true
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
  bundled_jbr="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  if [[ -x "$bundled_jbr/bin/java" ]]; then
    export JAVA_HOME="$bundled_jbr"
  fi
fi

sha256_file() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    echo "No SHA-256 tool found; install shasum or sha256sum" >&2
    exit 1
  fi
}

lint_warning_id_allowed() {
  case "$1" in
    AndroidGradlePluginVersion|GradleDependency|NewerVersionAvailable|OldTargetApi) return 0 ;;
    *) return 1 ;;
  esac
}

find_apksigner() {
  if command -v apksigner >/dev/null 2>&1; then
    command -v apksigner
    return
  fi
  local sdk
  for sdk in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Library/Android/sdk"; do
    if [[ -n "$sdk" && -d "$sdk/build-tools" ]]; then
      find "$sdk/build-tools" -maxdepth 2 -type f -name apksigner 2>/dev/null | sort | tail -1
      return
    fi
  done
}

find_jarsigner() {
  local candidate
  for candidate in \
    "${JAVA_HOME:-}/bin/jarsigner" \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/jarsigner"; do
    if [[ -x "$candidate" ]]; then
      printf '%s' "$candidate"
      return
    fi
  done
  if command -v jarsigner >/dev/null 2>&1; then
    command -v jarsigner
  fi
}

find_keytool() {
  local candidate
  for candidate in \
    "${JAVA_HOME:-}/bin/keytool" \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"; do
    if [[ -x "$candidate" ]]; then
      printf '%s' "$candidate"
      return
    fi
  done
  if command -v keytool >/dev/null 2>&1; then
    command -v keytool
  fi
}

aab_has_signature_files() {
  local artifact="$1"
  local entries
  entries="$(unzip -Z1 "$artifact" 2>/dev/null || true)"
  printf '%s\n' "$entries" | grep -Eq '^META-INF/[^/]+\.SF$' &&
    printf '%s\n' "$entries" | grep -Eq '^META-INF/[^/]+\.(RSA|DSA|EC)$'
}

aab_jarsigner_verified() {
  local signer="$1"
  local artifact="$2"
  local output
  output="$("$signer" -verify -verbose "$artifact" 2>&1)" || return 1
  if printf '%s\n' "$output" | grep -Eiq '(^|[^[:alpha:]])unsigned([^[:alpha:]]|$)|未签名'; then
    return 1
  fi
  printf '%s\n' "$output" | grep -Eq '^[[:space:]]*s[[:alpha:]]*[[:space:]]+[0-9]+[[:space:]]'
}

aab_signer_sha256() {
  local artifact="$1"
  local tool
  local digest
  tool="$(find_keytool)"
  [[ -z "$tool" ]] && return 1
  digest="$("$tool" -printcert -jarfile "$artifact" 2>/dev/null | sed -n 's/.*SHA256:[[:space:]]*//p' | head -1 | tr -d '[:space:]')"
  [[ "$digest" =~ ^([0-9A-Fa-f]{2}:){31}[0-9A-Fa-f]{2}$ ]] || return 1
  printf '%s' "$digest" | tr '[:lower:]' '[:upper:]'
}

xml_value() {
  local file="$1"
  local xpath="$2"
  if ! command -v xmllint >/dev/null 2>&1; then
    echo "No xmllint found; install libxml2 tools to verify the merged release manifest" >&2
    exit 1
  fi
  xmllint --xpath "string($xpath)" "$file"
}

xml_count() {
  local file="$1"
  local xpath="$2"
  if ! command -v xmllint >/dev/null 2>&1; then
    echo "No xmllint found; install libxml2 tools to verify the merged release manifest" >&2
    exit 1
  fi
  xmllint --xpath "count($xpath)" "$file"
}

require_xml_value() {
  local file="$1"
  local xpath="$2"
  local expected="$3"
  local label="$4"
  local actual
  actual="$(xml_value "$file" "$xpath")"
  if [[ "$actual" != "$expected" ]]; then
    echo "Unexpected merged manifest $label: expected '$expected', got '${actual:-<empty>}'" >&2
    exit 1
  fi
}

require_xml_count() {
  local file="$1"
  local xpath="$2"
  local expected="$3"
  local label="$4"
  local actual
  actual="$(xml_count "$file" "$xpath")"
  if [[ "$actual" != "$expected" ]]; then
    echo "Unexpected merged manifest $label count: expected '$expected', got '${actual:-<empty>}'" >&2
    exit 1
  fi
}

echo "== Android release preflight =="
if [[ -n "${JAVA_HOME:-}" ]]; then
  echo "JAVA_HOME=$JAVA_HOME"
else
  echo "JAVA_HOME is not set; relying on system Java"
fi

if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git_branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo unknown)"
  git_commit="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"
  git_dirty_count="$(git status --porcelain=v1 --untracked-files=all | wc -l | tr -d ' ')"
  tracked_signing_material="$(git ls-files | grep -E '\.(jks|keystore|p12|pfx)$' || true)"
  echo "Source state: branch=$git_branch, commit=$git_commit, dirtyFiles=$git_dirty_count"
  if [[ -n "$tracked_signing_material" ]]; then
    echo "Release signing material is tracked by git; remove these files from source control:" >&2
    echo "$tracked_signing_material" >&2
    exit 1
  fi
  if [[ "$require_clean_worktree" == true && "$git_dirty_count" != "0" ]]; then
    echo "Git worktree is not clean; commit/stash changes before release-candidate handoff." >&2
    exit 1
  fi
else
  echo "Source state: unavailable (not a git worktree)"
  if [[ "$require_clean_worktree" == true ]]; then
    echo "Cannot require a clean worktree outside a git worktree." >&2
    exit 1
  fi
fi

./gradlew testDebugUnitTest
./gradlew lintRelease --rerun-tasks
./gradlew assembleRelease bundleRelease

apk_metadata="app/build/outputs/apk/release/output-metadata.json"
if [[ ! -s "$apk_metadata" ]]; then
  echo "Missing APK output metadata: $apk_metadata" >&2
  exit 1
fi

apk_output="$(awk -F'"' '/"outputFile"/ { print $4; exit }' "$apk_metadata")"
if [[ -z "$apk_output" ]]; then
  echo "Could not read APK outputFile from $apk_metadata" >&2
  exit 1
fi

application_id="$(awk -F'"' '/"applicationId"/ { print $4; exit }' "$apk_metadata")"
version_code="$(awk -F': ' '/"versionCode"/ { gsub(/,/, "", $2); print $2; exit }' "$apk_metadata")"
version_name="$(awk -F'"' '/"versionName"/ { print $4; exit }' "$apk_metadata")"
echo "Release metadata: applicationId=${application_id:-unknown}, versionCode=${version_code:-unknown}, versionName=${version_name:-unknown}"

if [[ "$application_id" != "com.mygolfswing.app" ]]; then
  echo "Unexpected applicationId: ${application_id:-unknown}" >&2
  exit 1
fi

if [[ -n "$min_version_code" ]]; then
  if [[ ! "$version_code" =~ ^[0-9]+$ ]]; then
    echo "Could not validate non-numeric versionCode: ${version_code:-unknown}" >&2
    exit 1
  fi
  if (( version_code < min_version_code )); then
    echo "versionCode $version_code is lower than required minimum $min_version_code" >&2
    exit 1
  fi
fi

merged_manifest="app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml"
if [[ ! -s "$merged_manifest" ]]; then
  echo "Missing merged release manifest: $merged_manifest" >&2
  exit 1
fi

android_attr="namespace-uri()='http://schemas.android.com/apk/res/android'"
require_xml_value "$merged_manifest" "/*[local-name()='manifest']/@package" "com.mygolfswing.app" "package"
require_xml_value "$merged_manifest" "/*[local-name()='manifest']/@*[$android_attr and local-name()='versionCode']" "$version_code" "versionCode"
require_xml_value "$merged_manifest" "/*[local-name()='manifest']/@*[$android_attr and local-name()='versionName']" "$version_name" "versionName"
min_sdk="$(xml_value "$merged_manifest" "/*[local-name()='manifest']/*[local-name()='uses-sdk']/@*[$android_attr and local-name()='minSdkVersion']")"
target_sdk="$(xml_value "$merged_manifest" "/*[local-name()='manifest']/*[local-name()='uses-sdk']/@*[$android_attr and local-name()='targetSdkVersion']")"
echo "Release SDK: minSdk=${min_sdk:-unknown}, targetSdk=${target_sdk:-unknown}"
if [[ ! "$target_sdk" =~ ^[0-9]+$ ]]; then
  echo "Could not validate non-numeric targetSdk: ${target_sdk:-unknown}" >&2
  exit 1
fi
if (( 10#$target_sdk < 10#$min_target_sdk )); then
  echo "targetSdk $target_sdk is lower than required minimum $min_target_sdk" >&2
  exit 1
fi
require_xml_value "$merged_manifest" "/*[local-name()='manifest']/*[local-name()='application']/@*[$android_attr and local-name()='usesCleartextTraffic']" "false" "usesCleartextTraffic"
require_xml_value "$merged_manifest" "/*[local-name()='manifest']/*[local-name()='application']/@*[$android_attr and local-name()='allowBackup']" "false" "allowBackup"
require_xml_count "$merged_manifest" "/*[local-name()='manifest']/*[local-name()='application'][@*[$android_attr and local-name()='debuggable']='true']" "0" "debuggable release application"
require_xml_count "$merged_manifest" "/*[local-name()='manifest'][@*[$android_attr and local-name()='testOnly']='true'] | /*[local-name()='manifest']/*[local-name()='application'][@*[$android_attr and local-name()='testOnly']='true']" "0" "testOnly release manifest"
require_xml_count "$merged_manifest" "//*[local-name()='uses-permission'][@*[$android_attr and local-name()='name']='android.permission.READ_EXTERNAL_STORAGE'] | //*[local-name()='uses-permission'][@*[$android_attr and local-name()='name']='android.permission.WRITE_EXTERNAL_STORAGE'] | //*[local-name()='uses-permission'][@*[$android_attr and local-name()='name']='android.permission.MANAGE_EXTERNAL_STORAGE']" "0" "legacy broad storage permission"
require_xml_count "$merged_manifest" "//*[local-name()='uses-permission'][@*[$android_attr and local-name()='name']='com.android.vending.BILLING']" "1" "Google Play Billing permission"
require_xml_count "$merged_manifest" "//*[local-name()='activity'][@*[$android_attr and local-name()='name']='com.mygolfswing.app.MainActivity' and @*[$android_attr and local-name()='exported']='true']" "1" "exported MainActivity"
require_xml_count "$merged_manifest" "//*[local-name()='receiver'][@*[$android_attr and local-name()='name']='androidx.profileinstaller.ProfileInstallReceiver' and @*[$android_attr and local-name()='exported']='true' and @*[$android_attr and local-name()='permission']='android.permission.DUMP']" "1" "DUMP-protected ProfileInstallReceiver"
require_xml_count "$merged_manifest" "//*[local-name()='service'][@*[$android_attr and local-name()='name']='androidx.work.impl.background.systemjob.SystemJobService' and @*[$android_attr and local-name()='exported']='true' and @*[$android_attr and local-name()='permission']='android.permission.BIND_JOB_SERVICE']" "1" "system-only WorkManager JobService"
require_xml_count "$merged_manifest" "//*[local-name()='receiver'][@*[$android_attr and local-name()='name']='androidx.work.impl.diagnostics.DiagnosticsReceiver']" "0" "removed WorkManager diagnostics receiver"
require_xml_count "$merged_manifest" "//*[local-name()='activity' or local-name()='activity-alias' or local-name()='service' or local-name()='receiver' or local-name()='provider'][@*[$android_attr and local-name()='exported']='true' and not(local-name()='activity' and @*[$android_attr and local-name()='name']='com.mygolfswing.app.MainActivity') and not(local-name()='receiver' and @*[$android_attr and local-name()='name']='androidx.profileinstaller.ProfileInstallReceiver' and @*[$android_attr and local-name()='permission']='android.permission.DUMP') and not(local-name()='service' and @*[$android_attr and local-name()='name']='androidx.work.impl.background.systemjob.SystemJobService' and @*[$android_attr and local-name()='permission']='android.permission.BIND_JOB_SERVICE')]" "0" "unexpected exported component"
require_xml_count "$merged_manifest" "//*[local-name()='provider'][@*[$android_attr and local-name()='name']='androidx.core.content.FileProvider' and @*[$android_attr and local-name()='authorities']='com.mygolfswing.app.fileprovider' and @*[$android_attr and local-name()='exported']='false' and @*[$android_attr and local-name()='grantUriPermissions']='true']" "1" "private FileProvider"
echo "Merged release manifest check passed."

apk="app/build/outputs/apk/release/$apk_output"
aab="app/build/outputs/bundle/release/app-release.aab"
for artifact in "$apk" "$aab"; do
  if [[ ! -s "$artifact" ]]; then
    echo "Missing or empty release artifact: $artifact" >&2
    exit 1
  fi
  bytes="$(wc -c < "$artifact" | tr -d ' ')"
  echo "OK $artifact ($bytes bytes)"
  echo "SHA-256 $artifact $(sha256_file "$artifact")"
done

for artifact in "$apk" "$aab"; do
  if unzip -l "$artifact" | awk '{print $4}' | grep -Eq '(^|/)DebugProbesKt\.bin$'; then
    echo "Release artifact contains DebugProbesKt.bin: $artifact" >&2
    exit 1
  fi
done
echo "Release artifact debug-resource check passed."

if [[ "$require_signed" == true ]]; then
  if [[ "$apk_output" == *unsigned* ]]; then
    echo "Release APK appears unsigned: $apk" >&2
    exit 1
  fi
  apk_signer="$(find_apksigner)"
  if [[ -z "$apk_signer" ]]; then
    echo "No apksigner found; install Android SDK build-tools to verify the release APK signature." >&2
    exit 1
  fi
  if ! "$apk_signer" verify --verbose "$apk" >/dev/null; then
    echo "Release APK signature verification failed: $apk" >&2
    exit 1
  fi
  echo "Release APK signature verified by apksigner."
  if ! aab_has_signature_files "$aab"; then
    echo "Release AAB appears unsigned: $aab" >&2
    exit 1
  fi
  jar_signer="$(find_jarsigner)"
  if [[ -z "$jar_signer" ]]; then
    echo "No jarsigner found; install a JDK to verify the release AAB signature." >&2
    exit 1
  fi
  if ! aab_jarsigner_verified "$jar_signer" "$aab"; then
    echo "Release AAB signature verification failed: $aab" >&2
    exit 1
  fi
  aab_signer_digest="$(aab_signer_sha256 "$aab" || true)"
  if [[ -z "$aab_signer_digest" ]]; then
    echo "Could not read release AAB signer SHA-256 fingerprint: $aab" >&2
    exit 1
  fi
  echo "Release AAB signature verified by jarsigner."
  echo "Release AAB signer SHA-256: $aab_signer_digest"
  echo "Signed artifact check passed."
fi

lint_txt="app/build/reports/lint-results-release.txt"
if [[ ! -f "$lint_txt" ]]; then
  echo "Missing release lint text report: $lint_txt" >&2
  exit 1
fi

unexpected_lint_warning_ids=()
while IFS= read -r issue_id; do
  [[ -z "$issue_id" ]] && continue
  if ! lint_warning_id_allowed "$issue_id"; then
    unexpected_lint_warning_ids+=("$issue_id")
  fi
done < <(sed -n 's/.*\[\([A-Za-z0-9_]*\)\]$/\1/p' "$lint_txt" | sort -u)

if ((${#unexpected_lint_warning_ids[@]} > 0)); then
  echo "Release lint contains non-allowlisted warning ids:" >&2
  printf ' - %s\n' "${unexpected_lint_warning_ids[@]}" >&2
  exit 1
fi
echo "Release lint warning allowlist check passed."

lint_summary="$(tail -1 "$lint_txt")"
if [[ ! "$lint_summary" =~ ^([0-9]+)[[:space:]]+errors?,[[:space:]]+([0-9]+)[[:space:]]+warnings?$ ]]; then
  echo "Could not parse release lint summary from $lint_txt: ${lint_summary:-<empty>}" >&2
  exit 1
fi
lint_error_count="${BASH_REMATCH[1]}"
lint_warning_count="${BASH_REMATCH[2]}"
echo "$lint_summary"
echo "Release lint summary: errors=$lint_error_count, warnings=$lint_warning_count"

echo "Release preflight passed."
