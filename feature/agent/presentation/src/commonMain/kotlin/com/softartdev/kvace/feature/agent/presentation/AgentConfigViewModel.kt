package com.softartdev.kvace.feature.agent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
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

    fun observeProviders() {
        if (isObservingProviders) return
        isObservingProviders = true

        combine(repository.providers, repository.selectedProvider) { providers, selected ->
            providers to selected
        }.onEach { (providers, selected) ->
            uiState.update { state ->
                state.copy(
                    providers = providers,
                    selectedProviderId = selected?.id,
                )
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
            is AgentConfigAction.ProviderModelChanged -> updateProviderModel(action.id, action.modelName)
            is AgentConfigAction.ProviderSelected -> selectProvider(action.id)
        }
    }

    private fun updateProviderModel(id: AgentProviderId, modelName: String) {
        viewModelScope.launch(dispatchers.io) {
            val provider = repository.providers.value.firstOrNull { it.id == id } ?: return@launch
            repository.updateProvider(provider.copy(modelName = modelName))
            logger.i { "Updated provider model: $id" }
        }
    }
}
