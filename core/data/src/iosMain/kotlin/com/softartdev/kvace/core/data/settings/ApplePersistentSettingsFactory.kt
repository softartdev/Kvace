package com.softartdev.kvace.core.data.settings

import com.russhwolf.settings.NSUserDefaultsSettings

class ApplePersistentSettingsFactory : PersistentSettingsFactory {
    private val factory = NSUserDefaultsSettings.Factory()

    override fun create(name: String): PersistentSettings = PersistentSettings(factory.create(name))
}
