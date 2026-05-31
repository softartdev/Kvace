package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.core.data.settings.PersistentSettings
import com.softartdev.kvace.core.data.settings.PersistentSettingsFactory
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsAgentConfigurationRepository(
    private val ollamaEndpointProvider: OllamaEndpointProvider,
    private val onDeviceModelProvider: OnDeviceModelProvider,
    settingsFactory: PersistentSettingsFactory,
) : AgentConfigurationRepository {
    private val settings: PersistentSettings = settingsFactory.create(SETTINGS_NAME)
    private val initialProviders = createInitialProviders()

    private val _providers = MutableStateFlow(initialProviders)
    override val providers: StateFlow<List<AgentProviderConfig>> = _providers.asStateFlow()

    private val _selectedProvider = MutableStateFlow(initialSelectedProvider(initialProviders))
    override val selectedProvider: StateFlow<AgentProviderConfig?> = _selectedProvider.asStateFlow()

    override suspend fun selectProvider(id: AgentProviderId) {
        val provider = _providers.value.firstOrNull { it.id == id } ?: return
        settings.putString(KEY_SELECTED_PROVIDER_ID, id.name)
        _selectedProvider.value = provider
    }

    override suspend fun updateProvider(config: AgentProviderConfig) {
        _providers.value = _providers.value.map { provider ->
            if (provider.id == config.id) config else provider
        }
        if (_selectedProvider.value.id == config.id) {
            _selectedProvider.value = config
        }
        saveProvider(config)
    }

    private fun createInitialProviders(): List<AgentProviderConfig> {
        val defaultOllamaEndpoint = ollamaEndpointProvider.defaultEndpoint()
        return listOf(
            AgentProviderConfig(
                id = AgentProviderId.Ollama,
                modelName = settings.getStringOrNull(KEY_OLLAMA_MODEL) ?: DEFAULT_OLLAMA_MODEL,
                endpoint = settings.getStringOrNull(KEY_OLLAMA_ENDPOINT) ?: defaultOllamaEndpoint,
                isConfigured = settings.getBoolean(KEY_OLLAMA_CONFIGURED, defaultValue = true),
            ),
            AgentProviderConfig(
                id = AgentProviderId.OnDevice,
                modelName = settings.getStringOrNull(KEY_ON_DEVICE_MODEL) ?: onDeviceModelProvider.modelName,
                isConfigured = onDeviceModelProvider.isAvailable,
            ),
            AgentProviderConfig(
                id = AgentProviderId.OpenAI,
                modelName = settings.getStringOrNull(KEY_OPENAI_MODEL) ?: DEFAULT_OPENAI_MODEL,
                isConfigured = settings.getBoolean(KEY_OPENAI_CONFIGURED, defaultValue = false),
            ),
        )
    }

    private fun initialSelectedProvider(providers: List<AgentProviderConfig>): AgentProviderConfig {
        val savedProviderId = settings.getStringOrNull(KEY_SELECTED_PROVIDER_ID)
            ?.let { runCatching { AgentProviderId.valueOf(it) }.getOrNull() }
            ?: AgentProviderId.Ollama

        return providers.firstOrNull { it.id == savedProviderId }
            ?: providers.first { it.id == AgentProviderId.Ollama }
    }

    private fun saveProvider(config: AgentProviderConfig) {
        when (config.id) {
            AgentProviderId.Ollama -> {
                config.endpoint?.let { settings.putString(KEY_OLLAMA_ENDPOINT, it) }
                settings.putString(KEY_OLLAMA_MODEL, config.modelName)
                settings.putBoolean(KEY_OLLAMA_CONFIGURED, config.isConfigured)
            }
            AgentProviderId.OpenAI -> {
                settings.putString(KEY_OPENAI_MODEL, config.modelName)
                settings.putBoolean(KEY_OPENAI_CONFIGURED, config.isConfigured)
            }
            AgentProviderId.OnDevice -> {
                settings.putString(KEY_ON_DEVICE_MODEL, config.modelName)
            }
        }
    }

    private companion object {
        const val SETTINGS_NAME = "kvace_agent_configuration"

        const val KEY_SELECTED_PROVIDER_ID = "selected_provider_id"
        const val KEY_OLLAMA_ENDPOINT = "ollama_endpoint"
        const val KEY_OLLAMA_MODEL = "ollama_model"
        const val KEY_OLLAMA_CONFIGURED = "ollama_configured"
        const val KEY_OPENAI_MODEL = "openai_model"
        const val KEY_OPENAI_CONFIGURED = "openai_configured"
        const val KEY_ON_DEVICE_MODEL = "on_device_model"

        const val DEFAULT_OLLAMA_MODEL = "qwen3.5:0.8b"
        const val DEFAULT_OPENAI_MODEL = "gpt-4o"
    }
}
