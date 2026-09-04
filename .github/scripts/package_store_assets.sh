#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/version.properties"
screenshots_root="${1:-$repo_root/build/distribution/screenshots}"
output_dir="${2:-$repo_root/build/release}"
[[ "$screenshots_root" = /* ]] || screenshots_root="$repo_root/$screenshots_root"
[[ "$output_dir" = /* ]] || output_dir="$repo_root/$output_dir"
archive_name="Kvace-$VERSION_NAME-store-assets.zip"
archive="$output_dir/$archive_name"
sidecar="$archive.sha256"
stage_dir="$(mktemp -d "${TMPDIR:-/tmp}/kvace-store-assets-package.XXXXXX")"
archive_work_dir="$(mktemp -d "${TMPDIR:-/tmp}/kvace-store-assets-archive.XXXXXX")"
temp_archive="$archive_work_dir/$archive_name"
trap 'rm -rf "$stage_dir" "$archive_work_dir"' EXIT

command -v zip >/dev/null || { echo "zip is required" >&2; exit 1; }
"$repo_root/.github/scripts/validate_distribution.sh" --screenshots-dir "$screenshots_root"

mkdir -p "$stage_dir/screenshots" "$output_dir"
cp -R "$screenshots_root/." "$stage_dir/screenshots/"
cp "$repo_root/distribution/screenshots/manifest.tsv" "$stage_dir/manifest.tsv"
cp "$repo_root/distribution/metadata.env" "$stage_dir/metadata.env"
printf '%s\n' \
  'FORMAT_VERSION=1' \
  'APP_NAME=Kvace' \
  "VERSION_NAME=$VERSION_NAME" \
  "RELEASE_TAG=v$VERSION_NAME" \
  'SCREENSHOT_COUNT=28' > "$stage_dir/asset-info.env"

checksum_file() {
  if command -v sha256sum >/dev/null; then
    sha256sum "$1"
  else
    shasum -a 256 "$1"
  fi
}

(
  cd "$stage_dir"
  while IFS= read -r file; do
    checksum_file "$file"
  done < <(find screenshots -type f -name '*.png' -print | LC_ALL=C sort; printf '%s\n' asset-info.env manifest.tsv metadata.env) > SHA256SUMS
  find . -type f -exec chmod 0644 {} +
  find . -exec touch -t 198001010000 {} +
  find . -type f -print | LC_ALL=C sort | zip -0 -X -q "$temp_archive" -@
)

checksum_file "$temp_archive" | awk -v name="$archive_name" '{print $1 "  " name}' > "$temp_archive.sha256"
mv "$temp_archive" "$archive"
mv "$temp_archive.sha256" "$sidecar"
echo "Packaged $archive"
echo "Wrote $sidecar"
