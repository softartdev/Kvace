#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

ipa_path="${1:?Path to the release IPA is required}"
[[ -f "$ipa_path" ]] || { echo "IPA not found: $ipa_path" >&2; exit 1; }

source version.properties
source distribution/metadata.env

bundle_id="com.softartdev.kvace"
api_root="https://api.appstoreconnect.apple.com"
locale="$DEFAULT_LANGUAGE"
runner_temp="${RUNNER_TEMP:?RUNNER_TEMP is required}"

for name in \
  APP_STORE_CONNECT_KEY_ID \
  APP_STORE_CONNECT_ISSUER_ID \
  APP_STORE_CONNECT_PRIVATE_KEY \
  APP_REVIEW_CONTACT_FIRST_NAME \
  APP_REVIEW_CONTACT_LAST_NAME \
  APP_REVIEW_CONTACT_PHONE \
  APP_REVIEW_CONTACT_EMAIL; do
  [[ -n "${!name:-}" ]] || { echo "$name is required" >&2; exit 1; }
done
for command in curl jq openssl python3 xcrun md5; do
  command -v "$command" >/dev/null || { echo "$command is required" >&2; exit 1; }
done

auth_dir="$runner_temp/app-store-connect/private_keys"
key_path="$auth_dir/AuthKey_${APP_STORE_CONNECT_KEY_ID}.p8"
signature_der="$runner_temp/kvace-asc-signature.der"
signature_raw="$runner_temp/kvace-asc-signature.raw"
upload_part="$runner_temp/kvace-asc-upload.part"
mkdir -p "$auth_dir"
printf '%s\n' "$APP_STORE_CONNECT_PRIVATE_KEY" > "$key_path"
chmod 600 "$key_path"
export API_PRIVATE_KEYS_DIR="$auth_dir"

cleanup() {
  rm -f "$key_path" "$signature_der" "$signature_raw" "$upload_part"
}
trap cleanup EXIT

base64_url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

app_store_token() {
  local issued_at expires_at header payload unsigned signature
  issued_at="$(date +%s)"
  expires_at="$((issued_at + 1200))"
  header="$(jq -nc --arg kid "$APP_STORE_CONNECT_KEY_ID" '{alg:"ES256",kid:$kid,typ:"JWT"}' | base64_url)"
  payload="$(jq -nc \
    --arg iss "$APP_STORE_CONNECT_ISSUER_ID" \
    --argjson iat "$issued_at" \
    --argjson exp "$expires_at" \
    '{iss:$iss,iat:$iat,exp:$exp,aud:"appstoreconnect-v1"}' | base64_url)"
  unsigned="$header.$payload"
  printf '%s' "$unsigned" | openssl dgst -binary -sha256 -sign "$key_path" > "$signature_der"
  python3 - "$signature_der" "$signature_raw" <<'PY'
from pathlib import Path
import sys

data = Path(sys.argv[1]).read_bytes()
index = 0

def take_length():
    global index
    value = data[index]
    index += 1
    if value < 0x80:
        return value
    count = value & 0x7f
    value = int.from_bytes(data[index:index + count], "big")
    index += count
    return value

assert data[index] == 0x30
index += 1
take_length()
parts = []
for _ in range(2):
    assert data[index] == 0x02
    index += 1
    length = take_length()
    value = data[index:index + length]
    index += length
    value = value.lstrip(b"\x00")
    assert len(value) <= 32
    parts.append(value.rjust(32, b"\x00"))
Path(sys.argv[2]).write_bytes(b"".join(parts))
PY
  signature="$(openssl base64 -A -in "$signature_raw" | tr '+/' '-_' | tr -d '=')"
  printf '%s.%s' "$unsigned" "$signature"
}

asc_request() {
  local method="$1" path="$2" body="${3:-}" token
  token="$(app_store_token)"
  if [[ -n "$body" ]]; then
    curl --globoff --fail-with-body -sS \
      -X "$method" \
      -H "Authorization: Bearer $token" \
      -H 'Content-Type: application/json' \
      -d "$body" \
      "$api_root$path"
  else
    curl --globoff --fail-with-body -sS \
      -X "$method" \
      -H "Authorization: Bearer $token" \
      "$api_root$path"
  fi
}

markdown_section() {
  awk -v heading="## $1" '
    $0 == heading { found = 1; next }
    found && /^## / { exit }
    found { lines[++count] = $0 }
    END {
      first = 1
      while (first <= count && lines[first] == "") first++
      last = count
      while (last >= first && lines[last] == "") last--
      for (i = first; i <= last; i++) print lines[i]
    }
  ' distribution/listing-en.md
}

upsert_app_info_localization() {
  local app_id="$1" subtitle app_infos app_info_id localizations localization_id payload
  subtitle="$(markdown_section 'App Store subtitle')"
  app_infos="$(asc_request GET "/v1/apps/$app_id/appInfos?limit=200")"
  app_info_id="$(jq -r \
    '[.data[] | select(.attributes.appStoreState == "PREPARE_FOR_SUBMISSION")][0].id // .data[0].id // empty' \
    <<< "$app_infos")"
  [[ -n "$app_info_id" ]] || { echo "No editable App Info resource found" >&2; exit 1; }
  localizations="$(asc_request GET "/v1/appInfos/$app_info_id/appInfoLocalizations?filter[locale]=$locale&limit=2")"
  localization_id="$(jq -r '.data[0].id // empty' <<< "$localizations")"
  if [[ -n "$localization_id" ]]; then
    payload="$(jq -nc \
      --arg id "$localization_id" \
      --arg subtitle "$subtitle" \
      --arg privacy "$PRIVACY_URL" \
      '{data:{type:"appInfoLocalizations",id:$id,attributes:{subtitle:$subtitle,privacyPolicyUrl:$privacy}}}')"
    asc_request PATCH "/v1/appInfoLocalizations/$localization_id" "$payload" >/dev/null
  else
    payload="$(jq -nc \
      --arg appInfo "$app_info_id" \
      --arg locale "$locale" \
      --arg name "$APP_NAME" \
      --arg subtitle "$subtitle" \
      --arg privacy "$PRIVACY_URL" \
      '{data:{type:"appInfoLocalizations",attributes:{locale:$locale,name:$name,subtitle:$subtitle,privacyPolicyUrl:$privacy},relationships:{appInfo:{data:{type:"appInfos",id:$appInfo}}}}}')"
    asc_request POST '/v1/appInfoLocalizations' "$payload" >/dev/null
  fi
}

upsert_version_localization() {
  local version_id="$1" description keywords whats_new localizations localization_id payload attributes
  description="$(markdown_section 'Full description')"
  keywords="$(markdown_section 'App Store keywords')"
  whats_new="$(markdown_section "Release notes $VERSION_NAME")"
  attributes="$(jq -nc \
    --arg description "$description" \
    --arg keywords "$keywords" \
    --arg support "$SUPPORT_URL" \
    --arg marketing "$MARKETING_URL" \
    '{description:$description,keywords:$keywords,supportUrl:$support,marketingUrl:$marketing}')"
  if [[ "$VERSION_NAME" != "1.0.0" ]]; then
    attributes="$(jq -c --arg whatsNew "$whats_new" '. + {whatsNew:$whatsNew}' <<< "$attributes")"
  fi
  localizations="$(asc_request GET "/v1/appStoreVersions/$version_id/appStoreVersionLocalizations?filter[locale]=$locale&limit=2")"
  localization_id="$(jq -r '.data[0].id // empty' <<< "$localizations")"
  if [[ -n "$localization_id" ]]; then
    payload="$(jq -nc \
      --arg id "$localization_id" \
      --argjson attributes "$attributes" \
      '{data:{type:"appStoreVersionLocalizations",id:$id,attributes:$attributes}}')"
    asc_request PATCH "/v1/appStoreVersionLocalizations/$localization_id" "$payload" >/dev/null
  else
    payload="$(jq -nc \
      --arg version "$version_id" \
      --arg locale "$locale" \
      --argjson attributes "$attributes" \
      '{data:{type:"appStoreVersionLocalizations",attributes:($attributes + {locale:$locale}),relationships:{appStoreVersion:{data:{type:"appStoreVersions",id:$version}}}}}')"
    localizations="$(asc_request POST '/v1/appStoreVersionLocalizations' "$payload")"
    localization_id="$(jq -r '.data.id' <<< "$localizations")"
  fi
  printf '%s' "$localization_id"
}

upsert_review_detail() {
  local version_id="$1" notes linkage detail_id payload attributes
  notes="$(markdown_section 'App Review notes')"
  attributes="$(jq -nc \
    --arg first "$APP_REVIEW_CONTACT_FIRST_NAME" \
    --arg last "$APP_REVIEW_CONTACT_LAST_NAME" \
    --arg phone "$APP_REVIEW_CONTACT_PHONE" \
    --arg email "$APP_REVIEW_CONTACT_EMAIL" \
    --arg notes "$notes" \
    '{contactFirstName:$first,contactLastName:$last,contactPhone:$phone,contactEmail:$email,demoAccountRequired:false,notes:$notes}')"
  linkage="$(asc_request GET "/v1/appStoreVersions/$version_id/relationships/appStoreReviewDetail")"
  detail_id="$(jq -r '.data.id // empty' <<< "$linkage")"
  if [[ -n "$detail_id" ]]; then
    payload="$(jq -nc --arg id "$detail_id" --argjson attributes "$attributes" \
      '{data:{type:"appStoreReviewDetails",id:$id,attributes:$attributes}}')"
    asc_request PATCH "/v1/appStoreReviewDetails/$detail_id" "$payload" >/dev/null
  else
    payload="$(jq -nc \
      --arg version "$version_id" \
      --argjson attributes "$attributes" \
      '{data:{type:"appStoreReviewDetails",attributes:$attributes,relationships:{appStoreVersion:{data:{type:"appStoreVersions",id:$version}}}}}')"
    asc_request POST '/v1/appStoreReviewDetails' "$payload" >/dev/null
  fi
}

upload_screenshot_set() {
  local localization_id="$1" display_type="$2" directory="$3"
  local sets set_id payload response screenshot screenshot_id file_size checksum operation
  local offset length method url ordered_data state_response states failed_count incomplete_count
  local -a headers

  sets="$(asc_request GET "/v1/appStoreVersionLocalizations/$localization_id/appScreenshotSets?limit=200")"
  while IFS= read -r set_id; do
    [[ -z "$set_id" ]] || asc_request DELETE "/v1/appScreenshotSets/$set_id" >/dev/null
  done < <(jq -r --arg type "$display_type" '.data[] | select(.attributes.screenshotDisplayType == $type) | .id' <<< "$sets")

  payload="$(jq -nc \
    --arg localization "$localization_id" \
    --arg displayType "$display_type" \
    '{data:{type:"appScreenshotSets",attributes:{screenshotDisplayType:$displayType},relationships:{appStoreVersionLocalization:{data:{type:"appStoreVersionLocalizations",id:$localization}}}}}')"
  response="$(asc_request POST '/v1/appScreenshotSets' "$payload")"
  set_id="$(jq -r '.data.id' <<< "$response")"
  ordered_data='[]'

  for screenshot in "$directory"/*.png; do
    [[ -f "$screenshot" ]] || { echo "No screenshots found in $directory" >&2; exit 1; }
    file_size="$(stat -f%z "$screenshot")"
    payload="$(jq -nc \
      --arg set "$set_id" \
      --arg name "$(basename "$screenshot")" \
      --argjson size "$file_size" \
      '{data:{type:"appScreenshots",attributes:{fileName:$name,fileSize:$size},relationships:{appScreenshotSet:{data:{type:"appScreenshotSets",id:$set}}}}}')"
    response="$(asc_request POST '/v1/appScreenshots' "$payload")"
    screenshot_id="$(jq -r '.data.id' <<< "$response")"

    while IFS= read -r operation; do
      offset="$(jq -r '.offset' <<< "$operation")"
      length="$(jq -r '.length' <<< "$operation")"
      method="$(jq -r '.method' <<< "$operation")"
      url="$(jq -r '.url' <<< "$operation")"
      python3 - "$screenshot" "$offset" "$length" "$upload_part" <<'PY'
from pathlib import Path
import sys

with Path(sys.argv[1]).open("rb") as source:
    source.seek(int(sys.argv[2]))
    Path(sys.argv[4]).write_bytes(source.read(int(sys.argv[3])))
PY
      headers=()
      while IFS=$'\t' read -r header_name header_value; do
        headers+=(-H "$header_name: $header_value")
      done < <(jq -r '.requestHeaders[] | [.name, .value] | @tsv' <<< "$operation")
      curl --fail-with-body -sS -X "$method" "${headers[@]}" --data-binary "@$upload_part" "$url" >/dev/null
    done < <(jq -c '.data.attributes.uploadOperations[]' <<< "$response")

    checksum="$(md5 -q "$screenshot")"
    payload="$(jq -nc \
      --arg id "$screenshot_id" \
      --arg checksum "$checksum" \
      '{data:{type:"appScreenshots",id:$id,attributes:{uploaded:true,sourceFileChecksum:$checksum}}}')"
    asc_request PATCH "/v1/appScreenshots/$screenshot_id" "$payload" >/dev/null
    ordered_data="$(jq -c --arg id "$screenshot_id" '. + [{type:"appScreenshots",id:$id}]' <<< "$ordered_data")"
  done

  for _ in $(seq 1 60); do
    state_response="$(asc_request GET "/v1/appScreenshotSets/$set_id/appScreenshots?limit=50")"
    states="$(jq -r '.data[].attributes.assetDeliveryState.state // "UNKNOWN"' <<< "$state_response")"
    failed_count="$(grep -Ec 'FAILED|ERROR' <<< "$states" || true)"
    [[ "$failed_count" == 0 ]] || { echo "App Store screenshot processing failed: $states" >&2; exit 1; }
    incomplete_count="$(grep -Evc '^COMPLETE$' <<< "$states" || true)"
    if [[ "$incomplete_count" == 0 ]]; then
      payload="$(jq -nc --argjson data "$ordered_data" '{data:$data}')"
      asc_request PATCH "/v1/appScreenshotSets/$set_id/relationships/appScreenshots" "$payload" >/dev/null
      return
    fi
    sleep 5
  done
  echo "Timed out waiting for App Store screenshots to finish processing" >&2
  exit 1
}

wait_for_processed_build() {
  local app_id="$1" timeout="${APP_STORE_BUILD_WAIT_SECONDS:-1800}"
  local deadline response build_id state
  deadline="$(( $(date +%s) + timeout ))"
  while (( $(date +%s) < deadline )); do
    response="$(asc_request GET "/v1/builds?filter[app]=$app_id&filter[version]=$VERSION_CODE&sort=-uploadedDate&limit=1")"
    build_id="$(jq -r '.data[0].id // empty' <<< "$response")"
    state="$(jq -r '.data[0].attributes.processingState // empty' <<< "$response")"
    case "$state" in
      VALID) printf '%s' "$build_id"; return ;;
      FAILED|INVALID) echo "Uploaded App Store build entered $state" >&2; exit 1 ;;
    esac
    sleep 30
  done
  echo "Timed out waiting for App Store Connect to process build $VERSION_CODE" >&2
  exit 1
}

submit_for_review() {
  local app_id="$1" version_id="$2" submissions submission_id state items payload
  submissions="$(asc_request GET "/v1/apps/$app_id/reviewSubmissions?include=appStoreVersionForReview&limit=200")"
  submission_id="$(jq -r --arg version "$version_id" \
    '.data[] | select(.relationships.appStoreVersionForReview.data.id == $version) | .id' \
    <<< "$submissions" | head -n 1)"
  if [[ -n "$submission_id" ]]; then
    state="$(jq -r --arg id "$submission_id" '.data[] | select(.id == $id) | .attributes.state' <<< "$submissions")"
    if [[ "$state" != READY_FOR_REVIEW ]]; then
      echo "App Store version $VERSION_NAME is already in review submission state $state"
      return
    fi
  else
    payload="$(jq -nc --arg app "$app_id" \
      '{data:{type:"reviewSubmissions",attributes:{platform:"IOS"},relationships:{app:{data:{type:"apps",id:$app}}}}}')"
    submissions="$(asc_request POST '/v1/reviewSubmissions' "$payload")"
    submission_id="$(jq -r '.data.id' <<< "$submissions")"
  fi

  items="$(asc_request GET "/v1/reviewSubmissions/$submission_id/items?include=appStoreVersion&limit=50")"
  if ! jq -e --arg version "$version_id" \
    '.data[] | select(.relationships.appStoreVersion.data.id == $version)' \
    <<< "$items" >/dev/null; then
    payload="$(jq -nc \
      --arg submission "$submission_id" \
      --arg version "$version_id" \
      '{data:{type:"reviewSubmissionItems",relationships:{reviewSubmission:{data:{type:"reviewSubmissions",id:$submission}},appStoreVersion:{data:{type:"appStoreVersions",id:$version}}}}}')"
    asc_request POST '/v1/reviewSubmissionItems' "$payload" >/dev/null
  fi

  payload="$(jq -nc --arg id "$submission_id" \
    '{data:{type:"reviewSubmissions",id:$id,attributes:{submitted:true}}}')"
  asc_request PATCH "/v1/reviewSubmissions/$submission_id" "$payload" >/dev/null
}

echo "Uploading Kvace $VERSION_NAME ($VERSION_CODE) to App Store Connect"
xcrun altool \
  --upload-app \
  --type ios \
  --file "$ipa_path" \
  --apiKey "$APP_STORE_CONNECT_KEY_ID" \
  --apiIssuer "$APP_STORE_CONNECT_ISSUER_ID"

apps="$(asc_request GET "/v1/apps?filter[bundleId]=$bundle_id&limit=2")"
[[ "$(jq '.data | length' <<< "$apps")" == 1 ]] || {
  echo "Expected exactly one App Store Connect app for $bundle_id" >&2
  exit 1
}
app_id="$(jq -r '.data[0].id' <<< "$apps")"

versions="$(asc_request GET "/v1/apps/$app_id/appStoreVersions?filter[platform]=IOS&filter[versionString]=$VERSION_NAME&limit=2")"
version_count="$(jq '.data | length' <<< "$versions")"
if [[ "$version_count" == 0 ]]; then
  payload="$(jq -nc \
    --arg app "$app_id" \
    --arg version "$VERSION_NAME" \
    --arg copyright "$COPYRIGHT" \
    '{data:{type:"appStoreVersions",attributes:{platform:"IOS",versionString:$version,copyright:$copyright,releaseType:"AFTER_APPROVAL"},relationships:{app:{data:{type:"apps",id:$app}}}}}')"
  versions="$(asc_request POST '/v1/appStoreVersions' "$payload")"
  version_id="$(jq -r '.data.id' <<< "$versions")"
elif [[ "$version_count" == 1 ]]; then
  version_id="$(jq -r '.data[0].id' <<< "$versions")"
  payload="$(jq -nc --arg id "$version_id" --arg copyright "$COPYRIGHT" \
    '{data:{type:"appStoreVersions",id:$id,attributes:{copyright:$copyright,releaseType:"AFTER_APPROVAL"}}}')"
  asc_request PATCH "/v1/appStoreVersions/$version_id" "$payload" >/dev/null
else
  echo "Multiple App Store version records found for $VERSION_NAME" >&2
  exit 1
fi

upsert_app_info_localization "$app_id"
version_localization_id="$(upsert_version_localization "$version_id")"
upsert_review_detail "$version_id"
upload_screenshot_set "$version_localization_id" APP_IPHONE_67 distribution/screenshots/app-store/iphone
upload_screenshot_set "$version_localization_id" APP_IPAD_PRO_3GEN_129 distribution/screenshots/app-store/ipad

build_id="$(wait_for_processed_build "$app_id")"
payload="$(jq -nc --arg id "$build_id" '{data:{type:"builds",id:$id}}')"
asc_request PATCH "/v1/appStoreVersions/$version_id/relationships/build" "$payload" >/dev/null
submit_for_review "$app_id" "$version_id"

echo "Kvace $VERSION_NAME was attached to build $VERSION_CODE and submitted directly to App Review with release after approval."
