package com.softartdev.kvace.app.di

import com.softartdev.kvace.app.share.JvmTextShareInteractor
import com.softartdev.kvace.core.data.settings.JvmPersistentSettingsFactory
import com.softartdev.kvace.core.data.settings.PersistentSettingsFactory
import com.softartdev.kvace.core.presentation.TextShareInteractor
import com.softartdev.kvace.feature.agent.data.DefaultOllamaEndpointProvider
import com.softartdev.kvace.feature.agent.data.DefaultUnavailableOnDeviceModelProvider
import com.softartdev.kvace.feature.agent.data.OnDeviceModelProvider
import com.softartdev.kvace.feature.agent.data.OllamaEndpointProvider
import com.softartdev.kvace.feature.chat.data.local.ChatDatabaseDriverFactory
import com.softartdev.kvace.feature.chat.data.local.JvmChatDatabaseDriverFactory
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal actual val kvacePlatformModule = module {
    singleOf(::JvmPersistentSettingsFactory) bind PersistentSettingsFactory::class
    singleOf(::DefaultOllamaEndpointProvider) bind OllamaEndpointProvider::class
    singleOf(::DefaultUnavailableOnDeviceModelProvider) bind OnDeviceModelProvider::class
    singleOf(::JvmChatDatabaseDriverFactory) bind ChatDatabaseDriverFactory::class
    singleOf(::JvmTextShareInteractor) bind TextShareInteractor::class
}
