# System Design

Kvace is a local-first Kotlin Multiplatform agent workspace. The first product shape is a chat surface backed by configurable AI providers. The current working providers are local Ollama and platform on-device models where the host supports them; hosted providers are represented in the model but remain guarded until secure credential storage is implemented.

## Runtime Shape

- `:app:shared` owns the Compose root, navigation shell, theme wrapper, and Koin composition root.
- Feature modules own their domain, data, presentation, and UI boundaries.
- Koog is isolated in `:feature:agent:data`.
- Chat calls the domain-level `AgentRuntime`; it does not know whether the response comes from Ollama, on-device execution, OpenAI, or a later provider.
- Settings updates provider configuration through agent presentation/domain contracts instead of reaching into data implementations.

## Provider Flow

1. The selected provider is loaded from `AgentConfigurationRepository`.
2. Chat sends user input through `MessageSender`.
3. `SendMessageUseCase` appends the user message and asks `AgentRuntime` to execute the prompt.
4. `KoogAgentRuntime` maps Ollama to Koog's hosted client path or On-device to a custom Koog `LLMClient`.
5. The assistant, tool, or error event is mapped back to chat messages.

Ollama is selected by default and uses `qwen3.5:0.8b`. Android emulator builds default the host to `10.0.2.2`; other targets default to `127.0.0.1`.

Settings can query Ollama `/api/tags` through the domain-level `AgentModelCatalog`. This keeps server model discovery out of UI code and lets the app persist the selected server model before chat execution.

On-device support stays behind `OnDeviceModelProvider`. Android binds an ML Kit Prompt API provider and guards execution to API 26+. iOS binds an adapter around a Swift-supplied `OnDevicePromptApi`, which `iOSApp` registers only when Apple Foundation Models are available. JVM desktop and Web/Wasm bind unavailable providers that produce explicit unsupported-platform errors.

## Persistence

Multiplatform Settings stores lightweight app and provider preferences:

- selected provider
- Ollama host, port, endpoint, and model
- On-device model label
- provider configured state
- selected settings section

Android uses SharedPreferences-backed settings, iOS uses NSUserDefaults, Desktop JVM uses Preferences, and Wasm uses Web Storage through Multiplatform Settings `StorageSettings`.

## Secure Credentials

Secure provider credentials are required for hosted providers, not for local Ollama. Examples include OpenAI API keys, OAuth tokens, or provider-specific secret tokens. These should be stored in platform secure stores and should not be exposed through browser-side common code.

OpenAI remains a placeholder until that secure storage design is implemented.
