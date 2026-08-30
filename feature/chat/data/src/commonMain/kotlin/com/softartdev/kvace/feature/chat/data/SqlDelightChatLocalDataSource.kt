package com.softartdev.kvace.feature.chat.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.softartdev.kvace.feature.chat.data.local.ChatDatabase
import com.softartdev.kvace.feature.chat.data.local.ChatDatabaseDriverFactory
import com.softartdev.kvace.feature.chat.domain.ChatMessage
import com.softartdev.kvace.feature.agent.domain.AgentExecutionError
import com.softartdev.kvace.feature.chat.domain.ChatSummary
import com.softartdev.kvace.feature.chat.domain.Conversation
import com.softartdev.kvace.feature.chat.domain.MessageAuthor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SqlDelightChatLocalDataSource(
    private val driverFactory: ChatDatabaseDriverFactory,
) : ChatLocalDataSource {
    private val databaseMutex = Mutex()
    private var database: ChatDatabase? = null

    override suspend fun loadChatSummaries(): List<ChatSummary> =
        database().chatQueries.selectChatSummaries().awaitAsList().map { it.toDomain() }

    override suspend fun loadConversation(id: Long): Conversation? {
        val queries = database().chatQueries
        val chat = queries.selectChatById(id).awaitAsOneOrNull() ?: return null
        val messages = queries.selectMessagesByChatId(id).awaitAsList().map { it.toDomain() }
        return chat.toDomain(messages)
    }

    override suspend fun createConversation(title: String, createdAtMillis: Long): Conversation {
        val database = database()
        return database.transactionWithResult {
            database.chatQueries.insertChat(
                title = title,
                title_is_manual = 0L,
                created_at_millis = createdAtMillis,
                updated_at_millis = createdAtMillis,
            )
            val id = database.chatQueries.selectLastInsertRowId().awaitAsOne()
            requireNotNull(loadConversation(id))
        }
    }

    override suspend fun appendMessage(
        conversationId: Long,
        author: MessageAuthor,
        text: String,
        createdAtMillis: Long,
        generatedByModelName: String?,
        generatedAtMillis: Long?,
        error: AgentExecutionError?,
    ): ChatMessage {
        val database = database()
        return database.transactionWithResult {
            database.chatQueries.insertMessage(
                chat_id = conversationId,
                author = author.name,
                text = text,
                created_at_millis = createdAtMillis,
                generated_by_model_name = generatedByModelName,
                generated_at_millis = generatedAtMillis,
                error_type = error?.toStorageValue(),
            )
            database.chatQueries.touchChat(
                updated_at_millis = createdAtMillis,
                id = conversationId,
            )
            val id = database.chatQueries.selectLastInsertRowId().awaitAsOne()
            ChatMessage(
                id = id,
                author = author,
                text = text,
                createdAtMillis = createdAtMillis,
                generatedByModelName = generatedByModelName,
                generatedAtMillis = generatedAtMillis,
                error = error,
            )
        }
    }

    override suspend fun updateMessageText(
        conversationId: Long,
        messageId: Long,
        text: String,
        updatedAtMillis: Long,
        generatedByModelName: String?,
        generatedAtMillis: Long?,
    ) {
        val database = database()
        database.transaction {
            database.chatQueries.updateMessageText(
                text = text,
                generated_by_model_name = generatedByModelName,
                generated_at_millis = generatedAtMillis,
                id = messageId,
                chat_id = conversationId,
            )
            database.chatQueries.touchChat(
                updated_at_millis = updatedAtMillis,
                id = conversationId,
            )
        }
    }

    override suspend fun deleteMessage(conversationId: Long, messageId: Long, updatedAtMillis: Long) {
        val database = database()
        database.transaction {
            database.chatQueries.deleteMessage(
                id = messageId,
                chat_id = conversationId,
            )
            database.chatQueries.touchChat(
                updated_at_millis = updatedAtMillis,
                id = conversationId,
            )
        }
    }

    override suspend fun renameConversation(
        conversationId: Long,
        title: String,
        updatedAtMillis: Long,
        isManual: Boolean,
    ) {
        database().chatQueries.updateChatTitle(
            title = title,
            title_is_manual = isManual.toLong(),
            updated_at_millis = updatedAtMillis,
            id = conversationId,
        )
    }

    override suspend fun autoRenameConversationAfterFirstMessage(
        conversationId: Long,
        title: String,
        updatedAtMillis: Long,
    ) {
        database().chatQueries.autoUpdateChatTitleAfterFirstMessage(
            title = title,
            updated_at_millis = updatedAtMillis,
            id = conversationId,
            chat_id = conversationId,
        )
    }

    override suspend fun deleteConversation(conversationId: Long) {
        database().chatQueries.deleteChat(id = conversationId)
    }

    private suspend fun database(): ChatDatabase {
        database?.let { return it }
        return databaseMutex.withLock {
            database ?: ChatDatabase(driverFactory.createDriver()).also { database = it }
        }
    }

    private fun Boolean.toLong(): Long = if (this) 1L else 0L
}
