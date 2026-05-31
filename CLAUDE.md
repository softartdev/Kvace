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
# All unit tests (domain + presentation layers)
./gradlew :core:domain:allTests \
  :core:presentation:allTests \
  :feature:agent:domain:allTests \
  :feature:agent:presentation:allTests \
  :feature:chat:domain:allTests \
  :feature:chat:presentation:allTests \
  :feature:settings:domain:allTests \
  :feature:settings:presentation:allTests

# Single module
./gradlew :feature:chat:domain:allTests

# Full build
./gradlew build
```

UI tests are intentionally deferred; only domain and presentation layers have unit tests.

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
| UI | Compose Multiplatform 1.11.0 + Material 3 |
| Navigation | Navigation Compose 2.9.2 with `@Serializable` typed routes |
| DI | Koin 4.2.1 Safe DSL (`singleOf`, `factoryOf`, `viewModelOf`) |
| HTTP | Ktor 3.5.0 (CIO engine on Android) |
| Persistence | Multiplatform Settings 1.3.0 |
| Logging | Kermit 2.1.0 |
| AI runtime | Koog 1.0.0 (Ollama + on-device), isolated in `:feature:agent:data` |
| On-device AI | ML Kit Prompt API (Android), Apple Foundation Models via Swift bridge (iOS 26+) |
| Time | Kronos 0.0.2 (called at platform startup, not via DI) |

## ViewModel / Presentation Pattern

- ViewModels expose immutable `StateFlow<FeatureState>`
- UI events are named `FeatureAction` (not `FeatureEvent`)
- State and action models live in a separate `FeatureState.kt` file; behavior only in `FeatureViewModel.kt`
- No work in `init`; screen wrappers call explicit `load...` / `observe...` functions from `LaunchedEffect`
- ViewModels receive `CoroutineDispatchers` and Kermit `Logger` via constructor injection
- Screens call `koinViewModel()` to obtain ViewModels

## Compose Conventions

- Split each screen into a thin ViewModel-backed wrapper and a stateless content composable
- Provide `@Preview` for screens and major content variants
- User-facing strings go in Compose resources
- Use Material 3 components and existing `core:ui` primitives for adaptive layouts

## Data Layer Rules

- Keep external SDK types (Koog, Ktor, ML Kit) inside `data` modules; map errors to domain types before crossing into `presentation`
- Small preferences via Multiplatform Settings; do not persist hosted-provider secrets (secure storage not yet implemented)
- `AgentRuntime` is the only Koog-aware contract that `presentation` may reference
