package com.softartdev.kvace.feature.agent.presentation

import co.touchlab.kermit.Logger
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class AgentConfigViewModelTest {
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
    fun observesProvidersAndSelectedProvider() = runTest(dispatcher) {
        val repository = FakeAgentConfigurationRepository()
        val viewModel = AgentConfigViewModel(
            repository = repository,
            dispatchers = TestCoroutineDispatchers(dispatcher),
            logger = Logger.withTag("AgentConfigViewModelTest"),
        )

        viewModel.observeProviders()

        assertEquals(repository.providers.value, viewModel.uiState.value.providers)
        assertEquals(AgentProviderId.Ollama, viewModel.uiState.value.selectedProviderId)
    }

    @Test
    fun selectProviderDelegatesToRepository() = runTest(dispatcher) {
        val repository = FakeAgentConfigurationRepository()
        val viewModel = AgentConfigViewModel(
            repository = repository,
            dispatchers = TestCoroutineDispatchers(dispatcher),
            logger = Logger.withTag("AgentConfigViewModelTest"),
        )

        viewModel.onAction(AgentConfigAction.SelectProvider(AgentProviderId.OnDevice))

        assertEquals(AgentProviderId.OnDevice, repository.selectedProvider.value?.id)
    }
}

private class FakeAgentConfigurationRepository : AgentConfigurationRepository {
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
        providers.value = providers.value.map { if (it.id == config.id) config else it }
        if (selectedProvider.value?.id == config.id) {
            selectedProvider.value = config
        }
    }
}

private class TestCoroutineDispatchers(
    private val dispatcher: CoroutineDispatcher,
) : CoroutineDispatchers {
    override val default: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val unconfined: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
}
