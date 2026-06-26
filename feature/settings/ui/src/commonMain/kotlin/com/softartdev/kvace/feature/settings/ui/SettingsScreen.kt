@file:OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)

package com.softartdev.kvace.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.softartdev.kvace.core.ui.KvaceVerticalPaneExpansionDragHandle
import com.softartdev.kvace.core.ui.resources.Res
import com.softartdev.kvace.core.ui.resources.ic_arrow_back
import com.softartdev.kvace.core.ui.resources.ic_code
import com.softartdev.kvace.core.ui.resources.ic_info
import com.softartdev.kvace.core.ui.resources.ic_palette
import com.softartdev.kvace.core.ui.resources.ic_science
import com.softartdev.kvace.core.ui.resources.settings_about_message
import com.softartdev.kvace.core.ui.resources.settings_section_about
import com.softartdev.kvace.core.ui.resources.settings_section_appearance
import com.softartdev.kvace.core.ui.resources.settings_section_harness
import com.softartdev.kvace.core.ui.resources.settings_section_libraries
import com.softartdev.kvace.core.ui.resources.settings_source_code
import com.softartdev.kvace.core.ui.resources.settings_source_code_message
import com.softartdev.kvace.core.ui.resources.settings_title
import com.softartdev.kvace.feature.settings.domain.SettingsSection
import com.softartdev.kvace.feature.settings.presentation.HarnessSettingsAction
import com.softartdev.kvace.feature.settings.presentation.HarnessSettingsUiState
import com.softartdev.kvace.feature.settings.presentation.HarnessSettingsViewModel
import com.softartdev.kvace.feature.settings.presentation.SettingsUiState
import com.softartdev.kvace.feature.settings.presentation.SettingsViewModel
import com.softartdev.theme.material3.PreferableMaterialTheme
import com.softartdev.theme.material3.ThemePreferenceItem
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
    harnessSettingsViewModel: HarnessSettingsViewModel,
) {
    LaunchedEffect(settingsViewModel) { settingsViewModel.loadSettings() }
    LaunchedEffect(harnessSettingsViewModel) { harnessSettingsViewModel.observeConfig() }
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val harnessSettingsUiState by harnessSettingsViewModel.uiState.collectAsState()
    SettingsScreen(
        modifier = modifier,
        state = settingsUiState,
        harnessSettingsState = harnessSettingsUiState,
        onSectionSelected = settingsViewModel::selectSection,
        onThemeClick = settingsViewModel::openThemePicker,
        onHarnessAction = harnessSettingsViewModel::onAction,
    )
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    state: SettingsUiState,
    harnessSettingsState: HarnessSettingsUiState = HarnessSettingsUiState(),
    onSectionSelected: (SettingsSection) -> Unit,
    onThemeClick: () -> Unit,
    onHarnessAction: (HarnessSettingsAction) -> Unit = {},
) {
    val navigator: ThreePaneScaffoldNavigator<SettingsSection> =
        rememberListDetailPaneScaffoldNavigator<SettingsSection>()
    val paneExpansionState: PaneExpansionState = rememberPaneExpansionState()
    val coroutineScope = rememberCoroutineScope()
    val canNavigateBack = navigator.canNavigateBack()

    ListDetailPaneScaffold(
        modifier = modifier.fillMaxSize(),
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            SettingsMasterPane(
                settingsUiState = state,
                onSectionClick = { section ->
                    onSectionSelected(section)
                    coroutineScope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, section)
                    }
                },
            )
        },
        detailPane = {
            SettingsDetailPane(
                section = state.selectedSection,
                harnessSettingsState = harnessSettingsState,
                onThemeClick = onThemeClick,
                onHarnessAction = onHarnessAction,
                onBackClick = when {
                    canNavigateBack -> { { coroutineScope.launch { navigator.navigateBack() } } }
                    else -> null
                },
            )
        },
        paneExpansionDragHandle = { state: PaneExpansionState ->
            KvaceVerticalPaneExpansionDragHandle(state)
        },
        paneExpansionState = paneExpansionState,
    )
}

@Composable
private fun SettingsMasterPane(
    modifier: Modifier = Modifier,
    settingsUiState: SettingsUiState,
    onSectionClick: (SettingsSection) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(settingsUiState.sections, key = { it.name }) { section ->
                SettingsSectionItem(
                    section = section,
                    selected = section == settingsUiState.selectedSection,
                    onClick = { onSectionClick(section) },
                )
            }
        }
    }
}

@Composable
private fun SettingsDetailPane(
    modifier: Modifier = Modifier,
    section: SettingsSection,
    harnessSettingsState: HarnessSettingsUiState,
    onThemeClick: () -> Unit,
    onHarnessAction: (HarnessSettingsAction) -> Unit,
    onBackClick: (() -> Unit)?,
) = Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(section.stringRes),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = null,
                        )
                    }
                }
            },
        )
    },
) { paddingValues ->
    when (section) {
        SettingsSection.Libraries -> LibrariesSettings(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        )
        else -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                SettingsDetail(
                    section = section,
                    harnessSettingsState = harnessSettingsState,
                    onThemeClick = onThemeClick,
                    onHarnessAction = onHarnessAction,
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionItem(section: SettingsSection, selected: Boolean, onClick: () -> Unit) {
    val containerColor: Color = when {
        selected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_section_${section.name}")
            .clickable(onClick = onClick),
        leadingContent = { Icon(painter = section.icon, contentDescription = null) },
        headlineContent = { Text(stringResource(section.stringRes)) },
        colors = ListItemDefaults.colors(containerColor = containerColor),
    )
}

@Composable
private fun AboutSettings() {
    val uriHandler = LocalUriHandler.current
    Text(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        text = stringResource(Res.string.settings_about_message),
    )
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                uriHandler.openUri(PROJECT_GITHUB_URL)
            },
        leadingContent = {
            Icon(
                painter = painterResource(Res.drawable.ic_code),
                contentDescription = null,
            )
        },
        headlineContent = { Text(stringResource(Res.string.settings_source_code)) },
        supportingContent = { Text(stringResource(Res.string.settings_source_code_message)) },
    )
}

@Composable
private fun SettingsDetail(
    section: SettingsSection,
    harnessSettingsState: HarnessSettingsUiState,
    onThemeClick: () -> Unit,
    onHarnessAction: (HarnessSettingsAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (section) {
            SettingsSection.Appearance -> ThemePreferenceItem(onClick = onThemeClick)
            SettingsSection.Harness -> HarnessSettings(
                state = harnessSettingsState,
                onAction = onHarnessAction,
            )
            SettingsSection.Libraries -> Unit
            SettingsSection.About -> AboutSettings()
        }
    }
}

@Composable
private fun LibrariesSettings(modifier: Modifier = Modifier) {
    val libraries: Libs? by produceLibraries {
        Res.readBytes("files/aboutlibraries.json").decodeToString()
    }
    LibrariesContainer(libraries, modifier)
}

private val SettingsSection.icon: Painter
    @Composable get() = when (this) {
        SettingsSection.Appearance -> painterResource(Res.drawable.ic_palette)
        SettingsSection.Harness -> painterResource(Res.drawable.ic_science)
        SettingsSection.Libraries -> painterResource(Res.drawable.ic_code)
        SettingsSection.About -> painterResource(Res.drawable.ic_info)
    }

private val SettingsSection.stringRes: StringResource
    get() = when (this) {
        SettingsSection.Appearance -> Res.string.settings_section_appearance
        SettingsSection.Harness -> Res.string.settings_section_harness
        SettingsSection.Libraries -> Res.string.settings_section_libraries
        SettingsSection.About -> Res.string.settings_section_about
    }

private const val PROJECT_GITHUB_URL = "https://github.com/softartdev/Kvace"

@Preview
@Composable
private fun SettingsScreenPreview() {
    PreferableMaterialTheme {
        SettingsScreen(
            state = SettingsUiState(
                sections = SettingsSection.entries,
                selectedSection = SettingsSection.Appearance,
            ),
            harnessSettingsState = HarnessSettingsUiState(),
            onSectionSelected = {},
            onThemeClick = {},
            onHarnessAction = {},
        )
    }
}
