#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"
: "${APPLE_TEAM_ID:?APPLE_TEAM_ID is required}"
: "${IOS_PROVISIONING_PROFILE_NAME:?IOS_PROVISIONING_PROFILE_NAME is required}"

./gradlew :app:shared:linkReleaseFrameworkIosArm64
xcodebuild \
  -project app/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -sdk iphoneos \
  -archivePath "$PWD/build/Kvace.xcarchive" \
  DEVELOPMENT_TEAM="$APPLE_TEAM_ID" \
  CODE_SIGN_STYLE=Manual \
  CODE_SIGN_IDENTITY="Apple Distribution" \
  PROVISIONING_PROFILE_SPECIFIER="$IOS_PROVISIONING_PROFILE_NAME" \
  archive

export_options="$RUNNER_TEMP/Kvace-ExportOptions.plist"
cp app/iosApp/ExportOptions.plist "$export_options"
plutil -replace teamID -string "$APPLE_TEAM_ID" "$export_options"
plutil -replace provisioningProfiles -json \
  "{\"com.softartdev.kvace\":\"$IOS_PROVISIONING_PROFILE_NAME\"}" \
  "$export_options"

xcodebuild \
  -exportArchive \
  -archivePath "$PWD/build/Kvace.xcarchive" \
  -exportOptionsPlist "$export_options" \
  -exportPath "$PWD/build/export"

shopt -s nullglob
ipas=(build/export/*.ipa)
[[ "${#ipas[@]}" -eq 1 ]] || { echo "Expected exactly one exported IPA" >&2; exit 1; }
mv "${ipas[0]}" build/Kvace.ipa
