package com.softartdev.kvace.feature.agent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTestResult
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTester
import com.softartdev.kvace.feature.agent.domain.AgentModelCatalog
import com.softartdev.kvace.feature.agent.domain.AgentModelListResult
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OllamaEndpointSettingsViewModel(
    private val repository: AgentConfigurationRepository,
    private val connectionTester: AgentConnectionTester,
    private val modelCatalog: AgentModelCatalog,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {
    private val logger = Logger.withTag("OllamaEndpointSettingsViewModel")

    val uiState: StateFlow<OllamaEndpointSettingsUiState>
        field: MutableStateFlow<OllamaEndpointSettingsUiState> = MutableStateFlow(OllamaEndpointSettingsUiState())

    private var isObservingEndpoint = false
    private var isInputInitialized = false
    private var isModelInputInitialized = false
    private var ollamaProviderConfig: AgentProviderConfig? = null

    fun observeEndpoint() {
        if (isObservingEndpoint) return
        isObservingEndpoint = true

        repository.providers.onEach { providers ->
            val ollamaConfig: AgentProviderConfig? = providers.firstOrNull { it.id == AgentProviderId.Ollama }
            ollamaProviderConfig = ollamaConfig
            val endpoint = ollamaConfig?.endpoint
            val endpointInput = parseEndpoint(endpoint)
            val shouldInitializeInput = !isInputInitialized && endpoint != null
            val shouldInitializeModel = !isModelInputInitialized && ollamaConfig != null
            uiState.update { state ->
                state.copy(
                    hostInput = if (shouldInitializeInput) endpointInput.host else state.hostInput,
                    portInput = if (shouldInitializeInput) endpointInput.port else state.portInput,
                    modelInput = if (shouldInitializeModel) ollamaConfig.modelName else state.modelInput,
                )
            }
            if (shouldInitializeInput) {
                isInputInitialized = true
            }
            if (shouldInitializeModel) {
                isModelInputInitialized = true
            }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: OllamaEndpointSettingsAction) {
        when (action) {
            is OllamaEndpointSettingsAction.HostChanged -> uiState.update {
                it.copy(hostInput = action.host, connectionStatus = OllamaConnectionStatus.Idle)
            }
            is OllamaEndpointSettingsAction.PortChanged -> uiState.update {
                it.copy(portInput = action.port, connectionStatus = OllamaConnectionStatus.Idle)
            }
            is OllamaEndpointSettingsAction.ModelChanged -> updateModelInput(action.modelName)
            is OllamaEndpointSettingsAction.ModelSelected -> selectModel(action.modelName)
            OllamaEndpointSettingsAction.TestConnection -> testConnection()
            OllamaEndpointSettingsAction.LoadModels -> loadModels()
        }
    }

    private fun testConnection() {
        val state = uiState.value
        if (state.connectionStatus == OllamaConnectionStatus.Testing) return

        val host = state.hostInput.trim()
        if (host.isBlank()) {
            logger.w { "Rejected blank Ollama host" }
            uiState.update { it.copy(connectionStatus = OllamaConnectionStatus.InvalidHost) }
            return
        }
        val port = state.portInput.trim().toIntOrNull()
        if (port == null || port !in PORT_RANGE) {
            logger.w { "Rejected invalid Ollama port: ${state.portInput}" }
            uiState.update { it.copy(connectionStatus = OllamaConnectionStatus.InvalidPort) }
            return
        }
        val config = ollamaProviderConfig ?: return
        val pendingConfig = config.copy(
            endpoint = "http://$host:$port",
            isConfigured = false,
        )
        uiState.update { it.copy(connectionStatus = OllamaConnectionStatus.Testing) }
        viewModelScope.launch(dispatchers.io) {
            repository.updateProvider(pendingConfig)
            when (val result = connectionTester.testConnection(pendingConfig)) {
                AgentConnectionTestResult.Success -> {
                    logger.i { "Connected to ${pendingConfig.endpoint}" }
                    val configured = pendingConfig.copy(isConfigured = true)
                    repository.updateProvider(configured)
                    uiState.update { it.copy(connectionStatus = OllamaConnectionStatus.Success) }
                    loadModels(config = configured)
                }
                is AgentConnectionTestResult.Failure -> {
                    logger.w { "Failed to connect to ${pendingConfig.endpoint}: ${result.message}" }
                    uiState.update {
                        it.copy(connectionStatus = OllamaConnectionStatus.Failure(result.message))
                    }
                }
            }
        }
    }

    private fun updateModelInput(modelName: String) {
        uiState.update {
            it.copy(modelInput = modelName, modelsStatus = OllamaModelsStatus.Idle)
        }
        val trimmedModelName = modelName.trim()
        if (trimmedModelName.isNotEmpty()) {
            persistModel(trimmedModelName)
        }
    }

    private fun selectModel(modelName: String) {
        uiState.update {
            it.copy(modelInput = modelName, modelsStatus = OllamaModelsStatus.Loaded)
        }
        persistModel(modelName)
    }

    private fun persistModel(modelName: String) {
        val config = ollamaProviderConfig ?: return
        viewModelScope.launch(dispatchers.io) {
            repository.updateProvider(config.copy(modelName = modelName))
        }
    }

    private fun loadModels() {
        val config = buildPendingConfig() ?: return
        loadModels(config)
    }

    private fun loadModels(config: AgentProviderConfig) {
        if (uiState.value.modelsStatus == OllamaModelsStatus.Loading) return

        uiState.update { it.copy(modelsStatus = OllamaModelsStatus.Loading) }
        viewModelScope.launch(dispatchers.io) {
            when (val result = modelCatalog.loadModels(config)) {
                is AgentModelListResult.Success -> {
                    val selectedModel: String? = selectBestModel(
                        models = result.modelNames,
                        currentModel = uiState.value.modelInput,
                        fallbackModel = config.modelName,
                    )
                    if (selectedModel != null) {
                        repository.updateProvider(config.copy(modelName = selectedModel, isConfigured = true))
                    }
                    uiState.update {
                        val modelsStatus: OllamaModelsStatus =
                            if (result.modelNames.isEmpty()) OllamaModelsStatus.Empty else OllamaModelsStatus.Loaded
                        it.copy(
                            modelInput = selectedModel ?: it.modelInput,
                            availableModels = result.modelNames,
                            modelsStatus = modelsStatus,
                        )
                    }
                }
                is AgentModelListResult.Failure -> {
                    logger.w { "Failed to load models: ${result.message}" }
                    uiState.update {
                        it.copy(modelsStatus = OllamaModelsStatus.Failure(result.message))
                    }
                }
            }
        }
    }

    private fun buildPendingConfig(): AgentProviderConfig? {
        val state = uiState.value
        val host = state.hostInput.trim()
        if (host.isBlank()) {
            uiState.update { it.copy(connectionStatus = OllamaConnectionStatus.InvalidHost) }
            return null
        }
        val port = state.portInput.trim().toIntOrNull()
        if (port == null || port !in PORT_RANGE) {
            uiState.update { it.copy(connectionStatus = OllamaConnectionStatus.InvalidPort) }
            return null
        }
        val modelName = state.modelInput.trim()
        val config = ollamaProviderConfig ?: return null
        return config.copy(
            endpoint = "http://$host:$port",
            modelName = modelName.ifBlank { config.modelName },
            isConfigured = true,
        )
    }

    private data class EndpointInput(val host: String, val port: String)

    private companion object {
        val PORT_RANGE: IntRange = 1..65535

        fun selectBestModel(
            models: List<String>,
            currentModel: String,
            fallbackModel: String,
        ): String? {
            if (models.isEmpty()) return currentModel.ifBlank { fallbackModel }.ifBlank { null }

            val trimmedCurrentModel = currentModel.trim()
            return when {
                trimmedCurrentModel in models -> trimmedCurrentModel
                fallbackModel in models -> fallbackModel
                else -> models.first()
            }
        }

        fun parseEndpoint(endpoint: String?): EndpointInput {
            val hostAndPort = endpoint
                ?.substringAfter("://", endpoint)
                ?.substringBefore("/")
                ?: return EndpointInput(host = "", port = "")
            return EndpointInput(
                host = hostAndPort.substringBefore(":"),
                port = hostAndPort.substringAfter(":", ""),
            )
        }
    }
}
