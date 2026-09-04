#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
preview_file="app/shared/src/androidMain/kotlin/com/softartdev/kvace/preview/StoreScreenshotPreview.kt"
manifest="$repo_root/distribution/screenshots/manifest.tsv"
output_root="${1:-$repo_root/build/distribution/screenshots}"
[[ "$output_root" = /* ]] || output_root="$repo_root/$output_root"
opaque_png_dir="$(mktemp -d "${TMPDIR:-/tmp}/kvace-store-screenshots.XXXXXX")"
trap 'rm -rf "$opaque_png_dir"' EXIT

command -v android >/dev/null || { echo "android CLI is required" >&2; exit 1; }
command -v magick >/dev/null || { echo "ImageMagick is required to produce opaque store PNGs" >&2; exit 1; }
command -v sips >/dev/null || { echo "sips is required to inspect rendered dimensions" >&2; exit 1; }
command -v python3 >/dev/null || { echo "python3 is required to bound preview renders" >&2; exit 1; }

render_preview() {
  local composable="$1" rendered="$2" attempt
  for attempt in 1 2 3; do
    rm -f "$rendered"
    if python3 - "$repo_root" "$preview_file" "$composable" "$rendered" <<'PY'
import subprocess
import sys

repo_root, preview_file, composable, output = sys.argv[1:]
subprocess.run(
    [
        "android", "studio", "render-compose-preview",
        "--project=Kvace", preview_file, composable,
        f"--output-image-file={output}",
    ],
    cwd=repo_root,
    check=True,
    timeout=90,
)
PY
    then
      return
    fi
    echo "Preview render attempt $attempt/3 failed for $composable" >&2
    sleep "$((attempt * 2))"
  done
  echo "Preview failed after three attempts: $composable" >&2
  exit 1
}

mkdir -p "$output_root"
while IFS=$'\t' read -r target order scenario composable width height _; do
  [[ -z "${target}" || "${target}" == \#* ]] && continue
  output="$output_root/$target/$order-$scenario.png"
  rendered_png="$opaque_png_dir/rendered.png"
  mkdir -p "$(dirname "$output")"
  render_preview "$composable" "$rendered_png"
  opaque_png="$opaque_png_dir/$order-$scenario.png"
  magick "$rendered_png" -background white -alpha remove -alpha off "PNG24:$opaque_png"
  mv "$opaque_png" "$output"
  [[ -s "$output" ]] || { echo "Missing screenshot: $output" >&2; exit 1; }
  actual="$(sips -g pixelWidth -g pixelHeight "$output" | awk '/pixel(Width|Height)/ {print $2}' | paste -sd x -)"
  [[ "$actual" == "$width"x"$height" ]] || { echo "Unexpected dimensions for $output: $actual" >&2; exit 1; }
done < "$manifest"

"$repo_root/.github/scripts/validate_distribution.sh" --screenshots-dir "$output_root"
echo "Generated and validated store screenshots in $output_root"
