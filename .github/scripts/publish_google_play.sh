#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"
aab_path="${1:?Path to the release AAB is required}"
[[ -f "$aab_path" ]] || { echo "AAB not found: $aab_path" >&2; exit 1; }
: "${GOOGLE_PLAY_ACCESS_TOKEN:?GOOGLE_PLAY_ACCESS_TOKEN is required}"
command -v curl >/dev/null
command -v jq >/dev/null

package_name="com.softartdev.kvace"
api="https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$package_name"
auth_header="Authorization: Bearer $GOOGLE_PLAY_ACCESS_TOKEN"
committed=false

edit_id="$(curl --fail-with-body -sS -X POST -H "$auth_header" "$api/edits" | jq -r .id)"
cleanup() {
  if [[ "$committed" != true ]]; then
    curl -sS -X DELETE -H "$auth_header" "$api/edits/$edit_id" >/dev/null || true
  fi
}
trap cleanup EXIT

bundle="$(curl --fail-with-body -sS -X POST -H "$auth_header" -H 'Content-Type: application/octet-stream' --data-binary "@$aab_path" "https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/$package_name/edits/$edit_id/bundles?uploadType=media")"
version_code="$(jq -r .versionCode <<< "$bundle")"

source version.properties
[[ "$version_code" == "$VERSION_CODE" ]] || {
  echo "Uploaded version code $version_code does not match version.properties ($VERSION_CODE)" >&2
  exit 1
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

short_description="$(markdown_section 'Google Play short description')"
full_description="$(markdown_section 'Full description')"
curl --fail-with-body -sS -X PUT -H "$auth_header" -H 'Content-Type: application/json' \
  -d "$(jq -n --arg title Kvace --arg short "$short_description" --arg full "$full_description" '{title:$title,shortDescription:$short,fullDescription:$full}')" \
  "$api/edits/$edit_id/listings/en-US" >/dev/null

upload_screenshots() {
  local type="$1" directory="$2"
  curl --fail-with-body -sS -X DELETE -H "$auth_header" "$api/edits/$edit_id/listings/en-US/$type" >/dev/null || true
  for image in "$directory"/*.png; do
    curl --fail-with-body -sS -X POST -H "$auth_header" -H 'Content-Type: image/png' --data-binary "@$image" \
      "https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/$package_name/edits/$edit_id/listings/en-US/$type?uploadType=media" >/dev/null
  done
}
upload_screenshots phoneScreenshots distribution/screenshots/google-play/phone
upload_screenshots sevenInchScreenshots distribution/screenshots/google-play/tablet
upload_screenshots tenInchScreenshots distribution/screenshots/google-play/tablet

notes="$(markdown_section "Release notes $VERSION_NAME")"
release="$(jq -n --arg versionCode "$version_code" --arg notes "$notes" '{releases:[{versionCodes:[$versionCode],status:"completed",releaseNotes:[{language:"en-US",text:$notes}]}]}')"
curl --fail-with-body -sS -X PUT -H "$auth_header" -H 'Content-Type: application/json' -d "$release" "$api/edits/$edit_id/tracks/production" >/dev/null
curl --fail-with-body -sS -X POST -H "$auth_header" "$api/edits/$edit_id:validate" >/dev/null
curl --fail-with-body -sS -X POST -H "$auth_header" "$api/edits/$edit_id:commit" >/dev/null
committed=true
