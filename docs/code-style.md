# Code Style

## Kotlin

- Prefer clear names over abbreviations.
- Keep code ASCII unless a file already uses non-ASCII for a specific reason.
- Keep comments rare and useful; explain non-obvious decisions, not syntax.
- Leave one blank line after a class declaration before its first member.
- Use blank lines to show the local structure of the code, not by applying a fixed rule after every statement. Separate
  member groups such as dependencies, observable state, internal state, and public operations; keep members inside one
  group together.
- In function bodies, keep tightly connected preparation and execution together. Add a blank line where the reader
  benefits from seeing a transition, such as before the result or assertion section, but do not mechanically split every
  arrange, act, and assert step.
- Write a construct on one line when it fits within the line limit and remains readable. Do not expand short expressions,
  conditionals, function calls, parameter lists, or declarations solely to follow a multiline template.
- Prefer an expression body when a function consists of one readable expression, including delegation, a `when`
  expression, or a single root Composable.
- Add an explicit local type when a non-primitive inferred type is not obvious from the initializer or is important to
  the surrounding logic.
- Give a complex condition a descriptive Boolean name before passing it to another API.
- Prefer SDK and library constants over duplicated numeric constants. Do not add a companion-object constant for a
  literal that is used only once.
- Do not suppress a warning unless the warning is understood and the suppression is still necessary.
- Use constructor injection for ViewModels, repositories, runtimes, and testers.
- Prefer `factoryOf`, `singleOf`, and `viewModelOf` with constructor references in Koin modules.
- Group closely related models in a focused file; keep independent implementations and large classes separate.
- Move transformations between storage/data/domain/presentation/UI entities into focused mapper files near the owning layer. Do not hide cross-layer mapping inside repositories, use cases, ViewModels, or Composables.

## Presentation

- Keep each ViewModel separate, but group its related UI state, actions, routes, and statuses where that improves locality.
- Use a `FeatureAction` type for multiple events and a direct callback for one event.
- Use Kotlin explicit backing fields whenever a class exposes a read-only `StateFlow` or `SharedFlow` backed by a mutable
  flow, not only in ViewModels. Consumers still see only the declared read-only type; mutate the backing field inside its
  owner through explicit operations. Add an explicit mutable field type when inference would be too narrow.
- Avoid work in `init`; stateful screen overloads call explicit `observe...` or `load...` functions from `LaunchedEffect`.
- Inject `CoroutineDispatchers` and use the correct dispatcher for IO or background work.
- Declare Kermit `Logger.withTag(...)` in the class body.
- Do not repeat the logger tag/class name in log message text. The tag already carries that context.
- In a broad suspend-work catch, call `currentCoroutineContext().ensureActive()` before handling the failure.

## Compose

- Provide previews for composable screens and major stateful content variants.
- Keep stateful and stateless screen overloads in the same file.
- Keep ViewModel lookup in `:app:shared`; keep state collection and screen-owned effects in the stateful overload.
- Put `modifier: Modifier = Modifier` first in a Composable parameter list unless an API constraint gives a clear reason
  for a different order.
- Use `PreviewParameterProvider` when preview state or sample data takes several lines. Keep each substantial provider in
  a separate, clearly named file so the screen file remains focused on UI.
- Android screenshot previews used by `android-cli` belong to an Android source set and must call original composables only. Keep sample state in adjacent `PreviewParameterProvider` classes; never duplicate screen, section, or row composables for screenshot rendering.
- Keep all Compose resources in `:core:ui`. This includes strings, XML vector icons, images used by Compose, and bundled files
  such as `aboutlibraries.json`.
- Import `com.softartdev.kvace.core.ui.resources.*` where Compose resources are needed. Use
  `stringResource(Res.string...)` and `painterResource(Res.drawable...)` directly at the call site.
- Map enums and sealed UI values to typed Compose values such as `StringResource`, `Color`, and `Painter` through
  focused extension properties. Resolve a `StringResource` with `stringResource` only at the call site.
- Use Material 3 components and existing core UI primitives.
- Do not use the deprecated Material Icons dependency. Add Google Fonts Material Symbols as XML vector resources under
  `:core:ui/src/commonMain/composeResources/drawable` and call `painterResource(Res.drawable...)` directly. Do not add
  icon wrapper objects that mirror removed icon libraries.
- Keep UI modules independent from data modules.

## Data

- Keep external SDK types in data modules.
- Use explicit cancellation-aware `try/catch` around suspend HTTP calls.
- Persist first, then publish observable in-memory state.
- Persist small preferences through Multiplatform Settings.
- Do not persist hosted-provider secrets until secure storage is implemented.
