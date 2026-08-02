#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IOS_REPO="${MYGOLFSWING_IOS_REPO:-$ROOT_DIR/../MyGolfSwingApp}"
AUDIT_FILE="$ROOT_DIR/docs/android-ios-alignment-audit.md"

usage() {
  cat <<'USAGE'
Usage: scripts/android_check_ios_alignment.sh [--ios-repo=path] [--audit=path]

Checks that the Android/iOS alignment audit names the current iOS HEAD and any
dirty iOS parity files that can affect Android feature or build tracking. This is a
tracking guard, not part of the Android release preflight, because final Android
builds should not depend on a local iOS checkout.
USAGE
}

for arg in "$@"; do
  case "$arg" in
    --ios-repo=*)
      IOS_REPO="${arg#--ios-repo=}"
      ;;
    --audit=*)
      AUDIT_FILE="${arg#--audit=}"
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

if [[ ! -d "$IOS_REPO/.git" ]]; then
  echo "iOS git repo not found: $IOS_REPO" >&2
  exit 1
fi
if [[ ! -s "$AUDIT_FILE" ]]; then
  echo "Android/iOS alignment audit not found: $AUDIT_FILE" >&2
  exit 1
fi

audit_text="$(cat "$AUDIT_FILE")"
ios_head="$(git -C "$IOS_REPO" rev-parse --short=8 HEAD)"
errors=()

if [[ "$audit_text" != *"$ios_head"* ]]; then
  errors+=("Audit must mention current iOS HEAD: $ios_head")
fi

is_ios_parity_file() {
  case "$1" in
    *.swift|*.ts|*.tsx|*.md|*.json|\
    *.xcodeproj/project.pbxproj|*.xcworkspace/contents.xcworkspacedata|\
    *.xcscheme|*.xcconfig|*.entitlements|\
    Package.swift|Package.resolved|Podfile|Podfile.lock|Cartfile|Cartfile.resolved|\
    */Package.swift|*/Package.resolved|*/Podfile|*/Podfile.lock|*/Cartfile|*/Cartfile.resolved|\
    Info.plist|*/Info.plist)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

dirty_parity_files=()
while IFS= read -r file; do
  if [[ -n "$file" ]] && is_ios_parity_file "$file"; then
    dirty_parity_files+=("$file")
  fi
done < <(
  git -C "$IOS_REPO" status --porcelain --untracked-files=no |
    sed -E 's/^.. //'
)

for file in "${dirty_parity_files[@]}"; do
  basename="${file##*/}"
  if [[ "$audit_text" != *"$file"* && "$audit_text" != *"$basename"* ]]; then
    errors+=("Audit must mention dirty iOS parity file: $file")
  fi
done

if (( ${#errors[@]} > 0 )); then
  printf 'Android/iOS alignment audit is stale:\n' >&2
  printf -- '- %s\n' "${errors[@]}" >&2
  exit 1
fi

printf 'Android/iOS alignment audit is current: iosHead=%s, dirtyParityFiles=%s\n' \
  "$ios_head" "${#dirty_parity_files[@]}"
