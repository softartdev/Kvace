package com.softartdev.kvace.feature.agent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AgentConfigViewModel(
    private val repository: AgentConfigurationRepository,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {
    private val logger = Logger.withTag("AgentConfigViewModel")

    val uiState: StateFlow<AgentConfigUiState>
        field: MutableStateFlow<AgentConfigUiState> = MutableStateFlow(AgentConfigUiState())

    private var isObservingProviders = false
    private var isOpenAiModelInputInitialized = false
    private var openAiModelUpdateJob: Job? = null

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
            repository.updateProvider(provider.copy(modelName = trimmedModelName))
            logger.i { "Updated provider model: ${AgentProviderId.OpenAI}" }
        }
    }
}
