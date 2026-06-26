package com.softartdev.kvace.feature.chat.data

import com.softartdev.kvace.feature.chat.domain.ChatMessage
import com.softartdev.kvace.feature.chat.domain.ChatSummary
import com.softartdev.kvace.feature.chat.domain.Conversation
import com.softartdev.kvace.feature.chat.domain.MessageAuthor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersistentChatRepositoryTest {

    @Test
    fun createConversationPersistsSummaryAndSelection() = runTest {
        val localDataSource = FakeChatLocalDataSource()
        val repository = PersistentChatRepository(localDataSource)

        val conversation = repository.createConversation()

        assertEquals(conversation, repository.selectedConversation.value)
        assertEquals(listOf(conversation.id), repository.chatSummaries.value.map { it.id })
        assertEquals(conversation, localDataSource.loadConversation(conversation.id))
    }

    @Test
    fun loadChatsRefreshesSummariesAndSelectedConversation() = runTest {
        val localDataSource = FakeChatLocalDataSource()
        val conversation = localDataSource.createConversation("Saved", 0L)
        val repository = PersistentChatRepository(localDataSource)
        repository.selectConversation(conversation.id)

        localDataSource.appendMessage(conversation.id, MessageAuthor.User, "Persisted", 1L, null, null)
        repository.loadChats()

        assertEquals("Persisted", repository.chatSummaries.value.single().lastMessagePreview)
        assertEquals("Persisted", repository.selectedConversation.value?.messages?.single()?.text)
    }

    @Test
    fun selectConversationPublishesRequestedDetail() = runTest {
        val localDataSource = FakeChatLocalDataSource()
        val first = localDataSource.createConversation("First", 0L)
        val second = localDataSource.createConversation("Second", 1L)
        val repository = PersistentChatRepository(localDataSource)

        repository.selectConversation(second.id)

        assertEquals(second.id, repository.selectedConversation.value?.id)
        assertEquals(first.id, localDataSource.loadConversation(first.id)?.id)
    }

    @Test
    fun appendAndUpdateRefreshSelectedConversationAfterWrite() = runTest {
        val localDataSource = FakeChatLocalDataSource()
        val conversation = localDataSource.createConversation("Chat", 0L)
        val repository = PersistentChatRepository(localDataSource)
        repository.selectConversation(conversation.id)

        val message = repository.appendMessage(
            conversationId = conversation.id,
            author = MessageAuthor.Assistant,
            text = "Hel",
            generatedByModelName = "qwen3.5:0.8b",
        )
        repository.updateMessageText(
            conversationId = conversation.id,
            messageId = message.id,
            text = "Hello",
            generatedByModelName = "qwen3.5:0.8b",
        )

        assertEquals("Hello", repository.selectedConversation.value?.messages?.single()?.text)
        assertEquals("qwen3.5:0.8b", repository.selectedConversation.value?.messages?.single()?.generatedByModelName)
        assertEquals("Hello", repository.chatSummaries.value.single().lastMessagePreview)
    }

    @Test
    fun deleteMessageRefreshesSelectedConversationAfterWrite() = runTest {
        val localDataSource = FakeChatLocalDataSource()
        val conversation = localDataSource.createConversation("Chat", 0L)
        val repository = PersistentChatRepository(localDataSource)
        repository.selectConversation(conversation.id)
        val message = repository.appendMessage(conversation.id, MessageAuthor.User, "Remove me")

        repository.deleteMessage(conversation.id, message.id)

        assertEquals(emptyList(), repository.selectedConversation.value?.messages)
        assertEquals(null, repository.chatSummaries.value.single().lastMessagePreview)
    }

    @Test
    fun firstUserMessageAutomaticallyRenamesNonManualNewChat() = runTest {
        val localDataSource = FakeChatLocalDataSource()
        val conversation = localDataSource.createConversation("New chat", 0L)
        val repository = PersistentChatRepository(localDataSource)
        repository.selectConversation(conversation.id)

        repository.appendMessage(conversation.id, MessageAuthor.User, "This is the first prompt in a new chat")

        assertEquals("This is the first prompt in a new chat", repository.selectedConversation.value?.title)
    }

    @Test
    fun manualRenamePreventsAutomaticRename() = runTest {
        val localDataSource = FakeChatLocalDataSource()
        val conversation = localDataSource.createConversation("New chat", 0L)
        val repository = PersistentChatRepository(localDataSource)
        repository.selectConversation(conversation.id)

        repository.renameConversation(conversation.id, "Manual title")
        repository.appendMessage(conversation.id, MessageAuthor.User, "First prompt")

        assertEquals("Manual title", repository.selectedConversation.value?.title)
    }

    @Test
    fun deleteConversationRefreshesSummariesAndClearsSelection() = runTest {
        val localDataSource = FakeChatLocalDataSource()
        val conversation = localDataSource.createConversation("Chat", 0L)
        val repository = PersistentChatRepository(localDataSource)
        repository.selectConversation(conversation.id)

        repository.deleteConversation(conversation.id)

        assertEquals(emptyList(), repository.chatSummaries.value)
        assertNull(repository.selectedConversation.value)
    }

    @Test
    fun selectingMissingConversationClearsSelection() = runTest {
        val repository = PersistentChatRepository(FakeChatLocalDataSource())

        repository.selectConversation(404L)

        assertNull(repository.selectedConversation.value)
    }
}

private class FakeChatLocalDataSource : ChatLocalDataSource {
    private var nextConversationId = 1L
    private var nextMessageId = 1L
    private val conversations = mutableMapOf<Long, Conversation>()
    private val manualTitleConversationIds = mutableSetOf<Long>()

    override suspend fun loadChatSummaries(): List<ChatSummary> =
        conversations.values
            .sortedByDescending { it.updatedAtMillis }
            .map { it.summary() }

    override suspend fun loadConversation(id: Long): Conversation? = conversations[id]

    override suspend fun createConversation(title: String, createdAtMillis: Long): Conversation {
        val conversation = Conversation(
            id = nextConversationId++,
            title = title,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = createdAtMillis,
        )
        conversations[conversation.id] = conversation
        return conversation
    }

    override suspend fun appendMessage(
        conversationId: Long,
        author: MessageAuthor,
        text: String,
        createdAtMillis: Long,
        generatedByModelName: String?,
        generatedAtMillis: Long?,
    ): ChatMessage {
        val message = ChatMessage(
            id = nextMessageId++,
            author = author,
            text = text,
            createdAtMillis = createdAtMillis,
            generatedByModelName = generatedByModelName,
            generatedAtMillis = generatedAtMillis,
        )
        updateConversation(conversationId) { conversation ->
            conversation.copy(
                updatedAtMillis = createdAtMillis,
                messages = conversation.messages + message,
            )
        }
        return message
    }

    override suspend fun updateMessageText(
        conversationId: Long,
        messageId: Long,
        text: String,
        updatedAtMillis: Long,
        generatedByModelName: String?,
        generatedAtMillis: Long?,
    ) {
        updateConversation(conversationId) { conversation ->
            conversation.copy(
                updatedAtMillis = updatedAtMillis,
                messages = conversation.messages.map { message ->
                    if (message.id == messageId) {
                        message.copy(
                            text = text,
                            generatedByModelName = generatedByModelName,
                            generatedAtMillis = generatedAtMillis,
                        )
                    } else {
                        message
                    }
                },
            )
        }
    }

    override suspend fun deleteMessage(conversationId: Long, messageId: Long, updatedAtMillis: Long) {
        updateConversation(conversationId) { conversation ->
            conversation.copy(
                updatedAtMillis = updatedAtMillis,
                messages = conversation.messages.filterNot { it.id == messageId },
            )
        }
    }

    override suspend fun renameConversation(
        conversationId: Long,
        title: String,
        updatedAtMillis: Long,
        isManual: Boolean,
    ) {
        if (isManual) {
            manualTitleConversationIds += conversationId
        } else {
            manualTitleConversationIds -= conversationId
        }
        updateConversation(conversationId) { conversation ->
            conversation.copy(title = title, updatedAtMillis = updatedAtMillis)
        }
    }

    override suspend fun autoRenameConversationAfterFirstMessage(
        conversationId: Long,
        title: String,
        updatedAtMillis: Long,
    ) {
        val conversation = requireNotNull(conversations[conversationId])
        if (conversationId !in manualTitleConversationIds && conversation.messages.size == 1) {
            updateConversation(conversationId) {
                it.copy(title = title, updatedAtMillis = updatedAtMillis)
            }
        }
    }

    override suspend fun deleteConversation(conversationId: Long) {
        conversations -= conversationId
        manualTitleConversationIds -= conversationId
    }

    private fun updateConversation(id: Long, update: (Conversation) -> Conversation) {
        conversations[id] = update(requireNotNull(conversations[id]))
    }

    private fun Conversation.summary() = ChatSummary(
        id = id,
        title = title,
        lastMessagePreview = messages.lastOrNull()?.text,
        updatedAtMillis = updatedAtMillis,
        messageCount = messages.size.toLong(),
    )
}
