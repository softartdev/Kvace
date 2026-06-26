package com.softartdev.kvace.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.feature.agent.domain.HarnessConfig
import com.softartdev.kvace.feature.agent.domain.HarnessConfigurationRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HarnessSettingsViewModel(
    private val repository: HarnessConfigurationRepository,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {
    private val logger = Logger.withTag("HarnessSettingsViewModel")

    val uiState: StateFlow<HarnessSettingsUiState>
        field: MutableStateFlow<HarnessSettingsUiState> = MutableStateFlow(HarnessSettingsUiState())

    private var isObservingConfig = false

    fun observeConfig() {
        if (isObservingConfig) return
        isObservingConfig = true

        repository.config.onEach { config ->
            uiState.update {
                it.copy(
                    enabled = config.enabled,
                    systemPrompt = config.systemPrompt,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: HarnessSettingsAction) {
        when (action) {
            is HarnessSettingsAction.EnabledChanged -> updateConfig(
                uiState.value.copy(enabled = action.enabled)
            )
            HarnessSettingsAction.ResetClicked -> resetConfig()
            is HarnessSettingsAction.SystemPromptChanged -> updateConfig(
                uiState.value.copy(systemPrompt = action.prompt)
            )
        }
    }

    private fun updateConfig(state: HarnessSettingsUiState) {
        uiState.value = state
        viewModelScope.launch(dispatchers.io) {
            try {
                repository.updateConfig(HarnessConfig(enabled = state.enabled, systemPrompt = state.systemPrompt))
            } catch (error: Throwable) {
                currentCoroutineContext().ensureActive()
                logger.e(error) { "Failed to update harness config" }
            }
        }
    }

    private fun resetConfig() {
        viewModelScope.launch(dispatchers.io) {
            try {
                repository.resetConfig()
            } catch (error: Throwable) {
                currentCoroutineContext().ensureActive()
                logger.e(error) { "Failed to reset harness config" }
            }
        }
    }
}
