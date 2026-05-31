package com.softartdev.kvace.feature.chat.domain

import com.softartdev.kvace.feature.agent.domain.AgentExecutionEvent
import com.softartdev.kvace.feature.agent.domain.AgentRequest
import com.softartdev.kvace.feature.agent.domain.AgentRuntime
import kotlinx.coroutines.flow.collect

interface MessageSender {
    suspend operator fun invoke(text: String)
}

class SendMessageUseCase(
    private val chatRepository: ChatRepository,
    private val agentRuntime: AgentRuntime,
) : MessageSender {
    override suspend operator fun invoke(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty()) return

        chatRepository.appendMessage(MessageAuthor.User, prompt)
        var assistantMessage: ChatMessage? = null
        var reasoningMessage: ChatMessage? = null
        var toolMessage: ChatMessage? = null
        val assistantText = StringBuilder()
        val reasoningText = StringBuilder()
        val toolText = StringBuilder()

        agentRuntime.execute(AgentRequest(prompt)).collect { response ->
            when (response) {
                is AgentExecutionEvent.AssistantMessageDelta -> {
                    assistantText.append(response.text)
                    assistantMessage = upsertMessage(
                        message = assistantMessage,
                        author = MessageAuthor.Assistant,
                        text = assistantText.toString(),
                    )
                }
                is AgentExecutionEvent.AssistantMessage -> {
                    assistantText.clear()
                    assistantText.append(response.text)
                    assistantMessage = upsertMessage(
                        message = assistantMessage,
                        author = MessageAuthor.Assistant,
                        text = response.text,
                    )
                }
                is AgentExecutionEvent.ReasoningDelta -> {
                    reasoningText.append(response.text)
                    reasoningMessage = upsertMessage(
                        message = reasoningMessage,
                        author = MessageAuthor.Reasoning,
                        text = reasoningText.toString(),
                    )
                }
                is AgentExecutionEvent.ReasoningMessage -> {
                    reasoningText.clear()
                    reasoningText.append(response.text)
                    reasoningMessage = upsertMessage(
                        message = reasoningMessage,
                        author = MessageAuthor.Reasoning,
                        text = response.text,
                    )
                }
                is AgentExecutionEvent.ToolCallDelta -> {
                    toolText.append(response.text)
                    toolMessage = upsertMessage(
                        message = toolMessage,
                        author = MessageAuthor.Tool,
                        text = toolText.toString(),
                    )
                }
                is AgentExecutionEvent.ToolCall -> {
                    toolText.clear()
                    toolText.append(response.text)
                    toolMessage = upsertMessage(
                        message = toolMessage,
                        author = MessageAuthor.Tool,
                        text = response.text,
                    )
                }
                is AgentExecutionEvent.StreamFinished -> {
                    response.finishReason?.takeIf { it.isNotBlank() }?.let { finishReason ->
                        chatRepository.appendMessage(MessageAuthor.Event, finishReason)
                    }
                }
                is AgentExecutionEvent.Error -> {
                    chatRepository.appendMessage(MessageAuthor.Error, response.message)
                }
            }
        }
    }

    private suspend fun upsertMessage(
        message: ChatMessage?,
        author: MessageAuthor,
        text: String,
    ): ChatMessage =
        if (message == null) {
            chatRepository.appendMessage(author, text)
        } else {
            chatRepository.updateMessageText(message.id, text)
            message.copy(text = text)
        }
}
