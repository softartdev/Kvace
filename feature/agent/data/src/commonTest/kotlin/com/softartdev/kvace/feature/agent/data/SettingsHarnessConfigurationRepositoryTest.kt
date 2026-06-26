package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.core.data.settings.InMemoryPersistentSettingsFactory
import com.softartdev.kvace.feature.agent.domain.HarnessConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsHarnessConfigurationRepositoryTest {

    @Test
    fun savesAndRestoresHarnessConfig() = runTest {
        val factory = InMemoryPersistentSettingsFactory()
        SettingsHarnessConfigurationRepository(factory).updateConfig(
            HarnessConfig(enabled = false, systemPrompt = "Custom prompt")
        )

        assertEquals(
            HarnessConfig(enabled = false, systemPrompt = "Custom prompt"),
            SettingsHarnessConfigurationRepository(factory).config.value,
        )
    }

    @Test
    fun resetConfigRestoresDefaultConfig() = runTest {
        val repository = SettingsHarnessConfigurationRepository(InMemoryPersistentSettingsFactory())
        repository.updateConfig(HarnessConfig(enabled = false, systemPrompt = "Custom prompt"))

        repository.resetConfig()

        assertEquals(HarnessConfig(), repository.config.value)
    }
}
