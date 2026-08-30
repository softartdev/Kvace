package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.core.data.settings.PersistentSettings
import com.softartdev.kvace.core.data.settings.PersistentSettingsFactory
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.domain.OllamaEndpointValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsAgentConfigurationRepository(
    private val ollamaEndpointProvider: OllamaEndpointProvider,
    private val ollamaEndpointValidator: OllamaEndpointValidator,
    private val onDeviceModelProvider: OnDeviceModelProvider,
    settingsFactory: PersistentSettingsFactory,
) : AgentConfigurationRepository {
    private val settings: PersistentSettings = settingsFactory.create(SETTINGS_NAME)
    private val initialProviders = createInitialProviders()

    override val providers: StateFlow<List<AgentProviderConfig>>
        field = MutableStateFlow(initialProviders)

    override val selectedProvider: StateFlow<AgentProviderConfig?>
        field = MutableStateFlow(initialSelectedProvider(initialProviders))

    override suspend fun selectProvider(id: AgentProviderId) {
        val provider: AgentProviderConfig = providers.value.firstOrNull { it.id == id } ?: return
        settings.putString(KEY_SELECTED_PROVIDER_ID, id.name)
        selectedProvider.value = provider
    }

    override suspend fun updateProvider(config: AgentProviderConfig) {
        saveProvider(config)
        providers.value = providers.value.map { provider ->
            if (provider.id == config.id) config else provider
        }
        if (selectedProvider.value.id == config.id) {
            selectedProvider.value = config
        }
    }

    private fun createInitialProviders(): List<AgentProviderConfig> {
        val ollamaEndpoint = settings.getStringOrNull(KEY_OLLAMA_ENDPOINT) ?: ollamaEndpointProvider.defaultEndpoint()
        val ollamaModel = settings.getStringOrNull(KEY_OLLAMA_MODEL) ?: DEFAULT_OLLAMA_MODEL
        val validatedEndpoint = ollamaEndpointValidator.parse(ollamaEndpoint)?.value
        val isOllamaConfigured = settings.getBoolean(KEY_OLLAMA_CONFIGURED, defaultValue = false) &&
            validatedEndpoint == settings.getStringOrNull(KEY_OLLAMA_VALIDATED_ENDPOINT) &&
            ollamaModel == settings.getStringOrNull(KEY_OLLAMA_VALIDATED_MODEL)

        return listOf(
            AgentProviderConfig(
                id = AgentProviderId.Ollama,
                modelName = ollamaModel,
                endpoint = ollamaEndpoint,
                isConfigured = isOllamaConfigured,
            ),
            AgentProviderConfig(
                id = AgentProviderId.OnDevice,
                modelName = onDeviceModelProvider.modelName,
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
        val savedProviderId: AgentProviderId = settings.getStringOrNull(KEY_SELECTED_PROVIDER_ID)
            ?.let { runCatching { AgentProviderId.valueOf(it) }.getOrNull() }
            ?: AgentProviderId.Ollama

        return providers.firstOrNull { it.id == savedProviderId }
            ?: providers.first { it.id == AgentProviderId.Ollama }
    }

    private fun saveProvider(config: AgentProviderConfig) = when (config.id) {
        AgentProviderId.Ollama -> {
            val endpoint = config.endpoint
            val validatedEndpoint = endpoint?.let(ollamaEndpointValidator::parse)
            require(!config.isConfigured || validatedEndpoint?.value == endpoint) {
                "Configured Ollama endpoint must be valid and normalized."
            }
            require(!config.isConfigured || config.modelName.isNotBlank()) {
                "Configured Ollama model must not be blank."
            }
            config.endpoint?.let { settings.putString(KEY_OLLAMA_ENDPOINT, it) }
            settings.putString(KEY_OLLAMA_MODEL, config.modelName)
            settings.putBoolean(KEY_OLLAMA_CONFIGURED, config.isConfigured)
            if (config.isConfigured) {
                settings.putString(KEY_OLLAMA_VALIDATED_ENDPOINT, endpoint.orEmpty())
                settings.putString(KEY_OLLAMA_VALIDATED_MODEL, config.modelName)
            }
            Unit
        }
        AgentProviderId.OpenAI -> {
            settings.putString(KEY_OPENAI_MODEL, config.modelName)
            settings.putBoolean(KEY_OPENAI_CONFIGURED, config.isConfigured)
        }
        AgentProviderId.OnDevice -> Unit
    }

    private companion object {
        const val SETTINGS_NAME = "kvace_agent_configuration"

        const val KEY_SELECTED_PROVIDER_ID = "selected_provider_id"
        const val KEY_OLLAMA_ENDPOINT = "ollama_endpoint"
        const val KEY_OLLAMA_MODEL = "ollama_model"
        const val KEY_OLLAMA_CONFIGURED = "ollama_configured"
        const val KEY_OLLAMA_VALIDATED_ENDPOINT = "ollama_validated_endpoint"
        const val KEY_OLLAMA_VALIDATED_MODEL = "ollama_validated_model"
        const val KEY_OPENAI_MODEL = "openai_model"
        const val KEY_OPENAI_CONFIGURED = "openai_configured"
        const val DEFAULT_OLLAMA_MODEL = "qwen3.5:0.8b"
        const val DEFAULT_OPENAI_MODEL = "gpt-4o"
    }
}
