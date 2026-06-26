@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.softartdev.kvace.app.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.softartdev.kvace.app.snackbar.ComposeSnackbarInteractor
import com.softartdev.kvace.core.presentation.AppRoute
import com.softartdev.kvace.feature.agent.presentation.AgentConfigViewModel
import com.softartdev.kvace.feature.agent.presentation.OllamaEndpointSettingsViewModel
import com.softartdev.kvace.feature.agent.ui.AgentConfigScreen
import com.softartdev.kvace.feature.chat.presentation.ChatViewModel
import com.softartdev.kvace.feature.chat.ui.ChatScreen
import com.softartdev.kvace.feature.settings.presentation.HarnessSettingsViewModel
import com.softartdev.kvace.feature.settings.presentation.SettingsViewModel
import com.softartdev.kvace.feature.settings.ui.SettingsScreen
import com.softartdev.theme.material3.ThemeDialogContent
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.enums.EnumEntries

@Composable
fun KvaceNavigationShell() {
    val navController = rememberNavController()
    val router = koinInject<ComposeRouter>()
    val snackbarInteractor = koinInject<ComposeSnackbarInteractor>()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val uiScope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val destinations: EnumEntries<TopLevelDestination> = TopLevelDestination.entries
    val selectedDestination: TopLevelDestination? = destinations.firstOrNull { destination ->
        currentBackStackEntry
            ?.destination
            ?.hierarchy
            ?.any { it.hasRoute(destination.route::class) } == true
    }
    DisposableEffect(navController, router) {
        router.attach(navController)
        onDispose { router.release(navController) }
    }
    DisposableEffect(snackbarHostState, clipboardManager, uiScope, snackbarInteractor) {
        snackbarInteractor.attach(snackbarHostState, clipboardManager, uiScope)
        onDispose { snackbarInteractor.release(snackbarHostState) }
    }
    KvaceNavigationScaffold(
        destinations = destinations,
        selectedDestination = selectedDestination,
        onDestinationClick = { router.navigateTopLevel(it.route) },
        snackbarHostState = snackbarHostState,
        content = { modifier ->
            KvaceNavHost(
                modifier = modifier,
                navController = navController,
                router = router,
            )
        },
    )
}

@Composable
private fun KvaceNavigationScaffold(
    modifier: Modifier = Modifier,
    destinations: List<TopLevelDestination>,
    selectedDestination: TopLevelDestination?,
    onDestinationClick: (TopLevelDestination) -> Unit,
    snackbarHostState: SnackbarHostState,
    content: @Composable (Modifier) -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                destinations.forEach { destination ->
                    item(
                        modifier = Modifier.testTag("nav_${destination.key}"),
                        selected = destination == selectedDestination,
                        onClick = { onDestinationClick(destination) },
                        icon = {
                            Icon(
                                painter = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            },
        ) {
            Scaffold(
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding(),
                    )
                },
            ) { paddingValues ->
                content(
                    Modifier
                        .padding(paddingValues)
                        .consumeWindowInsets(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun KvaceNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    router: ComposeRouter,
) {
    NavHost(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        navController = navController,
        startDestination = AppRoute.Chat,
    ) {
        composable<AppRoute.Chat> {
            ChatScreen(viewModel = koinViewModel<ChatViewModel>())
        }
        composable<AppRoute.Agents> {
            AgentConfigScreen(
                viewModel = koinViewModel<AgentConfigViewModel>(),
                ollamaEndpointSettingsViewModel = koinViewModel<OllamaEndpointSettingsViewModel>(),
            )
        }
        composable<AppRoute.Settings> {
            SettingsScreen(
                settingsViewModel = koinViewModel<SettingsViewModel>(),
                harnessSettingsViewModel = koinViewModel<HarnessSettingsViewModel>(),
            )
        }
        dialog<AppRoute.ThemeDialog> {
            ThemeDialogContent(dismissDialog = router::popBackStack)
        }
    }
}

@Preview
@Composable
private fun KvaceNavigationShellPreview() {
    val destinations: EnumEntries<TopLevelDestination> = TopLevelDestination.entries
    MaterialTheme {
        KvaceNavigationScaffold(
            destinations = destinations,
            selectedDestination = destinations.first(),
            onDestinationClick = {},
            snackbarHostState = remember { SnackbarHostState() },
            content = { modifier ->
                Surface(modifier = modifier.fillMaxSize()) {
                    Text(
                        text = destinations.first().label,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            },
        )
    }
}
