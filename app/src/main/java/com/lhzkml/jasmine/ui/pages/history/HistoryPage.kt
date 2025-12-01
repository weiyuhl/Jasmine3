package com.lhzkml.jasmine.ui.pages.history;

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
 
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pin
import com.composables.icons.lucide.PinOff
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Trash2
import kotlinx.coroutines.launch
import com.lhzkml.jasmine.R
import com.lhzkml.jasmine.data.model.Conversation
import com.lhzkml.jasmine.ui.components.nav.BackButton
import com.lhzkml.jasmine.ui.context.LocalNavController
import com.lhzkml.jasmine.utils.navigateToChatPage
import com.lhzkml.jasmine.utils.plus
import com.lhzkml.jasmine.utils.toLocalDateTime
import org.koin.androidx.compose.koinViewModel

@Composable
fun HistoryPage(vm: HistoryVM = koinViewModel()) {
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var pendingDeleteConversation by remember { mutableStateOf<Conversation?>(null) }

    val allConversations by vm.conversations.collectAsStateWithLifecycle()
    val searchConversations by produceState(emptyList(), searchText) {
        runCatching {
            vm.searchConversations(searchText).collect {
                value = it
            }
        }.onFailure {
            it.printStackTrace()
        }
    }
    val showConversations = if (searchText.isEmpty()) {
        allConversations
    } else {
        searchConversations
    }
    val snackMessageDeleted = stringResource(R.string.history_page_conversation_deleted)
    val snackMessageUndo = stringResource(R.string.history_page_undo)

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.history_page_title))
                    },
                    navigationIcon = {
                        BackButton()
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                isSearchVisible = !isSearchVisible
                                if (!isSearchVisible) {
                                    searchText = ""
                                }
                            }
                        ) {
                            Icon(Lucide.Search, contentDescription = stringResource(R.string.history_page_search))
                        }
                        IconButton(
                            onClick = {
                                showDeleteAllDialog = true
                            }
                        ) {
                            Icon(Lucide.Trash2, contentDescription = stringResource(R.string.history_page_delete_all))
                        }
                    }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { contentPadding ->
        LazyColumn(
            contentPadding = contentPadding + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isSearchVisible) {
                stickyHeader {
                    SearchInput(
                        value = searchText,
                        onValueChange = { searchText = it },
                        onDismiss = {
                            isSearchVisible = false
                            searchText = ""
                        }
                    )
                }
            }
            items(showConversations, key = { it.id }) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    onTogglePin = { vm.togglePinStatus(conversation.id) },
                    onDelete = { pendingDeleteConversation = conversation },
                    onClick = {
                        navigateToChatPage(navController, conversation.id)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                )
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.history_page_delete_all_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteAllConversations()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAllDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (pendingDeleteConversation != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteConversation = null },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.history_page_delete_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val c = pendingDeleteConversation!!
                        vm.deleteConversation(c)
                        pendingDeleteConversation = null
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = snackMessageDeleted,
                                actionLabel = snackMessageUndo,
                                withDismissAction = true,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                vm.restoreConversation(c)
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteConversation = null }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(stringResource(R.string.history_page_search_placeholder))
            },
            shape = RoundedCornerShape(50),
            singleLine = true,
        )
        IconButton(
            onClick = onDismiss,
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.history_page_cancel)
            )
        }
    }
}

 

@Composable
private fun ConversationItem(
    conversation: Conversation,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit = {},
    onTogglePin: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(25),
        modifier = modifier
    ) {
        ListItem(
            headlineContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (conversation.isPinned) {
                        Icon(
                            imageVector = Lucide.Pin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = conversation.title.ifBlank { stringResource(R.string.history_page_new_conversation) }
                            .trim(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            },
            supportingContent = {
                Text(conversation.createAt.toLocalDateTime())
            },
            trailingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onTogglePin
                    ) {
                        Icon(
                            if (conversation.isPinned) Lucide.PinOff else Lucide.Pin,
                            contentDescription = if (conversation.isPinned) stringResource(R.string.history_page_unpin) else stringResource(
                                R.string.history_page_pin
                            )
                        )
                    }
                    IconButton(
                        onClick = onDelete
                    ) {
                        Icon(
                            Lucide.Trash2,
                            contentDescription = stringResource(R.string.history_page_delete)
                        )
                    }
                }
            }
        )
    }
}
