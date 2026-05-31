package com.softartdev.kvace.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.feature.settings.domain.AppSettings
import com.softartdev.kvace.feature.settings.domain.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: AppSettingsRepository,
    private val dispatchers: CoroutineDispatchers,
    private val logger: Logger,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var hasLoadedSettings = false

    fun loadSettings() {
        if (hasLoadedSettings) return
        hasLoadedSettings = true

        viewModelScope.launch(dispatchers.io) {
            val settings = repository.currentSettings()
            logger.i { "SettingsViewModel loaded settings" }
            _uiState.update { it.copy(selectedSection = settings.selectedSection) }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SelectSection -> viewModelScope.launch(dispatchers.io) {
                _uiState.update { it.copy(selectedSection = action.section) }
                repository.saveSettings(AppSettings(selectedSection = action.section))
                logger.i { "SettingsViewModel selected section: ${action.section}" }
            }
        }
    }
}
