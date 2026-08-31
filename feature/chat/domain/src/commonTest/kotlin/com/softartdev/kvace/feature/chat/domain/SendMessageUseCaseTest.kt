package com.softartdev.kvace.feature.chat.domain

import com.softartdev.kvace.feature.agent.domain.AgentConversationMessage
import com.softartdev.kvace.feature.agent.domain.AgentConversationRole
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentExecutionEvent
import com.softartdev.kvace.feature.agent.domain.AgentExecutionError
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.domain.AgentRequest
import com.softartdev.kvace.feature.agent.domain.AgentRuntime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SendMessageUseCaseTest {

    @Test
    fun appendsUserAndAssistantMessagesToSelectedChat() = runTest {
        val repository = FakeChatRepository(
            conversations = listOf(
                Conversation(id = 1L, title = "First"),
                Conversation(id = 2L, title = "Second"),
            ),
        )
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = FakeAgentRuntime(AgentExecutionEvent.AssistantMessage("Hello from agent")),
            agentConfigurationRepository = FakeAgentConfigurationRepository(),
        )

        useCase(2L, "Hello")

        assertEquals(emptyList(), repository.conversation(1L).messages)
        assertEquals(MessageAuthor.User, repository.conversation(2L).messages[0].author)
        assertEquals(MessageAuthor.Assistant, repository.conversation(2L).messages[1].author)
    }

    @Test
    fun ignoresBlankMessages() = runTest {
        val repository = FakeChatRepository()
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = FakeAgentRuntime(AgentExecutionEvent.AssistantMessage("Ignored")),
            agentConfigurationRepository = FakeAgentConfigurationRepository(),
        )

        useCase(1L, "   ")

        assertEquals(emptyList(), repository.conversation(1L).messages)
    }

    @Test
    fun appendsToolCallAsToolMessage() = runTest {
        val repository = FakeChatRepository()
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = FakeAgentRuntime(AgentExecutionEvent.ToolCall("Tool output")),
            agentConfigurationRepository = FakeAgentConfigurationRepository(),
        )

        useCase(1L, "Run tool")

        assertEquals(MessageAuthor.Tool, repository.conversation(1L).messages[1].author)
        assertEquals("Tool output", repository.conversation(1L).messages[1].text)
    }

    @Test
    fun appendsRuntimeErrorAsErrorMessage() = runTest {
        val repository = FakeChatRepository()
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = FakeAgentRuntime(AgentExecutionEvent.Error(AgentExecutionError.RequestFailed("Provider failed"))),
            agentConfigurationRepository = FakeAgentConfigurationRepository(),
        )

        useCase(1L, "Hello")

        assertEquals(MessageAuthor.Error, repository.conversation(1L).messages[1].author)
        assertEquals("", repository.conversation(1L).messages[1].text)
        assertEquals(
            AgentExecutionError.RequestFailed("Provider failed"),
            repository.conversation(1L).messages[1].error,
        )
    }

    @Test
    fun updatesSingleAssistantMessageFromStreamingDeltas() = runTest {
        val repository = FakeChatRepository()
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = object : AgentRuntime {
                override fun execute(request: AgentRequest): Flow<AgentExecutionEvent> = flow {
                    emit(AgentExecutionEvent.AssistantMessageDelta("Hel"))
                    emit(AgentExecutionEvent.AssistantMessageDelta("lo"))
                    emit(AgentExecutionEvent.AssistantMessage("Hello"))
                }
            },
            agentConfigurationRepository = FakeAgentConfigurationRepository(),
        )

        useCase(1L, "Hello")

        val messages = repository.conversation(1L).messages
        assertEquals(2, messages.size)
        assertEquals(MessageAuthor.Assistant, messages[1].author)
        assertEquals("Hello", messages[1].text)
    }

    @Test
    fun updatesSingleReasoningMessageFromStreamingDeltas() = runTest {
        val repository = FakeChatRepository()
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = object : AgentRuntime {
                override fun execute(request: AgentRequest): Flow<AgentExecutionEvent> = flow {
                    emit(AgentExecutionEvent.ReasoningDelta("Check"))
                    emit(AgentExecutionEvent.ReasoningDelta(" context"))
                    emit(AgentExecutionEvent.ReasoningMessage("Check context"))
                }
            },
            agentConfigurationRepository = FakeAgentConfigurationRepository(),
        )

        useCase(1L, "Think")

        val messages = repository.conversation(1L).messages
        assertEquals(2, messages.size)
        assertEquals(MessageAuthor.Reasoning, messages[1].author)
        assertEquals("Check context", messages[1].text)
    }

    @Test
    fun updatesSingleToolMessageFromStreamingDeltas() = runTest {
        val repository = FakeChatRepository()
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = object : AgentRuntime {
                override fun execute(request: AgentRequest): Flow<AgentExecutionEvent> = flow {
                    emit(AgentExecutionEvent.ToolCallDelta("ollama.generate"))
                    emit(AgentExecutionEvent.ToolCallDelta("\n{}"))
                    emit(AgentExecutionEvent.ToolCall("ollama.generate\n{}"))
                }
            },
            agentConfigurationRepository = FakeAgentConfigurationRepository(),
        )

        useCase(1L, "Use a tool")

        val messages = repository.conversation(1L).messages
        assertEquals(2, messages.size)
        assertEquals(MessageAuthor.Tool, messages[1].author)
        assertEquals("ollama.generate\n{}", messages[1].text)
    }

    @Test
    fun appendsStreamFinishReasonAsEventMessage() = runTest {
        val repository = FakeChatRepository()
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = FakeAgentRuntime(AgentExecutionEvent.StreamFinished("stop")),
            agentConfigurationRepository = FakeAgentConfigurationRepository(),
        )

        useCase(1L, "Hello")

        val messages = repository.conversation(1L).messages
        assertEquals(MessageAuthor.Event, messages[1].author)
        assertEquals("stop", messages[1].text)
    }

    @Test
    fun ignoresBlankStreamFinishReason() = runTest {
        val repository = FakeChatRepository()
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = FakeAgentRuntime(AgentExecutionEvent.StreamFinished("")),
            agentConfigurationRepository = FakeAgentConfigurationRepository(),
        )

        useCase(1L, "Hello")

        assertEquals(1, repository.conversation(1L).messages.size)
    }

    @Test
    fun passesPreviousUserAndAssistantMessagesAsContext() = runTest {
        val repository = FakeChatRepository(
            conversations = listOf(
                Conversation(
                    id = 1L,
                    title = "Context",
                    messages = listOf(
                        ChatMessage(1L, MessageAuthor.System, "Ready", 0L),
                        ChatMessage(2L, MessageAuthor.User, "First question", 1L),
                        ChatMessage(3L, MessageAuthor.Assistant, "First answer", 2L),
                        ChatMessage(4L, MessageAuthor.Tool, "Tool trace", 3L),
                    ),
                ),
            ),
        )
        val runtime = CapturingAgentRuntime(AgentExecutionEvent.AssistantMessage("Next answer"))
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = runtime,
            agentConfigurationRepository = FakeAgentConfigurationRepository(),
        )

        useCase(1L, "Continue")

        assertEquals(
            listOf(
                AgentConversationMessage(AgentConversationRole.User, "First question"),
                AgentConversationMessage(AgentConversationRole.Assistant, "First answer"),
            ),
            runtime.request.context,
        )
    }

    @Test
    fun locksSelectedProviderAndStoresGeneratedModelMetadata() = runTest {
        val repository = FakeChatRepository()
        val runtime = CapturingAgentRuntime(AgentExecutionEvent.AssistantMessage("Answer"))
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = runtime,
            agentConfigurationRepository = FakeAgentConfigurationRepository(
                selectedProviderConfig = AgentProviderConfig(
                    id = AgentProviderId.Ollama,
                    modelName = "qwen3.5:0.8b",
                    endpoint = "http://127.0.0.1:11434",
                    isConfigured = true,
                ),
            ),
        )

        useCase(1L, "Hello")

        assertEquals(AgentProviderId.Ollama, runtime.request.providerId)
        val assistantMessage = repository.conversation(1L).messages[1]
        assertEquals("qwen3.5:0.8b", assistantMessage.generatedByModelName)
        assertEquals(0L, assistantMessage.generatedAtMillis)
    }
}

private class FakeAgentRuntime(private val event: AgentExecutionEvent) : AgentRuntime {
    override fun execute(request: AgentRequest): Flow<AgentExecutionEvent> = flowOf(event)
}

private class CapturingAgentRuntime(private val event: AgentExecutionEvent) : AgentRuntime {
    lateinit var request: AgentRequest

    override fun execute(request: AgentRequest): Flow<AgentExecutionEvent> {
        this.request = request
        return flowOf(event)
    }
}

private class FakeChatRepository(
    conversations: List<Conversation> = listOf(Conversation(id = 1L, title = "Test")),
) : ChatRepository {
    private var nextMessageId = conversations.flatMap { it.messages }.maxOfOrNull { it.id }?.plus(1L) ?: 1L
    private var nextConversationId = conversations.maxOfOrNull { it.id }?.plus(1L) ?: 1L
    private val conversationsById = conversations.associateBy { it.id }.toMutableMap()

    override val chatSummaries = MutableStateFlow(conversations.map { it.summary() })
    override val selectedConversation = MutableStateFlow(conversations.firstOrNull())

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
        error: AgentExecutionError?,
    ): ChatMessage {
        val message = ChatMessage(
            id = nextMessageId++,
            author = author,
            text = text,
            createdAtMillis = 0L,
            generatedByModelName = generatedByModelName,
            generatedAtMillis = generatedAtMillis ?: generatedByModelName?.let { 0L },
            error = error,
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
                            generatedAtMillis = generatedAtMillis ?: generatedByModelName?.let { 0L },
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
        lastMessageError = messages.lastOrNull()?.error,
        updatedAtMillis = updatedAtMillis,
        messageCount = messages.size.toLong(),
    )
}

private class FakeAgentConfigurationRepository(
    selectedProviderConfig: AgentProviderConfig = AgentProviderConfig(
        id = AgentProviderId.Ollama,
        modelName = "qwen3.5:0.8b",
        endpoint = "http://127.0.0.1:11434",
        isConfigured = true,
    ),
) : AgentConfigurationRepository {
    override val providers = MutableStateFlow(listOf(selectedProviderConfig))
    override val selectedProvider = MutableStateFlow<AgentProviderConfig?>(selectedProviderConfig)

    override suspend fun selectProvider(id: AgentProviderId) {
        selectedProvider.value = providers.value.firstOrNull { it.id == id }
    }

    override suspend fun updateProvider(config: AgentProviderConfig) {
        providers.value = providers.value.map { if (it.id == config.id) config else it }
        if (selectedProvider.value?.id == config.id) {
            selectedProvider.value = config
        }
    }

    override suspend fun resetProvider(id: AgentProviderId) = Unit
}
