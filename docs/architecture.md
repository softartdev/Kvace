# Architecture

Kvace uses proportional Clean Architecture for a Kotlin and Compose Multiplatform application. The repository itself is the source of truth for architectural rules.

## Module Groups

- `app/*`: platform application entry points and the shared composition root.
- `core/domain`: cross-feature pure contracts such as dispatcher abstractions.
- `core/data`: shared data infrastructure such as persistent settings factories.
- `core/presentation`: UI-independent `Router`, `SnackbarInteractor`, and semantic snackbar messages.
- `core/ui`: shared Compose primitives, adaptive layout helpers, and the single public Compose `Res` class.
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

- ViewModels expose immutable `StateFlow` with Kotlin 2.4 explicit backing fields.
- Screens with multiple UI events use actions, for example `ChatAction`; screens with one event use a direct callback.
- Closely related domain models and presentation state/action/status types are grouped in focused files.
- ViewModels do not start long-running work in `init`; stateful screen overloads call explicit functions from `LaunchedEffect`.
- Broad `catch (Throwable)` blocks around suspend work call `currentCoroutineContext().ensureActive()` before handling the failure.
- Durable connection/model failures remain in screen state.

## Screen Boundary

Feature UI files keep stateful and stateless overloads together:

```kotlin
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel,
    modifier: Modifier = Modifier,
)

@Composable
fun FeatureScreen(
    state: FeatureUiState,
    onAction: (FeatureAction) -> Unit,
    optionalContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

The stateful overload receives a ViewModel, collects state, starts screen-owned effects, and delegates to the stateless overload. `:app:shared` destinations own `koinViewModel`.

## Application Infrastructure

- `ComposeRouter` attaches to the current `NavHostController` and does not buffer commands while detached.
- Top-level navigation uses `singleTop`, save/restore state, and selection derived from the back stack.
- Adaptive top-level navigation uses `NavigationSuiteScaffold`.
- `AppRoute.ThemeDialog` stays in the root navigation graph. `AppRoute` lives next to `Router` in `:core:presentation`, and ViewModels that own navigation decisions inject `Router` directly.
- Feature presentation can depend on a small navigator interface when navigation must be triggered from ViewModel logic.
  The application layer implements that interface with the app `Router`; feature UI should not call app routes directly.
- One global snackbar host is bound with clipboard and UI scope through `DisposableEffect`.
- Shared Koin bindings live in `kvaceModule`; it includes the platform-specific module.

### Platform Implementations And DI

Contracts with platform-dependent mechanics keep their contract and shared fallback behavior in common code. When
behavior or unavailable text differs by target, expose a named implementation from the corresponding platform source
set. This also applies to unsupported targets: use a concrete `Ios...`, `Web...`, or other platform adapter that owns its
platform-specific unavailable reason instead of constructing a generic fallback with a message inside
`kvacePlatformModule`. A shared fallback is sufficient when behavior and text are identical across targets.

Platform modules bind the concrete implementation to its contract directly when its constructor is fully resolvable:

```kotlin
singleOf(::WebShellCommandExecutor) bind ShellCommandExecutor::class
```

Do not add a local `create...` function around a zero-argument or fully injectable constructor. A constructor reference
does not apply Kotlin default arguments automatically: Koin still attempts to resolve those parameters. When a platform
implementation has non-DI configuration defaults, provide an explicit production constructor or use a narrow provider
function, then verify the binding by resolving the contract in the platform DI graph test.

## Compose Resources

All Compose resources are stored in `:core:ui`. App and feature modules import
`com.softartdev.kvace.core.ui.resources.*` and use direct `stringResource(Res.string...)`,
`painterResource(Res.drawable...)`, or `Res.readBytes(...)`. Keeping one public `Res` class avoids ambiguous generated
resource imports across feature modules.

## Provider Boundary

The domain-facing provider boundary is `AgentRuntime`. Koog types such as executors, clients, agents, models, and tools must stay in `:feature:agent:data`.

Provider result types remain domain contracts, while Koog and Ktor implementation types stay inside data modules.
