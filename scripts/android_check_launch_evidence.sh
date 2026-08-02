#!/usr/bin/env bash
set -eo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MIN_PLAY_TARGET_SDK=36

usage() {
  cat <<'USAGE'
Usage: scripts/android_check_launch_evidence.sh path/to/android-launch-evidence.env

Validates the external release evidence that cannot be proven by the local
Gradle preflight alone: signed artifact identity, Play Console upload/install,
Billing product/test-purchase proof, backend validation/idempotency notes, and
device QA coverage.
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

evidence_file="${1:-}"
if [[ -z "$evidence_file" ]]; then
  usage >&2
  exit 2
fi
if [[ ! -f "$evidence_file" ]]; then
  echo "Evidence file not found: $evidence_file" >&2
  exit 1
fi
EVIDENCE_DIR="$(cd "$(dirname "$evidence_file")" && pwd)"

REQUIRED_KEYS=(
  RELEASE_BRANCH
  RELEASE_COMMIT
  RELEASE_DIRTY_FILES
  RELEASE_APPLICATION_ID
  RELEASE_VERSION_CODE
  RELEASE_VERSION_NAME
  RELEASE_TARGET_SDK
  RELEASE_APK_PATH
  RELEASE_APK_SHA256
  RELEASE_AAB_PATH
  RELEASE_AAB_SHA256
  RELEASE_AAB_SIGNER_SHA256
  RELEASE_PREFLIGHT_LOG
  RELEASE_PREFLIGHT_SIGNED_CHECK
  RELEASE_PREFLIGHT_MANIFEST_CHECK
  RELEASE_PREFLIGHT_DEBUG_RESOURCE_CHECK
  RELEASE_PREFLIGHT_LINT_WARNING_CHECK
  RELEASE_LINT_WARNING_COUNT
  PLAY_APP_SIGNING_ENABLED
  PLAY_UPLOAD_KEY_REGISTERED
  PLAY_SIGNING_SETUP_EVIDENCE
  PLAY_LATEST_VERSION_CODE_BEFORE_UPLOAD
  PLAY_UPLOAD_TRACK
  PLAY_UPLOAD_RELEASE_ID
  PLAY_RELEASE_NOTES_EVIDENCE
  PLAY_UPLOAD_METADATA_EVIDENCE
  PLAY_UPLOAD_EVIDENCE
  PLAY_INTERNAL_INSTALL_TESTED
  PLAY_INTERNAL_INSTALL_EVIDENCE
  PLAY_INSTALL_TESTER_ACCOUNT
  BILLING_PRODUCT_100_ACTIVE
  BILLING_PRODUCT_300_ACTIVE
  BILLING_PRODUCT_1000_ACTIVE
  BILLING_PRODUCTS_CONSOLE_EVIDENCE
  BILLING_PRICES_LOADED_EVIDENCE
  BILLING_LOGIN_GATE_EVIDENCE
  BILLING_SUCCESS_PRODUCT_ID
  BILLING_SUCCESS_ORDER_LABEL
  BILLING_SUCCESS_SERVER_GRANT_LOG
  BILLING_SUCCESS_BALANCE_BEFORE
  BILLING_SUCCESS_BALANCE_AFTER
  BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE
  BILLING_CONSUMED_EVIDENCE
  BILLING_RESTORE_EVIDENCE
  BILLING_CANCEL_EVIDENCE
  BILLING_FAILURE_COPY_EVIDENCE
  BILLING_BACKEND_VALIDATION_POLICY
  BILLING_BACKEND_IDEMPOTENCY_EVIDENCE
  DEVICE_ANDROID12_PLUS
  DEVICE_ANDROID14_PLUS
  DEVICE_QA_MATRIX_EVIDENCE
  QA_LOGIN_CONTINUATION_EVIDENCE
  QA_CAMERA_PERMISSION_EVIDENCE
  QA_SWING_RECORDING_EVIDENCE
  QA_HOME_ANALYSIS_SOURCE_EVIDENCE
  QA_GOLFCAM_EXPORT_EVIDENCE
  QA_VIDEO_MEMO_SOURCES_EVIDENCE
  QA_PLAZA_CLOUD_EVIDENCE
  QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE
  QA_PROFILE_KEYBOARD_EVIDENCE
)

TRUE_KEYS=(
  RELEASE_PREFLIGHT_SIGNED_CHECK
  RELEASE_PREFLIGHT_MANIFEST_CHECK
  RELEASE_PREFLIGHT_DEBUG_RESOURCE_CHECK
  RELEASE_PREFLIGHT_LINT_WARNING_CHECK
  PLAY_APP_SIGNING_ENABLED
  PLAY_UPLOAD_KEY_REGISTERED
  PLAY_INTERNAL_INSTALL_TESTED
  BILLING_PRODUCT_100_ACTIVE
  BILLING_PRODUCT_300_ACTIVE
  BILLING_PRODUCT_1000_ACTIVE
)

TEXT_VALIDATED_KEYS=(
  PLAY_SIGNING_SETUP_EVIDENCE
  PLAY_RELEASE_NOTES_EVIDENCE
  PLAY_UPLOAD_METADATA_EVIDENCE
  PLAY_INTERNAL_INSTALL_EVIDENCE
  BILLING_PRODUCTS_CONSOLE_EVIDENCE
  BILLING_PRICES_LOADED_EVIDENCE
  BILLING_LOGIN_GATE_EVIDENCE
  BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE
  BILLING_CONSUMED_EVIDENCE
  BILLING_RESTORE_EVIDENCE
  BILLING_CANCEL_EVIDENCE
  BILLING_FAILURE_COPY_EVIDENCE
  BILLING_BACKEND_VALIDATION_POLICY
  BILLING_BACKEND_IDEMPOTENCY_EVIDENCE
  DEVICE_ANDROID12_PLUS
  DEVICE_ANDROID14_PLUS
  DEVICE_QA_MATRIX_EVIDENCE
  QA_LOGIN_CONTINUATION_EVIDENCE
  QA_CAMERA_PERMISSION_EVIDENCE
  QA_SWING_RECORDING_EVIDENCE
  QA_HOME_ANALYSIS_SOURCE_EVIDENCE
  QA_GOLFCAM_EXPORT_EVIDENCE
  QA_VIDEO_MEMO_SOURCES_EVIDENCE
  QA_PLAZA_CLOUD_EVIDENCE
  QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE
  QA_PROFILE_KEYBOARD_EVIDENCE
)

parsed_keys=()
parsed_values=()
errors=()
warnings=()

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "$value"
}

unquote_value() {
  local value
  value="$(trim "$1")"
  if [[ "$value" == \"*\" && "$value" == *\" && ${#value} -ge 2 ]]; then
    value="${value:1:${#value}-2}"
  elif [[ "$value" == \'*\' && "$value" == *\' && ${#value} -ge 2 ]]; then
    value="${value:1:${#value}-2}"
  fi
  printf '%s' "$value"
}

is_placeholder() {
  local value
  value="$(trim "$1")"
  [[ -z "$value" ]] ||
    [[ "$value" =~ ^\<.*\>$ ]] ||
    [[ "$value" =~ ^[Tt][Oo][Dd][Oo]$ ]] ||
    [[ "$value" =~ ^[Tt][Bb][Dd]$ ]] ||
    [[ "$value" =~ ^[Pp][Ee][Nn][Dd][Ii][Nn][Gg]$ ]] ||
    [[ "$value" =~ ^[Nn]/[Aa]$ ]]
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

resolve_path() {
  local path="$1"
  if [[ -z "$path" ]]; then
    printf '%s' "$ROOT_DIR"
    return
  fi
  if [[ "$path" = /* ]]; then
    printf '%s' "$path"
  elif [[ -e "$EVIDENCE_DIR/$path" ]]; then
    printf '%s/%s' "$EVIDENCE_DIR" "$path"
  else
    printf '%s/%s' "$ROOT_DIR" "$path"
  fi
}

record_value() {
  parsed_keys+=("$1")
  parsed_values+=("$2")
}

get_value() {
  local lookup="$1"
  local i
  for ((i = ${#parsed_keys[@]} - 1; i >= 0; i--)); do
    if [[ "${parsed_keys[$i]}" == "$lookup" ]]; then
      printf '%s' "${parsed_values[$i]}"
      return 0
    fi
  done
  return 1
}

has_key() {
  local lookup="$1"
  local key
  for key in "${parsed_keys[@]}"; do
    [[ "$key" == "$lookup" ]] && return 0
  done
  return 1
}

key_count() {
  local lookup="$1"
  local count=0
  local key
  for key in "${parsed_keys[@]}"; do
    [[ "$key" == "$lookup" ]] && count=$((count + 1))
  done
  printf '%s' "$count"
}

is_required_key() {
  local lookup="$1"
  local required
  for required in "${REQUIRED_KEYS[@]}"; do
    [[ "$required" == "$lookup" ]] && return 0
  done
  return 1
}

lowercase() {
  printf '%s' "$1" | tr '[:upper:]' '[:lower:]'
}

contains_secret_material() {
  local value="$1"
  local lower
  lower="$(lowercase "$value")"
  [[ "$value" =~ BEGIN[[:space:]]+(RSA[[:space:]]+)?PRIVATE[[:space:]]+KEY ]] ||
    [[ "$value" =~ AKIA[0-9A-Z]{16} ]] ||
    [[ "$lower" == *"mygolfswing_release_store_password"* ]] ||
    [[ "$lower" == *"mygolfswing_release_key_password"* ]] ||
    [[ "$lower" =~ (^|[^a-z0-9_])(store_password|key_password|api[_-]?key|secret|password)[[:space:]]*[:=] ]] ||
    [[ "$lower" =~ authorization:[[:space:]]*bearer[[:space:]]+[^[:space:]]+ ]]
}

text_file_contains_secret_material() {
  local path="$1"
  local sample
  LC_ALL=C grep -Iq . "$path" || return 1
  sample="$(LC_ALL=C head -c 1048576 "$path")"
  contains_secret_material "$sample"
}

text_file_path_for_key() {
  local key="$1"
  local value
  local resolved
  value="$(get_value "$key" || true)"
  [[ -z "$value" ]] && return 1
  resolved="$(resolve_path "$value")"
  [[ -f "$resolved" ]] || return 1
  LC_ALL=C grep -Iq . "$resolved" || return 1
  printf '%s' "$resolved"
}

existing_path_for_key() {
  local key="$1"
  local value
  local resolved
  value="$(get_value "$key" || true)"
  [[ -z "$value" ]] && return 1
  resolved="$(resolve_path "$value")"
  [[ -e "$resolved" ]] || return 1
  printf '%s' "$resolved"
}

is_non_empty_text_file() {
  local path="$1"
  [[ -f "$path" && -s "$path" ]] || return 1
  LC_ALL=C grep -Iq . "$path"
}

evidence_text_for_key() {
  local key="$1"
  local text_path
  if text_path="$(text_file_path_for_key "$key")"; then
    LC_ALL=C head -c 1048576 "$text_path"
  else
    get_value "$key" || true
  fi
}

evidence_text_lower_for_key() {
  lowercase "$(evidence_text_for_key "$1")"
}

while IFS= read -r raw_line || [[ -n "$raw_line" ]]; do
  line="$(trim "$raw_line")"
  [[ -z "$line" || "$line" == \#* ]] && continue
  if [[ ! "$line" =~ ^[A-Za-z_][A-Za-z0-9_]*= ]]; then
    errors+=("Invalid evidence line: $raw_line")
    continue
  fi
  key="${line%%=*}"
  value="${line#*=}"
  record_value "$key" "$(unquote_value "$value")"
done < "$evidence_file"

for key in "${parsed_keys[@]}"; do
  if ! is_required_key "$key"; then
    errors+=("Unknown evidence key: $key")
  fi
  value="$(get_value "$key" || true)"
  if contains_secret_material "$value"; then
    errors+=("Evidence value may contain secret material: $key")
  fi
  if (( $(key_count "$key") > 1 )); then
    errors+=("Duplicate key: $key")
  fi
done

for key in "${REQUIRED_KEYS[@]}"; do
  if ! has_key "$key"; then
    errors+=("Missing required key: $key")
  else
    value="$(get_value "$key" || true)"
    if is_placeholder "$value"; then
      errors+=("Required key is blank or placeholder: $key")
    fi
  fi
done

for key in "${TEXT_VALIDATED_KEYS[@]}"; do
  if resolved="$(existing_path_for_key "$key")"; then
    if ! is_non_empty_text_file "$resolved"; then
      errors+=("$key must be inline text or a non-empty text evidence file; binary proof needs companion textual metadata")
    fi
  fi
done

require_regex() {
  local key="$1"
  local regex="$2"
  local message="$3"
  local value
  value="$(get_value "$key" || true)"
  if [[ -n "$value" && ! "$value" =~ $regex ]]; then
    errors+=("$message: $key=$value")
  fi
}

require_equals() {
  local key="$1"
  local expected="$2"
  local value
  value="$(get_value "$key" || true)"
  if [[ -n "$value" && "$value" != "$expected" ]]; then
    errors+=("$key must be '$expected', got '$value'")
  fi
}

require_truthy() {
  local key="$1"
  local value
  value="$(lowercase "$(get_value "$key" || true)")"
  case "$value" in
    true|yes|pass|passed|1) ;;
    *) errors+=("$key must be true/pass evidence, got '$(get_value "$key" || true)'") ;;
  esac
}

log_field_value() {
  local line="$1"
  local field="$2"
  printf '%s\n' "$line" | sed -n "s/.*$field=\\([^, ]*\\).*/\\1/p"
}

require_log_field_match() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  if [[ -n "$expected" && -n "$actual" && "$expected" != "$actual" ]]; then
    errors+=("$label does not match RELEASE_PREFLIGHT_LOG; expected '$expected', got '$actual'")
  fi
}

contains_any_word() {
  local value="$1"
  shift
  local word
  for word in "$@"; do
    [[ "$value" == *"$word"* ]] && return 0
  done
  return 1
}

normalise_fingerprint() {
  printf '%s' "$1" | tr -d ':[:space:]' | tr '[:lower:]' '[:upper:]'
}

text_contains_fingerprint() {
  local text="$1"
  local fingerprint="$2"
  local normalised_text
  local normalised_fingerprint
  normalised_text="$(normalise_fingerprint "$text")"
  normalised_fingerprint="$(normalise_fingerprint "$fingerprint")"
  [[ -n "$normalised_fingerprint" && "$normalised_text" == *"$normalised_fingerprint"* ]]
}

android_device_version_ok() {
  local value="$1"
  local min_android="$2"
  local min_api="$3"
  local version
  if [[ "$value" =~ [Aa]ndroid[[:space:]]*([0-9]+) ]]; then
    version="${BASH_REMATCH[1]}"
    (( 10#$version >= min_android )) && return 0
  fi
  if [[ "$value" =~ [Aa][Pp][Ii][[:space:]]*([0-9]+) ]]; then
    version="${BASH_REMATCH[1]}"
    (( 10#$version >= min_api )) && return 0
  fi
  return 1
}

require_android_device_version() {
  local key="$1"
  local min_android="$2"
  local min_api="$3"
  local value
  value="$(evidence_text_for_key "$key")"
  if [[ -n "$value" ]] && ! android_device_version_ok "$value" "$min_android" "$min_api"; then
    errors+=("$key must mention Android $min_android+ or API $min_api+ device evidence")
  fi
}

require_evidence_terms() {
  local key="$1"
  local description="$2"
  shift 2
  local value
  value="$(evidence_text_lower_for_key "$key")"
  if [[ -n "$value" ]] && ! contains_any_word "$value" "$@"; then
    errors+=("$key must mention $description")
  fi
}

billing_order_label_ok() {
  local value="$1"
  local lower
  lower="$(lowercase "$value")"
  [[ "$value" =~ GPA\.[0-9]{4}(-[0-9]{4}){1,3}(-[0-9]{5})? ]] ||
    [[ "$lower" =~ (token|purchase[[:space:]]*token).*suffix[[:space:]:：-]*[a-z0-9]{6,16} ]] ||
    [[ "$value" =~ 订单凭据尾号[[:space:]:：-]*[A-Za-z0-9]{6,16} ]]
}

play_release_reference_ok() {
  local value="$1"
  local lower
  lower="$(lowercase "$value")"
  [[ "$value" =~ [0-9] ]] &&
    contains_any_word "$lower" release track version versioncode rollout internal closed open production 版本 发布 轨道
}

metadata_has_number_field() {
  local value="$1"
  local label_regex="$2"
  local expected="$3"
  local lower
  local regex
  lower="$(lowercase "$value")"
  regex="${label_regex}[^0-9]*${expected}([^0-9]|$)"
  [[ "$lower" =~ $regex ]]
}

server_grant_log_reference_ok() {
  local value="$1"
  local lower
  lower="$(lowercase "$value")"
  [[ "$value" =~ [0-9] ]] &&
    contains_any_word "$lower" grant log trace request cloud function server id transaction order 发放 入账 日志 请求 订单
}

require_equals RELEASE_DIRTY_FILES "0"
require_equals RELEASE_APPLICATION_ID "com.mygolfswing.app"
require_regex RELEASE_COMMIT '^[0-9a-fA-F]{7,40}$' "Release commit must be a git hash"
require_regex RELEASE_VERSION_CODE '^[1-9][0-9]*$' "Release versionCode must be positive"
require_regex RELEASE_TARGET_SDK '^[1-9][0-9]*$' "Release targetSdk must be positive"
require_regex RELEASE_LINT_WARNING_COUNT '^[0-9]+$' "Release lint warning count must be a non-negative integer"
require_regex RELEASE_APK_SHA256 '^[0-9a-fA-F]{64}$' "APK SHA-256 must be 64 hex chars"
require_regex RELEASE_AAB_SHA256 '^[0-9a-fA-F]{64}$' "AAB SHA-256 must be 64 hex chars"
require_regex RELEASE_AAB_SIGNER_SHA256 '^([0-9A-Fa-f]{2}:){31}[0-9A-Fa-f]{2}$' "AAB signer SHA-256 must be a colon-separated certificate fingerprint"
require_regex PLAY_LATEST_VERSION_CODE_BEFORE_UPLOAD '^[0-9]+$' "PLAY_LATEST_VERSION_CODE_BEFORE_UPLOAD must be a non-negative integer"
require_regex PLAY_INSTALL_TESTER_ACCOUNT '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$' "PLAY_INSTALL_TESTER_ACCOUNT must be an email-like tester account"
require_android_device_version DEVICE_ANDROID12_PLUS 12 31
require_android_device_version DEVICE_ANDROID14_PLUS 14 34
require_evidence_terms DEVICE_QA_MATRIX_EVIDENCE "Android 12/API 31 matrix proof" "android 12" "api 31"
require_evidence_terms DEVICE_QA_MATRIX_EVIDENCE "Android 14/API 34 matrix proof" "android 14" "api 34"
require_evidence_terms DEVICE_QA_MATRIX_EVIDENCE "pass/fail notes" pass fail passed failed 通过 失败
require_evidence_terms DEVICE_QA_MATRIX_EVIDENCE "camera matrix proof" camera 摄像 相机
require_evidence_terms DEVICE_QA_MATRIX_EVIDENCE "gallery matrix proof" gallery mediastore album photos 相册 图库
require_evidence_terms DEVICE_QA_MATRIX_EVIDENCE "GolfCam export matrix proof" golfcam "golf cam" export exported 导出
require_evidence_terms DEVICE_QA_MATRIX_EVIDENCE "Billing matrix proof" billing purchase payment 购买 支付 充值
require_evidence_terms DEVICE_QA_MATRIX_EVIDENCE "login continuation matrix proof" login continuation return 登录 续接 返回
require_evidence_terms DEVICE_QA_MATRIX_EVIDENCE "Plaza cloud matrix proof" plaza cloud community 广场 云端 社区
require_evidence_terms QA_LOGIN_CONTINUATION_EVIDENCE "login/sign-in proof" login "sign in" signin 登录
require_evidence_terms QA_LOGIN_CONTINUATION_EVIDENCE "fresh-install proof" "fresh install" fresh reinstall clean-install cleaninstall 新装 首次安装
require_evidence_terms QA_LOGIN_CONTINUATION_EVIDENCE "SMS login proof" sms code verification-code verificationcode 短信 验证码
require_evidence_terms QA_LOGIN_CONTINUATION_EVIDENCE "password login proof" password pwd 密码
require_evidence_terms QA_LOGIN_CONTINUATION_EVIDENCE "continuation/return-after-auth proof" continuation continue return resume resumed 返回 续接 继续
require_evidence_terms QA_LOGIN_CONTINUATION_EVIDENCE "original gated destination proof" gated destination original target blocked 入口 原入口 原目标 受限
require_evidence_terms QA_CAMERA_PERMISSION_EVIDENCE "camera proof" camera 摄像 相机
require_evidence_terms QA_CAMERA_PERMISSION_EVIDENCE "permission proof" permission allow grant authorize 权限 授权 允许
require_evidence_terms QA_CAMERA_PERMISSION_EVIDENCE "permission denial proof" deny denied denial reject rejected 拒绝 未授权 禁止
require_evidence_terms QA_CAMERA_PERMISSION_EVIDENCE "permanent-denial/settings recovery proof" permanent settings setting recover recovered recovery "don't ask again" dontaskagain 永久 设置 恢复 不再询问
require_evidence_terms QA_CAMERA_PERMISSION_EVIDENCE "swing-recording permission proof" swing recording record 挥杆 录制
require_evidence_terms QA_CAMERA_PERMISSION_EVIDENCE "video-Memo camera-capture permission proof" "video memo" videomemo capture captured 视频笔记 拍摄
require_evidence_terms QA_SWING_RECORDING_EVIDENCE "swing proof" swing 挥杆
require_evidence_terms QA_SWING_RECORDING_EVIDENCE "recording proof" record recording recorded capture captured 录制 拍摄
require_evidence_terms QA_SWING_RECORDING_EVIDENCE "local clip save proof" save saved local clip 保存 本地 片段
require_evidence_terms QA_SWING_RECORDING_EVIDENCE "replay-analysis proof" replay analysis review 回看 分析
require_evidence_terms QA_HOME_ANALYSIS_SOURCE_EVIDENCE "home analysis source proof" "home analysis" analysis source 首页 分析 来源
require_evidence_terms QA_HOME_ANALYSIS_SOURCE_EVIDENCE "realtime recording source proof" realtime "real time" recording record 实时 录制 拍摄
require_evidence_terms QA_HOME_ANALYSIS_SOURCE_EVIDENCE "album trim source proof" album trim gallery picker 相册 剪裁 截取
require_evidence_terms QA_HOME_ANALYSIS_SOURCE_EVIDENCE "existing library source proof" library existing history saved 素材库 已有 历史
require_evidence_terms QA_GOLFCAM_EXPORT_EVIDENCE "GolfCam proof" golfcam "golf cam" 轨迹
require_evidence_terms QA_GOLFCAM_EXPORT_EVIDENCE "video import proof" import imported video 导入 视频
require_evidence_terms QA_GOLFCAM_EXPORT_EVIDENCE "workflow completion proof" workflow step steps complete completed finish finished 流程 步骤 完成
require_evidence_terms QA_GOLFCAM_EXPORT_EVIDENCE "preview proof" preview preflight 预览
require_evidence_terms QA_GOLFCAM_EXPORT_EVIDENCE "export/save proof" export exported render rendered save saved 导出 保存 渲染
require_evidence_terms QA_GOLFCAM_EXPORT_EVIDENCE "gallery/MediaStore proof" gallery mediastore photos album 相册 图库
require_evidence_terms QA_VIDEO_MEMO_SOURCES_EVIDENCE "video memo proof" "video memo" 视频笔记 视频 memo
require_evidence_terms QA_VIDEO_MEMO_SOURCES_EVIDENCE "source proof" source sources 来源
require_evidence_terms QA_VIDEO_MEMO_SOURCES_EVIDENCE "local source proof" local 本地
require_evidence_terms QA_VIDEO_MEMO_SOURCES_EVIDENCE "camera source proof" camera capture captured 摄像 相机 拍摄
require_evidence_terms QA_VIDEO_MEMO_SOURCES_EVIDENCE "online/cloud source proof" online cloud remote url http 在线 云端 网络
require_evidence_terms QA_PLAZA_CLOUD_EVIDENCE "plaza/community proof" plaza community 广场 社区
require_evidence_terms QA_PLAZA_CLOUD_EVIDENCE "cloud/network proof" cloud network remote 云端 云 网络
require_evidence_terms QA_PLAZA_CLOUD_EVIDENCE "feed load proof" feed list load loaded refresh 广场流 列表 加载 刷新
require_evidence_terms QA_PLAZA_CLOUD_EVIDENCE "player-open proof" player playback play open opened 播放器 播放 打开
require_evidence_terms QA_PLAZA_CLOUD_EVIDENCE "logged-in save/favorite/comment proof" logged-in signed-in login favorite save comment 收藏 保存 评论 登录后
require_evidence_terms QA_PLAZA_CLOUD_EVIDENCE "logged-out login-gate proof" logged-out anonymous login-gate gated sign-in 登录前 未登录 游客 拦截
require_evidence_terms QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE "low-credit proof" "low credit" insufficient 不足 低积分 积分不足
require_evidence_terms QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE "online-download proof" "online download" download remote 下载 在线下载
require_evidence_terms QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE "billing/purchase proof" billing purchase pay payment 充值 购买 支付
require_evidence_terms QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE "successful purchase/grant proof" success successful 成功
require_evidence_terms QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE "credit-grant/balance proof" grant granted balance refreshed 入账 发放 余额
require_evidence_terms QA_LOW_CREDIT_BILLING_CONTINUATION_EVIDENCE "continuation/return proof" continuation continue return resume resumed 返回 续接 继续
require_evidence_terms QA_PROFILE_KEYBOARD_EVIDENCE "profile proof" profile account 个人资料 资料 账号
require_evidence_terms QA_PROFILE_KEYBOARD_EVIDENCE "avatar-update proof" avatar update updated profile-photo profile-picture 头像 更新
require_evidence_terms QA_PROFILE_KEYBOARD_EVIDENCE "QR image save proof" qr qrcode "qr code" save saved image 二维码 保存 图片
require_evidence_terms QA_PROFILE_KEYBOARD_EVIDENCE "feedback-submit proof" feedback submit submitted 反馈 提交
require_evidence_terms QA_PROFILE_KEYBOARD_EVIDENCE "account-security proof" "account security" security password phone 安全 账号安全 密码 手机号
require_evidence_terms QA_PROFILE_KEYBOARD_EVIDENCE "keyboard/input proof" keyboard input ime 键盘 输入
require_evidence_terms QA_PROFILE_KEYBOARD_EVIDENCE "navigation-bar reachability proof" navigation nav-bar reachable bottom system-bar 导航栏 底部 可达

release_application_id="$(get_value RELEASE_APPLICATION_ID || true)"
release_version_code="$(get_value RELEASE_VERSION_CODE || true)"
release_version_name="$(get_value RELEASE_VERSION_NAME || true)"
release_target_sdk="$(get_value RELEASE_TARGET_SDK || true)"
aab_signer_sha="$(get_value RELEASE_AAB_SIGNER_SHA256 || true)"
play_latest_version_code="$(get_value PLAY_LATEST_VERSION_CODE_BEFORE_UPLOAD || true)"
if [[ "$release_target_sdk" =~ ^[0-9]+$ ]] && (( 10#$release_target_sdk < MIN_PLAY_TARGET_SDK )); then
  errors+=("RELEASE_TARGET_SDK must be >= $MIN_PLAY_TARGET_SDK for current Play upload readiness")
fi
if [[ "$release_version_code" =~ ^[0-9]+$ && "$play_latest_version_code" =~ ^[0-9]+$ ]] &&
  (( 10#$release_version_code <= 10#$play_latest_version_code )); then
  errors+=("RELEASE_VERSION_CODE must be greater than PLAY_LATEST_VERSION_CODE_BEFORE_UPLOAD; got release '$release_version_code' and Play latest '$play_latest_version_code'")
fi

for key in "${TRUE_KEYS[@]}"; do
  require_truthy "$key"
done

play_signing_setup_evidence="$(evidence_text_lower_for_key PLAY_SIGNING_SETUP_EVIDENCE)"
if [[ -n "$play_signing_setup_evidence" ]]; then
  if [[ -n "$release_application_id" && "$play_signing_setup_evidence" != *"$(lowercase "$release_application_id")"* ]]; then
    errors+=("PLAY_SIGNING_SETUP_EVIDENCE must mention RELEASE_APPLICATION_ID '$release_application_id'")
  fi
  if ! contains_any_word "$play_signing_setup_evidence" app-signing "app signing" playsigning "play signing" 应用签名 签名; then
    errors+=("PLAY_SIGNING_SETUP_EVIDENCE must mention Play App Signing")
  fi
  if ! contains_any_word "$play_signing_setup_evidence" upload-key "upload key" uploadkey 上传密钥 上传证书; then
    errors+=("PLAY_SIGNING_SETUP_EVIDENCE must mention upload key registration")
  fi
  if ! contains_any_word "$play_signing_setup_evidence" fingerprint certificate cert sha-256 sha256 "sha 256" 证书 指纹; then
    errors+=("PLAY_SIGNING_SETUP_EVIDENCE must mention certificate fingerprint/SHA proof")
  fi
  if [[ -n "$aab_signer_sha" ]] && ! text_contains_fingerprint "$play_signing_setup_evidence" "$aab_signer_sha"; then
    errors+=("PLAY_SIGNING_SETUP_EVIDENCE must include RELEASE_AAB_SIGNER_SHA256 '$aab_signer_sha'")
  fi
fi

play_upload_track="$(get_value PLAY_UPLOAD_TRACK || true)"
play_upload_track_lower="$(lowercase "$play_upload_track")"
play_upload_track_spaced="${play_upload_track_lower//-/ }"
if [[ -n "$play_upload_track" ]]; then
  case "$play_upload_track_lower" in
    internal|internal-testing|closed|closed-testing|open|open-testing|production) ;;
    *) errors+=("PLAY_UPLOAD_TRACK must be one of: internal-testing, closed-testing, open-testing, production") ;;
  esac
fi

play_upload_release_id="$(get_value PLAY_UPLOAD_RELEASE_ID || true)"
if [[ -n "$play_upload_release_id" ]] && ! play_release_reference_ok "$play_upload_release_id"; then
  errors+=("PLAY_UPLOAD_RELEASE_ID must be a traceable Play release/version/track reference, not generic pass text")
fi

play_release_notes_evidence="$(evidence_text_for_key PLAY_RELEASE_NOTES_EVIDENCE)"
if [[ -n "$play_release_notes_evidence" && -n "$release_version_name" && "$play_release_notes_evidence" != *"$release_version_name"* ]]; then
  errors+=("PLAY_RELEASE_NOTES_EVIDENCE must mention RELEASE_VERSION_NAME '$release_version_name'")
fi

play_upload_metadata_evidence="$(evidence_text_for_key PLAY_UPLOAD_METADATA_EVIDENCE)"
play_upload_metadata_lower="$(lowercase "$play_upload_metadata_evidence")"
if [[ -n "$play_upload_metadata_evidence" ]]; then
  if [[ -n "$release_application_id" && "$play_upload_metadata_lower" != *"$(lowercase "$release_application_id")"* ]]; then
    errors+=("PLAY_UPLOAD_METADATA_EVIDENCE must mention RELEASE_APPLICATION_ID '$release_application_id'")
  fi
  if [[ -n "$release_version_code" ]] &&
    ! metadata_has_number_field "$play_upload_metadata_evidence" "(version[[:space:]_-]*code|versioncode|版本号)" "$release_version_code"; then
    errors+=("PLAY_UPLOAD_METADATA_EVIDENCE must mention RELEASE_VERSION_CODE '$release_version_code'")
  fi
  if [[ -n "$release_version_name" && "$play_upload_metadata_lower" != *"$(lowercase "$release_version_name")"* ]]; then
    errors+=("PLAY_UPLOAD_METADATA_EVIDENCE must mention RELEASE_VERSION_NAME '$release_version_name'")
  fi
  if [[ -n "$release_target_sdk" ]] &&
    ! metadata_has_number_field "$play_upload_metadata_evidence" "(target[[:space:]_-]*sdk|targetsdk|target[[:space:]_-]*api|目标[[:space:]]*sdk)" "$release_target_sdk"; then
    errors+=("PLAY_UPLOAD_METADATA_EVIDENCE must mention RELEASE_TARGET_SDK '$release_target_sdk'")
  fi
  if [[ -n "$play_upload_track" &&
      "$play_upload_metadata_lower" != *"$play_upload_track_lower"* &&
      "$play_upload_metadata_lower" != *"$play_upload_track_spaced"* ]]; then
    errors+=("PLAY_UPLOAD_METADATA_EVIDENCE must mention PLAY_UPLOAD_TRACK '$play_upload_track'")
  fi
  if [[ -n "$play_upload_release_id" && "$play_upload_metadata_lower" != *"$(lowercase "$play_upload_release_id")"* ]]; then
    errors+=("PLAY_UPLOAD_METADATA_EVIDENCE must mention PLAY_UPLOAD_RELEASE_ID '$play_upload_release_id'")
  fi
fi

play_install_tester_account="$(get_value PLAY_INSTALL_TESTER_ACCOUNT || true)"
play_internal_install_evidence="$(evidence_text_for_key PLAY_INTERNAL_INSTALL_EVIDENCE)"
play_internal_install_lower="$(lowercase "$play_internal_install_evidence")"
if [[ -n "$play_internal_install_evidence" ]]; then
  if [[ -n "$play_install_tester_account" && "$play_internal_install_lower" != *"$(lowercase "$play_install_tester_account")"* ]]; then
    errors+=("PLAY_INTERNAL_INSTALL_EVIDENCE must mention PLAY_INSTALL_TESTER_ACCOUNT '$play_install_tester_account'")
  fi
  if [[ -n "$release_application_id" && "$play_internal_install_lower" != *"$(lowercase "$release_application_id")"* ]]; then
    errors+=("PLAY_INTERNAL_INSTALL_EVIDENCE must mention RELEASE_APPLICATION_ID '$release_application_id'")
  fi
  if [[ -n "$release_version_code" ]] &&
    ! metadata_has_number_field "$play_internal_install_evidence" "(version[[:space:]_-]*code|versioncode|版本号)" "$release_version_code"; then
    errors+=("PLAY_INTERNAL_INSTALL_EVIDENCE must mention RELEASE_VERSION_CODE '$release_version_code'")
  fi
  if [[ -n "$release_version_name" && "$play_internal_install_lower" != *"$(lowercase "$release_version_name")"* ]]; then
    errors+=("PLAY_INTERNAL_INSTALL_EVIDENCE must mention RELEASE_VERSION_NAME '$release_version_name'")
  fi
  if [[ -n "$play_upload_track" &&
      "$play_internal_install_lower" != *"$play_upload_track_lower"* &&
      "$play_internal_install_lower" != *"$play_upload_track_spaced"* ]]; then
    errors+=("PLAY_INTERNAL_INSTALL_EVIDENCE must mention PLAY_UPLOAD_TRACK '$play_upload_track'")
  fi
  if ! contains_any_word "$play_internal_install_lower" install installed open opened launch launched 安装 打开 启动; then
    errors+=("PLAY_INTERNAL_INSTALL_EVIDENCE must mention install/open proof")
  fi
fi

billing_products_console_evidence="$(evidence_text_lower_for_key BILLING_PRODUCTS_CONSOLE_EVIDENCE)"
if [[ -n "$billing_products_console_evidence" ]]; then
  for product_id in \
    com.mygolfswingapp.credits.100 \
    com.mygolfswingapp.credits.300 \
    com.mygolfswingapp.credits.1000; do
    if [[ "$billing_products_console_evidence" != *"$product_id"* ]]; then
      errors+=("BILLING_PRODUCTS_CONSOLE_EVIDENCE must mention product '$product_id'")
    fi
  done
  if ! contains_any_word "$billing_products_console_evidence" active activated enabled available 可购买 已启用 生效; then
    errors+=("BILLING_PRODUCTS_CONSOLE_EVIDENCE must mention active/available product status")
  fi
  if ! contains_any_word "$billing_products_console_evidence" price prices localized amount 价格 金额; then
    errors+=("BILLING_PRODUCTS_CONSOLE_EVIDENCE must mention product price/localized amount status")
  fi
fi

billing_prices_loaded_evidence="$(evidence_text_lower_for_key BILLING_PRICES_LOADED_EVIDENCE)"
if [[ -n "$billing_prices_loaded_evidence" ]]; then
  if ! contains_any_word "$billing_prices_loaded_evidence" google_play google-play "google play" billing play; then
    errors+=("BILLING_PRICES_LOADED_EVIDENCE must mention Google Play Billing price proof")
  fi
  if ! contains_any_word "$billing_prices_loaded_evidence" price prices localized amount 价格 金额; then
    errors+=("BILLING_PRICES_LOADED_EVIDENCE must mention localized price/amount proof")
  fi
  for tier in 100 300 1000; do
    if [[ "$billing_prices_loaded_evidence" != *"$tier"* ]]; then
      errors+=("BILLING_PRICES_LOADED_EVIDENCE must mention $tier credit tier")
    fi
  done
fi

require_evidence_terms BILLING_LOGIN_GATE_EVIDENCE "login/sign-in gate proof" login "sign in" signin 登录
require_evidence_terms BILLING_LOGIN_GATE_EVIDENCE "purchase return/continuation proof" purchase billing return continue resume 购买 支付 充值 返回 继续 续接

billing_product_id="$(get_value BILLING_SUCCESS_PRODUCT_ID || true)"
if [[ -n "$billing_product_id" ]]; then
  case "$billing_product_id" in
    com.mygolfswingapp.credits.100|com.mygolfswingapp.credits.300|com.mygolfswingapp.credits.1000) ;;
    *) errors+=("BILLING_SUCCESS_PRODUCT_ID must be one of the configured Play products") ;;
  esac
fi

billing_order_label="$(get_value BILLING_SUCCESS_ORDER_LABEL || true)"
if [[ -n "$billing_order_label" ]] && ! billing_order_label_ok "$billing_order_label"; then
  errors+=("BILLING_SUCCESS_ORDER_LABEL must be a Google Play GPA order id or a purchase-token suffix label")
fi

billing_grant_log="$(get_value BILLING_SUCCESS_SERVER_GRANT_LOG || true)"
if [[ -n "$billing_grant_log" ]] && ! server_grant_log_reference_ok "$billing_grant_log"; then
  errors+=("BILLING_SUCCESS_SERVER_GRANT_LOG must be a traceable server grant log/request reference, not generic pass text")
fi

grant_payload_evidence="$(evidence_text_lower_for_key BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE)"
if [[ -n "$grant_payload_evidence" ]]; then
  if ! contains_any_word "$grant_payload_evidence" product_id product-id productid "product id"; then
    errors+=("BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE must mention product id")
  fi
  if ! contains_any_word "$grant_payload_evidence" purchase_token purchase-token "purchase token"; then
    errors+=("BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE must mention purchase token")
  fi
  if ! contains_any_word "$grant_payload_evidence" package_name package-name "package name"; then
    errors+=("BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE must mention package name")
  fi
  if ! contains_any_word "$grant_payload_evidence" quantity; then
    errors+=("BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE must mention quantity")
  fi
  if ! contains_any_word "$grant_payload_evidence" google_play google-play "google play"; then
    errors+=("BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE must mention google_play platform")
  fi
  if ! contains_any_word "$grant_payload_evidence" transaction_id transaction-id "transaction id"; then
    errors+=("BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE must mention transaction id")
  fi
  if ! contains_any_word "$grant_payload_evidence" amount; then
    errors+=("BILLING_SUCCESS_GRANT_PAYLOAD_EVIDENCE must mention amount")
  fi
fi

consumed_evidence="$(evidence_text_lower_for_key BILLING_CONSUMED_EVIDENCE)"
if [[ -n "$consumed_evidence" ]]; then
  if ! contains_any_word "$consumed_evidence" consume consumed consuming 消费 核销; then
    errors+=("BILLING_CONSUMED_EVIDENCE must mention consume/consumed proof")
  fi
  if ! contains_any_word "$consumed_evidence" grant granted balance 发放 入账 余额; then
    errors+=("BILLING_CONSUMED_EVIDENCE must tie consumption to a successful grant")
  fi
fi

restore_evidence="$(evidence_text_lower_for_key BILLING_RESTORE_EVIDENCE)"
if [[ -n "$restore_evidence" ]]; then
  if ! contains_any_word "$restore_evidence" restore restored recover recovered recovery 恢复; then
    errors+=("BILLING_RESTORE_EVIDENCE must mention restore/recovery proof")
  fi
  if ! contains_any_word "$restore_evidence" unconsumed pending interrupted unfinished 未消费 未完成 中断; then
    errors+=("BILLING_RESTORE_EVIDENCE must mention an unconsumed/interrupted purchase")
  fi
fi

cancel_evidence="$(evidence_text_lower_for_key BILLING_CANCEL_EVIDENCE)"
if [[ -n "$cancel_evidence" ]]; then
  if ! contains_any_word "$cancel_evidence" cancel canceled cancelled user_canceled user-canceled 用户取消 取消; then
    errors+=("BILLING_CANCEL_EVIDENCE must mention user-cancel proof")
  fi
fi

failure_copy_evidence="$(evidence_text_lower_for_key BILLING_FAILURE_COPY_EVIDENCE)"
if [[ -n "$failure_copy_evidence" ]]; then
  if ! contains_any_word "$failure_copy_evidence" unavailable product-unavailable product_unavailable 商品不可用 不可购买; then
    errors+=("BILLING_FAILURE_COPY_EVIDENCE must mention product-unavailable copy")
  fi
  if ! contains_any_word "$failure_copy_evidence" grant-failure grant_failure "grant failure" 发放失败 入账失败; then
    errors+=("BILLING_FAILURE_COPY_EVIDENCE must mention grant-failure copy")
  fi
  if ! contains_any_word "$failure_copy_evidence" consume-failure consume_failure "consume failure" 消费失败 核销失败; then
    errors+=("BILLING_FAILURE_COPY_EVIDENCE must mention consume-failure copy")
  fi
  if ! contains_any_word "$failure_copy_evidence" release-safe release_safe "release copy" raw-hidden "raw hidden" 隐藏 文案; then
    errors+=("BILLING_FAILURE_COPY_EVIDENCE must mention release-safe/raw-hidden copy")
  fi
fi

billing_expected_credits_for_product() {
  case "$1" in
    com.mygolfswingapp.credits.100) printf '100' ;;
    com.mygolfswingapp.credits.300) printf '300' ;;
    com.mygolfswingapp.credits.1000) printf '1000' ;;
    *) return 1 ;;
  esac
}

balance_before="$(get_value BILLING_SUCCESS_BALANCE_BEFORE || true)"
balance_after="$(get_value BILLING_SUCCESS_BALANCE_AFTER || true)"
if [[ -n "$balance_before" && ! "$balance_before" =~ ^[0-9]+$ ]]; then
  errors+=("BILLING_SUCCESS_BALANCE_BEFORE must be a non-negative integer")
fi
if [[ -n "$balance_after" && ! "$balance_after" =~ ^[0-9]+$ ]]; then
  errors+=("BILLING_SUCCESS_BALANCE_AFTER must be a non-negative integer")
fi
if [[ "$balance_before" =~ ^[0-9]+$ && "$balance_after" =~ ^[0-9]+$ ]]; then
  if (( 10#$balance_after <= 10#$balance_before )); then
    errors+=("BILLING_SUCCESS_BALANCE_AFTER must be greater than BILLING_SUCCESS_BALANCE_BEFORE")
  fi
  if expected_credits="$(billing_expected_credits_for_product "$billing_product_id")"; then
    actual_delta=$((10#$balance_after - 10#$balance_before))
    if (( actual_delta != expected_credits )); then
      errors+=("Billing balance delta must match BILLING_SUCCESS_PRODUCT_ID; expected +$expected_credits, got +$actual_delta")
    fi
  fi
fi

backend_validation_policy="$(evidence_text_lower_for_key BILLING_BACKEND_VALIDATION_POLICY)"
if [[ -n "$backend_validation_policy" ]]; then
  if [[ "$backend_validation_policy" != *"google"* || "$backend_validation_policy" != *"play"* || "$backend_validation_policy" != *"token"* ]]; then
    errors+=("BILLING_BACKEND_VALIDATION_POLICY must mention Google Play purchase token validation")
  fi
  if ! contains_any_word "$backend_validation_policy" validate verifies verified verification 校验 验证; then
    errors+=("BILLING_BACKEND_VALIDATION_POLICY must describe server-side validation")
  fi
  if ! contains_any_word "$backend_validation_policy" before pre-grant pregrant grant 发放 入账; then
    errors+=("BILLING_BACKEND_VALIDATION_POLICY must describe validation before credit grant")
  fi
  if [[ "$backend_validation_policy" != *"$(lowercase "$release_application_id")"* ]] &&
    ! contains_any_word "$backend_validation_policy" package_name package-name package appid "app id" "application id" 包名 应用包名; then
    errors+=("BILLING_BACKEND_VALIDATION_POLICY must describe package/app id matching")
  fi
  if ! contains_any_word "$backend_validation_policy" product_id product-id productid "product id" 商品id 商品; then
    errors+=("BILLING_BACKEND_VALIDATION_POLICY must describe product id matching")
  fi
  if ! contains_any_word "$backend_validation_policy" purchase_state purchase-state "purchase state" purchased paid completed 已购买 已支付 支付成功; then
    errors+=("BILLING_BACKEND_VALIDATION_POLICY must describe purchased/paid state validation")
  fi
fi

backend_idempotency_evidence="$(evidence_text_lower_for_key BILLING_BACKEND_IDEMPOTENCY_EVIDENCE)"
if [[ -n "$backend_idempotency_evidence" ]]; then
  if ! contains_any_word "$backend_idempotency_evidence" duplicate duplicated replay repeated retry same idempotent idempotency 幂等 重复 重放; then
    errors+=("BILLING_BACKEND_IDEMPOTENCY_EVIDENCE must describe duplicate/replay handling")
  fi
  if ! contains_any_word "$backend_idempotency_evidence" transaction order purchase token grant 订单 凭据 发放 入账; then
    errors+=("BILLING_BACKEND_IDEMPOTENCY_EVIDENCE must tie idempotency to the purchase/order grant")
  fi
  if ! contains_any_word "$backend_idempotency_evidence" same existing unchanged once single "no double" "not twice" same-grant "same grant" 不重复 只发放一次 同一; then
    errors+=("BILLING_BACKEND_IDEMPOTENCY_EVIDENCE must describe no-duplicate credit grant outcome")
  fi
fi

for key in RELEASE_APK_PATH RELEASE_AAB_PATH RELEASE_PREFLIGHT_LOG PLAY_UPLOAD_EVIDENCE; do
  value="$(get_value "$key" || true)"
  [[ -z "$value" ]] && continue
  resolved="$(resolve_path "$value")"
  if [[ ! -s "$resolved" ]]; then
    errors+=("$key must point to a non-empty local artifact/evidence file: $value")
  elif text_file_contains_secret_material "$resolved"; then
    errors+=("$key file may contain secret material: $value")
  fi
done

for key in "${REQUIRED_KEYS[@]}"; do
  value="$(get_value "$key" || true)"
  [[ -z "$value" ]] && continue
  resolved="$(resolve_path "$value")"
  if [[ -f "$resolved" && -s "$resolved" ]] && text_file_contains_secret_material "$resolved"; then
    errors+=("$key file may contain secret material: $value")
  fi
done

play_upload_evidence="$(get_value PLAY_UPLOAD_EVIDENCE || true)"
if [[ -n "$play_upload_evidence" && -s "$(resolve_path "$play_upload_evidence")" ]]; then
  play_upload_path="$(resolve_path "$play_upload_evidence")"
  for artifact_key in RELEASE_APK_PATH RELEASE_AAB_PATH RELEASE_PREFLIGHT_LOG; do
    artifact_value="$(get_value "$artifact_key" || true)"
    [[ -z "$artifact_value" ]] && continue
    artifact_path="$(resolve_path "$artifact_value")"
    if [[ -s "$artifact_path" && "$play_upload_path" -ef "$artifact_path" ]]; then
      errors+=("PLAY_UPLOAD_EVIDENCE must be an independent Play Console proof file, not the same file as $artifact_key")
    fi
  done
  if play_upload_text_path="$(text_file_path_for_key PLAY_UPLOAD_EVIDENCE)"; then
    play_upload_text="$(LC_ALL=C head -c 1048576 "$play_upload_text_path")"
    play_upload_text_lower="$(lowercase "$play_upload_text")"
    if [[ -n "$release_application_id" && "$play_upload_text_lower" != *"$(lowercase "$release_application_id")"* ]]; then
      errors+=("PLAY_UPLOAD_EVIDENCE text file must mention RELEASE_APPLICATION_ID '$release_application_id'")
    fi
    if [[ -n "$release_version_code" ]] &&
      ! metadata_has_number_field "$play_upload_text" "(version[[:space:]_-]*code|versioncode|版本号)" "$release_version_code"; then
      errors+=("PLAY_UPLOAD_EVIDENCE text file must mention RELEASE_VERSION_CODE '$release_version_code'")
    fi
    if [[ -n "$release_version_name" && "$play_upload_text_lower" != *"$(lowercase "$release_version_name")"* ]]; then
      errors+=("PLAY_UPLOAD_EVIDENCE text file must mention RELEASE_VERSION_NAME '$release_version_name'")
    fi
    if [[ -n "$play_upload_track" &&
        "$play_upload_text_lower" != *"$play_upload_track_lower"* &&
        "$play_upload_text_lower" != *"$play_upload_track_spaced"* ]]; then
      errors+=("PLAY_UPLOAD_EVIDENCE text file must mention PLAY_UPLOAD_TRACK '$play_upload_track'")
    fi
    if [[ -n "$play_upload_release_id" && "$play_upload_text_lower" != *"$(lowercase "$play_upload_release_id")"* ]]; then
      errors+=("PLAY_UPLOAD_EVIDENCE text file must mention PLAY_UPLOAD_RELEASE_ID '$play_upload_release_id'")
    fi
  fi
fi

apk_path="$(get_value RELEASE_APK_PATH || true)"
apk_sha="$(get_value RELEASE_APK_SHA256 || true)"
if [[ -n "$apk_path" && -s "$(resolve_path "$apk_path")" && -n "$apk_sha" ]]; then
  actual="$(sha256_file "$(resolve_path "$apk_path")")"
  if [[ "$(lowercase "$actual")" != "$(lowercase "$apk_sha")" ]]; then
    errors+=("RELEASE_APK_SHA256 does not match RELEASE_APK_PATH; expected $apk_sha, got $actual")
  fi
fi

aab_path="$(get_value RELEASE_AAB_PATH || true)"
aab_sha="$(get_value RELEASE_AAB_SHA256 || true)"
aab_signer_sha="$(get_value RELEASE_AAB_SIGNER_SHA256 || true)"
if [[ -n "$aab_path" && -s "$(resolve_path "$aab_path")" && -n "$aab_sha" ]]; then
  resolved_aab="$(resolve_path "$aab_path")"
  actual="$(sha256_file "$resolved_aab")"
  if [[ "$(lowercase "$actual")" != "$(lowercase "$aab_sha")" ]]; then
    errors+=("RELEASE_AAB_SHA256 does not match RELEASE_AAB_PATH; expected $aab_sha, got $actual")
  fi
  if ! aab_has_signature_files "$resolved_aab"; then
    errors+=("RELEASE_AAB_PATH must contain META-INF signature files from a signed release bundle")
  else
    jar_signer="$(find_jarsigner)"
    if [[ -z "$jar_signer" ]]; then
      errors+=("RELEASE_AAB_PATH signature could not be verified because jarsigner was not found")
    elif ! aab_jarsigner_verified "$jar_signer" "$resolved_aab"; then
      errors+=("RELEASE_AAB_PATH jarsigner verification failed")
    fi
  fi
  if [[ -n "$aab_signer_sha" ]]; then
    actual_aab_signer="$(aab_signer_sha256 "$resolved_aab" || true)"
    if [[ -z "$actual_aab_signer" ]]; then
      errors+=("Could not read RELEASE_AAB_PATH signer SHA-256")
    elif [[ "$actual_aab_signer" != "$(printf '%s' "$aab_signer_sha" | tr '[:lower:]' '[:upper:]')" ]]; then
      errors+=("RELEASE_AAB_SIGNER_SHA256 does not match RELEASE_AAB_PATH; expected $aab_signer_sha, got $actual_aab_signer")
    fi
  fi
fi

preflight_log="$(get_value RELEASE_PREFLIGHT_LOG || true)"
if [[ -n "$preflight_log" && -s "$(resolve_path "$preflight_log")" ]]; then
  log_text="$(cat "$(resolve_path "$preflight_log")")"
  log_text_lower="$(lowercase "$log_text")"
  for expected in \
    "Release APK signature verified by apksigner." \
    "Release AAB signature verified by jarsigner." \
    "Signed artifact check passed." \
    "Merged release manifest check passed." \
    "Release artifact debug-resource check passed." \
    "Release lint warning allowlist check passed." \
    "Release preflight passed."; do
    if [[ "$log_text" != *"$expected"* ]]; then
      errors+=("RELEASE_PREFLIGHT_LOG is missing: $expected")
    fi
  done
  source_line="$(printf '%s\n' "$log_text" | grep -E '^Source state:' | tail -1 || true)"
  if [[ -z "$source_line" ]]; then
    errors+=("RELEASE_PREFLIGHT_LOG is missing Source state line")
  else
    require_log_field_match RELEASE_BRANCH "$(get_value RELEASE_BRANCH || true)" "$(log_field_value "$source_line" branch)"
    require_log_field_match RELEASE_COMMIT "$(get_value RELEASE_COMMIT || true)" "$(log_field_value "$source_line" commit)"
    require_log_field_match RELEASE_DIRTY_FILES "$(get_value RELEASE_DIRTY_FILES || true)" "$(log_field_value "$source_line" dirtyFiles)"
  fi

  metadata_line="$(printf '%s\n' "$log_text" | grep -E '^Release metadata:' | tail -1 || true)"
  if [[ -z "$metadata_line" ]]; then
    errors+=("RELEASE_PREFLIGHT_LOG is missing Release metadata line")
  else
    require_log_field_match RELEASE_APPLICATION_ID "$(get_value RELEASE_APPLICATION_ID || true)" "$(log_field_value "$metadata_line" applicationId)"
    require_log_field_match RELEASE_VERSION_CODE "$(get_value RELEASE_VERSION_CODE || true)" "$(log_field_value "$metadata_line" versionCode)"
    require_log_field_match RELEASE_VERSION_NAME "$(get_value RELEASE_VERSION_NAME || true)" "$(log_field_value "$metadata_line" versionName)"
  fi

  sdk_line="$(printf '%s\n' "$log_text" | grep -E '^Release SDK:' | tail -1 || true)"
  if [[ -z "$sdk_line" ]]; then
    errors+=("RELEASE_PREFLIGHT_LOG is missing Release SDK line")
  else
    require_log_field_match RELEASE_TARGET_SDK "$(get_value RELEASE_TARGET_SDK || true)" "$(log_field_value "$sdk_line" targetSdk)"
  fi

  if [[ -n "$apk_sha" && "$log_text_lower" != *"$(lowercase "$apk_sha")"* ]]; then
    errors+=("RELEASE_PREFLIGHT_LOG does not contain RELEASE_APK_SHA256: $apk_sha")
  fi
  if [[ -n "$aab_sha" && "$log_text_lower" != *"$(lowercase "$aab_sha")"* ]]; then
    errors+=("RELEASE_PREFLIGHT_LOG does not contain RELEASE_AAB_SHA256: $aab_sha")
  fi
  if [[ -n "$aab_signer_sha" && "$log_text_lower" != *"$(lowercase "$aab_signer_sha")"* ]]; then
    errors+=("RELEASE_PREFLIGHT_LOG does not contain RELEASE_AAB_SIGNER_SHA256: $aab_signer_sha")
  fi
  lint_summary_line="$(printf '%s\n' "$log_text" | grep -E '^Release lint summary: errors=[0-9]+, warnings=[0-9]+$' | tail -1 || true)"
  if [[ -z "$lint_summary_line" ]]; then
    errors+=("RELEASE_PREFLIGHT_LOG is missing Release lint summary line")
  else
    lint_error_count="$(log_field_value "$lint_summary_line" errors)"
    if [[ "$lint_error_count" != "0" ]]; then
      errors+=("RELEASE_PREFLIGHT_LOG lint error count must be 0, got '${lint_error_count:-missing}'")
    fi
    require_log_field_match RELEASE_LINT_WARNING_COUNT "$(get_value RELEASE_LINT_WARNING_COUNT || true)" "$(log_field_value "$lint_summary_line" warnings)"
  fi
fi

if ((${#warnings[@]} > 0)); then
  printf 'Warnings:\n'
  printf ' - %s\n' "${warnings[@]}"
fi

if ((${#errors[@]} > 0)); then
  printf 'Android launch evidence check failed:\n' >&2
  printf ' - %s\n' "${errors[@]}" >&2
  exit 1
fi

echo "Android launch evidence check passed."
