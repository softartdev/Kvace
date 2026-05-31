# Architecture

Kvace follows the same practical Clean Architecture direction used in NoteDelight, with ktLAN-style common adaptive Compose screens where that keeps platform logic out of UI code.

## Module Groups

- `app/*`: platform application entry points and the shared composition root.
- `core/domain`: cross-feature pure contracts such as dispatcher abstractions.
- `core/data`: shared data infrastructure such as persistent settings factories.
- `core/presentation`: shared presentation helpers.
- `core/ui`: shared Compose primitives and adaptive layout helpers.
- `feature/<name>/domain`: models, repository interfaces, use cases, and domain abstractions.
- `feature/<name>/data`: repository implementations, provider integrations, DTOs, and platform data sources.
- `feature/<name>/presentation`: ViewModels, UI state, actions, and presentation logic.
- `feature/<name>/ui`: Compose screens, sections, components, and previews.

## Dependency Direction

```text
ui -> presentation -> domain
data -> domain
app/shared -> feature ui + feature presentation + feature data
```

Domain modules do not depend on Compose, Koog, Koin, platform APIs, or data implementations. UI modules do not depend on data modules. Feature UI modules should not depend on other feature UI modules.

## ViewModel Rules

- ViewModels expose immutable `StateFlow`.
- UI events are named actions, for example `ChatAction`.
- UI state and action models live in separate state files, for example `ChatState.kt`.
- ViewModels do not start long-running work in `init`; screen wrappers call explicit functions from `LaunchedEffect`.
- ViewModels receive `CoroutineDispatchers` and `Logger` through Koin.
- Compose screens use `koinViewModel`.

## Provider Boundary

The domain-facing provider boundary is `AgentRuntime`. Koog types such as executors, clients, agents, models, and tools must stay in `:feature:agent:data`.
