# Provider Configuration

## Ollama

Ollama is the default selected provider. The app starts with:

- host `127.0.0.1`
- Android emulator host `10.0.2.2`
- port `11434`
- model `qwen3.5:0.8b`

Users can edit the host, port, and model in Providers > Ollama and press the connection test button. A successful test stores the provider as configured.

The Providers screen can also load server models from Ollama `/api/tags`. If the currently stored model is not present on the server, Kvace selects the first returned model and persists that choice. Users can still type a model manually for advanced/local experimentation.

## Real Execution

Workspace execution now uses Koog for minimal single-turn requests. The UI and presentation layers do not call Koog directly; chat uses `MessageSender`, `SendMessageUseCase`, and `AgentRuntime`. Koog agent iteration limits are intentionally above one step because even simple agent runs may need more than one internal graph transition.

`SendMessageUseCase` locks the selected provider and model at the start of a request. Previous user/assistant messages from the selected conversation are passed as request context, and assistant-side messages store the model label and generation timestamp.

Ollama uses Koog's built-in Ollama client on Android, iOS, and Desktop JVM. Web/Wasm uses a direct Ktor request to
Ollama `/api/chat` and reads streaming NDJSON, because Koog execution is not available in the browser target yet. On-device
execution uses a custom Koog `LLMClient` with provider id `on-device`, so the agent boundary stays the same even though
the model runs through platform APIs instead of an HTTP endpoint. The on-device client supports single-turn text
generation only. It does not support tools, embeddings, moderation, or true token streaming.

Ollama tool calling is enabled for one `shell_command` tool call per user send. The command executor accepts only
read-only allowlisted commands (`pwd`, `ls`, `cat`, `head`, `tail`, `sed -n`, `rg`, and read-only `git status/diff/log/show`)
and rejects shell syntax, path escapes, absolute file arguments, unsafe `rg` options, unsafe `git` options, and `sed`
scripts outside numeric print ranges. Desktop JVM executes the command through `ProcessBuilder` without a shell
interpreter. Android currently returns an unsupported tool result until a Termux bridge is configured. iOS and Web/Wasm
return unsupported results.

## On-device

On-device is listed as a provider with no editable endpoint. Kvace persists the provider selection and a platform-specific model label:

- Android: `Gemini Nano`
- iOS/Catalyst-ready host: `Apple Foundation Models`
- unsupported targets: `On-device model`

Android support uses ML Kit Prompt API. The app min SDK remains `24`, but runtime execution is guarded to Android 8.0/API 26 or newer. Kvace does not request a named model download; ML Kit chooses and downloads the framework-supported model through `Generation.getClient()`. Devices that report the model as downloadable start ML Kit's download flow; while the model is downloading, unavailable, or download-failed, the chat path returns a clear error instead of pretending the provider is configured.

iOS support is supplied by the Swift host. `PromptApiIos` implements the shared Kotlin `OnDevicePromptApi` with `FoundationModels.LanguageModelSession`, and `iOSApp` registers it with `AppleOnDevicePromptApiRegistry` only under `#available(iOS 26.0, macCatalyst 26.0, *)`. The bridge is ready for Catalyst-capable hosts, but this project does not enable Mac Catalyst yet.

Desktop JVM and Web/Wasm keep the on-device provider visible but unavailable.

## Hosted Providers

OpenAI is listed as a future provider. It is intentionally not executable yet because hosted providers need secure credential storage for API keys and tokens.

Credential storage requirements:

- Android: Keystore-backed encrypted storage.
- iOS: Keychain.
- Desktop JVM: a platform secure store or explicit user-approved alternative.
- Web/Wasm: avoid storing provider API keys in browser-side local state for real hosted-provider execution.

## Browser Caveat

Local Ollama from Web/Wasm may require Ollama CORS configuration. A passing Desktop or Android connection test does not
guarantee browser execution access, because browser fetches enforce CORS even when native targets can connect.
