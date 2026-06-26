package com.softartdev.kvace.core.presentation

interface Router {
    fun <T : Any> navigate(route: T)

    fun <T : Any> navigateSingleTop(route: T)

    fun <T : Any> navigateTopLevel(route: T)

    fun <T : Any> navigateClearingBackStack(route: T)

    fun popBackStack(): Boolean
}
