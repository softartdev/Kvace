package com.softartdev.kvace.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.softartdev.kvace.feature.agent.domain.AgentExecutionError
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.presentation.AgentConfigUiState
import com.softartdev.kvace.feature.agent.presentation.OllamaConnectionStatus
import com.softartdev.kvace.feature.agent.presentation.OllamaEndpointSettingsUiState
import com.softartdev.kvace.feature.agent.presentation.OllamaModelsStatus
import com.softartdev.kvace.feature.chat.domain.ChatMessage
import com.softartdev.kvace.feature.chat.domain.ChatSummary
import com.softartdev.kvace.feature.chat.domain.Conversation
import com.softartdev.kvace.feature.chat.domain.MessageAuthor
import com.softartdev.kvace.feature.chat.presentation.ChatDeleteDialogState
import com.softartdev.kvace.feature.chat.presentation.ChatRenameDialogState
import com.softartdev.kvace.feature.chat.presentation.ChatUiState
import com.softartdev.kvace.feature.settings.domain.SettingsSection
import com.softartdev.kvace.feature.settings.presentation.HarnessSettingsUiState
import com.softartdev.kvace.feature.settings.presentation.SettingsUiState

data class ScreenshootWorkspacePreviewState(
    val chatState: ChatUiState,
)

data class ScreenshootProvidersPreviewState(
    val agentState: AgentConfigUiState,
    val ollamaState: OllamaEndpointSettingsUiState,
)

data class ScreenshootSettingsPreviewState(
    val settingsState: SettingsUiState,
    val harnessState: HarnessSettingsUiState = HarnessSettingsUiState(),
)

class ScreenshootWorkspaceSelectedPreviewProvider :
    PreviewParameterProvider<ScreenshootWorkspacePreviewState> {
    override val values: Sequence<ScreenshootWorkspacePreviewState> =
        sequenceOf(ScreenshootWorkspacePreviewState(ScreenshootPreviewSamplesForStore.workspaceSelected))
}

class ScreenshootWorkspacePlaceholderPreviewProvider :
    PreviewParameterProvider<ScreenshootWorkspacePreviewState> {
    override val values: Sequence<ScreenshootWorkspacePreviewState> =
        sequenceOf(ScreenshootWorkspacePreviewState(ScreenshootPreviewSamplesForStore.workspacePlaceholder))
}

class ScreenshootWorkspaceSendingPreviewProvider :
    PreviewParameterProvider<ScreenshootWorkspacePreviewState> {
    override val values: Sequence<ScreenshootWorkspacePreviewState> =
        sequenceOf(ScreenshootWorkspacePreviewState(ScreenshootPreviewSamplesForStore.workspaceSending))
}

class ScreenshootWorkspaceErrorPreviewProvider :
    PreviewParameterProvider<ScreenshootWorkspacePreviewState> {
    override val values: Sequence<ScreenshootWorkspacePreviewState> =
        sequenceOf(ScreenshootWorkspacePreviewState(ScreenshootPreviewSamplesForStore.workspaceError))
}

class ScreenshootWorkspaceLongChatPreviewProvider :
    PreviewParameterProvider<ScreenshootWorkspacePreviewState> {
    override val values: Sequence<ScreenshootWorkspacePreviewState> =
        sequenceOf(ScreenshootWorkspacePreviewState(ScreenshootPreviewSamplesForStore.workspaceLongChat))
}

class ScreenshootWorkspaceRenameDialogPreviewProvider :
    PreviewParameterProvider<ScreenshootWorkspacePreviewState> {
    override val values: Sequence<ScreenshootWorkspacePreviewState> =
        sequenceOf(ScreenshootWorkspacePreviewState(ScreenshootPreviewSamplesForStore.workspaceRenameDialog))
}

class ScreenshootWorkspaceDeleteDialogPreviewProvider :
    PreviewParameterProvider<ScreenshootWorkspacePreviewState> {
    override val values: Sequence<ScreenshootWorkspacePreviewState> =
        sequenceOf(ScreenshootWorkspacePreviewState(ScreenshootPreviewSamplesForStore.workspaceDeleteDialog))
}

class ScreenshootProvidersOllamaPreviewProvider :
    PreviewParameterProvider<ScreenshootProvidersPreviewState> {
    override val values: Sequence<ScreenshootProvidersPreviewState> =
        sequenceOf(
            ScreenshootProvidersPreviewState(
                agentState = ScreenshootPreviewSamplesForStore.providersOllama,
                ollamaState = ScreenshootPreviewSamplesForStore.ollamaLoaded,
            ),
        )
}

class ScreenshootProvidersOpenAiPreviewProvider :
    PreviewParameterProvider<ScreenshootProvidersPreviewState> {
    override val values: Sequence<ScreenshootProvidersPreviewState> =
        sequenceOf(
            ScreenshootProvidersPreviewState(
                agentState = ScreenshootPreviewSamplesForStore.providersOpenAi,
                ollamaState = ScreenshootPreviewSamplesForStore.ollamaLoaded,
            ),
        )
}

class ScreenshootProvidersOllamaErrorPreviewProvider :
    PreviewParameterProvider<ScreenshootProvidersPreviewState> {
    override val values: Sequence<ScreenshootProvidersPreviewState> =
        sequenceOf(
            ScreenshootProvidersPreviewState(
                agentState = ScreenshootPreviewSamplesForStore.providersOllama,
                ollamaState = ScreenshootPreviewSamplesForStore.ollamaSelectionRequired,
            ),
        )
}

class ScreenshootSettingsHarnessPreviewProvider :
    PreviewParameterProvider<ScreenshootSettingsPreviewState> {
    override val values: Sequence<ScreenshootSettingsPreviewState> =
        sequenceOf(
            ScreenshootSettingsPreviewState(
                settingsState = SettingsUiState(
                    sections = SettingsSection.entries,
                    selectedSection = SettingsSection.Harness,
                ),
                harnessState = ScreenshootPreviewSamplesForStore.harnessEnabled,
            ),
        )
}

class ScreenshootSettingsLibrariesPreviewProvider :
    PreviewParameterProvider<ScreenshootSettingsPreviewState> {
    override val values: Sequence<ScreenshootSettingsPreviewState> =
        sequenceOf(
            ScreenshootSettingsPreviewState(
                settingsState = SettingsUiState(
                    sections = SettingsSection.entries,
                    selectedSection = SettingsSection.Libraries,
                ),
            ),
        )
}

class ScreenshootSettingsAboutPreviewProvider :
    PreviewParameterProvider<ScreenshootSettingsPreviewState> {
    override val values: Sequence<ScreenshootSettingsPreviewState> =
        sequenceOf(
            ScreenshootSettingsPreviewState(
                settingsState = SettingsUiState(
                    sections = SettingsSection.entries,
                    selectedSection = SettingsSection.About,
                ),
            ),
        )
}

internal object ScreenshootPreviewSamplesForStore {
    val workspaceSelected = ChatUiState(
        chats = chatSummaries(),
        selectedConversation = Conversation(
            id = 1L,
            title = "Workspace plan",
            createdAtMillis = 1_000L,
            updatedAtMillis = 4_000L,
            messages = listOf(
                ChatMessage(1L, MessageAuthor.User, "Can you summarize this workspace?", 1_000L),
                ChatMessage(
                    2L,
                    MessageAuthor.Reasoning,
                    "The model is checking the local context before answering.",
                    2_000L,
                    generatedByModelName = "qwen3.5:0.8b",
                    generatedAtMillis = 2_000L,
                ),
                ChatMessage(
                    3L,
                    MessageAuthor.Assistant,
                    "Kvace is a local-first multiplatform agent workspace with persistent chat history, provider settings, and adaptive navigation.",
                    3_000L,
                    generatedByModelName = "qwen3.5:0.8b",
                    generatedAtMillis = 3_000L,
                ),
            ),
        ),
    )

    val workspacePlaceholder = ChatUiState(
        chats = chatSummaries(),
        selectedConversation = null,
    )

    val workspaceSending = workspaceSelected.copy(
        inputText = "Continue with the implementation details",
        isSending = true,
    )

    val workspaceError = workspaceSelected.copy(
        chats = workspaceSelected.chats.map { chat ->
            if (chat.id == 1L) {
                chat.copy(
                    lastMessagePreview = "",
                    lastMessageError = AgentExecutionError.Authentication,
                    messageCount = chat.messageCount + 1,
                )
            } else {
                chat
            }
        },
        selectedConversation = workspaceSelected.selectedConversation?.let { conversation ->
            conversation.copy(
                messages = conversation.messages + ChatMessage(
                    id = 4L,
                    author = MessageAuthor.Error,
                    text = "",
                    createdAtMillis = 4_000L,
                    generatedByModelName = "gpt-4o",
                    generatedAtMillis = 4_000L,
                    error = AgentExecutionError.Authentication,
                ),
            )
        },
    )

    val workspaceLongChat = workspaceSelected.copy(
        selectedConversation = Conversation(
            id = 4L,
            title = "Long history review",
            createdAtMillis = 1_000L,
            updatedAtMillis = 44_000L,
            messages = longChatMessages(),
        ),
    )

    val workspaceRenameDialog = workspaceSelected.copy(
        renameDialog = ChatRenameDialogState(
            conversationId = 1L,
            titleInput = "Workspace launch plan",
        ),
    )

    val workspaceDeleteDialog = workspaceSelected.copy(
        deleteDialog = ChatDeleteDialogState(
            conversationId = 1L,
            title = "Workspace plan",
        ),
    )

    val providersOllama = AgentConfigUiState(
        providers = providers(),
        selectedProviderId = AgentProviderId.Ollama,
        openAiModelInput = "gpt-4o",
    )

    val providersOpenAi = AgentConfigUiState(
        providers = providers(),
        selectedProviderId = AgentProviderId.OpenAI,
        openAiModelInput = "gpt-4o",
        openAiEndpointInput = "https://api.openai.com",
    )

    val ollamaLoaded = OllamaEndpointSettingsUiState(
        hostInput = "127.0.0.1",
        portInput = "11434",
        modelInput = "qwen3.5:0.8b",
        availableModels = listOf("qwen3.5:0.8b", "llama3.2:3b", "mistral:7b"),
        connectionStatus = OllamaConnectionStatus.Success,
        modelsStatus = OllamaModelsStatus.Loaded,
    )

    val ollamaSelectionRequired = ollamaLoaded.copy(
        modelInput = "",
        modelsStatus = OllamaModelsStatus.SelectionRequired,
    )

    val harnessEnabled = HarnessSettingsUiState(
        enabled = true,
        systemPrompt = "You are Kvace, a concise AI assistant inside a multiplatform agent app.",
    )

    val settingsHarness = SettingsUiState(
        sections = SettingsSection.entries,
        selectedSection = SettingsSection.Harness,
    )

    private fun chatSummaries() = listOf(
        ChatSummary(
            id = 1L,
            title = "Workspace plan",
            lastMessagePreview = "Kvace is a local-first multiplatform agent workspace with persistent chat history.",
            updatedAtMillis = 4_000L,
            messageCount = 3L,
        ),
        ChatSummary(
            id = 2L,
            title = "Release checklist",
            lastMessagePreview = "Run targeted tests, render screenshots, check the diff, and verify platform builds.",
            updatedAtMillis = 2_000L,
            messageCount = 8L,
        ),
        ChatSummary(
            id = 3L,
            title = "Provider setup",
            lastMessagePreview = "Ollama is selected by default and uses the local endpoint unless changed in Providers.",
            updatedAtMillis = 1_000L,
            messageCount = 5L,
        ),
    )

    private fun providers() = listOf(
        AgentProviderConfig(
            id = AgentProviderId.Ollama,
            modelName = "qwen3.5:0.8b",
            endpoint = "http://127.0.0.1:11434",
            isConfigured = true,
        ),
        AgentProviderConfig(
            id = AgentProviderId.OnDevice,
            modelName = "Platform model",
            isConfigured = false,
        ),
        AgentProviderConfig(
            id = AgentProviderId.OpenAI,
            modelName = "gpt-4o",
            endpoint = "https://api.openai.com",
            isConfigured = false,
        ),
    )

    private fun longChatMessages(): List<ChatMessage> = buildList {
        repeat(18) { index ->
            val userId = index * 2L + 1L
            val assistantId = index * 2L + 2L
            add(
                ChatMessage(
                    id = userId,
                    author = MessageAuthor.User,
                    text = "Review item ${index + 1}: keep the implementation focused and preserve the existing architecture.",
                    createdAtMillis = 1_000L + userId,
                ),
            )
            add(
                ChatMessage(
                    id = assistantId,
                    author = MessageAuthor.Assistant,
                    text = "Item ${index + 1} is accounted for. The next change stays inside the feature boundary and keeps the adaptive layout behavior intact.",
                    createdAtMillis = 1_000L + assistantId,
                    generatedByModelName = "qwen3.5:0.8b",
                    generatedAtMillis = 1_000L + assistantId,
                ),
            )
        }
    }
}
