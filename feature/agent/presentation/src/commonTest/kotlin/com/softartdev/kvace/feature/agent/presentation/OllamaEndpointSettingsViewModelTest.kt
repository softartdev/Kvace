package com.softartdev.kvace.feature.agent.presentation

import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTestResult
import com.softartdev.kvace.feature.agent.domain.AgentConnectionTester
import com.softartdev.kvace.feature.agent.domain.AgentModelCatalog
import com.softartdev.kvace.feature.agent.domain.AgentModelListResult
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.domain.OllamaEndpointValidationResult
import com.softartdev.kvace.feature.agent.domain.OllamaEndpointValidator
import com.softartdev.kvace.feature.agent.domain.ValidatedOllamaEndpoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        assertEquals(DEFAULT_MODEL, viewModel.uiState.value.modelInput)
    }

    @Test
    fun rejectsInvalidHostWithoutPersistenceOrNetworkCalls() = runTest(dispatcher) {
        val repository = FakeOllamaConfigurationRepository()
        val tester = FakeAgentConnectionTester()
        val modelCatalog = FakeAgentModelCatalog()
        val viewModel = createViewModel(repository, tester, modelCatalog)

        viewModel.onAction(OllamaEndpointSettingsAction.HostChanged("http://localhost"))
        viewModel.onAction(OllamaEndpointSettingsAction.PortChanged("11434"))
        viewModel.onAction(OllamaEndpointSettingsAction.TestConnection)

        assertEquals(OllamaConnectionStatus.InvalidHost, viewModel.uiState.value.connectionStatus)
        assertEquals(0, repository.updateCount)
        assertEquals(0, tester.callCount)
        assertEquals(0, modelCatalog.callCount)
    }

    @Test
    fun rejectsInvalidPortWithoutLoadingModels() = runTest(dispatcher) {
        val modelCatalog = FakeAgentModelCatalog()
        val viewModel = createViewModel(modelCatalog = modelCatalog)

        viewModel.onAction(OllamaEndpointSettingsAction.HostChanged("127.0.0.1"))
        viewModel.onAction(OllamaEndpointSettingsAction.PortChanged("70000"))
        viewModel.onAction(OllamaEndpointSettingsAction.LoadModels)

        assertEquals(OllamaConnectionStatus.InvalidPort, viewModel.uiState.value.connectionStatus)
        assertEquals(0, modelCatalog.callCount)
    }

    @Test
    fun successfulConnectionConfiguresCurrentModelOnlyAfterCatalogConfirmation() = runTest(dispatcher) {
        val repository = FakeOllamaConfigurationRepository()
        val tester = FakeAgentConnectionTester(AgentConnectionTestResult.Success)
        val modelCatalog = FakeAgentModelCatalog(AgentModelListResult.Success(listOf(DEFAULT_MODEL)))
        val viewModel = createViewModel(repository, tester, modelCatalog)

        viewModel.observeEndpoint()
        viewModel.onAction(OllamaEndpointSettingsAction.HostChanged("10.0.2.2"))
        viewModel.onAction(OllamaEndpointSettingsAction.PortChanged("11434"))
        viewModel.onAction(OllamaEndpointSettingsAction.TestConnection)

        val config = repository.ollamaProvider()
        assertEquals("http://10.0.2.2:11434", config.endpoint)
        assertTrue(config.isConfigured)
        assertEquals(OllamaConnectionStatus.Success, viewModel.uiState.value.connectionStatus)
        assertEquals(OllamaModelsStatus.Loaded, viewModel.uiState.value.modelsStatus)
        assertEquals(1, tester.callCount)
        assertEquals(1, modelCatalog.callCount)
    }

    @Test
    fun failedConnectionKeepsProviderUnconfiguredWithoutLoadingModels() = runTest(dispatcher) {
        val repository = FakeOllamaConfigurationRepository()
        val tester = FakeAgentConnectionTester(AgentConnectionTestResult.Failure("offline"))
        val modelCatalog = FakeAgentModelCatalog()
        val viewModel = createViewModel(repository, tester, modelCatalog)

        viewModel.observeEndpoint()
        viewModel.onAction(OllamaEndpointSettingsAction.TestConnection)

        assertFalse(repository.ollamaProvider().isConfigured)
        assertEquals(OllamaConnectionStatus.Failure("offline"), viewModel.uiState.value.connectionStatus)
        assertEquals(0, modelCatalog.callCount)
    }

    @Test
    fun missingSavedModelRequiresExplicitSelectionWithoutAutoSelectingFirst() = runTest(dispatcher) {
        val repository = FakeOllamaConfigurationRepository()
        val models = listOf("llama3.2:latest", "mistral:latest")
        val viewModel = createViewModel(
            repository = repository,
            modelCatalog = FakeAgentModelCatalog(AgentModelListResult.Success(models)),
        )

        viewModel.observeEndpoint()
        viewModel.onAction(OllamaEndpointSettingsAction.LoadModels)

        assertFalse(repository.ollamaProvider().isConfigured)
        assertEquals("", viewModel.uiState.value.modelInput)
        assertEquals(models, viewModel.uiState.value.availableModels)
        assertEquals(OllamaModelsStatus.SelectionRequired, viewModel.uiState.value.modelsStatus)
    }

    @Test
    fun selectedCatalogModelPersistsAndConfiguresProvider() = runTest(dispatcher) {
        val repository = FakeOllamaConfigurationRepository()
        val models = listOf("llama3.2:latest", "mistral:latest")
        val viewModel = createViewModel(
            repository = repository,
            modelCatalog = FakeAgentModelCatalog(AgentModelListResult.Success(models)),
        )
        viewModel.observeEndpoint()
        viewModel.onAction(OllamaEndpointSettingsAction.LoadModels)

        viewModel.onAction(OllamaEndpointSettingsAction.ModelSelected("mistral:latest"))

        assertEquals("mistral:latest", repository.ollamaProvider().modelName)
        assertTrue(repository.ollamaProvider().isConfigured)
        assertEquals("mistral:latest", viewModel.uiState.value.modelInput)
        assertEquals(OllamaModelsStatus.Loaded, viewModel.uiState.value.modelsStatus)
    }

    @Test
    fun arbitraryModelSelectionOutsideCatalogIsIgnored() = runTest(dispatcher) {
        val repository = FakeOllamaConfigurationRepository()
        val viewModel = createViewModel(
            repository = repository,
            modelCatalog = FakeAgentModelCatalog(
                AgentModelListResult.Success(listOf("mistral:latest")),
            ),
        )
        viewModel.observeEndpoint()
        viewModel.onAction(OllamaEndpointSettingsAction.LoadModels)
        val updateCountAfterLoad = repository.updateCount

        viewModel.onAction(OllamaEndpointSettingsAction.ModelSelected("invented:latest"))

        assertEquals(DEFAULT_MODEL, repository.ollamaProvider().modelName)
        assertFalse(repository.ollamaProvider().isConfigured)
        assertEquals(updateCountAfterLoad, repository.updateCount)
    }

    @Test
    fun emptyCatalogKeepsProviderUnconfigured() = runTest(dispatcher) {
        val repository = FakeOllamaConfigurationRepository()
        val viewModel = createViewModel(
            repository = repository,
            modelCatalog = FakeAgentModelCatalog(AgentModelListResult.Success(emptyList())),
        )

        viewModel.observeEndpoint()
        viewModel.onAction(OllamaEndpointSettingsAction.LoadModels)

        assertFalse(repository.ollamaProvider().isConfigured)
        assertEquals(OllamaModelsStatus.Empty, viewModel.uiState.value.modelsStatus)
    }

    @Test
    fun modelCatalogFailureKeepsProviderUnconfigured() = runTest(dispatcher) {
        val repository = FakeOllamaConfigurationRepository()
        val viewModel = createViewModel(
            repository = repository,
            modelCatalog = FakeAgentModelCatalog(AgentModelListResult.Failure("unavailable")),
        )

        viewModel.observeEndpoint()
        viewModel.onAction(OllamaEndpointSettingsAction.LoadModels)

        assertFalse(repository.ollamaProvider().isConfigured)
        assertEquals(OllamaModelsStatus.Failure("unavailable"), viewModel.uiState.value.modelsStatus)
    }

    @Test
    fun endpointEditClearsStaleCatalogAndSelection() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.observeEndpoint()
        viewModel.onAction(OllamaEndpointSettingsAction.LoadModels)

        viewModel.onAction(OllamaEndpointSettingsAction.HostChanged("other-host"))

        assertEquals(emptyList(), viewModel.uiState.value.availableModels)
        assertEquals("", viewModel.uiState.value.modelInput)
        assertEquals(OllamaConnectionStatus.Idle, viewModel.uiState.value.connectionStatus)
        assertEquals(OllamaModelsStatus.Idle, viewModel.uiState.value.modelsStatus)
    }

    private fun createViewModel(
        repository: FakeOllamaConfigurationRepository = FakeOllamaConfigurationRepository(),
        connectionTester: FakeAgentConnectionTester = FakeAgentConnectionTester(),
        modelCatalog: FakeAgentModelCatalog = FakeAgentModelCatalog(
            AgentModelListResult.Success(listOf(DEFAULT_MODEL)),
        ),
    ) = OllamaEndpointSettingsViewModel(
        repository = repository,
        connectionTester = connectionTester,
        modelCatalog = modelCatalog,
        endpointValidator = FakeOllamaEndpointValidator(),
        dispatchers = OllamaTestDispatchers(dispatcher),
    )

    private companion object {
        const val DEFAULT_MODEL = "qwen3.5:0.8b"
    }
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
    var updateCount = 0

    override suspend fun selectProvider(id: AgentProviderId) {
        selectedProvider.value = providers.value.firstOrNull { it.id == id }
    }

    override suspend fun updateProvider(config: AgentProviderConfig) {
        updateCount++
        providers.value = providers.value.map { if (it.id == config.id) config else it }
        if (selectedProvider.value?.id == config.id) selectedProvider.value = config
    }

    fun ollamaProvider(): AgentProviderConfig = providers.value.first()
}

private class FakeAgentConnectionTester(
    private val result: AgentConnectionTestResult = AgentConnectionTestResult.Success,
) : AgentConnectionTester {
    var callCount = 0

    override suspend fun testConnection(config: AgentProviderConfig): AgentConnectionTestResult {
        callCount++
        return result
    }

    override suspend fun testBrowserEndpoint(config: AgentProviderConfig): AgentConnectionTestResult = testConnection(config)
}

private class FakeAgentModelCatalog(
    private val result: AgentModelListResult = AgentModelListResult.Success(emptyList()),
) : AgentModelCatalog {
    var callCount = 0

    override suspend fun loadModels(config: AgentProviderConfig): AgentModelListResult {
        callCount++
        return result
    }
}

private class FakeOllamaEndpointValidator : OllamaEndpointValidator {
    override fun validate(hostInput: String, portInput: String): OllamaEndpointValidationResult {
        val host = hostInput.trim().removePrefix("[").removeSuffix("]")
        if (host.isBlank() || host.contains("://") || host.any(Char::isWhitespace)) {
            return OllamaEndpointValidationResult.InvalidHost
        }
        val port = portInput.trim().toIntOrNull()
        if (port == null || port !in 1..65535) return OllamaEndpointValidationResult.InvalidPort
        val urlHost = if (host.contains(':')) "[$host]" else host
        return OllamaEndpointValidationResult.Valid(
            ValidatedOllamaEndpoint(
                value = "http://$urlHost:$port",
                host = host,
                port = port,
            )
        )
    }

    override fun parse(endpoint: String): ValidatedOllamaEndpoint? {
        if (!endpoint.startsWith("http://")) return null
        val authority = endpoint.removePrefix("http://")
        val host: String
        val port: String
        if (authority.startsWith('[')) {
            val closingBracket = authority.indexOf(']')
            if (closingBracket < 0) return null
            host = authority.substring(1, closingBracket)
            port = authority.substring(closingBracket + 1).removePrefix(":")
        } else {
            host = authority.substringBeforeLast(':', missingDelimiterValue = "")
            port = authority.substringAfterLast(':', missingDelimiterValue = "")
        }
        return (validate(host, port) as? OllamaEndpointValidationResult.Valid)?.endpoint
    }
}

private class OllamaTestDispatchers(
    dispatcher: CoroutineDispatcher,
) : CoroutineDispatchers {
    override val default: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val unconfined: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
}
