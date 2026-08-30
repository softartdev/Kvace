package com.softartdev.kvace.feature.agent.presentation

import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTestResult
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTester
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialRepository
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialResult
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AgentConfigViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun observesProvidersAndSelectedProvider() = runTest(dispatcher) {
        val repository = FakeAgentConfigurationRepository()
        val viewModel = AgentConfigViewModel(
            repository = repository,
            credentialRepository = FakeCredentialRepository(),
            connectionTester = FakeConnectionTester(),
            dispatchers = TestCoroutineDispatchers(dispatcher),
        )
        viewModel.observeProviders()

        assertEquals(repository.providers.value, viewModel.uiState.value.providers)
        assertEquals(AgentProviderId.Ollama, viewModel.uiState.value.selectedProviderId)
        assertEquals("gpt-4o", viewModel.uiState.value.openAiModelInput)
    }

    @Test
    fun selectProviderDelegatesToRepository() = runTest(dispatcher) {
        val repository = FakeAgentConfigurationRepository()
        val viewModel = AgentConfigViewModel(
            repository = repository,
            credentialRepository = FakeCredentialRepository(),
            connectionTester = FakeConnectionTester(),
            dispatchers = TestCoroutineDispatchers(dispatcher),
        )
        viewModel.selectProvider(AgentProviderId.OnDevice)

        assertEquals(AgentProviderId.OnDevice, repository.selectedProvider.value?.id)
    }

    @Test
    fun openAiModelChangedUpdatesRepositoryWithTrimmedValue() = runTest(dispatcher) {
        val repository = FakeAgentConfigurationRepository()
        val viewModel = AgentConfigViewModel(
            repository = repository,
            credentialRepository = FakeCredentialRepository(),
            connectionTester = FakeConnectionTester(),
            dispatchers = TestCoroutineDispatchers(dispatcher),
        )

        viewModel.onAction(AgentConfigAction.OpenAiModelChanged("  gpt-4.1-mini  "))

        assertEquals(
            "gpt-4.1-mini",
            repository.providers.value.first { it.id == AgentProviderId.OpenAI }.modelName,
        )
        assertEquals("  gpt-4.1-mini  ", viewModel.uiState.value.openAiModelInput)
        assertEquals(null, viewModel.uiState.value.openAiModelValidationError)
    }

    @Test
    fun blankOpenAiModelRemainsDraftWithoutOverwritingStoredModel() = runTest(dispatcher) {
        val repository = FakeAgentConfigurationRepository()
        val viewModel = createViewModel(repository)
        viewModel.observeProviders()

        viewModel.onAction(AgentConfigAction.OpenAiModelChanged("   "))

        assertEquals("gpt-4o", repository.openAiProvider().modelName)
        assertEquals("   ", viewModel.uiState.value.openAiModelInput)
        assertEquals(OpenAiModelValidationError.Required, viewModel.uiState.value.openAiModelValidationError)
    }

    @Test
    fun latestOpenAiModelEditWinsWhenPreviousWriteIsStillPending() = runTest(dispatcher) {
        val repository = FakeAgentConfigurationRepository(delayedModel = "gpt-slow")
        val viewModel = createViewModel(repository)

        viewModel.onAction(AgentConfigAction.OpenAiModelChanged("gpt-slow"))
        viewModel.onAction(AgentConfigAction.OpenAiModelChanged("gpt-latest"))

        assertEquals("gpt-latest", repository.openAiProvider().modelName)
    }

    private fun createViewModel(repository: FakeAgentConfigurationRepository) = AgentConfigViewModel(
        repository = repository,
        credentialRepository = FakeCredentialRepository(),
        connectionTester = FakeConnectionTester(),
        dispatchers = TestCoroutineDispatchers(dispatcher),
    )
}

private class FakeCredentialRepository : ProviderCredentialRepository {
    override val openAiStatus = MutableStateFlow(ProviderCredentialStatus.Absent)
    override suspend fun readOpenAiApiKey(): String? = null
    override suspend fun saveOpenAiApiKey(apiKey: String): ProviderCredentialResult = ProviderCredentialResult.Success
    override suspend fun deleteOpenAiApiKey(): ProviderCredentialResult = ProviderCredentialResult.Success
    override suspend fun unlockOpenAiApiKey(masterPassword: String): ProviderCredentialResult = ProviderCredentialResult.Success
    override suspend fun clearLockedOpenAiApiKey(): ProviderCredentialResult = ProviderCredentialResult.Success
}

private class FakeConnectionTester : AgentConnectionTester {
    override suspend fun testConnection(config: AgentProviderConfig): AgentConnectionTestResult = AgentConnectionTestResult.Success
    override suspend fun testBrowserEndpoint(config: AgentProviderConfig): AgentConnectionTestResult = AgentConnectionTestResult.Success
}

private class FakeAgentConfigurationRepository(
    private val delayedModel: String? = null,
) : AgentConfigurationRepository {
    override val providers = MutableStateFlow(
        listOf(
            AgentProviderConfig(
                id = AgentProviderId.Ollama,
                modelName = "qwen3.5:0.8b",
                endpoint = "http://127.0.0.1:11434",
                isConfigured = true,
            ),
            AgentProviderConfig(
                id = AgentProviderId.OpenAI,
                modelName = "gpt-4o",
            ),
            AgentProviderConfig(
                id = AgentProviderId.OnDevice,
                modelName = "Gemini Nano",
                isConfigured = true,
            ),
        )
    )
    override val selectedProvider = MutableStateFlow<AgentProviderConfig?>(providers.value.first())

    override suspend fun selectProvider(id: AgentProviderId) {
        selectedProvider.value = providers.value.firstOrNull { it.id == id }
    }

    override suspend fun updateProvider(config: AgentProviderConfig) {
        if (config.modelName == delayedModel) delay(1_000)
        providers.value = providers.value.map { if (it.id == config.id) config else it }
        if (selectedProvider.value?.id == config.id) {
            selectedProvider.value = config
        }
    }

    fun openAiProvider(): AgentProviderConfig = providers.value.first { it.id == AgentProviderId.OpenAI }
}

private class TestCoroutineDispatchers(
    dispatcher: CoroutineDispatcher,
) : CoroutineDispatchers {
    override val default: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val unconfined: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
}
