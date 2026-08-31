package com.softartdev.kvace.core.data.settings

import com.russhwolf.settings.Settings

open class PersistentSettings internal constructor(
    private val settings: Settings?,
    private val values: MutableMap<String, String>,
    private val keyPrefix: String,
) {
    constructor(settings: Settings, keyPrefix: String = "") : this(settings, mutableMapOf(), keyPrefix)
    constructor() : this(null, mutableMapOf(), "")

    private fun String.namespaced(): String = keyPrefix + this

    open fun getStringOrNull(key: String): String? = settings?.getStringOrNull(key.namespaced()) ?: values[key]
    open fun putString(key: String, value: String) {
        settings?.putString(key.namespaced(), value) ?: values.set(key, value)
    }
    open fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        settings?.getBoolean(key.namespaced(), defaultValue) ?: values[key]?.toBooleanStrictOrNull() ?: defaultValue
    open fun putBoolean(key: String, value: Boolean) {
        settings?.putBoolean(key.namespaced(), value) ?: values.set(key, value.toString())
    }
    open fun remove(key: String) {
        settings?.remove(key.namespaced()) ?: values.remove(key)
    }
}

interface PersistentSettingsFactory {
    fun create(name: String): PersistentSettings
}

class InMemoryPersistentSettingsFactory : PersistentSettingsFactory {
    private val settingsByName = mutableMapOf<String, PersistentSettings>()

    override fun create(name: String): PersistentSettings =
        settingsByName.getOrPut(name) { PersistentSettings() }
}
