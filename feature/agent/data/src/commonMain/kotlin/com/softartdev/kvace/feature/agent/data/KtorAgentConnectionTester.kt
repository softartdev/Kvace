package com.softartdev.kvace.feature.agent.data

import co.touchlab.kermit.Logger
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTestResult
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTester
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class KtorAgentConnectionTester(
    private val credentialRepository: ProviderCredentialRepository,
) : AgentConnectionTester {
    private val logger = Logger.withTag("KtorAgentConnectionTester")

    override suspend fun testConnection(config: AgentProviderConfig): AgentConnectionTestResult {
        return when (config.id) {
            AgentProviderId.Ollama -> config.endpoint
                ?.let { endpoint -> testOllama(endpoint) }
                ?: AgentConnectionTestResult.Failure(null)
            AgentProviderId.OpenAI -> testOpenAi(config)
            AgentProviderId.OnDevice -> AgentConnectionTestResult.Failure("On-device AI does not use an HTTP endpoint.")
        }
    }

    override suspend fun testBrowserEndpoint(config: AgentProviderConfig): AgentConnectionTestResult = when (config.id) {
        AgentProviderId.OpenAI -> testOpenAiEndpoint(config)
        else -> AgentConnectionTestResult.Failure(null)
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

    private suspend fun testOpenAi(config: AgentProviderConfig): AgentConnectionTestResult {
        val apiKey = credentialRepository.readOpenAiApiKey() ?: return AgentConnectionTestResult.Failure("Missing credential")
        val endpoint = config.endpoint ?: return AgentConnectionTestResult.Failure(null)
        return testEndpoint("${endpoint.trimEnd('/')}/v1/models/${config.modelName}", apiKey)
    }

    private suspend fun testOpenAiEndpoint(config: AgentProviderConfig): AgentConnectionTestResult {
        val endpoint = config.endpoint ?: return AgentConnectionTestResult.Failure(null)
        return testEndpoint("${endpoint.trimEnd('/')}/v1/models", authorization = null)
    }

    private suspend fun testEndpoint(url: String, authorization: String?): AgentConnectionTestResult = try {
        val status = fetchAgentEndpointStatus(
            url = url,
            timeoutMillis = CONNECTION_TEST_TIMEOUT_MILLIS,
            authorization = authorization,
        )
        if (status in HTTP_SUCCESS_RANGE) AgentConnectionTestResult.Success
        else AgentConnectionTestResult.Failure("HTTP $status")
    } catch (error: Throwable) {
        currentCoroutineContext().ensureActive()
        logger.e(error) { "Failed to connect to provider endpoint" }
        AgentConnectionTestResult.Failure(error.message)
    }

    private companion object {
        const val CONNECTION_TEST_TIMEOUT_MILLIS = 15_000L
        val HTTP_SUCCESS_RANGE = 200..299
    }
}
