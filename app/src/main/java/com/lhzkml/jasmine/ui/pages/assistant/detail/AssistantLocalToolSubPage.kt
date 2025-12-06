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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lhzkml.jasmine.data.datastore.Settings
import com.lhzkml.jasmine.data.datastore.SettingsStore
import org.koin.compose.koinInject
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
    val settings = Settings.dummy()
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

        // 文件系统工具已移除
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

