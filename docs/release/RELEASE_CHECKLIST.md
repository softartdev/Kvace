# Kvace 1.0.0 Release Checklist

## Repository and assets

- [ ] The release commit is clean; CI is green; `version.properties`, Android, iOS, Desktop, changelog, and signed tag `v1.0.0` agree.
- [ ] `validate_release_contract.sh` and source-only `validate_distribution.sh` pass with no generated PNGs under `distribution/screenshots/`.
- [ ] `generate_store_screenshots.sh` produced and fully validated 28 opaque screenshots under `build/distribution/screenshots/`.
- [ ] Android CLI and Compose Hot Reload MCP visual gates passed on the reviewed sample states.
- [ ] `package_store_assets.sh` produced `Kvace-1.0.0-store-assets.zip` and its `.sha256`; external and internal checksums verify.
- [ ] The exact draft `v1.0.0` release was created after tag push, both store assets were uploaded without `--clobber`, and production jobs have not yet been approved.

## Access and bootstrap

- [ ] [Store Bootstrap](STORE_BOOTSTRAP.md) is complete and no certificate, profile, keystore, private key, JSON credential, password, or token exists in Git.
- [ ] Environments `google-play-production`, `app-store-production`, `github-release-production`, and `github-pages` exist with required reviewers and appropriate anti-bypass policy.
- [ ] If the Play account is personal and was created after 13 November 2023, at least 12 closed-test testers stayed opted in continuously for at least 14 days and Play granted Production access.
- [ ] Google WIF is restricted to this repository/environment and can impersonate only the minimally privileged Kvace publishing service account; no service-account JSON key is used.

## Google Play

- [ ] App record, Play App Signing, upload certificate, Data safety, content rating, target audience, pricing, countries, listing, and Production access are complete.
- [ ] Signed AAB version code 1 passes device/bundle checks.
- [ ] The approved workflow downloads and verifies exact draft assets before authentication, then commits directly to Production with status `completed`, no testing track and no staged rollout.

## Apple App Store

- [ ] Agreements, tax/banking, app record, categories, age rating, App Privacy, pricing, availability, export compliance, certificate, profile, API key, and review contact are complete.
- [ ] Signed archive/build 1 passes physical-device and signing/entitlements smoke checks.
- [ ] The approved workflow downloads and verifies exact draft assets before signing, uploads the IPA, attaches build 1 to version 1.0.0, submits directly to App Review, and selects automatic release after approval; no TestFlight group/test is created.

## Desktop, release, and public verification

- [ ] Installer and matching JAR smoke tests pass for macOS arm64/x64, Linux arm64/x64, and Windows x64; no Windows arm64 artifact exists.
- [ ] Both macOS DMGs are Developer ID signed, notarized, stapled, and assessed.
- [ ] The Windows 1.0.0 MSI is recorded as **not Authenticode-signed**; SmartScreen/Unknown publisher guidance is present and checksum/attestation verification passes.
- [ ] `SHA256SUMS` matches all ten Desktop artifacts and GitHub provenance attestations verify.
- [ ] Desktop workflow uploaded to the existing draft without overwriting any asset; final draft inventory was reviewed before `gh release edit v1.0.0 --draft=false`.
- [ ] GitHub Pages, privacy, support, Google Play, App Store, and GitHub Release URLs are reachable; public badges were added only after destinations became live.
- [ ] Final destination URLs and approval evidence are recorded in the private release issue.
