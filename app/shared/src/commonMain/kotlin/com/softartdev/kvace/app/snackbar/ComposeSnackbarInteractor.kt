package com.softartdev.kvace.app.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.platform.Clipboard
import com.softartdev.kvace.app.clipboard.setPlainText
import com.softartdev.kvace.core.presentation.SnackbarInteractor
import com.softartdev.kvace.core.presentation.SnackbarMessage
import com.softartdev.kvace.core.presentation.SnackbarMessageResource
import com.softartdev.kvace.core.ui.resources.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

class ComposeSnackbarInteractor : SnackbarInteractor {
    private var hostState: SnackbarHostState? = null
    private var clipboard: Clipboard? = null
    private var scope: CoroutineScope? = null
    private var messageJob: Job? = null

    fun attach(
        hostState: SnackbarHostState,
        clipboard: Clipboard,
        scope: CoroutineScope,
    ) {
        messageJob?.cancel()
        this.hostState = hostState
        this.clipboard = clipboard
        this.scope = scope
    }

    fun release(hostState: SnackbarHostState) {
        if (this.hostState === hostState) {
            messageJob?.cancel()
            messageJob = null
            this.hostState = null
            clipboard = null
            scope = null
        }
    }

    override fun showMessage(message: SnackbarMessage): Job? {
        messageJob?.cancel()
        return scope?.launch {
            val hostState = hostState ?: return@launch
            val text = message.resolveText()
            val copyable = message.copyable
            val result = hostState.showSnackbar(
                message = text,
                actionLabel = if (copyable) getString(Res.string.snackbar_action_copy) else null,
                duration = if (copyable) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (copyable && result == SnackbarResult.ActionPerformed) {
                clipboard?.setPlainText(text)
            }
        }.also { messageJob = it }
    }

    private suspend fun SnackbarMessage.resolveText(): String {
        val base = when (this) {
            is SnackbarMessage.Text -> value
            is SnackbarMessage.Resource -> getString(resource = resource.stringRes)
        }
        val suffix = (this as? SnackbarMessage.Resource)?.suffix
        return suffix?.takeIf(String::isNotBlank)?.let { "$base $it" } ?: base
    }

    private val SnackbarMessage.copyable: Boolean
        get() = when (this) {
            is SnackbarMessage.Resource -> copyable
            is SnackbarMessage.Text -> copyable
        }

    private val SnackbarMessageResource.stringRes: StringResource
        get() = when (this) {
            SnackbarMessageResource.AgentConfigurationSaveFailed ->
                Res.string.snackbar_agent_configuration_save_failed
            SnackbarMessageResource.AgentProviderSelectionFailed ->
                Res.string.snackbar_agent_provider_selection_failed
            SnackbarMessageResource.ChatSendFailed ->
                Res.string.snackbar_chat_send_failed
            SnackbarMessageResource.ChatShareFailed ->
                Res.string.snackbar_chat_share_failed
            SnackbarMessageResource.SettingsLoadFailed ->
                Res.string.snackbar_settings_load_failed
            SnackbarMessageResource.SettingsSaveFailed ->
                Res.string.snackbar_settings_save_failed
        }
}
