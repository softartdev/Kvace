package com.softartdev.kvace.feature.chat.domain

import com.softartdev.kvace.feature.agent.domain.AgentExecutionError
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
    val generatedByModelName: String? = null,
    val generatedAtMillis: Long? = null,
    val error: AgentExecutionError? = null,
) {
    constructor(
        id: Long,
        author: MessageAuthor,
        text: String,
        createdAtMillis: Long,
    ) : this(
        id = id,
        author = author,
        text = text,
        createdAtMillis = createdAtMillis,
        generatedByModelName = null,
        generatedAtMillis = null,
        error = null,
    )
}

data class ChatSummary(
    val id: Long,
    val title: String,
    val lastMessagePreview: String?,
    val updatedAtMillis: Long,
    val messageCount: Long,
    val lastMessageError: AgentExecutionError? = null,
)

data class Conversation(
    val id: Long,
    val title: String,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = createdAtMillis,
    val messages: List<ChatMessage> = emptyList(),
)

interface ChatRepository {
    val chatSummaries: StateFlow<List<ChatSummary>>
    val selectedConversation: StateFlow<Conversation?>
    suspend fun loadChats()
    suspend fun createConversation(): Conversation
    suspend fun selectConversation(id: Long)
    suspend fun getConversation(id: Long): Conversation?
    suspend fun appendMessage(
        conversationId: Long,
        author: MessageAuthor,
        text: String,
        generatedByModelName: String? = null,
        generatedAtMillis: Long? = null,
        error: AgentExecutionError? = null,
    ): ChatMessage

    suspend fun updateMessageText(
        conversationId: Long,
        messageId: Long,
        text: String,
        generatedByModelName: String? = null,
        generatedAtMillis: Long? = null,
    )

    suspend fun deleteMessage(conversationId: Long, messageId: Long)
    suspend fun renameConversation(conversationId: Long, title: String)
    suspend fun deleteConversation(conversationId: Long)
}
