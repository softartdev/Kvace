package com.softartdev.kvace.app.di

import com.softartdev.kvace.app.share.IosTextShareInteractor
import com.softartdev.kvace.core.data.settings.ApplePersistentSettingsFactory
import com.softartdev.kvace.core.data.settings.PersistentSettingsFactory
import com.softartdev.kvace.core.presentation.TextShareInteractor
import com.softartdev.kvace.feature.agent.data.DefaultOllamaEndpointProvider
import com.softartdev.kvace.feature.agent.data.OnDeviceModelProvider
import com.softartdev.kvace.feature.agent.data.OllamaEndpointProvider
import com.softartdev.kvace.feature.agent.data.com.softartdev.kvace.feature.agent.data.IosShellCommandExecutor
import com.softartdev.kvace.feature.agent.domain.ShellCommandExecutor
import com.softartdev.kvace.feature.chat.data.local.ChatDatabaseDriverFactory
import com.softartdev.kvace.feature.chat.data.local.IosChatDatabaseDriverFactory
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal actual val kvacePlatformModule = module {
    singleOf(::ApplePersistentSettingsFactory) bind PersistentSettingsFactory::class
    singleOf(::DefaultOllamaEndpointProvider) bind OllamaEndpointProvider::class
    singleOf(::AppleOnDeviceModelProvider) bind OnDeviceModelProvider::class
    singleOf(::IosShellCommandExecutor) bind ShellCommandExecutor::class
    singleOf(::IosChatDatabaseDriverFactory) bind ChatDatabaseDriverFactory::class
    singleOf(::IosTextShareInteractor) bind TextShareInteractor::class
}
