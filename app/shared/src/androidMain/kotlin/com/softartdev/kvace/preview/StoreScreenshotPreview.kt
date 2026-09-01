package com.softartdev.kvace.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.softartdev.kvace.feature.agent.ui.AgentConfigScreen
import com.softartdev.kvace.feature.chat.ui.ChatScreen
import com.softartdev.kvace.feature.settings.ui.SettingsScreen
import com.softartdev.theme.material3.PreferableMaterialTheme

private const val PLAY_PHONE = "spec:width=1080px,height=1920px,dpi=420"
private const val PLAY_TABLET = "spec:width=1920px,height=1080px,dpi=240"
private const val APP_STORE_IPHONE = "spec:width=1320px,height=2868px,dpi=460"
private const val APP_STORE_IPAD = "spec:width=2752px,height=2064px,dpi=320"
private const val DESKTOP_MARKETING = "spec:width=2560px,height=1600px,dpi=240"

@Preview(device = PLAY_PHONE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StorePlayPhone01WorkspacePreview() = StoreWorkspaceCompact()
@Preview(device = PLAY_PHONE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StorePlayPhone02ExecutionPreview() = StoreExecutionCompact()
@Preview(device = PLAY_PHONE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StorePlayPhone03ConversationPreview() = StoreConversationCompact()
@Preview(device = PLAY_PHONE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StorePlayPhone04OllamaPreview() = StoreOllamaCompact()
@Preview(device = PLAY_PHONE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StorePlayPhone05OpenAiPreview() = StoreOpenAiCompact()
@Preview(device = PLAY_PHONE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StorePlayPhone06SettingsPreview() = StoreSettingsCompact()

@Preview(device = PLAY_TABLET, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StorePlayTablet01WorkspacePreview() = StoreWorkspaceWide()
@Preview(device = PLAY_TABLET, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StorePlayTablet02ExecutionPreview() = StoreExecutionWide()
@Preview(device = PLAY_TABLET, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StorePlayTablet03ConversationPreview() = StoreConversationWide()
@Preview(device = PLAY_TABLET, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StorePlayTablet04OllamaPreview() = StoreOllamaWide()
@Preview(device = PLAY_TABLET, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StorePlayTablet05OpenAiPreview() = StoreOpenAiWide()
@Preview(device = PLAY_TABLET, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StorePlayTablet06SettingsPreview() = StoreSettingsWide()

@Preview(device = APP_STORE_IPHONE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreAppStoreIPhone01WorkspacePreview() = StoreWorkspaceCompact()
@Preview(device = APP_STORE_IPHONE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreAppStoreIPhone02ExecutionPreview() = StoreExecutionCompact()
@Preview(device = APP_STORE_IPHONE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreAppStoreIPhone03ConversationPreview() = StoreConversationCompact()
@Preview(device = APP_STORE_IPHONE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreAppStoreIPhone04OllamaPreview() = StoreOllamaCompact()
@Preview(device = APP_STORE_IPHONE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreAppStoreIPhone05OpenAiPreview() = StoreOpenAiCompact()
@Preview(device = APP_STORE_IPHONE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreAppStoreIPhone06SettingsPreview() = StoreSettingsCompact()

@Preview(device = APP_STORE_IPAD, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreAppStoreIPad01WorkspacePreview() = StoreWorkspaceWide()
@Preview(device = APP_STORE_IPAD, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreAppStoreIPad02ExecutionPreview() = StoreExecutionWide()
@Preview(device = APP_STORE_IPAD, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreAppStoreIPad03ConversationPreview() = StoreConversationWide()
@Preview(device = APP_STORE_IPAD, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreAppStoreIPad04OllamaPreview() = StoreOllamaWide()
@Preview(device = APP_STORE_IPAD, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreAppStoreIPad05OpenAiPreview() = StoreOpenAiWide()
@Preview(device = APP_STORE_IPAD, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreAppStoreIPad06SettingsPreview() = StoreSettingsWide()

@Preview(device = DESKTOP_MARKETING, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreDesktop01WorkspacePreview() = StoreWorkspaceWide()
@Preview(device = DESKTOP_MARKETING, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreDesktop02ExecutionPreview() = StoreExecutionWide()
@Preview(device = DESKTOP_MARKETING, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreDesktop03ProvidersPreview() = StoreOllamaWide()
@Preview(device = DESKTOP_MARKETING, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable fun StoreDesktop04SettingsPreview() = StoreSettingsWide()

@Composable private fun StoreWorkspaceCompact() = StoreTheme {
    ChatScreen(state = ScreenshootPreviewSamplesForStore.workspaceSelected, onAction = {}, showSelectedChatInitially = true)
}
@Composable private fun StoreExecutionCompact() = StoreTheme {
    ChatScreen(state = ScreenshootPreviewSamplesForStore.workspaceSending, onAction = {}, showSelectedChatInitially = true)
}
@Composable private fun StoreConversationCompact() = StoreTheme {
    ChatScreen(state = ScreenshootPreviewSamplesForStore.workspaceLongChat, onAction = {}, showSelectedChatInitially = true)
}
@Composable private fun StoreOllamaCompact() = StoreTheme {
    AgentConfigScreen(state = ScreenshootPreviewSamplesForStore.providersOllama, ollamaState = ScreenshootPreviewSamplesForStore.ollamaLoaded, onAction = {}, onOllamaAction = {}, showSelectedProviderInitially = true)
}
@Composable private fun StoreOpenAiCompact() = StoreTheme {
    AgentConfigScreen(state = ScreenshootPreviewSamplesForStore.providersOpenAi, ollamaState = ScreenshootPreviewSamplesForStore.ollamaLoaded, onAction = {}, onOllamaAction = {}, showSelectedProviderInitially = true)
}
@Composable private fun StoreSettingsCompact() = StoreTheme {
    SettingsScreen(state = ScreenshootPreviewSamplesForStore.settingsHarness, harnessSettingsState = ScreenshootPreviewSamplesForStore.harnessEnabled, onSectionSelected = {}, onThemeClick = {}, showSelectedSectionInitially = true)
}
@Composable private fun StoreWorkspaceWide() = StoreTheme { ChatScreen(state = ScreenshootPreviewSamplesForStore.workspaceSelected, onAction = {}) }
@Composable private fun StoreExecutionWide() = StoreTheme { ChatScreen(state = ScreenshootPreviewSamplesForStore.workspaceSending, onAction = {}) }
@Composable private fun StoreConversationWide() = StoreTheme { ChatScreen(state = ScreenshootPreviewSamplesForStore.workspaceLongChat, onAction = {}) }
@Composable private fun StoreOllamaWide() = StoreTheme { AgentConfigScreen(state = ScreenshootPreviewSamplesForStore.providersOllama, ollamaState = ScreenshootPreviewSamplesForStore.ollamaLoaded, onAction = {}, onOllamaAction = {}) }
@Composable private fun StoreOpenAiWide() = StoreTheme { AgentConfigScreen(state = ScreenshootPreviewSamplesForStore.providersOpenAi, ollamaState = ScreenshootPreviewSamplesForStore.ollamaLoaded, onAction = {}, onOllamaAction = {}) }
@Composable private fun StoreSettingsWide() = StoreTheme { SettingsScreen(state = ScreenshootPreviewSamplesForStore.settingsHarness, harnessSettingsState = ScreenshootPreviewSamplesForStore.harnessEnabled, onSectionSelected = {}, onThemeClick = {}) }
@Composable private fun StoreTheme(content: @Composable () -> Unit) { PreferableMaterialTheme { content() } }
