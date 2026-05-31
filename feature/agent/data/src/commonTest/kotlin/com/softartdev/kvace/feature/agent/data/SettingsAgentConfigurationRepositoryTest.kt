package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.core.data.settings.InMemoryPersistentSettingsFactory
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsAgentConfigurationRepositoryTest {
    @Test
    fun includesAvailableOnDeviceProvider() {
        val repository = createRepository(
            onDeviceModelProvider = FakeOnDeviceModelProvider(
                modelName = "Test on-device model",
                isAvailable = true,
            ),
        )

        val provider = repository.providers.value.first { it.id == AgentProviderId.OnDevice }

        assertEquals("Test on-device model", provider.modelName)
        assertNull(provider.endpoint)
        assertTrue(provider.isConfigured)
    }

    @Test
    fun marksOnDeviceProviderUnconfiguredWhenPlatformIsUnavailable() {
        val repository = createRepository(
            onDeviceModelProvider = FakeOnDeviceModelProvider(
                modelName = "Unavailable model",
                isAvailable = false,
            ),
        )

        val provider = repository.providers.value.first { it.id == AgentProviderId.OnDevice }

        assertEquals("Unavailable model", provider.modelName)
        assertFalse(provider.isConfigured)
    }

    @Test
    fun persistsSelectedOnDeviceProvider() = runTest {
        val settingsFactory = InMemoryPersistentSettingsFactory()
        val repository = createRepository(settingsFactory = settingsFactory)

        repository.selectProvider(AgentProviderId.OnDevice)

        val restoredRepository = createRepository(settingsFactory = settingsFactory)
        assertEquals(AgentProviderId.OnDevice, restoredRepository.selectedProvider.value?.id)
    }

    private fun createRepository(
        settingsFactory: InMemoryPersistentSettingsFactory = InMemoryPersistentSettingsFactory(),
        onDeviceModelProvider: OnDeviceModelProvider = FakeOnDeviceModelProvider(),
    ) = SettingsAgentConfigurationRepository(
        ollamaEndpointProvider = StaticOllamaEndpointProvider(LOOPBACK_HOST),
        onDeviceModelProvider = onDeviceModelProvider,
        settingsFactory = settingsFactory,
    )
}

private class FakeOnDeviceModelProvider(
    override val modelName: String = "Fake on-device model",
    override val isAvailable: Boolean = true,
) : OnDeviceModelProvider {
    override suspend fun generateContent(prompt: String): String = "Fake response"
}
