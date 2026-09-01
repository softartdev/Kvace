#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/version.properties"
contract_version_name="$VERSION_NAME"
contract_version_code="$VERSION_CODE"
source "$repo_root/distribution/metadata.env"
[[ "$VERSION_NAME" == "$contract_version_name" ]]
[[ "$contract_version_name" == "1.0.0" && "$contract_version_code" == "1" ]]
for file in \
  LICENSE \
  CHANGELOG.md \
  docs/MANUAL_BUILD_INSTALL.md \
  docs/release/RELEASE_GUIDE.md \
  docs/release/RELEASE_CHECKLIST.md \
  distribution/metadata.env \
  distribution/listing-en.md \
  distribution/privacy-data.md \
  distribution/screenshots/manifest.tsv; do
  [[ -s "$repo_root/$file" ]] || { echo "Missing distribution file: $file" >&2; exit 1; }
done
grep -q '^## Google Play short description$' "$repo_root/distribution/listing-en.md"
grep -q '^## App Store subtitle$' "$repo_root/distribution/listing-en.md"
for url in "$PRIVACY_URL" "$SUPPORT_URL" "$MARKETING_URL" "$SOURCE_URL"; do
  [[ "$url" == https://* ]] || { echo "Distribution URL must use HTTPS: $url" >&2; exit 1; }
done

python3 - "$repo_root/distribution/listing-en.md" <<'PY'
from pathlib import Path
import sys

text = Path(sys.argv[1]).read_text()

def section(name: str) -> str:
    marker = f"## {name}\n"
    assert marker in text, f"Missing metadata section: {name}"
    value = text.split(marker, 1)[1].split("\n## ", 1)[0].strip()
    assert value, f"Empty metadata section: {name}"
    return value

assert len(section("Google Play short description")) <= 80
assert len(section("App Store subtitle")) <= 30
assert len(section("App Store keywords")) <= 100
PY

screenshot_count=0
while IFS=$'\t' read -r target order scenario _ width height _; do
  [[ -z "${target}" || "${target}" == \#* ]] && continue
  screenshot_count=$((screenshot_count + 1))
  screenshot="$repo_root/distribution/screenshots/$target/$order-$scenario.png"
  [[ -s "$screenshot" ]] || { echo "Missing screenshot: $screenshot" >&2; exit 1; }
  python3 - "$screenshot" "$width" "$height" <<'PY'
import struct, sys
with open(sys.argv[1], "rb") as image:
    assert image.read(8) == b"\x89PNG\r\n\x1a\n"
    assert image.read(4) == b"\x00\x00\x00\r"
    assert image.read(4) == b"IHDR"
    width, height, bit_depth, color_type, _, _, _ = struct.unpack(">IIBBBBB", image.read(13))
assert (width, height) == (int(sys.argv[2]), int(sys.argv[3]))
assert color_type not in (4, 6), "PNG must not contain alpha"
PY
done < "$repo_root/distribution/screenshots/manifest.tsv"
[[ "$screenshot_count" -eq 28 ]] || { echo "Expected 28 screenshots, found $screenshot_count" >&2; exit 1; }

actual_png_count="$(find "$repo_root/distribution/screenshots" -type f -name '*.png' | wc -l | tr -d ' ')"
[[ "$actual_png_count" -eq 28 ]] || { echo "Expected exactly 28 screenshot PNGs, found $actual_png_count" >&2; exit 1; }
