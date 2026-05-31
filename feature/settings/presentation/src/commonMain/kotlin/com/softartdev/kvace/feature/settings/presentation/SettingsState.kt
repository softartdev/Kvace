package com.softartdev.kvace.feature.settings.presentation

import com.softartdev.kvace.feature.settings.domain.SettingsSection

data class SettingsUiState(
    val sections: List<SettingsSection> = SettingsSection.entries,
    val selectedSection: SettingsSection = SettingsSection.Appearance,
)

sealed interface SettingsAction {
    data class SelectSection(val section: SettingsSection) : SettingsAction
}
