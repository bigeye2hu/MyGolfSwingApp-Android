#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

evidence_file=""
check_ios=true
strict=false
min_version_code=""
status_errors=()

usage() {
  cat <<'USAGE'
Usage: scripts/android_launch_status.sh [--evidence=path] [--skip-ios] [--strict] [--min-version-code=N]

Prints a lightweight Android launch status summary from the current worktree,
last built release artifacts, optional launch evidence file, and iOS alignment
audit. It does not run Gradle; use android_release_preflight.sh for the full
build/lint/artifact gate.

With --strict, exits non-zero unless the snapshot is clean enough for a signed
release-candidate handoff and the external evidence file is provided and passes.
USAGE
}

for arg in "$@"; do
  case "$arg" in
    --evidence=*)
      evidence_file="${arg#--evidence=}"
      ;;
    --skip-ios)
      check_ios=false
      ;;
    --strict)
      strict=true
      ;;
    --min-version-code=*)
      min_version_code="${arg#--min-version-code=}"
      if [[ ! "$min_version_code" =~ ^[1-9][0-9]*$ ]]; then
        echo "--min-version-code must be a positive integer, got '$min_version_code'" >&2
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

sha256_file() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    printf 'unavailable'
  fi
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

xml_value_or_unknown() {
  local file="$1"
  local xpath="$2"
  if [[ -s "$file" ]] && command -v xmllint >/dev/null 2>&1; then
    xmllint --xpath "string($xpath)" "$file" 2>/dev/null || printf 'unknown'
  else
    printf 'unknown'
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

verify_artifact_signature() {
  local artifact="$1"
  local signer
  case "$artifact" in
    *.apk)
      if [[ "$artifact" == *unsigned* ]]; then
        echo "Signature: $artifact unsigned by filename"
        status_errors+=("Strict launch readiness requires signed release artifacts; found unsigned artifact: $artifact")
        return
      fi
      signer="$(find_apksigner)"
      if [[ -z "$signer" ]]; then
        echo "Signature: $artifact not checked; apksigner not found"
        status_errors+=("APK signature could not be verified because apksigner was not found: $artifact")
      elif "$signer" verify --verbose "$artifact" >/dev/null 2>&1; then
        echo "Signature: $artifact verified by apksigner"
      else
        echo "Signature: $artifact verification failed"
        status_errors+=("APK signature verification failed: $artifact")
      fi
      ;;
    *.aab)
      if ! aab_has_signature_files "$artifact"; then
        echo "Signature: $artifact unsigned (missing META-INF signature files)"
        status_errors+=("Strict launch readiness requires signed release artifacts; found unsigned AAB: $artifact")
        return
      fi
      signer="$(find_jarsigner)"
      if [[ -z "$signer" ]]; then
        echo "Signature: $artifact not checked; jarsigner not found"
        status_errors+=("AAB signature could not be verified because jarsigner was not found: $artifact")
      elif aab_jarsigner_verified "$signer" "$artifact"; then
        echo "Signature: $artifact verified by jarsigner"
      else
        echo "Signature: $artifact verification failed"
        status_errors+=("AAB signature verification failed: $artifact")
      fi
      ;;
  esac
}

echo "== Android launch status =="

if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || printf 'unknown')"
  commit="$(git rev-parse --short HEAD 2>/dev/null || printf 'unknown')"
  dirty_count="$(git status --porcelain=v1 --untracked-files=all | wc -l | tr -d ' ')"
  echo "Source state: branch=$branch, commit=$commit, dirtyFiles=$dirty_count"
  if [[ "$dirty_count" == "0" ]]; then
    echo "Source gate: clean"
  else
    echo "Source gate: development; signed release-candidate handoff requires dirtyFiles=0"
    status_errors+=("Source worktree must be clean for strict launch readiness; dirtyFiles=$dirty_count")
  fi
else
  echo "Source state: unavailable (not a git worktree)"
  echo "Source gate: unknown"
  status_errors+=("Source state is unavailable")
fi

apk_metadata="app/build/outputs/apk/release/output-metadata.json"
merged_manifest="app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml"
apk_output=""
if [[ -s "$apk_metadata" ]]; then
  apk_output="$(awk -F'"' '/"outputFile"/ { print $4; exit }' "$apk_metadata")"
  application_id="$(awk -F'"' '/"applicationId"/ { print $4; exit }' "$apk_metadata")"
  version_code="$(awk -F': ' '/"versionCode"/ { gsub(/,/, "", $2); print $2; exit }' "$apk_metadata")"
  version_name="$(awk -F'"' '/"versionName"/ { print $4; exit }' "$apk_metadata")"
else
  application_id="unknown"
  version_code="unknown"
  version_name="unknown"
fi
target_sdk="$(xml_value_or_unknown "$merged_manifest" "/*[local-name()='manifest']/*[local-name()='uses-sdk']/@*[namespace-uri()='http://schemas.android.com/apk/res/android' and local-name()='targetSdkVersion']")"
echo "Release metadata: applicationId=$application_id, versionCode=$version_code, versionName=$version_name, targetSdk=$target_sdk"
if [[ "$application_id" != "com.mygolfswing.app" ]]; then
  status_errors+=("Release applicationId must be com.mygolfswing.app; got $application_id")
fi
if [[ ! "$version_code" =~ ^[1-9][0-9]*$ ]]; then
  status_errors+=("Release versionCode must be a positive integer; got $version_code")
elif [[ -n "$min_version_code" ]] && (( 10#$version_code < 10#$min_version_code )); then
  status_errors+=("Release versionCode must be >= $min_version_code for this handoff; got $version_code")
fi
if [[ ! "$target_sdk" =~ ^[0-9]+$ ]]; then
  status_errors+=("Release targetSdk must be numeric; got $target_sdk")
elif (( 10#$target_sdk < 36 )); then
  status_errors+=("Release targetSdk must be >= 36; got $target_sdk")
fi

apk_path=""
if [[ -n "$apk_output" ]]; then
  apk_path="app/build/outputs/apk/release/$apk_output"
fi
aab_path="app/build/outputs/bundle/release/app-release.aab"
for artifact in "$apk_path" "$aab_path"; do
  [[ -z "$artifact" ]] && continue
  if [[ -s "$artifact" ]]; then
    bytes="$(wc -c < "$artifact" | tr -d ' ')"
    digest="$(sha256_file "$artifact")"
    echo "Artifact: $artifact bytes=$bytes sha256=$digest"
    if [[ ! "$digest" =~ ^[0-9a-fA-F]{64}$ ]]; then
      status_errors+=("Artifact SHA-256 unavailable for $artifact")
    fi
    verify_artifact_signature "$artifact"
  else
    echo "Artifact missing: $artifact"
    status_errors+=("Release artifact missing or empty: $artifact")
  fi
done

if [[ "$check_ios" == true ]]; then
  if [[ -x "scripts/android_check_ios_alignment.sh" ]]; then
    if ios_output="$(scripts/android_check_ios_alignment.sh 2>&1)"; then
      echo "iOS alignment: pass; $ios_output"
    else
      echo "iOS alignment: attention needed"
      printf '%s\n' "$ios_output" | sed 's/^/  /'
      status_errors+=("iOS alignment audit must pass")
    fi
  else
    echo "iOS alignment: checker missing"
    status_errors+=("iOS alignment checker is missing")
  fi
else
  echo "iOS alignment: skipped"
  status_errors+=("iOS alignment audit cannot be skipped for strict launch readiness")
fi

if [[ -n "$evidence_file" ]]; then
  if [[ -x "scripts/android_check_launch_evidence.sh" ]]; then
    if evidence_output="$(scripts/android_check_launch_evidence.sh "$evidence_file" 2>&1)"; then
      echo "External evidence: pass; $evidence_output"
    else
      echo "External evidence: attention needed"
      printf '%s\n' "$evidence_output" | sed 's/^/  /'
      status_errors+=("External launch evidence must pass")
    fi
  else
    echo "External evidence: checker missing"
    status_errors+=("External launch evidence checker is missing")
  fi
else
  echo "External evidence: not checked; pass --evidence=path/to/android-launch-evidence.env after Play/Billing/device proof is collected"
  status_errors+=("External launch evidence file is required for strict launch readiness")
fi

echo "Next signed-candidate gate: scripts/android_release_preflight.sh --require-clean-worktree --require-signed --min-version-code=<next Play versionCode> --min-target-sdk=36"

if [[ "$strict" == true ]]; then
  if (( ${#status_errors[@]} > 0 )); then
    echo "Launch readiness: attention needed"
    printf ' - %s\n' "${status_errors[@]}"
    exit 1
  fi
  echo "Launch readiness: pass"
fi
