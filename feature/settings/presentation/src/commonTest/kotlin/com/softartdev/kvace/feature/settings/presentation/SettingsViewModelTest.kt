package com.softartdev.kvace.feature.settings.presentation

import co.touchlab.kermit.Logger
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.feature.settings.domain.AppSettings
import com.softartdev.kvace.feature.settings.domain.AppSettingsRepository
import com.softartdev.kvace.feature.settings.domain.SettingsSection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
class SettingsViewModelTest {
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
    fun loadSettingsUpdatesSelectedSection() = runTest(dispatcher) {
        val repository = FakeAppSettingsRepository(AppSettings(SettingsSection.Agents))
        val viewModel = createViewModel(repository)

        viewModel.loadSettings()

        assertEquals(SettingsSection.Agents, viewModel.uiState.value.selectedSection)
    }

    @Test
    fun selectSectionUpdatesStateAndRepository() = runTest(dispatcher) {
        val repository = FakeAppSettingsRepository()
        val viewModel = createViewModel(repository)

        viewModel.onAction(SettingsAction.SelectSection(SettingsSection.About))

        assertEquals(SettingsSection.About, viewModel.uiState.value.selectedSection)
        assertEquals(SettingsSection.About, repository.settings.selectedSection)
    }

    private fun createViewModel(repository: FakeAppSettingsRepository) = SettingsViewModel(
        repository = repository,
        dispatchers = SettingsTestDispatchers(dispatcher),
        logger = Logger.withTag("SettingsViewModelTest"),
    )
}

private class FakeAppSettingsRepository(
    var settings: AppSettings = AppSettings(),
) : AppSettingsRepository {
    override suspend fun currentSettings(): AppSettings = settings

    override suspend fun saveSettings(settings: AppSettings) {
        this.settings = settings
    }
}

private class SettingsTestDispatchers(
    private val dispatcher: CoroutineDispatcher,
) : CoroutineDispatchers {
    override val default: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val unconfined: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
}
