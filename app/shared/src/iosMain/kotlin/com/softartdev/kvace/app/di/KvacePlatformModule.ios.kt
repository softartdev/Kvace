package com.softartdev.kvace.app.di

import com.softartdev.kvace.core.data.settings.ApplePersistentSettingsFactory
import com.softartdev.kvace.core.data.settings.PersistentSettingsFactory
import com.softartdev.kvace.feature.agent.data.OnDeviceModelProvider
import com.softartdev.kvace.feature.agent.data.OllamaEndpointProvider
import com.softartdev.kvace.feature.agent.data.defaultOllamaEndpointProvider
import org.koin.dsl.module

internal actual val kvacePlatformModule = module {
    single<PersistentSettingsFactory> { ApplePersistentSettingsFactory() }
    single<OllamaEndpointProvider> { defaultOllamaEndpointProvider() }
    single<OnDeviceModelProvider> { AppleOnDeviceModelProvider() }
}
