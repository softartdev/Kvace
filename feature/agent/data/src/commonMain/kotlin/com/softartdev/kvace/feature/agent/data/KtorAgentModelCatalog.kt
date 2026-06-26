package com.softartdev.kvace.feature.agent.data

import co.touchlab.kermit.Logger
import com.softartdev.kvace.feature.agent.domain.AgentModelCatalog
import com.softartdev.kvace.feature.agent.domain.AgentModelListResult
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

class KtorAgentModelCatalog : AgentModelCatalog {
    private val logger = Logger.withTag("KtorAgentModelCatalog")

    override suspend fun loadModels(config: AgentProviderConfig): AgentModelListResult = when (config.id) {
        AgentProviderId.Ollama -> config.endpoint
            ?.let { endpoint: String -> loadOllamaModels(endpoint) }
            ?: AgentModelListResult.Failure(null)
        AgentProviderId.OpenAI -> AgentModelListResult.Failure(null)
        AgentProviderId.OnDevice -> AgentModelListResult.Success(listOf(config.modelName))
    }

    private suspend fun loadOllamaModels(endpoint: String): AgentModelListResult = try {
        val url = "${endpoint.trimEnd('/')}/api/tags"
        val body = fetchAgentEndpointText(url, MODEL_LIST_TIMEOUT_MILLIS)
        val models = parseOllamaModelNames(body)
        logger.i { "Loaded ${models.size} Ollama models from $endpoint" }
        return AgentModelListResult.Success(models)
    } catch (error: Throwable) {
        currentCoroutineContext().ensureActive()
        logger.e(error) { "Failed to load Ollama models from $endpoint" }
        return AgentModelListResult.Failure(error.message)
    }

    private companion object {
        const val MODEL_LIST_TIMEOUT_MILLIS = 15_000L

        fun parseOllamaModelNames(body: String): List<String> {
            val root: JsonObject = Json.parseToJsonElement(body).jsonObject
            val models: JsonArray = root["models"] as? JsonArray ?: return emptyList()
            return models.asSequence()
                .mapNotNull { element: JsonElement -> element as? JsonObject }
                .mapNotNull { jsonObj: JsonObject -> jsonObj["name"] ?: jsonObj["model"] }
                .mapNotNull { element: JsonElement -> element as? JsonPrimitive }
                .mapNotNull(JsonPrimitive::contentOrNull)
                .distinct()
                .sorted()
                .toList()
        }
    }
}
