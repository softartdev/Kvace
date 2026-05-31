package com.softartdev.kvace.feature.chat.presentation

import com.softartdev.kvace.feature.chat.domain.ChatMessage

data class ChatUiState(
    val title: String = "New chat",
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
)

sealed interface ChatAction {
    data class InputChanged(val text: String) : ChatAction
    data object SendClicked : ChatAction
}
