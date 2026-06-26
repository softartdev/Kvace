package com.softartdev.kvace.feature.chat.data

import com.softartdev.kvace.feature.chat.data.local.Chat_messages
import com.softartdev.kvace.feature.chat.data.local.Chats
import com.softartdev.kvace.feature.chat.data.local.SelectChatSummaries
import com.softartdev.kvace.feature.chat.domain.ChatMessage
import com.softartdev.kvace.feature.chat.domain.ChatSummary
import com.softartdev.kvace.feature.chat.domain.Conversation
import com.softartdev.kvace.feature.chat.domain.MessageAuthor

internal fun SelectChatSummaries.toDomain() = ChatSummary(
    id = id,
    title = title,
    lastMessagePreview = lastMessagePreview,
    updatedAtMillis = updatedAtMillis,
    messageCount = messageCount,
)

internal fun Chats.toDomain(messages: List<ChatMessage>) = Conversation(
    id = id,
    title = title,
    createdAtMillis = created_at_millis,
    updatedAtMillis = updated_at_millis,
    messages = messages,
)

internal fun Chat_messages.toDomain() = ChatMessage(
    id = id,
    author = MessageAuthor.valueOf(author),
    text = text,
    createdAtMillis = created_at_millis,
    generatedByModelName = generated_by_model_name,
    generatedAtMillis = generated_at_millis,
)
