package com.softartdev.kvace.feature.agent.ui

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.softartdev.kvace.feature.agent.presentation.OllamaConnectionStatus
import com.softartdev.kvace.feature.agent.presentation.OllamaEndpointSettingsUiState
import com.softartdev.kvace.feature.agent.presentation.OllamaModelsStatus

internal class OllamaEndpointSettingsPreviewProvider : PreviewParameterProvider<OllamaEndpointSettingsUiState> {

    override val values: Sequence<OllamaEndpointSettingsUiState> = sequenceOf(loadedState)

    internal companion object {
        val loadedState = OllamaEndpointSettingsUiState(
            hostInput = "127.0.0.1",
            portInput = "11434",
            modelInput = "llama3.2:latest",
            availableModels = listOf("llama3.2:latest", "qwen3.5:0.8b"),
            connectionStatus = OllamaConnectionStatus.Idle,
            modelsStatus = OllamaModelsStatus.Loaded,
        )
    }
}
