package com.softartdev.kvace.feature.chat.domain

import com.softartdev.kvace.feature.agent.domain.AgentExecutionEvent
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
    fun appendsUserAndAssistantMessages() = runTest {
        val repository = FakeChatRepository()
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = object : AgentRuntime {
                override fun execute(request: AgentRequest): Flow<AgentExecutionEvent> =
                    flowOf(AgentExecutionEvent.AssistantMessage("Hello from agent"))
            },
        )

        useCase("Hello")

        assertEquals(MessageAuthor.User, repository.conversation.value.messages[0].author)
        assertEquals(MessageAuthor.Assistant, repository.conversation.value.messages[1].author)
    }

    @Test
    fun ignoresBlankMessages() = runTest {
        val repository = FakeChatRepository()
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = FakeAgentRuntime(AgentExecutionEvent.AssistantMessage("Ignored")),
        )

        useCase("   ")

        assertEquals(emptyList(), repository.conversation.value.messages)
    }

    @Test
    fun appendsToolCallAsToolMessage() = runTest {
        val repository = FakeChatRepository()
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = FakeAgentRuntime(AgentExecutionEvent.ToolCall("Tool output")),
        )

        useCase("Run tool")

        assertEquals(MessageAuthor.Tool, repository.conversation.value.messages[1].author)
        assertEquals("Tool output", repository.conversation.value.messages[1].text)
    }

    @Test
    fun appendsRuntimeErrorAsErrorMessage() = runTest {
        val repository = FakeChatRepository()
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = FakeAgentRuntime(AgentExecutionEvent.Error("Provider failed")),
        )

        useCase("Hello")

        assertEquals(MessageAuthor.Error, repository.conversation.value.messages[1].author)
        assertEquals("Provider failed", repository.conversation.value.messages[1].text)
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
        )

        useCase("Hello")

        val messages = repository.conversation.value.messages
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
        )

        useCase("Think")

        val messages = repository.conversation.value.messages
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
        )

        useCase("Use a tool")

        val messages = repository.conversation.value.messages
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
        )

        useCase("Hello")

        val messages = repository.conversation.value.messages
        assertEquals(MessageAuthor.Event, messages[1].author)
        assertEquals("stop", messages[1].text)
    }

    @Test
    fun ignoresBlankStreamFinishReason() = runTest {
        val repository = FakeChatRepository()
        val useCase = SendMessageUseCase(
            chatRepository = repository,
            agentRuntime = FakeAgentRuntime(AgentExecutionEvent.StreamFinished("")),
        )

        useCase("Hello")

        assertEquals(1, repository.conversation.value.messages.size)
    }
}

private class FakeAgentRuntime(
    private val event: AgentExecutionEvent,
) : AgentRuntime {
    override fun execute(request: AgentRequest): Flow<AgentExecutionEvent> = flowOf(event)
}

private class FakeChatRepository : ChatRepository {
    private var nextId = 1L
    override val conversation = MutableStateFlow(Conversation(id = 1L, title = "Test"))

    override suspend fun appendMessage(author: MessageAuthor, text: String): ChatMessage {
        val message = ChatMessage(nextId++, author, text, 0L)
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
