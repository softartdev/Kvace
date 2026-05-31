# Testing

## Current Coverage

Domain and presentation layers have focused unit tests for:

- agent provider models and configuration presentation behavior
- on-device provider defaults, platform availability flags, and custom Koog client behavior
- Ollama endpoint validation, connection-test state transitions, and model selection/loading behavior
- chat send-message use case mapping for assistant, tool, error, and blank input paths
- chat ViewModel state updates and send actions
- settings domain defaults and settings ViewModel section selection

UI tests are intentionally deferred and should cover navigation, adaptive layouts, theme selection, and provider forms later.

## Focused Layer Test Command

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

## Full Build

```bash
./gradlew build
```

## Test Style

- Test domain behavior without Compose.
- Test presentation behavior with fake repositories, fake testers/catalogs, and test dispatchers.
- Do not add UI module dependencies to presentation tests.
- Keep UI tests separate from the unit test layer.
