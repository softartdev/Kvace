package com.softartdev.kvace.core.data.settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings

class WasmPersistentSettingsFactory : PersistentSettingsFactory {
    private val storageSettings = StorageSettings()

    override fun create(name: String): PersistentSettings =
        NamespacedPersistentSettings(
            name = name,
            settings = storageSettings,
        )
}

private class NamespacedPersistentSettings(
    name: String,
    private val settings: Settings,
) : PersistentSettings {
    private val keyPrefix = "$name."

    override fun getStringOrNull(key: String): String? =
        settings.getStringOrNull(key.namespaced())

    override fun putString(key: String, value: String) {
        settings.putString(key.namespaced(), value)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        settings.getBoolean(key.namespaced(), defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        settings.putBoolean(key.namespaced(), value)
    }

    override fun remove(key: String) {
        settings.remove(key.namespaced())
    }

    private fun String.namespaced(): String = keyPrefix + this
}
