package com.softartdev.kvace.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.core.presentation.AppRoute
import com.softartdev.kvace.core.presentation.Router
import com.softartdev.kvace.feature.settings.domain.AppSettings
import com.softartdev.kvace.feature.settings.domain.AppSettingsRepository
import com.softartdev.kvace.feature.settings.domain.SettingsSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: AppSettingsRepository,
    private val router: Router,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {
    private val logger = Logger.withTag("SettingsViewModel")

    val uiState: StateFlow<SettingsUiState>
        field: MutableStateFlow<SettingsUiState> = MutableStateFlow(SettingsUiState())

    private var hasLoadedSettings = false

    fun loadSettings() {
        if (hasLoadedSettings) return
        hasLoadedSettings = true

        viewModelScope.launch(dispatchers.io) {
            val settings = repository.currentSettings()
            logger.i { "Loaded settings" }
            uiState.update { it.copy(selectedSection = settings.selectedSection) }
        }
    }

    fun selectSection(section: SettingsSection) {
        viewModelScope.launch(dispatchers.io) {
            uiState.update { it.copy(selectedSection = section) }
            repository.saveSettings(AppSettings(selectedSection = section))
            logger.i { "Selected section: $section" }
        }
    }

    fun openThemePicker() {
        router.navigate(AppRoute.ThemeDialog)
    }
}
