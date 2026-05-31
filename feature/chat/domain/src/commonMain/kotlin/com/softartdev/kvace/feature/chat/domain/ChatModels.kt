package com.softartdev.kvace.feature.chat.domain

import kotlinx.coroutines.flow.StateFlow

enum class MessageAuthor {
    User,
    Assistant,
    Reasoning,
    Tool,
    Event,
    System,
    Error,
}

data class ChatMessage(
    val id: Long,
    val author: MessageAuthor,
    val text: String,
    val createdAtMillis: Long,
)

data class Conversation(
    val id: Long,
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
)

interface ChatRepository {
    val conversation: StateFlow<Conversation>
    suspend fun appendMessage(author: MessageAuthor, text: String): ChatMessage
    suspend fun updateMessageText(messageId: Long, text: String)
    suspend fun replaceConversation(conversation: Conversation)
}
