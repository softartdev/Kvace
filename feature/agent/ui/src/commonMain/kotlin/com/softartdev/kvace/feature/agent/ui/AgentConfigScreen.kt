@file:OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)

package com.softartdev.kvace.feature.agent.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.softartdev.kvace.core.ui.KvaceVerticalPaneExpansionDragHandle
import com.softartdev.kvace.core.ui.resources.Res
import com.softartdev.kvace.core.ui.resources.agents_back_content_description
import com.softartdev.kvace.core.ui.resources.agents_configuration_placeholder
import com.softartdev.kvace.core.ui.resources.agents_configured
import com.softartdev.kvace.core.ui.resources.agents_endpoint_label
import com.softartdev.kvace.core.ui.resources.agents_model_label
import com.softartdev.kvace.core.ui.resources.agents_ollama_connection_failed
import com.softartdev.kvace.core.ui.resources.agents_ollama_connection_failed_detail
import com.softartdev.kvace.core.ui.resources.agents_ollama_connection_success
import com.softartdev.kvace.core.ui.resources.agents_ollama_endpoint_title
import com.softartdev.kvace.core.ui.resources.agents_ollama_host_label
import com.softartdev.kvace.core.ui.resources.agents_ollama_invalid_host
import com.softartdev.kvace.core.ui.resources.agents_ollama_invalid_port
import com.softartdev.kvace.core.ui.resources.agents_ollama_load_models
import com.softartdev.kvace.core.ui.resources.agents_ollama_loading_models
import com.softartdev.kvace.core.ui.resources.agents_ollama_model_label
import com.softartdev.kvace.core.ui.resources.agents_ollama_models_available
import com.softartdev.kvace.core.ui.resources.agents_ollama_models_empty
import com.softartdev.kvace.core.ui.resources.agents_ollama_models_failed
import com.softartdev.kvace.core.ui.resources.agents_ollama_models_failed_detail
import com.softartdev.kvace.core.ui.resources.agents_ollama_models_loaded
import com.softartdev.kvace.core.ui.resources.agents_ollama_model_selection_required
import com.softartdev.kvace.core.ui.resources.agents_ollama_port_label
import com.softartdev.kvace.core.ui.resources.agents_ollama_test_connection
import com.softartdev.kvace.core.ui.resources.agents_ollama_testing_connection
import com.softartdev.kvace.core.ui.resources.agents_on_device_available
import com.softartdev.kvace.core.ui.resources.agents_on_device_unavailable
import com.softartdev.kvace.core.ui.resources.agents_openai_credentials_message
import com.softartdev.kvace.core.ui.resources.agents_openai_credentials_title
import com.softartdev.kvace.core.ui.resources.agents_openai_api_key_label
import com.softartdev.kvace.core.ui.resources.agents_openai_delete_key
import com.softartdev.kvace.core.ui.resources.agents_openai_endpoint_label
import com.softartdev.kvace.core.ui.resources.agents_openai_save_verify
import com.softartdev.kvace.core.ui.resources.agents_openai_status_failure
import com.softartdev.kvace.core.ui.resources.agents_openai_status_success
import com.softartdev.kvace.core.ui.resources.agents_openai_master_password_label
import com.softartdev.kvace.core.ui.resources.agents_openai_unlock_storage
import com.softartdev.kvace.core.ui.resources.agents_openai_model_required
import com.softartdev.kvace.core.ui.resources.agents_provider_placeholder_message
import com.softartdev.kvace.core.ui.resources.agents_provider_placeholder_title
import com.softartdev.kvace.core.ui.resources.agents_reset_failed
import com.softartdev.kvace.core.ui.resources.agents_reset_provider_settings
import com.softartdev.kvace.core.ui.resources.agents_resetting
import com.softartdev.kvace.core.ui.resources.agents_selected_provider_content_description
import com.softartdev.kvace.core.ui.resources.agents_title
import com.softartdev.kvace.core.ui.resources.ic_arrow_back
import com.softartdev.kvace.core.ui.resources.ic_check_circle
import com.softartdev.kvace.core.ui.resources.ic_play_arrow
import com.softartdev.kvace.core.ui.resources.ic_radio_button_unchecked
import com.softartdev.kvace.core.ui.resources.ic_refresh
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
import com.softartdev.kvace.feature.agent.presentation.OpenAiModelValidationError
import com.softartdev.kvace.feature.agent.presentation.OpenAiConnectionStatus
import com.softartdev.kvace.feature.agent.presentation.ProviderResetStatus
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialStatus
import com.softartdev.theme.material3.PreferableMaterialTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AgentConfigScreen(
    modifier: Modifier = Modifier,
    viewModel: AgentConfigViewModel,
    ollamaEndpointSettingsViewModel: OllamaEndpointSettingsViewModel,
) {
    LaunchedEffect(viewModel) { viewModel.observeProviders() }
    LaunchedEffect(ollamaEndpointSettingsViewModel) { ollamaEndpointSettingsViewModel.observeEndpoint() }
    val state by viewModel.uiState.collectAsState()
    val ollamaState by ollamaEndpointSettingsViewModel.uiState.collectAsState()
    AgentConfigScreen(
        modifier = modifier,
        state = state,
        ollamaState = ollamaState,
        onAction = viewModel::onAction,
        onOllamaAction = ollamaEndpointSettingsViewModel::onAction,
    )
}

@Composable
fun OllamaEndpointSettings(
    modifier: Modifier = Modifier,
    viewModel: OllamaEndpointSettingsViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) { viewModel.observeEndpoint() }
    OllamaEndpointSettings(
        modifier = modifier,
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun AgentConfigScreen(
    modifier: Modifier = Modifier,
    state: AgentConfigUiState,
    ollamaState: OllamaEndpointSettingsUiState,
    onAction: (AgentConfigAction) -> Unit,
    onOllamaAction: (OllamaEndpointSettingsAction) -> Unit,
    autoNavigateToSelectedProvider: Boolean = false,
) {
    val navigator: ThreePaneScaffoldNavigator<AgentProviderId> =
        rememberListDetailPaneScaffoldNavigator<AgentProviderId>()
    val paneExpansionState: PaneExpansionState = rememberPaneExpansionState()
    val coroutineScope = rememberCoroutineScope()
    val canNavigateBack = navigator.canNavigateBack()

    LaunchedEffect(autoNavigateToSelectedProvider, state.selectedProviderId) {
        val selectedProviderId = state.selectedProviderId
        if (autoNavigateToSelectedProvider && selectedProviderId != null) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, selectedProviderId)
        }
    }

    ListDetailPaneScaffold(
        modifier = modifier.fillMaxSize(),
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            ProvidersMasterPane(
                state = state,
                onProviderClick = { providerId ->
                    onAction(AgentConfigAction.ProviderSelected(providerId))
                    coroutineScope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, providerId)
                    }
                },
            )
        },
        detailPane = {
            ProviderDetailPane(
                provider = state.selectedProvider,
                ollamaState = ollamaState,
                openAiModelInput = state.openAiModelInput,
                openAiEndpointInput = state.openAiEndpointInput,
                openAiConnectionStatus = state.openAiConnectionStatus,
                openAiCredentialStatus = state.openAiCredentialStatus,
                openAiModelValidationError = state.openAiModelValidationError,
                openAiResetStatus = state.openAiResetStatus,
                onAction = onAction,
                onOllamaAction = onOllamaAction,
                onBackClick = when {
                    canNavigateBack -> { { coroutineScope.launch { navigator.navigateBack() } } }
                    else -> null
                },
            )
        },
        paneExpansionDragHandle = { state: PaneExpansionState ->
            KvaceVerticalPaneExpansionDragHandle(state)
        },
        paneExpansionState = paneExpansionState,
    )

    if (ollamaState.isResetDialogVisible) {
        ProviderResetConfirmationDialog(
            provider = AgentProviderId.Ollama,
            onConfirm = { onOllamaAction(OllamaEndpointSettingsAction.ResetConfirmed) },
            onDismiss = { onOllamaAction(OllamaEndpointSettingsAction.ResetDismissed) },
        )
    }
    if (state.isOpenAiResetDialogVisible) {
        ProviderResetConfirmationDialog(
            provider = AgentProviderId.OpenAI,
            onConfirm = { onAction(AgentConfigAction.OpenAiResetConfirmed) },
            onDismiss = { onAction(AgentConfigAction.OpenAiResetDismissed) },
        )
    }
}

@Composable
private fun ProvidersMasterPane(
    modifier: Modifier = Modifier,
    state: AgentConfigUiState,
    onProviderClick: (AgentProviderId) -> Unit,
) = Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(Res.string.agents_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
        )
    },
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
                onClick = { onProviderClick(provider.id) },
            )
        }
    }
}

@Composable
private fun ProviderDetailPane(
    modifier: Modifier = Modifier,
    provider: AgentProviderConfig?,
    ollamaState: OllamaEndpointSettingsUiState,
    openAiModelInput: String,
    openAiEndpointInput: String,
    openAiConnectionStatus: OpenAiConnectionStatus,
    openAiCredentialStatus: ProviderCredentialStatus,
    openAiModelValidationError: OpenAiModelValidationError?,
    openAiResetStatus: ProviderResetStatus,
    onAction: (AgentConfigAction) -> Unit,
    onOllamaAction: (OllamaEndpointSettingsAction) -> Unit,
    onBackClick: (() -> Unit)?,
) = Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
        TopAppBar(
            title = {
                Text(
                    text = provider?.displayName ?: stringResource(Res.string.agents_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = stringResource(Res.string.agents_back_content_description),
                        )
                    }
                }
            },
        )
    },
) { paddingValues ->
    if (provider == null) {
        ProviderPlaceholder(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                when (provider.id) {
                    AgentProviderId.Ollama -> OllamaEndpointSettings(
                        state = ollamaState,
                        onAction = onOllamaAction,
                    )
                    AgentProviderId.OnDevice -> OnDeviceProviderDetail(provider)
                    AgentProviderId.OpenAI -> OpenAiProviderDetail(
                        modelInput = openAiModelInput,
                        endpointInput = openAiEndpointInput,
                        connectionStatus = openAiConnectionStatus,
                        credentialStatus = openAiCredentialStatus,
                        validationError = openAiModelValidationError,
                        resetStatus = openAiResetStatus,
                        onModelChanged = { modelName ->
                            onAction(AgentConfigAction.OpenAiModelChanged(modelName))
                        },
                        onEndpointChanged = { endpoint -> onAction(AgentConfigAction.OpenAiEndpointChanged(endpoint)) },
                        onApiKeySubmitted = { apiKey -> onAction(AgentConfigAction.OpenAiApiKeySubmitted(apiKey)) },
                        onDeleteKey = { onAction(AgentConfigAction.OpenAiCredentialDeleted) },
                        onStorageUnlock = { password -> onAction(AgentConfigAction.OpenAiStorageUnlocked(password)) },
                        onReset = { onAction(AgentConfigAction.OpenAiResetRequested) },
                    )
                }
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
    val configurationLabelRes: StringResource = when {
        provider.isConfigured -> Res.string.agents_configured
        else -> Res.string.agents_configuration_placeholder
    }
    val configurationColor: Color = when {
        provider.isConfigured -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val containerColor: Color = when {
        selected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("agent_provider_${provider.id.name}")
            .clickable(onClick = onClick),
        leadingContent = {
            Icon(
                painter = painterResource(
                    resource = when {
                        selected -> Res.drawable.ic_check_circle
                        else -> Res.drawable.ic_radio_button_unchecked
                    }
                ),
                contentDescription = when {
                    selected -> stringResource(Res.string.agents_selected_provider_content_description)
                    else -> null
                },
            )
        },
        headlineContent = {
            Text(
                text = provider.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(Res.string.agents_model_label, provider.modelName),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                provider.endpoint?.let { endpoint ->
                    Text(
                        text = stringResource(Res.string.agents_endpoint_label, endpoint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(configurationLabelRes),
                    color = configurationColor,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = containerColor),
    )
}

@Composable
private fun OnDeviceProviderDetail(provider: AgentProviderConfig) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AssistChip(
            onClick = {},
            label = {
                Text(
                    stringResource(
                        when {
                            provider.isConfigured -> Res.string.agents_on_device_available
                            else -> Res.string.agents_on_device_unavailable
                        }
                    )
                )
            },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = provider.modelName,
            onValueChange = {},
            label = { Text(stringResource(Res.string.agents_ollama_model_label)) },
            enabled = false,
            singleLine = true,
        )
    }
}

@Composable
private fun OpenAiProviderDetail(
    modelInput: String,
    endpointInput: String,
    connectionStatus: OpenAiConnectionStatus,
    credentialStatus: ProviderCredentialStatus,
    validationError: OpenAiModelValidationError?,
    resetStatus: ProviderResetStatus,
    onModelChanged: (String) -> Unit,
    onEndpointChanged: (String) -> Unit,
    onApiKeySubmitted: (String) -> Unit,
    onDeleteKey: () -> Unit,
    onStorageUnlock: (String) -> Unit,
    onReset: () -> Unit,
) {
    var apiKey: String by remember { androidx.compose.runtime.mutableStateOf("") }
    var masterPassword: String by remember { androidx.compose.runtime.mutableStateOf("") }
    val isResetting = resetStatus == ProviderResetStatus.Resetting
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = endpointInput,
            onValueChange = onEndpointChanged,
            label = { Text(stringResource(Res.string.agents_openai_endpoint_label)) },
            singleLine = true,
            enabled = !isResetting,
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("openai_model_field"),
            value = modelInput,
            onValueChange = onModelChanged,
            label = { Text(stringResource(Res.string.agents_ollama_model_label)) },
            supportingText = when (validationError) {
                OpenAiModelValidationError.Required -> {
                    {
                        Text(
                            modifier = Modifier.testTag("openai_model_error"),
                            text = stringResource(Res.string.agents_openai_model_required),
                        )
                    }
                }
                null -> null
            },
            isError = validationError != null,
            singleLine = true,
            enabled = !isResetting,
        )
        Text(
            text = stringResource(Res.string.agents_openai_credentials_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (credentialStatus == ProviderCredentialStatus.Locked) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = masterPassword,
                onValueChange = { masterPassword = it },
                label = { Text(stringResource(Res.string.agents_openai_master_password_label)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !isResetting,
            )
            OutlinedButton(
                onClick = {
                    onStorageUnlock(masterPassword)
                    masterPassword = ""
                },
                enabled = masterPassword.isNotBlank() && !isResetting,
            ) {
                Text(stringResource(Res.string.agents_openai_unlock_storage))
            }
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(stringResource(Res.string.agents_openai_api_key_label)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !isResetting,
        )
        Button(
            onClick = {
                onApiKeySubmitted(apiKey)
                apiKey = ""
            },
            enabled = apiKey.isNotBlank() &&
                connectionStatus !is OpenAiConnectionStatus.Verifying &&
                !isResetting,
        ) {
            Text(stringResource(Res.string.agents_openai_save_verify))
        }
        OutlinedButton(onClick = onDeleteKey, enabled = !isResetting) {
            Text(stringResource(Res.string.agents_openai_delete_key))
        }
        when (connectionStatus) {
            OpenAiConnectionStatus.Success -> Text(stringResource(Res.string.agents_openai_status_success))
            is OpenAiConnectionStatus.Failure -> Text(stringResource(Res.string.agents_openai_status_failure))
            else -> Unit
        }
        Text(
            text = stringResource(Res.string.agents_openai_credentials_message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ProviderResetControls(
            status = resetStatus,
            provider = AgentProviderId.OpenAI,
            onReset = onReset,
        )
    }
}

@Composable
private fun ProviderResetControls(
    status: ProviderResetStatus,
    provider: AgentProviderId,
    onReset: () -> Unit,
) {
    OutlinedButton(
        modifier = Modifier.testTag("provider_reset_button_${provider.name}"),
        onClick = onReset,
        enabled = status != ProviderResetStatus.Resetting,
    ) {
        Text(stringResource(Res.string.agents_reset_provider_settings))
    }
    when (status) {
        ProviderResetStatus.Idle -> Unit
        ProviderResetStatus.Resetting -> Text(
            text = stringResource(Res.string.agents_resetting),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ProviderResetStatus.Failure -> Text(
            text = stringResource(Res.string.agents_reset_failed),
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ProviderPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.agents_provider_placeholder_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(Res.string.agents_provider_placeholder_message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun OllamaEndpointSettings(
    modifier: Modifier = Modifier,
    state: OllamaEndpointSettingsUiState,
    onAction: (OllamaEndpointSettingsAction) -> Unit,
) {
    val isTesting = state.connectionStatus == OllamaConnectionStatus.Testing
    val isLoadingModels = state.modelsStatus == OllamaModelsStatus.Loading
    val isResetting = state.resetStatus == ProviderResetStatus.Resetting
    val isBusy = isTesting || isLoadingModels || isResetting
    val isHostInvalid = state.connectionStatus == OllamaConnectionStatus.InvalidHost
    val isPortInvalid = state.connectionStatus == OllamaConnectionStatus.InvalidPort
    val isModelSelectionRequired = state.modelsStatus == OllamaModelsStatus.SelectionRequired
    val connectionButtonLabelRes: StringResource = when {
        isTesting -> Res.string.agents_ollama_testing_connection
        else -> Res.string.agents_ollama_test_connection
    }
    val modelsButtonLabelRes: StringResource = when {
        isLoadingModels -> Res.string.agents_ollama_loading_models
        else -> Res.string.agents_ollama_load_models
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
            supportingText = if (isHostInvalid) {
                {
                    Text(
                        modifier = Modifier.testTag("ollama_host_error"),
                        text = stringResource(Res.string.agents_ollama_invalid_host),
                    )
                }
            } else {
                null
            },
            isError = isHostInvalid,
            singleLine = true,
            enabled = !isBusy,
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ollama_port_field"),
            value = state.portInput,
            onValueChange = { onAction(OllamaEndpointSettingsAction.PortChanged(it)) },
            label = { Text(stringResource(Res.string.agents_ollama_port_label)) },
            supportingText = if (isPortInvalid) {
                {
                    Text(
                        modifier = Modifier.testTag("ollama_port_error"),
                        text = stringResource(Res.string.agents_ollama_invalid_port),
                    )
                }
            } else {
                null
            },
            isError = isPortInvalid,
            singleLine = true,
            enabled = !isBusy,
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ollama_model_field"),
            value = state.modelInput,
            onValueChange = {},
            label = { Text(stringResource(Res.string.agents_ollama_model_label)) },
            supportingText = if (isModelSelectionRequired) {
                {
                    Text(
                        modifier = Modifier.testTag("ollama_model_error"),
                        text = stringResource(Res.string.agents_ollama_model_selection_required),
                    )
                }
            } else {
                null
            },
            isError = isModelSelectionRequired,
            readOnly = true,
            singleLine = true,
            enabled = !isBusy,
        )
        Button(
            modifier = Modifier.testTag("ollama_test_connection_button"),
            onClick = { onAction(OllamaEndpointSettingsAction.TestConnection) },
            enabled = !isBusy,
        ) {
            when {
                isTesting -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
                else -> Icon(
                    painter = painterResource(Res.drawable.ic_play_arrow),
                    contentDescription = null,
                )
            }
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(connectionButtonLabelRes),
            )
        }
        OllamaConnectionStatusText(
            modifier = Modifier.testTag("ollama_connection_status"),
            status = state.connectionStatus,
        )
        OutlinedButton(
            modifier = Modifier.testTag("ollama_load_models_button"),
            onClick = { onAction(OllamaEndpointSettingsAction.LoadModels) },
            enabled = !isBusy,
        ) {
            when {
                isLoadingModels -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
                else -> Icon(
                    painter = painterResource(Res.drawable.ic_refresh),
                    contentDescription = null,
                )
            }
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(modelsButtonLabelRes),
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.availableModels.forEach { modelName ->
                    FilterChip(
                        selected = modelName == state.modelInput.trim(),
                        onClick = { onAction(OllamaEndpointSettingsAction.ModelSelected(modelName)) },
                        label = { Text(modelName) },
                    )
                }
            }
        }
        ProviderResetControls(
            status = state.resetStatus,
            provider = AgentProviderId.Ollama,
            onReset = { onAction(OllamaEndpointSettingsAction.ResetRequested) },
        )
    }
}

@Composable
private fun OllamaConnectionStatusText(
    modifier: Modifier = Modifier,
    status: OllamaConnectionStatus,
) = when (status) {
    OllamaConnectionStatus.Idle,
    OllamaConnectionStatus.Testing -> Unit
    OllamaConnectionStatus.Success -> Text(
        modifier = modifier,
        text = stringResource(Res.string.agents_ollama_connection_success),
        color = MaterialTheme.colorScheme.primary,
    )
    OllamaConnectionStatus.InvalidHost,
    OllamaConnectionStatus.InvalidPort -> Unit
    is OllamaConnectionStatus.Failure -> Text(
        modifier = modifier,
        text = status.message?.let {
            stringResource(Res.string.agents_ollama_connection_failed_detail, it)
        } ?: stringResource(Res.string.agents_ollama_connection_failed),
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun OllamaModelsStatusText(modifier: Modifier = Modifier, status: OllamaModelsStatus) {
    when (status) {
        OllamaModelsStatus.Idle,
        OllamaModelsStatus.Loading,
        OllamaModelsStatus.SelectionRequired -> Unit
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

@Preview(widthDp = 920, heightDp = 720)
@Composable
private fun AgentConfigScreenPreview(
    @PreviewParameter(AgentConfigScreenPreviewProvider::class) state: AgentConfigUiState,
) = PreferableMaterialTheme {
    AgentConfigScreen(
        state = state,
        ollamaState = OllamaEndpointSettingsPreviewProvider.loadedState,
        onAction = {},
        onOllamaAction = {},
    )
}

@Preview
@Composable
private fun OllamaEndpointSettingsPreview(
    @PreviewParameter(OllamaEndpointSettingsPreviewProvider::class) state: OllamaEndpointSettingsUiState,
) = PreferableMaterialTheme { OllamaEndpointSettings(state = state, onAction = {}) }
