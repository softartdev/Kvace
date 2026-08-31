package com.softartdev.kvace.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.ui.ProviderResetConfirmationDialog
import com.softartdev.theme.material3.PreferableMaterialTheme

@Preview(widthDp = 920, heightDp = 720)
@Composable
fun ProviderResetOllamaDialogWidePreview() = PreferableMaterialTheme {
    ProviderResetConfirmationDialog(
        provider = AgentProviderId.Ollama,
        onConfirm = {},
        onDismiss = {},
    )
}

@Preview(widthDp = 390, heightDp = 760)
@Composable
fun ProviderResetOpenAiDialogCompactPreview() = PreferableMaterialTheme {
    ProviderResetConfirmationDialog(
        provider = AgentProviderId.OpenAI,
        onConfirm = {},
        onDismiss = {},
    )
}
