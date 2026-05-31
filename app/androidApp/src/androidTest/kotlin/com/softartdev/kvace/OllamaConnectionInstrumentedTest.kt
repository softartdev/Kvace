package com.softartdev.kvace

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OllamaConnectionInstrumentedTest {

    private val localNetworkPermissionRule = object : ExternalResource() {
        override fun before() {
            if (Build.VERSION.SDK_INT < LOCAL_NETWORK_PERMISSION_SDK) return

            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.uiAutomation.grantRuntimePermission(
                instrumentation.targetContext.packageName,
                ACCESS_LOCAL_NETWORK_PERMISSION,
            )
        }
    }

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: TestRule = RuleChain
        .outerRule(localNetworkPermissionRule)
        .around(composeRule)

    @Test
    fun settingsCanCheckConnectionToLocalOllamaHost() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithTag("settings_section_Agents").performClick()

        composeRule.onNodeWithTag("ollama_host_field")
            .performTextClearance()
        composeRule.onNodeWithTag("ollama_host_field")
            .performTextInput(ANDROID_EMULATOR_LOCALHOST)

        composeRule.onNodeWithTag("ollama_port_field")
            .performTextClearance()
        composeRule.onNodeWithTag("ollama_port_field")
            .performTextInput(OLLAMA_PORT)

        composeRule.onNodeWithTag("ollama_test_connection_button").performClick()

        composeRule.waitUntil(timeoutMillis = 20_000L) {
            composeRule
                .onAllNodesWithTag("ollama_connection_status")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        val statusText = composeRule.textForTag("ollama_connection_status")
        assertEquals("Connection successful", statusText)
        composeRule.onNodeWithTag("ollama_connection_status")
            .assertIsDisplayed()
    }

    private companion object {
        const val ACCESS_LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
        const val ANDROID_EMULATOR_LOCALHOST = "10.0.2.2"
        const val LOCAL_NETWORK_PERMISSION_SDK = 37
        const val OLLAMA_PORT = "11434"
    }
}

private fun ComposeTestRule.textForTag(tag: String): String {
    val text = onNodeWithTag(tag)
        .fetchSemanticsNode()
        .config[SemanticsProperties.Text]

    return text.joinToString(separator = "") { it.text }
}
