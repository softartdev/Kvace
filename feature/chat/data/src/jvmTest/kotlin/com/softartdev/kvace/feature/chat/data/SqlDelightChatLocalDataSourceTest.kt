package com.softartdev.kvace.feature.chat.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.softartdev.kvace.feature.chat.data.local.ChatDatabase
import com.softartdev.kvace.feature.chat.data.local.ChatDatabaseDriverFactory
import com.softartdev.kvace.feature.chat.domain.MessageAuthor
import kotlinx.coroutines.test.runTest
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlDelightChatLocalDataSourceTest {

    @Test
    fun createLoadAndSelectConversationFromDatabase() = runTest {
        val dataSource = createDataSource()

        val conversation = dataSource.createConversation("Database chat", 10L)
        val summaries = dataSource.loadChatSummaries()
        val loaded = dataSource.loadConversation(conversation.id)

        assertEquals(listOf(conversation.id), summaries.map { it.id })
        assertEquals("Database chat", loaded?.title)
        assertEquals(10L, loaded?.createdAtMillis)
        assertEquals(10L, loaded?.updatedAtMillis)
    }

    @Test
    fun appendAndUpdateMessagesPersistInCreatedOrder() = runTest {
        val dataSource = createDataSource()
        val conversation = dataSource.createConversation("Ordering", 0L)

        val later = dataSource.appendMessage(conversation.id, MessageAuthor.User, "Later", 20L, null, null)
        val earlier = dataSource.appendMessage(
            conversationId = conversation.id,
            author = MessageAuthor.Assistant,
            text = "Earlier",
            createdAtMillis = 10L,
            generatedByModelName = "qwen3.5:0.8b",
            generatedAtMillis = 10L,
        )
        dataSource.updateMessageText(
            conversationId = conversation.id,
            messageId = later.id,
            text = "Updated later",
            updatedAtMillis = 30L,
            generatedByModelName = null,
            generatedAtMillis = null,
        )
        val loaded = requireNotNull(dataSource.loadConversation(conversation.id))

        assertEquals(listOf(earlier.id, later.id), loaded.messages.map { it.id })
        assertEquals(listOf("Earlier", "Updated later"), loaded.messages.map { it.text })
        assertEquals("qwen3.5:0.8b", loaded.messages.first().generatedByModelName)
        assertEquals(10L, loaded.messages.first().generatedAtMillis)
        assertEquals(30L, loaded.updatedAtMillis)
    }

    @Test
    fun summariesUseLastMessagePreviewAndDescendingUpdatedTime() = runTest {
        val dataSource = createDataSource()
        val older = dataSource.createConversation("Older", 1L)
        val newer = dataSource.createConversation("Newer", 2L)

        dataSource.appendMessage(older.id, MessageAuthor.User, "Older message", 3L, null, null)
        dataSource.appendMessage(newer.id, MessageAuthor.User, "Newer message", 4L, null, null)
        val summaries = dataSource.loadChatSummaries()

        assertEquals(listOf(newer.id, older.id), summaries.map { it.id })
        assertEquals("Newer message", summaries.first().lastMessagePreview)
        assertEquals(1L, summaries.first().messageCount)
    }

    @Test
    fun missingConversationReturnsNull() = runTest {
        val dataSource = createDataSource()

        assertNull(dataSource.loadConversation(404L))
    }

    @Test
    fun deleteMessageRemovesMessageAndUpdatesConversationTime() = runTest {
        val dataSource = createDataSource()
        val conversation = dataSource.createConversation("Delete", 0L)
        val message = dataSource.appendMessage(conversation.id, MessageAuthor.User, "Remove", 1L, null, null)

        dataSource.deleteMessage(conversation.id, message.id, 2L)

        val loaded = requireNotNull(dataSource.loadConversation(conversation.id))
        assertEquals(emptyList(), loaded.messages)
        assertEquals(2L, loaded.updatedAtMillis)
    }

    @Test
    fun autoRenameAppliesOnlyAfterFirstMessageAndBeforeManualRename() = runTest {
        val dataSource = createDataSource()
        val conversation = dataSource.createConversation("New chat", 0L)

        dataSource.appendMessage(conversation.id, MessageAuthor.User, "First prompt", 1L, null, null)
        dataSource.autoRenameConversationAfterFirstMessage(conversation.id, "First prompt", 1L)
        dataSource.renameConversation(conversation.id, "Manual", 2L, isManual = true)
        dataSource.appendMessage(conversation.id, MessageAuthor.User, "Second prompt", 3L, null, null)
        dataSource.autoRenameConversationAfterFirstMessage(conversation.id, "Second prompt", 3L)

        val loaded = requireNotNull(dataSource.loadConversation(conversation.id))
        assertEquals("Manual", loaded.title)
    }

    @Test
    fun deleteConversationCascadesMessagesAndRemovesSummary() = runTest {
        val dataSource = createDataSource()
        val conversation = dataSource.createConversation("Delete", 0L)
        dataSource.appendMessage(conversation.id, MessageAuthor.User, "Message", 1L, null, null)

        dataSource.deleteConversation(conversation.id)

        assertNull(dataSource.loadConversation(conversation.id))
        assertEquals(emptyList(), dataSource.loadChatSummaries())
    }

    private fun createDataSource(): SqlDelightChatLocalDataSource =
        SqlDelightChatLocalDataSource(
            object : ChatDatabaseDriverFactory {
                override suspend fun createDriver(): SqlDriver =
                    JdbcSqliteDriver(
                        url = JdbcSqliteDriver.IN_MEMORY,
                        properties = Properties(),
                        schema = ChatDatabase.Schema.synchronous(),
                    )
            },
        )
}
