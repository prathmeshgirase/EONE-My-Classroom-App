#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v flutter >/dev/null || { echo "Flutter is not installed or on PATH" >&2; exit 1; }
tmp="$(mktemp -d)"
cp -R lib "$tmp/lib"
cp pubspec.yaml analysis_options.yaml "$tmp/"
flutter create --platforms=android,ios .
rm -rf lib
cp -R "$tmp/lib" ./lib
cp "$tmp/pubspec.yaml" ./pubspec.yaml
cp "$tmp/analysis_options.yaml" ./analysis_options.yaml
rm -rf "$tmp"
flutter pub get
echo "Native Flutter platform files generated successfully."
