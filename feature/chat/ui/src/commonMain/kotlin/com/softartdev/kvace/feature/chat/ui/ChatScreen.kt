package com.softartdev.kvace.feature.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softartdev.kvace.core.ui.KvaceScreenScaffold
import com.softartdev.kvace.feature.chat.domain.ChatMessage
import com.softartdev.kvace.feature.chat.domain.MessageAuthor
import com.softartdev.kvace.feature.chat.presentation.ChatAction
import com.softartdev.kvace.feature.chat.presentation.ChatUiState
import com.softartdev.kvace.feature.chat.presentation.ChatViewModel
import kvace.feature.chat.ui.generated.resources.Res
import kvace.feature.chat.ui.generated.resources.chat_author_assistant
import kvace.feature.chat.ui.generated.resources.chat_author_event
import kvace.feature.chat.ui.generated.resources.chat_author_error
import kvace.feature.chat.ui.generated.resources.chat_author_reasoning
import kvace.feature.chat.ui.generated.resources.chat_author_system
import kvace.feature.chat.ui.generated.resources.chat_author_tool
import kvace.feature.chat.ui.generated.resources.chat_author_user
import kvace.feature.chat.ui.generated.resources.chat_input_placeholder
import kvace.feature.chat.ui.generated.resources.chat_loading_response
import kvace.feature.chat.ui.generated.resources.chat_preview_assistant_message
import kvace.feature.chat.ui.generated.resources.chat_preview_event_message
import kvace.feature.chat.ui.generated.resources.chat_preview_input
import kvace.feature.chat.ui.generated.resources.chat_preview_reasoning_message
import kvace.feature.chat.ui.generated.resources.chat_preview_system_message
import kvace.feature.chat.ui.generated.resources.chat_preview_tool_message
import kvace.feature.chat.ui.generated.resources.chat_preview_user_message
import kvace.feature.chat.ui.generated.resources.chat_send_content_description
import kvace.feature.chat.ui.generated.resources.chat_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(viewModel) {
        viewModel.observeConversation()
    }
    val state by viewModel.uiState.collectAsState()
    ChatScreenContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun ChatScreenContent(
    state: ChatUiState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val messageListState = rememberLazyListState()
    val lastMessageText = state.messages.lastOrNull()?.text
    val scrollProgress by remember {
        derivedStateOf {
            val layoutInfo = messageListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            if (totalItems == 0 || lastVisibleItemIndex == null) {
                0f
            } else {
                ((lastVisibleItemIndex + 1).toFloat() / totalItems.toFloat()).coerceIn(0f, 1f)
            }
        }
    }
    val canScrollMessages by remember {
        derivedStateOf {
            messageListState.canScrollBackward || messageListState.canScrollForward
        }
    }

    LaunchedEffect(state.messages.size, lastMessageText, state.isSending) {
        val itemCount = state.messages.size + if (state.isSending) 1 else 0
        if (itemCount > 0) {
            messageListState.scrollToItem(itemCount - 1)
        }
    }

    KvaceScreenScaffold(
        title = stringResource(Res.string.chat_title),
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = messageListState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    ChatMessageItem(message)
                }
                if (state.isSending) {
                    item(key = "chat-loading") {
                        ChatLoadingIndicator()
                    }
                }
            }
            ChatMessageListScrollIndicator(
                isSending = state.isSending,
                canScroll = canScrollMessages,
                progress = scrollProgress,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.inputText,
                    onValueChange = { onAction(ChatAction.InputChanged(it)) },
                    placeholder = { Text(stringResource(Res.string.chat_input_placeholder)) },
                    enabled = !state.isSending,
                    singleLine = false,
                    minLines = 1,
                    maxLines = 4,
                )
                Button(
                    onClick = { onAction(ChatAction.SendClicked) },
                    enabled = state.inputText.isNotBlank() && !state.isSending,
                ) {
                    if (state.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(Res.string.chat_send_content_description),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageListScrollIndicator(
    isSending: Boolean,
    canScroll: Boolean,
    progress: Float,
) {
    when {
        isSending -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        canScroll -> LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = { progress },
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun ChatLoadingIndicator() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
            Text(
                text = stringResource(Res.string.chat_loading_response),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage) {
    val colors = when (message.author) {
        MessageAuthor.User -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        MessageAuthor.Assistant -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        MessageAuthor.Reasoning -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        MessageAuthor.Tool -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        MessageAuthor.Event -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        MessageAuthor.System -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        MessageAuthor.Error -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    }
    Card(colors = colors) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AssistChip(
                onClick = {},
                label = { Text(message.author.label) },
            )
            Text(message.text)
        }
    }
}

private val MessageAuthor.label: String
    @Composable get() = when (this) {
        MessageAuthor.User -> stringResource(Res.string.chat_author_user)
        MessageAuthor.Assistant -> stringResource(Res.string.chat_author_assistant)
        MessageAuthor.Reasoning -> stringResource(Res.string.chat_author_reasoning)
        MessageAuthor.Tool -> stringResource(Res.string.chat_author_tool)
        MessageAuthor.Event -> stringResource(Res.string.chat_author_event)
        MessageAuthor.System -> stringResource(Res.string.chat_author_system)
        MessageAuthor.Error -> stringResource(Res.string.chat_author_error)
    }

@Preview
@Composable
private fun ChatScreenPreview() {
    MaterialTheme {
        ChatScreenContent(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(
                        id = 1L,
                        author = MessageAuthor.System,
                        text = stringResource(Res.string.chat_preview_system_message),
                        createdAtMillis = 0L,
                    ),
                    ChatMessage(
                        id = 2L,
                        author = MessageAuthor.User,
                        text = stringResource(Res.string.chat_preview_user_message),
                        createdAtMillis = 0L,
                    ),
                    ChatMessage(
                        id = 3L,
                        author = MessageAuthor.Reasoning,
                        text = stringResource(Res.string.chat_preview_reasoning_message),
                        createdAtMillis = 0L,
                    ),
                    ChatMessage(
                        id = 4L,
                        author = MessageAuthor.Tool,
                        text = stringResource(Res.string.chat_preview_tool_message),
                        createdAtMillis = 0L,
                    ),
                    ChatMessage(
                        id = 5L,
                        author = MessageAuthor.Assistant,
                        text = stringResource(Res.string.chat_preview_assistant_message),
                        createdAtMillis = 0L,
                    ),
                    ChatMessage(
                        id = 6L,
                        author = MessageAuthor.Event,
                        text = stringResource(Res.string.chat_preview_event_message),
                        createdAtMillis = 0L,
                    ),
                ),
                inputText = stringResource(Res.string.chat_preview_input),
                isSending = true,
            ),
            onAction = {},
        )
    }
}
