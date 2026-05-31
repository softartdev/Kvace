package com.softartdev.kvace.core.presentation

import kotlin.test.Test
import kotlin.test.assertIs

class StateActionTest {
    @Test
    fun markerInterfacesCanBeUsedForFeatureStateAndActions() {
        val state = TestState
        val action = TestAction

        assertIs<UiState>(state)
        assertIs<UiAction>(action)
    }
}

private data object TestState : UiState

private data object TestAction : UiAction
