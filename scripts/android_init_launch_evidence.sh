#!/usr/bin/env bash
set -eo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMPLATE="$ROOT_DIR/docs/android-launch-evidence.template.env"
MIN_PLAY_TARGET_SDK=36

usage() {
  cat <<'USAGE'
Usage: scripts/android_init_launch_evidence.sh --preflight-log=path --output=path [--force]

Creates an Android launch evidence draft from a saved release preflight log.
Build-artifact fields are copied from the log; Play Console, Billing, backend,
and device-QA fields stay blank until real external evidence is attached.
USAGE
}

preflight_log=""
output_file=""
force=false

for arg in "$@"; do
  case "$arg" in
    --preflight-log=*)
      preflight_log="${arg#--preflight-log=}"
      ;;
    --output=*)
      output_file="${arg#--output=}"
      ;;
    --force)
      force=true
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

if [[ -z "$preflight_log" || -z "$output_file" ]]; then
  usage >&2
  exit 2
fi
if [[ ! -s "$preflight_log" ]]; then
  echo "Preflight log not found or empty: $preflight_log" >&2
  exit 1
fi
if [[ ! -s "$TEMPLATE" ]]; then
  echo "Launch evidence template not found: $TEMPLATE" >&2
  exit 1
fi
if [[ -e "$output_file" && "$force" != true ]]; then
  echo "Output already exists: $output_file (use --force to overwrite)" >&2
  exit 1
fi

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "$value"
}

log_field_value() {
  local line="$1"
  local field="$2"
  printf '%s\n' "$line" | sed -n "s/.*$field=\\([^, ]*\\).*/\\1/p"
}

sha_line_path() {
  printf '%s\n' "$1" | sed -n 's/^SHA-256 //; s/ [0-9a-fA-F]\{64\}$//p'
}

sha_line_digest() {
  printf '%s\n' "$1" | awk '{print $NF}'
}

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

resolve_log_artifact_path() {
  local path="$1"
  if [[ "$path" = /* ]]; then
    printf '%s' "$path"
  else
    printf '%s/%s' "$ROOT_DIR" "$path"
  fi
}

validate_logged_artifact() {
  local label="$1"
  local line="$2"
  local path
  local digest
  local resolved
  local actual
  local jar_signer
  path="$(sha_line_path "$line")"
  digest="$(sha_line_digest "$line")"
  resolved="$(resolve_log_artifact_path "$path")"
  if [[ ! -s "$resolved" ]]; then
    errors+=("$label artifact from preflight log must still exist and be non-empty: $path")
    return
  fi
  actual="$(sha256_file "$resolved")"
  if [[ "$(printf '%s' "$actual" | tr '[:upper:]' '[:lower:]')" != "$(printf '%s' "$digest" | tr '[:upper:]' '[:lower:]')" ]]; then
    errors+=("$label artifact SHA-256 does not match preflight log; expected $digest, got $actual")
  fi
  if [[ "$label" == "AAB" ]] && ! aab_has_signature_files "$resolved"; then
    errors+=("AAB artifact from preflight log must contain META-INF signature files: $path")
  elif [[ "$label" == "AAB" ]]; then
    jar_signer="$(find_jarsigner)"
    if [[ -z "$jar_signer" ]]; then
      errors+=("AAB artifact signature could not be verified because jarsigner was not found: $path")
    elif ! aab_jarsigner_verified "$jar_signer" "$resolved"; then
      errors+=("AAB artifact jarsigner verification failed: $path")
    fi
  fi
}

env_escape() {
  local value="$1"
  if [[ -z "$value" ]]; then
    printf ''
  elif [[ "$value" =~ ^[A-Za-z0-9_./:@%+=,-]+$ ]]; then
    printf '%s' "$value"
  else
    value="${value//\\/\\\\}"
    value="${value//\"/\\\"}"
    printf '"%s"' "$value"
  fi
}

source_line="$(grep -E '^Source state:' "$preflight_log" | tail -1 || true)"
metadata_line="$(grep -E '^Release metadata:' "$preflight_log" | tail -1 || true)"
sdk_line="$(grep -E '^Release SDK:' "$preflight_log" | tail -1 || true)"
apk_sha_line="$(grep -E '^SHA-256 .*\.apk [0-9a-fA-F]{64}$' "$preflight_log" | tail -1 || true)"
aab_sha_line="$(grep -E '^SHA-256 .*\.aab [0-9a-fA-F]{64}$' "$preflight_log" | tail -1 || true)"
lint_summary_line="$(grep -E '^Release lint summary: errors=[0-9]+, warnings=[0-9]+$' "$preflight_log" | tail -1 || true)"
aab_signer_line="$(grep -E '^Release AAB signer SHA-256: ([0-9A-Fa-f]{2}:){31}[0-9A-Fa-f]{2}$' "$preflight_log" | tail -1 || true)"

errors=()
[[ -z "$source_line" ]] && errors+=("Preflight log is missing Source state line")
[[ -z "$metadata_line" ]] && errors+=("Preflight log is missing Release metadata line")
[[ -z "$sdk_line" ]] && errors+=("Preflight log is missing Release SDK line")
[[ -z "$apk_sha_line" ]] && errors+=("Preflight log is missing APK SHA-256 line")
[[ -z "$aab_sha_line" ]] && errors+=("Preflight log is missing AAB SHA-256 line")
[[ -z "$lint_summary_line" ]] && errors+=("Preflight log is missing Release lint summary line")
[[ -z "$aab_signer_line" ]] && errors+=("Preflight log is missing Release AAB signer SHA-256 line")

if [[ -n "$source_line" ]]; then
  dirty_files="$(log_field_value "$source_line" dirtyFiles)"
  if [[ "$dirty_files" != "0" ]]; then
    errors+=("Preflight log must come from a clean release candidate; expected dirtyFiles=0, got '${dirty_files:-missing}'")
  fi
fi

if [[ -n "$sdk_line" ]]; then
  target_sdk="$(log_field_value "$sdk_line" targetSdk)"
  if [[ ! "$target_sdk" =~ ^[0-9]+$ ]]; then
    errors+=("Preflight log targetSdk must be numeric; got '${target_sdk:-missing}'")
  elif (( 10#$target_sdk < MIN_PLAY_TARGET_SDK )); then
    errors+=("Preflight log targetSdk must be >= $MIN_PLAY_TARGET_SDK for current Play upload readiness; got '$target_sdk'")
  fi
fi

if [[ -n "$lint_summary_line" ]]; then
  lint_error_count="$(log_field_value "$lint_summary_line" errors)"
  if [[ "$lint_error_count" != "0" ]]; then
    errors+=("Preflight log must have zero release lint errors; got '${lint_error_count:-missing}'")
  fi
fi

for required_marker in \
  "Release APK signature verified by apksigner." \
  "Release AAB signature verified by jarsigner." \
  "Signed artifact check passed." \
  "Merged release manifest check passed." \
  "Release artifact debug-resource check passed." \
  "Release lint warning allowlist check passed." \
  "Release preflight passed."; do
  if ! grep -qF "$required_marker" "$preflight_log"; then
    case "$required_marker" in
      "Signed artifact check passed.")
        errors+=("Preflight log is missing signed artifact verification; run scripts/android_release_preflight.sh --require-clean-worktree --require-signed")
        ;;
      *)
        errors+=("Preflight log is missing required success marker: $required_marker")
        ;;
    esac
  fi
done

[[ -n "$apk_sha_line" ]] && validate_logged_artifact "APK" "$apk_sha_line"
[[ -n "$aab_sha_line" ]] && validate_logged_artifact "AAB" "$aab_sha_line"
if [[ -n "$aab_sha_line" && -n "$aab_signer_line" ]]; then
  logged_aab_signer="${aab_signer_line#Release AAB signer SHA-256: }"
  actual_aab_signer="$(aab_signer_sha256 "$(resolve_log_artifact_path "$(sha_line_path "$aab_sha_line")")" || true)"
  if [[ -z "$actual_aab_signer" ]]; then
    errors+=("Could not read AAB signer SHA-256 from artifact")
  elif [[ "$actual_aab_signer" != "$(printf '%s' "$logged_aab_signer" | tr '[:lower:]' '[:upper:]')" ]]; then
    errors+=("AAB signer SHA-256 does not match preflight log; expected $logged_aab_signer, got $actual_aab_signer")
  fi
fi

if ((${#errors[@]} > 0)); then
  printf 'Could not initialize launch evidence:\n' >&2
  printf ' - %s\n' "${errors[@]}" >&2
  exit 1
fi

generated_value() {
  local key="$1"
  case "$key" in
    RELEASE_BRANCH) log_field_value "$source_line" branch ;;
    RELEASE_COMMIT) log_field_value "$source_line" commit ;;
    RELEASE_DIRTY_FILES) log_field_value "$source_line" dirtyFiles ;;
    RELEASE_APPLICATION_ID) log_field_value "$metadata_line" applicationId ;;
    RELEASE_VERSION_CODE) log_field_value "$metadata_line" versionCode ;;
    RELEASE_VERSION_NAME) log_field_value "$metadata_line" versionName ;;
    RELEASE_TARGET_SDK) log_field_value "$sdk_line" targetSdk ;;
    RELEASE_APK_PATH) sha_line_path "$apk_sha_line" ;;
    RELEASE_APK_SHA256) sha_line_digest "$apk_sha_line" ;;
    RELEASE_AAB_PATH) sha_line_path "$aab_sha_line" ;;
    RELEASE_AAB_SHA256) sha_line_digest "$aab_sha_line" ;;
    RELEASE_AAB_SIGNER_SHA256) printf '%s' "${aab_signer_line#Release AAB signer SHA-256: }" ;;
    RELEASE_PREFLIGHT_LOG) printf '%s' "$preflight_log" ;;
    RELEASE_PREFLIGHT_SIGNED_CHECK)
      grep -qF "Signed artifact check passed." "$preflight_log" && printf 'true' || printf ''
      ;;
    RELEASE_PREFLIGHT_MANIFEST_CHECK)
      grep -qF "Merged release manifest check passed." "$preflight_log" && printf 'true' || printf ''
      ;;
    RELEASE_PREFLIGHT_DEBUG_RESOURCE_CHECK)
      grep -qF "Release artifact debug-resource check passed." "$preflight_log" && printf 'true' || printf ''
      ;;
    RELEASE_PREFLIGHT_LINT_WARNING_CHECK)
      grep -qF "Release lint warning allowlist check passed." "$preflight_log" && printf 'true' || printf ''
      ;;
    RELEASE_LINT_WARNING_COUNT) log_field_value "$lint_summary_line" warnings ;;
    *) printf '' ;;
  esac
}

output_dir="$(dirname "$output_file")"
mkdir -p "$output_dir"

{
  while IFS= read -r line || [[ -n "$line" ]]; do
    trimmed="$(trim "$line")"
    if [[ "$trimmed" =~ ^[A-Z0-9_]+= ]]; then
      key="${trimmed%%=*}"
      printf '%s=%s\n' "$key" "$(env_escape "$(generated_value "$key")")"
    else
      printf '%s\n' "$line"
    fi
  done < "$TEMPLATE"
} > "$output_file"

echo "Wrote Android launch evidence draft: $output_file"
