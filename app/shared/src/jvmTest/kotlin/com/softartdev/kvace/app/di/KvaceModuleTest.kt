package com.softartdev.kvace.app.di

import com.softartdev.kvace.core.data.settings.PersistentSettingsFactory
import com.softartdev.kvace.core.presentation.Router
import com.softartdev.kvace.core.presentation.SnackbarInteractor
import com.softartdev.kvace.core.presentation.TextShareInteractor
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTester
import com.softartdev.kvace.feature.agent.domain.AgentModelCatalog
import com.softartdev.kvace.feature.agent.domain.AgentRuntime
import com.softartdev.kvace.feature.agent.domain.HarnessConfigurationRepository
import com.softartdev.kvace.feature.agent.presentation.AgentConfigViewModel
import com.softartdev.kvace.feature.agent.presentation.OllamaEndpointSettingsViewModel
import com.softartdev.kvace.feature.chat.data.ChatLocalDataSource
import com.softartdev.kvace.feature.chat.data.local.ChatDatabaseDriverFactory
import com.softartdev.kvace.feature.chat.domain.ChatRepository
import com.softartdev.kvace.feature.chat.domain.MessageSender
import com.softartdev.kvace.feature.chat.presentation.ChatViewModel
import com.softartdev.kvace.feature.settings.domain.AppSettingsRepository
import com.softartdev.kvace.feature.settings.presentation.HarnessSettingsViewModel
import com.softartdev.kvace.feature.settings.presentation.SettingsViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.context.stopKoin
import org.koin.core.context.startKoin
import org.koin.test.verify.verify
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class KvaceModuleTest {

    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    @OptIn(KoinExperimentalAPI::class)
    fun graphPassesStaticVerification() {
        kvaceModule.verify()
    }

    @Test
    fun graphResolvesCoreInfrastructure() {
        val koin = startKoin { modules(kvaceModule) }.koin

        assertNotNull(koin.get<Router>())
        assertNotNull(koin.get<SnackbarInteractor>())
        assertNotNull(koin.get<TextShareInteractor>())
        assertNotNull(koin.get<PersistentSettingsFactory>())
        assertNotNull(koin.get<ChatDatabaseDriverFactory>())
        assertNotNull(koin.get<AgentConfigurationRepository>())
        assertNotNull(koin.get<HarnessConfigurationRepository>())
        assertNotNull(koin.get<AgentConnectionTester>())
        assertNotNull(koin.get<AgentModelCatalog>())
        assertNotNull(koin.get<AgentRuntime>())
        assertNotNull(koin.get<ChatLocalDataSource>())
        assertNotNull(koin.get<ChatRepository>())
        assertNotNull(koin.get<MessageSender>())
        assertNotNull(koin.get<AppSettingsRepository>())
        assertNotNull(koin.get<AgentConfigViewModel>())
        assertNotNull(koin.get<OllamaEndpointSettingsViewModel>())
        assertNotNull(koin.get<HarnessSettingsViewModel>())
        assertNotNull(koin.get<ChatViewModel>())
        assertNotNull(koin.get<SettingsViewModel>())
    }
}
