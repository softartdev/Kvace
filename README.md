[![CI](https://github.com/softartdev/Kvace/actions/workflows/ci.yml/badge.svg)](https://github.com/softartdev/Kvace/actions/workflows/ci.yml)
[![Build & Deploy CI/CD](https://github.com/softartdev/Kvace/actions/workflows/gh-pages.yml/badge.svg)](https://github.com/softartdev/Kvace/actions/workflows/gh-pages.yml)

# Kvace

<p align="center">
  <img src="app/desktopApp/src/main/resources/icons/Kvace-iOS-Default-1024x1024@1x.png" width="120" alt="Kvace App Icon" />
</p>

Kvace is a Kotlin Multiplatform AI agent workspace built with Compose Multiplatform. The first milestone is a shared chat-oriented UI for configuring and talking to AI agents. The agent layer is wrapped behind Kvace domain contracts so JetBrains Koog can be used without leaking Koog-specific types into presentation or UI modules.

See the [roadmap](docs/roadmap.md) for the planned build-out from the current scaffold to richer agent execution, durable chat history, and later terminal capabilities.

## Targets

- Android
- iOS
- Desktop JVM
- Web through Wasm JS

## Project Structure

Application entry modules:

- [app/androidApp](app/androidApp): Android application entry point.
- [app/iosApp](app/iosApp): SwiftUI/Xcode iOS application. It embeds the `Shared` framework from `:app:shared`.
- [app/desktopApp](app/desktopApp): JVM desktop launcher and packaging.
- [app/webApp](app/webApp): Wasm JS browser application.
- [app/shared](app/shared): shared Compose application root, adaptive navigation shell, platform-aware DI aggregation, and iOS `MainViewController`.

Core modules:

- `:core:domain`: shared domain contracts and pure abstractions.
- `:core:data`: shared data infrastructure placeholder for cross-feature data utilities.
- `:core:presentation`: shared presentation helpers.
- `:core:ui`: shared Compose UI primitives and adaptive layout helpers.

Feature modules:

- `:feature:chat:{domain,data,presentation,ui}`
- `:feature:agent:{domain,data,presentation,ui}`
- `:feature:settings:{domain,data,presentation,ui}`

Each feature follows the same dependency direction:

```text
ui -> presentation -> domain
data -> domain
app/shared -> feature ui + feature data + feature presentation + core ui
```

Domain modules stay free of Compose, Koog, platform APIs, and data implementations. UI modules contain Compose screens and previews. Presentation modules expose state and actions, with state/action models kept separate from ViewModel files.

## Current Features

- Adaptive navigation shell with bottom navigation on narrow screens and navigation rail on wider screens.
- Chat screen with in-memory conversation state and single-turn Ollama or on-device execution where available.
- Agent provider configuration for Ollama, On-device, and a guarded OpenAI placeholder.
- Ollama host, port, and model editing with a connection test button.
- Ollama model discovery from the server through `/api/tags`, with selectable returned models.
- Android emulator Ollama defaults resolve to `10.0.2.2:11434`; other targets default to `127.0.0.1:11434`.
- Ollama is the default selected provider and starts configured with model `qwen3.5:0.8b`.
- On-device provider support for Android API 26+ through ML Kit Prompt API/Gemini Nano and iOS 26+ through an Apple Foundation Models Swift bridge.
- Selected provider, Ollama endpoint fields, model names, and settings selection are persisted with Multiplatform Settings.
- Settings screen with MaterialThemePrefs theme switching.
- Settings screen integrates agent configuration state so local Ollama host, port, and model can be edited from Settings.
- Compose resources for visible app and screen strings.
- Koog integration isolated in `:feature:agent:data`.
- Koin Compose owns Android `Context` registration; platform-specific DI modules provide Android emulator-aware Ollama defaults.
- Kronos network time sync called directly from supported platform entry points:
  - Android `MainApplication`: `Clock.Network.sync(applicationContext)`
  - Desktop JVM `main`: `Clock.Network.sync()`

## Agent Configuration

The Agents tab lists available providers and provider status. Editable provider settings live under Settings > Agents.

Ollama configuration supports:

- Host or IP address input.
- Port input.
- Model input and server model loading from Ollama.
- Connection test through the data-layer `AgentConnectionTester`.
- Android emulator localhost detection. Emulator builds default to `10.0.2.2:11434`; other targets default to `127.0.0.1:11434`.
- Real single-turn chat execution through Koog.

OpenAI is present as a provider placeholder. Secure provider credentials means API tokens and other hosted-provider secrets that need platform secure storage, such as Android Keystore-backed storage, iOS Keychain, and a desktop secure store. Ollama does not need those credentials for local execution, but hosted providers should not be enabled until secret storage is designed and implemented.

On-device configuration has no endpoint form because execution is owned by the platform. Android keeps the app min SDK at `24`, but Gemini Nano execution is available only on Android 8.0/API 26 or newer and may trigger ML Kit's framework-managed model download flow before the first response. iOS uses a Swift `OnDevicePromptApi` implementation backed by `FoundationModels.LanguageModelSession` when the host runs on iOS 26 or newer. `iOSApp` registers that implementation with the shared Apple on-device provider before Compose starts. Desktop JVM and Web/Wasm list the provider as unavailable.

## Key Libraries

- Kotlin `2.3.21`
- Compose Multiplatform `1.11.0`
- Material 3 `1.11.0-alpha07`
- Android Gradle Plugin `9.2.1`
- Android min SDK `24`
- Koin `4.2.1`
- Koog `1.0.0`
- ML Kit Prompt API `1.0.0-beta2`
- Ktor BOM `3.5.0`
- Multiplatform Settings `1.3.0`
- MaterialThemePrefs `1.0.0`
- Kronos Multiplatform `0.0.2`

## Running Apps

Android debug build:

```bash
./gradlew :app:androidApp:assembleDebug
```

Desktop compile check:

```bash
./gradlew :app:desktopApp:mainClasses
```

Desktop run:

```bash
./gradlew :app:desktopApp:run
```

Web/Wasm development build:

```bash
./gradlew :app:webApp:wasmJsBrowserDevelopmentWebpack
```

Web/Wasm development run:

```bash
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun
```

iOS framework check:

```bash
./gradlew :app:shared:linkDebugFrameworkIosSimulatorArm64
```

iOS app:

Open [app/iosApp](app/iosApp) in Xcode and run the app target.

## Tests And Validation

Shared metadata compile:

```bash
./gradlew :app:shared:metadataCommonMainClasses
```

Feature UI metadata compile:

```bash
./gradlew :feature:chat:ui:metadataCommonMainClasses \
  :feature:settings:ui:metadataCommonMainClasses \
  :feature:agent:ui:metadataCommonMainClasses
```

Domain and presentation layer tests:

```bash
./gradlew :core:domain:allTests \
  :core:presentation:allTests \
  :feature:agent:domain:allTests \
  :feature:agent:presentation:allTests \
  :feature:chat:domain:allTests \
  :feature:chat:presentation:allTests \
  :feature:settings:domain:allTests \
  :feature:settings:presentation:allTests
```

Broad platform smoke check:

```bash
./gradlew :app:androidApp:assembleDebug \
  :app:desktopApp:mainClasses \
  :app:webApp:wasmJsBrowserDevelopmentWebpack \
  :app:shared:linkDebugFrameworkIosSimulatorArm64
```

## Development Notes

- Prefer common Kotlin and Compose code whenever platform APIs are not required.
- Keep platform-specific APIs behind platform entry points, source sets, or expect/actual abstractions.
- Do not call Koog directly from UI or presentation modules; use `AgentRuntime` and related domain contracts.
- Use Koin-backed platform modules for platform-aware data entities. Do not pass Android context manually through `App()`.
- Do not route Kronos through DI. Use the library directly from supported platform startup code.
- Keep user-facing text in Compose resources.
- Add previews for new composable screens and prefer stateless previewable content composables around ViewModel-backed screen wrappers.

## Documentation

- [System design](docs/system-design.md)
- [Architecture](docs/architecture.md)
- [Development approach](docs/development-approach.md)
- [Code style](docs/code-style.md)
- [Provider configuration](docs/provider-configuration.md)
- [Testing](docs/testing.md)
- [Roadmap](docs/roadmap.md)
