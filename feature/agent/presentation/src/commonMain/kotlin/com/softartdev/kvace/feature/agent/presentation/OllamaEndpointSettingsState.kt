package com.softartdev.kvace.feature.agent.presentation

data class OllamaEndpointSettingsUiState(
    val hostInput: String = "",
    val portInput: String = "",
    val modelInput: String = "",
    val availableModels: List<String> = emptyList(),
    val connectionStatus: OllamaConnectionStatus = OllamaConnectionStatus.Idle,
    val modelsStatus: OllamaModelsStatus = OllamaModelsStatus.Idle,
)

sealed interface OllamaEndpointSettingsAction {
    data class HostChanged(val host: String) : OllamaEndpointSettingsAction
    data class PortChanged(val port: String) : OllamaEndpointSettingsAction
    data class ModelSelected(val modelName: String) : OllamaEndpointSettingsAction
    data object TestConnection : OllamaEndpointSettingsAction
    data object LoadModels : OllamaEndpointSettingsAction
}

sealed interface OllamaConnectionStatus {
    data object Idle : OllamaConnectionStatus
    data object Testing : OllamaConnectionStatus
    data object Success : OllamaConnectionStatus
    data object InvalidHost : OllamaConnectionStatus
    data object InvalidPort : OllamaConnectionStatus
    data class Failure(val message: String?) : OllamaConnectionStatus
}

sealed interface OllamaModelsStatus {
    data object Idle : OllamaModelsStatus
    data object Loading : OllamaModelsStatus
    data object Loaded : OllamaModelsStatus
    data object SelectionRequired : OllamaModelsStatus
    data object Empty : OllamaModelsStatus
    data class Failure(val message: String?) : OllamaModelsStatus
}
