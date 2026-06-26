package com.softartdev.kvace.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.softartdev.kvace.feature.agent.ui.AgentConfigScreen
import com.softartdev.kvace.feature.chat.ui.ChatScreen
import com.softartdev.kvace.feature.settings.ui.SettingsScreen
import com.softartdev.theme.material3.PreferableMaterialTheme

@Preview(widthDp = 920, heightDp = 720)
@Composable
fun ScreenshootWorkspaceWidePreview(
    @PreviewParameter(ScreenshootWorkspaceSelectedPreviewProvider::class) state: ScreenshootWorkspacePreviewState,
) = PreferableMaterialTheme {
    ChatScreen(
        state = state.chatState,
        onAction = {},
    )
}

@Preview(widthDp = 920, heightDp = 720)
@Composable
fun ScreenshootWorkspacePlaceholderPreview(
    @PreviewParameter(ScreenshootWorkspacePlaceholderPreviewProvider::class) state: ScreenshootWorkspacePreviewState,
) = PreferableMaterialTheme {
    ChatScreen(
        state = state.chatState,
        onAction = {},
    )
}

@Preview(widthDp = 390, heightDp = 760)
@Composable
fun ScreenshootWorkspaceCompactPreview(
    @PreviewParameter(ScreenshootWorkspaceSelectedPreviewProvider::class) state: ScreenshootWorkspacePreviewState,
) = PreferableMaterialTheme {
    ChatScreen(
        state = state.chatState,
        onAction = {},
        autoNavigateToSelectedChat = true,
    )
}

@Preview(widthDp = 920, heightDp = 720)
@Composable
fun ScreenshootWorkspaceSendingPreview(
    @PreviewParameter(ScreenshootWorkspaceSendingPreviewProvider::class) state: ScreenshootWorkspacePreviewState,
) = PreferableMaterialTheme {
    ChatScreen(
        state = state.chatState,
        onAction = {},
    )
}

@Preview(widthDp = 920, heightDp = 720)
@Composable
fun ScreenshootWorkspaceLongChatPreview(
    @PreviewParameter(ScreenshootWorkspaceLongChatPreviewProvider::class) state: ScreenshootWorkspacePreviewState,
) = PreferableMaterialTheme {
    ChatScreen(
        state = state.chatState,
        onAction = {},
    )
}

@Preview(widthDp = 920, heightDp = 720)
@Composable
fun ScreenshootWorkspaceRenameDialogPreview(
    @PreviewParameter(ScreenshootWorkspaceRenameDialogPreviewProvider::class) state: ScreenshootWorkspacePreviewState,
) = PreferableMaterialTheme {
    ChatScreen(
        state = state.chatState,
        onAction = {},
    )
}

@Preview(widthDp = 920, heightDp = 720)
@Composable
fun ScreenshootWorkspaceDeleteDialogPreview(
    @PreviewParameter(ScreenshootWorkspaceDeleteDialogPreviewProvider::class) state: ScreenshootWorkspacePreviewState,
) = PreferableMaterialTheme {
    ChatScreen(
        state = state.chatState,
        onAction = {},
    )
}

@Preview(widthDp = 920, heightDp = 720)
@Composable
fun ScreenshootProvidersWidePreview(
    @PreviewParameter(ScreenshootProvidersOllamaPreviewProvider::class) state: ScreenshootProvidersPreviewState,
) = PreferableMaterialTheme {
    AgentConfigScreen(
        state = state.agentState,
        ollamaState = state.ollamaState,
        onAction = {},
        onOllamaAction = {},
    )
}

@Preview(widthDp = 390, heightDp = 760)
@Composable
fun ScreenshootProvidersCompactPreview(
    @PreviewParameter(ScreenshootProvidersOllamaPreviewProvider::class) state: ScreenshootProvidersPreviewState,
) = PreferableMaterialTheme {
    AgentConfigScreen(
        state = state.agentState,
        ollamaState = state.ollamaState,
        onAction = {},
        onOllamaAction = {},
        autoNavigateToSelectedProvider = true,
    )
}

@Preview(widthDp = 920, heightDp = 720)
@Composable
fun ScreenshootProvidersOpenAiPreview(
    @PreviewParameter(ScreenshootProvidersOpenAiPreviewProvider::class) state: ScreenshootProvidersPreviewState,
) = PreferableMaterialTheme {
    AgentConfigScreen(
        state = state.agentState,
        ollamaState = state.ollamaState,
        onAction = {},
        onOllamaAction = {},
    )
}

@Preview(widthDp = 920, heightDp = 720)
@Composable
fun ScreenshootSettingsHarnessPreview(
    @PreviewParameter(ScreenshootSettingsHarnessPreviewProvider::class) state: ScreenshootSettingsPreviewState,
) = PreferableMaterialTheme {
    SettingsScreen(
        state = state.settingsState,
        harnessSettingsState = state.harnessState,
        onSectionSelected = {},
        onThemeClick = {},
        onHarnessAction = {},
    )
}

@Preview(widthDp = 920, heightDp = 720)
@Composable
fun ScreenshootSettingsLibrariesPreview(
    @PreviewParameter(ScreenshootSettingsLibrariesPreviewProvider::class) state: ScreenshootSettingsPreviewState,
) = PreferableMaterialTheme {
    SettingsScreen(
        state = state.settingsState,
        harnessSettingsState = state.harnessState,
        onSectionSelected = {},
        onThemeClick = {},
        onHarnessAction = {},
    )
}

@Preview(widthDp = 920, heightDp = 720)
@Composable
fun ScreenshootSettingsAboutPreview(
    @PreviewParameter(ScreenshootSettingsAboutPreviewProvider::class) state: ScreenshootSettingsPreviewState,
) = PreferableMaterialTheme {
    SettingsScreen(
        state = state.settingsState,
        harnessSettingsState = state.harnessState,
        onSectionSelected = {},
        onThemeClick = {},
        onHarnessAction = {},
    )
}
