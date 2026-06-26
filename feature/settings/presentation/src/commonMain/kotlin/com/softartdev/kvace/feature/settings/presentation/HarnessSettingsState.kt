package com.softartdev.kvace.feature.settings.presentation

data class HarnessSettingsUiState(
    val enabled: Boolean = true,
    val systemPrompt: String = "",
)

sealed interface HarnessSettingsAction {
    data class EnabledChanged(val enabled: Boolean) : HarnessSettingsAction
    data class SystemPromptChanged(val prompt: String) : HarnessSettingsAction
    data object ResetClicked : HarnessSettingsAction
}
