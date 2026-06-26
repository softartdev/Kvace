package com.softartdev.kvace.feature.agent.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AgentModelsTest {

    @Test
    fun providerDisplayNamesStayStable() {
        assertEquals("OpenAI", AgentProviderId.OpenAI.displayName)
        assertEquals("Ollama", AgentProviderId.Ollama.displayName)
        assertEquals("On-device", AgentProviderId.OnDevice.displayName)
    }

    @Test
    fun providerConfigDefaultsToUnconfiguredWithoutEndpoint() {
        val config = AgentProviderConfig(
            id = AgentProviderId.OpenAI,
            modelName = "gpt-4o",
        )

        assertEquals("OpenAI", config.displayName)
        assertNull(config.endpoint)
        assertFalse(config.isConfigured)
    }

    @Test
    fun agentRequestCanUseSelectedProviderByDefault() {
        val request = AgentRequest(prompt = "Hello")

        assertEquals("Hello", request.prompt)
        assertNull(request.providerId)
    }

    @Test
    fun modelListSuccessKeepsServerModelNames() {
        val result = AgentModelListResult.Success(
            modelNames = listOf("llama3.2:latest", "mistral:latest"),
        )

        assertEquals(listOf("llama3.2:latest", "mistral:latest"), result.modelNames)
    }
}
