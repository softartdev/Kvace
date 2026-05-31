package com.softartdev.kvace.app.di

import android.content.Context
import com.softartdev.kvace.core.data.settings.AndroidPersistentSettingsFactory
import com.softartdev.kvace.core.data.settings.PersistentSettingsFactory
import com.softartdev.kvace.feature.agent.data.AndroidOnDeviceModelProvider
import com.softartdev.kvace.feature.agent.data.OnDeviceModelProvider
import com.softartdev.kvace.feature.agent.data.OllamaEndpointProvider
import com.softartdev.kvace.feature.agent.data.androidOllamaEndpointProvider
import org.koin.dsl.module

internal actual val kvacePlatformModule = module {
    single<PersistentSettingsFactory> { AndroidPersistentSettingsFactory(get<Context>()) }
    single<OllamaEndpointProvider> { androidOllamaEndpointProvider(get<Context>()) }
    single<OnDeviceModelProvider> { AndroidOnDeviceModelProvider() }
}
