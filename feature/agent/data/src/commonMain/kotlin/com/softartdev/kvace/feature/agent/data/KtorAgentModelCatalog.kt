package com.softartdev.kvace.feature.agent.data

import co.touchlab.kermit.Logger
import com.softartdev.kvace.feature.agent.domain.AgentModelCatalog
import com.softartdev.kvace.feature.agent.domain.AgentModelListResult
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

class KtorAgentModelCatalog(
    private val logger: Logger,
) : AgentModelCatalog {
    override suspend fun loadModels(config: AgentProviderConfig): AgentModelListResult {
        return when (config.id) {
            AgentProviderId.Ollama -> config.endpoint
                ?.let { endpoint -> loadOllamaModels(endpoint) }
                ?: AgentModelListResult.Failure(null)
            AgentProviderId.OpenAI -> AgentModelListResult.Failure(null)
            AgentProviderId.OnDevice -> AgentModelListResult.Success(listOf(config.modelName))
        }
    }

    private suspend fun loadOllamaModels(endpoint: String): AgentModelListResult =
        runCatching {
            val url = "${endpoint.trimEnd('/')}/api/tags"
            val body = fetchAgentEndpointText(
                url = url,
                timeoutMillis = MODEL_LIST_TIMEOUT_MILLIS,
            )
            val models = parseOllamaModelNames(body)
            logger.i { "KtorAgentModelCatalog loaded ${models.size} Ollama models from $endpoint" }
            AgentModelListResult.Success(models)
        }.getOrElse { error ->
            logger.e(error) { "KtorAgentModelCatalog failed to load Ollama models from $endpoint" }
            AgentModelListResult.Failure(error.message)
        }

    private companion object {
        const val MODEL_LIST_TIMEOUT_MILLIS = 15_000L

        fun parseOllamaModelNames(body: String): List<String> {
            val root = Json.parseToJsonElement(body).jsonObject
            val models = root["models"] as? JsonArray ?: return emptyList()
            return models
                .mapNotNull { element ->
                    val model = element as? JsonObject ?: return@mapNotNull null
                    (model["name"] as? JsonPrimitive)?.contentOrNull
                        ?: (model["model"] as? JsonPrimitive)?.contentOrNull
                }
                .distinct()
                .sorted()
        }
    }
}
