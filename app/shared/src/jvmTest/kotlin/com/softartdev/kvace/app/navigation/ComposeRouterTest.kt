package com.softartdev.kvace.app.navigation

import androidx.navigation.NavHostController
import com.softartdev.kvace.core.presentation.AppRoute
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFalse

class ComposeRouterTest {

    @Test
    fun commandsAreNoOpWithoutAttachedController() {
        val router = ComposeRouter()

        router.navigate(AppRoute.Chat)
        router.navigateSingleTop(AppRoute.Chat)
        router.navigateTopLevel(AppRoute.Chat)
        router.navigateClearingBackStack(AppRoute.Chat)

        assertFalse(router.popBackStack())
    }

    @Test
    fun releaseOnlyDetachesMatchingController() {
        val first = NavHostController()
        val second = NavHostController()
        val router = ComposeRouter()
        router.attach(first)
        router.attach(second)

        router.release(first)
        assertFails { router.navigate(AppRoute.Chat) }

        router.release(second)
        router.navigate(AppRoute.Chat)
    }
}
