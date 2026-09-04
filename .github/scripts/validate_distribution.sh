#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
screenshots_dir=""

if [[ $# -gt 0 ]]; then
  [[ "$1" == "--screenshots-dir" && $# -eq 2 ]] || {
    echo "Usage: $0 [--screenshots-dir <generated-screenshot-root>]" >&2
    exit 2
  }
  screenshots_dir="$2"
  [[ "$screenshots_dir" = /* ]] || screenshots_dir="$repo_root/$screenshots_dir"
fi

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
  docs/adr/0001-store-release-automation.md \
  docs/release/RELEASE_GUIDE.md \
  docs/release/RELEASE_CHECKLIST.md \
  docs/release/STORE_BOOTSTRAP.md \
  distribution/metadata.env \
  distribution/listing-en.md \
  distribution/privacy-data.md \
  distribution/screenshots/manifest.tsv \
  app/shared/src/androidMain/kotlin/com/softartdev/kvace/preview/StoreScreenshotPreview.kt; do
  [[ -s "$repo_root/$file" ]] || { echo "Missing distribution source file: $file" >&2; exit 1; }
done

grep -q '^## Google Play short description$' "$repo_root/distribution/listing-en.md"
grep -q '^## App Store subtitle$' "$repo_root/distribution/listing-en.md"
for url in "$PRIVACY_URL" "$SUPPORT_URL" "$MARKETING_URL" "$SOURCE_URL"; do
  [[ "$url" == https://* ]] || { echo "Distribution URL must use HTTPS: $url" >&2; exit 1; }
done

python3 - \
  "$repo_root/distribution/listing-en.md" \
  "$repo_root/distribution/screenshots/manifest.tsv" \
  "$repo_root/app/shared/src/androidMain/kotlin/com/softartdev/kvace/preview/StoreScreenshotPreview.kt" \
  "$repo_root/distribution/screenshots" \
  "$screenshots_dir" <<'PY'
from __future__ import annotations

import hashlib
from pathlib import Path
import re
import struct
import sys

listing_path, manifest_path, preview_path, source_root = map(Path, sys.argv[1:5])
generated_argument = sys.argv[5]
text = listing_path.read_text()

def section(name: str) -> str:
    marker = f"## {name}\n"
    assert marker in text, f"Missing metadata section: {name}"
    value = text.split(marker, 1)[1].split("\n## ", 1)[0].strip()
    assert value, f"Empty metadata section: {name}"
    return value

assert len(section("Google Play short description")) <= 80
assert len(section("App Store subtitle")) <= 30
assert len(section("App Store keywords")) <= 100

expected_targets = {
    "google-play/phone": (6, 1080, 1920),
    "google-play/tablet": (6, 1920, 1080),
    "app-store/iphone": (6, 1320, 2868),
    "app-store/ipad": (6, 2752, 2064),
    "desktop": (4, 2560, 1600),
}
preview_source = preview_path.read_text()
rows: list[tuple[str, str, str, str, int, int, str]] = []
seen_paths: set[str] = set()
seen_composables: set[str] = set()
target_counts = {target: 0 for target in expected_targets}

for line_number, line in enumerate(manifest_path.read_text().splitlines(), 1):
    if not line or line.startswith("#"):
        continue
    columns = line.split("\t")
    assert len(columns) == 7, f"Manifest line {line_number} must contain seven tab-separated fields"
    target, order, scenario, composable, width_text, height_text, alt_text = columns
    assert target in expected_targets, f"Unsupported screenshot target on line {line_number}: {target}"
    assert re.fullmatch(r"\d{2}", order), f"Invalid screenshot order on line {line_number}: {order}"
    assert re.fullmatch(r"[a-z0-9-]+", scenario), f"Invalid scenario on line {line_number}: {scenario}"
    assert re.fullmatch(r"[A-Za-z][A-Za-z0-9]+Preview", composable), f"Invalid composable on line {line_number}"
    width, height = int(width_text), int(height_text)
    expected_count, expected_width, expected_height = expected_targets[target]
    assert (width, height) == (expected_width, expected_height), f"Unexpected dimensions on line {line_number}"
    assert alt_text.strip() == alt_text and len(alt_text) >= 20, f"Missing or weak alt text on line {line_number}"
    relative = f"{target}/{order}-{scenario}.png"
    assert relative not in seen_paths, f"Duplicate screenshot path: {relative}"
    assert composable not in seen_composables, f"Duplicate screenshot composable: {composable}"
    assert re.search(rf"\bfun\s+{re.escape(composable)}\s*\(", preview_source), f"Missing preview function: {composable}"
    seen_paths.add(relative)
    seen_composables.add(composable)
    target_counts[target] += 1
    rows.append((target, order, scenario, composable, width, height, alt_text))

assert len(rows) == 28, f"Expected 28 screenshot manifest rows, found {len(rows)}"
for target, (expected_count, _, _) in expected_targets.items():
    assert target_counts[target] == expected_count, f"Expected {expected_count} manifest rows for {target}, found {target_counts[target]}"

source_pngs = sorted(source_root.rglob("*.png"))
assert not source_pngs, "Generated screenshot PNGs must not be stored under distribution/screenshots"

if generated_argument:
    generated_root = Path(generated_argument)
    assert generated_root.is_dir(), f"Screenshot directory does not exist: {generated_root}"
    actual = {str(path.relative_to(generated_root)) for path in generated_root.rglob("*.png")}
    assert actual == seen_paths, f"Screenshot file set differs from manifest; missing={sorted(seen_paths - actual)}, extra={sorted(actual - seen_paths)}"
    hashes: set[str] = set()
    for target, order, scenario, _, width, height, _ in rows:
        path = generated_root / target / f"{order}-{scenario}.png"
        data = path.read_bytes()
        assert len(data) > 1024, f"Screenshot content is unexpectedly small: {path}"
        assert data[:8] == b"\x89PNG\r\n\x1a\n", f"Not a PNG: {path}"
        assert data[12:16] == b"IHDR", f"PNG has no IHDR chunk: {path}"
        actual_width, actual_height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(">IIBBBBB", data[16:29])
        assert (actual_width, actual_height) == (width, height), f"Unexpected dimensions for {path}: {actual_width}x{actual_height}"
        assert bit_depth == 8 and color_type in (0, 2, 3), f"Screenshot must be an opaque 8-bit PNG: {path}"
        assert compression == 0 and filtering == 0 and interlace in (0, 1), f"Unsupported PNG encoding: {path}"
        assert data.endswith(b"IEND\xaeB`\x82"), f"PNG is incomplete: {path}"
        digest = hashlib.sha256(data).hexdigest()
        assert digest not in hashes, f"Duplicate screenshot content: {path}"
        hashes.add(digest)

suffix = f" and {len(rows)} generated screenshots in {generated_argument}" if generated_argument else " without generated PNGs"
print(f"Validated distribution source contract ({len(rows)} manifest rows){suffix}")
PY
