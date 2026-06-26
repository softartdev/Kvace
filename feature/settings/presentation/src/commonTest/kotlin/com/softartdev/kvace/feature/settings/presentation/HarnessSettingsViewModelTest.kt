package com.softartdev.kvace.feature.settings.presentation

import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.feature.agent.domain.HarnessConfig
import com.softartdev.kvace.feature.agent.domain.HarnessConfigurationRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class HarnessSettingsViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun observesHarnessConfig() = runTest(dispatcher) {
        val repository = FakeHarnessConfigurationRepository(
            HarnessConfig(enabled = false, systemPrompt = "Custom")
        )
        val viewModel = createViewModel(repository)

        viewModel.observeConfig()

        assertFalse(viewModel.uiState.value.enabled)
        assertEquals("Custom", viewModel.uiState.value.systemPrompt)
    }

    @Test
    fun enabledChangedPersistsConfig() = runTest(dispatcher) {
        val repository = FakeHarnessConfigurationRepository()
        val viewModel = createViewModel(repository)
        viewModel.observeConfig()

        viewModel.onAction(HarnessSettingsAction.EnabledChanged(false))

        assertFalse(repository.config.value.enabled)
    }

    @Test
    fun systemPromptChangedPersistsConfig() = runTest(dispatcher) {
        val repository = FakeHarnessConfigurationRepository()
        val viewModel = createViewModel(repository)
        viewModel.observeConfig()

        viewModel.onAction(HarnessSettingsAction.SystemPromptChanged("New prompt"))

        assertEquals("New prompt", repository.config.value.systemPrompt)
    }

    @Test
    fun resetClickedRestoresDefaultConfig() = runTest(dispatcher) {
        val repository = FakeHarnessConfigurationRepository(
            HarnessConfig(enabled = false, systemPrompt = "Custom")
        )
        val viewModel = createViewModel(repository)
        viewModel.observeConfig()

        viewModel.onAction(HarnessSettingsAction.ResetClicked)

        assertEquals(HarnessConfig(), repository.config.value)
        assertEquals(HarnessConfig().systemPrompt, viewModel.uiState.value.systemPrompt)
    }

    private fun createViewModel(repository: FakeHarnessConfigurationRepository) = HarnessSettingsViewModel(
        repository = repository,
        dispatchers = HarnessTestCoroutineDispatchers(dispatcher),
    )
}

private class FakeHarnessConfigurationRepository(
    initialConfig: HarnessConfig = HarnessConfig(),
) : HarnessConfigurationRepository {
    override val config = MutableStateFlow(initialConfig)

    override suspend fun updateConfig(config: HarnessConfig) {
        this.config.value = config
    }

    override suspend fun resetConfig() {
        config.value = HarnessConfig()
    }
}

private class HarnessTestCoroutineDispatchers(
    dispatcher: CoroutineDispatcher,
) : CoroutineDispatchers {
    override val default: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val unconfined: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
}
