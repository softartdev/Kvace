package com.softartdev.kvace.feature.agent.data

import co.touchlab.kermit.Logger
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentExecutionEvent
import com.softartdev.kvace.feature.agent.domain.AgentExecutionError
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.domain.AgentRequest
import com.softartdev.kvace.feature.agent.domain.AgentRuntime
import com.softartdev.kvace.feature.agent.domain.HarnessConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialRepository
import com.softartdev.kvace.feature.agent.domain.ShellCommandExecutor
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readLine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

actual class KoogAgentRuntime actual constructor(
    private val configurationRepository: AgentConfigurationRepository,
    private val harnessConfigurationRepository: HarnessConfigurationRepository,
    onDeviceModelProvider: OnDeviceModelProvider,
    shellCommandExecutor: ShellCommandExecutor,
    private val credentialRepository: ProviderCredentialRepository,
) : AgentRuntime {
    private val logger = Logger.withTag("KoogAgentRuntime")

    actual override fun execute(request: AgentRequest): Flow<AgentExecutionEvent> = flow {
        val provider: AgentProviderConfig? = request.providerId
            ?.let { id -> configurationRepository.providers.value.firstOrNull { it.id == id } }
            ?: configurationRepository.selectedProvider.value

        if (provider == null || !provider.isConfigured) {
            emit(AgentExecutionEvent.Error(AgentExecutionError.ProviderNotConfigured))
            return@flow
        }
        when (provider.id) {
            AgentProviderId.Ollama -> executeOllama(provider, request)
            AgentProviderId.OpenAI -> executeOpenAi(provider, request)
            AgentProviderId.OnDevice -> emit(AgentExecutionEvent.Error(AgentExecutionError.ProviderNotConfigured))
        }
    }

    private suspend fun FlowCollector<AgentExecutionEvent>.executeOllama(
        provider: AgentProviderConfig,
        request: AgentRequest,
    ) {
        val endpoint = provider.endpoint
        if (endpoint.isNullOrBlank()) {
            emit(AgentExecutionEvent.Error(AgentExecutionError.ProviderNotConfigured))
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
                    emit(AgentExecutionEvent.Error(status.toExecutionError()))
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
                            emit(AgentExecutionEvent.Error(AgentExecutionError.RequestFailed()))
                        }
                        streamEvent.finishReason?.let { finishReason ->
                            emit(AgentExecutionEvent.StreamFinished(finishReason))
                        }
                        return@execute
                    }
                }
                if (!hasContent) {
                    emit(AgentExecutionEvent.Error(AgentExecutionError.RequestFailed()))
                }
            }
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            logger.e(error) { "Failed to execute browser Ollama request with ${provider.modelName} at $endpoint" }
            emit(AgentExecutionEvent.Error(AgentExecutionError.Network))
        } finally {
            client.close()
        }
    }

    private suspend fun FlowCollector<AgentExecutionEvent>.executeOpenAi(
        provider: AgentProviderConfig,
        request: AgentRequest,
    ) {
        val apiKey = credentialRepository.readOpenAiApiKey()
        if (apiKey.isNullOrBlank()) {
            emit(AgentExecutionEvent.Error(AgentExecutionError.MissingCredential))
            return
        }
        val endpoint = provider.endpoint
        if (endpoint.isNullOrBlank()) {
            emit(AgentExecutionEvent.Error(AgentExecutionError.ProviderNotConfigured))
            return
        }
        val client = createAgentHttpClient(tag = "Ktor/WebOpenAiRuntime") {
            install(HttpTimeout) {
                connectTimeoutMillis = OLLAMA_CHAT_TIMEOUT_MILLIS
                requestTimeoutMillis = OLLAMA_CHAT_TIMEOUT_MILLIS
                socketTimeoutMillis = OLLAMA_CHAT_TIMEOUT_MILLIS
            }
        }
        try {
            val statement = client.preparePost("${endpoint.trimEnd('/')}/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(openAiChatRequest(provider, request))
            }
            statement.execute { response ->
                if (response.status.value !in 200..299) {
                    emit(AgentExecutionEvent.Error(response.status.value.toExecutionError()))
                    return@execute
                }
                var hasContent = false
                val channel = response.bodyAsChannel()
                while (true) {
                    val line = channel.readLine() ?: break
                    val payload = line.removePrefix("data:").trim()
                    if (payload == "[DONE]") break
                    val delta = runCatching {
                        Json.parseToJsonElement(payload)
                            .jsonObject["choices"]
                            ?.jsonArray
                            ?.firstOrNull()
                            ?.jsonObject
                            ?.get("delta")
                            ?.jsonObject
                            ?.get("content")
                            ?.jsonPrimitive
                            ?.content
                    }.getOrNull()
                    if (!delta.isNullOrEmpty()) {
                        hasContent = true
                        emit(AgentExecutionEvent.AssistantMessageDelta(delta))
                    }
                }
                if (!hasContent) emit(AgentExecutionEvent.Error(AgentExecutionError.RequestFailed()))
            }
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            logger.e(error) { "Failed to execute browser OpenAI request" }
            emit(AgentExecutionEvent.Error(AgentExecutionError.CorsBlocked))
        } finally {
            client.close()
        }
    }

    private fun openAiChatRequest(provider: AgentProviderConfig, request: AgentRequest): String = buildJsonObject {
        put("model", provider.modelName)
        put("stream", true)
        put("messages", buildJsonArray {
            harnessConfigurationRepository.config.value.systemPromptIfEnabled()?.let { prompt ->
                add(buildJsonObject {
                    put("role", "system")
                    put("content", prompt)
                })
            }
            request.context.forEach { message ->
                add(buildJsonObject {
                    put("role", if (message.role.name == "User") "user" else "assistant")
                    put("content", message.text)
                })
            }
            add(buildJsonObject {
                put("role", "user")
                put("content", request.prompt)
            })
        })
    }.toString()

    private companion object {
        const val OLLAMA_CHAT_TIMEOUT_MILLIS = 120_000L
    }
}

private fun Int.toExecutionError(): AgentExecutionError = when (this) {
    401, 403 -> AgentExecutionError.Authentication
    404 -> AgentExecutionError.ModelUnavailable
    else -> AgentExecutionError.RequestFailed("HTTP $this")
}
