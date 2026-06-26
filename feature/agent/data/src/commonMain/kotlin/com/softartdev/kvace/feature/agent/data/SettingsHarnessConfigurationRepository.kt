package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.core.data.settings.PersistentSettings
import com.softartdev.kvace.core.data.settings.PersistentSettingsFactory
import com.softartdev.kvace.feature.agent.domain.DEFAULT_HARNESS_SYSTEM_PROMPT
import com.softartdev.kvace.feature.agent.domain.HarnessConfig
import com.softartdev.kvace.feature.agent.domain.HarnessConfigurationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsHarnessConfigurationRepository(
    settingsFactory: PersistentSettingsFactory,
) : HarnessConfigurationRepository {
    private val settings: PersistentSettings = settingsFactory.create(SETTINGS_NAME)

    override val config: StateFlow<HarnessConfig>
        field = MutableStateFlow(loadConfig())

    override suspend fun updateConfig(config: HarnessConfig) {
        settings.putBoolean(KEY_ENABLED, config.enabled)
        settings.putString(KEY_SYSTEM_PROMPT, config.systemPrompt)
        this.config.value = config
    }

    override suspend fun resetConfig() {
        updateConfig(HarnessConfig())
    }

    private fun loadConfig() = HarnessConfig(
        enabled = settings.getBoolean(KEY_ENABLED, defaultValue = true),
        systemPrompt = settings.getStringOrNull(KEY_SYSTEM_PROMPT) ?: DEFAULT_HARNESS_SYSTEM_PROMPT,
    )

    private companion object {
        const val SETTINGS_NAME = "kvace_harness_configuration"
        const val KEY_ENABLED = "enabled"
        const val KEY_SYSTEM_PROMPT = "system_prompt"
    }
}
