package com.softartdev.kvace.feature.settings.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsModelsTest {

    @Test
    fun appSettingsDefaultsToAppearanceSection() {
        assertEquals(SettingsSection.Appearance, AppSettings().selectedSection)
    }

    @Test
    fun settingsSectionOrderIsStable() {
        assertEquals(
            listOf(
                SettingsSection.Appearance,
                SettingsSection.Harness,
                SettingsSection.Libraries,
                SettingsSection.About,
            ),
            SettingsSection.entries,
        )
    }
}
