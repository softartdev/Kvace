# AGENTS.md

This file provides repository-wide guidance for AI coding agents working in Kvace.

## Build & Run Commands

```bash
# Android
./gradlew :app:androidApp:assembleDebug

# Desktop
./gradlew :app:desktopApp:run

# Desktop with Compose Hot Reload (for live UI validation through MCP)
./gradlew :app:desktopApp:hotRun

# macOS ARM64 Apple Foundation Models helper (requires Xcode 26+)
./gradlew :app:desktopApp:verifyMacOsFoundationModelsBridge

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
| Language | Kotlin 2.4.10 |
| UI | Compose Multiplatform 1.12.0 + Compose Material3 1.9.0 (stable) |
| Navigation | Navigation Compose 2.9.2 with `@Serializable` typed routes |
| DI | Koin 4.2.2 Safe DSL (`singleOf`, `factoryOf`, `viewModelOf`) |
| HTTP | Ktor 3.5.2 (CIO engine on Android) |
| Persistence | Multiplatform Settings 1.3.0 |
| Logging | Kermit 2.1.0 |
| AI runtime | Koog 1.2.0 (Ollama + on-device), isolated in `:feature:agent:data` |
| On-device AI | ML Kit Prompt API (Android), Apple Foundation Models via Swift bridges (iOS/Catalyst and macOS 26+ JVM helper) |
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

## Visual UI Validation for Agents

Do not infer the rendered UI from Compose source alone. Validate a UI change with the smallest suitable visual tool:

- Use Android CLI screenshot previews for an isolated stateless Composable and its representative states. Render only
  the Android-source-set preview functions under
  `app/shared/src/androidMain/kotlin/com/softartdev/kvace/preview`; they must delegate to the original feature
  Composables and use adjacent `PreviewParameterProvider` sample state.
- Use Compose Hot Reload MCP for a live Desktop JVM application when a change depends on navigation, window size,
  interactions, remembered state, cross-screen behavior, or the real composition. It complements Preview rendering;
  it does not replace it.

The desktop module already applies `org.jetbrains.compose.hot-reload`. Compose Hot Reload MCP is experimental and
works with the Desktop JVM application, not Android, iOS, or Web/Wasm targets.

### Compose Hot Reload MCP workflow

1. Ensure the agent host registers the `compose-hot-reload` MCP server from the repository root. For Codex, the
   one-time user configuration is:

   ```toml
   [mcp_servers.compose-hot-reload]
   command = "./gradlew"
   args = ["--no-daemon", "--quiet", "--console=plain", "hotMcpServer"]
   ```

   MCP tools are discovered when a task starts. After adding or changing this configuration, start a new task in this
   repository before expecting the tools to appear.
2. Start the live app with `./gradlew :app:desktopApp:hotRun` and keep that process running. The agent owns the MCP
   server lifecycle; the server waits for the app and connects automatically.
3. Call `status` and require `connected: true`. After a source edit, call `reload`, then check that reload completed,
   there is no `lastError`, and no window is reporting a UI error.
4. Call `list_windows`, then inspect the target with `get_semantic_tree` and `take_screenshot`. Use semantic node IDs
   for `click`, `type_text`, and scrolling; after an interaction, inspect the changed state and take another
   screenshot when visual output matters.
5. Use `get_logs` or `get_ui_error` to diagnose a failed render. Do not claim visual verification succeeded if the
   app is disconnected, reloading, or has a UI error.

MCP interaction can mutate application state. Restrict clicks, typing, and reset/restart actions to the requested UI
scenario and safe test data; treat destructive, persistent, network, or provider actions as requiring the same care
as ordinary application-side effects.

## Data Layer Rules

- Keep external SDK types (Koog, Ktor, ML Kit) inside `data` modules
- Keep the macOS JVM Foundation Models process protocol and lifecycle inside `:feature:agent:data`; the Swift helper
  source and packaging belong to `:app:desktopApp`
- Use cancellation-aware `try/catch` around suspend I/O; never map cancellation to a failure
- Persist successfully before publishing a new repository `StateFlow` value
- Take the On-device model label from `OnDeviceModelProvider`; it is platform-owned and must not be read from or
  written to Settings
- Small preferences via Multiplatform Settings; do not persist hosted-provider secrets (secure storage not yet implemented)
- `AgentRuntime` is the only Koog-aware contract that `presentation` may reference

## Application Infrastructure

- `Router` and `SnackbarInteractor` are UI-independent contracts in `:core:presentation`
- `ComposeRouter` and `ComposeSnackbarInteractor` are singleton implementations in `:app:shared`
- Bind and release navigation, snackbar host, clipboard, and UI scope with `DisposableEffect`
- Keep durable connection/model errors inline; reserve snackbar infrastructure for transient messages
- Shared bindings live in one `kvaceModule`; platform bindings remain in `kvacePlatformModule`
- When a contract has target-specific behavior or unavailable text, give it a named implementation in the owning
  platform source set, including unsupported adapters such as `Ios...` or `Web...`; keep platform messages and behavior
  out of app DI declarations
- Bind platform implementations with `singleOf(::PlatformImplementation) bind Contract::class` when every constructor
  parameter is a Koin dependency. Kotlin default parameters are still treated as dependencies by constructor-reference
  bindings, so use an explicit production constructor or a narrow provider function when configuration values are not
  supplied by Koin
