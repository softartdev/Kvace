# Kvace 1.0.0 Release Guide

This repository contains credential-free validation and protected production automation. It does not create accounts, approve agreements, grant store access, create/push tags, or publish anything during ordinary builds. Complete [Store Bootstrap](STORE_BOOTSTRAP.md) and the [Release Checklist](RELEASE_CHECKLIST.md) first.

## Release destinations and approval boundaries

| Destination | Protected environment | Approved behavior |
|---|---|---|
| Google Play | `google-play-production` | Signed AAB plus verified listing/screenshots directly to Production at 100%, status `completed` |
| Apple App Store | `app-store-production` | IPA upload, verified metadata/screenshots, build association, direct App Review submission, automatic release after approval |
| GitHub Releases | `github-release-production` | Five native installers, five OS/ABI JARs, checksums, and provenance uploaded to an existing draft |
| GitHub Pages | `github-pages` | Web app, privacy page, and support page from `main` |

The Google and Apple jobs do not use a testing track, staged rollout, TestFlight group, or Fastlane. Approving either job authorizes a production-side effect.

## 1. Prepare the release commit

1. Confirm `version.properties` is `VERSION_NAME=1.0.0` and `VERSION_CODE=1`, the changelog heading is correct, and Android/iOS/Desktop version declarations derive from that file.
2. Review `distribution/listing-en.md`, `distribution/privacy-data.md`, `distribution/metadata.env`, and every row/alt text in `distribution/screenshots/manifest.tsv` against the final binary.
3. Generate and fully validate the 28 screenshots. Android Studio, Android CLI, ImageMagick, and `sips` are required:

   ```bash
   .github/scripts/generate_store_screenshots.sh
   ```

   Output is `build/distribution/screenshots/`; no generated PNG belongs in Git. Complete the repository's Android CLI preview and Compose Hot Reload MCP visual gates.

4. Run the credential-free release checks:

   ```bash
   .github/scripts/validate_release_contract.sh
   .github/scripts/validate_distribution.sh
   ./gradlew :core:domain:allTests :core:presentation:allTests \
     :feature:agent:domain:allTests :feature:agent:data:allTests :feature:agent:presentation:allTests \
     :feature:chat:domain:allTests :feature:chat:data:allTests :feature:chat:presentation:allTests \
     :feature:settings:domain:allTests :feature:settings:data:allTests :feature:settings:presentation:allTests \
     :app:shared:allTests
   ./gradlew :app:androidApp:lint :app:androidApp:assembleDebug \
     :app:desktopApp:test :app:webApp:wasmJsBrowserProductionWebpack \
     :app:shared:linkDebugFrameworkIosSimulatorArm64
   ```

5. Package the already-reviewed screenshots in a deterministic archive:

   ```bash
   .github/scripts/package_store_assets.sh
   (cd build/release && shasum -a 256 -c Kvace-1.0.0-store-assets.zip.sha256)
   ```

   The archive contains the screenshots, exact manifest, metadata, release identity, and per-file `SHA256SUMS`. The external `.sha256` authenticates the archive bytes consumed by CI.

## 2. Tag, create the draft, and upload store assets

Only a maintainer performs this section. The tag push starts protected jobs; do not approve them yet.

```bash
git tag -s v1.0.0 -m 'Kvace 1.0.0'
git push origin v1.0.0
gh release create v1.0.0 \
  --draft \
  --verify-tag \
  --title 'Kvace v1.0.0' \
  --generate-notes
gh release upload v1.0.0 \
  build/release/Kvace-1.0.0-store-assets.zip \
  build/release/Kvace-1.0.0-store-assets.zip.sha256
```

There is deliberately no `--clobber`. If the release or an asset already exists, stop and inspect it; do not replace evidence in place. A retry uses the existing signed tag and exact existing assets.

Both mobile workflows call the same `.github/scripts/download_store_assets.sh`. It requires exact tag `v1.0.0`, an existing draft release, one correctly named ZIP and sidecar, matching external SHA-256, safe archive paths, matching committed manifest/metadata/release identity, valid internal checksums, exactly 28 expected files, exact dimensions, opaque PNG encoding, and unique nontrivial content. Any absence or mismatch fails before credentials are used.

## 3. Approve protected workflows

1. Review the tag, draft asset names/checksums, CI result, bootstrap evidence, and the checklist.
2. Approve `github-release-production`. Its workflow uploads named installers/JARs, `SHA256SUMS`, and attestations to the existing draft without overwriting assets. It leaves the release in draft state.
3. Approve `google-play-production` only when direct Production is intended. The workflow exchanges GitHub OIDC for a short-lived Google token through WIF and commits one Android Publisher edit.
4. Approve `app-store-production` only when direct App Review submission is intended. The workflow uploads build 1 and submits version 1.0.0; it does not use TestFlight.
5. If a tag-triggered job must be retried, dispatch the same workflow with input `v1.0.0`. Never point a production workflow at a branch or different tag.

## 4. Publish the GitHub draft and verify public destinations

After desktop uploads and both store submissions have the expected result, verify the draft inventory before making it public:

```bash
gh release view v1.0.0 --json isDraft,tagName,assets
gh release download v1.0.0 --pattern 'SHA256SUMS' --pattern 'Kvace-1.0.0-*'
shasum -a 256 -c SHA256SUMS
gh attestation verify Kvace-1.0.0-macos-arm64.dmg --repo softartdev/Kvace
gh release edit v1.0.0 --draft=false
```

Verify each of the ten Desktop artifacts on matching hardware. macOS DMGs must be Developer ID signed/notarized/stapled. The 1.0.0 Windows MSI is not Authenticode-signed and can trigger SmartScreen; SHA-256 and attestation verification are mandatory. Linux packages and JARs are checksum/attestation-covered but not platform-signed.

Confirm Google Play, App Store Connect, GitHub Release, Pages, privacy, and support URLs. Add public store/download badges only after their final destinations work, and record final URLs in the private release issue.

## Design record

[ADR 0001](../adr/0001-store-release-automation.md) records why this project keeps official Android Publisher and App Store Connect API/CLI mechanisms instead of Fastlane or third-party Marketplace publication Actions.
