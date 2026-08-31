package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.core.data.settings.InMemoryPersistentSettingsFactory
import com.softartdev.kvace.core.data.settings.PersistentSettings
import com.softartdev.kvace.core.data.settings.PersistentSettingsFactory
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialRepository
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialResult
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsAgentConfigurationRepositoryTest {

    @Test
    fun legacyOllamaConfigurationRequiresCatalogRevalidation() {
        val settingsFactory = InMemoryPersistentSettingsFactory()
        settingsFactory.agentSettings().apply {
            putString("ollama_endpoint", "http://127.0.0.1:11434")
            putString("ollama_model", "qwen3.5:0.8b")
            putBoolean("ollama_configured", true)
        }

        val provider = createRepository(settingsFactory).ollamaProvider()

        assertFalse(provider.isConfigured)
    }

    @Test
    fun restoresCatalogValidatedOllamaConfiguration() {
        val settingsFactory = InMemoryPersistentSettingsFactory()
        settingsFactory.agentSettings().apply {
            putString("ollama_endpoint", "http://127.0.0.1:11434")
            putString("ollama_model", "mistral:latest")
            putBoolean("ollama_configured", true)
            putString("ollama_validated_endpoint", "http://127.0.0.1:11434")
            putString("ollama_validated_model", "mistral:latest")
        }

        val provider = createRepository(settingsFactory).ollamaProvider()

        assertTrue(provider.isConfigured)
    }

    @Test
    fun configuredOllamaWritePersistsValidationMarkers() = runTest {
        val settingsFactory = InMemoryPersistentSettingsFactory()
        val repository = createRepository(settingsFactory)
        val configured = repository.ollamaProvider().copy(
            endpoint = "http://127.0.0.1:11434",
            modelName = "mistral:latest",
            isConfigured = true,
        )

        repository.updateProvider(configured)

        val restored = createRepository(settingsFactory).ollamaProvider()
        assertEquals(configured, restored)
        assertTrue(restored.isConfigured)
    }

    @Test
    fun rejectsOllamaConfigurationWhenValidationStampDoesNotMatch() {
        val settingsFactory = InMemoryPersistentSettingsFactory()
        settingsFactory.agentSettings().apply {
            putString("ollama_endpoint", "http://127.0.0.1:11434")
            putString("ollama_model", "mistral:latest")
            putBoolean("ollama_configured", true)
            putString("ollama_validated_endpoint", "http://other-host:11434")
            putString("ollama_validated_model", "llama3.2:latest")
        }

        val provider = createRepository(settingsFactory).ollamaProvider()

        assertFalse(provider.isConfigured)
    }

    @Test
    fun rejectsStampedOllamaConfigurationWithInvalidEndpoint() {
        val settingsFactory = InMemoryPersistentSettingsFactory()
        settingsFactory.agentSettings().apply {
            putString("ollama_endpoint", "http://bad host:11434")
            putString("ollama_model", "mistral:latest")
            putBoolean("ollama_configured", true)
            putString("ollama_validated_endpoint", "http://bad host:11434")
            putString("ollama_validated_model", "mistral:latest")
        }

        val provider = createRepository(settingsFactory).ollamaProvider()

        assertFalse(provider.isConfigured)
    }

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
    fun platformOnDeviceLabelOverridesLegacySavedValue() {
        val settingsFactory = InMemoryPersistentSettingsFactory()
        settingsFactory.create("kvace_agent_configuration")
            .putString("on_device_model", "Legacy saved model")

        val repository = createRepository(
            settingsFactory = settingsFactory,
            onDeviceModelProvider = FakeOnDeviceModelProvider(
                modelName = APPLE_ON_DEVICE_MODEL_LABEL,
                isAvailable = true,
            ),
        )

        val provider = repository.providers.value.first { it.id == AgentProviderId.OnDevice }
        assertEquals(APPLE_ON_DEVICE_MODEL_LABEL, provider.modelName)
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
            ollamaEndpointValidator = KtorOllamaEndpointValidator(),
            onDeviceModelProvider = FakeOnDeviceModelProvider(),
            credentialRepository = FakeCredentialRepository(),
            openAiEndpointValidator = DefaultOpenAiEndpointValidator(),
            settingsFactory = FailingPersistentSettingsFactory(),
        )
        val original = repository.providers.value.first { it.id == AgentProviderId.Ollama }
        val updated = original.copy(modelName = "mistral:latest")

        assertFailsWith<IllegalStateException> {
            repository.updateProvider(updated)
        }
        assertEquals(original, repository.providers.value.first { it.id == AgentProviderId.Ollama })
    }

    @Test
    fun resetsOllamaToPlatformDefaultsAndRemovesValidationMarkers() = runTest {
        val settingsFactory = InMemoryPersistentSettingsFactory()
        val repository = createRepository(settingsFactory)
        repository.updateProvider(
            repository.ollamaProvider().copy(
                endpoint = "http://remote-host:12345",
                modelName = "mistral:latest",
                isConfigured = true,
            )
        )
        repository.selectProvider(AgentProviderId.Ollama)

        repository.resetProvider(AgentProviderId.Ollama)

        val reset = repository.ollamaProvider()
        assertEquals("http://127.0.0.1:11434", reset.endpoint)
        assertEquals("qwen3.5:0.8b", reset.modelName)
        assertFalse(reset.isConfigured)
        assertEquals(AgentProviderId.Ollama, repository.selectedProvider.value?.id)
        assertNull(settingsFactory.agentSettings().getStringOrNull("ollama_validated_endpoint"))
        assertNull(settingsFactory.agentSettings().getStringOrNull("ollama_validated_model"))
        assertEquals(reset, createRepository(settingsFactory).ollamaProvider())
    }

    @Test
    fun resetsOpenAiWithoutDeletingCredentialOrChangingSelection() = runTest {
        val settingsFactory = InMemoryPersistentSettingsFactory()
        val credentialRepository = FakeCredentialRepository(ProviderCredentialStatus.Stored)
        val repository = createRepository(settingsFactory, credentialRepository = credentialRepository)
        val custom = repository.providers.value.first { it.id == AgentProviderId.OpenAI }.copy(
            endpoint = "https://example.com/v1",
            modelName = "custom-model",
            isConfigured = true,
        )
        repository.updateProvider(custom)
        repository.selectProvider(AgentProviderId.OpenAI)

        repository.resetProvider(AgentProviderId.OpenAI)

        val reset = repository.providers.value.first { it.id == AgentProviderId.OpenAI }
        assertEquals("https://api.openai.com", reset.endpoint)
        assertEquals("gpt-4o", reset.modelName)
        assertFalse(reset.isConfigured)
        assertEquals(reset, repository.selectedProvider.value)
        assertEquals(0, credentialRepository.deleteCallCount)
        assertNull(settingsFactory.agentSettings().getStringOrNull("openai_validated_endpoint"))
        assertNull(settingsFactory.agentSettings().getStringOrNull("openai_validated_model"))
        val restored = createRepository(
            settingsFactory = settingsFactory,
            credentialRepository = credentialRepository,
        ).providers.value.first { it.id == AgentProviderId.OpenAI }
        assertEquals(reset, restored)
    }

    @Test
    fun resetOnDeviceIsNoOp() = runTest {
        val repository = createRepository()
        val originalProviders = repository.providers.value

        repository.resetProvider(AgentProviderId.OnDevice)

        assertEquals(originalProviders, repository.providers.value)
    }

    @Test
    fun failedResetDoesNotPublishInMemoryState() = runTest {
        val repository = SettingsAgentConfigurationRepository(
            ollamaEndpointProvider = StaticOllamaEndpointProvider(LOOPBACK_HOST),
            ollamaEndpointValidator = KtorOllamaEndpointValidator(),
            onDeviceModelProvider = FakeOnDeviceModelProvider(),
            credentialRepository = FakeCredentialRepository(),
            openAiEndpointValidator = DefaultOpenAiEndpointValidator(),
            settingsFactory = ResetFailingPersistentSettingsFactory(),
        )
        val original = repository.ollamaProvider()

        assertFailsWith<IllegalStateException> {
            repository.resetProvider(AgentProviderId.Ollama)
        }

        assertEquals(original, repository.ollamaProvider())
    }

    private fun createRepository(
        settingsFactory: InMemoryPersistentSettingsFactory = InMemoryPersistentSettingsFactory(),
        onDeviceModelProvider: OnDeviceModelProvider = FakeOnDeviceModelProvider(),
        credentialRepository: ProviderCredentialRepository = FakeCredentialRepository(),
    ) = SettingsAgentConfigurationRepository(
        ollamaEndpointProvider = StaticOllamaEndpointProvider(LOOPBACK_HOST),
        ollamaEndpointValidator = KtorOllamaEndpointValidator(),
        onDeviceModelProvider = onDeviceModelProvider,
        credentialRepository = credentialRepository,
        openAiEndpointValidator = DefaultOpenAiEndpointValidator(),
        settingsFactory = settingsFactory,
    )

    private fun InMemoryPersistentSettingsFactory.agentSettings(): PersistentSettings =
        create("kvace_agent_configuration")

    private fun SettingsAgentConfigurationRepository.ollamaProvider() =
        providers.value.first { it.id == AgentProviderId.Ollama }
}

private class FakeCredentialRepository(
    status: ProviderCredentialStatus = ProviderCredentialStatus.Absent,
) : ProviderCredentialRepository {
    override val openAiStatus = MutableStateFlow(status)
    var deleteCallCount = 0
    override suspend fun readOpenAiApiKey(): String? = null
    override suspend fun saveOpenAiApiKey(apiKey: String): ProviderCredentialResult = ProviderCredentialResult.Success
    override suspend fun deleteOpenAiApiKey(): ProviderCredentialResult {
        deleteCallCount++
        return ProviderCredentialResult.Success
    }
    override suspend fun unlockOpenAiApiKey(masterPassword: String): ProviderCredentialResult = ProviderCredentialResult.Success
    override suspend fun clearLockedOpenAiApiKey(): ProviderCredentialResult = ProviderCredentialResult.Success
}

private class FailingPersistentSettingsFactory : PersistentSettingsFactory {
    override fun create(name: String): PersistentSettings = object : PersistentSettings() {
        override fun getStringOrNull(key: String): String? = null
        override fun putString(key: String, value: String) {
            if (key == "ollama_model") error("write failed")
        }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = defaultValue
        override fun putBoolean(key: String, value: Boolean) = Unit
        override fun remove(key: String) = Unit
    }
}

private class ResetFailingPersistentSettingsFactory : PersistentSettingsFactory {
    private val values = mutableMapOf(
        "ollama_endpoint" to "http://remote-host:12345",
        "ollama_model" to "mistral:latest",
        "ollama_configured" to "true",
        "ollama_validated_endpoint" to "http://remote-host:12345",
        "ollama_validated_model" to "mistral:latest",
    )

    override fun create(name: String): PersistentSettings = object : PersistentSettings() {
        override fun getStringOrNull(key: String): String? = values[key]
        override fun putString(key: String, value: String) {
            values[key] = value
        }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
            values[key]?.toBooleanStrictOrNull() ?: defaultValue
        override fun putBoolean(key: String, value: Boolean) {
            values[key] = value.toString()
        }
        override fun remove(key: String) {
            error("remove failed")
        }
    }
}

private class FakeOnDeviceModelProvider(
    override val modelName: String = "Fake on-device model",
    override val isAvailable: Boolean = true,
) : OnDeviceModelProvider {
    override suspend fun generateContent(prompt: String): String = "Fake response"
}
