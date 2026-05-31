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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softartdev.kvace.core.ui.KvaceVerticalPaneExpansionDragHandle
import com.softartdev.kvace.feature.settings.domain.SettingsSection
import com.softartdev.kvace.feature.settings.presentation.SettingsAction
import com.softartdev.kvace.feature.settings.presentation.SettingsUiState
import com.softartdev.kvace.feature.settings.presentation.SettingsViewModel
import com.softartdev.theme.material3.PreferableMaterialTheme
import com.softartdev.theme.material3.ThemePreferenceItem
import kvace.feature.settings.ui.generated.resources.Res
import kvace.feature.settings.ui.generated.resources.settings_about_message
import kvace.feature.settings.ui.generated.resources.settings_agents_message
import kvace.feature.settings.ui.generated.resources.settings_section_about
import kvace.feature.settings.ui.generated.resources.settings_section_agents
import kvace.feature.settings.ui.generated.resources.settings_section_appearance
import kvace.feature.settings.ui.generated.resources.settings_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onThemeClick: () -> Unit,
    modifier: Modifier = Modifier,
    agentSettingsContent: @Composable () -> Unit = { DefaultAgentSettingsMessage() },
) {
    LaunchedEffect(settingsViewModel) { settingsViewModel.loadSettings() }
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    SettingsScreenContent(
        modifier = modifier,
        settingsUiState = settingsUiState,
        onSettingsAction = settingsViewModel::onAction,
        onThemeClick = onThemeClick,
        agentSettingsContent = agentSettingsContent,
    )
}

@Composable
private fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    settingsUiState: SettingsUiState,
    onSettingsAction: (SettingsAction) -> Unit,
    onThemeClick: () -> Unit,
    agentSettingsContent: @Composable () -> Unit,
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
                settingsUiState = settingsUiState,
                onSectionClick = { section ->
                    onSettingsAction(SettingsAction.SelectSection(section))
                    coroutineScope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, section)
                    }
                },
            )
        },
        detailPane = {
            SettingsDetailPane(
                section = settingsUiState.selectedSection,
                onThemeClick = onThemeClick,
                onBackClick = if (canNavigateBack) {
                    {
                        coroutineScope.launch { navigator.navigateBack() }
                    }
                } else {
                    null
                },
                agentSettingsContent = agentSettingsContent,
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
    settingsUiState: SettingsUiState,
    onSectionClick: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
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
    section: SettingsSection,
    onThemeClick: () -> Unit,
    onBackClick: (() -> Unit)?,
    agentSettingsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = section.label,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    }
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
            item {
                SettingsDetail(
                    section = section,
                    onThemeClick = onThemeClick,
                    agentSettingsContent = agentSettingsContent,
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionItem(
    section: SettingsSection,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_section_${section.name}")
            .clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = section.icon,
                contentDescription = null,
            )
        },
        headlineContent = { Text(section.label) },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    )
}

@Composable
private fun SettingsDetail(
    section: SettingsSection,
    onThemeClick: () -> Unit,
    agentSettingsContent: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (section) {
            SettingsSection.Appearance -> ThemePreferenceItem(onClick = onThemeClick)
            SettingsSection.Agents -> agentSettingsContent()
            SettingsSection.About -> Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                text = stringResource(Res.string.settings_about_message),
            )
        }
    }
}

@Composable
private fun DefaultAgentSettingsMessage() {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        text = stringResource(Res.string.settings_agents_message),
    )
}

private val SettingsSection.icon: ImageVector
    get() = when (this) {
        SettingsSection.Appearance -> Icons.Default.Palette
        SettingsSection.Agents -> Icons.Default.Psychology
        SettingsSection.About -> Icons.Default.Info
    }

private val SettingsSection.label: String
    @Composable get() = when (this) {
        SettingsSection.Appearance -> stringResource(Res.string.settings_section_appearance)
        SettingsSection.Agents -> stringResource(Res.string.settings_section_agents)
        SettingsSection.About -> stringResource(Res.string.settings_section_about)
    }

@Preview
@Composable
private fun SettingsScreenPreview() {
    PreferableMaterialTheme {
        SettingsScreenContent(
            settingsUiState = SettingsUiState(
                sections = SettingsSection.entries,
                selectedSection = SettingsSection.Appearance,
            ),
            onSettingsAction = {},
            onThemeClick = {},
            agentSettingsContent = { DefaultAgentSettingsMessage() },
        )
    }
}
