package com.softartdev.kvace.app.di

import com.softartdev.kvace.app.navigation.ComposeRouter
import com.softartdev.kvace.app.snackbar.ComposeSnackbarInteractor
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.core.domain.util.CoroutineDispatchersImpl
import com.softartdev.kvace.core.presentation.Router
import com.softartdev.kvace.core.presentation.SnackbarInteractor
import com.softartdev.kvace.feature.agent.data.KoogAgentRuntime
import com.softartdev.kvace.feature.agent.data.KtorAgentConnectionTester
import com.softartdev.kvace.feature.agent.data.KtorAgentModelCatalog
import com.softartdev.kvace.feature.agent.data.KtorOllamaEndpointValidator
import com.softartdev.kvace.feature.agent.data.SettingsAgentConfigurationRepository
import com.softartdev.kvace.feature.agent.data.SettingsHarnessConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTester
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentModelCatalog
import com.softartdev.kvace.feature.agent.domain.AgentRuntime
import com.softartdev.kvace.feature.agent.domain.HarnessConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.OllamaEndpointValidator
import com.softartdev.kvace.feature.agent.presentation.AgentConfigViewModel
import com.softartdev.kvace.feature.agent.presentation.OllamaEndpointSettingsViewModel
import com.softartdev.kvace.feature.chat.data.ChatLocalDataSource
import com.softartdev.kvace.feature.chat.data.PersistentChatRepository
import com.softartdev.kvace.feature.chat.data.SqlDelightChatLocalDataSource
import com.softartdev.kvace.feature.chat.domain.ChatRepository
import com.softartdev.kvace.feature.chat.domain.MessageSender
import com.softartdev.kvace.feature.chat.domain.SendMessageUseCase
import com.softartdev.kvace.feature.chat.presentation.ChatViewModel
import com.softartdev.kvace.feature.settings.data.PersistentAppSettingsRepository
import com.softartdev.kvace.feature.settings.domain.AppSettingsRepository
import com.softartdev.kvace.feature.settings.presentation.HarnessSettingsViewModel
import com.softartdev.kvace.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val kvaceModule = module {
    includes(kvacePlatformModule)

    singleOf(::CoroutineDispatchersImpl) bind CoroutineDispatchers::class
    singleOf(::ComposeRouter) bind Router::class
    singleOf(::ComposeSnackbarInteractor) bind SnackbarInteractor::class

    singleOf(::SettingsAgentConfigurationRepository) bind AgentConfigurationRepository::class
    singleOf(::SettingsHarnessConfigurationRepository) bind HarnessConfigurationRepository::class
    singleOf(::KtorAgentConnectionTester) bind AgentConnectionTester::class
    singleOf(::KtorAgentModelCatalog) bind AgentModelCatalog::class
    singleOf(::KtorOllamaEndpointValidator) bind OllamaEndpointValidator::class
    singleOf(::KoogAgentRuntime) bind AgentRuntime::class
    viewModelOf(::AgentConfigViewModel)
    viewModelOf(::HarnessSettingsViewModel)
    viewModelOf(::OllamaEndpointSettingsViewModel)

    singleOf(::SqlDelightChatLocalDataSource) bind ChatLocalDataSource::class
    singleOf(::PersistentChatRepository) bind ChatRepository::class
    factoryOf(::SendMessageUseCase) bind MessageSender::class
    viewModelOf(::ChatViewModel)

    singleOf(::PersistentAppSettingsRepository) bind AppSettingsRepository::class
    viewModelOf(::SettingsViewModel)
}
