package com.softartdev.kvace.core.data.settings

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings

class AndroidPersistentSettingsFactory(context: Context) : PersistentSettingsFactory {
    private val factory = SharedPreferencesSettings.Factory(context.applicationContext)

    override fun create(name: String): PersistentSettings = PersistentSettings(factory.create(name))
}
