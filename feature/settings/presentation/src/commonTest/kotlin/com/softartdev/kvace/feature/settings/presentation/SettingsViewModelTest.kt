package com.softartdev.kvace.feature.settings.presentation

import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.core.presentation.AppRoute
import com.softartdev.kvace.core.presentation.Router
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
        val repository = FakeAppSettingsRepository(AppSettings(SettingsSection.Harness))
        val viewModel = createViewModel(repository)

        viewModel.loadSettings()

        assertEquals(SettingsSection.Harness, viewModel.uiState.value.selectedSection)
    }

    @Test
    fun selectSectionUpdatesStateAndRepository() = runTest(dispatcher) {
        val repository = FakeAppSettingsRepository()
        val viewModel = createViewModel(repository)

        viewModel.selectSection(SettingsSection.About)

        assertEquals(SettingsSection.About, viewModel.uiState.value.selectedSection)
        assertEquals(SettingsSection.About, repository.settings.selectedSection)
    }

    @Test
    fun selectLibrariesSectionUpdatesStateAndRepository() = runTest(dispatcher) {
        val repository = FakeAppSettingsRepository()
        val viewModel = createViewModel(repository)

        viewModel.selectSection(SettingsSection.Libraries)

        assertEquals(SettingsSection.Libraries, viewModel.uiState.value.selectedSection)
        assertEquals(SettingsSection.Libraries, repository.settings.selectedSection)
    }

    @Test
    fun openThemePickerNavigatesToThemeDialog() = runTest(dispatcher) {
        val router = FakeRouter()
        val viewModel = createViewModel(router = router)

        viewModel.openThemePicker()

        assertEquals(1, router.themeDialogNavigationCount)
    }

    private fun createViewModel(
        repository: FakeAppSettingsRepository = FakeAppSettingsRepository(),
        router: FakeRouter = FakeRouter(),
    ) = SettingsViewModel(
        repository = repository,
        router = router,
        dispatchers = SettingsTestDispatchers(dispatcher),
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

private class FakeRouter : Router {
    var themeDialogNavigationCount = 0
        private set

    override fun <T : Any> navigate(route: T) {
        if (route === AppRoute.ThemeDialog) {
            themeDialogNavigationCount++
        }
    }

    override fun <T : Any> navigateSingleTop(route: T) = Unit

    override fun <T : Any> navigateTopLevel(route: T) = Unit

    override fun <T : Any> navigateClearingBackStack(route: T) = Unit

    override fun popBackStack(): Boolean = false
}

private class SettingsTestDispatchers(
    dispatcher: CoroutineDispatcher,
) : CoroutineDispatchers {
    override val default: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val unconfined: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
}
