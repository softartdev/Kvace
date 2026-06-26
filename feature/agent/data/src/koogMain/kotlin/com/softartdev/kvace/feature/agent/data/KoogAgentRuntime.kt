package com.softartdev.kvace.feature.agent.data

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import co.touchlab.kermit.Logger
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentConversationRole
import com.softartdev.kvace.feature.agent.domain.AgentExecutionEvent
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.domain.AgentRequest
import com.softartdev.kvace.feature.agent.domain.AgentRuntime
import com.softartdev.kvace.feature.agent.domain.HarnessConfigurationRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

actual class KoogAgentRuntime actual constructor(
    private val configurationRepository: AgentConfigurationRepository,
    private val harnessConfigurationRepository: HarnessConfigurationRepository,
    private val onDeviceModelProvider: OnDeviceModelProvider,
) : AgentRuntime {
    private val logger = Logger.withTag("KoogAgentRuntime")

    actual override fun execute(request: AgentRequest): Flow<AgentExecutionEvent> = flow {
        val provider: AgentProviderConfig? = request.providerId
            ?.let { id -> configurationRepository.providers.value.firstOrNull { it.id == id } }
            ?: configurationRepository.selectedProvider.value

        if (provider == null || !provider.isConfigured) {
            emit(AgentExecutionEvent.Error("Configure an agent provider before running agent requests."))
            return@flow
        }
        when (provider.id) {
            AgentProviderId.Ollama -> executeOllama(provider, request)
            AgentProviderId.OnDevice -> executeOnDevice(provider, request)
            AgentProviderId.OpenAI -> emit(
                AgentExecutionEvent.Error("OpenAI execution needs secure credential storage before it can be enabled.")
            )
        }
    }

    private suspend fun FlowCollector<AgentExecutionEvent>.executeOllama(
        provider: AgentProviderConfig,
        request: AgentRequest,
    ) {
        val endpoint: String? = provider.endpoint
        if (endpoint.isNullOrBlank()) {
            emit(AgentExecutionEvent.Error("Configure the Ollama endpoint before sending messages."))
            return
        }
        val baseClient = createAgentHttpClient(tag = "Ktor/KoogAgentRuntime") {}
        val client = OllamaClient(
            httpClientFactory = KtorKoogHttpClient.Factory(baseClient),
            baseUrl = endpoint,
        )
        try {
            val model = LLModel(
                provider = LLMProvider.Ollama,
                id = provider.modelName,
                capabilities = listOf(LLMCapability.Temperature),
                contextLength = DEFAULT_CONTEXT_LENGTH,
            )
            val streamingPrompt: Prompt = prompt(
                id = "kvace-chat",
                params = LLMParams(temperature = DEFAULT_TEMPERATURE),
            ) {
                harnessConfigurationRepository.config.value.systemPromptIfEnabled()?.let { system(it) }
                request.context.forEach { message ->
                    when (message.role) {
                        AgentConversationRole.User -> user(message.text)
                        AgentConversationRole.Assistant -> assistant(message.text)
                    }
                }
                user(request.prompt)
            }
            val streamedText = StringBuilder()
            var textCompleteReceived = false
            client.executeStreaming(streamingPrompt, model).collect { frame: StreamFrame ->
                when (frame) {
                    is StreamFrame.TextDelta -> {
                        streamedText.append(frame.text)
                        emit(AgentExecutionEvent.AssistantMessageDelta(frame.text))
                    }
                    is StreamFrame.TextComplete -> {
                        textCompleteReceived = true
                        streamedText.clear()
                        streamedText.append(frame.text)
                        if (frame.text.isNotBlank()) {
                            emit(AgentExecutionEvent.AssistantMessage(frame.text))
                        }
                    }
                    is StreamFrame.ReasoningDelta -> {
                        frame.displayText()?.let { text ->
                            emit(AgentExecutionEvent.ReasoningDelta(text))
                        }
                    }
                    is StreamFrame.ReasoningComplete -> {
                        frame.displayText()?.let { text ->
                            emit(AgentExecutionEvent.ReasoningMessage(text))
                        }
                    }
                    is StreamFrame.ToolCallDelta -> {
                        frame.displayText()?.let { text ->
                            emit(AgentExecutionEvent.ToolCallDelta(text))
                        }
                    }
                    is StreamFrame.ToolCallComplete -> {
                        emit(AgentExecutionEvent.ToolCall(frame.displayText()))
                    }
                    is StreamFrame.End -> {
                        emit(AgentExecutionEvent.StreamFinished(frame.finishReason))
                    }
                }
            }
            val response = streamedText.toString()
            if (!textCompleteReceived && response.isNotBlank()) {
                emit(AgentExecutionEvent.AssistantMessage(response))
            }
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            logger.e(error) { "Failed to execute Ollama request with ${provider.modelName} at $endpoint" }
            emit(AgentExecutionEvent.Error(error.message ?: "Ollama request failed."))
        } finally {
            client.close()
        }
    }

    private suspend fun FlowCollector<AgentExecutionEvent>.executeOnDevice(
        provider: AgentProviderConfig,
        request: AgentRequest,
    ) {
        if (!onDeviceModelProvider.isAvailable) {
            emit(AgentExecutionEvent.Error("On-device AI is unavailable on this platform or OS version."))
            return
        }
        val client = OnDeviceLLMClient(onDeviceModelProvider)
        try {
            val model = onDeviceLLModel(provider.modelName)
            val singleTurnPrompt = prompt(
                id = "kvace-on-device-chat",
                params = LLMParams(temperature = DEFAULT_TEMPERATURE),
            ) {
                harnessConfigurationRepository.config.value.systemPromptIfEnabled()?.let { system(it) }
                request.context.forEach { message ->
                    when (message.role) {
                        AgentConversationRole.User -> user(message.text)
                        AgentConversationRole.Assistant -> assistant(message.text)
                    }
                }
                user(request.prompt)
            }
            val response = client.execute(singleTurnPrompt, model)
            val text = response.textContent().trim()
            val event: AgentExecutionEvent = when {
                text.isBlank() -> AgentExecutionEvent.Error("On-device AI returned an empty response.")
                else -> AgentExecutionEvent.AssistantMessage(text)
            }
            emit(event)
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            logger.e(error) { "Failed to execute on-device request with ${provider.modelName}" }
            emit(AgentExecutionEvent.Error(error.message ?: "On-device AI request failed."))
        } finally {
            client.close()
        }
    }

    private fun StreamFrame.ReasoningDelta.displayText(): String? =
        listOfNotNull(text, summary)
            .joinNonBlank(separator = "\n")

    private fun StreamFrame.ReasoningComplete.displayText(): String? =
        listOfNotNull(
            content.joinNonBlank(separator = "\n"),
            summary?.joinNonBlank(separator = "\n"),
            encrypted,
        ).joinNonBlank(separator = "\n\n")

    private fun StreamFrame.ToolCallDelta.displayText(): String? =
        listOfNotNull(name, content)
            .joinNonBlank(separator = "\n")

    private fun StreamFrame.ToolCallComplete.displayText(): String =
        listOf(name, content)
            .joinNonBlank(separator = "\n")
            .orEmpty()

    private fun Iterable<String?>.joinNonBlank(separator: String): String? =
        mapNotNull { text -> text?.takeIf { it.isNotBlank() } }
            .joinToString(separator = separator)
            .takeIf { it.isNotBlank() }

    private fun com.softartdev.kvace.feature.agent.domain.HarnessConfig.systemPromptIfEnabled(): String? =
        systemPrompt.trim().takeIf { enabled && it.isNotBlank() }

    private companion object {
        const val DEFAULT_CONTEXT_LENGTH = 4096L
        const val DEFAULT_TEMPERATURE = 0.7
    }
}
