package com.softartdev.kvace.feature.agent.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class AgentProviderId(val displayName: String) {
    OpenAI("OpenAI"),
    Ollama("Ollama"),
    OnDevice("On-device"),
}

const val DEFAULT_HARNESS_SYSTEM_PROMPT = "You are Kvace, a concise AI assistant inside a multiplatform agent app."

data class AgentProviderConfig(
    val id: AgentProviderId,
    val displayName: String = id.displayName,
    val modelName: String,
    val endpoint: String? = null,
    val isConfigured: Boolean = false,
)

enum class ProviderCredentialStatus {
    Absent,
    Stored,
    Locked,
    SessionOnly,
    Unavailable,
}

sealed interface ProviderCredentialResult {
    data object Success : ProviderCredentialResult
    data object Invalid : ProviderCredentialResult
    data object Locked : ProviderCredentialResult
    data object Unavailable : ProviderCredentialResult
    data class Failure(val reason: String? = null) : ProviderCredentialResult
}

data class ValidatedOllamaEndpoint(
    val value: String,
    val host: String,
    val port: Int,
)

sealed interface OllamaEndpointValidationResult {
    data class Valid(val endpoint: ValidatedOllamaEndpoint) : OllamaEndpointValidationResult
    data object InvalidHost : OllamaEndpointValidationResult
    data object InvalidPort : OllamaEndpointValidationResult
}

interface OllamaEndpointValidator {
    fun validate(hostInput: String, portInput: String): OllamaEndpointValidationResult
    fun parse(endpoint: String): ValidatedOllamaEndpoint?
}

interface OpenAiEndpointValidator {
    fun normalize(input: String): String?
}

data class AgentRequest(
    val prompt: String,
    val providerId: AgentProviderId? = null,
    val context: List<AgentConversationMessage> = emptyList(),
)

enum class AgentConversationRole {
    User,
    Assistant,
}

data class AgentConversationMessage(
    val role: AgentConversationRole,
    val text: String,
)

data class HarnessConfig(
    val enabled: Boolean = true,
    val systemPrompt: String = DEFAULT_HARNESS_SYSTEM_PROMPT,
)

sealed interface AgentExecutionEvent {
    data class AssistantMessageDelta(val text: String) : AgentExecutionEvent
    data class AssistantMessage(val text: String) : AgentExecutionEvent
    data class ReasoningDelta(val text: String) : AgentExecutionEvent
    data class ReasoningMessage(val text: String) : AgentExecutionEvent
    data class ToolCallDelta(val text: String) : AgentExecutionEvent
    data class ToolCall(val text: String) : AgentExecutionEvent
    data class StreamFinished(val finishReason: String?) : AgentExecutionEvent
    data class Error(val error: AgentExecutionError) : AgentExecutionEvent
}

sealed interface AgentExecutionError {
    data object ProviderNotConfigured : AgentExecutionError
    data object MissingCredential : AgentExecutionError
    data object CredentialLocked : AgentExecutionError
    data object Network : AgentExecutionError
    data object Authentication : AgentExecutionError
    data object ModelUnavailable : AgentExecutionError
    data object CorsBlocked : AgentExecutionError
    data class RequestFailed(val detail: String? = null) : AgentExecutionError
}

sealed class AgentRuntimeException(message: String) : RuntimeException(message) {
    data object AgentNotConfigured : AgentRuntimeException("No agent provider is configured yet.")
}

interface AgentRuntime {
    fun execute(request: AgentRequest): Flow<AgentExecutionEvent>
}

sealed interface AgentConnectionTestResult {
    data object Success : AgentConnectionTestResult
    data class Failure(val message: String?) : AgentConnectionTestResult
}

interface AgentConnectionTester {
    suspend fun testConnection(config: AgentProviderConfig): AgentConnectionTestResult
    suspend fun testBrowserEndpoint(config: AgentProviderConfig): AgentConnectionTestResult
}

sealed interface AgentModelListResult {
    data class Success(val modelNames: List<String>) : AgentModelListResult
    data class Failure(val message: String?) : AgentModelListResult
}

interface AgentModelCatalog {
    suspend fun loadModels(config: AgentProviderConfig): AgentModelListResult
}

interface AgentConfigurationRepository {
    val providers: StateFlow<List<AgentProviderConfig>>
    val selectedProvider: StateFlow<AgentProviderConfig?>
    suspend fun selectProvider(id: AgentProviderId)
    suspend fun updateProvider(config: AgentProviderConfig)
    suspend fun resetProvider(id: AgentProviderId)
}

interface ProviderCredentialRepository {
    val openAiStatus: StateFlow<ProviderCredentialStatus>
    suspend fun readOpenAiApiKey(): String?
    suspend fun saveOpenAiApiKey(apiKey: String): ProviderCredentialResult
    suspend fun deleteOpenAiApiKey(): ProviderCredentialResult
    suspend fun unlockOpenAiApiKey(masterPassword: String): ProviderCredentialResult
    suspend fun clearLockedOpenAiApiKey(): ProviderCredentialResult
}

interface HarnessConfigurationRepository {
    val config: StateFlow<HarnessConfig>
    suspend fun updateConfig(config: HarnessConfig)
    suspend fun resetConfig()
}
