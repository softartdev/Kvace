package com.softartdev.kvace.feature.settings.domain

enum class SettingsSection {
    Appearance,
    Harness,
    Libraries,
    About,
}

data class AppSettings(
    val selectedSection: SettingsSection = SettingsSection.Appearance,
)

interface AppSettingsRepository {
    suspend fun currentSettings(): AppSettings
    suspend fun saveSettings(settings: AppSettings)
}
