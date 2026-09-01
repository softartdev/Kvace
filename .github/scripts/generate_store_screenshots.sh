#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
preview_file="app/shared/src/androidMain/kotlin/com/softartdev/kvace/preview/StoreScreenshotPreview.kt"
manifest="distribution/screenshots/manifest.tsv"
opaque_png_dir="$(mktemp -d /tmp/kvace-store-screenshots.XXXXXX)"
trap 'rmdir "$opaque_png_dir" 2>/dev/null || true' EXIT

command -v android >/dev/null || { echo "android CLI is required" >&2; exit 1; }
command -v magick >/dev/null || { echo "ImageMagick is required to produce opaque store PNGs" >&2; exit 1; }

while IFS=$'\t' read -r target order scenario composable width height _; do
  [[ -z "${target}" || "${target}" == \#* ]] && continue
  output="$repo_root/distribution/screenshots/$target/$order-$scenario.png"
  mkdir -p "$(dirname "$output")"
  android studio render-compose-preview --project=Kvace "$preview_file" "$composable" --output-image-file="$output"
  opaque_png="$opaque_png_dir/$order-$scenario.png"
  magick "$output" -background white -alpha remove -alpha off "PNG24:$opaque_png"
  mv "$opaque_png" "$output"
  [[ -s "$output" ]] || { echo "Missing screenshot: $output" >&2; exit 1; }
  actual="$(sips -g pixelWidth -g pixelHeight "$output" | awk '/pixel(Width|Height)/ {print $2}' | paste -sd x -)"
  [[ "$actual" == "$width"x"$height" ]] || { echo "Unexpected dimensions for $output: $actual" >&2; exit 1; }
done < "$repo_root/$manifest"
