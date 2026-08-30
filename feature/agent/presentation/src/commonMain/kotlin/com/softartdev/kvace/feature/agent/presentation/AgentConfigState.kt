package com.softartdev.kvace.feature.agent.presentation

import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId

data class AgentConfigUiState(
    val providers: List<AgentProviderConfig> = emptyList(),
    val selectedProviderId: AgentProviderId? = null,
    val openAiModelInput: String = "",
    val openAiModelValidationError: OpenAiModelValidationError? = null,
) {
    val selectedProvider: AgentProviderConfig?
        get() = providers.firstOrNull { it.id == selectedProviderId }
}

sealed interface AgentConfigAction {
    data class ProviderSelected(val id: AgentProviderId) : AgentConfigAction
    data class OpenAiModelChanged(val modelName: String) : AgentConfigAction
}

sealed interface OpenAiModelValidationError {
    data object Required : OpenAiModelValidationError
}
