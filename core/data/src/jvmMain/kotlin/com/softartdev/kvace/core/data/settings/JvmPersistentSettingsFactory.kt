package com.softartdev.kvace.core.data.settings

import com.russhwolf.settings.PreferencesSettings

class JvmPersistentSettingsFactory : PersistentSettingsFactory {
    private val factory = PreferencesSettings.Factory()

    override fun create(name: String): PersistentSettings = PersistentSettings(factory.create(name))
}
