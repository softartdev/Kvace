# Kvace Roadmap

This roadmap starts from the current KMP + Compose architecture after enabling persisted provider settings, persisted chat history, and minimal real Ollama execution.

## Current Baseline

- Modular KMP structure with `app`, `core`, and `feature` modules.
- Shared Compose root with adaptive navigation for Workspace, Providers, and Settings.
- Koin-based composition root with platform modules for Android, iOS, Desktop JVM, and Wasm.
- Workspace feature with SQLDelight-backed conversation history, adaptive master-detail layout, stop generation, message actions, and single-turn Ollama execution, plus on-device execution where platform APIs are available.
- Providers feature with adaptive provider configuration, provider-specific endpoint/model validation, persisted
  validation markers, and Koog isolated in data.
- Settings feature with MaterialThemePrefs theme switching, Harness prompt editing, and About information.
- Ollama host/port connection testing and server model loading through data-layer clients.
- Ollama `shell_command` tool calling for one read-only allowlisted command per send. Desktop JVM executes through
  `ProcessBuilder`; Android, iOS, and Web/Wasm return unsupported results until platform bridges exist.
- On-device provider selection with Android ML Kit Prompt API, an iOS Swift bridge, and a macOS JVM Swift helper for
  Apple Foundation Models.
- OpenAI and OpenAI-compatible execution with configurable endpoint/model, secure credentials on native targets,
  session-only browser credentials, streaming responses, and typed failures.
- Android emulator localhost handling through emulator detection, defaulting Ollama to `10.0.2.2:11434`.
- Multiplatform Settings persistence for selected provider, provider endpoint/model fields, app settings selection, and Wasm browser storage.
- Harness prompt persistence through Multiplatform Settings.
- Chat history persists across restarts on Android, iOS, and Desktop JVM. Web/Wasm chat history is session-only.
- Domain, data, and presentation tests for chat, agent, and settings behavior.

## Phase 1: Stabilize The Foundation

- [x] Keep feature modules buildable across Android, iOS, Desktop JVM, and Wasm.
- [x] Add focused tests for `AgentConfigViewModel` provider selection and `OllamaEndpointSettingsViewModel` endpoint/model transitions.
- [x] Add fake `AgentConnectionTester` and `AgentModelCatalog` coverage for success, failure, invalid host, invalid port, and model selection paths.
- [x] Clean up preview coverage for Settings, Providers, and Workspace states.
- [x] Keep preference persistence backed by platform-specific Multiplatform Settings factories, including browser `StorageSettings` for Wasm.

Validation gate:

```bash
./gradlew :core:domain:allTests \
  :core:presentation:allTests \
  :feature:agent:presentation:allTests \
  :feature:chat:domain:allTests \
  :feature:chat:presentation:allTests \
  :feature:settings:domain:allTests \
  :feature:settings:presentation:allTests \
  :app:androidApp:assembleDebug \
  :app:desktopApp:mainClasses \
  :app:webApp:wasmJsBrowserDevelopmentWebpack \
  :app:shared:linkDebugFrameworkIosSimulatorArm64
```

## Phase 2: Provider Configuration Depth

- [x] Add provider editing for model names beyond the current defaults. Ollama model loading and OpenAI-compatible model editing are implemented.
- [x] Add provider-specific validation for model names and endpoints. Ollama endpoints are normalized and models must
  come from the server catalog; OpenAI-compatible endpoints are normalized and require successful credential/model verification.
- [x] Add secure API-key storage for OpenAI and OpenAI-compatible endpoints.
- [x] Keep secrets out of persisted common UI state; Web/Wasm API keys remain an opt-in in-memory CORS-gated experiment.
- [x] Add provider reset behavior after the persistence model is stable.

## Kvace 1.0.0 Release & Distribution (before Phase 3)

- [ ] Publish the first stable `1.0.0` release on Google Play Production, App Store, GitHub Releases, and GitHub Pages.
- [ ] Produce deterministic marketing screenshots from Compose Preview through Android CLI and verify them with Compose Hot Reload MCP.
- [ ] Publish signed/notarized macOS arm64/x64, Linux arm64/x64, and Windows x64 Desktop installers plus matching JARs.
- [ ] Complete privacy, support, manual install/build, store metadata, and release documentation.

## Phase 3: Richer Agent Execution

- Expand the current minimal single-turn Ollama flow.
- [x] Continue mapping domain-level provider configuration to Koog client/model configuration inside `:feature:agent:data`.
- Keep Koog types out of domain, presentation, and UI modules.
- [x] Add the first constrained agent tool call path. `shell_command` is implemented for Desktop JVM with read-only
  allowlist and unsupported placeholders on other targets.
- [x] Stream or incrementally publish assistant output through domain events.
- [x] Surface typed user-facing errors for missing provider configuration, credentials, locked storage, network/CORS,
  authentication, unavailable model, and request failure. Compose UI localizes the error type through resources.

## Phase 4: Chat Workspace

- [x] Persist conversations and messages with local SQLDelight chat history.
- [x] Add conversation list, rename/delete actions, and empty/error states. Conversation list, empty states, rename/delete, and first-message auto-title are implemented.
- Support provider/model selection per conversation.
- Add markdown/code rendering only after basic persistence and execution paths are reliable.
- [x] Add cancellation for in-flight agent responses. Stop generation is implemented.
- Add retry for failed agent responses.

## Phase 5: Platform-Specific Capabilities

- [x] Add Apple Foundation Models to Desktop JVM on Apple Silicon macOS 26+ through a signed, packaged Swift helper,
  with process isolation, cancellation, resource extraction, and Compose Hot Reload support.
- Desktop JVM: expand beyond the current read-only `shell_command` tool only after the safety model is proven.
- Android: implement the planned Termux bridge for terminal-backed capabilities.
- iOS and Web/Wasm: keep terminal features disabled or provide remote/limited abstractions where platform rules require it.
- Keep platform tool APIs out of shared UI and domain models unless represented as stable cross-platform contracts.

## Phase 6: Production Hardening

- Add structured logging around provider execution and connection tests.
- Add error analytics hooks behind a common abstraction if needed.
- Improve accessibility labels and keyboard navigation for desktop/web.
- Add release packaging checks for Desktop, Android, and iOS.
- Document provider setup, local Ollama setup, and emulator networking behavior for end users.

## Non-Goals For The Near Term

- Full multi-agent orchestration.
- Terminal automation on every platform.
- Browser-side storage of hosted-provider API keys.
- Complex database, sync, or account systems before the local single-user workflow is solid.
