package com.softartdev.kvace.feature.agent.data

import co.touchlab.kermit.Logger
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTestResult
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTester
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class KtorAgentConnectionTester : AgentConnectionTester {
    private val logger = Logger.withTag("KtorAgentConnectionTester")

    override suspend fun testConnection(config: AgentProviderConfig): AgentConnectionTestResult {
        return when (config.id) {
            AgentProviderId.Ollama -> config.endpoint
                ?.let { endpoint -> testOllama(endpoint) }
                ?: AgentConnectionTestResult.Failure(null)
            AgentProviderId.OpenAI -> AgentConnectionTestResult.Failure(null)
            AgentProviderId.OnDevice -> AgentConnectionTestResult.Failure("On-device AI does not use an HTTP endpoint.")
        }
    }

    private suspend fun testOllama(endpoint: String): AgentConnectionTestResult {
        return try {
            val url = "${endpoint.trimEnd('/')}/api/version"
            val status = fetchAgentEndpointStatus(
                url = url,
                timeoutMillis = CONNECTION_TEST_TIMEOUT_MILLIS,
            )
            if (status in HTTP_SUCCESS_RANGE) {
                logger.i { "Connected to Ollama at $endpoint" }
                AgentConnectionTestResult.Success
            } else {
                logger.w { "Received HTTP $status from Ollama at $endpoint" }
                AgentConnectionTestResult.Failure("HTTP $status")
            }
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            logger.e(error) { "Failed to connect to Ollama at $endpoint" }
            AgentConnectionTestResult.Failure(error.message)
        }
    }

    private companion object {
        const val CONNECTION_TEST_TIMEOUT_MILLIS = 15_000L
        val HTTP_SUCCESS_RANGE = 200..299
    }
}
