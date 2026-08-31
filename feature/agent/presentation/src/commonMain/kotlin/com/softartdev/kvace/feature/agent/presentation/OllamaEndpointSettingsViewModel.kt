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
import com.softartdev.kvace.feature.agent.domain.OllamaEndpointValidationResult
import com.softartdev.kvace.feature.agent.domain.OllamaEndpointValidator
import com.softartdev.kvace.feature.agent.domain.ValidatedOllamaEndpoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    private val endpointValidator: OllamaEndpointValidator,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {
    private val logger = Logger.withTag("OllamaEndpointSettingsViewModel")

    val uiState: StateFlow<OllamaEndpointSettingsUiState>
        field: MutableStateFlow<OllamaEndpointSettingsUiState> = MutableStateFlow(OllamaEndpointSettingsUiState())

    private var isObservingEndpoint = false
    private var isInputInitialized = false
    private var isModelInputInitialized = false
    private var ollamaProviderConfig: AgentProviderConfig? = null
    private var endpointOperationJob: Job? = null
    private var catalogEndpoint: String? = null

    fun observeEndpoint() {
        if (isObservingEndpoint) return
        isObservingEndpoint = true

        repository.providers.onEach { providers ->
            val ollamaConfig: AgentProviderConfig? = providers.firstOrNull { it.id == AgentProviderId.Ollama }
            ollamaProviderConfig = ollamaConfig
            val endpoint = ollamaConfig?.endpoint
            val parsedEndpoint = endpoint?.let(endpointValidator::parse)
            val shouldInitializeInput = !isInputInitialized && endpoint != null
            val shouldInitializeModel = !isModelInputInitialized && ollamaConfig != null
            uiState.update { state ->
                state.copy(
                    hostInput = if (shouldInitializeInput) parsedEndpoint?.host.orEmpty() else state.hostInput,
                    portInput = if (shouldInitializeInput) parsedEndpoint?.port?.toString().orEmpty() else state.portInput,
                    modelInput = if (shouldInitializeModel) ollamaConfig.modelName else state.modelInput,
                )
            }
            if (shouldInitializeInput) isInputInitialized = true
            if (shouldInitializeModel) isModelInputInitialized = true
        }.launchIn(viewModelScope)
    }

    fun onAction(action: OllamaEndpointSettingsAction) {
        when (action) {
            is OllamaEndpointSettingsAction.HostChanged -> updateEndpointDraft(host = action.host)
            is OllamaEndpointSettingsAction.PortChanged -> updateEndpointDraft(port = action.port)
            is OllamaEndpointSettingsAction.ModelSelected -> selectModel(action.modelName)
            OllamaEndpointSettingsAction.TestConnection -> testConnection()
            OllamaEndpointSettingsAction.LoadModels -> loadModels()
            OllamaEndpointSettingsAction.ResetRequested -> uiState.update {
                it.copy(isResetDialogVisible = true, resetStatus = ProviderResetStatus.Idle)
            }
            OllamaEndpointSettingsAction.ResetConfirmed -> resetProvider()
            OllamaEndpointSettingsAction.ResetDismissed -> uiState.update {
                it.copy(isResetDialogVisible = false)
            }
        }
    }

    private fun updateEndpointDraft(host: String? = null, port: String? = null) {
        if (uiState.value.resetStatus == ProviderResetStatus.Resetting) return
        endpointOperationJob?.cancel()
        catalogEndpoint = null
        uiState.update { state ->
            state.copy(
                hostInput = host ?: state.hostInput,
                portInput = port ?: state.portInput,
                modelInput = "",
                availableModels = emptyList(),
                connectionStatus = OllamaConnectionStatus.Idle,
                modelsStatus = OllamaModelsStatus.Idle,
            )
        }
    }

    private fun testConnection() {
        if (uiState.value.isBusy) return
        val pendingConfig = buildPendingConfig() ?: return

        endpointOperationJob?.cancel()
        uiState.update {
            it.copy(
                connectionStatus = OllamaConnectionStatus.Testing,
                modelsStatus = OllamaModelsStatus.Idle,
                availableModels = emptyList(),
            )
        }
        endpointOperationJob = viewModelScope.launch(dispatchers.io) {
            repository.updateProvider(pendingConfig)
            when (val result = connectionTester.testConnection(pendingConfig)) {
                AgentConnectionTestResult.Success -> {
                    logger.i { "Connected to ${pendingConfig.endpoint}" }
                    uiState.update { it.copy(connectionStatus = OllamaConnectionStatus.Success) }
                    loadModelsAndApply(pendingConfig)
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

    private fun loadModels() {
        if (uiState.value.isBusy) return
        val pendingConfig = buildPendingConfig() ?: return

        endpointOperationJob?.cancel()
        endpointOperationJob = viewModelScope.launch(dispatchers.io) {
            repository.updateProvider(pendingConfig)
            loadModelsAndApply(pendingConfig)
        }
    }

    private suspend fun loadModelsAndApply(config: AgentProviderConfig) {
        uiState.update { it.copy(modelsStatus = OllamaModelsStatus.Loading) }
        when (val result = modelCatalog.loadModels(config)) {
            is AgentModelListResult.Success -> applyLoadedModels(config, result.modelNames)
            is AgentModelListResult.Failure -> {
                logger.w { "Failed to load models: ${result.message}" }
                catalogEndpoint = null
                uiState.update {
                    it.copy(modelsStatus = OllamaModelsStatus.Failure(result.message))
                }
            }
        }
    }

    private suspend fun applyLoadedModels(config: AgentProviderConfig, modelNames: List<String>) {
        val availableModels = modelNames.filter(String::isNotBlank)
        val selectedModel = config.modelName.takeIf { it in availableModels }
        catalogEndpoint = config.endpoint
        if (selectedModel != null) {
            repository.updateProvider(config.copy(isConfigured = true))
        }
        uiState.update {
            it.copy(
                modelInput = selectedModel.orEmpty(),
                availableModels = availableModels,
                connectionStatus = OllamaConnectionStatus.Success,
                modelsStatus = when {
                    availableModels.isEmpty() -> OllamaModelsStatus.Empty
                    selectedModel == null -> OllamaModelsStatus.SelectionRequired
                    else -> OllamaModelsStatus.Loaded
                },
            )
        }
    }

    private fun selectModel(modelName: String) {
        val state = uiState.value
        if (modelName !in state.availableModels || state.isBusy) return
        val endpoint = validatedEndpoint() ?: return
        if (endpoint.value != catalogEndpoint) return
        val config = ollamaProviderConfig ?: return

        endpointOperationJob?.cancel()
        endpointOperationJob = viewModelScope.launch(dispatchers.io) {
            repository.updateProvider(
                config.copy(
                    modelName = modelName,
                    endpoint = endpoint.value,
                    isConfigured = true,
                )
            )
            uiState.update {
                it.copy(modelInput = modelName, modelsStatus = OllamaModelsStatus.Loaded)
            }
        }
    }

    private fun resetProvider() {
        endpointOperationJob?.cancel()
        catalogEndpoint = null
        uiState.update {
            it.copy(
                isResetDialogVisible = false,
                resetStatus = ProviderResetStatus.Resetting,
            )
        }
        endpointOperationJob = viewModelScope.launch(dispatchers.io) {
            try {
                repository.resetProvider(AgentProviderId.Ollama)
                val provider = repository.providers.value.first { it.id == AgentProviderId.Ollama }
                val endpoint = provider.endpoint?.let(endpointValidator::parse)
                uiState.update {
                    it.copy(
                        hostInput = endpoint?.host.orEmpty(),
                        portInput = endpoint?.port?.toString().orEmpty(),
                        modelInput = provider.modelName,
                        availableModels = emptyList(),
                        connectionStatus = OllamaConnectionStatus.Idle,
                        modelsStatus = OllamaModelsStatus.Idle,
                        resetStatus = ProviderResetStatus.Idle,
                    )
                }
            } catch (error: Throwable) {
                currentCoroutineContext().ensureActive()
                logger.e(error) { "Failed to reset Ollama provider" }
                uiState.update { it.copy(resetStatus = ProviderResetStatus.Failure) }
            }
        }
    }

    private fun buildPendingConfig(): AgentProviderConfig? {
        val endpoint = validatedEndpoint() ?: return null
        val config = ollamaProviderConfig ?: return null
        return config.copy(endpoint = endpoint.value, isConfigured = false)
    }

    private fun validatedEndpoint(): ValidatedOllamaEndpoint? {
        val state = uiState.value
        return when (val result = endpointValidator.validate(state.hostInput, state.portInput)) {
            is OllamaEndpointValidationResult.Valid -> result.endpoint
            OllamaEndpointValidationResult.InvalidHost -> {
                logger.w { "Rejected invalid Ollama host: ${state.hostInput}" }
                uiState.update { it.copy(connectionStatus = OllamaConnectionStatus.InvalidHost) }
                null
            }
            OllamaEndpointValidationResult.InvalidPort -> {
                logger.w { "Rejected invalid Ollama port: ${state.portInput}" }
                uiState.update { it.copy(connectionStatus = OllamaConnectionStatus.InvalidPort) }
                null
            }
        }
    }

    private val OllamaEndpointSettingsUiState.isBusy: Boolean
        get() = connectionStatus == OllamaConnectionStatus.Testing ||
            modelsStatus == OllamaModelsStatus.Loading ||
            resetStatus == ProviderResetStatus.Resetting
}
