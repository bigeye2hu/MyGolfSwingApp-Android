#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

fail_if_dirty=false
max_paths=30
show_grouped_paths=false

usage() {
  cat <<'USAGE'
Usage: scripts/android_launch_dirty_summary.sh [--fail-if-dirty] [--max-paths=N] [--grouped-paths]

Prints a lightweight summary of the dirty Android launch worktree, grouped by
git status and launch-review area. It does not run Gradle or mutate files.

With --grouped-paths, prints all dirty paths grouped by launch review order.
With --fail-if-dirty, exits non-zero when any dirty path is present.
USAGE
}

for arg in "$@"; do
  case "$arg" in
    --fail-if-dirty)
      fail_if_dirty=true
      ;;
    --max-paths=*)
      max_paths="${arg#--max-paths=}"
      if [[ ! "$max_paths" =~ ^[0-9]+$ ]]; then
        echo "--max-paths must be a non-negative integer, got '$max_paths'" >&2
        exit 2
      fi
      ;;
    --grouped-paths)
      show_grouped_paths=true
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

classify_status() {
  local code="$1"
  case "$code" in
    "??")
      printf 'untracked'
      ;;
    *U*|U*)
      printf 'conflicted'
      ;;
    *D*|D*)
      printf 'deleted'
      ;;
    *A*|A*)
      printf 'added'
      ;;
    *R*|R*)
      printf 'renamed'
      ;;
    *C*|C*)
      printf 'copied'
      ;;
    *M*|M*)
      printf 'modified'
      ;;
    *)
      printf 'other'
      ;;
  esac
}

classify_area() {
  local path="$1"
  case "$path" in
    app/src/main/java/*|app/src/main/kotlin/*)
      printf 'app source'
      ;;
    app/src/main/res/*|app/src/main/assets/*)
      printf 'app resources/assets'
      ;;
    app/src/test/*|app/src/androidTest/*)
      printf 'app tests'
      ;;
    docs/*)
      printf 'docs'
      ;;
    scripts/*)
      printf 'scripts'
      ;;
    app/build.gradle*|build.gradle*|settings.gradle*|gradle.properties|gradle/*)
      printf 'gradle/build config'
      ;;
    .gitignore|.gitattributes|README*|LICENSE*)
      printf 'repo config/docs'
      ;;
    *)
      printf 'other'
      ;;
  esac
}

count_status() {
  local label="$1"
  case "$label" in
    modified) modified_count=$((modified_count + 1)) ;;
    added) added_count=$((added_count + 1)) ;;
    deleted) deleted_count=$((deleted_count + 1)) ;;
    renamed) renamed_count=$((renamed_count + 1)) ;;
    copied) copied_count=$((copied_count + 1)) ;;
    untracked) untracked_count=$((untracked_count + 1)) ;;
    conflicted) conflicted_count=$((conflicted_count + 1)) ;;
    *) other_status_count=$((other_status_count + 1)) ;;
  esac
}

count_area() {
  local label="$1"
  case "$label" in
    "app source") app_source_count=$((app_source_count + 1)) ;;
    "app resources/assets") app_resources_count=$((app_resources_count + 1)) ;;
    "app tests") app_tests_count=$((app_tests_count + 1)) ;;
    docs) docs_count=$((docs_count + 1)) ;;
    scripts) scripts_count=$((scripts_count + 1)) ;;
    "gradle/build config") gradle_count=$((gradle_count + 1)) ;;
    "repo config/docs") repo_config_count=$((repo_config_count + 1)) ;;
    *) other_area_count=$((other_area_count + 1)) ;;
  esac
}

print_count() {
  local label="$1"
  local value="$2"
  if (( value > 0 )); then
    printf '  %-22s %s\n' "$label:" "$value"
  fi
}

echo "== Android launch dirty summary =="

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Source state: unavailable (not a git worktree)"
  exit 1
fi

branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || printf 'unknown')"
commit="$(git rev-parse --short HEAD 2>/dev/null || printf 'unknown')"
dirty_count="$(git status --porcelain=v1 --untracked-files=all | wc -l | tr -d ' ')"
echo "Source state: branch=$branch, commit=$commit, dirtyFiles=$dirty_count"

if [[ "$dirty_count" == "0" ]]; then
  echo "Dirty gate: clean"
  echo "Launch source tree is clean."
  exit 0
fi

echo "Dirty gate: development; signed release-candidate handoff requires dirtyFiles=0"

modified_count=0
added_count=0
deleted_count=0
renamed_count=0
copied_count=0
untracked_count=0
conflicted_count=0
other_status_count=0

app_source_count=0
app_resources_count=0
app_tests_count=0
docs_count=0
scripts_count=0
gradle_count=0
repo_config_count=0
other_area_count=0

review_main_count=0
review_tests_count=0
review_release_ops_count=0
review_config_count=0
review_other_count=0

paths_seen=0
path_lines=()
product_path_lines=()
test_path_lines=()
release_ops_path_lines=()
config_path_lines=()
other_path_lines=()

while IFS= read -r line; do
  status_code="${line:0:2}"
  path="${line:3}"
  case "$path" in
    *" -> "*)
      path="${path##* -> }"
      ;;
  esac

  status_label="$(classify_status "$status_code")"
  area_label="$(classify_area "$path")"
  count_status "$status_label"
  count_area "$area_label"

  path_line="  $status_code $path"

  case "$area_label" in
    "app source"|"app resources/assets")
      review_main_count=$((review_main_count + 1))
      product_path_lines+=("$path_line")
      ;;
    "app tests")
      review_tests_count=$((review_tests_count + 1))
      test_path_lines+=("$path_line")
      ;;
    docs|scripts)
      review_release_ops_count=$((review_release_ops_count + 1))
      release_ops_path_lines+=("$path_line")
      ;;
    "gradle/build config"|"repo config/docs")
      review_config_count=$((review_config_count + 1))
      config_path_lines+=("$path_line")
      ;;
    *)
      review_other_count=$((review_other_count + 1))
      other_path_lines+=("$path_line")
      ;;
  esac

  if (( paths_seen < max_paths )); then
    path_lines+=("$path_line")
  fi
  paths_seen=$((paths_seen + 1))
done < <(git status --porcelain=v1 --untracked-files=all)

echo "Status counts:"
print_count "modified" "$modified_count"
print_count "added" "$added_count"
print_count "deleted" "$deleted_count"
print_count "renamed" "$renamed_count"
print_count "copied" "$copied_count"
print_count "untracked" "$untracked_count"
print_count "conflicted" "$conflicted_count"
print_count "other" "$other_status_count"

echo "Area counts:"
print_count "app source" "$app_source_count"
print_count "app resources/assets" "$app_resources_count"
print_count "app tests" "$app_tests_count"
print_count "docs" "$docs_count"
print_count "scripts" "$scripts_count"
print_count "gradle/build config" "$gradle_count"
print_count "repo config/docs" "$repo_config_count"
print_count "other" "$other_area_count"

echo "Review order:"
print_count "product/runtime" "$review_main_count"
print_count "tests" "$review_tests_count"
print_count "release docs/scripts" "$review_release_ops_count"
print_count "build/repo config" "$review_config_count"
print_count "other" "$review_other_count"

if (( max_paths > 0 )); then
  echo "Dirty paths (first $max_paths):"
  printf '%s\n' "${path_lines[@]}"
  remaining=$((dirty_count - ${#path_lines[@]}))
  if (( remaining > 0 )); then
    echo "  ... $remaining more"
  fi
fi

print_path_group() {
  local label="$1"
  shift
  local lines=("$@")
  if (( ${#lines[@]} == 0 )); then
    return
  fi
  echo "$label (${#lines[@]}):"
  printf '%s\n' "${lines[@]}"
}

if [[ "$show_grouped_paths" == true ]]; then
  echo "Grouped dirty paths:"
  if (( ${#product_path_lines[@]} > 0 )); then
    print_path_group "Product/runtime" "${product_path_lines[@]}"
  fi
  if (( ${#test_path_lines[@]} > 0 )); then
    print_path_group "Tests" "${test_path_lines[@]}"
  fi
  if (( ${#release_ops_path_lines[@]} > 0 )); then
    print_path_group "Release docs/scripts" "${release_ops_path_lines[@]}"
  fi
  if (( ${#config_path_lines[@]} > 0 )); then
    print_path_group "Build/repo config" "${config_path_lines[@]}"
  fi
  if (( ${#other_path_lines[@]} > 0 )); then
    print_path_group "Other" "${other_path_lines[@]}"
  fi
fi

echo "Next cleanup gate: scripts/android_launch_dirty_summary.sh --fail-if-dirty"

if [[ "$fail_if_dirty" == true ]]; then
  echo "Dirty readiness: attention needed"
  exit 1
fi
