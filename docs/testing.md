# Testing

## Current Coverage

- Domain and presentation behavior for the current agent, chat, and settings contracts.
- Data persistence ordering and repository behavior, including SQLDelight chat storage on JVM.
- Router attachment, release, detached no-op behavior, and top-level options.
- Android UI coverage for repeated top-level navigation, back behavior, and the typed theme dialog.
- The live Ollama instrumentation test is opt-in and excluded from the deterministic suite.

## Focused Layer Test Command

```bash
./gradlew :core:domain:allTests \
  :core:presentation:allTests \
  :feature:agent:domain:allTests \
  :feature:agent:data:allTests \
  :feature:agent:presentation:allTests \
  :feature:chat:domain:allTests \
  :feature:chat:data:allTests \
  :feature:chat:presentation:allTests \
  :feature:settings:domain:allTests \
  :feature:settings:data:allTests \
  :feature:settings:presentation:allTests \
  :app:shared:allTests
```

## Android UI Tests

The normal suite is deterministic and does not require Ollama:

```bash
./gradlew :app:androidApp:connectedDebugAndroidTest
```

Enable the live Ollama test explicitly:

```bash
./gradlew :app:androidApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.runLiveOllamaTests=true
```

## Compose Screenshot Previews

Android Studio can show commonMain previews, but `android-cli` screenshot rendering can only render previews declared in an Android source set. Renderable screenshot previews live in:

```text
app/shared/src/androidMain/kotlin/com/softartdev/kvace/preview/ScreenshootPreview.kt
```

The preview functions in that file must only call original feature composables. Put sample state in adjacent
`PreviewParameterProvider` classes and pass it through `@PreviewParameter`; do not duplicate UI functions for screenshot
rendering.

Example commands:

```bash
android studio render-compose-preview \
  --project=/Users/artur/AndroidStudioProjects/Kvace \
  --output-image-file=/private/tmp/kvace-workspace-wide.png \
  app/shared/src/androidMain/kotlin/com/softartdev/kvace/preview/ScreenshootPreview.kt \
  ScreenshootWorkspaceWidePreview

android studio render-compose-preview \
  --project=/Users/artur/AndroidStudioProjects/Kvace \
  --output-image-file=/private/tmp/kvace-providers-wide.png \
  app/shared/src/androidMain/kotlin/com/softartdev/kvace/preview/ScreenshootPreview.kt \
  ScreenshootProvidersWidePreview

android studio render-compose-preview \
  --project=/Users/artur/AndroidStudioProjects/Kvace \
  --output-image-file=/private/tmp/kvace-settings-harness.png \
  app/shared/src/androidMain/kotlin/com/softartdev/kvace/preview/ScreenshootPreview.kt \
  ScreenshootSettingsHarnessPreview

android studio render-compose-preview \
  --project=/Users/artur/AndroidStudioProjects/Kvace \
  --output-image-file=/private/tmp/kvace-settings-libraries.png \
  app/shared/src/androidMain/kotlin/com/softartdev/kvace/preview/ScreenshootPreview.kt \
  ScreenshootSettingsLibrariesPreview
```

Current screenshot preview entry points cover Workspace wide/compact/sending/placeholder/long-chat/rename/delete,
Providers wide/compact/OpenAI, and Settings Harness/Libraries/About.

The Settings Libraries preview uses the real AboutLibraries `LibrariesContainer`. If Android Studio's renderer reports
`NoClassDefFoundError: com/mikepenz/aboutlibraries/ui/compose/ExtensionsKt`, treat it as an Android Studio preview
classpath issue after confirming Gradle Android compilation succeeds. Do not work around it by copying the Libraries UI
into the screenshot preview file.

Run Web/Wasm checks from the repository root:

```bash
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun
```

If `render-compose-preview` reports stale bytecode errors after model or enum changes, first run the relevant AndroidMain compile task with `--rerun-tasks`. If Android Studio still renders old classes, restart Android Studio and rerun the command; do not work around the cache by copying original composables into the screenshot preview file.

After dependency changes, refresh the bundled AboutLibraries metadata:

```bash
./gradlew :app:shared:exportLibraryDefinitions
```

## Full Build

```bash
./gradlew build
```

## Test Style

- Test domain behavior without Compose.
- Test presentation behavior with fake repositories, fake testers/catalogs, and test dispatchers.
- Use one `TestCoroutineScheduler` for all dispatchers in a test.
- Verify cancellation separately when a component catches failures from suspend work.
- Do not add UI module dependencies to presentation tests.
- Keep UI tests separate from the unit test layer.
