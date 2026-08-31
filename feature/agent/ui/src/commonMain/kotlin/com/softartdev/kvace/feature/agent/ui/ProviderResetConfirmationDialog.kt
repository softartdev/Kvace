package com.softartdev.kvace.feature.agent.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.softartdev.kvace.core.ui.resources.Res
import com.softartdev.kvace.core.ui.resources.agents_reset_cancel
import com.softartdev.kvace.core.ui.resources.agents_reset_confirmation_title
import com.softartdev.kvace.core.ui.resources.agents_reset_confirm
import com.softartdev.kvace.core.ui.resources.agents_reset_ollama_message
import com.softartdev.kvace.core.ui.resources.agents_reset_openai_message
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProviderResetConfirmationDialog(
    provider: AgentProviderId,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (provider == AgentProviderId.OnDevice) return

    val message = stringResource(
        when (provider) {
            AgentProviderId.Ollama -> Res.string.agents_reset_ollama_message
            AgentProviderId.OpenAI -> Res.string.agents_reset_openai_message
            AgentProviderId.OnDevice -> error("On-device provider does not support reset")
        },
    )
    AlertDialog(
        modifier = Modifier.testTag("provider_reset_dialog_${provider.name}"),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.agents_reset_confirmation_title, provider.displayName))
        },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("provider_reset_confirm_${provider.name}"),
                onClick = onConfirm,
            ) {
                Text(stringResource(Res.string.agents_reset_confirm))
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag("provider_reset_cancel_${provider.name}"),
                onClick = onDismiss,
            ) {
                Text(stringResource(Res.string.agents_reset_cancel))
            }
        },
    )
}
