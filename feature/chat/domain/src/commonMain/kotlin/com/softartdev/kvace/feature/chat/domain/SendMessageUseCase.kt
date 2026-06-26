package com.softartdev.kvace.feature.chat.domain

import com.softartdev.kvace.feature.agent.domain.AgentConversationMessage
import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentExecutionEvent
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentRequest
import com.softartdev.kvace.feature.agent.domain.AgentRuntime

interface MessageSender {
    suspend operator fun invoke(conversationId: Long, text: String)
}

class SendMessageUseCase(
    private val chatRepository: ChatRepository,
    private val agentRuntime: AgentRuntime,
    private val agentConfigurationRepository: AgentConfigurationRepository,
) : MessageSender {

    override suspend operator fun invoke(conversationId: Long, text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty()) return

        val conversation = chatRepository.getConversation(conversationId) ?: return
        val context: List<AgentConversationMessage> = conversation.messages.mapNotNull { message ->
            message.toAgentConversationMessage()
        }
        val provider: AgentProviderConfig? = agentConfigurationRepository.selectedProvider.value
        val generatedByModelName = provider?.modelName
        chatRepository.appendMessage(conversationId, MessageAuthor.User, prompt)
        var assistantMessage: ChatMessage? = null
        var reasoningMessage: ChatMessage? = null
        var toolMessage: ChatMessage? = null
        val assistantText = StringBuilder()
        val reasoningText = StringBuilder()
        val toolText = StringBuilder()

        agentRuntime.execute(
            AgentRequest(
                prompt = prompt,
                providerId = provider?.id,
                context = context,
            )
        ).collect { response ->
            when (response) {
                is AgentExecutionEvent.AssistantMessageDelta -> {
                    assistantText.append(response.text)
                    assistantMessage = upsertMessage(
                        conversationId = conversationId,
                        message = assistantMessage,
                        author = MessageAuthor.Assistant,
                        text = assistantText.toString(),
                        generatedByModelName = generatedByModelName,
                    )
                }
                is AgentExecutionEvent.AssistantMessage -> {
                    assistantText.clear()
                    assistantText.append(response.text)
                    assistantMessage = upsertMessage(
                        conversationId = conversationId,
                        message = assistantMessage,
                        author = MessageAuthor.Assistant,
                        text = response.text,
                        generatedByModelName = generatedByModelName,
                    )
                }
                is AgentExecutionEvent.ReasoningDelta -> {
                    reasoningText.append(response.text)
                    reasoningMessage = upsertMessage(
                        conversationId = conversationId,
                        message = reasoningMessage,
                        author = MessageAuthor.Reasoning,
                        text = reasoningText.toString(),
                        generatedByModelName = generatedByModelName,
                    )
                }
                is AgentExecutionEvent.ReasoningMessage -> {
                    reasoningText.clear()
                    reasoningText.append(response.text)
                    reasoningMessage = upsertMessage(
                        conversationId = conversationId,
                        message = reasoningMessage,
                        author = MessageAuthor.Reasoning,
                        text = response.text,
                        generatedByModelName = generatedByModelName,
                    )
                }
                is AgentExecutionEvent.ToolCallDelta -> {
                    toolText.append(response.text)
                    toolMessage = upsertMessage(
                        conversationId = conversationId,
                        message = toolMessage,
                        author = MessageAuthor.Tool,
                        text = toolText.toString(),
                        generatedByModelName = generatedByModelName,
                    )
                }
                is AgentExecutionEvent.ToolCall -> {
                    toolText.clear()
                    toolText.append(response.text)
                    toolMessage = upsertMessage(
                        conversationId = conversationId,
                        message = toolMessage,
                        author = MessageAuthor.Tool,
                        text = response.text,
                        generatedByModelName = generatedByModelName,
                    )
                }
                is AgentExecutionEvent.StreamFinished -> {
                    response.finishReason?.takeIf { it.isNotBlank() }?.let { finishReason ->
                        chatRepository.appendMessage(
                            conversationId = conversationId,
                            author = MessageAuthor.Event,
                            text = finishReason,
                            generatedByModelName = generatedByModelName,
                        )
                    }
                }
                is AgentExecutionEvent.Error -> {
                    chatRepository.appendMessage(
                        conversationId = conversationId,
                        author = MessageAuthor.Error,
                        text = response.message,
                        generatedByModelName = generatedByModelName,
                    )
                }
            }
        }
    }

    private suspend fun upsertMessage(
        conversationId: Long,
        message: ChatMessage?,
        author: MessageAuthor,
        text: String,
        generatedByModelName: String?,
    ): ChatMessage = when (message) {
        null -> chatRepository.appendMessage(
            conversationId = conversationId,
            author = author,
            text = text,
            generatedByModelName = generatedByModelName,
        )
        else -> {
            chatRepository.updateMessageText(
                conversationId = conversationId,
                messageId = message.id,
                text = text,
                generatedByModelName = generatedByModelName,
            )
            message.copy(text = text, generatedByModelName = generatedByModelName)
        }
    }
}
