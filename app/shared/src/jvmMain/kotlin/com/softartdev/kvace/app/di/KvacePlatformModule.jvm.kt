package com.softartdev.kvace.app.di

import com.softartdev.kvace.core.data.settings.JvmPersistentSettingsFactory
import com.softartdev.kvace.core.data.settings.PersistentSettingsFactory
import com.softartdev.kvace.feature.agent.data.OnDeviceModelProvider
import com.softartdev.kvace.feature.agent.data.OllamaEndpointProvider
import com.softartdev.kvace.feature.agent.data.UnavailableOnDeviceModelProvider
import com.softartdev.kvace.feature.agent.data.defaultOllamaEndpointProvider
import org.koin.dsl.module

internal actual val kvacePlatformModule = module {
    single<PersistentSettingsFactory> { JvmPersistentSettingsFactory() }
    single<OllamaEndpointProvider> { defaultOllamaEndpointProvider() }
    single<OnDeviceModelProvider> { UnavailableOnDeviceModelProvider() }
}
