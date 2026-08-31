package com.softartdev.kvace.core.data.settings

import com.russhwolf.settings.StorageSettings

class WasmPersistentSettingsFactory : PersistentSettingsFactory {
    private val storageSettings = StorageSettings()

    override fun create(name: String): PersistentSettings = PersistentSettings(storageSettings, "$name.")
}
