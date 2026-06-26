package com.softartdev.kvace.feature.chat.domain

import com.softartdev.kvace.feature.agent.domain.AgentConversationMessage
import com.softartdev.kvace.feature.agent.domain.AgentConversationRole

internal fun ChatMessage.toAgentConversationMessage(): AgentConversationMessage? = when (author) {
    MessageAuthor.User -> AgentConversationMessage(AgentConversationRole.User, text)
    MessageAuthor.Assistant -> AgentConversationMessage(AgentConversationRole.Assistant, text)
    MessageAuthor.Reasoning,
    MessageAuthor.Tool,
    MessageAuthor.Event,
    MessageAuthor.System,
    MessageAuthor.Error,
    -> null
}
