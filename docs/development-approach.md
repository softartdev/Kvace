# Development Approach

Kvace should grow in small buildable steps. Each step should keep Android, iOS, Desktop JVM, and Wasm source sets compiling unless a target is explicitly unsupported.

## Implementation Priorities

- Prefer common Kotlin and Compose code.
- Add platform source-set code only when a platform API is unavoidable.
- Keep module boundaries simple and explicit.
- Add abstractions only when they protect a real boundary, such as provider runtimes, settings storage, dispatchers, or platform-specific networking.
- Keep the first implementation narrow, then broaden after validation.

## Layer Mapping

Transformations between entities from different layers or storage formats should live in focused mapper files next to the owning implementation. Keep repositories, local data sources, use cases, ViewModels, and Composables focused on orchestration and behavior instead of embedding `data -> domain`, `domain -> presentation`, or `presentation -> UI` conversion functions inline.

## Adaptive UI

The root shell uses Material 3 `NavigationSuiteScaffold` to choose the appropriate top-level navigation presentation. Workspace, Providers, and Settings use a master-detail pattern so lists and details can sit side by side on large screens while remaining navigable on phones.

Adaptive behavior is implemented with shared Compose primitives and verified against this repository's screen and navigation contracts.

When an adaptive master-detail screen can show different detail content, the feature UI owns that detail switching inside
its detail pane. The app navigation shell should pass feature-level ViewModels and callbacks, not nested detail
Composables.

Android CLI screenshot rendering can only render previews from Android source sets. Keep renderable screenshot previews in `app/shared/src/androidMain/kotlin/com/softartdev/kvace/preview/ScreenshootPreview.kt`, and keep larger sample state in adjacent `PreviewParameterProvider` classes. Those preview functions must call the original composables from feature UI modules; do not duplicate original screen, section, or row composables for screenshots.

## Agent-Driven Visual Feedback

Agents must close every task with both Android CLI Compose Preview and Compose Hot Reload MCP validation, even when the
task only changes non-UI code or documentation. UI work validates the affected screen and representative states;
otherwise use the wide Workspace preview and initial Desktop Workspace as baseline smoke checks. Do not claim
completion when either gate is unavailable or unsuccessful. Reload the Desktop app, inspect its semantic tree, and
capture a screenshot. The full procedure and safety boundaries are documented in
[Testing](testing.md#live-desktop-ui-validation-with-compose-hot-reload-mcp).

## Provider Rollout

Ollama remains the default provider because it can run locally without API credentials. Providers can load available Ollama models from the configured server and persist the selected model. On-device providers also avoid hosted credentials, but must stay behind platform adapters because Android and Apple expose different native APIs. Hosted providers should be added only after platform secure credential storage is available.

## Platform Notes

- Android emulator localhost must use `10.0.2.2`.
- Android min SDK is `26`. Koog `1.2.0` Android artifacts currently declare min SDK `35`, and ML Kit GenAI artifacts also need manifest override handling, so the Android app manifest temporarily uses `tools:overrideLibrary` while runtime calls remain guarded by platform checks.
- Desktop and iOS simulator local Ollama normally use `127.0.0.1`.
- Web/Wasm local Ollama execution uses a direct streaming `/api/chat` request instead of Koog. It may require Ollama
  CORS configuration; CORS failures should surface as normal request failures rather than being blocked before execution.
- Web/Wasm chat history is currently session-only. Android, iOS, and Desktop JVM use file-backed SQLDelight SQLite storage.
- Run the Web/Wasm app from the repository root with `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun`. IDE Gradle run configurations should use the repository root as the external project path, not a non-existent `webApp` root directory.
- The iOS app target links SQLite explicitly with `OTHER_LDFLAGS = $(inherited) -lsqlite3` because SQLDelight native SQLite references symbols such as `_sqlite3_bind_blob`.
- Kronos is used directly from platform startup code, not through DI.

## Compose Resources

All Compose resources live in `:core:ui` to keep one public generated `Res` class. The module configures
`publicResClass = true`, `packageOfResClass = "com.softartdev.kvace.core.ui.resources"`, and
`generateResClass = always`. Feature and app modules import `com.softartdev.kvace.core.ui.resources.*` and use
`stringResource(Res.string...)` or `painterResource(Res.drawable...)` directly.

The Settings Libraries section renders `aboutlibraries.json` from `:core:ui` resources. Refresh it after dependency
changes:

```bash
./gradlew :app:shared:exportLibraryDefinitions
```
