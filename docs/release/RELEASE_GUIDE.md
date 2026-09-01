# Kvace 1.0.0 Release Guide

This repository contains the release automation and metadata for 1.0.0. It does not create store records, credentials, approvals, tags, or public releases by itself. No publication should occur until the one-time bootstrap and the release checklist are complete.

## Release destinations

| Destination | Protected environment | Behavior |
|---|---|---|
| Google Play | `google-play-production` | Uploads the signed AAB directly to Production at 100% with status `completed`; no testing track or staged rollout |
| Apple App Store | `app-store-production` | Uploads the IPA, attaches the processed build to version 1.0.0, submits directly to App Review, and releases automatically after approval; no TestFlight workflow |
| GitHub Releases | `github-release-production` | Publishes five native installers and five matching OS/ABI JARs, checksums, and provenance attestations |
| GitHub Pages | `github-pages` | Publishes the Web app, privacy policy, and support page from `main` |

Configure every environment with required reviewers and disable administrator bypass where organization policy permits. The production workflows also support manual dispatch, but require an existing release tag and check out that exact tag.

## One-time Google Play bootstrap

1. Create the Google Play app record for package `com.softartdev.kvace` and complete Play App Signing, category, content rating, target audience, Data Safety, privacy policy, pricing, countries, and Production-access requirements.
2. Create a Google Cloud service account or workload identity with the minimum Play Console permissions needed to edit listings and publish Production releases.
3. Add these GitHub environment secrets:

   - `ANDROID_KEYSTORE_BASE64`
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`
   - `GOOGLE_WORKLOAD_IDENTITY_PROVIDER`
   - `GOOGLE_PLAY_SERVICE_ACCOUNT`

The workflow uses the Android Publisher API and commits version code 1 directly to the `production` track with release status `completed`.

## One-time App Store Connect bootstrap

1. Create the App Store Connect app record for bundle ID `com.softartdev.kvace` and finish agreements, tax/banking, primary category, age rating, App Privacy, pricing, availability, and required compliance declarations.
2. Create an Apple Distribution certificate and App Store provisioning profile. Create an App Store Connect API key with the minimum role that can upload builds, edit app metadata, manage screenshots, and submit for review.
3. Add these GitHub environment secrets:

   - `IOS_CERTIFICATE_BASE64`
   - `IOS_CERTIFICATE_PASSWORD`
   - `IOS_PROVISIONING_PROFILE_BASE64`
   - `IOS_PROVISIONING_PROFILE_NAME`
   - `APPLE_TEAM_ID`
   - `APP_STORE_CONNECT_KEY_ID`
   - `APP_STORE_CONNECT_ISSUER_ID`
   - `APP_STORE_CONNECT_PRIVATE_KEY`
   - `APP_REVIEW_CONTACT_FIRST_NAME`
   - `APP_REVIEW_CONTACT_LAST_NAME`
   - `APP_REVIEW_CONTACT_PHONE`
   - `APP_REVIEW_CONTACT_EMAIL`

The workflow uses Xcode export, Apple's upload CLI, and the official App Store Connect API. It creates or updates the 1.0.0 metadata and screenshots, waits for build 1 to finish processing, selects automatic release after approval, and submits the version to App Review. It does not create a TestFlight test or group.

## One-time Desktop and Pages bootstrap

Add the Desktop signing secrets to `github-release-production`:

- `MACOS_CERTIFICATE_BASE64`
- `MACOS_CERTIFICATE_PASSWORD`
- `MACOS_SIGNING_IDENTITY`
- `APPLE_ID`
- `APPLE_TEAM_ID`
- `APPLE_APP_PASSWORD`
- `WINDOWS_CERTIFICATE_BASE64`
- `WINDOWS_CERTIFICATE_PASSWORD`

The Desktop matrix publishes macOS arm64/x64 DMGs, Linux arm64/x64 DEBs, and a Windows x64 MSI plus a matching JAR for each axis. Windows arm64 is intentionally unsupported. macOS artifacts are signed, notarized, and stapled; the Windows MSI is Authenticode-signed. `SHA256SUMS` and GitHub build-provenance attestations cover the published files.

In repository Pages settings, choose GitHub Actions as the source and protect `github-pages` with the release reviewer. The Pages workflow deploys from `main`; do not add public store badges until their final URLs work.

## Prepare and validate

1. Generate and inspect the deterministic assets. Android Studio must be open and ready, Android CLI must be installed, and ImageMagick must provide `magick`.

   ```bash
   .github/scripts/generate_store_screenshots.sh
   .github/scripts/validate_distribution.sh
   ```

2. Complete the repository's Android CLI and Compose Hot Reload MCP visual gates.
3. Run the release contract and platform builds.

   ```bash
   .github/scripts/validate_release_contract.sh
   ./gradlew :app:androidApp:bundleRelease
   ./gradlew :app:desktopApp:packageReleaseUberJarForCurrentOS \
     :app:desktopApp:packageReleaseDistributionForCurrentOS
   ./gradlew :app:webApp:wasmJsBrowserProductionWebpack
   ./gradlew :app:shared:linkDebugFrameworkIosSimulatorArm64
   ```

4. Review `distribution/listing-en.md`, `distribution/privacy-data.md`, screenshots, and the release notes against the final binary behavior.
5. Complete every item in `RELEASE_CHECKLIST.md` before creating the tag.

## Publish after approval

Create and push the signed `v1.0.0` tag only after the protected environments and store records are ready. Tag-triggered workflows stop at their environment approval gates. Approving a job is authorization for its production-side effect; Google Play and App Store jobs are not staging dry runs.

If a tag-triggered run must be retried, manually dispatch the workflow with the existing tag name. Never point a production workflow at an untagged branch.

After publication, verify checksums and attestations, smoke-test every public artifact, record the final URLs, and only then add public store/download badges to the README.

## Current limitations

- Ollama requires a user-managed reachable server; hosted providers require the user's credentials and can incur provider charges.
- Web provider access requires CORS. Web credentials and history are session-only.
- Android on-device inference depends on compatible hardware and model availability.
- Apple Foundation Models require iOS 26+ or Apple Silicon macOS 26+; the iOS app itself supports iOS 18.2+.
- Desktop JARs require Java 21 and the matching OS/ABI. Native installers bundle their runtime.
- Desktop `shell_command` execution is read-only and allowlisted. It is unavailable on Android, iOS, and Web.
