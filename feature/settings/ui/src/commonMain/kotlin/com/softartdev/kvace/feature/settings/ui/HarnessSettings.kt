package com.softartdev.kvace.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softartdev.kvace.feature.settings.presentation.HarnessSettingsAction
import com.softartdev.kvace.feature.settings.presentation.HarnessSettingsUiState
import com.softartdev.kvace.feature.settings.presentation.HarnessSettingsViewModel
import com.softartdev.theme.material3.PreferableMaterialTheme
import com.softartdev.kvace.core.ui.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun HarnessSettings(
    modifier: Modifier = Modifier,
    viewModel: HarnessSettingsViewModel,
) {
    LaunchedEffect(viewModel) { viewModel.observeConfig() }
    val state by viewModel.uiState.collectAsState()
    HarnessSettings(
        modifier = modifier,
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun HarnessSettings(
    modifier: Modifier = Modifier,
    state: HarnessSettingsUiState,
    onAction: (HarnessSettingsAction) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.harness_enabled_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Switch(
                checked = state.enabled,
                onCheckedChange = { onAction(HarnessSettingsAction.EnabledChanged(it)) },
            )
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.systemPrompt,
            onValueChange = { onAction(HarnessSettingsAction.SystemPromptChanged(it)) },
            label = { Text(stringResource(Res.string.harness_system_prompt_label)) },
            enabled = state.enabled,
            minLines = 4,
            maxLines = 8,
        )
        Text(
            text = stringResource(Res.string.harness_inference_order),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { onAction(HarnessSettingsAction.ResetClicked) }) {
            Text(stringResource(Res.string.harness_reset))
        }
    }
}

@Preview
@Composable
private fun HarnessSettingsPreview() = PreferableMaterialTheme {
    HarnessSettings(
        state = HarnessSettingsUiState(
            enabled = true,
            systemPrompt = "You are Kvace, a concise AI assistant inside a multiplatform agent app.",
        ),
        onAction = {},
    )
}
