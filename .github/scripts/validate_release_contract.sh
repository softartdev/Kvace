#!/usr/bin/env bash
set -euo pipefail

source version.properties
[[ "$VERSION_NAME" == "1.0.0" && "$VERSION_CODE" == "1" ]]
grep -q 'versionName = releaseVersion.getProperty("VERSION_NAME")' app/androidApp/build.gradle.kts
grep -q 'versionCode = releaseVersion.getProperty("VERSION_CODE").toInt()' app/androidApp/build.gradle.kts
grep -q 'packageVersion = releaseVersion.getProperty("VERSION_NAME")' app/desktopApp/build.gradle.kts
grep -q 'PRODUCT_BUNDLE_IDENTIFIER=com.softartdev.kvace' app/iosApp/Configuration/Config.xcconfig
grep -q 'CURRENT_PROJECT_VERSION=$(VERSION_CODE)' app/iosApp/Configuration/Config.xcconfig
grep -q 'MARKETING_VERSION=$(VERSION_NAME)' app/iosApp/Configuration/Config.xcconfig
grep -q 'GNU GENERAL PUBLIC LICENSE' LICENSE
grep -q '^## \[1.0.0\] - Unreleased$' CHANGELOG.md
if [[ "${GITHUB_REF_TYPE:-}" == tag ]]; then
  [[ "${GITHUB_REF_NAME}" == "v${VERSION_NAME}" ]]
fi
