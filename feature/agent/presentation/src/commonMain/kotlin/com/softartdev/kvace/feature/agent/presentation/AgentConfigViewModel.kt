package com.softartdev.kvace.feature.agent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AgentConfigViewModel(
    private val repository: AgentConfigurationRepository,
    private val dispatchers: CoroutineDispatchers,
    private val logger: Logger,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgentConfigUiState())
    val uiState: StateFlow<AgentConfigUiState> = _uiState.asStateFlow()

    private var isObservingProviders = false

    fun observeProviders() {
        if (isObservingProviders) return
        isObservingProviders = true

        combine(repository.providers, repository.selectedProvider) { providers, selected ->
            providers to selected
        }.onEach { (providers, selected) ->
            _uiState.update { state ->
                state.copy(
                    providers = providers,
                    selectedProviderId = selected?.id,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: AgentConfigAction) {
        when (action) {
            is AgentConfigAction.SelectProvider -> viewModelScope.launch(dispatchers.io) {
                repository.selectProvider(action.id)
                logger.i { "AgentConfigViewModel selected provider: ${action.id}" }
            }
        }
    }
}
