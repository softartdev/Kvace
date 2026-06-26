package com.softartdev.kvace.feature.agent.data

import co.touchlab.kermit.Logger
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentExecutionEvent
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.domain.AgentRequest
import com.softartdev.kvace.feature.agent.domain.AgentRuntime
import com.softartdev.kvace.feature.agent.domain.HarnessConfigurationRepository
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readLine
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

actual class KoogAgentRuntime actual constructor(
    private val configurationRepository: AgentConfigurationRepository,
    private val harnessConfigurationRepository: HarnessConfigurationRepository,
    onDeviceModelProvider: OnDeviceModelProvider,
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
            AgentProviderId.OpenAI -> emit(
                AgentExecutionEvent.Error("OpenAI execution needs secure credential storage before it can be enabled.")
            )
            AgentProviderId.OnDevice -> emit(AgentExecutionEvent.Error("On-device AI is unavailable in browser targets."))
        }
    }

    private suspend fun FlowCollector<AgentExecutionEvent>.executeOllama(
        provider: AgentProviderConfig,
        request: AgentRequest,
    ) {
        val endpoint = provider.endpoint
        if (endpoint.isNullOrBlank()) {
            emit(AgentExecutionEvent.Error("Configure the Ollama endpoint before sending messages."))
            return
        }
        val client = createAgentHttpClient(tag = "Ktor/WebOllamaRuntime") {
            install(HttpTimeout) {
                connectTimeoutMillis = OLLAMA_CHAT_TIMEOUT_MILLIS
                requestTimeoutMillis = OLLAMA_CHAT_TIMEOUT_MILLIS
                socketTimeoutMillis = OLLAMA_CHAT_TIMEOUT_MILLIS
            }
        }
        try {
            val statement = client.preparePost("${endpoint.trimEnd('/')}/api/chat") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    ollamaChatRequestJson(
                        modelName = provider.modelName,
                        stream = true,
                        messages = request.toOllamaChatMessages(
                            harnessConfigurationRepository.config.value.systemPromptIfEnabled(),
                        ),
                    ),
                )
            }
            statement.execute { response ->
                val status = response.status.value
                if (status !in 200..299) {
                    val body = response.bodyAsText()
                    emit(AgentExecutionEvent.Error(body.ifBlank { "HTTP $status" }))
                    return@execute
                }
                var hasContent = false
                val channel = response.bodyAsChannel()
                while (true) {
                    val line = channel.readLine() ?: break
                    val streamEvent = parseOllamaChatStreamEvent(line) ?: continue
                    streamEvent.content?.let { content ->
                        hasContent = true
                        emit(AgentExecutionEvent.AssistantMessageDelta(content))
                    }
                    if (streamEvent.done) {
                        if (!hasContent) {
                            emit(AgentExecutionEvent.Error("Ollama returned an empty response."))
                        }
                        streamEvent.finishReason?.let { finishReason ->
                            emit(AgentExecutionEvent.StreamFinished(finishReason))
                        }
                        return@execute
                    }
                }
                if (!hasContent) {
                    emit(AgentExecutionEvent.Error("Ollama returned an empty response."))
                }
            }
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            logger.e(error) { "Failed to execute browser Ollama request with ${provider.modelName} at $endpoint" }
            emit(AgentExecutionEvent.Error(error.message ?: "Ollama request failed."))
        } finally {
            client.close()
        }
    }

    private companion object {
        const val OLLAMA_CHAT_TIMEOUT_MILLIS = 120_000L
    }
}
