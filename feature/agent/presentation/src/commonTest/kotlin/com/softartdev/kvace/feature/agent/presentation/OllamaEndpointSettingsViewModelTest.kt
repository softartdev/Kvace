package com.softartdev.kvace.feature.agent.presentation

import co.touchlab.kermit.Logger
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTestResult
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTester
import com.softartdev.kvace.feature.agent.domain.AgentModelCatalog
import com.softartdev.kvace.feature.agent.domain.AgentModelListResult
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OllamaEndpointSettingsViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initializesEndpointInputFromRepository() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.observeEndpoint()

        assertEquals("127.0.0.1", viewModel.uiState.value.hostInput)
        assertEquals("11434", viewModel.uiState.value.portInput)
        assertEquals("qwen3.5:0.8b", viewModel.uiState.value.modelInput)
    }

    @Test
    fun rejectsBlankHost() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onAction(OllamaEndpointSettingsAction.HostChanged(" "))
        viewModel.onAction(OllamaEndpointSettingsAction.TestConnection)

        assertEquals(OllamaConnectionStatus.InvalidHost, viewModel.uiState.value.connectionStatus)
    }

    @Test
    fun rejectsInvalidPort() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onAction(OllamaEndpointSettingsAction.HostChanged("127.0.0.1"))
        viewModel.onAction(OllamaEndpointSettingsAction.PortChanged("70000"))
        viewModel.onAction(OllamaEndpointSettingsAction.TestConnection)

        assertEquals(OllamaConnectionStatus.InvalidPort, viewModel.uiState.value.connectionStatus)
    }

    @Test
    fun successfulConnectionPersistsConfiguredEndpoint() = runTest(dispatcher) {
        val repository = FakeOllamaConfigurationRepository()
        val tester = FakeAgentConnectionTester(AgentConnectionTestResult.Success)
        val viewModel = createViewModel(repository = repository, connectionTester = tester)

        viewModel.observeEndpoint()
        viewModel.onAction(OllamaEndpointSettingsAction.HostChanged("10.0.2.2"))
        viewModel.onAction(OllamaEndpointSettingsAction.PortChanged("11434"))
        viewModel.onAction(OllamaEndpointSettingsAction.TestConnection)

        val ollamaConfig = repository.providers.value.first { it.id == AgentProviderId.Ollama }
        assertEquals("http://10.0.2.2:11434", ollamaConfig.endpoint)
        assertTrue(ollamaConfig.isConfigured)
        assertEquals(OllamaConnectionStatus.Success, viewModel.uiState.value.connectionStatus)
    }

    @Test
    fun failedConnectionKeepsProviderUnconfigured() = runTest(dispatcher) {
        val repository = FakeOllamaConfigurationRepository()
        val tester = FakeAgentConnectionTester(AgentConnectionTestResult.Failure("offline"))
        val viewModel = createViewModel(repository = repository, connectionTester = tester)

        viewModel.observeEndpoint()
        viewModel.onAction(OllamaEndpointSettingsAction.TestConnection)

        val ollamaConfig = repository.providers.value.first { it.id == AgentProviderId.Ollama }
        assertEquals(false, ollamaConfig.isConfigured)
        assertEquals(OllamaConnectionStatus.Failure("offline"), viewModel.uiState.value.connectionStatus)
    }

    @Test
    fun modelInputPersistsProviderModel() = runTest(dispatcher) {
        val repository = FakeOllamaConfigurationRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.observeEndpoint()
        viewModel.onAction(OllamaEndpointSettingsAction.ModelChanged("llama3.2:latest"))

        val ollamaConfig = repository.providers.value.first { it.id == AgentProviderId.Ollama }
        assertEquals("llama3.2:latest", ollamaConfig.modelName)
        assertEquals("llama3.2:latest", viewModel.uiState.value.modelInput)
    }

    @Test
    fun loadModelsAutoSelectsFirstServerModelWhenCurrentModelIsMissing() = runTest(dispatcher) {
        val repository = FakeOllamaConfigurationRepository()
        val modelCatalog = FakeAgentModelCatalog(
            AgentModelListResult.Success(listOf("llama3.2:latest", "mistral:latest")),
        )
        val viewModel = createViewModel(
            repository = repository,
            modelCatalog = modelCatalog,
        )

        viewModel.observeEndpoint()
        viewModel.onAction(OllamaEndpointSettingsAction.LoadModels)

        val ollamaConfig = repository.providers.value.first { it.id == AgentProviderId.Ollama }
        assertEquals("llama3.2:latest", ollamaConfig.modelName)
        assertEquals("llama3.2:latest", viewModel.uiState.value.modelInput)
        assertEquals(listOf("llama3.2:latest", "mistral:latest"), viewModel.uiState.value.availableModels)
        assertEquals(OllamaModelsStatus.Loaded, viewModel.uiState.value.modelsStatus)
    }

    @Test
    fun selectedServerModelPersistsProviderModel() = runTest(dispatcher) {
        val repository = FakeOllamaConfigurationRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.observeEndpoint()
        viewModel.onAction(OllamaEndpointSettingsAction.ModelSelected("mistral:latest"))

        val ollamaConfig = repository.providers.value.first { it.id == AgentProviderId.Ollama }
        assertEquals("mistral:latest", ollamaConfig.modelName)
        assertEquals("mistral:latest", viewModel.uiState.value.modelInput)
    }

    private fun createViewModel(
        repository: FakeOllamaConfigurationRepository = FakeOllamaConfigurationRepository(),
        connectionTester: AgentConnectionTester = FakeAgentConnectionTester(AgentConnectionTestResult.Success),
        modelCatalog: AgentModelCatalog = FakeAgentModelCatalog(AgentModelListResult.Success(emptyList())),
    ) = OllamaEndpointSettingsViewModel(
        repository = repository,
        connectionTester = connectionTester,
        modelCatalog = modelCatalog,
        dispatchers = OllamaTestDispatchers(dispatcher),
        logger = Logger.withTag("OllamaEndpointSettingsViewModelTest"),
    )
}

private class FakeOllamaConfigurationRepository : AgentConfigurationRepository {
    override val providers = MutableStateFlow(
        listOf(
            AgentProviderConfig(
                id = AgentProviderId.Ollama,
                modelName = "qwen3.5:0.8b",
                endpoint = "http://127.0.0.1:11434",
                isConfigured = true,
            ),
        )
    )
    override val selectedProvider = MutableStateFlow<AgentProviderConfig?>(providers.value.first())

    override suspend fun selectProvider(id: AgentProviderId) {
        selectedProvider.value = providers.value.firstOrNull { it.id == id }
    }

    override suspend fun updateProvider(config: AgentProviderConfig) {
        providers.value = providers.value.map { if (it.id == config.id) config else it }
        if (selectedProvider.value?.id == config.id) {
            selectedProvider.value = config
        }
    }
}

private class FakeAgentConnectionTester(
    private val result: AgentConnectionTestResult,
) : AgentConnectionTester {
    override suspend fun testConnection(config: AgentProviderConfig): AgentConnectionTestResult = result
}

private class FakeAgentModelCatalog(
    private val result: AgentModelListResult,
) : AgentModelCatalog {
    override suspend fun loadModels(config: AgentProviderConfig): AgentModelListResult = result
}

private class OllamaTestDispatchers(
    private val dispatcher: CoroutineDispatcher,
) : CoroutineDispatchers {
    override val default: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val unconfined: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
}
