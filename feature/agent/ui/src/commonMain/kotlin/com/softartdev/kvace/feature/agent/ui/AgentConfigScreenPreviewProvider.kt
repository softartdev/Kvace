package com.softartdev.kvace.feature.agent.ui

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.presentation.AgentConfigUiState

internal class AgentConfigScreenPreviewProvider : PreviewParameterProvider<AgentConfigUiState> {

    override val values: Sequence<AgentConfigUiState> = sequenceOf(
        AgentConfigUiState(
            providers = listOf(
                AgentProviderConfig(
                    id = AgentProviderId.OpenAI,
                    modelName = "gpt-4o",
                    isConfigured = false,
                ),
                AgentProviderConfig(
                    id = AgentProviderId.Ollama,
                    modelName = "qwen3.5:0.8b",
                    endpoint = "http://127.0.0.1:11434",
                    isConfigured = true,
                ),
                AgentProviderConfig(
                    id = AgentProviderId.OnDevice,
                    modelName = "Apple Foundation Models",
                    isConfigured = true,
                ),
            ),
            selectedProviderId = AgentProviderId.Ollama,
            openAiModelInput = "gpt-4o",
        ),
    )
}
