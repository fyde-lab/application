#!/usr/bin/env bash
# Regenerates design/current/ from the code: renders every ScreenshotFixtures
# entry at Compact, Medium and Expanded through ScreenshotsJvmTest (English,
# 2x), replaces current/screens/ with them, rebuilds the contact sheets and
# checks that every screen has a catalogue row in current/design-system.md.
#
# Run from anywhere: design/capture.sh. Starts from a clean environment (a
# nested nix-shell overflows the linker's argument list) and loads the
# repo's shell.nix itself.
set -euo pipefail

design="$(cd "$(dirname "$0")" && pwd)"
repo="$(dirname "$design")"
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT

cd "$repo"
env -i HOME="$HOME" USER="${USER:-$(id -un)}" TERM="${TERM:-dumb}" \
    PATH="/run/current-system/sw/bin:$HOME/.nix-profile/bin" NIX_PATH="${NIX_PATH:-}" \
    FYDE_SCREENSHOTS_DIR="$out" FYDE_SCREENSHOTS_SCALE=2 \
    nix-shell shell.nix --run \
    "./gradlew --no-daemon :shared:jvmTest --tests com.peltoche.fyde.ScreenshotsJvmTest --rerun"

count="$(find "$out" -name '*.png' | wc -l)"
[ "$count" -gt 0 ] || { echo "capture.sh: no screen rendered" >&2; exit 1; }

mkdir -p "$design/current/screens"
find "$design/current/screens" -name '*.png' -delete
cp "$out"/*.png "$design/current/screens/"
echo "capture.sh: $count screens in design/current/screens/"

nix-shell -p 'python3.withPackages(p: [p.pillow])' --run \
    "python3 '$design/current/sheets.py' && python3 '$design/current/check.py'"
