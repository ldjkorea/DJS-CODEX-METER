#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
VERSION_NAME="9.9.13"
DIST="$ROOT/dist"
SIGNING_DIR="$ROOT/.local-signing"
KEYSTORE="$SIGNING_DIR/codex-meter-local.p12"
PASS_FILE="$SIGNING_DIR/password"

if [[ -z "${JAVA_HOME:-}" && -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi
if [[ -z "${ANDROID_SDK_ROOT:-}" && -d "$HOME/Library/Android/sdk" ]]; then
  export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
fi

if [[ ! -f "$KEYSTORE" || ! -f "$PASS_FILE" ]]; then
  echo "Existing Korean release signing key/password is missing; refusing to create a replacement key." >&2
  exit 1
fi
mkdir -p "$DIST"

"$ROOT/gradlew" --project-dir "$ROOT" \
  :app:assembleRelease \
  --console=plain

SOURCE_APK="$ROOT/app/build/outputs/apk/release/app-release.apk"
OUT="$DIST/GPT-HUD-BETA-$VERSION_NAME.apk"
cp "$SOURCE_APK" "$OUT"

APKSIGNER="$(find "$ANDROID_SDK_ROOT/build-tools" -type f \( -name apksigner -o -name apksigner.bat \) | sort | tail -1)"
"$APKSIGNER" verify --verbose --print-certs "$OUT"
(cd "$DIST" && sha256sum "$(basename "$OUT")") | tee "$DIST/SHA256SUMS.txt"
echo "Built $OUT"
