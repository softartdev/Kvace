package com.softartdev.kvace.feature.chat.data

import com.softartdev.kvace.feature.chat.domain.ChatMessage
import com.softartdev.kvace.feature.chat.domain.ChatRepository
import com.softartdev.kvace.feature.chat.domain.Conversation
import com.softartdev.kvace.feature.chat.domain.MessageAuthor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryChatRepository : ChatRepository {
    private var nextMessageId = 1L
    private val _conversation = MutableStateFlow(
        Conversation(
            id = 1L,
            title = "New chat",
            messages = listOf(
                ChatMessage(
                    id = nextMessageId++,
                    author = MessageAuthor.System,
                    text = "Kvace is ready. Ollama is selected by default; configure the endpoint in Settings if needed.",
                    createdAtMillis = currentTimeMillis(),
                )
            ),
        )
    )

    override val conversation: StateFlow<Conversation> = _conversation.asStateFlow()

    override suspend fun appendMessage(author: MessageAuthor, text: String): ChatMessage {
        val message = ChatMessage(
            id = nextMessageId++,
            author = author,
            text = text,
            createdAtMillis = currentTimeMillis(),
        )
        _conversation.value = _conversation.value.copy(
            messages = _conversation.value.messages + message,
        )
        return message
    }

    override suspend fun updateMessageText(messageId: Long, text: String) {
        _conversation.value = _conversation.value.copy(
            messages = _conversation.value.messages.map { message ->
                if (message.id == messageId) {
                    message.copy(text = text)
                } else {
                    message
                }
            },
        )
    }

    override suspend fun replaceConversation(conversation: Conversation) {
        _conversation.value = conversation
    }
}
