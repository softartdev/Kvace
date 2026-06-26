package com.softartdev.kvace.feature.agent.data

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.params.LLMParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OnDeviceLLMClientTest {

    @Test
    fun executesPromptWithOnDeviceProvider() = runTest {
        val provider = FakeClientOnDeviceModelProvider(response = "On-device response")
        val client = OnDeviceLLMClient(provider)

        val response = client.execute(testPrompt(), onDeviceLLModel("Fake on-device model"))

        assertEquals("On-device response", response.textContent())
        assertContains(provider.receivedPrompt, "System: You are testing Kvace.")
        assertContains(provider.receivedPrompt, "User: Hello")
    }

    @Test
    fun rejectsTools() = runTest {
        val client = OnDeviceLLMClient(FakeClientOnDeviceModelProvider())

        assertFailsWith<UnsupportedOperationException> {
            client.execute(
                prompt = testPrompt(),
                model = onDeviceLLModel("Fake on-device model"),
                tools = listOf(ToolDescriptor(name = "test", description = "Unsupported tool")),
            )
        }
    }

    @Test
    fun listsModelOnlyWhenProviderIsAvailable() = runTest {
        val availableClient = OnDeviceLLMClient(FakeClientOnDeviceModelProvider(isAvailable = true))
        val unavailableClient = OnDeviceLLMClient(FakeClientOnDeviceModelProvider(isAvailable = false))

        assertEquals(listOf(onDeviceLLModel("Fake on-device model")), availableClient.models())
        assertEquals(emptyList(), unavailableClient.models())
    }

    private fun testPrompt() = prompt(id = "on-device-test", params = LLMParams()) {
        system("You are testing Kvace.")
        user("Hello")
    }
}

private class FakeClientOnDeviceModelProvider(
    private val response: String = "Fake response",
    override val modelName: String = "Fake on-device model",
    override val isAvailable: Boolean = true,
) : OnDeviceModelProvider {
    var receivedPrompt: String = ""
        private set

    override suspend fun generateContent(prompt: String): String {
        receivedPrompt = prompt
        return response
    }
}
