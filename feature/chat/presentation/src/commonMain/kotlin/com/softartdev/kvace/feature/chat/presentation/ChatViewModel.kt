package com.softartdev.kvace.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.feature.chat.domain.ChatRepository
import com.softartdev.kvace.feature.chat.domain.MessageSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val sendMessage: MessageSender,
    private val dispatchers: CoroutineDispatchers,
    private val logger: Logger,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var isObservingConversation = false

    fun observeConversation() {
        if (isObservingConversation) return
        isObservingConversation = true

        chatRepository.conversation.onEach { conversation ->
            _uiState.update {
                it.copy(
                    title = conversation.title,
                    messages = conversation.messages,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.InputChanged -> _uiState.update { it.copy(inputText = action.text) }
            ChatAction.SendClicked -> sendCurrentInput()
        }
    }

    private fun sendCurrentInput() {
        val text = _uiState.value.inputText
        if (text.isBlank() || _uiState.value.isSending) return

        _uiState.update { it.copy(inputText = "", isSending = true) }
        viewModelScope.launch(dispatchers.io) {
            try {
                sendMessage(text)
            } catch (error: Throwable) {
                logger.e(error) { "ChatViewModel failed to send message" }
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }
}
