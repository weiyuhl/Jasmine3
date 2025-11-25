package com.lhzkml.jasmine.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lhzkml.jasmine.data.datastore.Settings
import com.lhzkml.jasmine.data.datastore.SettingsStore
import org.koin.compose.koinInject
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import android.provider.DocumentsContract
import com.lhzkml.jasmine.R
import com.lhzkml.jasmine.data.ai.tools.LocalToolOption
import com.lhzkml.jasmine.data.model.Assistant
import com.lhzkml.jasmine.ui.components.ui.FormItem
import kotlinx.coroutines.launch

@Composable
fun AssistantLocalToolSubPage(
    assistant: Assistant,
    onUpdate: (Assistant) -> Unit
) {
    val store = koinInject<SettingsStore>()
    val settings by store.settingsFlow.collectAsStateWithLifecycle(initialValue = Settings.dummy())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            scope.launch {
                store.update { it.copy(defaultSaveDir = uri.toString()) }
            }
        }
    }
    val isAuthorized = settings.defaultSaveDir?.let {
        val u = Uri.parse(it)
        context.contentResolver.persistedUriPermissions.any { perm ->
            perm.uri == u && perm.isReadPermission && perm.isWritePermission
        }
    } ?: false
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // JavaScript 引擎工具卡片
        LocalToolCard(
            title = stringResource(R.string.assistant_page_local_tools_javascript_engine_title),
            description = stringResource(R.string.assistant_page_local_tools_javascript_engine_desc),
            isEnabled = assistant.localTools.contains(LocalToolOption.JavascriptEngine),
            onToggle = { enabled ->
                val newLocalTools = if (enabled) {
                    assistant.localTools + LocalToolOption.JavascriptEngine
                } else {
                    assistant.localTools - LocalToolOption.JavascriptEngine
                }
                onUpdate(assistant.copy(localTools = newLocalTools))
            }
        )

        // Markdown 保存工具卡片
        val mdDescription = buildString {
            append(stringResource(R.string.assistant_page_local_tools_markdown_desc))
            append('\n')
            append(stringResource(R.string.assistant_page_local_tools_markdown_note_saf_restriction))
            append('\n')
            append(stringResource(R.string.assistant_page_local_tools_markdown_note_utf8))
        }
        LocalToolCard(
            title = stringResource(R.string.assistant_page_local_tools_markdown_title),
            description = mdDescription,
            isEnabled = assistant.localTools.contains(LocalToolOption.MarkdownTxt),
            onToggle = { enabled ->
                val newLocalTools = if (enabled) {
                    assistant.localTools + LocalToolOption.MarkdownTxt
                } else {
                    assistant.localTools - LocalToolOption.MarkdownTxt
                }
                onUpdate(assistant.copy(localTools = newLocalTools))
                if (enabled && !isAuthorized) {
                    launcher.launch(null)
                }
            },
            content = {
                val friendly = settings.defaultSaveDir?.let { value ->
                    val u = Uri.parse(value)
                    val docId = DocumentsContract.getTreeDocumentId(u)
                    val path = if (docId.startsWith("primary:")) {
                        "/storage/emulated/0/" + docId.removePrefix("primary:")
                    } else {
                        docId.replace(':', '/')
                    }
                    path
                }
                FormItem(
                    label = { Text(text = friendly ?: stringResource(R.string.assistant_page_local_tools_markdown_not_authorized)) },
                    tail = {
                        Button(onClick = { launcher.launch(null) }) { Text(stringResource(R.string.assistant_page_local_tools_markdown_choose_directory)) }
                    }
                )
            }
        )

        val fsDescription = buildString {
            append(stringResource(R.string.assistant_page_local_tools_filesystem_capability_read_file))
            append('\n')
            append(stringResource(R.string.assistant_page_local_tools_filesystem_capability_list_directory))
            append('\n')
            append(stringResource(R.string.assistant_page_local_tools_filesystem_capability_get_dir_tree))
            append('\n')
            append(stringResource(R.string.assistant_page_local_tools_filesystem_capability_search_pathnames_only))
            append('\n')
            append(stringResource(R.string.assistant_page_local_tools_filesystem_capability_search_for_files))
            append('\n')
            append(stringResource(R.string.assistant_page_local_tools_filesystem_capability_search_in_file))
            append('\n')
            append(stringResource(R.string.assistant_page_local_tools_filesystem_capability_create_file_or_folder))
            append('\n')
            append(stringResource(R.string.assistant_page_local_tools_filesystem_capability_delete_file_or_folder))
            append('\n')
            append(stringResource(R.string.assistant_page_local_tools_filesystem_capability_edit_file))
            append('\n')
            append(stringResource(R.string.assistant_page_local_tools_filesystem_capability_rewrite_file))
            append('\n')
            append(stringResource(R.string.assistant_page_local_tools_filesystem_capability_saf_restriction))
        }
        LocalToolCard(
            title = stringResource(R.string.assistant_page_local_tools_filesystem_title),
            description = fsDescription,
            isEnabled = assistant.localTools.contains(LocalToolOption.FileSystem),
            onToggle = { enabled ->
                val newLocalTools = if (enabled) {
                    assistant.localTools + LocalToolOption.FileSystem
                } else {
                    assistant.localTools - LocalToolOption.FileSystem
                }
                onUpdate(assistant.copy(localTools = newLocalTools))
                if (enabled && !isAuthorized) {
                    launcher.launch(null)
                }
            },
            content = {
                val friendly = settings.defaultSaveDir?.let { value ->
                    val u = Uri.parse(value)
                    val docId = DocumentsContract.getTreeDocumentId(u)
                    val path = if (docId.startsWith("primary:")) {
                        "/storage/emulated/0/" + docId.removePrefix("primary:")
                    } else {
                        docId.replace(':', '/')
                    }
                    path
                }
                FormItem(
                    label = { Text(text = friendly ?: stringResource(R.string.assistant_page_local_tools_markdown_not_authorized)) },
                    tail = {
                        Button(onClick = { launcher.launch(null) }) { Text(stringResource(R.string.assistant_page_local_tools_markdown_choose_directory)) }
                    }
                )
            }
        )
    }
}

@Composable
private fun LocalToolCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable (() -> Unit)? = null
) {
    Card {
        FormItem(
            modifier = Modifier.padding(8.dp),
            label = {
                Text(title)
            },
            description = {
                Text(description)
            },
            tail = {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle
                )
            },
            content = {
                if (isEnabled && content != null) {
                    content()
                }
            }
        )
    }
}

