package com.softartdev.kvace.feature.chat.data

import co.touchlab.kermit.Logger
import com.softartdev.kvace.feature.chat.domain.ChatMessage
import com.softartdev.kvace.feature.agent.domain.AgentExecutionError
import com.softartdev.kvace.feature.chat.domain.ChatRepository
import com.softartdev.kvace.feature.chat.domain.ChatSummary
import com.softartdev.kvace.feature.chat.domain.Conversation
import com.softartdev.kvace.feature.chat.domain.MessageAuthor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PersistentChatRepository(
    private val localDataSource: ChatLocalDataSource,
) : ChatRepository {
    private val logger = Logger.withTag("PersistentChatRepository")
    private val writeMutex = Mutex()

    override val chatSummaries: StateFlow<List<ChatSummary>>
        field: MutableStateFlow<List<ChatSummary>> = MutableStateFlow(emptyList())

    override val selectedConversation: StateFlow<Conversation?>
        field: MutableStateFlow<Conversation?> = MutableStateFlow(null)

    override suspend fun loadChats() {
        writeMutex.withLock {
            chatSummaries.value = localDataSource.loadChatSummaries()
            selectedConversation.value = selectedConversation.value?.let { selected ->
                localDataSource.loadConversation(selected.id)
            }
        }
    }

    override suspend fun createConversation(): Conversation = writeMutex.withLock {
        val conversation = localDataSource.createConversation(
            title = DEFAULT_CHAT_TITLE,
            createdAtMillis = currentTimeMillis(),
        )
        chatSummaries.value = localDataSource.loadChatSummaries()
        selectedConversation.value = conversation
        conversation
    }

    override suspend fun selectConversation(id: Long) {
        writeMutex.withLock {
            selectedConversation.value = localDataSource.loadConversation(id)
        }
    }

    override suspend fun getConversation(id: Long): Conversation? = writeMutex.withLock {
        localDataSource.loadConversation(id)
    }

    override suspend fun appendMessage(
        conversationId: Long,
        author: MessageAuthor,
        text: String,
        generatedByModelName: String?,
        generatedAtMillis: Long?,
        error: AgentExecutionError?,
    ): ChatMessage = writeMutex.withLock {
        val createdAtMillis = currentTimeMillis()
        val message = localDataSource.appendMessage(
            conversationId = conversationId,
            author = author,
            text = text,
            createdAtMillis = createdAtMillis,
            generatedByModelName = generatedByModelName,
            generatedAtMillis = generatedAtMillis ?: generatedByModelName?.let { createdAtMillis },
            error = error,
        )
        if (author == MessageAuthor.User) {
            text.toAutomaticChatTitle()
                .takeIf { it.isNotBlank() }
                ?.let { title ->
                    localDataSource.autoRenameConversationAfterFirstMessage(
                        conversationId = conversationId,
                        title = title,
                        updatedAtMillis = createdAtMillis,
                    )
                }
        }
        refreshAfterConversationWrite(conversationId)
        message
    }

    override suspend fun updateMessageText(
        conversationId: Long,
        messageId: Long,
        text: String,
        generatedByModelName: String?,
        generatedAtMillis: Long?,
    ) {
        writeMutex.withLock {
            val updatedAtMillis = currentTimeMillis()
            localDataSource.updateMessageText(
                conversationId = conversationId,
                messageId = messageId,
                text = text,
                updatedAtMillis = updatedAtMillis,
                generatedByModelName = generatedByModelName,
                generatedAtMillis = generatedAtMillis ?: generatedByModelName?.let { updatedAtMillis },
            )
            refreshAfterConversationWrite(conversationId)
        }
    }

    override suspend fun deleteMessage(conversationId: Long, messageId: Long) {
        writeMutex.withLock {
            localDataSource.deleteMessage(
                conversationId = conversationId,
                messageId = messageId,
                updatedAtMillis = currentTimeMillis(),
            )
            refreshAfterConversationWrite(conversationId)
        }
    }

    override suspend fun renameConversation(conversationId: Long, title: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) return
        writeMutex.withLock {
            localDataSource.renameConversation(
                conversationId = conversationId,
                title = trimmedTitle,
                updatedAtMillis = currentTimeMillis(),
                isManual = true,
            )
            refreshAfterConversationWrite(conversationId)
        }
    }

    override suspend fun deleteConversation(conversationId: Long) {
        writeMutex.withLock {
            localDataSource.deleteConversation(conversationId)
            chatSummaries.value = localDataSource.loadChatSummaries()
            if (selectedConversation.value?.id == conversationId) {
                selectedConversation.value = null
            }
        }
    }

    private suspend fun refreshAfterConversationWrite(conversationId: Long) {
        chatSummaries.value = localDataSource.loadChatSummaries()
        if (selectedConversation.value?.id == conversationId) {
            selectedConversation.value = localDataSource.loadConversation(conversationId)
        } else {
            logger.d { "Wrote to unselected chat $conversationId" }
        }
    }

    private companion object {
        const val DEFAULT_CHAT_TITLE = "New chat"
    }
}
