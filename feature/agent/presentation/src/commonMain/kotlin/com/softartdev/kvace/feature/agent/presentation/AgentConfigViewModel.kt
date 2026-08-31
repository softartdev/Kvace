package com.softartdev.kvace.feature.agent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTestResult
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTester
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialRepository
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AgentConfigViewModel(
    private val repository: AgentConfigurationRepository,
    private val credentialRepository: ProviderCredentialRepository,
    private val connectionTester: AgentConnectionTester,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {
    private val logger = Logger.withTag("AgentConfigViewModel")

    val uiState: StateFlow<AgentConfigUiState>
        field: MutableStateFlow<AgentConfigUiState> = MutableStateFlow(AgentConfigUiState())

    private var isObservingProviders = false
    private var isOpenAiModelInputInitialized = false
    private var openAiModelUpdateJob: Job? = null
    private var openAiOperationJob: Job? = null

    fun observeProviders() {
        if (isObservingProviders) return
        isObservingProviders = true

        combine(repository.providers, repository.selectedProvider) { providers, selected ->
            providers to selected
        }.onEach { (providers, selected) ->
            val openAiProvider = providers.firstOrNull { it.id == AgentProviderId.OpenAI }
            val shouldInitializeOpenAiModel = !isOpenAiModelInputInitialized && openAiProvider != null
            uiState.update { state ->
                state.copy(
                    providers = providers,
                    selectedProviderId = selected?.id,
                    openAiModelInput = if (shouldInitializeOpenAiModel) {
                        openAiProvider.modelName
                    } else {
                        state.openAiModelInput
                    },
                    openAiEndpointInput = if (shouldInitializeOpenAiModel) {
                        openAiProvider.endpoint.orEmpty()
                    } else {
                        state.openAiEndpointInput
                    },
                    openAiCredentialStatus = credentialRepository.openAiStatus.value,
                )
            }
            if (shouldInitializeOpenAiModel) {
                isOpenAiModelInputInitialized = true
            }
        }.launchIn(viewModelScope)
    }

    fun selectProvider(id: AgentProviderId) {
        viewModelScope.launch(dispatchers.io) {
            repository.selectProvider(id)
            logger.i { "Selected provider: $id" }
        }
    }

    fun onAction(action: AgentConfigAction) {
        when (action) {
            is AgentConfigAction.OpenAiModelChanged -> updateOpenAiModel(action.modelName)
            is AgentConfigAction.OpenAiEndpointChanged -> updateOpenAiEndpoint(action.endpoint)
            is AgentConfigAction.OpenAiApiKeySubmitted -> saveAndVerifyOpenAiKey(action.apiKey)
            AgentConfigAction.OpenAiCredentialDeleted -> deleteOpenAiKey()
            is AgentConfigAction.OpenAiStorageUnlocked -> unlockOpenAiStorage(action.masterPassword)
            AgentConfigAction.OpenAiLockedCredentialCleared -> clearLockedCredential()
            AgentConfigAction.OpenAiResetRequested -> uiState.update {
                it.copy(isOpenAiResetDialogVisible = true, openAiResetStatus = ProviderResetStatus.Idle)
            }
            AgentConfigAction.OpenAiResetConfirmed -> resetOpenAiProvider()
            AgentConfigAction.OpenAiResetDismissed -> uiState.update {
                it.copy(isOpenAiResetDialogVisible = false)
            }
            is AgentConfigAction.ProviderSelected -> selectProvider(action.id)
        }
    }

    private fun updateOpenAiModel(modelName: String) {
        val trimmedModelName = modelName.trim()
        uiState.update {
            it.copy(
                openAiModelInput = modelName,
                openAiModelValidationError = if (trimmedModelName.isEmpty()) {
                    OpenAiModelValidationError.Required
                } else {
                    null
                },
            )
        }
        openAiModelUpdateJob?.cancel()
        if (trimmedModelName.isEmpty()) return

        openAiModelUpdateJob = viewModelScope.launch(dispatchers.io) {
            val provider = repository.providers.value
                .firstOrNull { it.id == AgentProviderId.OpenAI }
                ?: return@launch
            repository.updateProvider(provider.copy(modelName = trimmedModelName, isConfigured = false))
            logger.i { "Updated provider model: ${AgentProviderId.OpenAI}" }
        }
    }

    private fun updateOpenAiEndpoint(endpoint: String) {
        uiState.update { it.copy(openAiEndpointInput = endpoint, openAiConnectionStatus = OpenAiConnectionStatus.Idle) }
        openAiModelUpdateJob?.cancel()
        openAiModelUpdateJob = viewModelScope.launch(dispatchers.io) {
            val provider = repository.providers.value.firstOrNull { it.id == AgentProviderId.OpenAI } ?: return@launch
            runCatching { repository.updateProvider(provider.copy(endpoint = endpoint, isConfigured = false)) }
                .onFailure { uiState.update { state -> state.copy(openAiConnectionStatus = OpenAiConnectionStatus.Failure(null)) } }
        }
    }

    private fun saveAndVerifyOpenAiKey(apiKey: String) {
        openAiOperationJob?.cancel()
        openAiOperationJob = viewModelScope.launch(dispatchers.io) {
            uiState.update { it.copy(openAiConnectionStatus = OpenAiConnectionStatus.Verifying) }
            when (credentialRepository.saveOpenAiApiKey(apiKey)) {
                ProviderCredentialResult.Success -> verifyOpenAiProvider()
                else -> uiState.update { it.copy(openAiConnectionStatus = OpenAiConnectionStatus.Failure(null)) }
            }
        }
    }

    private fun deleteOpenAiKey() = viewModelScope.launch(dispatchers.io) {
        credentialRepository.deleteOpenAiApiKey()
        repository.providers.value.firstOrNull { it.id == AgentProviderId.OpenAI }?.let { provider ->
            repository.updateProvider(provider.copy(isConfigured = false))
        }
    }

    private fun unlockOpenAiStorage(masterPassword: String) = viewModelScope.launch(dispatchers.io) {
        credentialRepository.unlockOpenAiApiKey(masterPassword)
        uiState.update { it.copy(openAiCredentialStatus = credentialRepository.openAiStatus.value) }
    }

    private fun clearLockedCredential() = viewModelScope.launch(dispatchers.io) {
        credentialRepository.clearLockedOpenAiApiKey()
        uiState.update { it.copy(openAiCredentialStatus = credentialRepository.openAiStatus.value) }
    }

    private fun resetOpenAiProvider() {
        openAiModelUpdateJob?.cancel()
        openAiOperationJob?.cancel()
        uiState.update {
            it.copy(
                isOpenAiResetDialogVisible = false,
                openAiResetStatus = ProviderResetStatus.Resetting,
            )
        }
        openAiOperationJob = viewModelScope.launch(dispatchers.io) {
            try {
                repository.resetProvider(AgentProviderId.OpenAI)
                val provider = repository.providers.value.first { it.id == AgentProviderId.OpenAI }
                uiState.update {
                    it.copy(
                        openAiModelInput = provider.modelName,
                        openAiEndpointInput = provider.endpoint.orEmpty(),
                        openAiModelValidationError = null,
                        openAiConnectionStatus = OpenAiConnectionStatus.Idle,
                        openAiResetStatus = ProviderResetStatus.Idle,
                    )
                }
            } catch (error: Throwable) {
                currentCoroutineContext().ensureActive()
                logger.e(error) { "Failed to reset OpenAI provider" }
                uiState.update { it.copy(openAiResetStatus = ProviderResetStatus.Failure) }
            }
        }
    }

    private suspend fun verifyOpenAiProvider() {
        val provider = repository.providers.value.firstOrNull { it.id == AgentProviderId.OpenAI } ?: return
        val result = connectionTester.testConnection(provider)
        currentCoroutineContext().ensureActive()
        when (result) {
            AgentConnectionTestResult.Success -> {
                repository.updateProvider(provider.copy(isConfigured = true))
                uiState.update { it.copy(openAiConnectionStatus = OpenAiConnectionStatus.Success) }
            }
            is AgentConnectionTestResult.Failure -> uiState.update {
                it.copy(openAiConnectionStatus = OpenAiConnectionStatus.Failure(result.message))
            }
        }
    }
}
