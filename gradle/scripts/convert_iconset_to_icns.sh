#!/bin/bash

set -euo pipefail

if [[ $# -ne 2 ]]; then
    echo "Usage: $0 <source-1024-png> <output-icns>" >&2
    exit 64
fi

src="$1"
output="$2"

if [[ ! -f "$src" ]]; then
    echo "Source icon does not exist: $src" >&2
    exit 66
fi

if command -v magick >/dev/null 2>&1; then
    convert_cmd=(magick)
elif command -v convert >/dev/null 2>&1; then
    convert_cmd=(convert)
else
    echo "ImageMagick is required: install magick or convert" >&2
    exit 69
fi

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

iconset="$tmpdir/icon.iconset"
mkdir -p "$iconset"

sizes=(16 32 128 256 512)

# macOS app icons keep the solid rounded-square body inside a larger transparent canvas.
macos_icon_body_numerator=824
macos_icon_body_denominator=1024
macos_icon_corner_radius_numerator=226
macos_icon_corner_radius_denominator=1024

render_icon() {
    local target_size="$1"
    local output="$2"
    local inner_size radius
    local resized="$tmpdir/resized-${target_size}.png"
    local mask="$tmpdir/mask-${target_size}.png"
    local rounded="$tmpdir/rounded-${target_size}.png"

    inner_size=$(( (target_size * macos_icon_body_numerator + macos_icon_body_denominator / 2) / macos_icon_body_denominator ))
    radius=$(( (inner_size * macos_icon_corner_radius_numerator + macos_icon_corner_radius_denominator / 2) / macos_icon_corner_radius_denominator ))

    "${convert_cmd[@]}" "$src" -resize "${inner_size}x${inner_size}" "$resized"
    "${convert_cmd[@]}" -size "${inner_size}x${inner_size}" xc:none \
        -fill white \
        -draw "roundrectangle 0,0,$((inner_size - 1)),$((inner_size - 1)),$radius,$radius" \
        "$mask"
    "${convert_cmd[@]}" "$resized" "$mask" -alpha off -compose CopyOpacity -composite "$rounded"
    "${convert_cmd[@]}" -size "${target_size}x${target_size}" xc:none "$rounded" \
        -gravity center \
        -compose over \
        -composite \
        "$output"
}

for size in "${sizes[@]}"; do
    render_icon "$size" "$iconset/icon_${size}x${size}.png"
    render_icon "$((size * 2))" "$iconset/icon_${size}x${size}@2x.png"
done

mkdir -p "$(dirname "$output")"
iconutil -c icns "$iconset" -o "$output"
