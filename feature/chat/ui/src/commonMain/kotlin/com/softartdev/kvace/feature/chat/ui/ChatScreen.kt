@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@file:Suppress("DEPRECATION")

package com.softartdev.kvace.feature.chat.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.softartdev.kvace.core.ui.KvaceVerticalPaneExpansionDragHandle
import com.softartdev.kvace.feature.chat.domain.ChatMessage
import com.softartdev.kvace.feature.chat.domain.ChatSummary
import com.softartdev.kvace.feature.chat.domain.Conversation
import com.softartdev.kvace.feature.chat.domain.MessageAuthor
import com.softartdev.kvace.feature.chat.presentation.ChatAction
import com.softartdev.kvace.feature.chat.presentation.ChatUiState
import com.softartdev.kvace.feature.chat.presentation.ChatViewModel
import com.softartdev.kvace.core.ui.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChatScreen(modifier: Modifier = Modifier, viewModel: ChatViewModel) {
    LaunchedEffect(viewModel) {
        viewModel.observeChats()
    }
    val state by viewModel.uiState.collectAsState()
    ChatScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    state: ChatUiState,
    onAction: (ChatAction) -> Unit,
    autoNavigateToSelectedChat: Boolean = false,
) {
    val navigator: ThreePaneScaffoldNavigator<Long> =
        rememberListDetailPaneScaffoldNavigator<Long>()
    val paneExpansionState: PaneExpansionState = rememberPaneExpansionState()
    val coroutineScope = rememberCoroutineScope()
    val canNavigateBack = navigator.canNavigateBack()
    var navigateToCreatedChat by remember { mutableStateOf(false) }

    LaunchedEffect(autoNavigateToSelectedChat, navigateToCreatedChat, state.selectedConversation?.id) {
        val selectedChatId = state.selectedConversation?.id
        if ((autoNavigateToSelectedChat || navigateToCreatedChat) && selectedChatId != null) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, selectedChatId)
            navigateToCreatedChat = false
        }
    }

    ListDetailPaneScaffold(
        modifier = modifier.fillMaxSize(),
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            ChatMasterPane(
                state = state,
                onAction = onAction,
                onNewChatClick = {
                    navigateToCreatedChat = true
                    onAction(ChatAction.NewChatClicked)
                },
                onChatClick = { chatId ->
                    onAction(ChatAction.ChatSelected(chatId))
                    coroutineScope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, chatId)
                    }
                },
            )
        },
        detailPane = {
            ChatDetailPane(
                state = state,
                onAction = onAction,
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

    ChatDialogs(
        state = state,
        onAction = { action ->
            onAction(action)
            if (
                action == ChatAction.DeleteChatConfirmed &&
                canNavigateBack &&
                state.deleteDialog?.conversationId == state.selectedConversation?.id
            ) {
                coroutineScope.launch { navigator.navigateBack() }
            }
        },
    )
}

@Composable
private fun ChatMasterPane(
    modifier: Modifier = Modifier,
    state: ChatUiState,
    onAction: (ChatAction) -> Unit,
    onNewChatClick: () -> Unit,
    onChatClick: (Long) -> Unit,
) = Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(Res.string.chat_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
        )
    },
    floatingActionButton = {
        ExtendedFloatingActionButton(
            text = { Text(stringResource(Res.string.chat_new_label)) },
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_add),
                    contentDescription = stringResource(Res.string.chat_new_content_description),
                )
            },
            onClick = onNewChatClick,
        )
    },
) { paddingValues ->
    if (state.chats.isEmpty()) {
        EmptyChatList(
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
        ) {
            items(state.chats, key = { it.id }) { chat ->
                ChatSummaryItem(
                    chat = chat,
                    selected = chat.id == state.selectedConversation?.id,
                    onClick = { onChatClick(chat.id) },
                    onRenameClick = { onAction(ChatAction.RenameChatRequested(chat.id)) },
                    onDeleteClick = { onAction(ChatAction.DeleteChatRequested(chat.id)) },
                )
            }
        }
    }
}

@Composable
private fun ChatDetailPane(
    modifier: Modifier = Modifier,
    state: ChatUiState,
    onAction: (ChatAction) -> Unit,
    onBackClick: (() -> Unit)?,
) = Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
        TopAppBar(
            title = {
                Text(
                    text = state.selectedConversation?.title ?: stringResource(Res.string.chat_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = stringResource(Res.string.chat_back_content_description),
                        )
                    }
                }
            },
            actions = {
                state.selectedConversation?.let { conversation ->
                    ChatActionsMenu(
                        onRenameClick = { onAction(ChatAction.RenameChatRequested(conversation.id)) },
                        onDeleteClick = { onAction(ChatAction.DeleteChatRequested(conversation.id)) },
                    )
                }
            },
        )
    },
) { paddingValues ->
    val selectedConversation = state.selectedConversation
    if (selectedConversation == null) {
        SelectChatPlaceholder(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
        )
    } else {
        ConversationDetail(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            conversation = selectedConversation,
            inputText = state.inputText,
            isSending = state.isSending,
            onAction = onAction,
        )
    }
}

@Composable
private fun ConversationDetail(
    modifier: Modifier = Modifier,
    conversation: Conversation,
    inputText: String,
    isSending: Boolean,
    onAction: (ChatAction) -> Unit,
) {
    key(conversation.id) {
        ConversationDetailContent(
            modifier = modifier,
            conversation = conversation,
            inputText = inputText,
            isSending = isSending,
            onAction = onAction,
        )
    }
}

@Composable
private fun ConversationDetailContent(
    modifier: Modifier = Modifier,
    conversation: Conversation,
    inputText: String,
    isSending: Boolean,
    onAction: (ChatAction) -> Unit,
) {
    val initialItemCount = conversation.messages.size + if (isSending) 1 else 0
    val initialItemIndex = (initialItemCount - 1).coerceAtLeast(0)
    val messageListState = rememberLazyListState(initialFirstVisibleItemIndex = initialItemIndex)
    val coroutineScope = rememberCoroutineScope()
    val lastMessageText = conversation.messages.lastOrNull()?.text
    val itemCount = conversation.messages.size + if (isSending) 1 else 0
    val scrollProgress by remember(messageListState) {
        derivedStateOf {
            messageListState.scrollProgress()
        }
    }
    val canScrollMessages by remember(messageListState) {
        derivedStateOf {
            messageListState.canScrollBackward || messageListState.canScrollForward
        }
    }
    val isNearLatestMessage by remember(messageListState, itemCount) {
        derivedStateOf {
            messageListState.isNearLatestMessage(itemCount)
        }
    }
    LaunchedEffect(conversation.id, conversation.messages.size, lastMessageText, isSending) {
        if (itemCount > 0 && isNearLatestMessage) {
            messageListState.animateScrollToItem(itemCount - 1)
        }
    }

    Column(
        modifier = modifier,
    ) {
        ChatMessageListScrollIndicator(
            isSending = isSending,
            canScroll = canScrollMessages,
            progress = scrollProgress,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                state = messageListState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                if (conversation.messages.isEmpty()) {
                    item(key = "chat-empty-detail") {
                        Text(
                            modifier = Modifier.padding(vertical = 12.dp),
                            text = stringResource(Res.string.chat_no_messages),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(conversation.messages, key = { it.id }) { message ->
                    ChatMessageItem(
                        message = message,
                        onAction = onAction,
                    )
                }
                if (isSending) {
                    item(key = "chat-loading") {
                        ChatLoadingIndicator()
                    }
                }
            }
            if (!isNearLatestMessage && itemCount > 0) {
                SmallFloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                    onClick = {
                        coroutineScope.launch {
                            messageListState.animateScrollToItem(itemCount - 1)
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_vertical_align_bottom),
                        contentDescription = stringResource(
                            Res.string.chat_scroll_to_latest_content_description,
                        ),
                    )
                }
            }
        }
        ChatInputRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            inputText = inputText,
            isSending = isSending,
            onAction = onAction,
        )
    }
}

@Composable
private fun ChatSummaryItem(
    chat: ChatSummary,
    selected: Boolean,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val containerColor: Color = when {
        selected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = chat.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = chat.lastMessagePreview ?: stringResource(Res.string.chat_no_preview),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.chat_message_count, chat.messageCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ChatActionsMenu(
                    onRenameClick = onRenameClick,
                    onDeleteClick = onDeleteClick,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = containerColor),
    )
}

@Composable
private fun ChatInputRow(
    modifier: Modifier = Modifier,
    inputText: String,
    isSending: Boolean,
    onAction: (ChatAction) -> Unit,
) {
    val canSend = inputText.isNotBlank()
    fun submitOrStop() {
        onAction(
            when {
                isSending -> ChatAction.StopGenerationClicked
                else -> ChatAction.SendClicked
            }
        )
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .onPreviewKeyEvent { event ->
                    if (
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.Enter &&
                        !event.isShiftPressed &&
                        canSend &&
                        !isSending
                    ) {
                        onAction(ChatAction.SendClicked)
                        true
                    } else {
                        false
                    }
                },
            value = inputText,
            onValueChange = { onAction(ChatAction.InputChanged(it)) },
            placeholder = { Text(stringResource(Res.string.chat_input_placeholder)) },
            enabled = !isSending,
            singleLine = false,
            minLines = 1,
            maxLines = 4,
        )
        Button(
            onClick = ::submitOrStop,
            enabled = isSending || canSend,
        ) {
            Icon(
                painter = when {
                    isSending -> painterResource(Res.drawable.ic_stop_circle)
                    else -> painterResource(Res.drawable.ic_send)
                },
                contentDescription = stringResource(
                    when {
                        isSending -> Res.string.chat_stop_generation_content_description
                        else -> Res.string.chat_send_content_description
                    }
                ),
            )
        }
    }
}

@Composable
private fun EmptyChatList(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.chat_empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.chat_empty_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectChatPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.chat_select_placeholder_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.chat_select_placeholder_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChatMessageListScrollIndicator(
    modifier: Modifier = Modifier,
    isSending: Boolean,
    canScroll: Boolean,
    progress: Float,
) {
    when {
        isSending -> LinearProgressIndicator(modifier = modifier.fillMaxWidth())
        canScroll -> LinearProgressIndicator(
            modifier = modifier.fillMaxWidth(),
            progress = { progress },
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun ChatLoadingIndicator() = Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.chat_loading_response),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChatActionsMenu(
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    IconButton(
        modifier = Modifier.size(40.dp),
        onClick = { menuExpanded = true },
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_more_vert),
            contentDescription = stringResource(Res.string.chat_summary_menu_content_description),
        )
    }
    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.chat_rename_confirm_title)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_drive_file_rename_outline),
                    contentDescription = null,
                )
            },
            onClick = {
                menuExpanded = false
                onRenameClick()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.chat_message_action_delete)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_delete),
                    contentDescription = null,
                )
            },
            onClick = {
                menuExpanded = false
                onDeleteClick()
            },
        )
    }
}

@Composable
private fun ChatDialogs(state: ChatUiState, onAction: (ChatAction) -> Unit) {
    state.renameDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = { onAction(ChatAction.RenameChatDismissed) },
            title = { Text(stringResource(Res.string.chat_rename_confirm_title)) },
            text = {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = dialog.titleInput,
                    onValueChange = { onAction(ChatAction.RenameChatInputChanged(it)) },
                    label = { Text(stringResource(Res.string.chat_rename_title_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = dialog.titleInput.isNotBlank(),
                    onClick = { onAction(ChatAction.RenameChatConfirmed) },
                ) {
                    Text(stringResource(Res.string.chat_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(ChatAction.RenameChatDismissed) }) {
                    Text(stringResource(Res.string.chat_cancel))
                }
            },
        )
    }
    state.deleteDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = { onAction(ChatAction.DeleteChatDismissed) },
            title = { Text(stringResource(Res.string.chat_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.chat_delete_confirm_message, dialog.title)) },
            confirmButton = {
                TextButton(onClick = { onAction(ChatAction.DeleteChatConfirmed) }) {
                    Text(stringResource(Res.string.chat_message_action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(ChatAction.DeleteChatDismissed) }) {
                    Text(stringResource(Res.string.chat_cancel))
                }
            },
        )
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage, onAction: (ChatAction) -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuExpanded = true
                },
                onLongClickLabel = stringResource(Res.string.chat_message_menu_long_click_label),
            ),
            colors = CardDefaults.cardColors(containerColor = message.author.containerColor),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(message.author.stringRes)) },
                    )
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_more_vert),
                            contentDescription = stringResource(Res.string.chat_message_menu_content_description),
                        )
                    }
                }
                SelectionContainer {
                    Text(message.text)
                }
            }
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.chat_message_action_copy)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_content_copy),
                        contentDescription = null,
                    )
                },
                onClick = {
                    clipboardManager.setText(AnnotatedString(message.text))
                    menuExpanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.chat_message_action_share)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_share),
                        contentDescription = null,
                    )
                },
                onClick = {
                    onAction(ChatAction.MessageShared(message.id))
                    menuExpanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.chat_message_action_delete)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_delete),
                        contentDescription = null,
                    )
                },
                onClick = {
                    onAction(ChatAction.MessageDeleted(message.id))
                    menuExpanded = false
                },
            )
        }
    }
}

private fun LazyListState.scrollProgress(): Float {
    scrollIndicatorState?.progress()?.let { return it }
    val layoutInfo = layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty() || layoutInfo.totalItemsCount == 0) return 0f
    val averageItemSize = visibleItems.sumOf { it.size }.toFloat() / visibleItems.size
    val viewportSize = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(0)
    val contentSize = averageItemSize * layoutInfo.totalItemsCount
    val scrollRange = contentSize - viewportSize
    if (scrollRange <= 0f) return 0f
    val firstVisible = visibleItems.first()
    val scrollOffset = firstVisible.index * averageItemSize - firstVisible.offset
    return (scrollOffset / scrollRange).coerceIn(0f, 1f)
}

private fun ScrollIndicatorState.progress(): Float? {
    if (scrollOffset == Int.MAX_VALUE || contentSize == Int.MAX_VALUE || viewportSize == Int.MAX_VALUE) return null
    val scrollRange = contentSize - viewportSize
    if (scrollRange <= 0) return null
    return (scrollOffset.toFloat() / scrollRange.toFloat()).coerceIn(0f, 1f)
}

private fun LazyListState.isNearLatestMessage(itemCount: Int): Boolean {
    if (itemCount == 0 || !canScrollForward) return true
    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return true
    return lastVisibleItemIndex >= itemCount - NEAR_LATEST_ITEM_THRESHOLD
}

private val MessageAuthor.containerColor: Color
    @Composable get() = when (this) {
        MessageAuthor.User -> MaterialTheme.colorScheme.primaryContainer
        MessageAuthor.Assistant -> MaterialTheme.colorScheme.secondaryContainer
        MessageAuthor.Reasoning -> MaterialTheme.colorScheme.tertiaryContainer
        MessageAuthor.Tool -> MaterialTheme.colorScheme.surfaceVariant
        MessageAuthor.Event -> MaterialTheme.colorScheme.surfaceVariant
        MessageAuthor.System -> MaterialTheme.colorScheme.surfaceVariant
        MessageAuthor.Error -> MaterialTheme.colorScheme.errorContainer
    }

private val MessageAuthor.stringRes: StringResource
    get() = when (this) {
        MessageAuthor.User -> Res.string.chat_author_user
        MessageAuthor.Assistant -> Res.string.chat_author_assistant
        MessageAuthor.Reasoning -> Res.string.chat_author_reasoning
        MessageAuthor.Tool -> Res.string.chat_author_tool
        MessageAuthor.Event -> Res.string.chat_author_event
        MessageAuthor.System -> Res.string.chat_author_system
        MessageAuthor.Error -> Res.string.chat_author_error
    }

private const val NEAR_LATEST_ITEM_THRESHOLD = 2

@Preview(widthDp = 920, heightDp = 720)
@Composable
fun ChatScreenWidePreview() {
    MaterialTheme {
        ChatScreen(
            state = ChatScreenPreviewProvider.selectedState,
            onAction = {},
        )
    }
}

@Preview(widthDp = 920, heightDp = 720)
@Composable
fun ChatScreenWidePlaceholderPreview() {
    MaterialTheme {
        ChatScreen(
            state = ChatScreenPreviewProvider.placeholderState,
            onAction = {},
        )
    }
}

@Preview(widthDp = 390, heightDp = 760)
@Composable
fun ChatScreenCompactPreview() {
    MaterialTheme {
        ChatScreen(
            state = ChatScreenPreviewProvider.selectedState,
            onAction = {},
            autoNavigateToSelectedChat = true,
        )
    }
}

@Preview(widthDp = 920, heightDp = 720)
@Composable
fun ChatScreenSendingPreview() {
    MaterialTheme {
        ChatScreen(
            state = ChatScreenPreviewProvider.sendingState,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ChatScreenPreview(
    @PreviewParameter(ChatScreenPreviewProvider::class) state: ChatUiState
) = MaterialTheme { ChatScreen(state = state, onAction = {}) }
