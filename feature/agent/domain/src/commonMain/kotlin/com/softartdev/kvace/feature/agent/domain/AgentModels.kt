package com.softartdev.kvace.feature.agent.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class AgentProviderId(val displayName: String) {
    OpenAI("OpenAI"),
    Ollama("Ollama"),
    OnDevice("On-device"),
}

data class AgentProviderConfig(
    val id: AgentProviderId,
    val displayName: String = id.displayName,
    val modelName: String,
    val endpoint: String? = null,
    val isConfigured: Boolean = false,
)

data class AgentRequest(
    val prompt: String,
    val providerId: AgentProviderId? = null,
)

sealed interface AgentExecutionEvent {
    data class AssistantMessageDelta(val text: String) : AgentExecutionEvent
    data class AssistantMessage(val text: String) : AgentExecutionEvent
    data class ReasoningDelta(val text: String) : AgentExecutionEvent
    data class ReasoningMessage(val text: String) : AgentExecutionEvent
    data class ToolCallDelta(val text: String) : AgentExecutionEvent
    data class ToolCall(val text: String) : AgentExecutionEvent
    data class StreamFinished(val finishReason: String?) : AgentExecutionEvent
    data class Error(val message: String) : AgentExecutionEvent
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
}
