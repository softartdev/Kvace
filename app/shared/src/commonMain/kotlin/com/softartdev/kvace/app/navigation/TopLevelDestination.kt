package com.softartdev.kvace.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.softartdev.kvace.core.presentation.AppRoute
import com.softartdev.kvace.core.ui.resources.Res
import com.softartdev.kvace.core.ui.resources.ic_hub
import com.softartdev.kvace.core.ui.resources.ic_settings
import com.softartdev.kvace.core.ui.resources.ic_workspaces
import com.softartdev.kvace.core.ui.resources.nav_agents
import com.softartdev.kvace.core.ui.resources.nav_chat
import com.softartdev.kvace.core.ui.resources.nav_settings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

enum class TopLevelDestination(
    val key: String,
    val stringResource: StringResource,
    val drawableResource: DrawableResource,
    val route: AppRoute,
) {
    Chat(
        key = "chat",
        stringResource = Res.string.nav_chat,
        drawableResource = Res.drawable.ic_workspaces,
        route = AppRoute.Chat,
    ),
    Agents(
        key = "agents",
        stringResource = Res.string.nav_agents,
        drawableResource = Res.drawable.ic_hub,
        route = AppRoute.Agents,
    ),
    Settings(
        key = "settings",
        stringResource = Res.string.nav_settings,
        drawableResource = Res.drawable.ic_settings,
        route = AppRoute.Settings,
    );

    val label: String
        @Composable get() = stringResource(stringResource)

    val icon: Painter
        @Composable get() = painterResource(drawableResource)
}
