package com.softartdev.kvace.feature.chat.ui

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.softartdev.kvace.feature.chat.domain.ChatMessage
import com.softartdev.kvace.feature.chat.domain.ChatSummary
import com.softartdev.kvace.feature.chat.domain.Conversation
import com.softartdev.kvace.feature.chat.domain.MessageAuthor
import com.softartdev.kvace.feature.chat.presentation.ChatUiState

internal class ChatScreenPreviewProvider : PreviewParameterProvider<ChatUiState> {

    override val values: Sequence<ChatUiState> = sequenceOf(
        selectedState,
        placeholderState,
        sendingState,
    )

    internal companion object {
        private val messages = listOf(
            ChatMessage(1L, MessageAuthor.System, "Kvace is ready.", 0L),
            ChatMessage(2L, MessageAuthor.User, "Draft a migration plan.", 1L),
            ChatMessage(3L, MessageAuthor.Reasoning, "Checking the local context.", 2L),
            ChatMessage(4L, MessageAuthor.Tool, "Reading project files.", 3L),
            ChatMessage(5L, MessageAuthor.Assistant, "The migration plan is ready.", 4L),
            ChatMessage(6L, MessageAuthor.Event, "Completed successfully.", 5L),
        )

        private val selectedConversation = Conversation(
            id = 10L,
            title = "Workspace migration",
            createdAtMillis = 0L,
            updatedAtMillis = 5L,
            messages = messages,
        )

        private val chats = listOf(
            ChatSummary(
                id = 10L,
                title = "Workspace migration",
                lastMessagePreview = "The migration plan is ready.",
                updatedAtMillis = 5L,
                messageCount = 6L,
            ),
            ChatSummary(
                id = 11L,
                title = "Agent setup",
                lastMessagePreview = "Configure Ollama in Settings.",
                updatedAtMillis = 4L,
                messageCount = 3L,
            ),
            ChatSummary(
                id = 12L,
                title = "Release notes",
                lastMessagePreview = null,
                updatedAtMillis = 3L,
                messageCount = 0L,
            ),
        )

        val selectedState = ChatUiState(
            chats = chats,
            selectedConversation = selectedConversation,
            inputText = "Follow up with implementation details",
        )

        val placeholderState = ChatUiState(
            chats = chats,
            selectedConversation = null,
        )

        val sendingState = ChatUiState(
            chats = chats,
            selectedConversation = selectedConversation,
            inputText = "",
            isSending = true,
        )
    }
}
