package com.softartdev.kvace.feature.chat.presentation

import co.touchlab.kermit.Logger
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.feature.chat.domain.ChatMessage
import com.softartdev.kvace.feature.chat.domain.ChatRepository
import com.softartdev.kvace.feature.chat.domain.Conversation
import com.softartdev.kvace.feature.chat.domain.MessageSender
import com.softartdev.kvace.feature.chat.domain.MessageAuthor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    fun observesConversationState() = runTest(dispatcher) {
        val repository = FakeChatRepository()
        val viewModel = createViewModel(repository)

        viewModel.observeConversation()

        assertEquals("Test chat", viewModel.uiState.value.title)
        assertEquals(repository.conversation.value.messages, viewModel.uiState.value.messages)
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
        val viewModel = createViewModel(repository)

        viewModel.onAction(ChatAction.InputChanged("Hello"))
        viewModel.onAction(ChatAction.SendClicked)

        assertEquals("", viewModel.uiState.value.inputText)
        assertFalse(viewModel.uiState.value.isSending)
        assertEquals(MessageAuthor.User, repository.conversation.value.messages[0].author)
        assertEquals(MessageAuthor.Assistant, repository.conversation.value.messages[1].author)
    }

    @Test
    fun blankInputIsIgnored() = runTest(dispatcher) {
        val repository = FakeChatRepository()
        val viewModel = createViewModel(repository)

        viewModel.onAction(ChatAction.InputChanged("  "))
        viewModel.onAction(ChatAction.SendClicked)

        assertEquals(emptyList(), repository.conversation.value.messages)
    }

    private fun createViewModel(
        repository: FakeChatRepository = FakeChatRepository(),
    ) = ChatViewModel(
        chatRepository = repository,
        sendMessage = FakeMessageSender(repository),
        dispatchers = ChatTestDispatchers(dispatcher),
        logger = Logger.withTag("ChatViewModelTest"),
    )
}

private class FakeChatRepository : ChatRepository {
    private var nextMessageId = 1L
    override val conversation = MutableStateFlow(Conversation(id = 1L, title = "Test chat"))

    override suspend fun appendMessage(author: MessageAuthor, text: String): ChatMessage {
        val message = ChatMessage(
            id = nextMessageId++,
            author = author,
            text = text,
            createdAtMillis = 0L,
        )
        conversation.value = conversation.value.copy(messages = conversation.value.messages + message)
        return message
    }

    override suspend fun updateMessageText(messageId: Long, text: String) {
        conversation.value = conversation.value.copy(
            messages = conversation.value.messages.map { message ->
                if (message.id == messageId) {
                    message.copy(text = text)
                } else {
                    message
                }
            },
        )
    }

    override suspend fun replaceConversation(conversation: Conversation) {
        this.conversation.value = conversation
    }
}

private class FakeMessageSender(
    private val repository: ChatRepository,
) : MessageSender {
    override suspend fun invoke(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty()) return

        repository.appendMessage(MessageAuthor.User, prompt)
        repository.appendMessage(MessageAuthor.Assistant, "Agent response")
    }
}

private class ChatTestDispatchers(
    private val dispatcher: CoroutineDispatcher,
) : CoroutineDispatchers {
    override val default: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val unconfined: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
}
