package com.softartdev.kvace.app.di

import com.softartdev.kvace.app.share.WasmTextShareInteractor
import com.softartdev.kvace.core.data.settings.PersistentSettingsFactory
import com.softartdev.kvace.core.data.settings.WasmPersistentSettingsFactory
import com.softartdev.kvace.core.presentation.TextShareInteractor
import com.softartdev.kvace.feature.agent.data.DefaultOllamaEndpointProvider
import com.softartdev.kvace.feature.agent.data.DefaultUnavailableOnDeviceModelProvider
import com.softartdev.kvace.feature.agent.data.OnDeviceModelProvider
import com.softartdev.kvace.feature.agent.data.OllamaEndpointProvider
import com.softartdev.kvace.feature.chat.data.local.ChatDatabaseDriverFactory
import com.softartdev.kvace.feature.chat.data.local.WasmChatDatabaseDriverFactory
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal actual val kvacePlatformModule = module {
    singleOf(::WasmPersistentSettingsFactory) bind PersistentSettingsFactory::class
    singleOf(::DefaultOllamaEndpointProvider) bind OllamaEndpointProvider::class
    singleOf(::DefaultUnavailableOnDeviceModelProvider) bind OnDeviceModelProvider::class
    singleOf(::WasmChatDatabaseDriverFactory) bind ChatDatabaseDriverFactory::class
    singleOf(::WasmTextShareInteractor) bind TextShareInteractor::class
}
