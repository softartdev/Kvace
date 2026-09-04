# Manual Build and Install

All commands run from the repository root unless a section says otherwise. Kvace 1.0.0 targets Android 8.0/API 26+, iOS 18.2+, macOS arm64/x64, Linux arm64/x64, Windows x64, and modern WebAssembly browsers. Windows arm64 is not a release target.

## Prerequisites

- Git and a Java 21 JDK. Confirm with `java -version`; Gradle release CI also uses Java 21.
- macOS or Linux: the checked-in `./gradlew`; Windows PowerShell: `.\gradlew.bat`.
- Android: Android Studio, an installed Android SDK, `adb`, and either an emulator or a USB-debug-enabled device.
- iOS: macOS with Xcode, the `iosApp` shared scheme, and an iOS Simulator runtime. Physical-device and archive builds also need an Apple team and valid signing assets.
- Web: no global Node installation is required; the Kotlin/Gradle toolchain provisions its own tooling.
- Store screenshots: Android Studio, the [`android` CLI](https://developer.android.com/tools), ImageMagick's `magick`, and macOS `sips`.
- Release verification: GitHub CLI `gh`; `shasum` or `sha256sum`; and PowerShell `Get-FileHash` on Windows.

## A. Run from source

### Desktop

On macOS or Linux:

```bash
./gradlew :app:desktopApp:run
```

On Windows PowerShell:

```powershell
.\gradlew.bat :app:desktopApp:run
```

For Desktop development with Compose Hot Reload, use `./gradlew :app:desktopApp:hotRun` or `.\gradlew.bat :app:desktopApp:hotRun`.

### Android device or emulator

Start an emulator or connect one device, verify it is listed, install the debug APK, and launch the verified launcher activity:

```bash
adb devices
./gradlew :app:androidApp:installDebug
adb shell am start -n com.softartdev.kvace/.MainActivity
```

Windows PowerShell uses the same `adb` commands and `.\gradlew.bat :app:androidApp:installDebug`.

### iOS Simulator

List available simulator names, choose one, then build, install, and launch. The example uses `iPhone 17 Pro`; replace it with a name returned by the first command.

```bash
xcrun simctl list devices available
xcrun simctl boot "iPhone 17 Pro" || true
open -a Simulator
xcodebuild -project app/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -derivedDataPath build/xcode-simulator \
  build
xcrun simctl install booted build/xcode-simulator/Build/Products/Debug-iphonesimulator/Kvace.app
xcrun simctl launch booted com.softartdev.kvace
```

### Physical iOS device

Connect and trust the device, substitute the development team ID and device identifier, then build, install, and launch:

```bash
xcrun devicectl list devices
xcodebuild -project app/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphoneos \
  -destination 'generic/platform=iOS' \
  -derivedDataPath build/xcode-device \
  -allowProvisioningUpdates \
  DEVELOPMENT_TEAM=<APPLE_TEAM_ID> \
  build
xcrun devicectl device install app \
  --device <DEVICE_IDENTIFIER> \
  build/xcode-device/Build/Products/Debug-iphoneos/Kvace.app
xcrun devicectl device process launch \
  --device <DEVICE_IDENTIFIER> \
  com.softartdev.kvace
```

### Web

Run the development server on macOS/Linux:

```bash
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun
```

On Windows PowerShell:

```powershell
.\gradlew.bat :app:webApp:wasmJsBrowserDevelopmentRun
```

The Gradle task prints the local URL. Keep the process running while using the app.

## B. Install and launch downloaded artifacts

Download the artifact matching the operating system and CPU architecture from the `v1.0.0` GitHub Release. Native installers include a runtime. JARs require Java 21 and are OS/ABI-specific because they contain native Compose/Skiko libraries.

### Verify before installing

On macOS or Linux, put the asset beside `SHA256SUMS`:

```bash
shasum -a 256 -c SHA256SUMS
gh attestation verify Kvace-1.0.0-<platform>-<arch>.<extension> --repo softartdev/Kvace
```

On Linux, `sha256sum --check SHA256SUMS` is equivalent. On Windows PowerShell, compare this value with the asset's line in `SHA256SUMS`, then verify the attestation:

```powershell
Get-FileHash .\Kvace-1.0.0-windows-x64.msi -Algorithm SHA256
gh attestation verify .\Kvace-1.0.0-windows-x64.msi --repo softartdev/Kvace
```

GitHub attestations establish which workflow produced an artifact; they do not replace platform code signing.

### macOS DMG

Use the architecture-appropriate `Kvace-1.0.0-macos-arm64.dmg` or `Kvace-1.0.0-macos-x64.dmg`:

```bash
hdiutil attach ./Kvace-1.0.0-macos-arm64.dmg -nobrowse
ditto /Volumes/com.softartdev.kvace/com.softartdev.kvace.app /Applications/com.softartdev.kvace.app
hdiutil detach /Volumes/com.softartdev.kvace
open /Applications/com.softartdev.kvace.app
```

Substitute `macos-x64` only in the DMG filename on Intel. The release workflow Developer ID signs, notarizes, and staples both DMGs; inspect them with `spctl --assess --type open --context context:primary-signature -vv <dmg>` and `xcrun stapler validate <dmg>`.

### Windows MSI

Kvace 1.0.0's Windows x64 MSI is **not Authenticode-signed**. SHA-256 and GitHub provenance can verify the downloaded bytes and workflow origin, but Windows SmartScreen may show an Unknown publisher warning. Confirm the checksum and attestation before proceeding; do not bypass a warning for an unverified file.

From an elevated PowerShell prompt:

```powershell
Start-Process msiexec.exe -Wait -ArgumentList '/i', '.\Kvace-1.0.0-windows-x64.msi'
Start-Process "$env:ProgramFiles\com.softartdev.kvace\com.softartdev.kvace.exe"
```

### Linux DEB

On matching Debian/Ubuntu x64 or arm64 hardware:

```bash
sudo apt install ./Kvace-1.0.0-linux-x64.deb
/opt/com.softartdev.kvace/bin/com.softartdev.kvace
```

Substitute `linux-arm64` on arm64. The DEBs are not distribution-signed; verify SHA-256 and the GitHub attestation first.

### Desktop JAR

Run only the JAR matching the current OS and architecture:

```bash
java -version
java -jar Kvace-1.0.0-macos-arm64.jar
```

Other valid axes are `macos-x64`, `linux-x64`, `linux-arm64`, and `windows-x64`. These are not universal JARs.

### Android APK and AAB

If an APK is supplied through an approved testing channel, install and launch it with:

```bash
adb install -r ./Kvace-1.0.0.apk
adb shell am start -n com.softartdev.kvace/.MainActivity
```

The production artifact is `androidApp-release.aab`. An AAB is a Google Play publishing bundle, not a directly installable APK; `adb install` cannot install it. Install the production app from Google Play, or use `bundletool` to produce device-specific APKs from an AAB for authorized local testing. Never distribute the upload keystore.

## C. Build artifacts and run checks

### Android

```bash
./gradlew :app:androidApp:assembleDebug
./gradlew :app:androidApp:assembleRelease
./gradlew :app:androidApp:bundleRelease
```

Outputs:

- debug APK: `app/androidApp/build/outputs/apk/debug/androidApp-debug.apk`
- unsigned local release APK: `app/androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk`
- local release bundle: `app/androidApp/build/outputs/bundle/release/androidApp-release.aab`

The production workflow calls `.github/scripts/build_android_release.sh`, which injects signing values from protected environment secrets and writes the signed AAB to the same bundle path. Do not put a keystore or its passwords in Git.

### iOS framework, archive, and IPA

Simulator and physical-device frameworks:

```bash
./gradlew :app:shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :app:shared:linkReleaseFrameworkIosArm64
```

Outputs are under `app/shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework` and `app/shared/build/bin/iosArm64/releaseFramework/Shared.framework`.

For a signed App Store archive/IPA, provide the same protected signing variables used by CI and run:

```bash
APPLE_TEAM_ID=<team-id> \
IOS_PROVISIONING_PROFILE_NAME=<profile-name> \
.github/scripts/build_ios_release.sh
```

The verified scheme is `iosApp`; outputs are `build/Kvace.xcarchive` and `build/Kvace.ipa`.

### Desktop native installer and Uber JAR

Build both formats for the current OS/architecture:

```bash
./gradlew :app:desktopApp:packageReleaseDistributionForCurrentOS \
  :app:desktopApp:packageReleaseUberJarForCurrentOS
```

Windows uses the same tasks through `.\gradlew.bat`. Native installers are under `app/desktopApp/build/compose/binaries/main-release/{dmg,msi,deb}/`; the current OS/ABI JAR is under `app/desktopApp/build/compose/jars/`. The release workflow renames these to `Kvace-1.0.0-<platform>-<arch>.<extension>`.

### Web production bundle

```bash
./gradlew :app:webApp:wasmJsBrowserProductionWebpack
```

Windows: `.\gradlew.bat :app:webApp:wasmJsBrowserProductionWebpack`. Output: `app/webApp/build/dist/wasmJs/productionExecutable/`.

### Store screenshots and package

One command generates all 28 opaque RGB PNGs and fully validates their manifest paths, dimensions, alpha state, and content:

```bash
.github/scripts/generate_store_screenshots.sh
```

Output: `build/distribution/screenshots/`. Generated PNGs are gitignored; only preview code, sample data, metadata, alt text, and `distribution/screenshots/manifest.tsv` belong in Git.

Package a deterministic release asset and external checksum separately:

```bash
.github/scripts/package_store_assets.sh
```

Outputs: `build/release/Kvace-1.0.0-store-assets.zip` and `build/release/Kvace-1.0.0-store-assets.zip.sha256`. The release operator flow is in [Release Guide](release/RELEASE_GUIDE.md).

### Tests and validation

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

With an emulator/device attached, run `./gradlew :app:androidApp:connectedDebugAndroidTest`. The live Ollama test stays disabled unless explicitly enabled with `-Pandroid.testInstrumentationRunnerArguments.runLiveOllamaTests=true`.

## Runtime limitations

- Ollama must be reachable from the target; the Android emulator maps the development host to `10.0.2.2` by default.
- OpenAI-compatible providers use user-supplied credentials. Web credentials and history are session-only, and provider endpoints must permit browser CORS.
- Gemini Nano still depends on compatible Android hardware/model availability. Apple Foundation Models require iOS 26+ or Apple Silicon macOS 26+, although the iOS app supports iOS 18.2+.
- Desktop `shell_command` is read-only and allowlisted; Android, iOS, and Web do not expose it.
