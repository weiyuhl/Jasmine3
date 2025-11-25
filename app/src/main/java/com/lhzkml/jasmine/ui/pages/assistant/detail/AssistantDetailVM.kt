package com.lhzkml.jasmine.ui.pages.assistant.detail

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.lhzkml.jasmine.data.datastore.Settings
import com.lhzkml.jasmine.data.datastore.SettingsStore
import com.lhzkml.jasmine.data.model.Assistant
import com.lhzkml.jasmine.data.model.AssistantMemory
import com.lhzkml.jasmine.data.model.Avatar
import com.lhzkml.jasmine.data.repository.MemoryRepository
import com.lhzkml.jasmine.utils.deleteChatFiles
import kotlin.uuid.Uuid

private const val TAG = "AssistantDetailVM"

class AssistantDetailVM(
    private val id: String,
    private val settingsStore: SettingsStore,
    private val memoryRepository: MemoryRepository,
    private val context: Application,
) : ViewModel() {
    private val assistantId = Uuid.parse(id)

    val settings: StateFlow<Settings> =
        settingsStore.settingsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, Settings.dummy())

    val mcpServerConfigs = settingsStore
        .settingsFlow.map { settings ->
            settings.mcpServers
        }.stateIn(
            scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList()
        )

    val assistant: StateFlow<Assistant> = settingsStore
        .settingsFlow
        .map { settings ->
            settings.assistants.find { it.id == assistantId } ?: Assistant()
        }.stateIn(
            scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = Assistant()
        )

    val memories = memoryRepository.getMemoriesOfAssistantFlow(assistantId.toString())
        .stateIn(
            scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList()
        )

    val providers = settingsStore
        .settingsFlow
        .map { settings ->
            settings.providers
        }.stateIn(
            scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList()
        )


    fun update(assistant: Assistant) {
        viewModelScope.launch {
            val settings = settings.value
            settingsStore.update(
                settings = settings.copy(
                    assistants = settings.assistants.map {
                        if (it.id == assistant.id) {
                            checkAvatarDelete(old = it, new = assistant) // 删除旧头像
                            assistant
                        } else {
                            it
                        }
                    })
            )
        }
    }

    fun addMemory(memory: AssistantMemory) {
        viewModelScope.launch {
            memoryRepository.addMemory(
                assistantId = assistantId.toString(),
                content = memory.content
            )
        }
    }

    fun updateMemory(memory: AssistantMemory) {
        viewModelScope.launch {
            memoryRepository.updateContent(id = memory.id, content = memory.content)
        }
    }

    fun deleteMemory(memory: AssistantMemory) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(id = memory.id)
            val key = assistantId.toString()
            val current = settings.value.disabledMemories[key] ?: emptyList()
            if (current.isNotEmpty() && memory.id in current) {
                val updated = current.filterNot { it == memory.id }
                val newMap = settings.value.disabledMemories.toMutableMap().apply {
                    if (updated.isEmpty()) remove(key) else put(key, updated)
                }
                settingsStore.update { it.copy(disabledMemories = newMap) }
            }
        }
    }

    fun setMemoryEnabled(id: Int, enabled: Boolean) {
        viewModelScope.launch {
            val key = assistantId.toString()
            val current = settings.value.disabledMemories[key] ?: emptyList()
            val updated = if (enabled) current.filterNot { it == id } else current.plus(id).distinct()
            val newMap = settings.value.disabledMemories.toMutableMap().apply {
                if (updated.isEmpty()) remove(key) else put(key, updated)
            }
            settingsStore.update { it.copy(disabledMemories = newMap) }
        }
    }

    fun checkAvatarDelete(old: Assistant, new: Assistant) {
        if (old.avatar is Avatar.Image && old.avatar != new.avatar) {
            context.deleteChatFiles(listOf(old.avatar.url.toUri()))
        }
    }

}
