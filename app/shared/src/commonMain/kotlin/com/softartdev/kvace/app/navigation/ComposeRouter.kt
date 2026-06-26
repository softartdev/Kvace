package com.softartdev.kvace.app.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.softartdev.kvace.core.presentation.Router

class ComposeRouter : Router {
    private var controller: NavHostController? = null

    fun attach(controller: NavHostController) {
        this.controller = controller
    }

    fun release(controller: NavHostController) {
        if (this.controller === controller) {
            this.controller = null
        }
    }

    override fun <T : Any> navigate(route: T) {
        controller?.navigate(route)
    }

    override fun <T : Any> navigateSingleTop(route: T) {
        controller?.navigate(route) {
            launchSingleTop = true
        }
    }

    override fun <T : Any> navigateTopLevel(route: T) {
        val navController = controller ?: return
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    override fun <T : Any> navigateClearingBackStack(route: T) {
        val navController = controller ?: return
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    override fun popBackStack(): Boolean = controller?.popBackStack() ?: false
}
