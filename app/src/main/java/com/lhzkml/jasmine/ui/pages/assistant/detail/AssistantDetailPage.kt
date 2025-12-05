package com.lhzkml.jasmine.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.lhzkml.jasmine.ui.modifier.clearFocusOnTap
import androidx.compose.ui.focus.onFocusChanged
import com.lhzkmlai.provider.ModelType
import com.lhzkmlai.provider.ProviderSetting
import com.lhzkml.jasmine.R
import com.lhzkml.jasmine.data.ai.mcp.McpServerConfig
import com.lhzkml.jasmine.data.model.Assistant
import com.lhzkml.jasmine.ui.components.ai.McpPicker
import com.lhzkml.jasmine.ui.components.ai.ModelSelector
import com.lhzkml.jasmine.ui.components.ai.ReasoningButton
import com.lhzkml.jasmine.ui.components.nav.BackButton
import com.lhzkml.jasmine.ui.components.ui.FormItem
import com.lhzkml.jasmine.ui.components.ui.Tag
import com.lhzkml.jasmine.ui.components.ui.TagType
import com.lhzkml.jasmine.ui.components.ui.UIAvatar
import com.lhzkml.jasmine.utils.toFixed
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

@Composable
fun AssistantDetailPage(id: String) {
    val vm: AssistantDetailVM = koinViewModel(
        parameters = {
            parametersOf(id)
        }
    )
    val scope = rememberCoroutineScope()

    val mcpServerConfigs by vm.mcpServerConfigs.collectAsStateWithLifecycle()
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    val memories by vm.memories.collectAsStateWithLifecycle()
    val providers by vm.providers.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    fun onUpdate(assistant: Assistant) {
        vm.update(assistant)
    }

    val tabs = listOf(
        stringResource(R.string.assistant_page_tab_basic),
        stringResource(R.string.assistant_page_tab_prompt),
        stringResource(R.string.assistant_page_tab_memory),
        stringResource(R.string.assistant_page_tab_request),
        stringResource(R.string.assistant_page_tab_mcp),
        stringResource(R.string.assistant_page_tab_local_tools)
    )
    val pagerState = rememberPagerState { tabs.size }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = assistant.name.ifBlank {
                            stringResource(R.string.assistant_page_default_assistant)
                        },
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    BackButton()
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .clearFocusOnTap(),
        ) {
            SecondaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 20.dp,
                minTabWidth = 20.dp,
            ) {
                tabs.fastForEachIndexed { index, tab ->
                    Tab(
                        selected = index == pagerState.currentPage,
                        onClick = { scope.launch { pagerState.scrollToPage(index) } },
                        text = {
                            Text(tab)
                        }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        AssistantBasicSettings(
                            assistant = assistant,
                            providers = providers,
                            onUpdate = { onUpdate(it) },
                            vm = vm
                        )
                    }

                    1 -> {
                        AssistantPromptSubPage(
                            assistant = assistant,
                            onUpdate = {
                                onUpdate(it)
                            }
                        )
                    }

                    2 -> {
                        AssistantMemorySettings(
                            assistant = assistant,
                            memories = memories,
                            onUpdateAssistant = { onUpdate(it) },
                            onDeleteMemory = { vm.deleteMemory(it) },
                            onAddMemory = { vm.addMemory(it) },
                            onUpdateMemory = { vm.updateMemory(it) },
                            disabledIds = settings.disabledMemories[assistant.id.toString()] ?: emptyList(),
                            onSetEnabled = { memId, enabled -> vm.setMemoryEnabled(memId, enabled) }
                        )
                    }

                    3 -> {
                        AssistantCustomRequestSettings(assistant = assistant) {
                            onUpdate(it)
                        }
                    }

                    4 -> {
                        AssistantMcpSettings(
                            assistant = assistant,
                            onUpdate = {
                                onUpdate(it)
                            },
                            mcpServerConfigs = mcpServerConfigs
                        )
                    }

                    5 -> {
                        AssistantLocalToolSubPage(
                            assistant = assistant,
                            onUpdate = { onUpdate(it) }
                        )
                    }
                }
            }
        }
    }

}

@Composable
private fun AssistantBasicSettings(
    assistant: Assistant,
    providers: List<ProviderSetting>,
    onUpdate: (Assistant) -> Unit,
    vm: AssistantDetailVM,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UIAvatar(
                value = assistant.avatar,
                name = assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) },
                onUpdate = { avatar ->
                    onUpdate(
                        assistant.copy(
                            avatar = avatar
                        )
                    )
                },
                modifier = Modifier.size(80.dp)
            )
        }

        Card {
            FormItem(
                label = {
                    Text(stringResource(R.string.assistant_page_name))
                },
                modifier = Modifier.padding(8.dp),
            ) {
                OutlinedTextField(
                    value = assistant.name,
                    onValueChange = {
                        onUpdate(
                            assistant.copy(
                                name = it
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

        }

        Card {
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_chat_model))
                },
                description = {
                    Text(stringResource(R.string.assistant_page_chat_model_desc))
                },
                content = {
                    ModelSelector(
                        modelId = assistant.chatModelId,
                        providers = providers,
                        type = ModelType.CHAT,
                        onSelect = {
                            onUpdate(
                                assistant.copy(
                                    chatModelId = it.id
                                )
                            )
                        },
                    )
                }
            )
            HorizontalDivider()
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_temperature))
                },
                tail = {
                    Switch(
                        checked = assistant.temperature != null,
                        onCheckedChange = { enabled ->
                            onUpdate(
                                assistant.copy(
                                    temperature = if (enabled) 1.0f else null
                                )
                            )
                        }
                    )
                }
            ) {
                if (assistant.temperature != null) {
                    var temperatureText by remember(assistant.temperature) { mutableStateOf(assistant.temperature?.toFixed(2) ?: "") }
                    OutlinedTextField(
                        value = temperatureText,
                        onValueChange = { text ->
                            temperatureText = text
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    val t = temperatureText.trim()
                                    if (t.isEmpty()) {
                                        temperatureText = assistant.temperature?.toFixed(2) ?: ""
                                    } else {
                                        val v = t.toFloatOrNull()
                                        val clamped = v?.coerceIn(0f, 2f)
                                        val rounded = clamped?.let { (it * 100).roundToInt() / 100f }
                                        if (rounded != null) {
                                            onUpdate(
                                                assistant.copy(
                                                    temperature = rounded
                                                )
                                            )
                                        }
                                    }
                                }
                            },
                        placeholder = { Text("0.0–2.0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val currentTemperature = assistant.temperature
                        val tagType = when (currentTemperature) {
                            in 0.0f..0.3f -> TagType.INFO
                            in 0.3f..1.0f -> TagType.SUCCESS
                            in 1.0f..1.5f -> TagType.WARNING
                            in 1.5f..2.0f -> TagType.ERROR
                            else -> TagType.ERROR
                        }
                        Tag(
                            type = TagType.INFO
                        ) {
                            Text(
                                text = "$currentTemperature"
                            )
                        }

                        Tag(
                            type = tagType
                        ) {
                            Text(
                                text = when (currentTemperature) {
                                    in 0.0f..0.3f -> stringResource(R.string.assistant_page_strict)
                                    in 0.3f..1.0f -> stringResource(R.string.assistant_page_balanced)
                                    in 1.0f..1.5f -> stringResource(R.string.assistant_page_creative)
                                    in 1.5f..2.0f -> stringResource(R.string.assistant_page_chaotic)
                                    else -> "?"
                                }
                            )
                        }
                    }
                }
            }
            HorizontalDivider()
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_top_p))
                },
                description = {
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.assistant_page_top_p_warning))
                        }
                    )
                },
                tail = {
                    Switch(
                        checked = assistant.topP != null,
                        onCheckedChange = { enabled ->
                            onUpdate(
                                assistant.copy(
                                    topP = if (enabled) 1.0f else null
                                )
                            )
                        }
                    )
                }
            ) {
                assistant.topP?.let { topP ->
                    var topPText by remember(assistant.topP) { mutableStateOf(topP.toFixed(2)) }
                    OutlinedTextField(
                        value = topPText,
                        onValueChange = { text ->
                            topPText = text
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    val t = topPText.trim()
                                    if (t.isEmpty()) {
                                        topPText = topP.toFixed(2)
                                    } else {
                                        val v = t.toFloatOrNull()
                                        val clamped = v?.coerceIn(0f, 1f)
                                        val rounded = clamped?.let { (it * 100).roundToInt() / 100f }
                                        if (rounded != null) {
                                            onUpdate(
                                                assistant.copy(
                                                    topP = rounded
                                                )
                                            )
                                        }
                                    }
                                }
                            },
                        placeholder = { Text("0.0–1.0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    Text(
                        text = stringResource(
                            R.string.assistant_page_top_p_value,
                            topP.toString()
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                    )
                }
            }
            HorizontalDivider()
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_context_message_size))
                },
                description = {
                    Text(
                        text = stringResource(R.string.assistant_page_context_message_desc),
                    )
                }
            ) {
                var contextSizeText by remember(assistant.contextMessageSize) { mutableStateOf(assistant.contextMessageSize.toString()) }
                OutlinedTextField(
                    value = contextSizeText,
                    onValueChange = { text ->
                        contextSizeText = text
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                val t = contextSizeText.trim()
                                if (t.isEmpty()) {
                                    contextSizeText = assistant.contextMessageSize.toString()
                                } else {
                                    val v = t.toIntOrNull()
                                    val clamped = v?.coerceIn(0, 512)
                                    if (clamped != null) {
                                        onUpdate(
                                            assistant.copy(
                                                contextMessageSize = clamped
                                            )
                                        )
                                    }
                                }
                            }
                        },
                    placeholder = { Text("0–512") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    text = if(assistant.contextMessageSize > 0) stringResource(
                        R.string.assistant_page_context_message_count,
                        assistant.contextMessageSize
                    ) else stringResource(R.string.assistant_page_context_message_unlimited),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                )
            }
            HorizontalDivider()
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_stream_output))
                },
                description = {
                    Text(stringResource(R.string.assistant_page_stream_output_desc))
                },
                tail = {
                    Switch(
                        checked = assistant.streamOutput,
                        onCheckedChange = {
                            onUpdate(
                                assistant.copy(
                                    streamOutput = it
                                )
                            )
                        }
                    )
                }
            )
            HorizontalDivider()
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_thinking_budget))
                },
            ) {
                ReasoningButton(
                    reasoningTokens = assistant.thinkingBudget ?: 0,
                    onUpdateReasoningTokens = { tokens ->
                        onUpdate(
                            assistant.copy(
                                thinkingBudget = tokens
                            )
                        )
                    }
                )
            }
            HorizontalDivider()
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_max_tokens))
                },
                description = {
                    Text(stringResource(R.string.assistant_page_max_tokens_desc))
                }
            ) {
                OutlinedTextField(
                    value = assistant.maxTokens?.toString() ?: "",
                    onValueChange = { text ->
                        val tokens = if (text.isBlank()) {
                            null
                        } else {
                            text.toIntOrNull()?.takeIf { it > 0 }
                        }
                        onUpdate(
                            assistant.copy(
                                maxTokens = tokens
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(stringResource(R.string.assistant_page_max_tokens_no_limit))
                    },
                    supportingText = {
                        if (assistant.maxTokens != null) {
                            Text(stringResource(R.string.assistant_page_max_tokens_limit, assistant.maxTokens))
                        } else {
                            Text(stringResource(R.string.assistant_page_max_tokens_no_token_limit))
                        }
                    }
                )
            }
        }

        
    }
}

@Composable
private fun AssistantCustomRequestSettings(
    assistant: Assistant,
    onUpdate: (Assistant) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CustomHeaders(
            headers = assistant.customHeaders,
            onUpdate = {
                onUpdate(
                    assistant.copy(
                        customHeaders = it
                    )
                )
            }
        )

        HorizontalDivider()

        CustomBodies(
            customBodies = assistant.customBodies,
            onUpdate = {
                onUpdate(
                    assistant.copy(
                        customBodies = it
                    )
                )
            }
        )
    }
}

@Composable
private fun AssistantMcpSettings(
    assistant: Assistant,
    onUpdate: (Assistant) -> Unit,
    mcpServerConfigs: List<McpServerConfig>
) {
    McpPicker(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        assistant = assistant,
        servers = mcpServerConfigs,
        onUpdateAssistant = onUpdate,
    )
}

