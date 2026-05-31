package com.softartdev.kvace.feature.settings.data

import com.softartdev.kvace.core.data.settings.PersistentSettings
import com.softartdev.kvace.core.data.settings.PersistentSettingsFactory
import com.softartdev.kvace.feature.settings.domain.AppSettings
import com.softartdev.kvace.feature.settings.domain.AppSettingsRepository
import com.softartdev.kvace.feature.settings.domain.SettingsSection

class PersistentAppSettingsRepository(
    settingsFactory: PersistentSettingsFactory,
) : AppSettingsRepository {
    private val settings: PersistentSettings = settingsFactory.create(SETTINGS_NAME)

    override suspend fun currentSettings(): AppSettings {
        val selectedSection = settings.getStringOrNull(KEY_SELECTED_SECTION)
            ?.let { runCatching { SettingsSection.valueOf(it) }.getOrNull() }
            ?: SettingsSection.Appearance

        return AppSettings(selectedSection = selectedSection)
    }

    override suspend fun saveSettings(settings: AppSettings) {
        this.settings.putString(KEY_SELECTED_SECTION, settings.selectedSection.name)
    }

    private companion object {
        const val SETTINGS_NAME = "kvace_app_settings"
        const val KEY_SELECTED_SECTION = "selected_section"
    }
}
