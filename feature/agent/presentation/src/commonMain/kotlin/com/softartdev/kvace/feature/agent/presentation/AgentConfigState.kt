package com.softartdev.kvace.feature.agent.presentation

import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId

data class AgentConfigUiState(
    val providers: List<AgentProviderConfig> = emptyList(),
    val selectedProviderId: AgentProviderId? = null,
)

sealed interface AgentConfigAction {
    data class SelectProvider(val id: AgentProviderId) : AgentConfigAction
}
