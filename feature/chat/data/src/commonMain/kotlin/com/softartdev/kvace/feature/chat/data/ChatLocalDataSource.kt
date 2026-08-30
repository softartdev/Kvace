package com.softartdev.kvace.feature.chat.data

import com.softartdev.kvace.feature.chat.domain.ChatMessage
import com.softartdev.kvace.feature.agent.domain.AgentExecutionError
import com.softartdev.kvace.feature.chat.domain.ChatSummary
import com.softartdev.kvace.feature.chat.domain.Conversation
import com.softartdev.kvace.feature.chat.domain.MessageAuthor

interface ChatLocalDataSource {

    suspend fun loadChatSummaries(): List<ChatSummary>

    suspend fun loadConversation(id: Long): Conversation?

    suspend fun createConversation(title: String, createdAtMillis: Long): Conversation

    suspend fun appendMessage(
        conversationId: Long,
        author: MessageAuthor,
        text: String,
        createdAtMillis: Long,
        generatedByModelName: String?,
        generatedAtMillis: Long?,
        error: AgentExecutionError?,
    ): ChatMessage

    suspend fun updateMessageText(
        conversationId: Long,
        messageId: Long,
        text: String,
        updatedAtMillis: Long,
        generatedByModelName: String?,
        generatedAtMillis: Long?,
    )

    suspend fun deleteMessage(conversationId: Long, messageId: Long, updatedAtMillis: Long)

    suspend fun renameConversation(
        conversationId: Long,
        title: String,
        updatedAtMillis: Long,
        isManual: Boolean,
    )

    suspend fun autoRenameConversationAfterFirstMessage(
        conversationId: Long,
        title: String,
        updatedAtMillis: Long,
    )

    suspend fun deleteConversation(conversationId: Long)
}
