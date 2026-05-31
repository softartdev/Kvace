package com.softartdev.kvace.app.di

import co.touchlab.kermit.Logger
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.core.domain.util.CoroutineDispatchersImpl
import com.softartdev.kvace.feature.agent.data.KoogAgentRuntime
import com.softartdev.kvace.feature.agent.data.KtorAgentConnectionTester
import com.softartdev.kvace.feature.agent.data.KtorAgentModelCatalog
import com.softartdev.kvace.feature.agent.data.SettingsAgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTester
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentModelCatalog
import com.softartdev.kvace.feature.agent.domain.AgentRuntime
import com.softartdev.kvace.feature.agent.presentation.AgentConfigViewModel
import com.softartdev.kvace.feature.agent.presentation.OllamaEndpointSettingsViewModel
import com.softartdev.kvace.feature.chat.data.InMemoryChatRepository
import com.softartdev.kvace.feature.chat.domain.ChatRepository
import com.softartdev.kvace.feature.chat.domain.MessageSender
import com.softartdev.kvace.feature.chat.domain.SendMessageUseCase
import com.softartdev.kvace.feature.chat.presentation.ChatViewModel
import com.softartdev.kvace.feature.settings.data.PersistentAppSettingsRepository
import com.softartdev.kvace.feature.settings.domain.AppSettingsRepository
import com.softartdev.kvace.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val kvaceModule = module {
    includes(kvacePlatformModule)

    singleOf(::CoroutineDispatchersImpl) bind CoroutineDispatchers::class
    single { Logger.withTag("Kvace") }

    singleOf(::SettingsAgentConfigurationRepository) bind AgentConfigurationRepository::class
    singleOf(::KtorAgentConnectionTester) bind AgentConnectionTester::class
    singleOf(::KtorAgentModelCatalog) bind AgentModelCatalog::class
    singleOf(::KoogAgentRuntime) bind AgentRuntime::class
    viewModelOf(::AgentConfigViewModel)
    viewModelOf(::OllamaEndpointSettingsViewModel)

    singleOf(::InMemoryChatRepository) bind ChatRepository::class
    factoryOf(::SendMessageUseCase) bind MessageSender::class
    viewModelOf(::ChatViewModel)

    singleOf(::PersistentAppSettingsRepository) bind AppSettingsRepository::class
    viewModelOf(::SettingsViewModel)
}
