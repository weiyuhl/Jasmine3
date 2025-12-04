package com.lhzkml.jasmine.ui.pages.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
 
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
 
import com.lhzkml.jasmine.R
import com.lhzkml.jasmine.Screen
import com.lhzkml.jasmine.data.datastore.DEFAULT_ASSISTANTS_IDS
import com.lhzkml.jasmine.data.datastore.Settings
import com.lhzkml.jasmine.data.model.Assistant
import com.lhzkml.jasmine.data.model.AssistantMemory
import com.lhzkml.jasmine.ui.components.nav.BackButton
import com.lhzkml.jasmine.ui.components.ui.FormItem
import com.lhzkml.jasmine.ui.components.ui.Tag
import com.lhzkml.jasmine.ui.components.ui.TagType
import com.lhzkml.jasmine.ui.components.ui.Tooltip
import com.lhzkml.jasmine.ui.components.ui.UIAvatar
import com.lhzkml.jasmine.ui.context.LocalNavController
import com.lhzkml.jasmine.ui.hooks.EditState
import com.lhzkml.jasmine.ui.hooks.EditStateContent
import com.lhzkml.jasmine.ui.hooks.useEditState
import com.lhzkml.jasmine.ui.modifier.onClick
 
import org.koin.androidx.compose.koinViewModel
 
import kotlin.uuid.Uuid
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete

@Composable
fun AssistantPage(vm: AssistantVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val createState = useEditState<Assistant> {
        vm.addAssistant(it)
    }
    val navController = LocalNavController.current

    val filteredAssistants = settings.assistants

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(stringResource(R.string.assistant_management_page_title))
            }, navigationIcon = {
                BackButton()
            }, actions = {
                IconButton(
                    onClick = {
                        createState.open(Assistant())
                    }) {
                    Icon(Icons.Filled.Add, stringResource(R.string.assistant_page_add))
                }
            })
        }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .consumeWindowInsets(it),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val lazyListState = rememberLazyStaggeredGridState()

            

            LazyVerticalStaggeredGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(16.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                state = lazyListState,
                columns = StaggeredGridCells.Fixed(2)
            ) {
                items(filteredAssistants, key = { assistant -> assistant.id }) { assistant ->
                    val memories by vm.getMemories(assistant).collectAsStateWithLifecycle(
                        initialValue = emptyList(),
                    )
                    AssistantItem(
                        assistant = assistant,
                        settings = settings,
                        memories = memories,
                        onEdit = {
                            navController.navigate(Screen.AssistantDetail(id = assistant.id.toString()))
                        },
                        onDelete = {
                            vm.removeAssistant(assistant)
                        },
                        onCopy = {
                            vm.copyAssistant(assistant)
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        dragHandle = {}
                    )
                }
            }
        }
    }
    AssistantCreationSheet(createState)
}

 

@Composable
private fun AssistantCreationSheet(
    state: EditState<Assistant>,
) {
    state.EditStateContent { assistant, update ->
        ModalBottomSheet(
            onDismissRequest = {
                state.dismiss()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = {},
            sheetGesturesEnabled = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FormItem(
                        label = {
                            Text(stringResource(R.string.assistant_page_name))
                        },
                    ) {
                        OutlinedTextField(
                            value = assistant.name, onValueChange = {
                                update(
                                    assistant.copy(
                                        name = it
                                    )
                                )
                            }, modifier = Modifier.fillMaxWidth()
                        )
                    }

                    
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = {
                            state.dismiss()
                        }) {
                        Text(stringResource(R.string.assistant_page_cancel))
                    }
                    TextButton(
                        onClick = {
                            state.confirm()
                        }) {
                        Text(stringResource(R.string.assistant_page_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantItem(
    assistant: Assistant,
    settings: Settings,
    modifier: Modifier = Modifier,
    memories: List<AssistantMemory>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    dragHandle: @Composable () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(), onClick = onEdit
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UIAvatar(
                    name = assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) },
                    value = assistant.avatar,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                dragHandle()
            }

            Text(
                text = assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (assistant.enableMemory) {
                Tag(type = TagType.SUCCESS) {
                    Text(stringResource(R.string.assistant_page_memory_count, memories.size))
                }
            }

            

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (assistant.id !in DEFAULT_ASSISTANTS_IDS) {
                    Tooltip(tooltip = { Text(stringResource(R.string.assistant_page_delete)) }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.assistant_page_delete),
                            modifier = Modifier
                                .onClick {
                                    showDeleteDialog = true
                                }
                                .size(18.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.65f),
                        )
                    }
                }
                Tooltip(tooltip = { Text(stringResource(R.string.assistant_page_clone)) }) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.assistant_page_clone),
                        modifier = Modifier
                            .onClick {
                                onCopy()
                            }
                            .size(18.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
                }
            }
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text(stringResource(R.string.confirm_delete))
            },
            text = {
                Text(stringResource(R.string.assistant_page_delete_dialog_text))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
