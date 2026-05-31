# Development Approach

Kvace should grow in small buildable steps. Each step should keep Android, iOS, Desktop JVM, and Wasm source sets compiling unless a target is explicitly unsupported.

## Implementation Priorities

- Prefer common Kotlin and Compose code.
- Add platform source-set code only when a platform API is unavoidable.
- Keep module boundaries simple and explicit.
- Add abstractions only when they protect a real boundary, such as provider runtimes, settings storage, dispatchers, or platform-specific networking.
- Keep the first implementation narrow, then broaden after validation.

## Adaptive UI

The root shell chooses bottom navigation for narrow layouts and a navigation rail for wider layouts. Settings uses a master-detail pattern so the selected section and details can sit side by side on large screens while remaining navigable on phones.

The approach mirrors ktLAN’s common adaptive screen structure and NoteDelight’s settings split-screen pattern.

## Provider Rollout

Ollama remains the default provider because it can run locally without API credentials. Settings can load available Ollama models from the configured server and persist the selected model. On-device providers also avoid hosted credentials, but must stay behind platform adapters because Android and Apple expose different native APIs. Hosted providers should be added only after platform secure credential storage is available.

## Platform Notes

- Android emulator localhost must use `10.0.2.2`.
- Android min SDK stays `24`. Koog `1.0.0` Android artifacts currently declare min SDK `35`, and ML Kit GenAI artifacts also need manifest override handling, so the Android app manifest temporarily uses `tools:overrideLibrary` while runtime calls remain guarded by platform checks.
- Desktop and iOS simulator local Ollama normally use `127.0.0.1`.
- Web/Wasm local Ollama may require CORS configuration and is expected to be less reliable until browser-specific provider rules are finalized.
- Kronos is used directly from platform startup code, not through DI.
