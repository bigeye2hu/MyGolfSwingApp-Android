#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IOS_REPO="${MYGOLFSWING_IOS_REPO:-$ROOT_DIR/../MyGolfSwingApp}"
AUDIT_FILE="$ROOT_DIR/docs/android-ios-alignment-audit.md"
DIRTY_HASH_FILE=""
ASSET_MAP_FILE=""

usage() {
  cat <<'USAGE'
Usage: scripts/android_check_ios_alignment.sh [--android-root=path] [--ios-repo=path] [--audit=path] [--dirty-hashes=path] [--asset-map=path]

Checks that the Android/iOS alignment audit names the current iOS HEAD and any
dirty iOS parity files that can affect Android feature or build tracking. Dirty
file content hashes are pinned as well, so further edits to an already-known file
cannot pass only because its filename was previously audited. When the
iOS project declares a marketing version, it also verifies that Android's default
display version matches it. The current literal coach-workspace actions used by
iOS must also have Android counterparts; explicitly retired iOS compatibility
actions are excluded. PageBack artwork and the key visual assets listed in the
asset map are compared byte-for-byte, and imported Android resources must also be
referenced by production Kotlin/Java/XML so copied-but-unused imagery cannot count
as parity. This is a tracking guard, not part of the Android release preflight,
because final Android builds should not depend on a local iOS checkout.
USAGE
}

for arg in "$@"; do
  case "$arg" in
    --android-root=*)
      ROOT_DIR="${arg#--android-root=}"
      ;;
    --ios-repo=*)
      IOS_REPO="${arg#--ios-repo=}"
      ;;
    --audit=*)
      AUDIT_FILE="${arg#--audit=}"
      ;;
    --dirty-hashes=*)
      DIRTY_HASH_FILE="${arg#--dirty-hashes=}"
      ;;
    --asset-map=*)
      ASSET_MAP_FILE="${arg#--asset-map=}"
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

if [[ -z "$DIRTY_HASH_FILE" ]]; then
  DIRTY_HASH_FILE="$(dirname "$AUDIT_FILE")/ios-parity-dirty-files.sha256"
fi
if [[ -z "$ASSET_MAP_FILE" ]]; then
  ASSET_MAP_FILE="$(dirname "$AUDIT_FILE")/ios-parity-assets.tsv"
fi

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
ios_project="$IOS_REPO/MyGolfSwingApp.xcodeproj/project.pbxproj"
android_gradle="$ROOT_DIR/app/build.gradle.kts"
ios_coach_service="$IOS_REPO/MyGolfSwingApp/V2/Coach/Controllers/CoachWorkspaceService.swift"
android_coach_repository="$ROOT_DIR/app/src/main/java/com/mygolfswing/app/data/repo/V2CoachWorkspaceRepository.kt"
ios_marketing_version=""
android_version_name=""
ios_current_coach_action_count="unavailable"
ios_page_artwork_count=0
ios_visual_asset_count=0
ios_referenced_visual_asset_count=0
android_runtime_visual_asset_count=0

android_resource_is_referenced() {
  local resource_path="$1"
  local resource_file resource_name resource_type candidate
  resource_file="${resource_path##*/}"
  resource_name="${resource_file%.*}"
  case "$resource_path" in
    */raw/*) resource_type="raw" ;;
    */mipmap*/*) resource_type="mipmap" ;;
    *) resource_type="drawable" ;;
  esac
  while IFS= read -r candidate; do
    if grep -Eq "R\\.${resource_type}\\.${resource_name}([^[:alnum:]_]|$)|@${resource_type}/${resource_name}([^[:alnum:]_]|$)" "$candidate"; then
      return 0
    fi
  done < <(
    find "$ROOT_DIR/app/src/main/java" "$ROOT_DIR/app/src/main/res" \
      -type f \( -name '*.kt' -o -name '*.java' -o -name '*.xml' \) -print 2>/dev/null | sort
  )
  return 1
}

if [[ "$audit_text" != *"$ios_head"* ]]; then
  errors+=("Audit must mention current iOS HEAD: $ios_head")
fi

if [[ -s "$ios_project" ]]; then
  # The project contains helper/test target versions as well as the app target.
  # The current app marketing version is the highest semantic version declared.
  ios_marketing_version="$(
    sed -nE 's/^[[:space:]]*MARKETING_VERSION = ([^;]+);/\1/p' "$ios_project" |
      sort -V | tail -n 1
  )"
fi
if [[ -s "$android_gradle" ]]; then
  android_version_name="$(
    sed -nE 's/.*releaseVersionName = releaseProperty\("MYGOLFSWING_VERSION_NAME"\) \?: "([^"]+)".*/\1/p' "$android_gradle" |
      head -n 1
  )"
fi
if [[ -n "$ios_marketing_version" ]]; then
  if [[ -z "$android_version_name" ]]; then
    errors+=("Could not determine Android default versionName from app/build.gradle.kts")
  elif [[ "$android_version_name" != "$ios_marketing_version" ]]; then
    errors+=("Android default versionName $android_version_name does not match iOS MARKETING_VERSION $ios_marketing_version")
  fi
  if [[ "$audit_text" != *"$ios_marketing_version"* ]]; then
    errors+=("Audit must mention current iOS MARKETING_VERSION: $ios_marketing_version")
  fi
fi

# These endpoints remain in iOS only to resume drafts made by its retired
# pre-unified video/library upload pipeline. Android began on the unified
# prepareAssetUpload -> completeAssetUpload path and must not add dead traffic
# merely to make the literal action sets identical.
is_retired_ios_coach_action() {
  case "$1" in
    commitVideoUpload|createCoachPost|prepareVideoUpload|recoverPendingLibraryUploads)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

extract_ios_coach_actions() {
  perl -0ne 'while (/\b(?:call|callIdempotentWrite)\(\s*action:\s*"([A-Za-z0-9_]+)"/g) { print "$1\n" }' "$1" |
    sort -u
}

extract_android_coach_actions() {
  perl -0ne 'while (/\bcall\(\s*"([A-Za-z0-9_]+)"/g) { print "$1\n" }' "$1" |
    sort -u
}

if [[ -s "$ios_coach_service" && -s "$android_coach_repository" ]]; then
  ios_coach_actions=()
  while IFS= read -r action; do
    [[ -n "$action" ]] && ios_coach_actions+=("$action")
  done < <(extract_ios_coach_actions "$ios_coach_service")

  android_coach_actions="$(extract_android_coach_actions "$android_coach_repository")"
  ios_current_coach_action_count=0
  for action in "${ios_coach_actions[@]}"; do
    if is_retired_ios_coach_action "$action"; then
      continue
    fi
    ios_current_coach_action_count=$((ios_current_coach_action_count + 1))
    if ! grep -Fqx "$action" <<<"$android_coach_actions"; then
      errors+=("Android coach workspace repository is missing current iOS action: $action")
    fi
  done
fi

ios_assets_root="$IOS_REPO/MyGolfSwingApp/Assets.xcassets"
android_artwork_root="$ROOT_DIR/app/src/main/res/drawable-nodpi"
if [[ -d "$ios_assets_root" ]]; then
  while IFS= read -r imageset_dir; do
    [[ -n "$imageset_dir" ]] || continue
    imageset_name="${imageset_dir##*/}"
    page_number="$(sed -nE 's/^PageBack([0-9]+)\.imageset$/\1/p' <<<"$imageset_name")"
    [[ -n "$page_number" ]] || continue
    ios_page_artwork_count=$((ios_page_artwork_count + 1))
    ios_artwork="$(
      find "$imageset_dir" -maxdepth 1 -type f \( -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.webp' \) -print |
        sort | head -n 1
    )"
    android_artwork="$android_artwork_root/page_back_${page_number}.png"
    if [[ -z "$ios_artwork" ]]; then
      errors+=("iOS $imageset_name does not contain an image file")
    elif [[ ! -s "$android_artwork" ]]; then
      errors+=("Android is missing imported iOS artwork: page_back_${page_number}.png")
    elif ! cmp -s "$ios_artwork" "$android_artwork"; then
      errors+=("Android artwork differs from iOS $imageset_name: page_back_${page_number}.png")
    elif ! android_resource_is_referenced "$android_artwork"; then
      errors+=("Android imported iOS artwork is not referenced by production UI: page_back_${page_number}.png")
    else
      android_runtime_visual_asset_count=$((android_runtime_visual_asset_count + 1))
    fi
  done < <(find "$ios_assets_root" -type d -name 'PageBack*.imageset' -print | sort)
fi

# Stable, non-sequential visual assets cannot be discovered from a filename
# convention like PageBack. Keep their explicit cross-platform paths in a small
# TSV manifest and compare the original bytes, rather than accepting a visually
# similar re-export that may drift in crop, color, or compression.
if [[ ! -s "$ASSET_MAP_FILE" ]]; then
  errors+=("iOS visual asset map not found or empty: $ASSET_MAP_FILE")
else
  while IFS=$'\t' read -r ios_relative_path android_relative_path asset_label usage_mode; do
    [[ -n "${ios_relative_path:-}" && "${ios_relative_path:0:1}" != "#" ]] || continue
    ios_visual_asset_count=$((ios_visual_asset_count + 1))
    ios_asset="$IOS_REPO/$ios_relative_path"
    android_asset="$ROOT_DIR/$android_relative_path"
    display_name="${asset_label:-$android_relative_path}"
    if [[ ! -s "$ios_asset" ]]; then
      errors+=("iOS visual asset is missing: $display_name ($ios_relative_path)")
    elif [[ ! -s "$android_asset" ]]; then
      errors+=("Android is missing imported iOS visual asset: $display_name ($android_relative_path)")
    elif ! cmp -s "$ios_asset" "$android_asset"; then
      errors+=("Android visual asset differs from iOS: $display_name ($android_relative_path)")
    elif [[ "${usage_mode:-runtime}" != "source-only" && "$android_relative_path" == app/src/main/res/* ]] && ! android_resource_is_referenced "$android_asset"; then
      errors+=("Android imported iOS visual asset is not referenced by production UI: $display_name ($android_relative_path)")
    elif [[ "${usage_mode:-runtime}" != "source-only" && "$android_relative_path" == app/src/main/res/* ]]; then
      android_runtime_visual_asset_count=$((android_runtime_visual_asset_count + 1))
    fi
  done < "$ASSET_MAP_FILE"
fi

# Catch newly referenced imagery even before somebody remembers to extend the
# explicit path map. V2 still routes into the legacy JustSwing, SwingAnalysis,
# and GolfCam source trees, so those production surfaces must participate in
# the same asset gate as files physically stored under V2. Every Swift string
# literal is resolved against the real catalog, which also catches names
# returned by models/enums instead of being passed directly to Image(...).
# Isolated CoachDesignPreview-only assets are intentionally excluded because
# that source is disabled in normal builds. PageBack assets stay under the
# convention-based check above.
ios_visual_source_roots=(
  "$IOS_REPO/MyGolfSwingApp/V2"
  "$IOS_REPO/MyGolfSwingApp/Views/JustSwing"
  "$IOS_REPO/MyGolfSwingApp/Views/SwingAnalysis"
  "$IOS_REPO/MyGolfSwingApp/Views/GolfCam"
)
existing_ios_visual_source_roots=()
for source_root in "${ios_visual_source_roots[@]}"; do
  [[ -d "$source_root" ]] && existing_ios_visual_source_roots+=("$source_root")
done
if (( ${#existing_ios_visual_source_roots[@]} > 0 )) && [[ -d "$ios_assets_root" ]]; then
  while IFS= read -r asset_name; do
    [[ -n "$asset_name" ]] || continue
    [[ "$asset_name" != CoachDemo* ]] || continue
    imageset_dir="$(find "$ios_assets_root" -type d -name "$asset_name.imageset" -print | sort | head -n 1)"
    [[ -n "$imageset_dir" ]] || continue
    ios_referenced_visual_asset_count=$((ios_referenced_visual_asset_count + 1))
    if [[ "$asset_name" == PageBack* ]]; then
      continue
    fi
    referenced_asset="$({
      find "$imageset_dir" -maxdepth 1 -type f \( -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.webp' -o -iname '*.svg' \) -print || true
    } | sort | head -n 1)"
    [[ -n "$referenced_asset" ]] || continue
    referenced_relative_path="${referenced_asset#"$IOS_REPO/"}"
    if ! awk -F $'\t' -v target="$referenced_relative_path" '
      $1 == target { found = 1 }
      END { exit(found ? 0 : 1) }
    ' "$ASSET_MAP_FILE"; then
      errors+=("iOS active visual asset is referenced but not tracked for Android: $asset_name ($referenced_relative_path)")
    fi
  done < <(
    # Intersect catalog names with source literals before the loop. Running a
    # catalog-wide find for every ordinary UI string made the live gate much
    # slower even though only a small number can ever resolve to an imageset.
    comm -12 \
      <(
        find "$ios_assets_root" -type d -name '*.imageset' -print |
          sed -E 's#^.*/([^/]+)\.imageset$#\1#' |
          sort -u
      ) \
      <(
        find "${existing_ios_visual_source_roots[@]}" -type f -name '*.swift' -print0 |
          xargs -0 perl -ne 'while (/"([^"]+)"/g) { print "$1\n" }' |
          sort -u
      )
  )
fi

is_ios_parity_file() {
  case "$1" in
    *.swift|*.ts|*.tsx|*.md|*.json|*.png|*.jpg|*.jpeg|*.svg|*.webp|\
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
  # Newly-created Swift/config files are often the most important product delta;
  # keep them in the parity gate instead of only tracking modifications to files
  # that have already been committed.
  git -C "$IOS_REPO" status --porcelain --untracked-files=all |
    sed -E 's/^.. //'
)

if (( ${#dirty_parity_files[@]} > 0 )); then
  for file in "${dirty_parity_files[@]}"; do
    basename="${file##*/}"
    if [[ "$audit_text" != *"$file"* && "$audit_text" != *"$basename"* ]]; then
      errors+=("Audit must mention dirty iOS parity file: $file")
    fi
  done
fi

# A filename-only list is insufficient for a continuously edited iOS checkout:
# once a file is named in the audit, later changes to that same file would
# otherwise pass unnoticed. Pin the exact current bytes until Android has
# reviewed and synchronized the new revision.
dirty_hash_entry_count=0
if [[ -s "$DIRTY_HASH_FILE" ]]; then
  dirty_hash_entry_count="$({ grep -Ev '^[[:space:]]*(#|$)' "$DIRTY_HASH_FILE" || true; } | wc -l | tr -d '[:space:]')"
fi
if (( dirty_hash_entry_count != ${#dirty_parity_files[@]} )); then
  errors+=("Dirty iOS hash manifest has $dirty_hash_entry_count entries but current parity worktree has ${#dirty_parity_files[@]} files")
fi

if (( ${#dirty_parity_files[@]} > 0 )); then
  for file in "${dirty_parity_files[@]}"; do
    expected_hash=""
    if [[ -s "$DIRTY_HASH_FILE" ]]; then
      while read -r candidate_hash candidate_path; do
        [[ -n "${candidate_hash:-}" && "${candidate_hash:0:1}" != "#" ]] || continue
        if [[ "$candidate_path" == "$file" ]]; then
          expected_hash="$candidate_hash"
          break
        fi
      done < "$DIRTY_HASH_FILE"
    fi
    if [[ -z "$expected_hash" ]]; then
      errors+=("Dirty iOS hash manifest is missing: $file")
      continue
    fi
    actual_hash="$(shasum -a 256 "$IOS_REPO/$file" | awk '{print $1}')"
    if [[ "$actual_hash" != "$expected_hash" ]]; then
      errors+=("Dirty iOS parity file changed after its last Android review: $file")
    fi
  done
fi

if (( ${#errors[@]} > 0 )); then
  printf 'Android/iOS alignment audit is stale:\n' >&2
  printf -- '- %s\n' "${errors[@]}" >&2
  exit 1
fi

printf 'Android/iOS alignment audit is current: iosHead=%s, marketingVersion=%s, dirtyParityFiles=%s, dirtyHashes=verified, currentCoachActions=%s, iosPageArtwork=%s, iosVisualAssets=%s, iosReferencedVisualAssets=%s, androidRuntimeVisualAssets=%s\n' \
  "$ios_head" "${ios_marketing_version:-unavailable}" "${#dirty_parity_files[@]}" "$ios_current_coach_action_count" "$ios_page_artwork_count" "$ios_visual_asset_count" "$ios_referenced_visual_asset_count" "$android_runtime_visual_asset_count"
