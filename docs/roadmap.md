# Kvace Roadmap

This roadmap starts from the current KMP + Compose architecture after enabling persisted provider settings, persisted chat history, and minimal real Ollama execution.

## Current Baseline

- Modular KMP structure with `app`, `core`, and `feature` modules.
- Shared Compose root with adaptive navigation for Workspace, Providers, and Settings.
- Koin-based composition root with platform modules for Android, iOS, Desktop JVM, and Wasm.
- Workspace feature with SQLDelight-backed conversation history, adaptive master-detail layout, stop generation, message actions, and single-turn Ollama execution, plus on-device execution where platform APIs are available.
- Providers feature with adaptive provider configuration, provider domain contracts, persisted provider/model configuration, and Koog isolated in data.
- Settings feature with MaterialThemePrefs theme switching, Harness prompt editing, and About information.
- Ollama host/port connection testing and server model loading through data-layer clients.
- Ollama `shell_command` tool calling for one read-only allowlisted command per send. Desktop JVM executes through
  `ProcessBuilder`; Android, iOS, and Web/Wasm return unsupported results until platform bridges exist.
- On-device provider selection with Android ML Kit Prompt API and an iOS Swift bridge for Apple Foundation Models.
- Android emulator localhost handling through emulator detection, defaulting Ollama to `10.0.2.2:11434`.
- Multiplatform Settings persistence for selected provider, provider endpoint/model fields, app settings selection, and Wasm browser storage.
- Harness prompt persistence through Multiplatform Settings.
- Chat history persists across restarts on Android, iOS, and Desktop JVM. Web/Wasm chat history is session-only.
- Domain, data, and presentation tests for chat, agent, and settings behavior.

## Phase 1: Stabilize The Foundation

- Keep feature modules buildable across Android, iOS, Desktop JVM, and Wasm.
- Add focused tests for `AgentConfigViewModel` provider selection and `OllamaEndpointSettingsViewModel` endpoint/model transitions.
- Add fake `AgentConnectionTester` and `AgentModelCatalog` coverage for success, failure, invalid host, invalid port, and model selection paths.
- Clean up preview coverage for Settings, Providers, and Workspace states.
- Keep preference persistence backed by platform-specific Multiplatform Settings factories, including browser `StorageSettings` for Wasm.

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

- Add provider editing for model names beyond the current defaults. Basic model editing and Ollama model loading are implemented.
- Add provider-specific validation for model names and endpoints.
- Add secure API-key storage design for OpenAI and other hosted providers.
- Keep secrets out of common UI and avoid browser-side secret storage for real hosted-provider execution.
- Add import/export or reset behavior only after the persistence model is stable.

## Phase 3: Richer Agent Execution

- Expand the current minimal single-turn Ollama flow.
- Continue mapping domain-level provider configuration to Koog client/model configuration inside `:feature:agent:data`.
- Keep Koog types out of domain, presentation, and UI modules.
- Add the first constrained agent tool call path. `shell_command` is implemented for Desktop JVM with read-only
  allowlist and unsupported placeholders on other targets.
- Stream or incrementally publish assistant output through domain events.
- Surface typed user-facing errors for missing provider configuration, network failure, auth failure, and model failure.

## Phase 4: Chat Workspace

- Persist conversations and messages. Partially implemented with local SQLDelight chat history.
- Add conversation list, rename/delete actions, and empty/error states. Conversation list, empty states, rename/delete, and first-message auto-title are implemented.
- Support provider/model selection per conversation.
- Add markdown/code rendering only after basic persistence and execution paths are reliable.
- Add cancellation and retry for in-flight agent responses. Stop generation is implemented; retry is still pending.

## Phase 5: Platform-Specific Capabilities

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
