package com.softartdev.kvace.feature.agent.presentation

import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialStatus

data class AgentConfigUiState(
    val providers: List<AgentProviderConfig> = emptyList(),
    val selectedProviderId: AgentProviderId? = null,
    val openAiModelInput: String = "",
    val openAiEndpointInput: String = "",
    val openAiModelValidationError: OpenAiModelValidationError? = null,
    val openAiCredentialStatus: ProviderCredentialStatus = ProviderCredentialStatus.Absent,
    val openAiConnectionStatus: OpenAiConnectionStatus = OpenAiConnectionStatus.Idle,
    val isOpenAiResetDialogVisible: Boolean = false,
    val openAiResetStatus: ProviderResetStatus = ProviderResetStatus.Idle,
) {
    val selectedProvider: AgentProviderConfig?
        get() = providers.firstOrNull { it.id == selectedProviderId }
}

sealed interface AgentConfigAction {
    data class ProviderSelected(val id: AgentProviderId) : AgentConfigAction
    data class OpenAiModelChanged(val modelName: String) : AgentConfigAction
    data class OpenAiEndpointChanged(val endpoint: String) : AgentConfigAction
    data class OpenAiApiKeySubmitted(val apiKey: String) : AgentConfigAction
    data object OpenAiCredentialDeleted : AgentConfigAction
    data class OpenAiStorageUnlocked(val masterPassword: String) : AgentConfigAction
    data object OpenAiLockedCredentialCleared : AgentConfigAction
    data object OpenAiResetRequested : AgentConfigAction
    data object OpenAiResetConfirmed : AgentConfigAction
    data object OpenAiResetDismissed : AgentConfigAction
}

sealed interface OpenAiModelValidationError {
    data object Required : OpenAiModelValidationError
}

sealed interface OpenAiConnectionStatus {
    data object Idle : OpenAiConnectionStatus
    data object CheckingEndpoint : OpenAiConnectionStatus
    data object Verifying : OpenAiConnectionStatus
    data object Success : OpenAiConnectionStatus
    data class Failure(val reason: String?) : OpenAiConnectionStatus
}

sealed interface ProviderResetStatus {
    data object Idle : ProviderResetStatus
    data object Resetting : ProviderResetStatus
    data object Failure : ProviderResetStatus
}
