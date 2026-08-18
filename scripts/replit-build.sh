#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# LoreCanvas — Replit build script.
#
# This does, from a plain Linux shell, exactly what Android Studio does for
# you automatically: makes sure a Gradle wrapper exists, makes sure an
# Android SDK is present, then runs a Gradle build. It is idempotent — the
# SDK and Gradle wrapper are downloaded once and cached in this project
# folder (Replit persists the filesystem between runs), so re-running this
# script later just rebuilds.
#
# Output: app/build/outputs/apk/debug/app-debug.apk
#
# IMPORTANT — what this script does NOT do: Replit has no Android emulator
# and no display, so you cannot see or interact with the app's UI here.
# This script only proves the app builds. To actually run it, copy the APK
# to your Android phone (Replit lets you download files from the file
# tree) and install it directly — or open this same project in Android
# Studio, which gives you an emulator plus this exact same Gradle project.
# ---------------------------------------------------------------------------

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

SDK_ROOT="${ANDROID_SDK_ROOT:-$PROJECT_ROOT/.android-sdk}"
CMDLINE_TOOLS_DIR="$SDK_ROOT/cmdline-tools/latest"
COMPILE_SDK="36"
BUILD_TOOLS="36.0.0"

echo "=== LoreCanvas Replit build ==="
echo "Project root: $PROJECT_ROOT"
echo "Android SDK root: $SDK_ROOT"
echo

# ---------------------------------------------------------------------------
# Step 1 — Gradle wrapper
#
# If gradlew hasn't been generated yet (it isn't committed, since the wrapper
# jar is a binary this environment can't produce ahead of time), use the
# `gradle` binary Nix installed to generate a real one, pinned to a known
# good version. After this runs once, ./gradlew works everywhere — Replit,
# Android Studio, or any other machine that opens this project.
# ---------------------------------------------------------------------------
if [ ! -f "./gradlew" ]; then
  echo "--- No Gradle wrapper found yet. Generating one (one-time step)... ---"
  if ! command -v gradle >/dev/null 2>&1; then
    echo "ERROR: 'gradle' is not on PATH. Make sure replit.nix includes pkgs.gradle" >&2
    exit 1
  fi
  gradle wrapper --gradle-version 8.11.1 --distribution-type bin
  chmod +x ./gradlew
  echo "--- Gradle wrapper generated. ---"
  echo
fi

# ---------------------------------------------------------------------------
# Step 2 — Android SDK (cached under $SDK_ROOT)
# ---------------------------------------------------------------------------
if [ ! -d "$CMDLINE_TOOLS_DIR" ]; then
  echo "--- No cached Android SDK found. Downloading command-line tools... ---"
  mkdir -p "$SDK_ROOT"
  TMP_ZIP="$(mktemp --suffix=.zip)"

  # Ask Google's own download page for the current Linux command-line tools
  # URL rather than hardcoding a build number, since that number changes
  # over time. Falls back to a known-good build if the scrape fails.
  DOWNLOAD_URL="$(curl -fsSL https://developer.android.com/studio 2>/dev/null \
    | grep -o 'https://dl.google.com/android/repository/commandlinetools-linux-[0-9]*_latest.zip' \
    | head -n 1 || true)"
  if [ -z "$DOWNLOAD_URL" ]; then
    DOWNLOAD_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    echo "(Could not scrape the current download URL — falling back to a pinned known-good version.)"
  fi

  echo "Downloading: $DOWNLOAD_URL"
  wget -q --show-progress -O "$TMP_ZIP" "$DOWNLOAD_URL"

  mkdir -p "$SDK_ROOT/cmdline-tools"
  unzip -q "$TMP_ZIP" -d "$SDK_ROOT/cmdline-tools"
  mv "$SDK_ROOT/cmdline-tools/cmdline-tools" "$CMDLINE_TOOLS_DIR"
  rm -f "$TMP_ZIP"
  echo "--- Command-line tools installed. ---"
  echo
fi

export ANDROID_SDK_ROOT="$SDK_ROOT"
export ANDROID_HOME="$SDK_ROOT"
export PATH="$CMDLINE_TOOLS_DIR/bin:$SDK_ROOT/platform-tools:$PATH"

# Write local.properties so the Android Gradle Plugin knows where the SDK
# is without relying on the environment variable alone.
echo "sdk.dir=$SDK_ROOT" > "$PROJECT_ROOT/local.properties"

if [ ! -d "$SDK_ROOT/platforms/android-$COMPILE_SDK" ]; then
  echo "--- Accepting SDK licenses and installing platform + build-tools (first run only)... ---"
  yes | sdkmanager --sdk_root="$SDK_ROOT" --licenses >/dev/null 2>&1 || true
  sdkmanager --sdk_root="$SDK_ROOT" \
    "platform-tools" \
    "platforms;android-$COMPILE_SDK" \
    "build-tools;$BUILD_TOOLS"
  echo "--- SDK packages installed. ---"
  echo
fi

# ---------------------------------------------------------------------------
# Step 3 — Build
# ---------------------------------------------------------------------------
echo "--- Building debug APK (./gradlew assembleDebug)... ---"
./gradlew assembleDebug --console=plain

APK_PATH="$PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"
echo
if [ -f "$APK_PATH" ]; then
  echo "=== BUILD SUCCEEDED ==="
  echo "APK: $APK_PATH"
  echo
  echo "To actually use the app: download app-debug.apk from the file tree"
  echo "(or run 'adb install $APK_PATH' if a device is connected) and install"
  echo "it on your Android phone. Replit itself cannot run/display the UI —"
  echo "there's no emulator or screen here, only the build."
else
  echo "=== BUILD FINISHED, but the expected APK was not found at: ==="
  echo "$APK_PATH"
  echo "Check the Gradle output above for the actual output path/errors."
fi
