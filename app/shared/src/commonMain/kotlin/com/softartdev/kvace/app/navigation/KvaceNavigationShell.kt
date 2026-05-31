package com.softartdev.kvace.app.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.softartdev.kvace.core.ui.NavigationLayoutType
import com.softartdev.kvace.core.ui.isImeVisible
import com.softartdev.kvace.core.ui.navigationLayoutTypeForWidth
import com.softartdev.kvace.feature.agent.presentation.AgentConfigViewModel
import com.softartdev.kvace.feature.agent.presentation.OllamaEndpointSettingsViewModel
import com.softartdev.kvace.feature.agent.ui.AgentConfigScreen
import com.softartdev.kvace.feature.agent.ui.OllamaEndpointSettings
import com.softartdev.kvace.feature.chat.presentation.ChatViewModel
import com.softartdev.kvace.feature.chat.ui.ChatScreen
import com.softartdev.kvace.feature.settings.presentation.SettingsViewModel
import com.softartdev.kvace.feature.settings.ui.SettingsScreen
import com.softartdev.theme.material3.ThemeDialogContent
import kvace.app.shared.generated.resources.Res
import kvace.app.shared.generated.resources.nav_agents
import kvace.app.shared.generated.resources.nav_chat
import kvace.app.shared.generated.resources.nav_settings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun KvaceNavigationShell() {
    val navController = rememberNavController()
    val chatLabel = stringResource(Res.string.nav_chat)
    val agentsLabel = stringResource(Res.string.nav_agents)
    val settingsLabel = stringResource(Res.string.nav_settings)
    val destinations = remember(chatLabel, agentsLabel, settingsLabel) {
        listOf(
            TopLevelDestination("chat", chatLabel, Icons.AutoMirrored.Filled.Chat, AppRoute.Chat),
            TopLevelDestination("agents", agentsLabel, Icons.Default.Psychology, AppRoute.Agents),
            TopLevelDestination(
                "settings",
                settingsLabel,
                Icons.Default.Settings,
                AppRoute.Settings
            ),
        )
    }
    var selectedKey by rememberSaveable { mutableStateOf(destinations.first().key) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        BoxWithConstraints {
            when (navigationLayoutTypeForWidth(maxWidth)) {
                NavigationLayoutType.BottomBar -> {
                    Scaffold(
                        content = { paddingValues ->
                            KvaceNavHost(
                                modifier = Modifier
                                    .padding(paddingValues)
                                    .consumeWindowInsets(paddingValues),
                                navController = navController
                            )
                        },
                        bottomBar = {
                            if (!WindowInsets.isImeVisible) KvaceBottomBar(
                                destinations = destinations,
                                selectedKey = selectedKey,
                                onDestinationClick = { destination ->
                                    selectedKey = destination.key
                                    navController.navigate(destination.route)
                                },
                            )
                        },
                    )
                }
                NavigationLayoutType.NavigationRail -> {
                    Row(Modifier.fillMaxSize()) {
                        KvaceNavigationRail(
                            destinations = destinations,
                            selectedKey = selectedKey,
                            onDestinationClick = { destination ->
                                selectedKey = destination.key
                                navController.navigate(destination.route)
                            },
                        )
                        KvaceNavHost(navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
private fun KvaceNavHost(modifier: Modifier = Modifier, navController: NavHostController) {
    NavHost(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        navController = navController,
        startDestination = AppRoute.Chat,
    ) {
        composable<AppRoute.Chat> {
            val viewModel = koinViewModel<ChatViewModel>()
            ChatScreen(viewModel = viewModel)
        }
        composable<AppRoute.Agents> {
            val viewModel = koinViewModel<AgentConfigViewModel>()
            AgentConfigScreen(viewModel = viewModel)
        }
        composable<AppRoute.Settings> {
            val settingsViewModel = koinViewModel<SettingsViewModel>()
            val ollamaEndpointSettingsViewModel = koinViewModel<OllamaEndpointSettingsViewModel>()
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                onThemeClick = { navController.navigate(AppRoute.ThemeDialog) },
                agentSettingsContent = {
                    OllamaEndpointSettings(viewModel = ollamaEndpointSettingsViewModel)
                },
            )
        }
        dialog<AppRoute.ThemeDialog> {
            ThemeDialogContent(dismissDialog = navController::popBackStack)
        }
    }
}

@Composable
private fun KvaceBottomBar(
    destinations: List<TopLevelDestination>,
    selectedKey: String,
    onDestinationClick: (TopLevelDestination) -> Unit,
) {
    NavigationBar {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = destination.key == selectedKey,
                onClick = { onDestinationClick(destination) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
private fun KvaceNavigationRail(
    destinations: List<TopLevelDestination>,
    selectedKey: String,
    onDestinationClick: (TopLevelDestination) -> Unit,
) {
    NavigationRail {
        destinations.forEach { destination ->
            NavigationRailItem(
                selected = destination.key == selectedKey,
                onClick = { onDestinationClick(destination) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
            )
        }
    }
}

private data class TopLevelDestination(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val route: AppRoute,
)
