# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Android
./gradlew :app:androidApp:assembleDebug

# Desktop
./gradlew :app:desktopApp:run

# Web (Wasm)
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun

# iOS framework (simulator)
./gradlew :app:shared:linkDebugFrameworkIosSimulatorArm64
```

## Testing

```bash
# All affected shared tests
./gradlew :core:domain:allTests :core:presentation:allTests \
  :feature:agent:domain:allTests :feature:agent:data:allTests :feature:agent:presentation:allTests \
  :feature:chat:domain:allTests :feature:chat:data:allTests :feature:chat:presentation:allTests \
  :feature:settings:domain:allTests :feature:settings:data:allTests :feature:settings:presentation:allTests \
  :app:shared:allTests

# Single module
./gradlew :feature:chat:domain:allTests

# Deterministic Android UI tests
./gradlew :app:androidApp:connectedDebugAndroidTest

# Full build
./gradlew build
```

The live Ollama instrumentation test is disabled unless
`-Pandroid.testInstrumentationRunnerArguments.runLiveOllamaTests=true` is supplied.

## Architecture

Clean Architecture with strict dependency direction:

```
ui → presentation → domain
data → domain
app/shared → feature ui + feature presentation + feature data
```

Each feature (`chat`, `agent`, `settings`) is split into four modules: `domain`, `data`, `presentation`, `ui`. Cross-feature shared code lives under `core/`.

**Hard constraints:**
- `domain` modules: no Compose, Koin, Koog, or platform APIs
- `ui` modules: no `data` module dependencies, no cross-feature UI dependencies
- Koog types (executors, agents, models, tools) stay in `:feature:agent:data` only; the domain-facing contract is `AgentRuntime`

## Key Technologies

| Concern | Library |
|---|---|
| Language | Kotlin 2.4.0 |
| UI | Compose Multiplatform 1.11.1 + Material 3 |
| Navigation | Navigation Compose 2.9.2 with `@Serializable` typed routes |
| DI | Koin 4.2.1 Safe DSL (`singleOf`, `factoryOf`, `viewModelOf`) |
| HTTP | Ktor 3.5.0 (CIO engine on Android) |
| Persistence | Multiplatform Settings 1.3.0 |
| Logging | Kermit 2.1.0 |
| AI runtime | Koog 1.0.0 (Ollama + on-device), isolated in `:feature:agent:data` |
| On-device AI | ML Kit Prompt API (Android), Apple Foundation Models via Swift bridge (iOS 26+) |
| Time | Kronos 0.0.2 (called at platform startup, not via DI) |

## ViewModel / Presentation Pattern

- ViewModels expose immutable `StateFlow<FeatureUiState>` using Kotlin explicit backing fields
- Use a `FeatureAction` type when a screen has multiple UI events; use a direct callback for one event
- Closely related models and presentation state/action/status types may share a focused file
- No work in `init`; stateful screen overloads call explicit `load...` / `observe...` functions from `LaunchedEffect`
- ViewModels receive their functional dependencies and dispatchers through constructor injection
- Kermit loggers are declared in the class body with a class-specific tag
- Broad suspend-work catches call `currentCoroutineContext().ensureActive()` before handling a failure

## Compose Conventions

- Keep stateful and stateless screen overloads in the same file
- Stateful overloads receive a ViewModel, collect state, run screen-owned effects, and delegate to the stateless overload
- `:app:shared` destinations own `koinViewModel`
- Provide `@Preview` for screens and major content variants
- Use `PreviewParameterProvider` for previews with substantial sample state
- User-facing strings go in Compose resources
- Use Material 3 adaptive components such as `NavigationSuiteScaffold` instead of custom window-width switching

## Data Layer Rules

- Keep external SDK types (Koog, Ktor, ML Kit) inside `data` modules
- Use cancellation-aware `try/catch` around suspend I/O; never map cancellation to a failure
- Persist successfully before publishing a new repository `StateFlow` value
- Small preferences via Multiplatform Settings; do not persist hosted-provider secrets (secure storage not yet implemented)
- `AgentRuntime` is the only Koog-aware contract that `presentation` may reference

## Application Infrastructure

- `Router` and `SnackbarInteractor` are UI-independent contracts in `:core:presentation`
- `ComposeRouter` and `ComposeSnackbarInteractor` are singleton implementations in `:app:shared`
- Bind and release navigation, snackbar host, clipboard, and UI scope with `DisposableEffect`
- Keep durable connection/model errors inline; reserve snackbar infrastructure for transient messages
- Shared bindings live in one `kvaceModule`; platform bindings remain in `kvacePlatformModule`
