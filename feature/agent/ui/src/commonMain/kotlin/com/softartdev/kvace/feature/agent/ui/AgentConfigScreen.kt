package com.softartdev.kvace.feature.agent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softartdev.kvace.core.ui.KvaceScreenScaffold
import com.softartdev.kvace.feature.agent.domain.AgentProviderConfig
import com.softartdev.kvace.feature.agent.domain.AgentProviderId
import com.softartdev.kvace.feature.agent.presentation.AgentConfigAction
import com.softartdev.kvace.feature.agent.presentation.AgentConfigUiState
import com.softartdev.kvace.feature.agent.presentation.AgentConfigViewModel
import com.softartdev.kvace.feature.agent.presentation.OllamaConnectionStatus
import com.softartdev.kvace.feature.agent.presentation.OllamaEndpointSettingsAction
import com.softartdev.kvace.feature.agent.presentation.OllamaEndpointSettingsUiState
import com.softartdev.kvace.feature.agent.presentation.OllamaEndpointSettingsViewModel
import com.softartdev.kvace.feature.agent.presentation.OllamaModelsStatus
import kvace.feature.agent.ui.generated.resources.Res
import kvace.feature.agent.ui.generated.resources.agents_configuration_placeholder
import kvace.feature.agent.ui.generated.resources.agents_configured
import kvace.feature.agent.ui.generated.resources.agents_endpoint_label
import kvace.feature.agent.ui.generated.resources.agents_model_label
import kvace.feature.agent.ui.generated.resources.agents_ollama_connection_failed
import kvace.feature.agent.ui.generated.resources.agents_ollama_connection_failed_detail
import kvace.feature.agent.ui.generated.resources.agents_ollama_connection_success
import kvace.feature.agent.ui.generated.resources.agents_ollama_endpoint_title
import kvace.feature.agent.ui.generated.resources.agents_ollama_host_label
import kvace.feature.agent.ui.generated.resources.agents_ollama_invalid_host
import kvace.feature.agent.ui.generated.resources.agents_ollama_invalid_port
import kvace.feature.agent.ui.generated.resources.agents_ollama_load_models
import kvace.feature.agent.ui.generated.resources.agents_ollama_loading_models
import kvace.feature.agent.ui.generated.resources.agents_ollama_model_label
import kvace.feature.agent.ui.generated.resources.agents_ollama_models_available
import kvace.feature.agent.ui.generated.resources.agents_ollama_models_failed
import kvace.feature.agent.ui.generated.resources.agents_ollama_models_failed_detail
import kvace.feature.agent.ui.generated.resources.agents_ollama_models_loaded
import kvace.feature.agent.ui.generated.resources.agents_ollama_models_empty
import kvace.feature.agent.ui.generated.resources.agents_ollama_port_label
import kvace.feature.agent.ui.generated.resources.agents_ollama_test_connection
import kvace.feature.agent.ui.generated.resources.agents_ollama_testing_connection
import kvace.feature.agent.ui.generated.resources.agents_select
import kvace.feature.agent.ui.generated.resources.agents_selected
import kvace.feature.agent.ui.generated.resources.agents_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun AgentConfigScreen(
    viewModel: AgentConfigViewModel,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(viewModel) {
        viewModel.observeProviders()
    }
    val state by viewModel.uiState.collectAsState()
    AgentConfigScreenContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
fun OllamaEndpointSettings(
    viewModel: OllamaEndpointSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) { viewModel.observeEndpoint() }
    OllamaEndpointEditor(
        modifier = modifier.padding(horizontal = 16.dp),
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun AgentConfigScreenContent(
    state: AgentConfigUiState,
    onAction: (AgentConfigAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    KvaceScreenScaffold(
        title = stringResource(Res.string.agents_title),
        modifier = modifier,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(state.providers, key = { it.id.name }) { provider ->
                AgentProviderItem(
                    provider = provider,
                    selected = provider.id == state.selectedProviderId,
                    onClick = { onAction(AgentConfigAction.SelectProvider(provider.id)) },
                )
            }
        }
    }
}

@Composable
private fun AgentProviderItem(
    provider: AgentProviderConfig,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            Icon(
                imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
            )
        },
        headlineContent = { Text(provider.displayName) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(Res.string.agents_model_label, provider.modelName))
                provider.endpoint?.let { endpoint ->
                    Text(stringResource(Res.string.agents_endpoint_label, endpoint))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(
                        onClick = onClick,
                        label = {
                            Text(
                                if (selected) {
                                    stringResource(Res.string.agents_selected)
                                } else {
                                    stringResource(Res.string.agents_select)
                                },
                            )
                        },
                    )
                    Text(
                        text = if (provider.isConfigured) {
                            stringResource(Res.string.agents_configured)
                        } else {
                            stringResource(Res.string.agents_configuration_placeholder)
                        },
                        color = if (provider.isConfigured) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        },
    )
}

@Composable
fun OllamaEndpointEditor(
    modifier: Modifier = Modifier,
    state: OllamaEndpointSettingsUiState,
    onAction: (OllamaEndpointSettingsAction) -> Unit,
) {
    val isTesting = state.connectionStatus == OllamaConnectionStatus.Testing
    val isLoadingModels = state.modelsStatus == OllamaModelsStatus.Loading
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.agents_ollama_endpoint_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ollama_host_field"),
            value = state.hostInput,
            onValueChange = { onAction(OllamaEndpointSettingsAction.HostChanged(it)) },
            label = { Text(stringResource(Res.string.agents_ollama_host_label)) },
            singleLine = true,
            enabled = !isTesting,
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ollama_port_field"),
            value = state.portInput,
            onValueChange = { onAction(OllamaEndpointSettingsAction.PortChanged(it)) },
            label = { Text(stringResource(Res.string.agents_ollama_port_label)) },
            singleLine = true,
            enabled = !isTesting,
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ollama_model_field"),
            value = state.modelInput,
            onValueChange = { onAction(OllamaEndpointSettingsAction.ModelChanged(it)) },
            label = { Text(stringResource(Res.string.agents_ollama_model_label)) },
            singleLine = true,
            enabled = !isTesting && !isLoadingModels,
        )
        Button(
            modifier = Modifier.testTag("ollama_test_connection_button"),
            onClick = { onAction(OllamaEndpointSettingsAction.TestConnection) },
            enabled = !isTesting && !isLoadingModels,
        ) {
            if (isTesting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                )
            }
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = if (isTesting) {
                    stringResource(Res.string.agents_ollama_testing_connection)
                } else {
                    stringResource(Res.string.agents_ollama_test_connection)
                },
            )
        }
        OllamaConnectionStatusText(
            modifier = Modifier.testTag("ollama_connection_status"),
            status = state.connectionStatus,
        )
        OutlinedButton(
            modifier = Modifier.testTag("ollama_load_models_button"),
            onClick = { onAction(OllamaEndpointSettingsAction.LoadModels) },
            enabled = !isTesting && !isLoadingModels,
        ) {
            if (isLoadingModels) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                )
            }
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = if (isLoadingModels) {
                    stringResource(Res.string.agents_ollama_loading_models)
                } else {
                    stringResource(Res.string.agents_ollama_load_models)
                },
            )
        }
        OllamaModelsStatusText(
            modifier = Modifier.testTag("ollama_models_status"),
            status = state.modelsStatus,
        )
        if (state.availableModels.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.agents_ollama_models_available),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.availableModels.forEach { modelName ->
                    FilterChip(
                        selected = modelName == state.modelInput.trim(),
                        onClick = { onAction(OllamaEndpointSettingsAction.ModelSelected(modelName)) },
                        label = { Text(modelName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OllamaConnectionStatusText(
    status: OllamaConnectionStatus,
    modifier: Modifier = Modifier,
) {
    when (status) {
        OllamaConnectionStatus.Idle,
        OllamaConnectionStatus.Testing -> Unit
        OllamaConnectionStatus.Success -> Text(
            modifier = modifier,
            text = stringResource(Res.string.agents_ollama_connection_success),
            color = MaterialTheme.colorScheme.primary,
        )
        OllamaConnectionStatus.InvalidHost -> Text(
            modifier = modifier,
            text = stringResource(Res.string.agents_ollama_invalid_host),
            color = MaterialTheme.colorScheme.error,
        )
        OllamaConnectionStatus.InvalidPort -> Text(
            modifier = modifier,
            text = stringResource(Res.string.agents_ollama_invalid_port),
            color = MaterialTheme.colorScheme.error,
        )
        is OllamaConnectionStatus.Failure -> Text(
            modifier = modifier,
            text = status.message?.let {
                stringResource(Res.string.agents_ollama_connection_failed_detail, it)
            } ?: stringResource(Res.string.agents_ollama_connection_failed),
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun OllamaModelsStatusText(
    status: OllamaModelsStatus,
    modifier: Modifier = Modifier,
) {
    when (status) {
        OllamaModelsStatus.Idle,
        OllamaModelsStatus.Loading -> Unit
        OllamaModelsStatus.Loaded -> Text(
            modifier = modifier,
            text = stringResource(Res.string.agents_ollama_models_loaded),
            color = MaterialTheme.colorScheme.primary,
        )
        OllamaModelsStatus.Empty -> Text(
            modifier = modifier,
            text = stringResource(Res.string.agents_ollama_models_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is OllamaModelsStatus.Failure -> Text(
            modifier = modifier,
            text = status.message?.let {
                stringResource(Res.string.agents_ollama_models_failed_detail, it)
            } ?: stringResource(Res.string.agents_ollama_models_failed),
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Preview
@Composable
private fun AgentConfigScreenPreview() {
    MaterialTheme {
        AgentConfigScreenContent(
            state = AgentConfigUiState(
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
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun OllamaEndpointSettingsPreview() {
    MaterialTheme {
        OllamaEndpointEditor(
            state = OllamaEndpointSettingsUiState(
                hostInput = "127.0.0.1",
                portInput = "11434",
                modelInput = "llama3.2:latest",
                availableModels = listOf("llama3.2:latest", "qwen3.5:0.8b"),
                connectionStatus = OllamaConnectionStatus.Idle,
                modelsStatus = OllamaModelsStatus.Loaded,
            ),
            onAction = {},
        )
    }
}
