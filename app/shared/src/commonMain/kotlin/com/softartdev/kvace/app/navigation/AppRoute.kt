package com.softartdev.kvace.app.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object Chat : AppRoute

    @Serializable
    data object Agents : AppRoute

    @Serializable
    data object Settings : AppRoute

    @Serializable
    data object ThemeDialog : AppRoute
}
