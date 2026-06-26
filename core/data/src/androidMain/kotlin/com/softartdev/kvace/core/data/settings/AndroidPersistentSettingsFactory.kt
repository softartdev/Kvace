package com.softartdev.kvace.core.data.settings

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

class AndroidPersistentSettingsFactory(context: Context) : PersistentSettingsFactory {
    private val factory = SharedPreferencesSettings.Factory(context.applicationContext)

    override fun create(name: String): PersistentSettings = RusshwolfPersistentSettings(
        settings = factory.create(name)
    )
}

private class RusshwolfPersistentSettings(private val settings: Settings) : PersistentSettings {
    override fun getStringOrNull(key: String): String? = settings.getStringOrNull(key)
    override fun putString(key: String, value: String) = settings.putString(key, value)
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = settings.getBoolean(key, defaultValue)
    override fun putBoolean(key: String, value: Boolean) = settings.putBoolean(key, value)
}
