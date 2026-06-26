package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.AgentConversationMessage
import com.softartdev.kvace.feature.agent.domain.AgentConversationRole
import com.softartdev.kvace.feature.agent.domain.AgentRequest
import com.softartdev.kvace.feature.agent.domain.HarnessConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OllamaChatRequestMapperTest {

    @Test
    fun mapsHarnessHistoryAndPromptInInferenceOrder() {
        val request = AgentRequest(
            prompt = "Current prompt",
            context = listOf(
                AgentConversationMessage(AgentConversationRole.User, "Previous user"),
                AgentConversationMessage(AgentConversationRole.Assistant, "Previous assistant"),
            ),
        )

        val messages = request.toOllamaChatMessages(systemPrompt = "Harness prompt")

        assertEquals(
            listOf(
                OllamaChatMessage(role = "system", content = "Harness prompt"),
                OllamaChatMessage(role = "user", content = "Previous user"),
                OllamaChatMessage(role = "assistant", content = "Previous assistant"),
                OllamaChatMessage(role = "user", content = "Current prompt"),
            ),
            messages,
        )
    }

    @Test
    fun omitsDisabledHarnessPrompt() {
        val config = HarnessConfig(enabled = false, systemPrompt = "Harness prompt")

        assertNull(config.systemPromptIfEnabled())
    }

    @Test
    fun serializesStreamingOllamaChatRequest() {
        val json = ollamaChatRequestJson(
            modelName = "gemma4:latest",
            messages = listOf(OllamaChatMessage(role = "user", content = "Hello")),
            stream = true,
        )
        val root = Json.parseToJsonElement(json).jsonObject

        assertEquals("gemma4:latest", root.getValue("model").jsonPrimitive.content)
        assertEquals("true", root.getValue("stream").jsonPrimitive.content)
        val message = root.getValue("messages").jsonArray.single().jsonObject
        assertEquals("user", message.getValue("role").jsonPrimitive.content)
        assertEquals("Hello", message.getValue("content").jsonPrimitive.content)
    }

    @Test
    fun parsesAssistantMessageContent() {
        val content = parseOllamaChatResponseContent(
            """{"message":{"role":"assistant","content":" Hello "},"done":true}""",
        )

        assertEquals("Hello", content)
    }

    @Test
    fun blankAssistantMessageContentReturnsNull() {
        val content = parseOllamaChatResponseContent(
            """{"message":{"role":"assistant","content":"   "},"done":true}""",
        )

        assertNull(content)
    }

    @Test
    fun parsesStreamingContentDelta() {
        val event = parseOllamaChatStreamEvent(
            """{"message":{"role":"assistant","content":"Hel"},"done":false}""",
        )

        assertEquals(OllamaChatStreamEvent(content = "Hel", done = false, finishReason = null), event)
    }

    @Test
    fun parsesStreamingDoneReason() {
        val event = parseOllamaChatStreamEvent(
            """{"done":true,"done_reason":"stop"}""",
        )

        assertEquals(OllamaChatStreamEvent(content = null, done = true, finishReason = "stop"), event)
    }

    @Test
    fun blankStreamingLineReturnsNull() {
        assertNull(parseOllamaChatStreamEvent("   "))
    }
}
