# Snackbar Guide

Kvace has one snackbar host in `:app:shared`. The UI-independent `SnackbarInteractor` contract from `:core:presentation` is available for transient messages.

## Use A Snackbar For

- transient load failures
- transient save failures
- provider-selection failures
- chat send failures
- chat share failures

Keep connection status, model-list status, validation errors, and other durable screen context in UI state.

## Message Shape

Prefer `SnackbarMessage.Resource` with a semantic `SnackbarMessageResource`. Resolve visible text only in Compose resources. Use `suffix` for safe dynamic context such as a request identifier. Set `copyable = true` only when copying the complete rendered message is useful.

Copyable messages use a long duration and a localized copy action. Text is copied through Compose Multiplatform's common `ClipboardManager` API.

## Lifecycle

`ComposeSnackbarInteractor` is a singleton. The app root attaches `SnackbarHostState`, `ClipboardManager`, and UI `CoroutineScope` through `DisposableEffect`, and releases the exact binding on disposal. Calls while detached are no-ops. Showing a new message cancels the previous snackbar job.
