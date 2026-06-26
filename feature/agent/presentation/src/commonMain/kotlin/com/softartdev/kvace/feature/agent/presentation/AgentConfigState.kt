package com.softartdev.kvace.feature.agent.presentation

import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId

data class AgentConfigUiState(
    val providers: List<AgentProviderConfig> = emptyList(),
    val selectedProviderId: AgentProviderId? = null,
) {
    val selectedProvider: AgentProviderConfig?
        get() = providers.firstOrNull { it.id == selectedProviderId }
}

sealed interface AgentConfigAction {
    data class ProviderSelected(val id: AgentProviderId) : AgentConfigAction
    data class ProviderModelChanged(val id: AgentProviderId, val modelName: String) : AgentConfigAction
}
