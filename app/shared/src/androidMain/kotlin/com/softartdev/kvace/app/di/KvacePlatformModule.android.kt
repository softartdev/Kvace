package com.softartdev.kvace.app.di

import com.softartdev.kvace.app.share.AndroidTextShareInteractor
import com.softartdev.kvace.core.data.settings.AndroidPersistentSettingsFactory
import com.softartdev.kvace.core.data.settings.PersistentSettingsFactory
import com.softartdev.kvace.core.presentation.TextShareInteractor
import com.softartdev.kvace.feature.agent.data.AndroidOllamaEndpointProvider
import com.softartdev.kvace.feature.agent.data.AndroidOnDeviceModelProvider
import com.softartdev.kvace.feature.agent.data.OnDeviceModelProvider
import com.softartdev.kvace.feature.agent.data.OllamaEndpointProvider
import com.softartdev.kvace.feature.agent.data.TermuxShellCommandExecutor
import com.softartdev.kvace.feature.agent.domain.ShellCommandExecutor
import com.softartdev.kvace.feature.chat.data.local.AndroidChatDatabaseDriverFactory
import com.softartdev.kvace.feature.chat.data.local.ChatDatabaseDriverFactory
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal actual val kvacePlatformModule = module {
    singleOf(::AndroidPersistentSettingsFactory) bind PersistentSettingsFactory::class
    singleOf(::AndroidOllamaEndpointProvider) bind OllamaEndpointProvider::class
    singleOf(::AndroidOnDeviceModelProvider) bind OnDeviceModelProvider::class
    singleOf(::TermuxShellCommandExecutor) bind ShellCommandExecutor::class
    singleOf(::AndroidChatDatabaseDriverFactory) bind ChatDatabaseDriverFactory::class
    singleOf(::AndroidTextShareInteractor) bind TextShareInteractor::class
}
