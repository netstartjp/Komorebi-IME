#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CARGO_ROOT="${CARGO_HOME:-${HOME}/.cargo}"
CARGO_BIN="${CARGO_BIN:-${CARGO_ROOT}/bin/cargo}"
NDK_DIR="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"

if [[ -z "$NDK_DIR" ]]; then
  SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
  if [[ -z "$SDK_DIR" && -f "$PROJECT_DIR/local.properties" ]]; then
    SDK_DIR="$(sed -n 's/^sdk.dir=//p' "$PROJECT_DIR/local.properties" | tail -1)"
  fi
  if [[ -n "$SDK_DIR" && -d "$SDK_DIR/ndk" ]]; then
    NDK_DIR="$(find "$SDK_DIR/ndk" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)"
  fi
fi

if [[ ! -x "$CARGO_BIN" ]]; then
  echo "cargo not found: $CARGO_BIN" >&2
  exit 1
fi
if [[ -z "$NDK_DIR" || ! -d "$NDK_DIR/toolchains/llvm/prebuilt" ]]; then
  echo "Android NDK not found: $NDK_DIR" >&2
  exit 1
fi

cd "$PROJECT_DIR/third_party/karukan"
ANDROID_NDK="$NDK_DIR" \
ANDROID_NDK_HOME="$NDK_DIR" \
ANDROID_NDK_ROOT="$NDK_DIR" \
NDK_ROOT="$NDK_DIR" \
  "$CARGO_BIN" ndk \
  --target arm64-v8a \
  --platform 24 \
  --output-dir "$PROJECT_DIR/karukan/src/main/jniLibs" \
  build --package karukan-android --release

LIB_DIR="$PROJECT_DIR/karukan/src/main/jniLibs/arm64-v8a"
cp \
  "$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so" \
  "$LIB_DIR/libc++_shared.so"
"$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" \
  --strip-unneeded "$LIB_DIR/libc++_shared.so"
