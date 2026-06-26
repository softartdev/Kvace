package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.AgentConversationRole
import com.softartdev.kvace.feature.agent.domain.AgentRequest
import com.softartdev.kvace.feature.agent.domain.HarnessConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

internal data class OllamaChatMessage(
    val role: String,
    val content: String,
)

internal fun AgentRequest.toOllamaChatMessages(systemPrompt: String?): List<OllamaChatMessage> = buildList {
    systemPrompt?.let { prompt ->
        add(OllamaChatMessage(role = "system", content = prompt))
    }
    context.forEach { message ->
        val role = when (message.role) {
            AgentConversationRole.User -> "user"
            AgentConversationRole.Assistant -> "assistant"
        }
        add(OllamaChatMessage(role = role, content = message.text))
    }
    add(OllamaChatMessage(role = "user", content = prompt))
}

internal data class OllamaChatStreamEvent(
    val content: String?,
    val done: Boolean,
    val finishReason: String?,
)

internal fun ollamaChatRequestJson(
    modelName: String,
    messages: List<OllamaChatMessage>,
    stream: Boolean,
): String =
    buildJsonObject {
        put("model", modelName)
        put("stream", stream)
        putJsonArray("messages") {
            messages.forEach { message ->
                addJsonObject {
                    put("role", message.role)
                    put("content", message.content)
                }
            }
        }
    }.toString()

internal fun parseOllamaChatResponseContent(body: String): String? {
    val root: JsonObject = Json.parseToJsonElement(body).jsonObject
    return (root["message"] as? JsonObject)
        ?.get("content")
        ?.asStringOrNull()
        ?.trim()
        ?.takeIf(String::isNotBlank)
}

internal fun parseOllamaChatStreamEvent(line: String): OllamaChatStreamEvent? {
    val trimmedLine = line.trim()
    if (trimmedLine.isBlank()) return null

    val root: JsonObject = Json.parseToJsonElement(trimmedLine).jsonObject
    val content = (root["message"] as? JsonObject)
        ?.get("content")
        ?.asStringOrNull()
        ?.takeIf(String::isNotEmpty)
    return OllamaChatStreamEvent(
        content = content,
        done = root["done"]?.jsonPrimitive?.content == "true",
        finishReason = root["done_reason"]?.asStringOrNull()?.takeIf(String::isNotBlank),
    )
}

internal fun HarnessConfig.systemPromptIfEnabled(): String? =
    systemPrompt.trim().takeIf { enabled && it.isNotBlank() }

private fun JsonElement.asStringOrNull(): String? =
    (this as? JsonPrimitive)?.jsonPrimitive?.contentOrNull
