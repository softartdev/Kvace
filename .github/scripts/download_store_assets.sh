#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/version.properties"
release_tag="${1:?Release tag is required (for example, v1.0.0)}"
destination="${2:-$repo_root/build/distribution/release-assets}"
repository="${GITHUB_REPOSITORY:-softartdev/Kvace}"
expected_tag="v$VERSION_NAME"
archive_name="Kvace-$VERSION_NAME-store-assets.zip"
sidecar_name="$archive_name.sha256"
download_dir="$(mktemp -d "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/kvace-store-assets-download.XXXXXX")"
extract_dir="$(mktemp -d "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/kvace-store-assets-extract.XXXXXX")"
trap 'rm -rf "$download_dir" "$extract_dir"' EXIT

[[ "$release_tag" == "$expected_tag" ]] || { echo "Release tag $release_tag does not match version.properties ($expected_tag)" >&2; exit 1; }
[[ ! -e "$destination" ]] || { echo "Refusing to replace existing asset directory: $destination" >&2; exit 1; }
for command in gh jq python3; do
  command -v "$command" >/dev/null || { echo "$command is required" >&2; exit 1; }
done

release_json="$(gh api "repos/$repository/releases/tags/$release_tag")"
[[ "$(jq -r '.tag_name' <<< "$release_json")" == "$release_tag" ]] || { echo "Release tag lookup mismatch" >&2; exit 1; }
[[ "$(jq -r '.draft' <<< "$release_json")" == true ]] || { echo "Release $release_tag must still be a draft while production workflows consume store assets" >&2; exit 1; }
[[ "$(jq --arg name "$archive_name" '[.assets[] | select(.name == $name)] | length' <<< "$release_json")" == 1 ]] || { echo "Draft release must contain exactly one $archive_name asset" >&2; exit 1; }
[[ "$(jq --arg name "$sidecar_name" '[.assets[] | select(.name == $name)] | length' <<< "$release_json")" == 1 ]] || { echo "Draft release must contain exactly one $sidecar_name asset" >&2; exit 1; }

gh release download "$release_tag" --repo "$repository" --pattern "$archive_name" --pattern "$sidecar_name" --dir "$download_dir"
[[ "$(find "$download_dir" -maxdepth 1 -type f | wc -l | tr -d ' ')" == 2 ]] || { echo "Unexpected store asset download set" >&2; exit 1; }

expected_hash="$(awk -v name="$archive_name" '$2 == name {print $1}' "$download_dir/$sidecar_name")"
[[ "$expected_hash" =~ ^[0-9a-f]{64}$ ]] || { echo "Invalid SHA-256 sidecar for $archive_name" >&2; exit 1; }
if command -v sha256sum >/dev/null; then
  actual_hash="$(sha256sum "$download_dir/$archive_name" | awk '{print $1}')"
else
  actual_hash="$(shasum -a 256 "$download_dir/$archive_name" | awk '{print $1}')"
fi
[[ "$actual_hash" == "$expected_hash" ]] || { echo "Store asset archive SHA-256 mismatch" >&2; exit 1; }

python3 - "$download_dir/$archive_name" "$extract_dir" <<'PY'
from pathlib import Path, PurePosixPath
import stat
import sys
from zipfile import ZipFile

archive, destination = map(Path, sys.argv[1:])
with ZipFile(archive) as package:
    infos = package.infolist()
    names = [info.filename for info in infos]
    assert names and len(names) == len(set(names)), "Archive contains duplicate entries"
    assert len(infos) == 32, f"Expected exactly 32 archive files, found {len(infos)}"
    assert sum(info.file_size for info in infos) < 100_000_000, "Archive expands beyond the expected size limit"
    for info in infos:
        name = info.filename
        path = PurePosixPath(name)
        assert not path.is_absolute() and ".." not in path.parts and "" not in path.parts, f"Unsafe archive entry: {name}"
        mode = (info.external_attr >> 16) & 0o170000
        assert mode != stat.S_IFLNK and not info.is_dir(), f"Archive entries must be regular files: {name}"
    package.extractall(destination)
PY

cmp "$extract_dir/manifest.tsv" "$repo_root/distribution/screenshots/manifest.tsv"
cmp "$extract_dir/metadata.env" "$repo_root/distribution/metadata.env"
[[ "$(wc -l < "$extract_dir/asset-info.env" | tr -d ' ')" == 5 ]]
grep -qx 'FORMAT_VERSION=1' "$extract_dir/asset-info.env"
grep -qx 'APP_NAME=Kvace' "$extract_dir/asset-info.env"
grep -qx "VERSION_NAME=${expected_tag#v}" "$extract_dir/asset-info.env"
grep -qx "RELEASE_TAG=$expected_tag" "$extract_dir/asset-info.env"
grep -qx 'SCREENSHOT_COUNT=28' "$extract_dir/asset-info.env"
(
  cd "$extract_dir"
  if command -v sha256sum >/dev/null; then
    sha256sum --check --strict SHA256SUMS
  else
    shasum -a 256 --check SHA256SUMS
  fi
)
"$repo_root/.github/scripts/validate_distribution.sh" --screenshots-dir "$extract_dir/screenshots"

mkdir -p "$(dirname "$destination")"
mv "$extract_dir" "$destination"
echo "STORE_ASSETS_DIR=$destination"
