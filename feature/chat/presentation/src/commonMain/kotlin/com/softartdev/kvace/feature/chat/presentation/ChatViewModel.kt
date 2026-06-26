package com.softartdev.kvace.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.softartdev.kvace.core.domain.util.CoroutineDispatchers
import com.softartdev.kvace.core.presentation.SnackbarInteractor
import com.softartdev.kvace.core.presentation.SnackbarMessage
import com.softartdev.kvace.core.presentation.SnackbarMessageResource
import com.softartdev.kvace.core.presentation.TextShareInteractor
import com.softartdev.kvace.feature.chat.domain.ChatRepository
import com.softartdev.kvace.feature.chat.domain.MessageAuthor
import com.softartdev.kvace.feature.chat.domain.MessageSender
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val sendMessage: MessageSender,
    private val textShareInteractor: TextShareInteractor,
    private val snackbarInteractor: SnackbarInteractor,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {
    private val logger = Logger.withTag("ChatViewModel")

    val uiState: StateFlow<ChatUiState>
        field: MutableStateFlow<ChatUiState> = MutableStateFlow(ChatUiState())

    private var isObservingChats = false
    private var sendJob: Job? = null

    fun observeChats() {
        if (isObservingChats) return
        isObservingChats = true

        chatRepository.chatSummaries.onEach { chats ->
            uiState.update {
                it.copy(chats = chats)
            }
        }.launchIn(viewModelScope)

        chatRepository.selectedConversation.onEach { conversation ->
            uiState.update {
                it.copy(selectedConversation = conversation)
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch(dispatchers.io) {
            chatRepository.loadChats()
        }
    }

    fun onAction(action: ChatAction) = when (action) {
        is ChatAction.ChatSelected -> selectChat(action.id)
        is ChatAction.InputChanged -> uiState.update { it.copy(inputText = action.text) }
        is ChatAction.MessageDeleted -> deleteMessage(action.messageId)
        is ChatAction.MessageShared -> shareMessage(action.messageId)
        is ChatAction.RenameChatInputChanged -> uiState.update {
            it.copy(renameDialog = it.renameDialog?.copy(titleInput = action.title))
        }
        is ChatAction.RenameChatRequested -> requestRenameChat(action.id)
        is ChatAction.DeleteChatRequested -> requestDeleteChat(action.id)
        is ChatAction.DeleteChatConfirmed -> confirmDeleteChat()
        is ChatAction.DeleteChatDismissed -> uiState.update { it.copy(deleteDialog = null) }
        is ChatAction.NewChatClicked -> createChat()
        is ChatAction.RenameChatConfirmed -> confirmRenameChat()
        is ChatAction.RenameChatDismissed -> uiState.update { it.copy(renameDialog = null) }
        is ChatAction.SendClicked -> sendCurrentInput()
        is ChatAction.StopGenerationClicked -> stopGeneration()
    }

    private fun createChat() {
        viewModelScope.launch(dispatchers.io) {
            try {
                chatRepository.createConversation()
            } catch (error: Throwable) {
                currentCoroutineContext().ensureActive()
                logger.e(error) { "Failed to create chat" }
            }
        }
    }

    private fun selectChat(id: Long) {
        viewModelScope.launch(dispatchers.io) {
            try {
                chatRepository.selectConversation(id)
            } catch (error: Throwable) {
                currentCoroutineContext().ensureActive()
                logger.e(error) { "Failed to select chat $id" }
            }
        }
    }

    private fun sendCurrentInput() {
        val text = uiState.value.inputText
        val conversationId = uiState.value.selectedConversation?.id
        if (text.isBlank() || conversationId == null || uiState.value.isSending) return

        uiState.update { it.copy(inputText = "", isSending = true) }
        lateinit var job: Job
        job = viewModelScope.launch(dispatchers.io, start = CoroutineStart.LAZY) {
            try {
                sendMessage(conversationId, text)
            } catch (error: Throwable) {
                currentCoroutineContext().ensureActive()
                logger.e(error) { "Failed to send message" }
            } finally {
                if (sendJob === job) {
                    sendJob = null
                    uiState.update { it.copy(isSending = false) }
                }
            }
        }
        sendJob = job
        job.start()
    }

    private fun stopGeneration() {
        val conversationId = uiState.value.selectedConversation?.id ?: return
        val job = sendJob ?: return
        sendJob = null
        job.cancel()
        uiState.update { it.copy(isSending = false) }
        viewModelScope.launch(dispatchers.io) {
            try {
                chatRepository.appendMessage(
                    conversationId = conversationId,
                    author = MessageAuthor.Event,
                    text = GENERATION_STOPPED_MESSAGE,
                )
            } catch (error: Throwable) {
                currentCoroutineContext().ensureActive()
                logger.e(error) { "Failed to append generation stopped event" }
            }
        }
    }

    private fun shareMessage(messageId: Long) = try {
        val message = uiState.value.messages.first { it.id == messageId }
        textShareInteractor.shareText(message.text)
    } catch (error: Throwable) {
        logger.e(error) { "Failed to share message $messageId" }
        val snackbarMessage = error.message?.let { msg: String ->
            SnackbarMessage.Text(value = msg, copyable = true)
        } ?: SnackbarMessage.Resource(SnackbarMessageResource.ChatShareFailed)
        snackbarInteractor.showMessage(snackbarMessage)
    }

    private fun deleteMessage(messageId: Long) {
        val conversationId = uiState.value.selectedConversation?.id ?: return
        viewModelScope.launch(dispatchers.io) {
            try {
                chatRepository.deleteMessage(conversationId, messageId)
            } catch (error: Throwable) {
                currentCoroutineContext().ensureActive()
                logger.e(error) { "Failed to delete message $messageId" }
            }
        }
    }

    private fun requestRenameChat(conversationId: Long) {
        val title = uiState.value.chatTitle(conversationId) ?: return
        uiState.update {
            it.copy(renameDialog = ChatRenameDialogState(conversationId = conversationId, titleInput = title))
        }
    }

    private fun confirmRenameChat() {
        val dialog = uiState.value.renameDialog ?: return
        val title = dialog.titleInput.trim()
        if (title.isBlank()) return
        uiState.update { it.copy(renameDialog = null) }
        viewModelScope.launch(dispatchers.io) {
            try {
                chatRepository.renameConversation(dialog.conversationId, title)
            } catch (error: Throwable) {
                currentCoroutineContext().ensureActive()
                logger.e(error) { "Failed to rename chat ${dialog.conversationId}" }
            }
        }
    }

    private fun requestDeleteChat(conversationId: Long) {
        val title = uiState.value.chatTitle(conversationId) ?: return
        uiState.update {
            it.copy(deleteDialog = ChatDeleteDialogState(conversationId = conversationId, title = title))
        }
    }

    private fun confirmDeleteChat() {
        val dialog = uiState.value.deleteDialog ?: return
        val deletingSelectedChat = uiState.value.selectedConversation?.id == dialog.conversationId
        if (deletingSelectedChat) {
            sendJob?.cancel()
            sendJob = null
        }
        uiState.update {
            it.copy(
                deleteDialog = null,
                renameDialog = it.renameDialog?.takeUnless { rename ->
                    rename.conversationId == dialog.conversationId
                },
                inputText = if (deletingSelectedChat) "" else it.inputText,
                isSending = if (deletingSelectedChat) false else it.isSending,
            )
        }
        viewModelScope.launch(dispatchers.io) {
            try {
                chatRepository.deleteConversation(dialog.conversationId)
            } catch (error: Throwable) {
                currentCoroutineContext().ensureActive()
                logger.e(error) { "Failed to delete chat ${dialog.conversationId}" }
            }
        }
    }

    private fun ChatUiState.chatTitle(conversationId: Long): String? =
        selectedConversation?.takeIf { it.id == conversationId }?.title
            ?: chats.firstOrNull { it.id == conversationId }?.title

    private companion object {
        const val GENERATION_STOPPED_MESSAGE = "Generation stopped"
    }
}
