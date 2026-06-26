package com.softartdev.kvace.feature.chat.presentation

import com.softartdev.kvace.feature.chat.domain.ChatMessage
import com.softartdev.kvace.feature.chat.domain.ChatSummary
import com.softartdev.kvace.feature.chat.domain.Conversation

data class ChatUiState(
    val chats: List<ChatSummary> = emptyList(),
    val selectedConversation: Conversation? = null,
    val inputText: String = "",
    val isSending: Boolean = false,
    val renameDialog: ChatRenameDialogState? = null,
    val deleteDialog: ChatDeleteDialogState? = null,
) {
    val title: String
        get() = selectedConversation?.title ?: "Chat"

    val messages: List<ChatMessage>
        get() = selectedConversation?.messages.orEmpty()
}

data class ChatRenameDialogState(
    val conversationId: Long,
    val titleInput: String,
)

data class ChatDeleteDialogState(
    val conversationId: Long,
    val title: String,
)

sealed interface ChatAction {
    data class ChatSelected(val id: Long) : ChatAction
    data class InputChanged(val text: String) : ChatAction
    data class MessageDeleted(val messageId: Long) : ChatAction
    data class MessageShared(val messageId: Long) : ChatAction
    data class RenameChatRequested(val id: Long) : ChatAction
    data class RenameChatInputChanged(val title: String) : ChatAction
    data class DeleteChatRequested(val id: Long) : ChatAction
    data object NewChatClicked : ChatAction
    data object SendClicked : ChatAction
    data object StopGenerationClicked : ChatAction
    data object RenameChatConfirmed : ChatAction
    data object RenameChatDismissed : ChatAction
    data object DeleteChatConfirmed : ChatAction
    data object DeleteChatDismissed : ChatAction
}
