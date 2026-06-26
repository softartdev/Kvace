package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.core.data.settings.InMemoryPersistentSettingsFactory
import com.softartdev.kvace.core.data.settings.PersistentSettings
import com.softartdev.kvace.core.data.settings.PersistentSettingsFactory
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    @Test
    fun failedProviderWriteDoesNotPublishInMemoryState() = runTest {
        val repository = SettingsAgentConfigurationRepository(
            ollamaEndpointProvider = StaticOllamaEndpointProvider(LOOPBACK_HOST),
            onDeviceModelProvider = FakeOnDeviceModelProvider(),
            settingsFactory = FailingPersistentSettingsFactory(),
        )
        val original = repository.providers.value.first { it.id == AgentProviderId.Ollama }
        val updated = original.copy(modelName = "mistral:latest")

        assertFailsWith<IllegalStateException> {
            repository.updateProvider(updated)
        }
        assertEquals(original, repository.providers.value.first { it.id == AgentProviderId.Ollama })
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

private class FailingPersistentSettingsFactory : PersistentSettingsFactory {
    override fun create(name: String): PersistentSettings = object : PersistentSettings {
        override fun getStringOrNull(key: String): String? = null
        override fun putString(key: String, value: String) {
            if (key == "ollama_model") error("write failed")
        }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = defaultValue
        override fun putBoolean(key: String, value: Boolean) = Unit
    }
}

private class FakeOnDeviceModelProvider(
    override val modelName: String = "Fake on-device model",
    override val isAvailable: Boolean = true,
) : OnDeviceModelProvider {
    override suspend fun generateContent(prompt: String): String = "Fake response"
}
