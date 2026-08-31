package com.softartdev.kvace.core.data.settings

interface PersistentSettings {
    fun getStringOrNull(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
}

interface PersistentSettingsFactory {
    fun create(name: String): PersistentSettings
}

class InMemoryPersistentSettingsFactory : PersistentSettingsFactory {
    private val settingsByName = mutableMapOf<String, InMemoryPersistentSettings>()

    override fun create(name: String): PersistentSettings =
        settingsByName.getOrPut(name) { InMemoryPersistentSettings() }
}

private class InMemoryPersistentSettings : PersistentSettings {
    private val values = mutableMapOf<String, String>()

    override fun getStringOrNull(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        values[key]?.toBooleanStrictOrNull() ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value.toString()
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
