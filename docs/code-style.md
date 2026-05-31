# Code Style

## Kotlin

- Prefer clear names over abbreviations.
- Keep code ASCII unless a file already uses non-ASCII for a specific reason.
- Keep comments rare and useful; explain non-obvious decisions, not syntax.
- Use constructor injection for ViewModels, repositories, runtimes, and testers.
- Prefer `factoryOf`, `singleOf`, and `viewModelOf` with constructor references in Koin modules.

## Presentation

- State files contain UI state and action declarations.
- ViewModel files contain behavior only.
- Actions are named `FeatureAction`, not `FeatureEvent`.
- Avoid work in `init`; use explicit `observe...` or `load...` functions called from `LaunchedEffect`.
- Inject `CoroutineDispatchers` and use the correct dispatcher for IO or background work.
- Inject Kermit `Logger` for operational errors.

## Compose

- Provide previews for composable screens and major stateful content variants.
- Keep ViewModel-backed wrappers thin and place previewable UI in stateless content composables.
- Keep user-facing strings in Compose resources.
- Use Material 3 components and existing core UI primitives.
- Keep UI modules independent from data modules.

## Data

- Keep external SDK types in data modules.
- Map external errors to domain-level events or errors before they cross into presentation.
- Persist small preferences through Multiplatform Settings.
- Do not persist hosted-provider secrets until secure storage is implemented.
