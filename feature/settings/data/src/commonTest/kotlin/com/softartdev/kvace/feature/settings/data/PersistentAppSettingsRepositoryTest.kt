package com.softartdev.kvace.feature.settings.data

import com.softartdev.kvace.core.data.settings.InMemoryPersistentSettingsFactory
import com.softartdev.kvace.feature.settings.domain.AppSettings
import com.softartdev.kvace.feature.settings.domain.SettingsSection
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PersistentAppSettingsRepositoryTest {

    @Test
    fun savesAndRestoresSelectedSection() = runTest {
        val factory = InMemoryPersistentSettingsFactory()
        PersistentAppSettingsRepository(factory).saveSettings(
            AppSettings(SettingsSection.Harness),
        )
        assertEquals(
            SettingsSection.Harness,
            PersistentAppSettingsRepository(factory).currentSettings().selectedSection,
        )
    }
}
