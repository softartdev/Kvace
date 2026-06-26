package com.softartdev.kvace

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationInstrumentedTest {
    private val localNetworkPermissionRule = LocalNetworkPermissionRule()
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: TestRule = RuleChain
        .outerRule(localNetworkPermissionRule)
        .around(composeRule)

    @Test
    fun repeatedTopLevelNavigationDoesNotDuplicateBackStack() {
        repeat(3) {
            composeRule.onNodeWithTag("nav_settings").performClick()
        }
        composeRule.onNodeWithTag("nav_settings").assertIsSelected()

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithTag("nav_chat").assertIsSelected()
    }

    @Test
    fun themeActionOpensTypedDialogAndReturnsToSettings() {
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("settings_section_Appearance").performClick()
        composeRule.onNodeWithText("Choose theme").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule
                .onAllNodesWithText("Choose theme", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size > 1
        }
        pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_settings").assertIsSelected()
    }
}
