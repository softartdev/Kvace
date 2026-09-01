# Manual Build and Install

## Prerequisites

- Git and Java 21 for source builds or release JARs.
- Android Studio/Android SDK for Android builds and Android CLI screenshot rendering.
- Xcode for iOS builds and macOS signing.
- ImageMagick for producing opaque store PNGs from Android CLI renders.
- The matching OS and CPU architecture for every Desktop JAR.

## Install release artifacts

Download the matching asset from GitHub Releases and verify it against `SHA256SUMS` before opening it. Verify provenance with `gh attestation verify <asset> --repo softartdev/Kvace`.

- macOS: open the matching arm64 or x64 DMG and drag Kvace to Applications.
- Windows: run the Authenticode-signed x64 MSI.
- Linux: install the matching x64 or arm64 DEB using your package manager.
- JAR: run only the matching OS/ABI artifact with `java -jar Kvace-<version>-<platform>-<arch>.jar`.

Native installers include a runtime. JARs require Java 21 and are not universal because they include native Compose/Skiko components. Windows arm64 is not produced.

The release workflow signs the Windows MSI, signs and notarizes both macOS DMGs, publishes SHA-256 checksums, and creates GitHub build-provenance attestations. Linux packages and all JARs are covered by checksums and attestations but are not code-signed.

## Build from source

```bash
./gradlew :app:androidApp:assembleDebug
./gradlew :app:androidApp:bundleRelease
./gradlew :app:desktopApp:run
./gradlew :app:desktopApp:hotRun
./gradlew :app:desktopApp:packageReleaseUberJarForCurrentOS
./gradlew :app:desktopApp:packageReleaseDistributionForCurrentOS
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun
./gradlew :app:webApp:wasmJsBrowserProductionWebpack
./gradlew :app:shared:linkDebugFrameworkIosSimulatorArm64
```

Open `app/iosApp` in Xcode to run the iOS target. The app requires iOS 18.2 or newer; Apple Foundation Models require iOS 26+ or supported Apple Silicon macOS 26+ hardware.

## Provider requirements and limitations

- Ollama requires a user-managed server reachable from the app. Android emulators use `10.0.2.2` for the development host by default.
- OpenAI-compatible providers require the user's credentials. Web credentials are kept only for the current tab and the endpoint must permit browser CORS.
- Native Android, iOS, and Desktop chat history is local. Web history lasts only for the browser session.
- Gemini Nano depends on device support and model availability even on Android API 26+.
- The Desktop `shell_command` tool permits only its read-only allowlist and is unavailable on Android, iOS, and Web.

## Marketing screenshots

With Kvace open in Android Studio, generate deterministic store assets:

```bash
.github/scripts/generate_store_screenshots.sh
.github/scripts/validate_distribution.sh
```

Complete the visual gate by opening the same populated sample states through Compose Hot Reload MCP and checking compact and wide layouts before committing the generated PNGs.

The generator requires ImageMagick's `magick` command because App Store screenshots must be flattened to opaque RGB PNGs. It writes exactly 28 assets: six Google Play phone, six Google Play tablet, six App Store iPhone, six App Store iPad, and four Desktop screenshots.
