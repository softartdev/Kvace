package com.softartdev.kvace.feature.agent.data

import co.touchlab.kermit.Logger
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentExecutionEvent
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.domain.AgentRequest
import com.softartdev.kvace.feature.agent.domain.AgentRuntime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class KoogAgentRuntime actual constructor(
    private val configurationRepository: AgentConfigurationRepository,
    onDeviceModelProvider: OnDeviceModelProvider,
) : AgentRuntime {
    private val logger = Logger.withTag("KoogAgentRuntime")

    actual override fun execute(request: AgentRequest): Flow<AgentExecutionEvent> = flow {
        val provider = request.providerId
            ?.let { id -> configurationRepository.providers.value.firstOrNull { it.id == id } }
            ?: configurationRepository.selectedProvider.value

        when (provider?.id) {
            AgentProviderId.Ollama -> {
                logger.w { "Ollama execution is disabled in browser targets." }
                emit(AgentExecutionEvent.Error(BROWSER_EXECUTION_MESSAGE))
            }
            AgentProviderId.OpenAI -> emit(
                AgentExecutionEvent.Error("OpenAI execution needs secure credential storage before it can be enabled.")
            )
            AgentProviderId.OnDevice -> emit(AgentExecutionEvent.Error("On-device AI is unavailable in browser targets."))
            null -> emit(AgentExecutionEvent.Error("Configure an agent provider before running agent requests."))
        }
    }

    private companion object {
        const val BROWSER_EXECUTION_MESSAGE =
            "Ollama execution is available on Android, iOS, and Desktop. Browser support needs a CORS-safe runtime or server-side bridge."
    }
}
