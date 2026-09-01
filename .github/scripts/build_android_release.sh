#!/usr/bin/env bash
set -euo pipefail

keystore="$RUNNER_TEMP/kvace-upload.jks"
echo "$ANDROID_KEYSTORE_BASE64" | base64 --decode > "$keystore"
./gradlew :app:androidApp:bundleRelease \
  -Pandroid.injected.signing.store.file="$keystore" \
  -Pandroid.injected.signing.store.password="$ANDROID_KEYSTORE_PASSWORD" \
  -Pandroid.injected.signing.key.alias="$ANDROID_KEY_ALIAS" \
  -Pandroid.injected.signing.key.password="$ANDROID_KEY_PASSWORD"
