package com.softartdev.kvace.core.presentation

import kotlinx.coroutines.Job

interface SnackbarInteractor {
    fun showMessage(message: SnackbarMessage): Job?
}

sealed interface SnackbarMessage {
    data class Text(
        val value: String,
        val copyable: Boolean = false,
    ) : SnackbarMessage

    data class Resource(
        val resource: SnackbarMessageResource,
        val suffix: String? = null,
        val copyable: Boolean = false,
    ) : SnackbarMessage
}

enum class SnackbarMessageResource {
    AgentConfigurationSaveFailed,
    AgentProviderSelectionFailed,
    ChatSendFailed,
    ChatShareFailed,
    SettingsLoadFailed,
    SettingsSaveFailed,
}
