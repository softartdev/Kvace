# ADR 0001: Use platform APIs for store publication

- Status: Accepted
- Date: 2026-09-02
- Scope: Google Play and Apple App Store release automation

## Decision

Keep Kvace's small, repository-owned release scripts. Android authenticates with Google Workload Identity Federation and calls the Android Publisher API directly. iOS builds and exports with Xcode, uploads with Apple's CLI, and manages metadata, screenshots, build linking, and review submission through the App Store Connect API. Do not add Fastlane or a Marketplace publication action.

GitHub Actions are referenced by readable major-version tags (for example, `actions/checkout@v4`) so Dependabot can track the selected release line and the workflow remains easy to maintain. Repository administrators should review action updates through Dependabot and normal pull-request review.

## Context

Google does not publish an official end-to-end GitHub Action for Play listing, bundle, screenshot, track, and edit publication. `r0adkll/upload-google-play` is third-party and commonly authenticates with service-account JSON (although it also documents WIF); it would still leave parts of Kvace's listing contract outside the Action. Gradle Play Publisher describes itself as an [unofficial plugin in maintenance mode](https://github.com/Triple-T/gradle-play-publisher#project-status-maintenance-mode). The official mechanism that covers bundle upload, listings, graphics, tracks, transactional validation, and commit is the [Google Play Publishing API](https://developer.android.com/google/play/developer-api#publishing_api).

`Apple-Actions/upload-testflight-build` is a third-party Action from an Apple-named GitHub organization, not an Action certified or reviewed by GitHub. GitHub notes that Marketplace Actions are published without GitHub review when listing requirements are met, and a verified-creator badge verifies the partner organization rather than its code; see [Publishing Actions in GitHub Marketplace](https://docs.github.com/en/actions/how-tos/create-and-publish-actions/publish-in-github-marketplace#about-badges). The Action's declared scope is build upload to TestFlight; it does not own Kvace's App Store metadata, screenshots, version/build association, or direct App Review submission. Apple documents Xcode/CLI upload in [Upload builds](https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds) and metadata/review automation in the [App Store Connect API](https://developer.apple.com/documentation/appstoreconnectapi/).

## Alternatives considered

- Fastlane: mature and broad, but adds Ruby/plugin lifecycle and a second metadata abstraction for two compact scripts.
- `r0adkll/upload-google-play`: convenient bundle/track upload, but third-party, historically JSON-key-oriented, and not a complete replacement for the current listing/screenshot contract.
- Gradle Play Publisher: broad feature set, but unofficial and explicitly maintenance-mode.
- `Apple-Actions/upload-testflight-build`: useful for TestFlight upload, but Kvace submits directly to App Review and still needs the remaining App Store Connect API work.
- Manual console-only publication: fewer scripts, but less reproducible and harder to review.

## Consequences

The implementation does not change publication providers or add dependencies. The repository must maintain its compact REST/CLI scripts and API contract tests, while protected GitHub environments remain the approval boundary for production side effects.
