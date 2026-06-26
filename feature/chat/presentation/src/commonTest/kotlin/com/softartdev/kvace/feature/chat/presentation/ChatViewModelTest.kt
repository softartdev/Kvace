package com.softartdev.kvace.feature.chat.presentation

import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.core.presentation.SnackbarInteractor
import com.softartdev.kvace.core.presentation.SnackbarMessage
import com.softartdev.kvace.core.presentation.SnackbarMessageResource
import com.softartdev.kvace.core.presentation.TextShareInteractor
import com.softartdev.kvace.feature.chat.domain.ChatMessage
import com.softartdev.kvace.feature.chat.domain.ChatRepository
import com.softartdev.kvace.feature.chat.domain.ChatSummary
import com.softartdev.kvace.feature.chat.domain.Conversation
import com.softartdev.kvace.feature.chat.domain.MessageAuthor
import com.softartdev.kvace.feature.chat.domain.MessageSender
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun observesChatHistoryState() = runTest(dispatcher) {
        val repository = FakeChatRepository()
        val viewModel = createViewModel(repository)

        viewModel.observeChats()

        assertEquals(repository.chatSummaries.value, viewModel.uiState.value.chats)
        assertEquals(repository.selectedConversation.value, viewModel.uiState.value.selectedConversation)
    }

    @Test
    fun chatSelectedUpdatesSelectedConversation() = runTest(dispatcher) {
        val repository = FakeChatRepository(
            conversations = listOf(
                Conversation(id = 1L, title = "First"),
                Conversation(id = 2L, title = "Second"),
            ),
        )
        val viewModel = createViewModel(repository)
        viewModel.observeChats()

        viewModel.onAction(ChatAction.ChatSelected(2L))

        assertEquals(2L, viewModel.uiState.value.selectedConversation?.id)
    }

    @Test
    fun newChatClickedCreatesAndSelectsConversation() = runTest(dispatcher) {
        val repository = FakeChatRepository()
        val viewModel = createViewModel(repository)
        viewModel.observeChats()

        viewModel.onAction(ChatAction.NewChatClicked)

        assertEquals(2L, viewModel.uiState.value.selectedConversation?.id)
        assertEquals(2, viewModel.uiState.value.chats.size)
    }

    @Test
    fun inputChangedUpdatesState() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onAction(ChatAction.InputChanged("Hello"))

        assertEquals("Hello", viewModel.uiState.value.inputText)
    }

    @Test
    fun sendClickSendsCurrentInputAndClearsSendingState() = runTest(dispatcher) {
        val repository = FakeChatRepository()
        val sender = FakeMessageSender(repository)
        val viewModel = createViewModel(repository, sender)
        viewModel.observeChats()

        viewModel.onAction(ChatAction.InputChanged("Hello"))
        viewModel.onAction(ChatAction.SendClicked)

        assertEquals(1L, sender.conversationId)
        assertEquals("Hello", sender.text)
        assertEquals("", viewModel.uiState.value.inputText)
        assertFalse(viewModel.uiState.value.isSending)
        assertEquals(MessageAuthor.User, repository.conversation(1L).messages[0].author)
        assertEquals(MessageAuthor.Assistant, repository.conversation(1L).messages[1].author)
    }

    @Test
    fun stopGenerationCancelsCurrentSendAndAppendsEvent() = runTest(dispatcher) {
        val repository = FakeChatRepository()
        val sender = SuspendingMessageSender()
        val viewModel = createViewModel(repository, sender)
        viewModel.observeChats()

        viewModel.onAction(ChatAction.InputChanged("Hello"))
        viewModel.onAction(ChatAction.SendClicked)
        viewModel.onAction(ChatAction.StopGenerationClicked)

        assertTrue(sender.cancelled)
        assertFalse(viewModel.uiState.value.isSending)
        assertEquals(MessageAuthor.Event, repository.conversation(1L).messages.single().author)
        assertEquals("Generation stopped", repository.conversation(1L).messages.single().text)
    }

    @Test
    fun duplicateSendWhileSendingIsIgnored() = runTest(dispatcher) {
        val repository = FakeChatRepository()
        val sender = SuspendingMessageSender()
        val viewModel = createViewModel(repository, sender)
        viewModel.observeChats()

        viewModel.onAction(ChatAction.InputChanged("Hello"))
        viewModel.onAction(ChatAction.SendClicked)
        viewModel.onAction(ChatAction.InputChanged("Again"))
        viewModel.onAction(ChatAction.SendClicked)

        assertEquals(1, sender.invocationCount)

        viewModel.onAction(ChatAction.StopGenerationClicked)
    }

    @Test
    fun messageSharedDelegatesToShareInteractor() = runTest(dispatcher) {
        val repository = FakeChatRepository(
            conversations = listOf(
                Conversation(
                    id = 1L,
                    title = "Test chat",
                    messages = listOf(ChatMessage(1L, MessageAuthor.Assistant, "Share me", 0L)),
                ),
            ),
        )
        val shareInteractor = FakeTextShareInteractor()
        val viewModel = createViewModel(repository = repository, shareInteractor = shareInteractor)
        viewModel.observeChats()

        viewModel.onAction(ChatAction.MessageShared(1L))

        assertEquals("Share me", shareInteractor.sharedText)
    }

    @Test
    fun shareFailureShowsSnackbar() = runTest(dispatcher) {
        val repository = FakeChatRepository(
            conversations = listOf(
                Conversation(
                    id = 1L,
                    title = "Test chat",
                    messages = listOf(ChatMessage(1L, MessageAuthor.Assistant, "Share me", 0L)),
                ),
            ),
        )
        val shareInteractor = FakeTextShareInteractor(shareFailure = IllegalStateException("No share target"))
        val snackbarInteractor = FakeSnackbarInteractor()
        val viewModel = createViewModel(
            repository = repository,
            shareInteractor = shareInteractor,
            snackbarInteractor = snackbarInteractor,
        )
        viewModel.observeChats()

        viewModel.onAction(ChatAction.MessageShared(1L))

        assertEquals(
            SnackbarMessage.Resource(SnackbarMessageResource.ChatShareFailed),
            snackbarInteractor.messages.single(),
        )
    }

    @Test
    fun shareSuccessDoesNotShowSnackbar() = runTest(dispatcher) {
        val repository = FakeChatRepository(
            conversations = listOf(
                Conversation(
                    id = 1L,
                    title = "Test chat",
                    messages = listOf(ChatMessage(1L, MessageAuthor.Assistant, "Share me", 0L)),
                ),
            ),
        )
        val snackbarInteractor = FakeSnackbarInteractor()
        val viewModel = createViewModel(repository = repository, snackbarInteractor = snackbarInteractor)
        viewModel.observeChats()

        viewModel.onAction(ChatAction.MessageShared(1L))

        assertEquals(emptyList(), snackbarInteractor.messages)
    }

    @Test
    fun messageDeletedDelegatesToRepository() = runTest(dispatcher) {
        val repository = FakeChatRepository(
            conversations = listOf(
                Conversation(
                    id = 1L,
                    title = "Test chat",
                    messages = listOf(ChatMessage(1L, MessageAuthor.Assistant, "Delete me", 0L)),
                ),
            ),
        )
        val viewModel = createViewModel(repository)
        viewModel.observeChats()

        viewModel.onAction(ChatAction.MessageDeleted(1L))

        assertEquals(emptyList(), repository.conversation(1L).messages)
    }

    @Test
    fun renameChatDialogFlowRenamesConversation() = runTest(dispatcher) {
        val repository = FakeChatRepository(
            conversations = listOf(Conversation(id = 1L, title = "Old title")),
        )
        val viewModel = createViewModel(repository)
        viewModel.observeChats()

        viewModel.onAction(ChatAction.RenameChatRequested(1L))
        viewModel.onAction(ChatAction.RenameChatInputChanged("New title"))
        viewModel.onAction(ChatAction.RenameChatConfirmed)

        assertEquals("New title", repository.conversation(1L).title)
        assertEquals(null, viewModel.uiState.value.renameDialog)
    }

    @Test
    fun renameChatDismissClearsDialog() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.observeChats()

        viewModel.onAction(ChatAction.RenameChatRequested(1L))
        viewModel.onAction(ChatAction.RenameChatDismissed)

        assertEquals(null, viewModel.uiState.value.renameDialog)
    }

    @Test
    fun deleteChatDialogFlowDeletesConversationAndClearsSelection() = runTest(dispatcher) {
        val repository = FakeChatRepository(
            conversations = listOf(Conversation(id = 1L, title = "Delete me")),
        )
        val viewModel = createViewModel(repository)
        viewModel.observeChats()

        viewModel.onAction(ChatAction.DeleteChatRequested(1L))
        viewModel.onAction(ChatAction.DeleteChatConfirmed)

        assertEquals(null, repository.getConversation(1L))
        assertEquals(null, viewModel.uiState.value.selectedConversation)
        assertEquals(null, viewModel.uiState.value.deleteDialog)
    }

    @Test
    fun deleteChatDismissClearsDialog() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.observeChats()

        viewModel.onAction(ChatAction.DeleteChatRequested(1L))
        viewModel.onAction(ChatAction.DeleteChatDismissed)

        assertEquals(null, viewModel.uiState.value.deleteDialog)
    }

    @Test
    fun sendWithoutSelectedChatIsIgnored() = runTest(dispatcher) {
        val repository = FakeChatRepository(selectedConversationId = null)
        val sender = FakeMessageSender(repository)
        val viewModel = createViewModel(repository, sender)
        viewModel.observeChats()

        viewModel.onAction(ChatAction.InputChanged("Hello"))
        viewModel.onAction(ChatAction.SendClicked)

        assertEquals(null, sender.conversationId)
        assertEquals(emptyList(), repository.conversation(1L).messages)
    }

    @Test
    fun blankInputIsIgnored() = runTest(dispatcher) {
        val repository = FakeChatRepository()
        val sender = FakeMessageSender(repository)
        val viewModel = createViewModel(repository, sender)
        viewModel.observeChats()

        viewModel.onAction(ChatAction.InputChanged("  "))
        viewModel.onAction(ChatAction.SendClicked)

        assertEquals(null, sender.conversationId)
        assertEquals(emptyList(), repository.conversation(1L).messages)
    }

    private fun createViewModel(
        repository: FakeChatRepository = FakeChatRepository(),
        sender: MessageSender = FakeMessageSender(repository),
        shareInteractor: FakeTextShareInteractor = FakeTextShareInteractor(),
        snackbarInteractor: FakeSnackbarInteractor = FakeSnackbarInteractor(),
    ) = ChatViewModel(
        chatRepository = repository,
        sendMessage = sender,
        textShareInteractor = shareInteractor,
        snackbarInteractor = snackbarInteractor,
        dispatchers = ChatTestDispatchers(dispatcher),
    )
}

private class FakeChatRepository(
    conversations: List<Conversation> = listOf(Conversation(id = 1L, title = "Test chat")),
    selectedConversationId: Long? = conversations.firstOrNull()?.id,
) : ChatRepository {
    private var nextConversationId = conversations.maxOfOrNull { it.id }?.plus(1L) ?: 1L
    private var nextMessageId = conversations.flatMap { it.messages }.maxOfOrNull { it.id }?.plus(1L) ?: 1L
    private val conversationsById = conversations.associateBy { it.id }.toMutableMap()

    override val chatSummaries = MutableStateFlow(conversations.map { it.summary() })
    override val selectedConversation = MutableStateFlow(selectedConversationId?.let { conversationsById[it] })

    fun conversation(id: Long): Conversation = requireNotNull(conversationsById[id])

    override suspend fun loadChats() {
        refreshSummaries()
    }

    override suspend fun createConversation(): Conversation {
        val conversation = Conversation(id = nextConversationId++, title = "New chat")
        conversationsById[conversation.id] = conversation
        selectedConversation.value = conversation
        refreshSummaries()
        return conversation
    }

    override suspend fun selectConversation(id: Long) {
        selectedConversation.value = conversationsById[id]
    }

    override suspend fun getConversation(id: Long): Conversation? = conversationsById[id]

    override suspend fun appendMessage(
        conversationId: Long,
        author: MessageAuthor,
        text: String,
        generatedByModelName: String?,
        generatedAtMillis: Long?,
    ): ChatMessage {
        val message = ChatMessage(
            id = nextMessageId++,
            author = author,
            text = text,
            createdAtMillis = 0L,
            generatedByModelName = generatedByModelName,
            generatedAtMillis = generatedAtMillis,
        )
        updateConversation(conversationId) { conversation ->
            conversation.copy(messages = conversation.messages + message)
        }
        return message
    }

    override suspend fun updateMessageText(
        conversationId: Long,
        messageId: Long,
        text: String,
        generatedByModelName: String?,
        generatedAtMillis: Long?,
    ) {
        updateConversation(conversationId) { conversation ->
            conversation.copy(
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

    override suspend fun deleteMessage(conversationId: Long, messageId: Long) {
        updateConversation(conversationId) { conversation ->
            conversation.copy(messages = conversation.messages.filterNot { it.id == messageId })
        }
    }

    override suspend fun renameConversation(conversationId: Long, title: String) {
        updateConversation(conversationId) { conversation ->
            conversation.copy(title = title)
        }
    }

    override suspend fun deleteConversation(conversationId: Long) {
        conversationsById -= conversationId
        if (selectedConversation.value?.id == conversationId) {
            selectedConversation.value = null
        }
        refreshSummaries()
    }

    private fun updateConversation(id: Long, update: (Conversation) -> Conversation) {
        val conversation = update(requireNotNull(conversationsById[id]))
        conversationsById[id] = conversation
        if (selectedConversation.value?.id == id) {
            selectedConversation.value = conversation
        }
        refreshSummaries()
    }

    private fun refreshSummaries() {
        chatSummaries.value = conversationsById.values.map { it.summary() }
    }

    private fun Conversation.summary() = ChatSummary(
        id = id,
        title = title,
        lastMessagePreview = messages.lastOrNull()?.text,
        updatedAtMillis = updatedAtMillis,
        messageCount = messages.size.toLong(),
    )
}

private class FakeTextShareInteractor(
    private val shareFailure: Throwable? = null,
) : TextShareInteractor {
    var sharedText: String? = null
        private set

    override fun shareText(text: String) {
        shareFailure?.let { throw it }
        sharedText = text
    }
}

private class FakeSnackbarInteractor : SnackbarInteractor {
    val messages = mutableListOf<SnackbarMessage>()

    override fun showMessage(message: SnackbarMessage): Job? {
        messages += message
        return null
    }
}

private class FakeMessageSender(
    private val repository: ChatRepository,
) : MessageSender {
    var conversationId: Long? = null
        private set
    var text: String? = null
        private set

    override suspend fun invoke(conversationId: Long, text: String) {
        this.conversationId = conversationId
        this.text = text
        val prompt = text.trim()
        if (prompt.isEmpty()) return

        repository.appendMessage(conversationId, MessageAuthor.User, prompt)
        repository.appendMessage(conversationId, MessageAuthor.Assistant, "Agent response")
    }
}

private class SuspendingMessageSender : MessageSender {
    var invocationCount = 0
        private set
    var cancelled = false
        private set

    override suspend fun invoke(conversationId: Long, text: String) {
        invocationCount++
        try {
            awaitCancellation()
        } finally {
            cancelled = true
        }
    }
}

private class ChatTestDispatchers(
    dispatcher: CoroutineDispatcher,
) : CoroutineDispatchers {
    override val default: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val unconfined: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
}
