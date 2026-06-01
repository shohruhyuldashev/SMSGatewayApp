#!/usr/bin/env bash
set -euo pipefail

# Build helper: uses local.properties SDK path if present, otherwise environment.
SDK_PATH=""
if [ -f local.properties ]; then
  SDK_PATH=$(grep -E '^sdk.dir=' local.properties | cut -d'=' -f2- || true)
fi
if [ -z "$SDK_PATH" ]; then
  SDK_PATH=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
fi
if [ -z "$SDK_PATH" ]; then
  echo "Android SDK path not found. Set ANDROID_SDK_ROOT or update local.properties." >&2
  exit 1
fi

ANDROID_SDK_ROOT="$SDK_PATH" ANDROID_HOME="$SDK_PATH" ./gradlew "$@"
