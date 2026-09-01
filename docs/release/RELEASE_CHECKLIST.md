# Kvace 1.0.0 Release Checklist

- [ ] The release commit is clean, CI is green, and `version.properties`, Android, iOS, Desktop, and the existing `v1.0.0` tag agree.
- [ ] `validate_release_contract.sh` and `validate_distribution.sh` pass on the tagged commit.
- [ ] All 28 opaque screenshot PNGs pass dimension validation, Android CLI preview review, and Compose Hot Reload MCP review.
- [ ] GitHub environments `google-play-production`, `app-store-production`, `github-release-production`, and `github-pages` exist with required reviewers.
- [ ] Google Play record, Production access, service identity, App Signing, Data Safety, content rating, pricing, availability, and listing are complete.
- [ ] The Android AAB passes bundletool/device smoke checks and the approved workflow submits version code 1 directly to Production with status `completed`; no testing track or staged rollout is selected.
- [ ] App Store Connect agreements, app record, categories, age rating, App Privacy, pricing, availability, certificates, profile, and review contact are complete.
- [ ] The iOS archive passes signing and device smoke checks; the approved workflow uploads the build, attaches it to 1.0.0, submits directly to App Review, and uses automatic release after approval. It does not use TestFlight.
- [ ] Desktop installer and matching JAR smoke tests pass for macOS arm64/x64, Linux arm64/x64, and Windows x64; no Windows arm64 asset exists.
- [ ] Both macOS DMGs are Developer ID signed, notarized, stapled, and assessed; the Windows MSI is Authenticode-signed and verified.
- [ ] `SHA256SUMS` matches all ten Desktop assets and GitHub provenance attestations verify.
- [ ] GitHub Pages, privacy, support, and release pages are reachable after their protected deployments.
- [ ] Store/download badges and public store links are added only after their destination URLs are live.
- [ ] A maintainer records the final Google Play, App Store Connect, GitHub Release, and Pages URLs in the release issue.
